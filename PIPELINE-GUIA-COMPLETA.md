# 📘 GUÍA COMPLETA JENKINS PIPELINE - qa-scotia-frameworks

**Fecha:** 2026-02-16  
**Pipeline:** pipeline.jenkins (1,280 líneas)  
**Estado:** ✅ 100% Completo - Enterprise Grade

---

## 🎯 ESTRATEGIA IMPLEMENTADA (Opción A)

```
DEVELOP (Validación completa - 40 min):
├─ Build + Tests + Coverage + Quality Gate (11 checks)
├─ SonarQube (calidad) + Checkmarx (security)
└─ Publicar Maven Local (opcional)
→ Detectar problemas ANTES del merge a master

MASTER (Re-validación + Release - 45 min):
├─ Build + Tests + Coverage + Quality Gate
├─ SonarQube + Checkmarx (re-validación)
└─ Aprobar → Verificar → Publicar Artifactory
→ Doble check antes de producción
```

**Ventaja:** Feedback rápido en develop, master siempre estable

---

## ⚙️ PARÁMETROS (10)

| Parámetro | Default | Descripción |
|-----------|---------|-------------|
| **SKIP_TESTS** | false | Saltar tests (NO recomendado) |
| **CUSTOM_VERSION** | "" | Override versión manual (ej: "2.0.0-RC1") |
| **AUTO_INCREMENT_VERSION** | false | Auto-incrementar desde Artifactory |
| **ENABLE_PUBLISHING** | false | Habilitar publicación artefactos |
| **PUBLISH_TARGET** | NONE | NONE/MAVEN_LOCAL/ARTIFACTORY/BOTH |
| **RUN_COVERAGE** | true | Jacoco coverage |
| **Sonar** | true | SonarQube: calidad + duplicados + comentados |
| **Checkmarx** | true | Security: SQL Injection, XSS, secrets |
| **RUN_OWASP_SCAN** | true | CVEs en dependencias (opcional) |
| **REQUIRE_APPROVAL** | false | Aprobación manual en master |

---

## 🔒 HERRAMIENTAS DE ANÁLISIS

### **Comparación:**

| Herramienta | Qué detecta | Tiempo | Costo |
|-------------|-------------|--------|-------|
| **Jacoco** | Coverage % | 2 min | Gratis |
| **SonarQube** | Duplicados, comentados, complejidad, code smells | 5 min | Licencia |
| **Checkmarx** | SQL Injection, XSS, hardcoded secrets | 25 min | Licencia |
| **OWASP** | CVEs en dependencias (Log4j, Spring, etc.) | 5 min | Gratis |

### **¿Qué NO detecta cada una?**

**Checkmarx NO detecta:**
- ❌ Código duplicado, comentado, complejidad → Usa SonarQube
- ❌ CVEs en dependencias → Usa OWASP

**SonarQube NO detecta:**
- ❌ Security avanzada (SQL Injection) → Usa Checkmarx
- ❌ CVEs en dependencias (community) → Usa OWASP

**OWASP NO detecta:**
- ❌ Vulnerabilidades en tu código → Usa Checkmarx
- ❌ Calidad de código → Usa SonarQube

**Conclusión:** Necesitas las 3 para cobertura completa

---

## 🚦 QUALITY GATE (11 Checks Exigentes)

| # | Check | Métrica | Severidad |
|---|-------|---------|-----------|
| 3 | Tests mínimos | >= 10 tests | ⚠️ Warning |
| 4 | Tasa éxito | >= 95% | ❌ Crítico |
| 5 | Coverage total | >= 70% | ❌ Crítico |
| 6 | Branch coverage | >= 60% | ⚠️ Warning |
| 7 | Artefactos | 12 JARs (4×3) | ⚠️ Warning |
| 8 | Tamaño JAR | > 10 KB | ⚠️ Warning |
| 11 | Warnings | <= 50 deprecations | ⚠️ Warning |
| 12 | Errores Javadoc | 0 errores | ❌ Crítico |
| 13 | SemVer | X.Y.Z formato | ❌ Crítico |

**Errores críticos → Build FALLA ❌**  
**Warnings → Build pasa pero alerta ⚠️**

### **Branch Coverage explicado:**

