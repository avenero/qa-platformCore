package com.qa.webcore.plugin;

import com.qa.common.api.runtime.ApiContextHolder;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.api.runtime.ServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test (FEC-API-SHIP-WEB-SHARE): verifica que {@link WebPlugin} publica el
 * {@code APIRequestContext} del browser en {@link ApiContextHolder} al iniciar un escenario
 * {@code @web} y lo limpia al terminarlo. Es la mitad {@code web-core} del puerto neutral;
 * la mitad {@code http-core} (holder → engine → sesión compartida) la cubre
 * {@code HybridWebApiSessionSharingIT}.
 *
 * <p>Gateado por {@code -Dplaywright.it=true} (lanza un browser real). Sin la property,
 * JUnit lo marca skipped (verde).</p>
 */
@EnabledIfSystemProperty(
        named = "playwright.it",
        matches = "true",
        disabledReason = "Requiere binario nativo de Playwright (browser); habilitar con -Dplaywright.it=true")
@DisplayName("WebPluginSessionHolderIT — WebPlugin publica/limpia la sesión del browser en ApiContextHolder")
class WebPluginSessionHolderIT {

    private final WebPlugin plugin = new WebPlugin();

    @AfterEach
    void tearDown() {
        WebPlugin.resetLifecycleHooksForTesting();
        ApiContextHolder.clear();
        ExecutionContext.deactivate();
    }

    @Test
    @DisplayName("onScenarioStart publica el APIRequestContext del browser; onScenarioEnd lo limpia")
    void onScenarioStartSetsHolderAndEndClears() {
        ExecutionContext context = ExecutionContext.builder()
                .registry(new ServiceRegistry())
                .config(new ExecutionConfig.Builder()
                        .property("playwright.browser", "chromium")
                        .property("web.headless", "true")
                        .build())
                .build();

        assertThat(ApiContextHolder.current()).as("limpio antes del escenario").isNull();

        plugin.onScenarioStart(context);
        try {
            assertThat(ApiContextHolder.current())
                    .as("WebPlugin debe publicar el contexto HTTP del browser")
                    .isNotNull();
        } finally {
            plugin.onScenarioEnd(context);
        }

        assertThat(ApiContextHolder.current())
                .as("onScenarioEnd debe limpiar el holder")
                .isNull();
    }
}
