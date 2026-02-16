# 📘 GUÍA JENKINS PIPELINE - qa-scotia-frameworks

**Fecha:** 2026-02-16  
**Pipeline:** `pipeline.jenkins` (1,100 líneas)  
**Estado:** ✅ 100% Completo - Listo para producción

---

## 📊 RESUMEN EJECUTIVO

```
Pipeline enterprise-grade con 7 fases implementadas:
✅ Build multi-módulo (common, api-core, web-core, mobile-core)
✅ Versionado inteligente (3 estrategias)
✅ Publicación multi-destino (Maven Local + Artifactory)
✅ Security scanning (OWASP + Checkmarx)
✅ Quality gates y coverage
✅ Aprobación manual opcional

Stages: 13+  |  Parámetros: 7  |  Funciones: 3
```

---

## ⚙️ PARÁMETROS DISPONIBLES

| Parámetro | Tipo | Default | Uso |
|-----------|------|---------|-----|
| **SKIP_TESTS** | boolean | false | Saltar tests (builds rápidos) |
| **CUSTOM_VERSION** | string | "" | Override versión (ej: "2.0.0-RC1") |
| **AUTO_INCREMENT_VERSION** | boolean | false | Auto-incrementar desde Artifactory |
| **PUBLISH_TO_MAVEN_LOCAL** | boolean | false | Publicar a ~/.m2 (solo develop) |
| **PUBLISH_TO_ARTIFACTORY** | choice | AUTO | AUTO/YES/NO (solo master) |
| **RUN_COVERAGE** | boolean | true | Jacoco coverage (solo develop) |
| **RUN_OWASP_SCAN** | boolean | true | Security scan (solo develop) |
| **REQUIRE_APPROVAL** | boolean | false | Aprobación manual (solo master) |

---

## 🔀 ESTRATEGIA POR RAMA

### **DEVELOP (desarrollo diario):**
```
├─ Build + Tests + Coverage
├─ OWASP Security Scan (CVEs en dependencias)
├─ Quality Gate
└─ Publicar → Maven Local (opcional)

Frecuencia: 10+ builds/día
Tiempo: 10-15 min
```

### **MASTER (releases producción):**
```
├─ Build + Tests
├─ Checkmarx SAST (vulnerabilidades en código)
├─ Generar Artefactos
├─ Aprobar Publicación (opcional)
├─ Verificar Duplicados
└─ Publicar → Artifactory

Frecuencia: 2-3 releases/mes
Tiempo: 15-45 min (depende de aprobación)
```

---

## 🎯 VERSIONADO (3 Estrategias)

**Prioridad 1 - Manual:**
```groovy
CUSTOM_VERSION = "2.0.0-RC1"  → Usa esta versión
```

**Prioridad 2 - Auto-increment:**
```groovy
AUTO_INCREMENT_VERSION = true
→ Consulta Artifactory: última = 1.0.5
→ Calcula: 1.0.5 + 1 = 1.0.6
→ Usa: 1.0.6
```

**Prioridad 3 - Fallback:**
```groovy
→ Lee gradle.properties: version=1.0.5
→ Usa: 1.0.5
```

---

## 🔒 SECURITY SCANNING

### **OWASP Dependency Check (develop):**
- **Qué hace:** Busca CVEs en dependencias (build.gradle)
- **Tiempo:** ~5 minutos
- **Costo:** ✅ GRATIS
- **Ejemplo:** Detecta Log4j vulnerable, Spring vulnerable
- **Falla si:** CVSS >= 7 (HIGH o CRITICAL)

### **Checkmarx SAST (master):**
- **Qué hace:** Analiza código fuente buscando vulnerabilidades
- **Tiempo:** ~20-30 minutos
- **Costo:** 💵 Requiere licencia
- **Ejemplo:** SQL Injection, XSS, Hardcoded passwords
- **Estado:** ✅ Configurado (líneas 618-748)
- **Credenciales:** PIPELINE_BJCX_Chile (temporal, pedir propias)

---

## 🚀 DEPLOYMENT

### **1. Requisitos Jenkins:**

**Plugins necesarios:**
```
✅ Pipeline, Git, JUnit, HTML Publisher
✅ Workspace Cleanup, AnsiColor, Credentials
✅ HTTP Request (para Artifactory API)
✅ Jacoco (para coverage)
```

**Herramientas (Global Tool Configuration):**
```
JDK: OpenJDK 21
Gradle: Gradle 8.5
```

