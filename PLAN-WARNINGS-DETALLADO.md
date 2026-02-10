# 🔬 PLAN DETALLADO: Corrección de Warnings

> **Objetivo:** Eliminar todos los warnings de compilación  
> **Duración:** 1 semana (35-40 horas)  
> **Prioridad:** Alta

---

## 📋 INVENTARIO COMPLETO DE WARNINGS

### **Módulo: common/**

| Archivo | Línea | Warning | Prioridad | Esfuerzo |
|---------|-------|---------|-----------|----------|
| `DataUtilities.java` | 105 | Raw type `Map` | 💥 P1 | 2h |
| `DataUtilities.java` | 107 | Raw type `Map` | 💥 P1 | 2h |
| `DataUtilities.java` | 109-110 | Raw type `Map` | 💥 P1 | 2h |
| `DataUtilities.java` | 113-114 | Raw type `Map` | 💥 P1 | 2h |
| `DataUtilities.java` | 658 | Unchecked cast `(Map<String, Object>)` | ⚠️ P2 | 1h |
| `DataUtilities.java` | 663 | Unchecked cast `(List<Object>)` | ⚠️ P2 | 1h |
| `BaseConfigurationProvider.java` | 96 | Raw type `Map` | 💥 P1 | 1h |
| `BaseConfigurationProvider.java` | 97 | Raw type `Map` | 💥 P1 | 1h |
| `BaseConfigurationProvider.java` | 247 | Unchecked cast | ⚠️ P2 | 0.5h |
| `BaseConfigurationProvider.java` | 260 | Unchecked cast | ⚠️ P2 | 0.5h |
| `BaseConfigurationProvider.java` | 639 | Unchecked cast | ⚠️ P2 | 0.5h |
| `HttpResponse.java` | 14 | Field should be final | 📘 P3 | 0.5h |
| `CucumberResultAdapter.java` | 188 | Raw type `List` | ⚠️ P2 | 1h |
| `QueryRepository.java` | 166 | Raw types in generics | ⚠️ P2 | 1h |

**Subtotal common:** ~16 horas

---

### **Módulo: web-core/**

| Archivo | Línea | Warning | Prioridad | Esfuerzo |
|---------|-------|---------|-----------|----------|
| `WebHelper.java` | 1014 | XPath injection risk | 💥 P1 | 2h |
| `WebHelper.java` | 109 | Raw type `List` | ⚠️ P2 | 1h |
| `BasePage.java` | N/A | Uses deprecated API | ⚠️ P2 | 1h |
| `TableComponent.java` | 24 | Raw type `List` | ⚠️ P2 | 0.5h |
| `WebHelper.java` | Varios | `driver.findElement()` directo | 📘 P3 | 3h |

**Subtotal web-core:** ~7.5 horas

---

### **Módulo: mobile-core/**

| Archivo | Línea | Warning | Prioridad | Esfuerzo |
|---------|-------|---------|-----------|----------|
| `MobileDriverFactory.java` | 24-34 | Deprecated `DesiredCapabilities` | 💥 P1 | 4h |
| `MobileDriverFactory.java` | 40-48 | Deprecated constructor | 💥 P1 | 2h |
| `BaseScreen.java` | N/A | Review for deprecations | 📘 P3 | 1h |

**Subtotal mobile-core:** ~7 horas

---

### **Módulo: api-core/**

| Archivo | Línea | Warning | Prioridad | Esfuerzo |
|---------|-------|---------|-----------|----------|
| `BaseDatabaseService.java` | 168 | Raw type in cast | ⚠️ P2 | 1h |
| `BaseDatabaseService.java` | 314 | Raw type `List` | ⚠️ P2 | 1h |
| `DatabaseTestUtilities.java` | Varios | Propagated warnings | 📘 P3 | 1h |

**Subtotal api-core:** ~3 horas

---

## 🎯 CORRECCIONES DETALLADAS

---

### **1. DataUtilities.java - Variables de Clase**

**Ubicación:** Líneas 105-114

**ANTES:**
```java
// Store de variables thread-safe para todos los frameworks
private static final Map<String, String> variableStore = new ConcurrentHashMap<>();
// Store de objetos complejos thread-safe (nuevo - para deserialización)
private static final Map<String, Object> objectStore = new ConcurrentHashMap<>();
// Store de variables con namespace thread-safe (para parallel execution)
// Estructura: Map<namespace, Map<key, value>>
private static final Map<String, Map<String, String>> namespacedVariableStore = new ConcurrentHashMap<>();
// Store de objetos con namespace thread-safe
// Estructura: Map<namespace, Map<key, object>>
private static final Map<String, Map<String, Object>> namespacedObjectStore = new ConcurrentHashMap<>();
```

