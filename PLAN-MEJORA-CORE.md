# 🔧 PLAN DE MEJORA DEL CORE - Scotia QA Framework

> **Objetivo:** Eliminar warnings de compilación y agregar cobertura de tests crítica  
> **Fecha:** 2026-02-10  
> **Autor:** Abel Venero  
> **Prioridad:** Alta - Calidad del código y mantenibilidad

---

## 📊 RESUMEN EJECUTIVO

### **Estado Actual:**
```
✅ Código funcional y completo
⚠️  3 tipos de warnings de compilación
❌ 0 tests unitarios (cobertura: 0%)
```

### **Objetivos:**
```
✅ Eliminar 100% de warnings
✅ Cobertura mínima: 70% en módulos críticos
✅ Actualizar APIs deprecadas
⏱️  Tiempo estimado: 40-60 horas
```

---

# PARTE 1: ANÁLISIS DE WARNINGS

---

## 🐛 WARNING 1: Tipos Genéricos Raw (Common)

### **Ubicación:** `common/src/main/java/`

### **Archivos Afectados:**

#### **1.1. DataUtilities.java** - ⚠️ **ALTA PRIORIDAD**
**Líneas problemáticas:**
- Línea 105: `private static final Map variableStore`
- Línea 107: `private static final Map objectStore`
- Línea 109-110: `private static final Map namespacedVariableStore`
- Línea 113-114: `private static final Map namespacedObjectStore`

**Impacto:** 
- 💥 **CRÍTICO** - Clase central usada por todos los módulos
- ⚠️ Type safety comprometida
- 🔧 Fácil de corregir

**Corrección:**
```java
// ❌ ANTES (Raw types)
private static final Map<String, String> variableStore = new ConcurrentHashMap<>();
private static final Map<String, Object> objectStore = new ConcurrentHashMap<>();

// ✅ DESPUÉS (Genéricos explícitos)
private static final ConcurrentHashMap<String, String> variableStore = new ConcurrentHashMap<>();
private static final ConcurrentHashMap<String, Object> objectStore = new ConcurrentHashMap<>();
```

**Tests necesarios:** ✅ **SÍ - CRÍTICO**

---

#### **1.2. BaseConfigurationProvider.java**
**Líneas problemáticas:**
- Línea 96: `private final Map configurationCache`
- Línea 97: `private final Map propertiesCache`

**Corrección:**
```java
// ❌ ANTES
private final Map<String, Map<String, Object>> configurationCache = new ConcurrentHashMap<>();

// ✅ DESPUÉS
private final ConcurrentHashMap<String, Map<String, Object>> configurationCache = new ConcurrentHashMap<>();
```

**Tests necesarios:** ✅ **SÍ - ALTA**

---

#### **1.3. HttpResponse.java**
**Líneas problemáticas:**
- Línea 14: `private Map<String, String> headers`

**Corrección:**
```java
// ❌ ANTES
private Map<String, String> headers;

// ✅ DESPUÉS
private final Map<String, String> headers;
// Constructor inicializa con HashMap
```

**Tests necesarios:** ✅ **SÍ - MEDIA**

---

## 🐛 WARNING 2: APIs Deprecadas de Selenium (Web-Core)

### **Ubicación:** `web-core/src/main/java/`

### **Archivos Afectados:**

#### **2.1. WebHelper.java** - ⚠️ **ALTA PRIORIDAD**
**Métodos deprecados detectados:**

**Problema 1: `driver.findElement(By.xpath(...))` sin protección**
```java
// ❌ DEPRECADO (Selenium 4.6+)
WebElement element = driver.findElement(By.xpath("//*[contains(text(), '" + text + "')]"));

// ✅ ACTUALIZADO
WebElement element = driver.findElement(
    RelativeLocator.with(By.tagName("*"))
    .near(By.xpath("//body"))
);
// O mejor: usar Wait explícito
WebElement element = wait.until(
    ExpectedConditions.presenceOfElementLocated(By.xpath("..."))
);
```

**Problema 2: Xpath dinámico inseguro**
```java
// ❌ MAL (línea 1014) - Inyección XPath
By.xpath("//*[contains(text(), '" + text + "')]")

// ✅ BIEN - Xpath parametrizado
By.xpath(String.format("//*[contains(text(), '%s')]", 
    text.replace("'", "\\'")))
```

