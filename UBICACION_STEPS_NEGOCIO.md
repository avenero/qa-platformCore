# 📍 UBICACIÓN DE STEPS DE NEGOCIO - QA Scotia Frameworks

**Fecha:** 19 de Febrero 2026  
**Autor:** Abel Venero  

---

## 🎯 RESUMEN EJECUTIVO

Los **steps de negocio** específicos de Uruguay/Scotia **NO están en ApiSteps.java** ✅  

Están ubicados en el módulo **`common/`** en el archivo:

📂 **`common/src/main/java/com/scotia/qa/common/database/repository/QueryRepository.java`**

---

## 📊 INVENTARIO COMPLETO DE STEPS DE NEGOCIO

### 📍 UBICACIÓN: `QueryRepository.java` (Módulo: common)

#### **🗄️ STEPS DE DATABASE (Comentados - líneas 149-181):**
```java
/* COMENTADOS - NO ACTIVOS
@When("actualizo los valores en la base de datos DB2 segun la consulta")
@When("consulto la base de datos segun el parametro {string}")  
@When("consulto la base de datos {string} segun el parametro {string}")
@When("elimino uno o mas registros en {string}")
@When("consulto la base de datos en {string}")
@When("actualizo el o los registros en la base de datos en {string}")
*/
```
**Estado:** ❌ Comentados (métodos llamados no existen)  
**Total:** 6 steps de DB

---

#### **✅ STEPS ACTIVOS DE DATABASE (líneas 182-201):**
```java
@When("inserto uno o mas registros en {string}")                              // Línea 182
@When("recorro la respuesta buscando que se cumpla que {string} sea igual...") // Línea 187
@When("adjunto un archivo al scenario con la data")                           // Línea 193
@When("espero {string} segundos")                                             // Línea 198
```
**Estado:** ⚠️ ACTIVOS (llaman a métodos que pueden no existir)  
**Total:** 4 steps genéricos/utilidad

---

#### **✅ STEPS ACTIVOS DE VALIDACIÓN (líneas 204-279):**
```java
@Then("valido que el codigo de respuesta del servicio sea {int}")             // Línea 205
@Then("valido que el status del response sea {string}")                       // Línea 210
@Then("valido que el valor del campo {string} sea {string}")                  // Línea 215
@Then("valido que el valor almacenado en el campo {string} sea {string}")     // Línea 220
@Then("compruebo que se registre correctamente en MIS dado el parametro...")  // Línea 225
@Then("valido que el cuerpo de la respuesta sea")                             // Línea 230
@Then("valido que el valor dentro de la estructura {string} sea {string}")    // Línea 235
@Then("valido que el cuerpo de la respuesta contenga la siguiente cadena")    // Línea 241
@Then("valido que el cuerpo de la respuesta no contenga la siguiente cadena") // Línea 246
@Then("valido que el valor de la variable {string} sea {string}")             // Línea 251
@Then("valido que la fecha almacenada en el campo {string} sea {string}")     // Línea 262
@Then("valido que lo almacenado en el campo {string} sea nulo")               // Línea 267
@Then("verifico que la consulta este vacia")                                  // Línea 272
```
**Estado:** ⚠️ ACTIVOS (algunos pueden ser genéricos, otros de negocio)  
**Total:** 13 steps de validación

---

#### **🏦 STEPS DE NEGOCIO ESPECÍFICO (líneas 281-324):**
```java
// NEGOCIO: Homebanking Uruguay
@Given("que busco un documento que no exista en la bbdd de homebanking y guardo en {string}")
  → Línea 281-283

// NEGOCIO: Extracción de datos
@Then("obtengo el anio y el mes de {string} y lo guardo en las variables anio y mes")
  → Línea 286-288

@Then("obtengo los ultimo {string} digitos de {string} y lo guardo en la variable {string}")
  → Línea 291-293

// NEGOCIO: Onboarding Uruguay
@Given("que busco un documento valido para onboardingUy con el host {string}")
  → Línea 296-298

// NEGOCIO: Topaz (Sistema cliente Scotia)
@Given("busco un documento que tenga un cliente existente en topaz")
  → Línea 301-303

@Given("busco un documento que tenga un cliente prospecto")
  → Línea 306-308

@Given("busco un documento que tenga un cliente casado")
  → Línea 311-313

@Given("busco un documento que tenga un cliente soltero")
  → Línea 316-318

// NEGOCIO: Apertura de cuentas
@Then("valido nivel de apertura")
  → Línea 321-323
```
**Estado:** ✅ ACTIVOS (llaman a métodos de negocio)  
**Total:** 9 steps de negocio específico

---

#### **🔧 STEPS DE UTILIDAD (línea 327-330):**
```java
@When("modifico la variable {string} agregando en el path {string} la siguiente data")
  → Línea 327-329
```
**Estado:** ⚠️ ACTIVO (llama a `putVariable` que NO existe)  
**Total:** 1 step con TODO

---

