package com.qa.webcore.driver;

import com.qa.common.runtime.ExecutionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de concurrencia para el aislamiento de {@link WebDriver} vía
 * {@link com.qa.common.runtime.ServiceRegistry}.
 *
 * <p>Valida el principio arquitectónico central: cuando hay un {@link ExecutionContext}
 * activo, el {@link WebDriver} se obtiene desde {@code context.registry()} y no desde
 * {@link DriverManager} (ThreadLocal global). Dos ejecuciones BDD corriendo en paralelo
 * deben obtener instancias de driver completamente distintas, sin ningún cruce.
 *
 * <p>No se abre ningún navegador real. Se usa {@link StubWebDriver} —una implementación
 * mínima de la interfaz— para verificar identidad de objetos ({@code assertThat(...).isSameAs}).
 *
 * @author Abel Venero
 * @since 2.3.0
 */
@DisplayName("WebDriver — aislamiento entre ejecuciones concurrentes (vía ServiceRegistry)")
class DriverIsolationTest {

    // =========================================================================
    // Stub manual de WebDriver (sin Mockito — WebDriver es interfaz)
    // =========================================================================

    /**
     * Stub mínimo de {@link WebDriver} para tests de aislamiento.
     * Solo registra si {@code quit()} fue invocado y expone un {@code id} para depuración.
     */
    static final class StubWebDriver implements WebDriver {
        final String id;
        boolean quitCalled = false;

        StubWebDriver(String id) {
            this.id = id;
        }

        @Override public void quit()   { quitCalled = true; }
        @Override public void get(String url) {}
        @Override public String getCurrentUrl()          { return "stub://"; }
        @Override public String getTitle()               { return "stub-title"; }
        @Override public List<WebElement> findElements(By b) { return List.of(); }
        @Override public WebElement findElement(By b)    { return null; }
        @Override public String getPageSource()          { return ""; }
        @Override public void close()                    {}
        @Override public String getWindowHandle()        { return "stub-" + id; }
        @Override public Set<String> getWindowHandles()  { return Set.of("stub-" + id); }
        @Override public Options manage()                { return null; }
        @Override public Navigation navigate()           { return null; }
        @Override public TargetLocator switchTo()        { return null; }
    }