**Tests necesarios:** ✅ **SÍ - CRÍTICO**

---

#### **2.2. BasePage.java**
**Problema:** `WebDriverWait` constructor deprecado
```java
// ❌ DEPRECADO (Selenium 4.0+)
this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));

// ✅ ACTUALIZADO (ya está correcto) ✓
// NO HAY CAMBIO NECESARIO - Ya usa Duration
```

**Tests necesarios:** ⚠️ **MEDIA - Validar timeout**

---

#### **2.3. WaitUtils.java** (si existe)
**Buscar:** `FluentWait`, `WebDriverWait` con tipos deprecados

**Tests necesarios:** ✅ **SÍ - ALTA**

---

## 🐛 WARNING 3: MobileDriverFactory.java Deprecado (Mobile-Core)

### **Ubicación:** `mobile-core/src/main/java/com/scotia/qa/mobilecore/driver/MobileDriverFactory.java`

### **Problemas Detectados:**

#### **3.1. Constructor de AndroidDriver/IOSDriver deprecado**
```java
// ❌ DEPRECADO (Appium 8.0+)
driver = new AndroidDriver(serverUrl, capabilities);
driver = new IOSDriver(serverUrl, capabilities);

// ✅ ACTUALIZADO
driver = new AndroidDriver(serverUrl, getAndroidOptions(capabilities));
driver = new IOSDriver(serverUrl, getIOSOptions(capabilities));

// Método helper necesario:
private static UiAutomator2Options getAndroidOptions(DesiredCapabilities caps) {
    UiAutomator2Options options = new UiAutomator2Options();
    caps.asMap().forEach((key, value) -> 
        options.setCapability(key, value)
    );
    return options;
}
```

**Tests necesarios:** ✅ **SÍ - CRÍTICO**

---

#### **3.2. DesiredCapabilities deprecado**
```java
// ❌ DEPRECADO
public static AppiumDriver createDriver(Platform platform, 
    DesiredCapabilities capabilities, String appiumUrl)

// ✅ ACTUALIZADO
public static AppiumDriver createDriver(Platform platform, 
    AbstractDriverOptions<?> options, String appiumUrl)
```

**Tests necesarios:** ✅ **SÍ - ALTA**

---

# PARTE 2: IDENTIFICACIÓN DE ÁREAS CRÍTICAS SIN TESTS

---

## 📊 ANÁLISIS DE COBERTURA ACTUAL

### **Estado:**
```
Total de archivos Java: ~50
Total de tests: 0
Cobertura: 0%
```

### **Módulos sin Tests:**
```
❌ common/       - 0 tests (0% cobertura)
❌ api-core/     - 0 tests (0% cobertura)
❌ web-core/     - 0 tests (0% cobertura)
❌ mobile-core/  - 0 tests (0% cobertura)
```

---

## 🎯 CLASES CRÍTICAS QUE NECESITAN TESTS

### **Prioridad 1: CRÍTICA (deben tener tests)**

#### **1. DataUtilities.java** (common) 💥
**Razón:** Clase central, usada en todos los módulos
**Complejidad:** Alta (2,099 líneas)
**Métodos críticos:**
- `storeVariable()` - Almacenamiento thread-safe
- `getVariable()` - Recuperación de variables
- `replaceVariables()` - Reemplazo en strings
- `getByJsonPath()` - Navegación JSON
- `deserializeJson()` - Deserialización segura

**Tests recomendados:** 30-40 test cases
**Cobertura objetivo:** 85%

---

#### **2. BaseDatabaseService.java** (api-core) 💥
**Razón:** Interacción con BD, alto riesgo de SQL injection
**Complejidad:** Alta (447 líneas)
**Métodos críticos:**
- `queryForMap()` - Consultas parametrizadas
- `queryForList()` - Resultados múltiples
- `executeBatch()` - Operaciones batch
- `validateSql()` - Seguridad SQL

**Tests recomendados:** 25-35 test cases
**Cobertura objetivo:** 80%

---

#### **3. BaseConfigurationProvider.java** (common) 💥
**Razón:** Configuración global, afecta todo el framework
**Complejidad:** Alta (630 líneas)
**Métodos críticos:**
- `loadConfiguration()` - Carga desde múltiples fuentes
- `getConfigurationValue()` - Acceso con tipos genéricos
- `getConfigurationList()` - Listas tipadas
- `convertValue()` - Conversión type-safe

