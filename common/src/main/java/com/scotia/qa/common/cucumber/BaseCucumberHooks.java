package com.scotia.qa.common.cucumber;

import com.scotia.qa.common.logging.EvidenceManager;
import com.scotia.qa.common.logging.TestLogger;

/**
 * Clase base abstracta para hooks de Cucumber reutilizables.
 * Proporciona funcionalidad común para Before, After, BeforeStep, AfterStep hooks.
 * Los frameworks específicos (API, Web, Mobile) deben extender esta clase.
 *
 * 📚 DOCUMENTACIÓN COMPLETA: Ver /common/README.md sección "🥒 Paquete Cucumber"
 *    Incluye guía detallada sobre:
 *    - Cómo usar esta clase base
 *    - Qué métodos abstractos implementar
 *    - Ciclo de vida completo de hooks
 *    - Ejemplos paso a paso
 *
 * @see ExampleFrameworkHooks - Template con ejemplo completo de implementación
 * @see CucumberTestContext - Contexto compartido entre steps
 *
 * @author Scotia QA Framework Team
 * @since 1.0.0
 */
public abstract class BaseCucumberHooks {


    /**
     * Framework específico (API, WEB, MOBILE).
     * Debe ser implementado por las clases hijas.
     */
    protected abstract String getFrameworkType();

    // =================================================================================
    // BEFORE HOOKS
    // =================================================================================

    /**
     * Hook que se ejecuta antes de cada escenario.
     * Inicializa el contexto común y llama a la inicialización específica del framework.
     */
    protected void beforeScenario(String scenarioName, String featureName) {
        try {
            TestLogger.logInfo("SCENARIO_INIT",
                              String.format("=== STARTING SCENARIO: %s ===", scenarioName), null);

            // Inicializar contexto común
            CucumberTestContext.initializeScenario(scenarioName, featureName, getFrameworkType());

            // Log del inicio
            TestLogger.logStep("SCENARIO_START",
                              String.format("Iniciando escenario '%s' en feature '%s'", scenarioName, featureName));

            // Inicialización específica del framework
            performFrameworkSpecificInitialization();

        } catch (Exception e) {
            TestLogger.logError("SCENARIO_INIT_ERROR",
                               String.format("Error in beforeScenario hook: %s", e.getMessage()), null);
            CucumberTestContext.markScenarioAsFailed("Error en inicialización", e);

            try {
                EvidenceManager.saveErrorEvidence("BEFORE_SCENARIO_ERROR", e.getMessage(),
                                                 getStackTrace(e));
            } catch (Exception evidenceError) {
                TestLogger.logWarning("EVIDENCE_ERROR",
                                     String.format("Failed to save error evidence: %s", evidenceError.getMessage()), null);
            }
        }
    }

    /**
     * Hook que se ejecuta antes de todos los tests.
     * Configuración global del framework.
     */
    protected void beforeAll() {
        try {
            TestLogger.logInfo("SUITE_INIT",
                              String.format("=== STARTING TEST SUITE FOR %s FRAMEWORK ===", getFrameworkType()), null);

            // Configuración global
            performGlobalSetup();

            TestLogger.logInfo("SUITE_START",
                             String.format("Iniciando suite de tests para framework %s", getFrameworkType()),
                             null);

        } catch (Exception e) {
            TestLogger.logError("SUITE_INIT_ERROR",
                               String.format("Error in beforeAll hook: %s", e.getMessage()), null);
            try {
                EvidenceManager.saveErrorEvidence("BEFORE_ALL_ERROR", e.getMessage(),
                                                 getStackTrace(e));
            } catch (Exception evidenceError) {
                TestLogger.logWarning("EVIDENCE_ERROR",
                                     String.format("Failed to save error evidence: %s", evidenceError.getMessage()), null);
            }
        }
    }

    // =================================================================================
    // AFTER HOOKS
    // =================================================================================

