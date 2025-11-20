package com.scotia.qa.common.jiraXray.adapter.cucumber;

import com.scotia.qa.common.jiraXray.adapter.ResultAdapter;
import com.scotia.qa.common.jiraXray.extractor.TagBasedExtractor;
import com.scotia.qa.common.jiraXray.model.*;
import com.scotia.qa.common.logging.TestLogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

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
    private final TagBasedExtractor tagExtractor;

    public CucumberResultAdapter() {
        this.objectMapper = new ObjectMapper();
        this.tagExtractor = new TagBasedExtractor();
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

        TestLogger.logInfo("CUCUMBER_ADAPTER", "✅ Procesado scenario: " + testKey + " -> " + scenario.getStatus(), null);
        return scenario;
    }

    private String extractTestKey(JsonNode elementNode) {
        List<String> tagNames = extractTags(elementNode);
        return tagExtractor.extractTestKey(tagNames);
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
