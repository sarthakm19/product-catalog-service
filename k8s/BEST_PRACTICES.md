# Kubernetes Best Practices and Architecture Guide

## Architecture Overview

### Kubernetes Cluster Architecture for EKS

```
┌─────────────────────────────────────────────────────────────────┐
│                         AWS EKS Cluster                          │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Control Plane (AWS Managed)            │  │
│  │  - kube-apiserver, kube-scheduler, kube-controller-manager│  │
│  │  - etcd (distributed key-value store)                    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Worker Nodes (EC2 Instances)                │  │
│  │                                                            │  │
│  │  ┌─────────────────┐  ┌─────────────────┐                │  │
│  │  │   Node 1        │  │   Node 2        │                │  │
│  │  │                 │  │                 │                │  │
│  │  │  ┌───────────┐  │  │  ┌───────────┐  │                │  │
│  │  │  │ Pod 1     │  │  │  │ Pod 2     │  │                │  │
│  │  │  │ (App)     │  │  │  │ (App)     │  │                │  │
│  │  │  └───────────┘  │  │  └───────────┘  │                │  │
│  │  │  ┌───────────┐  │  │  ┌───────────┐  │                │  │
│  │  │  │ kubelet   │  │  │  │ kubelet   │  │                │  │
│  │  │  │ container │  │  │  │ container │  │                │  │
│  │  │  │ runtime   │  │  │  │ runtime   │  │                │  │
│  │  │  └───────────┘  │  │  └───────────┘  │                │  │
│  │  └─────────────────┘  └─────────────────┘                │  │
│  │                                                            │  │
│  │  ┌─────────────────┐  ┌─────────────────┐                │  │
│  │  │   Node 3        │  │   Node N        │                │  │
│  │  │   ...           │  │   ...           │                │  │
│  │  └─────────────────┘  └─────────────────┘                │  │
│  │                                                            │  │
│  │  Add-ons:                                                 │  │
│  │  - CoreDNS (Service discovery)                            │  │
│  │  - kube-proxy (Service networking)                        │  │
│  │  - AWS VPC CNI (Container networking)                     │  │
│  │  - AWS Load Balancer Controller (Ingress)                 │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Namespaces (Logical Isolation)              │  │
│  │                                                            │  │
│  │  - product-catalog (production)                          │  │
│  │  - product-catalog-staging                               │  │
│  │  - product-catalog-dev                                   │  │
│  │  - kube-system (system components)                       │  │
│  │  - kube-public                                           │  │
│  │  - monitoring (Prometheus, Grafana)                      │  │
│  │  - ingress-nginx (Ingress controller)                    │  │
│  │  - argocd (GitOps controller)                            │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │                   │
            ┌───────▼────────┐  ┌─────▼─────────┐
            │   AWS Services │  │  AWS Services │
            │                │  │                │
            │ - RDS          │  │ - Secrets      │
            │   (PostgreSQL) │  │   Manager      │
            │ - ALB/NLB      │  │ - CloudWatch   │
            │   (LB)         │  │ - ECR          │
            │ - Auto Scaling │  │ - IAM          │
            │                │  │ - VPC          │
            └────────────────┘  └────────────────┘
```

## Application Deployment Architecture

