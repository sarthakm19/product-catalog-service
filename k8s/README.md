# Kubernetes Deployment Configuration for Product Catalog Service

This directory contains comprehensive Kubernetes configurations for deploying the Product Catalog Service to AWS EKS (Elastic Kubernetes Service).

## Directory Structure

```
k8s/
├── base/                           # Base kustomization configuration
│   ├── kustomization.yaml
│   ├── namespace.yaml              # Namespace definition
│   ├── configmap.yaml              # Non-sensitive configuration
│   ├── secret.yaml                 # Sensitive data (template)
│   ├── deployment.yaml             # Main deployment manifest
│   ├── service.yaml                # Service definitions
│   ├── hpa.yaml                    # Horizontal Pod Autoscaler
│   ├── pdb.yaml                    # Pod Disruption Budget
│   └── rbac.yaml                   # Role-based access control
├── overlays/
│   ├── dev/                        # Development environment
│   │   ├── kustomization.yaml
│   │   ├── configmap-patch.yaml
│   │   └── hpa-patch.yaml
│   ├── staging/                    # Staging environment
│   │   ├── kustomization.yaml
│   │   ├── configmap-patch.yaml
│   │   └── hpa-patch.yaml
│   └── production/                 # Production environment
│       ├── kustomization.yaml
│       ├── configmap-patch.yaml
│       ├── hpa-patch.yaml
│       ├── networkpolicy.yaml      # Network policies
│       ├── podsecuritypolicy.yaml  # Pod security
│       └── priorityclass.yaml      # Priority classes
├── ingress/
│   ├── ingress.yaml                # Ingress configuration
│   └── ingress-tls.yaml            # TLS configuration
├── monitoring/
│   ├── servicemonitor.yaml         # Prometheus monitoring
│   ├── alerts.yaml                 # Alerting rules
│   └── dashboards.yaml             # Grafana dashboards
├── networking/
│   ├── networkpolicy.yaml          # Network segmentation
│   └── servicemesh/                # Istio/Linkerd configs
└── helm/                           # Helm chart (alternative to kustomize)
    ├── Chart.yaml
    ├── values.yaml
    ├── values-dev.yaml
    ├── values-staging.yaml
    ├── values-production.yaml
    ├── templates/
    │   ├── deployment.yaml
    │   ├── service.yaml
    │   ├── configmap.yaml
    │   ├── secret.yaml
    │   └── hpa.yaml
    └── README.md
```

## Prerequisites

### AWS Account and EKS Cluster
- AWS EKS cluster already provisioned and running
- `kubectl` CLI configured to access your EKS cluster
- AWS IAM permissions to manage EKS resources

### CLI Tools Required
```bash
# Install required tools
brew install kubectl      # Kubernetes CLI
brew install kustomize    # Kustomization tool
brew install helm         # Helm package manager
brew install aws-cli      # AWS CLI
brew install eksctl       # EKS cluster management
```

### Configure AWS Credentials
```bash
aws configure
# or
export AWS_PROFILE=your-profile
```

## Deployment Instructions

### 1. Using Kustomize (Recommended for flexibility)

#### Development Environment
```bash
kubectl apply -k k8s/overlays/dev
```

#### Staging Environment
```bash
kubectl apply -k k8s/overlays/staging
```

#### Production Environment
```bash
# Review changes first
kubectl apply -k k8s/overlays/production --dry-run=client -o yaml

# Apply production deployment
kubectl apply -k k8s/overlays/production
```

### 2. Using Helm (Alternative approach)

#### Add repository (if using remote charts)
```bash
helm repo add product-catalog ./k8s/helm
helm repo update
```

#### Install/Upgrade Release

**Development:**
```bash
helm install product-catalog-dev ./k8s/helm \
  -f ./k8s/helm/values-dev.yaml \
  -n product-catalog-dev \
  --create-namespace
```

**Production:**
```bash
helm upgrade --install product-catalog-prod ./k8s/helm \
  -f ./k8s/helm/values-production.yaml \
  -n product-catalog-prod \
  --create-namespace \
  --wait
```

