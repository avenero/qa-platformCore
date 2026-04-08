package com.qa.common.runtime;

import java.util.List;
import java.util.Set;

/**
 * Contrato que cada módulo (api-core, web-core, mobile-core, database) debe implementar.
 *
 * <p>Define el ciclo de vida de un plugin dentro del runtime de ejecución BDD:
 * qué tags lo activan, qué servicios registra, qué componentes de steps aporta,
 * y cómo se comporta al inicio/fin de cada escenario.
 *
 * <p>Los plugins se descubren automáticamente por Java SPI ({@link java.util.ServiceLoader})
 * mediante archivos {@code META-INF/services/com.qa.common.runtime.CorePlugin}.
 *
 * <p><b>Ejemplo de implementación:</b>
 * <pre>
 * public class ApiPlugin implements CorePlugin {
 *     public String getName() { return "api"; }
 *     public Set&lt;String&gt; getActivationTags() { return Set.of("@api", "@rest"); }
 *     // ...
 * }
 * </pre>
 *
 * @author Abel Venero
 * @since 2.0.0
 * @see StepComponent
 * @see ExecutionContext
 */
public interface CorePlugin {

    /**
     * Nombre único del plugin.
     *
     * <p>Usado para identificación en logs, discovery, y configuración.
     * Ejemplos: {@code "api"}, {@code "web"}, {@code "mobile"}, {@code "database"}.
     *
     * @return nombre del plugin, nunca null ni vacío
     */
    String getName();

    /**
     * Tags de Cucumber que activan este plugin.
     *
     * <p>Si un escenario tiene al menos uno de estos tags, el plugin se inicializa
     * para esa ejecución. Ejemplo: {@code Set.of("@api", "@rest", "@http", "@service")}.
     *
     * @return conjunto inmutable de tags (con prefijo @), nunca null
     */
    Set<String> getActivationTags();

    /**
     * Registra servicios en el registry de la ejecución actual.
     *
     * <p>Se invoca una vez al inicio de la ejecución, antes de que corran los escenarios.
     * Los servicios registrados aquí estarán disponibles para los steps vía
     * {@code ExecutionContext.current().service(Class)}.
     *
     * <p><b>Ejemplo:</b>
     * <pre>
     * registry.registerLazy(HttpClient.class, () -&gt; new BaseHttpClient());
     * </pre>
     *
     * @param registry registro de servicios de la ejecución
     * @param config   configuración inmutable de la ejecución
     */
    void registerServices(ServiceRegistry registry, ExecutionConfig config);

    /**
     * Callback invocado al inicio de cada escenario que active este plugin.
     *
     * <p>Útil para inicialización por escenario (ej: crear WebDriver, limpiar headers HTTP).
     * El {@link LifecycleManager} coordina la invocación respetando el orden de plugins.
     *
     * @param context  contexto de la ejecución activa
     */
    void onScenarioStart(ExecutionContext context);

    /**
     * Callback invocado al final de cada escenario que activó este plugin.
     *
     * <p>Útil para limpieza por escenario (ej: cerrar WebDriver, reset HTTP client).
     *
     * @param context  contexto de la ejecución activa
     */
    void onScenarioEnd(ExecutionContext context);

    /**
     * Declara los componentes de steps que este plugin aporta.
     *
     * <p>Cada componente agrupa steps cohesivos por responsabilidad y fase BDD
     * (Given/When/Then). El Backend usa esto para descubrimiento estructurado
     * y el Frontend para construir la paleta visual de diseño.
     *
     * @return lista de componentes, nunca null (puede ser vacía)
     * @see StepComponent
     */
    default List<StepComponent> getComponents() {
        return List.of();
    }

    /**
     * Prioridad de inicialización. Menor valor = se inicializa primero.
     *
     * <p>Valores sugeridos:
     * <ul>
     *   <li>{@code 0} — infraestructura base (database, config)</li>
     *   <li>{@code 50} — API plugin</li>
     *   <li>{@code 100} — Web plugin (default)</li>
     *   <li>{@code 150} — Mobile plugin</li>
     * </ul>
     *
     * @return orden de inicialización
     */
    default int getOrder() {
        return 100;
    }
}

