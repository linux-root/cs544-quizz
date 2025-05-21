package com.example.coolproject.service;

import com.example.coolproject.entity.Question;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class QSmartGenService {

  private final ChatClient chatClient;

  public QSmartGenService(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  /**
   * Generates a list of quiz questions on a given topic, suitable for a specific
   * duration.
   *
   * @param topic             The topic for the quiz questions (e.g., "Spring Boot
   *                          Fundamentals", "Spring Data JPA").
   * @param durationInMinutes The approximate duration the quiz is intended for,
   *                          in minutes.
   * @return A list of Question objects.
   */
  public List<Question> generateQuestions(String prompt, int durationInMinutes) {
    System.out.printf("Requesting quiz questions from AI for topic: %s (duration: %d minutes)...%n", prompt,
        durationInMinutes);

    String userMessage = String.format(
        "Generate 3 quiz questions specifically about '%s'. " +
            "The quiz is intended to be completed in approximately %d minutes. " +
            "Ensure the questions and model answers are appropriate for this timeframe and topic, adhering to the established JSON output format.",
        prompt,
        durationInMinutes);

    List<Question> quizQuestions = chatClient.prompt()
        .user(userMessage)
        .call()
        .entity(new ParameterizedTypeReference<List<Question>>() {
        });

    if (quizQuestions != null && !quizQuestions.isEmpty()) {
      System.out.printf("Successfully generated %d questions for topic: %s%n", quizQuestions.size(), prompt);
    } else {
      System.out.printf("No quiz questions were generated for topic: %s, or the response was empty.%n", prompt);
    }
    return quizQuestions;
  }
}
