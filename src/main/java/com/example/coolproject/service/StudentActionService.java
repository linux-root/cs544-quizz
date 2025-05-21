package com.example.coolproject.service;

import com.example.coolproject.dto.StudentActionDTO;
import com.example.coolproject.entity.*;
import com.example.coolproject.repository.QuestionRepository;
import com.example.coolproject.repository.QuizzSessionRepository;
import com.example.coolproject.repository.StudentActionRepository;
import com.example.coolproject.repository.StudentRepository;
import com.example.coolproject.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class StudentActionService {

  private static final Logger logger = LoggerFactory.getLogger(StudentActionService.class);
  private final StudentActionRepository studentActionRepository;
  private final QuizzSessionRepository quizzSessionRepository;
  private final QuestionRepository questionRepository;
  private final UserRepository userRepository;
  private final SimpMessagingTemplate messagingTemplate;

  @Autowired
  public StudentActionService(StudentActionRepository studentActionRepository,
      StudentRepository studentRepository,
      QuizzSessionRepository quizzSessionRepository,
      QuestionRepository questionRepository,
      UserRepository userRepository,
      SimpMessagingTemplate messagingTemplate,
      ObjectMapper objectMapper) {
    this.studentActionRepository = studentActionRepository;
    this.quizzSessionRepository = quizzSessionRepository;
    this.questionRepository = questionRepository;
    this.userRepository = userRepository;
    this.messagingTemplate = messagingTemplate;
  }

  @Transactional
  public StudentAction createStudentAction(StudentActionDTO dto, String userEmail) {
    User user = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

    if (!(user instanceof Student)) {
      throw new IllegalArgumentException("User with email " + userEmail + " is not a Student.");
    }
    Student student = (Student) user;

    QuizzSession quizzSession = quizzSessionRepository.findById(dto.getSessionId())
        .orElseThrow(() -> new RuntimeException("QuizzSession not found with id: " + dto.getSessionId()));

    Question question = null;
    if (dto.getQuestionId() != null) {
      question = questionRepository.findById(dto.getQuestionId())
          .orElseThrow(() -> new RuntimeException("Question not found with id: " + dto.getQuestionId()));
    }

    StudentAction studentAction = new StudentAction();
    studentAction.setActionType(dto.getActionType());
    studentAction.setStartTimestamp(dto.getStartTimestamp() != null ? dto.getStartTimestamp() : LocalDateTime.now());
    studentAction.setEndTimestamp(dto.getEndTimestamp() != null ? dto.getEndTimestamp() : LocalDateTime.now());

    studentAction.setStudent(student);
    studentAction.setQuizzSession(quizzSession);
    studentAction.setQuestion(question);
    studentAction.setActionValue(dto.getActionValue());

    StudentAction savedAction = studentActionRepository.save(studentAction);

    // Send WebSocket message
    String topic = String.format("/topic/session/%d/studentAction", quizzSession.getId());
    Map<String, Object> messagePayload = new HashMap<>();
    messagePayload.put("actionType", savedAction.getActionType().name());
    messagePayload.put("studentId", savedAction.getStudent().getId());
    messagePayload.put("sessionId", savedAction.getQuizzSession().getId());
    messagePayload.put("timestamp", savedAction.getEndTimestamp().toString());
    if (savedAction.getQuestion() != null) {
      messagePayload.put("questionId", savedAction.getQuestion().getId());
    }
    messagePayload.put("actionValue", savedAction.getActionValue());

    try {
      messagingTemplate.convertAndSend(topic, messagePayload);
      logger.info("Sent WebSocket message to {}: New student action {} by student {}", topic,
          savedAction.getActionType(), student.getId());
    } catch (Exception e) {
      logger.error("Error sending WebSocket message for student action to topic {}: {}", topic, e.getMessage(), e);
    }

    return savedAction;
  }
}
