# mobile-core — Capa de Pruebas Mobile (CuAleon Core)

> **Versión:** 2.0.0 | **Grupo:** `com.qa` | **Artefacto:** `mobile-core`  
> **Última actualización:** Abril 2026  
> **Autor:** Abel Venero  
> **Plugin:** `MobilePlugin` — tags de activación: `@mobile`, `@android`, `@ios`, `@appium` — orden: `150`

Capa de automatización mobile de la plataforma CuAleon. Provee steps BDD genéricos sobre Appium 8+ para Android e iOS, con auto-descubrimiento de dispositivos, pool thread-safe y soporte de ejecuciones paralelas.

---

## Arquitectura interna

```
mobile-core/src/main/java/com/qa/mobilecore/
├── plugin/
│   └── MobilePlugin.java              ← Punto de entrada SPI; registra MobileHelper
├── model/
│   ├── DeviceType.java                ← ANDROID_EMULATOR | ANDROID_PHYSICAL | IOS_SIMULATOR | IOS_PHYSICAL | REMOTE_GRID
│   ├── DeviceStatus.java              ← AVAILABLE | BUSY | OFFLINE | UNKNOWN
│   └── DeviceDescriptor.java          ← Descriptor inmutable de dispositivo (Builder pattern)
├── config/
│   └── MobileConfigKeys.java          ← Constantes de configuración (mobile.*)
├── discovery/
│   ├── AdbDeviceScanner.java          ← Detecta dispositivos Android via `adb devices -l`
│   ├── IosDeviceScanner.java          ← Detecta simuladores iOS via `xcrun simctl list --json`
│   └── DeviceDiscoveryService.java    ← Orquesta escaneo; punto de integración para el BE
├── pool/
│   └── DevicePool.java                ← Pool thread-safe (CAS); asigna puertos Appium únicos
├── driver/
│   ├── MobileDriverFactory.java       ← Crea AndroidDriver / IOSDriver (Appium 8+ API)
│   └── MobileDriverManager.java       ← ThreadLocal<AppiumDriver>; análogo a DriverManager de web-core
├── appium/
│   └── AppiumServerManager.java       ← Health check /status + auto-start opt-in (solo local dev)
├── helper/
│   ├── ElementLocatorHelper.java      ← Resolución de locators por prefijo (~, id:, xpath:, text:...)
│   ├── GestureHelper.java             ← Gestos W3C Actions API (tap, swipe, longPress, pinch, zoom)
│   └── MobileHelper.java             ← Fachada principal registrada en ServiceRegistry
├── components/                        ← 10 StepComponent con metadatos para FE/BE
└── steps/
    ├── config/
    │   ├── DeviceConfigSteps.java     ← GIVEN: plataforma, device id, orientación, server URL
    │   └── AppManagementSteps.java    ← GIVEN/WHEN: instalar, lanzar, cerrar, background, restart
    ├── device/
    │   ├── DevicePermissionSteps.java ← GIVEN: permisos del SO (ubicación, cámara, notifs...)
    │   ├── NotificationSteps.java     ← WHEN/THEN: panel de notificaciones
    │   └── SensorSteps.java           ← GIVEN/WHEN: GPS, red, modo avión, rotación
    ├── interaction/
    │   ├── GestureSteps.java          ← WHEN: tap, doble tap, long press, swipe, scroll, pinch, zoom
    │   ├── NativeElementSteps.java    ← WHEN/THEN: escribir, limpiar, tocar, validar elementos
    │   └── ContextSwitchSteps.java    ← WHEN/THEN: nativo ↔ WebView
    ├── validation/
    │   ├── MobileElementValidationSteps.java ← THEN: visibilidad, texto, atributos, listas, screenshot
    │   └── AppStateValidationSteps.java      ← THEN: app abierta/cerrada, instalada, orientación
    └── MobileHooksSteps.java          ← @Before/@After: logging + screenshot on failure + quit driver
```

---

## Dependencias clave

