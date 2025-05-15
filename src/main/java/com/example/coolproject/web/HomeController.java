package com.example.coolproject.web;

import com.example.coolproject.entity.Student;
import com.example.coolproject.entity.User;
import com.example.coolproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final UserRepository userRepository;

    @Autowired
    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model, Authentication authentication) { // OAuth2User principal can be inferred if needed
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName(); // For both OAuth2 and form login, email is the name
            Optional<User> userOptional = userRepository.findByEmail(email);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                model.addAttribute("username", user.getEmail()); // or user.getName() if preferred for display
                model.addAttribute("displayName", user.getName());
                
                String roles = user.getRoles().stream().collect(Collectors.joining(", "));
                model.addAttribute("roles", roles);
                model.addAttribute("isProfessor", user.getRoles().contains("ROLE_PROFESSOR"));

                if (user instanceof Student) {
                    Student student = (Student) user;
                    model.addAttribute("avatarUrl", student.getAvatarUrl());
                } else {
                     model.addAttribute("avatarUrl", null); // Or a default professor avatar
                }

            } else {
                // User authenticated but not found in DB, which shouldn't happen with current setup
                // Fallback to basic auth info
                model.addAttribute("username", email);
                model.addAttribute("displayName", email);
                String authRoles = authentication.getAuthorities().stream()
                                     .map(GrantedAuthority::getAuthority)
                                     .collect(Collectors.joining(", "));
                model.addAttribute("roles", authRoles);
                model.addAttribute("isProfessor", authentication.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals("ROLE_PROFESSOR")));
                 model.addAttribute("avatarUrl", null);
            }
        }
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
} 