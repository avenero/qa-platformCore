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
- [Pipeline Interno](#-pipeline-interno)
- [Ejemplos Completos](#-ejemplos-completos)
- [Troubleshooting](#-troubleshooting)
- [API Reference](#-api-reference)

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
import com.scotia.qa.common.logging.EvidenceManager;
import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;

public class CucumberHooks {
    
    @Before
    public void beforeScenario(Scenario scenario) {
        // Establecer contexto para evidencias
        String framework = detectFramework(scenario.getSourceTagNames());
        EvidenceManager.setTestContext(
            framework, 
            "LoginFeature", 
            scenario.getName()
        );
    }
    
    @After
    public void afterScenario(Scenario scenario) {
        if (scenario.isFailed()) {
            // Screenshot de error automático
            byte[] screenshot = captureScreenshot();
            EvidenceManager.saveScreenshot(screenshot, "failure");
        }
        
        EvidenceManager.clearTestContext();
    }
}
```

### **Paso 3: Generar Reportes (AfterAll)**

```java
import com.scotia.qa.common.reporting.core.config.ReportingConfig;
import com.scotia.qa.common.reporting.manager.ReportingManager;
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
