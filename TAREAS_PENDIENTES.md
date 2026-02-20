# 📋 TAREAS PENDIENTES - Framework Scotia QA

**Fecha:** 19 de Febrero 2026  
**Estado Actual:** ✅ BUILD SUCCESSFUL  
**Compilación:** Todos los módulos OK

---

## ✅ COMPLETADO (Último Sprint)

### Refactorización de Código:
- ✅ ApiHelper creado con 15 métodos (10 validaciones + 5 navegación de objetos)
- ✅ ValidationUtilities extendida con isPrimitiveOrWrapper()
- ✅ WebHelper extendido con 32 métodos nuevos
- ✅ 6 steps de ApiSteps refactorizados (1 línea cada uno)
- ✅ 2 steps de WebSteps refactorizados
- ✅ 30 steps genéricos de Web implementados
- ✅ 297 líneas de código duplicado eliminadas de ApiSteps

### Documentación:
- ✅ INVENTARIO_STEPS_GENERICOS.md creado
- ✅ STEPS_NEGOCIO_AUTOMOTOR.md creado
- ✅ RESUMEN_EJECUTIVO.md creado
- ✅ RESUMEN_REFACTORIZACION.md creado
- ✅ README.md actualizado

---

## 🚧 PENDIENTE CRÍTICO

### 1️⃣ Refactorizar Steps Restantes de ApiSteps (~40 steps) ⚠️ ALTA PRIORIDAD

**Steps con lógica que necesitan refactorización:**

```java
// ❌ ANTES (8-15 líneas con lógica)
@When("ejecuto una petición {string}")
public void ejecutoUnaPeticionAlEndpoint(String method, String endpoint) {
    try {
        // 10+ líneas de lógica
        getHttpClient().execute(method, endpoint);
        TestLogger.logInfo(...);
    } catch (Exception e) {
        throw new FrameworkTechnicalException(...);
    }
}

// ✅ DESPUÉS (1 línea limpia)
@When("ejecuto una petición {string}")
public void ejecutoUnaPeticionAlEndpoint(String method, String endpoint) 
    throws FrameworkTechnicalException {
    getApiHelper().executeRequest(method, endpoint);
}
```

**Steps a refactorizar:**
- [ ] `ejecutoUnaPeticionAlEndpoint` (10 líneas → 1)
- [ ] `serializoLaRespuestaEnLaClase` (15 líneas → 1)
- [ ] `guardoElObjetoSerializadoComo` (20 líneas → 1)
- [ ] `obtengoElCampoDelObjetoYLoGuardoComo` (30 líneas → 1)
- [ ] `agregoAutenticacionClientCredentials` (5 líneas → 1)
- [ ] `agregoAutenticacionBearerParaRUT` (6 líneas → 1)
- [ ] Otros ~35 steps más

**Métodos a agregar en ApiHelper:**
- [ ] `executeRequest(String method, String endpoint)`
- [ ] `deserializeResponse(String className)`
- [ ] `saveDeserializedObject(String variableName)`
- [ ] `extractFieldFromObject(String fieldName, String objectPath, String variableName)`
- [ ] `addClientCredentialsAuth()`
- [ ] `addBearerAuthForRut(String rut)`

**Estimado:** ~8-10 horas de trabajo

---

### 2️⃣ Aumentar Coverage de Tests ⚠️ ALTA PRIORIDAD

**Estado actual:**
```
common:       ~35% coverage ✅ (287 tests)
api-core:      0% coverage  ❌ (0 tests)
web-core:      0% coverage  ❌ (0 tests)  
mobile-core:   0% coverage  ❌ (0 tests)
TOTAL:        19% coverage  ⚠️
```

**Objetivo Sprint 2: 35% coverage total**

**Tests unitarios a crear:**

#### ApiHelper (15 tests):
- [ ] `testValidateResponseStatusCode_Success()`
- [ ] `testValidateResponseStatusCode_Failure()`
- [ ] `testValidateResponseContainsText_Found()`
- [ ] `testValidateResponseContainsText_NotFound()`
- [ ] `testValidateResponseSchema_Valid()`
- [ ] `testValidateResponseSchema_Invalid()`
- [ ] `testConfigureEndpointFromConfig_Success()`
- [ ] `testConfigureEndpointFromConfig_MissingBaseUrl()`
- [ ] `testAddBasicAuthentication_Success()`
- [ ] `testAddBearerToken_Success()`
- [ ] `testResolveObjectPath_SimplePath()`
- [ ] `testResolveObjectPath_NestedPath()`
- [ ] `testExtractFieldValue_FromMap()`
- [ ] `testExtractFieldValue_FromPojo()`
- [ ] `testFindObjectInStructure_Recursive()`

