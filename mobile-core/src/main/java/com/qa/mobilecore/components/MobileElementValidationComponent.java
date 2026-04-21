package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.validation.MobileElementValidationSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.Map;

/**
 * Componente de steps Mobile: Validacion de Elementos Mobile.
 * Fase BDD: THEN. Categoria: Validacion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("mobile.validation")
public class MobileElementValidationComponent implements StepComponent {

    private static final int DISPLAY_ORDER = 90;

    @Override public String getName()                  { return "Validacion de Elementos Mobile"; }
    @Override public String getDisplayName()           { return "Validacion de Elementos Mobile"; }
    @Override public String getDescription()           { return "Visibilidad, texto y estado de elementos nativos"; }
    @Override public BddPhase getPhase()               { return BddPhase.THEN; }
    @Override public String getCategory()              { return "Validacion Mobile"; }
    @Override public String getIcon()                  { return "check_circle"; }
    @Override public int getDisplayOrder()             { return DISPLAY_ORDER; }
    @Override public Class<?> getStepDefinitionClass() { return MobileElementValidationSteps.class; }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Validacion de Elementos Mobile",
            "en", "Mobile Element Validation",
            "fr", "Validation des elements mobiles"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Visibilidad, texto y estado de elementos nativos",
            "en", "Visibility, text and state of native elements",
            "fr", "Visibilite, texte et etat des elements natifs"
        );
    }
}
