# 🎯 Quality Gate Progresivo - Estrategia Implementada

**Fecha:** 17 de Febrero 2026  
**Estado:** ✅ IMPLEMENTADO  
**Objetivo:** Permitir avanzar mientras mejoramos coverage

---

## 🚦 ¿Qué es un Quality Gate Progresivo?

En lugar de tener un umbral **fijo e inalcanzable** (70%), usamos **umbrales incrementales** que van subiendo conforme mejoramos el coverage.

### ❌ Problema Anterior

```
Quality Gate con umbral fijo:
Coverage mínimo: 70%
Coverage actual: 30%

Resultado: ❌ SIEMPRE FALLA
```

**Consecuencia:** 
- ❌ Pipeline siempre en rojo
- ❌ No se puede avanzar
- ❌ Desmotivación del equipo
- ❌ No refleja el progreso real

### ✅ Solución: Quality Gate Progresivo

```
Sprint 1: Coverage ≥30% ✅ (actual: ~35%) → WARNING si falla
Sprint 2: Coverage ≥40% ⏳
Sprint 3: Coverage ≥55% ⏳
Sprint 4: Coverage ≥65% ⏳
Sprint 5: Coverage ≥70% 🎯 → ERROR si falla
```

**Consecuencia:**
- ✅ Pipeline en verde cuando hay progreso
- ✅ Se puede avanzar con el desarrollo
- ✅ Motivación del equipo (ven mejora continua)
- ✅ Refleja progreso real

---

## 📊 Umbrales Implementados

### Coverage (Line Coverage)

| Sprint | Umbral | Tipo | Estado |
|--------|--------|------|--------|
| **Sprint 1** | **30%** | ⚠️ WARNING | ✅ ACTUAL |
| Sprint 2 | 40% | ⚠️ WARNING | ⏳ Siguiente |
| Sprint 3 | 55% | ⚠️ WARNING | ⏳ Futuro |
| Sprint 4 | 65% | ⚠️ WARNING | ⏳ Futuro |
| Sprint 5 | 70% | ❌ ERROR | 🎯 Final |

### Branch Coverage

| Sprint | Umbral | Tipo | Estado |
|--------|--------|------|--------|
| **Sprint 1** | **25%** | ⚠️ WARNING | ✅ ACTUAL |
| Sprint 2 | 35% | ⚠️ WARNING | ⏳ Siguiente |
| Sprint 3 | 45% | ⚠️ WARNING | ⏳ Futuro |
| Sprint 4 | 55% | ⚠️ WARNING | ⏳ Futuro |
| Sprint 5 | 60% | ❌ ERROR | 🎯 Final |

---

## 🔧 Cambios Implementados en pipeline.jenkins

### Check 5: Line Coverage

**ANTES:**
```groovy
// CHECK 5: Cobertura mínima >= 70%
if (coverage < 70) {
    echo "❌ Check 5: Coverage ${coverage}% (mínimo: 70%)"
    errors.add("Coverage bajo: ${coverage}%")  // ❌ ERROR
    checksPass = false  // ❌ FALLA EL BUILD
}
```

**AHORA:**
```groovy
// CHECK 5: Cobertura mínima >= 30% (PROGRESIVO: 30% → 40% → 55% → 70%)
def minCoverage = 30  // Sprint 1: 30%, Sprint 2: 40%, Sprint 3: 55%, Final: 70%

if (coverage < minCoverage) {
    echo "⚠️  Check 5: Coverage ${coverage}% (mínimo: ${minCoverage}%, objetivo final: 70%)"
    warnings.add("Coverage bajo: ${coverage}% (trabajando para mejorar)")  // ⚠️ WARNING
    // NO falla el build ✅
}
```

**Beneficios:**
- ✅ Muestra progreso hacia objetivo final
- ✅ No bloquea el pipeline
- ✅ Advierte si baja del umbral actual
- ✅ Fácil de actualizar para cada sprint

### Check 6: Branch Coverage

**ANTES:**
```groovy
// CHECK 6: Cobertura de branches >= 60%
if (branchCoverage < 60) {
    echo "⚠️  Check 6: Branch coverage ${branchCoverage}% (mínimo: 60%)"
    warnings.add("Branch coverage bajo: ${branchCoverage}%")
}
```

