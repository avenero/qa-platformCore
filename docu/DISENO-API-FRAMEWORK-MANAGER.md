# Diseño: Framework Manager API

**Fecha:** 25 Feb 2026  
**Estado:** Diseño confirmado — listo para implementar  
**Objetivo:** Exponer una API REST que permita al Backend (Spring Boot) listar los steps
disponibles del framework y disparar ejecuciones de tests, integrándose con un Frontend
React Native para que los QA del equipo puedan operar sin necesidad de Jenkins.

---

## 1. Contexto y restricción principal

El framework hoy es una **librería Java** — no una aplicación. Los módulos la importan
como dependencia Gradle desde Artifactory.

```
qa-scotia-frameworks  ←  librería publicada en Artifactory (JAR)
       ↑
qa-module-autos       ←  módulo consumidor
qa-module-banking     ←  módulo consumidor
```

El framework-core **no puede levantar un servidor HTTP** porque es un JAR de testing,
no una aplicación. Por eso la solución es un proyecto separado que lo importa.

---

## 2. Arquitectura general del sistema completo

```
┌─────────────────────┐
│  React Native (FE)  │  ← App móvil/web del equipo QA
└──────────┬──────────┘
           │ HTTP/REST
┌──────────▼──────────┐
│  Spring Boot (BC)   │  ← Backend de la compañera (repo aparte)
└──────────┬──────────┘
           │ HTTP/REST
┌──────────▼────────────────────────────────────────────────────────┐
│  qa-framework-manager  (Spring Boot, puerto 8090)                  │
│  ← REPO INDEPENDIENTE — esto es lo que construimos                │
│                                                                    │
│  ┌──────────────────────────┐                                     │
│  │ StepIntrospectionService │  Reflexión sobre JARs del core      │
│  └──────────────────────────┘                                     │
│  ┌──────────────────────────┐                                     │
│  │ TestExecutionService     │  Lanza ./gradlew de forma async     │
│  └──────────────────────────┘                                     │
│                                                                    │
│  features-repo/          ← git submodule DENTRO de este repo      │
│  │  (se actualiza con: git submodule update --remote)             │
│  ├── qa-autos/                                                     │
│  │   ├── features/       ← apuntado por cucumber.features         │
│  │   ├── steps/          ← steps de negocio custom (opcional)     │
│  │   └── steps-manifest.json                                      │
│  ├── qa-banking/                                                   │
│  └── qa-mobile/                                                    │
└────────────────────────────────────────────────────────────────────┘
           │ importa JARs como dependencia Gradle (igual que los módulos)
┌──────────▼─────────────────────────────────────────────┐      ┌──────────────────────────────┐
│  qa-scotia-frameworks                                   │      │  qa-features-repo            │
│  publicado en Artifactory como JAR                      │◄─────│  repo en Bitbucket           │
│  - api-core                                             │  git │  ← los QA pushean sus        │
│  - web-core                                             │  sub │    features y steps aquí     │
│  - common                                               │  mod │                              │
│                                                         │  ule │  ├── qa-autos/               │
│  features/   ← git submodule → qa-features-repo        │      │  │   ├── features/            │
│  ├── qa-autos/                                          │      │  │   ├── steps/               │
│  │   ├── features/                                      │      │  │   └── steps-manifest.json  │
│  │   ├── steps/                                         │      │  ├── qa-banking/              │
│  │   └── steps-manifest.json                           │      │  └── qa-mobile/               │
│  ├── qa-banking/                                        │      └──────────────────────────────┘
│  └── qa-mobile/                                         │
└─────────────────────────────────────────────────────────┘
```

**Flujo de actualización del submodule:**

```
QA del equipo  →  pushea features/steps  →  qa-features-repo (Bitbucket)
                                                    │
                              git submodule update --remote
                                          ┌─────────┴──────────┐
                                          ▼                     ▼
                              qa-scotia-frameworks/      qa-framework-manager/
                              features/                  features-repo/
                              (mismo contenido — dos repos apuntan al mismo submodule)
```

