# 🚀 Scripts del Framework Scotia QA

Colección de scripts genéricos y reutilizables para automatización de testing en múltiples módulos.

---

## 📑 ÍNDICE

- [🎯 ¿Dónde se Alojan y Dónde se Usan?](#-dónde-se-alojan-y-dónde-se-usan)
- [📋 Descripción General](#-descripción-general)
- [🗂️ Estructura de Archivos](#️-estructura-de-archivos)
- [⚡ Inicio Rápido](#-inicio-rápido)
- [📖 Scripts Disponibles](#-scripts-disponibles)
  - [create-module.sh - Creador de Módulos](#create-modulesh---creador-de-módulos)
  - [test.sh - Script Principal](#testsh---script-principal)
  - [utils.sh - Utilidades Compartidas](#utilssh---utilidades-compartidas)
  - [clean-ide.sh - Limpieza de IDE](#clean-idesh---limpieza-de-ide)
- [🔧 Configuración](#-configuración)
  - [Prioridad de Configuración](#prioridad-de-configuración)
  - [Variables de Entorno Soportadas](#variables-de-entorno-soportadas)
  - [Archivos .env](#archivos-env)
- [📝 Ejemplos de Uso](#-ejemplos-de-uso)
  - [Desarrollo Local](#desarrollo-local)
  - [Jenkins / CI-CD](#jenkins--ci-cd)
  - [GitLab CI](#gitlab-ci)
  - [GitHub Actions](#github-actions)
- [🏗️ Estructura de .env](#️-estructura-de-env)
- [🔒 Seguridad](#-seguridad)
- [🐛 Troubleshooting](#-troubleshooting)
- [🚀 Jenkins Integration](#-jenkins-integration)
- [📚 Documentación Adicional](#-documentación-adicional)

---

## 🎯 ¿Dónde se Alojan y Dónde se Usan?

### 📍 **Ubicación: EN EL FRAMEWORK**

Los scripts están **alojados centralmente** en el framework:

```
qa-scotia-frameworks/
└── scripts/                    ← Scripts viven AQUÍ
    ├── run-test.sh
    ├── utils.sh
    ├── validate-framework.sh
    └── jenkins/
```

### 🚀 **Ejecución: DESDE LOS MÓDULOS**

Los scripts se **ejecutan desde los módulos** de prueba:

```
┌────────────────────────────────────────────────────────────────┐
│  FRAMEWORK                                                     │
│  qa-scotia-frameworks/scripts/run-test.sh                     │
│                              ↑                                 │
│                              │ Se ejecuta desde aquí           │
└──────────────────────────────┼─────────────────────────────────┘
                               │
┌──────────────────────────────┼─────────────────────────────────┐
│  MÓDULO                      │                                 │
│  qa-module-banking/          │                                 │
│  $ ../qa-scotia-frameworks/scripts/run-test.sh  ← Llama script│
│                                                                 │
│  El script auto-detecta:                                       │
│  • Módulo actual: "banking"                                    │
│  • Configuración: .env.local del módulo                        │
│  • Tests: src/test/ del módulo                                 │
└─────────────────────────────────────────────────────────────────┘
```

### ✅ **Ventajas de Esta Estrategia**

| Ventaja | Descripción |
|---------|-------------|
| **Centralizado** | Scripts en UN solo lugar (framework) |
| **Sin duplicación** | Módulos NO copian scripts |
| **Actualización automática** | Mejoras en scripts → afectan a TODOS los módulos |
| **Consistencia** | Mismo comportamiento en todos los módulos |
| **Versionado** | Scripts versionados junto con el framework |

### 💻 **Ejemplo de Uso Real**

```bash
# 1. Estructura de directorios (hermanos)
/projects/
  ├── qa-scotia-frameworks/    ← Framework con scripts
  └── qa-module-banking/       ← Tu módulo de pruebas

# 2. Ejecutar desde el módulo
cd /projects/qa-module-banking
../qa-scotia-frameworks/scripts/run-test.sh

# El script automáticamente:
# ✅ Detecta módulo: "banking"
# ✅ Lee configuración: .env.local del módulo
# ✅ Ejecuta tests: src/test/ del módulo
```

### 🔧 **Alternativas de Uso**

**Opción 1: Ruta Relativa (Recomendada)**
```bash
cd qa-module-banking
../qa-scotia-frameworks/scripts/run-test.sh
```

**Opción 2: Variable de Entorno**
```bash
export QA_FRAMEWORK_HOME="/path/to/qa-scotia-frameworks"
cd qa-module-banking
$QA_FRAMEWORK_HOME/scripts/run-test.sh
```

**Opción 3: Alias (Desarrollo Local)**
```bash
# En ~/.bashrc o ~/.zshrc
alias qa-test='../qa-scotia-frameworks/scripts/run-test.sh'

# Usar en cualquier módulo
cd qa-module-banking
qa-test
```

---

## 📋 Descripción General

Los scripts en este directorio proporcionan una **solución unificada y genérica** para ejecutar tests en cualquier módulo del framework Scotia QA, sin necesidad de hardcodear nombres, rutas o configuraciones específicas.

### ✨ Características Principales

- ✅ **Auto-detección** de módulo y configuración
- ✅ **Sin hardcodeo** de nombres o rutas
- ✅ **Compatible con CI/CD** (Jenkins, GitLab, GitHub Actions)
- ✅ **Búsqueda inteligente** de archivos `.env`
- ✅ **Modo interactivo** para configuración inicial
- ✅ **Prioridad configurable** (CLI > ENV > Archivos)
- ✅ **Soporte multi-ambiente** (local, qa, uat, prod)

---

## 🗂️ Estructura de Archivos

```
scripts/
├── test.sh                    # 🎯 Script principal de ejecución
│                              #    - Unifica setup + ejecución
│                              #    - Auto-detección completa
│                              #    - Compatible CI/CD
│
├── utils.sh                   # 🛠️ Utilidades compartidas
│                              #    - Funciones de logging
│                              #    - Detección de entorno
│                              #    - Validación de variables
│                              #    - Construcción de comandos
│
├── clean-ide.sh               # 🧹 Limpieza de archivos IDE
│                              #    - Elimina .idea/
│                              #    - Elimina .vscode/
│                              #    - Limpia archivos temporales
│
├── jenkins/                   # 🏗️ Configuraciones CI/CD
│   ├── README.md              #    - Guía completa de Jenkins
│   └── Jenkinsfile.simple     #    - Pipeline de ejemplo
│
└── README.md                  # 📖 Este archivo
```

---

## ⚡ Inicio Rápido

### 🎬 Primera Ejecución (Configuración Interactiva)

```bash
# 1. Navegar al módulo de testing
cd /path/to/qa-module-banking

# 2. Ejecutar modo setup (interactivo)
../qa-scotia-frameworks/scripts/run-test.sh --setup

# 3. Seguir el asistente:
#    - Seleccionar ambiente (local/qa/uat/prod)
#    - Ingresar credenciales de BD
#    - Configurar URLs opcionales
#    - El script creará .env.{ambiente} automáticamente
```

### 🏃 Ejecuciones Posteriores

```bash
# Ejecutar con configuración guardada
../qa-scotia-frameworks/scripts/run-test.sh

# Ejecutar ambiente específico
../qa-scotia-frameworks/scripts/run-test.sh --env qa

# Ejecutar tags específicos
../qa-scotia-frameworks/scripts/run-test.sh --tags @smoke
```

---

## 📖 Scripts Disponibles

### `create-module.sh` - Creador de Módulos 🎯 ⭐ **NUEVO**

**Propósito:** Crear un módulo de testing completo desde cero con toda la estructura, configuración y ejemplos necesarios.

**Ubicación:** `/Users/abel.venero/Documents/qa-scotia-frameworks/scripts/create-module.sh`

---

#### 💡 ¿Para Qué Sirve?

Este script **automatiza la creación de módulos** completos en 30 segundos, incluyendo:

- ✅ Estructura de directorios completa
- ✅ Scripts de testing copiados (run-test.sh, utils.sh)
- ✅ build.gradle configurado con dependencias del framework
- ✅ Archivos de configuración (.env.local, config-scotia.properties)
- ✅ Feature de ejemplo funcionando
- ✅ Steps de ejemplo implementados
- ✅ Hooks configurados
- ✅ .gitignore con reglas de seguridad
- ✅ README.md completo con documentación

**Beneficio:** De 0 a test funcionando en **menos de 1 minuto**.

---

#### 📝 Uso Básico

```bash
# Crear módulo con TODAS las capas (API + Web + Mobile)
./create-module.sh banking

# Resultado:
qa-module-banking/
├── scripts/           ← Scripts copiados
├── src/test/          ← Estructura completa
│   ├── java/
│   │   └── com/banking/
│   │       ├── steps/ExampleSteps.java
│   │       └── hooks/TestHooks.java
│   └── resources/
│       ├── features/banking/ejemplo.feature
│       └── config-scotia.properties
├── build.gradle       ← Configurado
├── .env.local         ← Template
└── README.md          ← Documentado
```

---

#### 🎯 Opciones Avanzadas

##### **1. Crear en Directorio Específico**

```bash
# Crear en ~/projects/
./create-module.sh banking --dest ~/projects

# Resultado: ~/projects/qa-module-banking/
```

##### **2. Solo API Testing**

```bash
./create-module.sh cards --with-api

# build.gradle incluye SOLO:
#   - common
#   - api-core
```

##### **3. Solo Web Testing**

```bash
./create-module.sh banking --with-web

# build.gradle incluye SOLO:
#   - common
#   - web-core
```

##### **4. Solo Mobile Testing**

```bash
./create-module.sh app --with-mobile

# build.gradle incluye SOLO:
#   - common
#   - mobile-core
```

---

#### 📋 Tabla de Opciones

| Opción | Descripción | Ejemplo |
|--------|-------------|---------|
| `<nombre>` | Nombre del módulo (requerido) | `banking`, `autos` |
| `--dest <dir>` | Directorio destino | `--dest ~/projects` |
| `--with-api` | Solo api-core | `--with-api` |
| `--with-web` | Solo web-core | `--with-web` |
| `--with-mobile` | Solo mobile-core | `--with-mobile` |
| `-h, --help` | Ayuda | `-h` |

**Nota:** Por defecto incluye **TODAS** las capas (api + web + mobile).

---

#### 🚀 Flujo Completo: De 0 a Test Funcionando

```bash
# ═══════════════════════════════════════════════════════════
# PASO 1: Crear el módulo
# ═══════════════════════════════════════════════════════════

cd ~/projects
../qa-scotia-frameworks/scripts/create-module.sh banking

# Salida:
# ════════════════════════════════════════════════════════════
#   🚀 Scotia QA Framework - Creador de Módulos
# ════════════════════════════════════════════════════════════
# 
# ✓ Dependencias OK
# 
# 📁 Creando Estructura de Directorios
# ✓ Estructura de directorios creada
# 
# 📜 Copiando Scripts de Testing
# ✓ run-test.sh copiado
# ✓ utils.sh copiado
# ...
# 
# ✅ Módulo Creado Exitosamente
# 
# Módulo: qa-module-banking
# Ubicación: /Users/tu-usuario/projects/qa-module-banking


# ═══════════════════════════════════════════════════════════
# PASO 2: Configurar credenciales
# ═══════════════════════════════════════════════════════════

cd qa-module-banking
nano .env.local   # Editar con tus credenciales reales


# ═══════════════════════════════════════════════════════════
# PASO 3: Ejecutar test de ejemplo
# ═══════════════════════════════════════════════════════════

source .env.local
./gradlew test

# Salida:
# > Task :test
# 
# ✓ Módulo banking configurado correctamente
# ✓ Ejecutando escenario de ejemplo
# ✓ Test pasó exitosamente!
# 
# BUILD SUCCESSFUL in 5s


# ═══════════════════════════════════════════════════════════
# PASO 4: Ver reportes
# ═══════════════════════════════════════════════════════════

open build/reports/cucumber/cucumber-html-report.html
```

**⏱️ Tiempo total:** ~2 minutos

---

#### 📦 ¿Qué Se Crea Exactamente?

##### **Estructura Completa:**

```
qa-module-banking/
│
├── 📜 scripts/                              ← Scripts de testing
│   ├── run-test.sh                          (copiado del framework)
│   └── utils.sh                             (copiado del framework)
│
├── 📁 src/test/
│   ├── java/com/banking/
│   │   ├── RunCucumberTest.java            ← Test Runner
│   │   ├── steps/
│   │   │   └── ExampleSteps.java           ← Steps de ejemplo
│   │   └── hooks/
│   │       └── TestHooks.java              ← Hooks configurados
│   │
│   └── resources/
│       ├── features/banking/
│       │   └── ejemplo.feature             ← Feature de ejemplo
│       └── config-scotia.properties        ← Configuración framework
│
├── 🔧 Configuración
│   ├── build.gradle                         ← Gradle con dependencias
│   ├── settings.gradle                      ← Nombre del proyecto
│   ├── gradle.properties                    ← Properties Gradle
│   ├── .env.local                           ← Variables de entorno (gitignored)
│   └── .gitignore                           ← Reglas de seguridad
│
└── 📖 README.md                             ← Documentación completa
```

##### **Archivos Clave Generados:**

**1. build.gradle** (con dependencias correctas):
```groovy
dependencies {
    testImplementation 'com.scotia.qa:common:1.0.0'
    testImplementation 'com.scotia.qa:api-core:1.0.0'
    testImplementation 'com.scotia.qa:web-core:1.0.0'
    testImplementation 'com.scotia.qa:mobile-core:1.0.0'
    // + Cucumber, JUnit, etc.
}
```

**2. .env.local** (template con todas las variables):
```bash
TEST_ENV=local
DB_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
DB_USER=testuser
DB_PASS=TestPass123
API_BASE_URL=https://api-dev.example.com
# ... etc
```

**3. Feature de ejemplo** (funcionando):
```gherkin
@banking @ejemplo @smoke @test
Escenario: Ejemplo básico
  Dado que tengo el módulo banking configurado
  Cuando ejecuto este escenario de ejemplo
  Entonces debería pasar exitosamente
```

**4. Steps implementados:**
```java
@Dado("que tengo el módulo banking configurado")
public void moduloConfigurado() {
    System.out.println("✓ Módulo banking configurado correctamente");
}
```

---

#### ✅ Validaciones Automáticas

El script valida automáticamente:

- ✅ Nombre del módulo válido (solo minúsculas, números, guiones)
- ✅ Directorio no existe (evita sobrescribir)
- ✅ Dependencias instaladas (Java, Gradle)
- ✅ Framework disponible (para copiar scripts)

**Ejemplo de error:**
```bash
./create-module.sh Banking

# ✗ Nombre inválido. Usar solo minúsculas, números y guiones 
#   (ej: banking, autos-prestamos)
```

---

#### 🎯 Casos de Uso

##### **Caso 1: Nuevo Proyecto de Pruebas**

```bash
# Equipo de QA necesita empezar a probar módulo Banking
./create-module.sh banking
cd qa-module-banking
# Configurar y empezar a escribir tests
```

##### **Caso 2: PoC Rápido**

```bash
# Necesitas demostrar el framework rápidamente
./create-module.sh demo --with-api
cd qa-module-demo
source .env.local && ./gradlew test
# Muestra test funcionando en 1 minuto
```

##### **Caso 3: Onboarding de Nuevos QAs**

```bash
# Nuevo QA se une al equipo
./create-module.sh training
cd qa-module-training
# Ya tiene estructura completa para aprender
```

##### **Caso 4: Testing Especializado**

```bash
# Solo necesitas probar APIs
./create-module.sh api-tests --with-api

# Solo necesitas probar Web
./create-module.sh web-tests --with-web

# Solo necesitas probar Mobile
./create-module.sh mobile-tests --with-mobile
```

---

#### 🔒 Seguridad Incluida

El `.gitignore` generado automáticamente **protege credenciales**:

```gitignore
# Secrets (IMPORTANTE)
.env.local
.env.*.local
**/secrets/
**/*-secrets.*
```

**Resultado:** NO puedes commitear accidentalmente `.env.local` con credenciales.

---

#### 💡 Tips y Mejores Prácticas

##### **Tip 1: Nombrar Módulos**

```bash
# ✅ BIEN
./create-module.sh banking
./create-module.sh autos-prestamos
./create-module.sh mobile-app

# ❌ MAL
./create-module.sh Banking         # Mayúsculas
./create-module.sh autos_prestamos # Underscore
./create-module.sh mobile app      # Espacio
```

##### **Tip 2: Organización de Proyectos**

```bash
# Estructura recomendada:
/projects/
  ├── qa-scotia-frameworks/    ← Framework (1 vez)
  ├── qa-module-banking/       ← Módulo 1
  ├── qa-module-autos/         ← Módulo 2
  └── qa-module-cards/         ← Módulo 3

# Crear todos:
cd /projects
for module in banking autos cards; do
    ./qa-scotia-frameworks/scripts/create-module.sh $module
done
```

##### **Tip 3: Repositorios Git**

```bash
# Después de crear el módulo:
cd qa-module-banking
git init
git add .
git commit -m "feat: módulo inicial generado con create-module.sh"
git remote add origin https://github.com/scotia/qa-module-banking.git
git push -u origin main
```

---

#### 🐛 Troubleshooting

##### **Error: "Módulo ya existe"**

```bash
# Problema:
./create-module.sh banking
# ✗ El módulo ya existe: /path/qa-module-banking

# Solución 1: Eliminar
rm -rf qa-module-banking
./create-module.sh banking

# Solución 2: Usar otro nombre
./create-module.sh banking-v2
```

##### **Error: "Dependencias faltantes"**

```bash
# Problema:
# ✗ Dependencias faltantes: java

# Solución: Instalar Java 21
brew install openjdk@21  # macOS
```

##### **Error: "No se pueden copiar scripts"**

```bash
# Problema: Framework no está donde se espera

# Solución: Ejecutar desde la ubicación correcta
cd /path/to/qa-scotia-frameworks
./scripts/create-module.sh banking
```

---

#### 📚 Próximos Pasos Después de Crear

1. **Configurar credenciales:**
   ```bash
   nano .env.local  # Editar con valores reales
   ```

2. **Ejecutar test de ejemplo:**
   ```bash
   source .env.local && ./gradlew test
   ```

3. **Leer documentación generada:**
   ```bash
   cat README.md
   ```

4. **Empezar a escribir tus tests:**
   ```bash
   # Crear nueva feature
   nano src/test/resources/features/banking/login.feature
   
   # Implementar steps
   nano src/test/java/com/banking/steps/LoginSteps.java
   ```

5. **Ejecutar con scripts del framework:**
   ```bash
   ./scripts/run-test.sh
   ```

---

### `test.sh` - Script Principal

**Propósito:** Script unificado que combina configuración y ejecución de tests en un solo comando.

**Ubicación:** `/Users/abel.venero/Documents/qa-scotia-frameworks/scripts/test.sh`

#### 🎯 Qué Hace

1. **Auto-detecta** el módulo actual (desde directorio, gradle.properties, o variable)
2. **Busca** archivos de configuración `.env*` en orden de prioridad
3. **Carga** variables de entorno desde archivo o sistema
4. **Valida** que las variables requeridas estén configuradas
5. **Ejecuta** Gradle con las propiedades correctas
6. **Reporta** el resultado de la ejecución

#### 📋 Opciones Disponibles

| Opción | Alias | Descripción | Ejemplo |
|--------|-------|-------------|---------|
| `--help` | `-h` | Muestra ayuda completa con ejemplos | `./test.sh --help` |
| `--setup` | `-s` | Modo configuración interactiva | `./test.sh --setup` |
| `--env ENV` | `-e` | Selecciona ambiente (local/qa/uat/prod) | `./test.sh --env qa` |
| `--tags TAGS` | `-t` | Ejecuta tags específicos de Cucumber | `./test.sh --tags "@smoke and not @wip"` |
| `--module NAME` | `-m` | Especifica nombre del módulo | `./test.sh --module banking` |
| `--env-file FILE` | `-f` | Usa archivo .env específico | `./test.sh --env-file .env.custom` |
| `--verbose` | `-v` | Activa logging detallado (Gradle --info) | `./test.sh --verbose` |
| `--dry-run` | - | Muestra comando sin ejecutar | `./test.sh --dry-run` |

#### 💡 Ejemplos de Uso

**Configuración inicial (primera vez):**
```bash
cd qa-module-banking
../qa-scotia-frameworks/scripts/run-test.sh --setup
```

**Ejecución básica:**
```bash
./run-test.sh                              # Auto-detecta todo
```

**Ejecutar ambiente específico:**
```bash
./run-test.sh --env qa                     # Usa .env.qa
./run-test.sh --env uat                    # Usa .env.uat
```

**Ejecutar tags de Cucumber:**
```bash
./run-test.sh --tags @smoke                # Solo smoke tests
./run-test.sh --tags "@regression and @api"  # Combinación de tags
./run-test.sh --tags "not @wip"            # Excluir WIP
```

**Comandos Gradle personalizados:**
```bash
./run-test.sh clean build                  # Solo build
./run-test.sh test --info                  # Tests con log detallado
./run-test.sh clean test --tests "com.scotia.qa.tests.*"  # Tests específicos
```

**Ver comando sin ejecutar (debug):**
```bash
./run-test.sh --dry-run                    # Muestra el comando completo
./run-test.sh --env qa --dry-run           # Ver comando para QA
```

**Combinar múltiples opciones:**
```bash
./run-test.sh --env qa --tags @smoke --verbose
./run-test.sh --module banking --env uat --tags "@api and @critical"
```

---

### `utils.sh` - Utilidades Compartidas

**Propósito:** Biblioteca de funciones reutilizables para todos los scripts del framework (como una "caja de herramientas").

**Ubicación:** `/Users/abel.venero/Documents/qa-scotia-frameworks/scripts/utils.sh`

---

#### 💡 ¿Qué es utils.sh? (Explicación Simple)

`utils.sh` **NO es un script que ejecutes directamente**. Es como una **biblioteca de funciones** que otros scripts importan y usan.

**Analogía:** Piénsalo como un **"cajón de herramientas"**:

```
┌─────────────────────────────────────────────────────────────────┐
│  run-test.sh (Tu script principal)                              │
│  ├─ Necesita mostrar mensajes bonitos → Usa log_success()      │
│  ├─ Necesita detectar el módulo       → Usa detect_module()    │
│  ├─ Necesita cargar .env              → Usa load_env_file()    │
│  └─ Necesita validar variables        → Usa validate_vars()    │
│                                                                  │
│  Todas esas funciones vienen de → utils.sh                      │
└─────────────────────────────────────────────────────────────────┘
```

**Sin utils.sh (código duplicado):**
```bash
# ❌ En run-test.sh
echo -e "\033[32m✓ Success\033[0m"

# ❌ En setup.sh  
echo -e "\033[32m✓ Success\033[0m"

# ❌ En deploy.sh
echo -e "\033[32m✓ Success\033[0m"

# 😱 Si cambias el formato, cambias 3 archivos!
```

**Con utils.sh (código reutilizable):**
```bash
# ✅ En utils.sh (UNA sola vez)
log_success() {
    echo -e "\033[32m✓ $1\033[0m"
}

# ✅ En run-test.sh
source utils.sh
log_success "Tests OK"

# ✅ En setup.sh
source utils.sh
log_success "Setup OK"

# 🎉 Cambias una función, todos se benefician!
```

---

#### 🔑 ¿Cómo se usa?

**1. Importar en tu script:**
```bash
#!/bin/bash

# Al inicio del script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/utils.sh"

# Ahora puedes usar todas las funciones
```

**2. Usar las funciones:**
```bash
# Ya no necesitas escribir echo complicados
log_success "¡Éxito!"           # ✓ en verde
log_error "Error crítico"       # ✗ en rojo
log_warning "Advertencia"       # ⚠️ en amarillo
```

---

#### 📦 ¿Qué funciones tiene? (Catálogo Completo)

##### 🎨 **1. Funciones de Logging** (Mensajes bonitos en consola)

| Función | ¿Para qué? | Ejemplo |
|---------|-----------|---------|
| `log_success "mensaje"` | Éxito ✓ verde | `log_success "Tests pasaron"` |
| `log_error "mensaje"` | Error ✗ rojo | `log_error "Falló build"` |
| `log_warning "mensaje"` | Advertencia ⚠️ amarillo | `log_warning "Sin .env"` |
| `log_info "mensaje"` | Info ℹ️ cyan | `log_info "Ejecutando..."` |
| `log_banner "título"` | Banner decorado | `log_banner "TESTS"` |
| `log_separator` | Línea separadora | `log_separator` |

**Ejemplo visual:**
```bash
log_banner "Ejecutando Tests"
# ════════════════════════════════
#   Ejecutando Tests
# ════════════════════════════════

log_success "Build exitoso"
# ✓ Build exitoso

log_error "Falló conexión BD"
# ✗ Falló conexión BD
```

---

##### 🔍 **2. Funciones de Detección** (Auto-descubrir cosas)

| Función | ¿Qué detecta? | Retorna | Ejemplo |
|---------|---------------|---------|---------|
| `detect_module_name` | Nombre del módulo actual | "banking", "autos" | `MODULE=$(detect_module_name)` |
| `detect_os` | Sistema operativo | "macos", "linux", "windows" | `OS=$(detect_os)` |
| `is_jenkins` | ¿Estamos en Jenkins? | true/false (exit code) | `if is_jenkins; then ...` |
| `is_ci` | ¿Estamos en CI/CD? | true/false (exit code) | `if is_ci; then ...` |

**Cómo detecta el módulo:**
```bash
# 1. Busca en variable de entorno
if [[ -n "$MODULE_NAME" ]]; then echo "$MODULE_NAME"

# 2. Lee gradle.properties
rootProject.name=qa-module-banking → "banking"

# 3. Lee nombre del directorio
/path/to/qa-module-autos → "autos"
```

---

##### 🔎 **3. Funciones de Búsqueda** (Encontrar archivos)

| Función | ¿Qué busca? | Retorna | Ejemplo |
|---------|-------------|---------|---------|
| `find_env_file` | Archivo .env con prioridad | Ruta completa o vacío | `ENV=$(find_env_file)` |

**Orden de búsqueda:**
```bash
find_env_file busca en orden:
  1. .env.local         # ← Prioridad 1 (desarrollo)
  2. .env.${TEST_ENV}   # ← Prioridad 2 (por ambiente)
  3. .env               # ← Prioridad 3 (genérico)
  4. (vacío)            # ← No encontró nada
```

---

##### ✅ **4. Funciones de Validación** (Verificar que todo esté OK)

| Función | ¿Qué valida? | Ejemplo |
|---------|--------------|---------|
| `validate_required_vars "VAR1" "VAR2"` | Variables existen y no vacías | `validate_required_vars "DB_URL" "DB_USER"` |
| `check_command "cmd"` | Comando está instalado | `check_command "java"` |
| `check_framework_dependencies` | Java y Gradle instalados | `check_framework_dependencies` |

**Ejemplo:**
```bash
# Validar que DB_URL y DB_USER estén configuradas
if ! validate_required_vars "DB_URL" "DB_USER"; then
    log_error "Faltan variables requeridas"
    exit 1
fi
```

---

##### 🏗️ **5. Funciones de Construcción** (Armar comandos)

| Función | ¿Qué hace? | Ejemplo |
|---------|-----------|---------|
| `build_gradle_properties` | Convierte variables en `-Dkey=value` | Ver abajo |
| `get_gradle_command` | Retorna `./gradlew` o `gradle` | `GRADLE=$(get_gradle_command)` |

**Ejemplo de build_gradle_properties:**
```bash
# Variables en el sistema:
export DB_URL="jdbc:..."
export DB_USER="user"

# Llamar función:
PROPS=$(build_gradle_properties)

# Retorna:
"-DDB_URL=jdbc:... -DDB_USER=user"

# Usar en comando:
./gradlew test ${PROPS}
```

---

##### 🔧 **6. Funciones de Utilidad** (Varias)

| Función | ¿Para qué? | Ejemplo |
|---------|-----------|---------|
| `load_env_file "archivo"` | Carga variables desde .env | `load_env_file ".env.qa"` |
| `normalize_line_endings "archivo"` | Convierte CRLF → LF | `normalize_line_endings ".env"` |

---

#### 💻 Ejemplo Completo: Crear Tu Propio Script

```bash
#!/bin/bash

# ============================================
# Mi script personalizado usando utils.sh
# ============================================

# 1. Importar utilidades
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/utils.sh"

# 2. Mostrar banner
log_banner "Mi Script de Deploy"

# 3. Validar que Java existe
if ! check_command "java"; then
    log_error "Java no está instalado"
    exit 1
fi

# 4. Auto-detectar módulo
MODULE_NAME=$(detect_module_name)
log_info "Módulo detectado: ${MODULE_NAME}"

# 5. Buscar archivo .env
ENV_FILE=$(find_env_file)
if [[ -z "${ENV_FILE}" ]]; then
    log_warning "No se encontró archivo .env"
else
    log_info "Cargando: ${ENV_FILE}"
    load_env_file "${ENV_FILE}"
fi

# 6. Validar variables requeridas
log_info "Validando configuración..."
if ! validate_required_vars "DB_URL" "DB_USER" "DB_PASS"; then
    log_error "Faltan variables de configuración"
    exit 1
fi

# 7. Construir comando Gradle
GRADLE_CMD=$(get_gradle_command)
GRADLE_PROPS=$(build_gradle_properties)

# 8. Ejecutar
log_info "Ejecutando deploy..."
${GRADLE_CMD} deploy ${GRADLE_PROPS}

# 9. Resultado
if [ $? -eq 0 ]; then
    log_success "¡Deploy exitoso!"
else
    log_error "Deploy falló"
    exit 1
fi
```

---

#### 🚫 Lo que utils.sh NO es

- ❌ **NO** es un script ejecutable (`./utils.sh` → error)
- ❌ **NO** tiene lógica de negocio
- ❌ **NO** ejecuta tests o builds
- ❌ **NO** contiene configuración

#### ✅ Lo que utils.sh SÍ es

- ✅ **Biblioteca de funciones** reutilizables
- ✅ **Estandarización** de comportamiento
- ✅ **Reducción de duplicación** de código
- ✅ **Facilita mantenimiento** (cambias en un lugar)

---

#### 🎯 Beneficios de Usar utils.sh

1. **DRY (Don't Repeat Yourself)** - Escribes la función UNA vez
2. **Mantenibilidad** - Cambias en UN lugar, afecta a todos
3. **Consistencia** - Todos los scripts se ven/comportan igual
4. **Testeable** - Puedes probar funciones individualmente
5. **Reutilizable** - Cualquier script nuevo puede usarlas
6. **Legibilidad** - `log_success()` es más claro que `echo -e "\033[32m✓"`

---

### `clean-ide.sh` - Limpieza de IDE

**Propósito:** Eliminar archivos y directorios generados por IDEs (IntelliJ IDEA, VS Code, Eclipse).

**Ubicación:** `/Users/abel.venero/Documents/qa-scotia-frameworks/scripts/clean-ide.sh`

#### 🎯 Qué Hace

Limpia archivos de configuración del IDE que pueden causar conflictos:
- `.idea/` (IntelliJ IDEA)
- `.vscode/` (Visual Studio Code)
- `.settings/` (Eclipse)
- `.classpath`, `.project` (Eclipse)
- `*.iml` (IntelliJ módulos)
- `.DS_Store` (macOS)

#### 💡 Cuándo Usar

- **Después de cambiar de IDE** (ej: Eclipse → IntelliJ)
- **Al tener problemas de sincronización** del proyecto
- **Antes de commitear** para evitar archivos del IDE en git
- **Al recibir el proyecto** por primera vez (limpiar config de otros)

#### 🚀 Uso

```bash
# Desde el framework
cd /path/to/qa-scotia-frameworks
./scripts/clean-ide.sh

# Desde un módulo
cd /path/to/qa-module-banking
../qa-scotia-frameworks/scripts/clean-ide.sh

# Ver qué se eliminará sin ejecutar
./scripts/clean-ide.sh --dry-run
```

---

## 🔧 Configuración

### Prioridad de Configuración

El sistema de configuración sigue un modelo de **cascada con override**, donde cada nivel puede sobreescribir los anteriores:

```
┌─────────────────────────────────────┐
│  1. Argumentos CLI (--env, --tags)  │ ← MÁXIMA PRIORIDAD
├─────────────────────────────────────┤
│  2. Variables de Entorno (export)   │
├─────────────────────────────────────┤
│  3. Archivo .env.local              │
├─────────────────────────────────────┤
│  4. Archivo .env.${TEST_ENV}        │
├─────────────────────────────────────┤
│  5. Archivo .env (genérico)         │ ← MÍNIMA PRIORIDAD
└─────────────────────────────────────┘
```

#### 📖 Explicación Detallada

**1. Argumentos CLI** (Prioridad Máxima)
```bash
./run-test.sh --env qa --tags @smoke
# Siempre gana sobre cualquier otra configuración
```

**2. Variables de Entorno del Sistema**
```bash
export DB_URL=jdbc:oracle:thin:@//host:1521/service
export TEST_ENV=qa
./run-test.sh
# Útil para Jenkins/CI-CD
```

**3. Archivo .env.local** (Desarrollo Local)
```bash
# Creado en tu máquina, gitignored
# Sobrescribe .env.qa pero no variables de entorno
```

**4. Archivo .env.${TEST_ENV}** (Por Ambiente)
```bash
# .env.qa, .env.uat, .env.prod
# Se selecciona según TEST_ENV
```

**5. Archivo .env** (Fallback)
```bash
# Valores por defecto genéricos
# Se usa si no existe ningún otro
```

### Búsqueda de Archivos .env

El script busca archivos en el **directorio del módulo** (no del framework):

```bash
qa-module-banking/
├── .env.local        # ← Prioridad 1 (si existe)
├── .env.qa           # ← Prioridad 2 (si TEST_ENV=qa)
├── .env.uat          # ← Prioridad 2 (si TEST_ENV=uat)
├── .env.prod         # ← Prioridad 2 (si TEST_ENV=prod)
└── .env              # ← Prioridad 3 (fallback)
```

**Lógica de Búsqueda:**
1. Si existe `.env.local` → **usarlo** (siempre)
2. Si no, buscar `.env.${TEST_ENV}` → **usarlo** (según ambiente)
3. Si no, buscar `.env` → **usarlo** (genérico)
4. Si no hay ninguno → **solo variables de entorno**

### Variables de Entorno Soportadas

#### 🎯 Variables del Framework

| Variable | Descripción | Requerida | Default | Ejemplo |
|----------|-------------|-----------|---------|---------|
| `MODULE_NAME` | Nombre del módulo de testing | ⚪ Auto-detecta | - | `banking`, `autos` |
| `TEST_ENV` | Ambiente de ejecución | ⚪ Opcional | `local` | `qa`, `uat`, `prod` |

#### 🗄️ Variables de Base de Datos

| Variable | Descripción | Requerida | Default | Ejemplo |
|----------|-------------|-----------|---------|---------|
| `DB_URL` | URL JDBC de conexión | 🔴 **Sí** | - | `jdbc:oracle:thin:@//host:1521/service` |
| `DB_USER` | Usuario de base de datos | 🔴 **Sí** | - | `qa_user`, `dev_user` |
| `DB_PASS` | Password de base de datos | 🔴 **Sí** | - | `SecurePass123` |
| `DB_DRIVER` | Driver JDBC (clase completa) | ⚪ Opcional | `oracle.jdbc.OracleDriver` | `org.postgresql.Driver` |

#### 🌐 Variables de API Testing

| Variable | Descripción | Requerida | Default | Ejemplo |
|----------|-------------|-----------|---------|---------|
| `API_BASE_URL` | URL base del API a testear | 🟡 Condicional* | - | `https://api-qa.example.com/v1` |
| `API_TOKEN` | Token de autenticación | 🟡 Condicional* | - | `Bearer abc123xyz`, `ApiKey xyz` |

_* Requeridas solo si el módulo ejecuta tests de API_

#### 🖥️ Variables de Web Testing

| Variable | Descripción | Requerida | Default | Ejemplo |
|----------|-------------|-----------|---------|---------|
| `WEB_BASE_URL` | URL base de la aplicación web | 🟡 Condicional* | - | `https://web-qa.example.com` |
| `BROWSER` | Navegador a usar | ⚪ Opcional | `chrome` | `chrome`, `firefox`, `edge` |
| `HEADLESS` | Ejecutar sin interfaz gráfica | ⚪ Opcional | `false` | `true`, `false` |

_* Requeridas solo si el módulo ejecuta tests web_

#### 📱 Variables de Mobile Testing

| Variable | Descripción | Requerida | Default | Ejemplo |
|----------|-------------|-----------|---------|---------|
| `APP_PATH` | Ruta del APK/IPA a testear | 🟡 Condicional* | - | `/path/to/app-qa.apk` |
| `PLATFORM` | Plataforma móvil | 🟡 Condicional* | - | `Android`, `iOS` |

_* Requeridas solo si el módulo ejecuta tests mobile_

#### 🔑 Leyenda de Requisitos

- 🔴 **Requerida**: Debe estar configurada obligatoriamente
- 🟡 **Condicional**: Requerida según el tipo de tests
- ⚪ **Opcional**: Tiene valor por defecto o se auto-detecta

### Archivos .env

#### 📝 Convenciones de Nombres

```bash
.env              # Configuración genérica (commitear como .env.example)
.env.local        # Desarrollo local (NUNCA commitear)
.env.qa           # Ambiente QA
.env.uat          # Ambiente UAT/Staging
.env.prod         # Ambiente Producción (con cuidado!)
```

#### ⚠️ Importante para .gitignore

```gitignore
# En tu módulo, agregar:
.env
.env.*
!.env.example    # Permitir solo el template
```

---

## 📝 Ejemplos de Uso

### Desarrollo Local

#### 🎬 Primera Vez (Setup Completo)

```bash
# 1. Clonar módulo
git clone https://github.com/tu-org/qa-module-banking.git
cd qa-module-banking

# 2. Ejecutar configuración interactiva
../qa-scotia-frameworks/scripts/run-test.sh --setup

# Salida esperada:
# ════════════════════════════════════════
#   🚀 Configuración Interactiva
# ════════════════════════════════════════
# 
# ℹ️  Módulo detectado: qa-module-banking
# 
# ¿Qué ambiente deseas configurar?
#   1) Local (desarrollo)
#   2) QA
#   3) UAT
#   4) PROD
# Opción [1]: 1
#
# DB URL [jdbc:oracle:thin:@//host:port/service]: jdbc:oracle:thin:@//localhost:1521/XEPDB1
# DB User: dev_user
# DB Password: ********
# 
# API Base URL [Enter para omitir]: http://localhost:8080/api
# 
# ✓ Archivo .env.local creado exitosamente
# 
# ¿Deseas ejecutar los tests ahora? (s/N): s
```

#### 🏃 Ejecuciones Diarias

```bash
# Ejecución estándar
./run-test.sh

# Solo smoke tests (rápido)
./run-test.sh --tags @smoke

# Solo tests de API
./run-test.sh --tags @api

# Tests de regresión completa
./run-test.sh --tags @regression

# Con logs detallados (debug)
./run-test.sh --verbose
```

#### 🔄 Cambiar de Ambiente

```bash
# Crear configuración para QA
./run-test.sh --setup    # Seleccionar opción 2 (QA)

# Ahora ejecutar en QA
./run-test.sh --env qa

# Volver a local
./run-test.sh --env local
```

### Jenkins / CI-CD

#### 🔧 Pipeline Básico

```groovy
// Jenkinsfile en la raíz del módulo
pipeline {
    agent any
    
    environment {
        TEST_ENV = 'qa'
        DB_URL = credentials('db-url-qa')
        DB_USER = credentials('db-user-qa')
        DB_PASS = credentials('db-pass-qa')
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Test') {
            steps {
                sh '''
                    chmod +x ../qa-scotia-frameworks/scripts/run-test.sh
                    ../qa-scotia-frameworks/scripts/run-test.sh
                '''
            }
        }
    }
    
    post {
        always {
            junit '**/build/test-results/**/*.xml'
            publishHTML([
                reportDir: 'build/reports/tests/test',
                reportFiles: 'index.html',
                reportName: 'Test Report'
            ])
        }
    }
}
```

#### 🎯 Pipeline con Tags Dinámicos

```groovy
pipeline {
    parameters {
        choice(
            name: 'TEST_SUITE',
            choices: ['smoke', 'regression', 'api', 'all'],
            description: 'Suite de tests a ejecutar'
        )
    }
    
    stages {
        stage('Test') {
            steps {
                script {
                    def tags = params.TEST_SUITE == 'all' ? '' : "--tags @${params.TEST_SUITE}"
                    sh """
                        ../qa-scotia-frameworks/scripts/run-test.sh ${tags}
                    """
                }
            }
        }
    }
}
```

**Ver más ejemplos en:** [scripts/jenkins/README.md](jenkins/README.md)

### GitLab CI

```yaml
# .gitlab-ci.yml
variables:
  TEST_ENV: "qa"

stages:
  - test

test:smoke:
  stage: test
  variables:
    DB_URL: $DB_URL_QA
    DB_USER: $DB_USER_QA
    DB_PASS: $DB_PASS_QA
  script:
    - chmod +x ../qa-scotia-frameworks/scripts/run-test.sh
    - ../qa-scotia-frameworks/scripts/run-test.sh --tags @smoke
  only:
    - merge_requests
    - main

test:regression:
  stage: test
  variables:
    DB_URL: $DB_URL_QA
    DB_USER: $DB_USER_QA
    DB_PASS: $DB_PASS_QA
  script:
    - ../qa-scotia-frameworks/scripts/run-test.sh --tags @regression
  only:
    - schedules
```

### GitHub Actions

```yaml
# .github/workflows/test.yml
name: Test Suite

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    env:
      TEST_ENV: qa
      DB_URL: ${{ secrets.DB_URL_QA }}
      DB_USER: ${{ secrets.DB_USER_QA }}
      DB_PASS: ${{ secrets.DB_PASS_QA }}
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Run Tests
      run: |
        chmod +x ../qa-scotia-frameworks/scripts/test.sh
        ../qa-scotia-frameworks/scripts/test.sh --tags @smoke
    
    - name: Publish Test Report
      if: always()
      uses: mikepenz/action-junit-report@v3
      with:
        report_paths: '**/build/test-results/**/*.xml'
```

### Ejecución Manual con Variables

```bash
# Un solo comando con todas las variables
TEST_ENV=qa \
DB_URL=jdbc:oracle:thin:@//qa-db:1521/QA \
DB_USER=qa_user \
DB_PASS=SecurePass123 \
API_BASE_URL=https://api-qa.example.com \
./run-test.sh --tags @smoke --verbose

# O exportar y luego ejecutar
export TEST_ENV=qa
export DB_URL=jdbc:oracle:thin:@//qa-db:1521/QA
export DB_USER=qa_user
export DB_PASS=SecurePass123
./run-test.sh
```

---

## 🏗️ Estructura de .env

### 📋 Template Completo (.env.example)

Copia este archivo como base para tus configuraciones:

```bash
# ============================================================================
# Scotia QA Framework - Configuración de Variables de Entorno
# ============================================================================
# 
# INSTRUCCIONES:
#   1. Copiar este archivo:
#      cp .env.example .env.local
#   
#   2. Editar .env.local con valores reales
#   
#   3. NUNCA commitear .env.local (debe estar en .gitignore)
#   
#   4. Para ejecutar tests:
#      ../qa-scotia-frameworks/scripts/run-test.sh
# 
# ============================================================================

# ====================================================================
# AMBIENTE
# ====================================================================
TEST_ENV=local

# ====================================================================
# BASE DE DATOS (Oracle)
# ====================================================================
DB_URL=jdbc:oracle:thin:@//host:1521/service
DB_USER=your_username
DB_PASS=your_password
DB_DRIVER=oracle.jdbc.OracleDriver

# Otros drivers soportados:
# PostgreSQL: org.postgresql.Driver
# MySQL:      com.mysql.cj.jdbc.Driver
# SQL Server: com.microsoft.sqlserver.jdbc.SQLServerDriver

# ====================================================================
# API TESTING (opcional - solo si usas api-core)
# ====================================================================
API_BASE_URL=https://api-dev.example.com/v1
API_TOKEN=your_api_token_or_bearer

# ====================================================================
# WEB TESTING (opcional - solo si usas web-core)
# ====================================================================
WEB_BASE_URL=https://web-dev.example.com
BROWSER=chrome
HEADLESS=false

# ====================================================================
# MOBILE TESTING (opcional - solo si usas mobile-core)
# ====================================================================
APP_PATH=/path/to/your/app.apk
PLATFORM=Android

# ====================================================================
# NOTAS IMPORTANTES
# ====================================================================
# 1. NO commitear este archivo si contiene credenciales reales
# 2. Agregar .env* al .gitignore
# 3. Rotar credenciales regularmente
# 4. Para CI/CD, usar Jenkins Credentials o secrets del sistema
```

### 🔧 Por Ambiente

**`.env.local` (Desarrollo Local)**
```bash
TEST_ENV=local
DB_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
DB_USER=dev_user
DB_PASS=DevPass123
API_BASE_URL=http://localhost:8080/api
```

**`.env.qa` (Ambiente QA)**
```bash
TEST_ENV=qa
DB_URL=jdbc:oracle:thin:@//qa-db.example.com:1521/QA
DB_USER=qa_user
DB_PASS=QaSecurePass456
API_BASE_URL=https://api-qa.example.com/v1
WEB_BASE_URL=https://web-qa.example.com
```

**`.env.uat` (UAT/Staging)**
```bash
TEST_ENV=uat
DB_URL=jdbc:oracle:thin:@//uat-db.example.com:1521/UAT
DB_USER=uat_user
DB_PASS=UatSecurePass789
API_BASE_URL=https://api-uat.example.com/v1
WEB_BASE_URL=https://web-uat.example.com
```

**`.env.prod` (Producción - ⚠️ Con Cuidado)**
```bash
TEST_ENV=prod
DB_URL=jdbc:oracle:thin:@//prod-db.example.com:1521/PROD
DB_USER=prod_readonly_user   # ⚠️ SOLO LECTURA
DB_PASS=ProdReadOnlyPass
API_BASE_URL=https://api.example.com/v1
WEB_BASE_URL=https://www.example.com
```

---

## 🔒 Seguridad

### ✅ Buenas Prácticas

1. **✅ NUNCA commitear** archivos `.env*` con credenciales reales
2. **✅ SIEMPRE agregar** `.env*` al `.gitignore`
3. **✅ USAR** Jenkins Credentials o GitHub Secrets para CI/CD
4. **✅ ROTAR** credenciales regularmente
5. **✅ LIMITAR** permisos (usar usuarios read-only cuando sea posible)
6. **✅ ENCRIPTAR** archivos .env sensibles con herramientas como `git-crypt`
7. **✅ AUDITAR** accesos a credenciales

### 📝 .gitignore Recomendado

```gitignore
# Variables de entorno - IMPORTANTE
.env
.env.*
!.env.example       # Permitir solo el template

# Build outputs
build/
target/
*.jar
*.war

# IDE
.idea/
.vscode/
*.iml

# Sistema
.DS_Store
Thumbs.db
```

### 🔍 Verificar Seguridad

```bash
# 1. Verificar que .env no está trackeado en git
git ls-files | grep "\.env"
# ✅ Solo debe aparecer: .env.example

# 2. Verificar que está en .gitignore
cat .gitignore | grep ".env"
# ✅ Debe contener: .env y .env.*

# 3. Buscar credenciales expuestas en historial
git log --all --full-history --source -- "*.env*"
# ✅ No debe haber commits con .env.local o .env.qa

# 4. Verificar permisos del archivo
ls -la .env.local
# ✅ Debe ser: -rw------- (600) o similar
```

### 🚨 ¿Commiteaste credenciales por error?

```bash
# 1. INMEDIATAMENTE cambiar las credenciales expuestas
# 2. Eliminar del historial (PELIGROSO - coordinar con equipo)
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch .env.local" \
  --prune-empty --tag-name-filter cat -- --all

# 3. Forzar push (requiere coordinación)
git push origin --force --all
```

---

## 🐛 Troubleshooting

### ❌ Problema: "Permission denied"

**Síntoma:**
```bash
bash: ./run-test.sh: Permission denied
```

**Causa:** Scripts sin permisos de ejecución

**Solución:**
```bash
chmod +x /path/to/qa-scotia-frameworks/scripts/run-test.sh
chmod +x /path/to/qa-scotia-frameworks/scripts/utils.sh
```

---

### ❌ Problema: "Module not detected"

**Síntoma:**
```
⚠️ No se pudo detectar el nombre del módulo
```

**Causas posibles:**
- No estás en el directorio del módulo
- El módulo no tiene `gradle.properties` o `settings.gradle`

**Soluciones:**
```bash
# Opción 1: Ejecutar desde el directorio del módulo
cd qa-module-banking
../qa-scotia-frameworks/scripts/run-test.sh

# Opción 2: Especificar manualmente
./run-test.sh --module banking

# Opción 3: Variable de entorno
export MODULE_NAME=banking
./run-test.sh

# Opción 4: Crear gradle.properties en el módulo
echo "rootProject.name=qa-module-banking" > gradle.properties
```

---

### ❌ Problema: "No se encontró archivo de configuración"

**Síntoma:**
```
⚠️ No se encontró archivo de configuración .env
```

**Causa:** No existe ningún archivo `.env*` en el módulo

**Soluciones:**
```bash
# Opción 1: Modo setup (recomendado)
./run-test.sh --setup

# Opción 2: Copiar template
cp .env.example .env.local
# Editar .env.local con valores reales

# Opción 3: Usar variables de entorno
export DB_URL=jdbc:...
export DB_USER=user
export DB_PASS=pass
./run-test.sh
```

---

### ❌ Problema: "Variables no configuradas"

**Síntoma:**
```
✗ DB_URL no está configurada
✗ DB_USER no está configurada
✗ DB_PASS no está configurada
```

**Causa:** Variables requeridas no están definidas

**Solución:**
```bash
# Ver qué variables faltan
./run-test.sh --dry-run

# Configurar interactivamente
./run-test.sh --setup

# O editar tu archivo .env
vi .env.local
```

---

### ❌ Problema: "Gradle not found"

**Síntoma:**
```
gradle: command not found
```

**Causa:** Gradle no está instalado y no existe `./gradlew`

**Solución:**
```bash
# El script busca automáticamente ./gradlew primero

# Si no tienes wrapper, instalar Gradle:

# macOS
brew install gradle

# Linux con SDKMAN
curl -s "https://get.sdkman.io" | bash
sdk install gradle

# O generar wrapper
gradle wrapper --gradle-version=8.14
```

---

### ❌ Problema: "utils.sh: No such file"

**Síntoma:**
```
source: utils.sh: No such file or directory
```

**Causa:** El script no encuentra `utils.sh`

**Solución:**
```bash
# Verificar que utils.sh existe
ls -la /path/to/qa-scotia-frameworks/scripts/utils.sh

# Si no existe, re-descargar el framework
git pull origin main
```

---

### ❌ Problema: Tests fallan con "Connection refused"

**Síntoma:**
```
java.sql.SQLException: Connection refused
```

**Causa:** DB_URL incorrecta o BD no accesible

**Solución:**
```bash
# 1. Verificar que la BD está corriendo
ping db-host

# 2. Verificar URL en .env
cat .env.local | grep DB_URL

# 3. Probar conexión manualmente
sqlplus ${DB_USER}/${DB_PASS}@${DB_URL}

# 4. Verificar firewall/VPN
telnet db-host 1521
```

---

### ❌ Problema: Variables con espacios o caracteres especiales

**Síntoma:**
```
-Dpassword=my: command not found
```

**Causa:** Password con espacios sin comillas

**Solución:**
```bash
# En .env, usar comillas para valores con espacios
DB_PASS="my password 123"
API_TOKEN="Bearer abc def"

# O escapar caracteres especiales
DB_PASS=my\ password\ 123
```

---

## 🚀 Jenkins Integration

Para configuración detallada de Jenkins, ver: **[jenkins/README.md](jenkins/README.md)**

### ⚡ Inicio Rápido Jenkins

```groovy
// Jenkinsfile básico
pipeline {
    agent any
    environment {
        TEST_ENV = 'qa'
        DB_URL = credentials('db-url-qa')
        DB_USER = credentials('db-user-qa')
        DB_PASS = credentials('db-pass-qa')
    }
    stages {
        stage('Test') {
            steps {
                sh '../qa-scotia-frameworks/scripts/run-test.sh'
            }
        }
    }
}
```

---

## 📚 Documentación Adicional

### 📖 Framework

- **[Framework Guide](../documentacion/FRAMEWORK-GUIDE.md)** - Guía completa del framework
- **[Contributing Guide](../CONTRIBUTING.md)** - Cómo contribuir
- **[Troubleshooting](../TROUBLESHOOTING.md)** - Solución de problemas generales

### 🎯 Capas del Framework

- **[Common README](../common/README.md)** - Módulo común
- **[API Core README](../api-core/README.md)** - Testing de APIs
- **[Web Core README](../web-core/README.md)** - Testing web
- **[Mobile Core README](../mobile-core/README.md)** - Testing mobile

### 🔧 Scripts y Configuración

- **[Jenkins Integration](jenkins/README.md)** - Configuración CI/CD
- **[Template Script](../documentacion/template-script.md)** - Plantilla para nuevos scripts

---

## 🤝 Soporte y Contribución

### ❓ ¿Necesitas Ayuda?

1. **Revisar documentación:**
   - [TROUBLESHOOTING.md](../TROUBLESHOOTING.md) - Problemas comunes
   - Este README - Ejemplos de uso
   - [jenkins/README.md](jenkins/README.md) - Configuración CI/CD

2. **Consultar ejemplos:**
   - [jenkins/Jenkinsfile.simple](jenkins/Jenkinsfile.simple) - Pipeline básico
   - [../doc/](../documentacion/) - Documentación detallada

3. **Contactar al equipo:**
   - Abrir issue en el repositorio
   - Consultar con el equipo QA

### 🐛 Reportar Problemas

Si encuentras un bug o tienes una sugerencia:

1. Verificar que no esté en [TROUBLESHOOTING.md](../TROUBLESHOOTING.md)
2. Abrir un issue con:
   - Descripción clara del problema
   - Pasos para reproducir
   - Logs relevantes
   - Versión del framework

### 💡 Contribuir

Ver [CONTRIBUTING.md](../CONTRIBUTING.md) para guías de contribución.

---

## 💡 Scripts Adicionales Recomendados

Los siguientes scripts **NO están implementados aún**, pero son sugerencias para mejorar la calidad y productividad del framework. Todos pueden usar `utils.sh` como base.

### 🧪 `validate-framework.sh` - Validador del Framework

**Propósito:** Validar que el framework está correctamente instalado y configurado.

**Funcionalidades:**
- ✅ Verificar estructura de directorios completa
- ✅ Validar que existen las capas (common, api-core, web-core, mobile-core)
- ✅ Verificar versiones de Java (≥ 21) y Gradle (≥ 8.14)
- ✅ Detectar dependencias faltantes en build.gradle
- ✅ Validar publicación en Maven Local
- ✅ Verificar permisos de scripts

**Uso:**
```bash
# Validación completa
./validate-framework.sh

# Solo verificar Java/Gradle
./validate-framework.sh --check-deps

# Generar reporte detallado
./validate-framework.sh --report
```

**Beneficio:** Detectar problemas de instalación **antes** de ejecutar tests.

---

### 📊 `analyze-results.sh` - Analizador de Resultados

**Propósito:** Analizar resultados de tests y generar métricas útiles.

**Funcionalidades:**
- ✅ Parsear archivos JUnit XML de `build/test-results/`
- ✅ Generar estadísticas (passed/failed/skipped)
- ✅ Identificar los 10 tests más lentos
- ✅ Detectar tests "flaky" (intermitentes)
- ✅ Calcular % de cobertura por tag (@api, @web, @smoke)
- ✅ Generar reporte en HTML/Markdown

**Uso:**
```bash
# Analizar último build
./analyze-results.sh

# Analizar directorio específico
./analyze-results.sh --dir build/test-results/test

# Generar HTML
./analyze-results.sh --output html
```

**Beneficio:** Identificar problemas de performance y estabilidad.

---

### 🔄 `sync-modules.sh` - Sincronizador de Módulos

**Propósito:** Sincronizar múltiples módulos con la última versión del framework.

**Funcionalidades:**
- ✅ Detectar módulos en directorio padre
- ✅ Verificar versión del framework en cada módulo
- ✅ Actualizar referencias en `build.gradle`
- ✅ Validar compatibilidad de versiones
- ✅ Ejecutar smoke tests después de actualizar

**Uso:**
```bash
# Detectar módulos desactualizados
./sync-modules.sh --check

# Actualizar todos los módulos
./sync-modules.sh --update-all

# Actualizar módulo específico
./sync-modules.sh --module banking --update
```

**Beneficio:** Mantener consistencia de versiones en múltiples módulos.

---

### 🎯 `tag-analyzer.sh` - Analizador de Tags

**Propósito:** Analizar y validar el uso de tags de Cucumber en features.

**Funcionalidades:**
- ✅ Listar todos los tags usados en el proyecto
- ✅ Detectar tags huérfanos (nunca ejecutados)
- ✅ Sugerir tags faltantes (@author, @priority, etc.)
- ✅ Validar nomenclatura de tags (convenciones)
- ✅ Generar estadísticas de cobertura por tag

**Uso:**
```bash
# Listar todos los tags
./tag-analyzer.sh --list

# Detectar tags no usados
./tag-analyzer.sh --orphans

# Validar convenciones
./tag-analyzer.sh --validate
```

**Beneficio:** Mantener orden y consistencia en tags de Cucumber.

---

### 🚀 `pre-commit.sh` - Hook de Pre-Commit

**Propósito:** Validaciones automáticas antes de hacer commit (Git hook).

**Funcionalidades:**
- ✅ Ejecutar smoke tests (@smoke)
- ✅ Validar formato de código con Spotless
- ✅ Verificar que no hay credenciales expuestas
- ✅ Validar sintaxis de archivos .feature (Gherkin)
- ✅ Verificar que build.gradle no tiene versiones SNAPSHOT

**Instalación:**
```bash
# Copiar a hooks de git
cp pre-commit.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

**Uso:**
```bash
# Se ejecuta automáticamente al hacer commit
git commit -m "feat: nueva funcionalidad"

# Ejecutar manualmente
./pre-commit.sh

# Saltar validaciones (emergencia)
git commit --no-verify
```

**Beneficio:** Evitar commits con problemas (calidad preventiva).

---

### 🔧 Cómo Implementar Estos Scripts

Todos estos scripts pueden usar `utils.sh` como base. Ejemplo:

```bash
#!/bin/bash
# validate-framework.sh

# Importar utilidades
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/utils.sh"

log_banner "Validación del Framework"

# Verificar Java
if check_command "java"; then
    JAVA_VERSION=$(java -version 2>&1 | grep version | awk '{print $3}')
    log_success "Java instalado: ${JAVA_VERSION}"
else
    log_error "Java NO encontrado"
    exit 1
fi

# ... más validaciones usando funciones de utils.sh
```

---

## 📄 Licencia

Scotia QA Framework © 2025  
Todos los derechos reservados.

---

## 📋 Changelog

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0.0 | Nov 2025 | Release inicial con scripts genéricos |
| | | - test.sh: Script unificado |
| | | - utils.sh: Biblioteca de utilidades |
| | | - Soporte Jenkins/GitLab/GitHub |

---

**Última actualización**: 28 de Noviembre de 2025  
**Autor**: Abel Venero  
**Versión**: 1.0.0  
**Mantenido por**: Equipo Scotia QA

