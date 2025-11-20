package com.scotia.qa.common.interfaces;

import java.util.Map;
import java.util.List;

/**
 * Interface que define el contrato para servicios de hooks de Cucumber del framework Scotia QA.
 *
 * <p>Esta interface proporciona una abstracción de alto nivel para la gestión de hooks
 * de Cucumber que pueden ser utilizados por cualquier framework consumidor.
 * Permite especialización por framework (API, Web, Mobile) manteniendo consistencia.
 *
 * <p><b>Características principales:</b>
 * <ul>
 *   <li>Gestión de lifecycle de scenarios y features</li>
 *   <li>Contexto compartido thread-safe entre steps</li>
 *   <li>Captura automática de evidencias</li>
 *   <li>Logging integrado con TestLogger</li>
 *   <li>Soporte para ejecución paralela</li>
 *   <li>Configuración específica por framework</li>
 * </ul>
 *
 * <p><b>Hooks soportados:</b>
 * <ul>
 *   <li><b>@Before</b> - Inicialización antes de cada escenario</li>
 *   <li><b>@After</b> - Limpieza después de cada escenario</li>
 *   <li><b>@BeforeStep</b> - Acciones antes de cada paso</li>
 *   <li><b>@AfterStep</b> - Acciones después de cada paso</li>
 *   <li><b>@BeforeAll</b> - Configuración global del framework</li>
 *   <li><b>@AfterAll</b> - Limpieza global del framework</li>
 * </ul>
 *
 * <p><b>Uso típico en frameworks específicos:</b>
 * <pre>
 * // En api-core
 * public class ApiCucumberHooks {
 *     private CucumberHooksService hooksService = CucumberServiceFactory.getInstance("API");
 *
 *     &#64;Before
 *     public void beforeScenario(Scenario scenario) {
 *         hooksService.executeBeforeScenario(scenario.getName(), getFeatureName(scenario));
 *     }
 *
 *     &#64;After
 *     public void afterScenario(Scenario scenario) {
 *         hooksService.executeAfterScenario(scenario.isFailed(), scenario.getFailureReason());
 *     }
 * }
 * </pre>
 *
 * <p><b>Gestión de contexto entre steps:</b>
 * <pre>
 * // En step definitions
 * public class ApiSteps {
 *     private CucumberHooksService hooksService = CucumberServiceFactory.getInstance("API");
 *
 *     &#64;Given("I have a user with id {string}")
 *     public void i_have_user_with_id(String userId) {
 *         hooksService.storeTestData("userId", userId);
 *         // ... resto de la lógica
 *     }
 *
 *     &#64;When("I call the user API")
 *     public void i_call_user_api() {
 *         String userId = hooksService.getTestData("userId");
 *         // ... usar el userId
 *     }
 * }
 * </pre>
 *
 * @author Scotia QA Framework Team
 * @version 1.0.0
 * @since 2.0.0
 */
public interface CucumberHooksService {

    // =================================================================================
    // CONFIGURACIÓN DEL SERVICIO
    // =================================================================================

    /**
     * Configura el tipo de framework que utilizará este servicio.
     *
     * @param frameworkType tipo de framework ("API", "WEB", "MOBILE")
     */
    void setFrameworkType(String frameworkType);

    /**
     * Obtiene el tipo de framework configurado.
     *
     * @return tipo de framework actual
     */
    String getFrameworkType();

    /**
     * Configura propiedades específicas del framework.
     *
     * @param properties mapa de propiedades de configuración
     */
    void configureFrameworkProperties(Map<String, Object> properties);

    /**
     * Obtiene las propiedades configuradas del framework.
     *
     * @return mapa con las propiedades actuales
     */
    Map<String, Object> getFrameworkProperties();

    // =================================================================================
    // EJECUCIÓN DE HOOKS - SCENARIO LIFECYCLE
    // =================================================================================

    /**
     * Ejecuta las acciones before scenario.
     * Debe ser llamado desde el hook @Before de Cucumber.
     *
     * @param scenarioName nombre del escenario actual
     * @param featureName nombre del feature actual
     */
    void executeBeforeScenario(String scenarioName, String featureName);

    /**
     * Ejecuta las acciones after scenario.
     * Debe ser llamado desde el hook @After de Cucumber.
     *
     * @param scenarioFailed indica si el escenario falló
     * @param failureReason razón del fallo (si aplica)
     */
    void executeAfterScenario(boolean scenarioFailed, String failureReason);

    /**
     * Ejecuta las acciones before step.
     * Debe ser llamado desde el hook @BeforeStep de Cucumber.
     *
     * @param stepText texto del paso que se va a ejecutar
     */
    void executeBeforeStep(String stepText);

    /**
     * Ejecuta las acciones after step.
     * Debe ser llamado desde el hook @AfterStep de Cucumber.
     *
     * @param stepText texto del paso ejecutado
     * @param stepFailed indica si el paso falló
     * @param failureReason razón del fallo (si aplica)
     */
    void executeAfterStep(String stepText, boolean stepFailed, String failureReason);

    // =================================================================================
    // EJECUCIÓN DE HOOKS - SUITE LIFECYCLE
    // =================================================================================

    /**
     * Ejecuta la configuración global antes de todos los tests.
     * Debe ser llamado desde el hook @BeforeAll de Cucumber.
     */
    void executeBeforeAll();

