# http-core — Testing HTTP / REST / GraphQL

Módulo del `qa-platformCore` especializado en testing de APIs HTTP. Provee step definitions Cucumber, dos motores HTTP intercambiables (Playwright APIRequestContext default + Apache HttpClient 5 fallback), validaciones de contrato y reporting detallado de cada request.

> **Coordenada Maven:** `com.qa:http-core:<version>`
> **Catálogo público de pasos:** [COMPONENTS.md](COMPONENTS.md) (auto-generado)

---

## Tabla de contenidos

1. [Propósito](#propósito)
2. [Coordenada Maven](#coordenada-maven)
3. [Dependencias clave](#dependencias-clave)
4. [Capabilities reportadas](#capabilities-reportadas)
5. [Cómo se usa standalone](#cómo-se-usa-standalone)
6. [Selección de motor HTTP](#selección-de-motor-http)
7. [Cómo se comunica con el exterior](#cómo-se-comunica-con-el-exterior)
8. [Component Catalog](#component-catalog)
9. [Reglas inviolables](#reglas-inviolables)

---

## Propósito

`http-core` aporta al Core los componentes para validar APIs HTTP/REST/GraphQL desde un escenario Gherkin:

- Configuración: URL base, headers, cookies, autenticación (Basic/Bearer/JWT/OAuth), body (JSON/raw/form/multipart), parámetros de query.
- Ejecución: GET/POST/PUT/PATCH/DELETE/HEAD/OPTIONS.
- Validación: status code, body (JSONPath, schemas JSON), headers, performance (SLA), security (TLS, headers HSTS/CSP).
- Reporting: cada request HTTP queda con request/response redactado en el reporte Extent + WebSocket del FE.

## Coordenada Maven

```groovy
dependencies {
    api 'com.qa:http-core:2.0.0'
}
```

Trae transitivamente `common`. **Ningún consumidor necesita declarar `common` por separado.**

## Dependencias clave

| Familia | Librería | Uso |
|---|---|---|
| Engine HTTP default | `com.microsoft.playwright:playwright:1.50.0` | `PlaywrightHttpClientImpl` (TASK-E03) |
| Engine HTTP fallback | `org.apache.httpcomponents.client5:httpclient5` | `ApacheHttpClientImpl` |
| JSON | jackson, json-path, jsonassert (vía common) | Validaciones body |
| Schema validation | `com.networknt:json-schema-validator` | Aserción contra JSON Schema |
| Diff de JSON | jsonassert (`org.skyscreamer`) | Comparación tolerante |

> **Eliminado en TASK-J01:** Unirest. `BaseHttpClient` fue removido del classpath. `HttpClientFactory` redirige `-Dhttp.client=unirest` → `ApacheHttpClientImpl` con warning.

## Capabilities reportadas

`ApiPlugin.describeCapabilities()` reporta `CapabilityReport.available("HTTP", [...])` con descriptors de los motores disponibles. El BE expone esto en `GET /api/executions/http-engines` (TASK-EW-E) que el FE consume vía `useHttpEngines()`.

| Engine | Default | Notas |
|---|---|---|
| `PLAYWRIGHT` | sí (configurable vía `HttpEngine.resolveDefault()`) | Multi-protocol (HTTP/2, HTTP/3 opcional), interceptación, mocking nativo. |
| `APACHE` | no | Fallback estable; usar cuando se requiere control fino del connection pool. |

## Cómo se usa standalone

```gherkin
Feature: API testing standalone
  Scenario: Listar usuarios paginados
    Given establezco la URL base "https://api.example.com"
    And agrego el header "Authorization" con valor "Bearer {{token}}"
    And agrego el parámetro "page" con valor "1"
    When envío GET a "/users"
    Then el código de respuesta es 200
    And la respuesta JSON contiene "$.data[0].id"
    And el tiempo de respuesta es menor a 500 ms
```

Para correr localmente sin BE:

```groovy
// build.gradle del proyecto de tests
dependencies {
    testImplementation 'com.qa:http-core:2.0.0'
    testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.34.3'
}
```

Y un runner JUnit Platform que apunte a `glue = ["com.qa.httpcore.steps"]`.

## Selección de motor HTTP

| Mecanismo | Prioridad | Ejemplo |
|---|---|---|
| Campo `httpEngine` del `ExecutionConfig` | más alta | API directa desde el BE |
| System property | media | `-Dexecution.http.engine=APACHE` |
| Variable de entorno | baja | `EXECUTION_HTTP_ENGINE=APACHE` |
| Default del Core | fallback | `PLAYWRIGHT` |

Resolución centralizada en `com.qa.common.runtime.HttpEngine.resolveDefault()`.

## Cómo se comunica con el exterior

| Quién | Cómo | Notas |
|---|---|---|
| **BE** | importa `com.qa.common.*` y configura `ExecutionConfig.httpEngine` | NO importa `com.qa.httpcore.*` (ArchUnit `H04 #1`). Excepción transitoria: 4 tipos `httpcore.reporting.*` (RFC-REPORTING-01). |
| **FE** | consume `GET /api/executions/http-engines` para poblar el selector | Hook `useHttpEngines()` (TASK-EW-E). |
| **Engine Cucumber** | descubre `ApiPlugin` vía SPI | `META-INF/services/com.qa.common.runtime.CorePlugin`. |
| **mobile-agent** | recibe el feature, instancia el engine, http-core viaja en su classpath | El agente reporta también su capability HTTP. |

## Component Catalog

Lista completa, auto-generada, en [COMPONENTS.md](COMPONENTS.md). Para regenerar:

```bash
./gradlew :http-core:test --tests "*HttpComponentCatalogTest"
```

El test es **idempotente** y se ejecuta como guardia anti-drift en CI. Si la lista de componentes del `ApiPlugin` cambia, el archivo se actualiza al re-correr.

## Reglas inviolables

- **R-HTTP-1:** todos los components del módulo declaran `@StepId("api.<dominio>")` para el contrato público estable. Cambiar un id es **breaking**.
- **R-HTTP-2:** los headers `Authorization` y bodies sensibles se redactan antes de loguearse. Usar `HttpDetailRedactor` (en `httpcore.reporting`) — NUNCA `LOG.info(headers)` directo.
- **R-HTTP-3:** el módulo NO importa de `web-core`/`mobile-core`/`database-core` (ArchUnit guard cross-module).
- **R-HTTP-4:** `BaseHttpClient` (Unirest) fue eliminado en TASK-J01. Los plugins/steps nuevos NO referencian `kong.unirest.*`.
- **R-HTTP-5:** la versión legacy `-Dhttp.client=unirest` cae a Apache con warning — back-compat sólo, no usar para nuevas configs.

---

> **Para QAs/POs:** la lista canónica de "qué puedo hacer con HTTP en el Scenario Builder" está en [COMPONENTS.md](COMPONENTS.md). Cada componente lleva `displayName` ES/EN/FR, fase BDD, ícono y keywords semánticos.
