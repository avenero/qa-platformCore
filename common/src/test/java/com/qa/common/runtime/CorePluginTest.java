package com.qa.common.runtime;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // =========================================================================
    // getGluePackages() — nuevo default method @since 2.2.0
    // =========================================================================

    @Nested
    @DisplayName("getGluePackages() — derivacion automatica de paquetes SPI")
    class GetGluePackagesTests {

        @Test
        @DisplayName("Plugin sin componentes retorna lista vacia")
        void sinComponentesRetornaListaVacia() {
            CorePlugin plugin = new MinimalPlugin(); // getComponents() = []
            assertThat(plugin.getGluePackages()).isEmpty();
        }

        @Test
        @DisplayName("Plugin con un componente retorna el paquete de su step class")
        void conUnComponenteRetornaPaquete() {
            CorePlugin plugin = new FullPlugin();
            // FullPlugin.getComponents() tiene un componente con getStepDefinitionClass() = FullPlugin.class
            // FullPlugin.class está en com.qa.common.runtime (paquete del test)
            List<String> glue = plugin.getGluePackages();

            assertThat(glue).isNotEmpty();
            assertThat(glue).contains(FullPlugin.class.getPackageName());
        }

        @Test
        @DisplayName("Paquetes duplicados se deduplicn: dos componentes en el mismo paquete → un entry")
        void paquetesDuplicadosSeDeduplicaN() {
            CorePlugin plugin = new CorePlugin() {
                @Override public String getName() { return "dup-test"; }
                @Override public Set<String> getActivationTags() { return Set.of("@dup"); }
                @Override public void registerServices(ServiceRegistry r, ExecutionConfig c) {}
                @Override public void onScenarioStart(ExecutionContext ctx) {}
                @Override public void onScenarioEnd(ExecutionContext ctx) {}

                @Override
                public List<StepComponent> getComponents() {
                    // Dos componentes en el MISMO paquete
                    StepComponent comp1 = new StepComponent() {
                        @Override public String getName() { return "c1"; }
                        @Override public BddPhase getPhase() { return BddPhase.GIVEN; }
                        @Override public Class<?> getStepDefinitionClass() { return MinimalPlugin.class; }
                    };
                    StepComponent comp2 = new StepComponent() {
                        @Override public String getName() { return "c2"; }
                        @Override public BddPhase getPhase() { return BddPhase.WHEN; }
                        @Override public Class<?> getStepDefinitionClass() { return FullPlugin.class; }
                    };
                    return List.of(comp1, comp2); // ambas en com.qa.common.runtime
                }
            };

            List<String> glue = plugin.getGluePackages();

            assertThat(glue).hasSize(1);
            assertThat(glue).containsExactly(MinimalPlugin.class.getPackageName());
        }

        @Test
        @DisplayName("Componente con getStepDefinitionClass() == null se omite")
        void componenteConNullStepClassSeOmite() {
            CorePlugin plugin = new CorePlugin() {
                @Override public String getName() { return "null-test"; }
                @Override public Set<String> getActivationTags() { return Set.of("@null"); }
                @Override public void registerServices(ServiceRegistry r, ExecutionConfig c) {}
                @Override public void onScenarioStart(ExecutionContext ctx) {}
                @Override public void onScenarioEnd(ExecutionContext ctx) {}

                @Override
                public List<StepComponent> getComponents() {
                    return List.of(
                        new StepComponent() {
                            @Override public String getName() { return "pendiente"; }
                            @Override public BddPhase getPhase() { return BddPhase.WHEN; }
                            @Override public Class<?> getStepDefinitionClass() { return null; } // no impl aún
                        }
                    );
                }
            };

            assertThat(plugin.getGluePackages()).isEmpty();
        }

        @Test
        @DisplayName("Resultado es inmutable (List.of / toUnmodifiableList)")
        void resultadoEsInmutable() {
            CorePlugin plugin = new FullPlugin();
            List<String> glue = plugin.getGluePackages();

            assertThatThrownBy(() -> glue.add("com.qa.hacked"))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Resultado esta ordenado lexicograficamente")
        void resultadoEstaOrdenado() {
            CorePlugin plugin = new CorePlugin() {
                @Override public String getName() { return "order-test"; }
                @Override public Set<String> getActivationTags() { return Set.of("@order"); }
                @Override public void registerServices(ServiceRegistry r, ExecutionConfig c) {}
                @Override public void onScenarioStart(ExecutionContext ctx) {}
                @Override public void onScenarioEnd(ExecutionContext ctx) {}

                @Override
                public List<StepComponent> getComponents() {
                    // Dos paquetes distintos
                    StepComponent compZ = new StepComponent() {
                        @Override public String getName() { return "z"; }
                        @Override public BddPhase getPhase() { return BddPhase.WHEN; }
                        @Override public Class<?> getStepDefinitionClass() { return java.util.zip.ZipEntry.class; }
                    };
                    StepComponent compA = new StepComponent() {
                        @Override public String getName() { return "a"; }
                        @Override public BddPhase getPhase() { return BddPhase.GIVEN; }
                        @Override public Class<?> getStepDefinitionClass() { return java.awt.Color.class; }
                    };
                    return List.of(compZ, compA); // z va primero en lista, pero 'a' debe ir primero alfabéticamente
                }
            };

            List<String> glue = plugin.getGluePackages();
            assertThat(glue).isSortedAccordingTo(String::compareTo);
        }
    }
}