**Tests recomendados:** 20-30 test cases
**Cobertura objetivo:** 75%

---

#### **4. MobileDriverFactory.java** (mobile-core) 💥
**Razón:** Inicialización de drivers, fallas bloquean tests
**Complejidad:** Media (67 líneas)
**Métodos críticos:**
- `createDriver()` - Factory principal
- `createAndroidDriver()` - Driver Android
- `createIOSDriver()` - Driver iOS
- `configureDriver()` - Configuración post-creación

**Tests recomendados:** 15-20 test cases
**Cobertura objetivo:** 90%

---

#### **5. WebDriverFactory.java** (web-core) 💥
**Razón:** Inicialización de navegadores, crítico para Web
**Complejidad:** Alta (401 líneas)
**Métodos críticos:**
- `createDriver()` - Factory con config
- `createLocalDriver()` - Drivers locales
- `createGridDriver()` - Selenium Grid
- `getChromeOptions()` - Opciones Chrome

**Tests recomendados:** 20-25 test cases
**Cobertura objetivo:** 80%

---

### **Prioridad 2: ALTA (importante pero no bloquea)**

#### **6. WebHelper.java** (web-core) ⚠️
**Razón:** 1,200+ líneas, muchas operaciones DOM
**Métodos críticos:**
- `getElement()` - Localización con retry
- `clickElement()` - Click con manejo de errores
- `sendKeys()` - Input con validación
- `clickElementInShadow()` - Shadow DOM

**Tests recomendados:** 35-45 test cases
**Cobertura objetivo:** 70%

---

#### **7. HttpResponse.java** (common) ⚠️
**Razón:** Modelo compartido entre módulos
**Métodos críticos:**
- `isSuccessful()` - Validación status
- `getHeaders()` - Acceso inmutable
- Constructor con validación

**Tests recomendados:** 10-15 test cases
**Cobertura objetivo:** 95%

---

#### **8. CucumberResultAdapter.java** (common) ⚠️
**Razón:** Parsing de resultados, datos para reportes
**Métodos críticos:**
- `adaptResults()` - Transformación JSON
- `processFeature()` - Extracción de features
- `processScenario()` - Mapeo de scenarios

**Tests recomendados:** 15-20 test cases
**Cobertura objetivo:** 75%

---

### **Prioridad 3: MEDIA (deseable)**

#### **9. TableComponent.java** (web-core)
**Tests recomendados:** 8-10 test cases
**Cobertura objetivo:** 80%

#### **10. EvidenceCollector.java** (common)
**Tests recomendados:** 10-12 test cases
**Cobertura objetivo:** 75%

---

## 🎯 RESUMEN DE TESTS NECESARIOS

```
┌─────────────────────────┬──────────┬─────────────┬──────────────┐
│ Clase                   │ Prioridad│ Tests       │ Cobertura    │
├─────────────────────────┼──────────┼─────────────┼──────────────┤
│ DataUtilities           │ 💥 P1    │ 30-40       │ 85%          │
│ BaseDatabaseService     │ 💥 P1    │ 25-35       │ 80%          │
│ BaseConfigurationProvider│💥 P1    │ 20-30       │ 75%          │
│ MobileDriverFactory     │ 💥 P1    │ 15-20       │ 90%          │
│ WebDriverFactory        │ 💥 P1    │ 20-25       │ 80%          │
│ WebHelper               │ ⚠️  P2    │ 35-45       │ 70%          │
│ HttpResponse            │ ⚠️  P2    │ 10-15       │ 95%          │
│ CucumberResultAdapter   │ ⚠️  P2    │ 15-20       │ 75%          │
│ TableComponent          │ 📘 P3    │ 8-10        │ 80%          │
│ EvidenceCollector       │ 📘 P3    │ 10-12       │ 75%          │
├─────────────────────────┼──────────┼─────────────┼──────────────┤
│ **TOTAL**               │          │ **188-252** │ **~80% avg** │
└─────────────────────────┴──────────┴─────────────┴──────────────┘
```

---

# PARTE 3: PLAN DE EJECUCIÓN

---

## 📅 FASE 1: CORRECCIÓN DE WARNINGS (Semana 1)