#### WebHelper validaciones nuevas (30 tests):
- [ ] `testValidateFieldAcceptsOnlyNumbers_Valid()`
- [ ] `testValidateFieldAcceptsOnlyNumbers_Invalid()`
- [ ] `testValidatePhoneFormat_ValidUruguay()`
- [ ] `testValidatePhoneFormat_ValidChile()`
- [ ] `testValidateFieldMatchesPattern_Valid()`
- [ ] `testValidateMinLength_Valid()`
- [ ] `testValidateMaxLength_Valid()`
- [ ] `testValidatePlaceholder_Correct()`
- [ ] `testValidateTooltip_Correct()`
- [ ] `testValidateMessageIsVisible_Present()`
- [ ] Otros 20 tests más...

**Estimado:** ~12-15 horas de trabajo

---

### 3️⃣ Agregar Steps Genéricos para API-CORE 📝 MEDIA PRIORIDAD

**Steps genéricos faltantes (~20 steps):**

```gherkin
# Validaciones de JSON
Then el campo "{string}" en la respuesta debe existir
Then el campo "{string}" en la respuesta debe ser de tipo "{string}"
Then el campo "{string}" en la respuesta debe tener el valor "{string}"
Then el array "{string}" en la respuesta debe contener {int} elementos
Then el array "{string}" en la respuesta no debe estar vacío

# Validaciones de headers
Then el header "{string}" debe existir
Then el header "{string}" debe tener el valor "{string}"
Then el header "{string}" debe contener "{string}"

# Validaciones de respuesta
Then el tiempo de respuesta debe ser menor a {int} milisegundos
Then el cuerpo de la respuesta no debe estar vacío
Then el cuerpo de la respuesta debe ser un JSON válido
Then el cuerpo de la respuesta debe ser un array

# Validaciones numéricas en JSON
Then el campo "{string}" debe ser mayor a {int}
Then el campo "{string}" debe ser menor a {int}
Then el campo "{string}" debe estar entre {int} y {int}

# Validaciones de strings en JSON
Then el campo "{string}" debe tener longitud mínima de {int}
Then el campo "{string}" debe tener longitud máxima de {int}
Then el campo "{string}" debe coincidir con el patrón "{string}"
```

**Estimado:** ~6-8 horas de trabajo

---

### 4️⃣ Limpiar Imports No Usados 🧹 BAJA PRIORIDAD

**ApiSteps.java tiene imports no usados:**
- [ ] `import java.lang.reflect.Field;` ❌
- [ ] `import java.lang.reflect.Method;` ❌
- [ ] `import java.util.Base64;` ❌

**WebSteps.java revisar imports:**
- [ ] Verificar si hay imports no usados

**Estimado:** 15 minutos

---

### 5️⃣ Pipeline - Resolver Errores de Quality Gate 🚨 ALTA PRIORIDAD

**Errores actuales en Jenkins:**

#### A. Error de redondeo en cálculo de tasa de éxito:
```groovy
// ❌ ERROR: BigDecimal.round() no acepta Integer
def successRate = (passedTests / totalTests * 100).round(2)

// ✅ SOLUCIÓN: Usar setScale() con RoundingMode
import java.math.RoundingMode
def successRate = (passedTests / totalTests * 100).setScale(2, RoundingMode.HALF_UP)
```

#### B. Error en integración con librería pipeline-utils:
```
❌ Error: Failed to determinate kind to register in project.json
```

**Causa:** La app `qaauy` no está registrada en `projects.json` de pipeline-utils

**Soluciones:**
- [ ] Opción A: Registrar app en projects.json (requiere ticket)
- [ ] Opción B: Hacer que codeQuality funcione sin registro
- [ ] Opción C: Deshabilitar temporalmente codeQuality hasta registrar

#### C. Coverage bajo (19%) vs objetivo (30%):
```
⚠️ Coverage: 19% (objetivo: 30%)
⚠️ Branch coverage: 19% (objetivo: 25%)
```

**Solución:** Agregar tests unitarios (ver punto 2)

**Estimado:** 4-6 horas

---

### 6️⃣ Consolidar Documentación 📚 BAJA PRIORIDAD

