package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.interaction.AlertSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Web: Alertas y Dialogos.
 * Fase BDD: WHEN. Categoria: Interaccion.
 * @author Abel Venero
 * @since 2.0.0
 */
public class AlertComponent implements StepComponent {
    @Override public String getName()                  { return "Alertas y Dialogos"; }
    @Override public String getId()                    { return "web.alert"; }
    @Override public String getDisplayName()           { return "Alertas y Dialogos"; }
    @Override public String getDescription()           { return "Aceptar, cancelar y leer alertas del navegador"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Interaccion"; }
    @Override public String getIcon()                  { return "warning"; }
    @Override public int getDisplayOrder()             { return 100; }
    @Override public Class<?> getStepDefinitionClass() { return AlertSteps.class; }
}
