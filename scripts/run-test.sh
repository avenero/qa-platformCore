#!/bin/bash

# ============================================================================
# Scotia QA Framework - Script de Testing Genérico
# VERSION: 1.0.0
# ============================================================================
#
# Script unificado para configuración y ejecución de tests.
# Auto-detecta el módulo, configuración y ejecuta tests con Gradle.
#
# CARACTERÍSTICAS:
#   ✅ Auto-detección del módulo actual
#   ✅ Búsqueda automática de archivos .env
#   ✅ Soporte para variables de entorno (Jenkins/CI-CD)
#   ✅ Modo interactivo para configuración
#   ✅ Sin hardcodeo de nombres o rutas
#
# USO:
#   ./run-test.sh                          # Auto-detecta y ejecuta tests
#   ./run-test.sh --setup                  # Modo configuración interactiva
#   ./run-test.sh --env qa                 # Usar ambiente específico
#   ./run-test.sh --tags @smoke            # Ejecutar tags específicos
#   ./run-test.sh clean test --info        # Comando Gradle personalizado
#
# JENKINS/CI-CD:
#   TEST_ENV=qa DB_URL=... ./run-test.sh  # Usa variables de entorno
#
# @author Abel Venero
# @version 1.0.0
# ============================================================================

set -e  # Salir si hay algún error

# ============================================================================
# CARGAR UTILIDADES
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/utils.sh"

# Verificar actualizaciones de scripts (no bloqueante)
check_script_updates

# ============================================================================
# VARIABLES GLOBALES
# ============================================================================

SETUP_MODE=false
ENV_FILE=""
MODULE_NAME=""
GRADLE_ARGS="clean test"
TEST_ENV="${TEST_ENV:-local}"

# ============================================================================
# FUNCIONES PRINCIPALES
# ============================================================================

# Mostrar ayuda del script
show_help() {
    cat << EOF
${BOLD}Scotia QA Framework - Test Runner${NC}

${BOLD}USO:${NC}
    ./test.sh [OPCIONES] [COMANDOS_GRADLE]

${BOLD}OPCIONES:${NC}
    -h, --help              Mostrar esta ayuda
    -s, --setup             Modo configuración interactiva
    -e, --env ENV           Usar ambiente específico (qa, uat, prod)
    -t, --tags TAGS         Ejecutar tags específicos de Cucumber
    -m, --module NAME       Especificar nombre del módulo
    -f, --env-file FILE     Usar archivo .env específico
    -v, --verbose           Modo verbose (Gradle --info)
    --dry-run               Mostrar comandos sin ejecutar

${BOLD}EJEMPLOS:${NC}
    ${CYAN}# Configuración inicial (interactiva)${NC}
    ./test.sh --setup

    ${CYAN}# Ejecución simple (auto-detección)${NC}
    ./test.sh

    ${CYAN}# Usar ambiente QA${NC}
    ./test.sh --env qa

    ${CYAN}# Ejecutar solo tests con tag @smoke${NC}
    ./test.sh --tags @smoke

    ${CYAN}# Comando Gradle personalizado${NC}
    ./test.sh clean test --info

    ${CYAN}# Desde Jenkins (usando variables de entorno)${NC}
    TEST_ENV=qa DB_URL=jdbc:... ./test.sh

${BOLD}VARIABLES DE ENTORNO SOPORTADAS:${NC}
    MODULE_NAME             Nombre del módulo
    TEST_ENV                Ambiente (qa, uat, prod)
    DB_URL                  URL de base de datos
    DB_USER                 Usuario de BD
    DB_PASS                 Password de BD
    API_BASE_URL            URL base de API
    WEB_BASE_URL            URL base de aplicación web

${BOLD}ORDEN DE PRIORIDAD (mayor a menor):${NC}
    1. Argumentos CLI (--env, --tags, etc.)
    2. Variables de entorno (export VAR=value)
    3. Archivo .env.local
    4. Archivo .env.\${TEST_ENV}
    5. Archivo .env

${BOLD}MÁS INFORMACIÓN:${NC}
    Documentación: ../doc/README.md
    Guía rápida: ../README.md

EOF
}

