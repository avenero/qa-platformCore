package com.qa.common.reporting.core.adapter.cucumber;

import com.qa.common.logging.TestLogger;
import com.qa.common.reporting.core.adapter.ResultAdapter;
import com.qa.common.reporting.core.model.Attachment;
import com.qa.common.reporting.core.model.ScenarioResult;
import com.qa.common.reporting.core.model.StepResult;
import com.qa.common.reporting.core.model.TestExecutionResult;
import com.qa.common.reporting.core.model.TestStatus;
import com.qa.common.reporting.core.util.EvidenceCollector;
import com.qa.common.reporting.core.util.TagExtractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador para resultados de Cucumber en formato JSON estándar.
 * Extrae solo la información relevante para reportar a Jira.
 * Integrado con el sistema de logging del QA Automation Framework.
 *
 * @author QA Automation Framework Team
 * @since 1.0.0
 */
public class CucumberResultAdapter implements ResultAdapter {

    /** Maximum characters for a business-facing error message (Jira limit). */
    private static final int MAX_BUSINESS_ERROR_LENGTH = 300;

    private final ObjectMapper objectMapper;
    private final boolean collectEvidences;

    /**
     * Constructor por defecto con recolección de evidencias habilitada.
     */
    public CucumberResultAdapter() {
        this(true); // Por defecto, recolectar evidencias
    }

    /**
     * Constructor con control explícito de recolección de evidencias.
     *
     * @param collectEvidences {@code true} para recolectar evidencias del filesystem
     */
    public CucumberResultAdapter(boolean collectEvidences) {
        this.objectMapper = new ObjectMapper();
        this.collectEvidences = collectEvidences;
    }

    @Override
    public TestExecutionResult convert(String rawResults) {
        TestLogger.logInfo("CUCUMBER_ADAPTER", "🥒 Procesando resultados de Cucumber", null);

        try {
            JsonNode rootNode = objectMapper.readTree(rawResults);

            TestExecutionResult result = new TestExecutionResult();
            result.setExecutionStart(LocalDateTime.now());
            result.setExecutionEnd(LocalDateTime.now());
            result.setSummary("Test Execution - " + LocalDateTime.now());

            List<ScenarioResult> scenarios = new ArrayList<>();

            // Procesar cada feature
            if (rootNode.isArray()) {
                for (JsonNode featureNode : rootNode) {
                    scenarios.addAll(processFeature(featureNode));
                }
            }

            result.setScenarios(scenarios);

            TestLogger.logInfo("CUCUMBER_ADAPTER",
                "✅ Procesados " + scenarios.size() + " scenarios de Cucumber", null);
            return result;

        } catch (Exception e) {
            TestLogger.logException("CUCUMBER_ADAPTER", "❌ Error procesando resultados de Cucumber", e);
            throw new RuntimeException("Error al convertir resultados de Cucumber", e);
        }
    }

    private List<ScenarioResult> processFeature(JsonNode featureNode) {
        List<ScenarioResult> scenarios = new ArrayList<>();

        String featureName = featureNode.path("name").asText("Unknown Feature");
        String featureUri = featureNode.path("uri").asText("unknown.feature");

        JsonNode elementsNode = featureNode.path("elements");
        if (elementsNode.isArray()) {
            for (JsonNode elementNode : elementsNode) {
                ScenarioResult scenario = processScenario(elementNode, featureName, featureUri);
                if (scenario != null) {
                    scenarios.add(scenario);
                }
            }
        }

        return scenarios;
    }

