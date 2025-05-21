package com.example.coolproject.web;

import com.example.coolproject.entity.Student;
import com.example.coolproject.entity.User;
import com.example.coolproject.entity.QuizzSession;
import com.example.coolproject.repository.UserRepository;
import com.example.coolproject.service.QuizzService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import com.example.coolproject.security.ProfessorAuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class HomeController {

  private final UserRepository userRepository;
  private final QuizzService quizzService;
  private final ProfessorAuthenticationProvider professorAuthenticationProvider;
  private final SecurityContextRepository securityContextRepository;

  @Autowired
  public HomeController(UserRepository userRepository, QuizzService quizzService,
      ProfessorAuthenticationProvider professorAuthenticationProvider,
      SecurityContextRepository securityContextRepository) {
    this.userRepository = userRepository;
    this.quizzService = quizzService;
    this.professorAuthenticationProvider = professorAuthenticationProvider;
    this.securityContextRepository = securityContextRepository;
  }

  @GetMapping("/")
  public String root() {
    return "redirect:/home";
  }

  @GetMapping("/home")
  public String home(Model model, Authentication authentication) {
    // Spring Security ensures this path is only reached by authenticated users.
    // If authentication is null or not authenticated here, it's an unexpected state
    // or a direct call bypassing normal web flow, which Spring Security should
    // prevent.
    if (authentication == null || !authentication.isAuthenticated()) {
      // This case should ideally not be reached due to Spring Security config.
      // Redirecting to login or showing an error might be appropriate.
      // For now, let's log an error and redirect to login as a safeguard.
      // System.err.println("Error: /home accessed without authentication despite
      // SecurityConfig.");
      return "redirect:/login";
    }

    String email = authentication.getName();
    Optional<User> userOptional = userRepository.findByEmail(email);

    // boolean isProfessor; // Declared below
    String displayName;
    String rolesString;
    String avatarUrl = null;
    boolean isProfessor;
    User currentUser = null; // Variable to hold the current user

    if (userOptional.isPresent()) {
      User user = userOptional.get();
      currentUser = user; // Store the user
      displayName = user.getName();
      rolesString = user.getRoles().stream().collect(Collectors.joining(", "));
      isProfessor = user.getRoles().contains("ROLE_PROFESSOR");
      if (user instanceof Student) {
        Student student = (Student) user;
        avatarUrl = student.getAvatarUrl();
      }
    } else {
      // User authenticated (e.g., via OAuth2 initially) but not yet in DB or lookup
      // failed.
      // This case was handled before, relying on authentication object for roles.
      // This is a critical state if user is authenticated but not found.
      // For now, rely on authorities from Authentication object if user not in DB.
      displayName = email; // Fallback display name
      rolesString = authentication.getAuthorities().stream()
          .map(GrantedAuthority::getAuthority)
          .collect(Collectors.joining(", "));
      isProfessor = authentication.getAuthorities().stream()
          .anyMatch(ga -> ga.getAuthority().equals("ROLE_PROFESSOR"));
      // Log this situation, as it's unusual post-login
      System.err
          .println("Authenticated user " + email + " not found in database. Roles derived from security context.");
    }

    model.addAttribute("username", email);
    model.addAttribute("displayName", displayName);
    model.addAttribute("roles", rolesString);
    // model.addAttribute("isProfessor", isProfessor); // isProfessor variable used
    // directly for routing
    if (avatarUrl != null) {
      model.addAttribute("avatarUrl", avatarUrl);
    } else {
      model.addAttribute("avatarUrl", null);
    }

    if (isProfessor) {
      return "professor-home";
    } else {
      // For Students (or any non-professor authenticated user)
      // Check for any open quiz sessions
      // The check 'currentUser instanceof Student' ensures we only do this for actual
      // students,
      // although current logic routes any non-professor here.
      if (currentUser instanceof Student) {
        Optional<QuizzSession> openSessionOpt = quizzService.findAnyOpenQuizSession();
        if (openSessionOpt.isPresent()) {
          model.addAttribute("hasOpenQuizSession", true);
          model.addAttribute("openQuizSessionId", openSessionOpt.get().getId());
          model.addAttribute("openQuizSessionTitle", openSessionOpt.get().getQuizz().getTitle());
        } else {
          model.addAttribute("hasOpenQuizSession", false);
        }
      } else {
        model.addAttribute("hasOpenQuizSession", false); // Default for non-students in this branch
      }
      return "student-home";
    }
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/verify-professor")
  public String verifyProfessorPage(HttpSession session, Model model) {
    String email = (String) session.getAttribute("professorEmailForVerification");
    if (email == null) {
      // If email is not in session, perhaps the user navigated here directly
      // or session expired. Redirect to initial login step.
      return "redirect:/login";
    }
    // Optionally add email to model if needed by the template directly (already
    // available via session object)
    // model.addAttribute("professorEmail", email);
    return "verify-professor";
  }

  @GetMapping("/error/403")
  public String accessDenied() {
    return "error/403";
  }

  @PostMapping("/login/professor/initiate")
  public String initiateProfessorLogin(@RequestParam String email, HttpSession session,
      RedirectAttributes redirectAttributes) {
    try {
      String code = professorAuthenticationProvider.generateAndStoreVerificationCode(email);
      session.setAttribute("professorEmailForVerification", email);
      redirectAttributes.addFlashAttribute("successMessage",
          "A verification code has been sent to your console (simulating email).");
      return "redirect:/verify-professor";
    } catch (BadCredentialsException e) {
      redirectAttributes.addFlashAttribute("error", "true"); // Generic error on login page
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage()); // More specific message if login page can
                                                                            // show it
      return "redirect:/login?error=true&message=InvalidProfessorEmail"; // Or a more specific error parameter
    }
  }

  @PostMapping("/login/professor/verify")
  public String verifyProfessorCode(@RequestParam String code,
      HttpSession session,
      RedirectAttributes redirectAttributes,
      HttpServletRequest request,
      HttpServletResponse response) {
    String email = (String) session.getAttribute("professorEmailForVerification");
    if (email == null) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "Session expired or invalid state. Please try logging in again.");
      return "redirect:/login?error=true";
    }

    try {
      Authentication authentication = professorAuthenticationProvider.verifyCodeAndBuildAuthentication(email, code);
      if (authentication != null && authentication.isAuthenticated()) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

        session.removeAttribute("professorEmailForVerification");
        return "redirect:/home";
      } else {
        redirectAttributes.addFlashAttribute("error", "true");
        return "redirect:/verify-professor?error=true";
      }
    } catch (BadCredentialsException e) {
      redirectAttributes.addFlashAttribute("error", "true");
      return "redirect:/verify-professor?error=true";
    }
  }
}
