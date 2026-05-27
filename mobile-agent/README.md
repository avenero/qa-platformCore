# mobile-agent

Spring Boot wrapper sobre `InProcessTransport` (TASK-I02) que empaqueta el
`CucumberRuntimeEngine` del Core como un servicio HTTP/SSE remoto. Lo consume
`HttpAgentTransport` (TASK-I03) cuando el BE necesita correr el engine en
una máquina externa con Android SDK / iOS Simulator instalado.

## Wire-protocol v1

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/v1/runs` | Submit ejecución (body = `SubmitRequest`) |
| `GET`  | `/v1/runs/{id}/events` | Stream SSE de `AgentEvent` |
| `POST` | `/v1/runs/{id}/cancel` | Cancela idempotentemente |
| `GET`  | `/v1/capabilities` | Lista de `CapabilityReport` |
| `GET`  | `/v1/devices` | Lista de `MobileDeviceDescriptor` (devices descubiertos via ADB + xcrun). `503` si discovery falla en el host del agente. UDID NO se publica (PII filtrada en wire). _Añadido en TASK-K03COV-BE7 (parte agente)._ |
| `GET`  | `/actuator/health` | Liveness + readiness |

> **R-2 (RFC-AGENT-01):** los campos del wire-protocol son aditivos.
> **NUNCA** se renombran ni borran. Para evolucionar, añadir campos
> opcionales o un nuevo tipo de evento.

### `SubmitRequest` (JSON)

```json
{
  "environment": "qa",
  "browser": "",
  "tags": "@smoke",
  "parallelEnabled": false,
  "threadCount": 1,
  "httpEngine": "PLAYWRIGHT",
  "trustAllSsl": false,
  "properties": {
    "mobile.platform": "android",
    "mobile.device.id": "emulator-5554",
    "mobile.appium.server.url": "http://localhost:4723"
  },
  "featureContents": {
    "login.feature": "Feature: Login\n  Scenario: ...\n",
    "checkout.feature": "Feature: Checkout\n  Scenario: ...\n"
  }
}
```

**Respuesta (202 Accepted):**

```json
{ "executionId": "8b4f...-...", "eventsUrl": "/v1/runs/8b4f...-.../events" }
```

### `AgentEvent` (SSE)

```
event: STEP_PASSED
data: { "type":"STEP_PASSED", "executionId":"8b4f...", "stepText":"Given ...", "durationMs": 142, ... }
```

Tipos: `SCENARIO_STARTED`, `STEP_STARTED`, `STEP_PASSED`, `STEP_FAILED`,
`STEP_SKIPPED`, `SCENARIO_COMPLETED`, `EXECUTION_COMPLETED`, `EXECUTION_ERROR`.

## Build

```bash
./gradlew :mobile-agent:bootJar
# Output: qa-platformCore/mobile-agent/build/libs/mobile-agent-<version>.jar
```

## Run (standalone)

```bash
java -jar build/libs/mobile-agent-*.jar --server.port=8090
# Verificación rápida:
curl http://localhost:8090/v1/capabilities
curl http://localhost:8090/actuator/health
```

Variables relevantes (sobrescriben `application.yml`):

| Variable | Default | Uso |
|---|---|---|
| `SERVER_PORT` | `8090` | Puerto HTTP |
| `AGENT_WORKSPACE` | `${java.io.tmpdir}/mobile-agent` | Donde se materializan los `.feature` recibidos |
| `AGENT_SSE_BUFFER_CAPACITY` | `1024` | Capacidad de la cola por ejecución |

## Docker

```bash
./gradlew :mobile-agent:bootJar
docker build -t mobile-agent:local -f Dockerfile .
docker run --rm -p 8090:8090 mobile-agent:local
```

> La image base **NO** incluye Android SDK. Para mobile real, derivar una
> image que monte `ANDROID_HOME` o `apt-install` el SDK; mantener la
> base ligera para web/HTTP/DB.

## Despliegue con Android SDK

En la máquina externa (Linux/macOS) con Android SDK + emulador instalados:

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
$ANDROID_HOME/emulator/emulator -avd Pixel_5_API_33 &
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --install "platform-tools"

java -jar mobile-agent.jar --server.port=8090
```

Luego desde el BE configurar `HttpAgentTransport` apuntando a esta máquina
(diseño completo en TASK-I03).

## Arquitectura

```
HttpAgentTransport (BE) ──HTTP──▶ ExecuteController
                                       │
                                       ▼
                          AgentExecutionService
                          ├── FeatureMaterializer  (escribe .feature en workspace)
                          ├── SseStepReporter      (encola AgentEvent)
                          └── ExecutionTransport   (= InProcessTransport.withDefaults())
                                       │
                                       ▼
                                CucumberRuntimeEngine (SPI: web/mobile/http/db)
```

- `InProcessTransport.withDefaults()` descubre plugins via SPI: cualquier
  módulo presente en classpath (ej `mobile-core` + `web-core`) reportará
  capabilities en `/v1/capabilities`.
- El reporter `SseStepReporter` usa una cola bounded (default 1024 eventos);
  si el cliente HTTP es lento, descarta los eventos más antiguos para
  proteger al engine de back-pressure.
- El workspace por ejecución se borra recursivamente al completar
  (`AgentExecutionService.submit` registra `whenComplete`).

## Reglas inviolables (RFC-AGENT-01 §16)

| ID | Regla |
|---|---|
| R-1 | El BE en producción NO instancia `InProcessTransport` directamente; siempre habla con este agente vía `HttpAgentTransport`. |
| R-2 | Wire-protocol v1 — sólo cambios aditivos. |
| R-5 | Tokens de auth (cuando llegue I03) **NUNCA** en logs. |

## Telemetría sugerida

- `agent.runs.submitted{outcome=accepted|rejected}` — counter
- `agent.runs.duration_ms` — histogram
- `agent.sse.events.dropped` — counter (cola overflow)
- `agent.workspace.bytes` — gauge (tamaño de `${agent.workspace}`)

Documentar en `propuesta-desde-0-core.md` Sección métricas Bloque I.
