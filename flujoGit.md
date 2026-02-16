# 🔄 FLUJO DE GIT - BUENAS PRÁCTICAS

## ⚠️ CONCEPTO CLAVE - SIEMPRE COMMIT ANTES DE MERGE

### **❌ ERROR COMÚN:**
```bash
# Estás en feature/mi-funcionalidad con cambios SIN commitear
git checkout develop        # ❌ MAL - los cambios se "mueven" contigo
git merge feature/mi-funcionalidad  # ❌ No hay nada nuevo que mergear
```

### **✅ FLUJO CORRECTO:**
```bash
# Estás en feature/mi-funcionalidad con cambios
git add .
git commit -m "feat: mi funcionalidad completa"  # ✅ PRIMERO commit
git checkout develop        # ✅ Cambias de rama limpio
git merge feature/mi-funcionalidad  # ✅ Ahora SÍ hay commits que mergear
```

---

## 📊 SITUACIÓN ACTUAL

- ✅ `feature/FixWarnnings` (local) - con cambios de vulnerabilidades
- ✅ `develop` (remota) - rama de integración
- ✅ `master` (remota) - rama de producción

---

## 📋 SECUENCIA CORRECTA DETALLADA

### **FASE 1: TRABAJAR EN FEATURE BRANCH**

#### **PASO 1.1: Crear feature branch (si no existe)**
```bash
git checkout develop
git pull origin develop
git checkout -b feature/mi-funcionalidad
```

#### **PASO 1.2: Desarrollar y hacer commits frecuentes**
```bash
# Hacer cambios en el código...
git add .
git commit -m "feat: implementar parte 1"

# Más cambios...
git add .
git commit -m "feat: implementar parte 2"

# Correcciones...
git add .
git commit -m "fix: corregir bug en parte 1"
```

**⚠️ IMPORTANTE:** Haz commits **FRECUENTEMENTE** en tu feature branch.

#### **PASO 1.3: Verificar que TODO está commiteado**
```bash
git status
```

**Resultado esperado:**
```
On branch feature/mi-funcionalidad
nothing to commit, working tree clean  ← ✅ ESTO ES CRÍTICO
```

**Si ves archivos modificados (M):**
```
M   archivo1.java
M   archivo2.java
```

**Debes commitear ANTES de continuar:**
```bash
git add .
git commit -m "feat: completar funcionalidad"
```

---

### **FASE 2: INTEGRAR A DEVELOP**

#### **PASO 2.1: Actualizar develop local**
```bash
git checkout develop
git pull origin develop
```

**Verificar estado:**
```bash
git status
```

**Resultado esperado:**
```
On branch develop
Your branch is up to date with 'origin/develop'.
nothing to commit, working tree clean  ← ✅ Limpio
```

**⚠️ Si ves archivos modificados (M) aquí:**
```
M   archivo1.java  ← ❌ ERROR - olvidaste commitear en feature branch
```

**Solución:**
```bash
# Volver a feature branch
git checkout feature/mi-funcionalidad

# Commitear los cambios
git add .
git commit -m "feat: cambios finales"

# Ahora sí, volver a develop
git checkout develop
```

---

#### **PASO 2.2: Mergear feature branch a develop**
```bash
git merge feature/mi-funcionalidad
```

**Resultado esperado:**
```
Updating abc123..def456
Fast-forward
 archivo1.java | 10 +++++
 archivo2.java | 5 +++
 2 files changed, 15 insertions(+)
```

**Si dice "Already up to date":**
- Significa que `feature/mi-funcionalidad` no tiene commits nuevos
- Probablemente olvidaste commitear los cambios en la feature branch

---

#### **PASO 2.3: Ejecutar tests en develop (CRÍTICO)**
```bash
./gradlew clean test
```

**Si fallan tests:**
```bash
# Corregir el código
git add .
git commit -m "fix: corregir tests"
```

---

#### **PASO 2.4: Push a develop remota**
```bash
git push origin develop
```

---

### **FASE 3: LLEVAR A MASTER (PRODUCCIÓN)**

#### **PASO 3.1: Actualizar master local**
```bash
git checkout master
git pull origin master
```

---

