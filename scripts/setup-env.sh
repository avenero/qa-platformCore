#!/usr/bin/env bash
# ============================================================================
# Scotia QA Framework - Configuración de Variables de Entorno
# VERSION: 1.0.0
# ============================================================================
#
# Script para cargar variables de entorno desde archivo .env.local
# y exportarlas al shell actual para ejecución de tests.
#
# CARACTERÍSTICAS:
#   ✅ Carga automática de .env.local
#   ✅ Validación de variables críticas
#   ✅ Verificación interactiva
#   ✅ Compatible con bash/zsh
#
# USO:
#   source ./scripts/setup-env.sh              # Cargar en shell actual
#   . ./scripts/setup-env.sh                   # Alternativa
#
# LUEGO EJECUTAR TESTS:
#   ./gradlew test                             # Desde terminal
#   O ejecutar desde IntelliJ (variables ya están en el shell)
#
# IMPORTANTE:
#   ⚠️  Debes usar 'source' o '.' para que las variables se exporten al shell actual
#   ⚠️  NO ejecutar con ./setup-env.sh (las variables no se exportarán)
#
# @author Abel Venero
# @version 1.0.0
# ============================================================================

set -e  # Exit on error

# ============================================================================
# CONFIGURACIÓN
# ============================================================================

ENV_FILE=".env.local"
SCRIPT_VERSION="1.0.0"

# Colores
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m' # No Color
readonly BOLD='\033[1m'

# Variables críticas (obligatorias para DB)
REQUIRED_DB_VARS=(
    "DB_URL"
    "DB_USER"
    "DB_PASS"
)

# ============================================================================
# FUNCIONES DE LOGGING
# ============================================================================

log_info() {
    echo -e "${BLUE}ℹ${NC}  $1"
}

log_success() {
    echo -e "${GREEN}✓${NC}  $1"
}

log_warning() {
    echo -e "${YELLOW}⚠${NC}  $1"
}

log_error() {
    echo -e "${RED}✗${NC}  $1"
}

log_banner() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════${NC}"
    echo ""
}

log_separator() {
    echo -e "${CYAN}───────────────────────────────────────────${NC}"
}

# ============================================================================
# VALIDACIONES
# ============================================================================

check_sourced() {
    if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
        log_error "Este script debe ejecutarse con 'source' o '.'"
        echo ""
        echo "Uso correcto:"
        echo "  ${GREEN}source ./scripts/setup-env.sh${NC}"
        echo "  ${GREEN}. ./scripts/setup-env.sh${NC}"
        echo ""
        exit 1
    fi
}

check_env_file_exists() {
    if [[ ! -f "$ENV_FILE" ]]; then
        log_error "Archivo $ENV_FILE no encontrado"
        echo ""
        log_info "Crea el archivo copiando el template:"
        echo "  ${CYAN}cp config/templates/.env.local.template .env.local${NC}"
        echo ""
        log_info "Y edita con tus valores reales"
        return 1
    fi
    return 0
}

# ============================================================================
# CARGA DE VARIABLES
# ============================================================================

