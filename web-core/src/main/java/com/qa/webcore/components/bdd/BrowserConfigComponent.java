package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.config.BrowserConfigSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;

/**
 * Componente de steps Web: Configuracion de Navegador.
 * Fase BDD: GIVEN. Categoria: Configuracion Web.
 * @author Abel Venero
 * @since 2.0.0
 */
public class BrowserConfigComponent implements StepComponent {
    @Override public String getName()                  { return "Configuracion de Navegador"; }
    @Override public String getId()                    { return "web.browser.config"; }
    @Override public String getDisplayName()           { return "Configuracion de Navegador"; }
    @Override public String getDescription()           { return "Configuracion del browser, modo headless, capabilities"; }
    @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
    @Override public String getCategory()              { return "Configuracion Web"; }
    @Override public String getIcon()                  { return "web"; }
    @Override public int getDisplayOrder()             { return 10; }
    @Override public Class<?> getStepDefinitionClass() { return BrowserConfigSteps.class; }
}
