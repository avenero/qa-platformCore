# 🪟 Guía de Uso: setup-env.ps1 en Windows

**Última actualización**: 8 de Diciembre 2025  
**Script**: `setup-env.ps1`  
**Versión**: 1.0.1

---


## ✅ RESPUESTA RÁPIDA: ¿Necesito Permisos de Administrador?

**NO** ❌ - Este script **NO requiere permisos de administrador**.

El script solo:
- ✅ Lee un archivo de texto (`.env.local`)
- ✅ Configura variables de entorno en **TU sesión actual** de PowerShell
- ✅ No modifica el sistema, registro, ni configuraciones globales

---

## 📋 PRE-REQUISITOS

### 1. PowerShell 5.1 o superior

**Verificar versión:**
```powershell
$PSVersionTable.PSVersion
```

**Salida esperada:**
```
Major  Minor  Build  Revision
-----  -----  -----  --------
5      1      xxxxx  xxxx      ← Debe ser 5.1 o mayor
```

### 2. Archivo `.env.local` configurado

Debe existir en la raíz de tu módulo:
```
C:\proyectos\qa-module-autos\.env.local
```

Si no existe, copia el template:
```powershell
Copy-Item config\templates\.env.local.template -Destination .env.local
```

---

## 🚀 USO DEL SCRIPT

### ⚠️ IMPORTANTE: Usar Dot-Sourcing

**FORMA CORRECTA** ✅:
```powershell
. .\scripts\setup-env.ps1
```

**FORMA INCORRECTA** ❌:
```powershell
.\scripts\setup-env.ps1     # Las variables NO se exportarán
```

**¿Por qué el punto inicial?**
- El `.` (dot-sourcing) ejecuta el script en el **contexto actual** de PowerShell
- Las variables quedan disponibles en tu sesión
- Sin el `.`, las variables desaparecen al terminar el script

---

## 🛠️ EJECUCIÓN PASO A PASO

### Paso 1: Abrir PowerShell

**Opción A - Terminal de Windows:**
1. Presiona `Win + X`
2. Selecciona "Windows PowerShell" (NO necesitas Admin)

**Opción B - Terminal desde VS Code / IntelliJ:**
1. Abre terminal integrada
2. Asegúrate que sea PowerShell (no CMD)

### Paso 2: Navegar al módulo

```powershell
cd C:\proyectos\qa-module-autos
```

### Paso 3: Verificar que existe `.env.local`

```powershell
Test-Path .env.local
```

**Salida esperada:** `True`

Si sale `False`:
```powershell
# Copiar template
Copy-Item config\templates\.env.local.template -Destination .env.local

# Editar con valores reales
notepad .env.local
```

### Paso 4: Ejecutar el script

```powershell
. .\scripts\setup-env.ps1
```

**Salida esperada:**
```
═══════════════════════════════════════════
  🔧 Configurar Variables de Entorno
═══════════════════════════════════════════

ℹ  Cargando variables desde: .env.local

✓  Variables cargadas: 8

───────────────────────────────────────────

Variables en .env.local:

    1. TEST_ENV               = QA
    2. DB_URL                 = jdbc:oracle:thin:@//...
    3. DB_USER                = tu_usuario
    4. DB_PASS                = ***HIDDEN***
    5. API_BASE_URL           = https://...
    ...

───────────────────────────────────────────

Estado en el entorno actual:

✓  TEST_ENV (cargada)
✓  DB_URL (cargada)
✓  DB_USER (cargada)
✓  DB_PASS (cargada)
...

✓  Todas las variables están cargadas en el entorno actual
✓  Configuración de BD completa

───────────────────────────────────────────

✓  Variables de entorno configuradas exitosamente

ℹ  Ahora puedes ejecutar tests:

   .\gradlew test                    # Todos los tests
   .\gradlew test --tests '*Smoke*'  # Tests específicos

ℹ  O ejecutar desde IntelliJ (las variables ya están disponibles en este proceso)
```

### Paso 5: Verificar que las variables están cargadas

