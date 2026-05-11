package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.config.BrowserConfigSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Web: Configuracion de Navegador.
 * Fase BDD: GIVEN. Categoria: Configuracion Web.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("web.browser.config")
public class BrowserConfigComponent implements StepComponent {
    @Override
    public String getName() {
        return "Configuracion de Navegador";
    }

    @Override
    public String getDisplayName() {
        return "Configuracion de Navegador";
    }

    @Override
    public String getDescription() {
        return "Configuracion del browser, modo headless, capabilities";
    }

    @Override
    public BddPhase getPhase() {
        return BddPhase.GIVEN;
    }

    @Override
    public String getCategory() {
        return "Configuracion Web";
    }

    @Override
    public String getIcon() {
        return "web";
    }

    @Override
    public int getDisplayOrder() {
        return 10;
    }

    @Override
    public Class<?> getStepDefinitionClass() {
        return BrowserConfigSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "browser", "navegador", "config", "headless", "chrome",
            "firefox", "viewport", "window-size", "capability", "browser-config"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Configuracion de Navegador",
            "en", "Browser Configuration",
            "fr", "Configuration du navigateur"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Configuracion del browser, modo headless, capabilities",
            "en", "Browser setup, headless mode, capabilities",
            "fr", "Configuration du navigateur, mode sans interface, capacites"
        );
    }
}
