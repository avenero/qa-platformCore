package com.scotia.qa.common.jiraXray.client;

import com.scotia.qa.common.jiraXray.config.ReportConfig;
import com.scotia.qa.common.jiraXray.model.TestExecutionResult;
import com.scotia.qa.common.jiraXray.model.ScenarioResult;
import com.scotia.qa.common.logging.TestLogger;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Cliente HTTP para comunicación con Jira.
 * Maneja el envío de resultados de tests a Jira en batches.
 * Integrado con el sistema de logging del framework Scotia QA.
 *
 * @author Scotia QA Framework Team
 * @since 1.0.0
 */
public class JiraClient {


    private static final String JIRA_BASE_URL = "https://jira.agile.bns";

    private final ReportConfig config;

    public JiraClient(ReportConfig config) {
        this.config = config;
    }

    /**
     * Envía resultados de ejecución a Jira en batch optimizado
     *
     * @param testResults resultados procesados de la ejecución
     * @return resumen de la operación
     */
    public BatchUpdateResult sendTestResults(TestExecutionResult testResults) {
        TestLogger.logInfo("JIRA_CLIENT", "📤 Enviando batch OPTIMIZADO de " + testResults.getScenarios().size() + " resultados a Jira", null);

        BatchUpdateResult batchResult = new BatchUpdateResult();
        List<ScenarioResult> scenarios = testResults.getScenarios();

        // OPTIMIZACIÓN: Agrupar scenarios por test key para evitar duplicados
        java.util.Map<String, ScenarioResult> uniqueScenarios = new java.util.LinkedHashMap<>();

        for (ScenarioResult scenario : scenarios) {
            String testKey = scenario.getTestKey();

            // Si ya existe este test key, usar el último estado (failed prevalece sobre passed)
            if (uniqueScenarios.containsKey(testKey)) {
                ScenarioResult existing = uniqueScenarios.get(testKey);

                // Si el nuevo es FAIL y el existente no, reemplazar
                if (scenario.getStatus() == com.scotia.qa.common.jiraXray.model.TestStatus.FAIL &&
                    existing.getStatus() != com.scotia.qa.common.jiraXray.model.TestStatus.FAIL) {
                    uniqueScenarios.put(testKey, scenario);
                    TestLogger.logInfo("JIRA_CLIENT", "🔄 Actualizando " + testKey + " de " + existing.getStatus() + " a " + scenario.getStatus() + " (failed prevalece)", null);
                }
            } else {
                uniqueScenarios.put(testKey, scenario);
            }
        }

        TestLogger.logInfo("JIRA_CLIENT", "📊 Consolidados " + uniqueScenarios.size() + " scenarios únicos de " + scenarios.size() + " totales", null);

        // Enviar todos los scenarios únicos en un solo batch
        try {
            updateTestExecutionBatch(new java.util.ArrayList<>(uniqueScenarios.values()), testResults.getTestExecutionKey());

            // Marcar todos como exitosos si el batch funcionó
            for (String testKey : uniqueScenarios.keySet()) {
                batchResult.addSuccess(testKey);
            }

        } catch (Exception e) {
            TestLogger.logException("JIRA_CLIENT", "❌ Error enviando batch: " + e.getMessage(), e);

            // Marcar todos como fallidos si el batch falló
            for (String testKey : uniqueScenarios.keySet()) {
                batchResult.addFailure(testKey, e.getMessage());
            }
        }

        logBatchSummary(batchResult);
        return batchResult;
    }

