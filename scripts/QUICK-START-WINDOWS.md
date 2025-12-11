# ⚡ GUÍA RÁPIDA: setup-env-simple.ps1 (Windows)

**Script Simplificado para Windows - SIN Problemas de Encoding**

---

## 🎯 USO INMEDIATO (3 PASOS)

### 1️⃣ Copiar el script al módulo

```powershell
# Desde Downloads
cd C:\Users\s2994840\Downloads

# Copiar script simplificado
Copy-Item .\qa-scotia-frameworks\scripts\setup-env-simple.ps1 -Destination .\qa-module-autos\scripts\ -Force

# Ir al módulo
cd .\qa-module-autos
```

### 2️⃣ Crear .env.local (si no existe)

```powershell
# Verificar si existe
Test-Path .env.local

# Si NO existe, crear desde template
Copy-Item config\templates\.env.local.template -Destination .env.local

# Editar con valores reales
notepad .env.local
```

### 3️⃣ Ejecutar el script y tests

```powershell
# Permitir scripts (si aparece error)
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process

# Cargar variables - esto generará un archivo run-tests.bat
. .\scripts\setup-env.ps1

# Ejecutar tests usando el comando generado
.\run-tests.bat
```

**⚠️ IMPORTANTE:** En Windows, las variables NO se pasan automáticamente a Gradle. El script genera un archivo `run-tests.bat` con el comando correcto que incluye todas las variables como parámetros `-D`.

---

## ✅ SALIDA ESPERADA

```
=======================================
  Configurar Variables de Entorno
=======================================

  TEST_ENV = QA
  DB_URL = jdbc:oracle:thin:@//10.34.36.43:1628/Banking
  DB_USER = AVENERO_PROXY
  DB_PASS = ***HIDDEN***
  API_BASE_URL = https://...

Variables cargadas: 8

=======================================
  IMPORTANTE PARA WINDOWS
=======================================

En Windows, las variables NO se heredan automaticamente a Gradle.
Debes ejecutar tests con este comando:

.\gradlew test -DTEST_ENV="QA" -DDB_URL="jdbc:oracle:..." -DDB_USER="AVENERO_PROXY" -DDB_PASS="***" ...

O ejecuta este comando guardado:
  .\run-tests.bat

Archivo 'run-tests.bat' creado en el directorio actual.
```

---

## 🔍 VERIFICAR QUE FUNCIONÓ

```powershell
# Ver una variable específica
echo $env:DB_URL

# Listar todas las variables
Get-ChildItem Env: | Where-Object { $_.Name -match "DB_|API_|TEST_" }
```

---

## 🚀 FLUJO COMPLETO (COPY & PASTE)

```powershell
# Bloque completo - ejecutar todo junto
cd C:\Users\s2994840\Downloads
Copy-Item .\qa-scotia-frameworks\scripts\setup-env.ps1 -Destination .\qa-module-autos\scripts\ -Force
cd .\qa-module-autos
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process

# Generar comando con variables
. .\scripts\setup-env.ps1

# Ejecutar tests usando el archivo generado
.\run-tests.bat
```

**Explicación del flujo:**
1. `setup-env.ps1` lee `.env.local` y crea `run-tests.bat`
2. `run-tests.bat` contiene el comando completo con todas las variables como `-D`
3. Ejecutar `run-tests.bat` pasa correctamente las variables a Java/Gradle

---

## ❓ CARACTERÍSTICAS DEL SCRIPT

| Aspecto | Estado |
|---------|--------|
| **Encoding** | ✅ Sin problemas de caracteres especiales |
| **Complejidad** | ✅ Solo 80 líneas - Simple y directo |
| **Funcionalidad** | ✅ Carga todas las variables de .env.local |
| **Compatibilidad** | ✅ Todas las versiones de PowerShell |
| **Errores comunes** | ✅ Ninguno - Script robusto |

**Conclusión:** Script simple, robusto y sin complicaciones para cargar tus variables de entorno.

---

## 🔄 USO DIARIO

```powershell
# Cada vez que abras PowerShell:
cd C:\Users\s2994840\Downloads\qa-module-autos

# Generar run-tests.bat (solo si cambiaste .env.local)
. .\scripts\setup-env.ps1

# Ejecutar tests
.\run-tests.bat
```

**💡 TIP:** El archivo `run-tests.bat` se crea automáticamente cada vez que ejecutas `setup-env.ps1`. Si tus variables no cambian, puedes ejecutar directamente `.\run-tests.bat` sin volver a ejecutar el script.

---

## 💡 TIP: Crear un alias

```powershell
# Agregar a tu perfil de PowerShell
notepad $PROFILE

# Agregar esta línea:
function loadenv { . .\scripts\setup-env.ps1 }

# Guardar y cerrar

# Recargar perfil
. $PROFILE

# Ahora puedes usar simplemente:
loadenv
```

---

## 🆘 SI AÚN ASÍ FALLA

**El script es tan simple que si falla, es un problema de sistema:**

1. **Verificar PowerShell:**
   ```powershell
   $PSVersionTable.PSVersion
   # Debe ser 5.1 o superior
   ```

2. **Verificar archivo .env.local existe:**
   ```powershell
   Test-Path .env.local
   # Debe devolver True
   ```

3. **Verificar contenido de .env.local:**
   ```powershell
   Get-Content .env.local | Select-Object -First 10
   # Debe mostrar tus variables
   ```

4. **Ejecutar manualmente línea por línea:**
   ```powershell
   $ENV_FILE = ".env.local"
   Get-Content $ENV_FILE
   ```

---

## ✅ RESUMEN

- **Script súper simple**: solo 80 líneas
- **Sin problemas de encoding**: caracteres ASCII puros
- **Sin dependencias**: no usa funciones complejas
- **Mismo resultado**: carga tus variables de .env.local

**¿Listo para probarlo?** 🚀

---

**Autor**: Abel Venero  
**Versión**: 1.0.1  
**Fecha**: 8 de Diciembre 2025

