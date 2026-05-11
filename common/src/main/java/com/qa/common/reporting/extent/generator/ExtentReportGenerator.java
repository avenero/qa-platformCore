package com.qa.common.reporting.extent.generator;
import com.qa.common.utils.security.SecurityUtilities;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.qa.common.api.logging.TestLogger;
import com.qa.common.reporting.core.config.ExtentConfig;
import com.qa.common.reporting.core.model.Attachment;
import com.qa.common.reporting.core.model.EnvironmentInfo;
import com.qa.common.reporting.core.model.ScenarioResult;
import com.qa.common.reporting.core.model.StepResult;
import com.qa.common.reporting.core.model.TestExecutionResult;
import com.qa.common.reporting.core.model.TestStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Generador de reportes Extent (HTML).
 * Convierte TestExecutionResult a reporte HTML visual.
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 * @deprecated since 2.0.0 — use {@link com.qa.common.reporting.core.service.ExtentReportGeneratorImpl}
 *             with {@link com.qa.common.reporting.core.port.ReportGeneratorPort} and bridge models
 *             from {@code com.qa.common.reporting.core.bridge} instead.
 *             This class will be removed in a future version.
 */
@Deprecated(since = "2.0.0", forRemoval = false)
public class ExtentReportGenerator {

    private final ExtentConfig config;
    private ExtentReports extent;

    /**
     * Crea un generador con la configuración dada.
     *
     * @param config configuración del reporte Extent, no null
     */
    public ExtentReportGenerator(ExtentConfig config) {
        this.config = config;
    }

    /**
     * Genera el reporte Extent a partir de TestExecutionResult.
     *
     * @param result resultados de ejecución
     * @return path del reporte HTML generado
     */
    public String generate(TestExecutionResult result) throws IOException {
        TestLogger.logInfo("EXTENT_GENERATOR", "📊 Generando reporte Extent Reports", null);

        // Inicializar Extent
        initializeExtent();

        // Agregar system info
        if (config.isIncludeSystemInfo() && result.getEnvironmentInfo() != null) {
            addSystemInfo(result.getEnvironmentInfo());
        }

        // Procesar cada scenario
        int scenariosProcessed = 0;
        for (ScenarioResult scenario : result.getScenarios()) {
            processScenario(scenario);
            scenariosProcessed++;
        }

        // Flush report
        extent.flush();

        TestLogger.logInfo("EXTENT_GENERATOR",
            String.format("✅ Reporte generado: %d scenarios procesados", scenariosProcessed), null);
        TestLogger.logInfo("EXTENT_GENERATOR",
            String.format("   Output: %s", config.getFullReportPath()), null);

        return config.getFullReportPath();
    }

    /**
     * Inicializa ExtentReports con configuración.
     */
    private void initializeExtent() throws IOException {
        // Crear directorio de output si no existe
        File outputDir = new File(config.getOutputPath());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Configurar reporter
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(config.getFullReportPath());

        // Configurar UI
        sparkReporter.config().setDocumentTitle(config.getDocumentTitle());
        sparkReporter.config().setReportName(config.getReportTitle());

        // Tema
        if ("DARK".equalsIgnoreCase(config.getTheme())) {
            sparkReporter.config().setTheme(Theme.DARK);
        } else {
            sparkReporter.config().setTheme(Theme.STANDARD);
        }

        // Timeline
        if (config.isIncludeTimeline()) {
            sparkReporter.config().setTimelineEnabled(true);
        }

        // Crear ExtentReports
        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);

