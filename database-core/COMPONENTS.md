# database-core — Component Catalog

> **Auto-generado** — TASK-J03. NO editar manualmente.
> Regenerar con `./gradlew :database-core:test --tests "*ComponentCatalogTest"`.
>
> **Plugin:** `com.qa.databasecore.plugin.DatabasePlugin`  
> **Platform ID:** `DATABASE`  
> **Display:** Database Testing  
> **Componentes:** 3  
> **Última generación:** 2026-06-23

Esta tabla es el **contrato público** de los `StepComponent` que el módulo `database-core` expone al Backend (catálogo i18n) y al Frontend (paleta del Scenario Builder). Cada entrada se deriva por reflexión vía SPI (`ServiceLoader<CorePlugin>`).

**Convenciones**

- `@StepId`: identificador estable. Cambiar un id es **breaking** (rompe escenarios persistidos en BD del BE).
- Fase BDD: `GIVEN | WHEN | THEN | ANY`.
- Keywords: enriquecen el `ScenarioSuggestionEngine` (TASK-C06).
- Locale: si una clave no aparece en `displayNameByLocale`, el FE cae a `displayName` (ES).

---

## `db.setup`

- **Display:** **Conexion a Base de Datos** _(es)_ / **Database Connection** _(en)_ / **Connexion a la base de donnees** _(fr)_
- **Categoría:** Configuracion  · **Fase BDD:** `GIVEN`  · **Display order:** `10`
- **Icono:** `storage`  · **Keywords:** setup, configurar, connect, connection, host, port, database, schema, datasource, jdbc
- **Descripción:** Establecer conexion a Oracle, PostgreSQL, MySQL o SQL Server
- **Glue:** `com.qa.databasecore.steps.DatabaseConnectionSteps`

## `db.execution`

- **Display:** **Consultas y Sentencias SQL** _(es)_ / **SQL Queries & Statements** _(en)_ / **Requetes et instructions SQL** _(fr)_
- **Categoría:** Ejecucion  · **Fase BDD:** `WHEN`  · **Display order:** `20`
- **Icono:** `code`  · **Keywords:** sql, query, consulta, execute, ejecutar, insert, update, delete, statement, stored-proc
- **Descripción:** Ejecutar SELECT, INSERT, UPDATE y DELETE con PreparedStatement (anti SQL injection)
- **Glue:** `com.qa.databasecore.steps.DatabaseConnectionSteps`

## `db.validation`

- **Display:** **Validacion de Resultados DB** _(es)_ / **DB Results Validation** _(en)_ / **Validation des resultats DB** _(fr)_
- **Categoría:** Validacion  · **Fase BDD:** `THEN`  · **Display order:** `30`
- **Icono:** `fact_check`  · **Keywords:** sql, query, consulta, select, rows, filas, validar, verificar, assert, count, exists, column
- **Descripción:** Validar existencia de resultados, valores de columnas y extraer datos al contexto del escenario
- **Glue:** `com.qa.databasecore.steps.DatabaseConnectionSteps`

---

> Para añadir un componente nuevo: implementar `com.qa.common.api.runtime.StepComponent`, anotar la clase con `@com.qa.common.api.runtime.annotation.StepId("<id>")`, registrarla en el plugin (`getComponents()`), y regenerar este documento.

> Para deprecar: marcar `@StepId(value=..., deprecated=true, replacedBy="<nuevo-id>")` y mantener la clase activa al menos un sprint para permitir migración FE.
