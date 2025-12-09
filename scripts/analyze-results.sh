#!/bin/bash

# ============================================================================
# Scotia QA Framework - Analizador de Resultados de Tests
# ============================================================================
# VERSION: 1.0.0
#
# Analiza resultados de tests y genera métricas útiles:
#   ✅ Estadísticas (passed/failed/skipped)
#   ✅ Tests más lentos
#   ✅ Detección de tests flaky
#   ✅ Cobertura por tags
#   ✅ Reporte HTML/Markdown
#
# USO:
#   ./analyze-results.sh                          # Analizar último build
#   ./analyze-results.sh --dir build/test-results # Directorio específico
#   ./analyze-results.sh --output html            # Generar HTML
#   ./analyze-results.sh --flaky                  # Solo tests flaky
#   ./analyze-results.sh --top 20                 # Top 20 tests más lentos
#
# @author Abel Venero
# @version 1.0.0
# ============================================================================

set -e

# ============================================================================
# CARGAR UTILIDADES
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# NOTE: utils.sh fue eliminado - funciones básicas implementadas localmente

# ============================================================================
# VARIABLES GLOBALES
# ============================================================================

RESULTS_DIR="build/test-results/test"
OUTPUT_FORMAT="terminal"  # terminal, html, markdown
TOP_N=10
SHOW_FLAKY_ONLY=false
MIN_DURATION=0

# Contadores
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0
TOTAL_DURATION=0

# ============================================================================
# FUNCIONES DE PARSEO
# ============================================================================

# Parsear archivos XML de JUnit
parse_junit_xml() {
    local xml_file="$1"

    if [[ ! -f "$xml_file" ]]; then
        return 1
    fi

    # Extraer estadísticas del archivo XML
    local tests=$(grep -o 'tests="[0-9]*"' "$xml_file" | head -1 | grep -o '[0-9]*')
    local failures=$(grep -o 'failures="[0-9]*"' "$xml_file" | head -1 | grep -o '[0-9]*')
    local skipped=$(grep -o 'skipped="[0-9]*"' "$xml_file" | head -1 | grep -o '[0-9]*')
    local time=$(grep -o 'time="[0-9.]*"' "$xml_file" | head -1 | grep -o '[0-9.]*')

    # Actualizar contadores globales
    TOTAL_TESTS=$((TOTAL_TESTS + ${tests:-0}))
    FAILED_TESTS=$((FAILED_TESTS + ${failures:-0}))
    SKIPPED_TESTS=$((SKIPPED_TESTS + ${skipped:-0}))
    TOTAL_DURATION=$(echo "$TOTAL_DURATION + ${time:-0}" | bc)
}

# Extraer tests individuales de XML
extract_test_cases() {
    local xml_file="$1"
    local output_file="$2"

    # Usar xmllint si está disponible, sino usar grep
    if command -v xmllint &> /dev/null; then
        xmllint --xpath "//testcase" "$xml_file" 2>/dev/null | \
            sed 's/<testcase /\n<testcase /g' >> "$output_file"
    else
        grep -o '<testcase[^>]*>' "$xml_file" >> "$output_file" || true
    fi
}

# ============================================================================
# ANÁLISIS DE TESTS
# ============================================================================

# Analizar todos los archivos XML
analyze_results() {
    log_section "📊 Analizando Resultados"

    if [[ ! -d "$RESULTS_DIR" ]]; then
        log_error "Directorio de resultados no encontrado: $RESULTS_DIR"
        log_info "Ejecuta tests primero: ./gradlew test"
        exit 1
    fi

    # Buscar archivos XML
    local xml_files=$(find "$RESULTS_DIR" -name "TEST-*.xml" 2>/dev/null)

    if [[ -z "$xml_files" ]]; then
        log_warning "No se encontraron archivos de resultados XML"
        exit 1
    fi

    log_info "Procesando archivos XML..."

    # Archivo temporal para tests individuales
    local temp_tests="/tmp/test_cases_$$.txt"
    > "$temp_tests"

    # Parsear cada archivo XML
    while IFS= read -r xml_file; do
        parse_junit_xml "$xml_file"
        extract_test_cases "$xml_file" "$temp_tests"
    done <<< "$xml_files"

    # Calcular passed
    PASSED_TESTS=$((TOTAL_TESTS - FAILED_TESTS - SKIPPED_TESTS))

    log_success "Análisis completado"
}

