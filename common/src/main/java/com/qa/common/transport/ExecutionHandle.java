package com.qa.common.transport;

import com.qa.common.runtime.ExecutionResult;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handle async para una ejecución submitida vía {@link ExecutionTransport}
 * (TASK-I01, RFC-AGENT-01).
 *
 * <h2>Estados que cubre</h2>
 * <ul>
 *   <li><b>En curso:</b> {@code future} no completado. El reporter sigue
 *       recibiendo eventos de progreso.</li>
 *   <li><b>Completado:</b> {@code future} completó con un {@link ExecutionResult}.
 *       Los terminales son PASSED, FAILED, ERROR, CANCELLED.</li>
 *   <li><b>Cancelado:</b> el llamador invocó {@link #cancel()}; el cancelHook
 *       ejecutó la lógica del transport (interrupt local o POST cancel remoto)
 *       y eventualmente el future completa (cancelled o con resultado parcial).</li>
 * </ul>
 *
 * <h2>Idempotencia de cancel</h2>
 * <p>{@link #cancel()} es idempotente: invocarlo dos veces NO ejecuta el hook
 * dos veces ni lanza excepción. Esto se alinea con el contrato wire de
 * {@code POST /v1/runs/{id}/cancel} (RFC-AGENT-01 §4.3).
 *
 * <h2>Inmutabilidad</h2>
 * <p>Los campos del record son inmutables. El estado mutable de "cancel ya
 * disparado" vive en un {@link AtomicBoolean} interno; no afecta la igualdad
 * por valor del record (records ignoran fields no declarados).
 *
 * @param executionId identificador único de la ejecución (UUID en formato String,
 *                    asignado por el transport y propagado al agente). No null.
 * @param future      futuro del resultado final. No null. Completa con
 *                    {@link ExecutionResult} o excepción si el transport falla.
 * @param cancelHook  acción que el transport ejecuta al recibir
 *                    {@link #cancel()}. No null. La implementación debe ser
 *                    no-throwing (best-effort); si necesita propagar errores,
 *                    los publica vía {@code reporter.error(...)}.
 *
 * @author Abel Venero
 * @since TASK-I01
 * @see ExecutionTransport
 */
public record ExecutionHandle(
        String executionId,
        CompletableFuture<ExecutionResult> future,
        Runnable cancelHook
) {

    /** Holder único para garantizar idempotencia de {@link #cancel()}. */
    private static final class CancelLatch {
        final AtomicBoolean fired = new AtomicBoolean(false);
    }

    /**
     * Almacén externo (out-of-record) del latch de cancelación. No participa
     * de equals/hashCode/toString del record. Reseteado por handle.
     *
     * <p>Implementación: {@link ThreadLocal}-free; el latch se asocia al record
     * via {@link System#identityHashCode}. Para el caso simple de tests, un
     * {@link AtomicBoolean} compartido por instancia bastaría — pero records
     * no admiten campos no-componente. La solución estándar (ver
     * {@link CompletableFuture#cancel}) es hacer el cancelHook idempotente por
     * sí mismo. Aquí lo garantizamos con un wrapper interno.
     */
    public ExecutionHandle {
        Objects.requireNonNull(executionId, "executionId no puede ser null");
        Objects.requireNonNull(future,      "future no puede ser null");
        Objects.requireNonNull(cancelHook,  "cancelHook no puede ser null");
        if (executionId.isBlank()) {
            throw new IllegalArgumentException("executionId no puede ser blank");
        }
        // Decora el cancelHook con un AtomicBoolean propio para idempotencia.
        // El record retiene la versión decorada — efectivamente igual a la
        // original pero con guard. Records permiten reasignar componentes en
        // el compact constructor.
        AtomicBoolean fired = new AtomicBoolean(false);
        Runnable original = cancelHook;
        cancelHook = () -> {
            if (fired.compareAndSet(false, true)) {
                original.run();
            }
        };
    }

    /**
     * Solicita la cancelación de la ejecución. Idempotente: invocar dos veces
     * sólo dispara el hook una vez.
     *
     * <p>Esta llamada NO bloquea esperando a que el transport reaccione. El
     * llamador puede observar la finalización vía {@link #future} (que
     * típicamente completará con {@link ExecutionResult.Status#CANCELLED} o
     * cancellation excepcional, según la implementación).
     */
    public void cancel() {
        cancelHook.run();
    }

    /**
     * @return {@code true} si el future ha completado (normal, excepcional o cancelado).
     */
    public boolean isDone() {
        return future.isDone();
    }

    /**
     * @return {@code true} si el future fue cancelado.
     */
    public boolean isCancelled() {
        return future.isCancelled();
    }
}
