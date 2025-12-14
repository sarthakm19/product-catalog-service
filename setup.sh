#!/bin/bash

# Product Catalog Service - Build and Run Script
# This script helps you build and run the application with proper error handling

set -e  # Exit on error

echo "🚀 Product Catalog Service - Setup Script"
echo "=========================================="
echo ""

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
echo "📋 Checking prerequisites..."

if ! command_exists java; then
    echo "❌ Java is not installed. Please install Java 21."
    exit 1
fi

if ! command_exists docker; then
    echo "⚠️  Docker is not installed. You'll need to set up PostgreSQL manually."
else
    echo "✅ Docker found"
fi

# Check Java version (require 25+)
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | grep -oE '[0-9]+' | head -n 1)
if [ -z "$JAVA_VERSION" ] || [ "$JAVA_VERSION" -lt 25 ]; then
    echo "❌ Java 25 or newer is required. Current: $JAVA_VERSION"
    exit 1
fi

# Check port 8080
if command -v lsof >/dev/null 2>&1 && lsof -i :8080 >/dev/null 2>&1; then
    echo "⚠️  Port 8080 is in use. Set SERVER_PORT to a free port."
fi

# Remind about JWT_SECRET
if [ -z "$JWT_SECRET" ]; then
    echo "⚠️  JWT_SECRET is not set. Set it for production: export JWT_SECRET=your-very-secret-key"
fi

echo ""
echo "🐘 Setting up PostgreSQL database..."

# Check if database container is running
if docker ps | grep -q product-catalog-db; then
    echo "✅ Database container is already running"
else
    echo "🔄 Starting PostgreSQL container..."
    docker run --name product-catalog-db \
        -e POSTGRES_DB=product_catalog_db \
        -e POSTGRES_USER=postgres \
        -e POSTGRES_PASSWORD=password \
        -p 5432:5432 \
        -d postgres:15

    echo "⏳ Waiting for database to be ready..."
    sleep 5
    echo "✅ Database started"
fi

echo ""
echo "🔨 Building the application..."
echo "   (This may take a few minutes on first build for dependency download and MapStruct code generation)"

# Clean and build
./gradlew clean build -x test --console=plain

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
else
    echo "❌ Build failed. Common issues:"
    echo "   1. Check if all Java files are properly formatted"
    echo "   2. Try: ./gradlew clean build --refresh-dependencies"
    echo "   3. In IntelliJ: File → Invalidate Caches → Restart"
    exit 1
fi

echo ""
echo "🎉 Setup complete!"
echo ""
echo "📝 To run the application:"
echo "   ./gradlew bootRun"
echo ""
echo "   OR in IntelliJ IDEA:"
echo "   Run → ProductCatalogServiceApplication"
echo ""
echo "🌐 Once running, access:"
echo "   Swagger UI: http://localhost:8080/swagger-ui.html"
echo "   API Docs:   http://localhost:8080/v3/api-docs"
echo "   Health:     http://localhost:8080/actuator/health"
echo ""
echo "🔑 Default credentials:"
echo "   Username: admin"
echo "   Password: admin123"
echo ""
echo "📚 See QUICK_START.md for more details"
echo ""
