package com.qa.mobilecore.components;

import com.qa.mobilecore.steps.device.NotificationSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Mobile: Notificaciones.
 * Fase BDD: WHEN. Categoria: Interaccion Mobile.
 * @author Abel Venero
 * @since 2.0.0
 */
public class NotificationComponent implements StepComponent {
    @Override public String getName()                  { return "Notificaciones"; }
    @Override public String getId()                    { return "mobile.notification"; }
    @Override public String getDisplayName()           { return "Notificaciones"; }
    @Override public String getDescription()           { return "Interaccion con notificaciones push y del sistema"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Interaccion Mobile"; }
    @Override public String getIcon()                  { return "notifications"; }
    @Override public int getDisplayOrder()             { return 70; }
    @Override public Class<?> getStepDefinitionClass() { return NotificationSteps.class; }
}
