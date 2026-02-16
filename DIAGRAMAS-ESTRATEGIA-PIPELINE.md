# 📊 ESTRATEGIA DE PIPELINE - DIAGRAMAS Y FLUJOS

**Fecha:** 2025-02-15  
**Propósito:** Visualización de la estrategia completa de CI/CD

---

## 🌳 DIAGRAMA 1: ESTRATEGIA DE BRANCHING

```
                    PRODUCCIÓN (Artifactory)
                           ↑
                           │ publish (versión inmutable)
                           │
┌──────────────────────────┴─────────────────────────────┐
│                       MASTER                            │
│  ┌──────────────────────────────────────────────────┐ │
│  │ ✅ Jenkins Pipeline                               │ │
│  │ • Build + Test                                    │ │
│  │ • Consultar versión Artifactory                   │ │
│  │ • Verificar duplicados                            │ │
│  │ • Aprobación manual                               │ │
│  │ • Publicar a Artifactory                          │ │
│  └──────────────────────────────────────────────────┘ │
└────────────────────────▲───────────────────────────────┘
                         │
                         │ Pull Request (con approval)
                         │
┌────────────────────────┴───────────────────────────────┐
│                      DEVELOP                            │
│  ┌──────────────────────────────────────────────────┐ │
│  │ ✅ Jenkins Pipeline                               │ │
│  │ • Build + Test                                    │ │
│  │ • Coverage (Jacoco > 70%)                         │ │
│  │ • CVE Scanning                                    │ │
│  │ • Quality Gates                                   │ │
│  │ • ❌ NO publica                                   │ │
│  └──────────────────────────────────────────────────┘ │
└────▲────────▲────────▲────────▲────────────────────────┘
     │        │        │        │
     │        │        │        │ Pull Requests
     │        │        │        │
  ┌──┴──┐  ┌─┴───┐  ┌─┴────┐  ┌┴────────┐
  │feat/│  │feat/│  │bugfix│  │hotfix/  │
  │  A  │  │  B  │  │  /C  │  │urgent   │
  └─────┘  └─────┘  └──────┘  └─────────┘
    ❌       ❌       ❌         ✅ directo
  (no       (no      (no        a master
 Jenkins) Jenkins) Jenkins)   si crítico
```

---

## 🔄 DIAGRAMA 2: FLUJO DE DESARROLLO COMPLETO