### **Sprint 1.1: Common Module (2-3 días)**

**Día 1:**
- ✅ Corregir `DataUtilities.java`
  - Agregar tipos genéricos explícitos
  - Eliminar `@SuppressWarnings("unchecked")` innecesarios
  - Validar no rompe código existente

**Día 2:**
- ✅ Corregir `BaseConfigurationProvider.java`
  - Agregar tipos genéricos
  - Actualizar métodos con conversiones unsafe
  
- ✅ Corregir `HttpResponse.java`
  - Hacer `headers` final
  - Actualizar constructores

**Día 3:**
- ✅ Compilar y validar `common/`
- ✅ Ejecutar build completo
- ✅ Verificar 0 warnings en common

**Entregable:** ✅ Common sin warnings

---

### **Sprint 1.2: Web-Core Module (2 días)**

**Día 4:**
- ✅ Analizar APIs deprecadas en `WebHelper.java`
- ✅ Refactorizar métodos problemáticos
  - `checkTextAndClic()`
  - `clickElementInShadow()`
  - Xpath dinámicos inseguros

**Día 5:**
- ✅ Actualizar `BasePage.java` (si necesario)
- ✅ Compilar y validar web-core
- ✅ Verificar 0 warnings en web-core

**Entregable:** ✅ Web-Core sin warnings

---

### **Sprint 1.3: Mobile-Core Module (1 día)**

**Día 6:**
- ✅ Refactorizar `MobileDriverFactory.java`
  - Cambiar de `DesiredCapabilities` a `AbstractDriverOptions`
  - Actualizar `createAndroidDriver()`
  - Actualizar `createIOSDriver()`
  - Agregar métodos helper para conversión

**Entregable:** ✅ Mobile-Core sin warnings

---

### **Sprint 1.4: API-Core Module (1 día)**

**Día 7:**
- ✅ Revisar y corregir warnings en `api-core`
- ✅ Compilar módulo completo
- ✅ Ejecutar build full del proyecto
- ✅ **VALIDAR: 0 warnings en TODO el proyecto**

**Entregable:** ✅ **Proyecto 100% sin warnings**

---

## 📅 FASE 2: TESTS CRÍTICOS (Semanas 2-3)

### **Sprint 2.1: DataUtilities Tests (3 días)**

**Objetivo:** 85% cobertura

**Test Suite Structure:**
```
DataUtilitiesTest.java
├── Variable Storage Tests (8 tests)
│   ├── testStoreAndRetrieveVariable()
│   ├── testStoreVariableThreadSafe()
│   ├── testReplaceVariables()
│   └── testReplaceVariablesNested()
├── JSON Tests (12 tests)
│   ├── testDeserializeJson()
│   ├── testGetByJsonPath()
│   ├── testGetListByJsonPath()
│   └── testInvalidJson()
├── Namespace Tests (6 tests)
│   ├── testNamespacedStorage()
│   ├── testNamespaceIsolation()
│   └── testClearNamespace()
└── Edge Cases (10 tests)
    ├── testNullHandling()
    ├── testEmptyStrings()
    ├── testConcurrentAccess()
    └── testMemoryLeaks()
```

**Estimación:** 20-24 horas

---

### **Sprint 2.2: BaseDatabaseService Tests (3 días)**

**Objetivo:** 80% cobertura

**Test Suite Structure:**
```
BaseDatabaseServiceTest.java
├── Query Tests (10 tests)
│   ├── testQueryForMap()
│   ├── testQueryForList()
│   ├── testQueryWithParameters()
│   └── testQueryForCount()
├── Security Tests (8 tests)
│   ├── testSqlInjectionPrevention()
│   ├── testValidateSql()
│   └── testSanitizeSql()
├── Batch Tests (5 tests)
│   ├── testExecuteBatch()
│   ├── testBatchRollback()
│   └── testBatchPerformance()
└── Connection Tests (7 tests)
    ├── testConnectionTimeout()
    ├── testConnectionPool()
    └── testConnectionCleanup()
```

**Estimación:** 20-24 horas

---

### **Sprint 2.3: BaseConfigurationProvider Tests (2 días)**

**Objetivo:** 75% cobertura

