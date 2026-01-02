# Kubernetes Troubleshooting Guide

## Common Issues and Solutions

### 1. Pod Failing to Start

#### Symptoms
- Pod status: `CrashLoopBackOff` or `Error`
- Pod not becoming `Running`

#### Investigation
```bash
# Get pod details
kubectl describe pod <pod-name> -n product-catalog

# Check recent logs
kubectl logs <pod-name> -n product-catalog --tail=100

# Check previous logs if pod crashed
kubectl logs <pod-name> -n product-catalog --previous

# Check events
kubectl get events -n product-catalog --sort-by='.lastTimestamp'
```

#### Common Causes and Fixes

**Image Pull Error**
```bash
# Verify image exists in registry
aws ecr describe-images --repository-name product-catalog

# Check image pull secret
kubectl get secret container-registry -n product-catalog

# Recreate secret if needed
kubectl create secret docker-registry container-registry \
  --docker-server=ACCOUNT_ID.dkr.ecr.REGION.amazonaws.com \
  --docker-username=AWS \
  --docker-password=$(aws ecr get-authorization-token --region REGION --output text --query 'authorizationData[0].authorizationToken' | base64 -d | cut -d: -f2) \
  --docker-email=you@example.com \
  -n product-catalog --dry-run=client -o yaml | kubectl apply -f -
```

**Database Connection Failure**
```bash
# Verify database is accessible
kubectl run -it --rm debug --image=busybox --restart=Never -- \
  wget -O- http://localhost:8087/actuator/health/liveness

# Check if database secrets are set
kubectl get secret product-catalog-secrets -n product-catalog -o yaml

# Verify database connectivity from pod
kubectl exec -it <pod-name> -n product-catalog -- /bin/sh
# Inside pod:
nc -zv postgres 5432
```

**Insufficient Resources**
```bash
# Check node resources
kubectl describe nodes

# Check pod resource requests vs available
kubectl describe pod <pod-name> -n product-catalog

# List pods by memory usage
kubectl top pods -n product-catalog --sort-by=memory
```

**Init Container Failure**
```bash
# Check init container logs
kubectl logs <pod-name> -c wait-for-db -n product-catalog

# Verify database host is resolvable
kubectl exec -it <pod-name> -n product-catalog -- nslookup postgres
```

### 2. Service Not Accessible

#### Symptoms
- Cannot reach service endpoint
- Connection refused
- Timeout

#### Investigation
```bash
# Verify service exists
kubectl get svc -n product-catalog

# Check service endpoints
kubectl get endpoints product-catalog -n product-catalog

# Test from within cluster
kubectl run -it --rm debug --image=busybox --restart=Never -- \
  wget -O- http://product-catalog:8087/actuator/health

# Check service ports
kubectl describe svc product-catalog -n product-catalog
```

#### Common Causes and Fixes

**No Endpoints**
```bash
# Check if any pods are running
kubectl get pods -n product-catalog -l app=product-catalog

# Verify pod readiness probe
kubectl get pods -n product-catalog -o wide

# If pods not ready, check why
kubectl describe pod <pod-name> -n product-catalog
```

**Port Mismatch**
```bash
# Verify service port matches container port
kubectl get svc product-catalog -n product-catalog -o yaml | grep -A 5 ports:

# Check deployment container ports
kubectl get deployment product-catalog -n product-catalog -o yaml | grep -A 5 containerPort:
```

**Network Policy Blocking Traffic**
```bash
# List network policies
kubectl get networkpolicy -n product-catalog

# Describe policy
kubectl describe networkpolicy <policy-name> -n product-catalog

# Test with policy disabled (caution in production)
kubectl delete networkpolicy product-catalog-allow-external -n product-catalog
```

### 3. High Resource Usage

#### Symptoms
- Pods being evicted
- OOMKilled errors
- Slow response times

#### Investigation
```bash
# Check current usage
kubectl top pods -n product-catalog
kubectl top nodes

# Get resource metrics
kubectl describe node <node-name>

# Check for memory leaks in logs
kubectl logs deployment/product-catalog -n product-catalog | grep -i memory

# Analyze pod resource requests vs limits
kubectl get pods -n product-catalog -o json | \
  jq '.items[] | {name: .metadata.name, resources: .spec.containers[0].resources}'
```

#### Solutions

**Increase Resource Limits**
```bash
# Edit deployment
kubectl edit deployment product-catalog -n product-catalog

# Update resources section:
# resources:
#   requests:
#     cpu: 500m
#     memory: 768Mi
#   limits:
#     cpu: 1500m
#     memory: 1024Mi
```

