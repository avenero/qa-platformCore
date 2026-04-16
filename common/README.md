# common — Capa Base del Framework (CuAleon Core)

> **Versión:** 2.0.0 | **Grupo:** `com.qa` | **Artefacto:** `common`  
> **Última actualización:** Abril 2026  
> **Autor:** Abel Venero

---

## 📑 Índice

1. [¿Qué es Common?](#1-qué-es-common)
2. [Mapa completo de paquetes](#2-mapa-completo-de-paquetes)
3. [El Motor de Ejecución — runtime/](#3-el-motor-de-ejecución--runtime)
4. [Sistema de Logging — logging/](#4-sistema-de-logging--logging)
5. [Gestión de Configuración — config/](#5-gestión-de-configuración--config)
6. [HTTP Base — http/](#6-http-base--http)
7. [Base de Datos — database/](#7-base-de-datos--database)
8. [Utilidades — utils/](#8-utilidades--utils)
9. [Hooks de Cucumber — cucumber/hooks/](#9-hooks-de-cucumber--cucumberhooks)
10. [Driver (WebDriver compartido) — driver/](#10-driver-webdriver-compartido--driver)
11. [Reportes — reporting/](#11-reportes--reporting)
12. [Cómo usar Common en otro módulo](#12-cómo-usar-common-en-otro-módulo)
13. [Dependencias](#13-dependencias)
14. [Convención de IDs de Step](#14-convención-de-ids-de-step)

---

## 1. ¿Qué es Common?

**Common** es la **capa fundacional** del framework — la base sobre la que se construyen las capas especializadas (`api-core`, `web-core`, `mobile-core`). Ninguna de esas capas puede funcionar sin `common`.

Piénsalo como los **cimientos de un edificio**: no ves los cimientos cuando miras el edificio terminado, pero sin ellos, nada se sostiene.

### ¿Qué hace Common?

| Paquete | Función en palabras simples |
|---------|----------------------------|
| `runtime/` | El motor que orquesta cómo se ejecutan los escenarios de prueba |
| `logging/` | Registra todo lo que pasa durante las pruebas, de forma ordenada y segura |
| `config/` | Lee la configuración del proyecto (URLs, credenciales, timeouts) |
| `http/` | Define el modelo básico de una petición/respuesta HTTP |
| `database/` | Conecta con bases de datos (Oracle, PostgreSQL, MySQL, SQL Server) |
| `utils/` | Herramientas para manejar JSON, textos, fechas, archivos y variables |
| `cucumber/hooks/` | Administra el ciclo de vida de cada escenario de prueba |
| `driver/` | Base compartida para el manejo de WebDrivers |
| `reporting/` | Genera evidencias y reportes de las pruebas ejecutadas |

### ¿Qué NO hace Common?

- ❌ **No contiene steps de Selenium ni Appium** (esos van en web-core y mobile-core)
- ❌ **No tiene lógica de negocio** (eso va en el proyecto de pruebas)
- ❌ **No conoce las URLs ni la estructura** de ningún sistema específico

> **Nota:** `database/` sí contiene steps BDD (`DatabaseConnectionSteps`) porque la BD es un recurso transversal usado por cualquier capa — no es exclusivo de API, Web ni Mobile.

---

## 2. Mapa Completo de Paquetes

```
common/
└── src/main/java/com/qa/common/
    │
    ├── runtime/                         ← ⭐ MOTOR DE EJECUCIÓN (novedad v2.0)
    │   ├── CorePlugin.java              ← Interfaz que todo plugin debe implementar
    │   ├── CucumberRuntimeEngine.java   ← Director de la orquesta de ejecución
    │   ├── ExecutionContext.java        ← Tablero de control de un escenario
    │   ├── ExecutionConfig.java         ← Configuración de una ejecución
    │   ├── ExecutionRequest.java        ← Solicitud de ejecución
    │   ├── ExecutionResult.java         ← Resultado de una ejecución
    │   ├── ServiceRegistry.java         ← Casillero de servicios (lazy injection)
    │   ├── VariableStore.java           ← Cuaderno de variables entre steps
    │   ├── StepComponent.java           ← Ficha técnica de un grupo de steps
    │   ├── BddPhase.java                ← Enum: GIVEN, WHEN, THEN
    │   ├── DefaultLifecycleManager.java ← Gestiona ciclo de vida de plugins
    │   ├── LifecycleManager.java        ← Interfaz del lifecycle manager
    │   ├── InMemoryResultCollector.java ← Colecta resultados en memoria
    │   ├── ScenarioMetadata.java        ← Record: metadata de un escenario (2.1.0)
    │   ├── ScenarioLifecycleBridge.java ← Bridge Cucumber events → LifecycleManager (2.1.0)
    │   ├── StepDiscoveryService.java    ← Descubre steps (nivel componente y nivel método)
    │   ├── StepInfo.java                ← DTO nivel componente (para el Backend)
    │   ├── StepDefinitionInfo.java      ← DTO nivel step individual (2.2.0) ⭐
    │   ├── ParamInfo.java               ← DTO parámetro de un step (2.2.0)
    │   ├── StepMethodScanner.java       ← Scanner reflexivo @Given/@When/@Then (2.2.0) ⭐
    │   ├── annotation/
    │   │   ├── StepId.java              ← @StepId en clase StepComponent (ID estable)
    │   │   └── StepDef.java             ← @StepDef en método step (ID nivel método) (2.2.0)
    │   └── events/                      ← Eventos del ciclo de vida
    │
    ├── logging/                         ← SISTEMA DE LOGGING
    │   ├── TestLogger.java              ← Logger principal con contexto automático
    │   └── LoggingInitializer.java      ← Inicializa el contexto de log (MDC)
    │
    ├── config/                          ← GESTIÓN DE CONFIGURACIÓN
    │   ├── ConfigManager.java           ← Singleton que lee config del proyecto
    │   └── providers/                   ← Proveedores de configuración por fuente
    │
    ├── http/                            ← HTTP BASE (modelos compartidos)
    │   ├── model/
    │   │   └── HttpResponse.java        ← Modelo de respuesta HTTP (status+body+headers)
    │   ├── enums/
    │   │   └── HttpMethod.java          ← Enum: GET, POST, PUT, PATCH, DELETE
    │   └── exceptions/
    │       ├── FrameworkBusinessException.java   ← Excepción de validación fallida
    │       └── FrameworkTechnicalException.java  ← Excepción técnica (timeout, red)
    │
    ├── database/                        ← PLUGIN DE BASE DE DATOS (CorePlugin)
    │   ├── plugin/
    │   │   └── DatabasePlugin.java       ← Implementa CorePlugin; orden=0 (primero)
    │   ├── components/                   ← 3 StepComponent (GIVEN/WHEN/THEN)
    │   │   ├── DatabaseSetupComponent.java      ← GIVEN: establecer conexión
    │   │   ├── DatabaseExecutionComponent.java  ← WHEN: ejecutar SQL
    │   │   └── DatabaseValidationComponent.java ← THEN: validar resultados
    │   ├── connectors/
    │   │   ├── BaseConnector.java        ← Base compartida
    │   │   ├── OracleConnector.java      ← Oracle DB
    │   │   ├── PostgreSQLConnector.java  ← PostgreSQL
    │   │   ├── MySQLConnector.java       ← MySQL
    │   │   └── SQLServerConnector.java   ← SQL Server
    │   ├── factory/
    │   │   └── DbConnectorFactory.java   ← Crea y cachea conectores
    │   ├── helpers/
    │   │   └── DatabaseHelper.java       ← Fachada: ejecuta queries y valida resultados
    │   ├── interfaces/
    │   │   └── DatabaseConnector.java    ← Interfaz de conector genérico
    │   ├── config/
    │   │   └── DatabaseConfig.java       ← Configuración HikariCP
    │   ├── repository/
    │   │   └── QueryRepository.java      ← Ejecuta queries genéricos (sin steps)
    │   └── steps/
    │       └── DatabaseConnectionSteps.java ← Steps BDD para BD (GIVEN/WHEN/THEN)
    │
    ├── utils/                           ← UTILIDADES GENERALES
    │   ├── DataUtilities.java           ← Variables entre steps + interpolación ${...}
    │   ├── DataGenerator.java           ← Genera datos aleatorios (UUID, timestamps, etc.)
    │   ├── JsonUtilities.java           ← Parseo y manipulación de JSON
    │   ├── TextUtilities.java           ← Operaciones con texto
    │   ├── FileUtilities.java           ← Lectura de archivos
    │   ├── SecurityUtilities.java       ← Enmascaramiento de datos sensibles
    │   └── ConfigurationUtilities.java  ← Helpers para configuración
    │
    ├── cucumber/hooks/                  ← HOOKS DE CUCUMBER
    │   └── ScenarioExecutionHooks.java  ← Hooks Before/After del ciclo de vida
    │
    ├── driver/                          ← BASE DE DRIVER (compartida por web y mobile)
    │
    └── reporting/                       ← REPORTES Y EVIDENCIAS
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
// Obtener el contexto del escenario actual
ExecutionContext ctx = ExecutionContext.current();

// Acceder a un servicio registrado (ej: desde un step)
HttpClient client = ctx.service(HttpClient.class);

// Acceder a las variables del escenario
ctx.variables().set("token", "Bearer eyJhbG...");
String token = ctx.variables().resolve("token");

// Interpolar variables en un string
String body = ctx.variables().interpolate('{"token": "${token}"}');
// → {"token": "Bearer eyJhbG..."}
```

### `ServiceRegistry` — El casillero de servicios

El `ServiceRegistry` es el mecanismo de **inyección de dependencias sin Spring**. Funciona con inicialización *lazy* (perezosa): los servicios se crean solo cuando alguien los pide por primera vez.

```java
// En ApiPlugin.registerServices():
registry.registerLazy(HttpClient.class, () -> HttpClientFactory.create(config));

// En un step, al ejecutarse:
HttpClient client = context.service(HttpClient.class);
// → Se crea el HttpClient en ese momento (si no existía)
// → La siguiente vez que se pida, se reutiliza el mismo objeto
```

**¿Por qué lazy?** Porque si un escenario tiene `@api` y `@web` pero no usa autenticación, no tiene sentido crear el `AuthenticationService`. Se crea solo si algún step lo pide.

### `VariableStore` — El cuaderno de notas

Almacena variables que los steps guardan y comparten entre sí:

```java
VariableStore vars = context.variables();

// Guardar una variable
vars.set("userId", "12345");

// Leer una variable
String id = vars.resolve("userId");  // → "12345"

// Reemplazar ${variables} en un texto
String body = vars.interpolate("https://api.com/users/${userId}");
// → "https://api.com/users/12345"
```

### `BddPhase` — Enum de fases

```java
BddPhase.GIVEN  // Pasos de configuración (Given / And antes de When)
BddPhase.WHEN   // Pasos de ejecución (When)
BddPhase.THEN   // Pasos de validación (Then / And después de When)
```

---

## 4. Sistema de Logging — `logging/`

### `TestLogger` — El logger principal

Todos los steps y servicios del framework usan `TestLogger` (nunca `System.out.println()`). Agrega automáticamente contexto de módulo y escenario a cada línea de log.

```java
// Mensajes informacionales
TestLogger.logInfo("API_HELPER_CONFIG", "✅ Host base establecido: " + url);

// Mensajes de error
TestLogger.logError("HTTP_ERROR", "Error al ejecutar petición: " + e.getMessage());

// Mensajes de debug (no aparecen en producción)
TestLogger.logDebug("HTTP_EXEC", "Armando petición con headers: " + headers);
```

**Formato en el log:**
```
12:02:36.014 INFO  [API] [Mi Escenario] com.qa.common.logging.TestLogger - [API_HELPER_CONFIG] ✅ Host base establecido: https://api.com
```

Cada parte tiene significado:
- `[API]` → Módulo activo (api, web, mobile)
- `[Mi Escenario]` → Nombre del escenario en ejecución
- `[API_HELPER_CONFIG]` → Categoría del mensaje (fácil de filtrar en logs)

### Enmascaramiento automático de datos sensibles

El framework detecta automáticamente palabras clave sensibles en los logs y las enmascara:

```
Antes: Authorization: Bearer eyJhbGciOiJIUzM4...
Después: Authorization: Bearer ***MASKED***
```

Las palabras clave que se enmascaran incluyen: `password`, `token`, `secret`, `authorization`, `apikey`, `api_key`, `credential`.

---

## 5. Gestión de Configuración — `config/`

### `ConfigManager` — El lector de configuración

Singleton que lee configuración desde múltiples fuentes en orden de prioridad:

```
1. Propiedades del sistema (-Dkey=value en línea de comandos)
2. Variables de entorno (export KEY=value)
3. Archivo config-app.properties (en src/test/resources/)
4. Valores por defecto del framework
```

**Uso básico:**

```java
ConfigManager config = ConfigManager.getInstance();

// Leer un valor
String baseUrl = config.get("api.base.url");

// Leer con valor por defecto si no existe
int timeout = Integer.parseInt(config.get("api.timeout", "30000"));

// Verificar si existe una clave
if (config.has("db.url")) {
    // conectar a BD...
}
```

**Archivo de configuración del proyecto** (`config-app.properties`):

```properties
# URLs del sistema a probar
api.base.url=https://mi-sistema-qa.com/
web.base.url=https://mi-sistema-qa.com

# Configuración del navegador
web.browser=chrome
web.headless=true
web.timeout=30

# Timeouts
api.timeout=30000

# Variables que se toman de environment (.env.local)
db.username=${DB_USER}
db.password=${DB_PASS}
```

---

## 6. HTTP Base — `http/`

Este paquete define los **tipos de datos compartidos** que todas las capas usan para representar peticiones y respuestas HTTP. No hace peticiones por sí solo — eso lo hace `api-core` con `BaseHttpClient`.

### `HttpResponse` — Modelo de respuesta

```java
// Representa cualquier respuesta HTTP recibida
HttpResponse response = httpClient.getLastResponse();

int status    = response.getStatus();    // Ej: 200, 404, 500
String body   = response.getBody();      // El cuerpo como texto
Map<String, String> headers = response.getHeaders(); // Headers de respuesta
long timeMs   = response.getElapsedTimeMs(); // Tiempo en milisegundos
```

### `HttpMethod` — Métodos HTTP

```java
// Los métodos HTTP estándar
HttpMethod.GET     // Consultar datos
HttpMethod.POST    // Crear / enviar datos
HttpMethod.PUT     // Reemplazar datos
HttpMethod.PATCH   // Modificar parcialmente
HttpMethod.DELETE  // Eliminar datos
```

### Excepciones

| Excepción | Cuándo se lanza |
|-----------|-----------------|
| `FrameworkBusinessException` | Una validación falló (ej: status esperado 200, se recibió 404) |
| `FrameworkTechnicalException` | Error técnico (timeout, sin conexión de red, servidor no disponible) |

Ambas extienden de `RuntimeException`, por lo que no requieren declaración en `throws`.

---

## 7. Base de Datos — `database/`

Permite a los tests conectarse a bases de datos para **preparar datos de prueba** o **verificar que las operaciones del sistema afectaron la BD correctamente**.

### Flujo de conexión

```
DbConnectorFactory.connectAndCache("oracle")
        │
        ├── Lee oracle.db.url, oracle.db.username, oracle.db.password de config
        ├── Detecta el tipo de BD por la URL
        ├── Crea OracleConnector con pool HikariCP
        └── Cachea la conexión (reutilizable en el mismo escenario)
```

### Conectores disponibles

| Conector | Tipo de BD | URL típica |
|----------|-----------|------------|
| `OracleConnector` | Oracle DB | `jdbc:oracle:thin:@//host:1521/NOMBRE` |
| `PostgreSQLConnector` | PostgreSQL | `jdbc:postgresql://host:5432/nombre` |
| `MySQLConnector` | MySQL | `jdbc:mysql://host:3306/nombre` |
| `SQLServerConnector` | SQL Server | `jdbc:sqlserver://host:1433;databaseName=nombre` |

### `DatabaseHelper` — Ejecutar queries

```java
// Ejecutar una query que devuelve una fila
Map<String, Object> resultado = DatabaseHelper.executeQuery(
    connector,
    "SELECT balance, status FROM accounts WHERE user_id = ?",
    "12345"
);

// Obtener un valor específico
Object saldo = DatabaseHelper.getColumnValue(resultado, "balance");

// Validar que la query retornó resultados
DatabaseHelper.validateHasResults(resultado);

// Validar el valor de una columna
DatabaseHelper.validateColumnValue(resultado, "status", "ACTIVE");
```

### Steps de BD disponibles (Gherkin)

```gherkin
# Conectar a la BD
Given establezco conexion a base de datos "oracle"
Given establezco conexion a base de datos "postgresql"

# Ejecutar queries
When ejecuto la consulta "SELECT * FROM users WHERE id = ?" con parametros "12345"
When ejecuto la sentencia "UPDATE users SET status = ? WHERE id = ?" con parametros "ACTIVE,12345"

# Validar resultados
Then valido que la consulta retorne resultados
Then valido que la consulta no retorne resultados
Then valido que la columna "status" tenga el valor "ACTIVE"
Then obtengo el valor de la columna "balance" y lo almaceno en "saldo"
```

**Configuración necesaria en `config-app.properties`:**

```properties
# Una sección por cada BD que se use
oracle.db.url=jdbc:oracle:thin:@//servidor:1521/DB
oracle.db.username=${ORACLE_USER}
oracle.db.password=${ORACLE_PASS}

postgresql.db.url=jdbc:postgresql://servidor:5432/testdb
postgresql.db.username=${PG_USER}
postgresql.db.password=${PG_PASS}
```

---

## 8. Utilidades — `utils/`

### `DataUtilities` — Variables entre steps e interpolación

Esta es la clase más usada por los steps. Gestiona las variables del escenario y reemplaza `${variable}` en textos.

```java
// Guardar una variable (típicamente desde un step "And almaceno...")
DataUtilities.storeValue("token", "Bearer eyJhbG...");

// Leer una variable
String token = DataUtilities.getValue("token");

// Reemplazar ${variables} en un texto
String body = DataUtilities.replaceVariables('{"token": "${token}"}');
// → '{"token": "Bearer eyJhbG..."}'

// Verificar si un JSON tiene un campo (usando JSONPath)
boolean existe = DataUtilities.hasJsonField(responseBody, "$.user.id");

// Extraer un valor de un JSON
String userId = DataUtilities.getJsonParameter(responseBody, "$.user.id");
```

### `DataGenerator` — Generar datos de prueba

```java
// UUID v4 aleatorio (para IDs únicos en cada ejecución)
String uuid = DataGenerator.generateUUID();
// → "550e8400-e29b-41d4-a716-446655440000"

// Timestamp actual en milisegundos
long ts = DataGenerator.generateTimestamp();

// Número aleatorio en un rango
int numero = DataGenerator.generateRandomNumber(1, 100);

// String aleatorio de longitud N
String random = DataGenerator.generateRandomString(8);
```

### `JsonUtilities` — Manipular JSON

```java
// Parsear un JSON a un Map
Map<String, Object> mapa = JsonUtilities.toMap(jsonString);

// Convertir un objeto a JSON
String json = JsonUtilities.toJson(miObjeto);

// Extraer valor con JSONPath
Object valor = JsonUtilities.extractValue(jsonString, "$.user.name");

// Validar si un string es JSON válido
boolean esJson = JsonUtilities.isValidJson(texto);
```

### `SecurityUtilities` — Datos sensibles

```java
// Enmascarar un valor sensible para el log
String seguro = SecurityUtilities.mask("mi-contraseña-secreta");
// → "mi-c********************"

// Verificar si una clave parece sensible
boolean esSensible = SecurityUtilities.isSensitiveKey("password");
// → true
```

---

## 9. Hooks de Cucumber — `cucumber/hooks/`

### `ScenarioExecutionHooks` — El ciclo de vida del escenario

Esta clase se ejecuta automáticamente antes y después de **cada escenario**:

```
@Before (orden 0)
    → Inicializar el ExecutionContext
    → Activar los plugins correspondientes a los tags del escenario
    → Registrar los servicios que cada plugin declara
    → Llamar onScenarioStart() en cada plugin activo

Cucumber ejecuta los steps del escenario...

@After (orden 0)
    → Llamar onScenarioEnd() en cada plugin activo
    → Limpiar el ExecutionContext
    → Limpiar las variables del escenario (VariableStore)
```

**¿Por qué importa esto?** Porque garantiza que cada escenario **empieza con estado limpio**. El cliente HTTP del escenario anterior no contamina el siguiente.

---

## 10. Driver (WebDriver compartido) — `driver/`

Paquete base con utilidades compartidas entre `web-core` (Selenium) y `mobile-core` (Appium). Contiene abstracciones genéricas para el manejo de drivers que luego cada capa especializa.

---

## 11. Reportes — `reporting/`

Gestiona la generación de evidencias de las pruebas:
- Capturas de pantalla automáticas en fallos
- Logs de ejecución por escenario
- Datos de contexto cuando falla un test

---

## 12. Contrato con el Backend (API Pública del Core)

El paquete `runtime/` expone las clases que el Backend de CuAleon consume directamente. **Estas son las únicas clases que el Backend debe importar:**

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
result.getTotalScenarios()  // int: total de escenarios ejecutados
result.getPassedScenarios() // int: escenarios exitosos
result.getFailedScenarios() // int: escenarios fallidos
result.getDurationMs()      // long: tiempo total en ms
result.getScenarioResults() // List<ScenarioResult>: detalle por escenario
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
cd qa-frameworks-core
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

---

## 13. Catálogo de Steps — Modelo de Dos Niveles (v2.2.0)

El `StepDiscoveryService` expone el catálogo en dos granularidades complementarias:

### Nivel 1: Componente (`StepInfo`)

Agrupa steps por responsabilidad. Cada `StepComponent` es un grupo cohesivo de steps.

```java
// Endpoint sugerido: GET /api/steps
List<StepInfo> components = discovery.discoverAllAsStepInfo();
// → [{id: "api.authentication", layer: "api", phase: "GIVEN", ...}, ...]

// Resolver un componente por su ID
Optional<ComponentInfo> info = discovery.resolveStep("api.authentication");
```

### Nivel 2: Step Individual (`StepDefinitionInfo`)

Representa cada método `@Given/@When/@Then` con su patrón Cucumber y parámetros. Usa reflexión sobre `StepComponent.getStepDefinitionClass()`.

```java
// Endpoint sugerido: GET /api/steps/defs
List<StepDefinitionInfo> defs = discovery.discoverAllStepDefs();
// → [{stepDefId: "api.auth.bearer.rut",
//     cucumberPattern: "agrego autenticación Bearer para RUT {string}",
//     phase: GIVEN, layer: "api", componentId: "api.authentication",
//     params: [{position:0, name:"rut", javaType:"String", cucumberToken:"{string}"}]}, ...]

// Resolver un step individual por su ID
Optional<StepDefinitionInfo> sdi = discovery.resolveStepDef("api.auth.bearer.rut");

// Obtener todos los steps de un componente
List<StepDefinitionInfo> authSteps = discovery.discoverStepDefsByComponent("api.authentication");
```

### Declarar IDs estables con `@StepDef`

La anotación `@StepDef` en el método de step declara su ID estable. Sin ella, el scanner deriva un ID como `{componentId}#{methodName}` (menos estable ante renombrados):

```java
// En AuthenticationSteps.java
@StepDef("api.authentication.bearer.rut")        // ID estable declarado explícitamente
@Given("agrego autenticación Bearer para RUT {string}")
public void agregoAutenticacionBearerParaRUT(String rut) { ... }

// Ciclo de deprecación a nivel step
@StepDef(value = "api.auth.old", deprecated = true, replacedBy = "api.auth.bearer.rut")
@Given("patron viejo")
public void patronViejo() { ... }
```

### Extracción de parámetros

El scanner mapea tokens del patrón Cucumber a parámetros Java por posición:

| Patrón | Parámetro Java | `cucumberToken` | `javaType` |
|--------|---------------|-----------------|------------|
| `{string}` | `String rut` | `"{string}"` | `"String"` |
| `{int}` | `int code` | `"{int}"` | `"int"` |
| *(sin token)* | `Map<String,String> claims` | `null` | `"Map"` |

Los parámetros sin token (`cucumberToken == null`) son DataTable o DocString inyectados por Cucumber.

---

## 14. Convención de IDs de Step

El **ID de un step component** (`stepId`) es el identificador estable que el Backend almacena en la base de datos para referenciar un componente de steps dentro de escenarios, ejecuciones, exports y operaciones de lint/import. Es el contrato de integración entre Core, Backend y Frontend.

### Formato canónico

```
{capa}.{dominio}[.{subdominio}]
```

| Segmento | Descripción | Ejemplos |
|----------|-------------|---------|
| `{capa}` | Capa origen del componente | `api`, `web`, `mobile`, `db` |
| `{dominio}` | Responsabilidad principal (lowercase, sin espacios) | `authentication`, `navigation`, `device.config` |
| `{subdominio}` | *(Opcional)* Refinamiento cuando hay varios componentes en el mismo dominio | `response.body`, `validation.element` |

### Cómo declarar un ID

Toda clase que implementa `StepComponent` **debe** llevar la anotación `@StepId`:

```java
import com.qa.common.runtime.annotation.StepId;

@StepId("api.authentication")
public class ApiAuthComponent implements StepComponent {
    // No es necesario hacer @Override de getId() — la anotación lo resuelve
    ...
}
```

La anotación tiene precedencia sobre cualquier implementación del método `getId()`. El método `StepComponent.getId()` la lee automáticamente vía reflexión en tiempo de ejecución.

### Reglas de estabilidad

> ⚠️ El `stepId` es un **contrato público**. El Backend lo persiste en la base de datos. Cambiar un ID sin deprecación previa romperá escenarios existentes.

| Regla | Detalle |
|-------|---------|
| **No cambiar IDs sin deprecar primero** | Si se debe renombrar un componente, mantener el ID anterior marcado como `deprecated = true` durante al menos una release. |
| **Declarar `replacedBy`** | Al deprecar, indicar el ID del sucesor para que el Backend pueda migrar automáticamente los escenarios persistidos. |
| **Unicidad obligatoria** | No pueden existir dos componentes con el mismo `stepId` en el classpath. `StepDiscoveryService` detecta duplicados al inicializar y emite una advertencia. |
| **Formato consistente** | Lowercase, separado con puntos, sin espacios ni guiones bajos. Se permiten guiones en el último segmento (`app-state`, `drag-drop`) solo si mejoran la legibilidad. |

### Ciclo de deprecación

```java
// Release N — marcar el ID anterior como deprecated
@StepId(value = "api.old.url", deprecated = true, replacedBy = "api.url")
public class ApiUrlComponent implements StepComponent { ... }

// Release N+1 — usar el nuevo ID
@StepId("api.url")
public class ApiUrlComponent implements StepComponent { ... }
```

### Resolución desde el Backend

El `StepDiscoveryService` expone `resolveStep(String stepId)` como bridge principal:

```java
// En un servicio del Backend (ej: ScenarioExecutionService)
StepDiscoveryService discovery = CucumberRuntimeEngine.withServiceLoader().getDiscoveryService();

Optional<StepDiscoveryService.ComponentInfo> info = discovery.resolveStep("api.authentication");

info.ifPresent(c -> {
    // c.component() → el StepComponent
    // c.pluginName() → "api"
    // c.getDisplayNameForLocale("en") → "Authentication"
});
```

---

## 15. Catálogo de Steps a nivel método — API v2.3.0

> Contrato de catálogo step-level para macros/CustomSteps, lint BDD y sugerencias IA.

### 15.1 Modelo de dos niveles

| Nivel | Clase | Descripción |
|---|---|---|
| **Componente** | `StepInfo` | Agrupa steps por responsabilidad (ej: `api.authentication`). Expuesto en `GET /api/steps`. |
| **Step individual** | `StepDefinitionInfo` | Un método Cucumber concreto (ej: `api.authentication.bearer.identifier`). Expuesto en `GET /api/steps/defs`. |

### 15.2 `ParamSchema` — tipos lógicos de parámetros

`ParamSchema` es la vista semántica de un parámetro, orientada al consumo por el Backend y el FE:

| Tipo lógico | Tipos Java que lo generan | Uso en FE |
|---|---|---|
| `string` | `String`, CharSequence | Text input |
| `number` | `int`, `Integer`, `long`, `double`, `BigDecimal`… | Number input |
| `boolean` | `boolean`, `Boolean` | Toggle / checkbox |
| `json` | `Map` (DataTable como mapa) | JSON editor |
| `list` | `List` (DataTable como lista) | Tabla de filas |
| `table` | `DataTable` nativo | Data table editor |
| `docstring` | `String` sin token Cucumber | Text area multilínea |

Obtención desde el Backend:
```java
StepDefinitionInfo sdi = discovery.findById("api.url.set-endpoint").orElseThrow();
sdi.paramSchemas().forEach(schema ->
    log.info("param={} type={} required={}", schema.name(), schema.type(), schema.required()));
```

### 15.3 `StepDefinitionInfo` enriquecido (v2.3.0)

Desde v2.3.0, `StepDefinitionInfo` incluye:

```
stepDefId()            → "api.authentication.bearer.identifier"
cucumberPattern()      → "agrego autenticación Bearer con identificador {string}"
phase()                → BddPhase.GIVEN
layer()                → "api"
componentId()          → "api.authentication"
params()               → List<ParamInfo>  (reflexión Java)
paramSchemas()         → List<ParamSchema> (tipos lógicos — derivado de params())
displayName()          → "Autenticación Bearer por identificador"
displayNameByLocale()  → {"es": "...", "en": "...", "fr": "..."} (heredado del componente)
descriptionByLocale()  → {"es": "...", "en": "...", "fr": "..."} (heredado del componente)
deprecated()           → false
replacementStepDefId() → null
```

### 15.4 API de `StepDiscoveryService` — nivel step

```java
StepDiscoveryService discovery = StepDiscoveryService.withServiceLoader();

// Catálogo completo de steps individuales
List<StepDefinitionInfo> catalog = discovery.discoverAllSteps();

// Resolución directa por ID (bridge BE ↔ Core)
Optional<StepDefinitionInfo> sdi = discovery.findById("api.authentication.bearer.identifier");
sdi.ifPresent(s -> {
    log.info("Patrón: {}", s.cucumberPattern());
    log.info("Nombre ES: {}", s.getDisplayNameForLocale("es"));
    s.paramSchemas().forEach(p ->
        log.info("  {} : {} required={}", p.name(), p.type(), p.required()));
});

// También disponibles (nombres legacy, misma semántica):
List<StepDefinitionInfo> all  = discovery.discoverAllStepDefs();   // = discoverAllSteps()
Optional<StepDefinitionInfo>  = discovery.resolveStepDef("id");    // = findById("id")
```

### 15.5 Convención de IDs con `@StepDef`

Los IDs de step explícitos se declaran con la anotación `@StepDef` (en el método) y siguen el formato:

```
{componentId}.{sub-id}
```

Ejemplos:
```java
@StepDef("api.url.set-endpoint")        // componente: api.url
@StepDef("api.authentication.basic")    // componente: api.authentication
@StepDef("web.navigation.go-to-url")    // componente: web.navigation
@StepDef("mobile.device.config.platform") // componente: mobile.device.config
```

Sin `@StepDef`, el scanner deriva el ID como `{componentId}#{methodName}` (menos estable frente a renombrados).

**Contrato de estabilidad:** una vez publicado, un `@StepDef` ID es un contrato público. Para cambiarlo:
```java
// Paso 1 (mínimo una release): marcar como deprecated
@StepDef(value = "api.url.legacy-ambiente",
         deprecated = true, replacedBy = "api.url.set-endpoint")
@Given("configuro el ambiente {string}")
public void configuroElAmbiente(String env) { ... }

// Paso 2 (release siguiente): eliminar el step antiguo
```

### 15.6 Steps anotados por módulo (referencia)

| Módulo | Componente | IDs canónicos |
|---|---|---|
| api-core | `api.url` | `api.url.set-endpoint`, `api.url.set-base-path`, `api.url.set-host`, `api.url.set-full-url`, `api.url.set-protocol`, `api.url.set-timeout`, `api.url.set-encoding` |
| api-core | `api.authentication` | `api.authentication.client-credentials`, `api.authentication.bearer.identifier`, `api.authentication.custom-token`, `api.authentication.basic`, `api.authentication.oauth2`, `api.authentication.api-key.header`, `api.authentication.api-key.query`, `api.authentication.jwt`, `api.authentication.none` |
| api-core | `api.execution` | `api.execution.execute`, `api.execution.get`, `api.execution.post`, `api.execution.put`, `api.execution.patch`, `api.execution.delete`, `api.execution.with-timeout`, `api.execution.poll-status`, `api.execution.poll-field` |
| api-core | `api.status` | `api.status.exact`, `api.status.success`, `api.status.client-error`, `api.status.server-error`, `api.status.range`, `api.status.not` |
| web-core | `web.navigation` | `web.navigation.go-to-url`, `web.navigation.refresh`, `web.navigation.back`, `web.navigation.forward` |
| web-core | `web.input` | `web.input.type-text`, `web.input.type-from-variable`, `web.input.type-random-name`, `web.input.type-if-exists`, `web.input.clear`, `web.input.upload-file` |
| mobile-core | `mobile.device.config` | `mobile.device.config.platform`, `mobile.device.config.device-id`, `mobile.device.config.platform-version`, `mobile.device.config.emulator`, `mobile.device.config.physical`, `mobile.device.config.ios-simulator`, `mobile.device.config.appium-server`, `mobile.device.config.orientation`, `mobile.device.config.capabilities`, `mobile.device.config.udid` |

> Los steps del resto de componentes (headers, body, response, click, gestos, etc.) están
> **pendientes de anotar** con `@StepDef`. Sus IDs derivados siguen el patrón
> `{componentId}#{methodName}` hasta que se agreguen las anotaciones explícitas.

---

> 📖 **Documentación relacionada:**
> - [api-core/README.md](../api-core/README.md) — Capa de pruebas de API
> - [web-core/README.md](../web-core/README.md) — Capa de pruebas Web
> - [mobile-core/README.md](../mobile-core/README.md) — Capa de pruebas Mobile
> - [README.md](../README.md) — Visión general del framework
