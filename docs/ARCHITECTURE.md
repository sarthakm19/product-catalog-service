# Architecture

Application and infrastructure architecture for the Product Catalog Service.

---

## Table of Contents

1. [Application Architecture](#1-application-architecture)
2. [Request/Response Flow](#2-requestresponse-flow)
3. [Security Architecture](#3-security-architecture)
4. [Infrastructure Architecture](#4-infrastructure-architecture)
5. [GitOps Deployment Flow](#5-gitops-deployment-flow)
6. [Design Patterns](#6-design-patterns)

---

## 1. Application Architecture

### Layered Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     Client (Browser / Mobile / CLI)          │
└───────────────────────────┬──────────────────────────────────┘
                            │ HTTP/HTTPS + JWT
┌───────────────────────────▼──────────────────────────────────┐
│  Security Layer (JwtAuthenticationFilter)                     │
│  Validates JWT → Sets SecurityContext                         │
├──────────────────────────────────────────────────────────────┤
│  Controller Layer                                            │
│  AuthController (/auth/**)   ProductController (/products/**)│
│  Receives/returns DTOs       @Valid request validation        │
├──────────────────────────────────────────────────────────────┤
│  Mapper Layer (MapStruct)                                    │
│  DTO ↔ Domain ↔ Entity transformations                       │
├──────────────────────────────────────────────────────────────┤
│  Service Layer                                               │
│  ProductServiceImpl: business rules, validation, txns        │
│  AuthServiceImpl: authentication, token generation           │
├──────────────────────────────────────────────────────────────┤
│  Repository Layer (Spring Data JPA)                          │
│  ProductRepository  CategoryRepository  CatalogRepository    │
├──────────────────────────────────────────────────────────────┤
│  PostgreSQL (local Docker / AWS RDS)                         │
│  Schema managed by Liquibase                                 │
└──────────────────────────────────────────────────────────────┘
```

### Cross-Cutting Concerns

| Concern | Implementation |
|---------|---------------|
| Exception Handling | `GlobalExceptionHandler` with consistent `ErrorResponse` |
| Logging | SLF4J + Logback (structured JSON in K8s) |
| Validation | Jakarta Bean Validation (`@Valid`) |
| API Docs | SpringDoc OpenAPI → Swagger UI |
| Metrics | Spring Actuator + Prometheus endpoint |

---

## 2. Request/Response Flow

### Creating a Product (POST /api/v1/products)

```
Client
  │ POST + JWT Token
  ▼
JwtAuthenticationFilter
  │ Validate token → Set SecurityContext
  ▼
ProductController
  │ Receive CreateProductRequest DTO → @Valid
  ▼
ProductMapper
  │ CreateProductRequest → ProductDomain
  ▼
ProductServiceImpl
  │ Validate business rules
  │ Resolve Category & Catalog references
  │ @Transactional
  ▼
ProductMapper
  │ ProductDomain → Product Entity
  ▼
ProductRepository.save()
  │ JPA → PostgreSQL
  ▼
ProductMapper
  │ Product Entity → ProductDomain → ProductResponse DTO
  ▼
ProductController
  │ Return ResponseEntity<ProductResponse> (HTTP 201)
  ▼
Client receives JSON response
```

---

## 3. Security Architecture

### JWT Authentication Flow

```
Client Request
  │ Header: Authorization: Bearer <JWT>
  ▼
JwtAuthenticationFilter
  ├─ Extract token from header
  ├─ Verify signature (HMAC-SHA256)
  ├─ Check expiration
  ├─ Extract username (sub claim)
  ├─ Load UserDetails
  └─ Set Authentication in SecurityContext
  ▼
SecurityConfig
  ├─ Public:  /auth/**, /actuator/**, /swagger-ui/**, /v3/api-docs/**
  └─ Protected: /api/v1/products/** (requires valid JWT)
  ▼
Controller handles request
```

### JWT Token Structure

```
Header:  { "alg": "HS256", "typ": "JWT" }
Payload: { "sub": "admin", "iat": 1702556400, "exp": 1702642800 }
Signature: HMACSHA256(header.payload, secret)
```

### Security Properties

| Property | Value |
|----------|-------|
| Algorithm | HS256 |
| Expiration | 24 hours (configurable) |
| Session | Stateless |
| Password Encoding | BCrypt |
| User Store | In-memory (production: swap to DB) |

---

## 4. Infrastructure Architecture

### Production Deployment (AWS EKS)

```
┌─────────────────────────────────────────────────────────────────────┐
│                            AWS Cloud                                │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                    VPC                                       │   │
│  │                                                              │   │
│  │  ┌─────────────────┐     ┌──────────────────────────────┐   │   │
│  │  │  Public Subnets  │     │  Private Subnets              │   │   │
│  │  │                 │     │                              │   │   │
│  │  │  ALB (Ingress)  │────▶│  EKS Worker Nodes            │   │   │
│  │  │                 │     │  ┌────────────────────────┐  │   │   │
│  │  └─────────────────┘     │  │ product-catalog NS     │  │   │   │
│  │                          │  │                        │  │   │   │
│  │                          │  │  Pod 1  Pod 2  Pod 3   │  │   │   │
│  │                          │  │  (HPA: 3-10 replicas)  │  │   │   │
│  │                          │  └───────────┬────────────┘  │   │   │
│  │                          │              │               │   │   │
│  │                          │  ┌───────────▼────────────┐  │   │   │
│  │                          │  │  RDS PostgreSQL        │  │   │   │
│  │                          │  │  (Private subnet)      │  │   │   │
│  │                          │  └────────────────────────┘  │   │   │
│  │                          └──────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌────────────┐  ┌─────────────────┐  ┌──────────────────────┐     │
│  │    ECR     │  │ Secrets Manager │  │  IAM (IRSA)          │     │
│  │  (Images)  │  │  (DB creds/JWT) │  │  (Service Accounts)  │     │
│  └────────────┘  └─────────────────┘  └──────────────────────┘     │
└─────────────────────────────────────────────────────────────────────┘
```

### Kubernetes Resources

| Resource | Purpose |
|----------|---------|
| Namespace | `product-catalog` (per environment) |
| Deployment | 3 replicas, rolling update, pod anti-affinity |
| Service | ClusterIP for internal routing |
| Ingress | AWS ALB with health checks |
| ConfigMap | Non-sensitive config (ports, logging, HikariCP) |
| Secret | DB credentials, JWT secret |
| HPA | Auto-scale 3→10 pods (CPU/Memory) |
| PDB | Min 2 pods available during disruptions |
| NetworkPolicy | Restrict ingress/egress in production |
| ServiceAccount | IRSA for AWS API access |
| ServiceMonitor | Prometheus metric scraping |

### Health Probes

| Probe | Path | Purpose |
|-------|------|---------|
| Startup | `/actuator/health` | Wait for app initialization (up to 280s) |
| Liveness | `/actuator/health/liveness` | Restart if deadlocked |
| Readiness | `/actuator/health/readiness` | Remove from LB if unhealthy |

### Container Security

- Non-root execution (UID 1000)
- All capabilities dropped (only `NET_BIND_SERVICE` added)
- No privilege escalation
- Pod Security Standards: `restricted`
- seccomp profile: `RuntimeDefault`

---

## 5. GitOps Deployment Flow

```
Developer pushes code
       │
       ▼
GitHub Actions (CI)
  ├─ Build JAR
  ├─ Run tests
  ├─ Build Docker image
  ├─ Push to ECR
  ├─ Security scan (Trivy)
  ├─ Update image tag in k8s/overlays/{env}/kustomization.yaml
  └─ Commit tag update to Git
       │
       ▼
Git Repository (source of truth)
       │
       ▼ (polls every 3 min)
ArgoCD (CD)
  ├─ Detects manifest change
  ├─ Renders Kustomize (base + overlay)
  ├─ Compares desired state vs cluster state
  ├─ Applies changes to EKS
  └─ Self-heals configuration drift
       │
       ▼
EKS Cluster (updated)
```

### Environment Pipeline

| Environment | Branch | ArgoCD Sync | Promotion |
|-------------|--------|-------------|-----------|
| Dev | `develop` | Manual | Merge PR to staging |
| Staging | `staging` | Auto + SelfHeal | Merge PR to main |
| Production | `main` | Auto + SelfHeal + Prune | — |

### Kustomize Structure

```
k8s/
├── base/                    # Shared resources
│   ├── deployment.yaml      # 3 replicas, probes, security context
│   ├── service.yaml         # ClusterIP
│   ├── configmap.yaml       # Application config
│   ├── secret.yaml          # Secret template
│   ├── namespace.yaml       # Namespace + PSS labels
│   ├── rbac.yaml            # ServiceAccount, Roles
│   ├── hpa.yaml             # Horizontal Pod Autoscaler
│   ├── pdb.yaml             # Pod Disruption Budget
│   └── kustomization.yaml   # Base kustomization
├── overlays/
│   ├── dev/                 # 1 pod, DEBUG, relaxed
│   ├── staging/             # 2 pods, INFO, moderate
│   └── production/          # 3+ pods, WARN, strict + NetworkPolicy
└── argocd/                  # ArgoCD Application manifests
```

---

## 6. Design Patterns

| Pattern | Usage |
|---------|-------|
| **Layered Architecture** | Controller → Service → Repository |
| **DTO Pattern** | Separate API contracts from domain/entities |
| **Repository Pattern** | Spring Data JPA abstracts data access |
| **Service Pattern** | Business logic encapsulated in services |
| **Mapper Pattern** | MapStruct for type-safe layer transformations |
| **Dependency Injection** | Constructor-based, interface-driven |
| **Strategy Pattern** | JWT authentication mechanism |
| **Chain of Responsibility** | Spring Security filter chain |
| **Builder Pattern** | Lombok `@Builder` for DTOs |
| **Factory Pattern** | MapStruct-generated mapper implementations |

### SOLID Principles

| Principle | Application |
|-----------|------------|
| **SRP** | Controllers handle HTTP; Services handle logic; Repos handle data |
| **OCP** | Service interfaces allow extension without modification |
| **LSP** | `ProductService` implementations are substitutable |
| **ISP** | Small, focused interfaces per concern |
| **DIP** | Depend on abstractions (`ProductService`, not `ProductServiceImpl`) |
