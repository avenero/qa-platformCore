# 🚗 Estrategia de WebDrivers - Local + Artifactory

**Framework**: Scotia QA Framework v1.0.0  
**Fecha**: Diciembre 2025  
**Autor**: Abel Venero

---

## 📑 Índice

- [🎯 Objetivo](#-objetivo)
- [🏗️ Arquitectura de Fallback](#️-arquitectura-de-fallback)
- [📊 Flujo de Decisión](#-flujo-de-decisión)
- [⚙️ Configuración](#️-configuración)
- [📁 Estructura de Directorios](#-estructura-de-directorios)
- [🔧 Implementación](#-implementación)
- [📝 Ejemplos de Uso](#-ejemplos-de-uso)
- [🐛 Troubleshooting](#-troubleshooting)
- [✅ Mejores Prácticas](#-mejores-prácticas)

---

## 🎯 Objetivo

Implementar un sistema **inteligente y resiliente** para gestionar WebDrivers con **triple estrategia de fallback**:

1. **🏠 Local Path Fijo** → Ruta específica donde el desarrollador coloca drivers manualmente
2. **💾 Caché Local** → Drivers descargados previamente (automático)
3. **☁️ Artifactory** → Repositorio corporativo (último recurso)

### Ventajas de esta Estrategia

| Ventaja | Descripción |
|---------|-------------|
| ✅ **Flexibilidad** | Desarrolladores pueden usar sus propios drivers sin configurar Artifactory |
| ✅ **Offline-first** | Funciona sin conexión a Artifactory si el driver existe localmente |
| ✅ **Performance** | Busca primero en local (instantáneo), luego caché, finalmente descarga |
| ✅ **Fallback robusto** | Si una fuente falla, intenta la siguiente automáticamente |
| ✅ **Control total** | Cada desarrollador decide qué versión usar (local) o usar la corporativa (Artifactory) |

---

## 🏗️ Arquitectura de Fallback

```
┌─────────────────────────────────────────────────────────────────┐
│                    SOLICITUD DE WEBDRIVER                        │
│                   (chromedriver v114.0.5735.90)                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
        ┌──────────────────────────────────────────────┐
        │  🔍 ESTRATEGIA 1: LOCAL PATH FIJO            │
        │  Buscar en: driver.local.base.path          │
        │  Ejemplo: ~/drivers/chromedriver/chromedriver│
        └──────────────────┬───────────────────────────┘
                           │
                  ┌────────┴────────┐
                  │  ¿Existe?       │
                  └────────┬────────┘
                     SÍ   │   NO
            ┌─────────────┴─────────────┐
            ▼                           ▼
    ┌───────────────┐       ┌──────────────────────────────┐
    │ ✅ USAR LOCAL │       │ 🔍 ESTRATEGIA 2: CACHÉ LOCAL│
    │ return path   │       │ Buscar en: ~/.qa-drivers/   │
    └───────────────┘       └──────────┬───────────────────┘
                                       │
                              ┌────────┴────────┐
                              │  ¿Existe?       │
                              └────────┬────────┘
                                 SÍ   │   NO
                        ┌─────────────┴─────────────┐
                        ▼                           ▼
                ┌───────────────┐       ┌─────────────────────────────┐
                │ ✅ USAR CACHÉ │       │ 🔍 ESTRATEGIA 3: ARTIFACTORY│
                │ return path   │       │ Descargar desde Artifactory │
                └───────────────┘       └──────────┬──────────────────┘
                                                   │
                                          ┌────────┴────────┐
                                          │  ¿Descargó?     │
                                          └────────┬────────┘
                                             SÍ   │   NO
                                    ┌─────────────┴──────────┐
                                    ▼                        ▼
                            ┌──────────────┐      ┌──────────────────┐
                            │ ✅ USAR       │      │ ❌ ERROR         │
                            │ ARTIFACTORY  │      │ Driver no        │
                            │ + guardar    │      │ disponible       │
                            │ en caché     │      │                  │
                            └──────────────┘      └──────────────────┘
```

---

## 📊 Flujo de Decisión

### Pseudocódigo del Algoritmo

```java
Path getDriver(String driverName, String version) {
    // ESTRATEGIA 1: Local Path Fijo
    if (config.isEnabled("driver.local.enabled")) {
        Path localPath = checkLocalPath(driverName, version);
        if (localPath != null && Files.exists(localPath)) {
            log("✓ Driver encontrado en LOCAL PATH");
            return localPath;
        }
    }
    
    // ESTRATEGIA 2: Caché Local
    if (config.isEnabled("driver.cache.enabled")) {
        Path cachedPath = checkCache(driverName, version);
        if (cachedPath != null && Files.exists(cachedPath)) {
            log("✓ Driver encontrado en CACHÉ");
            return cachedPath;
        }
    }
    
    // ESTRATEGIA 3: Artifactory (fallback)
    if (config.isEnabled("driver.artifactory.enabled")) {
        try {
            Path downloadedPath = downloadFromArtifactory(driverName, version);
            cacheDriver(downloadedPath, driverName, version);
            log("✓ Driver descargado desde ARTIFACTORY y cacheado");
            return downloadedPath;
        } catch (IOException e) {
            throw new DriverNotFoundException("No se pudo obtener driver desde ninguna fuente");
        }
    }
    
    throw new DriverNotFoundException("Todas las estrategias fallaron");
}
```

---

## ⚙️ Configuración

### `config-scotia.properties`

```properties
# ====================================================================
# WEBDRIVERS - ESTRATEGIA DE FALLBACK
# ====================================================================

# Estrategia general (local, cache, artifactory)
driver.strategy=fallback

# ────────────────────────────────────────────────────────────────────
# ESTRATEGIA 1: LOCAL PATH FIJO
# ────────────────────────────────────────────────────────────────────
# Habilitar búsqueda en path local fijo
driver.local.enabled=true

# Directorio base donde el desarrollador coloca drivers manualmente
# Estructura esperada: {base.path}/{driver-name}/{version}/chromedriver
# Ejemplo: ~/drivers/chromedriver/114.0.5735.90/chromedriver
driver.local.base.path=${DRIVER_LOCAL_PATH}

# Modo strict: Si está en true y el driver local no existe, falla inmediatamente
# Si está en false, intenta caché y luego Artifactory
driver.local.strict=false

# ────────────────────────────────────────────────────────────────────
# ESTRATEGIA 2: CACHÉ LOCAL (Automático)
# ────────────────────────────────────────────────────────────────────
# Habilitar caché de drivers descargados
driver.cache.enabled=true

# Directorio de caché (se crea automáticamente)
driver.cache.dir=${user.home}/.qa-drivers

# Tiempo de expiración del caché (en días, 0 = nunca expira)
driver.cache.ttl=30

# ────────────────────────────────────────────────────────────────────
# ESTRATEGIA 3: ARTIFACTORY (Fallback)
# ────────────────────────────────────────────────────────────────────
# Habilitar descarga desde Artifactory
driver.artifactory.enabled=true

# URL base de Artifactory
driver.artifactory.base.url=${ARTIFACTORY_BASE_URL}

# Credenciales (desde .env.local)
driver.artifactory.user=${ARTIFACTORY_USER}
driver.artifactory.token=${ARTIFACTORY_TOKEN}

# Timeout para descargas (segundos)
driver.artifactory.timeout=60

# Reintentos
driver.artifactory.retry.enabled=true
driver.artifactory.retry.max=3
driver.artifactory.retry.backoff=exponential

# ────────────────────────────────────────────────────────────────────
# VERSIONES DE DRIVERS
# ────────────────────────────────────────────────────────────────────
driver.chrome.version=114.0.5735.90
driver.firefox.version=0.33.0
driver.edge.version=114.0.1823.37

# ────────────────────────────────────────────────────────────────────
# LOGS Y DIAGNÓSTICO
# ────────────────────────────────────────────────────────────────────
# Nivel de detalle en logs (info, debug)
driver.logging.level=info

# Mostrar estrategia usada en cada ejecución
driver.logging.show.strategy=true
```

### `.env.local`

```bash
# ====================================================================
# WEBDRIVERS - CONFIGURACIÓN LOCAL
# ====================================================================

# Path local donde colocas tus drivers manualmente
# Estructura: {DRIVER_LOCAL_PATH}/{driver-name}/{version}/chromedriver
DRIVER_LOCAL_PATH=/Users/tu_usuario/drivers

# Artifactory (solo si no tienes drivers locales)
ARTIFACTORY_BASE_URL=https://artifactory.cldevops.chl.bns/artifactory/qa-drivers
ARTIFACTORY_USER=qa-automation-reader
ARTIFACTORY_TOKEN=xxxxxxxxxxxxx

# Versiones específicas (opcional, sobrescribe config-scotia.properties)
CHROMEDRIVER_VERSION=114.0.5735.90
GECKODRIVER_VERSION=0.33.0
EDGEDRIVER_VERSION=114.0.1823.37
```

---

## 📁 Estructura de Directorios

### Local Path (Manual por Desarrollador)

```
~/drivers/                              ← DRIVER_LOCAL_PATH
│
├── chromedriver/
│   ├── 114.0.5735.90/
│   │   └── chromedriver                ← Ejecutable
│   └── 115.0.5790.98/
│       └── chromedriver
│
├── geckodriver/
│   └── 0.33.0/
│       └── geckodriver
│
└── edgedriver/
    └── 114.0.1823.37/
        └── msedgedriver.exe            ← Windows
```

**Cómo configurar** (desarrollador):

```bash
# 1. Crear estructura
mkdir -p ~/drivers/chromedriver/114.0.5735.90

# 2. Descargar driver manualmente
# Chrome: https://chromedriver.chromium.org/downloads
curl -o ~/Downloads/chromedriver.zip https://...

# 3. Extraer a la ubicación correcta
unzip ~/Downloads/chromedriver.zip -d ~/drivers/chromedriver/114.0.5735.90/

# 4. Dar permisos de ejecución (macOS/Linux)
chmod +x ~/drivers/chromedriver/114.0.5735.90/chromedriver

# 5. Configurar en .env.local
echo "DRIVER_LOCAL_PATH=$HOME/drivers" >> .env.local
```

---

### Caché Local (Automático)

```
~/.qa-drivers/                          ← Generado automáticamente
│
├── chromedriver/
│   └── 114.0.5735.90/
│       ├── chromedriver                ← Descargado desde Artifactory
│       └── .metadata.json              ← Info de descarga
│
├── geckodriver/
│   └── 0.33.0/
│       ├── geckodriver
│       └── .metadata.json
│
└── .cache-info.json                    ← Estado general del caché
```

**Metadata de caché** (`.metadata.json`):

```json
{
  "driver": "chromedriver",
  "version": "114.0.5735.90",
  "source": "artifactory",
  "downloadedAt": "2025-12-05T10:30:00Z",
  "downloadUrl": "https://artifactory.../chromedriver.zip",
  "os": "mac64",
  "checksum": "sha256:abc123...",
  "expiresAt": "2026-01-04T10:30:00Z"
}
```

---

### Artifactory (Corporativo)

Ver estructura completa en: **[RESUMEN-ARTIFACTORY.md](./RESUMEN-ARTIFACTORY.md#-estructura-en-artifactory-requerida)**

```
https://artifactory.cldevops.chl.bns/artifactory/qa-drivers/
│
├── chromedriver/
│   └── 114.0.5735.90/
│       ├── linux64/chromedriver.zip
│       ├── mac64/chromedriver.zip
│       ├── mac_arm64/chromedriver.zip
│       └── win32/chromedriver.zip
│
├── geckodriver/...
└── edgedriver/...
```

---

## 🔧 Implementación

### Clase Principal: `WebDriverManager`

```java
package com.scotia.qa.common.driver;

import com.scotia.qa.common.config.ConfigManager;
import com.scotia.qa.common.logging.TestLogger;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

/**
 * Gestor inteligente de WebDrivers con estrategia de fallback.
 * 
 * <p>Orden de búsqueda:</p>
 * <ol>
 *   <li>Local Path Fijo (manual)</li>
 *   <li>Caché Local (automático)</li>
 *   <li>Artifactory (descarga)</li>
 * </ol>
 */
public class WebDriverManager {
    
    private static final ConfigManager config = ConfigManager.getInstance();
    
    /**
     * Obtiene el driver usando estrategia de fallback.
     * 
     * @param driverName Nombre del driver (chromedriver, geckodriver, edgedriver)
     * @param version Versión específica (ej: "114.0.5735.90")
     * @return Path al ejecutable del driver
     * @throws DriverNotFoundException Si no se encuentra en ninguna fuente
     */
    public static Path getDriver(String driverName, String version) 
            throws DriverNotFoundException {
        
        boolean showStrategy = config.getBoolean("driver.logging.show.strategy", true);
        
        // ESTRATEGIA 1: Local Path Fijo
        if (config.getBoolean("driver.local.enabled", true)) {
            try {
                Path localPath = getDriverFromLocalPath(driverName, version);
                if (localPath != null) {
                    if (showStrategy) {
                        TestLogger.logInfo("DRIVER_MANAGER", 
                            String.format("✓ Usando driver desde LOCAL PATH: %s %s", 
                                driverName, version),
                            Map.of("strategy", "local-path", "path", localPath.toString()));
                    }
                    return localPath;
                }
                
                // Modo strict: falla si no existe en local
                if (config.getBoolean("driver.local.strict", false)) {
                    throw new DriverNotFoundException(
                        String.format("driver.local.strict=true pero %s %s no existe en %s",
                            driverName, version, config.get("driver.local.base.path")));
                }
            } catch (IOException e) {
                TestLogger.logWarn("DRIVER_MANAGER", 
                    "No se pudo acceder a LOCAL PATH, intentando caché...", null);
            }
        }
        
        // ESTRATEGIA 2: Caché Local
        if (config.getBoolean("driver.cache.enabled", true)) {
            Path cachedPath = getDriverFromCache(driverName, version);
            if (cachedPath != null) {
                if (showStrategy) {
                    TestLogger.logInfo("DRIVER_MANAGER", 
                        String.format("✓ Usando driver desde CACHÉ: %s %s", 
                            driverName, version),
                        Map.of("strategy", "cache", "path", cachedPath.toString()));
                }
                return cachedPath;
            }
        }
        
        // ESTRATEGIA 3: Artifactory
        if (config.getBoolean("driver.artifactory.enabled", true)) {
            try {
                Path downloadedPath = downloadFromArtifactory(driverName, version);
                if (showStrategy) {
                    TestLogger.logInfo("DRIVER_MANAGER", 
                        String.format("✓ Driver descargado desde ARTIFACTORY: %s %s", 
                            driverName, version),
                        Map.of("strategy", "artifactory", "path", downloadedPath.toString()));
                }
                return downloadedPath;
            } catch (IOException e) {
                TestLogger.logError("DRIVER_MANAGER", 
                    "Falló descarga desde Artifactory", 
                    Map.of("error", e.getMessage()));
            }
        }
        
        // Todas las estrategias fallaron
        throw new DriverNotFoundException(
            String.format("No se pudo obtener %s %s desde ninguna fuente. " +
                "Verifica: (1) driver.local.base.path, (2) caché en ~/.qa-drivers, " +
                "(3) credenciales de Artifactory", driverName, version));
    }
    
    /**
     * Busca driver en path local fijo configurado por el desarrollador.
     */
    private static Path getDriverFromLocalPath(String driverName, String version) 
            throws IOException {
        String basePath = config.get("driver.local.base.path");
        if (basePath == null || basePath.isEmpty()) {
            return null;
        }
        
        // Resolver variables de entorno y home
        basePath = basePath.replace("${user.home}", System.getProperty("user.home"));
        basePath = basePath.replace("~", System.getProperty("user.home"));
        
        // Construir path esperado
        String executableName = getExecutableName(driverName);
        Path driverPath = Paths.get(basePath, driverName, version, executableName);
        
        if (Files.exists(driverPath) && Files.isExecutable(driverPath)) {
            return driverPath;
        }
        
        return null;
    }
    
    /**
     * Busca driver en caché local.
     */
    private static Path getDriverFromCache(String driverName, String version) {
        String cacheDir = config.get("driver.cache.dir", 
            System.getProperty("user.home") + "/.qa-drivers");
        
        Path driverPath = Paths.get(cacheDir, driverName, version, 
            getExecutableName(driverName));
        
        if (Files.exists(driverPath) && Files.isExecutable(driverPath)) {
            // Verificar expiración
            if (isCacheExpired(driverPath)) {
                TestLogger.logWarn("DRIVER_MANAGER", 
                    "Driver en caché expirado, se descargará nuevo", null);
                return null;
            }
            return driverPath;
        }
        
        return null;
    }
    
    /**
     * Descarga driver desde Artifactory.
     */
    private static Path downloadFromArtifactory(String driverName, String version) 
            throws IOException {
        // Delegar a ArtifactoryDriverManager existente
        return ArtifactoryDriverManager.getDriver(driverName, version);
    }
    
    /**
     * Verifica si el caché expiró según TTL configurado.
     */
    private static boolean isCacheExpired(Path driverPath) {
        int ttlDays = config.getInt("driver.cache.ttl", 30);
        if (ttlDays == 0) {
            return false; // Nunca expira
        }
        
        try {
            long lastModified = Files.getLastModifiedTime(driverPath).toMillis();
            long now = System.currentTimeMillis();
            long daysSinceDownload = (now - lastModified) / (1000 * 60 * 60 * 24);
            return daysSinceDownload > ttlDays;
        } catch (IOException e) {
            return false; // En caso de error, no expirar
        }
    }
    
    /**
     * Obtiene el nombre del ejecutable según OS.
     */
    private static String getExecutableName(String driverName) {
        boolean isWindows = System.getProperty("os.name")
            .toLowerCase().contains("win");
        
        switch (driverName) {
            case "chromedriver":
                return isWindows ? "chromedriver.exe" : "chromedriver";
            case "geckodriver":
                return isWindows ? "geckodriver.exe" : "geckodriver";
            case "edgedriver":
                return isWindows ? "msedgedriver.exe" : "msedgedriver";
            default:
                throw new IllegalArgumentException("Driver desconocido: " + driverName);
        }
    }
}

/**
 * Excepción lanzada cuando no se encuentra el driver en ninguna fuente.
 */
class DriverNotFoundException extends RuntimeException {
    public DriverNotFoundException(String message) {
        super(message);
    }
}
```

---

## 📝 Ejemplos de Uso

### Ejemplo 1: Desarrollador con Drivers Locales

**Escenario**: Desarrollador tiene Chrome 114 descargado manualmente en `~/drivers`

**Configuración** (`.env.local`):
```bash
DRIVER_LOCAL_PATH=/Users/abel/drivers
CHROMEDRIVER_VERSION=114.0.5735.90
```

**Estructura local**:
```
/Users/abel/drivers/
└── chromedriver/
    └── 114.0.5735.90/
        └── chromedriver
```

**Uso en test**:
```java
// Obtener driver (usa local automáticamente)
Path chromeDriver = WebDriverManager.getDriver("chromedriver", "114.0.5735.90");
System.setProperty("webdriver.chrome.driver", chromeDriver.toString());

// Crear WebDriver
WebDriver driver = new ChromeDriver();
```

**Logs**:
```
✓ Usando driver desde LOCAL PATH: chromedriver 114.0.5735.90
  strategy: local-path
  path: /Users/abel/drivers/chromedriver/114.0.5735.90/chromedriver
```

---

### Ejemplo 2: Desarrollador sin Drivers Locales (Usa Caché)

**Escenario**: Primera ejecución descargó desde Artifactory, segunda usa caché

**Primera ejecución**:
```java
Path chromeDriver = WebDriverManager.getDriver("chromedriver", "114.0.5735.90");
// Descarga desde Artifactory → guarda en ~/.qa-drivers/
```

**Logs (primera vez)**:
```
⚠️  Driver no encontrado en LOCAL PATH
⚠️  Driver no encontrado en CACHÉ
⬇️  Descargando desde Artifactory: chromedriver 114.0.5735.90
✓ Driver descargado desde ARTIFACTORY: chromedriver 114.0.5735.90
  strategy: artifactory
  path: ~/.qa-drivers/chromedriver/114.0.5735.90/chromedriver
```

**Segunda ejecución** (mismo día):
```java
Path chromeDriver = WebDriverManager.getDriver("chromedriver", "114.0.5735.90");
// Encuentra en caché → retorna inmediatamente
```

**Logs (segunda vez)**:
```
✓ Usando driver desde CACHÉ: chromedriver 114.0.5735.90
  strategy: cache
  path: ~/.qa-drivers/chromedriver/114.0.5735.90/chromedriver
```

---

### Ejemplo 3: CI/CD (Solo Artifactory)

**Escenario**: Jenkins/GitLab ejecuta tests, no tiene drivers locales

**Configuración** (CI/CD environment variables):
```bash
DRIVER_LOCAL_ENABLED=false          # Deshabilitar local path
DRIVER_CACHE_ENABLED=true           # Habilitar caché (para builds subsecuentes)
DRIVER_ARTIFACTORY_ENABLED=true
ARTIFACTORY_USER=ci-automation
ARTIFACTORY_TOKEN=${SECRET_TOKEN}
```

**Uso**:
```java
Path chromeDriver = WebDriverManager.getDriver("chromedriver", "114.0.5735.90");
// Descarga desde Artifactory (primera vez)
// Usa caché en builds siguientes
```

---

### Ejemplo 4: Modo Strict (Solo Local)

**Escenario**: Desarrollador quiere forzar uso de su driver local (no usar Artifactory)

**Configuración**:
```properties
driver.local.enabled=true
driver.local.strict=true            # Falla si no existe en local
driver.cache.enabled=false
driver.artifactory.enabled=false
```

**Resultado**:
- ✅ Si existe en local → usa
- ❌ Si NO existe en local → falla inmediatamente (no intenta Artifactory)

---

## 🐛 Troubleshooting

### ❌ Error: "No se pudo obtener chromedriver desde ninguna fuente"

**Causa**: Todas las estrategias fallaron

**Diagnóstico**:
```java
// Activar logs detallados
driver.logging.level=debug
driver.logging.show.strategy=true
```

**Verificar**:
1. **Local Path**: ¿Existe el archivo en `driver.local.base.path`?
   ```bash
   ls -la ~/drivers/chromedriver/114.0.5735.90/chromedriver
   ```

2. **Caché**: ¿Existe en `~/.qa-drivers`?
   ```bash
   ls -la ~/.qa-drivers/chromedriver/114.0.5735.90/
   ```

3. **Artifactory**: ¿Credenciales correctas?
   ```bash
   curl -u $ARTIFACTORY_USER:$ARTIFACTORY_TOKEN -I \
     https://artifactory.../chromedriver/114.0.5735.90/mac64/chromedriver.zip
   ```

---

### ❌ Error: "Permission denied" al ejecutar driver

**Causa**: Driver sin permisos de ejecución (macOS/Linux)

**Solución**:
```bash
# Local path
chmod +x ~/drivers/chromedriver/114.0.5735.90/chromedriver

# Caché
chmod +x ~/.qa-drivers/chromedriver/114.0.5735.90/chromedriver
```

---

### ⚠️ Warning: "Driver en caché expirado"

**Causa**: TTL del caché alcanzado (`driver.cache.ttl=30` días por defecto)

**Solución 1**: Limpiar caché (forzar re-descarga)
```java
WebDriverManager.clearCache();
```

**Solución 2**: Aumentar TTL
```properties
driver.cache.ttl=90    # 90 días
# O nunca expirar:
driver.cache.ttl=0
```

---

### 🐢 Performance: "Descarga muy lenta desde Artifactory"

**Causa**: Red corporativa lenta

**Solución**: Usar local path para desarrollo
```bash
# Descargar una vez manualmente
curl -o ~/Downloads/chromedriver.zip https://chromedriver.../114.0.5735.90/...
unzip ~/Downloads/chromedriver.zip -d ~/drivers/chromedriver/114.0.5735.90/

# Configurar
echo "DRIVER_LOCAL_PATH=$HOME/drivers" >> .env.local
```

---

## ✅ Mejores Prácticas

### Para Desarrolladores

1. **✅ Usa Local Path en Desarrollo**
   - Descarga drivers manualmente
   - Colócalos en `~/drivers/`
   - Configura `DRIVER_LOCAL_PATH` en `.env.local`
   - **Ventaja**: Ejecución instantánea, funciona offline

2. **✅ Documenta Versiones Usadas**
   ```bash
   # En README.md del módulo
   ## Drivers Locales (Opcional)
   - Chrome: 114.0.5735.90
   - Firefox: 0.33.0
   ```

3. **✅ No Commitees Drivers**
   ```gitignore
   # .gitignore
   drivers/
   *.exe
   chromedriver
   geckodriver
   ```

### Para CI/CD

1. **✅ Deshabilita Local Path**
   ```properties
   driver.local.enabled=false
   ```

2. **✅ Habilita Caché**
   ```properties
   driver.cache.enabled=true
   driver.cache.dir=/shared/jenkins/driver-cache  # Compartido entre builds
   ```

3. **✅ Usa Artifactory**
   ```bash
   # Variables de entorno en Jenkins
   ARTIFACTORY_USER=ci-automation
   ARTIFACTORY_TOKEN=${ARTIFACTORY_TOKEN_SECRET}
   ```

### Para el Equipo

1. **✅ Documenta Estrategia en Wiki**
   - Cómo configurar local path
   - Dónde descargar drivers manualmente
   - Credenciales de Artifactory

2. **✅ Estandariza Versiones**
   ```properties
   # Todos usan las mismas versiones
   driver.chrome.version=114.0.5735.90
   driver.firefox.version=0.33.0
   ```

3. **✅ Comunica Cambios**
   - Nueva versión de driver disponible
   - Actualizar local path si aplica
   - Limpiar caché si es necesario

---

## 📊 Comparación de Estrategias

| Aspecto | Local Path | Caché | Artifactory |
|---------|-----------|-------|-------------|
| **Setup** | Manual | Automático | Automático |
| **Velocidad** | ⚡ Instantánea | ⚡ Instantánea | 🐢 10-30s (primera vez) |
| **Offline** | ✅ Funciona | ✅ Funciona | ❌ Requiere red |
| **Actualizaciones** | 🔧 Manual | 🔧 Manual (limpiar caché) | ✅ Automático |
| **Mantenimiento** | 👤 Desarrollador | 🤖 Framework | 🏢 Infra |
| **Uso recomendado** | Desarrollo local | Todos | CI/CD + fallback |

---

## 🎯 Conclusión

Esta estrategia de **triple fallback** proporciona:

✅ **Flexibilidad**: Desarrolladores usan drivers locales si quieren  
✅ **Automatización**: Caché y Artifactory gestionan todo automáticamente  
✅ **Resiliencia**: Si una fuente falla, intenta las siguientes  
✅ **Performance**: Prioriza fuentes rápidas (local > caché > descarga)  
✅ **Offline-first**: Funciona sin conexión a Artifactory si el driver existe

**Recomendación final**:
- **Desarrollo**: Usa Local Path + Caché
- **CI/CD**: Usa Artifactory + Caché compartido
- **Producción**: Solo Artifactory (controlado por Infra)

---

## 📚 Referencias

- **Implementación completa**: [ArtifactoryDriverManager.java](../common/src/main/java/com/scotia/qa/common/driver/ArtifactoryDriverManager.java)
- **Configuración de Artifactory**: [RESUMEN-ARTIFACTORY.md](./RESUMEN-ARTIFACTORY.md)
- **Instrucciones para Infra**: [INSTRUCCIONES-ARTIFACTORY-PARA-INFRA.md](./INSTRUCCIONES-ARTIFACTORY-PARA-INFRA.md)
- **Ejemplos de uso**: [EJEMPLOS-USO-ARTIFACTORY.md](./EJEMPLOS-USO-ARTIFACTORY.md)

---

**Última actualización**: Diciembre 8, 2025  
**Framework**: Scotia QA Framework v1.0.0  
**Autor**: Abel Venero

