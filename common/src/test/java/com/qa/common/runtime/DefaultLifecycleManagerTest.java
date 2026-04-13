package com.qa.common.runtime;

import com.qa.common.runtime.events.EventBus;
import com.qa.common.runtime.events.ExecutionEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests unitarios para DefaultLifecycleManager.
 *
 * <p>Verifica la inicializacion de ExecutionContext, el filtrado de plugins
 * por tags, el orden de invocacion en onScenarioStart/End y el shutdown.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("DefaultLifecycleManager")
class DefaultLifecycleManagerTest {

    // =========================================================================
    // Stubs
    // =========================================================================

    private final List<String> callLog = new ArrayList<>();

    private CorePlugin makePlugin(String name, int order, Set<String> tags) {
        return new CorePlugin() {
            @Override public String getName()                { return name; }
            @Override public Set<String> getActivationTags(){ return tags; }
            @Override public int getOrder()                  { return order; }
            @Override public List<StepComponent> getComponents() { return List.of(); }
            @Override public void registerServices(ServiceRegistry r, ExecutionConfig c) {
                callLog.add("registerServices:" + name);
            }
            @Override public void onScenarioStart(ExecutionContext ctx) {
                callLog.add("onScenarioStart:" + name);
            }
            @Override public void onScenarioEnd(ExecutionContext ctx) {
                callLog.add("onScenarioEnd:" + name);
            }
        };
    }

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
        callLog.clear();
    }

    // =========================================================================

    @Nested
    @DisplayName("Constructor")
    class ConstructorTest {

        @Test
        @DisplayName("Lista null de plugins lanza NullPointerException")
        void testNullPluginsLanzaException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new DefaultLifecycleManager(null));
        }

        @Test
        @DisplayName("Lista vacia es valida")
        void testListaVaciaEsValida() {
            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of());
            assertThat(mgr.getPlugins()).isEmpty();
        }

        @Test
        @DisplayName("Los plugins se ordenan por getOrder() ascendente")
        void testPluginsOrdenadosPorOrder() {
            CorePlugin api    = makePlugin("api",    50,  Set.of("@api"));
            CorePlugin db     = makePlugin("db",      0,  Set.of("@db"));
            CorePlugin mobile = makePlugin("mobile", 150, Set.of("@mobile"));

            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of(mobile, api, db));

            List<String> nombres = mgr.getPlugins().stream().map(CorePlugin::getName).toList();
            assertThat(nombres).containsExactly("db", "api", "mobile");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("initialize()")
    class InitializeTest {

        @Test
        @DisplayName("Retorna ExecutionContext con todos los componentes")
        void testRetornaContextValido() {
            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of(
                    makePlugin("api", 50, Set.of("@api"))
            ));
            ExecutionRequest req = ExecutionRequest.of(
                    List.of("classpath:features/"),
                    List.of("com.qa"),
                    new ExecutionConfig.Builder().tags("@api").build()
            );

            ExecutionContext ctx = mgr.initialize(req);

            assertThat(ctx).isNotNull();
            assertThat(ExecutionContext.current()).isPresent().hasValue(ctx);
        }

        @Test
        @DisplayName("Request null lanza NullPointerException")
        void testRequestNullLanzaException() {
            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of());
            assertThatNullPointerException().isThrownBy(() -> mgr.initialize(null));
        }

        @Test
        @DisplayName("Invoca registerServices en plugins activos segun tags")
        void testInvokaRegisterServicesEnPluginsActivos() {
            CorePlugin api = makePlugin("api", 50, Set.of("@api"));
            CorePlugin web = makePlugin("web", 100, Set.of("@web"));

            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of(api, web));
            ExecutionRequest req = ExecutionRequest.of(
                    List.of("classpath:features/"),
                    List.of("com.qa"),
                    new ExecutionConfig.Builder().tags("@api").build()
            );

            mgr.initialize(req);

            assertThat(callLog).contains("registerServices:api");
            assertThat(callLog).doesNotContain("registerServices:web");
        }

        @Test
        @DisplayName("Sin tags activa todos los plugins")
        void testSinTagsActivaTodos() {
            CorePlugin api = makePlugin("api", 50, Set.of("@api"));
            CorePlugin web = makePlugin("web", 100, Set.of("@web"));

            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of(api, web));
            ExecutionRequest req = ExecutionRequest.of(
                    List.of("classpath:features/"),
                    List.of("com.qa"),
                    new ExecutionConfig.Builder().build()
            );

            mgr.initialize(req);

            assertThat(callLog).containsExactlyInAnyOrder("registerServices:api", "registerServices:web");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("resolveActivePlugins()")
    class ResolveActivePluginsTest {

        @Test
        @DisplayName("Tags '@api and @smoke' activa solo ApiPlugin")
        void testResolveConApiTag() {
            CorePlugin api = makePlugin("api", 50, Set.of("@api"));
            CorePlugin web = makePlugin("web", 100, Set.of("@web"));
            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of(api, web));

            List<CorePlugin> activos = mgr.resolveActivePlugins("@api and @smoke");
            assertThat(activos).extracting(CorePlugin::getName).containsOnly("api");
        }

        @Test
        @DisplayName("Tags '@api and @web' activa ambos plugins")
        void testResolveConMultiplesTags() {
            CorePlugin api = makePlugin("api", 50, Set.of("@api"));
            CorePlugin web = makePlugin("web", 100, Set.of("@web"));
            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of(api, web));

            List<CorePlugin> activos = mgr.resolveActivePlugins("@api and @web");
            assertThat(activos).extracting(CorePlugin::getName)
                    .containsExactlyInAnyOrder("api", "web");
        }

        @Test
        @DisplayName("Tags null retorna todos los plugins")
        void testTagsNullRetornaTodos() {
            CorePlugin api = makePlugin("api", 50, Set.of("@api"));
            CorePlugin web = makePlugin("web", 100, Set.of("@web"));
            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of(api, web));

            List<CorePlugin> activos = mgr.resolveActivePlugins(null);
            assertThat(activos).hasSize(2);
        }

        @Test
        @DisplayName("Tags vacios retorna todos los plugins")
        void testTagsVaciosRetornaTodos() {
            CorePlugin api = makePlugin("api", 50, Set.of("@api"));
            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of(api));

            List<CorePlugin> activos = mgr.resolveActivePlugins("   ");
            assertThat(activos).hasSize(1);
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("onScenarioStart() / onScenarioEnd()")
    class ScenarioLifecycleTest {

        @Test
        @DisplayName("onScenarioStart invoca plugins en orden ascendente de order")
        void testOnScenarioStartOrden() {
            CorePlugin api = makePlugin("api", 50,  Set.of("@api"));
            CorePlugin db  = makePlugin("db",   0,  Set.of("@db"));

            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of(api, db));
            ExecutionContext ctx = crearContext(mgr, "@api @db");

            mgr.onScenarioStart(ctx, List.of("@db", "@api"));

            // db (order=0) debe invocarse antes que api (order=50)
            int dbIdx  = callLog.indexOf("onScenarioStart:db");
            int apiIdx = callLog.indexOf("onScenarioStart:api");
            assertThat(dbIdx).isLessThan(apiIdx);
        }

        @Test
        @DisplayName("onScenarioEnd invoca plugins en orden inverso")
        void testOnScenarioEndOrdenInverso() {
            CorePlugin api = makePlugin("api", 50,  Set.of("@api"));
            CorePlugin db  = makePlugin("db",   0,  Set.of("@db"));

            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of(api, db));
            ExecutionContext ctx = crearContext(mgr, "@api @db");

            mgr.onScenarioEnd(ctx, List.of("@db", "@api"));

            // api (order=50) debe cerrarse antes que db (order=0) → orden inverso
            int dbIdx  = callLog.indexOf("onScenarioEnd:db");
            int apiIdx = callLog.indexOf("onScenarioEnd:api");
            assertThat(apiIdx).isLessThan(dbIdx);
        }

        @Test
        @DisplayName("onScenarioStart con context null lanza NullPointerException")
        void testOnScenarioStartContextNull() {
            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of());
            assertThatNullPointerException()
                    .isThrownBy(() -> mgr.onScenarioStart(null, List.of("@api")));
        }

        @Test
        @DisplayName("onScenarioStart con tags null no lanza excepcion")
        void testOnScenarioStartTagsNullNoLanza() {
            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of());
            ExecutionContext ctx = crearContext(mgr, null);
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> mgr.onScenarioStart(ctx, null)
            );
        }

        @Test
        @DisplayName("Plugin que lanza excepcion en onScenarioStart no aborta los siguientes")
        void testExcepcionEnPluginNoPropaga() {
            CorePlugin problematico = new CorePlugin() {
                @Override public String getName()                { return "bad"; }
                @Override public Set<String> getActivationTags(){ return Set.of("@bad"); }
                @Override public int getOrder()                  { return 50; }
                @Override public List<StepComponent> getComponents() { return List.of(); }
                @Override public void registerServices(ServiceRegistry r, ExecutionConfig c) {}
                @Override public void onScenarioStart(ExecutionContext ctx) {
                    throw new RuntimeException("Plugin roto");
                }
                @Override public void onScenarioEnd(ExecutionContext ctx) {}
            };
            CorePlugin bueno = makePlugin("good", 100, Set.of("@bad"));

            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of(problematico, bueno));
            ExecutionContext ctx = crearContext(mgr, "@bad");

            // No debe lanzar; el plugin bueno debe ejecutarse igual
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> mgr.onScenarioStart(ctx, List.of("@bad"))
            );
            assertThat(callLog).contains("onScenarioStart:good");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("shutdown()")
    class ShutdownTest {

        @Test
        @DisplayName("shutdown null context no lanza excepcion")
        void testShutdownNullNoLanza() {
            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> mgr.shutdown(null));
        }

        @Test
        @DisplayName("shutdown limpia el ExecutionContext activo")
        void testShutdownLimpiaContext() {
            DefaultLifecycleManager mgr = new DefaultLifecycleManager(List.of());
            ExecutionContext ctx = crearContext(mgr, null);

            assertThat(ExecutionContext.current()).isPresent();
            mgr.shutdown(ctx);
            assertThat(ExecutionContext.current()).isEmpty();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ExecutionContext crearContext(DefaultLifecycleManager mgr, String tags) {
        ExecutionConfig config = tags != null
                ? new ExecutionConfig.Builder().tags(tags).build()
                : new ExecutionConfig.Builder().build();
        ExecutionRequest req = ExecutionRequest.of(
                List.of("classpath:features/"), List.of("com.qa"), config);
        return mgr.initialize(req);
    }
}
