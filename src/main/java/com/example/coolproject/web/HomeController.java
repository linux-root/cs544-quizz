package com.example.coolproject.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.stream.Collectors;

@Controller
public class HomeController {

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model, Authentication authentication, @AuthenticationPrincipal OAuth2User oauth2User) {
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("username", authentication.getName());
            
            String roles = authentication.getAuthorities().stream()
                                 .map(GrantedAuthority::getAuthority)
                                 .collect(Collectors.joining(", "));
            model.addAttribute("roles", roles);
            model.addAttribute("isProfessor", authentication.getAuthorities().stream()
                .anyMatch(ga -> ga.getAuthority().equals("ROLE_PROFESSOR")));

            // Specific handling for OAuth2 user details like avatar
            if (oauth2User != null) {
                model.addAttribute("displayName", oauth2User.getAttribute("name")); // Or login, or preferred username
                model.addAttribute("avatarUrl", oauth2User.getAttribute("avatar_url"));
            } else {
                // For form login (professor), name is already set as authentication.getName()
                 model.addAttribute("displayName", authentication.getName());
            }
        }
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
} 