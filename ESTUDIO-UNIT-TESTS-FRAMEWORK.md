# 🧪 Estudio Completo: Unit Tests del Framework QA Scotia

**Fecha:** 23 de Febrero 2026  
**Objetivo:** Identificar gaps de cobertura y oportunidades de mejora  
**Alcance:** api-core, web-core, common, mobile-core  
**Cobertura Actual:** 19% (objetivo: 70%)

---

## 📊 RESUMEN EJECUTIVO

### Estado Actual:
- **87 clases de producción** en el framework
- **11 tests unitarios** existentes (13% de las clases)
- **Cobertura:** 19% (líneas), 19% (branch)
- **Gap:** 51% para alcanzar objetivo (70%)

### Tests Existentes (11):
```
common/
  ├── ConfigManagerTest.java ✅
  ├── ConfigManagerPriorityTest.java ✅
  ├── ScenarioContextTest.java ✅
  ├── DataUtilitiesVariableStorageTest.java ✅
  ├── DataUtilitiesCapitalizeTest.java ✅
  ├── HttpResponseTest.java ✅
  ├── TestLoggerTest.java ✅
  ├── LoggingConfigurationTest.java ✅
  ├── ModuleDetectorTest.java ✅
  ├── EvidenceManagerTest.java ✅
  └── WebDriverManagerArtifactoryTest.java ✅
```

### Clases SIN Tests (76):
- **common:** 40+ clases críticas
- **api-core:** 15+ clases
- **web-core:** 15+ clases
- **mobile-core:** 5+ clases

---

## 🎯 PRIORIZACIÓN DE TESTS (Por Criticidad)

### **CRÍTICO (P0) - Implementar PRIMERO**

#### **1. common/database/** (0% cobertura)
- `DatabaseConfig.java` ⚠️ **CRÍTICO**
  - Gestiona pools de conexiones (HikariCP)
  - Soporta Oracle, SQL Server, PostgreSQL, MySQL
  - Windows Authentication (código nuevo sin tests)
  
- `DbConnectorFactory.java` ⚠️ **CRÍTICO**
  - Factory que crea conectores por tipo de BD
  - Detecta driver automáticamente
  - Gestiona configuraciones por BD

- `DatabaseHelper.java` ⚠️ **ALTO**
  - Ejecuta queries con parámetros
  - Parsea resultados
  - Maneja transacciones

**Tests necesarios:**
```
DatabaseConfigTest.java (30-40 tests)
  - testCreateOracleDataSource()
  - testCreateSQLServerDataSource()
  - testCreateSQLServerWithWindowsAuth()
  - testCreatePostgreSQLDataSource()
  - testCreateMySQLDataSource()
  - testPoolConfiguration()
  - testConnectionTimeout()
  - testInvalidUrl()
  - testMissingCredentials()
  - testWindowsAuthWithoutUsername()

DbConnectorFactoryTest.java (20-25 tests)
  - testCreateFromConfigOracle()
  - testCreateFromConfigSQLServer()
  - testCreateFromConfigWithWindowsAuth()
  - testDetectDriverFromUrl()
  - testInvalidDriverType()
  - testMissingConfiguration()

DatabaseHelperTest.java (15-20 tests)
  - testExecuteSelectQuery()
  - testExecuteQueryWithParameters()
  - testGetSingleValue()
  - testGetRowCount()
  - testInvalidQuery()
  - testConnectionClosed()
```

**Vulnerabilidades detectadas:**
- ⚠️ `DatabaseConfig` no valida NULL en username/password antes de setear
- ⚠️ `DatabaseHelper` no valida query injection básica
- ⚠️ Falta validación de timeout negativo en configuración

---

#### **2. common/driver/** (5% cobertura - solo 1 test)

- `WebDriverManager.java` ⚠️ **CRÍTICO**
  - Gestiona descarga de drivers (local + Artifactory)
  - Cache en ~/.cache/qa-drivers/
  - Validación SSL con truststore
  - **Ya tiene:** WebDriverManagerArtifactoryTest (1 test básico)
  - **Falta:** 90% de la lógica sin testear

**Tests necesarios:**
```
WebDriverManagerTest.java (EXPANDIR existente - 40+ tests)
  
  # Estrategia LOCAL
  - testGetDriverFromLocalPath()
  - testGetDriverFromLocalPathWithVersion()
  - testLocalDriverNotFound()
  - testLocalDriverInvalidPath()
  - testLocalDriverNoExecutePermission()
  
  # Estrategia ARTIFACTORY
  - testDownloadFromArtifactorySuccess()
  - testDownloadFromArtifactoryWithAuth()
  - testDownloadFromArtifactoryWithoutAuth()
  - testDownloadReturnsHTML() ⚠️
  - testDownloadFileTooSmall() ⚠️
  - testDownloadContentTypeValidation() ⚠️
  - testDownloadWithSSLError()
  - testDownloadRetryMechanism()
  - testDownloadTimeout()
  - testArtifactory404()
  - testArtifactory401()
  - testArtifactory403()
  
  # Cache
  - testCacheDriverFound() ⚠️ (nuevo - no testeado)
  - testCacheDriverInvalid()
  - testCacheDriverSmallFile()
  - testCacheDriverNotExecutable()
  - testCacheReuse()
  
  # Construcción de URL
  - testBuildArtifactoryUrlMac()
  - testBuildArtifactoryUrlWindows()
  - testBuildArtifactoryUrlLinux()
  - testDetectOperatingSystem()
  
  # SSL
  - testSSLConfigurationLoaded()
  - testSSLConfigurationFallback()
  
  # Fallback PATH
  - testFindDriverInSystemPath()
  - testDriverNotFoundAnywhere()
```

