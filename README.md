# Cool Project - Quiz Platform

## 1. Introduction

Cool Project is a web-based quiz platform built with Spring Boot. It allows professors to create quizzes, optionally using AI to generate questions, and students to take these quizzes by writing answers. The platform focuses on managing one active quiz session at a time per professor for creation and administration, and provides a streamlined experience for students to join and participate in ongoing quizzes.

## 2. Technologies Used

*   **Backend:**
    *   Java 21
    *   Spring Boot 3.4.5
    *   Spring MVC
    *   Spring Security (Form-based and OAuth2 login)
    *   Spring Data JPA
    *   Thymeleaf (Server-side templating)
    *   PostgreSQL (Database)
    *   Maven (Build tool and dependency management)
*   **Frontend:**
    *   HTML5, CSS3
    *   Tailwind CSS v4
    *   DaisyUI v5 (Tailwind CSS component library)
    *   JavaScript (for theme handling and potentially other interactions)
*   **Development & Other:**
    *   Node.js & Yarn (for frontend asset building via `frontend-maven-plugin`)
    *   Lombok (for reducing boilerplate code in entities - *inferred, common practice*)
    *   SLF4J (Logging)
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
│   │   │       ├── entity/             # JPA entities (User, Professor, Student, Quizz, Question, QuizzSession, StudentQuizAttempt)
│   │   │       ├── repository/         # Spring Data JPA repositories
│   │   │       ├── security/           # Custom security components (CustomUserDetailsService, ProfessorAuthenticationProvider)
│   │   │       ├── service/            # Business logic (QuizzService, AIService)
│   │   │       └── web/                # Spring MVC Controllers (HomeController, QuizzController) and DTOs
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

*   **`com.example.coolproject.config`**: Contains Spring configuration classes, notably `SecurityConfig.java` for web security and `WebSocketConfig.java`.
*   **`com.example.coolproject.entity`**: Defines the JPA entities representing the core domain:
    *   `User`: Base class for users.
    *   `Professor`: Extends `User`, represents a quiz creator.
    *   `Student`: Extends `User`, represents a quiz taker.
    *   `Quizz`: Represents a collection of questions.
    *   `Question`: A single question within a quiz, includes question text and model answer.
    *   `QuizzSession`: Manages the lifecycle of a quiz instance (scheduled, open, closed), timings, and participants.
    *   `StudentQuizAttempt`: Tracks a student's answers and progress for a specific quiz session.
*   **`com.example.coolproject.repository`**: Holds Spring Data JPA interfaces for database operations on entities.
*   **`com.example.coolproject.security`**: Includes:
    *   `CustomUserDetailsService`: Loads user details for Spring Security.
    *   `ProfessorAuthenticationProvider`: Provides custom authentication logic for professors.
*   **`com.example.coolproject.service`**: Contains the business logic:
    *   `QuizzService`: Handles quiz creation, session management, student attempts, and answer processing.
    *   `AIService`: Responsible for generating questions based on a prompt (currently a mock implementation).
*   **`com.example.coolproject.web`**: Contains Spring MVC controllers and Data Transfer Objects (DTOs):
    *   `HomeController`: Manages general navigation (home page, login).
    *   `QuizzController`: Handles all professor-facing actions related to quiz creation, question management, and session control. It is expected that student quiz-taking interactions are handled via other controllers or WebSocket communications, orchestrated by `QuizzService`.
    *   `dto/`: Contains DTOs for transferring data between controllers and views/services.

### Frontend Structure:

*   **Thymeleaf Templates**: Located in `src/main/resources/templates/`, used for server-side rendering of HTML.
*   **Static Assets**: CSS, JavaScript, and images are in `src/main/resources/static/`.
*   **Styling**:
    *   Tailwind CSS is the primary CSS framework.
    *   DaisyUI is used as a plugin for Tailwind CSS, providing pre-built UI components.
    *   The main Tailwind CSS directives and DaisyUI plugin are included in `src/main/resources/static/css/input.css`.
    *   The `frontend-maven-plugin` in `pom.xml` invokes `yarn` to run Tailwind CLI (`tailwindcss -i ... -o ...`) to process `input.css` and generate `output.css`.

## 4. Domain Model & Functionality

The platform revolves around Users (Professors and Students) and Quizzes.

### Users

*   **`User`**: A base entity with common user attributes.
*   **`Professor`**:
    *   Can create and manage quizzes.
    *   Generates questions using an AI service (based on a prompt) or inputs them manually.
    *   Schedules, starts, and stops quiz sessions.
    *   A professor can only have one "open" quiz session (for creation/active management) at a time.