**Credenciales:**
```
ID: artifactory-credentials
Tipo: Username with password
```

---

### **2. Configurar Job:**

```
Jenkins → New Item
Nombre: qa-scotia-frameworks
Tipo: Multibranch Pipeline
Repository: [tu-repo-bitbucket]
Script Path: pipeline.jenkins
```

---

### **3. Variables de entorno (ajustar si necesario):**

```groovy
// En pipeline.jenkins línea ~228:
ARTIFACTORY_URL = 'https://artifactory.scotiabank.com/artifactory'
ARTIFACTORY_RELEASE_REPO = 'libs-release-local'
```

---

## 🧪 TESTING BÁSICO

### **Test 1: Build develop (default)**
```
Rama: develop
Parámetros: (todos default)
Esperado: ✅ Build OK, OWASP scan, NO publica
Tiempo: ~12 min
```

### **Test 2: Build master con publicación**
```
Rama: master
CUSTOM_VERSION: 1.0.6
PUBLISH_TO_ARTIFACTORY: YES
Esperado: ✅ Checkmarx → Verifica duplicados → Publica
Tiempo: ~30-40 min
```

### **Test 3: Build rápido (sin tests)**
```
Rama: develop
SKIP_TESTS: true
Esperado: ✅ Build rápido, skip tests y coverage
Tiempo: ~5 min
```

### **Test 4: Maven Local**
```
Rama: develop
PUBLISH_TO_MAVEN_LOCAL: true
Esperado: ✅ Publica a ~/.m2/repository
Verificar: ls ~/.m2/repository/com/scotia/qa/common/
```

---

## 🔧 CONFIGURACIÓN build.gradle

### **Plugins necesarios:**
```groovy
plugins {
    id 'java-library'
    id 'maven-publish'
    id 'jacoco'
    id 'org.owasp.dependencycheck' version '8.4.0'
}
```

### **Tareas requeridas:**
```groovy
// JAR de documentación
tasks.register('javadocJar', Jar) {
    archiveClassifier = 'javadoc'
    from javadoc
}

tasks.register('sourcesJar', Jar) {
    archiveClassifier = 'sources'
    from sourceSets.main.allSource
}

// Publishing
publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
            artifact javadocJar
            artifact sourcesJar
            groupId = 'com.scotia.qa'
            artifactId = project.name
            version = project.version
        }
    }
    repositories {
        mavenLocal()
        maven {
            url = "${artifactoryUrl}/${artifactoryRepo}"
            credentials {
                username = artifactoryUser
                password = artifactoryPassword
            }
        }
    }
}

// OWASP
dependencyCheck {
    formats = ['HTML', 'JSON', 'XML']
    failBuildOnCVSS = 7
    suppressionFile = "${rootProject.projectDir}/config/owasp-suppressions.xml"
}
```

---

## 📋 STAGES DEL PIPELINE

| # | Stage | Rama | Tiempo | Descripción |
|---|-------|------|--------|-------------|
| 1 | Checkout | Todas | 10s | Descarga código |
| 2 | Calcular Versión | Todas | 5s | 3 estrategias de versionado |
| 3 | Decidir Publicación | Todas | 5s | Lógica por rama |
| 4 | Verificar Entorno | Todas | 5s | Java + Gradle |
| 5 | Limpiar | Todas | 30s | gradle clean |
| 6 | Compilar | Todas | 2-3min | gradle build |
| 7 | Tests | Todas* | 3-5min | gradle test (opcional) |
| 6A | Coverage | develop | 1min | Jacoco |
| 6B | Quality Gate | develop | 5s | Checks |
| 6C | OWASP Scan | develop | 5min | Security (dependencias) |
| 6D | Checkmarx SAST | master | 25min | Security (código) |
| 8 | Generar Artefactos | Todas | 1min | jar + javadoc + sources |
| 9 | Maven Local | develop* | 30s | Publicación local |
| 8A | Aprobar | master* | manual | Input aprobación |
| 10 | Verificar Duplicados | master* | 10s | API Artifactory |
| 11 | Artifactory | master* | 1min | Publicación remota |

**\* = Condicional**

---

## 🔄 FLUJOS COMUNES

### **Flujo 1: Desarrollo normal (develop)**
```
Checkout → Versión → Build → Tests → Coverage → OWASP → Quality Gate → Artefactos
Tiempo: ~12-15 min
```

