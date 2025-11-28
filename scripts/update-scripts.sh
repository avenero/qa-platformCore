#!/bin/bash

# ============================================================================
# Scotia QA Framework - Script de Actualización de Scripts
# ============================================================================
#
# Este script se copia al módulo y actualiza los scripts desde el framework.
#
# USO:
#   ./update-scripts.sh              # Actualizar con detección automática
#   ./update-scripts.sh --framework-path /path/to/framework
#   ./update-scripts.sh --check      # Solo verificar si hay actualizaciones
#   ./update-scripts.sh --force      # Forzar actualización sin confirmación
#
# @author Abel Venero
# @version 1.0.0
# ============================================================================

set -e

# ============================================================================
# COLORES
# ============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'
BOLD='\033[1m'

# ============================================================================
# FUNCIONES DE LOGGING
# ============================================================================

log_success() { echo -e "${GREEN}✓${NC} $1"; }
log_error() { echo -e "${RED}✗${NC} $1" >&2; }
log_warning() { echo -e "${YELLOW}⚠${NC} $1"; }
log_info() { echo -e "${CYAN}ℹ${NC} $1"; }

log_banner() {
    echo ""
    echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
    echo ""
}

# ============================================================================
# VARIABLES
# ============================================================================

FRAMEWORK_PATH=""
CHECK_ONLY=false
FORCE=false
MODULE_SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Scripts a actualizar
SCRIPTS_TO_UPDATE=(
    "run-test.sh"
    "utils.sh"
    "update-scripts.sh"
)

# ============================================================================
# DETECCIÓN DEL FRAMEWORK
# ============================================================================

detect_framework_location() {
    log_info "Buscando framework..."

    # 1. Variable de entorno
    if [[ -n "${QA_FRAMEWORK_HOME}" ]] && [[ -d "${QA_FRAMEWORK_HOME}/scripts" ]]; then
        FRAMEWORK_PATH="${QA_FRAMEWORK_HOME}"
        log_success "Framework encontrado vía QA_FRAMEWORK_HOME: ${FRAMEWORK_PATH}"
        return 0
    fi

    # 2. Submodule (dentro del módulo)
    local submodule_path="${MODULE_SCRIPTS_DIR}/../qa-scotia-frameworks"
    if [[ -d "$submodule_path/scripts" ]]; then
        FRAMEWORK_PATH="$(cd "$submodule_path" && pwd)"
        log_success "Framework encontrado como submodule: ${FRAMEWORK_PATH}"
        return 0
    fi

    # 3. Directorio hermano
    local sibling_path="${MODULE_SCRIPTS_DIR}/../../qa-scotia-frameworks"
    if [[ -d "$sibling_path/scripts" ]]; then
        FRAMEWORK_PATH="$(cd "$sibling_path" && pwd)"
        log_success "Framework encontrado como directorio hermano: ${FRAMEWORK_PATH}"
        return 0
    fi

    # 4. Preguntar al usuario
    log_warning "No se pudo detectar automáticamente la ubicación del framework"
    echo ""
    read -p "Ingresa la ruta del framework: " user_path

    if [[ -d "$user_path/scripts" ]]; then
        FRAMEWORK_PATH="$user_path"
        log_success "Framework encontrado: ${FRAMEWORK_PATH}"

        # Guardar para próxima vez
        echo "export QA_FRAMEWORK_HOME=\"${FRAMEWORK_PATH}\"" > "${MODULE_SCRIPTS_DIR}/.framework-location"
        log_info "Ubicación guardada en .framework-location"
        return 0
    else
        log_error "Directorio inválido: $user_path"
        return 1
    fi
}

# ============================================================================
# VERIFICACIÓN DE ACTUALIZACIONES
# ============================================================================

check_script_version() {
    local script_name="$1"
    local local_script="${MODULE_SCRIPTS_DIR}/${script_name}"
    local framework_script="${FRAMEWORK_PATH}/scripts/${script_name}"

    if [[ ! -f "$local_script" ]]; then
        echo "nuevo"
        return
    fi

    if [[ ! -f "$framework_script" ]]; then
        echo "no-existe"
        return
    fi

    # Comparar archivos
    if diff -q "$local_script" "$framework_script" > /dev/null 2>&1; then
        echo "igual"
    else
        echo "diferente"
    fi
}

show_updates_available() {
    log_banner "🔍 Verificando Actualizaciones"

    local updates_found=false

    for script in "${SCRIPTS_TO_UPDATE[@]}"; do
        local status=$(check_script_version "$script")

        case $status in
            "nuevo")
                echo -e "${GREEN}  📄 ${script}${NC} - Nuevo (será copiado)"
                updates_found=true
                ;;
            "diferente")
                echo -e "${YELLOW}  📝 ${script}${NC} - Actualización disponible"
                updates_found=true
                ;;
            "igual")
                echo -e "${GREEN}  ✓ ${script}${NC} - Actualizado"
                ;;
            "no-existe")
                echo -e "${RED}  ✗ ${script}${NC} - No existe en framework"
                ;;
        esac
    done

    echo ""

    if [[ "$updates_found" == "true" ]]; then
        log_warning "Hay actualizaciones disponibles"
        return 0
    else
        log_success "Todos los scripts están actualizados"
        return 1
    fi
}

