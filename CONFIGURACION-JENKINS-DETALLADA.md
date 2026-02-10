# 🔧 CONFIGURACIÓN JENKINS - Guía Paso a Paso Detallada

> **Basado en tu Jenkinsfile existente + Infraestructura actual**  
> Autor: Abel Venero | Fecha: 2026-02-10

---

## 🎯 DATOS REALES DE TU INFRAESTRUCTURA

```
✅ Agente Jenkins: jslave1
✅ JDK configurado: OpenJDK 21
✅ Gradle configurado: Gradle 8.5
✅ Credencial Artifactory: 'Artifactory' (ya existe)
✅ Repositorio: libs-release-thirdparty (o libs-release-local)
✅ Librería compartida: pipeline-utils (opcional para ti)
```

---

## 📋 TABLA DE CAMBIOS NECESARIOS

| Línea | Componente | Valor Actual | Cambiar A | Por Qué |
|-------|------------|--------------|-----------|---------|
| 36 | Agent label | `'any'` | `'jslave1'` | Tu agente existente |
| Después 36 | Tools | ❌ No existe | `tools { jdk... }` | Usar tools configurados |
| 73 | Repo name | `'libs-release-local'` | `'libs-release-thirdparty'` | Tu repo real (verificar) |
| 77 | Credencial ID | `'artifactory-credentials'` | `'Artifactory'` | Tu credencial existente |

---

## 🔵 PASO 1: ACTUALIZAR JENKINSFILE (10 min)

### Cambio 1: Agent (línea 36)

```groovy
// ANTES:
agent {
    label 'any'
}

// DESPUÉS:
agent {
    label 'jslave1'  // ✅ Tu agente que ya funciona
}
```

### Cambio 2: Agregar Tools (DESPUÉS de agent, nueva sección)

```groovy
agent {
    label 'jslave1'
}

// ========================================================================
// TOOLS - NUEVO: Agregar esta sección completa
// ========================================================================
tools {
    jdk 'OpenJDK 21'      // ✅ Ya configurado en Jenkins
    gradle 'Gradle 8.5'   // ✅ Ya configurado en Jenkins
}

// ========================================================================
// PARÁMETROS (continúa igual)
// ========================================================================
parameters {
    ...
```

### Cambio 3: Repositorio Artifactory (línea 73)

```groovy
// ANTES:
ARTIFACTORY_RELEASE_REPO = 'libs-release-local'  // ⚠️ ACTUALIZAR

// OPCIÓN A - Si es para third-party/librerías externas:
ARTIFACTORY_RELEASE_REPO = 'libs-release-thirdparty'  // ✅ De tu Jenkins actual

// OPCIÓN B - Si es para código propio:
ARTIFACTORY_RELEASE_REPO = 'libs-release-local'  // Mantener

// ⚠️ VERIFICAR con DevOps cuál usar para TU framework
```

### Cambio 4: ID Credencial Artifactory (línea 77)

```groovy
// ANTES:
ARTIFACTORY_CREDS = credentials('artifactory-credentials')

// DESPUÉS:
ARTIFACTORY_CREDS = credentials('Artifactory')  // ✅ Tu credencial existente
```

### Aplicar cambios:

```bash
cd /Users/abel.venero/Documents/qa-scotia-frameworks

# Si el archivo pipeline.jenkins no existe aún, créalo primero
# (está en tus cambios pendientes de commit)

git status  # Ver si pipeline.jenkins está en "new file" o "modified"

# Editar manualmente:
vi pipeline.jenkins  # O tu editor favorito

# Hacer los 4 cambios arriba

# Guardar y salir
# En vi: Esc → :wq → Enter
```

---

## 🟢 PASO 2: CONFIGURAR MICROSOFT TEAMS (15 min)

### 2.1. Preparar canal Teams

```
1. Abrir Microsoft Teams (app o web)

2. OPCIÓN A - Usar canal existente:
   - Si ya tienes #builds, #cicd, #notifications
   - Ir a ese canal

3. OPCIÓN B - Crear canal nuevo (RECOMENDADO):
   a) En tu Team → Click ⋯ → "Agregar canal" / "Add channel"
   
   b) Configurar:
      Nombre: qa-builds
      Descripción: Notificaciones de builds QA Framework
      Privacidad: ● Estándar (todo el equipo)
      
   c) Crear
   
   d) ✅ Canal creado
```

### 2.2. Configurar Incoming Webhook (el paso crítico)

