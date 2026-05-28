# mobile-core — Component Catalog

> **Auto-generado** — TASK-J03. NO editar manualmente.
> Regenerar con `./gradlew :mobile-core:test --tests "*ComponentCatalogTest"`.
>
> **Plugin:** `com.qa.mobilecore.plugin.MobilePlugin`  
> **Platform ID:** `MOBILE`  
> **Display:** Mobile App Testing  
> **Componentes:** 10  
> **Última generación:** 2026-05-28

Esta tabla es el **contrato público** de los `StepComponent` que el módulo `mobile-core` expone al Backend (catálogo i18n) y al Frontend (paleta del Scenario Builder). Cada entrada se deriva por reflexión vía SPI (`ServiceLoader<CorePlugin>`).

**Convenciones**

- `@StepId`: identificador estable. Cambiar un id es **breaking** (rompe escenarios persistidos en BD del BE).
- Fase BDD: `GIVEN | WHEN | THEN | ANY`.
- Keywords: enriquecen el `ScenarioSuggestionEngine` (TASK-C06).
- Locale: si una clave no aparece en `displayNameByLocale`, el FE cae a `displayName` (ES).

---

## `mobile.device.config`

- **Display:** **Configuracion de Dispositivo** _(es)_ / **Device Configuration** _(en)_ / **Configuration du dispositif** _(fr)_
- **Categoría:** Configuracion Mobile  · **Fase BDD:** `GIVEN`  · **Display order:** `10`
- **Icono:** `phone_android`  · **Keywords:** device, dispositivo, config, orientation, portrait, landscape, rotation, language, locale, udid
- **Descripción:** Configurar capacidades del dispositivo (plataforma, version, UDID)
- **Glue:** `com.qa.mobilecore.steps.config.DeviceConfigSteps`

## `mobile.app.management`

- **Display:** **Gestion de App** _(es)_ / **App Management** _(en)_ / **Gestion de l'application** _(fr)_
- **Categoría:** Configuracion Mobile  · **Fase BDD:** `GIVEN`  · **Display order:** `20`
- **Icono:** `apps`  · **Keywords:** app, application, install, launch, start, stop, activate, deactivate, bundle-id, app-management
- **Descripción:** Instalar, lanzar y cerrar la aplicacion movil
- **Glue:** `com.qa.mobilecore.steps.config.AppManagementSteps`

## `mobile.permissions`

- **Display:** **Permisos del Dispositivo** _(es)_ / **Device Permissions** _(en)_ / **Autorisations du dispositif** _(fr)_
- **Categoría:** Configuracion Mobile  · **Fase BDD:** `GIVEN`  · **Display order:** `60`
- **Icono:** `security`  · **Keywords:** permission, permiso, camera, location, microphone, notification, grant, allow, deny, autorisation
- **Descripción:** Conceder o denegar permisos del sistema operativo
- **Glue:** `com.qa.mobilecore.steps.device.DevicePermissionSteps`

## `mobile.gesture`

- **Display:** **Gestos** _(es)_ / **Gestures** _(en)_ / **Gestes** _(fr)_
- **Categoría:** Interaccion Mobile  · **Fase BDD:** `WHEN`  · **Display order:** `30`
- **Icono:** `gesture`  · **Keywords:** gesture, tap, long-press, swipe, pinch, zoom, flick, double-tap, scroll, touch
- **Descripción:** Tap, long press, swipe, pinch, zoom
- **Glue:** `com.qa.mobilecore.steps.interaction.GestureSteps`

## `mobile.element`

- **Display:** **Elementos Nativos** _(es)_ / **Native Elements** _(en)_ / **Elements natifs** _(fr)_
- **Categoría:** Interaccion Mobile  · **Fase BDD:** `WHEN`  · **Display order:** `40`
- **Icono:** `widgets`  · **Keywords:** native, element, interact, accessibility-id, xpath, class-name, id, ui-element, nativo
- **Descripción:** Interaccion con elementos de la UI nativa
- **Glue:** `com.qa.mobilecore.steps.interaction.NativeElementSteps`

## `mobile.context`

- **Display:** **Cambio de Contexto** _(es)_ / **Context Switch** _(en)_ / **Changement de contexte** _(fr)_
- **Categoría:** Interaccion Mobile  · **Fase BDD:** `WHEN`  · **Display order:** `50`
- **Icono:** `swap_horiz`  · **Keywords:** context, webview, native, switch, cambiar, webkit, hybrid, contexto-nativo, context-switch
- **Descripción:** Cambiar entre contexto nativo y WebView
- **Glue:** `com.qa.mobilecore.steps.interaction.ContextSwitchSteps`

## `mobile.notification`

- **Display:** **Notificaciones** _(es)_ / **Notifications** _(en)_ / **Notifications** _(fr)_
- **Categoría:** Interaccion Mobile  · **Fase BDD:** `WHEN`  · **Display order:** `70`
- **Icono:** `notifications`  · **Keywords:** notification, notificacion, push, alert, banner, dismiss, clear, system-notification, notif
- **Descripción:** Interaccion con notificaciones push y del sistema
- **Glue:** `com.qa.mobilecore.steps.device.NotificationSteps`

## `mobile.sensor`

- **Display:** **Sensores del Dispositivo** _(es)_ / **Device Sensors** _(en)_ / **Capteurs du dispositif** _(fr)_
- **Categoría:** Interaccion Mobile  · **Fase BDD:** `WHEN`  · **Display order:** `80`
- **Icono:** `settings_remote`  · **Keywords:** sensor, gps, location, accelerometer, gyroscope, coordinates, latitude, longitude, simulate, battery
- **Descripción:** Simular GPS, acelerometro, bateria
- **Glue:** `com.qa.mobilecore.steps.device.SensorSteps`

## `mobile.validation`

- **Display:** **Validacion de Elementos Mobile** _(es)_ / **Mobile Element Validation** _(en)_ / **Validation des elements mobiles** _(fr)_
- **Categoría:** Validacion Mobile  · **Fase BDD:** `THEN`  · **Display order:** `90`
- **Icono:** `check_circle`  · **Keywords:** element, elemento, visible, enabled, displayed, assert, check, validar, mobile-element, accessibility
- **Descripción:** Visibilidad, texto y estado de elementos nativos
- **Glue:** `com.qa.mobilecore.steps.validation.MobileElementValidationSteps`

## `mobile.validation.app-state`

- **Display:** **Estado de la Aplicacion** _(es)_ / **Application State** _(en)_ / **Etat de l'application** _(fr)_
- **Categoría:** Validacion Mobile  · **Fase BDD:** `THEN`  · **Display order:** `95`
- **Icono:** `mobile_friendly`  · **Keywords:** state, estado, foreground, background, running, terminated, app-state, lifecycle, sesion-activa
- **Descripción:** Valida el estado global de la app mobile: foreground/background, instalacion, orientacion del dispositivo y sesion activa
- **Glue:** `com.qa.mobilecore.steps.validation.AppStateValidationSteps`

---

> Para añadir un componente nuevo: implementar `com.qa.common.api.runtime.StepComponent`, anotar la clase con `@com.qa.common.api.runtime.annotation.StepId("<id>")`, registrarla en el plugin (`getComponents()`), y regenerar este documento.

> Para deprecar: marcar `@StepId(value=..., deprecated=true, replacedBy="<nuevo-id>")` y mantener la clase activa al menos un sprint para permitir migración FE.
