#!/bin/bash

# Start script for Render deployment
# This script is automatically executed by Render to start the application

echo "🚀 Starting DevPulseX Backend..."

# Use PORT environment variable provided by Render
# Fallback to 8080 if not set (for local testing)
export SERVER_PORT=${PORT:-8080}

echo "📡 Server will start on port: $SERVER_PORT"
echo "🌍 Environment: ${SPRING_PROFILES_ACTIVE:-default}"

# Start the Spring Boot application
exec java \
    -Dserver.port=$SERVER_PORT \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} \
    -Xmx512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -jar target/DevPulseX-Backend-1.0.0-mvp.jar
