# http-core — Component Catalog

> **Auto-generado** — TASK-J03. NO editar manualmente.
> Regenerar con `./gradlew :http-core:test --tests "*ComponentCatalogTest"`.
>
> **Plugin:** `com.qa.httpcore.plugin.ApiPlugin`  
> **Platform ID:** `HTTP`  
> **Display:** HTTP API Testing  
> **Componentes:** 12  
> **Última generación:** 2026-05-11

Esta tabla es el **contrato público** de los `StepComponent` que el módulo `http-core` expone al Backend (catálogo i18n) y al Frontend (paleta del Scenario Builder). Cada entrada se deriva por reflexión vía SPI (`ServiceLoader<CorePlugin>`).

**Convenciones**

- `@StepId`: identificador estable. Cambiar un id es **breaking** (rompe escenarios persistidos en BD del BE).
- Fase BDD: `GIVEN | WHEN | THEN | ANY`.
- Keywords: enriquecen el `ScenarioSuggestionEngine` (TASK-C06).
- Locale: si una clave no aparece en `displayNameByLocale`, el FE cae a `displayName` (ES).

---

## `api.url`

- **Display:** **URL / Ambiente** _(es)_ / **URL / Environment** _(en)_ / **URL / Environnement** _(fr)_
- **Categoría:** Configuracion de Peticion  · **Fase BDD:** `GIVEN`  · **Display order:** `10`
- **Icono:** `link`  · **Keywords:** url, endpoint, base-url, ambiente, environment, entorno, protocol, https, host, route, path
- **Descripción:** Configuracion de base URL, ambiente y protocolo
- **Glue:** `com.qa.httpcore.steps.config.UrlConfigSteps`

## `api.authentication`

- **Display:** **Autenticacion** _(es)_ / **Authentication** _(en)_ / **Authentification** _(fr)_
- **Categoría:** Configuracion de Peticion  · **Fase BDD:** `GIVEN`  · **Display order:** `20`
- **Icono:** `lock`  · **Keywords:** auth, authentication, autenticacion, authentification, token, bearer, jwt, oauth, oauth2, login, credentials, credenciales, apikey, session
- **Descripción:** Bearer Token, Basic Auth, API Key, OAuth 2.0, JWT
- **Glue:** `com.qa.httpcore.steps.config.AuthenticationSteps`

## `api.headers`

- **Display:** **Headers** _(es)_ / **Headers** _(en)_ / **En-tetes HTTP** _(fr)_
- **Categoría:** Configuracion de Peticion  · **Fase BDD:** `GIVEN`  · **Display order:** `30`
- **Icono:** `view_list`  · **Keywords:** header, headers, cabecera, en-tete, content-type, accept, authorization, custom-header, x-header, http-header
- **Descripción:** Gestion de cabeceras HTTP de la peticion
- **Glue:** `com.qa.httpcore.steps.config.HeaderSteps`

## `api.cookies`

- **Display:** **Cookies** _(es)_ / **Cookies** _(en)_ / **Cookies** _(fr)_
- **Categoría:** Configuracion de Peticion  · **Fase BDD:** `GIVEN`  · **Display order:** `40`
- **Icono:** `cookie`  · **Keywords:** cookie, cookies, galleta, session, set-cookie, http-only, secure-cookie, jar, domain, path
- **Descripción:** Gestion de cookies en la peticion HTTP
- **Glue:** `com.qa.httpcore.steps.config.CookieSteps`

## `api.parameters`

- **Display:** **Query Parameters** _(es)_ / **Query Parameters** _(en)_ / **Parametres de requete** _(fr)_
- **Categoría:** Configuracion de Peticion  · **Fase BDD:** `GIVEN`  · **Display order:** `50`
- **Icono:** `search`  · **Keywords:** param, parameter, parametro, query, querystring, path-param, path-variable, url-param, variable, placeholder
- **Descripción:** Parametros de URL y path de la peticion
- **Glue:** `com.qa.httpcore.steps.config.ParameterSteps`

## `api.body`

> ⚠️ **Deprecado** — usar `api.request.body` en su lugar.

