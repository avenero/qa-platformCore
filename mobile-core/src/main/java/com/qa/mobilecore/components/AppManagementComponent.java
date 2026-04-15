package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.config.AppManagementSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.Map;

/**
 * Componente de steps Mobile: Gestion de App.
 * Fase BDD: GIVEN. Categoria: Configuracion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("mobile.app.management")
public class AppManagementComponent implements StepComponent {
    @Override public String getName()                  { return "Gestion de App"; }
    @Override public String getDisplayName()           { return "Gestion de App"; }
    @Override public String getDescription()           { return "Instalar, lanzar y cerrar la aplicacion movil"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion Mobile"; }
    @Override public String getIcon()                  { return "apps"; }
    @Override public int getDisplayOrder()             { return 20; }
    @Override public Class<?> getStepDefinitionClass() { return AppManagementSteps.class; }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Gestion de App",
            "en", "App Management",
            "fr", "Gestion de l'application"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Instalar, lanzar y cerrar la aplicacion movil",
            "en", "Install, launch and close the mobile application",
            "fr", "Installer, lancer et fermer l'application mobile"
        );
    }
}
