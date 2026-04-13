package com.qa.common.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests unitarios para StepDiscoveryService.
 *
 * <p>Usa plugins stub inyectados directamente (sin SPI) para testear toda la
 * logica de descubrimiento, agrupacion y filtraje de componentes.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("StepDiscoveryService")
class StepDiscoveryServiceTest {

    // =========================================================================
    // Stubs reutilizables
    // =========================================================================

    private static StepComponent makeComponent(String name, BddPhase phase) {
        return new StepComponent() {
            @Override public String getName()                  { return name; }
            @Override public BddPhase getPhase()               { return phase; }
            @Override public Class<?> getStepDefinitionClass() { return Object.class; }
            @Override public String getId()                    { return name.toLowerCase().replace(" ", "."); }
        };
    }

    private static StepComponent makeI18nComponent(String name, BddPhase phase,
                                                     Map<String, String> displayNames,
                                                     Map<String, String> descriptions) {
        return new StepComponent() {
            @Override public String getName()                  { return name; }
            @Override public BddPhase getPhase()               { return phase; }
            @Override public Class<?> getStepDefinitionClass() { return Object.class; }
            @Override public String getId()                    { return name.toLowerCase().replace(" ", "."); }
            @Override public String getDisplayName()           { return name; }
            @Override public String getDescription()           { return "fallback-desc"; }
            @Override public Map<String, String> getDisplayNameByLocale()  { return displayNames; }
            @Override public Map<String, String> getDescriptionByLocale()  { return descriptions; }
        };
    }

    private static CorePlugin makePlugin(String pluginName, int order,
                                          Set<String> tags, List<StepComponent> components) {
        return new CorePlugin() {
            @Override public String getName()                { return pluginName; }
            @Override public Set<String> getActivationTags(){ return tags; }
            @Override public int getOrder()                  { return order; }
            @Override public void registerServices(ServiceRegistry r, ExecutionConfig c) {}
            @Override public void onScenarioStart(ExecutionContext ctx) {}
            @Override public void onScenarioEnd(ExecutionContext ctx)   {}
            @Override public List<StepComponent> getComponents()       { return components; }
        };
    }

    // =========================================================================

    @Nested
    @DisplayName("Constructor")
    class ConstructorTest {

