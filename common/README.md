# common — Capa Base del Framework (CuAleon Core)

> **Versión:** 2.0.0 | **Grupo:** `com.qa` | **Artefacto:** `common`  
> **Última actualización:** Abril 2026  
> **Autor:** Abel Venero

---

## Índice

1. [¿Qué es Common?](#1-qué-es-common)
2. [Mapa completo de paquetes](#2-mapa-completo-de-paquetes)
3. [El Motor de Ejecución — runtime/](#3-el-motor-de-ejecución--runtime)
4. [Excepciones del Framework — exception/](#4-excepciones-del-framework--exception)
5. [Sistema de Logging — logging/](#5-sistema-de-logging--logging)
6. [Gestión de Configuración — config/](#6-gestión-de-configuración--config)
7. [HTTP Base — http/](#7-http-base--http)
8. [Base de Datos — database/](#8-base-de-datos--database)
9. [Utilidades — utils/](#9-utilidades--utils)
10. [Hooks de Cucumber — cucumber/hooks/](#10-hooks-de-cucumber--cucumberhooks)
11. [Driver (WebDriver compartido) — driver/](#11-driver-webdriver-compartido--driver)
12. [Reportes — reporting/](#12-reportes--reporting)
13. [Contrato con el Backend](#13-contrato-con-el-backend-api-pública-del-core)
14. [Cómo usar Common en otro módulo](#14-cómo-usar-common-en-otro-módulo)
15. [Dependencias](#15-dependencias)
16. [Convención de IDs de Step](#16-convención-de-ids-de-step)
17. [Catálogo de Steps a nivel método — API v2.3.0](#17-catálogo-de-steps-a-nivel-método--api-v230)

---

## 1. ¿Qué es Common?

**Common** es la **capa fundacional** del framework — la base sobre la que se construyen las capas especializadas (`api-core`, `web-core`, `mobile-core`). Ninguna de esas capas puede funcionar sin `common`.

### ¿Qué hace Common?

| Paquete | Función |
|---------|---------|
| `runtime/` | Motor que orquesta cómo se ejecutan los escenarios de prueba |
| `exception/` | Jerarquía de excepciones del framework (errores de negocio y técnicos) |
| `logging/` | Registra todo lo que pasa durante las pruebas, de forma ordenada y segura |
| `config/` | Lee la configuración del proyecto (URLs, credenciales, timeouts) |
| `http/` | Define los modelos básicos de una petición/respuesta HTTP |
| `database/` | Conecta con bases de datos (Oracle, PostgreSQL, MySQL, SQL Server) |
| `utils/` | Herramientas para variables de escenario, datos de prueba, JSON y texto |
| `cucumber/hooks/` | Administra el ciclo de vida de cada escenario de prueba |
| `driver/` | Base compartida para el manejo de WebDrivers |
| `reporting/` | Genera reportes HTML y captura evidencia HTTP de las pruebas ejecutadas |

### ¿Qué NO hace Common?

- **No contiene steps de Selenium ni Appium** (esos van en web-core y mobile-core)
- **No tiene lógica de negocio** (eso va en el proyecto de pruebas)
- **No conoce las URLs ni la estructura** de ningún sistema específico

> `database/` sí contiene steps BDD (`DatabaseConnectionSteps`) porque la BD es un recurso transversal usado por cualquier capa — no es exclusivo de API, Web ni Mobile.

---

## 2. Mapa Completo de Paquetes

```
common/
└── src/main/java/com/qa/common/
    │
    ├── runtime/                              ← MOTOR DE EJECUCIÓN
    │   ├── CorePlugin.java
    │   ├── CucumberRuntimeEngine.java
    │   ├── ExecutionContext.java
    │   ├── ExecutionConfig.java
    │   ├── ExecutionRequest.java
    │   ├── ExecutionResult.java
    │   ├── ServiceRegistry.java
    │   ├── VariableStore.java
    │   ├── StepComponent.java
    │   ├── BddPhase.java
    │   ├── DefaultLifecycleManager.java
    │   ├── LifecycleManager.java
    │   ├── InMemoryResultCollector.java
    │   ├── ScenarioMetadata.java
    │   ├── ScenarioLifecycleBridge.java
    │   ├── StepDiscoveryService.java
    │   ├── StepInfo.java
    │   ├── StepDefinitionInfo.java
    │   ├── ParamInfo.java
    │   ├── StepMethodScanner.java
    │   ├── annotation/
    │   │   ├── StepId.java
    │   │   └── StepDef.java
    │   └── events/
    │
    ├── exception/                            ← EXCEPCIONES DEL FRAMEWORK
    │   ├── FrameworkException.java           ← Base abstracta de toda excepción del framework
    │   ├── FrameworkBusinessException.java   ← Validación fallida, regla de negocio violada
    │   └── FrameworkTechnicalException.java  ← Error de infraestructura (timeout, red, config)
    │
    ├── logging/                              ← SISTEMA DE LOGGING
    │   ├── TestLogger.java                   ← Logger principal con contexto automático (MDC)
    │   └── LoggingInitializer.java           ← Inicializa el contexto de log por escenario
    │
    ├── config/                               ← GESTIÓN DE CONFIGURACIÓN
    │   ├── ConfigManager.java
    │   └── providers/
    │
    ├── http/                                 ← HTTP BASE (modelos compartidos)
    │   ├── model/
    │   │   └── HttpResponse.java             ← status + body + headers + elapsedMs
    │   └── enums/
    │       └── HttpMethod.java               ← GET, POST, PUT, PATCH, DELETE
    │
    ├── database/                             ← PLUGIN DE BASE DE DATOS (CorePlugin)
    │   ├── plugin/
    │   │   └── DatabasePlugin.java
    │   ├── components/
    │   │   ├── DatabaseSetupComponent.java
    │   │   ├── DatabaseExecutionComponent.java
    │   │   └── DatabaseValidationComponent.java
    │   ├── connectors/
    │   │   ├── BaseConnector.java
    │   │   ├── OracleConnector.java
    │   │   ├── PostgreSQLConnector.java
    │   │   ├── MySQLConnector.java
    │   │   └── SQLServerConnector.java
    │   ├── factory/
    │   │   └── DbConnectorFactory.java
    │   ├── helpers/
    │   │   └── DatabaseHelper.java
    │   ├── interfaces/
    │   │   └── DatabaseConnector.java
    │   ├── config/
    │   │   └── DatabaseConfig.java
    │   ├── repository/
    │   │   └── QueryRepository.java
    │   └── steps/
    │       └── DatabaseConnectionSteps.java
    │
    ├── utils/                                ← UTILIDADES GENERALES
    │   ├── DataUtilities.java                ← Variables de escenario + interpolación de placeholders
    │   ├── DataGenerator.java                ← UUID, timestamps, strings aleatorios
    │   ├── JsonUtilities.java                ← Parseo, extracción JSONPath, validación de esquemas
    │   ├── TextUtilities.java                ← Operaciones con texto y sanitización de logs
    │   ├── FileUtilities.java                ← Lectura de archivos
    │   ├── SecurityUtilities.java            ← Enmascaramiento de datos sensibles
    │   └── ConfigurationUtilities.java       ← Helpers para configuración
    │
    ├── cucumber/hooks/                       ← HOOKS DE CUCUMBER
    │   └── ScenarioExecutionHooks.java       ← Before/After ciclo de vida de escenario
    │
    ├── driver/                               ← BASE DE DRIVER (compartida por web y mobile)
    │
    └── reporting/                            ← REPORTES Y EVIDENCIAS
        ├── cucumber/
        │   └── CucumberReportingPlugin.java  ← EventListener: genera reporte post-ejecución
        ├── core/
        │   ├── adapter/
        │   │   ├── ResultAdapter.java
        │   │   └── cucumber/
        │   │       └── CucumberResultAdapter.java  ← JSON Cucumber → modelo propio
        │   ├── config/
        │   │   ├── ExtentConfig.java
        │   │   └── ReportingConfig.java
        │   ├── model/
        │   │   ├── Attachment.java
        │   │   ├── EnvironmentInfo.java
        │   │   ├── HttpStepDetail.java        ← DTO inmutable: snapshot HTTP redactado
        │   │   ├── ScenarioResult.java
        │   │   ├── StepResult.java
        │   │   ├── TestExecutionResult.java
        │   │   └── TestStatus.java
        │   └── util/
        │       ├── EvidenceCollector.java
        │       ├── HttpDetailRedactor.java    ← Construye HttpStepDetail con redacción
        │       └── TagExtractor.java
        ├── extent/
        │   └── generator/
        │       ├── ExtentReportGenerator.java
        │       └── ReportingManager.java      ← Fachada del pipeline
        └── manager/
            └── pipeline/
                ├── PipelineContext.java
                ├── PipelineResult.java
                ├── PipelineStepResult.java
                ├── ReportingPipeline.java
                ├── ReportingStep.java
                └── steps/
                    ├── ConversionStep.java
                    └── ExtentGenerationStep.java
```

---

## 3. El Motor de Ejecución — `runtime/`

Este es el paquete más importante de Common. Sin él, nada funciona. Es el que hace que al escribir `@api` en un escenario, automáticamente el cliente HTTP esté disponible.

### El patrón Plugin (SPI)

El motor usa el **Service Provider Interface (SPI)** de Java estándar. Funciona así:

1. Cada capa especializada (api-core, web-core, mobile-core) tiene un archivo en:  
   `META-INF/services/com.qa.common.runtime.CorePlugin`  
   que contiene el nombre completo de su plugin.

2. Cuando el motor arranca, usa `java.util.ServiceLoader` para descubrir automáticamente todos esos plugins, sin que nadie tenga que registrarlos manualmente.

3. Al ejecutar un escenario, el motor activa solo los plugins cuyos tags de activación están presentes en el escenario.

```
Escenario con @api y @web
        │
        ▼
ServiceLoader descubre: ApiPlugin, WebPlugin, MobilePlugin
        │
        ▼
Motor activa: ApiPlugin (tiene @api), WebPlugin (tiene @web)
Motor ignora: MobilePlugin (no hay @mobile)
```

### `CorePlugin` — La Interfaz del Plugin

Todo plugin debe implementar estos métodos:

```java
public interface CorePlugin {
    String getName();                              // Nombre único ("api", "web", "mobile")
    Set<String> getActivationTags();               // Tags que activan este plugin
    int getOrder();                                // Orden de inicialización (menor = primero)
    void registerServices(ServiceRegistry, ExecutionConfig); // Registra sus servicios
    void onScenarioStart(ExecutionContext context); // Se llama al inicio de cada escenario
    void onScenarioEnd(ExecutionContext context);   // Se llama al final de cada escenario
    List<StepComponent> getComponents();           // Declara sus grupos de steps
}
```

### `ExecutionContext` — El tablero de control

El `ExecutionContext` es el objeto que existe durante la vida de un escenario y reúne todo lo que se necesita:

```java
ExecutionContext ctx = ExecutionContext.requireCurrent();

// Acceder a un servicio registrado (ej: desde un step)
HttpClient client = ctx.service(HttpClient.class);

// Acceder a las variables del escenario
ctx.variables().set("token", "Bearer eyJhbG...");

// Interpolar variables en un string
String body = ctx.variables().interpolate('{"token": "${token}"}');
// → {"token": "Bearer eyJhbG..."}
```

### `ServiceRegistry` — El casillero de servicios

El `ServiceRegistry` es el mecanismo de **inyección de dependencias sin Spring**. Los servicios se crean solo cuando alguien los pide por primera vez (lazy initialization):

```java
// En ApiPlugin.registerServices():
registry.registerLazy(HttpClient.class, () -> HttpClientFactory.create(config));

// En un step:
HttpClient client = context.service(HttpClient.class);
// → Se crea el HttpClient en ese momento si no existía
```

### `VariableStore` — El cuaderno de notas

Almacena variables que los steps guardan y comparten entre sí dentro de un mismo escenario:

```java
VariableStore vars = context.variables();

vars.set("userId", "12345");
String id = vars.resolve("userId");  // → "12345"

// Reemplazar ${variables} en un texto
String url = vars.interpolate("https://api.com/users/${userId}");
// → "https://api.com/users/12345"
```

### `BddPhase` — Enum de fases

```java
BddPhase.GIVEN  // Pasos de configuración
BddPhase.WHEN   // Pasos de ejecución
BddPhase.THEN   // Pasos de validación
```

---

## 4. Excepciones del Framework — `exception/`

El framework tiene una jerarquía de excepciones propia en el paquete `com.qa.common.exception`. **Todas las capas del framework lanzan estas excepciones** — son el contrato de error entre Core, api-core, web-core, mobile-core y el Backend.

```
FrameworkException  (abstracta — base de toda excepción del framework)
    ├── FrameworkBusinessException   ← error funcional / de validación
    └── FrameworkTechnicalException  ← error de infraestructura
```

### Cuándo usar cada una

| Excepción | Cuándo se lanza | Ejemplos |
|-----------|-----------------|---------|
| `FrameworkBusinessException` | Una validación falló o una regla de negocio fue violada | Status esperado 200, se recibió 404; campo requerido ausente; dato no cumple el esquema |
| `FrameworkTechnicalException` | Error en la infraestructura que impide continuar | Fallo al inicializar el WebDriver, timeout de conexión, archivo de config no encontrado, error de cifrado |

Ambas extienden `RuntimeException` vía `FrameworkException`, por lo que no requieren declaración en `throws`.

```java
// En un step de validación
if (!status.equals(expected)) {
    throw new FrameworkBusinessException(
        "validateStatusCode",
        "Status esperado " + expected + " pero se recibió " + status);
}

// En un servicio técnico
try {
    driver = factory.createDriver(config);
} catch (Exception e) {
    throw new FrameworkTechnicalException("initDriver", "No se pudo inicializar el driver", e);
}
```

---

## 5. Sistema de Logging — `logging/`

El paquete `logging/` contiene exactamente dos clases: `TestLogger` y `LoggingInitializer`. Estas son responsables del **logging en tiempo real** durante la ejecución de pruebas. No confundir con la captura de evidencia HTTP para reportes (eso es `reporting/core/util/HttpDetailRedactor`).

### Relación entre las 4 clases de observabilidad

```
TestLogger / LoggingInitializer      → logs en consola/archivo en tiempo real (Logback + MDC)
                    ↕  capas distintas, se complementan
HttpDetailRedactor                   → construye snapshot HTTP redactado → HttpStepDetail
HttpStepDetail                       → DTO inmutable: DB, WebSocket, FE, adjuntos externos
```

Un módulo de pruebas que importe el framework obtiene **ambos beneficios automáticamente**:
- Logging estructurado en tiempo real via `TestLogger` (visible en consola/archivo)
- Captura de evidencia HTTP para reportes via `HttpDetailRedactor` / `HttpStepDetail`

### `TestLogger` — El logger principal

Todos los steps y servicios del framework usan `TestLogger`. Agrega automáticamente contexto de módulo y escenario a cada línea de log vía MDC (Mapped Diagnostic Context de Logback).

```java
TestLogger.logInfo("API_HELPER_CONFIG", "Host base establecido: " + url, null);
TestLogger.logError("HTTP_ERROR", "Error al ejecutar petición", Map.of("error", e.getMessage()));
TestLogger.logDebug("HTTP_EXEC", "Armando petición", Map.of("headers", headers));
TestLogger.logWarning("DATA_UTILITIES", "Variable no encontrada", null);
```

**Formato en el log:**
```
12:02:36.014 INFO  [API] [Mi Escenario] - [API_HELPER_CONFIG] Host base establecido: https://api.com
```

- `[API]` → Módulo activo (api, web, mobile)
- `[Mi Escenario]` → Nombre del escenario en ejecución
- `[API_HELPER_CONFIG]` → Categoría del mensaje (fácil de filtrar)

### `LoggingInitializer` — Inicialización del contexto MDC

Se ejecuta al inicio de cada escenario (vía `ScenarioExecutionHooks`) e inicializa el contexto MDC con los datos del escenario activo. Sin esto, el MDC no tiene valores y los logs pierden el contexto `[módulo] [escenario]`.

### Enmascaramiento automático de datos sensibles

El framework detecta automáticamente palabras clave sensibles en los logs y las enmascara. Las palabras clave incluyen: `password`, `token`, `secret`, `authorization`, `apikey`, `api_key`, `credential`.

```
Antes: Authorization: Bearer eyJhbGciOiJIUzM4...
Después: Authorization: Bearer ***MASKED***
```

---

## 6. Gestión de Configuración — `config/`

### `ConfigManager` — El lector de configuración

Singleton que lee configuración desde múltiples fuentes en orden de prioridad:

```
1. Propiedades del sistema (-Dkey=value en línea de comandos)
2. Variables de entorno (export KEY=value)
3. Archivo config-app.properties (en src/test/resources/)
4. Valores por defecto del framework
```

```java
ConfigManager config = ConfigManager.getInstance();

String baseUrl = config.get("api.base.url");
int timeout = Integer.parseInt(config.get("api.timeout", "30000"));
if (config.has("db.url")) { ... }
```

**Archivo `config-app.properties`:**

```properties
api.base.url=https://mi-sistema-qa.com/
web.base.url=https://mi-sistema-qa.com
web.browser=chrome
web.headless=true
api.timeout=30000

# Valores tomados de variables de entorno
db.username=${DB_USER}
db.password=${DB_PASS}

# Reporting
reporting.enabled=true
extent.enabled=true
reporting.cucumber.json.path=target/cucumber-reports/cucumber.json
```

---

## 7. HTTP Base — `http/`

Define los **tipos de datos compartidos** que todas las capas usan para representar respuestas HTTP. No hace peticiones por sí solo — eso lo hace `api-core` con `BaseHttpClient`.

### `HttpResponse` — Modelo de respuesta

```java
HttpResponse response = httpClient.getLastResponse();

int status    = response.getStatus();        // 200, 404, 500...
String body   = response.getBody();          // Cuerpo como texto
Map<String, String> headers = response.getHeaders();
long timeMs   = response.getElapsedTimeMs(); // Tiempo en ms
```

### `HttpMethod` — Métodos HTTP

```java
HttpMethod.GET | POST | PUT | PATCH | DELETE
```

> Las excepciones del framework ya no residen en este paquete. Ver [Sección 4 — exception/](#4-excepciones-del-framework--exception).

---

## 8. Base de Datos — `database/`

Permite a los tests conectarse a bases de datos para preparar datos de prueba o verificar resultados.

### Conectores disponibles

| Conector | Tipo de BD | URL típica |
|----------|-----------|------------|
| `OracleConnector` | Oracle DB | `jdbc:oracle:thin:@//host:1521/NOMBRE` |
| `PostgreSQLConnector` | PostgreSQL | `jdbc:postgresql://host:5432/nombre` |
| `MySQLConnector` | MySQL | `jdbc:mysql://host:3306/nombre` |
| `SQLServerConnector` | SQL Server | `jdbc:sqlserver://host:1433;databaseName=nombre` |

### `DatabaseHelper` — Ejecutar queries

```java
Map<String, Object> row = DatabaseHelper.executeQuery(
    connector,
    "SELECT balance, status FROM accounts WHERE user_id = ?",
    "12345"
);
DatabaseHelper.validateColumnValue(row, "status", "ACTIVE");
Object saldo = DatabaseHelper.getColumnValue(row, "balance");
```

### Steps de BD disponibles (Gherkin)

```gherkin
Given establezco conexion a base de datos "oracle"
When ejecuto la consulta "SELECT * FROM users WHERE id = ?" con parametros "12345"
Then valido que la columna "status" tenga el valor "ACTIVE"
Then obtengo el valor de la columna "balance" y lo almaceno en "saldo"
```

**Configuración en `config-app.properties`:**

```properties
oracle.db.url=jdbc:oracle:thin:@//servidor:1521/DB
oracle.db.username=${ORACLE_USER}
oracle.db.password=${ORACLE_PASS}
```

---

## 9. Utilidades — `utils/`

### `DataUtilities` — Variables de escenario e interpolación de placeholders

Gestiona las variables del escenario y resuelve placeholders `{{var}}` y `${var}` en textos.

**Responsabilidad única:** acceso al `VariableStore` del `ExecutionContext` activo y resolución de variables en texto. Para operaciones JSON usar `JsonUtilities`; para texto usar `TextUtilities`.

```java
// Guardar/leer variables en el escenario
DataUtilities.storeValue("token", "Bearer eyJhbG...");
String token = DataUtilities.getValue("token");

// Guardar/recuperar objetos tipados
DataUtilities.storeObject("miPojo", unObjeto);
MiClase obj = DataUtilities.getObject("miPojo", MiClase.class);
boolean existe = DataUtilities.hasObject("miPojo");

// Resolver placeholders en texto
// {{var}} → busca solo en ExecutionContext
// ${var}  → busca en ExecutionContext → System.getProperty → System.getenv
String body = DataUtilities.replaceVariables('{"id": "${userId}"}');

// Guardar con prefijo de capa (crea clave "api.token")
DataUtilities.saveToContext("api", "token", "abc123");

// Acceso bulk (mapa inmutable)
Map<String, Object> all = DataUtilities.getAllVariables();
```

> Para código nuevo preferir directamente:
> `ExecutionContext.requireCurrent().variables().set(key, value)`

### `DataGenerator` — Generar datos de prueba

```java
String uuid    = DataGenerator.generateUUID();
long ts        = DataGenerator.generateTimestamp();
int numero     = DataGenerator.generateRandomNumber(1, 100);
String random  = DataGenerator.generateRandomString(8);
```

### `JsonUtilities` — Manipular JSON

```java
Map<String, Object> mapa = JsonUtilities.toMap(jsonString);
String json              = JsonUtilities.toJson(miObjeto);
Object valor             = JsonUtilities.extractValue(jsonString, "$.user.name");
boolean esJson           = JsonUtilities.isValidJson(texto);
```

### `TextUtilities` — Operaciones con texto

```java
String capitalizado = TextUtilities.capitalize("hola mundo");
boolean valido      = TextUtilities.isValidString(texto);
String sanitizado   = TextUtilities.sanitizeForLogging("password", valor); // → "***MASKED***"
```

### `SecurityUtilities` — Enmascaramiento de datos sensibles

```java
String seguro      = SecurityUtilities.mask("mi-contraseña");
boolean esSensible = SecurityUtilities.isSensitiveKey("password"); // → true
```

---

## 10. Hooks de Cucumber — `cucumber/hooks/`

### `ScenarioExecutionHooks` — El ciclo de vida del escenario

Se ejecuta automáticamente antes y después de **cada escenario**:

```
@Before (orden 0)
    → Inicializar el ExecutionContext (ThreadLocal)
    → Inicializar contexto MDC de logging (LoggingInitializer)
    → Activar los plugins correspondientes a los tags del escenario
    → Registrar los servicios que cada plugin declara
    → Llamar onScenarioStart() en cada plugin activo

Cucumber ejecuta los steps del escenario...

@After (orden 0)
    → Llamar onScenarioEnd() en cada plugin activo
    → Limpiar el ExecutionContext
    → Limpiar las variables del escenario (VariableStore)
    → Limpiar el contexto MDC de logging
```

Garantiza que cada escenario **empieza con estado limpio**. El cliente HTTP del escenario anterior no contamina el siguiente.

---

## 11. Driver (WebDriver compartido) — `driver/`

Paquete base con abstracciones genéricas para manejo de drivers compartidas entre `web-core` (Selenium/Playwright) y `mobile-core` (Appium).

---

## 12. Reportes — `reporting/`

Opera **desacoplado** del motor de ejecución: se activa después de que todos los escenarios terminan, vía evento `TestRunFinished`.

```
Tests ejecutan (Cucumber)
    ↓
Cucumber escribe: target/cucumber-reports/cucumber.json
    ↓  TestRunFinished event
CucumberReportingPlugin.handleTestRunFinished()
    ↓  Guard clauses verifican habilitación y existencia del JSON
ReportingManager → Pipeline:
    ├── ConversionStep        → JSON → TestExecutionResult (modelo en memoria)
    └── ExtentGenerationStep  → TestExecutionResult → HTML
Output: build/reports/extent/execution-report.html
```

### Guard Clauses — Sin Fallos Silenciosos

| Guard | Condición | Log emitido |
|-------|-----------|-------------|
| #1 | `reporting.enabled=false` | `INFO` — reporting deshabilitado intencionalmente |
| #2 | `cucumber.json` ausente tras 10 reintentos con backoff progresivo | `ERROR` — ruta esperada + acción correctiva |
| #3 | JSON vacío o sin escenarios (`[]`) | `ERROR` — sin escenarios ejecutados |

### Modelo de datos — Contrato Core ↔ Backend ↔ Frontend

Los modelos en `reporting/core/model/` son el contrato de datos entre el Core y el Backend. Son serializables a JSON y pueden ser consumidos directamente por el BE para persistencia o por el FE para visualización.

```
TestExecutionResult
  ├── EnvironmentInfo          (entorno, browser, fecha, duración total)
  └── List<ScenarioResult>
        ├── status, durationMs, errorMessage
        └── List<StepResult>
              ├── keyword, name, status, durationMs, errorMessage
              ├── HttpStepDetail  (opcional — solo API layer, ya redactado)
              └── List<Attachment> (screenshots, logs)
```

### `HttpStepDetail` y `HttpDetailRedactor` — Evidencia HTTP

Estas clases están en `reporting.core` (no en `logging`) porque su función es capturar snapshots HTTP para **reportes y persistencia**, no para logging en tiempo real.

```java
// En api-core, al ejecutar una petición HTTP:
HttpStepDetail detail = HttpDetailRedactor.build(
    method, url, requestHeaders, requestBody,
    responseStatus, responseHeaders, responseBody, durationMs);

// El HttpStepDetail resultante es un DTO inmutable con datos sensibles redactados,
// seguro para: base de datos · WebSocket · FE · adjuntos externos
```

### Artefactos generados

| Artefacto | Path | Condición |
|-----------|------|-----------|
| Reporte HTML ExtentReports | `build/reports/extent/execution-report.html` | `extent.enabled=true` |
| cucumber.json | `target/cucumber-reports/cucumber.json` | Plugin JSON registrado en el runner |
| cucumber.html | `target/cucumber-reports/cucumber.html` | Plugin HTML registrado en el runner |

### Integración con plataformas externas — Estado arquitectónico

La integración con Jira/Xray, Azure DevOps y otras plataformas es **responsabilidad del Backend**. El Core provee el HTML y/o el modelo `TestExecutionResult`.

| Funcionalidad | Jira/Xray | Azure DevOps |
|---------------|-----------|--------------|
| Sincronizar resultados | Implementado (BE) | Implementado (BE) |
| Adjuntar HTML (manual, desde FE) | Implementado (BE) | Pendiente |
| Adjuntar HTML (automático) | Pendiente | Pendiente |

**Decisión arquitectónica pendiente — Opción A vs B:**

| | Opción A (actual) | Opción B |
|--|--|--|
| **Quién genera el HTML** | Core (ExtentReports) | Backend |
| **Flujo** | Core produce HTML → BE almacena URL → FE adjunta | Core expone `TestExecutionResult` JSON → BE genera artefacto |
| **Ventaja** | Sin cambios en Core ni BE | Mayor flexibilidad de formato (HTML, PDF, etc.) |
| **Desventaja** | Formato acoplado a ExtentReports en el Core | Requiere `JsonExportStep` + BE genera el reporte |

### Extensión del pipeline

Implementar `ReportingStep` y registrar en `ReportingManager`:

```java
public class JsonExportStep implements ReportingStep {
    @Override
    public PipelineStepResult execute(PipelineContext context) {
        TestExecutionResult result = context.getExecutionResult();
        // serializar a JSON en build/reports/execution-result.json
        return PipelineStepResult.success("jsonExport");
    }

    @Override
    public boolean isEnabled(ReportingConfig config) { return config.isJsonExportEnabled(); }

    @Override
    public String getName() { return "JSON Export"; }
}
```

---

## 13. Contrato con el Backend (API Pública del Core)

El paquete `runtime/` expone las clases que el Backend consume directamente:

| Clase | Paquete | Rol |
|-------|---------|-----|
| `CucumberRuntimeEngine` | `com.qa.common.runtime` | Entry point: `execute(ExecutionRequest)` |
| `ExecutionRequest` | `com.qa.common.runtime` | Parámetros de entrada (features, tags, config) |
| `ExecutionResult` | `com.qa.common.runtime` | Resultado final (estado, métricas, escenarios) |
| `StepDiscoveryService` | `com.qa.common.runtime` | Catálogo de steps disponibles por plugin |
| `EventSubscriber` | `com.qa.common.runtime.events` | Interface para streaming WebSocket |

### Ciclo de ejecución (Backend → Core)

```
1. Backend construye ExecutionRequest con featurePaths + tags + variables
2. Backend registra un EventSubscriber (adapter WebSocket) en el EventBus
3. Backend llama CucumberRuntimeEngine.execute(request)
4. Core activa los plugins según los tags (ApiPlugin, WebPlugin, etc.)
5. Core publica eventos en el EventBus durante la ejecución:
       ScenarioStarted → StepStarted → StepFinished → ScenarioFinished
6. El EventSubscriber del Backend los reenvía al Frontend vía WebSocket
7. execute() retorna ExecutionResult con el estado final
```

### `ExecutionResult` — estructura de datos

```java
ExecutionResult result = engine.execute(request);

result.getStatus()          // PASSED | FAILED | ERROR
result.getTotalScenarios()
result.getPassedScenarios()
result.getFailedScenarios()
result.getDurationMs()
result.getScenarioResults() // List<ScenarioResult>
```

---

## 14. Cómo usar Common en otro módulo

### En `build.gradle` del módulo que usa el framework:

```groovy
dependencies {
    implementation 'com.qa:common:2.0.0'     // Siempre requerido
    implementation 'com.qa:api-core:2.0.0'   // Si necesita probar APIs
    // common viene incluido transitivamente en api-core, web-core y mobile-core
}
```

### Publicar Common localmente (para desarrollo):

```bash
cd qa-platformCore
./gradlew :common:publishToMavenLocal

# O publicar todo el framework de una vez:
./gradlew publishToMavenLocal
```

---

## 15. Dependencias

| Dependencia | Versión | Propósito |
|-------------|---------|-----------|
| **Java** | 21 LTS | Lenguaje base |
| **Cucumber Java** | 7.18.0 | Motor BDD |
| **Cucumber JUnit Platform Engine** | 7.18.0 | Runner para JUnit 5 |
| **JUnit Platform Suite** | 1.10.x | Suite de tests |
| **SLF4J + Logback** | 1.5.x | Sistema de logging |
| **Jackson Databind** | 2.15.x | Serialización JSON |
| **JsonPath** | 2.x | Navegación JSONPath |
| **Unirest** | 4.4.4 | HTTP Client base |
| **HikariCP** | 5.x | Pool de conexiones BD |
| **AssertJ** | 3.24.x | Aserciones fluidas |
| **OJDBC** | 23.x | Driver Oracle |
| **PostgreSQL** | 42.x | Driver PostgreSQL |
| **MySQL Connector** | 8.x | Driver MySQL |
| **MSSQL JDBC** | 12.x | Driver SQL Server |
| **ExtentReports** | 5.x | Generación de reportes HTML |

---

## 16. Convención de IDs de Step

El **ID de un step component** (`stepId`) es el identificador estable que el Backend almacena en la base de datos para referenciar un componente de steps dentro de escenarios, ejecuciones y operaciones de lint/import.

### Formato canónico

```
{capa}.{dominio}[.{subdominio}]
```

| Segmento | Descripción | Ejemplos |
|----------|-------------|---------|
| `{capa}` | Capa origen | `api`, `web`, `mobile`, `db` |
| `{dominio}` | Responsabilidad principal (lowercase) | `authentication`, `navigation`, `device.config` |
| `{subdominio}` | Refinamiento opcional | `response.body`, `validation.element` |

### Cómo declarar un ID

```java
@StepId("api.authentication")
public class ApiAuthComponent implements StepComponent { ... }
```

### Reglas de estabilidad

> El `stepId` es un **contrato público**. El Backend lo persiste. Cambiar un ID sin deprecación previa romperá escenarios existentes.

| Regla | Detalle |
|-------|---------|
| No cambiar IDs sin deprecar primero | Mantener el ID anterior con `deprecated = true` durante al menos una release |
| Declarar `replacedBy` | Para que el Backend pueda migrar automáticamente los escenarios persistidos |
| Unicidad obligatoria | `StepDiscoveryService` detecta duplicados al inicializar |
| Formato consistente | Lowercase, separado con puntos, sin espacios ni guiones bajos |

### Ciclo de deprecación

```java
// Release N — marcar el ID anterior como deprecated
@StepId(value = "api.old.url", deprecated = true, replacedBy = "api.url")
public class ApiUrlComponent implements StepComponent { ... }

// Release N+1 — usar el nuevo ID
@StepId("api.url")
public class ApiUrlComponent implements StepComponent { ... }
```

---

## 17. Catálogo de Steps a nivel método — API v2.3.0

### Modelo de dos niveles

| Nivel | Clase | Descripción |
|---|---|---|
| **Componente** | `StepInfo` | Agrupa steps por responsabilidad. Expuesto en `GET /api/steps`. |
| **Step individual** | `StepDefinitionInfo` | Un método Cucumber concreto. Expuesto en `GET /api/steps/defs`. |

### `ParamSchema` — tipos lógicos de parámetros

| Tipo lógico | Tipos Java que lo generan | Uso en FE |
|---|---|---|
| `string` | `String`, CharSequence | Text input |
| `number` | `int`, `Integer`, `long`, `double`, `BigDecimal`… | Number input |
| `boolean` | `boolean`, `Boolean` | Toggle / checkbox |
| `json` | `Map` (DataTable como mapa) | JSON editor |
| `list` | `List` (DataTable como lista) | Tabla de filas |
| `table` | `DataTable` nativo | Data table editor |
| `docstring` | `String` sin token Cucumber | Text area multilínea |

### `StepDefinitionInfo` enriquecido (v2.3.0)

```
stepDefId()            → "api.authentication.bearer.identifier"
cucumberPattern()      → "agrego autenticación Bearer con identificador {string}"
phase()                → BddPhase.GIVEN
layer()                → "api"
componentId()          → "api.authentication"
params()               → List<ParamInfo>
paramSchemas()         → List<ParamSchema>
displayName()          → "Autenticación Bearer por identificador"
displayNameByLocale()  → {"es": "...", "en": "...", "fr": "..."}
deprecated()           → false
```

### API de `StepDiscoveryService`

```java
StepDiscoveryService discovery = StepDiscoveryService.withServiceLoader();

List<StepDefinitionInfo> catalog = discovery.discoverAllSteps();

Optional<StepDefinitionInfo> sdi = discovery.findById("api.authentication.bearer.identifier");
sdi.ifPresent(s -> {
    log.info("Patrón: {}", s.cucumberPattern());
    s.paramSchemas().forEach(p ->
        log.info("  {} : {} required={}", p.name(), p.type(), p.required()));
});
```

### Declarar IDs estables con `@StepDef`

```java
@StepDef("api.authentication.bearer.rut")
@Given("agrego autenticación Bearer para RUT {string}")
public void agregoAutenticacionBearerParaRUT(String rut) { ... }

// Ciclo de deprecación
@StepDef(value = "api.auth.old", deprecated = true, replacedBy = "api.auth.bearer.rut")
@Given("patron viejo")
public void patronViejo() { ... }
```

### Steps anotados por módulo (referencia)

| Módulo | Componente | IDs canónicos |
|---|---|---|
| api-core | `api.url` | `api.url.set-endpoint`, `api.url.set-base-path`, `api.url.set-host`, `api.url.set-full-url`, `api.url.set-protocol`, `api.url.set-timeout`, `api.url.set-encoding` |
| api-core | `api.authentication` | `api.authentication.client-credentials`, `api.authentication.bearer.identifier`, `api.authentication.custom-token`, `api.authentication.basic`, `api.authentication.oauth2`, `api.authentication.api-key.header`, `api.authentication.api-key.query`, `api.authentication.jwt`, `api.authentication.none` |
| api-core | `api.execution` | `api.execution.execute`, `api.execution.get`, `api.execution.post`, `api.execution.put`, `api.execution.patch`, `api.execution.delete`, `api.execution.with-timeout`, `api.execution.poll-status`, `api.execution.poll-field` |
| api-core | `api.status` | `api.status.exact`, `api.status.success`, `api.status.client-error`, `api.status.server-error`, `api.status.range`, `api.status.not` |
| web-core | `web.navigation` | `web.navigation.go-to-url`, `web.navigation.refresh`, `web.navigation.back`, `web.navigation.forward` |
| web-core | `web.input` | `web.input.type-text`, `web.input.type-from-variable`, `web.input.type-random-name`, `web.input.type-if-exists`, `web.input.clear`, `web.input.upload-file` |
| mobile-core | `mobile.device.config` | `mobile.device.config.platform`, `mobile.device.config.device-id`, `mobile.device.config.platform-version`, `mobile.device.config.emulator`, `mobile.device.config.physical`, `mobile.device.config.ios-simulator`, `mobile.device.config.appium-server`, `mobile.device.config.orientation`, `mobile.device.config.capabilities`, `mobile.device.config.udid` |

> Los steps de los componentes restantes (headers, body, response, click, gestos, etc.) están pendientes de anotar con `@StepDef`. Sus IDs derivados siguen el patrón `{componentId}#{methodName}` hasta que se agreguen las anotaciones explícitas.

---

> **Documentación relacionada:**
> - [api-core/README.md](../api-core/README.md) — Capa de pruebas de API
> - [web-core/README.md](../web-core/README.md) — Capa de pruebas Web
> - [mobile-core/README.md](../mobile-core/README.md) — Capa de pruebas Mobile
> - [README.md](../README.md) — Visión general del framework
> - [reporting/README.md](src/main/java/com/qa/common/reporting/README.md) — Módulo de reportes
