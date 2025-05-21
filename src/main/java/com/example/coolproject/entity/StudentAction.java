package com.example.coolproject.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class StudentAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentActionType actionType;

    @Column(nullable = false)
    private LocalDateTime startTimestamp;

    @Column(nullable = false)
    private LocalDateTime endTimestamp;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "quizz_session_id", nullable = false)
    private QuizzSession quizzSession;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = true)
    private Question question;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String actionValue;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StudentActionType getActionType() {
        return actionType;
    }

    public void setActionType(StudentActionType actionType) {
        this.actionType = actionType;
    }

    public LocalDateTime getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(LocalDateTime startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public LocalDateTime getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(LocalDateTime endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public QuizzSession getQuizzSession() {
        return quizzSession;
    }

    public void setQuizzSession(QuizzSession quizzSession) {
        this.quizzSession = quizzSession;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public String getActionValue() {
        return actionValue;
    }

    public void setActionValue(String actionValue) {
        this.actionValue = actionValue;
    }
} 