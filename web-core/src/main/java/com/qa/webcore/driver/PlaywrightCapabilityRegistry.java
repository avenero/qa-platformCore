package com.qa.webcore.driver;

import com.qa.common.pool.CapabilityDescriptor;
import com.qa.common.pool.CapabilityRegistry;
import com.qa.common.pool.DeviceHandle;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Implementación de {@link CapabilityRegistry} para la plataforma web.
 *
 * <p>Los browsers web son recursos ilimitados: no hay pool compartido, no hay reserva real.
 * {@link #acquire} siempre retorna un {@link DeviceHandle} válido (siempre que el id sea
 * soportado); {@link #release} es no-op. Esto mantiene simetría de contrato con
 * {@code AppiumCapabilityRegistry} (TASK-D04) sin agregar complejidad innecesaria.
 *
 * <p>Browsers soportados: {@code chromium}, {@code firefox}, {@code webkit}, {@code edge}.
 * Corresponden a los motores que Playwright puede controlar de forma nativa.
 *
 * @see CapabilityRegistry
 * @since 3.1.0
 */
public class PlaywrightCapabilityRegistry implements CapabilityRegistry {

    /**
     * Duración de lease asignada a cada handle web: 100 años.
     * Modela "sin expiración práctica" evitando el desbordamiento de {@code Instant.MAX}
     * que causaría {@code Duration.ofSeconds(Long.MAX_VALUE / 2)}.
     */
    static final Duration WEB_LEASE = Duration.ofDays(365L * 100);

    private static final Set<String> SUPPORTED = Set.of("chromium", "firefox", "webkit", "edge");

    @Override
    public String platformId() {
        return "WEB";
    }

    @Override
    public List<CapabilityDescriptor> list() {
        return SUPPORTED.stream()
                .sorted()
                .map(id -> CapabilityDescriptor.available(id, friendlyName(id), "WEB", "BROWSER"))
                .toList();
    }

    @Override
    public boolean isAvailable(String capabilityId) {
        if (capabilityId == null || capabilityId.isBlank()) {
            return false;
        }
        return SUPPORTED.contains(capabilityId);
    }

    /**
     * Reserva un browser web. La operación es instantánea: no bloquea, no hay pool.
     *
     * @param capabilityId browser a reservar (chromium / firefox / webkit / edge)
     * @param timeout      ignorado para web — incluido por simetría de contrato; no debe ser null
     * @return handle con lease prácticamente infinito, o vacío si el id no es soportado
     * @throws IllegalArgumentException si {@code capabilityId} es null/vacío o {@code timeout} no es positivo
     */
    @Override
    public Optional<DeviceHandle> acquire(String capabilityId, Duration timeout) {
        Objects.requireNonNull(capabilityId, "capabilityId no puede ser null");
        if (capabilityId.isBlank()) {
            throw new IllegalArgumentException("capabilityId no puede ser vacío");
        }
        Objects.requireNonNull(timeout, "timeout no puede ser null");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout debe ser positivo, fue: " + timeout);
        }
        if (!isAvailable(capabilityId)) {
            return Optional.empty();
        }
        return Optional.of(new DeviceHandle(
                capabilityId,
                UUID.randomUUID().toString(),
                Instant.now(),
                WEB_LEASE));
    }

    /**
     * No-op: los browsers web no son un recurso escaso que deba liberarse.
     *
     * @param handle token de sesión previo; no null (validación silenciosa por idempotencia)
     */
    @Override
    public void release(DeviceHandle handle) {
        // no-op: web browsers no son recurso escaso
    }

    private static String friendlyName(String id) {
        return switch (id) {
            case "chromium" -> "Chrome / Chromium";
            case "firefox"  -> "Mozilla Firefox";
            case "webkit"   -> "Safari / WebKit";
            case "edge"     -> "Microsoft Edge";
            default         -> id;
        };
    }
}
