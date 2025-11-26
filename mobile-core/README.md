# 📱 Mobile-Core - Framework de Testing Mobile

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Appium](https://img.shields.io/badge/Appium-8.6.0-purple.svg)](http://appium.io/)
[![Version](https://img.shields.io/badge/version-1.0.2-blue.svg)](https://github.com/scotia-qa/qa-scotia-frameworks)
[![Status](https://img.shields.io/badge/status-beta-yellow.svg)]()

> Framework especializado para automatización de pruebas de aplicaciones Mobile (iOS y Android). Proporciona gestión de Appium, steps de Cucumber predefinidos, y soporte para gestos nativos.

---

## 📑 Índice

- [🎯 Visión General](#-visión-general)
- [🏗️ Arquitectura](#️-arquitectura)
- [📦 Componentes Principales](#-componentes-principales)
- [📱 Configuración de Appium](#-configuración-de-appium)
- [🥒 Steps Disponibles](#-steps-disponibles)
- [💡 Ejemplos Completos](#-ejemplos-completos)
- [🔗 Integración con API/Web](#-integración-con-apiweb)
- [⚠️ Troubleshooting](#️-troubleshooting)

---

## 🎯 Visión General

### ¿Qué es Mobile-Core?

**Mobile-Core** es la capa especializada para **testing de aplicaciones Mobile**. Extiende **common** y proporciona:

- 📱 **Appium Integration** (iOS y Android)
- 🎮 **Gestos Nativos** (swipe, tap, long-press, pinch)
- 📲 **Device Management** (emuladores y dispositivos reales)
- 🥒 **Steps de Cucumber** para Mobile
- 📸 **Screenshots** automáticos
- 🔄 **App State Management** (install, launch, terminate)

### ¿Para Qué Usar Mobile-Core?

- ✅ Automatizar pruebas de apps Mobile (iOS/Android)
- ✅ Testing en emuladores y dispositivos reales
- ✅ Gestos nativos (swipe, tap, scroll)
- ✅ Flujos híbridos (Mobile + API validations)
- ✅ Testing cross-platform

### ⚠️ Estado Actual

**Mobile-Core está en BETA**. Funcionalidades core están implementadas, pero aún en desarrollo activo.

---

## 🏗️ Arquitectura

### Diagrama de Flujo

```
┌──────────────────────────────────────────────────┐
│          Feature (Cucumber Gherkin)              │
│  When toco el elemento "loginButton"            │
│  And deslizo hacia arriba en "productList"      │
└──────────────────────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────┐
│           MobileSteps (Cucumber)                 │
│  - Define contratos Gherkin                      │
│  - Orquesta interacciones mobile                 │
└──────────────────────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────┐
│      MobileDriverFactory (Appium)                │
│  - Crea AppiumDriver (iOS/Android)               │
│  - Gestiona capabilities                         │
└──────────────────────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────┐
│         AppiumDriver → Device/Emulator           │
│  - Ejecuta comandos nativos                      │
│  - Captura eventos                               │
└──────────────────────────────────────────────────┘
```

### Estructura de Paquetes

```
mobile-core/
├── driver/                 # Gestión de Appium Driver
│   ├── MobileDriverFactory
│   ├── MobileDriverManager
│   └── MobileCapabilities
│
├── steps/                  # Cucumber step definitions
│   └── MobileSteps
│
├── utils/                  # Utilidades Mobile
│   ├── MobileHelper
│   ├── GestureUtils
│   └── MobileScreenshotUtils
│
└── pages/                  # Base para Page Objects
    └── BaseMobilePage
```

---

## 📦 Componentes Principales

### MobileDriverFactory

Crea instancias de AppiumDriver según plataforma.

```java
// Android
AppiumDriver driver = MobileDriverFactory.createAndroidDriver(
    "app.apk",
    "Android",
    "12.0",
    "emulator-5554"
);

// iOS
AppiumDriver driver = MobileDriverFactory.createIOSDriver(
    "app.app",
    "iOS",
    "16.0",
    "iPhone 14 Simulator"
);
```

**Configuración (`mobile-config.properties`):**
```properties
# Plataforma: android, ios
platform=android

# Device
device.name=Pixel 5 Emulator
device.udid=emulator-5554

# OS Version
platform.version=12.0

# App
app.path=/path/to/app.apk

# Appium Server
appium.server.url=http://localhost:4723
```

### MobileDriverManager

Gestión thread-safe de AppiumDriver.

```java
// Establecer driver
MobileDriverManager.setDriver(driver);

// Obtener driver
AppiumDriver driver = MobileDriverManager.getDriver();

// Cerrar driver
MobileDriverManager.quitDriver();
```

### MobileHelper

Helper con métodos para interacciones mobile.

```java
MobileHelper helper = new MobileHelper();

// Tap
helper.tapElement("loginButton");
helper.tapCoordinates(100, 200);

// Swipe
helper.swipeUp();
helper.swipeDown();
helper.swipeLeft();
helper.swipeRight();
helper.swipeElement("productList", "UP");

// Long Press
helper.longPress("menuItem");

// Typing
helper.typeText("username", "john.doe");

// Validaciones
boolean exists = helper.isElementPresent("welcomeMessage");
String text = helper.getTextOf("userName");
```

### GestureUtils

Utilidades para gestos complejos.

```java
// Scroll hasta elemento
GestureUtils.scrollToElement("targetElement");

// Pinch (zoom)
GestureUtils.pinchZoom(element);

// Double tap
GestureUtils.doubleTap(element);

// Drag & Drop
GestureUtils.dragAndDrop(sourceElement, targetElement);
```

---

## 📱 Configuración de Appium

### Prerequisitos

1. **Appium Server** instalado y corriendo
2. **Android SDK** (para Android)
3. **Xcode** (para iOS, solo macOS)
4. **Emuladores** o dispositivos físicos

### Instalar Appium

```bash
# Instalar Appium globalmente
npm install -g appium

# Instalar drivers
appium driver install uiautomator2  # Android
appium driver install xcuitest      # iOS

# Verificar instalación
appium doctor
```

### Iniciar Appium Server

```bash
# Puerto default (4723)
appium

# Puerto custom
appium -p 4724

# Con logs
appium --log-level debug
```

### Android Setup

```bash
# 1. Instalar Android Studio
# 2. Configurar ANDROID_HOME
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/platform-tools

# 3. Verificar dispositivos
adb devices

# 4. Crear emulador (opcional)
avdmanager create avd -n Pixel5 -k "system-images;android-31;google_apis;x86_64"
emulator -avd Pixel5
```

### iOS Setup (macOS only)

```bash
# 1. Instalar Xcode
# 2. Instalar Xcode Command Line Tools
xcode-select --install

# 3. Listar simuladores
xcrun simctl list

# 4. Iniciar simulador
xcrun simctl boot "iPhone 14"
```

---

## 🥒 Steps Disponibles

### Inicialización

```gherkin
Given inicio la aplicación mobile
And lanzo la app "{appPackage}"
When cierro la aplicación
And reinstalo la aplicación
```

### Interacciones Básicas

```gherkin
# Tap
When toco el elemento "loginButton"
And hago tap en el elemento "submitButton"
And toco en las coordenadas x="100" y="200"

# Typing
When ingreso el texto "john.doe" en el campo "username"
And ingreso el texto "password123" en el campo "password"
And limpio el campo "searchBox"

# Long Press
When presiono largo el elemento "menuItem"
```

### Gestos

```gherkin
# Swipe
When deslizo hacia arriba
And deslizo hacia abajo
And deslizo hacia la izquierda
And deslizo hacia la derecha
And deslizo hacia arriba en el elemento "productList"

# Scroll
When hago scroll hasta el elemento "targetItem"
And hago scroll hasta que el texto "Contact Us" sea visible

# Pinch/Zoom
When hago zoom en el elemento "mapView"
And hago pinch en el elemento "imageView"

# Double Tap
When hago doble tap en el elemento "imagePreview"
```

### Validaciones

```gherkin
# Existencia
Then verifico que existe el elemento "welcomeMessage"
And verifico que no existe el elemento "errorAlert"

# Texto
Then verifico que el texto del elemento "userName" sea "John Doe"
And verifico que el texto del elemento "title" contenga "Welcome"

# Estados
Then verifico que el elemento "checkbox" este seleccionado
And verifico que el elemento "submitButton" este habilitado
```

### App Management

```gherkin
# Background/Foreground
When envío la app al background por "5" segundos
And traigo la app al foreground

# Reinstalar
When reinstalo la aplicación
And limpio los datos de la app

# Permisos
When acepto los permisos de ubicación
And acepto los permisos de notificaciones
```

### Dispositivo

```gherkin
# Orientación
When cambio la orientación a "LANDSCAPE"
And cambio la orientación a "PORTRAIT"

# Teclado
When oculto el teclado
And verifico que el teclado esté visible

# Notificaciones (Android)
When abro el panel de notificaciones
And cierro el panel de notificaciones
```

---

## 💡 Ejemplos Completos

### Ejemplo 1: Login Mobile

```gherkin
Feature: Login en app Banking Mobile

  Scenario: Login exitoso con credenciales válidas
    Given inicio la aplicación mobile
    
    # Esperar pantalla de login
    And espero hasta que el elemento "loginScreen" sea visible
    
    # Ingresar credenciales
    When ingreso el texto "john.doe" en el campo "username"
    And ingreso el texto "SecurePass123!" en el campo "password"
    And toco el elemento "loginButton"
    
    # Validar login exitoso
    Then espero hasta que el elemento "homeScreen" sea visible
    And verifico que el texto del elemento "welcomeMessage" contenga "Welcome John"
```

### Ejemplo 2: Lista de Productos con Scroll

```gherkin
Feature: Búsqueda de productos

  Scenario: Buscar y seleccionar producto
    Given inicio la aplicación mobile
    
    # Navegar a productos
    When toco el elemento "productsTab"
    And espero hasta que el elemento "productList" sea visible
    
    # Buscar producto específico
    When ingreso el texto "Laptop" en el campo "searchBox"
    And toco el elemento "searchButton"
    
    # Scroll hasta encontrar
    And hago scroll hasta el elemento "productLaptopPro"
    
    # Seleccionar
    When toco el elemento "productLaptopPro"
    Then espero hasta que el elemento "productDetail" sea visible
    And verifico que el texto del elemento "productName" sea "Laptop Pro 15"
```

### Ejemplo 3: Carrito de Compras

```gherkin
Feature: Carrito de compras

  Scenario: Agregar productos al carrito
    Given inicio la aplicación mobile
    
    # Buscar producto
    When toco el elemento "searchIcon"
    And ingreso el texto "Mouse" en el campo "searchBox"
    And toco el elemento "searchButton"
    
    # Agregar al carrito
    And espero hasta que el elemento "productList" sea visible
    And toco el elemento "addToCart-mouse-wireless"
    Then verifico que el texto del elemento "cartBadge" sea "1"
    
    # Ver carrito
    When toco el elemento "cartIcon"
    And espero hasta que el elemento "cartScreen" sea visible
    Then verifico que el texto del elemento "cartItem-1" contenga "Mouse"
    
    # Checkout
    When deslizo hacia arriba
    And toco el elemento "checkoutButton"
    Then espero hasta que el elemento "checkoutScreen" sea visible
```

### Ejemplo 4: Gestos Complejos

```gherkin
Feature: Galería de imágenes

  Scenario: Navegar galería con gestos
    Given inicio la aplicación mobile
    
    # Ir a galería
    When toco el elemento "galleryTab"
    And espero hasta que el elemento "imageGallery" sea visible
    
    # Ver primera imagen
    When toco el elemento "image-1"
    And espero hasta que el elemento "imageViewer" sea visible
    
    # Zoom
    When hago zoom en el elemento "imageViewer"
    Then verifico que el elemento "zoomIndicator" sea visible
    
    # Swipe a siguiente imagen
    When deslizo hacia la izquierda
    And espero "2" segundos
    Then verifico que el texto del elemento "imageCounter" sea "2 / 10"
    
    # Doble tap para reset zoom
    When hago doble tap en el elemento "imageViewer"
```

---

## 🔗 Integración con API/Web

### Flujo API → Mobile

```gherkin
Feature: Login con token de API

  Scenario: Obtener token en API y usar en Mobile
    # 1. API: Login y obtener token
    Given el host "https://api.banking.com" mas el contexto "/auth/login"
    And agrego el header "Content-Type" con valor "application/json"
    And agrego el request
      """
      {"username": "john.doe", "password": "pass123"}
      """
    When ejecuto la consulta con el metodo "POST"
    Then valido que el codigo de respuesta del servicio sea 200
    And obtengo el campo "token" del objeto "data" y lo guardo como "authToken"
    
    # 2. Mobile: Usar token
    Given inicio la aplicación mobile
    When inyecto el token "{authToken}" en la app
    And espero hasta que el elemento "homeScreen" sea visible
    Then verifico que el texto del elemento "userName" contenga "John Doe"
```

### Flujo Mobile → API

```gherkin
Feature: Validar orden mobile en backend

  Scenario: Crear orden en Mobile y validar en API
    # 1. Mobile: Crear orden
    Given inicio la aplicación mobile
    When toco el elemento "productsTab"
    And toco el elemento "product-laptop"
    And toco el elemento "addToCartButton"
    And toco el elemento "checkoutButton"
    And toco el elemento "confirmOrderButton"
    Then espero hasta que el elemento "orderConfirmation" sea visible
    And guardo texto del elemento "orderNumber" en variable temporal llamada "orderNumber"
    
    # 2. API: Validar orden
    Given el host "https://api.shop.com" mas el contexto "/orders/{orderNumber}"
    When ejecuto la consulta con el metodo "GET"
    Then valido que el codigo de respuesta del servicio sea 200
    And valido que el campo "status" del response sea "pending"
```

---

## ⚠️ Troubleshooting

### Error: "Could not start Appium session"

**Causa:** Appium Server no está corriendo o configuración incorrecta.

**Solución:**
```bash
# 1. Verificar que Appium está corriendo
ps aux | grep appium

# 2. Iniciar Appium
appium

# 3. Verificar configuración en mobile-config.properties
appium.server.url=http://localhost:4723
```

### Error: "App not found"

**Causa:** Path del .apk/.app incorrecto.

**Solución:**
```properties
# Usar path absoluto
app.path=/Users/user/apps/myapp.apk

# O relativo desde proyecto
app.path=./apps/myapp.apk
```

### Error: "Element not found"

**Causa:** Selector incorrecto o elemento no visible.

**Solución:**
```gherkin
# Usar Appium Inspector para encontrar el selector correcto
# Esperar a que sea visible
And espero hasta que el elemento "button" sea visible
```

### Error: "Device not found"

**Causa:** Emulador/dispositivo no conectado.

**Solución:**
```bash
# Android
adb devices

# iOS
xcrun simctl list | grep Booted
```

---

## 📚 Dependencias

| Librería | Versión | Propósito |
|----------|---------|-----------|
| **common** | 1.0.2 | Capa base |
| **Appium Java Client** | 8.6.0 | Appium driver |
| **Selenium** | 4.13.0 | Base WebDriver |
| **Cucumber** | 7.18.0 | BDD Framework |
| **TestNG** | 7.8.0 | Testing framework |

---

## 📱 Plataformas Soportadas

| Plataforma | Versión Mínima | Driver | Estado |
|------------|----------------|--------|--------|
| **Android** | 8.0 (API 26) | UiAutomator2 | ✅ Soportado |
| **iOS** | 14.0 | XCUITest | ✅ Soportado |

---

## 🔗 Enlaces Relacionados

- **[Common README](../common/README.md)** - Capa base
- **[API Core README](../api-core/README.md)** - Testing REST
- **[Web Core README](../web-core/README.md)** - Testing Web UI
- **[Troubleshooting](../TROUBLESHOOTING.md)** - Solución de problemas

---

## 📞 Soporte Mobile

Mobile-Core está en **BETA**. Para soporte:

- 📧 Email: qa-mobile-team@scotiabank.com
- 💬 Slack: #qa-automation-mobile
- 📝 Issues: [GitHub Issues](https://github.com/scotia-qa/qa-scotia-frameworks/issues)

---

<div align="center">

**[⬆ Volver arriba](#-mobile-core---framework-de-testing-mobile)**

**Versión:** 1.0.2 (Beta) | **Autor:** Abel Venero | **QA Team - Scotia Bank**

</div>

