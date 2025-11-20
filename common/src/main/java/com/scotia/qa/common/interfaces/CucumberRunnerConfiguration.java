package com.scotia.qa.common.interfaces;

import java.util.Map;
import java.util.List;

/**
 * Interface que define el contrato para configuración de runners de Cucumber del framework Scotia QA.
 *
 * <p>Esta interface proporciona una abstracción para configurar runners de Cucumber
 * de manera estandarizada y extensible. Permite definir configuraciones específicas
 * por framework (API, Web, Mobile) manteniendo consistencia en la ejecución.
 *
 * <p><b>Características principales:</b>
 * <ul>
 *   <li>Configuración unificada de runners de Cucumber</li>
 *   <li>Soporte para múltiples formatos de reporte</li>
 *   <li>Gestión de tags y filtros de ejecución</li>
 *   <li>Configuración de paralelismo</li>
 *   <li>Integración con sistemas de reporte externos</li>
 *   <li>Configuración específica por entorno</li>
 * </ul>
 *
 * <p><b>Configuraciones soportadas:</b>
 * <ul>
 *   <li><b>Features Path</b> - Ubicación de archivos .feature</li>
 *   <li><b>Glue Package</b> - Paquetes con step definitions</li>
 *   <li><b>Tags</b> - Filtros de ejecución por tags</li>
 *   <li><b>Plugins</b> - Reportes y plugins adicionales</li>
 *   <li><b>Parallel Execution</b> - Configuración de ejecución paralela</li>
 *   <li><b>Dry Run</b> - Validación sin ejecución</li>
 * </ul>
 *
 * <p><b>Uso típico en frameworks específicos:</b>
 * <pre>
 * // En api-core
 * &#64;RunWith(Cucumber.class)
 * &#64;CucumberOptions(
 *     features = "src/test/resources/features",
 *     glue = "com.scotia.qa.apicore.steps"
 * )
 * public class ApiTestRunner {
 *     private static CucumberRunnerConfiguration config =
 *         CucumberConfigurationFactory.getApiConfiguration();
 *
 *     &#64;BeforeClass
 *     public static void setUp() {
 *         config.configureRunner("API");
 *     }
 * }
 * </pre>
 *
 * <p><b>Configuración programática:</b>
 * <pre>
 * CucumberRunnerConfiguration config = new BaseCucumberRunnerConfiguration();
 * config.setFeaturesPath("src/test/resources/features");
 * config.setGluePackages(Arrays.asList("com.scotia.qa.steps"));
 * config.setTags("@smoke and not @ignore");
 * config.addPlugin("pretty", "html:target/cucumber-reports");
 * config.setParallelExecution(true, 4);
 * </pre>
 *
 * @author Scotia QA Framework Team
 * @version 1.0.0
 * @since 2.0.0
 */
public interface CucumberRunnerConfiguration {

    // =================================================================================
    // CONFIGURACIÓN BÁSICA DEL RUNNER
    // =================================================================================

    /**
     * Configura la ruta donde se encuentran los archivos .feature.
     *
     * @param featuresPath ruta de los features (relativa o absoluta)
     */
    void setFeaturesPath(String featuresPath);

    /**
     * Obtiene la ruta configurada de los features.
     *
     * @return ruta de los features
     */
    String getFeaturesPath();

    /**
     * Configura los paquetes donde se encuentran las step definitions.
     *
     * @param gluePackages lista de paquetes con step definitions
     */
    void setGluePackages(List<String> gluePackages);

    /**
     * Agrega un paquete de step definitions.
     *
     * @param gluePackage paquete a agregar
     */
    void addGluePackage(String gluePackage);

    /**
     * Obtiene los paquetes de step definitions configurados.
     *
     * @return lista de paquetes glue
     */
    List<String> getGluePackages();

    /**
     * Configura el directorio de salida para reportes.
     *
     * @param outputPath directorio donde generar reportes
     */
    void setOutputPath(String outputPath);

    /**
     * Obtiene el directorio de salida configurado.
     *
     * @return directorio de reportes
     */
    String getOutputPath();

    // =================================================================================
    // CONFIGURACIÓN DE FILTROS Y TAGS
    // =================================================================================

    /**
     * Configura la expresión de tags para filtrar scenarios.
     * Ejemplos: "@smoke", "@api and not @ignore", "@regression or @critical"
     *
     * @param tagExpression expresión de tags de Cucumber
     */
    void setTagExpression(String tagExpression);

    /**
     * Obtiene la expresión de tags configurada.
     *
     * @return expresión de tags actual
     */
    String getTagExpression();

    /**
     * Configura nombres específicos de scenarios a ejecutar.
     *
     * @param scenarioNames lista de nombres de scenarios
     */
    void setScenarioNames(List<String> scenarioNames);

    /**
     * Agrega un scenario específico para ejecutar.
     *
     * @param scenarioName nombre del scenario
     */
    void addScenarioName(String scenarioName);

    /**
     * Obtiene los nombres de scenarios configurados.
     *
     * @return lista de scenarios específicos
     */
    List<String> getScenarioNames();

    // =================================================================================
    // CONFIGURACIÓN DE PLUGINS Y REPORTES
    // =================================================================================

    /**
     * Agrega un plugin de reporte de Cucumber.
     *
     * @param pluginType tipo de plugin ("pretty", "html", "json", "junit")
     * @param outputLocation ubicación de salida (opcional para algunos plugins)
     */
    void addPlugin(String pluginType, String outputLocation);

