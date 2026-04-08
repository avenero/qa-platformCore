package com.qa.common.runtime;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para {@link CorePlugin}.
 * Verifica contrato de la interfaz y comportamiento de metodos default.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("CorePlugin")
class CorePluginTest {

    /**
     * Plugin minimo para testing (solo metodos obligatorios).
     */
    static class MinimalPlugin implements CorePlugin {
        @Override
        public String getName() { return "minimal-plugin"; }

        @Override
        public Set<String> getActivationTags() { return Set.of("@minimal"); }

        @Override
        public void registerServices(ServiceRegistry registry, ExecutionConfig config) {
            registry.registerInstance(String.class, "minimal-service");
        }

        @Override
        public void onScenarioStart(ExecutionContext context) { }

        @Override
        public void onScenarioEnd(ExecutionContext context) { }
    }

    /**
     * Plugin con metodos default sobrescritos.
     */
    static class FullPlugin implements CorePlugin {
        @Override
        public String getName() { return "full-plugin"; }

        @Override
        public Set<String> getActivationTags() { return Set.of("@api", "@rest"); }

        @Override
        public void registerServices(ServiceRegistry registry, ExecutionConfig config) { }

        @Override
        public void onScenarioStart(ExecutionContext context) { }

        @Override
        public void onScenarioEnd(ExecutionContext context) { }

        @Override
        public List<StepComponent> getComponents() {
            return List.of(
                    new StepComponent() {
                        @Override public String getName() { return "HTTP Request"; }
                        @Override public BddPhase getPhase() { return BddPhase.WHEN; }
                        @Override public Class<?> getStepDefinitionClass() { return FullPlugin.class; }
                    }
            );
        }

        @Override
        public int getOrder() { return 50; }
    }

    @Nested
    @DisplayName("Metodos obligatorios")
    class MetodosObligatoriosTests {

        @Test
        @DisplayName("getName retorna nombre del plugin")
        void getNameRetornaNombre() {
            CorePlugin plugin = new MinimalPlugin();
            assertThat(plugin.getName()).isEqualTo("minimal-plugin");
        }

        @Test
        @DisplayName("getActivationTags retorna tags configurados")
        void getActivationTagsRetornaTags() {
            CorePlugin plugin = new MinimalPlugin();
            assertThat(plugin.getActivationTags()).containsExactly("@minimal");
        }

        @Test
        @DisplayName("registerServices registra servicios en el registry")
        void registerServicesRegistraServicios() {
            CorePlugin plugin = new MinimalPlugin();
            ServiceRegistry registry = new ServiceRegistry();
            ExecutionConfig config = new ExecutionConfig.Builder().build();

            plugin.registerServices(registry, config);

            assertThat(registry.isRegistered(String.class)).isTrue();
            assertThat(registry.require(String.class)).isEqualTo("minimal-service");
        }
    }

    @Nested
    @DisplayName("Metodos default")
    class MetodosDefaultTests {

        @Test
        @DisplayName("getComponents retorna lista vacia por defecto")
        void getComponentsRetornaListaVaciaPorDefecto() {
            CorePlugin plugin = new MinimalPlugin();
            assertThat(plugin.getComponents()).isEmpty();
        }

        @Test
        @DisplayName("getOrder retorna 100 por defecto")
        void getOrderRetorna100PorDefecto() {
            CorePlugin plugin = new MinimalPlugin();
            assertThat(plugin.getOrder()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Metodos default sobrescritos")
    class MetodosSobrescritosTests {

        @Test
        @DisplayName("getComponents retorna componentes personalizados")
        void getComponentsRetornaComponentesPersonalizados() {
            CorePlugin plugin = new FullPlugin();
            List<StepComponent> components = plugin.getComponents();
            assertThat(components).hasSize(1);
            assertThat(components.get(0).getName()).isEqualTo("HTTP Request");
            assertThat(components.get(0).getPhase()).isEqualTo(BddPhase.WHEN);
        }

        @Test
        @DisplayName("getOrder retorna valor personalizado")
        void getOrderRetornaValorPersonalizado() {
            CorePlugin plugin = new FullPlugin();
            assertThat(plugin.getOrder()).isEqualTo(50);
        }

        @Test
        @DisplayName("Plugin con multiples activation tags")
        void pluginConMultiplesActivationTags() {
            CorePlugin plugin = new FullPlugin();
            assertThat(plugin.getActivationTags()).containsExactlyInAnyOrder("@api", "@rest");
        }
    }
}

