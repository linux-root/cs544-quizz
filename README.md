# Cool-Project: Spring Boot + GitHub Login + DaisyUI + PostgreSQL

This is a demo project showcasing the integration of Spring Boot with GitHub OAuth2 login, DaisyUI (via Tailwind CSS) for styling, and PostgreSQL for the database.

## Prerequisites

*   **Java JDK 21** or later
*   **Apache Maven** 3.6.x or later
*   **(Optional for local build, handled by Maven plugin) Node.js** (which includes npm) LTS version recommended
*   **(Optional for local build, handled by Maven plugin) Yarn** (can be installed via `npm install --global yarn`)
*   **PostgreSQL** server running locally or accessible
*   **Docker** (if you want to build and run the Docker image)

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
        # For Docker, you might pass these as environment variables, see Docker section.
        ```

## Building and Running the Application

### Local Development (using Maven)

1.  **Build and Run:**
    *   The frontend build (Tailwind CSS compilation) is now integrated into the Maven build process via the `frontend-maven-plugin`.
    *   To build the application (including frontend assets) and run it:
        ```bash
        ./mvnw spring-boot:run
        ```
    *   (On Windows, use `mvnw.cmd spring-boot:run`)
    *   This command will compile Java sources, process frontend assets, and start the Spring Boot application.

2.  **Build JAR file:**
    *   To create an executable JAR file:
        ```bash
        ./mvnw clean package
        ```
    *   Then run the JAR:
        ```bash
        java -jar target/cool-project-0.0.1-SNAPSHOT.jar
        ```

3.  **Access the Application:**
    *   Open your browser and go to `http://localhost:8080`.

### Using Docker

1.  **Build the Docker Image:**
    *   Ensure Docker is installed and running.
    *   Use the provided build script:
        ```bash
        ./build.sh
        ```
    *   This script will use the `Dockerfile` to build a Docker image named `cool-project:latest`.
    *   The Docker build process itself runs `./mvnw package`, so it will also build the frontend assets.

2.  **Run the Docker Container:**
    *   After the image is built, you can run it as a container. You **must** provide the necessary environment variables for the database connection and GitHub OAuth credentials.
    *   Example:
        ```bash
        docker run -p 8080:8080 \
          -e SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_DB_HOST:DB_PORT/cool_project_db \
          -e SPRING_DATASOURCE_USERNAME=your_db_user \
          -e SPRING_DATASOURCE_PASSWORD=your_db_password \
          -e SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_ID=your_github_client_id \
          -e SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_SECRET=your_github_client_secret \
          cool-project:latest
        ```
    *   **Important Notes for Docker:**
        *   Replace `YOUR_DB_HOST:DB_PORT` with your PostgreSQL host and port. If running PostgreSQL in another Docker container on the same Docker network, you might use the service name of the PostgreSQL container as the host.
        *   If running PostgreSQL on your host machine from a Docker container, `YOUR_DB_HOST` might be `host.docker.internal` (on Docker Desktop) or your machine's IP address on the Docker bridge network.
        *   Ensure the PostgreSQL database (`cool_project_db`) is accessible from where the Docker container will run.

3.  **Access the Application (Dockerized):**
    *   Open your browser and go to `http://localhost:8080` (assuming you mapped port 8080).

## Development

*   **Frontend (Tailwind/DaisyUI):**
    *   If you are making CSS changes and want to see them reflected quickly during local development (without a full `./mvnw spring-boot:run` restart), you can still run the Yarn watch script in a separate terminal:
        ```bash
        yarn run watch:css
        ```
    *   This will watch for changes in your HTML/template files and `input.css`, and recompile `output.css` on the fly. Spring Boot Devtools might pick up changes to `output.css` in `static` resources and live-reload.
*   **Backend (Spring Boot):**
    *   Spring Boot Devtools is included for live reload and other development-time features.

## Next Steps

*   Create JPA entities and repositories to interact with your PostgreSQL database.
*   Build out more UI features using Thymeleaf and DaisyUI components.
*   Implement the actual code generation/verification logic for professor email login if the "cheat code" is not sufficient.
*   Consider setting up database migrations (e.g., Flyway, Liquibase) for more robust schema management. 