**Test Suite Structure:**
```
BaseConfigurationProviderTest.java
├── Load Tests (6 tests)
│   ├── testLoadYaml()
│   ├── testLoadJson()
│   ├── testLoadProperties()
│   └── testLoadInvalidFormat()
├── Access Tests (8 tests)
│   ├── testGetConfigurationValue()
│   ├── testGetConfigurationList()
│   ├── testGetNestedValue()
│   └── testDefaultValue()
├── Type Conversion Tests (6 tests)
│   ├── testConvertToString()
│   ├── testConvertToInteger()
│   ├── testConvertToList()
│   └── testInvalidConversion()
└── Cache Tests (4 tests)
    ├── testCacheHit()
    ├── testCacheInvalidation()
    └── testCacheThreadSafe()
```

**Estimación:** 16-18 horas

---

### **Sprint 2.4: MobileDriverFactory Tests (2 días)**

**Objetivo:** 90% cobertura

**Test Suite Structure:**
```
MobileDriverFactoryTest.java
├── Creation Tests (8 tests)
│   ├── testCreateAndroidDriver()
│   ├── testCreateIOSDriver()
│   ├── testCreateWithOptions()
│   └── testInvalidPlatform()
├── Configuration Tests (6 tests)
│   ├── testConfigureDriver()
│   ├── testImplicitWait()
│   └── testCapabilitiesMapping()
└── Error Handling Tests (6 tests)
    ├── testNullCapabilities()
    ├── testInvalidUrl()
    └── testConnectionFailure()
```

**Estimación:** 14-16 horas

---

### **Sprint 2.5: WebDriverFactory Tests (2 días)**

**Objetivo:** 80% cobertura

**Test Suite Structure:**
```
WebDriverFactoryTest.java
├── Local Driver Tests (8 tests)
│   ├── testCreateChromeDriver()
│   ├── testCreateFirefoxDriver()
│   ├── testCreateEdgeDriver()
│   └── testHeadlessMode()
├── Grid Driver Tests (6 tests)
│   ├── testCreateGridDriver()
│   ├── testGridConnection()
│   └── testGridFailover()
├── Configuration Tests (6 tests)
│   ├── testChromeOptions()
│   ├── testFirefoxOptions()
│   └── testProxyConfiguration()
└── Error Handling Tests (5 tests)
    ├── testDriverNotFound()
    ├── testInvalidBrowser()
    └── testGridTimeout()
```

**Estimación:** 16-18 horas

---

## 📅 FASE 3: TESTS COMPLEMENTARIOS (Semana 4)

### **Sprint 3.1: Prioridad 2 Tests (4 días)**

**Clases:**
- WebHelper.java (35-45 tests) - 2 días
- HttpResponse.java (10-15 tests) - 1 día
- CucumberResultAdapter.java (15-20 tests) - 1 día

**Estimación:** 30-35 horas

---

### **Sprint 3.2: Prioridad 3 Tests (2 días)**

**Clases:**
- TableComponent.java (8-10 tests)
- EvidenceCollector.java (10-12 tests)

**Estimación:** 14-16 horas

---

## 📅 FASE 4: INTEGRACIÓN Y DOCUMENTACIÓN (Semana 5)

### **Sprint 4.1: Integración (2 días)**
- Ejecutar suite completa de tests
- Generar reporte de cobertura (Jacoco)
- Validar > 70% en módulos críticos
- Corregir tests fallidos

### **Sprint 4.2: Documentación (2 días)**
- Actualizar README con cobertura
- Documentar cómo ejecutar tests
- Crear guía de contribución con tests obligatorios
- Actualizar pipeline Jenkins con validación de cobertura

### **Sprint 4.3: Revisión (1 día)**
- Code review de todos los cambios
- Merge a rama `develop`
- Publicar versión 1.1.0 del framework

---

# PARTE 4: ESTIMACIONES Y RECURSOS

---

## ⏱️ TIEMPO ESTIMADO

```
┌─────────────────────────────┬──────────┬─────────────┐
│ Fase                        │ Duración │ Horas       │
├─────────────────────────────┼──────────┼─────────────┤
│ FASE 1: Warnings            │ 1 semana │ 35-40 horas │
│ FASE 2: Tests Críticos      │ 2 semanas│ 86-100 horas│
│ FASE 3: Tests Complementarios│1 semana │ 44-51 horas │
│ FASE 4: Integración/Docs    │ 1 semana │ 30-35 horas │
├─────────────────────────────┼──────────┼─────────────┤
│ **TOTAL**                   │**5 sem** │**195-226 h**│
└─────────────────────────────┴──────────┴─────────────┘
```

