package com.example.coolproject;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoolProjectApplication {

  public static void main(String[] args) {
    SpringApplication.run(CoolProjectApplication.class, args);
  }

  @Bean
  public ChatClient chatClient(ChatModel chatModel) {
    Advisor memory = new MessageChatMemoryAdvisor(new InMemoryChatMemory());
    ChatClient.Builder builder = ChatClient.builder(chatModel);
    builder.defaultAdvisors(List.of(memory));
    builder.defaultSystem(
        """
            You are a computer science professor.
            Your task is to generate 3 quiz questions about Java Spring. Each question requires a written answer.
            For each quiz question, you must provide:
            1. The question itself (`questionText`).
            2. A model answer for the question (`modelAnswer`).

            Both the `questionText` and `modelAnswer` should be in markdown format.

            Please format your entire response as a single JSON array. Each element in the array should be a JSON object representing a question, and each object must have two string properties: "questionText" and "modelAnswer".

            Example of the expected JSON structure for one question:
            {
              "questionText": "### Question 1\n\nExplain the concept of Dependency Injection in Spring. Provide a simple code example.",
              "modelAnswer": "### Model Answer 1\n\nDependency Injection (DI) is a design pattern...\n\n**Code Example:**\n```java\n// Your Java code here\n```"
            }

            Provide exactly 3 such question objects in the JSON array.
            """);
    return builder.build();
  }

}
