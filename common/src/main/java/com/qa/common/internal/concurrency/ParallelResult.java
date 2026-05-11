package com.qa.common.internal.concurrency;

import com.qa.common.utils.security.SecurityUtilities;

/**
 * Resultado de la ejecución de una tarea en el {@link ParallelStepExecutor}.
 *
 * @param success   true si la tarea completó sin excepción ni timeout
 * @param timedOut  true si la tarea fue cancelada por timeout
 * @param value     valor retornado por la tarea (null si falló o timed-out)
 * @param error     excepción capturada (null si success o timed-out)
 * @since 2.1.0
 */
public record ParallelResult(boolean success, boolean timedOut, Object value, Throwable error) {

    public static ParallelResult success(Object value) {
        return new ParallelResult(true, false, value, null);
    }

    public static ParallelResult failure(Throwable error) {
        return new ParallelResult(false, false, null, error);
    }

    public static ParallelResult timeout() {
        return new ParallelResult(false, true, null, null);
    }

    public boolean failed() {
        return !success && !timedOut;
    }
}
