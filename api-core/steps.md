# 📋 ANÁLISIS DE STEPS - API-CORE

> **Documento de Análisis Técnico**  
> **Proyecto:** QA Scotia Frameworks - Refactorización Fase 1  
> **Módulo:** api-core  
> **Fecha:** 11 de Noviembre 2025  
> **Responsable:** Abel Venero

---

## 📊 RESUMEN EJECUTIVO

### **Estado Actual:**
- ✅ **15 steps funcionales** refactorizados con nueva arquitectura
- 🔄 **1 step duplicado** identificado (headers) - consolidación requerida
- 🔴 **40 steps con errores** - requieren refactorización
- 💤 **15 steps comentados** - postponer (DB e integraciones)
- 🔴 **94 errores de compilación** detectados

### **Hallazgos Clave:**
1. ✅ **21 métodos ya existen en `common`** y solo requieren refactorización
2. ⚠️ **26 métodos faltantes** requieren implementación en `common`
3. 🎯 **~72% de errores se pueden resolver** con refactorización directa (Fases 1-2)
4. 🔧 **9 métodos de autenticación específicos** requieren decisión arquitectónica

### **Prioridades Inmediatas:**
1. 🔴 **CRÍTICO:** Decidir estrategia para métodos de autenticación específicos
2. 🟡 **ALTA:** Refactorizar steps con métodos existentes (~30 minutos)
3. 🟡 **ALTA:** Implementar métodos faltantes en `common` (~3 horas)
4. 🟢 **MEDIA:** Consolidar headers duplicados (~15 minutos)
5. 🟢 **BAJA:** Implementar wait utilities y URL parsing (~1 hora)

### **Tiempo Estimado Total:** ~5 horas de desarrollo

---

## 📑 TABLA DE CONTENIDOS