```java
if (user.isActive()) {   // Branch 1 (if)
    return "Active";     
} else {                 // Branch 2 (else)
    return "Inactive";   
}

Test solo del IF → 50% branch coverage
Test IF + ELSE → 100% branch coverage
```

**Mide:** ¿Testeaste TODAS las rutas posibles?

### **SemVer (Semantic Versioning):**

```
Formato: MAJOR.MINOR.PATCH
Ejemplo: 2.3.7
         │ │ │
         │ │ └─ PATCH: Bug fixes (compatible)
         │ └─── MINOR: Features (compatible)
         └───── MAJOR: Breaking changes

Válidos: 1.0.0, 1.0.5-RC1, 2.3.7-SNAPSHOT
Inválidos: 1.0, v1.0.5, latest
```

---

## 🔧 CONFIGURACIÓN JENKINS

### **Plugins necesarios:**
```
Pipeline, Git, JUnit, HTML Publisher
Jacoco, HTTP Request, Credentials
Workspace Cleanup, AnsiColor
```

### **Credenciales:**
```
ID: Artifactory
Tipo: Username with password
```

### **Herramientas (Global Tool Configuration):**
```
JDK: OpenJDK 21
Gradle: Gradle 8.5
```

---

## 📊 REPORTES EN JENKINS UI

**Después del build:**
```
Build #14
├─ 📊 Test Report - common
├─ 📊 Test Report - api-core
├─ 📊 Test Report - web-core
├─ 📊 Test Report - mobile-core
├─ 📊 Coverage Report - common
├─ 📊 Coverage Report - api-core
├─ 📊 Coverage Report - web-core
├─ 📊 Coverage Report - mobile-core
├─ 🔒 SonarQube Dashboard (link externo)
├─ 🔒 Checkmarx Dashboard (link externo)
└─ 📦 Build Artifacts (12 JARs)
```

---

## 🚀 CASOS DE USO

### **1. Desarrollo normal (develop):**
```
Parámetros: (todos default)
Resultado: Build + Tests + Coverage + Sonar + Checkmarx
Tiempo: ~40 min
Publica: NO (ENABLE_PUBLISHING=false)
```

### **2. Build rápido (develop):**
```
SKIP_TESTS: false
Sonar: false
Checkmarx: false
RUN_COVERAGE: false
Resultado: Solo build + tests
Tiempo: ~8 min
```

### **3. Release producción (master):**
```
Rama: master
ENABLE_PUBLISHING: true
PUBLISH_TARGET: ARTIFACTORY
REQUIRE_APPROVAL: true
Resultado: Full validación + aprobación + publicación
Tiempo: ~50 min
```

### **4. Testing con Maven Local:**
```
ENABLE_PUBLISHING: true
PUBLISH_TARGET: MAVEN_LOCAL
Resultado: Publica a ~/.m2/repository
```

---

## 🔍 BUSCAR SONARQUBE EN BITBUCKET

### **En Jenkinsfile:**
Buscar: `withSonarQubeEnv`, `sonarqube`, `waitForQualityGate`

### **En build.gradle:**
Buscar: `id 'org.sonarqube'`, `sonar.projectKey`

### **Archivos:**
Buscar: `sonar-project.properties`, `.sonarqube/`

---

## 🛠️ TROUBLESHOOTING

### **Error: Credentials not found**
Crear: Manage Jenkins → Credentials → ID: `Artifactory`

### **Error: codeQuality.runCodeQuality() not found**
Verificar: `@Library('pipeline-utils') _` en línea 2

### **Error: No test reports**
Verificar: Tests se ejecutaron (SKIP_TESTS=false)

### **Error: Coverage report not found**
Normal si módulo no tiene tests. Usa `allowMissing: true`

### **Variables Checkmarx manual (si lo descomentas):**
Definir: USF_USR, USF_PSW, HTTPS_PROXY en environment o credentials

---

## 📦 PUBLICACIONES

### **Artefactos generados (siempre):**
```
build/libs/
├── common-1.0.0.jar
├── common-1.0.0-javadoc.jar
├── common-1.0.0-sources.jar
└── ... (12 JARs total)
```