**Optimize Application**
```bash
# Check for slow queries in logs
kubectl logs deployment/product-catalog -n product-catalog | grep SLOW

# Check database connection pool
kubectl logs deployment/product-catalog -n product-catalog | grep -i hikari

# Review application heap size settings
```

**Adjust Autoscaling**
```bash
# Check HPA status
kubectl get hpa product-catalog-hpa -n product-catalog

# Edit HPA thresholds
kubectl edit hpa product-catalog-hpa -n product-catalog
```

### 4. Database Issues

#### Symptoms
- Connection timeouts
- Liquibase migration failures
- Data inconsistency

#### Investigation
```bash
# Verify database pod is running (if using in-cluster PostgreSQL)
kubectl get pods -n product-catalog -l app=postgres

# Check database logs
kubectl logs -f deployment/postgres -n product-catalog

# Test database connectivity
kubectl run -it --rm psql --image=postgres:15-alpine --restart=Never -- \
  psql -h postgres.product-catalog.svc.cluster.local \
       -U postgres \
       -d product_catalog_db \
       -c "SELECT 1"
```

#### Solutions

**Fix Connection String**
```bash
# Verify connection URL in secrets
kubectl get secret product-catalog-secrets -n product-catalog -o jsonpath='{.data.db-url}' | base64 -d

# Update if needed
kubectl patch secret product-catalog-secrets -n product-catalog \
  --type merge -p '{"stringData":{"db-url":"new-connection-url"}}'
```

**Run Database Migration Manually**
```bash
# Port-forward to database
kubectl port-forward svc/postgres 5432:5432 -n product-catalog &

# Run Liquibase
./gradlew update -Dspring.liquibase.url=jdbc:postgresql://localhost:5432/product_catalog_db

# Check migration status
kubectl exec -it <postgres-pod> -n product-catalog -- \
  psql -U postgres -d product_catalog_db -c "SELECT * FROM databasechangelog;"
```

**Fix Permission Issues**
```bash
# Create database user if missing
kubectl exec -it <postgres-pod> -n product-catalog -- \
  psql -U postgres -c "CREATE USER app_user WITH PASSWORD 'password';"

# Grant permissions
kubectl exec -it <postgres-pod> -n product-catalog -- \
  psql -U postgres -d product_catalog_db \
       -c "GRANT ALL PRIVILEGES ON SCHEMA public TO app_user;"
```

### 5. Health Check Failures

#### Symptoms
- Pods not ready (Readiness probe failing)
- Pods being restarted (Liveness probe failing)

#### Investigation
```bash
# Check health endpoint
kubectl port-forward svc/product-catalog 8087:8087 -n product-catalog &
curl http://localhost:8087/actuator/health

# Check detailed health
curl http://localhost:8087/actuator/health/liveness
curl http://localhost:8087/actuator/health/readiness

# Check probe configuration
kubectl get deployment product-catalog -n product-catalog -o yaml | grep -A 10 livenessProbe:
```

#### Solutions

**Adjust Probe Timing**
```bash
# Edit deployment
kubectl edit deployment product-catalog -n product-catalog

# Update probe timings:
# livenessProbe:
#   initialDelaySeconds: 60  # Increase if app takes time to start
#   periodSeconds: 10
#   failureThreshold: 3
```

**Fix Application Startup Issues**
```bash
# Check application logs
kubectl logs deployment/product-catalog -n product-catalog

# Look for initialization errors
kubectl logs deployment/product-catalog -n product-catalog | grep -i error

# Check database availability during startup
kubectl logs deployment/product-catalog -n product-catalog | grep -i database
```

### 6. Deployment Rollout Issues

#### Symptoms
- Deployment stuck in update
- Pods not rolling out correctly
- Old pods not terminating

#### Investigation
```bash
# Check rollout status
kubectl rollout status deployment/product-catalog -n product-catalog

# View rollout history
kubectl rollout history deployment/product-catalog -n product-catalog

# Describe deployment
kubectl describe deployment product-catalog -n product-catalog
```

#### Solutions

**Force Rollout**
```bash
# Restart deployment
kubectl rollout restart deployment/product-catalog -n product-catalog

# Wait for rollout
kubectl rollout status deployment/product-catalog -n product-catalog --timeout=5m

# Rollback if needed
kubectl rollout undo deployment/product-catalog -n product-catalog

# Rollback to specific revision
kubectl rollout undo deployment/product-catalog \
  --to-revision=2 -n product-catalog
```

**Fix PDB (Pod Disruption Budget) Conflicts**
```bash
# Check PDB
kubectl get pdb -n product-catalog

# Verify pod eviction is allowed
kubectl get pdb product-catalog-pdb -n product-catalog -o yaml

# Temporarily delete PDB if necessary
kubectl delete pdb product-catalog-pdb -n product-catalog
```

