# syntax=docker/dockerfile:1

# Multi-stage build: build with Maven on JDK 25, then run on a slim JRE 25

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Build the application (skip tests for faster CI deploys)
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:25-jre
WORKDIR /app

# Set default runtime env
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

# Copy compiled jar from builder image
COPY --from=builder /app/target/DevPulseX-Backend-1.0.0-mvp.jar /app/app.jar

# Document the port (Render provides $PORT at runtime)
EXPOSE 8080

# Start the Spring Boot app binding to $PORT provided by Render
CMD ["sh", "-c", "java -Dserver.port=${PORT} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -jar /app/app.jar"]
