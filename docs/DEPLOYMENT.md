# Deployment Guide

Complete guide for deploying the Product Catalog Service — from local development through production Kubernetes with GitOps.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development](#1-local-development)
3. [Docker Deployment](#2-docker-deployment)
4. [Kubernetes (EKS) Deployment](#3-kubernetes-eks-deployment)
5. [GitOps with ArgoCD](#4-gitops-with-argocd)
6. [CI/CD Pipeline](#5-cicd-pipeline)
7. [Pre-Deployment Checklist](#6-pre-deployment-checklist)
8. [Environment Configuration Reference](#7-environment-configuration-reference)

---

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 25+ | Application runtime |
| Gradle | 9.x (wrapper included) | Build tool |
| Docker | 20.10+ | Containerization |
| Docker Compose | 2.0+ | Local multi-service orchestration |
| kubectl | Latest | Kubernetes CLI |
| kustomize | Latest | K8s manifest management |
| AWS CLI | 2.x | AWS resource management |
| ArgoCD CLI | Latest | GitOps deployment (optional) |

---

## 1. Local Development

### Start Database

```bash
docker run --name product-catalog-db \
  -e POSTGRES_DB=product_catalog_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15
```

### Build & Run

```bash
export JWT_SECRET=your-very-secret-key
export SERVER_PORT=8087
./gradlew clean build
./gradlew bootRun
```

### Verify

| Endpoint | URL |
|----------|-----|
| Swagger UI | http://localhost:8087/swagger-ui.html |
| API Docs | http://localhost:8087/v3/api-docs |
| Health Check | http://localhost:8087/actuator/health |

---

## 2. Docker Deployment

### Docker Compose (Recommended)

```bash
# Build and start all services (app, postgres, pgadmin, redis)
docker-compose up -d --build

# View logs
docker-compose logs -f app

# Stop services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

**Services included:** Application (port 8087), PostgreSQL (5432), PgAdmin (8081), Redis.

### Build Script

```bash
./docker-build.sh build      # Build image
./docker-build.sh run        # Start with compose
./docker-build.sh stop       # Stop services
./docker-build.sh clean      # Clean up
./docker-build.sh logs       # View logs
```

### Manual Docker Build

```bash
# Build image
docker build -t product-catalog-service:latest .

# Run container
docker run -d \
  --name product-catalog-service \
  -p 8087:8087 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/product_catalog_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -e JWT_SECRET=your-very-secret-key \
  product-catalog-service:latest
```

### Multi-Architecture Build (for cloud)

```bash
docker buildx create --name multiarch-builder --use
docker buildx build --platform linux/amd64,linux/arm64 \
  -t your-registry.com/product-catalog-service:latest \
  --push .
```

### Docker Image Details

- **Base:** `eclipse-temurin:25-jre-alpine` (~350-400MB final)
- **Build:** Multi-stage (JDK for build, JRE for runtime)
- **Security:** Runs as non-root `spring` user
- **JVM Tuning:** Container-aware memory (`-XX:MaxRAMPercentage=75.0`)
- **Health Check:** Built-in via `/actuator/health`

---

## 3. Kubernetes (EKS) Deployment

### 3.1 AWS Infrastructure Prerequisites

1. **EKS Cluster** running Kubernetes 1.25+
2. **ECR Repository** for Docker images
3. **RDS PostgreSQL** instance (see [docs/DATABASE.md](./DATABASE.md))
4. **AWS Secrets Manager** secrets created
5. **AWS Load Balancer Controller** installed on the cluster
6. **IAM Roles** configured (IRSA for service account)

### 3.2 Push Image to ECR

```bash
# Authenticate to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Tag and push
docker tag product-catalog-service:latest \
  <account-id>.dkr.ecr.us-east-1.amazonaws.com/product-catalog-service:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/product-catalog-service:latest
```

### 3.3 Configure Secrets

Create the Kubernetes secret (or use External Secrets Operator with AWS Secrets Manager):

```bash
kubectl create secret generic product-catalog-secrets \
  --namespace product-catalog \
  --from-literal=jwt-secret='your-production-jwt-secret' \
  --from-literal=db-url='jdbc:postgresql://your-rds-endpoint:5432/product_catalog_db' \
  --from-literal=db-username='your-db-user' \
  --from-literal=db-password='your-db-password'
```

### 3.4 Deploy with Kustomize

The manifests use a **base + overlays** pattern under `k8s/`:

```bash
# Development
kubectl apply -k k8s/overlays/dev

# Staging
kubectl apply -k k8s/overlays/staging

# Production
kubectl apply -k k8s/overlays/production
```

### 3.5 Deploy with Automated Script

```bash
chmod +x k8s/setup-deployment.sh
./k8s/setup-deployment.sh production
```

### 3.6 Deploy with Helm (Alternative)

```bash
helm install product-catalog ./k8s/helm \
  -f ./k8s/helm/values-production.yaml \
  --namespace product-catalog
```

### 3.7 Verify Deployment

```bash
# Check pods
kubectl get pods -n product-catalog

# Check services
kubectl get svc -n product-catalog

# Check ingress (ALB)
kubectl get ingress -n product-catalog

# View logs
kubectl logs -f deployment/product-catalog -n product-catalog

# Test health
kubectl exec -n product-catalog <pod-name> -- \
  curl -s http://localhost:8087/actuator/health
```

### 3.8 Environment Specifications

| Attribute | Dev | Staging | Production |
|-----------|-----|---------|------------|
| Replicas | 1 | 2 (2-6 HPA) | 3 (3-10 HPA) |
| CPU Request/Limit | 100m / 500m | 200m / 750m | 500m / 1500m |
| Memory Request/Limit | 256Mi / 512Mi | 384Mi / 768Mi | 768Mi / 1024Mi |
| Log Level | DEBUG | INFO | WARN |
| Autoscaling | Disabled | Enabled | Aggressive |
| Network Policies | Relaxed | Standard | Strict |
| PDB Min Available | — | 1 | 2 |

---

## 4. GitOps with ArgoCD

### 4.1 Architecture

```
Developer pushes code → GitHub Actions (CI) → Build & Push to ECR
                                             → Update image tag in Git
                       → ArgoCD (CD) detects Git change → Syncs to EKS cluster
```

**Key Principle:** CI builds and pushes images; CD (ArgoCD) manages cluster state. No `kubectl` in CI pipelines.

### 4.2 Install ArgoCD

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f \
  https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

### 4.3 Register Applications

Update the repository URL in the ArgoCD application files, then apply:

```bash
# Create all three environment applications
kubectl apply -f k8s/argocd/application-dev.yaml
kubectl apply -f k8s/argocd/application-staging.yaml
kubectl apply -f k8s/argocd/application-prod.yaml
```

| Application | Namespace | Branch | Sync Policy |
|-------------|-----------|--------|-------------|
| `product-catalog-dev` | `product-catalog-dev` | `develop` | Manual |
| `product-catalog-staging` | `product-catalog-staging` | `staging` | Auto + SelfHeal |
| `product-catalog` | `product-catalog` | `main` | Auto + SelfHeal + Prune |

### 4.4 Daily Operations

```bash
# List applications
argocd app list

# Manual sync (dev)
argocd app sync product-catalog-dev

# Check status
argocd app get product-catalog

# View diff
argocd app diff product-catalog

# Rollback
argocd app rollback product-catalog <revision>

# Force sync
argocd app sync product-catalog --force --prune
```

### 4.5 GitOps Workflow Example

```bash
# 1. Make code change and push
git commit -m "feat: add new feature"
git push origin develop

# 2. GitHub Actions automatically:
#    - Builds JAR → Docker image → Pushes to ECR
#    - Updates k8s/overlays/dev/kustomization.yaml with new tag
#    - Commits tag update back to Git

# 3. ArgoCD detects Git change and syncs (auto for staging/prod, manual for dev)

# 4. Verify
argocd app get product-catalog-dev
kubectl get pods -n product-catalog-dev
```

### 4.6 Security Benefits of GitOps

| Aspect | Traditional | GitOps |
|--------|------------|--------|
| Cluster Access | CI has kubectl | Only ArgoCD has kubectl |
| Audit Trail | CI logs | Git history + ArgoCD |
| Drift Detection | None | Automatic |
| Rollback | Manual kubectl | Git revert |

---

## 5. CI/CD Pipeline

### GitHub Actions Workflow (`.github/workflows/deploy.yml`)

**Pipeline stages:**

```
build → security-scan (Trivy) → update-manifest-{env} → notify
```

**Triggers:**
- Push to `develop` → deploys to dev
- Push to `staging` → deploys to staging
- Push to `main` → deploys to production
- Path filter: `src/**`, `Dockerfile`, `build.gradle`, `k8s/**`

---

## 6. Pre-Deployment Checklist

### Application
- [ ] `./gradlew clean build` succeeds
- [ ] All tests pass
- [ ] `application.yml` has `ddl-auto: none` (Liquibase manages schema)
- [ ] SecurityConfig permits `/actuator/**`
- [ ] Health probes enabled (`management.endpoint.health.probes.enabled=true`)

### Docker
- [ ] `docker build` succeeds
- [ ] Image tagged and pushed to ECR
- [ ] Image runs locally with `docker run`

### Kubernetes
- [ ] Secrets created in target namespace
- [ ] ConfigMap values reviewed (especially `SERVER_PORT: "8087"`)
- [ ] Kustomize overlay has correct ECR image reference
- [ ] Ingress annotations match ALB controller configuration
- [ ] Network policies allow egress to RDS

### ArgoCD
- [ ] Application YAML has correct repo URL and branch
- [ ] ArgoCD can access the Git repository
- [ ] Initial sync succeeds

---

## 7. Environment Configuration Reference

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `SERVER_PORT` | Application port | No | 8087 |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | Yes | — |
| `SPRING_DATASOURCE_USERNAME` | DB username | Yes | — |
| `SPRING_DATASOURCE_PASSWORD` | DB password | Yes | — |
| `JWT_SECRET` | JWT signing secret | Yes (prod) | — |
| `JWT_EXPIRATION` | Token expiration (ms) | No | 86400000 |
| `SPRING_PROFILES_ACTIVE` | Active profiles | No | default |
| `JAVA_OPTS` | JVM options | No | Auto-configured |

### Cloud Deployment Targets

The Docker image can deploy to any container platform:

- **AWS:** ECS/Fargate, EKS
- **Azure:** Container Instances, AKS
- **GCP:** Cloud Run, GKE
- **Other:** Heroku, DigitalOcean, Railway, Render

