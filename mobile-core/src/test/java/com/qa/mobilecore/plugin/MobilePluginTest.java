package com.qa.mobilecore.plugin;

import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;
import com.qa.common.runtime.StepComponent;
import com.qa.mobilecore.driver.MobileDriverFactory;
import com.qa.mobilecore.driver.MobileDriverManager;
import com.qa.mobilecore.helper.MobileHelper;
import io.appium.java_client.AppiumDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

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

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
        try { MobileDriverManager.quitDriverSafely(); } catch (Exception ignored) {}
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
            // Solo valida que no se lanza excepcion (el plugin hace auto-scan interno)
            // Si DevicePool.initialize() falla por ausencia de ADB/simctl, es aceptable en CI
            assertThatNoException().isThrownBy(() -> plugin.onScenarioStart(ctx));
        }

        @Test
        @DisplayName("onScenarioEnd() no lanza excepcion cuando registry esta vacio")
        void testOnScenarioEndRegistryVacio() {
            ExecutionContext ctx = ExecutionContext.builder()
                    .registry(new ServiceRegistry())
                    .build();
            // Ningún servicio registrado → get() retorna Optional.empty() en ambos casos
            assertThatNoException().isThrownBy(() -> plugin.onScenarioEnd(ctx));
        }

        @Test
        @DisplayName("onScenarioEnd() no lanza excepcion cuando MobileDriverFactory no fue registrada")
        void testOnScenarioEndSinFactory() {
            ServiceRegistry registry = new ServiceRegistry();
            // Solo helper, sin factory
            registry.registerLazy(MobileHelper.class, MobileHelper::new);
            ExecutionContext ctx = ExecutionContext.builder().registry(registry).build();

            assertThatNoException().isThrownBy(() -> plugin.onScenarioEnd(ctx));
        }

        @Test
        @DisplayName("onScenarioEnd() llama quitIfCreated() en la MobileDriverFactory registrada")
        void testOnScenarioEndLlamaQuitIfCreated() {
            // Arrange: inyectar factory mock con driver activo
            MobileDriverFactory factory = new MobileDriverFactory();
            AppiumDriver mockDriver = Mockito.mock(AppiumDriver.class);
            factory.injectDriver(mockDriver);   // driver "activo" sin servidor real

            ServiceRegistry registry = new ServiceRegistry();
            registry.registerInstance(MobileDriverFactory.class, factory);
            ExecutionContext ctx = ExecutionContext.builder().registry(registry).build();

            // Act
            plugin.onScenarioEnd(ctx);

            // Assert: quit() fue llamado en el driver
            Mockito.verify(mockDriver, Mockito.times(1)).quit();
            assertThat(factory.isDriverCreated()).isFalse();
        }

        @Test
        @DisplayName("onScenarioEnd() absorbe excepcion de quit() sin propagarla")
        void testOnScenarioEndAbsorbeExcepcionDeQuit() {
            MobileDriverFactory factory = new MobileDriverFactory();
            AppiumDriver mockDriver = Mockito.mock(AppiumDriver.class);
            Mockito.doThrow(new RuntimeException("session gone")).when(mockDriver).quit();
            factory.injectDriver(mockDriver);

            ServiceRegistry registry = new ServiceRegistry();
            registry.registerInstance(MobileDriverFactory.class, factory);
            ExecutionContext ctx = ExecutionContext.builder().registry(registry).build();

            assertThatNoException()
                    .as("onScenarioEnd no debe propagar excepciones del driver")
                    .isThrownBy(() -> plugin.onScenarioEnd(ctx));
        }

        @Test
        @DisplayName("onScenarioEnd() es idempotente: segunda llamada con driver ya cerrado no lanza")
        void testOnScenarioEndIdempotente() {
            MobileDriverFactory factory = new MobileDriverFactory();
            AppiumDriver mockDriver = Mockito.mock(AppiumDriver.class);
            factory.injectDriver(mockDriver);

            ServiceRegistry registry = new ServiceRegistry();
            registry.registerInstance(MobileDriverFactory.class, factory);
            ExecutionContext ctx = ExecutionContext.builder().registry(registry).build();

            plugin.onScenarioEnd(ctx);                // primer cierre exitoso
            Mockito.doThrow(new RuntimeException("already closed")).when(mockDriver).quit();

            assertThatNoException().isThrownBy(() -> plugin.onScenarioEnd(ctx)); // segundo cierre
        }

        @Test
        @DisplayName("registerServices() registra MobileDriverFactory en el registry")
        void testRegisterServicesRegistraFactory() {
            ServiceRegistry registry = new ServiceRegistry();
            ExecutionConfig config = new ExecutionConfig.Builder().build();

            plugin.registerServices(registry, config);

            assertThat(registry.isRegistered(MobileDriverFactory.class))
                    .as("MobileDriverFactory debe estar registrada")
                    .isTrue();
        }

        @Test
        @DisplayName("registerServices() registra MobileHelper con factory inyectada (misma instancia)")
        void testRegisterServicesHelperConFactoryInyectada() {
            ServiceRegistry registry = new ServiceRegistry();
            ExecutionConfig config = new ExecutionConfig.Builder().build();

            plugin.registerServices(registry, config);

            // Al resolver ambos, deben ser singletons (misma instancia en llamadas repetidas)
            MobileDriverFactory f1 = registry.get(MobileDriverFactory.class).orElseThrow();
            MobileDriverFactory f2 = registry.get(MobileDriverFactory.class).orElseThrow();
            assertThat(f1).isSameAs(f2);

            MobileHelper h1 = registry.get(MobileHelper.class).orElseThrow();
            MobileHelper h2 = registry.get(MobileHelper.class).orElseThrow();
            assertThat(h1).isSameAs(h2);
        }
    }

    // =========================================================================
    // getGluePackages() — auto-derivación SPI
    // =========================================================================

    @Nested
    @DisplayName("getGluePackages() — derivacion automatica de paquetes")
    class GetGluePackagesTests {

        @Test
        @DisplayName("No retorna lista vacia (los 10 componentes tienen step classes)")
        void noRetornaListaVacia() {
            assertThat(plugin.getGluePackages()).isNotEmpty();
        }

        @Test
        @DisplayName("Contiene el paquete de steps de mobile-core")
        void contieneElPaqueteDeSteps() {
            assertThat(plugin.getGluePackages())
                .anyMatch(pkg -> pkg.startsWith("com.qa.mobilecore"));
        }

        @Test
        @DisplayName("Sin duplicados: multiples componentes en el mismo paquete → un entry")
        void sinDuplicados() {
            assertThat(plugin.getGluePackages()).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("Resultado esta ordenado lexicograficamente")
        void resultadoOrdenado() {
            assertThat(plugin.getGluePackages())
                .isSortedAccordingTo(String::compareTo);
        }

        @Test
        @DisplayName("Resultado es inmutable")
        void resultadoEsInmutable() {
            assertThatThrownBy(() -> plugin.getGluePackages().add("com.hacked"))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // =========================================================================
    // Metadata i18n
    // =========================================================================

    @Nested
    @DisplayName("Metadata i18n por componente")
    class I18nMetadataTests {

        @Test
        @DisplayName("Todos los componentes tienen displayNameByLocale no nulo")
        void todosLosComponentesTienenDisplayNameByLocale() {
            plugin.getComponents().forEach(c ->
                assertThat(c.getDisplayNameByLocale())
                    .as("displayNameByLocale para '%s'", c.getId())
                    .isNotNull()
            );
        }

        @Test
        @DisplayName("Todos los componentes proveen locales ES, EN y FR en displayName")
        void todosLosComponentesTienenTresLocalesEnDisplayName() {
            plugin.getComponents().forEach(c ->
                assertThat(c.getDisplayNameByLocale())
                    .as("locales en displayName para '%s'", c.getId())
                    .containsKeys("es", "en", "fr")
            );
        }

        @Test
        @DisplayName("Todos los componentes proveen locales ES, EN y FR en description")
        void todosLosComponentesTienenTresLocalesEnDescription() {
            plugin.getComponents().forEach(c ->
                assertThat(c.getDescriptionByLocale())
                    .as("locales en description para '%s'", c.getId())
                    .containsKeys("es", "en", "fr")
            );
        }

        @Test
        @DisplayName("Ninguna traduccion de displayName es nula o vacia")
        void ningunaTraduccionDeDisplayNameEsVacia() {
            plugin.getComponents().forEach(c ->
                c.getDisplayNameByLocale().values().forEach(v ->
                    assertThat(v)
                        .as("valor displayName para '%s'", c.getId())
                        .isNotNull().isNotBlank()
                )
            );
        }

        @Test
        @DisplayName("Ninguna traduccion de description es nula o vacia")
        void ningunaTraduccionDeDescriptionEsVacia() {
            plugin.getComponents().forEach(c ->
                c.getDescriptionByLocale().values().forEach(v ->
                    assertThat(v)
                        .as("valor description para '%s'", c.getId())
                        .isNotNull().isNotBlank()
                )
            );
        }

        @Test
        @DisplayName("DeviceConfigComponent tiene displayName EN = 'Device Configuration'")
        void deviceConfigTieneDisplayNameEn() {
            plugin.getComponents().stream()
                .filter(c -> "mobile.device.config".equals(c.getId()))
                .findFirst()
                .ifPresent(c ->
                    assertThat(c.getDisplayNameByLocale().get("en")).isEqualTo("Device Configuration")
                );
        }

        @Test
        @DisplayName("GestureComponent tiene displayName FR = 'Gestes'")
        void gestureComponentTieneDisplayNameFr() {
            plugin.getComponents().stream()
                .filter(c -> "mobile.gesture".equals(c.getId()))
                .findFirst()
                .ifPresent(c ->
                    assertThat(c.getDisplayNameByLocale().get("fr")).isEqualTo("Gestes")
                );
        }

        @Test
        @DisplayName("AppStateValidationComponent tiene displayName EN = 'Application State'")
        void appStateValidationTieneDisplayNameEn() {
            plugin.getComponents().stream()
                .filter(c -> "mobile.validation.app-state".equals(c.getId()))
                .findFirst()
                .ifPresent(c ->
                    assertThat(c.getDisplayNameByLocale().get("en")).isEqualTo("Application State")
                );
        }
    }
}
