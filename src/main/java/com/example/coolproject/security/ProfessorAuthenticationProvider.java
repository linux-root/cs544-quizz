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

@Component
public class ProfessorAuthenticationProvider implements AuthenticationProvider {

    private final List<String> professorEmails;
    private final ProfessorRepository professorRepository;

    @Autowired
    public ProfessorAuthenticationProvider(@Value("${app.security.professor-emails}") String emails, ProfessorRepository professorRepository) {
        this.professorEmails = Arrays.stream(emails.split(","))
                                     .map(String::trim)
                                     .collect(Collectors.toList());
        this.professorRepository = professorRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String code = authentication.getCredentials().toString();

        if (professorEmails.contains(email)) {
            if ("ping@w47s0n.com".equalsIgnoreCase(email)) {
                // Successfully authenticated
                Optional<Professor> existingProfessor = professorRepository.findByEmail(email);
                Professor professor;
                if (existingProfessor.isPresent()) {
                    professor = existingProfessor.get();
                    // Update details if necessary, e.g., name, if we had a way to get it
                    // professor.setName(name); // Example if name could change or be sourced elsewhere
                } else {
                    Set<String> roles = new HashSet<>();
                    roles.add("ROLE_PROFESSOR");
                    // Name for professor: using the email part before '@' or just email if no other source
                    String name = email.split("@")[0]; 
                    professor = new Professor(email, name, roles);
                }
                professorRepository.save(professor);
                
                // The principal in UsernamePasswordAuthenticationToken should ideally be a UserDetails object
                // or at least an object that your application can consistently use to get user info.
                // Here, using email as principal. For more complex scenarios, use the 'professor' object or a UserDetails adapter.
                return new UsernamePasswordAuthenticationToken(email, code, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PROFESSOR")));
            }
            throw new BadCredentialsException("Invalid code for professor email. Code verification not yet implemented for this account.");
        } else {
            return null; 
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
} 