#### **PASO 3.2: Mergear develop a master**
```bash
git merge develop
```

**Si hay conflictos:**
```bash
# Resolver conflictos manualmente en los archivos
git add .
git commit -m "merge: resolver conflictos develop → master"
```

---

#### **PASO 3.3: Ejecutar tests en master (DOBLE VERIFICACIÓN)**
```bash
./gradlew clean test
```

**⚠️ CRÍTICO:** Los tests **DEBEN pasar** antes de pushear a master.

---

#### **PASO 3.4: Push a master remota**
```bash
git push origin master
```

---

#### **PASO 3.5: Crear tag de versión**
```bash
git tag -a v1.0.1 -m "fix: resolver 7 CVEs y mejorar compatibilidad Windows"
git push origin v1.0.1
```

---

#### **PASO 3.6: Volver a develop**
```bash
git checkout develop
```

---

#### **PASO 3.7: Sincronizar develop con master (IMPORTANTE)**

Después de hacer cambios directos en master (como el commit SSL), debes sincronizar develop:

```bash
git checkout develop
git merge master
git push origin develop
```

**¿Por qué?** Para que develop tenga TODOS los cambios que master tiene.

---

#### **PASO 3.8: Eliminar feature branch (opcional)**
```bash
git branch -d feature/mi-funcionalidad
```

---

## 🎯 DIAGRAMA DEL FLUJO COMPLETO

```
┌─────────────────────────────────────────────────────────┐
│ FASE 1: DESARROLLO EN FEATURE BRANCH                   │
└─────────────────────────────────────────────────────────┘

feature/mi-funcionalidad
    ↓ git add . && git commit -m "..."  (MÚLTIPLES VECES)
feature/mi-funcionalidad (con commits)
    ↓ git status → "nothing to commit" ✅
    ↓ git checkout develop

┌─────────────────────────────────────────────────────────┐
│ FASE 2: INTEGRACIÓN EN DEVELOP                         │
└─────────────────────────────────────────────────────────┘

develop (local)
    ↓ git pull origin develop
develop (actualizada)
    ↓ git merge feature/mi-funcionalidad
develop (con nuevos commits)
    ↓ ./gradlew clean test ✅
develop (tests pasan)
    ↓ git push origin develop
develop (remota) ✅

┌─────────────────────────────────────────────────────────┐
│ FASE 3: PRODUCCIÓN EN MASTER                           │
└─────────────────────────────────────────────────────────┘

master (local)
    ↓ git pull origin master
master (actualizada)
    ↓ git merge develop
master (con cambios de develop)
    ↓ ./gradlew clean test ✅
master (tests pasan)
    ↓ git push origin master
master (remota) ✅
    ↓ git tag -a v1.0.1 -m "..."
    ↓ git push origin v1.0.1
v1.0.1 (tag) ✅

┌─────────────────────────────────────────────────────────┐
│ FASE 4: SINCRONIZACIÓN Y LIMPIEZA                      │
└─────────────────────────────────────────────────────────┘

    ↓ git checkout develop
develop (local)
    ↓ git merge master (sincronizar cambios de master)
    ↓ git push origin develop
develop (sincronizada) ✅
    ↓ git branch -d feature/mi-funcionalidad
feature branch eliminada ✅
```

---

## ⚠️ REGLAS DE ORO

1. **SIEMPRE** commitear cambios en feature branch ANTES de hacer checkout
2. **NUNCA** cambiar de rama con cambios sin commitear (se "mueven" contigo)
3. **SIEMPRE** verificar `git status` antes de cambiar de rama
4. **SIEMPRE** ejecutar tests antes de mergear a develop
5. **SIEMPRE** ejecutar tests antes de mergear a master
6. **SIEMPRE** crear tags en master para versiones estables
7. **SIEMPRE** sincronizar develop con master después de cambios directos en master
8. **NUNCA** trabajar directamente en master

---

## 🔍 VERIFICACIÓN EN CADA PASO

### **Antes de cambiar de rama:**
```bash
git status
```

**✅ Resultado esperado:**
```
On branch feature/mi-funcionalidad
nothing to commit, working tree clean
```