    /**
     * Hook que se ejecuta después de cada escenario.
     * Realiza limpieza y captura evidencias en caso de fallo.
     */
    protected void afterScenario(boolean scenarioFailed, String failureReason) {
        String scenarioName = CucumberTestContext.getCurrentScenario();
        long duration = CucumberTestContext.getCurrentScenarioDuration();

        try {
            if (scenarioFailed) {
                TestLogger.logError("SCENARIO_FAILED",
                                  String.format("=== SCENARIO FAILED: %s === - %s", scenarioName, failureReason),
                                  CucumberTestContext.getAllData());

                // Capturar evidencias de fallo
                captureFailureEvidence(failureReason);

            } else {
                TestLogger.logInfo("SCENARIO_PASSED",
                                 String.format("=== SCENARIO PASSED: %s ===", scenarioName), null);
                TestLogger.logStep("SCENARIO_PASS",
                                 String.format("Escenario '%s' completado exitosamente", scenarioName));
            }

            // Log de duración
            TestLogger.logInfo("SCENARIO_DURATION",
                             String.format("Escenario completado en %d ms", duration),
                             null);

            // Limpieza específica del framework
            performFrameworkSpecificCleanup(scenarioFailed);

        } catch (Exception e) {
            TestLogger.logError("AFTER_SCENARIO_ERROR",
                               String.format("Error in afterScenario hook: %s", e.getMessage()), null);
            try {
                EvidenceManager.saveErrorEvidence("AFTER_SCENARIO_ERROR", e.getMessage(),
                                                 getStackTrace(e));
            } catch (Exception evidenceError) {
                TestLogger.logWarning("EVIDENCE_ERROR",
                                     String.format("Failed to save error evidence: %s", evidenceError.getMessage()), null);
            }
        } finally {
            // Siempre limpiar el contexto
            CucumberTestContext.cleanupScenario();
        }
    }

    /**
     * Hook que se ejecuta después de todos los tests.
     * Limpieza global del framework.
     */
    protected void afterAll() {
        try {
            TestLogger.logInfo("SUITE_COMPLETION",
                              String.format("=== COMPLETING TEST SUITE FOR %s FRAMEWORK ===", getFrameworkType()), null);

            // Limpieza global
            performGlobalCleanup();

            TestLogger.logInfo("SUITE_END",
                             String.format("Finalizando suite de tests para framework %s", getFrameworkType()),
                             null);

        } catch (Exception e) {
            TestLogger.logError("AFTER_ALL_ERROR",
                               String.format("Error in afterAll hook: %s", e.getMessage()), null);
            try {
                EvidenceManager.saveErrorEvidence("AFTER_ALL_ERROR", e.getMessage(),
                                                 getStackTrace(e));
            } catch (Exception evidenceError) {
                TestLogger.logWarning("EVIDENCE_ERROR",
                                     String.format("Failed to save error evidence: %s", evidenceError.getMessage()), null);
            }
        }
    }

    // =================================================================================
    // STEP HOOKS
    // =================================================================================

    /**
     * Hook que se ejecuta antes de cada step.
     */
    protected void beforeStep(String stepText) {
        try {
            TestLogger.logStep("STEP_START", stepText);

            // Preparación específica del framework para el step
            prepareFrameworkForStep(stepText);

        } catch (Exception e) {
            TestLogger.logError("BEFORE_STEP_ERROR",
                               String.format("Error in beforeStep hook: %s", e.getMessage()), null);
            CucumberTestContext.markScenarioAsFailed("Error en beforeStep", e);
        }
    }

