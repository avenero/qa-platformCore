package com.scotia.qa.common.jiraXray.model;

/**
 * Información extraída de un scenario de Cucumber
 */
public class ScenarioInfo {
    private final String fileName;
    private final String scenarioName;
    private final String jiraKey;
    private final String formattedContent;

    public ScenarioInfo(String fileName, String scenarioName, String jiraKey, String formattedContent) {
        this.fileName = fileName;
        this.scenarioName = scenarioName;
        this.jiraKey = jiraKey;
        this.formattedContent = formattedContent;
    }

    public String getFileName() {
        return fileName;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public String getJiraKey() {
        return jiraKey;
    }

    public String getFormattedContent() {
        return formattedContent;
    }
}
