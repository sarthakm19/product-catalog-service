# Skills, Guidelines & Standards — Product Catalog Service

> This document is the single source of truth for how code is written, structured, tested,
> and shipped in this repository. Update it when a decision changes — do not let it drift.
>
> **Stack:** Java 25 · Spring Boot 4.0 · Gradle · PostgreSQL · Liquibase · MapStruct · JUnit 5 · Mockito

---

## Table of Contents

1. [Architecture Rules](#1-architecture-rules)
2. [Design Patterns](#2-design-patterns)
3. [Coding Guidelines](#3-coding-guidelines)
4. [Code Formatting & Style](#4-code-formatting--style)
5. [Validation Strategy](#5-validation-strategy)
6. [Exception Handling](#6-exception-handling)
7. [Testing Standards](#7-testing-standards)
8. [Logging Standards](#8-logging-standards)
9. [Database & Migration Guidelines](#9-database--migration-guidelines)
10. [API Design Guidelines](#10-api-design-guidelines)
11. [Security Guidelines](#11-security-guidelines)
12. [Git & Commit Guidelines](#12-git--commit-guidelines)
13. [Claude AI Assistant Rules](#13-claude-ai-assistant-rules)

---

## 1. Architecture Rules

### 1.1 Strict 4-Layer Architecture

Every feature **must** follow this flow without skipping layers:

```
HTTP Request
    │
    ▼
Controller           (dto package)        → @Valid, ResponseEntity, Swagger annotations
    │
    ▼  (via Mapper)
Service Interface    (service package)    → interface only; impl in service/impl/
    │
    ▼  (via Mapper)
Repository           (repository package) → Spring Data JPA; no business logic
    │
    ▼
Database             (PostgreSQL)         → schema managed by Liquibase only
```

**Domain objects** (`domain/` package) are the currency between Controller↔Service and Service↔Repository.
They are plain Java objects — never JPA entities, never request/response DTOs.

### 1.2 Layer Responsibilities

| Layer | Package | Allowed To | Must NOT |
|-------|---------|------------|----------|
| Controller | `controller/` | Accept/return DTOs, call mapper, delegate to service | Contain business logic, access repositories, catch exceptions |
| Mapper | `mapper/` | Convert between DTO ↔ Domain ↔ Entity | Contain any logic beyond field mapping |
| Service | `service/impl/` | Business logic, orchestrate repositories, validate rules | Build HTTP responses, access `HttpServletRequest` |
| Repository | `repository/` | Data access via Spring Data JPA | Contain business logic |
| Domain | `domain/` | Hold business state, simple validation methods | Have JPA/HTTP/DTO annotations |
| Entity | `entity/` | JPA mapping only | Contain business logic |
| DTO | `dto/` | API contracts (request/response) | Reference entities or domain objects |

### 1.3 Dependency Direction

Dependencies flow **inward only**:

```
Controller → Service (interface) → Repository
   ↓                ↓
 Mapper           Mapper
```

Controllers depend on service **interfaces**, never on `*Impl` classes directly.
Services depend on repository **interfaces**.

### 1.4 Adding a New Resource

When adding a new resource (e.g., `Supplier`), create **one file per layer**:

1. `entity/Supplier.java` — JPA entity
2. `domain/SupplierDomain.java` — domain object with `isValid()` method
3. `dto/CreateSupplierRequest.java`, `SupplierResponse.java`, etc.
4. `mapper/SupplierMapper.java` — MapStruct interface
5. `repository/SupplierRepository.java` — extends `JpaRepository`
6. `service/SupplierService.java` — interface
7. `service/impl/SupplierServiceImpl.java` — implementation
8. `controller/SupplierController.java` — REST controller
9. `exception/` entries if new exception types are needed
10. `src/main/resources/database/liquibase/changelogs/NNN-add-supplier.xml` — Liquibase changelog

---

## 2. Design Patterns

### 2.1 Patterns in Use

| Pattern | Where | Notes |
|---------|-------|-------|
| **Layered Architecture** | Whole app | See §1 |
| **DTO Pattern** | `dto/` | API contracts isolated from domain/entity |
| **Domain Model** | `domain/` | Business objects with behaviour (`isValid()`, `markOutOfStock()`) |
| **Repository Pattern** | `repository/` | Spring Data JPA; custom queries via `@Query` |
| **Service Layer** | `service/` | Interface + `impl/` implementation |
| **Mapper Pattern** | `mapper/` | MapStruct; three-way: DTO ↔ Domain ↔ Entity |
| **Builder Pattern** | Domain objects | Hand-written static inner `Builder` class |
| **Chain of Responsibility** | Security filter chain | `JwtAuthenticationFilter` in Spring Security chain |
| **Factory** | MapStruct | Mapper implementations generated at compile time |
| **Strategy** | Authentication | `AuthenticationManager` abstraction |

### 2.2 Constructor Injection (mandatory)

Always use constructor injection. Never use `@Autowired` on fields.

```java
// CORRECT
@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }
}

// WRONG — never do this
@Autowired
private ProductRepository productRepository;
```

### 2.3 Builder Pattern for Domain Objects

Domain objects with many fields expose a static `Builder`:

```java
ProductDomain product = ProductDomain.builder()
    .code("PROD-001")
    .name("Widget")
    .basePrice(price)
    .isInStock(true)
    .build();
```

### 2.4 MapStruct Rules

- Mapper interfaces go in `mapper/` package, annotated with `@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)`
- Use `nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE` for partial updates
- Use `@BeanMapping` on patch-style update methods
- Use `@MappingTarget` for in-place entity updates
- Ignore relationship fields (`category`, `catalog`, `reviews`) in entity mappers; set them manually in the service
- Always run a full build (`./gradlew clean build -x test`) after mapper changes — MapStruct generates at compile time

### 2.5 SOLID Principles

| Principle | Enforcement |
|-----------|-------------|
| **SRP** | Controllers → HTTP only; Services → business only; Repos → data only |
| **OCP** | Extend via new service implementations; don't modify existing interfaces |
| **LSP** | Service impls must honour their interface contracts completely |
| **ISP** | Keep service interfaces focused; split if they grow too large |
| **DIP** | Inject interfaces (`ProductService`), not implementations (`ProductServiceImpl`) |

---

## 3. Coding Guidelines

### 3.1 General Rules

- **No Lombok** — this project uses hand-written getters/setters and builders. Do not add Lombok.
- **No `@Autowired` field injection** — use constructor injection (see §2.2).
- **No wildcard imports** — `import com.product.catalog.dto.*` is only acceptable in existing controllers; prefer explicit imports in new code.
- **Immutability** — make fields `final` wherever possible (service class fields, security constants).
- **Null safety** — check for null before dereferencing; use `Optional.orElseThrow()` over `Optional.get()`.
- **Streams** — prefer streams + `Collectors.toList()` for list transformations (Java 25 → can use `toList()` directly).
- **No magic numbers/strings** — extract to constants or configuration.

### 3.2 Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Classes | `PascalCase` | `ProductServiceImpl` |
| Interfaces | `PascalCase` (no `I` prefix) | `ProductService` |
| Methods | `camelCase` | `getProductByCode` |
| Variables | `camelCase` | `productDomain` |
| Constants | `UPPER_SNAKE_CASE` | `MAX_PAGE_SIZE` |
| Packages | `lowercase` | `com.product.catalog.service` |
| DB tables | `snake_case` (plural) | `products`, `categories` |
| DB columns | `snake_case` | `stock_keeping_unit` |
| REST paths | `kebab-case` (plural nouns) | `/api/v1/products`, `/api/v1/catalog-versions` |

### 3.3 Exception Naming

| Exception | HTTP Status | When to throw |
|-----------|------------|---------------|
| `ResourceNotFoundException` | 404 | Entity not found by ID/code |
| `ResourceAlreadyExistsException` | 409 | Duplicate code/identifier |
| `BusinessValidationException` | 400 | Business rule violation (not input validation) |

Use the `(resourceName, fieldName, fieldValue)` constructor:
```java
throw new ResourceNotFoundException("Product", "code", code);
throw new ResourceAlreadyExistsException("Product", "code", product.getCode());
```

### 3.4 Transactional Rules

- Annotate the **service class** with `@Transactional` (applies to all write methods by default).
- Annotate individual **read methods** with `@Transactional(readOnly = true)` to allow DB-level optimisations.
- Never put `@Transactional` on a controller method.

```java
@Service
@Transactional                          // default for writes
public class ProductServiceImpl implements ProductService {

    @Override
    @Transactional(readOnly = true)     // override for reads
    public ProductDomain getProductByCode(String code) { ... }
}
```

### 3.5 Service Method Structure

Follow this order inside each service method:
1. Log entry (info level) with identifying fields
2. Input/business validation
3. Database reads (fetch existing state)
4. Business logic
5. Database write
6. Log success
7. Return domain object

```java
@Override
public ProductDomain createProduct(ProductDomain productDomain) {
    log.info("Creating product with code: {}", productDomain.getCode());   // 1. log

    if (!productDomain.isValid()) {                                          // 2. validate
        throw new BusinessValidationException("Invalid product data");
    }
    if (productRepository.existsByCode(productDomain.getCode())) {
        throw new ResourceAlreadyExistsException("Product", "code", productDomain.getCode());
    }

    Product product = productMapper.domainToEntity(productDomain);           // 3-4. convert
    setProductRelationships(product, productDomain);

    Product saved = productRepository.save(product);                         // 5. write
    log.info("Product created successfully with code: {}", saved.getCode()); // 6. log success

    return productMapper.entityToDomain(saved);                              // 7. return
}
```

### 3.6 Controller Method Structure

```java
@PostMapping
@Operation(summary = "Create a new product")
public ResponseEntity<ProductResponse> createProduct(
        @Valid @RequestBody CreateProductRequest request
) {
    log.info("POST /api/v1/products - code: {}", request.getCode());

    ProductDomain domain = productMapper.createRequestToDomain(request);
    ProductDomain created = productService.createProduct(domain);
    ProductResponse response = productMapper.domainToResponse(created);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

### 3.7 Repository Rules

- Extend `JpaRepository<Entity, IdType>`
- Use derived query methods for simple lookups: `findByCode`, `existsByCode`, `deleteByCode`
- Use `@Query` with JPQL (not native SQL) for complex queries
- Use `@Param` for named parameters in `@Query`
- Never put business logic in repositories

---

## 4. Code Formatting & Style

### 4.1 Indentation & Braces

- **4 spaces** for indentation (no tabs)
- Opening brace on the **same line** (K&R style)
- Closing brace on its own line
- Always use braces, even for single-line `if`/`for`

```java
// CORRECT
if (condition) {
    doSomething();
}

// WRONG
if (condition) doSomething();
```

### 4.2 Line Length

- Soft limit: **120 characters**
- Method parameters: one per line when they exceed the limit, aligned with the opening paren

```java
// CORRECT — long constructor
public ProductServiceImpl(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          CatalogRepository catalogRepository,
                          ProductMapper productMapper,
                          PriceMapper priceMapper) {
```

### 4.3 Blank Lines

- **1 blank line** between methods
- **1 blank line** between logical sections inside a method (with an inline comment)
- **No trailing whitespace**
- File ends with a **single newline**

### 4.4 Imports

- No wildcard imports in new code (`import com.product.catalog.dto.*` is legacy)
- Order: static imports → `java.*` → `javax.*`/`jakarta.*` → third-party → internal
- No unused imports

### 4.5 Javadoc

Add Javadoc on:
- All `public` methods in controllers, services (interfaces), repositories, and mappers
- All exception classes
- Classes with non-obvious purpose

Format:
```java
/**
 * Find product by its unique code.
 *
 * @param code the unique product code
 * @return the product domain object
 * @throws ResourceNotFoundException if no product with the given code exists
 */
ProductDomain getProductByCode(String code);
```

Inline comments for non-obvious logic blocks:
```java
// Parse sort parameter — format: "field,direction" e.g. "name,desc"
String[] sortParams = sort.split(",");
```

### 4.6 Tooling (Future)

When a formatter is adopted, this section will be updated with configuration.
Candidates: **Google Java Format** or **Spotless** with `googleJavaFormat()`.
Until then, follow §4.1–4.5 manually.

---

## 5. Validation Strategy

### 5.1 Three-Layer Validation

| Layer | Tool | Validates |
|-------|------|-----------|
| Controller | Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`, `@Size`, `@Positive`) | Input format, nulls, size constraints |
| Domain | `isValid()` method on domain objects | Business invariants (code + name + price required) |
| Database | Liquibase constraints (`NOT NULL`, `UNIQUE`, FK) | Data integrity as last resort |

### 5.2 DTO Validation Annotations

```java
@NotBlank(message = "Product code is required")
@Size(min = 1, max = 50, message = "Product code must be between 1 and 50 characters")
private String code;

@NotNull(message = "Base price is required")
private PriceDto basePrice;
```

- Use `@NotBlank` (not `@NotNull` + `@NotEmpty`) for String fields
- Provide meaningful error messages in every constraint annotation
- Use `@Valid` on nested objects (e.g., `PriceDto`)

### 5.3 Business Validation in Service

```java
if (!productDomain.isValid()) {
    throw new BusinessValidationException("Invalid product data");
}
if (productRepository.existsByCode(productDomain.getCode())) {
    throw new ResourceAlreadyExistsException("Product", "code", productDomain.getCode());
}
```

---

## 6. Exception Handling

### 6.1 Custom Exception Hierarchy

```
RuntimeException
    ├── ResourceNotFoundException         → HTTP 404
    ├── ResourceAlreadyExistsException    → HTTP 409
    └── BusinessValidationException       → HTTP 400
```

### 6.2 GlobalExceptionHandler Rules

- `@RestControllerAdvice` in `exception/GlobalExceptionHandler.java`
- Every `@ExceptionHandler` method must log at `error` level and return a uniform `ErrorResponse`
- `ErrorResponse` always contains: `timestamp`, `status`, `error`, `message`, `path`
- Never expose stack traces in error responses
- The catch-all `Exception.class` handler must return HTTP 500 with a generic message

### 6.3 ErrorResponse Pattern

```java
ErrorResponse errorResponse = new ErrorResponse(
    LocalDateTime.now(),
    HttpStatus.NOT_FOUND.value(),
    HttpStatus.NOT_FOUND.getReasonPhrase(),
    ex.getMessage(),
    request.getRequestURI()
);
return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
```

---

## 7. Testing Standards

### 7.1 Test Frameworks

- **JUnit 5** (`@Test`, `@BeforeEach`, `@ExtendWith`)
- **Mockito** (`@Mock`, `@InjectMocks`, `@ExtendWith(MockitoExtension.class)`)
- **Spring Boot Test** (`@SpringBootTest`) for integration tests
- **Testcontainers** for DB integration tests (PostgreSQL)
- **H2 in-memory** (PostgreSQL mode) for fast unit/slice tests

### 7.2 Unit Test Structure

Follow **Given / When / Then** with comments:

```java
@Test
void authenticate_WithValidCredentials_ShouldReturnLoginResponse() {
    // Given
    String expectedToken = "jwt.token.here";
    when(authenticationManager.authenticate(any())).thenReturn(authentication);
    when(jwtTokenUtil.generateToken(userDetails)).thenReturn(expectedToken);

    // When
    LoginResponse response = authService.authenticate(loginRequest);

    // Then
    assertNotNull(response);
    assertEquals(expectedToken, response.getToken());
    verify(authenticationManager).authenticate(any());
}
```

### 7.3 Test Naming Convention

`methodName_StateUnderTest_ExpectedBehaviour`

```
createProduct_WithDuplicateCode_ShouldThrowResourceAlreadyExistsException
getProductByCode_WhenNotFound_ShouldThrowResourceNotFoundException
authenticate_WithInvalidCredentials_ShouldThrowBadCredentialsException
```

### 7.4 Test Class Placement

- Unit tests: `src/test/java/.../service/impl/` or alongside the class being tested
- Integration tests: suffix with `IT` (e.g., `ProductControllerIT`)
- Test resources in `src/test/resources/application.yml`

### 7.5 What to Test

| Layer | Test Type | What |
|-------|-----------|------|
| Service | Unit | All business logic paths, all exception cases |
| Controller | Slice (`@WebMvcTest`) | Request/response mapping, validation, HTTP status codes |
| Repository | Slice (`@DataJpaTest`) | Custom queries, pagination |
| Security | Unit | `JwtTokenUtil` (generate, validate, expire) |
| Integration | `@SpringBootTest` + Testcontainers | End-to-end happy paths |

### 7.6 Coverage Expectations

- Service layer: **≥ 80% line coverage**
- Critical paths (auth, product CRUD): **100% branch coverage**

---

## 8. Logging Standards

### 8.1 Logger Declaration

Always declare a `private static final` SLF4J logger:

```java
private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);
```

Never use `System.out.println`.

### 8.2 Log Levels

| Level | When to use |
|-------|-------------|
| `log.error(...)` | Exceptions caught in `GlobalExceptionHandler` |
| `log.warn(...)` | JWT validation failures, degraded states |
| `log.info(...)` | Entry to service methods, successful operations |
| `log.debug(...)` | Detailed diagnostic info (DB query parameters, etc.) |

### 8.3 Log Message Format

- Include the operation and the primary identifier:
  ```java
  log.info("Creating product with code: {}", productDomain.getCode());
  log.info("Product created successfully with code: {}", saved.getCode());
  ```
- For error logs that include an exception, pass the exception as the last argument:
  ```java
  log.error("Unexpected error occurred: ", ex);  // full stack trace
  log.error("Resource not found: {}", ex.getMessage());  // message only
  ```
- Never log sensitive data (passwords, tokens, PII).

### 8.4 Environment Log Levels

| Environment | Format | Level |
|-------------|--------|-------|
| Local / Dev | Plain text | `DEBUG` |
| Staging | Structured JSON | `INFO` |
| Production | Structured JSON | `WARN` |

---

## 9. Database & Migration Guidelines

### 9.1 Liquibase Rules

- **Never** use `ddl-auto: create`, `update`, or `create-drop` in non-test environments
- `ddl-auto: none` is the only valid value for `default` and `kubernetes` profiles
- All schema changes go through Liquibase XML changelogs in `src/main/resources/database/liquibase/changelogs/`
- Changelog file naming: `NNN-description.xml` (e.g., `003-add-supplier-table.xml`)
- Register new changelogs in `db.changelog-master.xml`
- Never modify an existing applied changelog — create a new one instead
- Always include a `rollback` section for DDL changes

### 9.2 Migration Checklist

Before creating a migration:
- [ ] Filename follows `NNN-description.xml` convention
- [ ] `changeSet` has `id` and `author` attributes
- [ ] All `NOT NULL` columns have `defaultValueComputed` or are added with data migration
- [ ] `rollback` block is present for DDL
- [ ] Migration tested locally with `./gradlew bootRun` before committing

### 9.3 Entity Design

- Use `String code` as the natural business key (not surrogate `Long id`) for entities that have a meaningful code (Product, Category, Catalog)
- Use `@Embedded` for value objects (e.g., `Price`)
- Use `FetchType.LAZY` for all `@ManyToOne` and `@OneToMany` associations
- Use `CascadeType.ALL` + `orphanRemoval = true` for owned collections (e.g., `Product.reviews`)
- Provide `addXxx()` and `removeXxx()` helper methods to maintain bidirectional consistency

---

## 10. API Design Guidelines

### 10.1 URL Conventions

- Base path: `/api/v1/`
- Resources: plural nouns, kebab-case: `/api/v1/products`, `/api/v1/catalog-versions`
- Sub-resources: `/api/v1/products/{code}/reviews`
- Batch: `/api/v1/products/batch` (POST to create, DELETE to remove)

### 10.2 HTTP Method → Operation Mapping

| HTTP Method | Operation | Success Code |
|-------------|-----------|-------------|
| `GET` | Read (single / collection) | 200 |
| `POST` | Create | 201 |
| `PUT` | Full update | 200 |
| `PATCH` | Partial update | 200 |
| `DELETE` | Delete | 204 (no body) |

### 10.3 Pagination

- Default page size: 20 (configurable via `size` param)
- Sort param format: `field,direction` (e.g., `code,asc`)
- Return `ProductPageResponse` with: `content`, `pageNumber`, `pageSize`, `totalElements`, `totalPages`, `last`

### 10.4 OpenAPI / Swagger Annotations

Every controller **must** have:
```java
@Tag(name = "Products", description = "Product management operations")
@SecurityRequirement(name = "bearerAuth")
```

Every endpoint **must** have:
```java
@Operation(summary = "Short one-line description")
```

Path and query parameters **must** have:
```java
@Parameter(description = "Product code", required = true)
```

### 10.5 Response Envelope

Never return raw entities. Always map through: `Entity → Domain → Response DTO`.

---

## 11. Security Guidelines

### 11.1 JWT Rules

- Secret from `JWT_SECRET` env var — never hardcode production secrets
- Algorithm: HS256 (HMAC-SHA256); migration to RS256 is tracked in ADR-002
- Expiration: 24 hours (configurable via `JWT_EXPIRATION` env var)
- Token format: `Authorization: Bearer <token>`

### 11.2 Public vs Protected Endpoints

| Path Pattern | Auth Required |
|-------------|--------------|
| `POST /api/v1/auth/login` | No |
| `/swagger-ui/**`, `/v3/api-docs/**` | No |
| `/actuator/**` | No (internal monitoring) |
| Everything else | Yes — valid JWT |

### 11.3 Security Do-Nots

- Never log JWT tokens or passwords
- Never return stack traces to clients
- Never store credentials in code — use environment variables
- Never skip JWT validation for convenience in non-test code
- Never disable CSRF protection without documenting why (stateless JWT is the reason here)

---

## 12. Git & Commit Guidelines

### 12.1 Branch Strategy

| Branch | Purpose | Deploys to |
|--------|---------|-----------|
| `develop` | Feature integration | dev environment |
| `staging` | Pre-production validation | staging environment |
| `main` | Production-ready code | production environment |

Feature branches: `feature/<short-description>` (e.g., `feature/add-supplier-resource`)
Bug fixes: `fix/<short-description>` (e.g., `fix/product-patch-null-category`)
Chores/infra: `chore/<short-description>` (e.g., `chore/update-k8s-hpa-limits`)

### 12.2 Conventional Commits

All commit messages **must** follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary in imperative mood>

[optional body]

[optional footer(s)]
```

**Types:**

| Type | When |
|------|------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation only changes |
| `style` | Formatting, missing semicolons (no logic change) |
| `refactor` | Code change that is neither a feature nor a fix |
| `test` | Adding or updating tests |
| `chore` | Build process, tooling, CI/CD, dependency updates |
| `perf` | Performance improvement |
| `ci` | Changes to GitHub Actions workflows |
| `revert` | Reverts a previous commit |

**Scopes (optional but recommended):**
`api`, `auth`, `product`, `mapper`, `db`, `k8s`, `ci`, `security`, `test`, `config`

**Examples:**
```
feat(product): add batch delete endpoint

fix(auth): handle null username in JWT filter

docs(architecture): update C4 container diagram

chore(dev): update image to 692298411961.dkr.ecr.us-east-1.amazonaws.com/base-infrastructure-dev-app:dev-abc1234

test(product): add unit tests for ProductServiceImpl.patchProduct

refactor(mapper): extract price mapping to dedicated PriceMapper

ci: enable trivy security scan step in deploy workflow
```

### 12.3 Commit Rules

- Subject line: **50 characters max**, imperative mood ("add", not "added" or "adds")
- No period at the end of the subject
- Body: wrap at 72 characters; explain *what* and *why*, not *how*
- Reference issues: `Closes #123` or `Refs #456` in footer
- **Do not** include "Co-Authored-By: Claude" in commit messages

### 12.4 PR Guidelines

- PR title follows the same Conventional Commits format as commit messages
- All PRs require passing CI (build + tests) before merge
- Squash small fixup commits before merging to `staging` or `main`
- PRs to `main` must go through `staging` first

### 12.5 What NOT to Commit

- `.env` files or any file containing secrets
- IDE-specific files (`.idea/`, `.vscode/`, `*.iml`) — add to `.gitignore`
- Compiled outputs (`build/`, `*.class`)
- Unintentional `System.out.println` or debugging statements

---

## 13. Claude AI Assistant Rules

> These rules govern how the Claude Code assistant should behave in this repository.
> They supplement (and in case of conflict, override) Claude's default behaviours.

### 13.1 Before Writing Any Code

1. **Always read the relevant existing files first** — never modify code that hasn't been read in the current session
2. **Check the layer** — identify which layer(s) the change touches before writing
3. **Check for existing patterns** — search for how similar things are done (e.g., existing service methods, exception throws, mapper methods)
4. **Verify the architecture is preserved** — new code must fit the 4-layer model

### 13.2 Tools to Always Use

When working on this project, Claude must:

| Situation | Tool/Action |
|-----------|-------------|
| Before editing any `.java` file | Read the file first |
| After writing/modifying Java code | Run `./gradlew test` to verify correctness |
| After adding a new entity | Check Liquibase changelogs are updated |
| When unsure about a mapper | Run `./gradlew clean build -x test` (MapStruct compiles at annotation processing) |
| Before committing | Verify commit message follows Conventional Commits (§12.2) |
| Adding a new resource | Follow the 8-step checklist in §1.4 |

### 13.3 Code Generation Rules

When generating Java code for this project:

- **No Lombok** — write explicit getters, setters, constructors, builders
- **Constructor injection** — never `@Autowired` on fields
- **Logger pattern** — `private static final Logger log = LoggerFactory.getLogger(X.class);`
- **Service interface** — always create `service/XxxService.java` AND `service/impl/XxxServiceImpl.java`
- **Exceptions** — use the three existing exception types; add new ones only if truly needed
- **Transactions** — `@Transactional` on class, `@Transactional(readOnly = true)` on reads
- **Swagger** — `@Tag`, `@Operation`, `@Parameter`, `@SecurityRequirement` on all controllers/methods
- **Validation** — `@Valid` on all `@RequestBody` parameters; validation constraints on DTO fields with messages
- **Return types** — controllers always return `ResponseEntity<Xxx>`

### 13.4 What Claude Must NOT Do

- Add Lombok annotations (`@Data`, `@Builder`, `@Getter`, etc.)
- Skip a layer (e.g., inject a repository directly into a controller)
- Put business logic in controllers or repositories
- Use `@Autowired` field injection
- Return JPA entities directly from controllers
- Create ad-hoc SQL in service methods (use repositories)
- Introduce a breaking change to `ErrorResponse` structure without updating all handlers
- Modify existing Liquibase changelogs that have already been applied
- Add `System.out.println` debugging statements
- Commit secrets or hardcoded credentials
- Use `ddl-auto: update` or `create` in application configuration

### 13.5 Quality Checklist Before Completing Any Task

- [ ] All affected files read before modification
- [ ] Architecture layers respected (no cross-layer violations)
- [ ] New service methods have proper `@Transactional` annotations
- [ ] New endpoints have Swagger `@Operation` and `@Parameter` annotations
- [ ] Input validation present on DTO fields and controller `@RequestBody`
- [ ] Exceptions use existing types with `(resourceName, fieldName, fieldValue)` constructor
- [ ] Logger added if a new class was created
- [ ] Tests written or updated for new/changed behaviour
- [ ] Commit message follows Conventional Commits format
- [ ] No secrets or debug statements in the code