# 🚀 Deployment Checklist

## Pre-Deployment Setup

### 1. ArgoCD Installation
- [ ] Install ArgoCD in the cluster
  ```bash
  kubectl create namespace argocd
  kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
  ```

- [ ] Wait for ArgoCD to be ready
  ```bash
  kubectl wait --for=condition=available --timeout=300s deployment/argocd-server -n argocd
  ```

- [ ] Get ArgoCD admin password
  ```bash
  kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
  ```

- [ ] (Optional) Install ArgoCD CLI
  ```bash
  # macOS
  brew install argocd
  
  # Linux
  curl -sSL -o argocd-linux-amd64 https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
  sudo install -m 555 argocd-linux-amd64 /usr/local/bin/argocd
  ```

### 2. Update Configuration Files

- [ ] Update ECR registry in `k8s/base/kustomization.yaml`
  ```bash
  # Replace with your AWS account ID
  sed -i '' 's|123456789|YOUR_AWS_ACCOUNT_ID|g' k8s/base/kustomization.yaml
  ```

- [ ] Update ECR registry in `.github/workflows/deploy.yml`
  ```bash
  # Update REGISTRY in env section
  sed -i '' 's|123456789|YOUR_AWS_ACCOUNT_ID|g' .github/workflows/deploy.yml
  ```

- [ ] Update Git repository URLs in ArgoCD applications
  ```bash
  cd k8s/argocd
  sed -i '' 's|your-org/product-catalog.git|YOUR_GITHUB_ORG/YOUR_REPO.git|g' application-*.yaml
  ```

### 3. GitHub Secrets Configuration

- [ ] Add AWS role to GitHub secrets: `AWS_ROLE_TO_ASSUME`
  - Go to GitHub → Settings → Secrets and variables → Actions
  - Add: `arn:aws:iam::YOUR_ACCOUNT_ID:role/github-actions-role`

- [ ] (Optional) Add ArgoCD secrets if using manual sync trigger
  - `ARGOCD_SERVER`: ArgoCD server URL
  - `ARGOCD_AUTH_TOKEN`: ArgoCD authentication token

### 4. AWS Configuration

- [ ] Create ECR repository
  ```bash
  aws ecr create-repository \
    --repository-name product-catalog \
    --region us-east-1
  ```

- [ ] Configure IAM role for GitHub Actions (OIDC)
  - Create role with ECR push permissions
  - Configure trust policy for GitHub OIDC

- [ ] Verify EKS cluster access for ArgoCD
  ```bash
  kubectl auth can-i '*' '*' --all-namespaces --as system:serviceaccount:argocd:argocd-application-controller
  ```

## Deployment Steps

### Phase 1: Development Environment

- [ ] Create dev ArgoCD application
  ```bash
  kubectl apply -f k8s/argocd/application-dev.yaml
  ```

- [ ] Verify application is created
  ```bash
  kubectl get application product-catalog-dev -n argocd
  argocd app get product-catalog-dev
  ```

- [ ] Initial sync (manual for dev)
  ```bash
  argocd app sync product-catalog-dev
  ```

- [ ] Wait for sync to complete
  ```bash
  argocd app wait product-catalog-dev --sync
  ```

- [ ] Verify deployment
  ```bash
  kubectl get all -n product-catalog-dev
  kubectl get pods -n product-catalog-dev
  ```

- [ ] Check application health
  ```bash
  argocd app get product-catalog-dev
  kubectl get pods -n product-catalog-dev -o wide
  ```

- [ ] Test the application endpoint
  ```bash
  kubectl port-forward svc/product-catalog -n product-catalog-dev 8080:8087
  curl http://localhost:8080/actuator/health
  ```

### Phase 2: Staging Environment

- [ ] Create staging ArgoCD application
  ```bash
  kubectl apply -f k8s/argocd/application-staging.yaml
  ```

- [ ] Verify application is created
  ```bash
  kubectl get application product-catalog-staging -n argocd
  argocd app get product-catalog-staging
  ```

- [ ] Sync (auto-sync enabled, but can force)
  ```bash
  argocd app sync product-catalog-staging
  ```

