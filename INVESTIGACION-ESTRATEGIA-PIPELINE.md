# 🔍 INVESTIGACIÓN: ESTRATEGIA DE PIPELINE PARA FRAMEWORK QA

**Fecha:** 2025-02-15  
**Contexto:** Framework de QA compartido (librerías publicadas en Artifactory)  
**Objetivo:** Definir estrategia de branching, versionado y ejecución de pipelines

---

## 📚 INVESTIGACIÓN: MEJORES PRÁCTICAS

### **1️⃣ ESTRATEGIA DE BRANCHING PARA LIBRERÍAS**

#### **🏆 Patrón Recomendado: GitHub Flow Simplificado**

```
main/master ─────●────────●──────────●──────────●─────→ (RELEASES)
                  ↑         ↑          ↑          ↑
                  │         │          │          │
develop ─────●───●────●────●────●─────●────●─────●─────→ (INTEGRATION)
             ↑        ↑         ↑          ↑
             │        │         │          │
feature/x ───┘        │         │          │
feature/y ────────────┘         │          │
bugfix/z ───────────────────────┘          │
hotfix/urgent ─────────────────────────────┘
```

**Roles de cada rama:**

| Rama | Propósito | Pipeline | Publicación |
|------|-----------|----------|-------------|
| **master/main** | Código estable en producción | ✅ **FULL** (build + test + publish) | ✅ **Artifactory (RELEASE)** |
| **develop** | Integración continua | ✅ **FULL** (build + test + coverage + análisis) | ✅ **Maven Local** o **NO publish** |
| **feature/*** | Desarrollo de funcionalidades | ❌ NO pipeline (o básico: solo compile) | ❌ NO publish |
| **bugfix/*** | Corrección de bugs | ❌ NO pipeline | ❌ NO publish |
| **hotfix/*** | Correcciones urgentes | ✅ Pipeline **directo a master** | ✅ **Artifactory (inmediato)** |

---

### **2️⃣ FLUJO DE TRABAJO RECOMENDADO**

#### **Desarrollo Normal (Features):**

```
1. Developer crea feature/new-functionality desde develop
2. Developer trabaja en local (NO ejecuta Jenkins)
3. Developer hace tests locales: ./gradlew test publishToMavenLocal
4. Developer abre PR: feature/new-functionality → develop
5. Code Review (otro developer revisa)
6. Merge a develop → 🚀 JENKINS EJECUTA EN DEVELOP:
   ├── ✅ Build
   ├── ✅ Tests (con coverage + quality gate)
   ├── ✅ Análisis de vulnerabilidades
   ├── ✅ Validaciones
   └── ❌ NO publica (solo verifica calidad)
7. Cuando hay suficientes features acumuladas:
   - Tech Lead abre PR: develop → master
   - Code Review final
   - Merge a master → 🚀 JENKINS EJECUTA EN MASTER:
     ├── ✅ Build
     ├── ✅ Tests completos
     ├── ✅ Quality gates
     └── ✅ PUBLICA A ARTIFACTORY (versión RELEASE)
```

#### **Hotfixes (Urgentes):**

```
1. Developer crea hotfix/critical-bug desde master
2. Fix en hotfix/critical-bug
3. PR: hotfix/critical-bug → master (directo)
4. Merge a master → 🚀 Publica inmediatamente
5. Merge master → develop (backport del fix)
```

---

### **3️⃣ VERSIONADO AUTOMÁTICO: ESTRATEGIAS**

#### **📋 OPCIÓN A: CONSULTAR ARTIFACTORY (RECOMENDADA)**

**Ventajas:**
- ✅ Fuente única de verdad (Artifactory)
- ✅ Evita duplicados garantizado
- ✅ No depende de Git tags
- ✅ Funciona aunque se borren ramas

**Implementación:**

```groovy
def getLatestVersionFromArtifactory(groupId, artifactId) {
    def apiUrl = "${ARTIFACTORY_URL}/api/search/latestVersion"
    def params = "?g=${groupId}&a=${artifactId}&repos=${ARTIFACTORY_RELEASE_REPO}"
    
    try {
        def response = httpRequest(
            url: apiUrl + params,
            authentication: 'Artifactory',
            validResponseCodes: '200,404',
            timeout: 10
        )
        
        if (response.status == 200) {
            return response.content.trim()
        }
    } catch (Exception e) {
        echo "⚠️ No hay versión previa en Artifactory (primera publicación)"
    }
    
    return null  // Primera publicación
}

// Uso:
def latestVersion = getLatestVersionFromArtifactory('com.scotia.qa', 'common')
def nextVersion = incrementVersion(latestVersion ?: '0.9.0')  // Si null → 1.0.0
```

**Cálculo de siguiente versión:**

```groovy
def incrementVersion(currentVersion) {
    def parts = currentVersion.split('\\.')
    def major = parts[0].toInteger()
    def minor = parts[1].toInteger()
    def patch = parts[2].toInteger()
    
    // Incrementar PATCH por defecto
    patch++
    
    return "${major}.${minor}.${patch}"
}
```

**API de Artifactory:**
```
GET /api/search/latestVersion?g=com.scotia.qa&a=common&repos=libs-release-thirdparty

Response: "1.0.5"
```

---

#### **📋 OPCIÓN B: GIT TAGS (Alternativa)**

**Ventajas:**
- ✅ Historial visible en Git
- ✅ Fácil rollback

**Desventajas:**
- ❌ Si borras rama, pierdes info
- ❌ Requiere disciplina (crear tag en cada release)

**Implementación:**

```groovy
def latestTag = sh(script: 'git describe --tags --abbrev=0 2>/dev/null || echo "v0.9.0"', returnStdout: true).trim()
def nextVersion = incrementVersion(latestTag.replaceFirst('v', ''))
```

---

#### **📋 OPCIÓN C: GRADLE.PROPERTIES + BUILD NUMBER (Hybrid)**

**Ventajas:**
- ✅ Simple
- ✅ Visible en código

**Desventajas:**
- ❌ Requiere commit cada vez que cambia versión
- ❌ Puede generar conflictos en merges

**Implementación:**

```groovy
// gradle.properties:
version=1.0.0

// Jenkins agrega build number automático:
env.VERSION = "${baseVersion}.${env.BUILD_NUMBER}"
// Resultado: 1.0.0.42
```

---

### **🏆 RECOMENDACIÓN FINAL: OPCIÓN A (Artifactory API)**

**Por qué:**
1. ✅ No requiere tags manuales
2. ✅ No requiere commits para cambiar versión
3. ✅ Evita duplicados garantizado (404 si ya existe)
4. ✅ Fuente única de verdad
5. ✅ Funciona con cualquier estrategia de Git

---

## 4️⃣ JENKINS: ¿UNA O DOS RAMAS?

### **🏆 RECOMENDACIÓN: MULTIBRANCH PIPELINE (2 RAMAS)**

**Configurar Jenkins para ejecutar EN:**
- ✅ **develop** (para QA/validación)
- ✅ **master** (para publicación)

#### **¿Por qué 2 ramas?**

| Aspecto | Solo master | develop + master |
|---------|-------------|------------------|
| **Detección temprana de bugs** | ❌ Bugs llegan a master | ✅ Detectados en develop |
| **Coverage analysis** | Solo al final | ✅ Feedback continuo |
| **Vulnerabilidades CVE** | ❌ Detectadas tarde | ✅ Detectadas antes de release |
| **Quality Gates** | Solo en release | ✅ Validación continua |
| **Riesgo de release** | ❌ Alto (sin validación previa) | ✅ Bajo (ya validado) |
| **Feedback a developers** | Lento (solo en releases) | ✅ Rápido (cada merge a develop) |

---

#### **Configuración Recomendada:**

**Pipeline en develop:**
```groovy
when {
    branch 'develop'
}
stages {
    stage('Build') { /* ... */ }
    stage('Tests') { /* ... */ }
    stage('Coverage') { 
        // Jacoco + reportes
        // Quality gate: min 70% coverage
    }
    stage('Vulnerabilidades') { 
        // OWASP Dependency Check
        // CVE scanning
    }
    stage('Quality Gate') {
        // SonarQube (si disponible)
        // O validaciones custom
    }
    // ❌ NO PUBLICAR
}
```

**Pipeline en master:**
```groovy
when {
    branch pattern: "main|master", comparator: "REGEXP"
}
stages {
    stage('Build') { /* ... */ }
    stage('Tests') { /* Más rápido, ya validado en develop */ }
    stage('Verificar Duplicados') {
        // Consultar Artifactory API
        // Evitar sobrescritura
    }
    stage('Publicar') { 
        // ✅ PUBLICAR A ARTIFACTORY
    }
}
```

---

### **5️⃣ PROTECCIÓN DE RAMA MASTER**

#### **✅ Configuraciones Recomendadas en Bitbucket/Git:**

1. **Branch Permissions (en Bitbucket):**
   ```
   ☑ Prevent deletion
   ☑ Restrict changes (solo via PR)
   ☑ Require pull request reviews: 1 reviewer mínimo
   ☑ Require all tasks to be resolved
   ☑ Require passing build
   ```

2. **Merge Checks:**
   ```
   ☑ At least 1 approval
   ☑ All tasks resolved
   ☑ Build passing (Jenkins develop debe estar ✅)
   ☑ No merge conflicts
   ```

3. **Usuarios con permisos de merge a master:**
   - Tech Lead
   - QA Lead
   - (Máximo 2-3 personas)

4. **Estrategia de merge:**
   ```
   ☑ Squash commits (recomendado para features grandes)
   ☐ Merge commit (preserva historia completa)
   ☐ Fast-forward (no recomendado para librerías)
   ```

---

### **6️⃣ PUBLICACIÓN: MAVEN LOCAL vs ARTIFACTORY**

#### **Cuándo usar cada uno:**

| Escenario | Maven Local | Artifactory |
|-----------|-------------|-------------|
| **Desarrollo local** | ✅ Siempre | ❌ NO |
| **Testing en develop (manual)** | ✅ Si `PUBLISH_TO_MAVEN_LOCAL=true` | ❌ NO |
| **CI/CD develop (default)** | ❌ NO (más rápido) | ❌ NO (aún no listo) |
| **CI/CD master** | ❌ NO necesario | ✅ SIEMPRE |
| **Hotfixes urgentes** | ❌ NO | ✅ Directo a Artifactory |

#### **Implementación en Pipeline:**

```groovy
stage('📦 Publicar a Maven Local') {
    when {
        allOf {
            branch 'develop'  // Solo en develop
            expression { params.PUBLISH_TO_MAVEN_LOCAL == true }  // Solo si usuario lo activa
        }
    }
    steps {
        script {
            echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
            echo '📦 Publicando a Maven Local (opcional - solo testing)...'
            echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        }
        sh './gradlew publishToMavenLocal'
        script {
            echo '✅ Publicado en Maven Local del servidor Jenkins'
            echo '⚠️  NOTA: Esto NO afecta Artifactory'
        }
    }
}

