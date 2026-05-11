package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.interaction.AlertSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Web: Alertas y Dialogos.
 * Fase BDD: WHEN. Categoria: Interaccion.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("web.alert")
public class AlertComponent implements StepComponent {
    @Override
    public String getName() {
        return "Alertas y Dialogos";
    }

    @Override
    public String getDisplayName() {
        return "Alertas y Dialogos";
    }

    @Override
    public String getDescription() {
        return "Aceptar, cancelar y leer alertas del navegador";
    }

    @Override
    public BddPhase getPhase() {
        return BddPhase.WHEN;
    }

    @Override
    public String getCategory() {
        return "Interaccion";
    }

    @Override
    public String getIcon() {
        return "warning";
    }

    @Override
    public int getDisplayOrder() {
        return 100;
    }

    @Override
    public Class<?> getStepDefinitionClass() {
        return AlertSteps.class;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "alert", "popup", "modal", "dialog", "dialogo",
            "alerte", "confirm", "accept", "dismiss", "javascript-alert"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Alertas y Dialogos",
            "en", "Alerts & Dialogs",
            "fr", "Alertes et boites de dialogue"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Aceptar, cancelar y leer alertas del navegador",
            "en", "Accept, cancel and read browser alerts",
            "fr", "Accepter, annuler et lire les alertes du navigateur"
        );
    }
}