load_env_file() {
    local file=$1
    local count=0

    log_info "Cargando variables desde: ${BOLD}$file${NC}"
    echo ""

    # Leer archivo línea por línea
    while IFS= read -r line || [[ -n "$line" ]]; do
        # Ignorar líneas vacías y comentarios
        if [[ -z "$line" ]] || [[ "$line" =~ ^[[:space:]]*# ]]; then
            continue
        fi

        # Extraer nombre=valor
        if [[ "$line" =~ ^[[:space:]]*([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
            local var_name="${BASH_REMATCH[1]}"
            local var_value="${BASH_REMATCH[2]}"

            # Remover comillas si existen
            var_value="${var_value%\"}"
            var_value="${var_value#\"}"
            var_value="${var_value%\'}"
            var_value="${var_value#\'}"

            # Exportar variable
            export "$var_name=$var_value"
            ((count++))
        fi
    done < "$file"

    log_success "Variables cargadas: $count"
    return 0
}

# ============================================================================
# VERIFICACIÓN DE VARIABLES
# ============================================================================

verify_variables() {
    log_separator
    echo ""
    echo -e "${BOLD}Variables en .env.local:${NC}"
    echo ""

    local var_num=1
    local has_sensitive=false

    # Leer y mostrar variables (ocultar valores sensibles)
    while IFS= read -r line || [[ -n "$line" ]]; do
        if [[ -z "$line" ]] || [[ "$line" =~ ^[[:space:]]*# ]]; then
            continue
        fi

        if [[ "$line" =~ ^[[:space:]]*([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
            local var_name="${BASH_REMATCH[1]}"
            local var_value="${BASH_REMATCH[2]}"

            # Ocultar valores sensibles
            if [[ "$var_name" =~ (PASS|PASSWORD|TOKEN|SECRET|KEY) ]]; then
                var_value="***HIDDEN***"
                has_sensitive=true
            fi

            # Formatear valor (remover comillas)
            var_value="${var_value%\"}"
            var_value="${var_value#\"}"
            var_value="${var_value%\'}"
            var_value="${var_value#\'}"

            printf "   ${CYAN}%2d.${NC} ${BOLD}%-25s${NC} = %s\n" "$var_num" "$var_name" "$var_value"
            ((var_num++))
        fi
    done < "$ENV_FILE"

    echo ""
    log_separator
    echo ""
}

verify_environment() {
    echo -e "${BOLD}Estado en el entorno actual:${NC}"
    echo ""

    local all_set=true
    local checked_vars=()

    # Verificar variables del archivo
    while IFS= read -r line || [[ -n "$line" ]]; do
        if [[ -z "$line" ]] || [[ "$line" =~ ^[[:space:]]*# ]]; then
            continue
        fi

        if [[ "$line" =~ ^[[:space:]]*([A-Za-z_][A-Za-z0-9_]*)= ]]; then
            local var_name="${BASH_REMATCH[1]}"

            # Evitar duplicados
            if [[ " ${checked_vars[@]} " =~ " ${var_name} " ]]; then
                continue
            fi
            checked_vars+=("$var_name")

            if [[ -n "${!var_name}" ]]; then
                log_success "$var_name (cargada)"
            else
                log_warning "$var_name (vacía o no configurada)"
                all_set=false
            fi
        fi
    done < "$ENV_FILE"

    echo ""

    if $all_set; then
        log_success "Todas las variables están cargadas en el entorno actual"
    else
        log_warning "Algunas variables están vacías"
        log_info "Edita $ENV_FILE y vuelve a ejecutar este script"
    fi

    echo ""
}

# ============================================================================
# VERIFICACIÓN ESPECÍFICA DE BD
# ============================================================================

verify_database_config() {
    local missing_vars=()

    for var in "${REQUIRED_DB_VARS[@]}"; do
        if [[ -z "${!var}" ]]; then
            missing_vars+=("$var")
        fi
    done

    if [[ ${#missing_vars[@]} -gt 0 ]]; then
        log_separator
        echo ""
        log_warning "Configuración de Base de Datos incompleta"
        echo ""
        log_info "Variables faltantes:"
        for var in "${missing_vars[@]}"; do
            echo "   • $var"
        done
        echo ""
        log_info "Si vas a ejecutar tests de database (@db, @database), configura estas variables"
        echo ""
        return 1
    else
        log_success "Configuración de BD completa"
        return 0
    fi
}

# ============================================================================
# FUNCIÓN PRINCIPAL
# ============================================================================

main() {
    log_banner "🚀 Ejecutar Tests - qa-module-autos"

    # Validar que se ejecutó con source
    check_sourced

    # Verificar que existe .env.local
    if ! check_env_file_exists; then
        return 1
    fi

    # Cargar variables
    if ! load_env_file "$ENV_FILE"; then
        return 1
    fi

    echo ""

    # Mostrar variables cargadas
    verify_variables

    # Verificar estado en el entorno
    verify_environment

    # Verificar configuración de BD
    verify_database_config

    log_separator
    echo ""
    log_success "Variables de entorno configuradas exitosamente"
    echo ""
    log_info "Ahora puedes ejecutar tests:"
    echo ""
    echo "   ${GREEN}./gradlew test${NC}                    # Todos los tests"
    echo "   ${GREEN}./gradlew test --tests '*Smoke*'${NC}  # Tests específicos"
    echo ""
    log_info "O ejecutar desde IntelliJ (las variables ya están disponibles en este shell)"
    echo ""

    # Instrucciones adicionales
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${BOLD}📝 Notas:${NC}"
    echo "   • Las variables solo están disponibles en esta sesión de terminal"
    echo "   • Si cierras la terminal, deberás ejecutar este script nuevamente"
    echo "   • Para ejecutar en IntelliJ, abre IntelliJ desde esta misma terminal"
    echo ""
}

# ============================================================================
# EJECUCIÓN
# ============================================================================

main "$@"