stage('🚀 Publicar a Artifactory') {
    when {
        allOf {
            branch pattern: "main|master", comparator: "REGEXP"  // Solo en master
            expression { env.WILL_PUBLISH == 'true' }
        }
    }
    steps {
        script {
            echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
            echo "🚀 Publicando versión ${env.VERSION} a Artifactory..."
            echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        }
        
        sh """
            ./gradlew publish \
                -Pversion=${env.VERSION} \
                -PartifactoryUrl=${ARTIFACTORY_URL}/${ARTIFACTORY_RELEASE_REPO} \
                -PartifactoryUser=${ARTIFACTORY_CREDS_USR} \
                -PartifactoryPassword=${ARTIFACTORY_CREDS_PSW}
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

### **7️⃣ ESTRUCTURA DE PIPELINE.JENKINS**

#### **¿Dónde debe estar el archivo?**

```
qa-scotia-frameworks/
├── pipeline.jenkins  ✅ RAÍZ (recomendado para Multibranch)
└── Jenkinsfile       ✅ Alternativa (nombre estándar)
```

**Respuesta:** Puede estar en la **raíz** con nombre `pipeline.jenkins` O `Jenkinsfile`.

**Configuración en Jenkins:**
- Multibranch Pipeline → Script Path: `pipeline.jenkins`
- O renombrar a `Jenkinsfile` (auto-detectado)

---

#### **¿El archivo debe existir en develop Y master?**

**✅ SÍ - DEBE ESTAR EN AMBAS RAMAS**

**Razón:**
- Jenkins Multibranch Pipeline lee el archivo **de cada rama**
- Si develop no tiene pipeline.jenkins → No ejecuta pipeline en develop
- Si master no tiene pipeline.jenkins → No ejecuta pipeline en master

**Diferencias entre ramas:**

```
develop/pipeline.jenkins:
├── Stages: Build + Test + Coverage + Vulnerabilities
└── NO publica (o publica a Maven Local)

master/pipeline.jenkins:
├── Stages: Build + Test + Publish
└── Publica a Artifactory
```

**PERO:** En la práctica, es mejor tener **UN SOLO pipeline.jenkins** con lógica condicional:

```groovy
stage('📊 Coverage') {
    when {
        branch 'develop'  // Solo en develop
    }
    steps { /* análisis detallado */ }
}

stage('🚀 Publicar') {
    when {
        branch pattern: "main|master", comparator: "REGEXP"  // Solo en master
    }
    steps { /* publicar a Artifactory */ }
}
```

---

## 8️⃣ CONSULTA DE ÚLTIMA VERSIÓN EN ARTIFACTORY

### **✅ Implementación con Artifactory API:**

```groovy
/**
 * Consulta la última versión publicada de un módulo en Artifactory.
 * 
 * @param groupId    Ej: 'com.scotia.qa'
 * @param artifactId Ej: 'common'
 * @return Última versión publicada o null si es primera publicación
 */
def getLatestVersionFromArtifactory(groupId, artifactId) {
    echo "🔍 Consultando última versión de ${artifactId} en Artifactory..."
    
    def apiUrl = "${ARTIFACTORY_URL}/api/search/latestVersion"
    def params = "?g=${groupId}&a=${artifactId}&repos=${ARTIFACTORY_RELEASE_REPO}"
    
    try {
        def response = httpRequest(
            url: apiUrl + params,
            authentication: 'Artifactory',  // ID de credencial en Jenkins
            validResponseCodes: '200,404',
            consoleLogResponseBody: false,
            timeout: 10
        )
        
        if (response.status == 200) {
            def version = response.content.trim()
            echo "✅ Última versión encontrada: ${version}"
            return version
        } else if (response.status == 404) {
            echo "ℹ️  No hay versiones previas (primera publicación)"
            return null
        }
    } catch (Exception e) {
        echo "⚠️  Error consultando Artifactory: ${e.message}"
        echo "⚠️  Asumiendo primera publicación"
        return null
    }
}

/**
 * Incrementa versión automáticamente.
 * 
 * Estrategia: Incrementar PATCH por defecto
 * - 1.0.0 → 1.0.1
 * - 1.0.5 → 1.0.6
 * 
 * Para MINOR/MAJOR: Especificar en gradle.properties o CUSTOM_VERSION
 */
def calculateNextVersion(currentVersion) {
    if (!currentVersion) {
        return '1.0.0'  // Primera versión
    }
    
    def parts = currentVersion.split('\\.')
    if (parts.size() != 3) {
        echo "⚠️  Versión inválida: ${currentVersion}, usando 1.0.0"
        return '1.0.0'
    }
    
    def major = parts[0].toInteger()
    def minor = parts[1].toInteger()
    def patch = parts[2].toInteger()
    
    // Incrementar PATCH
    patch++
    
    def nextVersion = "${major}.${minor}.${patch}"
    echo "📈 Versión calculada: ${currentVersion} → ${nextVersion}"
    
    return nextVersion
}

// USO EN PIPELINE:
stage('🔢 Calcular Versión') {
    steps {
        script {
            if (params.CUSTOM_VERSION) {
                env.VERSION = params.CUSTOM_VERSION
                echo "✅ Versión manual: ${env.VERSION}"
            } else {
                // Consultar Artifactory para cada módulo
                def latestCommon = getLatestVersionFromArtifactory('com.scotia.qa', 'common')
                def nextVersion = calculateNextVersion(latestCommon)
                
                env.VERSION = nextVersion
                echo "✅ Versión automática: ${env.VERSION}"
            }
        }
    }
}
```

---

#### **📋 OPCIÓN B: GRADLE.PROPERTIES + MANUAL (Más Simple)**

**Ventajas:**
- ✅ Muy simple
- ✅ Versión visible en código

**Desventajas:**
- ❌ Requiere commit para cambiar versión
- ❌ Puede generar conflictos

**Implementación:**

```properties
# gradle.properties
version=1.0.5
```

```groovy
// Pipeline lee de gradle.properties
def baseVersion = readFile('gradle.properties')
    .split('\n')
    .find { it.startsWith('version=') }
    .split('=')[1]
    .trim()

env.VERSION = baseVersion
```

---

### **9️⃣ VERIFICACIÓN DE DUPLICADOS**

#### **Antes de publicar, verificar que la versión NO existe:**

```groovy
stage('🔍 Verificar Duplicados') {
    when {
        expression { env.WILL_PUBLISH == 'true' }
    }
    steps {
        script {
            echo "🔍 Verificando si versión ${env.VERSION} ya existe..."
            
            def modules = ['common', 'api-core', 'web-core', 'mobile-core']
            
            modules.each { module ->
                def checkUrl = "${ARTIFACTORY_URL}/${ARTIFACTORY_RELEASE_REPO}" +
                              "/com/scotia/qa/${module}/${env.VERSION}/${module}-${env.VERSION}.jar"
                
                try {
                    def response = httpRequest(
                        url: checkUrl,
                        authentication: 'Artifactory',
                        validResponseCodes: '200,404',
                        httpMode: 'HEAD',
                        timeout: 10
                    )
                    
                    if (response.status == 200) {
                        error("❌ ERROR: Versión ${env.VERSION} del módulo ${module} YA EXISTE en Artifactory")
                    } else {
                        echo "✅ [${module}] Versión ${env.VERSION} disponible"
                    }
                } catch (Exception e) {
                    if (e.message.contains('YA EXISTE')) {
                        throw e  // Re-lanzar error de duplicado
                    }
                    echo "✅ [${module}] Versión ${env.VERSION} disponible"
                }
            }
            
            echo "✅ Todas las versiones disponibles para publicar"
        }
    }
}
```

---

## 🔟 CONFIGURACIÓN DE JENKINS

### **Tipo de Job: MULTIBRANCH PIPELINE**

**Ventajas:**
- ✅ Auto-descubre ramas
- ✅ Un job para todas las ramas
- ✅ Ejecuta pipeline.jenkins de cada rama
- ✅ Limpia jobs de ramas eliminadas

**Configuración:**

```
Jenkins → New Item → Multibranch Pipeline
├── Branch Sources: Bitbucket
├── Behaviors:
│   ├── Discover branches: All branches
│   └── Filter by name (regex): (develop|master|main)  ← Solo estas 2
├── Build Configuration:
│   └── Script Path: pipeline.jenkins
└── Scan Multibranch Pipeline Triggers:
    └── Periodically if not otherwise run: 5 minutes
```

---

### **Branch Discovery Filter:**

```groovy
// En pipeline.jenkins (opcional - filtrar en código)
when {
    anyOf {
        branch 'develop'
        branch pattern: "main|master", comparator: "REGEXP"
    }
}
```

---

## 1️⃣1️⃣ TRIGGERS: ¿AUTOMÁTICO O MANUAL?

### **🏆 RECOMENDACIÓN: HÍBRIDO**

| Rama | Trigger | Razón |
|------|---------|-------|
| **develop** | ⚙️ **Automático** (cada push/merge) | Feedback rápido a developers |
| **master** | 🎯 **Semi-automático** (webhook + aprobación) | Control sobre releases |

**Implementación:**

```groovy
triggers {
    // Polling SCM solo en develop (cada 5 min)
    pollSCM(env.BRANCH_NAME == 'develop' ? 'H/5 * * * *' : '')
    
    // En master: webhook dispara, pero hay stage de aprobación
}

// En master, agregar stage de aprobación:
stage('⏸️ Aprobar Publicación') {
    when {
        branch pattern: "main|master", comparator: "REGEXP"
        expression { env.WILL_PUBLISH == 'true' }
    }
    steps {
        script {
            def userInput = input(
                message: "¿Publicar versión ${env.VERSION} a Artifactory?",
                ok: 'Publicar',
                parameters: [
                    choice(
                        name: 'CONFIRM',
                        choices: ['CANCELAR', 'PUBLICAR'],
                        description: 'Confirmar publicación'
                    )
                ]
            )
            
            if (userInput == 'CANCELAR') {
                error("❌ Publicación cancelada por el usuario")
            }
            
            echo "✅ Publicación aprobada por ${env.BUILD_USER}"
        }
    }
}
```

---

## 1️⃣2️⃣ PARÁMETROS DEL PIPELINE

### **Parámetros Recomendados:**

```groovy
parameters {
    choice(
        name: 'PUBLISH_TO_ARTIFACTORY',
        choices: ['AUTO', 'YES', 'NO'],
        description: '''Publicar a Artifactory:
• AUTO: Solo si es master (recomendado)
• YES: Forzar publicación ⚠️ 
• NO: Solo build + tests'''
    )
    
    booleanParam(
        name: 'PUBLISH_TO_MAVEN_LOCAL',
        defaultValue: false,
        description: '''📦 Publicar a Maven Local (opcional):
• Solo disponible en rama develop
• Útil para testing local del framework
• NO afecta Artifactory
• Default: false (más rápido)'''
    )
    
    string(
        name: 'CUSTOM_VERSION',
        defaultValue: '',
        description: '''Versión personalizada (OPCIONAL):
• Vacío: Auto-calculada desde Artifactory
• Manual: Solo para hotfixes/RCs
• Ejemplo: 1.2.0-hotfix, 2.0.0-rc1'''
    )
    
    booleanParam(
        name: 'SKIP_TESTS',
        defaultValue: false,
        description: '⚠️ Saltar tests (NO recomendado)'
    )
    
    booleanParam(
        name: 'RUN_CVE_SCAN',
        defaultValue: true,
        description: 'Escanear vulnerabilidades CVE'
    )
}
```

---

## 1️⃣3️⃣ ESTRATEGIA DE VERSIONADO SEMÁNTICO

### **🎯 Recomendación: MAJOR.MINOR.PATCH**

```
MAJOR: Cambios incompatibles con versión anterior (breaking changes)
MINOR: Nueva funcionalidad compatible hacia atrás
PATCH: Corrección de bugs (auto-incrementado)
```

#### **Cuándo incrementar cada número:**

| Cambio | Versión | Ejemplo |
|--------|---------|---------|
| **Fix bug** | PATCH++ | 1.0.5 → 1.0.6 |
| **Nueva feature compatible** | MINOR++ | 1.0.6 → 1.1.0 |
| **Breaking change** | MAJOR++ | 1.5.0 → 2.0.0 |

#### **Cómo controlar MINOR/MAJOR:**

**Opción A: Mediante gradle.properties**

```properties
# Cuando quieres nueva MINOR:
version=1.1.0

# Pipeline calcula automáticamente PATCH:
# 1.1.0 → 1.1.1 → 1.1.2 → ...
```

**Opción B: Mediante CUSTOM_VERSION en Jenkins**

```
Build with Parameters:
├── CUSTOM_VERSION: 2.0.0  ← Para breaking change
└── PUBLISH_TO_ARTIFACTORY: YES
```

---

## 1️⃣4️⃣ PROTECCIÓN CONTRA SOBRESCRITURA

### **Validación CRÍTICA antes de publicar:**

```groovy
stage('🔍 Verificar Duplicados') {
    when {
        expression { env.WILL_PUBLISH == 'true' }
    }
    steps {
        script {
            def duplicateFound = false
            
            ['common', 'api-core', 'web-core', 'mobile-core'].each { module ->
                def artifactUrl = "${ARTIFACTORY_URL}/${ARTIFACTORY_RELEASE_REPO}/" +
                                 "com/scotia/qa/${module}/${env.VERSION}/${module}-${env.VERSION}.jar"
                
                try {
                    def response = httpRequest(
                        url: artifactUrl,
                        authentication: 'Artifactory',
                        validResponseCodes: '200,404',
                        httpMode: 'HEAD',
                        consoleLogResponseBody: false,
                        timeout: 10
                    )
                    
                    if (response.status == 200) {
                        echo "❌ [${module}] Versión ${env.VERSION} YA EXISTE"
                        duplicateFound = true
                    } else {
                        echo "✅ [${module}] Versión ${env.VERSION} disponible"
                    }
                } catch (Exception e) {
                    // 404 o error de red = versión no existe (OK)
                    echo "✅ [${module}] Versión ${env.VERSION} disponible"
                }
            }
            
            if (duplicateFound) {
                error("""
❌ ERROR: La versión ${env.VERSION} YA EXISTE en Artifactory

🔧 SOLUCIONES:
1. Incrementar versión en gradle.properties
2. Usar CUSTOM_VERSION con versión nueva
3. Si es hotfix: usar ${env.VERSION}-hotfix-${env.BUILD_NUMBER}

⚠️  RELEASE REPOS SON INMUTABLES (no se puede sobrescribir)
""")
            }
            
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            echo "✅ Versión ${env.VERSION} DISPONIBLE para todos los módulos"
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        }
    }
}
```

---

## 1️⃣5️⃣ RESPUESTAS A TUS PREGUNTAS

### **❓ ¿El pipeline.jenkins debe estar solo en master?**

**❌ NO - Debe estar en AMBAS ramas (develop Y master)**

**Razón:** Jenkins Multibranch lee el archivo de cada rama. Si develop no lo tiene, no ejecuta pipeline en develop.

**Solución:** Tener el mismo archivo en ambas ramas con **lógica condicional** (when) para diferenciar comportamiento.

---

### **❓ ¿Ejecutar pipeline en develop también?**

**✅ SÍ - RECOMENDADO**

**Beneficios:**

1. ✅ **Detección temprana de bugs** (antes de llegar a master)
2. ✅ **Feedback continuo** a developers (cada merge a develop)
3. ✅ **Coverage + Quality Gates** antes de release
4. ✅ **Escaneo de vulnerabilidades** preventivo
5. ✅ **Validación de compilación** en todas las capas

**Pipeline en develop debe:**
- ✅ Build completo
- ✅ Tests completos
- ✅ Coverage (Jacoco)
- ✅ Quality gates (min 70%)
- ✅ CVE scanning
- ❌ **NO publicar a Artifactory**

---

### **❓ ¿Cómo limitar quién puede hacer merge/push a master?**

**✅ Configurar en Bitbucket (Branch Permissions):**

**Paso 1: Repository Settings → Branch permissions**

```
Branch: master
├── ☑ Prevent deletion
├── ☑ Prevent changes without a pull request
├── ☑ Require at least 1 approval
├── ☑ Require all tasks to be resolved
└── ☑ Require passing build (Jenkins develop = ✅)

Merge permissions:
├── ✅ Tech Lead (usuario1)
├── ✅ QA Lead (usuario2)
└── ❌ Resto del equipo (NO pueden hacer merge directo)
```

**Paso 2: Pull Request Settings**

```
Default merge strategy: Squash
├── ☑ Delete source branch after merge
├── ☑ At least 1 successful build
└── ☑ No open tasks
```

---

### **❓ ¿Consultar última versión de Artifactory o usar otra estrategia?**

**🏆 RECOMENDACIÓN: CONSULTAR ARTIFACTORY**

**Por qué:**
1. ✅ **Fuente única de verdad** (lo que está en Artifactory es lo publicado)
2. ✅ **Evita duplicados garantizado** (verificación antes de publish)
3. ✅ **No depende de Git** (funciona aunque borres branches/tags)
4. ✅ **Robusto** (API de Artifactory es confiable)

**Estrategia completa:**

```
1. Leer versión base de gradle.properties (ej: 1.0.0)
2. Consultar última versión en Artifactory
   - Si no existe → usar gradle.properties
   - Si existe → incrementar automáticamente
3. Verificar que nueva versión NO existe
4. Publicar
```

---

### **❓ ¿Publicar en Maven Local o Artifactory?**

**📊 Tabla de Decisión:**

| Escenario | Maven Local | Artifactory | Razón |
|-----------|-------------|-------------|-------|
| Developer en local | ✅ | ❌ | Testing rápido |
| Pipeline develop | ❌ (opcional) | ❌ | Solo validación |
| Pipeline master | ❌ | ✅ | Distribución oficial |
| Hotfix urgente | ❌ | ✅ | Directo a producción |
| Testing pre-release | ✅ | ❌ | Validar antes de publicar |

**Recomendación:** 
- **Maven Local:** Solo desarrollo local
- **Artifactory:** SOLO en master después de validaciones

---

## 🎯 PROPUESTA FINAL: ESTRATEGIA COMPLETA

### **📋 Resumen Ejecutivo:**

```
┌─────────────────────────────────────────────────────────────┐
│ DESARROLLO                                                  │
├─────────────────────────────────────────────────────────────┤
│ 1. Developer: feature/xxx → PR → develop                   │
│ 2. Jenkins (develop):                                       │
│    ✅ Build + Test + Coverage + CVE                         │
│    ❌ NO publica                                            │
│ 3. Feedback en <5 min                                       │
├─────────────────────────────────────────────────────────────┤
│ RELEASE                                                     │
├─────────────────────────────────────────────────────────────┤
│ 1. Tech Lead: develop → PR → master (con approval)         │
│ 2. Jenkins (master):                                        │
│    ✅ Build + Tests                                         │
│    ✅ Consultar última versión en Artifactory               │
│    ✅ Calcular siguiente versión (auto)                     │
│    ✅ Verificar duplicados                                  │
│    ⏸️  Aprobación manual (input step)                      │
│    ✅ Publicar a Artifactory                                │
│ 3. Versión publicada e inmutable                            │
└─────────────────────────────────────────────────────────────┘
```

---

### **📐 Arquitectura de Branches:**

```
master ──────●───────────●────────────●──────→ (1.0.0, 1.1.0, 1.2.0)
              ↑            ↑            ↑
              │ PR         │ PR         │ PR
              │ (approval) │            │
develop ─●───●────●───●───●────●───●───●────→ (validación continua)
         ↑        ↑       ↑        ↑
         │        │       │        │
feature/a┘   feature/b   │   bugfix/c
                    feature/d
```

---

### **🔐 Protección de master:**

```yaml
Bitbucket Branch Permissions:
  master:
    - Prevent deletion: YES
    - Require PR: YES
    - Min approvals: 1
    - Approvers:
      - Tech Lead
      - QA Lead
    - Require build pass: YES (develop debe estar ✅)
    - Merge strategy: Squash
```

---

### **⚙️ Jenkins Multibranch Configuration:**

```yaml
Type: Multibranch Pipeline
Branch Sources: Bitbucket Server
Discover branches: develop, master only
Script Path: pipeline.jenkins
Scan triggers: Every 5 minutes
```

---

### **📦 Versionado:**

```yaml
Strategy: Consultar Artifactory API
Fallback: gradle.properties
Increment: PATCH automático (1.0.5 → 1.0.6)
Override: CUSTOM_VERSION parameter
Format: MAJOR.MINOR.PATCH
```

---

### **🚀 Publicación:**

```yaml
develop:
  - NO publica (solo valida)
  - Feedback rápido (<5 min)

master:
  - Consulta última versión
  - Calcula siguiente versión
  - Verifica duplicados
  - Solicita aprobación manual
  - Publica a Artifactory
  - Versión INMUTABLE
```

---

## 📝 CHECKLIST DE IMPLEMENTACIÓN

### **Fase 1: Configuración Jenkins (1 hora)**

```
☐ Crear Multibranch Pipeline Job
☐ Configurar Branch Sources (develop + master)
☐ Verificar credencial 'Artifactory'
☐ Instalar plugin: HTTP Request
☐ Verificar agente jslave1 (Java 21 + Gradle 8.5)
```

---

### **Fase 2: Configuración Git (30 min)**

```
☐ Bitbucket → Branch Permissions → master
  ├── ☑ Require pull request
  ├── ☑ Min 1 approval
  └── ☑ Build passing
☐ Definir usuarios con permiso de merge
☐ Configurar default merge strategy (Squash)
```

---

### **Fase 3: Actualizar pipeline.jenkins (1 hora)**

```
☐ Agregar función getLatestVersionFromArtifactory()
☐ Agregar función calculateNextVersion()
☐ Actualizar stage 'Calcular Versión' (usar Artifactory)
☐ Agregar stage 'Verificar Duplicados' mejorado
☐ Agregar stage 'Aprobar Publicación' (solo master)
☐ Agregar parámetro PUBLISH_TARGET
☐ Agregar lógica condicional (when branch)
```

---

### **Fase 4: Pruebas (2 horas)**

```
☐ Merge feature → develop
  └── Verificar: Build ✅, Tests ✅, NO publica
  
☐ Merge develop → master
  ├── Verificar: Build ✅, Tests ✅
  ├── Verifica versión calculada
  ├── Verifica duplicados
  ├── Solicita aprobación
  └── Publica a Artifactory ✅
  
☐ Verificar en Artifactory que se publicó
☐ Crear módulo de prueba que importe librería
☐ Verificar que funciona
```

---

## 🎯 PROPUESTA FINAL RECOMENDADA

### **✅ Branching:**
- develop + master (Multibranch Pipeline)
- Feature branches (sin pipeline)

### **✅ Versionado:**
- Consultar Artifactory API (última versión)
- Auto-incrementar PATCH
- Override con CUSTOM_VERSION

### **✅ Pipeline develop:**
- Build + Test + Coverage + CVE
- NO publicar
- Trigger: Automático cada push

### **✅ Pipeline master:**
- Build + Test + Verificar duplicados
- Publicar a Artifactory
- Trigger: Automático + Aprobación manual

### **✅ Protección master:**
- Solo PR (no push directo)
- Min 1 approval
- Build passing en develop

### **✅ Publicación:**
- SOLO en master
- SOLO a Artifactory (RELEASE)
- Versión INMUTABLE

---

**⏱️ Tiempo de implementación:** ~4-5 horas  
**🎯 Resultado:** Pipeline robusto, profesional y a prueba de errores