```
┌─────────────────────────────────────────────────────────────┐
│ DEVELOPER LOCAL                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  git checkout -b feature/nueva-funcionalidad               │
│  [Developer escribe código + tests]                         │
│  ./gradlew test publishToMavenLocal  ← Prueba local        │
│  git commit -m "feat: nueva funcionalidad"                 │
│  git push origin feature/nueva-funcionalidad               │
│                                                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ BITBUCKET: PULL REQUEST                                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  feature/nueva-funcionalidad  →  develop                   │
│  ├── Code Review (otro developer)                           │
│  ├── Discusiones / Comentarios                             │
│  └── Approval ✅                                            │
│                                                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼ Merge to develop
┌─────────────────────────────────────────────────────────────┐
│ JENKINS PIPELINE (DEVELOP)                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  🔽 Checkout                                                │
│  🔢 Calcular Versión (NO se usa, solo validación)          │
│  🔍 Verificar Entorno                                       │
│  🧹 Limpiar                                                 │
│  🔨 Compilar (todos los módulos)                            │
│  🧪 Tests (unit tests)                         ← 3-5 min   │
│  📊 Coverage (Jacoco, min 70%)                 ← 2 min     │
│  🛡️ CVE Scanning (vulnerabilidades)            ← 3 min     │
│  🚦 Quality Gate (validaciones)                ← 1 min     │
│  📦 Artefactos (genera JARs en workspace)                   │
│  📦 Maven Local (OPCIONAL)                     ← 1 min     │
│     └── Solo si PUBLISH_TO_MAVEN_LOCAL=true                │
│     └── Útil para testing del framework                    │
│  ❌ NO PUBLICA a Artifactory                                │
│                                                             │
│  Result: ✅ SUCCESS → Feedback a developer                 │
│         ❌ FAILED  → Developer corrige                     │
│                                                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           │ (Acumular features)
                           │
                           ▼ Cuando hay suficientes features
┌─────────────────────────────────────────────────────────────┐
│ BITBUCKET: PULL REQUEST (RELEASE)                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  develop  →  master                                         │
│  ├── Code Review (Tech Lead)                                │
│  ├── Verificar tests passing en develop                     │
│  ├── Verificar coverage > 70%                               │
│  ├── Verificar no hay CVEs críticos                         │
│  └── Approval ✅ (solo Tech Lead / QA Lead)                 │
│                                                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼ Merge to master
┌─────────────────────────────────────────────────────────────┐
│ JENKINS PIPELINE (MASTER)                                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  🔽 Checkout                                                │
│  🔢 Calcular Versión                                        │
│     ├── Consultar Artifactory API:                          │
│     │   GET /api/search/latestVersion                       │
│     │   ?g=com.scotia.qa&a=common                           │
│     │   Response: "1.0.5"                                   │
│     ├── Incrementar: 1.0.5 → 1.0.6                          │
│     └── VERSION=1.0.6                                       │
│  🔍 Verificar Duplicados                                    │
│     ├── HEAD libs-release/.../common/1.0.6/common-1.0.6.jar│
│     ├── HEAD libs-release/.../api-core/1.0.6/...           │
│     ├── HEAD libs-release/.../web-core/1.0.6/...           │
│     └── HEAD libs-release/.../mobile-core/1.0.6/...        │
│     └── Si 200 → ERROR (ya existe)                          │
│         Si 404 → OK (continuar)                             │
│  🔍 Verificar Entorno                                       │
│  🧹 Limpiar                                                 │
│  🔨 Compilar                                                │
│  🧪 Tests (validación rápida)                               │
│  📦 Artefactos (JAR + sources + javadoc)                    │
│  ⏸️  Aprobar Publicación                                    │
│     └── Input: ¿Publicar 1.0.6? [PUBLICAR / CANCELAR]      │
│  🚀 Publicar                                                │
│     └── gradle publish                                      │
│         -Pversion=1.0.6                                     │
│         -PartifactoryUrl=...                                │
│                                                             │
│  Result: ✅ SUCCESS                                         │
│          📦 Versión 1.0.6 publicada e INMUTABLE             │
│                                                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ ARTIFACTORY                                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  libs-release-thirdparty/                                   │
│  └── com/scotia/qa/                                         │
│      ├── common/1.0.6/                                      │
│      │   ├── common-1.0.6.jar                               │
│      │   ├── common-1.0.6-sources.jar                       │
│      │   └── common-1.0.6-javadoc.jar                       │
│      ├── api-core/1.0.6/                                    │
│      ├── web-core/1.0.6/                                    │
│      └── mobile-core/1.0.6/                                 │
│                                                             │
│  🔒 VERSIÓN INMUTABLE (no se puede sobrescribir)           │
│                                                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ MÓDULOS DE PRUEBA (Consumers)                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  dependencies {                                             │
│      implementation 'com.scotia.qa:common:1.0.6'            │
│      implementation 'com.scotia.qa:api-core:1.0.6'          │
│      implementation 'com.scotia.qa:web-core:1.0.6'          │
│      implementation 'com.scotia.qa:mobile-core:1.0.6'       │
│  }                                                          │
│                                                             │
│  ./gradlew test → Descarga de Artifactory automáticamente  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚡ DIAGRAMA 3: FLUJO DE HOTFIX (URGENTE)

```
┌─────────────────────────────────────────────────────────────┐
│ PRODUCCIÓN CON BUG CRÍTICO                                  │
│ ❌ Bug en versión 1.0.6 en producción                       │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ DEVELOPER                                                   │
│                                                             │
│  git checkout master                                        │
│  git checkout -b hotfix/critical-bug                        │
│  [Fix crítico]                                              │
│  ./gradlew test publishToMavenLocal  ← Prueba local        │
│  git commit -m "hotfix: corrige bug crítico"               │
│  git push origin hotfix/critical-bug                        │
│                                                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ BITBUCKET: PULL REQUEST (URGENTE)                           │
│                                                             │
│  hotfix/critical-bug  →  master  (DIRECTO, SIN DEVELOP)    │
│  ├── Code Review (Tech Lead - RÁPIDO)                       │
│  └── Approval ✅                                            │
│                                                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼ Merge to master
┌─────────────────────────────────────────────────────────────┐
│ JENKINS (MASTER)                                            │
│                                                             │
│  Versión automática: 1.0.6 → 1.0.7                          │
│  O versión manual: 1.0.6-hotfix                             │
│  Pipeline completo (5-8 min)                                │
│  🚀 Publica 1.0.7 a Artifactory                             │
│                                                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ BACKPORT A DEVELOP                                          │
│                                                             │
│  git checkout develop                                       │
│  git merge master  ← Trae el hotfix a develop              │
│  git push origin develop                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔐 DIAGRAMA 4: PROTECCIÓN DE MASTER