```
📍 UBICACIÓN DEL CANAL:
   Team → Canales → qa-builds

1. Click en ⋯ (tres puntos) junto al nombre "qa-builds"
   
   Aparecerá menú:
   - Obtener enlace al canal
   - Administrar canal
   - Conectores          ← CLICK AQUÍ
   - ...

2. Se abre ventana "Conectores"
   
   Buscar: "Incoming Webhook"
   (Usar barra de búsqueda arriba)
   
   Resultado:
   ┌─────────────────────────────────────┐
   │ 📨 Incoming Webhook                 │
   │ Recibir datos de servicios externos │
   │                                     │
   │ [Configurar]       ← CLICK AQUÍ    │
   └─────────────────────────────────────┘

3. Formulario de configuración:
   
   ┌─────────────────────────────────────────────┐
   │ Nombre del webhook:                         │
   │ Jenkins - QA Scotia Framework               │
   │                                             │
   │ Cargar imagen: (opcional)                   │
   │ [Examinar...] → Puedes subir logo Jenkins  │
   │                                             │
   │ [Crear]                  ← CLICK AQUÍ       │
   └─────────────────────────────────────────────┘

4. ⚠️ PANTALLA CRÍTICA - URL Generada:
   
   Aparece:
   ┌──────────────────────────────────────────────────────────┐
   │ ✅ Webhook creado exitosamente                          │
   │                                                          │
   │ URL:                                                     │
   │ ┌──────────────────────────────────────────────────┐   │
   │ │https://outlook.office.com/webhook/               │   │
   │ │a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6@             │   │
   │ │p6o5n4m3-l2k1-j0i9-h8g7-f6e5d4c3b2a1/             │   │
   │ │IncomingWebhook/                                   │   │
   │ │q1w2e3r4t5y6u7i8o9p0/                             │   │
   │ │z9x8c7v6b5n4m3l2k1j0                              │   │
   │ └──────────────────────────────────────────────────┘   │
   │                                                          │
   │ ⚠️ COPIAR ESTA URL COMPLETA (es muy larga)             │
   │                                                          │
   │ [Listo]                                                  │
   └──────────────────────────────────────────────────────────┘

5. **COPIAR URL COMPLETA**
   - Seleccionar TODO el texto (triple click)
   - Ctrl+C (o Cmd+C en Mac)
   - Pegar en un archivo temporal (Notepad, etc)
   
   ⚠️ La URL es GIGANTE (varias líneas)
   ⚠️ Se muestra solo UNA VEZ
   ⚠️ Si cierras sin copiar, tendrás que crear otro

6. Click: Listo

7. Verificar en el canal:
   - Debe aparecer mensaje: "Jenkins - QA Scotia Framework configurado"
```

### 2.3. Probar webhook (OPCIONAL pero recomendado)

```bash
# Desde tu terminal Mac:

curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"text":"🧪 Test desde terminal - Webhook funcionando"}' \
  "https://outlook.office.com/webhook/xxx..."
#  ↑ PEGAR TU URL COMPLETA AQUÍ

# Resultado esperado:
# 1. El comando devuelve: 1
# 2. En Teams aparece el mensaje "🧪 Test desde terminal..."

# Si aparece en Teams:
✅ Webhook funciona correctamente

# Si NO aparece:
❌ Verificar URL (puede estar mal copiada)
❌ Verificar firewall corporativo
```

### 2.4. Crear credencial en Jenkins

```
1. Jenkins → Manage Jenkins → Manage Credentials

2. Click: (global) domain

3. Add Credentials

4. Formulario:
   ┌──────────────────────────────────────────────────────────┐
   │ Kind: Secret text            ← IMPORTANTE: Secret text   │
   │ Scope: Global (Jenkins, nodes, items, all child items...)│
   │                                                          │
   │ Secret:                                                  │
   │ ┌──────────────────────────────────────────────────┐   │
   │ │ [PEGAR AQUÍ LA URL COMPLETA DEL WEBHOOK]        │   │
   │ │ https://outlook.office.com/webhook/...           │   │
   │ │ (toda la URL, sin espacios, sin saltos de línea) │   │
   │ └──────────────────────────────────────────────────┘   │
   │                                                          │
   │ ID: teams-webhook-qa-framework                           │
   │                                                          │
   │ Description: MS Teams Webhook - QA Framework CI/CD      │
   └──────────────────────────────────────────────────────────┘

5. Click: OK

6. Verificar aparece en lista:
   ID: teams-webhook-qa-framework
   Kind: Secret text
   Description: MS Teams Webhook...
```

---

## 🟡 PASO 3: VERIFICAR PLUGIN HTTP REQUEST (5 min)

```
1. Jenkins → Manage Jenkins → Manage Plugins

2. Tab: Installed

3. Buscar (Ctrl+F): HTTP Request

4. Verificar que aparece:
   ☑ HTTP Request Plugin  v1.x.x
   
   Si aparece:
   ✅ Ya instalado, no hacer nada

5. Si NO aparece:
   a) Tab: Available
   b) Buscar: HTTP Request
   c) Marcar checkbox: ☑ HTTP Request Plugin
   d) Click: Install without restart
   e) Esperar instalación (1-2 min)
   f) ✅ Instalado

6. Volver a Dashboard
```

---

## 🔴 PASO 4: VERIFICAR/ADAPTAR CREDENCIAL ARTIFACTORY

### 4.1. Verificar credencial existente

