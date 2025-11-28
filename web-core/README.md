# 🖥️ Web Core Layer - Testing de Aplicaciones Web

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Selenium](https://img.shields.io/badge/Selenium-4.27.0-brightgreen.svg)](https://www.selenium.dev/)
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)]()

> Capa especializada para testing de aplicaciones web. Proporciona steps de Cucumber, gestión de WebDriver y utilidades para automatizar pruebas de interfaces de usuario.

---

## 📑 Índice

- [Visión General](#visión-general)
- [Características](#características)
- [Arquitectura](#arquitectura)
- [Steps Disponibles](#steps-disponibles)
- [Estrategia de Locators](#estrategia-de-locators)
- [Ejemplos de Uso](#ejemplos-de-uso)
- [Configuración](#configuración)
- [Integración con Módulos](#integración-con-módulos)
- [Referencia Rápida](#referencia-rápida)

---

## Visión General

**Web-Core** es la capa especializada del framework para **testing de aplicaciones web**. Se construye sobre **Common Layer** y proporciona:

✅ **Steps de Cucumber** para interacciones web
✅ **Gestión de WebDriver** (Chrome, Firefox, Edge)
✅ **Estrategia Module-First** para locators
✅ **Waits inteligentes** y manejo de sincronización
✅ **Capturas de pantalla** automáticas en fallos
✅ **Soporte headless** para CI/CD
✅ **Integración con ScenarioContext** para compartir datos

### Dependencias

```
web-core
    └── common (automática)
        ├── Logging (TestLogger)
        ├── Config (ConfigManager)
        ├── ScenarioContext
        └── WaitUtils
```

---

## Características

### 🎯 Steps de Cucumber

Web-Core proporciona **+50 steps** listos para usar:

#### Navegación
- `Dado que navego a la URL "..."`
- `Cuando hago clic en el elemento "..."`
- `Y espero hasta que elemento "..." este visible`

#### Interacción
- `Cuando ingreso el texto "..." en el elemento "..."`
- `Y selecciono la opción "..." del dropdown "..."`
- `Y marco el checkbox "..."`

#### Validaciones
- `Entonces debo ver el elemento "..."`
- `Y el texto del elemento "..." debe ser "..."`
- `Y el elemento "..." debe estar visible`

#### Manejo de Datos
- `Y guardo texto del elemento "..." en variable "..."`

**Ver lista completa:** [QUICK-REFERENCE.md](QUICK-REFERENCE.md)

---

## Arquitectura

```
web-core/
├── src/main/java/com/scotia/qa/webcore/
│   ├── steps/
│   │   └── WebSteps.java              ← Steps de Cucumber
│   │
│   ├── driver/
│   │   ├── DriverManager.java         ← Gestión thread-safe de drivers
│   │   └── WebDriverFactory.java      ← Factory para crear drivers
│   │
│   ├── utils/
│   │   ├── WebHelper.java             ← Utilidades web
│   │   ├── WaitUtils.java             ← Waits inteligentes
│   │   └── ScreenshotUtils.java       ← Capturas de pantalla
│   │
│   └── locators/
│       └── LocatorStrategy.java       ← Estrategias de localización
│
└── src/main/resources/
    └── drivers/                        ← WebDrivers (si se usan locales)
```

### Flujo de Ejecución

```
┌────────────────────────────────────────────────────────────────┐
│  FEATURE (Gherkin)                                             │
│  @web                                                          │
│  Escenario: Login en aplicación web                           │
│    Dado que navego a la URL "https://app.com/login"           │
│    Cuando ingreso "user" en el elemento "username"            │
│    Y hago clic en el elemento "loginButton"                   │
│    Entonces debo ver el elemento "welcomeMessage"             │
└────────────────────────────────────────────────────────────────┘
                             ↓
┌────────────────────────────────────────────────────────────────┐
│  WEB-CORE (WebSteps.java)                                      │
│  • Obtiene WebDriver de DriverManager                         │
│  • Localiza elementos (Module-First strategy)                 │
│  • Ejecuta acciones (click, sendKeys, etc.)                   │
│  • Aplica waits inteligentes                                  │
│  • Guarda datos en ScenarioContext                            │
└────────────────────────────────────────────────────────────────┘
                             ↓
┌────────────────────────────────────────────────────────────────┐
│  SELENIUM WEBDRIVER                                            │
│  • Controla el navegador                                      │
│  • Ejecuta JavaScript                                         │
│  • Captura screenshots                                        │
│  • Maneja iframes, alerts, ventanas                           │
└────────────────────────────────────────────────────────────────┘
```

---

## Steps Disponibles

### Categoría: Navegación

| Step | Descripción | Ejemplo |
|------|-------------|---------|
| `Dado que navego a la URL {string}` | Navega a una URL | `Dado que navego a la URL "https://app.com"` |
| `Dado que actualizo URL en el navegador {string}` | Navega sin limpiar estado | `Dado que actualizo URL en el navegador "https://app.com/page2"` |

### Categoría: Interacción con Elementos

| Step | Descripción | Ejemplo |
|------|-------------|---------|
| `Cuando ingreso el texto {string} en el elemento {string}` | Escribe texto | `Cuando ingreso el texto "usuario" en el elemento "username"` |
| `Cuando hago clic en el elemento {string}` | Hace clic | `Cuando hago clic en el elemento "submitButton"` |
| `Y presiono el boton {string}` | Hace clic en botón | `Y presiono el boton "loginBtn"` |
| `Y selecciono la opción {string} del dropdown {string}` | Selecciona de dropdown | `Y selecciono la opción "Argentina" del dropdown "country"` |
| `Y marco el checkbox {string}` | Marca checkbox | `Y marco el checkbox "acceptTerms"` |

### Categoría: Validaciones

| Step | Descripción | Ejemplo |
|------|-------------|---------|
| `Entonces debo ver el elemento {string}` | Verifica existencia | `Entonces debo ver el elemento "welcomeMessage"` |
| `Y el texto del elemento {string} debe ser {string}` | Valida texto exacto | `Y el texto del elemento "title" debe ser "Dashboard"` |
| `Y el texto del elemento {string} debe contener {string}` | Valida substring | `Y el texto del elemento "message" debe contener "exitoso"` |
| `Y el elemento {string} debe estar visible` | Verifica visibilidad | `Y el elemento {string} debe estar visible` |
| `Y verifico si existe el elemento {string}` | Verifica existencia (soft) | `Y verifico si existe el elemento "optionalBanner"` |

### Categoría: Waits

| Step | Descripción | Ejemplo |
|------|-------------|---------|
| `Y espero hasta que elemento {string} este visible` | Wait explícito | `Y espero hasta que elemento "loadingSpinner" este visible` |
| `Y espero {int} segundos` | Wait fijo (no recomendado) | `Y espero 3 segundos` |

### Categoría: Manejo de Datos

| Step | Descripción | Ejemplo |
|------|-------------|---------|
| `Y guardo texto del elemento {string} en variable {string}` | Guarda en contexto | `Y guardo texto del elemento "userId" en variable "id"` |

---

## Estrategia de Locators

Web-Core usa la estrategia **Module-First** donde los **módulos definen sus propios locators**.

### Enfoque Module-First

```
┌─────────────────────────────────────────────┐
│  WEB-CORE (Framework)                       │
│  • Proporciona steps genéricos              │
│  • NO conoce locators específicos           │
│  • Busca en ComponentManager del módulo     │
└─────────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────────┐
│  MÓDULO (qa-banking, qa-autos, etc.)        │
│  • Define ComponentManager.java             │
│  • Registra componentes con locators        │
│  • Específico para su aplicación           │
└─────────────────────────────────────────────┘
```

### Ejemplo de Implementación

**En el módulo (`ComponentManager.java`):**

```java
package com.tu.proyecto.components;

import com.scotia.qa.webcore.components.Component;
import java.util.Map;
import java.util.HashMap;

public class ComponentManager {
    private static final Map<String, Component> components = new HashMap<>();
    
    static {
        // Definir componentes de tu aplicación
        components.put("username", Component.builder()
            .id("loginUsername")
            .name("username")
            .css("#login-form input[name='username']")
            .build());
        
        components.put("password", Component.builder()
            .id("loginPassword")
            .css("#login-form input[type='password']")
            .build());
        
        components.put("loginButton", Component.builder()
            .id("btnLogin")
            .css("button.login-submit")
            .xpath("//button[contains(text(),'Iniciar sesión')]")
            .build());
    }
    
    public static Component get(String componentName) {
        return components.get(componentName);
    }
}
```

**En el feature:**

```gherkin
@web
Escenario: Login
  Dado que navego a "https://app.com/login"
  Cuando ingreso "user" en el elemento "username"
  Y ingreso "pass" en el elemento "password"
  Y hago clic en el elemento "loginButton"
```

**Ventajas:**
- ✅ Framework NO conoce tu aplicación
- ✅ Módulo controla sus locators
- ✅ Fácil mantenimiento (un solo lugar)
- ✅ Reutilización de componentes

---

## Ejemplos de Uso

### Ejemplo 1: Login Simple

```gherkin
@web @test
Escenario: Login exitoso
  Dado que navego a la URL "https://app.example.com/login"
  Cuando ingreso el texto "testuser" en el elemento "username"
  Y ingreso el texto "Test123" en el elemento "password"
  Y hago clic en el elemento "loginButton"
  Entonces debo ver el elemento "welcomeMessage"
  Y el texto del elemento "userDisplay" debe contener "testuser"
```

### Ejemplo 2: Formulario Completo

```gherkin
@web @test
Escenario: Completar formulario de registro
  Dado que navego a la URL "https://app.com/register"
  Cuando ingreso el texto "Juan Pérez" en el elemento "fullName"
  Y ingreso el texto "juan@test.com" en el elemento "email"
  Y ingreso el texto "Test123" en el elemento "password"
  Y selecciono la opción "Argentina" del dropdown "country"
  Y marco el checkbox "acceptTerms"
  Y hago clic en el elemento "submitButton"
  Entonces debo ver el elemento "successMessage"
  Y el texto del elemento "successMessage" debe contener "registro exitoso"
```

### Ejemplo 3: Flujo de Compra

```gherkin
@web @test
Escenario: Comprar producto
  # Búsqueda
  Dado que navego a la URL "https://shop.com"
  Cuando ingreso el texto "laptop" en el elemento "searchBox"
  Y hago clic en el elemento "searchButton"
  Y espero hasta que elemento "searchResults" este visible
  
  # Selección
  Cuando hago clic en el elemento "firstProduct"
  Y espero hasta que elemento "productDetails" este visible
  Y guardo texto del elemento "productPrice" en variable "price"
  Y hago clic en el elemento "addToCartButton"
  
  # Checkout
  Cuando hago clic en el elemento "cartIcon"
  Entonces el texto del elemento "cartTotal" debe contener "{price}"
  Cuando hago clic en el elemento "checkoutButton"
  Entonces debo ver el elemento "checkoutForm"
```

### Ejemplo 4: Integración con API (Cross-Layer)

```gherkin
@api @web
Escenario: Crear producto por API y buscar en Web
  # Crear por API
  Dado que tengo el endpoint "/products"
  Y agrego el request:
    """
    {"name": "Nuevo Producto", "sku": "PROD-123"}
    """
  Cuando ejecuto una petición POST
  Entonces el código de respuesta debe ser 201
  Y guardo el valor del campo "sku" en variable "productSku"
  
  # Buscar en web
  Dado que navego a la URL "https://app.com/products"
  Cuando ingreso el texto "{productSku}" en el elemento "searchBox"
  Y hago clic en el elemento "searchButton"
  Entonces el texto del elemento "productName" debe ser "Nuevo Producto"
```

---

## Configuración

### En el Módulo

**1. Agregar dependencia en `build.gradle`:**

```groovy
dependencies {
    testImplementation 'com.scotia.qa:web-core:1.0.0'
    // common se incluye automáticamente
}
```

**2. Configurar en `config-scotia.properties`:**

```properties
# Web Testing
web.base.url=${{WEB_BASE_URL}}
web.browser=chrome
web.headless=false
web.timeout=30
web.implicit.wait=10
```

**3. Configurar variables en `.env.local`:**

```bash
WEB_BASE_URL=https://app-qa.example.com
BROWSER=chrome
HEADLESS=false
```

**4. Agregar glue en `RunCucumberTest.java`:**

```java
@ConfigurationParameter(
    key = "cucumber.glue",
    value = "com.scotia.qa.webcore, com.scotia.qa.common, com.tu.proyecto.steps"
)
```

### Navegadores Soportados

| Navegador | Valor Config | Requisitos |
|-----------|--------------|------------|
| **Chrome** | `chrome` | Chrome instalado + ChromeDriver (auto-download) |
| **Firefox** | `firefox` | Firefox instalado + GeckoDriver (auto-download) |
| **Edge** | `edge` | Edge instalado + EdgeDriver (auto-download) |

### Modo Headless (CI/CD)

```properties
# Para Jenkins/GitLab CI
web.headless=true
```

---

## Integración con Módulos

### Estructura Típica

```
qa-module-tu-proyecto/
├── src/test/
│   ├── java/
│   │   └── com/tu/proyecto/
│   │       ├── RunCucumberTest.java
│   │       ├── components/
│   │       │   └── ComponentManager.java    ← Locators
│   │       ├── pages/
│   │       │   ├── LoginPage.java           ← Page Objects (opcional)
│   │       │   └── DashboardPage.java
│   │       └── steps/
│   │           └── CustomSteps.java
│   │
│   └── resources/
│       ├── features/
│       │   └── web/
│       │       ├── login.feature
│       │       └── dashboard.feature
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
# Navegación
Dado que navego a la URL "https://app.com"

# Interacciones básicas
Cuando ingreso el texto "valor" en el elemento "inputId"
Y hago clic en el elemento "buttonId"

# Waits
Y espero hasta que elemento "loadingSpinner" este visible

# Validaciones
Entonces debo ver el elemento "successMessage"
Y el texto del elemento "title" debe ser "Dashboard"

# Guardar datos
Y guardo texto del elemento "userId" en variable "id"
```

### Uso de Variables

```gherkin
# Guardar desde API
Y guardo el valor del campo "userId" en variable "id"

# Usar en Web (automático)
Dado que navego a la URL "https://app.com/users/{id}"
Entonces el texto del elemento "userName" debe contener "{fullName}"
```

---

## 📚 Documentación Adicional

- **[QUICK-REFERENCE.md](QUICK-REFERENCE.md)** - Referencia rápida de todos los steps
- **[../FRAMEWORK-GUIDE.md](../FRAMEWORK-GUIDE.md)** - Arquitectura del framework
- **[../QUICK-START.md](../QUICK-START.md)** - Guía de inicio rápido
- **[../common/README.md](../common/README.md)** - Documentación de Common Layer

---

## 🐛 Troubleshooting

### ❌ WebDriver no se inicializa

**Problema:** `NullPointerException` al acceder al driver.

**Solución:** Verificar que el scenario tiene el tag correcto:

```gherkin
@web  # ← Este tag es obligatorio
Escenario: Mi test web
```

### ❌ Elemento no encontrado

**Problema:** `NoSuchElementException`.

**Solución:**
1. Verificar que el locator está definido en `ComponentManager`
2. Agregar wait explícito:
   ```gherkin
   Y espero hasta que elemento "miElemento" este visible
   ```

### ❌ ChromeDriver version mismatch

**Problema:** Versión incompatible de ChromeDriver.

**Solución:** WebDriverManager lo descarga automáticamente. Si falla:

```bash
# Limpiar cache
rm -rf ~/.m2/repository/.cache/selenium
```

---

**Última actualización:** 28 de Noviembre de 2025  
**Autor:** Abel Venero  
**Versión:** 1.0.0

