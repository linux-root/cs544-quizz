package com.example.coolproject.service;

import com.example.coolproject.entity.Professor;
import com.example.coolproject.entity.Question;
import com.example.coolproject.entity.Quizz;
import com.example.coolproject.entity.QuizzSession;
import com.example.coolproject.repository.QuestionRepository;
import com.example.coolproject.repository.QuizzRepository;
import com.example.coolproject.repository.QuizzSessionRepository;
import com.example.coolproject.web.dto.QuizzSessionViewDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired
  public QuizzService(
      QuizzRepository quizzRepository,
      QuestionRepository questionRepository,
      QuizzSessionRepository quizzSessionRepository,
      AIService aiService) {
    this.quizzRepository = quizzRepository;
    this.questionRepository = questionRepository;
    this.quizzSessionRepository = quizzSessionRepository;
    this.aiService = aiService;
  }

  /**
   * Create a new quiz with AI-generated questions based on the description
   */
  @Transactional
  public Quizz createQuizzWithGeneratedQuestions(String description, Professor professor) {
    logger.info("Creating quizz with description: {} for professor: {}", description, professor.getEmail());

    Quizz quizz = new Quizz();
    quizz.setDescription(description);
    quizz.setCreator(professor);

    logger.debug("Saving initial quizz");
    Quizz savedQuizz = quizzRepository.save(quizz);
    logger.info("Initial quizz saved with ID: {}", savedQuizz.getId());

    // Generate questions using AI
    logger.debug("Generating questions using AI service");
    List<Question> questions = aiService.generateQuestions(description);
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
  public Quizz createQuizzWithQuestions(String description, Professor professor, List<Question> questions) {
    logger.info("Creating quizz with provided questions, description: {} for professor: {}", description,
        professor.getEmail());

    Quizz quizz = new Quizz();
    quizz.setDescription(description);
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
  public Quizz regenerateQuizzQuestions(Long quizzId, String newDescription) {
    logger.info("Regenerating questions for quizz ID: {} with new description: {}", quizzId, newDescription);

    Quizz quizz = quizzRepository.findById(quizzId)
        .orElseThrow(() -> {
          logger.error("Quizz not found with ID: {}", quizzId);
          return new IllegalArgumentException("Quizz not found with ID: " + quizzId);
        });

    // Update description if changed
    quizz.setDescription(newDescription);

    // Clear existing questions
    logger.debug("Clearing {} existing questions", quizz.getQuestions().size());
    quizz.getQuestions().clear();

    // Generate new questions
    logger.debug("Generating new questions using AI service");
    List<Question> newQuestions = aiService.generateQuestions(newDescription);
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
  public QuizzSessionViewDTO startQuizzSession(Long quizzId) {
    logger.info("Starting session for quizz ID: {}", quizzId);
    
    Quizz quizz = quizzRepository.findById(quizzId)
            .orElseThrow(() -> {
                logger.error("Quizz not found with ID: {}", quizzId);
                return new IllegalArgumentException("Quizz not found with ID: " + quizzId);
            });
    
    if (quizz.getSession() != null) {
        logger.error("Quizz with ID: {} already has a session", quizzId);
        throw new IllegalStateException("Quizz with ID: " + quizzId + " already has a session");
    }
    
    QuizzSession sessionEntity = new QuizzSession();
    sessionEntity.setQuizz(quizz); 
    sessionEntity.setActualStartTime(LocalDateTime.now());
    sessionEntity.setStatus(QuizzSession.SessionStatus.OPEN);
    
    quizz.setSession(sessionEntity); 
    
    Quizz updatedQuizz = quizzRepository.save(quizz);
    
    QuizzSession persistedSessionEntity = updatedQuizz.getSession();
    if (persistedSessionEntity == null || persistedSessionEntity.getId() == null) {
        logger.error("Session was not persisted correctly via cascade for quizz ID: {}", quizzId);
        throw new IllegalStateException("Session not persisted via cascade for quizz ID: " + quizzId);
    }
    logger.info("Session entity started with ID: {} for quizz ID: {}", persistedSessionEntity.getId(), quizzId);
    
    String quizzDescription;
    if (updatedQuizz.getDescription() != null) {
        quizzDescription = updatedQuizz.getDescription();
        logger.debug("Quizz description for session {} (from Quizz ID {}): {}", 
                     persistedSessionEntity.getId(), updatedQuizz.getId(), quizzDescription);
    } else {
        quizzDescription = "Error: Quizz description missing"; // Fallback or specific error
        logger.error("Quizz (ID: {}) has a null description. Session ID: {}", 
                     updatedQuizz.getId(), persistedSessionEntity.getId());
        // Consider if this state warrants an exception if Quizz description is mandatory
    }
    
    return new QuizzSessionViewDTO(persistedSessionEntity.getId(), quizzDescription);
  }

  /**
   * Schedule a quiz session to start after X minutes
   */
  @Transactional
  public QuizzSessionViewDTO scheduleQuizzSession(Long quizzId, int minutesFromNow) {
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
    
    Quizz updatedQuizz = quizzRepository.save(quizz);
    
    QuizzSession persistedSessionEntity = updatedQuizz.getSession();
    if (persistedSessionEntity == null || persistedSessionEntity.getId() == null) {
        logger.error("Session was not persisted correctly via cascade for quizz ID: {}", quizzId);
        throw new IllegalStateException("Session not persisted via cascade for quizz ID: " + quizzId);
    }
    logger.info("Session entity scheduled with ID: {} for quizz ID: {} at {}", persistedSessionEntity.getId(), quizzId, persistedSessionEntity.getScheduledStartTime());
    
    String quizzDescription;
    if (updatedQuizz.getDescription() != null) {
        quizzDescription = updatedQuizz.getDescription();
        logger.debug("Quizz description for scheduled session {} (from Quizz ID {}): {}", 
                     persistedSessionEntity.getId(), updatedQuizz.getId(), quizzDescription);
    } else {
        quizzDescription = "Error: Quizz description missing"; // Fallback or specific error
        logger.error("Quizz (ID: {}) has a null description. Scheduled session ID: {}", 
                     updatedQuizz.getId(), persistedSessionEntity.getId());
        // Consider if this state warrants an exception
    }

    return new QuizzSessionViewDTO(persistedSessionEntity.getId(), quizzDescription, persistedSessionEntity.getScheduledStartTime());
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
}
