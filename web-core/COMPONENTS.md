# web-core — Component Catalog

> **Auto-generado** — TASK-J03. NO editar manualmente.
> Regenerar con `./gradlew :web-core:test --tests "*ComponentCatalogTest"`.
>
> **Plugin:** `com.qa.webcore.plugin.WebPlugin`  
> **Platform ID:** `WEB`  
> **Display:** Web Browser Testing  
> **Componentes:** 16  
> **Última generación:** 2026-06-09

Esta tabla es el **contrato público** de los `StepComponent` que el módulo `web-core` expone al Backend (catálogo i18n) y al Frontend (paleta del Scenario Builder). Cada entrada se deriva por reflexión vía SPI (`ServiceLoader<CorePlugin>`).

**Convenciones**

- `@StepId`: identificador estable. Cambiar un id es **breaking** (rompe escenarios persistidos en BD del BE).
- Fase BDD: `GIVEN | WHEN | THEN | ANY`.
- Keywords: enriquecen el `ScenarioSuggestionEngine` (TASK-C06).
- Locale: si una clave no aparece en `displayNameByLocale`, el FE cae a `displayName` (ES).

---

## `web.browser.config`

- **Display:** **Configuracion de Navegador** _(es)_ / **Browser Configuration** _(en)_ / **Configuration du navigateur** _(fr)_
- **Categoría:** Configuracion Web  · **Fase BDD:** `GIVEN`  · **Display order:** `10`
- **Icono:** `web`  · **Keywords:** browser, navegador, config, headless, chrome, firefox, viewport, window-size, capability, browser-config
- **Descripción:** Configuracion del browser, modo headless, capabilities
- **Glue:** `com.qa.webcore.steps.config.BrowserConfigSteps`

## `web.environment`

- **Display:** **Ambiente Web** _(es)_ / **Web Environment** _(en)_ / **Environnement Web** _(fr)_
- **Categoría:** Configuracion Web  · **Fase BDD:** `GIVEN`  · **Display order:** `20`
- **Icono:** `settings`  · **Keywords:** environment, entorno, config, variable, env-var, web-config, ambiente, setting
- **Descripción:** URL base, timeouts, cookies de configuracion
- **Glue:** `com.qa.webcore.steps.config.WebEnvironmentSteps`

## `web.navigation`

- **Display:** **Navegacion** _(es)_ / **Navigation** _(en)_ / **Navigation** _(fr)_
- **Categoría:** Navegacion  · **Fase BDD:** `WHEN`  · **Display order:** `30`
- **Icono:** `navigation`  · **Keywords:** navigate, navegar, go-to, open, url, page, load, back, forward, refresh, pagina
- **Descripción:** Navegar a URL, historial, refresh, flujos complejos
- **Glue:** `com.qa.webcore.steps.navigation.NavigationSteps`

## `web.frame`

- **Display:** **Frames e iFrames** _(es)_ / **Frames & iFrames** _(en)_ / **Frames et iFrames** _(fr)_
- **Categoría:** Navegacion  · **Fase BDD:** `WHEN`  · **Display order:** `40`
- **Icono:** `layers`  · **Keywords:** frame, iframe, context, switch-to, shadow-dom, nested-frame, cambiar-contexto, embebido
- **Descripción:** Cambio de contexto a frames e iFrames
- **Glue:** `com.qa.webcore.steps.navigation.FrameSteps`

## `web.window`

- **Display:** **Ventanas y Pestanas** _(es)_ / **Windows & Tabs** _(en)_ / **Fenetres et onglets** _(fr)_
- **Categoría:** Navegacion  · **Fase BDD:** `WHEN`  · **Display order:** `50`
- **Icono:** `tab`  · **Keywords:** window, tab, nueva-tab, ventana, popup, switch, handle, close, new-window, onglet
- **Descripción:** Gestion de multiples ventanas y pestanas
- **Glue:** `com.qa.webcore.steps.navigation.WindowSteps`

## `web.click`

- **Display:** **Clicks e Interacciones** _(es)_ / **Clicks & Interactions** _(en)_ / **Clics et interactions** _(fr)_
- **Categoría:** Interaccion  · **Fase BDD:** `WHEN`  · **Display order:** `60`
- **Icono:** `touch_app`  · **Keywords:** click, clic, tap, press, pulsar, boton, button, link, enlace, interact
- **Descripción:** Click, doble click, click derecho, hover, shadow DOM
- **Glue:** `com.qa.webcore.steps.interaction.ClickSteps`

## `web.input`

- **Display:** **Entrada de Texto** _(es)_ / **Text Input** _(en)_ / **Saisie de texte** _(fr)_
- **Categoría:** Interaccion  · **Fase BDD:** `WHEN`  · **Display order:** `70`
- **Icono:** `keyboard`  · **Keywords:** input, text, write, type, fill, completar, digitar, field, campo, textbox, textarea, clear
- **Descripción:** Escribir texto, limpiar campos, teclado
- **Glue:** `com.qa.webcore.steps.interaction.InputSteps`

## `web.select`

