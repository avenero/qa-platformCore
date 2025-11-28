# 🌐 API Core Layer - Testing de APIs REST

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)]()

> Capa especializada para testing de APIs REST/SOAP. Proporciona steps de Cucumber, validaciones y utilidades para automatizar pruebas de servicios web.

---

## 📑 Índice

- [Visión General](#visión-general)
- [Características](#características)
- [Arquitectura](#arquitectura)
- [Steps Disponibles](#steps-disponibles)
- [Ejemplos de Uso](#ejemplos-de-uso)
- [Configuración](#configuración)
- [Integración con Módulos](#integración-con-módulos)
- [Referencia Rápida](#referencia-rápida)

---

## Visión General

**API-Core** es la capa especializada del framework para **testing de APIs**. Se construye sobre **Common Layer** y proporciona:

✅ **Steps de Cucumber** específicos para APIs REST
✅ **Validaciones** de status codes, headers, body (JSON/XML)
✅ **Manejo de autenticación** (Bearer, Basic, API Key)
✅ **Extracción de datos** con JsonPath
✅ **Integración con ScenarioContext** para compartir datos
✅ **Soporte para múltiples métodos** HTTP (GET, POST, PUT, DELETE, PATCH)

### Dependencias

```
api-core
    └── common (automática)
        ├── HTTP Client (BaseHttpClient)
        ├── Logging (TestLogger)
        ├── Config (ConfigManager)
        └── ScenarioContext
```

---

## Características

### 🎯 Steps de Cucumber

API-Core proporciona **+40 steps** listos para usar en tus features:

#### Configuración de Request
- `Dado que tengo el endpoint "..."`
- `Y agrego el header "..." con valor "..."`
- `Y agrego el request: """..."""`
- `Y agrego parámetro de query "..." con valor "..."`

#### Ejecución
- `Cuando ejecuto una petición GET`
- `Cuando ejecuto una petición POST`
- `Cuando ejecuto una petición PUT`
- `Cuando ejecuto una petición DELETE`

#### Validaciones
- `Entonces el código de respuesta debe ser ...`
- `Y el tiempo de respuesta debe ser menor a ... ms`
- `Y el body debe contener el campo "..."`
- `Y el campo "..." debe ser "..."`
- `Y el campo "..." debe contener "..."`

#### Manejo de Datos
- `Y guardo el valor del campo "..." en variable "..."`
- `Y extraigo con JsonPath "..." y guardo en "..."`

**Ver lista completa:** [QUICK-REFERENCE.md](QUICK-REFERENCE.md)

---

## Arquitectura

```
api-core/
├── src/main/java/com/scotia/qa/apicore/
│   ├── steps/
│   │   └── ApiSteps.java          ← Steps de Cucumber para API
│   │
│   ├── client/
│   │   └── RestClient.java        ← Cliente REST especializado
│   │
│   ├── validators/
│   │   ├── ResponseValidator.java ← Validaciones de responses
│   │   └── SchemaValidator.java   ← Validaciones de schemas
│   │
│   └── utils/
│       ├── JsonUtils.java         ← Utilidades JSON
│       └── AuthUtils.java         ← Utilidades de auth
│
└── src/main/resources/
    └── schemas/                    ← JSON Schemas para validación
```

### Flujo de Ejecución

```
┌────────────────────────────────────────────────────────────────┐
│  FEATURE (Gherkin)                                             │
│  @api                                                          │
│  Escenario: Login en API                                      │
│    Dado que tengo el endpoint "/auth/login"                   │
│    Y agrego el request: """{"user":"test"}"""                 │
│    Cuando ejecuto una petición POST                           │
│    Entonces el código de respuesta debe ser 200               │
└────────────────────────────────────────────────────────────────┘
                             ↓
┌────────────────────────────────────────────────────────────────┐
│  API-CORE (ApiSteps.java)                                      │
│  • Construye request con headers, body, params                │
│  • Usa BaseHttpClient (de Common)                             │
│  • Ejecuta petición HTTP                                      │
│  • Valida respuesta                                           │
│  • Guarda datos en ScenarioContext                            │
└────────────────────────────────────────────────────────────────┘
                             ↓
┌────────────────────────────────────────────────────────────────┐
│  COMMON (BaseHttpClient)                                       │
│  • Ejecuta petición HTTP con Unirest                          │
│  • Maneja timeouts y retries                                  │
│  • Sanitiza logs (oculta passwords)                           │
│  • Retorna Response wrapper                                   │
└────────────────────────────────────────────────────────────────┘
```

---

## Steps Disponibles

### Categoría: Configuración de Request

| Step | Descripción | Ejemplo |
|------|-------------|---------|
| `Dado que tengo el endpoint {string}` | Define el endpoint a llamar | `Dado que tengo el endpoint "/users"` |
| `Y el host {string} mas el contexto {string}` | Define host + path | `Y el host "https://api.com" mas el contexto "/v1/users"` |
| `Y agrego el header {string} con valor {string}` | Agrega header HTTP | `Y agrego el header "Content-Type" con valor "application/json"` |
| `Y agrego el request:` | Agrega body JSON/XML | Ver ejemplos abajo |
| `Y agrego parámetro de query {string} con valor {string}` | Query parameter | `Y agrego parámetro de query "page" con valor "1"` |

### Categoría: Ejecución

| Step | Descripción | Ejemplo |
|------|-------------|---------|
| `Cuando ejecuto una petición GET` | Ejecuta GET | `Cuando ejecuto una petición GET` |
| `Cuando ejecuto una petición POST` | Ejecuta POST | `Cuando ejecuto una petición POST` |
| `Cuando ejecuto la consulta con el metodo {string}` | Método genérico | `Cuando ejecuto la consulta con el metodo "PATCH"` |

### Categoría: Validaciones

| Step | Descripción | Ejemplo |
|------|-------------|---------|
| `Entonces el código de respuesta debe ser {int}` | Valida status code | `Entonces el código de respuesta debe ser 200` |
| `Y el tiempo de respuesta debe ser menor a {int} ms` | Valida performance | `Y el tiempo de respuesta debe ser menor a 2000 ms` |
| `Y el body debe contener el campo {string}` | Verifica existencia | `Y el body debe contener el campo "data"` |
| `Y el campo {string} debe ser {string}` | Valida igualdad | `Y el campo "status" debe ser "success"` |
| `Y el campo {string} debe contener {string}` | Valida substring | `Y el campo "email" debe contener "@"` |

### Categoría: Extracción de Datos

| Step | Descripción | Ejemplo |
|------|-------------|---------|
| `Y guardo el valor del campo {string} en variable {string}` | Guarda en contexto | `Y guardo el valor del campo "id" en variable "userId"` |
| `Y obtengo el campo {string} del objeto {string} y lo guardo como {string}` | Extracción anidada | Ver ejemplos |

---

## Ejemplos de Uso

### Ejemplo 1: Login Simple

```gherkin
@api @test
Escenario: Login exitoso en API
  Dado que tengo el endpoint "/auth/login"
  Y agrego el header "Content-Type" con valor "application/json"
  Y agrego el request:
    """
    {
      "username": "testuser",
      "password": "Test123"
    }
    """
  Cuando ejecuto una petición POST
  Entonces el código de respuesta debe ser 200
  Y el body debe contener el campo "token"
  Y guardo el valor del campo "token" en variable "authToken"
```

### Ejemplo 2: Consulta con Autenticación

```gherkin
@api @test
Escenario: Consultar perfil de usuario
  Dado que tengo el endpoint "/users/me"
  Y agrego el header "Authorization" con valor "Bearer {authToken}"
  Cuando ejecuto una petición GET
  Entonces el código de respuesta debe ser 200
  Y el campo "email" debe contener "@"
  Y el campo "status" debe ser "active"
```

### Ejemplo 3: CRUD Completo

```gherkin
@api @test
Escenario: CRUD de producto
  # CREATE
  Dado que tengo el endpoint "/products"
  Y agrego el request:
    """
    {
      "name": "Laptop",
      "price": 999.99,
      "category": "Electronics"
    }
    """
  Cuando ejecuto una petición POST
  Entonces el código de respuesta debe ser 201
  Y guardo el valor del campo "id" en variable "productId"
  
  # READ
  Dado que tengo el endpoint "/products/{productId}"
  Cuando ejecuto una petición GET
  Entonces el código de respuesta debe ser 200
  Y el campo "name" debe ser "Laptop"
  
  # UPDATE
  Dado que tengo el endpoint "/products/{productId}"
  Y agrego el request:
    """
    {
      "price": 899.99
    }
    """
  Cuando ejecuto una petición PATCH
  Entonces el código de respuesta debe ser 200
  
  # DELETE
  Dado que tengo el endpoint "/products/{productId}"
  Cuando ejecuto una petición DELETE
  Entonces el código de respuesta debe ser 204
```

### Ejemplo 4: Integración con Web (Cross-Layer)

```gherkin
@api @web
Escenario: Crear usuario por API y validar en Web
  # Crear usuario por API
  Dado que tengo el endpoint "/users"
  Y agrego el request:
    """
    {
      "name": "Juan Pérez",
      "email": "juan@test.com"
    }
    """
  Cuando ejecuto una petición POST
  Entonces el código de respuesta debe ser 201
  Y guardo el valor del campo "id" en variable "userId"
  
  # Validar en interfaz web
  Dado que navego a la URL "https://app.example.com/users/{userId}"
  Entonces el texto del elemento "userName" debe ser "Juan Pérez"
```

---

## Configuración

### En el Módulo

**1. Agregar dependencia en `build.gradle`:**

```groovy
dependencies {
    testImplementation 'com.scotia.qa:api-core:1.0.0'
    // common se incluye automáticamente
}
```

**2. Configurar URLs en `config-scotia.properties`:**

```properties
# API Testing
api.base.url=${{API_BASE_URL}}
api.timeout=30000
api.retry.count=3
```

**3. Configurar variables de entorno en `.env.local`:**

```bash
API_BASE_URL=https://api-qa.example.com/v1
API_TOKEN=your_token_here
```

**4. Agregar glue en `RunCucumberTest.java`:**

```java
@ConfigurationParameter(
    key = "cucumber.glue",
    value = "com.scotia.qa.apicore, com.scotia.qa.common, com.tu.proyecto.steps"
)
```

---

## Integración con Módulos

### Estructura Típica del Módulo

```
qa-module-tu-proyecto/
├── src/test/
│   ├── java/
│   │   └── com/tu/proyecto/
│   │       ├── RunCucumberTest.java      ← Configuración Cucumber
│   │       └── steps/
│   │           └── CustomSteps.java      ← Steps personalizados
│   │
│   └── resources/
│       ├── features/
│       │   └── api/
│       │       ├── login.feature          ← Features de API
│       │       └── users.feature
│       │
│       ├── config-scotia.properties       ← Configuración
│       └── test-data/                     ← Test data
│
├── .env.local                             ← Credenciales (gitignored)
└── build.gradle                           ← Dependencias
```

### Flujo de Trabajo

```
1. Escribir Feature (Gherkin)
   └─> features/api/mi-test.feature
   
2. Usar Steps de api-core
   └─> Ya disponibles, no requiere implementación
   
3. Ejecutar Tests
   └─> ./gradlew test
   o
   └─> ../qa-scotia-frameworks/scripts/test.sh
   
4. Ver Reportes
   └─> build/reports/cucumber/cucumber-html-report.html
```

---

## Referencia Rápida

### Cheat Sheet de Steps Comunes

```gherkin
# Configurar endpoint
Dado que tengo el endpoint "/api/resource"

# Agregar autenticación
Y agrego el header "Authorization" con valor "Bearer {token}"

# Agregar body JSON
Y agrego el request:
  """
  {"key": "value"}
  """

# Ejecutar
Cuando ejecuto una petición POST

# Validar
Entonces el código de respuesta debe ser 200
Y el campo "status" debe ser "success"

# Guardar dato
Y guardo el valor del campo "id" en variable "resourceId"
```

### Variables en Contexto

```java
// API-Core guarda automáticamente en ScenarioContext
ScenarioContext.setByLayer("api", "token", "abc123");

// Recuperar en cualquier step
String token = (String) ScenarioContext.getFromLayer("api", "token");

// O usar en Gherkin con {}
Dado que tengo el endpoint "/users/{userId}"
Y agrego el header "Authorization" con valor "Bearer {authToken}"
```

---

## 📚 Documentación Adicional

- **[QUICK-REFERENCE.md](QUICK-REFERENCE.md)** - Referencia rápida de todos los steps
- **[../FRAMEWORK-GUIDE.md](../FRAMEWORK-GUIDE.md)** - Arquitectura del framework
- **[../QUICK-START.md](../QUICK-START.md)** - Guía de inicio rápido
- **[../common/README.md](../common/README.md)** - Documentación de Common Layer

---

## 🐛 Troubleshooting

### ❌ Step undefined

**Problema:** Cucumber no encuentra los steps de api-core.

**Solución:** Verificar que el glue incluye `com.scotia.qa.apicore`:

```java
@ConfigurationParameter(
    key = "cucumber.glue",
    value = "com.scotia.qa.apicore, com.scotia.qa.common, com.tu.proyecto"
)
```

### ❌ Connection refused

**Problema:** No se puede conectar al API.

**Solución:** Verificar configuración en `.env.local`:

```bash
API_BASE_URL=https://correct-url.com
```

### ❌ Variables no se resuelven

**Problema:** `{userId}` no se reemplaza.

**Solución:** Verificar que la variable se guardó antes:

```gherkin
# Primero guardar
Y guardo el valor del campo "id" en variable "userId"

# Luego usar
Dado que tengo el endpoint "/users/{userId}"
```

---

**Última actualización:** 28 de Noviembre de 2025  
**Autor:** Abel Venero  
**Versión:** 1.0.0

