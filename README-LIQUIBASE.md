# Product Catalog Service - Database Schema Management

This project uses **Liquibase** for database schema management and migration.

## Database Setup

### Prerequisites
- Java 25 or newer
- Docker (for PostgreSQL) or local PostgreSQL 13+

### Quick Start

1. **Start the database using Docker Compose:**
   ```bash
   docker-compose up -d postgres
   ```

2. **Start the application:**
   ```bash
   ./gradlew bootRun
   ```
   
   Liquibase will automatically run the migrations on startup.

3. **Access pgAdmin (optional):**
   - URL: http://localhost:8081
   - Email: admin@admin.com
   - Password: admin

## Liquibase Structure

```
src/main/resources/database/liquibase/
├── db.changelog-master.xml          # Main changelog file
├── liquibase.properties             # Liquibase configuration
└── changelogs/
    ├── 001-create-initial-schema.xml # Initial database schema
    └── 002-insert-sample-data.xml    # Sample data insertion
```

## Database Schema

The schema includes the following tables:

### Tables Created:
- **catalogs** - Product catalogs (STAGED/ONLINE)
- **categories** - Product categories with hierarchical structure
- **category_subcategories** - Many-to-many relationship for category hierarchy
- **products** - Product information with embedded price
- **reviews** - Product reviews and ratings

### Key Features:
- Primary keys and foreign key constraints
- Check constraints for data validation
- Indexes for performance optimization
- Sample data for testing

## Environment Variables

You can customize the database connection using environment variables:

- `DB_USERNAME` - Database username (default: postgres)
- `DB_PASSWORD` - Database password (default: password)
- `JWT_SECRET` - JWT signing secret (required for production)
- `SERVER_PORT` - Server port (default: 8080)
- `SHOW_SQL` - Show SQL queries in logs (default: false)
- `LIQUIBASE_CONTEXTS` - Liquibase contexts to run (default: default)

## Liquibase Commands

### Manual Migration Commands:
```bash
# Update database to latest version
./gradlew liquibaseUpdate

# Generate SQL for review
./gradlew liquibaseSql

# Rollback last changeset
./gradlew liquibaseRollbackCount -PliquibaseCommandValue=1

# Validate changelog
./gradlew liquibaseValidate
```

## Adding New Migrations

1. Create a new changelog file in `changelogs/` directory
2. Follow naming convention: `00X-description.xml`
3. Add the file to `db.changelog-master.xml`
4. Test the migration in development environment

### Example new migration:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="011-add-new-feature" author="developer">
        <!-- Your changes here -->
    </changeSet>
</databaseChangeLog>
```

## Monitoring

The application exposes Liquibase information through Spring Boot Actuator:
- Health check: http://localhost:8080/actuator/health
- Liquibase info: http://localhost:8080/actuator/liquibase
