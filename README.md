# qa-platformCore

Motor de testing end-to-end para CuAleon Test Engineering Platform. Modular, basado en plugins SPI, agnóstico al lugar donde corre (in-process o agente remoto).

> **Audiencia:**
> - **QAs / POs** — diseñan escenarios desde el Scenario Builder del Backend; aquí encuentran qué pueden hacer y cómo.
> - **Devs del Core** — extienden / mantienen los módulos especializados.
> - **Devs del BE** — consumen el Core como dependencia Maven y lo invocan via `ExecutionTransport`.

---

## Tabla de contenidos

1. [¿Qué es el Core?](#qué-es-el-core)
2. [Arquitectura — vista de pájaro](#arquitectura--vista-de-pájaro)
3. [Cómo se comunica con el BE](#cómo-se-comunica-con-el-be)
4. [Reglas de comunicación entre módulos](#reglas-de-comunicación-entre-módulos)
5. [Módulos](#módulos)
6. [Cómo escribir un escenario hoy](#cómo-escribir-un-escenario-hoy)
7. [Build / publicar / consumir](#build--publicar--consumir)
8. [Modo agente remoto (mobile-agent)](#modo-agente-remoto-mobile-agent)
9. [Convenciones](#convenciones)
10. [Documentos relacionados](#documentos-relacionados)

---

## ¿Qué es el Core?

`qa-platformCore` es el motor de ejecución de tests del producto. Provee:

- **Cucumber engine** que orquesta features `.feature` con plugins de protocolo (HTTP, Web, Mobile, DB).
- **Step components** clasificados por dominio + fase BDD; consumibles desde el Scenario Builder visual del FE.
- **Plugin SPI** (`CorePlugin`) — cada módulo especializado se descubre vía `ServiceLoader` y aporta sus components, capabilities y hooks de ciclo de vida.
- **Reporting** integrado (Extent HTML).
- **Transport abstraction** (`ExecutionTransport`) — el mismo engine puede invocarse en la JVM del BE (`InProcessTransport`) o sobre un agente remoto (`HttpAgentTransport` → `mobile-agent`).

El Core **no contiene lógica de negocio del producto** ni de un dominio vertical concreto: es genérico y reutilizable. Todo lo específico del usuario vive en el Backend (CRUD de proyectos, gestión de ambientes, persistencia de escenarios) o en el Frontend (UI).

## Arquitectura — vista de pájaro

```
                ┌──────────────────────────────────────────────────────────┐
                │                     qa-platformBE                        │
                │   (Spring Boot — REST API · WebSocket · BD · Auth)       │
                └──────┬──────────────────────────────────┬────────────────┘
                       │                                  │
              ExecutionTransport.submit          GET /steps/catalog
                       │                                  │
                       ▼                                  ▼
              ┌──────────────────┐              ┌────────────────────┐
              │ InProcessTransport│              │ StepDiscoveryService│
              │ (default, mismo  │              │ (SPI ServiceLoader) │
              │  proceso)         │              └─────────┬──────────┘
              └─────────┬────────┘                        │
                        │ o HttpAgentTransport            │
                        ▼ (mobile-agent remoto)           │
              ┌──────────────────────────────────────────────────┐
              │            CucumberRuntimeEngine (common)        │
              │  ─────────────────────────────────────────────   │
              │  Discovers via SPI:    CorePlugin                │
              │     ├── ApiPlugin         (http-core)            │
              │     ├── WebPlugin         (web-core)             │
              │     ├── MobilePlugin      (mobile-core)          │
              │     └── DatabasePlugin    (database-core)        │
              └──────────────────────────────────────────────────┘
```

**Cómo se descubren los componentes:** cada módulo trae un archivo
`META-INF/services/com.qa.common.runtime.CorePlugin` apuntando a su `*Plugin`.
Al arrancar, el engine carga todos los plugins disponibles en el classpath.

## Cómo se comunica con el BE

El Backend **NO importa entities ni clases internas del Core** — habla por dos puertas:

| Puerta | Quién la inicia | Para qué |
|---|---|---|
| **`ExecutionTransport.submit(config, featurePaths, reporter)`** | BE | Ejecutar un set de `.feature`. Recibe `ExecutionHandle` (future + cancelHook). Eventos vía `StepReporter`. |
| **`StepDiscoveryService` + `StepCatalogProvider` (SPI)** | BE | Listar componentes / steps con i18n para construir la paleta del FE. |

El **único acoplamiento "tipado"** del BE con el Core es a través de:

- `com.qa.common.runtime.*` — `ExecutionConfig`, `ExecutionRequest`, `ExecutionResult`, `HttpEngine`, `StepReporter`.
- `com.qa.common.transport.*` — interfaces de ejecución.
- `com.qa.common.driver.CapabilityReport` / `CapabilityDescriptor`.
- `com.qa.common.stepcatalog.*` — SPI de catálogo i18n.

Reglas que un dev del BE **NO debe romper**:

- ❌ Importar `com.qa.httpcore.*`, `com.qa.webcore.*`, `com.qa.mobilecore.*`, `com.qa.databasecore.*` en código BE (ArchUnit guard `H04 #1`). Excepción transitoria documentada: 4 tipos `httpcore.reporting.*` legacy migrarán a `common` en RFC-REPORTING-01.
- ❌ Crear instancias directas de `InProcessTransport` en producción (R-1 RFC-AGENT-01). Producción usa `HttpAgentTransport` → `mobile-agent`.
- ❌ Loguear el body crudo de un `StepReporter.onStepFailed` — usar el redactado.

## Reglas de comunicación entre módulos

Las reglas se enforced con **ArchUnit** (`<module>/src/test/java/.../arch/ArchitectureTest.java`):

1. **`common` es el shared kernel.** Todos los módulos especializados dependen de `common`. `common` **no depende** de ningún módulo especializado.
2. **Los módulos especializados NO se conocen entre sí.** `http-core` no importa de `web-core`/`mobile-core`/`database-core`, y viceversa. La integración cross-protocol vive en el caller (BE / tests).
3. **El BE solo importa de `common`** (`com.qa.common.*`) y, transitoriamente, los 4 tipos legacy de `httpcore.reporting.*` (deuda con plan de salida en RFC-REPORTING-01).
4. **Los `StepComponent` son contratos públicos.** Cambiar un `@StepId` es **breaking** — rompe escenarios persistidos en BD del BE. Para deprecar usar `@StepId(deprecated=true, replacedBy="<nuevo>")` y mantener al menos un sprint para migración FE.
5. **No usar `System.out` ni `java.util.logging`** (regla `no_standard_streams` + `no_java_util_logging`). Usar SLF4J.
6. **El `mobile-agent` NO depende del BE** — es Core puro, deployable por separado.

## Módulos

| Módulo | Propósito | Coordenada Maven | Catálogo |
|---|---|---|---|
| [`common`](common/README.md) | Shared kernel: SPI, runtime, reporting, SSL, ExecutionTransport | `com.qa:common:<v>` | (no expone components) |
| [`http-core`](http-core/README.md) | Testing HTTP/REST/GraphQL — Playwright + Apache | `com.qa:http-core:<v>` | [COMPONENTS.md](http-core/COMPONENTS.md) |
| [`web-core`](web-core/README.md) | Testing Web — Playwright (default) / Selenium | `com.qa:web-core:<v>` | [COMPONENTS.md](web-core/COMPONENTS.md) |
| [`mobile-core`](mobile-core/README.md) | Testing Mobile — Appium 8 (Android + iOS) | `com.qa:mobile-core:<v>` | [COMPONENTS.md](mobile-core/COMPONENTS.md) |
| [`database-core`](database-core/README.md) | Testing SQL — Oracle / PostgreSQL / MySQL / SQL Server | `com.qa:database-core:<v>` | [COMPONENTS.md](database-core/COMPONENTS.md) |
| [`mobile-agent`](mobile-agent/README.md) | Spring Boot wrapper sobre `InProcessTransport` para correr el engine en una máquina externa con Android SDK / iOS Simulator | (no se publica — bootJar ejecutable) | (n/a) |

## Cómo escribir un escenario hoy

**Audiencia:** QAs/POs trabajando desde el Scenario Builder del FE.

1. Elegí la **plataforma** (HTTP / Web / Mobile / DB) — el FE filtra los componentes disponibles según el plugin activo del proyecto.
2. Para cada step, elegís un **`StepComponent`** desde la paleta. Cada componente lleva `@StepId`, displayName i18n, fase BDD (Given/When/Then), keywords y ejemplo Gherkin.
3. La lista completa por módulo está en su `COMPONENTS.md` — son **contratos estables** versionados.
4. Cuando ejecutás el escenario, el BE serializa los pasos a `.feature` y los entrega al Core vía `ExecutionTransport.submit`.

**Ejemplo end-to-end (HTTP):**
```gherkin
Feature: API testing
  Scenario: Listar usuarios
    Given establezco la URL base "https://api.example.com"
    And agrego el header "Authorization" con valor "Bearer {{token}}"
    When envío GET a "/users"
    Then el código de respuesta es 200
    And la respuesta JSON contiene "$[0].id"
```

## Build / publicar / consumir

```bash
# Compilar y testear todo
./gradlew build

# Publicar a Maven Local (consumido por qa-platformBE en dev)
./gradlew publishToMavenLocal

# Regenerar todos los COMPONENTS.md (uno por módulo especializado)
./gradlew generateComponentCatalog
# (alias de los 4 :module:test --tests "*ComponentCatalogTest")
```

**Versionado:** `<major>.<minor>.<patch>-SNAPSHOT` durante desarrollo. Releases tagueados en GitHub. Los snapshots van a Maven Local; los releases a GitHub Packages.

## Modo agente remoto (mobile-agent)

Cuando el test necesita Android SDK / iOS Simulator (o cualquier capability que no podemos instalar en el server del BE), el BE delega al [`mobile-agent`](mobile-agent/README.md) — un Spring Boot que empaqueta el engine y expone wire-protocol v1 (HTTP + SSE):

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/v1/runs` | Submit (body = `featureContents` map) |
| `GET`  | `/v1/runs/{id}/events` | Stream SSE de eventos `StepReporter` |
| `POST` | `/v1/runs/{id}/cancel` | Idempotente |
| `GET`  | `/v1/capabilities` | Capabilities reportadas por los plugins |

El BE consume vía `com.qa.common.transport.HttpAgentTransport` (TASK-I03 — JDK builtin, cero deps).

## Convenciones

- **Java 21** en todos los módulos. Toolchain enforced en cada `build.gradle`.
- **Encoding UTF-8** + flags `-Xlint:unchecked -Xlint:deprecation`.
- **Tests:** JUnit 5 + Mockito + AssertJ. ArchUnit para guards arquitectónicos.
- **Logging:** SLF4J + Logback. No `System.out`, no `java.util.logging`.
- **CVEs aceptados:** documentados en [`docs/SECURITY_ACCEPTED_RISKS.md`](../docs/SECURITY_ACCEPTED_RISKS.md) (TASK-J02). Política de revisión 90 días.
- **Cliente HTTP:** `HttpClientFactory.create(config)` (Playwright o Apache 5). Unirest fue eliminado en TASK-J01.

## Documentos relacionados

| Documento | Para qué |
|---|---|
| [propuesta-desde-0-core.md](../propuesta-desde-0-core.md) | Plan maestro / roadmap / RFCs activos |
| [docs/SECURITY_ACCEPTED_RISKS.md](../docs/SECURITY_ACCEPTED_RISKS.md) | Registro formal de CVEs aceptados |
| [docs/MIGRATION_GUIDE_v2_to_v2.1.md](../docs/MIGRATION_GUIDE_v2_to_v2.1.md) | Guía de migración para consumidores externos (api-core → http-core, etc.) |
| [common/README.md](common/README.md) | Shared kernel — SPI, runtime, reporting, SSL |
| [`*/COMPONENTS.md`](#módulos) | Catálogo público de StepComponents por módulo |

---

**Mantenedor:** Equipo de plataforma (rotativo cada quarter).
**Última revisión:** 2026-05-10 (TASK-J03).
