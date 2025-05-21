package com.example.coolproject.dto;

import com.example.coolproject.entity.StudentActionType;
import java.time.LocalDateTime;

public class StudentActionDTO {

    private StudentActionType actionType;
    private LocalDateTime startTimestamp;
    private LocalDateTime endTimestamp;
    private Long sessionId;
    private Long questionId;
    private String actionValue;

    // Getters and Setters
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

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getActionValue() {
        return actionValue;
    }

    public void setActionValue(String actionValue) {
        this.actionValue = actionValue;
    }

    // Consider adding a constructor, toString, equals, and hashCode methods as needed.
} 