# CuAleon Test Engineering Platform — Core

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Gradle](https://img.shields.io/badge/Gradle-8.14-blue.svg)](https://gradle.org/)
[![Selenium](https://img.shields.io/badge/Selenium-4.27.0-green.svg)](https://www.selenium.dev/)
[![Cucumber](https://img.shields.io/badge/Cucumber-7.18.0-brightgreen.svg)](https://cucumber.io/)
[![Version](https://img.shields.io/badge/version-2.0.0-blue.svg)](https://github.com/avenero/qa-framework-core)

> Motor de ejecución BDD del ecosistema **CuAleon Test Engineering Platform** — framework modular de automatización para API REST, Web UI, Mobile y Base de Datos, construido sobre Cucumber en español y consumido por el Backend a través de una API de ejecución uniforme.

---

## 📑 Índice

- [¿Qué es este framework?](#-qué-es-este-framework)
- [¿Para quién es?](#-para-quién-es)
- [Arquitectura de capas](#️-arquitectura-de-capas)
- [El motor de ejecución — el corazón del sistema](#-el-motor-de-ejecución--el-corazón-del-sistema)
- [Cómo se usa — Quick Start](#-cómo-se-usa--quick-start)
- [Las 4 capas explicadas](#-las-4-capas-explicadas)
- [Matriz de tecnologías](#-matriz-de-tecnologías)
- [Estado del proyecto](#-estado-del-proyecto)
- [Mejores prácticas](#-mejores-prácticas)
- [Contribución](#-contribución)
- [Soporte](#-soporte)

---

## 🎯 ¿Qué es este framework?

Imagina que necesitas verificar que un sistema funciona correctamente — que cuando alguien inicia sesión, el sistema realmente lo deja entrar; que cuando se hace una compra, el dinero se descuenta bien; que la app móvil muestra los datos correctos. Eso es lo que hace este framework: **automatizar todas esas verificaciones** para que no tengan que hacerse a mano cada vez que el sistema cambia.

El framework está organizado como un **kit de herramientas por capas**:
- Una capa base con todas las piezas comunes (motor de ejecución, logging, config, HTTP, BD)
- Capas especializadas para cada tipo de prueba (API REST, Web UI, Mobile, Base de Datos)

La clave del diseño es que **las capas especializadas no se conocen entre sí** — solo conocen la base. Esto permite combinarlas libremente y reemplazar cualquier pieza sin afectar las demás.

> **Contexto de plataforma:** Este Core es el motor que el **Backend de CuAleon** consume directamente. El Backend recibe solicitudes de ejecución desde el Frontend (React), invoca `CucumberRuntimeEngine.execute(ExecutionRequest)`, y retorna `ExecutionResult` con los resultados en tiempo real vía `EventBus`. El Core no tiene UI propia — es una librería pura publicada como JARs.

### ¿Qué significa BDD?

BDD (Behavior-Driven Development) es una forma de escribir las pruebas usando **lenguaje natural** que cualquier persona puede leer, no solo programadores:

```gherkin
# Esto es una prueba escrita en BDD (Gherkin en español)
@api
Scenario: El sistema permite login con credenciales válidas
  Given configuro endpoint con base "https://mi-api.com/" y path "api/auth/login"
  And agrego el header "Content-Type" con valor "application/json"
  And agrego el request
    """
    { "username": "admin", "password": "Admin@QA2026!" }
    """
  When ejecuto la consulta con el metodo "POST"
  Then valido que el codigo de respuesta del servicio sea 200
  And valido que el campo "$.accessToken" NO sea null
```

Eso es suficiente para ejecutar una prueba real sobre un sistema real. No se necesita código Java adicional si se usan los steps que ya vienen incluidos.

---

## 👥 ¿Para quién es?

| Perfil | ¿Qué obtiene del framework? |
|--------|-----------------------------|
| **QA Engineer** | Steps listos para escribir pruebas en español sin código Java |
| **Desarrollador** | Base sólida para agregar nuevos tipos de steps o capacidades |
| **Tech Lead / Arquitecto** | Arquitectura limpia basada en SOLID y patrones reconocidos |
| **Gerente / Product Owner** | Pruebas legibles que documentan el comportamiento esperado del sistema |

---

## 🏗️ Arquitectura de Capas

### Vista General

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PROYECTOS DE PRUEBAS                             │
│              (repositorios independientes por equipo)                │
│                                                                     │
│   qa-proyecto-alpha/          qa-proyecto-beta/                     │
│   • features/*.feature        • features/*.feature                  │
│   • steps propios             • steps propios                       │
│   • config del proyecto       • config del proyecto                 │
│                                                                     │
│   build.gradle: implementation 'com.qa:api-core:2.0.0'            │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ dependen de
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│               qa-frameworks-core (este repositorio)                  │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │                   CAPA 1 — common                        │       │
│  │              Fundación compartida por todo               │       │
│  │                                                          │       │
│  │  Motor de Ejecución  •  Logging  •  HTTP Base            │       │
│  │  Configuración       •  Base de Datos  •  Utilidades     │       │
│  │  Plugin System (SPI) •  VariableStore  •  ExecutionCtx   │       │
│  │                                                          │       │
│  │  📖 Detalle completo: common/README.md                   │       │
│  └──────────────────────────────────────────────────────────┘       │
│                          ▲  ▲  ▲                                    │
│                          │  │  │  (cada capa especializada usa common)│
│           ┌──────────────┘  │  └──────────────┐                     │
│           │                 │                  │                     │
│  ┌────────┴──────┐  ┌──────┴──────┐  ┌────────┴──────┐             │
│  │  api-core     │  │  web-core   │  │  mobile-core  │             │
│  │               │  │             │  │               │             │
│  │ Pruebas REST  │  │ Pruebas Web │  │ Pruebas Móvil │             │
│  │ ~92 steps     │  │ ~80 steps   │  │ ~60 steps     │             │
│  │ 13 clases     │  │ 16 componentes│  │ 10 componentes│            │
│  │               │  │             │  │               │             │
│  │ 📖 api-core/  │  │ 📖 web-core/│  │ 📖 mobile-    │             │
│  │    README.md  │  │    README.md│  │    core/      │             │
│  └───────────────┘  └─────────────┘  │    README.md  │             │
│                                       └───────────────┘             │
└─────────────────────────────────────────────────────────────────────┘
```

### Principios de Diseño

El framework sigue principios SOLID y patrones reconocidos de la industria:

| Principio | Cómo se aplica |
|-----------|----------------|
| **Responsabilidad Única** | Cada clase hace una sola cosa (ej: `StatusCodeSteps` solo valida códigos HTTP) |
| **Abierto/Cerrado** | Se agregan capacidades via Plugin sin tocar el motor de ejecución |
| **Inversión de Dependencias** | Los steps dependen de interfaces (`HttpClient`), no de implementaciones (`BaseHttpClient`) |
| **DRY** (no repetir) | El código común va en `common`; las capas lo reutilizan sin copiarlo |
| **Convención sobre configuración** | Los plugins se auto-registran via SPI; no hay XML ni configuración manual |

---

## ⚙️ El Motor de Ejecución — el corazón del sistema

La gran novedad de la versión 2.0 es el **sistema de plugins** que vive en `common/runtime/`. Este es el mecanismo que hace que todo funcione de manera ordenada y extensible.

### ¿Cómo funciona? (sin tecnicismos)

Piensa en el motor de ejecución como el **director de una orquesta**:

1. **Recibe la partitura** (el escenario `.feature` con tags como `@api` o `@web`)
2. **Convoca a los músicos correctos** (activa solo los plugins que corresponden a los tags)
3. **Prepara el escenario** (cada plugin registra sus servicios: HttpClient, WebDriver, etc.)
4. **Dirige la ejecución** (Cucumber ejecuta cada step, que llama al servicio registrado)
5. **Cierra ordenadamente** (al terminar el escenario, cada plugin limpia sus recursos)

### El ciclo de vida de un escenario

```
Cucumber detecta @api en el feature
         │
         ▼
CucumberRuntimeEngine activa ApiPlugin
         │
         ├── ApiPlugin.registerServices()
         │     → registra HttpClient (lazy)
         │     → registra AuthenticationService (lazy)
         │     → registra ApiHelper (lazy)
         │
         ├── ApiPlugin.onScenarioStart()
         │     → limpia el estado HTTP previo
         │
         ├── [Cucumber ejecuta cada step Given/When/Then]
         │     → cada step pide su servicio a ServiceRegistry
         │     → el servicio se crea solo la primera vez que se pide
         │
         └── ApiPlugin.onScenarioEnd()
               → resetea el cliente HTTP
               → listo para el siguiente escenario
```

### Los componentes del motor (paquete `common/runtime/`)

| Clase | Función en palabras simples |
|-------|----------------------------|
| `CucumberRuntimeEngine` | El director: orquesta la ejecución de escenarios |
| `ExecutionContext` | El "tablero de control" de un escenario: guarda todo lo que pasa durante su ejecución |
| `ServiceRegistry` | El "casillero de servicios": guarda y entrega los objetos que los steps necesitan |
| `VariableStore` | El "cuaderno de notas": almacena variables que se pasan entre steps |
| `CorePlugin` | La "interfaz del músico": todo plugin que quiera participar debe implementarla |
| `StepComponent` | La "ficha técnica" de un grupo de steps: nombre, fase BDD, descripción |
| `DefaultLifecycleManager` | Gestiona el ciclo de vida de los plugins en orden correcto |
| `StepDiscoveryService` | Descubre automáticamente qué steps existen en las capas activas |

---

## 🚀 Cómo se usa — Quick Start

### Prerequisitos

- ☕ **Java 21** (versión mínima requerida)
- 🐘 **Gradle 8.x** (incluido via `./gradlew`)
- 🌐 **Git**

### 1. Clonar y compilar el framework

```bash
git clone https://github.com/avenero/qa-framework-core.git
cd qa-framework-core

# Compilar y publicar en Maven Local (~/.m2/repository)
./gradlew clean build publishToMavenLocal
```

### 2. Crear tu proyecto de pruebas

En tu proyecto de pruebas, agrega la dependencia en `build.gradle`:

```groovy
repositories {
    mavenLocal()    // Busca primero en tu máquina (el framework publicado)
    mavenCentral()  // Busca las demás librerías
}

dependencies {
    // Elige SOLO las capas que necesitas
    implementation 'com.qa:common:2.0.0'      // Siempre requerido
    implementation 'com.qa:api-core:2.0.0'    // Para pruebas de API REST
    implementation 'com.qa:web-core:2.0.0'    // Para pruebas de interfaz Web
    implementation 'com.qa:mobile-core:2.0.0'    // Para pruebas Mobile (Android + iOS)
}
```

### 3. Crear una prueba de API (ejemplo completo)

**`src/test/resources/features/api/login.feature`:**

```gherkin
# language: es
@api @smoke
Feature: Autenticación de usuarios

  Scenario: Login exitoso con credenciales válidas
    Given configuro endpoint con base "https://mi-sistema.com/" y path "api/auth/login"
    And agrego el header "Content-Type" con valor "application/json"
    And agrego el request
      """
      { "username": "admin", "password": "Admin@QA2026!" }
      """
    When ejecuto la consulta con el metodo "POST"
    Then valido que el codigo de respuesta del servicio sea 200
    And valido que el campo "$.accessToken" NO sea null
    And valido que el tiempo de respuesta sea menor a 3000 milisegundos

  Scenario: Login fallido con credenciales incorrectas
    Given configuro endpoint con base "https://mi-sistema.com/" y path "api/auth/login"
    And agrego el header "Content-Type" con valor "application/json"
    And agrego el request
      """
      { "username": "admin", "password": "contraseña_incorrecta" }
      """
    When ejecuto la consulta con el metodo "POST"
    Then valido que el codigo de respuesta del servicio sea 401
```

### 4. Crear una prueba Web (ejemplo completo)

```gherkin
# language: es
@web @smoke
Feature: Interfaz de login web

  Scenario: El formulario de login es accesible
    Given configuro el driver del navegador "chrome" en modo headless "true"
    And navego a la URL "https://mi-sistema.com/login"
    Then espero que el elemento "username" sea visible
    And espero que el elemento "password" sea visible
    And espero que el elemento "loginButton" sea visible

  Scenario: Login exitoso navega al dashboard
    Given configuro el driver del navegador "chrome" en modo headless "true"
    And navego a la URL "https://mi-sistema.com/login"
    When ingreso "admin" en el elemento "username"
    And ingreso "Admin@QA2026!" en el elemento "password"
    And hago clic en el elemento "loginButton"
    Then espero que el elemento "dashboardTitle" sea visible
    And el texto del elemento "dashboardTitle" debe contener "Dashboard"
```

### 5. Ejecutar las pruebas

```bash
# Ejecutar todos los tests
./gradlew test

# Ejecutar solo los @smoke
./gradlew test -Dcucumber.filter.tags="@smoke"

# Ejecutar solo los de API
./gradlew test -Dcucumber.filter.tags="@api"

# Ejecutar tests de una feature específica
./gradlew test -Dcucumber.features="src/test/resources/features/api/login.feature"
```

---

## 📦 Las 4 Capas Explicadas

### 🔧 [common](./common/README.md) — La Fundación

**¿Qué es?** La caja de herramientas que todas las demás capas usan. No hace pruebas por sí sola.

**Lo más importante que tiene:**
- El **motor de ejecución** (runtime/): el sistema de plugins que hace todo funcionar
- El **HTTP base**: modelo de petición/respuesta HTTP compartido
- El **sistema de logging**: para registrar todo lo que pasa durante las pruebas
- La **configuración**: lee archivos `.properties` y variables de entorno
- Las **utilidades**: JSON, texto, datos aleatorios, variables entre steps
- La **base de datos**: conectores para Oracle, PostgreSQL, MySQL, SQL Server

**Versión:** `com.qa:common:2.0.0`

📖 **[Ver documentación completa → common/README.md](./common/README.md)**

---

### 🌐 [api-core](./api-core/README.md) — Pruebas de API REST

**¿Qué es?** Todo lo necesario para probar servicios web REST. Si tu sistema expone una API HTTP, esta capa la prueba.

**Lo más importante que tiene:**
- **~92 steps en español** organizados en 13 clases
- **ApiPlugin**: se activa con `@api`, `@rest`, `@http`, `@service`
- **12 grupos de steps**: URL, Autenticación, Headers, Cookies, Parámetros, Body, Ejecución, Status Code, Body de Respuesta, Headers de Respuesta, Performance, Seguridad
- **ApiHelper**: fachada que conecta steps con el cliente HTTP y el validador

**Versión:** `com.qa:api-core:2.0.0`

**Ejemplo rápido:**
```gherkin
@api
Scenario: Verificar endpoint de salud
  Given configuro endpoint con base "https://mi-api.com/" y path "health"
  When ejecuto la consulta con el metodo "GET"
  Then valido que el codigo de respuesta del servicio sea 200
  And valido que el tiempo de respuesta sea menor a 1000 milisegundos
```

📖 **[Ver documentación completa → api-core/README.md](./api-core/README.md)**

---

### 💻 [web-core](./web-core/README.md) — Pruebas de Interfaz Web

**¿Qué es?** Todo lo necesario para controlar un navegador web y verificar que la interfaz funciona correctamente usando Selenium WebDriver.

**Lo más importante que tiene:**
- **~80 steps en español** organizados en 16 grupos
- **WebPlugin**: se activa con `@web`, `@ui`, `@browser`, `@selenium`
- **16 componentes**: BrowserConfig, Navegación, Frames, Ventanas, Click, Input, Select, Scroll, DragDrop, Alert, Waits, ElementValidation, PageValidation, TableValidation, Screenshot, WebEnvironment
- **WebHelper**: fachada que combina DriverManager + WaitUtils + ScreenshotUtils

**Versión:** `com.qa:web-core:2.0.0`

**Ejemplo rápido:**
```gherkin
@web
Scenario: El menú principal tiene los ítems correctos
  Given configuro el driver del navegador "chrome" en modo headless "true"
  And navego a la URL "https://mi-sistema.com"
  Then el elemento "menuInicio" debe estar visible
  And el elemento "menuProductos" debe estar visible
```

📖 **[Ver documentación completa → web-core/README.md](./web-core/README.md)**

---

### 📱 [mobile-core](./mobile-core/README.md) — Pruebas de Apps Móviles

**¿Qué es?** Todo lo necesario para controlar aplicaciones móviles en Android e iOS usando Appium 8+.

**Lo más importante que tiene:**
- **~80 steps en español** organizados en 10 componentes BDD
- **MobilePlugin**: se activa con `@mobile`, `@android`, `@ios`, `@appium`
- **Auto-descubrimiento de dispositivos**: `DeviceDiscoveryService` detecta emuladores (ADB) y simuladores (simctl) para presentarlos en el FE
- **Pool thread-safe**: `DevicePool` asigna dispositivos y puertos Appium únicos por ejecución paralela
- **MobileDriverManager**: ThreadLocal garantiza aislamiento total entre escenarios paralelos
- **GestureHelper**: W3C Actions API (Appium 8+) — sin TouchAction deprecado
- **ElementLocatorHelper**: estrategia de localización por prefijo (`~`, `id:`, `xpath:`, `text:`...) diseñada para entrenamiento de IA de sugerencias en el FE
- **AppiumServerManager**: health check automático + auto-start opt-in para desarrollo local

**Versión:** `com.qa:mobile-core:2.0.0`

**Ejemplo rápido:**
```gherkin
@android
Scenario: La app muestra la pantalla de login al iniciarse
  Given configuro el dispositivo movil como "ANDROID"
  Given configuro que la app se ejecute en un emulador
  Given lanzo la aplicacion
  Then deberia ver el texto "Iniciar sesion" en la pantalla
  And el elemento "~username_field" debe estar habilitado
  And tomo screenshot mobile
```

📖 **[Ver documentación completa → mobile-core/README.md](./mobile-core/README.md)**

---

## 🔧 Matriz de Tecnologías

| Tecnología | Versión | Propósito | Capa |
|------------|---------|-----------|------|
| **Java** | 21 LTS | Lenguaje de desarrollo | Todas |
| **Gradle** | 8.14 | Sistema de build | Todas |
| **Cucumber** | 7.18.0 | Motor BDD (Gherkin → Java) | Todas |
| **JUnit Platform** | 1.10.0 | Runner de tests | Todas |
| **Unirest** | 4.4.4 | HTTP Client (peticiones REST) | common, api-core |
| **Jackson** | 2.15.x | Serialización/deserialización JSON | common, api-core |
| **JsonPath** | 2.x | Navegar documentos JSON (`$.campo`) | api-core |
| **JSON Schema Validator** | 1.x | Validar estructura de JSON | api-core |
| **Logback / SLF4J** | 1.5.x | Sistema de logging | Todas |
| **AssertJ** | 3.24.x | Aserciones fluidas (fáciles de leer) | api-core |
| **Selenium WebDriver** | 4.27.0 | Automatizar navegadores | web-core |
| **WebDriverManager** | 5.x | Descarga drivers automáticamente | web-core |
| **Appium Java Client** | 8.6.0 | Automatizar apps móviles (W3C API) | mobile-core |
| **HikariCP** | 5.x | Pool de conexiones a BD | common |

### Navegadores soportados (web-core)

| Navegador | Soporte | Modo headless |
|-----------|---------|---------------|
| Chrome | ✅ Completo | ✅ Sí |
| Firefox | ✅ Completo | ✅ Sí |
| Edge | ✅ Completo | ✅ Sí |

### Plataformas móviles soportadas (mobile-core)

| Plataforma | Versión mínima | Framework | Soporte |
|------------|----------------|-----------|---------|
| Android | 8.0 (API 26) | UiAutomator2 | ✅ Estable |
| iOS | 14.0 | XCUITest | ✅ Estable (requiere macOS) |

---

## 🔌 Contrato con el Backend (API Pública del Core)

El Backend de CuAleon integra este Core como librería Java. Estas son las **5 clases públicas** que el Backend consume directamente, todas en `com.qa.common.runtime`:

| Clase | Rol | Descripción |
|-------|-----|-------------|
| `CucumberRuntimeEngine` | **Entry point** | `execute(ExecutionRequest) → ExecutionResult` |
| `ExecutionRequest` | **Input** | Feature paths, tags, variables de entorno, config |
| `ExecutionResult` | **Output** | Estado final, métricas, escenarios pasados/fallados |
| `StepDiscoveryService` | **Catálogo** | Lista todos los `StepComponent` disponibles por plugin |
| `EventSubscriber` | **Streaming** | Interfaz implementada por el adapter WebSocket del Backend |

### Flujo de integración

```
Backend recibe POST /executions
        │
        ▼
ExecutionRequest req = ExecutionRequest.builder()
    .featurePaths(List.of("classpath:features/login.feature"))
    .tags(List.of("@api", "@smoke"))
    .environmentVars(Map.of("BASE_URL", "https://qa.empresa.com"))
    .build();
        │
        ▼
CucumberRuntimeEngine engine = new CucumberRuntimeEngine();
engine.getEventBus().subscribe(webSocketAdapter);   // streaming tiempo real
ExecutionResult result = engine.execute(req);
        │
        ▼
Backend persiste result y retorna al Frontend
```

### Catálogo de steps para el Frontend

```java
StepDiscoveryService discovery = new StepDiscoveryService();
List<StepComponent> allSteps = discovery.discoverAll();
// → ApiPlugin (12 componentes) + WebPlugin (16) + MobilePlugin (10) + DatabasePlugin (3)
// → Total: 41 grupos de steps con metadatos para la paleta visual del FE
```

---

## 📊 Estado del Proyecto

**Versión actual:** 2.0.0  
**Última actualización:** Abril 2026

| Capa | Versión | Estado | Componentes | Build |
|------|---------|--------|-------------|-------|
| **common** | 2.0.0 | ✅ Estable | Runtime + DB (3 componentes) + Reporting | ✅ Verde |
| **api-core** | 2.0.0 | ✅ Estable | 12 componentes (~92 steps) | ✅ Verde |
| **web-core** | 2.0.0 | ✅ Estable | 16 componentes (~80 steps) | ✅ Verde |
| **mobile-core** | 2.0.0 | ✅ Estable | 10 componentes (~80 steps) | ✅ Verde |

### ¿Qué cambió de v1.x a v2.0?

La versión 2.0 fue un rediseño arquitectónico completo. Los cambios más importantes:

| Antes (v1.x) | Ahora (v2.0) |
|--------------|--------------|
| `ApiSteps.java` con 478 líneas mezclando todo | 13 clases de steps con responsabilidad única |
| Una clase `WebSteps.java` con todo | 16 clases de steps por función |
| Dependencia de Scotia/Scotiabank en paquetes | Paquetes genéricos `com.qa.*` |
| Sin motor de ejecución propio | `CucumberRuntimeEngine` con sistema de plugins |
| Servicios creados manualmente en cada step | `ServiceRegistry` con lazy initialization |
| Sin `VariableStore` central | `ExecutionContext.variables()` para todas las capas |
| Grupo `com.scotia.qa` | Grupo `com.qa` (independiente) |

### Publicación en GitHub Packages

El framework se publica automáticamente en GitHub Packages vía GitHub Actions.

```bash
# Para publicar manualmente en tu máquina:
./gradlew clean build publishToMavenLocal

# Para publicar en GitHub Packages (requiere token configurado):
./gradlew publishAllPublicationsToGitHubPackagesRepository
```

Para usar desde GitHub Packages en tu proyecto:

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/avenero/qa-framework-core")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

---

## ✅ Mejores Prácticas

### Organización de un proyecto de pruebas

```
qa-mi-proyecto/
├── src/test/
│   ├── java/com/mi/proyecto/
│   │   ├── runner/
│   │   │   └── RunCucumberTest.java      ← Configuración del runner
│   │   └── steps/
│   │       └── MisStepsEspecificos.java  ← Steps propios del proyecto
│   └── resources/
│       ├── features/
│       │   ├── api/
│       │   │   └── login-api.feature
│       │   └── web/
│       │       └── login-web.feature
│       └── config-app.properties         ← Configuración del proyecto
├── .env.local                            ← Variables de entorno (no commitear)
└── build.gradle
```

### Tags — cómo y cuándo usarlos

```gherkin
# Tag de CAPA (obligatorio): activa el plugin correcto
@api    → pruebas de API REST
@web    → pruebas de interfaz web
@mobile → pruebas de app móvil

# Tags de TIPO (opcionales): para filtrar ejecuciones
@smoke      → prueba rápida de que nada está roto
@regression → prueba completa del sistema
@security   → pruebas de seguridad

# Ejecutar solo @smoke de @api:
./gradlew test -Dcucumber.filter.tags="@api and @smoke"
```

### Variables entre steps

```gherkin
# Guardar un valor
Given genero un UUID y lo guardo como "idTransaccion"

# Usar el valor guardado (con ${...})
Given agrego el request
  """
  { "transactionId": "${idTransaccion}" }
  """

# O guardado desde una respuesta
And el resultado almaceno el valor que está dentro de la estructura "accessToken" en "token"

# Usar el token en el siguiente step
And agrego el token personalizado ${token}
```

### Configuración por ambiente

```properties
# config-app.properties
api.base.url=https://mi-sistema-qa.com/
web.base.url=https://mi-sistema-qa.com
web.browser=chrome
web.headless=true
```

```bash
# .env.local (NUNCA commitear — agregar a .gitignore)
API_KEY=mi-api-key-secreta
DB_PASSWORD=mi-password
```

---

## 🤝 Contribución

### Proceso para contribuir

1. Crea un branch desde `main`:
   ```bash
   git checkout -b feature/nueva-funcionalidad
   ```

2. Aplica los cambios siguiendo las convenciones del proyecto

3. Verifica que todo compila y los tests pasan:
   ```bash
   ./gradlew clean build
   ```

4. Crea un Pull Request hacia `main`

### Convenciones de código

| Elemento | Idioma | Ejemplo |
|----------|--------|---------|
| Nombres de clases y métodos | Inglés | `validateResponseStatus()` |
| Javadoc y comentarios | Español | `/** Valida el código de respuesta HTTP */` |
| Steps de Cucumber | Español | `Then valido que el codigo sea {int}` |
| Mensajes de log | Español | `"✅ Endpoint configurado"` |
| Mensajes de error | Español | `"Status esperado: 200, obtenido: 404"` |

### Conventional Commits

```bash
feat(api-core): agregar step para validar UUID
fix(web-core): corregir timeout en espera de elementos
docs(readme): actualizar guía de instalación
refactor(common): mejorar rendimiento de VariableStore
test(api-core): agregar tests para ValidationUtilities
```

### Antes de hacer commit

```bash
# 1. Todo compila
./gradlew clean build

# 2. Los tests unitarios pasan
./gradlew test

# 3. Generar reportes de cobertura
./gradlew jacocoTestReport

# 4. Ver reporte (macOS)
open common/build/reports/jacoco/test/html/index.html
```

---

## 📞 Soporte

- **GitHub Issues:** https://github.com/avenero/qa-framework-core/issues
- **Autor principal:** Abel Venero
- **Documentación detallada por capa:**
  - [common/README.md](./common/README.md)
  - [api-core/README.md](./api-core/README.md)
  - [web-core/README.md](./web-core/README.md)
  - [mobile-core/README.md](./mobile-core/README.md)

---

<div align="center">

**[⬆ Volver arriba](#cualeon-test-engineering-platform--core)**

CuAleon Test Engineering Platform — construido por el QA Engineering Team

</div>

