# 💻 Web-Core - Framework de Testing Web UI

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Selenium](https://img.shields.io/badge/Selenium-4.27.0-green.svg)](https://www.selenium.dev/)
[![Version](https://img.shields.io/badge/version-1.0.2-blue.svg)](https://github.com/scotia-qa/qa-scotia-frameworks)

> Framework especializado para automatización de pruebas de aplicaciones Web UI. Proporciona WebDriver management, Page Object Model, steps de Cucumber predefinidos, y waits inteligentes.

---

## 📑 Índice

- [🎯 Visión General](#-visión-general)
- [🏗️ Arquitectura](#️-arquitectura)
- [📦 Componentes Principales](#-componentes-principales)
- [🎯 Estrategia de Localizadores](#-estrategia-de-localizadores)
- [⏱️ Sistema de Waits Inteligentes](#️-sistema-de-waits-inteligentes)
- [🥒 Steps Disponibles](#-steps-disponibles)
- [💡 Ejemplos Completos](#-ejemplos-completos)
- [🔗 Integración con API](#-integración-con-api)
- [⚠️ Troubleshooting](#️-troubleshooting)

---

## 🎯 Visión General

### ¿Qué es Web-Core?

**Web-Core** es la capa especializada para **testing de aplicaciones Web UI**. Extiende **common** y proporciona:

- 🌐 **WebDriver Management** (Chrome, Firefox, Edge, Safari)
- 🎨 **Page Object Model** con Component Pattern
- ⏱️ **Waits Inteligentes** (sin hardcode)
- 🥒 **80+ Steps de Cucumber** predefinidos
- 📸 **Screenshot** automático en fallos
- 🔄 **Selenium Grid** support
- 🎯 **Estrategia de Localizadores** Module-First

### ¿Para Qué Usar Web-Core?

- ✅ Automatizar pruebas de UI Web
- ✅ Testing cross-browser (Chrome, Firefox, Edge, Safari)
- ✅ Page Object Model con componentes reutilizables
- ✅ Flujos híbridos (Web + API validations)
- ✅ Testing en Selenium Grid

---

## 🏗️ Arquitectura

### Diagrama de Flujo de Validación

```
┌─────────────────────────────────────────┐
│ Feature (Gherkin)                       │
│ And verifico si existe el elemento      │
│     "userButton"                        │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│ WebSteps.verificoSiExisteElElemento()  │
│ - Define contrato Cucumber              │
│ - NO conoce negocio ni módulo           │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│ WebHelper.waitForVisibleElement()      │
│ - Lee timeout desde configuración       │
│ - Usa WebDriverWait (espera explícita) │
│ - Retorna boolean (no lanza excepción) │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│ WebDriverWait + ExpectedConditions      │
│ - Polling cada 500ms hasta timeout      │
│ - visibilityOfElementLocated()         │
└─────────────────────────────────────────┘
```

### Estructura de Paquetes

```
web-core/
├── driver/                 # Gestión de WebDriver
│   ├── WebDriverFactory
│   ├── DriverManager
│   └── WebDriverConfig
│
├── steps/                  # Cucumber step definitions
│   └── WebSteps
│
├── utils/                  # Utilidades Web
│   ├── WebHelper
│   ├── WaitUtils
│   └── ScreenshotUtils
│
└── pages/                  # Base para Page Objects
    └── BasePage
```

---

## 📦 Componentes Principales

### WebDriverFactory

Crea y configura instancias de WebDriver según configuración.

```java
// Crear driver desde configuración (web-config.properties)
WebDriver driver = WebDriverFactory.createDriver();

// Crear driver específico
WebDriver chrome = WebDriverFactory.createDriver(BrowserType.CHROME);
WebDriver firefox = WebDriverFactory.createDriver(BrowserType.FIREFOX);

// Modo Grid
WebDriver gridDriver = WebDriverFactory.createDriver(
    BrowserType.CHROME, 
    ExecutionMode.GRID
);
```

**Configuración (`web-config.properties`):**
```properties
# Navegador: chrome, firefox, edge, safari
browser=chrome

# Modo headless
headless=false

# Ejecución: local, grid
execution.mode=local

# Selenium Grid Hub
grid.hub.url=http://localhost:4444/wd/hub

# Timeouts
explicit.wait=15
implicit.wait=10
page.load.timeout=30
```

### DriverManager

Gestión thread-safe de instancias de WebDriver.

```java
// Establecer driver (automático en hooks)
DriverManager.setDriver(driver);

// Obtener driver en cualquier parte del código
WebDriver driver = DriverManager.getDriver();

// Cerrar driver
DriverManager.quitDriver();
```

**Thread-Local para Tests Paralelos:**
```java
// ✅ SEGURO - Cada thread tiene su propio driver
@Before
public void setup() {
    WebDriver driver = WebDriverFactory.createDriver();
    DriverManager.setDriver(driver);
}

@After
public void teardown() {
    DriverManager.quitDriver();
}
```

### WebHelper

Helper principal con 100+ métodos para interacciones Web.

```java
WebHelper helper = new WebHelper();

// Interacciones básicas
helper.clickElement("loginButton");
helper.setText("username", "john.doe");
helper.selectOptionComboBox("country", "USA");

// Validaciones
boolean exists = helper.isPresent("welcomeMessage");
String text = helper.getTextOf("userName");
boolean isEnabled = helper.isEnabled("submitButton");

// Esperas inteligentes
boolean visible = helper.waitForVisibleElement("dashboard");
boolean enabled = helper.waitForElementEnabled("button", 10);

// Screenshots
helper.captureScreen(scenario);

// Navegación
helper.navigateToUrl("https://example.com");
helper.refreshPage();

// iFrames
helper.changeIFrame("frameId", "");
helper.leaveIFrame();

// Ventanas
helper.switchToNewWindow();
helper.closeCurrentWindow();
```

### WaitUtils

Utilidades de espera sin hardcode de tiempos.

```java
// Esperar elemento visible
WaitUtils.waitForElementToBeVisible(element);

// Esperar elemento clickable
WaitUtils.waitForElementToBeClickable(element);

// Esperar elemento estable (no en animación)
boolean stable = WaitUtils.waitForElementToBeStable(element);

// Esperar página lista (DOM + jQuery)
WaitUtils.waitForPageReady();

// Configuración desde web-config.properties
// No hardcode de timeouts!
```

---

## 🎯 Estrategia de Localizadores

### Principio: Module-First

Los **localizadores NO van en el framework**, van en los **módulos de negocio**.

```
❌ INCORRECTO - Localizadores en web-core
web-core/
  └── locators/
      └── LoginPageLocators.java  ← NO!

✅ CORRECTO - Localizadores en módulo
qa-banking/
  └── locators/
      └── LoginPageLocators.java  ← SÍ!
```

### Definir Localizadores en tu Módulo

```java
// En tu módulo: qa-banking/src/main/java/locators/
package com.scotia.qa.banking.locators;

import org.openqa.selenium.By;

public class LoginPageLocators {
    // IDs (preferidos)
    public static final By USERNAME = By.id("username");
    public static final By PASSWORD = By.id("password");
    public static final By LOGIN_BUTTON = By.id("loginButton");
    
    // CSS Selectors (segunda opción)
    public static final By ERROR_MESSAGE = By.cssSelector(".error-message");
    public static final By WELCOME_MESSAGE = By.cssSelector("div.welcome h1");
    
    // XPath (último recurso)
    public static final By SUBMIT = By.xpath("//button[@type='submit']");
}
```

### Estrategia de Localización

**Orden de Preferencia:**

1. **ID** → Más rápido y único
2. **CSS Selector** → Flexible y rápido
3. **Name** → Si no hay ID
4. **XPath** → Solo si no hay otra opción

```java
// ✅ MEJOR - ID
By.id("username")

// ✅ BUENO - CSS Selector
By.cssSelector("#username")
By.cssSelector("input[name='username']")
By.cssSelector("div.login input.user-field")

// ⚠️ ACEPTABLE - Name
By.name("username")

// ❌ EVITAR - XPath complejo
By.xpath("//div[@class='container']//div[@class='form']//input[@name='username']")
```

### WebHelper: Multi-Strategy Locator

WebHelper intenta **múltiples estrategias automáticamente**:

```java
// Puedes pasar solo el valor, WebHelper prueba:
// 1. By.id("loginButton")
// 2. By.name("loginButton")
// 3. By.cssSelector("#loginButton")
// 4. By.xpath("//loginButton")

helper.clickElement("loginButton");

// ✅ Funciona con cualquier estrategia
helper.clickElement("loginButton");           // Busca por id
helper.clickElement("#loginBtn");             // CSS Selector
helper.clickElement("//button[@id='login']"); // XPath
```

---

### Componentes Genéricos: El Complemento de Module-First

Además de los **localizadores en módulos**, la estrategia Module-First se complementa con **Componentes Genéricos** en el framework.

#### ¿Qué son los Componentes Genéricos?

Son **clases reutilizables** que encapsulan el **comportamiento** de elementos UI comunes (botones, inputs, dropdowns, tablas), **sin conocer los localizadores específicos**.

```
┌─────────────────────────────────────────────────────────┐
│              ESTRATEGIA COMPLETA                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  📦 FRAMEWORK (web-core)                                │
│  ├── Componentes Genéricos (comportamiento)            │
│  │   ├── ButtonComponent    → cómo hacer click        │
│  │   ├── InputComponent     → cómo escribir           │
│  │   ├── DropdownComponent  → cómo seleccionar        │
│  │   └── TableComponent     → cómo buscar en tabla    │
│  │                                                      │
│  └── WebHelper (interacciones base)                    │
│                                                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  🏢 MÓDULO (qa-banking)                                 │
│  ├── Localizadores (dónde están los elementos)         │
│  │   └── LoginPageLocators.java                        │
│  │       • USERNAME = By.id("username")                │
│  │       • PASSWORD = By.id("password")                │
│  │       • LOGIN_BUTTON = By.id("loginButton")         │
│  │                                                      │
│  └── Page Objects (orquestación)                       │
│      └── LoginPage.java                                │
│          • Usa componentes + localizadores             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### Componentes Disponibles en Web-Core

| Componente | Responsabilidad | Métodos Principales |
|------------|-----------------|---------------------|
| **ButtonComponent** | Clicks en botones | `click()`, `doubleClick()`, `jsClick()` |
| **InputComponent** | Entrada de texto | `type()`, `clear()`, `pressEnter()` |
| **DropdownComponent** | Selección de opciones | `selectByText()`, `selectByValue()`, `selectByIndex()` |
| **TableComponent** | Interacción con tablas | `getRow()`, `searchInColumn()`, `getCellValue()` |
| **FormComponent** | Llenado de formularios | `fillField()`, `submitForm()` |
| **ModalComponent** | Modals/Dialogs | `waitForModal()`, `closeModal()`, `getModalTitle()` |

#### Ejemplo: Usar Componentes en tu Módulo

**En tu módulo (qa-banking):**

```java
// 1. Definir localizadores
package com.scotia.qa.banking.locators;

public class LoginPageLocators {
    public static final By USERNAME = By.id("username");
    public static final By PASSWORD = By.id("password");
    public static final By LOGIN_BUTTON = By.id("loginButton");
    public static final By REMEMBER_ME = By.id("rememberMe");
    public static final By COUNTRY_SELECT = By.id("country");
}

// 2. Crear Page Object usando componentes del framework
package com.scotia.qa.banking.pages;

import com.scotia.qa.webcore.components.*;
import com.scotia.qa.banking.locators.LoginPageLocators;

public class LoginPage extends BasePage {
    
    // Componentes genéricos + localizadores específicos
    private InputComponent usernameField;
    private InputComponent passwordField;
    private ButtonComponent loginButton;
    private CheckboxComponent rememberMeCheckbox;
    private DropdownComponent countryDropdown;
    
    public LoginPage(WebDriver driver) {
        super(driver);
        // Inicializar componentes con localizadores del módulo
        this.usernameField = new InputComponent(driver, LoginPageLocators.USERNAME);
        this.passwordField = new InputComponent(driver, LoginPageLocators.PASSWORD);
        this.loginButton = new ButtonComponent(driver, LoginPageLocators.LOGIN_BUTTON);
        this.rememberMeCheckbox = new CheckboxComponent(driver, LoginPageLocators.REMEMBER_ME);
        this.countryDropdown = new DropdownComponent(driver, LoginPageLocators.COUNTRY_SELECT);
    }
    
    // Métodos de negocio usando componentes
    public void login(String username, String password) {
        usernameField.type(username);
        passwordField.type(password);
        loginButton.click();
    }
    
    public void loginWithCountry(String username, String password, String country) {
        usernameField.type(username);
        passwordField.type(password);
        countryDropdown.selectByText(country);
        rememberMeCheckbox.check();
        loginButton.click();
    }
    
    public boolean isLoginButtonEnabled() {
        return loginButton.isEnabled();
    }
}
```

#### Ventajas de esta Estrategia

**✅ Separación de Responsabilidades:**
- **Framework:** Cómo interactuar con elementos (componentes)
- **Módulo:** Dónde están los elementos (localizadores) + lógica de negocio (pages)

**✅ Reutilización:**
```java
// ❌ ANTES - Duplicar código en cada página
public void clickLoginButton() {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    WebElement button = wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON));
    button.click();
}

// ✅ DESPUÉS - Usar componente reutilizable
loginButton.click();  // El componente maneja waits, retry, etc.
```

**✅ Mantenibilidad:**
```java
// Si cambia la estrategia de click (ej: agregar retry logic)
// Solo se modifica ButtonComponent en el framework
// Todos los módulos se benefician automáticamente
```

**✅ Testing más fácil:**
```java
// Los componentes ya tienen built-in:
// - Waits automáticos
// - Retry logic
// - Logging
// - Error handling
```

#### Ejemplo Completo: Login con Componentes

**Feature (Gherkin):**
```gherkin
Feature: Login Banking

  Scenario: Login con componentes
    Given navego a la página de login
    When ingreso credenciales de usuario "john.doe"
    And selecciono país "United States"
    And marco recordar sesión
    And hago click en login
    Then verifico que estoy en dashboard
```

**Step Definition:**
```java
@Given("navego a la página de login")
public void navegoALogin() {
    loginPage = new LoginPage(driver);
    loginPage.navigateTo("https://banking.example.com/login");
}

@When("ingreso credenciales de usuario {string}")
public void ingresoCredenciales(String username) {
    String password = getPasswordFor(username);
    loginPage.login(username, password);
}

@When("selecciono país {string}")
public void seleccionoPais(String country) {
    loginPage.selectCountry(country);
}

@When("marco recordar sesión")
public void marcoRecordar() {
    loginPage.checkRememberMe();
}

@When("hago click en login")
public void clickLogin() {
    loginPage.submitLogin();
}
```

**Implementación en LoginPage:**
```java
public void selectCountry(String country) {
    // El componente maneja:
    // - Wait hasta que dropdown esté visible
    // - Scroll si es necesario
    // - Select de la opción
    // - Logging automático
    countryDropdown.selectByText(country);
}

public void checkRememberMe() {
    rememberMeCheckbox.check();
}

public void submitLogin() {
    loginButton.click();
}
```

#### Crear Componentes Custom (Avanzado)

Si necesitas un componente específico para tu módulo:

```java
// En tu módulo
package com.scotia.qa.banking.components;

import com.scotia.qa.webcore.components.BaseComponent;

public class BankingDatePickerComponent extends BaseComponent {
    
    public BankingDatePickerComponent(WebDriver driver, By locator) {
        super(driver, locator);
    }
    
    public void selectDate(LocalDate date) {
        click(); // Método heredado de BaseComponent
        
        // Lógica específica del date picker de banking
        WebElement monthDropdown = driver.findElement(By.id("month"));
        WebElement dayInput = driver.findElement(By.id("day"));
        WebElement yearInput = driver.findElement(By.id("year"));
        
        new Select(monthDropdown).selectByValue(String.valueOf(date.getMonthValue()));
        dayInput.sendKeys(String.valueOf(date.getDayOfMonth()));
        yearInput.sendKeys(String.valueOf(date.getYear()));
    }
}
```

#### Resumen: Module-First Completo

```
FRAMEWORK proporciona:
  ✅ Componentes genéricos (ButtonComponent, InputComponent, etc.)
  ✅ Comportamientos reutilizables
  ✅ Waits automáticos
  ✅ Error handling

MÓDULO proporciona:
  ✅ Localizadores específicos (dónde están los elementos)
  ✅ Page Objects (orquestación)
  ✅ Lógica de negocio
  ✅ Componentes custom (si es necesario)

RESULTADO:
  ✅ Framework genérico y reutilizable
  ✅ Módulos independientes y mantenibles
  ✅ Zero acoplamiento entre módulos
  ✅ Testing robusto y escalable
```

---

## ⏱️ Sistema de Waits Inteligentes

### Problema: Hardcode de Timeouts

```java
// ❌ MALO - Hardcode
Thread.sleep(5000);
wait.until(condition, Duration.ofSeconds(60));
```

### Solución: Configuración Centralizada

```properties
# web-config.properties
explicit.wait=15      # Waits explícitos
implicit.wait=10      # Implicit wait (NO mezclar con explícitos)
page.load.timeout=30  # Timeout de carga de página
```

### Waits Disponibles

#### 1. **waitForVisibleElement()** - Elemento Visible

```java
// Usar timeout de configuración (15s)
boolean visible = helper.waitForVisibleElement("dashboard");

// Timeout custom
boolean visible = helper.waitForVisibleElement("dashboard", 30);
```

**Cuándo usar:** Validar que un elemento aparezca y sea visible.

#### 2. **waitForElementEnabled()** - Elemento Habilitado

```java
boolean enabled = helper.waitForElementEnabled("submitButton", 10);
```

**Cuándo usar:** Antes de hacer click/type en un campo que puede estar disabled inicialmente.

#### 3. **waitForElementToBeStable()** - Elemento Estable

```java
boolean stable = WaitUtils.waitForElementToBeStable(element);
```

**Cuándo usar:** Elementos en animación (modals, sliders, etc.).

#### 4. **waitForPageReady()** - Página Lista

```java
WaitUtils.waitForPageReady();
```

**Cuándo usar:** Después de navegaciones, reloads, o acciones que recargan la página.

**Verifica:**
- `document.readyState === 'complete'`
- `jQuery.active === 0` (si existe)

#### 5. **waitForInvisibilityOfElement()** - Elemento Desaparece

```java
boolean invisible = WaitUtils.waitForInvisibilityOfElement(By.id("loadingSpinner"));
```

**Cuándo usar:** Esperar que un loading spinner desaparezca.

### Ejemplo: Reemplazo de Sleeps

```java
// ❌ ANTES - Hardcode
public void waitForHome() {
    Thread.sleep(2000);  // 2s
    driver.navigate().to(url);
    Thread.sleep(5000);  // 5s más
}
// Total: 7 segundos SIEMPRE

// ✅ DESPUÉS - Inteligente
public void waitForHome() {
    driver.navigate().to(url);
    WaitUtils.waitForPageReady();  // Solo el tiempo necesario
}
// Total: ~1-2 segundos (95% de los casos)
```

**Resultado:**
- ⚡ 70% más rápido
- ✅ Sin falsos negativos
- ✅ Logs más limpios

---

## 🥒 Steps Disponibles

### Navegación

```gherkin
Given actualizo URL en el navegador "https://banking.example.com"
When navego a la URL "https://banking.example.com/dashboard"
And recargo la página
```

### Interacciones con Elementos

```gherkin
# Click
When presiono el botón "loginButton"
And hago click en el elemento "submitButton"
And realizo click en "acceptTerms"

# Typing
When ingreso el texto "john.doe" en el elemento "username"
And ingreso el texto "password123" en el elemento "password"
And ingreso el texto "{authToken}" en el elemento "tokenField"

# Selects/Dropdowns
When selecciono el texto "United States" en el combobox "country"
And selecciono la opción "USD" en el combobox "currency"

# Checkbox/Radio
When selecciono el checkbox "acceptTerms"
And selecciono el radio button con valor "male"

# Limpiar campos
When limpio el campo "searchBox"
```

### Esperas

```gherkin
# Esperar visibilidad
And espero hasta que elemento "dashboard" este visible
And espero hasta que elemento "loadingSpinner" no este visible

# Esperar habilitado
And espero hasta que elemento "submitButton" este habilitado

# Esperar tiempo (EVITAR - usar esperas inteligentes)
And espero un tiempo de "5" segundos
```

### Validaciones

```gherkin
# Existencia
Then verifico si existe el elemento "welcomeMessage"
And verifico que no exista el elemento "errorMessage"

# Texto
Then verifico que el texto en "userName" sea "John Doe"
And verifico que el texto en "welcome" contenga el texto "Welcome back"
And verifico que el texto en "userName" sea "{fullName}"

# Estados
Then verifico que el elemento "submitButton" este habilitado
And verifico que el elemento "loadingSpinner" este deshabilitado
And verifico que el checkbox "acceptTerms" este seleccionado

# Validación condicional
Then verifico si existe el elemento "userButton" y valido que el texto sea "{full_name}"
And verifico si existe el elemento "banner" y hago clic
```

### Screenshots

```gherkin
When capturo una imagen de la pantalla
And tomo screenshot de "estado_actual"
```

### Variables Temporales

```gherkin
# Guardar
And guardo texto del elemento "orderNumber" en variable temporal llamada "orderNumber"
And guardo texto "john.doe@example.com" en variable temporal llamada "email"

# Usar
And ingreso el texto "{orderNumber}" en el elemento "searchBox"
Then verifico que el texto en "emailDisplay" sea "{email}"
```

### iFrames

```gherkin
When cambio al iframe con path "paymentFrame"
And cambio al iframe con nombre "checkoutFrame"
And salgo del iframe y vuelvo al contenido principal
```

### Ventanas

```gherkin
When abro nueva ventana
And cambio a la nueva ventana
And cierro la ventana actual
And vuelvo a la ventana principal
```

---

## 💡 Ejemplos Completos

### Ejemplo 1: Login Simple

```gherkin
Feature: Login en aplicación Banking

  Scenario: Login exitoso con credenciales válidas
    Given actualizo URL en el navegador "https://banking.example.com/login"
    
    # Esperar que la página cargue
    And espero hasta que elemento "username" este visible
    
    # Ingresar credenciales
    When ingreso el texto "john.doe" en el elemento "username"
    And ingreso el texto "SecurePass123!" en el elemento "password"
    And presiono el botón "loginButton"
    
    # Validar login exitoso
    Then espero hasta que elemento "dashboard" este visible
    And verifico si existe el elemento "welcomeMessage"
    And verifico que el texto en "userName" contenga el texto "John Doe"
```

### Ejemplo 2: Formulario Complejo

```gherkin
Feature: Registro de cliente

  Scenario: Completar formulario de registro
    Given actualizo URL en el navegador "https://banking.example.com/register"
    
    # Datos personales
    When ingreso el texto "John" en el elemento "firstName"
    And ingreso el texto "Doe" en el elemento "lastName"
    And ingreso el texto "john.doe@example.com" en el elemento "email"
    And ingreso el texto "555-1234" en el elemento "phone"
    
    # Selects
    And selecciono el texto "United States" en el combobox "country"
    And selecciono el texto "New York" en el combobox "state"
    
    # Checkbox
    And selecciono el checkbox "acceptTerms"
    
    # Submit
    And presiono el botón "submitButton"
    
    # Validar
    Then espero hasta que elemento "successMessage" este visible
    And verifico que el texto en "successMessage" contenga el texto "Registration successful"
    And guardo texto del elemento "customerId" en variable temporal llamada "customerId"
```

### Ejemplo 3: Búsqueda y Validación de Tabla

```gherkin
Feature: Búsqueda de órdenes

  Scenario: Buscar y validar orden específica
    Given actualizo URL en el navegador "https://shop.example.com/admin/orders"
    
    # Login (asumiendo ya logueado)
    When ingreso el texto "ORD-12345" en el elemento "searchBox"
    And presiono el botón "searchButton"
    
    # Esperar resultados
    And espero hasta que elemento "resultsTable" este visible
    And espero hasta que elemento "loadingSpinner" no este visible
    
    # Validar resultado
    Then verifico si existe el elemento "orderRow-ORD-12345"
    And verifico que el texto en "orderStatus-ORD-12345" sea "Shipped"
    And verifico que el texto en "orderTotal-ORD-12345" sea "$1,234.56"
    
    # Click en detalle
    When presiono el botón "viewDetails-ORD-12345"
    Then espero hasta que elemento "orderDetailModal" este visible
```

---

## 🔗 Integración con API

### Flujo API → Web

**Use Case:** Obtener token en API, usar en Web.

```gherkin
Feature: Login híbrido API + Web

  Scenario: Login con token de API
    # 1. API: Obtener token
    Given el host "https://api.banking.com" mas el contexto "/auth/login"
    And agrego el header "Content-Type" con valor "application/json"
    And agrego el request
      """
      {"username": "john.doe", "password": "pass123"}
      """
    When ejecuto la consulta con el metodo "POST"
    Then valido que el codigo de respuesta del servicio sea 200
    And obtengo el campo "token" del objeto "data" y lo guardo como "authToken"
    And obtengo el campo "user_full_name" del objeto "data" y lo guardo como "fullName"
    
    # 2. Web: Usar token
    Given actualizo URL en el navegador "https://banking.com/dashboard"
    # JavaScript para inyectar token en localStorage
    When ejecuto JavaScript para guardar token "{authToken}"
    And recargo la página
    
    # 3. Validar
    Then espero hasta que elemento "welcomeMessage" este visible
    And verifico que el texto en "userName" contenga el texto "{fullName}"
```

### Flujo Web → API

**Use Case:** Crear orden en Web, validar en API.

```gherkin
Feature: Validación backend de orden Web

  Scenario: Crear orden en Web y validar en API
    # 1. Web: Crear orden
    Given actualizo URL en el navegador "https://shop.example.com"
    When ingreso el texto "Laptop" en el elemento "productSearch"
    And presiono el botón "searchButton"
    And presiono el botón "addToCart-laptop"
    And presiono el botón "checkout"
    And presiono el botón "confirmOrder"
    Then espero hasta que elemento "orderConfirmation" este visible
    And guardo texto del elemento "orderNumber" en variable temporal llamada "orderNumber"
    
    # 2. API: Validar orden en backend
    Given el host "https://api.shop.example.com" mas el contexto "/orders/{orderNumber}"
    And agrego el header "Authorization" con valor "Bearer admin-token"
    When ejecuto la consulta con el metodo "GET"
    Then valido que el codigo de respuesta del servicio sea 200
    And valido que el campo "status" del response sea "pending"
    And valido que el campo "items[0].product" del response contenga "Laptop"
```

---

## ⚠️ Troubleshooting

### Error: "Element not found"

**Causa:** Elemento no existe o tarda en aparecer.

**Solución:**
```gherkin
# ✅ CORRECTO - Usar espera inteligente
And espero hasta que elemento "button" este visible
Then verifico si existe el elemento "button"

# ❌ INCORRECTO - Verificación inmediata
Then verifico si existe el elemento "button"
```

### Error: "StaleElementReferenceException"

**Causa:** El DOM se actualizó (AJAX/React).

**Solución:** El framework **ya tiene retry logic automático**. Si persiste:
```gherkin
# Esperar que la página esté completamente lista
And espero hasta que la página termine de cargar
```

### Error: "Element is not clickable"

**Causa:** Elemento oculto por otro elemento o aún no es clickable.

**Solución:**
```gherkin
# Esperar que sea clickable
And espero hasta que elemento "button" este habilitado

# O hacer scroll al elemento
And hago scroll hasta el elemento "button"
```

### Error: "SessionNotCreatedException"

**Causa:** Versión incompatible de ChromeDriver/Chrome.

**Solución:** El framework usa WebDriverManager que **descarga automáticamente** el driver correcto. Si falla:
```bash
# Limpiar caché
rm -rf ~/.cache/selenium/

# Volver a ejecutar
./gradlew test
```

### Warning: "Sleep usado: 5000ms"

**Causa:** Usando esperas hardcodeadas.

**Solución:**
```gherkin
# ❌ EVITAR
And espero un tiempo de "5" segundos

# ✅ USAR
And espero hasta que elemento "dashboard" este visible
```

---

## 📚 Dependencias

| Librería | Versión | Propósito |
|----------|---------|-----------|
| **common** | 1.0.2 | Capa base |
| **Selenium** | 4.27.0 | WebDriver |
| **WebDriverManager** | 5.6.2 | Driver management |
| **Cucumber** | 7.18.0 | BDD Framework |
| **AssertJ** | 3.24.2 | Assertions |
| **AShot** | 1.5.4 | Screenshots |
| **Awaitility** | 4.2.0 | Waits |

---

## 🎯 Navegadores Soportados

| Navegador | Versión Mínima | Driver | Estado |
|-----------|----------------|--------|--------|
| **Chrome** | 120+ | ChromeDriver | ✅ Completo |
| **Firefox** | 115+ | GeckoDriver | ✅ Completo |
| **Edge** | 120+ | EdgeDriver | ✅ Completo |
| **Safari** | 17+ | SafariDriver | ✅ Completo |

---

## 🔗 Enlaces Relacionados

- **[🚀 Quick Reference](./QUICK-REFERENCE.md)** - Cheat sheet rápida de steps
- **[Common README](../common/README.md)** - Capa base del framework
- **[API Core README](../api-core/README.md)** - Testing REST
- **[Framework Guide](../FRAMEWORK-GUIDE.md)** - Guía completa
- **[Troubleshooting](../TROUBLESHOOTING.md)** - Solución de problemas

---

<div align="center">

**[⬆ Volver arriba](#-web-core---framework-de-testing-web-ui)**

**Versión:** 1.0.2 | **Autor:** Abel Venero | **QA Team - Scotia Bank**

</div>

