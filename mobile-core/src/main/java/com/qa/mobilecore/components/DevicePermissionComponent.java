package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.device.DevicePermissionSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Mobile: Permisos del Dispositivo.
 * Fase BDD: GIVEN. Categoria: Configuracion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
public class DevicePermissionComponent implements StepComponent {
    @Override public String getName()                  { return "Permisos del Dispositivo"; }
    @Override public String getId()                    { return "mobile.permissions"; }
    @Override public String getDisplayName()           { return "Permisos del Dispositivo"; }
    @Override public String getDescription()           { return "Conceder o denegar permisos del sistema operativo"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion Mobile"; }
    @Override public String getIcon()                  { return "security"; }
    @Override public int getDisplayOrder()             { return 60; }
    @Override public Class<?> getStepDefinitionClass() { return DevicePermissionSteps.class; }
}
