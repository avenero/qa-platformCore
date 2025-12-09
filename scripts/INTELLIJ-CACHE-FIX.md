# 🔧 SOLUCIÓN: Errores de "Cannot resolve symbol" en IntelliJ IDEA

**Fecha**: 9 de Diciembre 2025  
**Problema**: IntelliJ marca errores en `DriverManager.java` - "Cannot resolve symbol 'WebDriver'"

---

## 🔴 PROBLEMA

Después de compilar exitosamente con Gradle, IntelliJ muestra errores:

```
Cannot resolve symbol 'WebDriver'
Cannot resolve method 'quit()'
```

**Archivo afectado**: `web-core/src/main/java/com/scotia/qa/webcore/driver/DriverManager.java`

---

## ✅ CAUSA

Este es un problema común de **caché del IDE** después de:
- Cambiar versiones de dependencias
- Publicar en Maven Local
- Cambiar configuración de repositorios
- Refactorizar estructura de módulos

**La compilación con Gradle funciona correctamente** ✅, pero IntelliJ no sincronizó las dependencias.

---

## 🚀 SOLUCIÓN (3 OPCIONES)

### Opción 1: Invalidar Caché del IDE (RECOMENDADO)

**En IntelliJ IDEA:**

1. **File** → **Invalidate Caches...**
2. Seleccionar:
   - ✅ **Invalidate and Restart**
   - ✅ **Clear file system cache and Local History**
   - ✅ **Clear downloaded shared indexes**
3. Click **Invalidate and Restart**
4. Esperar que IntelliJ reinicie y re-indexe el proyecto

**Tiempo**: 2-3 minutos

---

### Opción 2: Reimportar Proyecto Gradle

1. **View** → **Tool Windows** → **Gradle**
2. En la ventana de Gradle, click en el botón **Reload All Gradle Projects** (🔄)
3. Esperar que termine la sincronización
4. Si persiste, ir a **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Gradle**
5. Verificar que **Build and run using:** está en **Gradle** (no IntelliJ IDEA)

---

### Opción 3: Limpiar y Re-sincronizar Manualmente

**Desde terminal:**

```bash
cd /Users/abel.venero/Documents/qa-scotia-frameworks

# 1. Limpiar todo
./gradlew clean --refresh-dependencies

# 2. Eliminar directorios de build
rm -rf */build
rm -rf .gradle
rm -rf .idea

# 3. Re-generar configuración de IntelliJ
./gradlew cleanIdea idea

# 4. Abrir IntelliJ y reimportar el proyecto
```

**Luego en IntelliJ:**
1. **File** → **Close Project**
2. **Open** → Seleccionar el proyecto
3. Esperar sincronización automática

---

## 🔍 VERIFICACIÓN

### 1. Verificar que Gradle compila correctamente:

```bash
cd /Users/abel.venero/Documents/qa-scotia-frameworks
./gradlew :web-core:build -x test
```

**Salida esperada:**
```
BUILD SUCCESSFUL in Xs
```

✅ Si esto funciona, **el código está correcto**. El problema es solo del IDE.

### 2. Verificar dependencias de web-core:

```bash
./gradlew :web-core:dependencies | grep selenium
```

**Salida esperada:**
```
org.seleniumhq.selenium:selenium-java:4.27.0
```

### 3. Verificar que IntelliJ reconoce las clases:

Después de invalidar caché, en IntelliJ:
1. Abrir `DriverManager.java`
2. Hacer **Ctrl+Click** (Cmd+Click en Mac) sobre `WebDriver`
3. Debería abrir la clase de Selenium

✅ Si abre la clase, el problema está resuelto.

---

## 💡 PREVENCIÓN

Para evitar este problema en el futuro:

### 1. Configuración recomendada en IntelliJ:

**Settings** → **Build, Execution, Deployment** → **Build Tools** → **Gradle**

- ✅ **Build and run using**: Gradle
- ✅ **Run tests using**: Gradle
- ✅ **Use Gradle from**: 'gradle-wrapper.properties' file
- ✅ **Gradle JVM**: Project SDK (Java 21)

### 2. Después de publicar en Maven Local:

Siempre ejecutar:
```bash
./gradlew --refresh-dependencies
```

Y en IntelliJ: **Reload All Gradle Projects** (🔄)

### 3. Mantener sincronizado:

- Después de cambiar `build.gradle`: **Reload Gradle**
- Después de cambiar dependencias: **Refresh Dependencies**
- Si aparecen errores raros: **Invalidate Caches**

---

## 🆘 SI AÚN PERSISTE EL PROBLEMA

### Verificar Versión de Java en IntelliJ:

1. **File** → **Project Structure** → **Project**
2. Verificar:
   - **SDK**: 21 (Oracle OpenJDK version 21.x)
   - **Language level**: SDK default (21)

### Verificar Módulos:

1. **File** → **Project Structure** → **Modules**
2. Para cada módulo (common, api-core, web-core, mobile-core):
   - **Sources**: Verificar que `src/main/java` está marcado como **Sources**
   - **Dependencies**: Verificar que aparece `org.seleniumhq.selenium:selenium-java:4.27.0`

### Último Recurso - Proyecto Limpio:

```bash
# Cerrar IntelliJ completamente

cd /Users/abel.venero/Documents/qa-scotia-frameworks

# Eliminar TODA configuración de IntelliJ
rm -rf .idea
rm -rf *.iml
rm -rf */*.iml

# Eliminar caché de Gradle
rm -rf .gradle
rm -rf ~/.gradle/caches

# Re-abrir proyecto en IntelliJ
# IntelliJ detectará el proyecto Gradle automáticamente
```

---

## 📊 CHECKLIST DE RESOLUCIÓN

- [ ] Ejecuté `./gradlew clean --refresh-dependencies`
- [ ] Verifiqué que `./gradlew :web-core:build -x test` funciona ✅
- [ ] En IntelliJ: **File** → **Invalidate Caches** → **Invalidate and Restart**
- [ ] Esperé que IntelliJ termine de re-indexar (barra de progreso abajo)
- [ ] En Gradle tool window: Click en **Reload All Gradle Projects** (🔄)
- [ ] Abrí `DriverManager.java` y **NO** veo errores rojos
- [ ] Puedo hacer Ctrl+Click en `WebDriver` y abre la clase de Selenium ✅

---

## ✅ CONFIRMACIÓN FINAL

**Si después de seguir estos pasos:**
- ✅ No hay líneas rojas en `DriverManager.java`
- ✅ IntelliJ reconoce todas las clases de Selenium
- ✅ El autocompletado funciona correctamente

**¡Problema resuelto!** 🎉

---

**Nota Importante**: 
- **La compilación con Gradle SIEMPRE funcionó correctamente** ✅
- Este era solo un problema de sincronización del IDE
- Los módulos publicados en Maven Local están correctos y funcionales
- Los módulos de Windows que actualicen las dependencias funcionarán sin problemas

---

**Autor**: Abel Venero  
**Fecha**: 9 de Diciembre 2025  
**Versión**: 1.0.0