```
┌────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│                                                              │
│  Ingress/ALB                                                │
│       │                                                      │
│  ┌────▼──────────────────────────────────────────────────┐ │
│  │         Kubernetes Services                           │ │
│  │  ┌────────────────────────────────────────────────┐  │ │
│  │  │ ClusterIP: Internal communication              │  │ │
│  │  │ NodePort: Node-level access                    │  │ │
│  │  │ LoadBalancer: External access (AWS NLB)        │  │ │
│  │  └────────────────────────────────────────────────┘  │ │
│  └────┬───────────────────────────────────────────────────┘ │
│       │                                                      │
│  ┌────▼──────────────────────────────────────────────────┐ │
│  │         Deployment Controller                         │ │
│  │  ┌────────────────────────────────────────────────┐  │ │
│  │  │ Desired State: 3 replicas                      │  │ │
│  │  │ Rolling Update Strategy                        │  │ │
│  │  │ maxSurge: 1, maxUnavailable: 0                │  │ │
│  │  └────────────────────────────────────────────────┘  │ │
│  └────┬───────────────────────────────────────────────────┘ │
│       │                                                      │
│  ┌────▼──────────────────────────────────────────────────┐ │
│  │  Replica Sets (Pod Templates)                         │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │ │
│  │  │   Pod 1     │  │   Pod 2     │  │   Pod 3     │  │ │
│  │  │             │  │             │  │             │  │ │
│  │  │ ┌─────────┐ │  │ ┌─────────┐ │  │ ┌─────────┐ │  │ │
│  │  │ │Container│ │  │ │Container│ │  │ │Container│ │  │ │
│  │  │ │  Image  │ │  │ │  Image  │ │  │ │  Image  │ │  │ │
│  │  │ │         │ │  │ │         │ │  │ │         │ │  │ │
│  │  │ │ Port    │ │  │ │ Port    │ │  │ │ Port    │ │  │ │
│  │  │ │ 8087    │ │  │ │ 8087    │ │  │ │ 8087    │ │  │ │
│  │  │ └─────────┘ │  │ └─────────┘ │  │ └─────────┘ │  │ │
│  │  │             │  │             │  │             │  │ │
│  │  │ Volumes:    │  │ Volumes:    │  │ Volumes:    │  │ │
│  │  │ - ConfigMap │  │ - ConfigMap │  │ - ConfigMap │  │ │
│  │  │ - Secrets   │  │ - Secrets   │  │ - Secrets   │  │ │
│  │  │ - EmptyDir  │  │ - EmptyDir  │  │ - EmptyDir  │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  │ │
│  │  (Distributed across 3 nodes via Pod Anti-Affinity)  │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │         Horizontal Pod Autoscaler (HPA)               │ │
│  │  ┌────────────────────────────────────────────────┐  │ │
│  │  │ Metrics: CPU (70%), Memory (80%)               │  │ │
│  │  │ Min Replicas: 3, Max Replicas: 10              │  │ │
│  │  │ Scale Up: +50% or +2 pods every 30s             │  │ │
│  │  │ Scale Down: -10% every 60s                      │  │ │
│  │  └────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │         Pod Disruption Budget (PDB)                   │ │
│  │  ┌────────────────────────────────────────────────┐  │ │
│  │  │ Minimum Available: 2 pods                       │  │ │
│  │  │ Prevents accidental eviction during updates     │  │ │
│  │  └────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

## Implementation Patterns

### 1. Kustomize vs Helm

| Aspect | Kustomize | Helm |
|--------|-----------|------|
| **Type** | Template-free patching | Package manager |
| **Complexity** | Simpler for modifications | More powerful templating |
| **Use Case** | Environment-specific patches | Reusable charts |
| **Learning Curve** | Easier | Steeper |
| **Flexibility** | Great for overlays | Great for parameterization |

**Recommendation**: Use Kustomize for environment-specific deployments (dev, staging, prod) and Helm for reusable charts.

### 2. Configuration Management

```yaml
# Best Practice: Separation of Concerns

# ConfigMap - Non-sensitive configuration
kind: ConfigMap
data:
  SPRING_PROFILES_ACTIVE: kubernetes
  LOGGING_LEVEL: INFO
  SERVER_PORT: "8087"

# Secret - Sensitive data (use external secrets manager)
kind: Secret
data:
  jwt-secret: (from AWS Secrets Manager)
  db-password: (from AWS Secrets Manager)
  # NEVER check secrets into git!
```

### 3. Pod Security Best Practices

```yaml
spec:
  securityContext:
    runAsNonRoot: true      # Never run as root
    runAsUser: 1000         # Specific user ID
    fsGroup: 1000           # File system group
    seccompProfile:
      type: RuntimeDefault  # Restrict syscalls
  
  containers:
  - name: app
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: false  # Set true if possible
      capabilities:
        drop:
          - ALL                      # Drop all capabilities
        add:
          - NET_BIND_SERVICE        # Only add what's needed
```

### 4. Resource Management

```yaml
# Always set requests and limits
resources:
  requests:           # What the pod needs
    cpu: 250m         # 0.25 CPU cores
    memory: 512Mi      # 512 Mebibytes
  limits:             # Maximum allowed
    cpu: 1000m        # 1 CPU core
    memory: 1024Mi     # 1 Gibibyte

# Help Kubernetes:
# - Schedule pods correctly
# - Prevent over-commitment
# - Enable autoscaling
```

### 5. Health Checks Strategy

```yaml
# Liveness Probe - Is the container alive?
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
  initialDelaySeconds: 60   # Wait for app to start
  periodSeconds: 10
  failureThreshold: 3

# Readiness Probe - Is the container ready for traffic?
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
  initialDelaySeconds: 30   # Start checking earlier
  periodSeconds: 5
  failureThreshold: 3

# Startup Probe - Has the app finished starting?
startupProbe:
  httpGet:
    path: /actuator/health
  periodSeconds: 3
  failureThreshold: 30  # 30 * 3s = 90s max startup time
```

### 6. Affinity and Pod Placement

```yaml
affinity:
  # Spread pods across different nodes
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 100
      podAffinityTerm:
        labelSelector:
          matchLabels:
            app: product-catalog
        topologyKey: kubernetes.io/hostname  # Different nodes
  
  # Use specific node types
  nodeAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 50
      preference:
        matchExpressions:
        - key: node.kubernetes.io/instance-type
          operator: In
          values:
          - t3.medium
          - t3.large