```
1. Jenkins → Manage Jenkins → Manage Credentials

2. Click: (global)

3. Buscar en la lista: Artifactory

4. Click en "Artifactory" para ver detalles:
   
   ┌────────────────────────────────────────┐
   │ ID: Artifactory                        │
   │ Kind: Username with password           │
   │ Username: xxxxxxxxx                    │
   │ Description: ...                       │
   └────────────────────────────────────────┘

5. Anotar:
   ID: Artifactory  ← Este es el que usarás
```

### 4.2. Actualizar Jenkinsfile

```groovy
// Línea 77 - Cambiar de:
ARTIFACTORY_CREDS = credentials('artifactory-credentials')

// A:
ARTIFACTORY_CREDS = credentials('Artifactory')  // ✅ Tu credencial existente
```

### 4.3. (ALTERNATIVA) Crear credencial nueva

```
Solo si quieres credencial dedicada para QA Framework:

1. Add Credentials

2. Configurar:
   Kind: Username with password
   Username: [MISMO usuario que credencial "Artifactory"]
   Password: [MISMO password que credencial "Artifactory"]
   ID: artifactory-credentials
   Description: Artifactory QA Framework dedicated

3. OK

Jenkinsfile: Mantener línea 77:
  ARTIFACTORY_CREDS = credentials('artifactory-credentials')
```

---

## PASO 5: CONFIRMAR REPOSITORIO ARTIFACTORY (10 min)

### 5.1. Verificar en Artifactory UI

```
1. Navegador: https://artifactory.cldevops.chl.bns/ui/

2. Login (mismo usuario de credencial Jenkins)

3. Left menu: Application → Artifactory → Artifacts

4. Ver lista de repositorios disponibles:
   
   Buscar repositorios tipo "maven" o "gradle":
   ☐ libs-release-local
   ☐ libs-release-thirdparty
   ☐ libs-snapshot-local
   ☐ otros...

5. Identificar el correcto:
   
   ¿Cuál usar para TU framework (código propio)?
   
   OPCIÓN A: libs-release-local
     Uso: Código desarrollado internamente
     ✅ RECOMENDADO para qa-scotia-frameworks
   
   OPCIÓN B: libs-release-thirdparty
     Uso: Librerías de terceros/externas
     ⚠️ Posiblemente NO es para tu código

6. Anotar nombre EXACTO:
   Repositorio: _________________________________
```

### 5.2. Confirmar con DevOps (RECOMENDADO)

```
Email rápido:

Para: devops@scotia.com
Asunto: Confirmar repositorio Artifactory - QA Framework

Hola,

Estoy configurando publicación de qa-scotia-frameworks a Artifactory.

¿Qué repositorio debo usar para nuestro framework Java interno?

Opciones que veo:
  - libs-release-local
  - libs-release-thirdparty

Mi suposición: libs-release-local (código propio)

¿Es correcto?

Gracias,
Abel
```

### 5.3. Actualizar Jenkinsfile

```groovy
// Línea 73 - Actualizar con nombre confirmado:

// Si es libs-release-local:
ARTIFACTORY_RELEASE_REPO = 'libs-release-local'  // ✅ Para código propio

// O si DevOps dice otro:
ARTIFACTORY_RELEASE_REPO = '[NOMBRE_REAL_DEVOPS]'
```

---

## PASO 6: CREAR PIPELINE JOB EN JENKINS (15 min)

### 6.1. Crear Job

```
1. Jenkins Dashboard → New Item

2. Llenar:
   ┌────────────────────────────────────────────────┐
   │ Enter an item name:                            │
   │ ┌────────────────────────────────────────┐    │
   │ │ qa-scotia-frameworks                    │    │
   │ └────────────────────────────────────────┘    │
   │                                                │
   │ Select:                                        │
   │ ○ Freestyle project                            │
   │ ● Pipeline                ← SELECCIONAR        │
   │ ○ Multi-configuration                          │
   │                                                │
   │ Copy from: (dejar vacío)                       │
   │                                                │
   │ [OK]                                           │
   └────────────────────────────────────────────────┘

3. Click: OK
```

### 6.2. Configurar General

```
Sección: General
────────────────────────────────────────────────

Description:
  CI/CD Pipeline para Scotia QA Framework.
  Publica automáticamente a Artifactory cuando se hace push a main.

☐ GitHub project
  (Marcar solo si usas GitHub)
  Project url: https://github.com/tu-org/qa-scotia-frameworks

☑ Discard old builds
  Strategy: Log Rotation
  Days to keep builds: 90
  Max # of builds to keep: 30
  Artifact Days to keep: (vacío)
  Artifact Max # to keep: 10

☐ This project is parameterized
  (NO marcar - parámetros en Jenkinsfile)

☑ Do not allow concurrent builds
  ⚠️ IMPORTANTE: Previene publicaciones simultáneas
```

### 6.3. Configurar Build Triggers