Tanto `qa-scotia-frameworks` como `qa-framework-manager` tienen el `qa-features-repo`
referenciado como git submodule. El contenido es el mismo — la diferencia es el uso:
- En `qa-scotia-frameworks/features/` → el runner de Gradle sabe dónde están las features para ejecutar
- En `qa-framework-manager/features-repo/` → el manager lee el catálogo y construye el comando de ejecución

---

## 3. Decisión: Repo independiente (confirmado ✅)

**`framework-manager` vive en su propio repositorio**, no dentro de `qa-scotia-frameworks`.

**Motivos:**
- Es una *aplicación* (tiene `main`, servidor HTTP, ciclo de vida propio) — no una librería
- Mezclarla con el core confunde responsabilidades y complica el pipeline de publicación
- Puede evolucionar a su propio ritmo sin afectar las versiones del framework
- Importa `api-core`, `web-core` y `common` como dependencias Gradle desde Artifactory,
  exactamente igual que hacen los módulos hoy

**Estructura del nuevo repo:**
```
qa-framework-manager/
  ├── build.gradle
  ├── settings.gradle
  ├── features/               ← git submodule → qa-features-repo (mismo que el core)
  │   ├── qa-autos/
  │   ├── qa-banking/
  │   └── qa-mobile/
  └── src/main/java/
        └── com/scotia/qa/manager/
              ├── FrameworkManagerApplication.java
              ├── api/
              │   ├── StepController.java
              │   └── ExecutionController.java
              ├── service/
              │   ├── StepIntrospectionService.java
              │   └── TestExecutionService.java
              ├── model/
              │   ├── StepDefinition.java
              │   ├── ExecutionRequest.java
              │   └── ExecutionResult.java
              └── config/
                  └── ManagerConfig.java
```

> **Nota:** Tanto `qa-scotia-frameworks` como `qa-framework-manager` apuntan al **mismo**
> `qa-features-repo` como git submodule. Cuando un QA pushea nuevas features al repo
> centralizado, ambos repos se actualizan con `git submodule update --remote`.

---

## 4. Decisión: Git Submodule para features (confirmado ✅)

**Se usa un repo centralizado de features referenciado como git submodule.**

### Por qué esta opción gana

| Aspecto | Git Submodule | Módulos independientes |
|---|---|---|
| Acceso a features desde el manager | ✅ Un solo clone | ❌ Necesita clonar N repos |
| Métricas por proyecto | ✅ Branches por proyecto | ❌ Hay que consultar N repos |
| Ejecución remota | ✅ Path local conocido | ❌ Ruta variable por máquina |
| Visibilidad del estado | ✅ Todo en un lugar | ❌ Disperso |
| Simplicidad operativa | ✅ `git submodule update` | ❌ Scripts de sync custom |

### Estructura del repo de features

```
qa-features-repo/                    ← repo independiente en Bitbucket
  ├── qa-autos/
  │   ├── features/
  │   │   ├── Login/pantallaLogin.feature
  │   │   └── Prestamos/solicitarPrestamo.feature
  │   ├── steps/                      ← steps de negocio propios de este proyecto
  │   │   └── AutosSteps.java         (solo si el proyecto tiene steps custom)
  │   ├── config-qa.properties
  │   └── steps-manifest.json         ← declara los steps custom del proyecto
  ├── qa-banking/
  │   ├── features/
  │   ├── steps/
  │   └── steps-manifest.json
  └── qa-mobile/
      └── features/
```

### Steps de negocio por proyecto — solución con steps-manifest.json

Cada proyecto **puede tener sus propios steps de negocio** además de los del framework-core.
El manager necesita conocerlos para poder listarlos en el FE.

La solución: cada proyecto declara un `steps-manifest.json` que el manager lee al escanear
el submodule. Este archivo es simple y lo mantiene el equipo de cada proyecto:

