# Docker Deployment Guide - Product Catalog Service

This guide provides instructions for building, running, and deploying the Product Catalog Service using Docker.

## Prerequisites

- Docker 20.10+ installed
- Docker Compose 2.0+ installed
- At least 2GB RAM available for Docker
- Port 8080, 5432, and 8081 available (or configure alternative ports)

## Quick Start with Docker Compose

### 1. Build and Run Everything

```bash
# Build and start all services (app, postgres, pgadmin, redis)
docker-compose up -d --build

# View logs
docker-compose logs -f app

# Check service health
docker-compose ps
```

### 2. Access the Application

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/actuator/health
- **PgAdmin**: http://localhost:8081 (admin@admin.com / admin)

### 3. Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

## Building the Docker Image

### Build Locally

```bash
# Build the image
docker build -t product-catalog-service:latest .

# Build with specific tag
docker build -t product-catalog-service:1.0.0 .

# Build for specific platform (e.g., for ARM64 or AMD64)
docker build --platform linux/amd64 -t product-catalog-service:latest .
```

### Multi-Architecture Build (for cloud deployment)

```bash
# Create a builder instance
docker buildx create --name multiarch-builder --use

# Build for multiple platforms
docker buildx build --platform linux/amd64,linux/arm64 \
  -t your-registry.com/product-catalog-service:latest \
  --push .
```

## Running with Docker

### Run with Default Settings

```bash
# Start PostgreSQL first
docker run -d \
  --name product-catalog-db \
  -e POSTGRES_DB=product_catalog_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  postgres:15-alpine

# Run the application
docker run -d \
  --name product-catalog-service \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/product_catalog_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -e JWT_SECRET=your-very-secret-key \
  product-catalog-service:latest
```

### Run with Custom Configuration

```bash
docker run -d \
  --name product-catalog-service \
  -p 9090:8080 \
  -e SERVER_PORT=8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/your_db \
  -e SPRING_DATASOURCE_USERNAME=your_user \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  -e JWT_SECRET=your-production-secret-key \
  -e JWT_EXPIRATION=86400000 \
  -e SPRING_PROFILES_ACTIVE=prod \
  product-catalog-service:latest
```

## Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SERVER_PORT` | Application port | 8080 | No |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | - | Yes |
| `SPRING_DATASOURCE_USERNAME` | Database username | - | Yes |
| `SPRING_DATASOURCE_PASSWORD` | Database password | - | Yes |
| `JWT_SECRET` | JWT signing secret | - | Yes (Production) |
| `JWT_EXPIRATION` | Token expiration (ms) | 86400000 | No |
| `SPRING_PROFILES_ACTIVE` | Active Spring profiles | default | No |
| `JAVA_OPTS` | JVM options | Auto-configured | No |

## Cloud Deployment

### AWS ECS/Fargate

1. **Push image to ECR:**
   ```bash
   # Authenticate to ECR
   aws ecr get-login-password --region us-east-1 | \
     docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
   
   # Tag image
   docker tag product-catalog-service:latest \
     <account-id>.dkr.ecr.us-east-1.amazonaws.com/product-catalog-service:latest
   
   # Push image
   docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/product-catalog-service:latest
   ```

2. **Create ECS Task Definition** with:
   - Container image: ECR repository URL
   - Environment variables: Set from AWS Secrets Manager or Parameter Store
   - Port mappings: 8080
   - Health check: `/actuator/health`
   - CPU: 512-1024 (0.5-1 vCPU)
   - Memory: 1024-2048 MB

### Azure Container Instances

```bash
# Create resource group
az group create --name product-catalog-rg --location eastus

# Create container instance
az container create \
  --resource-group product-catalog-rg \
  --name product-catalog-service \
  --image <your-registry>/product-catalog-service:latest \
  --dns-name-label product-catalog \
  --ports 8080 \
  --environment-variables \
    SERVER_PORT=8080 \
    SPRING_DATASOURCE_URL='jdbc:postgresql://your-db:5432/db' \
  --secure-environment-variables \
    SPRING_DATASOURCE_PASSWORD='password' \
    JWT_SECRET='secret' \
  --cpu 1 \
  --memory 2
```

### Google Cloud Run

```bash
# Build and push to GCR
gcloud builds submit --tag gcr.io/PROJECT_ID/product-catalog-service

# Deploy to Cloud Run
gcloud run deploy product-catalog-service \
  --image gcr.io/PROJECT_ID/product-catalog-service \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars SPRING_DATASOURCE_URL=jdbc:postgresql://... \
  --set-secrets JWT_SECRET=jwt-secret:latest \
  --port 8080 \
  --memory 1Gi \
  --cpu 1
```

### Kubernetes Deployment

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: product-catalog-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: product-catalog-service
  template:
    metadata:
      labels:
        app: product-catalog-service
    spec:
      containers:
      - name: app
        image: your-registry/product-catalog-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres-service:5432/product_catalog_db"
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: product-catalog-service
spec:
  selector:
    app: product-catalog-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
```

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker logs product-catalog-service

# Check specific error
docker logs product-catalog-service 2>&1 | grep ERROR

# Exec into container
docker exec -it product-catalog-service sh
```

### Database Connection Issues

```bash
# Test database connectivity from container
docker exec -it product-catalog-service sh -c \
  "wget --spider jdbc:postgresql://postgres:5432/product_catalog_db"

# Check network
docker network inspect product-catalog-network
```

### Memory Issues

```bash
# Check container stats
docker stats product-catalog-service

# Increase memory limit
docker run -d --memory="2g" --memory-swap="2g" \
  product-catalog-service:latest
```

## Security Best Practices

1. **Never commit secrets** - Use Docker secrets, environment variables from secure sources
2. **Use non-root user** - Dockerfile already configured with `spring` user
3. **Scan images** - Run security scans before deployment
   ```bash
   docker scan product-catalog-service:latest
   ```
4. **Keep base images updated** - Regularly rebuild with latest base images
5. **Use specific tags** - Avoid `:latest` in production
6. **Enable health checks** - Configured in Dockerfile and compose.yaml
7. **Limit resources** - Set CPU and memory limits
8. **Use private registries** - Don't expose images publicly

## Monitoring and Logs

### View Logs

```bash
# Follow logs
docker logs -f product-catalog-service

# Last 100 lines
docker logs --tail 100 product-catalog-service

# Logs since specific time
docker logs --since 10m product-catalog-service
```

### Export Logs

```bash
# Export to file
docker logs product-catalog-service > app.log 2>&1
```

### Health Monitoring

```bash
# Check health status
docker inspect --format='{{.State.Health.Status}}' product-catalog-service

# Continuous health monitoring
watch -n 5 'curl -s http://localhost:8080/actuator/health | jq'
```

## Performance Optimization

### JVM Tuning

Already configured in Dockerfile:
- `-XX:+UseContainerSupport` - Respect container limits
- `-XX:MaxRAMPercentage=75.0` - Use 75% of available RAM
- `-XX:InitialRAMPercentage=50.0` - Start with 50% of RAM

### Build Optimization

- Multi-stage build reduces final image size
- Gradle dependencies cached in separate layer
- `.dockerignore` excludes unnecessary files

## Image Size

```bash
# Check image size
docker images product-catalog-service

# Expected size: ~350-400MB (with JRE 25 Alpine)
```

## Support

For issues or questions:
- Check logs: `docker logs product-catalog-service`
- Review documentation: README.md, QUICK_START.md
- Contact: support@productcatalog.com

