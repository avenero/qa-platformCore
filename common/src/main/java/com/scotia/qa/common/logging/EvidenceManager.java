package com.scotia.qa.common.logging;

import com.scotia.qa.common.http.exceptions.FrameworkTechnicalException;
import com.scotia.qa.common.utils.DataUtilities;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor centralizado de evidencias para todos los frameworks de testing.
 * Maneja capturas de pantalla, respuestas de API, logs de test y otros artefactos.
 *
 * @author Abel Venero
 * @since 1.0.0
 */
public class EvidenceManager {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");

    // Configuración de directorios
    private static String baseEvidenceDir = "test-evidences";
    private static final Map<String, String> currentTestPaths = new ConcurrentHashMap<>();

    // Contexto del test actual
    private static final ThreadLocal<String> currentFeature = new ThreadLocal<>();
    private static final ThreadLocal<String> currentScenario = new ThreadLocal<>();
    private static final ThreadLocal<String> currentFramework = new ThreadLocal<>();

    private EvidenceManager() {
        // Utility class - no instances
    }

    // =================================================================================
    // CONFIGURACIÓN
    // =================================================================================

    /**
     * Configura el directorio base para evidencias.
     */
    public static void setBaseEvidenceDirectory(String directory) {
        baseEvidenceDir = directory;
        TestLogger.logInfo("EVIDENCE_CONFIG", "Evidence base directory set to: " + directory, null);
    }

    /**
     * Establece el contexto del test actual.
     */
    public static void setTestContext(String framework, String feature, String scenario) {
        currentFramework.set(framework);
        currentFeature.set(feature);
        currentScenario.set(scenario);

        // Crear directorio para este test
        String testPath = createTestDirectory(framework, feature, scenario);
        currentTestPaths.put(Thread.currentThread().getName(), testPath);

        TestLogger.logInfo("EVIDENCE_CONTEXT",
                          String.format("Test context set - Framework: %s, Feature: %s, Scenario: %s",
                                       framework, feature, scenario), null);
    }

    /**
     * Limpia el contexto del test actual.
     */
    public static void clearTestContext() {
        currentTestPaths.remove(Thread.currentThread().getName());
        currentFramework.remove();
        currentFeature.remove();
        currentScenario.remove();
    }

    // =================================================================================
    // CAPTURA DE EVIDENCIAS
    // =================================================================================

    /**
     * Guarda una captura de pantalla.
     */
    public static String saveScreenshot(byte[] screenshotData, String description) throws FrameworkTechnicalException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String filename = String.format("screenshot_%s_%s.png",
                                       sanitizeFilename(description), timestamp);

        String filepath = getEvidenceFilePath(filename);

