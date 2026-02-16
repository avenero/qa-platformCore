# 📦 PARÁMETRO PUBLISH_TO_MAVEN_LOCAL - GUÍA COMPLETA

**Fecha:** 2025-02-15  
**Propósito:** Explicación detallada del parámetro opcional de publicación en Maven Local

---

## 🎯 ¿QUÉ ES Y PARA QUÉ SIRVE?

### **Definición:**

`PUBLISH_TO_MAVEN_LOCAL` es un parámetro **opcional** del pipeline que permite publicar el framework en Maven Local **solo en la rama develop** para facilitar testing antes de publicar a Artifactory.

---

## ✅ CASOS DE USO

### **Escenario 1: Testing del Framework Antes de Release**

```
Situación:
  Developer A agregó nueva funcionalidad en develop
  Developer B quiere probar esa funcionalidad en su módulo de prueba
  NO hay release oficial aún en Artifactory

Solución:
  1. Jenkins (develop): Build with Parameters
  2. PUBLISH_TO_MAVEN_LOCAL: YES ✅
  3. Jenkins publica en Maven Local del servidor
  4. Developer B ejecuta módulo de prueba que usa Maven Local
  5. ✅ Puede probar funcionalidad sin esperar a release
```

---

### **Escenario 2: Validación de Breaking Changes**

```
Situación:
  Tech Lead hizo cambio mayor en develop
  Quiere verificar impacto en módulos consumidores
  
Solución:
  1. Jenkins (develop): PUBLISH_TO_MAVEN_LOCAL=YES
  2. Developer corre módulo de prueba con versión local
  3. Detecta problemas de compatibilidad
  4. Corrige antes de publicar a master
```

---

## 🚫 CUÁNDO NO USARLO

### **NO usar si:**

- ✅ Solo quieres validar que compile (default: NO publicar es más rápido)
- ✅ No necesitas probar en módulos consumidores aún
- ✅ Estás en rama master (ignorado, solo publica a Artifactory)
- ✅ Es un merge automático sin testing manual

**Default:** `false` (NO publica, pipeline ~1 minuto más rápido)

---

## ⚙️ CONFIGURACIÓN EN PIPELINE.JENKINS

### **Declaración del Parámetro:**

```groovy
parameters {
    choice(
        name: 'PUBLISH_TO_ARTIFACTORY',
        choices: ['AUTO', 'YES', 'NO'],
        description: '''Publicar a Artifactory:
• AUTO: Solo si es rama master (recomendado)
• YES: Forzar publicación ⚠️
• NO: Solo compilar y testear'''
    )
    
    // 🆕 PARÁMETRO OPCIONAL PARA MAVEN LOCAL
    booleanParam(
        name: 'PUBLISH_TO_MAVEN_LOCAL',
        defaultValue: false,  // ← Por defecto NO publica (más rápido)
        description: '''📦 Publicar a Maven Local (opcional):
• Solo disponible en rama develop
• Útil para testing local del framework
• NO afecta Artifactory
• Agrega ~1 minuto al pipeline'''
    )
    
    string(
        name: 'CUSTOM_VERSION',
        defaultValue: '',
        description: '''Versión personalizada (OPCIONAL):
• Vacío: Auto-calculada desde Artifactory
• Manual: Para hotfixes/RCs'''
    )
    
    booleanParam(
        name: 'SKIP_TESTS',
        defaultValue: false,
        description: '⚠️ Saltar tests (NO recomendado)'
    )
}
```

---

### **Stage de Publicación a Maven Local:**

```groovy
stage('📦 Publicar a Maven Local') {
    when {
        allOf {
            branch 'develop'  // ← Solo en develop
            expression { params.PUBLISH_TO_MAVEN_LOCAL == true }  // ← Solo si usuario lo activa
        }
    }
    steps {
        script {
            echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
            echo '📦 Publicando a Maven Local (opcional - solo testing)...'
            echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
            echo "📌 Versión: ${env.VERSION}"
            echo "📂 Destino: ~/.m2/repository/com/scotia/qa/"
            echo "🎯 Propósito: Testing del framework antes de release"
        }
        
        sh './gradlew publishToMavenLocal'
        
        script {
            echo '✅ Publicado en Maven Local del servidor Jenkins'
            echo "📦 Módulos publicados: ${env.MODULES}"
            echo '⚠️  NOTA: Esto NO afecta Artifactory'
            echo '💡 TIP: Developers pueden usar esta versión para testing'
        }
    }
}
```

---

### **Stage de Publicación a Artifactory (sin cambios):**

