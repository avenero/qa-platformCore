package com.qa.webcore.steps.config;

import com.qa.common.runtime.ExecutionContext;
import com.qa.webcore.config.WebConfigKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BrowserConfigSteps")
class BrowserConfigStepsTest {

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    @Test
    @DisplayName("Guarda browser/headless en VariableStore del contexto")
    void guardaConfiguracionEnVariableStore() throws Exception {
        ExecutionContext ctx = ExecutionContext.builder().build();
        ctx.activate();

        BrowserConfigSteps steps = new BrowserConfigSteps();
        steps.configurarDriverDelNavegador("chrome", "true");

        assertThat(ctx.variables().get(WebConfigKeys.BROWSER_RUNTIME_VAR, String.class))
            .contains("chromium");
        assertThat(ctx.variables().get(WebConfigKeys.HEADLESS_RUNTIME_VAR, Boolean.class))
            .contains(true);
    }
}
