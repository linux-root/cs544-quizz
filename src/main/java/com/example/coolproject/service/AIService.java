package com.example.coolproject.service;

import com.example.coolproject.entity.Question;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AIService {

    /**
     * Mock method to generate questions based on a quiz description.
     * In a real implementation, this would call an AI service.
     * 
     * @param description Description of the quiz
     * @return List of 5 generated questions
     */
    public List<Question> generateQuestions(String description) {
        List<Question> questions = new ArrayList<>();
        
        // Mock questions based on description
        String[] questionTexts = {
            "Explain the key concepts of " + description,
            "Compare and contrast the main approaches in " + description,
            "What are the historical developments in " + description + "?",
            "Analyze the impact of " + description + " on modern society",
            "Describe potential future developments in " + description
        };
        
        String[] modelAnswers = {
            "The key concepts of " + description + " include understanding the fundamental principles, theoretical frameworks, and practical applications...",
            "There are several approaches in " + description + ". The traditional approach focuses on..., while the modern approach emphasizes...",
            "The historical development of " + description + " began in the early period with basic concepts. Later, significant advancements were made...",
            "The impact of " + description + " on modern society is profound. It has influenced areas such as technology, culture, and economic systems...",
            "Future developments in " + description + " are likely to include technological advancements, new theoretical frameworks, and innovative applications..."
        };
        
        for (int i = 0; i < 5; i++) {
            Question question = new Question();
            question.setQuestionText(questionTexts[i]);
            question.setModelAnswer(modelAnswers[i]);
            question.setOrderIndex(i);
            questions.add(question);
        }
        
        return questions;
    }
} 