**Documentos actuales en raíz:**
- ✅ README.md (principal)
- ✅ PIPELINE-GUIA-COMPLETA.md (pipeline)
- ✅ INVENTARIO_STEPS_GENERICOS.md (steps)
- ✅ STEPS_NEGOCIO_AUTOMOTOR.md (negocio)
- ✅ RESUMEN_EJECUTIVO.md (resumen)
- ⚠️ RESUMEN_REFACTORIZACION.md (redundante)
- ⚠️ flujoGit.md (puede ir en README)

**Acciones:**
- [ ] Consolidar flujoGit.md en README sección "Flujo de Trabajo Git"
- [ ] Consolidar RESUMEN_REFACTORIZACION.md en INVENTARIO_STEPS_GENERICOS.md
- [ ] Eliminar documentos redundantes
- [ ] Dejar solo: README, PIPELINE-GUIA-COMPLETA, INVENTARIO_STEPS_GENERICOS, STEPS_NEGOCIO_AUTOMOTOR

**Estimado:** 1 hora

---

### 7️⃣ Generar Features Cucumber Completas 🎭 MEDIA PRIORIDAD

**Estado actual:**
- ✅ 30 steps genéricos implementados
- ✅ ~95/118 escenarios ejecutables (81%)
- ⚠️ Features en directorio temporal (no en proyecto)

**Tareas:**
- [ ] Crear directorio `features-examples/` en raíz
- [ ] Generar 10-15 features ejemplo usando steps genéricos:
  - [ ] `formulario-registro.feature` - Validación de formularios
  - [ ] `validaciones-campo.feature` - Validaciones de tipo/formato
  - [ ] `api-usuarios.feature` - CRUD de usuarios
  - [ ] `api-autenticacion.feature` - Auth flows
  - [ ] `navegacion-web.feature` - Navegación y UX
  - [ ] `mensajes-error.feature` - Manejo de errores
  - [ ] `dropdowns-opciones.feature` - Selección múltiple
  - [ ] `multi-pais.feature` - Validaciones por país
- [ ] Documentar en README cómo ejecutar features

**Estimado:** 3-4 horas

---

### 8️⃣ Refactorizar Steps Viejos de WebSteps (Opcional) 🔄 BAJA PRIORIDAD

**Steps con 3-5 líneas que podrían limpiarse:**
- [ ] `seleccionoElTextoEnElCombobox` (4 líneas → 2)
- [ ] `esperarHastaQueElementoEsteVisible` (2 líneas → 1)
- [ ] `esperarUnTiempo` (4 líneas → 2)
- [ ] `cambioIFramePath` (2 líneas → 1)
- [ ] Otros ~10 steps

**Beneficio:** Código más limpio y consistente  
**Riesgo:** Bajo (solo encapsular logging)  
**Estimado:** 2-3 horas

---

### 9️⃣ Implementar Reportes Mejorados 📊 MEDIA PRIORIDAD

**Problemas actuales:**
```
⚠️ No se generan reportes HTML de coverage para api-core, web-core, mobile-core
⚠️ Solo common genera reporte en build/reports/jacoco/test/html/index.html
```

**Tareas:**
- [ ] Configurar JaCoCo para api-core
- [ ] Configurar JaCoCo para web-core
- [ ] Configurar JaCoCo para mobile-core
- [ ] Agregar reporte consolidado multi-módulo
- [ ] Integrar con SonarQube si está disponible

**Archivos a modificar:**
- [ ] `api-core/build.gradle`
- [ ] `web-core/build.gradle`
- [ ] `mobile-core/build.gradle`
- [ ] `config/conf.codeCoverage.gradle` (ya existe)

**Estimado:** 2 horas

---

### 🔟 Limpiar Directorios de Evidencias (Opcional) 🧹 BAJA PRIORIDAD

**Directorios actuales:**
```
common/
├── custom-evidences/  ⚠️ Creado en runtime
├── test-evidences/    ⚠️ Creado en runtime
├── custom-logs/       ⚠️ Creado en runtime
└── logs/              ⚠️ Creado en runtime
```

**Opciones:**
- [ ] Opción A: Eliminar directorios y configurar para que NO se creen
- [ ] Opción B: Moverlos a directorio temporal del sistema
- [ ] Opción C: Agregar a .gitignore y dejar que se creen

**Estimado:** 1 hora

---

## 📊 RESUMEN DE PRIORIDADES

### 🔴 ALTA PRIORIDAD (Hacer primero):

1. **Resolver errores de Pipeline Jenkins** (4-6h)
   - Arreglar error BigDecimal.round()
   - Resolver integración con pipeline-utils
   - Desbloquear publicación a Artifactory