- [ ] Verify deployment
  ```bash
  kubectl get all -n product-catalog-staging
  kubectl get pods -n product-catalog-staging
  ```

- [ ] Check application health
  ```bash
  argocd app get product-catalog-staging
  kubectl get hpa -n product-catalog-staging
  ```

### Phase 3: Production Environment

- [ ] **Review all configs before production deployment!**

- [ ] Create production ArgoCD application
  ```bash
  kubectl apply -f k8s/argocd/application-prod.yaml
  ```

- [ ] Verify application is created
  ```bash
  kubectl get application product-catalog -n argocd
  argocd app get product-catalog
  ```

- [ ] Sync (auto-sync enabled, but can force)
  ```bash
  argocd app sync product-catalog
  ```

- [ ] Verify deployment
  ```bash
  kubectl get all -n product-catalog
  kubectl get pods -n product-catalog -o wide
  ```

- [ ] Verify HPA and scaling
  ```bash
  kubectl get hpa -n product-catalog
  kubectl top pods -n product-catalog
  ```

- [ ] Verify network policies (production only)
  ```bash
  kubectl get networkpolicy -n product-catalog
  ```

- [ ] Check ingress
  ```bash
  kubectl get ingress -n product-catalog
  ```

## Post-Deployment Verification

### 1. ArgoCD Status Check

- [ ] All applications are healthy
  ```bash
  argocd app list
  ```

- [ ] No sync errors
  ```bash
  argocd app get product-catalog-dev
  argocd app get product-catalog-staging
  argocd app get product-catalog
  ```

### 2. Kubernetes Resources Check

- [ ] All pods are running
  ```bash
  kubectl get pods -n product-catalog-dev
  kubectl get pods -n product-catalog-staging
  kubectl get pods -n product-catalog
  ```

- [ ] Services are available
  ```bash
  kubectl get svc -n product-catalog-dev
  kubectl get svc -n product-catalog-staging
  kubectl get svc -n product-catalog
  ```

- [ ] ConfigMaps and Secrets exist
  ```bash
  kubectl get configmap -n product-catalog-dev
  kubectl get secrets -n product-catalog-dev
  ```

### 3. Application Health Check

- [ ] Health endpoints respond
  ```bash
  # Port-forward and test each environment
  kubectl port-forward svc/product-catalog -n product-catalog-dev 8080:8087
  curl http://localhost:8080/actuator/health
  ```

- [ ] Metrics endpoints available
  ```bash
  curl http://localhost:8080/actuator/prometheus
  ```

- [ ] Database connectivity
  ```bash
  kubectl logs -n product-catalog-dev -l app=product-catalog --tail=50 | grep -i database
  ```

## Testing the GitOps Workflow

### 1. Test Image Update (Dev)

- [ ] Make a code change
  ```bash
  echo "// test" >> src/main/java/com/product/ProductCatalogServiceApplication.java
  ```

- [ ] Commit and push to develop
  ```bash
  git add .
  git commit -m "test: trigger GitOps workflow"
  git push origin develop
  ```

- [ ] Monitor GitHub Actions
  - Go to GitHub → Actions
  - Watch the workflow run
  - Verify build, test, push to ECR complete
  - Verify kustomization.yaml update

- [ ] Wait for ArgoCD to detect (or manually sync)
  ```bash
  # Check sync status
  argocd app get product-catalog-dev
  
  # Manual sync
  argocd app sync product-catalog-dev
  ```

- [ ] Verify new image is deployed
  ```bash
  kubectl describe pod -n product-catalog-dev -l app=product-catalog | grep Image:
  ```

### 2. Test Configuration Update

- [ ] Update a config in overlay
  ```bash
  vim k8s/overlays/dev/kustomization.yaml
  # Change replica count or resource limits
  ```

- [ ] Commit and push
  ```bash
  git add k8s/overlays/dev/kustomization.yaml
  git commit -m "chore(dev): update configuration"
  git push origin develop
  ```

- [ ] Verify ArgoCD detects and syncs
  ```bash
  argocd app get product-catalog-dev --refresh
  argocd app sync product-catalog-dev
  ```

- [ ] Verify changes applied
  ```bash
  kubectl get deployment product-catalog -n product-catalog-dev -o yaml
  ```

