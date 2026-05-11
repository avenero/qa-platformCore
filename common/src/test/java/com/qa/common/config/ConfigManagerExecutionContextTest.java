package com.qa.common.config;


import com.qa.common.internal.config.ConfigManager;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.ExecutionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para el bridge ExecutionConfig → ConfigManager.
 *
 * <p>Verifica que {@link ConfigManager#get(String)} consulta primero el
 * {@link ExecutionContext} activo en el hilo (step 0), antes de recurrir a
 * System Properties, variables de entorno y archivos de configuración.
 *
 * <p><b>Prioridad verificada:</b>
 * <pre>
 * ExecutionContext.config.getProperty(key)   ← step 0 (más alta)
 * System.getProperty(key)                    ← step 1
 * System.getenv(key)                         ← step 2
 * loadedProperties.getProperty(key)          ← step 3
 * null / defaultValue                        ← step 4
 * </pre>
 *
 * <p><b>Nota de aislamiento:</b> cada test desactiva el {@link ExecutionContext}
 * en {@code @AfterEach} para no contaminar el singleton de {@link ConfigManager}
 * ni otros tests de la suite.
 *
 * @author Abel Venero
 * @since 2.1.0
 */
@DisplayName("ConfigManager — Bridge ExecutionContext (step 0)")
class ConfigManagerExecutionContextTest {

    // Clave que NO existe en ningún archivo de configuración ni en System Properties reales
    private static final String TEST_KEY = "execution.context.bridge.test.key";

    private ConfigManager config;

    @BeforeEach
    void setUp() {
        System.clearProperty(TEST_KEY);
        ExecutionContext.deactivate(); // Garantizar contexto limpio
        config = ConfigManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(TEST_KEY);
        ExecutionContext.deactivate(); // Siempre limpiar el ThreadLocal
    }

    // =========================================================================
    // Comportamiento base: sin ExecutionContext activo
    // =========================================================================

    @Nested
    @DisplayName("Sin ExecutionContext activo — comportamiento idéntico al anterior")
    class SinContextoActivoTest {

        @Test
        @DisplayName("get() retorna null cuando no hay contexto ni valor global")
        void testGetSinContextoRetornaNull() {
            assertThat(config.get(TEST_KEY)).isNull();
        }

        @Test
        @DisplayName("get(key, default) retorna default cuando no hay contexto ni valor global")
        void testGetConDefaultSinContexto() {
            assertThat(config.get(TEST_KEY, "fallback")).isEqualTo("fallback");
        }

        @Test
        @DisplayName("getWithPriority() retorna default cuando no hay contexto ni valor global")
        void testGetWithPrioritySinContexto() {
            assertThat(config.getWithPriority(TEST_KEY, "default-val")).isEqualTo("default-val");
        }

        @Test
        @DisplayName("getBoolean() retorna default cuando no hay contexto")
        void testGetBooleanSinContexto() {
            assertThat(config.getBoolean(TEST_KEY, true)).isTrue();
        }

        @Test
        @DisplayName("getInt() retorna default cuando no hay contexto")
        void testGetIntSinContexto() {
            assertThat(config.getInt(TEST_KEY, 99)).isEqualTo(99);
        }

        @Test
        @DisplayName("getLong() retorna default cuando no hay contexto")
        void testGetLongSinContexto() {
            assertThat(config.getLong(TEST_KEY, 999L)).isEqualTo(999L);
        }

        @Test
        @DisplayName("System Property sigue funcionando sin contexto")
        void testSystemPropertySinContexto() {
            System.setProperty(TEST_KEY, "sysprop-value");
            assertThat(config.get(TEST_KEY)).isEqualTo("sysprop-value");
        }
    }

    // =========================================================================
    // ExecutionContext es step 0: prioridad máxima
    // =========================================================================

    @Nested
    @DisplayName("ExecutionContext activo — step 0 con máxima prioridad")
    class ConContextoActivoTest {

        @Test
        @DisplayName("get() devuelve el valor del ExecutionContext cuando existe")
        void testGetDesdeExecutionContext() {
            activarContexto(TEST_KEY, "valor-de-ejecucion");

            assertThat(config.get(TEST_KEY)).isEqualTo("valor-de-ejecucion");
        }

        @Test
        @DisplayName("get(key, default) devuelve el valor del ExecutionContext (ignora default)")
        void testGetConDefaultDesdeExecutionContext() {
            activarContexto(TEST_KEY, "exec-value");

            assertThat(config.get(TEST_KEY, "ignorado")).isEqualTo("exec-value");
        }

        @Test
        @DisplayName("getWithPriority() devuelve el valor del ExecutionContext")
        void testGetWithPriorityDesdeExecutionContext() {
            activarContexto(TEST_KEY, "exec-priority");

            assertThat(config.getWithPriority(TEST_KEY, "ignorado")).isEqualTo("exec-priority");
        }

        @Test
        @DisplayName("getBoolean() resuelve desde ExecutionContext")
        void testGetBooleanDesdeExecutionContext() {
            activarContexto(TEST_KEY, "true");

            assertThat(config.getBoolean(TEST_KEY, false)).isTrue();
        }

        @Test
        @DisplayName("getInt() resuelve desde ExecutionContext")
        void testGetIntDesdeExecutionContext() {
            activarContexto(TEST_KEY, "42");

            assertThat(config.getInt(TEST_KEY, 0)).isEqualTo(42);
        }

        @Test
        @DisplayName("getLong() resuelve desde ExecutionContext")
        void testGetLongDesdeExecutionContext() {
            activarContexto(TEST_KEY, "123456789");

            assertThat(config.getLong(TEST_KEY, 0L)).isEqualTo(123456789L);
        }

        @Test
        @DisplayName("contains() retorna true cuando la clave está en ExecutionContext")
        void testContainsDesdeExecutionContext() {
            activarContexto(TEST_KEY, "presente");

            assertThat(config.contains(TEST_KEY)).isTrue();
        }

        @Test
        @DisplayName("ExecutionContext sin la clave cae al siguiente step (System Property)")
        void testFallbackCuandoContextoNoTieneLaClave() {
            // Contexto sin TEST_KEY
            ExecutionConfig execConfig = new ExecutionConfig.Builder()
                    .property("otra.clave", "otro-valor")
                    .build();
            ExecutionContext ctx = ExecutionContext.builder()
                    .config(execConfig)
                    .build();
            ctx.activate();

            // System Property sí tiene la clave
            System.setProperty(TEST_KEY, "sistema-fallback");

            assertThat(config.get(TEST_KEY)).isEqualTo("sistema-fallback");
        }
    }

    // =========================================================================
    // Prioridad: ExecutionContext supera a System Properties
    // =========================================================================

    @Nested
    @DisplayName("ExecutionContext supera a System Properties (step 0 > step 1)")
    class PrioridadExecutionVsSystemPropertyTest {

        @Test
        @DisplayName("ExecutionContext gana sobre System Property con la misma clave")
        void testExecutionContextGanaASystemProperty() {
            // System Property configurado (normalmente la fuente más alta)
            System.setProperty(TEST_KEY, "valor-sistema");

            // ExecutionContext con la misma clave (debe ganar)
            activarContexto(TEST_KEY, "valor-ejecucion");

            assertThat(config.get(TEST_KEY)).isEqualTo("valor-ejecucion");
        }

        @Test
        @DisplayName("getWithPriority(): ExecutionContext gana sobre System Property")
        void testGetWithPriorityExecutionContextGana() {
            System.setProperty(TEST_KEY, "desde-D-flag");
            activarContexto(TEST_KEY, "desde-ejecucion");

            assertThat(config.getWithPriority(TEST_KEY, "default")).isEqualTo("desde-ejecucion");
        }

        @Test
        @DisplayName("Al desactivar el contexto, System Property vuelve a ganar")
        void testDesactivarContextoRestauralaPrioridad() {
            System.setProperty(TEST_KEY, "valor-sistema");
            activarContexto(TEST_KEY, "valor-ejecucion");

            // Con contexto → ExecutionContext gana
            assertThat(config.get(TEST_KEY)).isEqualTo("valor-ejecucion");

            // Desactivar contexto
            ExecutionContext.deactivate();

            // Sin contexto → System Property gana
            assertThat(config.get(TEST_KEY)).isEqualTo("valor-sistema");
        }
    }

    // =========================================================================
    // Aislamiento por hilo (Thread safety)
    // =========================================================================

    @Nested
    @DisplayName("Aislamiento por hilo — cada hilo ve su propio ExecutionContext")
    class AislamientoHilosTest {

        @Test
        @DisplayName("Dos hilos con contextos distintos ven valores distintos")
        void testDosHilosContextoIndependiente() throws InterruptedException {
            List<String> resultados = new ArrayList<>();
            List<String> errores    = new ArrayList<>();

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch  = new CountDownLatch(2);

            // Hilo 1: contexto con "chrome"
            Thread hilo1 = new Thread(() -> {
                try {
                    activarContexto("web.browser", "chrome");
                    startLatch.await(); // Esperar que ambos hilos tengan contexto
                    String valor = ConfigManager.getInstance().get("web.browser");
                    synchronized (resultados) { resultados.add("hilo1:" + valor); }
                } catch (Exception e) {
                    synchronized (errores) { errores.add(e.getMessage()); }
                } finally {
                    ExecutionContext.deactivate();
                    doneLatch.countDown();
                }
            });

            // Hilo 2: contexto con "firefox"
            Thread hilo2 = new Thread(() -> {
                try {
                    activarContexto("web.browser", "firefox");
                    startLatch.await();
                    String valor = ConfigManager.getInstance().get("web.browser");
                    synchronized (resultados) { resultados.add("hilo2:" + valor); }
                } catch (Exception e) {
                    synchronized (errores) { errores.add(e.getMessage()); }
                } finally {
                    ExecutionContext.deactivate();
                    doneLatch.countDown();
                }
            });

            hilo1.start();
            hilo2.start();
            startLatch.countDown(); // Arrancar ambos hilos simultáneamente
            doneLatch.await();

            assertThat(errores).isEmpty();
            assertThat(resultados).containsExactlyInAnyOrder("hilo1:chrome", "hilo2:firefox");
        }

        @Test
        @DisplayName("Hilo sin contexto no ve el contexto activo en otro hilo")
        void testHiloSinContextoNoVeOtroContexto() throws InterruptedException {
            CountDownLatch contextoActivoLatch = new CountDownLatch(1);
            CountDownLatch doneLatch           = new CountDownLatch(1);
            List<String> resultadoHiloPrincipal = new ArrayList<>();

            // Hilo secundario activa un contexto con web.browser=edge
            Thread hiloConContexto = new Thread(() -> {
                try {
                    activarContexto("web.browser", "edge");
                    contextoActivoLatch.countDown(); // Avisar que el contexto está activo
                    doneLatch.await(); // Esperar que el hilo principal lea
                } catch (Exception e) {
                    // ignore
                } finally {
                    ExecutionContext.deactivate();
                }
            });
            hiloConContexto.start();

            // Hilo principal (sin contexto) lee la clave
            contextoActivoLatch.await(); // Esperar que el hilo secundario active su contexto
            resultadoHiloPrincipal.add(config.get("web.browser", "no-hay-contexto"));
            doneLatch.countDown();
            hiloConContexto.join();

            // El hilo principal NO debe ver el contexto del hilo secundario
            assertThat(resultadoHiloPrincipal).doesNotContain("edge");
        }

        @Test
        @DisplayName("El mismo hilo con distintos contextos secuenciales se aísla correctamente")
        void testMismoHiloContextoSecuencial() {
            final String KEY = "secuencial.test.key";

            // Primera ejecución
            activarContexto(KEY, "primera-ejecucion");
            assertThat(config.get(KEY)).isEqualTo("primera-ejecucion");

            // Desactivar y activar un nuevo contexto
            ExecutionContext.deactivate();
            activarContexto(KEY, "segunda-ejecucion");
            assertThat(config.get(KEY)).isEqualTo("segunda-ejecucion");

            // Sin contexto → null
            ExecutionContext.deactivate();
            assertThat(config.get(KEY)).isNull();
        }
    }

    // =========================================================================
    // Claves típicas de la plataforma Kiu·Aleon
    // =========================================================================

    @Nested
    @DisplayName("Claves del dominio Kiu·Aleon: web, mobile, api, env")
    class ClavesDominioTest {

        @Test
        @DisplayName("web.browser desde ExecutionContext")
        void testWebBrowser() {
            activarContextoMultiple(
                    "web.browser",  "safari",
                    "web.headless", "true",
                    "web.base.url", "https://qa.mi-app.com"
            );

            assertThat(config.get("web.browser")).isEqualTo("safari");
            assertThat(config.getBoolean("web.headless", false)).isTrue();
            assertThat(config.get("web.base.url")).isEqualTo("https://qa.mi-app.com");
        }

        @Test
        @DisplayName("mobile.* desde ExecutionContext")
        void testMobileConfig() {
            activarContextoMultiple(
                    "mobile.platform",         "Android",
                    "mobile.device.id",        "emulator-5554",
                    "mobile.appium.server.url", "http://localhost:4723",
                    "mobile.app.path",         "/apps/test.apk"
            );

            assertThat(config.get("mobile.platform")).isEqualTo("Android");
            assertThat(config.get("mobile.device.id")).isEqualTo("emulator-5554");
            assertThat(config.get("mobile.appium.server.url")).isEqualTo("http://localhost:4723");
            assertThat(config.get("mobile.app.path")).isEqualTo("/apps/test.apk");
        }

        @Test
        @DisplayName("api.base.url desde ExecutionContext supera al archivo de config")
        void testApiBaseUrlDesdeEjecucion() {
            // api.base.url podría estar en config-app.properties como http://localhost:8080
            // La ejecución lo sobreescribe por ejecución
            activarContexto("api.base.url", "https://api.qa-banking.internal");

            assertThat(config.get("api.base.url")).isEqualTo("https://api.qa-banking.internal");
            assertThat(config.getWithPriority("api.base.url", "fallback"))
                    .isEqualTo("https://api.qa-banking.internal");
        }

        @Test
        @DisplayName("Múltiples propiedades conviven: algunas en ExecutionContext, otras en config file")
        void testMezclaEjecucionYConfigFile() {
            // Solo web.browser está en el contexto de ejecución
            activarContexto("web.browser", "edge");

            // web.browser → desde ExecutionContext
            assertThat(config.get("web.browser")).isEqualTo("edge");

            // api.timeout → desde config-app.properties (config file), no desde ExecutionContext
            // (asumiendo que config-app.properties tiene api.timeout=30000)
            assertThat(config.getInt("api.timeout", -1)).isNotEqualTo(-1);
        }
    }

    // =========================================================================
    // Robustez: contextos con edge cases
    // =========================================================================

    @Nested
    @DisplayName("Robustez — valores especiales en ExecutionContext")
    class RobustezTest {

        @Test
        @DisplayName("Valor vacío en ExecutionContext no se devuelve (vacío ≠ presente)")
        void testValorVacioEnContexto() {
            // ExecutionConfig no permite valores null, pero sí vacíos
            activarContexto(TEST_KEY, "");

            // Un valor vacío en el mapa SÍ está presente, Optional.of("") es presente
            // ConfigManager lo devuelve tal cual (cadena vacía)
            String result = config.get(TEST_KEY);
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("ExecutionContext con múltiples propiedades resuelve cada una correctamente")
        void testMultiplesPropiedadesEnContexto() {
            activarContextoMultiple(
                    "key.a", "valor-a",
                    "key.b", "valor-b",
                    "key.c", "valor-c"
            );

            assertThat(config.get("key.a")).isEqualTo("valor-a");
            assertThat(config.get("key.b")).isEqualTo("valor-b");
            assertThat(config.get("key.c")).isEqualTo("valor-c");
            assertThat(config.get("key.d")).isNull(); // key.d no está en ninguna fuente
        }

        @Test
        @DisplayName("El ExecutionContext no aplica resolución de ${VAR} (valores ya son finales)")
        void testNoResolucionDolarEnContexto() {
            // Si el Backend pone un valor literal con ${} (inusual pero posible),
            // ConfigManager lo devuelve sin interpretar
            activarContexto(TEST_KEY, "${NO_SE_RESUELVE}");

            assertThat(config.get(TEST_KEY)).isEqualTo("${NO_SE_RESUELVE}");
        }

        @Test
        @DisplayName("ExecutionContext con una propiedad no rompe la resolución de otras claves")
        void testContextoParcialNoRompeOtrasClaves() {
            // Contexto con solo UNA propiedad
            activarContexto("solo.esta.clave", "presente");

            // Otras claves del archivo de configuración siguen funcionando
            assertThat(config.get("test.string.simple")).isEqualTo("testValue");
            assertThat(config.getInt("test.number.int", -1)).isEqualTo(42);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Activa un {@link ExecutionContext} con una sola propiedad en el hilo actual.
     */
    private void activarContexto(String key, String value) {
        ExecutionConfig execConfig = new ExecutionConfig.Builder()
                .property(key, value)
                .build();
        ExecutionContext ctx = ExecutionContext.builder()
                .config(execConfig)
                .build();
        ctx.activate();
    }

    /**
     * Activa un {@link ExecutionContext} con múltiples propiedades (pares clave-valor).
     * Los argumentos deben pasarse como: key1, val1, key2, val2, ...
     */
    private void activarContextoMultiple(String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Los pares clave-valor deben ser pares (número par de argumentos)");
        }
        ExecutionConfig.Builder builder = new ExecutionConfig.Builder();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            builder.property(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        ExecutionContext ctx = ExecutionContext.builder()
                .config(builder.build())
                .build();
        ctx.activate();
    }
}