```powershell
# Verificar variable específica
echo $env:DB_URL

# Listar todas las variables cargadas
Get-ChildItem Env: | Where-Object { $_.Name -match "DB_|API_|TEST_" }
```

### Paso 6: Ejecutar tests

```powershell
.\gradlew test
```

---

## ⚠️ POSIBLES PROBLEMAS Y SOLUCIONES

### ❌ Problema 1: "Execution Policy"

**Error:**
```
.\setup-env.ps1 : File cannot be loaded because running scripts is disabled on this system.
```

**Solución:**
```powershell
# Permitir scripts SOLO para esta sesión (NO requiere Admin)
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process

# Luego ejecutar el script
. .\scripts\setup-env.ps1
```

**Explicación:**
- `-Scope Process` solo afecta la sesión actual
- No requiere permisos de administrador
- Al cerrar PowerShell, vuelve a la política anterior

---

### ❌ Problema 2: "Script setup-env.ps1 no encontrado"

**Error:**
```
El término '.\scripts\setup-env.ps1' no se reconoce como nombre de un cmdlet...
CommandNotFoundException
```

**Causa:**
El script no existe en tu módulo. Necesitas copiarlo desde el framework.

**Solución:**
```powershell
# Verificar ubicación del framework
Test-Path ..\qa-scotia-frameworks\scripts\setup-env.ps1

# Copiar desde el framework
Copy-Item ..\qa-scotia-frameworks\scripts\setup-env.ps1 -Destination .\scripts\ -Force

# Verificar que se copió
Test-Path .\scripts\setup-env.ps1

# Ahora ejecutar
. .\scripts\setup-env.ps1
```

**Si el framework está en otra ubicación:**
```powershell
# Ajustar la ruta según tu caso
Copy-Item C:\ruta\al\framework\scripts\setup-env.ps1 -Destination .\scripts\ -Force
```

---

### ❌ Problema 3: "Archivo .env.local no encontrado"

**Error:**
```
✗  Archivo .env.local no encontrado
```

**Solución:**
```powershell
# Copiar template
Copy-Item config\templates\.env.local.template -Destination .env.local

# Editar con valores reales
notepad .env.local
```

---

### ❌ Problema 4: Variables no persisten

**Síntoma:**
- El script se ejecuta sin errores
- Pero `echo $env:DB_URL` muestra vacío

**Causa:**
Ejecutaste el script sin el `.` inicial

**Solución:**
```powershell
# INCORRECTO ❌
.\scripts\setup-env.ps1

# CORRECTO ✅
. .\scripts\setup-env.ps1
```

---

### ❌ Problema 5: "Error de Parser / MissingTypename"

**Error:**
```
En setup-env.ps1: 131 Carácter: 51
Falta el nombre de tipo después de '['.
```

**Causa:**
El script tiene un problema de codificación de caracteres (comillas).

**Solución:**
El script ya fue corregido en la última versión. Si aún ves este error:

```powershell
# Re-descargar el script desde el framework actualizado
Copy-Item ..\qa-scotia-frameworks\scripts\setup-env.ps1 -Destination .\scripts\ -Force
```

---

### ❌ Problema 6: "Error cargando archivo"

**Error:**
```
✗  Error cargando archivo: Access denied
```

**Causas posibles:**
1. El archivo `.env.local` tiene caracteres especiales mal codificados
2. El archivo está abierto en otro programa

**Solución:**
```powershell
# Cerrar el archivo en todos los programas

# Verificar encoding (debe ser UTF-8)
Get-Content .env.local -Encoding UTF8 | Select-Object -First 5
```

---

## 🔄 FLUJO COMPLETO DE TRABAJO

### Primer Uso (Configuración Inicial)

```powershell
# 1. Navegar al módulo
cd C:\proyectos\qa-module-autos

# 2. Crear .env.local
Copy-Item config\templates\.env.local.template -Destination .env.local

# 3. Editar con valores reales
notepad .env.local

# 4. Permitir ejecución de scripts (si aparece error)
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process

# 5. Cargar variables
. .\scripts\setup-env.ps1

# 6. Ejecutar tests
.\gradlew test
```

