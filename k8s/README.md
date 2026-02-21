# Kubernetes Manifests Reference

Kubernetes configuration files for deploying the Product Catalog Service to AWS EKS. Uses Kustomize base + overlays pattern with ArgoCD for GitOps.

> **Deployment instructions:** See [docs/DEPLOYMENT.md](../docs/DEPLOYMENT.md)
> **Troubleshooting:** See [docs/TROUBLESHOOTING.md](../docs/TROUBLESHOOTING.md)

---

## Directory Structure

```
k8s/
├── base/                          # Shared base resources
│   ├── kustomization.yaml         # Base kustomization config
│   ├── namespace.yaml             # Namespace + Pod Security Standards labels
│   ├── configmap.yaml             # App config (ports, HikariCP, logging, actuator)
│   ├── secret.yaml                # Secret template (DB creds, JWT secret)
│   ├── rbac.yaml                  # ServiceAccount, Roles, RoleBindings, NetworkPolicies
│   ├── deployment.yaml            # Deployment (3 replicas, probes, security context)
│   ├── service.yaml               # ClusterIP + Headless services
│   ├── hpa.yaml                   # Horizontal Pod Autoscaler (CPU/Memory)
│   ├── pdb.yaml                   # Pod Disruption Budget (min 2 available)
│   └── postgres-deployment.yaml   # In-cluster Postgres (dev only, not for RDS)
│
├── overlays/
│   ├── dev/                       # Development environment
│   │   ├── kustomization.yaml     # 1 replica, DEBUG, relaxed resources
│   │   └── ingress.yaml           # ALB ingress (HTTP)
│   ├── staging/                   # Staging environment
│   │   ├── kustomization.yaml     # 2 replicas, INFO, moderate resources
│   │   └── ingress.yaml           # ALB ingress (HTTP)
│   └── production/                # Production environment
│       ├── kustomization.yaml     # 3 replicas, WARN, strict resources
│       ├── ingress.yaml           # ALB ingress (HTTP/HTTPS)
│       ├── networkpolicy.yaml     # Strict ingress/egress rules
│       └── priorityclass.yaml     # High-priority scheduling
│
├── argocd/                        # ArgoCD Application manifests
│   ├── application-dev.yaml       # Dev app (manual sync, develop branch)
│   ├── application-staging.yaml   # Staging app (auto sync, staging branch)
│   ├── application-prod.yaml      # Prod app (auto sync + prune, main branch)
│   └── README.md                  # ArgoCD operations guide
│
├── external-secrets/              # AWS Secrets Manager integration
│   └── secretstore.yaml           # ExternalSecrets Operator config
│
├── monitoring/                    # Observability
│   └── servicemonitor.yaml        # Prometheus ServiceMonitor + alert rules
│
├── helm/                          # Helm chart (alternative to Kustomize)
│   ├── Chart.yaml                 # Chart metadata
│   ├── values.yaml                # Default values
│   ├── values-dev.yaml            # Dev overrides
│   ├── values-staging.yaml        # Staging overrides
│   └── values-production.yaml     # Production overrides
│
├── ALB_CONTROLLER_IAM_POLICY.json # IAM policy for AWS LB Controller
├── setup-deployment.sh            # Interactive deployment script
└── update-alb-policy.sh           # ALB IAM policy update script
```

---

## Quick Commands

```bash
# Deploy
kubectl apply -k k8s/overlays/dev           # Dev
kubectl apply -k k8s/overlays/staging        # Staging
kubectl apply -k k8s/overlays/production     # Production

# Verify
kubectl get pods -n product-catalog
kubectl get svc -n product-catalog
kubectl get ingress -n product-catalog
kubectl logs -f deployment/product-catalog -n product-catalog

# Preview rendered manifests (dry run)
kustomize build k8s/overlays/production
kubectl apply -k k8s/overlays/production --dry-run=client

# Rollback
kubectl rollout undo deployment/product-catalog -n product-catalog

# Restart pods
kubectl rollout restart deployment/product-catalog -n product-catalog
```

---

## Environment Comparison

| Setting | Dev | Staging | Production |
|---------|-----|---------|------------|
| Namespace | `product-catalog-dev` | `product-catalog-staging` | `product-catalog` |
| Replicas | 1 | 2 (HPA: 2-6) | 3 (HPA: 3-10) |
| CPU Req/Limit | 100m / 500m | 200m / 750m | 500m / 1500m |
| Memory Req/Limit | 256Mi / 512Mi | 384Mi / 768Mi | 768Mi / 1024Mi |
| Log Level | DEBUG | INFO | WARN |
| Network Policies | No | No | Yes (strict) |
| PDB Min Available | — | 1 | 2 |
| ArgoCD Sync | Manual | Auto + SelfHeal | Auto + SelfHeal + Prune |
| Git Branch | `develop` | `staging` | `main` |