### **Publicación (opcional):**
```
Maven Local: ~/.m2/repository/com/scotia/qa/
Artifactory: libs-release-thirdparty/com/scotia/qa/
```

**Nota:** Generar ≠ Publicar
- Generar: Crear JARs (siempre)
- Publicar: Subir a repo (opcional)

---

## 🎯 STAGES ANIDADOS (Organización UI - Opcional)

Para organizar la UI de Jenkins con secciones:

```groovy
stages {
    stage('⚙️ CONFIGURACIÓN') {
        stages {
            stage('Checkout') { }
            stage('Versión') { }
        }
    }
    
    stage('🔨 BUILD') {
        stages {
            stage('Limpiar') { }
            stage('Compilar') { }
        }
    }
    
    stage('🧪 TESTS Y CALIDAD') {
        stages {
            stage('Tests') { }
            stage('Coverage') { }
            stage('Quality Gate') { }
        }
    }
    
    stage('🔒 SEGURIDAD') {
        stages {
            stage('Code Quality') { }  // Sonar + Checkmarx
        }
    }
    
    stage('📦 PUBLICACIONES') {
        stages {
            stage('Generar') { }
            stage('Aprobar') { }
            stage('Publicar') { }
        }
    }
}
```

**Resultado:** UI organizada con secciones colapsables

---

## 📋 CHECKLIST PRE-PRODUCCIÓN

- [ ] Plugins instalados en Jenkins
- [ ] Credencial `Artifactory` creada
- [ ] JDK 21 y Gradle 8.5 configurados
- [ ] `@Library('pipeline-utils')` funciona
- [ ] Test build en develop ejecutado
- [ ] Verificar que codeQuality.runCodeQuality() funciona
- [ ] Ver 8 reportes en Jenkins UI
- [ ] Test en master con publicación
- [ ] Capacitar equipo en parámetros nuevos

---

## 🎯 DECISIÓN FINAL: HERRAMIENTAS

### **Usar (3 herramientas):**
```
✅ SonarQube → Calidad general (duplicados, comentados, complejidad)
✅ Checkmarx → Security avanzada (SQL Injection, XSS)
✅ OWASP → CVEs en dependencias (opcional si Sonar no los detecta)
```

### **NO usar:**
```
❌ BlackDuck → OWASP lo reemplaza (gratis vs $$$)
```

**Solo usar BlackDuck si:** Compliance legal lo requiere

---

## 🔑 FUNCIÓN COMPARTIDA

### **¿Qué es `codeQuality.runCodeQuality()`?**

Función de `pipeline-utils` library que ejecuta Sonar y/o Checkmarx automáticamente.

**Uso:**
```groovy
codeQuality.runCodeQuality(runCheckmarx, runSonar)
```

**Maneja internamente:**
- ✅ Autenticación (credenciales)
- ✅ Configuración (project keys)
- ✅ Ejecución de scans
- ✅ Publicación de reportes
- ✅ Quality gates
- ✅ Errores y fallos

---

## ⚡ QUICK REFERENCE

### **Versionado (3 estrategias):**
```
1. CUSTOM_VERSION = "2.0.0" → Usa esta
2. AUTO_INCREMENT_VERSION = true → Consulta Artifactory + incrementa
3. Fallback → Lee gradle.properties
```

### **Publicación:**
```
ENABLE_PUBLISHING=false → NO publica
ENABLE_PUBLISHING=true + PUBLISH_TARGET:
  - MAVEN_LOCAL → ~/.m2/repository
  - ARTIFACTORY → Artifactory (solo master)
  - BOTH → Ambos (testing)
```

### **Security scanning:**
```
Develop: Sonar + Checkmarx + Quality Gate
Master: Sonar + Checkmarx (re-validación)
```

---

## 📞 SOPORTE

**Documentación:** JENKINS-PIPELINE-GUIA.md (guía principal)  
**Pipeline:** pipeline.jenkins  
**Build config:** build.gradle  

**Para issues:**
- Artifactory → DevOps
- SonarQube/Checkmarx → Security Team
- Pipeline → QA Team Lead

---

**📅 Última actualización:** 2026-02-16  
**✅ Estado:** Listo para producción  
**🚀 Siguiente:** Commit → Scan → Test build

