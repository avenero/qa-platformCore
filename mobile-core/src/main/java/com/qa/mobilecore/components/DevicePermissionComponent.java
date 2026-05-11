package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.device.DevicePermissionSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Mobile: Permisos del Dispositivo.
 * Fase BDD: GIVEN. Categoria: Configuracion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("mobile.permissions")
public class DevicePermissionComponent implements StepComponent {

    private static final int DISPLAY_ORDER = 60;

    @Override public String getName()                  { return "Permisos del Dispositivo"; }
    @Override public String getDisplayName()           { return "Permisos del Dispositivo"; }
    @Override public String getDescription()           { return "Conceder o denegar permisos del sistema operativo"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion Mobile"; }
    @Override public String getIcon()                  { return "security"; }
    @Override public int getDisplayOrder()             { return DISPLAY_ORDER; }
    @Override public Class<?> getStepDefinitionClass() { return DevicePermissionSteps.class; }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "permission", "permiso", "camera", "location", "microphone",
            "notification", "grant", "allow", "deny", "autorisation"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Permisos del Dispositivo",
            "en", "Device Permissions",
            "fr", "Autorisations du dispositif"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Conceder o denegar permisos del sistema operativo",
            "en", "Grant or deny operating system permissions",
            "fr", "Accorder ou refuser des autorisations du systeme d'exploitation"
        );
    }
}
