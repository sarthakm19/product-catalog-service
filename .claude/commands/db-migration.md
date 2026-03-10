---
argument-hint: "<NNN-description> e.g. 003-add-supplier-table"
---

Create a new Liquibase changelog for: **$ARGUMENTS**

Before writing, read these files:
- `src/main/resources/database/liquibase/db.changelog-master.xml`
- `src/main/resources/database/liquibase/changelogs/001-create-initial-schema.xml`
- `src/main/resources/database/liquibase/changelogs/002-insert-sample-data.xml`

Then:

1. Create `src/main/resources/database/liquibase/changelogs/$ARGUMENTS.xml`
   - Use the same XML namespace and schema declarations as existing changelogs
   - Each logical change is a separate `<changeSet id="..." author="system">`
   - Include `<rollback>` for every DDL changeSet
   - For `NOT NULL` columns added to existing tables, include a data migration step or `defaultValue`
   - Use `<sql>` with `<![CDATA[...]]>` for constraints that Liquibase cannot express natively

2. Register the new file in `db.changelog-master.xml` by adding:
   ```xml
   <include file="database/liquibase/changelogs/$ARGUMENTS.xml" relativeToChangelogFile="false"/>
   ```

3. Confirm the file is registered and show the final `db.changelog-master.xml`.

Changelog rules (from `docs/SKILLS.md §9`):
- Never modify an existing applied changelog — always create a new one
- `changeSet id` format: `NNN-description-of-change`
- `author` is always `system`
- Every DDL changeSet must have a `<rollback>` block