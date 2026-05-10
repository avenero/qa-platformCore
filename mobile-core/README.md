# mobile-core — Testing Mobile (Appium 8)

Módulo del `qa-platformCore` para automatización de aplicaciones móviles nativas e híbridas. Backend: **Appium 2** + drivers `UiAutomator2` (Android) y `XCUITest` (iOS).

> **Coordenada Maven:** `com.qa:mobile-core:<version>`
> **Catálogo público de pasos:** [COMPONENTS.md](COMPONENTS.md) (auto-generado)

---

## Tabla de contenidos

1. [Propósito](#propósito)
2. [Coordenada Maven](#coordenada-maven)
3. [Dependencias clave + CVEs](#dependencias-clave--cves)
4. [Capabilities reportadas](#capabilities-reportadas)
5. [Cómo se usa standalone](#cómo-se-usa-standalone)
6. [Configuración del device](#configuración-del-device)
7. [Cómo se comunica con el exterior](#cómo-se-comunica-con-el-exterior)
8. [Despliegue remoto: `mobile-agent`](#despliegue-remoto-mobile-agent)
9. [Component Catalog](#component-catalog)
10. [Reglas inviolables](#reglas-inviolables)

---

## Propósito

`mobile-core` permite escribir escenarios Gherkin que automatizan apps móviles:

- **App management:** instalar / desinstalar / abrir / cerrar / background.
- **Native elements:** find por id/accessibility/xpath, tap, type, scroll, swipe.
- **Gestures:** swipe, pinch, long-press, drag, multi-touch.
- **Sensors:** localización, orientación, batería simulada.
- **Permisos:** cámara, mic, ubicación, notificaciones.
- **Context switch:** native ↔ webview.
- **Validación:** estado de app, propiedades de elementos, alerts.

## Coordenada Maven

```groovy
dependencies {
    api 'com.qa:mobile-core:2.0.0'
}
```

Trae `common` transitivamente.

## Dependencias clave + CVEs

| Familia | Librería | Notas |
|---|---|---|
| Appium | `io.appium:java-client:8.6.0` | Pinneado a 8.6 por compatibilidad de Netty. |
| Selenium (subdep de Appium) | `org.seleniumhq.selenium:selenium-java:4.13.0` | |
| Image diff | `ru.yandex.qatools.ashot` | Comparación de screenshots |

**CVEs aceptados** sin parche upstream — fuente única [docs/SECURITY_ACCEPTED_RISKS.md](../../docs/SECURITY_ACCEPTED_RISKS.md) (TASK-J02). Política de revisión cada 90 días:

- `CVE-2025-58056` (LOW) — netty-codec-http chunk extensions.
- `CVE-2025-67735` (MEDIUM) — netty-codec-http CRLF injection.
- `CVE-2026-33870` (HIGH) — netty-codec-http chunked extension smuggling.

Mitigación: `netty-codec-http:4.1.121.Final` pinneado vía `constraints` en `build.gradle`.

## Capabilities reportadas

`MobilePlugin.describeCapabilities()` reporta los devices/emuladores accesibles vía Appium server. El BE/FE consumen el resultado para poblar el selector "Device".

## Cómo se usa standalone

```gherkin
Feature: Login mobile standalone
  Scenario: Login en Android
    Given inicio la app "com.example.app" en el dispositivo "emulator-5554"
    When toco el elemento con accessibility id "btn_login"
    And escribo "user@example.com" en el elemento con id "input_email"
    And escribo "{{password}}" en el elemento con id "input_password"
    And toco el elemento con id "btn_submit"
    Then el elemento con id "tv_welcome" tiene texto "Bienvenido"
```

## Configuración del device

| Propiedad | Ejemplo | Notas |
|---|---|---|
| `mobile.platform` | `android` / `ios` | |
| `mobile.device.id` | `emulator-5554` / `iPhone-15` | |
| `mobile.appium.server.url` | `http://localhost:4723` | |
| `mobile.app.path` | `/path/to/app.apk` | Local (in-process) o remoto al `mobile-agent` |

Las claves canónicas viven en `com.qa.mobilecore.config.MobileConfigKeys` — usar las constantes.

## Cómo se comunica con el exterior

| Quién | Cómo |
|---|---|
| **BE** | configura `ExecutionConfig.properties.mobile.*` + selecciona transport. NO importa `com.qa.mobilecore.*`. |
| **FE** | populates el dropdown "Device" con las capabilities reportadas. |
| **Appium server** | binding HTTP típicamente en `localhost:4723` (in-process) o en la máquina del `mobile-agent` (remoto). |
| **`mobile-agent`** | empaqueta este módulo + Appium server en una máquina con Android SDK / iOS Simulator. |

## Despliegue remoto: `mobile-agent`

Para ejecutar en una máquina externa con Android SDK / iOS Simulator (común porque el server del BE no puede tener emuladores):

```bash
# En la máquina objetivo:
export ANDROID_HOME=$HOME/Library/Android/sdk
$ANDROID_HOME/emulator/emulator -avd Pixel_5_API_33 &
java -jar mobile-agent.jar --server.port=8090
```

El BE configura `HttpAgentTransport` apuntando a esa IP:8090. Detalles en [`mobile-agent/README.md`](../mobile-agent/README.md) y RFC-AGENT-01.

## Component Catalog

[COMPONENTS.md](COMPONENTS.md) — auto-generado. Regenerar:

```bash
./gradlew :mobile-core:test --tests "*MobileComponentCatalogTest"
```

## Reglas inviolables

- **R-MOB-1:** todos los components declaran `@StepId("mobile.<dominio>")`. Cambios = breaking.
- **R-MOB-2:** el módulo NO importa de `http-core`, `web-core` ni `database-core`.
- **R-MOB-3:** los CVEs sin parche aceptados se documentan en `docs/SECURITY_ACCEPTED_RISKS.md` con dueño + fecha de revisión (R-J02-1). NO mantener listas paralelas.
- **R-MOB-4:** la versión de Appium se sincroniza con `web-core` (Selenium) — bump conjunto.
- **R-MOB-5:** las APKs / IPAs cargadas durante un test viven en el workspace efímero del agente y se borran al completar (R-MA-E del `mobile-agent`).

---

> **Para QAs/POs:** lista completa de "qué puedo hacer con Mobile" en [COMPONENTS.md](COMPONENTS.md).
