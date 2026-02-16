# 🏗️ PLAN DE CONSTRUCCIÓN INCREMENTAL - 7 FASES

**Pipeline:** pipeline.jenkins  
**Estrategia:** Construcción paso a paso, cada fase completamente funcional  
**Tiempo total estimado:** 4-5 horas (distribuidas en varias sesiones)

---

## 📋 FILOSOFÍA

```
✅ Cada fase debe ser FUNCIONAL antes de continuar
✅ Probar en Jenkins después de cada fase
✅ Si falla una fase → Corregir antes de avanzar
✅ Documentar problemas encontrados
✅ Rollback fácil (git checkout) si es necesario
```

---

## 🎯 FASE 1: MVP ✅ **COMPLETADA**

**Archivo:** `pipeline.jenkins` (255 líneas)

**Features:**
- ✅ Checkout, Verificar Entorno, Limpiar
- ✅ Compilar, Tests, Artefactos
- ✅ Reportes JUnit + HTML
- ✅ Post actions (success/failure)

**Testing:** Ejecutar "Construir ahora" en Jenkins

**Duración:** ~8-12 min

---

## ⏳ FASE 2: PARÁMETROS BÁSICOS

**Features a agregar:**

```groovy
parameters {
    booleanParam(
        name: 'SKIP_TESTS',
        defaultValue: false,
        description: '⚠️ Saltar ejecución de tests (NO recomendado)'
    )
}

stage('🧪 Tests') {
    when {
        expression { params.SKIP_TESTS != true }
    }
    // ...existing code...
}
```

**Cambios:**
1. Agregar bloque `parameters { }` después de `options`
2. Modificar stage Tests con condición `when`

**Testing:** 
- Ejecutar con SKIP_TESTS=false → Tests deben correr
- Ejecutar con SKIP_TESTS=true → Stage Tests debe ser SKIPPED

**Duración:** +10 min implementación

---

## ⏳ FASE 3: VERSIONADO SIMPLE

**Features a agregar:**

```groovy
parameters {
    string(
        name: 'CUSTOM_VERSION',
        defaultValue: '',
        description: 'Versión manual (opcional): 1.0.0, 1.2.0-hotfix'
    )
}

stage('🔢 Calcular Versión') {
    steps {
        script {
            if (params.CUSTOM_VERSION?.trim()) {
                env.VERSION = params.CUSTOM_VERSION.trim()
            } else {
                // Leer de gradle.properties
                def props = readFile('gradle.properties')
                def matcher = (props =~ /version=(.+)/)
                env.VERSION = matcher.find() ? matcher.group(1).trim() : '1.0.0'
            }
            echo "📦 VERSIÓN: ${env.VERSION}"
        }
    }
}
```

**Cambios:**
1. Agregar parámetro CUSTOM_VERSION
2. Agregar stage 'Calcular Versión' (después de Checkout)
3. Usar `-Pversion=${env.VERSION}` en todos los comandos gradle

**Testing:**
- Ejecutar sin CUSTOM_VERSION → Debe usar gradle.properties
- Ejecutar con CUSTOM_VERSION=1.2.3 → Debe usar 1.2.3
- Verificar versión en nombre de JARs

**Duración:** +15 min implementación

---

## ⏳ FASE 4: PUBLICACIÓN MAVEN LOCAL

**Features a agregar:**

```groovy
parameters {
    booleanParam(
        name: 'PUBLISH_TO_MAVEN_LOCAL',
        defaultValue: false,
        description: '📦 Publicar a Maven Local (solo develop, opcional)'
    )
}

stage('📦 Publicar a Maven Local') {
    when {
        allOf {
            branch 'develop'
            expression { params.PUBLISH_TO_MAVEN_LOCAL == true }
        }
    }
    steps {
        sh './gradlew publishToMavenLocal'
    }
}
```

**Cambios:**
1. Agregar parámetro PUBLISH_TO_MAVEN_LOCAL
2. Agregar stage 'Publicar a Maven Local' (después de Artefactos)

