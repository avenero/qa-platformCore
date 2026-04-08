package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.validation.ScreenshotSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Web: Capturas de Pantalla.
 * Fase BDD: THEN. Categoria: Validacion Web.
 * @author Abel Venero
 * @since 2.0.0
 */
public class ScreenshotComponent implements StepComponent {
    @Override public String getName()                  { return "Capturas de Pantalla"; }
    @Override public String getId()                    { return "web.screenshot"; }
    @Override public String getDisplayName()           { return "Capturas de Pantalla"; }
    @Override public String getDescription()           { return "Capturar evidencia y adjuntar al reporte"; }
    @Override public BddPhase getPhase()               { return BddPhase.THEN; }
    @Override public String getCategory()              { return "Validacion Web"; }
    @Override public String getIcon()                  { return "photo_camera"; }
    @Override public int getDisplayOrder()             { return 150; }
    @Override public Class<?> getStepDefinitionClass() { return ScreenshotSteps.class; }
}
