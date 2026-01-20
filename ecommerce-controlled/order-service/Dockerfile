# Single-stage Dockerfile for order-service
# Java 21 + Spring Boot 3.2.1

FROM maven:3.9.6-eclipse-temurin-21-alpine

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

# Expose application port
EXPOSE 8082

# Set environment variables with defaults
ENV SPRING_PROFILES_ACTIVE=default
ENV SERVER_PORT=8082
ENV KAFKA_BROKERS=127.0.0.1:29023
ENV INVENTORY_SERVICE_URL=http://localhost:8081

# Run the application
ENTRYPOINT ["java", "-jar", "target/order-service-1.0.0.jar"]
