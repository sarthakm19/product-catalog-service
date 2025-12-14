# Multi-stage Dockerfile for Product Catalog Service
# Stage 1: Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder

# Install necessary tools
RUN apk add --no-cache bash

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application (skip tests for faster builds)
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: Runtime stage
FROM eclipse-temurin:25-jre-alpine

# Set metadata
LABEL maintainer="support@productcatalog.com"
LABEL description="Product Catalog Service - E-commerce Microservice"
LABEL version="1.0.0"

# Create a non-root user for running the application
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Set JVM options for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

