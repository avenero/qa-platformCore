# 🚀 Web-Core - Quick Reference

> Cheat sheet rápida de los steps más usados de web-core. Para documentación completa, ver [README.md](./README.md).

---

## 📑 Índice Rápido

- [Configuración de Driver](#configuración-de-driver) ⭐ NUEVO
- [Navegación](#navegación)
- [Interacciones](#interacciones)
- [Esperas](#esperas)
- [Validaciones](#validaciones)
- [Variables](#variables)
- [Screenshots](#screenshots)

---

## Configuración de Driver

### ⭐ NUEVO (v1.2.0): Configurar navegador desde Gherkin

```gherkin
# Configurar navegador y modo headless
Given configuro el driver del navegador "chrome" en modo headless "false"
Given configuro el driver del navegador "firefox" en modo headless "true"
Given configuro el driver del navegador "edge" en modo headless "false"

# Navegadores soportados: chrome, firefox, edge, safari
# Headless: true/false, yes/no, si/no, 1/0

# Si NO usas este step, usa el navegador por defecto (config-qa.properties)
```

**💡 Casos de uso:**

```gherkin
# Desarrollo local - Ver UI
Given configuro el driver del navegador "chrome" en modo headless "false"

# Pipeline CI/CD - Sin UI
Given configuro el driver del navegador "chrome" en modo headless "true"

# Cross-browser testing
Scenario Outline: Login en <browser>
  Given configuro el driver del navegador "<browser>" en modo headless "true"
  When navego a la URL "https://app.com"
  
  Examples:
    | browser |
    | chrome  |
    | firefox |
    | edge    |
```

**🔧 Configuración (config-qa.properties):**

```properties
# Navegador por defecto (si no usas el step)
web.browser=chrome

# Headless por defecto
web.headless=false

# Versión de drivers (automático)
driver.chrome.version=143.0.7499.41
driver.firefox.version=0.35.0
driver.edge.version=130.0.2849.68

# Estrategia de descarga
driver.strategy=artifactory  # o "local"
```

---

## Configuración de Driver

### ⭐ NUEVO (v1.2.0): Configurar navegador desde Gherkin

```gherkin
# Configurar navegador y modo headless
Given configuro el driver del navegador "chrome" en modo headless "false"
Given configuro el driver del navegador "firefox" en modo headless "true"
Given configuro el driver del navegador "edge" en modo headless "false"

# Navegadores soportados: chrome, firefox, edge, safari
# Headless: true/false, yes/no, si/no, 1/0
```

**💡 Casos de uso:**

```gherkin
# Desarrollo local - Ver UI
@web
Scenario: Login visual
  Given configuro el driver del navegador "chrome" en modo headless "false"
  When navego a la URL "https://app.com/login"
  And ingreso "user@mail.com" en el campo "email"

# Pipeline CI/CD - Sin UI
@web
Scenario: Login automatizado
  Given configuro el driver del navegador "chrome" en modo headless "true"
  When navego a la URL "https://app.com/login"

# Cross-browser testing
@web
Scenario Outline: Login en <browser>
  Given configuro el driver del navegador "<browser>" en modo headless "true"
  When navego a la URL "https://app.com"
  
  Examples:
    | browser |
    | chrome  |
    | firefox |
    | edge    |
```

**📋 Configuración (config-qa.properties):**

```properties
# Navegador por defecto (si no usas el step)
web.browser=chrome

# Headless por defecto
web.headless=false

# Versión de drivers (automático desde Artifactory)
driver.chrome.version=143.0.7499.41
driver.firefox.version=0.35.0
driver.edge.version=130.0.2849.68

# Estrategia de descarga
driver.strategy=artifactory  # o "local"
driver.artifactory.base.url=${ARTIFACTORY_BASE_URL}
driver.artifactory.user=${ARTIFACTORY_USER}
driver.artifactory.token=${ARTIFACTORY_TOKEN}
```

---

## Navegación

```gherkin
# Navegar a URL
Given actualizo URL en el navegador "https://example.com"
When navego a la URL "https://example.com/page"

# Recarga
And recargo la página

# Navegación browser
And voy hacia atrás en el navegador
And voy hacia adelante en el navegador
```

---

## Interacciones

### Click

```gherkin
When presiono el botón "loginButton"
And hago click en el elemento "submitButton"
And realizo click en "acceptTerms"

# Click JavaScript (para elementos ocultos)
And hago click con JavaScript en "hiddenButton"

# Click derecho
And hago click derecho en el elemento "contextMenu"

# Doble click
And hago doble click en el elemento "file"
```

### Typing

```gherkin
# Ingresar texto
When ingreso el texto "john.doe" en el elemento "username"
And escribo "password123" en el campo "password"

# Con variable
And ingreso el texto "{authToken}" en el elemento "tokenField"

# Limpiar y escribir
And limpio el campo "searchBox"
When ingreso el texto "Laptop" en el elemento "searchBox"

# Presionar Enter
And presiono Enter en el elemento "searchBox"
```

### Selects/Dropdowns

```gherkin
# Por texto visible
When selecciono el texto "United States" en el combobox "country"

# Por valor
And selecciono la opción con valor "US" en el combobox "country"

# Por índice
And selecciono la opción en posición "2" del combobox "country"
```

### Checkbox/Radio

```gherkin
# Checkbox
When selecciono el checkbox "acceptTerms"
And deselecciono el checkbox "newsletter"

# Radio button
When selecciono el radio button con valor "male"
```

### Otros

```gherkin
# Hover
When sitúo el cursor sobre el elemento "menuItem"

# Scroll
And hago scroll hasta el elemento "footer"
And hago scroll hacia "arriba"
And hago scroll hacia "abajo"

# iFrame
When cambio al iframe con path "paymentFrame"
And salgo del iframe

# Ventanas
When cambio a la nueva ventana
And cierro la ventana actual
And vuelvo a la ventana principal
```

---

## Esperas

### Esperas Inteligentes (Recomendadas)

```gherkin
# Esperar visible
And espero hasta que elemento "dashboard" este visible

# Esperar no visible (loading, spinner)
And espero hasta que elemento "loadingSpinner" no este visible

# Esperar habilitado
And espero hasta que elemento "submitButton" este habilitado

# Esperar clickable
And espero hasta que elemento "button" sea clickable
```

### Esperas de Tiempo (Evitar)

```gherkin
# ⚠️ Solo usar si es absolutamente necesario
And espero un tiempo de "5" segundos
```

**Mejor usar:** Esperas inteligentes que esperan condiciones específicas.

---

## Validaciones

### Existencia

```gherkin
# Existe
Then verifico si existe el elemento "welcomeMessage"
And verifico que existe el elemento "header"

# No existe
And verifico que no exista el elemento "errorMessage"
```

### Texto

```gherkin
# Texto exacto
Then verifico que el texto en "userName" sea "John Doe"

# Contiene texto
And verifico que el texto en "welcome" contenga el texto "Welcome"

# Con variable
Then verifico que el texto en "email" sea "{userEmail}"
```

### Estados

```gherkin
# Habilitado/Deshabilitado
Then verifico que el elemento "submitButton" este habilitado
And verifico que el elemento "submitButton" este deshabilitado

# Visible/No visible
Then verifico que el elemento "banner" este visible
And verifico que el elemento "modal" no este visible

# Seleccionado (checkbox/radio)
Then verifico que el checkbox "terms" este seleccionado
```

### Validaciones Condicionales

```gherkin
# Si existe, validar texto
Then verifico si existe el elemento "userName" y valido que el texto sea "John"

# Si existe, hacer click
And verifico si existe el elemento "banner" y hago clic

# Si existe, seleccionar opción
And verifico si existe el combobox "country" y selecciono el valor "USA"
```

---

## Variables

### Guardar

```gherkin
# Guardar texto de elemento
And guardo texto del elemento "orderNumber" en variable temporal llamada "orderNumber"

# Guardar texto directo
And guardo texto "admin@example.com" en variable temporal llamada "adminEmail"
```

### Usar

```gherkin
# Usar en campos
And ingreso el texto "{orderNumber}" en el elemento "searchBox"

# Usar en validaciones
Then verifico que el texto en "orderDisplay" sea "{orderNumber}"

# Usar en URLs
Given actualizo URL en el navegador "https://app.com/orders/{orderNumber}"
```

### Disponibilidad

Las variables guardadas en **ScenarioContext** están disponibles en:
- ✅ Otros steps del mismo escenario
- ✅ Otras capas (API, Web, Mobile)
- ❌ Otros escenarios (se limpian automáticamente)

---

## Screenshots

```gherkin
# Screenshot automático (en cada step importante)
# Ya se toman automáticamente

# Screenshot manual
When capturo una imagen de la pantalla
And tomo screenshot con nombre "estado_actual"
```

---

## 🎯 Patrones Comunes

### Login Completo

```gherkin
Given actualizo URL en el navegador "https://app.com/login"
And espero hasta que elemento "username" este visible
When ingreso el texto "john.doe" en el elemento "username"
And ingreso el texto "password123" en el elemento "password"
And presiono el botón "loginButton"
Then espero hasta que elemento "dashboard" este visible
And verifico si existe el elemento "welcomeMessage"
```

### Formulario con Validación

```gherkin
When ingreso el texto "John" en el elemento "firstName"
And ingreso el texto "Doe" en el elemento "lastName"
And ingreso el texto "john@example.com" en el elemento "email"
And selecciono el texto "United States" en el combobox "country"
And selecciono el checkbox "acceptTerms"
And presiono el botón "submitButton"
Then espero hasta que elemento "successMessage" este visible
And verifico que el texto en "successMessage" contenga "Success"
```

### Búsqueda y Validación

```gherkin
When ingreso el texto "ORD-12345" en el elemento "searchBox"
And presiono el botón "searchButton"
And espero hasta que elemento "resultsTable" este visible
And espero hasta que elemento "loadingSpinner" no este visible
Then verifico si existe el elemento "orderRow-ORD-12345"
And verifico que el texto en "orderStatus" sea "Shipped"
```

### Modal/Dialog

```gherkin
When presiono el botón "openModal"
And espero hasta que elemento "modal" este visible
Then verifico que el texto en "modalTitle" sea "Confirmation"
When presiono el botón "confirmButton"
And espero hasta que elemento "modal" no este visible
```

---

## ⚠️ Anti-Patrones (Evitar)

### ❌ Usar sleeps en lugar de waits

```gherkin
# ❌ MAL
And espero un tiempo de "5" segundos
Then verifico si existe el elemento "button"

# ✅ BIEN
And espero hasta que elemento "button" este visible
Then verifico si existe el elemento "button"
```

### ❌ No esperar antes de validar

```gherkin
# ❌ MAL - Puede fallar si el elemento tarda
When presiono el botón "submit"
Then verifico si existe el elemento "successMessage"

# ✅ BIEN - Esperar primero
When presiono el botón "submit"
And espero hasta que elemento "successMessage" este visible
Then verifico si existe el elemento "successMessage"
```

### ❌ XPath complejos

```gherkin
# ❌ MAL - Frágil y lento
And presiono el botón "//div[@class='container']//form//button[@type='submit']"

# ✅ BIEN - Usar ID o CSS
And presiono el botón "submitButton"
# O
And presiono el botón "#submit-btn"
```

---

## 🔗 Enlaces

- **[README.md](./README.md)** - Documentación completa de web-core
- **[Troubleshooting](../TROUBLESHOOTING.md)** - Solución de problemas
- **[Framework Guide](../FRAMEWORK-GUIDE.md)** - Guía maestra del framework

---

## 💡 Tips

1. **Siempre espera visibilidad** antes de interactuar
2. **Usa IDs** cuando sea posible para localizadores
3. **Guarda datos** en variables para reutilizar
4. **Evita sleeps** - Usa esperas inteligentes
5. **Toma screenshots** en puntos clave del flujo
6. **Valida estados** (enabled, visible) antes de acciones

---

<div align="center">

**[⬆ Volver arriba](#-web-core---quick-reference)**

**Para documentación completa:** [README.md](./README.md)

</div>

