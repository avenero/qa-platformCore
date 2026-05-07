package com.qa.webcore.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests unitarios para {@link WebConfigKeys}.
 *
 * <p>Verifica que:
 * <ul>
 *   <li>Las claves obligatorias (requeridas por la tarea) tienen el valor correcto</li>
 *   <li>Las claves de VariableStore tienen el valor correcto</li>
 *   <li>Todas las constantes son {@code public static final String}</li>
 *   <li>No hay claves duplicadas (dos constantes con el mismo valor)</li>
 *   <li>El constructor privado no es instanciable</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.2.0
 */
@DisplayName("WebConfigKeys")
class WebConfigKeysTest {

    // =========================================================================
    // Claves obligatorias — requeridas por la especificación de la tarea
    // =========================================================================

    @Nested
    @DisplayName("Claves obligatorias (especificación)")
    class ClavesObligatorias {

        @Test
        @DisplayName("BROWSER tiene el valor 'web.browser'")
        void testBrowser() {
            assertThat(WebConfigKeys.BROWSER).isEqualTo("web.browser");
        }

        @SuppressWarnings("deprecation") // HEADLESS queda como alias de compatibilidad.
        @Test
        @DisplayName("HEADLESS tiene el valor 'web.headless'")
        void testHeadless() {
            assertThat(WebConfigKeys.HEADLESS).isEqualTo("web.headless");
        }

        @Test
        @DisplayName("BROWSER_HEADLESS tiene el valor 'web.headless'")
        void testBrowserHeadless() {
            assertThat(WebConfigKeys.BROWSER_HEADLESS).isEqualTo("web.headless");
        }

        @Test
        @DisplayName("BASE_URL tiene el valor 'web.base.url'")
        void testBaseUrl() {
            assertThat(WebConfigKeys.BASE_URL).isEqualTo("web.base.url");
        }
    }

    // =========================================================================
    // Claves de VariableStore
    // =========================================================================

    @Nested
    @DisplayName("Claves de VariableStore (_RUNTIME_VAR)")
    class ClavesRuntimeVar {

        @Test
        @DisplayName("BROWSER_RUNTIME_VAR tiene el valor 'web.browser.type'")
        void testBrowserRuntimeVar() {
            assertThat(WebConfigKeys.BROWSER_RUNTIME_VAR).isEqualTo("web.browser.type");
        }

        @Test
        @DisplayName("HEADLESS_RUNTIME_VAR tiene el valor 'web.headless.override'")
        void testHeadlessRuntimeVar() {
            assertThat(WebConfigKeys.HEADLESS_RUNTIME_VAR).isEqualTo("web.headless.override");
        }
    }

    // =========================================================================
    // Resto de claves
    // =========================================================================

    @Nested
    @DisplayName("Claves adicionales")
    class ClavesAdicionales {

        @Test
        @DisplayName("PROXY_URL tiene el valor 'web.proxy.url'")
        void testProxyUrl() {
            assertThat(WebConfigKeys.PROXY_URL).isEqualTo("web.proxy.url");
        }

        @Test
        @DisplayName("GRID_ENABLED tiene el valor 'web.grid.enabled'")
        void testGridEnabled() {
            assertThat(WebConfigKeys.GRID_ENABLED).isEqualTo("web.grid.enabled");
        }

        @Test
        @DisplayName("GRID_URL tiene el valor 'web.grid.url'")
        void testGridUrl() {
            assertThat(WebConfigKeys.GRID_URL).isEqualTo("web.grid.url");
        }

        @Test
        @DisplayName("DRIVER_STRATEGY tiene el valor 'driver.strategy'")
        void testDriverStrategy() {
            assertThat(WebConfigKeys.DRIVER_STRATEGY).isEqualTo("driver.strategy");
        }

        @Test
        @DisplayName("PAGE_LOAD_TIMEOUT_SEC tiene el valor 'web.page.load.timeout.sec'")
        void testPageLoadTimeoutSec() {
            assertThat(WebConfigKeys.PAGE_LOAD_TIMEOUT_SEC).isEqualTo("web.page.load.timeout.sec");
        }

        @Test
        @DisplayName("EXPLICIT_WAIT_SEC tiene el valor 'web.explicit.wait.sec'")
        void testExplicitWaitSec() {
            assertThat(WebConfigKeys.EXPLICIT_WAIT_SEC).isEqualTo("web.explicit.wait.sec");
        }

        @Test
        @DisplayName("IMPLICIT_WAIT_SEC tiene el valor 'web.implicit.wait.sec'")
        void testImplicitWaitSec() {
            assertThat(WebConfigKeys.IMPLICIT_WAIT_SEC).isEqualTo("web.implicit.wait.sec");
        }

