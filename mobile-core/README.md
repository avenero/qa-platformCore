# 📱 Mobile Core Layer - Testing de Aplicaciones Móviles

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Appium](https://img.shields.io/badge/Appium-9.1.0-purple.svg)](https://appium.io/)
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)]()

> Capa especializada para testing de aplicaciones móviles (Android/iOS). Proporciona steps de Cucumber, gestión de Appium y utilidades para automatizar pruebas en dispositivos y emuladores.

---

## 📑 Índice

- [Visión General](#visión-general)
- [Características](#características)
- [Arquitectura](#arquitectura)
- [Steps Disponibles](#steps-disponibles)
- [Plataformas Soportadas](#plataformas-soportadas)
- [Ejemplos de Uso](#ejemplos-de-uso)
- [Configuración](#configuración)
- [Integración con Módulos](#integración-con-módulos)
- [Referencia Rápida](#referencia-rápida)

---

## Visión General

**Mobile-Core** es la capa especializada del framework para **testing de aplicaciones móviles**. Se construye sobre **Common Layer** y proporciona:

✅ **Steps de Cucumber** para interacciones móviles
✅ **Gestión de Appium** (Android e iOS)
✅ **Soporte para emuladores** y dispositivos reales
✅ **Gestos móviles** (swipe, scroll, tap, long press)
✅ **Manejo de permisos** y notificaciones
✅ **Capturas de pantalla** en dispositivos
✅ **Integración con ScenarioContext** para compartir datos

### Dependencias

```
mobile-core
    └── common (automática)
        ├── Logging (TestLogger)
        ├── Config (ConfigManager)
        ├── ScenarioContext
        └── WaitUtils
```

---

## Características

### 🎯 Steps de Cucumber

Mobile-Core proporciona **+45 steps** específicos para mobile:

#### Navegación Mobile
- `Dado que inicio la aplicación móvil`
- `Cuando hago tap en el elemento móvil "..."`
- `Y deslizo hacia arriba`

#### Interacción Mobile
- `Cuando ingreso el texto "..." en el campo móvil "..."`
- `Y hago swipe en el elemento "..." hacia la dirección "..."`
- `Y mantengo presionado el elemento "..."`

#### Validaciones
- `Entonces debo ver el elemento móvil "..."`
- `Y el texto del elemento móvil "..." debe ser "..."`
- `Y el elemento móvil "..." debe estar habilitado`

#### Gestos
- `Cuando hago scroll hasta el elemento "..."`
- `Y deslizo desde "..." hasta "..."`

---

## Arquitectura

```
mobile-core/
├── src/main/java/com/scotia/qa/mobilecore/
│   ├── steps/
│   │   └── MobileSteps.java           ← Steps de Cucumber
│   │
│   ├── driver/
│   │   ├── AppiumDriverManager.java   ← Gestión de drivers
│   │   └── DriverFactory.java         ← Factory para crear drivers
│   │
│   ├── utils/
│   │   ├── MobileHelper.java          ← Utilidades mobile
│   │   ├── GestureUtils.java          ← Gestos (swipe, scroll)
│   │   └── ScreenshotUtils.java       ← Capturas
│   │
│   └── capabilities/
│       ├── AndroidCapabilities.java   ← Capabilities Android
│       └── IOSCapabilities.java       ← Capabilities iOS
│
└── src/main/resources/
    └── apps/                           ← APKs/IPAs de prueba
```

### Flujo de Ejecución

```
┌────────────────────────────────────────────────────────────────┐
│  FEATURE (Gherkin)                                             │
│  @mobile                                                       │
│  Escenario: Login en app móvil                                │
│    Dado que inicio la aplicación móvil                        │
│    Cuando ingreso "user" en el campo móvil "username"         │
│    Y hago tap en el elemento móvil "loginButton"              │
│    Entonces debo ver el elemento móvil "homeScreen"           │
└────────────────────────────────────────────────────────────────┘
                             ↓
┌────────────────────────────────────────────────────────────────┐
│  MOBILE-CORE (MobileSteps.java)                                │
│  • Obtiene AppiumDriver de DriverManager                      │
│  • Localiza elementos móviles                                 │
│  • Ejecuta acciones (tap, sendKeys, swipe)                    │
│  • Aplica waits específicos de mobile                         │
│  • Guarda datos en ScenarioContext                            │
└────────────────────────────────────────────────────────────────┘
                             ↓
┌────────────────────────────────────────────────────────────────┐
│  APPIUM                                                        │
│  • Controla el dispositivo/emulador                           │
│  • Ejecuta gestos nativos                                     │
│  • Captura screenshots                                        │
│  • Maneja permisos y notificaciones                           │
└────────────────────────────────────────────────────────────────┘
```

---

## Steps Disponibles

### Categoría: Inicialización

| Step | Descripción | Ejemplo |
|------|-------------|---------|
| `Dado que inicio la aplicación móvil` | Inicia la app | `Dado que inicio la aplicación móvil` |
| `Y reinicio la aplicación` | Reinicia la app | `Y reinicio la aplicación` |
| `Y cierro la aplicación` | Cierra la app | `Y cierro la aplicación` |

### Categoría: Interacción

| Step | Descripción | Ejemplo |
|------|-------------|---------|
| `Cuando hago tap en el elemento móvil {string}` | Tap en elemento | `Cuando hago tap en el elemento móvil "loginButton"` |
| `Cuando ingreso el texto {string} en el campo móvil {string}` | Escribe texto | `Cuando ingreso el texto "usuario" en el campo móvil "username"` |
| `Y mantengo presionado el elemento {string}` | Long press | `Y mantengo presionado el elemento "menuItem"` |
| `Y hago doble tap en el elemento {string}` | Double tap | `Y hago doble tap en el elemento "image"` |

### Categoría: Gestos

| Step | Descripción | Ejemplo |
|------|-------------|---------|
| `Cuando deslizo hacia arriba` | Swipe up | `Cuando deslizo hacia arriba` |
| `Y deslizo hacia abajo` | Swipe down | `Y deslizo hacia abajo` |
| `Y deslizo hacia la izquierda` | Swipe left | `Y deslizo hacia la izquierda` |
| `Y deslizo hacia la derecha` | Swipe right | `Y deslizo hacia la derecha` |
| `Y hago scroll hasta el elemento {string}` | Scroll to element | `Y hago scroll hasta el elemento "submitButton"` |

### Categoría: Validaciones

| Step | Descripción | Ejemplo |
|------|-------------|---------|
| `Entonces debo ver el elemento móvil {string}` | Verifica existencia | `Entonces debo ver el elemento móvil "welcomeMessage"` |
| `Y el texto del elemento móvil {string} debe ser {string}` | Valida texto | `Y el texto del elemento móvil "title" debe ser "Dashboard"` |
| `Y el elemento móvil {string} debe estar habilitado` | Verifica estado | `Y el elemento móvil "submitButton" debe estar habilitado` |

### Categoría: Manejo de Permisos

| Step | Descripción | Ejemplo |
|------|-------------|---------|
| `Y acepto los permisos de la aplicación` | Acepta permisos | `Y acepto los permisos de la aplicación` |
| `Y deniego los permisos de la aplicación` | Deniega permisos | `Y deniego los permisos de la aplicación` |

---

## Plataformas Soportadas

### Android

**Requisitos:**
- Android SDK instalado
- Android Studio (para emuladores)
- Appium instalado

**Capabilities típicas:**
```java
{
  "platformName": "Android",
  "platformVersion": "13.0",
  "deviceName": "Pixel_5_API_33",
  "app": "/path/to/app.apk",
  "automationName": "UiAutomator2"
}
```

### iOS

**Requisitos:**
- macOS con Xcode
- iOS Simulator
- Appium instalado

**Capabilities típicas:**
```java
{
  "platformName": "iOS",
  "platformVersion": "16.0",
  "deviceName": "iPhone 14",
  "app": "/path/to/app.ipa",
  "automationName": "XCUITest"
}
```

---

## Ejemplos de Uso

### Ejemplo 1: Login en App Móvil

```gherkin
@mobile @test
Escenario: Login exitoso en app móvil
  Dado que inicio la aplicación móvil
  Cuando ingreso el texto "testuser" en el campo móvil "username"
  Y ingreso el texto "Test123" en el campo móvil "password"
  Y hago tap en el elemento móvil "loginButton"
  Entonces debo ver el elemento móvil "homeScreen"
  Y el texto del elemento móvil "welcomeMessage" debe contener "Bienvenido"
```

### Ejemplo 2: Navegación con Gestos

```gherkin
@mobile @test
Escenario: Navegar por la app
  Dado que inicio la aplicación móvil
  Cuando hago tap en el elemento móvil "menuButton"
  Y espero 2 segundos
  Y deslizo hacia arriba
  Y hago scroll hasta el elemento "settingsOption"
  Y hago tap en el elemento móvil "settingsOption"
  Entonces debo ver el elemento móvil "settingsScreen"
```

### Ejemplo 3: Formulario Móvil

```gherkin
@mobile @test
Escenario: Completar formulario en app
  Dado que inicio la aplicación móvil
  Cuando hago tap en el elemento móvil "newFormButton"
  Y ingreso el texto "Juan Pérez" en el campo móvil "fullName"
  Y ingreso el texto "juan@test.com" en el campo móvil "email"
  Y hago scroll hasta el elemento "phoneField"
  Y ingreso el texto "1234567890" en el campo móvil "phoneField"
  Y hago tap en el elemento móvil "submitButton"
  Entonces debo ver el elemento móvil "successMessage"
```

### Ejemplo 4: Integración con API (Cross-Layer)

```gherkin
@api @mobile
Escenario: Crear usuario por API y validar en App
  # Crear usuario por API
  Dado que tengo el endpoint "/users"
  Y agrego el request:
    """
    {
      "name": "Test User",
      "email": "test@example.com"
    }
    """
  Cuando ejecuto una petición POST
  Entonces el código de respuesta debe ser 201
  Y guardo el valor del campo "id" en variable "userId"
  
  # Validar en app móvil
  Dado que inicio la aplicación móvil
  Cuando hago tap en el elemento móvil "searchButton"
  Y ingreso el texto "{userId}" en el campo móvil "searchInput"
  Y hago tap en el elemento móvil "searchSubmit"
  Entonces el texto del elemento móvil "userName" debe ser "Test User"
```

---

## Configuración

### Prerequisitos

**1. Instalar Appium:**

```bash
# Usando npm
npm install -g appium

# Verificar instalación
appium --version
```

**2. Instalar drivers:**

```bash
# Driver Android
appium driver install uiautomator2

# Driver iOS (solo macOS)
appium driver install xcuitest
```

### En el Módulo

**1. Agregar dependencia en `build.gradle`:**

```groovy
dependencies {
    testImplementation 'com.scotia.qa:mobile-core:1.0.0'
    // common se incluye automáticamente
}
```

**2. Configurar en `config-scotia.properties`:**

```properties
# Mobile Testing
mobile.platform=Android
mobile.device.name=Pixel_5_API_33
mobile.platform.version=13.0
mobile.app.path=${{APP_PATH}}
mobile.automation.name=UiAutomator2
appium.server.url=http://localhost:4723
```

**3. Configurar variables en `.env.local`:**

```bash
APP_PATH=/Users/tu-usuario/apps/app-debug.apk
PLATFORM=Android
```

**4. Agregar glue en `RunCucumberTest.java`:**

```java
@ConfigurationParameter(
    key = "cucumber.glue",
    value = "com.scotia.qa.mobilecore, com.scotia.qa.common, com.tu.proyecto.steps"
)
```

### Iniciar Appium Server

```bash
# Iniciar servidor Appium
appium

# Output esperado:
# [Appium] Welcome to Appium v2.x.x
# [Appium] Appium REST http interface listener started on http://0.0.0.0:4723
```

---

## Integración con Módulos

### Estructura Típica

```
qa-module-tu-app-mobile/
├── src/test/
│   ├── java/
│   │   └── com/tu/proyecto/
│   │       ├── RunCucumberTest.java
│   │       ├── screens/
│   │       │   ├── LoginScreen.java      ← Screen Objects
│   │       │   └── HomeScreen.java
│   │       └── steps/
│   │           └── CustomSteps.java
│   │
│   └── resources/
│       ├── features/
│       │   └── mobile/
│       │       ├── login.feature
│       │       └── navigation.feature
│       │
│       ├── apps/
│       │   └── app-debug.apk            ← APK de prueba
│       │
│       └── config-scotia.properties
│
├── .env.local
└── build.gradle
```

---

## Referencia Rápida

### Cheat Sheet de Steps Comunes

```gherkin
# Inicialización
Dado que inicio la aplicación móvil

# Interacciones
Cuando ingreso el texto "valor" en el campo móvil "inputId"
Y hago tap en el elemento móvil "buttonId"

# Gestos
Y deslizo hacia arriba
Y hago scroll hasta el elemento "elemento"

# Validaciones
Entonces debo ver el elemento móvil "successMessage"
Y el texto del elemento móvil "title" debe ser "Dashboard"
```

### Locators Móviles

**Android:**
```xml
<!-- resource-id -->
<Button android:id="@+id/loginButton" />

<!-- text -->
<TextView android:text="Login" />

<!-- content-desc -->
<ImageView android:contentDescription="Logo" />
```

**iOS:**
```xml
<!-- accessibilityId -->
<UIButton accessibilityIdentifier="loginButton" />

<!-- label -->
<UILabel text="Login" />
```

---

## 📚 Documentación Adicional

- **[../FRAMEWORK-GUIDE.md](../FRAMEWORK-GUIDE.md)** - Arquitectura del framework
- **[../QUICK-START.md](../QUICK-START.md)** - Guía de inicio rápido
- **[../common/README.md](../common/README.md)** - Documentación de Common Layer
- **[Appium Docs](https://appium.io/docs/en/latest/)** - Documentación oficial de Appium

---

## 🐛 Troubleshooting

### ❌ Appium server not running

**Problema:** No se puede conectar a Appium.

**Solución:** Iniciar servidor Appium:

```bash
appium
```

### ❌ App no se instala

**Problema:** La app no se instala en el dispositivo.

**Solución:** Verificar ruta del APK/IPA:

```bash
# Verificar que existe
ls -la /path/to/app.apk

# Actualizar en config
APP_PATH=/path/correcto/app.apk
```

### ❌ Elemento no encontrado

**Problema:** `NoSuchElementException`.

**Solución:**
1. Verificar locator con Appium Inspector
2. Agregar wait:
   ```gherkin
   Y espero 3 segundos
   ```

### ❌ Emulador no inicia

**Problema:** El emulador Android no inicia.

**Solución:**

```bash
# Listar emuladores
emulator -list-avds

# Iniciar emulador
emulator -avd Pixel_5_API_33
```

---

**Última actualización:** 28 de Noviembre de 2025  
**Autor:** Abel Venero  
**Versión:** 1.0.0

