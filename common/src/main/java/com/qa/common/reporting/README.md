# Módulo de Reporting — `com.qa.common.reporting`

> **Estado:** Funcional para generación HTML local (ExtentReports). Integración con plataformas externas (Jira/Xray, Azure DevOps) gestionada por el Backend — ver sección 6.

---

## 1. Visión General

El módulo genera reportes de ejecución de tests en formato HTML a partir del archivo
`cucumber.json` producido por Cucumber. Opera **desacoplado** del motor de ejecución:
se activa después de que todos los escenarios terminan, vía evento `TestRunFinished`.

```
Tests ejecutan (Cucumber)
    ↓
Cucumber escribe: target/cucumber-reports/cucumber.json
    ↓  TestRunFinished event
CucumberReportingPlugin.handleTestRunFinished()
    ↓  Guard clauses verifican habilitación y existencia del JSON
ReportingManager → Pipeline:
    ├── ConversionStep        → JSON → TestExecutionResult (modelo en memoria)
    └── ExtentGenerationStep  → TestExecutionResult → HTML
Output: build/reports/extent/execution-report.html
```

---

## 2. Estructura de Paquetes

```
com.qa.common.reporting/
├── cucumber/
│   └── CucumberReportingPlugin.java        ← EventListener de Cucumber (trigger post-ejecución)
├── core/
│   ├── adapter/
│   │   ├── ResultAdapter.java              ← Interface de conversión de resultados
│   │   └── cucumber/
│   │       └── CucumberResultAdapter.java  ← Convierte JSON Cucumber → modelo propio
│   ├── config/
│   │   ├── ExtentConfig.java               ← Configuración de ExtentReports
│   │   └── ReportingConfig.java            ← Configuración general del módulo
│   ├── model/                              ← Modelos de resultado (contrato de datos)
│   │   ├── Attachment.java
│   │   ├── EnvironmentInfo.java
│   │   ├── HttpStepDetail.java             ← Snapshot HTTP redactado (de API steps)
│   │   ├── ScenarioResult.java
│   │   ├── StepResult.java
│   │   ├── TestExecutionResult.java
│   │   └── TestStatus.java
│   └── util/
│       ├── EvidenceCollector.java          ← Recolección de screenshots y logs
│       ├── HttpDetailRedactor.java         ← Construye HttpStepDetail con redacción
│       └── TagExtractor.java              ← Extrae tags de escenarios Cucumber
├── extent/
│   └── generator/
│       ├── ExtentReportGenerator.java      ← Genera el HTML con ExtentReports
│       └── ReportingManager.java           ← Fachada del pipeline
└── manager/
    └── pipeline/
        ├── PipelineContext.java
        ├── PipelineResult.java
        ├── PipelineStepResult.java
        ├── ReportingPipeline.java          ← Orquestador (Chain of Responsibility)
        ├── ReportingStep.java              ← Interface para pasos del pipeline
        └── steps/
            ├── ConversionStep.java         ← JSON → TestExecutionResult
            └── ExtentGenerationStep.java   ← TestExecutionResult → HTML
```

---

## 3. Configuración

En el archivo `config-app.properties` del módulo consumidor:

```properties
# Habilita/deshabilita el pipeline completo
reporting.enabled=true

# Habilita/deshabilita específicamente la generación Extent HTML
extent.enabled=true

# Path del cucumber.json (relativo al working directory del módulo)
reporting.cucumber.json.path=target/cucumber-reports/cucumber.json
```

### Registro del plugin en el runner Cucumber

```java
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "json:target/cucumber-reports/cucumber.json, " +
            "html:target/cucumber-reports/cucumber.html, " +
            "pretty, " +
            "com.qa.common.reporting.cucumber.CucumberReportingPlugin"
)
```

> El plugin `json:` debe declararse **antes** que `CucumberReportingPlugin` para que
> el archivo esté disponible cuando el plugin lo lea.

---

## 4. Guard Clauses — Sin Fallos Silenciosos

`CucumberReportingPlugin` verifica tres condiciones antes de procesar:

| Guard | Condición | Log emitido |
|-------|-----------|-------------|
| #1 | `reporting.enabled=false` | `INFO` — reporting deshabilitado intencionalmente |
| #2 | `cucumber.json` ausente tras 10 reintentos con backoff | `ERROR` — ruta esperada + acción correctiva |
| #3 | JSON vacío o sin escenarios (`[]`) | `ERROR` — sin escenarios ejecutados |

Todos los fallos son explícitos con contexto accionable. No hay fallos silenciosos.

