package com.example.coolproject.entity;

import jakarta.persistence.*;

@Entity
public class Question {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 1000)
    private String questionText;
    
    @Column(length = 2000)
    private String modelAnswer;
    
    @ManyToOne
    private Quizz quizz;
    
    private int orderIndex;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getQuestionText() {
        return questionText;
    }
    
    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }
    
    public String getModelAnswer() {
        return modelAnswer;
    }
    
    public void setModelAnswer(String modelAnswer) {
        this.modelAnswer = modelAnswer;
    }
    
    public Quizz getQuizz() {
        return quizz;
    }
    
    public void setQuizz(Quizz quizz) {
        this.quizz = quizz;
    }
    
    public int getOrderIndex() {
        return orderIndex;
    }
    
    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
} 