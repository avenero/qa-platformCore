# 📦 PUBLICACIÓN EN MAVEN LOCAL - API-CORE

## ✅ PUBLICACIÓN EXITOSA

**Fecha:** 11 de Noviembre 2025  
**Framework:** Scotia QA Framework  
**Módulo:** api-core  
**Versión:** 1.0.0

---

## 📊 INFORMACIÓN DE PUBLICACIÓN

```
Group ID:    com.scotia.qa
Artifact ID: api-core
Version:     1.0.0

Maven Coordinates:
com.scotia.qa:api-core:1.0.0
```

---

## 🎯 ¿QUÉ INCLUYE ESTE JAR?

### **📂 Contenido Directo:**
- ✅ **ApiSteps.java** - 26 steps de Cucumber funcionales para testing de APIs REST
- ✅ **Configuración de Cucumber** optimizada
- ✅ **Configuración de logging** para testing

### **📦 Dependencias Transitivas (incluidas automáticamente):**
Al usar `api-core`, automáticamente obtienes acceso a:

#### **Del módulo `common`:**
- ✅ **HttpClient** (BaseHttpClient) - Cliente HTTP con Unirest
- ✅ **AuthenticationService** (BaseAuthenticationManager) - Gestión de autenticación
- ✅ **ConfigurationProvider** - Lectura de configuraciones (YAML, JSON, Properties)
- ✅ **DataUtilities** - Manipulación de datos y variables
- ✅ **ValidationUtilities** - Validaciones robustas
- ✅ **TestLogger** - Sistema de logging
- ✅ **DatabaseService** - Acceso a bases de datos (MySQL, PostgreSQL, Oracle, SQL Server)

#### **Librerías externas:**
- Unirest (HTTP Client)
- Jackson (JSON/YAML)
- Cucumber (BDD)
- JUnit (Testing)
- HikariCP (Connection pooling)
- Logback (Logging)
- Y más...

---

## 🚀 CÓMO USAR EN TU PROYECTO

### **1. Crear un nuevo proyecto de automatización**

```bash
# Crear estructura de carpetas
mkdir scotia-api-tests
cd scotia-api-tests
mkdir -p src/test/{java,resources}/features
```

### **2. Crear `build.gradle`**

```groovy
plugins {
    id 'java'
}

group = 'com.scotia.tests'
version = '1.0.0'

repositories {
    mavenLocal()  // ← IMPORTANTE: Buscar primero en Maven local
    mavenCentral()
}

dependencies {
    // Framework Scotia QA - API Core
    testImplementation 'com.scotia.qa:api-core:1.0.0'
    
    // Cucumber para ejecución de tests
    testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.18.0'
    testRuntimeOnly 'org.junit.platform:junit-platform-suite:1.10.0'
}

test {
    useJUnitPlatform()
    
    systemProperties = [
        'cucumber.publish.quiet': 'true'
    ]
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

### **3. Crear tu primera Feature**

**Archivo:** `src/test/resources/features/login.feature`

```gherkin
# language: es
@api @login
Característica: Testing de API de Login

  Escenario: Login exitoso con credenciales válidas
    Dado establezco el host base como https://api-qa.scotia.com
    Y agrego el header Content-Type con valor application/json
    Y establezco el cuerpo JSON con los siguientes datos
      | username | testuser     |
      | password | P@ssw0rd123 |
    Cuando ejecuto una petición POST al endpoint /auth/login
    Entonces valido que el código de respuesta del servicio sea 200
    Y valido que la respuesta contenga el texto access_token
```

### **4. Crear Runner de Cucumber**

**Archivo:** `src/test/java/com/scotia/tests/TestRunner.java`

```java
package com.scotia.tests;

