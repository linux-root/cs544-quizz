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

  @GetMapping("/create")
  @PreAuthorize("hasAuthority('ROLE_PROFESSOR')")
  public String showCreateQuizzForm() {
    return "quizz/create";
  }

  @PostMapping("/generate")
  @PreAuthorize("hasAuthority('ROLE_PROFESSOR')")
  public String generateQuizz(@RequestParam String title,
      @RequestParam String prompt,
      Model model,
      HttpSession session, Authentication authentication) {
    logger.info("Generate quizz request received with title: '{}', prompt: '{}'", title, prompt);
    String email = authentication.getName();

    try {
      List<Question> questions = aiService.generateQuestions(prompt);
      logger.info("Generated {} questions for professor {}", questions.size(), email);

      session.setAttribute("tempQuestions", questions);
      session.setAttribute("quizzTitle", title);
      session.setAttribute("quizzPrompt", prompt);

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
      Model model, Authentication authentication) {
    logger.info("Create quizz with questions request received");
    String email = authentication.getName();
    Optional<Professor> professorOpt = professorRepository.findByEmail(email);

    if (!professorOpt.isPresent()) {
      logger.error("Authenticated user {} is not found as a professor in the database.", email);
      return "redirect:/error/403";
    }
    Professor professor = professorOpt.get();

    String title = (String) httpSession.getAttribute("quizzTitle");
    if (title == null) {
      logger.error("No quizz title found in session for professor {}", email);
      return "redirect:/quizz/create?error=session";
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

      Quizz quizz = quizzService.createQuizzWithQuestions(title, professor, questions);
      logger.info("Quizz created with ID: {} by professor {}", quizz.getId(), email);

      httpSession.removeAttribute("tempQuestions");
      httpSession.removeAttribute("quizzTitle");
      httpSession.removeAttribute("quizzPrompt");

      model.addAttribute("quizz", quizz);
      return "quizz/session-options";
    } catch (Exception e) {
      logger.error("Error creating quizz for professor {}: ", email, e);
      return "redirect:/quizz/create?error=create";
    }
  }

  @PostMapping("/start-session/{quizzId}")
  @PreAuthorize("hasAuthority('ROLE_PROFESSOR')")
  public String startQuizzSessionNow(@PathVariable Long quizzId,
      Model model, Authentication authentication) {
    logger.info("Start session request received for quizz ID: {} by professor {}", quizzId, authentication.getName());
    String email = authentication.getName();

    Quizz quizz = quizzService.getQuizzById(quizzId);
    if (quizz.getSession() != null && quizz.getSession().getStatus() == QuizzSession.SessionStatus.OPEN) {
      logger.warn("Quizz {} already has an active session. Requested by professor {}", quizzId, email);
      model.addAttribute("error", "This quizz already has an active session");
      return "quizz/my-quizzes";
    }

    QuizzSession session = quizzService.startQuizzSession(quizzId);
    model.addAttribute("quizzSessionInfo", session);
    return "quizz/session-started";
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
      return "redirect:/error/403";
    }
    Professor professor = professorOpt.get();
    List<Quizz> quizzes = quizzService.getQuizzesByProfessor(professor);
    logger.info("Found {} quizzes for professor {}", quizzes.size(), email);

    model.addAttribute("quizzes", quizzes);
    return "quizz/my-quizzes";
  }
}