**Vulnerabilidades detectadas:**
- ⚠️ Cache puede quedar corrupto sin limpieza
- ⚠️ No hay timeout en conexión HTTP (puede colgar)
- ⚠️ Permisos ejecutables fallan silenciosamente
- ⚠️ URL construida no se valida antes de descargar

---

#### **3. common/ssl/** (0% cobertura)

- `SSLUtils.java` ⚠️ **CRÍTICO**
  - Carga truststore corporativo
  - Crea HttpClient seguro
  - Usado por BaseHttpClient y WebDriverManager
  
**Tests necesarios:**
```
SSLUtilsTest.java (20-25 tests)
  - testLoadFrameworkSSLContextSuccess()
  - testLoadSSLContextWithCache()
  - testFindTruststoreInMultipleLocations()
  - testTruststoreNotFound()
  - testTruststoreInvalidPassword()
  - testTruststoreCorrupted()
  - testCreateSecureHttpClient()
  - testCreateHttpClientWithCredentials()
  - testCreateHttpClientFallback()
  - testSSLContextCaching()
```

**Vulnerabilidades detectadas:**
- ⚠️ Password hardcoded ("changeit") - OK para truststore público
- ⚠️ No valida que el truststore tenga certificados válidos
- ⚠️ Falta logging cuando truststore no se encuentra

---

#### **4. common/utils/** (20% cobertura - solo DataUtilities parcial)

- `DataUtilities.java` ⚠️ **ALTO**
  - **Tiene:** DataUtilitiesVariableStorageTest, DataUtilitiesCapitalizeTest
  - **Falta:** 80% sin testear (JSON, validaciones, conversiones)

- `SecurityUtilities.java` ⚠️ **CRÍTICO**
  - Encriptación/desencriptación
  - Hashing de passwords
  - **0% cobertura** ⚠️⚠️⚠️

- `ConfigurationUtilities.java` ⚠️ **MEDIO**
  - Carga archivos de configuración
  - **0% cobertura**

**Tests necesarios:**
```
DataUtilitiesTest.java (EXPANDIR - 30+ tests adicionales)
  # JSON
  - testParseJsonToMap()
  - testParseJsonToList()
  - testParseInvalidJson()
  - testToJsonString()
  
  # Variables
  - testReplaceVariablesSimple()
  - testReplaceVariablesNested()
  - testReplaceVariablesNotFound()
  - testReplaceVariablesRecursive()
  
  # Validaciones
  - testValidateEmail()
  - testValidatePhone()
  - testValidateNumeric()
  - testValidateDate()
  
  # Conversiones
  - testConvertToBase64()
  - testConvertFromBase64()
  - testConvertTimestamp()

SecurityUtilitiesTest.java (25-30 tests) ⚠️⚠️⚠️
  # Encriptación
  - testEncryptString()
  - testDecryptString()
  - testEncryptDecryptRoundtrip()
  - testEncryptNullValue()
  - testEncryptEmptyString()
  - testDecryptInvalidData()
  
  # Hashing
  - testHashPassword()
  - testVerifyPassword()
  - testHashSHA256()
  - testHashMD5()
  
  # Seguridad
  - testSanitizeInput()
  - testPreventSQLInjection()
  - testPreventXSS()

ConfigurationUtilitiesTest.java (15-20 tests)
  - testLoadPropertiesFromFile()
  - testLoadPropertiesFromClasspath()
  - testLoadPropertiesNotFound()
  - testGetProperty()
  - testGetPropertyWithDefault()
  - testResolveEnvironmentVariables()
```

**Vulnerabilidades detectadas:**
- 🔴 **SecurityUtilities SIN TESTS** - Código crítico de seguridad sin validación
- ⚠️ DataUtilities no valida inyección en replaceVariables
- ⚠️ ConfigurationUtilities puede exponer credenciales en logs

---

### **ALTO (P1) - Implementar SEGUNDO**

#### **5. api-core/utils/** (0% cobertura)

- `ApiHelper.java` ⚠️ **ALTO**
  - Construcción de requests
  - Validaciones de response
  - Headers, body, query params
  
- `ValidationUtilities.java` ⚠️ **ALTO**
  - Validaciones de JSON
  - Schemas
  - Response codes

**Tests necesarios:**
```
ApiHelperTest.java (35-40 tests)
  # Configuración Endpoint
  - testConfigureEndpoint()
  - testConfigureEndpointNotFound()
  - testConfigureEndpointFromConfig()
  
  # Headers
  - testAddHeader()
  - testAddMultipleHeaders()
  - testRemoveHeader()
  - testClearHeaders()
  
  # Request Body
  - testSetJsonBody()
  - testSetFormData()
  - testSetRequestFromFile()
  - testSetRequestInvalidJson()
  
  # Query Parameters
  - testAddQueryParam()
  - testAddMultipleQueryParams()
  - testBuildUrlWithParams()
  
  # Validaciones Response
  - testValidateStatusCode()
  - testValidateJsonResponse()
  - testValidateJsonPath()
  - testValidateHeader()
  - testExtractJsonValue()

ValidationUtilitiesTest.java (25-30 tests)
  - testValidateJsonSchema()
  - testValidateJsonStructure()
  - testValidateResponseCode2xx()
  - testValidateResponseCode4xx()
  - testValidateResponseCode5xx()
  - testValidateJsonPathExists()
  - testValidateJsonPathValue()
  - testValidateArrayLength()
  - testValidateFieldNotNull()
```