        @Test
        @DisplayName("Lista null de plugins lanza NullPointerException")
        void testNullPluginsLanzaException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new StepDiscoveryService(null))
                    .withMessageContaining("plugins");
        }

        @Test
        @DisplayName("Lista vacia de plugins es valida")
        void testListaVaciaEsValida() {
            StepDiscoveryService svc = new StepDiscoveryService(List.of());
            assertThat(svc.discoverAll()).isEmpty();
            assertThat(svc.totalComponents()).isZero();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("discoverAll()")
    class DiscoverAllTest {

        @Test
        @DisplayName("Retorna todos los componentes de todos los plugins")
        void testDiscoverAllRetornaTodo() {
            CorePlugin api = makePlugin("api", 50, Set.of("@api"), List.of(
                    makeComponent("URL Config", BddPhase.GIVEN),
                    makeComponent("Execution", BddPhase.WHEN)
            ));
            CorePlugin web = makePlugin("web", 100, Set.of("@web"), List.of(
                    makeComponent("Navigation", BddPhase.WHEN)
            ));

            StepDiscoveryService svc = new StepDiscoveryService(List.of(api, web));
            List<StepDiscoveryService.ComponentInfo> all = svc.discoverAll();

            assertThat(all).hasSize(3);
        }

        @Test
        @DisplayName("Retorna lista inmutable")
        void testDiscoverAllEsInmutable() {
            StepDiscoveryService svc = new StepDiscoveryService(List.of(
                    makePlugin("api", 50, Set.of("@api"),
                            List.of(makeComponent("c1", BddPhase.GIVEN)))
            ));
            List<StepDiscoveryService.ComponentInfo> all = svc.discoverAll();
            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> all.add(null)
            );
        }

        @Test
        @DisplayName("ComponentInfo contiene pluginName y component correctos")
        void testComponentInfoContienePluginName() {
            CorePlugin api = makePlugin("api", 50, Set.of("@api"), List.of(
                    makeComponent("URL Config", BddPhase.GIVEN)
            ));
            StepDiscoveryService svc = new StepDiscoveryService(List.of(api));

            StepDiscoveryService.ComponentInfo info = svc.discoverAll().get(0);
            assertThat(info.pluginName()).isEqualTo("api");
            assertThat(info.component().getName()).isEqualTo("URL Config");
        }

        @Test
        @DisplayName("qualifiedName retorna 'plugin/componentName'")
        void testQualifiedName() {
            CorePlugin api = makePlugin("api", 50, Set.of("@api"), List.of(
                    makeComponent("URL Config", BddPhase.GIVEN)
            ));
            StepDiscoveryService svc = new StepDiscoveryService(List.of(api));
            assertThat(svc.discoverAll().get(0).qualifiedName()).isEqualTo("api/URL Config");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("discoverByPhase()")
    class DiscoverByPhaseTest {

        @Test
        @DisplayName("Filtra solo componentes GIVEN")
        void testFiltrarGiven() {
            CorePlugin p = makePlugin("api", 50, Set.of("@api"), List.of(
                    makeComponent("Setup",      BddPhase.GIVEN),
                    makeComponent("Execution",  BddPhase.WHEN),
                    makeComponent("Validation", BddPhase.THEN)
            ));
            StepDiscoveryService svc = new StepDiscoveryService(List.of(p));
            List<StepDiscoveryService.ComponentInfo> given = svc.discoverByPhase(BddPhase.GIVEN);

            assertThat(given).hasSize(1);
            assertThat(given.get(0).component().getName()).isEqualTo("Setup");
        }

        @Test
        @DisplayName("Retorna lista vacia si no hay componentes de la fase")
        void testFaseVacia() {
            CorePlugin p = makePlugin("api", 50, Set.of("@api"), List.of(
                    makeComponent("Execution", BddPhase.WHEN)
            ));
            StepDiscoveryService svc = new StepDiscoveryService(List.of(p));
            assertThat(svc.discoverByPhase(BddPhase.THEN)).isEmpty();
        }

        @Test
        @DisplayName("phase null lanza NullPointerException")
        void testFaseNullLanzaException() {
            StepDiscoveryService svc = new StepDiscoveryService(List.of());
            assertThatNullPointerException().isThrownBy(() -> svc.discoverByPhase(null));
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("discoverByPlugin()")
    class DiscoverByPluginTest {

        @Test
        @DisplayName("Retorna solo componentes del plugin especificado")
        void testFiltrarPorPlugin() {
            CorePlugin api = makePlugin("api", 50, Set.of("@api"), List.of(
                    makeComponent("URL Config", BddPhase.GIVEN)
            ));
            CorePlugin web = makePlugin("web", 100, Set.of("@web"), List.of(
                    makeComponent("Navigation", BddPhase.WHEN),
                    makeComponent("Click",      BddPhase.WHEN)
            ));
            StepDiscoveryService svc = new StepDiscoveryService(List.of(api, web));

            List<StepDiscoveryService.ComponentInfo> webComponents = svc.discoverByPlugin("web");
            assertThat(webComponents).hasSize(2);
            webComponents.forEach(c -> assertThat(c.pluginName()).isEqualTo("web"));
        }

        @Test
        @DisplayName("Plugin inexistente retorna lista vacia")
        void testPluginInexistente() {
            StepDiscoveryService svc = new StepDiscoveryService(List.of(
                    makePlugin("api", 50, Set.of("@api"), List.of(
                            makeComponent("c1", BddPhase.GIVEN)))
            ));
            assertThat(svc.discoverByPlugin("mobile")).isEmpty();
        }

        @Test
        @DisplayName("pluginName null lanza NullPointerException")
        void testPluginNullLanzaException() {
            StepDiscoveryService svc = new StepDiscoveryService(List.of());
            assertThatNullPointerException().isThrownBy(() -> svc.discoverByPlugin(null));
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("groupByPhase()")
    class GroupByPhaseTest {

        @Test
        @DisplayName("Agrupa correctamente por fase BDD")
        void testGroupByPhase() {
            CorePlugin p = makePlugin("api", 50, Set.of("@api"), List.of(
                    makeComponent("Setup",      BddPhase.GIVEN),
                    makeComponent("Execution",  BddPhase.WHEN),
                    makeComponent("Validation", BddPhase.THEN),
                    makeComponent("Validation2",BddPhase.THEN)
            ));
            StepDiscoveryService svc = new StepDiscoveryService(List.of(p));
            Map<BddPhase, List<StepDiscoveryService.ComponentInfo>> grouped = svc.groupByPhase();

            assertThat(grouped.get(BddPhase.GIVEN)).hasSize(1);
            assertThat(grouped.get(BddPhase.WHEN)).hasSize(1);
            assertThat(grouped.get(BddPhase.THEN)).hasSize(2);
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("groupByPlugin()")
    class GroupByPluginTest {

        @Test
        @DisplayName("Agrupa correctamente por nombre de plugin")
        void testGroupByPlugin() {
            CorePlugin api = makePlugin("api", 50, Set.of("@api"), List.of(
                    makeComponent("c1", BddPhase.GIVEN),
                    makeComponent("c2", BddPhase.WHEN)
            ));
            CorePlugin web = makePlugin("web", 100, Set.of("@web"), List.of(
                    makeComponent("c3", BddPhase.WHEN)
            ));
            StepDiscoveryService svc = new StepDiscoveryService(List.of(api, web));
            Map<String, List<StepDiscoveryService.ComponentInfo>> grouped = svc.groupByPlugin();

            assertThat(grouped).containsKeys("api", "web");
            assertThat(grouped.get("api")).hasSize(2);
            assertThat(grouped.get("web")).hasSize(1);
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("totalComponents() / getPluginNames()")
    class MetricasTest {

        @Test
        @DisplayName("totalComponents suma correctamente los componentes de todos los plugins")
        void testTotalComponents() {
            CorePlugin api = makePlugin("api", 50, Set.of("@api"), List.of(
                    makeComponent("c1", BddPhase.GIVEN),
                    makeComponent("c2", BddPhase.WHEN)
            ));
            CorePlugin web = makePlugin("web", 100, Set.of("@web"), List.of(
                    makeComponent("c3", BddPhase.THEN)
            ));
            StepDiscoveryService svc = new StepDiscoveryService(List.of(api, web));
            assertThat(svc.totalComponents()).isEqualTo(3);
        }

        @Test
        @DisplayName("getPluginNames retorna nombres en orden de insercion")
        void testGetPluginNames() {
            CorePlugin api = makePlugin("api", 50, Set.of("@api"), List.of());
            CorePlugin web = makePlugin("web", 100, Set.of("@web"), List.of());
            StepDiscoveryService svc = new StepDiscoveryService(List.of(api, web));

            assertThat(svc.getPluginNames()).containsExactly("api", "web");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("ComponentInfo — locale-aware helpers")
    class ComponentInfoLocaleTest {

        private final Map<String, String> displayNames = Map.of(
                "es", "Nombre ES",
                "en", "Name EN",
                "fr", "Nom FR"
        );
        private final Map<String, String> descriptions = Map.of(
                "es", "Descripcion ES",
                "en", "Description EN",
                "fr", "Description FR"
        );

        @Test
        @DisplayName("getDisplayNameForLocale retorna traduccion espanol")
        void testDisplayNameEs() {
            StepComponent comp = makeI18nComponent("Test", BddPhase.GIVEN, displayNames, descriptions);
            StepDiscoveryService.ComponentInfo info = new StepDiscoveryService.ComponentInfo("api", comp);
            assertThat(info.getDisplayNameForLocale("es")).isEqualTo("Nombre ES");
        }

        @Test
        @DisplayName("getDisplayNameForLocale retorna traduccion ingles")
        void testDisplayNameEn() {
            StepComponent comp = makeI18nComponent("Test", BddPhase.GIVEN, displayNames, descriptions);
            StepDiscoveryService.ComponentInfo info = new StepDiscoveryService.ComponentInfo("api", comp);
            assertThat(info.getDisplayNameForLocale("en")).isEqualTo("Name EN");
        }

        @Test
        @DisplayName("getDisplayNameForLocale retorna traduccion frances")
        void testDisplayNameFr() {
            StepComponent comp = makeI18nComponent("Test", BddPhase.GIVEN, displayNames, descriptions);
            StepDiscoveryService.ComponentInfo info = new StepDiscoveryService.ComponentInfo("api", comp);
            assertThat(info.getDisplayNameForLocale("fr")).isEqualTo("Nom FR");
        }

        @Test
        @DisplayName("getDisplayNameForLocale hace fallback a getDisplayName() si locale no existe")
        void testDisplayNameFallback() {
            StepComponent comp = makeI18nComponent("Test", BddPhase.GIVEN, displayNames, descriptions);
            StepDiscoveryService.ComponentInfo info = new StepDiscoveryService.ComponentInfo("api", comp);
            assertThat(info.getDisplayNameForLocale("de")).isEqualTo("Test");
        }

        @Test
        @DisplayName("getDisplayNameForLocale hace fallback a getDisplayName() si mapa vacio")
        void testDisplayNameFallbackMapaVacio() {
            StepComponent comp = makeComponent("Sin i18n", BddPhase.WHEN);
            StepDiscoveryService.ComponentInfo info = new StepDiscoveryService.ComponentInfo("web", comp);
            assertThat(info.getDisplayNameForLocale("en")).isEqualTo("Sin i18n");
        }

        @Test
        @DisplayName("getDescriptionForLocale retorna descripcion en espanol")
        void testDescriptionEs() {
            StepComponent comp = makeI18nComponent("Test", BddPhase.THEN, displayNames, descriptions);
            StepDiscoveryService.ComponentInfo info = new StepDiscoveryService.ComponentInfo("db", comp);
            assertThat(info.getDescriptionForLocale("es")).isEqualTo("Descripcion ES");
        }

        @Test
        @DisplayName("getDescriptionForLocale retorna descripcion en ingles")
        void testDescriptionEn() {
            StepComponent comp = makeI18nComponent("Test", BddPhase.THEN, displayNames, descriptions);
            StepDiscoveryService.ComponentInfo info = new StepDiscoveryService.ComponentInfo("db", comp);
            assertThat(info.getDescriptionForLocale("en")).isEqualTo("Description EN");
        }

        @Test
        @DisplayName("getDescriptionForLocale hace fallback a getDescription() si locale ausente")
        void testDescriptionFallback() {
            StepComponent comp = makeI18nComponent("Test", BddPhase.THEN, displayNames, descriptions);
            StepDiscoveryService.ComponentInfo info = new StepDiscoveryService.ComponentInfo("db", comp);
            assertThat(info.getDescriptionForLocale("pt")).isEqualTo("fallback-desc");
        }

        @Test
        @DisplayName("getDescriptionForLocale hace fallback si componente no tiene i18n")
        void testDescriptionFallbackSinI18n() {
            StepComponent comp = makeComponent("Sin descripcion", BddPhase.GIVEN);
            StepDiscoveryService.ComponentInfo info = new StepDiscoveryService.ComponentInfo("api", comp);
            String result = info.getDescriptionForLocale("en");
            assertThat(result).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("getDisplayNameForLocale con locale null lanza NullPointerException")
        void testDisplayNameLocaleNullLanzaException() {
            StepComponent comp = makeComponent("Test", BddPhase.GIVEN);
            StepDiscoveryService.ComponentInfo info = new StepDiscoveryService.ComponentInfo("api", comp);
            assertThatNullPointerException().isThrownBy(() -> info.getDisplayNameForLocale(null));
        }

        @Test
        @DisplayName("getDescriptionForLocale con locale null lanza NullPointerException")
        void testDescriptionLocaleNullLanzaException() {
            StepComponent comp = makeComponent("Test", BddPhase.GIVEN);
            StepDiscoveryService.ComponentInfo info = new StepDiscoveryService.ComponentInfo("api", comp);
            assertThatNullPointerException().isThrownBy(() -> info.getDescriptionForLocale(null));
        }
    }
}
