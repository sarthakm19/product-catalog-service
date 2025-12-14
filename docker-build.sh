#!/bin/bash

# Docker Build and Run Script for Product Catalog Service

set -e

echo "🐳 Product Catalog Service - Docker Build & Run"
echo "================================================"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if Docker is running
if ! docker info &> /dev/null; then
    echo "❌ Docker daemon is not running. Please start Docker."
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Parse command line arguments
ACTION=${1:-build}
IMAGE_NAME="product-catalog-service"
IMAGE_TAG=${2:-latest}

case $ACTION in
    build)
        echo "🔨 Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG}"
        docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .

        if [ $? -eq 0 ]; then
            echo "✅ Docker image built successfully!"
            echo ""
            echo "Image details:"
            docker images ${IMAGE_NAME}:${IMAGE_TAG}
            echo ""
            echo "To run the image:"
            echo "  ./docker-build.sh run"
            echo ""
            echo "To push to registry:"
            echo "  docker tag ${IMAGE_NAME}:${IMAGE_TAG} your-registry/${IMAGE_NAME}:${IMAGE_TAG}"
            echo "  docker push your-registry/${IMAGE_NAME}:${IMAGE_TAG}"
        else
            echo "❌ Docker build failed"
            exit 1
        fi
        ;;

    run)
        echo "🚀 Starting services with Docker Compose..."
        docker-compose up -d

        echo ""
        echo "⏳ Waiting for services to be healthy..."
        sleep 10

        echo ""
        echo "📊 Service status:"
        docker-compose ps

        echo ""
        echo "🌐 Application URLs:"
        echo "  Swagger UI: http://localhost:8080/swagger-ui.html"
        echo "  Health:     http://localhost:8080/actuator/health"
        echo "  PgAdmin:    http://localhost:8081"
        echo ""
        echo "📝 View logs:"
        echo "  docker-compose logs -f app"
        echo ""
        echo "🛑 Stop services:"
        echo "  docker-compose down"
        ;;

    stop)
        echo "🛑 Stopping services..."
        docker-compose down
        echo "✅ Services stopped"
        ;;

    clean)
        echo "🧹 Cleaning up..."
        docker-compose down -v
        docker rmi ${IMAGE_NAME}:${IMAGE_TAG} 2>/dev/null || true
        echo "✅ Cleanup complete"
        ;;

    logs)
        echo "📋 Showing application logs..."
        docker-compose logs -f app
        ;;

    *)
        echo "Usage: $0 {build|run|stop|clean|logs} [tag]"
        echo ""
        echo "Commands:"
        echo "  build [tag]  - Build Docker image (default tag: latest)"
        echo "  run          - Start all services with docker-compose"
        echo "  stop         - Stop all services"
        echo "  clean        - Stop services and remove images/volumes"
        echo "  logs         - Show application logs"
        echo ""
        echo "Examples:"
        echo "  $0 build           # Build with 'latest' tag"
        echo "  $0 build 1.0.0     # Build with '1.0.0' tag"
        echo "  $0 run             # Start all services"
        echo "  $0 logs            # View logs"
        exit 1
        ;;
esac

