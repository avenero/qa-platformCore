# ANÁLISIS DE FEATURES - SOLICITUD EVAUT
## Fecha: 2026-02-25
---
## 1. STEPS FALTANTES
### 1.1 WebSteps — Pendiente implementar
| Step faltante | Escenario | Prioridad |
|---|---|---|
| `Dado que navego a la pantalla de bienvenida` | Todos los escenarios como precondición | ALTA |
| `el diseño de {string} debe coincidir con Figma` | EVAUT-98, 104, 105 | MEDIA |
| `valido que la variable {string} sea igual a la variable {string}` | EVAUT-CROSS-003 | MEDIA |
| `el mensaje {string} debe contener el texto de la variable {string}` | EVAUT-CROSS-004 | MEDIA |
| `print {string}` (loguear variable de ScenarioContext) | EVAUT-CROSS-005 debug | BAJA |
NOTAS:
- "el valor del campo {string} debe ser {string}" (EVAUT-111, 114) es DUPLICADO de
  "verifico que el texto en {string} sea {string}" ya existente. NO crear nuevo step.
- "Dado que navego a la pantalla de bienvenida" puede implementarse leyendo
  config.getProperty("web.url.bienvenida") y navegando con driver.navigate().to().
- "el diseño de {string} debe coincidir con Figma" requiere integración visual
  (Percy/Applitools). Por ahora puede quedarse como @PendingException.
### 1.2 ApiSteps — Pendiente implementar
| Step faltante | Escenario | Prioridad |
|---|---|---|
| Soporte {{variableName}} en `agrego el queryparam` | EVAUT-CROSS-001 | ALTA |
NOTA: DataUtilities.replaceVariables no resuelve variables guardadas con
"guardo texto del elemento". Debe agregarse soporte al patron {{var}} para
recuperar valores del ScenarioContext.
### 1.3 Steps de negocio (van en qa-module-autos, NO en el core)
- `el parámetro "INGRESO_MINIMO" debe estar configurado` — conoce nombre de negocio
- `el valor debe ser obtenido dinámicamente, no hardcoded` — validación de negocio
- Texto exacto del tooltip de PEP (EVAUT-151) — texto específico de Figma/negocio
- `Dado que el usuario ha iniciado sesión exitosamente`
- `Dado que el usuario ha completado sus datos personales`
- `Dado que navego a la sección {string}` (con lógica de navegación de negocio)
---
## 2. STEPS DUPLICADOS / REDUNDANTES (analizar para próxima iteración)
### 2.1 Visibilidad de elementos — DUPLICADOS
| Step A (estilo viejo) | Step B (estilo nuevo) | Acción |
|---|---|---|
| `verifico si existe el elemento {string}` | `el elemento {string} debe ser visible` | Deprecar A |
| `verifico que no exista el elemento {string}` | `el elemento {string} no debe ser visible` | Deprecar A |
### 2.2 Validación de texto — CASI DUPLICADOS
| Step A | Step B | Acción |
|---|---|---|
| `verifico que el texto en {string} sea {string}` | `verifico si existe el elemento {string} y valido que el texto sea {string}` | B es más robusto (tiene wait). Deprecar A |
| `el mensaje {string} debe contener el texto {string}` | `verifico que el texto en {string} contenga el texto {string}` | MISMA LÓGICA, nombres distintos. Unificar |
### 2.3 Estado habilitado/deshabilitado — DUPLICADOS
| Step A | Step B | Acción |
|---|---|---|
| `verifico que el elemento {string} este habilitado` | `el campo {string} debe estar habilitado` | MISMA LÓGICA. Estandarizar en `el elemento {string} debe estar habilitado` |
| `el botón {string} debe estar activo` | `el campo {string} debe estar habilitado` | Ambos llaman `helper.validateButtonIsEnabled()`. Unificar |
| `verifico que el elemento {string} este desactivado` | `verifico que el elemento {string} este deshabilitado` | usan isActive() vs isDisabled(). Revisar si la diferencia es intencional |
### 2.4 Selección en combobox — DUPLICADOS
| Step A | Step B | Acción |
|---|---|---|
| `selecciono el valor {string} en el combobox {string}` | `selecciono la opcion con el valor {string} en el combobox {string}` | DUPLICADOS — mismo helper. Deprecar B |
| `verifico si existe el combobox {string} y selecciono el valor {string}` | `selecciono el valor {string} en el combobox {string}` | REDUNDANTE. El primero agrega wait. Agregar wait al segundo y deprecar el primero |
### 2.5 Autenticación API — DUPLICADOS
| Step A (nuevo) | Step B (viejo) | Acción |
|---|---|---|
| `agrego autenticación Client Credentials` | `agrego el token requerido del tipo Client-Credentials` | DUPLICADOS. Deprecar B |
| `agrego autenticación Bearer para RUT {word}` | `agrego el token requerido del tipo Bearer-Token para el rut {string}` | DUPLICADOS. Deprecar B |
### 2.6 Configuración de endpoint — DUPLICADOS
| Step A | Step B | Acción |
|---|---|---|
| `configuro endpoint con base {string} y path {string}` | `el host {string} mas el contexto {string}` | MISMA LÓGICA. Deprecar B (semántica más antigua) |
---
## 3. ESCENARIOS CROSS-PLATFORM SUGERIDOS
Implementados en solicitud.feature:
- EVAUT-CROSS-001: UI → API (formulario enviado llega al backend)
- EVAUT-CROSS-002: UI → BD (solicitud se persiste en base de datos)
- EVAUT-CROSS-003: API ↔ BD (parámetro INGRESO_MINIMO consistente entre ambos)
Sugeridos pero NO implementados aún (requieren steps faltantes):
EVAUT-CROSS-004: Consistencia mensaje error UI vs API
- Objetivo: el mensaje de error mostrado en UI debe ser EXACTAMENTE el mismo que
  retorna el API en el campo "message" del error.
- Step faltante: "el mensaje {string} debe contener el texto de la variable {string}"
EVAUT-CROSS-005: OTP generado por API se registra en BD
- Objetivo: luego de llamar al endpoint de envío de OTP, verificar en BD que
  se creó el registro correspondiente.
- Step faltante: "print {string}" para debug, aunque no es bloqueante.
- Bloqueo real: acceso a BD sqlserver del ambiente QA con Windows Authentication.
---
## 4. TABLA RESUMEN: STEPS NUEVOS A IMPLEMENTAR EN CORE
| # | Step | Capa | Prioridad |
|---|---|---|---|
| 1 | `Dado que navego a la pantalla de bienvenida` | WebSteps | ALTA |
| 2 | Soporte {{variableName}} en agrego el queryparam | ApiSteps/DataUtilities | ALTA |
| 3 | `valido que la variable {string} sea igual a la variable {string}` | WebSteps | MEDIA |
| 4 | `el mensaje {string} debe contener el texto de la variable {string}` | WebSteps | MEDIA |
| 5 | `print {string}` | WebSteps o común | BAJA |
| 6 | `el diseño de {string} debe coincidir con Figma` | WebSteps (integración visual) | BAJA |
