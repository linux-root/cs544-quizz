package com.example.coolproject.web;

import com.example.coolproject.entity.Professor;
import com.example.coolproject.entity.Question;
import com.example.coolproject.entity.Quizz;
import com.example.coolproject.entity.QuizzSession;
import com.example.coolproject.repository.ProfessorRepository;
import com.example.coolproject.repository.UserRepository;
import com.example.coolproject.service.AIService;
import com.example.coolproject.service.QuizzService;
import com.example.coolproject.web.dto.QuestionFormData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/quizz")
public class QuizzController {

  private static final Logger logger = LoggerFactory.getLogger(QuizzController.class);
  private final QuizzService quizzService;
  private final ProfessorRepository professorRepository;
  private final AIService aiService;

  @Autowired
  public QuizzController(QuizzService quizzService,
      UserRepository userRepository,
      ProfessorRepository professorRepository,
      AIService aiService) {
    this.quizzService = quizzService;
    this.professorRepository = professorRepository;
    this.aiService = aiService;
  }

  private Optional<Professor> getCurrentProfessor(Authentication authentication) {
    String email = authentication.getName();
    return professorRepository.findByEmail(email);
  }

  @GetMapping("/create")
  @PreAuthorize("hasAuthority('ROLE_PROFESSOR')")
  public String showCreateQuizzForm(Authentication authentication, RedirectAttributes redirectAttributes, Model model) {
    Optional<Professor> professorOpt = getCurrentProfessor(authentication);
    if (!professorOpt.isPresent()) {
        logger.warn("Professor not found for user: {}", authentication.getName());
        redirectAttributes.addFlashAttribute("errorMessage", "Professor profile not found.");
        return "redirect:/quizz/my-quizzes";
    }
    if (quizzService.professorHasOpenQuizzSession(professorOpt.get())) {
        redirectAttributes.addFlashAttribute("errorMessage", "You already have an open quiz. Please close it before creating a new one.");
        return "redirect:/quizz/my-quizzes";
    }
    model.addAttribute("title", ""); // Initialize form backing object attributes if needed
    model.addAttribute("prompt", "");
    return "quizz/create";
  }

  @PostMapping("/generate")
  @PreAuthorize("hasAuthority('ROLE_PROFESSOR')")
  public String generateQuizz(@RequestParam String title,
      @RequestParam String prompt,
      @RequestParam Integer durationMinutes,
      Model model,
      HttpSession session, Authentication authentication, RedirectAttributes redirectAttributes) {
    logger.info("Generate quizz request received with title: '{}', prompt: '{}', duration: {} minutes", title, prompt, durationMinutes);
    Optional<Professor> professorOpt = getCurrentProfessor(authentication);
    if (!professorOpt.isPresent()) {
        redirectAttributes.addFlashAttribute("errorMessage", "Professor profile not found.");
        return "redirect:/quizz/my-quizzes"; // Or appropriate error page
    }
    if (quizzService.professorHasOpenQuizzSession(professorOpt.get())) {
        redirectAttributes.addFlashAttribute("errorMessage", "You already have an open quiz. Please close it before creating a new one.");
        return "redirect:/quizz/my-quizzes";
    }
    String email = authentication.getName();

    try {
      List<Question> questions = aiService.generateQuestions(prompt);
      logger.info("Generated {} questions for professor {}", questions.size(), email);

      session.setAttribute("tempQuestions", questions);
      session.setAttribute("quizzTitle", title);
      session.setAttribute("quizzPrompt", prompt);
      session.setAttribute("quizzDurationMinutes", durationMinutes);

      model.addAttribute("questions", questions);
      model.addAttribute("title", title);
      model.addAttribute("prompt", prompt);

      return "quizz/questions";
    } catch (Exception e) {
      logger.error("Error generating quizz for professor {}: title '{}', prompt '{}'", email, title, prompt, e);
      model.addAttribute("error", "Failed to generate quiz: " + e.getMessage());
      return "redirect:/quizz/create?error=true";
    }
  }

