package com.scotia.qa.webcore.utils;

import com.scotia.qa.webcore.driver.WebDriverFactory;
import com.scotia.qa.webcore.driver.WebDriverFactory.BrowserType;
import com.scotia.qa.webcore.driver.WebDriverFactory.DriverConfig;
import com.scotia.qa.webcore.driver.WebDriverFactory.ExecutionMode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para WebDriverFactory.DriverConfig (builder pattern).
 *
 * <p><b>Estrategia:</b> Solo se prueba el builder pattern (DriverConfig) ya que
 * createDriver() requiere un navegador real instalado.</p>
 *
 * @author Abel Venero
 * @since 1.0.0
 */
@DisplayName("WebDriverFactory.DriverConfig (builder)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebDriverFactoryConfigTest {

    // =========================================================================
    // Constructor y valores por defecto
    // =========================================================================

    @Nested
    @DisplayName("DriverConfig - valores por defecto")
    @Order(1)
    class DefaultValuesTests {

        @Test
        @DisplayName("DriverConfig crea config con browser indicado")
        void configuracionConBrowserCorrecto() {
            DriverConfig config = new DriverConfig(BrowserType.CHROME);
            assertThat(config.getBrowserType()).isEqualTo(BrowserType.CHROME);
        }

        @Test
        @DisplayName("Por defecto el modo es LOCAL")
        void modoEsLocalPorDefecto() {
            DriverConfig config = new DriverConfig(BrowserType.CHROME);
            assertThat(config.getExecutionMode()).isEqualTo(ExecutionMode.LOCAL);
        }

        @Test
        @DisplayName("Por defecto headless es false")
        void headlessEsFalsePorDefecto() {
            DriverConfig config = new DriverConfig(BrowserType.CHROME);
            assertThat(config.isHeadless()).isFalse();
        }

        @Test
        @DisplayName("Por defecto acceptInsecureCerts es true")
        void acceptInsecureCertsTruePorDefecto() {
            DriverConfig config = new DriverConfig(BrowserType.CHROME);
            assertThat(config.isAcceptInsecureCerts()).isTrue();
        }
    }

    // =========================================================================
    // Builder methods
    // =========================================================================

    @Nested
    @DisplayName("DriverConfig - builder methods")
    @Order(2)
    class BuilderMethodsTests {

        @Test
        @DisplayName("withHeadless(true) activa modo headless")
        void headlessTrueActiva() {
            DriverConfig config = new DriverConfig(BrowserType.CHROME)
                .withHeadless(true);
            assertThat(config.isHeadless()).isTrue();
        }

        @Test
        @DisplayName("withHeadless(false) desactiva modo headless")
        void headlessFalseDesactiva() {
            DriverConfig config = new DriverConfig(BrowserType.CHROME)
                .withHeadless(true)
                .withHeadless(false);
            assertThat(config.isHeadless()).isFalse();
        }

        @Test
        @DisplayName("withGrid establece modo GRID y la URL del hub")
        void withGridEstableceModo() {
            String hubUrl = "http://selenium-grid:4444/wd/hub";
            DriverConfig config = new DriverConfig(BrowserType.CHROME)
                .withGrid(hubUrl);

            assertThat(config.getExecutionMode()).isEqualTo(ExecutionMode.GRID);
            assertThat(config.getGridHubUrl()).isEqualTo(hubUrl);
        }

        @Test
        @DisplayName("withProxy establece la URL del proxy")
        void withProxyEstableceUrl() {
            String proxyUrl = "http://proxy.corp.com:8080";
            DriverConfig config = new DriverConfig(BrowserType.CHROME)
                .withProxy(proxyUrl);
            assertThat(config.getProxyUrl()).isEqualTo(proxyUrl);
        }

        @Test
        @DisplayName("Builder es fluido (retorna la misma instancia)")
        void builderEsFluido() {
            DriverConfig config = new DriverConfig(BrowserType.FIREFOX);
            DriverConfig result = config.withHeadless(true);
            assertThat(result).isSameAs(config);
        }

        @Test
        @DisplayName("Builder permite encadenar múltiples configuraciones")
        void builderEncadenaConfiguraciones() {
            DriverConfig config = new DriverConfig(BrowserType.EDGE)
                .withHeadless(true)
                .withProxy("http://proxy:8080");

            assertThat(config.getBrowserType()).isEqualTo(BrowserType.EDGE);
            assertThat(config.isHeadless()).isTrue();
            assertThat(config.getProxyUrl()).isEqualTo("http://proxy:8080");
        }
    }

    // =========================================================================
    // BrowserType enum
    // =========================================================================

    @Nested
    @DisplayName("BrowserType enum")
    @Order(3)
    class BrowserTypeEnumTests {

        @ParameterizedTest
        @EnumSource(BrowserType.class)
        @DisplayName("Todos los BrowserType crean DriverConfig válido")
        void todosBrowserTypeCreanConfig(BrowserType browser) {
            DriverConfig config = new DriverConfig(browser);
            assertThat(config.getBrowserType()).isEqualTo(browser);
        }

        @Test
        @DisplayName("BrowserType incluye Chrome, Firefox, Edge y Safari")
        void browserTypeIncluyeNavegadoresPrincipales() {
            assertThat(BrowserType.values()).contains(
                BrowserType.CHROME, BrowserType.FIREFOX,
                BrowserType.EDGE, BrowserType.SAFARI
            );
        }
    }

    // =========================================================================
    // ExecutionMode enum
    // =========================================================================

    @Nested
    @DisplayName("ExecutionMode enum")
    @Order(4)
    class ExecutionModeEnumTests {

        @Test
        @DisplayName("ExecutionMode incluye LOCAL y GRID")
        void executionModeIncluyeLocalYGrid() {
            assertThat(ExecutionMode.values()).contains(
                ExecutionMode.LOCAL, ExecutionMode.GRID
            );
        }

        @Test
        @DisplayName("DriverConfig recién creado tiene modo LOCAL")
        void nuevoConfigTieneModoLocal() {
            DriverConfig config = new DriverConfig(BrowserType.CHROME);
            assertThat(config.getExecutionMode()).isEqualTo(ExecutionMode.LOCAL);
        }

        @Test
        @DisplayName("DriverConfig cambia a GRID con withGrid()")
        void configCambiaAGridConWithGrid() {
            DriverConfig config = new DriverConfig(BrowserType.CHROME)
                .withGrid("http://grid:4444");
            assertThat(config.getExecutionMode()).isEqualTo(ExecutionMode.GRID);
        }
    }
}

