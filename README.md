# Cool Project - Quiz Platform

## 1. Introduction

Cool Project is a web-based quiz platform built with Spring Boot. It allows professors to create quizzes, optionally using AI to generate questions, and students to take these quizzes by writing answers. The platform focuses on managing one active quiz session at a time per professor for creation and administration, and provides a streamlined experience for students to join and participate in ongoing quizzes. The system also includes features for monitoring student activity during quizzes and detailed evaluation of student submissions.

## 2. Technologies Used

*   **Backend:**
    *   Java 21
    *   Spring Boot 3.4.5
    *   Spring MVC
    *   Spring Security (Form-based 2-step verification for Professors, OAuth2 for Students)
    *   Spring Data JPA
    *   Spring AI (with Google Generative Language API - e.g., Gemini)
    *   Spring WebSockets
    *   Thymeleaf (Server-side templating with `thymeleaf-extras-springsecurity6`)
    *   PostgreSQL (Database)
    *   Maven (Build tool and dependency management)
*   **Frontend:**
    *   HTML5, CSS3
    *   Tailwind CSS v4.1.6
    *   DaisyUI v5.0.35 (Tailwind CSS component library, integrated via `@plugin` in CSS)
    *   JavaScript (for theme handling, WebSocket interactions, and student activity tracking)
*   **Development & Other:**
    *   Node.js v20.11.0 & Yarn v1.22.19 (for frontend asset building via `frontend-maven-plugin`)
    *   SLF4J (Logging - transitive via Spring Boot)
    *   Docker (for containerization)

## 3. Project Structure

The project is a standard Maven project.

```
cool-project/
├── pom.xml                   # Maven project configuration
├── Dockerfile                # Docker configuration
├── package.json              # Node.js dependencies (Tailwind, DaisyUI)
├── tailwind.config.js        # Tailwind CSS configuration
├── postcss.config.js         # PostCSS configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/coolproject/
│   │   │       ├── CoolProjectApplication.java # Spring Boot main application class
│   │   │       ├── config/             # Spring configurations (SecurityConfig, WebSocketConfig)
│   │   │       ├── dto/                # Data Transfer Objects (StudentActionDTO)
│   │   │       ├── entity/             # JPA entities (User, Professor, Student, Quizz, Question, QuizzSession, Answer, StudentAction, StudentActionType)
│   │   │       ├── repository/         # Spring Data JPA repositories
│   │   │       ├── security/           # Custom security components (CustomUserDetailsService, ProfessorAuthenticationProvider)
│   │   │       ├── service/            # Business logic (QuizzService, QSmartGenService, StudentActionService)
│   │   │       └── web/                # Spring MVC Controllers (HomeController, QuizzController, QuizAttemptController, StudentActionController)
│   │   │           └── dto/            # DTOs specific to web layer (QuestionFormData, EvaluationForm)
│   │   └── resources/
│   │       ├── static/               # Static assets (CSS, JavaScript, images)
│   │       │   └── css/
│   │       │       ├── input.css     # Tailwind CSS main input file (imports tailwindcss and daisyui plugin)
│   │       │       └── output.css    # Generated CSS file
│   │       ├── templates/            # Thymeleaf HTML templates
│   │       └── application.properties  # Spring Boot application configuration
├── .mvn/                     # Maven wrapper
├── mvnw                      # Maven wrapper script (Unix)
├── mvnw.cmd                  # Maven wrapper script (Windows)
└── ... (other configuration files like .gitignore)
```

### Key Packages:

*   **`com.example.coolproject.config`**: Contains Spring configuration classes:
    *   `SecurityConfig.java`: Manages web security (OAuth2 for students, custom 2-step form login for professors), CSRF protection, public/protected paths, and `@EnableMethodSecurity`.
    *   `WebSocketConfig.java`: Configures STOMP over WebSockets with a simple broker.