```json
// qa-features-repo/qa-autos/steps-manifest.json
{
  "project": "qa-autos",
  "displayName": "Préstamos Automotor",
  "customSteps": [
    {
      "class": "AutosSteps",
      "file": "steps/AutosSteps.java",
      "description": "Steps específicos de préstamos automotor"
    }
  ]
}
```

El `StepIntrospectionService` combina dos fuentes al responder `GET /api/steps?project=qa-autos`:
1. **Steps del framework-core** — escaneados por reflexión sobre los JARs (ApiSteps, WebSteps, DB)
2. **Steps de negocio del proyecto** — parseados desde los archivos `.java` en `steps/` del submodule

Para los steps custom, el manager parsea las anotaciones directamente del código fuente
(no hace falta compilar — solo extrae el string del `@Given/@When/@Then`).

### Impacto en el comando de ejecución

El `TestExecutionService` ya no ejecuta un módulo Gradle independiente — ejecuta los tests
del **framework apuntando al directorio de features del submodule**:

```bash
# En lugar de: ./gradlew :qa-module-autos:test
# Se usa:
./gradlew :api-core:test \
  -Dcucumber.features=features/qa-autos/features \
  -Dcucumber.filter.tags="@EVAUT-55" \
  -Denv=qa
```

Esto significa que **el framework-manager tiene su propio runner Gradle** que apunta
dinámicamente al directorio de features del proyecto solicitado.

---

## 5. Decisión: Ejecución Asíncrona (confirmado ✅)

Los tests tardan entre 1 y 10 minutos. Una llamada HTTP que bloquea ese tiempo es inviable —
cualquier proxy, API Gateway o timeout de red la cortaría antes de que finalice.

### Flujo asíncrono completo

```
FE  →  BC: "ejecutar @EVAUT-55 en qa-autos"
BC  →  POST /api/executions  →  manager responde en ~100ms:
                                { executionId: "exec-001", status: "RUNNING" }

BC  →  GET /api/executions/exec-001  (cada 5 seg)  →  { status: "RUNNING"  }
BC  →  GET /api/executions/exec-001  (cada 5 seg)  →  { status: "RUNNING"  }
BC  →  GET /api/executions/exec-001                →  { status: "FINISHED",
                                                         result: "PASSED", ... }
BC  →  FE: notifica resultado final
```

### Qué tiene que implementar el BC (explicación para tu compañera)

El BC recibe el `executionId` del primer POST. A partir de ahí tiene que consultar
`GET /api/executions/{id}` periódicamente — eso se llama **polling**.

**Implementación recomendada en Spring Boot:**

```java
// En el servicio del BC — opción simple y directa
public ExecutionResult waitForResult(String executionId) {
    int maxAttempts = 120;  // 120 intentos x 5 seg = 10 minutos máximo
    int attempt = 0;

    while (attempt < maxAttempts) {
        ExecutionResult result = frameworkManagerClient.getExecution(executionId);

        if ("FINISHED".equals(result.getStatus()) || "ERROR".equals(result.getStatus())) {
            return result;  // listo — devolver al FE
        }

        Thread.sleep(5000);  // esperar 5 segundos
        attempt++;
    }
    throw new TimeoutException("La ejecución no terminó en 10 minutos");
}
```

**¿Por qué es importante que el BC implemente esto y no el FE directamente?**

- El FE (React Native) no debería hacer polling HTTP durante 10 minutos — consume batería,
  puede quedar en background y perder la conexión
- El BC actúa como intermediario: el FE le pide al BC "avisame cuando termine" y puede
  cerrar la pantalla. El BC notifica al FE cuando tiene el resultado final (push notification
  o WebSocket)
- El BC puede agregar lógica extra: reintentar si falla, guardar historial, enviar notificaciones

**Alternativa futura (v2):** El manager notifica al BC via webhook cuando termina.
El BC configura una URL de callback en el POST inicial. Elimina el polling completamente.
Se puede agregar sin romper el contrato actual.

---

## 6. API REST — Contrato final

### 6.1 Listar steps disponibles

