#!/bin/bash

# Set the environment variables for GitHub OAuth
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_ID="Iv23liuMV7Qiu1yfKnYW"
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_SECRET="8c004cc56ffe673ad99cf62f70baac7013457729"

# Override JPA DDL auto setting
export SPRING_JPA_HIBERNATE_DDL_AUTO="update"

# Run the Spring Boot application using Maven wrapper
echo "Starting Spring Boot application with overridden GitHub OAuth credentials..."
./mvnw spring-boot:run

# Unset the variables after the application stops (optional, good practice)
unset SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_ID
unset SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_SECRET

# Unset JPA DDL auto setting
unset SPRING_JPA_HIBERNATE_DDL_AUTO

echo "Spring Boot application stopped."
