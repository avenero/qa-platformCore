package com.qa.common.runtime;

import java.util.Collection;

/**
 * Coordinador del ciclo de vida de la ejecucion BDD.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Descubrir plugins activos segun tags</li>
 *   <li>Coordinar {@code onScenarioStart} / {@code onScenarioEnd}</li>
 *   <li>Gestionar orden de inicializacion por prioridad</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public interface LifecycleManager {

    /**
     * Inicializa la ejecucion: descubre plugins, registra servicios.
     * @param request solicitud de ejecucion
     * @return contexto inicializado
     */
    ExecutionContext initialize(ExecutionRequest request);

    /**
     * Notifica inicio de escenario a todos los plugins activos.
     * @param context contexto activo
     * @param scenarioTags tags del escenario
     */
    void onScenarioStart(ExecutionContext context, Collection<String> scenarioTags);

    /**
     * Notifica fin de escenario a todos los plugins activos.
     * @param context contexto activo
     * @param scenarioTags tags del escenario
     */
    void onScenarioEnd(ExecutionContext context, Collection<String> scenarioTags);

    /**
     * Limpieza final de la ejecucion.
     * @param context contexto a limpiar
     */
    void shutdown(ExecutionContext context);
}