```
GET /api/steps
GET /api/steps?layer=api
GET /api/steps?layer=web
GET /api/steps?layer=database
GET /api/steps?keyword=valido
```

> **Nota sobre los totales:** El `total` y los conteos por capa se calculan
> dinámicamente por reflexión sobre los JARs en classpath. Se actualizan automáticamente
> cada vez que se publica una nueva versión del framework-core en Artifactory y el
> framework-manager se reinicia — sin cambios en el código del manager.

**Response:**
```json
{
  "total": 87,
  "layers": { "api": 38, "web": 41, "database": 8 },
  "steps": [
    {
      "id": "api_001",
      "layer": "api",
      "keyword": "Given",
      "pattern": "el host {string} mas el contexto {string}",
      "description": "Configura el host base más el contexto del endpoint",
      "method": "usarHostMasElContexto",
      "class": "com.scotia.qa.apicore.steps.ApiSteps"
    }
  ]
}
```

### 6.2 Listar proyectos disponibles

```
GET /api/projects
```

**Response:**
```json
{
  "projects": [
    { "id": "qa-autos",   "name": "Préstamos Automotor", "branch": "develop", "featureCount": 12 },
    { "id": "qa-banking", "name": "Home Banking",         "branch": "develop", "featureCount": 8  }
  ]
}
```

### 6.3 Iniciar ejecución

```
POST /api/executions
```

**Request:**
```json
{
  "project": "qa-autos",
  "tags": "@EVAUT-55",
  "environment": "qa",
  "executionType": "TAG"
}
```

`executionType` puede ser: `TAG` | `SUITE` | `FEATURE`

**Response (inmediata ~100ms):**
```json
{
  "executionId": "exec-20260225-143012-001",
  "status": "RUNNING",
  "startedAt": "2026-02-25T14:30:12"
}
```

### 6.4 Consultar resultado

```
GET /api/executions/{executionId}
```

**Response:**
```json
{
  "executionId": "exec-20260225-143012-001",
  "project": "qa-autos",
  "status": "FINISHED",
  "result": "PASSED",
  "totalScenarios": 5,
  "passed": 4,
  "failed": 1,
  "skipped": 0,
  "duration": "2m 34s",
  "startedAt": "2026-02-25T14:30:12",
  "finishedAt": "2026-02-25T14:32:46",
  "failedScenarios": [
    {
      "name": "Validar mensaje de error al ingresar email",
      "feature": "Login/pantallaLogin.feature",
      "error": "Expected 400 but was 404"
    }
  ]
}
```

### 6.5 Historial de ejecuciones

```
GET /api/executions?project=qa-autos&limit=10
```

---

## 7. ExecutionRequest — Parámetros detallados

### Lo que el BC manda al manager

```json
{
  "project":       "qa-autos",       // REQUERIDO — carpeta en el submodule
  "environment":   "qa",             // REQUERIDO — qa | dev | prod
  "executionType": "TAG",            // REQUERIDO — TAG | SUITE | FEATURE
  "tags":          "@EVAUT-55",      // requerido si executionType=TAG
  "featurePath":   "Login/login.feature", // requerido si executionType=FEATURE
  "layer":         "api",            // OPCIONAL — api | web | all (default: all)
  "callbackUrl":   "https://bc/notify" // OPCIONAL — para webhook en v2
}
```

### Cómo se mapea cada parámetro al comando Gradle

| Parámetro del request | Fragmento generado en el comando |
|---|---|
| `project = "qa-autos"` | `-Dcucumber.features=features-repo/qa-autos/features` |
| `environment = "qa"` | `-Denv=qa` |
| `tags = "@EVAUT-55"` | `-Dcucumber.filter.tags="@EVAUT-55"` |
| `executionType = "SUITE"` | sin filtro de tags ni feature — corre todo |
| `executionType = "FEATURE"` + `featurePath` | `-Dcucumber.features=.../Login/login.feature` |
| `layer = "api"` | `:api-core:test` en lugar de `:api-core:test` + `:web-core:test` |

### Ejemplos de comandos generados

