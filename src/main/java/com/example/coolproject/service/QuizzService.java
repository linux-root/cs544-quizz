package com.example.coolproject.service;

import com.example.coolproject.entity.Professor;
import com.example.coolproject.entity.Question;
import com.example.coolproject.entity.Quizz;
import com.example.coolproject.entity.QuizzSession;
import com.example.coolproject.repository.QuestionRepository;
import com.example.coolproject.repository.QuizzRepository;
import com.example.coolproject.repository.QuizzSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuizzService {

  private static final Logger logger = LoggerFactory.getLogger(QuizzService.class);
  private final QuizzRepository quizzRepository;
  private final QuestionRepository questionRepository;
  private final QuizzSessionRepository quizzSessionRepository;
  private final AIService aiService;
  private final SimpMessagingTemplate messagingTemplate;

  private volatile boolean schedulerActive = false;

  @Autowired
  public QuizzService(
      QuizzRepository quizzRepository,
      QuestionRepository questionRepository,
      QuizzSessionRepository quizzSessionRepository,
      AIService aiService,
      SimpMessagingTemplate messagingTemplate) {
    this.quizzRepository = quizzRepository;
    this.questionRepository = questionRepository;
    this.quizzSessionRepository = quizzSessionRepository;
    this.aiService = aiService;
    this.messagingTemplate = messagingTemplate;
  }

  /**
   * Create a new quiz with AI-generated questions based on the description
   */
  @Transactional
  public Quizz createQuizzWithGeneratedQuestions(String title, String prompt, Professor professor) {
    logger.info("Creating quizz with title: '{}', prompt: '{}' for professor: {}", title, prompt, professor.getEmail());

    Quizz quizz = new Quizz();
    quizz.setTitle(title);
    quizz.setCreator(professor);

    logger.debug("Saving initial quizz");
    Quizz savedQuizz = quizzRepository.save(quizz);
    logger.info("Initial quizz saved with ID: {}", savedQuizz.getId());

    // Generate questions using AI
    logger.debug("Generating questions using AI service with prompt: {}", prompt);
    List<Question> questions = aiService.generateQuestions(prompt);
    logger.info("Generated {} questions", questions.size());

    // Associate questions with the quiz
    logger.debug("Associating questions with quizz");
    for (Question question : questions) {
      savedQuizz.addQuestion(question);
      logger.debug("Added question: {} with order: {}", question.getQuestionText(), question.getOrderIndex());
    }

    logger.debug("Saving quizz with questions");
    Quizz finalQuizz = quizzRepository.save(savedQuizz);
    logger.info("Quizz saved with ID: {} and {} questions", finalQuizz.getId(), finalQuizz.getQuestions().size());

    return finalQuizz;
  }

  /**
   * Create a new quiz with pre-defined questions
   */
  @Transactional
  public Quizz createQuizzWithQuestions(String title, Professor professor, List<Question> questions) {
    logger.info("Creating quizz with provided questions, title: {} for professor: {}", title,
        professor.getEmail());

    Quizz quizz = new Quizz();
    quizz.setTitle(title);
    quizz.setCreator(professor);

    logger.debug("Saving initial quizz");
    Quizz savedQuizz = quizzRepository.save(quizz);
    logger.info("Initial quizz saved with ID: {}", savedQuizz.getId());

    // Associate questions with the quiz
    logger.debug("Associating {} provided questions with quizz", questions.size());
    for (int i = 0; i < questions.size(); i++) {
      Question question = questions.get(i);
      question.setOrderIndex(i);
      savedQuizz.addQuestion(question);
      logger.debug("Added question at index {}: {}", i, question.getQuestionText());
    }

    logger.debug("Saving quizz with questions");
    Quizz finalQuizz = quizzRepository.save(savedQuizz);
    logger.info("Quizz saved with ID: {} and {} questions", finalQuizz.getId(), finalQuizz.getQuestions().size());

    return finalQuizz;
  }

  /**
   * Regenerate questions for an existing quiz
   */
  @Transactional
  public Quizz regenerateQuizzQuestions(Long quizzId, String newTitle, String newPrompt) {
    logger.info("Regenerating questions for quizz ID: {} with new title: '{}', new prompt: '{}'", quizzId, newTitle,
        newPrompt);

    Quizz quizz = quizzRepository.findById(quizzId)
        .orElseThrow(() -> {
          logger.error("Quizz not found with ID: {}", quizzId);
          return new IllegalArgumentException("Quizz not found with ID: " + quizzId);
        });

    // Update title if changed
    quizz.setTitle(newTitle);

    // Clear existing questions
    logger.debug("Clearing {} existing questions", quizz.getQuestions().size());
    quizz.getQuestions().clear();

    // Generate new questions
    logger.debug("Generating new questions using AI service with prompt: {}", newPrompt);
    List<Question> newQuestions = aiService.generateQuestions(newPrompt);
    logger.info("Generated {} new questions", newQuestions.size());

    // Associate new questions with the quiz
    logger.debug("Associating new questions with quizz");
    for (Question question : newQuestions) {
      quizz.addQuestion(question);
      logger.debug("Added question: {} with order: {}", question.getQuestionText(), question.getOrderIndex());
    }

    logger.debug("Saving quizz with regenerated questions");
    Quizz updatedQuizz = quizzRepository.save(quizz);
    logger.info("Updated quizz ID: {} with {} regenerated questions", updatedQuizz.getId(),
        updatedQuizz.getQuestions().size());

    return updatedQuizz;
  }

  /**
   * Update questions for a quiz
   */
  @Transactional
  public Quizz updateQuizzQuestions(Long quizzId, List<Question> updatedQuestions) {
    logger.info("Updating questions for quizz ID: {}", quizzId);

    Quizz quizz = quizzRepository.findById(quizzId)
        .orElseThrow(() -> {
          logger.error("Quizz not found with ID: {}", quizzId);
          return new IllegalArgumentException("Quizz not found with ID: " + quizzId);
        });

    // Clear existing questions
    logger.debug("Clearing {} existing questions", quizz.getQuestions().size());
    quizz.getQuestions().clear();

    // Add updated questions
    logger.debug("Adding {} updated questions", updatedQuestions.size());
    for (int i = 0; i < updatedQuestions.size(); i++) {
      Question question = updatedQuestions.get(i);
      question.setOrderIndex(i);
      quizz.addQuestion(question);
      logger.debug("Added updated question at index {}: {}", i, question.getQuestionText());
    }

    Quizz savedQuizz = quizzRepository.save(quizz);
    logger.info("Saved quizz with {} updated questions", savedQuizz.getQuestions().size());

    return savedQuizz;
  }

  /**
   * Start a quiz session immediately
   */
  @Transactional
  public QuizzSession startQuizzSession(Long quizzId) {
    Quizz quizz = quizzRepository.findById(quizzId)
        .orElseThrow(() -> {
          logger.error("Quizz not found with ID: {}", quizzId);
          return new IllegalArgumentException("Quizz not found with ID: " + quizzId);
        });

    if (quizz.getSession() != null) {
      throw new IllegalStateException("Quizz with ID: " + quizzId + " already has a session");
    }

    QuizzSession sessionEntity = new QuizzSession();
    sessionEntity.setQuizz(quizz);
    sessionEntity.setActualStartTime(LocalDateTime.now());
    sessionEntity.setStatus(QuizzSession.SessionStatus.OPEN);

    quizz.setSession(sessionEntity);
    Quizz savedQuizz = quizzRepository.save(quizz);
    return savedQuizz.getSession();
  }

  /**
   * Schedule a quiz session to start after X minutes
   */
  @Transactional
  public QuizzSession scheduleQuizzSession(Long quizzId, int minutesFromNow) {
    logger.info("Scheduling session for quizz ID: {} to start in {} minutes", quizzId, minutesFromNow);

    Quizz quizz = quizzRepository.findById(quizzId)
        .orElseThrow(() -> {
          logger.error("Quizz not found with ID: {}", quizzId);
          return new IllegalArgumentException("Quizz not found with ID: " + quizzId);
        });

    if (quizz.getSession() != null) {
      logger.error("Quizz with ID: {} already has a session", quizzId);
      throw new IllegalStateException("Quizz with ID: " + quizzId + " already has a session");
    }

    LocalDateTime scheduledTime = LocalDateTime.now().plusMinutes(minutesFromNow);

    QuizzSession sessionEntity = new QuizzSession();
    sessionEntity.setQuizz(quizz);
    sessionEntity.setScheduledStartTime(scheduledTime);
    sessionEntity.setStatus(QuizzSession.SessionStatus.SCHEDULED);

    quizz.setSession(sessionEntity);

    quizzRepository.save(quizz);
    this.schedulerActive = true;
    logger.info("Quiz scheduler activated due to new session scheduling.");
    return sessionEntity;
  }

  /**
   * Get all quizzes created by a professor
   */
  public List<Quizz> getQuizzesByProfessor(Professor professor) {
    logger.info("Getting quizzes for professor: {}", professor.getEmail());
    List<Quizz> quizzes = quizzRepository.findByCreator(professor);
    logger.info("Found {} quizzes", quizzes.size());
    return quizzes;
  }

  /**
   * Get a quiz by ID
   */
  public Quizz getQuizzById(Long id) {
    logger.info("Getting quizz by ID: {}", id);
    return quizzRepository.findById(id)
        .orElseThrow(() -> {
          logger.error("Quizz not found with ID: {}", id);
          return new IllegalArgumentException("Quizz not found with ID: " + id);
        });
  }

  /**
   * Get questions for a quiz
   */
  public List<Question> getQuizzQuestions(Long quizzId) {
    logger.info("Getting questions for quizz ID: {}", quizzId);
    Quizz quizz = quizzRepository.findById(quizzId)
        .orElseThrow(() -> {
          logger.error("Quizz not found with ID: {}", quizzId);
          return new IllegalArgumentException("Quizz not found with ID: " + quizzId);
        });
    List<Question> questions = questionRepository.findByQuizzOrderByOrderIndex(quizz);
    logger.info("Found {} questions for quizz ID: {}", questions.size(), quizzId);
    return questions;
  }

  /**
   * Get a session by ID
   */
  public QuizzSession getSessionById(Long sessionId) {
    logger.info("Getting session by ID: {}", sessionId);
    return quizzSessionRepository.findById(sessionId)
        .orElseThrow(() -> {
          logger.error("Session not found with ID: {}", sessionId);
          return new IllegalArgumentException("Session not found with ID: " + sessionId);
        });
  }

  @Scheduled(fixedRateString = "${quiz.scheduler.fixedRate:1000}")
  public void checkAndOpenScheduledQuizzSessions() {
      logger.info("Scheduler: Checking for due quiz sessions. Switch active: {}", this.schedulerActive);

      if (!this.schedulerActive) {
          logger.info("Scheduler: Switch is inactive, skipping check.");
          return;
      }

      List<QuizzSession> sessionsToOpen = quizzSessionRepository.findByStatusAndScheduledStartTimeLessThanEqual(
              QuizzSession.SessionStatus.SCHEDULED,
              LocalDateTime.now()
      );

      if (sessionsToOpen.isEmpty()) {
          logger.info("Scheduler: No scheduled sessions are due to be opened at this time.");
          return;
      }

      logger.info("Scheduler: Found {} session(s) to open.", sessionsToOpen.size());
      boolean sessionOpenedThisCycle = false;
      for (QuizzSession session : sessionsToOpen) {
          try {
              session.setStatus(QuizzSession.SessionStatus.OPEN);
              session.setActualStartTime(LocalDateTime.now()); 
              quizzSessionRepository.save(session);
              
              logger.info("Scheduler: Opened session ID: {}. New status: OPEN.", session.getId());
              sessionOpenedThisCycle = true;

              // Send WebSocket message
              String topic = "/topic/quizStatusUpdates";
              java.util.Map<String, Object> messagePayload = new java.util.HashMap<>();
              messagePayload.put("action", "sessionOpened");
              messagePayload.put("sessionId", session.getId());
              messagePayload.put("newStatus", session.getStatus().name());
              messagePayload.put("message", "Quizz session " + session.getId() + " is now OPEN. Please reload.");
              messagingTemplate.convertAndSend(topic, messagePayload);
              logger.info("Sent WebSocket message to {}: {}", topic, messagePayload);

          } catch (Exception e) {
              logger.error("Scheduler: Error opening session ID: {} or sending WebSocket message", session.getId(), e);
          }
      }

      if (sessionOpenedThisCycle) {
          logger.info("Scheduler: One or more sessions were opened. Deactivating scheduler switch.");
          this.schedulerActive = false;
      } else {
           logger.info("Scheduler: No sessions were actually opened in this cycle. Switch remains active: {}", this.schedulerActive);
      }
  }
}