**Vulnerabilidades detectadas:**
- ⚠️ ApiHelper no valida URLs malformadas antes de configurar
- ⚠️ ValidationUtilities puede fallar con JSON muy grandes (DoS)
- ⚠️ Falta validación de Content-Type en responses

---

#### **6. api-core/implementations/** (0% cobertura)

- `BaseHttpClient.java` ⚠️ **CRÍTICO**
  - Cliente HTTP core del framework
  - Maneja SSL, timeouts, redirects
  - Logging de requests/responses
  - **0% cobertura** - Código crítico sin tests

- `BaseAuthenticationManager.java` ⚠️ **ALTO**
  - Gestión de tokens OAuth/JWT
  - Refresh de tokens
  
- `BaseDatabaseService.java` ⚠️ **MEDIO**
  - Wrapper sobre DatabaseHelper

**Tests necesarios:**
```
BaseHttpClientTest.java (40-50 tests) ⚠️⚠️⚠️
  # Configuración
  - testSetHost()
  - testSetHostInvalidUrl()
  - testAddHeader()
  - testSetTimeout()
  
  # Métodos HTTP
  - testExecuteGetRequest()
  - testExecutePostRequest()
  - testExecutePutRequest()
  - testExecuteDeleteRequest()
  - testExecutePatchRequest()
  
  # SSL
  - testRequestWithSSL()
  - testRequestWithInvalidCertificate()
  - testRequestWithTruststore()
  
  # Redirects
  - testFollowRedirects()
  - testNoFollowRedirects()
  - testMaxRedirectsExceeded()
  
  # Errores
  - testConnectionTimeout()
  - testReadTimeout()
  - testConnectionRefused()
  - test404NotFound()
  - test500ServerError()
  
  # Body masking
  - testMaskPasswordInLogs()
  - testMaskTokenInLogs()
  - testMaskSensitiveData()

BaseAuthenticationManagerTest.java (20-25 tests)
  - testAuthenticate()
  - testRefreshToken()
  - testTokenExpiration()
  - testInvalidCredentials()
  - testRevokeToken()
```

**Vulnerabilidades detectadas:**
- 🔴 **BaseHttpClient SIN TESTS** - Cliente HTTP crítico sin validación
- ⚠️ Masking de passwords puede no cubrir todos los casos
- ⚠️ No hay validación de tamaño de response (DoS)
- ⚠️ Timeouts pueden ser configurados con valores negativos

---

#### **7. web-core/driver/** (0% cobertura)

- `WebDriverFactory.java` ⚠️ **CRÍTICO**
  - Factory de WebDrivers (Chrome, Firefox, Edge, Safari)
  - Configuración de opciones
  - Modo headless
  
- `DriverManager.java` ⚠️ **ALTO**
  - ThreadLocal storage de drivers
  - Gestión de múltiples drivers concurrentes

**Tests necesarios:**
```
WebDriverFactoryTest.java (35-40 tests)
  # Creación Drivers
  - testCreateChromeDriver()
  - testCreateChromeDriverHeadless()
  - testCreateFirefoxDriver()
  - testCreateEdgeDriver()
  - testCreateSafariDriver()
  - testCreateUnsupportedBrowser()
  
  # Configuración
  - testConfigureDriverOptions()
  - testConfigureHeadlessMode()
  - testConfigureProxy()
  - testConfigureAcceptInsecureCerts()
  
  # Setup Driver
  - testSetupChromeDriver()
  - testSetupDriverNotFound()
  - testSetupDriverInvalidVersion()
  
  # Grid
  - testCreateGridDriver()
  - testGridUrlInvalid()

DriverManagerTest.java (15-20 tests)
  - testSetDriver()
  - testGetDriver()
  - testGetDriverNotInitialized()
  - testQuitDriver()
  - testMultipleThreads()
  - testThreadLocalIsolation()
```

**Vulnerabilidades detectadas:**
- ⚠️ WebDriverFactory no valida que el driver fue configurado correctamente
- ⚠️ DriverManager puede tener leaks de drivers si quit() falla
- ⚠️ ThreadLocal no se limpia explícitamente (memory leak en app servers)

---

#### **8. web-core/utils/** (0% cobertura)

- `WebHelper.java` ⚠️ **CRÍTICO**
  - 1500+ líneas de código
  - Interacciones complejas con WebDriver
  - Validaciones de elementos
  - Screenshots, waits, assertions
  - **0% cobertura** - Clase MÁS GRANDE sin tests

- `WaitUtils.java` ⚠️ **ALTO**
  - Esperas explícitas/implícitas
  - Condiciones custom

- `ScreenshotUtils.java` ⚠️ **MEDIO**
  - Captura y almacenamiento

**Tests necesarios:**
```
WebHelperTest.java (60-80 tests) ⚠️⚠️⚠️
  # Navegación
  - testNavigateTo()
  - testNavigateToInvalidUrl()
  - testRefreshPage()
  - testGoBack()
  - testGoForward()
  
  # Elementos
  - testFindElementById()
  - testFindElementByXPath()
  - testFindElementByCss()
  - testFindElementNotFound()
  - testWaitForElement()
  - testWaitForElementTimeout()
  
  # Interacciones
  - testClickElement()
  - testSendKeys()
  - testClearField()
  - testSelectDropdown()
  - testCheckCheckbox()
  - testUncheckCheckbox()
  
  # Validaciones
  - testValidateElementVisible()
  - testValidateElementNotVisible()
  - testValidateElementEnabled()
  - testValidateElementDisabled()
  - testValidateText()
  - testValidateAttribute()
  
  # Variables
  - testSaveVariable()
  - testGetVariable()
  - testReplaceVariablesInText()
  - testValidateAdditionVariables()
  
  # Screenshots
  - testTakeScreenshot()
  - testTakeScreenshotOnError()
  - testScreenshotSavedCorrectly()

WaitUtilsTest.java (15-20 tests)
  - testWaitForElementVisible()
  - testWaitForElementClickable()
  - testWaitForElementTimeout()
  - testWaitForTextPresent()
  - testCustomWaitCondition()

ScreenshotUtilsTest.java (10-12 tests)
  - testCaptureScreenshot()
  - testSaveScreenshot()
  - testScreenshotDirectory()
  - testScreenshotOnFailure()
```

