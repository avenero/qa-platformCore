package com.scotia.qa.common.jiraXray.service;

import com.scotia.qa.common.jiraXray.config.ReportConfig;
import com.scotia.qa.common.jiraXray.client.JiraClient;
import com.scotia.qa.common.jiraXray.adapter.ResultAdapter;
import com.scotia.qa.common.jiraXray.adapter.cucumber.CucumberResultAdapter;
import com.scotia.qa.common.jiraXray.adapter.junit.JUnitResultAdapter;
import com.scotia.qa.common.jiraXray.model.TestExecutionResult;
import com.scotia.qa.common.jiraXray.model.ScenarioResult;
import com.scotia.qa.common.logging.TestLogger;

import java.util.Arrays;
import java.util.List;

/**
 * Servicio principal para procesar y enviar resultados de tests a Jira.
 * Usa adaptadores para diferentes formatos y envía en batches.
 * Integrado con el sistema de logging del framework Scotia QA.
 *
 * @author Abel Venero
 * @since 1.0.0
 */
public class JiraTestCaseUpdaterService {


    private final ReportConfig config;
    private final JiraClient jiraClient;
    private final List<ResultAdapter> adapters;

    public JiraTestCaseUpdaterService(ReportConfig config) {
        this.config = config;
        this.jiraClient = new JiraClient(config);

        // Inicializar adaptadores disponibles
        this.adapters = Arrays.asList(
            new CucumberResultAdapter(),
            new JUnitResultAdapter()
        );
    }

    /**
     * Procesa resultados de tests y los envía a Jira
     *
     * @param rawResults resultados en formato nativo (JSON, XML, etc.)
     */
    public void processAndSendResults(String rawResults) {
        TestLogger.logInfo("JIRA_SERVICE", "🚀 Procesando resultados de tests para envío a Jira", null);

        // Verificar si se debe subir o no
        if (!Boolean.TRUE.equals(config.getUploadReport())) {
            TestLogger.logInfo("JIRA_SERVICE", "⏭️ Upload deshabilitado en configuración, omitiendo envío", null);
            return;
        }

        // Encontrar adaptador apropiado
        ResultAdapter adapter = findAdapter(rawResults);
        if (adapter == null) {
            TestLogger.logException("JIRA_SERVICE", "❌ No se encontró adaptador compatible para los resultados proporcionados", new RuntimeException("No adapter found"));
            return;
        }

        TestLogger.logInfo("JIRA_SERVICE", "🔧 Usando adaptador: " + adapter.getName(), null);

        try {
            // Convertir resultados al modelo estándar
            TestExecutionResult testResults = adapter.convert(rawResults);

            // Validar que tenemos resultados válidos
            if (testResults.getScenarios() == null || testResults.getScenarios().isEmpty()) {
                TestLogger.logWarning("JIRA_SERVICE", "⚠️ No se encontraron scenarios válidos en los resultados", null);
                return;
            }

            // Filtrar solo scenarios con test keys válidos
            List<ScenarioResult> validScenarios = testResults.getScenarios().stream()
                .filter(scenario -> scenario.getTestKey() != null && !scenario.getTestKey().trim().isEmpty())
                .toList();

            if (validScenarios.isEmpty()) {
                TestLogger.logWarning("JIRA_SERVICE", "⚠️ No se encontraron scenarios con test keys válidos", null);
                return;
            }

            testResults.setScenarios(validScenarios);

            // Configurar test execution key
            if (testResults.getTestExecutionKey() == null) {
                testResults.setTestExecutionKey(config.getTestExecution());
            }

            TestLogger.logInfo("JIRA_SERVICE", "📊 Procesados " + validScenarios.size() + " scenarios válidos de " + testResults.getScenarios().size() + " totales", null);

            // Enviar a Jira en batch
            JiraClient.BatchUpdateResult result = jiraClient.sendTestResults(testResults);

            // Mostrar resumen final
            showFinalSummary(testResults, result);

        } catch (Exception e) {
            TestLogger.logException("JIRA_SERVICE", "❌ Error procesando y enviando resultados", e);
            // No re-lanzar la excepción para no fallar la ejecución de tests
        }
    }

