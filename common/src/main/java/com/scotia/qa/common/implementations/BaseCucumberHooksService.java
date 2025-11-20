package com.scotia.qa.common.implementations;

import com.scotia.qa.common.interfaces.CucumberHooksService;
import com.scotia.qa.common.cucumber.CucumberTestContext;
import com.scotia.qa.common.logging.EvidenceManager;
import com.scotia.qa.common.logging.TestLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementación base del servicio de hooks de Cucumber para el framework Scotia QA.
 *
 * <p>Esta clase proporciona una implementación completa y robusta del servicio de hooks
 * de Cucumber que puede ser utilizada directamente o extendida por los frameworks específicos.
 * Integra las clases existentes del paquete cucumber con la nueva arquitectura de interfaces.
 *
 * <p><b>Características implementadas:</b>
 * <ul>
 *   <li>Gestión completa del lifecycle de scenarios y features</li>
 *   <li>Contexto compartido thread-safe usando CucumberTestContext</li>
 *   <li>Captura automática de evidencias con EvidenceManager</li>
 *   <li>Logging integrado con TestLogger</li>
 *   <li>Estadísticas de ejecución en tiempo real</li>
 *   <li>Callbacks configurables por framework</li>
 *   <li>Soporte para ejecución paralela</li>
 * </ul>
 *
 * <p><b>Uso típico:</b>
 * <pre>
 * // Configuración básica
 * CucumberHooksService hooksService = new BaseCucumberHooksService();
 * hooksService.setFrameworkType("API");
 * hooksService.setAutoEvidenceCaptureEnabled(true);
 *
 * // En hooks de Cucumber
 * &#64;Before
 * public void beforeScenario(Scenario scenario) {
 *     hooksService.executeBeforeScenario(scenario.getName(), getFeatureName(scenario));
 * }
 *
 * // En step definitions
 * &#64;When("I store user data")
 * public void i_store_user_data() {
 *     hooksService.storeTestData("userId", "12345");
 * }
 *
 * &#64;Then("I can retrieve user data")
 * public void i_can_retrieve_user_data() {
 *     String userId = hooksService.getTestData("userId");
 *     Assert.assertEquals("12345", userId);
 * }
 * </pre>
 *
 * <p><b>Configuración avanzada con callbacks:</b>
 * <pre>
 * BaseCucumberHooksService hooksService = new BaseCucumberHooksService();
 * hooksService.setFrameworkType("API");
 *
 * // Configurar callback específico del framework
 * hooksService.setFrameworkInitializationCallback(() -> {
 *     // Inicializar HttpClient, configurar base URL, etc.
 *     ApiTestHelper.initializeApiClient();
 * });
 *
 * hooksService.setFrameworkCleanupCallback(() -> {
 *     // Limpiar conexiones, cerrar drivers, etc.
 *     ApiTestHelper.cleanupResources();
 * });
 * </pre>
 *
 * @author Scotia QA Framework Team
 * @version 1.0.0
 * @since 2.0.0
 * @see CucumberHooksService
 * @see CucumberTestContext
 * @see EvidenceManager
 */
public class BaseCucumberHooksService implements CucumberHooksService {

    private static final TestLogger.LoggerWrapper log = TestLogger.getLogger(BaseCucumberHooksService.class);

    // Configuración del servicio
    private String frameworkType = "DEFAULT";
    private final Map<String, Object> frameworkProperties = new ConcurrentHashMap<>();
    private boolean autoEvidenceCaptureEnabled = true;

    // Callbacks configurables
    private Runnable frameworkInitializationCallback;
    private Runnable frameworkCleanupCallback;
    private Runnable globalSetupCallback;
    private Runnable globalTeardownCallback;

    // Estadísticas de ejecución
    private final AtomicInteger totalScenariosExecuted = new AtomicInteger(0);
    private final AtomicInteger totalScenariosFailed = new AtomicInteger(0);
    private final AtomicInteger totalStepsExecuted = new AtomicInteger(0);
    private final AtomicInteger totalStepsFailed = new AtomicInteger(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);

    // =================================================================================
    // IMPLEMENTACIÓN DE CONFIGURACIÓN DEL SERVICIO
    // =================================================================================

    @Override
    public void setFrameworkType(String frameworkType) {
        if (frameworkType == null || frameworkType.trim().isEmpty()) {
            throw new IllegalArgumentException("Framework type no puede ser null o vacío");
        }
        this.frameworkType = frameworkType.toUpperCase().trim();
        log.debug("Framework type establecido: {}", this.frameworkType);
    }