**❌ Si ves esto, NO cambies de rama todavía:**
```
On branch feature/mi-funcionalidad
Changes not staged for commit:
  M   archivo1.java
  M   archivo2.java
```

**Solución:**
```bash
git add .
git commit -m "feat: descripción"
# Ahora sí, cambiar de rama
git checkout develop
```

---

### **Después de merge:**
```bash
git log --oneline -5
```

**Debe mostrar los commits de feature branch en develop:**
```
def456 (HEAD -> develop) feat: implementar parte 2
abc123 feat: implementar parte 1
...
```

---

### **Antes de push a master:**
```bash
git log --oneline master..develop
```

**Debe mostrar los commits que se van a integrar:**
```
def456 feat: implementar parte 2
abc123 feat: implementar parte 1
```

**Si no muestra nada, develop y master ya están sincronizadas.**

---

## 💡 EJEMPLO REAL CON TU CASO

### **SITUACIÓN ACTUAL (lo que pasó):**

```bash
# Estabas en feature/FixWarnnings
# Hiciste cambios (SSL, warnings, etc.)
# ❌ NO hiciste commit
git checkout develop
# Los cambios se "movieron" a develop (aparecen como M)

git merge feature/FixWarnnings
# Already up to date (porque no hay commits nuevos en feature)
```

---

### **LO QUE DEBISTE HACER:**

```bash
# PASO 1: En feature/FixWarnnings - COMMITEAR PRIMERO
git add .
git commit -m "fix: resolver warnings unchecked y SSL Javadoc"

# PASO 2: Verificar que está limpio
git status
# → nothing to commit, working tree clean ✅

# PASO 3: Cambiar a develop
git checkout develop
# → Switched to branch 'develop' (sin archivos M)

# PASO 4: Mergear feature
git merge feature/FixWarnnings
# → Mergeando commits (no "Already up to date")

# PASO 5: Push a develop
git push origin develop
```

---

## 🚀 SOLUCIÓN PARA TU SITUACIÓN ACTUAL

Como ya tienes los cambios en develop (sin commitear), hazlo así:

```bash
# PASO 1: Commitear los cambios en develop
git add .
git commit -m "fix: eliminar warnings unchecked y configurar SSL para Javadoc

- Agregar @SuppressWarnings en BaseConfigurationProvider
- Agregar @SuppressWarnings en JiraUpdateService
- Agregar @SuppressWarnings en DataUtilities
- Configurar SSL truststore para Javadoc via jFlags
- Resolver errores PKIX en Javadoc Windows/Mac"

# PASO 2: Push a develop remota
git push origin develop

# PASO 3: Tests (opcional, ya los ejecutaste antes)
./gradlew clean test

# PASO 4: Mergear develop a master
git checkout master
git pull origin master
git merge develop

# PASO 5: Tests en master
./gradlew clean test

# PASO 6: Push a master
git push origin master

# PASO 7: Tag de versión
git tag -a v1.0.2 -m "fix: eliminar warnings y SSL Javadoc Windows"
git push origin v1.0.2

# PASO 8: Volver a develop y sincronizar
git checkout develop
git merge master
git push origin develop
```

---

## 🎓 FLUJO IDEAL PASO A PASO

### **ESCENARIO: Desarrollar nueva funcionalidad**

