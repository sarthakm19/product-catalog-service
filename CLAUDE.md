# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **Full standards reference:** [`docs/SKILLS.md`](docs/SKILLS.md)
> — covers coding guidelines, design patterns, architecture rules, formatting, testing standards, and git conventions.
> CLAUDE.md holds the quick-reference constraints; SKILLS.md holds the detail.

---

## Hard Constraints (Non-Negotiable)

These rules are enforced on every task, no exceptions:

1. **Read before edit** — always read any file before modifying it in the same session.
2. **4-layer architecture** — Controller → (Mapper) → Service → (Mapper) → Repository. Never skip a layer or cross-inject (e.g., no repository in a controller).
3. **No Lombok** — project uses hand-written getters/setters/builders. Do not add `@Data`, `@Builder`, `@Getter`, `@Setter`, or any other Lombok annotation.
4. **Constructor injection only** — never use `@Autowired` on fields or setters.
5. **No entity in API responses** — always map Entity → Domain → DTO before returning from a controller.
6. **Liquibase manages schema** — `ddl-auto: none` always; never use `create`, `update`, or `create-drop` in non-test profiles. Never edit an already-applied changelog.
7. **Conventional Commits** — all commit messages must follow `type(scope): summary` format. See `docs/SKILLS.md §12`.
8. **No secrets in code** — credentials and JWT secrets come from environment variables only.
9. **Run tests after changes** — run `./gradlew test` after any Java code change to verify nothing is broken.
10. **Swagger on all endpoints** — every new controller class needs `@Tag` + `@SecurityRequirement`; every method needs `@Operation`; every path/query param needs `@Parameter`.

## Always Use These Tools / Commands

| Situation | Command |
|-----------|---------|
| After any Java change | `./gradlew test` |
| After mapper changes | `./gradlew clean build -x test` (MapStruct compiles at annotation processing) |
| Full build with tests | `./gradlew clean build` |
| Single test class | `./gradlew test --tests "com.product.catalog.XxxTest"` |

---

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