```groovy
stage('🚀 Publicar a Artifactory') {
    when {
        allOf {
            branch pattern: "main|master", comparator: "REGEXP"  // ← Solo en master
            expression { env.WILL_PUBLISH == 'true' }
        }
    }
    steps {
        script {
            echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
            echo "🚀 Publicando versión ${env.VERSION} a Artifactory..."
            echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
            echo "📁 Repositorio: ${env.ARTIFACTORY_RELEASE_REPO}"
            echo "🌐 URL: ${env.ARTIFACTORY_URL}/${env.ARTIFACTORY_RELEASE_REPO}"
            echo '⚠️  RELEASE = INMUTABLE (no se puede sobrescribir)'
        }
        
        sh """
            ./gradlew publish \
                -Pversion=${env.VERSION} \
                -PartifactoryUrl=${env.ARTIFACTORY_URL}/${env.ARTIFACTORY_RELEASE_REPO} \
                -PartifactoryUser=${env.ARTIFACTORY_CREDS_USR} \
                -PartifactoryPassword=${env.ARTIFACTORY_CREDS_PSW}
        """
        
        script {
            echo '✅ PUBLICACIÓN EXITOSA'
            echo "📦 Versión ${env.VERSION} en Artifactory"
            echo '🔒 Versión INMUTABLE'
        }
    }
}
```

---

## 🔄 COMPORTAMIENTO POR RAMA

### **En rama DEVELOP:**

#### **Default (PUBLISH_TO_MAVEN_LOCAL = false):**

```
Pipeline ejecuta:
├── Build ✅
├── Tests ✅
├── Coverage ✅
├── CVE Scan ✅
├── Quality Gate ✅
├── Artefactos ✅
├── Stage "Publicar a Maven Local" → SKIPPED ⏭️ (más rápido)
└── Stage "Publicar a Artifactory" → SKIPPED ⏭️

Tiempo: ~8-10 minutos
```

#### **Opcional (PUBLISH_TO_MAVEN_LOCAL = true):**

```
Pipeline ejecuta:
├── Build ✅
├── Tests ✅
├── Coverage ✅
├── CVE Scan ✅
├── Quality Gate ✅
├── Artefactos ✅
├── Stage "Publicar a Maven Local" → EJECUTA ✅ (~1 min)
└── Stage "Publicar a Artifactory" → SKIPPED ⏭️

Tiempo: ~9-11 minutos (1 min más)
```

**Resultado:**
```
~/.m2/repository/com/scotia/qa/
├── common/1.0.5-SNAPSHOT/
├── api-core/1.0.5-SNAPSHOT/
├── web-core/1.0.5-SNAPSHOT/
└── mobile-core/1.0.5-SNAPSHOT/
```

---

### **En rama MASTER:**

```
Pipeline ejecuta:
├── Build ✅
├── Tests ✅
├── Calcular versión (consulta Artifactory) ✅
├── Verificar duplicados ✅
├── Aprobar publicación ✅ (manual)
├── Stage "Publicar a Maven Local" → SKIPPED ⏭️ (no aplica en master)
└── Stage "Publicar a Artifactory" → EJECUTA ✅

Tiempo: ~10-12 minutos
```

**NOTA:** En master, `PUBLISH_TO_MAVEN_LOCAL` se **ignora** (siempre publica a Artifactory).

---

## 📊 COMPARATIVA: DEFAULT vs OPCIONAL

| Aspecto | Default (NO publicar) | Opcional (Publicar Maven Local) |
|---------|----------------------|--------------------------------|
| **Tiempo de pipeline** | 8-10 min ⚡ | 9-11 min |
| **Propósito** | Validación rápida | Testing del framework |
| **Cuando usar** | Merge normal a develop | Testing antes de release |
| **Impacto en Artifactory** | ❌ Ninguno | ❌ Ninguno |
| **Útil para** | Feedback rápido | Validación manual |
| **Frecuencia recomendada** | 90% de los casos | 10% (cuando se necesita) |

---

## 🎯 EJEMPLOS DE USO REAL

### **Ejemplo 1: Merge normal (default)**

```
Developer hace PR: feature/login-fix → develop
Tech Lead aprueba y hace merge

Jenkins ejecuta AUTOMÁTICAMENTE:
├── PUBLISH_TO_ARTIFACTORY: AUTO (interpreta como NO en develop)
├── PUBLISH_TO_MAVEN_LOCAL: false (default)
└── Resultado:
    ├── Build ✅
    ├── Tests ✅
    ├── Coverage ✅
    └── NO publica (SKIPPED)

Tiempo: 8 minutos
Feedback: Developer recibe ✅ en <10 min
```

---

### **Ejemplo 2: Testing manual del framework**

