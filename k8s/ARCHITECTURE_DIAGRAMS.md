# GitOps Architecture Diagrams

## Complete GitOps Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           DEVELOPER WORKSTATION                           │
│                                                                           │
│  ┌─────────────────────┐         ┌──────────────────────┐               │
│  │  Write Code         │────────▶│  Git Commit & Push   │               │
│  │  src/**/*.java      │         │  to Branch           │               │
│  └─────────────────────┘         └──────────┬───────────┘               │
│                                              │                            │
└──────────────────────────────────────────────┼────────────────────────────┘
                                               │
                          ┌────────────────────┼────────────────────┐
                          │                    │                    │
                          ▼                    ▼                    ▼
                    ┌──────────┐        ┌──────────┐        ┌──────────┐
                    │ develop  │        │ staging  │        │   main   │
                    │  branch  │        │  branch  │        │  branch  │
                    └─────┬────┘        └─────┬────┘        └─────┬────┘
                          │                   │                    │
┌─────────────────────────┼───────────────────┼────────────────────┼─────────┐
│                         │   GITHUB ACTIONS  │                    │         │
│                         ▼                   ▼                    ▼         │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │                         BUILD JOB                                     │ │
│  │  1. Checkout code                                                     │ │
│  │  2. Setup Java 25                                                     │ │
│  │  3. Build with Gradle (./gradlew build)                              │ │
│  │  4. Run tests                                                         │ │
│  │  5. Generate image tag:                                               │ │
│  │     - develop  → dev-<short-sha>                                      │ │
│  │     - staging  → staging-<short-sha>                                  │ │
│  │     - main     → latest                                               │ │
│  │  6. Build Docker image                                                │ │
│  │  7. Push to ECR: 123456789.dkr.ecr.us-east-1.amazonaws.com/...      │ │
│  └───────────────────────────────┬──────────────────────────────────────┘ │
│                                  │                                         │
│  ┌───────────────────────────────▼──────────────────────────────────────┐ │
│  │                      SECURITY SCAN JOB                                │ │
│  │  1. Run Trivy vulnerability scanner                                   │ │
│  │  2. Upload results to GitHub Security                                 │ │
│  └───────────────────────────────┬──────────────────────────────────────┘ │
│                                  │                                         │
│  ┌───────────────────────────────▼──────────────────────────────────────┐ │
│  │              UPDATE MANIFEST JOB (per environment)                    │ │
│  │  1. Checkout code                                                     │ │
│  │  2. Update k8s/overlays/{env}/kustomization.yaml                     │ │
│  │     - Set images[0].newTag to new image tag                          │ │
│  │  3. Commit updated kustomization.yaml                                │ │
│  │  4. Push back to Git repository                                       │ │
│  └───────────────────────────────┬──────────────────────────────────────┘ │
│                                  │                                         │
└──────────────────────────────────┼─────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          GIT REPOSITORY (Updated)                           │
│                                                                             │
│  k8s/overlays/dev/kustomization.yaml         ← Updated image tag           │
│  k8s/overlays/staging/kustomization.yaml     ← Updated image tag           │
│  k8s/overlays/production/kustomization.yaml  ← Updated image tag           │
│                                                                             │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                                     │ (ArgoCD polls every 3 min)
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ARGOCD                                         │
│                          (Running in EKS)                                   │
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────┐   │
│  │  Application Controller                                             │   │
│  │  - Monitors Git repositories                                        │   │
│  │  - Detects changes in manifests                                     │   │
│  │  - Compares desired state (Git) vs actual state (Cluster)          │   │
│  └────────────────────────────────┬────────────────────────────────────┘   │
│                                   │                                         │
│                    ┌──────────────┼──────────────┐                         │
│                    ▼              ▼              ▼                         │
│         ┌──────────────┐ ┌──────────────┐ ┌──────────────┐               │
│         │ App: dev     │ │ App: staging │ │ App: prod    │               │
│         │ Sync: Manual │ │ Sync: Auto   │ │ Sync: Auto   │               │
│         │ Branch: dev  │ │ Branch: stag │ │ Branch: main │               │
│         └──────┬───────┘ └──────┬───────┘ └──────┬───────┘               │
│                │                 │                 │                        │
└────────────────┼─────────────────┼─────────────────┼────────────────────────┘
                 │                 │                 │
                 │ (Kustomize)     │ (Kustomize)     │ (Kustomize)
                 │                 │                 │
                 ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        KUBERNETES CLUSTER (EKS)                             │
