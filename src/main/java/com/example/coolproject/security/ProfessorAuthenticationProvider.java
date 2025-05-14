package com.example.coolproject.security;

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
import java.util.stream.Collectors;

@Component
public class ProfessorAuthenticationProvider implements AuthenticationProvider {

    private final List<String> professorEmails;

    public ProfessorAuthenticationProvider(@Value("${app.security.professor-emails}") String emails) {
        this.professorEmails = Arrays.stream(emails.split(","))
                                     .map(String::trim)
                                     .collect(Collectors.toList());
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String code = authentication.getCredentials().toString();

        if (professorEmails.contains(email)) {
            // Special "cheat code" for ping@w47s0n.com
            if ("ping@w47s0n.com".equalsIgnoreCase(email) && "1440".equals(code)) {
                return new UsernamePasswordAuthenticationToken(email, code, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PROFESSOR")));
            }
            // TODO: Implement actual code sending/verification for other professors
            // For now, other configured professor emails won't be able to log in without this logic
            throw new BadCredentialsException("Invalid code for professor email. Code verification not yet implemented for this account.");
        } else {
            // To avoid disclosing which emails are valid professor emails, 
            // throw BadCredentialsException or UsernameNotFoundException (though the latter might be too revealing).
            // For simplicity here, if email not in list, it's treated as bad credentials for this provider.
            // Spring Security will try other providers if available.
            return null; // Indicates this provider cannot authenticate this request
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
} 