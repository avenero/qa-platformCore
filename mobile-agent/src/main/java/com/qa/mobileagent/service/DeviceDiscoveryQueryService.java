package com.qa.mobileagent.service;

import com.qa.common.api.transport.MobileDeviceDescriptor;
import com.qa.mobilecore.discovery.DeviceDiscoveryService;
import com.qa.mobilecore.model.DeviceDescriptor;
import com.qa.mobilecore.model.DeviceType;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Wrapper Spring sobre {@link DeviceDiscoveryService} (mobile-core) que expone
 * el resultado en el formato wire-protocol del agente
 * ({@link MobileDeviceDescriptor}).
 *
 * <p>Mapeo:
 * <ul>
 *   <li>{@code id, name, platformVersion} → directos.</li>
 *   <li>{@link DeviceType} enum → {@code type} string canónico.</li>
 *   <li>{@code platformName} → {@code "android"} | {@code "ios"} (lowercase).
 *       El descubrimiento real ya separa Android e iOS, pero {@code DeviceDescriptor}
 *       no expone {@code platformName} (es derivable del {@code DeviceType}); aquí
 *       lo derivamos para evitar otra source-of-truth en el wire format.</li>
 *   <li>{@code status} → {@code "AVAILABLE"} por defecto post-discovery. La
 *       semántica {@code BUSY}/{@code OFFLINE} pertenece al {@code DevicePool}
 *       en runtime de ejecución; en discovery puro no aplica todavía. Se deja
 *       como string para permitir extensión futura sin romper el wire.</li>
 *   <li>{@code udid} — <b>NO se publica</b>. PII para devices BYOD físicos.</li>
 * </ul>
 *
 * @since TASK-K03COV-BE7
 */
@Service
public class DeviceDiscoveryQueryService {

    private final DeviceDiscoveryService discovery;

    public DeviceDiscoveryQueryService(DeviceDiscoveryService discovery) {
        this.discovery = discovery;
    }

    public List<MobileDeviceDescriptor> listAvailable() {
        return discovery.discoverAll().stream().map(DeviceDiscoveryQueryService::toWire).toList();
    }

    static MobileDeviceDescriptor toWire(DeviceDescriptor d) {
        return new MobileDeviceDescriptor(
                d.getId(),
                d.getDeviceName(),
                d.getType().name(),
                derivePlatform(d.getType()),
                d.getPlatformVersion(),
                "AVAILABLE"
        );
    }

    private static String derivePlatform(DeviceType type) {
        return switch (type) {
            case ANDROID_EMULATOR, ANDROID_PHYSICAL -> "android";
            case IOS_SIMULATOR, IOS_PHYSICAL        -> "ios";
            case REMOTE_GRID                        -> "unknown";
        };
    }
}
