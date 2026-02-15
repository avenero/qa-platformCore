# 🔄 FLUJO DE GIT - BUENAS PRÁCTICAS

## 📊 SITUACIÓN ACTUAL

- ✅ `feature/warningsFixed` (local) - con cambios de vulnerabilidades
- ✅ `develop` (remota) - rama de integración
- ✅ `master` (remota) - rama de producción

---

## 📋 SECUENCIA CORRECTA

### **PASO 1: Asegurar que tienes los últimos cambios de develop**

```bash
git checkout develop
git pull origin develop
```

---

### **PASO 2: Mergear tu feature branch a develop**

```bash
git merge feature/warningsFixed
```

Si hay conflictos, resuelve y luego:

```bash
git add .
git commit -m "Merge feature/warningsFixed into develop"
```

---

### **PASO 3: Ejecutar tests en develop (CRÍTICO)**

```bash
./gradlew clean test
```

**⚠️ IMPORTANTE:** Si algo falla, corrige antes de seguir.

---

### **PASO 4: Push a develop remota**

```bash
git push origin develop
```

---

### **PASO 5: Actualizar master con los últimos cambios de develop**

```bash
git checkout master
git pull origin master
git merge develop
```

---

### **PASO 6: Ejecutar tests en master (DOBLE VERIFICACIÓN)**

```bash
./gradlew clean test
```

---

### **PASO 7: Push a master remota**

```bash
git push origin master
```

---

### **PASO 8: Crear tag de versión (RECOMENDADO)**

```bash
git tag -a v1.0.1 -m "fix: resolver 7 CVEs y mejorar compatibilidad Windows"
git push origin v1.0.1
```

---

### **PASO 9: Volver a develop para seguir trabajando**

```bash
git checkout develop
```

---

## 🎯 DIAGRAMA DEL FLUJO

```
feature/warningsFixed (local)
        ↓ merge
    develop (local)
        ↓ test ✅
    develop (remota) ← push
        ↓ merge
    master (local)
        ↓ test ✅
    master (remota) ← push
        ↓ tag
    v1.0.1 (tag) ← push
```

---

## ⚠️ REGLAS DE ORO

1. **NUNCA** hacer push directo a master sin pasar por develop
2. **SIEMPRE** ejecutar tests antes de mergear a master
3. **SIEMPRE** crear tags en master para versiones estables
4. **NUNCA** trabajar directo en master, siempre usar feature branches
5. **SIEMPRE** hacer pull antes de merge para evitar conflictos

---

## 💡 ALTERNATIVAS

### **Opción A: Si develop tiene cambios que no quieres**

Si `develop` remota tiene cambios de otros desarrolladores que NO quieres llevar a master aún:

```bash
# Cherry-pick solo tus commits
git checkout master
git cherry-pick <commit-hash-1> <commit-hash-2>
git push origin master
```

---

### **Opción B: Usar release branch**

```bash
git checkout develop
git checkout -b release/1.0.1
# revisar y ajustar
git checkout master
git merge release/1.0.1
git push origin master
```

---

## 🔍 COMANDOS DE VERIFICACIÓN

### **Ver en qué rama estás:**
```bash
git branch
```

### **Ver estado actual:**
```bash
git status
```

### **Ver último commit:**
```bash
git log --oneline -1
```

### **Ver diferencias antes de merge:**
```bash
git diff develop..feature/warningsFixed
```

### **Ver historial gráfico:**
```bash
git log --graph --oneline --all
```

---

## 📊 ESTRATEGIA DE RAMAS

### **Ramas permanentes:**
- `master` - Producción, solo código estable
- `develop` - Integración, cambios en desarrollo

### **Ramas temporales:**
- `feature/nombre` - Nueva funcionalidad
- `bugfix/nombre` - Corrección de bugs
- `hotfix/nombre` - Corrección urgente en producción
- `release/x.y.z` - Preparación de release

