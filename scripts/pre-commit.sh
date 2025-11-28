#!/bin/bash

# ============================================================================
# Scotia QA Framework - Pre-Commit Hook
# ============================================================================
# VERSION: 1.0.0
#
# Validaciones automáticas antes de hacer commit:
#   ✅ Ejecuta smoke tests (@smoke)
#   ✅ Valida formato de código (Spotless)
#   ✅ Detecta credenciales expuestas
#   ✅ Valida sintaxis Gherkin (.feature)
#   ✅ Verifica versiones SNAPSHOT
#   ✅ Valida que .env.local no se commitee
#
# INSTALACIÓN:
#   cp pre-commit.sh .git/hooks/pre-commit
#   chmod +x .git/hooks/pre-commit
#
# USO:
#   # Se ejecuta automáticamente al hacer commit
#   git commit -m "feat: nueva funcionalidad"
#
#   # Saltar validaciones (emergencia)
#   git commit --no-verify -m "fix: hotfix"
#
# @author Abel Venero
# @version 1.0.0
# ============================================================================

set -e

# ============================================================================
# CONFIGURACIÓN
# ============================================================================

# Configurar si queremos ejecutar tests
RUN_SMOKE_TESTS=true
CHECK_CODE_FORMAT=true
CHECK_CREDENTIALS=true
CHECK_GHERKIN_SYNTAX=true
CHECK_SNAPSHOT_VERSIONS=true
CHECK_ENV_FILES=true

# Colores
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

log_section() {
    echo ""
    echo -e "${BOLD}$1${NC}"
    echo "────────────────────────────────────────────────────────────"
}

# ============================================================================
# VALIDACIONES
# ============================================================================

# Ejecutar smoke tests
run_smoke_tests() {
    if [[ "$RUN_SMOKE_TESTS" != "true" ]]; then
        return 0
    fi

    log_section "🧪 Ejecutando Smoke Tests"

    # Verificar si existen tests smoke
    local smoke_tests=$(find src/test/resources/features -name "*.feature" -exec grep -l "@smoke" {} \; 2>/dev/null | wc -l)

    if [[ $smoke_tests -eq 0 ]]; then
        log_info "No hay tests @smoke, omitiendo"
        return 0
    fi

    log_info "Encontrados $smoke_tests features con @smoke"

    # Ejecutar solo tests smoke
    if ./gradlew test -Dcucumber.filter.tags="@smoke" --quiet 2>&1 | grep -q "BUILD SUCCESSFUL"; then
        log_success "Smoke tests pasaron"
        return 0
    else
        log_error "Smoke tests fallaron"
        return 1
    fi
}

# Validar formato de código
check_code_format() {
    if [[ "$CHECK_CODE_FORMAT" != "true" ]]; then
        return 0
    fi

    log_section "💅 Validando Formato de Código"

    # Verificar si Spotless está configurado
    if ! grep -q "spotless" build.gradle 2>/dev/null; then
        log_info "Spotless no configurado, omitiendo"
        return 0
    fi

    log_info "Verificando formato con Spotless..."

    if ./gradlew spotlessCheck --quiet 2>&1 | grep -q "BUILD SUCCESSFUL"; then
        log_success "Formato de código correcto"
        return 0
    else
        log_error "Formato de código incorrecto"
        log_info "Ejecuta: ./gradlew spotlessApply"
        return 1
    fi
}

# Detectar credenciales expuestas
check_credentials() {
    if [[ "$CHECK_CREDENTIALS" != "true" ]]; then
        return 0
    fi

    log_section "🔐 Detectando Credenciales Expuestas"

    # Patrones de búsqueda
    local patterns=(
        "password\s*=\s*['\"][^'\"]{3,}"
        "api[_-]?key\s*=\s*['\"][^'\"]{10,}"
        "secret\s*=\s*['\"][^'\"]{10,}"
        "token\s*=\s*['\"][^'\"]{10,}"
        "jdbc:.*://.*:.*@"
    )

    local issues_found=false

    # Obtener archivos staged
    local staged_files=$(git diff --cached --name-only --diff-filter=ACM)

    if [[ -z "$staged_files" ]]; then
        log_info "No hay archivos staged"
        return 0
    fi

    log_info "Analizando archivos staged..."

    for file in $staged_files; do
        # Ignorar archivos binarios y ciertos directorios
        if [[ "$file" =~ \.(jar|class|png|jpg|gif)$ ]] || \
           [[ "$file" =~ ^build/ ]] || \
           [[ "$file" =~ ^\.gradle/ ]]; then
            continue
        fi

        # Ignorar archivos de ejemplo/template
        if [[ "$file" =~ (example|template|sample) ]]; then
            continue
        fi

        # Buscar patrones sospechosos
        for pattern in "${patterns[@]}"; do
            if grep -qiE "$pattern" "$file" 2>/dev/null; then
                log_warning "Posible credencial en: $file"
                issues_found=true
            fi
        done
    done

    if [[ "$issues_found" == "true" ]]; then
        log_error "Credenciales potenciales detectadas"
        log_info "Revisa los archivos marcados antes de commitear"
        return 1
    else
        log_success "No se detectaron credenciales expuestas"
        return 0
    fi
}

