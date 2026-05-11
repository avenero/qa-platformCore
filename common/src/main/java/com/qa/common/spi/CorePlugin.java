package com.qa.common.spi;

import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.internal.runtime.ExecutionContext;
import com.qa.common.internal.runtime.ServiceRegistry;
import com.qa.common.utils.security.SecurityUtilities;

import com.qa.common.api.driver.CapabilityReport;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contrato que cada módulo de testing (http-core, web-core, mobile-core, database-core)
 * debe implementar para integrarse con el runtime de ejecución BDD.
 *
 * <h2>Responsabilidades del plugin</h2>
 * <ul>
 *   <li><b>Identidad</b> — {@link #platformId()}, {@link #displayName()}, {@link #version()}</li>
 *   <li><b>Introspección</b> — {@link #describeCapabilities()} para que el BE/FE presenten
 *       opciones disponibles al usuario antes de diseñar o ejecutar escenarios</li>
 *   <li><b>Ciclo de vida</b> — hooks de suite y escenario ({@link #onSuiteStart},
 *       {@link #onScenarioStart}, {@link #onScenarioEnd}, {@link #onSuiteEnd})</li>
 *   <li><b>Servicios</b> — {@link #registerServices} para publicar drivers y helpers
 *       en el {@link ServiceRegistry}</li>
 *   <li><b>Steps Cucumber</b> — {@link #getComponents()} y {@link #getGluePackages()}</li>
 * </ul>
 *
 * <h2>Hook order — matriz de prioridades</h2>
 * <p>Menor valor = se inicializa primero. El orden garantiza que los plugins de
 * infraestructura estén listos antes que los de protocolo.
 * <pre>
 *   Orden   Plugin           Razón
 *   ─────   ──────────────   ──────────────────────────────────────────────────
 *      0    DatabasePlugin   Provee datos/fixtures precondición para los demás
 *     50    ApiPlugin        Infraestructura HTTP lista antes de ciclos Web/Mobile
 *    100    WebPlugin        Valor default — browsers post-HTTP
 *    150    MobilePlugin     Última en inicializarse; requiere dispositivo disponible
 * </pre>
 *
 * <h2>Descubrimiento vía SPI</h2>
 * <p>Los plugins se descubren automáticamente con {@link java.util.ServiceLoader}
 * desde archivos {@code META-INF/services/com.qa.common.runtime.CorePlugin} en cada
 * módulo. Cada módulo registra su propio plugin; {@code common} no registra ninguno.
 *
 * <h2>Ejemplo mínimo</h2>
 * <pre>
 * public class ApiPlugin implements CorePlugin {
 *     {@literal @}Override public String platformId() { return "HTTP"; }
 *     {@literal @}Override public String displayName() { return "HTTP API Testing"; }
 *     {@literal @}Override public CapabilityReport describeCapabilities() {
 *         return CapabilityReport.available("HTTP", List.of(
 *             new CapabilityDescriptor("rest", "REST", "HTTP/HTTPS endpoints")
 *         ));
 *     }
 *     // ... lifecycle hooks, registerServices, getActivationTags, etc.
 * }
 * </pre>
 *
 * @author Abel Venero
 * @since 2.0.0 (firma original)
 * @since 3.0.0 (platformId, displayName, describeCapabilities añadidos)
 * @see com.qa.common.api.driver.CapabilityReport
 * @see StepComponent
 * @see ExecutionContext
 */
public interface CorePlugin {

    // =========================================================================
    // Identidad (NUEVO en 3.0.0)
    // =========================================================================

    /**
     * Identificador técnico de la plataforma, en mayúsculas.
     *
     * <p>Valores canónicos: {@code "HTTP"}, {@code "WEB"}, {@code "ANDROID"},
     * {@code "IOS"}, {@code "MOBILE"}, {@code "DATABASE"}.
     *
     * <p>Este identificador reemplaza a {@link #getName()} como fuente de verdad
     * y se propaga al backend para routing de ejecuciones.
     *
     * @return identificador de plataforma, nunca null ni vacío
     * @since 3.0.0
     */
    String platformId();

    /**
     * Nombre legible del plugin para el usuario final.
     *
     * <p>Se muestra en el FE (selector de plataforma, logs de ejecución).
     * Ejemplos: {@code "HTTP API Testing"}, {@code "Web Browser Testing"},
     * {@code "Mobile App Testing"}, {@code "Database Testing"}.
     *
     * @return nombre de display, nunca null ni vacío
     * @since 3.0.0
     */
    String displayName();

    /**
     * Versión semántica del plugin.
     *
     * <p>Por defecto retorna {@code "2.1.0"}; sobrescribir en implementaciones
     * que publiquen una versión de artefacto distinta.
     *
     * @return versión SemVer del plugin, nunca null
     * @since 3.0.0
     */
    default String version() {
        return "2.1.0";
    }

    /**
     * Nombre corto del plugin (alias de {@link #platformId()} en minúsculas).
     *
     * @return nombre del plugin (ej: {@code "api"}, {@code "web"}, {@code "mobile"}, {@code "database"})
     * @deprecated desde 3.0.0 — usar {@link #platformId()} para identidad técnica o
     *             {@link #displayName()} para presentación al usuario.
     *             Será eliminado en 4.0.0.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    default String getName() {
        return platformId().toLowerCase();
    }

    // =========================================================================
    // Introspección de capabilities (NUEVO en 3.0.0)
    // =========================================================================

    /**
     * Describe qué capacidades ofrece este plugin en el entorno de ejecución actual.
     *
     * <p>El motor de ejecución y el backend invocan este método para construir
     * dinámicamente las opciones que el usuario ve al diseñar o ejecutar escenarios
     * (ej: lista de browsers disponibles, dispositivos conectados, motores de BD configurados).
     *
     * <p>Implementaciones deben preferir introspección real del entorno (verificar si
     * Appium está levantado, si hay drivers de BD disponibles, etc.) cuando el costo
     * de la comprobación es bajo. Para comprobaciones costosas, retornar el conjunto
     * estático conocido y delegar la validación real al {@link #onSuiteStart}.
     *
     * @return informe de disponibilidad y opciones configurables, nunca null
     * @since 3.0.0
     * @see com.qa.common.api.driver.CapabilityReport
     * @see com.qa.common.api.driver.CapabilityDescriptor
     */
    CapabilityReport describeCapabilities();

    // =========================================================================
    // Activación
    // =========================================================================

    /**
     * Tags de Cucumber que activan este plugin.
     *
     * <p>Si un escenario tiene al menos uno de estos tags, el plugin se inicializa
     * para esa ejecución.
     *
     * <p>Convención: todos los tags llevan prefijo {@code @}.
     * Ejemplo: {@code Set.of("@api", "@rest", "@http", "@service")}.
     *
     * @return conjunto inmutable de tags de activación, nunca null
     */
    Set<String> getActivationTags();

    // =========================================================================
    // Cucumber glue
    // =========================================================================

    /**
     * Paquetes Java donde Cucumber debe buscar step definitions y hooks.
     *
     * <p>La implementación por defecto deriva los paquetes automáticamente desde
     * {@link #getComponents()}, eliminando duplicados y ordenando el resultado.
     * Sobrescribir para incluir paquetes de hooks separados.
     *
     * <p><b>Ejemplo de sobrescritura:</b>
     * <pre>
     * {@literal @}Override
     * public List&lt;String&gt; getGluePackages() {
     *     List&lt;String&gt; base = new ArrayList&lt;&gt;(CorePlugin.super.getGluePackages());
     *     base.add("com.qa.mymodule.hooks");
     *     base.sort(Comparator.naturalOrder());
     *     return Collections.unmodifiableList(base);
     * }
     * </pre>
     *
     * @return lista de paquetes Java sin duplicados, ordenada, nunca null
     * @since 2.2.0
     */
    default List<String> getGluePackages() {
        return getComponents().stream()
            .map(StepComponent::getStepDefinitionClass)
            .filter(Objects::nonNull)
            .map(Class::getPackageName)
            .distinct()
            .sorted()
            .collect(Collectors.toUnmodifiableList());
    }

    // =========================================================================
    // Ciclo de vida — suite
    // =========================================================================

    /**
     * Callback invocado una sola vez al inicio de la suite (antes del primer escenario).
     *
     * <p>Usar para inicialización pesada que no debe repetirse por escenario:
     * abrir conexión de pool compartida, iniciar servidor embebido, etc.
     *
     * @param config configuración inmutable de la suite
     * @since 3.0.0
     */
    default void onSuiteStart(ExecutionConfig config) { }

    /**
     * Callback invocado una sola vez al final de la suite (después del último escenario).
     *
     * <p>Usar para liberar recursos inicializados en {@link #onSuiteStart}.
     *
     * @since 3.0.0
     */
    default void onSuiteEnd() { }

    // =========================================================================
    // Ciclo de vida — escenario
    // =========================================================================

    /**
     * Registra servicios en el registry de la ejecución.
     *
     * <p>Se invoca una vez al inicio de la ejecución, antes de que corran los escenarios.
     * Los servicios registrados aquí estarán disponibles para los steps vía
     * {@code ExecutionContext.current().service(Class)}.
     *
     * @param registry registro de servicios de la ejecución
     * @param config   configuración inmutable de la ejecución
     */
    void registerServices(ServiceRegistry registry, ExecutionConfig config);

    /**
     * Callback invocado al inicio de cada escenario que active este plugin.
     *
     * @param context contexto de la ejecución activa
     */
    void onScenarioStart(ExecutionContext context);

    /**
     * Callback invocado al final de cada escenario que activó este plugin.
     *
     * @param context contexto de la ejecución activa
     */
    void onScenarioEnd(ExecutionContext context);

    // =========================================================================
    // Steps
    // =========================================================================

    /**
     * Componentes de steps que este plugin aporta al framework.
     *
     * <p>Cada componente agrupa steps cohesivos por responsabilidad y fase BDD.
     * El backend usa esto para descubrimiento estructurado y el frontend para
     * construir la paleta visual de diseño de escenarios.
     *
     * @return lista de componentes, nunca null (puede ser vacía)
     * @see StepComponent
     */
    default List<StepComponent> getComponents() {
        return List.of();
    }

    // =========================================================================
    // Ordering
    // =========================================================================

    /**
     * Orden de inicialización del plugin. Menor valor = se inicializa primero.
     *
     * <p>Matriz de órdenes canónicos:
     * <table>
     *   <tr><th>Orden</th><th>Plugin</th>          <th>Razón</th></tr>
     *   <tr><td>0</td>    <td>DatabasePlugin</td>  <td>Provee datos precondición</td></tr>
     *   <tr><td>50</td>   <td>ApiPlugin</td>       <td>HTTP ready antes de Web/Mobile</td></tr>
     *   <tr><td>100</td>  <td>WebPlugin</td>       <td>Valor default</td></tr>
     *   <tr><td>150</td>  <td>MobilePlugin</td>    <td>Requiere dispositivo disponible</td></tr>
     * </table>
     *
     * @return orden de inicialización (default: 100)
     * @see #getHookOrder()
     */
    default int getOrder() {
        return 100;
    }

    /**
     * Alias moderno de {@link #getOrder()} — mismo contrato, nombre más descriptivo.
     *
     * <p>Prefiere usar este método en código nuevo; {@link #getOrder()} se mantiene
     * por compatibilidad con implementaciones existentes.
     *
     * @return orden de inicialización del plugin
     * @since 3.0.0
     */
    default int getHookOrder() {
        return getOrder();
    }
}