import org.junit.platform.suite.api.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = "cucumber.glue", value = "com.scotia.qa.apicore.steps")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty, html:target/cucumber-reports.html, json:target/cucumber.json")
public class TestRunner {
    // Esta clase solo sirve como punto de entrada para JUnit
}
```

### **5. Ejecutar los tests**

```bash
./gradlew test
```

---

## 📖 STEPS DISPONIBLES

### **Configuración de Endpoints:**
```gherkin
Dado configuro el endpoint usando "api.base.url" del archivo "config.yml"
Dado establezco el host base como https://api.example.com
Dado el host "https://api.example.com" mas el contexto "/v1"
```

### **Autenticación:**
```gherkin
Dado agrego autenticación Client Credentials
Dado agrego autenticación Bearer para RUT 12345678-9
Dado agrego el token personalizado mi_token_custom
Dado agrego autenticación básica con usuario "user" y password "pass"
```

### **Headers y Parámetros:**
```gherkin
Y agrego el header Content-Type con valor application/json
Y agrego el parámetro de consulta page con valor 1
Y agrego el field username con el valor "testuser"
Y agrego el queryparam "limit" con el valor "10"
```

### **Body:**
```gherkin
Dado establezco el cuerpo de la petición como
  """
  {"username": "test", "password": "123"}
  """

Dado establezco el cuerpo JSON con los siguientes datos
  | username | testuser     |
  | password | P@ssw0rd123 |
```

### **Ejecución:**
```gherkin
Cuando ejecuto una petición GET al endpoint /users
Cuando ejecuto una petición POST al endpoint /auth/login
Cuando ejecuto la consulta con el metodo "PUT"
```

### **Validaciones:**
```gherkin
Entonces valido que el código de respuesta del servicio sea 200
Entonces valido que la respuesta contenga el texto success
```

### **Variables:**
```gherkin
Dado almaceno el valor mi_valor como variable_test
Dado establezco la key "api_token" con el valor "{{token}}"
Dado el resultado almaceno el valor de "$.data.token"
```

### **Debugging:**
```gherkin
Entonces muestro la información de la última petición
```

---

## 🔧 CONFIGURACIÓN AVANZADA

### **Usar variables de entorno**

Todos los steps soportan reemplazo de variables con `{{variable}}`:

```gherkin
Dado establezco el host base como {{BASE_URL}}
Y agrego el header Authorization con valor {{API_TOKEN}}
```

**Variables desde:**
- Variables de entorno del sistema
- Valores almacenados con `DataUtilities.storeValue()`
- Valores extraídos de respuestas JSON

### **Archivo de configuración**

**Archivo:** `src/test/resources/config.yml`

```yaml
environments:
  qa:
    api:
      base_url: https://api-qa.scotia.com
      timeout: 30000
    auth:
      client_id: test_client_id
      client_secret: test_client_secret
  
  prod:
    api:
      base_url: https://api.scotia.com
      timeout: 60000
```

**Uso en feature:**
```gherkin
Dado configuro el endpoint usando "environments.qa.api.base_url" del archivo "config.yml"
```

---

## 📁 ESTRUCTURA DE PROYECTO RECOMENDADA

```
scotia-api-tests/
├── build.gradle
├── settings.gradle
├── src/
│   └── test/
│       ├── java/
│       │   └── com/scotia/tests/
│       │       ├── TestRunner.java
│       │       └── hooks/
│       │           └── TestHooks.java (opcional)
│       └── resources/
│           ├── features/
│           │   ├── login.feature
│           │   ├── users.feature
│           │   └── products.feature
│           ├── config.yml
│           └── logback.xml (opcional)
└── target/
    └── cucumber-reports.html
