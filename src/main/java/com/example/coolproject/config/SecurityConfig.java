package com.example.coolproject.config;

import com.example.coolproject.entity.Student;
import com.example.coolproject.repository.StudentRepository;
import com.example.coolproject.security.CustomUserDetailsService;
import com.example.coolproject.security.ProfessorAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final ProfessorAuthenticationProvider professorAuthenticationProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final StudentRepository studentRepository;

    @Autowired
    public SecurityConfig(ProfessorAuthenticationProvider professorAuthenticationProvider,
                          CustomUserDetailsService customUserDetailsService,
                          StudentRepository studentRepository) {
        this.professorAuthenticationProvider = professorAuthenticationProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.studentRepository = studentRepository;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(customUserDetailsService);
        auth.authenticationProvider(professorAuthenticationProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                    .requestMatchers("/", "/login", "/login/professor", "/oauth2/**", "/error").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                    .anyRequest().authenticated()
            )
            .formLogin(formLogin ->
                formLogin
                    .loginPage("/login")
                    .loginProcessingUrl("/login/professor")
                    .usernameParameter("email")
                    .passwordParameter("code")
                    .defaultSuccessUrl("/home", true)
                    .failureUrl("/login?error=true")
            )
            .oauth2Login(oauth2Login ->
                oauth2Login
                    .loginPage("/login")
                    .successHandler(oauth2LoginSuccessHandler())
                    .failureUrl("/login?error=true")
            )
            .logout(logout ->
                logout
                    .logoutSuccessUrl("/").permitAll()
            )
            .exceptionHandling(exceptions -> 
                exceptions.accessDeniedPage("/error/403")
            );
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oauth2LoginSuccessHandler() {
        return (request, response, authentication) -> {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oauth2User.getAttributes();

            String principalNameForDbEmailField = oauth2User.getName();

            String displayNameFromGitHub = (String) attributes.get("name");
            if (displayNameFromGitHub == null || displayNameFromGitHub.isEmpty()) {
                displayNameFromGitHub = (String) attributes.get("login");
                if (displayNameFromGitHub == null || displayNameFromGitHub.isEmpty()) {
                    displayNameFromGitHub = principalNameForDbEmailField;
                }
            }
            
            String githubId = String.valueOf(attributes.get("id"));
            String avatarUrl = (String) attributes.get("avatar_url");

            Optional<Student> existingStudentOpt = studentRepository.findByGithubId(githubId);

            Student student;
            if (existingStudentOpt.isPresent()) {
                student = existingStudentOpt.get();
                student.setEmail(principalNameForDbEmailField);
                student.setName(displayNameFromGitHub);
                student.setAvatarUrl(avatarUrl);
            } else {
                Set<String> roles = new HashSet<>();
                roles.add("ROLE_STUDENT");
                student = new Student(principalNameForDbEmailField, displayNameFromGitHub, githubId, avatarUrl, roles);
            }
            studentRepository.save(student);

            response.sendRedirect("/home");
        };
    }
} 