# GitOps Deployment Summary - Product Catalog Service

## ✅ What Was Updated

### 1. ArgoCD Application Files
Created separate ArgoCD applications for each environment:

- **`k8s/argocd/application-dev.yaml`**
  - App Name: `product-catalog-dev`
  - Namespace: `product-catalog-dev`
  - Branch: `develop`
  - Sync: Manual (requires explicit sync command)
  
- **`k8s/argocd/application-staging.yaml`**
  - App Name: `product-catalog-staging`
  - Namespace: `product-catalog-staging`
  - Branch: `staging`
  - Sync: Automated with selfHeal
  
- **`k8s/argocd/application-prod.yaml`**
  - App Name: `product-catalog`
  - Namespace: `product-catalog`
  - Branch: `main`
  - Sync: Automated with selfHeal + prune

### 2. GitHub Actions Workflow
Updated `.github/workflows/deploy.yml`:

**Removed:**
- ❌ Direct `kubectl apply` commands
- ❌ Manual `kubectl set image` operations
- ❌ AWS EKS kubeconfig setup in deployment jobs
- ❌ Manual rollout status checks

**Kept:**
- ✅ Build and test jobs
- ✅ Docker image build and push to ECR
- ✅ Security scanning with Trivy

**Added:**
- ✅ Update kustomization.yaml with new image tags
- ✅ Commit changes back to Git
- ✅ Optional ArgoCD sync triggering

### 3. Namespace Configuration
Already correctly configured! Each environment has its own namespace:
- Dev: `product-catalog-dev`
- Staging: `product-catalog-staging`
- Production: `product-catalog`

### 4. Kustomize Configuration
Updated `k8s/base/kustomization.yaml` to use correct ECR registry URL matching the workflow.

## 📋 Deployment Workflow

### Current GitOps Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     Developer Workflow                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ Commit Code      │
                    │ to Git Branch    │
                    └────────┬─────────┘
                              │
                ┏━━━━━━━━━━━━━┻━━━━━━━━━━━━━┓
                ▼             ▼              ▼
          [develop]      [staging]       [main]
                              
