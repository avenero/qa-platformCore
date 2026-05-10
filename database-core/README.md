# database-core — Testing SQL (Oracle / PostgreSQL / MySQL / SQL Server)

Módulo del `qa-platformCore` para validación contra bases de datos relacionales en escenarios BDD. Usado típicamente como **precondición** (Given), **mutación** (When) o **validación** (Then) en flujos end-to-end.

> **Coordenada Maven:** `com.qa:database-core:<version>`
> **Catálogo público de pasos:** [COMPONENTS.md](COMPONENTS.md) (auto-generado)

---

## Tabla de contenidos

1. [Propósito](#propósito)
2. [Coordenada Maven](#coordenada-maven)
3. [Dependencias clave](#dependencias-clave)
4. [Capabilities reportadas](#capabilities-reportadas)
5. [Cómo se usa standalone](#cómo-se-usa-standalone)
6. [Configuración del datasource](#configuración-del-datasource)
7. [Cómo se comunica con el exterior](#cómo-se-comunica-con-el-exterior)
8. [Component Catalog](#component-catalog)
9. [Reglas inviolables](#reglas-inviolables)

---

## Propósito

`database-core` permite a un escenario:

- **Conexión:** abrir/cerrar conexión JDBC contra Oracle, PostgreSQL, MySQL o SQL Server (driver detectado por URL).
- **Setup:** seed de datos para precondición (truncate, insert, batch).
- **Ejecución:** SELECT/INSERT/UPDATE/DELETE con `PreparedStatement` (anti SQL injection by default).
- **Validación:** existencia de filas, valor de columna, count, regex, comparación con expected, extracción al variable store del escenario.

## Coordenada Maven

```groovy
dependencies {
    api 'com.qa:database-core:2.0.0'
}
```

Trae `common` transitivamente, **además de** los 4 drivers JDBC (TASK-A06):

| Driver | Coordenada |
|---|---|
| Oracle | `com.oracle.database.jdbc:ojdbc11` |
| PostgreSQL | `org.postgresql:postgresql` |
| MySQL | `com.mysql:mysql-connector-j:8.4.0` (CVE-2023-22102 resuelto) |
| SQL Server | `com.microsoft.sqlserver:mssql-jdbc:12.8.2.jre11` (CVE-2025-59250 resuelto) |
| Pool | `com.zaxxer:HikariCP` |

> **Nota:** estos ~30 MB no se cargan en `http-core`/`web-core`/`mobile-core` — viven aquí (TASK-A06).

## Capabilities reportadas

`DatabasePlugin.describeCapabilities()` reporta los motores SQL disponibles. Usado por el FE para mostrar el selector "Tipo de DB" en el step `db.setup`.

## Cómo se usa standalone

```gherkin
Feature: Validación de orden creada
  Scenario: La orden quedó persistida con estado PENDING
    Given conecto a la base "qa-orders" con usuario "{{db.user}}" y password "{{db.password}}"
    When ejecuto la consulta:
      """
      SELECT status, total FROM orders WHERE id = '{{orderId}}'
      """
    Then la consulta retorna 1 fila
    And la columna "status" tiene valor "PENDING"
    And la columna "total" es mayor que 0
    And cierro la conexión
```

## Configuración del datasource

| Propiedad | Ejemplo | Notas |
|---|---|---|
| `db.url` | `jdbc:postgresql://host:5432/qa_orders` | El driver se detecta del prefijo |
| `db.user` | `qa_runner` | Resuelto desde el ambiente del BE |
| `db.password` | `{{secret://db/qa-orders/password}}` | Resuelto vía secret manager — NUNCA literal |
| `db.pool.max-size` | `5` | Default conservador |
| `db.timeout-ms` | `5000` | Aplicado a connect + query |

## Cómo se comunica con el exterior

| Quién | Cómo |
|---|---|
| **BE** | inyecta `ExecutionConfig.properties.db.*`. NO importa `com.qa.databasecore.*`. |
| **FE** | el step `db.setup` muestra un combo poblado por `DatabasePlugin.describeCapabilities()`. |
| **DB target** | conexión JDBC directa desde el proceso del engine (in-process) o del agente (remoto). |

## Component Catalog

[COMPONENTS.md](COMPONENTS.md) — auto-generado (3 componentes: setup / execution / validation). Regenerar:

```bash
./gradlew :database-core:test --tests "*DatabaseComponentCatalogTest"
```

## Reglas inviolables

- **R-DB-1:** los components declaran `@StepId("db.<dominio>")`. Cambios = breaking.
- **R-DB-2:** **NUNCA** ejecutar SQL concatenando strings — siempre `PreparedStatement` con `?` placeholders. Los components ya enforced este patrón.
- **R-DB-3:** las credenciales NUNCA se loguean — ni en INFO ni en DEBUG. El driver-level connection-string se redacta antes de `log.info`.
- **R-DB-4:** los drivers JDBC viven aquí (no en `common` ni en otros core). Si un módulo nuevo necesita DB, importa `database-core`, no copia drivers.
- **R-DB-5:** el módulo NO importa de `http-core`, `web-core` ni `mobile-core`.

---

> **Para QAs/POs:** lista completa de "qué puedo hacer con DB" en [COMPONENTS.md](COMPONENTS.md).
