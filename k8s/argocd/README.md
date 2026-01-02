# ArgoCD GitOps Deployment Guide

## Overview

This project uses ArgoCD for GitOps-based continuous deployment. Each environment (dev, staging, production) has its own ArgoCD Application that monitors the Git repository and automatically syncs changes to the Kubernetes cluster.

## Architecture

```
GitHub Repository (k8s/overlays/*) 
    ↓ 
ArgoCD (monitors & syncs)
    ↓
Kubernetes Cluster (EKS)
```

## Environments & Namespaces

| Environment | Namespace | Branch | ArgoCD App Name | Overlay Path |
|------------|-----------|--------|-----------------|--------------|
| Development | `product-catalog-dev` | `develop` | `product-catalog-dev` | `k8s/overlays/dev` |
| Staging | `product-catalog-staging` | `staging` | `product-catalog-staging` | `k8s/overlays/staging` |
| Production | `product-catalog` | `main` | `product-catalog` | `k8s/overlays/production` |

## Prerequisites

1. **ArgoCD installed in the cluster**
   ```bash
   kubectl create namespace argocd
   kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
   ```

2. **ArgoCD CLI (optional, for manual operations)**
   ```bash
   # macOS
   brew install argocd
   
   # Linux
   curl -sSL -o argocd-linux-amd64 https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
   sudo install -m 555 argocd-linux-amd64 /usr/local/bin/argocd
   ```

3. **Configure Git Repository Access**
   - For public repos: No configuration needed
   - For private repos: Add SSH key or token in ArgoCD settings

## Deployment Steps

### Step 1: Update Repository URLs

Before creating the applications, update the `repoURL` in each application file:

```bash
# Update all application files
sed -i '' 's|https://github.com/your-org/product-catalog.git|https://github.com/YOUR_ORG/YOUR_REPO.git|g' k8s/argocd/application-*.yaml
```

### Step 2: Create ArgoCD Applications

You can create applications individually or all at once:

#### Option A: Create All Environments
```bash
kubectl apply -f k8s/argocd/application-dev.yaml
kubectl apply -f k8s/argocd/application-staging.yaml
kubectl apply -f k8s/argocd/application-prod.yaml
```

#### Option B: Create One Environment at a Time
```bash
# Development only
kubectl apply -f k8s/argocd/application-dev.yaml

# Staging only
kubectl apply -f k8s/argocd/application-staging.yaml

# Production only
kubectl apply -f k8s/argocd/application-prod.yaml
```

### Step 3: Verify Applications

```bash
# List all ArgoCD applications
kubectl get applications -n argocd

# Check application status
argocd app list

# Get detailed status of specific application
argocd app get product-catalog-dev
argocd app get product-catalog-staging
argocd app get product-catalog
```

## ArgoCD Application Configuration

### Development Environment (`application-dev.yaml`)

```yaml
- namespace: product-catalog-dev
- branch: develop
- sync: Manual (requires explicit sync)
- prune: Disabled (safer for dev)
```

**Use Case:** Rapid development and testing with manual control over deployments.

### Staging Environment (`application-staging.yaml`)

```yaml
- namespace: product-catalog-staging
- branch: staging
- sync: Automated with selfHeal
- prune: Disabled (safer for testing)
```

**Use Case:** Pre-production testing with automatic deployment but manual resource cleanup.

### Production Environment (`application-prod.yaml`)

```yaml
- namespace: product-catalog
- branch: main
- sync: Automated with selfHeal and prune
- retries: 5 attempts with exponential backoff
```

**Use Case:** Production deployments with full automation and self-healing capabilities.

## GitOps Workflow

### 1. Code Changes Flow

```
Developer commits code
    ↓
GitHub Actions triggers
    ↓
Build & Test
    ↓
Build Docker Image
    ↓
Push to ECR
    ↓
Update kustomization.yaml (image tag)
    ↓
Commit to Git repository
    ↓
ArgoCD detects change
    ↓
ArgoCD syncs to cluster
    ↓
Deployment complete
```

### 2. Manual Configuration Changes

```
Developer updates k8s/overlays/* files
    ↓
Commit & Push to appropriate branch
    ↓
ArgoCD detects change
    ↓
ArgoCD syncs to cluster
```

## Common Operations

### Manual Sync

Force an immediate sync if auto-sync is disabled:

```bash
# Dev environment
argocd app sync product-catalog-dev

# Staging environment
argocd app sync product-catalog-staging

# Production environment
argocd app sync product-catalog
```

### Check Application Health

```bash
argocd app get product-catalog-dev --refresh
```

### View Application History

```bash
argocd app history product-catalog
```

### Rollback to Previous Version

```bash
# View history first
argocd app history product-catalog

# Rollback to specific revision
argocd app rollback product-catalog <REVISION_NUMBER>
```

### View Real-time Logs

```bash
argocd app logs product-catalog -f
```

### Access ArgoCD UI

```bash
# Port-forward ArgoCD server
kubectl port-forward svc/argocd-server -n argocd 8080:443

# Get admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d

# Open browser
open https://localhost:8080
```

## Troubleshooting

### Application OutOfSync

```bash
# Check what's different
argocd app diff product-catalog-dev

# Force sync
argocd app sync product-catalog-dev --force
```

### Application Degraded

```bash
# Check pods
kubectl get pods -n product-catalog-dev

# Check events
kubectl get events -n product-catalog-dev --sort-by='.lastTimestamp'

# View application logs
argocd app logs product-catalog-dev
```

### Sync Failed

```bash
# View sync operation details
argocd app get product-catalog-dev

# Check ArgoCD logs
kubectl logs -n argocd -l app.kubernetes.io/name=argocd-application-controller
```

## Updating Image Tags

### Automatic (via GitHub Actions)

When you push code to develop/staging/main branches:
1. GitHub Actions builds the image
2. Pushes to ECR with appropriate tag
3. Updates `kustomization.yaml` with new tag
4. Commits the change
5. ArgoCD detects and deploys

### Manual (for testing)

```bash
# Update the image tag in kustomization.yaml
cd k8s/overlays/dev
yq eval -i '.images[0].newTag = "dev-abc123"' kustomization.yaml

# Commit and push
git add kustomization.yaml
git commit -m "chore(dev): update image tag"
git push

# ArgoCD will automatically sync (if auto-sync enabled)
# Or manually sync:
argocd app sync product-catalog-dev
```

## Best Practices

### 1. Branch Strategy
- `develop` → Development environment
- `staging` → Staging environment  
- `main` → Production environment

### 2. Image Tagging
- Development: `dev-<short-sha>`
- Staging: `staging-<short-sha>`
- Production: `latest` or semantic version

### 3. Configuration Changes
- Test in dev first
- Promote to staging via PR
- Promote to production via PR with review

### 4. Rollbacks
- Always use ArgoCD rollback for GitOps compliance
- Avoid manual `kubectl` changes (they will be reverted)

### 5. Secrets Management
- Never commit secrets to Git
- Use Kubernetes Secrets (sealed secrets or external-secrets-operator)
- Manage secrets outside of ArgoCD sync

## Security Considerations

### Repository Access
```bash
# Add private repository credentials
argocd repo add https://github.com/your-org/product-catalog.git \
  --username <username> \
  --password <token>

# Or with SSH
argocd repo add git@github.com:your-org/product-catalog.git \
  --ssh-private-key-path ~/.ssh/id_rsa
```

### RBAC Configuration
```yaml
# Example: Restrict staging access
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: argocd-staging-access
  namespace: product-catalog-staging
subjects:
- kind: ServiceAccount
  name: argocd-application-controller
  namespace: argocd
roleRef:
  kind: Role
  name: argocd-application-controller
  apiGroup: rbac.authorization.k8s.io
```

## Monitoring & Alerts

### Prometheus Metrics
ArgoCD exposes metrics at `/metrics`:
- `argocd_app_sync_total`
- `argocd_app_info`
- `argocd_app_sync_status`

### Health Checks
```bash
# Application health
argocd app get product-catalog --show-operation

# Sync status
argocd app list -o json | jq '.[] | {name:.metadata.name, sync:.status.sync.status, health:.status.health.status}'
```

## Additional Resources

- [ArgoCD Official Documentation](https://argo-cd.readthedocs.io/)
- [Kustomize Documentation](https://kustomize.io/)
- [GitOps Principles](https://opengitops.dev/)

## Support

For issues or questions:
1. Check ArgoCD application status
2. Review ArgoCD controller logs
3. Check the GitHub Actions workflow logs
4. Review this guide for common solutions