2. **Aumentar Coverage a 35%** (12-15h)
   - Tests unitarios para ApiHelper (15 tests)
   - Tests unitarios para WebHelper nuevos (30 tests)
   - Objetivo: Pasar Quality Gate

3. **Refactorizar steps restantes de ApiSteps** (8-10h)
   - ~40 steps con try-catch a limpiar
   - Agregar métodos en ApiHelper

---

### 🟡 MEDIA PRIORIDAD (Después):

4. **Agregar steps genéricos de API** (6-8h)
   - 20 steps nuevos para validaciones JSON
   - Headers, arrays, tipos de datos

5. **Configurar reportes JaCoCo** (2h)
   - Habilitar en api-core, web-core, mobile-core
   - Reporte consolidado

6. **Generar features Cucumber ejemplo** (3-4h)
   - 10-15 features con steps genéricos
   - Documentación de uso

---

### 🟢 BAJA PRIORIDAD (Cuando haya tiempo):

7. **Limpiar imports no usados** (15min)
8. **Consolidar documentación** (1h)
9. **Refactorizar steps viejos de WebSteps** (2-3h)
10. **Limpiar directorios de evidencias** (1h)

---

## 📈 ROADMAP ESTIMADO

### Sprint 2 (Semana 1-2):
- ✅ Resolver errores de pipeline
- ✅ Aumentar coverage a 35%
- ✅ Refactorizar 50% de steps de ApiSteps

### Sprint 3 (Semana 3-4):
- ✅ Refactorizar 100% de steps de ApiSteps
- ✅ Agregar 20 steps genéricos de API
- ✅ Configurar reportes JaCoCo

### Sprint 4 (Semana 5-6):
- ✅ Aumentar coverage a 55%
- ✅ Generar features ejemplo
- ✅ Consolidar documentación

### Sprint 5 (Semana 7-8):
- ✅ Coverage objetivo: 70%
- ✅ Refactorización opcional de WebSteps
- ✅ Pulido final

---

## 🎯 MÉTRICAS OBJETIVO

### Coverage:
```
Sprint 1: 19% → 35%  ✅ +16%
Sprint 2: 35% → 45%  🎯 +10%
Sprint 3: 45% → 55%  🎯 +10%
Sprint 4: 55% → 70%  🎯 +15%
```

### Steps Disponibles:
```
Actual:   ~110 steps
Sprint 2: ~130 steps (+20 API)
Sprint 3: ~150 steps (+20 avanzados)
Final:    ~170 steps 🎯
```

### Código Limpio:
```
Actual:   ~60% de steps limpios
Sprint 2: ~80% de steps limpios
Final:    100% de steps limpios 🎯
```

---

## ✅ PRIMEROS PASOS RECOMENDADOS

### Hacer AHORA (Próxima Sesión):

1. **Arreglar error BigDecimal en pipeline.jenkins** (15min)
   ```groovy
   // Línea 885 - Cambiar:
   .round(2) 
   // Por:
   .setScale(2, java.math.RoundingMode.HALF_UP)
   ```

2. **Crear primer test de ApiHelper** (30min)
   ```java
   // ApiHelperTest.java
   @Test
   void testValidateResponseStatusCode_Success() {
       // Arrange
       HttpResponse response = mock(HttpResponse.class);
       when(response.getStatusCode()).thenReturn(200);
       
       // Act & Assert
       apiHelper.validateResponseStatusCode(200); // No exception
   }
   ```

3. **Refactorizar 1 step complejo de ApiSteps** (20min)
   - Elegir `ejecutoUnaPeticionAlEndpoint`
   - Mover lógica a ApiHelper
   - Verificar que compile

**Total:** ~1 hora para desbloquear progreso

---

## 📞 DECISIONES PENDIENTES

### ¿Qué hacer con directorios de evidencias?
- ❓ ¿Eliminar custom-evidences, test-evidences, custom-logs, logs?
- ❓ ¿O solo agregar a .gitignore?

### ¿Registrar app en projects.json?
- ❓ ¿Crear ticket para DevOps?
- ❓ ¿O deshabilitar codeQuality temporalmente?

### ¿Generar features en proyecto o separado?
- ❓ ¿Crear `/features-examples` en raíz?
- ❓ ¿O mantener separado del framework?

---

**Última actualización:** 19 de Febrero 2026  
**Estado:** ✅ Análisis completo - Listo para próximo sprint

