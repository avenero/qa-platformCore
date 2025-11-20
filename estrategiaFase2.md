# 🚀 ESTRATEGIA FASE 2: PROCESOS QA AVANZADOS Y HERRAMIENTAS
## Framework Scotia QA - Evolución Post-Refactoring

> **Última actualización**: 19 de Noviembre, 2025  
> **Estado**: Arquitectura Fase 1 completada ✅ | Fase 2 en definición

---

## 📋 **CONTEXTO Y ALCANCE**

### **🎯 Situación Post-Refactoring (Fase 1 Completada)**
- **Arquitectura de interfaces**: ✅ Implementada y estabilizada
- **Framework Scotia QA**: ✅ Funcionando con patrón Interface + Factory + Specialization
- **Módulo Common**: ✅ Refactorizado completamente (sin Spring Boot)
- **Documentación**: ✅ FRAMEWORK-GUIDE.md + common/README.md consolidados
- **Testing local**: ✅ Validado sin dependencias externas
- **Equipo**: 10-15 QA Engineers familiarizándose con nueva arquitectura

### **🎯 Objetivo Fase 2 (Actualizado)**
Implementar **procesos QA críticos probables 100% localmente** antes de integrar con infraestructura externa:

#### **✅ PRIORIDAD ALTA - IMPLEMENTAR INMEDIATAMENTE (Esta Semana)**
1. **Quality Gates Scripts** → Validación automática local
2. **Feature Flags System** → Despliegue controlado de features
3. **Semantic Versioning** → Versionado automático del framework

#### **✅ PRIORIDAD MEDIA - IMPLEMENTAR A CORTO PLAZO (2-4 Semanas)**
4. **Contract Testing (Pact)** → Validar compatibilidad entre frameworks
5. **Security Scanner Básico** → Detección de vulnerabilidades locales
6. **Performance Baselines** → Métricas de performance automatizadas
7. **Flaky Test Detection** → Identificar tests inestables

#### **⚠️ REQUIEREN INFRAESTRUCTURA EXTERNA (Futuro)**
- Pull Request Governance (requiere Bitbucket Server + webhooks)
- Jenkins Pipeline Enhancement (requiere Jenkins Server)
- ELK Stack Integration (requiere infraestructura externa)
- Docker Environment Management (requiere Docker registry corporativo)

---

## 📊 **RESUMEN EJECUTIVO - QUÉ IMPLEMENTAR AHORA**

```yaml
✅ IMPLEMENTABLES 100% LOCALMENTE (Sin infraestructura externa):
├── Quality Gates Scripts         → 1-2 horas
├── Feature Flags System          → 2-3 horas  
├── Semantic Versioning           → 1-2 horas
├── Contract Testing (Pact)       → 2-3 horas
├── Security Scanner Básico       → 1 hora
├── Performance Baselines         → 2-3 horas
└── Flaky Test Detection          → 2-3 horas

⚠️ IMPLEMENTABLES PARCIALMENTE (Setup local + infra externa):
├── Pull Request Governance       → Solo preparar scripts
├── Jenkins Pipeline              → Solo preparar Jenkinsfile
└── Bitbucket Pipelines           → Solo crear YAML

❌ NO IMPLEMENTABLES LOCALMENTE (Requieren infra compleja):
├── ELK Stack Integration
├── Grafana Dashboards
├── Vault Secrets Management
└── Docker Registry Corporativo
```

---

## 🎯 **ROADMAP DE IMPLEMENTACIÓN RECOMENDADO**

### **📅 SEMANA 1: FUNDAMENTOS CRÍTICOS**
**Objetivo**: Validación automática y control de features

#### **Día 1-2: Quality Gates Scripts** ⭐ CRÍTICO
- **Tiempo**: 1-2 horas
- **Beneficio**: Validación automática antes de commits
- **Implementación**: Ver sección detallada abajo

#### **Día 2-4: Feature Flags System** ⭐ CRÍTICO  
- **Tiempo**: 2-3 horas
- **Beneficio**: Despliegue controlado, A/B testing, rollback sin redeploy
- **Implementación**: Ver sección detallada abajo

#### **Día 4-5: Semantic Versioning**
- **Tiempo**: 1-2 horas
- **Beneficio**: Versionado automático del framework
- **Implementación**: Ver sección detallada abajo

---

### **📅 SEMANA 2-3: COMPATIBILIDAD Y SEGURIDAD**
**Objetivo**: Garantizar calidad y seguridad del framework

#### **Contract Testing (Pact)**
- **Tiempo**: 2-3 horas
- **Beneficio**: Validar compatibilidad entre api-core, mobile-core, web-core
- **Implementación**: Ver sección detallada abajo

#### **Security Scanner Básico**
- **Tiempo**: 1 hora
- **Beneficio**: Detección temprana de vulnerabilidades
- **Implementación**: Ver sección detallada abajo

---

### **📅 SEMANA 3-4: OBSERVABILIDAD Y ESTABILIDAD**
**Objetivo**: Métricas y detección de problemas

#### **Performance Baselines**
- **Tiempo**: 2-3 horas
- **Beneficio**: Detectar regresiones de performance
- **Implementación**: Ver sección detallada abajo

#### **Flaky Test Detection**
- **Tiempo**: 2-3 horas
- **Beneficio**: Identificar tests inestables
- **Implementación**: Ver sección detallada abajo

---

## 🚀 **IMPLEMENTACIONES CRÍTICAS - DETALLE COMPLETO**

---

## **1. 🔍 QUALITY GATES SCRIPTS** ⭐ PRIORIDAD MÁXIMA

### **🔧 INTEGRACIÓN CON BITBUCKET**

> **Objetivo**: Automatizar Quality Gates en Bitbucket para que NADIE pueda hacer merge sin pasar validaciones.

---

#### **📊 FLUJO END-TO-END: Developer → PR → Quality Gates → Merge**

```yaml
🧑‍💻 PASO 1: Developer Trabaja en Feature
├── Crea branch: git checkout -b feature/QAAUY-123-new-validation
├── Hace cambios en common/src/main/java/...
├── Ejecuta quality gates LOCAL: ./scripts/quality-gates.sh
├── Si PASA → commit + push
└── Si FALLA → arregla y re-ejecuta

📤 PASO 2: Developer Crea Pull Request
├── Abre PR desde feature/QAAUY-123 → develop
├── Bitbucket aplica PR Template automáticamente
├── Developer completa checklist del template
├── Bitbucket asigna Default Reviewers automáticamente
└── Webhook notifica a Jenkins

🔍 PASO 3: Jenkins Ejecuta Quality Gates (Automático)
├── Jenkins recibe webhook de Bitbucket
├── Clona repo en branch feature/QAAUY-123
├── Ejecuta: ./scripts/quality-gates.sh
├── Publica resultado a Bitbucket como Build Status
└── Si FALLA → Bitbucket bloquea merge + notifica developer

👀 PASO 4: Code Review (Humano)
├── Reviewers revisan código
├── Validan que checklist está completo
├── Agregan comments/tasks si es necesario
├── Developer resuelve tasks
└── Reviewers aprueban (2 approvals para main)

✅ PASO 5: Merge (Solo si Quality Gates + Approvals OK)
├── Bitbucket valida:
│   ├── ✅ Build status = Success (Quality Gates pasaron)
│   ├── ✅ Minimum approvals = 2
│   ├── ✅ All tasks resolved
│   └── ✅ No merge conflicts
├── Developer hace merge
├── Bitbucket auto-delete feature branch
├── Webhook notifica a Jenkins para deploy (si main)
└── Notification a Slack: "PR merged ✅"

🚀 PASO 6: Deploy Automático (Solo Main)
├── Jenkins detecta push a main
├── Ejecuta semantic versioning: v1.2.3 → v1.2.4
├── Compila + tests + quality gates nuevamente
├── Publica a Artifactory: common:1.2.4
├── Notifica a equipos: "Nueva versión disponible"
└── Módulos pueden actualizar: implementation 'com.scotia.qa:common:1.2.4'
```

---

#### **A. Configuraciones a Nivel de Repositorio Bitbucket**

##### **1️⃣ Branch Permissions (Branch Protection Rules)**

**Ubicación en Bitbucket**: `Repository Settings > Branch permissions`

```yaml
Configuraciones Recomendadas para FRAMEWORK (qa-scotia-frameworks):

🔒 Proteger rama main:
├── Require pull requests: ✅ Enabled
├── Minimum approvals: 2 reviewers
├── Dismiss stale approvals: ✅ Enabled (al hacer nuevos commits)
├── Require tasks to be completed: ✅ Enabled
├── Require builds to pass: ✅ Enabled (Quality Gates Script)
└── Restrict push: Solo QA Lead + DevOps

🔒 Proteger rama develop:
├── Require pull requests: ✅ Enabled
├── Minimum approvals: 1 reviewer
├── Require builds to pass: ✅ Enabled
└── Any developer can push

🔓 Feature branches (feature/*):
├── No restrictions
└── Developers pueden crear/push libremente
```

**Configuraciones para MÓDULOS (qa-module-xxx)**:
```yaml
🔒 Proteger rama main:
├── Require pull requests: ✅ Enabled
├── Minimum approvals: 1 reviewer
├── Require builds to pass: ✅ Enabled (Quality Gates Module)
└── Restrict push: Solo Module Owner

🔓 Feature branches:
├── No restrictions
└── Auto-delete after merge: ✅ Enabled
```

---

##### **2️⃣ Default Reviewers (Revisores Automáticos)**

**Ubicación en Bitbucket**: `Repository Settings > Default reviewers`

```yaml
Para FRAMEWORK (qa-scotia-frameworks):

Regla 1 - Cambios en common/:
├── Pattern: common/**/*
├── Reviewers: @qa-core-team, @qa-lead
├── Required approvals: 2
└── Reason: Common es base de todo, requiere revisión exhaustiva

Regla 2 - Cambios en api-core/:
├── Pattern: api-core/**/*
├── Reviewers: @api-qa-team
├── Required approvals: 1
└── Reason: Especialistas API validan cambios

Regla 3 - Cambios en scripts/:
├── Pattern: scripts/**/*
├── Reviewers: @qa-lead, @devops-team
├── Required approvals: 2
└── Reason: Scripts afectan CI/CD, requiere aprobación DevOps

Regla 4 - Cambios en documentación:
├── Pattern: **/*.md
├── Reviewers: @qa-lead
├── Required approvals: 1
└── Reason: Validar que documentación es clara
```

**Para MÓDULOS**:
```yaml
Regla 1 - Cambios en features/:
├── Pattern: src/test/resources/features/**/*
├── Reviewers: @qa-functional-team
├── Required approvals: 1
└── Reason: Validar que scenarios son correctos

Regla 2 - Cambios en steps/:
├── Pattern: src/test/java/**/steps/**/*
├── Reviewers: @qa-automation-team
├── Required approvals: 1
└── Reason: Steps deben seguir convenciones del framework
```

---

##### **3️⃣ Merge Checks (Validaciones Pre-Merge)**

**Ubicación en Bitbucket**: `Repository Settings > Merge checks`

```yaml
Configurar para FRAMEWORK:

✅ All tasks must be resolved:
├── Enabled: ✅ Yes
└── Bloquea merge si hay tasks pendientes en PR

✅ Minimum successful builds (Quality Gates):
├── Build: "Quality Gates - Framework"
├── Status required: Success
└── Bloquea merge si quality gates fallan

✅ Minimum approvals:
├── Count: 2 (para main), 1 (para develop)
├── Reset on new commits: ✅ Yes
└── Authors cannot approve: ✅ Yes

✅ All reviewers must approve:
├── Enabled: ⚠️ No (puede ser muy restrictivo)
└── Suficiente con minimum approvals

❌ No merge commits:
├── Enabled: ⚠️ Opcional
└── Fuerza squash/rebase (puede complicar historial)
```

**Para MÓDULOS**:
```yaml
✅ All tasks resolved: ✅ Yes
✅ Minimum build: "Quality Gates - Module" → Success
✅ Minimum approvals: 1
✅ Reset on new commits: ✅ Yes
```

---

##### **4️⃣ PR Templates (Plantillas de Pull Request)**

**Ubicación**: Crear archivo `.bitbucket/pull_request_template.md` en raíz del repo

