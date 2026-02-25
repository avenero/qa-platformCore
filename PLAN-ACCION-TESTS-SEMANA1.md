# PLAN DE ACCION: Unit Tests Framework - Semana 1

**Fecha:** 23 de Febrero 2026  
**Sprint:** Semana 1 - Tests Criticos  
**Objetivo:** Cobertura 19% → 35% (+16%)  
**Tiempo:** 9 dias laborables

---

## CONTEXTO

**Problema:** Coverage actual 19% (objetivo: 70%)  
**Root cause:** 87 clases, solo 11 con tests (13%)  
**Riesgo:** Codigo critico sin validacion (seguridad, HTTP, BD)

---

## TESTS A IMPLEMENTAR (SEMANA 1)

### DIA 1-2: SecurityUtilitiesTest.java (P0 - CRITICO)

**Clase:** `common/src/main/java/com/scotia/qa/common/utils/SecurityUtilities.java`  
**Lineas:** 176  
**Cobertura actual:** 0%  
**Riesgo:** 🔴🔴🔴 Codigo de seguridad sin tests

**Tests (30):**
```
# Generacion de tokens
- testGenerateSecureToken()
- testGenerateSecureTokenLength()
- testGenerateSecureTokenUniqueness()
- testGenerateSecureTokenNegativeLength()
- testGenerateSecureTokenZeroLength()

# Generacion de passwords
- testGenerateSecurePassword()
- testGenerateSecurePasswordLength()
- testGenerateSecurePasswordComplexity()
- testGenerateSecurePasswordNegativeLength()

# Masking
- testMaskPasswordNull()
- testMaskPasswordEmpty()
- testMaskPassword1Char()
- testMaskPassword2Chars()
- testMaskPassword4Chars()
- testMaskPassword5Chars()
- testMaskPassword20Chars()
- testMaskTokenNull()
- testMaskTokenEmpty()
- testMaskTokenShort()

# Hashing
- testSha256HashSuccess()
- testSha256HashNull()
- testSha256HashEmpty()
- testSha256HashLargeString()
- testSha256HashConsistency()

# Validacion passwords
- testIsSecurePasswordNull()
- testIsSecurePasswordShort()
- testIsSecurePasswordValid()
- testIsSecurePasswordWeakAllLowercase() ⚠️ BUG
- testIsSecurePasswordWeakNoNumbers() ⚠️ BUG
- testIsSecurePasswordWeakNoSymbols() ⚠️ BUG
```

**Vulnerabilidades a cubrir:**
- 🔴 `isSecurePassword()` NO valida complejidad (acepta "aaaaaaaa")
- 🔴 `generateSecureToken(-5)` causa NegativeArraySizeException
- 🔴 `sha256Hash(null)` causa NullPointerException

---

### DIA 3-5: BaseHttpClientTest.java (P0 - CRITICO)

**Clase:** `api-core/src/main/java/com/scotia/qa/apicore/implementations/BaseHttpClient.java`  
**Lineas:** 1421  
**Cobertura actual:** 0%  
**Riesgo:** 🔴🔴 Cliente HTTP core sin validacion

**Tests (50):**
```
# Configuracion basica
- testSetHost()
- testSetHostNull()
- testSetHostEmpty()
- testSetHostInvalidUrl()
- testSetHostWithProtocol()
- testSetHostWithoutProtocol()

# Headers
- testAddHeader()
- testAddHeaderNull()
- testAddMultipleHeaders()
- testRemoveHeader()
- testClearHeaders()
- testHeadersThreadSafe()

# Body
- testSetBody()
- testSetBodyNull()
- testSetBodyEmpty()
- testSetBodyLarge() (>10MB)

# Metodos HTTP
- testExecuteGet()
- testExecutePost()
- testExecutePut()
- testExecutePatch()
- testExecuteDelete()
- testExecuteHead()
- testExecuteOptions()

# Timeouts
- testSetConnectionTimeout()
- testSetConnectionTimeoutNegative() ⚠️
- testSetReadTimeout()
- testConnectionTimeoutExceeded()
- testReadTimeoutExceeded()

# SSL
- testExecuteWithSSL()
- testExecuteWithInvalidCertificate()
- testExecuteWithTruststore()

# Redirects
- testFollowRedirects()
- testNoFollowRedirects()
- testMaxRedirectsExceeded()

# Errores HTTP
- test404NotFound()
- test500ServerError()
- testConnectionRefused()
- testHostNotResolved()

# Sanitizacion
- testSanitizePasswordInBody() ⚠️ CRITICO
- testSanitizeTokenInHeader() ⚠️ CRITICO
- testSanitizeUrlWithCredentials()

# Response
- testParseJsonResponse()
- testParseXmlResponse()
- testParseHtmlResponse()
- testResponseBodyTooLarge() ⚠️
- testResponseBodyNull()

# Cookies
- testAddCookie()
- testGetCookies()
- testClearCookies()
```

