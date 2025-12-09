# 🔧 Setup Environment - Configuración de Variables de Entorno

**Scripts**: `setup-env.sh` (macOS/Linux) + `setup-env.ps1` (Windows)  
**Versión**: 1.0.0  
**Propósito**: Cargar variables de entorno desde `.env.local` para ejecución de tests

---

## 📋 DESCRIPCIÓN

Estos scripts leen el archivo `.env.local` y exportan todas las variables al proceso actual del shell/PowerShell, permitiendo que:
- **IntelliJ IDEA** las reconozca si se abre desde esa terminal
- **Gradle** las use al ejecutar `./gradlew test`
- **Tests** accedan a las configuraciones (DB, API, etc.)

---

## 🎯 CUÁNDO USAR

### ✅ Debes usar setup-env cuando:
- Ejecutas tests desde **terminal** con `./gradlew test`
- Quieres abrir **IntelliJ desde terminal** con variables cargadas
- Trabajas en **ambientes locales** (no CI/CD)
- Necesitas configurar **credenciales de BD, APIs, etc.**

### ❌ NO necesitas setup-env cuando:
- Ejecutas en **Jenkins/GitLab CI** (variables en el servidor)
- Ya configuraste **IntelliJ Run Configurations** manualmente
- Usas **`config-scotia.properties`** en lugar de variables de entorno

---

## 📦 REQUISITOS PREVIOS

### 1. Tener `.env.local` configurado

```bash
# Si no existe, copiarlo desde el template
cp config/templates/.env.local.template .env.local

# Editar con tus valores reales
# (Ver sección "Variables Disponibles" abajo)
```

### 2. Asegurar que `.env.local` está en `.gitignore`

```bash
# .gitignore
.env.local
.env.*
```

---

## 🚀 USO

### 🍎 **macOS / Linux (Bash/Zsh)**

```bash
# 1. Cargar variables (IMPORTANTE: usa 'source' o '.')
source ./scripts/setup-env.sh

# Alternativa:
. ./scripts/setup-env.sh

# 2. Verificar que se cargaron
echo $DB_URL

# 3. Ejecutar tests
./gradlew test

# 4. O abrir IntelliJ desde esta terminal
idea .   # Si 'idea' está en PATH
```

**⚠️ IMPORTANTE**: Debes usar `source` o `.` para que las variables se exporten al shell actual. Si ejecutas con `./setup-env.sh`, las variables NO persistirán.

---

### 🪟 **Windows (PowerShell)**

```powershell
# 1. Cargar variables (IMPORTANTE: usa '.' al inicio)
. .\scripts\setup-env.ps1

# 2. Verificar que se cargaron
echo $env:DB_URL

# 3. Ejecutar tests
.\gradlew test

# 4. O abrir IntelliJ desde esta terminal
& "C:\Program Files\JetBrains\IntelliJ IDEA\bin\idea64.exe" .
```

**⚠️ IMPORTANTE**: Debes usar `. .\setup-env.ps1` (con punto inicial) para que las variables se exporten al proceso actual.

---

## 📝 VARIABLES DISPONIBLES

El archivo `.env.local` soporta estas categorías de variables:

### 🔧 **AMBIENTE**
```bash
TEST_ENV=QA          # Ambiente actual (qa, uat, prod)
CI=false             # Indica si es ejecución local o CI/CD
```

### 💾 **BASE DE DATOS**
```bash
DB_URL=jdbc:oracle:thin:@//host:port/service
DB_USER=tu_usuario
DB_PASS=tu_password
DB_DRIVER=oracle.jdbc.OracleDriver
```

### 🌐 **API TESTING**
```bash
API_BASE_URL=https://api-dev.your-app.com/v1
API_TOKEN=tu_token_api
```

### 🖥️ **WEB TESTING**
```bash
WEB_BASE_URL=https://dev.your-app.com
DRIVER_LOCAL_PATH=/path/to/drivers  # Opcional
```

### 📱 **MOBILE TESTING**
```bash
MOBILE_PLATFORM=android
MOBILE_DEVICE=emulator-5554
APP_PATH=/path/to/app.apk
```

### 📊 **JIRA / XRAY**
```bash
JIRA_USER=tu_usuario
JIRA_PASSWORD=tu_password
TEST_EXECUTION_ID=QAAUY-XXX
```

---

## 🔍 VERIFICACIÓN

Ambos scripts muestran un resumen interactivo al ejecutarse:

