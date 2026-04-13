package com.qa.apicore.components;

import com.qa.apicore.steps.validation.ResponseHeaderSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

import java.util.Map;

/**
 * Componente de steps: Response Headers.
 * Fase BDD: THEN. Categoria: Validacion de Respuesta.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ApiResponseHeaderComponent implements StepComponent {
    @Override public String getName()                  { return "Response Headers"; }
    @Override public String getId()                    { return "api.response.headers"; }
    @Override public String getDisplayName()           { return "Response Headers"; }
    @Override public String getDescription()           { return "Validacion de cabeceras de respuesta"; }
    @Override public BddPhase getPhase()               { return BddPhase.THEN; }
    @Override public String getCategory()              { return "Validacion de Respuesta"; }
    @Override public String getIcon()                  { return "receipt_long"; }
    @Override public int getDisplayOrder()             { return 100; }
    @Override public Class<?> getStepDefinitionClass() { return ResponseHeaderSteps.class; }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Response Headers",
            "en", "Response Headers",
            "fr", "En-tetes de reponse"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Validacion de cabeceras de respuesta",
            "en", "Response headers validation",
            "fr", "Validation des en-tetes de reponse"
        );
    }
}
