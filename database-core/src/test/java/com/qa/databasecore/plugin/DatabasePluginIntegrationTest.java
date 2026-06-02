package com.qa.databasecore.plugin;


import com.qa.common.spi.CorePlugin;
import com.qa.common.api.runtime.BddPhase;
import com.qa.common.internal.runtime.CucumberRuntimeEngine;
import com.qa.common.internal.runtime.DefaultLifecycleManager;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.api.runtime.LifecycleManager;
import com.qa.common.api.runtime.ServiceRegistry;
import com.qa.common.internal.runtime.StepDiscoveryService;
import com.qa.databasecore.helper.DatabaseHelper;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integración de DatabasePlugin con el runtime de ejecución BDD.
 *
 * <p>Movido desde common en TASK-A03: DatabasePlugin vive en database-core,
 * por lo que sus tests de integración también pertenecen aquí.</p>
 *
 * @author Abel Venero
 * @since 2.1.0
 */
@DisplayName("DatabasePlugin — Integración con Runtime")
class DatabasePluginIntegrationTest {

    private final DatabasePlugin plugin = new DatabasePlugin();

    private ExecutionContext buildContext() {
        return ExecutionContext.builder().build();
    }

    // =========================================================================
    // Contrato del plugin
    // =========================================================================

    @Nested
    @DisplayName("Contrato del Plugin")
    class PluginContractTests {

        @Test
        @DisplayName("platformId() retorna 'DATABASE'")
        void platformIdRetornaDatabase() {
            assertThat(plugin.platformId()).isEqualTo("DATABASE");
        }

        @Test
        @DisplayName("displayName() retorna nombre legible")
        void displayNameRetornaNombre() {
            assertThat(plugin.displayName()).isEqualTo("Database Testing");
        }

        @Test
        @DisplayName("describeCapabilities() retorna 4 motores JDBC disponibles")
        void describeCapabilitiesRetorna4Motores() {
            var report = plugin.describeCapabilities();
            assertThat(report.available()).isTrue();
            assertThat(report.platformId()).isEqualTo("DATABASE");
            assertThat(report.options()).extracting("id")
                .containsExactlyInAnyOrder("mysql", "postgresql", "sqlserver", "oracle");
        }

        @Test
        @SuppressWarnings("deprecation")
        @DisplayName("getName() (deprecated) retorna 'database' — alias de platformId en minusculas")
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
        @DisplayName("getOrder() retorna 0 — se inicializa primero")
        void getOrderRetornaCero() {
            assertThat(plugin.getOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("registerServices() registra DatabaseHelper en el registry")
        void registerServicesRegistraDatabaseHelper() {
            ServiceRegistry registry = new ServiceRegistry();
            ExecutionConfig config = new ExecutionConfig.Builder().build();

            plugin.registerServices(registry, config);

            assertThat(registry.isRegistered(DatabaseHelper.class)).isTrue();
        }

        @Test
        @DisplayName("getComponents() declara exactamente 3 componentes (GIVEN/WHEN/THEN)")
        void getComponentsDeclara3Componentes() {
            assertThat(plugin.getComponents()).hasSize(3);
        }

        @Test
        @DisplayName("Los componentes DB cubren las tres fases BDD con nombre y descripción")
        void componentesDbCubrenTresFases() {
            var components = plugin.getComponents();
            assertThat(components).anyMatch(c -> c.getPhase() == BddPhase.GIVEN);
            assertThat(components).anyMatch(c -> c.getPhase() == BddPhase.WHEN);
            assertThat(components).anyMatch(c -> c.getPhase() == BddPhase.THEN);
            components.forEach(c -> {
                assertThat(c.getName()).isNotBlank();
                assertThat(c.getDescription()).isNotBlank();
            });
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
    // Integración con CucumberRuntimeEngine
    // =========================================================================

    @Nested
    @DisplayName("CucumberRuntimeEngine — Descubrimiento de Componentes DB")
    class EngineComponentDiscoveryTests {

        @Test
        @DisplayName("Engine instanciado con DatabasePlugin expone sus 3 componentes")
        void engineConDatabasePluginExponeComponentes() {
            LifecycleManager lm = new DefaultLifecycleManager(List.of(plugin));
            StepDiscoveryService discovery = new StepDiscoveryService(List.of(plugin));
            CucumberRuntimeEngine engine = new CucumberRuntimeEngine(lm, discovery);

            assertThat(engine.getDiscoveryService().getPlugins())
                    .hasSize(1)
                    .extracting(CorePlugin::platformId)
                    .containsExactly("DATABASE");

            assertThat(engine.getDiscoveryService().totalComponents()).isEqualTo(3);
        }

        @Test
        @DisplayName("StepDiscoveryService agrupa los 3 componentes bajo clave 'DATABASE'")
        void discoveryAgrupaPorPlugin() {
            StepDiscoveryService discovery = new StepDiscoveryService(List.of(plugin));

            var componentsByPlugin = discovery.groupByPlugin();
            assertThat(componentsByPlugin).containsKey("DATABASE");
            assertThat(componentsByPlugin.get("DATABASE")).hasSize(3);
        }
    }
}
