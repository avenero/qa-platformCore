# 🔧 Common - Capa Base del Framework

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Gradle](https://img.shields.io/badge/Gradle-8.14-blue.svg)](https://gradle.org/)
[![Version](https://img.shields.io/badge/version-1.0.2-blue.svg)](https://github.com/scotia-qa/qa-scotia-frameworks)

> La capa fundacional del QA Scotia Automation Framework. Proporciona componentes, interfaces y utilidades compartidas por todas las capas especializadas (API, Web, Mobile).

---

## 📑 Índice

- [🎯 Visión General](#-visión-general)
- [🏗️ Arquitectura de Common](#️-arquitectura-de-common)
- [📦 Estructura de Paquetes](#-estructura-de-paquetes)
- [🔍 Detalle de Componentes](#-detalle-de-componentes)
  - [Logging System](#logging-system)
  - [ScenarioContext](#scenariocontext)
  - [HTTP Client Base](#http-client-base)
  - [Cucumber Hooks](#cucumber-hooks)
  - [Security & Sanitization](#security--sanitization)
  - [Data Utilities](#data-utilities)
- [🚀 Cómo Usar Common](#-cómo-usar-common)
- [💡 Ejemplos Prácticos](#-ejemplos-prácticos)
- [⚠️ Troubleshooting](#️-troubleshooting)
- [📚 API Reference](#-api-reference)

---

## 🎯 Visión General

### ¿Qué es Common?

**Common** es la **capa base y fundacional** del framework. No es un framework completo por sí solo, sino el **núcleo genérico y reutilizable** que proporciona:

- 🏗️ **Arquitectura base** mediante interfaces y contratos
- 🔌 **HTTP Client genérico** (Unirest)
- 📊 **Sistema de logging** estructurado y contextual
- 🔗 **ScenarioContext** para compartir datos entre capas
- 🥒 **Cucumber Hooks** base
- 🔒 **Utilidades de seguridad** (sanitización, encriptación)
- 🛠️ **Utilidades generales** (JSON, data handling, etc.)

### ¿Para Qué NO es Common?

- ❌ **NO contiene** lógica de negocio específica
- ❌ **NO conoce** nada sobre APIs REST, WebDriver o Appium
- ❌ **NO tiene** steps de Cucumber específicos
- ❌ **NO depende** de ningún framework especializado

### Principio de Diseño: Module-First

Common sigue el principio **"Module-First"**:
- Los **módulos de negocio** definen sus propios localizadores/endpoints
- **Common** solo provee herramientas genéricas
- Sin acoplamiento a proyectos específicos

---

## 🏗️ Arquitectura de Common

### Diagrama de Arquitectura General

```
┌─────────────────────────────────────────────────────────────┐
│                    MÓDULOS CONSUMIDORES                     │
│          qa-banking • qa-autos • qa-logistics               │
└─────────────────────────────────────────────────────────────┘
                            ↓ usan
┌─────────────────────────────────────────────────────────────┐
│              FRAMEWORKS ESPECIALIZADOS (CORE)               │
│        api-core  •  web-core  •  mobile-core               │
└─────────────────────────────────────────────────────────────┘
                            ↓ extienden
┌─────────────────────────────────────────────────────────────┐
│                      COMMON (BASE)                          │
│  ┌──────────────┬──────────────┬──────────────────────┐    │
│  │  Interfaces  │   Logging    │   ScenarioContext    │    │
│  │  HTTP Base   │   Security   │   Data Utilities     │    │
│  │  Cucumber    │   Factories  │   Configuration      │    │
│  └──────────────┴──────────────┴──────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Diagrama de Paquetes

```
common/
├── cucumber/              # Hooks y contexto de Cucumber
│   ├── ScenarioContext
│   └── ScenarioContextHooks
│
├── logging/               # Sistema de logging estructurado
│   ├── TestLogger
│   ├── LoggingInitializer
│   └── LogMaskingConverter
│
├── http/                  # Cliente HTTP base
│   ├── BaseHttpClient
│   └── HttpResponse
│
├── security/              # Seguridad y sanitización
│   ├── DataSanitizer
│   └── EncryptionUtil
│
├── factories/             # Factories genéricos
│   └── BaseFactory
│
└── utils/                 # Utilidades generales
    ├── DataUtilities
    ├── DateUtils
    └── JsonUtils
```

---

## 📦 Estructura de Paquetes

### Vista Detallada

| Paquete | Propósito | Clases Principales |
|---------|-----------|-------------------|
| **`cucumber/`** | Gestión de contexto BDD | `ScenarioContext`, `ScenarioContextHooks` |
| **`logging/`** | Logging estructurado | `TestLogger`, `LoggingInitializer` |
| **`http/`** | Cliente HTTP base | `BaseHttpClient`, `HttpResponse` |
| **`security/`** | Sanitización y seguridad | `DataSanitizer`, `EncryptionUtil` |
| **`factories/`** | Factories genéricos | `BaseFactory` |
| **`utils/`** | Utilidades transversales | `DataUtilities`, `JsonUtils` |

---

## 🔍 Detalle de Componentes

### Logging System

#### 📊 Arquitectura del Sistema de Logging

El sistema de logging usa **SLF4J + Logback** con **MDC (Mapped Diagnostic Context)** para logs contextuales.

```
┌─────────────────────────────────────────────────────────┐
│                    TestLogger (API)                     │
│  logInfo() • logDebug() • logWarning() • logError()    │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│              LoggingInitializer (Context)               │
│  initModuleContext() • setTestContext() • clearMDC()   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                 SLF4J + Logback                         │
│         MDC Context (module, testName, etc.)           │
└─────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────┬──────────────────────────────────┐
│   Console (colors)   │   File (JSON/Plain)              │
│   DEBUG-ERROR        │   Rotación diaria                │
└──────────────────────┴──────────────────────────────────┘
```

#### 🎯 Características

- ✅ **Logs contextuales** con módulo y test name
- ✅ **Thread-safe** para ejecución paralela
- ✅ **Colores en consola** para mejor legibilidad
- ✅ **Archivos separados** por módulo (API, WEB, MOBILE)
- ✅ **Masking automático** de datos sensibles
- ✅ **Formato JSON** para integración con ELK/Splunk

#### 📝 Uso Básico

```java
import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.logging.LoggingInitializer;

// 1. Inicializar módulo (una vez por test suite)
LoggingInitializer.initModuleContext("API");

// 2. Establecer contexto de test (por cada test)
LoggingInitializer.setTestContext("Login Test");

// 3. Loguear
TestLogger.logInfo("AUTH", "Usuario autenticado exitosamente", null);

TestLogger.logDebug("DEBUG", "Token recibido: " + token, null);

TestLogger.logWarning("VALIDATION", "Campo email vacío", null);

TestLogger.logError("EXCEPTION", "Falló conexión a BD", exception);

// 4. Limpiar al final
LoggingInitializer.clearTestContext();
```

#### 🎨 Niveles de Logging

| Nivel | Cuándo Usar | Color en Consola |
|-------|-------------|------------------|
| **DEBUG** | Información detallada para debugging | 🔵 Azul |
| **INFO** | Eventos normales del flujo | ⚪ Blanco |
| **WARN** | Situaciones anómalas pero no críticas | 🟡 Amarillo |
| **ERROR** | Errores que requieren atención | 🔴 Rojo |

#### 🔒 Masking de Datos Sensibles

El sistema **automáticamente enmascara** datos sensibles en los logs:

```java
// ✅ ANTES DE LOGUEAR
String password = "MySecretPass123!";
String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

TestLogger.logInfo("AUTH", "Password: " + password, null);
TestLogger.logInfo("AUTH", "Token: " + token, null);

// ✅ EN EL LOG (MASKING AUTOMÁTICO)
// [AUTH] Password: ********
// [AUTH] Token: eyJhbG...***
```

**Palabras clave que activan masking:**
- `password`, `passwd`, `pwd`
- `token`, `bearer`
- `secret`, `api_key`
- `credit_card`, `cvv`

#### 📂 Estructura de Archivos de Log

```
logs/
├── api/
│   ├── api-2025-11-26.log      # Log diario
│   └── api-2025-11-26.json     # Formato JSON
├── web/
│   ├── web-2025-11-26.log
│   └── web-2025-11-26.json
└── mobile/
    ├── mobile-2025-11-26.log
    └── mobile-2025-11-26.json
```

#### 🔧 Configuración Personalizada

Puedes personalizar el logging editando `logback.xml`:

```xml
<configuration>
    <!-- Cambiar nivel global -->
    <root level="INFO">  <!-- DEBUG, INFO, WARN, ERROR -->
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
    
    <!-- Silenciar librerías ruidosas -->
    <logger name="org.apache.http" level="WARN"/>
    <logger name="io.restassured" level="ERROR"/>
</configuration>
```

---

### ScenarioContext

#### 🔗 ¿Qué es ScenarioContext?

**ScenarioContext** es un almacén de datos **thread-safe** que permite **compartir información entre capas** (API ↔ Web ↔ Mobile) dentro de un mismo escenario de Cucumber.

#### 🎯 Casos de Uso

1. **API obtiene token → Web lo usa para navegar**
2. **Web obtiene ID de cliente → API valida en backend**
3. **API crea registro → Mobile verifica en app**
4. **Compartir datos entre steps** del mismo escenario

#### 📊 Arquitectura

```
┌────────────────────────────────────────────────────────┐
│              ScenarioContext (Thread-Safe)             │
│  ┌──────────┬──────────┬──────────┬──────────────┐   │
│  │   API    │   WEB    │  MOBILE  │   COMMON     │   │
│  │  Layer   │  Layer   │  Layer   │   Layer      │   │
│  │          │          │          │              │   │
│  │  token   │  userId  │  orderId │  testData    │   │
│  │  apiUrl  │  element │  device  │  timestamp   │   │
│  └──────────┴──────────┴──────────┴──────────────┘   │
└────────────────────────────────────────────────────────┘
           ↑                                 ↑
           │                                 │
      Cucumber Hooks               Cucumber Steps
    (auto-cleanup)              (set/get/getAllData)
```

#### 📝 API del ScenarioContext

```java
// Guardar datos en capa específica
ScenarioContext.setInLayer("api", "authToken", token);
ScenarioContext.setInLayer("web", "username", "john.doe");

// Obtener datos de capa específica
String token = (String) ScenarioContext.getFromLayer("api", "authToken");

// Buscar en TODAS las capas (recomendado)
String username = (String) ScenarioContext.getFromAnyLayer("username");

// Obtener todos los datos de todas las capas
Map<String, Object> allData = ScenarioContext.getAllFromAllLayers();

// Limpiar una capa
ScenarioContext.clearLayer("api");

// Limpiar todo (automático en hooks)
ScenarioContext.clearAll();
```

#### 💡 Ejemplo Completo: Flujo API → Web

**Scenario:**
```gherkin
Scenario: Login con API y validar en Web
  # 1. API obtiene token
  Given ejecuto login en API con usuario "john.doe"
  And guardo el token en contexto como "authToken"
  
  # 2. Web usa el token
  When navego a dashboard usando token guardado
  Then verifico que el usuario logueado sea "john.doe"
```

**API Steps:**
```java
@Given("ejecuto login en API con usuario {string}")
public void ejecutoLoginAPI(String username) {
    Response response = apiClient.post("/auth/login", 
        Map.of("username", username, "password", "test123"));
    
    String token = response.jsonPath().getString("token");
    
    // Guardar en contexto para que Web lo use
    ScenarioContext.setInLayer("api", "authToken", token);
    ScenarioContext.setInLayer("api", "username", username);
    
    TestLogger.logInfo("API", "Token guardado en contexto", null);
}
```

**Web Steps:**
```java
@When("navego a dashboard usando token guardado")
public void navegoADashboard() {
    // Obtener token que guardó API
    String token = (String) ScenarioContext.getFromLayer("api", "authToken");
    
    driver.get("https://app.example.com/dashboard");
    
    // Inyectar token en localStorage
    ((JavascriptExecutor) driver).executeScript(
        "localStorage.setItem('authToken', '" + token + "');"
    );
    
    driver.navigate().refresh();
    TestLogger.logInfo("WEB", "Dashboard cargado con token de API", null);
}

@Then("verifico que el usuario logueado sea {string}")
public void verificoUsuario(String expectedUsername) {
    String actualUsername = driver.findElement(By.id("username")).getText();
    
    // También podemos obtener del contexto
    String usernameFromAPI = (String) ScenarioContext.getFromLayer("api", "username");
    
    assertThat(actualUsername).isEqualTo(expectedUsername);
    assertThat(actualUsername).isEqualTo(usernameFromAPI);
}
```

#### 🧹 Limpieza Automática

El framework limpia automáticamente el ScenarioContext usando hooks:

```java
@After
public void cleanupScenarioContext(Scenario scenario) {
    ScenarioContext.clearAll();
    TestLogger.logDebug("HOOKS", "ScenarioContext limpiado", null);
}
```

**No necesitas** limpiar manualmente, pero puedes hacerlo si quieres:

```java
// Limpiar solo API layer
ScenarioContext.clearLayer("api");

// Limpiar todo
ScenarioContext.clearAll();
```

---

### HTTP Client Base

#### 🌐 BaseHttpClient

Cliente HTTP genérico basado en **Unirest** para comunicación con servicios REST.

#### 🎯 Características

- ✅ **Unirest-powered** - Cliente HTTP robusto
- ✅ **SSL flexible** - Soporta certificados auto-firmados
- ✅ **Logging automático** de requests/responses
- ✅ **Headers management**
- ✅ **JSON/XML support**
- ✅ **Timeouts configurables**

#### 📝 Uso Básico

```java
import com.scotia.qa.common.http.BaseHttpClient;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;

public class MyApiClient extends BaseHttpClient {
    
    public MyApiClient() {
        super("https://api.example.com");
    }
    
    public HttpResponse<JsonNode> login(String user, String pass) {
        return post("/auth/login", 
            Map.of("username", user, "password", pass));
    }
    
    public HttpResponse<JsonNode> getUserData(String userId) {
        return get("/users/" + userId);
    }
}
```

#### 🔧 Métodos Disponibles

```java
// GET request
HttpResponse<JsonNode> response = get("/endpoint");

// POST request con body
HttpResponse<JsonNode> response = post("/endpoint", bodyMap);

// PUT request
HttpResponse<JsonNode> response = put("/endpoint", bodyMap);

// DELETE request
HttpResponse<JsonNode> response = delete("/endpoint");

// Con headers custom
addHeader("Authorization", "Bearer " + token);
HttpResponse<JsonNode> response = get("/protected");
```

---

### Cucumber Hooks

#### 🥒 ScenarioContextHooks

Hooks automáticos para gestión del ciclo de vida de tests.

```java
package com.scotia.qa.common.cucumber;

public class ScenarioContextHooks {
    
    @Before
    public void beforeScenario(Scenario scenario) {
        // Inicializar contexto de logging
        LoggingInitializer.setTestContext(scenario.getName());
        
        TestLogger.logInfo("SCENARIO_START", 
            "Iniciando escenario: " + scenario.getName(), 
            Map.of("tags", scenario.getSourceTagNames()));
    }
    
    @After
    public void afterScenario(Scenario scenario) {
        // Limpiar ScenarioContext
        ScenarioContext.clearAll();
        
        // Log final
        if (scenario.isFailed()) {
            TestLogger.logError("SCENARIO_FAILED", 
                "Escenario falló: " + scenario.getName(), null);
        } else {
            TestLogger.logInfo("SCENARIO_PASSED", 
                "Escenario exitoso: " + scenario.getName(), null);
        }
        
        // Limpiar contexto de logging
        LoggingInitializer.clearTestContext();
    }
}
```

---

### Security & Sanitization

#### 🔒 DataSanitizer

Utilidad para sanitizar y enmascarar datos sensibles.

```java
import com.scotia.qa.common.security.DataSanitizer;

// Enmascarar passwords
String sanitized = DataSanitizer.maskSensitiveData(
    "password=MySecret123&token=abc123"
);
// Resultado: "password=*******&token=***"

// Remover caracteres peligrosos (prevenir inyección)
String safe = DataSanitizer.removeDangerousChars("<script>alert(1)</script>");
// Resultado: "scriptalert1script"

// Validar formato de email
boolean isValid = DataSanitizer.isValidEmail("user@example.com");
```

---

### Data Utilities

#### 🛠️ DataUtilities

Utilidades para manejo de datos comunes.

```java
import com.scotia.qa.common.utils.DataUtilities;

// Generar timestamps
String timestamp = DataUtilities.generateTimestamp();
String date = DataUtilities.generateDate("yyyy-MM-dd");

// Manipular JSON
Map<String, Object> jsonMap = DataUtilities.parseJson(jsonString);
String jsonString = DataUtilities.toJson(map);

// Obtener valores de JSON usando JSON Path
String value = DataUtilities.getValueFromJson(json, "$.user.name");

// Generar datos aleatorios
String randomEmail = DataUtilities.generateRandomEmail();
String randomPhone = DataUtilities.generateRandomPhone();
int randomNumber = DataUtilities.generateRandomNumber(1, 100);
```

---

## 🚀 Cómo Usar Common

### Agregar Dependencia

En el `build.gradle` de tu módulo:

```groovy
dependencies {
    implementation 'com.scotia.qa:common:1.0.2'
}
```

### Setup Inicial

```java
import com.scotia.qa.common.logging.LoggingInitializer;
import com.scotia.qa.common.cucumber.ScenarioContext;

@BeforeAll
public static void setupFramework() {
    // Inicializar logging para tu módulo
    LoggingInitializer.initModuleContext("BANKING");
}

@Before
public void beforeEachTest(Scenario scenario) {
    // Contexto por test
    LoggingInitializer.setTestContext(scenario.getName());
}

@After
public void afterEachTest() {
    // Limpiar
    ScenarioContext.clearAll();
    LoggingInitializer.clearTestContext();
}
```

---

## 💡 Ejemplos Prácticos

### Ejemplo 1: Flujo API → Web Completo

```gherkin
Feature: Proceso de compra E-commerce

  Scenario: Crear orden en API y validar en Web
    # API: Crear orden
    Given creo una orden en API con producto "Laptop"
    And guardo el ID de orden como "orderId"
    And guardo el monto total como "totalAmount"
    
    # Web: Verificar orden
    When navego al portal de administración
    And busco la orden con ID guardado
    Then verifico que el monto sea el guardado
    And verifico que el estado sea "Pendiente"
```

**Implementación:**

```java
// API Steps
@Given("creo una orden en API con producto {string}")
public void creoOrdenAPI(String producto) {
    Map<String, Object> orderData = Map.of(
        "product", producto,
        "quantity", 1,
        "customer", "john.doe@example.com"
    );
    
    HttpResponse<JsonNode> response = apiClient.post("/orders", orderData);
    
    String orderId = response.getBody().getObject().getString("orderId");
    double totalAmount = response.getBody().getObject().getDouble("total");
    
    // Guardar en contexto para Web
    ScenarioContext.setInLayer("api", "orderId", orderId);
    ScenarioContext.setInLayer("api", "totalAmount", totalAmount);
    
    TestLogger.logInfo("API", "Orden creada: " + orderId, null);
}

// Web Steps
@When("busco la orden con ID guardado")
public void buscoOrden() {
    String orderId = (String) ScenarioContext.getFromLayer("api", "orderId");
    
    driver.get("https://admin.example.com/orders");
    driver.findElement(By.id("searchBox")).sendKeys(orderId);
    driver.findElement(By.id("searchButton")).click();
    
    TestLogger.logInfo("WEB", "Buscando orden: " + orderId, null);
}

@Then("verifico que el monto sea el guardado")
public void verificoMonto() {
    Double expectedAmount = (Double) ScenarioContext.getFromLayer("api", "totalAmount");
    
    String actualAmount = driver.findElement(By.id("orderTotal")).getText();
    assertThat(Double.parseDouble(actualAmount)).isEqualTo(expectedAmount);
}
```

### Ejemplo 2: Compartir Datos Entre Steps

```java
@Given("genero datos de usuario aleatorios")
public void generoDatosAleatorios() {
    Map<String, Object> userData = Map.of(
        "email", DataUtilities.generateRandomEmail(),
        "phone", DataUtilities.generateRandomPhone(),
        "timestamp", DataUtilities.generateTimestamp()
    );
    
    // Guardar en contexto
    userData.forEach((key, value) -> 
        ScenarioContext.setInLayer("common", key, value));
    
    TestLogger.logInfo("SETUP", "Datos aleatorios generados", userData);
}

@When("registro usuario con datos generados")
public void registroUsuario() {
    String email = (String) ScenarioContext.getFromLayer("common", "email");
    String phone = (String) ScenarioContext.getFromLayer("common", "phone");
    
    // Usar datos en el registro...
}
```

---

## ⚠️ Troubleshooting

### Problema: Logs no aparecen

**Solución:**
```java
// Verificar que inicializaste el módulo
LoggingInitializer.initModuleContext("TU_MODULO");

// Verificar nivel de logging en logback.xml
<root level="INFO">  <!-- Cambiar a DEBUG si necesitas más detalle -->
```

### Problema: Variables no se comparten entre capas

**Solución:**
```java
// ✅ CORRECTO - Usar getFromAnyLayer()
String value = (String) ScenarioContext.getFromAnyLayer("miVariable");

// ❌ INCORRECTO - Capa específica incorrecta
String value = (String) ScenarioContext.getFromLayer("web", "miVariable");
// Si la guardaste en "api", no la encontrará
```

### Problema: "Cannot resolve symbol 'ScenarioContext'"

**Solución:**
```groovy
// Verificar dependencia en build.gradle
dependencies {
    implementation 'com.scotia.qa:common:1.0.2'
}

// Reimportar en IDE
./gradlew clean build --refresh-dependencies
```

---

## 📚 API Reference

### TestLogger

| Método | Descripción | Ejemplo |
|--------|-------------|---------|
| `logDebug(tag, msg, context)` | Log nivel DEBUG | `TestLogger.logDebug("DEBUG", "Variable x = " + x, null)` |
| `logInfo(tag, msg, context)` | Log nivel INFO | `TestLogger.logInfo("AUTH", "Login exitoso", null)` |
| `logWarning(tag, msg, context)` | Log nivel WARN | `TestLogger.logWarning("VALIDATION", "Campo vacío", null)` |
| `logError(tag, msg, exception)` | Log nivel ERROR | `TestLogger.logError("EXCEPTION", "Error BD", e)` |

### ScenarioContext

| Método | Descripción | Retorno |
|--------|-------------|---------|
| `setInLayer(layer, key, value)` | Guardar en capa específica | `void` |
| `getFromLayer(layer, key)` | Obtener de capa específica | `Object` |
| `getFromAnyLayer(key)` | Buscar en todas las capas | `Object` |
| `getAllFromAllLayers()` | Obtener todos los datos | `Map<String, Object>` |
| `clearLayer(layer)` | Limpiar una capa | `void` |
| `clearAll()` | Limpiar todo | `void` |

### DataUtilities

| Método | Descripción | Retorno |
|--------|-------------|---------|
| `generateTimestamp()` | Timestamp actual | `String` |
| `generateDate(format)` | Fecha formateada | `String` |
| `generateRandomEmail()` | Email aleatorio | `String` |
| `generateRandomPhone()` | Teléfono aleatorio | `String` |
| `generateRandomNumber(min, max)` | Número aleatorio | `int` |
| `parseJson(jsonString)` | Parsear JSON a Map | `Map<String, Object>` |
| `toJson(map)` | Convertir Map a JSON | `String` |
| `getValueFromJson(json, path)` | Extraer valor con JSONPath | `String` |

---

## 📦 Dependencias Incluidas

Common incluye estas dependencias que están disponibles para todos los módulos:

| Librería | Versión | Propósito |
|----------|---------|-----------|
| **SLF4J** | 2.0.9 | Logging facade |
| **Logback** | 1.5.13 | Implementación de logging |
| **Jansi** | 2.4.0 | Colores en consola |
| **Jackson** | 2.15.2 | JSON processing |
| **Unirest** | 4.4.4 | HTTP client |
| **Cucumber** | 7.18.0 | BDD framework |
| **JUnit** | 5.10.0 | Testing framework |
| **Commons Lang3** | 3.18.0 | Utilidades Java |
| **JSON Path** | 2.9.0 | Query JSON |

---

## 🔗 Enlaces Relacionados

- **[Framework Guide](../FRAMEWORK-GUIDE.md)** - Guía completa del framework
- **[API Core](../api-core/README.md)** - Framework para APIs REST
- **[Web Core](../web-core/README.md)** - Framework para Web UI
- **[Troubleshooting](../TROUBLESHOOTING.md)** - Solución de problemas
- **[Contributing](../CONTRIBUTING.md)** - Guía de contribución

---

## 📞 Soporte

¿Problemas con Common?

- 📧 Email: qa-team@scotiabank.com
- 💬 Slack: #qa-automation
- 📝 Issues: [GitHub Issues](https://github.com/scotia-qa/qa-scotia-frameworks/issues)

---

<div align="center">

**[⬆ Volver arriba](#-common---capa-base-del-framework)**

**Versión:** 1.0.2 | **Autor:** Abel Venero | **QA Team - Scotia Bank**

</div>

