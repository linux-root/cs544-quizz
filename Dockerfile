# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw # Ensure mvnw is executable

# Download Maven dependencies (this layer will be cached if pom.xml or mvnw scripts don't change)
# Running a simple goal like dependency:go-offline to leverage caching
RUN ./mvnw dependency:go-offline

# Copy the rest of the application source code
COPY src ./src
COPY package.json yarn.lock tailwind.config.js postcss.config.js ./
# Note: frontend-maven-plugin will handle yarn install and build:css

# Build the application JAR
# The frontend-maven-plugin will execute yarn install and yarn run build:css
RUN ./mvnw package -DskipTests

# Stage 2: Create the final lightweight image
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/cool-project-0.0.1-SNAPSHOT.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 