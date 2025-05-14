# Cool-Project: Spring Boot + GitHub Login + DaisyUI + PostgreSQL

This is a demo project showcasing the integration of Spring Boot with GitHub OAuth2 login, DaisyUI (via Tailwind CSS) for styling, and PostgreSQL for the database.

## Prerequisites

*   **Java JDK 21** or later
*   **Apache Maven** 3.6.x or later
*   **Node.js** (which includes npm) LTS version recommended
*   **Yarn** (can be installed via `npm install --global yarn`)
*   **PostgreSQL** server running locally or accessible

## Configuration

1.  **GitHub OAuth App:**
    *   Go to your GitHub account settings > Developer settings > OAuth Apps.
    *   Click "New OAuth App".
    *   Application name: (e.g., `Cool-Project Dev`)
    *   Homepage URL: `http://localhost:8080`
    *   Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`
    *   Generate a new client secret.
    *   Open `src/main/resources/application.properties` and update the following:
        ```properties
        spring.security.oauth2.client.registration.github.client-id=YOUR_ACTUAL_GITHUB_CLIENT_ID
        spring.security.oauth2.client.registration.github.client-secret=YOUR_ACTUAL_GITHUB_CLIENT_SECRET
        ```

2.  **PostgreSQL Database:**
    *   Ensure your PostgreSQL server is running.
    *   Create a database (e.g., `cool_project_db`).
    *   Open `src/main/resources/application.properties` and update the following with your PostgreSQL details if they differ from the defaults:
        ```properties
        spring.datasource.url=jdbc:postgresql://localhost:5432/cool_project_db
        spring.datasource.username=your_postgres_user
        spring.datasource.password=your_postgres_password
        ```

## Building and Running the Application

1.  **Build Tailwind CSS (Important!):**
    *   The Tailwind CSS styles need to be compiled. Open a terminal in the project root directory and run:
        ```bash
        yarn install
        yarn run build:css
        ```
    *   This will generate the `src/main/resources/static/css/output.css` file required by the application.
    *   If you encounter issues with `yarn run build:css` (e.g., "tailwindcss: command not found"), ensure your Node.js/Yarn environment is correctly set up and that `tailwindcss` (installed as a dev dependency) can be executed. You might need to troubleshoot your local Node.js/Yarn PATH or try alternative ways to run locally installed Node CLI tools.

2.  **Run the Spring Boot Application:**
    *   You can run the application using Maven:
        ```bash
        ./mvnw spring-boot:run
        ```
    *   (On Windows, use `mvnw.cmd spring-boot:run`)
    *   Alternatively, you can build a JAR file and run it:
        ```bash
        ./mvnw clean package
        java -jar target/cool-project-0.0.1-SNAPSHOT.jar
        ```

3.  **Access the Application:**
    *   Open your browser and go to `http://localhost:8080`.
    *   You should be redirected to the login page, where you can log in with GitHub.

## Development

*   **Frontend (Tailwind/DaisyUI):**
    *   To automatically rebuild CSS on changes, run the watch script in a separate terminal:
        ```bash
        yarn run watch:css
        ```
*   **Backend (Spring Boot):**
    *   Spring Boot Devtools is included, which provides live reload and other development-time features. Most IDEs will automatically pick this up when running the application.

## Next Steps

*   Create JPA entities and repositories to interact with your PostgreSQL database.
*   Build out more UI features using Thymeleaf and DaisyUI components.
*   Consider setting up database migrations (e.g., Flyway, Liquibase) for more robust schema management as your project grows. 