# Identificar tests más lentos
find_slowest_tests() {
    log_section "🐌 Tests Más Lentos (Top $TOP_N)"

    local temp_file="/tmp/slow_tests_$$.txt"
    > "$temp_file"

    # Buscar y extraer tiempos de tests
    find "$RESULTS_DIR" -name "TEST-*.xml" -exec grep -h '<testcase' {} \; 2>/dev/null | \
        grep -o 'name="[^"]*" classname="[^"]*" time="[^"]*"' | \
        awk -F'"' '{printf "%s\t%s\t%s\n", $6, $2, $4}' | \
        sort -rn > "$temp_file"

    if [[ ! -s "$temp_file" ]]; then
        log_warning "No se pudieron extraer tiempos de tests"
        return
    fi

    echo ""
    printf "${BOLD}%-10s %-50s %-40s${NC}\n" "Tiempo" "Test" "Clase"
    printf "%-10s %-50s %-40s\n" "──────────" "──────────────────────────────────────────────────" "────────────────────────────────────────"

    head -n "$TOP_N" "$temp_file" | while IFS=$'\t' read -r time test_name class_name; do
        # Formatear tiempo
        local formatted_time="${time}s"
        if (( $(echo "$time > 60" | bc -l 2>/dev/null || echo 0) )); then
            formatted_time=$(printf "%.1fm" $(echo "$time / 60" | bc -l))
        fi

        # Truncar nombres largos
        test_name=$(echo "$test_name" | cut -c1-48)
        class_name=$(echo "$class_name" | cut -c1-38)

        printf "%-10s %-50s %-40s\n" "$formatted_time" "$test_name" "$class_name"
    done

    echo ""
    rm -f "$temp_file"
}

# Detectar tests flaky (análisis histórico requiere múltiples ejecuciones)
detect_flaky_tests() {
    log_section "🎲 Detección de Tests Flaky"

    # Nota: Para detectar flaky reales, necesitamos historial de múltiples ejecuciones
    # Esta es una implementación básica

    log_info "Buscando patrones de inestabilidad..."

    # Buscar tests que fallaron
    local failed_tests=$(find "$RESULTS_DIR" -name "TEST-*.xml" -exec grep -l '<failure' {} \; 2>/dev/null | wc -l)

    if [[ $failed_tests -eq 0 ]]; then
        log_success "No se detectaron tests con fallos"
        echo ""
        return
    fi

    echo ""
    log_warning "Tests con fallos detectados: $failed_tests"
    log_info "Para análisis flaky completo, ejecuta múltiples veces y compara resultados"
    echo ""

    # Listar tests fallidos
    find "$RESULTS_DIR" -name "TEST-*.xml" -exec grep -h '<testcase.*<failure' {} \; 2>/dev/null | \
        grep -o 'name="[^"]*"' | \
        sed 's/name="//;s/"$//' | \
        sort -u | \
        head -n 10 | \
        while read -r test; do
            echo "  ❌ $test"
        done
}

# Cobertura por tags (requiere análisis de features)
analyze_tag_coverage() {
    log_section "🏷️  Cobertura por Tags"

    # Buscar archivos .feature
    local feature_dir="src/test/resources/features"

    if [[ ! -d "$feature_dir" ]]; then
        log_warning "Directorio de features no encontrado: $feature_dir"
        return
    fi

    log_info "Analizando tags en features..."

    # Extraer y contar tags
    local tags=$(find "$feature_dir" -name "*.feature" -exec grep -h '@' {} \; 2>/dev/null | \
        grep -o '@[a-zA-Z0-9_-]*' | \
        sort | uniq -c | sort -rn)

    if [[ -z "$tags" ]]; then
        log_warning "No se encontraron tags en features"
        return
    fi

    echo ""
    printf "${BOLD}%-10s %-30s${NC}\n" "Usos" "Tag"
    printf "%-10s %-30s\n" "──────────" "──────────────────────────────"

    echo "$tags" | head -n 15 | while read -r count tag; do
        printf "%-10s %-30s\n" "$count" "$tag"
    done

    echo ""
}

