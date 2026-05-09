package com.qa.common.runtime;

import org.junit.jupiter.api.*;

import javax.net.ssl.SSLContext;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para {@link ExecutionConfig}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("ExecutionConfig")
class ExecutionConfigTest {

    @Nested
    @DisplayName("Builder - valores por defecto")
    class BuilderDefaultsTests {

        @Test
        @DisplayName("Config con defaults tiene environment 'default'")
        void configConDefaultsTieneEnvironmentDefault() {
            ExecutionConfig config = new ExecutionConfig.Builder().build();
            assertThat(config.getEnvironment()).isEqualTo("default");
        }

        @Test
        @DisplayName("Config con defaults tiene browser vacio")
        void configConDefaultsTieneBrowserVacio() {
            ExecutionConfig config = new ExecutionConfig.Builder().build();
            assertThat(config.getBrowser()).isEmpty();
        }

        @Test
        @DisplayName("Config con defaults tiene tags vacio")
        void configConDefaultsTieneTagsVacio() {
            ExecutionConfig config = new ExecutionConfig.Builder().build();
            assertThat(config.getTags()).isEmpty();
        }

        @Test
        @DisplayName("Config con defaults tiene parallel deshabilitado")
        void configConDefaultsTieneParallelDeshabilitado() {
            ExecutionConfig config = new ExecutionConfig.Builder().build();
            assertThat(config.isParallelEnabled()).isFalse();
        }