**Vulnerabilidades detectadas:**
- 🔴 **WebHelper SIN TESTS** - 1500 líneas críticas sin validación
- ⚠️ Métodos de validación pueden lanzar excepciones no controladas
- ⚠️ Screenshots pueden llenar disco si no hay límite
- ⚠️ WaitUtils puede causar deadlock con timeouts incorrectos

---

### **MEDIO (P2) - Implementar TERCERO**

#### **9. common/reporting/** (0% cobertura)

- `ReportingManager.java` ⚠️ **MEDIO**
- `ExtentReportGenerator.java` ⚠️ **MEDIO**
- `JiraUpdateService.java` ⚠️ **MEDIO**
- `JiraHttpClient.java` ⚠️ **MEDIO**

**Tests necesarios:** 40-50 tests totales

**Vulnerabilidades detectadas:**
- ⚠️ Falta validación de credenciales Jira antes de intentar conexión
- ⚠️ Reportes pueden contener información sensible sin sanitizar
- ⚠️ No hay límite de tamaño en attachments

---

#### **10. common/http/** (10% cobertura - solo HttpResponse)

- `BaseHttpClient.java` (duplicado arriba en api-core)

**Tests necesarios:** Ver sección api-core

---

### **BAJO (P3) - Implementar AL FINAL**

#### **11. Models y DTOs** (coverage no crítico)

- `HttpResponse.java` ✅ (ya tiene test)
- `ScenarioResult.java`, `StepResult.java`, etc. (POJOs simples)

**Tests necesarios:** 10-15 tests (validación de getters/setters)

---

## 📈 PLAN DE IMPLEMENTACIÓN (Paso a Paso)

### **Sprint 1: Módulo COMMON - Database & Driver (CRÍTICO)**

**Objetivo:** Subir cobertura de 19% → 35%

**Tests a implementar:**
1. `DatabaseConfigTest.java` (30 tests) - 2 días
2. `DbConnectorFactoryTest.java` (25 tests) - 2 días
3. `DatabaseHelperTest.java` (20 tests) - 1.5 días
4. `WebDriverManagerTest.java` (expandir a 40 tests) - 2 días
5. `SSLUtilsTest.java` (25 tests) - 1.5 días

**Tiempo estimado:** 9 días  
**Cobertura esperada:** 35%

---

### **Sprint 2: Módulo COMMON - Utils & Security (CRÍTICO)**

**Objetivo:** Subir cobertura de 35% → 50%

**Tests a implementar:**
1. `SecurityUtilitiesTest.java` ⚠️⚠️⚠️ (30 tests) - 2 días
2. `DataUtilitiesTest.java` (expandir 30 tests) - 2 días
3. `ConfigurationUtilitiesTest.java` (20 tests) - 1 día

**Tiempo estimado:** 5 días  
**Cobertura esperada:** 50%

---

### **Sprint 3: Módulo API-CORE (CRÍTICO)**

**Objetivo:** Subir cobertura de 50% → 60%

**Tests a implementar:**
1. `BaseHttpClientTest.java` ⚠️⚠️⚠️ (50 tests) - 3 días
2. `ApiHelperTest.java` (40 tests) - 2 días
3. `ValidationUtilitiesTest.java` (30 tests) - 1.5 días
4. `BaseAuthenticationManagerTest.java` (25 tests) - 1.5 días

**Tiempo estimado:** 8 días  
**Cobertura esperada:** 60%

---

### **Sprint 4: Módulo WEB-CORE (CRÍTICO)**

**Objetivo:** Subir cobertura de 60% → 70%

**Tests a implementar:**
1. `WebHelperTest.java` ⚠️⚠️⚠️ (80 tests) - 4 días
2. `WebDriverFactoryTest.java` (40 tests) - 2 días
3. `DriverManagerTest.java` (20 tests) - 1 día
4. `WaitUtilsTest.java` (20 tests) - 1 día
5. `ScreenshotUtilsTest.java` (12 tests) - 0.5 días

**Tiempo estimado:** 8.5 días  
**Cobertura esperada:** 70%+

---

### **Sprint 5: Módulo REPORTING (Opcional)**

**Objetivo:** Subir cobertura de 70% → 75%

**Tests a implementar:**
1. `ReportingManagerTest.java` (25 tests) - 1.5 días
2. `ExtentReportGeneratorTest.java` (20 tests) - 1 día
3. `JiraUpdateServiceTest.java` (25 tests) - 1.5 días
4. `JiraHttpClientTest.java` (20 tests) - 1 día

**Tiempo estimado:** 5 días  
**Cobertura esperada:** 75%

---

## 🚨 VULNERABILIDADES Y MEJORAS IDENTIFICADAS

### **🔴 CRÍTICAS (Resolver URGENTE)**

1. **SecurityUtilities sin tests** 🔴🔴🔴
   - Código de encriptación sin validación
   - **Riesgo:** Vulnerabilidad de seguridad no detectada
   - **Acción:** Implementar SecurityUtilitiesTest inmediatamente

