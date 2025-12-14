# Authentication API Documentation

## Overview

The Product Catalog Service uses JWT (JSON Web Token) based authentication for securing API endpoints. This document describes the authentication flow and how to use the authentication endpoints.

## Authentication Endpoint

### POST /api/v1/auth/login

Authenticate a user and receive a JWT token for subsequent API requests.

#### Request

**URL:** `http://localhost:8087/api/v1/auth/login`

**Method:** `POST`

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Request Body Schema:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| username | string | Yes | User's username |
| password | string | Yes | User's password |

#### Response

**Success Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTYzOTU4NzYwMCwiZXhwIjoxNjM5Njc0MDAwfQ.xyz123...",
  "type": "Bearer",
  "expiresIn": 86400
}
```

**Response Schema:**
| Field | Type | Description |
|-------|------|-------------|
| token | string | JWT access token |
| type | string | Token type (always "Bearer") |
| expiresIn | number | Token expiration time in seconds |

**Error Response (401 Unauthorized):**
```json
{
  "timestamp": "2024-12-14T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password",
  "path": "/api/v1/auth/login"
}
```

**Validation Error (400 Bad Request):**
```json
{
  "timestamp": "2024-12-14T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/auth/login"
}
```

## Using the JWT Token

Once you receive the JWT token, include it in the `Authorization` header for all subsequent API requests:

```
Authorization: Bearer <your-jwt-token>
```

### Example cURL Request

**Login:**
```bash
curl -X POST http://localhost:8087/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Using Token:**
```bash
curl -X GET http://localhost:8087/api/v1/products \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTYzOTU4NzYwMCwiZXhwIjoxNjM5Njc0MDAwfQ.xyz123..."
```

## Default Users

For development and testing, the following users are available:

| Username | Password | Description |
|----------|----------|-------------|
| admin | admin123 | Administrator user |
| user | user123 | Regular user |

**⚠️ Warning:** These are demo credentials. In production, implement proper user management with a database.

## Token Properties

- **Algorithm:** HMAC-SHA256 (HS256)
- **Expiration:** 24 hours (86400 seconds)
- **Secret:** Configurable via `jwt.secret` property
- **Claims:** 
  - `sub` (subject): username
  - `iat` (issued at): token creation timestamp
  - `exp` (expiration): token expiration timestamp

## Security Configuration

### Environment Variables

Configure JWT settings via environment variables:

```bash
# Required in production
export JWT_SECRET="your-very-long-secret-key-at-least-256-bits"

# Optional - defaults to 24 hours
export JWT_EXPIRATION=86400000
```

### Application Properties

Alternatively, configure in `application.yml`:

```yaml
jwt:
  secret: ${JWT_SECRET:mySecretKeyForJWTTokenGenerationAndValidationThatIsLongEnoughToMeetRequirements}
  expiration: ${JWT_EXPIRATION:86400000} # 24 hours in milliseconds
```

## Protected Endpoints

All endpoints under `/api/v1/products` require authentication. The following endpoints are publicly accessible:

- `/api/v1/auth/**` - Authentication endpoints
- `/actuator/health` - Health check
- `/actuator/info` - Application info
- `/v3/api-docs/**` - OpenAPI documentation
- `/swagger-ui/**` - Swagger UI

## Token Validation

The service validates tokens on every request by:

1. Extracting the token from the `Authorization` header
2. Verifying the token signature
3. Checking token expiration
4. Extracting the username from token claims
5. Loading user details
6. Setting Spring Security context

## Error Handling

### Common Authentication Errors

| Status Code | Error | Description | Solution |
|-------------|-------|-------------|----------|
| 401 | Unauthorized | Invalid credentials | Check username/password |
| 401 | Unauthorized | Token expired | Login again to get new token |
| 401 | Unauthorized | Invalid token | Ensure token is correctly formatted |
| 403 | Forbidden | Insufficient permissions | User lacks required permissions |
| 400 | Bad Request | Missing/invalid fields | Check request body format |

## Best Practices

1. **Store tokens securely:** Use secure storage (e.g., HttpOnly cookies, secure localStorage)
2. **Handle token expiration:** Implement token refresh logic before expiration
3. **Use HTTPS:** Always use HTTPS in production to prevent token interception
4. **Rotate secrets:** Regularly rotate JWT secret keys in production
5. **Validate input:** Always validate username/password format
6. **Rate limiting:** Implement rate limiting on login endpoint to prevent brute force
7. **Monitor:** Log and monitor authentication failures

## Testing with Postman/Bruno

Import the provided Postman collection (`product-catalog-service.postman_collection.json`) which includes:

1. Pre-configured authentication request
2. Environment variables for token storage
3. Automatic token inclusion in subsequent requests

### Steps:

1. Run the "Auth - Login" request
2. Token is automatically saved to `{{jwtToken}}` variable
3. Use `{{jwtToken}}` in Authorization headers for other requests

## Architecture Notes

### Design Patterns Used

- **Service Layer Pattern:** Authentication logic in `AuthService`
- **Strategy Pattern:** JWT token generation/validation in `JwtTokenUtil`
- **Filter Pattern:** `JwtAuthenticationFilter` intercepts requests
- **Constructor Injection:** Immutable dependencies
- **DTO Pattern:** Separate request/response objects

### Components

```
┌─────────────────┐
│  AuthController │  ← REST endpoint
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   AuthService   │  ← Business logic
└────────┬────────┘
         │
         ├─────────────────┐
         ▼                 ▼
┌──────────────────┐  ┌──────────────┐
│ AuthManager      │  │ JwtTokenUtil │
│ (Spring Security)│  │              │
└──────────────────┘  └──────────────┘
```

### Thread Safety

All authentication components are thread-safe:
- `JwtTokenUtil` uses stateless operations
- `AuthService` has no mutable state
- Spring Security manages context per-thread

### Performance Considerations

- Token validation: O(1) time complexity
- No database lookup on every request (stateless)
- Token caching not needed (validation is fast)
- Consider Redis for token blacklisting if needed

## Troubleshooting

### "Invalid username or password"

- Verify credentials are correct
- Check user exists in `CustomUserDetailsService`
- Ensure password encoding matches

### "Token validation failed"

- Check JWT secret matches between token generation and validation
- Verify token hasn't expired
- Ensure token format is correct (Bearer + space + token)

### "Cannot resolve symbol 'AuthenticationManager'"

- Ensure `SecurityConfig` exposes `AuthenticationManager` bean
- Check Spring Security dependencies are present

## Future Enhancements

Potential improvements for production:

1. **Refresh Tokens:** Implement refresh token mechanism
2. **Token Blacklist:** Add token revocation support
3. **OAuth2/OIDC:** Integration with external identity providers
4. **Multi-factor Auth:** Add 2FA support
5. **User Management:** Database-backed user storage
6. **Role-based Access:** Implement fine-grained permissions
7. **Password Policies:** Enforce strong password requirements
8. **Account Lockout:** Prevent brute force attacks

## Support

For issues or questions:
- Check logs: Look for authentication-related errors
- Review configuration: Ensure JWT properties are set correctly
- Consult documentation: QUICK_START.md, README.md
- Contact: support@productcatalog.com