**Para FRAMEWORK (qa-scotia-frameworks/.bitbucket/pull_request_template.md)**:
```markdown
## 📋 Pull Request - Framework Scotia QA

### 🎯 Tipo de Cambio
- [ ] 🐛 Bug Fix (cambio no-breaking que arregla un issue)
- [ ] ✨ New Feature (cambio no-breaking que agrega funcionalidad)
- [ ] 💥 Breaking Change (fix o feature que rompe compatibilidad)
- [ ] 📚 Documentation (cambios solo en documentación)
- [ ] 🔧 Refactoring (cambio que no arregla bug ni agrega feature)
- [ ] ⚡ Performance (mejora de performance)
- [ ] 🧪 Tests (agregar/modificar tests)

### 📝 Descripción del Cambio
<!-- Describe qué hace este PR y por qué es necesario -->

### 🔗 Issue/Ticket Relacionado
<!-- Link a Jira: QAAUY-XXX -->
Jira: [QAAUY-XXX](https://jira.scotia.com/browse/QAAUY-XXX)

### 🧪 Testing Realizado
- [ ] ✅ Tests unitarios ejecutados localmente
- [ ] ✅ Quality Gates Script ejecutado localmente
- [ ] ✅ Contract tests validados (si aplica)
- [ ] ✅ Cross-framework testing (common + api-core + mobile-core + web-core)
- [ ] ✅ Módulos consumidores probados con nueva versión

### 📊 Impacto en Frameworks
<!-- Marca los frameworks afectados -->
- [ ] 🔵 common (base para todos)
- [ ] 🟢 api-core
- [ ] 🟡 mobile-core
- [ ] 🟠 web-core

### 💥 Breaking Changes
<!-- Si marcaste Breaking Change arriba, describe el impacto -->
- [ ] ❌ No hay breaking changes
- [ ] ⚠️ Sí hay breaking changes (describe abajo):

**Describe los breaking changes**:
<!-- Qué se rompe y cómo migrarlo -->

### 📚 Documentación Actualizada
- [ ] ✅ README.md actualizado (si aplica)
- [ ] ✅ FRAMEWORK-GUIDE.md actualizado (si aplica)
- [ ] ✅ JavaDoc agregado/actualizado en métodos públicos
- [ ] ✅ Ejemplos de uso agregados (si es feature nueva)

### ✅ Checklist Pre-Merge
- [ ] ✅ Quality Gates Script pasa localmente
- [ ] ✅ No hay console.logs/System.out.println olvidados
- [ ] ✅ No hay TODOs sin ticket asociado
- [ ] ✅ No hay secrets/passwords hardcodeados
- [ ] ✅ Código sigue convenciones del framework
- [ ] ✅ Tests tienen cobertura >80%

### 📸 Screenshots (si aplica)
<!-- Si hay cambios visuales en logs, agrega screenshots -->

### 🚀 Deployment Notes
<!-- Instrucciones especiales para deployment (si aplica) -->
```

**Para MÓDULOS (qa-module-xxx/.bitbucket/pull_request_template.md)**:
```markdown
## 📋 Pull Request - Módulo QA

### 🎯 Tipo de Cambio
- [ ] 🧪 New Test Scenarios
- [ ] 🐛 Bug Fix en Tests
- [ ] 📚 Documentation
- [ ] 🔧 Refactoring de Steps

### 📝 Descripción
<!-- Qué scenarios/tests agrega o modifica este PR -->

### 🔗 Jira Ticket
Jira: [QAAUY-XXX](https://jira.scotia.com/browse/QAAUY-XXX)

### 🧪 Testing Realizado
- [ ] ✅ Quality Gates Module ejecutado localmente
- [ ] ✅ Features ejecutadas manualmente
- [ ] ✅ No hay tests flaky

### ✅ Checklist
- [ ] ✅ Features siguen convenciones Gherkin
- [ ] ✅ Steps reutilizan framework (no duplican lógica)
- [ ] ✅ No hay datos sensibles en features
- [ ] ✅ Documentación de scenarios actualizada
```

---

##### **5️⃣ Webhooks para Jenkins (Automated Builds)**

**Ubicación en Bitbucket**: `Repository Settings > Webhooks`

```yaml
Webhook 1 - Trigger Quality Gates en PR:
├── Name: "Jenkins - Quality Gates PR Validation"
├── URL: https://jenkins.scotia.com/bitbucket-hook/
├── Status: Active
├── Triggers:
│   ├── Pull request: opened ✅
│   ├── Pull request: updated ✅ (nuevos commits)
│   ├── Pull request: approved ❌
│   └── Pull request: merged ❌
├── Headers:
│   └── Authorization: Bearer <jenkins-token>
└── SSL/TLS: ✅ Verify

Webhook 2 - Trigger Deploy en Merge a Main:
├── Name: "Jenkins - Deploy Framework"
├── URL: https://jenkins.scotia.com/bitbucket-hook/deploy
├── Status: Active
├── Triggers:
│   ├── Repository: push ✅ (solo main)
│   └── Branch: main
└── Jenkins Job: "deploy-framework-to-artifactory"

Webhook 3 - Notificaciones a Slack/Teams:
├── Name: "Slack - PR Notifications"
├── URL: https://hooks.slack.com/services/XXX
├── Triggers:
│   ├── Pull request: opened ✅
│   ├── Pull request: approved ✅
│   ├── Pull request: merged ✅
│   └── Pull request: declined ✅
└── Canal: #qa-framework-prs
```

---

##### **6️⃣ Repository Variables (Configuración Centralizada)**

**Ubicación en Bitbucket**: `Repository Settings > Repository variables`

```yaml
Variables para CI/CD y Quality Gates:

FRAMEWORK (qa-scotia-frameworks):
├── ARTIFACTORY_URL: https://artifactory.scotia.com
├── FRAMEWORK_VERSION: (automático vía semantic versioning)
├── MIN_COVERAGE: 80
├── QUALITY_GATES_ENABLED: true
├── CONTRACT_TESTING_ENABLED: true
├── SECURITY_SCAN_ENABLED: true
└── SLACK_WEBHOOK: https://hooks.slack.com/...

MÓDULOS (qa-module-xxx):
├── FRAMEWORK_VERSION: 1.2.3 (versión que usa)
├── MODULE_NAME: loan-automation
├── MIN_COVERAGE: 70 (puede ser menor que framework)
├── QUALITY_GATES_ENABLED: true
└── ENVIRONMENT: dev/qa/prod
```

---

##### **7️⃣ Bitbucket Pipelines (CI/CD Nativo de Bitbucket)**

**Ubicación**: Crear archivo `bitbucket-pipelines.yml` en raíz del repo

**Para FRAMEWORK (qa-scotia-frameworks/bitbucket-pipelines.yml)**:
```yaml
image: gradle:8.5-jdk21

definitions:
  steps:
    - step: &quality-gates
        name: 🔍 Quality Gates - Framework
        caches:
          - gradle
        script:
          # Ejecutar quality gates script
          - chmod +x scripts/quality-gates.sh
          - ./scripts/quality-gates.sh
        artifacts:
          - build/reports/**
          - build/test-results/**

    - step: &contract-tests
        name: 🌐 Contract Testing
        caches:
          - gradle
        script:
          - ./gradlew contractTest
        artifacts:
          - build/pacts/**

    - step: &publish-artifactory
        name: 📦 Publish to Artifactory
        deployment: production
        script:
          # Semantic versioning
          - chmod +x scripts/semantic-versioning.sh
          - export NEW_VERSION=$(./scripts/semantic-versioning.sh)
          - echo "Publishing version ${NEW_VERSION}"
          
          # Publish common
          - ./gradlew :common:clean :common:build :common:publish
          
          # Publish api-core
          - ./gradlew :api-core:clean :api-core:build :api-core:publish

pipelines:
  pull-requests:
    '**': # Todas las PRs
      - step: *quality-gates
      - step: *contract-tests

  branches:
    main:
      - step: *quality-gates
      - step: *contract-tests
      - step: *publish-artifactory

  custom:
    manual-quality-gates:
      - step: *quality-gates
```

**Para MÓDULOS (qa-module-xxx/bitbucket-pipelines.yml)**:
```yaml
image: gradle:8.5-jdk21

definitions:
  steps:
    - step: &quality-gates-module
        name: 🔍 Quality Gates - Module
        caches:
          - gradle
        script:
          # Descargar quality gates script
          - chmod +x scripts/download-quality-gates.sh
          - ./scripts/download-quality-gates.sh
          
          # Ejecutar quality gates
          - chmod +x scripts/quality-gates.sh
          - ./scripts/quality-gates.sh
        artifacts:
          - build/reports/**
          - build/cucumber.json

    - step: &run-tests
        name: 🧪 Run Cucumber Tests
        script:
          - ./gradlew clean test
        artifacts:
          - build/reports/**
          - build/cucumber.json

pipelines:
  pull-requests:
    '**':
      - step: *quality-gates-module
      - step: *run-tests

  branches:
    main:
      - step: *quality-gates-module
      - step: *run-tests
```

---

##### **8️⃣ Branch Workflow (Flujo de Trabajo de Ramas)**

**Estrategia Git Flow Adaptada para QA Framework**:

```yaml
Estructura de Ramas para FRAMEWORK:

main (protected):
├── Rama de producción
├── Solo merges desde release/* o hotfix/*
├── Cada merge = nueva versión publicada a Artifactory
├── Quality Gates obligatorios
└── Tag automático: v1.2.3

develop (semi-protected):
├── Rama de integración continua
├── Merges desde feature/*
├── Quality Gates obligatorios
└── Puede tener features experimentales

feature/* (no protected):
├── Creadas desde develop
├── Nombradas: feature/QAAUY-123-descripcion
├── Quality Gates opcionales (local)
└── Merge a develop vía PR

release/* (protected):
├── Creadas desde develop cuando ready for release
├── Nombradas: release/v1.2.0
├── Solo bug fixes permitidos
├── Merge a main (tag) y develop
└── Quality Gates obligatorios

hotfix/* (protected):
├── Creadas desde main para fixes urgentes
├── Nombradas: hotfix/v1.2.1-critical-bug
├── Merge a main (tag) y develop
└── Quality Gates obligatorios + validación extra
```

**Para MÓDULOS**:
```yaml
Estructura más simple:

main:
├── Código estable
└── Quality Gates obligatorios

feature/*:
├── Desarrollo normal
└── Merge a main vía PR
```

---

##### **9️⃣ PR Tasks (Tareas Obligatorias en PR)**

**Configuración manual en cada PR (o automatizada con bot)**:

```yaml
Tareas Automáticas al Crear PR en FRAMEWORK:

✅ Checklist Pre-Review:
├── [ ] Quality Gates Script ejecutado localmente
├── [ ] Tests unitarios pasan (>80% coverage)
├── [ ] Contract tests validados
├── [ ] Breaking changes documentados
├── [ ] JavaDoc actualizado
└── [ ] README actualizado

✅ Revisión de Seguridad:
├── [ ] No hay secrets hardcodeados
├── [ ] No hay console.log/System.out.println
├── [ ] Dependencias actualizadas
└── [ ] Security scan pasado

✅ Validación de Arquitectura:
├── [ ] Sigue patrón Interface + Factory
├── [ ] No rompe encapsulamiento
├── [ ] Reutiliza utilities existentes
└── [ ] No duplica lógica
```

---

##### **🔟 Bitbucket Access Keys (Para CI/CD)**

**Ubicación**: `Repository Settings > Access keys`

```yaml
Agregar SSH Keys para:

Jenkins Server:
├── Purpose: Pull código para builds
├── Type: Read-only
└── Key: <jenkins-ssh-public-key>

Deployment Server:
├── Purpose: Deploy de módulos
├── Type: Read-only
└── Key: <deploy-server-ssh-key>
```

---

## **1. 🔍 QUALITY GATES SCRIPTS** ⭐ PRIORIDAD MÁXIMA

### **¿Por qué es CRÍTICO?**
- **10-15 personas** modificando framework → validación automática obligatoria
- **Prevención de breaking changes** → detectar problemas antes de commit
- **Ejecución 100% local** → no requiere infraestructura externa
- **Integrable con Git hooks** → validación en pre-commit/pre-push

### **📋 Checklist de Validaciones**
```yaml
Validaciones Incluidas:
✅ Compilación exitosa de common module
✅ Tests cross-framework (api-core, mobile-core, web-core)
✅ Cobertura de código mínima (80%)
✅ Detección de breaking changes en interfaces
✅ Security scan básico (secrets, vulnerabilidades)
✅ Validación de nomenclatura de clases
✅ Verificación de JavaDoc en métodos públicos
```

