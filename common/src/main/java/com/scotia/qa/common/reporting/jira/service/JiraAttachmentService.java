package com.scotia.qa.common.reporting.jira.service;

import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.reporting.core.config.JiraConfig;
import com.scotia.qa.common.reporting.core.model.Attachment;
import com.scotia.qa.common.reporting.core.model.ScenarioResult;
import com.scotia.qa.common.reporting.core.model.TestExecutionResult;
import com.scotia.qa.common.reporting.jira.client.JiraHttpClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class JiraAttachmentService {

    private final JiraConfig config;
    private final JiraHttpClient httpClient;

    public JiraAttachmentService(JiraConfig config) {
        this.config = config;
        this.httpClient = new JiraHttpClient(config);
    }

    public int uploadAttachments(TestExecutionResult result) throws IOException {
        TestLogger.logInfo("JIRA_ATTACHMENT", "📎 Subiendo attachments a Jira", null);

        int totalUploaded = 0;

        for (ScenarioResult scenario : result.getScenarios()) {
            if (scenario.getTestKey() == null) {
                continue;
            }

            try {
                int uploaded = uploadScenarioAttachments(scenario);
                totalUploaded += uploaded;
            } catch (IOException e) {
                if (config.isFailOnError()) {
                    throw e;
                }
            }
        }

        TestLogger.logInfo("JIRA_ATTACHMENT",
            String.format("✅ %d attachments subidos en total", totalUploaded), null);

        return totalUploaded;
    }

    public void uploadReport(String testExecutionKey, String reportPath) throws IOException {
        if (testExecutionKey == null || reportPath == null) {
            return;
        }

        File reportFile = new File(reportPath);
        if (!reportFile.exists()) {
            TestLogger.logWarning("JIRA_ATTACHMENT",
                "⚠️  Reporte no encontrado: " + reportPath, null);
            return;
        }

        TestLogger.logInfo("JIRA_ATTACHMENT",
            String.format("📄 Subiendo reporte HTML a %s", testExecutionKey), null);

        httpClient.postAttachment(testExecutionKey, reportFile);

        TestLogger.logInfo("JIRA_ATTACHMENT", "✅ Reporte subido exitosamente", null);
    }

    private int uploadScenarioAttachments(ScenarioResult scenario) throws IOException {
        List<File> filesToUpload = new ArrayList<>();

        if (config.isIncludeEvidences() && scenario.getScreenshots() != null) {
            for (Attachment screenshot : scenario.getScreenshots()) {
                File file = prepareAttachment(screenshot);
                if (file != null && shouldUpload(file)) {
                    filesToUpload.add(file);
                }
            }
        }

        if (filesToUpload.isEmpty()) {
            return 0;
        }

        for (File file : filesToUpload) {
            httpClient.postAttachment(scenario.getTestKey(), file);
        }

        return filesToUpload.size();
    }

    private File prepareAttachment(Attachment attachment) {
        if (attachment.getPath() != null) {
            File file = new File(attachment.getPath());
            if (file.exists()) {
                return file;
            }
        }

        if (attachment.getContent() != null) {
            try {
                File tempFile = File.createTempFile("jira_attachment_", "_" + attachment.getName());
                Files.write(tempFile.toPath(), attachment.getContent());
                tempFile.deleteOnExit();
                return tempFile;
            } catch (IOException e) {
                TestLogger.logWarning("JIRA_ATTACHMENT",
                    "⚠️  No se pudo crear archivo temporal: " + e.getMessage(), null);
            }
        }

        return null;
    }

    private boolean shouldUpload(File file) {
        long maxSizeBytes = config.getMaxAttachmentSizeMb() * 1024L * 1024L;
        if (file.length() > maxSizeBytes) {
            TestLogger.logWarning("JIRA_ATTACHMENT",
                String.format("⚠️  Archivo muy grande, omitiendo: %s (%.2f MB > %d MB)",
                    file.getName(),
                    file.length() / 1024.0 / 1024.0,
                    config.getMaxAttachmentSizeMb()), null);
            return false;
        }

        return true;
    }

    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}

