package com.qa.common.internal.runtime;



import com.qa.common.api.driver.CapabilityReport;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.ExecutionRequest;
import com.qa.common.api.runtime.LifecycleManager;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.internal.runtime.CucumberRuntimeEngine;
import com.qa.common.internal.runtime.DefaultLifecycleManager;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.api.runtime.ServiceRegistry;
import com.qa.common.internal.runtime.StepDiscoveryService;
import com.qa.common.spi.CorePlugin;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para la resolución de glue paths en {@link CucumberRuntimeEngine}.
 *
 * <p><b>Cobertura:</b>
 * <ul>
 *   <li>{@code resolveGluePaths()} — auto-derivación desde plugins cuando request no tiene glue</li>
 *   <li>Prioridad: gluePaths explícitos del request sobrescriben la auto-derivación</li>
 *   <li>Deduplicación: dos plugins con el mismo paquete → un único entry en glue</li>
 *   <li>Plugin sin componentes → no aporta glue paths</li>
 *   <li>{@link StepDiscoveryService#resolveGluePaths()} — API pública del mismo comportamiento</li>
 * </ul>
 *
 * <p><b>Estrategia:</b> stubs manuales de {@link CorePlugin} y {@link StepComponent}.
 * No se usa Mockito ni navegador real.
 *
 * @author Abel Venero
 * @since 2.2.0
 */
@DisplayName("CucumberRuntimeEngine — resolución de glue paths (SPI)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CucumberRuntimeEngineGlueTest {

    // =========================================================================
    // Stubs de apoyo
    // =========================================================================

    /** Paquete "A" conocido — usa una clase de JDK para evitar dependencias externas. */
    private static final String PKG_A = java.util.ArrayList.class.getPackageName(); // java.util
    /** Paquete "B" conocido — diferente a PKG_A. */
    private static final String PKG_B = java.time.Duration.class.getPackageName();  // java.time

    /** Componente stub que apunta a un paquete conocido. */
    static StepComponent stubComponent(String name, Class<?> stepClass) {
        return new StepComponent() {
            @Override public String getName()              { return name; }
            @Override public BddPhase getPhase()          { return BddPhase.WHEN; }
            @Override public Class<?> getStepDefinitionClass() { return stepClass; }
        };
    }

    /** Plugin stub con componentes en un paquete conocido. */
    static CorePlugin stubPlugin(String pluginName, List<StepComponent> components) {
        return new CorePlugin() {
            @Override public String platformId()                 { return pluginName.toUpperCase(); }
            @Override public String displayName()                { return pluginName + " Plugin"; }
            @Override public com.qa.common.api.driver.CapabilityReport describeCapabilities() {
                return com.qa.common.api.driver.CapabilityReport.available(pluginName.toUpperCase(), List.of());
            }
            @Override public Set<String> getActivationTags()    { return Set.of("@" + pluginName); }
            @Override public void registerServices(ServiceRegistry r, ExecutionConfig c) {}
            @Override public void onScenarioStart(ExecutionContext ctx) {}
            @Override public void onScenarioEnd(ExecutionContext ctx)   {}
            @Override public List<StepComponent> getComponents()       { return components; }
        };
    }

    /** Crea un engine usando los plugins dados (sin ServiceLoader real). */
    static CucumberRuntimeEngine engineWith(List<CorePlugin> plugins) {
        StepDiscoveryService discovery   = new StepDiscoveryService(plugins);
        LifecycleManager     lifecycle   = new DefaultLifecycleManager(plugins);
        return new CucumberRuntimeEngine(lifecycle, discovery);
    }

    private ExecutionConfig config;

    @BeforeEach
    void setUp() {
        config = new ExecutionConfig.Builder().environment("test").build();
    }

    // =========================================================================
    // 1. resolveGluePaths — auto-derivación (request sin glue)
    // =========================================================================

    @Nested
    @DisplayName("1. Auto-derivacion desde plugins SPI (request sin gluePaths)")
    @Order(1)
    class AutoDerivacionTests {

        @Test
        @DisplayName("Un plugin con un componente → su paquete aparece en glue")
        void unPluginUnPaquete() {
            CorePlugin plugin = stubPlugin("alpha",
                    List.of(stubComponent("comp", java.util.ArrayList.class)));
            CucumberRuntimeEngine engine = engineWith(List.of(plugin));
            ExecutionRequest request = ExecutionRequest.of(List.of("f.feature"), config);

            List<String> glue = engine.resolveGluePaths(request);

            assertThat(glue).contains(PKG_A);
        }

        @Test
        @DisplayName("Dos plugins en paquetes distintos → dos entries en glue")
        void dosPluginsDosEntries() {
            CorePlugin pluginA = stubPlugin("pluginA",
                    List.of(stubComponent("ca", java.util.ArrayList.class)));   // java.util
            CorePlugin pluginB = stubPlugin("pluginB",
                    List.of(stubComponent("cb", java.time.Duration.class)));    // java.time
            CucumberRuntimeEngine engine = engineWith(List.of(pluginA, pluginB));
            ExecutionRequest request = ExecutionRequest.of(List.of("f.feature"), config);

            List<String> glue = engine.resolveGluePaths(request);

            assertThat(glue).contains(PKG_A, PKG_B);
        }

        @Test
        @DisplayName("Dos plugins con el mismo paquete → un unico entry (deduplicacion)")
        void dosPluginsMismoPaqueteUnEntry() {
            CorePlugin p1 = stubPlugin("p1", List.of(stubComponent("c1", java.util.ArrayList.class)));
            CorePlugin p2 = stubPlugin("p2", List.of(stubComponent("c2", java.util.LinkedList.class)));
            // Ambas en java.util
            CucumberRuntimeEngine engine = engineWith(List.of(p1, p2));
            ExecutionRequest request = ExecutionRequest.of(List.of("f.feature"), config);

            List<String> glue = engine.resolveGluePaths(request);

            assertThat(glue).containsOnlyOnce(PKG_A);
        }

        @Test
        @DisplayName("Plugin sin componentes no aporta glue paths")
        void pluginSinComponentesNoAportaGlue() {
            CorePlugin vacio = stubPlugin("vacio", List.of());
            CucumberRuntimeEngine engine = engineWith(List.of(vacio));
            ExecutionRequest request = ExecutionRequest.of(List.of("f.feature"), config);

            List<String> glue = engine.resolveGluePaths(request);

            assertThat(glue).isEmpty();
        }

        @Test
        @DisplayName("Componente con getStepDefinitionClass() null se omite")
        void componenteConNullStepClassSeOmite() {
            StepComponent compNull = new StepComponent() {
                @Override public String getName()               { return "null-comp"; }
                @Override public BddPhase getPhase()           { return BddPhase.WHEN; }
                @Override public Class<?> getStepDefinitionClass() { return null; }
            };
            CorePlugin plugin = stubPlugin("pnull", List.of(compNull));
            CucumberRuntimeEngine engine = engineWith(List.of(plugin));
            ExecutionRequest request = ExecutionRequest.of(List.of("f.feature"), config);

            assertThat(engine.resolveGluePaths(request)).isEmpty();
        }

        @Test
        @DisplayName("Sin plugins el resultado es lista vacia")
        void sinPluginsListaVacia() {
            CucumberRuntimeEngine engine = engineWith(List.of());
            ExecutionRequest request = ExecutionRequest.of(List.of("f.feature"), config);

            assertThat(engine.resolveGluePaths(request)).isEmpty();
        }

        @Test
        @DisplayName("El resultado esta ordenado lexicograficamente")
        void resultadoOrdenado() {
            CorePlugin p1 = stubPlugin("p1", List.of(stubComponent("c1", java.util.ArrayList.class)));   // java.util
            CorePlugin p2 = stubPlugin("p2", List.of(stubComponent("c2", java.time.Duration.class)));   // java.time
            CucumberRuntimeEngine engine = engineWith(List.of(p1, p2));
            ExecutionRequest request = ExecutionRequest.of(List.of("f.feature"), config);

            List<String> glue = engine.resolveGluePaths(request);

            assertThat(glue).isSortedAccordingTo(String::compareTo);
        }
    }

    // =========================================================================
    // 2. resolveGluePaths — prioridad de glue explícito del request
    // =========================================================================

    @Nested
    @DisplayName("2. Glue explicito del request tiene prioridad sobre SPI")
    @Order(2)
    class GlueExplicitoTests {

        @Test
        @DisplayName("Glue explicito se usa tal cual, ignorando plugins")
        void glueExplicitoSobrescribePlugins() {
            CorePlugin plugin = stubPlugin("alpha",
                    List.of(stubComponent("c", java.util.ArrayList.class)));
            CucumberRuntimeEngine engine = engineWith(List.of(plugin));
            List<String> explicito = List.of("com.qa.custom.steps");
            ExecutionRequest request = ExecutionRequest.of(
                    List.of("f.feature"), explicito, config);

            List<String> glue = engine.resolveGluePaths(request);

            assertThat(glue).containsExactlyElementsOf(explicito);
            assertThat(glue).doesNotContain(PKG_A);
        }

        @Test
        @DisplayName("Glue explicito vacio → equivale a modo SPI (auto-derivacion)")
        void glueExplicitoVacioEquivaleASPI() {
            CorePlugin plugin = stubPlugin("beta",
                    List.of(stubComponent("c", java.util.ArrayList.class)));
            CucumberRuntimeEngine engine = engineWith(List.of(plugin));
            // 3-arg factory con lista vacía = mismo efecto que 2-arg
            ExecutionRequest request = ExecutionRequest.of(
                    List.of("f.feature"), List.of(), config);

            List<String> glue = engine.resolveGluePaths(request);

            assertThat(glue).contains(PKG_A);
        }
    }

    // =========================================================================
    // 3. StepDiscoveryService.resolveGluePaths() — API pública BE
    // =========================================================================

    @Nested
    @DisplayName("3. StepDiscoveryService.resolveGluePaths() — API para el Backend")
    @Order(3)
    class DiscoveryServiceGlueTests {

        @Test
        @DisplayName("Un plugin → retorna sus paquetes")
        void unPluginRetornaPaquetes() {
            CorePlugin plugin = stubPlugin("ds",
                    List.of(stubComponent("c", java.util.ArrayList.class)));
            StepDiscoveryService service = new StepDiscoveryService(List.of(plugin));

            assertThat(service.resolveGluePaths()).contains(PKG_A);
        }

        @Test
        @DisplayName("Sin plugins → lista vacia")
        void sinPluginsListaVacia() {
            StepDiscoveryService service = new StepDiscoveryService(List.of());

            assertThat(service.resolveGluePaths()).isEmpty();
        }

        @Test
        @DisplayName("Resultado es inmutable")
        void resultadoEsInmutable() {
            CorePlugin plugin = stubPlugin("ds2",
                    List.of(stubComponent("c", java.util.ArrayList.class)));
            StepDiscoveryService service = new StepDiscoveryService(List.of(plugin));

            assertThatThrownBy(() -> service.resolveGluePaths().add("com.hacked"))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Resultado es consistente con CorePlugin.getGluePackages() individual")
        void resultadoConsistenteConGetGluePackages() {
            CorePlugin p1 = stubPlugin("g1", List.of(stubComponent("c1", java.util.ArrayList.class)));
            CorePlugin p2 = stubPlugin("g2", List.of(stubComponent("c2", java.time.Duration.class)));
            StepDiscoveryService service = new StepDiscoveryService(List.of(p1, p2));

            List<String> fromService  = service.resolveGluePaths();
            List<String> fromPlugins  = List.of(p1, p2).stream()
                    .flatMap(p -> p.getGluePackages().stream())
                    .distinct()
                    .sorted()
                    .toList();

            assertThat(fromService).isEqualTo(fromPlugins);
        }
    }
}