### **Implementación Completa:**
```bash
# scripts/quality-gates.sh
#!/bin/bash

echo "🔍 Running Quality Gates for Framework Scotia..."

# 1. Validar que common compila sin errores
echo "📦 Testing common module compilation..."
./gradlew :common:clean :common:build
if [ $? -ne 0 ]; then
    echo "❌ Common module compilation failed"
    exit 1
fi

# 2. Verificar que no hay breaking changes en interfaces
echo "🔌 Checking for breaking changes in interfaces..."
java -cp "build/libs/*" BreakingChangeDetector \
    --baseline-version="$(git describe --tags --abbrev=0)" \
    --current-interfaces="common/src/main/java/com/scotia/qa/common/interfaces"

if [ $? -ne 0 ]; then
    echo "❌ Breaking changes detected in interfaces"
    exit 1
fi

# 3. Ejecutar tests de todos los frameworks
echo "🧪 Running cross-framework compatibility tests..."
./gradlew test -x :common:test
if [ $? -ne 0 ]; then
    echo "❌ Framework compatibility tests failed"
    exit 1
fi

# 4. Verificar cobertura de código mínima
echo "📊 Checking code coverage..."
./gradlew jacocoTestReport
COVERAGE=$(grep -o '[0-9]\+%' build/reports/jacoco/test/html/index.html | head -1 | sed 's/%//')
if [ "$COVERAGE" -lt 80 ]; then
    echo "❌ Code coverage ($COVERAGE%) below minimum (80%)"
    exit 1
fi

# 5. Security scan básico
echo "🛡️ Running security scan..."
grep -r "password\|secret\|key" --include="*.java" --include="*.properties" src/
if [ $? -eq 0 ]; then
    echo "⚠️  Potential secrets found - please review"
    # No falla, solo alerta
fi

echo "✅ All quality gates passed!"
```

#### **Configuración en Bitbucket:**
```yaml
# bitbucket-pipelines.yml
pipelines:
  pull-requests:
    '**':
      - step:
          name: Quality Gates
          script:
            - chmod +x scripts/quality-gates.sh
            - ./scripts/quality-gates.sh
          services:
            - docker
          
  branches:
    main:
      - step:
          name: Main Branch Validation
          script:
            - ./scripts/quality-gates.sh
            - ./scripts/semantic-versioning.sh
            
    develop:
      - step:
          name: Development Validation
          script:
            - ./scripts/quality-gates.sh
```

#### **Branch Protection Rules:**
```yaml
# Configuración en Bitbucket (via UI o API)
Branch: main
Protection Rules:
├── Require pull request reviews: 2 reviewers minimum
├── Require status checks: "Quality Gates" must pass
├── Require branches to be up to date: enabled
├── Restrict pushes to matching branches: enabled
└── Allow force pushes: disabled

Branch: develop  
Protection Rules:
├── Require pull request reviews: 1 reviewer minimum
├── Require status checks: "Quality Gates" must pass
└── Allow force pushes: disabled (only for hotfixes)
```

---

### **2. 📌 SEMANTIC VERSIONING PARA FRAMEWORK COMMON**

#### **¿Por qué es CRÍTICO?**
- **Dependency management** entre frameworks
- **Breaking change communication** clara al equipo
- **Release automation** basada en cambios
- **Rollback capabilities** cuando sea necesario

#### **Implementación:**
```bash
# scripts/semantic-versioning.sh
#!/bin/bash

# Obtener último tag
LAST_TAG=$(git describe --tags --abbrev=0)
echo "📋 Last version: $LAST_TAG"

# Analizar commits desde último tag
COMMITS=$(git log $LAST_TAG..HEAD --oneline)

# Detectar tipo de cambio
BREAKING_CHANGE=false
FEATURE_CHANGE=false
PATCH_CHANGE=false

# Analizar mensajes de commit
while IFS= read -r commit; do
    if [[ $commit == *"BREAKING CHANGE"* ]] || [[ $commit == *"feat!"* ]]; then
        BREAKING_CHANGE=true
    elif [[ $commit == *"feat:"* ]]; then
        FEATURE_CHANGE=true
    elif [[ $commit == *"fix:"* ]] || [[ $commit == *"refactor:"* ]]; then
        PATCH_CHANGE=true
    fi
done <<< "$COMMITS"

# Calcular nueva versión
IFS='.' read -r -a VERSION_PARTS <<< "${LAST_TAG//v/}"
MAJOR=${VERSION_PARTS[0]}
MINOR=${VERSION_PARTS[1]}
PATCH=${VERSION_PARTS[2]}

if [ "$BREAKING_CHANGE" = true ]; then
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
    CHANGE_TYPE="MAJOR"
elif [ "$FEATURE_CHANGE" = true ]; then
    MINOR=$((MINOR + 1))
    PATCH=0
    CHANGE_TYPE="MINOR"
elif [ "$PATCH_CHANGE" = true ]; then
    PATCH=$((PATCH + 1))
    CHANGE_TYPE="PATCH"
else
    echo "ℹ️  No version change needed"
    exit 0
fi

NEW_VERSION="v$MAJOR.$MINOR.$PATCH"
echo "🚀 New version: $NEW_VERSION ($CHANGE_TYPE)"

# Crear tag y actualizar build.gradle
git tag -a $NEW_VERSION -m "Release $NEW_VERSION"
sed -i "s/version = .*/version = '$MAJOR.$MINOR.$PATCH'/" build.gradle

# Generar changelog automático
echo "📝 Generating changelog..."
python scripts/generate-changelog.py --from=$LAST_TAG --to=$NEW_VERSION

echo "✅ Version updated to $NEW_VERSION"
```

#### **Integration con Gradle:**
```gradle
// build.gradle - common module
plugins {
    id 'maven-publish'
    id 'signing'
}

version = '2.1.0' // Actualizado automáticamente por script

publishing {
    publications {
        maven(MavenPublication) {
            from components.java
            
            pom {
                name = 'Scotia Framework Common'
                description = 'Common interfaces and implementations for Scotia QA Framework'
                url = 'https://bitbucket.org/scotia/qa-framework'
                
                scm {
                    connection = 'scm:git:git://bitbucket.org/scotia/qa-framework.git'
                    url = 'https://bitbucket.org/scotia/qa-framework'
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "ScotiaInternal"
            url = "${project.findProperty('nexusUrl') ?: 'http://nexus.scotia.internal/repository/maven-public/'}"
            credentials {
                username = project.findProperty('nexusUsername')
                password = project.findProperty('nexusPassword')  
            }
        }
    }
}

// Automatic versioning task
task updateVersion {
    doLast {
        exec {
            commandLine 'bash', 'scripts/semantic-versioning.sh'
        }
    }
}
```

---

---

#### **B. Ejemplos Prácticos de Configuración Bitbucket**

##### **Ejemplo 1: Configurar Branch Permission para Main (Framework)**

```yaml
Paso a paso en Bitbucket UI:

1. Ir a Repository → Settings → Branch permissions
2. Click en "Add permission"
3. Configurar:
   ├── Branch or pattern: main
   ├── Type: Branch
   ├── Write access: Restrict to:
   │   └── Select users: qa-lead, devops-admin
   ├── Merge via pull request: ✅ Required
   ├── Pull request merging:
   │   ├── Minimum approvals: 2
   │   ├── Default reviewers: ✅ Required
   │   └── Successful builds: ✅ Required
   └── Prevent changes to this branch
       └── Rewrite branch history: ✅ Prevent

4. Save

Resultado:
❌ Developer NO puede push directo a main
✅ Developer DEBE crear PR
✅ PR requiere 2 approvals + Quality Gates success
✅ Solo QA Lead puede emergency push (si necesario)
```

---

##### **Ejemplo 2: Configurar Webhook para Jenkins**

```yaml
Paso a paso en Bitbucket UI:

1. Ir a Repository → Settings → Webhooks
2. Click en "Add webhook"
3. Configurar:
   ├── Title: Jenkins Quality Gates PR Validation
   ├── URL: https://jenkins.scotia.com/bitbucket-hook/
   ├── Status: Active ✅
   ├── SSL/TLS: ✅ Verify certificate
   ├── Triggers:
   │   ├── Repository: ❌ (no marcar)
   │   ├── Pull request:
   │   │   ├── Created ✅
   │   │   ├── Updated ✅
   │   │   ├── Source branch updated ✅
   │   │   └── Merged ❌
   │   └── Build status: ❌ (no marcar)
   └── Headers (opcional):
       └── Authorization: Bearer <token-jenkins>

4. Save
5. Test webhook: Click "View requests" → "Test connection"

Resultado:
✅ Cada vez que se crea/actualiza PR → Jenkins ejecuta Quality Gates
✅ Jenkins publica build status a Bitbucket
✅ Bitbucket muestra status en PR: ✅ Success / ❌ Failed
```

---

##### **Ejemplo 3: Configurar Default Reviewer para Cambios en Common**

```yaml
Paso a paso en Bitbucket UI:

1. Ir a Repository → Settings → Default reviewers
2. Click en "Add default reviewer"
3. Configurar:
   ├── Name: Common Module Changes
   ├── File path pattern: common/**/*
   ├── Match type: Glob
   ├── Reviewers:
   │   ├── Add user: qa-lead ✅
   │   ├── Add user: qa-core-member-1 ✅
   │   └── Add group: @qa-core-team ✅
   ├── Required approvals: 2
   └── Apply to all branches: ✅

4. Save

Resultado:
✅ Cualquier PR que toque common/ → auto-asigna qa-lead + qa-core-team
✅ Requiere 2 approvals antes de merge
✅ Notificaciones automáticas a reviewers
```

---

##### **Ejemplo 4: Bitbucket Pipelines para Quality Gates**

```yaml
Ejemplo Completo en bitbucket-pipelines.yml:

# bitbucket-pipelines.yml para FRAMEWORK
image: gradle:8.5-jdk21

definitions:
  caches:
    gradle: ~/.gradle

pipelines:
  # Ejecutar en TODAS las Pull Requests
  pull-requests:
    '**':
      - parallel:
          - step:
              name: 🔍 Quality Gates - Full Suite
              caches:
                - gradle
              script:
                - echo "🚀 Starting Quality Gates..."
                - chmod +x scripts/quality-gates.sh
                - ./scripts/quality-gates.sh
              artifacts:
                - build/reports/**
                - build/test-results/**
                - build/jacoco/**
          
          - step:
              name: 🧪 Unit Tests
              caches:
                - gradle
              script:
                - ./gradlew :common:test
                - ./gradlew :api-core:test
              artifacts:
                - build/test-results/**

  # Ejecutar en Push a Main (Deploy)
  branches:
    main:
      - step:
          name: 🔍 Quality Gates + Deploy
          deployment: production
          caches:
            - gradle
          script:
            - echo "🚀 Quality Gates for Production..."
            - ./scripts/quality-gates.sh
            
            - echo "📌 Calculating new version..."
            - chmod +x scripts/semantic-versioning.sh
            - export NEW_VERSION=$(./scripts/semantic-versioning.sh)
            - echo "New version: ${NEW_VERSION}"
            
            - echo "📦 Publishing to Artifactory..."
            - ./gradlew :common:publish
            - ./gradlew :api-core:publish
            
            - echo "✅ Deploy completed: ${NEW_VERSION}"

  # Manual pipeline para testing
  custom:
    test-quality-gates:
      - step:
          name: 🧪 Test Quality Gates Manually
          script:
            - ./scripts/quality-gates.sh

Resultado:
✅ Cada PR ejecuta Quality Gates automáticamente
✅ Bitbucket muestra resultado en PR UI
✅ Merge a main → deploy automático a Artifactory
✅ Developers pueden ejecutar manualmente para testing
```

---

#### **C. Troubleshooting Común en Bitbucket**

##### **Problema 1: PR No Muestra Build Status de Quality Gates**

```yaml
Síntoma:
❌ PR creada pero no aparece status de Jenkins/Bitbucket Pipelines

Causas Posibles:
1. Webhook no configurado correctamente
2. Jenkins no está publicando status a Bitbucket
3. Bitbucket Pipelines no tiene permisos

Solución:

Para Webhooks (Jenkins):
├── Verificar webhook: Settings → Webhooks → View requests
├── Ver logs de requests
├── Verificar que URL de Jenkins es accesible
└── En Jenkins: instalar plugin "Bitbucket Build Status Notifier"

Para Bitbucket Pipelines:
├── Verificar bitbucket-pipelines.yml en raíz del repo
├── Ver logs en: Pipelines tab del repo
├── Verificar que image tiene herramientas necesarias
└── Repository Settings → Pipelines → Enable ✅
```

---

##### **Problema 2: Quality Gates Pasan Local pero Fallan en Bitbucket**

```yaml
Síntoma:
✅ Local: ./scripts/quality-gates.sh → Success
❌ Bitbucket Pipelines: Quality Gates → Failed

Causas Posibles:
1. Diferencias de entorno (Java version, Gradle version)
2. Dependencias no cacheadas en Bitbucket
3. Permisos de archivos (scripts no ejecutables)
4. Variables de entorno faltantes

Solución:

Alinear Entornos:
# En bitbucket-pipelines.yml
image: gradle:8.5-jdk21  # Misma versión que local

# Verificar versiones locales:
java -version    # Debe coincidir con image
gradle -version  # Debe coincidir con image

Permisos:
# En bitbucket-pipelines.yml
script:
  - chmod +x scripts/*.sh  # Asegurar permisos
  - ./scripts/quality-gates.sh

Cache de Dependencias:
# En bitbucket-pipelines.yml
caches:
  - gradle   # Cachear ~/.gradle
  - maven    # Si usas Maven también

Variables:
# Repository Settings → Repository variables
MIN_COVERAGE: 80
JAVA_OPTS: -Xmx1024m
```