    /**
     * Actualiza un test individual en el Test Execution de Jira
     */
    private void updateTestExecution(ScenarioResult scenario, String testExecutionKey) throws IOException {
        // Endpoint para actualizar test execution results
        String endpoint = JIRA_BASE_URL + "/rest/raven/1.0/import/execution";

        TestLogger.logInfo("JIRA_CLIENT", "🎯 Enviando a Jira:" + null, null);
        TestLogger.logInfo("JIRA_CLIENT", "   URL: " + endpoint, null);
        TestLogger.logInfo("JIRA_CLIENT", "   Test Key: " + scenario.getTestKey(), null);
        TestLogger.logInfo("JIRA_CLIENT", "   Status: " + scenario.getStatus().getJiraStatus(), null);
        TestLogger.logInfo("JIRA_CLIENT", "   Test Execution: " + (testExecutionKey != null ? testExecutionKey : config.getTestExecution()), null);

        // Construir JSON para Xray (formato estándar para test execution)
        String json = buildTestExecutionPayload(scenario, testExecutionKey);

        TestLogger.logInfo("JIRA_CLIENT", "📤 Payload JSON:", null);
        TestLogger.logInfo("JIRA_CLIENT", json, null);

        HttpURLConnection connection = createConnection(endpoint, "POST");

        // Enviar datos
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Leer respuesta
        int responseCode = connection.getResponseCode();
        String response = readResponse(connection, responseCode >= 400);

        TestLogger.logInfo("JIRA_CLIENT", "📥 Respuesta de Jira:", null);
        TestLogger.logInfo("JIRA_CLIENT", "   Código HTTP: " + responseCode, null);
        TestLogger.logInfo("JIRA_CLIENT", "   Respuesta: " + response, null);

        if (responseCode >= 400) {
            TestLogger.logException("JIRA_CLIENT", "❌ Error en envío a Jira: Test Key " + scenario.getTestKey() + ", HTTP " + responseCode + ": " + response, new IOException("HTTP " + responseCode));
            throw new IOException("Error HTTP " + responseCode + " para " + scenario.getTestKey() + ": " + response);
        }

        TestLogger.logInfo("JIRA_CLIENT", "✅ Envío exitoso para " + scenario.getTestKey(), null);
    }

    /**
     * Actualiza múltiples tests en el Test Execution de Jira en un solo request batch
     */
    private void updateTestExecutionBatch(List<ScenarioResult> scenarios, String testExecutionKey) throws IOException {
        String endpoint = JIRA_BASE_URL + "/rest/raven/1.0/import/execution";

        TestLogger.logInfo("JIRA_CLIENT", "🎯 Enviando BATCH a Jira:", null);
        TestLogger.logInfo("JIRA_CLIENT", "   URL: " + endpoint, null);
        TestLogger.logInfo("JIRA_CLIENT", "   Test Execution: " + (testExecutionKey != null ? testExecutionKey : config.getTestExecution()), null);
        TestLogger.logInfo("JIRA_CLIENT", "   Scenarios en batch: " + scenarios.size(), null);

        for (ScenarioResult scenario : scenarios) {
            TestLogger.logInfo("JIRA_CLIENT", "   - Test Key: " + scenario.getTestKey() + " -> Status: " + scenario.getStatus().getJiraStatus(), null);
        }

        // Construir JSON batch para Xray
        String json = buildBatchTestExecutionPayload(scenarios, testExecutionKey);

        TestLogger.logInfo("JIRA_CLIENT", "📤 Payload JSON BATCH:", null);
        TestLogger.logInfo("JIRA_CLIENT", json, null);

        HttpURLConnection connection = createConnection(endpoint, "POST");

        // Enviar datos
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = json.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Leer respuesta
        int responseCode = connection.getResponseCode();
        String response = readResponse(connection, responseCode >= 400);

        TestLogger.logInfo("JIRA_CLIENT", "📥 Respuesta de Jira:", null);
        TestLogger.logInfo("JIRA_CLIENT", "   Código HTTP: " + responseCode, null);
        TestLogger.logInfo("JIRA_CLIENT", "   Respuesta: " + response, null);

        if (responseCode >= 400) {
            TestLogger.logException("JIRA_CLIENT", "❌ Error en envío batch a Jira: HTTP " + responseCode + ": " + response, new IOException("HTTP " + responseCode));
            throw new IOException("Error HTTP " + responseCode + " en batch: " + response);
        }

        TestLogger.logInfo("JIRA_CLIENT", "✅ Envío batch exitoso para " + scenarios.size() + " scenarios", null);
    }

