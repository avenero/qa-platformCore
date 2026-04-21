package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.validation.PageValidationSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.Map;

/**
 * Componente de steps Web: Validacion de Pagina.
 * Fase BDD: THEN. Categoria: Validacion Web.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("web.validation.page")
public class PageValidationComponent implements StepComponent {

    /** Display order for this component in the UI. */
    private static final int DISPLAY_ORDER = 130;

    @Override
    public String getName() {
        return "Validacion de Pagina";
    }

    @Override
    public String getDisplayName() {
        return "Validacion de Pagina";
    }

    @Override
    public String getDescription() {
        return "Titulo, URL, existencia de texto en pagina";
    }

    @Override
    public BddPhase getPhase() {
        return BddPhase.THEN;
    }

    @Override
    public String getCategory() {
        return "Validacion Web";
    }

    @Override
    public String getIcon() {
        return "pageview";
    }

    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }

    @Override
    public Class<?> getStepDefinitionClass() {
        return PageValidationSteps.class;
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Validacion de Pagina",
            "en", "Page Validation",
            "fr", "Validation de la page"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Titulo, URL, existencia de texto en pagina",
            "en", "Title, URL, text existence on page",
            "fr", "Titre, URL, existence de texte sur la page"
        );
    }
}
