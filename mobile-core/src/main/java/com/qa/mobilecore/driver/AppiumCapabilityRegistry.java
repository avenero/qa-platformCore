package com.qa.mobilecore.driver;

import com.qa.common.api.pool.CapabilityDescriptor;
import com.qa.common.api.pool.CapabilityRegistry;
import com.qa.common.api.pool.DeviceHandle;
import com.qa.mobilecore.model.DeviceStatus;
import com.qa.mobilecore.model.DeviceType;
import com.qa.mobilecore.pool.DevicePool;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter que expone el {@link DevicePool} de mobile-core como un {@link CapabilityRegistry}
 * del contrato común.
 *
 * <p>Permite al backend consultar en tiempo real qué dispositivos móviles están disponibles
 * y reservar uno antes de lanzar una ejecución, sin conocer los detalles de Appium o ADB.
 *
 * <h2>Modelo de reserva</h2>
 * <p>{@link #acquire} delega en {@link DevicePool#tryAcquire}, que hace CAS atómico
 * en el estado del dispositivo. Si el dispositivo está libre lo marca BUSY y retorna un
 * {@link DeviceHandle}; si está ocupado retorna {@code Optional.empty()} sin bloquear.
 *
 * <h2>Lease de dispositivo</h2>
 * <p>El lease asignado ({@value #MOBILE_LEASE_HOURS}h) cubre una sesión de pruebas típica.
 * El caller siempre debe invocar {@link #release} al finalizar para liberar el dispositivo.
 *
 * <h2>platformId()</h2>
 * <p>Retorna {@code "MOBILE"} para alinearse con {@code MobilePlugin.platformId()}.
 * Los {@link CapabilityDescriptor} individuales indican {@code "ANDROID"} o {@code "IOS"}
 * según el tipo real del dispositivo.
 *
 * @see CapabilityRegistry
 * @see DevicePool
 * @since 3.1.0
 */
public class AppiumCapabilityRegistry implements CapabilityRegistry {

    static final int MOBILE_LEASE_HOURS = 4;
    static final Duration MOBILE_LEASE = Duration.ofHours(MOBILE_LEASE_HOURS);

    private final DevicePool pool;

    public AppiumCapabilityRegistry(DevicePool pool) {
        this.pool = Objects.requireNonNull(pool, "pool no puede ser null");
    }

    @Override
    public String platformId() {
        return "MOBILE";
    }

    /**
     * Lista todos los dispositivos registrados en el pool con su estado actual.
     *
     * <p>Usa {@link DevicePool#getAllDevices()} (dispositivos ya inicializados) en lugar
     * de invocar el scanner en cada llamada — el escaneo es costoso (ADB/xcrun) y se hace
     * solo en {@link DevicePool#initialize}.
     *
     * @return snapshot del estado de todos los dispositivos; nunca null
     */
    @Override
    public List<CapabilityDescriptor> list() {
        return pool.getAllDevices().stream()
                .map(this::toDescriptor)
                .toList();
    }

    /**
     * Verifica si un dispositivo puede reservarse en este instante.
     *
     * <p>Consulta no-bloqueante: {@code true} no garantiza éxito en un {@link #acquire}
     * posterior si otro thread lo reserva entre medio.
     *
     * @param capabilityId ID del dispositivo (ej: {@code "pixel7"})
     * @return {@code true} si el dispositivo existe y está en estado AVAILABLE
     */
    @Override
    public boolean isAvailable(String capabilityId) {
        if (capabilityId == null || capabilityId.isBlank()) {
            return false;
        }
        return pool.getStatus(capabilityId)
                .map(s -> s == DeviceStatus.AVAILABLE)
                .orElse(false);
    }

    /**
     * Reserva un dispositivo para uso exclusivo.
     *
     * <p>Operación no-bloqueante: si el dispositivo está ocupado retorna
     * {@code Optional.empty()} inmediatamente sin esperar {@code timeout}.
     * El parámetro {@code timeout} está presente por simetría de contrato y se
     * reserva para una implementación futura con espera real.
     *
     * @param capabilityId ID del dispositivo a reservar; no null ni vacío
     * @param timeout      por contrato no null y positivo; no usado en esta versión
     * @return handle con sessionId único si el dispositivo fue reservado, o vacío
     * @throws IllegalArgumentException si {@code capabilityId} es null/vacío o {@code timeout} ≤ 0
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
        return pool.tryAcquire(capabilityId, timeout)
                .map(device -> new DeviceHandle(
                        capabilityId,
                        UUID.randomUUID().toString(),
                        Instant.now(),
                        MOBILE_LEASE));
    }

    /**
     * Libera el dispositivo reservado previamente.
     *
     * <p>Idempotente: liberar un handle ya liberado es silencioso.
     * El {@code sessionId} del handle no se valida — el pool solo trackea el deviceId.
     *
     * @param handle token emitido por {@link #acquire}; no null
     */
    @Override
    public void release(DeviceHandle handle) {
        Objects.requireNonNull(handle, "handle no puede ser null");
        pool.release(handle.capabilityId());
    }

    // =========================================================================
    // Mapping privado
    // =========================================================================

    private CapabilityDescriptor toDescriptor(com.qa.mobilecore.model.DeviceDescriptor d) {
        DeviceStatus status = pool.getStatus(d.getId()).orElse(DeviceStatus.UNKNOWN);
        boolean available = (status == DeviceStatus.AVAILABLE);
        boolean inUse     = (status == DeviceStatus.BUSY);
        Long eta          = inUse ? pool.estimatedFreeSeconds(d.getId()) : null;

        String name = buildFriendlyName(d);
        String platformId = resolvePlatformId(d.getType());
        String type       = resolveType(d.getType());

        return new CapabilityDescriptor(
                d.getId(),
                name,
                platformId,
                type,
                available,
                inUse,
                eta,
                Map.of("osVersion", nullToEmpty(d.getPlatformVersion())));
    }

    private static String buildFriendlyName(com.qa.mobilecore.model.DeviceDescriptor d) {
        String base = d.getDeviceName() != null ? d.getDeviceName() : d.getId();
        String platform = d.getPlatformName() != null ? d.getPlatformName() : "";
        String version  = d.getPlatformVersion() != null ? d.getPlatformVersion() : "";
        if (!platform.isBlank() && !version.isBlank()) {
            return "%s (%s %s)".formatted(base, platform, version);
        } else if (!platform.isBlank()) {
            return "%s (%s)".formatted(base, platform);
        }
        return base;
    }

    private static String resolvePlatformId(DeviceType type) {
        if (type.isAndroid()) { return "ANDROID"; }
        if (type.isIOS()) { return "IOS"; }
        return "MOBILE"; // REMOTE_GRID
    }

    private static String resolveType(DeviceType type) {
        return type.isVirtual() ? "EMULATOR" : "REAL";
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
