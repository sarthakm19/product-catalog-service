---
argument-hint: "<ResourceName> e.g. Supplier"
---

Scaffold a complete new resource called **$ARGUMENTS** following the strict 4-layer architecture defined in `docs/SKILLS.md`.

Before writing any code, read these files for context and patterns:
- `src/main/java/com/product/catalog/entity/Product.java`
- `src/main/java/com/product/catalog/domain/ProductDomain.java`
- `src/main/java/com/product/catalog/dto/CreateProductRequest.java`
- `src/main/java/com/product/catalog/mapper/ProductMapper.java`
- `src/main/java/com/product/catalog/service/ProductService.java`
- `src/main/java/com/product/catalog/service/impl/ProductServiceImpl.java`
- `src/main/java/com/product/catalog/controller/ProductController.java`
- `src/main/resources/database/liquibase/changelogs/001-create-initial-schema.xml`
- `src/main/resources/database/liquibase/db.changelog-master.xml`

Then create these files in order:

1. **Entity** — `src/main/java/com/product/catalog/entity/$ARGUMENTS.java`
   - JPA entity with `@Entity`, `@Table`
   - String `code` as primary key (`@Id`)
   - `FetchType.LAZY` on all relationships
   - No Lombok — explicit getters/setters and constructors

2. **Domain** — `src/main/java/com/product/catalog/domain/${ARGUMENTS}Domain.java`
   - Plain Java object — no JPA or HTTP annotations
   - `isValid()` method checking required fields
   - Static inner `Builder` class
   - `equals()`, `hashCode()`, `toString()`

3. **Request DTOs** — in `src/main/java/com/product/catalog/dto/`
   - `Create${ARGUMENTS}Request.java` — with `@NotBlank`/`@NotNull`/`@Size` constraints and messages
   - `Update${ARGUMENTS}Request.java` — same fields, all required
   - `Patch${ARGUMENTS}Request.java` — all fields optional (nullable)
   - `${ARGUMENTS}Response.java` — response shape

4. **Mapper** — `src/main/java/com/product/catalog/mapper/${ARGUMENTS}Mapper.java`
   - MapStruct `@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)`
   - Methods: `entityToDomain`, `domainToEntity`, `createRequestToDomain`, `updateRequestToDomain`, `domainToResponse`
   - List variants: `entitiesToDomains`, `domainsToResponses`

5. **Repository** — `src/main/java/com/product/catalog/repository/${ARGUMENTS}Repository.java`
   - Extends `JpaRepository<$ARGUMENTS, String>`
   - `findByCode`, `existsByCode`, `deleteByCode`
   - `@Query` JPQL for any filter methods needed
   - Javadoc on each method

6. **Service interface** — `src/main/java/com/product/catalog/service/${ARGUMENTS}Service.java`
   - CRUD methods returning domain objects: `create`, `getByCode`, `getAll` (paginated), `update`, `patch`, `delete`

7. **Service implementation** — `src/main/java/com/product/catalog/service/impl/${ARGUMENTS}ServiceImpl.java`
   - `@Service @Transactional` on class
   - `@Transactional(readOnly = true)` on read methods
   - Constructor injection
   - `private static final Logger log = LoggerFactory.getLogger(${ARGUMENTS}ServiceImpl.class)`
   - Business validation before writes
   - Use `ResourceNotFoundException`, `ResourceAlreadyExistsException`, `BusinessValidationException`

8. **Controller** — `src/main/java/com/product/catalog/controller/${ARGUMENTS}Controller.java`
   - `@RestController @RequestMapping("/api/v1/${arguments_plural_kebab}")`
   - `@Tag`, `@SecurityRequirement(name = "bearerAuth")` on class
   - `@Operation` on every method, `@Parameter` on every path/query param
   - `@Valid` on every `@RequestBody`
   - Return `ResponseEntity<Xxx>` always
   - Full CRUD + batch endpoints matching `ProductController` structure

9. **Liquibase changelog** — determine the next sequence number by checking existing changelogs, then create:
   `src/main/resources/database/liquibase/changelogs/NNN-add-${arguments_snake}_table.xml`
   - Follow the XML structure in `001-create-initial-schema.xml`
   - Include `<rollback>` for all DDL
   - Register it in `db.changelog-master.xml`

After all files are created:
- Run `./gradlew clean build -x test` to verify MapStruct compiles and there are no errors
- Fix any compilation issues before finishing
- Report the full list of files created