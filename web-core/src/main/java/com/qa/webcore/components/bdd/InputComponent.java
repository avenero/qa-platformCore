package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.interaction.InputSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

import java.util.Map;

/**
 * Componente de steps Web: Entrada de Texto.
 * Fase BDD: WHEN. Categoria: Interaccion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class InputComponent implements StepComponent {
    @Override public String getName()                  { return "Entrada de Texto"; }
    @Override public String getId()                    { return "web.input"; }
    @Override public String getDisplayName()           { return "Entrada de Texto"; }
    @Override public String getDescription()           { return "Escribir texto, limpiar campos, teclado"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Interaccion"; }
    @Override public String getIcon()                  { return "keyboard"; }
    @Override public int getDisplayOrder()             { return 70; }
    @Override public Class<?> getStepDefinitionClass() { return InputSteps.class; }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Entrada de Texto",
            "en", "Text Input",
            "fr", "Saisie de texte"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Escribir texto, limpiar campos, teclado",
            "en", "Write text, clear fields, keyboard",
            "fr", "Saisir du texte, vider des champs, clavier"
        );
    }
}