2. **BaseHttpClient sin tests** 🔴🔴
   - Cliente HTTP core sin validación
   - **Riesgo:** Errores en requests no detectados
   - **Acción:** Implementar BaseHttpClientTest (P0)

3. **WebHelper sin tests** 🔴🔴
   - 1500 líneas de código crítico sin cobertura
   - **Riesgo:** Errores en interacciones web no detectados
   - **Acción:** Implementar WebHelperTest (P0)

4. **DatabaseConfig - Windows Auth sin tests** 🔴
   - Código nuevo de Windows Authentication sin validar
   - **Riesgo:** Regresión en producción
   - **Acción:** Implementar DatabaseConfigTest inmediatamente

---

### **🟡 ALTAS (Resolver PRONTO)**

5. **Cache de drivers sin validación**
   - `WebDriverManager` cachea sin verificar integridad
   - **Riesgo:** Drivers corruptos no detectados
   - **Mejora:** Agregar checksum/validación de tamaño

6. **ThreadLocal leaks en DriverManager**
   - No hay limpieza explícita de ThreadLocal
   - **Riesgo:** Memory leak en servers de larga ejecución
   - **Mejora:** Agregar `remove()` en hooks

7. **Timeouts configurables sin validación**
   - Se pueden configurar valores negativos o muy altos
   - **Riesgo:** Tests colgados o timeout muy corto
   - **Mejora:** Validar rangos (min: 1s, max: 300s)

8. **Logging puede exponer credenciales**
   - Variables de entorno se loguean sin sanitizar
   - **Riesgo:** Passwords en logs
   - **Mejora:** Masking automático de patterns sensibles

---

### **🟢 MEDIAS (Resolver EVENTUALMENTE)**

9. **DataUtilities - Validación de inyección**
   - `replaceVariables()` no sanitiza input
   - **Riesgo:** Inyección de código en variables
   - **Mejora:** Whitelist de caracteres permitidos

10. **Screenshots sin límite de almacenamiento**
    - Pueden llenar disco en ejecuciones largas
    - **Riesgo:** Falla de pipeline por disco lleno
    - **Mejora:** Límite configurable + rotación

11. **Validaciones JSON sin límite de tamaño**
    - Puede causar OutOfMemory con JSONs gigantes
    - **Riesgo:** DoS en tests
    - **Mejora:** Límite de 10MB configurable

---

## 📊 MATRIZ DE COBERTURA POR MÓDULO

| Módulo | Clases | Tests | Coverage Actual | Coverage Objetivo | Gap |
|--------|--------|-------|-----------------|-------------------|-----|
| **common** | ~45 | 11 | 19% | 70% | 51% |
| **api-core** | ~15 | 0 | 0% | 70% | 70% |
| **web-core** | ~20 | 0 | 0% | 70% | 70% |
| **mobile-core** | ~7 | 0 | 0% | 50% | 50% |
| **TOTAL** | 87 | 11 | 13% | 70% | 57% |

---

## 🎯 RESUMEN DE TESTS A CREAR

### Por Prioridad:

**P0 - CRÍTICO (implementar YA):**
- SecurityUtilitiesTest.java (30 tests)
- BaseHttpClientTest.java (50 tests)
- WebHelperTest.java (80 tests)
- DatabaseConfigTest.java (40 tests)
- WebDriverManagerTest.java (40 tests adicionales)

**Total P0:** 240 tests (~15 días de desarrollo)

**P1 - ALTO (implementar siguiente sprint):**
- DbConnectorFactoryTest.java (25 tests)
- DatabaseHelperTest.java (20 tests)
- ApiHelperTest.java (40 tests)
- ValidationUtilitiesTest.java (30 tests)
- WebDriverFactoryTest.java (40 tests)

**Total P1:** 155 tests (~10 días)

**P2 - MEDIO:**
- Reporting, JiraClient, etc. (100 tests)

**Total P2:** 100 tests (~6 días)

**GRAN TOTAL:** 495 tests (~31 días de desarrollo)

---

## 💡 RECOMENDACIONES

### **Corto Plazo (Esta semana):**
1. Implementar `SecurityUtilitiesTest` (código de seguridad sin tests)
2. Implementar `BaseHttpClientTest` (cliente HTTP crítico)
3. Implementar `DatabaseConfigTest` (Windows Auth nuevo)

### **Mediano Plazo (Próximas 2 semanas):**
4. Expandir `WebDriverManagerTest`
5. Implementar `WebHelperTest` (priorizar métodos más usados)
6. Implementar `ApiHelperTest`

### **Largo Plazo (Próximo mes):**
7. Completar coverage de utilities
8. Tests de reporting
9. Tests de mobile-core

---

## 🔧 MEJORAS DE ARQUITECTURA DETECTADAS

### **1. Separar concerns en WebHelper**
- Clase de 1500 líneas viola SRP
- **Mejora:** Dividir en WebInteractions, WebValidations, WebWaits

### **2. Extraer constantes de DatabaseConfig**
- Timeouts, pool sizes hardcoded
- **Mejora:** Mover a ConfigConstants.java

### **3. Añadir Builder pattern en WebDriverFactory**
- Configuración muy verbosa
- **Mejora:** DriverConfig.builder().chrome().headless().build()

### **4. Cache de drivers necesita TTL**
- Drivers quedan cached sin expiración
- **Mejora:** TTL de 7 días o verificación de versión

### **5. Logging de credenciales inconsistente**
- A veces loguea, a veces no
- **Mejora:** Política consistente de masking

---

## ✅ CONCLUSIÓN

