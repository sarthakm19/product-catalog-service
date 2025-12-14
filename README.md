# Product Catalog Service - API Documentation

## Overview

This is a comprehensive Product Catalog Service built using **Spring Boot 4.0.0** and **Java 25**, following **API-driven development** and **clean architecture** principles. The service provides REST APIs for managing products with full CRUD operations, pagination, filtering, and JWT-based security.

## Architecture

### Layered Architecture

The application follows a clean, layered architecture pattern:

```
┌─────────────────────────────────────┐
│         Controller Layer            │  ← DTOs for API requests/responses
├─────────────────────────────────────┤
│         Service Layer               │  ← Domain models for business logic
├─────────────────────────────────────┤
│         Repository Layer            │  ← Entities for database
└─────────────────────────────────────┘
```

### Key Design Patterns

1. **API-First Development**: OpenAPI specification defines the contract
2. **DTO Pattern**: Separate DTOs for API layer
3. **Domain Model Pattern**: Business logic operates on domain models
4. **Entity Pattern**: JPA entities for persistence
5. **Mapper Pattern**: MapStruct for layer-to-layer conversions
6. **Repository Pattern**: Spring Data JPA repositories
7. **Service Pattern**: Business logic encapsulation
8. **Strategy Pattern**: For authentication and authorization

### Package Structure

```
com.product.catalog/
├── config/              # Configuration classes (OpenAPI, etc.)
├── controller/          # REST controllers (API layer)
├── dto/                 # Data Transfer Objects (API contracts)
├── domain/              # Domain models (business logic)
├── entity/              # JPA entities (persistence layer)
├── exception/           # Custom exceptions and global handler
├── mapper/              # MapStruct mappers
├── repository/          # Spring Data repositories
├── security/            # JWT security configuration
└── service/             # Business logic services
    └── impl/            # Service implementations
```

## Features

### Core Features

- ✅ **CRUD Operations**: Create, Read, Update, Delete products
- ✅ **Batch Operations**: Create/delete multiple products
- ✅ **Pagination**: Efficient data retrieval with pagination
- ✅ **Filtering**: Filter by category, stock status
- ✅ **Sorting**: Sort by any field (asc/desc)
- ✅ **JWT Authentication**: Secure API endpoints
- ✅ **Validation**: Request validation with meaningful error messages
- ✅ **Exception Handling**: Global exception handler with consistent error responses
- ✅ **API Documentation**: Swagger UI for interactive API testing
- ✅ **Database Migration**: Liquibase for schema management

### Technical Features

- **Spring Boot 4.0.0** with Java 25
- **Spring Security** with JWT authentication
- **Spring Data JPA** with PostgreSQL
- **MapStruct** for object mapping
- **Lombok** for reducing boilerplate
- **SpringDoc OpenAPI** for API documentation
- **Liquibase** for database versioning
- **SLF4J** for logging

## API Endpoints

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/auth/login` | Authenticate and get JWT token | No |

**📖 For detailed authentication documentation, see [AUTHENTICATION.md](AUTHENTICATION.md)**

### Products

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/products` | Get all products (paginated) | Yes |
| GET | `/api/v1/products/{code}` | Get product by code | Yes |
| POST | `/api/v1/products` | Create a new product | Yes |
| POST | `/api/v1/products/batch` | Create multiple products | Yes |
| PUT | `/api/v1/products/{code}` | Update product (full) | Yes |
| PATCH | `/api/v1/products/{code}` | Update product (partial) | Yes |
| DELETE | `/api/v1/products/{code}` | Delete product | Yes |
| DELETE | `/api/v1/products/batch` | Delete multiple products | Yes |

## Getting Started

### Prerequisites

- Java 25 or newer
- Spring Boot 4.0.0
- Gradle 9.x (wrapper included)
- PostgreSQL 13+ (or Docker)

### Setup Database

1. **Using Docker:**
   ```bash
   docker run --name product-catalog-db \
     -e POSTGRES_DB=product_catalog_db \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=password \
     -p 5432:5432 \
     -d postgres:15
   ```

2. **Manual Setup:**
   - Create database: `product_catalog_db`
   - Update credentials in `application.yml` if needed

### Setup & Running

1. Ensure Java 25+ is installed: `java -version`
2. Start PostgreSQL (see below for Docker command)
3. (Optional) Set environment variables for JWT secret and server port:
   - `export JWT_SECRET=your-very-secret-key`
   - `export SERVER_PORT=8087` (default port)
4. Build and run:
   ```sh
   ./gradlew clean build
   ./gradlew bootRun
   ```

The application will start on `http://localhost:8087`

## Docker Deployment

### Quick Start with Docker Compose

