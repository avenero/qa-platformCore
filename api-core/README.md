# 🌐 api-core — Capa de Pruebas de API REST

> **Versión:** 2.0.0 | **Grupo:** `com.qa` | **Artefacto:** `api-core`  
> **Última actualización:** Abril 2026  
> **Autor:** Abel Venero  
> **Para quién es este manual:** Para cualquier persona que quiera entender, mantener o extender esta capa — desde un desarrollador experimentado hasta alguien sin conocimientos técnicos que necesita entender qué hace cada pieza.

---

## 📖 Índice

1. [¿Qué es api-core en palabras simples?](#1-qué-es-api-core-en-palabras-simples)
2. [Conceptos Clave Antes de Empezar](#2-conceptos-clave-antes-de-empezar)
3. [El Lugar de api-core en la Plataforma](#3-el-lugar-de-api-core-en-la-plataforma)
4. [Mapa Completo del Módulo](#4-mapa-completo-del-módulo)
5. [Las 7 Zonas de la Arquitectura](#5-las-7-zonas-de-la-arquitectura)
   - 5.1 [La Puerta de Entrada — `plugin/`](#51-la-puerta-de-entrada--plugin)
   - 5.2 [El Catálogo de Capacidades — `components/`](#52-el-catálogo-de-capacidades--components)
   - 5.3 [Los Pasos de Prueba — `steps/`](#53-los-pasos-de-prueba--steps)
   - 5.4 [Los Contratos — `interfaces/`](#54-los-contratos--interfaces)
   - 5.5 [Las Máquinas Reales — `implementations/`](#55-las-máquinas-reales--implementations)
   - 5.6 [Las Fábricas — `factories/`](#56-las-fábricas--factories)
   - 5.7 [Las Herramientas — `utils/`](#57-las-herramientas--utils)
6. [Los 12 Componentes de Steps API](#6-los-12-componentes-de-steps-api)
7. [Catálogo Completo de Steps BDD](#7-catálogo-completo-de-steps-bdd)
8. [Flujo Completo de una Prueba API](#8-flujo-completo-de-una-prueba-api)
9. [Relaciones Entre Clases](#9-relaciones-entre-clases)
10. [Patrones de Diseño Usados](#10-patrones-de-diseño-usados)
11. [Dependencias del Módulo](#11-dependencias-del-módulo)
12. [Estado Actual y Pendientes](#12-estado-actual-y-pendientes)

---

## 1. ¿Qué es api-core en palabras simples?

Imagina que tienes un **asistente de pruebas** cuyo único trabajo es verificar que los servicios web de Scotiabank funcionan correctamente. Este asistente sabe cómo:

- **Armar una solicitud**: "Voy a conectarme al endpoint `/api/transferencias`, con mi token de seguridad, y voy a enviar estos datos en el cuerpo del mensaje."
- **Ejecutar la solicitud**: "Ahora envío la solicitud con el método POST."
- **Validar la respuesta**: "El servicio me respondió 200 (éxito), el JSON tiene el campo `transactionId` no vacío, y respondió en menos de 2 segundos."

Todo eso — armar, ejecutar y validar — es lo que hace **api-core**. Pero en vez de hacerlo manualmente cada vez, lo hace siguiendo **instrucciones escritas en español** que parecen lenguaje natural:

```gherkin
Given configuro el endpoint "api.transferencias.url"
  And agrego autenticación Bearer para RUT 12345678
  And establezco el cuerpo de la petición como
    """
    {"monto": 50000, "cuentaDestino": "123456789"}
    """
When ejecuto una petición "POST"
Then valido que el codigo de respuesta del servicio sea 200
  And valido que el campo "$.transactionId" NO sea null
  And valido que el tiempo de respuesta sea menor a 2000 milisegundos
```

Esas instrucciones en español se llaman **steps BDD** y son el corazón de api-core.

---

## 2. Conceptos Clave Antes de Empezar

### 🌐 ¿Qué es una API?

Una API (Application Programming Interface) es como una **ventanilla de banco**: tú haces una solicitud con ciertos datos, y la ventanilla te devuelve una respuesta. En el mundo digital, esa ventanilla es un servicio web al que le envías mensajes por internet (HTTP) y te responde con datos (generalmente en formato JSON).

**Ejemplo real:**
- Tú envías: `GET /api/cuentas/12345` (preguntando por la cuenta 12345)
- El servicio responde: `{"saldo": 1500000, "titular": "Juan Pérez"}`

### 📋 ¿Qué es BDD?

BDD (Behavior-Driven Development) es una forma de escribir pruebas usando **lenguaje natural** que cualquier persona puede leer y entender, no solo programadores. Usa las palabras clave:

- **Given** (Dado que): la condición inicial o configuración
- **When** (Cuando): la acción que se ejecuta
- **Then** (Entonces): el resultado esperado que validamos

### 🥒 ¿Qué es Cucumber?

Cucumber es el **motor** que lee esos pasos en español y los ejecuta. Lee archivos `.feature` que contienen los escenarios BDD y busca en el código Java qué función corresponde a cada frase.

### 🧩 ¿Qué es un "step"?

Un step es la conexión entre una **frase en español** (como `Given configuro el endpoint "url"`) y el **código Java** que realmente hace esa acción. Cada frase del archivo `.feature` tiene un método Java anotado with `@Given`, `@When` o `@Then` que la ejecuta.

---

## 3. El Lugar de api-core en la Plataforma

La plataforma completa tiene tres sistemas: el **Frontend** (interfaz web para los QA), el **Backend** (servidor que orquesta todo) y el **Core** (motor de ejecución de pruebas). api-core es un módulo dentro del Core:

```
┌─────────────────────────────────────────────────────────────────┐
│                    qa-frameworks-core (el Core)                  │
│                                                                   │
│  ┌────────────┐   ┌────────────┐   ┌────────────┐  ┌─────────┐  │
│  │   common   │   │  api-core  │   │  web-core  │  │mob-core │  │
│  │            │   │            │   │            │  │         │  │
│  │ • Runtime  │◄──│ • ApiPlugin│   │ • WebPlugin│  │• Mobile │  │
│  │ • Config   │   │ • Steps    │   │ • Steps    │  │  Plugin │  │
│  │ • HTTP     │   │ • HttpClient│  │ • WebDriver│  │• Steps  │  │
│  │ • Logging  │   │ • Auth     │   │ • Selenium │  │• Appium │  │
│  │ • Reporting│   │ • Validar  │   │            │  │         │  │
│  └────────────┘   └────────────┘   └────────────┘  └─────────┘  │
│         ▲                                                         │
│         │ usa como base                                           │
└─────────────────────────────────────────────────────────────────┘
         ▲
         │ importa como librería Java
┌─────────────────┐
│ Backend (Spring)│ ← invoca engine.execute(request)
└─────────────────┘
```

**api-core aporta al sistema:**
- Los steps BDD para probar servicios HTTP/REST (~92 steps en español, 13 clases)
- El cliente HTTP que hace las peticiones reales al servidor
- El servicio de autenticación (Bearer, Basic, OAuth2, JWT, API Key)
- Las validaciones de respuesta (status code, cuerpo JSON, headers, performance, seguridad)
- El `ApiPlugin` que se registra automáticamente en el motor de ejecución

**api-core NO hace:**
- No ejecuta pruebas por sí solo (eso lo hace el `CucumberRuntimeEngine` de `common`)
- No guarda resultados en base de datos (eso lo hace el Backend)
- No integra con Jira (eso lo hace el Backend)
- No tiene interfaz gráfica (eso lo hace el Frontend)
- No prueba navegadores web (eso lo hace `web-core`)

---

## 4. Mapa Completo del Módulo

```
api-core/
└── src/
    └── main/
        ├── java/com/qa/apicore/
        │   │
        │   ├── plugin/                          ← ENTRADA PRINCIPAL
        │   │   └── ApiPlugin.java               ← Registra servicios + declara componentes
        │   │
        │   ├── components/                      ← CATÁLOGO DE CAPACIDADES (metadatos)
        │   │   ├── ApiUrlComponent.java          ← Descriptor: URL y Ambiente
        │   │   ├── ApiAuthComponent.java         ← Descriptor: Autenticación
        │   │   ├── ApiHeaderComponent.java       ← Descriptor: Headers de petición
        │   │   ├── ApiCookieComponent.java       ← Descriptor: Cookies
        │   │   ├── ApiParameterComponent.java    ← Descriptor: Query/Path params
        │   │   ├── ApiRequestBodyComponent.java  ← Descriptor: Body de la petición
        │   │   ├── ApiExecutionComponent.java    ← Descriptor: Ejecución HTTP
        │   │   ├── ApiStatusCodeComponent.java   ← Descriptor: Validación status code
        │   │   ├── ApiResponseBodyComponent.java ← Descriptor: Validación cuerpo respuesta
        │   │   ├── ApiResponseHeaderComponent.java ← Descriptor: Validación headers respuesta
        │   │   ├── ApiPerformanceComponent.java  ← Descriptor: Validación performance
        │   │   └── ApiSecurityComponent.java     ← Descriptor: Validación seguridad
        │   │
        │   ├── steps/                           ← LOS PASOS DE PRUEBA (steps BDD)
        │   │   ├── VariableSteps.java            ← Steps transversales: variables
        │   │   ├── config/                       ← GIVEN: pasos de configuración
        │   │   │   ├── UrlConfigSteps.java        ← Configura endpoint/host/ambiente
        │   │   │   ├── AuthenticationSteps.java   ← Configura autenticación
        │   │   │   ├── HeaderSteps.java           ← Configura headers HTTP
        │   │   │   ├── CookieSteps.java           ← Configura cookies
        │   │   │   ├── ParameterSteps.java        ← Configura query/path params
        │   │   │   └── RequestBodySteps.java      ← Configura el body del request
        │   │   ├── execution/                    ← WHEN: pasos de ejecución
        │   │   │   └── HttpExecutionSteps.java    ← Ejecuta la petición HTTP
        │   │   └── validation/                   ← THEN: pasos de validación
        │   │       ├── StatusCodeSteps.java       ← Valida código HTTP (200, 404, etc.)
        │   │       ├── ResponseBodySteps.java     ← Valida el cuerpo de la respuesta
        │   │       ├── ResponseHeaderSteps.java   ← Valida headers de la respuesta
        │   │       ├── ResponsePerformanceSteps.java ← Valida tiempo y tamaño
        │   │       └── ResponseSecuritySteps.java ← Valida controles de seguridad
        │   │
        │   ├── interfaces/                      ← LOS CONTRATOS (qué pueden hacer)
        │   │   ├── HttpClient.java               ← Contrato del cliente HTTP
        │   │   ├── AuthenticationService.java    ← Contrato del servicio de auth
        │   │   └── DatabaseService.java          ← Contrato del servicio de BD (API tests)
        │   │
        │   ├── implementations/                 ← LAS MÁQUINAS REALES
        │   │   ├── BaseHttpClient.java           ← Implementación del cliente HTTP (Unirest)
        │   │   ├── BaseAuthenticationManager.java ← Implementación de autenticación
        │   │   ├── BaseDatabaseConfiguration.java ← Config base de BD para API tests
        │   │   └── BaseDatabaseService.java      ← Servicio base de BD para API tests
        │   │
        │   ├── factories/                       ← LAS FÁBRICAS (cómo se crean)
        │   │   ├── HttpClientFactory.java        ← Crea instancias de HttpClient
        │   │   ├── AuthenticationServiceFactory.java ← Crea el servicio de auth
        │   │   └── DatabaseServiceFactory.java   ← Crea el servicio de BD
        │   │
        │   └── utils/                           ← LAS HERRAMIENTAS
        │       ├── ApiHelper.java                ← Fachada: conecta steps con servicios
        │       ├── ValidationUtilities.java      ← Utilidades de validación puras
        │       └── DatabaseTestUtilities.java    ← Utilidades de BD para tests API
        │
        └── resources/
            ├── logback.xml                       ← Configuración de logs
            └── META-INF/services/
                └── com.qa.common.runtime.CorePlugin  ← SPI: registra ApiPlugin
```

---

## 5. Las 7 Zonas de la Arquitectura

### 5.1 La Puerta de Entrada — `plugin/`

#### `ApiPlugin.java`

**¿Qué es?** Es la **puerta de entrada oficial** de toda la capa api-core al motor de ejecución. Es lo primero que el motor activa cuando encuentra un escenario con los tags `@api`, `@rest`, `@http` o `@service`.

**¿Qué hace?**
1. **Registra los servicios** que los steps van a necesitar: crea el `HttpClient`, el `AuthenticationService` y el `ApiHelper`, pero de forma *lazy* (solo se crean cuando alguien los usa, no antes).
2. **Declara los 12 componentes** de steps que api-core aporta, con sus metadatos (nombre, fase BDD, categoría, icono).
3. **Gestiona el ciclo de vida** de cada escenario: al inicio limpia el estado HTTP, al final lo reinicia.
4. **Se auto-registra** en el motor sin que nadie tenga que escribir código extra — gracias al archivo SPI.

**El archivo SPI** (`META-INF/services/com.qa.common.runtime.CorePlugin`) contiene una sola línea:
```
com.qa.apicore.plugin.ApiPlugin
```
Eso le dice a Java: "cuando alguien busque plugins de tipo `CorePlugin`, incluye este".

**¿Cuándo se activa?** Solo cuando el escenario tiene uno de estos tags:
```gherkin
@api        ← prueba genérica de API
@rest       ← prueba de servicio REST
@http       ← prueba de comunicación HTTP
@service    ← prueba de microservicio
```

**Estructura del plugin:**
```
ApiPlugin
├── getName()              → "api"
├── getActivationTags()    → {"@api", "@rest", "@http", "@service"}
├── getOrder()             → 50 (prioridad de inicialización)
├── registerServices()     → registra HttpClient, AuthService, ApiHelper
├── onScenarioStart()      → limpia estado HTTP del HttpClient
├── onScenarioEnd()        → resetea el cliente HTTP
└── getComponents()        → retorna los 12 ApiXxxComponent
```

---

### 5.2 El Catálogo de Capacidades — `components/`

Hay 12 clases en esta carpeta, una por cada **componente** de api-core. Cada clase es muy pequeña (~24 líneas) y solo contiene **metadatos descriptivos** sobre un grupo de steps.

**¿Para qué sirven?** Son la **ficha técnica** de cada grupo de steps. El Backend usa estas fichas para exponer el catálogo al Frontend. El Frontend los muestra en la paleta visual de drag-and-drop para construir escenarios.

**Información que describe cada componente:**

| Campo | Tipo | Ejemplo para `ApiAuthComponent` |
|-------|------|---------|
| `id` | identificador único | `"api.authentication"` |
| `displayName` | nombre legible | `"Autenticación"` |
| `description` | descripción | `"Bearer Token, Basic Auth, API Key, OAuth 2.0, JWT"` |
| `phase` | fase BDD | `BddPhase.GIVEN` |
| `category` | categoría visual | `"Configuración de Petición"` |
| `icon` | icono de la UI | `"lock"` |
| `displayOrder` | orden en la paleta | `20` |
| `stepDefinitionClass` | clase Java que tiene los steps | `AuthenticationSteps.class` |

**Lista de los 12 componentes:**

| Clase Java | ID | Fase | Categoría | Clase de Steps |
|-----------|-----|------|-----------|---------------|
| `ApiUrlComponent` | `api.url` | GIVEN | Config. Petición | `UrlConfigSteps` |
| `ApiAuthComponent` | `api.authentication` | GIVEN | Config. Petición | `AuthenticationSteps` |
| `ApiHeaderComponent` | `api.headers` | GIVEN | Config. Petición | `HeaderSteps` |
| `ApiCookieComponent` | `api.cookies` | GIVEN | Config. Petición | `CookieSteps` |
| `ApiParameterComponent` | `api.parameters` | GIVEN | Config. Petición | `ParameterSteps` |
| `ApiRequestBodyComponent` | `api.request.body` | GIVEN | Config. Petición | `RequestBodySteps` |
| `ApiExecutionComponent` | `api.execution` | WHEN | Ejecución HTTP | `HttpExecutionSteps` |
| `ApiStatusCodeComponent` | `api.status.code` | THEN | Validación | `StatusCodeSteps` |
| `ApiResponseBodyComponent` | `api.response.body` | THEN | Validación | `ResponseBodySteps` |
| `ApiResponseHeaderComponent` | `api.response.headers` | THEN | Validación | `ResponseHeaderSteps` |
| `ApiPerformanceComponent` | `api.performance` | THEN | Validación | `ResponsePerformanceSteps` |
| `ApiSecurityComponent` | `api.security` | THEN | Validación | `ResponseSecuritySteps` |

> **Nota:** `VariableSteps.java` es un step transversal no asociado a un componente específico — gestiona variables entre steps de cualquier tipo.

---

### 5.3 Los Pasos de Prueba — `steps/`

Esta es la carpeta más importante de api-core. Aquí viven los **13 archivos de steps**, organizados siguiendo las fases BDD:

```
steps/
├── VariableSteps.java        ← transversal: gestión de variables
├── config/                   ← GIVEN: configurar la petición
│   ├── UrlConfigSteps.java
│   ├── AuthenticationSteps.java
│   ├── HeaderSteps.java
│   ├── CookieSteps.java
│   ├── ParameterSteps.java
│   └── RequestBodySteps.java
├── execution/                ← WHEN: ejecutar la petición
│   └── HttpExecutionSteps.java
└── validation/               ← THEN: validar la respuesta
    ├── StatusCodeSteps.java
    ├── ResponseBodySteps.java
    ├── ResponseHeaderSteps.java
    ├── ResponsePerformanceSteps.java
    └── ResponseSecuritySteps.java
```

**¿Por qué esta separación?**

Antes existía una sola clase `ApiSteps.java` con 478 líneas que mezclaba absolutamente todo. Era como una bodega desordenada donde el martillo, el bisturí y el volante estaban en el mismo cajón. Ahora cada clase tiene **una sola responsabilidad** (principio SOLID), lo que significa:
- Es fácil encontrar un step específico
- Es fácil agregar nuevos steps sin tocar los existentes
- Es fácil entender qué hace cada clase con solo leer su nombre

**Descripción de cada archivo:**

#### `VariableSteps.java`
Pasos para gestionar variables que se pasan entre steps dentro de un escenario. Por ejemplo, guardar el ID de una transacción creada en el paso `When` para verificarlo en el paso `Then`.

#### `config/UrlConfigSteps.java`
Pasos para configurar el **destino** de la petición HTTP. Establece la URL base, el endpoint específico, el ambiente de pruebas (qa, staging, prod) o el protocolo (http/https).

#### `config/AuthenticationSteps.java`
Pasos para configurar **quién dice ser** la petición — la identidad. Soporta múltiples mecanismos de seguridad: Bearer Token (JWT), Basic Authentication (usuario+contraseña codificados en Base64), Client Credentials OAuth2, API Key en header o query param, JWT con claims personalizados, y tokens especiales para pruebas negativas de seguridad (expirados, inválidos).

#### `config/HeaderSteps.java`
Pasos para agregar **metadatos** a la petición HTTP. El Content-Type le dice al servidor qué formato estás enviando (JSON, XML...), el Accept le dice qué formato esperas recibir, la versión de API le dice qué versión del servicio usar. También permite agregar cualquier header personalizado o eliminar uno.

#### `config/CookieSteps.java`
Pasos para gestionar **cookies** en la petición. Se usan principalmente en flujos que requieren sesión persistente. También incluye casos especiales para pruebas de seguridad (cookies expiradas).

#### `config/ParameterSteps.java`
Pasos para agregar **parámetros** a la URL. Los query params van después del `?` en la URL (ej: `/api/cuentas?pagina=2`), y los path params reemplazan marcadores de posición en la URL (ej: `/api/cuentas/{cuentaId}` → `/api/cuentas/12345`).

#### `config/RequestBodySteps.java`
Pasos para definir **qué datos se envían** en el cuerpo (body) de la petición. Soporta múltiples formatos: JSON como string directo, JSON construido campo a campo, XML, form-data multipart, lectura desde archivo externo, y templates con variables dinámicas.

#### `execution/HttpExecutionSteps.java`
Pasos para **disparar la petición**. El único paso de fase WHEN. Soporta todos los métodos HTTP: GET (consultar), POST (crear), PUT (actualizar completo), PATCH (actualizar parcial), DELETE (eliminar). También permite atajos directos que combinan configuración y ejecución en un solo paso, y control de timeouts.

#### `validation/StatusCodeSteps.java`
Pasos para validar el **código HTTP de respuesta** (el número que indica si la petición fue exitosa o falló). Valida un código exacto, una familia entera (todos los 200, todos los 400, todos los 500), un rango personalizado, o que el código NOT sea un valor específico.

#### `validation/ResponseBodySteps.java`
Pasos para validar el **contenido del cuerpo** de la respuesta JSON. Es el componente más rico: valida texto libre, esquemas JSON completos, valores en campos específicos usando JSONPath, tipos de datos, tamaños de arrays, patrones regex, y permite extraer y guardar valores para usarlos en pasos posteriores.

#### `validation/ResponseHeaderSteps.java`
Pasos para validar los **headers de la respuesta** HTTP. Verifica valores exactos o parciales de headers, y permite extraer un header y guardarlo como variable para usarlo en pasos posteriores.

#### `validation/ResponsePerformanceSteps.java`
Pasos para validar el **rendimiento** de la respuesta. Verifica que el tiempo total de respuesta no exceda un umbral dado (en milisegundos o segundos), y que el tamaño del cuerpo no exceda un límite en kilobytes.

#### `validation/ResponseSecuritySteps.java`
Pasos para validar **controles básicos de seguridad** en la respuesta. Verifica el uso de HTTPS, la ausencia de headers que revelan información del servidor, la presencia de headers de seguridad estándar (X-Content-Type-Options, X-Frame-Options), la ausencia de stack traces Java en la respuesta, y que el servidor rechace correctamente intentos de SQL Injection y XSS.

---

### 5.4 Los Contratos — `interfaces/`

Las interfaces definen **qué puede hacer** un componente, sin especificar cómo lo hace. Es como el plano de un edificio: dice qué habitaciones debe tener, pero no cómo se construye cada una.

#### `HttpClient.java` — El contrato del cliente HTTP

Es el contrato más importante de api-core. Define todas las operaciones que cualquier cliente HTTP debe poder realizar:

**Configuración (antes de enviar):**
- `setHost(String host)` — establece la URL base
- `addHeader(String key, String value)` — agrega un header
- `addHeaders(Map<String,String> headers)` — agrega múltiples headers
- `addQueryParam(String key, String value)` — agrega query param
- `addPathParam(String key, String value)` — reemplaza path param
- `setBody(String body)` — establece el body
- `addField(String key, String value)` — agrega un campo al body
- `setTimeout(int ms)` — establece timeout en milisegundos
- `removeHeader(String key)` — elimina un header
- `clearQueryParams()` — limpia todos los query params

**Ejecución (enviar):**
- `get(String endpoint)` — ejecuta GET
- `post(String endpoint)` — ejecuta POST
- `put(String endpoint)` — ejecuta PUT
- `patch(String endpoint)` — ejecuta PATCH
- `delete(String endpoint)` — ejecuta DELETE

**Consulta (después de enviar):**
- `getLastResponse()` — obtiene la última respuesta recibida
- `getHost()` — retorna el host configurado
- `getLastRequestUrl()` — retorna la URL completa de la última petición
- `hasValidHost()` — verifica si hay host configurado

**Limpieza:**
- `clearRequestData()` — limpia headers, params y body
- `reset()` — reseteo completo del estado

> **¿Por qué existe la interface?** Porque si mañana decidimos cambiar de Unirest a OkHttp o a RestAssured, solo cambiamos la implementación. Todos los steps siguen funcionando sin cambios.

#### `AuthenticationService.java` — El contrato de autenticación

Define cómo obtener tokens de seguridad para diferentes esquemas de autenticación:
- `getClientCredentialsToken()` — OAuth2 Client Credentials (credenciales de la config)
- `getClientCredentialsToken(String clientId, String clientSecret)` — OAuth2 con credenciales explícitas
- `getBearerTokenForIdentifier(String identifier)` — Bearer token para un RUT/ID
- `getCustomToken(String type, Map<String,String> claims)` — token personalizado (JWT con claims específicos)

#### `DatabaseService.java` — El contrato de base de datos para API tests

Define operaciones de base de datos que los tests API pueden usar para preparar o verificar datos: ejecutar queries, obtener resultados, limpiar datos de prueba. Permite verificar que una operación de API efectivamente creó o modificó datos en la base de datos.

---

### 5.5 Las Máquinas Reales — `implementations/`

Las implementaciones son el **código concreto** que hace el trabajo real. Cada una implementa el contrato definido en `interfaces/`.

#### `BaseHttpClient.java` — El motor HTTP (basado en Unirest)

Es la implementación concreta del `HttpClient`. Internamente usa la librería **Unirest** para hacer peticiones HTTP reales a los servicios web.

**Estado que mantiene internamente (por escenario):**
- `host` — URL base configurada
- `headers` — mapa de headers a enviar
- `queryParams` — parámetros de URL
- `pathParams` — reemplazos en la URL
- `body` — cuerpo de la petición
- `lastResponse` — última respuesta recibida (`HttpResponse`)
- `lastResponseTimeMs` — tiempo que tardó la última petición
- `lastRequestUrl` — URL completa de la última petición

**¿Qué pasa cuando se llama `httpClient.post("")`?**
1. Unirest arma la URL: host + path params
2. Agrega todos los headers configurados
3. Agrega los query params a la URL
4. Envía el body configurado
5. Mide el tiempo de respuesta
6. Convierte la respuesta al modelo `HttpResponse`
7. Guarda todo para consulta posterior
8. Limpia params y body para la siguiente petición

#### `BaseAuthenticationManager.java` — El gestor de tokens

Implementa el `AuthenticationService`. Sabe cómo obtener tokens de seguridad reales:
- Lee la configuración del servidor de autorización desde `ConfigManager` (URL, client_id, client_secret)
- Hace peticiones HTTP al endpoint de autorización
- Soporta OAuth2 grant types (client_credentials)
- Cachea tokens válidos para no hacer peticiones innecesarias
- Retorna el token listo para usar como Bearer en el header `Authorization`

#### `BaseDatabaseConfiguration.java` y `BaseDatabaseService.java`

Soporte de base de datos para tests API:
- `BaseDatabaseConfiguration`: POJO con la configuración de conexión a BD para tests de API (URL, usuario, contraseña, pool config)
- `BaseDatabaseService`: implementación base de `DatabaseService` que permite a los tests API verificar estados en la BD después de ejecutar operaciones via API (ej: verificar que una transferencia creada por API aparece en la tabla `TRANSFERENCIAS` con los datos correctos)

---

### 5.6 Las Fábricas — `factories/`

Las fábricas son clases cuya única responsabilidad es **saber cómo crear** otros objetos correctamente configurados.

#### `HttpClientFactory.java`
Crea instancias de `HttpClient`. Por defecto crea un `BaseHttpClient`. Si en el futuro hay múltiples implementaciones (con SSL especial, con proxy, con autenticación mutua), la fábrica decide cuál crear según la configuración.

#### `AuthenticationServiceFactory.java`
Crea instancias de `AuthenticationService`. Inyecta el `HttpClient` necesario para que el servicio de autenticación pueda hacer las peticiones HTTP al servidor de autorización.

#### `DatabaseServiceFactory.java`
Crea instancias de `DatabaseService` configuradas con los parámetros de conexión correctos según el ambiente de pruebas (qa, staging, etc.).

---

### 5.7 Las Herramientas — `utils/`

#### `ApiHelper.java` — La Fachada Central ⭐

**Es la clase más importante y más usada por los steps.**

`ApiHelper` es una **fachada**: un punto de acceso único que combina el `HttpClient` con `ValidationUtilities` y agrega logging automático. Los steps le delegan el trabajo en vez de hacerlo directamente.

**¿Por qué existe si ya tenemos HttpClient?**

| Tarea | Sin ApiHelper | Con ApiHelper |
|-------|--------------|---------------|
| Obtener respuesta y validar status | 5-8 líneas con try-catch | 1 línea |
| Loguear qué pasó | Manual en cada step | Automático |
| Manejo de errores | Repetido en cada step | Centralizado |
| Reemplazar variables `${...}` | Manual en cada step | Automático |
| Acceder al body de la respuesta | `httpClient.getLastResponse().getBody()` | `apiHelper.getLastResponse().getBody()` |

**Responsabilidades de ApiHelper:**
- **Configuración**: `configureEndpoint()`, `setBaseHost()`, `addHeader()`, `addQueryParam()`, `setRequestBody()`, `setJsonBody()`, `setXmlBody()`, `setFormDataBody()`, `setBodyFromFile()`, `setBodyFromTemplate()`
- **Autenticación**: `addBearerToken()`, `addBasicAuthentication()`
- **Validaciones de respuesta**: `validateResponseStatusCode()`, `validateResponseSchema()`, `validateJsonPathValue()`, `validateJsonPathNotNull()`, `validateJsonPathType()`, `validateJsonArraySize()`, `validateJsonPathMatchesPattern()`
- **Extracción de datos**: `extractAndStoreJsonValue()`, `extractFieldFromObject()`, `saveDeserializedObject()`, `extractAndStoreJsonValueSimple()`
- **Performance**: `getLastResponseTimeMs()`, `getLastRequestUrl()`
- **Diagnóstico**: `showLastRequestInfo()` — imprime toda la información de la última petición en el log

**Flujo de delegación:**
```
Step llama apiHelper.validateResponseStatusCode(200)
    │
    ▼
ApiHelper obtiene httpClient.getLastResponse()
    │
    ▼
ApiHelper llama ValidationUtilities.validateStatusCode(response, 200)
    │
    ▼
Si falla → lanza FrameworkBusinessException (con mensaje descriptivo)
Si pasa → TestLogger.logInfo("Status code validado: 200")
```

#### `ValidationUtilities.java` — El Validador Técnico Puro

Contiene **validaciones técnicas puras**: funciones que reciben datos y determinan si son válidos, sin estado interno ni efectos secundarios. Pueden ser llamadas desde cualquier parte del framework.

**Grupos de validaciones disponibles:**

| Grupo | Métodos | ¿Cuándo usar? |
|-------|---------|--------------|
| **HTTP Status** | `validateStatusCode()`, `validateStatusCodeRange()` | Verificar que el servidor respondió con el código correcto |
| **HTTP Headers** | `validateHeaderExists()`, `validateHeaderValue()`, `validateHeaderContains()` | Verificar los headers de la respuesta |
| **JSON Schema** | `validateJsonSchema(HttpResponse, schema)`, `validateJsonSchema(String, schema)` | Verificar que el JSON tiene la estructura correcta según un esquema |
| **JSON Path** | `validateJsonPath()`, `validateJsonPathExists()`, `validateJsonType()`, `validateJsonArraySize()`, `validateJsonPathMatchesPattern()` | Verificar campos específicos del JSON usando rutas tipo `$.user.name` |
| **Patrones** | `validatePattern()`, `validateEmail()`, `validateUrl()`, `validateUUID()` | Verificar formatos con expresiones regulares |
| **Chile** | `isValidRut()`, `isValidPhoneChile()` | Validar formatos específicos de Chile |
| **Numérico** | `validateRange()`, `validatePositive()` | Verificar rangos y valores positivos |
| **Tipo** | `isPrimitiveOrWrapper()` | Verificar si un tipo Java es primitivo o wrapper |

**Características clave:**
- Todos los métodos son `static` (no necesitan crear instancia)
- Thread-safe: no comparte ningún estado
- Lanzan `FrameworkBusinessException` con mensajes muy descriptivos cuando falla
- Log automático en nivel `debug` cuando pasan, `error` cuando fallan

**Patrones comunes disponibles como constantes:**
```java
ValidationUtilities.EMAIL_PATTERN     // ^[A-Za-z0-9+_.-]+@...
ValidationUtilities.URL_PATTERN       // ^https?://...
ValidationUtilities.UUID_PATTERN      // ^[0-9a-fA-F]{8}-...
ValidationUtilities.RUT_PATTERN       // ^[0-9]{1,2}\.[0-9]{3}...
ValidationUtilities.PHONE_PATTERN     // ^\+56[0-9]{8,9}$
```

#### `DatabaseTestUtilities.java` — Utilidades BD para Tests API

Provee métodos de conveniencia para que los tests de API puedan verificar el estado de la base de datos. Por ejemplo: después de crear una transferencia por API, ejecutar una query SQL para verificar que el registro existe en la tabla `TRANSFERENCIAS` con los datos correctos.

---

## 6. Los 12 Componentes de Steps API

Los 12 componentes se organizan en tres grupos según la fase BDD que representan:

### 🔵 GIVEN — Configuración de la Petición (6 componentes)

Todos estos pasos **preparan** la petición antes de enviarla. Son el setup de "qué voy a enviar y hacia dónde".

```
┌─────────────────────────────────────────────────────────────────┐
│  FASE GIVEN — "Dado que..." / Setup                              │
├──────────────┬──────────────────────────────────────────────────┤
│ 1. URL       │ ¿Adónde va la petición?                          │
│              │ Configura host, endpoint, ambiente, protocolo     │
├──────────────┼──────────────────────────────────────────────────┤
│ 2. Auth      │ ¿Quién dice ser?                                 │
│              │ Bearer, Basic, OAuth2, JWT, API Key               │
├──────────────┼──────────────────────────────────────────────────┤
│ 3. Headers   │ ¿Qué metadatos lleva?                            │
│              │ Content-Type, Accept, headers custom              │
├──────────────┼──────────────────────────────────────────────────┤
│ 4. Cookies   │ ¿Qué cookies lleva?                              │
│              │ Sesión, expiradas, ninguna                        │
├──────────────┼──────────────────────────────────────────────────┤
│ 5. Params    │ ¿Qué parámetros de URL lleva?                    │
│              │ Query params (?key=val), path params /{id}        │
├──────────────┼──────────────────────────────────────────────────┤
│ 6. Body      │ ¿Qué datos lleva en el cuerpo?                   │
│              │ JSON, XML, form-data, lectura desde archivo externo, templates |
└──────────────┴──────────────────────────────────────────────────┘
```

### 🟡 WHEN — Ejecución (1 componente)

Un solo paso **dispara la petición**. El momento en que el asistente "presiona enviar".

```
┌─────────────────────────────────────────────────────────────────┐
│  FASE WHEN — "Cuando..." / Acción                                │
├──────────────┬──────────────────────────────────────────────────┤
│ 7. Ejecución │ ¿Qué método HTTP se ejecuta?                     │
│              │ GET, POST, PUT, PATCH, DELETE, con/sin timeout    │
└──────────────┴──────────────────────────────────────────────────┘
```

### 🟢 THEN — Validación (5 componentes)

Estos pasos **verifican que la respuesta sea correcta**. Son el "¿qué esperaba y qué obtuve?".

```
┌─────────────────────────────────────────────────────────────────┐
│  FASE THEN — "Entonces..." / Verificación                        │
├───────────────────┬─────────────────────────────────────────────┤
│ 8. Status Code    │ ¿El servidor respondió con el código        │
│                   │ correcto? (200, 404, 422...)                 │
├───────────────────┼─────────────────────────────────────────────┤
│ 9. Response Body  │ ¿El cuerpo tiene los datos correctos?       │
│                   │ Texto, campos JSON, tipos, listas, esquema  │
├───────────────────┼─────────────────────────────────────────────┤
│ 10. Resp. Headers │ ¿Los headers de respuesta son los           │
│                   │ esperados? Content-Type, Cache-Control...   │
├───────────────────┼─────────────────────────────────────────────┤
│ 11. Performance   │ ¿Respondió suficientemente rápido?          │
│                   │ Tiempo en ms/s, tamaño del body en KB       │
├───────────────────┼─────────────────────────────────────────────┤
│ 12. Security      │ ¿Cumple los controles de seguridad?         │
│                   │ HTTPS, headers seguridad, sin stack traces  │
└───────────────────┴─────────────────────────────────────────────┘
```

---

## 7. Catálogo Completo de Steps BDD

### 📍 URL y Ambiente (`UrlConfigSteps`)

| Step en Gherkin | Descripción |
|-----------------|-------------|
| `Given configuro el endpoint {string}` | Lee la URL de una clave en `config-{env}.properties`. Ejemplo: `"api.login.url"` → busca `api.login.url=https://api-qa.../auth/login` |
| `Given configuro endpoint con base {string} y path {string}` | Combina una URL base + un path, ambos leídos de la config |
| `Given establezco el host base como {word}` | Establece directamente la URL base (sin leer de config). Soporta variables `${...}` |
| `Given configuro el ambiente {word}` | Lee `api.baseurl.{ambiente}` de la config. Ejemplo: `"qa"` → lee `api.baseurl.qa` |
| `Given configuro la URL completa {string}` | Establece la URL completa directamente como string |
| `Given configuro el protocolo {word}` | Cambia el protocolo. Ejemplo: `"https"` → convierte la URL a HTTPS |

### 🔐 Autenticación (`AuthenticationSteps`)

| Step en Gherkin | Descripción |
|-----------------|-------------|
| `Given agrego autenticacion Client Credentials` | Obtiene token OAuth2 Client Credentials desde la config y lo agrega como `Bearer` |
| `Given agrego autenticación Client Credentials` | Alias con acento (compatibilidad) |
| `Given agrego autenticación Bearer para RUT {word}` | Obtiene token Bearer para un RUT. Soporta `${variable}` |
| `Given agrego el token personalizado {word}` | Agrega directamente un token Bearer (útil cuando el token está en una variable) |
| `Given agrego autenticación básica con usuario {string} y password {string}` | Agrega `Authorization: Basic {Base64(user:pass)}` |
| `Given configuro autenticación OAuth2 con client_id {string} y client_secret {string}` | OAuth2 con credenciales explícitas en el step |
| `Given agrego API Key {string} en header {string}` | Agrega API Key como header personalizado. Soporta variables |
| `Given agrego API Key {string} como query param {string}` | Agrega API Key como query parameter. Soporta variables |
| `Given configuro JWT con las siguientes claims` | Genera JWT con claims de una tabla de datos (`\| key \| value \|`) |
| `Given agrego token expirado para prueba de seguridad` | Agrega el string `"EXPIRED_TOKEN_FOR_SECURITY_TEST"` como Bearer (para pruebas negativas) |
| `Given agrego token inválido para prueba de seguridad` | Agrega un token malformado como Bearer (para pruebas negativas) |
| `Given no agrego autenticación` | Remueve el header `Authorization` |

### 📋 Headers (`HeaderSteps`)

| Step en Gherkin | Descripción |
|-----------------|-------------|
| `Given agrego el header {word} con valor {word}` | Agrega un header. Soporta variables `${...}` en el valor |
| `Given agrego Content-Type {string}` | Agrega `Content-Type`. Ejemplo: `"application/json"` |
| `Given agrego Accept {string}` | Agrega `Accept`. Ejemplo: `"application/json"` |
| `Given agrego los siguientes headers` | Agrega múltiples headers desde una tabla Cucumber (`\| Header \| Valor \|`) |
| `Given agrego header de versión API {string}` | Agrega el header `X-API-Version` con ese valor |
| `Given remuevo el header {string}` | Elimina ese header de la petición |

### 🍪 Cookies (`CookieSteps`)

| Step en Gherkin | Descripción |
|-----------------|-------------|
| `Given agrego cookie {string} con valor {string}` | Agrega `Cookie: {name}={valor}`. Soporta variables en el valor |
| `Given agrego cookie de sesión {string}` | Agrega el string completo de cookie (incluyendo path, domain, etc.) |
| `Given agrego cookie expirada {string} para prueba de seguridad` | Agrega `Cookie: {name}=EXPIRED; Max-Age=0` para pruebas de seguridad |
| `Given no agrego cookies` | Remueve el header `Cookie` |

### 🔗 Parámetros (`ParameterSteps`)

| Step en Gherkin | Descripción |
|-----------------|-------------|
| `Given agrego el query param {string} con valor {string}` | Agrega `?param=valor` a la URL. Soporta variables |
| `Given agrego el queryparam {string} con el valor {string}` | Alias con ortografía alternativa |
| `Given agrego los siguientes query params` | Agrega múltiples params desde DataTable |
| `Given reemplazo el path param {string} con el valor {string}` | Sustituye `{param}` en la URL por ese valor |
| `Given no envío query params` | Limpia todos los query params configurados |
| `Given agrego query param {string} sin valor` | Agrega un flag booleano como `?flag=` |

### 📦 Body de la Petición (`RequestBodySteps`)

| Step en Gherkin | Descripción |
|-----------------|-------------|
| `Given establezco el cuerpo de la petición como` | Establece el body desde un DocString (bloque `"""`). Soporta variables |
| `Given establezco el cuerpo JSON con los siguientes datos` | Crea JSON desde DataTable (`\| campo \| valor \|`) |
| `Given agrego el request body {string}` | Establece el body como string inline |
| `Given agrego el request` | Establece el body desde DocString (alias) |
| `Given agrego el field {string} con el valor {string}` | Agrega un campo al body field por field |
| `Given establezco el cuerpo XML como` | Establece el body en formato XML (DocString) |
| `Given establezco el cuerpo como form-data con los siguientes campos` | Body multipart/form-data desde DataTable |
| `Given establezco el cuerpo desde el archivo {string}` | Lee el body desde un archivo en disco |
| `Given establezco el cuerpo con el template {string} y los datos` | Usa un template con sustitución de variables desde DataTable |

### ▶️ Ejecución HTTP (`HttpExecutionSteps`)

| Step en Gherkin | Descripción |
|-----------------|-------------|
| `When ejecuto una petición {string}` | Dispara la petición con el método dado: `"GET"`, `"POST"`, `"PUT"`, `"PATCH"`, `"DELETE"` |
| `When ejecuto la consulta con el metodo {string}` | Alias del anterior |
| `When ejecuto la consulta con el metodo {string} sin redireccion` | Ejecuta sin seguir redirects HTTP (301/302) |
| `When ejecuto GET a {string}` | Configura endpoint directo y ejecuta GET |
| `When ejecuto POST a {string}` | Configura endpoint directo y ejecuta POST |
| `When ejecuto PUT a {string}` | Configura endpoint directo y ejecuta PUT |
| `When ejecuto PATCH a {string}` | Configura endpoint directo y ejecuta PATCH |
| `When ejecuto DELETE a {string}` | Configura endpoint directo y ejecuta DELETE |
| `When ejecuto la petición y espero {int} segundos máximo` | Ejecuta GET con timeout personalizado |

### ✅ Status Code (`StatusCodeSteps`)

| Step en Gherkin | Descripción |
|-----------------|-------------|
| `Then valido que el codigo de respuesta del servicio sea {int}` | El código HTTP es exactamente ese número |
| `Then valido que el servicio responda con éxito` | El código está entre 200 y 299 (familia 2xx) |
| `Then valido que el servicio responda con error de cliente` | El código está entre 400 y 499 (familia 4xx) |
| `Then valido que el servicio responda con error de servidor` | El código está entre 500 y 599 (familia 5xx) |
| `Then valido que el status code esté entre {int} y {int}` | El código está en el rango [min, max] |
| `Then valido que el status code NO sea {int}` | El código NO es ese número |

### 📨 Cuerpo de Respuesta (`ResponseBodySteps`)

| Step en Gherkin | Descripción |
|-----------------|-------------|
| `Then valido que la respuesta contenga el texto {word}` | El body contiene ese texto (búsqueda de substring) |
| `Then valido que el cuerpo de la respuesta tenga el siguiente esquema` | El JSON cumple el JSON Schema del DocString (o archivo) |
| `Then serializo la respuesta en la clase {string}` | Deserializa el JSON a un objeto Java de esa clase |
| `Then guardo el objeto serializado como {string}` | Guarda el objeto Java deserializado como variable |
| `Then obtengo el campo {string} del objeto {string} y lo guardo como {string}` | Extrae campo de objeto guardado y lo guarda como nueva variable |
| `Then el resultado almaceno el valor de {string}` | Extrae valor con JSONPath y lo guarda automáticamente |
| `Then el resultado almaceno el valor que está dentro de la estructura {string} en {string}` | Extrae con JSONPath y guarda con nombre específico |
| `Then valido que la respuesta NO contenga el texto {string}` | El body NO contiene ese texto |
| `Then valido que el campo {string} tenga el valor {string}` | El campo en esa ruta JSONPath tiene ese valor exacto |
| `Then valido que el campo {string} NO sea null` | El campo existe y su valor no es null |
| `Then valido que el campo {string} sea de tipo {word}` | El campo es de tipo: `string`, `integer`, `boolean`, `array`, `object` |
| `Then valido que la lista {string} tenga {int} elementos` | El array JSON en esa ruta tiene exactamente N elementos |
| `Then valido que la respuesta sea un JSON vacío` | El body es `{}` o `[]` o vacío |
| `Then valido que el campo {string} cumpla el patrón {string}` | El campo cumple esa expresión regular |
| `Then valido que el campo {string} del error contenga {string}` | En mensajes de error: ese campo contiene ese texto |

> **JSONPath:** Rutas que permiten navegar JSON como `$.user.name`, `$.items[0].id`, `$.data.accounts[*].balance`. El símbolo `$` es la raíz del JSON.

### 🗂️ Headers de Respuesta (`ResponseHeaderSteps`)

| Step en Gherkin | Descripción |
|-----------------|-------------|
| `Then valido que el header de respuesta {string} sea {string}` | El header tiene exactamente ese valor |
| `Then valido que el header de respuesta {string} contenga {string}` | El header contiene ese texto (substring) |
| `Then valido que la respuesta tenga Content-Type {string}` | El Content-Type contiene ese valor |
| `Then valido que la respuesta tenga header Cache-Control` | Existe el header Cache-Control (cualquier valor) |
| `Then valido que la respuesta tenga header Set-Cookie` | Existe el header Set-Cookie (cualquier valor) |
| `Then extraigo el header {string} y lo guardo como {string}` | Guarda el valor del header en una variable para uso posterior |

### ⚡ Performance (`ResponsePerformanceSteps`)

| Step en Gherkin | Descripción |
|-----------------|-------------|
| `Then valido que el tiempo de respuesta sea menor a {int} milisegundos` | Tiempo total ≤ N ms |
| `Then valido que el tiempo de respuesta sea menor a {int} segundos` | Tiempo total ≤ N segundos |
| `Then guardo el tiempo de respuesta como {string}` | Guarda el tiempo en ms como variable (para comparaciones posteriores) |
| `Then valido que el tamaño de la respuesta sea menor a {int} KB` | El tamaño del body ≤ N kilobytes |

### 🔒 Seguridad (`ResponseSecuritySteps`)

| Step en Gherkin | Descripción |
|-----------------|-------------|
| `Then valido que la respuesta use HTTPS` | La URL de la petición comenzó con `https://` |
| `Then valido que no haya headers de información sensible expuestos` | No existen: `Server`, `X-Powered-By`, `X-AspNet-Version`, `X-AspNetMvc-Version` |
| `Then valido que el header X-Content-Type-Options sea {string}` | `X-Content-Type-Options` tiene ese valor (tipicamente `"nosniff"`) |
| `Then valido que el header X-Frame-Options esté presente` | Existe `X-Frame-Options` (contra clickjacking) |
| `Then valido que la respuesta no contenga trazas de stack` | No hay `"at com."`, `"StackTrace"`, `"NullPointerException"` en el body |
| `Then valido protección contra SQL injection intentando {string}` | El status fue 400, 401, 403 o 422 (el servidor rechazó el payload) |
| `Then valido protección contra XSS intentando {string}` | El body no refleja tags `<script>` del payload enviado |

### 🔄 Variables (`VariableSteps`)

| Step en Gherkin | Descripción |
|-----------------|-------------|
| `Given almaceno el valor {word} como {word}` | Guarda un valor en una variable. Ejemplo: `"activo"` como `"estado"` |
| `Given establezco la key {string} con el valor {string}` | Alias del anterior con sintaxis de comillas dobles |
| `Then muestro la información de la última petición` | Imprime en el log: URL, método, headers, body, status, response body, tiempo |
| `Given genero un UUID y lo guardo como {string}` | Genera UUID v4 aleatorio y lo guarda como variable |
| `Given genero un timestamp y lo guardo como {string}` | Guarda el timestamp Unix actual en milisegundos |
| `Given genero un número aleatorio entre {int} y {int} y lo guardo como {string}` | Genera número random en [min, max] y lo guarda |
| `Then muestro el valor de la variable {string}` | Imprime el valor de una variable en el log de prueba |

---

## 8. Flujo Completo de una Prueba API

Sigamos el viaje de este escenario BDD de principio a fin:

```gherkin
@api @smoke
Scenario: Verificar que el login retorna un token válido
  Given configuro el endpoint "api.auth.login.url"
  And agrego autenticación básica con usuario "qa_user" y password "qa_pass"
  And establezco el cuerpo de la petición como
    """
    {"username": "testuser", "password": "testpass123"}
    """
  When ejecuto una petición "POST"
  Then valido que el codigo de respuesta del servicio sea 200
  And valido que el campo "$.access_token" NO sea null
  And valido que el campo "$.token_type" tenga el valor "Bearer"
  And valido que el tiempo de respuesta sea menor a 3000 milisegundos
```

### Paso 0: El Motor de Ejecución Inicia

El `CucumberRuntimeEngine` (en `common/`) recibe la solicitud de ejecutar este escenario. Detecta el tag `@api` → activa `ApiPlugin` → este registra en el `ServiceRegistry` del `ExecutionContext`: `HttpClient` (lazy), `AuthenticationService` (lazy), `ApiHelper` (lazy).

### Paso 1: `Given configuro el endpoint "api.auth.login.url"`

```
Cucumber → UrlConfigSteps.configuroElEndpoint("api.auth.login.url")
         → apiHelper.configureEndpoint("api.auth.login.url")
         → ConfigManager.getInstance().get("api.auth.login.url")
         → Retorna: "https://api-qa.scotiabank.cl/auth/login"
         → DataUtilities.replaceVariables("https://...") (no hay variables, sin cambio)
         → httpClient.setHost("https://api-qa.scotiabank.cl/auth/login")
         → BaseHttpClient guarda el host
         → Log: "✅ Endpoint configurado: api.auth.login.url = https://..."
```

### Paso 2: `And agrego autenticación básica con usuario "qa_user" y password "qa_pass"`

```
Cucumber → AuthenticationSteps.agregoAutenticacionBasicaConUsuarioYPassword("qa_user", "qa_pass")
         → apiHelper.addBasicAuthentication("qa_user", "qa_pass")
         → Base64("qa_user:qa_pass") = "cWFfdXNlcjpxYV9wYXNz"
         → httpClient.addHeader("Authorization", "Basic cWFfdXNlcjpxYV9wYXNz")
         → BaseHttpClient guarda el header
```

### Paso 3: `And establezco el cuerpo...` (DocString JSON)

```
Cucumber → RequestBodySteps.establezcoElCuerpoDeLaPeticionComo(jsonBody)
         → apiHelper.setRequestBody("{\"username\": \"testuser\", ...}")
         → DataUtilities.replaceVariables(body) (sin variables, sin cambio)
         → httpClient.setBody("{\"username\": \"testuser\", \"password\": \"testpass123\"}")
         → BaseHttpClient guarda el body
         → Log: "✅ Request body agregado"
```

### Paso 4: `When ejecuto una petición "POST"` ← EL MOMENTO CLAVE

```
Cucumber → HttpExecutionSteps.ejecutoUnaPeticionAlEndpoint("POST")
         → httpClient.post("")
         → BaseHttpClient arma la petición completa con Unirest:
              URL:     https://api-qa.scotiabank.cl/auth/login
              Method:  POST
              Headers: {Authorization: "Basic cWFf...", Content-Type: "application/json"}
              Body:    {"username": "testuser", "password": "testpass123"}
         → Unirest envía al servidor real
         → Servidor responde:
              Status:  200
              Body:    {"access_token": "eyJhbGci...", "token_type": "Bearer", "expires_in": 3600}
              Tiempo:  450ms
         → BaseHttpClient guarda: lastResponse, lastResponseTimeMs=450
         → HttpExecutionSteps intenta deserializar el body (para acceso posterior)
         → Log: "POST ejecutado"
```

### Paso 5: `Then valido que el codigo de respuesta del servicio sea 200`

```
Cucumber → StatusCodeSteps.validoQueElCodigoDeRespuestaDelServicioSea(200)
         → apiHelper.validateResponseStatusCode(200)
         → httpClient.getLastResponse() → HttpResponse{status=200, body="..."}
         → ValidationUtilities.validateStatusCode(response, 200)
              actualStatus = 200
              200 == 200 → ✅ PASA
         → Log: "Status code validado: 200"
         → Paso marcado en VERDE ✅
```

### Paso 6: `And valido que el campo "$.access_token" NO sea null`

```
Cucumber → ResponseBodySteps.validoCampoNoSeaNull("$.access_token")
         → apiHelper.validateJsonPathNotNull("$.access_token")
         → ValidationUtilities.validateJsonPathExists(response, "$.access_token")
              DataUtilities.hasJsonField(body, "$.access_token") → true
         → Luego verifica que el valor no sea null
              DataUtilities.getJsonParameter(body, "$.access_token") → "eyJhbGci..."
              "eyJhbGci..." != null → ✅ PASA
```

### Paso 7: `And valido que el campo "$.token_type" tenga el valor "Bearer"`

```
Cucumber → ResponseBodySteps.validoCampoTengaValor("$.token_type", "Bearer")
         → apiHelper.validateJsonPathValue("$.token_type", "Bearer")
         → ValidationUtilities.validateJsonPath(response, "$.token_type", "Bearer")
              DataUtilities.getJsonParameter(body, "$.token_type") → "Bearer"
              "Bearer".equals("Bearer") → ✅ PASA
```

### Paso 8: `And valido que el tiempo de respuesta sea menor a 3000 milisegundos`

```
Cucumber → ResponsePerformanceSteps.validoTiempoRespuesta(3000)
         → apiHelper.getLastResponseTimeMs() → 450L
         → AssertJ: assertThat(450L).isLessThanOrEqualTo(3000) → ✅ PASA
```

### Fin del escenario ✅

`ApiPlugin.onScenarioEnd()` se llama automáticamente → `httpClient.reset()` limpia todo el estado HTTP → listo para el siguiente escenario.

---

## 9. Relaciones Entre Clases

```
SPI / ServiceLoader descubre:
                    │
                    ▼
         ┌─────────────────────┐
         │      ApiPlugin       │  ← registerServices() registra en ServiceRegistry:
         └──────────┬──────────┘
                    │ registra (lazy)
      ┌─────────────┼─────────────────┐
      ▼             ▼                 ▼
┌──────────┐  ┌──────────────┐  ┌──────────────┐
│HttpClient│  │  AuthService  │  │   ApiHelper  │
│(interface│  │ (interface)  │  │  (facade)    │
└────┬─────┘  └──────┬───────┘  └──────┬───────┘
     │                │                │
     ▼                ▼                │ usa ambos
┌─────────────┐ ┌────────────────┐    │
│BaseHttpClient│ │BaseAuthManager│    │
│ (Unirest)   │ │ (OAuth2/Bearer)│    │
└─────────────┘ └────────────────┘    │
      ▲                               │
      └───────────────────────────────┘
                                      │ usa además
                                      ▼
                         ┌────────────────────────┐
                         │   ValidationUtilities   │
                         │ (validaciones estáticas) │
                         └────────────────────────┘

STEPS — 13 clases (cada una usa ApiHelper u HttpClient):
┌─────────────────────────────────────────────────────┐
│  config/       execution/      validation/           │
│  UrlConfig     HttpExecution   StatusCode            │
│  Auth                          ResponseBody          │
│  Headers                       ResponseHeaders       │
│  Cookies                       ResponsePerformance   │
│  Parameters                    ResponseSecurity      │
│  RequestBody                                         │
│  VariableSteps (transversal)                         │
└─────────────────────────────────────────────────────┘

COMPONENTS — 12 descriptores de metadatos (apuntan a clases de steps):
┌─────────────────────────────────────────────────────┐
│  ApiUrlComponent → UrlConfigSteps.class              │
│  ApiAuthComponent → AuthenticationSteps.class        │
│  ApiHeaderComponent → HeaderSteps.class              │
│  ... (un descriptor por cada clase de steps)         │
└─────────────────────────────────────────────────────┘
```

---

## 10. Patrones de Diseño Usados

### 🔌 Plugin / SPI (Service Provider Interface)
`ApiPlugin` implementa `CorePlugin` y se auto-registra vía `META-INF/services/`. El motor de ejecución los descubre automáticamente usando el mecanismo `java.util.ServiceLoader` de Java estándar. Permite agregar o quitar capacidades sin modificar el Core.

### 🏭 Factory (Fábrica)
`HttpClientFactory`, `AuthenticationServiceFactory` y `DatabaseServiceFactory` centralizan la creación de objetos. Ocultan la complejidad de la construcción y permiten cambiar la implementación sin impacto en quien las usa.

### 🎭 Facade (Fachada)
`ApiHelper` es el ejemplo más claro. Es un punto de acceso unificado que oculta la complejidad de coordinar `HttpClient`, `ValidationUtilities`, `DataUtilities` y `TestLogger`. Los steps le delegan el trabajo en vez de hacerlo directamente.

### 📋 Interface / Abstraction (Abstracción)
`HttpClient` y `AuthenticationService` son interfaces que permiten múltiples implementaciones intercambiables. El principio de Dependency Inversion (D de SOLID) en acción.

### 🏷️ Component / Metadata (Metadato)
Las 12 clases en `components/` implementan la interface `StepComponent` y actúan como descriptores de metadatos enriquecidos, usados por el sistema de descubrimiento del Backend y la paleta visual del Frontend.

### 🔄 Lazy Initialization (Inicialización perezosa)
El `ServiceRegistry` del `ExecutionContext` (en `common/`) crea los servicios solo cuando son solicitados por primera vez. Esto evita crear objetos que quizás no se necesitan en un escenario específico.

### 📦 Strategy (Estrategia)
El `BaseAuthenticationManager` puede usar diferentes estrategias de autenticación (Client Credentials, Bearer, Basic) dependiendo del método invocado. La implementación interna varía, pero la interfaz es la misma.

---

## 11. Dependencias del Módulo

### `build.gradle` de api-core (simplificado)
```groovy
dependencies {
    implementation project(':common')          // Base del framework
    implementation "io.cucumber:cucumber-java:7.18.0"
    implementation "com.konghq:unirest-java:3.x.x"          // HTTP client
    implementation "com.networknt:json-schema-validator:1.x" // JSON Schema
    implementation "com.jayway.jsonpath:json-path:2.x.x"     // JSONPath
    implementation "com.fasterxml.jackson.core:jackson-databind:2.x.x"  // JSON
    implementation "org.assertj:assertj-core:3.x.x"          // Aserciones fluidas
    testImplementation "io.cucumber:cucumber-junit-platform-engine:7.18.0"
    testImplementation "org.junit.platform:junit-platform-suite:1.x.x"
}
```

### Clases de `common` que usa api-core

| Clase en `common` | Cómo la usa api-core |
|------------------|---------------------|
| `ConfigManager` | Leer propiedades de config (`api.auth.url`, `api.baseurl.qa`, etc.) |
| `DataUtilities` | `replaceVariables()` — interpola `${variables}` en strings; `storeValue()`/`getValue()` — variables entre steps |
| `TestLogger` | Logging uniforme con categorías en todos los steps y ApiHelper |
| `HttpResponse` | Modelo de respuesta HTTP (status, body, headers, tiempo) |
| `HttpMethod` | Enum de métodos HTTP (GET, POST, PUT, PATCH, DELETE) |
| `FrameworkBusinessException` | Excepción que falla el step con mensaje descriptivo |
| `FrameworkTechnicalException` | Excepción para errores técnicos (timeout, sin conexión) |
| `CorePlugin` | Interface que `ApiPlugin` implementa |
| `StepComponent` | Interface que los 12 `ApiXxxComponent` implementan |
| `BddPhase` | Enum GIVEN/WHEN/THEN para categorizar componentes |
| `ServiceRegistry` | Registro de servicios del contexto de ejecución |
| `ExecutionContext` | Contexto de la ejecución actual (en proceso de migración) |

---

## 12. Estado Actual y Pendientes

### ✅ Completado en esta capa

| Elemento | Estado | Descripción |
|----------|--------|-------------|
| `ApiPlugin.java` | ✅ | Plugin completo: servicios + 12 componentes + lifecycle |
| 12 clases `ApiXxxComponent` | ✅ | Todos los descriptores de metadatos implementados |
| 6 step classes en `steps/config/` | ✅ | UrlConfig, Auth, Headers, Cookies, Parameters, RequestBody |
| 1 step class en `steps/execution/` | ✅ | HttpExecutionSteps |
| 5 step classes en `steps/validation/` | ✅ | StatusCode, ResponseBody, ResponseHeaders, Performance, Security |
| `VariableSteps.java` | ✅ | Steps transversales |
| `META-INF/services/` SPI | ✅ | Auto-registro de ApiPlugin |
| `ValidationUtilities.java` | ✅ | Validaciones completas con tests unitarios |
| `ApiHelper.java` | ✅ | Fachada completa |
| `BaseHttpClient.java` | ✅ | Implementación funcional con Unirest |
| `BaseAuthenticationManager.java` | ✅ | OAuth2 Client Credentials + Bearer |
| `ApiSteps.java` (God Class original) | ✅ | Eliminada — reemplazada por los 12 componentes |

### 🔄 Pendiente (Fase 2 → Fase 3 del roadmap)

| Pendiente | Descripción |
|-----------|-------------|
| **Migración a ExecutionContext** | Los steps actualmente instancian sus dependencias con `new BaseHttpClient()`. En la arquitectura final usarán `ExecutionContext.current().service(HttpClient.class)` para garantizar que todos los steps de un escenario compartan el mismo cliente HTTP. |
| **Migración de DataUtilities** | `DataUtilities.replaceVariables()` y `DataUtilities.storeValue()` serán reemplazados por `ExecutionContext.current().variables().resolve()` y `.set()`. Hay bridges temporales en su lugar. |
| **Simplificar `HttpClient` interface** | Actualmente tiene ~60 métodos. Se reducirá a ~25 métodos esenciales para mejor mantenibilidad (ítem M-02 de auditoría). |
| **Simplificar `BaseHttpClient`** | Proporcional a la reducción de la interface. |

> **Impacto de los pendientes en el funcionamiento actual:** Ninguno. Los steps funcionan correctamente. Los pendientes son mejoras de arquitectura que afectan aislamiento en ejecución paralela, no la funcionalidad en uso secuencial normal.

### 📊 Métricas del módulo

| Métrica | Valor |
|---------|-------|
| Archivos Java en producción | **35** |
| Steps BDD disponibles | **~92** steps en 13 clases |
| Componentes declarados | **12** descriptores |
| Interfaces (contratos) | **3** |
| Implementaciones | **4** |
| Fábricas | **3** |
| Utilidades | **3** (ApiHelper, ValidationUtilities, DatabaseTestUtilities) |
| Archivos de test | **2** (ApiHelperTest, ValidationUtilitiesTest) |
| Líneas de código aproximadas | **~3,500** |

---

> **Documentos complementarios:**
>
> - `docu/REDISENO-ARQUITECTONICO-CORE.md` — Arquitectura completa y tracking de fases
> - `docu/DISENO-STEPS-POR-COMPONENTES.md` — Diseño detallado de la componentización
> - `docu/AUDIT-ARQUITECTONICO-CORE.md` — Auditoría clase por clase del estado original
> - `docu/prompt.md` — Contexto maestro de la plataforma completa (Core + Backend + Frontend)