        TestLogger.logDebug("EXTENT_GENERATOR", "ExtentReports inicializado", null);
    }

    /**
     * Agrega información del sistema al reporte.
     */
    private void addSystemInfo(EnvironmentInfo envInfo) {
        extent.setSystemInfo("OS", envInfo.getOs());
        extent.setSystemInfo("OS Version", envInfo.getOsVersion());
        extent.setSystemInfo("Java Version", envInfo.getJavaVersion());

        if (envInfo.getBrowser() != null) {
            extent.setSystemInfo("Browser", envInfo.getBrowser());
            if (envInfo.getBrowserVersion() != null) {
                extent.setSystemInfo("Browser Version", envInfo.getBrowserVersion());
            }
        }

        if (envInfo.getDevice() != null) {
            extent.setSystemInfo("Device", envInfo.getDevice());
            extent.setSystemInfo("Platform", envInfo.getPlatform());
        }

        // Custom info
        if (envInfo.getCustomInfo() != null) {
            envInfo.getCustomInfo().forEach((key, value) ->
                extent.setSystemInfo(key, value)
            );
        }

        // System info custom del config
        if (config.getSystemInfo() != null) {
            config.getSystemInfo().forEach((key, value) ->
                extent.setSystemInfo(key, value)
            );
        }

        TestLogger.logDebug("EXTENT_GENERATOR", "System info agregada", null);
    }

    /**
     * Procesa un scenario y lo agrega al reporte.
     */
    private void processScenario(ScenarioResult scenario) {
        ExtentTest test = extent.createTest(scenario.getScenarioName());
        assignCategoriesAndTags(test, scenario);

        test.info("<span style='font-size: 14px;'><b>📁 Feature File:</b> "
            + scenario.getFeatureFile() + "</span>");

        addStepsToTest(test, scenario);
        addDurationAndLogs(test, scenario);
        addScreenshotsToTest(test, scenario);
        addFailureDetailsToTest(test, scenario);

        Status status = mapStatus(scenario.getStatus());
        test.log(status, "Test " + status.toString().toLowerCase());

        TestLogger.logDebug("EXTENT_GENERATOR",
            String.format("Scenario procesado: %s - %s", scenario.getTestKey(), status), null);
    }

    /**
     * Asigna la test key y los tags como categorías al test, evitando duplicados.
     */
    private void assignCategoriesAndTags(ExtentTest test, ScenarioResult scenario) {
        String testKey = scenario.getTestKey();
        if (testKey != null) {
            test.assignCategory(testKey);
        }
        if (scenario.getTags() != null) {
            scenario.getTags().forEach(tag -> {
                if (testKey != null && (tag.equals(testKey) || tag.equals("@" + testKey))) {
                    return;
                }
                test.assignCategory(tag);
            });
        }
    }

    /**
     * Agrega los steps detallados al test, o un resumen si no hay steps disponibles.
     */
    private void addStepsToTest(ExtentTest test, ScenarioResult scenario) {
        if (scenario.getSteps() != null && !scenario.getSteps().isEmpty()) {
            test.info(String.format(
                "<span style='font-size: 14px;'><b>📋 Steps Executed:</b> %d total "
                + "<span style='color: #4CAF50;'>(Passed: %d)</span> "
                + "<span style='color: #f44336;'>(Failed: %d)</span> "
                + "<span style='color: #ff9800;'>(Skipped: %d)</span></span>",
                scenario.getTotalSteps(), scenario.getPassedSteps(),
                scenario.getFailedSteps(), scenario.getSkippedSteps()));

            for (StepResult step : scenario.getSteps()) {
                logStepResult(test, step);
            }
        } else {
            test.info(String.format(
                "<span style='font-size: 14px;'>Total Steps: %d "
                + "(Passed: %d, Failed: %d, Skipped: %d)</span>",
                scenario.getTotalSteps(), scenario.getPassedSteps(),
                scenario.getFailedSteps(), scenario.getSkippedSteps()));
        }
    }

    /**
     * Registra un step individual en el test con el status correspondiente.
     */
    private void logStepResult(ExtentTest test, StepResult step) {
        String emoji = step.getStatus() == TestStatus.PASS ? "" : step.getStatusEmoji() + " ";
        String stepText = String.format(
            "<span style='font-size: 14px;'>%s<span style='font-weight: 500;'>%s</span> "
            + "<span style='color: #aaa; font-size: 13px;'>(%s)</span></span>",
            emoji,
            escapeHtml(step.getFullStepText()),
            step.getFormattedDuration());

        switch (step.getStatus()) {
            case PASS -> test.pass(stepText);
            case FAIL -> test.fail(stepText);
            case SKIP -> test.skip(stepText);
            default -> test.info(stepText);
        }
    }

    /**
     * Agrega la duración total y los logs de ejecución al test.
     */
    private void addDurationAndLogs(ExtentTest test, ScenarioResult scenario) {
        if (scenario.getDurationMs() > 0) {
            test.info("<span style='font-size: 14px;'><b>⏱️ Duration:</b> "
                + scenario.getDurationMs() + " ms</span>");
        }
        if (scenario.getLogs() != null) {
            scenario.getLogs().forEach(log -> test.info(log));
        }
    }

    /**
     * Agrega los screenshots al test si la configuración lo permite.
     */
    private void addScreenshotsToTest(ExtentTest test, ScenarioResult scenario) {
        if (config.isIncludeScreenshots() && scenario.getScreenshots() != null) {
            for (Attachment screenshot : scenario.getScreenshots()) {
                addScreenshot(test, screenshot);
            }
        }
    }

    /**
     * Agrega los detalles de error al test cuando el scenario falló.
     */
    private void addFailureDetailsToTest(ExtentTest test, ScenarioResult scenario) {
        if (scenario.getStatus() != TestStatus.FAIL) {
            return;
        }
        String displayError = scenario.getBusinessMessage() != null
                && !scenario.getBusinessMessage().isEmpty()
                ? scenario.getBusinessMessage()
                : (scenario.getErrorMessage() != null ? scenario.getErrorMessage() : "Test failed");

        test.fail("<span style='font-size: 14px;'><b>Error:</b> "
            + escapeHtml(displayError) + "</span>");

        if (scenario.getBusinessMessage() != null && scenario.getErrorMessage() != null) {
            test.fail("<details style='font-size: 13px; margin-top: 8px;'>"
                    + "<summary style='cursor: pointer; color: #aaa;'>"
                    + "<i>Ver detalles técnicos</i></summary>"
                    + "<pre style='background: #1e1e1e; padding: 12px; border-radius: 4px;"
                    + " color: #d4d4d4; font-size: 12px; overflow-x: auto;'>"
                    + escapeHtml(scenario.getErrorMessage()) + "</pre></details>");
        }

        if (scenario.getStackTrace() != null) {
            test.fail("<details style='font-size: 13px; margin-top: 8px;'>"
                    + "<summary style='cursor: pointer; color: #aaa;'>"
                    + "<i>Stack Trace</i></summary>"
                    + "<pre style='background: #1e1e1e; padding: 12px; border-radius: 4px;"
                    + " color: #d4d4d4; font-size: 12px; overflow-x: auto;'>"
                    + escapeHtml(scenario.getStackTrace()) + "</pre></details>");
        }
    }

    /**
     * Agrega screenshot al test.
     * Si el scenario falló, usa test.fail() y hace el screenshot colapsable.
     */
    private void addScreenshot(ExtentTest test, Attachment screenshot) {
        try {
            String screenshotHtml;

            // Si tiene content, embeber en base64
            if (screenshot.getContent() != null) {
                String base64 = Base64.getEncoder().encodeToString(screenshot.getContent());
                screenshotHtml = "data:image/png;base64," + base64;
            }
            // Si no, usar path relativo
            else if (screenshot.getPath() != null) {
                File screenshotFile = new File(screenshot.getPath());
                if (screenshotFile.exists()) {
                    byte[] fileContent = Files.readAllBytes(Paths.get(screenshot.getPath()));
                    String base64 = Base64.getEncoder().encodeToString(fileContent);
                    screenshotHtml = "data:image/png;base64," + base64;
                } else {
                    TestLogger.logWarning("EXTENT_GENERATOR",
                        "⚠️  Screenshot no encontrado: " + screenshot.getPath(), null);
                    return;
                }
            } else {
                return;
            }

            // ✨ MEJORADO: Screenshot colapsable y con status FAIL
            String screenshotName = screenshot.getName() != null ? screenshot.getName() : "Screenshot";
            String collapsibleScreenshot =
                "<details style='margin-top: 8px;'>" +
                "<summary style='cursor: pointer; font-size: 14px;'>" +
                "<b>📸 " + escapeHtml(screenshotName) + "</b>"
                + " <i style='color: #aaa; font-size: 12px;'>(click para expandir)</i>" +
                "</summary>" +
                "<div style='margin-top: 8px;'>" +
                "<img src='" + screenshotHtml + "'"
                + " style='max-width: 100%; border: 1px solid #444; border-radius: 4px;'/>" +
                "</div>" +
                "</details>";

            // ✅ Usar test.fail() para que aparezca con badge rojo "Fail"
            test.fail(collapsibleScreenshot);

            TestLogger.logDebug("EXTENT_GENERATOR",
                "Screenshot agregado: " + screenshot.getName(), null);

        } catch (Exception e) {
            TestLogger.logWarning("EXTENT_GENERATOR",
                "⚠️  Error agregando screenshot: " + e.getMessage(), null);
        }
    }

    /**
     * Mapea TestStatus a Extent Status.
     */
    private Status mapStatus(TestStatus testStatus) {
        switch (testStatus) {
            case PASS:
                return Status.PASS;
            case FAIL:
                return Status.FAIL;
            case SKIP:
                return Status.SKIP;
            case TODO:
            case EXECUTING:
                return Status.WARNING;
            default:
                return Status.INFO;
        }
    }

    /**
     * Escapa caracteres HTML para evitar problemas de renderizado.
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").
                   replace("'", "&#39;");
    }
}

