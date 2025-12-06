# 📦 Instrucciones para Publicar WebDrivers en Artifactory

**Destinatario**: Equipo de Infraestructura  
**Fecha**: Diciembre 2025  
**Solicitante**: Equipo QA Automation  
**Framework**: Scotia QA Framework v1.0.0

---

## 🎯 Objetivo

Publicar los WebDrivers (chromedriver, geckodriver, edgedriver) en Artifactory para que los módulos de prueba automatizada puedan descargarlos automáticamente, evitando dependencias de URLs públicas bloqueadas.

---

## 📍 Estructura Requerida en Artifactory

### Repositorio Base

```
artifactory.scotia.com/artifactory/qa-drivers/
```

### Estructura de Directorios

```
qa-drivers/
├── chromedriver/
│   ├── 114.0.5735.90/
│   │   ├── linux64/
│   │   │   └── chromedriver.zip
│   │   ├── mac64/
│   │   │   └── chromedriver.zip
│   │   ├── mac_arm64/
│   │   │   └── chromedriver.zip
│   │   └── win32/
│   │       └── chromedriver.zip
│   └── 115.0.5790.98/
│       └── ... (misma estructura)
│
├── geckodriver/
│   └── 0.33.0/
│       ├── linux64/
│       │   └── geckodriver.zip
│       ├── mac64/
│       │   └── geckodriver.zip
│       ├── mac_arm64/
│       │   └── geckodriver.zip
│       └── win32/
│           └── geckodriver.zip
│
└── edgedriver/
    └── 114.0.1823.37/
        ├── linux64/
        │   └── msedgedriver.zip
        ├── mac64/
        │   └── msedgedriver.zip
        └── win32/
            └── msedgedriver.zip
```

---

## 📥 Drivers a Publicar

### 1. ChromeDriver

**Versiones requeridas**:
- `114.0.5735.90` (actual, para Chrome 114)
- `115.0.5790.98` (opcional, para Chrome 115)

**URLs de descarga oficiales**:

```bash
# Linux 64-bit
https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_linux64.zip

# macOS Intel (64-bit)
https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_mac64.zip

# macOS Apple Silicon (ARM64)
https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_mac_arm64.zip

# Windows 32-bit
https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_win32.zip
```

**Nombre del ejecutable dentro del zip**:
- Linux/Mac: `chromedriver`
- Windows: `chromedriver.exe`

---

### 2. GeckoDriver (Firefox)

**Versión requerida**:
- `0.33.0` (compatible con Firefox 102+)

**URLs de descarga oficiales**:

```bash
# Linux 64-bit
https://github.com/mozilla/geckodriver/releases/download/v0.33.0/geckodriver-v0.33.0-linux64.tar.gz

# macOS (Intel + ARM)
https://github.com/mozilla/geckodriver/releases/download/v0.33.0/geckodriver-v0.33.0-macos.tar.gz

# Windows 64-bit
https://github.com/mozilla/geckodriver/releases/download/v0.33.0/geckodriver-v0.33.0-win64.zip
```

**⚠️ IMPORTANTE**: GeckoDriver viene en `.tar.gz` para Linux/Mac. Convertir a `.zip` antes de subir.

**Nombre del ejecutable**:
- Linux/Mac: `geckodriver`
- Windows: `geckodriver.exe`

---

### 3. EdgeDriver (Microsoft Edge)

**Versión requerida**:
- `114.0.1823.37` (compatible con Edge 114)

**URLs de descarga oficiales**:

```bash
# Windows 64-bit
https://msedgedriver.azureedge.net/114.0.1823.37/edgedriver_win64.zip

# Linux 64-bit
https://msedgedriver.azureedge.net/114.0.1823.37/edgedriver_linux64.zip

# macOS Intel
https://msedgedriver.azureedge.net/114.0.1823.37/edgedriver_mac64.zip

# macOS ARM64
https://msedgedriver.azureedge.net/114.0.1823.37/edgedriver_mac64_m1.zip
```

**Nombre del ejecutable**:
- Linux/Mac: `msedgedriver`
- Windows: `msedgedriver.exe`

---

## 🛠️ Script de Descarga y Preparación

### Para Bash (Linux/macOS)

