#!/bin/bash

# Build script for Render deployment
# This script is automatically executed by Render during deployment

echo "🚀 Starting DevPulseX Backend build..."

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
