package com.scotia.qa.common.reporting.core.config;

/**
 * Configuración específica para integración con Jira/Xray.
 * Los módulos deben configurar estas propiedades en config-scotia.properties:
 *
 * <pre>
 * # Jira Connection
 * jira.url=https://jira.agile.bns
 * jira.user=${JIRA_USER}
 * jira.password=${JIRA_PASSWORD}
 *
 * # Test Execution
 * jira.projectKey=QAAUY
 * jira.testExecutionId=${TEST_EXECUTION_ID}
 * jira.autoCreateExecution=false
 * jira.testEnvironment=QA
 *
 * # Behavior
 * jira.updateStatus=true
 * jira.uploadReport=true
 * jira.includeEvidences=true
 * jira.maxAttachmentSizeMb=10
 * jira.failOnError=false
 *
 * # Strategy
 * jira.updateMode=BATCH
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class JiraConfig {

    // Conexión
    private String url;
    private String user;
    private String password;

    // Test Execution
    private String projectKey;
    private String testExecutionId;
    private boolean autoCreateExecution;
    private String testEnvironment;

    // Comportamiento
    private boolean updateStatus;
    private boolean uploadReport;
    private boolean includeEvidences;
    private int maxAttachmentSizeMb;
    private boolean failOnError;

    // Estrategia
    private UpdateMode updateMode;

    public JiraConfig() {
        // Defaults
        this.updateMode = UpdateMode.BATCH;
        this.maxAttachmentSizeMb = 10;
        this.failOnError = false;
    }

    /**
     * Modos de actualización de tests en Jira
     */
    public enum UpdateMode {
        /**
         * Actualiza tests uno por uno (más lento pero más robusto)
         */
        SINGLE,

        /**
         * Actualiza todos los tests en un solo request (más rápido)
         */
        BATCH
    }

    // Getters and Setters

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getTestExecutionId() {
        return testExecutionId;
    }

    public void setTestExecutionId(String testExecutionId) {
        this.testExecutionId = testExecutionId;
    }

    public boolean isAutoCreateExecution() {
        return autoCreateExecution;
    }

    public void setAutoCreateExecution(boolean autoCreateExecution) {
        this.autoCreateExecution = autoCreateExecution;
    }

    public String getTestEnvironment() {
        return testEnvironment;
    }

    public void setTestEnvironment(String testEnvironment) {
        this.testEnvironment = testEnvironment;
    }

    public boolean isUpdateStatus() {
        return updateStatus;
    }

    public void setUpdateStatus(boolean updateStatus) {
        this.updateStatus = updateStatus;
    }

    public boolean isUploadReport() {
        return uploadReport;
    }

    public void setUploadReport(boolean uploadReport) {
        this.uploadReport = uploadReport;
    }

    public boolean isIncludeEvidences() {
        return includeEvidences;
    }

    public void setIncludeEvidences(boolean includeEvidences) {
        this.includeEvidences = includeEvidences;
    }

    public int getMaxAttachmentSizeMb() {
        return maxAttachmentSizeMb;
    }

    public void setMaxAttachmentSizeMb(int maxAttachmentSizeMb) {
        this.maxAttachmentSizeMb = maxAttachmentSizeMb;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public UpdateMode getUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(UpdateMode updateMode) {
        this.updateMode = updateMode;
    }

    /**
     * Valida que la configuración sea válida
     */
    public boolean isValid() {
        return url != null && !url.trim().isEmpty()
                && user != null && !user.trim().isEmpty()
                && password != null && !password.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "JiraConfig{" +
                "url='" + url + '\'' +
                ", user='" + user + '\'' +
                ", projectKey='" + projectKey + '\'' +
                ", testExecutionId='" + testExecutionId + '\'' +
                ", updateStatus=" + updateStatus +
                ", uploadReport=" + uploadReport +
                ", updateMode=" + updateMode +
                '}';
    }
}

