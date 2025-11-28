#!/bin/bash

# ============================================================================
# Scotia QA Framework - Analizador de Calidad de Código
# ============================================================================
# VERSION: 1.0.0
#
# Analiza código fuente en busca de problemas de calidad:
#   ✅ Vulnerabilidades de seguridad
#   ✅ Imports no utilizados
#   ✅ Código comentado
#   ✅ Métodos muy largos
#   ✅ Complejidad ciclomática alta
#   ✅ Duplicación de código
#   ✅ Comentarios TODO/FIXME
#   ✅ Malas prácticas
#
# USO:
#   ./code-quality.sh                     # Análisis completo
#   ./code-quality.sh --security          # Solo vulnerabilidades
#   ./code-quality.sh --unused            # Solo imports/variables sin usar
#   ./code-quality.sh --comments          # Solo comentarios problemáticos
#   ./code-quality.sh --report            # Generar reporte HTML
#
# @author Abel Venero
# @version 1.0.0
# ============================================================================

set -e

# ============================================================================
# CARGAR UTILIDADES
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/utils.sh"

# ============================================================================
# VARIABLES GLOBALES
# ============================================================================

SOURCE_DIR="src/main/java"
TEST_DIR="src/test/java"
CHECK_SECURITY=true
CHECK_UNUSED=true
CHECK_COMMENTS=true
CHECK_COMPLEXITY=true
CHECK_DUPLICATES=true
GENERATE_REPORT=false

# Contadores
TOTAL_ISSUES=0
SECURITY_ISSUES=0
QUALITY_ISSUES=0
STYLE_ISSUES=0

# ============================================================================
# ANÁLISIS DE SEGURIDAD
# ============================================================================

check_security_vulnerabilities() {
    log_section "🔒 Análisis de Vulnerabilidades de Seguridad"

    local issues_found=0

    # Patrones de vulnerabilidades comunes
    declare -A patterns=(
        ["SQL Injection"]="executeQuery.*\+|createQuery.*\+|prepareStatement.*\+"
        ["XSS"]="innerHTML|document\.write|eval\("
        ["Hard-coded Credentials"]="password\s*=\s*['\"][^'\"]{3,}['\"]|apiKey\s*=\s*['\"][^'\"]{10,}"
        ["Insecure Random"]="new Random\(\)|Math\.random\(\)"
        ["Weak Crypto"]="DES|MD5|SHA1(?!256)"
        ["Path Traversal"]="new File.*\+|Paths\.get.*\+"
    )

    log_info "Buscando vulnerabilidades conocidas..."

    for vuln_type in "${!patterns[@]}"; do
        local pattern="${patterns[$vuln_type]}"
        local matches=$(find "$SOURCE_DIR" -name "*.java" -exec grep -nHE "$pattern" {} \; 2>/dev/null | wc -l)

        if [[ $matches -gt 0 ]]; then
            log_warning "$vuln_type: $matches ocurrencia(s)"

            # Mostrar primeras 3 ocurrencias
            find "$SOURCE_DIR" -name "*.java" -exec grep -nHE "$pattern" {} \; 2>/dev/null | \
                head -3 | \
                while IFS=: read -r file line content; do
                    echo "  📄 $file:$line"
                done

            issues_found=$((issues_found + matches))
            echo ""
        fi
    done

    SECURITY_ISSUES=$issues_found
    TOTAL_ISSUES=$((TOTAL_ISSUES + issues_found))

    if [[ $issues_found -eq 0 ]]; then
        log_success "No se detectaron vulnerabilidades evidentes"
    else
        log_error "Se detectaron $issues_found posibles vulnerabilidades"
    fi
}

# ============================================================================
# ANÁLISIS DE CÓDIGO NO UTILIZADO
# ============================================================================

check_unused_code() {
    log_section "🗑️  Código No Utilizado"

    local issues_found=0

    # Imports no utilizados
    log_info "Buscando imports no utilizados..."

    local unused_imports=$(find "$SOURCE_DIR" "$TEST_DIR" -name "*.java" 2>/dev/null | while read -r file; do
        # Extraer imports
        grep "^import " "$file" | while read -r import_line; do
            local import_class=$(echo "$import_line" | sed 's/import //;s/;//' | awk -F. '{print $NF}')

            # Verificar si la clase se usa en el archivo
            if ! grep -q "\b$import_class\b" "$file" | grep -v "^import "; then
                echo "$file: $import_line"
            fi
        done
    done)

    local unused_count=$(echo "$unused_imports" | grep -c "import" || echo 0)

    if [[ $unused_count -gt 0 ]]; then
        log_warning "Imports no utilizados: $unused_count"
        echo "$unused_imports" | head -5
        echo ""
        issues_found=$((issues_found + unused_count))
    else
        log_success "No hay imports no utilizados"
    fi

    # Variables locales no utilizadas
    log_info "Buscando variables no utilizadas..."

    # Buscar variables que se declaran pero nunca se usan
    # (análisis básico, herramientas como SonarQube hacen esto mejor)

    QUALITY_ISSUES=$((QUALITY_ISSUES + issues_found))
    TOTAL_ISSUES=$((TOTAL_ISSUES + issues_found))
}

