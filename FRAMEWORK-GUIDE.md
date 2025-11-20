# 🚀 Framework QA Automation - Guía Completa

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Gradle](https://img.shields.io/badge/Gradle-8.14-blue.svg)](https://gradle.org/)
[![Cucumber](https://img.shields.io/badge/Cucumber-7.18.0-green.svg)](https://cucumber.io/)
[![Unirest](https://img.shields.io/badge/Unirest-4.4.4-red.svg)](http://kong.github.io/unirest-java/)

---

## 📑 Índice

1. [Introducción](#-introducción)
2. [Tecnologías y Versiones](#-tecnologías-y-versiones)
3. [Arquitectura y Diseño](#-arquitectura-y-diseño)
4. [Setup e Instalación](#-setup-e-instalación)
5. [Configuración del Framework](#-configuración-del-framework)
6. [Componentes Principales](#-componentes-principales)
   - [HTTP Client](#http-client)
   - [Validaciones](#validaciones)
   - [Configuración](#configuración)
   - [Base de Datos](#base-de-datos)
   - [Logging](#logging)
   - [Utilidades](#utilidades)
7. [Steps Implementados](#-steps-implementados)
8. [Cómo Crear Nuevos Steps](#-cómo-crear-nuevos-steps)
9. [Ejemplos de Uso](#-ejemplos-de-uso)
10. [Mejores Prácticas](#-mejores-prácticas)
11. [Seguridad](#-seguridad)
12. [Troubleshooting](#-troubleshooting)
13. [Contribución](#-contribución)

---

## 🎯 Introducción

**Framework QA Common** es un framework de automatización de pruebas robusto, genérico y extensible diseñado para soportar testing de APIs, Web y Mobile. Construido sobre principios de arquitectura limpia y desacoplamiento total.

### ¿Por qué este Framework?

- ✅ **Sin Spring Boot**: Menos dependencias, más rápido, más liviano
- ✅ **Genérico y Reutilizable**: No conoce lógica de negocio específica
- ✅ **Arquitectura por Capas**: Common → Core (API/Web/Mobile) → Módulos
- ✅ **Type-Safe**: Validaciones en tiempo de compilación
- ✅ **Seguro**: Sanitización de logs, protección contra inyección
- ✅ **Extensible**: Fácil agregar nuevas capacidades

### ¿Qué NO es este Framework?

- ❌ No es específico de ningún cliente (Scotia, Santander, etc.)
- ❌ No contiene lógica de negocio
- ❌ No depende de Spring Boot
- ❌ No está acoplado a ninguna herramienta específica

---

## 🛠️ Tecnologías y Versiones

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Java** | 21 | Lenguaje base |
| **Gradle** | 8.14 | Build tool |
| **Cucumber** | 7.18.0 | BDD Framework |
| **JUnit Platform** | 1.10.0 | Test runner |
| **Unirest** | 4.4.4 | HTTP Client |
| **Jackson** | 2.18.2 | JSON Processing |
| **Logback** | 1.4.11 | Logging |
| **SLF4J** | 2.0.7 | Logging facade |
| **Oracle JDBC** | 21.9.0.0 | Database driver |
| **MySQL Connector** | 8.0.33 | Database driver |
| **JsonPath** | 2.9.0 | JSON querying |
| **Jansi** | 2.4.0 | Terminal colors |

### Dependencias Opcionales

```gradle
// Para testing de DB
testImplementation 'com.h2database:h2:2.2.220'

// Para assertions avanzadas
testImplementation 'org.assertj:assertj-core:3.24.2'
```

---

## 🏗️ Arquitectura y Diseño

### Diagrama de Capas

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           🏢 MÓDULOS CONSUMIDORES                                │
│                          (Proyectos Específicos)                               │
│   qa-autos • qa-banking • qa-mobile-app • qa-integration • qa-e2e-flows        │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        ↑
                                   ┌────┴────┐
                                   │ Consume │
                                   └────┬────┘
                                        ↓
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        🎯 CAPA 2: FRAMEWORKS ESPECIALIZADOS                    │
├─────────────────────┬─────────────────────┬─────────────────────────────────────┤
│   📱 mobile-core    │    🌐 api-core      │       💻 web-core                  │
│                     │                     │                                     │
│ • Appium            │ • REST Testing      │ • Selenium WebDriver                │
│ • Device Mgmt       │ • API Steps         │ • Page Object Models               │
│ • Native Apps       │ • Orchestration     │ • Cross-browser                     │
│ • Mobile Utils      │ • HTTP specialized  │ • UI Components                     │
└─────────────────────┴─────────────────────┴─────────────────────────────────────┘
                                        ↑
                                ┌───────┴───────┐
                                │   Dependen    │
                                └───────┬───────┘
                                        ↓
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          🔧 CAPA 1: COMMON (BASE)                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│ interfaces/                                                                     │
│ ├── HttpClient.java           ├── ValidationService.java                       │
│ ├── AuthenticationService.java ├── ConfigurationProvider.java                  │
│ └── DatabaseService.java      └── CucumberHooksService.java                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│ implementations/                                                                │
│ ├── BaseHttpClient.java       ├── ValidationUtils.java                         │
│ ├── BaseAuthService.java      ├── YamlConfigurationProvider.java               │
│ └── BaseDatabaseService.java  └── BaseCucumberHooks.java                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ utils/ • logging/ • http/ • cucumber/ • security/ • database/                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Flujo de Dependencias

```
MÓDULOS CONSUMIDORES (Ejemplos específicos)
     ↓
┌─────────────┬─────────────┬─────────────┬─────────────────┐
│qa-autos     │qa-banking   │qa-mobile-app│qa-integration   │
│             │             │             │                 │
│Uses:        │Uses:        │Uses:        │Uses:            │
│• api-core   │• web-core   │• mobile-core│• ALL cores      │
│             │• api-core   │• api-core   │                 │
└─────────────┴─────────────┴─────────────┴─────────────────┘
                           ↓ consume
┌─────────────────────────────────────────────────────────────┐
│              FRAMEWORKS ESPECIALIZADOS                     │
│  mobile-core  ←→  api-core  ←→  web-core                   │
│                    ↓ extend                                 │
│              COMMON (interfaces + base implementations)    │
└─────────────────────────────────────────────────────────────┘
```

### Matriz de Uso por Proyecto

Tabla que muestra qué proyectos usan qué frameworks:

```
┌─────────────────┬─────────┬─────────┬─────────────┬─────────┐
│ PROYECTO        │ COMMON  │API-CORE │ WEB-CORE    │MOB-CORE │
├─────────────────┼─────────┼─────────┼─────────────┼─────────┤
│ qa-autos        │    ✓    │    ✓    │      -      │    -    │
│ qa-banking      │    ✓    │    ✓    │      ✓      │    -    │
│ qa-mobile-app   │    ✓    │    ✓    │      -      │    ✓    │
│ qa-integration  │    ✓    │    ✓    │      ✓      │    ✓    │
│ qa-performance  │    ✓    │    ✓    │      -      │    -    │
│ qa-e2e-flows    │    ✓    │    ✓    │      ✓      │    ✓    │
└─────────────────┴─────────┴─────────┴─────────────┴─────────┘
```

**Puntos clave:**
- Todos los proyectos dependen de **COMMON** (base obligatoria)
- **api-core** es el más usado (APIs son fundamentales)
- Proyectos pueden combinar múltiples cores según necesidad
- Integración E2E usa todos los cores para flujos completos

### Principios de Diseño

1. **Separation of Concerns**: Cada capa tiene responsabilidades claras
2. **Dependency Inversion**: Las capas superiores dependen de abstracciones
3. **Interface Segregation**: Contratos pequeños y específicos
4. **Single Responsibility**: Una clase, una razón para cambiar
5. **Open/Closed**: Abierto a extensión, cerrado a modificación

### Flujo de Ejecución

```
Feature (Gherkin)
    ↓
Step Definition (Módulo)
    ↓
ApiSteps (API-CORE) ← Puede usar helpers del módulo
    ↓
BaseHttpClient (COMMON)
    ↓
Unirest HTTP
    ↓
API Backend
```

---

## 📌 Versionado y Dependencias

### Estrategia de Versionado Independiente

Cada capa del framework tiene su propio versionado independiente:

```
COMMON v1.0.1 (Base estable)
     ↓ depende de
┌─────────────────────────────────────┐
│ api-core v1.0.1                     │ ← Depende de common v1.0.1
│ web-core v1.0.0                     │ ← Depende de common v1.0.1  
│ mobile-core v1.0.0                  │ ← Depende de common v1.0.1
└─────────────────────────────────────┘
     ↓ consume
┌─────────────────────────────────────┐
│ qa-autos v3.2.1                     │ ← Usa api-core v1.0.1
│ qa-banking v2.8.0                   │ ← Usa web-core v1.0.0 + api-core v1.0.1
│ qa-mobile-app v1.9.2                │ ← Usa mobile-core v1.0.0 + api-core v1.0.1
└─────────────────────────────────────┘
```

### Ventajas del Versionado Independiente

| Ventaja | Descripción |
|---------|-------------|
| **Estabilidad** | Cambios en un core no afectan a otros |
| **Flexibilidad** | Cada proyecto puede usar versiones diferentes según necesidad |
| **Mantenimiento** | Actualizaciones incrementales sin romper todo |
| **Testing** | Puedes probar nueva versión de api-core sin afectar web-core |

### Cómo Actualizar Versiones

**En tu proyecto (módulo consumidor):**

```gradle
dependencies {
    // Especifica la versión exacta que necesitas
    implementation 'com.scotia.qa:api-core:1.0.1'  // ← Controlas la versión
    
    // Puedes usar diferentes versiones si es necesario
    // implementation 'com.scotia.qa:web-core:1.0.0'
}
```

**Recomendaciones:**
- ✅ Mantén **common** en la versión más reciente estable
- ✅ Actualiza **cores** cuando necesites nuevas features
- ✅ Actualiza **módulos** a tu propio ritmo
- ⚠️ Verifica compatibilidad antes de actualizar

---

## ⚙️ Setup e Instalación

### 1. Pre-requisitos

```bash
# Java 21
java -version
# Debe mostrar: openjdk version "21.x.x"

# Gradle 8.14
gradle -version
# Debe mostrar: Gradle 8.14
```

### 2. Clonar y Configurar

```bash
# Clonar el repositorio
git clone <repo-url>
cd qa-scotia-frameworks

# Estructura del proyecto
# qa-scotia-frameworks/
# ├── common/          ← Framework base
# ├── api-core/        ← Especialización API
# ├── web-core/        ← Especialización Web
# ├── mobile-core/     ← Especialización Mobile
# └── settings.gradle
```

### 3. Compilar Common

```bash
cd common
./gradlew clean build

# Output esperado:
# BUILD SUCCESSFUL in Xs
```

### 4. Publicar en Maven Local

```bash
./gradlew publishToMavenLocal

# Verifica:
ls ~/.m2/repository/com/scotia/qa/common/1.0.1/
# Debe existir: common-1.0.1.jar, common-1.0.1.pom
```

### 5. Compilar API-CORE

```bash
cd ../api-core
./gradlew clean build publishToMavenLocal

# Verifica:
ls ~/.m2/repository/com/scotia/qa/api-core/1.0.1/
# Debe existir: api-core-1.0.1.jar, api-core-1.0.1.pom
```

### 6. Crear Módulo Consumidor

```gradle
// build.gradle del módulo
plugins {
    id 'java'
}

group = 'com.module'
version = '1.0.0'

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenLocal()  // ← IMPORTANTE
    mavenCentral()
}

dependencies {
    // Framework
    implementation 'com.scotia.qa:api-core:1.0.1'
    
    // Cucumber
    testImplementation 'io.cucumber:cucumber-java:7.18.0'
    testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.18.0'
    testImplementation 'org.junit.platform:junit-platform-suite:1.10.0'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
    
    // Logging
    implementation 'org.slf4j:slf4j-api:2.0.7'
    implementation 'ch.qos.logback:logback-classic:1.4.11'
}

test {
    useJUnitPlatform()
    systemProperty "cucumber.plugin", "json:build/cucumber.json"
}
```

---

## 🔧 Configuración del Framework

### Logback (común)

```xml
<!-- common/src/main/resources/logback-base.xml -->
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <withJansi>true</withJansi>
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %highlight(%-5level) [%cyan(%logger{0})] %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

### Logback (módulo consumidor)

```xml
<!-- modulo/src/test/resources/logback.xml -->
<configuration>
    <include resource="logback-base.xml"/>
    
    <!-- Silenciar logs innecesarios -->
    <logger name="org.apache" level="ERROR"/>
    <logger name="com.zaxxer.hikari" level="ERROR"/>
    <logger name="io.cucumber" level="WARN"/>
</configuration>
```

### Cucumber Runner

```java
// modulo/src/test/java/com/module/runner/RunCucumberTest.java
package com.module.runner;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.module.steps,com.scotia.qa.apicore.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty,json:build/cucumber.json")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@test")
public class RunCucumberTest {
}
```

### Estructura de Features

```
modulo/
└── src/
    └── test/
        ├── java/
        │   └── com/
        │       └── module/
        │           ├── runner/
        │           │   └── RunCucumberTest.java
        │           └── steps/
        │               └── CustomSteps.java
        └── resources/
            ├── features/
            │   ├── api/
            │   │   └── users.feature
            │   └── integration/
            │       └── journey.feature
            └── logback.xml
```

---

## 🧩 Componentes Principales

### HTTP Client

El `BaseHttpClient` es el corazón de las peticiones HTTP.

#### Métodos Principales

```java
// Configuración
void setHost(String host)
void setContext(String context)
void addHeader(String key, String value)
void addQueryParam(String key, String value)
void setBody(String body)

// Ejecución
HttpResponse executeRequest(HttpMethod method, String endpoint)
HttpResponse get(String endpoint)
HttpResponse post(String endpoint)
HttpResponse put(String endpoint)
HttpResponse delete(String endpoint)
HttpResponse patch(String endpoint)

// Respuesta
HttpResponse getLastResponse()

// Configuración avanzada
void setFollowRedirects(boolean follow)
void setTimeout(int millis)
void configureSslForTesting()
```

#### Ejemplo de Uso Directo

```java
BaseHttpClient client = new BaseHttpClient();
client.setHost("https://api.example.com");
client.setContext("/v1/users");
client.addHeader("Authorization", "Bearer token123");
client.addQueryParam("page", "1");

HttpResponse response = client.get("");

System.out.println("Status: " + response.getStatusCode());
System.out.println("Body: " + response.getBody());
```

---

### Validaciones

El `ValidationUtils` proporciona validaciones robustas.

#### Métodos Disponibles

```java
// HTTP
void validateStatusCode(HttpResponse response, int expected)
void validateHeader(HttpResponse response, String header, String expected)
void validateBodyContains(HttpResponse response, String expected)
void validateBodyNotEmpty(HttpResponse response)

// JSON
void validateJsonPath(String json, String path, Object expected)
void validateJsonSchema(String json, String schema)

// Strings
void validateNotEmpty(String value, String fieldName)
void validatePattern(String value, String pattern, String fieldName)

// Números
void validateRange(int value, int min, int max, String fieldName)
void validatePositive(int value, String fieldName)
```

---

### Configuración

Soporte para múltiples formatos de configuración.

#### YAML

```yaml
# config/test.yml
environment:
  name: test
  baseUrl: https://api.test.example.com
  timeout: 5000

auth:
  username: testuser
  password: testpass

database:
  host: localhost
  port: 5432
  name: testdb
```

```java
ConfigurationProvider provider = new YamlConfigurationProvider("config/test.yml");
String baseUrl = provider.getProperty("environment.baseUrl");
```

#### JSON

```json
{
  "environment": {
    "name": "test",
    "baseUrl": "https://api.test.example.com"
  }
}
```

```java
ConfigurationProvider provider = new JsonConfigurationProvider("config/test.json");
```

#### Properties

```properties
# config.properties
environment.name=test
environment.baseUrl=https://api.test.example.com
```

```java
ConfigurationProvider provider = new PropertiesConfigurationProvider("config.properties");
```

---

### Base de Datos

Soporte para Oracle y MySQL con pool de conexiones.

#### Configuración

```properties
# db-config.properties
db.driver=oracle.jdbc.driver.OracleDriver
db.url=jdbc:oracle:thin:@localhost:1521:XE
db.username=testuser
db.password=testpass
db.pool.size=10
db.pool.timeout=30000
```

#### Uso

```java
DatabaseService dbService = new BaseDatabaseService();
dbService.connect("db-config.properties");

// Query
List<Map<String, Object>> results = dbService.executeQuery(
    "SELECT * FROM users WHERE status = ?",
    "active"
);

// Update
int rowsAffected = dbService.executeUpdate(
    "UPDATE users SET last_login = ? WHERE id = ?",
    new Date(), userId
);

dbService.disconnect();
```

---

### Logging

Sistema de logging estructurado con niveles y contexto.

#### Métodos

```java
// Niveles
TestLogger.logInfo(String category, String message, String testName)
TestLogger.logDebug(String category, String message, String testName)
TestLogger.logWarn(String category, String message, String testName)
TestLogger.logError(String category, String message, String testName)

// HTTP específico
TestLogger.logHttpRequest(String method, String url, String body)
TestLogger.logHttpResponse(int status, String body, long duration)
```

#### Características

- ✅ Sanitización automática de datos sensibles (passwords, tokens)
- ✅ Colores en terminal (ANSI)
- ✅ Timestamps precisos
- ✅ Categorización
- ✅ Formato estructurado

---

### Utilidades

#### DataUtilities

```java
// Serialización/Deserialización
<T> T deserialize(String json, Class<T> clazz)
String serialize(Object object)

// Context Management
void storeObject(String key, Object value)
<T> T getObject(String key, Class<T> clazz)
void clearContext()

// JSONPath
Object extractJsonPath(String json, String path)
List<Object> extractJsonPathList(String json, String path)

// Comparación
boolean compareJson(String expected, String actual)
String jsonDiff(String expected, String actual)
```

#### DateUtilities

```java
// Parsing
LocalDate parseDate(String date, String format)
LocalDateTime parseDateTime(String dateTime, String format)

// Formateo
String formatDate(LocalDate date, String format)
String formatDateTime(LocalDateTime dateTime, String format)

// Aritmética
LocalDate addDays(LocalDate date, int days)
LocalDate subtractDays(LocalDate date, int days)
long daysBetween(LocalDate start, LocalDate end)

// Validación
boolean isBefore(LocalDate date1, LocalDate date2)
boolean isAfter(LocalDate date1, LocalDate date2)
boolean isWeekend(LocalDate date)
boolean isBusinessDay(LocalDate date)
```

#### JsonPathUtilities

```java
// Queries avanzadas
Object query(String json, String path)
List<Object> queryList(String json, String path)

// Wildcards y filtros
// $.store.book[*].author
// $.store.book[?(@.price < 10)]
```

---

## 📝 Steps Implementados

### Configuración de Host y Contexto

```gherkin
Given el host "https://api.example.com" mas el contexto "/v1/users"
```

```java
@Given("el host {string} mas el contexto {string}")
public void usarHostMasElContexto(String host, String context)
```

---

### Headers

```gherkin
# Recomendado ✅
And agrego el header "Content-Type" con valor "application/json"
And agrego el header "Authorization" con valor "Bearer {token}"

# Deprecado ⚠️ (aún funciona)
And agrego el header "Content-Type" con el valor "application/json"
```

```java
@And("agrego el header {word} con valor {word}")
public void agregoElHeaderConValor(String key, String value)
```

---

### Query Parameters

```gherkin
And agrego el query param "page" con valor "1"
And agrego el query param "size" con valor "10"
```

```java
@And("agrego el query param {word} con valor {word}")
public void agregoElQueryParamConValor(String key, String value)
```

---

### Body

```gherkin
And agrego el request
"""
{
  "username": "testuser",
  "email": "test@example.com"
}
"""
```

```java
@And("agrego el request")
public void agregoElRequest(String body)
```

---

### Ejecución

```gherkin
# Con redirects
When ejecuto la consulta con el metodo "POST"

# Sin redirects (recomendado para APIs)
When ejecuto la consulta con el metodo "POST" sin redireccion

# Métodos soportados: GET, POST, PUT, DELETE, PATCH
```

```java
@When("ejecuto la consulta con el metodo {string}")
public void ejecutoLaConsultaConElMetodo(String method)

@When("ejecuto la consulta con el metodo {string} sin redireccion")
public void ejecutoLaConsultaConElMetodoSinRedireccion(String method)
```

---

### Validaciones de Status

```gherkin
Then valido que el codigo de respuesta del servicio sea 200
Then valido que el codigo de respuesta del servicio sea 404
```

```java
@Then("valido que el codigo de respuesta del servicio sea {int}")
public void validoQueElCodigoDeRespuestaSea(int expectedStatus)
```

---

### Validaciones de Body

```gherkin
Then valido que el body contiene "success"
Then valido que el body no está vacío
```

```java
@Then("valido que el body contiene {string}")
public void validoQueElBodyContiene(String expected)

@Then("valido que el body no está vacío")
public void validoQueElBodyNoEstaVacio()
```

---

### Validaciones de Headers

```gherkin
Then valido que el header "Content-Type" contiene "application/json"
```

```java
@Then("valido que el header {string} contiene {string}")
public void validoQueElHeaderContiene(String header, String expected)
```

---

### JSONPath

```gherkin
Then valido que el json path "$.user.name" es "John Doe"
Then valido que el json path "$.items[0].price" es "99.99"
```

```java
@Then("valido que el json path {string} es {string}")
public void validoQueElJsonPathEs(String path, String expected)
```

---

### JSON Schema

```gherkin
Then valido que el cuerpo de la respuesta tenga el siguiente esquema
"""
{
  "type": "object",
  "properties": {
    "id": {"type": "string"},
    "name": {"type": "string"},
    "age": {"type": "number"}
  },
  "required": ["id", "name"]
}
"""
```

```java
@Then("valido que el cuerpo de la respuesta tenga el siguiente esquema")
public void validoQueElResponseTengaElSiguienteEsquema(String schema)
```

---

### Serialización y Context

```gherkin
# Serializar respuesta en clase
Then serializo la respuesta en la clase "com.module.models.User"

# Guardar objeto en contexto
And guardo el objeto serializado como "currentUser"

# Recuperar objeto
Then obtengo el objeto "currentUser" del contexto
```

```java
@Then("serializo la respuesta en la clase {string}")
public void serializoLaRespuestaEnLaClase(String className)

@And("guardo el objeto serializado como {string}")
public void guardoElObjetoSerializadoComo(String key)

@Then("obtengo el objeto {string} del contexto")
public void obtengoElObjetoDelContexto(String key)
```

---

## 🎓 Cómo Crear Nuevos Steps

### Paso 1: Crear Step Definition en el Módulo

```java
// modulo/src/test/java/com/module/steps/CustomSteps.java
package com.module.steps;

import com.scotia.qa.common.implementations.BaseHttpClient;
import com.scotia.qa.common.http.HttpResponse;
import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.utils.DataUtilities;
import io.cucumber.java.en.*;

public class CustomSteps {
    
    private final BaseHttpClient httpClient;
    
    public CustomSteps() {
        this.httpClient = new BaseHttpClient();
    }
    
    @Given("configuro autenticación con token {string}")
    public void configuroAutenticacionConToken(String token) {
        httpClient.addHeader("Authorization", "Bearer " + token);
        TestLogger.logInfo("AUTH", "Token configurado exitosamente", null);
    }
    
    @When("obtengo la lista de usuarios paginada con página {int} y tamaño {int}")
    public void obtengoListaUsuariosPaginada(int page, int size) {
        httpClient.setHost("https://api.example.com");
        httpClient.setContext("/v1/users");
        httpClient.addQueryParam("page", String.valueOf(page));
        httpClient.addQueryParam("size", String.valueOf(size));
        
        httpClient.get("");
        
        TestLogger.logInfo("USER_LIST", 
            String.format("Usuarios obtenidos - página %d, tamaño %d", page, size), 
            null);
    }
    
    @Then("extraigo el id del primer usuario y lo guardo como {string}")
    public void extraigoIdPrimerUsuario(String key) {
        HttpResponse response = httpClient.getLastResponse();
        String json = response.getBody();
        
        Object userId = DataUtilities.extractJsonPath(json, "$.users[0].id");
        DataUtilities.storeObject(key, userId);
        
        TestLogger.logInfo("USER_EXTRACT", "User ID guardado: " + userId, null);
    }
}
```

---

### Paso 2: Crear Feature

```gherkin
# modulo/src/test/resources/features/users/list-users.feature
@api @users
Feature: Gestión de usuarios

  Scenario: Obtener lista de usuarios con paginación
    Given configuro autenticación con token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
    When obtengo la lista de usuarios paginada con página 1 y tamaño 10
    Then valido que el codigo de respuesta del servicio sea 200
    And valido que el json path "$.users" no está vacío
    And extraigo el id del primer usuario y lo guardo como "userId"
```

---

### Paso 3: Ejecutar

```bash
./gradlew clean test --tests RunCucumberTest
```

---

## 💡 Ejemplos de Uso

### Ejemplo 1: Login Simple

```gherkin
@test @auth
Feature: Autenticación

  Scenario: Login exitoso
    Given el host "https://api.example.com" mas el contexto "/auth/login"
    And agrego el header "Content-Type" con valor "application/json"
    And agrego el request
    """
    {
      "username": "testuser",
      "password": "Test123!"
    }
    """
    When ejecuto la consulta con el metodo "POST" sin redireccion
    Then valido que el codigo de respuesta del servicio sea 200
    And valido que el json path "$.token" no está vacío
```

---

### Ejemplo 2: CRUD Completo

```gherkin
@test @crud
Feature: CRUD de Productos

  Background:
    Given el host "https://api.example.com"
    And agrego el header "Content-Type" con valor "application/json"
    And agrego el header "Authorization" con valor "Bearer token123"

  Scenario: Crear producto
    Given el contexto "/v1/products"
    And agrego el request
    """
    {
      "name": "Laptop",
      "price": 999.99,
      "stock": 50
    }
    """
    When ejecuto la consulta con el metodo "POST" sin redireccion
    Then valido que el codigo de respuesta del servicio sea 201
    And serializo la respuesta en la clase "com.module.models.Product"
    And guardo el objeto serializado como "newProduct"

  Scenario: Consultar producto
    Given el contexto "/v1/products/123"
    When ejecuto la consulta con el metodo "GET" sin redireccion
    Then valido que el codigo de respuesta del servicio sea 200
    And valido que el json path "$.name" es "Laptop"

  Scenario: Actualizar producto
    Given el contexto "/v1/products/123"
    And agrego el request
    """
    {
      "price": 899.99
    }
    """
    When ejecuto la consulta con el metodo "PUT" sin redireccion
    Then valido que el codigo de respuesta del servicio sea 200

  Scenario: Eliminar producto
    Given el contexto "/v1/products/123"
    When ejecuto la consulta con el metodo "DELETE" sin redireccion
    Then valido que el codigo de respuesta del servicio sea 204
```

---

### Ejemplo 3: Journey End-to-End

```gherkin
@test @e2e @journey
Feature: Journey de compra completa

  Scenario: Usuario completa una compra exitosa
    # Paso 1: Autenticación
    Given el host "https://api.example.com" mas el contexto "/auth/login"
    And agrego el header "Content-Type" con valor "application/json"
    And agrego el request
    """
    {"username": "buyer@test.com", "password": "Test123!"}
    """
    When ejecuto la consulta con el metodo "POST" sin redireccion
    Then valido que el codigo de respuesta del servicio sea 200
    And serializo la respuesta en la clase "com.module.models.AuthResponse"
    And guardo el objeto serializado como "auth"

    # Paso 2: Buscar productos
    Given el contexto "/v1/products"
    And agrego el query param "category" con valor "electronics"
    When ejecuto la consulta con el metodo "GET" sin redireccion
    Then valido que el codigo de respuesta del servicio sea 200

    # Paso 3: Agregar al carrito
    Given el contexto "/v1/cart/items"
    And agrego el request
    """
    {"productId": "prod-123", "quantity": 2}
    """
    When ejecuto la consulta con el metodo "POST" sin redireccion
    Then valido que el codigo de respuesta del servicio sea 201

    # Paso 4: Checkout
    Given el contexto "/v1/orders/checkout"
    And agrego el request
    """
    {
      "paymentMethod": "credit_card",
      "shippingAddress": {
        "street": "123 Main St",
        "city": "Test City",
        "zip": "12345"
      }
    }
    """
    When ejecuto la consulta con el metodo "POST" sin redireccion
    Then valido que el codigo de respuesta del servicio sea 200
    And valido que el json path "$.status" es "confirmed"
```

---

### Ejemplo 4: Data-Driven Testing

```gherkin
@test @data-driven
Feature: Validación de emails

  Scenario Outline: Validar registro con diferentes emails
    Given el host "https://api.example.com" mas el contexto "/users/register"
    And agrego el header "Content-Type" con valor "application/json"
    And agrego el request
    """
    {
      "email": "<email>",
      "password": "Test123!"
    }
    """
    When ejecuto la consulta con el metodo "POST" sin redireccion
    Then valido que el codigo de respuesta del servicio sea <expectedStatus>

    Examples:
      | email                | expectedStatus |
      | valid@example.com    | 201            |
      | invalid.email        | 400            |
      | missing@domain       | 400            |
      | @nodomain.com        | 400            |
```

---

### Ejemplo 5: JSON Schema Validation

```gherkin
@test @schema
Feature: Validación de contratos API

  Scenario: Validar schema de respuesta de usuario
    Given el host "https://api.example.com" mas el contexto "/v1/users/123"
    When ejecuto la consulta con el metodo "GET" sin redireccion
    Then valido que el codigo de respuesta del servicio sea 200
    And valido que el cuerpo de la respuesta tenga el siguiente esquema
    """
    {
      "type": "object",
      "properties": {
        "id": {"type": "string", "pattern": "^[0-9]+$"},
        "name": {"type": "string", "minLength": 1},
        "email": {"type": "string", "format": "email"},
        "age": {"type": "integer", "minimum": 0, "maximum": 150},
        "roles": {
          "type": "array",
          "items": {"type": "string"}
        },
        "createdAt": {"type": "string", "format": "date-time"}
      },
      "required": ["id", "name", "email"]
    }
    """
```

---

## ✨ Mejores Prácticas

### 1. Organización de Features

```
features/
├── auth/
│   ├── login.feature
│   └── logout.feature
├── users/
│   ├── create.feature
│   ├── list.feature
│   └── update.feature
└── integration/
    └── complete-journey.feature
```

---

### 2. Uso de Background

```gherkin
Feature: Gestión de productos

  Background:
    Given el host "https://api.example.com"
    And agrego el header "Content-Type" con valor "application/json"
    And agrego el header "Authorization" con valor "Bearer token123"

  Scenario: Crear producto
    # Ya tienes headers configurados
    Given el contexto "/v1/products"
    # ...
```

---

### 3. Tags para Organización

```gherkin
@api @smoke @critical
Feature: Funcionalidad crítica

@api @regression
Feature: Pruebas de regresión

@api @wip
Feature: Work in progress
```

```bash
# Ejecutar solo smoke
./gradlew test -Dcucumber.filter.tags="@smoke"

# Excluir WIP
./gradlew test -Dcucumber.filter.tags="not @wip"
```

---

### 4. Reutilización con Helpers

```java
// Módulo: CustomHelper.java
public class AuthHelper {
    
    private final BaseHttpClient client;
    
    public String loginAndGetToken(String username, String password) {
        client.setHost("https://api.example.com");
        client.setContext("/auth/login");
        client.addHeader("Content-Type", "application/json");
        
        String body = String.format(
            "{\"username\":\"%s\",\"password\":\"%s\"}", 
            username, password
        );
        client.setBody(body);
        client.post("");
        
        HttpResponse response = client.getLastResponse();
        return (String) DataUtilities.extractJsonPath(
            response.getBody(), 
            "$.token"
        );
    }
}

// En Steps
@Given("me autentico como {string} con password {string}")
public void meAutenticoComo(String username, String password) {
    String token = authHelper.loginAndGetToken(username, password);
    httpClient.addHeader("Authorization", "Bearer " + token);
}
```

---

### 5. Manejo de Datos Sensibles

```java
// ✅ Buena práctica: Variables de entorno
String apiKey = System.getenv("API_KEY");
httpClient.addHeader("X-API-Key", apiKey);

// ✅ Buena práctica: Archivo de configuración fuera del repo
ConfigurationProvider config = new YamlConfigurationProvider("config/secrets.yml");
String password = config.getProperty("test.user.password");

// ❌ Mala práctica: Hardcodear secretos
httpClient.addHeader("X-API-Key", "abc123secret"); // NO HACER
```

---

### 6. Assertions Claras

```gherkin
# ✅ Bueno: Específico y claro
Then valido que el codigo de respuesta del servicio sea 201
And valido que el json path "$.id" no está vacío
And valido que el json path "$.status" es "active"

# ❌ Malo: Genérico
Then la respuesta es correcta
```

---

### 7. Nombrado de Scenarios

```gherkin
# ✅ Bueno: Descriptivo y orientado a negocio
Scenario: Usuario registrado puede iniciar sesión exitosamente
Scenario: Sistema rechaza login con credenciales inválidas

# ❌ Malo: Técnico y poco claro
Scenario: Test 1
Scenario: POST /auth/login returns 200
```

---

## 🔒 Seguridad

### Sanitización de Logs

El framework sanitiza automáticamente datos sensibles:

```java
// Automáticamente sanitiza:
- password
- token
- access_token
- refresh_token
- authorization
- api_key
- secret

// Log real:
{
  "username": "user@test.com",
  "password": "***HIDDEN***",
  "token": "***HIDDEN***"
}
```

---

### Validación de Entrada

```java
// El framework valida automáticamente:
- JSON bien formado
- Tipos de datos correctos
- Rangos válidos
- Patrones de regex

// Protección contra:
- Inyección de código
- JSON malformado
- Deserialización insegura
```

---

### SSL/TLS para Testing

```java
// Para ambientes de testing (NO PRODUCCIÓN):
httpClient.configureSslForTesting();

// WARNING: Esto deshabilita validación de certificados
// Solo usar en ambientes controlados de testing
```

---

## 🐛 Troubleshooting

### Error: Response is null

```
FrameworkBusinessException: Response is null
```

**Causa**: No se ejecutó un request antes de validar la respuesta.

**Solución**:
```gherkin
# Asegúrate de ejecutar el request primero
When ejecuto la consulta con el metodo "POST" sin redireccion
Then valido que el codigo de respuesta del servicio sea 200
```

---

### Error: Could not find com.scotia.qa:api-core

```
Could not find com.scotia.qa:api-core:1.0.1
```

**Causa**: No está publicado en Maven Local.

**Solución**:
```bash
cd api-core
./gradlew publishToMavenLocal
```

---

### Error: Invalid JSON

```
JsonProcessingException: Unexpected character
```

**Causa**: JSON malformado en el body.

**Solución**: Valida el JSON en un editor o herramienta online.

```gherkin
# ✅ Correcto
And agrego el request
"""
{
  "name": "test",
  "value": 123
}
"""

# ❌ Incorrecto
And agrego el request
"""
{
  name: "test",  # Falta comillas
  value: 123
}
"""
```

---

### Error: SSL Handshake Failed

```
SSLException: Unsupported or unrecognized SSL message
```

**Solución**:
```java
// En tu Step o Helper
httpClient.configureSslForTesting();
```

---

### Error: Connection Timeout

```
SocketTimeoutException: connect timed out
```

**Solución**:
```java
// Aumentar timeout
httpClient.setTimeout(30000); // 30 segundos
```

---

### Logs no muestran colores

**Solución**: Asegúrate de tener Jansi configurado.

```gradle
implementation 'org.fusesource.jansi:jansi:2.4.0'
```

```xml
<!-- logback.xml -->
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <withJansi>true</withJansi>
    <!-- ... -->
</appender>
```

---

### Guidelines de Código

```java
// ✅ Buenas prácticas:
- Interfaces para contratos
- Implementations para lógica
- Javadoc en métodos públicos
- Logging estructurado
- Manejo robusto de errores
- Sin lógica de negocio específica
- Sin hardcode de valores de negocio

// ❌ Evitar:
- Acoplamiento a módulos específicos
- Dependencias innecesarias
- Lógica de negocio en Common
- Magic numbers
- Código duplicado
```

---

### Ejemplo de Nueva Funcionalidad

```java
// 1. Interface en common/src/main/java/com/scotia/qa/common/interfaces/
public interface CacheService {
    void put(String key, Object value);
    Object get(String key);
    void clear();
}

// 2. Implementation en common/src/main/java/com/scotia/qa/common/implementations/
public class BaseCacheService implements CacheService {
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    
    @Override
    public void put(String key, Object value) {
        cache.put(key, value);
        TestLogger.logDebug("CACHE", "Stored: " + key, null);
    }
    
    // ...
}

// 3. Step en api-core/src/main/java/com/scotia/qa/apicore/steps/
@And("guardo en cache {string} con valor {string}")
public void guardoEnCache(String key, String value) {
    cacheService.put(key, value);
}

// 4. Documentar en README
```

---

## 📚 Referencias

### Enlaces Útiles

- [Cucumber Documentation](https://cucumber.io/docs/cucumber/)
- [Unirest Java Docs](http://kong.github.io/unirest-java/)
- [Jackson Databind](https://github.com/FasterXML/jackson-databind)
- [JSONPath Online Evaluator](https://jsonpath.com/)
- [JSON Schema Validator](https://www.jsonschemavalidator.net/)

---

### Contacto y Soporte

Para dudas, problemas o sugerencias:

1. Revisar este README
2. Consultar JavaDocs en el código
3. Revisar logs de ejecución
4. Contactar al equipo de QA Framework

---

## 📊 Estado del Proyecto

### Versión Actual: 1.0.1

### Últimas Actualizaciones

- ✅ Migración completa de Spring Boot a Unirest
- ✅ Implementación de interfaces y arquitectura limpia
- ✅ Sistema de logging robusto con sanitización
- ✅ Soporte para JSON Schema validation
- ✅ Utilidades avanzadas de JSONPath
- ✅ Context management para serialización
- ✅ Soporte multi-formato de configuración
- ✅ Sanitización de seguridad en logs

---

### Roadmap Futuro

- [ ] Soporte para GraphQL
- [ ] Integración con Allure Reports
- [ ] Performance testing utilities
- [ ] Mock server integrado
- [ ] Contract testing (Pact)

---

## 🎉 ¡Listo para Comenzar!

Ahora tienes todo lo necesario 
---

*Framework QA Common - Construido con ❤️ por el equipo de QA*

