# 📦 Publicar Framework en Maven Local desde Windows

**Framework**: Scotia QA Framework v1.0.0  
**Fecha**: Diciembre 2025  
**Para**: Equipos QA con Windows

---

## ✅ Requisitos Previos (Windows)

### 1. Java Development Kit (JDK) 21

**Verificar si está instalado**:
```powershell
java -version
```

**Salida esperada**:
```
openjdk version "21.0.x" 2024-xx-xx
OpenJDK Runtime Environment (build 21.0.x+xx)
OpenJDK 64-Bit Server VM (build 21.0.x+xx, mixed mode, sharing)
```

**Si NO está instalado**:

#### Opción A: Instalar OpenJDK 21 (Recomendado)

```powershell
# Usar winget (Windows 10/11)
winget install Microsoft.OpenJDK.21

# O descargar manualmente desde:
# https://learn.microsoft.com/en-us/java/openjdk/download
```

#### Opción B: Instalar Oracle JDK 21

```powershell
# Descargar desde:
# https://www.oracle.com/java/technologies/downloads/#java21
```

**Configurar JAVA_HOME**:
```powershell
# Abrir PowerShell como Administrador
[System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-21.0.x', 'Machine')
[System.Environment]::SetEnvironmentVariable('PATH', "$env:JAVA_HOME\bin;$env:PATH", 'Machine')

# Cerrar y abrir nueva terminal
# Verificar
java -version
echo $env:JAVA_HOME
```

---

### 2. Gradle (Ya incluido en el framework)

**El framework incluye Gradle Wrapper** (`gradlew.bat`), NO necesitas instalar Gradle.

**Verificar**:
```powershell
cd C:\ruta\a\qa-scotia-frameworks
.\gradlew.bat --version
```

**Salida esperada**:
```
------------------------------------------------------------
Gradle 8.5
------------------------------------------------------------

Build time:   2023-11-29 14:08:57 UTC
Revision:     28aca86a7180baa17117e0e5ba01d8ea9feca598

Kotlin:       1.9.20
Groovy:       3.0.17
Ant:          Apache Ant(TM) version 1.10.13 compiled on January 4 2023
JVM:          21.0.x (Microsoft 21.0.x+xx)
OS:           Windows 11 10.0 amd64
```

---

### 3. Git (Opcional, para clonar)

**Verificar**:
```powershell
git --version
```

**Si NO está instalado**:
```powershell
# Usar winget
winget install Git.Git

# O descargar desde:
# https://git-scm.com/download/win
```

---

## 📂 Estructura de Maven Local en Windows

```
C:\Users\{TU_USUARIO}\.m2\repository\
└── com\scotia\qa\
    ├── common\
    │   └── 1.0.0\
    │       ├── common-1.0.0.jar
    │       ├── common-1.0.0.pom
    │       ├── common-1.0.0-sources.jar
    │       └── common-1.0.0-javadoc.jar
    ├── api-core\
    │   └── 1.0.0\
    │       ├── api-core-1.0.0.jar
    │       ├── api-core-1.0.0.pom
    │       ├── api-core-1.0.0-sources.jar
    │       └── api-core-1.0.0-javadoc.jar
    ├── web-core\
    │   └── 1.0.0\...
    └── mobile-core\
        └── 1.0.0\...
```

---

## 🚀 PUBLICAR EN MAVEN LOCAL (Windows)

### Opción 1: Publicar TODAS las Capas (Recomendado)

```powershell
# 1. Navegar al directorio del framework
cd C:\ruta\a\qa-scotia-frameworks

# 2. Limpiar builds anteriores
.\gradlew.bat clean

# 3. Compilar y publicar todas las capas
.\gradlew.bat publishToMavenLocal

# Salida esperada:
# > Task :common:publishToMavenLocal
# > Task :api-core:publishToMavenLocal
# > Task :web-core:publishToMavenLocal
# > Task :mobile-core:publishToMavenLocal
#
# BUILD SUCCESSFUL in Xs
```

---

### Opción 2: Publicar Solo una Capa Específica

```powershell
# Solo common
.\gradlew.bat :common:publishToMavenLocal

# Solo api-core
.\gradlew.bat :api-core:publishToMavenLocal

# Solo web-core
.\gradlew.bat :web-core:publishToMavenLocal

# Solo mobile-core
.\gradlew.bat :mobile-core:publishToMavenLocal
```

---

### Opción 3: Script PowerShell Automatizado

**Crear archivo**: `publish-all.ps1`