```bash
# ═══════════════════════════════════════════════════════════
# FASE 1: CREAR Y TRABAJAR EN FEATURE BRANCH
# ═══════════════════════════════════════════════════════════

# 1.1 Crear feature branch desde develop actualizada
git checkout develop
git pull origin develop
git checkout -b feature/nueva-funcionalidad

# 1.2 Hacer cambios en el código
# (editar archivos...)

# 1.3 COMMITEAR cambios (FRECUENTEMENTE)
git add .
git commit -m "feat: implementar parte 1"

# 1.4 Más cambios...
# (editar más archivos...)

# 1.5 COMMITEAR de nuevo
git add .
git commit -m "feat: implementar parte 2"

# 1.6 Correcciones...
# (editar archivos...)

# 1.7 COMMITEAR correcciones
git add .
git commit -m "fix: corregir bug en parte 1"

# 1.8 VERIFICAR que TODO está commiteado (CRÍTICO)
git status

# ✅ DEBE decir:
# On branch feature/nueva-funcionalidad
# nothing to commit, working tree clean

# ❌ Si dice "Changes not staged for commit", hacer:
git add .
git commit -m "feat: cambios finales"

# ═══════════════════════════════════════════════════════════
# FASE 2: INTEGRAR A DEVELOP
# ═══════════════════════════════════════════════════════════

# 2.1 Cambiar a develop
git checkout develop
# ✅ Debe decir: "Switched to branch 'develop'" (sin archivos M)

# 2.2 Actualizar develop con cambios remotos
git pull origin develop

# 2.3 Mergear feature branch a develop
git merge feature/nueva-funcionalidad

# ✅ DEBE decir algo como:
# Updating abc123..def456
# Fast-forward
#  archivo1.java | 10 +++++
#  2 files changed, 15 insertions(+)

# ❌ Si dice "Already up to date":
# → Olvidaste commitear en feature branch
# → Vuelve a feature branch y commitea:
#   git checkout feature/nueva-funcionalidad
#   git add .
#   git commit -m "feat: cambios finales"
#   git checkout develop
#   git merge feature/nueva-funcionalidad

# 2.4 Ejecutar tests en develop (CRÍTICO)
./gradlew clean test

# ✅ Si pasan:
# BUILD SUCCESSFUL

# ❌ Si fallan:
# Corregir el código
git add .
git commit -m "fix: corregir tests"
./gradlew clean test  # Verificar de nuevo

# 2.5 Push a develop remota
git push origin develop

# ═══════════════════════════════════════════════════════════
# FASE 3: LLEVAR A MASTER (PRODUCCIÓN)
# ═══════════════════════════════════════════════════════════

# 3.1 Cambiar a master
git checkout master

# 3.2 Actualizar master local
git pull origin master

# 3.3 Mergear develop a master
git merge develop

# 3.4 Ejecutar tests en master (DOBLE VERIFICACIÓN)
./gradlew clean test

# ✅ DEBEN pasar TODOS los tests antes de continuar

# 3.5 Push a master remota
git push origin master

# 3.6 Crear tag de versión
git tag -a v1.0.2 -m "feat: nueva funcionalidad implementada"
git push origin v1.0.2

# ═══════════════════════════════════════════════════════════
# FASE 4: SINCRONIZACIÓN Y LIMPIEZA
# ═══════════════════════════════════════════════════════════

# 4.1 Volver a develop
git checkout develop

# 4.2 Sincronizar develop con master
# (Por si hubo commits directos en master)
git merge master

# 4.3 Push a develop remota
git push origin develop

# 4.4 Eliminar feature branch local
git branch -d feature/nueva-funcionalidad

# 4.5 Verificar estado final
git status

# ✅ DEBE decir:
# On branch develop
# Your branch is up to date with 'origin/develop'.
# nothing to commit, working tree clean
```

---

## 🔑 CONCEPTOS CLAVE

### **1. Los cambios SIN commitear se "mueven" contigo**

```bash
# Ejemplo:
# Estás en feature/mi-rama con cambios sin commitear
git status
# → M   archivo.java (modificado, sin commitear)

git checkout develop
# → Los cambios se mueven a develop

git status
# → M   archivo.java (mismo archivo modificado ahora en develop)
```

**Problema:** El archivo modificado **NO está en ninguna rama** - está en tu working directory.

**Solución:** Siempre commitear antes de cambiar de rama.

---

### **2. Merge solo funciona con COMMITS**

```bash
git merge feature/mi-rama
```

**Esto mergea:** Los **COMMITS** de `feature/mi-rama` que no están en la rama actual.

**NO mergea:** Archivos modificados sin commitear.

**Ejemplo:**

```bash
# En feature/mi-rama
echo "cambio" >> archivo.txt
# NO hacer commit

git checkout develop
git merge feature/mi-rama
# → Already up to date (porque no hay commits nuevos)
```

---

### **3. "Already up to date" significa:**

```
La rama actual YA contiene TODOS los commits de la rama que intentas mergear.
```

**Causas comunes:**
- Ya hiciste el merge antes
- Olvidaste commitear en la feature branch
- La feature branch no tiene commits nuevos

