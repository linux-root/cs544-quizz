package com.example.coolproject.web.dto;

import java.time.LocalDateTime;

public class QuizzSessionViewDTO {
    private Long id;
    private String quizzDescription;
    private LocalDateTime scheduledStartTime; // Added for scheduled sessions
    // Add other fields from QuizzSession or Quizz that session-started.html might need

    // Constructor for started sessions
    public QuizzSessionViewDTO(Long id, String quizzDescription) {
        this.id = id;
        this.quizzDescription = quizzDescription;
    }

    // Constructor for scheduled sessions
    public QuizzSessionViewDTO(Long id, String quizzDescription, LocalDateTime scheduledStartTime) {
        this.id = id;
        this.quizzDescription = quizzDescription;
        this.scheduledStartTime = scheduledStartTime;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getQuizzDescription() {
        return quizzDescription;
    }

    public LocalDateTime getScheduledStartTime() {
        return scheduledStartTime;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setQuizzDescription(String quizzDescription) {
        this.quizzDescription = quizzDescription;
    }

    public void setScheduledStartTime(LocalDateTime scheduledStartTime) {
        this.scheduledStartTime = scheduledStartTime;
    }
} 