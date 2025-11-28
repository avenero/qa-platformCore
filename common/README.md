# 🔧 Common - Capa Base del Framework Scotia QA

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Gradle](https://img.shields.io/badge/Gradle-8.14-blue.svg)](https://gradle.org/)
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/scotia-qa/qa-scotia-frameworks)

> La capa fundacional del QA Scotia Automation Framework. Proporciona componentes, interfaces y utilidades compartidas por todas las capas especializadas (API, Web, Mobile).

---

## 📑 Índice General

### Parte I: Visión General
- [🎯 ¿Qué es Common?](#-qué-es-common)
- [🏗️ Arquitectura de Common](#️-arquitectura-de-common)
- [📦 Estructura de Paquetes](#-estructura-de-paquetes)

### Parte II: Componentes Principales
- [📊 Sistema de Logging](#-sistema-de-logging)
- [🔗 ScenarioContext](#-scenariocontext)
- [🌐 HTTP Client Base](#-http-client-base)
- [🥒 Cucumber Hooks](#-cucumber-hooks)
- [🔒 Seguridad y Sanitización](#-seguridad-y-sanitización)
- [⚙️ Gestión de Configuraciones](#️-gestión-de-configuraciones)
- [💾 Conexión a Base de Datos](#-conexión-a-base-de-datos)
- [🔍 Test Data Finder](#-test-data-finder)

### Parte III: Guías de Uso
- [🏷️ Guía de Tags para Hooks](#️-guía-de-tags-para-hooks)
- [🔄 Ejemplo de Flujo Completo](#-ejemplo-de-flujo-completo)
- [💡 Ejemplos Prácticos](#-ejemplos-prácticos)
- [⚠️ Troubleshooting](#️-troubleshooting)

---

## 🎯 ¿Qué es Common?

### Definición

**Common** es la **capa base y fundacional** del framework Scotia QA. No es un framework completo por sí solo, sino el **núcleo genérico y reutilizable** que proporciona:

| Componente | Propósito |
|------------|-----------|
| 🏗️ **Arquitectura Base** | Interfaces y contratos genéricos |
| 🔌 **HTTP Client** | Cliente HTTP genérico (Unirest) con SSL configurado |
| 📊 **Logging** | Sistema de logging estructurado y contextual con MDC |
| 🔗 **ScenarioContext** | Compartir datos entre capas (API ↔ Web ↔ Mobile) |
| 🥒 **Cucumber Hooks** | Hooks base con inicialización condicional por tags |
| 🔒 **Seguridad** | Sanitización, encriptación, manejo seguro de credenciales |
| ⚙️ **Configuración** | ConfigManager para variables de entorno y properties |
| 💾 **Database** | Conexión a BD con pool HikariCP |
| 🔍 **Test Data** | UserFinderService para búsqueda de datos de prueba |
| 🛠️ **Utilidades** | JSON parsing, data handling, validaciones |

### ¿Para Qué NO es Common?

- ❌ **NO contiene** lógica de negocio específica
- ❌ **NO conoce** nada sobre APIs REST, WebDriver o Appium
- ❌ **NO tiene** steps de Cucumber específicos de negocio
- ❌ **NO depende** de frameworks especializados (Selenium, Appium, RestAssured)

### Principio de Diseño: Module-First

Common sigue el principio **"Module-First"**:
- Los **módulos de negocio** definen sus propios localizadores/endpoints
- **Common** solo provee herramientas genéricas y reutilizables
- Sin acoplamiento a proyectos específicos

---

## 🏗️ Arquitectura de Common

### Diagrama de Capas del Framework

```
┌─────────────────────────────────────────────────────────────┐
│                    MÓDULOS CONSUMIDORES                     │
│          qa-banking • qa-autos • qa-logistics               │
│          (Repositorios independientes)                      │
└─────────────────────────────────────────────────────────────┘
                            ↓ importan como librería
┌─────────────────────────────────────────────────────────────┐
│              FRAMEWORKS ESPECIALIZADOS (CORE)               │
│        api-core  •  web-core  •  mobile-core               │
│     (Selenium, Appium, RestAssured específicos)             │
└─────────────────────────────────────────────────────────────┘
                            ↓ extienden y dependen de
┌─────────────────────────────────────────────────────────────┐
│                      COMMON (BASE)                          │
│  ┌──────────────┬──────────────┬──────────────────────┐    │
│  │  Interfaces  │   Logging    │   ScenarioContext    │    │
│  │  HTTP Base   │   Security   │   Data Utilities     │    │
│  │  Cucumber    │   Factories  │   Configuration      │    │
│  │  Database    │   TestData   │   Utilities          │    │
│  └──────────────┴──────────────┴──────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Relación con Otras Capas

```
common/
  ↓ es base de
  ├─→ api-core (agrega: RestAssured, HTTP específico)
  ├─→ web-core (agrega: Selenium, WebDriver, Page Objects)
  └─→ mobile-core (agrega: Appium, Gestures móviles)
```

---

## 📦 Estructura de Paquetes

```
common/
├── src/main/java/com/scotia/qa/common/
│   │
│   ├── config/                    # 📁 Gestión de Configuraciones
│   │   ├── ConfigManager.java           # Singleton para configuración
│   │   └── providers/
│   │       ├── ConfigurationProvider.java      # Interface
│   │       └── BaseConfigurationProvider.java  # Implementación base
│   │
│   ├── cucumber/                  # 📁 Cucumber & Contexto
│   │   ├── context/
│   │   │   ├── ScenarioContext.java     # Compartir datos entre steps
│   │   │   └── ScenarioContextHooks.java # Hooks de contexto
│   │   ├── hooks/
│   │   │   ├── ConditionalHookDefinition.java  # Interface hooks
│   │   │   ├── ApiHooks.java            # Hooks @api
│   │   │   ├── WebHooks.java            # Hooks @web
│   │   │   ├── MobileHooks.java         # Hooks @mobile
│   │   │   └── DatabaseHooks.java       # Hooks @database (lazy)
│   │   └── tags/
│   │       └── TagDetector.java         # Detecta tags del escenario
│   │
│   ├── database/                  # 📁 Conexión a Base de Datos
│   │   ├── config/
│   │   │   └── DatabaseConfig.java      # Config HikariCP
│   │   ├── connectors/
│   │   │   ├── OracleConnector.java     # Conector Oracle
│   │   │   ├── PostgresConnector.java   # Conector PostgreSQL
│   │   │   ├── MySQLConnector.java      # Conector MySQL
│   │   │   └── SQLServerConnector.java  # Conector SQL Server
│   │   ├── factory/
│   │   │   └── DbConnectorFactory.java  # Factory para crear conectores
│   │   ├── interfaces/
│   │   │   └── DatabaseConnector.java   # Interface base
│   │   └── repository/
│   │       └── QueryRepository.java     # Ejecución de queries genéricas
│   │
│   ├── http/                      # 📁 Cliente HTTP Base
│   │   ├── BaseHttpClient.java          # Cliente Unirest genérico
│   │   ├── HttpResponse.java            # Wrapper de respuestas
│   │   └── ssl/
│   │       └── SSLUtils.java            # Utilidades SSL para testing
│   │
│   ├── logging/                   # 📁 Sistema de Logging
│   │   ├── LoggingInitializer.java      # Inicializa MDC por módulo
│   │   ├── TestLogger.java              # Logger estructurado
│   │   └── LogMaskingConverter.java     # Enmascara credenciales en logs
│   │
│   ├── security/                  # 📁 Seguridad
│   │   ├── DataSanitizer.java           # Sanitiza datos sensibles
│   │   └── EncryptionUtils.java         # Encriptación básica
│   │
│   └── utils/                     # 📁 Utilidades
│       ├── json/
│       │   └── JsonUtils.java           # Parsing JSON
│       ├── testdata/
│       │   ├── model/
│       │   │   └── TestUser.java        # Modelo de usuario de prueba
│       │   ├── repository/
│       │   │   └── TestDataRepository.java  # Repo de test data
│       │   ├── service/
│       │   │   └── UserFinderService.java   # Buscar usuarios de prueba
│       │   └── steps/
│       │       └── UserFinderSteps.java     # Steps Cucumber para test data
│       └── DataUtilities.java           # Utilidades de datos
│
└── src/main/resources/
    ├── logback.xml                      # Configuración logging
    └── templates/
        ├── config-scotia.properties.template  # Template configuración
        └── README.md                    # Guía de templates
```

---

## 📊 Sistema de Logging

### Características

- ✅ **Logging estructurado** con MDC (Mapped Diagnostic Context)
- ✅ **Por módulo y escenario**: Logs identifican módulo y test actual
- ✅ **Enmascaramiento automático** de credenciales (passwords, tokens)
- ✅ **Formato unificado** en todas las capas
- ✅ **Niveles configurables** por ambiente

### Componentes Clave

#### 1. LoggingInitializer

```java
// Inicializa contexto por módulo
LoggingInitializer.initModuleContext("BANKING");

// Establece contexto del test actual
LoggingInitializer.setTestContext("Login exitoso");

// Limpia contexto al finalizar
LoggingInitializer.clearTestContext();
```

#### 2. TestLogger

```java
// Logging estructurado con contexto
TestLogger.logInfo("USER_LOGIN", "Usuario autenticado", 
    Map.of("userId", "12345", "timestamp", System.currentTimeMillis()));

TestLogger.logError("API_ERROR", "Fallo en llamada", 
    Map.of("endpoint", "/api/users", "status", 500));

TestLogger.logSuccess("TEST_PASSED", "Test completado", null);
```

#### 3. LogMaskingConverter

Enmascara automáticamente:
- Passwords
- Tokens
- API Keys
- Números de tarjeta
- SSNs

**Ejemplo:**

```
ANTES: {"password": "MySecret123"}
DESPUÉS: {"password": "***MASKED***"}
```

### Formato de Log

```
12:30:45.123 INFO [BANKING] [Login exitoso] c.s.q.c.logging.TestLogger - [BANKING][Login exitoso][USER_LOGIN] Usuario autenticado
Context: {userId=12345, timestamp=1701181845000}
```

---

## 🔗 ScenarioContext

### ¿Qué es?

**ScenarioContext** permite **compartir datos entre diferentes steps y capas** dentro de un mismo escenario de Cucumber.

### Casos de Uso

#### 1. Compartir entre Steps del Mismo Tipo

```java
// En LoginSteps.java
@When("me autentico con credenciales válidas")
public void autenticar() {
    String token = authService.login("user", "pass");
    ScenarioContext.set("authToken", token);  // ← Guardar
}

// En TransferSteps.java
@When("realizo una transferencia")
public void transferir() {
    String token = ScenarioContext.get("authToken");  // ← Recuperar
    transferService.transfer(token, amount);
}
```

#### 2. Compartir entre API y Web (Híbrido)

```gherkin
@api @web @e2e
Scenario: Crear usuario en API y verificar en Web
  Given creo usuario via API                    # ← API guarda userId
  When navego al perfil del usuario creado      # ← Web usa userId
  Then veo los datos correctos                  # ← Web valida
```

```java
// ApiSteps.java
@Given("creo usuario via API")
public void crearUsuario() {
    String userId = apiClient.post("/users", userData).get("id");
    ScenarioContext.set("userId", userId);  // ← Compartir a Web
}

// WebSteps.java
@When("navego al perfil del usuario creado")
public void navegarPerfil() {
    String userId = ScenarioContext.get("userId");  // ← Recibir de API
    driver.get("https://app.com/profile/" + userId);
}
```

#### 3. Compartir Test Data

```java
// UserFinderSteps.java (@database)
@Given("usuario con cuenta activa")
public void usuarioConCuentaActiva() {
    TestUser user = userFinder.findUserWith("cuenta-activa");
    ScenarioContext.set("testUser", user);  // ← Guardar usuario
}

// LoginSteps.java (@web)
@When("me autentico con el usuario de prueba")
public void autenticar() {
    TestUser user = ScenarioContext.get("testUser");  // ← Usar usuario
    loginPage.login(user.getUsername(), user.getPassword());
}
```

### Métodos Principales

```java
// Guardar dato
ScenarioContext.set("key", value);

// Recuperar dato
String value = ScenarioContext.get("key");

// Verificar existencia
boolean exists = ScenarioContext.has("key");

// Remover dato
ScenarioContext.remove("key");

// Limpiar todo (se hace automáticamente después de cada escenario)
ScenarioContext.clear();
```

### Gestión Automática

**ScenarioContextHooks** limpia automáticamente el contexto:

```java
@After(order = 999)  // Se ejecuta al final
public void cleanupContext(Scenario scenario) {
    ScenarioContext.clear();  // ← Limpieza automática
}
```

---

## 🌐 HTTP Client Base

### BaseHttpClient

Cliente HTTP genérico basado en **Unirest** con configuración para testing.

### Características

- ✅ SSL deshabilitado para entornos de prueba
- ✅ Timeouts configurables
- ✅ Headers predefinidos
- ✅ Logging automático de requests/responses
- ✅ Manejo de errores centralizado

### Uso Básico

```java
BaseHttpClient client = new BaseHttpClient();

// GET
HttpResponse<String> response = client.get("https://api.test.com/users");

// POST
String body = "{\"name\": \"John\"}";
HttpResponse<String> response = client.post("https://api.test.com/users", body);

// Headers personalizados
client.addHeader("Authorization", "Bearer token123");
client.get("https://api.test.com/protected");
```

### Configuración SSL

```java
// SSL ya viene deshabilitado para testing
// Si necesitas habilitarlo:
SSLUtils.enableSSLVerification();
```

---

## 🥒 Cucumber Hooks

### Sistema de Hooks Condicionales

Common implementa **inicialización condicional** basada en **tags de Cucumber**.

### Hooks Disponibles

| Hook | Tag Requerido | Qué Inicializa |
|------|---------------|----------------|
| `ApiHooks` | `@api`, `@rest`, `@http` | HttpClient (lazy) |
| `WebHooks` | `@web`, `@ui`, `@selenium` | WebDriver |
| `MobileHooks` | `@mobile`, `@android`, `@ios` | AppiumDriver |
| `DatabaseHooks` | `@database`, `@db`, `@sql` | DatabaseConnector (lazy) |

### Orden de Ejecución

```
@Before (order = 10) → Detectar tags
@Before (order = 20) → Inicializar componentes según tags
@After (order = 999) → Limpiar ScenarioContext
@After (order = 1000) → Cerrar conexiones
```

### Ejemplo de Uso

```gherkin
@web @api
Scenario: Flujo híbrido Web + API
  Given navego a la página de login          # ← WebDriver activo
  When me autentico
  Then valido en API que la sesión existe    # ← HttpClient activo
```

**Resultado:**
- ✅ WebDriver se inicializa (por `@web`)
- ✅ HttpClient se inicializa (por `@api`)
- ❌ AppiumDriver NO se inicializa (sin `@mobile`)
- ❌ Database NO se inicializa (sin `@database`)

---

## 🏷️ Guía de Tags para Hooks

### Tags Soportados por Capa

#### 🌐 Web Testing

**Tags:** `@web`, `@ui`, `@selenium`, `@browser`

**Inicializa:**
- WebDriver (Selenium)
- Navegador configurado
- Page Objects
- Screenshots automáticos

**Ejemplo:**
```gherkin
@web @smoke
Scenario: Login exitoso
  Given navego a "https://qa.banking.com/login"
  When ingreso credenciales válidas
  Then veo el dashboard
```

#### 🔌 API Testing

**Tags:** `@api`, `@rest`, `@http`, `@service`

**Inicializa:**
- HttpClient (lazy)
- Gestión de headers
- SSL configurado para testing

**Ejemplo:**
```gherkin
@api @integration
Scenario: Consulta de usuarios
  Given configuro el endpoint "/users"
  When ejecuto GET request
  Then el código de respuesta es 200
```

#### 📱 Mobile Testing

**Tags:** `@mobile`, `@android`, `@ios`, `@appium`

**Inicializa:**
- AppiumDriver
- Gestures móviles
- Context switching (NATIVE/WEBVIEW)

**Ejemplo:**
```gherkin
@mobile @smoke
Scenario: Login en app móvil
  Given abro la aplicación móvil
  When ingreso credenciales
  Then veo el home
```

#### 💾 Database Testing

**Tags:** `@database`, `@db`, `@sql`

**NO inicializa automáticamente** - La conexión se crea lazy al usar `UserFinderService`.

**Ejemplo:**
```gherkin
@database @testdata
Scenario: Buscar usuario con cuenta activa
  Given usuario con cuenta activa
  Then valido que tiene saldo disponible
```

### Combinaciones (Tests Híbridos)

#### Web + API

```gherkin
@web @api @e2e
Scenario: Transferencia con validación backend
  Given navego a la página de transferencias
  When realizo transferencia de $100
  Then veo confirmación en pantalla
  And valido en API que el movimiento se registró
```

#### Mobile + API

```gherkin
@mobile @api @integration
Scenario: Pago en app con validación
  Given abro la app de pagos
  When realizo un pago
  Then veo confirmación
  And valido en API el débito
```

#### Web + API + Database

```gherkin
@web @api @database @e2e
Scenario: Flujo completo de registro
  Given usuario nuevo desde base de datos
  When registro usuario en web
  Then valido creación en API
  And valido registro en BD
```

### ⚠️ Reglas Importantes

#### 1. Siempre Agregar el Tag Correcto

❌ **MAL:**
```gherkin
Scenario: Login en web
  Given navego a "https://..."
```
**Problema:** Sin tag `@web`, WebDriver NO se inicializa → Test falla

✅ **BIEN:**
```gherkin
@web
Scenario: Login en web
  Given navego a "https://..."
```

#### 2. Usar Múltiples Tags para Tests Híbridos

```gherkin
@web @api  # ← Ambos tags necesarios
Scenario: Test que usa Web y API
```

#### 3. Tags Específicos de Móvil

```gherkin
@mobile @android  # Para Android
@mobile @ios      # Para iOS
```

---

## 🔒 Seguridad y Sanitización

### DataSanitizer

Elimina o enmascara datos sensibles antes de logging.

```java
// Sanitizar JSON con passwords
String json = "{\"user\":\"john\", \"password\":\"secret123\"}";
String sanitized = DataSanitizer.sanitizeJson(json);
// Resultado: {"user":"john", "password":"***MASKED***"}

// Sanitizar URLs con query params sensibles
String url = "https://api.com?token=abc123&key=secret";
String sanitized = DataSanitizer.sanitizeUrl(url);
// Resultado: https://api.com?token=***&key=***
```

### EncryptionUtils

Encriptación básica para almacenamiento temporal.

```java
// Encriptar
String encrypted = EncryptionUtils.encrypt("myPassword");

// Desencriptar
String decrypted = EncryptionUtils.decrypt(encrypted);
```

---

## ⚙️ Gestión de Configuraciones

### ConfigManager

Singleton para gestionar configuraciones desde múltiples fuentes.

### Prioridad de Carga

```
1. System Properties (-Dkey=value)
2. Environment Variables (export KEY=value)
3. Archivos .properties (config-qa.properties)
4. Valores por defecto
```

### Uso

```java
// Obtener instancia
ConfigManager config = ConfigManager.getInstance();

// Leer valores
String dbUrl = config.get("db.url");
String timeout = config.get("api.timeout", "30000");  // Con default

// Verificar existencia
if (config.has("db.url")) {
    // ...
}
```

### Archivo de Configuración

**`config-scotia.properties`:**

```properties
# Base de Datos
db.url=jdbc:oracle:thin:@//qa-db:1521/XE
db.username=${DB_USER}
db.password=${DB_PASS}
db.driver=oracle.jdbc.OracleDriver

# API
api.base.url=https://api-qa.scotia.com
api.timeout=30000

# Web
web.base.url=https://qa.scotia.com
web.browser=chrome
web.headless=true
```

### Variables de Entorno

**`.env.local`:**

```bash
# Base de Datos
DB_USER=test_user
DB_PASS=test_password

# API
API_TOKEN=your_token_here
```

---

## 💾 Conexión a Base de Datos

### Componentes

#### 1. DbConnectorFactory

Factory para crear conectores de BD.

```java
// Crear conector desde ConfigManager
DatabaseConnector connector = DbConnectorFactory.createFromConfig();

// Crear conector con parámetros directos
DatabaseConnector connector = DbConnectorFactory.create(
    "jdbc:oracle:thin:@//localhost:1521/XE",
    "user",
    "password",
    "oracle.jdbc.OracleDriver"
);
```

#### 2. Conectores Específicos

- `OracleConnector` - Oracle DB
- `PostgresConnector` - PostgreSQL
- `MySQLConnector` - MySQL
- `SQLServerConnector` - SQL Server

#### 3. QueryRepository

Ejecuta queries genéricas.

```java
QueryRepository repo = new QueryRepository(connector);

// Query que retorna mapa
Map<String, Object> result = repo.queryForMap("SELECT * FROM users WHERE id = ?", userId);

// Query que retorna lista
List<Map<String, Object>> results = repo.queryForList("SELECT * FROM users WHERE status = ?", "ACTIVE");
```

### Pool de Conexiones

Usa **HikariCP** para pool de conexiones:

```properties
db.pool.size.min=2
db.pool.size.max=10
db.pool.connectionTimeout=30000
```

---

## 🔍 Test Data Finder

### ¿Qué es?

Sistema para **buscar usuarios de prueba** en base de datos según características específicas.

### Componentes

#### 1. UserFinderService

```java
// Inicializar (lee configuración automáticamente)
UserFinderService userFinder = new UserFinderService("test-data-queries.yml");

// Buscar usuario con característica
TestUser user = userFinder.findUserWith("cuenta-activa");

// Usar usuario
System.out.println(user.getUserId());
System.out.println(user.getUsername());
System.out.println(user.getPassword());
```

#### 2. test-data-queries.yml

Define queries para diferentes tipos de usuarios.

```yaml
queries:
  cuenta-activa:
    sql: |
      SELECT user_id, username, password, email, phone
      FROM test_users u
      INNER JOIN accounts a ON u.user_id = a.user_id
      WHERE a.status = 'ACTIVE' 
        AND a.balance > 0
        AND u.reserved_by IS NULL
      LIMIT 1
    description: "Usuario con cuenta activa"
  
  tarjeta-credito:
    sql: |
      SELECT user_id, username, password, email, phone
      FROM test_users u
      INNER JOIN credit_cards cc ON u.user_id = cc.user_id
      WHERE cc.status = 'ACTIVE'
        AND u.reserved_by IS NULL
      LIMIT 1
    description: "Usuario con tarjeta de crédito"
```

#### 3. TestUser Model

```java
public class TestUser {
    private String userId;
    private String username;
    private String password;
    private String email;
    private String phone;
    private Map<String, Object> additionalData;
    
    // Getters...
}
```

#### 4. UserFinderSteps (Cucumber)

Steps predefinidos para usar en features.

```gherkin
@database
Scenario: Login con usuario de prueba
  Given usuario con "cuenta-activa"            # ← Step del framework
  When me autentico con el usuario de prueba   # ← Usa ScenarioContext
  Then veo el dashboard
```

```java
// El step guarda el usuario en ScenarioContext automáticamente
@Given("usuario con {string}")
public void usuarioConCaracteristica(String caracteristica) {
    TestUser user = userFinder.findUserWith(caracteristica);
    ScenarioContext.set("testUser", user);
}
```

### Implementación en Módulos

#### PASO 1: Agregar Dependencias

```groovy
dependencies {
    implementation 'com.scotia.qa:common:1.0.0'
}
```

#### PASO 2: Crear test-data-queries.yml

Ubicación: `src/test/resources/test-data-queries.yml`

#### PASO 3: Configurar BD

```properties
# config-scotia.properties
db.url=jdbc:oracle:thin:@//testdb:1521/TESTDB
db.username=${DB_USER}
db.password=${DB_PASS}
```

#### PASO 4: Usar en Steps

```java
@Given("usuario con cuenta activa")
public void usuarioConCuentaActiva() {
    TestUser user = userFinder.findUserWith("cuenta-activa");
    ScenarioContext.set("testUser", user);
}
```

---

## 🔄 Ejemplo de Flujo Completo

### Arquitectura del Flujo

```
┌──────────────────────────────────────────────────────────────┐
│  MÓDULO qa-banking (repositorio independiente)              │
└──────────────────────────────────────────────────────────────┘
         │
         │ 1. Lee configuraciones
         ↓
┌──────────────────────────────────────────────────────────────┐
│  ConfigManager (common/config/)                              │
│  └─→ config-qa.properties                                    │
│      ├─ db.url=jdbc:oracle:thin:@//qa-db:1521/XE           │
│      ├─ db.username=${DB_USER}                              │
│      └─ db.password=${DB_PASS}                              │
└──────────────────────────────────────────────────────────────┘
         │
         │ 2. Pasa config a Database
         ↓
┌──────────────────────────────────────────────────────────────┐
│  DbConnectorFactory (common/database/factory/)               │
│  └─→ OracleConnector                                         │
│      └─→ HikariDataSource                                    │
│          └─→ Connection a Oracle DB                          │
└──────────────────────────────────────────────────────────────┘
         │
         │ 3. Connection disponible
         ↓
┌──────────────────────────────────────────────────────────────┐
│  UserFinderService (common/utils/testdata/)                  │
│  └─→ Carga test-data-queries.yml                            │
│      └─→ Ejecuta query con QueryRepository                  │
│          └─→ Retorna TestUser                                │
└──────────────────────────────────────────────────────────────┘
         │
         │ 4. TestUser listo
         ↓
┌──────────────────────────────────────────────────────────────┐
│  LoginSteps (módulo qa-banking)                              │
│  └─→ Usa TestUser en escenarios Cucumber                    │
└──────────────────────────────────────────────────────────────┘
```

### Feature de Ejemplo

```gherkin
@web @api @database @e2e
Feature: Login y validación completa

  Scenario: Login con usuario de BD y validar en API
    # 1. Test Data Finder busca usuario
    Given usuario con "cuenta-activa"
    
    # 2. Web usa el usuario encontrado
    And navego a "https://qa.banking.com/login"
    When me autentico con el usuario de prueba
    Then veo el dashboard
    
    # 3. API valida la sesión
    And valido en API que la sesión está activa
```

### Steps Implementados

```java
// DatabaseSteps.java
@Given("usuario con {string}")
public void buscarUsuario(String caracteristica) {
    TestUser user = userFinder.findUserWith(caracteristica);
    ScenarioContext.set("testUser", user);
    TestLogger.logInfo("USER_FOUND", "Usuario encontrado", 
        Map.of("userId", user.getUserId()));
}

// WebSteps.java
@When("me autentico con el usuario de prueba")
public void autenticar() {
    TestUser user = ScenarioContext.get("testUser");
    loginPage.login(user.getUsername(), user.getPassword());
}

// ApiSteps.java
@Then("valido en API que la sesión está activa")
public void validarSesion() {
    TestUser user = ScenarioContext.get("testUser");
    Response response = apiClient.get("/sessions/" + user.getUserId());
    assertThat(response.jsonPath().getString("status")).isEqualTo("ACTIVE");
}
```

---

## 💡 Ejemplos Prácticos

### Ejemplo 1: Test Híbrido Web + API

```gherkin
@web @api
Feature: Transferencias

  Scenario: Transferencia con validación backend
    Given usuario con "cuenta-con-saldo"
    And navego a transferencias
    When transfiero $100 a cuenta "123456"
    Then veo mensaje de confirmación
    And valido en API que se registró el movimiento
```

### Ejemplo 2: Test con Test Data

```gherkin
@database @web
Feature: Productos

  Scenario: Solicitar tarjeta
    Given usuario sin "tarjeta-credito"
    And navego a solicitudes
    When solicito tarjeta de crédito
    Then veo confirmación de solicitud
```

### Ejemplo 3: Test E2E Completo

```gherkin
@web @api @database @e2e
Feature: Registro completo

  Scenario: Registro de usuario nuevo
    Given usuario nuevo desde base de datos
    When completo formulario de registro en web
    Then veo bienvenida
    And valido en API que el usuario existe
    And valido en BD que se crearon sus productos por defecto
```

---

## ⚠️ Troubleshooting

### Error: "WebDriver no inicializado"

**Causa:** Falta tag `@web` en el escenario.

**Solución:**
```gherkin
@web  # ← Agregar tag
Scenario: Test web
```

### Error: "No se encontró config-scotia.properties"

**Causa:** Archivo no está en `src/test/resources/`

**Solución:**
```bash
# Copiar template
cp common/src/main/resources/templates/config-scotia.properties.template \
   src/test/resources/config-scotia.properties
```

### Error: "Database connection failed"

**Causa:** Variables de entorno no cargadas.

**Solución:**
```bash
# Cargar .env.local
source .env.local
./gradlew test
```

### Error: "TestUser es null"

**Causa:** Query no retornó resultados.

**Solución:**
- Verificar que hay datos de prueba en BD
- Revisar query en `test-data-queries.yml`
- Verificar conexión a BD

### Error: "ScenarioContext.get() retorna null"

**Causa:** Dato no fue guardado antes con `set()`.

**Solución:**
```java
// Siempre verificar antes de usar
if (ScenarioContext.has("key")) {
    String value = ScenarioContext.get("key");
} else {
    throw new RuntimeException("Dato no disponible en contexto");
}
```

---

## 📚 Recursos Adicionales

### Documentación Relacionada

- **Framework General:** `/documentacion/FRAMEWORK-GUIDE.md`
- **Quick Start:** `/documentacion/QUICK-START.md`
- **API Core:** `/api-core/README.md`
- **Web Core:** `/web-core/README.md`
- **Mobile Core:** `/mobile-core/README.md`
- **Scripts:** `/scripts/README.md`

### Dependencias Principales

| Dependencia | Versión | Propósito |
|-------------|---------|-----------|
| Java | 21 | Lenguaje base |
| Cucumber | 7.18.0 | BDD Framework |
| SLF4J/Logback | 2.0.x | Logging |
| Unirest | 3.14.5 | HTTP Client |
| HikariCP | 5.1.0 | Connection Pool |
| Jackson | 2.15.x | JSON parsing |

---

**Versión:** 1.0.0  
**Fecha:** 28 de Noviembre de 2025  
**Autor:** Abel Venero  
**Framework:** Scotia QA Framework