    private ScenarioResult processScenario(JsonNode elementNode, String featureName, String featureUri) {
        String type = elementNode.path("type").asText();

        // Solo procesar scenarios, ignorar hooks y otros elementos
        if (!"scenario".equals(type)) {
            return null;
        }

        ScenarioResult scenario = new ScenarioResult();

        // Información básica
        scenario.setScenarioName(elementNode.path("name").asText("Unknown Scenario"));
        scenario.setFeatureFile(featureUri);

        // Extraer test key de tags
        String testKey = extractTestKey(elementNode);
        if (testKey == null) {
            TestLogger.logInfo("CUCUMBER_ADAPTER",
                "⏭️ Scenario '" + scenario.getScenarioName() + "' sin test key válido, omitiendo", null);
            return null;
        }
        scenario.setTestKey(testKey);

        // Procesar tags
        List<String> tags = extractTags(elementNode);
        scenario.setTags(tags);

        // Determinar si es outline
        boolean isOutline = isScenarioOutline(elementNode);
        scenario.setOutline(isOutline);

        // Procesar steps y determinar estado general
        processSteps(elementNode, scenario);

        // Calcular tiempos
        calculateTiming(elementNode, scenario);

        // ✨ NUEVO: Extraer embeddings/attachments (screenshots) del JSON
        extractEmbeddings(elementNode, scenario);

        // NUEVO: Recolectar evidencias si está habilitado
        if (collectEvidences) {
            collectScenarioEvidences(scenario, featureName);
        }

        TestLogger.logInfo("CUCUMBER_ADAPTER",
            "✅ Procesado scenario: " + testKey + " -> " + scenario.getStatus(), null);
        return scenario;
    }

    /**
     * Recolecta evidencias capturadas por EvidenceManager para este scenario.
     * Asume que EvidenceManager fue usado durante la ejecución del test.
     */
    private void collectScenarioEvidences(ScenarioResult scenario, String featureName) {
        try {
            // Determinar framework desde tags (si tiene @web, @api, @mobile)
            String framework = detectFrameworkFromTags(scenario.getTags());

            // Obtener directorio de evidencias
            String evidenceDir = EvidenceCollector.getScenarioEvidenceDirectory(
                framework,
                featureName,
                scenario.getScenarioName()
            );

            // Recolectar screenshots del filesystem
            List<Attachment> screenshots = EvidenceCollector.collectScreenshotsFromDirectory(evidenceDir);
            if (!screenshots.isEmpty()) {
                // ✅ AGREGAR a la lista existente (no reemplazar embeddings)
                for (Attachment screenshot : screenshots) {
                    scenario.addScreenshot(screenshot);
                }
                TestLogger.logInfo("CUCUMBER_ADAPTER",
                    String.format("📸 %d screenshots del filesystem adjuntados a: %s",
                        screenshots.size(), scenario.getTestKey()),
                    null);
            }

            // Recolectar otras evidencias (API, errors) como logs
            List<Attachment> apiEvidences = EvidenceCollector.collectEvidencesByPrefix(
                evidenceDir, "api_response_", Attachment.AttachmentType.JSON
            );
            List<Attachment> errorEvidences = EvidenceCollector.collectEvidencesByPrefix(
                evidenceDir, "error_", Attachment.AttachmentType.JSON
            );

            // Convertir evidencias a logs (paths)
            List<String> logs = new ArrayList<>();
            apiEvidences.forEach(att -> logs.add("API: " + att.getPath()));
            errorEvidences.forEach(att -> logs.add("ERROR: " + att.getPath()));

            if (!logs.isEmpty()) {
                scenario.setLogs(logs);
                TestLogger.logDebug("CUCUMBER_ADAPTER",
                    String.format("📎 %d logs adjuntados a: %s", logs.size(), scenario.getTestKey()),
                    null);
            }

        } catch (Exception e) {
            // No fallar si no se pueden recolectar evidencias
            TestLogger.logWarning("CUCUMBER_ADAPTER",
                "⚠️ No se pudieron recolectar evidencias para: " + scenario.getScenarioName() +
                " - " + e.getMessage(), null);
        }
    }

    /**
     * Detecta el framework desde los tags del scenario.
     */
    private String detectFrameworkFromTags(List<String> tags) {
        if (tags == null) {
            return "COMMON";
        }

        for (String tag : tags) {
            String tagLower = tag.toLowerCase();
            if (tagLower.contains("web")) {
                return "WEB";
            }
            if (tagLower.contains("api")) {
                return "API";
            }
            if (tagLower.contains("mobile")) {
                return "MOBILE";
            }
        }

        return "COMMON";
    }

