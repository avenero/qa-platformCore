package com.scotia.qa.common.reporting.manager.pipeline.steps;

import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.reporting.core.config.ReportingConfig;
import com.scotia.qa.common.reporting.core.model.TestExecutionResult;
import com.scotia.qa.common.reporting.jira.service.JiraAttachmentService;
import com.scotia.qa.common.reporting.manager.pipeline.PipelineContext;
import com.scotia.qa.common.reporting.manager.pipeline.ReportingStep;
import com.scotia.qa.common.reporting.manager.pipeline.StepResult;

/**
 * Step 4: Upload de attachments (screenshots y reporte HTML) a Jira.
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class JiraUploadAttachmentsStep implements ReportingStep {

    @Override
    public String getName() {
        return "JiraUploadAttachments";
    }

    @Override
    public boolean isEnabled(ReportingConfig config) {
        return config.getJira().isUploadReport() || config.getJira().isIncludeEvidences();
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        TestLogger.logInfo("JIRA_UPLOAD_STEP", "📎 Subiendo attachments a Jira", null);

        TestExecutionResult result = context.getTestExecutionResult();
        if (result == null) {
            return StepResult.failure("TestExecutionResult no disponible");
        }

        JiraAttachmentService service = null;
        try {
            service = new JiraAttachmentService(context.getConfig().getJira());

            int totalUploaded = 0;

            if (context.getConfig().getJira().isIncludeEvidences()) {
                int uploaded = service.uploadAttachments(result);
                totalUploaded += uploaded;
                TestLogger.logInfo("JIRA_UPLOAD_STEP",
                    String.format("   📸 %d evidencias subidas", uploaded), null);
            }

            if (context.getConfig().getJira().isUploadReport()) {
                String reportPath = context.getExtentReportPath();
                String testExecutionKey = context.getConfig().getJira().getTestExecutionId();

                if (reportPath != null && testExecutionKey != null) {
                    service.uploadReport(testExecutionKey, reportPath);
                    totalUploaded++;
                    TestLogger.logInfo("JIRA_UPLOAD_STEP", "   📄 Reporte HTML subido", null);
                }
            }

            return StepResult.success(
                String.format("%d attachments subidos a Jira", totalUploaded)
            );

        } catch (Exception e) {
            TestLogger.logException("JIRA_UPLOAD_STEP",
                "Error subiendo attachments: " + e.getMessage(), e);
            return StepResult.failure("Error subiendo attachments: " + e.getMessage(), e);
        } finally {
            if (service != null) {
                try {
                    service.close();
                } catch (Exception e) {
                    TestLogger.logWarning("JIRA_UPLOAD_STEP",
                        "Error cerrando JiraAttachmentService: " + e.getMessage(), null);
                }
            }
        }
    }
}

