package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.device.SensorSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Mobile: Sensores del Dispositivo.
 * Fase BDD: WHEN. Categoria: Interaccion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
public class SensorComponent implements StepComponent {
    @Override public String getName()                  { return "Sensores del Dispositivo"; }
    @Override public String getId()                    { return "mobile.sensor"; }
    @Override public String getDisplayName()           { return "Sensores del Dispositivo"; }
    @Override public String getDescription()           { return "Simular GPS, acelerometro, bateria"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Interaccion Mobile"; }
    @Override public String getIcon()                  { return "settings_remote"; }
    @Override public int getDisplayOrder()             { return 80; }
    @Override public Class<?> getStepDefinitionClass() { return SensorSteps.class; }
}
