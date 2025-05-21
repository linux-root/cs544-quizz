package com.example.coolproject.web;

import com.example.coolproject.entity.Student;
import com.example.coolproject.entity.QuizzSession;
import com.example.coolproject.entity.Quizz;
import com.example.coolproject.entity.Question;
import com.example.coolproject.entity.Answer;
import com.example.coolproject.repository.StudentRepository;
import com.example.coolproject.repository.QuizzSessionRepository;
import com.example.coolproject.repository.AnswerRepository;
import com.example.coolproject.repository.QuestionRepository;
import com.example.coolproject.service.QuizzService;
import com.example.coolproject.service.StudentActionService;
import com.example.coolproject.dto.StudentActionDTO;
import com.example.coolproject.entity.StudentActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@Controller
@RequestMapping("/student") // Changed base request mapping for clarity
public class QuizAttemptController {

    private static final Logger logger = LoggerFactory.getLogger(QuizAttemptController.class);

    private final QuizzSessionRepository quizzSessionRepository;
    private final StudentRepository studentRepository;
    private final QuizzService quizzService;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final StudentActionService studentActionService;

    @Autowired
    public QuizAttemptController(QuizzSessionRepository quizzSessionRepository,
                                 StudentRepository studentRepository,
                                 QuizzService quizzService,
                                 AnswerRepository answerRepository,
                                 QuestionRepository questionRepository,
                                 StudentActionService studentActionService) {
        this.quizzSessionRepository = quizzSessionRepository;
        this.studentRepository = studentRepository;
        this.quizzService = quizzService;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.studentActionService = studentActionService;
    }