```powershell
# ============================================================
# Script para Publicar Framework en Maven Local (Windows)
# ============================================================

Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  Scotia QA Framework - Publicar en Maven Local" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Verificar que estamos en el directorio correcto
if (-not (Test-Path ".\gradlew.bat")) {
    Write-Host "❌ Error: gradlew.bat no encontrado" -ForegroundColor Red
    Write-Host "   Ejecuta este script desde: qa-scotia-frameworks\" -ForegroundColor Yellow
    exit 1
}

# Verificar Java
Write-Host "🔍 Verificando Java..." -ForegroundColor Yellow
$javaVersion = & java -version 2>&1 | Select-String "version" | Select-Object -First 1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Java no encontrado. Instala JDK 21." -ForegroundColor Red
    exit 1
}
Write-Host "✓ Java detectado: $javaVersion" -ForegroundColor Green
Write-Host ""

# Limpiar builds anteriores
Write-Host "🧹 Limpiando builds anteriores..." -ForegroundColor Yellow
& .\gradlew.bat clean --quiet
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Error en clean" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Limpieza completada" -ForegroundColor Green
Write-Host ""

# Publicar todas las capas
Write-Host "📦 Publicando capas en Maven Local..." -ForegroundColor Yellow
Write-Host ""

$capas = @("common", "api-core", "web-core", "mobile-core")

foreach ($capa in $capas) {
    Write-Host "  → Publicando $capa..." -ForegroundColor Cyan
    & .\gradlew.bat ":$capa:publishToMavenLocal" --quiet
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "    ✓ $capa publicado" -ForegroundColor Green
    } else {
        Write-Host "    ❌ Error publicando $capa" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  ✅ PUBLICACIÓN COMPLETADA" -ForegroundColor Green
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Mostrar ubicación
$mavenLocal = "$env:USERPROFILE\.m2\repository\com\scotia\qa"
Write-Host "📁 Ubicación:" -ForegroundColor Yellow
Write-Host "   $mavenLocal" -ForegroundColor White
Write-Host ""

# Verificar artefactos
Write-Host "📋 Artefactos publicados:" -ForegroundColor Yellow
foreach ($capa in $capas) {
    $artifactPath = "$mavenLocal\$capa\1.0.0"
    if (Test-Path $artifactPath) {
        $files = Get-ChildItem $artifactPath -File | Select-Object -ExpandProperty Name
        Write-Host "   ✓ $capa" -ForegroundColor Green
        foreach ($file in $files) {
            Write-Host "     - $file" -ForegroundColor Gray
        }
    } else {
        Write-Host "   ❌ $capa - No encontrado" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "🚀 Próximos pasos:" -ForegroundColor Yellow
Write-Host "   1. Los módulos ya pueden importar el framework" -ForegroundColor White
Write-Host "   2. Ejecutar: .\gradlew.bat test desde tu módulo" -ForegroundColor White
Write-Host ""
```

**Ejecutar**:
```powershell
# Dar permisos (primera vez)
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

# Ejecutar
.\publish-all.ps1
```

---

## ✅ Verificar Publicación Exitosa

### Verificar con PowerShell

```powershell
# Navegar a Maven Local
cd $env:USERPROFILE\.m2\repository\com\scotia\qa

# Listar capas publicadas
Get-ChildItem -Recurse -Filter "*.jar" | Format-Table Name, Length, LastWriteTime
```

**Salida esperada**:
```
Name                           Length LastWriteTime
----                           ------ -------------
common-1.0.0.jar              234567 12/5/2025 10:30 AM
common-1.0.0-sources.jar      156789 12/5/2025 10:30 AM
common-1.0.0-javadoc.jar       89012 12/5/2025 10:30 AM
api-core-1.0.0.jar            123456 12/5/2025 10:30 AM
api-core-1.0.0-sources.jar     78901 12/5/2025 10:30 AM
api-core-1.0.0-javadoc.jar     45678 12/5/2025 10:30 AM
...
```

### Verificar con Gradle (desde un módulo)

```powershell
# Crear módulo de prueba
cd C:\workspace
C:\ruta\a\qa-scotia-frameworks\scripts\create-module.sh test-verify

cd qa-module-test-verify

# Verificar dependencias
.\gradlew.bat dependencies --configuration testRuntimeClasspath | Select-String "scotia"
```

**Salida esperada**:
```
+--- com.scotia.qa:common:1.0.0
+--- com.scotia.qa:api-core:1.0.0
+--- com.scotia.qa:web-core:1.0.0
+--- com.scotia.qa:mobile-core:1.0.0
```

---

## 🐛 Troubleshooting Windows