    private String extractTestKey(JsonNode elementNode) {
        List<String> tagNames = extractTags(elementNode);
        return TagExtractor.extractTestKey(tagNames);
    }

    private List<String> extractTags(JsonNode elementNode) {
        List<String> tags = new ArrayList<>();

        JsonNode tagsNode = elementNode.path("tags");
        if (tagsNode.isArray()) {
            for (JsonNode tagNode : tagsNode) {
                String tagName = tagNode.path("name").asText();
                if (tagName != null && !tagName.isEmpty()) {
                    tags.add(tagName);
                }
            }
        }

        return tags;
    }

    private boolean isScenarioOutline(JsonNode elementNode) {
        String keyword = elementNode.path("keyword").asText("");
        return "Scenario Outline".equals(keyword) || "Scenario Template".equals(keyword);
    }

    private void processSteps(JsonNode elementNode, ScenarioResult scenario) {
        JsonNode stepsNode = elementNode.path("steps");

        int totalSteps = 0;
        int passedSteps = 0;
        int failedSteps = 0;
        int skippedSteps = 0;

        TestStatus overallStatus = TestStatus.PASS;
        String errorMessage = null;

        List<StepResult> stepResults = new ArrayList<>();

        if (stepsNode.isArray()) {
            totalSteps = stepsNode.size();

            for (JsonNode stepNode : stepsNode) {
                // Crear StepResult para este step
                StepResult stepResult = parseStep(stepNode);
                stepResults.add(stepResult);

                // Actualizar contadores
                JsonNode resultNode = stepNode.path("result");
                if (!resultNode.isMissingNode()) {
                    String stepStatus = resultNode.path("status").asText("undefined");

                    switch (stepStatus) {
                        case "passed" -> passedSteps++;
                        case "failed" -> {
                            failedSteps++;
                            overallStatus = TestStatus.FAIL;
                            if (errorMessage == null) {
                                errorMessage = resultNode.path("error_message").asText(null);
                            }
                        }
                        case "skipped" -> {
                            skippedSteps++;
                            if (overallStatus == TestStatus.PASS) {
                                overallStatus = TestStatus.SKIP;
                            }
                        }
                        default -> {
                            skippedSteps++;
                            if (overallStatus == TestStatus.PASS) {
                                overallStatus = TestStatus.TODO;
                            }
                        }
                    }
                }
            }
        }

        scenario.setTotalSteps(totalSteps);
        scenario.setPassedSteps(passedSteps);
        scenario.setFailedSteps(failedSteps);
        scenario.setSkippedSteps(skippedSteps);
        scenario.setStatus(overallStatus);
        scenario.setSteps(stepResults);  // ← NUEVO: Asignar lista de steps detallados

        if (errorMessage != null) {
            scenario.setErrorMessage(truncateMessage(errorMessage, 500));
            scenario.setBusinessMessage(cleanErrorForBusiness(errorMessage));
        }
    }

    /**
     * Parsea un step individual del JSON de Cucumber y crea un StepResult.
     */
    private StepResult parseStep(JsonNode stepNode) {
        StepResult stepResult = new StepResult();

        // Keyword (Given, When, Then, And, But)
        stepResult.setKeyword(stepNode.path("keyword").asText("").trim());

        // Name (texto del step)
        stepResult.setName(stepNode.path("name").asText(""));

        // Line number
        stepResult.setLine(stepNode.path("line").asInt(0));

        // Result
        JsonNode resultNode = stepNode.path("result");
        if (!resultNode.isMissingNode()) {
            String status = resultNode.path("status").asText("undefined");
            stepResult.setStatus(mapCucumberStatus(status));

            // Duration (en nanosegundos en Cucumber JSON)
            long durationNanos = resultNode.path("duration").asLong(0);
            stepResult.setDurationNanos(durationNanos);

            // Error message si falló
            String errorMsg = resultNode.path("error_message").asText(null);
            if (errorMsg != null) {
                stepResult.setErrorMessage(errorMsg);
            }
        }

        // Location (clase.método que ejecuta el step)
        JsonNode matchNode = stepNode.path("match");
        if (!matchNode.isMissingNode()) {
            stepResult.setLocation(matchNode.path("location").asText(null));
        }

        return stepResult;
    }