- **Display:** **Request Body** _(es)_ / **Request Body** _(en)_ / **Corps de la requete** _(fr)_
- **Categoría:** Configuracion de Peticion  · **Fase BDD:** `GIVEN`  · **Display order:** `60`
- **Icono:** `description`  · **Keywords:** body, cuerpo, payload, json, xml, form-data, multipart, template, raw, request-body
- **Descripción:** Cuerpo de la peticion: JSON, XML, form-data, template
- **Glue:** `com.qa.httpcore.steps.config.RequestBodySteps`

## `api.execution`

- **Display:** **Ejecucion HTTP** _(es)_ / **HTTP Execution** _(en)_ / **Execution HTTP** _(fr)_
- **Categoría:** Ejecucion  · **Fase BDD:** `WHEN`  · **Display order:** `70`
- **Icono:** `send`  · **Keywords:** request, peticion, requete, send, enviar, invoke, get, post, put, delete, patch, http, rest, call
- **Descripción:** Envio de peticiones GET, POST, PUT, DELETE, PATCH
- **Glue:** `com.qa.httpcore.steps.execution.HttpExecutionSteps`

## `api.status`

- **Display:** **Status Code** _(es)_ / **Status Code** _(en)_ / **Code de statut** _(fr)_
- **Categoría:** Validacion de Respuesta  · **Fase BDD:** `THEN`  · **Display order:** `80`
- **Icono:** `check_circle`  · **Keywords:** status, code, codigo, statut, http-code, response-code, 200, 201, 400, 401, 403, 404, 500
- **Descripción:** Validacion del codigo de estado HTTP
- **Glue:** `com.qa.httpcore.steps.validation.StatusCodeSteps`

## `api.response.body`

- **Display:** **Response Body** _(es)_ / **Response Body** _(en)_ / **Corps de la reponse** _(fr)_
- **Categoría:** Validacion de Respuesta  · **Fase BDD:** `THEN`  · **Display order:** `90`
- **Icono:** `data_object`  · **Keywords:** response, respuesta, reponse, body, cuerpo, json, jsonpath, xpath, field, campo, extract, extraccion, assert, validar, check
- **Descripción:** Validacion y extraccion del cuerpo de respuesta JSON
- **Glue:** `com.qa.httpcore.steps.validation.ResponseBodySteps`

## `api.response.headers`

- **Display:** **Response Headers** _(es)_ / **Response Headers** _(en)_ / **En-tetes de reponse** _(fr)_
- **Categoría:** Validacion de Respuesta  · **Fase BDD:** `THEN`  · **Display order:** `100`
- **Icono:** `receipt_long`  · **Keywords:** response-header, cabecera-respuesta, content-type, location, etag, cache-control, x-header, header-validation, validar-cabecera
- **Descripción:** Validacion de cabeceras de respuesta
- **Glue:** `com.qa.httpcore.steps.validation.ResponseHeaderSteps`

## `api.performance`

- **Display:** **Performance** _(es)_ / **Performance** _(en)_ / **Performance** _(fr)_
- **Categoría:** Validacion de Respuesta  · **Fase BDD:** `THEN`  · **Display order:** `110`
- **Icono:** `speed`  · **Keywords:** performance, rendimiento, latencia, latency, response-time, tiempo-respuesta, throughput, tps, sla, timeout, body-size, tamano
- **Descripción:** Validacion de tiempo de respuesta y tamano del body
- **Glue:** `com.qa.httpcore.steps.validation.ResponsePerformanceSteps`

## `api.security`

- **Display:** **Seguridad** _(es)_ / **Security** _(en)_ / **Securite** _(fr)_
- **Categoría:** Validacion de Respuesta  · **Fase BDD:** `THEN`  · **Display order:** `120`
- **Icono:** `security`  · **Keywords:** security, seguridad, securite, cors, xss, injection, ssl, tls, hsts, csp, x-frame-options, vulnerability, vulnerabilidad
- **Descripción:** Validacion de controles de seguridad HTTP
- **Glue:** `com.qa.httpcore.steps.validation.ResponseSecuritySteps`

---

> Para añadir un componente nuevo: implementar `com.qa.common.api.runtime.StepComponent`, anotar la clase con `@com.qa.common.api.runtime.annotation.StepId("<id>")`, registrarla en el plugin (`getComponents()`), y regenerar este documento.

> Para deprecar: marcar `@StepId(value=..., deprecated=true, replacedBy="<nuevo-id>")` y mantener la clase activa al menos un sprint para permitir migración FE.
