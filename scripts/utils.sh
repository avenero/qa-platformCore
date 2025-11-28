#!/bin/bash

# ============================================================================
# Utilidades Compartidas para Scripts del Framework
# ============================================================================
#
# Este archivo contiene funciones compartidas utilizadas por todos los scripts
# del framework Scotia QA.
#
# @author Abel Venero
# @version 1.0.0
# ============================================================================

# Versión de los scripts
SCRIPT_VERSION="1.0.0"


# ============================================================================
# COLORES Y FORMATO
# ============================================================================

# Colores para output
export GREEN='\033[0;32m'
export BLUE='\033[0;34m'
export YELLOW='\033[1;33m'
export RED='\033[0;31m'
export CYAN='\033[0;36m'
export MAGENTA='\033[0;35m'
export BOLD='\033[1m'
export NC='\033[0m' # No Color

# ============================================================================
# FUNCIONES DE LOGGING
# ============================================================================

# Imprimir mensaje de éxito
# Uso: log_success "Mensaje de éxito"
log_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Imprimir mensaje de error
# Uso: log_error "Mensaje de error"
log_error() {
    echo -e "${RED}✗ $1${NC}" >&2
}

# Imprimir mensaje de advertencia
# Uso: log_warning "Mensaje de advertencia"
log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Imprimir mensaje de información
# Uso: log_info "Mensaje informativo"
log_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

# Imprimir banner/título
# Uso: log_banner "Título del Banner"
log_banner() {
    echo -e "${BLUE}════════════════════════════════════════${NC}"
    echo -e "${BLUE}  🚀 $1${NC}"
    echo -e "${BLUE}════════════════════════════════════════${NC}"
    echo ""
}

# Imprimir separador
log_separator() {
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# ============================================================================
# DETECCIÓN DE ENTORNO
# ============================================================================

# Detectar el sistema operativo
# Retorna: "macOS", "Linux", "Windows", o "Unknown"
detect_os() {
    case "$(uname -s)" in
        Darwin*)
            echo "macOS"
            ;;
        Linux*)
            echo "Linux"
            ;;
        MINGW*|MSYS*|CYGWIN*)
            echo "Windows"
            ;;
        *)
            echo "Unknown"
            ;;
    esac
}

# Detectar si estamos en Jenkins
# Retorna: 0 si es Jenkins, 1 si no
is_jenkins() {
    [[ -n "${JENKINS_HOME}" ]] || [[ -n "${JENKINS_URL}" ]]
}

# Detectar si estamos en CI/CD
# Retorna: 0 si es CI/CD, 1 si no
is_ci() {
    [[ -n "${CI}" ]] || [[ -n "${CONTINUOUS_INTEGRATION}" ]] || is_jenkins
}

# ============================================================================
# DETECCIÓN DE MÓDULO
# ============================================================================

# Auto-detectar el nombre del módulo desde múltiples fuentes
# Prioridad: 1) Parámetro, 2) Variable entorno, 3) gradle.properties, 4) Directorio
# Uso: MODULE_NAME=$(detect_module_name)
detect_module_name() {
    local module_name=""

    # 1. Desde variable de entorno
    if [[ -n "${MODULE_NAME}" ]]; then
        module_name="${MODULE_NAME}"
        log_info "Módulo detectado desde variable de entorno: ${module_name}"
        echo "${module_name}"
        return 0
    fi

    # 2. Desde gradle.properties
    if [[ -f "gradle.properties" ]]; then
        module_name=$(grep "^rootProject.name" gradle.properties 2>/dev/null | cut -d'=' -f2 | tr -d ' ')
        if [[ -n "${module_name}" ]]; then
            log_info "Módulo detectado desde gradle.properties: ${module_name}"
            echo "${module_name}"
            return 0
        fi
    fi

    # 3. Desde settings.gradle
    if [[ -f "settings.gradle" ]]; then
        module_name=$(grep "rootProject.name" settings.gradle 2>/dev/null | sed "s/.*['\"]\\([^'\"]*\\)['\"].*/\\1/")
        if [[ -n "${module_name}" ]]; then
            log_info "Módulo detectado desde settings.gradle: ${module_name}"
            echo "${module_name}"
            return 0
        fi
    fi

    # 4. Desde directorio actual
    module_name=$(basename "$(pwd)")
    log_info "Módulo detectado desde directorio actual: ${module_name}"
    echo "${module_name}"
}

# ============================================================================
# BÚSQUEDA DE ARCHIVOS DE CONFIGURACIÓN
# ============================================================================

# Buscar archivo de configuración .env
# Prioridad: .env.local > .env.${TEST_ENV} > .env
# Retorna: ruta del archivo encontrado o cadena vacía
find_env_file() {
    local env_file=""
    local test_env="${TEST_ENV:-local}"

    # 1. Buscar .env.local (máxima prioridad)
    if [[ -f ".env.local" ]]; then
        env_file=".env.local"
        log_info "Archivo de configuración encontrado: ${env_file}"
        echo "${env_file}"
        return 0
    fi

    # 2. Buscar .env.${TEST_ENV}
    if [[ -f ".env.${test_env}" ]]; then
        env_file=".env.${test_env}"
        log_info "Archivo de configuración encontrado: ${env_file}"
        echo "${env_file}"
        return 0
    fi

    # 3. Buscar .env genérico
    if [[ -f ".env" ]]; then
        env_file=".env"
        log_info "Archivo de configuración encontrado: ${env_file}"
        echo "${env_file}"
        return 0
    fi

    # No se encontró archivo
    log_warning "No se encontró archivo de configuración .env"
    echo ""
    return 1
}

