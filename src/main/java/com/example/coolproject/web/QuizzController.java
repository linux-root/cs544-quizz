package com.example.coolproject.web;

import com.example.coolproject.entity.Professor;
import com.example.coolproject.entity.Question;
import com.example.coolproject.entity.Quizz;
import com.example.coolproject.entity.QuizzSession;
import com.example.coolproject.repository.ProfessorRepository;
import com.example.coolproject.repository.UserRepository;
import com.example.coolproject.repository.QuizzSessionRepository;
import com.example.coolproject.repository.StudentActionRepository;
import com.example.coolproject.service.QSmartGenService;
import com.example.coolproject.service.QuizzService;
import com.example.coolproject.web.dto.QuestionFormData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.Duration;
import com.example.coolproject.entity.Student;
import com.example.coolproject.entity.StudentAction;
import com.example.coolproject.entity.StudentActionType;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/quizz")
public class QuizzController {

  private static final Logger logger = LoggerFactory.getLogger(QuizzController.class);
  private final QuizzService quizzService;
  private final ProfessorRepository professorRepository;
  private final QSmartGenService aiService;
  private final SimpMessagingTemplate messagingTemplate;
  private final QuizzSessionRepository quizzSessionRepository;
  private final StudentActionRepository studentActionRepository;

  @Autowired
  public QuizzController(QuizzService quizzService,
      UserRepository userRepository,
      ProfessorRepository professorRepository,
      QSmartGenService aiService,
      SimpMessagingTemplate messagingTemplate,
      QuizzSessionRepository quizzSessionRepository,
      StudentActionRepository studentActionRepository) {
    this.quizzService = quizzService;
    this.professorRepository = professorRepository;
    this.aiService = aiService;
    this.messagingTemplate = messagingTemplate;
    this.quizzSessionRepository = quizzSessionRepository;
    this.studentActionRepository = studentActionRepository;
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
      redirectAttributes.addFlashAttribute("errorMessage",
          "You already have an open quiz. Please close it before creating a new one.");
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
    logger.info("Generate quizz request received with title: '{}', prompt: '{}', duration: {} minutes", title, prompt,
        durationMinutes);
    Optional<Professor> professorOpt = getCurrentProfessor(authentication);
    if (!professorOpt.isPresent()) {
      redirectAttributes.addFlashAttribute("errorMessage", "Professor profile not found.");
      return "redirect:/quizz/my-quizzes"; // Or appropriate error page
    }
    if (quizzService.professorHasOpenQuizzSession(professorOpt.get())) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "You already have an open quiz. Please close it before creating a new one.");
      return "redirect:/quizz/my-quizzes";
    }
    String email = authentication.getName();

    try {
      List<Question> questions = aiService.generateQuestions(prompt, durationMinutes);
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
      List<Question> questions = aiService.generateQuestions(prompt, 15); // FIXME: hardcoded
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
      redirectAttributes.addFlashAttribute("errorMessage",
          "You already have an open quiz. Please close it before creating a new one.");
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
      logger.warn("No quizz duration found in session for professor {}, defaulting or error handling might be needed.",
          professor.getEmail());
      // Decide on default behavior: error out, or use a default value. For now, let's
      // assume it must be present or was defaulted in the form.
      // If defaulting here: durationMinutes = 15; // Example default
      // If erroring out because it should have been set by the generate step:
      redirectAttributes.addFlashAttribute("errorMessage",
          "Quiz duration was not set. Please try creating the quiz again.");
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
      logger.info("Quizz created with ID: {} by professor {} with duration {} minutes", quizz.getId(),
          professor.getEmail(), durationMinutes);

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
      logger.warn("Quizz {} already has a session (status: {}). Requested by professor {}", quizzId,
          quizz.getSession().getStatus(), email);
      model.addAttribute("error", "This quizz already has a session or is scheduled.");
      return "quizz/my-quizzes";
    }

    QuizzSession session = quizzService.scheduleQuizzSession(quizzId, minutesFromNow);
    logger.info("Session scheduled with ID {} for quizz ID {} by professor {}. Title: {}", session.getId(), quizzId,
        email, session.getQuizz().getTitle());
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
      quizzService.stopQuizzSession(sessionId);
      redirectAttributes.addFlashAttribute("successMessage", "Quizz session stopped successfully.");
      messagingTemplate.convertAndSend("/topic/quizStatusUpdates", "{\"action\":\"sessionClosed\",\"sessionId\":" + sessionId + "}");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("errorMessage", "Error stopping quizz session: " + e.getMessage());
    }
    return "redirect:/quizz/my-quizzes";
  }

  @GetMapping("/session/{sessionId}/monitor")
  public String monitorQuizzSession(@PathVariable Long sessionId, Model model, RedirectAttributes redirectAttributes) {
    Optional<QuizzSession> sessionOpt = quizzSessionRepository.findById(sessionId);
    if (sessionOpt.isPresent()) {
        QuizzSession quizzSession = sessionOpt.get();
        model.addAttribute("quizzSession", quizzSession);
        Set<Student> participants = quizzSession.getParticipants();
        model.addAttribute("participants", participants);
        model.addAttribute("sessionId", sessionId);

        Map<Long, String> studentJoinTimeStrings = new HashMap<>();
        if (participants != null && !participants.isEmpty()) {
            // Fetch all JOIN_SESSION actions for this session, ordered by time
            List<StudentAction> joinActions = studentActionRepository.findByQuizzSessionAndActionTypeOrderByStartTimestampAsc(
                quizzSession, StudentActionType.JOIN_SESSION);

            Map<Long, LocalDateTime> earliestJoinTimes = new HashMap<>();
            for (StudentAction action : joinActions) {
                // Only record the first join time for each student
                earliestJoinTimes.putIfAbsent(action.getStudent().getId(), action.getStartTimestamp());
            }

            LocalDateTime now = LocalDateTime.now();
            for (Student student : participants) {
                LocalDateTime joinTime = earliestJoinTimes.get(student.getId());
                if (joinTime != null) {
                    Duration duration = Duration.between(joinTime, now);
                    long minutes = duration.toMinutes();
                    String timeAgoString;
                    if (minutes < 1) {
                        timeAgoString = "Just now";
                    } else if (minutes < 60) {
                        timeAgoString = minutes + (minutes == 1 ? " minute ago" : " minutes ago");
                    } else if (minutes < 1440) { // Less than 24 hours
                        long hours = duration.toHours();
                        timeAgoString = hours + (hours == 1 ? " hour ago" : " hours ago");
                    } else {
                        long days = duration.toDays();
                        timeAgoString = days + (days == 1 ? " day ago" : " days ago");
                    }
                    studentJoinTimeStrings.put(student.getId(), timeAgoString);
                } else {
                    studentJoinTimeStrings.put(student.getId(), "Join time not recorded");
                }
            }
        }
        model.addAttribute("studentJoinTimeStrings", studentJoinTimeStrings);

        Map<Long, Map<String, String>> studentLastActionData = new HashMap<>();
        if (participants != null && !participants.isEmpty()) {
            LocalDateTime nowForLastAction = LocalDateTime.now();
            for (Student student : participants) {
                Optional<StudentAction> lastActionOpt = studentActionRepository.findFirstByQuizzSessionAndStudentOrderByEndTimestampDesc(quizzSession, student);
                Map<String, String> actionData = new HashMap<>();
                if (lastActionOpt.isPresent()) {
                    StudentAction lastAction = lastActionOpt.get();
                    LocalDateTime actionTime = lastAction.getEndTimestamp();
                    String timeAgoString;

                    Duration duration = Duration.between(actionTime, nowForLastAction);
                    long minutes = duration.toMinutes();
                    if (minutes < 0) minutes = 0; // Handle potential clock sync issues or future timestamps gracefully

                    if (minutes < 1) {
                        timeAgoString = "Just now";
                    } else if (minutes < 60) {
                        timeAgoString = minutes + (minutes == 1 ? " minute ago" : " minutes ago");
                    } else if (minutes < 1440) { // Less than 24 hours
                        long hours = duration.toHours();
                        timeAgoString = hours + (hours == 1 ? " hour ago" : " hours ago");
                    } else {
                        long days = duration.toDays();
                        timeAgoString = days + (days == 1 ? " day ago" : " days ago");
                    }
                    // Format action type for better readability (e.g., JOIN_SESSION -> JOIN SESSION)
                    String formattedActionType = lastAction.getActionType().toString().replace("_", " ");
                    actionData.put("text", formattedActionType + " (" + timeAgoString + ")");
                    actionData.put("type", lastAction.getActionType().name()); // e.g., "JOIN_SESSION"
                    studentLastActionData.put(student.getId(), actionData);
                } else {
                    actionData.put("text", "No recent actions");
                    actionData.put("type", "NONE");
                    studentLastActionData.put(student.getId(), actionData);
                }
            }
        }
        model.addAttribute("studentLastActionData", studentLastActionData);

        // Format session timestamps for US locale
        DateTimeFormatter usDateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
        if (quizzSession.getScheduledStartTime() != null) {
            model.addAttribute("formattedScheduledStartTime", quizzSession.getScheduledStartTime().format(usDateTimeFormatter));
        }
        if (quizzSession.getActualStartTime() != null) {
            model.addAttribute("formattedActualStartTime", quizzSession.getActualStartTime().format(usDateTimeFormatter));
        }
        if (quizzSession.getEndTime() != null) {
            model.addAttribute("formattedEndTime", quizzSession.getEndTime().format(usDateTimeFormatter));
        }

    } else {
        redirectAttributes.addFlashAttribute("errorMessage", "Session not found.");
        return "redirect:/quizz/my-quizzes";
    }
    return "quizz/session-monitor";
  }

  @GetMapping("/session/{sessionId}/evaluate")
  public String evaluateQuizzSession(@PathVariable Long sessionId, Model model) {
    // Optional: Add logic to fetch session details, student answers etc. for the evaluation page
    model.addAttribute("sessionId", sessionId);
    return "quizz/session-evaluate";
  }
}
