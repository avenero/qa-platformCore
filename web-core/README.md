 # 💻 web-core — Capa de Pruebas de Interfaz Web

> **Versión:** 2.0.0 | **Grupo:** `com.qa` | **Artefacto:** `web-core`  
> **Última actualización:** Abril 2026  
> **Autor:** Abel Venero

---

## 📑 Índice

1. [¿Qué es web-core en palabras simples?](#1-qué-es-web-core-en-palabras-simples)
2. [Conceptos clave antes de empezar](#2-conceptos-clave-antes-de-empezar)
3. [El lugar de web-core en el framework](#3-el-lugar-de-web-core-en-el-framework)
4. [Mapa completo del módulo](#4-mapa-completo-del-módulo)
5. [Las zonas de la arquitectura](#5-las-zonas-de-la-arquitectura)
   - 5.1 [La Puerta de Entrada — WebPlugin](#51-la-puerta-de-entrada--webplugin)
   - 5.2 [Los 16 Componentes de Steps](#52-los-16-componentes-de-steps)
   - 5.3 [Las Clases de Steps — organizadas por función](#53-las-clases-de-steps--organizadas-por-función)
   - 5.4 [Las Herramientas — utils/](#54-las-herramientas--utils)
   - 5.5 [El Driver Manager](#55-el-driver-manager)
6. [Estrategia Module-First para locators](#6-estrategia-module-first-para-locators)
7. [Catálogo de Steps por Categoría](#7-catálogo-de-steps-por-categoría)
8. [Flujo completo de una prueba Web](#8-flujo-completo-de-una-prueba-web)
9. [Ejemplos prácticos](#9-ejemplos-prácticos)
10. [Configuración](#10-configuración)
11. [Patrones de diseño usados](#11-patrones-de-diseño-usados)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. ¿Qué es web-core en palabras simples?

Imagina que necesitas verificar que el formulario de login de tu sistema web funciona correctamente: que cuando escribes el usuario y la contraseña y presionas el botón, el sistema te lleva al dashboard. Para hacer eso manualmente hay que abrir el navegador, escribir la URL, llenar los campos, hacer clic… y repetirlo cada vez que haya un cambio en el sistema.

**web-core** es el asistente que hace eso automáticamente. Puede:

- **Abrir un navegador** (Chrome, Firefox o Edge) y navegar a una URL
- **Encontrar elementos** en la página (campos, botones, menús)
- **Interactuar** con ellos (escribir texto, hacer clic, seleccionar opciones)
- **Verificar** que lo que aparece en pantalla es lo esperado
- **Tomar capturas de pantalla** cuando algo falla

Todo eso, siguiendo instrucciones escritas en español que cualquier persona puede entender:

```gherkin
@web
Scenario: Login exitoso navega al dashboard
  Given configuro el driver del navegador "chrome" en modo headless "true"
  And navego a la URL "https://mi-sistema.com/login"
  When ingreso "admin" en el elemento "usernameField"
  And ingreso "Admin@2026!" en el elemento "passwordField"
  And hago clic en el elemento "loginButton"
  Then espero que el elemento "dashboardTitle" sea visible
  And el texto del elemento "dashboardTitle" debe contener "Bienvenido"
```

---

## 2. Conceptos Clave Antes de Empezar

### 🌐 ¿Qué es Selenium WebDriver?

Selenium WebDriver es la librería que permite controlar un navegador web desde código Java. Es como tener una "mano virtual" que puede hacer todo lo que haría un usuario real en el navegador.

### 🎯 ¿Qué es un locator?

Un locator es la "dirección" de un elemento en la página web — le dice a Selenium dónde encontrarlo. Los locators más comunes son:
- **CSS Selector**: `#loginButton`, `.btn-primary`, `input[name='username']`
- **XPath**: `//button[contains(text(),'Iniciar sesión')]`
- **ID**: `loginButton` (si el elemento tiene `id="loginButton"`)

### 📦 ¿Qué es Module-First?

El framework usa un principio llamado "Module-First" que significa que **el framework no conoce los locators** de tu aplicación. Tu proyecto de pruebas es quien los define. El framework solo provee los steps genéricos. Esto hace que el framework sea completamente reutilizable para cualquier sistema.

### 🖥️ ¿Qué es headless?

Modo headless significa que el navegador se ejecuta **sin ventana visible**. Es útil en pipelines de CI/CD donde no hay pantalla. Para desarrollo local es mejor `"false"` para ver qué está haciendo el test.

---

## 3. El Lugar de web-core en el Framework

```
┌──────────────────────────────────────────────────────────────┐
│              qa-frameworks-core                               │
│                                                              │
│  ┌──────────┐  ┌─────────────────────────────────────────┐  │
│  │  common  │  │              web-core                   │  │
│  │          │◄─┤                                         │  │
│  │ Runtime  │  │  WebPlugin       WebHelper              │  │
│  │ Config   │  │  16 components   WaitUtils              │  │
│  │ Logging  │  │  Steps (7 pkg)   ScreenshotUtils        │  │
│  │ HTTP     │  │  DriverManager   WebDriverFactory       │  │
│  └──────────┘  └─────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
          ▲
          │ importa como librería
┌─────────────────────────────────────┐
│  Tu proyecto de pruebas             │
│  • features/*.feature (Gherkin)     │
│  • ComponentManager (tus locators)  │
│  • config-app.properties           │
└─────────────────────────────────────┘
```

**web-core aporta:**
- Los ~80 steps en español para controlar navegadores
- El `WebPlugin` que se activa con los tags `@web`, `@ui`, `@browser`, `@selenium`
- El `WebHelper` con toda la lógica de Selenium
- El `DriverManager` para gestionar el ciclo de vida del WebDriver

**web-core NO hace:**
- No conoce los locators de tu aplicación (eso lo define tu proyecto)
- No contiene Page Objects (esos van en tu proyecto)
- No prueba APIs (eso es `api-core`)
- No prueba apps móviles (eso es `mobile-core`)

---

## 4. Mapa Completo del Módulo

```
web-core/
└── src/main/java/com/qa/webcore/
    │
    ├── plugin/
    │   └── WebPlugin.java                 ← PUERTA DE ENTRADA: activa la capa Web
    │
    ├── components/bdd/                    ← CATÁLOGO DE 16 CAPACIDADES (metadatos)
    │   ├── BrowserConfigComponent.java    ← Configuración de navegador
    │   ├── WebEnvironmentComponent.java   ← Configuración de ambiente/URL base
    │   ├── NavigationComponent.java       ← Navegación (ir a URL, back, refresh)
    │   ├── FrameComponent.java            ← Manejo de iframes
    │   ├── WindowComponent.java           ← Manejo de ventanas/pestañas
    │   ├── ClickComponent.java            ← Clics en elementos
    │   ├── InputComponent.java            ← Escritura en campos de texto
    │   ├── SelectComponent.java           ← Selección en dropdowns
    │   ├── ScrollComponent.java           ← Scroll de página
    │   ├── DragDropComponent.java         ← Arrastrar y soltar
    │   ├── AlertComponent.java            ← Manejo de popups/alerts
    │   ├── WaitComponent.java             ← Esperas inteligentes
    │   ├── ElementValidationComponent.java ← Validar elementos (visible, texto, etc.)
    │   ├── PageValidationComponent.java   ← Validar la página (título, URL, etc.)
    │   ├── TableValidationComponent.java  ← Validar tablas HTML
    │   └── ScreenshotComponent.java       ← Captura de pantalla
    │
    ├── steps/                             ← LOS STEPS BDD (clases Java con @Given/@When/@Then)
    │   ├── WebHooksSteps.java             ← Cierre automático del navegador al terminar
    │   ├── WebVariableSteps.java          ← Guardar/usar variables de elementos web
    │   ├── config/
    │   │   ├── BrowserConfigSteps.java    ← GIVEN: configurar navegador y modo headless
    │   │   └── WebEnvironmentSteps.java   ← GIVEN: configurar URL base del ambiente
    │   ├── navigation/
    │   │   ├── NavigationSteps.java       ← WHEN: navegar a URL, back, refresh
    │   │   ├── FrameSteps.java            ← WHEN: entrar/salir de iframes
    │   │   └── WindowSteps.java           ← WHEN: cambiar entre ventanas/pestañas
    │   ├── interaction/
    │   │   ├── ClickSteps.java            ← WHEN: hacer clic en elementos
    │   │   ├── InputSteps.java            ← WHEN: escribir texto en campos
    │   │   ├── SelectSteps.java           ← WHEN: seleccionar opciones de dropdown
    │   │   ├── ScrollSteps.java           ← WHEN: hacer scroll
    │   │   ├── DragDropSteps.java         ← WHEN: arrastrar y soltar elementos
    │   │   └── AlertSteps.java            ← WHEN: aceptar/rechazar alerts/popups
    │   ├── wait/
    │   │   └── WaitSteps.java             ← WHEN: esperar condiciones en la página
    │   └── validation/
    │       ├── ElementValidationSteps.java ← THEN: validar elementos (visible, texto, etc.)
    │       ├── PageValidationSteps.java    ← THEN: validar título de página, URL, etc.
    │       ├── TableValidationSteps.java   ← THEN: validar contenido de tablas
    │       └── ScreenshotSteps.java        ← THEN: tomar captura de pantalla
    │
    ├── utils/
    │   ├── WebHelper.java                 ← ⭐ FACHADA CENTRAL: combina todo
    │   ├── WaitUtils.java                 ← Esperas inteligentes (Selenium FluentWait)
    │   └── ScreenshotUtils.java           ← Captura de pantalla y guardado
    │
    ├── driver/
    │   ├── DriverManager.java             ← Gestión thread-safe del WebDriver
    │   └── WebDriverFactory.java          ← Crea drivers para Chrome, Firefox, Edge
    │
    └── pages/                             ← Base de Page Objects (si el proyecto los usa)
```

---

## 5. Las Zonas de la Arquitectura

### 5.1 La Puerta de Entrada — WebPlugin

#### `WebPlugin.java`

Es la **puerta de entrada oficial** de toda la capa web-core al motor de ejecución. Se activa cuando el escenario tiene los tags `@web`, `@ui`, `@browser` o `@selenium`.

**¿Qué hace?**
1. **Registra** el servicio `WebHelper` en el `ServiceRegistry` (lazy — se crea solo cuando se necesita)
2. **Declara los 16 componentes** de steps con sus metadatos
3. **Gestiona el ciclo de vida**: al inicio del escenario no hace nada especial; al final cierra el navegador si estaba abierto

**Tags de activación:**
```gherkin
@web       ← tag principal de prueba web
@ui        ← alias (testing de interfaz de usuario)
@browser   ← alias (testing de navegador)
@selenium  ← alias (para tests de Selenium específicos)
```

**Orden de inicialización:** 100 (después de `ApiPlugin` que es 50)

### 5.2 Los 16 Componentes de Steps

Los 16 componentes se organizan en 4 grupos según su función:

#### 🔵 GIVEN — Configuración (2 componentes)

```
┌─────────────────────────────────────────────────────────────────┐
│  Configuran el entorno ANTES de navegar                         │
├──────────────────┬──────────────────────────────────────────────┤
│ 1. BrowserConfig │ ¿Qué navegador usar? ¿Headless o con UI?    │
│                  │ Chrome, Firefox, Edge — visible o invisible   │
├──────────────────┼──────────────────────────────────────────────┤
│ 2. WebEnvironment│ ¿Cuál es la URL base del ambiente?          │
│                  │ QA, Staging, Producción                       │
└──────────────────┴──────────────────────────────────────────────┘
```

#### 🟡 WHEN — Acción (9 componentes)

```
┌──────────────────────────────────────────────────────────────────┐
│  Ejecutan acciones en el navegador y la página                   │
├───────────────┬──────────────────────────────────────────────────┤
│ 3. Navigation │ Ir a URL, volver, actualizar, historial          │
├───────────────┼──────────────────────────────────────────────────┤
│ 4. Frame      │ Entrar a un iframe, salir, cambiar de frame      │
├───────────────┼──────────────────────────────────────────────────┤
│ 5. Window     │ Cambiar entre pestañas, cerrar ventana           │
├───────────────┼──────────────────────────────────────────────────┤
│ 6. Click      │ Clic, clic derecho, doble clic en elementos      │
├───────────────┼──────────────────────────────────────────────────┤
│ 7. Input      │ Escribir texto, limpiar campos, presionar teclas │
├───────────────┼──────────────────────────────────────────────────┤
│ 8. Select     │ Seleccionar opciones de dropdown (por texto/valor)│
├───────────────┼──────────────────────────────────────────────────┤
│ 9. Scroll     │ Scroll a elemento, al tope, al fondo de página   │
├───────────────┼──────────────────────────────────────────────────┤
│ 10. DragDrop  │ Arrastrar elemento A y soltarlo en elemento B    │
├───────────────┼──────────────────────────────────────────────────┤
│ 11. Alert     │ Aceptar, rechazar, leer texto de popups/alerts   │
├───────────────┼──────────────────────────────────────────────────┤
│ 12. Wait      │ Esperar N segundos o hasta que elemento aparezca │
└───────────────┴──────────────────────────────────────────────────┘
```

#### 🟢 THEN — Validación (4 componentes)

```
┌─────────────────────────────────────────────────────────────────┐
│  Verifican que la página muestra lo correcto                     │
├─────────────────────┬───────────────────────────────────────────┤
│ 13. ElementValidation│ ¿El elemento existe? ¿Tiene ese texto?  │
│                      │ ¿Está visible? ¿Está habilitado?         │
├─────────────────────┼───────────────────────────────────────────┤
│ 14. PageValidation  │ ¿El título de la página es correcto?     │
│                      │ ¿La URL contiene lo esperado?            │
├─────────────────────┼───────────────────────────────────────────┤
│ 15. TableValidation │ ¿La tabla tiene N filas? ¿La celda [x,y] │
│                      │ tiene ese texto?                          │
├─────────────────────┼───────────────────────────────────────────┤
│ 16. Screenshot      │ Tomar captura de pantalla en este momento │
└─────────────────────┴───────────────────────────────────────────┘
```

### 5.3 Las Clases de Steps — organizadas por función

Las clases de steps siguen las fases BDD:

```
steps/
├── config/         ← GIVEN: configuración inicial
├── navigation/     ← WHEN: navegar en el navegador
├── interaction/    ← WHEN: interactuar con elementos
├── wait/           ← WHEN: esperar condiciones
└── validation/     ← THEN: verificar resultados
```

**Antes de la versión 2.0** existía una sola clase `WebSteps.java` con todo mezclado. Ahora cada grupo de steps tiene su propia clase con una responsabilidad específica.

### 5.4 Las Herramientas — `utils/`

#### `WebHelper.java` — La Fachada Central ⭐

Es la clase más importante de web-core. Todos los steps le delegan el trabajo. Combina el `DriverManager` (el WebDriver), el `WaitUtils` (las esperas) y el `ScreenshotUtils` (las capturas) en un solo punto de acceso.

**¿Por qué existe WebHelper?**

| Sin WebHelper | Con WebHelper |
|---------------|---------------|
| Cada step maneja su propia espera | Las esperas están centralizadas |
| Cada step captura sus propias excepciones | El manejo de errores es uniforme |
| Código repetido en cada clase de step | Una sola implementación |

**Lo que hace WebHelper:**
- Localiza elementos en la página (busca en el `ComponentManager` del proyecto)
- Aplica esperas inteligentes antes de interactuar
- Registra en el log cada acción que realiza
- Toma capturas de pantalla automáticamente cuando falla algo

#### `WaitUtils.java` — Esperas Inteligentes

En vez de esperar un número fijo de segundos (lo que hace que los tests sean lentos o frágiles), `WaitUtils` espera **hasta que se cumpla una condición**:

```
WaitUtils.waitForVisible(elemento)
    │
    └── Intenta cada 500ms durante máximo 30 segundos
          Si aparece antes → continúa inmediatamente
          Si no aparece en 30s → lanza TimeoutException
```

Tipos de esperas disponibles:
- `waitForVisible(element)` — esperar que sea visible
- `waitForClickable(element)` — esperar que sea cliqueable
- `waitForText(element, text)` — esperar que tenga un texto específico
- `waitForInvisible(element)` — esperar que desaparezca

#### `ScreenshotUtils.java` — Capturas de Pantalla

Toma capturas de pantalla del estado actual del navegador y las guarda en `logs/web/`. Son fundamentales para el análisis de fallas: puedes ver exactamente qué había en pantalla cuando el test falló.

### 5.5 El Driver Manager

#### `DriverManager.java` — Gestión del WebDriver

Gestiona el ciclo de vida del driver de forma **thread-safe** usando `ThreadLocal`, lo que permite ejecutar tests en paralelo sin que los drivers se mezclen entre sí.

```
WebDriverFactory.createDriver("chrome", headless=true)
        │
        ├── Descarga ChromeDriver automáticamente (WebDriverManager)
        ├── Configura opciones: --headless, --no-sandbox, --window-size=1920x1080
        └── Retorna un ChromeDriver listo para usar

DriverManager.setDriver(driver)
        └── Guarda el driver en el ThreadLocal del thread actual

DriverManager.getDriver()
        └── Recupera el driver del thread actual (no el de otro thread)

DriverManager.quitDriver()
        └── Cierra el navegador y limpia el ThreadLocal
```

---

## 6. Estrategia Module-First para Locators

Esta es la decisión de diseño más importante de web-core: **el framework no conoce ningún locator**. Los locators son responsabilidad del proyecto de pruebas.

### ¿Por qué?

- El framework debe ser **reutilizable** para cualquier sistema web
- Los locators cambian con frecuencia; si estuvieran en el framework, cada cambio requeriría actualizar el framework
- Cada proyecto tiene sus propias convenciones de locators

### ¿Cómo funciona?

Cuando el framework ejecuta el step `hago clic en el elemento "loginButton"`, busca "loginButton" en un `ComponentManager` que el **proyecto de pruebas** debe proveer:

**En el proyecto de pruebas** (no en el framework):

```java
// src/test/java/com/mi/proyecto/components/ComponentManager.java
public class ComponentManager {
    
    private static final Map<String, By> locators = new HashMap<>();
    
    static {
        // Cada equipo define sus propios locators aquí
        locators.put("usernameField", By.id("username"));
        locators.put("passwordField", By.name("password"));
        locators.put("loginButton",   By.cssSelector("button[type='submit']"));
        locators.put("dashboardTitle", By.xpath("//h1[contains(@class,'title')]"));
        locators.put("errorMessage",  By.cssSelector(".alert-danger"));
    }
    
    public static By getLocator(String componentName) {
        By locator = locators.get(componentName);
        if (locator == null) {
            throw new RuntimeException("Locator no encontrado: " + componentName);
        }
        return locator;
    }
}
```

**En el feature** (usando nombres de locators, no los CSS/XPath reales):

```gherkin
When ingreso "admin" en el elemento "usernameField"
And hago clic en el elemento "loginButton"
```

**Ventajas:**
- ✅ El framework no cambia cuando cambia la UI
- ✅ Los locators están en un solo lugar
- ✅ Los features son legibles (hablan de "loginButton", no de `#btn-login-submit-v2`)

---

## 7. Catálogo de Steps por Categoría

### ⚙️ Configuración (`BrowserConfigSteps`, `WebEnvironmentSteps`)

| Step | Descripción |
|------|-------------|
| `Given configuro el driver del navegador {string} en modo headless {string}` | Inicia el navegador. Ej: `"chrome"` con headless `"true"` o `"false"` |
| `Given configuro el ambiente web {string}` | Establece la URL base del ambiente desde config (`web.baseurl.{ambiente}`) |
| `Given configuro la URL base web como {string}` | Establece la URL base directamente |

### 🧭 Navegación (`NavigationSteps`, `FrameSteps`, `WindowSteps`)

| Step | Descripción |
|------|-------------|
| `When navego a la URL {string}` | Navega a esa URL |
| `When actualizo URL en el navegador {string}` | Navega sin limpiar estado anterior |
| `When hago clic en el boton de atras del navegador` | Equivale a presionar "Atrás" |
| `When recargo la pagina` | Equivale a presionar F5 |
| `When entro al iframe {string}` | Cambia el foco al iframe especificado |
| `When salgo del iframe` | Vuelve al documento principal |
| `When cambio a la ventana {int}` | Cambia a la pestaña número N (0 = primera) |
| `When cierro la ventana actual` | Cierra la pestaña actual |

### 🖱️ Interacción (`ClickSteps`, `InputSteps`, `SelectSteps`, `ScrollSteps`, `DragDropSteps`, `AlertSteps`)

| Step | Descripción |
|------|-------------|
| `When hago clic en el elemento {string}` | Clic en el elemento |
| `When hago doble clic en el elemento {string}` | Doble clic en el elemento |
| `When hago clic derecho en el elemento {string}` | Clic derecho (abre menú contextual) |
| `When ingreso {string} en el elemento {string}` | Escribe el texto en el campo |
| `When limpio el elemento {string}` | Borra el contenido del campo |
| `When presiono la tecla {string} en el elemento {string}` | Presiona una tecla (Enter, Tab, etc.) |
| `When selecciono la opcion {string} del dropdown {string}` | Selecciona por texto visible |
| `When selecciono la opcion con valor {string} del dropdown {string}` | Selecciona por value del `<option>` |
| `When hago scroll hasta el elemento {string}` | Hace scroll hasta que el elemento sea visible |
| `When hago scroll al tope de la pagina` | Scroll al inicio de la página |
| `When hago scroll al fondo de la pagina` | Scroll al final de la página |
| `When arrastro {string} y lo suelto en {string}` | Drag & Drop |
| `When acepto el alert` | Hace clic en "Aceptar" del popup |
| `When cancelo el alert` | Hace clic en "Cancelar" del popup |
| `When ingreso {string} en el alert` | Escribe texto en un prompt (alert con input) |

### ⏱️ Esperas (`WaitSteps`)

| Step | Descripción |
|------|-------------|
| `When espero {int} segundos` | Espera fija (usar solo cuando sea necesario) |
| `When espero que el elemento {string} sea visible` | Espera hasta que el elemento aparezca |
| `When espero que el elemento {string} sea clickeable` | Espera hasta que pueda hacerse clic |
| `When espero que el elemento {string} desaparezca` | Espera hasta que el elemento se oculte |
| `When espero que el texto {string} aparezca en {string}` | Espera hasta que el elemento tenga ese texto |

### ✅ Validaciones (`ElementValidationSteps`, `PageValidationSteps`, `TableValidationSteps`)

| Step | Descripción |
|------|-------------|
| `Then el elemento {string} debe estar visible` | Falla si el elemento no es visible |
| `Then el elemento {string} debe estar habilitado` | Falla si el elemento está disabled |
| `Then el elemento {string} debe estar deshabilitado` | Falla si el elemento está enabled |
| `Then el elemento {string} debe estar seleccionado` | Para checkboxes y radio buttons |
| `Then el texto del elemento {string} debe ser {string}` | Valida texto exacto |
| `Then el texto del elemento {string} debe contener {string}` | Valida que contiene el texto |
| `Then el atributo {string} del elemento {string} debe ser {string}` | Valida un atributo HTML |
| `Then no debe existir el elemento {string}` | Falla si el elemento SÍ existe |
| `Then el titulo de la pagina debe ser {string}` | Valida el título de la pestaña |
| `Then la URL debe contener {string}` | Valida que la URL contiene ese texto |
| `Then la URL debe ser {string}` | Valida la URL completa |
| `Then la tabla {string} debe tener {int} filas` | Valida número de filas de una tabla |
| `Then la celda [{int},{int}] de la tabla {string} debe contener {string}` | Valida una celda específica |

### 📸 Screenshot (`ScreenshotSteps`)

| Step | Descripción |
|------|-------------|
| `Then tomo una captura de pantalla` | Guarda screenshot en el log |
| `Then tomo una captura de pantalla con nombre {string}` | Guarda con nombre personalizado |

### 💾 Variables (`WebVariableSteps`)

| Step | Descripción |
|------|-------------|
| `And guardo el texto del elemento {string} como {string}` | Extrae el texto y lo guarda para uso posterior |
| `And guardo el atributo {string} del elemento {string} como {string}` | Extrae un atributo y lo guarda |
| `And almaceno el valor {string} como {string}` | Guarda un valor literal como variable |

---

## 8. Flujo Completo de una Prueba Web

Sigamos el viaje de este escenario de principio a fin:

```gherkin
@web @smoke
Scenario: Login exitoso muestra el dashboard
  Given configuro el driver del navegador "chrome" en modo headless "true"
  And navego a la URL "https://mi-sistema.com/login"
  When ingreso "admin" en el elemento "usernameField"
  And ingreso "Admin@2026!" en el elemento "passwordField"
  And hago clic en el elemento "loginButton"
  Then espero que el elemento "dashboardTitle" sea visible
  And el texto del elemento "dashboardTitle" debe contener "Dashboard"
```

### Paso 0: El motor activa WebPlugin

`ScenarioExecutionHooks.@Before` detecta `@web` → activa `WebPlugin` → registra `WebHelper` en `ServiceRegistry`.

### Paso 1: `Given configuro el driver del navegador "chrome" en modo headless "true"`

```
BrowserConfigSteps.configurarDriver("chrome", "true")
    │
    ▼
WebHelper.configureBrowser("chrome", true)
    │
    ▼
WebDriverFactory.createDriver("chrome", headless=true)
    │
    ├── WebDriverManager descarga ChromeDriver compatible automáticamente
    ├── Configura ChromeOptions: --headless, --no-sandbox, etc.
    └── Retorna ChromeDriver listo
    │
    ▼
DriverManager.setDriver(chromeDriver)
    └── Guarda el driver en ThreadLocal del thread actual
    │
    ▼
Log: "✅ Driver configurado: chrome | headless: true"
```

### Paso 2: `And navego a la URL "https://mi-sistema.com/login"`

```
NavigationSteps.navegarAUrl("https://mi-sistema.com/login")
    │
    ▼
WebHelper.navigateTo("https://mi-sistema.com/login")
    │
    ▼
driver.get("https://mi-sistema.com/login")
    └── El navegador abre la página de login
    │
    ▼
Log: "✅ Navegando a: https://mi-sistema.com/login"
```

### Paso 3: `When ingreso "admin" en el elemento "usernameField"`

```
InputSteps.ingresoTextoEnElemento("admin", "usernameField")
    │
    ▼
WebHelper.typeText("usernameField", "admin")
    │
    ├── ComponentManager.getLocator("usernameField") → By.id("username")
    │
    ├── WaitUtils.waitForVisible(By.id("username"))
    │       Espera hasta 30s que el campo sea visible
    │
    ├── element.clear()                 → Limpia el campo
    ├── element.sendKeys("admin")       → Escribe "admin"
    │
    └── Log: "✅ Texto ingresado: 'admin' en 'usernameField'"
```

### Paso 4: `And hago clic en el elemento "loginButton"`

```
ClickSteps.hacerClicEnElemento("loginButton")
    │
    ▼
WebHelper.click("loginButton")
    │
    ├── ComponentManager.getLocator("loginButton") → By.cssSelector("button[type='submit']")
    ├── WaitUtils.waitForClickable(locator)  → Espera que sea cliqueable
    ├── element.click()                      → Hace el clic
    └── Log: "✅ Clic en: 'loginButton'"
    
    → El sistema procesa el login y navega al dashboard
```

### Paso 5: `Then espero que el elemento "dashboardTitle" sea visible`

```
WaitSteps.esperarElementoVisible("dashboardTitle")
    │
    ▼
WebHelper.waitForVisible("dashboardTitle")
    │
    ├── ComponentManager.getLocator("dashboardTitle") → By.xpath("//h1[contains(@class,'title')]")
    ├── WaitUtils.waitForVisible(locator)  → Espera hasta 30s
    └── El título aparece → continúa ✅
```

### Paso 6: `And el texto del elemento "dashboardTitle" debe contener "Dashboard"`

```
ElementValidationSteps.textoDebeContener("dashboardTitle", "Dashboard")
    │
    ▼
WebHelper.validateTextContains("dashboardTitle", "Dashboard")
    │
    ├── element.getText() → "Mi Dashboard Principal"
    ├── "Mi Dashboard Principal".contains("Dashboard") → true ✅
    └── Log: "✅ Texto validado: elemento 'dashboardTitle' contiene 'Dashboard'"
```

### Fin del escenario ✅

`WebHooksSteps.@After` → `DriverManager.quitDriver()` → Cierra el navegador → Listo para el siguiente escenario.

---

## 9. Ejemplos Prácticos

### Ejemplo 1: Prueba Cross-Browser

```gherkin
@web @smoke
Feature: Login funciona en todos los navegadores

  Scenario Outline: Login exitoso en <navegador>
    Given configuro el driver del navegador "<navegador>" en modo headless "true"
    And navego a la URL "https://mi-sistema.com/login"
    When ingreso "admin" en el elemento "usernameField"
    And ingreso "Admin@2026!" en el elemento "passwordField"
    And hago clic en el elemento "loginButton"
    Then espero que el elemento "dashboardTitle" sea visible

    Examples:
      | navegador |
      | chrome    |
      | firefox   |
      | edge      |
```

### Ejemplo 2: Formulario de Registro

```gherkin
@web @regression
Scenario: Registro de usuario nuevo
  Given configuro el driver del navegador "chrome" en modo headless "false"
  And navego a la URL "https://mi-sistema.com/registro"
  When ingreso "Juan Pérez" en el elemento "nombreCompleto"
  And ingreso "juan.perez@empresa.com" en el elemento "email"
  And ingreso "JuanPass@2026!" en el elemento "password"
  And selecciono la opcion "Chile" del dropdown "pais"
  And hago clic en el elemento "checkboxTerminos"
  And hago clic en el elemento "btnRegistrar"
  Then espero que el elemento "mensajeExito" sea visible
  And el texto del elemento "mensajeExito" debe contener "Registro exitoso"
```

### Ejemplo 3: Prueba Híbrida API + Web

```gherkin
@api @web @e2e
Scenario: Crear usuario por API y verificar en interfaz web
  # Crear usuario via API
  Given configuro endpoint con base "https://mi-sistema.com/" y path "api/users"
  And agrego el header "Content-Type" con valor "application/json"
  And agrego el request
    """
    { "nombre": "Test QA", "email": "test.qa@empresa.com", "rol": "viewer" }
    """
  When ejecuto la consulta con el metodo "POST"
  Then valido que el codigo de respuesta del servicio sea 201
  And el resultado almaceno el valor que está dentro de la estructura "id" en "nuevoUserId"

  # Verificar en la interfaz web de administración
  Given configuro el driver del navegador "chrome" en modo headless "true"
  And navego a la URL "https://mi-sistema.com/admin/usuarios"
  When ingreso "${nuevoUserId}" en el elemento "campoBusqueda"
  And hago clic en el elemento "btnBuscar"
  Then espero que el elemento "resultadoBusqueda" sea visible
  And el texto del elemento "emailUsuario" debe contener "test.qa@empresa.com"
```

### Ejemplo 4: Validar una Tabla de Datos

```gherkin
@web @regression
Scenario: La tabla de productos muestra los datos correctos
  Given configuro el driver del navegador "chrome" en modo headless "true"
  And navego a la URL "https://mi-sistema.com/productos"
  Then espero que el elemento "tablaProductos" sea visible
  And la tabla "tablaProductos" debe tener 5 filas
  And la celda [1,1] de la tabla "tablaProductos" debe contener "Producto A"
  And la celda [1,2] de la tabla "tablaProductos" debe contener "$10.000"
```

---

## 10. Configuración

### Dependencia en `build.gradle`

```groovy
dependencies {
    implementation 'com.qa:web-core:2.0.0'
    // common se incluye automáticamente
}
```

### Archivo de configuración del proyecto

**`src/test/resources/config-app.properties`:**

```properties
# Configuración Web
web.browser=chrome
web.headless=true
web.timeout=30
web.base.url=https://mi-sistema-qa.com

# URLs por ambiente (para el step "configuro el ambiente web")
web.baseurl.qa=https://mi-sistema-qa.com
web.baseurl.staging=https://mi-sistema-staging.com
web.baseurl.prod=https://mi-sistema.com
```

### Navegadores soportados

| Navegador | Valor en step | Modo headless | Requisito |
|-----------|---------------|---------------|-----------|
| Chrome | `"chrome"` | ✅ Sí | Chrome instalado (driver automático) |
| Firefox | `"firefox"` | ✅ Sí | Firefox instalado (driver automático) |
| Edge | `"edge"` | ✅ Sí | Edge instalado (driver automático) |

> Los drivers (ChromeDriver, GeckoDriver, EdgeDriver) se descargan **automáticamente** por WebDriverManager. No hay que instalar nada manualmente.

### Runner de Cucumber

```java
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME,
    value = "com.qa.webcore, com.qa.common, com.mi.proyecto.steps")
@ConfigurationParameter(key = Constants.FEATURES_PROPERTY_NAME,
    value = "src/test/resources/features")
public class RunCucumberTest {}
```

---

## 11. Patrones de Diseño Usados

| Patrón | Dónde | Para qué |
|--------|-------|----------|
| **Plugin / SPI** | `WebPlugin` + `META-INF/services/` | Auto-registro sin configuración manual |
| **Facade** | `WebHelper` | Un punto de acceso que oculta la complejidad de Selenium |
| **Factory** | `WebDriverFactory` | Crea drivers para diferentes navegadores |
| **ThreadLocal** | `DriverManager` | Ejecución en paralelo sin mezclar drivers |
| **Component / Metadata** | 16 clases `*Component` | Fichas técnicas usadas por el sistema de descubrimiento |
| **Strategy** | `WaitUtils` | Diferentes estrategias de espera según condición |

---

## 12. Troubleshooting

### ❌ El navegador no se abre / NullPointerException en el driver

**Causa:** Falta el tag `@web` en el escenario o feature.

**Solución:**
```gherkin
@web  ← Obligatorio para activar WebPlugin
Scenario: Mi test web
```

### ❌ NoSuchElementException — Elemento no encontrado

**Causa:** El locator no está definido en el `ComponentManager` del proyecto, o el elemento no aparece en el tiempo de espera.

**Solución:**
1. Verificar que existe en `ComponentManager.java`:
   ```java
   locators.put("miElemento", By.id("mi-id-real"));
   ```
2. Agregar un wait explícito:
   ```gherkin
   When espero que el elemento "miElemento" sea visible
   ```

### ❌ TimeoutException — Elemento no aparece en el tiempo esperado

**Causa:** La página tarda más de 30 segundos, o el elemento no va a aparecer.

**Solución:**
- Verificar que la URL es correcta
- Verificar que el locator es correcto (usar DevTools del navegador)
- Si es un elemento que tarda, aumentar el timeout en config: `web.timeout=60`

### ❌ ChromeDriver version mismatch

**Causa:** Rara vez sucede. WebDriverManager lo maneja automáticamente.

**Solución:**
```bash
# Limpiar cache de WebDriverManager
rm -rf ~/.cache/selenium
```

---

> 📖 **Documentación relacionada:**
> - [common/README.md](../common/README.md) — Capa base y motor de ejecución
> - [api-core/README.md](../api-core/README.md) — Para pruebas híbridas API+Web
> - [mobile-core/README.md](../mobile-core/README.md) — Para pruebas mobile
> - [README.md](../README.md) — Visión general del framework
