package com.qa.common.internal.runtime;


import com.qa.common.spi.CorePlugin;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.StepDefinitionInfo;
import com.qa.common.api.runtime.StepInfo;
import com.qa.common.utils.security.SecurityUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Servicio de descubrimiento de steps agrupados por componente y por método individual.
 *
 * <p>Escanea todos los {@link CorePlugin} registrados y extrae sus
 * {@link StepComponent}s, proporcionando metadata estructurada en dos niveles de
 * granularidad para:
 * <ul>
 *   <li>El Backend: endpoint {@code GET /api/steps} que expone el catálogo completo</li>
 *   <li>El Frontend: paleta visual de componentes BDD para el Scenario Builder</li>
 *   <li>El Engine: glue paths necesarios para una ejecucion</li>
 * </ul>
 *
 * <h2>Modelo de dos niveles</h2>
 * <p>El catálogo opera en dos granularidades complementarias:
 * <ol>
 *   <li><strong>Nivel componente</strong> — {@link #discoverAll()}: agrupa steps por
 *       responsabilidad. Ejemplo: {@code "api.authentication"} agrupa todos los steps
 *       de autenticación HTTP.</li>
 *   <li><strong>Nivel step</strong> — {@link #discoverAllStepDefs()}: cada método
 *       individual {@code @Given/@When/@Then}. Ejemplo: el step
 *       {@code "api.authentication.bearer.rut"} con patrón
 *       {@code "agrego autenticación Bearer para RUT {string}"}.</li>
 * </ol>
 * <p>El nivel step es construido por {@link StepMethodScanner} vía reflexión sobre
 * {@link StepComponent#getStepDefinitionClass()}.
 *
 * <h2>Resolución por ID (bridge BE ↔ Core)</h2>
 * <p>El Backend puede resolver de forma unívoca tanto un componente como un step individual
 * sin recorrer todos los plugins:
 * <pre>
 *   // Nivel componente
 *   Optional&lt;ComponentInfo&gt; info = discovery.resolveStep("api.authentication");
 *   info.ifPresent(c -> engine.execute(buildRequest(c)));
 *
 *   // Nivel step individual
 *   Optional&lt;StepDefinitionInfo&gt; sdi = discovery.resolveStepDef("api.authentication.bearer.rut");
 *   sdi.ifPresent(s -> LOG.info("Patrón: {}", s.cucumberPattern()));
 * </pre>
 *
 * <h2>Validación de IDs</h2>
 * <p>Al inicializarse, el servicio valida que no existan IDs duplicados entre todos los
 * componentes. Si los hay, emite una advertencia en el LOG (no falla la aplicación, para no
 * romper entornos con plugins de terceros). Puede forzarse la verificación programática con
 * {@link #validateIds()}.
 *
 * @author Abel Venero
 * @since 2.0.0
 * @see CorePlugin#getComponents()
 * @see StepComponent
 * @see com.qa.common.api.runtime.annotation.StepId
 * @see StepMethodScanner
 * @see StepDefinitionInfo
 */
public final class StepDiscoveryService {

    private static final Logger LOG = LoggerFactory.getLogger(StepDiscoveryService.class);

    private final List<CorePlugin> plugins;

    /** Escáner de reflexión para el nivel 2 del catálogo (steps individuales). */
    private final StepMethodScanner stepMethodScanner = new StepMethodScanner();

    /**
     * Constructor con plugins explicitamente proporcionados (para testing).
     * @param plugins lista de plugins, no null
     */
    public StepDiscoveryService(List<CorePlugin> plugins) {
        Objects.requireNonNull(plugins, "plugins no puede ser null");
        this.plugins = List.copyOf(plugins);
        LOG.debug("StepDiscoveryService inicializado con {} plugins", this.plugins.size());
        validateIds();
    }

    /**
     * Crea un StepDiscoveryService descubriendo plugins via Java SPI.
     * @return nueva instancia con plugins descubiertos
     */
    public static StepDiscoveryService withServiceLoader() {
        List<CorePlugin> discovered = StreamSupport.stream(ServiceLoader.load(CorePlugin.class).spliterator(), false).
                sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder())).collect(Collectors.toList());
        LOG.info("Plugins descubiertos via SPI: {}", discovered.size());
        discovered.forEach(p -> LOG.info("  Plugin: {} (order={})", p.getName(), p.getOrder()));
        return new StepDiscoveryService(discovered);
    }

    /**
     * Descubre todos los componentes de todos los plugins.
     *
     * @return lista de {@link ComponentInfo} con metadata de plugin + componente
     */
    public List<ComponentInfo> discoverAll() {
        return plugins.stream().flatMap(plugin -> plugin.getComponents().stream().
                        map(component -> new ComponentInfo(plugin.getName(), component))).
                collect(Collectors.toUnmodifiableList());
    }

    /**
     * Descubre componentes filtrados por fase BDD.
     *
     * @param phase fase BDD (GIVEN, WHEN, THEN)
     * @return componentes de la fase indicada
     */
    public List<ComponentInfo> discoverByPhase(BddPhase phase) {
        Objects.requireNonNull(phase, "phase no puede ser null");
        return discoverAll().stream().filter(info -> info.component().getPhase() == phase).
                collect(Collectors.toUnmodifiableList());
    }

    /**
     * Descubre componentes de un plugin especifico.
     *
     * @param pluginName nombre del plugin (ej: "api", "web")
     * @return componentes del plugin
     */
    public List<ComponentInfo> discoverByPlugin(String pluginName) {
        Objects.requireNonNull(pluginName, "pluginName no puede ser null");
        return plugins.stream().filter(p -> p.getName().equals(pluginName)).flatMap(p -> p.getComponents().stream().
                        map(c -> new ComponentInfo(p.getName(), c))).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Resuelve un componente por su {@code stepId} estable.
     *
     * <p>Este método es el <strong>bridge principal</strong> para que el Backend resuelva
     * un {@link StepComponent} a partir del ID persistido en la base de datos, sin recorrer
     * todos los plugins manualmente. Se usa en:
     * <ul>
     *   <li>Ejecución asíncrona: mapear {@code stepId} → glue path del escenario</li>
     *   <li>Export documental: resolver metadata del componente para generar documentación</li>
     *   <li>Lint BDD: verificar que todos los step IDs del escenario existen en el catálogo</li>
     *   <li>Import (Swagger/Postman/User Story): sugerir el componente más afín</li>
     * </ul>
     *
     * <p>Ejemplo de uso:
     * <pre>
     *   discovery.resolveStep("api.authentication")
     *       .map(ComponentInfo::component)
     *       .ifPresent(c -> LOG.info("Encontrado: {}", c.getDisplayName()));
     * </pre>
     *
     * <p>Si el {@code stepId} corresponde a un componente marcado como
     * {@link com.qa.common.api.runtime.annotation.StepId#deprecated() deprecated},
     * se emite una advertencia en el LOG indicando
     * el ID de reemplazo (si está declarado).
     *
     * @param stepId identificador estable del componente (ej: {@code "api.authentication"})
     * @return {@link Optional} con el primer {@link ComponentInfo} que coincida, o vacío si no existe
     */
    public Optional<ComponentInfo> resolveStep(String stepId) {
        Objects.requireNonNull(stepId, "stepId no puede ser null");
        Optional<ComponentInfo> found = discoverAll().stream().filter(info -> stepId.equals(info.component().getId())).
                findFirst();

        found.ifPresent(info -> {
            StepComponent c = info.component();
            if (c.isDeprecated()) {
                String replacement = c.getReplacementStepId() != null
                        ? "'" + c.getReplacementStepId() + "'"
                        : "(sin reemplazo declarado)";
                LOG.warn("StepId '{}' está marcado como DEPRECATED. Reemplazo: {}. "
                        + "Actualiza los escenarios que usen este ID.", stepId, replacement);
            }
        });

        if (found.isEmpty()) {
            LOG.debug("resolveStep('{}') → no encontrado en {} plugins", stepId, plugins.size());
        }

        return found;
    }

    /**
     * Agrupa todos los componentes por fase BDD.
     *
     * @return mapa: fase BDD → lista de componentes
     */
    public Map<BddPhase, List<ComponentInfo>> groupByPhase() {
        return discoverAll().stream().collect(Collectors.groupingBy(
                        info -> info.component().getPhase(),
                        Collectors.toUnmodifiableList()
                ));
    }

    /**
     * Agrupa todos los componentes por nombre de plugin.
     *
     * @return mapa: nombre plugin → lista de componentes
     */
    public Map<String, List<ComponentInfo>> groupByPlugin() {
        return discoverAll().stream().collect(Collectors.groupingBy(
                        ComponentInfo::pluginName,
                        Collectors.toUnmodifiableList()
                ));
    }

    /**
     * Obtiene los nombres de plugins registrados.
     * @return lista de nombres ordenados por prioridad
     */
    public List<String> getPluginNames() {
        return plugins.stream().map(CorePlugin::getName).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Cantidad total de componentes descubiertos.
     * @return total de componentes
     */
    public int totalComponents() {
        return plugins.stream().mapToInt(p -> p.getComponents().size()).sum();
    }

    /**
     * Exporta todos los componentes como {@link StepInfo}, el DTO que consume el Backend.
     *
     * <p>Cada {@link StepInfo} incluye:
     * <ul>
     *   <li>Mapas i18n copiados desde el componente, listos para serializar en
     *       {@code GET /api/steps} sin procesamiento adicional.</li>
     *   <li>Flags de deprecación: {@link StepInfo#deprecated()} y
     *       {@link StepInfo#replacementStepId()}, propagados desde
     *       {@link StepComponent#isDeprecated()} y {@link StepComponent#getReplacementStepId()}.</li>
     * </ul>
     *
     * @return lista inmutable de StepInfo; uno por componente registrado
     */
    public List<StepInfo> discoverAllAsStepInfo() {
        return discoverAll().stream().map(info -> {
                    StepComponent c = info.component();
                    return new StepInfo(
                            c.getId(),
                            c.getDisplayName(),
                            info.pluginName(),
                            c.getId(),
                            c.getPhase().name(),
                            c.getCategory(),
                            c.getIcon(),
                            c.getDisplayOrder(),
                            c.getDisplayName(),
                            c.getDescription(),
                            c.getDisplayNameByLocale(),
                            c.getDescriptionByLocale(),
                            c.isDeprecated(),
                            c.getReplacementStepId(),
                            c.getKeywords()
                    );
                }).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Verifica que no existan IDs duplicados entre todos los componentes registrados.
     *
     * <p>Un ID duplicado es un error de configuración: el Backend no podría resolver
     * unívocamente un componente por {@code stepId}. Si se detectan duplicados, se emite
     * una advertencia en el LOG con los IDs afectados. No lanza excepción para no romper
     * entornos con plugins de terceros.
     *
     * <p>Este método se llama automáticamente en el constructor. También puede invocarse
     * programáticamente (ej: en tests de integración o al arrancar el Backend).
     *
     * @return conjunto de IDs duplicados; vacío si todo es correcto
     */
    public Set<String> validateIds() {
        List<String> allIds = discoverAll().stream().map(info -> info.component().getId()).collect(Collectors.toList());

        Set<String> duplicates = allIds.stream().filter(id -> allIds.stream().filter(id::equals).count() > 1).
                collect(Collectors.toSet());

        if (!duplicates.isEmpty()) {
            LOG.warn("⚠️  StepDiscoveryService detectó IDs de step DUPLICADOS: {}. "
                    + "Esto impedirá la resolución unívoca por stepId desde el Backend. "
                    + "Revisá los componentes afectados y asigná IDs únicos con @StepId.",
                    duplicates);
        } else {
            LOG.debug("Validación de IDs OK: {} componentes con IDs únicos", allIds.size());
        }

        return duplicates;
    }

    /**
     * Obtiene la lista de plugins registrados.
     * @return lista inmutable de plugins
     */
    public List<CorePlugin> getPlugins() {
        return plugins;
    }

    /**
     * Retorna todos los paquetes de glue derivados de los plugins descubiertos.
     *
     * <p>Agrega los paquetes de {@link CorePlugin#getGluePackages()} de cada plugin,
     * elimina duplicados y ordena el resultado. El {@link CucumberRuntimeEngine} usa
     * este método cuando {@code ExecutionRequest.getGluePaths()} está vacío.
     *
     * <p>El Backend puede invocar este método en {@code CoreEngineConfig} para obtener
     * los paquetes de steps automáticamente, sin mantener ninguna lista manual:
     * <pre>
     *   StepDiscoveryService discovery = StepDiscoveryService.withServiceLoader();
     *   List&lt;String&gt; glue = discovery.resolveGluePaths();
     *   // glue = ["com.qa.httpcore.steps", "com.qa.webcore.steps", ...]
     * </pre>
     *
     * @return lista inmutable de paquetes Java, sin duplicados y ordenada; nunca null
     * @since 2.2.0
     */
    public List<String> resolveGluePaths() {
        List<String> paths = plugins.stream().flatMap(p -> p.getGluePackages().stream()).distinct().sorted().
                collect(Collectors.toUnmodifiableList());
        LOG.debug("resolveGluePaths(): {} paquetes de {} plugins", paths.size(), plugins.size());
        return paths;
    }

    // =========================================================================
    // Nivel 2 — descubrimiento de steps individuales (métodos @Given/@When/@Then)
    // =========================================================================

    /**
     * Descubre todos los steps individuales de todos los plugins.
     *
     * <p>Para cada {@link ComponentInfo} retornado por {@link #discoverAll()}, el
     * {@link StepMethodScanner} escanea vía reflexión la clase indicada por
     * {@link StepComponent#getStepDefinitionClass()} y detecta todos los métodos
     * anotados con {@code @Given}, {@code @When} o {@code @Then}.
     *
     * <p>Los componentes cuyo {@code getStepDefinitionClass()} retorna {@code null}
     * (aún no implementados) se omiten silenciosamente.
     *
     * <p>Ejemplo de uso desde el Backend (endpoint {@code GET /api/steps/defs}):
     * <pre>
     *   List&lt;StepDefinitionInfo&gt; defs = discovery.discoverAllStepDefs();
     *   // serializar a JSON y devolver al Frontend para autocompletar el Scenario Builder
     * </pre>
     *
     * @return lista inmutable de {@link StepDefinitionInfo}; nunca null, puede estar vacía
     * @since 2.2.0
     */
    public List<StepDefinitionInfo> discoverAllStepDefs() {
        return stepMethodScanner.scanAll(discoverAll());
    }

    /**
     * Resuelve un step individual por su {@code stepDefId} estable.
     *
     * <p>El Backend usa este método para resolver el patrón y los parámetros de un step
     * a partir de su ID persistido, sin recorrer todos los plugins manualmente.
     *
     * <p>Si el {@code stepDefId} pertenece a un step marcado como
     * {@link com.qa.common.api.runtime.annotation.StepDef#deprecated() deprecated},
     * se emite una advertencia en el LOG indicando el ID de reemplazo si está declarado.
     *
     * @param stepDefId ID estable del step individual (ej: {@code "api.authentication.bearer.rut"})
     * @return {@link Optional} con el primer {@link StepDefinitionInfo} que coincida,
     *         o vacío si no existe
     * @since 2.2.0
     */
    public Optional<StepDefinitionInfo> resolveStepDef(String stepDefId) {
        Objects.requireNonNull(stepDefId, "stepDefId no puede ser null");
        Optional<StepDefinitionInfo> found = discoverAllStepDefs().stream().
                filter(sdi -> stepDefId.equals(sdi.stepDefId())).findFirst();

        found.ifPresent(sdi -> {
            if (sdi.deprecated()) {
                String replacement = sdi.hasReplacement()
                        ? "'" + sdi.replacementStepDefId() + "'"
                        : "(sin reemplazo declarado)";
                LOG.warn("StepDefId '{}' está marcado como DEPRECATED. Reemplazo: {}. "
                        + "Actualiza los escenarios que usen este ID.", stepDefId, replacement);
            }
        });

        if (found.isEmpty()) {
            LOG.debug("resolveStepDef('{}') → no encontrado en {} plugins", stepDefId, plugins.size());
        }
        return found;
    }

    /**
     * Descubre todos los steps individuales de un componente específico.
     *
     * <p>Permite al Backend obtener los steps de un componente por su {@code componentId}
     * sin recorrer el catálogo completo. Útil para el Scenario Builder cuando el usuario
     * selecciona un componente y se quiere expandir su lista de steps.
     *
     * @param componentId ID del componente padre (valor de su {@code @StepId})
     * @return lista inmutable de {@link StepDefinitionInfo} del componente; vacía si no hay
     * @since 2.2.0
     */
    public List<StepDefinitionInfo> discoverStepDefsByComponent(String componentId) {
        Objects.requireNonNull(componentId, "componentId no puede ser null");
        return discoverAllStepDefs().stream().filter(sdi -> componentId.equals(sdi.componentId())).
                collect(Collectors.toUnmodifiableList());
    }

    /**
     * Cantidad total de steps individuales descubiertos en todos los plugins.
     *
     * <p>Suma los steps de todos los componentes de todos los plugins. Equivale a
     * {@code discoverAllStepDefs().size()} pero puede usarse como métrica rápida.
     *
     * @return total de steps individuales
     * @since 2.2.0
     */
    public int totalStepDefs() {
        return discoverAllStepDefs().size();
    }

    // =========================================================================
    // API step-level v2.3.0 — contrato público para macros, lint e IA
    // =========================================================================

    /**
     * Descubre todos los steps individuales de todos los plugins como {@link StepDefinitionInfo}.
     *
     * <p>Esta es la API de catálogo de nivel step recomendada para consumo desde el Backend
     * (macros / CustomSteps, lint BDD, sugerencias IA). Cada {@link StepDefinitionInfo}
     * incluye:
     * <ul>
     *   <li>ID estable ({@link StepDefinitionInfo#stepDefId()}) — explícito via
     *       {@link com.qa.common.api.runtime.annotation.StepDef} o derivado.</li>
     *   <li>Patrón Cucumber Expression ({@link StepDefinitionInfo#cucumberPattern()}).</li>
     *   <li>Fase BDD ({@link StepDefinitionInfo#phase()}).</li>
     *   <li>Capa y componente padre ({@link StepDefinitionInfo#layer()},
     *       {@link StepDefinitionInfo#componentId()}).</li>
     *   <li>Vista semántica de parámetros ({@link StepDefinitionInfo#paramSchemas()})
     *       con tipos lógicos ({@code "string"}, {@code "number"}, {@code "boolean"},
     *       {@code "json"}, etc.).</li>
     *   <li>Mapas i18n propagados del componente padre
     *       ({@link StepDefinitionInfo#displayNameByLocale()},
     *       {@link StepDefinitionInfo#descriptionByLocale()}).</li>
     *   <li>Flags de deprecación ({@link StepDefinitionInfo#deprecated()},
     *       {@link StepDefinitionInfo#replacementStepDefId()}).</li>
     * </ul>
     *
     * <p>Ejemplo desde el Backend:
     * <pre>
     *   StepDiscoveryService discovery = StepDiscoveryService.withServiceLoader();
     *   List&lt;StepDefinitionInfo&gt; catalog = discovery.discoverAllSteps();
     *   // Serializar a JSON y exponer en GET /api/steps/defs
     * </pre>
     *
     * <p>Equivalente funcional a {@link #discoverAllStepDefs()}.
     *
     * @return lista inmutable de {@link StepDefinitionInfo}; nunca null, puede estar vacía
     * @since 2.3.0
     * @see #discoverAllStepDefs()
     * @see #findById(String)
     */
    public List<StepDefinitionInfo> discoverAllSteps() {
        return discoverAllStepDefs();
    }

    /**
     * Resuelve un step individual por su {@code stepId} estable.
     *
     * <p>API de resolución directa para el Backend. Permite mapear un {@code stepId}
     * persistido en la BD a su metadata completa sin recorrer el catálogo manualmente:
     * <pre>
     *   discovery.findById("api.authentication.bearer.identifier")
     *       .ifPresent(sdi -> {
     *           LOG.info("Patrón: {}", sdi.cucumberPattern());
     *           sdi.paramSchemas().forEach(p ->
     *               LOG.info("  {} : {}", p.name(), p.type()));
     *       });
     * </pre>
     *
     * <p>Si el step está deprecado, se emite una advertencia en el LOG con el ID de reemplazo.
     *
     * <p>Equivalente funcional a {@link #resolveStepDef(String)}.
     *
     * @param stepId ID estable del step individual (ej: {@code "api.authentication.bearer.identifier"})
     * @return {@link java.util.Optional} con el primer {@link StepDefinitionInfo} que coincida,
     *         o vacío si no existe
     * @throws NullPointerException si {@code stepId} es null
     * @since 2.3.0
     * @see #resolveStepDef(String)
     * @see StepDefinitionInfo#stepDefId()
     */
    public java.util.Optional<StepDefinitionInfo> findById(String stepId) {
        return resolveStepDef(stepId);
    }

    @Override
    public String toString() {
        return "StepDiscoveryService{plugins=" + plugins.size()
                + ", components=" + totalComponents() + "}";
    }

    /**
     * Metadata de un componente con contexto de su plugin padre.
     *
     * @param pluginName nombre del plugin que aporta el componente
     * @param component  componente de steps
     */
    public record ComponentInfo(
            String pluginName,
            StepComponent component
    ) {
        public ComponentInfo {
            Objects.requireNonNull(pluginName, "pluginName no puede ser null");
            Objects.requireNonNull(component, "component no puede ser null");
        }

        /**
         * Nombre completo: "plugin/component".
         * @return nombre cualificado
         */
        public String qualifiedName() {
            return pluginName + "/" + component.getName();
        }

        /**
         * Fase BDD del componente.
         * @return fase
         */
        public BddPhase phase() {
            return component.getPhase();
        }

        /**
         * Nombre de display del componente para el locale dado.
         *
         * <p>Busca en {@link StepComponent#getDisplayNameByLocale()}; si el locale no existe
         * o el mapa está vacío, retorna {@link StepComponent#getDisplayName()} como fallback.
         *
         * @param locale locale corto ("es", "en", "fr")
         * @return nombre localizado, nunca null
         */
        public String getDisplayNameForLocale(String locale) {
            Objects.requireNonNull(locale, "locale no puede ser null");
            Map<String, String> byLocale = component.getDisplayNameByLocale();
            if (byLocale != null && byLocale.containsKey(locale)) {
                return byLocale.get(locale);
            }
            return component.getDisplayName();
        }

        /**
         * Descripción del componente para el locale dado.
         *
         * <p>Busca en {@link StepComponent#getDescriptionByLocale()}; si el locale no existe
         * o el mapa está vacío, retorna {@link StepComponent#getDescription()} como fallback.
         *
         * @param locale locale corto ("es", "en", "fr")
         * @return descripción localizada, nunca null
         */
        public String getDescriptionForLocale(String locale) {
            Objects.requireNonNull(locale, "locale no puede ser null");
            Map<String, String> byLocale = component.getDescriptionByLocale();
            if (byLocale != null && byLocale.containsKey(locale)) {
                return byLocale.get(locale);
            }
            return component.getDescription();
        }
    }
}
