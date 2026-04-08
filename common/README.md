# 🔧 common — Capa Base del Framework

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

- ❌ **No contiene steps de Cucumber** (esos van en las capas especializadas)
- ❌ **No sabe de Selenium, Appium ni API REST** (es completamente genérico)
- ❌ **No tiene lógica de negocio** (eso va en el proyecto de pruebas)
- ❌ **No conoce las URLs ni la estructura** de ningún sistema específico

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
    │   ├── StepDiscoveryService.java    ← Descubre steps disponibles
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
    ├── database/                        ← CONEXIÓN A BASE DE DATOS
    │   ├── connectors/
    │   │   ├── BaseConnector.java        ← Base compartida
    │   │   ├── OracleConnector.java      ← Oracle DB
    │   │   ├── PostgreSQLConnector.java  ← PostgreSQL
    │   │   ├── MySQLConnector.java       ← MySQL
    │   │   └── SQLServerConnector.java   ← SQL Server
    │   ├── factory/
    │   │   └── DbConnectorFactory.java   ← Crea y cachea conectores
    │   ├── helpers/
    │   │   └── DatabaseHelper.java       ← Ejecuta queries y valida resultados
    │   ├── interfaces/
    │   │   └── DatabaseConnector.java    ← Interfaz de conector genérico
    │   ├── config/
    │   │   └── DatabaseConfig.java       ← Configuración HikariCP
    │   ├── repository/
    │   │   └── QueryRepository.java      ← Ejecuta queries genéricos (sin steps)
    │   └── steps/
    │       └── DatabaseConnectionSteps.java ← Steps BDD para BD
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

## 12. Cómo usar Common en otro módulo

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

## 13. Dependencias

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

> 📖 **Documentación relacionada:**
> - [api-core/README.md](../api-core/README.md) — Capa de pruebas de API
> - [web-core/README.md](../web-core/README.md) — Capa de pruebas Web
> - [mobile-core/README.md](../mobile-core/README.md) — Capa de pruebas Mobile
> - [README.md](../README.md) — Visión general del framework
