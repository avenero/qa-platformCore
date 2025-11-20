# 📦 Common - Framework Base Layer

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Gradle](https://img.shields.io/badge/Gradle-8.14-blue.svg)](https://gradle.org/)
[![Unirest](https://img.shields.io/badge/Unirest-4.4.4-red.svg)](http://kong.github.io/unirest-java/)

---

## 📑 Índice

1. [🎯 ¿Qué es Common?](#-qué-es-common)
2. [🏗️ Arquitectura y Diseño](#️-arquitectura-y-diseño)
3. [🎨 Principio de Diseño: Interfaces sobre Clases Abstractas](#-principio-de-diseño-interfaces-sobre-clases-abstractas)
4. [📂 Estructura de Paquetes](#-estructura-de-paquetes)
5. [📦 Propósito de Cada Paquete](#-propósito-de-cada-paquete)
6. [🌐 Unirest: El Motor HTTP del Framework](#-unirest-el-motor-http-del-framework)
7. [🧩 Componentes Principales de Common](#-componentes-principales-de-common)
8. [🔄 Flujo de Uso](#-flujo-de-uso)
9. [📚 Publicación y Versionado](#-publicación-y-versionado)
10. [🥒 Paquete Cucumber: Gestión de Hooks y Contexto](#-paquete-cucumber-gestión-de-hooks-y-contexto)
11. [🔒 Seguridad Built-in](#-seguridad-built-in)
12. [✅ Mejores Prácticas](#-mejores-prácticas)
13. [📖 Referencias](#-referencias)

---

## 🎯 ¿Qué es Common?

**Common** es la **capa base y fundacional** del Framework QA Automation. No es un framework completo por sí solo, sino el **núcleo genérico y reutilizable** que proporciona:

- 🏗️ **Arquitectura base** mediante interfaces y contratos
- 🔧 **Implementaciones fundamentales** para HTTP, DB, Config, Logging
- 🛠️ **Utilidades transversales** que cualquier módulo puede necesitar
- 📐 **Patrones de diseño** que garantizan extensibilidad y mantenibilidad

### ¿Para quién es Common?

- ✅ **Desarrolladores de Frameworks Específicos** (api-core, web-core, mobile-core)
- ✅ **Arquitectos** que necesitan entender la estructura base
- ✅ **Desarrolladores avanzados** que quieren extender capacidades

### ¿Para quién NO es Common?

- ❌ **QA Engineers de módulos** → Usan api-core, web-core o mobile-core directamente
- ❌ **Proyectos finales** → Consumen frameworks especializados, no common
- ❌ **Lógica de negocio** → Common no conoce clientes específicos (Scotia, Santander, etc.)

---

## 🏗️ Arquitectura y Diseño

### El Rol de Common en el Framework

**Common** es la **capa más baja** de la jerarquía del framework QA:

```
Módulos (scotia-api-tests, santander-web...) 
    ↓ depende de
api-core / web-core / mobile-core
    ↓ depende de
COMMON (esta capa) ← Interfaces + Implementaciones + Utilities
```

**Responsabilidad de Common:**
- Define **contratos** mediante interfaces
- Proporciona **implementaciones base robustas**
- Ofrece **utilidades genéricas** (JSON, fechas, validaciones)
- **NO conoce** lógica de negocio ni clientes específicos

> 💡 Para ver la arquitectura completa de 3 capas, consulta [FRAMEWORK-GUIDE.md](../FRAMEWORK-GUIDE.md#-arquitectura-y-diseño)

---

## 🎨 Principio de Diseño: Interfaces sobre Clases Abstractas

### ❓ ¿Por qué Interfaces en lugar de Clases Abstractas?

Esta es una de las decisiones de arquitectura más importantes del framework.

#### ❌ Problema con Clases Abstractas

```java
// ANTES: Usando clases abstractas
public abstract class AbstractHttpClient {
    protected abstract HttpResponse doGet(String url);
    protected abstract HttpResponse doPost(String url, String body);
    
    // Lógica mezclada con contratos
    public void setTimeout(int timeout) { /* implementación */ }
}

// Los hijos están FORZADOS a heredar todo
public class ApiHttpClient extends AbstractHttpClient {
    // ❌ No puede heredar de otra clase (herencia única)
    // ❌ Está acoplado a la implementación del padre
    // ❌ Difícil de mockear en tests
}
```

#### ✅ Solución con Interfaces

```java
// AHORA: Interfaces puras + Implementaciones base
public interface HttpClient {
    HttpResponse get(String endpoint);
    HttpResponse post(String endpoint);
    void addHeader(String key, String value);
    // Solo contratos, SIN implementación
}

// Implementación base completa y funcional
public class BaseHttpClient implements HttpClient {
    // Lógica robusta que CUALQUIERA puede usar
    @Override
    public HttpResponse get(String endpoint) {
        // Implementación con Unirest
    }
}

// Los consumidores tienen OPCIONES:
public class ApiHttpClient implements HttpClient {
    // Opción 1: Composición (recomendado)
    private BaseHttpClient delegate = new BaseHttpClient();
    
    @Override
    public HttpResponse get(String endpoint) {
        // Pre-procesamiento específico de API
        validateApiEndpoint(endpoint);
        
        // Delega a la implementación base
        HttpResponse response = delegate.get(endpoint);
        
        // Post-procesamiento
        validateApiContract(response);
        return response;
    }
}

// O Opción 2: Herencia si lo prefieren
public class ApiHttpClient extends BaseHttpClient {
    @Override
    public HttpResponse get(String endpoint) {
        // Sobrescribe solo lo necesario
        return super.get(endpoint);
    }
}
```

### 🎯 Beneficios Concretos

| Aspecto | Clases Abstractas | Interfaces + Base |
|---------|-------------------|-------------------|
| **Flexibilidad** | ❌ Herencia única (solo 1 padre) | ✅ Múltiples interfaces implementables |
| **Acoplamiento** | ❌ Fuerte (depende del padre) | ✅ Bajo (solo contrato) |
| **Testing** | ❌ Difícil mockear | ✅ Fácil mockear (solo interfaz) |
| **Extensibilidad** | ❌ Limitada por el padre | ✅ Total libertad |
| **Composición** | ❌ No permite bien | ✅ Totalmente posible |
| **Cambios** | ❌ Rompe hijos | ✅ No afecta si respetas contrato |
| **Múltiples comportamientos** | ❌ Imposible | ✅ Puedes implementar N interfaces |

### 💡 Ejemplo Real del Beneficio

```java
// Con interfaces, puedes hacer esto:
public class AdvancedApiClient implements HttpClient, Serializable, Cloneable {
    // ✅ Implementa 3 interfaces diferentes
    // ✅ Usa composición para delegar en BaseHttpClient
    // ✅ Agrega comportamientos específicos
    
    private final BaseHttpClient httpDelegate = new BaseHttpClient();
    private final MetricsCollector metrics = new MetricsCollector();
    
    @Override
    public HttpResponse get(String endpoint) {
        metrics.startTimer();
        HttpResponse response = httpDelegate.get(endpoint);
        metrics.recordDuration();
        return response;
    }
}

// Con clases abstractas, estarías limitado a 1 padre
```

---

## 📂 Estructura de Paquetes

```
common/src/main/java/com/scotia/qa/common/
│
├── 📋 interfaces/                    ← Contratos puros (sin implementación)
│   ├── HttpClient.java               • Contrato para clientes HTTP
│   ├── DatabaseService.java          • Contrato para acceso a DB
│   ├── ConfigurationProvider.java    • Contrato para configuraciones
│   └── AuthenticationService.java    • Contrato para autenticación
│
├── 🏭 implementations/               ← Implementaciones base reutilizables
│   ├── BaseHttpClient.java           • Cliente HTTP completo con Unirest
│   ├── BaseDatabaseService.java      • Servicio DB con pooling
│   ├── YamlConfigurationProvider.java • Proveedor YAML
│   ├── JsonConfigurationProvider.java • Proveedor JSON
│   └── PropertiesConfigurationProvider.java • Proveedor Properties
│
├── 🌐 http/                          ← Clases relacionadas con HTTP
│   ├── HttpMethod.java               • Enum type-safe para métodos HTTP
│   ├── HttpResponse.java             • Wrapper de respuestas HTTP
│   ├── client/                       • Clientes especializados
│   └── exceptions/                   • Excepciones HTTP específicas
│       ├── FrameworkTechnicalException.java
│       └── FrameworkBusinessException.java
│
├── 🗄️ database/                      ← Componentes de base de datos
│   ├── connection/                   • Pool de conexiones
│   ├── connectors/                   • Conectores (Oracle, MySQL)
│   └── repository/                   • Patrones de repositorio
│
├── 🔐 security/                      ← Utilidades de seguridad
│   ├── SSLUtils.java                 • Configuración SSL/TLS
│   └── SensitiveDataSanitizer.java   • Sanitización de logs
│
├── 📝 logging/                       ← Sistema de logging estructurado
│   └── TestLogger.java               • Logger con contexto y sanitización
│
├── 🛠️ utils/                         ← Utilidades transversales
│   ├── DataUtilities.java            • JSON, serialización, contexto
│   ├── DateUtilities.java            • Manejo de fechas
│   ├── JsonPathUtilities.java        • Queries JSONPath avanzadas
│   ├── ValidationUtils.java          • Validaciones genéricas
│   └── ConfigurationUtilities.java   • Helpers de configuración
│
└── 🏷️ enums/                         ← Enumeraciones del framework
    └── HttpMethod.java               • GET, POST, PUT, DELETE, PATCH, etc.
```

---

## 📦 Propósito de Cada Paquete

### 📋 `interfaces/` - Contratos del Framework

**Propósito:** Define **QUÉ** debe hacer cada componente, no **CÓMO**.

**Responsabilidad:**
- Establecer contratos claros y estables
- Permitir múltiples implementaciones
- Facilitar testing con mocks
- Documentar la API pública del framework

**Ejemplo:**
```java
public interface HttpClient {
    HttpResponse get(String endpoint);
    HttpResponse post(String endpoint);
    void addHeader(String key, String value);
    // Define QUÉ, no CÓMO
}
```

---

### 🏭 `implementations/` - Lógica Base Funcional

**Propósito:** Implementaciones **completas y funcionales** listas para usar.

**Responsabilidad:**
- Implementar los contratos de `interfaces/`
- Proporcionar lógica robusta y probada
- Servir como base para especializaciones
- Encapsular complejidad técnica

**Ejemplo:**
```java
public class BaseHttpClient implements HttpClient {
    // Implementación completa con Unirest
    // Manejo de errores, timeouts, retry, logging
}
```

---

### 🌐 `http/` - Todo lo Relacionado con HTTP

**Propósito:** Centralizar el manejo de comunicación HTTP.

**Responsabilidad:**
- Cliente HTTP basado en **Unirest**
- Modelos de request/response
- Excepciones específicas de HTTP
- Configuraciones SSL/TLS para testing

**Componentes clave:**
- `HttpMethod` → Enum type-safe (GET, POST, PUT, DELETE, PATCH)
- `HttpResponse` → Wrapper de respuestas (status, headers, body)
- `BaseHttpClient` → Implementación completa del cliente

---

### 🗄️ `database/` - Acceso a Datos

**Propósito:** Componentes para interactuar con bases de datos.

**Responsabilidad:**
- Pool de conexiones (HikariCP)
- Conectores específicos (Oracle, MySQL, PostgreSQL)
- Manejo de transacciones
- Mapeo de ResultSet a objetos

---

### 🔐 `security/` - Seguridad del Framework

**Propósito:** Utilidades que protegen el framework y los datos.

**Responsabilidad:**
- Configuración SSL para ambientes de testing
- Sanitización de datos sensibles en logs (passwords, tokens)
- Validación de entrada para prevenir inyecciones
- Manejo seguro de credenciales

---

### 📝 `logging/` - Logging Estructurado

**Propósito:** Sistema de logging unificado y seguro.

**Responsabilidad:**
- Sanitiza automáticamente passwords, tokens, api_keys
- Colorea salida en terminal (ANSI)
- Estructura logs por categoría y nivel
- Facilita debugging y troubleshooting
- Timestamps precisos

---

### 🛠️ `utils/` - Utilidades Transversales

**Propósito:** Helpers que cualquier capa puede necesitar.

**Responsabilidad:**
- Serialización/deserialización JSON
- Manejo de fechas con múltiples formatos
- Queries JSONPath complejas (wildcards, filtros)
- Validaciones genéricas (status, headers, body)
- Comparación y diff de JSON

---

## 🌐 Unirest: El Motor HTTP del Framework

### ¿Qué es Unirest?

**Unirest** es una biblioteca HTTP ligera y moderna para Java que:
- ✅ Simplifica peticiones HTTP complejas
- ✅ Maneja automáticamente serialización/deserialización
- ✅ Soporta configuraciones avanzadas (timeouts, SSL, cookies)
- ✅ **No requiere Spring Boot** ni otras dependencias pesadas
- ✅ Es más rápido y ligero que RestTemplate o WebClient
- ✅ API fluida y fácil de usar

### ¿Por qué Unirest y no otros?

| Alternativa | Problema | Unirest |
|-------------|----------|---------|
| **RestTemplate** | Requiere Spring Boot, está deprecated | ✅ Sin dependencias pesadas, moderno |
| **WebClient** | Requiere Spring WebFlux (reactivo), complejo | ✅ API simple y directa |
| **Apache HttpClient** | API verbosa y compleja, mucho boilerplate | ✅ API fluida y concisa |
| **OkHttp** | Más bajo nivel, más código manual | ✅ Mayor abstracción, menos código |

### Configuración de Unirest en BaseHttpClient

#### 🔧 Timeouts: `connectTimeout` vs `socketTimeout`

```java
// En BaseHttpClient.java
Unirest.config()
    .connectTimeout(10000)      // 10 segundos para conectar
    .socketTimeout(30000);      // 30 segundos para leer respuesta
```

**¿Qué significan?**

| Timeout | Qué mide | Cuándo se usa | Excepción si se excede |
|---------|----------|---------------|------------------------|
| **`connectTimeout`** | Tiempo para **establecer** la conexión TCP | Al intentar conectar al servidor | `ConnectException` |
| **`socketTimeout`** | Tiempo para **recibir datos** una vez conectado | Después de conectar, al esperar respuesta | `SocketTimeoutException` |

**Ejemplo práctico:**

```
Cliente → [connectTimeout: 10s] → Servidor (establece conexión TCP)
            ↓
Cliente ← [socketTimeout: 30s] ← Servidor (recibe respuesta HTTP)
```

**Escenarios:**
- Si el **servidor está caído** → Falla en `connectTimeout` (no puede conectar)
- Si el **servidor conecta pero es lento** → Falla en `socketTimeout` (conectó pero tardó mucho en responder)
- Si el **servidor procesa mucho tiempo** → Falla en `socketTimeout`

---

#### 🔄 Follow Redirects: `true` vs `false`

Esta es una configuración **crítica** para API testing.

```java
HttpResponse response = client.executeRequest(HttpMethod.GET, endpoint, followRedirects);
```

| Configuración | Comportamiento | Cuándo Usar |
|---------------|----------------|-------------|
| **`followRedirects = true`** | Unirest sigue automáticamente redirects 3xx (301, 302, 303, 307, 308) hasta el destino final | ✅ Web testing con navegación<br>✅ APIs que redirigen a CDNs<br>✅ OAuth flows |
| **`followRedirects = false`** | Unirest retorna la respuesta 3xx **inmediatamente** sin seguir | ✅ **API testing** (mayoría de casos)<br>✅ Validar el redirect en sí<br>✅ Capturar `Location` header |

**Ejemplo visual de la diferencia:**

```java
// Servidor responde:
// GET /old-url → 301 Moved Permanently
// Location: /new-url

// ==========================================
// Caso 1: followRedirects = true
// ==========================================
HttpResponse response = client.executeRequest(HttpMethod.GET, "/old-url", true);

// Unirest automáticamente hace:
// 1. GET /old-url → recibe 301
// 2. Lee Location header → /new-url
// 3. GET /new-url → recibe 200

// Resultado final que obtienes:
response.getStatusCode()  → 200 (del destino final)
response.getBody()        → contenido de /new-url
// ¡Nunca ves el 301 ni el /old-url!

// ==========================================
// Caso 2: followRedirects = false (RECOMENDADO)
// ==========================================
HttpResponse response = client.executeRequest(HttpMethod.GET, "/old-url", false);

// Unirest hace SOLO:
// 1. GET /old-url → recibe 301
// 2. PARA y retorna la respuesta

// Resultado que obtienes:
response.getStatusCode()           → 301
response.getHeaders().get("Location") → "/new-url"
response.getBody()                 → vacío o mensaje de redirect
// ¡Puedes validar el redirect!
```

**📌 Recomendación del Framework:**

```java
// ✅ Para API Testing (DEFAULT del framework)
HttpResponse response = client.get(endpoint);  // followRedirects = false por defecto

// ✅ Para Web Testing (si lo necesitas)
HttpResponse response = client.executeRequest(HttpMethod.GET, endpoint, true);
```

**¿Por qué false por defecto?**
- Las APIs REST normalmente NO deberían redirigir
- Si redirigen, es importante **validar ese comportamiento**
- Puedes verificar el `Location` header
- Puedes decidir si seguir o no el redirect manualmente

---

#### 🔒 SSL/TLS para Testing

```java
// Método disponible en BaseHttpClient
client.configureSslForTesting();

// Internamente hace:
SSLContext sslContext = SSLUtils.createTrustAllContext();
HostnameVerifier allowAll = SSLUtils.createAllowAllVerifier();

Unirest.config()
    .sslContext(sslContext)
    .hostnameVerifier(allowAll);
```

**⚠️ ADVERTENCIA IMPORTANTE:**

Esta configuración:
- ✅ **SOLO para ambientes de testing locales**
- ✅ Permite conectar a HTTPS con certificados autofirmados
- ✅ Útil para ambientes dev/test sin certificados válidos
- ❌ **NUNCA usar en producción**
- ❌ Deshabilita validación de certificados SSL
- ❌ Vulnerable a ataques man-in-the-middle

**Cuándo usarla:**
```
Testing Local → ✅ OK
Testing Dev/QA con cert inválido → ✅ OK
Testing Pre-Prod con cert válido → ❌ NO (usar validación normal)
Producción → ❌ NUNCA
```

---

#### 🍪 Manejo Automático de Cookies

```java
// Habilitar manejo automático
client.setAutomaticCookieHandling(true);
```

**¿Qué hace Unirest cuando está habilitado?**

1. **Recibe cookies** del servidor en header `Set-Cookie`
2. **Almacena** las cookies en memoria
3. **Envía automáticamente** las cookies en siguientes requests al mismo dominio
4. **Respeta** `Domain`, `Path`, `Expires`, `Secure`, `HttpOnly`

**Ejemplo:**

```java
// Request 1: Login
client.post("/auth/login");
// Servidor responde:
// Set-Cookie: sessionId=abc123; Path=/; HttpOnly

// Request 2: Get Profile (automático con cookie)
client.get("/user/profile");
// Unirest automáticamente envía:
// Cookie: sessionId=abc123

// No necesitas hacer nada manualmente
```

**Útil para:**
- Session-based authentication
- Testing de flows que requieren sesión
- APIs que usan cookies para estado

---

## 🧩 Componentes Principales de Common

### 1. 🌐 HttpClient

**Interface:** `com.scotia.qa.common.interfaces.HttpClient`
**Implementación:** `com.scotia.qa.common.implementations.BaseHttpClient`

**Propósito:** Cliente HTTP genérico para comunicación con APIs.

**Características:**
- Métodos HTTP: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
- Configuración de headers, query params, body
- Soporte para JSON, XML, Form-Data, Multipart
- Timeouts configurables
- Política de reintentos
- SSL/TLS para testing
- Logging y sanitización automática

**Uso básico:**
```java
HttpClient client = new BaseHttpClient();
client.setHost("https://api.example.com");
client.addHeader("Authorization", "Bearer token");
HttpResponse response = client.get("/endpoint");
```

---

### 2. 🗄️ DatabaseService

**Interface:** `com.scotia.qa.common.interfaces.DatabaseService`
**Implementación:** `com.scotia.qa.common.implementations.BaseDatabaseService`

**Propósito:** Acceso genérico a bases de datos relacionales.

**Características:**
- Pool de conexiones (HikariCP)
- Soporte Oracle, MySQL, PostgreSQL
- Prevención de SQL injection (queries parametrizadas)
- Transacciones con commit/rollback
- Mapeo automático de ResultSet

---

### 3. ⚙️ ConfigurationProvider

**Interface:** `com.scotia.qa.common.interfaces.ConfigurationProvider`

**Implementaciones:**
- `YamlConfigurationProvider` → Archivos .yml/.yaml
- `JsonConfigurationProvider` → Archivos .json
- `PropertiesConfigurationProvider` → Archivos .properties

**Propósito:** Carga flexible de configuraciones.

**Ventaja:** Cambiar formato sin cambiar código.

---

### 4. 📝 TestLogger

**Clase:** `com.scotia.qa.common.logging.TestLogger`

**Propósito:** Logging estructurado y seguro.

**Características:**
- Sanitización automática de datos sensibles
- Colores ANSI en terminal
- Niveles: INFO, DEBUG, WARN, ERROR
- Categorización por componente

---

### 5. 🛠️ DataUtilities

**Clase:** `com.scotia.qa.common.utils.DataUtilities`

**Propósito:** Manejo avanzado de datos.

**Capacidades:**
- Serialización/deserialización JSON
- JSONPath queries (wildcards, filtros)
- Comparación y diff de JSON
- Context management (guardar/recuperar objetos)
- Validación de JSON Schema

---

### 6. 📅 DateUtilities

**Clase:** `com.scotia.qa.common.utils.DateUtilities`

**Propósito:** Manejo robusto de fechas.

**Capacidades:**
- Parsing con múltiples formatos
- Formateo personalizado
- Aritmética (add/subtract days/months/years)
- Validaciones (isBefore, isAfter, isWeekend, isBusinessDay)

---

### 7. ✅ ValidationUtils

**Clase:** `com.scotia.qa.common.utils.ValidationUtils`

**Propósito:** Validaciones genéricas.

**Tipos:**
- HTTP: status code, headers, body
- JSON: JSONPath, schema validation
- Strings: not empty, regex
- Números: ranges, positive/negative

---

## 🔄 Flujo de Uso

### Para Desarrolladores de Frameworks (api-core, web-core, mobile-core)

```java
// Paso 1: Implementar interfaces
public class ApiHttpClient implements HttpClient {
    // Tu lógica específica de API
}

// Paso 2: Extender implementations si necesitas
public class ApiHttpClient extends BaseHttpClient {
    @Override
    public HttpResponse post(String endpoint) {
        // Pre-procesamiento
        validateApiEndpoint(endpoint);
        
        // Lógica base
        HttpResponse response = super.post(endpoint);
        
        // Post-procesamiento
        validateApiContract(response);
        return response;
    }
}

// Paso 3: Usar utilities directamente
DataUtilities.deserialize(json, MyClass.class);
DateUtilities.addDays(date, 7);
ValidationUtils.validateStatusCode(response, 200);
```

### Patrón de Especialización por Framework

Este diagrama muestra cómo cada framework especializado extiende las implementaciones base de Common:

```
BaseHttpClient (en common)
     ↓ extends
┌─────────────────────────────────────────────────────────────────┐
│ HttpClientApi    (api-core)         │ ← Métricas, schemas      │
│ HttpClientWeb    (web-core)         │ ← CSRF, sessions         │  
│ HttpClientMobile (mobile-core)      │ ← Device headers, GPS    │
└─────────────────────────────────────────────────────────────────┘
     ↓ consume
┌─────────────────────────────────────────────────────────────────┐
│ Proyectos específicos (Módulos)    │ ← Usan especialización   │
└─────────────────────────────────────────────────────────────────┘
```

**Ejemplos de especialización:**

```java
// En api-core: Especialización para API testing
public class HttpClientApi extends BaseHttpClient {
    @Override
    public HttpResponse executeRequest(HttpMethod method, String endpoint) {
        // Pre-procesamiento: Métricas de performance
        long startTime = System.currentTimeMillis();
        
        // Validación de schema
        validateApiSchema(endpoint);
        
        // Ejecuta la lógica base
        HttpResponse response = super.executeRequest(method, endpoint);
        
        // Post-procesamiento: Registrar métricas
        recordMetrics(endpoint, System.currentTimeMillis() - startTime);
        
        return response;
    }
}

// En mobile-core: Especialización para mobile testing
public class HttpClientMobile extends BaseHttpClient {
    @Override  
    public HttpResponse executeRequest(HttpMethod method, String endpoint) {
        // Pre-procesamiento: Agregar headers específicos de mobile
        addHeader("User-Agent", "Mobile-App/1.0 Android");
        addHeader("X-Device-ID", deviceId);
        addHeader("X-GPS-Location", getCurrentGPS());
        
        // Ejecuta la lógica base
        HttpResponse response = super.executeRequest(method, endpoint);
        
        // Post-procesamiento: Validar respuesta mobile
        validateMobileContract(response);
        
        return response;
    }
}

// En web-core: Especialización para web testing
public class HttpClientWeb extends BaseHttpClient {
    @Override
    public HttpResponse executeRequest(HttpMethod method, String endpoint) {
        // Pre-procesamiento: Manejar CSRF tokens
        if (method == HttpMethod.POST || method == HttpMethod.PUT) {
            String csrfToken = extractCsrfToken();
            addHeader("X-CSRF-Token", csrfToken);
        }
        
        // Ejecuta la lógica base
        HttpResponse response = super.executeRequest(method, endpoint);
        
        // Post-procesamiento: Manejar cookies de sesión
        handleSessionCookies(response);
        
        return response;
    }
}
```

**Ventajas de este patrón:**

| Ventaja | Descripción |
|---------|-------------|
| **Reutilización** | La lógica HTTP core está en `BaseHttpClient` (una sola vez) |
| **Especialización** | Cada framework agrega solo su lógica específica |
| **Mantenimiento** | Cambios en la base afectan a todos automáticamente |
| **Flexibilidad** | Cada framework puede sobrescribir solo lo que necesita |
| **Testing** | La base está testeada; los frameworks solo testan su lógica adicional |

---

## 📚 Publicación y Versionado

**Maven Coordinates:** `com.scotia.qa:common:1.0.1`

**Publicación rápida:**
```bash
cd common
./gradlew clean build publishToMavenLocal
```

> 📖 Para instrucciones completas de setup, compilación y consumo desde módulos, ver [FRAMEWORK-GUIDE.md](../FRAMEWORK-GUIDE.md#-setup-e-instalación)

### Verificar

```bash
ls ~/.m2/repository/com/scotia/qa/common/1.0.1/
# Debe mostrar: common-1.0.1.jar, common-1.0.1.pom
```

### Consumir desde otros módulos

```gradle
// En api-core/build.gradle
dependencies {
    implementation 'com.scotia.qa:common:1.0.1'
}
```

---

## 🥒 Paquete Cucumber: Gestión de Hooks y Contexto

### 📋 ¿Qué es el Paquete Cucumber?

El paquete `com.scotia.qa.common.cucumber` proporciona una **infraestructura base reutilizable** para gestionar el ciclo de vida de tests con Cucumber. Incluye:

- **`BaseCucumberHooks`**: Clase base abstracta con hooks predefinidos
- **`CucumberTestContext`**: Contexto compartido entre steps para pasar datos
- **`ExampleFrameworkHooks`**: Template/ejemplo para frameworks específicos

### 🎯 ¿Para qué sirve?

**Problema que resuelve:**

En proyectos Cucumber grandes, cada framework (API, Web, Mobile) necesita:
- 🔄 Configurar/limpiar recursos antes/después de cada escenario
- 📊 Capturar evidencias cuando un test falla
- 🔗 Compartir datos entre steps (contexto)
- 📝 Logging estructurado y consistente
- 🧹 Cleanup automático de recursos

**Sin esta infraestructura**, cada framework tendría que:
- ❌ Escribir hooks manualmente (repetición)
- ❌ Gestionar contexto de forma inconsistente
- ❌ Lógica de evidencias duplicada
- ❌ Riesgo de fugas de recursos

**Con esta infraestructura:**
- ✅ Heredas una clase base con todo listo
- ✅ Solo implementas los métodos específicos de tu framework
- ✅ Contexto y evidencias gestionados automáticamente
- ✅ Logging y cleanup consistente

---

### 🏗️ Arquitectura del Paquete Cucumber

```
┌────────────────────────────────────────────────────────────────┐
│                   MÓDULOS CONSUMIDORES                         │
│             (Proyectos de test específicos)                    │
│  Ejemplo: scotia-mobile-tests, santander-api-tests            │
│                                                                 │
│  → Usan Steps que internamente usan CucumberTestContext       │
│  → Pueden leer contexto para flujos complejos                 │
└────────────────────────────────────────────────────────────────┘
                              ▲
                              │ usa
                              │
┌────────────────────────────────────────────────────────────────┐
│              FRAMEWORKS ESPECIALIZADOS                         │
│           api-core / web-core / mobile-core                    │
│                                                                 │
│  → Extienden ExampleFrameworkHooks                            │
│  → Implementan lógica específica (ej: cerrar WebDriver)      │
│  → Agregan anotaciones @Before, @After de Cucumber           │
└────────────────────────────────────────────────────────────────┘
                              ▲
                              │ hereda de
                              │
┌────────────────────────────────────────────────────────────────┐
│                    COMMON (Este Módulo)                        │
│                  com.scotia.qa.common.cucumber                 │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  BaseCucumberHooks (Clase abstracta)                     │ │
│  │  • beforeAll() - Setup global                            │ │
│  │  • afterAll() - Cleanup global                           │ │
│  │  • beforeScenario() - Setup por escenario                │ │
│  │  • afterScenario() - Cleanup + evidencias                │ │
│  │  • beforeStep() - Preparación pre-step                   │ │
│  │  • afterStep() - Post-procesamiento step                 │ │
│  └──────────────────────────────────────────────────────────┘ │
│                              ▲                                  │
│                              │ usa                              │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  CucumberTestContext (Clase utilitaria)                  │ │
│  │  • storeData(key, value) - Guardar datos                │ │
│  │  • getData(key) - Recuperar datos                        │ │
│  │  • getAllData() - Ver todo el contexto                   │ │
│  │  • clearContext() - Limpiar entre escenarios             │ │
│  │  • getCurrentScenario() - Nombre del escenario actual    │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  ExampleFrameworkHooks (Template/Ejemplo)                │ │
│  │  • Muestra cómo extender BaseCucumberHooks              │ │
│  │  • Documenta qué métodos implementar                     │ │
│  │  • Guía para frameworks específicos                      │ │
│  └──────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
```

---

### 📘 Componente 1: `CucumberTestContext`

**¿Qué es?**

Un **almacén de datos compartido** (thread-safe) que permite pasar información entre steps del mismo escenario.

**¿Por qué es necesario?**

En Cucumber, los steps son métodos independientes. Si necesitas pasar datos entre ellos:

```gherkin
# Ejemplo de feature
Scenario: Login y consulta de usuario
  Given envío un POST de login con credenciales válidas
  When guardo el token recibido
  And envío un GET a /users con el token guardado
  Then valido que la respuesta sea 200
```

Los steps son clases separadas. **¿Cómo comparten el token?**

**❌ Solución MAL (variables estáticas):**
```java
public class LoginSteps {
    private static String token; // ⚠️ Problema: no thread-safe, compartido entre escenarios
}
```

**✅ Solución BIEN (CucumberTestContext):**
```java
public class LoginSteps {
    @When("guardo el token recibido")
    public void guardarToken() {
        String token = response.extractToken();
        CucumberTestContext.storeData("authToken", token);
    }
}

public class UserSteps {
    @When("envío un GET a /users con el token guardado")
    public void consultarUsuario() {
        String token = (String) CucumberTestContext.getData("authToken");
        httpClient.addHeader("Authorization", "Bearer " + token);
        httpClient.get("/users");
    }
}
```

**API Completa de `CucumberTestContext`:**

```java
// Guardar datos en el contexto del escenario actual
CucumberTestContext.storeData("key", value);

// Recuperar datos
Object value = CucumberTestContext.getData("key");

// Verificar si existe una clave
boolean exists = CucumberTestContext.hasData("key");

// Eliminar una clave específica
CucumberTestContext.removeData("key");

// Obtener todo el contexto (útil para debugging)
Map<String, Object> allData = CucumberTestContext.getAllData();

// Limpiar todo (se hace automáticamente después de cada escenario)
CucumberTestContext.clearContext();

// Información del escenario actual
String scenarioName = CucumberTestContext.getCurrentScenario();
String featureName = CucumberTestContext.getCurrentFeature();
```

**🔒 Thread-Safety:**

`CucumberTestContext` usa `ThreadLocal` internamente, por lo que:
- ✅ Es **seguro para ejecución paralela** de escenarios
- ✅ Cada thread (escenario) tiene su propio contexto aislado
- ✅ No hay contaminación entre escenarios concurrentes

---

### 📘 Componente 2: `BaseCucumberHooks`

**¿Qué es?**

Una **clase abstracta** que define el ciclo de vida completo de tests con Cucumber mediante hooks.

**Ciclo de vida de un test:**

```
┌────────────────────────────────────────────────────────────────┐
│  beforeAll() - 1 vez al inicio de todos los tests             │
│  └─ performGlobalSetup() ← implementar en subclase            │
└────────────────────────────────────────────────────────────────┘
                              ▼
         ┌─────────────────────────────────────┐
         │  Para cada escenario:                │
         │                                      │
         │  beforeScenario()                    │
         │  ├─ Inicializa contexto              │
         │  ├─ Logging de inicio                │
         │  └─ performFrameworkSpecificInitialization() ← implementar
         │                                      │
         │         ┌──────────────────────┐    │
         │         │  Para cada step:      │    │
         │         │                       │    │
         │         │  beforeStep()         │    │
         │         │  └─ prepareFrameworkForStep() ← implementar
         │         │                       │    │
         │         │  [EJECUTA EL STEP]    │    │
         │         │                       │    │
         │         │  afterStep()          │    │
         │         │  └─ postProcessFrameworkStep() ← implementar
         │         │                       │    │
         │         └──────────────────────┘    │
         │                                      │
         │  afterScenario()                     │
         │  ├─ Captura evidencias si falló     │
         │  ├─ performFrameworkSpecificCleanup() ← implementar
         │  └─ Limpia contexto                  │
         └─────────────────────────────────────┘
                              ▼
┌────────────────────────────────────────────────────────────────┐
│  afterAll() - 1 vez al final de todos los tests               │
│  └─ performGlobalCleanup() ← implementar en subclase          │
└────────────────────────────────────────────────────────────────┘
```

**Métodos que debes implementar en tu framework:**

```java
// En api-core, web-core o mobile-core
public class ApiFrameworkHooks extends BaseCucumberHooks {
    
    @Override
    protected String getFrameworkType() {
        return "API"; // o "WEB", "MOBILE"
    }
    
    @Override
    protected void performGlobalSetup() {
        // Setup que se ejecuta 1 vez al inicio
        // Ejemplo: Configurar logging, cargar configs globales
    }
    
    @Override
    protected void performGlobalCleanup() {
        // Cleanup que se ejecuta 1 vez al final
        // Ejemplo: Cerrar pools de conexiones, generar reportes
    }
    
    @Override
    protected void performFrameworkSpecificInitialization() {
        // Setup ANTES de cada escenario
        // API: Configurar cliente HTTP, headers por defecto
        // WEB: Inicializar WebDriver, navegar a home page
        // MOBILE: Inicializar Appium session, instalar app
    }
    
    @Override
    protected void performFrameworkSpecificCleanup(boolean scenarioFailed) {
        // Cleanup DESPUÉS de cada escenario
        // API: Limpiar sesiones, resetear datos de test
        // WEB: Cerrar WebDriver, limpiar cookies
        // MOBILE: Resetear app, desinstalar si es necesario
    }
    
    @Override
    protected void prepareFrameworkForStep(String stepText) {
        // ANTES de cada step (opcional)
        // Ejemplo: Logging detallado, sincronización
    }
    
    @Override
    protected void postProcessFrameworkStep(String stepText, boolean stepFailed) {
        // DESPUÉS de cada step (opcional)
        // Ejemplo: Capturar screenshot si falló
    }
    
    @Override
    protected void captureFrameworkSpecificEvidence(String reason) {
        // Capturar evidencias cuando falla
        // API: Request/response, headers
        // WEB: Screenshot, HTML source, console logs
        // MOBILE: Screenshot, app logs, device info
    }
}
```

---

### 📘 Componente 3: `ExampleFrameworkHooks`

**¿Qué es?**

Un **template completo y documentado** que muestra cómo crear hooks para un framework específico.

**¿Cómo usarlo?**

1. **Copia** `ExampleFrameworkHooks.java` a tu módulo (api-core, web-core, mobile-core)
2. **Renombra** a algo como `ApiFrameworkHooks`, `WebFrameworkHooks`, etc.
3. **Implementa** los métodos abstractos con tu lógica específica
4. **Agrega** las anotaciones de Cucumber (`@Before`, `@After`, etc.)

**Ejemplo completo para API:**

```java
// En api-core/src/main/java/.../ApiFrameworkHooks.java
package com.scotia.qa.apicore.hooks;

import com.scotia.qa.common.cucumber.BaseCucumberHooks;
import com.scotia.qa.common.cucumber.CucumberTestContext;
import com.scotia.qa.common.implementations.BaseHttpClient;
import com.scotia.qa.common.logging.TestLogger;
import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Scenario;

public class ApiFrameworkHooks extends BaseCucumberHooks {
    
    private BaseHttpClient httpClient;
    
    @Override
    protected String getFrameworkType() {
        return "API";
    }
    
    // =============== HOOKS GLOBALES ===============
    
    @BeforeAll
    public static void setUpClass() {
        new ApiFrameworkHooks().beforeAll();
    }
    
    @AfterAll
    public static void tearDownClass() {
        new ApiFrameworkHooks().afterAll();
    }
    
    @Override
    protected void performGlobalSetup() {
        TestLogger.logInfo("GLOBAL_SETUP", "Configurando framework API", null);
    }
    
    @Override
    protected void performGlobalCleanup() {
        TestLogger.logInfo("GLOBAL_CLEANUP", "Limpieza global API completada", null);
    }
    
    // =============== HOOKS POR ESCENARIO ===============
    
    @Before
    public void setUp(Scenario scenario) {
        String scenarioName = scenario.getName();
        String featureName = scenario.getUri().toString();
        beforeScenario(scenarioName, featureName);
    }
    
    @After
    public void tearDown(Scenario scenario) {
        boolean failed = scenario.isFailed();
        String failureReason = failed ? "Scenario failed: " + scenario.getName() : null;
        afterScenario(failed, failureReason);
    }
    
    @Override
    protected void performFrameworkSpecificInitialization() {
        // Inicializar cliente HTTP para este escenario
        httpClient = new BaseHttpClient();
        CucumberTestContext.storeData("httpClient", httpClient);
        TestLogger.logStep("API_INIT", "Cliente HTTP inicializado para escenario");
    }
    
    @Override
    protected void performFrameworkSpecificCleanup(boolean scenarioFailed) {
        // Limpiar sesiones, resetear configuraciones
        if (httpClient != null) {
            httpClient.clearHeaders();
            httpClient = null;
        }
        TestLogger.logStep("API_CLEANUP", "Cleanup de API completado");
    }
}
```

---

### 🎓 Guía para Testers Junior: Cómo Usar Este Paquete

#### **Escenario 1: Soy QA en un módulo de test**

**¿Qué necesito hacer?**

**Nada especial.** Los hooks ya están configurados por tu framework (api-core, web-core, mobile-core).

Solo necesitas:

1. **Usar `CucumberTestContext`** para pasar datos entre steps:

```java
// Step 1: Guardar un token
@When("hago login")
public void hacerLogin() {
    // ... login logic ...
    String token = response.getToken();
    CucumberTestContext.storeData("authToken", token);
}

// Step 2: Usar el token guardado
@When("consulto mis datos")
public void consultarDatos() {
    String token = (String) CucumberTestContext.getData("authToken");
    httpClient.addHeader("Authorization", "Bearer " + token);
    // ... consulta ...
}
```

2. **Confiar en que los hooks limpiarán el contexto** entre escenarios automáticamente.

---

#### **Escenario 2: Soy desarrollador de api-core / web-core / mobile-core**

**¿Qué necesito hacer?**

1. **Copiar** `ExampleFrameworkHooks.java` a tu módulo
2. **Renombrar** según tu framework
3. **Implementar** los métodos abstractos con tu lógica
4. **Agregar anotaciones** de Cucumber (`@Before`, `@After`, etc.)

**Checklist de implementación:**

- [ ] Extender `BaseCucumberHooks`
- [ ] Implementar `getFrameworkType()` → retornar "API", "WEB" o "MOBILE"
- [ ] Implementar `performGlobalSetup()` → setup global
- [ ] Implementar `performGlobalCleanup()` → cleanup global
- [ ] Implementar `performFrameworkSpecificInitialization()` → setup por escenario
- [ ] Implementar `performFrameworkSpecificCleanup()` → cleanup por escenario
- [ ] Implementar `captureFrameworkSpecificEvidence()` → capturar evidencias
- [ ] Agregar métodos con anotaciones `@BeforeAll`, `@AfterAll`, `@Before`, `@After`
- [ ] (Opcional) Agregar `@BeforeStep`, `@AfterStep` si necesitas

---

### ⚠️ Errores Comunes y Soluciones

#### Error 1: "Context no se limpia entre escenarios"

**Causa:** No estás llamando a `afterScenario()` en tu hook `@After`.

**Solución:**
```java
@After
public void tearDown(Scenario scenario) {
    boolean failed = scenario.isFailed();
    afterScenario(failed, null); // ← Esto limpia el contexto
}
```

---

#### Error 2: "NullPointerException al obtener datos del contexto"

**Causa:** Intentas obtener una clave que no existe.

**Solución:**
```java
// ❌ MAL
String token = (String) CucumberTestContext.getData("token");

// ✅ BIEN - Verificar antes
if (CucumberTestContext.hasData("token")) {
    String token = (String) CucumberTestContext.getData("token");
} else {
    throw new IllegalStateException("Token no encontrado en contexto");
}
```

---

#### Error 3: "Datos contaminados entre escenarios paralelos"

**Causa:** Usas variables estáticas en lugar de `CucumberTestContext`.

**Solución:** Siempre usa `CucumberTestContext` para datos compartidos entre steps.

---

### 📊 Ventajas y Desventajas de Usar Este Paquete

| Aspecto | Ventajas | Desventajas |
|---------|----------|-------------|
| **Reutilización** | ✅ Evita código duplicado en cada framework | ❌ Requiere entender la jerarquía de herencia |
| **Mantenimiento** | ✅ Cambios en common afectan a todos | ⚠️ Cambios pueden romper frameworks si no se hace bien |
| **Consistencia** | ✅ Logging y evidencias uniformes | - |
| **Thread-Safety** | ✅ Contexto aislado por thread | - |
| **Curva de aprendizaje** | ⚠️ Junior debe entender hooks y ciclo de vida | ✅ Template bien documentado facilita adopción |
| **Flexibilidad** | ✅ Puedes sobrescribir cualquier método | - |
| **Debugging** | ✅ Logs estructurados facilitan troubleshooting | - |

---

### 🎯 Cuándo NO Usar Este Paquete

- ❌ Si tu proyecto **no usa Cucumber** (obviamente)
- ❌ Si prefieres **control total** y no quieres herencia
- ❌ Si tu proyecto es **muy simple** y no necesita contexto compartido
- ❌ Si tu equipo **no está familiarizado** con hooks de Cucumber

En esos casos, puedes:
- Crear tus propios hooks desde cero
- Usar solo `CucumberTestContext` sin hooks
- Implementar tu propia estrategia de contexto

---

### 📚 Recursos Adicionales

- **Código fuente:**
  - `BaseCucumberHooks.java` - Clase base con toda la lógica
  - `CucumberTestContext.java` - Gestión de contexto thread-safe
  - `ExampleFrameworkHooks.java` - Template completo

- **Documentación externa:**
  - [Cucumber Hooks Documentation](https://cucumber.io/docs/cucumber/api/#hooks)
  - [ThreadLocal en Java](https://docs.oracle.com/javase/8/docs/api/java/lang/ThreadLocal.html)

---

## 🔒 Seguridad Built-in

### 1. Sanitización Automática
- `password` → `***HIDDEN***`
- `token`, `access_token`, `refresh_token` → `***HIDDEN***`
- `authorization` → `***HIDDEN***`
- `api_key`, `secret` → `***HIDDEN***`

### 2. Validación de Entrada
- JSON parsing seguro
- Prevención de deserialización insegura
- Validación de tipos

### 3. SSL/TLS
- Producción: Validación completa
- Testing: Opcional deshabilitar (solo local)

---

## ✅ Mejores Prácticas

### ✅ DO (Hacer)

1. Implementa interfaces para contratos claros
2. Extiende clases base cuando necesites personalización
3. Usa composición sobre herencia
4. Documenta con JavaDoc
5. Mantén genérico - sin lógica de negocio

### ❌ DON'T (No Hacer)

1. No agregues lógica de negocio en common
2. No hardcodees valores específicos de clientes
3. No rompas contratos de interfaces
4. No mezcles responsabilidades
5. No ignores excepciones

---

## 📖 Referencias

- [Unirest Java](http://kong.github.io/unirest-java/)
- [Jackson Databind](https://github.com/FasterXML/jackson-databind)
- [HikariCP](https://github.com/brettwooldridge/HikariCP)
- [Logback](https://logback.qos.ch/)

---

**Common Framework** - La base sólida para frameworks de automatización QA.

*Para documentación completa del framework: `/FRAMEWORK-GUIDE.md`*