1. [🎯 Objetivo](#-objetivo)
2. [📊 Clasificación de Steps](#-clasificación-de-steps)
   - [Grupo 1: Steps Funcionales](#-grupo-1-steps-ya-funcionales-refactorizados-con-nueva-arquitectura)
   - [Grupo 2: Steps de Headers - Consolidación](#-grupo-2-steps-de-headers---consolidación-necesaria)
   - [Grupo 3: Steps con Errores](#-grupo-3-steps-con-errores---métodos-faltantes-en-common)
   - [Grupo 4: Steps Comentados](#-grupo-4-steps-comentados-dejamos-para-después)
3. [🔧 Métodos Faltantes en Common](#-métodos-faltantes-en-common---resumen-ejecutivo)
4. [📐 Estrategia de Refactorización](#-estrategia-de-refactorización)
5. [🎯 Próximos Pasos Inmediatos](#-próximos-pasos-inmediatos)
6. [🏗️ Arquitectura - Flujo de Consumo](#️-arquitectura---flujo-de-consumo)
7. [✅ Principios Arquitectónicos Aplicados](#-principios-arquitectónicos-aplicados)
8. [📚 Dependencias Necesarias](#-dependencias-necesarias)
9. [🚨 Puntos de Atención](#-puntos-de-atención)
10. [📊 Métricas del Análisis](#-métricas-del-análisis)
11. [💡 Ejemplos Concretos de Refactorización](#-ejemplos-concretos-de-refactorización)
12. [🎯 Resumen de Estrategias por Tipo](#-resumen-de-estrategias-por-tipo-de-step)
13. [🔴 Resumen de Errores de Compilación](#-resumen-de-errores-de-compilación-actuales)
14. [🎓 Conclusiones](#-conclusiones)

---

## 🎯 OBJETIVO
Analizar los steps existentes en `ApiSteps.java` para:
1. Identificar métodos faltantes en la arquitectura `common`
2. Consolidar steps duplicados (especialmente headers)
3. Mapear cada step a la implementación correcta en `common`
4. Mantener contratos de steps sin cambios
5. Asegurar desacoplamiento total entre capas

---

## 📊 CLASIFICACIÓN DE STEPS

### ✅ **GRUPO 1: STEPS YA FUNCIONALES (Refactorizados con nueva arquitectura)**

| Step | Responsabilidad | Usa desde Common |
|------|-----------------|------------------|
| `@Given("configuro el endpoint usando {string} del archivo {string}")` | Configurar host desde archivo | `ConfigurationProvider`, `HttpClient.setHost()`, `DataUtilities` |
| `@Given("establezco el host base como {word}")` | Configurar host directo | `HttpClient.setHost()`, `DataUtilities` |
| `@Given("agrego autenticación Client Credentials")` | Autenticación OAuth2 | `AuthenticationService.getClientCredentialsToken()`, `HttpClient.addHeader()` |
| `@Given("agrego autenticación Bearer para RUT {word}")` | Autenticación Bearer con identificador | `AuthenticationService.getBearerTokenForIdentifier()`, `HttpClient.addHeader()` |
| `@Given("agrego el token personalizado {word}")` | Token custom | `HttpClient.addHeader()`, `DataUtilities` |
| `@Given("agrego autenticación básica con usuario {string} y password {string}")` | Autenticación Basic | `HttpClient.addHeader()`, Base64, `DataUtilities` |
| `@And("agrego el header {word} con valor {word}")` | Agregar header HTTP | `HttpClient.addHeader()`, `DataUtilities` |
| `@And("agrego el parámetro de consulta {word} con valor {word}")` | Agregar query parameter | `HttpClient.addQueryParam()`, `DataUtilities` |
| `@Given("establezco el cuerpo de la petición como")` | Body raw string | `HttpClient.setBody()`, `DataUtilities` |
| `@Given("establezco el cuerpo JSON con los siguientes datos")` | Body JSON desde tabla | `HttpClient.setBody()`, `HttpClient.addHeader()`, `DataUtilities`, ObjectMapper |
| `@When("ejecuto una petición {word} al endpoint {word}")` | Ejecutar petición HTTP | `HttpClient.get/post/put/delete/patch()`, `DataUtilities` |
| `@Then("valido que el código de respuesta sea {word}")` | Validar status code | `HttpClient.getLastResponse()`, `ValidationUtilities.validateStatusCode()` |
| `@Then("valido que la respuesta contenga el texto {word}")` | Validar texto en body | `HttpClient.getLastResponse()`, `FrameworkBusinessException` |
| `@Given("almaceno el valor {word} como {word}")` | Guardar variable | `DataUtilities.storeValue()`, `DataUtilities.replaceVariables()` |
| `@Then("muestro la información de la última petición")` | Debug de request/response | `HttpClient.getLastResponse()`, `TestLogger` |

---

### 🔄 **GRUPO 2: STEPS DE HEADERS - CONSOLIDACIÓN NECESARIA**

#### 🚨 **Problema Identificado:** Múltiples steps hacen lo mismo (agregar headers)

| Step Actual | ¿Es Redundante? | Acción Recomendada |
|-------------|----------------|-------------------|
| `@And("agrego el header {word} con valor {word}")` | ❌ **MANTENER** - Step genérico principal | **Principal** - Ya funcional |
| `@Given("agrego el header {string} con el valor {string}")` | ✅ **SÍ** - Duplicado exacto con comillas diferentes | **Consolidar** - Usar el principal |

#### 📝 **Decisión de Consolidación:**

**STEP PRINCIPAL A MANTENER:**
```gherkin
@And("agrego el header {word} con valor {word}")
```

**RAZONES:**
1. ✅ Ya está refactorizado con la nueva arquitectura
2. ✅ Usa `{word}` que es más flexible (no requiere comillas)
3. ✅ Tiene logging y manejo de errores robusto
4. ✅ Soporta reemplazo de variables con `DataUtilities.replaceVariables()`

**STEP DUPLICADO A ELIMINAR (después de migración):**
```gherkin
@Given("agrego el header {string} con el valor {string}")
```

**ESTRATEGIA DE MIGRACIÓN:**
- Mantener ambos temporalmente durante refactorización
- El duplicado debe INTERNAMENTE llamar al principal (delegación)
- Después de migrar features, eliminar el duplicado

**Implementación temporal del duplicado:**
```java
@Given("agrego el header {string} con el valor {string}")
public void agregoElHeaderKeyConElValorValue(String arg0, String arg1) {
    // DELEGACIÓN al step principal (evita duplicación de lógica)
    agregoElHeaderConValor(arg0, arg1);
}
```

---

### 🔴 **GRUPO 3: STEPS CON ERRORES - MÉTODOS FALTANTES EN COMMON**

#### **3.1. AUTENTICACIÓN - Métodos de tokens específicos**

| Step | Método Llamado (No Existe) | Implementación Requerida en Common |
|------|---------------------------|-----------------------------------|
| `@Given("agrego el token requerido del tipo Client-Credentials")` | `getTokenCC()` | ✅ Ya existe: `AuthenticationService.getClientCredentialsToken()` - **Solo necesita refactorización** |
| `@Given("agrego el token requerido del tipo Bearer-Token para el rut {string}")` | `getTokenRut(arg0)` | ✅ Ya existe: `AuthenticationService.getBearerTokenForIdentifier()` - **Solo necesita refactorización** |
| `@Given("agrego el token de cuatro pasos al rut {string} y clave {string}")` | `getJwtToken(arg0, arg1)` | ❌ **FALTA** - Requiere método en `AuthenticationService` |
| `@Given("agrego el token requerido del tipo digital mortgage")` | `getTokenDigitalMortgage()` | ❌ **FALTA** - Requiere método en `AuthenticationService` |
| `@Given("agrego el token requerido del tipo baas latam en {string}")` | `getTokenBaasLatam(arg0)` | ❌ **FALTA** - Requiere método en `AuthenticationService` |
| `@Given("agrego el token requerido del tipo Client-Credentials token-generator")` | `getTokenCCTokenGenerator()` | ❌ **FALTA** - Requiere método en `AuthenticationService` |
| `@Given("agrego el token requerido del tipo en un paso {string}")` | `getTokenUP(arg0)` | ❌ **FALTA** - Requiere método en `AuthenticationService` |
| `@Given("agrego el opaque token al rut {string} y clave {string}")` | `getOpaqueToken(arg0, arg1)` | ❌ **FALTA** - Requiere método en `AuthenticationService` |
| `@Given("agrego el Opaque sin Bearer con Authorization del rut {string} y clave {string}")` | `getOpaqueToken(arg0, arg1)` | ❌ **FALTA** - Mismo método anterior |
| `@Given("agrego el nuevo opaque token")` | `getUrlTokenOpaqueDatalab()` | ❌ **FALTA** - Requiere método en `AuthenticationService` |
| `@Given("agrego el token cuatro pasos y el opaque token al rut {string} y clave {string}")` | `getJwtToken()` + `getOpaqueToken()` | ❌ **FALTA** - Combinación de dos tokens |
| `@Given("agrego el token Opaque sin Bearer del rut {string} y clave {string}")` | `getOpaqueToken(arg0, arg1)` | ❌ **FALTA** - Mismo método anterior |
| `@Given("agrego el token requerido de tipo Dal Token")` | `getTokenDal()` | ❌ **FALTA** - Requiere método en `AuthenticationService` |
| `@Given("agrego el token requerido del tipo Token Moppa para el rut {string}")` | `getTokenMoppa(arg0)` | ❌ **FALTA** - Requiere método en `AuthenticationService` |

#### **3.2. CONFIGURACIÓN - Métodos de fields y configuración**

| Step | Método Llamado (No Existe) | Implementación Requerida en Common |
|------|---------------------------|-----------------------------------|
| `@Given("agrego el field {string} con el valor {string}")` | `setFields(arg0, arg1)` | ✅ Ya existe: `HttpClient.addField()` - **Solo necesita refactorización** |
| `@Given("agrego el request")` | `body = replaceData(arg0)` | ✅ Ya existe: `HttpClient.setBody()` + `DataUtilities.replaceVariables()` - **Solo necesita refactorización** |
| `@Given("el parametro {string} remplazo el valor {string}")` | `setJsonParameters(arg0, arg1)` | ❌ **FALTA** - Requiere utilidad JSON en `DataUtilities` |
| `@Given("agrego el queryparam {string} con el valor {string}")` | `setQueryParams(arg0, arg1)` | ✅ Ya existe: `HttpClient.addQueryParam()` - **Solo necesita refactorización** |
| `@Given("establezco la key {string} con el valor {string}")` | `dataJson.put(arg0, arg1)` | ❌ **FALTA** - Requiere contexto JSON en `DataUtilities` |

#### **3.3. ALMACENAMIENTO Y EXTRACCIÓN DE DATOS**

| Step | Método Llamado (No Existe) | Implementación Requerida en Common |
|------|---------------------------|-----------------------------------|
| `@Given("el resultado almaceno el valor de {string}")` | `getJsonData(arg0)` | ❌ **FALTA** - Requiere extracción JSON con JsonPath en `DataUtilities` |
| `@Given("el resultado almaceno el valor que está dentro de la estructura {string} en {string}")` | `saveValueFromStructure(arg0, arg1)` | ❌ **FALTA** - Requiere navegación JSON anidada en `DataUtilities` |
| `@Given("el resultado de la ejecucion del servicio, almaceno el valor del header {string} en la variable {string}")` | `captureHeader(header, variableName)` | ❌ **FALTA** - Requiere método en `HttpClient` o `DataUtilities` |
| `@Given("la url {string}, capturo el valor de {string} y lo guardo en la variable {string}")` | `captureQueryParam(url, value, variableName)` | ❌ **FALTA** - Requiere parser de URL en `DataUtilities` |

#### **3.4. EJECUCIÓN DE PETICIONES**

| Step | Método Llamado (No Existe) | Implementación Requerida en Common |
|------|---------------------------|-----------------------------------|
| `@When("ejecuto la consulta con el metodo {string}")` | `executeWS(arg0, true)` | ✅ Ya existe: `HttpClient.get/post/etc()` - **Solo necesita refactorización** |
| `@When("ejecuto la consulta con el metodo {string} sin redireccion")` | `executeWS(arg0, false)` | ❌ **FALTA** - Requiere configuración de redirect en `HttpClient` |
| `@When("recorro la respuesta buscando que se cumpla que {string} sea igual a {string} y almaceno el valor de {string}")` | `getObjectInArrayResponse(arg0, arg1, arg2)` | ❌ **FALTA** - Requiere búsqueda en arrays JSON en `DataUtilities` |
| `@When("espero {string} segundos")` | `waitForSeconds(arg0)` | ❌ **FALTA** - Requiere utilidad de espera en `DataUtilities` o nueva clase `WaitUtilities` |

#### **3.5. VALIDACIONES**

| Step | Método Llamado (No Existe) | Implementación Requerida en Common |
|------|---------------------------|-----------------------------------|
| `@Then("valido que el codigo de respuesta del servicio sea {int}")` | `getHttpStatus()`, `getBodyResponse()` | ✅ Ya existe: `HttpClient.getLastResponse()` - **Solo necesita refactorización** |
| `@Then("valido que el status del response sea {string}")` | `getStatusHealth()` | ❌ **FALTA** - Requiere extracción de campo específico en `ValidationUtilities` |
| `@Then("valido que el valor almacenado en el campo {string} sea {string}")` | `isFieldEquals(arg0, arg1)` | ❌ **FALTA** - Requiere validación de contexto en `ValidationUtilities` |
| `@Then("valido que el cuerpo de la respuesta sea")` | `isEqualJson(arg0)` | ❌ **FALTA** - Requiere comparación JSON en `ValidationUtilities` |
| `@Then("valido que el valor dentro de la estructura {string} sea {string}")` | `validateJson(arg0, arg1)` | ❌ **FALTA** - Requiere validación JsonPath en `ValidationUtilities` |
| `@Then("valido que el cuerpo de la respuesta tenga el siguiente esquema")` | `validateJsonSchema(arg0)` | ❌ **FALTA** - Requiere validación JSON Schema en `ValidationUtilities` |
| `@Then("valido que el cuerpo de la respuesta contenga la siguiente cadena")` | `validateStringInResponse(arg0)` | ❌ **FALTA** - Requiere búsqueda de texto en `ValidationUtilities` |
| `@Then("valido que el cuerpo de la respuesta no contenga la siguiente cadena")` | `validateStringNotInResponse(arg0)` | ❌ **FALTA** - Requiere búsqueda de texto negativa en `ValidationUtilities` |
| `@Then("valido que el valor de la variable {string} sea {string}")` | `replaceData()` con validación | ❌ **FALTA** - Requiere validación de variables en `ValidationUtilities` |

---

### 💤 **GRUPO 4: STEPS COMENTADOS (Dejamos para después)**

| Step | Funcionalidad | Decisión |
|------|--------------|----------|
| `@When("actualizo los valores en la base de datos DB2...")` | Operaciones DB | ⏸️ **POSTPONER** - Requiere integración con DB |
| `@When("consulto la base de datos...")` | Consultas DB | ⏸️ **POSTPONER** - Ya existe `DatabaseService` en common |
| `@When("elimino uno o mas registros...")` | DELETE DB | ⏸️ **POSTPONER** - Ya existe `DatabaseService` |
| `@When("inserto uno o mas registros...")` | INSERT DB | ⏸️ **POSTPONER** - Ya existe `DatabaseService` |
| `@When("adjunto un archivo al scenario...")` | Adjuntar evidencia | ⏸️ **POSTPONER** - Requiere análisis de Cucumber hooks |
| Otros steps de validación DB y Jira | Integraciones específicas | ⏸️ **POSTPONER** - Analizar después de completar HTTP |

---

## 🔧 MÉTODOS FALTANTES EN COMMON - RESUMEN EJECUTIVO

### **A. AuthenticationService (Interface) - Métodos a Agregar**

```java
// TOKENS ESPECÍFICOS DE SCOTIA
String getJwtTokenWithCredentials(String username, String password) throws FrameworkBusinessException;
String getDigitalMortgageToken() throws FrameworkBusinessException;
String getBaasLatamToken(String environment) throws FrameworkBusinessException;
String getTokenGeneratorClientCredentials() throws FrameworkBusinessException;
String getOneStepToken(String identifier) throws FrameworkBusinessException;
String getOpaqueToken(String username, String password) throws FrameworkBusinessException;
String getDatalabOpaqueToken() throws FrameworkBusinessException;
String getDalToken() throws FrameworkBusinessException;
String getMoppaToken(String identifier) throws FrameworkBusinessException;
```

### **B. HttpClient (Interface) - Métodos a Agregar**

```java
// CONFIGURACIÓN DE REDIRECCIÓN
void setFollowRedirects(boolean followRedirects);
boolean isFollowingRedirects();

// ACCESO A HEADERS DE RESPUESTA
Map<String, String> getLastResponseHeaders();
String getLastResponseHeader(String headerName);
```

### **C. DataUtilities (Clase) - Métodos a Agregar**

#### ✅ **Métodos Ya Existentes (Usar directamente):**
```java
// JSON PATH NAVIGATION - YA EXISTE
Object getJsonParameter(String jsonBody, String fieldPath) throws FrameworkBusinessException; ✅
boolean hasJsonField(String jsonBody, String fieldPath); ✅

// VARIABLE STORAGE - YA EXISTE
void storeValue(String key, Object value); ✅
String getValue(String key); ✅
void clearVariables(); ✅
String replaceVariables(String text); ✅

// VALIDATION - YA EXISTE
boolean isValidJson(String json); ✅
```

#### ❌ **Métodos Faltantes (Implementar):**
```java
// JSON MANIPULATION
String setJsonValue(String jsonPath, Object value, String jsonContent) throws FrameworkTechnicalException;
String mergeJson(String baseJson, String overrideJson) throws FrameworkTechnicalException;

// JSON SEARCH IN ARRAYS
Object findInJsonArray(String arrayJsonPath, String searchField, Object searchValue, String resultField, String jsonContent) throws FrameworkTechnicalException;

// URL PARSING
Map<String, String> parseQueryParams(String url) throws FrameworkTechnicalException;
String extractQueryParam(String url, String paramName) throws FrameworkTechnicalException;

// WAIT UTILITIES
void waitSeconds(int seconds);
void waitMilliseconds(long milliseconds);
```

### **D. ValidationUtilities (Clase) - Métodos a Agregar**

#### ✅ **Métodos Ya Existentes (Usar directamente):**
```java
// HTTP VALIDATION - YA EXISTE
void validateStatusCode(HttpResponse response, int expectedStatus) throws FrameworkBusinessException; ✅
void validateStatusCodeRange(HttpResponse response, int minStatus, int maxStatus) throws FrameworkBusinessException; ✅
void validateHeaderExists(HttpResponse response, String headerName) throws FrameworkBusinessException; ✅
void validateHeaderValue(HttpResponse response, String headerName, String expectedValue) throws FrameworkBusinessException; ✅
void validateHeaderContains(HttpResponse response, String headerName, String expectedSubstring) throws FrameworkBusinessException; ✅

// JSON VALIDATION - YA EXISTE
void validateJsonPath(HttpResponse response, String jsonPath, Object expectedValue) throws FrameworkBusinessException; ✅
void validateJsonPathExists(HttpResponse response, String jsonPath) throws FrameworkBusinessException; ✅
void validateJsonType(HttpResponse response, String jsonPath, String expectedType) throws FrameworkBusinessException; ✅

// PATTERN VALIDATION - YA EXISTE
void validatePattern(String value, Pattern pattern) throws FrameworkBusinessException; ✅
void validateEmail(String email) throws FrameworkBusinessException; ✅
void validateUrl(String url) throws FrameworkBusinessException; ✅
void validateUUID(String uuid) throws FrameworkBusinessException; ✅
```

#### ❌ **Métodos Faltantes (Implementar):**
```java
// JSON VALIDATION COMPLEJA
void validateJsonEquals(String expectedJson, String actualJson) throws FrameworkBusinessException;
void validateJsonSchema(String jsonSchema, String actualJson) throws FrameworkBusinessException;

// STRING VALIDATION IN RESPONSES
void validateContainsText(HttpResponse response, String expectedText) throws FrameworkBusinessException;
void validateNotContainsText(HttpResponse response, String unexpectedText) throws FrameworkBusinessException;

// VARIABLE VALIDATION
void validateStoredValue(String variableName, Object expectedValue) throws FrameworkBusinessException;

// HEALTH CHECK SPECIFIC
String extractHealthStatus(HttpResponse response) throws FrameworkBusinessException;
void validateHealthStatus(String expectedStatus, HttpResponse response) throws FrameworkBusinessException;
```

---

## 📐 ESTRATEGIA DE REFACTORIZACIÓN

### **FASE 1: Consolidación de Headers ✅**
1. ✅ Mantener: `@And("agrego el header {word} con valor {word}")`
2. ✅ Deprecar temporalmente: `@Given("agrego el header {string} con el valor {string}")`
3. ✅ Implementar delegación del duplicado al principal
4. ⏳ Migrar features que usan el duplicado
5. ⏳ Eliminar step duplicado después de migración

### **FASE 2: Refactorización de Steps Simples ⏳**
Adaptar steps que solo necesitan cambios de nombres de métodos:
- `setFields()` → `HttpClient.addField()`
- `setQueryParams()` → `HttpClient.addQueryParam()`
- `executeWS()` → `HttpClient.get/post/etc()`
- `getTokenCC()` → `AuthenticationService.getClientCredentialsToken()`
- `getTokenRut()` → `AuthenticationService.getBearerTokenForIdentifier()`

### **FASE 3: Implementación de Métodos Faltantes - Autenticación ⏳**
1. Agregar métodos de tokens específicos en `AuthenticationService`
2. Implementar en `BaseAuthenticationManager`
3. Considerar uso de `ConfigurationProvider` para endpoints de autenticación

### **FASE 4: Implementación de Métodos Faltantes - JSON ⏳**
1. Agregar métodos JsonPath en `DataUtilities`
2. Usar biblioteca `com.jayway.jsonpath:json-path` (ya está en Gradle)
3. Implementar validaciones JSON en `ValidationUtilities`

### **FASE 5: Implementación de Métodos Faltantes - Utilidades ⏳**
1. Agregar wait utilities en `DataUtilities`
2. Agregar URL parsing en `DataUtilities`
3. Implementar configuración de redirects en `HttpClient`

### **FASE 6: Testing y Validación ⏳**
1. Compilar y verificar sin errores
2. Ejecutar features existentes
3. Validar cobertura de funcionalidades
4. Documentar cambios en README

---

## 🎯 PRÓXIMOS PASOS INMEDIATOS

### **1️⃣ ALTA PRIORIDAD - Consolidar Headers**
- [x] Identificar step principal
- [x] Identificar step duplicado
- [ ] Implementar delegación temporal
- [ ] Migrar features (fuera de este análisis)
- [ ] Eliminar duplicado

### **2️⃣ ALTA PRIORIDAD - Refactorizar Steps Simples**
```java
// Ejemplo de refactorización:
@Given("agrego el field {string} con el valor {string}")
public void agregoElFieldKeyConElValorValue(String key, String value) {
    String processedKey = DataUtilities.replaceVariables(key);
    String processedValue = DataUtilities.replaceVariables(value);
    httpClient.addField(processedKey, processedValue);
    TestLogger.logDebug("API_STEPS_CONFIG", 
        String.format("Field agregado: %s = %s", processedKey, processedValue), null);
}
```

### **3️⃣ MEDIA PRIORIDAD - Agregar Métodos JSON**
- [ ] Agregar `extractJsonValue()` en `DataUtilities`
- [ ] Agregar `validateJsonPath()` en `ValidationUtilities`
- [ ] Agregar `validateJsonSchema()` en `ValidationUtilities`

### **4️⃣ MEDIA PRIORIDAD - Agregar Métodos Autenticación**
- [ ] Definir contratos en `AuthenticationService`
- [ ] Implementar en `BaseAuthenticationManager`
- [ ] Configurar endpoints en archivos de configuración

### **5️⃣ BAJA PRIORIDAD - Utilidades Adicionales**
- [ ] Wait utilities
- [ ] URL parsing
- [ ] Redirect configuration

---

## 🏗️ ARQUITECTURA - FLUJO DE CONSUMO

### **Ejemplo: Step de Header Consolidado**

```
┌─────────────────────────────────────────────────────────┐
│ FEATURE (Módulo Consumidor - scotia-api)               │
│ Given agrego el header "Content-Type" con valor        │
│       "application/json"                                 │
└────────────────────┬────────────────────────────────────┘
                     │ invoca
                     ▼
┌─────────────────────────────────────────────────────────┐
│ API-CORE (ApiSteps.java)                                │
│ @And("agrego el header {word} con valor {word}")       │
│ public void agregoElHeaderConValor(String h, String v)  │
│ {                                                        │
│   String pValue = DataUtilities.replaceVariables(v);    │ ◄─── Usa common
│   httpClient.addHeader(h, pValue);                      │ ◄─── Usa common
│   TestLogger.logDebug(...);                             │ ◄─── Usa common
│ }                                                        │
└────────────────────┬────────────────────────────────────┘
                     │ delega a
                     ▼
┌─────────────────────────────────────────────────────────┐
│ COMMON (Interfaces + Implementaciones)                  │
│                                                          │
│ HttpClient (Interface)                                  │
│ ├─ void addHeader(String key, String value)             │
│                                                          │
│ BaseHttpClient (Implementation)                         │
│ ├─ @Override                                            │
│ ├─ public void addHeader(String key, String value) {    │
│ │    headers.put(key, value);                           │
│ │    TestLogger.logDebug("HTTP_CLIENT_CONFIG", ...);    │
│ │  }                                                     │
│                                                          │
│ DataUtilities                                           │
│ ├─ public static String replaceVariables(String text)   │
│ │    // {{variable}} → valor real                       │
│                                                          │
│ TestLogger                                              │
│ ├─ public static void logDebug(String component, ...)   │
└─────────────────────────────────────────────────────────┘
```

---

## ✅ PRINCIPIOS ARQUITECTÓNICOS APLICADOS

### **1. Desacoplamiento Total**
- ✅ `common` no conoce a `api-core`, `web-core`, `mobile-core`
- ✅ `api-core` solo conoce interfaces y clases públicas de `common`
- ✅ Módulos consumidores solo conocen steps de `api-core`

### **2. Composición sobre Herencia**
- ✅ `ApiSteps` NO extiende `BaseTest` (eliminado)
- ✅ `ApiSteps` COMPONE `HttpClient`, `AuthenticationService`, `ConfigurationProvider`
- ✅ Cada step delega responsabilidades a `common`

### **3. Reutilización por Contrato**
- ✅ Interfaces definen contratos (`HttpClient`, `AuthenticationService`)
- ✅ Implementaciones base proveen funcionalidad (`BaseHttpClient`, `BaseAuthenticationManager`)
- ✅ Frameworks específicos pueden extender o reemplazar implementaciones

### **4. Independencia de Dominio**
- ✅ `common` no conoce "Scotia", "Santander", "Itaú"
- ✅ Métodos genéricos: `getBearerTokenForIdentifier()` en lugar de `getTokenRut()`
- ✅ Configuración específica de dominio en módulos consumidores

### **5. Single Responsibility**
- ✅ `HttpClient` → Maneja HTTP
- ✅ `AuthenticationService` → Maneja autenticación
- ✅ `DataUtilities` → Maneja datos y variables
- ✅ `ValidationUtilities` → Maneja validaciones
- ✅ `ApiSteps` → Orquesta componentes para Cucumber

---

## 📚 DEPENDENCIAS NECESARIAS

### **Librerías ya disponibles en `common/build.gradle`:**
- ✅ `com.konghq:unirest-java` - Cliente HTTP
- ✅ `com.fasterxml.jackson.core:jackson-databind` - JSON processing
- ✅ `com.jayway.jsonpath:json-path` - JsonPath navegación
- ✅ `io.rest-assured:json-schema-validator` - JSON Schema validation
- ✅ `ch.qos.logback:logback-classic` - Logging

### **Verificar si están disponibles:**
- ⏳ `org.skyscreamer:jsonassert` - JSON comparison (para `validateJsonEquals`)

---

## 🚨 PUNTOS DE ATENCIÓN

### **1. Métodos de Autenticación Específicos**
- 🤔 **Pregunta:** Los métodos como `getJwtToken()`, `getTokenMoppa()`, `getTokenDal()` son específicos de Scotia Bank
- 🤔 **Dilema:** ¿Deben estar en `common` (genérico) o en `api-core` (Scotia específico)?
- 💡 **Recomendación:**
  - **Opción A (Recomendada):** Crear contratos genéricos en `AuthenticationService`:
    ```java
    String getCustomToken(String tokenType, Map<String, String> params) throws FrameworkBusinessException;
    ```
  - **Opción B:** Crear extensión `ScotiaAuthenticationService extends AuthenticationService` en `api-core`
  - **Opción C:** Mantener métodos específicos en `common` pero parametrizados desde configuración

### **2. Variable `body` global**
- 🤔 Step `@Given("agrego el request")` usa variable global `body`
- ⚠️ **Problema:** Rompe principio de encapsulación
- 💡 **Solución:** Usar `httpClient.setBody()` en lugar de variable de instancia

### **3. Variable `dataJson` global**
- 🤔 Step `@Given("establezco la key {string} con el valor {string}")` usa `dataJson.put()`
- ⚠️ **Problema:** Gestión de estado global puede causar conflictos en ejecución paralela
- 💡 **Solución:** Usar `DataUtilities.storeValue()` con contexto aislado por thread

### **4. Métodos con `InternalServerExceptionError`**
- ⚠️ Excepción custom que no está en `common`
- 💡 **Solución:** Usar `FrameworkTechnicalException` o `FrameworkBusinessException`

---

## 📊 MÉTRICAS DEL ANÁLISIS

| Categoría | Cantidad |
|-----------|----------|
| **Total Steps Analizados** | 71 |
| **Steps Funcionales (Refactorizados)** | 15 |
| **Steps Duplicados (Consolidar)** | 1 |
| **Steps con Errores (Refactorizar)** | 40 |
| **Steps Comentados (Postponer)** | 15 |
| **Métodos Faltantes en AuthenticationService** | 9 |
| **Métodos Faltantes en HttpClient** | 3 |
| **Métodos Faltantes en DataUtilities** | 7 (8 ya existen ✅) |
| **Métodos Faltantes en ValidationUtilities** | 7 (13 ya existen ✅) |
| **TOTAL Métodos a Implementar** | 26 |
| **TOTAL Métodos Ya Disponibles en Common** | 21 ✅ |

---

## 🎓 CONCLUSIONES

### **✅ Fortalezas del Análisis:**
1. ✅ Se identificaron claramente 4 grupos de steps con estrategias distintas
2. ✅ Se detectó y planificó consolidación de headers duplicados
3. ✅ Se levantaron **30 métodos faltantes** en `common` con firmas propuestas
4. ✅ Se respeta arquitectura de desacoplamiento total
5. ✅ Se mantienen contratos de steps sin cambios

### **⚠️ Desafíos Identificados:**
1. ⚠️ Decisión pendiente sobre métodos de autenticación específicos de Scotia
2. ⚠️ Migración de variables globales (`body`, `dataJson`) a gestión encapsulada
3. ⚠️ Estandarización de excepciones custom a framework exceptions

### **🚀 Siguiente Acción Recomendada:**
1. **Confirmar decisión** sobre métodos de autenticación (Opción A, B o C)
2. **Implementar** métodos faltantes en `common` (comenzar con JSON y validaciones)
3. **Refactorizar** steps del Grupo 3 usando nueva arquitectura
4. **Consolidar** headers duplicados
5. **Eliminar** variables globales problemáticas

---

## 💡 EJEMPLOS CONCRETOS DE REFACTORIZACIÓN

### **Ejemplo 1: Step de Field (Ya tenemos el método)**

**ANTES (Con errores):**
```java
@Given("agrego el field {string} con el valor {string}")
public void agregoElFieldKeyConElValorValue(String arg0, String arg1) {
    setFields(arg0, arg1); // ❌ Método no existe
}
```

**DESPUÉS (Usando arquitectura Common):**
```java
@Given("agrego el field {string} con el valor {string}")
public void agregoElFieldKeyConElValorValue(String key, String value) {
    String processedKey = DataUtilities.replaceVariables(key);
    String processedValue = DataUtilities.replaceVariables(value);
    httpClient.addField(processedKey, processedValue); // ✅ Usa HttpClient de common
    TestLogger.logDebug("API_STEPS_CONFIG", 
        String.format("Field agregado: %s = %s", processedKey, processedValue), null);
}
```

---

### **Ejemplo 2: Step de Query Param (Ya tenemos el método)**

**ANTES (Con errores):**
```java
@Given("agrego el queryparam {string} con el valor {string}")
public void agregoElQueryparamConElValor(String arg0, String arg1) {
    setQueryParams(arg0, arg1); // ❌ Método no existe
}
```

**DESPUÉS (Usando arquitectura Common):**
```java
@Given("agrego el queryparam {string} con el valor {string}")
public void agregoElQueryparamConElValor(String param, String value) {
    String processedParam = DataUtilities.replaceVariables(param);
    String processedValue = DataUtilities.replaceVariables(value);
    httpClient.addQueryParam(processedParam, processedValue); // ✅ Usa HttpClient de common
    TestLogger.logDebug("API_STEPS_CONFIG", 
        String.format("Query parameter agregado: %s = %s", processedParam, processedValue), null);
}
```

---

### **Ejemplo 3: Step de Extracción JSON (Ya tenemos el método)**

**ANTES (Con errores):**
```java
@Given("el resultado almaceno el valor de {string}")
public void elResultadoAlmacenoElValorDe(String arg0) {
    getJsonData(arg0); // ❌ Método no existe
}
```

**DESPUÉS (Usando arquitectura Common):**
```java
@Given("el resultado almaceno el valor de {string}")
public void elResultadoAlmacenoElValorDe(String jsonPath) {
    try {
        HttpResponse lastResponse = httpClient.getLastResponse(); // ✅ Usa HttpClient
        String responseBody = lastResponse.getBody();
        
        Object value = DataUtilities.getJsonParameter(responseBody, jsonPath); // ✅ Usa DataUtilities
        DataUtilities.storeValue(jsonPath, value); // ✅ Almacena en contexto
        
        TestLogger.logInfo("API_STEPS_DATA", 
            String.format("Valor almacenado desde JSON: %s = %s", jsonPath, value), null);
    } catch (FrameworkBusinessException e) {
        throw new RuntimeException("Error extrayendo valor JSON: " + e.getMessage(), e);
    }
}
```

---

### **Ejemplo 4: Step de Validación JSON Path (Ya tenemos el método)**

**ANTES (Con errores):**
```java
@Then("valido que el valor dentro de la estructura {string} sea {string}")
public void validoQueElValorDentroDeLaEstructuraSea(String arg0, String arg1) {
    Assert.assertTrue(validateJson(arg0, arg1)); // ❌ Método no existe
}
```

**DESPUÉS (Usando arquitectura Common):**
```java
@Then("valido que el valor dentro de la estructura {string} sea {string}")
public void validoQueElValorDentroDeLaEstructuraSea(String jsonPath, String expectedValue) 
    throws FrameworkBusinessException {
    try {
        HttpResponse lastResponse = httpClient.getLastResponse(); // ✅ Usa HttpClient
        String processedExpectedValue = DataUtilities.replaceVariables(expectedValue);
        
        ValidationUtilities.validateJsonPath(lastResponse, jsonPath, processedExpectedValue); // ✅ Usa ValidationUtilities
        
        TestLogger.logInfo("API_STEPS_VALIDATION", 
            String.format("Validación JSON exitosa: %s = %s", jsonPath, processedExpectedValue), null);
    } catch (FrameworkBusinessException e) {
        throw new FrameworkBusinessException(
            "validoQueElValorDentroDeLaEstructuraSea",
            String.format("Error validando JSON path '%s': %s", jsonPath, e.getMessage()));
    }
}
```

---

### **Ejemplo 5: Step de Ejecución con Método (Ya tenemos el método)**

**ANTES (Con errores):**
```java
@When("ejecuto la consulta con el metodo {string}")
public void ejecutoLaConsultaConElMetodo(String arg0) throws InternalServerExceptionError {
    executeWS(arg0, true); // ❌ Método no existe, excepción custom
}
```

**DESPUÉS (Usando arquitectura Common):**
```java
@When("ejecuto la consulta con el metodo {string}")
public void ejecutoLaConsultaConElMetodo(String method) throws FrameworkTechnicalException {
    try {
        String processedEndpoint = httpClient.getHost(); // Endpoint ya configurado
        
        switch (method.toUpperCase()) {
            case "GET":
                httpClient.get(""); // ✅ Usa HttpClient - endpoint ya configurado
                break;
            case "POST":
                httpClient.post("");
                break;
            case "PUT":
                httpClient.put("");
                break;
            case "DELETE":
                httpClient.delete("");
                break;
            case "PATCH":
                httpClient.patch("");
                break;
            default:
                throw new FrameworkTechnicalException(
                    "ejecutoLaConsultaConElMetodo", 
                    "Método HTTP no soportado: " + method);
        }
        
        TestLogger.logInfo("API_STEPS_EXECUTION", 
            String.format("Petición %s ejecutada exitosamente", method), null);
    } catch (Exception e) {
        throw new FrameworkTechnicalException(
            "ejecutoLaConsultaConElMetodo",
            String.format("Error ejecutando petición %s: %s", method, e.getMessage()));
    }
}
```

---

### **Ejemplo 6: Step de Validación Status Code (Ya refactorizado)**

**ANTES (Con errores):**
```java
@Then("valido que el codigo de respuesta del servicio sea {int}")
public void validoQueElCodigoDeRespuestaDelServicioSea(int arg0) {
    Assert.assertEquals("HttpStatus Error, se esperaba " + arg0 + ", llego " +
            getHttpStatus() + ". \nRespuesta del servicio: " + getBodyResponse() + ". \nBody enviado:: " + body, 
            arg0, getHttpStatus()); // ❌ Métodos y variable no existen
}
```

**DESPUÉS (Usando arquitectura Common):**
```java
@Then("valido que el codigo de respuesta del servicio sea {int}")
public void validoQueElCodigoDeRespuestaDelServicioSea(int expectedStatus) 
    throws FrameworkBusinessException {
    try {
        HttpResponse lastResponse = httpClient.getLastResponse(); // ✅ Usa HttpClient
        ValidationUtilities.validateStatusCode(lastResponse, expectedStatus); // ✅ Usa ValidationUtilities
        
        TestLogger.logInfo("API_STEPS_VALIDATION", 
            String.format("Código de respuesta validado exitosamente: %d", expectedStatus), null);
    } catch (FrameworkBusinessException e) {
        // Agregar contexto adicional al error
        String errorMessage = String.format(
            "HttpStatus Error, se esperaba %d, llegó %d. Respuesta del servicio: %s",
            expectedStatus,
            lastResponse.getStatusCode(),
            DataUtilities.truncateContent(lastResponse.getBody(), 500)
        );
        throw new FrameworkBusinessException("validoQueElCodigoDeRespuestaDelServicioSea", errorMessage);
    }
}
```

---

### **Ejemplo 7: Consolidación de Headers Duplicados**

**Step Principal (Ya Funcional):**
```java
@And("agrego el header {word} con valor {word}")
public void agregoElHeaderConValor(String header, String value) {
    String processedValue = DataUtilities.replaceVariables(value);
    httpClient.addHeader(header, processedValue);
    TestLogger.logDebug("API_STEPS_CONFIG", 
        String.format("Header agregado: %s = %s", header, processedValue), null);
}
```

**Step Duplicado (Temporalmente delega al principal):**
```java
@Given("agrego el header {string} con el valor {string}")
public void agregoElHeaderKeyConElValorValue(String key, String value) {
    // DELEGACIÓN TEMPORAL - Este step será eliminado después de migrar features
    agregoElHeaderConValor(key, value);
    TestLogger.logWarning("API_STEPS_DEPRECATED", 
        "Step deprecado usado. Migrar a: 'agrego el header {word} con valor {word}'", null);
}
```

---

## 🎯 RESUMEN DE ESTRATEGIAS POR TIPO DE STEP

| Tipo de Step | Estrategia | Complejidad | Ejemplo |
|--------------|-----------|-------------|---------|
| **Headers/Params/Fields** | ✅ Refactorización directa - métodos ya existen | 🟢 Baja | `setFields()` → `httpClient.addField()` |
| **Autenticación Simple** | ✅ Refactorización directa - métodos ya existen | 🟢 Baja | `getTokenCC()` → `authentication.getClientCredentialsToken()` |
| **Extracción JSON Simple** | ✅ Refactorización directa - métodos ya existen | 🟢 Baja | `getJsonData()` → `DataUtilities.getJsonParameter()` |
| **Validación JSON Simple** | ✅ Refactorización directa - métodos ya existen | 🟢 Baja | `validateJson()` → `ValidationUtilities.validateJsonPath()` |
| **Ejecución HTTP** | ✅ Refactorización directa - métodos ya existen | 🟢 Baja | `executeWS()` → `httpClient.get/post/etc()` |
| **Autenticación Compleja** | ⚠️ Requiere implementación nuevos métodos | 🟡 Media | `getJwtToken()` → Nuevo método en `AuthenticationService` |
| **JSON Manipulation** | ⚠️ Requiere implementación nuevos métodos | 🟡 Media | `setJsonParameters()` → Nuevo método en `DataUtilities` |
| **JSON Schema Validation** | ⚠️ Requiere implementación nuevos métodos | 🟡 Media | `validateJsonSchema()` → Nuevo método en `ValidationUtilities` |
| **Wait Utilities** | ⚠️ Requiere implementación nuevos métodos | 🟢 Baja | `waitForSeconds()` → Nuevo método en `DataUtilities` |
| **Redirect Config** | ⚠️ Requiere implementación en HttpClient | 🟡 Media | `executeWS(method, false)` → Nuevo método en `HttpClient` |

---

## 🔴 RESUMEN DE ERRORES DE COMPILACIÓN ACTUALES

### **Estado Actual: 94 Errores Detectados**

#### **Errores por Categoría:**

| Categoría | Cantidad | Tipo |
|-----------|----------|------|
| **Métodos No Resueltos** | 43 | 🔴 ERROR |
| **Símbolos No Resueltos** | 9 | 🔴 ERROR |
| **Métodos No Usados** | 42 | ⚠️ WARNING |

---

### **🔧 Métodos Faltantes Más Críticos (Top 10):**

1. ❌ `setHeaders()` - **13 ocurrencias** → Usar `httpClient.addHeader()`
2. ❌ `getOpaqueToken()` - **7 ocurrencias** → Implementar en `AuthenticationService`
3. ❌ `getJwtToken()` - **3 ocurrencias** → Implementar en `AuthenticationService`
4. ❌ `InternalServerExceptionError` - **6 ocurrencias** → Reemplazar por `FrameworkBusinessException`
5. ❌ `setHost()` / `setEndPoint()` - **2 ocurrencias** → Usar `httpClient.setHost()`
6. ❌ `getTokenCC()` - **1 ocurrencia** → Usar `authentication.getClientCredentialsToken()`
7. ❌ `getTokenRut()` - **1 ocurrencia** → Usar `authentication.getBearerTokenForIdentifier()`
8. ❌ `getTokenDigitalMortgage()` - **1 ocurrencia** → Implementar en `AuthenticationService`
9. ❌ `getTokenBaasLatam()` - **1 ocurrencia** → Implementar en `AuthenticationService`
10. ❌ `getTokenCCTokenGenerator()` - **1 ocurrencia** → Implementar en `AuthenticationService`

---

### **📋 Clasificación de Errores por Solución:**

#### **🟢 FÁCIL - Refactorización Directa (Ya tenemos el método en common)**
```
✅ setHeaders()          → httpClient.addHeader()
✅ setFields()           → httpClient.addField()
✅ setQueryParams()      → httpClient.addQueryParam()
✅ setHost()             → httpClient.setHost()
✅ getTokenCC()          → authentication.getClientCredentialsToken()
✅ getTokenRut()         → authentication.getBearerTokenForIdentifier()
✅ getJsonData()         → DataUtilities.getJsonParameter()
✅ replaceData()         → DataUtilities.replaceVariables()
✅ body (variable)       → httpClient.setBody()
✅ dataJson (variable)   → DataUtilities.storeValue()
✅ executeWS()           → httpClient.get/post/etc()
✅ Assert                → Usar JUnit o ValidationUtilities
```

#### **🟡 MEDIO - Implementación Requerida en Common**
```
⚠️ getJwtToken()                    → Nuevo método en AuthenticationService
⚠️ getOpaqueToken()                 → Nuevo método en AuthenticationService
⚠️ getTokenDigitalMortgage()        → Nuevo método en AuthenticationService
⚠️ getTokenBaasLatam()              → Nuevo método en AuthenticationService
⚠️ getTokenCCTokenGenerator()       → Nuevo método en AuthenticationService
⚠️ getTokenUP()                     → Nuevo método en AuthenticationService
⚠️ getUrlTokenOpaqueDatalab()       → Nuevo método en AuthenticationService
⚠️ getTokenDal()                    → Nuevo método en AuthenticationService
⚠️ getTokenMoppa()                  → Nuevo método en AuthenticationService
⚠️ setJsonParameters()              → Nuevo método en DataUtilities
⚠️ saveValueFromStructure()         → Nuevo método en DataUtilities
⚠️ captureHeader()                  → Nuevo método en DataUtilities o HttpClient
⚠️ captureQueryParam()              → Nuevo método en DataUtilities
⚠️ saveFieldBbddInVariable()        → Requiere análisis (DB relacionado)
```

#### **🔴 COMPLEJO - Excepciones y Estructuras Custom**
```
❌ InternalServerExceptionError     → Reemplazar por FrameworkBusinessException/FrameworkTechnicalException
```

---

### **🎯 PLAN DE ACCIÓN INMEDIATO - Orden de Ejecución**

#### **FASE 1: Limpieza de Excepciones (5 minutos)**
- [ ] Reemplazar `InternalServerExceptionError` por `FrameworkBusinessException` (6 ocurrencias)
- [ ] Agregar imports de `org.junit.Assert` o usar `ValidationUtilities`

#### **FASE 2: Refactorización Directa - Métodos Existentes (30 minutos)**
- [ ] `setHeaders()` → `httpClient.addHeader()` (13 ocurrencias)
- [ ] `setFields()` → `httpClient.addField()` (1 ocurrencia)
- [ ] `setQueryParams()` → `httpClient.addQueryParam()` (1 ocurrencia)
- [ ] `setHost()` / `setEndPoint()` → `httpClient.setHost()` (2 ocurrencias)
- [ ] `getTokenCC()` → `authentication.getClientCredentialsToken()` (1 ocurrencia)
- [ ] `getTokenRut()` → `authentication.getBearerTokenForIdentifier()` (1 ocurrencia)
- [ ] `getJsonData()` → `DataUtilities.getJsonParameter()` (1 ocurrencia)
- [ ] `replaceData()` → `DataUtilities.replaceVariables()` (3 ocurrencias)
- [ ] `body` variable → `httpClient.setBody()` (1 ocurrencia)
- [ ] `dataJson` variable → `DataUtilities.storeValue()` (1 ocurrencia)
- [ ] `executeWS()` → `httpClient.get/post/etc()` (2 ocurrencias)

**Resultado esperado:** ✅ **~26 errores resueltos** (de 94 totales)

#### **FASE 3: Implementación de Métodos Faltantes - Autenticación (2 horas)**
- [ ] Diseñar contratos en `AuthenticationService`
- [ ] Implementar en `BaseAuthenticationManager`
- [ ] Refactorizar steps que usan estos métodos

**Resultado esperado:** ✅ **~18 errores adicionales resueltos**

#### **FASE 4: Implementación de Métodos Faltantes - Utilidades (1 hora)**
- [ ] Implementar métodos JSON en `DataUtilities`
- [ ] Implementar métodos de captura en `DataUtilities` o `HttpClient`
- [ ] Refactorizar steps que usan estos métodos

**Resultado esperado:** ✅ **~6 errores adicionales resueltos**

#### **FASE 5: Consolidación y Testing (30 minutos)**
- [ ] Consolidar headers duplicados
- [ ] Compilar y verificar 0 errores
- [ ] Ejecutar tests básicos

**Resultado esperado:** ✅ **0 errores, código compilando correctamente**

---

### **📈 PROGRESO ESPERADO**

```
Estado Actual:    [██░░░░░░░░] 0%   - 94 errores
Después Fase 1:   [███░░░░░░░] 10%  - 88 errores
Después Fase 2:   [██████░░░░] 50%  - 62 errores
Después Fase 3:   [████████░░] 80%  - 44 errores
Después Fase 4:   [█████████░] 95%  - 38 errores (métodos DB postponidos)
Después Fase 5:   [██████████] 100% - 0 errores críticos
```

---

### **⚠️ DECISIONES PENDIENTES CRÍTICAS**

#### **1. Métodos de Autenticación Específicos de Scotia**
**Pregunta:** ¿Dónde implementar los 9 métodos de tokens específicos (`getJwtToken`, `getOpaqueToken`, etc.)?

**Opciones:**

**A) En `common` - Interface + Implementación Base (RECOMENDADA ✅)**
```java
// AuthenticationService (Interface)
String getJwtTokenWithCredentials(String username, String password) throws FrameworkBusinessException;
String getOpaqueToken(String username, String password) throws FrameworkBusinessException;
// ... otros métodos

// BaseAuthenticationManager (Implementación)
// Lectura de endpoints desde ConfigurationProvider
```

**Ventajas:**
- ✅ Reutilizable para otros bancos (Santander, Itaú)
- ✅ Nombres genéricos y parametrizables
- ✅ Configuración desde archivos YAML/Properties
- ✅ Mantiene desacoplamiento

**Desventajas:**
- ⚠️ `common` conoce tipos de autenticación específicos

---

**B) En `api-core` - Extensión de Interface**
```java
// ScotiaAuthenticationService extends AuthenticationService
public interface ScotiaAuthenticationService extends AuthenticationService {
    String getJwtTokenWithCredentials(String username, String password);
    String getOpaqueToken(String username, String password);
    // ... métodos específicos Scotia
}
```

**Ventajas:**
- ✅ `common` permanece 100% genérico
- ✅ Scotia-specific logic en `api-core`

**Desventajas:**
- ❌ Otros módulos (Santander) tendrían que reimplementar lógica similar
- ❌ Duplicación de código entre frameworks específicos

---

**C) En módulo consumidor - Steps específicos**
```java
// scotia-api/src/.../ScotiaAuthSteps.java
@Given("agrego el token JWT de Scotia para {string} y {string}")
public void agregoTokenJwtScotia(String user, String pass) {
    // Lógica específica Scotia aquí
}
```

**Ventajas:**
- ✅ `common` y `api-core` permanecen genéricos

**Desventajas:**
- ❌ No reutilizable entre proyectos Scotia
- ❌ Lógica de autenticación fuera de framework

---

**💡 RECOMENDACIÓN FINAL:** **Opción A**
- Implementar métodos genéricos en `AuthenticationService`
- Configurar endpoints y parámetros específicos desde archivos de configuración
- Ejemplo:
  ```yaml
  # scotia-api/config/auth-config.yml
  authentication:
    jwt:
      endpoint: "/api/auth/jwt-token"
      method: "POST"
      headers:
        Content-Type: "application/json"
    opaque:
      endpoint: "/api/auth/opaque-token"
      method: "POST"
  ```

---

**Fecha de Análisis:** 11 de Noviembre 2025  
**Responsable:** Abel Venero  
**Estado:** ✅ Análisis Completo - Pendiente Confirmación de Decisiones

