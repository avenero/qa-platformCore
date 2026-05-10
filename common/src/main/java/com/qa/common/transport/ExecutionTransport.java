package com.qa.common.transport;

import com.qa.common.driver.CapabilityReport;
import com.qa.common.reporter.StepReporter;
import com.qa.common.runtime.ExecutionConfig;

import java.util.List;

/**
 * Contrato para invocar el {@code CucumberRuntimeEngine} del Core de forma
 * agnóstica al lugar donde corre (TASK-I01, RFC-AGENT-01).
 *
 * <h2>Modos soportados (no implementados aquí; ver tareas siguientes)</h2>
 * <ul>
 *   <li><b>{@code InProcessTransport}</b> (TASK-I02) — invoca al engine en la
 *       misma JVM. Default para CI / tests / single-host. Sin overhead de red.</li>
 *   <li><b>{@code HttpAgentTransport}</b> (TASK-I03) — delega la ejecución a un
 *       agente remoto (`mobile-agent`, `web-agent`, …) vía HTTP/SSE. Necesario
 *       para mobile real con Android SDK físicamente separado del BE, BYO-runner
 *       desde laptop del dev, y escalado horizontal.</li>
 * </ul>
 *
 * <h2>Por qué esta abstracción</h2>
 * <p>El BE (capa application) consume {@link ExecutionTransport} sin saber dónde
 * corre el engine. Cambiar de in-process a remoto es una decisión 100% de
 * configuración (routing por pool de capabilities). El contrato es deliberadamente
 * minimalista (2 métodos) para que cualquier transport futuro — mensajería,
 * worker queue, etc. — pueda implementarlo sin cambios en la capa de aplicación.
 *
 * <h2>Reglas inviolables (RFC-AGENT-01 §16)</h2>
 * <ul>
 *   <li><b>R-1.</b> El BE en producción NO instancia {@code InProcessTransport}.
 *       Lo usa sólo en CI / tests integrados. Producción siempre
 *       {@code HttpAgentTransport}.</li>
 *   <li><b>R-2.</b> El wire protocol {@code v1} no se rompe nunca. Cambios en
 *       eventos = añadir campos opcionales o un evento nuevo, NUNCA renombrar
 *       o borrar.</li>
 *   <li><b>R-5.</b> Tokens de auth NUNCA en logs.</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * <p>Las implementaciones DEBEN ser thread-safe a nivel de método: múltiples
 * hilos pueden llamar {@link #submit} concurrentemente. Cada llamada genera un
 * {@link ExecutionHandle} independiente.
 *
 * @author Abel Venero
 * @since TASK-I01
 * @see ExecutionHandle
 */
public interface ExecutionTransport {

    /**
     * Inicia la ejecución de forma asíncrona y retorna un handle observable.
     *
     * <p>El llamador NO espera la finalización aquí; obtiene un
     * {@link ExecutionHandle} cuyo {@code future} completará cuando el engine
     * termine (o falle). Mientras tanto, el {@code reporter} recibe los
     * eventos de progreso (scenario start/finish, step pass/fail, etc.).
     *
     * @param config   configuración de la ejecución, no null. Debe contener al
     *                 menos {@code environment} y los campos relevantes para
     *                 la capa (browser, mobile, httpEngine, etc.).
     * @param reporter consumidor de eventos de progreso, no null. La instancia
     *                 viaja al lugar donde corre el engine (en remoto, los
     *                 eventos llegan via SSE y se traducen a llamadas a este
     *                 reporter).
     * @return handle async con executionId único, future del resultado y
     *         hook de cancelación. Nunca null.
     * @throws NullPointerException  si {@code config} o {@code reporter} son null.
     * @throws IllegalStateException si el transport está en estado de drain o
     *                               no puede aceptar nuevos runs (sólo
     *                               implementaciones remotas).
     */
    ExecutionHandle submit(ExecutionConfig config, StepReporter reporter);

    /**
     * Describe las capabilities que este transport puede atender (devices,
     * navegadores, plataformas) en este momento.
     *
     * <p>Usado por el {@code BE.transportRouter} para elegir el agente / pool
     * correcto al hacer routing. En in-process refleja las capabilities
     * descubiertas por SPI; en remoto refleja la respuesta de
     * {@code GET /v1/capabilities} del agente (con caché corto).
     *
     * @return lista (posiblemente vacía) de capabilities. Nunca null.
     */
    List<CapabilityReport> describeCapabilities();
}