### 3. Test Rollback

- [ ] View deployment history
  ```bash
  argocd app history product-catalog-dev
  ```

- [ ] Rollback to previous version
  ```bash
  argocd app rollback product-catalog-dev <REVISION_NUMBER>
  ```

- [ ] Verify rollback successful
  ```bash
  kubectl get pods -n product-catalog-dev -o wide
  argocd app get product-catalog-dev
  ```

## Monitoring Setup

### 1. ArgoCD UI Access

- [ ] Port-forward ArgoCD server
  ```bash
  kubectl port-forward svc/argocd-server -n argocd 8080:443
  ```

- [ ] Login to UI
  - Open: https://localhost:8080
  - User: admin
  - Password: (from initial setup)

- [ ] Verify all apps visible in UI

### 2. Application Monitoring

- [ ] Setup alerts for sync failures
- [ ] Setup alerts for degraded health
- [ ] Configure notifications (Slack/Email)

### 3. Logs

- [ ] Verify application logs are accessible
  ```bash
  kubectl logs -n product-catalog-dev -l app=product-catalog --tail=100
  ```

- [ ] Setup log aggregation (if needed)

## Security Verification

### 1. RBAC Check

- [ ] Verify ArgoCD service account permissions
  ```bash
  kubectl describe serviceaccount argocd-application-controller -n argocd
  ```

- [ ] Verify namespace isolation
  ```bash
  kubectl auth can-i create pods --namespace=product-catalog-dev --as=system:serviceaccount:argocd:argocd-application-controller
  ```

### 2. Network Policies (Production)

- [ ] Verify network policies are applied
  ```bash
  kubectl get networkpolicy -n product-catalog
  kubectl describe networkpolicy -n product-catalog
  ```

### 3. Secrets Management

- [ ] Verify secrets are not in Git
  ```bash
  git log --all --full-history -- "*secret*" "*password*"
  ```

- [ ] Verify secrets exist in cluster
  ```bash
  kubectl get secrets -n product-catalog-dev
  ```

## Cleanup Old Resources

- [ ] Remove old application.yaml (already renamed to .old)
  ```bash
  # Already done: k8s/argocd/application.yaml.old
  ```

- [ ] Update documentation references
- [ ] Remove any manual deployment scripts

## Documentation Review

- [ ] Read `k8s/SETUP_COMPLETE.md`
- [ ] Read `k8s/GITOPS_DEPLOYMENT_GUIDE.md`
- [ ] Bookmark `k8s/GITOPS_QUICK_REFERENCE.md`
- [ ] Review `k8s/ARCHITECTURE_DIAGRAMS.md`
- [ ] Read `k8s/argocd/README.md`

## Team Onboarding

- [ ] Share documentation with team
- [ ] Document ArgoCD access procedures
- [ ] Train team on GitOps workflow
- [ ] Setup access controls

## Success Criteria

✅ All ArgoCD applications are **Synced** and **Healthy**
✅ All pods are **Running** in all namespaces
✅ Health endpoints return **200 OK**
✅ GitHub Actions workflows complete successfully
✅ Image updates trigger automatic deployments
✅ Configuration changes sync via ArgoCD
✅ Rollbacks work correctly
✅ No cluster credentials in GitHub Actions
✅ Team understands GitOps workflow

## Emergency Contacts

- ArgoCD Issues: Check `k8s/argocd/README.md`
- Workflow Issues: Check `.github/workflows/deploy.yml` comments
- Architecture Questions: Check `k8s/ARCHITECTURE_DIAGRAMS.md`

---

## Quick Commands Summary

```bash
# Check all apps status
argocd app list

# Sync an app
argocd app sync product-catalog-dev

# View logs
argocd app logs product-catalog-dev -f

# Rollback
argocd app history product-catalog-dev
argocd app rollback product-catalog-dev <REVISION>

# Check pods
kubectl get pods -n product-catalog-dev

# Port-forward ArgoCD
kubectl port-forward svc/argocd-server -n argocd 8080:443

# Port-forward app
kubectl port-forward svc/product-catalog -n product-catalog-dev 8080:8087
```

---

**Once all checkboxes are ✅, your GitOps deployment is complete!** 🎉

