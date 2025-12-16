# 📦 Guía de Dependencias Gradle - Scotia QA Framework

## 📋 Índice
- [Estrategia de Repositorios](#estrategia-de-repositorios)
- [Desarrollo Local (mavenLocal)](#desarrollo-local-mavenlocal)
- [CI/CD (Artifactory)](#cicd-artifactory)
- [Publicación de Capas](#publicación-de-capas)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Estrategia de Repositorios

El framework soporta **2 estrategias** para resolución de dependencias:

| **Repositorio** | **Cuándo usar** | **Configuración** |
|----------------|-----------------|-------------------|
| **mavenLocal** | Desarrollo local | Por defecto |
| **Artifactory** | CI/CD, Producción | `-PuseArtifactory=true` |

---

## 🏠 Desarrollo Local (mavenLocal)

### ✅ Cuándo usar
- Desarrollo activo de las capas del framework
- Testing local de cambios
- Sin acceso a Artifactory
- Desarrollo offline

### 📦 Paso 1: Publicar capas en mavenLocal

```bash
# Limpiar mavenLocal (opcional)
rm -rf ~/.m2/repository/com/scotia/qa/

# Publicar TODAS las capas
cd /path/to/qa-scotia-frameworks
./gradlew publishToMavenLocal

# O publicar capas individuales
./gradlew :common:publishToMavenLocal
./gradlew :api-core:publishToMavenLocal
./gradlew :web-core:publishToMavenLocal
./gradlew :mobile-core:publishToMavenLocal
```

**Resultado:**
```
~/.m2/repository/com/scotia/qa/
├── common/1.0.0/
│   ├── common-1.0.0.jar
│   ├── common-1.0.0.pom
│   ├── common-1.0.0-sources.jar
│   └── common-1.0.0-javadoc.jar
├── api-core/1.0.0/
│   ├── api-core-1.0.0.jar
│   ├── api-core-1.0.0.pom
│   ├── api-core-1.0.0-sources.jar
│   └── api-core-1.0.0-javadoc.jar
├── web-core/1.0.0/
└── mobile-core/1.0.0/
```

### 🔧 Paso 2: Configurar módulo

#### **build.gradle del módulo**
```groovy
repositories {
    mavenLocal()  // ← Busca en ~/.m2/repository
    mavenCentral()
}

dependencies {
    implementation 'com.scotia.qa:common:1.0.0'
    implementation 'com.scotia.qa:api-core:1.0.0'
    implementation 'com.scotia.qa:web-core:1.0.0'
    // implementation 'com.scotia.qa:mobile-core:1.0.0'
}
```

### ✅ Paso 3: Ejecutar tests
```bash
cd /path/to/qa-module-banking
./gradlew clean test
```

**Logs esperados:**
```
> Task :compileJava
Resolving dependencies from mavenLocal...
- com.scotia.qa:common:1.0.0 ✓
- com.scotia.qa:api-core:1.0.0 ✓
```

---

## 🏢 CI/CD (Artifactory)

### ✅ Cuándo usar
- Ejecución en Jenkins/GitLab CI
- Ambientes compartidos (QA, UAT, PROD)
- Equipos distribuidos
- Producción

### 🔐 Paso 1: Configurar credenciales

#### **Opción A: Variables de entorno (Recomendado para CI/CD)**
```bash
export ARTIFACTORY_USER=tu_usuario
export ARTIFACTORY_PASSWORD=tu_token
```

#### **Opción B: gradle.properties (Desarrollo local)**
```properties
# ~/.gradle/gradle.properties (NO commitear)
artifactoryUser=tu_usuario
artifactoryPassword=tu_token
```

### 📦 Paso 2: Ejecutar con flag

```bash
# Compilar usando Artifactory
./gradlew build -PuseArtifactory=true

# Ejecutar tests usando Artifactory
./gradlew test -PuseArtifactory=true

# Limpiar y compilar
./gradlew clean build -PuseArtifactory=true
```

**Logs esperados:**
```
> Task :compileJava
Resolving dependencies from Artifactory...
- com.scotia.qa:common:1.0.0 ✓
- com.scotia.qa:api-core:1.0.0 ✓
Falling back to mavenCentral for external dependencies...
```

### 🔧 Configuración en build.gradle del módulo

```groovy
repositories {
    // Switch automático según flag -PuseArtifactory
    if (project.hasProperty('useArtifactory') && project.property('useArtifactory') == 'true') {
        maven {
            url 'https://artifactory.cldevops.chl.bns/artifactory/external-repository'
            credentials {
                username = project.findProperty('artifactoryUser') ?: System.getenv('ARTIFACTORY_USER')
                password = project.findProperty('artifactoryPassword') ?: System.getenv('ARTIFACTORY_PASSWORD')
            }
        }
        mavenCentral()
    } else {
        mavenLocal()
        mavenCentral()
    }
}

dependencies {
    implementation 'com.scotia.qa:common:1.0.0'
    implementation 'com.scotia.qa:api-core:1.0.0'
    implementation 'com.scotia.qa:web-core:1.0.0'
}
```

---

## 📤 Publicación de Capas

### 🏠 Publicar en mavenLocal (Único método disponible)

**IMPORTANTE:** Solo puedes publicar en `mavenLocal` porque **no tienes permisos de escritura** en Artifactory.

```bash
cd /path/to/qa-scotia-frameworks

# Publicar TODO
./gradlew publishToMavenLocal

# Publicar capa específica
./gradlew :common:publishToMavenLocal
./gradlew :api-core:publishToMavenLocal
./gradlew :web-core:publishToMavenLocal
./gradlew :mobile-core:publishToMavenLocal
```

**Verificar publicación:**
```bash
# Mac/Linux
ls -la ~/.m2/repository/com/scotia/qa/common/1.0.0/

# Windows
dir %USERPROFILE%\.m2\repository\com\scotia\qa\common\1.0.0\
```

**Resultado:**
```
~/.m2/repository/com/scotia/qa/common/1.0.0/
├── common-1.0.0.jar
├── common-1.0.0.pom
├── common-1.0.0-sources.jar
└── common-1.0.0-javadoc.jar
```

### 🏢 Publicar en Artifactory (Solo Infra/DevOps)

**Nota:** Tú **NO** puedes publicar directamente en Artifactory. Este proceso lo realiza Infra manualmente.

**Proceso para solicitar publicación:**

```bash
# 1. Generar artefactos completos
cd /path/to/qa-scotia-frameworks
./gradlew clean build -x test

# 2. Los artefactos se generan en:
common/build/libs/
├── common-1.0.0.jar
├── common-1.0.0-sources.jar
└── common-1.0.0-javadoc.jar

# Y el POM en:
common/build/publications/mavenJava/pom-default.xml

# 3. Recopilar artefactos para enviar a Infra
mkdir -p artifacts/common/1.0.0
cp common/build/libs/* artifacts/common/1.0.0/
cp common/build/publications/mavenJava/pom-default.xml artifacts/common/1.0.0/common-1.0.0.pom

# 4. Enviar a Infra con ticket solicitando publicación
# Incluir: .jar, .pom, -sources.jar, -javadoc.jar
```

---

## 🔄 Flujo de Trabajo Recomendado

### 👨‍💻 Desarrollo Local

```bash
# 1. Hacer cambios en el framework
cd /path/to/qa-scotia-frameworks
# ... editar código ...

# 2. Publicar localmente
./gradlew publishToMavenLocal

# 3. Probar en módulo
cd /path/to/qa-module-banking
./gradlew clean test

# 4. Si funciona, commitear y pushear framework
```

### 🚀 CI/CD (Jenkins)

```groovy
// Jenkinsfile
stage('Build') {
    steps {
        sh './gradlew clean build -PuseArtifactory=true'
    }
}

stage('Test') {
    steps {
        sh './gradlew test -PuseArtifactory=true'
    }
}
```

---

## 🐛 Troubleshooting

### ❌ Error: "Could not find com.scotia.qa:common:1.0.0"

**Causa:** Dependencia no publicada en el repositorio configurado.

**Solución:**
```bash
# Si estás usando mavenLocal (por defecto)
cd /path/to/qa-scotia-frameworks
./gradlew :common:publishToMavenLocal

# Verificar publicación
ls ~/.m2/repository/com/scotia/qa/common/1.0.0/

# Si estás usando Artifactory
# Contactar a Infra para verificar publicación
```

### ❌ Error: "401 Unauthorized" (Artifactory)

**Causa:** Credenciales incorrectas o expiradas.

**Solución:**
```bash
# 1. Verificar variables de entorno
echo $ARTIFACTORY_USER
echo $ARTIFACTORY_PASSWORD

# 2. Verificar credenciales
curl -u $ARTIFACTORY_USER:$ARTIFACTORY_PASSWORD \
  https://artifactory.cldevops.chl.bns/artifactory/external-repository

# 3. Si fallan, solicitar nuevas credenciales a Infra
```

### ❌ Error: "Connection timeout" (Artifactory)

**Causa:** Sin conexión a red o firewall bloqueando.

**Solución:**
```bash
# 1. Verificar conectividad
ping artifactory.cldevops.chl.bns

# 2. Cambiar temporalmente a mavenLocal
./gradlew clean test  # (sin -PuseArtifactory)

# 3. Verificar proxy corporativo si aplica
```

### ❌ Error: "Duplicate publication"

**Causa:** Múltiples publicaciones configuradas en build.gradle.

**Solución:**
```bash
# Ya está corregido en versión 1.0.0+
# Si usas versión antigua, actualizar framework
```

### ❌ Error: "Failed to resolve" con mavenLocal

**Causa:** Cache corrupto de Gradle.

**Solución:**
```bash
# 1. Limpiar cache de Gradle
rm -rf ~/.gradle/caches/

# 2. Republicar framework
cd /path/to/qa-scotia-frameworks
./gradlew clean publishToMavenLocal

# 3. Limpiar módulo y recompilar
cd /path/to/qa-module-banking
./gradlew clean --refresh-dependencies test
```

---

## 📊 Comparativa de Repositorios

| **Característica** | **mavenLocal** | **Artifactory** |
|-------------------|----------------|-----------------|
| **Setup inicial** | Rápido (5 min) | Requiere credenciales |
| **Sin internet** | ✅ Funciona | ❌ Requiere red |
| **CI/CD** | ❌ Requiere publicación manual | ✅ Ideal |
| **Velocidad** | ⚡ Inmediata | 🐌 Primera vez lenta |
| **Versionado** | Manual | Automático |
| **Compartido** | ❌ Solo tu máquina | ✅ Todo el equipo |

---

## 🎯 Recomendaciones Finales

### ✅ **Para Desarrollo Local**
```bash
# Sin flag (usa mavenLocal por defecto)
./gradlew test
```

### ✅ **Para CI/CD**
```bash
# Con flag
./gradlew test -PuseArtifactory=true
```

### ✅ **Para Infra (Publicación)**
```bash
# Generar artefactos completos
./gradlew clean build

# Enviar a Artifactory:
# - {module}-{version}.jar
# - {module}-{version}.pom
# - {module}-{version}-sources.jar
# - {module}-{version}-javadoc.jar
```

---

## 📞 Soporte

- **Documentación:** `/documentacion/FRAMEWORK-GUIDE.md`
- **Issues:** Contactar al equipo de QA
- **Artifactory:** Contactar a Infra/DevOps