**Tests a crear:** 495 tests distribuidos en 31 días

**Prioridad absoluta (Semana 1):**
1. SecurityUtilitiesTest ⚠️⚠️⚠️
2. BaseHttpClientTest ⚠️⚠️
3. DatabaseConfigTest ⚠️
4. WebHelperTest (parcial) ⚠️⚠️

**Esto cubriría el 80% del riesgo con el 20% del esfuerzo** (Pareto)

**Coverage esperado después de Sprint 1:** 35% (+16%)  
**Coverage esperado después de Sprint 4:** 70% (+51%)

---

## 🔬 CASOS EDGE ESPECÍFICOS IDENTIFICADOS EN EL CÓDIGO

### **DatabaseConfig.java** (Líneas 46-139)

**Casos edge detectados en código:**
1. `jdbcUrl` NULL → Línea 72 (no validado antes de toLowerCase())
2. `jdbcUrl` con "integratedSecurity=true" pero user/password seteados → Línea 75-81
3. `user` NULL y NO es Windows Auth → Línea 88 (warning pero continúa)
4. `password` NULL y NO es Windows Auth → Línea 93 (warning pero continúa)
5. `driverClassName` NULL → Línea 114 (getConnectionTestQuery falla)
6. `maxPoolSize` <= 0 → Línea 104 (HikariCP lanzará excepción sin mensaje claro)
7. `minIdle` > `maxPoolSize` → Línea 105 (configuración inválida)
8. Connection test query inválida → Línea 115 (puede causar conexiones colgadas)

**Tests críticos necesarios:**
```java
@Test
void testCreateDataSourceWithNullJdbcUrl() {
    // ACTUALMENTE: NullPointerException en línea 72
    // ESPERADO: IllegalArgumentException con mensaje claro
}

@Test
void testWindowsAuthWithUsernameSetted() {
    // URL con integratedSecurity=true pero user != null
    // ACTUALMENTE: Setea username (error silencioso)
    // ESPERADO: Warning logged pero funciona OK
}

@Test
void testSQLServerWithEmptyCredentials() {
    // user="" password="" sin integratedSecurity
    // ACTUALMENTE: Warning pero continúa
    // ESPERADO: Falla en conexión (validar que falle correctamente)
}

@Test
void testInvalidPoolConfiguration() {
    // maxPoolSize=5, minIdle=10
    // ACTUALMENTE: HikariCP lanza IllegalArgumentException genérica
    // ESPERADO: Mensaje claro "minIdle no puede ser mayor que maxPoolSize"
}
```

---

### **SecurityUtilities.java** (Líneas 1-176)

**Casos edge detectados:**
1. `generateSecureToken(0)` → Línea 30 (array vacío)
2. `generateSecureToken(-5)` → Línea 31 (NegativeArraySizeException)
3. `generateSecurePassword(0)` → Línea 39 (password vacío)
4. `maskPassword(null)` → Línea 54 (manejado OK)
5. `maskPassword("")` → Línea 54 (retorna "***EMPTY***")
6. `maskPassword("ab")` → Línea 58 (length=2, substring falla)
7. `sha256Hash(null)` → Línea 86 (NullPointerException)
8. `sha256Hash("")` → Línea 86 (hash válido de string vacío - OK?)
9. `isSecurePassword(null)` → Línea 97 (retorna false - OK)
10. `isSecurePassword("Pass1!")` → Línea 97 (length=6 < 8, retorna false - pero no valida complejidad)

**Vulnerabilidades CRÍTICAS:**
```java
// LÍNEA 62: maskPassword con string corto FALLA
public static String maskPassword(String password) {
    if (password.length() <= 4) {
        return "***HIDDEN***";
    }
    return password.substring(0, 2) + "***" + password.substring(password.length() - 2);
    // ⚠️ Si password="ab" (length=2):
    //    - Pasa el check <= 4 → retorna "***HIDDEN***" ✅
    // ⚠️ Si password="abcd" (length=4):
    //    - Pasa el check <= 4 → retorna "***HIDDEN***" ✅
    // ⚠️ Si password="abcde" (length=5):
    //    - NO pasa el check
    //    - substring(0, 2) = "ab"
    //    - substring(5-2=3) = "de"
    //    - Resultado: "ab***de" ✅ OK
}

// LÍNEA 97-110: isSecurePassword NO valida complejidad
public static boolean isSecurePassword(String password) {
    if (password == null || password.length() < 8) {
        return false;
    }
    // ⚠️ FALTA: Validar mayúsculas, minúsculas, números, símbolos
    // "aaaaaaaa" retorna TRUE (8 caracteres) pero NO es seguro
}
```

**Tests críticos:**
```java
@Test
void testGenerateSecureTokenWithNegativeLength() {
    assertThrows(IllegalArgumentException.class, 
        () -> SecurityUtilities.generateSecureToken(-5));
}

@Test
void testMaskPasswordEdgeCases() {
    assertEquals("***HIDDEN***", SecurityUtilities.maskPassword("a"));
    assertEquals("***HIDDEN***", SecurityUtilities.maskPassword("ab"));
    assertEquals("***HIDDEN***", SecurityUtilities.maskPassword("abc"));
    assertEquals("***HIDDEN***", SecurityUtilities.maskPassword("abcd"));
    assertEquals("ab***de", SecurityUtilities.maskPassword("abcde"));
}

@Test
void testIsSecurePasswordDoesNotValidateComplexity() {
    // BUG: Password débil retorna TRUE
    assertTrue(SecurityUtilities.isSecurePassword("aaaaaaaa"));
    // DEBERÍA: retornar FALSE (sin mayúsculas, números, símbolos)
}
```