```bash
# Caso 1: ejecutar por tag en qa-autos
./gradlew :api-core:test \
  -Dcucumber.features=features/qa-autos/features \
  -Dcucumber.filter.tags="@EVAUT-55" \
  -Denv=qa

# Caso 2: ejecutar suite completa de qa-banking
./gradlew :api-core:test :web-core:test \
  -Dcucumber.features=features/qa-banking/features \
  -Denv=qa

# Caso 3: ejecutar una feature específica
./gradlew :web-core:test \
  -Dcucumber.features=features/qa-autos/features/Login/pantallaLogin.feature \
  -Denv=qa
```

### Construcción dinámica en TestExecutionService

```java
private List<String> buildGradleCommand(ExecutionRequest request) {
    List<String> cmd = new ArrayList<>();

    // Wrapper o instalación local
    cmd.add(isWindows() ? "gradlew.bat" : "./gradlew");

    // Subproyecto(s) a ejecutar según layer
    if ("web".equals(request.getLayer())) {
        cmd.add(":web-core:test");
    } else if ("api".equals(request.getLayer())) {
        cmd.add(":api-core:test");
    } else {
        cmd.add(":api-core:test");
        cmd.add(":web-core:test");
    }

    // Path de features del submodule
    String featuresPath = "features/" + request.getProject() + "/features";

    // Para FEATURE específica, apuntar al archivo directamente
    if (ExecutionType.FEATURE == request.getExecutionType()
            && request.getFeaturePath() != null) {
        cmd.add("-Dcucumber.features=" + featuresPath + "/" + request.getFeaturePath());
    } else {
        cmd.add("-Dcucumber.features=" + featuresPath);
    }

    // Filtro de tags (solo para TAG — SUITE no filtra)
    if (ExecutionType.TAG == request.getExecutionType()
            && request.getTags() != null && !request.getTags().isBlank()) {
        cmd.add("-Dcucumber.filter.tags=" + request.getTags());
    }

    // Ambiente
    cmd.add("-Denv=" + request.getEnvironment());

    return cmd;
}

---

## 8. Métricas por proyecto

```
GET /api/metrics/qa-autos
```

```json
{
  "project": "qa-autos",
  "totalExecutions": 47,
  "lastExecution": "2026-02-25T14:32:46",
  "successRate": "87%",
  "avgDuration": "3m 12s",
  "trend": [
    { "date": "2026-02-24", "result": "PASSED", "passed": 11, "failed": 1 },
    { "date": "2026-02-25", "result": "PASSED", "passed": 12, "failed": 0 }
  ],
  "topFailingScenarios": [
    { "name": "Validar mensaje de error email", "failCount": 3 }
  ]
}
```

---

## 9. Relación con Jenkins — sin conflictos ✅

El `framework-manager` y Jenkins son **dos caminos paralelos** para ejecutar los mismos tests.
No se reemplazan — coexisten.

```
Jenkins pipeline          framework-manager
     │                          │
     │  ./gradlew :test         │  ./gradlew :api-core:test
     │  -Dcucumber.tags=...     │  -Dcucumber.features=...
     ▼                          ▼
  Mismos JARs del framework — mismo código — mismos resultados