    @GetMapping("/session/{sessionId}/take") // New mapping for taking the quiz
    @PreAuthorize("isAuthenticated()")
    public String showQuizTakingPage(@PathVariable Long sessionId, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        String studentEmail = authentication.getName();
        logger.debug("[TakeQuizPage] Authenticated student email: {}", studentEmail); // Log student email
        Optional<Student> currentStudentOpt = studentRepository.findByEmail(studentEmail);

        if (!currentStudentOpt.isPresent()) {
            logger.warn("[TakeQuizPage] Student profile not found for email: {}. Redirecting to home.", studentEmail);
            redirectAttributes.addFlashAttribute("errorMessage", "Student profile not found.");
            return "redirect:/home";
        }
        Student currentStudent = currentStudentOpt.get();
        logger.debug("[TakeQuizPage] Current student ID: {}, Email: {}", currentStudent.getId(), currentStudent.getEmail()); // Log student details

        Optional<QuizzSession> sessionOpt = quizzSessionRepository.findById(sessionId);
        if (!sessionOpt.isPresent()) {
            logger.warn("[TakeQuizPage] Quiz session with ID {} not found. Redirecting to home.", sessionId);
            redirectAttributes.addFlashAttribute("errorMessage", "Quiz session not found.");
            return "redirect:/home";
        }
        QuizzSession quizzSession = sessionOpt.get();
        logger.debug("[TakeQuizPage] Current QuizzSession ID: {}", quizzSession.getId()); // Log session ID
        
        // Add student to participants list if session is OPEN or SCHEDULED
        if (quizzSession.getStatus() == QuizzSession.SessionStatus.OPEN || quizzSession.getStatus() == QuizzSession.SessionStatus.SCHEDULED) {
            boolean added = quizzSession.getParticipants().add(currentStudent);
            if (added) {
                quizzSessionRepository.save(quizzSession);
                logger.info("[TakeQuizPage] Student {} added to participants for session ID {}. Total participants: {}", currentStudent.getEmail(), sessionId, quizzSession.getParticipants().size());
                // Log the JOIN_SESSION action
                try {
                    StudentActionDTO actionDTO = new StudentActionDTO();
                    actionDTO.setSessionId(sessionId);
                    actionDTO.setActionType(StudentActionType.JOIN_SESSION);
                    // studentActionService.createStudentAction will set timestamps to now()
                    studentActionService.createStudentAction(actionDTO, currentStudent.getEmail());
                    logger.info("[TakeQuizPage] Logged JOIN_SESSION action for student {} in session ID {}.", currentStudent.getEmail(), sessionId);
                } catch (Exception e) {
                    logger.error("[TakeQuizPage] Failed to log JOIN_SESSION action for student {} in session ID {}: {}", currentStudent.getEmail(), sessionId, e.getMessage());
                }
            } else {
                logger.info("[TakeQuizPage] Student {} was already a participant in session ID {}. Total participants: {}", currentStudent.getEmail(), sessionId, quizzSession.getParticipants().size());
            }
        } else {
            // This case is largely handled by the status check below, but included for completeness 
            // if the logic for allowed statuses to join changes in the future.
            logger.warn("[TakeQuizPage] Student {} attempting to join session ID {} with status {}. Not adding to participants.", 
                        currentStudent.getEmail(), sessionId, quizzSession.getStatus());
        }
        
        if (quizzSession.getStatus() != QuizzSession.SessionStatus.OPEN && quizzSession.getStatus() != QuizzSession.SessionStatus.SCHEDULED) { 
             if (quizzSession.getStatus() != QuizzSession.SessionStatus.OPEN) { 
                logger.warn("[TakeQuizPage] Student {} attempting to take non-OPEN quiz session ID {}. Status: {}. Redirecting to home.", studentEmail, sessionId, quizzSession.getStatus());
                redirectAttributes.addFlashAttribute("errorMessage", "This quiz is not currently open for taking.");
                return "redirect:/home";
            }
        }

        Quizz quizz = quizzSession.getQuizz();
        List<Question> questions = quizzService.getQuizzQuestions(quizz.getId());
        logger.debug("[TakeQuizPage] Loaded {} questions for Quizz ID {}", questions.size(), quizz.getId());
        
        Map<Long, String> existingAnswersMap = new HashMap<>();
        boolean isResubmission = false;
        
        // Always try to fetch answers for the current student and session.
        // The Answer entity itself links to Student and QuizzSession.
        // The QuizzSession.participants collection might be for a different purpose (e.g., enrollment list).
        logger.debug("[TakeQuizPage] Fetching existing answers for Student ID {} and Session ID {}.", currentStudent.getId(), quizzSession.getId());
        List<Answer> existingAnswersList = answerRepository.findByQuizzSessionAndStudent(quizzSession, currentStudent);
        logger.debug("[TakeQuizPage] Found {} existing answers.", existingAnswersList == null ? 0 : existingAnswersList.size());
            
        if (existingAnswersList != null && !existingAnswersList.isEmpty()) {
            isResubmission = true;
            for (Answer ans : existingAnswersList) {
                if (ans.getQuestion() != null && ans.getQuestion().getId() != null) { 
                    existingAnswersMap.put(ans.getQuestion().getId(), ans.getTextAnswer());
                    logger.debug("[TakeQuizPage] Added to map: QID {} -> Answer Text'{}'", ans.getQuestion().getId(), ans.getTextAnswer());
                } else {
                    logger.warn("[TakeQuizPage] Found an answer with null question or question ID for Answer ID: {}", ans.getId());
                }
            }
        } else {
            logger.debug("[TakeQuizPage] No existing answers found for Student ID {} in Session ID {}. This might be their first attempt or view.", currentStudent.getId(), quizzSession.getId());
        }
        logger.debug("[TakeQuizPage] Final existingAnswersMap: {}", existingAnswersMap);
        logger.debug("[TakeQuizPage] Final isResubmission flag: {}", isResubmission);

        model.addAttribute("quizzSessionId", quizzSession.getId());
        model.addAttribute("quizzTitle", quizz.getTitle());
        model.addAttribute("questions", questions);
        model.addAttribute("durationMinutes", quizz.getDurationMinutes());
        model.addAttribute("existingAnswers", existingAnswersMap); 
        model.addAttribute("isResubmission", isResubmission);
        model.addAttribute("isSessionOpen", quizzSession.getStatus() == QuizzSession.SessionStatus.OPEN);

        logger.info("[TakeQuizPage] Student {} viewing quiz session ID {} for quizz '{}'. {} questions. Resubmission: {}. Session Open: {}. Answers in model: {}", 
            studentEmail, sessionId, quizz.getTitle(), questions.size(), 
            model.getAttribute("isResubmission"), model.getAttribute("isSessionOpen"), model.getAttribute("existingAnswers"));
 
        return "student/take-quizz";
    }