```
Sección: Build Triggers
────────────────────────────────────────────────

☑ Poll SCM
  Schedule:
  ┌────────────────────────────────────────┐
  │ H/5 * * * *                            │
  └────────────────────────────────────────┘
  
  Explicación:
  - H = Hash (minuto aleatorio 0-4 para balanceo)
  - /5 = Cada 5 minutos
  - * * * * = Todos los días a todas horas
  
  Resultado:
  → Jenkins verifica Git cada 5 minutos
  → Solo en rama main (configurado en Branches)
  → Si hay commits nuevos → Build automático

☐ Build after other projects
☐ Build periodically
☐ GitHub hook trigger
  (No necesario si usas Poll SCM)
```

### 6.4. Configurar Pipeline (CRÍTICO)

```
Sección: Pipeline
────────────────────────────────────────────────

Definition: Pipeline script from SCM  ← SELECCIONAR en dropdown

SCM: Git  ← SELECCIONAR en dropdown

─────────────────────────────────────────────────
Repositories:
─────────────────────────────────────────────────

Repository URL:
  ┌───────────────────────────────────────────────┐
  │ [PEGAR URL DE TU REPOSITORIO GIT]            │
  └───────────────────────────────────────────────┘
  
  Obtener URL:
  - GitHub: Repo → Code → Clone → HTTPS
  - GitLab: Repo → Clone → Clone with HTTPS
  - Bitbucket: Similar
  
  Ejemplos:
  https://github.com/scotia-qa/qa-scotia-frameworks.git
  https://gitlab.scotia.com/qa/qa-scotia-frameworks.git

Credentials: [SELECT]
  
  Si el repo es PRIVADO:
    - Seleccionar tu credencial Git
    - Generalmente: tu_usuario/****** (GitHub/GitLab)
  
  Si el repo es PÚBLICO:
    - Seleccionar: - none -

─────────────────────────────────────────────────
Branches to build:
─────────────────────────────────────────────────

Branch Specifier (blank for 'any'):
  ┌───────────────────────────────────────────────┐
  │ */main                                        │
  └───────────────────────────────────────────────┘
  
  ⚠️ IMPORTANTE:
  - Si renombraste master → main: */main
  - Si sigues con master: */master
  - SOLO la rama principal (para publicación)

─────────────────────────────────────────────────
Repository browser: (auto)
─────────────────────────────────────────────────
  Dejar en "auto"

─────────────────────────────────────────────────
Script Path:
─────────────────────────────────────────────────
  ┌───────────────────────────────────────────────┐
  │ Jenkinsfile                                   │
  └───────────────────────────────────────────────┘
  
  ⚠️ EXACTAMENTE: Jenkinsfile
  - Sin ruta (no ./Jenkinsfile, no /Jenkinsfile)
  - Sin extensión (no .groovy, no .jenkins)
  - Mayúscula inicial: J
  - Está en la raíz del repo

☐ Lightweight checkout
  Dejar DESMARCADO (mejor para builds completos)
```

### 6.5. Guardar

```
1. Scroll hasta abajo

2. Click: Save (botón azul grande)

3. Redirige a: qa-scotia-frameworks Dashboard

4. ✅ Job creado exitosamente
```

---

## PASO 7: COMMIT CAMBIOS JENKINSFILE (5 min)

```bash
cd /Users/abel.venero/Documents/qa-scotia-frameworks

# Ver cambios pendientes
git status

# Agregar pipeline.jenkins
git add pipeline.jenkins

# Si hay otros cambios (gradle.properties, etc):
git add gradle.properties build.gradle

# Commit
git commit -m "chore: configure Jenkinsfile with real infrastructure

- Agent: jslave1 (existing)
- Tools: OpenJDK 21, Gradle 8.5 (already configured)
- Credentials: Artifactory (existing)
- Repo: libs-release-local (verified)
- Teams webhook: teams-webhook-qa-framework"

# Push
git push origin master  # O main si renombraste

# ✅ pipeline.jenkins en repositorio
```

---

## PASO 8: PRIMERA PRUEBA - BUILD SIN PUBLICAR (10 min)

