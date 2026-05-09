package com.qa.httpcore.factories;

import com.qa.httpcore.implementations.ApacheHttpClientImpl;
import com.qa.httpcore.interfaces.HttpClient;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.HttpEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HttpClientFactory — selección de motor HTTP por ExecutionConfig (TASK-E04)")
class HttpClientFactoryTest {

    private String previousLegacyProp;
    private String previousEngineProp;

    @BeforeEach
    void capturePropsAndResetRegistry() {
        previousLegacyProp = System.getProperty(HttpClientFactory.CLIENT_IMPL_KEY);
        previousEngineProp = System.getProperty(HttpEngine.SYSTEM_PROPERTY);
        System.clearProperty(HttpClientFactory.CLIENT_IMPL_KEY);
        System.clearProperty(HttpEngine.SYSTEM_PROPERTY);
        // Restaurar APACHE (estado inicial del registry) y limpiar cualquier registro custom.
        HttpClientFactory.unregister(HttpEngine.PLAYWRIGHT);
        HttpClientFactory.unregister(HttpEngine.APACHE);
    }

    @AfterEach
    void restoreProps() {
        if (previousLegacyProp == null) System.clearProperty(HttpClientFactory.CLIENT_IMPL_KEY);
        else System.setProperty(HttpClientFactory.CLIENT_IMPL_KEY, previousLegacyProp);
        if (previousEngineProp == null) System.clearProperty(HttpEngine.SYSTEM_PROPERTY);
        else System.setProperty(HttpEngine.SYSTEM_PROPERTY, previousEngineProp);
        HttpClientFactory.unregister(HttpEngine.PLAYWRIGHT);
        HttpClientFactory.unregister(HttpEngine.APACHE);
    }

    @Nested
    @DisplayName("create(ExecutionConfig)")
    class CreateFromConfig {

        @Test
        @DisplayName("APACHE → ApacheHttpClientImpl (pre-registrado)")
        void apacheReturnsApache() {
            ExecutionConfig cfg = new ExecutionConfig.Builder().httpEngine(HttpEngine.APACHE).build();
            HttpClient client = HttpClientFactory.create(cfg);
            assertThat(client).isInstanceOf(ApacheHttpClientImpl.class);
        }

        @Test
        @DisplayName("PLAYWRIGHT sin registrar → fallback a Apache + warning (no falla)")
        void playwrightUnregisteredFallsBackToApache() {
            assertThat(HttpClientFactory.isRegistered(HttpEngine.PLAYWRIGHT)).isFalse();
            ExecutionConfig cfg = new ExecutionConfig.Builder().httpEngine(HttpEngine.PLAYWRIGHT).build();
            HttpClient client = HttpClientFactory.create(cfg);
            assertThat(client).isInstanceOf(ApacheHttpClientImpl.class);
        }

        @Test
        @DisplayName("PLAYWRIGHT registrado por la capa wiring → usa el supplier custom")
        void playwrightRegisteredUsesCustomSupplier() {
            HttpClient stub = new ApacheHttpClientImpl(); // un HttpClient cualquiera para verificar identidad
            HttpClientFactory.register(HttpEngine.PLAYWRIGHT, () -> stub);

            ExecutionConfig cfg = new ExecutionConfig.Builder().httpEngine(HttpEngine.PLAYWRIGHT).build();
            HttpClient client = HttpClientFactory.create(cfg);
            assertThat(client).isSameAs(stub);
        }

        @Test
        @DisplayName("register es idempotente — segunda llamada reemplaza el supplier")
        void registerIsIdempotent() {
            AtomicInteger callsFirst  = new AtomicInteger();
            AtomicInteger callsSecond = new AtomicInteger();
            HttpClientFactory.register(HttpEngine.PLAYWRIGHT, () -> {
                callsFirst.incrementAndGet();
                return new ApacheHttpClientImpl();
            });
            HttpClientFactory.register(HttpEngine.PLAYWRIGHT, () -> {
                callsSecond.incrementAndGet();
                return new ApacheHttpClientImpl();
            });

            HttpClientFactory.create(new ExecutionConfig.Builder()
                    .httpEngine(HttpEngine.PLAYWRIGHT).build());

            assertThat(callsFirst.get()).isZero();
            assertThat(callsSecond.get()).isOne();
        }

