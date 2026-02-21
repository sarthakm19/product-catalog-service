# Product Catalog Service

A production-ready Product Catalog microservice built with **Spring Boot 4.0.0** and **Java 25**, deployed to **AWS EKS** via **ArgoCD GitOps**. Provides REST APIs for managing products with CRUD operations, pagination, filtering, JWT security, and Liquibase-managed schema.

---

## Documentation Map

| Document | Description |
|----------|-------------|
| **[README.md](README.md)** (this file) | Overview, API reference, quick start |
| **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** | Application + infrastructure architecture, design patterns |
| **[AUTHENTICATION.md](docs/AUTHENTICATION.md)** | JWT auth flow, endpoints, security configuration |
| **[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)** | Docker → Kubernetes → GitOps deployment guide |
| **[docs/DATABASE.md](docs/DATABASE.md)** | Liquibase, RDS config, HikariCP, entity model |
| **[docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)** | Symptom-indexed runbook for all known issues |
| **[k8s/README.md](k8s/README.md)** | Kubernetes manifest reference and file index |

---

## Quick Start

### Option A: Docker Compose

```bash
docker-compose up -d --build
# App: http://localhost:8087/swagger-ui.html
```

### Option B: Local Development

```bash
# 1. Start database
docker run --name product-catalog-db \
  -e POSTGRES_DB=product_catalog_db -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password -p 5432:5432 -d postgres:15

# 2. Build and run
export JWT_SECRET=your-very-secret-key
./gradlew clean build
./gradlew bootRun

# 3. Open http://localhost:8087/swagger-ui.html
```

### Option C: Kubernetes (EKS)

```bash
kubectl apply -k k8s/overlays/production
```

> See **[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)** for complete deployment instructions.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.0 |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| Database | PostgreSQL 15 (AWS RDS in production) |
| ORM | Spring Data JPA / Hibernate |
| Schema Management | Liquibase |
| Object Mapping | MapStruct |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Containerization | Docker (multi-stage, Alpine) |
| Orchestration | Kubernetes (AWS EKS) |
| GitOps | ArgoCD + Kustomize |
| CI/CD | GitHub Actions |
| Monitoring | Spring Actuator + Prometheus |

---

## API Reference

### Authentication

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/auth/login` | Get JWT token | No |

**Default users:** `admin/admin123`, `user/user123`

> See **[AUTHENTICATION.md](docs/AUTHENTICATION.md)** for full details.

### Products

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/products` | List products (paginated, filterable) |
| GET | `/api/v1/products/{code}` | Get product by code |
| POST | `/api/v1/products` | Create product |
| POST | `/api/v1/products/batch` | Create multiple products |
| PUT | `/api/v1/products/{code}` | Full update |
| PATCH | `/api/v1/products/{code}` | Partial update |
| DELETE | `/api/v1/products/{code}` | Delete product |
| DELETE | `/api/v1/products/batch` | Delete multiple products |

All product endpoints require JWT authentication (`Authorization: Bearer <token>`).

### Example Usage

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8087/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# 2. Create product
curl -X POST http://localhost:8087/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "code": "LAPTOP-001",
    "name": "Professional Laptop",
    "description": "High-performance laptop",
    "basePrice": {"value": 1299.99, "currency": "USD"},
    "isInStock": true,
    "stockKeepingUnit": "SKU-LAPTOP-001",
    "categoryCode": "electronics",
    "catalogCode": "main-catalog"
  }'

# 3. Get products with pagination and filtering
curl "http://localhost:8087/api/v1/products?page=0&size=10&sort=name,asc&categoryCode=electronics&inStock=true" \
  -H "Authorization: Bearer $TOKEN"

# 4. Partial update
curl -X PATCH http://localhost:8087/api/v1/products/LAPTOP-001 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"isInStock": false, "basePrice": {"value": 1199.99, "currency": "USD"}}'

# 5. Delete
curl -X DELETE http://localhost:8087/api/v1/products/LAPTOP-001 \
  -H "Authorization: Bearer $TOKEN"
```

---

## Package Structure

```
com.product.catalog/
├── config/          # OpenAPI configuration
├── controller/      # REST controllers (AuthController, ProductController)
├── dto/             # Request/response DTOs
├── domain/          # Domain models (business logic layer)
├── entity/          # JPA entities (persistence layer)
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── mapper/          # MapStruct mappers (DTO ↔ Domain ↔ Entity)
├── repository/      # Spring Data JPA repositories
├── security/        # JWT filter, SecurityConfig, UserDetailsService
└── service/         # Business logic (interfaces + implementations)
```

---

## Error Response Format

All errors return a consistent JSON structure:

```json
{
  "timestamp": "2025-12-14T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Product name is required",
  "path": "/api/v1/products"
}
```

| Status | Meaning |
|--------|---------|
| 200 | Success (GET, PUT, PATCH) |
| 201 | Created (POST) |
| 204 | No Content (DELETE) |
| 400 | Validation error |
| 401 | Authentication required |
| 404 | Resource not found |
| 409 | Resource already exists |
| 500 | Server error |

---

## Monitoring

```bash
# Health check
curl http://localhost:8087/actuator/health

# Liveness (for Kubernetes)
curl http://localhost:8087/actuator/health/liveness

# Readiness (for Kubernetes)
curl http://localhost:8087/actuator/health/readiness

# Prometheus metrics
curl http://localhost:8087/actuator/prometheus

# Liquibase migration status
curl http://localhost:8087/actuator/liquibase
```

---

## Configuration

### Key Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Application port | 8087 |
| `DB_USERNAME` | Database username | postgres |
| `DB_PASSWORD` | Database password | password |
| `JWT_SECRET` | JWT signing secret | (built-in dev default) |
| `JWT_EXPIRATION` | Token expiration (ms) | 86400000 (24h) |

### Profiles

| Profile | Usage |
|---------|-------|
| `default` | Local development |
| `kubernetes` | EKS deployment (set via ConfigMap) |

---

## License

This project is for educational purposes.