| Librería | Versión | Propósito |
|---|---|---|
| `io.appium:java-client` | 8.6.0 | Driver Appium (UiAutomator2, XCUITest) |
| `org.seleniumhq.selenium:selenium-java` | 4.13.0 | W3C Actions API, WebElement |
| `org.assertj:assertj-core` | 3.27.7 | Assertions fluentes en steps THEN |
| `com.fasterxml.jackson.core:jackson-databind` | 2.15.2 | Parseo JSON de simctl |
| `io.netty:netty-codec-http` | 4.1.121.Final | Seguridad (mitigación CVEs) |

---

## Configuración por proyecto

Crear `src/test/resources/config-app.properties`:

```properties
# Dispositivo
mobile.platform=ANDROID
mobile.device.type=ANDROID_EMULATOR
mobile.device.name=Pixel_6_API_33
mobile.platform.version=13
mobile.udid=

# Appium
mobile.appium.server.url=http://localhost:4723
mobile.appium.base.port=4723
mobile.appium.auto.start=false
mobile.appium.startup.timeout.sec=30
mobile.implicit.wait.sec=10

# App bajo prueba
mobile.app.path=/ruta/a/myapp.apk
mobile.app.package=com.example.myapp
mobile.app.activity=.MainActivity
mobile.app.bundle.id=com.example.MyApp
mobile.app.auto.launch=true
mobile.app.no.reset=false

# Descubrimiento
mobile.discovery.auto.scan=true
mobile.discovery.include.virtual=true
mobile.discovery.include.physical=true
```

---

## Estrategias de localización

Los steps aceptan un `{string}` con prefijo opcional:

| Prefijo | Estrategia | Ejemplo |
|---|---|---|
| `~` | Accessibility ID (recomendado) | `~login_button` |
| `id:` | Resource ID (Android) / name (iOS) | `id:com.app:id/btn` |
| `xpath:` | XPath | `xpath://android.widget.Button[@text='OK']` |
| `class:` | ClassName | `class:android.widget.EditText` |
| `text:` | Texto visible | `text:Iniciar sesion` |
| `pred:` | iOS NSPredicate | `pred:label == 'Login'` |
| `chain:` | iOS Class Chain | `chain:**/XCUIElementTypeButton` |
| `uia:` | Android UIAutomator | `uia:new UiSelector().text("OK")` |
| _(sin prefijo)_ | Accessibility ID por default | `login_button` |

---

## Paralelismo

Cada escenario paralelo tiene:
- Su propio `ExecutionContext` (ThreadLocal del runtime)
- Su propia instancia de `MobileHelper` (via `ServiceRegistry` lazy)
- Su propio `AppiumDriver` (via `MobileDriverManager` ThreadLocal)
- Un dispositivo exclusivo del `DevicePool` (CAS thread-safe)
- Un puerto Appium único asignado por el pool (`base + índice`)

No hay estado compartido entre ejecuciones paralelas.

---

## Integración con el Backend (CuAleon)

### Catálogo de steps para el Frontend

`MobilePlugin` es descubierto automáticamente por `StepDiscoveryService` y expone sus **10 componentes** con metadatos para la paleta visual del Frontend:

```java
StepDiscoveryService discovery = new StepDiscoveryService();
List<StepComponent> mobileComponents = discovery.discoverAll()
    .stream()
    .filter(c -> c.getId().startsWith("mobile."))
    .toList();
// → 10 componentes: mobile.device-config, mobile.app-management, mobile.gesture, ...
```

### Descubrimiento de dispositivos (para el BE)

```java
// El Backend llama esto para exponer dispositivos disponibles al FE
DeviceDiscoveryService discovery = new DeviceDiscoveryService();
List<DeviceDescriptor> devices = discovery.discoverAll();
// → Lista de DeviceDescriptor con: id, name, platform, type, status, appiumPort
```

### Propiedades requeridas en ExecutionRequest

```java
Map<String, String> props = new HashMap<>();
props.put("mobile.device.id",        selectedDeviceId);  // elegido desde FE (null = auto-asignar)
props.put("mobile.platform",         "ANDROID");
props.put("mobile.device.type",      "ANDROID_EMULATOR");
props.put("mobile.app.path",         "/opt/apps/myapp.apk");
props.put("mobile.appium.server.url","http://appium-hub:4723");
```

El `mobile.device.id` puede ser nulo: en ese caso el `DevicePool` asigna el primer dispositivo libre con un puerto Appium único.

