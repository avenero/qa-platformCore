#!/bin/bash

# ============================================================================
# sync-utils.sh - Sincronizar Scripts desde JAR de common
# ============================================================================
#
# Este script extrae utils.sh y utils.ps1 desde el JAR de common publicado
# en Maven local. Solo actualiza archivos CORE (utils.*), nunca toca archivos
# custom del módulo (run-tests.sh, etc.)
#
# Uso:
#   ./scripts/sync-utils.sh           # Sincronizar con última versión
#   ./scripts/sync-utils.sh --version 1.0.1  # Sincronizar con versión específica
#
# @author Abel Venero
# @version 1.0.0
# ============================================================================

set -e  # Terminar en error

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BLUE='\033[0;34m'
NC='\033[0m'

# ============================================================================
# FUNCIONES DE LOGGING
# ============================================================================

log_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

log_error() {
    echo -e "${RED}✗ $1${NC}" >&2
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

log_banner() {
    echo ""
    echo -e "${BLUE}════════════════════════════════════════${NC}"
    echo -e "${BLUE}  🔄 $1${NC}"
    echo -e "${BLUE}════════════════════════════════════════${NC}"
    echo ""
}

# ============================================================================
# FUNCIONES PRINCIPALES
# ============================================================================

# Buscar JAR de common en Maven local
find_common_jar() {
    local version=$1
    local maven_repo="${HOME}/.m2/repository/com/scotia/qa/common"

    if [[ ! -d "${maven_repo}" ]]; then
        log_error "Repositorio Maven local no encontrado: ${maven_repo}"
        log_info "Ejecuta primero: ./gradlew :common:publishToMavenLocal (en el framework)"
        return 1
    fi

    # Si se especificó versión, buscar esa versión específica
    if [[ -n "${version}" ]]; then
        local jar_path="${maven_repo}/${version}/common-${version}.jar"
        if [[ -f "${jar_path}" ]]; then
            echo "${jar_path}"
            return 0
        else
            log_error "JAR no encontrado: ${jar_path}"
            return 1
        fi
    fi

    # Buscar última versión (ordenar por fecha de modificación)
    local jar_path=$(find "${maven_repo}" -name "common-*.jar" -type f ! -name "*-sources.jar" ! -name "*-javadoc.jar" | \
                     head -1 | sort -t- -k2 -V | tail -1)

    if [[ -z "${jar_path}" ]]; then
        log_error "No se encontró ningún JAR de common en Maven local"
        log_info "Ejecuta: ./gradlew :common:publishToMavenLocal (en el framework)"
        return 1
    fi

    echo "${jar_path}"
    return 0
}

# Extraer scripts del JAR
extract_scripts_from_jar() {
    local jar_path=$1
    local temp_dir=$(mktemp -d)

    log_info "Extrayendo scripts desde: $(basename ${jar_path})"

    # Verificar que el JAR contiene los scripts
    if ! unzip -l "${jar_path}" 2>/dev/null | grep -q "META-INF/scripts/utils.sh"; then
        log_error "El JAR no contiene scripts en META-INF/scripts/"
        log_warning "Puede ser una versión antigua de common sin soporte cross-platform"
        rm -rf "${temp_dir}"
        return 1
    fi

    # Extraer SOLO utils.sh y utils.ps1
    unzip -q -o "${jar_path}" "META-INF/scripts/utils.sh" "META-INF/scripts/utils.ps1" -d "${temp_dir}" 2>/dev/null || true

    # Verificar extracción exitosa
    if [[ ! -f "${temp_dir}/META-INF/scripts/utils.sh" ]]; then
        log_error "Falló la extracción de utils.sh"
        rm -rf "${temp_dir}"
        return 1
    fi

    # Crear directorio scripts/ si no existe
    mkdir -p scripts

    # Copiar scripts (sobrescribir)
    cp "${temp_dir}/META-INF/scripts/utils.sh" scripts/utils.sh
    log_success "utils.sh actualizado"

    if [[ -f "${temp_dir}/META-INF/scripts/utils.ps1" ]]; then
        cp "${temp_dir}/META-INF/scripts/utils.ps1" scripts/utils.ps1
        log_success "utils.ps1 actualizado"
    fi

    # Hacer ejecutable utils.sh
    chmod +x scripts/utils.sh

    # Limpiar temporal
    rm -rf "${temp_dir}"

    return 0
}

# Mostrar información del JAR
show_jar_info() {
    local jar_path=$1
    local jar_name=$(basename "${jar_path}")
    local version=$(echo "${jar_name}" | sed 's/common-\(.*\)\.jar/\1/')
    local jar_date=$(date -r "${jar_path}" "+%Y-%m-%d %H:%M:%S" 2>/dev/null || stat -f "%Sm" "${jar_path}")

    echo ""
    log_info "JAR encontrado: ${jar_name}"
    log_info "Versión: ${version}"
    log_info "Fecha: ${jar_date}"
    echo ""
}

# Mostrar uso
show_usage() {
    echo "Uso: $0 [opciones]"
    echo ""
    echo "Opciones:"
    echo "  --version VERSION    Sincronizar con versión específica (ej: 1.0.1)"
    echo "  --help              Mostrar esta ayuda"
    echo ""
    echo "Ejemplos:"
    echo "  $0                  # Sincronizar con última versión"
    echo "  $0 --version 1.0.1  # Sincronizar con versión 1.0.1"
}

# ============================================================================
# MAIN
# ============================================================================

main() {
    local version=""

    # Parsear argumentos
    while [[ $# -gt 0 ]]; do
        case $1 in
            --version)
                version="$2"
                shift 2
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

    log_banner "Sincronizar Scripts desde common"

    # Buscar JAR
    local jar_path
    if ! jar_path=$(find_common_jar "${version}"); then
        exit 1
    fi

    # Mostrar información
    show_jar_info "${jar_path}"

    # Extraer scripts
    if ! extract_scripts_from_jar "${jar_path}"; then
        exit 1
    fi

    echo ""
    log_success "Scripts sincronizados exitosamente"
    log_info "Archivos actualizados:"
    echo "  • scripts/utils.sh"
    echo "  • scripts/utils.ps1"
    echo ""
    log_info "Nota: Solo se actualizan utils.* (archivos CORE)"
    log_info "      Los archivos custom del módulo no se tocan"
    echo ""
}

# Ejecutar main
main "$@"