**Testing:**
- Ejecutar en develop con PUBLISH=false → SKIPPED
- Ejecutar en develop con PUBLISH=true → EJECUTA
- Ejecutar en master → SKIPPED siempre

**Duración:** +15 min implementación

---

## ⏳ FASE 5: PUBLICACIÓN ARTIFACTORY

**Features a agregar:**

```groovy
parameters {
    choice(
        name: 'PUBLISH_TO_ARTIFACTORY',
        choices: ['AUTO', 'YES', 'NO'],
        description: 'AUTO=solo master, YES=forzar, NO=solo build'
    )
}

environment {
    ARTIFACTORY_URL = 'https://artifactory.cldevops.chl.bns/artifactory'
    ARTIFACTORY_RELEASE_REPO = 'libs-release-thirdparty'
    ARTIFACTORY_CREDS = credentials('Artifactory')
}

stage('🚀 Publicar a Artifactory') {
    when {
        allOf {
            branch pattern: "main|master", comparator: "REGEXP"
            expression { env.WILL_PUBLISH == 'true' }
        }
    }
    steps {
        sh """
            gradle publish \
                -Pversion=\${VERSION} \
                -PartifactoryUrl=\${ARTIFACTORY_URL}/\${ARTIFACTORY_RELEASE_REPO} \
                -PartifactoryUser=\${ARTIFACTORY_CREDS_USR} \
                -PartifactoryPassword=\${ARTIFACTORY_CREDS_PSW}
        """
    }
}
```

**Cambios:**
1. Agregar parámetro PUBLISH_TO_ARTIFACTORY
2. Agregar bloque `environment { }`
3. Agregar lógica para calcular WILL_PUBLISH en stage 'Calcular Versión'
4. Agregar stage 'Publicar a Artifactory'

**Pre-requisitos:**
- ☐ Credencial 'Artifactory' creada en Jenkins
- ☐ Verificar ARTIFACTORY_RELEASE_REPO correcto
- ☐ Verificar permisos de publicación

**Testing:**
- Ejecutar en develop → Stage SKIPPED
- Ejecutar en master con PUBLISH=NO → Stage SKIPPED
- Ejecutar en master con PUBLISH=AUTO → Stage EJECUTA

**Duración:** +30 min implementación

---

## ⏳ FASE 6: VERSIONADO INTELIGENTE

**Features a agregar:**

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
        return (response.status == 200) ? response.content.trim() : null
    } catch (Exception e) {
        return null
    }
}

def calculateNextVersion(currentVersion) {
    if (!currentVersion) return '1.0.0'
    def parts = currentVersion.split('\\.')
    def patch = parts[2].toInteger() + 1
    return "${parts[0]}.${parts[1]}.${patch}"
}

stage('🔢 Calcular Versión') {
    steps {
        script {
            if (!params.CUSTOM_VERSION?.trim()) {
                // Consultar Artifactory
                def latest = getLatestVersionFromArtifactory('com.scotia.qa', 'common')
                env.VERSION = calculateNextVersion(latest)
            }
        }
    }
}