# ============================================================================
# CARGA DE VARIABLES DE ENTORNO
# ============================================================================

# Cargar variables desde archivo .env
# Uso: load_env_file ".env.local"
load_env_file() {
    local env_file="$1"

    if [[ ! -f "${env_file}" ]]; then
        log_error "Archivo no encontrado: ${env_file}"
        return 1
    fi

    log_info "Cargando variables desde: ${env_file}"

    # Cargar el archivo usando source (más seguro que export manual)
    set -a  # Exportar todas las variables
    source "${env_file}"
    set +a  # Deshabilitar auto-export

    log_success "Variables cargadas exitosamente"
}

# ============================================================================
# VALIDACIÓN DE VARIABLES
# ============================================================================

# Validar que variables requeridas estén definidas
# Uso: validate_required_vars "DB_URL" "DB_USER" "DB_PASS"
# Retorna: 0 si todas están definidas, 1 si falta alguna
validate_required_vars() {
    local missing_vars=()
    local all_valid=true

    for var in "$@"; do
        if [[ -z "${!var}" ]]; then
            missing_vars+=("${var}")
            all_valid=false
            log_error "${var} no está configurada"
        else
            log_success "${var} configurada"
        fi
    done

    if [[ "${all_valid}" == false ]]; then
        echo ""
        log_error "Faltan variables requeridas: ${missing_vars[*]}"
        return 1
    fi

    return 0
}

# ============================================================================
# CONSTRUCCIÓN DE COMANDOS GRADLE
# ============================================================================

# Construir argumentos de Gradle desde variables de entorno
# Retorna: string con todos los argumentos -D para Gradle
build_gradle_properties() {
    local props=()

    # Lista de variables estándar del framework
    local vars=(
        "DB_URL"
        "DB_USER"
        "DB_PASS"
        "DB_DRIVER"
        "TEST_ENV"
        "API_BASE_URL"
        "API_TOKEN"
        "WEB_BASE_URL"
        "APP_PATH"
        "BROWSER"
        "HEADLESS"
        "PLATFORM"
    )

    # Construir propiedades para cada variable definida
    for var in "${vars[@]}"; do
        if [[ -n "${!var}" ]]; then
            props+=("-D${var}=${!var}")
        fi
    done

    # Retornar como string separado por espacios
    echo "${props[@]}"
}

# ============================================================================
# UTILIDADES DE ARCHIVOS
# ============================================================================

# Verificar si existe Gradle Wrapper
# Retorna: 0 si existe, 1 si no existe
has_gradle_wrapper() {
    [[ -f "gradlew" ]]
}

# Obtener comando Gradle apropiado (wrapper o instalación global)
get_gradle_command() {
    if has_gradle_wrapper; then
        echo "./gradlew"
    else
        echo "gradle"
    fi
}

# ============================================================================
# NORMALIZACIÓN DE LÍNEAS
# ============================================================================

# Normalizar line endings en archivo (útil para Windows)
normalize_line_endings() {
    local file="$1"

    if [[ ! -f "${file}" ]]; then
        return 1
    fi

    # Detectar si tenemos dos2unix
    if command -v dos2unix &> /dev/null; then
        dos2unix "${file}" 2>/dev/null
    elif command -v sed &> /dev/null; then
        # Fallback usando sed
        sed -i.bak 's/\r$//' "${file}" && rm -f "${file}.bak"
    fi
}

# ============================================================================
# MANEJO DE ERRORES
# ============================================================================

# Función de cleanup al salir (se ejecuta en EXIT trap)
cleanup_on_exit() {
    local exit_code=$?

    if [[ ${exit_code} -ne 0 ]]; then
        echo ""
        log_error "Script terminó con código de error: ${exit_code}"
    fi
}

# Configurar trap para cleanup
trap cleanup_on_exit EXIT

# ============================================================================
# VALIDACIÓN DE DEPENDENCIAS
# ============================================================================

# Verificar que un comando existe
# Uso: check_command "java" "Java JDK"
check_command() {
    local cmd="$1"
    local name="$2"

    if ! command -v "${cmd}" &> /dev/null; then
        log_error "${name} no está instalado (comando: ${cmd})"
        return 1
    fi

    log_success "${name} encontrado"
    return 0
}

# Verificar dependencias del framework
check_framework_dependencies() {
    local all_ok=true

    log_info "Verificando dependencias..."
    echo ""

    if ! check_command "java" "Java JDK"; then
        all_ok=false
    fi

    if ! has_gradle_wrapper && ! check_command "gradle" "Gradle"; then
        all_ok=false
    fi

    echo ""

    if [[ "${all_ok}" == false ]]; then
        log_error "Faltan dependencias requeridas"
        return 1
    fi

    log_success "Todas las dependencias están instaladas"
    return 0
}

# ============================================================================
# FUNCIONES DE AYUDA
# ============================================================================

# Mostrar versión de Java
show_java_version() {
    java -version 2>&1 | head -n 1
}

# Mostrar versión de Gradle
show_gradle_version() {
    $(get_gradle_command) --version 2>&1 | grep "Gradle" | head -n 1
}

# ============================================================================
# EXPORTAR FUNCIONES PARA USO EN OTROS SCRIPTS
# ============================================================================

export -f log_success
export -f log_error
export -f log_warning
export -f log_info
export -f log_banner
export -f log_separator
export -f detect_os
export -f is_jenkins
export -f is_ci
export -f detect_module_name
export -f find_env_file
export -f load_env_file
export -f validate_required_vars
export -f build_gradle_properties
export -f has_gradle_wrapper
export -f get_gradle_command
export -f check_command
export -f check_framework_dependencies

