# Quick Reference - Kubernetes Commands Cheat Sheet

## Cluster and Context Management

```bash
# View current context
kubectl config current-context

# List all contexts
kubectl config get-contexts

# Switch context
kubectl config use-context my-cluster

# View cluster info
kubectl cluster-info

# Get nodes
kubectl get nodes
kubectl get nodes -o wide
kubectl top nodes
```

## Namespace Management

```bash
# Create namespace
kubectl create namespace product-catalog

# Switch default namespace
kubectl config set-context --current --namespace=product-catalog

# View all namespaces
kubectl get namespaces

# Get resources in namespace
kubectl get all -n product-catalog

# Delete namespace (cascades to all resources)
kubectl delete namespace product-catalog
```

## Deployment Management

```bash
# Deploy from manifests
kubectl apply -f deployment.yaml
kubectl apply -k k8s/overlays/production

# View deployments
kubectl get deployments -n product-catalog
kubectl describe deployment product-catalog -n product-catalog

# Check rollout status
kubectl rollout status deployment/product-catalog -n product-catalog

# View rollout history
kubectl rollout history deployment/product-catalog -n product-catalog

# Rollback deployment
kubectl rollout undo deployment/product-catalog -n product-catalog

# Restart deployment
kubectl rollout restart deployment/product-catalog -n product-catalog

# Edit deployment
kubectl edit deployment product-catalog -n product-catalog

# Scale deployment
kubectl scale deployment product-catalog --replicas=5 -n product-catalog

# Set image
kubectl set image deployment/product-catalog \
  product-catalog=my-registry/product-catalog:v2 \
  -n product-catalog
```

## Pod Management

```bash
# Get pods
kubectl get pods -n product-catalog
kubectl get pods -n product-catalog -o wide
kubectl get pods -n product-catalog --watch

# Describe pod
kubectl describe pod <pod-name> -n product-catalog

# View logs
kubectl logs <pod-name> -n product-catalog
kubectl logs -f <pod-name> -n product-catalog            # Follow logs
kubectl logs <pod-name> --previous -n product-catalog    # Previous logs
kubectl logs -l app=product-catalog -n product-catalog   # All pods in label
kubectl logs deployment/product-catalog -n product-catalog  # All pods in deployment

# Execute command in pod
kubectl exec -it <pod-name> -n product-catalog -- /bin/sh
kubectl exec <pod-name> -n product-catalog -- ps aux

# Port forward
kubectl port-forward pod/<pod-name> 8087:8087 -n product-catalog
kubectl port-forward svc/product-catalog 8087:8087 -n product-catalog

# Delete pod
kubectl delete pod <pod-name> -n product-catalog

# Check pod events
kubectl describe pod <pod-name> -n product-catalog
kubectl get events -n product-catalog --sort-by='.lastTimestamp'
```

## Service Management

```bash
# Get services
kubectl get svc -n product-catalog
kubectl get svc -n product-catalog -o wide

# Describe service
kubectl describe svc product-catalog -n product-catalog

# Get service endpoints
kubectl get endpoints product-catalog -n product-catalog

# Create service
kubectl expose deployment product-catalog --type=LoadBalancer --port=8087

# Get LoadBalancer endpoint
kubectl get svc product-catalog-lb -n product-catalog
```

## ConfigMap and Secret Management

```bash
# Get ConfigMaps
kubectl get configmap -n product-catalog

# Create ConfigMap
kubectl create configmap app-config --from-literal=key=value -n product-catalog

# Edit ConfigMap
kubectl edit configmap product-catalog-config -n product-catalog

# Get Secrets
kubectl get secrets -n product-catalog

# Create Secret
kubectl create secret generic my-secret --from-literal=password=mypassword -n product-catalog

# View Secret (base64 decoded)
kubectl get secret product-catalog-secrets -n product-catalog -o jsonpath='{.data.jwt-secret}' | base64 -d

# Patch Secret
kubectl patch secret product-catalog-secrets -n product-catalog \
  --type merge -p '{"stringData":{"jwt-secret":"new-value"}}'
```

## Scaling and Autoscaling

```bash
# Manual scaling
kubectl scale deployment product-catalog --replicas=5 -n product-catalog

# View HPA
kubectl get hpa -n product-catalog
kubectl describe hpa product-catalog-hpa -n product-catalog

# Edit HPA
kubectl edit hpa product-catalog-hpa -n product-catalog

# View resource metrics
kubectl top pods -n product-catalog
kubectl top pods -n product-catalog --sort-by=memory
```

## Ingress and Network

```bash
# Get ingress
kubectl get ingress -n product-catalog
kubectl describe ingress product-catalog-ingress -n product-catalog

# Apply ingress
kubectl apply -f k8s/ingress/ingress.yaml

# Get NetworkPolicies
kubectl get networkpolicy -n product-catalog
kubectl describe networkpolicy product-catalog-ingress -n product-catalog

# View network connectivity
kubectl run -it --rm debug --image=busybox --restart=Never -- \
  wget -O- http://product-catalog:8087/actuator/health
```

## Health Checks and Probes