---

## 📋 CHECKLIST ANTES DE CADA ACCIÓN

### **Antes de cambiar de rama:**
- [ ] `git status` → debe decir "nothing to commit, working tree clean"
- [ ] Si hay archivos modificados, hacer `git add . && git commit -m "..."`

### **Antes de hacer merge:**
- [ ] `git pull` en la rama destino (develop o master)
- [ ] Verificar que feature branch tiene commits: `git log develop..feature/mi-rama`

### **Antes de push a develop:**
- [ ] Tests pasan: `./gradlew clean test`
- [ ] No hay archivos sin commitear: `git status`

### **Antes de push a master:**
- [ ] Tests pasan: `./gradlew clean test`
- [ ] Sin conflictos con master remota
- [ ] Versión actualizada en `gradle.properties` (si aplica)
- [ ] Documentación actualizada (si aplica)

---

## 🚨 SOLUCIÓN DE PROBLEMAS COMUNES

### **Problema 1: "Already up to date" al mergear**

**Causa:** No hay commits nuevos en feature branch.

**Diagnóstico:**
```bash
git log develop..feature/mi-rama
# Si no muestra nada → No hay commits nuevos
```

**Solución:**
```bash
# Volver a feature branch
git checkout feature/mi-rama

# Ver estado
git status
# Si hay archivos M → commitear
git add .
git commit -m "feat: cambios finales"

# Volver a develop y mergear
git checkout develop
git merge feature/mi-rama
```

---

### **Problema 2: Archivos modificados (M) al cambiar de rama**

**Causa:** Cambios sin commitear en la rama anterior.

**Solución:**
```bash
# Volver a la rama anterior
git checkout feature/mi-rama

# Commitear
git add .
git commit -m "feat: guardar cambios"

# Ahora sí cambiar de rama
git checkout develop
```

---

### **Problema 3: Conflictos al mergear**

**Síntomas:**
```
Auto-merging archivo.java
CONFLICT (content): Merge conflict in archivo.java
Automatic merge failed; fix conflicts and then commit the result.
```

**Solución:**
```bash
# Ver archivos en conflicto
git status

# Editar manualmente los archivos y resolver conflictos
# Buscar marcadores: <<<<<<<, =======, >>>>>>>

# Después de resolver
git add .
git commit -m "merge: resolver conflictos"
```

---

### **Problema 4: Cambios directos en master que no están en develop**

**Causa:** Hiciste commits en master sin pasar por develop.

**Solución:**
```bash
# Sincronizar develop con master
git checkout develop
git merge master
git push origin develop
```

---

## 🎯 FLUJO RESUMIDO (TU CASO ACTUAL)

```bash
# PASO 1: Commitear cambios actuales en develop
git add .
git commit -m "fix: eliminar warnings unchecked y configurar SSL para Javadoc"

# PASO 2: Push a develop remota
git push origin develop

# PASO 3: Llevar a master
git checkout master
git pull origin master
git merge develop
./gradlew clean test

# PASO 4: Push a master
git push origin master
git tag -a v1.0.2 -m "fix: warnings y SSL Javadoc"
git push origin v1.0.2

# PASO 5: Sincronizar develop con master
git checkout develop
git merge master
git push origin develop
```

---

## 📊 COMPARACIÓN: CORRECTO VS INCORRECTO

### **❌ FLUJO INCORRECTO:**

```bash
# En feature branch con cambios
git checkout develop              # ❌ Cambios se mueven
git merge feature/mi-rama         # ❌ "Already up to date"
git add .                         # ❌ Commiteando en develop directamente
git commit -m "..."               # ❌ Perdiste el historial de feature
```

---

### **✅ FLUJO CORRECTO:**

```bash
# En feature branch con cambios
git add .                         # ✅ Commitear primero
git commit -m "..."               # ✅ Guardar en feature branch
git checkout develop              # ✅ Cambias limpio
git merge feature/mi-rama         # ✅ Mergea commits de feature
git push origin develop           # ✅ Historial limpio
```

---

## 🎓 MEJORES PRÁCTICAS

