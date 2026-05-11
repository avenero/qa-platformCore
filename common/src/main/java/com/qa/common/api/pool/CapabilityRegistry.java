package com.qa.common.api.pool;
import com.qa.common.utils.security.SecurityUtilities;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Contrato para gestionar recursos reservables en una plataforma de testing.
 *
 * <p>Permite al backend preguntar qué dispositivos/browsers están disponibles, verificar
 * disponibilidad puntual y reservar un recurso para una ejecución. Cada plataforma
 * (web, mobile, database) proporciona su propia implementación.
 *
 * <h2>Modelo de reserva</h2>
 * <pre>
 * // Consulta previa (no bloquea)
 * boolean free = registry.isAvailable("pixel7");
 *
 * // Intento de reserva con timeout
 * Optional&lt;DeviceHandle&gt; handle = registry.acquire("pixel7", Duration.ofSeconds(30));
 * handle.ifPresent(h -&gt; {
 *     try {
 *         runTestsOn(h);
 *     } finally {
 *         registry.release(h);   // siempre liberar
 *     }
 * });
 * </pre>
 *
 * <h2>Invariantes</h2>
 * <ul>
 *   <li>{@link #list()} nunca retorna null; puede retornar lista vacía.</li>
 *   <li>{@link #acquire} retorna {@link Optional#empty()} (no lanza excepción) si el recurso
 *       no está disponible tras el timeout.</li>
 *   <li>{@link #release} es idempotente: liberar un handle ya liberado no produce error.</li>
 *   <li>Una implementación es singleton por plataforma — el pool interno serializa accesos
 *       concurrentes. El caller no debe sincronizar externamente.</li>
 * </ul>
 *
 * <h2>Implementaciones previstas</h2>
 * <ul>
 *   <li>{@code PlaywrightCapabilityRegistry} (web-core, TASK-D03) — acquire trivial, siempre disponible</li>
 *   <li>{@code AppiumCapabilityRegistry} (mobile-core, TASK-D04) — delega en {@code DevicePool} existente</li>
 * </ul>
 *
 * @see CapabilityDescriptor
 * @see DeviceHandle
 * @since 3.1.0
 */
public interface CapabilityRegistry {

    /**
     * Lista todos los recursos gestionados por este registry con su estado actual.
     *
     * <p>La lista refleja el estado en el instante de la llamada; puede cambiar si otros
     * threads adquieren o liberan recursos concurrentemente.
     *
     * @return snapshot inmutable de los recursos; nunca null, puede ser vacía
     */
    List<CapabilityDescriptor> list();

    /**
     * Verifica si un recurso concreto puede ser reservado en este instante.
     *
     * <p>Es una consulta no bloqueante. {@code true} no garantiza que un {@link #acquire}
     * posterior tenga éxito si otro thread reservó el recurso entre medio.
     *
     * @param capabilityId identificador del recurso (ej: {@code "pixel7"}, {@code "chromium"})
     * @return {@code true} si el recurso existe y está libre ahora mismo
     */
    boolean isAvailable(String capabilityId);

    /**
     * Reserva un recurso para uso exclusivo por el tiempo indicado como lease.
     *
     * <p>Si el recurso no está libre inmediatamente, el método bloqueará hasta {@code timeout}
     * esperando que se libere. Retorna {@link Optional#empty()} si transcurre el timeout sin
     * conseguir la reserva.
     *
     * @param capabilityId identificador del recurso a reservar; no null
     * @param timeout      tiempo máximo de espera; no null, debe ser positivo
     * @return {@link Optional} con el token de sesión si la reserva tuvo éxito, o vacío en caso contrario
     * @throws IllegalArgumentException si {@code capabilityId} es null/vacío o {@code timeout} no es positivo
     */
    Optional<DeviceHandle> acquire(String capabilityId, Duration timeout);

    /**
     * Libera un recurso reservado previamente.
     *
     * <p>El handle queda invalidado tras esta llamada. Llamadas posteriores con el mismo handle
     * son silenciosas (idempotente). El recurso pasa a disponible para futuras reservas.
     *
     * @param handle token de sesión retornado por {@link #acquire}; no null
     */
    void release(DeviceHandle handle);

    /**
     * Identificador de la plataforma que gestiona este registry.
     *
     * <p>Debe coincidir con el {@code platformId} de los {@link CapabilityDescriptor}
     * retornados por {@link #list()} y con el valor retornado por
     * {@link com.qa.common.runtime.CorePlugin#platformId()}.
     *
     * @return identificador de plataforma (ej: {@code "WEB"}, {@code "ANDROID"}, {@code "DATABASE"})
     */
    String platformId();
}
