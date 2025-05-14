#!/bin/bash
set -e # Exit immediately if a command exits with a non-zero status.

IMAGE_NAME="cool-project"
IMAGE_TAG="latest"

echo "Building Spring Boot application with Maven (includes frontend build)..."
# Optional: Run local Maven build first. This can be useful for local caching 
# and ensuring everything works before the Docker build. 
# However, the Dockerfile itself performs a full Maven build, so this is somewhat redundant for just the Docker image.
# ./mvnw clean package -DskipTests

echo "\nBuilding Docker image ${IMAGE_NAME}:${IMAGE_TAG}..."
docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" .

echo "\nDocker image build complete: ${IMAGE_NAME}:${IMAGE_TAG}"
echo "To run the container (example - maps container port 8080 to host port 8080):"
echo "  docker run -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_DB_HOST:DB_PORT/cool_project_db -e SPRING_DATASOURCE_USERNAME=your_user -e SPRING_DATASOURCE_PASSWORD=your_pass -e SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_ID=your_github_id -e SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_SECRET=your_github_secret ${IMAGE_NAME}:${IMAGE_TAG}"
echo "(Remember to replace placeholder environment variables with your actual PostgreSQL and GitHub OAuth details)" 