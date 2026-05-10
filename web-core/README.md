# web-core — Testing Web (Playwright + Selenium)

Módulo del `qa-platformCore` para automatización de UI Web. Default: **Playwright** (Chromium/Firefox/WebKit). Selenium WebDriver disponible como segundo motor para flujos legacy.

> **Coordenada Maven:** `com.qa:web-core:<version>`
> **Catálogo público de pasos:** [COMPONENTS.md](COMPONENTS.md) (auto-generado)

---

## Tabla de contenidos

1. [Propósito](#propósito)
2. [Coordenada Maven](#coordenada-maven)
3. [Dependencias clave](#dependencias-clave)
4. [Capabilities reportadas](#capabilities-reportadas)
5. [Cómo se usa standalone](#cómo-se-usa-standalone)
6. [Configuración del browser](#configuración-del-browser)
7. [Cómo se comunica con el exterior](#cómo-se-comunica-con-el-exterior)
8. [Component Catalog](#component-catalog)
9. [Reglas inviolables](#reglas-inviolables)

---

## Propósito

`web-core` aporta los components para diseñar escenarios de UI Web:

- **Navegación:** apertura/cierre de browser, ir a URL, back/forward, refresh, gestión de pestañas/ventanas/iframes.
- **Interacción:** click, input, hover, drag-drop, scroll, select, alert handling, screenshots.
- **Esperas:** explícitas (esperar visible/clickable/text/url), implícitas configurables.
- **Validación:** texto, atributos, visibilidad, estado de elemento, validación de tablas, validación de página completa.
- **Configuración:** browser, headless, viewport, locale, geolocation, timezone, network throttling.

## Coordenada Maven

```groovy
dependencies {
    api 'com.qa:web-core:2.0.0'
}
```

Trae `common` transitivamente.

## Dependencias clave

| Familia | Librería | Uso |
|---|---|---|
| Engine default | `com.microsoft.playwright:playwright:1.50.0` | `PlaywrightBrowserEngine` |
| Engine legacy | `org.seleniumhq.selenium:selenium-java:4.13.0` | Selenium WebDriver |
| Image diff | `ru.yandex.qatools.ashot` | Comparación de screenshots |

> Las versiones de Playwright se alinean con `mobile-core` (Appium) — no actualizar Playwright sin verificar compat.

## Capabilities reportadas

`WebPlugin.describeCapabilities()` reporta `CapabilityReport.available("WEB", [...])` con descriptors de browsers configurables. Lo consume el FE para poblar el selector "Browser" del Scenario Builder.

| Browser | Engine default |
|---|---|
| `chromium` | Playwright |
| `firefox`  | Playwright |
| `webkit`   | Playwright (Mac/Linux) |
| `chrome`   | Selenium (legacy) |
| `edge`     | Selenium (legacy) |

## Cómo se usa standalone

```gherkin
Feature: Login UI standalone
  Scenario: Login exitoso
    Given abro el browser en "https://app.example.com/login"
    When ingreso "user@example.com" en el campo "email"
    And ingreso "{{password}}" en el campo "password"
    And hago click en el botón "Iniciar sesión"
    Then la URL contiene "/dashboard"
    And el elemento "h1" tiene texto "Bienvenido"
```

Para correr localmente sin BE: ver el patrón en el README de [`http-core`](../http-core/README.md#cómo-se-usa-standalone).

## Configuración del browser

| Propiedad | Default | Notas |
|---|---|---|
| `web.browser` | `chromium` | Alineado con `WebConfigKeys` |
| `web.headless` | `true` en CI / `false` local | Override por `ExecutionConfig.properties` |
| `web.base.url` | (sin default) | URL base para steps relativos |
| `web.grid.enabled` | `false` | Para Selenium Grid externo |
| `web.grid.url` | — | Si grid activo |
| `driver.strategy` | `playwright` | `playwright` / `selenium` |

Las claves canónicas viven en `com.qa.webcore.config.WebConfigKeys` — usar las constantes, no literales.

## Cómo se comunica con el exterior

| Quién | Cómo |
|---|---|
| **BE** | `ExecutionConfig.browser` + propiedades `web.*`. NO importa `com.qa.webcore.*` (ArchUnit `H04 #1`). |
| **FE** | populates el dropdown "Browser" con las capabilities reportadas por `WebPlugin`. |
| **Engine Cucumber** | descubre `WebPlugin` vía SPI. |

## Component Catalog

[COMPONENTS.md](COMPONENTS.md) — auto-generado. Regenerar:

```bash
./gradlew :web-core:test --tests "*WebComponentCatalogTest"
```

## Reglas inviolables

- **R-WEB-1:** todos los components declaran `@StepId("web.<dominio>")`. Cambios = breaking.
- **R-WEB-2:** el módulo NO importa de `http-core`, `mobile-core` ni `database-core`.
- **R-WEB-3:** screenshots y videos NUNCA contienen credenciales en URL — usar `*` redactado en logs.
- **R-WEB-4:** las versiones de Playwright/Selenium se mantienen alineadas con la matriz de compat de Appium (mobile-core). No bumpear unilateralmente.

---

> **Para QAs/POs:** lista completa de "qué puedo hacer con Web" en [COMPONENTS.md](COMPONENTS.md).
