package com.qa.common.runtime;

/**
 * Coordinador del ciclo de vida de la ejecucion BDD.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Descubrir plugins activos segun tags</li>
 *   <li>Coordinar {@code onScenarioStart} / {@code onScenarioEnd} para cada escenario</li>
 *   <li>Gestionar orden de inicializacion por prioridad</li>
 * </ul>
 *
 * <p><b>Cambio de contrato en 2.1.0:</b> los métodos {@link #onScenarioStart} y
 * {@link #onScenarioEnd} reciben {@link ScenarioMetadata} en lugar de
 * {@code Collection<String> scenarioTags}. Esto permite a las implementaciones
 * acceder al nombre, URI y línea del escenario además de sus tags.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public interface LifecycleManager {

    /**
     * Inicializa la ejecucion: descubre plugins, registra servicios.
     *
     * @param request solicitud de ejecucion
     * @return contexto inicializado y activado en el ThreadLocal del hilo actual
     */
    ExecutionContext initialize(ExecutionRequest request);

    /**
     * Notifica inicio de escenario a todos los plugins activos para este escenario.
     *
     * <p>Los plugins se filtran por los tags presentes en {@code scenarioMetadata.tags()}.
     * Se invoca en orden ascendente de {@link CorePlugin#getOrder()}.
     * Las excepciones en plugins individuales se absorben (se loggean como ERROR)
     * para no abortar la cadena.
     *
     * @param context          contexto activo de la ejecucion, no null
     * @param scenarioMetadata metadatos del escenario a iniciar, no null
     */
    void onScenarioStart(ExecutionContext context, ScenarioMetadata scenarioMetadata);

    /**
     * Notifica fin de escenario a todos los plugins activos para este escenario.
     *
     * <p>Se invoca en orden <em>inverso</em> de {@link CorePlugin#getOrder()} para garantizar
     * una limpieza simétrica (LIFO). Las excepciones en plugins individuales se absorben.
     *
     * @param context          contexto activo de la ejecucion, no null
     * @param scenarioMetadata metadatos del escenario que finalizó, no null
     */
    void onScenarioEnd(ExecutionContext context, ScenarioMetadata scenarioMetadata);

    /**
     * Limpieza final de la ejecucion.
     *
     * <p>Debe invocarse en un bloque {@code finally} después de {@link #initialize}.
     * Cierra recursos ({@link ServiceRegistry#destroyAll()}) y desactiva el ThreadLocal.
     *
     * @param context contexto a limpiar (acepta null sin lanzar excepción)
     */
    void shutdown(ExecutionContext context);
}