### 3. Direct kubectl Apply
```bash
# Apply all manifests in order
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/rbac.yaml
kubectl apply -f k8s/base/configmap.yaml
kubectl apply -f k8s/base/secret.yaml
kubectl apply -f k8s/base/deployment.yaml
kubectl apply -f k8s/base/service.yaml
kubectl apply -f k8s/base/hpa.yaml
kubectl apply -f k8s/ingress/ingress.yaml
```

## Configuration Management

### Environment-Specific Configuration

#### ConfigMap Management
Each environment has specific configurations:
- **Development**: Debug logging, relaxed resource limits
- **Staging**: Production-like setup with fewer replicas
- **Production**: Optimized settings, strict resource limits

#### Secret Management

**Using AWS Secrets Manager (Recommended for EKS):**
```bash
# Store secrets in AWS Secrets Manager
aws secretsmanager create-secret \
  --name product-catalog/jwt-secret \
  --secret-string "your-jwt-secret-key"

# Reference in deployment using External Secrets Operator
# See external-secrets/ directory
```

**Using Sealed Secrets (Alternative):**
```bash
# Install Sealed Secrets controller
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml -n kube-system

# Create sealed secret
echo -n 'your-jwt-secret' | kubectl create secret generic jwt-secret \
  --dry-run=client --from-file=- \
  -o yaml | kubeseal > k8s/base/jwt-secret-sealed.yaml
```

**Using External Secrets Operator (Best Practice):**
```bash
# See external-secrets-operator configuration in monitoring/
# Allows syncing secrets from AWS Secrets Manager
```

### Database Secrets
```bash
# Create PostgreSQL secret
kubectl create secret generic postgres-credentials \
  --from-literal=username=postgres \
  --from-literal=password=your-secure-password \
  -n product-catalog
```

## Networking and Ingress

### AWS Network Load Balancer (NLB)
The service is configured with `Type: LoadBalancer` using AWS NLB.

```bash
# Get the LoadBalancer endpoint
kubectl get svc -n product-catalog
```

### Ingress with AWS ALB (Application Load Balancer)

```bash
# Install AWS Load Balancer Controller
helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=your-cluster-name

# Apply ingress configuration
kubectl apply -f k8s/ingress/ingress.yaml
```

### DNS and TLS/SSL

**Using AWS Certificate Manager:**
```bash
# Create certificate in ACM
aws acm request-certificate \
  --domain-name product-catalog.yourdomain.com \
  --validation-method DNS

# Reference certificate ARN in ingress annotation
# See k8s/ingress/ingress-tls.yaml
```

## Database Setup

### PostgreSQL in Kubernetes

**Option 1: Using RDS (Recommended for Production)**
```bash
# Database connection details stored in secrets
# See base/secret.yaml for configuration
```

**Option 2: Using PostgreSQL Helm Chart**
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install postgres bitnami/postgresql \
  -n product-catalog \
  --values ./k8s/database/postgres-values.yaml
```

### Database Migration with Liquibase
Liquibase migrations run automatically on pod startup (configured in deployment).

## Monitoring and Logging

### Prometheus Monitoring
```bash
# Install Prometheus Operator
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install kube-prometheus prometheus-community/kube-prometheus-stack \
  -n monitoring --create-namespace

# Apply ServiceMonitor configuration
kubectl apply -f k8s/monitoring/servicemonitor.yaml
```

### ELK/CloudWatch Logging
```bash
# For CloudWatch, ensure pods have IAM role with CloudWatch permissions
# See IRSA configuration in k8s/base/rbac.yaml
```

### Grafana Dashboards
```bash
# Dashboards automatically loaded from ConfigMap
kubectl apply -f k8s/monitoring/dashboards.yaml
```

## Security Considerations

### Pod Security
- Non-root user execution (spring:spring)
- Read-only root filesystem (where possible)
- Network policies restricting traffic
- Security context applied to all pods

### RBAC (Role-Based Access Control)
```bash
# Service account with minimal permissions
kubectl get serviceaccount -n product-catalog
```

### Image Security
- Use specific image tags (no :latest)
- Scan images for vulnerabilities
- Use private ECR registry
- Sign container images

### Network Policies
```bash
# Apply network policies
kubectl apply -f k8s/networking/networkpolicy.yaml
```

## Scaling

### Horizontal Pod Autoscaler (HPA)
Automatically scales pods based on CPU/memory metrics.

```bash
# Check HPA status
kubectl get hpa -n product-catalog

