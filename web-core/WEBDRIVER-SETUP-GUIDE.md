# 🚀 Guía de Configuración de WebDrivers - Multiplataforma

## 📋 Resumen

El framework ahora incluye **configuración robusta y multiplataforma** de WebDrivers que funciona en:

- ✅ **Windows**
- ✅ **macOS** 
- ✅ **Linux**

Con soporte para:
- ✅ Ambientes **con/sin internet**
- ✅ Ambientes **con/sin proxy corporativo**
- ✅ Ambientes **con/sin firewall**
- ✅ Todos los navegadores: **Chrome, Firefox, Edge, Safari**

---

## 🎯 Estrategia de Fallback

El framework intenta configurar el driver automáticamente usando **5 niveles de fallback**:

```
1️⃣ Driver Manual → Si especificaste -Dwebdriver.chrome.driver=...
   ↓ (si falla)
2️⃣ WebDriverManager → Intenta descargar desde internet (timeout: 15s)
   ↓ (si falla)
3️⃣ Cache Local → Busca en ~/.cache/selenium/
   ↓ (si falla)
4️⃣ PATH Sistema → Busca en directorios comunes (C:/webdrivers/, /usr/local/bin/, etc.)
   ↓ (si falla)
5️⃣ Error Descriptivo → Mensaje claro con instrucciones específicas por SO
```

---

## 🔧 SOLUCIÓN PARA AMBIENTES CORPORATIVOS

### ❌ **Problema Típico:**

```
WebDriverManager no puede descargar ChromeDriver desde:
https://googlechromelabs.github.io/chrome-for-testing/

Causa: Firewall/Proxy corporativo bloqueando
```

---

### ✅ **Solución 1: Especificar Driver Manual** (RECOMENDADO)

#### **Windows:**

1. Descargar ChromeDriver desde: https://googlechromelabs.github.io/chrome-for-testing/
2. Verificar versión de Chrome instalado:
   ```cmd
   reg query "HKEY_CURRENT_USER\Software\Google\Chrome\BLBeacon" /v version
   ```
3. Descargar versión compatible y extraer a `C:\webdrivers\chromedriver.exe`
4. Configurar en `config-scotia.properties`:
   ```properties
   webdriver.chrome.driver=C:/webdrivers/chromedriver.exe
   ```
   O como System Property:
   ```cmd
   gradlew test -Dwebdriver.chrome.driver=C:/webdrivers/chromedriver.exe
   ```

#### **macOS:**

1. Descargar ChromeDriver: https://googlechromelabs.github.io/chrome-for-testing/
2. Verificar versión de Chrome:
   ```bash
   /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --version
   ```
3. Extraer y dar permisos:
   ```bash
   sudo mv chromedriver /usr/local/bin/
   sudo chmod +x /usr/local/bin/chromedriver
   xattr -d com.apple.quarantine /usr/local/bin/chromedriver  # Remover cuarentena de macOS
   ```

#### **Linux:**

```bash
wget https://chromedriver.storage.googleapis.com/LATEST_RELEASE
VERSION=$(cat LATEST_RELEASE)
wget https://chromedriver.storage.googleapis.com/$VERSION/chromedriver_linux64.zip
unzip chromedriver_linux64.zip
sudo mv chromedriver /usr/local/bin/
sudo chmod +x /usr/local/bin/chromedriver
```

---

### ✅ **Solución 2: Configurar Proxy Corporativo**

Si tu empresa usa proxy, configurarlo antes de ejecutar tests:

#### **Windows (PowerShell):**

```powershell
$env:http_proxyHost="proxy.empresa.com"
$env:http_proxyPort="8080"
.\gradlew test
```

#### **macOS/Linux:**

```bash
export http.proxyHost=proxy.empresa.com
export http.proxyPort=8080
./gradlew test
```

O directamente en Gradle:

```bash
./gradlew test -Dhttp.proxyHost=proxy.empresa.com -Dhttp.proxyPort=8080
```

---

### ✅ **Solución 3: Usar Cache Local**

Si alguien ya descargó el driver en esa máquina:

#### **Windows:**

```cmd
dir %USERPROFILE%\.cache\selenium
```