```

### Sin bloqueantes técnicos

- Jenkins sigue ejecutando sus pipelines normalmente — sin cambios en `pipeline.jenkins`
- El manager es un proceso separado que lanza sus propios subprocesos Gradle
- No comparten estado, no se pisan entre sí

### Único punto de atención: ejecuciones simultáneas sobre el mismo servidor

Si Jenkins lanza un build y al mismo tiempo alguien dispara una ejecución desde el FE,
ambos corren en paralelo y pueden saturar CPU, RAM y conexiones a BD de test.

**Mitigación:** el manager expone si puede aceptar más ejecuciones:

```
GET /api/executions/active
→ { "count": 1, "canAcceptMore": true }
```

### Jenkins vs Manager — responsabilidades claras

```
Jenkins     → CI/CD automático en cada push (build + quality gate + publish)
Manager     → Ejecución manual on-demand desde el FE del equipo QA
```

---

## 10. Qué podemos construir sin BD

Todo lo siguiente **no necesita BD** y se puede implementar de inmediato:

| Entregable | Depende de BD |
|---|---|
| `GET /api/steps` — catálogo completo y filtros | ❌ No |
| `GET /api/projects` — lista proyectos del submodule | ❌ No |
| `GET /api/steps?project=qa-autos` — steps core + steps custom del proyecto | ❌ No |
| `POST /api/executions` (dry-run y real) | ❌ No |
| `GET /api/executions/{id}` — resultado en memoria | ❌ No |
| `GET /api/executions` — historial persistido entre reinicios | ✅ Sí (v2) |
| `GET /api/metrics/{project}` — tendencias históricas | ✅ Sí (v2) |

**Para v1:** el historial se guarda en un archivo JSON local. Se pierde si el servidor se
reinicia, pero es suficiente para validar la solución completa con el equipo.

---

## 11. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Gradle falla por permisos | Health check al arrancar: `./gradlew --version` |
| Submodule desactualizado | `git submodule update --remote` antes de cada ejecución |
| Ejecuciones concurrentes saturan el servidor | Semáforo configurable: máximo N ejecuciones simultáneas |
| Manager se reinicia y pierde historial | JSON local en v1, BD en v2 |
| API sin autenticación | Spring Security con Basic Auth desde el día 1 |
| Steps cambian al publicar nuevo JAR | Scan por reflexión se ejecuta fresh en cada arranque |

---

## 12. Plan de implementación

### Fase 1 — Catálogo de steps (entregable para el BC esta semana)
1. Crear repo `qa-framework-manager` con `build.gradle` (Spring Boot + core como dependencia)
2. Implementar `StepIntrospectionService` con reflexión sobre ApiSteps + WebSteps + DB
3. Exponer `GET /api/steps`, `GET /api/steps?project=X` y `GET /api/projects`
4. Validar contrato con la compañera del BC

### Fase 2 — Ejecución de tests
5. Configurar git submodule `features-repo`
6. Implementar `TestExecutionService` modo dry-run primero
7. Exponer `POST /api/executions` y `GET /api/executions/{id}`
8. Activar ejecución real y validar end-to-end con `qa-autos`

### Fase 3 — Métricas e historial
9. Persistir historial de ejecuciones en BD
10. Exponer `GET /api/metrics/{project}`
11. Integración final FE ↔ BC ↔ Manager

---

## 13. Pendiente de confirmar con el equipo

| Item | Decisión requerida |
|---|---|
| Autenticación de la API | ¿Basic Auth, Bearer Token o red interna sin auth? |
| Servidor donde corre el manager | ¿Jenkins, servidor dedicado o Docker? |
| Nombre del repo de features | ¿`qa-features-repo` u otro nombre en Bitbucket? |
| Persistencia v1 | ¿JSON local suficiente o se necesita BD desde el inicio? |

Con el git submodule y el historial de ejecuciones, el manager puede exponer:

```
GET /api/metrics/qa-autos
```

```json
{
  "project": "qa-autos",
  "totalExecutions": 47,
  "lastExecution": "2026-02-25T14:32:46",
  "successRate": "87%",
  "avgDuration": "3m 12s",
  "trend": [
    { "date": "2026-02-24", "result": "PASSED", "passed": 11, "failed": 1 },
    { "date": "2026-02-25", "result": "PASSED", "passed": 12, "failed": 0 }
  ],
  "topFailingScenarios": [
    { "name": "Validar mensaje de error email", "failCount": 3 }
  ]
}
```

---

## 9. Relación con Jenkins — sin conflictos ✅

El `framework-manager` y Jenkins son **dos caminos paralelos** para ejecutar los mismos tests.
No se reemplazan — coexisten.

```
Jenkins pipeline          framework-manager
     │                          │
     │  ./gradlew :test         │  ./gradlew :api-core:test
     │  -Dcucumber.tags=...     │  -Dcucumber.features=...
     ▼                          ▼
  Mismos JARs del framework — mismo código — mismos resultados
