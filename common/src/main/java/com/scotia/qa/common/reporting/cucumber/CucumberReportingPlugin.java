package com.scotia.qa.common.reporting.cucumber;

import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.reporting.core.config.ReportingConfig;
import com.scotia.qa.common.reporting.extent.generator.ReportingManager;
import com.scotia.qa.common.reporting.manager.pipeline.PipelineResult;
import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.*;

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
 *             "com.scotia.qa.common.reporting.cucumber.CucumberReportingPlugin"
 * )}
 * </pre>
 *
 * <p><b>Configuración:</b></p>
 * En config-scotia.properties del módulo:
 * <ul>
 *   <li>reporting.enabled=true</li>
 *   <li>jira.updateStatus=true</li>
 *   <li>extent.enabled=true</li>
 * </ul>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class CucumberReportingPlugin implements EventListener {

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        // Escuchar el evento de finalización de todos los tests
        publisher.registerHandlerFor(TestRunFinished.class, this::handleTestRunFinished);
    }

    /**
     * Se ejecuta cuando TODOS los tests han terminado y Cucumber ha escrito todos los reportes.
     *
     * @param event evento de finalización de Cucumber
     */
    private void handleTestRunFinished(TestRunFinished event) {
        System.out.println("═".repeat(80));
        System.out.println("📊 GENERANDO REPORTES POST-EJECUCIÓN");
        System.out.println("═".repeat(80));

        try {
            // Pequeño delay para asegurar que el IO ha terminado
            Thread.sleep(500);

            // 1. Obtener ruta del archivo cucumber.json
            Path moduleDir = Paths.get(System.getProperty("user.dir"));
            Path cucumberJsonPath = moduleDir.resolve("target/cucumber-reports/cucumber.json");

            System.out.println("🔍 Buscando cucumber.json en: " + cucumberJsonPath.toAbsolutePath());

            // 2. Verificar que existe
            if (!Files.exists(cucumberJsonPath)) {
                System.err.println("❌ cucumber.json NO EXISTE");
                return;
            }

            // 3. Leer contenido
            String cucumberJson = Files.readString(cucumberJsonPath);

            if (cucumberJson == null || cucumberJson.trim().isEmpty() || cucumberJson.equals("[]")) {
                System.err.println("❌ cucumber.json está VACÍO o sin scenarios");
                return;
            }

            long fileSize = Files.size(cucumberJsonPath);
            System.out.println("✅ cucumber.json encontrado: " + fileSize + " bytes, " + cucumberJson.length() + " caracteres");

            // 4. Cargar configuración de reporting
            ReportingConfig config = ReportingConfig.fromConfigManager();

            // 5. Inicializar ReportingManager
            ReportingManager.initialize(config);

            // 6. Procesar resultados (genera Extent + actualiza Jira)
            System.out.println("⚙️ Procesando resultados...");
            PipelineResult result = ReportingManager.processTestResults(cucumberJson);

            // 7. Validar resultado
            if (result.isSuccess()) {
                System.out.println("═".repeat(80));
                System.out.println("✅ REPORTING COMPLETADO EXITOSAMENTE");
                System.out.println("📄 Reporte HTML: " + result.getExtentReportPath());
                System.out.println("═".repeat(80));

                TestLogger.logInfo("REPORTING", "Reportes generados exitosamente", Map.of(
                        "extentReport", result.getExtentReportPath() != null ? result.getExtentReportPath() : "N/A"
                ));
            } else {
                System.err.println("═".repeat(80));
                System.err.println("❌ REPORTING FALLÓ");
                System.err.println("Step fallido: " + result.getFailedStep());
                System.err.println("Error: " + result.getErrorMessage());
                System.err.println("═".repeat(80));

                TestLogger.logError("REPORTING", "Error al generar reportes", Map.of(
                        "failedStep", result.getFailedStep(),
                        "error", result.getErrorMessage()
                ));
            }

        } catch (Exception e) {
            System.err.println("❌ Error crítico en reporting: " + e.getMessage());
            e.printStackTrace();

            TestLogger.logError("REPORTING", "Excepción al generar reportes", Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}

