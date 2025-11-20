# 📊 Módulo JiraXray - Framework Scotia QA

## 🎯 Descripción

El módulo **JiraXray** permite la integración automática con Jira para actualizar test cases y enviar resultados de ejecución. Está diseñado para ser utilizado por los frameworks Web, API y Mobile del ecosistema Scotia QA.

## ✨ Características

- ✅ **Envío automático de resultados**: Sincronización con Jira Xray
- ✅ **Múltiples formatos**: Soporte para Cucumber JSON y JUnit XML
- ✅ **Procesamiento en batch**: Envío optimizado de múltiples resultados
- ✅ **Logging integrado**: Sistema unificado con TestLogger
- ✅ **Configuración flexible**: Variables de entorno y archivos YAML
- ✅ **Adaptadores extensibles**: Fácil adición de nuevos formatos

## 🚀 Uso Básico

### Inicialización

```java
import com.scotia.qa.common.jiraXray.JiraTestCaseManager;

// Inicializar el manager (usar al inicio de la suite de tests)
JiraTestCaseManager.initialize();
```

### Envío de Resultados

```java
// Enviar resultados de Cucumber JSON
String cucumberResults = loadCucumberResults(); // Tu JSON de resultados
JiraTestCaseManager.sendTestResults(cucumberResults);

// Enviar resultados de JUnit XML (cuando esté implementado)
String junitResults = loadJunitResults();
JiraTestCaseManager.sendTestResults(junitResults);
```

### Cierre

```java
// Al final de la ejecución (opcional)
JiraTestCaseManager.shutdown();
```

## ⚙️ Configuración

### Variables de Entorno

```bash
# Credenciales de Jira
export JIRA_USER="tu-usuario"
export JIRA_PASSWORD="tu-token"

# Configuración de envío
export JIRA_UPLOAD_REPORT="true"
export JIRA_TEST_EXECUTION="QAAUY-123"
```

### Archivo YAML (opcional)

Crear `src/main/resources/reporting-config.yml`:

```yaml
user: "${JIRA_USER:usuario-default}"
password: "${JIRA_PASSWORD:token-default}"
params:
  uploadReport: "${JIRA_UPLOAD_REPORT:true}"
  testExecution: "${JIRA_TEST_EXECUTION:}"
```

## 🏗️ Arquitectura

### Componentes Principales

1. **JiraTestCaseManager**: Clase principal de entrada
2. **JiraClient**: Cliente HTTP para comunicación con Jira
3. **Adaptadores**: Conversores de formato (Cucumber, JUnit)
4. **Modelos**: Representación interna de resultados
5. **Extractores**: Parsers especializados (tags, features)

### Flujo de Datos

```
Resultados Raw → Adaptador → Modelo Interno → JiraClient → Jira Xray
```

## 🧩 Integración con Frameworks

### Framework Web (Selenium)

```java
@AfterSuite
public void sendResultsToJira() {
    if (shouldSendToJira()) {
        String results = getCucumberResults();
        JiraTestCaseManager.sendTestResults(results);
    }
}
```

### Framework API (RestAssured)

```java
@Test
public void setupJiraIntegration() {
    JiraTestCaseManager.initialize();
    // Configurar variables específicas para API
}
```

### Framework Mobile (Appium)

```java
@AfterClass
public void reportToJira() {
    String testResults = collectTestResults();
    JiraTestCaseManager.sendTestResults(testResults);
}
```

## 🏷️ Formato de Tags

Los test cases deben tener tags que identifiquen el test key de Jira:

```gherkin
@QAAUY-582 @smoke @regression
Scenario: Login exitoso
  Given que estoy en la página de login
  When ingreso credenciales válidas
  Then debería ver el dashboard
```

## 🔧 Configuración Avanzada

### Usar Servicio Directamente

```java
import com.scotia.qa.common.jiraXray.service.JiraTestCaseUpdaterService;
import com.scotia.qa.common.jiraXray.config.ReportConfig;

// Configuración manual
ReportConfig config = new ReportConfig();
config.setUser("usuario");
config.setPassword("token");
config.setUploadReport(true);
config.setTestExecution("QAAUY-123");

// Usar servicio directamente
JiraTestCaseUpdaterService service = new JiraTestCaseUpdaterService(config);
service.processAndSendResults(rawResults);
```

### Crear Adaptador Personalizado

```java
import com.scotia.qa.common.jiraXray.adapter.ResultAdapter;

public class MyCustomAdapter implements ResultAdapter {
    @Override
    public TestExecutionResult convert(String rawResults) {
        // Tu lógica de conversión
        return result;
    }
    
    @Override
    public boolean canHandle(String rawResults) {
        // Tu lógica de detección
        return canProcess;
    }
    
    @Override
    public String getName() {
        return "MyCustomAdapter";
    }
}
```

## 📝 Logging

El módulo utiliza el sistema de logging centralizado del framework:

```java
// Los logs se generan automáticamente
// Categorías: JIRA_MANAGER, JIRA_CLIENT, JIRA_SERVICE, CUCUMBER_ADAPTER, etc.
```

## ⚠️ Notas Importantes

1. **Variables de entorno**: Asegurar que estén configuradas correctamente
2. **Test keys**: Cada scenario debe tener un tag con formato `@PROJECT-123`
3. **Conectividad**: Verificar acceso a la URL de Jira desde el entorno de ejecución
4. **Permisos**: El usuario debe tener permisos para actualizar test executions
5. **Formato JSON**: Asegurar que el JSON de Cucumber esté bien formado

## 🚀 Próximas Funcionalidades

- [ ] Soporte completo para JUnit XML
- [ ] Integración con TestNG
- [ ] Soporte para múltiples proyectos Jira
- [ ] Dashboard de resultados
- [ ] Retry automático en caso de fallos de red

## 📞 Soporte

Para dudas o problemas:
1. Verificar configuración y logs
2. Validar formato de resultados
3. Consultar documentación de Jira Xray
4. Contactar al equipo de QA Framework

---
**Scotia QA Framework Team** | Version 1.0.0
