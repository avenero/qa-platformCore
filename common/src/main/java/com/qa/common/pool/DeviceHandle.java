package com.qa.common.pool;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Token de sesión reservada emitido por {@link CapabilityRegistry#acquire}.
 *
 * <p>Actúa como recibo: el caller recibe un {@code DeviceHandle} al reservar un recurso
 * y lo devuelve en {@link CapabilityRegistry#release} para liberar la reserva. El registry
 * usa el {@code sessionId} para identificar qué sesión está ocupando el recurso.
 *
 * <h2>Ciclo de vida</h2>
 * <pre>
 * Optional&lt;DeviceHandle&gt; handle = registry.acquire("pixel7", Duration.ofSeconds(30));
 * handle.ifPresent(h -&gt; {
 *     try {
 *         // ... ejecutar tests en el dispositivo ...
 *     } finally {
 *         registry.release(h);
 *     }
 * });
 * </pre>
 *
 * <h2>Lease y expiración</h2>
 * <p>El campo {@code leaseDuration} establece el tiempo máximo garantizado de la reserva.
 * Pasado ese tiempo, el registry puede liberar el recurso automáticamente. El caller debe
 * llamar a {@code release} antes de que expire para evitar condiciones de carrera.
 * Usar {@link Duration#ofSeconds(long) Duration.ofSeconds(Long.MAX_VALUE / 2)} para leases
 * indefinidos (ej: browsers web siempre disponibles).
 *
 * @param capabilityId  identificador del recurso reservado (ej: {@code "pixel7"})
 * @param sessionId     identificador único de esta sesión (UUID recomendado)
 * @param acquiredAt    instante en que se realizó la reserva
 * @param leaseDuration duración máxima de la reserva; nunca null
 *
 * @see CapabilityRegistry
 * @see CapabilityDescriptor
 * @since 3.1.0
 */
public record DeviceHandle(
    String capabilityId,
    String sessionId,
    Instant acquiredAt,
    Duration leaseDuration
) {

    /** Validación compacta del constructor canónico. */
    public DeviceHandle {
        Objects.requireNonNull(capabilityId, "capabilityId no puede ser null");
        if (capabilityId.isBlank()) {
            throw new IllegalArgumentException("capabilityId no puede ser vacío");
        }
        Objects.requireNonNull(sessionId, "sessionId no puede ser null");
        if (sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId no puede ser vacío");
        }
        Objects.requireNonNull(acquiredAt, "acquiredAt no puede ser null");
        Objects.requireNonNull(leaseDuration, "leaseDuration no puede ser null");
        if (leaseDuration.isNegative() || leaseDuration.isZero()) {
            throw new IllegalArgumentException("leaseDuration debe ser positiva, fue: " + leaseDuration);
        }
    }

    /**
     * Indica si el lease de esta reserva ha expirado.
     *
     * <p>Cuando retorna {@code true} el registry puede haber liberado ya el recurso;
     * el caller debe asumir que no tiene más acceso garantizado al mismo.
     *
     * @return {@code true} si {@code Instant.now()} supera {@code acquiredAt + leaseDuration}
     */
    public boolean isExpired() {
        return Instant.now().isAfter(acquiredAt.plus(leaseDuration));
    }

    /**
     * Tiempo restante del lease desde este instante.
     *
     * @return duración restante; {@link Duration#ZERO} si ya expiró
     */
    public Duration remainingLease() {
        Instant expiry = acquiredAt.plus(leaseDuration);
        Duration remaining = Duration.between(Instant.now(), expiry);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