```bash
# Check pod readiness
kubectl get pods -n product-catalog -o custom-columns=\
NAME:.metadata.name,\
READY:.status.conditions[?(@.type=="Ready")].status,\
RESTARTS:.status.containerStatuses[0].restartCount

# Test health endpoints
kubectl port-forward svc/product-catalog 8087:8087 -n product-catalog &
curl http://localhost:8087/actuator/health
curl http://localhost:8087/actuator/health/liveness
curl http://localhost:8087/actuator/health/readiness
```

## Resource and Quota Management

```bash
# Get resource quotas
kubectl get resourcequota -n product-catalog

# Get pod disruption budgets
kubectl get pdb -n product-catalog
kubectl describe pdb product-catalog-pdb -n product-catalog

# View resource usage
kubectl describe nodes
kubectl describe node <node-name>

# Get resource metrics
kubectl get --raw /apis/metrics.k8s.io/v1beta1/namespaces/product-catalog/pods

# Check resource requests vs usage
kubectl get pods -n product-catalog -o json | \
  jq '.items[] | {name: .metadata.name, \
  requests: .spec.containers[0].resources.requests, \
  limits: .spec.containers[0].resources.limits}'
```

## Monitoring and Observability

```bash
# Get events
kubectl get events -n product-catalog
kubectl get events -n product-catalog --sort-by='.lastTimestamp'

# View metrics (requires metrics-server)
kubectl top pods -n product-catalog
kubectl top nodes

# Check ServiceMonitor
kubectl get servicemonitor -n product-catalog
kubectl describe servicemonitor product-catalog-monitor -n product-catalog

# Check PrometheusRule
kubectl get prometheusrule -n product-catalog
```

## RBAC and Security

```bash
# Get service accounts
kubectl get serviceaccount -n product-catalog

# Get roles
kubectl get roles -n product-catalog
kubectl get clusterroles | grep product-catalog

# Get role bindings
kubectl get rolebindings -n product-catalog
kubectl get clusterrolebindings | grep product-catalog

# Check pod security
kubectl get psp
kubectl get pods -n product-catalog -o jsonpath='{.items[].spec.securityContext}'
```

## Debugging Commands

```bash
# Get cluster information
kubectl cluster-info
kubectl get componentstatuses

# Debug node issues
kubectl describe node <node-name>
kubectl debug node/<node-name> -it --image=ubuntu

# Debug pod issues
kubectl exec -it <pod-name> -n product-catalog -- /bin/sh

# Port forward for testing
kubectl port-forward pod/<pod-name> 8087:8087 -n product-catalog

# Copy files
kubectl cp product-catalog/<pod-name>:/path/to/file /local/path
kubectl cp /local/path product-catalog/<pod-name>:/path/to/file

# Get full manifest
kubectl get deployment product-catalog -n product-catalog -o yaml

# Validate manifest
kubectl apply -f deployment.yaml --dry-run=client

# Diff changes
kubectl diff -f deployment.yaml -n product-catalog
```

## Advanced Operations

```bash
# Get all resources
kubectl get all -n product-catalog

# Watch for changes
kubectl get pods -n product-catalog --watch

# Export/Import resources
kubectl get deployment product-catalog -n product-catalog -o yaml > backup.yaml
kubectl apply -f backup.yaml

# Apply with server-side apply (better for large manifests)
kubectl apply -f deployment.yaml --server-side

# Force delete (use with caution)
kubectl delete pod <pod-name> -n product-catalog --grace-period=0 --force

# Drain node (for maintenance)
kubectl drain <node-name> --ignore-daemonsets --delete-emptydir-data

# Cordon node (prevent new pods)
kubectl cordon <node-name>
kubectl uncordon <node-name>
```

## Kustomize Specific

```bash
# View kustomized output without applying
kubectl kustomize k8s/overlays/production

# Apply kustomized resources
kubectl apply -k k8s/overlays/production

# Dry run
kubectl apply -k k8s/overlays/production --dry-run=client -o yaml

# Build kustomization to file
kustomize build k8s/overlays/production > production.yaml

# Validate kustomization
kustomize build k8s/overlays/production | kubectl apply -f - --dry-run=client
```

## Helm Specific

```bash
# List releases
helm list -n product-catalog

# Search for charts
helm search repo product-catalog

# Install release
helm install product-catalog ./k8s/helm -n product-catalog --create-namespace

# Upgrade release
helm upgrade product-catalog ./k8s/helm -f values-production.yaml -n product-catalog

# Uninstall release
helm uninstall product-catalog -n product-catalog

# Get release values
helm get values product-catalog -n product-catalog

# Get release manifest
helm get manifest product-catalog -n product-catalog

# Test release
helm test product-catalog -n product-catalog

# View chart history
helm history product-catalog -n product-catalog

# Rollback release
helm rollback product-catalog 1 -n product-catalog
```

## Useful Aliases

```bash
# Add to ~/.bashrc or ~/.zshrc
alias k='kubectl'
alias kgp='kubectl get pods'
alias kgpa='kubectl get pods --all-namespaces'
alias kg='kubectl get'
alias kd='kubectl describe'
alias kl='kubectl logs'
alias kex='kubectl exec -it'
alias kgd='kubectl get deployment'
alias ksn='kubectl config set-context --current --namespace'

# Example usage
kgp -n product-catalog
kd pod <pod-name> -n product-catalog
kl deployment/product-catalog -n product-catalog
```

---

**Last Updated:** 2025-12-30

