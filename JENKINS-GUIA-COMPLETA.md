# 📘 GUÍA DE USO: pipeline.jenkins

> **Scotia QA Framework - Documentación del Pipeline CI/CD**  
> Versión 4.0.0 - Explicación detallada de cada bloque  
> Autor: Abel Venero | Fecha: 2026-02-10

---

## 📋 ÍNDICE

### PARTE 1: ESTRUCTURA DEL PIPELINE
1. [📦 Bloques Principales](#bloques-principales) ← **EMPEZAR AQUÍ**
2. [⚙️ Parámetros de Ejecución](#parámetros-de-ejecución)
3. [🌍 Variables de Entorno](#variables-de-entorno)
4. [⏰ Triggers y Opciones](#triggers-y-opciones)

### PARTE 2: STAGES (ETAPAS)
5. [🔽 Stage: Checkout](#stage-checkout)
6. [🔢 Stage: Calcular Versión](#stage-calcular-versión)
7. [🔍 Stage: Verificar Duplicados](#stage-verificar-duplicados)
8. [🔨 Stage: Compilar](#stage-compilar)
9. [🧪 Stage: Tests y Coverage](#stage-tests-y-coverage)
10. [📦 Stage: Artefactos](#stage-artefactos)
11. [🚀 Stage: Publicar](#stage-publicar)

### PARTE 3: POST-ACTIONS
12. [✅ Post: Success](#post-success)
13. [❌ Post: Failure](#post-failure)
14. [🧹 Post: Always](#post-always)

### PARTE 4: USO PRÁCTICO
15. [🚀 Cómo Ejecutar](#cómo-ejecutar-el-pipeline)
16. [🔧 Configuración Inicial](#configuración-inicial)
17. [🐛 Troubleshooting](#troubleshooting)

---

# PARTE 1: ESTRUCTURA DEL PIPELINE

---

## 1. BLOQUES PRINCIPALES

El archivo `pipeline.jenkins` tiene **423 líneas** organizadas en 6 bloques principales:

```groovy
pipeline {
    agent { ... }           // Líneas 5-5    | Dónde se ejecuta
    tools { ... }           // Líneas 7-10   | Herramientas (Java, Gradle)
    parameters { ... }      // Líneas 12-30  | Parámetros de usuario
    environment { ... }     // Líneas 35-50  | Variables globales
    triggers { ... }        // Líneas 55-57  | Cuándo ejecutar automático
    options { ... }         // Líneas 62-69  | Configuración del build
    
    stages { ... }          // Líneas 74-303 | ⭐ CORAZÓN DEL PIPELINE
    
    post { ... }            // Líneas 308-423| Acciones después del build
}
```

### **📍 Ubicación del archivo**

```
qa-scotia-frameworks/
├── pipeline.jenkins    ← 🎯 ESTE ARCHIVO (raíz del proyecto)
├── build.gradle
├── gradle.properties
└── settings.gradle
```

⚠️ **IMPORTANTE:** El archivo **DEBE** estar en la **raíz del proyecto** para que Jenkins lo detecte automáticamente.

---

## 2. PARÁMETROS DE EJECUCIÓN

**📍 Líneas: 12-30**

Estos parámetros aparecen cuando haces clic en **"Build with Parameters"** en Jenkins:

### **2.1. PUBLISH_TO_ARTIFACTORY** (Choice)

```groovy
choice(
    name: 'PUBLISH_TO_ARTIFACTORY',
    choices: ['AUTO', 'YES', 'NO'],
    description: '''Publicar a Artifactory:
    • AUTO: Solo si es rama main/master
    • YES: Forzar publicación ⚠️
    • NO: Solo compilar y testear'''
)
```

**¿Qué hace?**
- **AUTO** (por defecto): Publica **solo** si estás en `main` o `master`
- **YES**: Publica **siempre**, incluso en `develop` o `feature/*` ⚠️ Usar con cuidado
- **NO**: Solo compila, testa y genera JARs. **NO publica** en Artifactory

**Ejemplo de uso:**
```
┌─────────────────────────────────────────┐
│ PUBLISH_TO_ARTIFACTORY: [AUTO ▼]       │  ← Dropdown en Jenkins
│   ○ AUTO                                 │
│   ○ YES                                  │
│   ○ NO                                   │
└─────────────────────────────────────────┘
```

---

### **2.2. CUSTOM_VERSION** (String)

```groovy
string(
    name: 'CUSTOM_VERSION',
    defaultValue: '',
    description: 'Versión personalizada. Ej: 1.2.0'
)
```

**¿Qué hace?**
- Sobrescribe la versión de `gradle.properties`
- **Dejar vacío** para usar versionado automático

**Prioridad de versión:**
```
1. CUSTOM_VERSION (si lo llenas) 👈 MÁXIMA PRIORIDAD
2. Git Tag (ej: v1.0.1)
3. gradle.properties (version=1.0.0)
```

**Ejemplo de uso:**
```
┌─────────────────────────────────────────┐
│ CUSTOM_VERSION: [1.0.1            ]    │  ← Input en Jenkins
└─────────────────────────────────────────┘

Resultado: Publicará versión 1.0.1 en Artifactory
```

---

### **2.3. SKIP_TESTS** (Boolean)

```groovy
booleanParam(
    name: 'SKIP_TESTS',
    defaultValue: false,
    description: '⚠️ Saltar tests'
)
```

**¿Qué hace?**
- `false` (por defecto): **Ejecuta** tests y coverage
- `true`: **Omite** stages de Tests y Coverage

⚠️ **NO RECOMENDADO** para publicaciones a `main`

**Ejemplo de uso:**
```
┌─────────────────────────────────────────┐
│ SKIP_TESTS: ☐                          │  ← Checkbox en Jenkins
└─────────────────────────────────────────┘
```

---

## 3. VARIABLES DE ENTORNO

**📍 Líneas: 35-50**

Variables disponibles en **todos los stages**:

```groovy
environment {
    // ═══════════════════════════════════════
    // ARTIFACTORY
    // ═══════════════════════════════════════
    ARTIFACTORY_URL = 'https://artifactory.cldevops.chl.bns/artifactory'
    ARTIFACTORY_RELEASE_REPO = 'libs-release-thirdparty'
    ARTIFACTORY_CREDS = credentials('Artifactory')
    
    // ═══════════════════════════════════════
    // TEAMS (DESHABILITADO)
    // ═══════════════════════════════════════
    // TEAMS_WEBHOOK = credentials('teams-webhook-qa-framework')
    
    // ═══════════════════════════════════════
    // PROYECTO
    // ═══════════════════════════════════════
    PROJECT_GROUP = 'com.scotia.qa'
    PROJECT_NAME = 'qa-scotia-frameworks'
    MODULES = 'common,api-core,web-core,mobile-core'
    
    // ═══════════════════════════════════════
    // QUALITY GATES
    // ═══════════════════════════════════════
    MIN_CODE_COVERAGE = '70'
    
    // ═══════════════════════════════════════
    // RUNTIME (se calculan en el pipeline)
    // ═══════════════════════════════════════
    VERSION = ''          // Se calcula en stage "Calcular Versión"
    WILL_PUBLISH = 'false' // Se determina según parámetros y rama
}
```

### **📝 Explicación de cada variable:**

| Variable | Valor | ¿Qué es? | ¿Puedes cambiarlo? |
|----------|-------|----------|-------------------|
| `ARTIFACTORY_URL` | `https://...` | URL base de Artifactory | ⚠️ Solo si cambias de servidor |
| `ARTIFACTORY_RELEASE_REPO` | `libs-release-thirdparty` | Repositorio destino | ✅ **SÍ** - Confirmar con DevOps |
| `ARTIFACTORY_CREDS` | `credentials('Artifactory')` | ID de credencial Jenkins | ⚠️ Solo si cambias el ID |
| `PROJECT_GROUP` | `com.scotia.qa` | GroupId Maven | ❌ NO - Definido en proyecto |
| `PROJECT_NAME` | `qa-scotia-frameworks` | Nombre del proyecto | ❌ NO - Definido en proyecto |
| `MODULES` | `common,api-core,...` | Módulos a publicar | ❌ NO - Auto-detectados |
| `MIN_CODE_COVERAGE` | `70` | Cobertura mínima (%) | ✅ **SÍ** - Ajustar según equipo |

⚠️ **IMPORTANTE:** 
- `TEAMS_WEBHOOK` está **comentado** porque aún no se creó la credencial
- Descomentar cuando tengas el webhook configurado

---

## 4. TRIGGERS Y OPCIONES

**📍 Líneas: 55-69**

### **4.1. TRIGGERS** (Líneas 55-57)

```groovy
triggers {
    pollSCM(env.BRANCH_NAME in ['main', 'master'] ? 'H/5 * * * *' : '')
}
```

**¿Qué hace?**
- **En `main/master`**: Jenkins revisa Git cada **5 minutos** buscando cambios
- **En otras ramas**: **NO** ejecuta automáticamente (solo manual)

**Traducción del CRON:**
```
H/5 * * * *
│   │ │ │ │
│   │ │ │ └─── Día de semana (cualquiera)
│   │ │ └───── Mes (cualquiera)
│   │ └─────── Día del mes (cualquiera)
│   └───────── Hora (cualquiera)
└───────────── Cada 5 minutos (H = distribuido)
```

---

### **4.2. OPTIONS** (Líneas 62-69)

```groovy
options {
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
    timeout(time: 30, unit: 'MINUTES')
    timestamps()
    disableConcurrentBuilds()
    skipDefaultCheckout()
    ansiColor('xterm')
}
```

**¿Qué hace cada opción?**

| Opción | ¿Qué hace? | Beneficio |
|--------|------------|-----------|
| `buildDiscarder` | Mantiene solo últimos **30 builds** y **10 artefactos** | 💾 Ahorra espacio en Jenkins |
| `timeout` | Build falla si toma más de **30 minutos** | ⏱️ Evita builds colgados |
| `timestamps` | Agrega hora a cada línea del log | 🕐 Facilita debugging |
| `disableConcurrentBuilds` | Un build a la vez (no paralelos) | 🔒 Evita conflictos |
| `skipDefaultCheckout` | Usa checkout manual en stage | ⚡ Mayor control |
| `ansiColor` | Colorea output en consola | 🎨 Logs más legibles |

---

# PARTE 2: STAGES (ETAPAS)

---

## 5. STAGE: CHECKOUT

**📍 Líneas: 74-92**

```groovy
stage('🔽 Checkout') {
    steps {
        script {
            echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
            echo '📥 Checkout código fuente...'
            echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        }
        checkout scm
        script {
            env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
            env.GIT_COMMIT_MSG = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
            env.GIT_AUTHOR = sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
            echo "📌 Branch: ${env.BRANCH_NAME}"
            echo "📌 Commit: ${env.GIT_COMMIT_SHORT}"
            echo "📌 Autor: ${env.GIT_AUTHOR}"
        }
    }
}
```

**¿Qué hace?**
1. Descarga el código desde Git
2. Obtiene información del commit:
   - Hash corto (ej: `a1b2c3d`)
   - Mensaje del commit
   - Autor del commit
3. Imprime info en consola

**Ejemplo de output:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📥 Checkout código fuente...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📌 Branch: develop
📌 Commit: a1b2c3d
📌 Autor: Abel Venero
```

---

## 6. STAGE: CALCULAR VERSIÓN

**📍 Líneas: 93-146**

```groovy
stage('🔢 Calcular Versión') {
    steps {
        script {
            // 1. Determinar si se publicará
            def shouldPublish = false
            if (params.PUBLISH_TO_ARTIFACTORY == 'AUTO') {
                shouldPublish = (env.BRANCH_NAME in ['main', 'master'])
            } else if (params.PUBLISH_TO_ARTIFACTORY == 'YES') {
                shouldPublish = true
            } else {
                shouldPublish = false
            }
            env.WILL_PUBLISH = shouldPublish.toString()
            
            // 2. Calcular versión
            if (params.CUSTOM_VERSION) {
                env.VERSION = params.CUSTOM_VERSION
            } else {
                // Leer de gradle.properties
                def baseVersion = '1.0.0'
                def propsFile = readFile('gradle.properties')
                def matcher = (propsFile =~ /version=(.+)/)
                if (matcher.find()) {
                    baseVersion = matcher.group(1).trim()
                }
                
                // Buscar Git Tag
                def gitTag = sh(script: 'git describe --tags --exact-match 2>/dev/null || echo ""', returnStdout: true).trim()
                if (gitTag) {
                    env.VERSION = gitTag.startsWith('v') ? gitTag.substring(1) : gitTag
                } else {
                    env.VERSION = baseVersion
                }
            }
            
            echo "📦 VERSIÓN: ${env.VERSION}"
            echo "🚀 PUBLICAR: ${env.WILL_PUBLISH == 'true' ? 'SÍ' : 'NO'}"
        }
    }
}
```

**¿Qué hace?**

### **Paso 1: Determinar si publicará**

```
┌─────────────────────┬───────────┬────────────────┐
│ Parámetro           │ Rama      │ ¿Publica?      │
├─────────────────────┼───────────┼────────────────┤
│ AUTO                │ main      │ ✅ SÍ          │
│ AUTO                │ develop   │ ❌ NO          │
│ YES                 │ cualquiera│ ✅ SÍ (forzado)│
│ NO                  │ cualquiera│ ❌ NO          │
└─────────────────────┴───────────┴────────────────┘
```

### **Paso 2: Calcular versión**

```
┌──────────────────────────────────────────────────┐
│ PRIORIDAD 1: CUSTOM_VERSION (parámetro Jenkins) │
│              ↓ Si está vacío...                  │
│ PRIORIDAD 2: Git Tag (git describe)              │
│              ↓ Si no hay tag...                  │
│ PRIORIDAD 3: gradle.properties                   │
└──────────────────────────────────────────────────┘
```

**Ejemplo práctico:**

```bash
# Escenario A: Sin tag, sin parámetro
gradle.properties: version=1.0.0
Git Tag: (ninguno)
CUSTOM_VERSION: (vacío)
→ Resultado: 1.0.0

# Escenario B: Con tag
gradle.properties: version=1.0.0
Git Tag: v1.0.1
CUSTOM_VERSION: (vacío)
→ Resultado: 1.0.1

# Escenario C: Con parámetro
gradle.properties: version=1.0.0
Git Tag: v1.0.1
CUSTOM_VERSION: 1.0.2
→ Resultado: 1.0.2
```

---

## 7. STAGE: VERIFICAR DUPLICADOS

**📍 Líneas: 147-193**

```groovy
stage('🔍 Verificar Duplicados') {
    when {
        expression { env.WILL_PUBLISH == 'true' }  // Solo si va a publicar
    }
    steps {
        script {
            def versionExists = false
            def modulosExistentes = []
            
            // Verificar cada módulo en Artifactory
            env.MODULES.split(',').each { module ->
                def moduleTrimmed = module.trim()
                def artifactPath = "${env.PROJECT_GROUP.replace('.', '/')}/${moduleTrimmed}/${env.VERSION}"
                def checkUrl = "${env.ARTIFACTORY_URL}/${env.ARTIFACTORY_RELEASE_REPO}/${artifactPath}/${moduleTrimmed}-${env.VERSION}.jar"
                
                try {
                    def response = httpRequest(
                        httpMode: 'HEAD',
                        url: checkUrl,
                        authentication: 'Artifactory',
                        validResponseCodes: '100:599',
                        quiet: true
                    )
                    
                    if (response.status == 200) {
                        versionExists = true
                        modulosExistentes.add(moduleTrimmed)
                    }
                } catch (Exception e) {
                    // Versión no existe (es lo esperado)
                }
            }
            
            if (versionExists) {
                echo '❌ ERROR: VERSIÓN DUPLICADA'
                error("❌ Versión ${env.VERSION} ya existe")
            }
        }
    }
}
```

**¿Qué hace?**

### **Protección contra sobrescritura**

1. **Solo ejecuta** si `WILL_PUBLISH == 'true'`
2. Para cada módulo (`common`, `api-core`, etc.):
   - Construye URL del artefacto en Artifactory
   - Hace petición `HEAD` (no descarga, solo verifica existencia)
   - Si responde `200 OK` → Ya existe ❌
3. Si **algún módulo existe** → **FALLA EL BUILD**

**Ejemplo de URL verificada:**

```
https://artifactory.cldevops.chl.bns/artifactory/libs-release-thirdparty/com/scotia/qa/common/1.0.0/common-1.0.0.jar
│                                                     │                    │                  │            │
└─ Base URL                                          └─ Repo              └─ Group/Module    └─ Version   └─ Artefacto
```

**Ejemplo de error:**

```
🔍 Verificando versión 1.0.0 en Artifactory...
🔎 Verificando: common...
   ⚠️  common v1.0.0 YA EXISTE
🔎 Verificando: api-core...
   ⚠️  api-core v1.0.0 YA EXISTE

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
❌ ERROR: VERSIÓN DUPLICADA

Módulos existentes:
   - common v1.0.0
   - api-core v1.0.0

Soluciones:
  1. Incrementar versión en gradle.properties
  2. Crear nuevo tag Git
  3. Usar CUSTOM_VERSION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

⚠️ **POR QUÉ ES IMPORTANTE:**
- Artifactory **NO permite sobrescribir** releases
- Versiones deben ser **únicas e inmutables**
- Evita errores `409 Conflict` en publicación

---

## 8. STAGE: COMPILAR

**📍 Líneas: 195-233**

```groovy
stage('🔍 Verificar Entorno') {
    steps {
        sh '''
            echo "☕ Java:"
            java -version
            echo ""
            echo "🐘 Gradle:"
            gradle --version || ./gradlew --version
        '''
    }
}

stage('🧹 Limpiar') {
    steps {
        sh 'gradle clean || ./gradlew clean'
    }
}

stage('🔨 Compilar') {
    steps {
        script {
            echo "🔨 Compilando módulos: ${env.MODULES}"
        }
        sh """
            gradle build -x test -Pversion=${env.VERSION} || \
            ./gradlew build -x test -Pversion=${env.VERSION}
        """
    }
}
```

**¿Qué hace cada sub-stage?**

### **8.1. Verificar Entorno**
- Imprime versión de Java
- Imprime versión de Gradle
- **Para qué:** Debugging (saber qué versiones usa Jenkins)

**Output esperado:**
```
☕ Java:
openjdk version "21.0.1" 2023-10-17
OpenJDK Runtime Environment (build 21.0.1+12)

🐘 Gradle:
Gradle 8.5
```

### **8.2. Limpiar**
- Ejecuta `gradle clean`
- **Para qué:** Elimina builds anteriores (`build/` folder)
- Garantiza compilación limpia

### **8.3. Compilar**
- Ejecuta `gradle build -x test`
  - `-x test` = **Excluye tests** (se ejecutan en stage aparte)
  - `-Pversion=X.X.X` = Sobrescribe versión calculada
- Compila los **4 módulos**:
  - `common`
  - `api-core`
  - `web-core`
  - `mobile-core`

**Resultado:**
```
BUILD SUCCESSFUL in 45s
12 actionable tasks: 12 executed
```

---

## 9. STAGE: TESTS Y COVERAGE

**📍 Líneas: 234-265**

```groovy
stage('🧪 Tests') {
    when {
        expression { params.SKIP_TESTS == false }
    }
    steps {
        sh 'gradle test || ./gradlew test'
    }
    post {
        always {
            junit(allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml')
        }
    }
}

stage('📊 Coverage') {
    when {
        expression { params.SKIP_TESTS == false }
    }
    steps {
        sh 'gradle jacocoTestReport || ./gradlew jacocoTestReport'
        jacoco(
            execPattern: '**/build/jacoco/*.exec',
            classPattern: '**/build/classes/java/main',
            sourcePattern: '**/src/main/java',
            minimumLineCoverage: env.MIN_CODE_COVERAGE
        )
    }
}
```

**¿Qué hace?**

### **9.1. Stage Tests**

**Condición:** Solo ejecuta si `SKIP_TESTS == false`

1. Ejecuta todos los tests JUnit
2. En `post.always`: Publica resultados en Jenkins
   - ✅ Tests pasados (verde)
   - ❌ Tests fallidos (rojo)
   - ⚠️ Tests skipped (amarillo)

**Ejemplo de reporte:**
```
Tests Summary:
  ✅ 156 passed
  ❌ 2 failed
  ⚠️  3 skipped
  ━━━━━━━━━━━━━━
  Total: 161 tests
```

### **9.2. Stage Coverage**

**Condición:** Solo ejecuta si `SKIP_TESTS == false`

1. Genera reporte de cobertura con Jacoco
2. Valida cobertura mínima (`MIN_CODE_COVERAGE = 70%`)
3. Si cobertura < 70% → **Build UNSTABLE** ⚠️

**Ejemplo de reporte:**
```
Code Coverage:
  Lines:    85% ✅ (> 70%)
  Branches: 78% ✅ (> 70%)
  Methods:  92% ✅ (> 70%)
```

---

## 10. STAGE: ARTEFACTOS

**📍 Líneas: 266-278**

```groovy
stage('📦 Artefactos') {
    steps {
        sh """
            gradle jar javadocJar sourcesJar -Pversion=${env.VERSION} || \
            ./gradlew jar javadocJar sourcesJar -Pversion=${env.VERSION}
        """
    }
    post {
        always {
            archiveArtifacts(
                artifacts: '**/build/libs/*.jar',
                allowEmptyArchive: true,
                fingerprint: true
            )
        }
    }
}
```

**¿Qué hace?**

1. **Genera 3 tipos de JAR** por cada módulo:
   - `{module}-{version}.jar` → Binario compilado
   - `{module}-{version}-javadoc.jar` → Documentación
   - `{module}-{version}-sources.jar` → Código fuente

2. **Archiva en Jenkins** (disponibles para descarga)

**Ejemplo de artefactos generados:**

```
build/libs/
├── common-1.0.0.jar           (145 KB)
├── common-1.0.0-javadoc.jar   (89 KB)
├── common-1.0.0-sources.jar   (52 KB)
├── api-core-1.0.0.jar         (278 KB)
├── api-core-1.0.0-javadoc.jar (156 KB)
├── api-core-1.0.0-sources.jar (98 KB)
├── web-core-1.0.0.jar         (312 KB)
├── web-core-1.0.0-javadoc.jar (189 KB)
├── web-core-1.0.0-sources.jar (124 KB)
├── mobile-core-1.0.0.jar      (256 KB)
├── mobile-core-1.0.0-javadoc.jar (145 KB)
└── mobile-core-1.0.0-sources.jar (87 KB)
```

**Total:** 12 archivos JAR (3 por módulo × 4 módulos)

---

## 11. STAGE: PUBLICAR

**📍 Líneas: 279-303**

```groovy
stage('🚀 Publicar') {
    when {
        expression { env.WILL_PUBLISH == 'true' }  // Solo si debe publicar
    }
    steps {
        script {
            echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
            echo "🚀 Publicando versión ${env.VERSION} a Artifactory..."
            echo "📁 Repositorio: ${env.ARTIFACTORY_RELEASE_REPO}"
            echo "🌐 URL: ${env.ARTIFACTORY_URL}/${env.ARTIFACTORY_RELEASE_REPO}"
            echo '⚠️  RELEASE = INMUTABLE (no se puede sobrescribir)'
            echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        }
        sh """
            gradle publish \
                -Pversion=${env.VERSION} \
                -PartifactoryUrl=${env.ARTIFACTORY_URL}/${env.ARTIFACTORY_RELEASE_REPO} \
                -PartifactoryUser=${env.ARTIFACTORY_CREDS_USR} \
                -PartifactoryPassword=${env.ARTIFACTORY_CREDS_PSW} || \
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

**¿Qué hace?**

### **Paso 1: Verificar condición**
- Solo ejecuta si `WILL_PUBLISH == 'true'`
- Si `WILL_PUBLISH == 'false'` → Stage **SKIPPED** (aparece gris en Jenkins)

### **Paso 2: Publicar a Artifactory**

**Comando ejecutado:**
```bash
gradle publish \
  -Pversion=1.0.0 \
  -PartifactoryUrl=https://artifactory.cldevops.chl.bns/artifactory/libs-release-thirdparty \
  -PartifactoryUser=jenkins-user \
  -PartifactoryPassword=AKCp8kq7qK...
```

**Parámetros explicados:**

| Parámetro | Valor | Origen |
|-----------|-------|--------|
| `-Pversion` | `1.0.0` | Variable `env.VERSION` (calculada) |
| `-PartifactoryUrl` | URL completa | `ARTIFACTORY_URL + ARTIFACTORY_RELEASE_REPO` |
| `-PartifactoryUser` | Usuario | Credencial Jenkins `ARTIFACTORY_CREDS_USR` |
| `-PartifactoryPassword` | Token/Password | Credencial Jenkins `ARTIFACTORY_CREDS_PSW` |

⚠️ **NOTA:** Usa `credentials()` para inyectar user/password automáticamente

### **Paso 3: Resultado**

**En Artifactory se crea:**

```
libs-release-thirdparty/
└── com/
    └── scotia/
        └── qa/
            ├── common/
            │   └── 1.0.0/
            │       ├── common-1.0.0.jar
            │       ├── common-1.0.0-javadoc.jar
            │       ├── common-1.0.0-sources.jar
            │       └── common-1.0.0.pom
            ├── api-core/
            │   └── 1.0.0/
            │       └── ...
            ├── web-core/
            │   └── 1.0.0/
            │       └── ...
            └── mobile-core/
                └── 1.0.0/
                    └── ...
```

**Output esperado:**
```
🚀 Publicando versión 1.0.0 a Artifactory...
📁 Repositorio: libs-release-thirdparty
🌐 URL: https://artifactory.cldevops.chl.bns/artifactory/libs-release-thirdparty
⚠️  RELEASE = INMUTABLE (no se puede sobrescribir)

> Task :common:publish
> Task :api-core:publish
> Task :web-core:publish
> Task :mobile-core:publish

✅ PUBLICACIÓN EXITOSA
📦 Versión 1.0.0 en Artifactory
🔒 Versión INMUTABLE

BUILD SUCCESSFUL in 23s
```

---

# PARTE 3: POST-ACTIONS

---

## 12. POST: SUCCESS

**📍 Líneas: 308-360**

```groovy
post {
    success {
        script {
            echo '✅ BUILD EXITOSO'
            
            // ══════════════════════════════════════════════
            // NOTIFICACIONES TEAMS (DESHABILITADO)
            // ══════════════════════════════════════════════
            // TODO: Descomentar cuando se cree la credencial
            // ══════════════════════════════════════════════
            /*
            try {
                def publishStatus = env.WILL_PUBLISH == 'true' ? 'SÍ ✅' : 'NO ⚠️'
                def publishedInfo = ''
                
                if (env.WILL_PUBLISH == 'true') {
                    publishedInfo = "Publicado: ${env.VERSION} (INMUTABLE)"
                } else {
                    publishedInfo = "NO publicado (rama: ${env.BRANCH_NAME})"
                }
                
                def payload = [
                    '@type': 'MessageCard',
                    '@context': 'https://schema.org/extensions',
                    summary: 'Build Exitoso',
                    themeColor: '00FF00',
                    title: "✅ Build Exitoso - ${env.PROJECT_NAME}",
                    sections: [[
                        activityTitle: "Build #${env.BUILD_NUMBER}",
                        facts: [
                            [name: '📌 Branch', value: env.BRANCH_NAME],
                            [name: '📦 Versión', value: env.VERSION],
                            [name: '🚀 Publicado', value: publishStatus],
                            [name: '👤 Autor', value: env.GIT_AUTHOR],
                            [name: '💬 Commit', value: env.GIT_COMMIT_MSG]
                        ],
                        text: publishedInfo
                    ]],
                    potentialAction: [[
                        '@type': 'OpenUri',
                        name: 'Ver Build',
                        targets: [[os: 'default', uri: env.BUILD_URL]]
                    ]]
                ]
                
                httpRequest(
                    httpMode: 'POST',
                    contentType: 'APPLICATION_JSON',
                    requestBody: groovy.json.JsonOutput.toJson(payload),
                    url: env.TEAMS_WEBHOOK,
                    validResponseCodes: '200:299'
                )
                echo '✅ Notificación Teams enviada'
            } catch (Exception e) {
                echo "⚠️  Teams falló: ${e.message}"
            }
            */
            echo '📧 Notificaciones Teams deshabilitadas (falta credencial)'
        }
    }
```

**¿Qué hace?**

Se ejecuta **solo si el build fue exitoso** (todos los stages ✅)

### **Estado Actual: DESHABILITADO** ⚠️

Las notificaciones Teams están **comentadas** porque falta crear la credencial `teams-webhook-qa-framework`

### **Cuando lo habilites:**

Enviará una tarjeta adaptativa a Teams con:

**Ejemplo de mensaje:**

```
╔═══════════════════════════════════════════════════╗
║  ✅ Build Exitoso - qa-scotia-frameworks          ║
╠═══════════════════════════════════════════════════╣
║  Build #42                                        ║
║                                                   ║
║  📌 Branch:    develop                            ║
║  📦 Versión:   1.0.1                              ║
║  🚀 Publicado: SÍ ✅                              ║
║  👤 Autor:     Abel Venero                        ║
║  💬 Commit:    fix: corregir error en login       ║
║                                                   ║
║  Publicado: 1.0.1 (INMUTABLE)                     ║
║                                                   ║
║  [ Ver Build ]  ← Link a Jenkins                  ║
╚═══════════════════════════════════════════════════╝
```

### **Para habilitar:**

1. Crear credencial en Jenkins:
   ```
   ID: teams-webhook-qa-framework
   Type: Secret text
   Secret: https://outlook.office.com/webhook/...
   ```

2. Descomentar línea 43 en `pipeline.jenkins`:
   ```groovy
   TEAMS_WEBHOOK = credentials('teams-webhook-qa-framework')
   ```

3. Descomentar bloque completo (líneas 318-357)

---

## 13. POST: FAILURE

**📍 Líneas: 361-402**

```groovy
    failure {
        script {
            echo '❌ BUILD FALLIDO'
            
            // ══════════════════════════════════════════════
            // NOTIFICACIONES TEAMS (DESHABILITADO)
            // ══════════════════════════════════════════════
            /*
            try {
                def payload = [
                    '@type': 'MessageCard',
                    '@context': 'https://schema.org/extensions',
                    summary: 'Build Fallido',
                    themeColor: 'FF0000',
                    title: "❌ Build Fallido - ${env.PROJECT_NAME}",
                    sections: [[
                        activityTitle: "Build #${env.BUILD_NUMBER}",
                        facts: [
                            [name: '📌 Branch', value: env.BRANCH_NAME],
                            [name: '📦 Versión', value: env.VERSION ?: 'N/A'],
                            [name: '👤 Autor', value: env.GIT_AUTHOR ?: 'N/A']
                        ],
                        text: '⚠️ Build falló. Revisar logs.'
                    ]],
                    potentialAction: [[
                        '@type': 'OpenUri',
                        name: 'Ver Logs',
                        targets: [[os: 'default', uri: "${env.BUILD_URL}console"]]
                    ]]
                ]
                
                httpRequest(
                    httpMode: 'POST',
                    contentType: 'APPLICATION_JSON',
                    requestBody: groovy.json.JsonOutput.toJson(payload),
                    url: env.TEAMS_WEBHOOK,
                    validResponseCodes: '200:299'
                )
            } catch (Exception e) {
                echo "⚠️  Teams falló: ${e.message}"
            }
            */
            echo '📧 Notificaciones Teams deshabilitadas (falta credencial)'
        }
    }
```

**¿Qué hace?**

Se ejecuta **solo si el build falló** (algún stage ❌)

### **Causas comunes de fallo:**

| Stage | Error | Solución |
|-------|-------|----------|
| **Compilar** | Errores de sintaxis Java | Revisar código |
| **Tests** | Tests fallidos | Corregir tests |
| **Coverage** | Cobertura < 70% | Agregar tests |
| **Verificar Duplicados** | Versión ya existe | Incrementar versión |
| **Publicar** | 401 Unauthorized | Verificar credenciales |

### **Cuando lo habilites:**

Enviará mensaje rojo a Teams:

```
╔═══════════════════════════════════════════════════╗
║  ❌ Build Fallido - qa-scotia-frameworks          ║
╠═══════════════════════════════════════════════════╣
║  Build #43                                        ║
║                                                   ║
║  📌 Branch:  feature/nueva-funcionalidad          ║
║  📦 Versión: 1.0.1                                ║
║  👤 Autor:   Abel Venero                          ║
║                                                   ║
║  ⚠️ Build falló. Revisar logs.                    ║
║                                                   ║
║  [ Ver Logs ]  ← Link a console de Jenkins        ║
╚═══════════════════════════════════════════════════╝
```

---

## 14. POST: ALWAYS

**📍 Líneas: 403-423**

```groovy
    always {
        script {
            echo "⏱️  Duración: ${currentBuild.durationString}"
        }
        // Limpiar workspace como en tu Jenkinsfile existente
        cleanWs()
    }
}
```

**¿Qué hace?**

Se ejecuta **SIEMPRE**, sin importar si el build fue exitoso o falló

### **Acciones:**

1. **Imprime duración del build**
   ```
   ⏱️  Duración: 2 min 34 sec
   ```

2. **Limpia el workspace** (`cleanWs()`)
   - Elimina **todos los archivos** del workspace de Jenkins
   - Libera espacio en disco
   - Garantiza builds limpios (no quedan archivos de builds anteriores)

⚠️ **IMPORTANTE:** 
- Los **artefactos archivados** NO se eliminan (están en Jenkins master)
- Solo se limpia el workspace del **agente** (jslave1)

---

# PARTE 4: USO PRÁCTICO

---

## 15. CÓMO EJECUTAR EL PIPELINE

### **Opción 1: Ejecución Manual (Primera vez)**

La **primera vez** que ejecutes el pipeline en una rama, Jenkins NO conoce los parámetros todavía.

**Pasos:**

```
1. Jenkins → Seleccionar tu job

2. Seleccionar rama (ej: "develop")

3. Clic en "Construir ahora"  ← Solo esta opción disponible

4. Esperar a que termine

5. Refrescar página

6. Ahora verás "Build with Parameters" ✅
```

**¿Por qué?**
- Jenkins necesita **leer el Jenkinsfile** primero
- En la primera ejecución **descubre** los parámetros
- A partir de la segunda ejecución, muestra el formulario

---

### **Opción 2: Build with Parameters (Segunda vez en adelante)**

**Pasos:**

```
1. Jenkins → Seleccionar tu job → Rama "develop"

2. Clic en "Build with Parameters"

3. Configurar parámetros:

   ┌─────────────────────────────────────────────┐
   │ PUBLISH_TO_ARTIFACTORY: [AUTO ▼]           │
   │   ○ AUTO  ← Solo publica en main/master    │
   │   ○ YES   ← Fuerza publicación ⚠️          │
   │   ○ NO    ← Solo compilar y testear        │
   ├─────────────────────────────────────────────┤
   │ CUSTOM_VERSION: [                    ]     │
   │   Dejar vacío para usar automático         │
   ├─────────────────────────────────────────────┤
   │ SKIP_TESTS: ☐                              │
   │   Marcar para omitir tests                 │
   └─────────────────────────────────────────────┘

4. Clic en "Build"

5. Ver progreso en "Build History"
```

---

### **Opción 3: Automático (Solo en main/master)**

**Configurado en:** `triggers` (líneas 55-57)

**¿Cuándo se ejecuta?**
- Cada **5 minutos** Jenkins revisa si hay cambios en `main` o `master`
- Si detecta **nuevo commit** → Build automático

**Ejemplo:**

```
10:00 → Developer hace merge a main
10:03 → Jenkins detecta cambio (poll SCM)
10:03 → Inicia build automático
10:05 → Build exitoso ✅
10:05 → Publica versión a Artifactory
```

⚠️ **IMPORTANTE:** 
- **NO ejecuta automático en `develop` o `feature/*`**
- Solo manual en ramas de desarrollo

---

## 16. CONFIGURACIÓN INICIAL

Antes de usar el pipeline, necesitas configurar:

### **📋 Checklist de Configuración**

#### **✅ 1. Credenciales Jenkins (OBLIGATORIO)**

```
Manage Jenkins → Manage Credentials → (global) → Add Credentials

Credencial 1: Artifactory
├─ Kind: Username with password
├─ Username: [TU USUARIO ARTIFACTORY]
├─ Password: [TOKEN API ARTIFACTORY]
├─ ID: Artifactory
└─ Description: Artifactory CI/CD

Credencial 2: Teams (OPCIONAL)
├─ Kind: Secret text
├─ Secret: [URL WEBHOOK TEAMS]
├─ ID: teams-webhook-qa-framework
└─ Description: Teams Webhook QA
```

**¿Cómo obtener el token Artifactory?**
```
1. https://artifactory.cldevops.chl.bns/ui/
2. Login → User Profile → Generate API Key
3. Copiar token (se muestra solo una vez)
```

**¿Cómo obtener webhook Teams?**
```
1. Microsoft Teams → Canal #qa-builds
2. ⋯ (tres puntos) → Conectores
3. Incoming Webhook → Configurar
4. Copiar URL
```

---

#### **✅ 2. Verificar Repositorio Artifactory**

**Línea 38 del pipeline.jenkins:**

```groovy
ARTIFACTORY_RELEASE_REPO = 'libs-release-thirdparty'  ⬅️ Verificar este nombre
```

**Pasos:**

1. Contacta a **DevOps** y pregunta:
   - ¿Cuál es el nombre **exacto** del repositorio de releases?
   - Puede ser:
     - `libs-release-local`
     - `libs-release-thirdparty` ← Actual
     - Otro nombre

2. Si es diferente, **edita la línea 38** del `pipeline.jenkins`

---

#### **✅ 3. Configurar Agente Jenkins**

El pipeline usa:

```groovy
agent { label 'jslave1' }  ⬅️ Línea 5
```

**Verificar que el agente tenga:**

- ✅ **Java 21** (OpenJDK)
- ✅ **Gradle 8.5**
- ✅ **Git**
- ✅ **Plugin HTTP Request** (para verificar duplicados)

**¿Cómo verificar?**
```
Manage Jenkins → Manage Nodes → jslave1 → System Information
```

---

#### **✅ 4. Estructura de Ramas Git**

**Recomendado:**

```
main/master    ← Protegida, solo PRs, publicación automática
develop        ← Integración, NO publica automático
feature/*      ← Desarrollo, NO publica
hotfix/*       ← Correcciones urgentes
```

**Crear rama develop:**

```bash
cd /Users/abel.venero/Documents/qa-scotia-frameworks
git checkout main
git pull origin main
git checkout -b develop
git push -u origin develop
```

---

## 17. TROUBLESHOOTING

### **❌ Error: "Credentials not found: Artifactory"**

**Causa:** No existe la credencial en Jenkins

**Solución:**
```
1. Manage Jenkins → Manage Credentials
2. Crear credencial con ID exacto: "Artifactory"
3. Reintentar build
```

---

### **❌ Error: "Version X.X.X already exists in Artifactory"**

**Causa:** Stage "Verificar Duplicados" detectó que la versión ya existe

**Solución:**

**Opción 1: Incrementar versión en gradle.properties**
```bash
nano gradle.properties
# Cambiar: version=1.0.1
git commit -am "chore: bump version to 1.0.1"
git push
```

**Opción 2: Crear Git Tag**
```bash
git tag v1.0.1
git push origin v1.0.1
```

**Opción 3: Usar CUSTOM_VERSION**
```
Build with Parameters → CUSTOM_VERSION: 1.0.1
```

---

### **❌ Error: "401 Unauthorized" al publicar**

**Causa:** Credenciales inválidas o token expirado

**Solución:**
```
1. Artifactory UI → Regenerar API Key
2. Jenkins → Manage Credentials → Editar credencial "Artifactory"
3. Actualizar password con nuevo token
4. Reintentar build
```

---

### **❌ Error: "Tests failed"**

**Causa:** Uno o más tests JUnit fallaron

**Solución:**
```
1. Jenkins → Build → Test Result
2. Ver qué tests fallaron
3. Corregir código
4. Commit + push
5. Reintentar build
```

**Alternativa temporal (NO RECOMENDADO):**
```
Build with Parameters → SKIP_TESTS: ✅
```

---

### **❌ Error: "Coverage below minimum"**

**Causa:** Cobertura de código < 70%

**Solución:**
```
1. Jenkins → Build → Coverage Report
2. Ver clases sin cobertura
3. Agregar tests unitarios
4. Commit + push
5. Reintentar build
```

**Alternativa:** Reducir `MIN_CODE_COVERAGE` en línea 48 (no recomendado)

---

### **⚠️ Notificaciones Teams no llegan**

**Causa:** Webhook mal configurado o credencial faltante

**Solución:**
```
1. Verificar que la credencial "teams-webhook-qa-framework" exista
2. Verificar URL del webhook (debe empezar con https://outlook.office.com/webhook/)
3. Descomentar línea 43 y bloques de notificación
4. Probar webhook con curl:
   curl -X POST -H "Content-Type: application/json" \
        -d '{"text":"Test"}' \
        [URL_WEBHOOK]
```

---

### **⚠️ Build se queda "colgado"**

**Causa:** Timeout o proceso bloqueado

**Solución:**
```
1. Esperar hasta 30 minutos (timeout configurado)
2. Jenkins lo abortará automáticamente
3. Revisar logs para ver dónde se quedó
4. Verificar que el agente jslave1 esté online
```

---

### **❓ No veo "Build with Parameters"**

**Causa:** Primera ejecución del pipeline en esa rama

**Solución:**
```
1. Ejecutar "Construir ahora" UNA VEZ
2. Jenkins leerá el Jenkinsfile y descubrirá los parámetros
3. Refrescar página
4. Ahora aparecerá "Build with Parameters"
```

---

# 📊 RESUMEN RÁPIDO

## **Pipeline en Números**

```
📄 Archivo:     pipeline.jenkins
📏 Líneas:      423
⏱️  Duración:   ~3-5 minutos (sin tests) / ~6-10 minutos (con tests)
📦 Módulos:     4 (common, api-core, web-core, mobile-core)
🎯 Artefactos:  12 JARs (3 por módulo)
🔒 Repositorio: libs-release-thirdparty (INMUTABLE)
```

---

## **Stages en Orden**

```
1. 🔽 Checkout             (10 seg)  ← Descargar código
2. 🔢 Calcular Versión     (5 seg)   ← Determinar versión
3. 🔍 Verificar Duplicados (15 seg)  ← Evitar sobrescritura
4. 🔍 Verificar Entorno    (5 seg)   ← Java/Gradle versions
5. 🧹 Limpiar              (10 seg)  ← gradle clean
6. 🔨 Compilar             (60 seg)  ← gradle build
7. 🧪 Tests                (90 seg)  ← gradle test
8. 📊 Coverage             (30 seg)  ← Jacoco
9. 🚦 Quality Gate         (1 seg)   ← Validar antes de publicar
10. 📦 Artefactos          (20 seg)  ← Generar JARs
11. 🚀 Publicar            (45 seg)  ← Subir a Artifactory

Total: ~5 minutos (varía según tamaño del proyecto)
```

---

## **Decisión: ¿Publicar o No?**

```
┌─────────────────┬─────────────┬──────────────┐
│ Parámetro       │ Rama        │ ¿Publica?    │
├─────────────────┼─────────────┼──────────────┤
│ AUTO            │ main/master │ ✅ SÍ        │
│ AUTO            │ develop     │ ❌ NO        │
│ AUTO            │ feature/*   │ ❌ NO        │
│ YES             │ cualquiera  │ ✅ SÍ        │
│ NO              │ cualquiera  │ ❌ NO        │
└─────────────────┴─────────────┴──────────────┘
```

---

## **Rutas de Publicación**

```
Artifactory URL:
https://artifactory.cldevops.chl.bns/artifactory/libs-release-thirdparty/

Estructura Maven:
com/scotia/qa/{module}/{version}/{module}-{version}.jar

Ejemplo:
com/scotia/qa/common/1.0.0/common-1.0.0.jar
com/scotia/qa/api-core/1.0.0/api-core-1.0.0.jar
com/scotia/qa/web-core/1.0.0/web-core-1.0.0.jar
com/scotia/qa/mobile-core/1.0.0/mobile-core-1.0.0.jar
```

---

## **Próximos Pasos**

### **Fase 1: Configuración (30 min)**
```
☐ Crear credencial Artifactory en Jenkins
☐ Verificar nombre del repositorio
☐ (Opcional) Crear credencial Teams
☐ Verificar agente jslave1 tiene Java 21 + Gradle 8.5
```

### **Fase 2: Primera Prueba (15 min)**
```
☐ Ejecutar "Construir ahora" (primera vez)
☐ Ejecutar "Build with Parameters" con PUBLISH=NO
☐ Verificar que compile y tests pasen
☐ Revisar artefactos generados
```

### **Fase 3: Primera Publicación (20 min)**
```
☐ Incrementar versión a 1.0.1
☐ Ejecutar con PUBLISH=YES o hacer merge a main
☐ Verificar en Artifactory que se publicó
☐ ✅ LISTO - Pipeline operativo
```

---

## **Soporte**

- **Documentación:** Este archivo
- **Jenkins Logs:** `[URL_JENKINS]/job/[JOB]/[BUILD]/console`
- **Artifactory:** `https://artifactory.cldevops.chl.bns/ui/`
- **Gradle:** `./gradlew --help`

---

**Versión:** 4.0.0  
**Última Actualización:** 2026-02-10  
**Autor:** Abel Venero  
**Mantenedor:** Equipo QA Scotia

