package com.qa.common.runtime;

import com.qa.common.driver.CapabilityReport;
import com.qa.common.runtime.events.EventBus;
import io.cucumber.plugin.ConcurrentEventListener;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Comparator;
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
        @Override public String platformId() { return "FAKE"; }
        @Override public String displayName() { return "Fake Plugin"; }
        @Override public CapabilityReport describeCapabilities() {
            return CapabilityReport.available("FAKE", List.of());
        }
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
        @DisplayName("platformId() retorna identificador de plataforma")
        void platformIdRetornaIdentificador() {
            assertThat(plugin.platformId()).isEqualTo("FAKE");
        }

        @Test
        @DisplayName("getName() (deprecated) retorna platformId en minusculas")
        @SuppressWarnings("deprecation")
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
                    .extracting(CorePlugin::platformId)
                    .containsExactly("FAKE");
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

    // =========================================================================
    // listAllCapabilities() — puerto público BE→FE selector de plataforma
    // =========================================================================

    @Nested
    @DisplayName("CucumberRuntimeEngine — listAllCapabilities()")
    class ListAllCapabilitiesTests {

        private CorePlugin stubWith(String platformId) {
            return new CorePlugin() {
                @Override public String platformId()   { return platformId; }
                @Override public String displayName()  { return platformId + " Testing"; }
                @Override public CapabilityReport describeCapabilities() {
                    return CapabilityReport.available(platformId, List.of());
                }
                @Override public Set<String> getActivationTags() {
                    return Set.of("@" + platformId.toLowerCase());
                }
                @Override public void registerServices(ServiceRegistry r, ExecutionConfig c) {}
                @Override public void onScenarioStart(ExecutionContext ctx) {}
                @Override public void onScenarioEnd(ExecutionContext ctx)   {}
            };
        }

        @AfterEach
        void tearDown() { ExecutionContext.deactivate(); }

        @Test
        @DisplayName("Con HTTP y WEB plugins retorna 2 CapabilityReports")
        void dosPluginsRetornan2Reports() {
            CorePlugin http = stubWith("HTTP");
            CorePlugin web  = stubWith("WEB");
            LifecycleManager lm = new DefaultLifecycleManager(List.of(http, web));
            StepDiscoveryService ds = new StepDiscoveryService(List.of(http, web));
            CucumberRuntimeEngine engine = new CucumberRuntimeEngine(lm, ds);

            List<CapabilityReport> reports = engine.listAllCapabilities();

            assertThat(reports).hasSize(2);
            assertThat(reports).extracting(CapabilityReport::platformId)
                    .containsExactlyInAnyOrder("HTTP", "WEB");
        }

        @Test
        @DisplayName("Sin plugins retorna lista vacía")
        void sinPluginsListaVacia() {
            LifecycleManager lm = new DefaultLifecycleManager(List.of());
            StepDiscoveryService ds = new StepDiscoveryService(List.of());
            CucumberRuntimeEngine engine = new CucumberRuntimeEngine(lm, ds);

            assertThat(engine.listAllCapabilities()).isEmpty();
        }

        @Test
        @DisplayName("Cada report refleja el estado de disponibilidad declarado por el plugin")
        void reportRefleja_available_flag() {
            CorePlugin http = stubWith("HTTP");
            LifecycleManager lm = new DefaultLifecycleManager(List.of(http));
            StepDiscoveryService ds = new StepDiscoveryService(List.of(http));
            CucumberRuntimeEngine engine = new CucumberRuntimeEngine(lm, ds);

            List<CapabilityReport> reports = engine.listAllCapabilities();

            assertThat(reports).hasSize(1);
            assertThat(reports.get(0).available()).isTrue();
        }
    }

    // =========================================================================
    // Suite lifecycle hook order — onSuiteStart ascendente, onSuiteEnd descendente
    // =========================================================================

    @Nested
    @DisplayName("CucumberRuntimeEngine — Suite lifecycle hook order")
    class SuiteHookOrderTests {

        private final List<String> callLog = new ArrayList<>();

        private CorePlugin trackingPlugin(String name, int order) {
            return new CorePlugin() {
                @Override public String platformId()   { return name.toUpperCase(); }
                @Override public String displayName()  { return name; }
                @Override public CapabilityReport describeCapabilities() {
                    return CapabilityReport.available(name.toUpperCase(), List.of());
                }
                @Override public Set<String> getActivationTags() { return Set.of("@" + name); }
                @Override public int getOrder() { return order; }
                @Override public void registerServices(ServiceRegistry r, ExecutionConfig c) {}
                @Override public void onScenarioStart(ExecutionContext ctx) {}
                @Override public void onScenarioEnd(ExecutionContext ctx)   {}
                @Override public void onSuiteStart(ExecutionConfig c) { callLog.add("start:" + name); }
                @Override public void onSuiteEnd()                    { callLog.add("end:" + name); }
            };
        }

        /** Engine sin Cucumber real — sobreescribe runCucumber() como no-op. */
        private CucumberRuntimeEngine noOpEngine(List<CorePlugin> plugins) {
            List<CorePlugin> sorted = plugins.stream()
                    .sorted(Comparator.comparingInt(CorePlugin::getHookOrder))
                    .toList();
            StepDiscoveryService discovery = new StepDiscoveryService(sorted);
            LifecycleManager lm = new DefaultLifecycleManager(sorted);
            return new CucumberRuntimeEngine(lm, discovery) {
                @Override
                protected byte runCucumber(ExecutionRequest req,
                                           ExecutionContext ctx,
                                           InMemoryResultCollector col,
                                           List<ConcurrentEventListener> listeners) {
                    return 0;
                }
            };
        }

        @AfterEach
        void tearDown() {
            ExecutionContext.deactivate();
            callLog.clear();
        }

        @Test
        @DisplayName("onSuiteStart invoca plugins en orden ascendente de hookOrder")
        void suiteStartAscendente() {
            CorePlugin api = trackingPlugin("api", 50);
            CorePlugin db  = trackingPlugin("db",   0);
            CucumberRuntimeEngine engine = noOpEngine(List.of(api, db));
            ExecutionRequest req = ExecutionRequest.of(
                    List.of("dummy.feature"), new ExecutionConfig.Builder().build());

            engine.execute(req);

            // DB (hookOrder=0) debe invocarse antes que API (hookOrder=50)
            assertThat(callLog.indexOf("start:db")).isGreaterThanOrEqualTo(0);
            assertThat(callLog.indexOf("start:api")).isGreaterThanOrEqualTo(0);
            assertThat(callLog.indexOf("start:db")).isLessThan(callLog.indexOf("start:api"));
        }

        @Test
        @DisplayName("onSuiteEnd invoca plugins en orden descendente de hookOrder (LIFO)")
        void suiteEndDescendente() {
            CorePlugin api = trackingPlugin("api", 50);
            CorePlugin db  = trackingPlugin("db",   0);
            CucumberRuntimeEngine engine = noOpEngine(List.of(api, db));
            ExecutionRequest req = ExecutionRequest.of(
                    List.of("dummy.feature"), new ExecutionConfig.Builder().build());

            engine.execute(req);

            // API (hookOrder=50) debe cerrarse antes que DB (hookOrder=0) → orden inverso
            assertThat(callLog.indexOf("end:api")).isGreaterThanOrEqualTo(0);
            assertThat(callLog.indexOf("end:db")).isGreaterThanOrEqualTo(0);
            assertThat(callLog.indexOf("end:api")).isLessThan(callLog.indexOf("end:db"));
        }

        @Test
        @DisplayName("Con un solo plugin, start y end se invocan exactamente una vez")
        void unPluginStartEndUnaVez() {
            CorePlugin api = trackingPlugin("api", 50);
            CucumberRuntimeEngine engine = noOpEngine(List.of(api));
            ExecutionRequest req = ExecutionRequest.of(
                    List.of("dummy.feature"), new ExecutionConfig.Builder().build());

            engine.execute(req);

            assertThat(callLog).containsExactly("start:api", "end:api");
        }
    }
}