```bash
#!/bin/bash
# download-drivers.sh - Descargar drivers oficiales

DRIVERS_DIR="drivers-temp"
mkdir -p "$DRIVERS_DIR"
cd "$DRIVERS_DIR"

echo "🔽 Descargando ChromeDriver 114.0.5735.90..."
mkdir -p chromedriver/114.0.5735.90/{linux64,mac64,mac_arm64,win32}

curl -o chromedriver/114.0.5735.90/linux64/chromedriver.zip \
  https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_linux64.zip

curl -o chromedriver/114.0.5735.90/mac64/chromedriver.zip \
  https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_mac64.zip

curl -o chromedriver/114.0.5735.90/mac_arm64/chromedriver.zip \
  https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_mac_arm64.zip

curl -o chromedriver/114.0.5735.90/win32/chromedriver.zip \
  https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_win32.zip

echo "🔽 Descargando GeckoDriver 0.33.0..."
mkdir -p geckodriver/0.33.0/{linux64,mac64,mac_arm64,win32}

# Linux - convertir tar.gz a zip
curl -L -o /tmp/gecko-linux.tar.gz \
  https://github.com/mozilla/geckodriver/releases/download/v0.33.0/geckodriver-v0.33.0-linux64.tar.gz
tar -xzf /tmp/gecko-linux.tar.gz -C /tmp/
zip -j geckodriver/0.33.0/linux64/geckodriver.zip /tmp/geckodriver

# macOS - convertir tar.gz a zip
curl -L -o /tmp/gecko-mac.tar.gz \
  https://github.com/mozilla/geckodriver/releases/download/v0.33.0/geckodriver-v0.33.0-macos.tar.gz
tar -xzf /tmp/gecko-mac.tar.gz -C /tmp/
zip -j geckodriver/0.33.0/mac64/geckodriver.zip /tmp/geckodriver
cp geckodriver/0.33.0/mac64/geckodriver.zip geckodriver/0.33.0/mac_arm64/

# Windows - ya viene en zip
curl -L -o geckodriver/0.33.0/win32/geckodriver.zip \
  https://github.com/mozilla/geckodriver/releases/download/v0.33.0/geckodriver-v0.33.0-win64.zip

echo "🔽 Descargando EdgeDriver 114.0.1823.37..."
mkdir -p edgedriver/114.0.1823.37/{linux64,mac64,mac_arm64,win32}

curl -o edgedriver/114.0.1823.37/linux64/msedgedriver.zip \
  https://msedgedriver.azureedge.net/114.0.1823.37/edgedriver_linux64.zip

curl -o edgedriver/114.0.1823.37/mac64/msedgedriver.zip \
  https://msedgedriver.azureedge.net/114.0.1823.37/edgedriver_mac64.zip

curl -o edgedriver/114.0.1823.37/mac_arm64/msedgedriver.zip \
  https://msedgedriver.azureedge.net/114.0.1823.37/edgedriver_mac64_m1.zip

curl -o edgedriver/114.0.1823.37/win32/msedgedriver.zip \
  https://msedgedriver.azureedge.net/114.0.1823.37/edgedriver_win64.zip

echo "✅ Descarga completa. Estructura lista para subir a Artifactory."
tree chromedriver geckodriver edgedriver
```

---

## 📤 Subida a Artifactory

### Opción 1: Usando cURL (Manual)

```bash
#!/bin/bash
# upload-to-artifactory.sh

ARTIFACTORY_URL="https://artifactory.scotia.com/artifactory/qa-drivers"
ARTIFACTORY_USER="tu_usuario"
ARTIFACTORY_TOKEN="tu_token"

# Subir ChromeDriver
for os in linux64 mac64 mac_arm64 win32; do
  curl -u "$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN" \
    -X PUT "$ARTIFACTORY_URL/chromedriver/114.0.5735.90/$os/chromedriver.zip" \
    -T "chromedriver/114.0.5735.90/$os/chromedriver.zip"
done

# Subir GeckoDriver
for os in linux64 mac64 mac_arm64 win32; do
  curl -u "$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN" \
    -X PUT "$ARTIFACTORY_URL/geckodriver/0.33.0/$os/geckodriver.zip" \
    -T "geckodriver/0.33.0/$os/geckodriver.zip"
done

# Subir EdgeDriver
for os in linux64 mac64 mac_arm64 win32; do
  curl -u "$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN" \
    -X PUT "$ARTIFACTORY_URL/edgedriver/114.0.1823.37/$os/msedgedriver.zip" \
    -T "edgedriver/114.0.1823.37/$os/msedgedriver.zip"
done

echo "✅ Drivers subidos a Artifactory"
```

### Opción 2: Usando JFrog CLI (Recomendado)

```bash
# Instalar JFrog CLI (si no está instalado)
brew install jfrog-cli  # macOS
# o descargar desde: https://jfrog.com/getcli/

# Configurar servidor
jfrog config add artifactory-scotia \
  --artifactory-url=https://artifactory.scotia.com/artifactory \
  --user=tu_usuario \
  --access-token=tu_token

# Subir todos los drivers recursivamente
jfrog rt upload "chromedriver/*" qa-drivers/chromedriver/ --recursive
jfrog rt upload "geckodriver/*" qa-drivers/geckodriver/ --recursive
jfrog rt upload "edgedriver/*" qa-drivers/edgedriver/ --recursive

echo "✅ Drivers subidos con JFrog CLI"
```