stage('🔍 Verificar Duplicados') {
    when {
        expression { env.WILL_PUBLISH == 'true' }
    }
    steps {
        script {
            ['common', 'api-core', 'web-core', 'mobile-core'].each { module ->
                def url = "${ARTIFACTORY_URL}/${ARTIFACTORY_RELEASE_REPO}/com/scotia/qa/${module}/${env.VERSION}/${module}-${env.VERSION}.jar"
                def response = httpRequest(
                    url: url,
                    httpMode: 'HEAD',
                    validResponseCodes: '200,404',
                    authentication: 'Artifactory'
                )
                if (response.status == 200) {
                    error("❌ Versión ${env.VERSION} del módulo ${module} YA EXISTE")
                }
            }
        }
    }
}
```

**Cambios:**
1. Agregar funciones (al inicio del pipeline, antes de `pipeline { }`)
2. Modificar stage 'Calcular Versión' para usar funciones
3. Agregar stage 'Verificar Duplicados'

**Pre-requisitos:**
- ☐ Plugin: HTTP Request instalado
- ☐ API de Artifactory accesible

**Testing:**
- Consultar versión existente → Debe incrementar
- Intentar publicar versión existente → Debe fallar
- Primera publicación → Debe usar 1.0.0

**Duración:** +45 min implementación

---

## ⏳ FASE 7: FEATURES AVANZADAS

**Features a agregar:**

### **Coverage (Jacoco)**

```groovy
stage('📊 Coverage') {
    when {
        allOf {
            branch 'develop'
            expression { params.SKIP_TESTS != true }
        }
    }
    steps {
        sh 'gradle jacocoTestReport'
    }
    post {
        always {
            jacoco(
                execPattern: '**/build/jacoco/*.exec',
                classPattern: '**/build/classes',
                sourcePattern: '**/src/main/java',
                minimumInstructionCoverage: '70'
            )
        }
    }
}
```

### **Quality Gate**

```groovy
stage('🚦 Quality Gate') {
    when {
        branch 'develop'
    }
    steps {
        script {
            // Verificar coverage mínimo
            def coverage = // ... leer de Jacoco
            if (coverage < 70) {
                error("❌ Coverage ${coverage}% < 70% mínimo")
            }
        }
    }
}
```

### **Aprobar Publicación**

```groovy
stage('⏸️ Aprobar Publicación') {
    when {
        allOf {
            branch pattern: "main|master", comparator: "REGEXP"
            expression { env.WILL_PUBLISH == 'true' }
        }
    }
    steps {
        script {
            timeout(time: 10, unit: 'MINUTES') {
                input(
                    message: "¿Publicar versión ${env.VERSION} a Artifactory?",
                    ok: 'Publicar',
                    submitter: 'tech-lead,qa-lead'
                )
            }
        }
    }
}
```

### **Notificaciones Teams**

```groovy
environment {
    TEAMS_WEBHOOK = credentials('teams-webhook-qa')
}

post {
    success {
        script {
            // Office365Connector plugin
            office365ConnectorSend(
                webhookUrl: env.TEAMS_WEBHOOK,
                message: "✅ Build exitoso: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                status: 'Success'
            )
        }
    }
}
```

**Pre-requisitos:**
- ☐ Plugin: Jacoco instalado
- ☐ Plugin: Office 365 Connector instalado
- ☐ Credencial Teams webhook creada
- ☐ Configurar Jacoco en build.gradle

**Testing:**
- Verificar coverage en develop
- Verificar quality gate falla si coverage < 70%
- Verificar aprobación manual en master
- Verificar notificación Teams llega

**Duración:** +1-2 hrs implementación

---

## 📊 TIMELINE ESTIMADO

```
Fase 1 (MVP):              [===========] ✅ COMPLETADA (30 min)
Fase 2 (Parámetros):       [==] ⏳ Pendiente (10 min)
Fase 3 (Versionado):       [===] ⏳ Pendiente (15 min)
Fase 4 (Maven Local):      [===] ⏳ Pendiente (15 min)
Fase 5 (Artifactory):      [======] ⏳ Pendiente (30 min)
Fase 6 (Version API):      [=========] ⏳ Pendiente (45 min)
Fase 7 (Advanced):         [==================] ⏳ Pendiente (1-2 hrs)
────────────────────────────────────────────────────────────────
Total:                     ~4-5 horas
```

---

## 🎯 ESTADO ACTUAL

**FASE COMPLETADA:** 1 (MVP)  
**PRÓXIMA FASE:** 2 (Parámetros básicos)  
**BLOQUEADOR:** Probar Fase 1 en Jenkins primero

---

## 🚀 ACCIÓN INMEDIATA

```bash
# 1. Commit cambios
git add pipeline.jenkins *.md
git commit -m "feat: pipeline Jenkins FASE 1 MVP"
git push origin develop

# 2. Ir a Jenkins web
# 3. Ejecutar "Construir ahora"
# 4. Verificar que funciona ✅
# 5. Si funciona → Implementar Fase 2
```

---

**🏁 LISTO PARA PROBAR FASE 1**

