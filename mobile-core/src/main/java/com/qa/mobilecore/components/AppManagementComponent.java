package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.config.AppManagementSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Mobile: Gestion de App.
 * Fase BDD: GIVEN. Categoria: Configuracion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
public class AppManagementComponent implements StepComponent {
    @Override public String getName()                  { return "Gestion de App"; }
    @Override public String getId()                    { return "mobile.app.management"; }
    @Override public String getDisplayName()           { return "Gestion de App"; }
    @Override public String getDescription()           { return "Instalar, lanzar y cerrar la aplicacion movil"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion Mobile"; }
    @Override public String getIcon()                  { return "apps"; }
    @Override public int getDisplayOrder()             { return 20; }
    @Override public Class<?> getStepDefinitionClass() { return AppManagementSteps.class; }
}