    /**
     * Encuentra el adaptador apropiado para el formato de resultados
     */
    private ResultAdapter findAdapter(String rawResults) {
        for (ResultAdapter adapter : adapters) {
            if (adapter.canHandle(rawResults)) {
                return adapter;
            }
        }
        return null;
    }

    /**
     * Muestra resumen final de la operación
     */
    private void showFinalSummary(TestExecutionResult testResults, JiraClient.BatchUpdateResult batchResult) {
        TestLogger.logInfo("JIRA_SERVICE", "\n" + "=".repeat(80), null);
        TestLogger.logInfo("JIRA_SERVICE", "📋 RESUMEN FINAL DE ENVÍO A JIRA", null);
        TestLogger.logInfo("JIRA_SERVICE", "=".repeat(80), null);

        // Estadísticas de ejecución
        if (testResults.getStatistics() != null) {
            var stats = testResults.getStatistics();
            TestLogger.logInfo("JIRA_SERVICE", "📊 Estadísticas de Ejecución:", null);
            TestLogger.logInfo("JIRA_SERVICE", "   Total tests: " + stats.getTotal(), null);
            TestLogger.logInfo("JIRA_SERVICE", "   ✅ Pasados: " + stats.getPassed(), null);
            TestLogger.logInfo("JIRA_SERVICE", "   ❌ Fallidos: " + stats.getFailed(), null);
            TestLogger.logInfo("JIRA_SERVICE", "   ⏭️ Omitidos: " + stats.getSkipped(), null);
            TestLogger.logInfo("JIRA_SERVICE", "   📈 Tasa de éxito: " + String.format("%.1f", stats.getSuccessRate()) + "%", null);

            if (stats.getTotalDurationMs() > 0) {
                TestLogger.logInfo("JIRA_SERVICE", "   ⏱️ Duración total: " + stats.getTotalDurationMs() + "ms", null);
            }
        }

        TestLogger.logInfo("JIRA_SERVICE", "", null);

        // Estadísticas de envío
        TestLogger.logInfo("JIRA_SERVICE", "📤 Estadísticas de Envío:", null);
        TestLogger.logInfo("JIRA_SERVICE", "   ✅ Enviados exitosamente: " + batchResult.getSuccessCount(), null);
        TestLogger.logInfo("JIRA_SERVICE", "   ❌ Fallos de envío: " + batchResult.getFailureCount(), null);
        TestLogger.logInfo("JIRA_SERVICE", "   📈 Tasa de envío exitoso: " + String.format("%.1f", batchResult.getSuccessRate()) + "%", null);

        TestLogger.logInfo("JIRA_SERVICE", "=".repeat(80), null);

        // Detalles de scenarios procesados (siempre mostrar)
        TestLogger.logInfo("JIRA_SERVICE", "\n📋 Detalle de scenarios procesados:", null);
        for (ScenarioResult scenario : testResults.getScenarios()) {
            TestLogger.logInfo("JIRA_SERVICE", "   " + scenario.getTestKey() + " -> " + scenario.getScenarioName() + " (" + scenario.getStatus() + ")", null);
        }
    }

    /**
     * Método deprecated para compatibilidad con código anterior
     */
    @Deprecated
    public void updateTestCasesFromFeatures(String tagCode, String projectCode) {
        TestLogger.logWarning("JIRA_SERVICE", "⚠️ Método updateTestCasesFromFeatures está deprecated. " +
                "Usar processAndSendResults(String) en su lugar", null);

        // Crear resultado vacío para mantener compatibilidad
        TestExecutionResult emptyResult = new TestExecutionResult();
        emptyResult.setTestExecutionKey(config.getTestExecution());
        emptyResult.setScenarios(java.util.Collections.emptyList());

        TestLogger.logInfo("JIRA_SERVICE", "⏭️ No se procesaron resultados con el método deprecated", null);
    }
}
