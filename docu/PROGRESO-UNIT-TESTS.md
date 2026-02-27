# PROGRESO: Unit Tests Framework

**Última actualización:** 24 Feb 2026  
**Sprint activo:** Semana 1-2 — Tests Críticos  
**Objetivo cobertura:** 19% → 40%

---

## ESTADO GENERAL

| Clase | Tests | Estado | Notas |
|---|---|---|---|
| SecurityUtilities | 69 | Completado | P0 - 95% cobertura estimada |
| DatabaseConfig | 37 | Completado | Sin H2, sin conexiones reales |
| ConfigManager | 40 | Completado | Incluye PriorityTest separado |
| ConfigManagerPriority | 12 | Completado | Prioridades System > Env > File |
| ScenarioContext | 44 | Completado | Thread-safety incluida |
| DataUtilities (capitalize) | 10 | Completado | |
| DataUtilities (variables) | 44 | Completado | Thread-safety, replacements |
| HttpResponse | 20 | Completado | Inmutabilidad, status codes |
| TestLogger | 28 | Completado | Usa ListAppender real |
| EvidenceManager | 20 | Completado | Usa /tmp, no crea basura |
| LoggingConfiguration | 11 | Completado | Parametrizado consolidado |
| ModuleDetector | 12 | Completado | Cache, detección, fallback |
| WebDriverManagerArtifactory | 9 | Completado | detectOS, buildUrl |
| DbConnectorFactory | 30 | **NUEVO** | Sin conexiones reales |
| ConfigurationUtilities | 43 | **NUEVO** | parseYaml/Json, nested, SSLUtils |

**Total tests common:** ~429

---

## REFACTORIZACIONES APLICADAS (24 Feb 2026)

Se eliminaron redundancias y se mejoraron tests sin valor real:

- **ConfigManagerTest**: eliminados 2 tests que duplicaban `ConfigManagerPriorityTest` + 1 duplicado de trim
- **TestLoggerTest**: reescrito completo — ahora usa `ListAppender` para verificar eventos reales (no solo `doesNotThrowAnyException`)
- **EvidenceManagerTest**: eliminado `testMultipleContextSets` redundante con los tests individuales
- **LoggingConfigurationTest**: 4 tests idénticos de `configureDefault` consolidados en 1 `@ParameterizedTest` con 5 valores
- **DataUtilitiesVariableStorageTest**: `testConcurrentMixedOperations` ahora valida ausencia de excepciones; eliminado `testSystemPropPriorityOverEnv` duplicado

---

## NUEVOS TESTS: DbConnectorFactoryTest (30 tests)

```
getConnector() validaciones           - 8 tests (null, vacío, tipos inválidos, aliases)
Detección driver por URL JDBC         - 7 tests (5 URLs + inexistente + URL desconocida)
create() parámetros explícitos        - 2 tests
connectAndCache() gestión de cache    - 10 tests (null, vacío, sin config, getCached, disconnect)
Constructores específicos por tipo    - 4 tests (Oracle, PG, MySQL, SQLServer)
```

**Estrategia:** Se testea la lógica interna de la factory (resolución de tipos, detección
de drivers, validaciones) sin levantar pools reales. Las excepciones de conexión son
esperadas y verificadas.

---

## NUEVOS TESTS: ConfigurationUtilitiesTest (43 tests)

```
Constructor privado                   - 1 test
Validaciones de nombre de archivo     - 6 tests (null/vacío para yaml, json, properties)
Archivos inexistentes                 - 3 tests (RuntimeException con nombre en mensaje)
parseYaml() desde string              - 6 tests (simple, anidado, lista, null, vacío, inválido)
parseJson() desde string              - 5 tests (simple, anidado, null, vacío, malformado)
getNestedValue() notación de puntos   - 9 tests (nivel raíz, 2 y 3 niveles, null, vacío)
getStringValue() con default          - 4 tests
hasKey()                              - 6 tests
SSLUtils (en mismo archivo)           - 6 tests (constructor, isTruststoreAvailable, etc.)
```

**Estrategia:** Todo el parsing se hace desde strings, sin I/O de filesystem.
Tests deterministas y rápidos.

---

## CLASES AÚN SIN TESTS (candidatos próximos)

| Clase | Líneas | Prioridad | Motivo |
|---|---|---|---|
| DatabaseHelper | 329 | Alta | Usado por DatabaseConnectionSteps |
| ConfigurationProviderFactory | 455 | Media | Factory con cache, fácil de testear |
| BaseConfigurationProvider | ~200 | Media | Base del sistema de configuración |
| FrameworkException / Business / Technical | ~100 | Baja | Jerarquía de excepciones simple |
| HttpMethod | ~50 | Baja | Enum — no requiere tests complejos |

---

## MÉTRICAS

| Momento | Tests common | Coverage estimado |
|---|---|---|
| Inicio | 288 | 19% |
| Tras SecurityUtilities | 368 | ~25% |
| Tras DatabaseConfig | 405 | ~28% |
| Tras todos los refactors | ~329 | ~30% |
| **Hoy (24 Feb)** | **~429** | **~35-37%** |
| Objetivo Sprint 1 | — | 40% |
