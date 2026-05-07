# database-core

**Estado:** Esqueleto vacío — TASK-A02 ✅

Este módulo está reservado para el motor de testing de bases de datos del framework QA.

## Contenido actual

Solo la estructura de paquetes vacía. El código se agrega en sprints posteriores:

| Tarea | Descripción |
|---|---|
| TASK-A03 | Migrar `common/database/` → `database-core/` |
| TASK-A04 | Migrar lógica DB de `http-core/` → `database-core/` |
| TASK-A06 | Agregar HikariCP + drivers JDBC |
| TASK-B05 | Implementar `DatabasePlugin` y registrar en META-INF/services |

## Estructura de paquetes

```
com.qa.databasecore/
├── connector/      ← conectores DB (JDBC, pool)
├── factory/        ← fábricas de servicios DB
├── service/        ← servicios de testing DB
├── config/         ← configuración de conexiones
├── repository/     ← acceso a datos de test
├── helper/         ← utilitarios DB
├── utils/          ← utilidades generales
├── components/     ← componentes Cucumber DB
├── plugin/         ← DatabasePlugin (TASK-B05)
└── steps/          ← step definitions DB (TASK-A03)
```

## Dependencias

```
database-core → common
```

Ver `propuesta-desde-0-core.md` sección "Visión: cinco módulos limpios" para la arquitectura completa.
