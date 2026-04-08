package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.config.DeviceConfigSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Mobile: Configuracion de Dispositivo.
 * Fase BDD: GIVEN. Categoria: Configuracion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
public class DeviceConfigComponent implements StepComponent {
    @Override public String getName()                  { return "Configuracion de Dispositivo"; }
    @Override public String getId()                    { return "mobile.device.config"; }
    @Override public String getDisplayName()           { return "Configuracion de Dispositivo"; }
    @Override public String getDescription()           { return "Configurar capacidades del dispositivo (plataforma, version, UDID)"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion Mobile"; }
    @Override public String getIcon()                  { return "phone_android"; }
    @Override public int getDisplayOrder()             { return 10; }
    @Override public Class<?> getStepDefinitionClass() { return DeviceConfigSteps.class; }
}