        try {
            DataUtilities.writeStringToFile(new String(screenshotData), filepath);
            TestLogger.logInfo("EVIDENCE_SCREENSHOT",
                              String.format("Screenshot saved: %s - %s", description, filepath), null);
            return filepath;
        } catch (Exception e) {
            throw new FrameworkTechnicalException("saveScreenshot",
                    "Error saving screenshot: " + e.getMessage());
        }
    }

    /**
     * Guarda una respuesta de API.
     */
    public static String saveApiResponse(String method, String endpoint, int statusCode,
                                       String requestBody, String responseBody) throws FrameworkTechnicalException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String filename = String.format("api_response_%s_%s_%d_%s.json",
                                       method, sanitizeFilename(endpoint), statusCode, timestamp);

        String filepath = getEvidenceFilePath(filename);

        Map<String, Object> apiEvidence = Map.of(
            "timestamp", timestamp,
            "method", method,
            "endpoint", endpoint,
            "statusCode", statusCode,
            "requestBody", requestBody != null ? requestBody : "",
            "responseBody", responseBody != null ? responseBody : ""
        );

        try {
            String jsonContent = convertToJson(apiEvidence);
            DataUtilities.writeStringToFile(jsonContent, filepath);
            TestLogger.logInfo("EVIDENCE_API",
                              String.format("API response saved: %s %s - %s", method, endpoint, filepath), null);
            return filepath;
        } catch (Exception e) {
            throw new FrameworkTechnicalException("saveApiResponse",
                    "Error saving API response: " + e.getMessage());
        }
    }

    /**
     * Guarda logs de UI interactions.
     */
    public static String saveUiInteraction(String action, String element, String value,
                                         boolean success) throws FrameworkTechnicalException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String filename = String.format("ui_interaction_%s_%s_%s.json",
                                       sanitizeFilename(action), sanitizeFilename(element), timestamp);

        String filepath = getEvidenceFilePath(filename);

        Map<String, Object> uiEvidence = Map.of(
            "timestamp", timestamp,
            "action", action,
            "element", element,
            "value", value != null ? value : "",
            "success", success
        );

        try {
            String jsonContent = convertToJson(uiEvidence);
            DataUtilities.writeStringToFile(jsonContent, filepath);
            TestLogger.logInfo("EVIDENCE_UI",
                              String.format("UI interaction saved: %s on %s - %s", action, element, filepath), null);
            return filepath;
        } catch (Exception e) {
            throw new FrameworkTechnicalException("saveUiInteraction",
                    "Error saving UI interaction: " + e.getMessage());
        }
    }

    /**
     * Guarda información de error/excepción.
     */
    public static String saveErrorEvidence(String errorType, String message,
                                         String stackTrace) throws FrameworkTechnicalException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String filename = String.format("error_%s_%s.json",
                                       sanitizeFilename(errorType), timestamp);

        String filepath = getEvidenceFilePath(filename);

        Map<String, Object> errorEvidence = Map.of(
            "timestamp", timestamp,
            "errorType", errorType,
            "message", message,
            "stackTrace", stackTrace != null ? stackTrace : "",
            "framework", currentFramework.get() != null ? currentFramework.get() : "UNKNOWN",
            "feature", currentFeature.get() != null ? currentFeature.get() : "UNKNOWN",
            "scenario", currentScenario.get() != null ? currentScenario.get() : "UNKNOWN"
        );

        try {
            String jsonContent = convertToJson(errorEvidence);
            DataUtilities.writeStringToFile(jsonContent, filepath);
            TestLogger.logError("EVIDENCE_ERROR",
                               String.format("Error evidence saved: %s - %s", errorType, filepath), null);
            return filepath;
        } catch (Exception e) {
            throw new FrameworkTechnicalException("saveErrorEvidence",
                    "Error saving error evidence: " + e.getMessage());
        }
    }

    /**
     * Guarda evidencia personalizada.
     */
    public static String saveCustomEvidence(String evidenceType, String content,
                                          String fileExtension) throws FrameworkTechnicalException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String filename = String.format("%s_%s.%s",
                                       sanitizeFilename(evidenceType), timestamp, fileExtension);

        String filepath = getEvidenceFilePath(filename);

        try {
            DataUtilities.writeStringToFile(content, filepath);
            TestLogger.logInfo("EVIDENCE_CUSTOM",
                              String.format("Custom evidence saved: %s - %s", evidenceType, filepath), null);
            return filepath;
        } catch (Exception e) {
            throw new FrameworkTechnicalException("saveCustomEvidence",
                    "Error saving custom evidence: " + e.getMessage());
        }
    }

    // =================================================================================
    // MÉTODOS PRIVADOS
    // =================================================================================

    /**
     * Crea el directorio para el test actual.
     */
    private static String createTestDirectory(String framework, String feature, String scenario) {
        String date = LocalDateTime.now().format(DATE_FORMAT);
        Path testPath = Paths.get(baseEvidenceDir, date, framework,
                                sanitizeFilename(feature), sanitizeFilename(scenario));

        try {
            DataUtilities.createDirectoryIfNotExists(testPath.toString());
            return testPath.toString();
        } catch (IOException | RuntimeException e) {
            TestLogger.logError("EVIDENCE_ERROR",
                               String.format("Error creating test directory: %s", e.getMessage()), null);
            return baseEvidenceDir; // Fallback al directorio base
        }
    }

    /**
     * Obtiene la ruta completa para un archivo de evidencia.
     */
    private static String getEvidenceFilePath(String filename) {
        String testPath = currentTestPaths.get(Thread.currentThread().getName());
        if (testPath == null) {
            // Fallback si no hay contexto establecido
            String date = LocalDateTime.now().format(DATE_FORMAT);
            testPath = Paths.get(baseEvidenceDir, date, "unknown").toString();
        }

        return Paths.get(testPath, filename).toString();
    }

    /**
     * Sanitiza nombres de archivo.
     */
    private static String sanitizeFilename(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "unknown";
        }

        return name.replaceAll("[^a-zA-Z0-9._-]", "_")
                  .replaceAll("_{2,}", "_")
                  .toLowerCase();
    }

    /**
     * Convierte un mapa a JSON simple.
     */
    private static String convertToJson(Map<String, Object> data) {
        StringBuilder json = new StringBuilder("{\n");

        data.forEach((key, value) -> {
            json.append("  \"").append(key).append("\": ");

            if (value instanceof String) {
                json.append("\"").append(((String) value).replace("\"", "\\\"")).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value.toString());
            } else {
                json.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
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