# ============================================================================
# GENERACIÓN DE REPORTES
# ============================================================================

# Mostrar resumen en terminal
show_terminal_summary() {
    log_banner "Resumen de Ejecución"

    # Calcular porcentajes
    local pass_percent=0
    local fail_percent=0
    local skip_percent=0

    if [[ $TOTAL_TESTS -gt 0 ]]; then
        pass_percent=$(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)
        fail_percent=$(echo "scale=1; $FAILED_TESTS * 100 / $TOTAL_TESTS" | bc)
        skip_percent=$(echo "scale=1; $SKIPPED_TESTS * 100 / $TOTAL_TESTS" | bc)
    fi

    # Formatear duración
    local duration_formatted="${TOTAL_DURATION}s"
    if (( $(echo "$TOTAL_DURATION > 60" | bc -l) )); then
        duration_formatted=$(printf "%.1fm" $(echo "$TOTAL_DURATION / 60" | bc -l))
    fi

    echo ""
    echo -e "${BOLD}Estadísticas Generales:${NC}"
    echo "────────────────────────────────────────"
    echo -e "  ${GREEN}✓ Passed:${NC}   $PASSED_TESTS ($pass_percent%)"
    echo -e "  ${RED}✗ Failed:${NC}   $FAILED_TESTS ($fail_percent%)"
    echo -e "  ${YELLOW}⊘ Skipped:${NC}  $SKIPPED_TESTS ($skip_percent%)"
    echo "  ──────────────────────"
    echo -e "  ${BOLD}Total:${NC}      $TOTAL_TESTS tests"
    echo -e "  ${BOLD}Duración:${NC}   $duration_formatted"
    echo ""

    # Indicador de salud
    if [[ $FAILED_TESTS -eq 0 ]] && [[ $PASSED_TESTS -gt 0 ]]; then
        echo -e "  ${GREEN}${BOLD}✓ Build Exitoso${NC}"
    elif [[ $FAILED_TESTS -gt 0 ]]; then
        echo -e "  ${RED}${BOLD}✗ Build Falló${NC}"
    else
        echo -e "  ${YELLOW}${BOLD}⚠ Sin Tests Ejecutados${NC}"
    fi

    echo ""
}

