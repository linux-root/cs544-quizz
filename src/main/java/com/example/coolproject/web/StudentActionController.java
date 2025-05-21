package com.example.coolproject.web;

import com.example.coolproject.dto.StudentActionDTO;
import com.example.coolproject.entity.StudentAction;
import com.example.coolproject.service.StudentActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/student-actions")
public class StudentActionController {

  private final StudentActionService studentActionService;

  @Autowired
  public StudentActionController(StudentActionService studentActionService) {
    this.studentActionService = studentActionService;
  }

  @PostMapping
  public ResponseEntity<?> createStudentAction(@RequestBody StudentActionDTO studentActionDTO, Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
    }

    String userEmail = null;
    Object principal = authentication.getPrincipal();

    if (principal instanceof UserDetails) {
      userEmail = ((UserDetails) principal).getUsername();
    } else if (principal instanceof OAuth2User) {
      OAuth2User oauth2User = (OAuth2User) principal;
      userEmail = oauth2User.getAttribute("email");
      if (userEmail == null || userEmail.isEmpty()) {
        userEmail = oauth2User.getName();
      }
    } else if (principal instanceof String) {
      userEmail = (String) principal;
    } else {
      System.err.println("Unknown principal type: " + principal.getClass().getName());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unknown principal type: " + principal.getClass().getName());
    }

    if (userEmail == null || userEmail.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not determine user identifier from authentication principal.");
    }

    try {
      StudentAction createdAction = studentActionService.createStudentAction(studentActionDTO, userEmail);
      URI location = URI.create("/api/v1/student-actions/" + createdAction.getId());
      return ResponseEntity.created(location).body(createdAction);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    } catch (RuntimeException e) {
      if (e.getMessage().contains("not found")) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
      }
      // Log the exception for unexpected RuntimeExceptions
      // logger.error("Error creating student action for user: {}", userEmail, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
    }
  }
}
