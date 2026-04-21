package com.qa.common.reporting.cucumber;

import com.qa.common.config.ConfigManager;
import com.qa.common.logging.TestLogger;
import com.qa.common.reporting.core.config.ReportingConfig;
import com.qa.common.reporting.extent.generator.ReportingManager;
import com.qa.common.reporting.manager.pipeline.PipelineResult;
import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestRunFinished;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Plugin de Cucumber que genera reportes DESPUÉS de que Cucumber termine completamente.
 *
 * <p>Este plugin se ejecuta después del flush del JSON, garantizando que el archivo
 * cucumber.json esté completamente escrito antes de procesarlo.</p>
 *
 * <p><b>Uso en módulos:</b></p>
 * <pre>
 * {@code @ConfigurationParameter(
 *     key = PLUGIN_PROPERTY_NAME,
 *     value = "json:target/cucumber-reports/cucumber.json, " +
 *             "html:target/cucumber-reports/cucumber.html, " +
 *             "pretty, " +
 *             "com.qa.common.reporting.cucumber.CucumberReportingPlugin"
 * )}
 * </pre>
 *
 * <p><b>Configuración:</b></p>
 * En {@code config-app.properties} del módulo:
 * <ul>
 *   <li>reporting.enabled=true</li>
 *   <li>extent.enabled=true</li>
 *   <li>reporting.cucumber.json.path=target/cucumber-reports/cucumber.json (opcional)</li>
 * </ul>
 *
 * @author Abel Venero
 * @version 2.0.0
 * @since 1.0.0
 */
public class CucumberReportingPlugin implements EventListener {

    /** Path por defecto del JSON de Cucumber. Configurable via reporting.cucumber.json.path. */
    private static final String DEFAULT_JSON_PATH = "target/cucumber-reports/cucumber.json";


    /** Reintentos máximos esperando a que el archivo JSON esté disponible. */
    private static final int MAX_IO_WAIT_ATTEMPTS = 10;

    /** Milisegundos base por intento de backoff progresivo. */
    private static final long BACKOFF_BASE_MS = 100L;

    /** Number of separator characters for log section headers. */
    private static final int LOG_SEPARATOR_LENGTH = 60;

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunFinished.class, this::handleTestRunFinished);
    }

    /**
     * Se ejecuta cuando TODOS los tests han terminado y Cucumber ha escrito todos los reportes.
     *
     * @param event evento de finalización de Cucumber
     */
    private void handleTestRunFinished(TestRunFinished event) {
        TestLogger.logInfo("REPORTING",
            "═".repeat(LOG_SEPARATOR_LENGTH) + " GENERANDO REPORTES POST-EJECUCIÓN "
                + "═".repeat(LOG_SEPARATOR_LENGTH), null);

        try {
            ReportingConfig config = ReportingConfig.fromConfigManager();

            // Determinar path del JSON (configurable via reporting.cucumber.json.path en config-app.properties)
            String jsonRelPath = ConfigManager.getInstance().get(
                    "reporting.cucumber.json.path", DEFAULT_JSON_PATH);
            Path moduleDir = Paths.get(System.getProperty("user.dir"));
            Path cucumberJsonPath = moduleDir.resolve(jsonRelPath);

            TestLogger.logInfo("REPORTING", "Buscando cucumber.json",
                    Map.of("path", cucumberJsonPath.toAbsolutePath().toString()));

            // Esperar con backoff progresivo (en vez de Thread.sleep fijo de 500ms)
            if (!waitForFile(cucumberJsonPath)) {
                TestLogger.logError("REPORTING", "cucumber.json no encontrado después de reintentos",
                        Map.of("path", cucumberJsonPath.toAbsolutePath().toString()));
                return;
            }

            String cucumberJson = Files.readString(cucumberJsonPath);

            if (cucumberJson.isBlank() || cucumberJson.equals("[]")) {
                TestLogger.logError("REPORTING", "cucumber.json está vacío o sin scenarios",
                        Map.of("path", cucumberJsonPath.toAbsolutePath().toString()));
                return;
            }

            long fileSize = Files.size(cucumberJsonPath);
            TestLogger.logInfo("REPORTING", "cucumber.json leído correctamente",
                    Map.of("bytes", fileSize, "chars", cucumberJson.length()));

            // Inicializar y procesar
            ReportingManager.initialize(config);
            TestLogger.logInfo("REPORTING", "Procesando resultados...", null);
            PipelineResult result = ReportingManager.processTestResults(cucumberJson);

            if (result.isSuccess()) {
                TestLogger.logInfo("REPORTING", "Reportes generados exitosamente",
                        Map.of("extentReport", result.getExtentReportPath() != null
                                ? result.getExtentReportPath() : "N/A"));
            } else {
                TestLogger.logError("REPORTING", "Error al generar reportes",
                        Map.of("failedStep", result.getFailedStep(),
                                "error", result.getErrorMessage()));
            }

        } catch (Exception e) {
            TestLogger.logError("REPORTING", "Excepción crítica al generar reportes",
                    Map.of("error", e.getMessage(),
                            "type", e.getClass().getSimpleName()));
        }
    }

    /**
     * Espera a que el archivo exista y tenga contenido usando backoff progresivo.
     * Evita el {@code Thread.sleep(500)} fijo que puede fallar en CI/CD lentos.
     *
     * @param path ruta del archivo a esperar
     * @return {@code true} si el archivo existe y tiene contenido, {@code false} si se agotaron los reintentos
     */
    private boolean waitForFile(Path path) throws InterruptedException {
        for (int attempt = 1; attempt <= MAX_IO_WAIT_ATTEMPTS; attempt++) {
            try {
                if (Files.exists(path) && Files.size(path) > 2) {
                    return true;
                }
            } catch (Exception ignored) {
                // archivo aún no disponible o no accesible
            }
            long waitMs = BACKOFF_BASE_MS * attempt; // backoff: 100, 200, 300, ... 1000ms
            TestLogger.logDebug("REPORTING",
                    String.format("Esperando archivo (intento %d/%d, %dms)...", attempt, MAX_IO_WAIT_ATTEMPTS, waitMs),
                    null);
            Thread.sleep(waitMs);
        }
        return Files.exists(path);
    }
}