*   **`com.example.coolproject.dto`**: Contains general Data Transfer Objects like `StudentActionDTO`.
*   **`com.example.coolproject.entity`**: Defines the JPA entities representing the core domain:
    *   `User`: Base class for users (email, name, roles as comma-separated string). `Professor` and `Student` extend this.
    *   `Professor`: Represents a quiz creator.
    *   `Student`: Represents a quiz taker (includes `githubId` and `avatarUrl` from OAuth2).
    *   `Quizz`: Represents a collection of questions, linked to a `Professor` (creator), and has a `durationMinutes`.
    *   `Question`: A single question within a quiz (text, model answer, `orderIndex`).
    *   `QuizzSession`: Manages the lifecycle of a quiz instance (scheduled, open, closed), timings, and `participants` (`ManyToMany` with `Student`). Has `scheduledStartTime`, `actualStartTime`, `endTime`, and `SessionStatus` enum.
    *   `Answer`: Tracks a student's text answer (Markdown format), `score`, and professor's `comment` for a specific `Question` in a `QuizzSession`. Replaces the concept of `StudentQuizAttempt`.
    *   `StudentAction`: Records various student activities during a quiz session (e.g., typing, tab switching, copy-paste). Linked to `Student`, `QuizzSession`, optionally `Question`, and has a `StudentActionType`.
    *   `StudentActionType`: Enum defining types of student actions (`COPY_PASTE`, `START_TYPING`, `JOIN_SESSION`, etc.).
*   **`com.example.coolproject.repository`**: Holds Spring Data JPA interfaces for database operations on all entities, including `AnswerRepository` and `StudentActionRepository`.
*   **`com.example.coolproject.security`**: Includes:
    *   `CustomUserDetailsService`: Loads user details by email for Spring Security.
    *   `ProfessorAuthenticationProvider`: Implements custom 2-step code-based authentication for professors using an in-memory, one-time code store.
*   **`com.example.coolproject.service`**: Contains the business logic:
    *   `QuizzService`: Handles quiz creation (manual and AI-assisted), question/session management (including scheduling, auto-start/stop via a `@Scheduled` task), student participation linking, and enforcement of professor's single open/created quiz rule.
    *   `QSmartGenService` (formerly AIService): Responsible for generating questions using Spring AI with a configured AI model (e.g., Google Gemini), based on a prompt and quiz duration.
    *   `StudentActionService`: Manages the creation of `StudentAction` records and sends WebSocket updates for real-time monitoring.
*   **`com.example.coolproject.web`**: Contains Spring MVC controllers and Data Transfer Objects (DTOs):
    *   `HomeController`: Manages general navigation (home page, login flow) and directs users to professor or student dashboards. Implements the multi-step professor login.
    *   `QuizzController`: Handles all professor-facing actions related to quiz creation, question management, session control (start, stop), monitoring student activity in a session, and evaluating student answers.
    *   `QuizAttemptController` (mapped to `/student`): Manages student quiz-taking, including joining sessions (which logs a `JOIN_SESSION` action), viewing questions, submitting/resubmitting answers (which are stored in the `Answer` entity and clear previous scores/comments).
    *   `StudentActionController` (REST controller at `/api/v1/student-actions`): Provides an endpoint for client-side code to log `StudentAction` events.
    *   `web/dto/`: Contains DTOs used primarily by controllers, such as `QuestionFormData` (for quiz creation) and `EvaluationForm` (for answer grading).

### Frontend Structure:

*   **Thymeleaf Templates**: Located in `src/main/resources/templates/`, used for server-side rendering of HTML.
*   **Static Assets**: CSS, JavaScript, and images are in `src/main/resources/static/`.
*   **Styling**:
    *   Tailwind CSS is the primary CSS framework.
    *   DaisyUI is used as a plugin for Tailwind CSS, providing pre-built UI components.
    *   The main Tailwind CSS directives and DaisyUI plugin are included in `src/main/resources/static/css/input.css`.
    *   The `frontend-maven-plugin` in `pom.xml` invokes `yarn` to run Tailwind CLI (`tailwindcss -i ./src/main/resources/static/css/input.css -o ./src/main/resources/static/css/output.css`) to process `input.css` (which includes Tailwind directives and `@plugin "daisyui";`) and generate `output.css`.

