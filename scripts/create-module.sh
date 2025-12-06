#!/bin/bash

# ============================================================================
# Scotia QA Framework - Script de Creación de Módulo
# ============================================================================
#
# Crea un módulo de testing completo desde cero con:
#   ✅ Estructura de directorios estándar
#   ✅ Scripts de testing copiados
#   ✅ Archivos de configuración (build.gradle, .env, etc.)
#   ✅ Ejemplos de features y steps
#   ✅ README con documentación
#
# USO:
#   ./create-module.sh <nombre-modulo> [directorio-destino]
#
# EJEMPLOS:
#   ./create-module.sh banking              # Crea qa-module-banking/ aquí
#   ./create-module.sh banking ~/projects   # Crea en ~/projects/qa-module-banking/
#   ./create-module.sh autos --with-api     # Solo con api-core
#   ./create-module.sh cards --with-web     # Solo con web-core
#   ./create-module.sh mobile --with-mobile # Solo con mobile-core
#
# @author Abel Venero
# @version 1.0.0
# ============================================================================

set -e  # Salir si hay algún error

# ============================================================================
# COLORES Y FORMATO
# ============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# ============================================================================
# FUNCIONES DE LOGGING
# ============================================================================

log_success() {
    echo -e "${GREEN}✓${NC} $1"
}

log_error() {
    echo -e "${RED}✗${NC} $1" >&2
}

log_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

log_info() {
    echo -e "${CYAN}ℹ${NC} $1"
}

log_banner() {
    echo ""
    echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
    echo ""
}

log_section() {
    echo ""
    echo -e "${BOLD}$1${NC}"
    echo "────────────────────────────────────────────────────────────"
}

# ============================================================================
# VARIABLES GLOBALES
# ============================================================================

MODULE_NAME=""
MODULE_DIR=""
DESTINATION_DIR="."
FRAMEWORK_VERSION="1.0.0"
WITH_API=true
WITH_WEB=true
WITH_MOBILE=true
INTERACTIVE=false

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRAMEWORK_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# ============================================================================
# FUNCIONES DE VALIDACIÓN
# ============================================================================

validate_module_name() {
    local name="$1"

    if [[ -z "$name" ]]; then
        log_error "Nombre del módulo no puede estar vacío"
        return 1
    fi

    if [[ ! "$name" =~ ^[a-z][a-z0-9-]*$ ]]; then
        log_error "Nombre inválido. Usar solo minúsculas, números y guiones (ej: banking, autos-prestamos)"
        return 1
    fi

    return 0
}

