# 📱 mobile-core — Capa de Pruebas de Apps Móviles

> **Versión:** 2.0.0 | **Grupo:** `com.qa` | **Artefacto:** `mobile-core`  
> **Estado:** 🔄 Beta — Estructura completa implementada, en consolidación  
> **Última actualización:** Abril 2026  
> **Autor:** Abel Venero

---

## 📑 Índice

1. [¿Qué es mobile-core en palabras simples?](#1-qué-es-mobile-core-en-palabras-simples)
2. [Conceptos clave antes de empezar](#2-conceptos-clave-antes-de-empezar)
3. [El lugar de mobile-core en el framework](#3-el-lugar-de-mobile-core-en-el-framework)
4. [Mapa completo del módulo](#4-mapa-completo-del-módulo)
5. [Los 10 Componentes de Steps](#5-los-10-componentes-de-steps)
6. [Catálogo de Steps por Categoría](#6-catálogo-de-steps-por-categoría)
7. [Flujo completo de una prueba Mobile](#7-flujo-completo-de-una-prueba-mobile)
8. [Plataformas soportadas (Android e iOS)](#8-plataformas-soportadas-android-e-ios)
9. [Ejemplos prácticos](#9-ejemplos-prácticos)
10. [Configuración y prerequisitos](#10-configuración-y-prerequisitos)
11. [Estado actual y roadmap](#11-estado-actual-y-roadmap)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. ¿Qué es mobile-core en palabras simples?

Imagina que tienes una aplicación bancaria instalada en un teléfono celular y necesitas verificar que cuando el usuario inicia sesión, la app muestra correctamente el saldo de su cuenta. Para hacerlo manualmente habría que: encender el teléfono (o emulador), abrir la app, escribir usuario y contraseña, tocar el botón de ingresar, verificar que aparece el saldo correcto… y repetir esto cada vez que haya un cambio en la app.

**mobile-core** es el asistente que hace eso automáticamente. A través de **Appium** (una herramienta especializada), puede:

- **Conectarse a un dispositivo** Android o iOS real, o a un emulador/simulador
- **Abrir la aplicación** bajo prueba
- **Tocar elementos** en la pantalla (botones, campos, ítems de listas)
- **Escribir texto** en campos
- **Realizar gestos** (swipe hacia arriba/abajo, scroll, presión larga)
- **Cambiar de contexto** entre la app nativa y vistas web dentro de la app
- **Verificar** que lo que aparece en pantalla es lo esperado
- **Manejar permisos** (aceptar o denegar permisos del sistema)

Todo eso, siguiendo instrucciones escritas en español:

```gherkin
@mobile @android @smoke
Scenario: Login exitoso en la app muestra el home
  Given inicio la aplicacion movil
  When ingreso el texto "usuario@empresa.com" en el campo movil "emailField"
  And ingreso el texto "MiPassword@2026!" en el campo movil "passwordField"
  And toco el elemento movil "loginButton"
  Then el elemento movil "homeScreen" debe ser visible
  And el texto del elemento movil "bienvenidaMensaje" debe contener "Bienvenido"
```

---

## 2. Conceptos Clave Antes de Empezar

### 📱 ¿Qué es Appium?

Appium es la herramienta que permite controlar apps móviles desde código Java, de manera similar a como Selenium controla navegadores web. Actúa como intermediario entre el código de la prueba y el dispositivo/emulador.

```
Código Java (test) → Appium Server → Dispositivo Android/iOS
```

Appium necesita ejecutarse como un servidor antes de que corra el test.

### 🤖 ¿UiAutomator2 y XCUITest?

Son los "motores" que Appium usa para interactuar con las apps:
- **UiAutomator2** → para Android (creado por Google)
- **XCUITest** → para iOS (creado por Apple, solo funciona en macOS)

### 🖥️ ¿Emulador vs Dispositivo Real?

- **Emulador** (Android) / **Simulador** (iOS): Un dispositivo virtual que corre en tu computadora. Más fácil de configurar.
- **Dispositivo real**: Un teléfono físico conectado por USB. Más cercano a la realidad del usuario.

### 🔄 ¿Contexto Nativo vs WebView?

Algunas apps son "híbridas" — tienen partes nativas (controles del sistema operativo) y partes que son páginas web incrustadas (WebView). mobile-core puede cambiar entre estos dos contextos para interactuar con ambas partes.

### 🎯 ¿Qué es un locator móvil?

Es la "dirección" de un elemento en la pantalla del dispositivo. Los más comunes son:
- **Android**: `resource-id` (ej: `com.mi.app:id/loginButton`), `accessibility id`, `text`
- **iOS**: `accessibility id`, `class chain`, `predicate string`

---

## 3. El Lugar de mobile-core en el Framework

```
┌──────────────────────────────────────────────────────────────┐
│              qa-frameworks-core                               │
│                                                              │
│  ┌──────────┐  ┌─────────────────────────────────────────┐  │
│  │  common  │  │           mobile-core                   │  │
│  │          │◄─┤                                         │  │
│  │ Runtime  │  │  MobilePlugin      MobileHelper (*)     │  │
│  │ Config   │  │  10 components     GestureUtils (*)     │  │
│  │ Logging  │  │  Steps (4 pkg)     MobileDriverFactory  │  │
│  │ HTTP     │  │                                         │  │
│  └──────────┘  └─────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
          ▲
          │ importa como librería
┌─────────────────────────────────────────────────────────────┐
│  Tu proyecto de pruebas Mobile                               │
│  • features/mobile/*.feature (Gherkin)                       │
│  • ScreenManager (locators de pantallas) (*)                 │
│  • config-app.properties (Appium, device, app path)         │
└─────────────────────────────────────────────────────────────┘

(*) En proceso de implementación
```

**mobile-core aporta:**
- Los ~60 steps en español para controlar apps móviles
- El `MobilePlugin` que se activa con los tags `@mobile`, `@android`, `@ios`, `@appium`
- 10 componentes organizados por responsabilidad
- El `MobileDriverFactory` para crear el driver Appium correcto

**mobile-core NO hace:**
- No conoce los locators de tu app (eso lo define tu proyecto)
- No prueba APIs (eso es `api-core`)
- No prueba navegadores web (eso es `web-core`)
- No requiere modificar el código fuente de la app

---

## 4. Mapa Completo del Módulo

```
mobile-core/
└── src/main/java/com/qa/mobilecore/
    │
    ├── plugin/
    │   └── MobilePlugin.java              ← PUERTA DE ENTRADA: activa la capa Mobile
    │
    ├── components/                        ← CATÁLOGO DE 10 CAPACIDADES (metadatos)
    │   ├── AppManagementComponent.java    ← Gestión de la app (iniciar, cerrar, reinstalar)
    │   ├── DeviceConfigComponent.java     ← Configuración del dispositivo/emulador
    │   ├── GestureComponent.java          ← Gestos (swipe, scroll, tap, long press)
    │   ├── NativeElementComponent.java    ← Interacción con elementos nativos
    │   ├── ContextSwitchComponent.java    ← Cambiar entre Native y WebView
    │   ├── DevicePermissionComponent.java ← Permisos del sistema (cámara, ubicación, etc.)
    │   ├── NotificationComponent.java     ← Manejo de notificaciones push
    │   ├── SensorComponent.java           ← Simulación de sensores (GPS, batería, etc.)
    │   ├── MobileElementValidationComponent.java ← Validar elementos en pantalla
    │   └── AppStateValidationComponent.java      ← Validar estado de la app
    │
    ├── steps/                             ← LOS STEPS BDD
    │   ├── MobileHooksSteps.java          ← Cierre automático del driver al terminar
    │   ├── config/
    │   │   ├── AppManagementSteps.java    ← GIVEN: iniciar/cerrar/resetear la app
    │   │   └── DeviceConfigSteps.java     ← GIVEN: configurar dispositivo/emulador
    │   ├── interaction/
    │   │   ├── GestureSteps.java          ← WHEN: gestos (swipe, scroll, tap)
    │   │   ├── NativeElementSteps.java    ← WHEN: tocar, escribir en elementos
    │   │   └── ContextSwitchSteps.java    ← WHEN: cambiar entre Native y WebView
    │   ├── device/
    │   │   ├── DevicePermissionSteps.java ← WHEN: aceptar/denegar permisos del sistema
    │   │   ├── NotificationSteps.java     ← WHEN: interactuar con notificaciones
    │   │   └── SensorSteps.java           ← WHEN: simular GPS, batería, etc.
    │   └── validation/
    │       ├── MobileElementValidationSteps.java ← THEN: validar elementos en pantalla
    │       └── AppStateValidationSteps.java      ← THEN: validar estado de la app
    │
    └── driver/
        └── MobileDriverFactory.java       ← Crea el AppiumDriver (Android o iOS)
```

---

## 5. Los 10 Componentes de Steps

### 🔵 GIVEN — Configuración (2 componentes)

```
┌──────────────────────────────────────────────────────────────────┐
│  Preparan la app y el dispositivo antes de la prueba             │
├──────────────────────┬───────────────────────────────────────────┤
│ 1. AppManagement     │ ¿Cómo iniciar la app?                    │
│                      │ Iniciar, cerrar, reinstalar, limpiar datos│
├──────────────────────┼───────────────────────────────────────────┤
│ 2. DeviceConfig      │ ¿Qué dispositivo usar?                   │
│                      │ Android/iOS, emulador/real, versión OS    │
└──────────────────────┴───────────────────────────────────────────┘
```

### 🟡 WHEN — Acción (6 componentes)

```
┌───────────────────────────────────────────────────────────────────┐
│  Acciones sobre la app y el dispositivo                            │
├──────────────────────┬────────────────────────────────────────────┤
│ 3. Gesture           │ Swipe, scroll, tap, doble tap, long press  │
├──────────────────────┼────────────────────────────────────────────┤
│ 4. NativeElement     │ Tocar elemento, escribir texto, limpiar    │
├──────────────────────┼────────────────────────────────────────────┤
│ 5. ContextSwitch     │ Cambiar entre app nativa y webview         │
├──────────────────────┼────────────────────────────────────────────┤
│ 6. DevicePermission  │ Aceptar/denegar permisos del sistema       │
├──────────────────────┼────────────────────────────────────────────┤
│ 7. Notification      │ Abrir bandeja, tocar notificación          │
├──────────────────────┼────────────────────────────────────────────┤
│ 8. Sensor            │ Simular ubicación GPS, nivel de batería    │
└──────────────────────┴────────────────────────────────────────────┘
```

### 🟢 THEN — Validación (2 componentes)

```
┌───────────────────────────────────────────────────────────────────┐
│  Verifican que la app muestra y hace lo correcto                  │
├──────────────────────────┬────────────────────────────────────────┤
│ 9. MobileElementValidation│ ¿El elemento existe? ¿Tiene ese texto?│
│                            │ ¿Está habilitado? ¿Está visible?      │
├──────────────────────────┼────────────────────────────────────────┤
│ 10. AppStateValidation   │ ¿La app está en foreground/background? │
│                          │ ¿La pantalla X está activa?            │
└──────────────────────────┴────────────────────────────────────────┘
```

---

## 6. Catálogo de Steps por Categoría

### 📱 Gestión de la App (`AppManagementSteps`)

| Step | Descripción |
|------|-------------|
| `Given inicio la aplicacion movil` | Inicia la app con la configuración de Appium |
| `Given reinicio la aplicacion movil` | Cierra y vuelve a abrir la app |
| `Given cierro la aplicacion movil` | Cierra la app (no la desinstala) |
| `Given limpio los datos de la aplicacion movil` | Resetea el estado de la app (como reinstalar) |
| `Given pongo la aplicacion en segundo plano por {int} segundos` | Minimiza la app N segundos y vuelve |

### ⚙️ Configuración del Dispositivo (`DeviceConfigSteps`)

| Step | Descripción |
|------|-------------|
| `Given configuro el dispositivo movil {string} con plataforma {string}` | Establece el dispositivo y plataforma para la sesión |
| `Given configuro la orientacion del dispositivo como {string}` | `"portrait"` o `"landscape"` |
| `Given bloqueo el dispositivo` | Bloquea la pantalla |
| `Given desbloqueo el dispositivo` | Desbloquea la pantalla |

### 👆 Gestos (`GestureSteps`)

| Step | Descripción |
|------|-------------|
| `When toco el elemento movil {string}` | Tap simple en el elemento |
| `When hago doble tap en el elemento movil {string}` | Doble tap en el elemento |
| `When mantengo presionado el elemento movil {string}` | Long press (presión larga) |
| `When deslizo hacia arriba` | Swipe up en el centro de la pantalla |
| `When deslizo hacia abajo` | Swipe down en el centro de la pantalla |
| `When deslizo hacia la izquierda` | Swipe left (típico para ir a la siguiente pantalla) |
| `When deslizo hacia la derecha` | Swipe right (típico para volver) |
| `When hago scroll hasta el elemento movil {string}` | Scrollea hasta que el elemento sea visible |
| `When deslizo el elemento movil {string} hacia {string}` | Swipe en dirección específica sobre un elemento |

### ✍️ Elementos Nativos (`NativeElementSteps`)

| Step | Descripción |
|------|-------------|
| `When ingreso el texto {string} en el campo movil {string}` | Escribe el texto en el campo |
| `When limpio el campo movil {string}` | Borra el contenido del campo |
| `When selecciono el item {string} de la lista {string}` | Selecciona un ítem de una lista/picker |
| `When presiono el boton nativo {string}` | Presiona botones nativos del sistema (Back, Home, etc.) |

### 🔄 Cambio de Contexto (`ContextSwitchSteps`)

| Step | Descripción |
|------|-------------|
| `When cambio al contexto nativo` | Vuelve al contexto NATIVE de la app |
| `When cambio al contexto web` | Cambia al primer contexto WebView disponible |
| `When cambio al contexto {string}` | Cambia a un contexto específico por nombre |

### 🔐 Permisos del Sistema (`DevicePermissionSteps`)

| Step | Descripción |
|------|-------------|
| `When acepto el permiso del sistema` | Toca "Permitir" en el popup de permisos |
| `When deniego el permiso del sistema` | Toca "Denegar" en el popup de permisos |
| `When acepto el permiso {string}` | Acepta un permiso específico (cámara, ubicación, etc.) |
| `When deniego el permiso {string}` | Deniega un permiso específico |

### 🔔 Notificaciones (`NotificationSteps`)

| Step | Descripción |
|------|-------------|
| `When abro la bandeja de notificaciones` | Desliza desde arriba para ver notificaciones |
| `When toco la notificacion {string}` | Toca una notificación específica por texto |
| `When cierro la bandeja de notificaciones` | Cierra la bandeja de notificaciones |

### 📡 Sensores (`SensorSteps`)

| Step | Descripción |
|------|-------------|
| `When simulo la ubicacion GPS con latitud {double} y longitud {double}` | Establece ubicación GPS falsa |
| `When simulo el nivel de bateria al {int} por ciento` | Simula nivel de batería (emulador) |
| `When simulo modo avion {string}` | Activa/desactiva modo avión |

### ✅ Validación de Elementos (`MobileElementValidationSteps`)

| Step | Descripción |
|------|-------------|
| `Then el elemento movil {string} debe ser visible` | Falla si el elemento no está visible |
| `Then el elemento movil {string} no debe ser visible` | Falla si el elemento SÍ está visible |
| `Then el elemento movil {string} debe estar habilitado` | Falla si el elemento está deshabilitado |
| `Then el texto del elemento movil {string} debe ser {string}` | Valida texto exacto |
| `Then el texto del elemento movil {string} debe contener {string}` | Valida que contiene el texto |
| `Then el atributo {string} del elemento movil {string} debe ser {string}` | Valida un atributo del elemento |

### 📊 Validación del Estado de la App (`AppStateValidationSteps`)

| Step | Descripción |
|------|-------------|
| `Then la aplicacion debe estar en primer plano` | Verifica que la app está activa |
| `Then la aplicacion debe estar en segundo plano` | Verifica que la app está minimizada |
| `Then la pantalla actual debe ser {string}` | Verifica la pantalla activa por identificador |

---

## 7. Flujo Completo de una Prueba Mobile

Sigamos el viaje de este escenario de principio a fin:

```gherkin
@mobile @android @smoke
Scenario: Login exitoso muestra el balance de cuenta
  Given inicio la aplicacion movil
  When ingreso el texto "usuario@empresa.com" en el campo movil "emailField"
  And ingreso el texto "MiPassword@2026!" en el campo movil "passwordField"
  And toco el elemento movil "loginButton"
  Then el elemento movil "homeScreen" debe ser visible
  And el texto del elemento movil "welcomeLabel" debe contener "Bienvenido"
```

### Paso 0: El motor activa MobilePlugin

`ScenarioExecutionHooks.@Before` detecta `@mobile` (y opcionalmente `@android`) → activa `MobilePlugin` → registra el driver Appium en `ServiceRegistry` (lazy).

### Paso 1: `Given inicio la aplicacion movil`

```
AppManagementSteps.iniciarApp()
    │
    ▼
MobileDriverFactory.createDriver(plataforma, capabilities)
    │
    ├── Lee de config: appium.server.url, mobile.app.path,
    │                  mobile.platform, mobile.device.name,
    │                  mobile.platform.version
    │
    ├── Construye AndroidDriver con UiAutomator2Capabilities:
    │     {
    │       platformName: "Android",
    │       deviceName: "Pixel_6_API_33",
    │       app: "/ruta/app-debug.apk",
    │       automationName: "UiAutomator2"
    │     }
    │
    ├── Se conecta al servidor Appium en http://localhost:4723
    ├── Appium instala la app en el emulador/dispositivo
    └── Retorna AndroidDriver listo para usar
    │
    ▼
Log: "✅ App iniciada en Android | device: Pixel_6_API_33"
```

### Paso 2: `When ingreso el texto "usuario@empresa.com" en el campo movil "emailField"`

```
NativeElementSteps.ingresarTextoEnCampo("usuario@empresa.com", "emailField")
    │
    ▼
driver.findElement(By.accessibilityId("emailField"))
    → Espera hasta 30s que el campo aparezca
    → element.sendKeys("usuario@empresa.com")
    │
    ▼
Log: "✅ Texto ingresado en campo 'emailField'"
```

### Paso 3: `When toco el elemento movil "loginButton"`

```
GestureSteps.tocarElemento("loginButton")
    │
    ▼
driver.findElement(By.id("com.empresa.app:id/loginButton"))
    → Espera hasta que sea visible
    → element.click()  (en mobile, click = tap)
    │
    ▼
App procesa login → navega a la pantalla Home
```

### Paso 4: `Then el elemento movil "homeScreen" debe ser visible`

```
MobileElementValidationSteps.elementoDebeSerVisible("homeScreen")
    │
    ▼
driver.findElement(By.accessibilityId("homeScreen"))
    → Verifica que el elemento existe y es visible
    → ✅ PASA — El home screen apareció tras el login
```

### Fin del escenario ✅

`MobileHooksSteps.@After` → `driver.quit()` → Cierra la sesión Appium → Listo.

---

## 8. Plataformas Soportadas (Android e iOS)

### Android

| Elemento | Detalle |
|----------|---------|
| **Versión mínima** | Android 8.0 (API 26) |
| **Motor Appium** | UiAutomator2 |
| **Locators recomendados** | `resource-id`, `accessibility id`, `text`, `class` |
| **Emuladores** | Android Virtual Device (AVD) con Android Studio |
| **Dispositivos reales** | Conectar por USB con depuración USB activada |

**Capabilities típicas para Android:**

```properties
# config-app.properties
mobile.platform=Android
mobile.device.name=Pixel_6_API_33
mobile.platform.version=13.0
mobile.automation.name=UiAutomator2
mobile.app.path=${APP_PATH}
appium.server.url=http://localhost:4723
```

### iOS

| Elemento | Detalle |
|----------|---------|
| **Versión mínima** | iOS 14.0 |
| **Motor Appium** | XCUITest |
| **Requisito del sistema** | Solo macOS con Xcode |
| **Locators recomendados** | `accessibility id`, `class chain`, `predicate string` |
| **Simuladores** | Xcode iOS Simulator |
| **Dispositivos reales** | Requiere certificado de desarrollador Apple |

**Capabilities típicas para iOS:**

```properties
# config-app.properties
mobile.platform=iOS
mobile.device.name=iPhone 15
mobile.platform.version=17.0
mobile.automation.name=XCUITest
mobile.app.path=${IOS_APP_PATH}
appium.server.url=http://localhost:4723
```

### Diferencias importantes entre Android e iOS

| Aspecto | Android | iOS |
|---------|---------|-----|
| Locator principal | `resource-id` | `accessibility id` |
| Botón Atrás | Hardware/software | Gesto de swipe |
| Permisos | Popup en runtime | Se piden en primera ejecución |
| Modo emulador | AVD (Android Studio) | Xcode Simulator |
| Host requerido | Windows/Mac/Linux | Solo macOS |

---

## 9. Ejemplos Prácticos

### Ejemplo 1: Login Básico Android

```gherkin
@mobile @android @smoke
Scenario: Login exitoso con credenciales válidas
  Given inicio la aplicacion movil
  When ingreso el texto "admin@empresa.com" en el campo movil "emailInput"
  And ingreso el texto "Admin@2026!" en el campo movil "passwordInput"
  And toco el elemento movil "btnLogin"
  Then el elemento movil "dashboardScreen" debe ser visible
  And el texto del elemento movil "welcomeText" debe contener "Bienvenido"
```

### Ejemplo 2: Navegar con Gestos

```gherkin
@mobile @android @regression
Scenario: Navegar por la lista de transacciones
  Given inicio la aplicacion movil
  When toco el elemento movil "menuTransacciones"
  Then el elemento movil "listaTransacciones" debe ser visible
  When deslizo hacia arriba
  And deslizo hacia arriba
  And hago scroll hasta el elemento movil "transaccionMasAntigua"
  Then el elemento movil "transaccionMasAntigua" debe ser visible
```

### Ejemplo 3: Flujo con Permisos

```gherkin
@mobile @android @regression
Scenario: Activar notificaciones de la app
  Given inicio la aplicacion movil
  When toco el elemento movil "btnActivarNotificaciones"
  And acepto el permiso del sistema
  Then el texto del elemento movil "estadoNotificaciones" debe ser "Activadas"
```

### Ejemplo 4: App Híbrida con WebView

```gherkin
@mobile @android @regression
Scenario: Verificar el contenido de la sección de ayuda (WebView)
  Given inicio la aplicacion movil
  When toco el elemento movil "menuAyuda"
  And cambio al contexto web
  Then la URL debe contener "ayuda"
  And el texto del elemento movil "tituloPagina" debe contener "Centro de Ayuda"
  When cambio al contexto nativo
  Then el elemento movil "btnVolver" debe ser visible
```

### Ejemplo 5: Prueba con GPS simulado

```gherkin
@mobile @android @regression
Scenario: La app muestra sucursales cercanas según ubicación
  Given inicio la aplicacion movil
  When simulo la ubicacion GPS con latitud -33.4489 y longitud -70.6693
  And toco el elemento movil "btnSucursalesCercanas"
  Then el elemento movil "listaSucursales" debe ser visible
  And el texto del elemento movil "primerasSucursal" debe contener "Santiago"
```

### Ejemplo 6: Prueba Híbrida API + Mobile

```gherkin
@api @mobile @android @e2e
Scenario: Crear transacción por API y verificar en la app
  # Crear transacción via API
  Given configuro endpoint con base "https://api.empresa.com/" y path "api/transactions"
  And agrego autenticacion Client Credentials
  And agrego el header "Content-Type" con valor "application/json"
  And agrego el request
    """
    { "monto": 50000, "concepto": "Pago prueba QA", "destinatario": "12345678" }
    """
  When ejecuto la consulta con el metodo "POST"
  Then valido que el codigo de respuesta del servicio sea 201
  And el resultado almaceno el valor que está dentro de la estructura "transactionId" en "txId"

  # Verificar en la app mobile
  Given inicio la aplicacion movil
  When toco el elemento movil "menuTransacciones"
  And ingreso el texto "${txId}" en el campo movil "buscadorTransacciones"
  And toco el elemento movil "btnBuscar"
  Then el elemento movil "detalleTransaccion" debe ser visible
  And el texto del elemento movil "conceptoTransaccion" debe contener "Pago prueba QA"
```

---

## 10. Configuración y Prerequisitos

### Software requerido

#### Siempre requerido

```bash
# 1. Node.js y Appium 2.x
npm install -g appium
appium --version  # Verificar: debe mostrar 2.x.x

# 2. Appium Inspector (para encontrar locators)
# Descargar desde: https://github.com/appium/appium-inspector/releases
```

#### Para Android

```bash
# 3. Android Studio (incluye AVD Manager y Android SDK)
# Descargar desde: https://developer.android.com/studio

# 4. Driver de Appium para Android
appium driver install uiautomator2

# 5. Verificar que el emulador está disponible
emulator -list-avds

# 6. Iniciar el emulador
emulator -avd Pixel_6_API_33

# 7. Verificar que Appium lo detecta
adb devices  # Debe mostrar el emulador
```

#### Para iOS (solo macOS)

```bash
# 3. Xcode con Command Line Tools
xcode-select --install

# 4. Driver de Appium para iOS
appium driver install xcuitest

# 5. Iniciar simulador
open -a Simulator
```

### Iniciar el servidor Appium

```bash
# Iniciar Appium
appium

# Output esperado:
# [Appium] Welcome to Appium v2.x.x
# [Appium] Appium REST http interface listener started on http://0.0.0.0:4723
```

### Dependencia en `build.gradle`

```groovy
dependencies {
    implementation 'com.qa:mobile-core:2.0.0'
    // common se incluye automáticamente
}
```

### Archivo de configuración del proyecto

```properties
# config-app.properties

# Configuración Appium
appium.server.url=http://localhost:4723

# Android
mobile.platform=Android
mobile.device.name=Pixel_6_API_33
mobile.platform.version=13.0
mobile.automation.name=UiAutomator2
mobile.app.path=${ANDROID_APP_PATH}

# iOS (comentar Android y descomentar esto para iOS)
# mobile.platform=iOS
# mobile.device.name=iPhone 15
# mobile.platform.version=17.0
# mobile.automation.name=XCUITest
# mobile.app.path=${IOS_APP_PATH}
```

```bash
# .env.local
ANDROID_APP_PATH=/ruta/absoluta/a/mi-app-debug.apk
# IOS_APP_PATH=/ruta/absoluta/a/mi-app.ipa
```

### Runner de Cucumber

```java
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME,
    value = "com.qa.mobilecore, com.qa.common, com.mi.proyecto.steps")
@ConfigurationParameter(key = Constants.FEATURES_PROPERTY_NAME,
    value = "src/test/resources/features/mobile")
public class RunMobileCucumberTest {}
```

---

## 11. Estado Actual y Roadmap

### ✅ Implementado en esta versión

| Componente | Estado | Descripción |
|-----------|--------|-------------|
| `MobilePlugin.java` | ✅ | Plugin completo con 10 componentes y lifecycle |
| 10 clases `*Component` | ✅ | Descriptores de metadatos para el catálogo |
| `AppManagementSteps` | ✅ | Iniciar, cerrar, resetear app |
| `DeviceConfigSteps` | ✅ | Configurar dispositivo y orientación |
| `GestureSteps` | ✅ | Swipe, scroll, tap, long press |
| `NativeElementSteps` | ✅ | Tocar, escribir en elementos nativos |
| `ContextSwitchSteps` | ✅ | Cambiar entre NATIVE y WebView |
| `DevicePermissionSteps` | ✅ | Aceptar/denegar permisos |
| `NotificationSteps` | ✅ | Interactuar con notificaciones |
| `SensorSteps` | ✅ | Simular GPS, batería, modo avión |
| `MobileElementValidationSteps` | ✅ | Validar elementos en pantalla |
| `AppStateValidationSteps` | ✅ | Validar estado de la app |
| `MobileDriverFactory` | ✅ | Crea driver Appium (Android/iOS) |

### 🔄 En proceso / Próximas mejoras

| Pendiente | Descripción | Prioridad |
|-----------|-------------|-----------|
| **MobileHelper** | Fachada central (como WebHelper en web-core) para centralizar la lógica de interacción | Alta |
| **Screen Objects** | Patrón equivalente a Page Objects para pantallas móviles | Alta |
| **Tests de integración** | Pruebas de los steps con un app de ejemplo | Media |
| **Deep Links** | Step para abrir la app en una pantalla específica via deep link | Media |
| **Accessibility Snapshot** | Extraer y validar el árbol de accesibilidad completo | Baja |
| **Video Recording** | Grabar video de la ejecución (útil para debugging) | Baja |

### Métricas del módulo

| Métrica | Valor |
|---------|-------|
| Archivos Java en producción | **20** |
| Steps BDD implementados | **~60** steps en 8 clases |
| Componentes declarados | **10** descriptores |
| Plataformas soportadas | **2** (Android, iOS) |

---

## 12. Troubleshooting

### ❌ Appium server not running — Connection refused

**Problema:** El test no puede conectarse a Appium.

**Solución:**
```bash
# Verificar que Appium está corriendo
appium

# Si no estaba corriendo, iniciarlo y volver a ejecutar el test
```

### ❌ El emulador no es detectado

**Problema:** Appium no encuentra el dispositivo.

**Solución:**
```bash
# Verificar dispositivos conectados
adb devices

# Si el emulador no aparece, iniciarlo
emulator -avd Pixel_6_API_33 &

# Esperar que cargue completamente y ejecutar de nuevo
adb devices
```

### ❌ La app no se instala

**Problema:** El APK/IPA no se encuentra o tiene un error.

**Solución:**
```bash
# Verificar que la ruta existe
ls -la /ruta/a/mi-app.apk

# Verificar variable de entorno
echo $ANDROID_APP_PATH
```

### ❌ NoSuchElementException — Elemento no encontrado en pantalla

**Causa:** El locator es incorrecto, o la pantalla no cargó completamente.

**Solución:**
1. Usar **Appium Inspector** para verificar el locator real del elemento
2. Agregar un wait antes del step:
   ```gherkin
   # Si el elemento tarda en aparecer
   When hago scroll hasta el elemento movil "miElemento"
   ```
3. Verificar que el nombre en el step coincide exactamente con el locator definido en el proyecto

### ❌ Session not created — Version mismatch

**Problema:** La versión del driver Appium no es compatible con el dispositivo.

**Solución:**
```bash
# Actualizar el driver de Appium
appium driver update uiautomator2
# o
appium driver update xcuitest
```

---

> 📖 **Documentación relacionada:**
> - [common/README.md](../common/README.md) — Capa base y motor de ejecución
> - [api-core/README.md](../api-core/README.md) — Para pruebas híbridas API+Mobile
> - [web-core/README.md](../web-core/README.md) — Para pruebas de interfaz Web
> - [README.md](../README.md) — Visión general del framework
> - [Appium Documentation](https://appium.io/docs/en/latest/) — Documentación oficial de Appium