---

## Auto-start de Appium (solo desarrollo local)

```properties
mobile.appium.auto.start=true
```

Requiere `appium` en PATH:
```bash
npm install -g appium
appium driver install uiautomator2
appium driver install xcuitest   # solo macOS
```

En CI/CD mantener `mobile.appium.auto.start=false` y levantar Appium externamente (Docker, grid).

---

## Tags de activación

```gherkin
@mobile   @android   @ios   @appium
```

---

## Steps disponibles por componente

### DeviceConfigComponent — GIVEN
```gherkin
Dado que configuro el dispositivo movil como "ANDROID"
Dado que selecciono el dispositivo movil con id "device-pixel6"
Dado que configuro la version de plataforma movil "13"
Dado que configuro la orientacion del dispositivo como "portrait"
Dado que configuro que la app se ejecute en un emulador
Dado que configuro que la app se ejecute en un dispositivo fisico
Dado que configuro que la app se ejecute en un simulador de iOS
Dado que configuro el servidor de Appium en "http://localhost:4723"
```

### AppManagementComponent — GIVEN / WHEN
```gherkin
Dado que configuro el paquete de la app como "com.example.myapp"
Dado que configuro la actividad principal como ".MainActivity"
Dado que configuro el bundle id de la app como "com.example.MyApp"
Dado que instalo la app desde "/opt/apps/myapp.apk"
Dado que lanzo la aplicacion
Cuando cierro la aplicacion
Cuando reinicio la aplicacion movil
Cuando pongo la app en background por 5 segundos
Cuando traigo la aplicacion al primer plano con id "com.example.myapp"
```

### GestureComponent — WHEN
```gherkin
Cuando toco el elemento "~login_button"
Cuando hago doble tap en el elemento "~submit"
Cuando mantengo presionado el elemento "~menu_item" por 2 segundos
Cuando hago tap largo en el elemento "~card" por 1500 milisegundos
Cuando deslizo hacia "arriba"
Cuando deslizo hacia "izquierda"
Cuando hago swipe desde el elemento "~start" hasta el elemento "~end"
Cuando hago scroll hasta que el texto "Ver mas" sea visible
Cuando hago scroll mobile hasta el elemento "~footer"
Cuando hago pinch sobre el elemento "~map"
Cuando hago zoom sobre el elemento "~image"
```

### NativeElementComponent — WHEN / THEN
```gherkin
Cuando escribo "usuario@email.com" en el campo "~email_field"
Cuando borro el contenido del campo "~search_input"
Cuando toco el boton "~btn_login"
Cuando marco el switch "~remember_me"
Cuando desmarco el switch "~notifications"
Entonces el elemento "~welcome_message" debe ser visible
Entonces el elemento "~error_banner" no debe ser visible
Entonces el elemento "~submit_button" debe estar habilitado
Entonces el texto del elemento "~title" debe ser "Bienvenido"
Entonces el texto del elemento "~subtitle" debe contener "sesion"
Entonces el atributo "checked" del elemento "~checkbox" debe ser "true"
```

### ContextSwitchComponent — WHEN / THEN
```gherkin
Cuando cambio el contexto a "NATIVE_APP"
Cuando cambio el contexto a "WEBVIEW"
Entonces deberia existir un contexto WebView disponible
```

### DevicePermissionComponent — GIVEN
```gherkin
Dado que concedo permiso "android.permission.CAMERA" a la aplicacion
Dado que deniego permiso "android.permission.ACCESS_FINE_LOCATION" a la aplicacion
Dado que acepto el dialogo de permiso del sistema
Dado que concedo el permiso de ubicacion
Dado que concedo el permiso de camara
Dado que concedo el permiso de notificaciones
```

### NotificationComponent — WHEN / THEN
```gherkin
Cuando abro el panel de notificaciones
Cuando toco la notificacion que contiene "Nuevo mensaje"
Cuando descarto todas las notificaciones
Entonces verifico que existe una notificacion con el texto "Pago aprobado"
Entonces no deberia existir una notificacion con el texto "Error"
```