```
Objetivo: Verificar que Jenkins puede compilar el proyecto

1. Jenkins → Dashboard → qa-scotia-frameworks

2. Left menu → "Build with Parameters"
   (Si no aparece, click en el job primero)

3. Configurar parámetros:
   ┌────────────────────────────────────────────────┐
   │ PUBLISH_TO_ARTIFACTORY:                        │
   │ ● AUTO                                         │
   │ ○ YES                                          │
   │ ○ NO                    ← SELECCIONAR          │
   │                                                │
   │ CUSTOM_VERSION:                                │
   │ ┌──────────────────────────────────────┐      │
   │ │ (dejar vacío)                         │      │
   │ └──────────────────────────────────────┘      │
   │                                                │
   │ SKIP_TESTS:                                    │
   │ ☐ false             ← DEJAR desmarcado        │
   │                                                │
   │ [Build]                                        │
   └────────────────────────────────────────────────┘

4. Click: Build (botón verde/azul)

5. Aparece nuevo build: #1 (con bolita parpadeando)

6. Click en #1 → Console Output

7. Monitorear en tiempo real:
   
   Ver que pase cada stage:
   ✅ [Pipeline] Start
   ✅ Stage: 🔽 Checkout
   ✅ Stage: 🔢 Calcular Versión
      Output: "📦 VERSIÓN: 1.0.0"
              "🚀 ¿PUBLICAR?: NO (solo CI)"
   ✅ Stage: 🔍 Verificar Duplicados (SKIPPED - no va a publicar)
   ✅ Stage: 🔍 Verificar Entorno
      Output: "☕ Java: version 21.0.x"
              "🐘 Gradle: version 8.5"
   ✅ Stage: 🧹 Limpiar
   ✅ Stage: 🔨 Compilar
   ✅ Stage: 🧪 Tests
   ✅ Stage: 📊 Coverage
   ✅ Stage: 🚦 Quality Gate (SKIPPED)
   ✅ Stage: 📦 Artefactos
   ✅ Stage: 🚀 Publicar (SKIPPED - NO publicar)
   ✅ [Pipeline] Success

8. Resultado esperado:
   ════════════════════════════════════════
   Finished: SUCCESS
   ════════════════════════════════════════

Si ves SUCCESS:
  ✅ Jenkins puede compilar
  ✅ Tests pasan
  ✅ Coverage OK
  ✅ NO publicó (correcto)
  ✅ Listo para siguiente paso

Si ves FAILURE:
  → Ir a sección TROUBLESHOOTING
```

---

## PASO 9: SEGUNDA PRUEBA - BUILD CON PUBLICACIÓN (15 min)

### 9.1. Crear tag Git

```bash
cd /Users/abel.venero/Documents/qa-scotia-frameworks

git checkout master  # O main

# Crear tag v1.0.0
git tag -a v1.0.0 -m "Release v1.0.0 - Primera publicación a Artifactory"

# Push tag
git push origin v1.0.0

# ✅ Tag creado y pusheado
```

### 9.2. Ejecutar build (auto o manual)

```
OPCIÓN A: Esperar ejecución automática (5 min)
────────────────────────────────────────────────
Poll SCM detectará el tag en máximo 5 minutos
→ Build se ejecutará automáticamente
→ Ir a Dashboard y ver que aparezca build #2


OPCIÓN B: Ejecutar manual (inmediato)
────────────────────────────────────────────────
1. Jenkins → qa-scotia-frameworks

2. Build with Parameters

3. Configurar:
   ┌────────────────────────────────────────────────┐
   │ PUBLISH_TO_ARTIFACTORY:                        │
   │ ○ AUTO                                         │
   │ ● YES                   ← SELECCIONAR          │
   │ ○ NO                                           │
   │                                                │
   │ CUSTOM_VERSION: (vacío)                        │
   │ SKIP_TESTS: ☐ false                            │
   │                                                │
   │ [Build]                                        │
   └────────────────────────────────────────────────┘

4. Click: Build
```

### 9.3. Monitorear Console Output (CRÍTICO)

```
Click en #2 → Console Output

Verificar STAGE POR STAGE:

✅ Stage 1: Checkout
   "📌 Branch: main"
   "📌 Commit: abc1234"

✅ Stage 2: Calcular Versión
   "📦 VERSIÓN: 1.0.0"
   "🎯 TIPO: RELEASE (inmutable)"
   "🚀 ¿PUBLICAR?: SÍ (rama main)"

✅ Stage 3: Verificar Duplicados
   "🔎 Verificando common/1.0.0..."
   "   ✅ common v1.0.0 NO existe"
   "🔎 Verificando api-core/1.0.0..."
   "   ✅ api-core v1.0.0 NO existe"
   ... (todos los módulos)
   "✅ Versión 1.0.0 disponible para publicar"

✅ Stage 4-10: Build, Tests, Coverage, Gates, JARs

✅ Stage 11: Publicar ← CRÍTICO
   "🚀 Publicando a Artifactory..."
   "📦 Versión: 1.0.0"
   "📁 Repo: libs-release-local"
   "🌐 URL: https://artifactory.../libs-release-local"
   
   "./gradlew publish..."
   
   Output debe incluir:
   "> Task :common:publishMavenJavaPublicationToArtifactoryRepository"
   "> Task :api-core:publishMavenJavaPublicationToArtifactoryRepository"
   "> Task :web-core:publishMavenJavaPublicationToArtifactoryRepository"
   "> Task :mobile-core:publishMavenJavaPublicationToArtifactoryRepository"
   
   "✅ PUBLICACIÓN EXITOSA"

✅ Post: Success
   "🔔 Teams Alert"
   "✅ Notificación enviada a Microsoft Teams"

════════════════════════════════════════════════
Finished: SUCCESS
════════════════════════════════════════════════
```

### 9.4. SI FALLA EN STAGE 11 (Publicar):

```
Error 401 - Unauthorized:
  → Credencial incorrecta
  → Verificar credencial "Artifactory" en Jenkins
  → Regenerar token si es necesario

Error 404 - Not Found:
  → Repositorio no existe o nombre incorrecto
  → Verificar en Artifactory UI el nombre exacto
  → Actualizar Jenkinsfile línea 73

Error 409 - Conflict:
  → Versión ya existe
  → Alguien ya publicó v1.0.0
  → Incrementar versión a 1.0.1

Error "Could not resolve":
  → Problema de red/proxy
  → Contactar DevOps
```