**DESPUÉS:**
```java
// Store de variables thread-safe para todos los frameworks
private static final ConcurrentHashMap<String, String> variableStore = new ConcurrentHashMap<>();

// Store de objetos complejos thread-safe (nuevo - para deserialización)
private static final ConcurrentHashMap<String, Object> objectStore = new ConcurrentHashMap<>();

// Store de variables con namespace thread-safe (para parallel execution)
// Estructura: Map<namespace, Map<key, value>>
private static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> namespacedVariableStore = 
    new ConcurrentHashMap<>();

// Store de objetos con namespace thread-safe
// Estructura: Map<namespace, Map<key, object>>
private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> namespacedObjectStore = 
    new ConcurrentHashMap<>();
```

**Impacto:** Ninguno - Solo mejora type safety  
**Tests:** Validar que operaciones thread-safe funcionen

---

### **2. MobileDriverFactory.java - Actualizar a Appium 8+**

**Ubicación:** Líneas 24-48

**ANTES (Deprecado):**
```java
public static AppiumDriver createDriver(Platform platform, 
    DesiredCapabilities capabilities, String appiumUrl) {
    
    AppiumDriver driver;
    try {
        URL serverUrl = new URL(appiumUrl);
        switch (platform) {
            case ANDROID:
                driver = new AndroidDriver(serverUrl, capabilities);
                break;
            case IOS:
                driver = new IOSDriver(serverUrl, capabilities);
                break;
            default:
                throw new IllegalArgumentException("Platform no soportado: " + platform);
        }
    } catch (MalformedURLException e) {
        logger.error("URL de Appium inválida: {}", appiumUrl, e);
        throw new RuntimeException("URL inválida", e);
    }
    
    configureDriver(driver);
    return driver;
}
```

**DESPUÉS (Appium 8+):**
```java
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * Crea un driver móvil con capabilities (backward compatibility).
 * 
 * @deprecated Usar {@link #createDriver(Platform, AbstractDriverOptions, String)}
 */
@Deprecated
public static AppiumDriver createDriver(Platform platform, 
    DesiredCapabilities capabilities, String appiumUrl) {
    
    // Convertir DesiredCapabilities a AbstractDriverOptions moderno
    AbstractDriverOptions<?> options = convertToModernOptions(platform, capabilities);
    return createDriver(platform, options, appiumUrl);
}

/**
 * Crea un driver móvil con opciones modernas (Appium 8+).
 */
public static AppiumDriver createDriver(Platform platform, 
    AbstractDriverOptions<?> options, String appiumUrl) {
    
    logger.info("Creando {} driver con Appium 8+", platform);
    
    try {
        URL serverUrl = new URL(appiumUrl);
        AppiumDriver driver;
        
        switch (platform) {
            case ANDROID:
                UiAutomator2Options androidOptions = (options instanceof UiAutomator2Options)
                    ? (UiAutomator2Options) options
                    : convertToAndroidOptions(options);
                driver = new AndroidDriver(serverUrl, androidOptions);
                break;
                
            case IOS:
                XCUITestOptions iosOptions = (options instanceof XCUITestOptions)
                    ? (XCUITestOptions) options
                    : convertToIOSOptions(options);
                driver = new IOSDriver(serverUrl, iosOptions);
                break;
                
            default:
                throw new IllegalArgumentException("Platform no soportada: " + platform);
        }
        
        configureDriver(driver);
        logger.info("✅ Driver {} creado exitosamente", platform);
        return driver;
        
    } catch (MalformedURLException e) {
        logger.error("❌ URL de Appium inválida: {}", appiumUrl, e);
        throw new RuntimeException("URL inválida: " + appiumUrl, e);
    }
}

/**
 * Convierte DesiredCapabilities legacy a UiAutomator2Options.
 */
private static UiAutomator2Options convertToAndroidOptions(AbstractDriverOptions<?> options) {
    UiAutomator2Options androidOptions = new UiAutomator2Options();
    
    if (options != null) {
        options.asMap().forEach((key, value) -> {
            if (value != null) {
                androidOptions.setCapability(key, value);
            }
        });
    }
    
    return androidOptions;
}

/**
 * Convierte DesiredCapabilities legacy a XCUITestOptions.
 */
private static XCUITestOptions convertToIOSOptions(AbstractDriverOptions<?> options) {
    XCUITestOptions iosOptions = new XCUITestOptions();
    
    if (options != null) {
        options.asMap().forEach((key, value) -> {
            if (value != null) {
                iosOptions.setCapability(key, value);
            }
        });
    }
    
    return iosOptions;
}

/**
 * Convierte DesiredCapabilities a opciones modernas según plataforma.
 */
private static AbstractDriverOptions<?> convertToModernOptions(Platform platform, 
    DesiredCapabilities capabilities) {
    
    switch (platform) {
        case ANDROID:
            UiAutomator2Options androidOpts = new UiAutomator2Options();
            capabilities.asMap().forEach(androidOpts::setCapability);
            return androidOpts;
            
        case IOS:
            XCUITestOptions iosOpts = new XCUITestOptions();
            capabilities.asMap().forEach(iosOpts::setCapability);
            return iosOpts;
            
        default:
            throw new IllegalArgumentException("Platform no soportada");
    }
}
```