│                                                                             │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────┐ │
│  │ Namespace:           │  │ Namespace:           │  │ Namespace:       │ │
│  │ product-catalog-dev  │  │ product-catalog-     │  │ product-catalog  │ │
│  │                      │  │ staging              │  │                  │ │
│  │ ┌──────────────────┐ │  │ ┌──────────────────┐ │  │ ┌──────────────┐│ │
│  │ │ Deployment       │ │  │ │ Deployment       │ │  │ │ Deployment   ││ │
│  │ │ - Replicas: 1    │ │  │ │ - Replicas: 2    │ │  │ │ - Replicas: 3││ │
│  │ │ - Image: dev-sha │ │  │ │ - Image: stg-sha │ │  │ │ - Image:     ││ │
│  │ └──────────────────┘ │  │ └──────────────────┘ │  │ │   latest     ││ │
│  │                      │  │                      │  │ └──────────────┘│ │
│  │ ┌──────────────────┐ │  │ ┌──────────────────┐ │  │ ┌──────────────┐│ │
│  │ │ Service          │ │  │ │ Service          │ │  │ │ Service      ││ │
│  │ │ ConfigMap        │ │  │ │ ConfigMap        │ │  │ │ ConfigMap    ││ │
│  │ │ Secret           │ │  │ │ Secret           │ │  │ │ Secret       ││ │
│  │ │ HPA              │ │  │ │ HPA              │ │  │ │ HPA          ││ │
│  │ └──────────────────┘ │  │ └──────────────────┘ │  │ └──────────────┘│ │
│  └──────────────────────┘  └──────────────────────┘  └──────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

```
┌────────────────────────────────────────────────────────────────┐
│                    RESPONSIBILITY MATRIX                        │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  DEVELOPER:                                                     │
│  ✓ Write application code                                      │
│  ✓ Commit and push to Git                                      │
│  ✓ (Optional) Update K8s configs for new features             │
│                                                                 │
│  GITHUB ACTIONS (CI):                                          │
│  ✓ Build JAR file                                              │
│  ✓ Run tests                                                    │
│  ✓ Build Docker image                                          │
│  ✓ Push image to ECR                                           │
│  ✓ Security scan                                                │
│  ✓ Update Git with new image tags                             │
│  ✗ Does NOT deploy to Kubernetes                              │
│  ✗ Does NOT run kubectl commands                              │
│                                                                 │
│  ARGOCD (CD):                                                  │
│  ✓ Monitor Git repository                                      │
│  ✓ Detect configuration changes                                │
│  ✓ Render Kustomize overlays                                   │
│  ✓ Apply manifests to cluster                                  │
│  ✓ Perform rolling updates                                     │
│  ✓ Self-heal configuration drift                               │
│  ✓ Automatic retries on failure                                │
│  ✓ Prune deleted resources                                     │
│  ✗ Does NOT build images                                      │
│  ✗ Does NOT run tests                                         │
│                                                                 │
│  KUSTOMIZE:                                                    │
│  ✓ Manage base configurations                                  │
│  ✓ Manage environment overlays                                 │
│  ✓ Patch resources per environment                             │
│  ✓ Set namespace per environment                               │
│  ✓ Override image tags                                         │
│                                                                 │
│  KUBERNETES:                                                   │
│  ✓ Run containerized applications                              │
│  ✓ Manage pods, services, ingress                              │
│  ✓ Handle scaling (HPA)                                        │
│  ✓ Health checks and self-healing                              │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

## Environment Isolation

```
┌─────────────────────────────────────────────────────────────────┐
│                   ENVIRONMENT SEPARATION                         │
└─────────────────────────────────────────────────────────────────┘

Development (product-catalog-dev)
├── Git Branch: develop
├── Image Tag: dev-<commit-sha>
├── Replicas: 1
├── Resources: Low (256Mi RAM, 100m CPU)
├── Sync: Manual (argocd app sync product-catalog-dev)
└── Use: Rapid development, testing, experiments

Staging (product-catalog-staging)
├── Git Branch: staging
├── Image Tag: staging-<commit-sha>
├── Replicas: 2
├── Resources: Medium (384Mi RAM, 200m CPU)
├── Sync: Automated
└── Use: Pre-production testing, QA validation