# Generar reporte HTML
generate_html_report() {
    local output_file="build/reports/test-analysis.html"

    log_info "Generando reporte HTML: $output_file"

    mkdir -p "$(dirname "$output_file")"

    cat > "$output_file" << 'EOHTML'
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Análisis de Tests - Scotia QA Framework</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 20px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        h1 { color: #333; margin-bottom: 30px; }
        .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px; }
        .stat-card { padding: 20px; border-radius: 8px; text-align: center; }
        .stat-card.passed { background: #d4edda; border-left: 4px solid #28a745; }
        .stat-card.failed { background: #f8d7da; border-left: 4px solid #dc3545; }
        .stat-card.skipped { background: #fff3cd; border-left: 4px solid #ffc107; }
        .stat-card.total { background: #d1ecf1; border-left: 4px solid #17a2b8; }
        .stat-card h3 { font-size: 14px; color: #666; margin-bottom: 10px; }
        .stat-card .number { font-size: 36px; font-weight: bold; color: #333; }
        .stat-card .percent { font-size: 14px; color: #666; }
        .section { margin-top: 30px; }
        .section h2 { color: #333; margin-bottom: 15px; border-bottom: 2px solid #007bff; padding-bottom: 10px; }
        table { width: 100%; border-collapse: collapse; margin-top: 15px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
        th { background: #007bff; color: white; font-weight: 600; }
        tr:hover { background: #f8f9fa; }
        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; text-align: center; color: #666; font-size: 14px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>📊 Análisis de Tests - Scotia QA Framework</h1>

        <div class="stats">
            <div class="stat-card passed">
                <h3>✓ Passed</h3>
                <div class="number">PASSED_COUNT</div>
                <div class="percent">PASSED_PERCENT%</div>
            </div>
            <div class="stat-card failed">
                <h3>✗ Failed</h3>
                <div class="number">FAILED_COUNT</div>
                <div class="percent">FAILED_PERCENT%</div>
            </div>
            <div class="stat-card skipped">
                <h3>⊘ Skipped</h3>
                <div class="number">SKIPPED_COUNT</div>
                <div class="percent">SKIPPED_PERCENT%</div>
            </div>
            <div class="stat-card total">
                <h3>Total</h3>
                <div class="number">TOTAL_COUNT</div>
                <div class="percent">DURATION</div>
            </div>
        </div>

        <div class="section">
            <h2>🐌 Tests Más Lentos</h2>
            <table>
                <thead>
                    <tr>
                        <th>Tiempo</th>
                        <th>Test</th>
                        <th>Clase</th>
                    </tr>
                </thead>
                <tbody id="slowTests">
                    <!-- Se llenará dinámicamente -->
                </tbody>
            </table>
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
    local pass_percent=$(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)
    local fail_percent=$(echo "scale=1; $FAILED_TESTS * 100 / $TOTAL_TESTS" | bc)
    local skip_percent=$(echo "scale=1; $SKIPPED_TESTS * 100 / $TOTAL_TESTS" | bc)

    sed -i.bak "s/PASSED_COUNT/$PASSED_TESTS/g" "$output_file"
    sed -i.bak "s/FAILED_COUNT/$FAILED_TESTS/g" "$output_file"
    sed -i.bak "s/SKIPPED_COUNT/$SKIPPED_TESTS/g" "$output_file"
    sed -i.bak "s/TOTAL_COUNT/$TOTAL_TESTS/g" "$output_file"
    sed -i.bak "s/PASSED_PERCENT/$pass_percent/g" "$output_file"
    sed -i.bak "s/FAILED_PERCENT/$fail_percent/g" "$output_file"
    sed -i.bak "s/SKIPPED_PERCENT/$skip_percent/g" "$output_file"
    sed -i.bak "s/DURATION/${TOTAL_DURATION}s/g" "$output_file"
    sed -i.bak "s/TIMESTAMP/$(date '+%Y-%m-%d %H:%M:%S')/g" "$output_file"

    rm -f "${output_file}.bak"

    log_success "Reporte HTML generado: $output_file"
}

# ============================================================================
# PARSEADO DE ARGUMENTOS
# ============================================================================

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --dir)
                RESULTS_DIR="$2"
                shift 2
                ;;
            --output)
                OUTPUT_FORMAT="$2"
                shift 2
                ;;
            --top)
                TOP_N="$2"
                shift 2
                ;;
            --flaky)
                SHOW_FLAKY_ONLY=true
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
${BOLD}Scotia QA Framework - Analizador de Resultados${NC}

${BOLD}USO:${NC}
    ./analyze-results.sh [opciones]

${BOLD}OPCIONES:${NC}
    --dir <directorio>    Directorio de resultados (default: build/test-results/test)
    --output <formato>    Formato de salida: terminal, html, markdown
    --top <número>        Top N tests más lentos (default: 10)
    --flaky               Mostrar solo tests flaky
    -h, --help            Mostrar esta ayuda

${BOLD}EJEMPLOS:${NC}
    # Analizar último build
    ./analyze-results.sh

    # Generar reporte HTML
    ./analyze-results.sh --output html

    # Top 20 tests más lentos
    ./analyze-results.sh --top 20

    # Directorio personalizado
    ./analyze-results.sh --dir target/surefire-reports

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
    log_banner "Analizador de Resultados de Tests"

    parse_arguments "$@"

    # Analizar resultados
    analyze_results

    # Mostrar resumen
    show_terminal_summary

    # Análisis adicionales
    if [[ "$SHOW_FLAKY_ONLY" == "false" ]]; then
        find_slowest_tests
        detect_flaky_tests
        analyze_tag_coverage
    else
        detect_flaky_tests
    fi

    # Generar reporte según formato
    case "$OUTPUT_FORMAT" in
        html)
            generate_html_report
            ;;
        terminal)
            # Ya mostrado arriba
            ;;
        *)
            log_warning "Formato desconocido: $OUTPUT_FORMAT"
            ;;
    esac

    echo ""
    log_success "Análisis completado"

    # Código de salida según resultados
    if [[ $FAILED_TESTS -gt 0 ]]; then
        exit 1
    else
        exit 0
    fi
}

# Ejecutar
main "$@"

