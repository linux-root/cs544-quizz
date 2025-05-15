package com.example.coolproject.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Quizz {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String description;
    
    @OneToMany(mappedBy = "quizz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();
    
    @OneToMany(mappedBy = "quizz", cascade = CascadeType.ALL)
    private List<QuizzSession> sessions = new ArrayList<>();
    
    @ManyToOne
    private Professor creator;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<Question> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
    
    public void addQuestion(Question question) {
        questions.add(question);
        question.setQuizz(this);
    }
    
    public Professor getCreator() {
        return creator;
    }
    
    public void setCreator(Professor creator) {
        this.creator = creator;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public List<QuizzSession> getSessions() {
        return sessions;
    }
    
    public void setSessions(List<QuizzSession> sessions) {
        this.sessions = sessions;
    }
} 