### Error: "JAVA_HOME is not set"

**Causa**: Variable de entorno no configurada

**Solución**:
```powershell
# Verificar JAVA_HOME
echo $env:JAVA_HOME

# Si está vacío, configurar:
[System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-21.0.x', 'Machine')

# Cerrar y abrir nueva terminal
```

---

### Error: "gradlew.bat: The term is not recognized"

**Causa**: No estás en el directorio correcto

**Solución**:
```powershell
# Verificar ubicación
Get-Location

# Navegar al framework
cd C:\ruta\a\qa-scotia-frameworks

# Verificar que existe
Test-Path .\gradlew.bat
```

---

### Error: "Access denied" al publicar

**Causa**: Permisos insuficientes en `C:\Users\{usuario}\.m2\`

**Solución**:
```powershell
# Ejecutar PowerShell como Administrador
# Luego intentar de nuevo
.\gradlew.bat publishToMavenLocal
```

---

### Error: "Out of memory" durante compilación

**Causa**: Gradle necesita más memoria

**Solución**:
```powershell
# Editar gradle.properties
notepad gradle.properties

# Agregar:
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m

# Guardar y reintentar
.\gradlew.bat clean publishToMavenLocal
```

---

### Error: "Could not resolve dependency"

**Causa**: Maven Central no accesible (proxy corporativo)

**Solución**:
```powershell
# Opción 1: Configurar proxy en gradle.properties
notepad gradle.properties

# Agregar:
systemProp.http.proxyHost=proxy.scotia.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.scotia.com
systemProp.https.proxyPort=8080

# Opción 2: Usar VPN corporativa y reintentar
```

---

## 📊 Comparación: Windows vs macOS/Linux

| Aspecto | Windows | macOS/Linux |
|---------|---------|-------------|
| **Comando** | `.\gradlew.bat` | `./gradlew` |
| **Maven Local** | `C:\Users\{user}\.m2\` | `~/.m2/` |
| **JAVA_HOME** | `C:\Program Files\...` | `/Library/Java/...` |
| **Scripts** | PowerShell (`.ps1`) | Bash (`.sh`) |
| **Separador** | `\` (backslash) | `/` (forward slash) |

---

## 🎯 Checklist Completo (Windows)

### Antes de Publicar
- [ ] JDK 21 instalado (`java -version`)
- [ ] JAVA_HOME configurado
- [ ] Framework descargado/clonado
- [ ] Navegar a directorio: `cd qa-scotia-frameworks`
- [ ] Verificar Gradle: `.\gradlew.bat --version`

### Publicar
- [ ] Limpiar: `.\gradlew.bat clean`
- [ ] Publicar: `.\gradlew.bat publishToMavenLocal`
- [ ] Esperar "BUILD SUCCESSFUL"

### Verificar
- [ ] Navegar: `cd $env:USERPROFILE\.m2\repository\com\scotia\qa`
- [ ] Listar JARs: `Get-ChildItem -Recurse -Filter "*.jar"`
- [ ] Verificar 4 carpetas: common, api-core, web-core, mobile-core
- [ ] Cada carpeta tiene 4 archivos: `.jar`, `.pom`, `-sources.jar`, `-javadoc.jar`

### Usar en Módulos
- [ ] Crear módulo: `.\scripts\create-module.sh nombre`
- [ ] Navegar: `cd qa-module-nombre`
- [ ] Probar: `.\gradlew.bat test`

---

## 🚀 Comando Rápido (Todo en Uno)

```powershell
# Copiar y pegar esto en PowerShell:
cd C:\ruta\a\qa-scotia-frameworks; `
.\gradlew.bat clean publishToMavenLocal; `
Write-Host "✅ Publicación completada" -ForegroundColor Green; `
explorer "$env:USERPROFILE\.m2\repository\com\scotia\qa"
```

Esto:
1. Navega al framework
2. Limpia y publica
3. Muestra mensaje de éxito
4. Abre Explorer en Maven Local para verificar

---

## 📚 Referencias

- **OpenJDK 21**: https://learn.microsoft.com/en-us/java/openjdk/download
- **Gradle Docs**: https://docs.gradle.org/current/userguide/userguide.html
- **Maven Local**: https://maven.apache.org/guides/introduction/introduction-to-repositories.html

---

## 📞 Soporte

**Problemas con publicación en Windows**:
- Slack: #qa-automation
- Email: qa-automation@scotia.com
- Contacto: Abel Venero

---

**Última actualización**: Diciembre 2025  
**Versión Framework**: 1.0.0  
**Plataforma**: Windows 10/11