  @PostMapping("/regenerate")
  @PreAuthorize("hasAuthority('ROLE_PROFESSOR')")
  public String regenerateQuestions(@RequestParam String title,
      @RequestParam String prompt,
      Model model,
      HttpSession session, Authentication authentication) {
    logger.info("Regenerate questions request received with title: '{}', prompt: '{}'", title, prompt);
    String email = authentication.getName();

    try {
      List<Question> questions = aiService.generateQuestions(prompt);
      logger.info("Regenerated {} questions for professor {}", questions.size(), email);

      session.setAttribute("tempQuestions", questions);
      session.setAttribute("quizzTitle", title);
      session.setAttribute("quizzPrompt", prompt);

      model.addAttribute("questions", questions);
      model.addAttribute("title", title);
      model.addAttribute("prompt", prompt);

      return "quizz/questions";
    } catch (Exception e) {
      logger.error("Error regenerating questions for professor {}: title '{}', prompt '{}'", email, title, prompt, e);
      return "redirect:/quizz/create?error=regenerate";
    }
  }

  @PostMapping("/create-quizz")
  @PreAuthorize("hasAuthority('ROLE_PROFESSOR')")
  public String createQuizzWithQuestions(@ModelAttribute QuestionFormData questionFormData,
      HttpSession httpSession,
      Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
    logger.info("Create quizz with questions request received");
    Optional<Professor> professorOpt = getCurrentProfessor(authentication);

    if (!professorOpt.isPresent()) {
      logger.error("Authenticated user {} is not found as a professor in the database.", authentication.getName());
      redirectAttributes.addFlashAttribute("errorMessage", "Professor profile not found.");
      return "redirect:/quizz/create?error=professor_not_found"; // Or a more general error page
    }
    Professor professor = professorOpt.get();

    if (quizzService.professorHasOpenQuizzSession(professor)) {
        redirectAttributes.addFlashAttribute("errorMessage", "You already have an open quiz. Please close it before creating a new one.");
        return "redirect:/quizz/my-quizzes";
    }

    String title = (String) httpSession.getAttribute("quizzTitle");
    if (title == null) {
      logger.error("No quizz title found in session for professor {}", professor.getEmail());
      return "redirect:/quizz/create?error=session";
    }
    // Retrieve duration from session
    Integer durationMinutes = (Integer) httpSession.getAttribute("quizzDurationMinutes");
    if (durationMinutes == null) {
        logger.warn("No quizz duration found in session for professor {}, defaulting or error handling might be needed.", professor.getEmail());
        // Decide on default behavior: error out, or use a default value. For now, let's assume it must be present or was defaulted in the form.
        // If defaulting here: durationMinutes = 15; // Example default
        // If erroring out because it should have been set by the generate step:
        redirectAttributes.addFlashAttribute("errorMessage", "Quiz duration was not set. Please try creating the quiz again.");
        return "redirect:/quizz/create?error=session_duration_missing";
    }

    try {
      List<Question> questions = new ArrayList<>();
      for (QuestionFormData.QuestionDto dto : questionFormData.getQuestions()) {
        Question question = new Question();
        question.setQuestionText(dto.getQuestionText());
        question.setModelAnswer(dto.getModelAnswer());
        question.setOrderIndex(dto.getOrderIndex());
        questions.add(question);
      }

      Quizz quizz = quizzService.createQuizzWithQuestions(title, professor, questions, durationMinutes);
      logger.info("Quizz created with ID: {} by professor {} with duration {} minutes", quizz.getId(), professor.getEmail(), durationMinutes);

      httpSession.removeAttribute("tempQuestions");
      httpSession.removeAttribute("quizzTitle");
      httpSession.removeAttribute("quizzPrompt");
      httpSession.removeAttribute("quizzDurationMinutes"); // Remove duration from session

      model.addAttribute("quizz", quizz);
      return "quizz/session-options";
    } catch (Exception e) {
      logger.error("Error creating quizz for professor {}: ", professor.getEmail(), e);
      return "redirect:/quizz/create?error=create";
    }
  }