---

##### **Problema 3: Merge Bloqueado Aunque Quality Gates Pasaron**

```yaml
Síntoma:
✅ Build status: Success (Quality Gates passed)
✅ 2 approvals received
❌ "Merge" button disabled

Causas Posibles:
1. Tasks pendientes en PR
2. Merge conflicts
3. Branch protection rules mal configuradas
4. Approvals stale (nuevos commits después de approval)

Solución:

Verificar Tasks:
├── Ir a PR → Tasks tab
├── Resolver todos los tasks marcados
└── Re-request approval si es necesario

Verificar Merge Conflicts:
├── PR muestra "Conflicts detected"
├── Developer debe rebase/merge develop en feature branch
└── Push resuelve conflicts

Verificar Branch Permissions:
├── Settings → Branch permissions → main
├── Verificar que "Minimum approvals" coincide con cantidad
└── Verificar que "Dismiss stale approvals" no está bloqueando
```

---

##### **Problema 4: Bitbucket Pipelines Muy Lento**

```yaml
Síntoma:
⏱️ Quality Gates localmente: 2 minutos
⏱️ Quality Gates en Bitbucket: 10+ minutos

Causas:
1. No hay cache de dependencias
2. Re-descarga todo cada vez
3. Ejecuta tests innecesarios

Solución:

Optimizar Caches:
definitions:
  caches:
    gradle: ~/.gradle
    node: node_modules

pipelines:
  pull-requests:
    '**':
      - step:
          caches:
            - gradle
            - node

Paralelizar Steps:
pipelines:
  pull-requests:
    '**':
      - parallel:  # Ejecutar en paralelo
          - step:
              name: Quality Gates
              script: ./scripts/quality-gates.sh
          
          - step:
              name: Unit Tests
              script: ./gradlew test

Ejecutar Solo Lo Necesario:
# En scripts/quality-gates.sh
# Detectar qué módulos cambiaron
CHANGED_FILES=$(git diff --name-only origin/develop)

if [[ $CHANGED_FILES == *"common/"* ]]; then
    ./gradlew :common:test
fi

if [[ $CHANGED_FILES == *"api-core/"* ]]; then
    ./gradlew :api-core:test
fi
```

---

#### **D. Checklist de Configuración Bitbucket Completa**

```yaml
✅ CONFIGURACIONES OBLIGATORIAS (Framework):

Repository Settings:
├── ✅ Branch permissions configuradas (main protected)
├── ✅ Default reviewers configurados (por patrón de archivos)
├── ✅ Merge checks habilitados (approvals + builds)
├── ✅ PR template creado (.bitbucket/pull_request_template.md)
├── ✅ Webhooks configurados (Jenkins + Slack)
├── ✅ Repository variables configuradas (MIN_COVERAGE, etc.)
├── ✅ Bitbucket Pipelines habilitado
└── ✅ Access keys agregadas (Jenkins SSH key)

Archivos del Repo:
├── ✅ bitbucket-pipelines.yml creado y testeado
├── ✅ .bitbucket/pull_request_template.md creado
├── ✅ scripts/quality-gates.sh ejecutable y funcional
└── ✅ README.md con instrucciones de PR workflow

Validación:
├── ✅ Crear PR de prueba → verificar que Quality Gates se ejecutan
├── ✅ Intentar merge sin approvals → debe bloquear
├── ✅ Intentar merge con Quality Gates failed → debe bloquear
└── ✅ Merge exitoso → debe auto-delete branch
```

---

#### **E. 🎯 IMPLEMENTACIÓN REAL: ESCENARIO CON 3 REPOSITORIOS**

> **Tu Escenario Real**:
> - `qa-scotia-frameworks` (Framework: common, api-core, mobile-core, web-core)
> - `qa-module-autos` (Módulo consumidor 1)
> - `qa-module-mobile` (Módulo consumidor 2)

---

##### **📊 ESTRATEGIA RECOMENDADA: QUALITY GATES OBLIGATORIOS EN FRAMEWORK + OPCIONALES EN MÓDULOS**

```yaml
🎯 DECISIÓN ARQUITECTÓNICA:

✅ OBLIGATORIO en qa-scotia-frameworks:
├── Quality Gates completos y exhaustivos
├── Ejecutados SIEMPRE (Bitbucket Pipelines + Jenkins)
├── Bloqueo de merge si fallan
└── Responsabilidad: QA Core Team

⚠️ OPCIONAL pero RECOMENDADO en qa-module-autos y qa-module-mobile:
├── Quality Gates ligeros (solo validaciones del módulo)
├── Ejecutados en Bitbucket Pipelines (automático)
├── No bloquean merge (solo warning) ← CLAVE
└── Responsabilidad: Equipo del módulo

❌ SIN Quality Gates en módulos = RIESGO MANEJABLE:
├── Framework sigue siendo robusto
├── Módulos pueden tener código de menor calidad
├── No afecta al framework
└── Riesgo aceptable si módulos son independientes
```

---

##### **🗂️ UBICACIÓN DEL .sh EN CADA REPOSITORIO**

###### **REPOSITORIO 1: qa-scotia-frameworks (OBLIGATORIO)**

```
qa-scotia-frameworks/
├── scripts/
│   ├── quality-gates.sh                    ✅ OBLIGATORIO - Quality Gates completo
│   ├── quality-gates-module.sh             ✅ OBLIGATORIO - Template para módulos
│   ├── download-quality-gates.sh           ✅ OBLIGATORIO - Helper para módulos
│   ├── semantic-versioning.sh              ✅ OBLIGATORIO - Versionado automático
│   └── security-scan.sh                    ✅ OBLIGATORIO - Security scan
│
├── .bitbucket/
│   └── pull_request_template.md            ✅ OBLIGATORIO - Template de PR
│
├── bitbucket-pipelines.yml                 ✅ OBLIGATORIO - CI/CD automático
│
└── .git/hooks/
    ├── pre-push                            ⚠️ OPCIONAL - Validación local
    └── commit-msg                          ⚠️ OPCIONAL - Validar commit messages

Características:
✅ Quality Gates se ejecutan SIEMPRE en PR
✅ Bitbucket bloquea merge si fallan
✅ Jenkins ejecuta validaciones adicionales
✅ Deploy automático a Artifactory si pasa
```

---

###### **REPOSITORIO 2: qa-module-autos (RECOMENDADO)**

```
qa-module-autos/
├── scripts/
│   ├── download-quality-gates.sh           ✅ RECOMENDADO - Descarga script del framework
│   └── quality-gates.sh                    ⚠️ AUTO-DESCARGADO - No commitear manualmente
│
├── .bitbucket/
│   └── pull_request_template.md            ✅ RECOMENDADO - Template simplificado
│
├── bitbucket-pipelines.yml                 ✅ RECOMENDADO - CI/CD con Quality Gates
│
└── .git/hooks/
    └── pre-push                            ⚠️ OPCIONAL - Validación local

Características:
✅ Descarga quality-gates.sh automáticamente desde framework
✅ Bitbucket Pipelines ejecuta validaciones automáticamente
⚠️ No bloquea merge (solo warning) ← CLAVE DIFERENCIA
✅ Developer puede ignorar warnings (bajo su responsabilidad)
```

---

###### **REPOSITORIO 3: qa-module-mobile (RECOMENDADO)**

```
qa-module-mobile/
├── scripts/
│   ├── download-quality-gates.sh           ✅ RECOMENDADO - Igual que module-autos
│   └── quality-gates.sh                    ⚠️ AUTO-DESCARGADO
│
├── .bitbucket/
│   └── pull_request_template.md            ✅ RECOMENDADO
│
├── bitbucket-pipelines.yml                 ✅ RECOMENDADO
│
└── .git/hooks/
    └── pre-push                            ⚠️ OPCIONAL

Características:
✅ Misma estrategia que qa-module-autos
✅ Validaciones automáticas en Bitbucket
⚠️ No bloquea merge (solo warning)
```

---

##### **⏱️ CUÁNDO SE EJECUTA QUALITY GATES (CON Y SIN JENKINS)**

###### **ESCENARIO A: CON JENKINS (Recomendado para Producción)** ⭐

```yaml
📦 FRAMEWORK (qa-scotia-frameworks):

┌─────────────────────────────────────────────────────────────┐
│ 1. Developer: Push a feature branch                        │
│    ├── git push origin feature/QAAUY-123                   │
│    └── No ejecuta nada aún                                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Developer: Crea Pull Request en Bitbucket               │
│    ├── De: feature/QAAUY-123 → develop                     │
│    └── Bitbucket aplica PR template                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. Bitbucket: Webhook → Jenkins                            │
│    ├── Webhook trigger: PR created/updated                 │
│    ├── Jenkins recibe notificación                         │
│    └── Jenkins inicia Job: "QA-Framework-PR-Validation"    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. Jenkins: Ejecuta Quality Gates                          │
│    ├── Clona repo: git clone <feature-branch>              │
│    ├── Ejecuta: ./scripts/quality-gates.sh                 │
│    ├── Tiempo: 3-5 minutos                                 │
│    └── Resultado: ✅ Success / ❌ Failed                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. Jenkins: Publica Build Status a Bitbucket               │
│    ├── POST /rest/build-status/1.0/commits/<commit-sha>    │
│    ├── Status: SUCCESS / FAILED                            │
│    └── Bitbucket muestra status en PR UI                   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. Bitbucket: Evalúa Merge Checks                          │
│    ├── Build status: ✅ SUCCESS (de Jenkins)               │
│    ├── Approvals: ✅ 2/2                                   │
│    ├── Tasks: ✅ All resolved                              │
│    └── Resultado: Merge button ENABLED ✅                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 7. Developer: Hace Merge                                   │
│    ├── Click "Merge" button en Bitbucket                   │
│    ├── Branch merged to develop                            │
│    └── Feature branch auto-deleted                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 8. (Si merge a main) Jenkins: Deploy a Artifactory         │
│    ├── Webhook trigger: Push to main                       │
│    ├── Jenkins Job: "Deploy-Framework-to-Artifactory"      │
│    ├── Ejecuta: semantic-versioning.sh → v1.2.4            │
│    ├── Ejecuta: ./gradlew :common:publish                  │
│    └── Publica: com.scotia.qa:common:1.2.4 a Artifactory   │
└─────────────────────────────────────────────────────────────┘

---

📱 MÓDULOS (qa-module-autos, qa-module-mobile):

┌─────────────────────────────────────────────────────────────┐
│ 1. Developer: Crea Pull Request                            │
│    └── Bitbucket aplica PR template                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Bitbucket: Webhook → Jenkins (Opcional)                 │
│    ├── ⚠️ Puede no tener Jenkins configurado               │
│    └── ✅ O tiene Jenkins con validaciones ligeras         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. Jenkins/Bitbucket Pipelines: Quality Gates Ligeros      │
│    ├── Descarga: ./scripts/download-quality-gates.sh       │
│    ├── Ejecuta: ./scripts/quality-gates.sh                 │
│    ├── Validaciones: Solo compilación + tests básicos      │
│    └── Resultado: ⚠️ Warning (no bloquea merge)            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. Developer: Puede hacer Merge AUNQUE Quality Gates falle │
│    ├── Bitbucket NO bloquea merge                          │
│    ├── Solo muestra warning visible                        │
│    └── Developer decide bajo su responsabilidad            │
└─────────────────────────────────────────────────────────────┘
```

---

###### **ESCENARIO B: SIN JENKINS (Solo Bitbucket Pipelines)** ⚡

```yaml
📦 FRAMEWORK (qa-scotia-frameworks):

┌─────────────────────────────────────────────────────────────┐
│ 1. Developer: Crea Pull Request                            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Bitbucket Pipelines: Auto-trigger                       │
│    ├── Detecta bitbucket-pipelines.yml                     │
│    ├── Ejecuta: ./scripts/quality-gates.sh                 │
│    ├── Tiempo: 3-5 minutos                                 │
│    └── Resultado: ✅ Success / ❌ Failed                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. Bitbucket: Evalúa Merge Checks                          │
│    ├── Pipeline status: ✅ SUCCESS                         │
│    ├── Approvals: ✅ 2/2                                   │
│    └── Merge button: ENABLED ✅                            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. Developer: Hace Merge                                   │
└─────────────────────────────────────────────────────────────┘

VENTAJA: No requiere Jenkins Server
DESVENTAJA: Bitbucket Pipelines tiene límites de minutos mensuales
```

---

📱 MÓDULOS (Sin Jenkins):