        @Test
        @DisplayName("Config con defaults tiene threadCount 1")
        void configConDefaultsTieneThreadCount1() {
            ExecutionConfig config = new ExecutionConfig.Builder().build();
            assertThat(config.getThreadCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Config con defaults tiene properties vacio")
        void configConDefaultsTienePropertiesVacio() {
            ExecutionConfig config = new ExecutionConfig.Builder().build();
            assertThat(config.getProperties()).isEmpty();
        }

        @Test
        @DisplayName("Config con defaults tiene httpEngine PLAYWRIGHT")
        void configConDefaultsTieneHttpEnginePlaywright() {
            String previous = System.getProperty(HttpEngine.SYSTEM_PROPERTY);
            System.clearProperty(HttpEngine.SYSTEM_PROPERTY);
            try {
                ExecutionConfig config = new ExecutionConfig.Builder().build();
                assertThat(config.getHttpEngine()).isEqualTo(HttpEngine.PLAYWRIGHT);
            } finally {
                if (previous != null) {
                    System.setProperty(HttpEngine.SYSTEM_PROPERTY, previous);
                } else {
                    System.clearProperty(HttpEngine.SYSTEM_PROPERTY);
                }
            }
        }

        @Test
        @DisplayName("System property execution.http.engine fuerza el default del Builder")
        void systemPropertyFuerzaDefault() {
            String previous = System.getProperty(HttpEngine.SYSTEM_PROPERTY);
            System.setProperty(HttpEngine.SYSTEM_PROPERTY, "APACHE");
            try {
                ExecutionConfig config = new ExecutionConfig.Builder().build();
                assertThat(config.getHttpEngine()).isEqualTo(HttpEngine.APACHE);
            } finally {
                if (previous != null) {
                    System.setProperty(HttpEngine.SYSTEM_PROPERTY, previous);
                } else {
                    System.clearProperty(HttpEngine.SYSTEM_PROPERTY);
                }
            }
        }

        @Test
        @DisplayName("Builder.httpEngine(APACHE) prevalece sobre system property")
        void builderHttpEnginePrevaleceSobreSystemProperty() {
            String previous = System.getProperty(HttpEngine.SYSTEM_PROPERTY);
            System.setProperty(HttpEngine.SYSTEM_PROPERTY, "PLAYWRIGHT");
            try {
                ExecutionConfig config = new ExecutionConfig.Builder()
                        .httpEngine(HttpEngine.APACHE)
                        .build();
                assertThat(config.getHttpEngine()).isEqualTo(HttpEngine.APACHE);
            } finally {
                if (previous != null) {
                    System.setProperty(HttpEngine.SYSTEM_PROPERTY, previous);
                } else {
                    System.clearProperty(HttpEngine.SYSTEM_PROPERTY);
                }
            }
        }

        @Test
        @DisplayName("Builder.httpEngine(null) no rompe — resuelve al default")
        void builderHttpEngineNullResuelveDefault() {
            ExecutionConfig config = new ExecutionConfig.Builder()
                    .httpEngine(null)
                    .build();
            assertThat(config.getHttpEngine()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Builder - configuracion personalizada")
    class BuilderCustomTests {

        @Test
        @DisplayName("Environment se configura correctamente")
        void environmentSeConfiguraCorrectamente() {
            ExecutionConfig config = new ExecutionConfig.Builder()
                    .environment("staging")
                    .build();
            assertThat(config.getEnvironment()).isEqualTo("staging");
        }

        @Test
        @DisplayName("Browser se configura correctamente")
        void browserSeConfiguraCorrectamente() {
            ExecutionConfig config = new ExecutionConfig.Builder()
                    .browser("chrome")
                    .build();
            assertThat(config.getBrowser()).isEqualTo("chrome");
        }

        @Test
        @DisplayName("Tags se configura correctamente")
        void tagsSeConfiguraCorrectamente() {
            ExecutionConfig config = new ExecutionConfig.Builder()
                    .tags("@api and @smoke")
                    .build();
            assertThat(config.getTags()).isEqualTo("@api and @smoke");
        }

        @Test
        @DisplayName("Parallel enabled se configura correctamente")
        void parallelEnabledSeConfiguraCorrectamente() {
            ExecutionConfig config = new ExecutionConfig.Builder()
                    .parallelEnabled(true)
                    .build();
            assertThat(config.isParallelEnabled()).isTrue();
        }

        @Test
        @DisplayName("ThreadCount se configura correctamente")
        void threadCountSeConfiguraCorrectamente() {
            ExecutionConfig config = new ExecutionConfig.Builder()
                    .threadCount(4)
                    .build();
            assertThat(config.getThreadCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("Configuracion completa con todos los campos")
        void configuracionCompletaConTodosLosCampos() {
            ExecutionConfig config = new ExecutionConfig.Builder()
                    .environment("prod")
                    .browser("firefox")
                    .tags("@regression")
                    .parallelEnabled(true)
                    .threadCount(8)
                    .property("timeout", "30")
                    .property("retry", "3")
                    .build();

            assertThat(config.getEnvironment()).isEqualTo("prod");
            assertThat(config.getBrowser()).isEqualTo("firefox");
            assertThat(config.getTags()).isEqualTo("@regression");
            assertThat(config.isParallelEnabled()).isTrue();
            assertThat(config.getThreadCount()).isEqualTo(8);
            assertThat(config.getProperties()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Builder - validaciones")
    class BuilderValidationTests {

        @Test
        @DisplayName("Environment null lanza NullPointerException")
        void environmentNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionConfig.Builder().environment(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Browser null lanza NullPointerException")
        void browserNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionConfig.Builder().browser(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Tags null lanza NullPointerException")
        void tagsNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionConfig.Builder().tags(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("ThreadCount 0 lanza IllegalArgumentException")
        void threadCount0LanzaIAE() {
            assertThatThrownBy(() -> new ExecutionConfig.Builder().threadCount(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("ThreadCount negativo lanza IllegalArgumentException")
        void threadCountNegativoLanzaIAE() {
            assertThatThrownBy(() -> new ExecutionConfig.Builder().threadCount(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Property key null lanza NullPointerException")
        void propertyKeyNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionConfig.Builder().property(null, "val"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Property value null lanza NullPointerException")
        void propertyValueNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionConfig.Builder().property("key", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Properties map null lanza NullPointerException")
        void propertiesMapNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionConfig.Builder().properties(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Properties")
    class PropertiesTests {

        private ExecutionConfig config;

        @BeforeEach
        void setUp() {
            config = new ExecutionConfig.Builder()
                    .property("timeout", "30")
                    .property("baseUrl", "https://api.test.com")
                    .build();
        }

        @Test
        @DisplayName("getProperty retorna Optional con valor existente")
        void getPropertyRetornaOptionalConValorExistente() {
            Optional<String> value = config.getProperty("timeout");
            assertThat(value).isPresent().contains("30");
        }

        @Test
        @DisplayName("getProperty retorna Optional vacio para clave inexistente")
        void getPropertyRetornaOptionalVacioParaClaveInexistente() {
            Optional<String> value = config.getProperty("noExiste");
            assertThat(value).isEmpty();
        }

        @Test
        @DisplayName("getProperty con default retorna valor existente")
        void getPropertyConDefaultRetornaValorExistente() {
            String value = config.getProperty("timeout", "60");
            assertThat(value).isEqualTo("30");
        }

        @Test
        @DisplayName("getProperty con default retorna default para clave inexistente")
        void getPropertyConDefaultRetornaDefaultParaClaveInexistente() {
            String value = config.getProperty("noExiste", "fallback");
            assertThat(value).isEqualTo("fallback");
        }

        @Test
        @DisplayName("Properties es inmutable - modificar mapa lanza excepcion")
        void propertiesEsInmutable() {
            Map<String, String> props = config.getProperties();
            assertThatThrownBy(() -> props.put("new", "value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Builder properties(Map) agrega multiples propiedades")
        void builderPropertiesMapAgregaMultiples() {
            ExecutionConfig cfg = new ExecutionConfig.Builder()
                    .properties(Map.of("a", "1", "b", "2"))
                    .build();
            assertThat(cfg.getProperties()).hasSize(2)
                    .containsEntry("a", "1")
                    .containsEntry("b", "2");
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString contiene informacion relevante")
        void toStringContieneInformacionRelevante() {
            ExecutionConfig config = new ExecutionConfig.Builder()
                    .environment("dev")
                    .browser("chrome")
                    .build();
            String str = config.toString();
            assertThat(str).contains("dev").contains("chrome").contains("ExecutionConfig");
        }
    }

    @Nested
    @DisplayName("SSL configuration")
    class SslTests {

        @Test
        @DisplayName("Default config tiene sslContext null y trustAllSsl false")
        void defaultConfig_sslContext_isNull_trustAllSsl_isFalse() {
            ExecutionConfig config = new ExecutionConfig.Builder().build();
            assertThat(config.getSslContext()).isNull();
            assertThat(config.isTrustAllSsl()).isFalse();
        }

        @Test
        @DisplayName("trustAllSsl se configura correctamente via builder")
        void trustAllSsl_seConfiguraCorrectamente() {
            ExecutionConfig config = new ExecutionConfig.Builder()
                .trustAllSsl(true)
                .build();
            assertThat(config.isTrustAllSsl()).isTrue();
        }

        @Test
        @DisplayName("sslContext se configura y recupera via builder")
        void sslContext_seConfiguraYRecupera() throws Exception {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);

            ExecutionConfig config = new ExecutionConfig.Builder()
                .sslContext(sslContext)
                .build();

            assertThat(config.getSslContext()).isSameAs(sslContext);
        }

        @Test
        @DisplayName("sslContext null + trustAllSsl false es el estado SYSTEM (JVM default)")
        void nullSslContext_falseTrustAll_isSystemMode() {
            ExecutionConfig config = new ExecutionConfig.Builder()
                .sslContext(null)
                .trustAllSsl(false)
                .build();
            assertThat(config.getSslContext()).isNull();
            assertThat(config.isTrustAllSsl()).isFalse();
        }

        @Test
        @DisplayName("trustAllSsl no afecta sslContext — son independientes")
        void trustAllSsl_doesNotAffect_sslContext() throws Exception {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);

            ExecutionConfig config = new ExecutionConfig.Builder()
                .sslContext(sslContext)
                .trustAllSsl(true)
                .build();

            assertThat(config.getSslContext()).isSameAs(sslContext);
            assertThat(config.isTrustAllSsl()).isTrue();
        }
    }
}

