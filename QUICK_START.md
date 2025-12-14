# Quick Start Guide - Product Catalog Service

## 🎉 Implementation Complete!

Your Product Catalog Service has been fully implemented with:
- ✅ Clean Architecture with proper layer separation
- ✅ API-Driven Development with OpenAPI specification  
- ✅ JWT-based security
- ✅ Complete CRUD operations with pagination
- ✅ MapStruct mappers for clean data flow
- ✅ Global exception handling
- ✅ Swagger UI for API testing

## 🚀 Quick Start (3 Steps)

### Option A: Docker Compose (Recommended for Quick Start)

```bash
# Start everything with one command
docker-compose up -d --build

# View logs
docker-compose logs -f app

# Access Swagger UI: http://localhost:8080/swagger-ui.html
```

### Option B: Local Development

### Step 1: Start Database

```bash
docker run --name product-catalog-db \
  -e POSTGRES_DB=product_catalog_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15
```

### Step 2: Build & Run

```sh
export JWT_SECRET=your-very-secret-key
export SERVER_PORT=8080 # or any free port
./gradlew clean build
./gradlew bootRun
```

**Alternative:** Open in IntelliJ and run `ProductCatalogServiceApplication`

### Step 3: Test the API

Open Swagger UI: **http://localhost:8080/swagger-ui.html**

## 🔑 Authentication

1. Click on **POST /api/v1/auth/login**
2. Try it out with:
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
3. Copy the returned JWT token
4. Click "Authorize" button at top
5. Enter: `Bearer YOUR_TOKEN_HERE`
6. Now you can test all protected endpoints!

## 📝 Quick API Test

### Create a Product

```bash
# First, login and get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# Create a product
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "code": "LAPTOP-001",
    "name": "Professional Laptop",
    "description": "High-performance laptop for professionals",
    "basePrice": {
      "value": 1299.99,
      "currency": "USD"
    },
    "isInStock": true,
    "stockKeepingUnit": "SKU-LAPTOP-001"
  }'

# Get all products
curl -X GET "http://localhost:8080/api/v1/products?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# Get specific product
curl -X GET http://localhost:8080/api/v1/products/LAPTOP-001 \
  -H "Authorization: Bearer $TOKEN"
```

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Controller Layer                      │
│              (DTOs - API Contracts)                      │
│   AuthController, ProductController                     │
└────────────────────┬────────────────────────────────────┘
                     │ Mappers (MapStruct)
┌────────────────────▼────────────────────────────────────┐
│                    Service Layer                         │
│         (Domain Models - Business Logic)                │
│          ProductService, ProductServiceImpl             │
└────────────────────┬────────────────────────────────────┘
                     │ Mappers (MapStruct)
┌────────────────────▼────────────────────────────────────┐
│                  Repository Layer                        │
│              (Entities - Persistence)                    │
│   ProductRepository, CategoryRepository, etc.           │
└─────────────────────────────────────────────────────────┘
```

## 📂 Project Structure

```
src/main/java/com/product/catalog/
├── config/                 # Configuration classes
│   └── OpenApiConfig.java
├── controller/             # REST API endpoints
│   ├── AuthController.java
│   └── ProductController.java
├── dto/                    # API DTOs
│   ├── CreateProductRequest.java
│   ├── UpdateProductRequest.java
│   ├── PatchProductRequest.java
│   ├── ProductResponse.java
│   ├── ProductPageResponse.java
│   ├── PriceDto.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   └── ErrorResponse.java
├── domain/                 # Business domain models
│   ├── ProductDomain.java
│   └── PriceDomain.java
├── entity/                 # JPA entities
│   ├── Product.java
│   ├── Category.java
│   ├── Catalog.java
│   ├── Price.java (embeddable)
│   └── Review.java
├── exception/              # Exception handling
│   ├── ResourceNotFoundException.java
│   ├── ResourceAlreadyExistsException.java
│   ├── BusinessValidationException.java
│   └── GlobalExceptionHandler.java
├── mapper/                 # MapStruct mappers
│   ├── ProductMapper.java
│   └── PriceMapper.java
├── repository/             # Data access
│   ├── ProductRepository.java
│   ├── CategoryRepository.java
│   └── CatalogRepository.java
├── security/               # JWT security
│   ├── JwtTokenUtil.java
│   ├── JwtAuthenticationFilter.java
│   ├── SecurityConfig.java
│   └── CustomUserDetailsService.java
└── service/                # Business logic
    ├── ProductService.java
    └── impl/
        └── ProductServiceImpl.java
