# common — Shared Kernel del Core

Módulo base del `qa-platformCore`. Reúne los contratos, runtime y utilidades **comunes a todos los plugins** (HTTP, Web, Mobile, DB) y al BE consumidor.

> **Coordenada Maven:** `com.qa:common:<version>`
> **Audiencia primaria:** devs del Core (extensión) y devs del BE (consumo).
> **Para QAs / POs:** este módulo no expone `StepComponent` — los pasos están en los módulos especializados (`http-core`, `web-core`, etc.).

---

## Tabla de contenidos

1. [Propósito](#propósito)
2. [Coordenada Maven](#coordenada-maven)
3. [Dependencias clave](#dependencias-clave)
4. [Cómo se usa standalone](#cómo-se-usa-standalone)
5. [Áreas funcionales](#áreas-funcionales)
   - [SPI — `CorePlugin` + `StepComponent`](#spi--coreplugin--stepcomponent)
   - [Runtime — `CucumberRuntimeEngine` + `ExecutionConfig`](#runtime--cucumberruntimeengine--executionconfig)
   - [Transport — `ExecutionTransport`](#transport--executiontransport)
   - [Reporter — `StepReporter`](#reporter--stepreporter)
   - [Reporting — Extent HTML pipeline](#reporting--extent-html-pipeline)
   - [SSL / TLS — `SSLContextFactory` + truststore](#ssl--tls--sslcontextfactory--truststore)
   - [Step Catalog i18n — SPI](#step-catalog-i18n--spi)
6. [Cómo se comunica con el exterior](#cómo-se-comunica-con-el-exterior)
7. [Reglas inviolables](#reglas-inviolables)

---

## Propósito

`common` es el **shared kernel** del Core. Provee todo lo que tiene sentido compartir entre los plugins de protocolo y el BE consumidor:

- Interfaces SPI (`CorePlugin`, `StepComponent`, `StepCatalogProvider`, `UiDriver`).
- Runtime de ejecución (`CucumberRuntimeEngine`, `ExecutionContext`, `ExecutionConfig`/`Result`/`Request`).
- Abstracciones de transporte (`ExecutionTransport`, `InProcessTransport`, `HttpAgentTransport`).
- Reporter semántico (`StepReporter`) y bus de eventos (`EventBus`).
- Pipeline de reporting Extent HTML.
- Utilidades cross-cutting: SSL, logging, JSON, configuración, drivers.

**No contiene** lógica específica de un protocolo — eso vive en `http-core`, `web-core`, `mobile-core`, `database-core`.

## Coordenada Maven

```groovy
dependencies {
    api 'com.qa:common:2.0.0'   // o la versión activa en el repo
}
```

Todos los módulos especializados ya la traen como `api project(':common')`.

## Dependencias clave

| Familia | Librería |
|---|---|
| BDD engine | `io.cucumber:cucumber-java` (versión definida en root `ext.cucumberVersion`) |
| JSON | `com.fasterxml.jackson.core:jackson-databind` (root `ext.jacksonVersion`) + `com.jayway.jsonpath:json-path` + `org.skyscreamer:jsonassert` |
| YAML | `org.yaml:snakeyaml` |
| HTTP cliente (Jira, integraciones) | `org.apache.httpcomponents:httpclient:4.5.14` |
| Reporting | `com.aventstack:extentreports:5.1.2` |
| Logging | `org.slf4j:slf4j-api` + `ch.qos.logback:logback-classic` |
| Crypto | `org.bouncycastle:bcprov-jdk18on` |

## Cómo se usa standalone

`common` no se ejecuta solo (no contiene plugin propio). El uso típico es como dependencia transitiva:

```groovy
// En cualquier proyecto de tests Cucumber:
dependencies {
    api 'com.qa:common:2.0.0'
    api 'com.qa:http-core:2.0.0'    // o web-core / mobile-core / database-core
}
```

Para invocar el engine programáticamente desde código:

```java
ExecutionConfig cfg = new ExecutionConfig.Builder()
        .environment("qa")
        .tags("@smoke")
        .build();

InProcessTransport transport = InProcessTransport.withDefaults();   // descubre plugins via SPI
ExecutionHandle handle = transport.submit(
        cfg,
        List.of("/path/to/login.feature"),
        new NoOpStepReporter());

ExecutionResult result = handle.future().get();
System.out.println("Status: " + result.getStatus());
```

## Áreas funcionales

### SPI — `CorePlugin` + `StepComponent`

`com.qa.common.runtime.CorePlugin` es el contrato que cada módulo de protocolo implementa para integrarse:

- **Identidad:** `platformId()`, `displayName()`, `version()`.
- **Introspección:** `describeCapabilities()` → lista de `CapabilityReport` que el FE usa para construir selectores dinámicos (browsers disponibles, devices, etc.).
- **Steps Cucumber:** `getComponents()` y `getGluePackages()`.
- **Ciclo de vida:** `onSuiteStart/End`, `onScenarioStart/End`, `registerServices(ServiceRegistry)`.

Cada plugin se descubre vía `ServiceLoader<CorePlugin>` desde `META-INF/services/com.qa.common.runtime.CorePlugin` en su classpath.

`StepComponent` agrupa steps por responsabilidad y fase BDD. Su API rica (id estable, displayName i18n, keywords, deprecación) está documentada inline en la clase.

### Runtime — `CucumberRuntimeEngine` + `ExecutionConfig`

`CucumberRuntimeEngine.withServiceLoader()` arma un engine usando todos los plugins discoverable. `ExecutionConfig` (POJO inmutable, builder fluido) lleva environment, browser, tags, threads, SSL, http-engine y propiedades opacas.

`ExecutionContext` es el carry-on por escenario: registry de servicios, variable store, lifecycle hooks. Es accesible vía `ExecutionContext.current()` desde dentro de los steps.

### Transport — `ExecutionTransport`

Abstracción minimalista (2 métodos):
- `ExecutionHandle submit(config, featurePaths, reporter)`
- `List<CapabilityReport> describeCapabilities()`

Implementaciones:
- **`InProcessTransport`** (TASK-I02): invoca el engine en la misma JVM. Default para tests integrados.
- **`HttpAgentTransport`** (TASK-I03): delega a `mobile-agent` vía HTTP+SSE. Producción.

> **R-1 RFC-AGENT-01:** el BE en producción NUNCA instancia `InProcessTransport` directamente — siempre `HttpAgentTransport`.

### Reporter — `StepReporter`

Contrato semántico para reportar progreso en tiempo real:
`onScenarioStarted / onStepStarted / onStepPassed / onStepFailed / onStepSkipped / onScenarioCompleted / onExecutionCompleted`.

Implementaciones útiles:
- `NoOpStepReporter` (tests/standalone).
- `EventBusStepReporterAdapter` (publica al EventBus interno).
- `BEStepReporter` (en el BE — despacha eventos al WebSocket del FE).

### Reporting — Extent HTML pipeline

`com.qa.common.reporting` genera reportes HTML a partir del `cucumber.json` producido por Cucumber al finalizar la suite (`TestRunFinished`).

```
Cucumber → cucumber.json
   ↓ TestRunFinished
CucumberReportingPlugin
   ↓
ReportingManager.pipeline:
   ├── ConversionStep        cucumber.json → TestExecutionResult
   └── ExtentGenerationStep  TestExecutionResult → execution-report.html
```

**Cómo activarlo:** `cucumber.plugin = com.qa.common.reporting.cucumber.CucumberReportingPlugin` en la config Cucumber del runner.

**Output:** `build/reports/extent/execution-report.html`.

**Estado:** integraciones con Jira/Xray, Azure DevOps, etc. NO viven en este módulo — son responsabilidad del BE (`com.qa.platform.execution.infrastructure.testmanagement.*`).

### SSL / TLS — `SSLContextFactory` + truststore

`com.qa.common.ssl.SSLContextFactory` provee `SSLContext` listo para Apache HttpClient, OkHttp y APIRequestContext de Playwright. Auto-descubre el truststore en estos paths:

1. `<repo>/qa-platformCore/common/ssl/myTrustStore.jks`
2. `<repo>/common/ssl/myTrustStore.jks`
3. `ssl/myTrustStore.jks` (relativo al cwd)

**Truststore:** Java Keystore con certificados de servicios externos a los que el framework debe llegar (Jira corporativo, APIs internas con TLS custom). Commiteado al repo (cero secretos — solo certs públicos del servidor).

**Resuelve:** `PKIX path building failed: unable to find valid certification path to requested target`.

> Para reemplazar el truststore: regenerarlo con las CAs/certs nuevas, sustituir el `.jks`, commit. La password (si aplica) vive en el secret manager — NUNCA en el repo.

### Step Catalog i18n — SPI

`com.qa.common.stepcatalog.StepCatalogProvider` es la SPI que cada módulo de protocolo implementa para exponer su catálogo de steps al BE en múltiples idiomas (ES/EN/FR). El BE agrega los providers vía `StepCatalogAggregator` y sirve el resultado en `GET /api/steps/catalog?locale=es`.

## Cómo se comunica con el exterior

| Consumidor | Ruta de entrada | Notas |
|---|---|---|
| **BE** | `ExecutionTransport.submit` + `StepReporter` | El BE inyecta `InProcessTransport` (CI) o `HttpAgentTransport` (prod). |
| **BE** | `ServiceLoader.load(CorePlugin.class)` + `getComponents()` | Para construir el step catalog del FE. |
| **mobile-agent** | `InProcessTransport.withDefaults()` | El agente delega al engine en su propia JVM tras recibir el `POST /v1/runs`. |
| **Tests integrados (CI)** | `CucumberRuntimeEngine.withServiceLoader()` | Path corto sin transport; usado por las suites BDD del Core. |

## Reglas inviolables

- **R-COMMON-1:** `common` **no depende** de ningún módulo especializado del Core (`http-core`, `web-core`, `mobile-core`, `database-core`). ArchUnit guard activo.
- **R-COMMON-2:** ningún módulo del Core usa `System.out` ni `java.util.logging`. Solo SLF4J.
- **R-COMMON-3:** los DTOs públicos de `common` (en `runtime/`, `transport/`, `transport/wire/`) son **contratos estables**. Cambios son aditivos (R-2 RFC-AGENT-01).
- **R-COMMON-4:** secretos NUNCA viven en `common/ssl/` — solo certs públicos. Las passwords/keys van por env var o secret manager.
- **R-COMMON-5:** la `ServiceRegistry` por escenario es scoped — no compartir state global entre escenarios sin pasar por el `LifecycleManager`.

---

> **Para profundizar en un componente concreto:** los Javadoc de `CorePlugin`, `StepComponent`, `ExecutionTransport`, `SSLContextFactory` y `ReportingManager` son la fuente canónica.
>
> **Para ver el catálogo de steps:** ver el `COMPONENTS.md` de cada módulo de protocolo.