```yaml
┌─────────────────────────────────────────────────────────────┐
│ 1. Developer: Crea PR → Bitbucket Pipelines se ejecuta     │
│    └── Validaciones ligeras automáticas                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Resultado: ⚠️ Warning (no bloquea)                      │
│    └── Developer puede hacer merge igual                   │
└─────────────────────────────────────────────────────────────┘
```

---

##### **🔍 ¿CADA REPOSITORIO TIENE QUE CONTENER EL .sh?**

```yaml
📦 FRAMEWORK (qa-scotia-frameworks):
✅ SÍ - Contiene scripts/quality-gates.sh (código fuente)
✅ SÍ - Contiene scripts/quality-gates-module.sh (para módulos)
└── Es el ÚNICO lugar donde se mantiene el código de Quality Gates

📱 MÓDULOS (qa-module-autos, qa-module-mobile):
❌ NO - NO contienen quality-gates.sh commiteado
✅ SÍ - Contienen scripts/download-quality-gates.sh (descargador)
✅ SÍ - .gitignore debe ignorar: scripts/quality-gates.sh
└── El script se DESCARGA en tiempo de ejecución desde framework

Flujo de descarga en módulos:

# En bitbucket-pipelines.yml del módulo:
script:
  # 1. Descargar última versión del quality gates desde framework
  - chmod +x scripts/download-quality-gates.sh
  - ./scripts/download-quality-gates.sh
  
  # 2. Ejecutar quality gates descargado
  - chmod +x scripts/quality-gates.sh
  - ./scripts/quality-gates.sh
```

**Ventajas de este enfoque**:
- ✅ **Un solo lugar de mantenimiento** (framework)
- ✅ **Módulos siempre usan versión actualizada**
- ✅ **No hay duplicación de código**
- ✅ **Actualizaciones automáticas**

---

##### **⚠️ PROBLEMA: ¿QUÉ PASA SI DEVELOPERS NO EJECUTAN QUALITY GATES?**

###### **RIESGO REAL:**
```yaml
Escenario sin Bitbucket Pipelines ni Jenkins:

Developer malicioso/descuidado:
├── Omite ejecutar ./scripts/quality-gates.sh localmente
├── Push directo a develop (si no hay branch protection)
├── O crea PR y hace merge sin esperar validaciones
└── Código de mala calidad entra al repo

IMPACTO:
❌ Código sin tests
❌ Breaking changes no detectados
❌ Security issues no encontrados
❌ Cobertura de código baja
```

---

###### **SOLUCIÓN: BITBUCKET ENFORCED VALIDATION (OBLIGATORIO)** ⭐

```yaml
Configuración OBLIGATORIA en Bitbucket:

📦 FRAMEWORK (qa-scotia-frameworks):

1. Branch Permissions (Settings → Branch permissions):
   ├── Branch: main
   ├── Prevent changes without a pull request: ✅ ENABLED
   ├── Require successful builds: ✅ ENABLED
   │   └── Build: "Quality Gates - Framework"
   ├── Minimum approvals: 2
   └── Only allow users to push: qa-lead, devops-admin

2. Merge Checks (Settings → Merge checks):
   ├── Minimum successful builds: 1
   │   └── Build must match: "Quality Gates*"
   ├── Minimum approvals: 2
   └── All tasks must be resolved: ✅

Resultado:
✅ Bitbucket BLOQUEA merge si Quality Gates no se ejecutan
✅ Bitbucket BLOQUEA merge si Quality Gates fallan
✅ Developer NO PUEDE bypasear validaciones
✅ Única excepción: qa-lead puede emergency push (raro)

---

📱 MÓDULOS (qa-module-autos, qa-module-mobile):

OPCIÓN A: Enforcement ESTRICTO (Recomendado si módulos son críticos):

1. Branch Permissions:
   ├── Prevent changes without PR: ✅ ENABLED
   ├── Require successful builds: ✅ ENABLED
   │   └── Build: "Quality Gates - Module"
   └── Minimum approvals: 1

Resultado:
✅ Módulos DEBEN pasar Quality Gates (igual que framework)
⚠️ Puede ralentizar developers si validaciones son muy estrictas

---

OPCIÓN B: Enforcement SOFT (Recomendado para módulos menos críticos):

1. Branch Permissions:
   ├── Prevent changes without PR: ✅ ENABLED
   ├── Require successful builds: ❌ DISABLED  ← CLAVE
   └── Minimum approvals: 1

2. Bitbucket Pipelines SIEMPRE se ejecuta:
   ├── Muestra warnings visibles en PR
   ├── Genera reporte de calidad
   ├── Pero NO bloquea merge
   └── Developer decide bajo su responsabilidad

Resultado:
⚠️ Módulos pueden hacer merge aunque Quality Gates fallen
✅ Al menos hay visibilidad de la calidad del código
✅ Reportes se envían a Slack/Teams para monitoreo
✅ QA Lead puede revisar reportes semanalmente

---

OPCIÓN C: Sin Quality Gates en Módulos (NO RECOMENDADO):

Riesgos:
❌ Cero visibilidad de calidad de código en módulos
❌ Tests pueden estar rotos sin que nadie sepa
❌ Security issues no detectados
⚠️ Framework sigue protegido, pero módulos son "wild west"

Cuándo aceptable:
├── Módulos son temporales/experimentales
├── Equipo muy pequeño (1-2 developers) con disciplina
└── Módulos no van a producción (solo POCs)
```

---

##### **🎯 CONSECUENCIAS DE NO IMPLEMENTAR QUALITY GATES EN MÓDULOS**

###### **SI SOLO IMPLEMENTAS EN FRAMEWORK (qa-scotia-frameworks):**

```yaml
✅ VENTAJAS:

1. Framework Protegido:
   ├── common, api-core, mobile-core, web-core siempre robustos
   ├── Publicación a Artifactory solo si pasa validaciones
   └── Módulos consumen framework de calidad garantizada

2. Menos Overhead:
   ├── Solo 1 repo con Quality Gates complejos
   ├── Menos mantenimiento de scripts
   └── Menos carga en CI/CD (Bitbucket Pipelines/Jenkins)

3. Autonomía de Módulos:
   ├── Equipos de módulos trabajan más rápido
   ├── No se bloquean esperando validaciones
   └── Pueden iterar rápidamente

---

❌ DESVENTAJAS:

1. Calidad Inconsistente en Módulos:
   ├── qa-module-autos puede tener tests rotos
   ├── qa-module-mobile puede tener código duplicado
   ├── Security issues en módulos no detectados
   └── Cobertura de tests baja o nula

2. Debugging Difícil:
   ├── Si módulo falla en producción → ¿culpa del framework o del módulo?
   ├── Sin validaciones automáticas → tiempo perdido debuggeando
   └── Responsabilidad poco clara

3. Riesgo de Producción:
   ├── Módulos pueden llegar a producción con bugs
   ├── Features rotas que afectan usuarios finales
   └── Rollbacks más frecuentes

4. Falta de Métricas:
   ├── No hay visibilidad de calidad de código en módulos
   ├── Imposible medir mejora/deterioro de calidad
   └── No hay datos para tomar decisiones

---

⚖️ DECISIÓN RECOMENDADA:

IMPLEMENTAR Quality Gates en AMBOS, pero con diferentes estrategias:

📦 FRAMEWORK (qa-scotia-frameworks):
├── Quality Gates: COMPLETOS Y EXHAUSTIVOS
├── Enforcement: ESTRICTO (bloquea merge si falla)
├── Validaciones: 10-15 checks (compilation, tests, coverage, security, etc.)
├── Tiempo ejecución: 3-5 minutos
└── Responsabilidad: QA Core Team

📱 MÓDULOS (qa-module-autos, qa-module-mobile):
├── Quality Gates: LIGEROS Y RÁPIDOS
├── Enforcement: SOFT (warning pero no bloquea merge)
├── Validaciones: 3-5 checks básicos (compilation, basic tests, security scan)
├── Tiempo ejecución: 30 segundos - 1 minuto
└── Responsabilidad: Equipo del módulo

Beneficios:
✅ Framework siempre protegido (crítico)
✅ Módulos tienen VISIBILIDAD de calidad (importante)
✅ Módulos no se bloquean (velocidad)
✅ Reportes semanales de calidad para QA Lead
✅ Cultura de calidad se promueve gradualmente
```

---

##### **📊 CUADRO COMPARATIVO: CON VS SIN QUALITY GATES EN MÓDULOS**

| Aspecto | CON QG en Módulos | SIN QG en Módulos |
|---------|-------------------|-------------------|
| **Calidad Framework** | ✅ Alta | ✅ Alta (no afectada) |
| **Calidad Módulos** | ✅ Alta/Media | ❌ Baja/Variable |
| **Velocidad Desarrollo** | ⚠️ Puede ralentizar | ✅ Rápida |
| **Visibilidad Problemas** | ✅ Inmediata | ❌ Solo en runtime |
| **Debugging** | ✅ Fácil | ❌ Difícil |
| **Security** | ✅ Protegida | ⚠️ Riesgo medio |
| **Cobertura Tests** | ✅ Monitoreada | ❌ Desconocida |
| **Esfuerzo Setup** | ⚠️ Medio (3 repos) | ✅ Bajo (1 repo) |
| **Esfuerzo Mantenimiento** | ⚠️ Medio | ✅ Bajo |
| **Cultura de Calidad** | ✅ Se promueve | ⚠️ Se degrada |
| **Riesgo Producción** | ✅ Bajo | ⚠️ Medio-Alto |

---

##### **💡 RECOMENDACIÓN FINAL PARA TU ESCENARIO**

```yaml
🎯 ESTRATEGIA PRAGMÁTICA (BALANCE CALIDAD/VELOCIDAD):

FASE 1 (INMEDIATO - Esta semana):
✅ Implementar Quality Gates COMPLETOS en qa-scotia-frameworks
├── Enforcement: ESTRICTO
├── Bitbucket bloquea merge si falla
└── Tiempo: 2-3 horas de configuración

FASE 2 (CORTO PLAZO - 2-3 semanas):
⚠️ Implementar Quality Gates LIGEROS en qa-module-autos y qa-module-mobile
├── Enforcement: SOFT (warning only)
├── Bitbucket NO bloquea merge
├── Reportes semanales a QA Lead
└── Tiempo: 1-2 horas por módulo

FASE 3 (MEDIANO PLAZO - 1-2 meses):
📊 Revisar métricas y decidir:
├── Si módulos tienen muchos issues → aumentar enforcement
├── Si módulos son estables → mantener soft enforcement
└── Decisión basada en datos, no suposiciones

FASE 4 (FUTURO - 3-6 meses):
🔄 Cultura de calidad establecida:
├── Todos los repos con Quality Gates
├── Enforcement gradualmente más estricto
├── Developers acostumbrados y adoptan voluntariamente
└── Quality Gates es "parte del proceso"
```

---

##### **🚀 SIGUIENTE PASO CONCRETO PARA TI**

```bash
# Comenzar HOY con Framework:

1. cd qa-scotia-frameworks
2. Crear scripts/quality-gates.sh (2-3 horas)
3. Crear bitbucket-pipelines.yml (30 minutos)
4. Configurar Branch Permissions en Bitbucket (15 minutos)
5. Configurar Merge Checks en Bitbucket (10 minutos)
6. Crear PR de prueba para validar (30 minutos)

Total: 4-5 horas de trabajo → FRAMEWORK PROTEGIDO ✅

Módulos: Evaluar después de ver resultados en framework.
```

---

#### **F. 📋 RESUMEN EJECUTIVO: RESPUESTAS DIRECTAS**

##### **❓ Pregunta 1: ¿Cómo quedaría con 3 repos (framework + 2 módulos)?**

```yaml
qa-scotia-frameworks/ (FRAMEWORK - Priority: CRÍTICO):
├── scripts/quality-gates.sh               ✅ Código fuente (completo)
├── scripts/quality-gates-module.sh        ✅ Template para módulos
├── bitbucket-pipelines.yml                ✅ CI/CD automático
├── Bitbucket Config: Enforcement ESTRICTO ✅ Bloquea merge si falla
└── Responsable: QA Core Team

qa-module-autos/ (MÓDULO 1 - Priority: MEDIA):
├── scripts/download-quality-gates.sh      ✅ Descargador
├── scripts/quality-gates.sh               ⚠️ Auto-descargado (no commitear)
├── bitbucket-pipelines.yml                ✅ CI/CD automático
├── Bitbucket Config: Enforcement SOFT     ⚠️ Warning (no bloquea merge)
└── Responsable: Equipo Module Autos

qa-module-mobile/ (MÓDULO 2 - Priority: MEDIA):
├── scripts/download-quality-gates.sh      ✅ Descargador
├── scripts/quality-gates.sh               ⚠️ Auto-descargado (no commitear)
├── bitbucket-pipelines.yml                ✅ CI/CD automático
├── Bitbucket Config: Enforcement SOFT     ⚠️ Warning (no bloquea merge)
└── Responsable: Equipo Module Mobile
```

