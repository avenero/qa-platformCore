package com.qa.webcore.plugin;

import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;
import com.qa.webcore.components.bdd.*;
import com.qa.webcore.driver.DriverManager;
import com.qa.webcore.utils.WebHelper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para {@link WebPlugin}.
 *
 * <p><b>Cobertura:</b>
 * <ul>
 *   <li>Metadatos del plugin (nombre, orden, tags de activación)</li>
 *   <li>Registro de servicios (WebHelper como lazy en {@link ServiceRegistry})</li>
 *   <li>{@code onScenarioStart} — actualmente solo log; verifica que no lanza ni muta el registry</li>
 *   <li>{@code onScenarioEnd} — ídem; verifica que no afecta al {@link DriverManager}</li>
 *   <li>{@code getComponents()} — 16 componentes inmutables correctamente tipados</li>
 * </ul>
 *
 * <p><b>Estrategia:</b> Sin navegador real ni Mockito. Se usan stubs puros de
 * {@link ServiceRegistry} y {@link ExecutionContext#builder()} para construir contextos.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("WebPlugin")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebPluginTest {

    private WebPlugin plugin;
    private ServiceRegistry registry;
    private ExecutionConfig config;

    @BeforeEach
    void setUp() {
        plugin   = new WebPlugin();
        registry = new ServiceRegistry();
        config   = new ExecutionConfig.Builder().environment("test").build();
    }

    @AfterEach
    void tearDown() {
        registry.clear();
        ExecutionContext.deactivate();
        // Garantizar que el DriverManager no quede sucio entre tests
        try { DriverManager.quitDriver(); } catch (Exception ignored) {}
    }

    // =========================================================================
    // 1. Metadatos del plugin
    // =========================================================================

    @Nested
    @DisplayName("1. Metadatos del plugin")
    @Order(1)
    class MetadatosTests {

        @Test
        @DisplayName("getName() retorna 'web'")
        void getNameRetornaWeb() {
            assertThat(plugin.getName()).isEqualTo("web");
        }

        @Test
        @DisplayName("getOrder() retorna 100")
        void getOrderRetornaCien() {
            assertThat(plugin.getOrder()).isEqualTo(100);
        }

        @Test
        @DisplayName("getActivationTags() contiene exactamente 4 tags")
        void getActivationTagsTieneCuatroTags() {
            assertThat(plugin.getActivationTags()).hasSize(4);
        }

        @ParameterizedTest
        @ValueSource(strings = {"@web", "@ui", "@browser", "@selenium"})
        @DisplayName("getActivationTags() incluye el tag: ")
        void getActivationTagsIncluyeTag(String tag) {
            assertThat(plugin.getActivationTags()).contains(tag);
        }
    }

    // =========================================================================
    // 2. registerServices
    // =========================================================================

    @Nested
    @DisplayName("2. registerServices()")
    @Order(2)
    class RegisterServicesTests {

        @Test
        @DisplayName("Registra WebHelper en el registry")
        void registraWebHelper() {
            plugin.registerServices(registry, config);

            assertThat(registry.isRegistered(WebHelper.class)).isTrue();
        }

        @Test
        @DisplayName("WebHelper se instancia lazily en el primer get()")
        void webHelperEsLazy() {
            plugin.registerServices(registry, config);

            // El get() fuerza la inicialización lazy — debe retornar instancia presente
            assertThat(registry.get(WebHelper.class)).isPresent();
        }

        @Test
        @DisplayName("Cada get() de WebHelper retorna la misma instancia (singleton lazy)")
        void webHelperEsSingleton() {
            plugin.registerServices(registry, config);

            WebHelper primera  = registry.get(WebHelper.class).orElseThrow();
            WebHelper segunda  = registry.get(WebHelper.class).orElseThrow();

            assertThat(primera).isSameAs(segunda);
        }

        @Test
        @DisplayName("El registry aumenta en al menos 1 servicio tras registerServices()")
        void registryAumentaConWebHelper() {
            int antes = registry.size();
            plugin.registerServices(registry, config);

            assertThat(registry.size()).isGreaterThan(antes);
        }
    }

    // =========================================================================
    // 3. onScenarioStart
    // =========================================================================

    @Nested
    @DisplayName("3. onScenarioStart()")
    @Order(3)
    class OnScenarioStartTests {

        @Test
        @DisplayName("No lanza excepción con un ExecutionContext recién creado")
        void noLanzaExcepcionConContextoVacio() {
            ExecutionContext ctx = ExecutionContext.builder().build();

            assertThatNoException()
                .isThrownBy(() -> plugin.onScenarioStart(ctx));
        }

        @Test
        @DisplayName("No modifica el registry (implementación actual solo hace log.debug)")
        void noModificaElRegistry() {
            ExecutionContext ctx = ExecutionContext.builder()
                    .registry(registry)
                    .build();
            int sizeBefore = registry.size();

            plugin.onScenarioStart(ctx);

            assertThat(registry.size()).isEqualTo(sizeBefore);
        }

        @Test
        @DisplayName("No registra WebHelper si no estaba previamente en el registry")
        void noInstanciaWebHelperSiAusenteEnRegistry() {
            ExecutionContext ctx = ExecutionContext.builder()
                    .registry(registry)
                    .build();

            plugin.onScenarioStart(ctx);

            assertThat(registry.isRegistered(WebHelper.class)).isFalse();
        }

        @Test
        @DisplayName("No recrea ni reemplaza un WebHelper ya registrado")
        void noReemplazaWebHelperExistente() {
            plugin.registerServices(registry, config);
            WebHelper instanciaOriginal = registry.get(WebHelper.class).orElseThrow();

            ExecutionContext ctx = ExecutionContext.builder()
                    .registry(registry)
                    .build();
            plugin.onScenarioStart(ctx);

            assertThat(registry.get(WebHelper.class).orElseThrow())
                .isSameAs(instanciaOriginal);
        }
    }

    // =========================================================================
    // 4. onScenarioEnd
    // =========================================================================

    @Nested
    @DisplayName("4. onScenarioEnd()")
    @Order(4)
    class OnScenarioEndTests {

        @Test
        @DisplayName("No lanza excepción con un ExecutionContext recién creado")
        void noLanzaExcepcionConContextoVacio() {
            ExecutionContext ctx = ExecutionContext.builder().build();

            assertThatNoException()
                .isThrownBy(() -> plugin.onScenarioEnd(ctx));
        }

        @Test
        @DisplayName("No modifica el registry (implementación actual solo hace log.debug)")
        void noModificaElRegistry() {
            plugin.registerServices(registry, config);
            ExecutionContext ctx = ExecutionContext.builder()
                    .registry(registry)
                    .build();
            int sizeBefore = registry.size();

            plugin.onScenarioEnd(ctx);

            assertThat(registry.size()).isEqualTo(sizeBefore);
        }

        @Test
        @DisplayName("No inicializa ni cierra el WebDriver (DriverManager no afectado)")
        void noAfectaDriverManager() {
            // La implementación actual de onScenarioEnd solo hace log.debug,
            // no debe tocar DriverManager (eso lo hace WebHooksSteps con @After)
            ExecutionContext ctx = ExecutionContext.builder().build();

            plugin.onScenarioEnd(ctx);

            assertThat(DriverManager.isDriverInitialized()).isFalse();
        }

        @Test
        @DisplayName("No cierra ni destruye el WebHelper si estaba registrado")
        void noDestruyeWebHelperRegistrado() {
            plugin.registerServices(registry, config);
            WebHelper instancia = registry.get(WebHelper.class).orElseThrow();

            ExecutionContext ctx = ExecutionContext.builder()
                    .registry(registry)
                    .build();
            plugin.onScenarioEnd(ctx);

            // La instancia debe seguir disponible tras onScenarioEnd
            assertThat(registry.get(WebHelper.class).orElseThrow())
                .isSameAs(instancia);
        }
    }

    // =========================================================================
    // 5. getComponents
    // =========================================================================

    @Nested
    @DisplayName("5. getComponents()")
    @Order(5)
    class GetComponentsTests {

        @Test
        @DisplayName("Retorna exactamente 16 componentes")
        void retornaDieciseisComponentes() {
            assertThat(plugin.getComponents()).hasSize(16);
        }

        @Test
        @DisplayName("La lista no es null")
        void listaNoEsNull() {
            assertThat(plugin.getComponents()).isNotNull();
        }

        @Test
        @DisplayName("La lista es inmutable (List.of)")
        void listaEsInmutable() {
            assertThatThrownBy(() -> plugin.getComponents().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Incluye los 2 componentes de Configuración")
        void incluyeComponentesDeConfiguracion() {
            assertThat(plugin.getComponents())
                .anyMatch(c -> c instanceof BrowserConfigComponent)
                .anyMatch(c -> c instanceof WebEnvironmentComponent);
        }

        @Test
        @DisplayName("Incluye los 3 componentes de Navegación")
        void incluyeComponentesDeNavegacion() {
            assertThat(plugin.getComponents())
                .anyMatch(c -> c instanceof NavigationComponent)
                .anyMatch(c -> c instanceof FrameComponent)
                .anyMatch(c -> c instanceof WindowComponent);
        }

        @Test
        @DisplayName("Incluye los 6 componentes de Interacción")
        void incluyeComponentesDeInteraccion() {
            assertThat(plugin.getComponents())
                .anyMatch(c -> c instanceof ClickComponent)
                .anyMatch(c -> c instanceof InputComponent)
                .anyMatch(c -> c instanceof SelectComponent)
                .anyMatch(c -> c instanceof ScrollComponent)
                .anyMatch(c -> c instanceof DragDropComponent)
                .anyMatch(c -> c instanceof AlertComponent);
        }

        @Test
        @DisplayName("Incluye el componente de Esperas (WaitComponent)")
        void incluyeWaitComponent() {
            assertThat(plugin.getComponents())
                .anyMatch(c -> c instanceof WaitComponent);
        }

        @Test
        @DisplayName("Incluye los 4 componentes de Validación")
        void incluyeComponentesDeValidacion() {
            assertThat(plugin.getComponents())
                .anyMatch(c -> c instanceof ElementValidationComponent)
                .anyMatch(c -> c instanceof PageValidationComponent)
                .anyMatch(c -> c instanceof TableValidationComponent)
                .anyMatch(c -> c instanceof ScreenshotComponent);
        }
    }
}

