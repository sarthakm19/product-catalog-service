# EKS Deployment Guide

## Prerequisites

### AWS Account Setup
- AWS account with EKS cluster already provisioned
- IAM user/role with necessary permissions
- AWS CLI configured
- kubectl installed and configured

### Tools Installation

```bash
# Install AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Install kubectl
curl -O https://s3.us-west-2.amazonaws.com/amazon-eks/1.28.0/2023-09-14/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin

# Install kustomize
curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh" | bash
sudo mv kustomize /usr/local/bin

# Install helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Install AWS IAM Authenticator
curl -o aws-iam-authenticator https://amazon-eks.s3-us-west-2.amazonaws.com/1.21.2/2021-07-05/bin/linux/amd64/aws-iam-authenticator
chmod +x ./aws-iam-authenticator
sudo mv aws-iam-authenticator /usr/local/bin
```

## Step 1: Configure EKS Access

### Get EKS Cluster Information
```bash
# List EKS clusters
aws eks list-clusters --region us-east-1

# Update kubeconfig
aws eks update-kubeconfig \
  --name your-cluster-name \
  --region us-east-1 \
  --profile your-aws-profile

# Verify connection
kubectl cluster-info
kubectl get nodes
```

## Step 2: Prepare Environment

### Create Namespaces
```bash
# Using Kustomize (automatic with base config)
kubectl apply -f k8s/base/namespace.yaml

# Verify namespace
kubectl get namespaces
```

### Set Up Secrets

#### Option 1: Using Local Secrets (Development)
```bash
# Create secrets directly
kubectl create secret generic product-catalog-secrets \
  --from-literal=jwt-secret='your-jwt-secret-key' \
  --from-literal=db-username='postgres' \
  --from-literal=db-password='your-db-password' \
  --from-literal=db-url='jdbc:postgresql://postgres:5432/product_catalog_db' \
  -n product-catalog

# Or apply from secret.yaml and edit
kubectl apply -f k8s/base/secret.yaml

# Verify
kubectl get secrets -n product-catalog
```

#### Option 2: Using AWS Secrets Manager (Production - Recommended)
```bash
# Install External Secrets Operator
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets \
  -n external-secrets-system \
  --create-namespace

# Create secrets in AWS Secrets Manager
aws secretsmanager create-secret \
  --name product-catalog/jwt-secret \
  --secret-string 'your-jwt-secret-key'

aws secretsmanager create-secret \
  --name product-catalog/db-password \
  --secret-string 'your-db-password'

# Apply External Secrets configuration
kubectl apply -f k8s/external-secrets/secretstore.yaml
```

#### Option 3: Using AWS Secrets Manager with IAM Roles for Service Accounts (IRSA)
```bash
# Create OIDC provider (if not exists)
aws eks describe-cluster \
  --name your-cluster-name \
  --query 'cluster.identity.oidc.issuer' \
  --region us-east-1 \
  --output text

# Create IAM policy for External Secrets
cat > external-secrets-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": "arn:aws:secretsmanager:region:account-id:secret:product-catalog/*"
    }
  ]
}
EOF

# Create IAM role and associate with service account
aws iam create-role \
  --role-name product-catalog-external-secrets \
  --assume-role-policy-document '{"Version":"2012-10-17",...}'

# Apply the policy and IRSA annotation
# See k8s/base/rbac.yaml for service account configuration
```

## Step 3: Set Up Container Registry Access

### Create ECR Repository
```bash
# Create repository
aws ecr create-repository \
  --repository-name product-catalog \
  --region us-east-1

# Get repository URI
aws ecr describe-repositories \
  --repository-names product-catalog \
  --region us-east-1
```

### Create Docker Registry Secret
```bash
# Generate auth token
aws ecr get-authorization-token \
  --region us-east-1 \
  --output text \
  --query 'authorizationData[0].authorizationToken' | base64 -d

# Create secret
kubectl create secret docker-registry container-registry \
  --docker-server=YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com \
  --docker-username=AWS \
  --docker-password=$(aws ecr get-authorization-token --region us-east-1 --output text --query 'authorizationData[0].authorizationToken' | base64 -d | cut -d: -f2) \
  -n product-catalog

# Or use Kustomize to patch
kubectl patch serviceaccount product-catalog \
  -p '{"imagePullSecrets": [{"name": "container-registry"}]}' \
  -n product-catalog
```

## Step 4: Build and Push Docker Image

### Build Image Locally
```bash
# Build image
docker build -t product-catalog:latest .

# Tag for ECR
docker tag product-catalog:latest \
  YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/product-catalog:latest
```

### Push to ECR
```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com

# Push image
docker push YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/product-catalog:latest
```

## Step 5: Deploy Database (Optional - if using RDS, skip this)

### Using RDS PostgreSQL
```bash
# Create RDS instance via AWS Console or CLI
aws rds create-db-instance \
  --db-instance-identifier product-catalog-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --master-username postgres \
  --master-user-password 'your-secure-password' \
  --allocated-storage 20 \
  --region us-east-1

# Get endpoint
aws rds describe-db-instances \
  --db-instance-identifier product-catalog-db \
  --query 'DBInstances[0].Endpoint.Address'
```

### Using PostgreSQL Helm Chart
```bash
# Add Bitnami Helm repo
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# Create values file
cat > postgres-values.yaml << 'EOF'
auth:
  username: postgres
  password: your-secure-password
  database: product_catalog_db

primary:
  persistence:
    enabled: true
    size: 20Gi

metrics:
  enabled: true
EOF

# Install PostgreSQL
helm install postgres bitnami/postgresql \
  -f postgres-values.yaml \
  -n product-catalog
```