```

---

## 🎯 VENTAJAS DE USAR EL FRAMEWORK PUBLICADO

### ✅ **No necesitas código Java**
- Los 26 steps están listos para usar
- Solo escribes features en Gherkin
- El framework maneja toda la lógica

### ✅ **Actualización centralizada**
- Si actualizamos el framework, solo cambias la versión en `build.gradle`
- No necesitas copiar código entre proyectos

### ✅ **Dependencias gestionadas**
- Todas las librerías necesarias vienen incluidas
- No conflictos de versiones
- Maven gestiona todo automáticamente

### ✅ **Reutilización máxima**
- Mismo framework para todos los proyectos de Scotia
- Mismo framework podría usarse para Santander, Itaú, etc.
- Steps estandarizados

### ✅ **Mantenimiento simple**
- Un solo lugar donde actualizar (el framework)
- Todos los proyectos se benefician

---

## 🔄 ACTUALIZAR EL FRAMEWORK

### **1. Hacer cambios en el framework**
```bash
cd /Users/abel.venero/Downloads/qa-scotia-frameworks
# Hacer cambios en api-core o common
```

### **2. Incrementar versión**

**Archivo:** `build.gradle` (raíz)
```groovy
version = "1.0.1"  // ← Cambiar versión
```

### **3. Publicar nueva versión**
```bash
./gradlew clean :api-core:build :api-core:publishToMavenLocal
```

### **4. Actualizar en proyectos consumidores**

**En tu proyecto de tests:**
```groovy
dependencies {
    testImplementation 'com.scotia.qa:api-core:1.0.1'  // ← Nueva versión
}
```

```bash
./gradlew clean test --refresh-dependencies
```

---

## 📚 RECURSOS ADICIONALES

### **Documentación del Framework:**
- `api-core/README.md` - Documentación completa de api-core
- `api-core/steps.md` - Análisis detallado de todos los steps
- `api-core/REFACTORIZACION-COMPLETADA.md` - Resumen de cambios
- `common/README.md` - Documentación del módulo common

### **Ubicación del JAR publicado:**
```
~/.m2/repository/com/scotia/qa/api-core/1.0.0/
├── api-core-1.0.0.jar         ← JAR principal
├── api-core-1.0.0.pom         ← Metadatos Maven
└── api-core-1.0.0.module      ← Metadatos Gradle
```

---

## ⚠️ NOTAS IMPORTANTES

### **Maven Local vs Maven Central:**
- ✅ Publicado en **Maven Local** (repositorio local de tu máquina)
- ❌ **NO** está en Maven Central (repositorio público)
- 💡 Para compartir con el equipo, considera:
  - Publicar en Nexus/Artifactory interno de Scotia
  - O compartir el código fuente del framework

### **Compilación requerida:**
- Los proyectos que usen el framework necesitan Java 21
- Gradle 8.x recomendado

### **Steps comentados:**
- 18 steps están comentados (requieren implementación en common)
- Solo usa los 26 steps activos documentados arriba

---

## 🎓 EJEMPLO COMPLETO

**Feature completa:**

```gherkin
# language: es
@api @users
Característica: CRUD de Usuarios

  Antecedentes:
    Dado establezco el host base como https://api-qa.scotia.com
    Y agrego autenticación Client Credentials
    Y agrego el header Content-Type con valor application/json

  @create
  Escenario: Crear un nuevo usuario
    Dado establezco el cuerpo JSON con los siguientes datos
      | name     | Juan Pérez           |
      | email    | juan.perez@test.com  |
      | role     | admin                |
    Cuando ejecuto una petición POST al endpoint /api/v1/users
    Entonces valido que el código de respuesta del servicio sea 201
    Y valido que la respuesta contenga el texto id
    Y el resultado almaceno el valor de "$.data.id"
    Y almaceno el valor {{$.data.id}} como user_id

  @get
  Escenario: Obtener usuario por ID
    Cuando ejecuto una petición GET al endpoint /api/v1/users/{{user_id}}
    Entonces valido que el código de respuesta del servicio sea 200
    Y valido que la respuesta contenga el texto Juan
    Y muestro la información de la última petición
```

---

## 🚀 CONCLUSIÓN

**El framework `api-core` está listo para usar en producción:**

✅ Publicado exitosamente en Maven Local  
✅ 26 steps funcionales  
✅ Dependencias incluidas transitivamente  
✅ Documentación completa  
✅ Fácil de integrar en nuevos proyectos  
✅ Actualizable de forma centralizada  

**Próximo paso:** Crear tu primer proyecto de automatización usando el framework! 🎉

---

**Responsable:** Abel Venero  
**Fecha:** 11 de Noviembre 2025  
**Estado:** ✅ PUBLICADO Y LISTO PARA USO