Production (product-catalog)
├── Git Branch: main
├── Image Tag: latest
├── Replicas: 3+
├── Resources: High (768Mi RAM, 500m CPU)
├── Sync: Automated with prune
├── Security: NetworkPolicy, PodSecurityPolicy
└── Use: Live customer traffic
```

## GitOps Principles Applied

```
┌────────────────────────────────────────────────────────────┐
│              1. DECLARATIVE CONFIGURATION                   │
│  All desired state is declared in Git (k8s/overlays/*)     │
│  No imperative kubectl commands                             │
└────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────┐
│              2. VERSION CONTROL                             │
│  Every change is tracked in Git                            │
│  Full audit trail and history                              │
│  Easy rollback to any previous state                       │
└────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────┐
│              3. AUTOMATED SYNC                              │
│  ArgoCD continuously reconciles Git ↔ Cluster              │
│  No manual intervention needed                             │
│  Self-healing on drift                                     │
└────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────┐
│              4. CONTINUOUS DEPLOYMENT                       │
│  Changes to Git automatically deploy                       │
│  Fast, reliable, repeatable deployments                    │
│  Reduced human error                                       │
└────────────────────────────────────────────────────────────┘
```

## Deployment Timeline

```
Time: 0min - Developer pushes code to 'develop' branch
  │
  ├─ 0-1min: GitHub Actions triggered
  │    ├─ Build JAR
  │    ├─ Run tests
  │    ├─ Build Docker image
  │    └─ Push to ECR as 'dev-abc123'
  │
  ├─ 1-2min: Update manifest
  │    ├─ Update k8s/overlays/dev/kustomization.yaml
  │    └─ Commit back to 'develop' branch
  │
  ├─ 2-5min: ArgoCD detects change (polls every 3min)
  │    ├─ Detects new image tag in kustomization.yaml
  │    ├─ Renders Kustomize overlay
  │    └─ Waits for manual sync (dev is manual)
  │
  ├─ 5min+: Developer runs manual sync
  │    └─ argocd app sync product-catalog-dev
  │
  ├─ 5-7min: ArgoCD applies changes
  │    ├─ Updates Deployment with new image
  │    ├─ K8s starts new pods
  │    ├─ Rolling update (1 replica for dev)
  │    └─ Old pods terminated after new ones ready
  │
  └─ 7-10min: Deployment complete
       └─ New version running in product-catalog-dev namespace
```

## Security Model

```
┌────────────────────────────────────────────────────────────────┐
│                     SECURITY BOUNDARIES                         │
└────────────────────────────────────────────────────────────────┘

GitHub Repository (Git)
├── Authentication: GitHub OAuth/SSH
├── Authorization: Branch protection, required reviews
└── Secrets: None! (Secrets in K8s only)
                ↓
GitHub Actions (CI)
├── AWS Credentials: OIDC (role-to-assume)
├── ECR Push: Via AWS IAM role
└── Git Push: Via GITHUB_TOKEN
                ↓
ECR (Container Registry)
├── Authentication: AWS IAM
├── Encryption: At rest
└── Scanning: Trivy vulnerability scan
                ↓
ArgoCD (CD)
├── Authentication: K8s RBAC
├── Git Access: SSH key or token
├── Cluster Access: K8s ServiceAccount
└── No direct kubectl access needed
                ↓
Kubernetes (EKS)
├── Namespace isolation
├── RBAC policies
├── Network policies (production)
├── Pod security policies (production)
└── Secrets encrypted at rest
```

## Observability

```
┌────────────────────────────────────────────────────────────────┐
│                      MONITORING POINTS                          │
└────────────────────────────────────────────────────────────────┘

GitHub Actions
├── Build logs
├── Test results
├── Security scan results
└── Workflow status

ArgoCD
├── Sync status (Synced/OutOfSync)
├── Health status (Healthy/Progressing/Degraded)
├── Deployment history
├── Resource details
└── Metrics (Prometheus format)

Kubernetes
├── Pod status
├── Deployment rollout status
├── HPA metrics
├── Resource utilization
├── Events
└── Application logs

Application
├── Health endpoint: /actuator/health
├── Metrics endpoint: /actuator/prometheus
├── Liveness probe
└── Readiness probe
```

---

**This architecture implements GitOps best practices with clear separation of concerns:**
- **GitHub Actions** = Continuous Integration (CI)
- **ArgoCD** = Continuous Deployment (CD)
- **Git** = Single Source of Truth
- **Kustomize** = Configuration Management