Si existe, el framework lo usará automáticamente.

#### **macOS/Linux:**

```bash
ls ~/.cache/selenium/
```

---

## 🌐 CONFIGURACIÓN POR NAVEGADOR

### **Chrome**

```properties
# config-scotia.properties
webdriver.browser=CHROME
webdriver.chrome.driver=C:/webdrivers/chromedriver.exe  # Opcional
```

### **Firefox**

```properties
webdriver.browser=FIREFOX
webdriver.gecko.driver=/usr/local/bin/geckodriver  # Opcional
```

Descargar desde: https://github.com/mozilla/geckodriver/releases

### **Edge**

```properties
webdriver.browser=EDGE
webdriver.edge.driver=C:/webdrivers/msedgedriver.exe  # Opcional
```

Descargar desde: https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/

### **Safari** (solo macOS)

```properties
webdriver.browser=SAFARI
```

Safari viene con driver preinstalado en macOS (no requiere descarga).

---

## 📂 DIRECTORIOS COMUNES POR SO

El framework busca automáticamente drivers en:

### **Windows:**
- `C:\webdrivers\`
- `C:\Program Files\webdrivers\`
- `C:\selenium\drivers\`
- `%LOCALAPPDATA%\Programs\webdrivers\`

### **macOS:**
- `/usr/local/bin/`
- `/usr/bin/`
- `/opt/homebrew/bin/`
- `~/webdrivers/`

### **Linux:**
- `/usr/local/bin/`
- `/usr/bin/`
- `/opt/selenium/drivers/`
- `~/webdrivers/`

---

## 🧪 VERIFICAR CONFIGURACIÓN

### **Test Rápido:**

```bash
# Windows
gradlew web-core:test --tests "*.WebDriverFactoryTest" -Dwebdriver.chrome.driver=C:/webdrivers/chromedriver.exe

# macOS/Linux
./gradlew web-core:test --tests "*.WebDriverFactoryTest"
```

### **Logs Esperados:**

```
✅ Usando chromedriver manual: C:/webdrivers/chromedriver.exe
✅ WebDriver creado exitosamente: CHROME
```

O si usa WebDriverManager:

```
🔄 Intentando configurar chromedriver con WebDriverManager...
✅ chromedriver configurado correctamente vía WebDriverManager
```

O si usa cache:

```
⚠️ WebDriverManager falló para chromedriver: Connection timeout
✅ Usando chromedriver desde cache: C:\Users\usuario\.cache\selenium\chromedriver\win64\120.0.6099.109\chromedriver.exe
```

---

## ❓ TROUBLESHOOTING

### **Problema: "Connection timed out: getsockopt"**

**Causa:** Firewall bloqueando `googlechromelabs.github.io`

**Solución:** Usar **Solución 1** (driver manual) arriba ☝️

---

### **Problema: "WebDriver no inicializado"**

**Causa:** Driver no encontrado en ninguna ubicación

**Solución:** Verificar ruta en logs y descargar manualmente

---

### **Problema: "This version of ChromeDriver only supports Chrome version XX"**

**Causa:** Versión del driver no coincide con navegador instalado

**Solución:** 
1. Verificar versión de Chrome instalado
2. Descargar ChromeDriver compatible
3. Especificar ruta manualmente

---

## 📚 RECURSOS

- **Chrome for Testing:** https://googlechromelabs.github.io/chrome-for-testing/
- **GeckoDriver (Firefox):** https://github.com/mozilla/geckodriver/releases
- **EdgeDriver:** https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/
- **WebDriverManager:** https://github.com/bonigarcia/webdrivermanager

---

## 🆘 SOPORTE

Si ninguna solución funciona:

1. Ejecutar con logs detallados:
   ```bash
   ./gradlew test --debug > test-logs.txt 2>&1
   ```

2. Buscar en logs:
   - ¿Qué URLs intentó conectar?
   - ¿Dónde buscó el driver?
   - ¿Qué directorios exploró?

3. Contactar al equipo de QA con los logs completos.

---

**✅ Con esta configuración, los tests web funcionan en Windows, Mac y Linux sin problemas!**

