# 🎯 SOLUCIÓN FINAL PARA WINDOWS

**Fecha**: 9 de Diciembre 2025  
**Problema**: Variables de entorno no se pasan a Gradle en Windows

---

## 🔴 PROBLEMA IDENTIFICADO

Cuando ejecutas `. .\scripts\setup-env.ps1`, las variables se configuran en PowerShell, pero **Gradle lanza un proceso Java nuevo** que **NO hereda** esas variables.

**Error que veías:**
```
⚠️ Variable de entorno 'DB_URL' no encontrada
Driver oracle.jdbc.OracleDriver claims to not accept jdbcUrl, ${DB_URL}
```

---

## ✅ SOLUCIÓN IMPLEMENTADA

El script ahora **genera automáticamente** un archivo `run-tests.bat` con el comando correcto que pasa **todas las variables como parámetros `-D` a Gradle**.

---

## 🚀 CÓMO USARLO (3 PASOS)

### Paso 1: Ejecutar setup-env.ps1

```powershell
cd C:\Users\s2994840\Downloads\qa-module-autos
. .\scripts\setup-env.ps1
```

**Salida:**
```
=======================================
  Configurar Variables de Entorno
=======================================

  TEST_ENV = QA
  DB_URL = jdbc:oracle:thin:@//10.34.36.43:1628/Banking
  DB_USER = AVENERO_PROXY
  DB_PASS = ***HIDDEN***

Variables cargadas: 8

=======================================
  IMPORTANTE PARA WINDOWS
=======================================

En Windows, las variables NO se heredan automaticamente a Gradle.
Debes ejecutar tests con este comando:

.\gradlew test -DTEST_ENV="QA" -DDB_URL="jdbc:oracle:..." ...

O ejecuta este comando guardado:
  .\run-tests.bat

Archivo 'run-tests.bat' creado en el directorio actual.
```

### Paso 2: Verificar que se creó run-tests.bat

```powershell
Test-Path .\run-tests.bat
# Debe devolver: True

# Ver contenido (opcional)
Get-Content .\run-tests.bat
```

### Paso 3: Ejecutar tests

```powershell
.\run-tests.bat
```

---

## 📝 ¿QUÉ HACE EL SCRIPT?

### Archivo: `setup-env.ps1`

1. ✅ Lee `.env.local`
2. ✅ Carga variables en PowerShell (para verificación)
3. ✅ **Genera `run-tests.bat`** con el comando completo
4. ✅ Incluye todas las variables como `-DVAR="valor"`

### Archivo: `run-tests.bat` (generado automáticamente)

Contenido ejemplo:
```batch
@echo off
.\gradlew test -DTEST_ENV="QA" -DDB_URL="jdbc:oracle:thin:@//10.34.36.43:1628/Banking" -DDB_USER="AVENERO_PROXY" -DDB_PASS="Leo101224*2026*."
```

---

## 🔍 VERIFICACIÓN

### Antes de ejecutar tests:

```powershell
# Verificar que variables están en PowerShell
echo $env:DB_URL

# Verificar que run-tests.bat existe
Test-Path .\run-tests.bat

# Ver contenido de run-tests.bat
Get-Content .\run-tests.bat
```

### Durante la ejecución:

Al ejecutar `.\run-tests.bat`, las variables se pasan correctamente a Java y **NO verás más el error** `${DB_URL}` sin resolver.

---

## 💡 USO DIARIO

```powershell
# 1. Navegar al módulo
cd C:\Users\s2994840\Downloads\qa-module-autos

# 2. Generar run-tests.bat (solo necesario si cambiaste .env.local)
. .\scripts\setup-env.ps1

# 3. Ejecutar tests
.\run-tests.bat
```

**💡 TIP:** Una vez generado `run-tests.bat`, puedes ejecutarlo directamente sin volver a correr `setup-env.ps1` **a menos que cambies el .env.local**.

---

## 🎯 FLUJO VISUAL

```
┌─────────────────────────────────────────────────────────────┐
│  1. Usuario ejecuta: . .\scripts\setup-env.ps1             │
│                                                             │
│  2. Script lee: .env.local                                  │
│     TEST_ENV=QA                                             │
│     DB_URL=jdbc:oracle:...                                  │
│     DB_USER=AVENERO_PROXY                                   │
│     DB_PASS=Leo101224*2026*.                                │
│                                                             │
│  3. Script genera: run-tests.bat                            │
│     @echo off                                               │
│     .\gradlew test -DTEST_ENV="QA" -DDB_URL="jdbc:..." ...  │
│                                                             │
│  4. Usuario ejecuta: .\run-tests.bat                        │
│                                                             │
│  5. Gradle recibe variables correctamente                   │
│     ✅ ConfigManager encuentra DB_URL                       │
│     ✅ Conexión a BD funciona                               │
│     ✅ Tests ejecutan sin errores                           │
└─────────────────────────────────────────────────────────────┘
```

---

## ❓ PREGUNTAS FRECUENTES

### ¿Por qué no funciona directamente `.\gradlew test`?

En Windows, cuando PowerShell lanza Gradle (Java), las variables configuradas con `Process` scope **no se heredan** al proceso hijo. Java solo ve variables del **sistema** o **usuario**, no del proceso.

### ¿Por qué usar un archivo .bat en lugar de pasar las variables directamente?

Porque el comando completo con todas las variables es muy largo y difícil de escribir manualmente. El script lo genera automáticamente por ti.

### ¿Puedo editar run-tests.bat?

Sí, pero cada vez que ejecutes `setup-env.ps1` se regenerará. Si quieres cambios permanentes, edita `.env.local`.

### ¿Funciona en macOS/Linux también?

No, este es específico para Windows. En macOS/Linux usa:
```bash
source ./scripts/setup-env.sh
./gradlew test
```

---

## 🆘 SOLUCIÓN DE PROBLEMAS

### Problema: "run-tests.bat no se crea"

**Causa:** Error al ejecutar setup-env.ps1

**Solución:**
```powershell
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
. .\scripts\setup-env.ps1
```

### Problema: "Variables aún aparecen como ${DB_URL}"

**Causa:** Estás ejecutando `.\gradlew test` directamente en lugar de `.\run-tests.bat`

**Solución:**
```powershell
# INCORRECTO ❌
.\gradlew test

# CORRECTO ✅
.\run-tests.bat
```

### Problema: "Access Denied al crear run-tests.bat"

**Causa:** Permisos de archivo

**Solución:**
```powershell
# Eliminar archivo existente
Remove-Item .\run-tests.bat -Force

# Volver a ejecutar
. .\scripts\setup-env.ps1
```

---

## ✅ CHECKLIST FINAL

- [ ] `.env.local` existe y tiene valores reales
- [ ] Ejecuté `. .\scripts\setup-env.ps1` (con punto inicial)
- [ ] Se creó el archivo `run-tests.bat`
- [ ] Verificué contenido de `run-tests.bat`
- [ ] Ejecuto tests con `.\run-tests.bat` (no con `.\gradlew test`)
- [ ] Tests ejecutan sin errores de variables

---

**🎉 ¡PROBLEMA RESUELTO!**

Con este flujo, las variables se pasan correctamente a Gradle en Windows y los tests funcionarán sin errores de configuración.

---

**Autor**: Abel Venero  
**Fecha**: 9 de Diciembre 2025  
**Versión**: 1.0.1

