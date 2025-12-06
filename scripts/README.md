# 🚀 Scripts del Framework Scotia QA

Sistema de scripts **cross-platform** (macOS/Linux + Windows) para automatización de testing, mantenimiento y ejecución de módulos de pruebas.

**Versión:** 1.0.0  
**Última Actualización:** 5 de Diciembre de 2025  
**Sistemas Soportados:** macOS, Linux, Windows (PowerShell)

---

## 📑 ÍNDICE

- [🎯 Visión General](#-visión-general)
- [🏗️ Arquitectura Cross-Platform](#️-arquitectura-cross-platform)
- [📍 Ubicación y Uso](#-ubicación-y-uso)
- [📦 Scripts Disponibles](#-scripts-disponibles)
- [⚡ Inicio Rápido](#-inicio-rápido)
  - [Para Módulos Nuevos](#para-módulos-nuevos)
  - [Para Módulos Existentes](#para-módulos-existentes)
- [🔧 Configuración](#-configuración)
- [📖 Guía Detallada por Script](#-guía-detallada-por-script)
- [💻 Uso por Sistema Operativo](#-uso-por-sistema-operativo)
- [🔄 Actualización de Scripts](#-actualización-de-scripts)
- [📝 Ejemplos de Uso](#-ejemplos-de-uso)
- [🐛 Troubleshooting](#-troubleshooting)
- [🚀 CI/CD Integration](#-cicd-integration)
- [📚 Documentación Adicional](#-documentación-adicional)

---

## 🎯 Visión General

El framework incluye **11 scripts** organizados en 3 categorías:

| Categoría | Scripts | Ubicación | Propósito |
|-----------|---------|-----------|-----------|
| **CORE** | `utils.sh`, `utils.ps1` | JAR de common | Funciones compartidas (empaquetados) |
| **CUSTOM** | `run-test.*`, `sync-utils.*` | Módulos | Ejecución y sincronización |
| **FRAMEWORK** | `create-module.sh`, etc. | Solo framework | Herramientas de desarrollo |

---

## 🏗️ Arquitectura Cross-Platform

### **Innovación Principal: Scripts CORE en JAR**

Los scripts `utils.sh` y `utils.ps1` se empaquetan dentro del JAR de `common`:

```
common-1.0.0.jar
└── META-INF/scripts/
    ├── utils.sh      ← Funciones Bash (12 KB)
    └── utils.ps1     ← Funciones PowerShell (15 KB)
```

**Ventajas:**
- ✅ Versionado coherente (common:1.0.0 = scripts v1.0.0)
- ✅ Distribución automática vía Maven/Artifactory
- ✅ Actualización simple con `sync-utils.*`
- ✅ No requiere acceso al repositorio del framework

### **Flujo de Sincronización**

```
Framework → Compile → JAR → Maven Local → Módulos
   ↓           ↓        ↓         ↓           ↓
utils.sh   copyScripts common- ~/.m2/    sync-utils
           ToResources 1.0.0.jar          extrae scripts
```

---

## 📍 Ubicación y Uso

### **EN EL FRAMEWORK (Desarrollo)**

```
qa-scotia-frameworks/
└── scripts/
    ├── utils.sh              ← Master (se copia a JAR)
    ├── utils.ps1             ← Master (se copia a JAR)
    ├── sync-utils.sh         ← Se copia a módulos
    ├── sync-utils.ps1        ← Se copia a módulos
    ├── run-test.sh           ← Se copia a módulos
    ├── run-test.ps1          ← Se copia a módulos
    ├── create-module.sh      ← Solo en framework
    ├── analyze-results.sh    ← Opcional en módulos
    ├── code-quality.sh       ← Opcional en módulos
    ├── pre-commit.sh         ← Opcional en módulos
    └── clean-ide.sh          ← Opcional en módulos
```

### **EN LOS MÓDULOS (Ejecución)**

```
qa-module-banking/
├── scripts/
│   ├── utils.sh              ← 🔄 Desde JAR (actualizable)
│   ├── utils.ps1             ← 🔄 Desde JAR (actualizable)
│   ├── sync-utils.sh         ← 🔒 Custom (no cambia)
│   ├── sync-utils.ps1        ← 🔒 Custom (no cambia)
│   ├── run-test.sh           ← 🔒 Custom (no cambia)
│   ├── run-test.ps1          ← 🔒 Custom (no cambia)
│   └── [scripts opcionales]  ← Según necesidad
├── src/test/
├── .env.local                ← Configuración (gitignored)
└── build.gradle              ← Depende de common
```

**Clasificación:**
- 🔄 **CORE**: Se actualizan con `sync-utils.*`
- 🔒 **CUSTOM**: Copiados al crear módulo, personalizables
- 📋 **OPCIONALES**: Copiados solo si se necesitan

---

## 📦 Scripts Disponibles

### **🔵 Scripts CORE (En JAR)**

| Script | Sistema | Propósito | Actualización |
|--------|---------|-----------|---------------|
| `utils.sh` | macOS/Linux | Funciones compartidas Bash | `sync-utils.sh` |
| `utils.ps1` | Windows | Funciones compartidas PowerShell | `sync-utils.ps1` |

**Características:**
- Empaquetados en `common-X.X.X.jar`
- Versionados con el framework
- NUNCA se modifican en módulos
- Importados automáticamente por otros scripts

---

### **🟢 Scripts CUSTOM (Módulos)**

| Script | Sistema | Propósito | Personalizable |
|--------|---------|-----------|----------------|
| `run-test.sh` | macOS/Linux | Ejecutar tests | ✅ SÍ |
| `run-test.ps1` | Windows | Ejecutar tests | ✅ SÍ |
| `sync-utils.sh` | macOS/Linux | Sincronizar utils desde JAR | ✅ SÍ |
| `sync-utils.ps1` | Windows | Sincronizar utils desde JAR | ✅ SÍ |

**Características:**
- Copiados al crear módulo con `create-module.sh`
- Pueden personalizarse según necesidad del equipo
- NO se sobrescriben al actualizar utils
- Dependen de `utils.*` para funciones compartidas

---

### **🟡 Scripts FRAMEWORK (Herramientas)**

| Script | Sistema | Propósito | Ubicación |
|--------|---------|-----------|-----------|
| `create-module.sh` | macOS/Linux | Crear módulos nuevos | Solo framework |
| `analyze-results.sh` | Cross-platform | Analizar resultados de tests | Framework/Módulos |
| `code-quality.sh` | Cross-platform | Verificar calidad de código | Framework/Módulos |
| `pre-commit.sh` | Cross-platform | Hook Git pre-commit | Módulos |
| `clean-ide.sh` | Cross-platform | Limpiar archivos IDE | Framework/Módulos |

**Características:**
- Herramientas de desarrollo y mantenimiento
- Se copian opcionalmente a módulos
- Útiles para desarrollo local y CI/CD

---

## ⚡ Inicio Rápido

### **Para Módulos Nuevos**

#### **Paso 1: Crear Módulo**

```bash
# Desde el framework
cd qa-scotia-frameworks/
./scripts/create-module.sh banking

# Resultado: qa-module-banking/ con scripts incluidos
```

**Scripts copiados automáticamente:**
- ✅ `utils.sh` y `utils.ps1` (desde master)
- ✅ `sync-utils.sh` y `sync-utils.ps1`
- ✅ `run-test.sh` y `run-test.ps1`
- ✅ Estructura completa de directorios
- ✅ Archivos de configuración (`.env.local`, `config-scotia.properties`)

#### **Paso 2: Configurar Credenciales**

```bash
cd qa-module-banking/

# Editar .env.local con credenciales reales
nano .env.local
```

**Variables mínimas requeridas:**
```properties
# Ambiente
TEST_ENV=local

# Base de Datos (si usa Test Data Finder)
DB_URL=jdbc:oracle:thin:@//host:1521/service
DB_USER=usuario
DB_PASS=password

# API (si usa api-core)
API_BASE_URL=https://api-dev.example.com/v1

# Web (si usa web-core)
WEB_BASE_URL=https://app-dev.example.com
BROWSER=chrome
```

#### **Paso 3: Ejecutar Tests**

**macOS/Linux:**
```bash
./scripts/run-test.sh
```

**Windows:**
```powershell
.\scripts\run-test.ps1
```

**¡Listo!** 🎉 El módulo está operativo.

---

### **Para Módulos Existentes (Migración)**

Si ya tienes un módulo y quieres adoptar la nueva arquitectura cross-platform:

#### **Paso 1: Actualizar Dependencia**

Editar `build.gradle`:

```gradle
dependencies {
    // Actualizar versión de common
    testImplementation 'com.scotia.qa:common:1.0.0'  // ← Mínimo 1.0.0
    
    // Resto de dependencias
    testImplementation 'com.scotia.qa:api-core:1.0.0'
    testImplementation 'com.scotia.qa:web-core:1.0.0'
}
```

#### **Paso 2: Copiar Scripts de Sincronización**

```bash
# Desde el framework, copiar a tu módulo
cd qa-scotia-frameworks/
cp scripts/sync-utils.sh ../qa-module-banking/scripts/
cp scripts/sync-utils.ps1 ../qa-module-banking/scripts/
cp scripts/run-test.ps1 ../qa-module-banking/scripts/  # Si usas Windows

# Hacer ejecutables (macOS/Linux)
chmod +x ../qa-module-banking/scripts/sync-utils.sh
chmod +x ../qa-module-banking/scripts/run-test.sh
```

#### **Paso 3: Sincronizar Utils desde JAR**

```bash
cd qa-module-banking/

# macOS/Linux
./scripts/sync-utils.sh

# Windows
.\scripts\sync-utils.ps1
```

**Salida esperada:**
```
════════════════════════════════════════
  🔄 Sincronizar Scripts desde common
════════════════════════════════════════

ℹ️  JAR encontrado: common-1.0.0.jar
ℹ️  Versión: 1.0.0
ℹ️  Fecha: 2025-12-04 17:41:48

ℹ️  Extrayendo scripts desde: common-1.0.0.jar
✓ utils.sh actualizado
✓ utils.ps1 actualizado

✓ Scripts sincronizados exitosamente
```

#### **Paso 4: Eliminar Scripts Legacy (si existen)**

```bash
# Si tenías el viejo sistema
rm -f scripts/update-scripts.sh
```

#### **Paso 5: Probar**

```bash
./scripts/run-test.sh

# o en Windows
.\scripts\run-test.ps1
```

---

## 🔧 Configuración

### **Archivos de Configuración**

El framework soporta múltiples métodos de configuración:

#### **1. Archivo `.env.local` (Recomendado para desarrollo)**

```properties
# .env.local (NO commitear - debe estar en .gitignore)

TEST_ENV=local
DB_URL=jdbc:oracle:thin:@//host:1521/service
DB_USER=usuario
DB_PASS=password
API_BASE_URL=https://api-dev.example.com/v1
WEB_BASE_URL=https://app-dev.example.com
BROWSER=chrome
HEADLESS=false
```

#### **2. Archivo `config-scotia.properties`**

```properties
# src/test/resources/config-scotia.properties

# Soporta variables de entorno con ${VAR}
test.env=${{TEST_ENV}}
db.url=${{DB_URL}}
db.username=${{DB_USER}}
db.password=${{DB_PASS}}
api.base.url=${{API_BASE_URL}}
web.base.url=${{WEB_BASE_URL}}
```

#### **3. Variables de Entorno (CI/CD)**

```bash
# Jenkins, GitLab CI, GitHub Actions
export TEST_ENV=qa
export DB_URL=jdbc:oracle:thin:@//prod-host:1521/service
export DB_USER=qa_user
export DB_PASS=secure_password

./scripts/run-test.sh
```

### **Prioridad de Configuración**

```
1. Argumentos CLI (--env qa, --tags @smoke)     ← Mayor prioridad
2. Variables de entorno (export VAR=value)
3. Archivo .env.local
4. Archivo .env.${TEST_ENV} (ej: .env.qa)
5. Archivo .env (genérico)
6. config-scotia.properties (valores default)   ← Menor prioridad
```

### **Variables Soportadas**

| Variable | Propósito | Ejemplo | Requerida |
|----------|-----------|---------|-----------|
| `TEST_ENV` | Ambiente de ejecución | `local`, `qa`, `prod` | ✅ |
| `DB_URL` | URL de base de datos | `jdbc:oracle:thin:@//host:1521/service` | Si usa BD |
| `DB_USER` | Usuario de BD | `testuser` | Si usa BD |
| `DB_PASS` | Password de BD | `password123` | Si usa BD |
| `DB_DRIVER` | Driver JDBC | `oracle.jdbc.OracleDriver` | ❌ (default) |
| `API_BASE_URL` | URL base de API | `https://api.example.com/v1` | Si usa API |
| `API_TOKEN` | Token de autenticación | `Bearer xyz123...` | Si requiere auth |
| `WEB_BASE_URL` | URL de aplicación web | `https://app.example.com` | Si usa Web |
| `BROWSER` | Navegador para tests | `chrome`, `firefox` | ❌ (default: chrome) |
| `HEADLESS` | Modo headless | `true`, `false` | ❌ (default: false) |
| `APP_PATH` | Ruta a APK/IPA (mobile) | `/path/to/app.apk` | Si usa Mobile |

---

## 📖 Guía Detallada por Script

### **1. `create-module.sh` - Creador de Módulos**

**Propósito:** Crear módulos de prueba completos desde cero.

**Ubicación:** Solo en framework (`qa-scotia-frameworks/scripts/`)

**Uso:**
```bash
# Crear módulo con todas las capas
./scripts/create-module.sh banking

# Crear en ubicación específica
./scripts/create-module.sh autos --dest ~/projects

# Solo con api-core
./scripts/create-module.sh cards --with-api

# Solo con web-core
./scripts/create-module.sh mobile --with-web
```

**¿Qué crea?**
- ✅ Estructura completa de directorios
- ✅ Scripts (run-test, utils, sync-utils) para ambos OS
- ✅ `build.gradle` configurado con dependencias
- ✅ `.env.local` template
- ✅ `config-scotia.properties`
- ✅ Feature y Steps de ejemplo
- ✅ Cucumber hooks
- ✅ `.gitignore` configurado
- ✅ `README.md` del módulo

**Cuándo usar:**
- Al iniciar un nuevo proyecto de automatización
- Para crear módulos de prueba rápidamente
- Para estandarizar estructura entre equipos

---

### **2. `run-test.sh` / `run-test.ps1` - Ejecutor de Tests**

**Propósito:** Ejecutar tests con configuración automática.

**Ubicación:** Módulos (`qa-module-*/scripts/`)

**Uso Básico:**

**macOS/Linux:**
```bash
# Ejecución simple
./scripts/run-test.sh

# Modo setup (asistente interactivo)
./scripts/run-test.sh --setup

# Ambiente específico
./scripts/run-test.sh --env qa

# Tags de Cucumber
./scripts/run-test.sh --tags "@smoke"

# Modo verbose
./scripts/run-test.sh --verbose

# Dry-run (ver comando sin ejecutar)
./scripts/run-test.sh --dry-run
```

**Windows PowerShell:**
```powershell
# Ejecución simple
.\scripts\run-test.ps1

# Modo setup
.\scripts\run-test.ps1 -Setup

# Ambiente específico
.\scripts\run-test.ps1 -Env qa

# Tags de Cucumber
.\scripts\run-test.ps1 -Tags "@smoke"

# Modo verbose
.\scripts\run-test.ps1 -Verbose

# Dry-run
.\scripts\run-test.ps1 -DryRun
```

**Modo Setup Interactivo:**

```bash
./scripts/run-test.sh --setup

# Asistente pregunta:
# 1. ¿Qué ambiente? (local/qa/uat/prod)
# 2. URL de BD
# 3. Usuario de BD
# 4. Password de BD
# 5. API Base URL (opcional)
# 6. ¿Ejecutar tests ahora?

# Crea .env.local automáticamente
```

**Características:**
- ✅ Auto-detección del módulo
- ✅ Búsqueda automática de `.env` files
- ✅ Validación de dependencias (Java, Gradle)
- ✅ Construcción de propiedades Gradle (-D flags)
- ✅ Modo interactivo para setup inicial
- ✅ Soporte para CI/CD (variables de entorno)

**Cuándo usar:**
- Ejecución diaria de tests
- Desarrollo local
- CI/CD pipelines
- Validación rápida de cambios

---

### **3. `sync-utils.sh` / `sync-utils.ps1` - Sincronizador**

**Propósito:** Actualizar scripts CORE desde el JAR de common.

**Ubicación:** Módulos (`qa-module-*/scripts/`)

**Uso:**

**macOS/Linux:**
```bash
# Sincronizar con última versión
./scripts/sync-utils.sh

# Sincronizar con versión específica
./scripts/sync-utils.sh --version 1.0.1

# Ver ayuda
./scripts/sync-utils.sh --help
```

**Windows PowerShell:**
```powershell
# Sincronizar con última versión
.\scripts\sync-utils.ps1

# Sincronizar con versión específica
.\scripts\sync-utils.ps1 -Version "1.0.1"

# Ver ayuda
.\scripts\sync-utils.ps1 -Help
```

**¿Qué hace?**
1. Busca JAR de `common` en Maven local (`~/.m2/repository/`)
2. Extrae `META-INF/scripts/utils.sh` y `utils.ps1`
3. Copia a `scripts/` del módulo (sobrescribe SOLO utils.*)
4. Hace ejecutable `utils.sh` (macOS/Linux)

**Archivos que actualiza:**
- ✅ `scripts/utils.sh`
- ✅ `scripts/utils.ps1`

**Archivos que NO toca:**
- ✅ `scripts/run-test.*` (custom)
- ✅ `scripts/sync-utils.*` (custom)
- ✅ Cualquier otro script custom

**Cuándo usar:**
- Después de actualizar versión de `common` en build.gradle
- Cuando se agrega una nueva función a utils en el framework
- Al incorporar correcciones del framework
- Periódicamente (mensual) para mantener sincronizado

**Ejemplo completo:**

```bash
# 1. Actualizar dependencia (build.gradle)
# common:1.0.0 → common:1.0.1

# 2. Sincronizar scripts
cd qa-module-banking/
./scripts/sync-utils.sh

# Salida:
# ℹ️  JAR encontrado: common-1.0.1.jar
# ✓ utils.sh actualizado
# ✓ utils.ps1 actualizado

# 3. Verificar cambios
git diff scripts/utils.sh

# 4. Probar
./scripts/run-test.sh
```

---

### **4. `utils.sh` / `utils.ps1` - Funciones Compartidas**

**Propósito:** Librería de funciones reutilizables.

**Ubicación:** 
- **Master:** Framework (`qa-scotia-frameworks/scripts/`)
- **Distribución:** JAR de common (`META-INF/scripts/`)
- **Uso:** Módulos (`qa-module-*/scripts/`)

**NO SE EJECUTA DIRECTAMENTE** - Se importa desde otros scripts.

**Funciones Disponibles:**

#### **Logging:**
```bash
log_success "Operación completada"    # ✓ verde
log_error "Error crítico"              # ✗ rojo
log_warning "Advertencia"              # ⚠️ amarillo
log_info "Información"                 # ℹ️ cyan
log_banner "Título del Banner"         # Banner azul
log_separator                          # Línea separadora
```

#### **Detección de Entorno:**
```bash
os=$(detect_os)                        # "macOS", "Linux", "Windows"
is_jenkins && echo "Running in Jenkins"
is_ci && echo "Running in CI/CD"
```

#### **Detección de Módulo:**
```bash
module=$(detect_module_name)           # Auto-detecta desde gradle.properties
```

#### **Configuración:**
```bash
env_file=$(find_env_file)              # Busca .env.local, .env.qa, etc.
load_env_file ".env.local"             # Carga variables
```

#### **Validación:**
```bash
validate_required_vars "DB_URL" "DB_USER" "DB_PASS"
```

#### **Gradle:**
```bash
gradle_cmd=$(get_gradle_command)       # ./gradlew o gradle
props=$(build_gradle_properties)       # -DDB_URL=... -DDB_USER=...
```

#### **Dependencias:**
```bash
check_command "java" "Java JDK"
check_framework_dependencies
```

**Cómo importar en tus scripts custom:**

```bash
#!/bin/bash

# Cargar utilidades
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/utils.sh"

# Usar funciones
log_banner "Mi Script Custom"
module=$(detect_module_name)
log_success "Módulo detectado: $module"
```

**⚠️ IMPORTANTE:**
- NUNCA editar `utils.sh` o `utils.ps1` en módulos
- Se sobrescriben al ejecutar `sync-utils.*`
- Si necesitas funciones custom, créalas en otro archivo

---

### **5. Scripts Opcionales**

Estos scripts se copian opcionalmente según necesidad:

#### **`analyze-results.sh` - Analizador de Resultados**

Analiza resultados de tests y genera reportes.

```bash
./scripts/analyze-results.sh
```

#### **`code-quality.sh` - Calidad de Código**

Verifica calidad con Checkstyle, SpotBugs, etc.

```bash
./scripts/code-quality.sh
```

#### **`pre-commit.sh` - Hook Git**

Valida código antes de commit.

```bash
# Instalar como hook
cp scripts/pre-commit.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

#### **`clean-ide.sh` - Limpieza IDE**

Limpia archivos temporales de IntelliJ, Eclipse, etc.

```bash
./scripts/clean-ide.sh
```

---

## 💻 Uso por Sistema Operativo

### **🍎 macOS / Linux**

**Requisitos:**
- Bash 4.0+
- Java 21+
- Gradle 8.14+ (o usar wrapper)

**Comandos:**
```bash
# Crear módulo
./scripts/create-module.sh banking

# Sincronizar utils
cd qa-module-banking/
./scripts/sync-utils.sh

# Ejecutar tests
./scripts/run-test.sh

# Con opciones
./scripts/run-test.sh --env qa --tags "@smoke"
```

---

### **🪟 Windows (PowerShell)**

**Requisitos:**
- PowerShell 5.1+ (o PowerShell Core 7+)
- Java 21+
- Gradle 8.14+ (o usar wrapper)

**Configurar Política de Ejecución:**
```powershell
# Verificar política actual
Get-ExecutionPolicy

# Si es "Restricted", cambiar a "RemoteSigned"
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

---

#### **🚀 Cómo Ejecutar Scripts PowerShell (4 Métodos)**

##### **Método 1: PowerShell Terminal (RECOMENDADO) ✅**

**Paso 1: Abrir PowerShell**
```
1. Presionar tecla Windows
2. Escribir "PowerShell"
3. Click en "Windows PowerShell" o "PowerShell 7"
```

**Paso 2: Navegar al módulo**
```powershell
cd C:\Users\TuUsuario\Projects\qa-module-banking
```

**Paso 3: Ejecutar script**
```powershell
.\scripts\run-test.ps1
```

**Con opciones:**
```powershell
.\scripts\run-test.ps1 -Env qa -Tags "@smoke" -Verbose
```

---

##### **Método 2: Click Derecho "Ejecutar con PowerShell" ⚠️**

```
1. Navegar a la carpeta: C:\...\qa-module-banking\scripts\
2. Click DERECHO en: run-test.ps1
3. Seleccionar: "Ejecutar con PowerShell"
```

**⚠️ LIMITACIONES:**
- NO permite pasar parámetros (-Env, -Tags, etc.)
- Se ejecuta con configuración por defecto
- Ventana se cierra automáticamente al terminar
- **Solo usar para pruebas rápidas**

**🔧 Solución:** Crear un archivo `.bat` wrapper:

**`run-test-qa.bat`:**
```batch
@echo off
powershell.exe -ExecutionPolicy Bypass -File "%~dp0scripts\run-test.ps1" -Env qa -Tags "@smoke"
pause
```

Ahora puedes hacer **doble click en `run-test-qa.bat`** y se ejecutará con parámetros.

---

##### **Método 3: Visual Studio Code (VS Code) 🔵**

**Paso 1: Abrir VS Code**
```
1. Abrir carpeta del módulo: File → Open Folder → qa-module-banking
2. Abrir terminal integrada: Terminal → New Terminal
3. Asegurarse que esté en PowerShell (abajo a la derecha debe decir "pwsh" o "powershell")
```

**Paso 2: Ejecutar script**
```powershell
.\scripts\run-test.ps1
```

**Ventajas:**
- ✅ Terminal integrada
- ✅ Auto-completado
- ✅ Control de versiones integrado
- ✅ Debugging de scripts

---

##### **Método 4: Windows Terminal (MODERNO) 🟦**

**Paso 1: Instalar Windows Terminal (opcional)**
```
Microsoft Store → Buscar "Windows Terminal" → Instalar
```

**Paso 2: Abrir en la carpeta del módulo**
```
1. En Explorador de Windows, navegar a: C:\...\qa-module-banking
2. Click DERECHO en la carpeta
3. Seleccionar: "Abrir en Terminal" (Windows 11) o "Open in Windows Terminal"
```

**Paso 3: Ejecutar script**
```powershell
.\scripts\run-test.ps1
```

**Ventajas:**
- ✅ Moderna interfaz con pestañas
- ✅ Soporte para múltiples shells (PowerShell, CMD, WSL)
- ✅ Temas y personalización
- ✅ Mejor rendimiento

---

#### **⚡ Atajos de Teclado en PowerShell**

| Atajo | Función |
|-------|---------|
| `Tab` | Auto-completar rutas/comandos |
| `Ctrl + C` | Cancelar comando en ejecución |
| `Ctrl + L` | Limpiar pantalla (o `cls`) |
| `↑` / `↓` | Navegar historial de comandos |
| `Ctrl + R` | Buscar en historial |
| `F7` | Ver historial completo |

---

**Comandos Básicos:**
```powershell
# Crear módulo (usar Git Bash o WSL)
./scripts/create-module.sh banking

# Sincronizar utils
cd qa-module-banking\
.\scripts\sync-utils.ps1

# Ejecutar tests
.\scripts\run-test.ps1

# Con opciones
.\scripts\run-test.ps1 -Env qa -Tags "@smoke"
```

---

#### **📸 Ejemplo Visual: Ejecutar Tests en Windows**

```
┌────────────────────────────────────────────────────────────────┐
│ Explorador de Windows                                          │
├────────────────────────────────────────────────────────────────┤
│ 📁 C:\Users\TuUsuario\Projects\qa-module-banking              │
│                                                                │
│ 📁 .gradle                                                     │
│ 📁 build                                                       │
│ 📂 scripts                     ← Click aquí con SHIFT+Derecho │
│   ├── 📄 run-test.ps1         ← El script a ejecutar         │
│   ├── 📄 sync-utils.ps1                                       │
│   └── 📄 utils.ps1                                            │
│ 📁 src                                                         │
│ 📄 .env.local                                                  │
│ 📄 build.gradle                                                │
└────────────────────────────────────────────────────────────────┘

OPCIÓN 1: Abrir PowerShell aquí
─────────────────────────────────
1. SHIFT + Click DERECHO en carpeta "scripts"
2. Seleccionar: "Abrir ventana de PowerShell aquí"
3. Ejecutar: .\run-test.ps1

OPCIÓN 2: Navegar desde PowerShell
───────────────────────────────────
1. Abrir PowerShell (Tecla Windows → "PowerShell")
2. cd C:\Users\TuUsuario\Projects\qa-module-banking
3. .\scripts\run-test.ps1

OPCIÓN 3: Crear acceso directo
───────────────────────────────
1. Click DERECHO en escritorio → Nuevo → Acceso directo
2. Ubicación: 
   powershell.exe -ExecutionPolicy Bypass -File "C:\Users\TuUsuario\Projects\qa-module-banking\scripts\run-test.ps1"
3. Nombre: "Ejecutar Tests QA Banking"
4. Doble click para ejecutar
```

---

#### **🎬 Paso a Paso: Primera Ejecución en Windows**

**Escenario:** Tienes un módulo recién clonado y quieres ejecutar tests.

**Paso 1: Verificar Requisitos**
```powershell
# Abrir PowerShell
# Verificar Java
java -version
# Debe mostrar: openjdk version "21.0.x" o superior

# Verificar política de ejecución
Get-ExecutionPolicy
# Si muestra "Restricted", ejecutar:
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

**Paso 2: Navegar al Módulo**
```powershell
# Ver tu ubicación actual
pwd

# Navegar al módulo
cd C:\Users\TuUsuario\Projects\qa-module-banking

# Confirmar ubicación
pwd
# Debe mostrar: C:\Users\TuUsuario\Projects\qa-module-banking
```

**Paso 3: Verificar Scripts**
```powershell
# Listar scripts
Get-ChildItem scripts\

# Debe mostrar:
# run-test.ps1
# sync-utils.ps1
# utils.ps1
```

**Paso 4: Configurar Variables (Primera vez)**
```powershell
# Editar .env.local
notepad .env.local

# Agregar configuración mínima:
# TEST_ENV=local
# DB_URL=jdbc:oracle:thin:@//host:1521/service
# DB_USER=testuser
# DB_PASS=password123

# Guardar y cerrar
```

**Paso 5: Ejecutar Tests**
```powershell
# Ejecución simple
.\scripts\run-test.ps1

# Ver salida:
# ════════════════════════════════════════════
#   🚀 Ejecutar Tests - qa-module-banking
# ════════════════════════════════════════════
# 
# ✓ Variables cargadas desde .env.local
# ✓ Tests ejecutándose...
```

**Paso 6: Ver Reportes**
```powershell
# Abrir reporte HTML
Start-Process "build\reports\cucumber\cucumber-html-report.html"

# O navegar manualmente:
explorer.exe build\reports\cucumber\
```

---

#### **🔧 Troubleshooting: Ejecución en Windows**

**Problema 1: "No se puede ejecutar run-test.ps1"**
```
Error: run-test.ps1 : File cannot be loaded because running scripts is disabled on this system.
```

**Solución:**
```powershell
# Verificar política actual
Get-ExecutionPolicy

# Cambiar política (ejecutar como Administrador)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# O ejecutar con bypass (temporal)
powershell.exe -ExecutionPolicy Bypass -File .\scripts\run-test.ps1
```

---

**Problema 2: "Script no encontrado"**
```
Error: .\run-test.ps1 : The term '.\run-test.ps1' is not recognized
```

**Causa:** Estás en el directorio incorrecto.

**Solución:**
```powershell
# Ver dónde estás
pwd

# Navegar a la raíz del módulo
cd C:\Users\TuUsuario\Projects\qa-module-banking

# Listar archivos
Get-ChildItem

# Ahora ejecutar
.\scripts\run-test.ps1
```

---

**Problema 3: "Doble click no hace nada"**
```
Al hacer doble click en run-test.ps1, se abre y cierra rápidamente
```

**Causa:** Windows abre el script en editor por defecto.

**Solución A: Usar terminal**
```powershell
# Siempre usar PowerShell terminal
.\scripts\run-test.ps1
```

**Solución B: Crear archivo .bat**
```batch
REM Crear: ejecutar-tests.bat
@echo off
powershell.exe -ExecutionPolicy Bypass -NoExit -File "%~dp0scripts\run-test.ps1"
```

Ahora doble click en `ejecutar-tests.bat` funciona correctamente.

---

**Problema 4: "Ventana se cierra inmediatamente"**
```
Al ejecutar, la ventana de PowerShell se cierra antes de ver el resultado
```

**Solución A: Agregar pause**
```powershell
# Al final del script
.\scripts\run-test.ps1
pause
```

**Solución B: Usar -NoExit**
```powershell
powershell.exe -NoExit -File .\scripts\run-test.ps1
```

**Solución C: Ejecutar desde terminal ya abierta**
```powershell
# Abrir PowerShell primero, LUEGO ejecutar script
.\scripts\run-test.ps1
# La terminal permanece abierta
```

---

**Problema 5: "Click derecho no muestra opción PowerShell"**
```
Al hacer click derecho, no aparece "Ejecutar con PowerShell"
```

**Solución A: Usar SHIFT + Click Derecho**
```
SHIFT + Click Derecho → "Abrir ventana de PowerShell aquí"
```

**Solución B: Instalar Windows Terminal**
```
Microsoft Store → "Windows Terminal" → Instalar
Luego: Click Derecho → "Abrir en Terminal"
```

**Solución C: Agregar al menú contextual (Registry)**
```powershell
# Ejecutar como Administrador
reg add "HKCR\Microsoft.PowerShellScript.1\Shell\Run\Command" /d "\"C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe\" -NoExit -File \"%1\"" /f
```

---

**Problema 6: "Parámetros no funcionan con click derecho"**
```
Quiero ejecutar con -Env qa pero click derecho no permite parámetros
```

**Solución: Crear scripts wrapper personalizados**

**`ejecutar-tests-qa.bat`:**
```batch
@echo off
powershell.exe -ExecutionPolicy Bypass -NoExit -File "%~dp0scripts\run-test.ps1" -Env qa
```

**`ejecutar-tests-smoke.bat`:**
```batch
@echo off
powershell.exe -ExecutionPolicy Bypass -NoExit -File "%~dp0scripts\run-test.ps1" -Tags "@smoke"
```

Ahora puedes hacer doble click en cada `.bat` según necesites.

---

**Equivalencias Bash ↔ PowerShell:**

| Bash | PowerShell | Descripción |
|------|------------|-------------|
| `./script.sh` | `.\script.ps1` | Ejecutar script |
| `./script.sh --env qa` | `.\script.ps1 -Env qa` | Parámetro con valor |
| `./script.sh --verbose` | `.\script.ps1 -Verbose` | Flag booleano |
| `chmod +x script.sh` | N/A | No necesario en Windows |
| `ls -la` | `Get-ChildItem` o `dir` | Listar archivos |
| `pwd` | `Get-Location` o `pwd` | Ver directorio actual |
| `cd ~` | `cd $HOME` | Ir a home |
| `clear` | `Clear-Host` o `cls` | Limpiar pantalla |

---

## 🔄 Actualización de Scripts y Flujo de Trabajo

### **📋 Flujo Completo: De Framework a Módulos**

Este es el flujo detallado de cómo los scripts se actualizan y distribuyen desde el framework hasta los módulos:

```
┌──────────────────────────────────────────────────────────────────┐
│ 1️⃣  DESARROLLO EN FRAMEWORK (Desarrolladores Core)              │
└──────────────────────────────────────────────────────────────────┘
  qa-scotia-frameworks/
  └── scripts/
      ├── utils.sh         ← Se edita/actualiza aquí
      └── utils.ps1        ← Se edita/actualiza aquí
      
  ✏️  Editar funciones en utils.sh/utils.ps1
  ✅  Commit y push a repositorio del framework
  
┌──────────────────────────────────────────────────────────────────┐
│ 2️⃣  COMPILACIÓN Y EMPAQUETADO (build.gradle)                    │
└──────────────────────────────────────────────────────────────────┘
  ./gradlew :common:clean :common:build
  
  Scripts se copian al JAR durante compilación:
  
  common/src/main/resources/META-INF/scripts/
  ├── utils.sh
  └── utils.ps1
       ↓
  common-1.0.0.jar
  └── META-INF/scripts/
      ├── utils.sh      ← Empaquetado
      └── utils.ps1     ← Empaquetado

┌──────────────────────────────────────────────────────────────────┐
│ 3️⃣  PUBLICACIÓN (Maven Local o Artifactory)                     │
└──────────────────────────────────────────────────────────────────┘
  # Maven Local (desarrollo)
  ./gradlew :common:publishToMavenLocal
  
  # Artifactory (producción)
  ./gradlew :common:publish
  
  Resultado:
  ~/.m2/repository/com/scotia/qa/common/1.0.0/
  └── common-1.0.0.jar  ← Contiene los scripts actualizados

┌──────────────────────────────────────────────────────────────────┐
│ 4️⃣  ACTUALIZACIÓN EN MÓDULOS (QA/Testers)                       │
└──────────────────────────────────────────────────────────────────┘
  qa-module-banking/
  
  PASO A: Actualizar dependencia en build.gradle
  ─────────────────────────────────────────────────
  dependencies {
      testImplementation 'com.scotia.qa:common:1.0.0'  →  1.0.1
  }
  
  PASO B: Descargar nueva versión
  ─────────────────────────────────
  ./gradlew clean build --refresh-dependencies
  
  PASO C: Sincronizar scripts desde el JAR
  ─────────────────────────────────────────
  # macOS/Linux
  ./scripts/sync-utils.sh
  
  # Windows
  .\scripts\sync-utils.ps1
  
  El script sync-utils:
  1. Busca el JAR en ~/.m2/repository/
  2. Extrae META-INF/scripts/utils.sh y utils.ps1
  3. Copia a scripts/ del módulo (sobrescribe SOLO utils.*)
  4. Mantiene intactos run-test.*, sync-utils.* (custom)
  
  PASO D: Verificar y ejecutar
  ─────────────────────────────
  git diff scripts/utils.sh
  ./scripts/run-test.sh  # Probar nueva versión

┌──────────────────────────────────────────────────────────────────┐
│ 5️⃣  EJECUCIÓN DE TESTS (QA/CI/CD)                               │
└──────────────────────────────────────────────────────────────────┘
  Scripts en el módulo:
  
  🔄 CORE (actualizables vía sync-utils):
     ├── utils.sh          ← Versión del framework
     └── utils.ps1         ← Versión del framework
  
  🔒 CUSTOM (personalizables, NO se sobrescriben):
     ├── run-test.sh       ← Usa utils.sh
     ├── run-test.ps1      ← Usa utils.ps1
     ├── sync-utils.sh     ← Extrae de JAR
     └── sync-utils.ps1    ← Extrae de JAR
  
  📋 OPCIONALES (copiados al crear módulo, personalizables):
     ├── analyze-results.sh
     ├── code-quality.sh
     └── pre-commit.sh
```

---

### **🔁 Flujo Simplificado de Uso en Módulos**

#### **🎯 Objetivo: Ejecutar tests en un módulo**

```
┌─────────────────────────────────────────────────────────────┐
│ ESCENARIO A: Módulo Nuevo (Desde Cero)                     │
└─────────────────────────────────────────────────────────────┘

1️⃣  Crear módulo desde framework:
    cd qa-scotia-frameworks/
    ./scripts/create-module.sh banking
    
    ✅ Se copian TODOS los scripts (utils, run-test, sync-utils)
    ✅ Se crea .env.local template
    ✅ Se configura build.gradle con dependencias

2️⃣  Configurar credenciales:
    cd qa-module-banking/
    nano .env.local  # Agregar DB_URL, DB_USER, etc.

3️⃣  Ejecutar tests:
    # macOS/Linux
    ./scripts/run-test.sh
    
    # Windows
    .\scripts\run-test.ps1
    
    ✅ run-test.* importa utils.* automáticamente
    ✅ Carga variables desde .env.local
    ✅ Ejecuta tests con configuración correcta

┌─────────────────────────────────────────────────────────────┐
│ ESCENARIO B: Módulo Existente (Actualizar Scripts)         │
└─────────────────────────────────────────────────────────────┘

1️⃣  Actualizar dependencia de common:
    nano build.gradle
    # common:1.0.0 → common:1.0.1

2️⃣  Descargar nueva versión:
    ./gradlew clean build --refresh-dependencies

3️⃣  Sincronizar scripts CORE:
    # macOS/Linux
    ./scripts/sync-utils.sh
    
    # Windows
    .\scripts\sync-utils.ps1
    
    ✅ Se actualizan SOLO utils.sh y utils.ps1
    ❌ NO se tocan run-test.*, sync-utils.* (custom)

4️⃣  Ejecutar tests:
    ./scripts/run-test.sh  # Usa los utils actualizados

┌─────────────────────────────────────────────────────────────┐
│ ESCENARIO C: Análisis de Código (Opcional)                 │
└─────────────────────────────────────────────────────────────┘

Los scripts de análisis (analyze-results, code-quality) se usan
OPCIONALMENTE en los módulos para validar calidad:

1️⃣  Ejecutar tests:
    ./scripts/run-test.sh

2️⃣  Analizar resultados:
    ./scripts/analyze-results.sh
    
    ✅ Genera reporte con métricas
    ✅ Identifica tests fallidos
    ✅ Calcula cobertura

3️⃣  Verificar calidad de código:
    ./scripts/code-quality.sh
    
    ✅ Ejecuta Checkstyle
    ✅ Ejecuta SpotBugs
    ✅ Busca vulnerabilidades

4️⃣  Pre-commit hook (opcional):
    cp scripts/pre-commit.sh .git/hooks/pre-commit
    chmod +x .git/hooks/pre-commit
    
    ✅ Valida antes de commit
    ✅ Evita código con errores

🔍 IMPORTANTE: Estos scripts analizan el CÓDIGO DEL MÓDULO,
   NO del framework. Por eso se ejecutan en los módulos.
```

---

### **🪟 Configuración Específica para Windows**

#### **⚙️ Requisitos Previos**

```powershell
# 1. Verificar PowerShell
$PSVersionTable.PSVersion
# Debe ser 5.1+ o PowerShell Core 7+

# 2. Configurar política de ejecución
Get-ExecutionPolicy
# Si muestra "Restricted", cambiar:
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 3. Verificar Java
java -version
# Debe mostrar Java 21+

# 4. Verificar Gradle (opcional, se puede usar wrapper)
gradle --version
```

#### **📁 Estructura de Directorios en Windows**

```
C:\Users\TuUsuario\Projects\qa-module-banking\
├── scripts\
│   ├── utils.ps1          ← Funciones compartidas (PowerShell)
│   ├── sync-utils.ps1     ← Sincronizador (PowerShell)
│   └── run-test.ps1       ← Ejecutor de tests (PowerShell)
├── .env.local             ← Variables de entorno
└── build.gradle           ← Configuración del proyecto
```

⚠️ **NOTA:** Windows usa `\` como separador, pero Gradle y Git usan `/`

#### **🔧 Variables de Entorno en Windows**

**Opción 1: Archivo `.env.local` (Recomendado)**

```properties
# .env.local (Windows usa mismo formato que Linux)
TEST_ENV=local
DB_URL=jdbc:oracle:thin:@//10.34.36.43:1628/Banking
DB_USER=TESTUSER
DB_PASS=Password123!
API_BASE_URL=https://api-dev.example.com/v1
WEB_BASE_URL=https://app-dev.example.com
BROWSER=chrome
HEADLESS=false
```

**Opción 2: Variables de Sistema (Para todo el sistema)**

```powershell
# Abrir configuración de variables de entorno
rundll32 sysdm.cpl,EditEnvironmentVariables

# O vía PowerShell (temporal, solo sesión actual):
$env:TEST_ENV = "local"
$env:DB_URL = "jdbc:oracle:thin:@//host:1521/service"
$env:DB_USER = "testuser"
$env:DB_PASS = "password123"
```

**Opción 3: Variables en PowerShell Profile (Persistentes)**

```powershell
# Editar perfil de PowerShell
notepad $PROFILE

# Agregar variables:
$env:TEST_ENV = "local"
$env:DB_URL = "jdbc:oracle:thin:@//host:1521/service"
$env:DB_USER = "testuser"
$env:DB_PASS = "password123"

# Recargar perfil
. $PROFILE
```

#### **🚀 Ejecutar Tests en Windows**

**Ejecución Básica:**
```powershell
# Navegar al módulo
cd C:\Users\TuUsuario\Projects\qa-module-banking

# Ejecutar tests
.\scripts\run-test.ps1
```

**Con Opciones:**
```powershell
# Ambiente específico
.\scripts\run-test.ps1 -Env qa

# Tags de Cucumber
.\scripts\run-test.ps1 -Tags "@smoke"

# Modo verbose
.\scripts\run-test.ps1 -Verbose

# Ver comando sin ejecutar
.\scripts\run-test.ps1 -DryRun

# Modo setup interactivo
.\scripts\run-test.ps1 -Setup
```

**Sincronizar Scripts:**
```powershell
# Actualizar utils desde JAR de common
.\scripts\sync-utils.ps1

# Con versión específica
.\scripts\sync-utils.ps1 -Version "1.0.1"
```

#### **🔍 Solución de Problemas en Windows**

**Problema 1: "No se puede ejecutar scripts"**
```
Error: File cannot be loaded because running scripts is disabled
```
**Solución:**
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

---

**Problema 2: "Gradlew no encontrado"**
```
Error: .\gradlew : The term 'gradlew' is not recognized
```
**Solución:**
```powershell
# Usar gradlew.bat en Windows
.\gradlew.bat test

# O configurar alias
Set-Alias -Name gradlew -Value .\gradlew.bat
```

---

**Problema 3: "Encoding incorrecto en .env.local"**
```
Error: Invalid character in .env.local
```
**Solución:**
```powershell
# Guardar .env.local con UTF-8 sin BOM
# En Notepad++: Encoding → UTF-8 without BOM
# En VS Code: "Save with Encoding" → UTF-8
```

---

**Problema 4: "CRLF vs LF"**
```
Error: Line endings not compatible
```
**Solución:**
```powershell
# Configurar Git para manejar line endings
git config --global core.autocrlf true

# Para scripts Bash (sync-utils.sh, run-test.sh):
git config --global core.eol lf
```

---

**Problema 5: "JAR no encontrado en sync-utils"**
```
Error: No se encontró JAR de common
```
**Solución:**
```powershell
# Verificar Maven local
$mavenLocal = "$env:USERPROFILE\.m2\repository\com\scotia\qa\common"
Get-ChildItem $mavenLocal -Recurse -Filter "common-*.jar"

# Si no existe, descargar dependencia:
.\gradlew clean build --refresh-dependencies
```

---

#### **🔄 Equivalencias macOS/Linux ↔ Windows**

| Tarea | macOS/Linux | Windows PowerShell |
|-------|-------------|-------------------|
| Ejecutar script | `./script.sh` | `.\script.ps1` |
| Ver ayuda | `./script.sh --help` | `.\script.ps1 -Help` |
| Parámetro | `--env qa` | `-Env qa` |
| Flag booleano | `--verbose` | `-Verbose` |
| Variable temporal | `export VAR=value` | `$env:VAR = "value"` |
| Ver variable | `echo $VAR` | `$env:VAR` |
| Ruta actual | `pwd` | `Get-Location` o `pwd` |
| Listar archivos | `ls -la` | `Get-ChildItem` o `ls` |
| Limpiar consola | `clear` | `Clear-Host` o `cls` |
| Buscar texto | `grep "pattern"` | `Select-String "pattern"` |
| Maven local | `~/.m2/repository` | `$env:USERPROFILE\.m2\repository` |
| Separador rutas | `/` | `\` (pero `/` funciona en PowerShell) |

#### **📋 Checklist de Configuración en Windows**

**Para QA/Testers que ejecutan tests en Windows:**

- [ ] **PowerShell 5.1+ instalado**
  ```powershell
  $PSVersionTable.PSVersion
  ```

- [ ] **Política de ejecución configurada**
  ```powershell
  Get-ExecutionPolicy  # Debe mostrar "RemoteSigned" o "Unrestricted"
  ```

- [ ] **Java 21+ instalado**
  ```powershell
  java -version
  ```

- [ ] **JAVA_HOME configurado**
  ```powershell
  $env:JAVA_HOME  # Debe apuntar a JDK 21
  ```

- [ ] **Git configurado para line endings**
  ```powershell
  git config --global core.autocrlf true
  ```

- [ ] **Maven local accesible**
  ```powershell
  Test-Path "$env:USERPROFILE\.m2\repository"
  ```

- [ ] **Archivo .env.local con encoding UTF-8 (sin BOM)**
  - Usar Notepad++, VS Code, o cualquier editor que soporte UTF-8 sin BOM

- [ ] **Scripts descargados y en el módulo**
  ```powershell
  Get-ChildItem scripts\  # Debe mostrar utils.ps1, run-test.ps1, sync-utils.ps1
  ```

- [ ] **Tests ejecutándose correctamente**
  ```powershell
  .\scripts\run-test.ps1 -Verbose
  ```

---

### **📊 Versionado de Scripts**

Los scripts CORE están versionados con el framework:

| Versión common | Versión scripts | Cambios |
|----------------|-----------------|---------|
| 1.0.0 | 1.0.0 | Versión inicial con cross-platform |
| 1.0.1 | 1.0.1 | Agregar validate_json() |
| 1.1.0 | 1.1.0 | Soporte para feature flags |

**Ver versión actual:**

**macOS/Linux:**
```bash
head -20 scripts/utils.sh | grep "SCRIPT_VERSION"
```

**Windows:**
```powershell
Get-Content scripts\utils.ps1 | Select-Object -First 20 | Select-String "SCRIPT_VERSION"
```

---

### **🎯 Resumen del Flujo para QA/Testers**

```
┌─────────────────────────────────────────────────────────────┐
│ ¿QUÉ NECESITO HACER EN MI MÓDULO?                          │
└─────────────────────────────────────────────────────────────┘

1️⃣  ACTUALIZAR SCRIPTS DESDE EL FRAMEWORK:
   ✅ Editar build.gradle (actualizar versión de common)
   ✅ Ejecutar: sync-utils.sh o sync-utils.ps1
   ✅ Resultado: utils.* actualizados desde el JAR

2️⃣  EJECUTAR TESTS:
   ✅ Ejecutar: run-test.sh o run-test.ps1
   ✅ Resultado: Tests ejecutados con configuración correcta

3️⃣  ANALIZAR CÓDIGO/RESULTADOS (OPCIONAL):
   ✅ Ejecutar: analyze-results.sh, code-quality.sh, etc.
   ✅ Resultado: Reportes de calidad y cobertura
   
💡 IMPORTANTE:
   - Los scripts de análisis SE EJECUTAN EN LOS MÓDULOS
   - Analizan el CÓDIGO DEL MÓDULO, no del framework
   - Son opcionales, pero recomendados para CI/CD
```

---

## 📝 Ejemplos de Uso

### **Ejemplo 1: Desarrollo Local (macOS)**

```bash
# Día 1: Crear módulo
cd ~/projects/
git clone https://github.com/scotia/qa-scotia-frameworks.git
cd qa-scotia-frameworks/
./scripts/create-module.sh banking

# Configurar
cd ../qa-module-banking/
nano .env.local  # Agregar credenciales

# Ejecutar tests
./scripts/run-test.sh

# Ver resultados
open build/reports/cucumber/cucumber-html-report.html
```

---

### **Ejemplo 2: Equipo Mixto (Windows + macOS)**

**Repositorio compartido:**
```
qa-module-banking/
├── scripts/
│   ├── run-test.sh      ← Para macOS/Linux
│   ├── run-test.ps1     ← Para Windows
│   ├── utils.sh         ← Funciones Bash
│   └── utils.ps1        ← Funciones PowerShell
└── .gitignore           ← .env.local no se commitea
```

**QA en macOS:**
```bash
./scripts/run-test.sh
```

**QA en Windows:**
```powershell
.\scripts\run-test.ps1
```

**✅ Ambos ejecutan los mismos tests con la misma configuración**

---

### **Ejemplo 3: CI/CD (Jenkins en Windows)**

**Jenkinsfile:**
```groovy
pipeline {
    agent { label 'windows' }
    
    environment {
        TEST_ENV = 'qa'
        DB_URL = credentials('banking-db-url')
        DB_USER = credentials('banking-db-user')
        DB_PASS = credentials('banking-db-pass')
    }
    
    stages {
        stage('Checkout') {
            steps {
                git 'https://github.com/scotia/qa-module-banking.git'
            }
        }
        
        stage('Sync Scripts') {
            steps {
                powershell '.\\scripts\\sync-utils.ps1'
            }
        }
        
        stage('Tests') {
            steps {
                powershell '.\\scripts\\run-test.ps1 -Verbose'
            }
        }
        
        stage('Reports') {
            steps {
                publishHTML([
                    reportName: 'Cucumber Reports',
                    reportDir: 'build/reports/cucumber',
                    reportFiles: 'cucumber-html-report.html'
                ])
                
                junit 'build/test-results/test/*.xml'
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'build/reports/**', allowEmptyArchive: true
        }
    }
}
```

---

### **Ejemplo 4: GitLab CI (Linux)**

**`.gitlab-ci.yml`:**
```yaml
variables:
  TEST_ENV: "qa"
  DB_URL: $QA_DB_URL           # Variable de GitLab
  DB_USER: $QA_DB_USER
  DB_PASS: $QA_DB_PASS

stages:
  - sync
  - test
  - report

sync_scripts:
  stage: sync
  script:
    - ./scripts/sync-utils.sh
  artifacts:
    paths:
      - scripts/utils.sh
      - scripts/utils.ps1

run_tests:
  stage: test
  script:
    - ./scripts/run-test.sh --verbose
  artifacts:
    when: always
    reports:
      junit: build/test-results/test/*.xml
    paths:
      - build/reports/

publish_reports:
  stage: report
  script:
    - echo "Tests completados"
  dependencies:
    - run_tests
```

---

### **Ejemplo 5: GitHub Actions (Multi-OS)**

**`.github/workflows/tests.yml`:**
```yaml
name: Tests

on: [push, pull_request]

jobs:
  test-linux:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Sync Scripts
        run: ./scripts/sync-utils.sh
      
      - name: Run Tests
        env:
          TEST_ENV: qa
          DB_URL: ${{ secrets.DB_URL }}
          DB_USER: ${{ secrets.DB_USER }}
          DB_PASS: ${{ secrets.DB_PASS }}
        run: ./scripts/run-test.sh
      
      - name: Publish Reports
        uses: actions/upload-artifact@v3
        with:
          name: test-reports-linux
          path: build/reports/

  test-windows:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Sync Scripts
        run: .\scripts\sync-utils.ps1
        shell: powershell
      
      - name: Run Tests
        env:
          TEST_ENV: qa
          DB_URL: ${{ secrets.DB_URL }}
          DB_USER: ${{ secrets.DB_USER }}
          DB_PASS: ${{ secrets.DB_PASS }}
        run: .\scripts\run-test.ps1
        shell: powershell
      
      - name: Publish Reports
        uses: actions/upload-artifact@v3
        with:
          name: test-reports-windows
          path: build/reports/
```

---

## 🐛 Troubleshooting

### **Problema 1: "utils.sh not found"**

**Síntoma:**
```bash
./scripts/run-test.sh
source: utils.sh: No such file or directory
```

**Solución:**
```bash
# Sincronizar desde JAR
./scripts/sync-utils.sh

# Si falla, verificar dependencia
./gradlew dependencies | grep common

# Debe mostrar: com.scotia.qa:common:1.0.0
# Si no está, agregar en build.gradle
```

---

### **Problema 2: "JAR de common no encontrado"**

**Síntoma:**
```bash
./scripts/sync-utils.sh
❌ No se encontró ningún JAR de common en Maven local
```

**Soluciones:**

**Opción A: Publicar desde framework**
```bash
cd qa-scotia-frameworks/
./gradlew :common:publishToMavenLocal
```

**Opción B: Forzar descarga (si está en Artifactory)**
```bash
cd qa-module-banking/
./gradlew clean build --refresh-dependencies

# Verificar
ls ~/.m2/repository/com/scotia/qa/common/1.0.0/
# Debe listar: common-1.0.0.jar
```

---

### **Problema 3: Scripts PowerShell no se ejecutan**

**Síntoma:**
```powershell
.\scripts\run-test.ps1
run-test.ps1 cannot be loaded because running scripts is disabled
```

**Solución:**
```powershell
# Ver política actual
Get-ExecutionPolicy

# Si es "Restricted", cambiar
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# O ejecutar temporalmente
powershell -ExecutionPolicy Bypass -File .\scripts\run-test.ps1
```

---

### **Problema 4: Variables de .env.local no se cargan**

**Síntoma:**
```bash
./scripts/run-test.sh
⚠️ DB_URL no está configurada
```

**Soluciones:**

**1. Verificar que existe:**
```bash
ls -la .env.local
```

**2. Verificar formato:**
```bash
# Sin BOM, sin espacios extra
file .env.local
# Debe decir: ASCII text

# Ver contenido
cat .env.local | grep DB_URL
# Debe mostrar: DB_URL=jdbc:...
```

**3. Convertir saltos de línea (si viene de Windows):**
```bash
dos2unix .env.local
```

**4. Verificar .gitignore:**
```bash
cat .gitignore | grep .env
# Debe incluir:
# .env.local
# .env.*.local
```

---

### **Problema 5: "Permission denied" en scripts**

**Síntoma:**
```bash
./scripts/run-test.sh
-bash: ./scripts/run-test.sh: Permission denied
```

**Solución:**
```bash
# Hacer ejecutables
chmod +x scripts/*.sh

# O específicamente
chmod +x scripts/run-test.sh
chmod +x scripts/sync-utils.sh
```

---

## 🚀 CI/CD Integration

### **Variables de Entorno en CI/CD**

**Jenkins:**
```groovy
environment {
    TEST_ENV = 'qa'
    DB_URL = credentials('db-url-id')
    DB_USER = credentials('db-user-id')
    DB_PASS = credentials('db-pass-id')
}
```

**GitLab CI:**
```yaml
variables:
  TEST_ENV: "qa"
  DB_URL: $QA_DB_URL     # Variable protegida
  DB_USER: $QA_DB_USER
  DB_PASS: $QA_DB_PASS
```

**GitHub Actions:**
```yaml
env:
  TEST_ENV: qa
  DB_URL: ${{ secrets.DB_URL }}
  DB_USER: ${{ secrets.DB_USER }}
  DB_PASS: ${{ secrets.DB_PASS }}
```

### **Buenas Prácticas CI/CD**

✅ **DO:**
- Usar secretos/credentials para datos sensibles
- Sincronizar scripts antes de ejecutar (`sync-utils.*`)
- Archivar reportes como artifacts
- Usar variables de entorno en lugar de `.env` files
- Publicar reportes JUnit/Cucumber

❌ **DON'T:**
- Commitear `.env.local` con credenciales
- Hardcodear passwords en Jenkinsfile
- Olvidar actualizar scripts en CI/CD
- Ejecutar sin validar dependencias

---

## 📚 Documentación Adicional

### **Documentos Relacionados**

- **[SCRIPTS-GUIDE.md](./SCRIPTS-GUIDE.md)** - Documentación técnica completa (1442 líneas)
  - Arquitectura detallada
  - Diagramas de flujo
  - Plan de implementación
  - Troubleshooting avanzado
  - Roadmap futuro

- **[FRAMEWORK-GUIDE.md](../documentacion/FRAMEWORK-GUIDE.md)** - Guía general del framework
  - Arquitectura de capas
  - Patrones de diseño
  - Mejores prácticas

- **[QUICK-START.md](../documentacion/QUICK-START.md)** - Inicio rápido
  - Setup inicial
  - Primer módulo
  - Ejemplos básicos

### **Capas del Framework**

- **[common/README.md](../common/README.md)** - Componentes compartidos
- **[api-core/README.md](../api-core/README.md)** - Testing de APIs REST
- **[web-core/README.md](../web-core/README.md)** - Testing web con Selenium
- **[mobile-core/README.md](../mobile-core/README.md)** - Testing mobile con Appium

---

## 🎯 Resumen de Comandos

### **Setup Inicial (Una Vez)**

```bash
# Crear módulo
./scripts/create-module.sh banking

# Configurar
cd qa-module-banking/
nano .env.local

# Ejecutar
./scripts/run-test.sh
```

### **Uso Diario**

```bash
# Ejecutar tests
./scripts/run-test.sh

# Con opciones
./scripts/run-test.sh --env qa --tags "@smoke"

# Modo setup
./scripts/run-test.sh --setup
```

### **Actualización (Mensual)**

```bash
# 1. Actualizar common en build.gradle
nano build.gradle  # common:1.0.0 → common:1.0.1

# 2. Sincronizar scripts
./scripts/sync-utils.sh

# 3. Probar
./scripts/run-test.sh
```

### **Windows (PowerShell)**

```powershell
# Ejecutar tests
.\scripts\run-test.ps1

# Sincronizar
.\scripts\sync-utils.ps1

# Con opciones
.\scripts\run-test.ps1 -Env qa -Tags "@smoke"
```

---

## ✨ Características Principales

- ✅ **Cross-Platform**: Scripts para macOS/Linux (Bash) y Windows (PowerShell)
- ✅ **Versionado**: Scripts sincronizados con versión del framework
- ✅ **Auto-Detección**: Módulo, ambiente, archivos de configuración
- ✅ **CI/CD Ready**: Soporte para Jenkins, GitLab CI, GitHub Actions
- ✅ **Modo Interactivo**: Setup asistido para nuevos usuarios
- ✅ **Actualización Simple**: Un comando (`sync-utils.*`) sincroniza desde JAR
- ✅ **Sin Duplicación**: Scripts CORE empaquetados en common JAR
- ✅ **Personalizable**: Scripts CUSTOM modificables en módulos
- ✅ **Documentado**: Documentación exhaustiva y ejemplos

---

**Versión:** 1.0.0  
**Última Actualización:** 4 de Diciembre de 2025  
**Autor:** Abel Venero  
**Framework:** Scotia QA Framework

**¿Preguntas o problemas?** Consulta [SCRIPTS-GUIDE.md](./SCRIPTS-GUIDE.md) para documentación técnica completa.

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

## 📋 Quick Reference: Windows PowerShell

Comandos rápidos para el día a día en Windows:

### **🚀 Ejecución Rápida**

```powershell
# ========================================
# BÁSICO: Ejecutar tests (más común)
# ========================================
cd C:\Users\TuUsuario\Projects\qa-module-banking
.\scripts\run-test.ps1

# ========================================
# CON AMBIENTE ESPECÍFICO
# ========================================
.\scripts\run-test.ps1 -Env qa          # Ambiente QA
.\scripts\run-test.ps1 -Env uat         # Ambiente UAT
.\scripts\run-test.ps1 -Env prod        # Producción

# ========================================
# CON TAGS DE CUCUMBER
# ========================================
.\scripts\run-test.ps1 -Tags "@smoke"           # Solo smoke tests
.\scripts\run-test.ps1 -Tags "@regression"      # Regression completa
.\scripts\run-test.ps1 -Tags "@smoke and @api"  # Combinación

# ========================================
# MODO VERBOSE (Ver detalles)
# ========================================
.\scripts\run-test.ps1 -Verbose

# ========================================
# MODO DRY-RUN (Ver comando sin ejecutar)
# ========================================
.\scripts\run-test.ps1 -DryRun

# ========================================
# ACTUALIZAR SCRIPTS
# ========================================
.\scripts\sync-utils.ps1                # Última versión
.\scripts\sync-utils.ps1 -Version 1.0.1 # Versión específica

# ========================================
# ABRIR REPORTES
# ========================================
Start-Process "build\reports\cucumber\cucumber-html-report.html"
Start-Process "build\reports\tests\test\index.html"
```

### **📁 Navegación Básica**

```powershell
# Ver ubicación actual
pwd
Get-Location

# Cambiar directorio
cd C:\Users\TuUsuario\Projects\qa-module-banking

# Listar archivos
Get-ChildItem
dir
ls

# Listar scripts
Get-ChildItem scripts\

# Ver contenido archivo
Get-Content .env.local
type .env.local

# Editar archivo
notepad .env.local
code .env.local  # Si tienes VS Code
```

### **🔧 Configuración Primera Vez**

```powershell
# Paso 1: Verificar Java
java -version
# Debe mostrar: Java 21+

# Paso 2: Configurar política PowerShell
Get-ExecutionPolicy
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Paso 3: Navegar al módulo
cd C:\Users\TuUsuario\Projects\qa-module-banking

# Paso 4: Configurar .env.local
notepad .env.local
# Agregar:
# TEST_ENV=local
# DB_URL=jdbc:oracle:thin:@//host:1521/service
# DB_USER=testuser
# DB_PASS=password123

# Paso 5: Sincronizar scripts (primera vez)
.\scripts\sync-utils.ps1

# Paso 6: Ejecutar tests
.\scripts\run-test.ps1
```

### **⚡ Atajos Útiles**

```powershell
# Limpiar pantalla
Clear-Host
cls

# Cancelar comando en ejecución
# Presionar: Ctrl + C

# Ver historial de comandos
Get-History
history

# Repetir último comando
# Presionar: ↑ (flecha arriba)

# Auto-completar (TAB)
.\scr[TAB]  # Completa a .\scripts\
.\scripts\ru[TAB]  # Completa a .\scripts\run-test.ps1

# Buscar en historial
# Presionar: Ctrl + R
# Escribir parte del comando
```

### **🆘 Comandos de Emergencia**

```powershell
# Tests colgados - Forzar cierre
taskkill /F /IM java.exe
taskkill /F /IM chromedriver.exe

# Limpiar build
.\gradlew.bat clean

# Ver procesos Java
Get-Process | Where-Object {$_.Name -like "*java*"}

# Ver puertos en uso
netstat -ano | findstr :8080

# Eliminar archivos temporales
Remove-Item -Recurse -Force build\
Remove-Item -Recurse -Force .gradle\
```

### **📋 Archivos Wrapper para Doble Click**

Crear estos archivos en la raíz del módulo para ejecución con doble click:

**`ejecutar-tests-local.bat`:**
```batch
@echo off
echo.
echo ════════════════════════════════════════
echo   Ejecutando Tests en Ambiente LOCAL
echo ════════════════════════════════════════
echo.
powershell.exe -ExecutionPolicy Bypass -NoExit -File "%~dp0scripts\run-test.ps1" -Env local
```

**`ejecutar-tests-qa.bat`:**
```batch
@echo off
echo.
echo ════════════════════════════════════════
echo   Ejecutando Tests en Ambiente QA
echo ════════════════════════════════════════
echo.
powershell.exe -ExecutionPolicy Bypass -NoExit -File "%~dp0scripts\run-test.ps1" -Env qa
```

**`ejecutar-smoke-tests.bat`:**
```batch
@echo off
echo.
echo ════════════════════════════════════════
echo   Ejecutando Smoke Tests
echo ════════════════════════════════════════
echo.
powershell.exe -ExecutionPolicy Bypass -NoExit -File "%~dp0scripts\run-test.ps1" -Tags "@smoke"
```

**`actualizar-scripts.bat`:**
```batch
@echo off
echo.
echo ════════════════════════════════════════
echo   Actualizando Scripts desde Framework
echo ════════════════════════════════════════
echo.
powershell.exe -ExecutionPolicy Bypass -NoExit -File "%~dp0scripts\sync-utils.ps1"
pause
```

**`abrir-reportes.bat`:**
```batch
@echo off
start "" "build\reports\cucumber\cucumber-html-report.html"
start "" "build\reports\tests\test\index.html"
```

Ahora simplemente **doble click** en el `.bat` que necesites! 🎯

### **🔗 Links Útiles**

- **PowerShell Cheat Sheet:** https://ss64.com/ps/
- **Gradle Docs:** https://docs.gradle.org/current/userguide/userguide.html
- **Cucumber Docs:** https://cucumber.io/docs/cucumber/
- **Selenium WebDriver:** https://www.selenium.dev/documentation/

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

**Última actualización**: 5 de Diciembre de 2025  
**Autor**: Abel Venero  
**Versión**: 1.0.0  
**Mantenido por**: Equipo Scotia QA