    /**
     * Agrega un plugin simple sin ubicación específica.
     *
     * @param pluginType tipo de plugin
     */
    void addPlugin(String pluginType);

    /**
     * Obtiene todos los plugins configurados.
     *
     * @return lista de plugins configurados
     */
    List<String> getPlugins();

    /**
     * Limpia todos los plugins configurados.
     */
    void clearPlugins();

    /**
     * Configura plugins predeterminados según el framework.
     *
     * @param frameworkType tipo de framework ("API", "WEB", "MOBILE")
     */
    void setDefaultPluginsForFramework(String frameworkType);

    // =================================================================================
    // CONFIGURACIÓN DE EJECUCIÓN
    // =================================================================================

    /**
     * Habilita o deshabilita el modo dry-run.
     * En dry-run se validan los steps sin ejecutar la lógica.
     *
     * @param dryRun true para habilitar dry-run
     */
    void setDryRun(boolean dryRun);

    /**
     * Verifica si está habilitado el modo dry-run.
     *
     * @return true si está en modo dry-run
     */
    boolean isDryRun();

    /**
     * Habilita o deshabilita el modo strict.
     * En modo strict, steps indefinidos causan fallo.
     *
     * @param strict true para modo strict
     */
    void setStrict(boolean strict);

    /**
     * Verifica si está habilitado el modo strict.
     *
     * @return true si está en modo strict
     */
    boolean isStrict();

    /**
     * Configura la ejecución en paralelo.
     *
     * @param enabled true para habilitar paralelismo
     * @param threadCount número de threads a utilizar
     */
    void setParallelExecution(boolean enabled, int threadCount);

    /**
     * Verifica si la ejecución en paralelo está habilitada.
     *
     * @return true si está habilitada
     */
    boolean isParallelExecutionEnabled();

    /**
     * Obtiene el número de threads configurado para ejecución paralela.
     *
     * @return número de threads
     */
    int getThreadCount();

    // =================================================================================
    // CONFIGURACIÓN DE ENTORNOS Y PROPIEDADES
    // =================================================================================

    /**
     * Configura el entorno de ejecución.
     *
     * @param environment entorno ("dev", "test", "staging", "prod")
     */
    void setEnvironment(String environment);

    /**
     * Obtiene el entorno configurado.
     *
     * @return entorno actual
     */
    String getEnvironment();

    /**
     * Configura propiedades personalizadas del runner.
     *
     * @param properties mapa de propiedades
     */
    void setCustomProperties(Map<String, String> properties);

    /**
     * Agrega una propiedad personalizada.
     *
     * @param key clave de la propiedad
     * @param value valor de la propiedad
     */
    void setCustomProperty(String key, String value);

    /**
     * Obtiene una propiedad personalizada.
     *
     * @param key clave de la propiedad
     * @return valor de la propiedad o null si no existe
     */
    String getCustomProperty(String key);

    /**
     * Obtiene todas las propiedades personalizadas.
     *
     * @return mapa con todas las propiedades
     */
    Map<String, String> getCustomProperties();

    // =================================================================================
    // CONFIGURACIÓN DE INTEGRACIÓN EXTERNA
    // =================================================================================

    /**
     * Configura la integración con Jira/Xray.
     *
     * @param enabled true para habilitar integración
     * @param jiraUrl URL del servidor Jira
     * @param testPlanKey clave del test plan en Jira
     */
    void setJiraIntegration(boolean enabled, String jiraUrl, String testPlanKey);

    /**
     * Verifica si la integración con Jira está habilitada.
     *
     * @return true si está habilitada
     */
    boolean isJiraIntegrationEnabled();

    /**
     * Obtiene la URL configurada de Jira.
     *
     * @return URL de Jira
     */
    String getJiraUrl();

    /**
     * Obtiene la clave del test plan de Jira.
     *
     * @return clave del test plan
     */
    String getJiraTestPlanKey();

    // =================================================================================
    // MÉTODOS DE CONFIGURACIÓN Y UTILIDAD
    // =================================================================================

    /**
     * Configura el runner según el framework específico.
     *
     * @param frameworkType tipo de framework ("API", "WEB", "MOBILE")
     */
    void configureForFramework(String frameworkType);

    /**
     * Carga configuración desde un archivo properties.
     *
     * @param configFilePath ruta del archivo de configuración
     */
    void loadConfiguration(String configFilePath);

    /**
     * Guarda la configuración actual a un archivo properties.
     *
     * @param configFilePath ruta donde guardar la configuración
     */
    void saveConfiguration(String configFilePath);

    /**
     * Valida que la configuración actual sea válida.
     *
     * @return true si la configuración es válida
     * @throws IllegalStateException si la configuración tiene errores
     */
    boolean validateConfiguration();

    /**
     * Obtiene la configuración como array de opciones de Cucumber.
     * Útil para usar con @CucumberOptions programáticamente.
     *
     * @return mapa con las opciones configuradas
     */
    Map<String, Object> toCucumberOptions();

    /**
     * Obtiene información de debug sobre la configuración actual.
     *
     * @return string con toda la configuración para troubleshooting
     */
    String getDebugInfo();

    /**
     * Reinicia la configuración a valores por defecto.
     */
    void reset();

    /**
     * Clona la configuración actual.
     *
     * @return nueva instancia con la misma configuración
     */
    CucumberRunnerConfiguration clone();
}
