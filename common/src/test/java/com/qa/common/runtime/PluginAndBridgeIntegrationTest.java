package com.qa.common.runtime;

import com.qa.common.runtime.events.EventBus;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integración del sistema de Plugins y Runtime Core.
 *
 * <p>Verifica que:
 * <ol>
 *   <li>El contrato {@link CorePlugin} funciona con implementaciones concretas</li>
 *   <li>Los servicios se registran en {@link ServiceRegistry}</li>
 *   <li>Los componentes se declaran con los metadatos correctos</li>
 *   <li>Los steps escriben y leen de {@link VariableStore} via {@link ExecutionContext}</li>
 *   <li>El {@link CucumberRuntimeEngine} puede instanciarse con plugins y expone componentes</li>
 * </ol>
 *
 * <p><b>Nota:</b> Los tests específicos de DatabasePlugin se movieron a
 * {@code database-core/DatabasePluginIntegrationTest} en TASK-A03, ya que
 * {@code common} no puede depender de módulos especializados.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("Plugins + Runtime — Integración del Sistema")
class PluginAndBridgeIntegrationTest {

    // =========================================================================
    // Stub de plugin para tests de runtime (no depende de ningún módulo externo)
    // =========================================================================

    /** Plugin stub que verifica el contrato CorePlugin sin dependencias externas. */
    private static class FakeCorePlugin implements CorePlugin {
        @Override public String getName() { return "fake"; }
        @Override public Set<String> getActivationTags() { return Set.of("@fake"); }
        @Override public int getOrder() { return 99; }
        @Override public void registerServices(ServiceRegistry registry, ExecutionConfig config) {
            registry.registerInstance(String.class, "fakeService");
        }
        @Override public void onScenarioStart(ExecutionContext context) { /* no-op */ }
        @Override public void onScenarioEnd(ExecutionContext context) { /* no-op */ }
        @Override public List<StepComponent> getComponents() { return List.of(); }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ExecutionContext buildContext() {
        ExecutionConfig config = new ExecutionConfig.Builder().build();
        ServiceRegistry registry = new ServiceRegistry();
        VariableStore variables = new VariableStore();
        EventBus eventBus = new EventBus();
        return new ExecutionContext(config, registry, variables, eventBus);
    }

    // =========================================================================
    // CorePlugin stub — contrato del plugin
    // =========================================================================

    @Nested
    @DisplayName("FakeCorePlugin — Contrato del Plugin")
    class FakeCorePluginTests {

        private final FakeCorePlugin plugin = new FakeCorePlugin();

        @Test
        @DisplayName("getName() retorna identificador del plugin")
        void getNameRetornaIdentificador() {
            assertThat(plugin.getName()).isEqualTo("fake");
        }

        @Test
        @DisplayName("getActivationTags() retorna tags de activación")
        void getActivationTagsRetornaTags() {
            assertThat(plugin.getActivationTags()).containsExactly("@fake");
        }

        @Test
        @DisplayName("getOrder() retorna orden de inicialización")
        void getOrderRetornaOrden() {
            assertThat(plugin.getOrder()).isEqualTo(99);
        }

        @Test
        @DisplayName("registerServices() registra servicio correctamente")
        void registerServicesRegistraServicio() {
            ServiceRegistry registry = new ServiceRegistry();
            ExecutionConfig config = new ExecutionConfig.Builder().build();

            plugin.registerServices(registry, config);

            assertThat(registry.isRegistered(String.class)).isTrue();
        }

        @Test
        @DisplayName("getComponents() retorna lista (puede estar vacía)")
        void getComponentsRetornaLista() {
            assertThat(plugin.getComponents()).isNotNull();
        }