    @Override
    public String getFrameworkType() {
        return frameworkType;
    }

    @Override
    public void configureFrameworkProperties(Map<String, Object> properties) {
        if (properties != null) {
            this.frameworkProperties.putAll(properties);
            log.debug("Configuradas {} propiedades del framework", properties.size());
        }
    }

    @Override
    public Map<String, Object> getFrameworkProperties() {
        return new ConcurrentHashMap<>(frameworkProperties);
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE HOOKS - SCENARIO LIFECYCLE
    // =================================================================================

    @Override
    public void executeBeforeScenario(String scenarioName, String featureName) {
        try {
            log.info("=== INICIANDO SCENARIO: {} ===", scenarioName);

            // Inicializar contexto usando la clase existente
            CucumberTestContext.initializeScenario(scenarioName, featureName, frameworkType);

            // Log del inicio
            TestLogger.logStep("SCENARIO_START",
                String.format("Iniciando escenario '%s' en feature '%s'", scenarioName, featureName));

            // Ejecutar callback específico del framework
            if (frameworkInitializationCallback != null) {
                try {
                    frameworkInitializationCallback.run();
                    log.debug("Callback de inicialización del framework ejecutado exitosamente");
                } catch (Exception e) {
                    log.warn("Error en callback de inicialización: {}", e.getMessage());
                }
            }

            // Captura automática de evidencia si está habilitada
            if (autoEvidenceCaptureEnabled) {
                captureEvidence("SCENARIO_START",
                    "Inicio del escenario: " + scenarioName,
                    Map.of("scenario", scenarioName, "feature", featureName, "framework", frameworkType));
            }

        } catch (Exception e) {
            log.error("Error en beforeScenario hook: {}", e.getMessage());
            CucumberTestContext.markScenarioAsFailed("Error en inicialización", e);

            try {
                EvidenceManager.saveErrorEvidence("BEFORE_SCENARIO_ERROR", e.getMessage(), getStackTrace(e));
            } catch (Exception evidenceError) {
                log.warn("Failed to save error evidence: {}", evidenceError.getMessage());
            }
        }
    }

    @Override
    public void executeAfterScenario(boolean scenarioFailed, String failureReason) {
        String scenarioName = CucumberTestContext.getCurrentScenario();
        long duration = CucumberTestContext.getCurrentScenarioDuration();

        try {
            // Actualizar estadísticas
            totalScenariosExecuted.incrementAndGet();
            if (scenarioFailed) {
                totalScenariosFailed.incrementAndGet();
            }
            if (duration > 0) {
                totalExecutionTime.addAndGet(duration);
            }

            // Log de finalización
            String status = scenarioFailed ? "FALLIDO" : "EXITOSO";
            log.info("=== FINALIZANDO SCENARIO: {} - {} ({}ms) ===", scenarioName, status, duration);

            TestLogger.logStep("SCENARIO_END",
                String.format("Finalizando escenario '%s' - Status: %s - Duración: %dms",
                    scenarioName, status, duration));

            // Capturar evidencias automáticas
            if (autoEvidenceCaptureEnabled) {
                captureEvidence("SCENARIO_END",
                    "Final del escenario: " + scenarioName + " - " + status,
                    Map.of(
                        "scenario", scenarioName,
                        "status", status,
                        "duration", duration,
                        "failed", scenarioFailed,
                        "failureReason", failureReason != null ? failureReason : "N/A"
                    ));
            }

            // Capturar evidencia adicional en caso de fallo
            if (scenarioFailed) {
                try {
                    EvidenceManager.saveErrorEvidence("SCENARIO_FAILURE",
                        scenarioName + " - " + failureReason,
                        String.format("Scenario: %s\nReason: %s\nDuration: %dms\nFramework: %s",
                            scenarioName, failureReason, duration, frameworkType));
                } catch (Exception e) {
                    log.warn("Error saving scenario failure evidence: {}", e.getMessage());
                }
            }

            // Ejecutar callback de limpieza
            if (frameworkCleanupCallback != null) {
                try {
                    frameworkCleanupCallback.run();
                    log.debug("Callback de limpieza del framework ejecutado exitosamente");
                } catch (Exception e) {
                    log.warn("Error en callback de limpieza: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error en afterScenario hook: {}", e.getMessage());
        } finally {
            // Limpiar contexto del scenario
            CucumberTestContext.cleanupScenario();
        }
    }

    @Override
    public void executeBeforeStep(String stepText) {
        try {
            log.debug("Ejecutando step: {}", stepText);
            TestLogger.logStep("STEP_START", stepText);

            // Captura automática si está habilitada
            if (autoEvidenceCaptureEnabled) {
                captureEvidence("STEP_START", "Inicio del step",
                    Map.of("step", stepText, "scenario", getCurrentScenario()));
            }

        } catch (Exception e) {
            log.warn("Error en beforeStep hook: {}", e.getMessage());
        }
    }

    @Override
    public void executeAfterStep(String stepText, boolean stepFailed, String failureReason) {
        try {
            // Actualizar estadísticas
            totalStepsExecuted.incrementAndGet();
            if (stepFailed) {
                totalStepsFailed.incrementAndGet();
            }

            String status = stepFailed ? "FALLIDO" : "EXITOSO";
            log.debug("Step completado: {} - {}", stepText, status);
            TestLogger.logStep("STEP_END", stepText + " - " + status);

            // Captura automática si está habilitada
            if (autoEvidenceCaptureEnabled) {
                captureEvidence("STEP_END", "Final del step - " + status,
                    Map.of(
                        "step", stepText,
                        "status", status,
                        "failed", stepFailed,
                        "failureReason", failureReason != null ? failureReason : "N/A",
                        "scenario", getCurrentScenario()
                    ));
            }

            // Evidencia adicional en caso de fallo
            if (stepFailed) {
                try {
                    EvidenceManager.saveErrorEvidence("STEP_FAILURE",
                        "Step falló: " + stepText,
                        String.format("Step: %s\nReason: %s\nScenario: %s\nFramework: %s",
                            stepText, failureReason, getCurrentScenario(), frameworkType));
                } catch (Exception e) {
                    log.warn("Error saving step failure evidence: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.warn("Error en afterStep hook: {}", e.getMessage());
        }
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE HOOKS - SUITE LIFECYCLE
    // =================================================================================

    @Override
    public void executeBeforeAll() {
        try {
            log.info("=== INICIANDO SUITE DE TESTS PARA FRAMEWORK {} ===", frameworkType);

            // Ejecutar callback global
            if (globalSetupCallback != null) {
                try {
                    globalSetupCallback.run();
                    log.debug("Callback de configuración global ejecutado exitosamente");
                } catch (Exception e) {
                    log.warn("Error en callback de configuración global: {}", e.getMessage());
                }
            }

            TestLogger.logInfo("SUITE_START",
                String.format("Iniciando suite de tests para framework %s", frameworkType), null);

        } catch (Exception e) {
            log.error("Error en beforeAll hook: {}", e.getMessage());
            try {
                EvidenceManager.saveErrorEvidence("BEFORE_ALL_ERROR", e.getMessage(), getStackTrace(e));
            } catch (Exception evidenceError) {
                log.warn("Failed to save beforeAll error evidence: {}", evidenceError.getMessage());
            }
        }
    }

    @Override
    public void executeAfterAll() {
        try {
            log.info("=== FINALIZANDO SUITE DE TESTS PARA FRAMEWORK {} ===", frameworkType);

            // Log de estadísticas finales
            Map<String, Object> stats = getExecutionStatistics();
            log.info("Estadísticas finales: {}", stats);

            // Ejecutar callback global de limpieza
            if (globalTeardownCallback != null) {
                try {
                    globalTeardownCallback.run();
                    log.debug("Callback de limpieza global ejecutado exitosamente");
                } catch (Exception e) {
                    log.warn("Error en callback de limpieza global: {}", e.getMessage());
                }
            }

            TestLogger.logInfo("SUITE_END",
                String.format("Suite finalizada para framework %s - %s", frameworkType, stats), null);

        } catch (Exception e) {
            log.error("Error en afterAll hook: {}", e.getMessage());
            try {
                EvidenceManager.saveErrorEvidence("AFTER_ALL_ERROR", e.getMessage(), getStackTrace(e));
            } catch (Exception evidenceError) {
                log.warn("Failed to save afterAll error evidence: {}", evidenceError.getMessage());
            }
        }
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE GESTIÓN DE CONTEXTO
    // =================================================================================

    @Override
    public void storeTestData(String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            log.warn("Intento de almacenar dato con key null o vacía");
            return;
        }
        CucumberTestContext.setValue(key, value);
        log.debug("Dato almacenado en contexto: {} = {}", key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getTestData(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        return CucumberTestContext.getValue(key);
    }

    @Override
    public <T> T getTestData(String key, T defaultValue) {
        return CucumberTestContext.getValue(key, defaultValue);
    }

    @Override
    public boolean hasTestData(String key) {
        return key != null && CucumberTestContext.hasValue(key);
    }

    @Override
    public Map<String, Object> getAllTestData() {
        return CucumberTestContext.getAllData();
    }

    @Override
    public void clearTestData() {
        // CucumberTestContext no tiene método de limpiar datos específicamente
        // pero se limpia automáticamente en clearScenario()
        log.debug("Contexto de datos se limpia automáticamente al finalizar scenario");
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE INFORMACIÓN DEL SCENARIO ACTUAL
    // =================================================================================

    @Override
    public String getCurrentScenario() {
        return CucumberTestContext.getCurrentScenario();
    }

    @Override
    public String getCurrentFeature() {
        return CucumberTestContext.getCurrentFeature();
    }

    @Override
    public long getCurrentScenarioDuration() {
        return CucumberTestContext.getCurrentScenarioDuration();
    }

    @Override
    public boolean isCurrentScenarioFailed() {
        return CucumberTestContext.isScenarioFailed();
    }

    @Override
    public String getCurrentScenarioFailureReason() {
        return CucumberTestContext.getFailureReason();
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE GESTIÓN DE EVIDENCIAS
    // =================================================================================

    @Override
    public void captureEvidence(String evidenceType, String description) {
        captureEvidence(evidenceType, description, null);
    }

    @Override
    public void captureEvidence(String evidenceType, String description, Object evidenceData) {
        try {
            String scenarioName = getCurrentScenario();
            String featureName = getCurrentFeature();

            if (scenarioName == null) {
                scenarioName = "UNKNOWN_SCENARIO";
            }
            if (featureName == null) {
                featureName = "UNKNOWN_FEATURE";
            }

            // Crear contenido de evidencia
            StringBuilder content = new StringBuilder();
            content.append("Tipo de Evidencia: ").append(evidenceType).append("\n");
            content.append("Descripción: ").append(description).append("\n");
            content.append("Framework: ").append(frameworkType).append("\n");
            content.append("Feature: ").append(featureName).append("\n");
            content.append("Scenario: ").append(scenarioName).append("\n");
            content.append("Timestamp: ").append(java.time.LocalDateTime.now()).append("\n");

            if (evidenceData != null) {
                content.append("Datos Específicos:\n");
                if (evidenceData instanceof Map) {
                    Map<?, ?> dataMap = (Map<?, ?>) evidenceData;
                    dataMap.forEach((k, v) -> content.append("  ").append(k).append(": ").append(v).append("\n"));
                } else {
                    content.append("  ").append(evidenceData.toString()).append("\n");
                }
            }

            // Guardar usando EvidenceManager
            String filePath = EvidenceManager.saveCustomEvidence(evidenceType, content.toString(), "txt");
            log.debug("Evidencia capturada: {} - {}", evidenceType, filePath);

        } catch (Exception e) {
            log.warn("Error capturando evidencia {}: {}", evidenceType, e.getMessage());
        }
    }

    @Override
    public void markScenarioAsFailed(String reason, Throwable exception) {
        CucumberTestContext.markScenarioAsFailed(reason, exception);
        log.warn("Scenario marcado como fallido: {}", reason);

        // Capturar evidencia del fallo
        if (autoEvidenceCaptureEnabled) {
            try {
                String content = String.format("Reason: %s\nException: %s\nStackTrace: %s",
                    reason,
                    exception != null ? exception.getMessage() : "N/A",
                    exception != null ? getStackTrace(exception) : "N/A");

                EvidenceManager.saveErrorEvidence("SCENARIO_MARKED_FAILED", reason, content);
            } catch (Exception e) {
                log.warn("Error guardando evidencia de fallo marcado: {}", e.getMessage());
            }
        }
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE CALLBACKS
    // =================================================================================

    @Override
    public void setFrameworkInitializationCallback(Runnable callback) {
        this.frameworkInitializationCallback = callback;
        log.debug("Callback de inicialización del framework configurado");
    }

    @Override
    public void setFrameworkCleanupCallback(Runnable callback) {
        this.frameworkCleanupCallback = callback;
        log.debug("Callback de limpieza del framework configurado");
    }

    @Override
    public void setGlobalSetupCallback(Runnable callback) {
        this.globalSetupCallback = callback;
        log.debug("Callback de configuración global configurado");
    }

    @Override
    public void setGlobalTeardownCallback(Runnable callback) {
        this.globalTeardownCallback = callback;
        log.debug("Callback de limpieza global configurado");
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE UTILIDADES
    // =================================================================================

    @Override
    public void setAutoEvidenceCaptureEnabled(boolean enabled) {
        this.autoEvidenceCaptureEnabled = enabled;
        log.debug("Captura automática de evidencias: {}", enabled ? "HABILITADA" : "DESHABILITADA");
    }

    @Override
    public boolean isAutoEvidenceCaptureEnabled() {
        return autoEvidenceCaptureEnabled;
    }

    @Override
    public Map<String, Object> getExecutionStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("framework", frameworkType);
        stats.put("totalScenariosExecuted", totalScenariosExecuted.get());
        stats.put("totalScenariosFailed", totalScenariosFailed.get());
        stats.put("totalScenariosSuccess", totalScenariosExecuted.get() - totalScenariosFailed.get());
        stats.put("scenarioSuccessRate", calculateSuccessRate(totalScenariosExecuted.get(), totalScenariosFailed.get()));
        stats.put("totalStepsExecuted", totalStepsExecuted.get());
        stats.put("totalStepsFailed", totalStepsFailed.get());
        stats.put("totalStepsSuccess", totalStepsExecuted.get() - totalStepsFailed.get());
        stats.put("stepSuccessRate", calculateSuccessRate(totalStepsExecuted.get(), totalStepsFailed.get()));
        stats.put("totalExecutionTime", totalExecutionTime.get());
        stats.put("averageScenarioTime", calculateAverageTime());
        stats.put("autoEvidenceCapture", autoEvidenceCaptureEnabled);
        return stats;
    }

    @Override
    public String getDebugInfo() {
        Map<String, Object> stats = getExecutionStatistics();
        StringBuilder info = new StringBuilder();
        info.append("BaseCucumberHooksService Debug Info:\n");
        info.append("=====================================\n");
        info.append("Framework Type: ").append(frameworkType).append("\n");
        info.append("Auto Evidence Capture: ").append(autoEvidenceCaptureEnabled).append("\n");
        info.append("Framework Properties: ").append(frameworkProperties.size()).append(" configuradas\n");
        info.append("Callbacks Configurados:\n");
        info.append("  - Initialization: ").append(frameworkInitializationCallback != null ? "SÍ" : "NO").append("\n");
        info.append("  - Cleanup: ").append(frameworkCleanupCallback != null ? "SÍ" : "NO").append("\n");
        info.append("  - Global Setup: ").append(globalSetupCallback != null ? "SÍ" : "NO").append("\n");
        info.append("  - Global Teardown: ").append(globalTeardownCallback != null ? "SÍ" : "NO").append("\n");
        info.append("Estadísticas de Ejecución:\n");
        stats.forEach((key, value) -> info.append("  ").append(key).append(": ").append(value).append("\n"));

        // Info del contexto actual
        String currentScenario = getCurrentScenario();
        String currentFeature = getCurrentFeature();
        info.append("Contexto Actual:\n");
        info.append("  - Scenario: ").append(currentScenario != null ? currentScenario : "NINGUNO").append("\n");
        info.append("  - Feature: ").append(currentFeature != null ? currentFeature : "NINGUNO").append("\n");
        info.append("  - Scenario Failed: ").append(isCurrentScenarioFailed()).append("\n");

        return info.toString();
    }

    @Override
    public void reset() {
        totalScenariosExecuted.set(0);
        totalScenariosFailed.set(0);
        totalStepsExecuted.set(0);
        totalStepsFailed.set(0);
        totalExecutionTime.set(0);
        frameworkProperties.clear();
        log.debug("Servicio de hooks reiniciado");
    }

    // =================================================================================
    // MÉTODOS PRIVADOS DE UTILIDAD
    // =================================================================================

    private String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private double calculateSuccessRate(int total, int failed) {
        if (total == 0) return 100.0;
        return ((double) (total - failed) / total) * 100.0;
    }

    private double calculateAverageTime() {
        int executed = totalScenariosExecuted.get();
        if (executed == 0) return 0.0;
        return (double) totalExecutionTime.get() / executed;
    }
}