**Respuesta Corta**: Cada repo tiene su estructura, pero SOLO framework contiene código fuente del .sh. Módulos lo descargan automáticamente.

---

##### **❓ Pregunta 2: ¿Dónde se aloja el .sh?**

```yaml
CÓDIGO FUENTE (Único lugar de mantenimiento):
📦 qa-scotia-frameworks/scripts/quality-gates.sh

DESCARGA AUTOMÁTICA (No se commitea):
📱 qa-module-autos/scripts/quality-gates.sh        (descargado en runtime)
📱 qa-module-mobile/scripts/quality-gates.sh       (descargado en runtime)

Estrategia:
1. Framework publica quality-gates-module.sh a ubicación accesible:
   ├── Opción A: GitHub raw URL
   ├── Opción B: Artifactory
   └── Opción C: Bitbucket raw URL

2. Módulos descargan en Bitbucket Pipelines:
   script:
     - curl -o scripts/quality-gates.sh https://bitbucket.org/.../quality-gates-module.sh
     - chmod +x scripts/quality-gates.sh
     - ./scripts/quality-gates.sh
```

**Respuesta Corta**: Framework aloja el código fuente. Módulos lo descargan automáticamente.

---

##### **❓ Pregunta 3: ¿En qué momento del flujo se ejecuta (con y sin Jenkins)?**

###### **CON JENKINS:**

```yaml
📦 FRAMEWORK:
1. Developer crea PR → 2. Webhook a Jenkins → 3. Jenkins ejecuta Quality Gates
→ 4. Jenkins publica status a Bitbucket → 5. Bitbucket bloquea/permite merge

Tiempo total: 3-5 minutos desde crear PR hasta ver resultado

📱 MÓDULOS:
1. Developer crea PR → 2. Webhook a Jenkins (o Bitbucket Pipelines)
→ 3. Ejecuta Quality Gates ligeros → 4. Muestra warning (no bloquea)

Tiempo total: 30 seg - 1 min
```

###### **SIN JENKINS (Solo Bitbucket Pipelines):**

```yaml
📦 FRAMEWORK:
1. Developer crea PR → 2. Bitbucket Pipelines auto-trigger
→ 3. Ejecuta Quality Gates → 4. Bitbucket bloquea/permite merge

Tiempo total: 3-5 minutos

📱 MÓDULOS:
1. Developer crea PR → 2. Bitbucket Pipelines auto-trigger
→ 3. Ejecuta Quality Gates → 4. Muestra warning (no bloquea)

Tiempo total: 30 seg - 1 min
```

**Respuesta Corta**: Se ejecuta automáticamente al crear/actualizar PR. Con Jenkins: más potente. Sin Jenkins: más simple.

---

##### **❓ Pregunta 4: ¿Cada repositorio tiene que contener el fichero?**

```yaml
📦 FRAMEWORK:
✅ SÍ - Contiene el fichero (es el dueño)

📱 MÓDULOS:
❌ NO - NO contienen el fichero commiteado
✅ SÍ - Contienen el script que lo descarga
✅ SÍ - .gitignore debe ignorar: scripts/quality-gates.sh

# .gitignore en módulos:
scripts/quality-gates.sh
```

**Respuesta Corta**: NO. Módulos lo descargan automáticamente, no lo commitean.

---

##### **❓ Pregunta 5: ¿Qué pasa si developers no ejecutan el .sh?**

```yaml
⚠️ RIESGO SIN ENFORCEMENT:

Escenario:
Developer omite ejecutar ./scripts/quality-gates.sh localmente
└── Push código sin validar
    └── Crea PR
        └── Hace merge sin esperar validaciones
            └── ❌ Código de mala calidad entra al repo

---

✅ SOLUCIÓN: ENFORCEMENT EN BITBUCKET (OBLIGATORIO)

Configuración en Bitbucket:
Repository Settings → Branch permissions → main:
├── Prevent changes without PR: ✅ ENABLED
├── Require successful builds: ✅ ENABLED
│   └── Build: "Quality Gates*"
└── Minimum approvals: 2

Resultado:
✅ Developer NO PUEDE bypasear validaciones
✅ Bitbucket BLOQUEA merge si Quality Gates no se ejecutan
✅ Bitbucket BLOQUEA merge si Quality Gates fallan
✅ Developer DEBE esperar a que pasen las validaciones

---

📱 Para MÓDULOS (si quieres enforcement soft):

Configuración:
├── Require successful builds: ❌ DISABLED
└── Bitbucket Pipelines SIEMPRE ejecuta quality gates automáticamente

Resultado:
⚠️ Módulos pueden hacer merge aunque fallen (bajo su responsabilidad)
✅ Al menos hay visibilidad y reportes
✅ QA Lead puede monitorear semanalmente
```

**Respuesta Corta**: Configurar Bitbucket para FORZAR ejecución. Developer no puede bypasear.

---

##### **❓ Pregunta 6: ¿Qué pasa si NO implementamos en módulos?**

###### **CONSECUENCIAS REALES:**

```yaml
✅ FRAMEWORK SIGUE PROTEGIDO:
├── common, api-core, mobile-core, web-core → calidad garantizada
├── Publicación a Artifactory solo si pasa validaciones
└── Módulos consumen framework robusto

❌ MÓDULOS SIN PROTECCIÓN:
├── qa-module-autos → puede tener tests rotos
├── qa-module-mobile → puede tener código duplicado
├── Security issues no detectados
├── Cobertura de tests desconocida
└── Calidad inconsistente

⚠️ RIESGO MANEJABLE SI:
├── Módulos son temporales/experimentales
├── Equipo muy pequeño con disciplina
├── Módulos no van a producción crítica
└── Framework es lo realmente crítico (true in your case)

❌ RIESGO ALTO SI:
├── Módulos van a producción
├── Múltiples developers trabajando
├── Módulos críticos para negocio
└── Sin code review riguroso
```

###### **COMPARATIVA RÁPIDA:**

| Aspecto | Con QG en Módulos | Sin QG en Módulos |
|---------|-------------------|-------------------|
| **Calidad Framework** | ✅ | ✅ |
| **Calidad Módulos** | ✅ | ❌ |
| **Velocidad Dev** | ⚠️ | ✅ |
| **Debugging** | ✅ | ❌ |
| **Security** | ✅ | ⚠️ |
| **Setup Effort** | ⚠️ | ✅ |
| **Riesgo Prod** | ✅ | ⚠️ |

**Respuesta Corta**: Framework sigue protegido, pero módulos pueden degradarse. Riesgo manejable si módulos no son críticos.

---

##### **💡 RECOMENDACIÓN FINAL ESPECÍFICA PARA TI:**

```yaml
🎯 PLAN DE ACCIÓN PRAGMÁTICO:

SEMANA 1 (EMPEZAR HOY):
✅ Implementar Quality Gates COMPLETO en qa-scotia-frameworks
├── Enforcement: ESTRICTO (bloquea merge)
├── Tiempo: 4-5 horas
└── Resultado: Framework 100% protegido

SEMANA 2-3 (EVALUAR NECESIDAD):
⚠️ Implementar Quality Gates LIGERO en módulos
├── Enforcement: SOFT (warning only)
├── Tiempo: 1-2 horas por módulo
└── Resultado: Visibilidad de calidad, no bloqueo

DECISIÓN BASADA EN:
├── ¿Módulos van a producción? → SÍ = implementar QG
├── ¿Módulos son críticos? → SÍ = implementar QG
├── ¿Múltiples developers? → SÍ = implementar QG
├── ¿Módulos temporales? → NO = skip QG por ahora
└── ¿Framework es lo crítico? → SÍ = enfocarse en framework primero

---

🚀 SIGUIENTE PASO INMEDIATO:

cd qa-scotia-frameworks
# Crear Quality Gates completo (siguiente sección del doc)
```

---

## **2. 🎯 FEATURE FLAGS SYSTEM** ⭐ MÁXIMO IMPACTO

### **¿Por qué es CRÍTICO?**
- **Despliegue controlado** de nuevas features sin redeploy
- **A/B testing** entre implementaciones (legacy vs nueva)
- **Rollback instantáneo** cambiando solo un flag
- **Gradual rollout** a equipos específicos (por usuario, equipo, %)
- **Zero downtime** para cambios de comportamiento

### **📋 Casos de Uso Reales en Framework**
```yaml
Escenarios Prácticos:
✅ Habilitar nuevo sistema de validación solo para QA Core Team
✅ Probar nuevo HttpClient con 10% de usuarios aleatorios
✅ Activar métricas avanzadas solo en módulos específicos
✅ Rollback inmediato si nueva feature causa problemas
✅ A/B testing entre algoritmos de validación
```

### **Arquitectura de Implementación:**
```
common/
├── interfaces/
│   └── FeatureFlagService.java          (Contrato)
├── implementations/
│   └── BaseFeatureFlagService.java      (Implementación)
├── factories/
│   └── FeatureFlagServiceFactory.java   (Factory)
└── resources/
    └── feature-flags.yml                (Configuración)
```

### **Implementación en Framework Common:**
```java
// Agregar a common/src/main/java/com/scotia/qa/common/features/
@Component
public class FeatureFlagManager {
    
    private final Map<String, FeatureFlag> flags = new ConcurrentHashMap<>();
    private final ConfigurationService configService;
    
    public FeatureFlagManager(ConfigurationService configService) {
        this.configService = configService;
        loadFeatureFlags();
    }
    
    public boolean isEnabled(String featureName) {
        return isEnabled(featureName, getCurrentUser());
    }
    
    public boolean isEnabled(String featureName, String userId) {
        FeatureFlag flag = flags.get(featureName);
        if (flag == null) {
            TestLogger.warn("Feature flag not found: " + featureName);
            return false;
        }
        
        return flag.isEnabledFor(userId);
    }
    
    private void loadFeatureFlags() {
        try {
            // Cargar desde archivo de configuración
            Optional<Map<String, Object>> flagsConfig = 
                configService.readYaml("feature-flags.yml");
            
            if (flagsConfig.isPresent()) {
                flagsConfig.get().forEach((name, config) -> {
                    flags.put(name, FeatureFlag.fromConfig(name, config));
                });
            }
            
            TestLogger.info("Loaded {} feature flags", flags.size());
            
        } catch (Exception e) {
            TestLogger.error("Failed to load feature flags", e);
        }
    }
    
    public void refreshFlags() {
        flags.clear();
        loadFeatureFlags();
    }
}

// Feature Flag entity
public class FeatureFlag {
    private final String name;
    private final boolean defaultEnabled;
    private final Set<String> enabledUsers;
    private final Set<String> enabledTeams;
    private final double rolloutPercentage;
    
    public boolean isEnabledFor(String userId) {
        // 1. Check explicit user enable
        if (enabledUsers.contains(userId)) {
            return true;
        }
        
        // 2. Check team membership
        String userTeam = getUserTeam(userId);
        if (enabledTeams.contains(userTeam)) {
            return true;
        }
        
        // 3. Check rollout percentage
        if (rolloutPercentage > 0) {
            int hash = Math.abs(userId.hashCode());
            double userPercentile = (hash % 100) / 100.0;
            return userPercentile < rolloutPercentage;
        }
        
        return defaultEnabled;
    }
}
```

#### **Configuración de Feature Flags:**
```yaml
# common/src/main/resources/feature-flags.yml
features:
  NEW_VALIDATION_SYSTEM:
    enabled: false
    rollout_percentage: 0.1  # 10% de usuarios
    enabled_teams: ["qa-core-team"]
    enabled_users: ["abel.venero", "qa.lead"]
    
  ENHANCED_HTTP_CLIENT:
    enabled: true
    rollout_percentage: 0.5  # 50% rollout
    enabled_teams: ["api-team", "mobile-team"]
    
  ADVANCED_METRICS:
    enabled: false
    rollout_percentage: 0.0
    enabled_teams: ["qa-core-team"]
    
  PERFORMANCE_MONITORING:
    enabled: true
    rollout_percentage: 1.0  # 100% enabled
```

#### **Uso en Framework:**
```java
// En BaseHttpClient
public class BaseHttpClient implements HttpClient {
    
    private final FeatureFlagManager featureFlagManager;
    
    @Override
    public HttpResponse executeRequest(String method, String endpoint) {
        
        if (featureFlagManager.isEnabled("ENHANCED_HTTP_CLIENT")) {
            return executeEnhancedRequest(method, endpoint);
        } else {
            return executeLegacyRequest(method, endpoint);
        }
    }
    
    private HttpResponse executeEnhancedRequest(String method, String endpoint) {
        // Nueva implementación con mejoras
        TestLogger.info("Using enhanced HTTP client for: " + endpoint);
        // ... enhanced logic
    }
    
    private HttpResponse executeLegacyRequest(String method, String endpoint) {
        // Implementación actual estable
        // ... legacy logic
    }
}

// En ValidationService
public class BaseValidationService implements ValidationService {
    
    @Override
    public void validateJsonSchema(String jsonBody, String schemaPath) {
        
        if (featureFlagManager.isEnabled("NEW_VALIDATION_SYSTEM")) {
            validateWithNewSystem(jsonBody, schemaPath);
        } else {
            validateWithLegacySystem(jsonBody, schemaPath);
        }
    }
}
```