```
┌─────────────────────────────────────────────────────────────┐
│ BITBUCKET: CONFIGURACIÓN DE master                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Branch Permissions:                                        │
│  ├── ☑ Prevent deletion                                    │
│  ├── ☑ Prevent changes without PR                          │
│  ├── ☑ Require at least 1 approval                         │
│  ├── ☑ Require all tasks to be resolved                    │
│  └── ☑ Require passing build (Jenkins develop = ✅)        │
│                                                             │
│  Merge Permissions:                                         │
│  ├── ✅ Tech Lead (puede hacer merge)                      │
│  ├── ✅ QA Lead (puede hacer merge)                        │
│  └── ❌ Developers (NO pueden hacer merge directo)         │
│                                                             │
│  Merge Strategy:                                            │
│  └── Squash commits (commits limpios en master)            │
│                                                             │
└─────────────────────────────────────────────────────────────┘

                           │
                           ▼

┌─────────────────────────────────────────────────────────────┐
│ INTENTOS DE PUSH DIRECTO A MASTER                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Developer intenta:                                         │
│  $ git push origin master                                   │
│                                                             │
│  ❌ Bitbucket responde:                                     │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ Error: You're not allowed to push directly to master │ │
│  │ Use a Pull Request instead                            │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘

                           │
                           ▼

┌─────────────────────────────────────────────────────────────┐
│ FLUJO CORRECTO: VIA PULL REQUEST                            │
│                                                             │
│  1. Developer crea PR: develop → master                     │
│  2. Jenkins (develop) debe estar ✅                         │
│  3. Tech Lead revisa código                                 │
│  4. Tech Lead aprueba ✅                                    │
│  5. Tech Lead hace merge                                    │
│  6. Jenkins (master) se dispara automáticamente             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 DIAGRAMA 5: VERSIONADO AUTOMÁTICO (ARTIFACTORY API)

```
┌─────────────────────────────────────────────────────────────┐
│ JENKINS PIPELINE (STAGE: Calcular Versión)                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
           ┌───────────────────────────────┐
           │ ¿CUSTOM_VERSION especificada? │
           └───────────┬───────────────────┘
                       │
         ┌─────────────┴──────────────┐
         │                            │
        SÍ                           NO
         │                            │
         ▼                            ▼
┌──────────────────┐      ┌────────────────────────┐
│ Usar versión     │      │ Consultar Artifactory  │
│ manual           │      │ API                    │
│                  │      │                        │
│ VERSION =        │      │ GET /api/search/       │
│ "1.2.0-hotfix"   │      │   latestVersion        │
│                  │      │ ?g=com.scotia.qa       │
│ ✅ Continuar     │      │ &a=common              │
└──────────────────┘      │ &repos=libs-release... │
                          └────────┬───────────────┘
                                   │
                          ┌────────┴────────┐
                          │                 │
                    Status 200         Status 404
                          │                 │
                          ▼                 ▼
              ┌────────────────────┐  ┌──────────────────┐
              │ Versión encontrada │  │ Primera          │
              │ Response: "1.0.5"  │  │ publicación      │
              └────────┬───────────┘  └────────┬─────────┘
                       │                       │
                       │                       │
                       ▼                       ▼
              ┌────────────────────┐  ┌──────────────────┐
              │ Incrementar PATCH  │  │ Usar versión     │
              │ 1.0.5 → 1.0.6      │  │ base: 1.0.0      │
              └────────┬───────────┘  └────────┬─────────┘
                       │                       │
                       └───────────┬───────────┘
                                   │
                                   ▼
                    ┌──────────────────────────┐
                    │ VERSION = "1.0.6"        │
                    │ WILL_PUBLISH = "true"    │
                    └──────────┬───────────────┘
                               │
                               ▼
                    ┌──────────────────────────┐
                    │ Continuar pipeline       │
                    └──────────────────────────┘
