# 📁 Templates de Configuración - Framework Scotia QA v1.0.0

> Template consolidado para configuración de módulos de prueba

---

## 🎯 ¿Qué Hay Aquí?

Este directorio contiene **un solo template** que incluye **TODAS** las configuraciones posibles del framework:

- ✅ **WEB** - Selenium WebDriver, navegadores, timeouts
- ✅ **API** - REST clients, autenticación, SSL
- ✅ **MOBILE** - Appium, dispositivos, plataformas
- ✅ **DATABASE** - Conexiones, pools, test data finder
- ✅ **REPORTING** - Generación de reportes
- ✅ **LOGGING** - Configuración de logs

---

## 📦 Archivo Disponible

### `config-scotia.properties.template`

**Template consolidado** con todas las capas del framework en un solo archivo.

**Contiene:**
- Configuraciones para WEB, API, MOBILE, DATABASE
- Comentarios explicativos de cada propiedad
- Valores por defecto documentados
- Ejemplos de uso
- Referencias a variables de entorno

---

## 🚀 Cómo Usar

### Paso 1: Copiar el Template

```bash
# En tu módulo (ej: qa-banking)
cp /ruta/framework/common/src/main/resources/templates/config-scotia.properties.template \
   src/test/resources/config-qa.properties
```

### Paso 2: Configurar para Tu Módulo

```bash
# Editar el archivo
vi src/test/resources/config-qa.properties

# Descomenta SOLO las secciones que uses:
# - Si tu módulo es solo WEB → descomenta sección WEB
# - Si es API + Database → descomenta API y DATABASE
# - Si es híbrido (WEB+API) → descomenta ambas secciones
```

### Paso 3: Crear Archivo por Ambiente

```bash
# Crear config para cada ambiente
cp config-qa.properties config-dev.properties
cp config-qa.properties config-uat.properties

# Editar cada uno con valores específicos del ambiente
```

### Paso 4: Variables Sensibles en .env.local

```bash
# Crear archivo .env.local (NO commitear)
cat > .env.local <<EOF
# QA Environment
DB_USER_QA=qa_user
DB_PASS_QA=SecretPassword123!
API_TOKEN=abc123xyz789

# UAT Environment
DB_USER_UAT=uat_user
DB_PASS_UAT=UatPassword456!
EOF

# Cargar variables
source .env.local

# Ejecutar tests
./gradlew test -Denv=qa
```

---

## 📝 Ejemplo: Módulo Híbrido (WEB + API + Database)

```properties
# config-qa.properties (en tu módulo)

# ====================================================================
# MÓDULO
# ====================================================================
framework.module.name=BANKING
framework.module.type=HYBRID

# ====================================================================
# WEB CONFIGURATION
# ====================================================================
web.base.url=https://qa.banking.com
web.browser=chrome
web.headless=true
web.timeout.implicit=10
web.timeout.explicit=15

# ====================================================================
# API CONFIGURATION
# ====================================================================
api.base.url=https://api-qa.banking.com/v1
api.timeout.connection=5000
api.timeout.request=30000
api.ssl.verify=false

# ====================================================================
# DATABASE CONFIGURATION
# ====================================================================
db.url=jdbc:oracle:thin:@//qa-db:1521/TESTDB
db.username=${DB_USER_QA}
db.password=${DB_PASS_QA}
db.driver=oracle.jdbc.OracleDriver
db.pool.size.max=10
```

---

## 📝 Ejemplo: Módulo API Puro

```properties
# config-qa.properties (en tu módulo)

# ====================================================================
# MÓDULO
# ====================================================================
framework.module.name=API_USERS
framework.module.type=API

# ====================================================================
# API CONFIGURATION
# ====================================================================
api.base.url=https://api-qa.users.com/v1
api.timeout.request=30000
api.auth.type=bearer
api.auth.token=${API_TOKEN}

# ====================================================================
# REPORTING
# ====================================================================
report.enabled=true
report.output.dir=target/cucumber-reports
```

---

## ✅ Ventajas de Este Enfoque

1. **Un Solo Archivo por Ambiente**
   - config-dev.properties = TODO de desarrollo
   - config-qa.properties = TODO de QA
   - Fácil de mantener y versionar

2. **Visibilidad Completa**
   - Ves todas las configuraciones del ambiente en un lugar
   - No hay sorpresas en archivos ocultos

3. **Consistencia Garantizada**
   - No puedes tener web apuntando a QA y api a UAT por error
   - Cambiar ambiente = cambiar 1 archivo

4. **Flexibilidad Total**
   - Comenta/descomenta solo lo que necesites
   - Agregar nueva capa = descomentar sección

---

## 📚 Documentación Relacionada

- [Guía de Configuración de Módulos](../../../../doc/GUIA-CONFIGURACION-MODULOS.md)
- [Guía de Tags](../../../TAGS-GUIDE.md)
- [Framework Guide](../../../../FRAMEWORK-GUIDE.md)

---

## ⚠️ Notas Importantes

1. **Variables Sensibles**
   - ⚠️ NUNCA commitear passwords, tokens, credenciales
   - Usa siempre variables de entorno: `${DB_USER_QA}`
   - El archivo `.env.local` debe estar en `.gitignore`

2. **Un Archivo por Ambiente**
   - NO crear `config-web-qa.properties` y `config-api-qa.properties`
   - SÍ crear `config-qa.properties` con TODA la config de QA

3. **Comentar lo que NO Uses**
   - Si tu módulo no usa mobile, deja esa sección comentada
   - Mantiene el archivo limpio y claro

4. **Valores por Defecto**
   - Propiedades OPCIONAL tienen defaults en el framework
   - Solo configura lo que difiera del default

---

**Versión:** 1.0.0  
**Fecha:** 27 de noviembre de 2025  
**Autor:** Abnel Venero

