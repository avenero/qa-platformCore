# 🚀 API-Core - Quick Reference

> Cheat sheet rápida de los steps más usados de api-core. Para documentación completa, ver [README.md](./README.md).

---

## 📑 Índice Rápido

- [Configuración](#configuración)
- [Headers](#headers)
- [Body/Payload](#bodypayload)
- [Ejecución](#ejecución)
- [Validaciones](#validaciones)
- [Extraer Datos](#extraer-datos)
- [Patrones Comunes](#-patrones-comunes)

---

## Configuración

```gherkin
# Host + contexto
Given el host "https://api.example.com" mas el contexto "/api/v1/users"

# Solo host (cambiar base)
Given establezco el base URL como "https://api.qa.example.com"

# Solo contexto (cambiar endpoint)
Given establezco el contexto "/api/v1/orders"
```

---

## Headers

```gherkin
# Headers comunes
And agrego el header "Content-Type" con valor "application/json"
And agrego el header "Accept" con valor "application/json"

# Autenticación
And agrego el header "Authorization" con valor "Bearer token123"
And agrego el header "Authorization" con valor "Bearer {authToken}"

# API Key
And agrego el header "X-API-Key" con valor "api-key-12345"

# Custom headers
And agrego el header "X-Request-ID" con valor "req-12345"
And agrego el header "X-Tenant-ID" con valor "tenant-abc"
```

---

## Body/Payload

### JSON Inline

```gherkin
And agrego el request
  """
  {
    "username": "john.doe",
    "email": "john@example.com",
    "active": true,
    "roles": ["admin", "user"]
  }
  """
```

### JSON con Variables

```gherkin
And agrego el request
  """
  {
    "token": "{authToken}",
    "userId": "{userId}",
    "amount": 1000.50
  }
  """
```

### Desde Archivo

```gherkin
# Cargar JSON desde archivo
And cargo el request desde el archivo "templates/create-user.json"
And cargo el payload desde "data/order-request.json"
```

---

## Ejecución

```gherkin
# Métodos HTTP
When ejecuto la consulta con el metodo "GET"
When ejecuto la consulta con el metodo "POST"
When ejecuto la consulta con el metodo "PUT"
When ejecuto la consulta con el metodo "PATCH"
When ejecuto la consulta con el metodo "DELETE"

# Sin seguir redirects
When ejecuto la consulta con el metodo "POST" sin redireccion
```

---

## Validaciones

### Status Code

```gherkin
# Códigos comunes
Then valido que el codigo de respuesta del servicio sea 200  # OK
Then valido que el codigo de respuesta del servicio sea 201  # Created
Then valido que el codigo de respuesta del servicio sea 204  # No Content
Then valido que el codigo de respuesta del servicio sea 400  # Bad Request
Then valido que el codigo de respuesta del servicio sea 401  # Unauthorized
Then valido que el codigo de respuesta del servicio sea 403  # Forbidden
Then valido que el codigo de respuesta del servicio sea 404  # Not Found
Then valido que el codigo de respuesta del servicio sea 500  # Server Error
```

### Campos en Response

```gherkin
# Validar campo existe
Then valido que el response contenga el campo "id"
And valido que el response contenga el campo "data.user.email"
And valido que el response contenga el campo "items[0].name"

# Validar campo no existe
Then valido que el response no contenga el campo "password"
```

### Valores

```gherkin
# Valor exacto
Then valido que el campo "status" del response sea "active"
And valido que el campo "data.count" del response sea "10"
And valido que el campo "success" del response sea "true"

# Contiene texto
Then valido que el campo "email" del response contenga "@example.com"
And valido que el campo "message" del response contenga "success"
```

### Tipos de Datos

```gherkin
# Validar tipo
Then valido que el campo "id" sea de tipo "number"
Then valido que el campo "email" sea de tipo "string"
Then valido que el campo "active" sea de tipo "boolean"
Then valido que el campo "roles" sea de tipo "array"
Then valido que el campo "profile" sea de tipo "object"
Then valido que el campo "createdAt" sea de tipo "null"
```

### JSON Schema

```gherkin
# Validar contra schema
Then valido que el response cumpla con el schema "user-schema.json"
And valido el schema del response con "order-response-schema.json"
```

### Headers de Response

```gherkin
# Validar header
Then valido que el header "Content-Type" sea "application/json"
And valido que el header "X-Rate-Limit" contenga "100"
```

---

## Extraer Datos

### Guardar en ScenarioContext

```gherkin
# Extraer y guardar (disponible en Web/Mobile)
And obtengo el campo "token" del objeto "data" y lo guardo como "authToken"
And obtengo el campo "userId" del objeto "data" y lo guardo como "userId"
And obtengo el campo "email" del response y lo guardo como "userEmail"

# Usar en otro step
And agrego el header "Authorization" con valor "Bearer {authToken}"
```

### JSONPath

```gherkin
# Navegar objetos anidados
And obtengo el campo "data.user.profile.email" y lo guardo como "email"

# Arrays
And obtengo el campo "items[0].id" y lo guardo como "firstItemId"
And obtengo el campo "items[0].product.name" y lo guardo como "productName"
```

---

## 🎯 Patrones Comunes

### Login y Obtener Token

```gherkin
Given el host "https://api.example.com" mas el contexto "/auth/login"
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
And valido que el response contenga el campo "token"
And obtengo el campo "token" del objeto "data" y lo guardo como "authToken"
```

### GET con Autenticación

```gherkin
Given el host "https://api.example.com" mas el contexto "/users/123"
And agrego el header "Authorization" con valor "Bearer {authToken}"
And agrego el header "Accept" con valor "application/json"
When ejecuto la consulta con el metodo "GET"
Then valido que el codigo de respuesta del servicio sea 200
And valido que el campo "data.email" del response contenga "@example.com"
```

### POST - Crear Recurso

```gherkin
Given el host "https://api.example.com" mas el contexto "/users"
And agrego el header "Authorization" con valor "Bearer {authToken}"
And agrego el header "Content-Type" con valor "application/json"
And agrego el request
  """
  {
    "name": "John Doe",
    "email": "john@example.com",
    "role": "admin"
  }
  """
When ejecuto la consulta con el metodo "POST"
Then valido que el codigo de respuesta del servicio sea 201
And valido que el response contenga el campo "id"
And obtengo el campo "id" del response y lo guardo como "userId"
```

### PUT - Actualizar Completo

```gherkin
Given el host "https://api.example.com" mas el contexto "/users/{userId}"
And agrego el header "Authorization" con valor "Bearer {authToken}"
And agrego el header "Content-Type" con valor "application/json"
And agrego el request
  """
  {
    "name": "John Doe Updated",
    "email": "john.updated@example.com",
    "role": "admin",
    "active": true
  }
  """
When ejecuto la consulta con el metodo "PUT"
Then valido que el codigo de respuesta del servicio sea 200
And valido que el campo "data.name" del response sea "John Doe Updated"
```

### PATCH - Actualización Parcial

```gherkin
Given el host "https://api.example.com" mas el contexto "/users/{userId}"
And agrego el header "Authorization" con valor "Bearer {authToken}"
And agrego el header "Content-Type" con valor "application/json"
And agrego el request
  """
  {
    "active": false
  }
  """
When ejecuto la consulta con el metodo "PATCH"
Then valido que el codigo de respuesta del servicio sea 200
And valido que el campo "data.active" del response sea "false"
```

### DELETE - Eliminar

```gherkin
Given el host "https://api.example.com" mas el contexto "/users/{userId}"
And agrego el header "Authorization" con valor "Bearer {authToken}"
When ejecuto la consulta con el metodo "DELETE"
Then valido que el codigo de respuesta del servicio sea 204
```

### Validación de Lista

```gherkin
Given el host "https://api.example.com" mas el contexto "/users"
And agrego el header "Authorization" con valor "Bearer {authToken}"
When ejecuto la consulta con el metodo "GET"
Then valido que el codigo de respuesta del servicio sea 200
And valido que el response contenga el campo "data"
And valido que el campo "data" sea de tipo "array"
And valido que el campo "data[0].email" del response contenga "@example.com"
```

### Manejo de Errores

```gherkin
# Validar error 400
Given el host "https://api.example.com" mas el contexto "/users"
And agrego el header "Content-Type" con valor "application/json"
And agrego el request
  """
  {
    "email": "invalid-email"
  }
  """
When ejecuto la consulta con el metodo "POST"
Then valido que el codigo de respuesta del servicio sea 400
And valido que el campo "error.message" del response contenga "Invalid email"

# Validar error 401
Given el host "https://api.example.com" mas el contexto "/protected"
When ejecuto la consulta con el metodo "GET"
Then valido que el codigo de respuesta del servicio sea 401
And valido que el campo "error" del response sea "Unauthorized"
```

---

## 🔗 Integración API → Web

```gherkin
# 1. API: Obtener datos
Given el host "https://api.example.com" mas el contexto "/auth/login"
And agrego el header "Content-Type" con valor "application/json"
And agrego el request
  """
  {"username": "john", "password": "pass"}
  """
When ejecuto la consulta con el metodo "POST"
Then valido que el codigo de respuesta del servicio sea 200
And obtengo el campo "token" del objeto "data" y lo guardo como "authToken"
And obtengo el campo "user_name" del objeto "data" y lo guardo como "userName"

# 2. Web: Usar datos de API
Given actualizo URL en el navegador "https://app.example.com"
When inyecto token "{authToken}" en localStorage
And recargo la página
Then verifico que el texto en "welcome" contenga "{userName}"
```

---

## ⚠️ Anti-Patrones (Evitar)

### ❌ No validar status code

```gherkin
# ❌ MAL
When ejecuto la consulta con el metodo "POST"
And obtengo el campo "token" del response...

# ✅ BIEN
When ejecuto la consulta con el metodo "POST"
Then valido que el codigo de respuesta del servicio sea 200
And obtengo el campo "token" del response...
```

### ❌ Headers incorrectos

```gherkin
# ❌ MAL - Falta Content-Type
And agrego el request
  """
  {"name": "John"}
  """
When ejecuto la consulta con el metodo "POST"

# ✅ BIEN
And agrego el header "Content-Type" con valor "application/json"
And agrego el request
  """
  {"name": "John"}
  """
When ejecuto la consulta con el metodo "POST"
```

### ❌ No validar campos antes de extraer

```gherkin
# ❌ MAL
When ejecuto la consulta con el metodo "POST"
And obtengo el campo "token" del response...

# ✅ BIEN
When ejecuto la consulta con el metodo "POST"
Then valido que el codigo de respuesta del servicio sea 200
And valido que el response contenga el campo "token"
And obtengo el campo "token" del response...
```

---

## 💡 Tips

1. **Siempre valida status code** antes de extraer datos
2. **Usa variables** para tokens y IDs reutilizables
3. **Valida tipos de datos** para asegurar schema correcto
4. **Guarda en ScenarioContext** para compartir con Web/Mobile
5. **Usa JSON Schema** para validaciones complejas
6. **Log requests/responses** están automáticos
7. **Headers de autenticación** pueden usar variables: `{authToken}`

---

## 📚 Status Codes de Referencia

| Código | Significado | Cuándo Usar |
|--------|-------------|-------------|
| **200** | OK | GET, PUT exitoso |
| **201** | Created | POST exitoso |
| **204** | No Content | DELETE exitoso |
| **400** | Bad Request | Validación falló |
| **401** | Unauthorized | Sin autenticación |
| **403** | Forbidden | Sin permisos |
| **404** | Not Found | Recurso no existe |
| **409** | Conflict | Recurso duplicado |
| **422** | Unprocessable | Validación semántica |
| **500** | Server Error | Error del servidor |

---

## 🔗 Enlaces

- **[README.md](./README.md)** - Documentación completa de api-core
- **[Troubleshooting](../TROUBLESHOOTING.md)** - Solución de problemas
- **[Framework Guide](../FRAMEWORK-GUIDE.md)** - Guía maestra del framework

---

<div align="center">

**[⬆ Volver arriba](#-api-core---quick-reference)**

**Para documentación completa:** [README.md](./README.md)

</div>

