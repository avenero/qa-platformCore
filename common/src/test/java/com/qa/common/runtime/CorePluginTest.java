package com.qa.common.runtime;



import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.internal.runtime.ExecutionContext;
import com.qa.common.internal.runtime.ServiceRegistry;
import com.qa.common.spi.CorePlugin;
import com.qa.common.api.driver.CapabilityDescriptor;
import com.qa.common.api.driver.CapabilityReport;

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
 * @since 3.0.0 — cubre platformId, displayName, describeCapabilities, getHookOrder, onSuiteStart/End
 */
@DisplayName("CorePlugin")
class CorePluginTest {

    /**
     * Plugin minimo para testing — implementa los tres métodos abstract obligatorios de v3.0.0.
     */
    @SuppressWarnings("deprecation")
    static class MinimalPlugin implements CorePlugin {

        @Override
        public String platformId() { return "TEST"; }

        @Override
        public String displayName() { return "Test Plugin"; }

        @Override
        public CapabilityReport describeCapabilities() {
            return CapabilityReport.available("TEST", List.of());
        }

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
     * Plugin con metodos default sobrescritos, incluidos los de v3.0.0.
     */
    @SuppressWarnings("deprecation")
    static class FullPlugin implements CorePlugin {

        @Override
        public String platformId() { return "HTTP"; }

        @Override
        public String displayName() { return "HTTP API Testing"; }

        @Override
        public CapabilityReport describeCapabilities() {
            return CapabilityReport.available("HTTP", List.of(
                new CapabilityDescriptor("rest", "REST", "HTTP/HTTPS endpoints")
            ));
        }

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

    // =========================================================================
    // Identidad (v3.0.0)
    // =========================================================================

    @Nested
    @DisplayName("Identidad (v3.0.0)")
    class IdentidadTests {

        @Test
        @DisplayName("platformId retorna identificador de plataforma en mayusculas")
        void platformIdRetornaIdentificador() {
            CorePlugin plugin = new MinimalPlugin();
            assertThat(plugin.platformId()).isEqualTo("TEST");
        }

        @Test
        @DisplayName("displayName retorna nombre legible")
        void displayNameRetornaNombreLegible() {
            CorePlugin plugin = new MinimalPlugin();
            assertThat(plugin.displayName()).isEqualTo("Test Plugin");
        }

        @Test
        @SuppressWarnings("deprecation")
        @DisplayName("getName (deprecated) delega a platformId en minusculas")
        void getNombreDeprecadoDelegaAPlatformId() {
            CorePlugin plugin = new MinimalPlugin();
            // MinimalPlugin no sobreescribe getName() → usa el default
            assertThat(plugin.getName()).isEqualTo("test");
        }

        @Test
        @DisplayName("version retorna 2.1.0 por defecto")
        void versionRetornaValorDefault() {
            CorePlugin plugin = new MinimalPlugin();
            assertThat(plugin.version()).isEqualTo("2.1.0");
        }
    }

    // =========================================================================
    // describeCapabilities (v3.0.0)
    // =========================================================================

    @Nested
    @DisplayName("describeCapabilities (v3.0.0)")
    class DescribeCapabilitiesTests {

        @Test
        @DisplayName("Plugin minimal retorna informe available sin opciones")
        void minimalRetornaAvailableSinOpciones() {
            CapabilityReport report = new MinimalPlugin().describeCapabilities();
            assertThat(report).isNotNull();
            assertThat(report.available()).isTrue();
            assertThat(report.platformId()).isEqualTo("TEST");
            assertThat(report.options()).isEmpty();
            assertThat(report.unavailableReason()).isNull();
        }

        @Test
        @DisplayName("Plugin full retorna informe available con opciones")
        void fullPluginRetornaAvailableConOpciones() {
            CapabilityReport report = new FullPlugin().describeCapabilities();
            assertThat(report.available()).isTrue();
            assertThat(report.platformId()).isEqualTo("HTTP");
            assertThat(report.options()).hasSize(1);
            assertThat(report.options().get(0).id()).isEqualTo("rest");
        }

