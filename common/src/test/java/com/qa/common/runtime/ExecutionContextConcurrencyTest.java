package com.qa.common.runtime;

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

/**
 * Tests de concurrencia para {@link ExecutionContext}.
 *
 * <p>Verifica que dos {@link ExecutionContext} ejecutándose en hilos paralelos tienen
 * estado completamente aislado: cada hilo ve solo su propio contexto y sus propios
 * servicios, sin ninguna interferencia cruzada.
 *
 * <p>Premisa arquitectónica validada: los drivers (WebDriver, AppiumDriver) deben
 * obtenerse siempre desde el {@link ServiceRegistry} del contexto activo, no desde
 * singletons ni {@code ThreadLocal} globales desvinculados del contexto.
 *
 * @author Abel Venero
 * @since 2.3.0
 */
@DisplayName("ExecutionContext — aislamiento en ejecuciones concurrentes")
class ExecutionContextConcurrencyTest {

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    // =========================================================================
    // Stub mínimo que simula un "driver" sin dependencias externas
    // =========================================================================

    /** Representación genérica de cualquier recurso de sesión (WebDriver, AppiumDriver, etc.). */
    static final class StubSesion {
        final String id;
        boolean cerrada = false;

        StubSesion(String id) {
            this.id = id;
        }

        void cerrar() {
            cerrada = true;
        }

        @Override
        public String toString() {
            return "StubSesion{id='" + id + "'}";
        }
    }

    // =========================================================================
    // Aislamiento ThreadLocal
    // =========================================================================

    @Nested
    @DisplayName("Aislamiento de ThreadLocal entre hilos")
    class AislamientoThreadLocalTests {

        @Test
        @DisplayName("Un hilo no hereda el contexto activo del hilo principal")
        void workerNoHeredaContextoDelPrincipal() throws Exception {
            ExecutionContext ctxPrincipal = ExecutionContext.builder()
                    .scenarioId("scenario-principal")
                    .build();
            ctxPrincipal.activate();

            AtomicReference<ExecutionContext> vistoEnWorker = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            Thread worker = new Thread(() -> {
                vistoEnWorker.set(ExecutionContext.current().orElse(null));
                latch.countDown();
            });
            worker.start();
            latch.await(5, TimeUnit.SECONDS);

            // El hilo principal sigue viendo su contexto
            assertThat(ExecutionContext.requireCurrent()).isSameAs(ctxPrincipal);
            // El worker NO heredó el contexto del principal (ThreadLocal no se hereda)
            assertThat(vistoEnWorker.get())
                    .as("El worker no debe ver el contexto del hilo principal")
                    .isNull();
        }

