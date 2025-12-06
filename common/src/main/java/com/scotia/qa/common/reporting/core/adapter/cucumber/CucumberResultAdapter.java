package com.scotia.qa.common.reporting.core.adapter.cucumber;

import com.scotia.qa.common.reporting.core.adapter.ResultAdapter;
import com.scotia.qa.common.reporting.core.model.*;
import com.scotia.qa.common.reporting.core.util.EvidenceCollector;
import com.scotia.qa.common.reporting.core.util.TagExtractor;
import com.scotia.qa.common.logging.TestLogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;

/**
 * Adaptador para resultados de Cucumber en formato JSON estándar.
 * Extrae solo la información relevante para reportar a Jira.
 * Integrado con el sistema de logging del framework Scotia QA.
 *
 * @author Scotia QA Framework Team
 * @since 1.0.0
 */
public class CucumberResultAdapter implements ResultAdapter {


    private final ObjectMapper objectMapper;
    private final boolean collectEvidences;

    public CucumberResultAdapter() {
        this(true); // Por defecto, recolectar evidencias
    }

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

            TestLogger.logInfo("CUCUMBER_ADAPTER", "✅ Procesados " + scenarios.size() + " scenarios de Cucumber", null);
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
            TestLogger.logInfo("CUCUMBER_ADAPTER", "⏭️ Scenario '" + scenario.getScenarioName() + "' sin test key válido, omitiendo", null);
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

        // NUEVO: Recolectar evidencias si está habilitado
        if (collectEvidences) {
            collectScenarioEvidences(scenario, featureName);
        }

        TestLogger.logInfo("CUCUMBER_ADAPTER", "✅ Procesado scenario: " + testKey + " -> " + scenario.getStatus(), null);
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

            // Recolectar screenshots
            List<Attachment> screenshots = EvidenceCollector.collectScreenshotsFromDirectory(evidenceDir);
            if (!screenshots.isEmpty()) {
                scenario.setScreenshots(screenshots);
                TestLogger.logInfo("CUCUMBER_ADAPTER",
                    String.format("📸 %d screenshots adjuntados a: %s", screenshots.size(), scenario.getTestKey()),
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
            if (tagLower.contains("web")) return "WEB";
            if (tagLower.contains("api")) return "API";
            if (tagLower.contains("mobile")) return "MOBILE";
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

        if (stepsNode.isArray()) {
            totalSteps = stepsNode.size();

            for (JsonNode stepNode : stepsNode) {
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

        if (errorMessage != null) {
            scenario.setErrorMessage(truncateMessage(errorMessage, 500));
        }
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