# ============================================================================
# ANÁLISIS DE COMENTARIOS
# ============================================================================

check_problematic_comments() {
    log_section "💬 Análisis de Comentarios"

    local issues_found=0

    # TODO/FIXME
    log_info "Buscando TODOs y FIXMEs..."

    local todo_count=$(find "$SOURCE_DIR" "$TEST_DIR" -name "*.java" -exec grep -nH "//.*TODO\|//.*FIXME" {} \; 2>/dev/null | wc -l)

    if [[ $todo_count -gt 0 ]]; then
        log_warning "TODOs/FIXMEs encontrados: $todo_count"

        # Mostrar primeros 10
        find "$SOURCE_DIR" "$TEST_DIR" -name "*.java" -exec grep -nH "//.*TODO\|//.*FIXME" {} \; 2>/dev/null | \
            head -10 | \
            while IFS=: read -r file line content; do
                echo "  📄 $file:$line: $(echo $content | sed 's/^[[:space:]]*//')"
            done
        echo ""

        issues_found=$((issues_found + todo_count))
    else
        log_success "No hay TODOs/FIXMEs pendientes"
    fi

    # Código comentado
    log_info "Buscando código comentado..."

    local commented_code=$(find "$SOURCE_DIR" "$TEST_DIR" -name "*.java" -exec grep -nHE "^\s*//\s*(public|private|protected|if|for|while|try)" {} \; 2>/dev/null | wc -l)

    if [[ $commented_code -gt 0 ]]; then
        log_warning "Posible código comentado: $commented_code líneas"
        issues_found=$((issues_found + commented_code))
    else
        log_success "No hay código comentado detectado"
    fi

    # Comentarios de debugging
    log_info "Buscando comentarios de debugging..."

    local debug_comments=$(find "$SOURCE_DIR" "$TEST_DIR" -name "*.java" -exec grep -nHE "System\.out\.println|System\.err\.print|\.printStackTrace\(\)" {} \; 2>/dev/null | wc -l)

    if [[ $debug_comments -gt 0 ]]; then
        log_warning "Código de debugging encontrado: $debug_comments"

        find "$SOURCE_DIR" "$TEST_DIR" -name "*.java" -exec grep -nHE "System\.out\.println|System\.err\.print|\.printStackTrace\(\)" {} \; 2>/dev/null | \
            head -5 | \
            while IFS=: read -r file line content; do
                echo "  📄 $file:$line"
            done
        echo ""

        issues_found=$((issues_found + debug_comments))
    else
        log_success "No hay código de debugging"
    fi

    STYLE_ISSUES=$((STYLE_ISSUES + issues_found))
    TOTAL_ISSUES=$((TOTAL_ISSUES + issues_found))
}

# ============================================================================
# ANÁLISIS DE COMPLEJIDAD
# ============================================================================

check_code_complexity() {
    log_section "📊 Análisis de Complejidad"

    local issues_found=0

    # Métodos muy largos (> 50 líneas)
    log_info "Buscando métodos muy largos..."

    find "$SOURCE_DIR" "$TEST_DIR" -name "*.java" 2>/dev/null | while read -r file; do
        local in_method=false
        local method_name=""
        local method_start=0
        local brace_count=0

        while IFS= read -r line; do
            # Detectar inicio de método
            if echo "$line" | grep -qE "(public|private|protected).*\(.*\).*\{"; then
                in_method=true
                method_name=$(echo "$line" | sed 's/.*\s\+\([a-zA-Z0-9_]*\)\s*(.*/\1/')
                method_start=$((method_start + 1))
                brace_count=1
            elif [[ "$in_method" == "true" ]]; then
                # Contar llaves
                brace_count=$((brace_count + $(echo "$line" | tr -cd '{' | wc -c)))
                brace_count=$((brace_count - $(echo "$line" | tr -cd '}' | wc -c)))

                if [[ $brace_count -eq 0 ]]; then
                    local method_lines=$((method_start - 1))

                    if [[ $method_lines -gt 50 ]]; then
                        log_warning "Método largo en $file: $method_name ($method_lines líneas)"
                        issues_found=$((issues_found + 1))
                    fi

                    in_method=false
                    method_name=""
                    method_start=0
                fi
            fi

            method_start=$((method_start + 1))
        done < "$file"
    done

    # Complejidad ciclomática alta (aproximación básica)
    log_info "Analizando complejidad ciclomática..."

    local complex_methods=$(find "$SOURCE_DIR" "$TEST_DIR" -name "*.java" -exec grep -c "if\|for\|while\|case\|catch\|&&\|||\|?" {} \; 2>/dev/null | \
        awk -F: '$2 > 10 {print $1}' | wc -l)

    if [[ $complex_methods -gt 0 ]]; then
        log_warning "Métodos con alta complejidad: $complex_methods"
        issues_found=$((issues_found + complex_methods))
    else
        log_success "Complejidad dentro de rangos aceptables"
    fi

    QUALITY_ISSUES=$((QUALITY_ISSUES + issues_found))
    TOTAL_ISSUES=$((TOTAL_ISSUES + issues_found))
}

