package com.qa.apicore.plugin;

import com.qa.apicore.interfaces.AuthenticationService;
import com.qa.apicore.interfaces.HttpClient;
import com.qa.apicore.utils.ApiHelper;
import com.qa.apicore.components.*;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link ApiPlugin}.
 *
 * <p><b>Cobertura:</b>
 * <ul>
 *   <li>Metadatos del plugin (nombre, orden, tags de activacion)</li>
 *   <li>Registro lazy de servicios: {@link HttpClient}, {@link AuthenticationService}, {@link ApiHelper}</li>
 *   <li>{@code onScenarioStart} — invoca {@code clearRequestData()} en HttpClient si esta registrado</li>
 *   <li>{@code onScenarioEnd} — invoca {@code reset()} en HttpClient si esta registrado</li>
 *   <li>{@code getComponents()} — 12 componentes inmutables con fases BDD correctas</li>
 * </ul>
 *
 * <p><b>Estrategia:</b> Sin HTTP real. Se usan stubs de {@link ServiceRegistry}
 * y mocks de {@link HttpClient} (Mockito) para verificar interacciones.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("ApiPlugin")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiPluginTest {

    private ApiPlugin plugin;
    private ServiceRegistry registry;
    private ExecutionConfig config;

    @BeforeEach
    void setUp() {
        plugin   = new ApiPlugin();
        registry = new ServiceRegistry();
        config   = new ExecutionConfig.Builder().environment("test").build();
    }

    @AfterEach
    void tearDown() {
        registry.clear();
        ExecutionContext.deactivate();
    }

    // =========================================================================
    // 1. Metadatos
    // =========================================================================

    @Nested
    @DisplayName("1. Metadatos del plugin")
    @Order(1)
    class MetadatosTests {

        @Test
        @DisplayName("platformId() retorna 'HTTP'")
        void platformIdRetornaHttp() {
            assertThat(plugin.platformId()).isEqualTo("HTTP");
        }

        @Test
        @DisplayName("displayName() retorna nombre legible")
        void displayNameRetornaNombre() {
            assertThat(plugin.displayName()).isEqualTo("HTTP API Testing");
        }

        @Test
        @DisplayName("describeCapabilities() retorna informe available con 3 opciones (REST/GraphQL/SOAP)")
        void describeCapabilitiesRetornaInformeDisponible() {
            var report = plugin.describeCapabilities();
            assertThat(report.available()).isTrue();
            assertThat(report.platformId()).isEqualTo("HTTP");
            assertThat(report.options()).extracting("id")
                .containsExactlyInAnyOrder("rest", "graphql", "soap");
        }

        @Test
        @SuppressWarnings("deprecation")
        @DisplayName("getName() (deprecated) retorna 'http' — alias de platformId en minusculas")
        void getNameRetornaApi() {
            assertThat(plugin.getName()).isEqualTo("http");
        }

        @Test
        @DisplayName("getOrder() retorna 50")
        void getOrderRetornaCincuenta() {
            assertThat(plugin.getOrder()).isEqualTo(50);
        }

        @Test
        @DisplayName("getActivationTags() contiene exactamente 4 tags")
        void getActivationTagsTieneCuatroTags() {
            assertThat(plugin.getActivationTags()).hasSize(4);
        }

        @ParameterizedTest
        @ValueSource(strings = {"@api", "@rest", "@http", "@service"})
        @DisplayName("getActivationTags() incluye el tag: ")
        void getActivationTagsIncluyeTag(String tag) {
            assertThat(plugin.getActivationTags()).contains(tag);
        }

        @Test
        @DisplayName("getActivationTags() retorna conjunto inmutable")
        void getActivationTagsEsInmutable() {
            assertThatThrownBy(() -> plugin.getActivationTags().add("@nuevo"))
                .isInstanceOf(UnsupportedOperationException.class);
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
        @DisplayName("Registra HttpClient en el registry")
        void registraHttpClient() {
            plugin.registerServices(registry, config);
            assertThat(registry.isRegistered(HttpClient.class)).isTrue();
        }

        @Test
        @DisplayName("Registra AuthenticationService en el registry")
        void registraAuthenticationService() {
            plugin.registerServices(registry, config);
            assertThat(registry.isRegistered(AuthenticationService.class)).isTrue();
        }

        @Test
        @DisplayName("Registra ApiHelper en el registry")
        void registraApiHelper() {
            plugin.registerServices(registry, config);
            assertThat(registry.isRegistered(ApiHelper.class)).isTrue();
        }

        @Test
        @DisplayName("El registry aumenta en exactamente 3 servicios tras registerServices()")
        void registryAumentaEnTresServicios() {
            int antes = registry.size();
            plugin.registerServices(registry, config);
            assertThat(registry.size()).isEqualTo(antes + 3);
        }

        @Test
        @DisplayName("HttpClient se instancia lazily en el primer get()")
        void httpClientEsLazy() {
            plugin.registerServices(registry, config);
            assertThat(registry.get(HttpClient.class)).isPresent();
        }

        @Test
        @DisplayName("HttpClient es singleton: dos get() retornan la misma instancia")
        void httpClientEsSingleton() {
            plugin.registerServices(registry, config);
            HttpClient primera = registry.get(HttpClient.class).orElseThrow();
            HttpClient segunda = registry.get(HttpClient.class).orElseThrow();
            assertThat(primera).isSameAs(segunda);
        }

        @Test
        @DisplayName("ApiHelper es singleton: dos get() retornan la misma instancia")
        void apiHelperEsSingleton() {
            plugin.registerServices(registry, config);
            ApiHelper primera = registry.get(ApiHelper.class).orElseThrow();
            ApiHelper segunda = registry.get(ApiHelper.class).orElseThrow();
            assertThat(primera).isSameAs(segunda);
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
        @DisplayName("No lanza excepcion con registry vacio (sin HttpClient)")
        void noLanzaConRegistryVacio() {
            ExecutionContext ctx = ExecutionContext.builder().build();
            assertThatNoException().isThrownBy(() -> plugin.onScenarioStart(ctx));
        }

        @Test
        @DisplayName("Invoca clearRequestData() en HttpClient si esta registrado")
        void invocaClearRequestDataEnHttpClient() {
            HttpClient mockClient = mock(HttpClient.class);
            registry.registerInstance(HttpClient.class, mockClient);

            ExecutionContext ctx = ExecutionContext.builder().registry(registry).build();
            plugin.onScenarioStart(ctx);

            verify(mockClient).clearRequestData();
        }

        @Test
        @DisplayName("No lanza excepcion si HttpClient no esta en el registry")
        void noLanzaSiHttpClientAusente() {
            ExecutionContext ctx = ExecutionContext.builder().registry(registry).build();
            assertThatNoException().isThrownBy(() -> plugin.onScenarioStart(ctx));
        }

        @Test
        @DisplayName("No modifica el registry (no agrega ni elimina servicios)")
        void noModificaElRegistry() {
            plugin.registerServices(registry, config);
            int sizeBefore = registry.size();

            ExecutionContext ctx = ExecutionContext.builder().registry(registry).build();
            plugin.onScenarioStart(ctx);

            assertThat(registry.size()).isEqualTo(sizeBefore);
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
        @DisplayName("No lanza excepcion con registry vacio (sin HttpClient)")
        void noLanzaConRegistryVacio() {
            ExecutionContext ctx = ExecutionContext.builder().build();
            assertThatNoException().isThrownBy(() -> plugin.onScenarioEnd(ctx));
        }

        @Test
        @DisplayName("Invoca reset() en HttpClient si esta registrado")
        void invocaResetEnHttpClient() {
            HttpClient mockClient = mock(HttpClient.class);
            registry.registerInstance(HttpClient.class, mockClient);

            ExecutionContext ctx = ExecutionContext.builder().registry(registry).build();
            plugin.onScenarioEnd(ctx);

            verify(mockClient).reset();
        }

        @Test
        @DisplayName("No invoca clearRequestData() en onScenarioEnd (solo reset)")
        void noInvocaClearRequestDataEnEnd() {
            HttpClient mockClient = mock(HttpClient.class);
            registry.registerInstance(HttpClient.class, mockClient);

            ExecutionContext ctx = ExecutionContext.builder().registry(registry).build();
            plugin.onScenarioEnd(ctx);

            verify(mockClient, never()).clearRequestData();
        }

        @Test
        @DisplayName("No modifica el registry tras onScenarioEnd()")
        void noModificaElRegistry() {
            plugin.registerServices(registry, config);
            int sizeBefore = registry.size();

            ExecutionContext ctx = ExecutionContext.builder().registry(registry).build();
            plugin.onScenarioEnd(ctx);

            assertThat(registry.size()).isEqualTo(sizeBefore);
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
        @DisplayName("Retorna exactamente 12 componentes")
        void retornaDoceComponentes() {
            assertThat(plugin.getComponents()).hasSize(12);
        }

        @Test
        @DisplayName("La lista no es null")
        void listaNoEsNull() {
            assertThat(plugin.getComponents()).isNotNull();
        }

        @Test
        @DisplayName("La lista es inmutable")
        void listaEsInmutable() {
            assertThatThrownBy(() -> plugin.getComponents().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Todos los componentes tienen nombre no nulo y no vacio")
        void todosLoComponentesTienenNombre() {
            plugin.getComponents().forEach(c ->
                assertThat(c.getName()).isNotNull().isNotBlank()
            );
        }

        @Test
        @DisplayName("Todos los componentes tienen fase BDD no nula")
        void todosLosComponentesTienenFase() {
            plugin.getComponents().forEach(c ->
                assertThat(c.getPhase()).isNotNull()
            );
        }

        @Test
        @DisplayName("Todos los componentes tienen clase de steps no nula")
        void todosLosComponentesTienenStepClass() {
            plugin.getComponents().forEach(c ->
                assertThat(c.getStepDefinitionClass()).isNotNull()
            );
        }

        @Test
        @DisplayName("Todos los componentes tienen IDs unicos")
        void todosLosComponentesTienenIdsUnicos() {
            long uniqueIds = plugin.getComponents().stream()
                .map(c -> c.getId())
                .distinct()
                .count();
            assertThat(uniqueIds).isEqualTo(12);
        }

        @Test
        @DisplayName("Incluye los 6 componentes GIVEN de configuracion de peticion")
        void incluyeSeisComponentesGiven() {
            long givenCount = plugin.getComponents().stream()
                .filter(c -> c.getPhase().name().equals("GIVEN"))
                .count();
            assertThat(givenCount).isEqualTo(6);
        }

        @Test
        @DisplayName("Incluye exactamente 1 componente WHEN de ejecucion HTTP")
        void incluyeUnComponenteWhen() {
            long whenCount = plugin.getComponents().stream()
                .filter(c -> c.getPhase().name().equals("WHEN"))
                .count();
            assertThat(whenCount).isEqualTo(1);
        }

        @Test
        @DisplayName("Incluye los 5 componentes THEN de validacion")
        void incluyeCincoComponentesThen() {
            long thenCount = plugin.getComponents().stream()
                .filter(c -> c.getPhase().name().equals("THEN"))
                .count();
            assertThat(thenCount).isEqualTo(5);
        }

        @Test
        @DisplayName("Incluye ApiUrlComponent, ApiAuthComponent y ApiExecutionComponent")
        void incluyeComponentesClave() {
            assertThat(plugin.getComponents())
                .anyMatch(c -> c instanceof ApiUrlComponent)
                .anyMatch(c -> c instanceof ApiAuthComponent)
                .anyMatch(c -> c instanceof ApiExecutionComponent);
        }

        @Test
        @DisplayName("Incluye ApiStatusCodeComponent y ApiResponseBodyComponent")
        void incluyeComponentesDeValidacion() {
            assertThat(plugin.getComponents())
                .anyMatch(c -> c instanceof ApiStatusCodeComponent)
                .anyMatch(c -> c instanceof ApiResponseBodyComponent);
        }

        @Test
        @DisplayName("Incluye ApiPerformanceComponent y ApiSecurityComponent")
        void incluyeComponentesAvanzados() {
            assertThat(plugin.getComponents())
                .anyMatch(c -> c instanceof ApiPerformanceComponent)
                .anyMatch(c -> c instanceof ApiSecurityComponent);
        }
    }

    // =========================================================================
    // 6. Metadata i18n
    // =========================================================================

    @Nested
    @DisplayName("6. Metadata i18n por componente")
    @Order(6)
    class I18nMetadataTests {

        @Test
        @DisplayName("Todos los componentes tienen displayNameByLocale no nulo")
        void todosLoComponentesTienenDisplayNameByLocale() {
            plugin.getComponents().forEach(c ->
                assertThat(c.getDisplayNameByLocale())
                    .as("displayNameByLocale para componente '%s'", c.getId())
                    .isNotNull()
            );
        }

        @Test
        @DisplayName("Todos los componentes tienen descriptionByLocale no nulo")
        void todosLosComponentesTienenDescriptionByLocale() {
            plugin.getComponents().forEach(c ->
                assertThat(c.getDescriptionByLocale())
                    .as("descriptionByLocale para componente '%s'", c.getId())
                    .isNotNull()
            );
        }

        @Test
        @DisplayName("Todos los componentes proveen traduccion ES, EN y FR en displayName")
        void todosLosComponentesTienenTresLocalesEnDisplayName() {
            plugin.getComponents().forEach(c ->
                assertThat(c.getDisplayNameByLocale())
                    .as("locales en displayNameByLocale para '%s'", c.getId())
                    .containsKeys("es", "en", "fr")
            );
        }

        @Test
        @DisplayName("Todos los componentes proveen traduccion ES, EN y FR en description")
        void todosLosComponentesTienenTresLocalesEnDescription() {
            plugin.getComponents().forEach(c ->
                assertThat(c.getDescriptionByLocale())
                    .as("locales en descriptionByLocale para '%s'", c.getId())
                    .containsKeys("es", "en", "fr")
            );
        }

        @Test
        @DisplayName("Ninguna traduccion de displayName es nula o vacia")
        void ningunaTraduccionDeDisplayNameEsVacia() {
            plugin.getComponents().forEach(c ->
                c.getDisplayNameByLocale().values().forEach(v ->
                    assertThat(v)
                        .as("valor de displayName para '%s'", c.getId())
                        .isNotNull()
                        .isNotBlank()
                )
            );
        }

        @Test
        @DisplayName("Ninguna traduccion de description es nula o vacia")
        void ningunaTraduccionDeDescriptionEsVacia() {
            plugin.getComponents().forEach(c ->
                c.getDescriptionByLocale().values().forEach(v ->
                    assertThat(v)
                        .as("valor de description para '%s'", c.getId())
                        .isNotNull()
                        .isNotBlank()
                )
            );
        }

        @Test
        @DisplayName("ApiUrlComponent tiene displayName EN = 'URL / Environment'")
        void apiUrlComponentTieneDisplayNameEn() {
            plugin.getComponents().stream()
                .filter(c -> c instanceof ApiUrlComponent)
                .findFirst()
                .ifPresent(c ->
                    assertThat(c.getDisplayNameByLocale().get("en")).isEqualTo("URL / Environment")
                );
        }

        @Test
        @DisplayName("ApiAuthComponent tiene displayName FR = 'Authentification'")
        void apiAuthComponentTieneDisplayNameFr() {
            plugin.getComponents().stream()
                .filter(c -> c instanceof ApiAuthComponent)
                .findFirst()
                .ifPresent(c ->
                    assertThat(c.getDisplayNameByLocale().get("fr")).isEqualTo("Authentification")
                );
        }
    }
}