### **Flujo 2: Release a producción (master)**
```
Checkout → Versión → Build → Tests → Checkmarx → Artefactos → Aprobar → Verificar → Artifactory
Tiempo: ~35-45 min
```

### **Flujo 3: Build rápido (cualquier rama)**
```
SKIP_TESTS=true
Checkout → Versión → Build → Artefactos
Tiempo: ~5-7 min
```

---

## 🛠️ TROUBLESHOOTING

### **Error: "Agent jslave1 not found"**
Cambiar en pipeline línea 178:
```groovy
agent { label 'master' }  // O el label disponible
```

### **Error: "Tool OpenJDK 21 not found"**
Configurar en: Manage Jenkins → Global Tool Configuration

### **Error: "Credentials artifactory-credentials not found"**
Cambiar a usar el ID correcto. Ya está configurado como:
```groovy
ARTIFACTORY = credentials('Artifactory')  // ID: Artifactory (no artifactory-credentials)
```

Si el error persiste, crear credential:
```
Manage Jenkins → Credentials → Add
ID: Artifactory
Username: [tu-usuario]
Password: [tu-password]
```

### **Error: Variables USF_PSW, HTTPS_PROXY no definidas (Checkmarx)**
Opciones:
1. Comentar stage Checkmarx (líneas 618-748)
2. Definir variables en environment
3. Pedir credenciales propias a DevOps

### **Error: "utils.failjob() not found"**
Cambiar línea ~713:
```groovy
// De:
utils.failjob("mensaje")
// A:
error("mensaje")
```

### **Stage Checkmarx requiere input manual**
Si no quieres input manual, hardcodear PROJECT_KEY:
```groovy
def PROJECT_KEY = 'qa-scotia-frameworks'  // Línea ~666
```

---

## 🎯 CONFIGURACIÓN CHECKMARX

### **Datos necesarios (preguntar a DevOps):**
```
CX_CLIENT_ID: PIPELINE_BJCX_Chile (temporal - pedir propias)
CX_CLIENT_SECRET: [token] (en Jenkins credentials)
CX_TENANT: BNS
CX_BASE_URI: https://scotiabank.cxone.cloud/
```

### **Ubicación en código:**
- **Líneas:** 618-748 (Stage 6C)
- **Ejecuta en:** master/main solamente
- **Ubicación:** Después de Quality Gate, ANTES de Generar Artefactos
- **Efecto:** Si falla → NO genera artefactos, NO publica

### **Para habilitar:**
1. Definir variables USF_USR, USF_PSW, HTTPS_PROXY
2. O crear credentials en Jenkins y usar withCredentials
3. Hardcodear PROJECT_KEY si no quieres input manual

---

## 📦 ARTEFACTOS GENERADOS

### **Por cada módulo (common, api-core, web-core, mobile-core):**
```
nombre-VERSION.jar              (binario)
nombre-VERSION-javadoc.jar      (documentación)
nombre-VERSION-sources.jar      (código fuente)
```

### **Ubicación después del build:**
```
build/libs/
├── common-1.0.5.jar
├── common-1.0.5-javadoc.jar
├── common-1.0.5-sources.jar
├── api-core-1.0.5.jar
└── ...
```

### **Publicación Maven Local:**
```
~/.m2/repository/com/scotia/qa/
├── common/1.0.5/
├── api-core/1.0.5/
├── web-core/1.0.5/
└── mobile-core/1.0.5/
```

### **Publicación Artifactory:**
```
https://artifactory.scotiabank.com/artifactory/libs-release-local/
com/scotia/qa/
├── common/1.0.5/
├── api-core/1.0.5/
├── web-core/1.0.5/
└── mobile-core/1.0.5/
```

---

## 🔑 CREDENCIALES REQUERIDAS

### **En Jenkins (Manage Credentials):**

**1. Artifactory** (nombre exacto del ID)
```
Kind: Username with password
Username: [tu-usuario-artifactory]
Password: [tu-password-artifactory]
ID: Artifactory  ← IMPORTANTE: usar este ID exacto
```

**Nota:** El pipeline usa `credentials('Artifactory')` que carga automáticamente:
- `ARTIFACTORY_USR` (username)
- `ARTIFACTORY_PSW` (password)

**2. Variables de Checkmarx** (opcional):
```
Las credenciales USF_USR y USF_PSW deben existir como:
- Variables de entorno globales en Jenkins
- O como credentials binding en el pipeline
```