**Beneficios:**
- ✅ Mantiene backward compatibility (método deprecado)
- ✅ Provee API moderna
- ✅ Facilita migración gradual
- ✅ Type-safe con opciones específicas

---

### **3. WebHelper.java - XPath Injection**

**Ubicación:** Línea 1014

**ANTES (Inseguro):**
```java
public void checkTextAndClic(String text) {
    WebDriver driver = DriverManager.getDriver();
    // ❌ PELIGRO: XPath injection
    WebElement element = driver.findElement(
        By.xpath("//*[contains(text(), '" + text + "')]")
    );
    element.click();
}
```

**DESPUÉS (Seguro):**
```java
public void checkTextAndClic(String text) {
    WebDriver driver = DriverManager.getDriver();
    
    // ✅ SEGURO: Escapar comillas
    String safeText = text.replace("'", "\\'");
    
    // ✅ MEJOR: Usar wait explícito
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(
        By.xpath(String.format("//*[contains(text(), '%s')]", safeText))
    ));
    
    // ✅ Click con retry
    wait.until(ExpectedConditions.elementToBeClickable(element));
    element.click();
    
    TestLogger.logDebug("WEB_HELPER", 
        "Click realizado en elemento con texto: " + text, null);
}
```

**Beneficios:**
- ✅ Previene XPath injection
- ✅ Espera explícita (más estable)
- ✅ Mejor logging

---

## 🔧 GUÍA DE CORRECCIÓN PASO A PASO

### **Paso 1: Preparar Entorno**

```bash
# Crear rama
git checkout develop
git pull origin develop
git checkout -b feature/eliminate-warnings

# Backup
git tag backup-before-warnings-fix
```

---

### **Paso 2: Corregir Common (Día 1-2)**

**2.1. DataUtilities.java**

```bash
# Abrir archivo
code common/src/main/java/com/scotia/qa/common/utils/DataUtilities.java

# Ubicar líneas 105-114
# Reemplazar:
Map → ConcurrentHashMap<K, V>

# Compilar
./gradlew :common:compileJava

# Verificar warnings
./gradlew :common:compileJava 2>&1 | grep "warning"
# Debe mostrar 0 warnings
```

**2.2. BaseConfigurationProvider.java**

```bash
# Similar proceso
# Líneas 96-97: Map → ConcurrentHashMap

./gradlew :common:compileJava
```

**2.3. HttpResponse.java**

```bash
# Línea 14: Agregar final
private final Map<String, String> headers;

./gradlew :common:compileJava
```

**Commit:**
```bash
git add common/
git commit -m "fix(common): eliminar warnings de tipos genéricos

- DataUtilities: tipos explícitos en Maps
- BaseConfigurationProvider: tipos explícitos
- HttpResponse: field final para inmutabilidad

Closes #XXX"
```

---

### **Paso 3: Corregir Web-Core (Día 3-4)**

**3.1. WebHelper.java**

```bash
# Ubicar checkTextAndClic (línea 1014)
# Aplicar corrección XPath injection

# Compilar
./gradlew :web-core:compileJava

# Verificar
./gradlew :web-core:compileJava 2>&1 | grep "deprecation"
```

**3.2. TableComponent.java**

```bash
# Línea 24: Agregar tipo genérico
List<String> names = new ArrayList<>();

./gradlew :web-core:compileJava
```

**Commit:**
```bash
git add web-core/
git commit -m "fix(web-core): actualizar APIs deprecadas de Selenium

- WebHelper: prevenir XPath injection
- WebHelper: usar waits explícitos
- TableComponent: tipos genéricos en Lists

Closes #XXX"
```

