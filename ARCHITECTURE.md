# Product Catalog Service - Architecture Documentation

## 🏛️ System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          Client Layer                            │
│   (Web Browser, Mobile App, Postman, Swagger UI, etc.)         │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTP/HTTPS Requests
                         │ JSON Payload
                         │ JWT Token
┌────────────────────────▼────────────────────────────────────────┐
│                    API Gateway / Load Balancer                   │
│                     (Future Enhancement)                         │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│                   Spring Boot Application                        │
│                  (Product Catalog Service)                       │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              Security Layer (JWT Filter)                   │ │
│  │    - JWT Authentication Filter                             │ │
│  │    - Security Configuration                                │ │
│  │    - User Details Service                                  │ │
│  └──────────────────────┬─────────────────────────────────────┘ │
│                         │                                         │
│  ┌──────────────────────▼─────────────────────────────────────┐ │
│  │              Controller Layer (REST API)                   │ │
│  │    ┌──────────────────┐    ┌───────────────────┐          │ │
│  │    │ AuthController   │    │ ProductController │          │ │
│  │    │  - /auth/login   │    │  - /products/**   │          │ │
│  │    └──────────────────┘    └───────────────────┘          │ │
│  │              Uses DTOs (Data Transfer Objects)             │ │
│  └──────────────────────┬─────────────────────────────────────┘ │
│                         │                                         │
│                    ┌────▼─────┐                                  │
│                    │ Mappers  │ (MapStruct)                      │
│                    └────┬─────┘                                  │
│                         │                                         │
│  ┌──────────────────────▼─────────────────────────────────────┐ │
│  │              Service Layer (Business Logic)                │ │
│  │    ┌────────────────────────────────────────┐              │ │
│  │    │      ProductService (Interface)        │              │ │
│  │    └──────────────┬─────────────────────────┘              │ │
│  │                   │                                          │ │
│  │    ┌──────────────▼─────────────────────────┐              │ │
│  │    │      ProductServiceImpl                │              │ │
│  │    │  - Business validation                 │              │ │
│  │    │  - Transaction management              │              │ │
│  │    │  - Domain logic                        │              │ │
│  │    └──────────────┬─────────────────────────┘              │ │
│  │         Uses Domain Models                                  │ │
│  └──────────────────────┬─────────────────────────────────────┘ │
│                         │                                         │
│                    ┌────▼─────┐                                  │
│                    │ Mappers  │ (MapStruct)                      │
│                    └────┬─────┘                                  │
│                         │                                         │
│  ┌──────────────────────▼─────────────────────────────────────┐ │
│  │          Repository Layer (Data Access)                    │ │
│  │    ┌───────────┐ ┌──────────┐ ┌──────────┐                │ │
│  │    │ Product   │ │ Category │ │ Catalog  │                │ │
│  │    │ Repository│ │Repository│ │Repository│                │ │
│  │    └───────────┘ └──────────┘ └──────────┘                │ │
│  │              Uses JPA Entities                              │ │
│  └──────────────────────┬─────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │           Cross-Cutting Concerns                            ││
│  │  - Global Exception Handler                                 ││
│  │  - Logging (SLF4J)                                          ││
│  │  - Validation (Bean Validation)                             ││
│  │  - OpenAPI Documentation                                    ││
│  └─────────────────────────────────────────────────────────────┘│
└────────────────────────┬────────────────────────────────────────┘
                         │ JDBC
┌────────────────────────▼────────────────────────────────────────┐
│                    PostgreSQL Database                           │
│                   (product_catalog_db)                          │
│                                                                   │
│  Tables: products, categories, catalogs, catalog_versions,      │
│          reviews, category_subcategories                         │
└─────────────────────────────────────────────────────────────────┘
```

## 📦 Layered Architecture Details

### 1. Presentation Layer (Controllers)

**Purpose:** Handle HTTP requests and responses

**Components:**
- `AuthController` - Authentication endpoints
- `ProductController` - Product CRUD endpoints

**Responsibilities:**
- Request validation
- DTO serialization/deserialization
- HTTP status code management
- API documentation (Swagger annotations)

**Data Format:** DTOs (Data Transfer Objects)

### 2. Application Layer (Services)

**Purpose:** Implement business logic

**Components:**
- `ProductService` (Interface)
- `ProductServiceImpl` (Implementation)

**Responsibilities:**
- Business rule validation
- Transaction management
- Domain object orchestration
- Cross-entity operations

**Data Format:** Domain Models

### 3. Domain Layer

**Purpose:** Core business entities and logic

**Components:**
- `ProductDomain` - Product business object
- `PriceDomain` - Price business object

**Responsibilities:**
- Business logic encapsulation
- Domain validation
- Business rules

### 4. Infrastructure Layer (Repositories)

**Purpose:** Data persistence

**Components:**
- `ProductRepository`
- `CategoryRepository`
- `CatalogRepository`

**Responsibilities:**
- Database queries
- Entity CRUD operations
- Custom query methods

**Data Format:** JPA Entities

### 5. Cross-Cutting Concerns

**Components:**
- Security (JWT)
- Exception Handling
- Logging
- Validation
- API Documentation

## 🔄 Data Flow

### Request Flow (Creating a Product)

```
1. Client
   ↓ POST /api/v1/products + JWT Token
   
2. JwtAuthenticationFilter
   ↓ Validates JWT token
   ↓ Sets Security Context
   
3. ProductController
   ↓ Receives CreateProductRequest DTO
   ↓ Validates using @Valid
   
4. ProductMapper
   ↓ Maps CreateProductRequest → ProductDomain
   
5. ProductServiceImpl
   ↓ Validates business rules
   ↓ Checks if product exists
   ↓ Sets relationships (Category, Catalog)
   
6. ProductMapper
   ↓ Maps ProductDomain → Product Entity
   
7. ProductRepository
   ↓ Saves to database
   ↓ Returns saved entity
   
8. ProductMapper
   ↓ Maps Product Entity → ProductDomain
   
9. ProductServiceImpl
   ↓ Returns ProductDomain
   
10. ProductMapper
    ↓ Maps ProductDomain → ProductResponse DTO
    
11. ProductController
    ↓ Returns ResponseEntity<ProductResponse>
    ↓ HTTP 201 Created
    
12. Client
    ← Receives JSON response
```

## 🔐 Security Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Client Request                        │
│              POST /api/v1/products                       │
│         Header: Authorization: Bearer <JWT>              │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│          JwtAuthenticationFilter                         │
│                                                           │
│  1. Extract JWT from Authorization header                │
│  2. Validate JWT signature                               │
│  3. Extract username from token                          │
│  4. Load UserDetails                                     │
│  5. Validate token hasn't expired                        │
│  6. Set Authentication in Security Context               │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              Security Configuration                      │
│                                                           │
│  - Public endpoints: /auth/**, /swagger-ui/**            │
│  - Protected endpoints: /api/v1/products/**              │
│  - Stateless session management                          │
│  - BCrypt password encoding                              │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
              Controller Layer
```

### JWT Token Structure

```
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "admin",
  "iat": 1702556400,
  "exp": 1702642800
}

Signature:
HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret
)
```

## 🗺️ Component Interaction Diagram

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ├──────────────────────────────────────────┐
       │                                          │
       ▼                                          ▼
┌──────────────┐                          ┌──────────────┐
│AuthController│                          │ProductController│
└──────┬───────┘                          └──────┬───────┘
       │                                         │
       ▼                                         ▼
┌──────────────┐                          ┌──────────────┐
│JwtTokenUtil  │                          │ProductService│
└──────────────┘                          └──────┬───────┘
                                                 │
                                    ┌────────────┼────────────┐
                                    │            │            │
                                    ▼            ▼            ▼
                            ┌─────────────┐ ┌─────────┐ ┌─────────┐
                            │ProductRepo  │ │Category │ │Catalog  │
                            │             │ │Repo     │ │Repo     │
                            └─────────────┘ └─────────┘ └─────────┘
                                    │
                                    ▼
                            ┌─────────────┐
                            │ PostgreSQL  │
                            └─────────────┘
```

## 📊 Database Schema

```sql
┌─────────────────┐       ┌──────────────────┐       ┌─────────────────┐
│   categories    │       │    products      │       │    catalogs     │
├─────────────────┤       ├──────────────────┤       ├─────────────────┤
│ code (PK)       │◄──────┤ code (PK)        │──────►│ code (PK)       │
│ name            │       │ name             │       │ name            │
│ description     │       │ description      │       │ description     │
│ parent_cat_id   │       │ base_price_value │       │ is_active       │
└─────────────────┘       │ base_price_curr  │       └─────────────────┘
                          │ is_in_stock      │
                          │ sku              │
                          │ category_id (FK) │
                          │ catalog_code (FK)│
                          └────────┬─────────┘
                                   │
                                   │ 1:N
                                   ▼
                          ┌──────────────────┐
                          │     reviews      │
                          ├──────────────────┤
                          │ id (PK)          │
                          │ product_code (FK)│
                          │ rating           │
                          │ comment          │
                          │ reviewer_name    │
                          │ review_date      │
                          └──────────────────┘
```

## 🔀 Design Patterns Used

### 1. **Layered Architecture Pattern**
- Clear separation between presentation, business, and data layers
- Each layer has specific responsibilities

### 2. **Repository Pattern**
- Abstracts data access logic
- Provides clean interface for data operations

### 3. **Service Pattern**
- Encapsulates business logic
- Coordinates between repositories

### 4. **DTO Pattern**
- Separates API contracts from domain models
- Controls what data is exposed

### 5. **Mapper Pattern**
- Transforms data between layers
- Keeps layers independent

### 6. **Dependency Injection**
- Constructor-based injection
- Loose coupling between components

### 7. **Strategy Pattern**
- Security strategies (JWT authentication)
- Different authentication mechanisms

### 8. **Factory Pattern**
- MapStruct generates factory methods
- Object creation abstraction

### 9. **Builder Pattern**
- Lombok @Builder for DTOs
- Fluent object construction

### 10. **Chain of Responsibility**
- Spring Security filter chain
- Exception handler chain

## 🛡️ Exception Handling Flow

```
┌─────────────────────────────────────────────────────────┐
│                    Controller Method                     │
│              throws BusinessException                    │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│           GlobalExceptionHandler                         │
│                                                           │
│  @ExceptionHandler(ResourceNotFoundException.class)      │
│  → HTTP 404 + ErrorResponse                              │
│                                                           │
│  @ExceptionHandler(ResourceAlreadyExistsException.class) │
│  → HTTP 409 + ErrorResponse                              │
│                                                           │
│  @ExceptionHandler(BusinessValidationException.class)    │
│  → HTTP 400 + ErrorResponse                              │
│                                                           │
│  @ExceptionHandler(MethodArgumentNotValidException)      │
│  → HTTP 400 + ErrorResponse                              │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                    Client Response                       │
│                                                           │
│  {                                                        │
│    "timestamp": "2025-12-14T10:30:00",                   │
│    "status": 404,                                        │
│    "error": "Not Found",                                 │
│    "message": "Product not found with code: 'PROD-001'", │
│    "path": "/api/v1/products/PROD-001"                   │
│  }                                                        │
└─────────────────────────────────────────────────────────┘
```

## 🎯 SOLID Principles Application

### Single Responsibility Principle (SRP)
- Each class has one reason to change
- Controllers handle HTTP, Services handle business logic

### Open/Closed Principle (OCP)
- Services use interfaces
- Easy to extend without modifying existing code

### Liskov Substitution Principle (LSP)
- ProductService interface can be replaced with any implementation
- Subtypes are substitutable

### Interface Segregation Principle (ISP)
- Small, focused interfaces
- ProductService has specific methods

### Dependency Inversion Principle (DIP)
- Depend on abstractions (interfaces)
- Use dependency injection

## 📈 Scalability Considerations

### Current Architecture Supports:

1. **Horizontal Scaling**
   - Stateless design (JWT)
   - Can deploy multiple instances

2. **Database Scaling**
   - Read replicas
   - Connection pooling
   - Index optimization

3. **Caching Strategy (Future)**
   - Redis for product data
   - Cache invalidation on updates

4. **API Gateway (Future)**
   - Load balancing
   - Rate limiting
   - API versioning

5. **Microservices Ready**
   - Clean boundaries
   - Independent deployment possible

## 🔍 Monitoring & Observability

```
┌─────────────────────────────────────────┐
│         Application Metrics              │
│  - Spring Boot Actuator                  │
│  - /actuator/health                      │
│  - /actuator/metrics                     │
└─────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│         Logging (SLF4J + Logback)        │
│  - INFO: Business operations             │
│  - ERROR: Exceptions                     │
│  - DEBUG: Detailed flow                  │
└─────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│      Future Enhancements                 │
│  - Prometheus metrics                    │
│  - Grafana dashboards                    │
│  - ELK stack for logs                    │
│  - Distributed tracing (Zipkin)          │
└─────────────────────────────────────────┘
```

---

This architecture provides a **solid foundation** for a production-ready, scalable, and maintainable product catalog service!