```
Developer agregó nueva funcionalidad importante
Quiere que otros puedan probarla antes de release

Jenkins: "Build with Parameters" (manual)
├── PUBLISH_TO_ARTIFACTORY: NO
├── PUBLISH_TO_MAVEN_LOCAL: YES ✅ ← Activa publicación
└── Resultado:
    ├── Build ✅
    ├── Tests ✅
    ├── Coverage ✅
    ├── Publica a Maven Local ✅
    └── NO publica a Artifactory

Tiempo: 9-10 minutos

Ahora otros developers pueden:
└── En módulos de prueba:
    dependencies {
        implementation 'com.scotia.qa:common:1.0.5-SNAPSHOT'
    }
    ./gradlew test  ← Usa versión de Maven Local
```

---

### **Ejemplo 3: Release a master (ignora Maven Local)**

```
Tech Lead hace PR: develop → master
Aprueba y hace merge

Jenkins ejecuta AUTOMÁTICAMENTE:
├── PUBLISH_TO_ARTIFACTORY: AUTO (interpreta como YES en master)
├── PUBLISH_TO_MAVEN_LOCAL: false (ignorado en master)
└── Resultado:
    ├── Build ✅
    ├── Tests ✅
    ├── Versión: 1.0.5 → 1.0.6 (auto-calculada)
    ├── Verificar duplicados ✅
    ├── Aprobar publicación ✅ (manual)
    ├── Stage "Maven Local" → SKIPPED (no aplica)
    └── Stage "Artifactory" → EJECUTA ✅

Tiempo: 10-12 minutos
Resultado: Versión 1.0.6 publicada en Artifactory (INMUTABLE)
```

---

## ⚠️ LIMITACIONES Y CONSIDERACIONES

### **1. Maven Local está en el SERVIDOR Jenkins, no en tu máquina**

```
❌ INCORRECTO: "Publicar a Maven Local publica en mi máquina"
✅ CORRECTO: "Publica en ~/.m2/ del servidor Jenkins"
```

**Para publicar en TU máquina local:**
```bash
./gradlew publishToMavenLocal
```

---

### **2. Solo disponible en develop**

```
develop:
  ├── Stage "Maven Local" cuando PUBLISH_TO_MAVEN_LOCAL=true
  └── Stage "Artifactory" → SKIPPED

master:
  ├── Stage "Maven Local" → SKIPPED (ignorado)
  └── Stage "Artifactory" → EJECUTA (siempre)
```

---

### **3. NO afecta versionado de Artifactory**

```
Maven Local:
  └── Versión: 1.0.5-SNAPSHOT (de gradle.properties)

Artifactory (master):
  └── Versión: 1.0.6 (auto-calculada consultando Artifactory)
```

Son **independientes**.

---

### **4. Es OPCIONAL (no obligatorio)**

```
Default: false

Pipeline normal (90% de casos):
  └── SKIPPED (no publica, más rápido)

Testing específico (10% de casos):
  └── Build with Parameters → PUBLISH_TO_MAVEN_LOCAL=YES
```

---

## 🔧 IMPLEMENTACIÓN TÉCNICA

### **Condición when:**

```groovy
when {
    allOf {
        branch 'develop'  // Condición 1: Solo en develop
        expression { params.PUBLISH_TO_MAVEN_LOCAL == true }  // Condición 2: Usuario activó
    }
}
```

**Resultado:**
- ✅ En develop + parámetro=true → **EJECUTA**
- ⏭️ En develop + parámetro=false → **SKIPPED**
- ⏭️ En master (cualquier valor) → **SKIPPED**

---

### **Comando de publicación:**

```bash
./gradlew publishToMavenLocal
```

**NO necesita parámetros** porque:
- Versión viene de `gradle.properties`
- Destino es `~/.m2/repository/` (estándar Maven)

---

## 📊 MÉTRICAS DE IMPACTO

| Métrica | Sin Maven Local | Con Maven Local |
|---------|-----------------|-----------------|
| **Tiempo pipeline** | 8-10 min | 9-11 min (+1 min) |
| **Artefactos generados** | JARs en workspace | JARs + Maven Local |
| **Testing manual posible** | ❌ NO | ✅ SÍ |
| **Riesgo** | Ninguno | Ninguno |
| **Complejidad** | Baja | Baja |

**Costo:** +1 minuto  
**Beneficio:** Posibilidad de testing manual del framework

---

## 💡 RECOMENDACIONES

### **✅ USAR cuando:**

1. Nueva funcionalidad importante que necesita validación manual
2. Breaking changes que pueden afectar módulos consumidores
3. Testing de integración antes de release oficial
4. Developer específico necesita probar localmente

### **❌ NO USAR cuando:**

