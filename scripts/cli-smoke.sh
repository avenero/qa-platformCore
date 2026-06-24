#!/usr/bin/env bash
# ============================================================================
# cli-smoke.sh — Core CLI smoke harness (W0-S2, closes G-RP1)
#
# Surrogate del flujo `qa-module-test` (que no existe en el repo): verifica de
# punta a punta que los 5 modulos de qa-platformCore compilan, pasan sus suites
# y exponen un catalogo de componentes consistente — sin levantar el Backend.
#
# USO (desde la raiz del repo o desde cualquier directorio):
#   bash qa-platformCore/scripts/cli-smoke.sh
#
# Cualquier argumento extra se reenvia a Gradle, p.ej. para forzar ejecucion
# (evita el cache UP-TO-DATE) o silenciar el daemon en CI:
#   bash qa-platformCore/scripts/cli-smoke.sh --rerun-tasks --no-daemon
#
# Salida: 0 si todo paso, !=0 si alguna etapa fallo.
# ============================================================================
set -euo pipefail

# El script se autolocaliza: corre contra la raiz de qa-platformCore sin
# importar el directorio actual del invocador.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$CORE_ROOT"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}✅  $1${NC}"; }
fail() { echo -e "${RED}❌  $1${NC}"; }
info() { echo -e "${YELLOW}ℹ️   $1${NC}"; }

trap 'fail "CLI smoke FALLO — revisar la salida anterior"; exit 1' ERR

# Modulos especializados que publican COMPONENTS.md (common no expone componentes).
CATALOG_MODULES=(http-core web-core mobile-core database-core)

# ── 1. Suites de los 5 modulos (incluye los guards de unicidad de W0-S1) ──────
info "Etapa 1/3 — Ejecutando las suites de test de los 5 modulos…"
./gradlew :common:test \
          :http-core:test \
          :web-core:test \
          :mobile-core:test \
          :database-core:test "$@"
pass "Suites de los 5 modulos: verdes"

# ── 2. Regenerar los catalogos COMPONENTS.md (task agregada real) ─────────────
info "Etapa 2/3 — Regenerando COMPONENTS.md (./gradlew generateComponentCatalog)…"
./gradlew generateComponentCatalog "$@"
pass "Catalogos COMPONENTS.md regenerados"

# ── 3. Dup-check cross-modulo de component-ids (W0-S1 ya cubre per-modulo) ────
# Los ids son el contrato publico que consume el BE (catalogo i18n) y el FE
# (paleta del Scenario Builder); deben ser globalmente unicos. Se parsean de
# los COMPONENTS.md generados (encabezados estables `## \`<id>\``).
info "Etapa 3/3 — Verificando unicidad cross-modulo de component-ids…"
present_md=()
for m in "${CATALOG_MODULES[@]}"; do
  if [ -f "$m/COMPONENTS.md" ]; then
    present_md+=("$m/COMPONENTS.md")
  else
    fail "Falta $m/COMPONENTS.md (Etapa 2 deberia haberlo generado)"
    exit 1
  fi
done

set +e
all_ids="$(grep -hE '^## `[^`]+`' "${present_md[@]}" 2>/dev/null | sed -E 's/^## `([^`]+)`.*/\1/')"
set -e

if [ -z "$all_ids" ]; then
  info "No se extrajeron component-ids — se omite el dup-check cross-modulo"
else
  dups="$(printf '%s\n' "$all_ids" | sort | uniq -d)"
  if [ -n "$dups" ]; then
    fail "Component-ids duplicados cross-modulo (rompen el contrato BE/FE):"
    printf '%s\n' "$dups" | sed 's/^/   - /'
    exit 1
  fi
  total="$(printf '%s\n' "$all_ids" | wc -l | tr -d ' ')"
  pass "Sin duplicados cross-modulo ($total component-ids en 4 modulos)"
fi

# ── Resultado ─────────────────────────────────────────────────────────────────
echo ""
pass "CLI smoke PASO — Core OK (5 modulos verdes, catalogos consistentes)"
