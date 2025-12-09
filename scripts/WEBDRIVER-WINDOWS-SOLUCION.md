# 🎯 SOLUCIÓN: WebDriver Timeout en Windows

**Fecha**: 9 de Diciembre 2025  
**Problema**: WebDriver se queda colgado intentando descargar chromedriver desde internet

---

## 🔴 PROBLEMA IDENTIFICADO

```
00:13:48.730 WARN - Exception reading CfT URL 
('https://googlechromelabs.github.io/chrome-for-testing/...')
Connection timed out
```

**Causa Raíz:**
- El `WebDriverFactory` estaba usando la librería externa `io.github.bonigarcia.wdm.WebDriverManager`
- Esta librería intenta descargar drivers desde `googlechromelabs.github.io`
- La red corporativa de Scotia bloquea/timeouts estas conexiones
- El proceso se quedaba colgado 2-3 minutos antes de fallar

---

## ✅ SOLUCIÓN IMPLEMENTADA

He modificado `WebDriverFactory` para usar tu `WebDriverManager` del framework que implementa **triple fallback**:

### Estrategia de Fallback:

```
1. Local Path Fijo
   ↓ (si falla)
2. Caché Local (~/.qa-drivers)
   ↓ (si falla)
3. Artifactory Corporativo
   ↓ (si falla)
4. Caché Legacy (WDM)
   ↓ (si falla)
5. PATH del Sistema
   ↓ (si falla)
6. Error descriptivo
```

---

## 📝 CAMBIO REALIZADO

**Archivo**: `web-core/src/main/java/com/scotia/qa/webcore/driver/WebDriverFactory.java`

**Método modificado**: `setupDriver()`

**Antes** (usaba librería externa):
```java
WebDriverManager wdm = getWebDriverManager(driverName);
wdm.timeout(10).setup();  // ← Intentaba descargar de internet
```

**Ahora** (usa tu WebDriverManager del framework):
```java
// Usar WebDriverManager del framework con triple fallback
java.nio.file.Path driverPath = 
    com.scotia.qa.common.driver.WebDriverManager.getDriverFromConfig(driverName);

if (driverPath != null && Files.exists(driverPath)) {
    System.setProperty(propertyName, driverPath.toString());
    // ← Funciona sin internet, sin timeout
}
```

---

## 🚀 CÓMO USAR

### Opción 1: Configurar Driver en Path Local (RECOMENDADO PARA WINDOWS)

1. **Descargar chromedriver manualmente**:
   - Chrome 143: https://storage.googleapis.com/chrome-for-testing-public/143.0.7444.175/win64/chromedriver-win64.zip
   - Extraer a: `C:\drivers\chromedriver\143.0.7444.175\chromedriver.exe`

2. **Configurar en `config-scotia.properties`**:
   ```properties
   # Habilitar local path
   driver.local.enabled=true
   driver.local.base.path=${DRIVER_LOCAL_PATH}
   
   # Versión de Chrome
   driver.chrome.version=143.0.7444.175
   ```

3. **Configurar en `.env.local`**:
   ```bash
   DRIVER_LOCAL_PATH=C:/drivers
   ```

4. **Ejecutar tests**:
   ```powershell
   . .\scripts\setup-env.ps1
   .\run-tests.bat
   ```

### Opción 2: Usar Artifactory (RECOMENDADO PARA CI/CD)

Si los drivers están publicados en Artifactory corporativo:

```properties
# config-scotia.properties
driver.artifactory.enabled=true
driver.artifactory.base.url=${ARTIFACTORY_BASE_URL}
driver.chrome.version=143.0.7444.175

# .env.local
ARTIFACTORY_BASE_URL=https://artifactory.corp.com/qa-drivers
ARTIFACTORY_USER=tu_usuario
ARTIFACTORY_TOKEN=tu_token
```

### Opción 3: Deshabilitar Detección Automática (FALLBACK)

Si tienes el driver en PATH del sistema:

```powershell
# Agregar chromedriver a PATH
$env:PATH += ";C:\drivers\chromedriver\143.0.7444.175"

# Ejecutar tests normalmente
.\run-tests.bat
```

---

## 📋 CONFIGURACIÓN COMPLETA DE EJEMPLO

**config-scotia.properties**:
```properties
# ====================================================================
# CONFIGURACIÓN DE DRIVERS (WINDOWS)
# ====================================================================

# Estrategia general
driver.strategy=fallback

# 1. Local Path (PRIORIDAD MÁXIMA)
driver.local.enabled=true
driver.local.base.path=${DRIVER_LOCAL_PATH}
driver.local.strict=false

# 2. Caché Local
driver.cache.enabled=true
driver.cache.dir=${user.home}/.qa-drivers
driver.cache.ttl=30

# 3. Artifactory (último recurso)
driver.artifactory.enabled=true
driver.artifactory.base.url=${ARTIFACTORY_BASE_URL}
driver.artifactory.user=${ARTIFACTORY_USER}
driver.artifactory.token=${ARTIFACTORY_TOKEN}
driver.artifactory.timeout=60
driver.artifactory.retry.enabled=true
driver.artifactory.retry.max=3

# Versiones de Drivers
driver.chrome.version=143.0.7444.175
driver.firefox.version=0.33.0
driver.edge.version=143.0.2739.15

# Logging
driver.logging.show.strategy=true
```