### 7. Network Issues

#### Symptoms
- Pods can't reach external services
- DNS resolution failures
- Network timeouts

#### Investigation
```bash
# Check network policy
kubectl get networkpolicy -n product-catalog

# Test DNS from pod
kubectl exec -it <pod-name> -n product-catalog -- nslookup kubernetes.default

# Test external connectivity
kubectl exec -it <pod-name> -n product-catalog -- \
  wget -O- http://example.com

# Check service discovery
kubectl exec -it <pod-name> -n product-catalog -- \
  nslookup postgres.product-catalog.svc.cluster.local
```

#### Solutions

**Fix DNS Issues**
```bash
# Check CoreDNS pods
kubectl get pods -n kube-system -l k8s-app=kube-dns

# Check DNS configuration
kubectl get configmap coredns -n kube-system -o yaml

# Restart CoreDNS if needed
kubectl rollout restart deployment/coredns -n kube-system
```

**Update Network Policies**
```bash
# Check current policies
kubectl get networkpolicy -n product-catalog -o yaml

# Update to allow required traffic
kubectl edit networkpolicy product-catalog-egress -n product-catalog

# Test after update
kubectl exec -it <pod-name> -n product-catalog -- wget -O- http://external-url
```

### 8. Monitoring and Logging Issues

#### Symptoms
- No metrics available
- Logs not appearing
- Prometheus can't scrape metrics

#### Investigation
```bash
# Check ServiceMonitor
kubectl get servicemonitor -n product-catalog

# Check Prometheus scrape status
kubectl port-forward svc/prometheus 9090:9090 -n monitoring &
# Visit http://localhost:9090/targets

# Check pod logs
kubectl logs deployment/product-catalog -n product-catalog --tail=50

# Check if metrics endpoint responds
kubectl port-forward svc/product-catalog 8087:8087 -n product-catalog &
curl http://localhost:8087/actuator/prometheus
```

#### Solutions

**Enable Prometheus Metrics**
```bash
# Ensure metrics endpoint is enabled in application
# Check application.yml for:
# management:
#   endpoints:
#     web:
#       exposure:
#         include: health,info,metrics,prometheus

# Update if needed
kubectl patch configmap product-catalog-config \
  --type merge -p '{"data":{"MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE":"health,info,metrics,prometheus"}}' \
  -n product-catalog
```

**Fix Log Aggregation**
```bash
# Check ELK/CloudWatch configuration
kubectl get configmap -n product-catalog

# Verify logging sidecar (if configured)
kubectl get pods -n product-catalog -o json | \
  jq '.items[].spec.containers[].name'
```

## Quick Diagnosis Script

```bash
#!/bin/bash
NAMESPACE=product-catalog
DEPLOYMENT=product-catalog

echo "=== Cluster Status ==="
kubectl cluster-info

echo "=== Node Status ==="
kubectl get nodes

echo "=== Namespace Status ==="
kubectl get namespace $NAMESPACE

echo "=== Pods Status ==="
kubectl get pods -n $NAMESPACE

echo "=== Pod Details ==="
kubectl describe pod -n $NAMESPACE -l app=$DEPLOYMENT

echo "=== Service Status ==="
kubectl get svc -n $NAMESPACE

echo "=== Service Endpoints ==="
kubectl get endpoints -n $NAMESPACE

echo "=== Deployment Status ==="
kubectl describe deployment $DEPLOYMENT -n $NAMESPACE

echo "=== Recent Logs ==="
kubectl logs -n $NAMESPACE --all-containers=true -l app=$DEPLOYMENT --tail=50

echo "=== Events ==="
kubectl get events -n $NAMESPACE --sort-by='.lastTimestamp'

echo "=== Resource Usage ==="
kubectl top pods -n $NAMESPACE
kubectl top nodes
```

## Useful Debugging Commands

```bash
# Execute command in pod
kubectl exec -it <pod-name> -n product-catalog -- /bin/sh

# Copy file from pod
kubectl cp product-catalog/<pod-name>:/path/to/file /local/path

# View resource definitions
kubectl get <resource> -n product-catalog -o yaml

# Validate manifests
kubectl apply -f k8s/base/ --dry-run=client

# Get resource usage
kubectl describe nodes
kubectl describe node <node-name>

# Monitor pod logs in real-time
kubectl logs -f deployment/product-catalog -n product-catalog

# Compare current state with manifest
kubectl diff -f k8s/base/deployment.yaml -n product-catalog
```

---

**Last Updated:** 2025-12-30

