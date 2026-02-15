package com.scotia.qa.common.reporting.jira.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.reporting.core.config.JiraConfig;
import com.scotia.qa.common.reporting.core.model.ScenarioResult;
import com.scotia.qa.common.reporting.core.model.StepResult;
import com.scotia.qa.common.reporting.core.model.TestExecutionResult;
import com.scotia.qa.common.reporting.core.model.TestStatus;
import com.scotia.qa.common.reporting.jira.client.JiraHttpClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JiraUpdateService {

    private final JiraConfig config;
    private final JiraHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public JiraUpdateService(JiraConfig config) {
        this.config = config;
        this.httpClient = new JiraHttpClient(config);
        this.objectMapper = new ObjectMapper();
    }

    public int updateTestStatus(TestExecutionResult result) throws IOException {
        TestLogger.logInfo("JIRA_UPDATE",
            String.format("📤 Actualizando status en Jira (modo: %s)", config.getUpdateMode()), null);

        // ✅ NUEVO: Verificar si necesitamos crear Test Execution
        ensureTestExecutionExists(result);

        if (config.getUpdateMode() == JiraConfig.UpdateMode.BATCH) {
            return updateBatch(result);
        } else {
            return updateSingle(result);
        }
    }

    /**
     * Verifica que exista un Test Execution válido.
     * Si autoCreateExecution=true y no hay testExecutionId, crea uno nuevo.
     */
    private void ensureTestExecutionExists(TestExecutionResult result) throws IOException {
        // Si ya existe testExecutionId, no hacer nada
        if (config.getTestExecutionId() != null && !config.getTestExecutionId().isEmpty()) {
            TestLogger.logInfo("JIRA_UPDATE",
                String.format("✅ Usando Test Execution existente: %s", config.getTestExecutionId()), null);
            return;
        }

        // Si no está habilitada auto-creación, fallar
        if (!config.isAutoCreateExecution()) {
            throw new IllegalStateException(
                "❌ No se proporcionó testExecutionId y autoCreateExecution=false. " +
                "Configura jira.testExecutionId o habilita jira.autoCreateExecution=true"
            );
        }

        // ✅ Crear nuevo Test Execution
        TestLogger.logInfo("JIRA_UPDATE", "🆕 Creando nuevo Test Execution automáticamente...", null);

        Map<String, Object> payload = new HashMap<>();

        Map<String, Object> fields = new HashMap<>();
        fields.put("project", Map.of("key", config.getProjectKey()));
        fields.put("summary", result.getSummary() != null ?
            result.getSummary() : "Automated Test Execution - " + java.time.LocalDateTime.now());
        fields.put("issuetype", Map.of("name", "Test Execution"));

        // Environment personalizado de Xray
        if (config.getTestEnvironment() != null) {
            fields.put("customfield_11805", List.of(config.getTestEnvironment())); // ID del campo en tu Jira
        }

        payload.put("fields", fields);

        String endpoint = "/rest/api/2/issue";
        String jsonPayload = objectMapper.writeValueAsString(payload);

        String response = httpClient.post(endpoint, jsonPayload);

        // Parsear respuesta para obtener el ID del Test Execution creado
        @SuppressWarnings("unchecked")  // Suprime warning de conversión genérica
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        String newExecutionId = (String) responseMap.get("key");

        // ⚠️ NOTA: Necesitamos una forma de setear el testExecutionId en JiraConfig
        // Por ahora, lo logueamos para que el usuario lo sepa
        TestLogger.logInfo("JIRA_UPDATE",
            String.format("✅ Test Execution creado: %s", newExecutionId), null);

        // TODO: Agregar setter en JiraConfig para actualizar testExecutionId
        // config.setTestExecutionId(newExecutionId);
    }

    private int updateBatch(TestExecutionResult result) throws IOException {
        TestLogger.logInfo("JIRA_UPDATE", "🔄 Modo BATCH: actualizando tests usando Xray Import API", null);

        // ✅ USAR TEST EXECUTION EXISTENTE
        String testExecutionId = config.getTestExecutionId();

        if (testExecutionId == null || testExecutionId.isEmpty()) {
            throw new IllegalStateException(
                "❌ jira.testExecutionId no configurado. " +
                "Configura ${TEST_EXECUTION_ID} en tu .env.local"
            );
        }

        TestLogger.logInfo("JIRA_UPDATE",
            String.format("📋 Actualizando tests en Test Execution: %s", testExecutionId), null);

        // Construir payload usando el formato estándar de Xray JSON Import
        Map<String, Object> payload = new HashMap<>();

        // Especificar el Test Execution existente por su KEY
        payload.put("testExecutionKey", testExecutionId);

        // Agregar tests con sus resultados
        List<Map<String, Object>> tests = new java.util.ArrayList<>();

        for (ScenarioResult scenario : result.getScenarios()) {
            if (scenario.getTestKey() == null) {
                TestLogger.logWarning("JIRA_UPDATE",
                    "⚠️ Scenario sin test key, omitiendo: " + scenario.getScenarioName(), null);
                continue;
            }

            Map<String, Object> test = new HashMap<>();
            test.put("testKey", scenario.getTestKey());
            test.put("status", mapStatus(scenario.getStatus()));

            // Construir comentario enriquecido con steps
            String comment = buildJiraComment(scenario);

            if (comment != null && !comment.isEmpty()) {
                // Decodificar HTML entities para que Jira lo muestre correctamente
                comment = decodeHtmlEntities(comment);
                test.put("comment", comment);
            }

            tests.add(test);
        }

        payload.put("tests", tests);

        // Usar el endpoint de importación de Xray
        String endpoint = "/rest/raven/1.0/import/execution";
        String jsonPayload = objectMapper.writeValueAsString(payload);

        TestLogger.logDebug("JIRA_UPDATE", "Payload JSON: " + jsonPayload, null);

        try {
            httpClient.post(endpoint, jsonPayload);

            TestLogger.logInfo("JIRA_UPDATE",
                String.format("✅ %d tests actualizados en %s", tests.size(), testExecutionId), null);

            return tests.size();

        } catch (IOException e) {
            TestLogger.logError("JIRA_UPDATE",
                "❌ Error en Xray Import API v1.0, intentando con v2.0...", null);

            // Intentar con API v2.0 como fallback
            return updateBatchV2(testExecutionId, tests);
        }
    }

    /**
     * Fallback usando Xray API v2.0 (para instalaciones más nuevas)
     */
    private int updateBatchV2(String testExecutionId, List<Map<String, Object>> tests) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("testExecutionKey", testExecutionId);
        payload.put("tests", tests);

        String endpoint = "/rest/raven/2.0/import/execution";
        String jsonPayload = objectMapper.writeValueAsString(payload);

        httpClient.post(endpoint, jsonPayload);

        TestLogger.logInfo("JIRA_UPDATE",
            String.format("✅ %d tests actualizados en %s (usando API v2.0)", tests.size(), testExecutionId), null);

        return tests.size();
    }

    private int updateSingle(TestExecutionResult result) throws IOException {
        TestLogger.logInfo("JIRA_UPDATE", "🔄 Modo SINGLE: actualizando tests uno por uno", null);

        int updated = 0;
        for (ScenarioResult scenario : result.getScenarios()) {
            if (scenario.getTestKey() == null) {
                continue;
            }

            try {
                updateSingleTest(scenario);
                updated++;
            } catch (IOException e) {
                if (config.isFailOnError()) {
                    throw e;
                }
            }
        }

        TestLogger.logInfo("JIRA_UPDATE",
            String.format("✅ %d/%d tests actualizados", updated, result.getScenarios().size()), null);

        return updated;
    }

    private void updateSingleTest(ScenarioResult scenario) throws IOException {
        String endpoint = String.format("/rest/api/2/issue/%s/transitions", scenario.getTestKey());

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> transition = new HashMap<>();
        transition.put("id", getTransitionId(scenario.getStatus()));
        payload.put("transition", transition);

        String jsonPayload = objectMapper.writeValueAsString(payload);

        httpClient.post(endpoint, jsonPayload);
    }

    private String mapStatus(TestStatus status) {
        switch (status) {
            case PASS: return "PASS";
            case FAIL: return "FAIL";
            case SKIP: return "ABORTED";
            case TODO: return "TODO";
            case EXECUTING: return "EXECUTING";
            default: return "TODO";
        }
    }

    /**
     * Decodifica HTML entities a texto normal.
     * Convierte &oacute; → ó, &eacute; → é, &quot; → ", etc.
     *
     * @param text texto con HTML entities
     * @return texto con caracteres normales
     */
    private String decodeHtmlEntities(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Decodificar entidades HTML comunes manualmente
        // (Evitamos dependencia de Apache Commons Text por simplicidad)
        String decoded = text;

        // Caracteres especiales comunes en español
        decoded = decoded.replace("&aacute;", "á");
        decoded = decoded.replace("&eacute;", "é");
        decoded = decoded.replace("&iacute;", "í");
        decoded = decoded.replace("&oacute;", "ó");
        decoded = decoded.replace("&uacute;", "ú");
        decoded = decoded.replace("&ntilde;", "ñ");
        decoded = decoded.replace("&Aacute;", "Á");
        decoded = decoded.replace("&Eacute;", "É");
        decoded = decoded.replace("&Iacute;", "Í");
        decoded = decoded.replace("&Oacute;", "Ó");
        decoded = decoded.replace("&Uacute;", "Ú");
        decoded = decoded.replace("&Ntilde;", "Ñ");

        // Caracteres especiales comunes
        decoded = decoded.replace("&quot;", "\"");
        decoded = decoded.replace("&apos;", "'");
        decoded = decoded.replace("&lt;", "<");
        decoded = decoded.replace("&gt;", ">");
        decoded = decoded.replace("&amp;", "&");  // Este último para evitar double-decoding

        return decoded;
    }

    /**
     * Construye un comentario enriquecido para Jira con información de steps.
     *
     * @param scenario ScenarioResult con información del test
     * @return Comentario formateado para Jira
     */
    private String buildJiraComment(ScenarioResult scenario) {
        StringBuilder comment = new StringBuilder();

        // 1. Mensaje de error principal (si existe)
        String mainMessage = scenario.getBusinessMessage() != null && !scenario.getBusinessMessage().isEmpty()
                ? scenario.getBusinessMessage()
                : scenario.getErrorMessage();

        if (mainMessage != null && !mainMessage.isEmpty()) {
            comment.append("*Error:* ").append(mainMessage).append("\n\n");
        }

        // 2. Lista de steps ejecutados (si existen)
        if (scenario.getSteps() != null && !scenario.getSteps().isEmpty()) {
            comment.append("*Steps ejecutados:*\n");

            for (StepResult step : scenario.getSteps()) {
                String emoji = step.getStatusEmoji();
                String stepText = step.getFullStepText();
                String duration = step.getFormattedDuration();

                comment.append(String.format("%s {{%s}} (%s)\n",
                        emoji, stepText, duration));
            }

            comment.append("\n");
        }

        // 3. Resumen
        comment.append(String.format("*Resumen:* %d/%d steps passed (%dms)",
                scenario.getPassedSteps(),
                scenario.getTotalSteps(),
                scenario.getDurationMs()));

        return comment.toString();
    }

    private String getTransitionId(TestStatus status) {
        switch (status) {
            case PASS: return "31";
            case FAIL: return "41";
            default: return "11";
        }
    }

    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}

