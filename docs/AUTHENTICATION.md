# Authentication

JWT-based authentication for the Product Catalog Service — API usage, configuration, and implementation details.

---

## Table of Contents

1. [Quick Start](#1-quick-start)
2. [API Reference](#2-api-reference)
3. [Using JWT Tokens](#3-using-jwt-tokens)
4. [Configuration](#4-configuration)
5. [Implementation Details](#5-implementation-details)
6. [Troubleshooting](#6-troubleshooting)
7. [Production Considerations](#7-production-considerations)

---

## 1. Quick Start

```bash
# Login and get token
curl -X POST http://localhost:8087/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'

# Use token for API calls
curl -X GET http://localhost:8087/api/v1/products \
  -H "Authorization: Bearer <your-token>"
```

### Default Users

| Username | Password | Description |
|----------|----------|-------------|
| admin | admin123 | Administrator |
| user | user123 | Regular user |

> ⚠️ These are in-memory demo credentials. For production, implement database-backed user management.

---

## 2. API Reference

### POST /api/v1/auth/login

**Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Success Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "expiresIn": 86400
}
```

**Error Response (401):**
```json
{
  "timestamp": "2025-12-14T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password",
  "path": "/api/v1/auth/login"
}
```

---

## 3. Using JWT Tokens

Include the token in the `Authorization` header for all protected endpoints:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Public Endpoints (no token required)

- `/api/v1/auth/**` — Authentication
- `/actuator/**` — Health checks and metrics
- `/v3/api-docs/**` — OpenAPI specification
- `/swagger-ui/**` — Swagger UI

### Protected Endpoints (token required)

- `/api/v1/products/**` — All product operations

### Token Properties

| Property | Value |
|----------|-------|
| Algorithm | HMAC-SHA256 (HS256) |
| Expiration | 24 hours (configurable) |
| Claims | `sub` (username), `iat` (issued-at), `exp` (expiration) |
| Validation | Signature + expiration + username match |

### Testing with Swagger UI

1. Open http://localhost:8087/swagger-ui.html
2. Execute `POST /api/v1/auth/login`
3. Copy the `token` value from the response
4. Click **Authorize** button (top right)
5. Enter: `Bearer <your-token>`
6. All subsequent requests include the token automatically

### Testing with Postman

Import `product-catalog-service.postman_collection.json`. The collection includes:
- Pre-configured login request
- Automatic token storage in `{{jwtToken}}` variable
- Token auto-included in subsequent requests

---

## 4. Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | Signing secret (min 256 bits) | Built-in dev default |
| `JWT_EXPIRATION` | Token lifetime in milliseconds | 86400000 (24h) |

### application.yml

```yaml
jwt:
  secret: ${JWT_SECRET:mySecretKeyForJWTTokenGenerationAndValidationThatIsLongEnoughToMeetRequirements}
  expiration: ${JWT_EXPIRATION:86400000}
```

### Kubernetes

JWT secret is stored in the `product-catalog-secrets` Secret and referenced as an environment variable in the Deployment.

---

## 5. Implementation Details

### Component Architecture

```
AuthController          ← Thin REST endpoint, delegates to service
       │
       ▼
AuthServiceImpl         ← Business logic: authenticate + generate token
       │
       ├──────────────────────┐
       ▼                      ▼
AuthenticationManager    JwtTokenUtil
(Spring Security)        (Token operations)
       │
       ▼
CustomUserDetailsService ← In-memory user store
```

### Key Classes

| Class | Responsibility |
|-------|---------------|
| `AuthController` | REST endpoint, request validation, error handling |
| `AuthService` / `AuthServiceImpl` | Authentication logic, security context management |
| `JwtTokenUtil` | Token generation, validation, claim extraction |
| `JwtAuthenticationFilter` | Intercepts requests, validates tokens, sets security context |
| `SecurityConfig` | Filter chain, public/protected endpoint rules |
| `CustomUserDetailsService` | Loads user details for authentication |

### Design Decisions

- **Interface-based service layer** — `AuthService` interface with `AuthServiceImpl` for testability
- **Constructor injection** — All dependencies injected via constructor (no field injection)
- **Stateless** — No server-side sessions; JWT contains all needed info
- **Modern JJWT API (0.12.6)** — Uses `Jwts.parser().verifyWith()`, `parseSignedClaims()`, `SecretKey`

### Test Coverage

- `AuthServiceImplTest` — Valid/invalid credentials, null handling, security context verification
- `JwtTokenUtilTest` — Token generation, extraction, validation, expiration, malformed tokens, wrong secret

---

## 6. Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| "Invalid username or password" | Wrong credentials | Use `admin/admin123` or `user/user123` |
| "Token validation failed" | Expired or malformed token | Re-authenticate to get a new token |
| 401 on product endpoints | Missing/malformed header | Use `Authorization: Bearer <token>` (with space) |
| 403 Forbidden | JWT secret mismatch between envs | Ensure consistent `JWT_SECRET` across pods |

---

## 7. Production Considerations

### Must Do

1. **Set a strong `JWT_SECRET`** — At least 256 bits, rotated regularly
2. **Use HTTPS** — Prevent token interception in transit
3. **Replace in-memory users** — Implement `UserDetailsService` backed by database
4. **Rate-limit `/auth/login`** — Prevent brute-force attacks

### Future Enhancements

- Refresh token mechanism
- OAuth2/OIDC integration with external identity providers
- Role-based access control (RBAC) with fine-grained permissions
- Token blacklisting (Redis) for logout support
- Multi-factor authentication
- Account lockout policies