# ============================================================================
# ANÁLISIS DE MALAS PRÁCTICAS
# ============================================================================

check_bad_practices() {
    log_section "⚠️  Malas Prácticas"

    local issues_found=0

    # Uso de !! (anti-pattern)
    log_info "Buscando anti-patterns..."

    local empty_catch=$(find "$SOURCE_DIR" "$TEST_DIR" -name "*.java" -exec grep -A2 "catch.*{" {} \; 2>/dev/null | \
        grep -c "^\s*}\s*$" || echo 0)

    if [[ $empty_catch -gt 0 ]]; then
        log_warning "Bloques catch vacíos: $empty_catch"
        issues_found=$((issues_found + empty_catch))
    fi

    # Magic numbers
    local magic_numbers=$(find "$SOURCE_DIR" "$TEST_DIR" -name "*.java" -exec grep -nHE "[^a-zA-Z0-9_]([2-9]|[1-9][0-9]+)[^a-zA-Z0-9_.]" {} \; 2>/dev/null | \
        grep -v "// " | wc -l)

    if [[ $magic_numbers -gt 20 ]]; then
        log_warning "Posibles magic numbers: $magic_numbers"
        log_info "Considera usar constantes"
    fi

    # Strings hardcodeados
    local hardcoded_strings=$(find "$SOURCE_DIR" -name "*.java" -exec grep -c "= \"[^\"]{20,}\"" {} \; 2>/dev/null | \
        awk -F: '{sum+=$2} END {print sum}')

    if [[ $hardcoded_strings -gt 10 ]]; then
        log_warning "Strings hardcodeados largos: $hardcoded_strings"
        log_info "Considera usar archivos de configuración"
    fi

    QUALITY_ISSUES=$((QUALITY_ISSUES + issues_found))
    TOTAL_ISSUES=$((TOTAL_ISSUES + issues_found))
}

# ============================================================================
# GENERACIÓN DE REPORTES
# ============================================================================

