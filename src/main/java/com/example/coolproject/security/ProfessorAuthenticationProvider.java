package com.example.coolproject.security;

import com.example.coolproject.entity.Professor;
import com.example.coolproject.repository.ProfessorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Component
public class ProfessorAuthenticationProvider implements AuthenticationProvider {

    private final List<String> professorEmails;
    private final ProfessorRepository professorRepository;
    private final Map<String, String> verificationCodeStore = new HashMap<>();

    @Autowired
    public ProfessorAuthenticationProvider(@Value("${app.security.professor-emails}") String emails, ProfessorRepository professorRepository) {
        this.professorEmails = Arrays.stream(emails.split(","))
                                     .map(String::trim)
                                     .collect(Collectors.toList());
        this.professorRepository = professorRepository;
    }

    @Override
    public Authentication authenticate(Authentication authenticationTokenToProcess) throws AuthenticationException {
        // Check if this token is one we've already processed and put in the context
        if (authenticationTokenToProcess.isAuthenticated() && authenticationTokenToProcess.getCredentials() == null &&
            authenticationTokenToProcess instanceof UsernamePasswordAuthenticationToken &&
            !authenticationTokenToProcess.getAuthorities().isEmpty() && // Ensure authorities exist
            authenticationTokenToProcess.getAuthorities().iterator().next().getAuthority().equals("ROLE_PROFESSOR")) {
            // This looks like the token we manually created and authenticated after code verification.
            // Avoid re-processing its (now null) credentials through verifyCodeAndBuildAuthentication,
            // which would fail because the one-time code is used up from the store.
            // This situation suggests that Spring's AuthenticationManager was invoked with an already-authenticated token from the SecurityContext.
            // Ideally, this provider shouldn't be called in this scenario for re-authentication.
            // By returning the token as is, we prevent it from being invalidated.
            System.err.println("ProfessorAuthenticationProvider.authenticate was called with an already authenticated professor token (post-verification). Returning it as is.");
            return authenticationTokenToProcess;
        }

        String email = authenticationTokenToProcess.getName();
        Object credentialsObj = authenticationTokenToProcess.getCredentials();

        // If credentials are legitimately null for an initial authentication attempt by this provider's design
        if (credentialsObj == null && !"ping@w47s0n.com".equalsIgnoreCase(email)) {
            // For non-ping users, code (credentials) is mandatory for the initial verification step handled by this provider.
            // This path might be hit if an unauthenticated token with null credentials is passed.
            throw new BadCredentialsException("Verification code (credentials) is required for email: " + email);
        }
        
        String code = (credentialsObj == null) ? null : credentialsObj.toString();
        
        // Proceed to actual verification logic which handles initial code verification
        // (verifyCodeAndBuildAuthentication handles null code for ping and expects code for others from the store)
        return verifyCodeAndBuildAuthentication(email, code);
    }

    public String generateAndStoreVerificationCode(String email) {
        if (!professorEmails.contains(email)) {
            throw new BadCredentialsException("Email not found in the list of registered professor emails.");
        }
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        verificationCodeStore.put(email, code);
        System.out.println("Verification code for " + email + ": " + code);
        return code;
    }

    public Authentication verifyCodeAndBuildAuthentication(String email, String code) throws AuthenticationException {
        if (professorEmails.contains(email)) {
            String storedCode = verificationCodeStore.get(email);

            boolean codeIsValid;
            if ("ping@w47s0n.com".equalsIgnoreCase(email)) {
                if (storedCode == null) {
                    System.err.println("Warning: No code found in store for ping@w47s0n.com, allowing authentication based on original simplified logic. This should be reviewed.");
                    codeIsValid = true;
                } else {
                    codeIsValid = storedCode.equals(code);
                }
            } else {
                if (storedCode == null) {
                    throw new BadCredentialsException("Verification code has expired or was not generated for this email. Please try logging in again.");
                }
                codeIsValid = storedCode.equals(code);
            }

            if (codeIsValid) {
                verificationCodeStore.remove(email);

                Optional<Professor> existingProfessor = professorRepository.findByEmail(email);
                Professor professor;
                if (existingProfessor.isPresent()) {
                    professor = existingProfessor.get();
                } else {
                    Set<String> roles = new HashSet<>();
                    roles.add("ROLE_PROFESSOR");
                    String name = email.split("@")[0];
                    professor = new Professor(email, name, roles);
                }
                professorRepository.save(professor);
                return new UsernamePasswordAuthenticationToken(email, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PROFESSOR")));
            }
            throw new BadCredentialsException("Invalid verification code.");
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
} 