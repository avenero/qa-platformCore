# Guía de Configuración — CuAleon / A·Spectra Core

> **Framework**: qa-platformCore v2.x  
> **Namespace**: `com.qa.*`  
> **Actualizado**: 2026-05-06

---

## Índice

1. [Configuración de módulos](#1-configuración-de-módulos)
2. [Web — Playwright](#2-web--playwright)
3. [API](#3-api)
4. [Mobile — Appium](#4-mobile--appium)
5. [Reporting — Jira/Xray](#5-reporting--jiraxray)
6. [Certificados SSL](#6-certificados-ssl)
7. [Publicación local de JARs](#7-publicación-local-de-jars)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Configuración de módulos

Cada proyecto consumidor de Core necesita **2 archivos** de configuración:

### `config-app.properties`

**Ubicación**: `src/test/resources/config-app.properties`

```properties
# ===========================
# AMBIENTE
# ===========================
test.env=qa

# ===========================
# WEB (si usas web-core)
# ===========================
browser.engine=playwright
playwright.browser=chromium
web.headless=true
playwright.timeout.ms=30000
web.base.url=https://qa.your-app.com

# ===========================
# API (si usas api-core)
# ===========================
api.base.url=https://api-qa.your-app.com/v1
api.timeout=30

# ===========================
# MOBILE (si usas mobile-core)
# ===========================
mobile.platform=android
mobile.device.name=emulator-5554
appium.server.url=http://localhost:4723

# ===========================
# REPORTING
# ===========================
reporting.enabled=true
reporting.output.dir=target/reports
extent.enabled=true
extent.outputPath=build/reports/extent/
extent.reportName=execution-report.html

# Jira/Xray (opcional)
jira.enabled=true
jira.url=${JIRA_URL}
jira.user=${JIRA_USER}
jira.password=${JIRA_PASSWORD}
jira.projectKey=PROJ
jira.testExecutionId=${TEST_EXECUTION_ID}
jira.autoCreateExecution=false
jira.updateStatus=true
jira.uploadReport=true
jira.includeEvidences=true
jira.failOnError=false
jira.updateMode=BATCH
jira.testEnvironment=QA
```

### `.env.local`

**Ubicación**: `<raíz-módulo>/.env.local`  
**Agregar a `.gitignore`**

```bash
# Ambiente
TEST_ENV=qa

# Web
WEB_BASE_URL=https://qa.your-app.com

# API
API_BASE_URL=https://api-qa.your-app.com/v1
API_TOKEN=your_token_here

# DB (si aplica)
DB_URL=jdbc:postgresql://localhost:5432/qa
DB_USER=qa_user
DB_PASS=qa_password

# Jira
JIRA_URL=https://jira.your-company.com
JIRA_USER=automation_user
JIRA_PASSWORD=automation_password
```

### Pasos de setup

```bash
# 1. Copiar templates (si existen en config/templates/)
cp config/templates/config-app.properties.template \
   src/test/resources/config-app.properties

cp config/templates/.env.local.template .env.local

# 2. Cargar variables de entorno (macOS/Linux)
source .env.local

# 3. Verificar
./gradlew test --dry-run
```

---

## 2. Web — Playwright

El módulo `web-core` es **Playwright-only**. No hay dependencias de Selenium ni WebDriverManager.

### Browsers soportados

| Alias en config | Browser real |
|----------------|--------------|
| `chromium` / `chrome` | Chromium |
| `firefox` | Firefox |
| `webkit` / `safari` | WebKit |

### Configuración por entorno

**Desarrollo local (headless=false para debug visual):**
```properties
browser.engine=playwright
playwright.browser=chromium
web.headless=false
playwright.timeout.ms=30000
```

**CI/CD (headless=true):**
```properties
browser.engine=playwright
playwright.browser=chromium
web.headless=true
playwright.timeout.ms=30000
playwright.headless.compatibility=true
```

### Instalación de browsers en CI

Los binarios de Playwright se instalan automáticamente. En Docker/CI agregar al pipeline:

```bash
# Gradle
./gradlew playwrightInstall

# O usar imagen Docker con Playwright pre-instalado:
# FROM mcr.microsoft.com/playwright/java:v1.50.0-jammy
```

### Activación del plugin web

Agregar el tag correspondiente al escenario Cucumber:

```gherkin
@web          # activa WebPlugin
@ui           # alias
@playwright   # alias
@browser      # alias
```

### Ejecución local

```bash
./gradlew :web-core:test \
  -Dbrowser.engine=playwright \
  -Dplaywright.browser=chromium \
  -Dweb.headless=true
```

---

## 3. API

El módulo `api-core` gestiona requests HTTP a APIs REST.

### Configuración mínima

```properties
api.base.url=https://api.your-app.com/v1
api.timeout=30
api.ssl.verify=true
```

### Activación del plugin API

```gherkin
@api   # activa ApiPlugin
```

### Ejecución local

```bash
./gradlew :api-core:test \
  -Dapi.base.url=https://api.your-app.com \
  -Dtest.env=qa
```

---

## 4. Mobile — Appium

El módulo `mobile-core` usa Appium con W3C Actions API (Appium 8+).

### Configuración mínima

```properties
mobile.platform=android
mobile.device.name=emulator-5554
appium.server.url=http://localhost:4723
```

### Plataformas soportadas

```properties
# Android
mobile.platform=android
mobile.device.name=Pixel_4_API_30
mobile.app.path=/path/to/app.apk

# iOS
mobile.platform=ios
mobile.device.name=iPhone 14
mobile.app.path=/path/to/app.ipa
mobile.os.version=16.0
```

### Activación del plugin mobile

```gherkin
@mobile
@android  # alias para Android
@ios      # alias para iOS
```

---

## 5. Reporting — Jira/Xray

### Estrategia 1: Test Execution pre-existente (recomendada)

```properties
jira.autoCreateExecution=false
jira.testExecutionId=PROJ-640   # Issue pre-creado en Jira
```

### Estrategia 2: Auto-crear Test Execution

```properties
jira.autoCreateExecution=true
jira.projectKey=PROJ
jira.testEnvironment=QA
```

### Tags en Gherkin

```gherkin
@PROJ-123 @smoke @api
Scenario: Login exitoso
  ...
```

El framework busca tags con patrón `@([A-Z]{2,10}-\d+)` → vincula con el Test en Jira.

### Modos de actualización

- `jira.updateMode=BATCH` — un request por suite (más rápido, recomendado)
- `jira.updateMode=SINGLE` — un request por test (más tolerante a fallos)

### Configuraciones por entorno

```properties
# Desarrollo local — solo HTML
jira.updateStatus=false
jira.uploadReport=false
extent.enabled=true

# CI/CD con execution pre-creado
jira.updateStatus=true
jira.uploadReport=true
jira.autoCreateExecution=false
jira.testExecutionId=${TEST_EXECUTION_ID}

# CI/CD totalmente automático
jira.updateStatus=true
jira.autoCreateExecution=true
jira.projectKey=PROJ
jira.testEnvironment=${ENV}
```

---

## 6. Certificados SSL

El framework usa `SSLContextFactory` (`com.qa.common.ssl.SSLContextFactory`) para cargar un truststore Java con certificados de servicios externos (Jira, APIs con TLS custom).

**Ver guía completa:** [common/ssl/README.md](../common/ssl/README.md)

**Resumen:**
```bash
# Importar certificado al truststore
cd qa-platformCore/common/ssl
keytool -import -alias <nombre> -file /tmp/servicio.crt \
  -keystore myTrustStore.jks -storepass changeit -noprompt

# Republicar common
cd ..
./gradlew :common:publishToMavenLocal
```

Activar en `gradle.properties` del proyecto consumidor:
```properties
systemProp.javax.net.ssl.trustStore=common/ssl/myTrustStore.jks
systemProp.javax.net.ssl.trustStorePassword=changeit
systemProp.javax.net.ssl.trustStoreType=JKS
```

---

## 7. Publicación local de JARs

**Ver flujo completo:** [README.md → Flujo de versionado y publicación de JARs](../README.md#flujo-de-versionado-y-publicación-de-jars)

```bash
# Publicar todos los módulos a ~/.m2
./gradlew clean publishToMavenLocal

# Consumir versión local desde qa-platformBE
./gradlew clean test -PcoreVersion=2.0.8-SNAPSHOT --refresh-dependencies
```

Los JARs se publican en `~/.m2/repository/com/qa/`.

---

## 8. Troubleshooting

### Browser no arranca en CI

```
Error: Executable doesn't exist at .../chromium
```

Solución:
```bash
./gradlew playwrightInstall
# O usar imagen: mcr.microsoft.com/playwright/java:v1.50.0-jammy
```

Verificar también:
- `web.headless=true` activo en CI
- Permisos del contenedor (non-root puede requerir `--no-sandbox`)

### Step no encontrado en runtime

```
Step not found: "When hago click en el elemento..."
```

Solución:
- Verificar que el tag del escenario activa el plugin (`@web`, `@api`, `@mobile`)
- Verificar que `web-core` / `api-core` está en dependencias del proyecto
- El glue se deriva automáticamente por el plugin; no es necesario declararlo manualmente

### SSL error al conectar con Jira

```
PKIX path building failed: unable to find valid certification path
```

Solución: ver [common/ssl/README.md](../common/ssl/README.md)

### Gradle no resuelve `com.qa:*-core`

```
Could not resolve com.qa:api-core:2.0.x
```

Opciones:
1. Publicar localmente: `./gradlew publishToMavenLocal` (en qa-platformCore)
2. Exportar `GITHUB_ACTOR` y `GITHUB_TOKEN` para resolver desde GitHub Packages
3. En Docker: usar `syncCoreFromMavenLocal` primero (ver qa-platformBE README)

### Error de versión de Core

```
SPI mismatch: plugin requires core version 2.0.x
```

Verificar que `coreVersion` en `gradle.properties` del proyecto consumidor coincide con la versión publicada.