### Uso Diario

```powershell
# 1. Abrir PowerShell
# 2. Navegar al módulo
cd C:\proyectos\qa-module-autos

# 3. Cargar variables (si necesitas)
. .\scripts\setup-env.ps1

# 4. Trabajar normalmente
.\gradlew test
```

---

## 💡 TIPS Y MEJORES PRÁCTICAS

### Tip 1: Alias para facilitar el uso

Crear un alias en tu perfil de PowerShell:

```powershell
# Editar perfil
notepad $PROFILE

# Agregar esta línea:
function Load-Env { . .\scripts\setup-env.ps1 }
Set-Alias -Name loadenv -Value Load-Env

# Guardar y cerrar

# Recargar perfil
. $PROFILE

# Ahora puedes usar:
loadenv
```

### Tip 2: Verificar variables antes de ejecutar

```powershell
# Crear función helper
function Test-EnvLoaded {
    if ($env:DB_URL) {
        Write-Host "✓ Variables cargadas" -ForegroundColor Green
    }
    else {
        Write-Host "✗ Variables NO cargadas. Ejecuta: . .\scripts\setup-env.ps1" -ForegroundColor Red
    }
}

# Usar antes de ejecutar tests
Test-EnvLoaded
.\gradlew test
```

### Tip 3: Abrir IntelliJ con variables cargadas

```powershell
# Cargar variables
. .\scripts\setup-env.ps1

# Abrir IntelliJ desde este PowerShell
& "C:\Program Files\JetBrains\IntelliJ IDEA\bin\idea64.exe" .

# IntelliJ heredará las variables de entorno
```

---

## 🔒 SEGURIDAD

### ✅ Lo que hace el script (SEGURO):

- Lee archivo de texto (`.env.local`)
- Establece variables en el **proceso actual** (`Process` scope)
- No modifica configuración del sistema
- No requiere permisos elevados
- Las variables desaparecen al cerrar PowerShell

### ❌ Lo que NO hace el script:

- NO modifica el registro de Windows
- NO establece variables de sistema (`Machine` scope)
- NO establece variables de usuario (`User` scope)
- NO ejecuta código remoto
- NO descarga archivos de internet
- NO requiere conexión a red

---

## 📊 VERIFICACIÓN FINAL

### Checklist de Verificación:

```powershell
# ✓ PowerShell versión 5.1+
$PSVersionTable.PSVersion

# ✓ Archivo .env.local existe
Test-Path .env.local

# ✓ Script existe
Test-Path .\scripts\setup-env.ps1

# ✓ Cargar variables
. .\scripts\setup-env.ps1

# ✓ Verificar variable
echo $env:DB_URL

# ✓ Ejecutar tests
.\gradlew test
```

---

## 🆘 SOPORTE

Si sigues teniendo problemas:

1. **Verificar encoding del archivo:**
   ```powershell
   Get-Content .env.local -Raw | Format-Hex | Select-Object -First 1
   ```

2. **Revisar logs del script:**
   El script muestra información detallada de lo que hace

3. **Verificar permisos del archivo:**
   ```powershell
   Get-Acl .env.local | Format-List
   ```

4. **Ejecutar con verbose:**
   ```powershell
   . .\scripts\setup-env.ps1 -Verbose
   ```

---

## 📚 RECURSOS ADICIONALES

- [SETUP-ENV-GUIDE.md](SETUP-ENV-GUIDE.md) - Guía completa multiplataforma
- [README.md](README.md) - Documentación general de scripts
- [PowerShell Execution Policies](https://docs.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies)

---

**✅ RESUMEN:**
- **NO requiere permisos de administrador**
- Usar con `. .\scripts\setup-env.ps1` (nota el punto inicial)
- Las variables solo existen en la sesión actual de PowerShell
- Seguro y sin modificaciones al sistema

**Autor**: Abel Venero  
**Versión**: 1.0.0  
**Fecha**: 8 de Diciembre 2025

