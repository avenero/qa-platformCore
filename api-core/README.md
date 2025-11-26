# 🌐 API-Core - Framework de Testing REST

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Unirest](https://img.shields.io/badge/Unirest-4.4.4-red.svg)](http://kong.github.io/unirest-java/)
[![Version](https://img.shields.io/badge/version-1.0.2-blue.svg)](https://github.com/scotia-qa/qa-scotia-frameworks)

> Framework especializado para automatización de pruebas de APIs REST. Proporciona steps de Cucumber predefinidos, validaciones, y utilidades para testing de servicios web.

---

## 📑 Índice

- [🎯 Visión General](#-visión-general)
- [🏗️ Arquitectura](#️-arquitectura)
- [📦 Componentes Principales](#-componentes-principales)
- [🥒 Steps Disponibles](#-steps-disponibles)
- [💡 Ejemplos Completos](#-ejemplos-completos)
- [🔗 Integración con Web/Mobile](#-integración-con-webmobile)
- [⚠️ Troubleshooting](#️-troubleshooting)

---

## 🎯 Visión General

### ¿Qué es API-Core?

**API-Core** es la capa especializada para **testing de APIs REST**. Extiende **common** y proporciona:

- 🔌 **Cliente HTTP avanzado** (Unirest)
- ✅ **Validaciones REST** (status, headers, JSON, XML)
- 🔐 **Autenticación** (OAuth, JWT, Basic Auth, API Keys)
- 🥒 **Steps de Cucumber** predefinidos
- 🗄️ **Soporte de BD** para validaciones backend
- 📊 **JSON Path** y validaciones de schema

### ¿Para Qué Usar API-Core?

- ✅ Automatizar pruebas de APIs REST/SOAP
- ✅ Validar contratos y schemas JSON
- ✅ Testing de autenticación y autorización
- ✅ Validaciones de datos en base de datos
- ✅ Flujos híbridos (API + Web/Mobile)

---

## 🏗️ Arquitectura

### Diagrama de Flujo

```
┌──────────────────────────────────────────────────┐
│          Feature (Cucumber Gherkin)              │
│  Given el host "..." mas el contexto "..."      │
│  When ejecuto consulta con metodo "POST"        │
│  Then valido codigo de respuesta sea 200        │
└──────────────────────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────┐
│              ApiSteps (Cucumber)                 │
│  - Define contratos Gherkin                      │
│  - Orquesta llamadas                             │
│  - Guarda en ScenarioContext                     │
└──────────────────────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────┐
│          HTTP Client (Unirest)                   │
│  - Construye request                             │
│  - Maneja autenticación                          │
│  - Ejecuta llamada HTTP                          │
└──────────────────────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────┐
│         Validaciones & Assertions                │
│  - Status code                                   │
│  - Headers                                       │
│  - JSON/XML body                                 │
│  - JSON Schema                                   │
└──────────────────────────────────────────────────┘
```

### Estructura de Paquetes

```
api-core/
├── steps/                  # Cucumber step definitions
│   └── ApiSteps
│
├── implementations/        # Clientes HTTP especializados
│   └── RestApiClient
│
├── factories/              # Factories de requests
│   └── RequestFactory
│
└── utils/                  # Utilidades API
    ├── JsonValidator
    └── SchemaValidator
```

---

## 📦 Componentes Principales

### ApiSteps

**50+ steps predefinidos** para testing de APIs:

```gherkin
# Configuración de endpoint
Given el host "https://api.example.com" mas el contexto "/users"

# Headers
And agrego el header "Content-Type" con valor "application/json"
And agrego el header "Authorization" con valor "Bearer {token}"

# Body
And agrego el request
  """
  {"username": "john", "password": "secret"}
  """

# Ejecutar
When ejecuto la consulta con el metodo "POST"

# Validaciones
Then valido que el codigo de respuesta del servicio sea 200
And valido que el response contenga el campo "id"
And obtengo el campo "token" del objeto "data" y lo guardo como "authToken"
```

### RestApiClient

Cliente HTTP especializado basado en Unirest:

```java
RestApiClient client = new RestApiClient("https://api.example.com");

// GET
HttpResponse<JsonNode> response = client.get("/users/123");

// POST con body
Map<String, Object> body = Map.of("name", "John", "email", "john@example.com");
HttpResponse<JsonNode> response = client.post("/users", body);

// Headers
client.addHeader("Authorization", "Bearer " + token);
client.addHeader("Content-Type", "application/json");

// Response
int statusCode = response.getStatus();
String body = response.getBody().toString();
String value = response.getBody().getObject().getString("id");
```

---

## 🥒 Steps Disponibles

### Configuración de Endpoint

```gherkin
Given el host "https://api.example.com" mas el contexto "/api/v1/users"
And establezco el base URL como "https://api.qa.example.com"
```

### Headers

```gherkin
And agrego el header "Authorization" con valor "Bearer token123"
And agrego el header "Content-Type" con valor "application/json"
And agrego el header "Accept" con valor "application/json"
```

### Body/Payload

```gherkin
# JSON body
And agrego el request
  """
  {
    "username": "john.doe",
    "email": "john@example.com",
    "active": true
  }
  """

# Desde archivo
And cargo el request desde el archivo "templates/create-user.json"
```

### Ejecución

```gherkin
When ejecuto la consulta con el metodo "GET"
When ejecuto la consulta con el metodo "POST"
When ejecuto la consulta con el metodo "PUT"
When ejecuto la consulta con el metodo "DELETE"
When ejecuto la consulta con el metodo "PATCH"
```

### Validaciones de Status

```gherkin
Then valido que el codigo de respuesta del servicio sea 200
Then valido que el codigo de respuesta del servicio sea 201
Then valido que el codigo de respuesta del servicio sea 400
Then valido que el codigo de respuesta del servicio sea 401
Then valido que el codigo de respuesta del servicio sea 404
Then valido que el codigo de respuesta del servicio sea 500
```

### Validaciones de Response

```gherkin
# Validar campo existe
Then valido que el response contenga el campo "id"
Then valido que el response contenga el campo "data.user.email"

# Validar valor
Then valido que el campo "status" del response sea "active"
Then valido que el campo "data.count" del response sea "10"

# Validar tipo
Then valido que el campo "id" sea de tipo "number"
Then valido que el campo "email" sea de tipo "string"
Then valido que el campo "active" sea de tipo "boolean"
```

### Extraer y Guardar Datos

```gherkin
# Guardar en ScenarioContext para usar en Web/Mobile
And obtengo el campo "token" del objeto "data" y lo guardo como "authToken"
And obtengo el campo "userId" del objeto "data" y lo guardo como "userId"
And obtengo el campo "email" del response y lo guardo como "userEmail"

# Usar variable guardada
And agrego el header "Authorization" con valor "Bearer {authToken}"
```

### Validaciones JSON Schema

```gherkin
Then valido que el response cumpla con el schema "user-schema.json"
```

---

## 💡 Ejemplos Completos

### Ejemplo 1: Login y Crear Recurso

```gherkin
Feature: Gestión de órdenes - API Banking

  Scenario: Login y crear orden de transferencia
    # 1. Autenticación
    Given el host "https://api.banking.com" mas el contexto "/auth/login"
    And agrego el header "Content-Type" con valor "application/json"
    And agrego el request
      """
      {
        "username": "john.doe",
        "password": "SecurePass123!"
      }
      """
    When ejecuto la consulta con el metodo "POST"
    Then valido que el codigo de respuesta del servicio sea 200
    And obtengo el campo "token" del objeto "data" y lo guardo como "authToken"
    
    # 2. Crear orden de transferencia
    Given el host "https://api.banking.com" mas el contexto "/transfers"
    And agrego el header "Authorization" con valor "Bearer {authToken}"
    And agrego el header "Content-Type" con valor "application/json"
    And agrego el request
      """
      {
        "fromAccount": "123456789",
        "toAccount": "987654321",
        "amount": 1000.50,
        "currency": "USD",
        "description": "Payment for services"
      }
      """
    When ejecuto la consulta con el metodo "POST"
    Then valido que el codigo de respuesta del servicio sea 201
    And valido que el response contenga el campo "transferId"
    And valido que el campo "status" del response sea "pending"
    And obtengo el campo "transferId" del response y lo guardo como "transferId"
```

### Ejemplo 2: CRUD Completo

```gherkin
Feature: CRUD de Vehículos - API

  Background:
    Given el host "https://api.vehicles.com"
    And agrego el header "Content-Type" con valor "application/json"
    And agrego el header "Authorization" con valor "Bearer token123"

  Scenario: Crear, leer, actualizar y eliminar vehículo
    # CREATE
    Given establezco el contexto "/vehicles"
    And agrego el request
      """
      {
        "brand": "Toyota",
        "model": "Corolla",
        "year": 2024,
        "color": "Blue"
      }
      """
    When ejecuto la consulta con el metodo "POST"
    Then valido que el codigo de respuesta del servicio sea 201
    And obtengo el campo "id" del response y lo guardo como "vehicleId"
    
    # READ
    Given establezco el contexto "/vehicles/{vehicleId}"
    When ejecuto la consulta con el metodo "GET"
    Then valido que el codigo de respuesta del servicio sea 200
    And valido que el campo "brand" del response sea "Toyota"
    And valido que el campo "model" del response sea "Corolla"
    
    # UPDATE
    Given establezco el contexto "/vehicles/{vehicleId}"
    And agrego el request
      """
      {
        "color": "Red"
      }
      """
    When ejecuto la consulta con el metodo "PATCH"
    Then valido que el codigo de respuesta del servicio sea 200
    And valido que el campo "color" del response sea "Red"
    
    # DELETE
    Given establezco el contexto "/vehicles/{vehicleId}"
    When ejecuto la consulta con el metodo "DELETE"
    Then valido que el codigo de respuesta del servicio sea 204
```

### Ejemplo 3: Validaciones Complejas

```gherkin
Feature: Validaciones avanzadas de API

  Scenario: Validar estructura y tipos de respuesta
    Given el host "https://api.example.com" mas el contexto "/users/123"
    When ejecuto la consulta con el metodo "GET"
    Then valido que el codigo de respuesta del servicio sea 200
    
    # Validar campos existen
    And valido que el response contenga el campo "id"
    And valido que el response contenga el campo "data.email"
    And valido que el response contenga el campo "data.profile.address"
    
    # Validar tipos
    And valido que el campo "id" sea de tipo "number"
    And valido que el campo "data.email" sea de tipo "string"
    And valido que el campo "data.active" sea de tipo "boolean"
    And valido que el campo "data.roles" sea de tipo "array"
    
    # Validar valores específicos
    And valido que el campo "data.status" del response sea "active"
    And valido que el campo "data.email" del response contenga "@example.com"
    
    # Validar schema
    Then valido que el response cumpla con el schema "user-response-schema.json"
```

---

## 🔗 Integración con Web/Mobile

### Flujo API → Web

```gherkin
Feature: Login API y validar en Web

  Scenario: Obtener token en API y usar en Web
    # API: Login y obtener token
    Given el host "https://api.banking.com" mas el contexto "/auth/login"
    And agrego el header "Content-Type" con valor "application/json"
    And agrego el request
      """
      {"username": "john.doe", "password": "pass123"}
      """
    When ejecuto la consulta con el metodo "POST"
    Then valido que el codigo de respuesta del servicio sea 200
    And obtengo el campo "token" del objeto "data" y lo guardo como "authToken"
    And obtengo el campo "user_full_name" del objeto "data" y lo guardo como "fullName"
    
    # Web: Usar token para acceder
    Given actualizo URL en el navegador "https://banking.com/dashboard"
    When inyecto el token "{authToken}" en localStorage
    And recargo la página
    Then verifico que el elemento "welcomeMessage" contenga el texto "{fullName}"
```

### Flujo Web → API

```gherkin
Feature: Validar datos de Web en Backend

  Scenario: Crear orden en Web y validar en API
    # Web: Crear orden
    Given actualizo URL en el navegador "https://shop.example.com"
    When ingreso el texto "Laptop" en el elemento "productSearch"
    And presiono el botón "addToCart"
    And presiono el botón "checkout"
    And guardo texto del elemento "orderNumber" en variable temporal llamada "orderNumber"
    
    # API: Validar que la orden existe en backend
    Given el host "https://api.shop.example.com" mas el contexto "/orders/{orderNumber}"
    And agrego el header "Authorization" con valor "Bearer admin-token"
    When ejecuto la consulta con el metodo "GET"
    Then valido que el codigo de respuesta del servicio sea 200
    And valido que el campo "status" del response sea "pending"
    And valido que el campo "items[0].product" del response sea "Laptop"
```

---

## ⚠️ Troubleshooting

### Error: "Connection refused"

**Causa:** El servidor API no está disponible.

**Solución:**
```gherkin
# Verificar URL
Given el host "https://api.example.com"  # ← Verificar URL correcta

# Verificar red/VPN si es ambiente interno
```

### Error: "401 Unauthorized"

**Causa:** Token inválido o expirado.

**Solución:**
```gherkin
# 1. Verificar que el token se guardó
And obtengo el campo "token" del objeto "data" y lo guardo como "authToken"

# 2. Verificar que se usa correctamente
And agrego el header "Authorization" con valor "Bearer {authToken}"

# 3. Debug: Ver variables en contexto
# En tu step definition:
Map<String, Object> allData = ScenarioContext.getAllFromAllLayers();
System.out.println("Variables: " + allData);
```

### Error: "JSON Parse Error"

**Causa:** Response no es JSON válido o está vacío.

**Solución:**
```gherkin
# Validar status primero
Then valido que el codigo de respuesta del servicio sea 200

# Loguear response para debug
# En ApiSteps, el response se loguea automáticamente
```

### Error: "Campo no encontrado en response"

**Causa:** Path JSON incorrecto.

**Solución:**
```gherkin
# ✅ CORRECTO
And valido que el response contenga el campo "data.user.email"

# ❌ INCORRECTO
And valido que el response contenga el campo "data user email"

# JSONPath:
# - Usar punto (.) para navegar objetos: "data.user.name"
# - Usar corchetes para arrays: "items[0].name"
```

---

## 📚 Dependencias

| Librería | Versión | Propósito |
|----------|---------|-----------|
| **common** | 1.0.2 | Capa base (logging, context, utils) |
| **Unirest** | 4.4.4 | Cliente HTTP |
| **JSON Path** | 2.8.0 | Query JSON |
| **JSON Schema Validator** | 1.0.73 | Validar schemas |
| **HikariCP** | 5.0.1 | Connection pool para BD |
| **Oracle JDBC** | 21.9.0.0 | Driver Oracle |
| **MySQL Connector** | 8.0.33 | Driver MySQL |
| **Cucumber** | 7.18.0 | BDD Framework |

---

## 🔗 Enlaces Relacionados

- **[🚀 Quick Reference](./QUICK-REFERENCE.md)** - Cheat sheet rápida de steps API
- **[Common README](../common/README.md)** - Capa base del framework
- **[Web Core README](../web-core/README.md)** - Testing Web UI
- **[Framework Guide](../FRAMEWORK-GUIDE.md)** - Guía completa
- **[Troubleshooting](../TROUBLESHOOTING.md)** - Solución de problemas

---

<div align="center">

**[⬆ Volver arriba](#-api-core---framework-de-testing-rest)**

**Versión:** 1.0.2 | **Autor:** Abel Venero | **QA Team - Scotia Bank**

</div>

