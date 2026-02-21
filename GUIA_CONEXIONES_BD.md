# 🗄️ GESTIÓN DE CONEXIONES A BASE DE DATOS - Documentación Completa

**Última actualización:** 20 de Febrero 2026  
**Versión Framework:** 1.1.0  
**Estado:** ✅ Refactorización completada y validada

---

## 📋 ÍNDICE

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Arquitectura](#arquitectura)
3. [Componentes](#componentes)
4. [Configuración](#configuración)
5. [Steps de Cucumber](#steps-de-cucumber)
6. [Ejemplos de Uso](#ejemplos-de-uso)
7. [Troubleshooting](#troubleshooting)

---

## 📊 RESUMEN EJECUTIVO

### ✅ Capacidades Actuales

- ✅ **Multi-BD:** Conectar a múltiples BDs en el mismo test (Oracle, SQL Server, PostgreSQL, MySQL)
- ✅ **Cache automático:** Reutilización de conexiones sin recrearlas
- ✅ **Windows Authentication:** Soporte completo para SQL Server con integratedSecurity
- ✅ **Detección automática:** Driver detectado por URL JDBC
- ✅ **Steps genéricos:** Sin conocimiento de negocio, reutilizables
- ✅ **Configuración declarativa:** En config-{env}.properties con variables de entorno
- ✅ **Pool de conexiones:** HikariCP con configuración optimizada
- ✅ **Steps súper limpios:** Sin lógica, sin try-catch (3-7 líneas cada uno)

---

## 🏗️ ARQUITECTURA

```
┌─────────────────────────────────────────────────────────────┐
│                    CUCUMBER STEPS                           │
│              DatabaseConnectionSteps.java                   │
│         (SOLO coordinación - 3-7 líneas por step)          │
└─────────────────────────────────────────────────────────────┘
                            ↓ delega a
┌─────────────────────────────────────────────────────────────┐
│                    HELPER (LÓGICA)                          │
│                DatabaseHelper.java                          │
│   - executeQuery() - executeStatement()                     │
│   - getColumnValue() - validateHasResults()                 │
│   (Try-catch, PreparedStatement, ResultSet, logging)        │
└─────────────────────────────────────────────────────────────┘
                            ↓ usa
┌─────────────────────────────────────────────────────────────┐
│              FACTORY + MANAGER (CACHE)                      │
│              DbConnectorFactory.java                        │
│   - connectAndCache() - disconnect()                        │
│   - getConnectorFromConfigManager()                         │
└─────────────────────────────────────────────────────────────┘
                            ↓ crea
┌─────────────────────────────────────────────────────────────┐
│                    CONECTORES                               │
│  OracleConnector │ SQLServerConnector │ PostgreSQLConnector │
│                    MySQLConnector                           │
└─────────────────────────────────────────────────────────────┘
                            ↓ usa
┌─────────────────────────────────────────────────────────────┐
│                  POOL CONEXIONES                            │
│                 HikariCP (DatabaseConfig)                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 🧩 COMPONENTES

### 1️⃣ DbConnectorFactory (Factory + Manager)

**Ubicación:** `common/src/main/java/com/scotia/qa/common/database/factory/DbConnectorFactory.java`

**Responsabilidades:**
- ✅ Crear conectores por tipo de BD
- ✅ Gestionar cache de conexiones activas
- ✅ Leer configuración desde ConfigManager o System Properties
- ✅ Detectar driver automáticamente
- ✅ Cerrar conexiones (individual o todas)

**Métodos principales:**

```java
// ⭐ RECOMENDADO: Para Steps de Cucumber
DatabaseConnector connectAndCache(String dbType)  // Conecta y cachea
DatabaseConnector getCachedConnector(String dbType) // Obtiene del cache
void disconnect(String dbType)                     // Desconecta BD específica
void disconnectAll()                               // Cierra todas las conexiones

// Legacy (compatibilidad hacia atrás)
DatabaseConnector createFromConfig()               // Lee db.* de ConfigManager
DatabaseConnector getConnector(String dbType)      // Lee {dbType}.db.* de System Properties
DatabaseConnector create(url, user, pass, driver)  // Parámetros explícitos
```

---

### 2️⃣ DatabaseHelper

**Ubicación:** `common/src/main/java/com/scotia/qa/common/database/helpers/DatabaseHelper.java`

**Responsabilidades:**
- ✅ Encapsular TODA la lógica SQL
- ✅ Gestionar PreparedStatement y ResultSet
- ✅ Manejar try-catch con SQLException
- ✅ Logging estructurado
- ✅ Validaciones con excepciones claras

**Métodos principales:**

```java
// Ejecución
Map<String, Object> executeQuery(connector, query, parameters)
int executeStatement(connector, sql, parameters)

// Extracción
Object getColumnValue(queryResult, columnName)
int getRowCount(queryResult)
boolean hasResults(queryResult)

// Validaciones
void validateHasResults(queryResult)
void validateNoResults(queryResult)
void validateColumnValue(queryResult, columnName, expectedValue)
```

---

### 3️⃣ DatabaseConnectionSteps

**Ubicación:** `common/src/main/java/com/scotia/qa/common/database/steps/DatabaseConnectionSteps.java`

**Responsabilidades:**
- ✅ Proveer steps genéricos de Cucumber
- ✅ SOLO coordinación (SIN lógica)
- ✅ Delegar TODO al helper
- ✅ Gestionar ScenarioContext
- ✅ Cerrar conexiones automáticamente (@After)

**Steps disponibles:** (Ver sección Steps de Cucumber)

---

### 4️⃣ QueryRepository

**Ubicación:** `common/src/main/java/com/scotia/qa/common/database/repository/QueryRepository.java`

**Responsabilidades:**
- ✅ Métodos genéricos para ejecutar queries SQL
- ✅ Conversión ResultSet → Map/List
- ✅ Prepared Statements
- ✅ **SIN steps de Cucumber** (solo infraestructura)

**Métodos principales:**

```java
// Una fila
Map<String, Object> queryForMap(String sql, Object... params)

// Múltiples filas
List<Map<String, Object>> queryForList(String sql, Object... params)

// Mapeo a objetos custom
<T> T queryForObject(String sql, ResultSetMapper<T> mapper, Object... params)

// Contar
Long count(String sql, Object... params)

// Modificar
int execute(String sql, Object... params)
```

---

### 5️⃣ Conectores Específicos

**Ubicación:** `common/src/main/java/com/scotia/qa/common/database/connectors/`

- `OracleConnector.java` - Oracle DB
- `SQLServerConnector.java` - SQL Server (Windows Auth + SQL Auth)
- `PostgreSQLConnector.java` - PostgreSQL
- `MySQLConnector.java` - MySQL
- `BaseConnector.java` - Clase base compartida

Todos extienden `BaseConnector` e implementan `DatabaseConnector`.

---

## ⚙️ CONFIGURACIÓN

### En `config-{env}.properties` del módulo:

```properties
# ============================================================
# BASES DE DATOS - CONFIGURACIÓN MULTI-BD
# ============================================================

# Oracle (autenticación SQL estándar)
oracle.db.url=jdbc:oracle:thin:@//servidor:1521/SERVICENAME
oracle.db.username=${ORACLE_USER}
oracle.db.password=${ORACLE_PASSWORD}
oracle.db.pool.size.max=10

# SQL Server con Windows Authentication
sqlserver.db.url=jdbc:sqlserver://servidor:1433;databaseName=MiDB;integratedSecurity=true;encrypt=false;trustServerCertificate=true
sqlserver.db.username=
sqlserver.db.password=
sqlserver.db.pool.size.max=10

# SQL Server con SQL Authentication (alternativa)
#sqlserver.db.url=jdbc:sqlserver://servidor:1433;databaseName=MiDB;encrypt=false;trustServerCertificate=true
#sqlserver.db.username=${SQLSERVER_USER}
#sqlserver.db.password=${SQLSERVER_PASSWORD}

# PostgreSQL
postgresql.db.url=jdbc:postgresql://servidor:5432/midb
postgresql.db.username=${PG_USER}
postgresql.db.password=${PG_PASSWORD}
postgresql.db.pool.size.max=10

# MySQL
mysql.db.url=jdbc:mysql://servidor:3306/midb?useSSL=false&allowPublicKeyRetrieval=true
mysql.db.username=${MYSQL_USER}
mysql.db.password=${MYSQL_PASSWORD}
mysql.db.pool.size.max=10
```

### En `.env.local` del módulo:

```bash
# Oracle
ORACLE_USER=qa_user
ORACLE_PASSWORD=SecurePass123

# SQL Server (Windows Auth NO necesita credenciales)
# Ya configurado en URL con integratedSecurity=true

# PostgreSQL
PG_USER=postgres
PG_PASSWORD=postgres_pass

# MySQL
MYSQL_USER=root
MYSQL_PASSWORD=mysql_pass
```

---

## 🥒 STEPS DE CUCUMBER

### **GIVEN - Establecer Conexión**

```gherkin
Given establezco conexion a base de datos "oracle"
Given establezco conexion a base de datos "sqlserver"
Given establezco conexion a base de datos "postgresql"
Given establezco conexion a base de datos "mysql"
```

**Implementación (3 líneas):**
```java
@Given("establezco conexion a base de datos {string}")
public void establecerConexionABaseDeDatos(String dbType) throws FrameworkBusinessException {
    DatabaseConnector connector = DbConnectorFactory.connectAndCache(dbType);
    ScenarioContext.set("currentDbConnector", connector);
    ScenarioContext.set("currentDbType", dbType);
}
```

---

### **WHEN - Ejecutar Consultas (SELECT)**

```gherkin
When ejecuto la consulta "SELECT * FROM users"
When ejecuto la consulta "SELECT * FROM users WHERE user_id = ?" con parametros "12345"
When ejecuto la consulta "SELECT * FROM accounts WHERE user_id = ? AND status = ?" con parametros "12345","ACTIVE"
```

**Implementación (6 líneas):**
```java
@When("ejecuto la consulta {string} con parametros {string}")
public void ejecutarConsultaConParametros(String query, String parameters) throws FrameworkBusinessException {
    DatabaseConnector connector = (DatabaseConnector) ScenarioContext.get("currentDbConnector");
    Map<String, Object> result = DatabaseHelper.executeQuery(connector, query, parameters);
    
    ScenarioContext.set("queryResult", result);
    ScenarioContext.set("queryRowCount", DatabaseHelper.getRowCount(result));
}
```

---

### **WHEN - Ejecutar Sentencias (INSERT/UPDATE/DELETE)**

```gherkin
When ejecuto la sentencia "DELETE FROM temp_data WHERE date < '2025-01-01'"
When ejecuto la sentencia "UPDATE users SET status = ?" con parametros "ACTIVE"
When ejecuto la sentencia "INSERT INTO audit (user_id, action) VALUES (?, ?)" con parametros "12345","LOGIN"
```

**Implementación (5 líneas):**
```java
@When("ejecuto la sentencia {string} con parametros {string}")
public void ejecutarSentenciaConParametros(String sql, String parameters) throws FrameworkBusinessException {
    DatabaseConnector connector = (DatabaseConnector) ScenarioContext.get("currentDbConnector");
    int rowsAffected = DatabaseHelper.executeStatement(connector, sql, parameters);
    ScenarioContext.set("rowsAffected", rowsAffected);
}
```

---

### **THEN - Validaciones y Extracción**

```gherkin
# Obtener valor de columna
Then obtengo el valor de la columna "balance" y lo almaceno en "saldo"
Then obtengo el valor de la columna "user_id" y lo almaceno en "usuarioId"

# Validar existencia de resultados
Then valido que la consulta retorne resultados
Then valido que la consulta no retorne resultados

# Validar valor específico
Then valido que la columna "status" tenga el valor "ACTIVE"
```

**Implementaciones (5 líneas cada uno):**
```java
@Then("obtengo el valor de la columna {string} y lo almaceno en {string}")
public void obtenerValorColumna(String columnName, String variableName) throws FrameworkBusinessException {
    Map<String, Object> queryResult = (Map<String, Object>) ScenarioContext.get("queryResult");
    Object value = DatabaseHelper.getColumnValue(queryResult, columnName);
    ScenarioContext.set(variableName, value);
}

@Then("valido que la consulta retorne resultados")
public void validarQueRetorneResultados() throws FrameworkBusinessException {
    Map<String, Object> queryResult = (Map<String, Object>) ScenarioContext.get("queryResult");
    DatabaseHelper.validateHasResults(queryResult);
}

@Then("valido que la consulta no retorne resultados")
public void validarQueNoRetorneResultados() throws FrameworkBusinessException {
    Map<String, Object> queryResult = (Map<String, Object>) ScenarioContext.get("queryResult");
    DatabaseHelper.validateNoResults(queryResult);
}

@Then("valido que la columna {string} tenga el valor {string}")
public void validarValorColumna(String columnName, String expectedValue) throws FrameworkBusinessException {
    Map<String, Object> queryResult = (Map<String, Object>) ScenarioContext.get("queryResult");
    DatabaseHelper.validateColumnValue(queryResult, columnName, expectedValue);
}
```

---

## 💡 EJEMPLOS DE USO

### **Ejemplo 1: Consulta simple en Oracle**

```gherkin
Feature: Consultas Oracle

  Scenario: Verificar saldo de cuenta
    Given establezco conexion a base de datos "oracle"
    When ejecuto la consulta "SELECT balance FROM accounts WHERE account_id = ?" con parametros "ACC-001"
    Then valido que la consulta retorne resultados
    And obtengo el valor de la columna "balance" y lo almaceno en "saldoActual"
    And valido que la columna "balance" tenga el valor "5000.00"
```

---

### **Ejemplo 2: Multi-BD (Oracle + SQL Server)**

```gherkin
Feature: Sincronización de datos

  Scenario: Comparar balances entre Oracle y SQL Server
    # Consultar Oracle
    Given establezco conexion a base de datos "oracle"
    When ejecuto la consulta "SELECT balance FROM accounts WHERE user_id = ?" con parametros "12345"
    Then obtengo el valor de la columna "balance" y lo almaceno en "balanceOracle"
    
    # Consultar SQL Server
    Given establezco conexion a base de datos "sqlserver"
    When ejecuto la consulta "SELECT balance FROM accounts WHERE user_id = ?" con parametros "12345"
    Then obtengo el valor de la columna "balance" y lo almaceno en "balanceSqlServer"
    
    # Comparar
    Then valido que "{{balanceOracle}}" sea igual a "{{balanceSqlServer}}"
```

---

### **Ejemplo 3: INSERT/UPDATE/DELETE**

```gherkin
Feature: Modificar datos

  Scenario: Actualizar estado de usuario
    Given establezco conexion a base de datos "postgresql"
    When ejecuto la sentencia "UPDATE users SET status = ? WHERE user_id = ?" con parametros "ACTIVE","12345"
    # rowsAffected se guarda automáticamente en ScenarioContext
```

---

### **Ejemplo 4: Validar usuario inexistente**

```gherkin
Feature: Validaciones negativas

  Scenario: Usuario no debe existir
    Given establezco conexion a base de datos "mysql"
    When ejecuto la consulta "SELECT * FROM users WHERE user_id = ?" con parametros "USUARIO_INEXISTENTE"
    Then valido que la consulta no retorne resultados
```

---

### **Ejemplo 5: Uso programático (sin Steps)**

```java
// Desde un step custom
@When("obtengo el saldo del usuario {string}")
public void obtenerSaldoUsuario(String userId) throws SQLException {
    // Obtener conector del cache
    DatabaseConnector oracle = DbConnectorFactory.getCachedConnector("oracle");
    
    // Opción A: Usar QueryRepository
    QueryRepository repo = new QueryRepository(oracle);
    Map<String, Object> result = repo.queryForMap(
        "SELECT balance FROM accounts WHERE user_id = ?",
        userId
    );
    
    // Opción B: Usar DatabaseHelper
    Map<String, Object> result = DatabaseHelper.executeQuery(
        oracle, 
        "SELECT balance FROM accounts WHERE user_id = ?", 
        userId
    );
    
    ScenarioContext.set("balance", result.get("balance"));
}
```

---

## 🔧 TROUBLESHOOTING

### ❌ Error: "No hay conexión activa"

**Causa:** No se ejecutó el step `Given establezco conexion a base de datos`

**Solución:**
```gherkin
Given establezco conexion a base de datos "oracle"  # ← Agregar este step ANTES
When ejecuto la consulta "SELECT ..."
```

---

### ❌ Error: "Propiedad 'oracle.db.url' no configurada"

**Causa:** Falta configuración en `config-{env}.properties`

**Solución:** Agregar en config-qa.properties:
```properties
oracle.db.url=jdbc:oracle:thin:@//servidor:1521/DB
oracle.db.username=${ORACLE_USER}
oracle.db.password=${ORACLE_PASSWORD}
```

---

### ❌ Error: "Login failed for user ''" (SQL Server)

**Causa:** Windows Authentication mal configurada

**Solución:** Agregar `integratedSecurity=true` a la URL:
```properties
sqlserver.db.url=jdbc:sqlserver://servidor:1433;databaseName=DB;integratedSecurity=true;encrypt=false
sqlserver.db.username=
sqlserver.db.password=
```

---

### ❌ Error: "Columna no encontrada"

**Causa:** Nombre de columna incorrecto o query no retornó esa columna

**Solución:** Verificar que la columna exista en el SELECT:
```gherkin
# ✅ Correcto
When ejecuto la consulta "SELECT balance, status FROM accounts WHERE id = ?" con parametros "ACC-001"
Then obtengo el valor de la columna "balance" y lo almaceno en "saldo"

# ❌ Incorrecto (columna no incluida en SELECT)
When ejecuto la consulta "SELECT status FROM accounts WHERE id = ?" con parametros "ACC-001"
Then obtengo el valor de la columna "balance" y lo almaceno en "saldo"  # ← ERROR
```

---

## 📚 MIGRACIÓN DESDE CÓDIGO LEGACY

### Si actualmente tienes:

```java
// ❌ LEGACY
@Before
public void configurarBD() {
    System.setProperty("oracle.db.url", "jdbc:...");
    System.setProperty("oracle.db.username", "user");
    System.setProperty("oracle.db.password", "pass");
    connector = DbConnectorFactory.getConnector("oracle");
}
```

### Migra a:

```gherkin
# ✅ NUEVO
Given establezco conexion a base de datos "oracle"
```

Y configura en `config-qa.properties`:
```properties
oracle.db.url=jdbc:oracle:thin:@//servidor:1521/DB
oracle.db.username=${ORACLE_USER}
oracle.db.password=${ORACLE_PASSWORD}
```

---

## 🎯 BUENAS PRÁCTICAS

### ✅ DO (Hacer):

1. ✅ Usar `connectAndCache()` para Steps de Cucumber
2. ✅ Configurar BDs en `config-{env}.properties`
3. ✅ Usar variables de entorno para credenciales
4. ✅ Dejar que `@After` cierre conexiones automáticamente
5. ✅ Usar PreparedStatement (parámetros con `?`)
6. ✅ Validar resultados con los steps THEN

### ❌ DON'T (No hacer):

1. ❌ No usar `System.setProperty()` directamente
2. ❌ No hardcodear credenciales en código
3. ❌ No cerrar conexiones manualmente (usa `@After` hook)
4. ❌ No concatenar strings en SQL (usar `?` parameters)
5. ❌ No mezclar steps de negocio con steps genéricos
6. ❌ No agregar lógica en los steps (usar helpers)

---

## 🔐 SEGURIDAD

### Windows Authentication (SQL Server)

**Configuración correcta:**
```properties
# URL debe contener integratedSecurity=true
sqlserver.db.url=jdbc:sqlserver://servidor:1433;databaseName=DB;integratedSecurity=true;encrypt=false;trustServerCertificate=true

# Username y password VACÍOS (no se usan)
sqlserver.db.username=
sqlserver.db.password=
```

**Detección automática:**
- ✅ Framework detecta `integratedSecurity=true` automáticamente
- ✅ No valida username/password si está presente
- ✅ Logging informa que se usa Windows Auth

---

### Prevención SQL Injection

**Todos los steps usan PreparedStatement:**

```gherkin
# ✅ SEGURO (usa PreparedStatement)
When ejecuto la consulta "SELECT * FROM users WHERE user_id = ?" con parametros "12345"

# ❌ INSEGURO (concatenación de strings - NO SOPORTADO)
When ejecuto la consulta "SELECT * FROM users WHERE user_id = '12345'"
```

---

## 🧪 TESTING

### Crear tests unitarios para módulos custom:

```java
@Test
public void testObtenerSaldoOracle() {
    // Setup
    DbConnectorFactory.connectAndCache("oracle");
    
    // Ejecutar
    steps.ejecutarConsulta("SELECT balance FROM accounts WHERE id = '001'");
    
    // Validar
    steps.validarQueRetorneResultados();
    steps.obtenerValorColumna("balance", "saldo");
    
    Object saldo = ScenarioContext.get("saldo");
    assertThat(saldo).isNotNull();
}
```

---

## 📦 ARTEFACTOS GENERADOS

**Archivos creados en la refactorización:**

```
common/src/main/java/com/scotia/qa/common/database/
├── helpers/
│   └── DatabaseHelper.java              # 🆕 Helper con toda la lógica SQL
├── steps/
│   └── DatabaseConnectionSteps.java     # 🆕 Steps Cucumber genéricos
└── repository/
    └── QueryRepository.java             # ✅ Recreado sin steps
```

**Archivos modificados:**

```
common/src/main/java/com/scotia/qa/common/database/
└── factory/
    └── DbConnectorFactory.java          # ✅ Mejorado con cache
```

**Archivos eliminados:**

```
common/src/main/java/com/scotia/qa/common/database/
└── repository/
    └── QueryRepository.java (v1.0)      # ❌ Eliminado (tenía steps mezclados)
```

---

## 🚀 ESTADO DEL PROYECTO

```bash
✅ Compilación: BUILD SUCCESSFUL
✅ Tests: Pasando
✅ Coverage: 19% (trabajando en mejora)
✅ Steps: Súper limpios (3-7 líneas)
✅ Lógica: Encapsulada en helpers
✅ Multi-BD: Funcionando
✅ Windows Auth: Soportado
✅ Cache: Implementado
```

---

## 📞 SOPORTE

### Reportar issues:

1. Verificar configuración en `config-{env}.properties`
2. Verificar variables de entorno en `.env.local`
3. Revisar logs en `logs/{module}/test-execution.log`
4. Consultar esta documentación

---

## 🎓 PRÓXIMOS PASOS

### Para módulos consumidores:

1. ✅ Configurar multi-BD en `config-{env}.properties`
2. ✅ Crear features usando los nuevos steps
3. ✅ Migrar código legacy (opcional)

### Para el framework:

1. ⏳ Crear tests unitarios de DatabaseHelper
2. ⏳ Crear tests unitarios de DatabaseConnectionSteps
3. ⏳ Aumentar coverage a 70%

---

**Documentación completa y consolidada de la gestión de conexiones BD** ✅

