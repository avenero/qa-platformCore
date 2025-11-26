# 🤝 Guía de Contribución

**Framework:** QA Scotia Automation Framework  
**Última Actualización:** 26 de Noviembre, 2025

---

## 📑 Índice

- [🎯 Bienvenido](#-bienvenido)
- [🏗️ Setup del Entorno de Desarrollo](#️-setup-del-entorno-de-desarrollo)
  - [Prerrequisitos](#prerrequisitos)
  - [IntelliJ IDEA Setup](#intellij-idea-setup)
  - [Configuración Inicial](#configuración-inicial)
- [📋 Convenciones de Código](#-convenciones-de-código)
- [🔀 Flujo de Trabajo Git](#-flujo-de-trabajo-git)
- [✅ Testing](#-testing)
- [📝 Documentación](#-documentación)
- [🚀 Publicación y Versionado](#-publicación-y-versionado)
- [❓ Preguntas Frecuentes](#-preguntas-frecuentes)

---

## 🎯 Bienvenido

¡Gracias por contribuir al QA Scotia Automation Framework! Este documento te guiará en el proceso de contribución.

### ¿Cómo Puedes Contribuir?

- 🐛 **Reportar bugs** - Crea un issue con detalles del problema
- ✨ **Proponer features** - Abre un issue explicando tu idea
- 🔧 **Fix bugs** - Toma un issue existente y envía un PR
- 📝 **Mejorar documentación** - Siempre hay espacio para clarificar
- 🧪 **Agregar tests** - Más cobertura es siempre bienvenida

---

## 🏗️ Setup del Entorno de Desarrollo

### Prerrequisitos

Antes de empezar, asegúrate de tener instalado:

| Software | Versión Mínima | Comando de Verificación |
|----------|----------------|-------------------------|
| **Java JDK** | 21 LTS | `java -version` |
| **Gradle** | 8.14+ | `./gradlew --version` |
| **Git** | 2.x | `git --version` |
| **IntelliJ IDEA** | 2023.3+ | - |

#### Instalación de Java 21 (si no lo tienes)

**MacOS:**
```bash
brew install openjdk@21
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

**Windows:**
Descarga desde [Adoptium](https://adoptium.net/) o [Oracle](https://www.oracle.com/java/technologies/downloads/)

#### Verificar Instalación:
```bash
java -version
# Debería mostrar: openjdk version "21.x.x"
```

---

### IntelliJ IDEA Setup

#### 1️⃣ Clonar el Repositorio

```bash
git clone https://github.com/scotia-qa/qa-scotia-frameworks.git
cd qa-scotia-frameworks
```

#### 2️⃣ Abrir en IntelliJ

1. `File > Open...`
2. Selecciona la carpeta `qa-scotia-frameworks`
3. Click **Open as Project**
4. Espera a que Gradle termine de sincronizar (esquina inferior derecha)

#### 3️⃣ Configurar SDK

1. `File > Project Structure` (⌘ + ; en Mac / Ctrl + Alt + Shift + S en Windows/Linux)
2. En **Project**:
   - Project SDK: **21** (si no aparece, click en **Add SDK > Download JDK...**)
   - Project language level: **21**
3. Click **Apply** y **OK**

#### 4️⃣ Optimizar IntelliJ (IMPORTANTE)

**A. Excluir Directorios de Indexación:**

IntelliJ puede volverse lento si indexa carpetas innecesarias.

1. `File > Project Structure > Modules`
2. Expande el árbol del proyecto
3. Click derecho en estas carpetas y selecciona **"Mark Directory as > Excluded"**:
   - `.gradle/`
   - `.idea/`
   - `build/` (raíz)
   - `*/build/` (todos los módulos)
   - `out/`

**B. Aumentar Memoria de IntelliJ:**

1. `Help > Edit Custom VM Options...`
2. Si pregunta crear el archivo, acepta
3. Agrega/modifica estas líneas:

```properties
# Memoria inicial
-Xms512m

# Memoria máxima (ajusta según tu RAM)
-Xmx4096m

# Garbage Collector optimizado
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200

# Cache de código compilado
-XX:ReservedCodeCacheSize=512m

# Performance
-XX:+UseStringDeduplication
-XX:SoftRefLRUPolicyMSPerMB=50
```

4. Reinicia IntelliJ

**C. Configurar Gradle Settings:**

`File > Settings > Build, Execution, Deployment > Build Tools > Gradle`

- Build and run using: **Gradle**
- Run tests using: **Gradle**  
- Gradle JVM: **Project SDK (Java 21)**
- ☑️ **Download sources** (recomendado)
- ☑️ **Download documentation** (opcional)

**D. Invalidar Cachés (si hay problemas):**

```
File > Invalidate Caches... > Invalidate and Restart
```

#### 5️⃣ Instalar Plugins Recomendados

`File > Settings > Plugins`

- **Gherkin** (BDD Support) - ✅ Requerido para Cucumber
- **Cucumber for Java** - ✅ Requerido
- **Gradle** - ✅ Ya incluido
- **SonarLint** - 📊 Recomendado (análisis de calidad)
- **Save Actions** - 🔧 Opcional (formateo automático)

---

### Configuración Inicial

#### 1️⃣ Compilar el Framework

```bash
# Compilar todos los módulos
./gradlew build

# Si hay errores, limpiar y recompilar
./gradlew clean build
```

#### 2️⃣ Publicar a Maven Local

```bash
./gradlew publishToMavenLocal
```

Esto publica los módulos en `~/.m2/repository/`:
- `com.scotia.qa:common:1.0.2`
- `com.scotia.qa:api-core:1.0.2`
- `com.scotia.qa:web-core:1.0.2`
- `com.scotia.qa:mobile-core:1.0.2`

#### 3️⃣ Ejecutar Tests

```bash
# Todos los tests
./gradlew test

# Un módulo específico
./gradlew :web-core:test

# Un test específico
./gradlew :api-core:test --tests "*ApiStepsTest*"
```

---

## 📋 Convenciones de Código

### Estilo General

- ✅ **Idioma del código:** Inglés (clases, métodos, variables)
- ✅ **Idioma de documentación:** Español (comentarios, Javadoc, READMEs)
- ✅ **Idioma de Steps:** Español (Cucumber Gherkin)
- ✅ **Indentación:** 4 espacios (NO tabs)
- ✅ **Encoding:** UTF-8
- ✅ **Line endings:** LF (Unix style)

### Nomenclatura

#### Clases
```java
// ✅ CORRECTO - PascalCase
public class WebDriverFactory { }
public class ScenarioContext { }
public class ApiSteps { }

// ❌ INCORRECTO
public class webdriverfactory { }
public class Scenario_Context { }
```

#### Métodos
```java
// ✅ CORRECTO - camelCase
public void createDriver() { }
public boolean isElementVisible() { }

// ❌ INCORRECTO
public void CreateDriver() { }
public boolean is_element_visible() { }
```

#### Constantes
```java
// ✅ CORRECTO - UPPER_SNAKE_CASE
public static final int DEFAULT_TIMEOUT = 15;
public static final String BASE_URL = "https://example.com";

// ❌ INCORRECTO
public static final int defaultTimeout = 15;
```

#### Variables
```java
// ✅ CORRECTO - camelCase, descriptivo
String userName = "john.doe";
WebDriver driver = createDriver();
int maxRetries = 3;

// ❌ INCORRECTO
String un = "john.doe";
WebDriver d = createDriver();
int x = 3;
```

### Comentarios y Documentación

#### Javadoc Obligatorio Para:
- Clases públicas
- Métodos públicos
- Interfaces
- Métodos complejos

```java
/**
 * Crea una instancia de WebDriver según la configuración.
 *
 * <p>Este método lee la configuración de {@code web-config.properties}
 * y crea el driver apropiado (Chrome, Firefox, Edge, Safari).</p>
 *
 * <p><b>Ejemplo de uso:</b></p>
 * <pre>
 * WebDriver driver = WebDriverFactory.createDriver();
 * driver.get("https://example.com");
 * </pre>
 *
 * @param browserType Tipo de navegador (CHROME, FIREFOX, EDGE, SAFARI)
 * @param executionMode Modo de ejecución (LOCAL o GRID)
 * @return WebDriver configurado y listo para usar
 * @throws IllegalStateException si el browser no está soportado
 * @see WebDriverConfig
 */
public static WebDriver createDriver(BrowserType browserType, ExecutionMode executionMode) {
    // ...
}
```

#### Comentarios Inline
```java
// ✅ CORRECTO - Explica el "por qué", no el "qué"
// Usamos ThreadLocal para soportar tests en paralelo
private static ThreadLocal<WebDriver> driverThread = new ThreadLocal<>();

// ❌ INCORRECTO - Obvio
// Crea una variable
private static ThreadLocal<WebDriver> driverThread = new ThreadLocal<>();
```

### Manejo de Excepciones

```java
// ✅ CORRECTO - Específico y con contexto
try {
    element.click();
} catch (StaleElementReferenceException e) {
    TestLogger.logWarning("WEB_HELPER", 
        "Elemento stale, reintentando: " + locator, e);
    retryClick(locator);
}

// ❌ INCORRECTO - Genérico y sin contexto
try {
    element.click();
} catch (Exception e) {
    e.printStackTrace();
}
```

### Logging

```java
// ✅ CORRECTO - Niveles apropiados
TestLogger.logDebug("MODULE", "Valor de variable x: " + x, null);
TestLogger.logInfo("MODULE", "Usuario autenticado exitosamente", null);
TestLogger.logWarning("MODULE", "Timeout esperando elemento", null);
TestLogger.logError("MODULE", "Falló autenticación", exception);

// ❌ INCORRECTO - System.out
System.out.println("Debug: " + x);
```

### Tests Unitarios

```java
// ✅ CORRECTO - Nombres descriptivos
@Test
void shouldCreateChromeDriverWhenBrowserTypeIsChrome() {
    // Arrange
    BrowserType browserType = BrowserType.CHROME;
    
    // Act
    WebDriver driver = WebDriverFactory.createDriver(browserType);
    
    // Assert
    assertThat(driver).isInstanceOf(ChromeDriver.class);
}

// ❌ INCORRECTO - Nombres vagos
@Test
void test1() {
    WebDriver driver = WebDriverFactory.createDriver(BrowserType.CHROME);
    assertNotNull(driver);
}
```

---

## 🔀 Flujo de Trabajo Git

### Branches

El proyecto usa **Git Flow** simplificado:

```
main (producción)
  └── develop (desarrollo)
       ├── feature/JIRA-123-descripcion
       ├── bugfix/JIRA-456-descripcion
       └── hotfix/JIRA-789-descripcion
```

### Crear un Branch

```bash
# Actualizar develop
git checkout develop
git pull origin develop

# Crear feature branch
git checkout -b feature/JIRA-123-agregar-validacion-api

# Trabajar en tu branch
# ... hacer cambios ...

# Commit
git add .
git commit -m "feat(api-core): agregar validación de schema JSON"

# Push
git push origin feature/JIRA-123-agregar-validacion-api
```

### Convenciones de Commits

Usamos **[Conventional Commits](https://www.conventionalcommits.org/)**:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

#### Types:

- `feat`: Nueva funcionalidad
- `fix`: Fix de bug
- `docs`: Cambios en documentación
- `style`: Formato (sin cambio de lógica)
- `refactor`: Refactorización
- `test`: Agregar o modificar tests
- `chore`: Tareas de mantenimiento

#### Scopes:

- `common`: Cambios en módulo common
- `api-core`: Cambios en api-core
- `web-core`: Cambios en web-core
- `mobile-core`: Cambios en mobile-core

#### Ejemplos:

```bash
# Nueva funcionalidad
git commit -m "feat(web-core): agregar soporte para Safari"

# Fix de bug
git commit -m "fix(api-core): corregir parsing de JSON con nulls"

# Documentación
git commit -m "docs(web-core): actualizar README con ejemplos de waits"

# Refactorización
git commit -m "refactor(common): simplificar ScenarioContext API"

# Tests
git commit -m "test(api-core): agregar tests para validaciones OAuth"
```

### Pull Requests

#### Antes de crear un PR:

```bash
# 1. Actualizar tu branch con develop
git checkout develop
git pull origin develop
git checkout feature/tu-branch
git merge develop

# 2. Resolver conflictos si existen

# 3. Compilar y ejecutar tests
./gradlew clean build test

# 4. Push
git push origin feature/tu-branch
```

#### Template de PR:

```markdown
## 🎯 Descripción
Brief description of what this PR does.

## 🔗 JIRA Ticket
[JIRA-123](https://jira.scotiabank.com/browse/JIRA-123)

## 🧪 Tipo de Cambio
- [ ] 🐛 Bug fix
- [ ] ✨ Nueva funcionalidad
- [ ] 💥 Breaking change
- [ ] 📝 Documentación
- [ ] 🔧 Refactorización

## ✅ Checklist
- [ ] Código compila sin errores
- [ ] Tests pasando
- [ ] Documentación actualizada
- [ ] Javadoc agregado
- [ ] Sin warnings de compilación

## 📸 Screenshots (si aplica)
```

#### Revisión de Código

Tu PR será revisado por al menos **1 tech lead**. Esperamos:
- ✅ Código siguiendo convenciones
- ✅ Tests con cobertura >80%
- ✅ Documentación clara
- ✅ Sin code smells (SonarLint)

---

## ✅ Testing

### Estrategia de Testing

Cada módulo debe tener:
1. **Unit Tests** - Lógica de negocio (>80% coverage)
2. **Integration Tests** - Integración con servicios externos
3. **E2E Tests** (opcional) - Flujos completos

### Escribir Tests

#### Ubicación:
```
web-core/
  src/
    main/java/...
    test/java/...  ← Tests aquí
```

#### Ejemplo:
```java
package com.scotia.qa.webcore.driver;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class WebDriverFactoryTest {

    @Test
    @DisplayName("Debe crear ChromeDriver cuando browserType es CHROME")
    void shouldCreateChromeDriverWhenBrowserTypeIsChrome() {
        // Arrange
        BrowserType browserType = BrowserType.CHROME;
        
        // Act
        WebDriver driver = WebDriverFactory.createDriver(browserType);
        
        // Assert
        assertThat(driver).isNotNull();
        assertThat(driver).isInstanceOf(ChromeDriver.class);
        
        // Cleanup
        driver.quit();
    }
    
    @Test
    @DisplayName("Debe lanzar excepción cuando browserType es null")
    void shouldThrowExceptionWhenBrowserTypeIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> WebDriverFactory.createDriver(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("browserType no puede ser null");
    }
}
```

### Ejecutar Tests

```bash
# Todos los tests
./gradlew test

# Un módulo
./gradlew :web-core:test

# Con reporte HTML
./gradlew test
# Abre: build/reports/tests/test/index.html

# Con cobertura
./gradlew test jacocoTestReport
# Abre: build/reports/jacoco/test/html/index.html
```

---

## 📝 Documentación

### Actualizar Documentación

Cuando agregas/modificas funcionalidad, **DEBES** actualizar:

1. **Javadoc** en el código
2. **README.md** del módulo correspondiente
3. **FRAMEWORK-GUIDE.md** si afecta la arquitectura
4. **TROUBLESHOOTING.md** si resuelve un error común

### Ejemplo:

```java
// Agregaste un nuevo método en WebHelper
public void waitForElementToDisappear(String locator) {
    // ...
}
```

**Entonces actualizas:**

1. `web-core/README.md`:
```markdown
### WaitUtils

#### `waitForElementToDisappear(String locator)`
Espera a que un elemento desaparezca del DOM.

**Ejemplo:**
```java
helper.waitForElementToDisappear("loadingSpinner");
```

---

## 🚀 Publicación y Versionado

### Versionado Semántico

Usamos [SemVer](https://semver.org/): `MAJOR.MINOR.PATCH`

- **MAJOR**: Cambios incompatibles (breaking changes)
- **MINOR**: Nueva funcionalidad (backward compatible)
- **PATCH**: Bug fixes

**Versión Actual:** `1.0.2`

### Cómo Publicar una Nueva Versión

#### 1. Actualizar Versión

**`build.gradle` (raíz):**
```groovy
allprojects {
    group = 'com.scotia.qa'
    version = '1.1.0'  // ⬅️ Incrementar aquí
}
```

#### 2. Compilar y Publicar

```bash
# Limpiar
./gradlew clean

# Compilar
./gradlew build

# Publicar a Maven Local
./gradlew publishToMavenLocal

# Publicar a Maven Central (solo maintainers)
./gradlew publish
```

#### 3. Tag en Git

```bash
git tag -a v1.1.0 -m "Release 1.1.0 - Descripción"
git push origin v1.1.0
```

#### 4. Release Notes

Crear en GitHub un **Release** con:
- Changelog
- Breaking changes
- Migration guide (si aplica)

---

## ❓ Preguntas Frecuentes

### ¿Puedo usar libraries externas?

Sí, pero con aprobación del tech lead. Considera:
- ✅ Licencia compatible
- ✅ Mantenida activamente
- ✅ Sin vulnerabilidades conocidas

### ¿Cómo reporto un bug?

1. Busca en [Issues](https://github.com/scotia-qa/qa-scotia-frameworks/issues) si ya existe
2. Si no, crea uno nuevo con template:
   - Descripción del bug
   - Pasos para reproducir
   - Comportamiento esperado vs actual
   - Screenshots/logs
   - Versión del framework

### ¿Puedo trabajar en mobile-core?

Sí, pero ten en cuenta que está en **Beta**. Coordina con el equipo mobile.

### ¿Necesito permisos especiales?

Para **contribuir (PRs):** No  
Para **merge a develop:** Sí (Tech Leads)  
Para **merge a main:** Sí (Maintainers)

---

## 📞 Contacto

¿Dudas sobre cómo contribuir?

- 📧 Email: qa-team@scotiabank.com
- 💬 Slack: #qa-automation-dev
- 🙋 Tech Leads: Abel Venero, [Otros]

---

<div align="center">

**[⬆ Volver arriba](#-guía-de-contribución)**

¡Gracias por contribuir! 🙏

</div>