    // =========================================================================
    // Fixtures
    // =========================================================================

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
        DriverManager.quitDriverSafely();
    }

    // =========================================================================
    // Aislamiento en ejecuciones paralelas
    // =========================================================================

    @Nested
    @DisplayName("Dos ExecutionContext paralelos — drivers distintos sin cruce")
    class DosContextosParalelosTests {

        @Test
        @DisplayName("Cada contexto devuelve su propio WebDriver desde ServiceRegistry")
        void cadaContextoDevuelveSuPropioDriver() throws Exception {
            CyclicBarrier barrier = new CyclicBarrier(2);
            CountDownLatch done   = new CountDownLatch(2);

            StubWebDriver driver1 = new StubWebDriver("driver-escenario-1");
            StubWebDriver driver2 = new StubWebDriver("driver-escenario-2");

            ExecutionContext ctx1 = ExecutionContext.builder().scenarioId("s-1").build();
            ExecutionContext ctx2 = ExecutionContext.builder().scenarioId("s-2").build();
            ctx1.registry().registerInstance(WebDriver.class, driver1);
            ctx2.registry().registerInstance(WebDriver.class, driver2);

            AtomicReference<WebDriver> recuperadoT1 = new AtomicReference<>();
            AtomicReference<WebDriver> recuperadoT2 = new AtomicReference<>();
            AtomicReference<Throwable> errorT1      = new AtomicReference<>();
            AtomicReference<Throwable> errorT2      = new AtomicReference<>();

            Thread t1 = new Thread(() -> {
                try {
                    ctx1.activate();
                    barrier.await(5, TimeUnit.SECONDS);
                    // Acceso canónico: desde ServiceRegistry del contexto activo
                    recuperadoT1.set(ExecutionContext.requireCurrent().service(WebDriver.class));
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
                    recuperadoT2.set(ExecutionContext.requireCurrent().service(WebDriver.class));
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
                    .as("T1 debe obtener su propio driver del ServiceRegistry")
                    .isSameAs(driver1);
            assertThat(recuperadoT2.get())
                    .as("T2 debe obtener su propio driver del ServiceRegistry")
                    .isSameAs(driver2);
            assertThat(recuperadoT1.get())
                    .as("Los drivers de ambos contextos deben ser instancias distintas")
                    .isNotSameAs(recuperadoT2.get());
        }

        @Test
        @DisplayName("WebDriverFactory.closeDriver de un contexto no afecta al driver del otro")
        void closeDriverAisladoPorContexto() throws Exception {
            StubWebDriver driver1 = new StubWebDriver("driver-1");
            StubWebDriver driver2 = new StubWebDriver("driver-2");

            ExecutionContext ctx1 = ExecutionContext.builder().scenarioId("s-1").build();
            ExecutionContext ctx2 = ExecutionContext.builder().scenarioId("s-2").build();
            ctx1.registry().registerInstance(WebDriver.class, driver1);
            ctx2.registry().registerInstance(WebDriver.class, driver2);

            // Cerrar solo ctx1
            WebDriverFactory.closeDriver(ctx1);

            // driver1 fue cerrado, driver2 NO
            assertThat(driver1.quitCalled)
                    .as("driver1 debe haber recibido quit() al cerrar ctx1")
                    .isTrue();
            assertThat(driver2.quitCalled)
                    .as("driver2 NO debe haber recibido quit() — ctx2 no fue cerrado")
                    .isFalse();

            // ctx2 aún tiene su driver válido
            assertThat(ctx2.registry().get(WebDriver.class))
                    .as("ctx2 aún debe tener su driver en el registry")
                    .isPresent()
                    .containsSame(driver2);
        }
    }

    // =========================================================================
    // Aislamiento con pool de hilos (N ejecuciones concurrentes)
    // =========================================================================

    @Nested
    @DisplayName("N ejecuciones concurrentes — sin contaminación cruzada")
    class NEjecucionesParalelasTests {

        @Test
        @DisplayName("Cinco escenarios paralelos — cada hilo ve solo su driver")
        void cincoEscenariosCadaUnoConSuDriver() throws Exception {
            int n = 5;
            CyclicBarrier barrier = new CyclicBarrier(n);
            CountDownLatch done   = new CountDownLatch(n);
            AtomicBoolean hayConflicto = new AtomicBoolean(false);

            for (int i = 0; i < n; i++) {
                StubWebDriver miDriver = new StubWebDriver("driver-" + i);
                ExecutionContext ctx   = ExecutionContext.builder().scenarioId("s-" + i).build();
                ctx.registry().registerInstance(WebDriver.class, miDriver);

                Thread t = new Thread(() -> {
                    try {
                        ctx.activate();
                        barrier.await(5, TimeUnit.SECONDS);

                        WebDriver recuperado = ExecutionContext.requireCurrent()
                                .service(WebDriver.class);

                        if (recuperado != miDriver) {
                            hayConflicto.set(true);
                        }
                    } catch (Throwable e) {
                        hayConflicto.set(true);
                    } finally {
                        ExecutionContext.deactivate();
                        done.countDown();
                    }
                });
                t.start();
            }

            done.await(15, TimeUnit.SECONDS);

            assertThat(hayConflicto.get())
                    .as("Ningún hilo debe ver el WebDriver de otro hilo")
                    .isFalse();
        }

        @Test
        @DisplayName("DriverManager (ThreadLocal) es independiente del ServiceRegistry del contexto")
        void driverManagerIndependienteDelServiceRegistry() throws Exception {
            // Demostración: DriverManager y ServiceRegistry son fuentes independientes.
            // Con ExecutionContext, el acceso canónico es ServiceRegistry; DriverManager
            // actúa solo como fallback para modo standalone.
            StubWebDriver driverEnRegistry  = new StubWebDriver("registry-driver");
            StubWebDriver driverEnManager   = new StubWebDriver("manager-driver");

            ExecutionContext ctx = ExecutionContext.builder().scenarioId("s-dm").build();
            ctx.registry().registerInstance(WebDriver.class, driverEnRegistry);
            ctx.activate();
            DriverManager.setDriver(driverEnManager);

            try {
                // El acceso vía ServiceRegistry devuelve el driver del contexto
                WebDriver desdeRegistry = ctx.service(WebDriver.class);
                // El acceso vía DriverManager devuelve el driver del ThreadLocal
                WebDriver desdeManager  = DriverManager.getDriver();

                assertThat(desdeRegistry).isSameAs(driverEnRegistry);
                assertThat(desdeManager).isSameAs(driverEnManager);
                assertThat(desdeRegistry).isNotSameAs(desdeManager);
            } finally {
                ExecutionContext.deactivate();
                DriverManager.quitDriverSafely();
            }
        }
    }
}