```

## 📋 Available API Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/auth/login` | Get JWT token | ❌ |
| GET | `/api/v1/products` | Get all products (paginated) | ✅ |
| GET | `/api/v1/products/{code}` | Get product by code | ✅ |
| POST | `/api/v1/products` | Create product | ✅ |
| POST | `/api/v1/products/batch` | Create multiple products | ✅ |
| PUT | `/api/v1/products/{code}` | Update product (full) | ✅ |
| PATCH | `/api/v1/products/{code}` | Update product (partial) | ✅ |
| DELETE | `/api/v1/products/{code}` | Delete product | ✅ |
| DELETE | `/api/v1/products/batch` | Delete multiple products | ✅ |

## 🎯 Key Features Implemented

### 1. Pagination
```
GET /api/v1/products?page=0&size=20&sort=name,asc
```

### 2. Filtering
```
GET /api/v1/products?categoryCode=electronics&inStock=true
```

### 3. Batch Operations
```
POST /api/v1/products/batch
DELETE /api/v1/products/batch
```

### 4. Partial Updates
```
PATCH /api/v1/products/PROD-001
{
  "isInStock": false
}
```

### 5. JWT Security
- Token-based authentication
- Stateless sessions
- Protected endpoints

## 🔍 Troubleshooting

### Issue: Build fails or hangs

**Solution:**
```bash
# Clear Gradle cache
./gradlew clean --refresh-dependencies

# Or use IntelliJ
File → Invalidate Caches → Invalidate and Restart
```

### Issue: Database connection error

**Solution:**
- Check PostgreSQL is running: `docker ps`
- Verify connection in `application.yml`
- Check database exists: `docker exec -it product-catalog-db psql -U postgres -l`

### Issue: JWT authentication fails

**Solution:**
- Ensure you're using correct credentials (admin/admin123)
- Check token is included in Authorization header
- Verify token hasn't expired (24 hour default)

## 📚 Documentation Files

- **README.md** - Comprehensive guide with examples
- **IMPLEMENTATION_SUMMARY.md** - Complete implementation details
- **openapi/product-catalog-api.yaml** - OpenAPI 3.0 specification
- **QUICK_START.md** - This file

## 🎓 Best Practices Used

✅ **API-First Development** - OpenAPI spec drives implementation
✅ **Clean Architecture** - Clear separation of concerns
✅ **Domain-Driven Design** - Rich domain models
✅ **SOLID Principles** - Dependency injection, interfaces
✅ **Security** - JWT, password encryption
✅ **Validation** - Bean validation with custom messages
✅ **Exception Handling** - Global handler with consistent responses
✅ **Logging** - Comprehensive logging with SLF4J
✅ **Documentation** - Swagger UI, JavaDoc, Markdown docs

## 🚀 Next Steps

1. **Add Tests** - Unit and integration tests
2. **User Management** - Database-backed authentication
3. **Caching** - Add Redis for performance
4. **Search** - Implement Elasticsearch
5. **CI/CD** - Set up automated pipeline
6. **Monitoring** - Add Prometheus/Grafana

## 💡 Tips

- Use Swagger UI for quick API testing
- Check logs for debugging: `./gradlew bootRun --info`
- Sample data is loaded via Liquibase migrations
- All endpoints (except login) require JWT token

---

**🎉 You're all set! Start the application and visit http://localhost:8080/swagger-ui.html**

For detailed information, see:
- **README.md** - Full documentation
- **IMPLEMENTATION_SUMMARY.md** - Architecture details