    /**
     * Ejecuta la limpieza global después de todos los tests.
     * Debe ser llamado desde el hook @AfterAll de Cucumber.
     */
    void executeAfterAll();

    // =================================================================================
    // GESTIÓN DE CONTEXTO Y DATOS COMPARTIDOS
    // =================================================================================

    /**
     * Almacena datos en el contexto del test actual.
     * Los datos son específicos del thread y scenario actuales.
     *
     * @param key clave para almacenar el dato
     * @param value valor a almacenar
     */
    void storeTestData(String key, Object value);

    /**
     * Recupera datos del contexto del test actual.
     *
     * @param key clave del dato a recuperar
     * @return valor almacenado o null si no existe
     */
    <T> T getTestData(String key);

    /**
     * Recupera datos del contexto con un valor por defecto.
     *
     * @param key clave del dato a recuperar
     * @param defaultValue valor por defecto si no existe
     * @return valor almacenado o defaultValue
     */
    <T> T getTestData(String key, T defaultValue);

    /**
     * Verifica si existe un dato en el contexto.
     *
     * @param key clave a verificar
     * @return true si existe, false en caso contrario
     */
    boolean hasTestData(String key);

    /**
     * Obtiene todos los datos del contexto actual.
     *
     * @return mapa con todos los datos del contexto
     */
    Map<String, Object> getAllTestData();

    /**
     * Limpia todos los datos del contexto actual.
     */
    void clearTestData();

    // =================================================================================
    // GESTIÓN DE INFORMACIÓN DEL SCENARIO ACTUAL
    // =================================================================================

    /**
     * Obtiene el nombre del escenario actual.
     *
     * @return nombre del escenario o null si no hay escenario activo
     */
    String getCurrentScenario();

    /**
     * Obtiene el nombre del feature actual.
     *
     * @return nombre del feature o null si no hay feature activo
     */
    String getCurrentFeature();

    /**
     * Obtiene la duración del escenario actual en milisegundos.
     *
     * @return duración en ms, -1 si el escenario no ha terminado
     */
    long getCurrentScenarioDuration();

    /**
     * Verifica si el escenario actual ha fallado.
     *
     * @return true si ha fallado, false en caso contrario
     */
    boolean isCurrentScenarioFailed();

    /**
     * Obtiene la razón del fallo del escenario actual.
     *
     * @return razón del fallo o null si no ha fallado
     */
    String getCurrentScenarioFailureReason();

    // =================================================================================
    // GESTIÓN DE EVIDENCIAS Y LOGGING
    // =================================================================================

    /**
     * Captura evidencia automática según el tipo de framework.
     * - API: Guarda request/response details
     * - Web: Captura screenshot
     * - Mobile: Captura screenshot y app state
     *
     * @param evidenceType tipo de evidencia a capturar
     * @param description descripción de la evidencia
     */
    void captureEvidence(String evidenceType, String description);

    /**
     * Captura evidencia con datos específicos.
     *
     * @param evidenceType tipo de evidencia
     * @param description descripción
     * @param evidenceData datos específicos a guardar
     */
    void captureEvidence(String evidenceType, String description, Object evidenceData);

    /**
     * Marca el escenario actual como fallido.
     *
     * @param reason razón del fallo
     * @param exception excepción que causó el fallo (opcional)
     */
    void markScenarioAsFailed(String reason, Throwable exception);

    // =================================================================================
    // CONFIGURACIÓN Y CALLBACKS ESPECÍFICOS DEL FRAMEWORK
    // =================================================================================

    /**
     * Registra un callback para inicialización específica del framework.
     * Se ejecuta durante executeBeforeScenario().
     *
     * @param callback función a ejecutar
     */
    void setFrameworkInitializationCallback(Runnable callback);

    /**
     * Registra un callback para limpieza específica del framework.
     * Se ejecuta durante executeAfterScenario().
     *
     * @param callback función a ejecutar
     */
    void setFrameworkCleanupCallback(Runnable callback);

    /**
     * Registra un callback para configuración global del framework.
     * Se ejecuta durante executeBeforeAll().
     *
     * @param callback función a ejecutar
     */
    void setGlobalSetupCallback(Runnable callback);

    /**
     * Registra un callback para limpieza global del framework.
     * Se ejecuta durante executeAfterAll().
     *
     * @param callback función a ejecutar
     */
    void setGlobalTeardownCallback(Runnable callback);

    // =================================================================================
    // UTILIDADES Y INFORMACIÓN DE DEBUG
    // =================================================================================

    /**
     * Habilita o deshabilita la captura automática de evidencias.
     *
     * @param enabled true para habilitar, false para deshabilitar
     */
    void setAutoEvidenceCaptureEnabled(boolean enabled);

    /**
     * Verifica si la captura automática de evidencias está habilitada.
     *
     * @return true si está habilitada, false en caso contrario
     */
    boolean isAutoEvidenceCaptureEnabled();

    /**
     * Obtiene estadísticas de ejecución del servicio.
     *
     * @return mapa con estadísticas (scenarios ejecutados, fallidos, etc.)
     */
    Map<String, Object> getExecutionStatistics();

    /**
     * Obtiene información de debug sobre el estado del servicio.
     *
     * @return string con información detallada para troubleshooting
     */
    String getDebugInfo();

    /**
     * Reinicia las estadísticas y estado del servicio.
     */
    void reset();
}