```

---

## 🛡️ DIAGRAMA 6: VERIFICACIÓN DE DUPLICADOS

```
┌─────────────────────────────────────────────────────────────┐
│ JENKINS PIPELINE (STAGE: Verificar Duplicados)              │
│ Versión a publicar: 1.0.6                                   │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
            ┌──────────────────────────────┐
            │ Para cada módulo:            │
            │ • common                     │
            │ • api-core                   │
            │ • web-core                   │
            │ • mobile-core                │
            └──────────┬───────────────────┘
                       │
                       ▼
        ┌──────────────────────────────────────┐
        │ HEAD artifactory/.../                │
        │   com/scotia/qa/common/1.0.6/        │
        │   common-1.0.6.jar                   │
        └──────────┬───────────────────────────┘
                   │
          ┌────────┴────────┐
          │                 │
     Status 200        Status 404
          │                 │
          ▼                 ▼
┌──────────────────┐  ┌────────────────────┐
│ ❌ YA EXISTE     │  │ ✅ DISPONIBLE      │
│                  │  │                    │
│ ABORTAR PIPELINE │  │ Continuar con      │
│                  │  │ siguiente módulo   │
│ Error:           │  └────────┬───────────┘
│ "Versión 1.0.6   │           │
│  ya existe en    │           │
│  Artifactory"    │           ▼
│                  │  ┌────────────────────┐
│ 🔧 Soluciones:   │  │ Todos OK?          │
│ 1. Incrementar   │  └────────┬───────────┘
│    versión       │           │
│ 2. Usar          │          SÍ
│    CUSTOM_VERSION│           │
│ 3. Usar          │           ▼
│    -hotfix-N     │  ┌────────────────────┐
└──────────────────┘  │ ✅ Continuar a     │
                      │ stage Publicar     │
                      └────────────────────┘
```

---

## 🎯 DIAGRAMA 7: PARÁMETROS DEL BUILD

```
┌─────────────────────────────────────────────────────────────┐
│ JENKINS: Build with Parameters                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  PUBLISH_TO_ARTIFACTORY:                                    │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ ○ AUTO     ← Solo si rama = master (recomendado)     │ │
│  │ ○ YES      ← Forzar publicación ⚠️                   │ │
│  │ ● NO       ← Solo build + tests (default)            │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  PUBLISH_TO_MAVEN_LOCAL:                                    │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ ☐ Publicar a Maven Local (opcional)                   │ │
│  │                                                       │ │
│  │ • Solo disponible en rama develop                    │ │
│  │ • Útil para testing del framework                    │ │
│  │ • Default: NO (más rápido)                           │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  CUSTOM_VERSION:                                            │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ [          ]  ← Vacío = auto-calculada                │ │
│  │ Ejemplos:                                             │ │
│  │ • 1.2.0-hotfix                                        │ │
│  │ • 2.0.0-rc1                                           │ │
│  │ • 1.1.5-beta                                          │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  SKIP_TESTS:                                                │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ ☐ Saltar tests ⚠️ (NO recomendado)                   │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  RUN_CVE_SCAN:                                              │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ ☑ Escanear vulnerabilidades CVE (recomendado)        │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  [ Construir ]                                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📈 DIAGRAMA 8: ESTRATEGIA DE VERSIONADO SEMÁNTICO

```
┌─────────────────────────────────────────────────────────────┐
│ VERSIONADO SEMÁNTICO: MAJOR.MINOR.PATCH                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  MAJOR (Breaking Changes):                                  │
│  ├── Cambios incompatibles con versión anterior            │
│  ├── Ejemplo: 1.5.0 → 2.0.0                                 │
│  └── Trigger: CUSTOM_VERSION="2.0.0"                        │
│                                                             │
│  MINOR (Nuevas Features Compatibles):                       │
│  ├── Nueva funcionalidad sin romper compatibilidad         │
│  ├── Ejemplo: 1.0.5 → 1.1.0                                 │
│  └── Trigger: Actualizar gradle.properties: version=1.1.0  │
│                                                             │
│  PATCH (Bug Fixes):                                         │
│  ├── Correcciones de bugs                                  │
│  ├── Ejemplo: 1.0.5 → 1.0.6                                 │
│  └── Trigger: Automático (incremento desde Artifactory)    │
│                                                             │
└─────────────────────────────────────────────────────────────┘

EJEMPLOS:

1.0.0  ← Primera versión pública
  ├── 1.0.1  ← Fix bug en HttpHelper
  ├── 1.0.2  ← Fix warning en WebDriver
  ├── 1.0.3  ← Fix CVE en dependency
  └── 1.0.4  ← Fix test flaky

1.1.0  ← Nueva feature: WebDriverManager con Artifactory
  ├── 1.1.1  ← Fix bug en descarga de drivers
  └── 1.1.2  ← Fix SSL en Artifactory

1.2.0  ← Nueva feature: ConfigManager con prioridades

2.0.0  ← BREAKING: Eliminar API deprecated, cambiar estructura
```