    /**
     * Mapea status de Cucumber a TestStatus del framework.
     */
    private TestStatus mapCucumberStatus(String cucumberStatus) {
        return switch (cucumberStatus) {
            case "passed" -> TestStatus.PASS;
            case "failed" -> TestStatus.FAIL;
            case "skipped" -> TestStatus.SKIP;
            case "pending" -> TestStatus.TODO;
            default -> TestStatus.TODO;
        };
    }

    /**
     * Extrae embeddings/attachments (screenshots) del cucumber.json.
     * Cucumber guarda los screenshots como embeddings en base64.
     */
    private void extractEmbeddings(JsonNode elementNode, ScenarioResult scenario) {
        // Buscar embeddings en "after" hooks (donde se capturan los screenshots)
        JsonNode afterNode = elementNode.path("after");
        if (afterNode.isArray()) {
            for (JsonNode hookNode : afterNode) {
                extractEmbeddingsFromNode(hookNode.path("embeddings"), scenario, false);
            }
        }

        // También buscar en los steps individuales (si se adjuntaron ahí)
        JsonNode stepsNode = elementNode.path("steps");
        if (stepsNode.isArray()) {
            for (JsonNode stepNode : stepsNode) {
                extractEmbeddingsFromNode(stepNode.path("embeddings"), scenario, true);
            }
        }
    }

    /**
     * Extrae embeddings de imagen de un nodo JSON y los agrega al scenario.
     *
     * @param embeddingsNode nodo JSON que contiene la lista de embeddings
     * @param scenario       scenario al que se agregan los screenshots
     * @param fromStep       {@code true} si los embeddings provienen de un step individual
     */
    private void extractEmbeddingsFromNode(JsonNode embeddingsNode,
                                           ScenarioResult scenario,
                                           boolean fromStep) {
        if (!embeddingsNode.isArray()) {
            return;
        }
        for (JsonNode embeddingNode : embeddingsNode) {
            String mimeType = embeddingNode.path("mime_type").asText();
            String data = embeddingNode.path("data").asText();
            String name = embeddingNode.path("name").asText("screenshot");

            if (mimeType != null && mimeType.startsWith("image/") && data != null && !data.isEmpty()) {
                decodeAndAddScreenshot(scenario, mimeType, data, name, fromStep);
            }
        }
    }

    /**
     * Decodifica un embedding base64 y lo agrega como screenshot al scenario.
     *
     * @param scenario  scenario destino
     * @param mimeType  tipo MIME de la imagen
     * @param data      datos en base64
     * @param name      nombre del attachment
     * @param fromStep  {@code true} si el screenshot proviene de un step
     */
    private void decodeAndAddScreenshot(ScenarioResult scenario,
                                        String mimeType,
                                        String data,
                                        String name,
                                        boolean fromStep) {
        try {
            byte[] imageBytes = java.util.Base64.getDecoder().decode(data);

            Attachment attachment = new Attachment();
            attachment.setName(name);
            attachment.setMimeType(mimeType);
            attachment.setType(Attachment.AttachmentType.SCREENSHOT);
            attachment.setContent(imageBytes);

            scenario.addScreenshot(attachment);

            String source = fromStep ? "del step: " : ": ";
            TestLogger.logDebug("CUCUMBER_ADAPTER",
                "📸 Screenshot extraído" + source + name + " (" + imageBytes.length + " bytes)", null);
        } catch (IllegalArgumentException e) {
            String source = fromStep ? " base64 del step: " : " base64: ";
            TestLogger.logWarning("CUCUMBER_ADAPTER",
                "⚠️ Error decodificando screenshot" + source + name, null);
        }
    }

