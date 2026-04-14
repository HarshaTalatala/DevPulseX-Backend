#!/bin/bash

# Build script for Render deployment
# This script is automatically executed by Render during deployment

echo "🚀 Starting DevPulseX Backend build..."

# Prefer local JDK 25 when JAVA_HOME is not explicitly set
if [ -z "${JAVA_HOME}" ] && [ -d "${HOME}/.jdk/jdk-25" ]; then
    export JAVA_HOME="${HOME}/.jdk/jdk-25"
    export PATH="${JAVA_HOME}/bin:${PATH}"
    echo "☕ Using JAVA_HOME=${JAVA_HOME}"
fi

# Make Maven wrapper executable
chmod +x mvnw

# Clean and package the application (skip tests for faster builds)
echo "📦 Building with Maven..."
./mvnw clean package -DskipTests

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✅ Build completed successfully!"
    echo "📋 JAR file created: target/DevPulseX-Backend-1.0.0-mvp.jar"
    ls -lh target/*.jar
else
    echo "❌ Build failed!"
    exit 1
fi