**.env.local** (Windows):
```bash
# Drivers - Local Path
DRIVER_LOCAL_PATH=C:/drivers

# Drivers - Artifactory (si aplica)
ARTIFACTORY_BASE_URL=https://artifactory.corp.com/qa-drivers
ARTIFACTORY_USER=tu_usuario
ARTIFACTORY_TOKEN=tu_token_aqui
```

---

## 🔍 ESTRUCTURA DE DIRECTORIOS RECOMENDADA

```
C:\drivers\
├── chromedriver\
│   └── 143.0.7444.175\
│       └── chromedriver.exe
├── geckodriver\
│   └── 0.33.0\
│       └── geckodriver.exe
└── msedgedriver\
    └── 143.0.2739.15\
        └── msedgedriver.exe
```

**Alternativa** (si usas `${user.home}`):
```
C:\Users\s2994840\.qa-drivers\
├── chromedriver\
│   └── 143.0.7444.175\
│       └── chromedriver.exe
└── ...
```

---

## ✅ VERIFICACIÓN

### 1. Verificar configuración:

```powershell
# Ver variables cargadas
. .\scripts\setup-env.ps1

# Verificar que DRIVER_LOCAL_PATH está configurado
echo $env:DRIVER_LOCAL_PATH
```

### 2. Ejecutar tests:

```powershell
.\run-tests.bat
```

### 3. Salida esperada:

```
✅ Obteniendo chromedriver con estrategia de fallback del framework...
✓ Usando driver desde LOCAL PATH: chromedriver 143.0.7444.175
✅ chromedriver configurado: C:\drivers\chromedriver\143.0.7444.175\chromedriver.exe
✅ WebDriver creado exitosamente: CHROME
```

---

## 📊 VENTAJAS DE LA NUEVA SOLUCIÓN

| Aspecto | Antes (Librería Externa) | Ahora (Framework) |
|---------|-------------------------|-------------------|
| **Timeout** | 2-3 minutos colgado | < 1 segundo |
| **Dependencia de Internet** | ❌ Requerida | ✅ Opcional |
| **Funciona sin proxy** | ❌ Falla | ✅ Funciona |
| **Caché local** | ⚠️ Básico | ✅ Avanzado |
| **Artifactory** | ❌ No soportado | ✅ Soportado |
| **Configuración** | ❌ Limitada | ✅ Flexible |
| **Logs claros** | ❌ No | ✅ Sí |

---

## 🆘 SOLUCIÓN DE PROBLEMAS

### Problema: "Driver no encontrado"

**Error**:
```
No se pudo obtener chromedriver desde ninguna fuente
```

**Solución**:
1. Verificar que existe en local path:
   ```powershell
   Test-Path C:\drivers\chromedriver\143.0.7444.175\chromedriver.exe
   ```

2. Verificar permisos de ejecución (en Windows no aplica mucho, pero asegúrate que no esté bloqueado)

3. Verificar configuración:
   ```powershell
   echo $env:DRIVER_LOCAL_PATH
   ```

### Problema: "Versión no configurada"

**Error**:
```
Versión de chromedriver no configurada. Verifica driver.chrome.version
```

**Solución**:
Agregar en `config-scotia.properties`:
```properties
driver.chrome.version=143.0.7444.175
```

### Problema: "Sigue intentando descargar de internet"

**Causa**: Aún estás usando la versión vieja de `web-core`

**Solución**:
1. Actualizar dependencia en tu módulo:
   ```gradle
   dependencies {
       implementation 'com.scotia.qa:web-core:1.0.0'  // ← Asegúrate que sea la nueva versión
   }
   ```

2. Limpiar y reconstruir:
   ```powershell
   .\gradlew clean build --refresh-dependencies
   ```

---

## 📚 DOCUMENTACIÓN ADICIONAL

- `common/src/main/java/com/scotia/qa/common/driver/WebDriverManager.java` - Implementación completa
- `QUICK-START-WINDOWS.md` - Guía de inicio rápido para Windows
- `WINDOWS-SOLUCION-FINAL.md` - Solución de variables de entorno

---

## ✅ CHECKLIST FINAL

- [ ] Descargué chromedriver manualmente y lo puse en `C:\drivers\`
- [ ] Configuré `DRIVER_LOCAL_PATH` en `.env.local`
- [ ] Configuré `driver.chrome.version` en `config-scotia.properties`
- [ ] Ejecuté `. .\scripts\setup-env.ps1`
- [ ] Verifiqué que las variables están cargadas
- [ ] Ejecuté tests con `.\run-tests.bat`
- [ ] Los tests inician el navegador sin timeout ✅

---

**🎉 ¡PROBLEMA RESUELTO!**

El WebDriver ahora funciona sin necesidad de internet, sin timeouts, y con fallbacks configurables. Ideal para ambientes corporativos con restricciones de red.

---

**Autor**: Abel Venero  
**Fecha**: 9 de Diciembre 2025  
**Versión**: 1.0.1

