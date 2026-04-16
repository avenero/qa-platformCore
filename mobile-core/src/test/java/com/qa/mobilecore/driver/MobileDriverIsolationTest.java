package com.qa.mobilecore.driver;

import com.qa.common.runtime.ExecutionContext;
import io.appium.java_client.AppiumDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests de concurrencia para el aislamiento del {@link AppiumDriver} vía
 * {@link MobileDriverFactory} y {@link com.qa.common.runtime.ServiceRegistry}.
 *
 * <p>Valida que dos ejecuciones BDD móviles corriendo en paralelo utilizan instancias
 * de {@link AppiumDriver} completamente distintas: cada {@link MobileDriverFactory} vive
 * en su propio {@link ExecutionContext} y opera sobre su propia sesión Appium, sin ningún
 * estado compartido con otra ejecución concurrente.
 *
 * <p><b>Estrategia:</b> sin servidor Appium real. Se inyectan drivers falsos vía
 * {@link MobileDriverFactory#injectDriver(AppiumDriver)} (disponible para tests).
 * {@code AppiumDriver} extiende {@code RemoteWebDriver} (clase, no interfaz), por lo que
 * se usa Mockito para crear stubs — igual que en {@link MobileDriverFactoryTest}.
 *
 * @author Abel Venero
 * @since 2.3.0
 */
@DisplayName("MobileDriverFactory — aislamiento entre ejecuciones concurrentes")
class MobileDriverIsolationTest {

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
        MobileDriverManager.quitDriverSafely();
    }

    // =========================================================================
    // Aislamiento básico de dos factories
    // =========================================================================

    @Nested
    @DisplayName("Dos factories en contextos distintos — drivers distintos")
    class DosFactoriesTests {

        @Test
        @DisplayName("Cada factory devuelve su propio AppiumDriver — sin cruce entre contextos")
        void dosFactoriesContextosDistintosDriversDistintos() throws Exception {
            AppiumDriver driver1 = mock(AppiumDriver.class);
            AppiumDriver driver2 = mock(AppiumDriver.class);

            MobileDriverFactory factory1 = new MobileDriverFactory();
            MobileDriverFactory factory2 = new MobileDriverFactory();
            factory1.injectDriver(driver1);
            factory2.injectDriver(driver2);

            ExecutionContext ctx1 = ExecutionContext.builder().scenarioId("mobile-s-1").build();
            ExecutionContext ctx2 = ExecutionContext.builder().scenarioId("mobile-s-2").build();
            ctx1.registry().registerInstance(MobileDriverFactory.class, factory1);
            ctx2.registry().registerInstance(MobileDriverFactory.class, factory2);

            CyclicBarrier barrier = new CyclicBarrier(2);
            CountDownLatch done   = new CountDownLatch(2);

            AtomicReference<AppiumDriver> recuperadoT1 = new AtomicReference<>();
            AtomicReference<AppiumDriver> recuperadoT2 = new AtomicReference<>();
            AtomicReference<Throwable> errorT1 = new AtomicReference<>();
            AtomicReference<Throwable> errorT2 = new AtomicReference<>();

            Thread t1 = new Thread(() -> {
                try {
                    ctx1.activate();
                    barrier.await(5, TimeUnit.SECONDS);
                    recuperadoT1.set(
                        ExecutionContext.requireCurrent()
                            .service(MobileDriverFactory.class)
                            .getOrCreateDriver());
                } catch (Throwable t) {
                    errorT1.set(t);
                } finally {
                    ExecutionContext.deactivate();
                    done.countDown();
                }
            });

            Thread t2 = new Thread(() -> {
                try {
                    ctx2.activate();
                    barrier.await(5, TimeUnit.SECONDS);
                    recuperadoT2.set(
                        ExecutionContext.requireCurrent()
                            .service(MobileDriverFactory.class)
                            .getOrCreateDriver());
                } catch (Throwable t) {
                    errorT2.set(t);
                } finally {
                    ExecutionContext.deactivate();
                    done.countDown();
                }
            });

            t1.start();
            t2.start();
            done.await(10, TimeUnit.SECONDS);

            assertThat(errorT1.get()).isNull();
            assertThat(errorT2.get()).isNull();
            assertThat(recuperadoT1.get())
                    .as("T1 debe obtener su driver del contexto 1")
                    .isSameAs(driver1);
            assertThat(recuperadoT2.get())
                    .as("T2 debe obtener su driver del contexto 2")
                    .isSameAs(driver2);
            assertThat(recuperadoT1.get())
                    .as("Los AppiumDrivers deben ser instancias distintas")
                    .isNotSameAs(recuperadoT2.get());
        }

        @Test
        @DisplayName("quitIfCreated de una factory no afecta al driver de la otra")
        void quitIfCreatedAisladoPorFactory() {
            AppiumDriver driver1 = mock(AppiumDriver.class);
            AppiumDriver driver2 = mock(AppiumDriver.class);

            MobileDriverFactory factory1 = new MobileDriverFactory();
            MobileDriverFactory factory2 = new MobileDriverFactory();
            factory1.injectDriver(driver1);
            factory2.injectDriver(driver2);

            assertThat(factory1.isDriverCreated()).isTrue();
            assertThat(factory2.isDriverCreated()).isTrue();

            // Cerrar factory1 — factory2 no debe verse afectada
            factory1.quitIfCreated();

            assertThat(factory1.isDriverCreated())
                    .as("factory1 debe haber cerrado su driver")
                    .isFalse();
            assertThat(factory2.isDriverCreated())
                    .as("factory2 aún debe tener su driver activo")
                    .isTrue();

            // Verificar que solo driver1 recibió quit(), driver2 no fue tocado
            verify(driver1).quit();
            verifyNoInteractions(driver2);

            // factory2 aún devuelve su driver correctamente
            assertThat(factory2.getOrCreateDriver()).isSameAs(driver2);
        }

        @Test
        @DisplayName("Dos factories sin driver inyectado tienen estado independiente")
        void dosFactoriesNuevasEstadoIndependiente() {
            MobileDriverFactory factory1 = new MobileDriverFactory();
            MobileDriverFactory factory2 = new MobileDriverFactory();

            assertThat(factory1.isDriverCreated()).isFalse();
            assertThat(factory2.isDriverCreated()).isFalse();

            // Inyectar en factory1 no afecta a factory2
            factory1.injectDriver(mock(AppiumDriver.class));

            assertThat(factory1.isDriverCreated()).isTrue();
            assertThat(factory2.isDriverCreated())
                    .as("factory2 no debe ver el driver de factory1")
                    .isFalse();
        }
    }

    // =========================================================================
    // N factories en paralelo
    // =========================================================================

    @Nested
    @DisplayName("N factories paralelas — cada una opera sobre su propio driver")
    class NFactoriesParalelasTests {

        @Test
        @DisplayName("Cinco factories paralelas — sin contaminación cruzada de drivers")
        void cincoFactoriesParalelasAisladas() throws Exception {
            int n = 5;
            CyclicBarrier barrier = new CyclicBarrier(n);
            CountDownLatch done   = new CountDownLatch(n);
            AtomicBoolean hayConflicto = new AtomicBoolean(false);

            for (int i = 0; i < n; i++) {
                AppiumDriver miDriver      = mock(AppiumDriver.class);
                MobileDriverFactory factory = new MobileDriverFactory();
                factory.injectDriver(miDriver);

                Thread t = new Thread(() -> {
                    try {
                        barrier.await(5, TimeUnit.SECONDS);
                        AppiumDriver recuperado = factory.getOrCreateDriver();
                        if (recuperado != miDriver) {
                            hayConflicto.set(true);
                        }
                    } catch (Throwable e) {
                        hayConflicto.set(true);
                    } finally {
                        factory.quitIfCreated();
                        done.countDown();
                    }
                });
                t.start();
            }

            done.await(15, TimeUnit.SECONDS);

            assertThat(hayConflicto.get())
                    .as("Ninguna factory debe ver el AppiumDriver de otra")
                    .isFalse();
        }
    }

    // =========================================================================
    // Integración: Factory en ServiceRegistry de contextos paralelos
    // =========================================================================

    @Nested
    @DisplayName("ServiceRegistry — factories accedidas desde contextos paralelos")
    class ServiceRegistryFactoryTests {

        @Test
        @DisplayName("Cada contexto obtiene su factory y su driver — sin cruce en ServiceRegistry")
        void cadaContextoSuFactoryYSuDriver() throws Exception {
            CyclicBarrier barrier = new CyclicBarrier(2);
            CountDownLatch done   = new CountDownLatch(2);

            AppiumDriver driver1 = mock(AppiumDriver.class);
            AppiumDriver driver2 = mock(AppiumDriver.class);

            MobileDriverFactory factory1 = new MobileDriverFactory();
            MobileDriverFactory factory2 = new MobileDriverFactory();
            factory1.injectDriver(driver1);
            factory2.injectDriver(driver2);

            ExecutionContext ctx1 = ExecutionContext.builder().scenarioId("ctx-1").build();
            ExecutionContext ctx2 = ExecutionContext.builder().scenarioId("ctx-2").build();
            ctx1.registry().registerInstance(MobileDriverFactory.class, factory1);
            ctx2.registry().registerInstance(MobileDriverFactory.class, factory2);

            AtomicReference<Boolean> factory1EsCorrecta = new AtomicReference<>();
            AtomicReference<Boolean> factory2EsCorrecta = new AtomicReference<>();
            AtomicReference<Throwable> errorT1 = new AtomicReference<>();
            AtomicReference<Throwable> errorT2 = new AtomicReference<>();

            Thread t1 = new Thread(() -> {
                try {
                    ctx1.activate();
                    barrier.await(5, TimeUnit.SECONDS);
                    MobileDriverFactory f = ExecutionContext.requireCurrent()
                            .service(MobileDriverFactory.class);
                    factory1EsCorrecta.set(f.getOrCreateDriver() == driver1);
                } catch (Throwable t) {
                    errorT1.set(t);
                } finally {
                    ExecutionContext.deactivate();
                    done.countDown();
                }
            });

            Thread t2 = new Thread(() -> {
                try {
                    ctx2.activate();
                    barrier.await(5, TimeUnit.SECONDS);
                    MobileDriverFactory f = ExecutionContext.requireCurrent()
                            .service(MobileDriverFactory.class);
                    factory2EsCorrecta.set(f.getOrCreateDriver() == driver2);
                } catch (Throwable t) {
                    errorT2.set(t);
                } finally {
                    ExecutionContext.deactivate();
                    done.countDown();
                }
            });

            t1.start();
            t2.start();
            done.await(10, TimeUnit.SECONDS);

            assertThat(errorT1.get()).isNull();
            assertThat(errorT2.get()).isNull();
            assertThat(factory1EsCorrecta.get())
                    .as("ctx1 debe obtener driver1 desde su factory registrada")
                    .isTrue();
            assertThat(factory2EsCorrecta.get())
                    .as("ctx2 debe obtener driver2 desde su factory registrada")
                    .isTrue();
        }
    }
}