---

## PASO 10: VERIFICAR PUBLICACIÓN EN ARTIFACTORY (5 min)

```
1. Navegador: https://artifactory.cldevops.chl.bns/ui/

2. Login

3. Navegar árbol:
   Application → Artifactory → Artifacts
   
   → libs-release-local (o tu repo)
   → com
   → scotia
   → qa
   → common
   → 1.0.0

4. Verificar archivos:
   ✅ common-1.0.0.jar          (binario)
   ✅ common-1.0.0.pom          (metadata Maven)
   ✅ common-1.0.0-javadoc.jar  (documentación)
   ✅ common-1.0.0-sources.jar  (código fuente)

5. Repetir para otros módulos:
   qa → api-core → 1.0.0
   qa → web-core → 1.0.0
   qa → mobile-core → 1.0.0

6. Si TODOS existen:
   ✅ PUBLICACIÓN EXITOSA
   ✅ Framework disponible en Artifactory

7. Copiar URL de un artefacto (para testing):
   https://artifactory.cldevops.chl.bns/artifactory/
   libs-release-local/com/scotia/qa/common/1.0.0/common-1.0.0.jar
```

---

## PASO 11: VERIFICAR TEAMS (2 min)

```
1. Microsoft Teams → Canal #qa-builds

2. Debe haber mensaje nuevo:

   ╔══════════════════════════════════════════════╗
   ║ ✅ Build Exitoso - qa-scotia-frameworks      ║
   ║ ────────────────────────────────────────     ║
   ║ Build #2                                     ║
   ║                                              ║
   ║ 📌 Branch: main                             ║
   ║ 📦 Versión: 1.0.0                           ║
   ║ 🚀 Publicado: SÍ                            ║
   ║ 👤 Autor: Abel Venero                       ║
   ║ 💬 Commit: chore: configure Jenkinsfile...  ║
   ║                                              ║
   ║ - common v1.0.0                              ║
   ║ - api-core v1.0.0                            ║
   ║ - web-core v1.0.0                            ║
   ║ - mobile-core v1.0.0                         ║
   ║                                              ║
   ║ 🔒 INMUTABLE - No se puede sobrescribir     ║
   ║                                              ║
   ║ [Ver Build]  [Ver Reportes]                  ║
   ╚══════════════════════════════════════════════╝

Si aparece mensaje:
  ✅ Teams funciona correctamente

Si NO aparece:
  → Ver sección TROUBLESHOOTING Teams
```

---

## PASO 12: PROBAR PROTECCIÓN DUPLICADOS (3 min)

```
Objetivo: Verificar que NO se puede republicar misma versión

1. Jenkins → qa-scotia-frameworks → Build with Parameters

2. Configurar:
   PUBLISH_TO_ARTIFACTORY: YES
   CUSTOM_VERSION: 1.0.0  ← Misma versión que ya publicamos
   SKIP_TESTS: true (para que falle rápido)

3. Build

4. Console Output debe mostrar:

   Stage 3: Verificar Duplicados
   ────────────────────────────────────────
   🔎 Verificando common/1.0.0...
      HTTP HEAD → Status: 200
      ⚠️  common v1.0.0 YA EXISTE
   
   🔎 Verificando api-core/1.0.0...
      ⚠️  api-core v1.0.0 YA EXISTE
   
   ❌ ERROR: VERSIÓN DUPLICADA DETECTADA
   
   Soluciones:
     1. Incrementar versión → 1.0.1
     2. Crear tag v1.0.1
     ...
   
   ❌ Versión 1.0.0 ya existe en Artifactory
   
   Finished: FAILURE

5. Resultado esperado:
   ❌ BUILD FAILED (en 2-3 minutos)
   ✅ Protección funciona
   ✅ NO compiló (ahorró tiempo)
   ✅ NO publicó

6. Teams debe mostrar:
   ❌ Build Fallido - Versión duplicada

✅ Protección contra sobrescritura FUNCIONA
```

---

## 📊 CONFIGURACIÓN TEAMS - DETALLES VISUALES

### Dónde está cada opción en Teams

