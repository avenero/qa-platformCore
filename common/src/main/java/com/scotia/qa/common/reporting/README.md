# 📊 Sistema de Reporting - Scotia QA Framework

> **Sistema unificado de reportes que integra Extent Reports HTML y Jira/Xray para generación automática y distribución de resultados de tests.**

---

## 📑 Índice

- [Características](#-características)
- [Arquitectura](#-arquitectura)
- [Instalación](#-instalación)
- [Configuración](#️-configuración)
- [Guía de Uso](#-guía-de-uso)
- [Integración con Evidencias](#-integración-con-evidencias)
- [Comunicación con Jira/Xray](#-comunicación-con-jiraxray)
- [Pipeline Interno](#-pipeline-interno)
- [Ejemplos Completos](#-ejemplos-completos)
- [Troubleshooting](#-troubleshooting)
- [Preguntas Frecuentes](#-preguntas-frecuentes-faq)
- [API Reference](#-api-reference)
- [Referencias](#-referencias)

---

## 🎯 Características

| Característica | Descripción |
|----------------|-------------|
| **✅ Extent Reports** | Genera reportes HTML profesionales con screenshots embebidos |
| **✅ Jira/Xray Integration** | Actualiza status de tests y sube attachments automáticamente |
| **✅ Pipeline Modular** | Procesamiento en steps independientes (Chain of Responsibility) |
| **✅ Multi-formato** | Soporta Cucumber JSON, JUnit XML (extensible) |
| **✅ Evidencias Automáticas** | Integración transparente con EvidenceManager |
| **✅ Configuración Flexible** | Control granular de cada funcionalidad |
| **✅ Thread-Safe** | Soporta ejecución paralela de tests |
| **✅ Tolerante a Fallos** | Steps opcionales no abortan el pipeline |

---

## 🏗 Arquitectura

```
┌─────────────────────────────────────────────────────────────────────┐
│                         FLUJO COMPLETO                               │
└─────────────────────────────────────────────────────────────────────┘

1. TEST EXECUTION (Durante prueba)
   ↓
   WebSteps/ApiSteps → EvidenceManager.saveScreenshot()
                    → test-evidences/WEB/feature/scenario/screenshot_001.png
   
2. TEST COMPLETION (Cucumber genera cucumber.json)
   ↓
   
3. REPORTING (@AfterAll en módulo)
   ↓
   ReportingManager.processTestResults(cucumberJson)
   
   ┌─────────────────────────────────────────────┐
   │         PIPELINE (4 Steps)                  │
   ├─────────────────────────────────────────────┤
   │ Step 1: ConversionStep                      │
   │   • CucumberResultAdapter                   │
   │   • EvidenceCollector (automático)          │
   │   • TestExecutionResult generado            │
   ├─────────────────────────────────────────────┤
   │ Step 2: ExtentGenerationStep                │
   │   • ExtentReportGenerator                   │
   │   • HTML con screenshots embebidos (base64) │
   │   • build/reports/extent/report.html        │
   ├─────────────────────────────────────────────┤
   │ Step 3: JiraUpdateStatusStep                │
   │   • JiraUpdateService                       │
   │   • Actualiza PASS/FAIL en Jira/Xray        │
   ├─────────────────────────────────────────────┤
   │ Step 4: JiraUploadAttachmentsStep           │
   │   • JiraAttachmentService                   │
   │   • Sube screenshots y reporte HTML         │
   └─────────────────────────────────────────────┘
   
4. RESULTADO
   ✅ Reporte HTML generado
   ✅ Tests actualizados en Jira
   ✅ Evidencias adjuntas
```

### **Estructura de Paquetes**

```
reporting/
├── core/
│   ├── adapter/              # Conversión de formatos
│   │   ├── ResultAdapter.java
│   │   └── cucumber/
│   │       └── CucumberResultAdapter.java
│   ├── config/               # Configuraciones
│   │   ├── ReportingConfig.java
│   │   ├── ExtentConfig.java
│   │   └── JiraConfig.java
│   ├── model/                # Modelos de datos
│   │   ├── TestExecutionResult.java
│   │   ├── ScenarioResult.java
│   │   ├── Attachment.java
│   │   └── TestStatus.java
│   └── util/                 # Utilidades
│       ├── EvidenceCollector.java  # Puente EvidenceManager → Reporting
│       └── TagExtractor.java       # Extrae test keys desde tags
├── extent/
│   └── generator/
│       └── ExtentReportGenerator.java
├── jira/
│   ├── client/
│   │   └── JiraHttpClient.java
│   └── service/
│       ├── JiraUpdateService.java
│       └── JiraAttachmentService.java
└── manager/
    ├── ReportingManager.java      # ⭐ FACADE PRINCIPAL
    └── pipeline/
        ├── ReportingPipeline.java
        ├── ReportingStep.java
        └── steps/
            ├── ConversionStep.java
            ├── ExtentGenerationStep.java
            ├── JiraUpdateStatusStep.java
            └── JiraUploadAttachmentsStep.java
```

---

## 🚀 Instalación

### **En tu módulo `build.gradle`:**

```groovy
dependencies {
    // Framework Scotia QA (incluye reporting)
    testImplementation 'com.scotia.qa:common:1.0.0'
    testImplementation 'com.scotia.qa:api-core:1.0.0'  // Si usas API
    testImplementation 'com.scotia.qa:web-core:1.0.0'  // Si usas WEB
}
```

El sistema de reporting ya está incluido en `common`, no necesitas dependencias adicionales.

---

## ⚙️ Configuración

### **1. Archivo de Configuración**

Crear `src/test/resources/config-scotia.properties`:

```properties
# ============================================================================
# REPORTING CONFIGURATION
# ============================================================================

# Master Switch
reporting.enabled=true
reporting.environment=QA

# ============================================================================
# EXTENT REPORTS (Reportes HTML)
# ============================================================================

extent.enabled=true
extent.outputPath=build/reports/extent/
extent.reportName=execution-report.html
extent.documentTitle=Scotia QA - Test Results
extent.reportTitle=Automated Test Execution Report
extent.theme=STANDARD                    # STANDARD o DARK
extent.includeScreenshots=true
extent.includeSystemInfo=true
extent.includeTimeline=true

# ============================================================================
# JIRA/XRAY INTEGRATION
# ============================================================================

# Conexión (obligatorio si updateStatus=true o uploadReport=true)
jira.url=https://jira.agile.bns
jira.user=${JIRA_USER}                   # Desde variable de entorno
jira.password=${JIRA_PASSWORD}           # Desde variable de entorno

# Proyecto
jira.projectKey=QAAUY
jira.testExecutionId=${TEST_EXECUTION_ID}  # ej: QAAUY-640
jira.testEnvironment=QA

# Control Granular
jira.updateStatus=true                   # ¿Actualizar PASS/FAIL?
jira.uploadReport=true                   # ¿Subir HTML?
jira.includeEvidences=true               # ¿Adjuntar screenshots?
jira.maxAttachmentSizeMb=10
jira.failOnError=false                   # Continuar si Jira falla
jira.updateMode=BATCH                    # SINGLE o BATCH
```

### **2. Variables de Entorno**

Crear `.env.local` (no commitear):

```bash
# Jira Credentials
JIRA_USER=tu.usuario@scotia.com
JIRA_PASSWORD=tu_token_jira

# Test Execution
TEST_EXECUTION_ID=QAAUY-640
```

Cargar antes de ejecutar tests:

```bash
source .env.local
./gradlew test
```

---

## 📘 Guía de Uso

### **Paso 0: Configurar Cucumber Runner (IMPORTANTE)**

El framework incluye `CucumberReportingPlugin` que **DEBE** ser registrado en el runner de tu módulo:

```java
package com.module.runner;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(
    key = GLUE_PROPERTY_NAME,
    value = "com.scotia.qa.apicore.steps,com.module.hooks,com.module.steps"
)
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "json:target/cucumber-reports/cucumber.json, " +
            "html:target/cucumber-reports/cucumber.html, " +
            "pretty, " +
            "com.scotia.qa.common.reporting.cucumber.CucumberReportingPlugin"  // ← DEL FRAMEWORK
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@test"
)
public class RunCucumberTest {
}
```

**⚠️ IMPORTANTE:** 
- **NO CREAR** `ReportingPlugin.java` en tu módulo
- El plugin está **EN EL FRAMEWORK** (`common`)
- Solo registrarlo en `PLUGIN_PROPERTY_NAME`

---

### **Paso 1: Capturar Evidencias Durante Tests (Opcional pero Recomendado)**

```java
import com.scotia.qa.common.logging.EvidenceManager;

public class WebSteps {
    
    @When("hago clic en el botón {string}")

    public void hagoClicEnBoton(String buttonId) {
        driver.findElement(By.id(buttonId)).click();
        
        // Capturar screenshot
        byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        EvidenceManager.saveScreenshot(screenshot, "after_click_" + buttonId);
    }
}
```

### **Paso 2: Configurar Hooks de Cucumber**

```java
package com.module.hooks;

import com.scotia.qa.common.logging.EvidenceManager;
import com.scotia.qa.common.logging.LoggingInitializer;
import com.scotia.qa.common.logging.ModuleDetector;
import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.reporting.core.config.ReportingConfig;
import com.scotia.qa.common.reporting.extent.generator.ReportingManager;
import com.scotia.qa.common.reporting.manager.pipeline.PipelineResult;
import io.cucumber.java.*;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Hook unificado con Logging, Evidencias y Reporting.
 * 
 * <p><b>Responsabilidades:</b>
 * <ul>
 *   <li>Inicializar contexto de logging y módulo</li>
 *   <li>Establecer contexto de evidencias por scenario</li>
 *   <li>Capturar screenshots en caso de fallo</li>
 *   <li>Generar reportes Extent + actualizar Jira al finalizar suite</li>
 * </ul>
 */
public class Hook {
    
    // Inyectar WebDriver desde tu context (ajustar según tu implementación)
    // Ejemplo: private WebDriver driver = DriverManager.getDriver();
    
    /**
     * Hook que se ejecuta UNA VEZ antes de toda la suite.
     * Inicializa logging y contexto del módulo.
     */
    @BeforeAll
    public static void beforeAll() {
        String moduleName = ModuleDetector.detectModuleName();
        LoggingInitializer.initModuleContext(moduleName);
        
        TestLogger.logInfo("FRAMEWORK", "Sistema de logging inicializado",
                Map.of("module", moduleName));
    }
    
    /**
     * Hook que se ejecuta ANTES de cada scenario.
     * Establece contexto de logging + evidencias.
     */
    @Before(order = 0)
    public void before(Scenario scenario) {
        // 1. Contexto de logging
        LoggingInitializer.setTestContext(scenario.getName());
        
        // 2. Contexto de evidencias
        String framework = detectFramework(scenario.getSourceTagNames());
        String featureName = extractFeatureName(scenario.getUri().toString());
        
        EvidenceManager.setTestContext(
            framework, 
            featureName, 
            scenario.getName()
        );
        
        TestLogger.logInfo("SCENARIO_START", "Iniciando escenario", Map.of(
                "name", scenario.getName(),
                "framework", framework,
                "uri", scenario.getUri().toString(),
                "tags", scenario.getSourceTagNames()
        ));
    }
    
    /**
     * Hook que se ejecuta DESPUÉS de cada scenario.
     * Captura screenshot de error + limpia contextos.
     */
    @After(order = Integer.MAX_VALUE)
    public void after(Scenario scenario) {
        if (scenario.isFailed()) {
            // Capturar screenshot de error
            captureFailureScreenshot();
            
            TestLogger.logError("SCENARIO_FAILED", "Escenario falló", Map.of(
                    "name", scenario.getName(),
                    "status", scenario.getStatus().toString()
            ));
        } else {
            TestLogger.logInfo("SCENARIO_PASSED", "Escenario exitoso", Map.of(
                    "name", scenario.getName()
            ));
        }
        
        // Limpiar contextos
        EvidenceManager.clearTestContext();
        LoggingInitializer.clearTestContext();
    }
    
    /**
     * Hook que se ejecuta UNA VEZ después de toda la suite.
     * Genera reportes (Extent + Jira) y limpia contexto.
     */
    @AfterAll
    public static void afterAll() {
        TestLogger.logInfo("FRAMEWORK", "Suite de pruebas finalizada, generando reportes...", null);
        
        try {
            // 1. Obtener ruta del archivo cucumber.json
            Path moduleDir = Paths.get(System.getProperty("user.dir"));
            Path cucumberJsonPath = moduleDir.resolve("target/cucumber-reports/cucumber.json");
            
            System.out.println("🔍 Buscando cucumber.json en: " + cucumberJsonPath.toAbsolutePath());
            
            // 2. Esperar a que el archivo esté disponible y con contenido (retry logic)
            String cucumberJson = waitForCucumberJson(cucumberJsonPath);
            
            if (cucumberJson == null || cucumberJson.trim().isEmpty() || cucumberJson.equals("[]")) {
                System.err.println("❌ cucumber.json está VACÍO o no contiene scenarios");
                System.err.println("💡 Verificar que los tests se ejecutaron correctamente");
                return;
            }
            
            System.out.println("✅ cucumber.json cargado (" + cucumberJson.length() + " caracteres)");
            
            // 3. Cargar configuración de reporting
            ReportingConfig config = ReportingConfig.fromConfigManager();
            
            // 4. Inicializar ReportingManager
            ReportingManager.initialize(config);
            
            // 5. Procesar resultados (genera Extent + actualiza Jira)
            PipelineResult result = ReportingManager.processTestResults(cucumberJson);
            
            // 6. Validar resultado
            if (result.isSuccess()) {
                TestLogger.logInfo("REPORTING", "Reportes generados exitosamente", Map.of(
                    "extentReport", result.getExtentReportPath() != null ? result.getExtentReportPath() : "N/A"
                ));
                System.out.println("✅ Reporting completado");
                System.out.println("📄 Reporte HTML: " + result.getExtentReportPath());
            } else {
                TestLogger.logError("REPORTING", "Error al generar reportes", Map.of(
                    "failedStep", result.getFailedStep(),
                    "error", result.getErrorMessage()
                ));
                System.err.println("❌ Falló reporting: " + result.getErrorMessage());
            }
            
        } catch (Exception e) {
            TestLogger.logError("REPORTING", "Excepción al generar reportes", Map.of(
                "error", e.getMessage()
            ));
            System.err.println("❌ Error crítico en reporting: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Limpiar todo el contexto de logging
            LoggingInitializer.clearAllContext();
        }
    }
    
    /**
     * Espera a que el archivo cucumber.json esté disponible y con contenido válido.
     * Implementa retry logic con backoff para evitar timing issues.
     * 
     * @param cucumberJsonPath Ruta al archivo cucumber.json
     * @return Contenido del archivo JSON o null si falla
     */
    private static String waitForCucumberJson(Path cucumberJsonPath) {
        int maxRetries = 10;
        long waitMillis = 300; // 300ms entre intentos
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Verificar que el archivo existe
                if (!Files.exists(cucumberJsonPath)) {
                    System.out.println("⏳ Intento " + attempt + "/" + maxRetries + " - Esperando a que se cree el archivo...");
                    Thread.sleep(waitMillis);
                    continue;
                }
                
                // Verificar tamaño del archivo
                long fileSize = Files.size(cucumberJsonPath);
                if (fileSize == 0) {
                    System.out.println("⏳ Intento " + attempt + "/" + maxRetries + " - Archivo existe pero tamaño = 0 bytes");
                    Thread.sleep(waitMillis);
                    continue;
                }
                
                // Leer contenido
                String content = Files.readString(cucumberJsonPath, java.nio.charset.StandardCharsets.UTF_8);
                
                // DEBUG: Mostrar primeros caracteres
                if (content == null) {
                    System.out.println("⏳ Intento " + attempt + "/" + maxRetries + " - Contenido es NULL");
                    Thread.sleep(waitMillis);
                    continue;
                }
                
                String trimmed = content.trim();
                int contentLength = trimmed.length();
                
                if (contentLength == 0) {
                    System.out.println("⏳ Intento " + attempt + "/" + maxRetries + " - Contenido vacío después de trim()");
                    Thread.sleep(waitMillis);
                    continue;
                }
                
                if (trimmed.equals("[]")) {
                    System.out.println("⏳ Intento " + attempt + "/" + maxRetries + " - Contenido es '[]' (sin scenarios)");
                    Thread.sleep(waitMillis);
                    continue;
                }
                
                // Validar que empieza con '[{' (JSON válido de Cucumber)
                if (!trimmed.startsWith("[{")) {
                    System.out.println("⚠️ Intento " + attempt + "/" + maxRetries + " - Contenido no es JSON válido de Cucumber");
                    System.out.println("   Primeros 50 chars: " + trimmed.substring(0, Math.min(50, trimmed.length())));
                    Thread.sleep(waitMillis);
                    continue;
                }
                
                // TODO BIEN: Archivo con contenido válido
                if (attempt > 1) {
                    System.out.println("✅ cucumber.json disponible después de " + attempt + " intentos");
                }
                System.out.println("📊 Tamaño archivo: " + fileSize + " bytes, Contenido: " + contentLength + " caracteres");
                return content;
                
            } catch (java.nio.file.NoSuchFileException e) {
                System.out.println("⏳ Intento " + attempt + "/" + maxRetries + " - Archivo no existe aún");
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } catch (Exception e) {
                System.err.println("⚠️ Intento " + attempt + "/" + maxRetries + " - Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                e.printStackTrace();
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        
        // Timeout - último intento de diagnosticar
        try {
            if (Files.exists(cucumberJsonPath)) {
                long finalSize = Files.size(cucumberJsonPath);
                System.err.println("❌ Timeout: Archivo existe con " + finalSize + " bytes pero no pudo leerse correctamente");
            } else {
                System.err.println("❌ Timeout: Archivo NO existe después de " + maxRetries + " intentos");
            }
        } catch (Exception e) {
            System.err.println("❌ Timeout y error al diagnosticar: " + e.getMessage());
        }
        
        return null;
    }
    
    // ============================================================================
    // MÉTODOS AUXILIARES
    // ============================================================================
    
    /**
     * Detecta el framework desde los tags del scenario.
     * 
     * @param tags Tags del scenario (@web, @api, @mobile)
     * @return Framework detectado (WEB, API, MOBILE, TEST)
     */
    private String detectFramework(Collection<String> tags) {
        if (tags.contains("@web")) return "WEB";
        if (tags.contains("@api")) return "API";
        if (tags.contains("@mobile")) return "MOBILE";
        return "TEST"; // Fallback
    }
    
    /**
     * Extrae el nombre del feature desde la URI.
     * 
     * @param uri URI del feature (ej: file:///path/to/login.feature)
     * @return Nombre del feature (ej: LoginFeature)
     */
    private String extractFeatureName(String uri) {
        String fileName = uri.substring(uri.lastIndexOf('/') + 1);
        return fileName.replace(".feature", "")
                       .replaceAll("[^a-zA-Z0-9]", "_");
    }
    
    /**
     * Captura screenshot de error y lo guarda con EvidenceManager.
     * Ajustar según tu implementación de WebDriver.
     */
    private void captureFailureScreenshot() {
        try {
            // Ajustar según cómo obtienes tu WebDriver
            // Opción 1: DriverManager estático
            // WebDriver driver = DriverManager.getDriver();
            
            // Opción 2: ThreadLocal
            // WebDriver driver = DriverContext.get();
            
            // Opción 3: Cucumber PicoContainer / Dependency Injection
            // WebDriver driver = this.driver; (inyectado en constructor)
            
            // DESCOMENTAR Y AJUSTAR SEGÚN TU IMPLEMENTACIÓN:
            /*
            if (driver instanceof TakesScreenshot) {
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                EvidenceManager.saveScreenshot(screenshot, "failure");
                TestLogger.logInfo("EVIDENCE", "Screenshot de error capturado", null);
            }
            */
            
        } catch (Exception e) {
            TestLogger.logError("EVIDENCE", "Error al capturar screenshot", Map.of(
                "error", e.getMessage()
            ));
        }
    }
}
```

### **Paso 3: Generar Reportes (AfterAll)**

```java
import com.scotia.qa.common.reporting.core.config.ReportingConfig;
import com.scotia.qa.common.reporting.extent.generator.ReportingManager;
import com.scotia.qa.common.reporting.manager.pipeline.PipelineResult;
import io.cucumber.java.AfterAll;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ReportingHooks {
    
    @AfterAll
    public static void generateReports() throws Exception {
        // 1. Cargar resultados de Cucumber
        String cucumberJson = Files.readString(
            Paths.get("target/cucumber-reports/cucumber.json")
        );
        
        // 2. Cargar configuración
        ReportingConfig config = ReportingConfig.fromConfigManager();
        
        // 3. Inicializar ReportingManager
        ReportingManager.initialize(config);
        
        // 4. Procesar (TODO AUTOMÁTICO)
        PipelineResult result = ReportingManager.processTestResults(cucumberJson);
        
        // 5. Validar
        if (result.isSuccess()) {
            System.out.println("✅ Reporting completado");
            System.out.println("📄 HTML: " + result.getExtentReportPath());
        } else {
            System.err.println("❌ Falló: " + result.getErrorMessage());
        }
    }
}
```

---

## 🔗 Integración con Evidencias

### **Componentes del Sistema de Evidencias**

| Componente | Responsabilidad | Ubicación |
|------------|-----------------|-----------|
| **EvidenceManager** | Captura y guarda evidencias | `common/logging/` |
| **EvidenceCollector** | Convierte evidencias → Attachments | `reporting/core/util/` |
| **CucumberResultAdapter** | Auto-recolecta evidencias | `reporting/core/adapter/` |
| **Attachment** | Modelo de attachment | `reporting/core/model/` |

### **Flujo de Evidencias**

```
Test ejecuta
  ↓
EvidenceManager.saveScreenshot("screenshot.png")
  ↓
Guarda en: test-evidences/WEB/login_feature/valid_login/screenshot_20251204_101530.png
  ↓
CucumberResultAdapter.collectScenarioEvidences()
  ↓
EvidenceCollector.collectScreenshots() → List<Attachment>
  ↓
ScenarioResult.setScreenshots(attachments)
  ↓
ExtentReportGenerator: Embebe en HTML (base64)
JiraAttachmentService: Sube a Jira
```

### **Tipos de Evidencias Soportadas**

- ✅ Screenshots (PNG, JPG)
- ✅ API Responses (JSON)
- ✅ UI Interactions (JSON logs)
- ✅ Errors (JSON con stacktrace)

---

## 🔗 Comunicación con Jira/Xray

### **📌 ¿Cómo se identifica un Test en Jira?**

El framework utiliza **tags de Cucumber** para vincular scenarios con tests en Jira:

```gherkin
@QAAUY-123 @smoke @web
Scenario: Login exitoso
  Given usuario ingresa credenciales válidas
  When hace clic en Login
  Then debería ver el dashboard
```

**Extracción del Test Key:**
- `TagExtractor.java` busca pattern: `@([A-Z]{2,10}-\\d+)`
- Resultado: `QAAUY-123` → Este es el **Test ID en Jira**

**❌ Sin tag válido = No se reporta a Jira**

---

### **📋 ¿Qué es un Test Execution?**

Un **Test Execution** es un issue de tipo especial en Jira/Xray que **agrupa múltiples tests ejecutados juntos**.

**Ejemplo:**

```
Test Execution: QAAUY-640 - "Sprint 12 - Regression Tests"
├── Test 1: QAAUY-123 → PASS ✅
├── Test 2: QAAUY-124 → FAIL ❌
├── Test 3: QAAUY-125 → PASS ✅
└── Test 4: QAAUY-126 → SKIP ⏭️
```

**Beneficios:**
- ✅ Agrupa tests por sprint/release/ambiente
- ✅ Facilita tracking de ejecuciones históricas
- ✅ Permite comparar resultados entre ejecuciones

---

### **🎯 Dos Estrategias de Test Execution**

#### **Estrategia 1: Test Execution PRE-EXISTENTE** (Recomendado)

**Configuración:**
```properties
jira.autoCreateExecution=false         # ← DEFAULT
jira.testExecutionId=QAAUY-640        # ID del execution ya creado en Jira
```

**Flujo:**
1. ✅ Creas manualmente un Test Execution en Jira: `QAAUY-640`
2. ✅ Asocias tests al execution (QAAUY-123, QAAUY-124...)
3. ✅ Ejecutas tests localmente
4. ✅ El framework **actualiza el status de cada test** dentro del execution

**Ventajas:**
- ✅ Control total sobre qué tests van en cada execution
- ✅ Puedes pre-cargar tests antes de ejecutar
- ✅ Funciona sin permisos de creación de issues

**Desventaja:**
- ⚠️ Requiere creación manual del execution

---

#### **Estrategia 2: AUTO-CREAR Test Execution** (Automático)

**Configuración:**
```properties
jira.autoCreateExecution=true          # ← Habilitar auto-creación
jira.projectKey=QAAUY                  # Requerido
jira.testEnvironment=QA                # Requerido
# jira.testExecutionId NO necesario
```

**Flujo:**
1. ❌ **NO** proporcionas `testExecutionId`
2. ✅ El framework **crea automáticamente** un Test Execution:
   - Summary: "Automated Test Execution - 2025-12-19 15:30"
   - Project: QAAUY
   - Environment: QA
   - Ejecuta API: `POST /rest/api/2/issue`
3. ✅ Todos los tests del `cucumber.json` se asocian al nuevo execution
4. ✅ El execution ID se loguea para futuras referencias

**Ventajas:**
- ✅ Totalmente automático (ideal para CI/CD)
- ✅ No requiere preparación manual

**Desventajas:**
- ⚠️ Requiere permisos de creación de issues en Jira
- ⚠️ Crea un nuevo execution cada vez que ejecutas

**⚠️ IMPORTANTE:**
```properties
# Si ambas están configuradas, testExecutionId tiene prioridad:
jira.autoCreateExecution=true
jira.testExecutionId=QAAUY-640  # ← Se usará este, NO se crea uno nuevo
```

---

### **🔄 Flujo Completo de Comunicación**

```
┌────────────────────────────────────────────────────────────────────┐
│                    1. EJECUCIÓN DE TESTS                           │
└────────────────────────────────────────────────────────────────────┘
                              ↓
   Cucumber ejecuta scenarios con tags: @QAAUY-123, @QAAUY-124
                              ↓
┌────────────────────────────────────────────────────────────────────┐
│                  2. CUCUMBER GENERA cucumber.json                  │
└────────────────────────────────────────────────────────────────────┘
{
  "elements": [
    {
      "tags": ["@QAAUY-123", "@smoke"],
      "steps": [...],
      "status": "passed"
    },
    {
      "tags": ["@QAAUY-124", "@regression"],
      "steps": [...],
      "status": "failed"
    }
  ]
}
                              ↓
┌────────────────────────────────────────────────────────────────────┐
│         3. REPORTING PIPELINE - ConversionStep                     │
└────────────────────────────────────────────────────────────────────┘
   CucumberResultAdapter.convert(cucumber.json)
     ├─ TagExtractor.extractTestKey("@QAAUY-123") → "QAAUY-123"
     ├─ TagExtractor.extractTestKey("@QAAUY-124") → "QAAUY-124"
     └─ Genera: TestExecutionResult con 2 ScenarioResults
                              ↓
┌────────────────────────────────────────────────────────────────────┐
│         4. REPORTING PIPELINE - JiraUpdateStatusStep               │
└────────────────────────────────────────────────────────────────────┘
   JiraUpdateService.updateTestStatus(result)
     ├─ ensureTestExecutionExists()
     │   ├─ Si testExecutionId existe → Usar ese
     │   └─ Si NO existe y autoCreate=true → Crear nuevo
     │       POST /rest/api/2/issue
     │       {
     │         "fields": {
     │           "project": {"key": "QAAUY"},
     │           "summary": "Automated Test Execution - 2025-12-19",
     │           "issuetype": {"name": "Test Execution"}
     │         }
     │       }
     │       Respuesta: {"key": "QAAUY-750"} ✅
     │
     └─ updateBatch() o updateSingle()
         POST /rest/raven/2.0/import/execution
         {
           "info": {
             "project": "QAAUY",
             "testEnvironments": ["QA"]
           },
           "tests": [
             {"testKey": "QAAUY-123", "status": "PASS"},
             {"testKey": "QAAUY-124", "status": "FAIL"}
           ]
         }
                              ↓
┌────────────────────────────────────────────────────────────────────┐
│      5. REPORTING PIPELINE - JiraUploadAttachmentsStep             │
└────────────────────────────────────────────────────────────────────┘
   JiraAttachmentService.uploadAttachments()
     ├─ Sube reporte HTML → QAAUY-640
     ├─ Sube screenshot_001.png → QAAUY-123
     └─ Sube screenshot_002.png → QAAUY-124
         POST /rest/api/2/issue/{testKey}/attachments
         Content-Type: multipart/form-data
                              ↓
┌────────────────────────────────────────────────────────────────────┐
│                        6. RESULTADO FINAL                          │
└────────────────────────────────────────────────────────────────────┘
   ✅ Test Execution actualizado (QAAUY-640 o auto-creado)
   ✅ Tests con status correcto (PASS/FAIL)
   ✅ Screenshots adjuntos
   ✅ Reporte HTML adjunto
```

---

### **🛠️ Configuración por Caso de Uso**

#### **Caso 1: Desarrollo Local (Manual)**
```properties
jira.updateStatus=false             # No actualizar Jira
jira.uploadReport=false             # No subir attachments
extent.enabled=true                 # Solo generar HTML local
```

#### **Caso 2: CI/CD con Test Execution Pre-creado**
```properties
jira.updateStatus=true
jira.uploadReport=true
jira.autoCreateExecution=false
jira.testExecutionId=${TEST_EXECUTION_ID}  # Variable de Jenkins
```

#### **Caso 3: CI/CD Totalmente Automático**
```properties
jira.updateStatus=true
jira.uploadReport=true
jira.autoCreateExecution=true       # ← Crea execution automáticamente
jira.projectKey=QAAUY
jira.testEnvironment=${ENV}         # Variable de Jenkins (QA, PROD...)
```

---

### **📊 Modos de Actualización**

#### **BATCH Mode** (Recomendado)
```properties
jira.updateMode=BATCH
```

**Funcionamiento:**
- Envía **todos los tests en un solo request**
- API: `/rest/raven/2.0/import/execution`
- Más rápido (1 llamada HTTP)

**Ventaja:** Performance
**Desventaja:** Si falla, fallan todos

---

#### **SINGLE Mode**
```properties
jira.updateMode=SINGLE
```

**Funcionamiento:**
- Envía **cada test en un request separado**
- API: `/rest/api/2/issue/{testKey}/transitions`
- Más lento (N llamadas HTTP)

**Ventaja:** Tolerante a fallos (un test no afecta otros)
**Desventaja:** Lento con muchos tests

---

### **🚨 Troubleshooting Jira**

#### **Error: "Test Execution not found"**
```
❌ 404 Not Found: /rest/api/2/issue/QAAUY-640
```

**Solución:**
```properties
# Verifica que el execution exista en Jira
jira.testExecutionId=QAAUY-640

# O habilita auto-creación
jira.autoCreateExecution=true
```

---

#### **Error: "Test key not found in Jira"**
```
❌ 404 Not Found: Test QAAUY-999 does not exist
```

**Solución:**
1. Verifica que el test existe en Jira (proyecto QAAUY)
2. Verifica el tag en tu feature:
```gherkin
@QAAUY-999 @smoke
Scenario: Mi test
```

---

#### **Warning: "Scenario without test key"**
```
⏭️ Scenario 'Login exitoso' sin test key válido, omitiendo
```

**Solución:**
Agregar tag con test key:
```gherkin
@QAAUY-123  # ← Agregar esto
Scenario: Login exitoso
```

---

### **❓ Preguntas Frecuentes (FAQ)**

#### **Q1: ¿Necesito crear Test Executions manualmente siempre?**

**R:** Depende de tu estrategia:

- ✅ **Manual (Recomendado)**: Sí, creas `QAAUY-640` en Jira antes de ejecutar
  ```properties
  jira.autoCreateExecution=false
  jira.testExecutionId=QAAUY-640
  ```

- ✅ **Automática**: No, se crea automáticamente cada vez que ejecutas
  ```properties
  jira.autoCreateExecution=true
  jira.projectKey=QAAUY
  ```

**Recomendación**: Usa manual para control total, automática para CI/CD.

---

#### **Q2: ¿Puedo mezclar tests de diferentes módulos en un execution?**

**R:** ✅ **Sí**, un Test Execution puede contener tests de cualquier módulo:

```
Test Execution: QAAUY-640 - "Sprint 12 - Regression"
├── QAAUY-123 (módulo: qa-module-login)    → PASS ✅
├── QAAUY-124 (módulo: qa-module-checkout) → FAIL ❌
├── QAAUY-125 (módulo: qa-module-payments) → PASS ✅
└── QAAUY-126 (módulo: qa-module-reports)  → SKIP ⏭️
```

Esto es útil para **regression suites** que abarcan múltiples funcionalidades.

---

#### **Q3: ¿Qué pasa si un scenario no tiene tag `@QAAUY-XXX`?**

**R:** ⏭️ Se **omite de Jira** pero **sí se ejecuta** en Cucumber:

```gherkin
Scenario: Login exitoso (sin tag)
  → Se ejecuta ✅
  → NO se reporta a Jira ⏭️
  → Sí aparece en Extent Report HTML 📄
```

**Log esperado:**
```
⏭️ Scenario 'Login exitoso' sin test key válido, omitiendo actualización Jira
```

**Cuándo usar scenarios sin tag:**
- Tests exploratorios temporales
- Tests en desarrollo (WIP)
- Tests que no requieren trazabilidad en Jira

---

#### **Q4: ¿Puedo tener múltiples Test Executions en paralelo?**

**R:** ✅ **Sí**, configura diferentes `testExecutionId` por ejecución:

**Ejemplo con Jenkins Jobs:**
```bash
# Job 1: Regression QA
./gradlew test -DTEST_EXECUTION_ID=QAAUY-640 -Dcucumber.filter.tags="@regression"

# Job 2: Smoke Tests QA
./gradlew test -DTEST_EXECUTION_ID=QAAUY-641 -Dcucumber.filter.tags="@smoke"

# Job 3: Regression PROD
./gradlew test -DTEST_EXECUTION_ID=QAAUY-642 -DENV=PROD
```

Cada job reporta a un Test Execution diferente **sin conflictos**.

---

#### **Q5: ¿Qué es mejor: BATCH o SINGLE mode?**

**R:** Depende de tu escenario:

| Aspecto | BATCH | SINGLE |
|---------|-------|--------|
| **Velocidad** | ⚡ Rápido (1 request) | 🐢 Lento (N requests) |
| **Tolerancia a fallos** | ❌ Si falla, afecta todos | ✅ Un fallo no afecta otros |
| **Uso de red** | Bajo (1 conexión) | Alto (N conexiones) |
| **Recomendado para** | ≤100 tests, red estable | >100 tests, red inestable |
| **Timeout risk** | Medio (1 request grande) | Bajo (requests pequeños) |

**Configuración:**
```properties
# BATCH (default, recomendado)
jira.updateMode=BATCH

# SINGLE (para redes inestables o muchos tests)
jira.updateMode=SINGLE
```

**Recomendación**: Usa BATCH por defecto, cambia a SINGLE solo si tienes problemas.

---

#### **Q6: ¿Cómo depurar problemas de comunicación con Jira?**

**R:** Habilita logs detallados:

**1. Verifica configuración:**
```properties
jira.enabled=true
jira.updateStatus=true
jira.url=https://jira.your-company.com
jira.user=${JIRA_USER}
jira.password=${JIRA_PASSWORD}
```

**2. Revisa logs del pipeline:**
```
[JIRA_UPDATE_STEP] 📤 Actualizando status en Jira
[JIRA_UPDATE_STEP] ✅ 5 tests actualizados en Jira
```

**3. Verifica conectividad:**
```bash
# Test manual con curl
curl -u "$JIRA_USER:$JIRA_PASSWORD" \
  "https://jira.your-company.com/rest/api/2/issue/QAAUY-123"
```

**4. Habilita logging DEBUG:**
```xml
<!-- logback.xml -->
<logger name="com.scotia.qa.common.reporting.jira" level="DEBUG"/>
```

---

#### **Q7: ¿Los attachments tienen límite de tamaño?**

**R:** ✅ **Sí**, configurable:

```properties
jira.maxAttachmentSizeMb=10  # Default: 10 MB
```

**Comportamiento:**
- Screenshots: Se suben individualmente
- Reporte HTML: Se valida tamaño antes de subir
- Si excede límite: Se loguea WARNING y continúa

**Logs:**
```
⚠️ Attachment 'screenshot_large.png' (15 MB) excede límite (10 MB), omitiendo
```

**Solución si tienes screenshots muy grandes:**
```properties
jira.maxAttachmentSizeMb=20  # Incrementar límite
```

---

## ⚙️ Pipeline Interno

El sistema usa un **Pipeline Pattern** (Chain of Responsibility) con 4 steps:

### **Step 1: ConversionStep** ⚙️ REQUERIDO

**Responsabilidad:** Convertir raw results → TestExecutionResult

```
Cucumber JSON
  ↓
CucumberResultAdapter.convert()
  ├─ Parsea JSON
  ├─ Extrae scenarios con test keys (@QAAUY-123)
  ├─ EvidenceCollector.collectScreenshots() (automático)
  └─ TestExecutionResult
```

**Configuración:** Siempre habilitado

---

### **Step 2: ExtentGenerationStep** 📊 OPCIONAL

**Responsabilidad:** Generar reporte HTML

```
TestExecutionResult
  ↓
ExtentReportGenerator.generate()
  ├─ Inicializa ExtentReports
  ├─ Agrega system info (OS, Java, Browser)
  ├─ Procesa scenarios
  │   ├─ Crea test con categories (test key + tags)
  │   ├─ Embebe screenshots (base64)
  │   ├─ Agrega logs
  │   └─ Status (PASS/FAIL/SKIP)
  └─ extent.flush() → HTML
```

**Configuración:**
```properties
extent.enabled=true
```

---

### **Step 3: JiraUpdateStatusStep** 🔄 OPCIONAL

**Responsabilidad:** Actualizar status en Jira/Xray

**Modos:**
- **BATCH:** Un request con todos los tests (más rápido)
- **SINGLE:** Un request por test (más robusto)

```
TestExecutionResult
  ↓
JiraUpdateService.updateTestStatus()
  ├─ Para cada scenario con test key
  ├─ Mapea TestStatus → Jira Status
  │   PASS → "PASS"
  │   FAIL → "FAIL"
  │   SKIP → "ABORTED"
  └─ POST /rest/raven/2.0/import/execution
```

**Configuración:**
```properties
jira.updateStatus=true
jira.updateMode=BATCH
```

---

### **Step 4: JiraUploadAttachmentsStep** 📎 OPCIONAL

**Responsabilidad:** Subir attachments a Jira

```
TestExecutionResult
  ↓
JiraAttachmentService
  ├─ Para cada scenario con evidencias
  │   ├─ Filtra por tamaño (< maxAttachmentSizeMb)
  │   └─ POST /rest/api/2/issue/{testKey}/attachments
  └─ Sube reporte HTML a Test Execution
      └─ POST /rest/api/2/issue/{testExecutionId}/attachments
```

**Configuración:**
```properties
jira.uploadReport=true
jira.includeEvidences=true
jira.maxAttachmentSizeMb=10
```

---

## 💡 Ejemplos Completos

### **Ejemplo 1: Solo Extent Reports (Sin Jira)**

**config-scotia.properties:**
```properties
reporting.enabled=true
extent.enabled=true
extent.outputPath=build/reports/extent/
extent.reportName=execution-report.html
```

**Hook:**
```java
@AfterAll
public static void generateReports() throws Exception {
    String cucumberJson = Files.readString(Paths.get("target/cucumber-reports/cucumber.json"));
    
    ReportingConfig config = ReportingConfig.fromConfigManager();
    ReportingManager.initialize(config);
    
    PipelineResult result = ReportingManager.processTestResults(cucumberJson);
    // ✅ HTML generado en: build/reports/extent/execution-report.html
}
```

---

### **Ejemplo 2: Solo Jira (Sin Extent)**

**config-scotia.properties:**
```properties
reporting.enabled=true
extent.enabled=false

jira.url=https://jira.agile.bns
jira.user=${JIRA_USER}
jira.password=${JIRA_PASSWORD}
jira.projectKey=QAAUY
jira.testExecutionId=${TEST_EXECUTION_ID}
jira.updateStatus=true
jira.uploadReport=false
```

---

### **Ejemplo 3: Extent + Jira Completo**

**config-scotia.properties:**
```properties
reporting.enabled=true

# Extent
extent.enabled=true
extent.outputPath=build/reports/extent/
extent.includeScreenshots=true

# Jira
jira.url=https://jira.agile.bns
jira.user=${JIRA_USER}
jira.password=${JIRA_PASSWORD}
jira.projectKey=QAAUY
jira.testExecutionId=${TEST_EXECUTION_ID}
jira.updateStatus=true
jira.uploadReport=true
jira.includeEvidences=true
```

**Resultado:**
- ✅ HTML: `build/reports/extent/execution-report.html`
- ✅ Jira: Tests actualizados (PASS/FAIL)
- ✅ Jira: Reporte HTML adjunto
- ✅ Jira: Screenshots adjuntos por test

---

## 🔧 Troubleshooting

### **1. "ReportingManager no inicializado"**

**Error:**
```
IllegalStateException: ReportingManager no inicializado. Llama a ReportingManager.initialize(config) primero.
```

**Solución:**
```java
// ANTES de processTestResults()
ReportingConfig config = ReportingConfig.fromConfigManager();
ReportingManager.initialize(config);
```

---

### **2. "TestExecutionResult vacío"**

**Error:** Pipeline completa pero no hay tests en el reporte

**Causas:**
- Features sin tags de test key (`@QAAUY-123`)
- Cucumber JSON vacío
- Adaptador incorrecto

**Solución:**
```gherkin
@QAAUY-123 @smoke
Scenario: Login exitoso
  Given ...
```

---

### **3. "Jira authentication failed"**

**Error:**
```
IOException: Jira POST failed: 401 - Unauthorized
```

**Solución:**
1. Verificar credenciales en `.env.local`
2. Usar token en lugar de password
3. Verificar permisos en Jira

---

### **4. Screenshots no aparecen en Extent**

**Causa:** `extent.includeScreenshots=false`

**Solución:**
```properties
extent.includeScreenshots=true
```

---

### **5. "Attachment muy grande"**

**Warning:**
```
⚠️ Archivo muy grande, omitiendo: screenshot.png (12.5 MB > 10 MB)
```

**Solución:**
```properties
jira.maxAttachmentSizeMb=15
```

---

## 📚 API Reference

### **ReportingManager**

```java
// Inicializar
ReportingConfig config = ReportingConfig.fromConfigManager();
ReportingManager.initialize(config);

// Procesar resultados
PipelineResult result = ReportingManager.processTestResults(cucumberJson);

// Obtener configuración
ReportingConfig currentConfig = ReportingManager.getConfig();

// Reset (testing)
ReportingManager.reset();
```

### **PipelineResult**

```java
boolean success = result.isSuccess();
String failedStep = result.getFailedStep();
String errorMessage = result.getErrorMessage();
String extentReport = result.getExtentReportPath();
Map<String, StepResult> stepResults = result.getStepResults();
```

### **ReportingConfig**

```java
// Desde ConfigManager
ReportingConfig config = ReportingConfig.fromConfigManager();

// Validaciones
boolean isEnabled = config.isEnabled();
ExtentConfig extentConfig = config.getExtent();
JiraConfig jiraConfig = config.getJira();
```

### **EvidenceManager (en common/logging)**

```java
// Establecer contexto
EvidenceManager.setTestContext("WEB", "LoginFeature", "ValidLogin");

// Guardar evidencias
String path = EvidenceManager.saveScreenshot(bytes, "description");
String path = EvidenceManager.saveApiResponse(method, endpoint, status, req, res);
String path = EvidenceManager.saveErrorEvidence(type, message, stackTrace);

// Limpiar
EvidenceManager.clearTestContext();
```

---

## 🎓 Decisiones de Diseño

### **¿Por qué Pipeline Pattern?**
✅ **Modular:** Agregar/remover steps fácilmente  
✅ **Testeable:** Steps independientes  
✅ **Configurable:** Habilitar/deshabilitar por config  
✅ **Tolerante:** Steps opcionales no abortan  

### **¿Por qué separar EvidenceManager de Reporting?**
✅ **Separación de concerns:** Captura vs Distribución  
✅ **Sin acoplamiento:** Reporting independiente de cómo se capturan evidencias  
✅ **Reutilizable:** EvidenceCollector traduce cualquier formato  

### **¿Por qué auto-recolección en CucumberResultAdapter?**
✅ **DX:** Módulos no necesitan código extra  
✅ **Convención sobre configuración:** Funciona out-of-the-box  
✅ **Opcional:** Se puede deshabilitar si se necesita control manual  

---

## 📊 Métricas del Sistema

| Métrica | Valor |
|---------|-------|
| **Clases** | 23 |
| **Líneas de código** | ~3,500 |
| **Dependencias externas** | 3 (Extent, HttpClient, Jackson) |
| **Cobertura de tests** | Pendiente |
| **Performance** | < 5s para 100 scenarios |

---

## 📚 Referencias

### **Código Fuente**

| Componente | Clase Principal | Descripción |
|------------|----------------|-------------|
| **Tag Extraction** | `TagExtractor.java` | Extrae test keys desde tags Cucumber |
| **Jira Communication** | `JiraUpdateService.java` | Actualiza status en Jira/Xray |
| **Result Conversion** | `CucumberResultAdapter.java` | Convierte Cucumber JSON a modelo |
| **Evidence Collection** | `EvidenceCollector.java` | Recolecta screenshots y evidencias |
| **Extent Generation** | `ExtentReportGenerator.java` | Genera reportes HTML |
| **Pipeline Orchestration** | `ReportingPipeline.java` | Orquesta ejecución de steps |

### **Configuración**

- **Módulo Reporting**: Este README
- **Configuración General**: `/config/README.md`
- **Variables de Entorno**: `/config/README.md#variables-de-entorno`
- **Configuración Jira**: Sección [Comunicación con Jira/Xray](#-comunicación-con-jiraxray)

### **APIs Externas**

#### **Jira/Xray REST API**

| Recurso | URL |
|---------|-----|
| **Xray REST API Documentation** | [https://docs.getxray.app/display/XRAY/REST+API](https://docs.getxray.app/display/XRAY/REST+API) |
| **Import Execution Results** | [https://docs.getxray.app/display/XRAY/Import+Execution+Results](https://docs.getxray.app/display/XRAY/Import+Execution+Results) |
| **Jira REST API v2** | [https://docs.atlassian.com/software/jira/docs/api/REST/latest/](https://docs.atlassian.com/software/jira/docs/api/REST/latest/) |
| **Create Issue** | `POST /rest/api/2/issue` |
| **Add Attachment** | `POST /rest/api/2/issue/{issueKey}/attachments` |

#### **Extent Reports**

| Recurso | URL |
|---------|-----|
| **Extent Reports Documentation** | [https://www.extentreports.com/docs/](https://www.extentreports.com/docs/) |
| **Extent HTML Reporter** | [https://www.extentreports.com/docs/versions/5/java/index.html](https://www.extentreports.com/docs/versions/5/java/index.html) |

### **Dependencias Maven**

```xml
<!-- Extent Reports -->
<dependency>
    <groupId>com.aventstack</groupId>
    <artifactId>extentreports</artifactId>
    <version>5.1.1</version>
</dependency>

<!-- Apache HttpClient (Jira communication) -->
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5.14</version>
</dependency>

<!-- Jackson (JSON processing) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
```

### **Patrones de Diseño Implementados**

| Patrón | Uso en Reporting |
|--------|------------------|
| **Facade** | `ReportingManager` como punto de entrada único |
| **Chain of Responsibility** | `ReportingPipeline` ejecuta steps en cadena |
| **Strategy** | `ResultAdapter` permite múltiples formatos |
| **Builder** | `ReportingPipeline.Builder()` para construcción |
| **Factory** | `ResultAdapter` factory pattern |
| **Singleton** | `ReportingConfig` única instancia |

---

## 🚀 Roadmap

- [ ] Soporte JUnit XML adapter
- [ ] Soporte TestNG adapter
- [ ] Paralelización de upload a Jira
- [ ] Caché de resultados
- [ ] Webhooks post-reporting
- [ ] Integración con Slack/Teams

---

## 📝 Changelog

### **v1.0.0** (2025-12-04)
- ✅ Sistema de Pipeline completo
- ✅ Extent Reports integration
- ✅ Jira/Xray integration
- ✅ Integración automática con EvidenceManager
- ✅ Configuración flexible
- ✅ 4 Steps del pipeline implementados

---

## 👥 Contribuciones

Ver [CONTRIBUTING.md](../../../../../../CONTRIBUTING.md) en la raíz del framework.

---

## 📄 Licencia

Interno - Scotia Bank Uruguay

---

**¿Necesitas ayuda?** Contacta al equipo de QA Automation.
