package com.qa.httpcore.factories;

import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.HttpEngine;
import com.qa.httpcore.bootstrap.HttpEngineBootstrap;
import com.qa.httpcore.implementations.ApacheHttpClientImpl;
import com.qa.httpcore.implementations.PlaywrightHttpEngine;
import com.qa.httpcore.interfaces.HttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guard anti-regresión (FEC-API-SHIP-CORE): impide el regreso del "default advertido pero
 * no-registrado". {@link HttpEngine#resolveDefault()} y el FE anuncian {@code PLAYWRIGHT}
 * como motor por defecto; antes de {@link HttpEngineBootstrap#register()} el factory sólo
 * tenía {@code APACHE}, por lo que toda ejecución API caía silenciosamente a Apache. Estos
 * tests fijan que, tras el bootstrap, el motor por defecto resuelve a {@link PlaywrightHttpEngine}.
 */
@DisplayName("HttpEngineDefaultRegisteredTest — PLAYWRIGHT registrado como default tras el bootstrap")
class HttpEngineDefaultRegisteredTest {

    private String previousEngineProp;

    @BeforeEach
    void setUp() {
        previousEngineProp = System.getProperty(HttpEngine.SYSTEM_PROPERTY);
        System.clearProperty(HttpEngine.SYSTEM_PROPERTY);
        // Estado de partida: PLAYWRIGHT sin registrar (como en el static init del factory).
        HttpClientFactory.unregister(HttpEngine.PLAYWRIGHT);
    }

    @AfterEach
    void tearDown() {
        if (previousEngineProp == null) {
            System.clearProperty(HttpEngine.SYSTEM_PROPERTY);
        } else {
            System.setProperty(HttpEngine.SYSTEM_PROPERTY, previousEngineProp);
        }
        // No dejar PLAYWRIGHT registrado: otros tests (HttpClientFactoryTest) asumen el
        // estado base con sólo APACHE.
        HttpClientFactory.unregister(HttpEngine.PLAYWRIGHT);
    }

    @Test
    @DisplayName("El default advertido por resolveDefault() es PLAYWRIGHT")
    void defaultEngineIsPlaywright() {
        assertThat(HttpEngine.resolveDefault()).isEqualTo(HttpEngine.PLAYWRIGHT);
    }

    @Test
    @DisplayName("Antes del bootstrap, PLAYWRIGHT cae a Apache (reproduce el no-op que cerramos)")
    void beforeBootstrapPlaywrightFallsBackToApache() {
        assertThat(HttpClientFactory.isRegistered(HttpEngine.PLAYWRIGHT)).isFalse();
        ExecutionConfig cfg = new ExecutionConfig.Builder().httpEngine(HttpEngine.PLAYWRIGHT).build();
        assertThat(HttpClientFactory.create(cfg)).isInstanceOf(ApacheHttpClientImpl.class);
    }

    @Test
    @DisplayName("Tras el bootstrap, PLAYWRIGHT explícito crea PlaywrightHttpEngine, NO Apache")
    void afterBootstrapPlaywrightCreatesPlaywrightEngine() {
        HttpEngineBootstrap.register();

        ExecutionConfig cfg = new ExecutionConfig.Builder().httpEngine(HttpEngine.PLAYWRIGHT).build();
        HttpClient client = HttpClientFactory.create(cfg);

        assertThat(client)
                .isInstanceOf(PlaywrightHttpEngine.class)
                .isNotInstanceOf(ApacheHttpClientImpl.class);
    }

    @Test
    @DisplayName("Tras el bootstrap, una config con el motor por defecto crea PlaywrightHttpEngine")
    void afterBootstrapDefaultConfigCreatesPlaywrightEngine() {
        HttpEngineBootstrap.register();

        // Sin httpEngine explícito: el Builder resuelve el default (PLAYWRIGHT).
        ExecutionConfig cfg = new ExecutionConfig.Builder().build();
        assertThat(cfg.getHttpEngine()).isEqualTo(HttpEngine.PLAYWRIGHT);

        HttpClient client = HttpClientFactory.create(cfg);
        assertThat(client)
                .isInstanceOf(PlaywrightHttpEngine.class)
                .isNotInstanceOf(ApacheHttpClientImpl.class);
    }
}