### **1. Commits pequeños y frecuentes en feature branch:**
```bash
git commit -m "feat: agregar clase X"
git commit -m "feat: agregar método Y"
git commit -m "test: agregar tests para X"
git commit -m "fix: corregir bug en Y"
```

### **2. Siempre verificar antes de cambiar de rama:**
```bash
git status  # ← SIEMPRE antes de checkout
```

### **3. Usar alias para verificación rápida:**
```bash
# En ~/.gitconfig o ~/.zshrc
alias gs='git status'
alias gcheck='git status && echo "✅ OK para cambiar de rama" || echo "❌ Commitea primero"'
```

### **4. Commits descriptivos:**
```bash
# ✅ BIEN:
git commit -m "fix: resolver CVE-2023-51074 en json-path"

# ❌ MAL:
git commit -m "cambios"
git commit -m "fix"
```

---

## 🔍 CÓMO VERIFICAR SI DEVELOP Y MASTER REMOTAS ESTÁN SINCRONIZADAS

### **COMANDO 1: Ver diferencias entre develop y master remotas**

```bash
# Actualizar referencias remotas primero
git fetch origin

# Ver commits que están en develop pero NO en master
git log origin/master..origin/develop --oneline

# Ver commits que están en master pero NO en develop
git log origin/develop..origin/master --oneline
```

**✅ Si ambos comandos NO muestran nada:**
```
# (vacío) - Sin output
```
**→ Las ramas están SINCRONIZADAS** ✅

**❌ Si el primer comando muestra commits:**
```
abc123 feat: nueva funcionalidad
def456 fix: corregir bug
```
**→ develop tiene commits que master NO tiene** (develop está adelante)

**❌ Si el segundo comando muestra commits:**
```
xyz789 fix: hotfix en master
```
**→ master tiene commits que develop NO tiene** (master está adelante)

---

### **COMANDO 2: Comparar último commit de cada rama**

```bash
# Ver último commit de develop remota
git fetch origin
git log origin/develop --oneline -1

# Ver último commit de master remota
git log origin/master --oneline -1
```

**✅ Si muestran el MISMO commit hash:**
```
abc123 (origin/develop, origin/master) fix: último cambio
```
**→ Están EXACTAMENTE iguales** ✅

**❌ Si muestran commits diferentes:**
```
abc123 (origin/develop) fix: cambio en develop
xyz789 (origin/master) fix: cambio en master
```
**→ NO están sincronizadas** ❌

---

### **COMANDO 3: Ver estado visual de las ramas**

```bash
# Actualizar referencias remotas
git fetch origin

# Ver grafo de commits
git log --oneline --graph --all --decorate -10
```

**✅ Resultado si están sincronizadas:**
```
* abc123 (HEAD -> develop, origin/master, origin/develop, master) fix: último cambio
* def456 feat: funcionalidad anterior
* xyz789 fix: corrección
```

**Nota:** `origin/master` y `origin/develop` apuntan al **mismo commit** (abc123).

**❌ Resultado si NO están sincronizadas:**
```
* abc123 (HEAD -> develop, origin/develop) fix: cambio solo en develop
| * xyz789 (origin/master, master) fix: cambio solo en master
|/
* def456 feat: ancestro común
```

**Nota:** `origin/master` y `origin/develop` apuntan a commits **diferentes**.

---

### **COMANDO 4: Comparación detallada de archivos**

```bash
# Ver TODOS los archivos diferentes entre develop y master remotas
git fetch origin
git diff origin/master..origin/develop --name-only
```

**✅ Si NO muestra nada:**
```
# (vacío)
```
**→ No hay diferencias de archivos** ✅

**❌ Si muestra archivos:**
```
archivo1.java
archivo2.java
build.gradle
```
**→ Estos archivos son diferentes entre develop y master** ❌

---

### **COMANDO 5: Ver diferencias de código línea por línea**

```bash
# Ver diferencias completas de código
git fetch origin
git diff origin/master..origin/develop
```

**✅ Si NO muestra nada:**
```
# (vacío)
```
**→ Ramas idénticas** ✅