---

---

## **3. 🌐 CONTRACT TESTING (PACT)** ⭐ COMPATIBILIDAD FRAMEWORKS

### **¿Por qué es CRÍTICO?**
- **Evolución independiente** de frameworks sin romper compatibilidad
- **Detección temprana** de breaking changes entre api-core, mobile-core, web-core
- **Consumer-driven contracts** → quien consume define el contrato
- **Testing aislado** sin necesidad de servicios reales

### **📋 Problema que Resuelve**
```yaml
Escenario Actual (Sin Contract Testing):
❌ Mobile-core implementa cambio en HttpClient
❌ Api-core no se entera del cambio
❌ Módulos consumidores se rompen en runtime
❌ Debugging complejo y tardío

Escenario con Contract Testing:
✅ Mobile-core define contrato de su HttpClient
✅ Api-core valida que cumple el contrato
✅ Tests fallan si hay incompatibilidad
✅ Breaking changes detectados en CI antes de merge
```

### **Arquitectura de Contracts:**
```
common/
└── src/test/java/contracts/
    ├── ApiFrameworkContractTest.java      (API contracts)
    ├── MobileFrameworkContractTest.java   (Mobile contracts)
    ├── WebFrameworkContractTest.java      (Web contracts)
    └── CrossFrameworkContractTest.java    (Interoperabilidad)
```

### **Implementación con Pact:**
```xml
<!-- pom.xml o equivalente en build.gradle -->
<dependency>
    <groupId>au.com.dius.pact.consumer</groupId>
    <artifactId>junit5</artifactId>
    <version>4.3.15</version>
    <scope>test</scope>
</dependency>
```

```java
// Contract testing entre frameworks
// common/src/test/java/contracts/
@ExtendWith(PactConsumerTestExt.class)
public class ApiFrameworkContractTest {
    
    @Pact(consumer = "mobile-framework", provider = "scotia-api")
    public RequestResponsePact mobileUserProfile(PactDslWithProvider builder) {
        return builder
            .given("user exists with mobile preferences")
            .uponReceiving("get user profile for mobile")
            .path("/api/user/profile")
            .method("GET")
            .headers(Map.of("Framework-Type", "mobile"))
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(new PactDslJsonBody()
                .stringType("userId", "12345")
                .stringType("name", "Test User")
                .object("mobilePreferences")
                    .booleanType("pushNotifications", true)
                    .stringType("theme", "dark")
                    .closeObject()
            )
            .toPact();
    }
    
    @Test
    @PactTestFor(pactMethod = "mobileUserProfile")
    public void testMobileFrameworkContract(MockServer mockServer) {
        // Test usando HttpClientMobile
        HttpClient mobileClient = HttpClientFactory.getInstance("mobile");
        mobileClient.setHost(mockServer.getUrl());
        mobileClient.addHeader("Framework-Type", "mobile");
        
        HttpResponse response = mobileClient.executeRequest("GET", "/api/user/profile");
        
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getJsonField("userId")).isNotNull();
        assertThat(response.getJsonField("mobilePreferences.pushNotifications")).isNotNull();
    }
    
    @Pact(consumer = "web-framework", provider = "scotia-api")
    public RequestResponsePact webUserProfile(PactDslWithProvider builder) {
        return builder
            .given("user exists with web preferences")
            .uponReceiving("get user profile for web")
            .path("/api/user/profile")
            .method("GET")
            .headers(Map.of("Framework-Type", "web"))
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(new PactDslJsonBody()
                .stringType("userId", "12345")
                .stringType("name", "Test User")
                .object("webPreferences")
                    .stringType("language", "es")
                    .booleanType("cookies", true)
                    .closeObject()
            )
            .toPact();
    }
    
    @Test
    @PactTestFor(pactMethod = "webUserProfile")
    public void testWebFrameworkContract(MockServer mockServer) {
        // Test usando HttpClientWeb
        HttpClient webClient = HttpClientFactory.getInstance("web");
        webClient.setHost(mockServer.getUrl());
        webClient.addHeader("Framework-Type", "web");
        
        HttpResponse response = webClient.executeRequest("GET", "/api/user/profile");
        
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getJsonField("userId")).isNotNull();
        assertThat(response.getJsonField("webPreferences.language")).isNotNull();
    }
}
```

#### **Integration con Jenkins:**
```groovy
// Jenkinsfile para contract testing
pipeline {
    agent any
    
    stages {
        stage('Contract Tests') {
            steps {
                script {
                    // Run consumer contract tests
                    sh './gradlew contractTest'
                    
                    // Publish pacts to broker (or file system)
                    sh './gradlew pactPublish'
                    
                    // Verify provider contracts
                    sh './gradlew pactVerify'
                }
            }
            post {
                always {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'build/reports/pact',
                        reportFiles: 'index.html',
                        reportName: 'Pact Contract Test Report'
                    ])
                }
            }
        }
    }
}
```

---

### **5. 📊 JENKINS PIPELINE ENHANCEMENT**

#### **Enhanced Jenkinsfile:**
```groovy
pipeline {
    agent any
    
    environment {
        FRAMEWORK_VERSION = "${env.BUILD_NUMBER}"
        SLACK_CHANNEL = '#qa-framework-alerts'
    }
    
    stages {
        stage('Pre-build Validation') {
            steps {
                script {
                    // Validate branch naming convention
                    sh 'scripts/validate-branch-name.sh'
                    
                    // Check for merge conflicts
                    sh 'git merge-tree $(git merge-base HEAD main) HEAD main'
                }
            }
        }
        
        stage('Quality Gates') {
            parallel {
                stage('Code Quality') {
                    steps {
                        sh './scripts/quality-gates.sh'
                    }
                }
                stage('Security Scan') {
                    steps {
                        sh 'scripts/security-scan.sh'
                    }
                }
                stage('Dependency Check') {
                    steps {
                        sh './gradlew dependencyCheckAnalyze'
                    }
                }
            }
        }
        
        stage('Framework Testing') {
            matrix {
                axes {
                    axis {
                        name 'FRAMEWORK'
                        values 'common', 'api-core', 'mobile-core', 'web-core'
                    }
                    axis {
                        name 'JAVA_VERSION'
                        values '17', '21'
                    }
                }
                stages {
                    stage('Test Framework') {
                        steps {
                            script {
                                sh """
                                    export JAVA_HOME=\$JAVA${JAVA_VERSION}_HOME
                                    ./gradlew :${FRAMEWORK}:clean :${FRAMEWORK}:test
                                """
                            }
                        }
                    }
                }
            }
        }
        
        stage('Contract Testing') {
            steps {
                sh './gradlew contractTest'
                sh './gradlew pactPublish'
            }
        }
        
        stage('Performance Baseline') {
            when {
                branch 'main'
            }
            steps {
                sh 'scripts/performance-baseline.sh'
            }
        }
        
        stage('Semantic Versioning') {
            when {
                branch 'main'
            }
            steps {
                sh 'scripts/semantic-versioning.sh'
            }
        }
        
        stage('Publish Framework') {
            when {
                branch 'main'
            }
            steps {
                sh './gradlew publish'
            }
        }
        
        stage('Integration Tests') {
            steps {
                sh 'scripts/integration-tests.sh'
            }
        }
        
        stage('Update Jira') {
            steps {
                script {
                    // Update test execution results in Jira/Xray
                    sh """
                        python scripts/jira-integration.py \
                            --action update-execution \
                            --build-number ${BUILD_NUMBER} \
                            --results build/test-results/
                    """
                }
            }
        }
    }
    
    post {
        always {
            // Publish test results
            publishTestResults testResultsPattern: 'build/test-results/**/*.xml'
            
            // Archive artifacts
            archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            
            // Publish coverage report
            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'build/reports/jacoco/test/html',
                reportFiles: 'index.html',
                reportName: 'Coverage Report'
            ])
        }
        
        success {
            slackSend(
                channel: env.SLACK_CHANNEL,
                color: 'good',
                message: """
                    ✅ Framework Scotia build successful!
                    Branch: ${env.BRANCH_NAME}
                    Build: ${env.BUILD_NUMBER}
                    Duration: ${currentBuild.durationString}
                """
            )
        }
        
        failure {
            slackSend(
                channel: env.SLACK_CHANNEL,
                color: 'danger',
                message: """
                    ❌ Framework Scotia build failed!
                    Branch: ${env.BRANCH_NAME}
                    Build: ${env.BUILD_NUMBER}
                    Check: ${env.BUILD_URL}
                """
            )
        }
    }
}
```

---

---

## 🧪 **TESTING Y CONFIGURACIÓN LOCAL - FASE 2**

### **📋 PROCESOS IMPLEMENTABLES LOCALMENTE**

#### **✅ COMPLETAMENTE CONFIGURABLES LOCALMENTE (SIN DEPENDENCIAS EXTERNAS)**

##### **1. 🔍 Quality Gates Scripts**
```yaml
Ubicación: scripts/quality-gates.sh
Configuración Local:
├── Script bash ejecutable localmente
├── Compilación de common module
├── Tests cross-framework
├── Validación de cobertura
└── Security scan básico

Pruebas Locales Disponibles:
├── ./scripts/quality-gates.sh (ejecución completa)
├── Gradle build validation
├── Test execution verification
├── Coverage threshold testing
└── Security scan básico
```

##### **2. 🎯 Feature Flags (Interface + Factory)**
```yaml
Ubicación: common/interfaces/FeatureFlagService.java + implementations/
Configuración Local:
├── common/src/main/resources/feature-flags.yml
├── FeatureFlagServiceFactory configurado
├── Integración con BaseHttpClient y BaseValidationService
└── Testing completo sin dependencias externas

Pruebas Locales Disponibles:
├── Test de evaluación de flags por usuario/equipo
├── Test de rollout percentage
├── Test de integración con framework
└── Test de refresh de configuración
```

##### **2. 📈 Test Metrics Collection (Interface + Factory)**
```yaml
Ubicación: common/interfaces/TestMetricsCollector.java + implementations/
Configuración Local:
├── BaseTestMetricsCollector con archivos JSON
├── TestMetricsCollectorFactory configurado
├── Directorio build/test-metrics/ para almacenamiento
└── Generación de reportes HTML

Pruebas Locales Disponibles:
├── Test de recolección de métricas
├── Test de generación de reportes diarios
├── Test de análisis de tendencias
└── Test de integración con hooks de Cucumber
```

##### **3. 🔄 Test Data Management (Interface + Factory)**
```yaml
Ubicación: common/interfaces/TestDataManager.java + implementations/
Configuración Local:
├── BaseTestDataManager con BD local
├── TestDataManagerFactory configurado
├── Integration con BaseCucumberHooks
└── Sistema de isolation keys único

Pruebas Locales Disponibles:
├── Test de creación de datasets aislados
├── Test de cleanup automático
├── Test de integración con Cucumber
└── Test de prevención de conflictos de datos
```

##### **4. ⚡ Performance Baselines (Con Estado)**
```yaml
Ubicación: common/automation/PerformanceBaseline.java
Configuración Local:
├── Almacenamiento en performance/baselines/
├── Comparación automática con thresholds
├── Generación de alertas locales
└── Scripts de automatización

Pruebas Locales Disponibles:
├── Test de recording de métricas
├── Test de detección de regresiones
├── Test de threshold configuration
└── Test de baseline establishment
```

##### **5. 🌐 Contract Testing (Estático)**
```yaml
Ubicación: common/contracts/ContractTestManager.java + Pact tests
Configuración Local:
├── Pact testing libraries
├── MockServer para provider simulation
├── Consumer contracts entre frameworks
└── Contract validation automática

Pruebas Locales Disponibles:
├── Test de contratos mobile-framework vs scotia-api
├── Test de contratos web-framework vs scotia-api
├── Test de contratos api-framework vs scotia-api
└── Test de contract validation pipeline
```

##### **6. 📱 Cross-Framework Testing (Estático)**
```yaml
Ubicación: common/automation/CrossFrameworkTester.java
Configuración Local:
├── Tests de compatibilidad HTTP clients
├── Tests de compatibilidad validation services
├── Tests de consistencia de interfaces
└── Tests de factory instantiation

Pruebas Locales Disponibles:
├── Test de HttpClient compatibility across frameworks
├── Test de ValidationService consistency
├── Test de ConfigurationService compatibility
├── Test de FeatureFlagService compatibility
└── Test de interface consistency validation
```