        @Test
        @DisplayName("Dos hilos activan contextos distintos — cada uno ve el suyo")
        void dosHilosVenSusPropiosContextos() throws Exception {
            CyclicBarrier barrier = new CyclicBarrier(2);
            CountDownLatch done   = new CountDownLatch(2);

            ExecutionContext ctx1 = ExecutionContext.builder().scenarioId("s-1").build();
            ExecutionContext ctx2 = ExecutionContext.builder().scenarioId("s-2").build();

            AtomicReference<ExecutionContext> vistoPorT1 = new AtomicReference<>();
            AtomicReference<ExecutionContext> vistoPorT2 = new AtomicReference<>();
            AtomicReference<Throwable> errorT1 = new AtomicReference<>();
            AtomicReference<Throwable> errorT2 = new AtomicReference<>();

            Thread t1 = new Thread(() -> {
                try {
                    ctx1.activate();
                    barrier.await(5, TimeUnit.SECONDS);
                    vistoPorT1.set(ExecutionContext.requireCurrent());
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
                    vistoPorT2.set(ExecutionContext.requireCurrent());
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
            assertThat(vistoPorT1.get()).isSameAs(ctx1);
            assertThat(vistoPorT2.get()).isSameAs(ctx2);
            assertThat(vistoPorT1.get()).isNotSameAs(vistoPorT2.get());
        }
    }

    // =========================================================================
    // Aislamiento de ServiceRegistry (drivers/sesiones)
    // =========================================================================

    @Nested
    @DisplayName("ServiceRegistry — servicios aislados por contexto en paralelo")
    class AislamientoServiciosTests {

        @Test
        @DisplayName("Dos contextos paralelos tienen StubSesions distintas en sus registries")
        void dosContextosParalelosTienenSesionesDistintas() throws Exception {
            CyclicBarrier barrier = new CyclicBarrier(2);
            CountDownLatch done   = new CountDownLatch(2);

            StubSesion sesion1 = new StubSesion("sesion-escenario-1");
            StubSesion sesion2 = new StubSesion("sesion-escenario-2");

            ExecutionContext ctx1 = ExecutionContext.builder().scenarioId("s-1").build();
            ExecutionContext ctx2 = ExecutionContext.builder().scenarioId("s-2").build();
            ctx1.registry().registerInstance(StubSesion.class, sesion1);
            ctx2.registry().registerInstance(StubSesion.class, sesion2);

            AtomicReference<StubSesion> recuperadaT1 = new AtomicReference<>();
            AtomicReference<StubSesion> recuperadaT2 = new AtomicReference<>();
            AtomicReference<Throwable> errorT1 = new AtomicReference<>();
            AtomicReference<Throwable> errorT2 = new AtomicReference<>();

            Thread t1 = new Thread(() -> {
                try {
                    ctx1.activate();
                    barrier.await(5, TimeUnit.SECONDS);
                    recuperadaT1.set(ExecutionContext.requireCurrent().service(StubSesion.class));
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
                    recuperadaT2.set(ExecutionContext.requireCurrent().service(StubSesion.class));
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
            assertThat(recuperadaT1.get()).isSameAs(sesion1);
            assertThat(recuperadaT2.get()).isSameAs(sesion2);
            assertThat(recuperadaT1.get())
                    .as("Cada contexto devuelve su propia sesión — sin cruce")
                    .isNotSameAs(recuperadaT2.get());
        }

        @Test
        @DisplayName("cleanup de un contexto no destruye la sesión del otro")
        void cleanupDeUnContextoNoAfectaAlOtro() throws Exception {
            StubSesion sesion1 = new StubSesion("sesion-1");
            StubSesion sesion2 = new StubSesion("sesion-2");

            ExecutionContext ctx1 = ExecutionContext.builder().scenarioId("s-1").build();
            ExecutionContext ctx2 = ExecutionContext.builder().scenarioId("s-2").build();
            ctx1.registry().registerInstance(StubSesion.class, sesion1);
            ctx2.registry().registerInstance(StubSesion.class, sesion2);

            CountDownLatch ctx1Limpiado = new CountDownLatch(1);
            CountDownLatch done         = new CountDownLatch(1);

            AtomicBoolean ctx2TieneServicio = new AtomicBoolean();
            AtomicReference<Throwable> errorT2 = new AtomicReference<>();

            // T1 activa y limpia su contexto
            Thread t1 = new Thread(() -> {
                ctx1.activate();
                ctx1.cleanup(); // destruye registry de ctx1 — NO debe afectar ctx2
                ctx1Limpiado.countDown();
            });

            // T2 activa su contexto, espera a que T1 termine, y verifica que su sesión intacta
            Thread t2 = new Thread(() -> {
                try {
                    ctx2.activate();
                    ctx1Limpiado.await(5, TimeUnit.SECONDS);
                    ctx2TieneServicio.set(
                        ExecutionContext.requireCurrent().registry().isRegistered(StubSesion.class));
                } catch (Throwable t) {
                    errorT2.set(t);
                } finally {
                    ExecutionContext.deactivate();
                    done.countDown();
                }
            });

            t1.start();
            t2.start();
            t1.join(5_000);
            done.await(5, TimeUnit.SECONDS);

            assertThat(errorT2.get()).isNull();
            assertThat(ctx2TieneServicio.get())
                    .as("ctx2 debe seguir teniendo su sesión después del cleanup de ctx1")
                    .isTrue();
        }

        @Test
        @DisplayName("Cinco ejecuciones paralelas — cada una accede solo a su propia sesión")
        void cincoEjecucionesParalelasAisladas() throws Exception {
            int n = 5;
            CyclicBarrier barrier = new CyclicBarrier(n);
            CountDownLatch done   = new CountDownLatch(n);
            AtomicBoolean hayConflicto = new AtomicBoolean(false);

            for (int i = 0; i < n; i++) {
                StubSesion miSesion = new StubSesion("sesion-" + i);
                ExecutionContext ctx = ExecutionContext.builder().scenarioId("s-" + i).build();
                ctx.registry().registerInstance(StubSesion.class, miSesion);

                Thread t = new Thread(() -> {
                    try {
                        ctx.activate();
                        barrier.await(5, TimeUnit.SECONDS);
                        StubSesion recuperada = ExecutionContext.requireCurrent()
                                .service(StubSesion.class);
                        if (recuperada != miSesion) {
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
                    .as("Ningún hilo debe ver la sesión de otro — aislamiento garantizado")
                    .isFalse();
        }
    }

    // =========================================================================
    // Aislamiento de VariableStore
    // =========================================================================

    @Nested
    @DisplayName("VariableStore — variables aisladas por contexto en paralelo")
    class AislamientoVariablesTests {

        @Test
        @DisplayName("Variables de un contexto no son visibles desde el otro")
        void variablesAisladasEntreContextos() throws Exception {
            CyclicBarrier barrier = new CyclicBarrier(2);
            CountDownLatch done   = new CountDownLatch(2);

            ExecutionContext ctx1 = ExecutionContext.builder().scenarioId("s-1").build();
            ExecutionContext ctx2 = ExecutionContext.builder().scenarioId("s-2").build();
            ctx1.variables().set("clave", "valor-del-escenario-uno");
            ctx2.variables().set("clave", "valor-del-escenario-dos");

            AtomicReference<String> valorT1 = new AtomicReference<>();
            AtomicReference<String> valorT2 = new AtomicReference<>();
            AtomicReference<Throwable> errorT1 = new AtomicReference<>();
            AtomicReference<Throwable> errorT2 = new AtomicReference<>();

            Thread t1 = new Thread(() -> {
                try {
                    ctx1.activate();
                    barrier.await(5, TimeUnit.SECONDS);
                    valorT1.set(ExecutionContext.requireCurrent()
                            .variables().get("clave", String.class).orElse(null));
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
                    valorT2.set(ExecutionContext.requireCurrent()
                            .variables().get("clave", String.class).orElse(null));
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
            assertThat(valorT1.get()).isEqualTo("valor-del-escenario-uno");
            assertThat(valorT2.get()).isEqualTo("valor-del-escenario-dos");
        }
    }
}