```

### Sin bloqueantes técnicos

- Jenkins sigue ejecutando sus pipelines normalmente — no hay cambios en `pipeline.jenkins`
- El manager es un proceso separado que lanza sus propios subprocesos Gradle
- No comparten estado, no se pisan entre sí

### Único punto de atención: ejecuciones simultáneas sobre el mismo servidor

Si Jenkins lanza un build Y al mismo tiempo alguien dispara una ejecución desde el FE,
ambos procesos corren en paralelo sobre el mismo servidor. Eso puede saturar recursos
(CPU, RAM, conexiones a BD de test).

**Mitigación:** el manager expone un endpoint de salud que indica si hay ejecuciones activas:

```
GET /api/executions/active
→ { "count": 1, "canAcceptMore": true }
```

El BC puede consultarlo antes de lanzar una nueva ejecución y avisarle al FE si el
servidor está ocupado.

### Jenkins como fuente de verdad para CI/CD

El manager **no reemplaza Jenkins** para el pipeline de CI/CD (build, quality gate,
publicar a Artifactory). Eso sigue siendo responsabilidad exclusiva de Jenkins.

El manager resuelve un caso de uso distinto: **ejecución on-demand desde el FE**,
que hoy no existe y que un QA haría manualmente desde Jenkins.

```
Jenkins     → CI/CD automático en cada push (build + quality gate + publish)
Manager     → Ejecución manual on-demand desde el FE del equipo QA
```

---

## 10. Qué podemos construir sin BD (Fase 1 completa)

| Riesgo | Mitigación |
|---|---|
| Gradle falla por permisos en el servidor | Health check al arrancar que valida que `./gradlew --version` funciona |
| El submodule está desactualizado | `git submodule update --remote` antes de cada ejecución |
| Ejecuciones concurrentes saturan el servidor | Semáforo: máximo N ejecuciones simultáneas (configurable) |
| El manager se reinicia y pierde el historial en memoria | Persistir resultados en archivo JSON local (v1) o base de datos (v2) |
| La API queda expuesta sin autenticación | Spring Security con Basic Auth en red interna desde el día 1 |
| Los steps cambian al publicar nueva versión del JAR | El scan por reflexión se ejecuta fresh en cada arranque del manager |

---

## 10. Plan de implementación (en orden)

### Fase 1 — Catálogo de steps (sin ejecución)
1. Crear repo `qa-framework-manager` con `build.gradle` (Spring Boot + dependencias del core)
2. Implementar `StepIntrospectionService` con reflexión sobre ApiSteps + WebSteps + DB
3. Exponer `GET /api/steps` y `GET /api/projects`
4. Validar con el BC de tu compañera que recibe el contrato correcto

### Fase 2 — Ejecución de tests
5. Configurar el git submodule `features-repo` en el repo del manager
6. Implementar `TestExecutionService` con modo **dry-run** primero (construye el comando pero no lo ejecuta)
7. Exponer `POST /api/executions` y `GET /api/executions/{id}`
8. Activar ejecución real y validar end-to-end con un proyecto real (`qa-autos`)

### Fase 3 — Métricas
9. Persistir historial de ejecuciones (JSON local en v1)
10. Exponer `GET /api/metrics/{project}`
11. Integrar con el FE a través del BC

---

## 11. Pendiente de confirmar con el equipo

| Item | Decisión requerida |
|---|---|
| Autenticación de la API | ¿Basic Auth, Bearer Token o red interna sin auth? |
| Servidor donde corre el manager | ¿Jenkins, servidor dedicado o Docker? |
| Steps de negocio custom | ¿El catálogo incluye solo steps del core o también steps de cada módulo? |
| Nombre del repo de features centralizado | ¿`qa-features-repo` u otro nombre en Bitbucket? |
| Persistencia del historial | ¿JSON local es suficiente para v1 o se necesita BD desde el inicio? |