---

### **Paso 4: Corregir Mobile-Core (Día 5)**

**4.1. MobileDriverFactory.java**

```bash
# Aplicar refactor completo a Appium 8+
# Ver código de ejemplo arriba

./gradlew :mobile-core:compileJava

# Verificar
./gradlew :mobile-core:compileJava 2>&1 | grep "deprecation"
# Debe mostrar 0 warnings
```

**Commit:**
```bash
git add mobile-core/
git commit -m "fix(mobile-core): actualizar a Appium 8+ API

- MobileDriverFactory: usar UiAutomator2Options/XCUITestOptions
- Mantener backward compatibility con DesiredCapabilities
- Agregar métodos de conversión

BREAKING: Requiere Appium 8.0+

Closes #XXX"
```

---

### **Paso 5: Verificación Final (Día 6)**

```bash
# Build completo
./gradlew clean build

# Compilar con warnings detallados
./gradlew compileJava -Xlint:unchecked -Xlint:deprecation

# NO debe mostrar warnings
# Buscar: "warning:" en output

# Si hay warnings, corregir y repetir
```

---

### **Paso 6: Validación y Merge (Día 7)**

```bash
# Publicar a maven local
./gradlew publishToMavenLocal

# Verificar que no rompimos nada
ls ~/.m2/repository/com/scotia/qa/

# Push
git push origin feature/eliminate-warnings

# PR a develop
# Título: "fix: Eliminar warnings de compilación y actualizar APIs deprecadas"
# Descripción: Ver template abajo
```

---

## 📝 TEMPLATE DE PULL REQUEST

```markdown
## 🔧 Descripción

Eliminación de todos los warnings de compilación y actualización de APIs deprecadas.

## ✅ Cambios Realizados

### Common Module
- ✅ DataUtilities: Tipos genéricos explícitos en Maps
- ✅ BaseConfigurationProvider: ConcurrentHashMap tipado
- ✅ HttpResponse: Field final para inmutabilidad

### Web-Core Module
- ✅ WebHelper: Corregir XPath injection
- ✅ WebHelper: Usar ExpectedConditions
- ✅ TableComponent: Tipos genéricos en Lists

### Mobile-Core Module
- ✅ MobileDriverFactory: Actualizar a Appium 8+ API
- ✅ Mantener backward compatibility
- ✅ UiAutomator2Options y XCUITestOptions

### API-Core Module
- ✅ BaseDatabaseService: Tipos genéricos

## 🧪 Testing

- ✅ Build exitoso: `./gradlew clean build`
- ✅ 0 warnings: `./gradlew compileJava -Xlint:all`
- ✅ Backward compatibility validada
- ⚠️  Tests unitarios: Pendiente Fase 2

## 📊 Impacto

- **Archivos modificados:** 12
- **Líneas cambiadas:** ~80
- **Warnings eliminados:** 100%
- **Breaking changes:** Ninguno (solo en mobile si usa Appium < 8.0)

## 🎯 Revisores

@lead-qa @senior-dev

## 📋 Checklist

- [ ] Código revisado
- [ ] Build exitoso
- [ ] 0 warnings
- [ ] Documentación actualizada
```

---

## 🎓 LECCIONES APRENDIDAS

### **Para Evitar Raw Types:**

✅ **SÍ hacer:**
```java
Map<String, Object> data = new HashMap<>();
List<String> names = new ArrayList<>();
ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
```

❌ **NO hacer:**
```java
Map data = new HashMap();
List names = new ArrayList();
```

### **Para Evitar APIs Deprecadas:**

✅ **Revisar release notes:**
- Selenium 4.x: https://www.selenium.dev/blog/2021/announcing-selenium-4/
- Appium 8.x: http://appium.io/docs/en/about-appium/appium-2/

✅ **Usar IDE warnings:**
- IntelliJ marca deprecaciones con strikethrough
- Mostrar quick-fix con Alt+Enter

---

## 📦 DEPENDENCIAS NECESARIAS (Mobile-Core)

Actualizar `mobile-core/build.gradle`:

```gradle
dependencies {
    // ✅ Actualizar a Appium 8+
    implementation 'io.appium:java-client:8.6.0'  // Antes: 7.x
    
    // Selenium ya actualizado
    implementation 'org.seleniumhq.selenium:selenium-java:4.16.1'
}
```

---

**Próximo Documento:** `PLAN-TESTS-DETALLADO.md`  
**Estado:** ✅ LISTO PARA REVISIÓN