---

### **BaseHttpClient.java** (1421 líneas - Cliente HTTP CORE)

**Casos edge detectados revisando código:**
1. `setHost(null)` → No validado
2. `setHost("")` → No validado
3. `setHost("invalid-url")` → No validado
4. `addHeader(null, "value")` → Línea HashMap permite NULL keys
5. `addHeader("key", null)` → Línea HashMap permite NULL values
6. `setBody(null)` → No validado
7. `executeRequest(null, "/path")` → NullPointerException
8. `executeRequest(HttpMethod.GET, null)` → Unirest falla sin mensaje claro
9. Timeout negativo → No validado
10. Response body gigante (>100MB) → Sin límite
11. Redirect loop infinito → Unirest tiene límite pero no logueamos
12. SSL certificate expired → Manejado pero sin test

**Métodos críticos sin testear:**
- `executeRequest()` - 200+ líneas de lógica
- `sanitizeRequestBody()` - Masking de passwords
- `sanitizeResponse()` - Masking de tokens
- `buildCompleteUrl()` - Construcción de URL
- `handleUnirestException()` - Manejo de errores

**Tests críticos:**
```java
@Test
void testSetHostWithNull() {
    BaseHttpClient client = new BaseHttpClient();
    assertThrows(IllegalArgumentException.class, 
        () -> client.setHost(null));
}

@Test
void testExecuteRequestWithMalformedUrl() {
    BaseHttpClient client = new BaseHttpClient();
    client.setHost("not-a-valid-url");
    assertThrows(FrameworkTechnicalException.class,
        () -> client.get("/path"));
}

@Test
void testSanitizePasswordInRequestBody() {
    BaseHttpClient client = new BaseHttpClient();
    String body = "{\"user\":\"john\",\"password\":\"secret123\"}";
    // Debe loguear: {"user":"john","password":"***HIDDEN***"}
    // Verificar que el log NO contenga "secret123"
}

@Test
void testResponseBodyTooLarge() {
    // Response de 500MB
    // ACTUALMENTE: Carga todo en memoria (OutOfMemoryError)
    // ESPERADO: Límite configurable + warning
}
```

---

### **WebDriverManager.java** (564 líneas - Gestión de Drivers)

**Casos edge detectados:**
1. Cache corrupto (3KB HTML en lugar de driver) → Línea 308-324 (validado ✅)
2. Driver sin permisos de ejecución → Línea 384-387 (warning pero continúa)
3. Artifactory retorna HTML (text/html) → Línea 357-365 (validado ✅)
4. Content-Type NULL → Línea 358 (puede fallar)
5. File size overflow (>2GB) → Línea 376 (puede causar problemas)
6. OS no detectado → Línea 466-480 (lanza IllegalArgumentException - OK)
7. Driver en PATH no ejecutable → Línea 235-241 (no validado)
8. Artifactory URL sin base URL configurada → Línea 435 (retorna "NO_CONFIGURADO")
9. SSL handshake failure → Línea 318 (SSLUtils retorna null, continúa sin SSL)
10. Timeout en descarga → Línea 312 (configurado en 60s default)

**Tests críticos faltantes:**
```java
@Test
void testCacheWithCorruptedDriver() {
    // Simular cache con archivo HTML de 3KB
    // DEBE: Detectar size < 100KB y re-descargar
}

@Test
void testDownloadWithNullContentType() {
    // Response sin Content-Type header
    // DEBE: Continuar (no es crítico)
}

@Test
void testDriverWithoutExecutePermissions() {
    // Driver descargado sin chmod +x
    // ACTUALMENTE: Warning + continúa
    // ESPERADO: Selenium falla al intentar ejecutar
}

@Test
void testArtifactoryBaseUrlNotConfigured() {
    // driver.artifactory.base.url no existe
    // ACTUALMENTE: Construye URL con "NO_CONFIGURADO"
    // ESPERADO: Falla con mensaje claro
}
```

---

### **WebHelper.java** (1500+ líneas - Clase MÁS CRÍTICA)

**Revisión de métodos públicos:**
- 80+ métodos públicos expuestos
- Interacciones complejas con WebDriver
- Validaciones con assertions

**Casos edge críticos:**
1. `findElement()` con locator inválido
2. `click()` en elemento invisible
3. `sendKeys()` en elemento disabled
4. `waitForElement()` timeout excedido
5. `validateText()` con special characters
6. `saveVariable()` con key duplicada
7. `getVariable()` con key no existente
8. `takeScreenshot()` con directorio sin permisos
9. `validateAdditionVariables()` con valores no numéricos
10. `replaceVariablesInText()` con variables no encontradas

**Tests críticos (TOP 20 más usados):**
```java
// Interacciones básicas (más usados)
@Test void testClickElementNotVisible()
@Test void testClickElementStale()
@Test void testSendKeysToDisabledElement()
@Test void testClearFieldNotEditable()

// Validaciones (más críticas)
@Test void testValidateTextWithSpecialChars()
@Test void testValidateElementVisibleButOffscreen()
@Test void testValidateTextTimeout()

// Variables (más complejas)
@Test void testSaveVariableOverwriteExisting()
@Test void testGetVariableNotFound()
@Test void testReplaceVariablesCircularReference()

// Screenshots (más usados)
@Test void testTakeScreenshotDirectoryNotExists()
@Test void testTakeScreenshotNoWritePermission()
```

---

## 🛠️ MEJORAS DE CÓDIGO DETECTADAS

### **1. DatabaseConfig - Validación de parámetros**

