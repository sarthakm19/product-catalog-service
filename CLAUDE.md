# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build (skip tests)
./gradlew clean build -x test

# Build with tests
./gradlew clean build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.product.catalog.service.impl.AuthServiceImplTest"

# Run the application (requires PostgreSQL running locally)
export JWT_SECRET=your-very-secret-key
./gradlew bootRun

# Run with Docker Compose (app + postgres + redis + pgadmin)
docker-compose up -d --build
```

## Local Development Setup

The app runs on port `8087`. Swagger UI is at `http://localhost:8087/swagger-ui.html`.

For local dev without Docker Compose, start Postgres first:
```bash
docker run --name product-catalog-db \
  -e POSTGRES_DB=product_catalog_db -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password -p 5432:5432 -d postgres:15
```

Tests use H2 in-memory (PostgreSQL compatibility mode) — no external DB needed to run tests.

## Architecture

### Layer Structure

The service uses a strict 4-layer architecture with explicit domain objects separating API from persistence:

```
Controller → Mapper → Service → Mapper → Repository
   (DTO)    (Domain)           (Entity)
```

- **`controller/`** — REST layer only; delegates to service via `ProductMapper` to convert DTOs to `ProductDomain`
- **`domain/`** — Intermediate business objects (`ProductDomain`, `PriceDomain`); decouples API contracts from JPA entities
- **`service/`** — Business logic interfaces; `impl/` contains implementations (`ProductServiceImpl`, `AuthServiceImpl`)
- **`mapper/`** — MapStruct compile-time mappers (`ProductMapper`, `PriceMapper`); three-way mapping: DTO ↔ Domain ↔ Entity
- **`entity/`** — JPA entities (`Product`, `Category`, `Catalog`, `Review`); `Price` is an embedded value object
- **`repository/`** — Spring Data JPA repositories; no custom query logic beyond what Spring generates
- **`security/`** — JWT filter chain: `JwtAuthenticationFilter` → `JwtTokenUtil` + `CustomUserDetailsService`; users are in-memory (admin/admin123, user/user123)
- **`exception/`** — `GlobalExceptionHandler` (@ControllerAdvice) maps all exceptions to a uniform `ErrorResponse`

### Key Architectural Decisions

- **Users are in-memory** (not DB-backed) — `CustomUserDetailsService` hardcodes credentials. This is known technical debt (ADR-003).
- **JWT is HS256 symmetric** — secret comes from `JWT_SECRET` env var; default dev value is baked into `application.yml`.
- **Hibernate DDL is disabled** (`ddl-auto: none`) — schema is fully managed by Liquibase changelogs in `src/main/resources/database/liquibase/`.
- **MapStruct** generates mapper implementations at compile time via annotation processing. If mappers seem missing, run a full build.
- **`ProductController`** uses `@Valid` on request DTOs; business-rule validation (duplicate codes, category/catalog existence) happens in `ProductServiceImpl`.

### Spring Profiles

| Profile | When used |
|---------|-----------|
| `default` | Local development |
| `kubernetes` | EKS deployment (activated via ConfigMap) |

### Database

- Local: PostgreSQL 15 at `localhost:5432/product_catalog_db`
- Production: AWS RDS PostgreSQL (Multi-AZ)
- Changelogs: `001-create-initial-schema.xml`, `002-insert-sample-data.xml`
- Liquibase runs on startup; `drop-first: true` in test config resets schema before each test run

## Infrastructure / Deployment

CI/CD flow: GitHub Actions builds and pushes to AWS ECR, then commits the new image tag to `k8s/overlays/{env}/kustomization.yaml`. ArgoCD detects the Git change and syncs to EKS.

- **Branches**: `develop` → dev, `staging` → staging, `main` → production
- **Kustomize**: Base manifests in `k8s/base/`; per-env patches in `k8s/overlays/{dev,staging,production}/`
- **Secrets**: Managed by External Secrets Operator pulling from AWS Secrets Manager
- **Scaling**: HPA 3–10 pods (CPU 70%, memory 80%); PDB minimum 2 available

### Key Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8087` | Application port |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `password` | Database password |
| `JWT_SECRET` | (dev default in application.yml) | JWT signing key |
| `JWT_EXPIRATION` | `86400000` | Token TTL in ms (24h) |