# Modo configuración interactiva
run_setup_mode() {
    log_banner "Configuración Interactiva"

    echo -e "${CYAN}Este asistente te ayudará a configurar las variables de entorno.${NC}"
    echo ""

    # Detectar módulo
    MODULE_NAME=$(detect_module_name)
    log_info "Módulo detectado: ${MODULE_NAME}"
    echo ""

    # Preguntar por ambiente
    echo -e "${CYAN}¿Qué ambiente deseas configurar?${NC}"
    echo "  1) Local (desarrollo)"
    echo "  2) QA"
    echo "  3) UAT"
    echo "  4) PROD"
    read -p "Opción [1]: " env_choice

    case ${env_choice:-1} in
        1) TEST_ENV="local" ;;
        2) TEST_ENV="qa" ;;
        3) TEST_ENV="uat" ;;
        4) TEST_ENV="prod" ;;
        *) TEST_ENV="local" ;;
    esac

    ENV_FILE=".env.${TEST_ENV}"

    echo ""
    log_info "Configurando ambiente: ${TEST_ENV}"
    log_info "Archivo: ${ENV_FILE}"
    echo ""

    # Configurar variables de BD
    log_separator
    echo -e "${BOLD}Configuración de Base de Datos${NC}"
    log_separator
    echo ""

    read -p "DB URL [jdbc:oracle:thin:@//host:port/service]: " db_url
    read -p "DB User: " db_user
    read -s -p "DB Password: " db_pass
    echo ""

    # Configurar variables de API (opcional)
    echo ""
    log_separator
    echo -e "${BOLD}Configuración de API (opcional)${NC}"
    log_separator
    echo ""

    read -p "API Base URL [Enter para omitir]: " api_url

    # Crear archivo .env
    cat > "${ENV_FILE}" << EOF
# ============================================================================
# Configuración de Entorno - ${MODULE_NAME}
# ============================================================================
# Ambiente: ${TEST_ENV}
# Generado: $(date)
# ============================================================================

# ====================================================================
# AMBIENTE
# ====================================================================
TEST_ENV=${TEST_ENV}

# ====================================================================
# BASE DE DATOS
# ====================================================================
DB_URL=${db_url}
DB_USER=${db_user}
DB_PASS=${db_pass}
DB_DRIVER=oracle.jdbc.OracleDriver

EOF

    # Agregar API si se configuró
    if [[ -n "${api_url}" ]]; then
        cat >> "${ENV_FILE}" << EOF
# ====================================================================
# API
# ====================================================================
API_BASE_URL=${api_url}

EOF
    fi

    # Agregar instrucciones
    cat >> "${ENV_FILE}" << EOF
# ====================================================================
# INSTRUCCIONES
# ====================================================================
# 1. NO commitear este archivo (debe estar en .gitignore)
# 2. Para usar: source ${ENV_FILE}
# 3. Para tests: ./test.sh
# ====================================================================
EOF

    echo ""
    log_success "Archivo ${ENV_FILE} creado exitosamente"
    echo ""

    # Preguntar si ejecutar tests ahora
    read -p "¿Deseas ejecutar los tests ahora? (s/N): " run_now

    if [[ "${run_now}" =~ ^[Ss]$ ]]; then
        echo ""
        log_info "Ejecutando tests..."
        echo ""
        SETUP_MODE=false
        # Continuar con la ejecución normal
    else
        echo ""
        log_info "Para ejecutar tests más tarde, usa: ./test.sh"
        exit 0
    fi
}

# Procesar argumentos de línea de comandos
parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -s|--setup)
                SETUP_MODE=true
                shift
                ;;
            -e|--env)
                TEST_ENV="$2"
                shift 2
                ;;
            -t|--tags)
                GRADLE_ARGS="${GRADLE_ARGS} -Dcucumber.filter.tags=\"$2\""
                shift 2
                ;;
            -m|--module)
                MODULE_NAME="$2"
                shift 2
                ;;
            -f|--env-file)
                ENV_FILE="$2"
                shift 2
                ;;
            -v|--verbose)
                GRADLE_ARGS="${GRADLE_ARGS} --info"
                shift
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            *)
                # Cualquier otro argumento se pasa directamente a Gradle
                GRADLE_ARGS="$@"
                break
                ;;
        esac
    done
}

