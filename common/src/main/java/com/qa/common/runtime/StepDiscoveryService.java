package com.qa.common.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Servicio de descubrimiento de steps agrupados por componente.
 *
 * <p>Escanea todos los {@link CorePlugin} registrados y extrae sus
 * {@link StepComponent}s, proporcionando metadata estructurada para:
 * <ul>
 *   <li>El Backend: endpoint {@code GET /api/steps} que expone componentes disponibles</li>
 *   <li>El Frontend: paleta visual de componentes BDD para el Scenario Builder</li>
 *   <li>El Engine: glue paths necesarios para una ejecucion</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.0.0
 * @see CorePlugin#getComponents()
 * @see StepComponent
 */
public final class StepDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(StepDiscoveryService.class);

    private final List<CorePlugin> plugins;

    /**
     * Constructor con plugins explicitamente proporcionados (para testing).
     * @param plugins lista de plugins, no null
     */
    public StepDiscoveryService(List<CorePlugin> plugins) {
        Objects.requireNonNull(plugins, "plugins no puede ser null");
        this.plugins = List.copyOf(plugins);
        log.debug("StepDiscoveryService inicializado con {} plugins", this.plugins.size());
    }

    /**
     * Crea un StepDiscoveryService descubriendo plugins via Java SPI.
     * @return nueva instancia con plugins descubiertos
     */
    public static StepDiscoveryService withServiceLoader() {
        List<CorePlugin> discovered = StreamSupport
                .stream(ServiceLoader.load(CorePlugin.class).spliterator(), false)
                .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
                .collect(Collectors.toList());
        log.info("Plugins descubiertos via SPI: {}", discovered.size());
        discovered.forEach(p -> log.info("  Plugin: {} (order={})", p.getName(), p.getOrder()));
        return new StepDiscoveryService(discovered);
    }

    /**
     * Descubre todos los componentes de todos los plugins.
     *
     * @return lista de {@link ComponentInfo} con metadata de plugin + componente
     */
    public List<ComponentInfo> discoverAll() {
        return plugins.stream()
                .flatMap(plugin -> plugin.getComponents().stream()
                        .map(component -> new ComponentInfo(plugin.getName(), component)))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Descubre componentes filtrados por fase BDD.
     *
     * @param phase fase BDD (GIVEN, WHEN, THEN)
     * @return componentes de la fase indicada
     */
    public List<ComponentInfo> discoverByPhase(BddPhase phase) {
        Objects.requireNonNull(phase, "phase no puede ser null");
        return discoverAll().stream()
                .filter(info -> info.component().getPhase() == phase)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Descubre componentes de un plugin especifico.
     *
     * @param pluginName nombre del plugin (ej: "api", "web")
     * @return componentes del plugin
     */
    public List<ComponentInfo> discoverByPlugin(String pluginName) {
        Objects.requireNonNull(pluginName, "pluginName no puede ser null");
        return plugins.stream()
                .filter(p -> p.getName().equals(pluginName))
                .flatMap(p -> p.getComponents().stream()
                        .map(c -> new ComponentInfo(p.getName(), c)))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Agrupa todos los componentes por fase BDD.
     *
     * @return mapa: fase BDD → lista de componentes
     */
    public Map<BddPhase, List<ComponentInfo>> groupByPhase() {
        return discoverAll().stream()
                .collect(Collectors.groupingBy(
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
        return discoverAll().stream()
                .collect(Collectors.groupingBy(
                        ComponentInfo::pluginName,
                        Collectors.toUnmodifiableList()
                ));
    }

    /**
     * Obtiene los nombres de plugins registrados.
     * @return lista de nombres ordenados por prioridad
     */
    public List<String> getPluginNames() {
        return plugins.stream()
                .map(CorePlugin::getName)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Cantidad total de componentes descubiertos.
     * @return total de componentes
     */
    public int totalComponents() {
        return plugins.stream()
                .mapToInt(p -> p.getComponents().size())
                .sum();
    }

    /**
     * Exporta todos los componentes como {@link StepInfo}, el DTO que consume el Backend.
     *
     * <p>Cada {@link StepInfo} incluye los mapas i18n copiados desde el componente,
     * listos para serializar en {@code GET /api/steps} sin procesamiento adicional.
     *
     * @return lista inmutable de StepInfo; uno por componente registrado
     */
    public List<StepInfo> discoverAllAsStepInfo() {
        return discoverAll().stream()
                .map(info -> {
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
                            c.getDescriptionByLocale()
                    );
                })
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Obtiene la lista de plugins registrados.
     * @return lista inmutable de plugins
     */
    public List<CorePlugin> getPlugins() {
        return plugins;
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

