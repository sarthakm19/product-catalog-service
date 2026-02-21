# Troubleshooting Runbook

Symptom-indexed guide for diagnosing and fixing issues with the Product Catalog Service. Each entry follows: **Symptom → Cause → Fix**.

---

## Table of Contents

1. [Health Probes & Readiness](#1-health-probes--readiness)
2. [ArgoCD Sync Failures](#2-argocd-sync-failures)
3. [Database & Schema Issues](#3-database--schema-issues)
4. [Deployment & Image Issues](#4-deployment--image-issues)
5. [Networking & Load Balancer](#5-networking--load-balancer)
6. [Authentication Errors](#6-authentication-errors)
7. [General Kubernetes Debugging](#7-general-kubernetes-debugging)

---

## 1. Health Probes & Readiness

### 1.1 ArgoCD stuck on "Progressing" — pods never become Ready

**Symptom:** Application logs show successful startup, but ArgoCD shows yellow/in-progress. Pods show `READY 0/1`.

**Cause:** Spring Security was blocking `/actuator/health/liveness` and `/actuator/health/readiness`. Only `/actuator/health` was permitted.

**Fix:**
1. In `SecurityConfig.java`, allow all actuator endpoints:
   ```java
   .requestMatchers("/actuator/**").permitAll()
   ```
2. In `application.yml`, enable Kubernetes probes:
   ```yaml
   management:
     endpoint:
       health:
         probes:
           enabled: true
     health:
       livenessState:
         enabled: true
       readinessState:
         enabled: true
   ```
3. In `k8s/base/configmap.yaml`, add:
   ```yaml
   MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED: "true"
   MANAGEMENT_HEALTH_LIVENESSSTATE_ENABLED: "true"
   MANAGEMENT_HEALTH_READINESSSTATE_ENABLED: "true"
   ```

### 1.2 `strconv.Atoi: parsing "http": invalid syntax`

**Symptom:** Startup probe errors in ArgoCD with the above message.

**Cause:** Using named port references (`port: http`) in health probes. Kustomize strategic merge patches can lose the context, causing Kubernetes to try parsing "http" as an integer.

**Fix:** Use numeric port values in all probe definitions in `k8s/base/deployment.yaml`:
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8087        # Use numeric, not 'http'
    scheme: HTTP
```
Also ensure overlay patches include the complete `httpGet` block, not just timing parameters.

### 1.3 Connection Refused on startup probe

**Symptom:** `Startup probe failed: dial tcp <ip>:8087: connect: connection refused`

**Cause:** Application hasn't finished starting within the probe's initial delay.

**Fix:** Increase startup probe tolerance in `k8s/base/deployment.yaml`:
```yaml
startupProbe:
  initialDelaySeconds: 30
  periodSeconds: 5
  failureThreshold: 50   # Allows up to 280s for startup
```

### 1.4 `403 Forbidden` on readiness probe

**Symptom:** `Readiness probe failed: HTTP probe failed with statuscode: 403`

**Cause:** Old Docker image running with previous SecurityConfig that blocks actuator sub-endpoints.

**Fix:** Build and push a new Docker image with the updated SecurityConfig, then restart pods:
```bash
kubectl rollout restart deployment/product-catalog -n product-catalog
```

### 1.5 Testing health endpoints

```bash
# From inside a pod
kubectl exec -n product-catalog <pod> -- curl -s http://localhost:8087/actuator/health
kubectl exec -n product-catalog <pod> -- curl -s http://localhost:8087/actuator/health/liveness
kubectl exec -n product-catalog <pod> -- curl -s http://localhost:8087/actuator/health/readiness

# Check pod events for probe failures
kubectl describe pod <pod> -n product-catalog | grep -A 10 Events

# Expected healthy response
# {"status":"UP","groups":["liveness","readiness"]}
```

---

## 2. ArgoCD Sync Failures

### 2.1 `PodSecurityPolicy not found` CRD error

**Symptom:** `The Kubernetes API could not find policy/PodSecurityPolicy for requested resource`

**Cause:** PodSecurityPolicy was removed in Kubernetes 1.25+. The cluster no longer supports the PSP API.

**Fix:** Migrate to Pod Security Standards (PSS):
1. Remove any `PodSecurityPolicy` resource files from `k8s/overlays/production/`
2. Add PSS labels to `k8s/base/namespace.yaml`:
   ```yaml
   labels:
     pod-security.kubernetes.io/enforce: restricted
     pod-security.kubernetes.io/audit: restricted
     pod-security.kubernetes.io/warn: restricted
   ```
3. Ensure deployment container runs as non-root with security context:
   ```yaml
   securityContext:
     runAsNonRoot: true
     runAsUser: 1000
     allowPrivilegeEscalation: false
     capabilities:
       drop: ["ALL"]
   ```

### 2.2 Secret `container-registry` invalid JSON

**Symptom:** `Secret "container-registry" is invalid: data[.dockerconfigjson]: unexpected end of JSON input`

**Cause:** The `container-registry` secret in `k8s/base/secret.yaml` had an empty `.dockerconfigjson` value.

**Fix:** Either populate with valid ECR credentials or remove the secret if using IAM-based authentication (IRSA):
```bash
# If using ECR with IRSA, remove the secret entirely
# If credentials are needed:
kubectl create secret docker-registry container-registry \
  --docker-server=<account>.dkr.ecr.<region>.amazonaws.com \
  --docker-username=AWS \
  --docker-password=$(aws ecr get-login-password) \
  -n product-catalog
```

### 2.3 ArgoCD YAML corruption

**Symptom:** Sync fails with YAML parse errors.

**Fix:** Validate YAML before committing:
```bash
# Validate kustomize output
kustomize build k8s/overlays/production

# Validate with kubectl
kubectl apply -k k8s/overlays/production --dry-run=client
```

### 2.4 GitHub Actions workflow not triggering

**Symptom:** Pushing to `k8s/` directory doesn't trigger the deploy workflow.

**Cause:** Workflow `paths` filter only matches `src/**`, `Dockerfile`, `build.gradle`.

**Fix:** Add `k8s/**` to the paths filter in `.github/workflows/deploy.yml`:
```yaml
paths:
  - 'src/**'
  - 'Dockerfile'
  - 'build.gradle'
  - 'k8s/**'
  - '.github/workflows/deploy.yml'
```

---

## 3. Database & Schema Issues

### 3.1 `Schema-validation: missing table [catalogs]`

**Symptom:** Application fails on startup with `SchemaManagementException: missing table`.

**Cause:** Hibernate `ddl-auto` set to `validate` — it validates schema before Liquibase has a chance to create tables.

**Fix:** Set `ddl-auto: none` and let Liquibase manage everything:
```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: none
  liquibase:
    drop-first: false

# k8s/base/configmap.yaml
SPRING_JPA_HIBERNATE_DDL_AUTO: "none"
SPRING_LIQUIBASE_DROP_FIRST: "false"
```

### 3.2 Init container stuck: "Waiting for PostgreSQL to be ready..."

**Symptom:** Pod stuck in `Init:0/1` state indefinitely.

**Cause:** Init container tries to connect to `postgres.product-catalog.svc.cluster.local:5432` but no in-cluster PostgreSQL exists (using AWS RDS).

**Fix:** Remove the init container from `k8s/base/deployment.yaml`. AWS RDS is always available, and Spring Boot's HikariCP handles connection retries automatically:
```yaml
spec:
  template:
    spec:
      # No initContainers needed for AWS RDS
      containers:
        - name: product-catalog
          # ...
```

### 3.3 RDS connection timeouts

**Fix:** Optimize HikariCP settings in `k8s/base/configmap.yaml`:
```yaml
SPRING_DATASOURCE_HIKARI_CONNECTIONTIMEOUT: "30000"
SPRING_DATASOURCE_HIKARI_INITIALIZATIONFAILTIMEOUT: "1"
SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE: "20"
SPRING_DATASOURCE_HIKARI_MINIMUMIDLE: "5"
SPRING_DATASOURCE_HIKARI_KEEPALIVETIME: "60000"
SPRING_DATASOURCE_HIKARI_MAXLIFETIME: "1800000"
```

### 3.4 Debugging database connectivity

```bash
# Check from inside the pod
kubectl exec -n product-catalog <pod> -- nc -zv <rds-endpoint> 5432

# Check secret values
kubectl get secret product-catalog-secrets -n product-catalog -o jsonpath='{.data.db-url}' | base64 -d

# Check Liquibase logs
kubectl logs -n product-catalog <pod> | grep -i liquibase
```

---

## 4. Deployment & Image Issues

### 4.1 Incorrect image after Kustomize transformation

**Symptom:** Pods pull the wrong image or fail with `ImagePullBackOff`.

**Cause:** Kustomize `images` section in overlay has incorrect `newName`/`newTag` configuration.

**Fix:** Ensure the overlay kustomization.yaml uses the correct format:
```yaml
# k8s/overlays/production/kustomization.yaml
images:
  - name: product-catalog          # Must match the image name in deployment.yaml
    newName: <account>.dkr.ecr.<region>.amazonaws.com/product-catalog-service
    newTag: latest                  # Or specific tag like "v1.2.3"
```

Validate with:
```bash
kustomize build k8s/overlays/production | grep "image:"
```

### 4.2 Container won't start

```bash
# Check logs
kubectl logs <pod> -n product-catalog

# Check events
kubectl describe pod <pod> -n product-catalog

# Exec into container
kubectl exec -it <pod> -n product-catalog -- sh

# Check resources
kubectl top pod <pod> -n product-catalog
```

---

## 5. Networking & Load Balancer

### 5.1 ALB Controller IAM permission denied

**Symptom:** `AccessDenied: elasticloadbalancing:DescribeListenerAttributes`

**Cause:** ALB Controller IAM role lacks required permissions.

**Fix:** Update the IAM policy. Use the policy file at `k8s/ALB_CONTROLLER_IAM_POLICY.json`:
```bash
aws iam put-role-policy \
  --role-name <alb-controller-role> \
  --policy-name ALBControllerPolicy \
  --policy-document file://k8s/ALB_CONTROLLER_IAM_POLICY.json
```

### 5.2 Conflicting LoadBalancer and Ingress

**Symptom:** Service type `LoadBalancer` creates an NLB, conflicting with ALB Ingress.

**Cause:** Having both `service.yaml` with `type: LoadBalancer` and an Ingress resource.

**Fix:** Use `ClusterIP` service for internal routing and let the ALB Ingress handle external access:
```yaml
# k8s/base/service.yaml — keep only ClusterIP
spec:
  type: ClusterIP
  ports:
    - port: 8087
      targetPort: 8087
```

### 5.3 Ingress not creating ALB

```bash
# Check ingress status
kubectl get ingress -n product-catalog

# Check ALB controller logs
kubectl logs -n kube-system deployment/aws-load-balancer-controller

# Verify ingress class
kubectl get ingressclass
```

---

## 6. Authentication Errors

### 6.1 `Invalid username or password`

- Verify credentials: default users are `admin/admin123` and `user/user123`
- These are in-memory users defined in `CustomUserDetailsService`

### 6.2 Token expired / invalid

- Tokens expire after 24 hours (default)
- Re-authenticate: `POST /api/v1/auth/login`
- Ensure `Authorization` header format: `Bearer <token>` (with space)

### 6.3 JWT secret mismatch between environments

- Ensure the `JWT_SECRET` environment variable is consistent between token generation and validation
- In Kubernetes, check: `kubectl get secret product-catalog-secrets -n product-catalog -o jsonpath='{.data.jwt-secret}' | base64 -d`

---

## 7. General Kubernetes Debugging

### Useful Commands

```bash
# Pod status
kubectl get pods -n product-catalog -o wide

# Pod events
kubectl describe pod <pod> -n product-catalog

# Real-time logs
kubectl logs -f deployment/product-catalog -n product-catalog

# Previous container logs (after crash)
kubectl logs <pod> -n product-catalog --previous

# Resource usage
kubectl top pods -n product-catalog

# Service endpoints
kubectl get endpoints -n product-catalog

# ConfigMap verification
kubectl get configmap product-catalog-config -n product-catalog -o yaml

# Rollback deployment
kubectl rollout undo deployment/product-catalog -n product-catalog

# Force restart
kubectl rollout restart deployment/product-catalog -n product-catalog
```

### ArgoCD Debugging

```bash
# Application status
argocd app get product-catalog

# Sync status details
argocd app get product-catalog --show-operation

# Resource tree
argocd app resources product-catalog

# Force refresh from Git
argocd app get product-catalog --refresh

# Hard refresh (clear cache)
argocd app get product-catalog --hard-refresh
```

