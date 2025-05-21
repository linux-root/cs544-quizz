package com.example.coolproject.web.dto;

import java.util.List;

public class EvaluationForm {
    private List<EvaluationDetail> evaluations;

    public List<EvaluationDetail> getEvaluations() {
        return evaluations;
    }

    public void setEvaluations(List<EvaluationDetail> evaluations) {
        this.evaluations = evaluations;
    }

    public static class EvaluationDetail {
        private Long questionId;
        private Long answerId;
        private String comment;
        private Double score;

        public Long getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Long questionId) {
            this.questionId = questionId;
        }

        public Long getAnswerId() {
            return answerId;
        }

        public void setAnswerId(Long answerId) {
            this.answerId = answerId;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }
    }
} 