---

## 🔄 DIAGRAMA 9: CICLO DE VIDA COMPLETO (TIMELINE)

```
Semana 1:
  Lunes:
    Developer A → feature/login-improvements → develop
    ├── Jenkins (develop) ejecuta: ✅ PASS
    └── Feedback en 5 min

  Martes:
    Developer B → feature/api-retry-logic → develop
    ├── Jenkins (develop) ejecuta: ❌ FAIL (test falla)
    └── Developer B corrige y re-merge: ✅ PASS

  Miércoles:
    Developer A → bugfix/config-null-pointer → develop
    └── Jenkins (develop) ejecuta: ✅ PASS

  Jueves:
    Developer C → feature/mobile-ios-support → develop
    └── Jenkins (develop) ejecuta: ✅ PASS

  Viernes (Release):
    Tech Lead → PR: develop → master
    ├── Review de cambios acumulados
    ├── Verifica: coverage 75% ✅
    ├── Verifica: 0 CVEs críticos ✅
    ├── Approval ✅
    └── Merge

    Jenkins (master) ejecuta:
    ├── Consulta Artifactory: última versión = 1.0.3
    ├── Calcula: 1.0.3 → 1.0.4
    ├── Verifica duplicados: ✅ OK
    ├── Solicita aprobación: Tech Lead aprueba
    ├── Publica 1.0.4 a Artifactory
    └── ✅ SUCCESS

    Resultado:
    📦 Versión 1.0.4 disponible en Artifactory
    🔒 Inmutable (no se puede sobrescribir)

---

Semana 2:
  Lunes-Jueves:
    [Más features acumuladas en develop]

  Viernes:
    Nuevo release: 1.0.4 → 1.0.5

---

Mes 2:
  Breaking change:
    ├── Tech Lead actualiza gradle.properties: version=2.0.0
    ├── Merge a develop
    ├── Acumula features
    ├── Merge a master
    └── Jenkins publica: 2.0.0 (MAJOR release)
```

---

## ⚙️ DIAGRAMA 10: JENKINS MULTIBRANCH CONFIGURATION

```
┌─────────────────────────────────────────────────────────────┐
│ JENKINS → New Item                                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Name: qa-scotia-frameworks                                 │
│  Type: ● Multibranch Pipeline                               │
│                                                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Branch Sources                                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Source: Bitbucket Server                                   │
│  ├── Repository: qaauy/qaauy                                │
│  ├── Credentials: jenkins_bitbucket_token                   │
│  └── Behaviors:                                             │
│      ├── Discover branches: All branches                    │
│      └── Filter by name (regex): (develop|main|master)      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Build Configuration                                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Mode: by Jenkinsfile                                       │
│  Script Path: pipeline.jenkins                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Scan Multibranch Pipeline Triggers                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ☑ Periodically if not otherwise run                        │
│     Interval: 5 minutes                                     │
│                                                             │
│  ☑ Webhook trigger (Bitbucket)                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ RESULTADO: JENKINS DASHBOARD                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  qa-scotia-frameworks/                                      │
│  ├── develop      [#42] ✅ SUCCESS (5 min ago)             │
│  └── master       [#15] ✅ SUCCESS (2 days ago)            │
│                                                             │
│  Branches discovered: 2                                     │
│  Last scan: 2 minutes ago                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

**📚 DOCUMENTOS RELACIONADOS:**
- `INVESTIGACION-ESTRATEGIA-PIPELINE.md` (análisis completo)
- `JENKINS-GUIA-COMPLETA.md` (guía de uso del pipeline.jenkins)
- `pipeline.jenkins` (archivo de configuración)