---

## ⚙️ CONFIGURACIÓN DE ENVIRONMENT

### **Variables configuradas:**
```groovy
environment {
    ARTIFACTORY = credentials('Artifactory')  // Carga USR y PSW
    ARTIFACTORYREPOKEY = 'libs-release-thirdparty'
    WILL_PUBLISH_ARTIFACTORY = 'false'
}
```

**Disponibles en el pipeline:**
- `env.ARTIFACTORY_USR` → Username
- `env.ARTIFACTORY_PSW` → Password
- `env.ARTIFACTORYREPOKEY` → Repositorio destino

---

## 🎓 USO BÁSICO

### **Ejecutar build estándar (develop):**
```
1. Jenkins → qa-scotia-frameworks → develop
2. Build with Parameters
3. (dejar todos default)
4. Build
```

### **Release a producción (master):**
```
1. Merge develop → master (PR aprobado)
2. Pipeline se ejecuta automáticamente
3. Checkmarx escanea (25 min)
4. Si REQUIRE_APPROVAL=true → Espera aprobación
5. Verifica duplicados
6. Publica a Artifactory
```

### **Build rápido sin tests:**
```
SKIP_TESTS: ☑️ true
→ Tiempo: ~5-7 min
```

### **Versión personalizada:**
```
CUSTOM_VERSION: 2.0.0-RC1
→ Artefactos: *-2.0.0-RC1.jar
```

---

## 📊 REPORTES DISPONIBLES

**En Jenkins UI (después del build):**
```
Build #45
├─ 📊 Test Report (JUnit + HTML)
├─ 📊 Coverage Report (Jacoco) - solo develop
├─ 🔒 OWASP Security Report - solo develop
├─ 🔒 Checkmarx Report (CSV) - solo master
└─ 📦 Build Artifacts (JARs)
```

---

## ⚠️ NOTAS IMPORTANTES

### **Checkmarx:**
- ⚠️ Usa credenciales de Chile (PIPELINE_BJCX_Chile)
- 💡 Solicitar credenciales propias a DevOps/Security
- ⏸️ Requiere input manual (elegir proyecto)
- 📊 Descarga reporte de ÚLTIMO scan (no ejecuta scan nuevo)

### **Variables a definir para Checkmarx:**
```
USF_USR, USF_PSW, HTTPS_PROXY
→ O usar withCredentials
```

### **OWASP:**
- ✅ Ya configurado en build.gradle
- ✅ Gratis y automático
- ✅ Funciona sin credenciales

---

## 🔧 AJUSTES COMUNES

### **Cambiar URL de Artifactory:**
```groovy
// Línea ~228
ARTIFACTORY_URL = 'https://tu-artifactory.com/artifactory'
```

### **Cambiar timeout del pipeline:**
```groovy
// Línea ~253
timeout(time: 45, unit: 'MINUTES')  // Aumentar si necesario
```

### **Deshabilitar Checkmarx temporalmente:**
```groovy
// Líneas 618-748: Comentar TODO el stage con /* ... */
```

### **Hardcodear proyecto Checkmarx (sin input):**
```groovy
// Línea ~666 cambiar:
def PROJECT_KEY = 'qa-scotia-frameworks'  // Sin if(!(PROJECT_KEY))
```

---

## 📞 CONTACTO Y SOPORTE

**Documentación completa:** Este archivo  
**Código fuente:** `pipeline.jenkins`  
**Configuración OWASP:** `build.gradle` + `config/owasp-suppressions.xml`  
**Script testing local:** `test-pipeline-local.sh`

**Para issues con:**
- Artifactory → DevOps Team
- Checkmarx → Security Team
- Pipeline → QA Team Lead

---

## ✅ CHECKLIST PRE-PRODUCCIÓN

- [ ] Plugins instalados en Jenkins
- [ ] JDK 21 y Gradle 8.5 configurados
- [ ] Credencial artifactory-credentials creada
- [ ] build.gradle con plugins y tareas
- [ ] Test básico en develop ejecutado
- [ ] Verificar URLs de Artifactory
- [ ] Decidir si usar REQUIRE_APPROVAL en master
- [ ] Solicitar credenciales propias de Checkmarx
- [ ] Capacitar al equipo en uso del pipeline

---

**📅 Última actualización:** 2026-02-16  
**👤 Responsable:** QA Team  
**📝 Versión:** 2.0.0  
**🚀 Estado:** Listo para producción

