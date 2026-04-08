package com.qa.common.runtime;

import com.qa.common.database.plugin.DatabasePlugin;
import com.qa.common.runtime.events.EventBus;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integración de FASE 1 — Plugins + VariableStore.
 *
 * <p>Verifica que:
 * <ol>
 *   <li>Los plugins implementan correctamente {@link CorePlugin}</li>
 *   <li>Los servicios se registran en {@link ServiceRegistry}</li>
 *   <li>Los componentes se declaran con los metadatos correctos</li>
 *   <li>Los steps escriben y leen directamente de {@link VariableStore} via {@link ExecutionContext}</li>
 *   <li>El {@link CucumberRuntimeEngine} puede instanciarse con los plugins conocidos</li>
 * </ol>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("FASE 1 — Plugins + VariableStore (Integración)")
class Phase1IntegrationTest {

    private ExecutionContext buildContext() {
        ExecutionConfig config = new ExecutionConfig.Builder().build();
        ServiceRegistry registry = new ServiceRegistry();
        VariableStore variables = new VariableStore();
        EventBus eventBus = new EventBus();
        return new ExecutionContext(config, registry, variables, eventBus);
    }

    // =========================================================================
    // DatabasePlugin
    // =========================================================================

    @Nested
    @DisplayName("DatabasePlugin")
    class DatabasePluginTests {

        private final DatabasePlugin plugin = new DatabasePlugin();

        @Test @DisplayName("getName() retorna 'database'")
        void getNameRetornaDatabase() {
            assertThat(plugin.getName()).isEqualTo("database");
        }

        @Test @DisplayName("getActivationTags() incluye @db, @database, @sql, @jdbc")
        void getActivationTagsIncludeAllDbTags() {
            assertThat(plugin.getActivationTags())
                    .containsExactlyInAnyOrder("@db", "@database", "@sql", "@jdbc");
        }

        @Test @DisplayName("getOrder() retorna 0")
        void getOrderRetornaCero() {
            assertThat(plugin.getOrder()).isEqualTo(0);
        }

        @Test @DisplayName("registerServices() registra DatabaseHelper")
        void registerServicesRegistraDatabaseHelper() {
            ServiceRegistry registry = new ServiceRegistry();
            ExecutionConfig config = new ExecutionConfig.Builder().build();
            plugin.registerServices(registry, config);
            assertThat(registry.isRegistered(com.qa.common.database.helpers.DatabaseHelper.class))
                    .isTrue();
        }

        @Test @DisplayName("getComponents() declara exactamente 1 componente")
        void getComponentsDeclara1Componente() {
            assertThat(plugin.getComponents()).hasSize(1);
        }

        @Test @DisplayName("El componente DB tiene fase GIVEN")
        void componenteDbTieneFaseGiven() {
            StepComponent comp = plugin.getComponents().get(0);
            assertThat(comp.getPhase()).isEqualTo(BddPhase.GIVEN);
            assertThat(comp.getName()).isNotBlank();
            assertThat(comp.getDescription()).isNotBlank();
        }

