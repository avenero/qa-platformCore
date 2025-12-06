# 🚀 Uso de WebDrivers desde Artifactory - Ejemplos

Este documento muestra cómo usar la integración de Artifactory en tus módulos de prueba.

---

## ✅ Configuración Inicial (Una sola vez)

### 1. Configurar `config-scotia.properties`

```properties
# Estrategia de drivers: artifactory (recomendado en entornos corporativos)
driver.strategy=artifactory

# Artifactory habilitado
driver.artifactory.enabled=true
driver.artifactory.base.url=${ARTIFACTORY_BASE_URL}
driver.artifactory.user=${ARTIFACTORY_USER}
driver.artifactory.token=${ARTIFACTORY_TOKEN}
driver.artifactory.timeout=60
driver.artifactory.retry.enabled=true
driver.artifactory.retry.max=3

# Versiones de drivers
driver.chrome.version=114.0.5735.90
driver.firefox.version=0.33.0
driver.edge.version=114.0.1823.37

# Caché local (evita descargar en cada ejecución)
driver.cache.enabled=true
driver.cache.dir=${user.home}/.qa-drivers
```

### 2. Configurar `.env.local`

```bash
# Artifactory
ARTIFACTORY_BASE_URL=https://artifactory.scotia.com/artifactory/qa-drivers
ARTIFACTORY_USER=qa-automation-reader
ARTIFACTORY_TOKEN=tu_token_aqui

# Versiones (opcional, sino usa las del config)
CHROMEDRIVER_VERSION=114.0.5735.90
GECKODRIVER_VERSION=0.33.0
EDGEDRIVER_VERSION=114.0.1823.37
```

### 3. Cargar Variables de Entorno

```bash
# macOS/Linux
source .env.local

# Windows PowerShell
. .\.env.local
```

---

## 📦 Uso en Tests

### Ejemplo 1: ChromeDriver con Artifactory

```java
package com.mymodule.steps;

import com.scotia.qa.common.driver.ArtifactoryDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.nio.file.Path;

public class WebTestSteps {

    private WebDriver driver;

    @Before("@web")
    public void setupChromeDriver() throws Exception {
        // Obtener driver desde Artifactory (auto-descarga si no existe en caché)
        Path driverPath = ArtifactoryDriverManager.getDriverFromConfig("chromedriver");
        
        // Configurar System Property
        System.setProperty("webdriver.chrome.driver", driverPath.toString());
        
        // Crear WebDriver
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        driver = new ChromeDriver(options);
        
        System.out.println("✓ ChromeDriver cargado desde: " + driverPath);
    }

    @After("@web")
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
```

**Salida esperada**:
```
⬇️  Descargando driver desde Artifactory: chromedriver 114.0.5735.90
✓ Driver descargado: chromedriver (intento 1/3)
✓ Driver descargado y cacheado: chromedriver 114.0.5735.90
✓ ChromeDriver cargado desde: /Users/user/.qa-drivers/chromedriver/114.0.5735.90/chromedriver
```

**Segunda ejecución** (ya está en caché):
```
✓ Driver encontrado en caché: chromedriver 114.0.5735.90
✓ ChromeDriver cargado desde: /Users/user/.qa-drivers/chromedriver/114.0.5735.90/chromedriver
```

---

### Ejemplo 2: Especificar Versión Manualmente

```java
import com.scotia.qa.common.driver.ArtifactoryDriverManager;
import java.nio.file.Path;

public class CustomDriverSetup {

    public void setupSpecificVersion() throws Exception {
        // Forzar versión específica (ignora config)
        Path chrome114 = ArtifactoryDriverManager.getDriver("chromedriver", "114.0.5735.90");
        Path chrome115 = ArtifactoryDriverManager.getDriver("chromedriver", "115.0.5790.98");
        
        System.setProperty("webdriver.chrome.driver", chrome115.toString());
        // ...usar driver...
    }
}
```

---

### Ejemplo 3: GeckoDriver (Firefox)

```java
import com.scotia.qa.common.driver.ArtifactoryDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import java.nio.file.Path;

@Before("@firefox")
public void setupFirefoxDriver() throws Exception {
    Path geckodriver = ArtifactoryDriverManager.getDriverFromConfig("geckodriver");
    System.setProperty("webdriver.gecko.driver", geckodriver.toString());
    
    driver = new FirefoxDriver();
}
```

---

### Ejemplo 4: EdgeDriver (Microsoft Edge)

```java
import com.scotia.qa.common.driver.ArtifactoryDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import java.nio.file.Path;

@Before("@edge")
public void setupEdgeDriver() throws Exception {
    Path edgedriver = ArtifactoryDriverManager.getDriver("edgedriver", "114.0.1823.37");
    System.setProperty("webdriver.edge.driver", edgedriver.toString());
    
    driver = new EdgeDriver();
}
```