```bash
# Build and start all services
docker-compose up -d --build

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

### Using the Build Script

```bash
# Build Docker image
./docker-build.sh build

# Run with Docker Compose
./docker-build.sh run

# View logs
./docker-build.sh logs

# Stop services
./docker-build.sh stop
```

### Manual Docker Build and Run

```bash
# Build image
docker build -t product-catalog-service:latest .

# Run container
docker run -d \
  --name product-catalog-service \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/product_catalog_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -e JWT_SECRET=your-very-secret-key \
  product-catalog-service:latest
```

For comprehensive Docker deployment instructions, see **[DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)**

### Access Swagger UI

Open your browser and navigate to:
```
http://localhost:8080/swagger-ui.html
```

## Authentication

### Login

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "expiresIn": 86400
}
```

### Default Users

| Username | Password | Description |
|----------|----------|-------------|
| admin | admin123 | Administrator user |
| user | user123 | Regular user |

**Note:** For production, implement proper user management with database storage.

### Using JWT Token

Include the token in the `Authorization` header:
```bash
curl -X GET http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## API Examples

### Create a Product

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "code": "LAPTOP-001",
    "name": "Professional Laptop",
    "description": "High-performance laptop for professionals",
    "basePrice": {
      "value": 1299.99,
      "currency": "USD"
    },
    "isInStock": true,
    "stockKeepingUnit": "SKU-LAPTOP-001",
    "categoryCode": "electronics",
    "catalogCode": "main-catalog"
  }'
```

### Get Products with Pagination

```bash
curl -X GET "http://localhost:8080/api/v1/products?page=0&size=10&sort=name,asc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Filter Products

```bash
# Filter by category and stock status
curl -X GET "http://localhost:8080/api/v1/products?categoryCode=electronics&inStock=true&page=0&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Update a Product (Partial)

```bash
curl -X PATCH http://localhost:8080/api/v1/products/LAPTOP-001 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "isInStock": false,
    "basePrice": {
      "value": 1199.99,
      "currency": "USD"
    }
  }'
```

### Delete Multiple Products

```bash
curl -X DELETE http://localhost:8080/api/v1/products/batch \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '["PRODUCT-001", "PRODUCT-002", "PRODUCT-003"]'
```

## Error Handling

All errors follow a consistent format:

```json
{
  "timestamp": "2025-12-14T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Product name is required",
  "path": "/api/v1/products"
}
```

### HTTP Status Codes

- `200 OK`: Successful GET, PUT, PATCH
- `201 Created`: Successful POST
- `204 No Content`: Successful DELETE
- `400 Bad Request`: Validation error
- `401 Unauthorized`: Authentication required
- `404 Not Found`: Resource not found
- `409 Conflict`: Resource already exists
- `500 Internal Server Error`: Server error

## Best Practices Implemented

### 1. API-Driven Development
- OpenAPI specification first
- DTOs generated from spec
- Clear API contracts

### 2. Clean Architecture
- Separation of concerns
- Domain-driven design
- Layer independence

### 3. Security
- JWT-based authentication
- Stateless authentication
- Password encryption (BCrypt)

### 4. Code Quality
- Lombok for reducing boilerplate
- MapStruct for type-safe mapping
- Comprehensive logging
- Exception handling

### 5. Database
- Liquibase for schema management
- JPA for ORM
- Repository pattern

### 6. Testing
- Test containers support
- Spring Boot Test
- Security test support

## Configuration

### Application Properties

Key configuration in `application.yml`:

```yaml
# Database
spring.datasource.url: jdbc:postgresql://localhost:5432/product_catalog_db
spring.datasource.username: postgres
spring.datasource.password: password

# JWT
jwt.secret: your-secret-key
jwt.expiration: 86400000  # 24 hours

# Server
server.port: 8080
```

### Environment Variables

- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `JWT_SECRET`: JWT signing secret
- `JWT_EXPIRATION`: Token expiration time (ms)
- `SERVER_PORT`: Server port

## Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Available Actuator Endpoints

- `/actuator/health`: Application health status
- `/actuator/info`: Application information
- `/actuator/liquibase`: Database migration information

## Future Enhancements

- [ ] User management with database
- [ ] Role-based access control (RBAC)
- [ ] Product images support
- [ ] Search functionality (full-text search)
- [ ] Caching (Redis)
- [ ] Rate limiting
- [ ] API versioning
- [ ] Audit logging
- [ ] Event-driven architecture (Kafka)
- [ ] Metrics and monitoring (Prometheus, Grafana)

## Contributing

1. Follow the existing code style
2. Write tests for new features
3. Update documentation
4. Use meaningful commit messages

## License

This project is for educational purposes.

## Contact

For questions or support, contact: support@productcatalog.com