*   **`Student`**:
    *   Can view and join active/scheduled quiz sessions they are part of.
    *   Submits answers (written text, potentially Markdown) to questions during an active session.
    *   Their progress and answers are tracked in `StudentQuizAttempt`.

### Quiz Lifecycle

1.  **Quiz Creation (Professor)**:
    *   A professor initiates quiz creation, providing a title and a subject/prompt.
    *   The `AIService` can generate a list of questions and model answers based on the prompt.
    *   The professor reviews/edits these AI-generated questions or adds their own.
    *   The finalized `Quizz` (with its `Question` list) is saved.
2.  **Session Management (Professor)**:
    *   The professor takes a created `Quizz` and schedules a `QuizzSession` for it, defining a start time.
    *   The professor can manually start a scheduled session or an unscheduled one.
    *   The professor can stop an ongoing `QuizzSession`.
3.  **Quiz Participation (Student)**:
    *   Students see available `QuizzSession` instances that are `OPEN` or `SCHEDULED` (and past their start time) and for which they are participants.
    *   A student `commenceQuizAttempt` to start taking the quiz. This creates a `StudentQuizAttempt`.
    *   Students answer questions; answers are saved individually (`saveStudentAnswer`).
    *   Upon completion or when the session ends, the attempt is finalized.

### Quiz Session Management

*   A `QuizzSession` entity links to a `Quizz` and tracks its state:
    *   `CREATED`: Initial state.
    *   `SCHEDULED`: A start time is set for the future.
    *   `OPEN`: Actively running and students can participate.
    *   `CLOSED`: The session has ended.
*   The `QuizzService` contains logic like `professorHasOpenQuizzSession` to enforce that a professor manages only one quiz actively at any point.
*   Students find the relevant active session to join via `QuizzService.getActiveQuizDetailsForStudent()`.

## 5. Authentication and Authorization

Spring Security is used to manage access control.

### Authentication

*   **Students**:
    *   Authenticate via OAuth2 (e.g., GitHub integration is configured in `SecurityConfig`).
    *   Upon successful OAuth2 login, a `Student` entity is created or updated in the database, and they are assigned `ROLE_STUDENT`.
*   **Professors**:
    *   Authenticate via a custom form-based login at `/login/professor`.
    *   They use their `email` and a special `code` (instead of a traditional password) for login.
    *   Authentication is handled by `ProfessorAuthenticationProvider` and `CustomUserDetailsService`.
    *   Professors are expected to have `ROLE_PROFESSOR`.

### Authorization

*   `@EnableMethodSecurity(prePostEnabled = true)` allows for method-level security using annotations like `@PreAuthorize`.
*   **Publicly Accessible Paths**:
    *   `/`, `/home` (conditionally, content varies by role)
    *   `/login`, `/login/professor`
    *   `/oauth2/**` (for OAuth2 flow)
    *   Static resources (`/css/**`, `/js/**`, etc.)
    *   Error pages.
*   **Protected Paths**: All other paths require authentication by default.
*   **Role-Based Access Control**:
    *   Professor-specific functionalities (e.g., in `QuizzController` like `/quizz/create`, `/quizz/generate`, `/quizz/start-session`) are secured using `hasAuthority('ROLE_PROFESSOR')` or `hasRole('ROLE_PROFESSOR')`.
    *   Student access to quizzes is managed by ensuring they can only join sessions they are participants in and by verifying ownership of their `StudentQuizAttempt` during answer submission.

## 6. Setup and Running the Project

1.  **Prerequisites**:
    *   JDK 21
    *   Maven
    *   Node.js and Yarn (for frontend asset building if not using pre-built assets)
    *   PostgreSQL database server
2.  **Database Setup**:
    *   Ensure PostgreSQL is running.
    *   Configure database connection details in `src/main/resources/application.properties` (e.g., `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`).
3.  **Build**:
    *   The project uses `frontend-maven-plugin` to build frontend assets (Tailwind CSS).
    *   Run `mvn clean install` or `mvn spring-boot:run`. This will:
        *   Install Node/Yarn locally (if not available globally or if specified by plugin).
        *   Run `yarn install` to fetch frontend dependencies.
        *   Run `yarn run build:css` to compile `input.css` to `output.css`.
        *   Compile Java code and run the Spring Boot application.
4.  **Running**:
    *   After a successful build, the application can be run using `mvn spring-boot:run` or by executing the packaged JAR file.
    *   Access the application at `http://localhost:8080` (or the configured port).

*(Further details on environment variables, specific AI service setup if it were real, and advanced Docker usage would typically be included here if applicable.)* 