### SensorComponent — GIVEN / WHEN
```gherkin
Dado que simulo ubicacion GPS con latitud -34.603684 y longitud -58.381559
Dado que configuro el estado de la conexion de red como "offline"
Dado que configuro el estado de la conexion de red como "online"
Cuando cambio el modo avion a "activado"
Dado que simulo rotacion del dispositivo a "landscape"
```

### MobileElementValidationComponent — THEN
```gherkin
Entonces verifico que el elemento mobile "~title" este visible
Entonces verifico que el elemento mobile "~loader" NO este visible
Entonces verifico que el texto del elemento mobile "~price" sea "$100"
Entonces deberia ver el texto "Transaccion exitosa" en la pantalla
Entonces no deberia ver el texto "Error" en la pantalla
Entonces la lista "~product_list" debe tener 5 elementos
Entonces la lista "~product_list" debe contener un elemento con texto "iPhone"
Entonces tomo screenshot mobile
```

### AppStateValidationComponent — THEN
```gherkin
Entonces verifico que la sesion mobile este activa
Entonces la aplicacion debe estar abierta
Entonces verifico que la app "com.example.myapp" este instalada
Entonces verifico que la orientacion del dispositivo sea "portrait"
Entonces guardo el estado de la app como "${appState}"
```

---

---

## Convención de IDs de Step

Todos los componentes de `mobile-core` declaran su `stepId` con la anotación `@StepId`. Estos IDs son el **contrato con el Backend**: se persisten en la base de datos al guardar escenarios y ejecuciones, y deben mantenerse estables entre releases.

### Catálogo de IDs — mobile-core

| `stepId` | Clase | Fase BDD | Descripción |
|----------|-------|----------|-------------|
| `mobile.device.config` | `DeviceConfigComponent` | GIVEN | Plataforma, device ID, orientación, server Appium |
| `mobile.app.management` | `AppManagementComponent` | GIVEN/WHEN | Instalar, lanzar, cerrar, reiniciar la app |
| `mobile.permissions` | `DevicePermissionComponent` | GIVEN | Permisos del SO (cámara, ubicación, notificaciones) |
| `mobile.sensor` | `SensorComponent` | GIVEN/WHEN | GPS, red, modo avión, rotación de dispositivo |
| `mobile.gesture` | `GestureComponent` | WHEN | Tap, swipe, long press, scroll, pinch, zoom |
| `mobile.element` | `NativeElementComponent` | WHEN/THEN | Escribir, tocar, validar elementos nativos |
| `mobile.context` | `ContextSwitchComponent` | WHEN/THEN | Cambio de contexto nativo ↔ WebView |
| `mobile.notification` | `NotificationComponent` | WHEN/THEN | Panel de notificaciones, leer y descartar |
| `mobile.validation` | `MobileElementValidationComponent` | THEN | Visibilidad, texto, atributos y listas de elementos |
| `mobile.validation.app-state` | `AppStateValidationComponent` | THEN | Estado de la app: abierta, instalada, orientación |

> **Nota sobre guiones:** El segmento `app-state` usa guión (`-`) por legibilidad; es una excepción
> permitida por la convención cuando mejora la claridad. En general se prefieren puntos.

### Reglas de uso

- **No cambiar un `stepId`** sin marcar el anterior `deprecated = true` y declarar `replacedBy`.
- Si se agrega un nuevo componente mobile, seguir el formato `mobile.{dominio}[.{subdominio}]`.
- Los dos componentes de validación comparten el prefijo `mobile.validation[.*]` para agruparlos en el Scenario Builder.

```java
// Ejemplo: resolver el componente de gestos
discovery.resolveStep("mobile.gesture")
         .map(info -> info.getDisplayNameForLocale("en"))
         .orElse("(no encontrado)");
// → "Gestures"
```

> Para la convención general (formato, ciclo de deprecación, reglas globales) ver
> [common/README.md — Convención de IDs de Step](../common/README.md#14-convención-de-ids-de-step).

---

> **Documentación relacionada:**
> - [common/README.md](../common/README.md) — Motor de ejecución y contrato con el Backend
> - [api-core/README.md](../api-core/README.md) — Capa de pruebas de API REST
> - [web-core/README.md](../web-core/README.md) — Capa de pruebas Web
> - [README.md](../README.md) — Visión general de la plataforma CuAleon