- **Display:** **Select y Dropdowns** _(es)_ / **Select & Dropdowns** _(en)_ / **Selecteurs et menus deroulants** _(fr)_
- **Categoría:** Interaccion  · **Fase BDD:** `WHEN`  · **Display order:** `80`
- **Icono:** `arrow_drop_down`  · **Keywords:** select, dropdown, option, opcion, combo, choose, elegir, listbox, desplegable
- **Descripción:** Seleccion de opciones en elementos select y radio
- **Glue:** `com.qa.webcore.steps.interaction.SelectSteps`

## `web.scroll`

- **Display:** **Scroll** _(es)_ / **Scroll** _(en)_ / **Defilement** _(fr)_
- **Categoría:** Interaccion  · **Fase BDD:** `WHEN`  · **Display order:** `90`
- **Icono:** `swap_vert`  · **Keywords:** scroll, desplazar, bajar, subir, down, up, scroll-to, wheel, deslizar
- **Descripción:** Scroll hacia elementos o direcciones
- **Glue:** `com.qa.webcore.steps.interaction.ScrollSteps`

## `web.dragdrop`

> ⚠️ **Deprecado** — usar `web.drag.drop` en su lugar.

- **Display:** **Drag and Drop** _(es)_ / **Drag and Drop** _(en)_ / **Glisser-deposer** _(fr)_
- **Categoría:** Interaccion  · **Fase BDD:** `WHEN`  · **Display order:** `95`
- **Icono:** `open_with`  · **Keywords:** drag, drop, arrastar, soltar, move, mover, reorder, dnd, drag-and-drop, glisser-deposer
- **Descripción:** Arrastrar y soltar elementos
- **Glue:** `com.qa.webcore.steps.interaction.DragDropSteps`

## `web.alert`

- **Display:** **Alertas y Dialogos** _(es)_ / **Alerts & Dialogs** _(en)_ / **Alertes et boites de dialogue** _(fr)_
- **Categoría:** Interaccion  · **Fase BDD:** `WHEN`  · **Display order:** `100`
- **Icono:** `warning`  · **Keywords:** alert, popup, modal, dialog, dialogo, alerte, confirm, accept, dismiss, javascript-alert
- **Descripción:** Aceptar, cancelar y leer alertas del navegador
- **Glue:** `com.qa.webcore.steps.interaction.AlertSteps`

## `web.wait`

- **Display:** **Esperas** _(es)_ / **Waits** _(en)_ / **Attentes** _(fr)_
- **Categoría:** Esperas  · **Fase BDD:** `WHEN`  · **Display order:** `110`
- **Icono:** `hourglass_empty`  · **Keywords:** wait, esperar, pause, pausa, attendre, timeout, visible, presence, loading, spinner
- **Descripción:** Esperas explicitas sobre elementos y condiciones
- **Glue:** `com.qa.webcore.steps.wait.WaitSteps`

## `web.validation.element`

- **Display:** **Validacion de Elementos** _(es)_ / **Element Validation** _(en)_ / **Validation des elements** _(fr)_
- **Categoría:** Validacion Web  · **Fase BDD:** `THEN`  · **Display order:** `120`
- **Icono:** `check_box`  · **Keywords:** element, elemento, visible, enabled, disabled, present, exists, assert, check, validar
- **Descripción:** Visibilidad, texto, atributos y estado de elementos
- **Glue:** `com.qa.webcore.steps.validation.ElementValidationSteps`

## `web.validation.page`

- **Display:** **Validacion de Pagina** _(es)_ / **Page Validation** _(en)_ / **Validation de la page** _(fr)_
- **Categoría:** Validacion Web  · **Fase BDD:** `THEN`  · **Display order:** `130`
- **Icono:** `pageview`  · **Keywords:** page, titulo, title, url, current-url, validar-pagina, assert-url, source, pagina-cargada
- **Descripción:** Titulo, URL, existencia de texto en pagina
- **Glue:** `com.qa.webcore.steps.validation.PageValidationSteps`

## `web.validation.table`

- **Display:** **Validacion de Tablas** _(es)_ / **Table Validation** _(en)_ / **Validation des tableaux** _(fr)_
- **Categoría:** Validacion Web  · **Fase BDD:** `THEN`  · **Display order:** `140`
- **Icono:** `table_chart`  · **Keywords:** table, tabla, row, fila, cell, celda, column, columna, grid, datagrid
- **Descripción:** Filas, columnas, cabeceras y busqueda en tablas
- **Glue:** `com.qa.webcore.steps.validation.TableValidationSteps`

## `web.screenshot`

- **Display:** **Capturas de Pantalla** _(es)_ / **Screenshots** _(en)_ / **Captures d'ecran** _(fr)_
- **Categoría:** Validacion Web  · **Fase BDD:** `THEN`  · **Display order:** `150`
- **Icono:** `photo_camera`  · **Keywords:** screenshot, captura, foto, imagen, evidencia, capture, pantalla, take-screenshot, capture-screen
- **Descripción:** Capturar evidencia y adjuntar al reporte
- **Glue:** `com.qa.webcore.steps.validation.ScreenshotSteps`

---

> Para añadir un componente nuevo: implementar `com.qa.common.api.runtime.StepComponent`, anotar la clase con `@com.qa.common.api.runtime.annotation.StepId("<id>")`, registrarla en el plugin (`getComponents()`), y regenerar este documento.

> Para deprecar: marcar `@StepId(value=..., deprecated=true, replacedBy="<nuevo-id>")` y mantener la clase activa al menos un sprint para permitir migración FE.