## 4. Domain Model & Functionality

The platform revolves around Users (Professors and Students) and Quizzes.

### Users

*   **`User`**: A base entity with common user attributes.
*   **`Professor`**:
    *   Can create and manage quizzes.
    *   Generates questions using `QSmartGenService` (based on a prompt and duration) or inputs them manually.
    *   Schedules, starts, and stops quiz sessions. Active sessions can also be auto-closed based on duration.
    *   A professor can only have one "open" or "created" quiz session (for creation/active management) at a time.
    *   Can monitor student activity during a live quiz session (e.g., tab switches, typing).
    *   Can evaluate submitted student answers, providing scores and comments.
*   **`Student`**:
    *   Can view and join active/scheduled quiz sessions.
    *   Submits answers (written text, potentially Markdown) to questions during an active session. Multiple submissions are allowed while the session is open, with newer submissions overriding older ones (and requiring re-evaluation).
    *   Their answers are stored in the `Answer` entity. Their activities (e.g., `JOIN_SESSION`, `COPY_PASTE`) are tracked in `StudentAction`.

### Quiz Lifecycle

1.  **Quiz Creation (Professor)**:
    *   A professor initiates quiz creation, providing a title, a subject/prompt, and a `durationMinutes`.
    *   The `QSmartGenService` generates a list of questions and model answers based on the prompt and duration. Questions are assigned an `orderIndex`.
    *   The professor reviews/edits these AI-generated questions or adds their own.
    *   The finalized `Quizz` (with its `Question` list and `durationMinutes`) is saved.
2.  **Session Management (Professor)**:
    *   The professor takes a created `Quizz` and can schedule a `QuizzSession` for it (defining a start time) or start it immediately.
    *   Scheduled sessions are automatically opened by a system scheduler when their start time is reached.
    *   The professor can manually stop an ongoing `QuizzSession`.
    *   Open sessions are automatically closed by the system scheduler when their `actualStartTime` + `durationMinutes` has passed.
    *   Professors can monitor student actions in real-time during an open session via a dashboard.
3.  **Quiz Participation (Student)**:
    *   Students see available `QuizzSession` instances that are `OPEN` (or `SCHEDULED` and past their start time) via the `HomeController`.
    *   A student joins an active session by navigating to the quiz-taking page (`QuizAttemptController`). This action registers them as a participant and logs a `JOIN_SESSION` `StudentAction`.
    *   Students answer questions; answers are saved individually to the `Answer` entity. Resubmissions are allowed while the session is `OPEN`, and previous scores/comments on that answer are cleared.
    *   Various student actions (typing, tab switching) are logged via `StudentActionController` and `StudentActionService` and can be viewed by the professor.
    *   Upon completion, or when the session ends (manually or automatically), student submissions are available for evaluation.
4.  **Evaluation (Professor)**:
    *   After a session is `CLOSED`, the professor can access student submissions via `QuizzController`.
    *   They can review each student's answers (`Answer` entity) and provide a `score` and `comment`.

### Quiz Session Management

*   A `QuizzSession` entity links to a `Quizz` and tracks its state:
    *   `CREATED`: Initial state after quiz is defined but no session is active.
    *   `SCHEDULED`: A start time is set for the future.
    *   `OPEN`: Actively running and students can participate. Opened manually or by scheduler.
    *   `CLOSED`: The session has ended (manually by professor or automatically by scheduler based on duration).
*   The `QuizzService` contains logic like `professorHasOpenQuizzSession` (checks for `CREATED` or `OPEN` status) to enforce that a professor manages only one quiz actively at any point.
*   A background scheduler (`@Scheduled` in `QuizzService`) handles auto-opening `SCHEDULED` sessions and auto-closing `OPEN` sessions based on their duration. WebSocket messages (`sessionOpen`, `sessionAutoClosed`, `sessionClosed`) are sent to update clients.
*   Students find an active session via `QuizzService.findAnyOpenQuizSession()` called from `HomeController`.

