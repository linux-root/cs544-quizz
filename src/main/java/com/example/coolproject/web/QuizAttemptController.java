package com.example.coolproject.web;

import com.example.coolproject.entity.Student;
import com.example.coolproject.entity.QuizzSession;
import com.example.coolproject.entity.Quizz;
import com.example.coolproject.entity.Question;
import com.example.coolproject.repository.StudentRepository;
import com.example.coolproject.repository.QuizzSessionRepository;
import com.example.coolproject.service.QuizzService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/quiz") // Changed base request mapping for clarity
public class QuizAttemptController {

    private static final Logger logger = LoggerFactory.getLogger(QuizAttemptController.class);

    private final QuizzSessionRepository quizzSessionRepository;
    private final StudentRepository studentRepository;
    private final QuizzService quizzService;

    @Autowired
    public QuizAttemptController(QuizzSessionRepository quizzSessionRepository,
                                 StudentRepository studentRepository,
                                 QuizzService quizzService) {
        this.quizzSessionRepository = quizzSessionRepository;
        this.studentRepository = studentRepository;
        this.quizzService = quizzService;
    }

    @GetMapping("/session/{sessionId}/take") // New mapping for taking the quiz
    @PreAuthorize("isAuthenticated()")
    public String showQuizTakingPage(@PathVariable Long sessionId, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        String studentEmail = authentication.getName();
        Optional<Student> currentStudentOpt = studentRepository.findByEmail(studentEmail);

        if (!currentStudentOpt.isPresent()) {
            logger.warn("Attempt to take quiz session {} by non-existent student email {}", sessionId, studentEmail);
            redirectAttributes.addFlashAttribute("errorMessage", "Student profile not found.");
            return "redirect:/home";
        }

        Optional<QuizzSession> sessionOpt = quizzSessionRepository.findById(sessionId);
        if (!sessionOpt.isPresent()) {
            logger.warn("Quiz session with ID {} not found.", sessionId);
            redirectAttributes.addFlashAttribute("errorMessage", "Quiz session not found.");
            return "redirect:/home";
        }
        QuizzSession quizzSession = sessionOpt.get();
        
        if (quizzSession.getStatus() != QuizzSession.SessionStatus.OPEN) {
            logger.warn("Student {} attempting to take non-OPEN quiz session ID {}. Status: {}", studentEmail, sessionId, quizzSession.getStatus());
            redirectAttributes.addFlashAttribute("errorMessage", "This quiz is not currently open for taking.");
            return "redirect:/home";
        }

        Quizz quizz = quizzSession.getQuizz();
        List<Question> questions = quizzService.getQuizzQuestions(quizz.getId());

        model.addAttribute("quizzSessionId", quizzSession.getId());
        model.addAttribute("quizzTitle", quizz.getTitle());
        model.addAttribute("questions", questions);

        logger.info("Student {} viewing quiz session ID {} for quizz '{}'. {} questions loaded.", 
            studentEmail, sessionId, quizz.getTitle(), questions.size());

        return "quiz/take-quiz";
    }
} 