**Vulnerabilidades a cubrir:**
- 🔴 `setHost(null)` no validado
- 🔴 Response >100MB puede causar OutOfMemoryError
- 🔴 Timeout negativo no validado
- 🔴 Sanitizacion de passwords puede no cubrir todos los patterns

---

### DIA 6-7: DatabaseConfigTest.java (P0 - CRITICO)

**Clase:** `common/src/main/java/com/scotia/qa/common/database/config/DatabaseConfig.java`  
**Lineas:** 195  
**Cobertura actual:** 0%  
**Riesgo:** 🔴 Windows Authentication sin tests (codigo nuevo)

**Tests (40):**
```
# Creacion DataSource - Oracle
- testCreateOracleDataSource()
- testCreateOracleDataSourceWithNullUrl()
- testCreateOracleDataSourceWithNullUser()
- testCreateOracleDataSourceWithEmptyPassword()

# Creacion DataSource - SQL Server
- testCreateSQLServerDataSource()
- testCreateSQLServerWithWindowsAuth()
- testCreateSQLServerWindowsAuthWithUsername() ⚠️
- testCreateSQLServerWithoutIntegratedSecurity()

# Creacion DataSource - PostgreSQL
- testCreatePostgreSQLDataSource()
- testCreatePostgreSQLWithNullPassword()

# Creacion DataSource - MySQL
- testCreateMySQLDataSource()

# Pool configuration
- testMaxPoolSizeNegative() ⚠️
- testMaxPoolSizeZero() ⚠️
- testMinIdleGreaterThanMaxPool() ⚠️
- testConnectionTimeoutConfiguration()
- testIdleTimeoutConfiguration()
- testMaxLifetimeConfiguration()
- testLeakDetectionThreshold()

# Test queries
- testGetConnectionTestQueryOracle()
- testGetConnectionTestQuerySQLServer()
- testGetConnectionTestQueryPostgreSQL()
- testGetConnectionTestQueryMySQL()
- testGetConnectionTestQueryUnknown()

# Database type detection
- testExtractDatabaseTypeOracle()
- testExtractDatabaseTypeSQLServer()
- testExtractDatabaseTypePostgreSQL()
- testExtractDatabaseTypeMySQL()
- testExtractDatabaseTypeUnknown()

# Deteccion Windows Auth
- testDetectWindowsAuthTrue()
- testDetectWindowsAuthFalse()
- testDetectWindowsAuthCaseInsensitive()
- testDetectWindowsAuthWithNullUrl()

# Masking
- testMaskPasswordInUrl()
- testMaskPasswordInConnectionString()

# Errores
- testCreateDataSourceInvalidDriver()
- testCreateDataSourceConnectionFailed()
- testCreateDataSourceWithInvalidUrl()
```

**Vulnerabilidades a cubrir:**
- 🔴 `jdbcUrl` NULL causa NullPointerException
- 🔴 `maxPoolSize <= 0` no validado
- 🔴 `minIdle > maxPoolSize` causa configuracion invalida
- 🔴 Windows Auth con username seteado (silenciosamente incorrecto)

---

### DIA 8-9: WebDriverManagerTest.java EXPANDIR (P0)

**Clase:** `common/src/main/java/com/scotia/qa/common/driver/WebDriverManager.java`  
**Lineas:** 564  
**Cobertura actual:** 5% (1 test basico existente)  
**Riesgo:** 🔴 Cache + Artifactory sin validar

