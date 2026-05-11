package com.qa.common.internal.runtime.hooks;

import com.qa.common.api.Internal;
import com.qa.common.utils.security.SecurityUtilities;

import com.qa.common.api.logging.TestLogger;
import com.qa.common.api.runtime.ExecutionContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Hooks de Cucumber para gestionar el ciclo de vida del {@link ExecutionContext} por escenario.
 *
 * <p>Reemplaza a {@code ScenarioContextHooks} (eliminado en Fase 2).
 * Garantiza que:
 * <ul>
 *   <li>Haya un {@link ExecutionContext} activo antes de cada escenario.</li>
 *   <li>Las variables estén limpias al inicio de cada escenario (previene state bleeding).</li>
 *   <li>El contexto se limpie al finalizar (evita memory leaks).</li>
 * </ul>
 *
 * <p>Cuando el {@link com.qa.common.internal.runtime.CucumberRuntimeEngine} está activo,
 * ya gestiona el ciclo de vida; en ese caso este hook solo limpia las variables
 * sin crear un contexto duplicado.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@Internal(reason = "internal — hooks Cucumber del engine")
public class ScenarioExecutionHooks {

    /** Bandera interna para saber si este hook creó el contexto (y debe limpiarlo). */
    private boolean contextCreatedByThisHook = false;

    /**
     * Se ejecuta ANTES de cada escenario (order = 0 → primero).
     * Si no hay {@link ExecutionContext} activo (ejecución sin Engine), crea uno mínimo.
     * Si ya hay uno (Engine activo), solo limpia las variables del escenario anterior.
     *
     * @param scenario Escenario de Cucumber
     */
    @Before(order = 0)
    public void beforeScenario(Scenario scenario) {
        if (ExecutionContext.current().isEmpty()) {
            // Sin Engine activo → crear contexto mínimo para ejecución legacy/standalone
            ExecutionContext.builder().build().activate();
            contextCreatedByThisHook = true;
            TestLogger.logDebug("SCENARIO_EXEC_HOOKS",
                String.format("✅ ExecutionContext (standalone) activado para escenario: '%s'",
                    scenario.getName()), null);
        } else {
            // Engine activo → solo limpiar variables para aislamiento entre escenarios
            ExecutionContext.current().ifPresent(ctx -> ctx.variables().clear());
            contextCreatedByThisHook = false;
            TestLogger.logDebug("SCENARIO_EXEC_HOOKS",
                String.format("🧹 Variables limpiadas para escenario: '%s'", scenario.getName()), null);
        }
    }

    /**
     * Se ejecuta DESPUÉS de cada escenario (order = MAX → último).
     * Limpia las variables y, si este hook creó el contexto, lo cierra completamente
     * (liberando servicios AutoCloseable como WebDrivers o conexiones DB).
     *
     * @param scenario Escenario de Cucumber
     */
    @After(order = Integer.MAX_VALUE)
    public void afterScenario(Scenario scenario) {
        String scenarioName = scenario.getName();
        String status = scenario.getStatus().toString();

        if (contextCreatedByThisHook) {
            // Modo standalone: este hook creó el contexto → debe limpiarlo completamente,
            // incluyendo destrucción de servicios AutoCloseable (conexiones, drivers, etc.)
            ExecutionContext.current().ifPresent(ctx -> {
                int varCount = ctx.variables().size();
                if (varCount > 0) {
                    TestLogger.logDebug("SCENARIO_EXEC_HOOKS",
                        String.format("📊 Escenario '%s' finalizó con %d variables en VariableStore",
                            scenarioName, varCount), null);
                }
                ctx.cleanup();  // variables.clear() + registry.destroyAll() + eventBus.clear() + deactivate()
            });
            contextCreatedByThisHook = false;
        } else {
            // Modo Engine: solo limpiar variables, el Engine gestiona el ciclo de vida del contexto
            ExecutionContext.current().ifPresent(ctx -> {
                int varCount = ctx.variables().size();
                if (varCount > 0) {
                    TestLogger.logDebug("SCENARIO_EXEC_HOOKS",
                        String.format("📊 Escenario '%s' finalizó con %d variables en VariableStore",
                            scenarioName, varCount), null);
                }
                ctx.variables().clear();
            });
        }

        TestLogger.logDebug("SCENARIO_EXEC_HOOKS",
            String.format("✅ Cleanup completado tras escenario: '%s' (Status: %s)",
                scenarioName, status), null);
    }
}