1. Merge rutinario a develop (validación automática suficiente)
2. Fix menor que no afecta API pública
3. Solo quieres feedback rápido (default es más rápido)
4. Estás en master (se ignora de todas formas)

---

## 🎯 FLUJO COMPLETO CON MAVEN LOCAL

```
┌─────────────────────────────────────────────────────────────┐
│ DEVELOPER QUIERE TESTING MANUAL                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. Merge feature → develop                                  │
│    Jenkins ejecuta automáticamente (default)                │
│    └── PUBLISH_TO_MAVEN_LOCAL=false → SKIPPED              │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Developer quiere probar manualmente                      │
│    Jenkins: "Build with Parameters" (manual)                │
│    ├── PUBLISH_TO_ARTIFACTORY: NO                           │
│    ├── PUBLISH_TO_MAVEN_LOCAL: YES ✅                       │
│    └── SKIP_TESTS: false                                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Jenkins (develop) ejecuta pipeline                       │
│    ├── Build ✅                                             │
│    ├── Tests ✅                                             │
│    ├── Coverage ✅                                          │
│    └── Publicar a Maven Local ✅                            │
│        └── ./gradlew publishToMavenLocal                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Framework publicado en Maven Local del servidor          │
│    ~/.m2/repository/com/scotia/qa/                          │
│    ├── common/1.0.5-SNAPSHOT/                               │
│    ├── api-core/1.0.5-SNAPSHOT/                             │
│    ├── web-core/1.0.5-SNAPSHOT/                             │
│    └── mobile-core/1.0.5-SNAPSHOT/                          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Developer prueba en módulo consumidor                    │
│    build.gradle:                                            │
│    dependencies {                                           │
│        implementation 'com.scotia.qa:common:1.0.5-SNAPSHOT' │
│    }                                                        │
│                                                             │
│    ./gradlew test                                           │
│    └── Gradle busca en Maven Local                          │
│        └── Encuentra 1.0.5-SNAPSHOT ✅                      │
│                                                             │
│    ✅ Developer valida que funcionalidad nueva funciona     │
└─────────────────────────────────────────────────────────────┘
```

---

## 📝 RESUMEN PARA DEVELOPERS

### **Pregunta:** ¿Cuándo marcar `PUBLISH_TO_MAVEN_LOCAL=YES`?

**Respuesta:**

✅ **SÍ, márcalo cuando:**
- Agregaste funcionalidad nueva importante
- Quieres que otros puedan probarla antes de release
- Necesitas validar compatibilidad con módulos consumidores

❌ **NO, déjalo en false cuando:**
- Es un fix menor
- Solo quieres validar que compile
- No necesitas testing manual aún

---

### **Pregunta:** ¿Esto publica en MI Maven Local?

**Respuesta:**

❌ **NO** - Publica en el Maven Local del **servidor Jenkins**

✅ **Para publicar en TU máquina:**
```bash
./gradlew publishToMavenLocal
```

---

### **Pregunta:** ¿Afecta las publicaciones de Artifactory?

**Respuesta:**

❌ **NO** - Son completamente independientes:
- Maven Local: Versión de `gradle.properties` (ej: 1.0.5-SNAPSHOT)
- Artifactory: Versión auto-calculada (ej: 1.0.6)

---

### **Pregunta:** ¿Puedo usarlo en master?

**Respuesta:**

❌ **NO** - El parámetro se **ignora** en master.

Master **siempre** publica a Artifactory (no a Maven Local).

---

## ✅ VENTAJAS DE ESTA ESTRATEGIA

| Ventaja | Descripción |
|---------|-------------|
| ✅ **Flexibilidad** | Developers pueden publicar localmente cuando necesiten |
| ✅ **Opcional** | Por defecto NO publica (pipeline más rápido) |
| ✅ **Sin riesgos** | NO afecta Artifactory ni versionado oficial |
| ✅ **Testing** | Permite validar framework antes de release |
| ✅ **Simple** | Solo 1 parámetro boolean (fácil de entender) |
| ✅ **Aislado** | Solo en develop (master no se ve afectado) |

---

## 🚀 CONCLUSIÓN

### **✅ IMPLEMENTAR PARÁMETRO `PUBLISH_TO_MAVEN_LOCAL`**

**Razones:**
1. Útil para testing del framework antes de release
2. Opcional (no afecta flujo normal)
3. No interfiere con Artifactory
4. Costo mínimo (+1 minuto solo cuando se usa)
5. Mejora la calidad de releases (testing previo)

**Configuración:**
- Default: `false` (más rápido)
- Solo en develop
- Ignora en master

---

**🎯 RECOMENDACIÓN: AGREGAR AL PIPELINE**