    /**
     * Construye el JSON payload para múltiples tests en batch
     */
    private String buildBatchTestExecutionPayload(List<ScenarioResult> scenarios, String testExecutionKey) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"testExecutionKey\": \"").append(testExecutionKey != null ? testExecutionKey : config.getTestExecution()).append("\",\n");
        json.append("  \"tests\": [\n");

        for (int i = 0; i < scenarios.size(); i++) {
            ScenarioResult scenario = scenarios.get(i);

            json.append("    {\n");
            json.append("      \"testKey\": \"").append(scenario.getTestKey()).append("\",\n");
            json.append("      \"status\": \"").append(scenario.getStatus().getJiraStatus()).append("\"");

            // Agregar información adicional si está disponible
            if (scenario.getStartTime() != null) {
                json.append(",\n      \"start\": \"").append(scenario.getStartTime().toString()).append("\"");
            }
            if (scenario.getEndTime() != null) {
                json.append(",\n      \"finish\": \"").append(scenario.getEndTime().toString()).append("\"");
            }
            if (scenario.getErrorMessage() != null) {
                json.append(",\n      \"comment\": \"").append(escapeJson(scenario.getErrorMessage())).append("\"");
            }

            json.append("\n    }");

            // Agregar coma si no es el último elemento
            if (i < scenarios.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}");

        return json.toString();
    }

    /**
     * Construye el JSON payload para Xray test execution individual (método legacy)
     */
    private String buildTestExecutionPayload(ScenarioResult scenario, String testExecutionKey) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"testExecutionKey\": \"").append(testExecutionKey != null ? testExecutionKey : config.getTestExecution()).append("\",\n");
        json.append("  \"tests\": [\n");
        json.append("    {\n");
        json.append("      \"testKey\": \"").append(scenario.getTestKey()).append("\",\n");
        json.append("      \"status\": \"").append(scenario.getStatus().getJiraStatus()).append("\"");

        // Agregar información adicional si está disponible
        if (scenario.getStartTime() != null) {
            json.append(",\n      \"start\": \"").append(scenario.getStartTime().toString()).append("\"");
        }
        if (scenario.getEndTime() != null) {
            json.append(",\n      \"finish\": \"").append(scenario.getEndTime().toString()).append("\"");
        }
        if (scenario.getErrorMessage() != null) {
            json.append(",\n      \"comment\": \"").append(escapeJson(scenario.getErrorMessage())).append("\"");
        }

        json.append("\n    }\n");
        json.append("  ]\n");
        json.append("}");

        return json.toString();
    }

    /**
     * Escapa caracteres especiales para JSON
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Crea la conexión HTTP con headers apropiados
     */
    private HttpURLConnection createConnection(String endpoint, String method) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Agregar autenticación
        String authHeader = buildAuthorizationHeader();
        if (authHeader != null) {
            connection.setRequestProperty("Authorization", authHeader);
        } else {
            throw new IllegalStateException("Credenciales de Jira no configuradas");
        }

        return connection;
    }

    /**
     * Construye el header de autorización Basic Auth
     */
    private String buildAuthorizationHeader() {
        if (config.getUser() == null || config.getUser().trim().isEmpty()) {
            return null;
        }

        String credentials = config.getUser() + ":" +
                           (config.getPassword() != null ? config.getPassword() : "");

        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
        return "Basic " + encoded;
    }

    /**
     * Lee la respuesta HTTP
     */
    private String readResponse(HttpURLConnection connection, boolean isError) throws IOException {
        InputStream inputStream = isError ? connection.getErrorStream() : connection.getInputStream();

        if (inputStream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    /**
     * Log del resumen del batch
     */
    private void logBatchSummary(BatchUpdateResult result) {
        TestLogger.logInfo("JIRA_CLIENT", "\n📊 Resumen del envío a Jira:", null);
        TestLogger.logInfo("JIRA_CLIENT", "✅ Exitosos: " + result.getSuccessCount(), null);
        TestLogger.logInfo("JIRA_CLIENT", "❌ Fallidos: " + result.getFailureCount(), null);
        TestLogger.logInfo("JIRA_CLIENT", "📈 Tasa de éxito: " + String.format("%.1f", result.getSuccessRate()) + "%", null);

        if (!result.getFailures().isEmpty()) {
            TestLogger.logWarning("JIRA_CLIENT", "❌ Tests fallidos:", null);
            result.getFailures().forEach((testKey, error) ->
                TestLogger.logWarning("JIRA_CLIENT", "   " + testKey + " - " + error, null));
        }
    }

    /**
     * Clase para el resultado del batch update
     */
    public static class BatchUpdateResult {
        private int successCount = 0;
        private final java.util.Map<String, String> failures = new java.util.HashMap<>();

        public void addSuccess(String testKey) {
            successCount++;
        }

        public void addFailure(String testKey, String errorMessage) {
            failures.put(testKey, errorMessage);
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failures.size();
        }

        public java.util.Map<String, String> getFailures() {
            return failures;
        }

        public double getSuccessRate() {
            int total = successCount + failures.size();
            return total > 0 ? (double) successCount / total * 100 : 0;
        }
    }
}
