# Database Guide

Configuration and management of the PostgreSQL database for the Product Catalog Service — covering local setup, Liquibase migrations, AWS RDS, and connection pooling.

---

## Table of Contents

1. [Schema Overview](#1-schema-overview)
2. [Local Database Setup](#2-local-database-setup)
3. [Liquibase Migrations](#3-liquibase-migrations)
4. [AWS RDS Configuration](#4-aws-rds-configuration)
5. [HikariCP Connection Pool](#5-hikaricp-connection-pool)
6. [Configuration Reference](#6-configuration-reference)

---

## 1. Schema Overview

### Entity Relationships

```
┌─────────────────┐       ┌──────────────────┐       ┌─────────────────┐
│   categories    │       │    products       │       │    catalogs     │
├─────────────────┤       ├──────────────────┤       ├─────────────────┤
│ code (PK)       │◄──────│ code (PK)        │──────►│ code (PK)       │
│ name            │       │ name             │       │ name            │
│ description     │       │ description      │       │ description     │
│ parent_cat_id   │       │ base_price_value │       │ is_active       │
└─────────────────┘       │ base_price_curr  │       │ catalog_version │
       ▲                  │ is_in_stock      │       └─────────────────┘
       │                  │ sku              │
       │ M:N self         │ category_id (FK) │
       │                  │ catalog_code (FK)│
┌──────┴──────────┐       └────────┬─────────┘
│ category_       │                │ 1:N
│ subcategories   │                ▼
├─────────────────┤       ┌──────────────────┐
│ parent_id (FK)  │       │     reviews      │
│ child_id (FK)   │       ├──────────────────┤
└─────────────────┘       │ id (PK)          │
                          │ product_code (FK)│
                          │ rating           │
                          │ comment          │
                          │ reviewer_name    │
                          │ review_date      │
                          └──────────────────┘
```

### Domain Entities

| Entity | Key | Description |
|--------|-----|-------------|
| **Product** | `code` (String) | Core entity with name, description, price (embedded), stock status, SKU |
| **Category** | `code` (String) | Hierarchical categories via self-referencing M:N join table |
| **Catalog** | `code` (String) | Groups products; has version (STAGED/ONLINE) |
| **Review** | `id` (String) | Product reviews with rating (1-5) and comment |
| **Price** | Embedded in Product | Value + currency pair |

> **PlantUML diagram:** See [`doc/EntityRelationship.puml`](EntityRelationship.puml) for the full UML source.

---

## 2. Local Database Setup

### Using Docker Compose (Recommended)

```bash
docker-compose up -d postgres
```

This starts PostgreSQL 15 on port 5432 with database `product_catalog_db`. PgAdmin is available at http://localhost:8081 (admin@admin.com / admin).

### Using Docker Manually

```bash
docker run --name product-catalog-db \
  -e POSTGRES_DB=product_catalog_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15
```

### Direct Connection

```
Host: localhost
Port: 5432
Database: product_catalog_db
Username: postgres
Password: password
```

---

## 3. Liquibase Migrations

### Structure

```
src/main/resources/database/liquibase/
├── db.changelog-master.xml          # Master changelog (entry point)
├── liquibase.properties             # Liquibase settings
└── changelogs/
    ├── 001-create-initial-schema.xml # Tables, constraints, indexes
    └── 002-insert-sample-data.xml    # Sample data for testing
```

### How It Works

- Liquibase runs **automatically on application startup** (before Hibernate)
- Hibernate `ddl-auto` is set to `none` — Liquibase owns all schema management
- `drop-first` is `false` — schema is never dropped on restart
- Migration state is tracked in `DATABASECHANGELOG` and `DATABASECHANGELOGLOCK` tables

### Manual Commands

```bash
# Update database to latest version
./gradlew liquibaseUpdate

# Generate SQL for review (without applying)
./gradlew liquibaseSql

# Rollback last changeset
./gradlew liquibaseRollbackCount -PliquibaseCommandValue=1

# Validate changelog syntax
./gradlew liquibaseValidate
```

### Adding New Migrations

1. Create a new file: `changelogs/003-your-description.xml`
2. Add it to `db.changelog-master.xml`:
   ```xml
   <include file="changelogs/003-your-description.xml" relativeToChangelogFile="true"/>
   ```
3. Test in development before deploying

### Migration File Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="003-your-description" author="developer">
        <!-- Your changes here -->
    </changeSet>
</databaseChangeLog>
```

### Monitoring Migrations

```bash
# Via actuator
curl http://localhost:8087/actuator/liquibase

# In Kubernetes
kubectl logs -n product-catalog <pod> | grep -i liquibase
```

---

## 4. AWS RDS Configuration

### Why No Init Container?

AWS RDS is a managed service that is always available. Unlike an in-cluster PostgreSQL, there's no need to wait for database readiness. Spring Boot's HikariCP handles connection retries automatically.

### RDS Setup Requirements

1. **Engine:** PostgreSQL 15+
2. **Database name:** `product_catalog_db`
3. **Security Group:** Must allow inbound from EKS worker node security group on port 5432
4. **Subnet:** Must be in the same VPC as the EKS cluster (private subnets recommended)

### Providing Credentials in Kubernetes

Database URL, username, and password are stored in a Kubernetes Secret:

```bash
kubectl create secret generic product-catalog-secrets \
  --namespace product-catalog \
  --from-literal=db-url='jdbc:postgresql://<rds-endpoint>:5432/product_catalog_db' \
  --from-literal=db-username='<username>' \
  --from-literal=db-password='<password>' \
  --from-literal=jwt-secret='<jwt-secret>'
```

For production, use **External Secrets Operator** with AWS Secrets Manager (configured in `k8s/external-secrets/secretstore.yaml`).

---

## 5. HikariCP Connection Pool

### Kubernetes ConfigMap Settings

These are set in `k8s/base/configmap.yaml`:

| Property | Value | Description |
|----------|-------|-------------|
| `maximumPoolSize` | 20 | Max connections to RDS |
| `minimumIdle` | 5 | Min idle connections kept open |
| `connectionTimeout` | 30000ms | Max wait for a connection from pool |
| `idleTimeout` | 300000ms (5min) | Idle connection lifetime |
| `maxLifetime` | 1800000ms (30min) | Max connection lifetime |
| `keepaliveTime` | 60000ms (1min) | Keepalive interval |
| `leakDetectionThreshold` | 30000ms | Log warning if connection held >30s |
| `initializationFailTimeout` | 1ms | Fail fast if DB unreachable on startup |

### Sizing Guidelines

| Environment | Pool Size | Min Idle |
|-------------|-----------|----------|
| Dev (1 pod) | 10 | 2 |
| Staging (2 pods) | 15 | 3 |
| Production (3+ pods) | 20 | 5 |

> **Rule of thumb:** Total connections across all pods ≤ RDS `max_connections` (usually 100-400 depending on instance size).

---

## 6. Configuration Reference

### application.yml (Local)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/product_catalog_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
  jpa:
    hibernate:
      ddl-auto: none            # Liquibase manages schema
    show-sql: ${SHOW_SQL:false}
  liquibase:
    change-log: classpath:database/liquibase/db.changelog-master.xml
    drop-first: false
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_USERNAME` | Database username | postgres |
| `DB_PASSWORD` | Database password | password |
| `SPRING_DATASOURCE_URL` | JDBC URL (K8s override) | — |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Must be `none` | none |
| `SPRING_LIQUIBASE_ENABLED` | Enable migrations | true |
| `SPRING_LIQUIBASE_CONTEXTS` | Liquibase contexts | default |

### Important Rules

1. **Never set `ddl-auto: validate`** — causes race condition with Liquibase on fresh databases
2. **Never set `drop-first: true`** in staging/production — destroys all data
3. **Always test migrations** in dev before promoting to staging
4. **Use `none` for `ddl-auto`** — let Liquibase be the single source of truth for schema

