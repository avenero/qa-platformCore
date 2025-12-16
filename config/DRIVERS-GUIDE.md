# 🚗 Guía de WebDrivers - Scotia QA Framework

## 📋 Índice
- [Estrategia Simplificada](#estrategia-simplificada)
- [Opción 1: LOCAL (Desarrollo)](#opción-1-local-desarrollo)
- [Opción 2: ARTIFACTORY (CI/CD)](#opción-2-artifactory-cicd)
- [Configuración en Módulos](#configuración-en-módulos)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Estrategia Simplificada

El framework soporta **SOLO 2 estrategias** para gestión de WebDrivers:

| **Estrategia** | **Cuándo usar** | **Ventajas** | **Desventajas** |
|----------------|-----------------|--------------|-----------------|
| **LOCAL** | Desarrollo local | ✅ Simple<br>✅ Sin dependencias de red<br>✅ Control total | ❌ Configuración manual<br>❌ Mantenimiento por desarrollador |
| **ARTIFACTORY** | CI/CD, Jenkins | ✅ Centralizado<br>✅ Automático<br>✅ Versionado | ❌ Requiere configuración de credenciales<br>❌ Requiere red |

---

## 🏠 Opción 1: LOCAL (Desarrollo)

### ✅ Cuándo usar
- Desarrollo local en tu máquina
- Ambientes sin acceso a Artifactory
- Ambientes corporativos con firewall estricto
- Cuando quieres control total del driver

### 📦 Paso 1: Descargar el driver

#### **Chrome**
```bash
# Verificar versión de Chrome instalado
# Chrome → Ayuda → Acerca de Chrome → Versión (ej: 143.0.7444.176)

# Descargar desde:
https://googlechromelabs.github.io/chrome-for-testing/
```

#### **Firefox**
```bash
# Descargar desde:
https://github.com/mozilla/geckodriver/releases
```

#### **Edge**
```bash
# Descargar desde:
https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/
```

### 📁 Paso 2: Estructura de directorios

#### **Windows**
```
C:\drivers\
├── chromedriver\
│   └── 143.0.7499.41\
│       └── chromedriver.exe
├── geckodriver\
│   └── 0.33.0\
│       └── geckodriver.exe
└── msedgedriver\
    └── 143.0.2210.55\
        └── msedgedriver.exe
```

#### **Mac/Linux**
```
~/drivers/
├── chromedriver/
│   └── 143.0.7499.41/
│       └── chromedriver
├── geckodriver/
│   └── 0.33.0/
│       └── geckodriver
└── msedgedriver/
    └── 143.0.2210.55/
        └── msedgedriver
```

### ⚙️ Paso 3: Configurar módulo

#### **A) Crear/Editar `.env.local` en el módulo**
```bash
# Mac/Linux
DRIVER_LOCAL_PATH=~/drivers

# Windows
DRIVER_LOCAL_PATH=C:/drivers
```

#### **B) Editar `config-scotia.properties` en el módulo**
```properties
# Estrategia: LOCAL
driver.strategy=local

# Path base de drivers
driver.local.base.path=${DRIVER_LOCAL_PATH}

# Versiones de drivers
driver.chrome.version=143.0.7499.41
driver.firefox.version=0.33.0
driver.edge.version=143.0.2210.55
```

#### **C) Cargar variables de entorno**
```bash
# Mac/Linux
source .env.local

# Windows (PowerShell)
.\scripts\setup-env.ps1
```

### ✅ Paso 4: Ejecutar tests
```bash
# Gradle
./gradlew test

# O desde IntelliJ/Eclipse (Run Configuration)
```

---

## 🏢 Opción 2: ARTIFACTORY (CI/CD)

### ✅ Cuándo usar
- Ejecución en Jenkins/GitLab CI
- Ambientes compartidos (QA, UAT)
- Cuando quieres descarga automática
- Equipos grandes con múltiples proyectos

### 🔐 Paso 1: Obtener credenciales

Contacta a Infra/DevOps para obtener:
- `ARTIFACTORY_BASE_URL`
- `ARTIFACTORY_USER`
- `ARTIFACTORY_TOKEN`

### ⚙️ Paso 2: Configurar módulo

#### **A) Editar `.env.local`**
```bash
ARTIFACTORY_BASE_URL=https://artifactory.cldevops.chl.bns/artifactory/qa-drivers
ARTIFACTORY_USER=tu_usuario
ARTIFACTORY_TOKEN=tu_token_aqui
```

#### **B) Editar `config-scotia.properties`**
```properties
# Estrategia: ARTIFACTORY
driver.strategy=artifactory

# URL y credenciales de Artifactory
driver.artifactory.base.url=${ARTIFACTORY_BASE_URL}
driver.artifactory.user=${ARTIFACTORY_USER}
driver.artifactory.token=${ARTIFACTORY_TOKEN}

# Configuración de reintentos
driver.artifactory.timeout=60
driver.artifactory.retry.enabled=true
driver.artifactory.retry.max=3

# Versiones de drivers
driver.chrome.version=143.0.7499.41
driver.firefox.version=0.33.0
driver.edge.version=143.0.2210.55
```

#### **C) Cargar variables y ejecutar**
```bash
# Mac/Linux
source .env.local
./gradlew test

# Windows
.\scripts\setup-env.ps1
.\gradlew.bat test
```

---

## 🔧 Configuración en Módulos

### 📝 Archivo `config-scotia.properties` completo

```properties
# ============================================================
# DRIVERS - ESTRATEGIA DUAL
# ============================================================

# ELEGIR UNA: 'local' o 'artifactory'
driver.strategy=local

# OPCIÓN 1: LOCAL
driver.local.base.path=${DRIVER_LOCAL_PATH}

# OPCIÓN 2: ARTIFACTORY
driver.artifactory.base.url=${ARTIFACTORY_BASE_URL}
driver.artifactory.user=${ARTIFACTORY_USER}
driver.artifactory.token=${ARTIFACTORY_TOKEN}
driver.artifactory.timeout=60
driver.artifactory.retry.enabled=true
driver.artifactory.retry.max=3

# VERSIONES
driver.chrome.version=143.0.7499.41
driver.firefox.version=0.33.0
driver.edge.version=143.0.2210.55
```

### 📝 Archivo `.env.local` (gitignored)

#### **LOCAL (Mac/Linux)**
```bash
DRIVER_LOCAL_PATH=~/drivers
```

#### **LOCAL (Windows)**
```bash
DRIVER_LOCAL_PATH=C:/drivers
```

#### **ARTIFACTORY**
```bash
ARTIFACTORY_BASE_URL=https://artifactory.cldevops.chl.bns/artifactory/qa-drivers
ARTIFACTORY_USER=tu_usuario
ARTIFACTORY_TOKEN=tu_token
```

---

## 🐛 Troubleshooting

### ❌ Error: "Driver no encontrado en LOCAL PATH"

**Causa:** El driver no existe en la ruta configurada.

**Solución:**
```bash
# 1. Verificar ruta configurada
cat .env.local | grep DRIVER_LOCAL_PATH

# 2. Verificar estructura
# Mac/Linux
ls -la ~/drivers/chromedriver/143.0.7499.41/

# Windows
dir C:\drivers\chromedriver\143.0.7499.41\

# 3. Verificar permisos (Mac/Linux)
chmod +x ~/drivers/chromedriver/143.0.7499.41/chromedriver

# 4. Verificar versión en config-scotia.properties
cat config-scotia.properties | grep driver.chrome.version
```

### ❌ Error: "No se pudo descargar desde Artifactory"

**Causa:** Credenciales incorrectas o sin red.

**Solución:**
```bash
# 1. Verificar credenciales
echo $ARTIFACTORY_USER
echo $ARTIFACTORY_TOKEN  # (verificar que no esté vacío)

# 2. Verificar conectividad
curl -u $ARTIFACTORY_USER:$ARTIFACTORY_TOKEN \
  https://artifactory.cldevops.chl.bns/artifactory/qa-drivers

# 3. Cambiar temporalmente a LOCAL
# En config-scotia.properties:
driver.strategy=local
```

### ❌ Error: "Timeout de 85+ segundos"

**Causa:** Framework intentando descargar desde internet (legacy deshabilitado).

**Solución:**
✅ **Esto es CORRECTO** - El framework ya NO intenta descargar desde internet.

Si ves este error, significa que:
1. Estás usando una versión vieja del framework
2. Actualiza a versión 1.0.0+

### ❌ Error: "Variable de entorno no resuelta"

**Causa:** No cargaste `.env.local`.

**Solución:**
```bash
# Mac/Linux
source .env.local

# Windows
.\scripts\setup-env.ps1

# Verificar variables cargadas
# Mac/Linux
echo $DRIVER_LOCAL_PATH

# Windows
echo $env:DRIVER_LOCAL_PATH
```

---

## 📊 Comparativa de Estrategias

| **Característica** | **LOCAL** | **ARTIFACTORY** |
|-------------------|-----------|-----------------|
| **Setup inicial** | Manual (5-10 min) | Automático (requiere credenciales) |
| **Mantenimiento** | Manual por desarrollador | Centralizado por Infra |
| **Sin internet** | ✅ Funciona | ❌ Requiere red |
| **CI/CD** | ❌ Requiere preconfiguración | ✅ Ideal |
| **Velocidad** | ⚡ Inmediata | 🐌 Primera vez lenta, luego rápida |
| **Versionado** | Manual | Automático |

---

## 🎯 Recomendaciones Finales

### ✅ **Para Desarrollo Local**
```properties
driver.strategy=local
driver.local.base.path=${DRIVER_LOCAL_PATH}
```

### ✅ **Para CI/CD (Jenkins, GitLab)**
```properties
driver.strategy=artifactory
driver.artifactory.base.url=${ARTIFACTORY_BASE_URL}
```

### ✅ **Para Ambientes Corporativos con Firewall**
```properties
# Siempre LOCAL o ARTIFACTORY
# NUNCA depender de descarga desde internet
driver.strategy=local
```

---

## 📞 Soporte

- **Documentación:** `/documentacion/FRAMEWORK-GUIDE.md`
- **Issues:** Contactar al equipo de QA
- **Drivers:** [Chrome for Testing](https://googlechromelabs.github.io/chrome-for-testing/)