┌─────────────────────────────────────────────────────────────────┐
│                     GitHub Actions (CI)                          │
│  1. Build JAR with Gradle                                        │
│  2. Run Tests                                                    │
│  3. Build Docker Image                                           │
│  4. Push to ECR with tag (dev-<sha>/staging-<sha>/latest)      │
│  5. Run Security Scan                                            │
│  6. Update k8s/overlays/*/kustomization.yaml (image tag)       │
│  7. Commit updated kustomization.yaml                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Git Repository                               │
│  k8s/overlays/{dev|staging|production}/kustomization.yaml       │
│  (Updated with new image tags)                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     ArgoCD (Continuous Deployment)               │
│  - Monitors Git repository (polling every 3 minutes)             │
│  - Detects changes in kustomization.yaml                         │
│  - Renders Kustomize overlays                                    │
│  - Compares desired state (Git) vs actual state (Cluster)       │
│  - Applies changes to cluster (if auto-sync enabled)            │
│  - Self-heals drift (if selfHeal enabled)                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Kubernetes Cluster (EKS)                       │
│  Namespaces:                                                     │
│  - product-catalog-dev                                           │
│  - product-catalog-staging                                       │
│  - product-catalog                                               │
└─────────────────────────────────────────────────────────────────┘
```

## 🎯 Your Understanding is Correct!

### What You Do:
1. ✅ **Write code** and commit to Git
2. ✅ **GitHub Actions** builds and pushes image to ECR
3. ✅ **GitHub Actions** updates Git with new image tag (optional)
4. ✅ **Register ArgoCD application** (one-time setup)

### What ArgoCD Does Automatically:
1. ✅ **Monitors** the Git repository
2. ✅ **Detects** changes in manifests
3. ✅ **Syncs** to Kubernetes cluster
4. ✅ **Self-heals** if someone manually changes resources
5. ✅ **Retries** on failures
6. ✅ **Prunes** removed resources (if enabled)

## 🚀 Getting Started

### Step 1: Setup ArgoCD (One-time)

```bash
# Install ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Access ArgoCD UI
kubectl port-forward svc/argocd-server -n argocd 8080:443

# Get admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
```

### Step 2: Update Repository URLs

Before creating applications, update the Git repository URL in all application files:

```bash
cd k8s/argocd

# Update with your actual repository URL
sed -i '' 's|https://github.com/your-org/product-catalog.git|https://github.com/YOUR_ACTUAL_ORG/YOUR_ACTUAL_REPO.git|g' application-*.yaml
```

### Step 3: Create ArgoCD Applications

```bash
# Create all environments
kubectl apply -f k8s/argocd/application-dev.yaml
kubectl apply -f k8s/argocd/application-staging.yaml
kubectl apply -f k8s/argocd/application-prod.yaml

# Verify
kubectl get applications -n argocd
```

### Step 4: Initial Sync

```bash
# Dev (manual sync required)
argocd app sync product-catalog-dev

# Staging (auto-sync enabled, but you can force it)
argocd app sync product-catalog-staging

# Production (auto-sync enabled, but you can force it)
argocd app sync product-catalog
```

## ✓ Kustomize with ArgoCD - Best Practice

**Yes, using Kustomize with ArgoCD makes perfect sense!**

### Why?
1. **Kustomize** manages environment-specific configurations (DRY principle)
2. **ArgoCD** manages the deployment lifecycle and GitOps workflow
3. **ArgoCD natively supports Kustomize** - no pre-rendering needed
4. **Clean separation** of concerns:
   - Kustomize = Configuration management
   - ArgoCD = Deployment automation

### How It Works:
```yaml
# ArgoCD Application
source:
  path: k8s/overlays/dev  # ArgoCD knows this is Kustomize
  
# ArgoCD will automatically:
# 1. Run: kustomize build k8s/overlays/dev
# 2. Apply the rendered manifests to cluster
# 3. Monitor for drift and self-heal
```

## 📝 Key Changes Summary

### Before (Traditional CI/CD):
```yaml
GitHub Actions:
  - Build image
  - Push to ECR
  - Configure kubectl
  - Apply manifests directly to cluster ❌
  - Hope everything works
```

### After (GitOps):
```yaml
GitHub Actions:
  - Build image
  - Push to ECR
  - Update Git repository ✅
  
ArgoCD:
  - Monitor Git
  - Sync to cluster automatically ✅
  - Self-heal on drift ✅
  - Provide rollback capability ✅
```

## 🔒 Security Benefits

1. **No cluster credentials in GitHub Actions** (for deployment)
2. **Audit trail** - All changes tracked in Git
3. **Declarative** - Desired state in Git
4. **Self-healing** - Prevents configuration drift
5. **RBAC** - Fine-grained access control via ArgoCD

## 🎛️ Control & Flexibility

### Auto-Sync vs Manual Sync

**Development (Manual):**
- Good for testing changes
- Explicit sync required
```bash
argocd app sync product-catalog-dev
```

**Staging/Production (Auto-Sync):**
- Automatic deployment on Git changes
- Can disable temporarily if needed
```bash
argocd app set product-catalog --sync-policy none  # Disable
argocd app set product-catalog --sync-policy automated  # Enable
```

## 🔄 Rollback Strategy

```bash
# View deployment history
argocd app history product-catalog

# Rollback to previous version
argocd app rollback product-catalog <REVISION_NUMBER>

# Or rollback to specific Git commit
argocd app sync product-catalog --revision <GIT_COMMIT_SHA>
```

## 📊 Monitoring

### Check Application Status
```bash
# List all apps
argocd app list

# Get app details
argocd app get product-catalog-dev

# Watch sync progress
argocd app sync product-catalog-dev --watch
```

### Health Checks
```bash
# Application health
kubectl get applications -n argocd

# Pod health in namespace
kubectl get pods -n product-catalog-dev
kubectl get pods -n product-catalog-staging
kubectl get pods -n product-catalog
```

## 🧪 Testing the Setup

### 1. Test Dev Deployment
```bash
# Make a change to dev overlay
cd k8s/overlays/dev
# Edit kustomization.yaml or patches
git add .
git commit -m "test: update dev config"
git push origin develop

# Manually sync (since dev is manual)
argocd app sync product-catalog-dev

# Watch the deployment
kubectl rollout status deployment/product-catalog -n product-catalog-dev
```

### 2. Test Image Update
```bash
# Trigger GitHub Actions by pushing code
git checkout develop
echo "// test change" >> src/main/java/com/product/SomeFile.java
git add .
git commit -m "test: trigger pipeline"
git push origin develop

# GitHub Actions will:
# 1. Build new image
# 2. Push to ECR as dev-<sha>
# 3. Update k8s/overlays/dev/kustomization.yaml
# 4. Commit the change

# ArgoCD will (after you sync):
# 1. Detect the Git change
# 2. Apply the new image tag
# 3. Rolling update pods
```

## 📚 Additional Documentation

- [GITOPS_ANALYSIS.md](./GITOPS_ANALYSIS.md) - Detailed architecture analysis
- [README.md](./README.md) - ArgoCD operations guide
- [../KUBERNETES_START_HERE.md](../KUBERNETES_START_HERE.md) - K8s setup guide

## 🎉 Conclusion

Your setup is now properly configured for GitOps with ArgoCD:

✅ Separate namespaces per environment
✅ Dedicated ArgoCD applications per environment  
✅ GitHub Actions for CI (build & push)
✅ ArgoCD for CD (deploy & sync)
✅ Kustomize for configuration management
✅ Git as the single source of truth

**You were absolutely correct** - in GitOps, you only push images to ECR and register the Git repository with ArgoCD. The rest is automatic! 🚀