**Tests adicionales (40):**
```
# Cache (NUEVO - sin tests)
- testCacheDriverFound() ⚠️
- testCacheDriverInvalid()
- testCacheDriverCorrupted3KB() ⚠️
- testCacheDriverNotExecutable()
- testCacheDriverReuse()
- testCacheDriverOverwrite()

# Descarga Artifactory
- testDownloadFromArtifactorySuccess()
- testDownloadReturnsHTML() ⚠️ CRITICO
- testDownloadFileTooSmall() ⚠️ CRITICO
- testDownloadContentTypeTextHtml() ⚠️ CRITICO
- testDownloadContentTypeNull()
- testDownload404()
- testDownload403()
- testDownload401()
- testDownloadSSLError()
- testDownloadTimeout()
- testDownloadRetryMechanism()
- testDownloadAfter3Retries()

# Construccion URL
- testBuildArtifactoryUrlMac()
- testBuildArtifactoryUrlWindows()
- testBuildArtifactoryUrlLinux()
- testBuildArtifactoryUrlBaseUrlNull() ⚠️
- testDetectOperatingSystemMac()
- testDetectOperatingSystemWindows()
- testDetectOperatingSystemLinux()
- testDetectOperatingSystemUnknown()

# Estrategia LOCAL
- testGetDriverFromLocalPath()
- testGetDriverFromLocalPathNotFound()
- testGetDriverFromLocalPathNotExecutable()

# SSL
- testDownloadWithSSLTruststore()
- testDownloadWithSSLFallback()

# Permisos
- testSetExecutablePermissionSuccess()
- testSetExecutablePermissionFailed()

# Path sistema
- testFindDriverInSystemPath()
- testFindDriverInSystemPathNotFound()

# Integracion
- testGetDriverFullFlowArtifactory()
- testGetDriverFullFlowLocal()
- testGetDriverFullFlowFallbackToPath()
```

**Vulnerabilidades a cubrir:**
- 🔴 Cache con archivo HTML 3KB no detectado (AHORA SI - validar test)
- 🔴 Base URL "NO_CONFIGURADO" construye URL invalida
- 🔴 Permisos ejecutables fallan silenciosamente

---

## RESULTADO ESPERADO (SEMANA 1)

### Coverage:
- Antes: 19%
- Despues: 35%
- Delta: +16%

### Tests:
- Antes: 11 tests
- Despues: 171 tests
- Delta: +160 tests

### Vulnerabilidades resueltas:
- 🔴🔴🔴 SecurityUtilities validado
- 🔴🔴 BaseHttpClient validado
- 🔴 DatabaseConfig Windows Auth validado
- 🔴 WebDriverManager cache validado

---

## ORDEN DE IMPLEMENTACION (Optimizado)

### Lunes-Martes: SecurityUtilitiesTest
- Crear clase test
- Implementar 30 tests
- Ejecutar y validar coverage
- **Detectar BUG:** isSecurePassword() acepta "aaaaaaaa"
- **Fix:** Agregar validacion de complejidad

### Miercoles-Jueves-Viernes: BaseHttpClientTest  
- Crear clase test
- Implementar 50 tests (10 por dia)
- Mockear Unirest para tests unitarios
- Validar sanitizacion de passwords

### Lunes-Martes (Semana 2): DatabaseConfigTest
- Crear clase test
- Implementar 40 tests
- Testear con H2 in-memory
- Validar Windows Auth

### Miercoles-Jueves (Semana 2): WebDriverManagerTest
- Expandir test existente
- Agregar 40 tests
- Mockear HttpURLConnection
- Validar cache corrupto

---

## CHECKLIST PRE-IMPLEMENTACION

Antes de crear cada test, verificar:

- [ ] Clase a testear identificada
- [ ] Dependencias analizadas (Unirest, HikariCP, etc.)
- [ ] Mocks necesarios identificados
- [ ] Casos edge documentados
- [ ] Vulnerabilidades a cubrir listadas
- [ ] Assertions esperadas definidas

---

## METRICAS DE EXITO

Al finalizar Semana 1:

- [ ] 160 tests nuevos implementados
- [ ] Coverage >= 35%
- [ ] 0 tests fallando
- [ ] 4 vulnerabilidades criticas cubiertas
- [ ] Pipeline Quality Gate pasando (warnings OK)
- [ ] Documentacion actualizada

---

## PROXIMOS PASOS (Semana 2+)

Ver documento principal: ESTUDIO-UNIT-TESTS-FRAMEWORK.md

- Sprint 2: Utils & DataUtilities (50% coverage)
- Sprint 3: API-Core helpers (60% coverage)
- Sprint 4: Web-Core helpers (70% coverage)