### **Ciclo de vida:**

```
1. Crear feature branch desde develop:
   git checkout develop
   git checkout -b feature/nueva-funcionalidad

2. Desarrollar y commitear cambios:
   git add .
   git commit -m "feat: descripción"

3. Mergear a develop cuando termine:
   git checkout develop
   git merge feature/nueva-funcionalidad

4. Eliminar feature branch:
   git branch -d feature/nueva-funcionalidad

5. Cuando develop esté estable, mergear a master:
   git checkout master
   git merge develop
   git tag -a v1.x.x -m "Release x.x.x"
   git push origin master --tags
```

---

## 🏷️ CONVENCIONES DE COMMITS

### **Formato:**
```
tipo: descripción breve

Descripción detallada (opcional)
```

### **Tipos:**
- `feat:` - Nueva funcionalidad
- `fix:` - Corrección de bug
- `docs:` - Cambios en documentación
- `refactor:` - Refactorización de código
- `test:` - Agregar o modificar tests
- `chore:` - Tareas de mantenimiento
- `perf:` - Mejoras de performance
- `style:` - Formateo, indentación

### **Ejemplos:**
```bash
git commit -m "fix: resolver CVE-2023-51074 en json-path"
git commit -m "feat: agregar soporte para SQL Server"
git commit -m "refactor: eliminar hardcode de Oracle en DbConnectorFactory"
git commit -m "test: agregar 179 tests unitarios para common module"
```

---

## 🚨 SOLUCIÓN DE PROBLEMAS

### **Conflictos en merge:**

```bash
# Ver archivos en conflicto
git status

# Resolver manualmente los archivos
# Luego:
git add .
git commit -m "Merge: resolver conflictos"
```

### **Deshacer último commit (local):**

```bash
git reset --soft HEAD~1  # Mantiene cambios
git reset --hard HEAD~1  # Descarta cambios
```

### **Deshacer push (PELIGROSO):**

```bash
# Revertir commit pero mantener historial
git revert <commit-hash>
git push origin develop
```

---

## 📋 CHECKLIST PRE-MERGE A MASTER

Antes de hacer `git push origin master`, verifica:

- [ ] Tests pasan: `./gradlew clean test`
- [ ] Build compila: `./gradlew clean build`
- [ ] Sin conflictos con master remota
- [ ] Versión actualizada en `gradle.properties`
- [ ] Changelog/documentación actualizada
- [ ] Sin archivos temporales o builds en staging

---

## 🎓 EJEMPLO COMPLETO

```bash
# 1. Traer últimos cambios de develop
git checkout develop
git pull origin develop

# 2. Mergear feature
git merge feature/warningsFixed

# 3. Verificar
./gradlew clean test

# 4. Push a develop
git push origin develop

# 5. Mergear develop a master
git checkout master
git pull origin master
git merge develop

# 6. Verificar en master
./gradlew clean test

# 7. Push a master
git push origin master

# 8. Crear tag
git tag -a v1.0.1 -m "fix: resolver 7 CVEs y mejorar compatibilidad Windows"
git push origin v1.0.1

# 9. Volver a develop
git checkout develop

# 10. Eliminar feature branch (opcional)
git branch -d feature/warningsFixed
```

---

## 🎯 FLUJO SIMPLIFICADO PARA ESTE CASO

```bash
# Paso rápido (si develop local está actualizada):
git checkout develop
git merge feature/warningsFixed
./gradlew clean test
git push origin develop

git checkout master
git merge develop
./gradlew clean test
git push origin master

git tag -a v1.0.1 -m "fix: vulnerabilidades resueltas"
git push origin v1.0.1

git checkout develop
```

---

**Duración estimada:** 5-10 minutos (dependiendo de la velocidad de los tests).

**Resultado:** Cambios en master remota con tag v1.0.1. ✅

---

**Última actualización:** 2026-02-15  
**Autor:** Abel Venero