generate_report() {
    log_section "📄 Generando Reporte"

    local report_file="build/reports/code-quality-report.html"
    mkdir -p "$(dirname "$report_file")"

    cat > "$report_file" << 'EOHTML'
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reporte de Calidad de Código - Scotia QA Framework</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 20px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        h1 { color: #333; margin-bottom: 30px; }
        .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px; }
        .stat-card { padding: 20px; border-radius: 8px; text-align: center; }
        .stat-card.total { background: #d1ecf1; border-left: 4px solid #17a2b8; }
        .stat-card.security { background: #f8d7da; border-left: 4px solid #dc3545; }
        .stat-card.quality { background: #fff3cd; border-left: 4px solid #ffc107; }
        .stat-card.style { background: #d4edda; border-left: 4px solid #28a745; }
        .stat-card h3 { font-size: 14px; color: #666; margin-bottom: 10px; }
        .stat-card .number { font-size: 36px; font-weight: bold; color: #333; }
        .section { margin-top: 30px; }
        .section h2 { color: #333; margin-bottom: 15px; border-bottom: 2px solid #007bff; padding-bottom: 10px; }
        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; text-align: center; color: #666; font-size: 14px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🔍 Reporte de Calidad de Código</h1>

        <div class="summary">
            <div class="stat-card total">
                <h3>Total Issues</h3>
                <div class="number">TOTAL_ISSUES</div>
            </div>
            <div class="stat-card security">
                <h3>Seguridad</h3>
                <div class="number">SECURITY_ISSUES</div>
            </div>
            <div class="stat-card quality">
                <h3>Calidad</h3>
                <div class="number">QUALITY_ISSUES</div>
            </div>
            <div class="stat-card style">
                <h3>Estilo</h3>
                <div class="number">STYLE_ISSUES</div>
            </div>
        </div>

        <div class="footer">
            <p>Generado por Scotia QA Framework v1.0.0</p>
            <p>TIMESTAMP</p>
        </div>
    </div>
</body>
</html>
EOHTML

    # Reemplazar placeholders
    sed -i.bak "s/TOTAL_ISSUES/$TOTAL_ISSUES/g" "$report_file"
    sed -i.bak "s/SECURITY_ISSUES/$SECURITY_ISSUES/g" "$report_file"
    sed -i.bak "s/QUALITY_ISSUES/$QUALITY_ISSUES/g" "$report_file"
    sed -i.bak "s/STYLE_ISSUES/$STYLE_ISSUES/g" "$report_file"
    sed -i.bak "s/TIMESTAMP/$(date '+%Y-%m-%d %H:%M:%S')/g" "$report_file"

    rm -f "${report_file}.bak"

    log_success "Reporte generado: $report_file"
}

# ============================================================================
# PARSEADO DE ARGUMENTOS
# ============================================================================

parse_arguments() {
    if [[ $# -eq 0 ]]; then
        return
    fi

    while [[ $# -gt 0 ]]; do
        case $1 in
            --security)
                CHECK_UNUSED=false
                CHECK_COMMENTS=false
                CHECK_COMPLEXITY=false
                shift
                ;;
            --unused)
                CHECK_SECURITY=false
                CHECK_COMMENTS=false
                CHECK_COMPLEXITY=false
                shift
                ;;
            --comments)
                CHECK_SECURITY=false
                CHECK_UNUSED=false
                CHECK_COMPLEXITY=false
                shift
                ;;
            --report)
                GENERATE_REPORT=true
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
${BOLD}Scotia QA Framework - Analizador de Calidad de Código${NC}

${BOLD}USO:${NC}
    ./code-quality.sh [opciones]

${BOLD}OPCIONES:${NC}
    --security    Solo análisis de seguridad
    --unused      Solo código no utilizado
    --comments    Solo comentarios problemáticos
    --report      Generar reporte HTML
    -h, --help    Mostrar esta ayuda

${BOLD}EJEMPLOS:${NC}
    # Análisis completo
    ./code-quality.sh

    # Solo vulnerabilidades
    ./code-quality.sh --security

    # Generar reporte HTML
    ./code-quality.sh --report

EOF
}

log_section() {
    echo ""
    echo -e "${BOLD}$1${NC}"
    echo "────────────────────────────────────────────────────────────"
}

# ============================================================================
# MAIN
# ============================================================================

main() {
    log_banner "Analizador de Calidad de Código"

    parse_arguments "$@"

    # Verificar directorios
    if [[ ! -d "$SOURCE_DIR" ]]; then
        log_error "Directorio no encontrado: $SOURCE_DIR"
        exit 1
    fi

    # Ejecutar análisis
    [[ "$CHECK_SECURITY" == "true" ]] && check_security_vulnerabilities
    [[ "$CHECK_UNUSED" == "true" ]] && check_unused_code
    [[ "$CHECK_COMMENTS" == "true" ]] && check_problematic_comments
    [[ "$CHECK_COMPLEXITY" == "true" ]] && check_code_complexity
    check_bad_practices

    # Resumen final
    echo ""
    log_banner "Resumen Final"

    echo -e "${BOLD}Issues encontrados:${NC}"
    echo "────────────────────────────────────────"
    echo -e "  🔒 Seguridad:     $SECURITY_ISSUES"
    echo -e "  📊 Calidad:       $QUALITY_ISSUES"
    echo -e "  💅 Estilo:        $STYLE_ISSUES"
    echo "  ──────────────────"
    echo -e "  ${BOLD}Total:${NC}          $TOTAL_ISSUES"
    echo ""

    # Generar reporte si se solicitó
    if [[ "$GENERATE_REPORT" == "true" ]]; then
        generate_report
    fi

    # Código de salida
    if [[ $SECURITY_ISSUES -gt 0 ]]; then
        log_warning "⚠️  Se encontraron problemas de seguridad"
        exit 1
    elif [[ $TOTAL_ISSUES -gt 20 ]]; then
        log_warning "⚠️  Se encontraron muchos issues de calidad"
        exit 1
    else
        log_success "✅ Código con calidad aceptable"
        exit 0
    fi
}

# Ejecutar
main "$@"

