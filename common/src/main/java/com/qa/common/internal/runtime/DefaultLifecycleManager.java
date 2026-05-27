package com.qa.common.internal.runtime;




import com.qa.common.api.Internal;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.api.runtime.ServiceRegistry;
import com.qa.common.spi.CorePlugin;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.ExecutionRequest;
import com.qa.common.api.runtime.LifecycleManager;
import com.qa.common.api.runtime.ScenarioMetadata;
import com.qa.common.api.runtime.VariableStore;

import com.qa.common.api.runtime.events.EventBus;
import com.qa.common.api.runtime.events.ExecutionEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementacion por defecto de {@link LifecycleManager}.
 *
 * <p>Coordina el ciclo de vida de la ejecucion BDD:
 * <ul>
 *   <li>Crea el {@link ExecutionContext} con sus componentes</li>
 *   <li>Filtra plugins activos segun tags de la ejecucion</li>
 *   <li>Invoca {@code onScenarioStart/End} en orden de prioridad</li>
 *   <li>Gestiona limpieza al final</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@Internal(reason = "internal — usar api.runtime.LifecycleManager (contrato) inyectado por SPI")
public final class DefaultLifecycleManager implements LifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultLifecycleManager.class);

    private final List<CorePlugin> plugins;

    /**
     * Constructor con plugins ordenados por prioridad.
     * @param plugins lista de plugins (se ordenan internamente por {@link CorePlugin#getOrder()})
     */
    public DefaultLifecycleManager(List<CorePlugin> plugins) {
        Objects.requireNonNull(plugins, "plugins no puede ser null");
        this.plugins = plugins.stream()
                .sorted(Comparator.comparingInt(CorePlugin::getHookOrder))
                .collect(Collectors.toUnmodifiableList());
        LOG.debug("DefaultLifecycleManager creado con {} plugins: {}",
                this.plugins.size(),
                this.plugins.stream().map(CorePlugin::platformId).collect(Collectors.joining(", ")));
    }

    @Override
    public ExecutionContext initialize(ExecutionRequest request) {
        Objects.requireNonNull(request, "request no puede ser null");

        ExecutionConfig config = request.getConfig();
        ServiceRegistry registry = new ServiceRegistry();
        VariableStore variables = new VariableStore();
        EventBus eventBus = new EventBus();

        // Registrar servicios de todos los plugins activos
        List<CorePlugin> activePlugins = resolveActivePlugins(config.getTags());
        LOG.info("Plugins activos para ejecucion: {}",
                activePlugins.stream().map(CorePlugin::platformId).collect(Collectors.joining(", ")));

        for (CorePlugin plugin : activePlugins) {
            LOG.debug("Registrando servicios del plugin: {}", plugin.platformId());
            plugin.registerServices(registry, config);
        }

        ExecutionContext context = new ExecutionContext(config, registry, variables, eventBus);
        context.activate();

        // Publicar evento de inicio
        eventBus.publish(ExecutionEvent.of(
                ExecutionEvent.Type.EXECUTION_START,
                "Ejecucion inicializada con " + activePlugins.size() + " plugins"));

        return context;
    }

    @Override
    public void onScenarioStart(ExecutionContext context, ScenarioMetadata scenarioMetadata) {
        Objects.requireNonNull(context, "context no puede ser null");
        Objects.requireNonNull(scenarioMetadata, "scenarioMetadata no puede ser null");

        List<CorePlugin> activePlugins = resolveActivePluginsFromTags(scenarioMetadata.tags());

        // Advertir si el escenario tiene tags de capa pero ningún plugin los reconoce
        validateScenarioLayerTags(scenarioMetadata.tags(), activePlugins);

        LOG.debug("onScenarioStart: '{}' → {} plugin(s) activos",
                scenarioMetadata.name(), activePlugins.size());

        for (CorePlugin plugin : activePlugins) {
            try {
                plugin.onScenarioStart(context);
            } catch (Exception e) {
                LOG.error("Error en onScenarioStart del plugin [{}]: {}", plugin.platformId(), e.getMessage(), e);
            }
        }

        context.eventBus().publish(ExecutionEvent.of(
                ExecutionEvent.Type.SCENARIO_START,
                scenarioMetadata.name(),
                Map.of(
                        "tags", scenarioMetadata.tags(),
                        "uri",  scenarioMetadata.uri(),
                        "line", scenarioMetadata.line())));
    }

    @Override
    public void onScenarioEnd(ExecutionContext context, ScenarioMetadata scenarioMetadata) {
        Objects.requireNonNull(context, "context no puede ser null");
        Objects.requireNonNull(scenarioMetadata, "scenarioMetadata no puede ser null");

        List<CorePlugin> activePlugins = resolveActivePluginsFromTags(scenarioMetadata.tags());
        // Invocar en orden inverso (LIFO) para limpieza simétrica
        List<CorePlugin> reversed = new ArrayList<>(activePlugins);
        Collections.reverse(reversed);

        LOG.debug("onScenarioEnd: '{}' → {} plugin(s) activos (orden inverso)",
                scenarioMetadata.name(), reversed.size());

        for (CorePlugin plugin : reversed) {
            try {
                plugin.onScenarioEnd(context);
            } catch (Exception e) {
                LOG.error("Error en onScenarioEnd del plugin [{}]: {}", plugin.platformId(), e.getMessage(), e);
            }
        }

        context.eventBus().publish(ExecutionEvent.of(
                ExecutionEvent.Type.SCENARIO_END,
                scenarioMetadata.name(),
                Map.of(
                        "tags", scenarioMetadata.tags(),
                        "uri",  scenarioMetadata.uri(),
                        "line", scenarioMetadata.line())));
    }

    @Override
    public void shutdown(ExecutionContext context) {
        if (context == null) {
            return;
        }

        LOG.debug("Shutdown de ejecucion");
        context.eventBus().publish(ExecutionEvent.of(
                ExecutionEvent.Type.EXECUTION_END,
                "Ejecucion finalizada"));
        context.cleanup();
    }

    /**
     * Filtra plugins activos basandose en los tags de la configuracion.
     * Un plugin se activa si al menos uno de sus activation tags coincide.
     * Si tags es vacio, se activan todos los plugins.
     *
     * @param tags string de tags (ej: "@api and @smoke")
     * @return plugins activos ordenados por prioridad
     */
    List<CorePlugin> resolveActivePlugins(String tags) {
        if (tags == null || tags.isBlank()) {
            return plugins; // Sin filtro → todos activos
        }

        // Extraer tags individuales del string de tags
        Set<String> requestedTags = extractTags(tags);
        if (requestedTags.isEmpty()) {
            return plugins;
        }

        return plugins.stream().filter(plugin -> {
                    Set<String> activationTags = plugin.getActivationTags();
                    return activationTags.stream().anyMatch(requestedTags::contains);
                }).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Filtra plugins activos basandose en tags de escenario individual.
     */
    private List<CorePlugin> resolveActivePluginsFromTags(Collection<String> scenarioTags) {
        if (scenarioTags == null || scenarioTags.isEmpty()) {
            return plugins;
        }

        Set<String> tags = Set.copyOf(scenarioTags);
        return plugins.stream().filter(plugin -> plugin.getActivationTags().stream().anyMatch(tags::contains)).
                collect(Collectors.toUnmodifiableList());
    }

    /**
     * Extrae tags individuales de un string de expresion de tags Cucumber.
     * Ejemplo: "@api and @smoke" → {"@api", "@smoke"}
     */
    private Set<String> extractTags(String tagExpression) {
        if (tagExpression == null || tagExpression.isBlank()) {
            return Set.of();
        }
        // Extraer palabras que empiezan con @
        return java.util.regex.Pattern.compile("@\\w+").matcher(tagExpression).results().map(m -> m.group()).
                collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Retorna los plugins registrados.
     * @return lista inmutable de plugins
     */
    public List<CorePlugin> getPlugins() {
        return plugins;
    }

    /**
     * Retorna todos los tags de activación conocidos en todos los plugins registrados.
     *
     * <p>Útil para validación externa y para evitar duplicar la lista de tags en otras clases.</p>
     *
     * @return conjunto inmutable de tags (con prefijo @)
     */
    public Set<String> getKnownLayerTags() {
        return plugins.stream().flatMap(p -> p.getActivationTags().stream()).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Valida que los tags del escenario coincidan con al menos un plugin registrado.
     *
     * <p>Si el escenario tiene tags que parecen de capa (coinciden con tags conocidos)
     * pero ningún plugin se activó, se emite un warning para detectar typos o plugins
     * no registrados.</p>
     *
     * @param scenarioTags  tags del escenario
     * @param activePlugins plugins efectivamente activados para este escenario
     */
    private void validateScenarioLayerTags(Collection<String> scenarioTags,
                                           List<CorePlugin> activePlugins) {
        if (scenarioTags.isEmpty() || !activePlugins.isEmpty()) {
            return; // Sin tags → no aplica. Con plugins activos → todo correcto.
        }

        // Hay tags pero ningún plugin activó → detectar si son tags de capa mal escritos
        Set<String> knownTags = getKnownLayerTags();
        Set<String> matchedLayerTags = scenarioTags.stream().filter(t -> knownTags.contains(t.toLowerCase())).
                collect(Collectors.toUnmodifiableSet());

        if (!matchedLayerTags.isEmpty()) {
            LOG.warn("⚠️ Escenario tiene tags de capa {} pero ningún plugin se activó. " +
                     "Verificar que el plugin correspondiente esté registrado. " +
                     "Tags conocidos: {}", matchedLayerTags, knownTags);
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("ℹ️ Escenario con tags {} no coincide con ningún plugin de capa. " +
                      "Tags de capa conocidos: {}", scenarioTags, knownTags);
        }
    }
}