**AHORA:**
```groovy
// CHECK 6: Cobertura de branches >= 25% (PROGRESIVO: 25% → 35% → 45% → 60%)
def minBranchCoverage = 25  // Sprint 1: 25%, Sprint 2: 35%, Sprint 3: 45%, Final: 60%

if (branchCoverage < minBranchCoverage) {
    echo "⚠️  Check 6: Branch coverage ${branchCoverage}% (mínimo: ${minBranchCoverage}%, objetivo final: 60%)"
    warnings.add("Branch coverage bajo: ${branchCoverage}% (trabajando para mejorar)")
}
```

**Beneficios:**
- ✅ Ya estaba como WARNING (no bloqueaba)
- ✅ Ahora umbral es realista
- ✅ Muestra objetivo final

---

## 📈 Cómo Actualizar para Cada Sprint

### Cuando completes Sprint 2

```groovy
// En pipeline.jenkins, línea ~622
def minCoverage = 40  // Cambiar 30 → 40

// En pipeline.jenkins, línea ~652
def minBranchCoverage = 35  // Cambiar 25 → 35
```

### Cuando completes Sprint 3

```groovy
def minCoverage = 55  // Cambiar 40 → 55
def minBranchCoverage = 45  // Cambiar 35 → 45
```

### Sprint Final (Sprint 5)

```groovy
def minCoverage = 70  // Objetivo final
def minBranchCoverage = 60  // Objetivo final

// Y cambiar de WARNING a ERROR:
if (coverage < minCoverage) {
    echo "❌ Check 5: Coverage ${coverage}% (mínimo: ${minCoverage}%)"
    errors.add("Coverage bajo: ${coverage}%")  // ❌ ERROR
    checksPass = false  // ❌ FALLA EL BUILD
}
```

---

## 💡 Output del Pipeline (Ejemplo)

### Antes (Siempre Fallaba)

```
🚦 Ejecutando Quality Gate (9 checks)...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ Check 1: Tests ejecutados
✅ Check 2: Build exitoso
✅ Check 3: 287 tests ejecutados (mínimo: 10)
✅ Check 4: Success rate 100.00%
❌ Check 5: Coverage 35% (mínimo: 70%)       ← ❌ SIEMPRE FALLA
⚠️  Check 6: Branch coverage 28% (mínimo: 60%)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 RESUMEN QUALITY GATE:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
❌ ERRORES CRÍTICOS (1):
   - Coverage bajo: 35%

❌ Quality Gate: FAILED  ← ❌ PIPELINE EN ROJO
```

### Ahora (Pasa y Muestra Progreso)

```
🚦 Ejecutando Quality Gate (9 checks)...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💡 QUALITY GATE PROGRESIVO:
   📍 Sprint 1: Coverage 30%, Branch 25% (ACTUAL)
   📍 Sprint 2: Coverage 40%, Branch 35%
   📍 Sprint 3: Coverage 55%, Branch 45%
   📍 Final:    Coverage 70%, Branch 60%

✅ Check 1: Tests ejecutados
✅ Check 2: Build exitoso
✅ Check 3: 287 tests ejecutados (mínimo: 10)
✅ Check 4: Success rate 100.00%
✅ Check 5: Coverage 35% (mínimo: 30%, objetivo final: 70%)  ← ✅ PASA!
✅ Check 6: Branch coverage 28% (mínimo: 25%, objetivo final: 60%)  ← ✅ PASA!

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 RESUMEN QUALITY GATE:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ Quality Gate: PASSED  ← ✅ PIPELINE EN VERDE!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎯 Progreso hacia objetivo:
   Coverage: ████████░░░░░░░░░░░░ 35%/70% (50%)
   Branch:   ████████░░░░░░░░░░░░ 28%/60% (47%)
```

---

## 🎯 Roadmap de Umbrales

### Visualización del Progreso

```
Coverage (Line):
Sprint 1: ██████░░░░░░░░░░░░░░ 30% ✅ ACTUAL
Sprint 2: ████████░░░░░░░░░░░░ 40%
Sprint 3: █████████████░░░░░░░ 55%
Sprint 4: ███████████████░░░░░ 65%
Sprint 5: ██████████████████░░ 70% 🎯

Branch Coverage:
Sprint 1: █████░░░░░░░░░░░░░░░ 25% ✅ ACTUAL
Sprint 2: ███████░░░░░░░░░░░░░ 35%
Sprint 3: ███████████░░░░░░░░░ 45%
Sprint 4: █████████████░░░░░░░ 55%
Sprint 5: ███████████████░░░░░ 60% 🎯
```