**Actual:**
```java
public static DataSource createHikariDataSource(String jdbcUrl, String user, ...) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);  // ⚠️ No valida NULL
    // ...
}
```

**Mejora sugerida:**
```java
public static DataSource createHikariDataSource(String jdbcUrl, String user, ...) {
    // Validaciones al inicio
    if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
        throw new IllegalArgumentException("JDBC URL no puede ser null o vacío");
    }
    if (driverClassName == null || driverClassName.trim().isEmpty()) {
        throw new IllegalArgumentException("Driver class name no puede ser null");
    }
    if (maxPoolSize <= 0) {
        throw new IllegalArgumentException("maxPoolSize debe ser > 0, recibido: " + maxPoolSize);
    }
    if (minIdle > maxPoolSize) {
        throw new IllegalArgumentException(
            String.format("minIdle (%d) no puede ser mayor que maxPoolSize (%d)", 
                minIdle, maxPoolSize));
    }
    // ... continuar
}
```

---

### **2. SecurityUtilities - Validar longitud de password**

**Actual:**
```java
public static String maskPassword(String password) {
    if (password == null || password.isEmpty()) {
        return "***EMPTY***";
    }
    if (password.length() <= 4) {
        return "***HIDDEN***";
    }
    return password.substring(0, 2) + "***" + password.substring(password.length() - 2);
}
```

**Mejora sugerida:**
```java
public static String maskPassword(String password) {
    if (password == null || password.isEmpty()) {
        return "***EMPTY***";
    }
    
    int len = password.length();
    
    // Casos cortos: ocultar todo
    if (len <= 4) {
        return "***HIDDEN***";
    }
    
    // Casos medianos (5-8 chars): mostrar solo primer char
    if (len <= 8) {
        return password.charAt(0) + "***";
    }
    
    // Casos largos: mostrar primeros 2 y últimos 2
    return password.substring(0, 2) + "***" + password.substring(len - 2);
}
```

---

### **3. BaseHttpClient - Límite de response size**

**Mejora sugerida:**
```java
private static final long MAX_RESPONSE_SIZE = 100 * 1024 * 1024; // 100MB

private void validateResponseSize(HttpResponse<?> response) {
    long contentLength = response.getHeaders().getFirst("Content-Length")
        .map(Long::parseLong)
        .orElse(0L);
        
    if (contentLength > MAX_RESPONSE_SIZE) {
        throw new FrameworkTechnicalException("executeRequest",
            String.format("Response demasiado grande: %d MB (max: %d MB)",
                contentLength / (1024*1024), MAX_RESPONSE_SIZE / (1024*1024)));
    }
}
```

---

### **4. WebDriverManager - Validar base URL de Artifactory**

**Actual (Línea 435):**
```java
String baseUrl = config.get("driver.artifactory.base.url", "NO_CONFIGURADO");
```

**Mejora sugerida:**
```java
String baseUrl = config.get("driver.artifactory.base.url");
if (baseUrl == null || baseUrl.equals("NO_CONFIGURADO")) {
    throw new DriverNotFoundException(
        "driver.artifactory.base.url no configurado. " +
        "Configura ARTIFACTORY_BASE_URL en .env.local o " +
        "driver.artifactory.base.url en config-scotia.properties");
}
```

---

### **5. DriverManager - Limpiar ThreadLocal**

**Actual:**
```java
public static void quitDriver() {
    WebDriver driver = driverThreadLocal.get();
    if (driver != null) {
        driver.quit();
        // ⚠️ FALTA: driverThreadLocal.remove();
    }
}
```

**Mejora sugerida:**
```java
public static void quitDriver() {
    WebDriver driver = driverThreadLocal.get();
    if (driver != null) {
        try {
            driver.quit();
        } finally {
            driverThreadLocal.remove();  // ← CRÍTICO: Evitar memory leak
        }
    }
}
```

---

### **6. WebHelper - Validar locator antes de usar**

**Mejora sugerida:**
```java
private By parseLocator(String locator) {
    if (locator == null || locator.trim().isEmpty()) {
        throw new IllegalArgumentException("Locator no puede ser null o vacío");
    }
    
    // Validar XPath syntax
    if (locator.startsWith("/") || locator.startsWith("(")) {
        try {
            XPathFactory.newInstance().newXPath().compile(locator);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("XPath inválido: " + locator, e);
        }
    }
    
    // ... continuar con parsing
}
```

---

## 🎯 RESUMEN FINAL

### Tests CRÍTICOS (implementar en Semana 1):

| Test Class | Tests | Días | Vulnerabilidades Cubiertas |
|------------|-------|------|----------------------------|
| SecurityUtilitiesTest | 30 | 2 | 🔴🔴🔴 Seguridad sin validar |
| BaseHttpClientTest | 50 | 3 | 🔴🔴 Cliente HTTP sin tests |
| DatabaseConfigTest | 40 | 2 | 🔴 Windows Auth nuevo |
| WebDriverManagerTest (exp.) | 40 | 2 | 🔴 Cache + Artifactory |
| **TOTAL SEMANA 1** | **160** | **9** | **4 vulnerabilidades críticas** |

### Mejoras de Código (implementar progresivamente):

1. ✅ Validación de parámetros en DatabaseConfig
2. ✅ Límite de response size en BaseHttpClient
3. ✅ ThreadLocal.remove() en DriverManager
4. ✅ Validación de Artifactory base URL
5. ✅ Validación de XPath syntax en WebHelper
6. ✅ Mejora de isSecurePassword() complejidad

---

**¿Iniciamos con la implementación de SecurityUtilitiesTest (P0 más crítico)?** 🚀


