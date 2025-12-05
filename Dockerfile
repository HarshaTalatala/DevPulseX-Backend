# syntax=docker/dockerfile:1

############################################
# 1. BUILD STAGE (Maven + JDK 21)
############################################
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Cache dependencies first for faster builds
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Copy source
COPY src ./src

# Build the JAR
RUN mvn -q -DskipTests clean package


############################################
# 2. RUNTIME STAGE (Slim JRE 21)
############################################
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Set runtime environment vars
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

# Copy built JAR
COPY --from=builder /app/target/*.jar /app/app.jar

# Expose container port (Cloud Run injects $PORT)
EXPOSE 8080

# JVM Optimizations for Cloud Run
# - Lower memory footprint
# - Faster startup
# - Respect Cloud Run CPU throttling
ENTRYPOINT ["sh", "-c", "java \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=70.0 \
  -XX:+UseG1GC \
  -Dserver.port=${PORT} \
  -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
  -jar /app/app.jar"]
