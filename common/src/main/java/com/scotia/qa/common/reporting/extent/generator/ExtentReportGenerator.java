package com.scotia.qa.common.reporting.extent.generator;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.reporting.core.config.ExtentConfig;
import com.scotia.qa.common.reporting.core.model.*;

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
 */
public class ExtentReportGenerator {

    private final ExtentConfig config;
    private ExtentReports extent;

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
        // Crear test
        ExtentTest test = extent.createTest(scenario.getScenarioName());

        // Agregar test key como categoría
        if (scenario.getTestKey() != null) {
            test.assignCategory(scenario.getTestKey());
        }

        // Agregar tags
        if (scenario.getTags() != null) {
            scenario.getTags().forEach(tag -> test.assignCategory(tag));
        }

        // Agregar info básica
        test.info("Feature File: " + scenario.getFeatureFile());
        test.info(String.format("Total Steps: %d (Passed: %d, Failed: %d, Skipped: %d)",
            scenario.getTotalSteps(),
            scenario.getPassedSteps(),
            scenario.getFailedSteps(),
            scenario.getSkippedSteps()));

        // Agregar duración
        if (scenario.getDurationMs() > 0) {
            test.info("Duration: " + scenario.getDurationMs() + " ms");
        }

        // Agregar logs
        if (scenario.getLogs() != null) {
            scenario.getLogs().forEach(log -> test.info(log));
        }

        // Agregar screenshots
        if (config.isIncludeScreenshots() && scenario.getScreenshots() != null) {
            for (Attachment screenshot : scenario.getScreenshots()) {
                addScreenshot(test, screenshot);
            }
        }

        // Agregar error si falló
        if (scenario.getStatus() == TestStatus.FAIL) {
            String errorMsg = scenario.getErrorMessage() != null ?
                scenario.getErrorMessage() : "Test failed";

            test.fail(errorMsg);

            if (scenario.getStackTrace() != null) {
                test.fail("<pre>" + scenario.getStackTrace() + "</pre>");
            }
        }

        // Status final
        Status status = mapStatus(scenario.getStatus());
        test.log(status, "Test " + status.toString().toLowerCase());

        TestLogger.logDebug("EXTENT_GENERATOR",
            String.format("Scenario procesado: %s - %s", scenario.getTestKey(), status), null);
    }

    /**
     * Agrega screenshot al test.
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

            // Agregar al reporte
            test.info("<img src='" + screenshotHtml + "' width='800px'/>");

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
}

