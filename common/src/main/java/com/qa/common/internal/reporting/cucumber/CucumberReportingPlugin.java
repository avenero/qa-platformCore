package com.qa.common.internal.reporting.cucumber;
import com.qa.common.api.Internal;

import com.qa.common.api.config.ConfigLoaderHolder;
import com.qa.common.api.logging.TestLogger;
import com.qa.common.internal.reporting.config.ReportingConfig;
import com.qa.common.internal.reporting.extent.ReportingManager;
import com.qa.common.internal.reporting.manager.pipeline.PipelineResult;
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
 *             "com.qa.common.internal.reporting.cucumber.CucumberReportingPlugin"
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
@Internal

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
     * <p>El flujo es:
     * <ol>
     *   <li>Verifica que reporting esté habilitado ({@code reporting.enabled=true}).</li>
     *   <li>Espera con backoff progresivo a que {@code cucumber.json} exista y tenga contenido.</li>
     *   <li>Verifica que el JSON no esté vacío.</li>
     *   <li>Procesa el pipeline: ConversionStep → ExtentGenerationStep.</li>
     * </ol>
     *
     * <p><b>Fallo explícito:</b> cualquier condición que impida la generación
     * produce un {@code logError} con el motivo concreto — no hay fallos silenciosos.
     *
     * @param event evento de finalización de Cucumber
     */
    private void handleTestRunFinished(TestRunFinished event) {
        TestLogger.logInfo("REPORTING",
            "═".repeat(LOG_SEPARATOR_LENGTH) + " GENERANDO REPORTES POST-EJECUCIÓN "
                + "═".repeat(LOG_SEPARATOR_LENGTH), null);

        try {
            ReportingConfig config = ReportingConfig.load();

            // Guard #1 — Reporting deshabilitado explícitamente en configuración
            if (!config.isEnabled()) {
                TestLogger.logInfo("REPORTING",
                    "Reporting deshabilitado (reporting.enabled=false) — se omite generación de reporte.",
                    null);
                return;
            }

            // Determinar path del JSON (configurable via reporting.cucumber.json.path en config-app.properties)
            String jsonRelPath = ConfigLoaderHolder.get().getRaw(
                    "reporting.cucumberJsonPath", DEFAULT_JSON_PATH);
            Path moduleDir = Paths.get(System.getProperty("user.dir"));
            Path cucumberJsonPath = moduleDir.resolve(jsonRelPath);

            TestLogger.logInfo("REPORTING", "Buscando cucumber.json",
                    Map.of("path", cucumberJsonPath.toAbsolutePath().toString()));

            // Guard #2 — cucumber.json no disponible tras los reintentos
            if (!waitForFile(cucumberJsonPath)) {
                TestLogger.logError("REPORTING",
                    "cucumber.json no encontrado después de " + MAX_IO_WAIT_ATTEMPTS + " intentos. "
                        + "Verifica que el runner tenga 'json:target/cucumber-reports/cucumber.json' "
                        + "en sus plugins de Cucumber. Reporte HTML no generado.",
                    Map.of("path", cucumberJsonPath.toAbsolutePath().toString(),
                           "intentos", MAX_IO_WAIT_ATTEMPTS,
                           "accion", "Agrega 'json:<path>' a @ConfigurationParameter(key=PLUGIN_PROPERTY_NAME)"));
                return;
            }

            String cucumberJson = Files.readString(cucumberJsonPath);

            // Guard #3 — JSON vacío o sin escenarios
            if (cucumberJson.isBlank() || cucumberJson.equals("[]")) {
                TestLogger.logError("REPORTING",
                    "cucumber.json está vacío o no contiene escenarios. "
                        + "¿Se ejecutaron features válidas? Reporte HTML no generado.",
                    Map.of("path", cucumberJsonPath.toAbsolutePath().toString()));
                return;
            }

            long fileSize = Files.size(cucumberJsonPath);
            TestLogger.logInfo("REPORTING", "cucumber.json leído correctamente",
                    Map.of("bytes", fileSize, "chars", cucumberJson.length()));

            // Inicializar y procesar pipeline
            ReportingManager.initialize(config);
            TestLogger.logInfo("REPORTING", "Procesando resultados...", null);
            PipelineResult result = ReportingManager.processTestResults(cucumberJson);

            if (result.isSuccess()) {
                TestLogger.logInfo("REPORTING", "Reportes generados exitosamente",
                        Map.of("extentReport", result.getExtentReportPath() != null
                                ? result.getExtentReportPath() : "N/A"));
            } else {
                TestLogger.logError("REPORTING", "Error al generar reportes",
                        Map.of("failedStep", result.getFailedStep() != null ? result.getFailedStep() : "unknown",
                                "error", result.getErrorMessage() != null ? result.getErrorMessage() : "sin detalle"));
            }

        } catch (Exception e) {
            TestLogger.logError("REPORTING", "Excepción crítica al generar reportes",
                    Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getName(),
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
