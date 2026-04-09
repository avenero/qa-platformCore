package com.qa.mobilecore.plugin;

import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;
import com.qa.common.runtime.StepComponent;
import com.qa.mobilecore.helper.MobileHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para MobilePlugin.
 *
 * <p>Verifica metadatos del plugin, registro de servicios y declaracion
 * de componentes de steps con sus metadatos para el FE/BE.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("MobilePlugin")
class MobilePluginTest {

    private MobilePlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = new MobilePlugin();
    }

    // =========================================================================

    @Nested
    @DisplayName("Metadatos del plugin")
    class MetadatosTest {

        @Test
        @DisplayName("getName() debe retornar 'mobile'")
        void testGetName() {
            assertThat(plugin.getName()).isEqualTo("mobile");
        }

        @Test
        @DisplayName("getOrder() debe ser 150 (inicializa despues de api=50 y web=100)")
        void testGetOrder() {
            assertThat(plugin.getOrder()).isEqualTo(150);
        }

        @Test
        @DisplayName("getActivationTags() debe incluir @mobile, @ios, @android, @appium")
        void testGetActivationTags() {
            assertThat(plugin.getActivationTags())
                    .containsExactlyInAnyOrder("@mobile", "@ios", "@android", "@appium");
        }

        @Test
        @DisplayName("getActivationTags() retorna set inmutable")
        void testActivationTagsIsImmutable() {
            var tags = plugin.getActivationTags();
            assertThat(tags).isNotNull();
            org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> tags.add("@newTag"),
                "El set de activation tags debe ser inmutable"
            );
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("registerServices()")
    class RegisterServicesTest {

        @Test
        @DisplayName("Debe registrar MobileHelper como lazy en el registry")
        void testRegistersMobileHelper() {
            ServiceRegistry registry = new ServiceRegistry();
            ExecutionConfig config = new ExecutionConfig.Builder().build();

            plugin.registerServices(registry, config);

            assertThat(registry.isRegistered(MobileHelper.class))
                    .as("MobileHelper debe estar registrado")
                    .isTrue();
        }

        @Test
        @DisplayName("MobileHelper debe ser singleton (misma instancia en llamadas repetidas)")
        void testMobileHelperIsSingleton() {
            ServiceRegistry registry = new ServiceRegistry();
            ExecutionConfig config = new ExecutionConfig.Builder().build();
            plugin.registerServices(registry, config);

            MobileHelper first  = registry.require(MobileHelper.class);
            MobileHelper second = registry.require(MobileHelper.class);

            assertThat(first).isSameAs(second);
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("getComponents()")
    class GetComponentsTest {

        @Test
        @DisplayName("Debe retornar exactamente 10 componentes")
        void testReturnsTenComponents() {
            assertThat(plugin.getComponents()).hasSize(10);
        }

        @Test
        @DisplayName("Debe retornar lista inmutable")
        void testComponentsListIsImmutable() {
            List<StepComponent> components = plugin.getComponents();
            org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> components.add(null)
            );
        }

        @Test
        @DisplayName("Todos los componentes deben tener nombre no nulo")
        void testAllComponentsHaveName() {
            plugin.getComponents().forEach(c ->
                assertThat(c.getName()).as("Nombre de %s", c.getClass().getSimpleName()).isNotBlank()
            );
        }

        @Test
        @DisplayName("Todos los componentes deben tener fase BDD no nula")
        void testAllComponentsHavePhase() {
            plugin.getComponents().forEach(c ->
                assertThat(c.getPhase()).as("Fase de %s", c.getClass().getSimpleName()).isNotNull()
            );
        }

        @Test
        @DisplayName("Todos los componentes deben tener clase de step definida")
        void testAllComponentsHaveStepClass() {
            plugin.getComponents().forEach(c ->
                assertThat(c.getStepDefinitionClass())
                    .as("StepClass de %s", c.getClass().getSimpleName())
                    .isNotNull()
            );
        }

        @Test
        @DisplayName("Debe incluir componentes de configuracion (GIVEN)")
        void testHasGivenComponents() {
            long givenCount = plugin.getComponents().stream()
                    .filter(c -> c.getPhase() == BddPhase.GIVEN)
                    .count();
            assertThat(givenCount)
                    .as("Debe haber al menos un componente GIVEN")
                    .isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Debe incluir componentes de interaccion (WHEN)")
        void testHasWhenComponents() {
            long whenCount = plugin.getComponents().stream()
                    .filter(c -> c.getPhase() == BddPhase.WHEN)
                    .count();
            assertThat(whenCount)
                    .as("Debe haber al menos un componente WHEN")
                    .isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Debe incluir componentes de validacion (THEN)")
        void testHasThenComponents() {
            long thenCount = plugin.getComponents().stream()
                    .filter(c -> c.getPhase() == BddPhase.THEN)
                    .count();
            assertThat(thenCount)
                    .as("Debe haber al menos un componente THEN")
                    .isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Los IDs de componentes deben ser unicos")
        void testComponentIdsAreUnique() {
            List<String> ids = plugin.getComponents().stream()
                    .map(StepComponent::getId)
                    .toList();
            assertThat(ids).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("Los nombres de componentes deben ser unicos")
        void testComponentNamesAreUnique() {
            List<String> names = plugin.getComponents().stream()
                    .map(StepComponent::getName)
                    .toList();
            assertThat(names).doesNotHaveDuplicates();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("Lifecycle hooks")
    class LifecycleTest {

        @Test
        @DisplayName("onScenarioStart() no lanza excepcion con context valido")
        void testOnScenarioStartNoException() {
            ExecutionContext ctx = ExecutionContext.builder().scenarioId("test-mobile").build();
            ctx.activate();
            try {
                // Solo valida que no se lanza excepcion (el plugin hace auto-scan interno)
                // Si DevicePool.initialize() falla por ausencia de ADB/simctl, es aceptable en CI
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> plugin.onScenarioStart(ctx)
                );
            } finally {
                ExecutionContext.deactivate();
            }
        }

        @Test
        @DisplayName("onScenarioEnd() no lanza excepcion cuando MobileHelper no fue creado")
        void testOnScenarioEndWithoutDriver() {
            ExecutionContext ctx = ExecutionContext.builder().scenarioId("test-mobile-end").build();
            ctx.activate();
            try {
                // MobileHelper no fue registrado → get() debe retornar Optional.empty()
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> plugin.onScenarioEnd(ctx)
                );
            } finally {
                ExecutionContext.deactivate();
            }
        }
    }
}