```
UBICACIÓN EXACTA DEL MENÚ:
═══════════════════════════════════════════════════════════

1. Microsoft Teams (app de escritorio o web)

2. Panel izquierdo → Equipos → [Tu Team]

3. Lista de canales:
   📢 General
   📢 qa-builds          ← Tu canal
   📢 otros...

4. Click en "qa-builds" para abrirlo

5. Arriba a la derecha del nombre del canal, hay tabs:
   
   ┌─────────────────────────────────────────┐
   │ qa-builds    🔔 ⋯                        │  ← ⋯ es tres puntos
   └─────────────────────────────────────────┘
              ↑
              Click aquí

6. Menú desplegable:
   ┌────────────────────────────────────┐
   │ 📌 Anclar                          │
   │ 🔔 Silenciar                       │
   │ 📎 Obtener enlace al canal         │
   │ 🔧 Administrar canal               │
   │ 🔌 Conectores          ← CLICK     │
   │ ...                                │
   └────────────────────────────────────┘

7. Se abre modal "Conectores para qa-builds"
   
   Arriba hay barra de búsqueda:
   ┌────────────────────────────────────┐
   │ 🔍 Buscar conectores               │
   └────────────────────────────────────┘
   
   Escribir: Incoming Webhook

8. Aparece:
   ┌─────────────────────────────────────────┐
   │ 📨 Incoming Webhook                     │
   │ Microsoft Corporation                   │
   │ Recibir datos de servicios externos     │
   │                                         │
   │ [Configurar]          ← CLICK           │
   └─────────────────────────────────────────┘

9. Formulario pequeño:
   ┌─────────────────────────────────────────┐
   │ Proporciona un nombre descriptivo:     │
   │ ┌─────────────────────────────────┐   │
   │ │ Jenkins - QA Scotia Framework    │   │
   │ └─────────────────────────────────┘   │
   │                                         │
   │ Cargar imagen: [Examinar...] (opcional)│
   │                                         │
   │ [Crear]                ← CLICK          │
   └─────────────────────────────────────────┘

10. Pantalla de confirmación:
    ┌────────────────────────────────────────────────┐
    │ ✅ Webhook creado                              │
    │                                                │
    │ URL del webhook:                               │
    │ ┌──────────────────────────────────────────┐  │
    │ │ https://outlook.office.com/webhook/      │  │
    │ │ xxx-xxx-xxx.../IncomingWebhook/yyy...   │  │
    │ │                                          │  │
    │ │ ⚠️ SELECCIONAR TODO Y COPIAR            │  │
    │ └──────────────────────────────────────────┘  │
    │                                                │
    │ [Listo]                                        │
    └────────────────────────────────────────────────┘

11. COPIAR URL (triple click para seleccionar todo)

12. Click: Listo

13. En el canal debe aparecer mensaje:
    "Jenkins - QA Scotia Framework configurado"
```

---

## TROUBLESHOOTING - PROBLEMAS COMUNES

### ❌ Problema: "No such property: ARTIFACTORY_CREDS"

```
Error completo:
  groovy.lang.MissingPropertyException: No such property: ARTIFACTORY_CREDS

Causa:
  Credencial no existe en Jenkins

Solución:
  1. Verificar que reutilizas credencial existente:
     Jenkins → Credentials → Buscar "Artifactory"
     
  2. Si existe:
     Jenkinsfile línea 77 cambiar a:
     ARTIFACTORY_CREDS = credentials('Artifactory')
     
  3. Si NO existe:
     Crear credencial nueva (PASO 4.3)

  4. Commit + push Jenkinsfile
  
  5. Rebuild
```

### ❌ Problema: Teams no notifica

```
Error en logs:
  Post: success
  ⚠️ Teams notificación falló: ...

Solución:

1. Verificar plugin HTTP Request:
   Manage Plugins → Installed → Buscar "HTTP Request"
   Si no está: Instalar (PASO 3)

2. Verificar credencial teams-webhook-qa-framework:
   Credentials → Buscar "teams-webhook-qa-framework"
   Si no existe: Crear (PASO 2.4)

3. Probar webhook manualmente:
   curl -X POST \
     -H "Content-Type: application/json" \
     -d '{"text":"Test manual"}' \
     "[TU_URL_WEBHOOK]"
   
   Si llega a Teams → webhook OK
   Si no llega → URL incorrecta

4. Rebuild
```

### ❌ Problema: "Repository not found"

```
Error en Stage 11:
  Could not PUT '...libs-release-local...'
  404 Not Found

Solución:
  1. Artifactory UI → Ver nombre exacto del repositorio
  
  2. Opciones más comunes:
     - libs-release-local (código propio)
     - libs-release-thirdparty (terceros)
     - libs-releases (sin "local")
     - maven-releases
  
  3. Actualizar Jenkinsfile línea 73 con nombre EXACTO
  
  4. Commit + push
  
  5. Rebuild
```

### ❌ Problema: Build muy lento

```
Si build tarda más de 15 minutos:

Optimización 1: Usar Build Cache
  build.gradle ya tiene:
    buildCache { local { enabled = true } }
  
  Verificar que funcione:
    Console Output debe mostrar: "BUILD SUCCESSFUL in 2m 30s"

Optimización 2: Verificar agente jslave1
  Asegurar que tenga recursos suficientes:
  - 4+ GB RAM
  - CPU multi-core
  
Optimización 3: Gradle daemon
  gradle.properties ya tiene:
    org.gradle.daemon=true
```

---

## 📋 RESUMEN DE CONFIGURACIONES

### En Jenkins (UI):

