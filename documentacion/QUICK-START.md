# 🚀 Scotia QA Framework - Guía de Inicio Rápido

Guía completa paso a paso para configurar y ejecutar tu primer módulo de testing usando el framework Scotia QA.

---

## 📑 Índice

- [📖 Antes de Empezar](#-antes-de-empezar)
- [⚙️ Prerequisitos](#️-prerequisitos)
- [🎯 Flujo Completo (Diagrama)](#-flujo-completo-diagrama)
- [📦 Paso 1: Configuración del Framework](#-paso-1-configuración-del-framework)
- [🏗️ Paso 2: Crear Tu Módulo de Testing](#️-paso-2-crear-tu-módulo-de-testing)
- [⚙️ Paso 3: Configurar el Módulo](#️-paso-3-configurar-el-módulo)
- [✍️ Paso 4: Escribir Tu Primer Test](#️-paso-4-escribir-tu-primer-test)
- [▶️ Paso 5: Ejecutar los Tests](#️-paso-5-ejecutar-los-tests)
- [🔍 Paso 6: Revisar Resultados](#-paso-6-revisar-resultados)
- [🚀 Próximos Pasos](#-próximos-pasos)
- [🐛 Troubleshooting](#-troubleshooting)

---

## 📖 Antes de Empezar

### ¿Qué vamos a hacer?

En esta guía vas a:

1. ✅ Clonar y compilar el framework Scotia QA
2. ✅ Crear tu propio módulo de testing (proyecto independiente)
3. ✅ Configurar conexión a base de datos y APIs
4. ✅ Escribir tu primer test en Gherkin + Cucumber
5. ✅ Ejecutar y ver resultados

### Tiempo estimado

- ⏱️ **Primera vez**: ~30-45 minutos
- ⏱️ **Usuarios experimentados**: ~10-15 minutos

### Estructura que crearemos

```
📁 workspace/
├── 📁 qa-scotia-frameworks/     ← El framework (librería)
│   ├── common/
│   ├── api-core/
│   ├── web-core/
│   └── mobile-core/
│
└── 📁 qa-module-mi-proyecto/    ← Tu módulo (tests)
    ├── src/test/
    │   ├── java/                 ← Step Definitions
    │   └── resources/
    │       ├── features/         ← Tests en Gherkin
    │       └── config-scotia.properties  ← Configuración
    ├── .env.local                ← Credenciales (gitignored)
    └── build.gradle              ← Dependencias
```

---

## ⚙️ Prerequisitos

### Software Requerido

| Software | Versión Mínima | Instalación |
|----------|----------------|-------------|
| **Java JDK** | 21 (LTS) | [Oracle](https://www.oracle.com/java/technologies/downloads/) o [OpenJDK](https://adoptium.net/) |
| **Gradle** | 8.0+ | [gradle.org](https://gradle.org/install/) (o usar wrapper) |
| **Git** | 2.0+ | [git-scm.com](https://git-scm.com/) |
| **IDE** | Cualquiera | [IntelliJ IDEA](https://www.jetbrains.com/idea/) (recomendado) |

### Verificar Instalación

```bash
# Verificar Java
java -version
# Debe mostrar: openjdk version "21.x.x" o similar

# Verificar Gradle (opcional, usaremos wrapper)
gradle --version

# Verificar Git
git --version
```

### Accesos Necesarios

- ✅ Acceso al repositorio del framework
- ✅ Credenciales de base de datos de QA
- ✅ URLs de APIs de testing (si vas a testear APIs)
- ✅ Permisos para crear repositorios (para tu módulo)

---

## 🎯 Flujo Completo (Diagrama)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          🎬 INICIO DEL PROCESO                                   │
└─────────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  PASO 1: Configurar Framework                                                   │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │ 1.1 Clonar repositorio                                                    │  │
│  │ 1.2 Compilar capas (common, api-core, web-core, mobile-core)            │  │
│  │ 1.3 Publicar en Maven Local                                               │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  PASO 2: Crear Módulo de Testing                                                │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │ 2.1 Crear proyecto Gradle                                                 │  │
│  │ 2.2 Configurar build.gradle (dependencias del framework)                  │  │
│  │ 2.3 Crear estructura de directorios                                       │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  PASO 3: Configurar Módulo                                                      │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │ 3.1 Copiar template de configuración                                      │  │
│  │ 3.2 Ejecutar script de configuración interactiva                          │  │
│  │ 3.3 Crear .env.local con credenciales                                     │  │
│  │ 3.4 Configurar properties (URLs, timeouts, etc.)                          │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  PASO 4: Escribir Tests                                                         │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │ 4.1 Crear archivo .feature (Gherkin)                                      │  │
│  │ 4.2 Escribir scenarios                                                     │  │
│  │ 4.3 Usar steps del framework (o crear propios)                            │  │
│  │ 4.4 Definir test data                                                      │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  PASO 5: Ejecutar Tests                                                         │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │ 5.1 Opción A: Usar script test.sh (recomendado)                           │  │
│  │ 5.2 Opción B: Gradle directamente                                         │  │
│  │ 5.3 Opción C: IDE (IntelliJ/Eclipse)                                      │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  PASO 6: Revisar Resultados                                                     │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │ 6.1 Ver logs en consola                                                    │  │
│  │ 6.2 Abrir reporte HTML (Cucumber)                                         │  │
│  │ 6.3 Revisar screenshots (si hay fallos)                                   │  │
│  │ 6.4 Analizar métricas                                                      │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           ✅ TESTS EJECUTADOS                                   │
│                                                                                 │
│  📊 Resultados disponibles en:                                                  │
│     • build/reports/tests/test/index.html                                      │
│     • build/reports/cucumber/cucumber-html-report.html                         │
│     • target/screenshots/ (si hay fallos)                                      │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Paso 1: Configuración del Framework

### 1.1 Clonar el Repositorio

```bash
# Crear directorio de trabajo
mkdir -p ~/qa-automation
cd ~/qa-automation

# Clonar el framework
git clone https://github.com/scotiabank/qa-scotia-frameworks.git
cd qa-scotia-frameworks

# Verificar estructura
ls -la
# Debes ver: common/, api-core/, web-core/, mobile-core/, scripts/
```

### 1.2 Compilar el Framework

```bash
# Compilar todas las capas
./gradlew clean build -x test

# Salida esperada:
# BUILD SUCCESSFUL in 15s
```

### 1.3 Publicar en Maven Local

```bash
# Publicar todas las capas en tu repositorio Maven local
./gradlew publishToMavenLocal

# Verificar que se publicó
ls ~/.m2/repository/com/scotia/qa/
# Debes ver: common/, api-core/, web-core/, mobile-core/
```

**📝 Nota:** Esto instala el framework en tu máquina para que tu módulo pueda importarlo.

---

## 🏗️ Paso 2: Crear Tu Módulo de Testing

### 2.1 Crear Estructura del Proyecto

```bash
# Volver al directorio de trabajo
cd ~/qa-automation

# Crear módulo
mkdir qa-module-mi-proyecto
cd qa-module-mi-proyecto

# Inicializar Gradle
gradle init --type java-library --dsl groovy --test-framework junit-jupiter
```

### 2.2 Configurar `build.gradle`

Crear o reemplazar `build.gradle` con:

```groovy
plugins {
    id 'java'
}

group = 'com.scotia.qa'
version = '1.0.0'

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenLocal()     // Para usar el framework publicado localmente
    mavenCentral()
}

dependencies {
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // FRAMEWORK SCOTIA QA
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    
    // Importar solo las capas que necesitas:
    
    // Para tests de API
    testImplementation 'com.scotia.qa:api-core:1.0.0'
    
    // Para tests Web
    testImplementation 'com.scotia.qa:web-core:1.0.0'
    
    // Para tests Mobile
    // testImplementation 'com.scotia.qa:mobile-core:1.0.0'
    
    // Common siempre se incluye automáticamente (dependencia transitiva)
    
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CUCUMBER & TEST EXECUTION
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    
    testImplementation platform('io.cucumber:cucumber-bom:7.18.0')
    testImplementation 'io.cucumber:cucumber-java'
    testImplementation 'io.cucumber:cucumber-junit-platform-engine'
    
    testImplementation platform('org.junit:junit-bom:5.10.0')
    testImplementation 'org.junit.platform:junit-platform-suite'
    testImplementation 'org.junit.jupiter:junit-jupiter'
}

test {
    useJUnitPlatform()
    
    systemProperties = System.properties.findAll { 
        it.key.startsWith('cucumber.') || 
        it.key.startsWith('DB_') ||
        it.key.startsWith('API_') ||
        it.key.startsWith('WEB_')
    }
    
    testLogging {
        events "passed", "skipped", "failed"
        showStandardStreams = false
    }
}
```

### 2.3 Crear Estructura de Directorios

```bash
# Crear estructura estándar
mkdir -p src/test/java/com/mi/proyecto/steps
mkdir -p src/test/resources/features
mkdir -p src/test/resources

# Verificar estructura
tree src/
# src/
# └── test/
#     ├── java/
#     │   └── com/
#     │       └── mi/
#     │           └── proyecto/
#     │               └── steps/
#     └── resources/
#         └── features/
```

---

## ⚙️ Paso 3: Configurar el Módulo

### 3.1 Copiar Template de Configuración

```bash
# Copiar template desde el framework
cp ../qa-scotia-frameworks/common/src/main/resources/templates/config-scotia.properties.template \
   src/test/resources/config-scotia.properties
```

### 3.2 Usar Script de Configuración Interactiva

```bash
# Ejecutar script de configuración
../qa-scotia-frameworks/scripts/test.sh --setup

# El script te preguntará:
# ¿Qué ambiente deseas configurar?
#   1) Local (desarrollo)
#   2) QA
#   3) UAT
#   4) PROD
# Opción [1]: 1
#
# DB URL: jdbc:oracle:thin:@//localhost:1521/XEPDB1
# DB User: dev_user
# DB Password: ********
# API Base URL: http://localhost:8080/api
```

Esto creará automáticamente `.env.local` con tus credenciales.

### 3.3 Editar `config-scotia.properties`

Abrir `src/test/resources/config-scotia.properties` y ajustar:

```properties
# ════════════════════════════════════════════════════════════════
# Scotia QA Framework - Configuración del Módulo
# ════════════════════════════════════════════════════════════════

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# INFORMACIÓN DEL MÓDULO
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
module.name=mi-proyecto
module.description=Tests automatizados para Mi Proyecto

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# BASE DE DATOS
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
db.url=${{DB_URL}}
db.username=${{DB_USER}}
db.password=${{DB_PASS}}
db.driver=oracle.jdbc.OracleDriver
db.pool.size=10
db.pool.timeout=30000

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# API TESTING
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
api.base.url=${{API_BASE_URL}}
api.timeout=30000
api.retry.count=3

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# WEB TESTING
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
web.base.url=${{WEB_BASE_URL}}
web.browser=chrome
web.headless=false
web.timeout=30
```

### 3.4 Crear `.gitignore`

```bash
cat > .gitignore << 'EOF'
# Build
build/
target/
*.jar
*.war

# IDE
.idea/
.vscode/
*.iml
.settings/
.classpath
.project

# Variables de entorno (¡IMPORTANTE!)
.env
.env.*
!.env.example

# Sistema
.DS_Store
Thumbs.db

# Logs
*.log
logs/
EOF
```

---

## ✍️ Paso 4: Escribir Tu Primer Test

### 4.1 Crear Test Runner

Crear `src/test/java/com/mi/proyecto/RunCucumberTest.java`:

```java
package com.mi.proyecto;

import org.junit.platform.suite.api.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = "cucumber.glue", value = "com.scotia.qa, com.mi.proyecto.steps")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty, html:build/reports/cucumber/cucumber-html-report.html, json:build/reports/cucumber/cucumber-report.json")
@ConfigurationParameter(key = "cucumber.publish.enabled", value = "false")
public class RunCucumberTest {
}
```

### 4.2 Crear Feature (Gherkin)

Crear `src/test/resources/features/mi-primer-test.feature`:

```gherkin
# language: es
@api @test
Característica: Mi Primer Test de API

  Como QA engineer
  Quiero validar que la API responde correctamente
  Para asegurar que el sistema funciona

  Escenario: Validar que el endpoint de usuarios responde con éxito
    Dado que tengo la URL del servicio "/users"
    Cuando ejecuto una petición GET
    Entonces el código de respuesta debe ser 200
    Y el body debe contener el campo "data"
```

### 4.3 Crear Steps Personalizados (Opcional)

Si necesitas steps específicos, crear `src/test/java/com/mi/proyecto/steps/MisSteps.java`:

```java
package com.mi.proyecto.steps;

import com.scotia.qa.common.cucumber.context.ScenarioContext;
import com.scotia.qa.common.logging.TestLogger;
import io.cucumber.java.es.*;

import java.util.Map;

public class MisSteps {
    
    @Dado("que tengo datos de prueba específicos")
    public void configurarDatosPrueba() {
        // Tu lógica personalizada
        TestLogger.logInfo("MIS_STEPS", "Configurando datos de prueba", null);
        
        // Guardar en contexto para usar en otros steps
        ScenarioContext.setByLayer("shared", "testData", "mi-valor");
    }
    
    @Entonces("debo ver el mensaje {string}")
    public void validarMensaje(String mensajeEsperado) {
        // Tu validación personalizada
        TestLogger.logInfo("MIS_STEPS", "Validando mensaje", 
            Map.of("esperado", mensajeEsperado));
    }
}
```

---

## ▶️ Paso 5: Ejecutar los Tests

### Opción A: Usando el Script (Recomendado)

```bash
# Ejecución simple
../qa-scotia-frameworks/scripts/test.sh

# Con tags específicos
../qa-scotia-frameworks/scripts/test.sh --tags @smoke

# Con ambiente específico
../qa-scotia-frameworks/scripts/test.sh --env qa

# Ver comando sin ejecutar (debug)
../qa-scotia-frameworks/scripts/test.sh --dry-run
```

### Opción B: Gradle Directo

```bash
# Ejecutar todos los tests
./gradlew test

# Con system properties
./gradlew test -DDB_URL=jdbc:... -DDB_USER=user -DDB_PASS=pass

# Con tags de Cucumber
./gradlew test -Dcucumber.filter.tags="@smoke"

# Con logs detallados
./gradlew test --info
```

### Opción C: Desde el IDE

#### IntelliJ IDEA:

1. **Instalar plugin Cucumber:**
   - File → Settings → Plugins
   - Buscar "Cucumber for Java"
   - Instalar y reiniciar

2. **Ejecutar feature:**
   - Clic derecho en el archivo `.feature`
   - "Run 'Feature: ...'"

3. **Ejecutar scenario específico:**
   - Clic derecho en el scenario
   - "Run 'Scenario: ...'"

4. **Configurar Run Configuration:**
   - Run → Edit Configurations
   - Agregar variables de entorno (DB_URL, etc.)

#### Eclipse:

1. **Instalar plugin:**
   - Help → Eclipse Marketplace
   - Buscar "Cucumber Eclipse Plugin"
   - Instalar

2. **Ejecutar:**
   - Clic derecho en feature → Run As → Cucumber Feature

---

## 🔍 Paso 6: Revisar Resultados

### 6.1 Logs en Consola

Durante la ejecución verás logs estructurados:

```
INFO  [MI-PROYECTO] [Mi Primer Test] [HTTP_REQUEST] Ejecutando petición GET
Context: {url=http://api/users, timeout=30000}

INFO  [MI-PROYECTO] [Mi Primer Test] [HTTP_RESPONSE] Respuesta recibida
Context: {status=200, time=245ms}

✓ Scenario: Validar que el endpoint de usuarios responde con éxito
```

### 6.2 Reporte HTML de Cucumber

```bash
# Abrir reporte en navegador
open build/reports/cucumber/cucumber-html-report.html

# O manualmente navegar a:
# build/reports/cucumber/cucumber-html-report.html
```

**El reporte muestra:**
- ✅ Scenarios pasados/fallidos
- ⏱️ Tiempo de ejecución
- 📊 Estadísticas generales
- 📝 Detalles de cada step

### 6.3 Screenshots (si hay fallos)

```bash
# Ver screenshots capturados automáticamente
ls target/screenshots/

# Formato: <scenario-name>_<timestamp>.png
```

### 6.4 Reporte JUnit (para CI/CD)

```bash
# Ver resultados XML para Jenkins/GitLab
cat build/test-results/test/*.xml
```

---

## 🚀 Próximos Pasos

### 📚 Aprender Más

1. **Explorar Steps Disponibles:**
   - [API Core Steps](../api-core/QUICK-REFERENCE.md)
   - [Web Core Steps](../web-core/QUICK-REFERENCE.md)
   - [Common Utilities](../common/README.md)

2. **Patrones Avanzados:**
   - ScenarioContext para compartir datos
   - Test Data Finder para usuarios de BD
   - Componentes reutilizables (Module-First)
   - Hooks condicionales por tags

3. **Integración CI/CD:**
   - [Jenkins Integration](../scripts/jenkins/README.md)
   - GitLab CI
   - GitHub Actions

### 🎯 Casos de Uso Comunes

**Test de API Completo:**
```gherkin
@api
Escenario: Login y consulta de datos
  Dado que tengo el endpoint "/auth/login"
  Y agrego el header "Content-Type" con valor "application/json"
  Y agrego el request:
    """
    {
      "username": "testuser",
      "password": "Test123"
    }
    """
  Cuando ejecuto una petición POST
  Entonces el código de respuesta debe ser 200
  Y guardo el valor del campo "token" en variable "authToken"
  
  Dado que tengo el endpoint "/users/me"
  Y agrego el header "Authorization" con valor "Bearer {authToken}"
  Cuando ejecuto una petición GET
  Entonces el código de respuesta debe ser 200
  Y el campo "email" debe contener "@"
```

**Test Web Completo:**
```gherkin
@web
Escenario: Login en aplicación web
  Dado que navego a la URL "https://app.example.com/login"
  Cuando ingreso el texto "testuser" en el elemento "username"
  Y ingreso el texto "Test123" en el elemento "password"
  Y hago clic en el elemento "loginButton"
  Entonces debo ver el elemento "welcomeMessage"
  Y el texto del elemento "userDisplay" debe ser "Test User"
```

**Test Combinado (API + Web):**
```gherkin
@api @web
Escenario: Crear usuario por API y validar en Web
  # Crear usuario por API
  Dado que tengo el endpoint "/users"
  Y agrego el request:
    """
    {"name": "Nuevo Usuario", "email": "nuevo@test.com"}
    """
  Cuando ejecuto una petición POST
  Entonces el código de respuesta debe ser 201
  Y guardo el valor del campo "id" en variable "userId"
  
  # Validar en la interfaz web
  Dado que navego a la URL "https://app.example.com/users/{userId}"
  Entonces el texto del elemento "userName" debe ser "Nuevo Usuario"
```

---

## 🐛 Troubleshooting

### ❌ Error: "Module not found"

**Problema:**
```
Could not find com.scotia.qa:common:1.0.0
```

**Solución:**
```bash
# Volver al framework y republicar
cd ../qa-scotia-frameworks
./gradlew publishToMavenLocal

# Verificar publicación
ls ~/.m2/repository/com/scotia/qa/common/1.0.0/
```

---

### ❌ Error: "No features found"

**Problema:**
```
No features found at [classpath:features]
```

**Solución:**
```bash
# Verificar que los features están en la ruta correcta
ls src/test/resources/features/
# Debe haber al menos un archivo .feature

# Verificar RunCucumberTest.java
# Debe tener: @SelectClasspathResource("features")
```

---

### ❌ Error: "Step undefined"

**Problema:**
```
You can implement missing steps with the snippets below:

@Dado("que tengo la URL del servicio {string}")
public void que_tengo_la_url_del_servicio(String url) {
    // Write code here
}
```

**Solución:**

1. **Verificar que importaste la capa correcta:**
```groovy
// En build.gradle, debe estar:
testImplementation 'com.scotia.qa:api-core:1.0.0'  // Para steps de API
testImplementation 'com.scotia.qa:web-core:1.0.0'  // Para steps de Web
```

2. **Verificar configuración de glue:**
```java
// En RunCucumberTest.java:
@ConfigurationParameter(
    key = "cucumber.glue", 
    value = "com.scotia.qa, com.mi.proyecto.steps"  // ← Debe incluir com.scotia.qa
)
```

3. **Agregar tag correcto al scenario:**
```gherkin
@api  # ← Para que se inicialicen los steps de API
Escenario: Mi test
```

---

### ❌ Error: "Connection refused"

**Problema:**
```
java.net.ConnectException: Connection refused
```

**Solución:**

1. **Verificar que la BD/API está accesible:**
```bash
# Probar conexión a BD
telnet db-host 1521

# Probar API
curl http://api-host/health
```

2. **Verificar configuración en .env.local:**
```bash
cat .env.local | grep DB_URL
# Verificar que la URL es correcta
```

3. **Verificar que las variables se cargan:**
```bash
# Ejecutar con debug
../qa-scotia-frameworks/scripts/test.sh --verbose
```

---

### ❌ Error: "OutOfMemoryError"

**Problema:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solución:**

Agregar en `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

---

### 🆘 Más Ayuda

- **Documentación completa:** [FRAMEWORK-GUIDE.md](FRAMEWORK-GUIDE.md)
- **Troubleshooting detallado:** [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- **Ejemplos de cada capa:** Ver READMEs de cada módulo
- **Scripts de automatización:** [scripts/README.md](../scripts/README.md)

---

## ✅ Checklist Final

Antes de terminar, verifica:

- [ ] ✅ Framework compilado y publicado en Maven Local
- [ ] ✅ Módulo creado con estructura correcta
- [ ] ✅ `build.gradle` configurado con dependencias
- [ ] ✅ `.env.local` creado con credenciales (y gitignored)
- [ ] ✅ `config-scotia.properties` configurado
- [ ] ✅ Al menos un `.feature` creado
- [ ] ✅ `RunCucumberTest.java` configurado
- [ ] ✅ Tests ejecutándose correctamente
- [ ] ✅ Reportes generándose

---

## 🎉 ¡Felicidades!

Ya tienes tu primer módulo de testing funcionando con el Scotia QA Framework.

**Siguientes pasos recomendados:**

1. 📖 Leer [FRAMEWORK-GUIDE.md](FRAMEWORK-GUIDE.md) para entender la arquitectura
2. 🎯 Explorar [common/README.md](../common/README.md) para ver utilidades disponibles
3. 🚀 Revisar [scripts/README.md](../scripts/README.md) para automatizar ejecución
4. 🔧 Configurar [Jenkins](../scripts/jenkins/README.md) para CI/CD

---

**Documentación creada por:** Abel Venero  
**Última actualización:** 28 de Noviembre de 2025  
**Versión del Framework:** 1.0.0