#### **🎫 STEPS DE JIRA (línea 257-260):**
```java
@Given("actualizo los casos de los escenarios que tienen el tag {string} y codigo de jira {string}")
  → Línea 257-259
```
**Estado:** ✅ ACTIVO (integración con Jira)  
**Total:** 1 step de integración

---

## 📋 RESUMEN POR CATEGORÍA

### **En QueryRepository.java (`common/`):**

| Categoría | Cantidad | Estado | Ubicación |
|-----------|----------|--------|-----------|
| 🗄️ Database (comentados) | 6 steps | ❌ Comentados | Líneas 149-181 |
| ✅ Database activos | 4 steps | ⚠️ Activos | Líneas 182-201 |
| ✅ Validación genérica | 13 steps | ⚠️ Activos | Líneas 204-279 |
| 🏦 **Negocio específico** | **9 steps** | ✅ Activos | **Líneas 281-324** |
| 🎫 Jira integración | 1 step | ✅ Activo | Línea 257-260 |
| 🔧 Utilidad (roto) | 1 step | ⚠️ Roto | Línea 327-330 |

**TOTAL:** 34 steps en QueryRepository.java

---

## 🚨 PROBLEMAS IDENTIFICADOS

### 1️⃣ **QueryRepository NO debería tener Steps de Cucumber**
- ❌ Es un **Repository** (patrón de datos)
- ❌ Mezclado con Steps de Cucumber
- ❌ Viola Single Responsibility Principle

### 2️⃣ **Steps de negocio en `common/`**
- ❌ `common/` debe ser **genérico** (framework base)
- ❌ Negocio específico (Topaz, Homebanking UY) debe estar en módulo aparte

### 3️⃣ **Steps duplicados/redundantes**
- `valido que el codigo de respuesta del servicio sea {int}` → YA existe en ApiSteps ✅
- `valido que el valor dentro de la estructura {string} sea {string}` → Validaciones genéricas

---

## ✅ RECOMENDACIONES

### **OPCIÓN A: Mover Steps a Módulo Específico** ⭐ RECOMENDADO
```
Crear: business-uy/
  └── src/main/java/com/scotia/qa/business/uy/steps/
      ├── HomebankingSteps.java (documentos HB)
      ├── OnboardingSteps.java (onboarding UY)
      ├── TopazSteps.java (clientes Topaz)
      └── AccountSteps.java (apertura, nivel)
```

### **OPCIÓN B: Separar QueryRepository**
```
common/database/repository/
  ├── QueryRepository.java (solo métodos genéricos)
  └── DatabaseSteps.java (steps de DB genéricos)
```

### **OPCIÓN C: Comentar temporalmente**
```java
// Comentar todos los steps de negocio en QueryRepository
// Moverlos gradualmente a módulos específicos
```

---

## 📦 ESTRUCTURA ACTUAL

```
qa-scotia-frameworks/
├── api-core/
│   └── steps/ApiSteps.java ✅ LIMPIO (solo steps genéricos)
│
├── web-core/
│   └── steps/WebSteps.java ✅ LIMPIO (solo steps genéricos)
│
├── mobile-core/
│   └── steps/MobileSteps.java ✅ LIMPIO (solo steps genéricos)
│
└── common/
    ├── database/repository/
    │   └── QueryRepository.java ⚠️ MEZCLADO (infraestructura + steps + negocio)
    │
    └── utils/testdata/steps/
        └── UserFinderSteps.java ✅ GENÉRICO (busca usuarios de test)
```

---

## 🎯 PRÓXIMA ACCIÓN RECOMENDADA

### **REFACTORIZAR QueryRepository.java:**

**1. Separar responsabilidades (2h):**
```
QueryRepository.java → Solo métodos de infraestructura genérica
DatabaseSteps.java → Steps genéricos de DB
BusinessStepsUY.java → Steps de negocio UY específico
```

**2. Mover steps de negocio (1h):**
```
9 steps de negocio → Módulo business-uy/
1 step de Jira → JiraSteps.java (common/integration/)
```

**3. Comentar steps rotos (15min):**
```
1 step "modifico la variable..." → Comentar (putVariable no existe)
6 steps de DB comentados → Eliminar
```

---

## 📝 CONCLUSIÓN

### ✅ **ApiSteps.java ESTÁ LIMPIO:**
- ✅ Solo 499 líneas
- ✅ Solo steps genéricos
- ✅ Todos refactorizados y funcionando
- ✅ **NO tiene steps de negocio**

### ⚠️ **QueryRepository.java NECESITA LIMPIEZA:**
- ⚠️ 576 líneas mezcladas
- ⚠️ 9 steps de negocio UY específico
- ⚠️ 6 steps comentados con métodos faltantes
- ⚠️ 1 step roto (`putVariable`)

**Archivo a trabajar:** `common/src/main/java/com/scotia/qa/common/database/repository/QueryRepository.java`

---

**Estado:** ANÁLISIS COMPLETO  
**Siguiente tarea:** Refactorizar QueryRepository.java o crear módulo business-uy