# Ejecutar tests con Gradle
run_tests() {
    local gradle_cmd=$(get_gradle_command)
    local gradle_props=$(build_gradle_properties)

    log_separator
    echo -e "${CYAN}Comando a ejecutar:${NC}"
    echo ""

    # Construir comando completo
    local full_command="${gradle_cmd} ${GRADLE_ARGS} ${gradle_props}"

    # Mostrar comando (ocultando valores sensibles)
    local safe_command=$(echo "${full_command}" | \
        sed 's/-DDB_PASS=[^ ]*/-DDB_PASS=***HIDDEN***/g' | \
        sed 's/-DAPI_TOKEN=[^ ]*/-DAPI_TOKEN=***HIDDEN***/g')

    echo -e "${YELLOW}${safe_command}${NC}"
    echo ""
    log_separator
    echo ""

    # Ejecutar o simular (dry-run)
    if [[ "${DRY_RUN}" == true ]]; then
        log_warning "Modo DRY-RUN: No se ejecutará el comando"
        return 0
    fi

    log_success "🚀 Ejecutando tests..."
    echo ""

    # Ejecutar comando
    eval "${full_command}"

    local exit_code=$?

    echo ""
    log_separator

    if [[ ${exit_code} -eq 0 ]]; then
        log_success "Tests ejecutados exitosamente"
    else
        log_error "Tests fallaron con código: ${exit_code}"
        exit ${exit_code}
    fi
}

# ============================================================================
# FLUJO PRINCIPAL
# ============================================================================

main() {
    # Banner inicial
    log_banner "Scotia QA Framework - Test Runner"

    # Mostrar información del sistema
    log_info "Sistema: $(detect_os)"
    if is_ci; then
        log_info "Entorno: CI/CD"
    else
        log_info "Entorno: Local"
    fi
    echo ""

    # Procesar argumentos
    parse_arguments "$@"

    # Modo setup interactivo
    if [[ "${SETUP_MODE}" == true ]]; then
        run_setup_mode
    fi

    # Auto-detectar módulo si no se especificó
    if [[ -z "${MODULE_NAME}" ]]; then
        MODULE_NAME=$(detect_module_name)
    fi

    log_info "Módulo: ${MODULE_NAME}"
    log_info "Ambiente: ${TEST_ENV}"
    echo ""

    # Verificar dependencias
    if ! check_framework_dependencies; then
        exit 1
    fi

    echo ""

    # Buscar archivo de configuración si no se especificó
    if [[ -z "${ENV_FILE}" ]]; then
        ENV_FILE=$(find_env_file)
    fi

    # Cargar archivo de configuración si existe
    if [[ -n "${ENV_FILE}" ]] && [[ -f "${ENV_FILE}" ]]; then
        load_env_file "${ENV_FILE}"
        echo ""
    elif ! is_ci; then
        log_warning "No se encontró archivo de configuración"
        log_info "Usa --setup para configuración interactiva"
        log_info "O configura variables de entorno manualmente"
        echo ""
    fi

    # Validar variables requeridas (solo para tests de BD)
    if [[ "${GRADLE_ARGS}" =~ "test" ]]; then
        log_info "Validando variables requeridas..."
        echo ""

        # Variables mínimas para tests
        local required_vars=()

        # Detectar qué tipo de tests se ejecutarán
        if [[ -d "src/test" ]]; then
            # Si hay código Java, asumir que puede necesitar BD
            required_vars+=("DB_URL" "DB_USER" "DB_PASS")
        fi

        if [[ ${#required_vars[@]} -gt 0 ]]; then
            if ! validate_required_vars "${required_vars[@]}"; then
                echo ""
                log_error "Configura las variables faltantes antes de continuar"
                log_info "Usa: ./test.sh --setup"
                exit 1
            fi
        fi

        echo ""
    fi

    # Ejecutar tests
    run_tests
}

# ============================================================================
# EJECUCIÓN
# ============================================================================

main "$@"