---

## 🔄 Actualizar Drivers

### Cuando Sale Nueva Versión de Chrome

1. Infra publica la nueva versión en Artifactory (ej: `115.0.5790.98`)

2. Actualizar `config-scotia.properties`:
   ```properties
   driver.chrome.version=115.0.5790.98
   ```

3. Próxima ejecución descargará automáticamente la nueva versión:
   ```
   ⬇️  Descargando driver desde Artifactory: chromedriver 115.0.5790.98
   ✓ Driver descargado y cacheado: chromedriver 115.0.5790.98
   ```

---

## 🧹 Limpiar Caché (Opcional)

### Si necesitas forzar re-descarga

```java
import com.scotia.qa.common.driver.ArtifactoryDriverManager;

public class MaintenanceUtils {
    
    public static void clearDriverCache() throws Exception {
        ArtifactoryDriverManager.clearCache();
        System.out.println("✓ Caché de drivers limpiada");
        // Próxima ejecución descargará de nuevo
    }
}
```

O manualmente:
```bash
rm -rf ~/.qa-drivers
```

---

## 🐛 Troubleshooting

### Error: "Credenciales de Artifactory inválidas (HTTP 401)"

**Causa**: Usuario o token incorrectos en `.env.local`

**Solución**:
1. Verificar valores en `.env.local`:
   ```bash
   echo $ARTIFACTORY_USER
   echo $ARTIFACTORY_TOKEN
   ```
2. Solicitar nuevas credenciales a Infra si es necesario

---

### Error: "Driver no encontrado en Artifactory (HTTP 404)"

**Causa**: La versión especificada no existe en Artifactory

**Solución**:
1. Verificar versión en `config-scotia.properties`:
   ```properties
   driver.chrome.version=114.0.5735.90  # ¿Existe en Artifactory?
   ```
2. Consultar versiones disponibles con Infra
3. O probar con otra versión conocida

---

### Error: "Descarga fallida después de 3 intentos"

**Causa**: Problemas de red o timeout

**Solución**:
1. Verificar conectividad a Artifactory:
   ```bash
   curl -I https://artifactory.scotia.com/artifactory/qa-drivers/
   ```
2. Aumentar timeout en `config-scotia.properties`:
   ```properties
   driver.artifactory.timeout=120  # 2 minutos
   ```
3. Revisar proxy corporativo si aplica

---

### Error: "No se encontró chromedriver dentro del zip"

**Causa**: Estructura incorrecta del zip en Artifactory

**Solución**:
1. Notificar a Infra que revise estructura:
   ```
   chromedriver.zip/
   └── chromedriver  (o chromedriver.exe en Windows)
   ```
2. No debe haber subdirectorios dentro del zip

---

## 📊 Logs y Diagnóstico

### Habilitar Logs Detallados

```properties
# config-scotia.properties
logging.level=DEBUG
```

Verás logs como:
```
[DEBUG] DRIVER_MANAGER - Verificando caché: /Users/user/.qa-drivers/chromedriver/114.0.5735.90/chromedriver
[DEBUG] DRIVER_MANAGER - Caché miss, descargando...
[DEBUG] DRIVER_MANAGER - URL: https://artifactory.scotia.com/.../chromedriver.zip
[INFO]  DRIVER_MANAGER - ✓ Driver descargado: chromedriver (intento 1/3)
[INFO]  DRIVER_MANAGER - ✓ Driver descargado y cacheado
```

---

## 🎯 Best Practices

### ✅ DO

- **Usar caché**: Deja `driver.cache.enabled=true` (ahorra tiempo y ancho de banda)
- **Especificar versiones**: Siempre define `driver.chrome.version` en config
- **Validar localmente**: Probar descarga antes de CI/CD
- **Limpiar caché periódicamente**: Cada 2-3 meses o cuando cambies versiones

### ❌ DON'T

- **No hardcodear credenciales**: Siempre usa `.env.local` (gitignored)
- **No usar `latest`**: Siempre especifica versión exacta para reproducibilidad
- **No descargar en cada test**: Usa caché para performance
- **No commitear `.env.local`**: Mantenerlo en `.gitignore`

---

## 📚 Referencias

- **ChromeDriver Versions**: https://chromedriver.chromium.org/downloads
- **GeckoDriver Releases**: https://github.com/mozilla/geckodriver/releases
- **EdgeDriver Downloads**: https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/

---

**Última actualización**: Diciembre 2025  
**Versión del Framework**: 1.0.0

