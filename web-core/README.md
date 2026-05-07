# web-core — Automatización Web Playwright-Only

> Módulo Core para pruebas UI Web del framework `qa-platformCore`.
> Estado actual: **Playwright-only** (sin dependencias Selenium en código ni build).

---

## 1) Qué provee este módulo

- Plugin runtime: `WebPlugin`
- Motor abstracto: `BrowserEngine`
- Implementación concreta: `PlaywrightBrowserEngine`
- Ciclo de vida Playwright: `PlaywrightManager`
- Steps BDD organizados por componentes (`config`, `navigation`, `interaction`, `wait`, `validation`)

Este módulo no contiene locators de negocio. Los locators viven en los proyectos consumidores.

---

## 2) Activación y glue

- Tags de activación del plugin:
  - `@web`
  - `@ui`
  - `@browser`
  - `@playwright`
- Glue derivado por plugin:
  - paquetes de componentes
  - más paquete raíz `com.qa.webcore.steps` para hooks/steps transversales

---

## 3) Configuración relevante

Propiedades principales:

- `browser.engine=playwright`
- `playwright.browser=chromium|firefox|webkit`
- `web.headless=true|false`
- `playwright.timeout.ms=<int>`
- `playwright.screenshots.dir=<path>`
- `web.base.url=<url>`

Notas:

- `BrowserConfigSteps` normaliza browsers:
  - `chrome` y `chromium` -> `chromium`
  - `safari` y `webkit` -> `webkit`
- Defaults recomendados en CI:
  - `browser.engine=playwright`
  - `playwright.browser=chromium`
  - `web.headless=true`

---

## 4) Arquitectura resumida

```text
ExecutionContext
  -> WebPlugin.onScenarioStart()
    -> PlaywrightManager.initSuite(...)
    -> PlaywrightManager.startScenario()
    -> register BrowserEngine (PlaywrightBrowserEngine)

Steps
  -> BrowserEngine contract
  -> WebHelper (utilidades transversales Playwright)

ExecutionContext
  -> WebPlugin.onScenarioEnd()
    -> PlaywrightManager.endScenario()
```

---

## 5) Ejecución local rápida

Desde `qa-platformCore`:

```bash
./gradlew :web-core:test \
  -Dbrowser.engine=playwright \
  -Dplaywright.browser=chromium \
  -Dweb.headless=true \
  -Dplaywright.headless.compatibility=true
```

---

## 6) Alcance y compatibilidad

- `web-core` está migrado a Playwright.
- `mobile-core` mantiene su stack independiente (incluye Selenium por compatibilidad Appium).
- Existen constantes legacy en `WebConfigKeys` por compatibilidad histórica, pero la operación de `web-core` es Playwright-first.

---

## 7) Convenciones

- Contrato estable hacia afuera: steps/componentes.
- Infraestructura interna (`driver`, `engine`, `plugin`, `utils`) puede evolucionar sin romper contratos externos.
- Nuevas capacidades deben agregar tests de unidad y cobertura de regresión en `web-core`.

---

## 8) Quick Reference — Steps más usados

### Configuración mínima

```properties
browser.engine=playwright
playwright.browser=chromium
web.headless=true
```

```gherkin
Given configuro el driver del navegador "chromium" en modo headless "true"
```

Alias soportados: `chrome` → `chromium`, `safari` → `webkit`

### Navegación

```gherkin
Given actualizo URL en el navegador "https://example.com"
When navego a la URL "https://example.com/login"
And recargo la página
And voy hacia atrás en el navegador
And voy hacia adelante en el navegador
```

### Interacción

```gherkin
When hago click en el elemento "loginButton"
And ingreso el texto "qa.user" en el elemento "username"
And limpio el elemento "username"
And hago doble click en el elemento "cardItem"
And hago hover en el elemento "profileMenu"
And selecciono el valor "UY" en el combobox "countrySelect"
```

### Esperas

```gherkin
And espero hasta que elemento "dashboard" este visible
And espero hasta que elemento "loadingSpinner" no este visible
And espero hasta que elemento "submitButton" este habilitado
And espero hasta que el texto "Operación exitosa" sea visible en la página
```

Evitar esperas fijas salvo casos puntuales: `And espero 2 segundos`

### Validaciones

```gherkin
Then verifico si existe el elemento "welcomeMessage"
And verifico que el texto en "statusLabel" sea "Activo"
And verifico que el texto en "statusLabel" contenga "Act"
And verifico que el elemento "submitButton" este habilitado
And verifico que el elemento "submitButton" este deshabilitado
```

### Variables temporales

```gherkin
When guardo texto del elemento "orderNumber" en variable temporal llamada "orderId"
And guardo el valor del atributo "href" del elemento "detailLink" como "detailUrl"
Then verifico que el texto en "orderNumberLabel" sea "{orderId}"
```

### Screenshots y evidencia

```gherkin
When capturo una imagen de la pantalla
And adjunto a jira el archivo de texto llamado "evidencia"
And genero archivo de texto llamado "variables-runtime" con las variables temporales
```

---

## 9) Categorías de Steps Disponibles

| Categoría | Propósito |
|-----------|-----------|
| **Configuración** | browser/headless y contexto web |
| **Navegación** | URL, back/forward, refresh, frames, ventanas |
| **Interacción** | click, input, select, scroll, drag/drop, alertas |
| **Esperas** | visible, oculto, habilitado, texto, load state |
| **Validación** | elemento, página, tabla, screenshot/evidencia |
| **Variables** | almacenamiento temporal y reutilización en steps |

**Principios de diseño:**
- Los steps trabajan sobre el contrato `BrowserEngine` (no exponen APIs de drivers legacy)
- `WebHelper` centraliza utilidades transversales (resolución de variables, evidencia, validaciones)
- Steps deben ser parametrizados, no hardcodeados por negocio
- Nuevos steps requieren test unitario o de integración
- Steps deprecated deben tener `replacedBy` explícito y plan de retiro

### Troubleshooting rápido

- Si falla el arranque del browser en CI: verificar instalación Playwright en pipeline, `web.headless=true`, permisos del contenedor
- Si un step no se encuentra: verificar tag del escenario (`@web`/`@playwright`), módulo `web-core` cargado en runtime, glue derivado por plugin
