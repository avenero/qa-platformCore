package com.qa.mobilecore.components;

import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.mobilecore.steps.validation.AppStateValidationSteps;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
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
@StepId("mobile.validation.app-state")
public class AppStateValidationComponent implements StepComponent {

    private static final int DISPLAY_ORDER = 95;

    @Override
    public String getName() { return "Validacion Estado de App"; }

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
    public int getDisplayOrder() { return DISPLAY_ORDER; }

    @Override
    public Class<?> getStepDefinitionClass() { return AppStateValidationSteps.class; }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "state", "estado", "foreground", "background", "running",
            "terminated", "app-state", "lifecycle", "sesion-activa"
        );
    }

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
            "es", "Valida el estado global de la app mobile: foreground/background, "
                + "instalacion, orientacion del dispositivo y sesion activa",
            "en", "Validates the global state of the mobile app: foreground/background, "
                + "installation, device orientation and active session",
            "fr", "Valide l'etat global de l'application mobile : premier plan/arriere-plan, "
                + "installation, orientation du dispositif et session active"
        );
    }
}
