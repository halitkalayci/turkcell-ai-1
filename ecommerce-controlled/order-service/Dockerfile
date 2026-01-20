# Multi-stage Dockerfile for order-service
# Java 21 + Spring Boot 3.2.1

# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml
COPY pom.xml ./

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy the JAR from builder stage
COPY --from=builder /app/target/order-service-1.0.0.jar app.jar

# Expose application port
EXPOSE 8082

# Set environment variables with defaults
ENV SPRING_PROFILES_ACTIVE=k8s
ENV SERVER_PORT=8082

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