---

## ✅ Validación Post-Publicación

### Verificar URLs Públicas

Probar que los drivers son accesibles:

```bash
# ChromeDriver Linux
curl -I https://artifactory.scotia.com/artifactory/qa-drivers/chromedriver/114.0.5735.90/linux64/chromedriver.zip

# GeckoDriver Windows
curl -I https://artifactory.scotia.com/artifactory/qa-drivers/geckodriver/0.33.0/win32/geckodriver.zip

# EdgeDriver macOS ARM
curl -I https://artifactory.scotia.com/artifactory/qa-drivers/edgedriver/114.0.1823.37/mac_arm64/msedgedriver.zip
```

**Código esperado**: `HTTP/1.1 200 OK`

### Probar Descarga con Autenticación

```bash
curl -u "test_user:test_token" \
  -o /tmp/test-chromedriver.zip \
  https://artifactory.scotia.com/artifactory/qa-drivers/chromedriver/114.0.5735.90/linux64/chromedriver.zip

unzip -l /tmp/test-chromedriver.zip
# Debe mostrar: chromedriver (ejecutable)
```

---

## 🔐 Permisos y Acceso

### Usuarios que Necesitan Acceso

- **Equipo QA Automation** (lectura)
- **Desarrolladores** (lectura)
- **CI/CD (Jenkins)** (lectura)
- **Equipo Infra** (escritura)

### Nivel de Permisos

- **Lectura**: Todos los usuarios autenticados del dominio Scotia
- **Escritura**: Solo equipo de Infra y administradores de Artifactory

### Configuración de Repositorio

```yaml
# artifactory-repo-config.yaml (sugerido)
key: qa-drivers
packageType: generic
description: "WebDrivers para Scotia QA Framework"
repoLayoutRef: simple-default
dockerApiVersion: V2
handleReleases: true
handleSnapshots: false
suppressPomConsistencyChecks: false
maxUniqueSnapshots: 0
blackedOut: false
xrayIndex: false
propertySets: []
archiveBrowsingEnabled: false
calculateYumMetadata: false
yumRootDepth: 0
cdnRedirect: false
```

---

## 📋 Checklist para Infra

- [ ] Crear repositorio `qa-drivers` en Artifactory (si no existe)
- [ ] Configurar permisos (lectura pública, escritura restringida)
- [ ] Descargar drivers oficiales usando `download-drivers.sh`
- [ ] Verificar que los zips contienen los ejecutables correctos
- [ ] Subir drivers a Artifactory usando `upload-to-artifactory.sh` o JFrog CLI
- [ ] Validar URLs públicas (sin autenticación) retornan 401 o acceso con credenciales
- [ ] Validar descarga con usuario de prueba
- [ ] Proveer credenciales de solo-lectura al equipo QA:
  - `ARTIFACTORY_USER`: `qa-automation-reader`
  - `ARTIFACTORY_TOKEN`: [generar token]
- [ ] Documentar proceso de actualización de versiones futuras
- [ ] Notificar a equipo QA que drivers están disponibles

---

## 🔄 Proceso de Actualización (Versiones Futuras)

Cuando salga una nueva versión de Chrome/Firefox/Edge:

1. Descargar nueva versión desde URLs oficiales
2. Crear estructura de directorios: `{driver}/{version}/{os}/`
3. Subir zips a Artifactory
4. Notificar a equipo QA para actualizar `config-scotia.properties`:
   ```properties
   driver.chrome.version=NUEVA_VERSION
   ```
5. Los módulos descargarán automáticamente la nueva versión en próxima ejecución

---

## 📞 Contacto

**Equipo QA Automation**  
Email: qa-automation@scotia.com  
Slack: #qa-automation  

**Persona de Contacto**: Abel Venero  
Email: abel.venero@scotia.com

---

## 📚 Referencias

- **ChromeDriver**: https://chromedriver.chromium.org/downloads
- **GeckoDriver**: https://github.com/mozilla/geckodriver/releases
- **EdgeDriver**: https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/
- **JFrog CLI**: https://jfrog.com/getcli/
- **Artifactory REST API**: https://www.jfrog.com/confluence/display/JFROG/Artifactory+REST+API

---

**Generado por**: Scotia QA Framework  
**Fecha**: Diciembre 2025  
**Versión**: 1.0.0