    /**
     * Limpia el mensaje de error técnico para hacerlo amigable a equipos de negocio.
     *
     * Elimina:
     * - Nombres de excepciones Java (FrameworkBusinessException, AssertionError, etc.)
     * - Nombres de métodos (camelCase seguido de :)
     * - Stack traces (todo después de "at ")
     * - Response JSON completo (todo después de "Response:")
     *
     * Ejemplos:
     * Input:  "FrameworkBusinessException: validoQueElCodigo: Error validando código:
     *          Expected 201 but was 200. Response: {...} at ApiSteps.java:123"
     * Output: "Error validando código: Expected 201 but was 200"
     *
     * @param technicalError mensaje de error completo de Cucumber
     * @return mensaje limpio y amigable para PO/PM
     */
    private String cleanErrorForBusiness(String technicalError) {
        if (technicalError == null || technicalError.trim().isEmpty()) {
            return null;
        }

        String clean = technicalError;

        // 1. Eliminar clase de excepción Java (cualquier cosa que termine en Exception:, Error:, etc.)
        clean = clean.replaceFirst("^[\\w\\.]+(?:Exception|Error|Throwable):\\s*", "");

        // 2. Eliminar nombre de método (camelCase seguido de : y espacio)
        // Detecta patrones como: validoQueElCodigo:, executeQuery:, etc.
        clean = clean.replaceFirst("^[a-z][a-zA-Z0-9]*:\\s*", "");

        // 3. Repetir paso 2 por si hay métodos anidados (método1: método2: error)
        clean = clean.replaceFirst("^[a-z][a-zA-Z0-9]*:\\s*", "");

        // 4. Eliminar stack trace (todo después de " at " seguido de clase Java)
        int atIndex = clean.indexOf(" at ");
        if (atIndex > 0) {
            // Verificar que sea un stack trace real (contiene .java: o similar)
            String afterAt = clean.substring(atIndex);
            if (afterAt.contains(".java:") || afterAt.contains(".groovy:") || afterAt.contains(".kt:")) {
                clean = clean.substring(0, atIndex);
            }
        }

        // 5. Truncar response JSON si existe (todo después de "Response:")
        int responseIndex = clean.indexOf("Response:");
        if (responseIndex > 0) {
            clean = clean.substring(0, responseIndex).trim();
        }

        // 6. Truncar request JSON si existe (todo después de "Request:")
        int requestIndex = clean.indexOf("Request:");
        if (requestIndex > 0) {
            clean = clean.substring(0, requestIndex).trim();
        }

        // 7. Limpiar espacios múltiples y saltos de línea
        clean = clean.replaceAll("\\s+", " ").trim();

        // 8. Si el mensaje quedó vacío o muy corto, devolver el original truncado
        if (clean.length() < 10) {
            return truncateMessage(technicalError, 200);
        }

        // 9. Truncar para Jira (límite razonable)
        return truncateMessage(clean, MAX_BUSINESS_ERROR_LENGTH);
    }

    private void calculateTiming(JsonNode elementNode, ScenarioResult scenario) {
        JsonNode stepsNode = elementNode.path("steps");

        if (!stepsNode.isArray() || stepsNode.size() == 0) {
            return;
        }

        long totalDurationNs = 0;
        LocalDateTime startTime = LocalDateTime.now(); // Aproximado

        for (JsonNode stepNode : stepsNode) {
            JsonNode resultNode = stepNode.path("result");
            if (!resultNode.isMissingNode()) {
                long duration = resultNode.path("duration").asLong(0);
                totalDurationNs += duration;
            }
        }

        Duration totalDuration = Duration.ofNanos(totalDurationNs);
        scenario.setStartTime(startTime.minus(totalDuration));
        scenario.setEndTime(startTime);
        scenario.setDuration(totalDuration);
    }

    private String truncateMessage(String message, int maxLength) {
        if (message == null || message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...";
    }

    @Override
    public boolean canHandle(String rawResults) {
        try {
            JsonNode rootNode = objectMapper.readTree(rawResults);

            // Verificar si es un array de features de Cucumber
            if (rootNode.isArray() && rootNode.size() > 0) {
                JsonNode firstElement = rootNode.get(0);
                return firstElement.has("elements") || firstElement.has("name");
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getName() {
        return "CucumberResultAdapter";
    }
}