# Update thresholds in overlays/{env}/hpa-patch.yaml
```

### Vertical Pod Autoscaler (VPA)
```bash
# Install VPA
helm repo add autoscaling https://charts.jetstack.io
helm install vpa autoscaling/vpa -n kube-system --create-namespace
```

## Health Checks and Readiness

### Liveness Probe
```bash
# Checks if pod is running
# Path: /actuator/health
```

### Readiness Probe
```bash
# Checks if pod is ready to receive traffic
# Path: /actuator/health/readiness
```

## Deployment Strategies

### Blue-Green Deployment
```bash
# Apply new version
kubectl set image deployment/product-catalog \
  product-catalog=ecr-repo/product-catalog:v2 \
  -n product-catalog

# Check rollout status
kubectl rollout status deployment/product-catalog -n product-catalog
```

### Canary Deployment (with Flagger)
```bash
# Install Flagger
helm repo add flagger https://flagger.app
helm install flagger flagger/flagger -n istio-system --create-namespace

# Apply canary configuration
kubectl apply -f k8s/overlays/production/canary.yaml
```

### Rolling Update (Default)
Configured with `strategy.type: RollingUpdate` for gradual pod replacement.

## Troubleshooting

### Check Pod Status
```bash
# View logs
kubectl logs -f deployment/product-catalog -n product-catalog

# Describe pod
kubectl describe pod <pod-name> -n product-catalog

# Get events
kubectl get events -n product-catalog --sort-by='.lastTimestamp'
```

### Debug Issues
```bash
# SSH into pod for debugging
kubectl exec -it <pod-name> -n product-catalog -- /bin/sh

# Port-forward to local machine
kubectl port-forward svc/product-catalog 8087:8087 -n product-catalog
```

### Check Resource Usage
```bash
kubectl top pods -n product-catalog
kubectl top nodes
```

## CI/CD Integration

### GitOps with ArgoCD
```bash
# Install ArgoCD
helm repo add argo https://argoproj.github.io/argo-helm
helm install argocd argo/argo-cd -n argocd --create-namespace

# Create Application manifest
kubectl apply -f k8s/argocd/application.yaml
```

### GitHub Actions Integration
See `.github/workflows/deploy.yaml` for automated deployment.

## Cost Optimization

### Resource Requests and Limits
- Configured based on actual metrics
- Prevents resource starvation and cost overruns
- Review and adjust in overlays/

### Node Affinity and Pod Disruption Budget
- Ensures optimal node utilization
- Prevents accidental pod eviction

## Cleanup

### Delete Deployment
```bash
# Development
kubectl delete -k k8s/overlays/dev

# Production
kubectl delete -k k8s/overlays/production
```

### Delete Entire Namespace
```bash
kubectl delete namespace product-catalog
```

## Additional Resources

- [Kubernetes Official Documentation](https://kubernetes.io/docs/)
- [AWS EKS Best Practices](https://aws.github.io/aws-eks-best-practices/)
- [Kubernetes Security Best Practices](https://kubernetes.io/docs/concepts/security/)
- [Kustomize Documentation](https://kubectl.sigs.k8s.io/docs/home/)
- [Helm Documentation](https://helm.sh/docs/)

## Support and Maintenance

For issues or questions:
1. Check logs: `kubectl logs -f <pod-name> -n product-catalog`
2. Check events: `kubectl get events -n product-catalog`
3. Review configuration: `kubectl get all -n product-catalog`
4. Consult AWS EKS documentation and support

---

**Last Updated:** 2025-12-30
**Version:** 1.0.0