        @Test
        @DisplayName("describeCapabilities es inmutable — la lista no puede ser mutada")
        void reportOpcionesEsInmutable() {
            CapabilityReport report = new FullPlugin().describeCapabilities();
            assertThatThrownBy(() -> report.options().add(
                new CapabilityDescriptor("hack", "Hack", "...")))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // =========================================================================
    // Metodos obligatorios existentes
    // =========================================================================

    @Nested
    @DisplayName("Metodos obligatorios existentes")
    class MetodosObligatoriosTests {

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

    // =========================================================================
    // Metodos default existentes
    // =========================================================================

    @Nested
    @DisplayName("Metodos default existentes")
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

        @Test
        @DisplayName("getHookOrder delega a getOrder por defecto")
        void getHookOrderDelegaAGetOrder() {
            CorePlugin plugin = new MinimalPlugin();
            assertThat(plugin.getHookOrder()).isEqualTo(plugin.getOrder());
        }

        @Test
        @DisplayName("getHookOrder refleja getOrder sobrescrito")
        void getHookOrderReflejaGetOrderSobrescrito() {
            CorePlugin plugin = new FullPlugin(); // getOrder() = 50
            assertThat(plugin.getHookOrder()).isEqualTo(50);
        }

        @Test
        @DisplayName("onSuiteStart no lanza excepcion con config valida")
        void onSuiteStartNoLanzaExcepcion() {
            CorePlugin plugin = new MinimalPlugin();
            ExecutionConfig config = new ExecutionConfig.Builder().build();
            plugin.onSuiteStart(config); // debe ser no-op sin excepción
        }

        @Test
        @DisplayName("onSuiteEnd no lanza excepcion")
        void onSuiteEndNoLanzaExcepcion() {
            CorePlugin plugin = new MinimalPlugin();
            plugin.onSuiteEnd(); // debe ser no-op sin excepción
        }
    }

    // =========================================================================
    // Metodos default sobrescritos
    // =========================================================================

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
    // getGluePackages() — @since 2.2.0
    // =========================================================================

    @Nested
    @DisplayName("getGluePackages() — derivacion automatica de paquetes SPI")
    class GetGluePackagesTests {

        @Test
        @DisplayName("Plugin sin componentes retorna lista vacia")
        void sinComponentesRetornaListaVacia() {
            CorePlugin plugin = new MinimalPlugin();
            assertThat(plugin.getGluePackages()).isEmpty();
        }

        @Test
        @DisplayName("Plugin con un componente retorna el paquete de su step class")
        void conUnComponenteRetornaPaquete() {
            CorePlugin plugin = new FullPlugin();
            List<String> glue = plugin.getGluePackages();
            assertThat(glue).isNotEmpty();
            assertThat(glue).contains(FullPlugin.class.getPackageName());
        }

        @Test
        @DisplayName("Paquetes duplicados se deduplicaN: dos componentes en el mismo paquete → un entry")
        void paquetesDuplicadosSeDeduplicaN() {
            CorePlugin plugin = new CorePlugin() {
                @Override public String platformId() { return "DUP"; }
                @Override public String displayName() { return "Dup Test"; }
                @Override public CapabilityReport describeCapabilities() {
                    return CapabilityReport.available("DUP", List.of());
                }
                @Override public Set<String> getActivationTags() { return Set.of("@dup"); }
                @Override public void registerServices(ServiceRegistry r, ExecutionConfig c) {}
                @Override public void onScenarioStart(ExecutionContext ctx) {}
                @Override public void onScenarioEnd(ExecutionContext ctx) {}

                @Override
                public List<StepComponent> getComponents() {
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
                @Override public String platformId() { return "NULL"; }
                @Override public String displayName() { return "Null Test"; }
                @Override public CapabilityReport describeCapabilities() {
                    return CapabilityReport.available("NULL", List.of());
                }
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
                            @Override public Class<?> getStepDefinitionClass() { return null; }
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
                @Override public String platformId() { return "ORDER"; }
                @Override public String displayName() { return "Order Test"; }
                @Override public CapabilityReport describeCapabilities() {
                    return CapabilityReport.available("ORDER", List.of());
                }
                @Override public Set<String> getActivationTags() { return Set.of("@order"); }
                @Override public void registerServices(ServiceRegistry r, ExecutionConfig c) {}
                @Override public void onScenarioStart(ExecutionContext ctx) {}
                @Override public void onScenarioEnd(ExecutionContext ctx) {}

                @Override
                public List<StepComponent> getComponents() {
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
                    return List.of(compZ, compA);
                }
            };

            List<String> glue = plugin.getGluePackages();
            assertThat(glue).isSortedAccordingTo(String::compareTo);
        }
    }
}