```
═══════════════════════════════════════════
  🔧 Configurar Variables de Entorno
═══════════════════════════════════════════

ℹ  Cargando variables desde: .env.local

✓  Variables cargadas: 12

───────────────────────────────────────────

Variables en .env.local:

    1. TEST_ENV               = QA
    2. DB_URL                 = jdbc:oracle:thin:@//...
    3. DB_USER                = tu_usuario
    4. DB_PASS                = ***HIDDEN***
    ...

───────────────────────────────────────────

Estado en el entorno actual:

✓  TEST_ENV (cargada)
✓  DB_URL (cargada)
✓  DB_USER (cargada)
✓  DB_PASS (cargada)

✓  Todas las variables están cargadas en el entorno actual

✓  Configuración de BD completa

───────────────────────────────────────────

✓  Variables de entorno configuradas exitosamente

ℹ  Ahora puedes ejecutar tests:

   ./gradlew test                    # Todos los tests
   ./gradlew test --tests '*Smoke*'  # Tests específicos

ℹ  O ejecutar desde IntelliJ (las variables ya están disponibles)
```

---

## 🎓 CASOS DE USO

### 📌 **Caso 1: Ejecutar Tests desde Terminal**

```bash
# macOS/Linux
source ./scripts/setup-env.sh
./gradlew test -Dcucumber.filter.tags="@smoke"

# Windows
. .\scripts\setup-env.ps1
.\gradlew test -Dcucumber.filter.tags="@smoke"
```

---

### 📌 **Caso 2: Abrir IntelliJ con Variables Cargadas**

```bash
# macOS/Linux
source ./scripts/setup-env.sh
idea .   # IntelliJ heredará las variables

# Windows
. .\scripts\setup-env.ps1
& "C:\Program Files\JetBrains\IntelliJ IDEA\bin\idea64.exe" .
```

Ahora puedes ejecutar tests desde IntelliJ sin configurar nada adicional.

---

### 📌 **Caso 3: Verificar Solo Variables (Sin Ejecutar)**

```bash
# Ver qué variables están configuradas
source ./scripts/setup-env.sh

# Luego ejecutar manualmente cuando quieras
./gradlew test
```

---

## 🐛 TROUBLESHOOTING

### ❌ **Problema: "Variables no encontradas"**

```bash
ERROR: Propiedad 'db.url' no configurada
```

**Solución**:
1. Verificar que `.env.local` existe: `ls -la .env.local`
2. Verificar contenido: `cat .env.local`
3. Volver a cargar: `source ./scripts/setup-env.sh`

---

### ❌ **Problema: "Script no hace efecto"**

Si ejecutaste sin `source` o `.`:
```bash
# ❌ INCORRECTO (no exporta variables)
./setup-env.sh

# ✅ CORRECTO
source ./scripts/setup-env.sh
```

---

### ❌ **Problema: "Variables desaparecen al cerrar terminal"**

**Esto es normal**. Las variables solo viven en la sesión actual.

**Soluciones**:
- Ejecutar `setup-env` cada vez que abras una nueva terminal
- O configurar IntelliJ Run Configurations con las variables
- O usar `config-scotia.properties` en lugar de variables de entorno

---

### ❌ **Problema en Windows: "Execution Policy"**

```powershell
. .\scripts\setup-env.ps1
# Error: cannot be loaded because running scripts is disabled
```

**Solución**:
```powershell
# Permitir scripts solo para esta sesión
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process

# Luego ejecutar
. .\scripts\setup-env.ps1
```

---

## 🔐 SEGURIDAD

### ✅ **Buenas Prácticas**

1. **NUNCA commitear `.env.local`**
   ```bash
   # Asegurar que está en .gitignore
   echo ".env.local" >> .gitignore
   ```

2. **Usar valores sensibles ocultos**
   - Los scripts ocultan automáticamente PASSWORD, TOKEN, SECRET
   - Nunca compartas screenshots con valores reales

3. **Rotar credenciales periódicamente**
   - Actualizar `.env.local` cuando cambien passwords

---

## 📊 COMPARACIÓN CON OTRAS OPCIONES

| Método | Ventajas | Desventajas |
|--------|----------|-------------|
| **setup-env** | ✅ Rápido<br>✅ Compatible terminal<br>✅ Abre IntelliJ con vars | ⚠️ Sesión temporal<br>⚠️ Requiere re-ejecutar |
| **IntelliJ Run Config** | ✅ Persistente en IDE<br>✅ No requiere terminal | ⚠️ Manual por cada config<br>⚠️ No funciona en terminal |
| **config-scotia.properties** | ✅ Auto-detectado<br>✅ No requiere export | ⚠️ Fácil commitear por error<br>⚠️ Un archivo más |

**Recomendación**: Usar `setup-env` para desarrollo local + IntelliJ Run Config como backup.

---

## 🔗 INTEGRACIÓN CON OTROS FLUJOS

```bash
# Ejemplo: Configurar + Ejecutar + Analizar
source ./scripts/setup-env.sh
./gradlew test
./scripts/analyze-results.sh  # Opcional: si lo copiaste al módulo
```

---

## 📚 RECURSOS ADICIONALES

- [README.md](README.md) - Documentación general de scripts
- [SCRIPTS-GUIDE.md](SCRIPTS-GUIDE.md) - Guía detallada completa
- [config/templates/.env.local.template](../config/templates/.env.local.template) - Template con todas las variables

---

**Última actualización**: Diciembre 8, 2025  
**Versión del script**: 1.0.0

