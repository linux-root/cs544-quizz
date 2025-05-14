package com.example.coolproject.config;

import com.example.coolproject.security.ProfessorAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ProfessorAuthenticationProvider professorAuthenticationProvider;

    public SecurityConfig(ProfessorAuthenticationProvider professorAuthenticationProvider) {
        this.professorAuthenticationProvider = professorAuthenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(professorAuthenticationProvider) // Register custom provider
            .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                    .requestMatchers("/", "/login", "/login/professor", "/oauth2/**", "/error").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/images/**").permitAll() // Allow static resources
                    .anyRequest().authenticated()
            )
            .formLogin(formLogin ->
                formLogin
                    .loginPage("/login") // Custom login page
                    .loginProcessingUrl("/login/professor") // URL for professor form submission
                    .usernameParameter("email") // Corresponds to 'name="email"' in form
                    .passwordParameter("code")  // Corresponds to 'name="code"' in form
                    .defaultSuccessUrl("/home", true)
                    .failureUrl("/login?error=true") // Redirect on login failure
            )
            .oauth2Login(oauth2Login ->
                oauth2Login
                    .loginPage("/login") // Custom login page for OAuth2 too
                    .defaultSuccessUrl("/home", true)
                    .failureUrl("/login?error=true") // Redirect on OAuth failure
            )
            .logout(logout ->
                logout
                    .logoutSuccessUrl("/").permitAll()
            );
        return http.build();
    }
} 