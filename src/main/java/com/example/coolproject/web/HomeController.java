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

            User userToDisplay;
            boolean isProfessor;
            String displayName;
            String rolesString;
            String avatarUrl = null;

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                userToDisplay = user;
                displayName = user.getName();
                rolesString = user.getRoles().stream().collect(Collectors.joining(", "));
                isProfessor = user.getRoles().contains("ROLE_PROFESSOR");
                if (user instanceof Student) {
                    Student student = (Student) user;
                    avatarUrl = student.getAvatarUrl();
                }
            } else {
                // User authenticated but not found in DB (should ideally not happen)
                // Fallback to basic auth info for model, but primarily for role check
                userToDisplay = null; // No full User entity available
                displayName = email;
                rolesString = authentication.getAuthorities().stream()
                                     .map(GrantedAuthority::getAuthority)
                                     .collect(Collectors.joining(", "));
                isProfessor = authentication.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals("ROLE_PROFESSOR"));
            }

            model.addAttribute("username", email); // email is the consistent username/principal name
            model.addAttribute("displayName", displayName);
            model.addAttribute("roles", rolesString);
            model.addAttribute("isProfessor", isProfessor); // Kept for consistency if any fragment still uses it
            if (avatarUrl != null) {
                 model.addAttribute("avatarUrl", avatarUrl);
            } else {
                 model.addAttribute("avatarUrl", null); // Ensure attribute exists
            }

            if (isProfessor) {
                return "professor-home";
            } else {
                // Assuming any other authenticated user is a student for now
                // or a generic authenticated user if not strictly student/professor roles are enforced elsewhere.
                // Based on README, primary roles are Professor and Student.
                return "student-home";
            }
        } else {
            // Not authenticated
            return "home"; // This will render the unauthenticated version of home.html
        }
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
} 