# ============================================================================
# ACTUALIZACIÓN DE SCRIPTS
# ============================================================================

show_diff() {
    local script_name="$1"
    local local_script="${MODULE_SCRIPTS_DIR}/${script_name}"
    local framework_script="${FRAMEWORK_PATH}/scripts/${script_name}"

    echo ""
    echo -e "${BOLD}Cambios en ${script_name}:${NC}"
    echo "────────────────────────────────────────────────────────────"

    if [[ -f "$local_script" ]]; then
        diff -u "$local_script" "$framework_script" || true
    else
        echo "(Archivo nuevo)"
    fi

    echo ""
}

update_scripts() {
    log_banner "🔄 Actualizando Scripts"

    local updated_count=0

    for script in "${SCRIPTS_TO_UPDATE[@]}"; do
        local status=$(check_script_version "$script")

        if [[ "$status" == "igual" ]]; then
            continue
        fi

        local framework_script="${FRAMEWORK_PATH}/scripts/${script}"
        local local_script="${MODULE_SCRIPTS_DIR}/${script}"

        if [[ ! -f "$framework_script" ]]; then
            log_warning "Script no existe en framework, omitiendo: ${script}"
            continue
        fi

        # Mostrar diff si no es forzado
        if [[ "$FORCE" == "false" ]] && [[ -f "$local_script" ]]; then
            show_diff "$script"
            read -p "¿Actualizar ${script}? (S/n): " confirm
            confirm=${confirm:-S}

            if [[ ! "$confirm" =~ ^[sS]$ ]]; then
                log_info "Omitiendo: ${script}"
                continue
            fi
        fi

        # Crear backup
        if [[ -f "$local_script" ]]; then
            cp "$local_script" "${local_script}.backup"
            log_info "Backup creado: ${script}.backup"
        fi

        # Copiar script
        cp "$framework_script" "$local_script"
        chmod +x "$local_script"

        log_success "Actualizado: ${script}"
        ((updated_count++))
    done

    echo ""

    if [[ $updated_count -eq 0 ]]; then
        log_info "No se actualizó ningún script"
    else
        log_success "${updated_count} script(s) actualizados"
    fi
}

# ============================================================================
# PARSEADO DE ARGUMENTOS
# ============================================================================

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --framework-path)
                FRAMEWORK_PATH="$2"
                shift 2
                ;;
            --check)
                CHECK_ONLY=true
                shift
                ;;
            --force)
                FORCE=true
                shift
                ;;
            --help|-h)
                show_usage
                exit 0
                ;;
            *)
                log_error "Opción desconocida: $1"
                show_usage
                exit 1
                ;;
        esac
    done
}

show_usage() {
    cat << EOF
${BOLD}Scotia QA Framework - Actualizador de Scripts${NC}

${BOLD}USO:${NC}
    ./update-scripts.sh [opciones]

${BOLD}OPCIONES:${NC}
    --framework-path <path>   Ruta al framework
    --check                   Solo verificar actualizaciones
    --force                   Actualizar sin confirmación
    -h, --help                Mostrar esta ayuda

${BOLD}EJEMPLOS:${NC}
    # Actualizar (detecta framework automáticamente)
    ./update-scripts.sh

    # Solo verificar si hay actualizaciones
    ./update-scripts.sh --check

    # Especificar ubicación del framework
    ./update-scripts.sh --framework-path ~/projects/qa-scotia-frameworks

    # Actualizar sin preguntar
    ./update-scripts.sh --force

${BOLD}DETECCIÓN AUTOMÁTICA:${NC}
    El script busca el framework en:
    1. Variable de entorno: \$QA_FRAMEWORK_HOME
    2. Submodule: qa-scotia-frameworks/
    3. Directorio hermano: ../qa-scotia-frameworks/
    4. Pregunta al usuario

EOF
}

# ============================================================================
# MAIN
# ============================================================================

main() {
    log_banner "🔄 Actualizador de Scripts del Framework"

    parse_arguments "$@"

    # Detectar framework si no se especificó
    if [[ -z "$FRAMEWORK_PATH" ]]; then
        if ! detect_framework_location; then
            log_error "No se pudo encontrar el framework"
            exit 1
        fi
    fi

    # Verificar que el framework existe
    if [[ ! -d "${FRAMEWORK_PATH}/scripts" ]]; then
        log_error "Directorio de scripts no encontrado: ${FRAMEWORK_PATH}/scripts"
        exit 1
    fi

    # Verificar actualizaciones
    if show_updates_available; then
        if [[ "$CHECK_ONLY" == "true" ]]; then
            log_info "Ejecuta sin --check para actualizar"
            exit 0
        fi

        echo ""
        update_scripts
    fi

    echo ""
    log_success "✨ Proceso completado"
}

# Ejecutar
main "$@"