    @PostMapping("/session/{sessionId}/submit")
    @PreAuthorize("isAuthenticated()")
    public String handleSubmitQuiz(@PathVariable Long sessionId,
                                   @RequestParam Map<String, String> answersMap, // Renamed for clarity
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        String studentEmail = authentication.getName();
        Optional<Student> currentStudentOpt = studentRepository.findByEmail(studentEmail);

        if (!currentStudentOpt.isPresent()) {
            logger.warn("Attempt to submit quiz for session {} by non-existent student email {}", sessionId, studentEmail);
            redirectAttributes.addFlashAttribute("errorMessage", "Student profile not found. Cannot submit answers.");
            return "redirect:/home";
        }
        Student currentStudent = currentStudentOpt.get();

        Optional<QuizzSession> sessionOpt = quizzSessionRepository.findById(sessionId);
        if (!sessionOpt.isPresent()) {
            logger.warn("Quiz session with ID {} not found during submission.", sessionId);
            redirectAttributes.addFlashAttribute("errorMessage", "Quiz session not found. Cannot submit answers.");
            return "redirect:/home"; 
        }
        QuizzSession quizzSession = sessionOpt.get();

        if (quizzSession.getStatus() != QuizzSession.SessionStatus.OPEN) {
            logger.warn("Student {} attempting to submit to non-OPEN quiz session ID {}. Status: {}", studentEmail, sessionId, quizzSession.getStatus());
            redirectAttributes.addFlashAttribute("errorMessage", "This quiz is not currently open for submission.");
            // Redirect back to the take quiz page, which should now handle showing existing answers
            return "redirect:/student/session/" + sessionId + "/take"; 
        }

        logger.info("Processing submission for quiz session ID {} by student {}", sessionId, studentEmail);
        boolean isFirstSubmission = answerRepository.findByQuizzSessionAndStudent(quizzSession, currentStudent).isEmpty();

        for (Map.Entry<String, String> entry : answersMap.entrySet()) {
            String key = entry.getKey();
            String submittedTextAnswer = entry.getValue();

            if (key.startsWith("answers[")) {
                try {
                    String questionIdStr = key.substring(key.indexOf('[') + 1, key.indexOf(']'));
                    Long questionId = Long.parseLong(questionIdStr);
                    
                    Optional<Question> questionOpt = questionRepository.findById(questionId);
                    if (questionOpt.isPresent()) {
                        Question question = questionOpt.get();
                        
                        // Check if an answer already exists for this question, student, and session
                        Optional<Answer> existingAnswerOpt = answerRepository.findByQuizzSessionAndStudentAndQuestion(quizzSession, currentStudent, question);
                        
                        Answer answerToSave;
                        if (existingAnswerOpt.isPresent()) {
                            answerToSave = existingAnswerOpt.get();
                            logger.debug("Updating existing answer for question ID {} in session {} by student {}", questionId, sessionId, studentEmail);
                        } else {
                            answerToSave = new Answer();
                            answerToSave.setQuizzSession(quizzSession);
                            answerToSave.setStudent(currentStudent);
                            answerToSave.setQuestion(question);
                            logger.debug("Creating new answer for question ID {} in session {} by student {}", questionId, sessionId, studentEmail);
                        }
                        
                        answerToSave.setTextAnswer(submittedTextAnswer);
                        // Reset score and comment upon new submission/update, as they need re-evaluation
                        answerToSave.setScore(null); 
                        answerToSave.setComment(null);
                        
                        answerRepository.save(answerToSave);

                    } else {
                        logger.warn("Question with ID {} not found for quiz session {}. Answer not saved.", questionId, sessionId);
                    }
                } catch (Exception e) {
                    logger.error("Error processing answer key '{}' for session {}: {}", key, sessionId, e.getMessage());
                }
            }
        }
        
        // No need to update QuizzSession status here, as multiple submissions are allowed while OPEN.
        // Status update to CLOSED/GRADED would be a separate process.

        if (isFirstSubmission) {
            redirectAttributes.addFlashAttribute("successMessage", "Quiz submitted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Quiz answers updated successfully!");
        }
        // Redirect back to the take quiz page to show updated answers and button text
        return "redirect:/student/session/" + sessionId + "/take"; 
    }
} 