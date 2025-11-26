package com.scotia.qa.common.cucumber;

import com.scotia.qa.common.logging.TestLogger;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Hooks de Cucumber para gestionar el ciclo de vida del ScenarioContext.
 * Se ejecuta automáticamente antes y después de cada escenario.
 *
 * <p>Garantiza que:
 * <ul>
 *   <li>El contexto esté limpio al inicio de cada escenario (previene state bleeding)</li>
 *   <li>El contexto se limpie al finalizar (evita memory leaks)</li>
 *   <li>Se loguee información útil para debugging</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 1.0
 */
public class ScenarioContextHooks {

    /**
     * Se ejecuta ANTES de cada escenario.
     * Limpia el ScenarioContext para garantizar aislamiento entre escenarios.
     *
     * @param scenario Escenario de Cucumber
     */
    @Before(order = 0) // order = 0 para ejecutar PRIMERO
    public void beforeScenario(Scenario scenario) {
        String scenarioName = scenario.getName();

        // Limpiar contexto previo
        ScenarioContext.clear();

        TestLogger.logDebug("SCENARIO_CONTEXT_HOOKS",
            String.format("🧹 ScenarioContext limpiado para escenario: '%s'", scenarioName),
            null);
    }

    /**
     * Se ejecuta DESPUÉS de cada escenario.
     * Loguea información del contexto y lo limpia para prevenir memory leaks.
     *
     * @param scenario Escenario de Cucumber
     */
    @After(order = Integer.MAX_VALUE) // order = MAX para ejecutar AL FINAL
    public void afterScenario(Scenario scenario) {
        String scenarioName = scenario.getName();
        String status = scenario.getStatus().toString();

        // Loguear tamaño del contexto para debugging
        int contextSize = ScenarioContext.size();

        if (contextSize > 0) {
            TestLogger.logDebug("SCENARIO_CONTEXT_HOOKS",
                String.format("📊 Escenario '%s' finalizó con %d variables en contexto",
                    scenarioName, contextSize),
                null);

            // En caso de fallo, loguear las claves almacenadas (útil para debugging)
            if (scenario.isFailed()) {
                TestLogger.logDebug("SCENARIO_CONTEXT_HOOKS",
                    String.format("🔍 Variables en contexto: %s", ScenarioContext.getKeys()),
                    null);
            }
        }

        // Limpiar contexto
        ScenarioContext.clear();

        TestLogger.logDebug("SCENARIO_CONTEXT_HOOKS",
            String.format("✅ ScenarioContext limpiado después de escenario: '%s' (Status: %s)",
                scenarioName, status),
            null);
    }
}

