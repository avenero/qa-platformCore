# CuAleon Test Engineering Platform — Core

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Gradle](https://img.shields.io/badge/Gradle-8.14-blue.svg)](https://gradle.org/)
[![Selenium](https://img.shields.io/badge/Selenium-4.27.0-green.svg)](https://www.selenium.dev/)
[![Cucumber](https://img.shields.io/badge/Cucumber-7.18.0-brightgreen.svg)](https://cucumber.io/)
[![Version](https://img.shields.io/badge/version-2.0.2-blue.svg)](https://github.com/avenero/qa-platformCore)

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
- [Convención de IDs de Step](#-convención-de-ids-de-step)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Publicación en GitHub Packages](#-publicación-en-github-packages)
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
│   build.gradle: implementation 'com.qa:api-core:2.0.2'            │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ dependen de
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│               qa-platformCore (este repositorio)                     │
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
│  │ ~92 steps     │  │ ~80 steps   │  │ ~80 steps     │             │
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
- 🐘 **Gradle 8.14+** (incluido via `./gradlew`)
- 🌐 **Git**

### 1. Clonar y compilar el framework

```bash
git clone https://github.com/avenero/qa-platformCore.git
cd qa-platformCore

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
    implementation 'com.qa:common:2.0.2'        // Siempre requerido
    implementation 'com.qa:api-core:2.0.2'      // Para pruebas de API REST
    implementation 'com.qa:web-core:2.0.2'      // Para pruebas de interfaz Web
    implementation 'com.qa:mobile-core:2.0.2'   // Para pruebas Mobile (Android + iOS)
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

**Versión:** `com.qa:common:2.0.2`

📖 **[Ver documentación completa → common/README.md](./common/README.md)**

---

### 🌐 [api-core](./api-core/README.md) — Pruebas de API REST

**¿Qué es?** Todo lo necesario para probar servicios web REST. Si tu sistema expone una API HTTP, esta capa la prueba.

**Lo más importante que tiene:**
- **~92 steps en español** organizados en 13 clases
- **ApiPlugin**: se activa con `@api`, `@rest`, `@http`, `@service`
- **12 grupos de steps**: URL, Autenticación, Headers, Cookies, Parámetros, Body, Ejecución, Status Code, Body de Respuesta, Headers de Respuesta, Performance, Seguridad
- **ApiHelper**: fachada que conecta steps con el cliente HTTP y el validador

**Versión:** `com.qa:api-core:2.0.2`

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

**Versión:** `com.qa:web-core:2.0.2`

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

**Versión:** `com.qa:mobile-core:2.0.2`

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
| **Unirest** | 3.14.5 | HTTP Client (peticiones REST) | api-core |
| **Jackson** | 2.15.x | Serialización/deserialización JSON | common, api-core, mobile-core |
| **JsonPath** | 2.9.0 | Navegar documentos JSON (`$.campo`) | common, api-core |
| **JSON Schema Validator** | 1.0.87 | Validar estructura de JSON | api-core |
| **Logback / SLF4J** | 1.5.25 / 2.0.9 | Sistema de logging | Todas |
| **AssertJ** | 3.27.7 | Aserciones fluidas (fáciles de leer) | Todas |
| **Selenium WebDriver** | 4.27.0 | Automatizar navegadores | web-core |
| **Appium Java Client** | 8.6.0 | Automatizar apps móviles (W3C API) | mobile-core |
| **HikariCP** | 5.0.1 | Pool de conexiones a BD | common |
| **SpotBugs** | 4.8.6 | Detección estática de bugs | Build |
| **Checkstyle** | 10.21.0 | Análisis de estilo de código | Build |
| **JaCoCo** | 0.8.12 | Cobertura de código | Build |
| **ExtentReports** | 5.1.1 | Reportes HTML de ejecución | common, mobile-core |

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

### Bases de datos soportadas (common)

| BD | Driver | Versión |
|----|--------|---------|
| MySQL | `mysql-connector-j` | 8.4.0 |
| PostgreSQL | `postgresql` | 42.7.7 |
| SQL Server | `mssql-jdbc` | 12.8.2.jre11 |
| Oracle | `ojdbc11` | 23.2.0.0 |

---

## 🔌 Contrato con el Backend (API Pública del Core)

El Backend de CuAleon integra este Core como librería Java. Estas son las **5 clases públicas** que el Backend consume directamente, todas en `com.qa.common.runtime`:

| Clase | Rol | Descripción |
|-------|-----|-------------|
| `CucumberRuntimeEngine` | **Entry point** | `execute(ExecutionRequest)` o `execute(ExecutionRequest, List<ConcurrentEventListener>)` con listeners extras para step/scenario events |
| `ExecutionRequest` | **Input** | Feature paths, glue paths (auto via SPI), `ExecutionConfig` (browser, base.url, mobile, etc.) |
| `ExecutionResult` | **Output** | Estado final, métricas, escenarios pasados/fallados, duración |
| `ExecutionConfig` | **Config** | Configuración inmutable por ejecución: browser, base.url, web.headless, mobile.platform, etc. |
| `StepDiscoveryService` | **Catálogo** | Lista todos los `StepComponent` y `StepDefinitionInfo` disponibles por plugin |

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

## 🚦 CI/CD Pipeline

El pipeline corre automáticamente con cada push a `master` y se compone de dos jobs secuenciales.

### Flujo general

```
push → master
      │
      ▼
┌─────────────────────────────────────────────────────┐
│  JOB 1: quality  (ubuntu-latest)                    │
│                                                     │
│  1. Checkout (completo)                             │
│  2. JDK 21 Temurin                                  │
│  3. Cache Gradle                                    │
│  4. ./gradlew test jacocoTestReport --continue      │
│  5. ./gradlew checkstyleMain --continue             │
│  6. ./gradlew spotbugsMain --continue               │
│  7. Upload artefactos (test-results, jacoco,        │
│     static-analysis) → retención 30 días            │
└──────────────────────┬──────────────────────────────┘
                       │ solo si quality pasa
                       ▼
┌─────────────────────────────────────────────────────┐
│  JOB 2: publish  (ubuntu-latest)                    │
│                                                     │
│  1. Checkout                                        │
│  2. JDK 21 Temurin                                  │
│  3. Cache Gradle                                    │
│  4. ./gradlew publish --no-daemon                   │
│     → common, api-core, web-core, mobile-core       │
│        a GitHub Packages                            │
└─────────────────────────────────────────────────────┘
```

### Herramientas de calidad incluidas (sin dependencias externas)

| Herramienta | Qué analiza | Reporte generado |
|-------------|-------------|-----------------|
| **JaCoCo** | Cobertura de código (líneas, ramas, métodos) | XML + HTML navegable |
| **Checkstyle** | Estilo de código (línea máx. 120, imports, javadoc) | XML por módulo |
| **SpotBugs** | Bugs potenciales y vulnerabilidades (solo HIGH) | XML por módulo |

> **Nota:** El análisis de calidad es completamente self-contained — no requiere cuentas en servicios externos. Todos los reportes quedan disponibles como artefactos descargables en cada ejecución del pipeline.

### Secretos requeridos

| Secret | Fuente | Para qué |
|--------|--------|----------|
| `GITHUB_TOKEN` | Inyectado automáticamente por GitHub Actions | Publicar en GitHub Packages |

---

## 📤 Publicación en GitHub Packages

Los artefactos se publican automáticamente al pasar el job `quality`. El repositorio es:

```
https://maven.pkg.github.com/avenero/qa-platformCore
```

### Módulos publicados

| Módulo | Coordenadas Maven | Publicado |
|--------|------------------|-----------|
| common | `com.qa:common:2.0.2` | ✅ Sí |
| api-core | `com.qa:api-core:2.0.2` | ✅ Sí |
| web-core | `com.qa:web-core:2.0.2` | ✅ Sí |
| mobile-core | `com.qa:mobile-core:2.0.2` | ✅ Sí |

### Consumir desde tu proyecto

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/avenero/qa-platformCore")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation 'com.qa:common:2.0.2'
    implementation 'com.qa:api-core:2.0.2'      // Solo si necesitas pruebas de API
    implementation 'com.qa:web-core:2.0.2'      // Solo si necesitas pruebas Web
    implementation 'com.qa:mobile-core:2.0.2'   // Solo si necesitas pruebas Mobile
}
```

### Publicar localmente (desarrollo)

```bash
# Publicar en Maven Local (~/.m2/repository)
./gradlew clean build publishToMavenLocal

# Publicar a GitHub Packages manualmente (requiere GITHUB_ACTOR y GITHUB_TOKEN)
./gradlew publish --no-daemon
```

> **Importante:** GitHub Packages Maven **no permite sobreescribir versiones** ya publicadas.
> Si recibes un error `409 Conflict`, debes incrementar la versión en `build.gradle` antes de volver a publicar.

---

## 📊 Estado del Proyecto

**Versión actual:** 2.0.2
**Última actualización:** Abril 2026

| Capa | Versión | Estado | Componentes | Build |
|------|---------|--------|-------------|-------|
| **common** | 2.0.2 | ✅ Estable | Runtime + DB (3 componentes) + Reporting | ✅ Verde |
| **api-core** | 2.0.2 | ✅ Estable | 12 componentes (~92 steps) | ✅ Verde |
| **web-core** | 2.0.2 | ✅ Estable | 16 componentes (~80 steps) | ✅ Verde |
| **mobile-core** | 2.0.2 | ✅ Estable | 10 componentes (~80 steps) | ✅ Verde |

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

### ¿Qué cambió de v2.0.0 a v2.0.2?

| Área | Cambio |
|------|--------|
| **Repositorio** | Renombrado de `qa-framework-core` a `qa-platformCore` |
| **GitHub Packages URL** | Actualizada a `maven.pkg.github.com/avenero/qa-platformCore` |
| **api-core** | Ahora se publica correctamente en GitHub Packages |
| **Gradle 9.0** | Eliminadas todas las APIs deprecated (`tasks.withType`, `tasks.matching`, acceso eager a tareas) |
| **CI/CD** | Removido SonarCloud (análisis de calidad ahora completamente self-contained con JaCoCo + Checkstyle + SpotBugs) |
| **mobile-core** | Consolidado bloque `dependencies {}` duplicado |
| **build.gradle** | `rootProject.name` actualizado a `qa-platformCore` |

---

## ✅ Mejores Prácticas

### Flujo completo con ejemplo: CReacion de un Steps de API

```
┌──────────────────────────────────────────────────────────────────────────┐
│  CORE — Definición del Step                                              │
│                                                                          │
│  api-core/src/main/java/com/qa/apicore/                                 │
│                                                                          │
│  1. ApiPlugin.java (CorePlugin SPI)                                      │
│     ├─ getId() → "api"                                                   │
│     ├─ getActivationTags() → [@api, @rest, @http, @service]             │
│     ├─ getOrder() → 50                                                   │
│     ├─ registerServices(ServiceRegistry) → registra:                     │
│     │   ├─ HttpClient.class → lazy → HttpClientFactory.create()          │
│     │   ├─ AuthenticationService.class → lazy → AuthServiceFactory...    │
│     │   └─ ApiHelper.class → lazy → new ApiHelper(httpClient, ...)      │
│     ├─ onScenarioStart(ctx) → httpClient.reset() (limpia estado)        │
│     ├─ onScenarioEnd(ctx) → httpClient.cleanup()                        │
│     └─ getComponents() → [ApiUrlComponent, ApiAuthComponent, ...]       │
│                                                                          │
│  2. ApiUrlComponent.java (StepComponent — metadata)                      │
│     ├─ getId() → "api.url"                                              │
│     ├─ getName() → "URL & Ambiente"                                      │
│     ├─ getPhase() → GIVEN                                               │
│     ├─ getCategory() → "Configuracion"                                  │
│     ├─ getDisplayNameByLocale() → {es:"URL y Ambiente", en:"URL..."}    │
│     └─ getStepDefinitionClass() → UrlConfigSteps.class                  │
│                                                                          │
│  3. UrlConfigSteps.java (Cucumber step definitions)                      │
│     ├─ @Given("configuro el endpoint {string}")                         │
│     │   @StepId("api.url.configureEndpoint")                            │
│     │   public void configureEndpoint(String endpoint) {                │
│     │       ApiHelper api = ExecutionContext.current()                   │
│     │           .service(ApiHelper.class);  ← del ServiceRegistry       │
│     │       api.setEndpoint(endpoint);                                  │
│     │   }                                                                │
│     ├─ @Given("configuro la URL base {string}")                         │
│     │   @StepId("api.url.configureBaseUrl")                             │
│     │   ...                                                              │
│     └─ (7 métodos total)                                                │
│                                                                          │
│  META-INF/services/com.qa.common.runtime.CorePlugin                     │
│     → com.qa.apicore.plugin.ApiPlugin                                   │
└──────────────────────────────────────────────────────────────────────────┘
                            │ SPI Discovery
                            ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  BACKEND — Descubrimiento y Catálogo                                     │
│                                                                          │
│  Al arrancar Spring Boot:                                                │
│                                                                          │
│  1. CoreEngineConfig.coreRuntimeEngine()                                │
│     → CucumberRuntimeEngine.withServiceLoader()                         │
│     → ServiceLoader.load(CorePlugin.class) encuentra:                   │
│       [DatabasePlugin(0), ApiPlugin(50), WebPlugin(100), Mobile(150)]   │
│                                                                          │
│  2. CoreEngineConfig.coreStepDiscoveryService(engine)                   │
│     → engine.getDiscoveryService()                                      │
│     → StepDiscoveryService con 4 plugins y ~250 steps                   │
│                                                                          │
│  3. StepDiscoveryAdapter.@PostConstruct.buildCatalog()                  │
│     → coreDiscovery.discoverAllStepDefs()                               │
│     → Escanea UrlConfigSteps.class vía reflexión                        │
│     → Encuentra @Given("configuro el endpoint {string}")                │
│       + @StepId("api.url.configureEndpoint")                            │
│     → Crea StepInfo:                                                    │
│       { stepId: "api.url.configureEndpoint",                            │
│         pattern: "configuro el endpoint {string}",                      │
│         phase: "GIVEN", layer: "API",                                   │
│         componentId: "api.url",                                         │
│         componentName: "URL & Ambiente",                                │
│         parameters: [{name:"endpoint", type:"string", required:true}],  │
│         displayNameByLocale: {es:"URL y Ambiente", en:"URL & Env"} }    │
│                                                                          │
│  4. StepCatalogController → GET /api/catalog/steps                      │
│     → Retorna lista de StepInfo al Frontend                             │
└──────────────────────────────────────────────────────────────────────────┘
                            │ REST API
                            ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  FRONTEND — UI del Catálogo y Scenario Builder                           │
│                                                                          │
│  1. StepPalette.tsx                                                      │
│     → useQuery(stepsApi.getComponentsWithSteps('api'))                  │
│     → Renderiza steps agrupados por componente:                         │
│                                                                          │
│     ┌─ URL & Ambiente (GIVEN) ──────────────────────┐                   │
│     │  ◆ configuro el endpoint {string}              │                   │
│     │  ◆ configuro la URL base {string}              │                   │
│     └────────────────────────────────────────────────┘                   │
│                                                                          │
│  2. Usuario arrastra "configuro el endpoint {string}"                   │
│     → ScenarioCanvas agrega ScenarioStep:                               │
│       { stepId: "api.url.configureEndpoint",                            │
│         pattern: "configuro el endpoint {string}",                      │
│         phase: "GIVEN", layer: "API",                                   │
│         componentId: "api.url",                                         │
│         parameters: { param1: "/api/v1/users" } }   ← inline editing   │
│                                                                          │
│  3. Usuario hace clic "Guardar"                                         │
│     → POST /api/features/{id}/scenarios                                 │
│       { name: "Login exitoso",                                          │
│         tags: ["@api", "@smoke"],                                       │
│         steps: [                                                         │
│           { stepId: "api.url.configureEndpoint",                        │
│             phase: "GIVEN", layer: "API",                               │
│             parameters: { param1: "/api/v1/users" } },                  │
│           ...                                                            │
│         ] }                                                              │
│                                                                          │
│  4. Usuario hace clic "Ejecutar"                                        │
│     → Selecciona ambiente (Environment con baseUrl)                     │
│     → POST /api/executions                                              │
│       { projectId, environmentId, scenarioIds: [uuid],                  │
│         layer: "api" }                                                   │
└──────────────────────────────────────────────────────────────────────────┘
                            │ POST /api/executions
                            ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  BACKEND — Generación de Gherkin y Ejecución                             │
│                                                                          │
│  1. ExecutionController.launch()                                        │
│     → TestExecutionService.launchExecution()                            │
│       → Crea Execution(PENDING) + triggeredBy(user)                     │
│       → asyncRunner.run(executionId, request)                           │
│       → HTTP 202 Accepted                                               │
│                                                                          │
│  2. ExecutionAsyncRunner.run() [@Async]                                 │
│     → Status → RUNNING, WS → EXECUTION_STARTED                         │
│     → FeatureGeneratorService.generate():                               │
│       │  Para cada step del escenario:                                  │
│       │   stepResolutionService.resolve("api.url.configureEndpoint")    │
│       │   → StepDiscoveryPort.findById("api.url.configureEndpoint")    │
│       │   → pattern: "configuro el endpoint {string}"                   │
│       │  GherkinStepRenderer.resolve(pattern, {param1: "/api/v1..."})   │
│       │   → 'configuro el endpoint "/api/v1/users"'                     │
│       │                                                                  │
│       └→ Genera:                                                        │
│          Feature: Ejecución abc-123                                     │
│            @api @smoke                                                   │
│            Scenario: Login exitoso                                       │
│              Given configuro el endpoint "/api/v1/users"                │
│              When envío la petición "POST"                               │
│              Then el código de respuesta es 200                          │
│                                                                          │
│  3. buildExecutionConfig():                                             │
│     → ExecutionConfig { environment: "QA",                              │
│         properties: { "base.url": "https://api-qa.example.com" } }     │
│                                                                          │
│  4. coreExecutionBridge.execute(feature, config, eventCallback)         │
└──────────────────────────────────────────────────────────────────────────┘
                            │ Core Engine
                            ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  CORE — Ejecución del Step                                               │
│                                                                          │
│  1. CucumberRuntimeEngine.execute(request, [stepBridge])                │
│     → LifecycleManager.initialize(request)                              │
│       → Crea ExecutionContext:                                          │
│         ├─ ExecutionConfig (con base.url, etc.)                         │
│         ├─ ServiceRegistry (vacío, se llenará por plugins)              │
│         ├─ VariableStore (vacío)                                        │
│         └─ EventBus                                                     │
│       → ExecutionContext se activa en ThreadLocal                        │
│                                                                          │
│  2. Cucumber Runtime ejecuta el .feature                                │
│     → ScenarioLifecycleBridge recibe TestCaseStarted                    │
│       → Extrae tags del escenario: [@api, @smoke]                       │
│       → LifecycleManager.resolveActivePlugins(tags)                     │
│         → @api coincide con ApiPlugin.activationTags → ACTIVO           │
│         → @web NO coincide → WebPlugin INACTIVO                         │
│       → ApiPlugin.onScenarioStart(context):                            │
│         → ServiceRegistry ya tiene HttpClient, ApiHelper (lazy)         │
│         → httpClient.reset() limpia estado previo                       │
│                                                                          │
│  3. Cucumber ejecuta: Given configuro el endpoint "/api/v1/users"       │
│     → UrlConfigSteps.configureEndpoint("/api/v1/users")                │
│     → ExecutionContext.current() → el ThreadLocal activo                │
│     → ctx.service(ApiHelper.class) → lazy init:                        │
│       → HttpClientFactory.create() → BaseHttpClient (Unirest)          │
│       → BaseHttpClient lee base.url de ConfigManager:                   │
│         → ConfigManager.get("base.url")                                │
│         → Prioridad: ExecutionContext config > System > env > file      │
│         → Retorna "https://api-qa.example.com" del ExecutionConfig     │
│     → api.setEndpoint("/api/v1/users")                                 │
│       → host = "https://api-qa.example.com/api/v1/users"               │
│                                                                          │
│  4. StepEventBridge captura TestStepFinished                            │
│     → Emite StepEvent al callback                                      │
│     → ExecutionAsyncRunner lo recibe                                    │
│     → notificationPort.notifyStepFinished()                            │
│     → WebSocket → /topic/executions/{id}                               │
│     → FE actualiza en real-time                                        │
│                                                                          │
│  5. ScenarioLifecycleBridge recibe TestCaseFinished                     │
│     → ApiPlugin.onScenarioEnd(context):                                │
│       → httpClient.cleanup() libera conexiones                          │
│     → ServiceRegistry.destroyAll() si hay AutoCloseable                 │
│                                                                          │
│  6. LifecycleManager.shutdown(context)                                  │
│     → ExecutionContext desactivado del ThreadLocal                       │
│     → Recursos liberados                                                │
└──────────────────────────────────────────────────────────────────────────┘
```

### FLUJO: Step de Web (Selenium)

```
Diferencias clave vs API:

1. PLUGIN ACTIVACIÓN:
   Tags: @web, @ui, @browser, @selenium
   WebPlugin.registerServices(registry):
     → WebHelper.class → lazy → new WebHelper(driverManager, waitUtils, ...)
   
   WebPlugin.onScenarioStart(ctx):
     → Lee config: web.browser = "chrome", web.headless = "true"
     → WebDriverFactory.createDriver(browser, headless)
       → WebDriverManager.chromedriver().setup()  (auto-descarga binario)
       → new ChromeDriver(chromeOptions)           (headless si config dice)
     → DriverManager.setDriver(driver)             (ThreadLocal)
   
   WebPlugin.onScenarioEnd(ctx):
     → DriverManager.quitDriver()                  (cierra browser)

2. STEP EJEMPLO — NavigationSteps.java:
   @Given("navego a la URL {string}")
   @StepId("web.navigation.navigateToUrl")
   public void navigateToUrl(String url) {
       WebHelper web = ExecutionContext.current().service(WebHelper.class);
       web.navigateTo(url);
       // → DriverManager.getDriver().get(url)
   }

3. CONFIG BRIDGE (BE → Core):
   ExecutionConfig {
     browser: "chrome",
     properties: {
       "web.browser": "chrome",
       "web.headless": "true",
       "base.url": "https://web-qa.example.com"
     }
   }
```


###FLUJO: Step de Mobile (Appium)

```
Diferencias clave vs Web:

1. PLUGIN ACTIVACIÓN:
   Tags: @mobile, @ios, @android, @appium
   MobilePlugin.registerServices(registry):
     → MobileDriverFactory.class → lazy → new MobileDriverFactory(ctx)
     → MobileHelper.class → lazy → new MobileHelper(factory, ...)
   
   MobilePlugin.onScenarioStart(ctx):
     → Lee config: mobile.platform = "ANDROID"
     → MobileDriverFactory.getOrCreateDriver()
       → Lee mobile.device.id, mobile.appium.server.url, mobile.app.path
       → UiAutomator2Options options = new UiAutomator2Options()
       → options.setDeviceName(deviceId)
       → options.setApp(appPath)
       → new AndroidDriver(new URL(appiumUrl), options)
     → MobileDriverManager.setDriver(driver) (ThreadLocal)
   
   MobilePlugin.onScenarioEnd(ctx):
     → MobileDriverManager.quitDriver() (cierra sesión Appium)
     → DevicePool.release(deviceId)       (libera dispositivo)

2. STEP EJEMPLO — GestureSteps.java:
   @When("hago tap en el elemento {string}")
   @StepId("mobile.gesture.tapElement")
   public void tapElement(String locator) {
       MobileHelper mobile = ExecutionContext.current()
           .service(MobileHelper.class);
       mobile.tap(locator);
       // → ElementLocatorHelper.find(locator) con estrategia module-first:
       //   "~accessibilityId" → AppiumBy.accessibilityId(...)
       //   "id:com.app/btn"   → AppiumBy.id(...)
       //   "xpath://..."      → AppiumBy.xpath(...)
       // → GestureHelper.tap(element) via W3C Actions API
   }

3. CONFIG BRIDGE (BE → Core):
   ExecutionConfig {
     properties: {
       "mobile.platform": "ANDROID",
       "mobile.device.id": "emulator-5554",
       "mobile.appium.server.url": "http://127.0.0.1:4723",
       "mobile.app.path": "/path/to/app.apk",
       "base.url": "https://api-qa.example.com"  (para steps API mixtos)
     }
   }

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

1. Crea un branch desde `master`:
   ```bash
   git checkout -b feature/nueva-funcionalidad
   ```

2. Aplica los cambios siguiendo las convenciones del proyecto

3. Verifica que todo compila y los tests pasan:
   ```bash
   ./gradlew clean build
   ```

4. Crea un Pull Request hacia `master`

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

# 5. Ver reporte Checkstyle
open common/build/reports/checkstyle/main.html

# 6. Ver reporte SpotBugs
open common/build/reports/spotbugs/main.html
```

---

## 🏷️ Convención de IDs de Step

El **`stepId`** es el identificador estable que conecta los componentes del Core con el Backend y el Frontend. Es el contrato que permite al Backend resolver un componente por nombre, persistir escenarios y ejecuciones, y al Frontend construir la paleta visual del Scenario Builder.

### Formato

```
{capa}.{dominio}[.{subdominio}]
```

| Capa | Prefijo | Módulo |
|------|---------|--------|
| API REST | `api.` | `api-core` |
| Web UI | `web.` | `web-core` |
| Mobile | `mobile.` | `mobile-core` |
| Base de Datos | `db.` | `common/database` |

### Tabla de todos los IDs del framework (v2.1.0)

**api-core (12):**

| `stepId` | Fase | Propósito |
|----------|------|-----------|
| `api.url` | GIVEN | URL base y ambiente |
| `api.authentication` | GIVEN | Auth: Bearer, Basic, API Key, OAuth |
| `api.headers` | GIVEN | Cabeceras HTTP |
| `api.cookies` | GIVEN | Cookies |
| `api.parameters` | GIVEN | Query y path params |
| `api.body` | GIVEN | Cuerpo de la petición |
| `api.execution` | WHEN | Ejecutar petición HTTP |
| `api.status` | THEN | Validar código de estado |
| `api.response.body` | THEN | Validar cuerpo de respuesta |
| `api.response.headers` | THEN | Validar cabeceras de respuesta |
| `api.performance` | THEN | Validar tiempos de respuesta |
| `api.security` | THEN | Validaciones de seguridad |

**web-core (16):**

| `stepId` | Fase | Propósito |
|----------|------|-----------|
| `web.browser.config` | GIVEN | Configurar navegador |
| `web.environment` | GIVEN | Variables de ambiente |
| `web.navigation` | GIVEN/WHEN | Navegar URLs |
| `web.click` | WHEN | Clic sobre elementos |
| `web.input` | WHEN | Escribir en campos |
| `web.select` | WHEN | Seleccionar en dropdowns |
| `web.scroll` | WHEN | Scroll en página |
| `web.dragdrop` | WHEN | Arrastrar y soltar |
| `web.frame` | WHEN | Cambiar a iframes |
| `web.window` | WHEN | Gestión de tabs/ventanas |
| `web.alert` | WHEN | Alertas del navegador |
| `web.wait` | WHEN | Esperas explícitas |
| `web.screenshot` | THEN | Captura de pantalla |
| `web.validation.element` | THEN | Validar elementos |
| `web.validation.page` | THEN | Validar página |
| `web.validation.table` | THEN | Validar tablas HTML |

**mobile-core (10):**

| `stepId` | Fase | Propósito |
|----------|------|-----------|
| `mobile.device.config` | GIVEN | Configurar dispositivo |
| `mobile.app.management` | GIVEN/WHEN | Gestionar ciclo de vida de la app |
| `mobile.permissions` | GIVEN | Permisos del SO |
| `mobile.sensor` | GIVEN/WHEN | GPS, red, modo avión |
| `mobile.gesture` | WHEN | Gestos táctiles |
| `mobile.element` | WHEN/THEN | Interactuar con elementos nativos |
| `mobile.context` | WHEN/THEN | Cambiar contexto nativo/WebView |
| `mobile.notification` | WHEN/THEN | Notificaciones push |
| `mobile.validation` | THEN | Validar elementos mobile |
| `mobile.validation.app-state` | THEN | Validar estado de la app |

**common/database (3):**

| `stepId` | Fase | Propósito |
|----------|------|-----------|
| `db.setup` | GIVEN | Conectar a la base de datos |
| `db.execution` | WHEN | Ejecutar queries SQL |
| `db.validation` | THEN | Validar resultados de queries |

### Regla de oro

> **Nunca cambies un `stepId` sin deprecar primero el ID anterior.** El Backend lo persiste. Si lo cambias sin avisar, los escenarios guardados en la BD apuntarán a un componente inexistente.

```java
// Ciclo de deprecación correcto:
@StepId(value = "api.old.url", deprecated = true, replacedBy = "api.url")
public class ApiUrlComponent implements StepComponent { ... }
```

Para el detalle completo ver [common/README.md — Convención de IDs de Step](./common/README.md#14-convención-de-ids-de-step).

---

## 📞 Soporte

- **GitHub Issues:** https://github.com/avenero/qa-platformCore/issues
- **Autor principal:** Abel Venero
- **Documentación detallada por capa:**
  - [common/README.md](./common/README.md)
  - [api-core/README.md](./api-core/README.md)
  - [web-core/README.md](./web-core/README.md)
  - [mobile-core/README.md](./mobile-core/README.md)
  - [config/README.md](./config/README.md)

---

<div align="center">

**[⬆ Volver arriba](#cualeon-test-engineering-platform--core)**

CuAleon Test Engineering Platform — construido por el QA Engineering Team

</div>
