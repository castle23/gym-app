#!/bin/bash

set -e

echo "=== Gym Platform API - Production Deployment ==="
echo ""

# Configuration
ENVIRONMENT=${1:-production}
DOCKER_REGISTRY=${DOCKER_REGISTRY:-"docker.io"}
NAMESPACE=${2:-"gym-platform"}

echo "Deployment Configuration:"
echo "  Environment: $ENVIRONMENT"
echo "  Registry: $DOCKER_REGISTRY"
echo "  Namespace: $NAMESPACE"
echo ""

# Verify prerequisites
echo "Verifying prerequisites..."

if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "ERROR: Docker Compose is not installed"
    exit 1
fi

echo "✓ Docker and Docker Compose available"
echo ""

# Build phase
echo "=== Build Phase ==="
echo "Building Maven project..."

mvn clean package -DskipTests -q

if [ $? -ne 0 ]; then
    echo "ERROR: Build failed"
    exit 1
fi

echo "✓ Build successful"
echo ""

# Pre-deployment health checks
echo "=== Pre-Deployment Checks ==="

if docker-compose ps 2>/dev/null | grep -q "Up"; then
    echo "WARNING: Existing containers are running"
    read -p "Stop existing containers? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose down
        sleep 5
    fi
fi

echo "✓ Pre-deployment checks passed"
echo ""

# Deployment phase
echo "=== Deployment Phase ==="
echo "Starting services with docker-compose..."

if [ "$ENVIRONMENT" = "production" ]; then
    docker-compose -f docker-compose.prod.yml up -d
else
    docker-compose up -d
fi

if [ $? -ne 0 ]; then
    echo "ERROR: Docker Compose startup failed"
    exit 1
fi

echo "✓ Services started"
echo ""

# Wait for services to initialize
echo "Waiting for services to initialize (60 seconds)..."
sleep 60

# Post-deployment health checks
echo "=== Post-Deployment Validation ==="
./scripts/health-check.sh

if [ $? -ne 0 ]; then
    echo "WARNING: Some health checks failed"
    echo "Check service logs with: docker-compose logs -f"
    exit 1
fi

echo ""
echo "=== Deployment Complete ==="
echo ""
echo "Service URLs:"
echo "  Auth Service: http://localhost:8081"
echo "  Training Service: http://localhost:8082/training"
echo "  Tracking Service: http://localhost:8083/tracking"
echo "  Notification Service: http://localhost:8084/notifications"
echo ""
echo "Swagger UI URLs:"
echo "  Auth: http://localhost:8081/swagger-ui.html"
echo "  Training: http://localhost:8082/training/swagger-ui.html"
echo "  Tracking: http://localhost:8083/tracking/swagger-ui.html"
echo "  Notification: http://localhost:8084/notifications/swagger-ui.html"
echo ""