        @Test
        @DisplayName("onScenarioStart/onScenarioEnd no lanza excepciones")
        void lifecycleCallbacksNoLanzaExcepciones() {
            ExecutionContext ctx = buildContext();
            assertThatCode(() -> plugin.onScenarioStart(ctx)).doesNotThrowAnyException();
            assertThatCode(() -> plugin.onScenarioEnd(ctx)).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // VariableStore — escritura directa desde steps
    // =========================================================================

    @Nested
    @DisplayName("VariableStore — Escritura Directa desde Steps")
    class VariableStoreDirectWriteTests {

        @BeforeEach
        void limpiar() {
            ExecutionContext.deactivate();
        }

        @AfterEach
        void tearDown() {
            ExecutionContext.deactivate();
        }

        @Test
        @DisplayName("Steps escriben directamente al VariableStore del ExecutionContext activo")
        void stepsEscribenDirectoAlVariableStore() {
            ExecutionContext ctx = buildContext();
            ctx.activate();

            ctx.variables().set("miVariable", "miValor");

            assertThat(ctx.variables().get("miVariable", String.class))
                    .isPresent()
                    .hasValue("miValor");
        }

        @Test
        @DisplayName("VariableStore.resolve() sustituye ${var} con valores almacenados")
        void variableStoreResuelvePatronDollar() {
            ExecutionContext ctx = buildContext();
            ctx.activate();

            ctx.variables().set("token", "abc123");

            String resolved = ctx.variables().resolve("Authorization: Bearer ${token}");
            assertThat(resolved).isEqualTo("Authorization: Bearer abc123");
        }

        @Test
        @DisplayName("VariableStore.resolve() retorna el placeholder si la variable no existe")
        void variableStoreRetornaPlaceholderSiNoExiste() {
            ExecutionContext ctx = buildContext();
            ctx.activate();

            String resolved = ctx.variables().resolve("${inexistente}");
            assertThat(resolved).isEqualTo("${inexistente}");
        }

        @Test
        @DisplayName("Múltiples variables almacenadas y resueltas correctamente")
        void multiplesVariablesAlmacenadasYResueltas() {
            ExecutionContext ctx = buildContext();
            ctx.activate();

            ctx.variables().set("var1", "a");
            ctx.variables().set("var2", "b");
            ctx.variables().set("var3", "c");

            assertThat(ctx.variables().get("var1", Object.class)).isPresent();
            assertThat(ctx.variables().get("var2", Object.class)).isPresent();
            assertThat(ctx.variables().get("var3", Object.class)).isPresent();
            assertThat(ctx.variables().resolve("${var1}-${var2}-${var3}")).isEqualTo("a-b-c");
        }
    }

    // =========================================================================
    // ExecutionContext + ServiceRegistry — contenedor DI
    // =========================================================================

    @Nested
    @DisplayName("ExecutionContext + ServiceRegistry — Contenedor DI")
    class ExecutionContextDITests {

        @Test
        @DisplayName("service() resuelve servicio registrado por tipo")
        void serviceResuelveServicioRegistrado() {
            ExecutionContext ctx = buildContext();
            ctx.registry().registerInstance(String.class, "helloService");

            assertThat(ctx.service(String.class)).isEqualTo("helloService");
        }

        @Test
        @DisplayName("registry().require() lanza IllegalStateException si no está registrado")
        void requireLanzaExcepcionSiNoEstaRegistrado() {
            ExecutionContext ctx = buildContext();

            assertThatThrownBy(() -> ctx.service(Integer.class))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Integer");
        }

        @Test
        @DisplayName("cleanup() libera todos los recursos")
        void cleanupLiberaRecursos() {
            ExecutionContext ctx = buildContext();
            ctx.activate();
            ctx.variables().set("temp", "data");
            ctx.registry().registerInstance(String.class, "svc");

            ctx.cleanup();

            assertThat(ExecutionContext.current()).isEmpty();
            assertThat(ctx.variables().size()).isZero();
        }
    }

    // =========================================================================
    // CucumberRuntimeEngine — descubrimiento de componentes via plugins
    // =========================================================================

    @Nested
    @DisplayName("CucumberRuntimeEngine — Descubrimiento de Componentes")
    class EngineComponentDiscoveryTests {

        @Test
        @DisplayName("Engine instanciado con FakePlugin expone su nombre")
        void engineConFakePluginExponeNombre() {
            FakeCorePlugin fakePlugin = new FakeCorePlugin();
            LifecycleManager lm = new DefaultLifecycleManager(List.of(fakePlugin));
            StepDiscoveryService discovery = new StepDiscoveryService(List.of(fakePlugin));
            CucumberRuntimeEngine engine = new CucumberRuntimeEngine(lm, discovery);

            assertThat(engine.getDiscoveryService().getPlugins())
                    .hasSize(1)
                    .extracting(CorePlugin::getName)
                    .containsExactly("fake");
        }

        @Test
        @DisplayName("StepDiscoveryService con plugin sin componentes retorna mapa vacío")
        void discoveryConPluginSinComponentesRetornaMapaVacio() {
            FakeCorePlugin fakePlugin = new FakeCorePlugin();
            StepDiscoveryService discovery = new StepDiscoveryService(List.of(fakePlugin));

            // FakeCorePlugin.getComponents() == [] → no hay entradas en el mapa agrupado
            var componentsByPlugin = discovery.groupByPlugin();
            assertThat(componentsByPlugin).doesNotContainKey("fake");
            assertThat(discovery.totalComponents()).isZero();
        }
    }
}