## Step 6: Deploy Application

### Using Kustomize (Recommended)

#### Development Environment
```bash
# Validate manifests
kubectl kustomize k8s/overlays/dev

# Apply configuration
kubectl apply -k k8s/overlays/dev

# Verify deployment
kubectl get deployments -n product-catalog-dev
kubectl get pods -n product-catalog-dev
kubectl get svc -n product-catalog-dev
```

#### Staging Environment
```bash
kubectl apply -k k8s/overlays/staging
kubectl get all -n product-catalog-staging
```

#### Production Environment
```bash
# Review what will be deployed
kubectl apply -k k8s/overlays/production --dry-run=client -o yaml

# Apply production deployment
kubectl apply -k k8s/overlays/production

# Verify production deployment
kubectl get all -n product-catalog
kubectl get hpa -n product-catalog
kubectl get pdb -n product-catalog
```

### Using Helm (Alternative)

#### Development
```bash
helm install product-catalog-dev ./k8s/helm \
  -f ./k8s/helm/values-dev.yaml \
  -n product-catalog-dev \
  --create-namespace
```

#### Production
```bash
helm install product-catalog-prod ./k8s/helm \
  -f ./k8s/helm/values-production.yaml \
  -n product-catalog \
  --create-namespace
```

## Step 7: Verify Deployment

### Check Pod Status
```bash
# Get all resources
kubectl get all -n product-catalog

# Describe deployment
kubectl describe deployment product-catalog -n product-catalog

# Check pod logs
kubectl logs -f deployment/product-catalog -n product-catalog

# Check specific pod
kubectl describe pod <pod-name> -n product-catalog
```

### Test Service Access
```bash
# Port forward to local machine
kubectl port-forward svc/product-catalog 8087:8087 -n product-catalog

# Test API endpoints
curl http://localhost:8087/actuator/health

# Access Swagger UI
open http://localhost:8087/swagger-ui.html
```

### Verify Database Connection
```bash
# Check if Liquibase migrations ran
kubectl logs -f deployment/product-catalog -n product-catalog | grep -i liquibase

# Connect to database pod
kubectl exec -it <postgres-pod> -n product-catalog -- \
  psql -U postgres -d product_catalog_db -c "\dt"
```

## Step 8: Set Up Ingress/LoadBalancer

### Verify Load Balancer Service
```bash
# Get LoadBalancer endpoint
kubectl get svc product-catalog-lb -n product-catalog

# Wait for external IP to be assigned
kubectl get svc product-catalog-lb -n product-catalog --watch
```

### Configure AWS ALB Ingress Controller

#### Install ALB Controller
```bash
# Add AWS Helm repo
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Install controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=your-cluster-name

# Verify installation
kubectl get deployment -n kube-system aws-load-balancer-controller
```

#### Deploy Ingress
```bash
# Update ingress manifest with your domain
# Update: alb.ingress.kubernetes.io/certificate-arn
# Update: host: product-catalog.yourdomain.com

kubectl apply -f k8s/ingress/ingress.yaml
```

## Step 9: Set Up Monitoring (Optional)

### Install Prometheus and Grafana
```bash
# Add Prometheus Helm repo
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Install kube-prometheus-stack
helm install kube-prometheus prometheus-community/kube-prometheus-stack \
  -n monitoring \
  --create-namespace
```

### Apply ServiceMonitor
```bash
kubectl apply -f k8s/monitoring/servicemonitor.yaml
```

### Access Grafana
```bash
# Port forward to Grafana
kubectl port-forward svc/kube-prometheus-grafana 3000:80 -n monitoring

# Access at http://localhost:3000
# Default credentials: admin / prom-operator
```

## Step 10: Set Up GitOps with ArgoCD (Optional)

### Install ArgoCD
```bash
# Create namespace
kubectl create namespace argocd

# Install ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for deployment
kubectl wait --for=condition=available --timeout=300s deployment/argocd-server -n argocd
```

### Create ArgoCD Application
```bash
# Update repository URL in k8s/argocd/application.yaml
kubectl apply -f k8s/argocd/application.yaml

# Access ArgoCD
kubectl port-forward svc/argocd-server 8080:443 -n argocd

# Get initial password
kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath="{.data.password}" | base64 -d
```

## Post-Deployment Verification Checklist

- [ ] Namespaces created
- [ ] Secrets configured
- [ ] Deployment running with correct number of replicas
- [ ] Service endpoints accessible
- [ ] Database connectivity verified
- [ ] Health checks passing
- [ ] API endpoints responding
- [ ] Logs are clean (no errors)
- [ ] Monitoring configured and scraping metrics
- [ ] Ingress/LoadBalancer working
- [ ] Autoscaling rules active
- [ ] Pod Disruption Budget in place

## Troubleshooting

### Pods not starting
```bash
# Check pod events
kubectl describe pod <pod-name> -n product-catalog

# Check logs
kubectl logs <pod-name> -n product-catalog
```

### Database connection issues
```bash
# Test connectivity from pod
kubectl exec -it <pod-name> -n product-catalog -- \
  nc -zv postgres 5432
```

### Image pull errors
```bash
# Check container registry secret
kubectl get secret container-registry -n product-catalog

# Check image availability in ECR
aws ecr describe-images --repository-name product-catalog
```

### Scaling issues
```bash
# Check HPA status
kubectl get hpa -n product-catalog
kubectl describe hpa product-catalog-hpa -n product-catalog

# Check metrics
kubectl top pods -n product-catalog
```

---

**Last Updated:** 2025-12-30

