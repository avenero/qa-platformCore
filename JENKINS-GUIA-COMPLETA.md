# 📘 GUÍA COMPLETA: Jenkins + Artifactory + GitFlow (Solo Releases)

> **Scotia QA Framework - Pipeline CI/CD Completo**  
> Versión 3.0.0 - Adaptado para estrategia sin repositorio Snapshot  
> Autor: Abel Venero | Fecha: 2026-02-09

---

## 📋 ÍNDICE RÁPIDO

### CONFIGURACIÓN INICIAL
1. [✅ Checklist Completo](#checklist-completo) ← **EMPEZAR AQUÍ**
2. [Nombre del Jenkinsfile](#nombre-del-jenkinsfile)
3. [Estructura de Ramas](#estructura-de-ramas-git)
4. [Configuración Jenkins](#configuración-jenkins)
5. [Configuración Artifactory](#configuración-artifactory)
6. [Configuración Teams](#configuración-teams)

### FLUJO DE TRABAJO
7. [Diagrama Visual](#diagrama-visual-del-flujo)
8. [Flujo por Rol](#flujo-por-rol)
9. [Cuándo se Ejecuta Jenkins](#cuándo-se-ejecuta-jenkins)
10. [Versionado Semántico](#versionado-semántico)

### ESCENARIOS
11. [Feature Normal](#escenario-feature-normal)
12. [Release](#escenario-release)
13. [Hotfix](#escenario-hotfix)
14. [Error Duplicado](#escenario-error-duplicado)

### PRUEBAS
15. [Primera Prueba Completa](#primera-prueba-paso-a-paso)
16. [Troubleshooting](#troubleshooting)

---

# 1. CHECKLIST COMPLETO

## ✅ QUÉ NECESITAS PARA ESTAR LISTO

### FASE 1: Git (30 min) - ⚠️ HACER PRIMERO

```bash
☐ 1.1. Verificar rama main
      $ cd /Users/abel.venero/Documents/qa-scotia-frameworks
      $ git checkout main
      $ git pull origin main

☐ 1.2. Crear rama develop
      $ git checkout -b develop
      $ git push origin develop

☐ 1.3. Configurar branch protection
      GitHub/GitLab → Settings → Branches → main
      ✅ Require PR
      ✅ Require 1 approval
      ✅ No force push

☐ 1.4. Verificar version en gradle.properties
      $ cat gradle.properties | grep "^version="
      Debe ser: version=1.0.0
```

### FASE 2: Artifactory (30 min) - ⚠️ CONTACTAR DEVOPS

```bash
☐ 2.1. Contactar DevOps
      Solicitar:
      - Nombre EXACTO del repo: _____________________
      - Usuario para Jenkins:   _____________________
      - Generar/obtener token:  _____________________

☐ 2.2. Generar API Token
      Artifactory UI → User Profile → Generate API Key
      Token: ___________________________________________

☐ 2.3. Anotar datos:
      Repo:  ___________________________________________
      User:  ___________________________________________
      Token: ___________________________________________
```

### FASE 3: Jenkins (30 min)

```bash
☐ 3.1. Verificar plugins
      HTTP Request Plugin: ☐ Instalado

☐ 3.2. Crear credencial Artifactory
      ID: artifactory-credentials
      User: [DATO FASE 2]
      Pass: [TOKEN FASE 2]

☐ 3.3. Crear credencial Teams
      ID: teams-webhook-qa-framework
      Secret: [URL WEBHOOK]

☐ 3.4. Crear Pipeline Job
      Name: qa-scotia-frameworks
      Type: Pipeline
      SCM: Git → [TU REPO]
      Script Path: pipeline.jenkins
```

### FASE 4: Jenkinsfile (10 min)

```bash
☐ 4.1. Actualizar línea 73
      ARTIFACTORY_RELEASE_REPO = '[NOMBRE REAL]'

☐ 4.2. Commit y push
      $ git add pipeline.jenkins
      $ git commit -m "chore: configure Artifactory"
      $ git push origin main
```

### FASE 5: Primera Prueba (15 min)

```bash
☐ 5.1. Build manual sin publicar
      Jenkins → Build with Parameters
      PUBLISH_TO_ARTIFACTORY: NO

☐ 5.2. Verificar que compile

☐ 5.3. Build manual CON publicar
      PUBLISH_TO_ARTIFACTORY: YES

☐ 5.4. Verificar Artifactory

☐ 5.5. Verificar Teams

☐ 5.6. ✅ LISTO
```

---

# 2. NOMBRE DEL JENKINSFILE

## ✅ RESPUESTA: Está perfecto como está

```
Archivo actual: /Jenkinsfile
Nombre: Jenkinsfile (sin extensión)
Estado: ✅ CORRECTO - NO CAMBIAR
```

**Jenkins reconoce automáticamente:**
- ✅ `Jenkinsfile` ← Tu archivo
- ✅ `jenkinsfile`
- ✅ `Jenkinsfile.groovy`

**❌ NO reconoce:**
- ❌ `pipeline.jenkins`
- ❌ `Jenkins.file`
- ❌ `build.jenkinsfile`

**Configuración en Pipeline Job:**
```
Script Path: Jenkinsfile  ← Exactamente esto
```

---

# 3. ESTRUCTURA DE RAMAS GIT

## Paso a Paso

### 3.1. Crear Ramas Permanentes

```bash
cd /Users/abel.venero/Documents/qa-scotia-frameworks

# Verificar main
git checkout main
git pull origin main

# Crear develop
git checkout -b develop
git push -u origin develop

# Resultado:
# ✅ main (producción)
# ✅ develop (integración)
```

### 3.2. Configurar Branch Protection

**En GitHub:**

```
1. Repo → Settings → Branches

2. Add branch protection rule
   Branch name pattern: main

3. Configurar:
   ✅ Require pull request reviews (1 approval)
   ✅ Dismiss stale reviews
   ✅ Require status checks (Jenkins CI)
   ✅ Include administrators
   ❌ Allow force pushes

4. Save changes
```

**Resultado:**
```
main:    🔒 Protected (solo PRs)
develop: ⚠️  Semi-protected (PRs recomendados)
```

---

# 4. CONFIGURACIÓN JENKINS

## Paso 1: Crear Pipeline Job

```
1. Jenkins → Dashboard → New Item

2. Llenar:
   Item name: qa-scotia-frameworks
   Type: ● Pipeline

3. Click: OK
```

## Paso 2: Configurar Job

```
General:
  Description: "CI/CD Pipeline para Scotia QA Framework"
  
  ☑ Discard old builds
    Max # of builds to keep: 30

Pipeline:
  Definition: Pipeline script from SCM
  
  SCM: Git
    Repository URL: [TU GIT REPO URL]
    Credentials: [TUS GIT CREDS]
    Branches to build: */main
    
  Script Path: Jenkinsfile

Save
```

## Paso 3: Crear Credencial Artifactory

```
1. Manage Jenkins → Manage Credentials

2. (global) → Add Credentials

3. Configurar:
   Kind: Username with password
   Username: [USUARIO ARTIFACTORY]
   Password: [TOKEN API]
   ID: artifactory-credentials
   Description: Artifactory CI/CD

4. OK
```

## Paso 4: Crear Credencial Teams

```
1. Manage Jenkins → Manage Credentials

2. (global) → Add Credentials

3. Configurar:
   Kind: Secret text
   Secret: [URL WEBHOOK TEAMS]
   ID: teams-webhook-qa-framework
   Description: Teams Webhook QA

4. OK
```

---

# 5. CONFIGURACIÓN ARTIFACTORY

## Paso 1: Obtener Token

```
1. URL: https://artifactory.cldevops.chl.bns/ui/

2. Login con tu usuario

3. User Profile (esquina superior derecha)
   → Generate API Key

4. COPIAR token (se muestra solo una vez)
   Ejemplo: AKCp8kq7qKxxxxxxxxxxxxxxxxxxxxx
```

## Paso 2: Verificar Repositorio

```
1. Artifactory UI → Application → Artifacts

2. Buscar repositorio:
   - libs-release-local (si existe)
   - O anotar el nombre exacto que veas

3. Confirmar con DevOps si no estás seguro
```

---

# 6. CONFIGURACIÓN TEAMS

## Paso 1: Webhook

```
1. Microsoft Teams → Canal #qa-builds

2. ⋯ (tres puntos) → Conectores

3. Incoming Webhook → Configurar
   Nombre: Jenkins - QA Framework
   Crear

4. COPIAR URL (muy larga)

5. Pegar en Jenkins credentials
```

---

# 7. DIAGRAMA VISUAL DEL FLUJO

```
═══════════════════════════════════════════════════════════════════════════════
                        ESTRATEGIA SIN SNAPSHOTS
═══════════════════════════════════════════════════════════════════════════════

RAMAS:                    PUBLICACIÓN:                  ARTIFACTORY:
──────────────────────────────────────────────────────────────────────────────

main/master              ✅ Jenkins Auto               📦 libs-release-local
│                         │ Build                        │ v1.0.0 🔒
│ tag v1.0.0              │ Tests                        │ v1.0.1 🔒
│ tag v1.0.1              │ Publish                      │ v1.1.0 🔒
│ tag v1.1.0              │                              │ v2.0.0 🔒
│                         └──────────────────────────────► INMUTABLE
│                                                          ❌ NO sobrescribir
│
develop                   ❌ NO Publica                  ─
│                         (solo integración)
│ (feature acumuladas)
│
feature/login             ❌ NO Publica                  📁 ~/.m2/ (local)
feature/dashboard         (desarrollo local)             │ 1.0.0-SNAPSHOT
feature/api                                              └─ Cada developer


VERSIONADO:
──────────────────────────────────────────────────────────────────────────────

1.0.0 ─► 1.0.1 ─► 1.0.2 ─► 1.1.0 ─► 1.1.1 ─► 1.2.0 ─► 2.0.0
  │        │        │        │        │        │        │
  │        │        │        │        │        │        └─ MAJOR (breaking)
  │        │        │        │        │        └─ MINOR (features)
  │        │        │        │        └─ PATCH (hotfix)
  │        │        │        └─ MINOR (features)
  │        │        └─ PATCH (hotfix)
  │        └─ PATCH (hotfix)
  └─ Release inicial


FLUJO SEMANAL:
──────────────────────────────────────────────────────────────────────────────

LUNES-JUEVES                              VIERNES
Developer: feature → develop              Lead: develop → main
❌ NO publica                             ✅ Publica v1.1.0
🔧 Local (.m2)                            📦 Artifactory
```

---

# 8. FLUJO POR ROL

## 🟢 Developer

```bash
git checkout -b feature/nueva
# ... desarrollo ...
git push
# PR → develop
# ✅ Done (esperar release)
```

## 🟠 Lead

```bash
# Viernes:
vi gradle.properties → version=1.1.0
git commit + push
PR develop → main + Merge
git tag v1.1.0 + push
# ✅ Jenkins publica automático
```

## 🔴 DevOps

```bash
# Hotfix:
git checkout -b hotfix/bug
# Fix
PR → main
vi gradle.properties → version=1.1.1
git tag v1.1.1 + push
# ✅ Jenkins publica
# Merge a develop
```

---

# 9. CUÁNDO SE EJECUTA JENKINS

```
✅ AUTOMÁTICO:
   main/master → Poll SCM cada 5 min

❌ NO AUTOMÁTICO:
   develop, feature/* → Solo manual
```

---

# 10. VERSIONADO SEMÁNTICO

```
PATCH:  1.0.0 → 1.0.1  (hotfixes)
MINOR:  1.0.2 → 1.1.0  (features)
MAJOR:  1.9.0 → 2.0.0  (breaking)
```

---

# 11-14. ESCENARIOS

Ver secciones anteriores para detalles completos.

---

# 15. PRIMERA PRUEBA PASO A PASO

```bash
# 1. Verificar Git
git branch -a  # main y develop existen

# 2. Primera ejecución (NO publicar)
Jenkins → Build with Parameters
PUBLISH_TO_ARTIFACTORY: NO
→ Verificar que compile ✅

# 3. Segunda ejecución (SÍ publicar)
git tag v1.0.0 && git push origin v1.0.0
Jenkins → Build (automático o manual con YES)
→ Verificar Artifactory ✅
→ Verificar Teams ✅

# ✅ LISTO
```

---

# 16. TROUBLESHOOTING

```
Error "credentials not found"
→ Crear credencial en Jenkins

Error "repository not found"
→ Actualizar nombre repo en Jenkinsfile línea 73

Error "401 Unauthorized"
→ Regenerar token Artifactory

Teams no notifica
→ Verificar webhook URL
→ Verificar plugin HTTP Request

Tests fallan
→ Verificar Java version en agente
```

---

# RESUMEN FINAL

## ✅ Lo que tienes:

- Jenkinsfile completo (330 líneas)
- Protección duplicados
- Notificaciones Teams
- Versionado semántico
- Esta guía única

## ⚠️ Lo que falta:

1. Obtener nombre repo Artifactory
2. Crear credenciales Jenkins (2)
3. Webhook Teams
4. Crear ramas git
5. Primera prueba

## ⏱️ Tiempo: 2 horas

---

**🚀 PRÓXIMO PASO:**

1. Seguir FASE 1 del checklist (crear ramas)
2. Contactar DevOps (obtener datos Artifactory)
3. Configurar credenciales Jenkins
4. Primera prueba

**📞 Soporte:** Ver sección Troubleshooting

---

**Versión:** 3.0.0 | **Autor:** Abel Venero | **Fecha:** 2026-02-09