```

### 7. Network Security with Network Policies

```yaml
# Default: Allow all traffic (not secure!)
# Better: Explicit allow-list

# Ingress - Who can talk to my pods?
- from:
  - namespaceSelector:
      matchLabels:
        name: ingress-nginx  # Only from ingress controller
  ports:
  - port: 8087

# Egress - Who can my pods talk to?
- to:
  - namespaceSelector:
      matchLabels:
        name: kube-system    # Allow DNS
  ports:
  - port: 53
```

### 8. GitOps with ArgoCD

```yaml
# Single source of truth: Git repository
# ArgoCD automatically syncs desired state

apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: product-catalog
spec:
  source:
    repoURL: https://github.com/your-org/product-catalog
    path: k8s/overlays/production
  
  syncPolicy:
    automated:
      prune: true        # Remove resources not in Git
      selfHeal: true     # Reconcile drift
```

## Operational Checklist

### Pre-Deployment
- [ ] Build and test Docker image locally
- [ ] Push image to ECR
- [ ] Review Kustomization files
- [ ] Verify secrets are in AWS Secrets Manager
- [ ] Check resource requests/limits are reasonable
- [ ] Verify database is accessible
- [ ] Test database migrations

### Deployment
- [ ] Create namespace
- [ ] Apply RBAC policies
- [ ] Apply ConfigMaps
- [ ] Apply Secrets (from external manager)
- [ ] Deploy application
- [ ] Verify pods are running
- [ ] Check service endpoints
- [ ] Test health checks
- [ ] Verify database schema

### Post-Deployment
- [ ] Monitor pod logs
- [ ] Check resource usage
- [ ] Test API endpoints
- [ ] Verify autoscaling works
- [ ] Check monitoring/alerts
- [ ] Load test (if applicable)
- [ ] Document any issues

### Maintenance
- [ ] Monitor metrics regularly
- [ ] Review and update resource limits
- [ ] Check for deprecated Kubernetes features
- [ ] Update container images
- [ ] Review and rotate secrets
- [ ] Check for security vulnerabilities
- [ ] Test disaster recovery procedures

## Performance Optimization Tips

1. **Image Optimization**
   ```dockerfile
   # Use multi-stage builds
   # Use minimal base images (alpine, distroless)
   # Remove unnecessary layers
   ```

2. **Pod Scheduling**
   ```bash
   # Monitor node utilization
   kubectl top nodes
   
   # Check pod distribution
   kubectl get pods -n product-catalog -o wide
   ```

3. **Database Connection Pooling**
   ```yaml
   SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: "20"
   SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE: "5"
   ```

4. **Caching Strategy**
   - Use Redis for session/cache
   - Implement HTTP caching headers
   - Cache static content with CDN

5. **Rate Limiting**
   ```yaml
   # Implement in application or API Gateway
   # Protect against abuse
   ```

## Security Best Practices

1. **Never Commit Secrets**
   ```bash
   # Use .gitignore
   secrets.yaml
   .env
   ```

2. **Use Private Registry**
   - Store images in private ECR
   - Use container registry credentials

3. **Image Scanning**
   ```bash
   # Scan for vulnerabilities
   trivy image product-catalog:latest
   ```

4. **Network Isolation**
   - Apply NetworkPolicies
   - Restrict ingress/egress

5. **Access Control (RBAC)**
   - Use least privilege principle
   - Service accounts per deployment
   - Role-based access

6. **Secret Rotation**
   - Regular rotation of JWT secrets
   - Database password rotation
   - API key rotation

## Monitoring and Observability

### Metrics (Prometheus)
- CPU usage per pod
- Memory usage per pod
- HTTP request rate and latency
- Database connection pool status
- Error rates and types

### Logs (CloudWatch/ELK)
- Application logs
- Database query logs
- Kubernetes events
- Ingress/ALB access logs

### Traces (Optional - Jaeger/Zipkin)
- Request flow across services
- Performance bottlenecks
- Error propagation

### Alerts
- Pod crash loops
- High error rates
- Resource exhaustion
- Slow responses

## Disaster Recovery

1. **Backup Strategy**
   ```bash
   # Database backups
   - RDS automated backups
   - Point-in-time recovery
   
   # Configuration backups
   - Git repository as source of truth
   - Regular etcd snapshots
   ```

2. **Recovery Procedures**
   ```bash
   # Rolling back deployment
   kubectl rollout undo deployment/product-catalog
   
   # Restoring from backup
   # Follow RDS restore procedures
   ```

3. **Testing**
   - Regular backup restoration tests
   - Chaos engineering tests
   - Failure scenario testing

---

**Last Updated:** 2025-12-30