---

## 5. Modelos de Datos — Contrato

Los modelos en `core/model/` son el contrato de datos entre el Core y el Backend.
Son serializables a JSON y pueden ser consumidos directamente por el BE para persistencia
o por el FE para visualización de resultados.

```
TestExecutionResult
  ├── EnvironmentInfo          (entorno, browser, fecha, duración total)
  └── List<ScenarioResult>
        ├── status, durationMs, errorMessage
        └── List<StepResult>
              ├── keyword, name, status, durationMs, errorMessage
              ├── HttpStepDetail  (opcional — solo API layer, ya redactado)
              └── List<Attachment> (screenshots, logs)
```

---

## 6. HttpStepDetail y HttpDetailRedactor — Relación con Logging

Estas clases están en `reporting.core` (no en `logging`) porque su función es capturar
snapshots HTTP para **reportes y persistencia**, no para logging en tiempo real.

```
logging/TestLogger + LoggingInitializer  → logs en consola/archivo en tiempo real
                  ↕  capas distintas, se complementan
reporting/core/util/HttpDetailRedactor   → construye snapshot redactado
reporting/core/model/HttpStepDetail      → DTO inmutable: DB, WebSocket, FE, adjuntos externos
```

Un módulo de pruebas que importe el framework obtiene **ambos beneficios automáticamente**:
- Logging estructurado en tiempo real via `TestLogger`
- Captura de evidencia HTTP para reporting via `HttpDetailRedactor` / `HttpStepDetail`

---

## 7. Integración con Plataformas Externas — Estado y Arquitectura

La integración con plataformas de gestión de pruebas es **responsabilidad del Backend**.
El Core solo provee el HTML y/o el modelo `TestExecutionResult`.

### Estado actual de implementación (qa-platformBE)

| Funcionalidad | Jira/Xray | Azure DevOps | Otras |
|---------------|-----------|--------------|-------|
| Sincronizar resultados de ejecución | ✅ Implementado | ✅ Implementado | Enumerados |
| Adjuntar reporte HTML (manual, FE) | ✅ Implementado | Pendiente | — |
| Adjuntar reporte HTML (automático) | Pendiente | Pendiente | — |

### Flujo de adjunto desde el Frontend

```
FE: botón "Adjuntar a Jira / Azure DevOps" (en ExecutionLivePage)
    ↓ POST /api/executions/{id}/integrations/{provider}/attach-report
BE: ReportAttachmentController → ReportAttachmentService → Adapter del proveedor
    ↓
Proveedor: recibe el HTML como adjunto del Test Execution
```

### Evaluación arquitectónica pendiente

Hay dos opciones bajo evaluación para la generación del reporte final:

**Opción A — Core genera HTML (situación actual)**
- Core produce `execution-report.html` via ExtentReports
- BE almacena la URL y la sirve al FE
- BE usa ese HTML para adjuntar a plataformas externas

**Opción B — Core expone JSON estructurado, BE genera el reporte**
- Añadir `JsonExportStep` al pipeline del Core para escribir `TestExecutionResult` como JSON
- BE recibe ese JSON, persiste en DB y genera el artefacto según el formato necesario
- FE consume el JSON del BE directamente para visualización rica
- Mayor flexibilidad de formato (HTML, PDF, etc.) sin depender de ExtentReports en el Core

La elección entre A y B determina el rol a largo plazo de `CucumberReportingPlugin` y
`ExtentReportGenerator` en el Core.

---

## 8. Extensión del Pipeline

Para agregar un nuevo paso, implementar `ReportingStep` y registrar en `ReportingManager`:

```java
public class JsonExportStep implements ReportingStep {

    @Override
    public PipelineStepResult execute(PipelineContext context) {
        TestExecutionResult result = context.getExecutionResult();
        // serializar a JSON en build/reports/execution-result.json
        return PipelineStepResult.success("jsonExport");
    }

    @Override
    public boolean isEnabled(ReportingConfig config) {
        return config.isJsonExportEnabled();
    }

    @Override
    public String getName() { return "JSON Export"; }
}
```

---

## 9. Artefactos Generados

| Artefacto | Path | Condición |
|-----------|------|-----------|
| Reporte HTML ExtentReports | `build/reports/extent/execution-report.html` | `extent.enabled=true` |
| cucumber.json | `target/cucumber-reports/cucumber.json` | Plugin JSON registrado |
| cucumber.html | `target/cucumber-reports/cucumber.html` | Plugin HTML registrado |