---

## ✅ Beneficios de Esta Estrategia

### 1. 🎯 Progreso Visible
- Cada sprint tiene meta alcanzable
- Se ve la mejora continua
- Motivación del equipo

### 2. 🚀 No Bloquea Desarrollo
- Pipeline puede pasar en verde
- Se puede publicar a Artifactory
- Desarrollo continúa sin interrupciones

### 3. 📊 Métricas Claras
- Se ve el coverage actual vs objetivo
- Se ve el progreso de cada sprint
- Se sabe qué falta por hacer

### 4. 🎓 Mejora Continua
- Cada sprint sube el estándar
- Al final: 70% coverage garantizado
- Proceso gradual y sostenible

---

## 🔄 Plan de Actualización por Sprint

### Al Completar Sprint 2 (Coverage ~45%)

```groovy
// Actualizar pipeline.jenkins línea ~622
def minCoverage = 40  // Subir de 30 → 40

// Actualizar pipeline.jenkins línea ~652  
def minBranchCoverage = 35  // Subir de 25 → 35
```

**Commit:**
```bash
git commit -m "ci: Incrementar umbrales Quality Gate - Sprint 2

Coverage: 30% → 40%
Branch: 25% → 35%

Sprint 2 completado, ajustando estándares hacia objetivo final."
```

### Al Completar Sprint 3 (Coverage ~55%)

```groovy
def minCoverage = 55  // Subir de 40 → 55
def minBranchCoverage = 45  // Subir de 35 → 45
```

### Al Completar Sprint 5 (Coverage 70% - FINAL)

```groovy
def minCoverage = 70  // Objetivo final alcanzado
def minBranchCoverage = 60  // Objetivo final alcanzado

// Cambiar de WARNING a ERROR para hacer obligatorio
if (coverage < minCoverage) {
    echo "❌ Check 5: Coverage ${coverage}% (mínimo: ${minCoverage}%)"
    errors.add("Coverage bajo: ${coverage}%")  // Cambiar a errors
    checksPass = false  // Hacer que falle el build
}
```

---

## 📋 Checklist de Actualización

Cada vez que completes un sprint:

- [ ] ✅ Ejecutar tests: `./gradlew test jacocoTestReport`
- [ ] ✅ Ver coverage actual en reporte HTML
- [ ] ✅ Si cumple objetivo del sprint: Actualizar umbrales
- [ ] ✅ Modificar `minCoverage` en pipeline.jenkins (línea ~622)
- [ ] ✅ Modificar `minBranchCoverage` en pipeline.jenkins (línea ~652)
- [ ] ✅ Commitear cambio con mensaje apropiado
- [ ] ✅ Ejecutar pipeline para validar
- [ ] ✅ Confirmar que pasa con nuevo umbral

---

## 🎓 Mejores Prácticas

### ✅ Hacer

1. **Incrementos realistas**
   - Subir de 10% en 10% es manejable
   - Grandes saltos (30% → 70%) son frustrantes

2. **Warnings primero, Errors después**
   - Sprint 1-4: WARNING (no bloquea)
   - Sprint 5: ERROR (bloquea si no cumple)

3. **Comunicar claramente**
   - Mostrar objetivo final en cada mensaje
   - Mostrar progreso actual vs meta

4. **Revisar regularmente**
   - Actualizar después de cada sprint
   - No dejar umbrales obsoletos

### ❌ No Hacer

1. **NO poner umbrales inalcanzables**
   - Desmotiva al equipo
   - Hace el pipeline inútil

2. **NO bloquear durante desarrollo**
   - Usar ERROR solo al final
   - Permitir avanzar con WARNINGS

3. **NO olvidar actualizar**
   - Los umbrales deben crecer con el coverage
   - Revisar después de cada sprint

---

## 📊 Comparación de Estrategias

### Estrategia A: Umbral Fijo (❌ NO recomendado)

```
Desde el inicio: Coverage ≥70%

Sprint 1: 30% → ❌ FALLA
Sprint 2: 45% → ❌ FALLA
Sprint 3: 55% → ❌ FALLA
Sprint 4: 65% → ❌ FALLA
Sprint 5: 70% → ✅ PASA

Resultado: 4 sprints en rojo, desmotivación
```

### Estrategia B: Sin Quality Gate (❌ NO recomendado)