**❌ Si muestra diferencias:**
```diff
diff --git a/build.gradle b/build.gradle
index abc123..def456 100644
--- a/build.gradle
+++ b/build.gradle
@@ -10,3 +10,5 @@
 version = "1.0.0"
+// Nuevo cambio solo en develop
```
**→ Hay cambios en develop que no están en master** ❌

---

### **COMANDO 6: Estadísticas de diferencias**

```bash
# Ver resumen estadístico
git fetch origin
git diff --stat origin/master..origin/develop
```

**✅ Si NO muestra nada:**
```
# (vacío)
```
**→ Sin diferencias** ✅

**❌ Si muestra estadísticas:**
```
 archivo1.java | 10 +++++-----
 archivo2.java |  5 +++++
 2 files changed, 15 insertions(+), 5 deletions(-)
```
**→ 2 archivos diferentes** ❌

---

## 🎯 COMANDO RÁPIDO TODO-EN-UNO

```bash
# Verificación completa en un solo comando
git fetch origin && \
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" && \
echo "📊 COMPARACIÓN DEVELOP vs MASTER (remotas)" && \
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" && \
echo "" && \
echo "🔹 Último commit en DEVELOP:" && \
git log origin/develop --oneline -1 && \
echo "" && \
echo "🔹 Último commit en MASTER:" && \
git log origin/master --oneline -1 && \
echo "" && \
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" && \
echo "📋 Commits en DEVELOP que NO están en MASTER:" && \
git log origin/master..origin/develop --oneline || echo "(ninguno)" && \
echo "" && \
echo "📋 Commits en MASTER que NO están en DEVELOP:" && \
git log origin/develop..origin/master --oneline || echo "(ninguno)" && \
echo "" && \
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" && \
echo "📁 Archivos diferentes:" && \
git diff --name-only origin/master..origin/develop || echo "(ninguno)" && \
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
```

**Interpretación del resultado:**

**✅ Ramas sincronizadas:**
```
🔹 Último commit en DEVELOP:
abc123 fix: último cambio

🔹 Último commit en MASTER:
abc123 fix: último cambio

📋 Commits en DEVELOP que NO están en MASTER:
(ninguno)

📋 Commits en MASTER que NO están en DEVELOP:
(ninguno)

📁 Archivos diferentes:
(ninguno)
```

**❌ Ramas desincronizadas:**
```
🔹 Último commit en DEVELOP:
abc123 fix: cambio en develop

🔹 Último commit en MASTER:
xyz789 fix: cambio en master

📋 Commits en DEVELOP que NO están en MASTER:
abc123 fix: cambio en develop

📋 Commits en MASTER que NO están en DEVELOP:
xyz789 fix: cambio en master

📁 Archivos diferentes:
build.gradle
archivo1.java
```

---

## 🛠️ COMANDOS PARA SINCRONIZAR

### **Si develop está adelante de master:**

```bash
# Llevar cambios de develop a master
git checkout master
git pull origin master
git merge origin/develop
./gradlew clean test
git push origin master

# Sincronizar develop también
git checkout develop
git pull origin develop
```

---

### **Si master está adelante de develop:**

```bash
# Llevar cambios de master a develop
git checkout develop
git pull origin develop
git merge origin/master
git push origin develop
```

---

### **Si ambas tienen cambios diferentes:**

```bash
# Decidir cuál tiene prioridad (normalmente master)
git checkout develop
git pull origin develop
git merge origin/master
# Resolver conflictos si los hay
git add .
git commit -m "merge: sincronizar master → develop"
git push origin develop
```

---

## 📖 GLOSARIO

- **Working Directory:** Archivos modificados sin agregar al staging area (sin `git add`)
- **Staging Area:** Archivos agregados con `git add` listos para commit
- **Commit:** Snapshot permanente de cambios guardado en el historial
- **Branch:** Línea de desarrollo independiente
- **Merge:** Integrar commits de una rama a otra
- **Fast-forward:** Merge sin conflictos (solo avanza el puntero)
- **Tag:** Marca permanente en un commit específico (ej: v1.0.1)
- **origin/develop:** Referencia local a la rama develop remota
- **origin/master:** Referencia local a la rama master remota

---

**Última actualización:** 2025-02-15  
**Autor:** Abel Venero

