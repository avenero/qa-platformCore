package com.qa.mobilecore.driver;

import io.appium.java_client.AppiumDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests unitarios para MobileDriverManager.
 *
 * <p>Verifica gestion ThreadLocal del AppiumDriver: asignacion, obtencion,
 * cierre seguro y aislamiento entre threads.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("MobileDriverManager")
class MobileDriverManagerTest {

    @AfterEach
    void tearDown() {
        // Garantiza estado limpio entre tests
        MobileDriverManager.quitDriverSafely();
    }

    // =========================================================================

    @Nested
    @DisplayName("setDriver() / getDriver()")
    class SetGetDriverTest {

        @Test
        @DisplayName("setDriver + getDriver deben retornar el mismo driver")
        void testSetAndGet() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            MobileDriverManager.setDriver(mockDriver);
            assertThat(MobileDriverManager.getDriver()).isSameAs(mockDriver);
        }

        @Test
        @DisplayName("setDriver con null lanza IllegalArgumentException")
        void testSetNullDriver() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> MobileDriverManager.setDriver(null))
                    .withMessageContaining("null");
        }

        @Test
        @DisplayName("getDriver sin setDriver previo lanza IllegalStateException")
        void testGetDriverSinInicializar() {
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(MobileDriverManager::getDriver)
                    .withMessageContaining("no inicializado");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("getDriverOrNull()")
    class GetDriverOrNullTest {

        @Test
        @DisplayName("Retorna null si el driver no fue inicializado")
        void testGetOrNullSinDriver() {
            assertThat(MobileDriverManager.getDriverOrNull()).isNull();
        }

        @Test
        @DisplayName("Retorna el driver si fue inicializado")
        void testGetOrNullConDriver() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            MobileDriverManager.setDriver(mockDriver);
            assertThat(MobileDriverManager.getDriverOrNull()).isSameAs(mockDriver);
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("isInitialized()")
    class IsInitializedTest {

        @Test
        @DisplayName("Retorna false si no hay driver")
        void testFalseSinDriver() {
            assertThat(MobileDriverManager.isInitialized()).isFalse();
        }

        @Test
        @DisplayName("Retorna true despues de setDriver")
        void testTrueConDriver() {
            MobileDriverManager.setDriver(mock(AppiumDriver.class));
            assertThat(MobileDriverManager.isInitialized()).isTrue();
        }

        @Test
        @DisplayName("Retorna false despues de quitDriverSafely")
        void testFalseDespuesDeQuit() {
            MobileDriverManager.setDriver(mock(AppiumDriver.class));
            MobileDriverManager.quitDriverSafely();
            assertThat(MobileDriverManager.isInitialized()).isFalse();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("quitDriver()")
    class QuitDriverTest {

        @Test
        @DisplayName("Llama quit() en el driver y limpia el ThreadLocal")
        void testQuitLlamaQuitYLimpia() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            MobileDriverManager.setDriver(mockDriver);

            MobileDriverManager.quitDriver();

            verify(mockDriver).quit();
            assertThat(MobileDriverManager.isInitialized()).isFalse();
        }

        @Test
        @DisplayName("quitDriver cuando el driver lanza excepcion propaga RuntimeException")
        void testQuitDriverConExcepcion() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            doThrow(new RuntimeException("Session error")).when(mockDriver).quit();
            MobileDriverManager.setDriver(mockDriver);

            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(MobileDriverManager::quitDriver)
                    .withMessageContaining("cerrando AppiumDriver");

            // El ThreadLocal debe estar limpio incluso si quit() lanzó excepcion
            assertThat(MobileDriverManager.isInitialized()).isFalse();
        }

        @Test
        @DisplayName("quitDriver sin driver inicializado no lanza excepcion")
        void testQuitSinDriver() {
            // No debe lanzar excepcion
            MobileDriverManager.quitDriver();
            assertThat(MobileDriverManager.isInitialized()).isFalse();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("quitDriverSafely()")
    class QuitDriverSafelyTest {

        @Test
        @DisplayName("Llama quit() y limpia el ThreadLocal")
        void testQuitSafelyLimpia() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            MobileDriverManager.setDriver(mockDriver);

            MobileDriverManager.quitDriverSafely();

            verify(mockDriver).quit();
            assertThat(MobileDriverManager.isInitialized()).isFalse();
        }

        @Test
        @DisplayName("No propaga excepcion si quit() falla")
        void testQuitSafelyNoPropagoExcepcion() {
            AppiumDriver mockDriver = mock(AppiumDriver.class);
            doThrow(new RuntimeException("Fallo Appium")).when(mockDriver).quit();
            MobileDriverManager.setDriver(mockDriver);

            // No debe lanzar excepcion
            MobileDriverManager.quitDriverSafely();
            assertThat(MobileDriverManager.isInitialized()).isFalse();
        }

        @Test
        @DisplayName("No lanza excepcion si el driver no esta inicializado")
        void testQuitSafelySinDriver() {
            MobileDriverManager.quitDriverSafely();
            // Solo verifica que no lanza
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("Aislamiento ThreadLocal entre hilos")
    class ThreadLocalAislamientoTest {

        @Test
        @DisplayName("Cada thread debe tener su propio driver independiente")
        void testAislamientoEntreThreads() throws InterruptedException {
            AppiumDriver mainDriver = mock(AppiumDriver.class);
            MobileDriverManager.setDriver(mainDriver);

            AtomicBoolean threadInicializado = new AtomicBoolean(false);
            AtomicReference<AppiumDriver> threadDriverRef = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            Thread worker = new Thread(() -> {
                // El thread secundario NO debe ver el driver del main thread
                threadInicializado.set(MobileDriverManager.isInitialized());
                threadDriverRef.set(MobileDriverManager.getDriverOrNull());
                latch.countDown();
            });
            worker.start();
            latch.await(5, TimeUnit.SECONDS);

            // El main thread sigue viendo su driver
            assertThat(MobileDriverManager.getDriver()).isSameAs(mainDriver);

            // El worker thread no tenia driver propio
            assertThat(threadInicializado.get()).isFalse();
            assertThat(threadDriverRef.get()).isNull();
        }

        @Test
        @DisplayName("Threads paralelos pueden tener drivers distintos de forma aislada")
        void testDriversDistintosEnParalelo() throws InterruptedException {
            int numThreads = 5;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch allDone = new CountDownLatch(numThreads);
            AtomicBoolean anyConflict = new AtomicBoolean(false);

            for (int i = 0; i < numThreads; i++) {
                executor.submit(() -> {
                    try {
                        AppiumDriver myDriver = mock(AppiumDriver.class);
                        MobileDriverManager.setDriver(myDriver);

                        // Cada thread verifica que ve SU driver
                        AppiumDriver retrieved = MobileDriverManager.getDriver();
                        if (retrieved != myDriver) {
                            anyConflict.set(true);
                        }
                    } finally {
                        MobileDriverManager.quitDriverSafely();
                        allDone.countDown();
                    }
                });
            }

            allDone.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(anyConflict.get())
                    .as("Ningun thread debe ver el driver de otro thread")
                    .isFalse();
        }
    }
}
