package com.example.coolproject.web.dto;

import java.time.LocalDateTime;

public class QuizzSessionViewDTO {
    private Long id;
    private String quizzTitle;
    private LocalDateTime scheduledStartTime; // Added for scheduled sessions
    // Add other fields from QuizzSession or Quizz that session-started.html might need

    // Constructor for started sessions
    public QuizzSessionViewDTO(Long id, String quizzTitle) {
        this.id = id;
        this.quizzTitle = quizzTitle;
    }

    // Constructor for scheduled sessions
    public QuizzSessionViewDTO(Long id, String quizzTitle, LocalDateTime scheduledStartTime) {
        this.id = id;
        this.quizzTitle = quizzTitle;
        this.scheduledStartTime = scheduledStartTime;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getQuizzTitle() {
        return quizzTitle;
    }

    public LocalDateTime getScheduledStartTime() {
        return scheduledStartTime;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setQuizzTitle(String quizzTitle) {
        this.quizzTitle = quizzTitle;
    }

    public void setScheduledStartTime(LocalDateTime scheduledStartTime) {
        this.scheduledStartTime = scheduledStartTime;
    }
} 