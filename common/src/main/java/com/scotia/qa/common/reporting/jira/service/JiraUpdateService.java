package com.scotia.qa.common.reporting.jira.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.reporting.core.config.JiraConfig;
import com.scotia.qa.common.reporting.core.model.ScenarioResult;
import com.scotia.qa.common.reporting.core.model.TestExecutionResult;
import com.scotia.qa.common.reporting.core.model.TestStatus;
import com.scotia.qa.common.reporting.jira.client.JiraHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JiraUpdateService {

    private final JiraConfig config;
    private final JiraHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public JiraUpdateService(JiraConfig config) {
        this.config = config;
        this.httpClient = new JiraHttpClient(config);
        this.objectMapper = new ObjectMapper();
    }

    public int updateTestStatus(TestExecutionResult result) throws IOException {
        TestLogger.logInfo("JIRA_UPDATE",
            String.format("📤 Actualizando status en Jira (modo: %s)", config.getUpdateMode()), null);

        if (config.getUpdateMode() == JiraConfig.UpdateMode.BATCH) {
            return updateBatch(result);
        } else {
            return updateSingle(result);
        }
    }

    private int updateBatch(TestExecutionResult result) throws IOException {
        TestLogger.logInfo("JIRA_UPDATE", "🔄 Modo BATCH: enviando todos los tests juntos", null);

        Map<String, Object> payload = new HashMap<>();

        Map<String, Object> info = new HashMap<>();
        info.put("project", config.getProjectKey());
        info.put("summary", result.getSummary() != null ? result.getSummary() : "Test Execution");
        info.put("testEnvironments", List.of(config.getTestEnvironment()));
        payload.put("info", info);

        List<Map<String, Object>> tests = new ArrayList<>();
        for (ScenarioResult scenario : result.getScenarios()) {
            if (scenario.getTestKey() == null) {
                continue;
            }

            Map<String, Object> test = new HashMap<>();
            test.put("testKey", scenario.getTestKey());
            test.put("status", mapStatus(scenario.getStatus()));

            if (scenario.getErrorMessage() != null) {
                test.put("comment", scenario.getErrorMessage());
            }

            tests.add(test);
        }
        payload.put("tests", tests);

        String endpoint = "/rest/raven/2.0/import/execution";
        String jsonPayload = objectMapper.writeValueAsString(payload);

        httpClient.post(endpoint, jsonPayload);

        TestLogger.logInfo("JIRA_UPDATE",
            String.format("✅ %d tests actualizados en batch", tests.size()), null);

        return tests.size();
    }

    private int updateSingle(TestExecutionResult result) throws IOException {
        TestLogger.logInfo("JIRA_UPDATE", "🔄 Modo SINGLE: actualizando tests uno por uno", null);

        int updated = 0;
        for (ScenarioResult scenario : result.getScenarios()) {
            if (scenario.getTestKey() == null) {
                continue;
            }

            try {
                updateSingleTest(scenario);
                updated++;
            } catch (IOException e) {
                if (config.isFailOnError()) {
                    throw e;
                }
            }
        }

        TestLogger.logInfo("JIRA_UPDATE",
            String.format("✅ %d/%d tests actualizados", updated, result.getScenarios().size()), null);

        return updated;
    }

    private void updateSingleTest(ScenarioResult scenario) throws IOException {
        String endpoint = String.format("/rest/api/2/issue/%s/transitions", scenario.getTestKey());

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> transition = new HashMap<>();
        transition.put("id", getTransitionId(scenario.getStatus()));
        payload.put("transition", transition);

        String jsonPayload = objectMapper.writeValueAsString(payload);

        httpClient.post(endpoint, jsonPayload);
    }

    private String mapStatus(TestStatus status) {
        switch (status) {
            case PASS: return "PASS";
            case FAIL: return "FAIL";
            case SKIP: return "ABORTED";
            case TODO: return "TODO";
            case EXECUTING: return "EXECUTING";
            default: return "TODO";
        }
    }

    private String getTransitionId(TestStatus status) {
        switch (status) {
            case PASS: return "31";
            case FAIL: return "41";
            default: return "11";
        }
    }

    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}