# Validar sintaxis de archivos .feature
check_gherkin_syntax() {
    if [[ "$CHECK_GHERKIN_SYNTAX" != "true" ]]; then
        return 0
    fi

    log_section "📝 Validando Sintaxis Gherkin"

    # Obtener archivos .feature staged
    local feature_files=$(git diff --cached --name-only --diff-filter=ACM | grep "\.feature$" || true)

    if [[ -z "$feature_files" ]]; then
        log_info "No hay archivos .feature staged"
        return 0
    fi

    log_info "Validando archivos .feature..."

    local syntax_errors=false

    for file in $feature_files; do
        # Validaciones básicas
        if [[ ! -f "$file" ]]; then
            continue
        fi

        # Verificar que tenga Feature:
        if ! grep -q "^Feature:" "$file"; then
            log_error "Falta 'Feature:' en: $file"
            syntax_errors=true
        fi

        # Verificar que tenga al menos un Scenario
        if ! grep -qE "^(Scenario|Escenario):" "$file"; then
            log_warning "No tiene escenarios: $file"
        fi

        # Verificar estructura básica de steps
        if grep -qE "^(Given|When|Then|And|But|Dado|Cuando|Entonces|Y|Pero)\s*$" "$file"; then
            log_error "Steps vacíos en: $file"
            syntax_errors=true
        fi
    done

    if [[ "$syntax_errors" == "true" ]]; then
        log_error "Errores de sintaxis Gherkin encontrados"
        return 1
    else
        log_success "Sintaxis Gherkin correcta"
        return 0
    fi
}

# Verificar versiones SNAPSHOT en build.gradle
check_snapshot_versions() {
    if [[ "$CHECK_SNAPSHOT_VERSIONS" != "true" ]]; then
        return 0
    fi

    log_section "📦 Verificando Versiones SNAPSHOT"

    # Verificar si build.gradle está staged
    if ! git diff --cached --name-only | grep -q "build.gradle"; then
        log_info "build.gradle no modificado"
        return 0
    fi

    # Buscar versiones SNAPSHOT
    if git diff --cached build.gradle | grep -qE "^\+.*SNAPSHOT"; then
        log_error "Dependencias SNAPSHOT detectadas en build.gradle"
        log_info "Remueve versiones SNAPSHOT antes de commitear"
        return 1
    else
        log_success "No hay versiones SNAPSHOT"
        return 0
    fi
}

# Verificar que archivos .env no se commiteen
check_env_files() {
    if [[ "$CHECK_ENV_FILES" != "true" ]]; then
        return 0
    fi

    log_section "🚫 Verificando Archivos de Configuración"

    # Verificar archivos staged
    local env_files=$(git diff --cached --name-only | grep -E "\.(env|env\.local|env\.*)" || true)

    if [[ -n "$env_files" ]]; then
        log_error "Archivos .env detectados en staging:"
        echo "$env_files" | while read -r file; do
            echo "  ❌ $file"
        done
        log_info "Los archivos .env NO deben commitearse (contienen credenciales)"
        log_info "Ejecuta: git reset HEAD <archivo>"
        return 1
    else
        log_success "No hay archivos .env staged"
        return 0
    fi
}

# ============================================================================
# MAIN
# ============================================================================

main() {
    log_banner "🚀 Pre-Commit Hook - Scotia QA Framework"

    local all_checks_passed=true

    # Ejecutar todas las validaciones
    if ! check_env_files; then
        all_checks_passed=false
    fi

    if ! check_credentials; then
        all_checks_passed=false
    fi

    if ! check_snapshot_versions; then
        all_checks_passed=false
    fi

    if ! check_gherkin_syntax; then
        all_checks_passed=false
    fi

    if ! check_code_format; then
        all_checks_passed=false
    fi

    if ! run_smoke_tests; then
        all_checks_passed=false
    fi

    echo ""

    # Resultado final
    if [[ "$all_checks_passed" == "true" ]]; then
        log_success "✅ Todas las validaciones pasaron"
        log_info "Procediendo con el commit..."
        exit 0
    else
        log_error "❌ Algunas validaciones fallaron"
        echo ""
        log_info "Opciones:"
        log_info "  1. Corrige los problemas y vuelve a intentar"
        log_info "  2. Usa --no-verify para saltar validaciones (NO recomendado)"
        exit 1
    fi
}

# Ejecutar
main "$@"