##### **7. 🎮 Flaky Test Detection (Estático)**
```yaml
Ubicación: common/automation/FlakytestDetector.java + scripts/
Configuración Local:
├── Directorio test-history/ para almacenamiento
├── Scripts de análisis en Python
├── Simulación de multiple test runs
└── Detection algorithm configurable

Pruebas Locales Disponibles:
├── Test con historical data simulada
├── Test de cálculo de pass rates
├── Test de threshold configuration
└── Test de report generation
```

##### **8. 🔍 Quality Gates Scripts (Estáticos)**
```yaml
Ubicación: scripts/quality-gates.sh + related scripts
Configuración Local:
├── scripts/quality-gates.sh ejecutable localmente
├── Compilation testing de common module
├── Cross-framework compatibility testing
├── Code coverage verification
└── Basic security scanning

Pruebas Locales Disponibles:
├── ./scripts/quality-gates.sh (ejecución completa)
├── Gradle build validation
├── Test execution verification
├── Coverage threshold testing
└── Security scan básico
```

---

#### **⚠️ PARCIALMENTE CONFIGURABLES LOCALMENTE**

##### **9. 📊 Semantic Versioning (Estático)**
```yaml
Ubicación: scripts/semantic-versioning.sh
Limitaciones Locales:
├── Requiere git tags existentes
├── Análisis de commits desde último tag
├── Generación de changelog básica
└── No puede push tags automáticamente

Configuración Local Posible:
├── Crear tags git de prueba: git tag v1.0.0
├── Test de análisis de commit messages
├── Test de cálculo de nueva versión
└── Test de actualización de build.gradle
```

##### **10. 🛡️ Security Scanning (Estático)**
```yaml
Ubicación: common/automation/SecurityScanner.java + scripts/
Limitaciones Locales:
├── Security scanning básico solamente
├── No integración con herramientas avanzadas
├── Limited dependency vulnerability checking
└── No compliance reporting

Configuración Local Posible:
├── Secrets detection en código fuente
├── Basic security pattern scanning
├── File system security checks
└── Configuration security validation
```

---

#### **❌ NO CONFIGURABLES LOCALMENTE**

##### **11. 📊 Jenkins Pipeline Enhancement**
```yaml
Requiere Infrastructure Externa:
├── Jenkins server
├── Matrix testing agents
├── Slack webhook integration
├── Bitbucket integration
└── Multi-environment testing
```

##### **12. 🔍 Pull Request Governance**
```yaml
Requiere Infrastructure Externa:
├── Bitbucket server
├── Branch protection rules
├── Pull request webhooks
├── Code review automation
└── Repository access controls
```

---

---

### **🚀 SETUP PARA TESTING LOCAL COMPLETO**

#### **Preparación del Ambiente Local:**
```bash
# 1. Crear script de setup automático
cat > scripts/setup-local-testing.sh << 'EOF'
#!/bin/bash
echo "🚀 Setting up local testing environment for Phase 2..."

# Crear directorios necesarios
mkdir -p build/test-metrics
mkdir -p performance/baselines
mkdir -p test-history
mkdir -p common/src/main/resources

# Configurar git tags iniciales
if ! git describe --tags 2>/dev/null; then
    git tag v1.0.0 -m "Initial version for testing"
    echo "✅ Created initial git tag v1.0.0"
fi

# Verificar permisos de scripts
chmod +x scripts/*.sh

echo "✅ Local testing environment ready!"
EOF

chmod +x scripts/setup-local-testing.sh

# 2. Ejecutar setup
./scripts/setup-local-testing.sh

# 3. Configurar feature flags (si no existe)
# Archivo: common/src/main/resources/feature-flags.yml

# 4. Ejecutar tests locales específicos
./gradlew :common:clean :common:test
```

#### **Tests Locales Recomendados por Orden de Implementación:**

```yaml
📅 Semana 1 - Fundamentos Críticos (4-7 horas total):
├── Quality Gates Scripts          → 1-2h
├── Feature Flags System            → 2-3h
└── Semantic Versioning             → 1-2h

📅 Semana 2-3 - Compatibilidad y Seguridad (4-6 horas total):
├── Contract Testing (Pact)         → 2-3h
└── Security Scanner Básico         → 1h

📅 Semana 3-4 - Observabilidad y Estabilidad (4-6 horas total):
├── Performance Baselines           → 2-3h
└── Flaky Test Detection            → 2-3h
```

#### **Validación de Readiness Local:**
```bash
# Crear script de validación completa
cat > scripts/validate-phase2-readiness.sh << 'EOF'
#!/bin/bash
echo "🔍 Validating Phase 2 readiness..."

# 1. Verificar quality gates
echo "📋 Testing quality gates..."
./scripts/quality-gates.sh || exit 1

# 2. Verificar feature flags
echo "🎯 Testing feature flags..."
test -f common/src/main/resources/feature-flags.yml || exit 1

# 3. Verificar semantic versioning
echo "📌 Testing semantic versioning..."
git describe --tags || exit 1

# 4. Compilar y ejecutar tests
echo "🧪 Running all tests..."
./gradlew :common:clean :common:test || exit 1

# 5. Verificar cobertura
echo "📊 Checking coverage..."
./gradlew jacocoTestReport

echo "✅ Phase 2 ready for deployment!"
EOF

chmod +x scripts/validate-phase2-readiness.sh

# Ejecutar validación completa
./scripts/validate-phase2-readiness.sh
```

#### **Checklist de Validación Local:**
```yaml
✅ VALIDACIONES OBLIGATORIAS ANTES DE CONTINUAR:
├── Quality gates scripts ejecutándose sin errores
├── Common module compilando exitosamente
├── Feature flags configurados y testeados
├── Git tags creados correctamente
├── Semantic versioning funcionando
├── Security scan ejecutándose
├── Tests unitarios pasando (>80% cobertura)
└── Documentación actualizada
```

### **📋 BENEFICIOS DEL TESTING LOCAL**

#### **🎯 Desarrollo Incremental:**
- **Validación temprana** de cada componente sin esperar infraestructura
- **Debugging facilitado** sin dependencias externas complejas
- **Iteración rápida** en desarrollo (ciclo de feedback en minutos, no horas)
- **Confidence building** antes de integración con CI/CD

#### **🔧 Risk Mitigation:**
- **Detección de issues** antes de deploy a ambientes compartidos
- **Validación de arquitectura** Interface vs Static en tiempo real
- **Testing de integración** entre Fase 1 y Fase 2 sin romper producción
- **Performance verification** sin impacto en CI compartido

#### **👥 Team Enablement:**
- **Onboarding facilitado** para nuevos miembros (ambiente completo en laptop)
- **Learning environment** para experimentar con nuevos patterns
- **Experimentation space** para probar mejoras sin riesgo
- **Documentation validation** con ejemplos reales ejecutables

#### **💰 Ahorro de Recursos:**
- **Reducción de uso de CI/CD** → solo para validación final
- **Menos tiempo de infraestructura** → desarrolladores autosuficientes
- **Feedback loops más rápidos** → problemas detectados en minutos
- **Menor carga operacional** → menos tickets de soporte

---

## 🎯 **ORDEN DE IMPLEMENTACIÓN RECOMENDADO - ACCIÓN INMEDIATA**

### **📅 ESTA SEMANA (Prioridad Máxima)**

#### **Día 1: Quality Gates Scripts** ⏱️ 1-2 horas
```bash
Acciones:
1. Crear scripts/quality-gates.sh
2. Configurar validaciones básicas
3. Probar localmente con common module
4. Documentar uso en README
```

#### **Día 2-3: Feature Flags System** ⏱️ 2-3 horas  
```bash
Acciones:
1. Crear interfaces/FeatureFlagService.java
2. Implementar BaseFeatureFlagService.java
3. Configurar feature-flags.yml
4. Integrar con BaseHttpClient
5. Crear tests unitarios
```

#### **Día 4-5: Semantic Versioning** ⏱️ 1-2 horas
```bash
Acciones:
1. Crear scripts/semantic-versioning.sh
2. Configurar análisis de commits
3. Probar con git tags locales
4. Integrar con build.gradle
```

**🎯 Resultado Semana 1**: Framework con validación automática, feature flags operativo y versionado inteligente.

---

### **📅 SEMANA 2-3 (Prioridad Media)**

#### **Contract Testing (Pact)** ⏱️ 2-3 horas
```bash
Acciones:
1. Agregar dependencia Pact a common/build.gradle
2. Crear contracts tests para cada framework
3. Configurar MockServer local
4. Validar compatibilidad cross-framework
```

#### **Security Scanner Básico** ⏱️ 1 hora
```bash
Acciones:
1. Crear scripts/security-scan.sh
2. Implementar detección de secrets
3. Configurar dependency check
4. Integrar con quality gates
```

**🎯 Resultado Semana 2-3**: Framework con compatibilidad validada y seguridad básica.

---

### **📅 SEMANA 3-4 (Prioridad Baja)**

#### **Performance Baselines** ⏱️ 2-3 horas
```bash
Acciones:
1. Crear PerformanceBaseline.java
2. Configurar métricas de referencia
3. Implementar detección de regresiones
4. Documentar thresholds
```

#### **Flaky Test Detection** ⏱️ 2-3 horas
```bash
Acciones:
1. Crear FlakyTestDetector.java
2. Implementar análisis de pass rates
3. Configurar alertas
4. Integrar con reportes
```

**🎯 Resultado Semana 3-4**: Framework con observabilidad completa y detección de inestabilidad.

---

## 📊 **MÉTRICAS DE ÉXITO - FASE 2**

### **🎯 KPIs Medibles**
```yaml
✅ Validación Automática:
├── 100% de PRs con quality gates ejecutados
├── <5 minutos tiempo de validación local
├── 0 breaking changes no detectados
└── >80% cobertura de código mantenida

✅ Feature Flags:
├── 100% de nuevas features con flags
├── <2 minutos para rollback
├── 0 deploys fallidos por nuevas features
└── A/B testing en >3 features

✅ Contract Testing:
├── 100% compatibilidad entre frameworks
├── 0 breaking changes no detectados
├── <3 minutos ejecución de contract tests
└── 100% contratos documentados

✅ Seguridad:
├── 0 secrets en código
├── 0 vulnerabilidades críticas
├── 100% dependencias auditadas
└── <2 minutos security scan
```

---

## 🚀 **PRÓXIMOS PASOS DESPUÉS DE FASE 2**

### **📅 Futuro (Requiere Infraestructura Externa)**

#### **Pull Request Governance** (Requiere: Bitbucket Server + webhooks)
- Branch protection rules
- Automated code review
- PR templates y checklists
- Integration con Jira

#### **Jenkins Pipeline Enhancement** (Requiere: Jenkins Server)
- Matrix testing multi-framework
- Parallel execution
- Advanced reporting
- Automated deployment

#### **ELK Stack Integration** (Requiere: Elasticsearch + Kibana)
- Centralized logging
- Advanced metrics dashboards
- Log aggregation
- Real-time monitoring

#### **Grafana Dashboards** (Requiere: Grafana Server)
- Performance monitoring
- Test execution trends
- Framework health dashboard
- Custom metrics visualization

---

## 📖 **CONCLUSIÓN**

### **✅ Estado Actual**
- **Fase 1**: ✅ Completada - Arquitectura sólida y documentada
- **Fase 2**: 🚧 Lista para implementación local inmediata
- **Equipo**: 👥 Preparado para adopción incremental

### **🎯 Valor Inmediato de Fase 2**
1. **Quality Gates** → Prevención automática de errores
2. **Feature Flags** → Despliegue controlado y rollback instantáneo  
3. **Semantic Versioning** → Versionado inteligente automático
4. **Contract Testing** → Compatibilidad garantizada entre frameworks
5. **Security Scanning** → Detección temprana de vulnerabilidades

### **⏱️ Timeline Realista**
- **Semana 1**: Quality Gates + Feature Flags + Semantic Versioning
- **Semana 2-3**: Contract Testing + Security Scanner
- **Semana 3-4**: Performance Baselines + Flaky Test Detection
- **Total**: 3-4 semanas para Fase 2 completa

### **💡 Recomendación Final**
**Comenzar HOY con Quality Gates Scripts** - Es el proceso más rápido (1-2 horas) con mayor impacto inmediato para el equipo de 10-15 QA Engineers.

---

**🎯 ¿Siguiente Acción?**  
```bash
# Ejecutar para comenzar Fase 2
./scripts/setup-local-testing.sh
./scripts/quality-gates.sh
```

---

> **Última actualización**: 19 de Noviembre, 2025  
> **Mantenido por**: QA Core Team  
> **Próxima revisión**: Post-implementación Fase 2 (Diciembre 2025)
````