## 5. Authentication and Authorization

Spring Security is used to manage access control.

### Authentication

*   **Students**:
    *   Authenticate via OAuth2 (e.g., GitHub integration is configured in `SecurityConfig`).
    *   Upon successful OAuth2 login, a `Student` entity is created or updated (with GitHub ID and avatar URL) in the database, and they are assigned `ROLE_STUDENT`.
*   **Professors**:
    *   Authenticate via a custom 2-step form-based login flow:
        1.  Initial request at `/login/professor/initiate` with their `email`.
        2.  If the email is recognized (from `app.security.professor-emails` in `application.properties`), `ProfessorAuthenticationProvider` generates a one-time verification `code`, stores it in an in-memory map, and prints it to the server console (simulating an email/SMS for development). The user is redirected to a verification page.
        3.  User enters the code on the `/verify-professor` page, which is submitted to `/login/professor/verify`.
        4.  `ProfessorAuthenticationProvider` validates the code against the in-memory store. If valid, the professor is authenticated, and a `Professor` entity is created/updated. The one-time code is then removed from the store.
    *   Authentication is handled by `ProfessorAuthenticationProvider` and `CustomUserDetailsService`.
    *   Professors are expected to have `ROLE_PROFESSOR`.

### Authorization

*   `@EnableMethodSecurity(prePostEnabled = true)` allows for method-level security.
*   CSRF protection is enabled using `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
*   **Publicly Accessible Paths**:
    *   `/`, `/home` (conditionally, content varies by role)
    *   `/login`, `/login/professor/initiate`, `/verify-professor`, `/login/professor/verify`
    *   `/oauth2/**` (for OAuth2 flow)
    *   Static resources (`/css/**`, `/js/**`, etc.)
    *   WebSocket endpoint (`/ws/**`)
    *   Error pages.
*   **Protected Paths**:
    *   `/student/**` requires authentication (typically `ROLE_STUDENT`).
    *   `/quizz/**` (most professor actions) are secured with `hasAuthority('ROLE_PROFESSOR')` or `hasRole('PROFESSOR')`.
    *   `/api/v1/student-actions` (for logging student activity) requires authentication.
*   **Role-Based Access Control**:
    *   Professor-specific functionalities in `QuizzController` are secured.
    *   Student access to quiz-taking is managed by `QuizAttemptController`, ensuring they can only join/submit to `OPEN` (or view `SCHEDULED`) sessions.

## 6. Setup and Running the Project

1.  **Prerequisites**:
    *   JDK 21
    *   Maven
    *   Node.js (~v20.11.0) and Yarn (~v1.22.19) (for frontend asset building if not using pre-built assets)
    *   PostgreSQL database server
2.  **Database Setup**:
    *   Ensure PostgreSQL is running.
    *   Configure database connection details in `src/main/resources/application.properties` (e.g., `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`).
    *   The application uses `spring.jpa.hibernate.ddl-auto=create-drop`, so the schema will be created on startup and dropped on shutdown.
3.  **AI Service Setup**:
    *   The application is configured to use a Spring AI compatible service (e.g., Google's Generative Language API for Gemini).
    *   An API key must be provided in `src/main/resources/application.properties` for the `spring.ai.openai.api-key` property.
    *   The base URL and other options for the AI service are also in `application.properties`.
4.  **Build**:
    *   Run `mvn clean install` or `mvn spring-boot:run`. This will:
        *   Install Node/Yarn locally (if not available globally or if specified by plugin).
        *   Run `yarn install` to fetch frontend dependencies.
        *   Run `yarn run build:css` to compile `input.css` to `output.css`.
        *   Compile Java code and run the Spring Boot application.
5.  **Running**:
    *   After a successful build, the application can be run using `mvn spring-boot:run` or by executing the packaged JAR file.
    *   Access the application at `http://localhost:8080` (or the configured port).
    *   For professor login, check the application console for the one-time verification code after submitting your email.

*(Further details on environment variables and advanced Docker usage would typically be included here if applicable.)* 