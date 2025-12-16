# 📦 Gradle: Repositories vs Publishing - Guía Rápida

## 🎯 CONCEPTOS CLAVE

### 1️⃣ **`repositories` → DESCARGAR dependencias**
Define **DÓNDE BUSCAR** las dependencias que necesitas (bibliotecas externas o del framework).

### 2️⃣ **`publishing.repositories` → PUBLICAR artefactos**
Define **DÓNDE PUBLICAR** los artefactos que TÚ generas (las capas del framework).

---

## 🔍 TU CONFIGURACIÓN ACTUAL

### ✅ Para DESCARGAR dependencias (`repositories`)

```groovy
repositories {
    if (project.hasProperty('useArtifactory') && project.property('useArtifactory') == 'true') {
        // CI/CD: Priorizar Artifactory
        maven { url 'https://artifactory.cldevops.chl.bns/artifactory/external-repository' }
        mavenLocal()
        mavenCentral()
    } else {
        // Desarrollo: Priorizar mavenLocal, pero incluir Artifactory como fallback
        mavenLocal()
        maven { url 'https://artifactory.cldevops.chl.bns/artifactory/external-repository' }
        mavenCentral()
    }
}
```

**Uso:**
```bash
# Desarrollo: Busca en mavenLocal → Artifactory → mavenCentral
./gradlew build

# CI/CD: Busca en Artifactory → mavenLocal → mavenCentral
./gradlew build -PuseArtifactory=true
```

**¿Qué hace el flag?**
- **SIN flag:** Prioriza `mavenLocal`, luego busca en `Artifactory` si no encuentra
- **CON flag:** Prioriza `Artifactory`, luego busca en `mavenLocal` si no encuentra

**Ventajas de esta estrategia:**
- ✅ **Desarrollo offline:** Usa mavenLocal primero (rápido)
- ✅ **Ambientes corporativos:** Artifactory resuelve dependencias externas (sin firewall)
- ✅ **CI/CD:** Prioriza Artifactory (siempre actualizado)
- ✅ **Fallback automático:** Si uno falla, intenta el siguiente

---

### ✅ Para PUBLICAR artefactos (`publishing.repositories`)

```groovy
publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
        }
    }
    repositories {
        mavenLocal()  // ← SIEMPRE mavenLocal
    }
}
```

**Uso:**
```bash
# Publicar en mavenLocal (único permitido)
./gradlew publishToMavenLocal
```

**¿Qué hace?**
- Publica tus artefactos en `~/.m2/repository/com/scotia/qa/`
- **NO** hay switch porque **NO** tienes permisos de escritura en Artifactory

---

## 📊 TABLA COMPARATIVA

| **Acción** | **Comando** | **Orden de búsqueda** |
|------------|-------------|-----------------------|
| **DESCARGAR** (desarrollo) | `./gradlew build` | 1. mavenLocal<br>2. Artifactory<br>3. mavenCentral |
| **DESCARGAR** (CI/CD) | `./gradlew build -PuseArtifactory=true` | 1. Artifactory<br>2. mavenLocal<br>3. mavenCentral |
| **PUBLICAR** | `./gradlew publishToMavenLocal` | Siempre → mavenLocal |

**Nota clave:** Artifactory **SIEMPRE** está disponible como fallback, incluso sin flag.

---

## 🎬 EJEMPLOS PRÁCTICOS

### 📝 Escenario 1: Desarrollo local activo

**Situación:** Estás desarrollando cambios en el framework.

```bash
# 1. Hacer cambios en common
cd /path/to/qa-scotia-frameworks
vim common/src/main/java/...

# 2. Publicar en mavenLocal
./gradlew :common:publishToMavenLocal

# 3. Probar en módulo (usa mavenLocal automáticamente)
cd /path/to/qa-module-banking
./gradlew clean test
```

**¿Usa Artifactory?** ❌ No, usa mavenLocal (`~/.m2/repository/`)

---

### 📝 Escenario 2: CI/CD en Jenkins

**Situación:** Jenkins ejecuta tests del módulo.

```groovy
// Jenkinsfile
stage('Build') {
    steps {
        // Descarga dependencias desde Artifactory
        sh './gradlew clean build -PuseArtifactory=true'
    }
}
```

**¿Usa Artifactory?** ✅ Sí, porque pasas `-PuseArtifactory=true`

---

### 📝 Escenario 3: Publicar nueva versión del framework

**Situación:** Quieres que Infra publique nueva versión en Artifactory.

```bash
# 1. Generar artefactos
cd /path/to/qa-scotia-frameworks
./gradlew clean build -x test

# 2. Los artefactos están en:
common/build/libs/
├── common-1.0.0.jar
├── common-1.0.0-sources.jar
├── common-1.0.0-javadoc.jar
└── common-1.0.0.pom (en build/publications/mavenJava/)

# 3. Enviar a Infra para que publique en Artifactory
```

**¿Publicas tú en Artifactory?** ❌ No, Infra lo hace manualmente

---

## ❓ PREGUNTAS FRECUENTES

### ❓ ¿El flag `-PuseArtifactory` afecta la publicación?

❌ **NO.** Solo afecta la **descarga** de dependencias.

La publicación **siempre** va a `mavenLocal` porque no tienes permisos de escritura en Artifactory.

---

### ❓ ¿Cómo sé si está usando mavenLocal o Artifactory?

Mira los logs al compilar:

```bash
./gradlew build --info | grep -i "repository"
```

**Con mavenLocal:**
```
Resolving dependencies from mavenLocal...
```

**Con Artifactory:**
```
Resolving dependencies from https://artifactory.cldevops.chl.bns...
```

---

### ❓ ¿Por qué no puedo publicar en Artifactory?

Porque no tienes permisos de **escritura**. Artifactory está configurado como:
- ✅ **Lectura:** Cualquiera con acceso a la red corporativa
- ❌ **Escritura:** Solo Infra/DevOps

---

### ❓ ¿Cómo pruebo cambios antes de que Infra publique en Artifactory?

Publica en `mavenLocal` y prueba localmente:

```bash
# Framework
./gradlew :common:publishToMavenLocal

# Módulo (usa mavenLocal por defecto)
cd /path/to/qa-module-banking
./gradlew clean test
```

---

## 🎯 RESUMEN EJECUTIVO

### 📥 DESCARGAR dependencias (usa `repositories`)
```bash
# mavenLocal (desarrollo)
./gradlew build

# Artifactory (CI/CD)
./gradlew build -PuseArtifactory=true
```

### 📤 PUBLICAR artefactos (usa `publishing.repositories`)
```bash
# Siempre mavenLocal (único permitido)
./gradlew publishToMavenLocal
```

### 🔑 Diferencia clave
- **`repositories`** = De dónde **BAJAR** dependencias → Puedes elegir (flag)
- **`publishing.repositories`** = Dónde **SUBIR** artefactos → Siempre mavenLocal (sin flag)

---

## 📞 Soporte

- **Documentación:** `/config/GRADLE-DEPENDENCIES-GUIDE.md`
- **Issues:** Contactar al equipo de QA

