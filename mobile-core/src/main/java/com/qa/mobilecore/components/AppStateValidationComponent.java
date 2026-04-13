package com.qa.mobilecore.components;

import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.mobilecore.steps.validation.AppStateValidationSteps;

import java.util.Map;

/**
 * Componente de steps Mobile: Validacion del Estado de la App.
 *
 * <p>Agrupa steps que validan el estado global de la aplicacion mobile:
 * si esta en primer plano, instalada, orientacion del dispositivo, sesion activa.</p>
 *
 * <p>Fase BDD: THEN. Categoria: Validacion Mobile.</p>
 *
 * @author Abel Venero
 * @since 2.0.0
 * @see AppStateValidationSteps
 */
public class AppStateValidationComponent implements StepComponent {

    @Override
    public String getName() { return "Validacion Estado de App"; }

    @Override
    public String getId() { return "mobile.validation.app-state"; }

    @Override
    public String getDisplayName() { return "Estado de la Aplicacion"; }

    @Override
    public String getDescription() {
        return "Valida el estado global de la app mobile: foreground/background, " +
               "instalacion, orientacion del dispositivo y sesion activa";
    }

    @Override
    public BddPhase getPhase() { return BddPhase.THEN; }

    @Override
    public String getCategory() { return "Validacion Mobile"; }

    @Override
    public String getIcon() { return "mobile_friendly"; }

    @Override
    public int getDisplayOrder() { return 95; }

    @Override
    public Class<?> getStepDefinitionClass() { return AppStateValidationSteps.class; }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Estado de la Aplicacion",
            "en", "Application State",
            "fr", "Etat de l'application"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Valida el estado global de la app mobile: foreground/background, instalacion, orientacion del dispositivo y sesion activa",
            "en", "Validates the global state of the mobile app: foreground/background, installation, device orientation and active session",
            "fr", "Valide l'etat global de l'application mobile : premier plan/arriere-plan, installation, orientation du dispositif et session active"
        );
    }
}