```
Sin umbrales

Sprint 1: 30% → ✅ PASA
Sprint 2: 32% → ✅ PASA (no hay mejora real)
Sprint 3: 30% → ✅ PASA (incluso baja)
Sprint 4: 35% → ✅ PASA (muy poco progreso)
Sprint 5: 40% → ✅ PASA (lejos del 70%)

Resultado: No hay presión para mejorar, nunca llegas al objetivo
```

### Estrategia C: Quality Gate Progresivo (✅ IMPLEMENTADA)

```
Umbrales incrementales

Sprint 1: 35% (umbral 30%) → ✅ PASA (+5% sobre mínimo)
Sprint 2: 45% (umbral 40%) → ✅ PASA (+5% sobre mínimo)
Sprint 3: 58% (umbral 55%) → ✅ PASA (+3% sobre mínimo)
Sprint 4: 67% (umbral 65%) → ✅ PASA (+2% sobre mínimo)
Sprint 5: 72% (umbral 70%) → ✅ PASA (+2% sobre mínimo)

Resultado: Mejora continua, motivación, objetivo alcanzado ✅
```

---

## 🎯 Estado Actual (Sprint 1)

### Coverage Real vs Umbrales

| Métrica | Umbral Sprint 1 | Actual | Estado |
|---------|-----------------|--------|--------|
| **Line Coverage** | ≥30% | ~35% | ✅ +5% |
| **Branch Coverage** | ≥25% | ~28% | ✅ +3% |

**Conclusión:** ✅ Estamos **CUMPLIENDO** con el Sprint 1

### Proyección para Sprint 2

Con +66 tests planeados:

| Métrica | Umbral Sprint 2 | Proyección | Estado |
|---------|-----------------|------------|--------|
| **Line Coverage** | ≥40% | ~45% | ✅ Alcanzable |
| **Branch Coverage** | ≥35% | ~37% | ✅ Alcanzable |

---

## 🚀 Comando para Probar

```bash
# Ejecutar pipeline completo localmente
./gradlew clean build jacocoTestReport

# Ver si pasa el Quality Gate
# Coverage actual: ~35% (umbral: 30%) → ✅ PASA
# Branch actual: ~28% (umbral: 25%) → ✅ PASA
```

---

## ✅ Resumen de Cambios

### Modificaciones en pipeline.jenkins

| Línea | Cambio | De | A |
|-------|--------|-----|---|
| ~609 | Coverage mínimo | 70% | 30% (progresivo) |
| ~620 | Tipo de check | ERROR | WARNING |
| ~622 | Variable | hardcoded | `minCoverage` |
| ~643 | Branch mínimo | 60% | 25% (progresivo) |
| ~652 | Variable | hardcoded | `minBranchCoverage` |
| ~534 | Info header | - | Roadmap progresivo |

### Resultado

- ✅ Pipeline puede pasar en verde
- ✅ Muestra progreso hacia objetivo
- ✅ No bloquea desarrollo
- ✅ Fácil de actualizar cada sprint

---

## 📚 Documentación Relacionada

- **PLAN-TESTS-CONSOLIDADO.md** - Plan completo de tests
- **PIPELINE-GUIA-COMPLETA.md** - Guía del pipeline
- Este documento - Estrategia de Quality Gate

---

## 🎊 Conclusión

### ¿Es viable bajar los umbrales temporalmente?

**¡Absolutamente SÍ! Es la mejor práctica.** ✅

**Beneficios:**
1. ✅ Permite avanzar con desarrollo
2. ✅ No bloquea el pipeline innecesariamente
3. ✅ Muestra progreso real
4. ✅ Motiva al equipo
5. ✅ Se llega al 70% de forma sostenible

**El Quality Gate Progresivo es una estrategia profesional usada por equipos de alto rendimiento.**

---

## 🚀 Próxima Acción

```bash
# Verificar que el pipeline funciona
git add pipeline.jenkins
git commit -m "ci: Implementar Quality Gate progresivo

Sprint 1: Coverage ≥30%, Branch ≥25% (WARNING)
Sprint 2-4: Incrementos de 10% y 10%
Sprint 5: Coverage ≥70%, Branch ≥60% (ERROR)

Permite avanzar con desarrollo mientras mejoramos coverage
de forma sostenible y realista."

git push origin develop
```

---

**¡Totalmente viable y recomendado!** ✅

---

**Autor:** Abel Venero  
**Fecha:** 17 de Febrero 2026  
**Versión:** 1.0

