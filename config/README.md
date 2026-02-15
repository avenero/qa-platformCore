# 📖 Guía de Configuración - Scotia QA Framework

> **Framework**: Scotia QA Framework v1.0.0  
> **Última actualización**: Diciembre 16, 2025  
> **Autor**: Abel Venero

---

## 📑 Índice

1. [Introducción](#-introducción)
2. [Configuración de Módulos](#-configuración-de-módulos)
3. [Gestión de WebDrivers](#-gestión-de-webdrivers)
4. [Gradle: Dependencias y Repositorios](#-gradle-dependencias-y-repositorios)
5. [Integración con Artifactory](#-integración-con-artifactory)
6. [Certificados SSL](#-certificados-ssl)
7. [Publicación en Maven Local](#-publicación-en-maven-local)
8. [Troubleshooting](#-troubleshooting)
9. [Cambios Recientes](#-cambios-recientes)

---

## 🎯 Introducción

Este directorio contiene **toda la configuración necesaria** para trabajar con el Scotia QA Framework:

- ✅ **Templates** de configuración para módulos
- ✅ **Guías** de integración con Artifactory
- ✅ **Instrucciones** para gestión de WebDrivers
- ✅ **Configuración** de Gradle y dependencias
- ✅ **Solución** de problemas comunes

---

## 📦 Configuración de Módulos

### 🔧 Archivos de Configuración

Cada módulo de prueba necesita **2 archivos**:

#### 1. `config-scotia.properties`

**Ubicación**: `src/test/resources/config-scotia.properties`

**Plantilla**: [`templates/config-scotia.properties.template`](templates/config-scotia.properties.template)

**Configuraciones principales**:

```properties
# ============================================================
# AMBIENTE
# ============================================================
test.env=qa

# ============================================================
# WEB (si usas web-core)
# ============================================================
web.base.url=https://qa.your-app.com
web.browser=chrome
web.headless=false
web.timeout.implicit=10
web.timeout.explicit=30

# ============================================================
# API (si usas api-core)
# ============================================================
api.base.url=https://api-qa.your-app.com/v1
api.timeout=30

# ============================================================
# MOBILE (si usas mobile-core)
# ============================================================
mobile.platform=android
mobile.device.name=emulator-5554

# ============================================================
# WEBDRIVERS - Estrategia LOCAL
# ============================================================
driver.strategy=local
driver.local.base.path=${DRIVER_LOCAL_PATH}
driver.chrome.version=143.0.7499.41
driver.firefox.version=0.34.0
driver.edge.version=143.0.2357.81

# ============================================================
# WEBDRIVERS - Estrategia ARTIFACTORY (alternativa)
# ============================================================
# driver.strategy=artifactory
# driver.artifactory.base.url=${ARTIFACTORY_BASE_URL}
# driver.artifactory.user=${ARTIFACTORY_USER}
# driver.artifactory.token=${ARTIFACTORY_TOKEN}

# ============================================================
# BASE DE DATOS
# ============================================================
db.url=${DB_URL}
db.username=${DB_USER}
db.password=${DB_PASS}
db.driver=oracle.jdbc.OracleDriver
db.pool.size=10

# ============================================================
# REPORTING (Extent Reports + Jira/Xray)
# ============================================================
reporting.enabled=true
reporting.output.dir=target/reports

# Extent Reports
extent.enabled=true
extent.outputPath=build/reports/extent/
extent.reportName=execution-report.html
extent.theme=STANDARD                    # STANDARD o DARK

# Jira/Xray Integration
jira.enabled=true
jira.url=${JIRA_URL}
jira.user=${JIRA_USER}
jira.password=${JIRA_PASSWORD}

# Test Execution Strategy
jira.projectKey=QAAUY
jira.testExecutionId=${TEST_EXECUTION_ID}  # ej: QAAUY-640 (pre-existente)
jira.autoCreateExecution=false             # true = crear automáticamente

# Control Granular
jira.updateStatus=true                     # ¿Actualizar PASS/FAIL?
jira.uploadReport=true                     # ¿Subir HTML?
jira.includeEvidences=true                 # ¿Adjuntar screenshots?
jira.maxAttachmentSizeMb=10
jira.failOnError=false                     # Continuar si Jira falla
jira.updateMode=BATCH                      # SINGLE o BATCH
jira.testEnvironment=QA
```

---

### 📋 Jira/Xray: Estrategias de Test Execution

El framework soporta **2 estrategias** para gestionar Test Executions:

#### **Estrategia 1: Test Execution PRE-EXISTENTE** (Recomendada)

```properties
jira.autoCreateExecution=false
jira.testExecutionId=QAAUY-640  # Ya existe en Jira
```

**Flujo:**
1. ✅ Creas manualmente un Test Execution en Jira (QAAUY-640)
2. ✅ Asocias tests al execution (QAAUY-123, QAAUY-124...)
3. ✅ Ejecutas tests → El framework actualiza status automáticamente

**Ventajas:**
- Control total sobre qué tests incluir
- No requiere permisos de creación de issues
- Ideal para sprints planificados

---

#### **Estrategia 2: AUTO-CREAR Test Execution** (Automática)

```properties
jira.autoCreateExecution=true
jira.projectKey=QAAUY
jira.testEnvironment=QA
# No necesitas testExecutionId
```

**Flujo:**
1. ❌ **NO** proporcionas `testExecutionId`
2. ✅ El framework **crea automáticamente** un Test Execution:
   - Summary: "Automated Test Execution - 2025-12-19 15:30"
   - Tests incluidos: Todos los del `cucumber.json`
3. ✅ Execution ID se loguea para futuras referencias

**Ventajas:**
- Totalmente automático (ideal para CI/CD)
- No requiere preparación manual

**Desventajas:**
- Requiere permisos de creación de issues
- Crea un nuevo execution cada ejecución

---

### 🏷️ Tags de Cucumber para Jira

**Ejemplo de feature:**
```gherkin
@QAAUY-123 @smoke @web
Scenario: Login exitoso
  Given usuario ingresa credenciales válidas
  When hace clic en Login
  Then debería ver el dashboard
```

**Funcionamiento:**
- El framework busca tags con pattern: `@([A-Z]{2,10}-\\d+)`
- En este caso: `@QAAUY-123` → Este es el **Test ID en Jira**
- ❌ Sin tag válido = No se reporta a Jira

---

### 📊 Modos de Actualización Jira

#### **BATCH Mode** (Recomendado)
```properties
jira.updateMode=BATCH
```
- Envía todos los tests en **un solo request**
- Más rápido
- Si falla, afecta todos los tests

#### **SINGLE Mode**
```properties
jira.updateMode=SINGLE
```
- Envía cada test en un **request separado**
- Más lento
- Tolerante a fallos (un test no afecta otros)

---

### 🔧 Configuración por Caso de Uso

#### **Desarrollo Local (Solo HTML)**
```properties
jira.updateStatus=false
jira.uploadReport=false
extent.enabled=true
```

#### **CI/CD con Execution Pre-creado**
```properties
jira.updateStatus=true
jira.uploadReport=true
jira.autoCreateExecution=false
jira.testExecutionId=${TEST_EXECUTION_ID}  # Variable de Jenkins
```

#### **CI/CD Totalmente Automático**
```properties
jira.updateStatus=true
jira.uploadReport=true
jira.autoCreateExecution=true
jira.projectKey=QAAUY
jira.testEnvironment=${ENV}  # Variable de Jenkins
```

**📚 Más información**: Ver `/common/src/main/java/com/scotia/qa/common/reporting/README.md`
```

---

#### 2. `.env.local`

**Ubicación**: `<raíz-módulo>/.env.local`

**Plantilla**: [`templates/.env.local.template`](templates/.env.local.template)

**Variables sensibles**:

```bash
# ====================================================================
# AMBIENTE
# ====================================================================
TEST_ENV=qa

# ====================================================================
# WEB
# ====================================================================
WEB_BASE_URL=https://qa.your-app.com

# ====================================================================
# API
# ====================================================================
API_BASE_URL=https://api-qa.your-app.com/v1
API_TOKEN=your_token_here

# ====================================================================
# WEBDRIVERS - ESTRATEGIA LOCAL
# ====================================================================
# macOS/Linux:
DRIVER_LOCAL_PATH=/Users/tu_usuario/drivers

# Windows:
# DRIVER_LOCAL_PATH=C:/drivers

# ====================================================================
# WEBDRIVERS - ESTRATEGIA ARTIFACTORY
# ====================================================================
ARTIFACTORY_BASE_URL=https://artifactory.corp.com/qa-drivers
ARTIFACTORY_USER=qa-automation-reader
ARTIFACTORY_TOKEN=your_artifactory_token

# ====================================================================
# BASE DE DATOS
# ====================================================================
DB_URL=jdbc:oracle:thin:@//qa-db:1521/Banking
DB_USER=qa_user
DB_PASS=qa_password

# ====================================================================
# JIRA/XRAY
# ====================================================================
JIRA_URL=https://jira.your-company.com
JIRA_USER=automation_user
JIRA_PASSWORD=automation_password
```

---

### 📝 Pasos para Configurar un Módulo

1. **Copiar templates**:
   ```bash
   # Desde el framework
   cp config/templates/config-scotia.properties.template \
      /path/to/tu-modulo/src/test/resources/config-scotia.properties
   
   cp config/templates/.env.local.template \
      /path/to/tu-modulo/.env.local
   ```

2. **Editar valores**:
   - `config-scotia.properties`: URLs, timeouts, configuraciones generales
   - `.env.local`: Credenciales, tokens, datos sensibles

3. **Cargar variables de entorno**:
   ```bash
   # macOS/Linux
   source .env.local
   
   # Windows PowerShell
   .\scripts\setup-env.ps1
   ```

4. **Verificar configuración**:
   ```bash
   ./gradlew test --dry-run
   ```

---

## 🚗 Gestión de WebDrivers

### 📌 Dos Estrategias Soportadas

El framework soporta **2 estrategias** para gestionar WebDrivers:

#### **Estrategia 1: LOCAL** ⭐ (Recomendada para desarrollo)

**Ventajas**:
- ✅ Control total de versiones
- ✅ Funciona offline
- ✅ No depende de red corporativa

**Configuración**:

```properties
# config-scotia.properties
driver.strategy=local
driver.local.base.path=${DRIVER_LOCAL_PATH}
driver.chrome.version=143.0.7499.41
```

```bash
# .env.local
DRIVER_LOCAL_PATH=/Users/tu_usuario/drivers  # macOS/Linux
# DRIVER_LOCAL_PATH=C:/drivers  # Windows
```

**Estructura de directorios**:

```
/Users/tu_usuario/drivers/   (o C:/drivers/ en Windows)
├── chromedriver/
│   └── 143.0.7499.41/
│       └── chromedriver       (.exe en Windows)
├── geckodriver/
│   └── 0.34.0/
│       └── geckodriver
└── msedgedriver/
    └── 143.0.2357.81/
        └── msedgedriver
```

---

#### 🧠 ¿Por qué esta estructura? Path Base + Versión

El framework usa **3 niveles** de jerarquía:

```
{BASE_PATH} / {DRIVER_NAME} / {VERSION} / {EXECUTABLE}
     ↓             ↓              ↓            ↓
C:/drivers / chromedriver / 143.0.7499.41 / chromedriver.exe
```

**Razones del diseño**:

| **Ventaja** | **Descripción** |
|-------------|-----------------|
| ✅ **Múltiples versiones simultáneas** | Proyectos diferentes pueden usar versiones distintas sin conflicto |
| ✅ **Reutilización entre módulos** | Una sola variable `DRIVER_LOCAL_PATH` compartida por todos los proyectos |
| ✅ **Switch fácil de versión** | Solo cambias `driver.chrome.version` en config, no el path completo |
| ✅ **Mantenimiento centralizado** | Drivers en una sola ubicación, fácil de actualizar |
| ✅ **Ahorro de espacio** | No duplicar drivers por proyecto |
| ✅ **CI/CD friendly** | Misma estructura en todas las máquinas |

**Ejemplo real**: 3 proyectos en tu máquina:

```
qa-module-banking/     → Chrome 143.0.7499.41
qa-module-autos/       → Chrome 143.0.7499.41
qa-module-ecommerce/   → Chrome 142.0.7444.176 (versión anterior)

# Todos comparten:
DRIVER_LOCAL_PATH=C:/drivers

# Cada uno define su versión en config-scotia.properties:
# banking:  driver.chrome.version=143.0.7499.41
# autos:    driver.chrome.version=143.0.7499.41
# ecommerce: driver.chrome.version=142.0.7444.176
```

---

#### 🔄 Estrategias de búsqueda soportadas

El framework es **flexible** y busca en **3 estructuras diferentes** (en orden):

**1. Estructura versionada** (RECOMENDADA):
```
C:/drivers/chromedriver/143.0.7499.41/chromedriver.exe
```

**2. Carpeta del driver** (sin versión):
```
C:/drivers/chromedriver/chromedriver.exe
```

**3. Estructura plana** (directa):
```
C:/drivers/chromedriver.exe
```

**¿Cuál usar?**
- **Opción 1**: Si tienes múltiples proyectos o versiones
- **Opción 2**: Si solo usas 1 versión y la actualizas in-place
- **Opción 3**: Setup rápido para pruebas simples

---

#### 🎯 Alternativa: Path directo al ejecutable

Si prefieres definir la ruta completa sin usar la estructura recomendada:

```bash
# Ejecutar con System Property
./gradlew test -Dwebdriver.chrome.driver=C:/mi-carpeta/chromedriver.exe

# Windows PowerShell
.\gradlew.bat test -Dwebdriver.chrome.driver=C:/mi-carpeta/chromedriver.exe
```

El framework **detecta automáticamente** el System Property y **lo prioriza** sobre la búsqueda en path base.

**⚠️ Nota**: Esta opción pierde las ventajas de reutilización y mantenimiento centralizado.

**Descargas oficiales**:
- **ChromeDriver**: https://googlechromelabs.github.io/chrome-for-testing/
- **GeckoDriver (Firefox)**: https://github.com/mozilla/geckodriver/releases
- **EdgeDriver**: https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/

---

#### **Estrategia 2: ARTIFACTORY** (Para CI/CD)

**Ventajas**:
- ✅ Centralizado
- ✅ Controlado por Infra
- ✅ Caché automático

**Configuración**:

```properties
# config-scotia.properties
driver.strategy=artifactory
driver.artifactory.base.url=${ARTIFACTORY_BASE_URL}
driver.artifactory.user=${ARTIFACTORY_USER}
driver.artifactory.token=${ARTIFACTORY_TOKEN}
driver.chrome.version=143.0.7499.41
```

```bash
# .env.local
ARTIFACTORY_BASE_URL=https://artifactory.corp.com/artifactory/qa-drivers
ARTIFACTORY_USER=qa-automation-reader
ARTIFACTORY_TOKEN=your_token_here
```

**Estructura esperada en Artifactory**:

```
qa-drivers/
├── chromedriver/
│   └── 143.0.7499.41/
│       ├── mac64/chromedriver.zip
│       ├── mac_arm64/chromedriver.zip
│       ├── linux64/chromedriver.zip
│       └── win32/chromedriver.zip
├── geckodriver/
│   └── 0.34.0/
│       ├── mac64/geckodriver.zip
│       └── ...
└── msedgedriver/
    └── 143.0.2357.81/
        └── ...
```

**Caché local**: `~/.qa-drivers/` (o `%USERPROFILE%\.qa-drivers\` en Windows)

---

### 🔧 Troubleshooting de Drivers

#### ❌ Error: "chromedriver no encontrado"

**Causa**: Path incorrecto o driver no existe.

**Solución**:
1. Verificar que `DRIVER_LOCAL_PATH` está configurado:
   ```bash
   echo $DRIVER_LOCAL_PATH  # macOS/Linux
   echo %DRIVER_LOCAL_PATH%  # Windows CMD
   echo $env:DRIVER_LOCAL_PATH  # Windows PowerShell
   ```

2. Verificar estructura de directorios:
   ```bash
   ls -la $DRIVER_LOCAL_PATH/chromedriver/143.0.7499.41/
   # Debe mostrar el ejecutable
   ```

3. Verificar permisos de ejecución (macOS/Linux):
   ```bash
   chmod +x $DRIVER_LOCAL_PATH/chromedriver/143.0.7499.41/chromedriver
   ```

---

#### ❌ Error: "Estrategia 'fallback' no válida"

**Causa**: Artefactos viejos del framework en `mavenLocal`.

**Solución**:
```bash
# macOS/Linux
rm -rf ~/.m2/repository/com/scotia/qa/

# Windows PowerShell
Remove-Item -Recurse -Force $env:USERPROFILE\.m2\repository\com\scotia\qa\

# Actualizar dependencias
./gradlew clean --refresh-dependencies
```

---

#### ❌ Error: "Version mismatch"

**Causa**: Versión de Chrome instalada no coincide con chromedriver.

**Solución**:
1. Verificar versión de Chrome instalado:
   ```bash
   # macOS
   /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --version
   
   # Windows
   reg query "HKEY_CURRENT_USER\Software\Google\Chrome\BLBeacon" /v version
   ```

2. Descargar chromedriver de la **misma versión mayor**:
   - Chrome 143.x.x.x → chromedriver 143.x.x.x

---

## 📦 Gradle: Dependencias y Repositorios

### 🔑 Conceptos Clave

| **Bloque** | **Propósito** | **Configuración** |
|------------|---------------|-------------------|
| `repositories` | **DESCARGAR** dependencias | mavenLocal → Artifactory → mavenCentral |
| `publishing.repositories` | **PUBLICAR** artefactos | mavenLocal (único) |

---

### 📥 Descargar Dependencias

**En `build.gradle` del módulo**:

```groovy
repositories {
    // Switch entre mavenLocal (desarrollo) y Artifactory (CI/CD)
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

**Uso**:

```bash
# Desarrollo (usa mavenLocal primero)
./gradlew build

# CI/CD (usa Artifactory primero)
./gradlew build -PuseArtifactory=true
```

---

### 📤 Publicar Artefactos

**Solo para el framework**:

```bash
cd /path/to/qa-scotia-frameworks
./gradlew publishToMavenLocal
```

**Resultado**:
```
~/.m2/repository/com/scotia/qa/
├── common/1.0.0/
│   ├── common-1.0.0.jar
│   ├── common-1.0.0.pom
│   ├── common-1.0.0-sources.jar
│   └── common-1.0.0-javadoc.jar
├── api-core/1.0.0/
├── web-core/1.0.0/
└── mobile-core/1.0.0/
```

**⚠️ Nota**: Los módulos de prueba **NO** publican artefactos, solo consumen.

---

## 🏢 Integración con Artifactory

### 📋 Para QA (Usar Artifactory)

#### **Configuración**:

```properties
# config-scotia.properties
driver.strategy=artifactory
driver.artifactory.base.url=${ARTIFACTORY_BASE_URL}
driver.artifactory.user=${ARTIFACTORY_USER}
driver.artifactory.token=${ARTIFACTORY_TOKEN}
driver.chrome.version=143.0.7499.41
```

```bash
# .env.local
ARTIFACTORY_BASE_URL=https://artifactory.cldevops.chl.bns/artifactory/qa-drivers
ARTIFACTORY_USER=qa-automation-reader
ARTIFACTORY_TOKEN=xxxxxxxxxxxxx
```

#### **Flujo de ejecución**:

1. **Primera vez**: Descarga desde Artifactory → Guarda en `~/.qa-drivers/`
2. **Siguientes veces**: Usa caché local (instantáneo)

#### **Limpiar caché**:

```bash
# macOS/Linux
rm -rf ~/.qa-drivers/

# Windows
Remove-Item -Recurse -Force $env:USERPROFILE\.qa-drivers\
```

---

### 🛠️ Para Infra (Publicar en Artifactory)

#### **Estructura requerida**:

```
qa-drivers/
├── chromedriver/{version}/{os}/chromedriver.zip
├── geckodriver/{version}/{os}/geckodriver.zip
└── msedgedriver/{version}/{os}/msedgedriver.zip
```

**OS soportados**: `mac64`, `mac_arm64`, `linux64`, `win32`, `win64`

#### **Script de descarga y preparación**:

```bash
#!/bin/bash
# download-drivers.sh

VERSION="143.0.7499.41"
DRIVER="chromedriver"

# Descargar de URLs oficiales
wget https://storage.googleapis.com/chrome-for-testing-public/${VERSION}/mac-x64/${DRIVER}.zip \
  -O ${DRIVER}-mac64.zip

wget https://storage.googleapis.com/chrome-for-testing-public/${VERSION}/mac-arm64/${DRIVER}.zip \
  -O ${DRIVER}-mac_arm64.zip

wget https://storage.googleapis.com/chrome-for-testing-public/${VERSION}/win32/${DRIVER}.zip \
  -O ${DRIVER}-win32.zip

# Subir a Artifactory
jfrog rt upload "${DRIVER}-mac64.zip" \
  "qa-drivers/${DRIVER}/${VERSION}/mac64/${DRIVER}.zip"

jfrog rt upload "${DRIVER}-mac_arm64.zip" \
  "qa-drivers/${DRIVER}/${VERSION}/mac_arm64/${DRIVER}.zip"

jfrog rt upload "${DRIVER}-win32.zip" \
  "qa-drivers/${DRIVER}/${VERSION}/win32/${DRIVER}.zip"
```

#### **Credenciales de solo-lectura para QA**:

```bash
# Usuario: qa-automation-reader
# Permisos: Solo lectura en qa-drivers/
# Token: Generar en Artifactory UI
```

---

## 🔐 Certificados SSL

### 📌 Problema

Artifactory corporativo usa certificados SSL personalizados que pueden no ser confiables por Java.

**Error típico**:
```
PKIX path building failed: unable to find valid certification path to requested target
```

---

### ✅ Solución: Importar Certificado al JVM

#### **Paso 1: Obtener certificado**

```bash
# Descargar certificado desde browser o servidor
openssl s_client -connect artifactory.cldevops.chl.bns:443 -showcerts \
  < /dev/null 2>/dev/null | openssl x509 -outform PEM > artifactory.crt
```

#### **Paso 2: Importar a Java KeyStore**

```bash
# macOS/Linux
sudo keytool -import -trustcacerts -alias artifactory \
  -file artifactory.crt \
  -keystore $JAVA_HOME/lib/security/cacerts \
  -storepass changeit

# Windows (como Administrador)
keytool -import -trustcacerts -alias artifactory ^
  -file artifactory.crt ^
  -keystore "%JAVA_HOME%\lib\security\cacerts" ^
  -storepass changeit
```

#### **Paso 3: Verificar**

```bash
keytool -list -keystore $JAVA_HOME/lib/security/cacerts -alias artifactory -storepass changeit
```

---

### 🔧 Alternativa: TrustStore Personalizado

Si no tienes permisos de administrador:

1. **Crear TrustStore propio**:
   ```bash
   keytool -import -trustcacerts -alias artifactory \
     -file artifactory.crt \
     -keystore ~/myTrustStore.jks \
     -storepass changeit
   ```

2. **Configurar Gradle**:
   ```bash
   # En gradle.properties del módulo
   org.gradle.jvmargs=-Djavax.net.ssl.trustStore=/path/to/myTrustStore.jks \
                      -Djavax.net.ssl.trustStorePassword=changeit
   ```

---

## 📦 Publicación en Maven Local

### 🔧 Para Desarrolladores del Framework

#### **Publicar todas las capas**:

```bash
cd /path/to/qa-scotia-frameworks
./gradlew clean build -x test
./gradlew publishToMavenLocal
```

#### **Publicar capa específica**:

```bash
./gradlew :common:publishToMavenLocal
./gradlew :api-core:publishToMavenLocal
./gradlew :web-core:publishToMavenLocal
./gradlew :mobile-core:publishToMavenLocal
```

#### **Verificar publicación**:

```bash
# macOS/Linux
ls -la ~/.m2/repository/com/scotia/qa/common/1.0.0/

# Windows
dir %USERPROFILE%\.m2\repository\com\scotia\qa\common\1.0.0\
```

**Artefactos esperados**:
- `common-1.0.0.jar` (Código compilado)
- `common-1.0.0.pom` (Metadatos Maven)
- `common-1.0.0-sources.jar` (Código fuente)
- `common-1.0.0-javadoc.jar` (Documentación JavaDoc)

---

### 🧹 Limpiar Maven Local

Si tienes problemas con versiones viejas:

```bash
# macOS/Linux
rm -rf ~/.m2/repository/com/scotia/qa/

# Windows PowerShell
Remove-Item -Recurse -Force $env:USERPROFILE\.m2\repository\com\scotia\qa\

# Republicar
cd /path/to/qa-scotia-frameworks
./gradlew clean publishToMavenLocal
```

---

## 🐛 Troubleshooting

### 🔍 Problema 1: Config no encontrado

**Error**:
```
WARN [ConfigurationUtilities] Archivo de configuración no encontrado: config-qa.properties
```

**Causa**: Nombre de archivo incorrecto o ubicación incorrecta.

**Solución**:
1. Verificar nombre: `config-scotia.properties` (recomendado) o `config-qa.properties`
2. Verificar ubicación: `src/test/resources/config-scotia.properties`
3. El framework busca en este orden:
   - `config-{env}.properties` (ej: config-qa.properties)
   - `config-scotia.properties`
   - `config.properties`

---

### 🔍 Problema 2: Variables de entorno no resueltas

**Error**:
```
WARN [ConfigManager] ⚠️ Variable de entorno 'DB_URL' no encontrada
```

**Causa**: `.env.local` no cargado o variables mal configuradas.

**Solución**:

```bash
# Cargar variables manualmente
source .env.local  # macOS/Linux
.\scripts\setup-env.ps1  # Windows

# Verificar que se cargaron
echo $DB_URL  # macOS/Linux
echo $env:DB_URL  # Windows PowerShell

# Ejecutar tests
./gradlew test
```

---

### 🔍 Problema 3: Dependencias no resuelven

**Error**:
```
Could not resolve com.scotia.qa:common:1.0.0
```

**Causa**: Framework no publicado en mavenLocal o artefactos corruptos.

**Solución**:

```bash
# 1. Limpiar y republicar framework
cd /path/to/qa-scotia-frameworks
rm -rf ~/.m2/repository/com/scotia/qa/
./gradlew clean publishToMavenLocal

# 2. Actualizar dependencias en módulo
cd /path/to/tu-modulo
./gradlew clean --refresh-dependencies build
```

---

### 🔍 Problema 4: Tests fallan en Windows pero pasan en Mac

**Causas comunes**:
- ❌ Rutas con `\` en lugar de `/`
- ❌ Variables de entorno no cargadas
- ❌ Drivers no descargados o mal ubicados

**Solución**:

1. **Rutas**: Siempre usar `/` (Java las normaliza):
   ```properties
   # Correcto (funciona en ambos OS)
   driver.local.base.path=C:/drivers
   
   # Incorrecto
   driver.local.base.path=C:\drivers
   ```

2. **Variables**: Cargar con `setup-env.ps1`:
   ```powershell
   .\scripts\setup-env.ps1
   ```

3. **Drivers**: Verificar estructura:
   ```
   C:/drivers/chromedriver/143.0.7499.41/chromedriver.exe
   ```

---

## 📝 Cambios Recientes

### ✅ Diciembre 16, 2025

#### **Correcciones aplicadas**:

1. **ConfigManager**: Búsqueda flexible de archivos `.properties`
   - Ahora acepta `config-scotia.properties`, `config-qa.properties`, `config.properties`
   - Eliminados warnings innecesarios

2. **WebDriverFactory**: Logs simplificados
   - Reducido de 5-10 logs a 1 log conciso
   - Mensajes de error de 200 líneas a 5 líneas

3. **Gradle**: Repositorios en cascada
   - Artifactory siempre disponible como fallback
   - Orden: mavenLocal → Artifactory → mavenCentral

4. **Eliminación**: Estrategia de scripts en META-INF
   - Scripts ya NO se incluyen en JARs
   - Scripts se copian manualmente a módulos

#### **Archivos modificados**:
- `common/src/main/java/com/scotia/qa/common/config/ConfigManager.java`
- `web-core/src/main/java/com/scotia/qa/webcore/driver/WebDriverFactory.java`
- `common/build.gradle` (eliminada task `copyScriptsToResources`)
- `build.gradle` (actualizado bloque `repositories`)

---

### ✅ Diciembre 5, 2025

#### **Nueva funcionalidad**: Integración con Artifactory

- ✅ `ArtifactoryDriverManager.java` creado
- ✅ Estrategia dual: LOCAL y ARTIFACTORY
- ✅ Caché automático en `~/.qa-drivers/`
- ✅ Documentación completa para Infra y QA

---

## 📞 Soporte

### 📚 Documentación Adicional

- **Framework Guide**: `/documentacion/FRAMEWORK-GUIDE.md`
- **Quick Start**: `/documentacion/QUICK-START.md`
- **Scripts Guide**: `/scripts/SCRIPTS-GUIDE.md`

### 🛟 Contacto

- **Equipo QA**: qa-team@scotia.com
- **Infra/DevOps**: devops@scotia.com
- **Framework Lead**: Abel Venero

---

## 📄 Licencia

© 2025 Scotiabank - Uso interno exclusivo

---

**Preparado por**: Abel Venero  
**Versión**: 1.0.0  
**Última actualización**: Diciembre 16, 2025