**Con dedicación:**
- **Full-time (8h/día):** 5-6 semanas
- **Part-time (4h/día):** 10-12 semanas
- **Team de 2 personas:** 3-4 semanas

---

## 👥 RECURSOS NECESARIOS

### **Humanos:**
- **1 Senior Developer** (corrección warnings + tests críticos)
- **1 QA Engineer** (tests complementarios + validación)
- **Code Reviewer** (0.5 FTE - revisión continua)

### **Herramientas:**
```
✅ JUnit 5 (ya incluido)
✅ Mockito (agregar)
✅ AssertJ (agregar)
✅ Jacoco (ya incluido)
✅ Testcontainers (para tests de BD)
✅ WireMock (para tests de API)
```

### **Dependencias a Agregar:**

```gradle
testImplementation 'org.mockito:mockito-core:5.8.0'
testImplementation 'org.mockito:mockito-junit-jupiter:5.8.0'
testImplementation 'org.assertj:assertj-core:3.24.2'
testImplementation 'org.testcontainers:testcontainers:1.19.3'
testImplementation 'org.testcontainers:postgresql:1.19.3'
testImplementation 'org.testcontainers:mysql:1.19.3'
testImplementation 'com.github.tomakehurst:wiremock-jre8:2.35.1'
```

---

## 📊 MÉTRICAS DE ÉXITO

### **Fase 1:**
```
✅ 0 warnings de compilación
✅ Build exitoso sin suppressWarnings
✅ Backward compatibility mantenida
```

### **Fase 2-3:**
```
✅ > 80% cobertura en clases P1
✅ > 70% cobertura en clases P2
✅ > 60% cobertura en clases P3
✅ 100% tests pasan en CI/CD
```

### **Fase 4:**
```
✅ Documentación completa
✅ Pipeline con quality gate
✅ Framework publicado v1.1.0
```

---

## 🎯 BENEFICIOS ESPERADOS

### **Corto Plazo (1-2 meses):**
- ✅ Código sin warnings (profesional)
- ✅ Detección temprana de bugs
- ✅ Refactorings seguros
- ✅ Onboarding más rápido

### **Mediano Plazo (3-6 meses):**
- ✅ Reducción de bugs en producción (-30%)
- ✅ Tiempo de debugging (-40%)
- ✅ Confianza en cambios (+50%)
- ✅ Velocidad de desarrollo (+20%)

### **Largo Plazo (6-12 meses):**
- ✅ Mantenibilidad mejorada
- ✅ Deuda técnica reducida
- ✅ Framework robusto y confiable
- ✅ Adopción por más equipos

---

## 🚀 PRÓXIMOS PASOS INMEDIATOS

### **Semana 1 - Día 1 (HOY):**
```
☐ 1. Revisar y aprobar este plan
☐ 2. Crear rama: feature/eliminate-warnings-add-tests
☐ 3. Actualizar build.gradle con dependencias de test
☐ 4. Configurar Jacoco en pipeline
☐ 5. Crear estructura de directorios para tests:
      - common/src/test/java/
      - api-core/src/test/java/
      - web-core/src/test/java/
      - mobile-core/src/test/java/
```

### **Semana 1 - Día 2:**
```
☐ 1. Comenzar Sprint 1.1: Corregir DataUtilities.java
☐ 2. Crear PR con primera corrección
☐ 3. Configurar pre-commit hooks (checkstyle)
```

---

## 📋 CHECKLIST DE ACEPTACIÓN

### **Para cada Warning Corregido:**
```
☐ Código sin @SuppressWarnings
☐ Tipos genéricos explícitos
☐ Build sin warnings
☐ Tests de regresión pasan
☐ Code review aprobado
```

### **Para cada Clase con Tests:**
```
☐ > objetivo de cobertura alcanzado
☐ Tests pasan localmente
☐ Tests pasan en Jenkins
☐ Edge cases cubiertos
☐ Documentación actualizada
```

---

**📞 Contacto:** Abel Venero  
**📅 Última Actualización:** 2026-02-10  
**📌 Versión:** 1.0.0  
**🎯 Estado:** ✅ LISTO PARA EJECUCIÓN