        @Test
        @DisplayName("create(null) lanza NPE explícito")
        void createNullConfigThrows() {
            assertThatThrownBy(() -> HttpClientFactory.create(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("config");
        }

        @ParameterizedTest
        @EnumSource(HttpEngine.class)
        @DisplayName("create produce instancia no-null para todo motor del enum (con fallback)")
        void allEnginesProduceNonNull(HttpEngine engine) {
            ExecutionConfig cfg = new ExecutionConfig.Builder().httpEngine(engine).build();
            assertThat(HttpClientFactory.create(cfg)).isNotNull();
        }
    }

    @Nested
    @DisplayName("getInstance() — resolución por contexto / property legacy")
    class GetInstance {

        @Test
        @DisplayName("Sin ExecutionContext → cae a property legacy 'apache' (default)")
        void noContextDefaultsToApache() {
            HttpClient client = HttpClientFactory.getInstance();
            assertThat(client).isInstanceOf(ApacheHttpClientImpl.class);
        }

        @Test
        @DisplayName("Sin ExecutionContext + property http.client=apache → ApacheHttpClientImpl")
        void noContextLegacyApache() {
            System.setProperty(HttpClientFactory.CLIENT_IMPL_KEY, "apache");
            HttpClient client = HttpClientFactory.getInstance();
            assertThat(client).isInstanceOf(ApacheHttpClientImpl.class);
        }

        @Test
        @DisplayName("Sin ExecutionContext + property http.client=otro_inválido → fallback Apache + warning")
        void noContextLegacyInvalidFallsBack() {
            System.setProperty(HttpClientFactory.CLIENT_IMPL_KEY, "no-existe");
            HttpClient client = HttpClientFactory.getInstance();
            assertThat(client).isInstanceOf(ApacheHttpClientImpl.class);
        }
    }

    @Nested
    @DisplayName("Override por system property execution.http.engine")
    class SystemPropertyOverride {

        @Test
        @DisplayName("-Dexecution.http.engine=APACHE + Builder default → APACHE en el config")
        void systemPropertyApacheReachesFactory() {
            System.setProperty(HttpEngine.SYSTEM_PROPERTY, "APACHE");
            ExecutionConfig cfg = new ExecutionConfig.Builder().build(); // sin .httpEngine() explícito
            assertThat(cfg.getHttpEngine()).isEqualTo(HttpEngine.APACHE);
            assertThat(HttpClientFactory.create(cfg)).isInstanceOf(ApacheHttpClientImpl.class);
        }

        @Test
        @DisplayName("-Dexecution.http.engine=PLAYWRIGHT + supplier registrado → engine custom")
        void systemPropertyPlaywrightWithRegistered() {
            HttpClient stub = new ApacheHttpClientImpl();
            HttpClientFactory.register(HttpEngine.PLAYWRIGHT, () -> stub);
            System.setProperty(HttpEngine.SYSTEM_PROPERTY, "PLAYWRIGHT");

            ExecutionConfig cfg = new ExecutionConfig.Builder().build();
            assertThat(cfg.getHttpEngine()).isEqualTo(HttpEngine.PLAYWRIGHT);
            assertThat(HttpClientFactory.create(cfg)).isSameAs(stub);
        }
    }

    @Nested
    @DisplayName("API auxiliar")
    class Helpers {

        @Test
        @DisplayName("getInstanceWithHost setea host y retorna un HttpClient")
        void getInstanceWithHostSetsHost() {
            HttpClient client = HttpClientFactory.getInstanceWithHost("https://api.example.com");
            assertThat(client.getHost()).isEqualTo("https://api.example.com");
        }

        @Test
        @DisplayName("getInstanceWithHost lanza si host es null/blank")
        void getInstanceWithHostRejectsBlank() {
            assertThatThrownBy(() -> HttpClientFactory.getInstanceWithHost(""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> HttpClientFactory.getInstanceWithHost(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getJsonInstance configura content-type JSON")
        void getJsonInstanceConfiguresJson() {
            HttpClient client = HttpClientFactory.getJsonInstance();
            assertThat(client.isJsonContentType()).isTrue();
        }

        @Test
        @DisplayName("getFactoryInfo lista motores registrados")
        void factoryInfoListsEngines() {
            String info = HttpClientFactory.getFactoryInfo();
            assertThat(info).contains("APACHE");
        }

        @Test
        @DisplayName("isRegistered refleja el estado del registry")
        void isRegisteredReflectsState() {
            assertThat(HttpClientFactory.isRegistered(HttpEngine.APACHE)).isTrue();
            assertThat(HttpClientFactory.isRegistered(HttpEngine.PLAYWRIGHT)).isFalse();

            HttpClientFactory.register(HttpEngine.PLAYWRIGHT, ApacheHttpClientImpl::new);
            assertThat(HttpClientFactory.isRegistered(HttpEngine.PLAYWRIGHT)).isTrue();

            HttpClientFactory.unregister(HttpEngine.PLAYWRIGHT);
            assertThat(HttpClientFactory.isRegistered(HttpEngine.PLAYWRIGHT)).isFalse();
        }

        @Test
        @DisplayName("unregister(APACHE) reinstala su default (no lo borra del registry)")
        void unregisterApacheReinstallsDefault() {
            HttpClientFactory.register(HttpEngine.APACHE, () -> {
                throw new IllegalStateException("custom");
            });
            HttpClientFactory.unregister(HttpEngine.APACHE);
            // Tras unregister(APACHE), el supplier vuelve al default.
            ExecutionConfig cfg = new ExecutionConfig.Builder().httpEngine(HttpEngine.APACHE).build();
            assertThat(HttpClientFactory.create(cfg)).isInstanceOf(ApacheHttpClientImpl.class);
        }
    }
}