        @Test
        @DisplayName("BROWSER_ENGINE tiene el valor 'browser.engine'")
        void testBrowserEngine() {
            assertThat(WebConfigKeys.BROWSER_ENGINE).isEqualTo("browser.engine");
        }

        @Test
        @DisplayName("PLAYWRIGHT_BROWSER tiene el valor 'playwright.browser'")
        void testPlaywrightBrowser() {
            assertThat(WebConfigKeys.PLAYWRIGHT_BROWSER).isEqualTo("playwright.browser");
        }

        @Test
        @DisplayName("PW_TIMEOUT_MS tiene el valor 'playwright.timeout.ms'")
        void testPlaywrightTimeout() {
            assertThat(WebConfigKeys.PW_TIMEOUT_MS).isEqualTo("playwright.timeout.ms");
        }

        @Test
        @DisplayName("PW_SCREENSHOT_DIR tiene el valor 'playwright.screenshots.dir'")
        void testPlaywrightScreenshotDir() {
            assertThat(WebConfigKeys.PW_SCREENSHOT_DIR).isEqualTo("playwright.screenshots.dir");
        }

        @SuppressWarnings("deprecation") // HOST_LEGACY: deprecated desde 2.2.0 — test verifica la constante existe con el valor correcto
        @Test
        @DisplayName("HOST_LEGACY tiene el valor 'host'")
        void testHostLegacy() {
            assertThat(WebConfigKeys.HOST_LEGACY).isEqualTo("host");
        }
    }

    // =========================================================================
    // Estructura de la clase
    // =========================================================================

    @Nested
    @DisplayName("Estructura de la clase")
    class EstructuraClase {

        @Test
        @DisplayName("El constructor es privado — no instanciable")
        void testConstructorPrivado() throws Exception {
            Constructor<WebConfigKeys> constructor = WebConfigKeys.class.getDeclaredConstructor();
            assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
            constructor.setAccessible(true);
            assertThatExceptionOfType(java.lang.reflect.InvocationTargetException.class)
                    .isThrownBy(constructor::newInstance);
        }

        @Test
        @DisplayName("Todas las constantes son public static final String")
        void testTodasConstantesPublicStaticFinalString() {
            List<Field> camposPublicos = Arrays.stream(WebConfigKeys.class.getDeclaredFields())
                    .filter(f -> !f.isSynthetic())
                    .toList();

            assertThat(camposPublicos).isNotEmpty();

            for (Field f : camposPublicos) {
                int mods = f.getModifiers();
                assertThat(Modifier.isPublic(mods))
                        .as("Campo '%s' debe ser public", f.getName()).isTrue();
                assertThat(Modifier.isStatic(mods))
                        .as("Campo '%s' debe ser static", f.getName()).isTrue();
                assertThat(Modifier.isFinal(mods))
                        .as("Campo '%s' debe ser final", f.getName()).isTrue();
                assertThat(f.getType())
                        .as("Campo '%s' debe ser String", f.getName()).isEqualTo(String.class);
            }
        }

        @Test
        @DisplayName("No hay claves duplicadas entre constantes (excepto aliases explícitos)")
        void testSinValoresDuplicados() throws Exception {
            Field[] campos = WebConfigKeys.class.getDeclaredFields();

            Map<String, List<String>> nombresPorValor = Arrays.stream(campos)
                    .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
                    .collect(Collectors.groupingBy(f -> {
                        try { return (String) f.get(null); }
                        catch (IllegalAccessException e) { return null; }
                    }, Collectors.mapping(Field::getName, Collectors.toList())));

            Set<String> valoresAliasPermitidos = Set.of("web.headless");

            for (Map.Entry<String, List<String>> entry : nombresPorValor.entrySet()) {
                String valor = entry.getKey();
                List<String> nombres = entry.getValue();
                if (nombres.size() <= 1) {
                    continue;
                }

                assertThat(valoresAliasPermitidos)
                        .as("Valor duplicado no permitido: '%s' en constantes %s", valor, nombres)
                        .contains(valor);
            }
        }

        @Test
        @DisplayName("Ninguna constante tiene valor null ni vacío")
        void testSinValoresNullOVacios() throws Exception {
            Field[] campos = WebConfigKeys.class.getDeclaredFields();

            for (Field f : campos) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                    String valor = (String) f.get(null);
                    assertThat(valor)
                            .as("La constante '%s' tiene valor null o vacío", f.getName())
                            .isNotNull()
                            .isNotBlank();
                }
            }
        }
    }
}
