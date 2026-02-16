# 🎯 RESUMEN EJECUTIVO: ESTRATEGIA DE PIPELINE

**Fecha:** 2025-02-15  
**Framework:** qa-scotia-frameworks  
**Propósito:** Decisiones finales para implementación de CI/CD

---

## ✅ DECISIONES FINALES

### **1️⃣ BRANCHING STRATEGY**

```
✅ Estrategia: GitHub Flow Simplificado (develop + master)
```

| Rama | Pipeline | Publicación | Trigger |
|------|----------|-------------|---------|
| **develop** | ✅ Build + Test + Coverage + CVE | 📦 Maven Local (OPCIONAL) | ⚙️ Automático (cada push) |
| **master** | ✅ Build + Test + Publish | ✅ Artifactory (RELEASE) | ⚙️ Automático + Aprobación |
| feature/* | ❌ NO pipeline | ❌ NO publica | - |

**Flujo:**
```
feature → PR → develop → Jenkins valida → Acumula features → PR → master → Jenkins publica
```

---

### **2️⃣ VERSIONADO**

```
✅ Estrategia: Consultar Artifactory API + Auto-incremento PATCH
```

**Cálculo de versión:**
1. Consultar última versión en Artifactory (API: `/api/search/latestVersion`)
2. Incrementar PATCH automáticamente: `1.0.5 → 1.0.6`
3. Override manual con parámetro `CUSTOM_VERSION` (hotfixes/RCs)

**Formato:** `MAJOR.MINOR.PATCH`
- **PATCH:** Auto-incremento (bugs)
- **MINOR:** Manual en `gradle.properties` (features)
- **MAJOR:** Manual con `CUSTOM_VERSION` (breaking changes)

---

### **3️⃣ JENKINS CONFIGURATION**

```
✅ Tipo: Multibranch Pipeline
✅ Ramas: develop + master SOLAMENTE
✅ Script: pipeline.jenkins (en raíz del proyecto)
✅ Trigger: Polling SCM (5 min) + Webhook
```

**El archivo `pipeline.jenkins` DEBE existir en AMBAS ramas** (develop y master) con lógica condicional:

```groovy
// Stages específicos de develop:
stage('Coverage') {
    when { branch 'develop' }
    // ...
}

// Stages específicos de master:
stage('Publicar') {
    when { branch pattern: "main|master", comparator: "REGEXP" }
    // ...
}
```

---

### **4️⃣ PROTECCIÓN DE MASTER**

```
✅ Bitbucket Branch Permissions:
   ├── Require Pull Request: YES
   ├── Min Approvals: 1
   ├── Approvers: Tech Lead + QA Lead SOLAMENTE
   ├── Require Build Passing: YES (develop debe estar ✅)
   └── Merge Strategy: Squash
```

**Resultado:** Los developers NO pueden hacer push/merge directo a master.

---

### **5️⃣ PIPELINE EN DEVELOP**

```
┌─────────────────────────────────────────────────────┐
│ ✅ Build (compilar todos los módulos)              │
│ ✅ Tests (unit tests)                               │
│ ✅ Coverage (Jacoco, min 70%)                       │
│ ✅ CVE Scanning (vulnerabilidades)                  │
│ ✅ Quality Gates (validaciones)                     │
│ 📦 Maven Local (OPCIONAL)                           │
│    └── Solo si PUBLISH_TO_MAVEN_LOCAL=true         │
│    └── Útil para testing del framework             │
│ ❌ NO PUBLICA a Artifactory                        │
└─────────────────────────────────────────────────────┘

Tiempo estimado: 8-10 minutos (9-11 si publica a Maven Local)
Propósito: Feedback rápido a developers + testing opcional del framework
```

---

### **6️⃣ PIPELINE EN MASTER**

```
┌─────────────────────────────────────────────────────┐
│ ✅ Build                                            │
│ ✅ Tests (validación rápida)                        │
│ 🔍 Consultar última versión en Artifactory         │
│    └── API: GET /api/search/latestVersion          │
│ 📈 Calcular siguiente versión (auto PATCH++)       │
│    └── 1.0.5 → 1.0.6                                │
│ 🛡️ Verificar duplicados (HEAD cada módulo)        │
│    └── Si existe → ABORTAR                          │
│ ⏸️ Aprobar publicación (input step)                │
│    └── Manual: ¿Publicar 1.0.6? [SÍ/NO]            │
│ 🚀 Publicar a Artifactory (RELEASE)                │
│    └── Versión INMUTABLE (no sobrescribible)       │
└─────────────────────────────────────────────────────┘

Tiempo estimado: 10-12 minutos (incluyendo aprobación)
Propósito: Release controlado a producción
```

---

### **7️⃣ PARÁMETROS DEL BUILD**

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
• Manual: Para hotfixes/RCs
• Ejemplo: 1.2.0-hotfix, 2.0.0-rc1'''
    )
    
    booleanParam(
        name: 'SKIP_TESTS',
        defaultValue: false,
        description: '⚠️ Saltar tests (NO recomendado)'
    )
}
```

---

### **8️⃣ VERIFICACIÓN DE DUPLICADOS**

**CRÍTICO:** Antes de publicar, verificar que la versión NO existe en Artifactory:

```groovy
def checkUrl = "${ARTIFACTORY_URL}/${REPO}/com/scotia/qa/${module}/${version}/${module}-${version}.jar"

httpRequest(url: checkUrl, httpMode: 'HEAD', validResponseCodes: '200,404')
├── Status 200 → ❌ ERROR: Versión ya existe
└── Status 404 → ✅ OK: Versión disponible
```

**Si existe:** Pipeline falla con mensaje claro + soluciones.

---

### **9️⃣ CONSULTA DE ARTIFACTORY API**

**Endpoint:** `GET /api/search/latestVersion`

**Request:**
```
https://artifactory.cldevops.chl.bns/artifactory/api/search/latestVersion
?g=com.scotia.qa
&a=common
&repos=libs-release-thirdparty
```

**Response:**
```
200 OK
Content: "1.0.5"
```

**404 si no existe** (primera publicación).

---

### **🔟 PUBLICACIÓN: MAVEN LOCAL vs ARTIFACTORY**

| Escenario | Maven Local | Artifactory | Notas |
|-----------|-------------|-------------|-------|
| Developer local | ✅ Siempre | ❌ NO | `./gradlew publishToMavenLocal` |
| CI/CD develop (default) | ❌ NO | ❌ NO | Solo valida (más rápido) |
| CI/CD develop (opcional) | ✅ Si `PUBLISH_TO_MAVEN_LOCAL=true` | ❌ NO | Para testing del framework |
| CI/CD master | ❌ NO | ✅ SIEMPRE | Release oficial |
| Hotfix urgente | ❌ NO | ✅ Directo | Publicación inmediata |

**Comandos:**

```bash
# Maven Local (develop opcional):
./gradlew publishToMavenLocal

# Artifactory (master):
./gradlew publish \
  -Pversion=1.0.6 \
  -PartifactoryUrl=https://artifactory.../libs-release-thirdparty \
  -PartifactoryUser=${ARTIFACTORY_CREDS_USR} \
  -PartifactoryPassword=${ARTIFACTORY_CREDS_PSW}
```

---

## 📋 CHECKLIST DE IMPLEMENTACIÓN

### **Fase 1: Jenkins (1 hora)**

```
☐ Crear Multibranch Pipeline Job
☐ Configurar Branch Sources → Bitbucket
☐ Filtrar branches: (develop|main|master)
☐ Script Path: pipeline.jenkins
☐ Instalar plugin: HTTP Request
☐ Verificar credencial: Artifactory
☐ Configurar Polling SCM: H/5 * * * *
```

---

### **Fase 2: Bitbucket (30 min)**

```
☐ Repository Settings → Branch Permissions → master
  ├── ☑ Prevent deletion
  ├── ☑ Require PR
  ├── ☑ Min 1 approval
  └── ☑ Build passing
☐ Definir usuarios con permiso de merge (Tech Lead, QA Lead)
☐ Merge strategy: Squash
```

---

### **Fase 3: Código (2 horas)**

```
☐ Actualizar pipeline.jenkins:
  ├── Agregar getLatestVersionFromArtifactory()
  ├── Agregar calculateNextVersion()
  ├── Actualizar stage 'Calcular Versión'
  ├── Agregar stage 'Verificar Duplicados'
  ├── Agregar stage 'Aprobar Publicación' (master)
  └── Agregar lógica when { branch }
☐ Copiar pipeline.jenkins a develop
☐ Copiar pipeline.jenkins a master
```

---

### **Fase 4: Pruebas (2 horas)**

```
☐ Test 1: Merge feature → develop
  └── Verificar: Build ✅, Tests ✅, NO publica

☐ Test 2: Merge develop → master
  ├── Verificar: Versión calculada correctamente
  ├── Verificar: Verificación de duplicados funciona
  ├── Aprobar publicación manualmente
  └── Verificar: Publicado en Artifactory ✅

☐ Test 3: Verificar duplicados (re-ejecutar master)
  └── Debe fallar: "Versión 1.0.X ya existe"

☐ Test 4: Módulo consumer
  └── Importar librería y verificar que funciona
```

---

## ⚠️ PUNTOS CRÍTICOS A RECORDAR

### **1. El archivo pipeline.jenkins debe estar en AMBAS ramas**

```
❌ INCORRECTO: Solo en master
✅ CORRECTO: En develop Y master (mismo archivo con lógica condicional)
```

**Razón:** Jenkins Multibranch lee el archivo de cada rama.

---

### **2. Versiones RELEASE son INMUTABLES**

```
❌ NO se puede sobrescribir 1.0.5 en Artifactory
✅ Se debe publicar 1.0.6 (nueva versión)
```

**Protección:** Stage "Verificar Duplicados" falla si ya existe.

---

### **3. Develop NO publica (solo valida)**

```
develop:
  ✅ Build + Test + Coverage + CVE
  ❌ NO publica (ni Maven Local ni Artifactory)

master:
  ✅ Build + Test
  ✅ PUBLICA a Artifactory
```

**Razón:** Develop es integración continua, master es release.

---

### **4. Consultar Artifactory es la fuente de verdad**

```
NO usar:
  ❌ Git tags (se pueden borrar)
  ❌ Commits (no reflejan publicaciones)
  ❌ gradle.properties (se desincroniza)

SÍ usar:
  ✅ Artifactory API (fuente única de verdad)
```

---

### **5. Hotfixes van directo a master**

```
Urgencia:
  hotfix/critical → master (DIRECTO, sin develop)
  └── Después: master → develop (backport)
```

**No pasar por develop primero** (demora innecesaria en emergencias).

---

## 🚀 PRÓXIMOS PASOS

### **Inmediato (hoy):**

1. ✅ Revisar documentación completa:
   - `INVESTIGACION-ESTRATEGIA-PIPELINE.md`
   - `DIAGRAMAS-ESTRATEGIA-PIPELINE.md`
   - Este resumen

2. ✅ Decidir si implementar o ajustar estrategia

3. ✅ Preparar reunión con Tech Lead para aprobar estrategia

---

### **Siguiente sesión:**

1. Implementar cambios en `pipeline.jenkins`
2. Configurar Jenkins Multibranch
3. Configurar permisos en Bitbucket
4. Ejecutar pruebas

---

## 📚 DOCUMENTOS GENERADOS

1. ✅ `INVESTIGACION-ESTRATEGIA-PIPELINE.md` (análisis completo, 14 secciones)
2. ✅ `DIAGRAMAS-ESTRATEGIA-PIPELINE.md` (10 diagramas visuales)
3. ✅ `RESUMEN-EJECUTIVO-PIPELINE.md` (este documento)

**Total:** ~1500 líneas de análisis + recomendaciones + diagramas

---

## 💬 PREGUNTAS FRECUENTES

### **¿Por qué 2 ramas (develop + master)?**

✅ **Feedback temprano** (bugs detectados antes de master)  
✅ **Quality gates continuos** (coverage, CVEs)  
✅ **Releases más seguros** (ya validado en develop)

---

### **¿Por qué consultar Artifactory en vez de Git tags?**

✅ **Fuente única de verdad** (lo que está publicado)  
✅ **Evita duplicados** (verificación garantizada)  
✅ **No depende de Git** (funciona aunque borres tags)

---

### **¿Por qué aprobación manual en master?**

✅ **Control de releases** (no publicar por error)  
✅ **Compliance** (trazabilidad de quién aprobó)  
✅ **Prevención** (último checkpoint antes de producción)

---

### **¿Se puede publicar a Maven Local desde Jenkins?**

✅ **SÍ, en rama develop mediante parámetro `PUBLISH_TO_MAVEN_LOCAL`**

**Casos de uso:**
- Developer quiere probar framework localmente sin esperar a master
- Testing de cambios antes de release
- Validación de módulos consumidores

**Cómo usarlo:**
```
Jenkins (develop):
├── Build with Parameters
├── PUBLISH_TO_MAVEN_LOCAL: YES ✅
└── Resultado: Publica en Maven Local del servidor Jenkins

NOTA: NO publica en tu Maven Local (está en servidor Jenkins)
      Para testing local: ./gradlew publishToMavenLocal en tu máquina
```

**Default:** `false` (no publica, pipeline más rápido)

---

### **¿Cómo incrementar MINOR o MAJOR?**

**MINOR:** Actualizar `gradle.properties`: `version=1.1.0`  
**MAJOR:** Usar `CUSTOM_VERSION=2.0.0` en Jenkins

---

**🎯 FIN DEL ANÁLISIS - LISTO PARA IMPLEMENTACIÓN**