  @PostMapping("/schedule-session/{quizzId}")
  @PreAuthorize("hasAuthority('ROLE_PROFESSOR')")
  public String scheduleQuizzSession(@PathVariable Long quizzId,
      @RequestParam int minutesFromNow,
      Model model, Authentication authentication) {
    logger.info("Schedule session request for quizz ID: {} by professor {}", quizzId, authentication.getName());
    String email = authentication.getName();

    Quizz quizz = quizzService.getQuizzById(quizzId);
    if (quizz.getSession() != null) {
      logger.warn("Quizz {} already has a session (status: {}). Requested by professor {}", quizzId, quizz.getSession().getStatus(), email);
      model.addAttribute("error", "This quizz already has a session or is scheduled.");
      return "quizz/my-quizzes";
    }

    QuizzSession session = quizzService.scheduleQuizzSession(quizzId, minutesFromNow);
    logger.info("Session scheduled with ID {} for quizz ID {} by professor {}. Title: {}", session.getId(), quizzId, email, session.getQuizz().getTitle());
    model.addAttribute("quizzSessionInfo", session);
    return "quizz/session-scheduled";
  }

  @GetMapping("/my-quizzes")
  @PreAuthorize("hasAuthority('ROLE_PROFESSOR')")
  public String showMyQuizzes(Model model, Authentication authentication) {
    String email = authentication.getName();
    logger.info("My quizzes request for professor {}", email);

    Optional<Professor> professorOpt = professorRepository.findByEmail(email);
    if (!professorOpt.isPresent()) {
      logger.error("Authenticated user {} is not found as a professor in the database for /my-quizzes.", email);
      // Consider redirecting to an error page or login if professor not found
      return "redirect:/error/403"; 
    }
    Professor professor = professorOpt.get();
    List<Quizz> quizzes = quizzService.getQuizzesByProfessor(professor);
    logger.info("Found {} quizzes for professor {}", quizzes.size(), email);

    boolean canCreateNewQuizz = !quizzService.professorHasOpenQuizzSession(professor);
    model.addAttribute("canCreateNewQuizz", canCreateNewQuizz);
    model.addAttribute("quizzes", quizzes);
    return "quizz/my-quizzes";
  }

  @PostMapping("/start-session/{id}")
  @PreAuthorize("hasRole('PROFESSOR')")
  public String startQuizzSession(@PathVariable("id") Long quizzId, RedirectAttributes redirectAttributes) {
    try {
      quizzService.startQuizzSession(quizzId);
      redirectAttributes.addFlashAttribute("successMessage", "Quizz session started successfully!");
    } catch (IllegalArgumentException | IllegalStateException e) {
      redirectAttributes.addFlashAttribute("errorMessage", "Error starting session: " + e.getMessage());
    }
    return "redirect:/quizz/my-quizzes";
  }

  @PostMapping("/stop-session/{sessionId}")
  @PreAuthorize("hasRole('PROFESSOR')") // Or other appropriate authorization
  public String stopQuizzSession(@PathVariable("sessionId") Long sessionId, RedirectAttributes redirectAttributes) {
    try {
      QuizzSession stoppedSession = quizzService.stopQuizzSession(sessionId);
      if (stoppedSession.getStatus() == QuizzSession.SessionStatus.CLOSED) {
        redirectAttributes.addFlashAttribute("successMessage", "Quizz session " + sessionId + " stopped successfully!");
      } else {
        // This case might occur if the session wasn't OPEN when stop was called
        redirectAttributes.addFlashAttribute("infoMessage", "Quizz session " + sessionId + " was not stopped. Status: " + stoppedSession.getStatus());
      }
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("errorMessage", "Error stopping session: " + e.getMessage());
    }
    return "redirect:/quizz/my-quizzes";
  }
}
