package com.qa.common.runtime;

import com.qa.common.database.plugin.DatabasePlugin;
import com.qa.common.http.exceptions.FrameworkBusinessException;
import com.qa.common.runtime.events.EventBus;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integración del sistema de Plugins y Runtime Core.
 *
 * <p>Verifica que:
 * <ol>
 *   <li>Los plugins implementan correctamente {@link CorePlugin}</li>
 *   <li>Los servicios se registran en {@link ServiceRegistry}</li>
 *   <li>Los componentes se declaran con los metadatos correctos</li>
 *   <li>Los steps escriben y leen de {@link VariableStore} via {@link ExecutionContext}</li>
 *   <li>El {@link CucumberRuntimeEngine} puede instanciarse con los plugins
 *       conocidos y expone los componentes declarados</li>
 * </ol>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("Plugins + Runtime — Integración del Sistema")
class PluginAndBridgeIntegrationTest {

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Crea un {@link ExecutionContext} con componentes mínimos para tests. */
    private ExecutionContext buildContext() {
        ExecutionConfig config = new ExecutionConfig.Builder().build();
        ServiceRegistry registry = new ServiceRegistry();
        VariableStore variables = new VariableStore();
        EventBus eventBus = new EventBus();
        return new ExecutionContext(config, registry, variables, eventBus);
    }

    // =========================================================================
    // DatabasePlugin — contrato del plugin en módulo common
    // =========================================================================

    @Nested
    @DisplayName("DatabasePlugin — Contrato del Plugin")
    class DatabasePluginTests {

        private final DatabasePlugin plugin = new DatabasePlugin();

        @Test
        @DisplayName("getName() retorna 'database'")
        void getNameRetornaDatabase() {
            assertThat(plugin.getName()).isEqualTo("database");
        }

        @Test
        @DisplayName("getActivationTags() incluye @db, @database, @sql, @jdbc")
        void getActivationTagsIncludeAllDbTags() {
            assertThat(plugin.getActivationTags())
                    .containsExactlyInAnyOrder("@db", "@database", "@sql", "@jdbc");
        }

        @Test
        @DisplayName("getOrder() retorna 0 (se inicializa primero)")
        void getOrderRetornaCero() {
            assertThat(plugin.getOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("registerServices() registra DatabaseHelper")
        void registerServicesRegistraDatabaseHelper() {
            ServiceRegistry registry = new ServiceRegistry();
            ExecutionConfig config = new ExecutionConfig.Builder().build();

            plugin.registerServices(registry, config);

            assertThat(registry.isRegistered(com.qa.common.database.helpers.DatabaseHelper.class))
                    .isTrue();
        }

        @Test
        @DisplayName("getComponents() declara exactamente 1 componente")
        void getComponentsDeclara1Componente() {
            assertThat(plugin.getComponents()).hasSize(1);
        }

        @Test
        @DisplayName("El componente DB tiene fase GIVEN")
        void componenteDbTieneFaseGiven() {
            StepComponent comp = plugin.getComponents().getFirst();
            assertThat(comp.getPhase()).isEqualTo(BddPhase.GIVEN);
            assertThat(comp.getName()).isNotBlank();
            assertThat(comp.getDescription()).isNotBlank();
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
    // VariableStore — escritura directa desde steps (Fase 2)
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

            // Simula lo que hace un step migrado
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
        @DisplayName("VariableStore.resolve() lee de VariableStore como prioridad")
        void variableStoreResuelveDesdeContexto() {
            ExecutionContext ctx = buildContext();
            ctx.activate();

            // Steps escriben al VariableStore directamente
            ctx.variables().set("rut", "12345678-9");

            // VariableStore.resolve() resuelve placeholders ${var}
            String resolved = ctx.variables().resolve("usuario ${rut}");
            assertThat(resolved).isEqualTo("usuario 12345678-9");
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
        @DisplayName("Engine instanciado con DatabasePlugin expone sus componentes")
        void engineConDatabasePluginExponeComponentes() {
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

        @Test
        @DisplayName("StepDiscoveryService agrupa componentes por plugin")
        void discoveryAgrupaPorPlugin() {
            DatabasePlugin dbPlugin = new DatabasePlugin();
            StepDiscoveryService discovery = new StepDiscoveryService(java.util.List.of(dbPlugin));

            var componentsByPlugin = discovery.groupByPlugin();
            assertThat(componentsByPlugin).containsKey("database");
            assertThat(componentsByPlugin.get("database")).hasSize(1);
        }
    }
}