        @Test @DisplayName("onScenarioStart/onScenarioEnd no lanza excepciones")
        void lifecycleCallbacksNoLanzaExcepciones() {
            ExecutionContext ctx = buildContext();
            assertThatCode(() -> plugin.onScenarioStart(ctx)).doesNotThrowAnyException();
            assertThatCode(() -> plugin.onScenarioEnd(ctx)).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // VariableStore — escritura directa desde steps (Fase 2)
    // =========================================================================

    @Nested
    @DisplayName("VariableStore — Escritura Directa desde Steps")
    class VariableStoreDirectTests {

        @BeforeEach void limpiar() { ExecutionContext.deactivate(); }
        @AfterEach void tearDown() { ExecutionContext.deactivate(); }

        @Test @DisplayName("Steps escriben directamente al VariableStore")
        void stepsEscribenAlVariableStore() {
            ExecutionContext ctx = buildContext();
            ctx.activate();
            ctx.variables().set("miVariable", "miValor");
            assertThat(ctx.variables().get("miVariable", String.class))
                    .isPresent().hasValue("miValor");
        }

        @Test @DisplayName("VariableStore.resolve() sustituye ${var} con valores almacenados")
        void variableStoreResuelvePatronDollar() {
            ExecutionContext ctx = buildContext();
            ctx.activate();
            ctx.variables().set("rut", "12345678-9");
            assertThat(ctx.variables().resolve("usuario ${rut}")).isEqualTo("usuario 12345678-9");
        }

        @Test @DisplayName("VariableStore.resolve() lee del contexto activo")
        void variableStoreResuelveDesdeContexto() {
            ExecutionContext ctx = buildContext();
            ctx.activate();
            ctx.variables().set("userId", "ABC99");
            assertThat(ctx.variables().resolve("id=${userId}")).isEqualTo("id=ABC99");
        }

        @Test @DisplayName("VariableStore.set() propaga variables al contexto activo (fix §6.1)")
        void variableStoreSetPropagaAlContexto() {
            ExecutionContext ctx = buildContext();
            ctx.activate();
            ctx.variables().set("bridgePrueba", "valor");
            assertThat(ctx.variables().has("bridgePrueba")).isTrue();
            assertThat(ctx.variables().get("bridgePrueba", Object.class))
                .isPresent().satisfies(v -> assertThat(v.get().toString()).isEqualTo("valor"));
        }


        @Test @DisplayName("Múltiples variables en VariableStore")
        void multiplesVariablesEnVariableStore() {
            ExecutionContext ctx = buildContext();
            ctx.activate();
            ctx.variables().set("a", "1");
            ctx.variables().set("b", "2");
            ctx.variables().set("c", "3");
            assertThat(ctx.variables().resolve("${a}-${b}-${c}")).isEqualTo("1-2-3");
        }
    }

    // =========================================================================
    // ExecutionContext + ServiceRegistry
    // =========================================================================

    @Nested
    @DisplayName("ExecutionContext + ServiceRegistry")
    class ExecutionContextDITests {

        @Test @DisplayName("service() resuelve servicio registrado por tipo")
        void serviceResuelveServicioRegistrado() {
            ExecutionContext ctx = buildContext();
            ctx.registry().registerInstance(String.class, "helloService");
            assertThat(ctx.service(String.class)).isEqualTo("helloService");
        }

        @Test @DisplayName("registry().require() lanza IllegalStateException si no está registrado")
        void requireLanzaExcepcionSiNoEstaRegistrado() {
            ExecutionContext ctx = buildContext();
            assertThatThrownBy(() -> ctx.service(Integer.class))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Integer");
        }

        @Test @DisplayName("cleanup() libera todos los recursos")
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
    // CucumberRuntimeEngine — StepDiscovery
    // =========================================================================

    @Nested
    @DisplayName("CucumberRuntimeEngine — Descubrimiento de componentes")
    class EngineDiscoveryTests {

        @Test @DisplayName("Engine con DatabasePlugin expone componentes")
        void engineConDatabasePluginExponeDatabaseComponentes() {
            DatabasePlugin dbPlugin = new DatabasePlugin();
            LifecycleManager lm = new DefaultLifecycleManager(java.util.List.of(dbPlugin));
            StepDiscoveryService discovery = new StepDiscoveryService(java.util.List.of(dbPlugin));
            CucumberRuntimeEngine engine = new CucumberRuntimeEngine(lm, discovery);

            assertThat(engine.getDiscoveryService().getPlugins())
                    .hasSize(1)
                    .extracting(CorePlugin::getName)
                    .containsExactly("database");

            assertThat(engine.getDiscoveryService().totalComponents()).isEqualTo(1);
        }

        @Test @DisplayName("StepDiscoveryService agrupa componentes por plugin")
        void discoveryAgrupaPorPlugin() {
            DatabasePlugin dbPlugin = new DatabasePlugin();
            StepDiscoveryService discovery = new StepDiscoveryService(java.util.List.of(dbPlugin));
            var componentsByPlugin = discovery.groupByPlugin();
            assertThat(componentsByPlugin).containsKey("database");
            assertThat(componentsByPlugin.get("database")).hasSize(1);
        }
    }
}