```
TOOLS (Global Tool Configuration):
  ✅ JDK: OpenJDK 21 (ya configurado)
  ✅ Gradle: Gradle 8.5 (ya configurado)

CREDENTIALS (Manage Credentials):
  ✅ Artifactory (ya existe - reutilizar)
  ⚠️ teams-webhook-qa-framework (CREAR NUEVO)

PLUGINS (Manage Plugins):
  ⚠️ HTTP Request Plugin (verificar/instalar)

JOB (Pipeline qa-scotia-frameworks):
  ⚠️ Crear nuevo job (PASO 6)
  Script Path: Jenkinsfile
  Branches: */main (o */master)
```

### En Jenkinsfile (código):

```groovy
// Línea 36:
agent { label 'jslave1' }  // ✅ Cambiar de 'any'

// NUEVA sección después de agent:
tools {
    jdk 'OpenJDK 21'
    gradle 'Gradle 8.5'
}

// Línea 73:
ARTIFACTORY_RELEASE_REPO = 'libs-release-local'  // ⚠️ Verificar nombre

// Línea 77:
ARTIFACTORY_CREDS = credentials('Artifactory')  // ✅ Cambiar ID
```

---

## ✅ CHECKLIST FINAL

```
JENKINSFILE:
☐ Línea 36: label 'jslave1'
☐ Después línea 36: tools { jdk, gradle }
☐ Línea 73: nombre repo verificado
☐ Línea 77: credentials('Artifactory')
☐ Commiteado y pusheado

JENKINS CONFIGURACIÓN:
☐ Tools: OpenJDK 21 verificado
☐ Tools: Gradle 8.5 verificado
☐ Plugin: HTTP Request instalado
☐ Credencial: Artifactory verificada (reutilizar)
☐ Credencial: teams-webhook-qa-framework creada
☐ Job: qa-scotia-frameworks creado
☐ Script Path: Jenkinsfile

TEAMS:
☐ Canal creado/identificado
☐ Webhook configurado
☐ URL copiada
☐ Credencial Jenkins creada
☐ Test curl exitoso (opcional)

PRUEBAS:
☐ Build sin publicar → SUCCESS
☐ Tag v1.0.0 creado
☐ Build con publicar → SUCCESS
☐ Artifactory verificado (v1.0.0 existe)
☐ Teams notificación recibida
☐ Protección duplicados probada

✅ LISTO PARA PRODUCCIÓN
```

---

## 🚀 ORDEN DE EJECUCIÓN (2 horas total)

```
HOY - FASE GIT (15 min):
  1. Commit feature/jiraXrayReport
  2. Merge a master
  3. Crear develop
  4. Limpiar ramas obsoletas

HOY - CONFIGURACIÓN (45 min):
  5. Teams: Crear webhook (5 min)
  6. Jenkins: Verificar tools (5 min)
  7. Jenkins: Crear credencial Teams (5 min)
  8. Jenkins: Verificar credencial Artifactory (5 min)
  9. Jenkins: Instalar plugin HTTP Request (5 min)
  10. Jenkins: Crear Pipeline Job (15 min)
  11. Jenkinsfile: Hacer 4 cambios (5 min)
  12. Git: Commit + push Jenkinsfile (2 min)

HOY - PRUEBAS (20 min):
  13. Build sin publicar (10 min)
  14. Build con publicar (10 min)

HOY - VERIFICACIÓN (10 min):
  15. Artifactory: Verificar v1.0.0 (5 min)
  16. Teams: Verificar notificación (2 min)
  17. Probar duplicados (3 min)

✅ TODO LISTO EN 2 HORAS
```

---

## 💡 TIPS IMPORTANTES

### Teams Webhook

```
✅ La URL es MUY larga (200+ caracteres)
✅ Copiar TODO sin espacios
✅ Guardar en lugar seguro (se muestra solo una vez)
✅ Si pierdes la URL, crear nuevo webhook
✅ Puedes tener múltiples webhooks en mismo canal
```

### Credencial Artifactory

```
✅ Reutilizar credencial existente "Artifactory"
✅ Cambiar ID en Jenkinsfile línea 77
✅ No necesitas crear nueva (menos mantenimiento)
✅ Si token expira, actualizar la existente
```

### Repositorio

```
⚠️ CRÍTICO: Confirmar nombre exacto
libs-release-local      → Código propio (PROBABLE)
libs-release-thirdparty → Librerías externas (NO)

Verificar en Artifactory UI o preguntar a DevOps
```

---

## 📞 PLANTILLA EMAIL DEVOPS (si tienes dudas)

```
Para: devops@scotia.com
Asunto: Confirmar repositorio Artifactory - QA Framework

Hola equipo,

Estoy configurando Jenkins para qa-scotia-frameworks.

Vi que existe:
- libs-release-local
- libs-release-thirdparty

¿Cuál debo usar para publicar NUESTRO framework Java (código propio)?

Mi suposición: libs-release-local

¿Es correcto?

Gracias,
Abel
```

---

**Versión:** 4.0.0  
**Fecha:** 2026-02-10  
**Archivo:** CONFIGURACION-JENKINS-DETALLADA.md

**Próximo paso:** Hacer los 4 cambios en Jenkinsfile y crear job en Jenkins