    /**
     * Hook que se ejecuta después de cada step.
     */
    protected void afterStep(String stepText, boolean stepFailed, String errorMessage) {
        try {
            if (stepFailed) {
                TestLogger.logError("STEP_FAILED",
                                  String.format("Step falló: %s - %s", stepText, errorMessage),
                                  CucumberTestContext.getAllData());

                // Capturar evidencia del step fallido
                captureStepFailureEvidence(stepText, errorMessage);

                // Marcar escenario como fallido
                CucumberTestContext.markScenarioAsFailed("Step falló: " + stepText, null);

            } else {
                TestLogger.logStep("STEP_COMPLETED", stepText);
            }

            // Post-procesamiento específico del framework
            postProcessFrameworkStep(stepText, stepFailed);

        } catch (Exception e) {
            TestLogger.logError("AFTER_STEP_ERROR",
                               String.format("Error in afterStep hook: %s", e.getMessage()), null);
            try {
                EvidenceManager.saveErrorEvidence("AFTER_STEP_ERROR", e.getMessage(),
                                                 getStackTrace(e));
            } catch (Exception evidenceError) {
                TestLogger.logWarning("EVIDENCE_ERROR",
                                     String.format("Failed to save error evidence: %s", evidenceError.getMessage()), null);
            }
        }
    }

    // =================================================================================
    // MÉTODOS ABSTRACTOS - DEBEN SER IMPLEMENTADOS POR FRAMEWORKS ESPECÍFICOS
    // =================================================================================

    /**
     * Configuración global específica del framework.
     */
    protected abstract void performGlobalSetup();

    /**
     * Limpieza global específica del framework.
     */
    protected abstract void performGlobalCleanup();

    /**
     * Inicialización específica del framework para cada escenario.
     */
    protected abstract void performFrameworkSpecificInitialization();

    /**
     * Limpieza específica del framework para cada escenario.
     */
    protected abstract void performFrameworkSpecificCleanup(boolean scenarioFailed);

    /**
     * Preparación específica del framework antes de cada step.
     */
    protected abstract void prepareFrameworkForStep(String stepText);

    /**
     * Post-procesamiento específico del framework después de cada step.
     */
    protected abstract void postProcessFrameworkStep(String stepText, boolean stepFailed);

    /**
     * Captura evidencias específicas del framework en caso de fallo.
     */
    protected abstract void captureFrameworkSpecificEvidence(String reason);

    // =================================================================================
    // MÉTODOS DE UTILIDAD
    // =================================================================================

    /**
     * Captura evidencias generales en caso de fallo del escenario.
     */
    protected void captureFailureEvidence(String reason) {
        // Guardar contexto del test
        try {
            EvidenceManager.saveCustomEvidence("test_context",
                                               convertMapToJson(CucumberTestContext.getAllData()),
                                               "json");
        } catch (Exception e) {
            TestLogger.logWarning("EVIDENCE_ERROR",
                                 String.format("Failed to save test context evidence: %s", e.getMessage()), null);
        }

        // Capturar evidencias específicas del framework
        captureFrameworkSpecificEvidence(reason);

    }

    /**
     * Captura evidencias específicas para fallos de steps.
     */
    protected void captureStepFailureEvidence(String stepText, String errorMessage) {
        try {
            EvidenceManager.saveErrorEvidence("STEP_FAILURE",
                                             String.format("Step: %s, Error: %s", stepText, errorMessage),
                                             null);
        } catch (Exception e) {
            TestLogger.logWarning("EVIDENCE_ERROR",
                                 String.format("Failed to save step failure evidence: %s", e.getMessage()), null);
        }
    }

    /**
     * Convierte el stack trace a string.
     */
    protected String getStackTrace(Throwable throwable) {
        if (throwable == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");

        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Convierte un mapa a JSON simple.
     */
    protected String convertMapToJson(java.util.Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }

        StringBuilder json = new StringBuilder("{\n");

        map.forEach((key, value) -> {
            json.append("  \"").append(key).append("\": ");

            if (value instanceof String) {
                json.append("\"").append(((String) value).replace("\"", "\\\"")).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value.toString());
            } else {
                json.append("\"").append(value != null ? value.toString().replace("\"", "\\\"") : "null").append("\"");
            }

            json.append(",\n");
        });

        // Remover la última coma
        if (json.length() > 2) {
            json.setLength(json.length() - 2);
            json.append("\n");
        }

        json.append("}");
        return json.toString();
    }
}
