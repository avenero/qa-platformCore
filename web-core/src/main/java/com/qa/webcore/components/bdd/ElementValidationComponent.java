package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.validation.ElementValidationSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Web: Validacion de Elementos.
 * Fase BDD: THEN. Categoria: Validacion Web.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("web.validation.element")
public class ElementValidationComponent implements StepComponent {

    /** Display order for this component in the UI. */
    private static final int DISPLAY_ORDER = 120;

    @Override
    public String getName() {
        return "Validacion de Elementos";
    }

    @Override
    public String getDisplayName() {
        return "Validacion de Elementos";
    }

    @Override
    public String getDescription() {
        return "Visibilidad, texto, atributos y estado de elementos";
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
        return "check_box";
    }

    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }

    @Override
    public Class<?> getStepDefinitionClass() {
        return ElementValidationSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "element", "elemento", "visible", "enabled", "disabled",
            "present", "exists", "assert", "check", "validar"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Validacion de Elementos",
            "en", "Element Validation",
            "fr", "Validation des elements"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Visibilidad, texto, atributos y estado de elementos",
            "en", "Element visibility, text, attributes and state",
            "fr", "Visibilite, texte, attributs et etat des elements"
        );
    }
}
