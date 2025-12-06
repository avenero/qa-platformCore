package com.scotia.qa.common.reporting.manager.pipeline.steps;

import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.reporting.core.config.ReportingConfig;
import com.scotia.qa.common.reporting.core.model.TestExecutionResult;
import com.scotia.qa.common.reporting.jira.service.JiraUpdateService;
import com.scotia.qa.common.reporting.manager.pipeline.PipelineContext;
import com.scotia.qa.common.reporting.manager.pipeline.ReportingStep;
import com.scotia.qa.common.reporting.manager.pipeline.StepResult;

/**
 * Step 3: Actualización de status en Jira/Xray.
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class JiraUpdateStatusStep implements ReportingStep {

    @Override
    public String getName() {
        return "JiraUpdateStatus";
    }

    @Override
    public boolean isEnabled(ReportingConfig config) {
        return config.getJira().isUpdateStatus();
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public StepResult execute(PipelineContext context) {
        TestLogger.logInfo("JIRA_UPDATE_STEP", "📤 Actualizando status en Jira", null);

        TestExecutionResult result = context.getTestExecutionResult();
        if (result == null) {
            return StepResult.failure("TestExecutionResult no disponible");
        }

        JiraUpdateService service = null;
        try {
            service = new JiraUpdateService(context.getConfig().getJira());

            int updated = service.updateTestStatus(result);

            return StepResult.success(
                String.format("%d tests actualizados en Jira", updated)
            );

        } catch (Exception e) {
            TestLogger.logException("JIRA_UPDATE_STEP",
                "Error actualizando Jira: " + e.getMessage(), e);
            return StepResult.failure("Error actualizando Jira: " + e.getMessage(), e);
        } finally {
            if (service != null) {
                try {
                    service.close();
                } catch (Exception e) {
                    TestLogger.logWarning("JIRA_UPDATE_STEP",
                        "Error cerrando JiraUpdateService: " + e.getMessage(), null);
                }
            }
        }
    }
}