check_dependencies() {
    local missing=()

    if ! command -v java &> /dev/null; then
        missing+=("java")
    fi

    if ! command -v gradle &> /dev/null && [[ ! -f "./gradlew" ]]; then
        missing+=("gradle")
    fi

    if [[ ${#missing[@]} -gt 0 ]]; then
        log_error "Dependencias faltantes: ${missing[*]}"
        log_info "Instala las dependencias antes de continuar"
        return 1
    fi

    return 0
}

# ============================================================================
# FUNCIÓN PRINCIPAL DE CREACIÓN
# ============================================================================

create_module_structure() {
    log_section "📁 Creando Estructura de Directorios"

    # Directorios principales
    mkdir -p "${MODULE_DIR}"
    mkdir -p "${MODULE_DIR}/src/test/java/com/${MODULE_NAME}/steps"
    mkdir -p "${MODULE_DIR}/src/test/java/com/${MODULE_NAME}/hooks"
    mkdir -p "${MODULE_DIR}/src/test/resources/features/${MODULE_NAME}"
    mkdir -p "${MODULE_DIR}/src/test/resources"
    mkdir -p "${MODULE_DIR}/scripts"

    log_success "Estructura de directorios creada"
}

copy_scripts() {
    log_section "📜 Copiando Scripts de Testing (Cross-Platform)"

    # Scripts CORE (utils) - Se extraen desde JAR con sync-utils
    # Estos se copian inicialmente pero se actualizarán con sync-utils
    if [[ -f "${FRAMEWORK_DIR}/scripts/utils.sh" ]]; then
        cp "${FRAMEWORK_DIR}/scripts/utils.sh" "${MODULE_DIR}/scripts/"
        chmod +x "${MODULE_DIR}/scripts/utils.sh"
        log_success "utils.sh copiado"
    fi

    if [[ -f "${FRAMEWORK_DIR}/scripts/utils.ps1" ]]; then
        cp "${FRAMEWORK_DIR}/scripts/utils.ps1" "${MODULE_DIR}/scripts/"
        log_success "utils.ps1 copiado (Windows/PowerShell)"
    fi

    # Scripts de sincronización (mantener scripts actualizados)
    if [[ -f "${FRAMEWORK_DIR}/scripts/sync-utils.sh" ]]; then
        cp "${FRAMEWORK_DIR}/scripts/sync-utils.sh" "${MODULE_DIR}/scripts/"
        chmod +x "${MODULE_DIR}/scripts/sync-utils.sh"
        log_success "sync-utils.sh copiado (Bash)"
    fi

    if [[ -f "${FRAMEWORK_DIR}/scripts/sync-utils.ps1" ]]; then
        cp "${FRAMEWORK_DIR}/scripts/sync-utils.ps1" "${MODULE_DIR}/scripts/"
        log_success "sync-utils.ps1 copiado (PowerShell)"
    fi

    # Scripts de ejecución de tests
    if [[ -f "${FRAMEWORK_DIR}/scripts/run-test.sh" ]]; then
        cp "${FRAMEWORK_DIR}/scripts/run-test.sh" "${MODULE_DIR}/scripts/"
        chmod +x "${MODULE_DIR}/scripts/run-test.sh"
        log_success "run-test.sh copiado (Bash)"
    fi

    if [[ -f "${FRAMEWORK_DIR}/scripts/run-test.ps1" ]]; then
        cp "${FRAMEWORK_DIR}/scripts/run-test.ps1" "${MODULE_DIR}/scripts/"
        log_success "run-test.ps1 copiado (PowerShell)"
    fi

    # NOTA: update-scripts.sh está DEPRECADO y ya no se copia
    # Usar sync-utils.sh/ps1 en su lugar

    echo ""
    log_info "Scripts disponibles para macOS/Linux:"
    echo "  • run-test.sh - Ejecutar tests"
    echo "  • sync-utils.sh - Actualizar utils desde framework"
    echo ""
    log_info "Scripts disponibles para Windows:"
    echo "  • run-test.ps1 - Ejecutar tests"
    echo "  • sync-utils.ps1 - Actualizar utils desde framework"
    echo ""

    log_success "Scripts de testing copiados (soporte cross-platform)"
}

create_build_gradle() {
    log_section "🔧 Creando build.gradle"

    local api_dep=""
    local web_dep=""
    local mobile_dep=""

    [[ "$WITH_API" == "true" ]] && api_dep="    testImplementation 'com.scotia.qa:api-core:${FRAMEWORK_VERSION}'"
    [[ "$WITH_WEB" == "true" ]] && web_dep="    testImplementation 'com.scotia.qa:web-core:${FRAMEWORK_VERSION}'"
    [[ "$WITH_MOBILE" == "true" ]] && mobile_dep="    testImplementation 'com.scotia.qa:mobile-core:${FRAMEWORK_VERSION}'"

    cat > "${MODULE_DIR}/build.gradle" << EOF
plugins {
    id 'java'
}

group = 'com.scotia.qa'
version = '1.0.0'
sourceCompatibility = '21'

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Framework Scotia QA
    testImplementation 'com.scotia.qa:common:${FRAMEWORK_VERSION}'
${api_dep}
${web_dep}
${mobile_dep}

    // Testing
    testImplementation platform('org.junit:junit-bom:5.10.0')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.junit.platform:junit-platform-suite'

    // Cucumber
    testImplementation 'io.cucumber:cucumber-java:7.18.0'
    testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.18.0'
}

test {
    useJUnitPlatform()

    systemProperties = System.properties.findAll {
        it.key.startsWith('DB_') ||
        it.key.startsWith('API_') ||
        it.key.startsWith('WEB_') ||
        it.key == 'TEST_ENV'
    }

    testLogging {
        events "passed", "skipped", "failed"
        showStandardStreams = false
    }
}

// Configuración para Cucumber
configurations {
    cucumberRuntime {
        extendsFrom testImplementation
    }
}
EOF

    log_success "build.gradle creado"
}

create_settings_gradle() {
    log_section "⚙️ Creando settings.gradle"

    cat > "${MODULE_DIR}/settings.gradle" << EOF
rootProject.name = 'qa-module-${MODULE_NAME}'
EOF

    log_success "settings.gradle creado"
}

create_gradle_properties() {
    log_section "📝 Creando gradle.properties"

    cat > "${MODULE_DIR}/gradle.properties" << EOF
# Gradle Configuration
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true

# Module Info
MODULE_NAME=${MODULE_NAME}
EOF

    log_success "gradle.properties creado"
}

create_env_template() {
    log_section "🔐 Creando .env.local (template)"

    cat > "${MODULE_DIR}/.env.local" << 'EOF'
# ====================================================================
# Variables de Entorno para Desarrollo Local
# ====================================================================
# ⚠️ NO COMMITEAR ESTE ARCHIVO CON CREDENCIALES REALES
# ====================================================================

# Ambiente
TEST_ENV=local

# Base de Datos (si se usa Test Data Finder)
DB_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
DB_USER=testuser
DB_PASS=TestPass123

# API Testing (si se usa api-core)
API_BASE_URL=https://api-dev.example.com/v1
API_TOKEN=your_token_here

# Web Testing (si se usa web-core)
WEB_BASE_URL=https://app-dev.example.com
BROWSER=chrome
HEADLESS=false

# Mobile Testing (si se usa mobile-core)
APP_PATH=/path/to/app.apk
PLATFORM=Android
DEVICE_NAME=Pixel_5_API_33
EOF

    log_success ".env.local creado (template)"
}

create_config_properties() {
    log_section "📄 Creando config-scotia.properties"

    cat > "${MODULE_DIR}/src/test/resources/config-scotia.properties" << 'EOF'
# ====================================================================
# Scotia QA Framework - Configuración del Módulo
# ====================================================================

# Ambiente
test.env=${{TEST_ENV}}

# Base de Datos
db.url=${{DB_URL}}
db.username=${{DB_USER}}
db.password=${{DB_PASS}}
db.driver=oracle.jdbc.OracleDriver
db.pool.size=10

# API Configuration
api.base.url=${{API_BASE_URL}}
api.timeout=30000
api.retry.count=3

# Web Configuration
web.base.url=${{WEB_BASE_URL}}
web.browser=${{BROWSER}}
web.headless=${{HEADLESS}}
web.timeout=30
web.implicit.wait=10

# Mobile Configuration
mobile.platform=${{PLATFORM}}
mobile.device.name=${{DEVICE_NAME}}
mobile.app.path=${{APP_PATH}}
mobile.automation.name=UiAutomator2
appium.server.url=http://localhost:4723
EOF

    log_success "config-scotia.properties creado"
}

create_example_feature() {
    log_section "📝 Creando Feature de Ejemplo"

    cat > "${MODULE_DIR}/src/test/resources/features/${MODULE_NAME}/ejemplo.feature" << EOF
# language: es
@${MODULE_NAME} @ejemplo
Característica: Ejemplo de Feature para módulo ${MODULE_NAME}
  Como QA Engineer
  Quiero tener un ejemplo funcional
  Para empezar a escribir mis pruebas rápidamente

  @smoke @test
  Escenario: Ejemplo básico
    Dado que tengo el módulo ${MODULE_NAME} configurado
    Cuando ejecuto este escenario de ejemplo
    Entonces debería pasar exitosamente
EOF

    log_success "Feature de ejemplo creado"
}

create_example_steps() {
    log_section "🪜 Creando Steps de Ejemplo"

    local package_name="${MODULE_NAME//-/_}"

    cat > "${MODULE_DIR}/src/test/java/com/${MODULE_NAME}/steps/ExampleSteps.java" << EOF
package com.${MODULE_NAME}.steps;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;

/**
 * Steps de ejemplo para el módulo ${MODULE_NAME}.
 *
 * @author Tu Nombre
 * @version 1.0.0
 */
public class ExampleSteps {

    @Dado("que tengo el módulo ${MODULE_NAME} configurado")
    public void moduloConfigurado() {
        System.out.println("✓ Módulo ${MODULE_NAME} configurado correctamente");
    }

    @Cuando("ejecuto este escenario de ejemplo")
    public void ejecutoEscenario() {
        System.out.println("✓ Ejecutando escenario de ejemplo");
    }

    @Entonces("debería pasar exitosamente")
    public void deberiaP exitosamente() {
        System.out.println("✓ Test pasó exitosamente!");
        // Aquí van tus validaciones reales
    }
}
EOF

    log_success "Steps de ejemplo creados"
}

create_test_runner() {
    log_section "🏃 Creando Test Runner"

    cat > "${MODULE_DIR}/src/test/java/com/${MODULE_NAME}/RunCucumberTest.java" << EOF
package com.${MODULE_NAME};

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.*;

/**
 * Runner de Cucumber para el módulo ${MODULE_NAME}.
 *
 * @author Tu Nombre
 * @version 1.0.0
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.scotia.qa, com.${MODULE_NAME}")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:build/reports/cucumber/cucumber-html-report.html, json:build/reports/cucumber/cucumber.json")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/resources/features")
public class RunCucumberTest {
    // Este archivo solo configura la ejecución de Cucumber
    // No necesita código adicional
}
EOF

    log_success "Test Runner creado"
}

create_hooks() {
    log_section "🪝 Creando Hooks"

    cat > "${MODULE_DIR}/src/test/java/com/${MODULE_NAME}/hooks/TestHooks.java" << EOF
package com.${MODULE_NAME}.hooks;

import com.scotia.qa.common.logging.LoggingInitializer;
import com.scotia.qa.common.logging.TestLogger;
import io.cucumber.java.*;

import java.util.Map;

/**
 * Hooks de Cucumber para el módulo ${MODULE_NAME}.
 *
 * @author Tu Nombre
 * @version 1.0.0
 */
public class TestHooks {

    @BeforeAll
    public static void beforeAll() {
        // Inicializar logging para el módulo
        LoggingInitializer.initModuleContext("${MODULE_NAME^^}");
        TestLogger.logInfo("FRAMEWORK", "Sistema de logging inicializado para ${MODULE_NAME}",
                Map.of("module", "${MODULE_NAME}"));
    }

    @Before
    public void before(Scenario scenario) {
        // Establecer contexto del escenario
        LoggingInitializer.setTestContext(scenario.getName());

        TestLogger.logInfo("SCENARIO_START", "Iniciando escenario", Map.of(
                "name", scenario.getName(),
                "tags", scenario.getSourceTagNames(),
                "uri", scenario.getUri().toString()
        ));
    }

    @After
    public void after(Scenario scenario) {
        if (scenario.isFailed()) {
            TestLogger.logError("SCENARIO_FAILED", "Escenario falló", Map.of(
                    "name", scenario.getName(),
                    "status", scenario.getStatus().toString()
            ));
        } else {
            TestLogger.logInfo("SCENARIO_PASSED", "Escenario exitoso", Map.of(
                    "name", scenario.getName()
            ));
        }

        LoggingInitializer.clearTestContext();
    }

    @AfterAll
    public static void afterAll() {
        TestLogger.logInfo("FRAMEWORK", "Suite de pruebas finalizada para ${MODULE_NAME}", null);
        LoggingInitializer.clearAllContext();
    }
}
EOF

    log_success "Hooks creados"
}

create_gitignore() {
    log_section "🚫 Creando .gitignore"

    cat > "${MODULE_DIR}/.gitignore" << 'EOF'
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IntelliJ IDEA
.idea/
*.iml
*.iws
*.ipr
out/

# Eclipse
.classpath
.project
.settings/

# VS Code
.vscode/

# NetBeans
nbproject/
nbbuild/
dist/
nbdist/

# macOS
.DS_Store

# Windows
Thumbs.db

# Logs
*.log

# Secrets (IMPORTANTE)
.env.local
.env.*.local
**/secrets/
**/*-secrets.*

# Test Results
test-results/
cucumber-reports/
screenshots/

# Temporary
*.tmp
*.bak
*.swp
*~
EOF

    log_success ".gitignore creado"
}

create_readme() {
    log_section "📖 Creando README.md"

    cat > "${MODULE_DIR}/README.md" << EOF
# 🧪 QA Module: ${MODULE_NAME}

Módulo de pruebas automatizadas para ${MODULE_NAME} usando Scotia QA Framework.

---

## 🚀 Inicio Rápido

### 1️⃣ Configurar Entorno

\`\`\`bash
# Copiar template de configuración
cp .env.local .env.local.bak

# Editar .env.local con tus credenciales reales
nano .env.local
\`\`\`

### 2️⃣ Ejecutar Tests

\`\`\`bash
# Cargar variables de entorno
source .env.local

# Ejecutar todos los tests
./gradlew test

# O usar el script del framework
./scripts/run-test.sh
\`\`\`

### 3️⃣ Ver Reportes

\`\`\`bash
# Reporte Cucumber
open build/reports/cucumber/cucumber-html-report.html

# Reporte JUnit
open build/reports/tests/test/index.html
\`\`\`

---

## 📦 Dependencias del Framework

Este módulo usa las siguientes capas del framework:

EOF

    [[ "$WITH_API" == "true" ]] && echo "- ✅ **api-core** - Testing de APIs REST" >> "${MODULE_DIR}/README.md"
    [[ "$WITH_WEB" == "true" ]] && echo "- ✅ **web-core** - Testing de aplicaciones web" >> "${MODULE_DIR}/README.md"
    [[ "$WITH_MOBILE" == "true" ]] && echo "- ✅ **mobile-core** - Testing de aplicaciones móviles" >> "${MODULE_DIR}/README.md"

    cat >> "${MODULE_DIR}/README.md" << 'EOF'
- ✅ **common** - Componentes compartidos (siempre incluido)

---

## 📁 Estructura del Proyecto

```
qa-module-${MODULE_NAME}/
├── src/test/
│   ├── java/
│   │   └── com/${MODULE_NAME}/
│   │       ├── steps/          ← Step Definitions
│   │       └── hooks/          ← Cucumber Hooks
│   └── resources/
│       ├── features/           ← Archivos .feature
│       └── config-scotia.properties
├── scripts/                    ← Scripts de testing
├── .env.local                  ← Configuración local (NO commitear)
└── build.gradle
```

---

## 🔧 Configuración

### Variables de Entorno

Configura las siguientes variables en `.env.local`:

- `TEST_ENV` - Ambiente (local, qa, staging)
- `DB_URL`, `DB_USER`, `DB_PASS` - Credenciales de BD
- `API_BASE_URL`, `API_TOKEN` - Configuración API
- `WEB_BASE_URL`, `BROWSER` - Configuración Web
- `APP_PATH`, `PLATFORM` - Configuración Mobile

### Archivos de Configuración

- **config-scotia.properties** - Configuración del framework
- **.env.local** - Variables de entorno (gitignored)
- **build.gradle** - Dependencias y configuración Gradle

---

## 📝 Escribir Tests

### 1. Crear Feature

```gherkin
# src/test/resources/features/${MODULE_NAME}/mi-test.feature
@${MODULE_NAME} @test
Escenario: Mi primer test
  Dado que tengo configurado el sistema
  Cuando ejecuto una acción
  Entonces obtengo el resultado esperado
```

### 2. Implementar Steps

```java
// src/test/java/com/${MODULE_NAME}/steps/MiSteps.java
@Dado("que tengo configurado el sistema")
public void sistemaConfigurado() {
    // Tu código aquí
}
```

### 3. Ejecutar

```bash
./gradlew test
```

---

## 🐛 Troubleshooting

### Error: "Cannot resolve dependency"

```bash
# Verificar que el framework está publicado en Maven Local
cd /path/to/qa-scotia-frameworks
./gradlew publishToMavenLocal
```

### Error: "Variables de entorno no resueltas"

```bash
# Cargar .env.local antes de ejecutar
source .env.local
./gradlew test
```

### Error: "Step undefined"

Verificar que el glue incluye el paquete correcto en `RunCucumberTest.java`:
```java
@ConfigurationParameter(key = GLUE_PROPERTY_NAME,
    value = "com.scotia.qa, com.${MODULE_NAME}")
```

---

## 📚 Documentación

- **Framework:** [qa-scotia-frameworks/documentacion/FRAMEWORK-GUIDE.md](../qa-scotia-frameworks/documentacion/FRAMEWORK-GUIDE.md)
- **Quick Start:** [qa-scotia-frameworks/documentacion/QUICK-START.md](../qa-scotia-frameworks/documentacion/QUICK-START.md)
- **Scripts:** [qa-scotia-frameworks/scripts/README.md](../qa-scotia-frameworks/scripts/README.md)

---

**Creado con:** Scotia QA Framework v${FRAMEWORK_VERSION}
**Fecha:** $(date +%Y-%m-%d)
EOF

    log_success "README.md creado"
}

# ============================================================================
# FUNCIÓN DE RESUMEN
# ============================================================================

show_final_summary() {
    log_banner "✅ Módulo Creado Exitosamente"

    echo -e "${GREEN}Módulo:${NC} qa-module-${MODULE_NAME}"
    echo -e "${GREEN}Ubicación:${NC} ${MODULE_DIR}"
    echo ""

    echo -e "${BOLD}📦 Componentes Incluidos:${NC}"
    [[ "$WITH_API" == "true" ]] && echo "  ✓ API Core"
    [[ "$WITH_WEB" == "true" ]] && echo "  ✓ Web Core"
    [[ "$WITH_MOBILE" == "true" ]] && echo "  ✓ Mobile Core"
    echo "  ✓ Common Layer"
    echo "  ✓ Scripts de Testing (Cross-Platform)"
    echo "  ✓ Ejemplos de Features y Steps"
    echo "  ✓ Configuración Consolidada (config-scotia.properties)"
    echo "  ✓ Template de Variables de Entorno (.env.local)"
    echo ""

    echo -e "${BOLD}📄 Configuración Consolidada:${NC}"
    echo ""
    echo "  El módulo usa ${CYAN}UN SOLO ARCHIVO${NC} para configuración:"
    echo ""
    echo "  ${YELLOW}config-scotia.properties${NC}"
    echo "    └─ Drivers (Artifactory/Local/WebDriverManager)"
    echo "    └─ Base de Datos (Oracle/PostgreSQL/MySQL)"
    echo "    └─ Jira/Xray (Estados + Reportes)"
    echo "    └─ Reporting (Extent + Evidencias)"
    echo "    └─ API/Web/Mobile (URLs, timeouts, SSL)"
    echo "    └─ Logging, CI/CD, Feature Flags"
    echo ""
    echo "  ${YELLOW}.env.local${NC} (gitignored)"
    echo "    └─ Variables sensibles (passwords, tokens)"
    echo "    └─ Completar antes de ejecutar tests"
    echo ""

    echo -e "${BOLD}🚀 Próximos Pasos:${NC}"
    echo ""
    echo "  1️⃣  Navegar al módulo:"
    echo -e "      ${CYAN}cd ${MODULE_DIR}${NC}"
    echo ""
    echo "  2️⃣  Editar configuración principal:"
    echo -e "      ${CYAN}nano src/test/resources/config-scotia.properties${NC}"
    echo -e "      💡 Habilita/deshabilita secciones según tu proyecto"
    echo ""
    echo "  3️⃣  Configurar credenciales (gitignored):"
    echo -e "      ${CYAN}nano .env.local${NC}"
    echo -e "      ⚠️  Completa con valores reales (NO commitear)"
    echo ""
    echo "  4️⃣  Cargar variables y ejecutar test de ejemplo:"
    echo -e "      ${CYAN}source .env.local && ./gradlew test${NC}"
    echo -e "      ${CYAN}# Windows: . .\\.env.local; .\\gradlew.bat test${NC}"
    echo ""
    echo "  5️⃣  Ver resultados:"
    echo -e "      ${CYAN}open build/reports/tests/test/index.html${NC}"
    echo ""

    log_info "📚 Documentación completa en: ${MODULE_DIR}/README.md"
    log_info "🔧 Scripts cross-platform en: ${MODULE_DIR}/scripts/"
    log_info "📋 Configuración consolidada en: config-scotia.properties + .env.local"
}

# ============================================================================
# PARSEADO DE ARGUMENTOS
# ============================================================================

parse_arguments() {
    if [[ $# -eq 0 ]]; then
        show_usage
        exit 1
    fi

    MODULE_NAME="$1"
    shift

    while [[ $# -gt 0 ]]; do
        case $1 in
            --dest)
                DESTINATION_DIR="$2"
                shift 2
                ;;
            --with-api)
                WITH_API=true
                WITH_WEB=false
                WITH_MOBILE=false
                shift
                ;;
            --with-web)
                WITH_WEB=true
                WITH_API=false
                WITH_MOBILE=false
                shift
                ;;
            --with-mobile)
                WITH_MOBILE=true
                WITH_API=false
                WITH_WEB=false
                shift
                ;;
            --interactive|-i)
                INTERACTIVE=true
                shift
                ;;
            --help|-h)
                show_usage
                exit 0
                ;;
            *)
                DESTINATION_DIR="$1"
                shift
                ;;
        esac
    done
}

show_usage() {
    cat << EOF
${BOLD}Scotia QA Framework - Creador de Módulos${NC}

${BOLD}USO:${NC}
    ./create-module.sh                          # Modo interactivo (recomendado)
    ./create-module.sh <nombre-modulo> [opciones]

${BOLD}MODO INTERACTIVO:${NC}
    Sin argumentos, el script te guiará paso a paso:

    ${CYAN}./create-module.sh${NC}

    Te preguntará:
      • Nombre del módulo
      • Ubicación donde crearlo
      • Qué capas incluir (API/Web/Mobile)
      • Confirmación antes de crear

${BOLD}MODO DIRECTO:${NC}
    Con argumentos, creación rápida:

    ${CYAN}./create-module.sh <nombre-modulo> [opciones]${NC}

${BOLD}ARGUMENTOS:${NC}
    nombre-modulo     Nombre del módulo (ej: banking, autos, cards)

${BOLD}OPCIONES:${NC}
    --dest <dir>      Directorio destino (default: directorio actual)
    --with-api        Solo incluir api-core
    --with-web        Solo incluir web-core
    --with-mobile     Solo incluir mobile-core
    -i, --interactive Forzar modo interactivo
    -h, --help        Mostrar esta ayuda

${BOLD}EJEMPLOS:${NC}
    # Modo interactivo (paso a paso)
    ./create-module.sh

    # Crear módulo con todas las capas
    ./create-module.sh banking

    # Crear en directorio específico
    ./create-module.sh autos --dest ~/projects

    # Solo API testing
    ./create-module.sh cards --with-api

    # Solo Web testing
    ./create-module.sh mobile --with-web

    # Modo interactivo explícito
    ./create-module.sh -i

${BOLD}NOTA:${NC}
    El módulo se creará como: <dest>/qa-module-<nombre>/

EOF
}

# ============================================================================
# MAIN
# ============================================================================

main() {
    log_banner "🚀 Scotia QA Framework - Creador de Módulos"

    # Parsear argumentos
    parse_arguments "$@"

    # Validar nombre
    if ! validate_module_name "$MODULE_NAME"; then
        exit 1
    fi

    # Configurar directorio del módulo
    MODULE_DIR="${DESTINATION_DIR}/qa-module-${MODULE_NAME}"

    # Verificar que no existe
    if [[ -d "$MODULE_DIR" ]]; then
        log_error "El módulo ya existe: ${MODULE_DIR}"
        log_info "Elimina el directorio o usa otro nombre"
        exit 1
    fi

    # Verificar dependencias
    log_info "Verificando dependencias..."
    if ! check_dependencies; then
        exit 1
    fi
    log_success "Dependencias OK"

    # Crear módulo
    log_info "Creando módulo: qa-module-${MODULE_NAME}"
    echo ""

    create_module_structure
    copy_scripts
    create_build_gradle
    create_settings_gradle
    create_gradle_properties
    create_env_template
    create_config_properties
    create_example_feature
    create_example_steps
    create_test_runner
    create_hooks
    create_gitignore
    create_readme

    # Resumen final
    show_final_summary
}

# Ejecutar
main "$@"

