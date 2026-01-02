# GitOps Quick Reference Card

## 🚀 Initial Setup (One-time)

```bash
# 1. Install ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 2. Update repository URLs in application files
cd k8s/argocd
sed -i '' 's|your-org/product-catalog.git|YOUR_ORG/YOUR_REPO.git|g' application-*.yaml

# 3. Create ArgoCD applications
kubectl apply -f k8s/argocd/application-dev.yaml
kubectl apply -f k8s/argocd/application-staging.yaml
kubectl apply -f k8s/argocd/application-prod.yaml

# 4. Initial sync
argocd app sync product-catalog-dev
argocd app sync product-catalog-staging
argocd app sync product-catalog
```

## 📦 Deployment Process

### Developer Workflow
```bash
# 1. Write code
vim src/main/java/com/product/YourCode.java

# 2. Commit and push
git add .
git commit -m "feat: new feature"
git push origin develop  # or staging/main

# 3. Done! GitHub Actions + ArgoCD handle the rest
```

### What Happens Automatically
```
Push Code → GitHub Actions → Build → Push to ECR → Update Git → ArgoCD → Deploy
```

## 🎛️ Common Commands

### Check Status
```bash
# List all applications
argocd app list

# Get application details
argocd app get product-catalog-dev

# Check sync status
argocd app get product-catalog-dev --refresh
```

### Manual Operations
```bash
# Manual sync (dev environment)
argocd app sync product-catalog-dev

# Force sync (override)
argocd app sync product-catalog-dev --force

# Sync specific resource
argocd app sync product-catalog-dev --resource apps:Deployment:product-catalog
```

### Rollback
```bash
# View history
argocd app history product-catalog

# Rollback to previous version
argocd app rollback product-catalog <REVISION_NUMBER>
```

### Logs & Debugging
```bash
# View application logs
argocd app logs product-catalog-dev -f

# Check pod status
kubectl get pods -n product-catalog-dev

# View events
kubectl get events -n product-catalog-dev --sort-by='.lastTimestamp'
```

## 🏗️ Architecture Overview

### Environments & Namespaces
| Env | Namespace | Branch | App Name | Sync |
|-----|-----------|--------|----------|------|
| Dev | `product-catalog-dev` | `develop` | `product-catalog-dev` | Manual |
| Staging | `product-catalog-staging` | `staging` | `product-catalog-staging` | Auto |
| Production | `product-catalog` | `main` | `product-catalog` | Auto |

### Image Tags
- **Dev:** `123456789.dkr.ecr.us-east-1.amazonaws.com/product-catalog:dev-<sha>`
- **Staging:** `123456789.dkr.ecr.us-east-1.amazonaws.com/product-catalog:staging-<sha>`
- **Production:** `123456789.dkr.ecr.us-east-1.amazonaws.com/product-catalog:latest`

## 🔧 Configuration Updates

### Update Application Config (e.g., replicas)
```bash
# Edit overlay
vim k8s/overlays/dev/kustomization.yaml

# Commit and push
git add k8s/overlays/dev/kustomization.yaml
git commit -m "chore: update dev replicas"
git push origin develop

# For dev, manually sync
argocd app sync product-catalog-dev

# For staging/prod, automatic after push
```

### Update Image Tag (Manually)
```bash
cd k8s/overlays/dev

# Update image tag
yq eval -i '.images[0].newTag = "dev-abc123"' kustomization.yaml

# Commit and push
git add kustomization.yaml
git commit -m "chore: update image tag"
git push origin develop

# Sync
argocd app sync product-catalog-dev
```

## 🔐 Access ArgoCD UI

```bash
# Port-forward
kubectl port-forward svc/argocd-server -n argocd 8080:443

# Get password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d

# Open browser
open https://localhost:8080
# Login: admin / <password-from-above>
```

## 🆘 Troubleshooting

### Application OutOfSync
```bash
# Check differences
argocd app diff product-catalog-dev

# Sync
argocd app sync product-catalog-dev
```

### Application Degraded
```bash
# Check pods
kubectl get pods -n product-catalog-dev
kubectl describe pod <pod-name> -n product-catalog-dev

# Check logs
kubectl logs <pod-name> -n product-catalog-dev

# Check events
kubectl get events -n product-catalog-dev --sort-by='.lastTimestamp'
```

### Sync Failed
```bash
# Get detailed status
argocd app get product-catalog-dev

# Check ArgoCD logs
kubectl logs -n argocd -l app.kubernetes.io/name=argocd-application-controller --tail=100
```

### Image Pull Errors
```bash
# Check if image exists in ECR
aws ecr describe-images --repository-name product-catalog --region us-east-1

# Check image pull secrets
kubectl get secrets -n product-catalog-dev
kubectl describe secret <image-pull-secret> -n product-catalog-dev
```

## 📊 Monitoring

### Application Health
```bash
# All apps health
argocd app list -o json | jq '.[] | {name:.metadata.name, sync:.status.sync.status, health:.status.health.status}'

# Specific app
argocd app get product-catalog-dev -o json | jq '{sync:.status.sync.status, health:.status.health.status}'
```

### Resource Status
```bash
# Deployments
kubectl get deployments -n product-catalog-dev

# Services
kubectl get services -n product-catalog-dev

# Ingress
kubectl get ingress -n product-catalog-dev

# HPA
kubectl get hpa -n product-catalog-dev
```

## 🔄 Promotion Workflow

### Dev → Staging
```bash
# Merge develop into staging
git checkout staging
git merge develop
git push origin staging

# ArgoCD auto-syncs staging environment
```

### Staging → Production
```bash
# Merge staging into main
git checkout main
git merge staging
git push origin main

# ArgoCD auto-syncs production environment
```

## ⚡ Emergency Operations

### Pause Sync (Emergency)
```bash
# Disable auto-sync temporarily
argocd app set product-catalog --sync-policy none

# Re-enable when ready
argocd app set product-catalog --sync-policy automated
```

### Force Refresh
```bash
# Hard refresh application
argocd app get product-catalog --hard-refresh
```

### Delete and Recreate
```bash
# Delete application (keeps resources in cluster)
argocd app delete product-catalog --cascade=false

# Recreate
kubectl apply -f k8s/argocd/application-prod.yaml

# Sync
argocd app sync product-catalog
```

## 📝 Best Practices

1. ✅ **Always test in dev first**
2. ✅ **Use PRs for staging/production changes**
3. ✅ **Never use kubectl to modify resources directly** (ArgoCD will revert)
4. ✅ **Keep secrets out of Git** (use Sealed Secrets or External Secrets)
5. ✅ **Monitor ArgoCD sync status** regularly
6. ✅ **Use semantic versioning** for production images
7. ✅ **Tag production releases** in Git
8. ✅ **Document configuration changes** in commit messages

## 🔗 Resources

- [Full Deployment Guide](./GITOPS_DEPLOYMENT_GUIDE.md)
- [Architecture Analysis](./GITOPS_ANALYSIS.md)
- [ArgoCD Operations](./argocd/README.md)
- [ArgoCD Docs](https://argo-cd.readthedocs.io/)

---
**Remember:** Git is the source of truth. All changes must go through Git!

