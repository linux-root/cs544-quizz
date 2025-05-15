package com.example.coolproject.web.dto;

import java.util.ArrayList;
import java.util.List;

public class QuestionFormData {
    
    private List<QuestionDto> questions = new ArrayList<>();
    
    public List<QuestionDto> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<QuestionDto> questions) {
        this.questions = questions;
    }
    
    public static class QuestionDto {
        private String questionText;
        private String modelAnswer;
        private int orderIndex;
        
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
        
        public int getOrderIndex() {
            return orderIndex;
        }
        
        public void setOrderIndex(int orderIndex) {
            this.orderIndex = orderIndex;
        }
    }
} 