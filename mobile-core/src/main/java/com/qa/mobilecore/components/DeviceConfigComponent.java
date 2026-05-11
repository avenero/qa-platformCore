package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.config.DeviceConfigSteps;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Mobile: Configuracion de Dispositivo.
 * Fase BDD: GIVEN. Categoria: Configuracion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
@StepId("mobile.device.config")
public class DeviceConfigComponent implements StepComponent {

    private static final int DISPLAY_ORDER = 10;

    @Override public String getName()                  { return "Configuracion de Dispositivo"; }
    @Override public String getDisplayName()           { return "Configuracion de Dispositivo"; }
    @Override public String getDescription()           {
        return "Configurar capacidades del dispositivo (plataforma, version, UDID)";
    }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion Mobile"; }
    @Override public String getIcon()                  { return "phone_android"; }
    @Override public int getDisplayOrder()             { return DISPLAY_ORDER; }
    @Override public Class<?> getStepDefinitionClass() { return DeviceConfigSteps.class; }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "device", "dispositivo", "config", "orientation", "portrait",
            "landscape", "rotation", "language", "locale", "udid"
        );
    }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Configuracion de Dispositivo",
            "en", "Device Configuration",
            "fr", "Configuration du dispositif"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Configurar capacidades del dispositivo (plataforma, version, UDID)",
            "en", "Configure device capabilities (platform, version, UDID)",
            "fr", "Configurer les capacites du dispositif (plateforme, version, UDID)"
        );
    }
}
