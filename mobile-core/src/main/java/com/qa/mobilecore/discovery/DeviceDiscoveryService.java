package com.qa.mobilecore.discovery;

import com.qa.common.config.ConfigManager;
import com.qa.common.logging.TestLogger;
import com.qa.mobilecore.config.MobileConfigKeys;
import com.qa.mobilecore.model.DeviceDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Servicio de descubrimiento automático de dispositivos mobile.
 *
 * <p>Orquesta el escaneo de dispositivos Android (via {@link AdbDeviceScanner})
 * e iOS (via {@link IosDeviceScanner}), retornando una lista unificada de
 * {@link DeviceDescriptor} disponibles para ser registrados en el {@code DevicePool}.
 *
 * <p><b>Integración con el Backend (BE):</b>
 * El BE puede exponer un endpoint (ej: {@code GET /api/devices}) que invoque
 * este servicio para retornar al FE la lista de dispositivos disponibles.
 * El usuario selecciona el dispositivo desde el FE, y el BE envía el
 * {@code mobile.device.id} en el {@code ExecutionRequest.properties}.
 *
 * <p><b>Instrucciones para el BE:</b>
 * <ol>
 *   <li>Instanciar {@code DeviceDiscoveryService}</li>
 *   <li>Llamar a {@code discoverAll()} para obtener la lista de dispositivos</li>
 *   <li>Serializar la lista como JSON y exponerla en {@code GET /api/devices}</li>
 *   <li>Al recibir una ejecución desde el FE, incluir:
 *       {@code properties.put("mobile.device.id", selectedDeviceId)}</li>
 * </ol>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class DeviceDiscoveryService {

    private final AdbDeviceScanner adbScanner;
    private final IosDeviceScanner iosScanner;
    private final ConfigManager    config;

    public DeviceDiscoveryService() {
        this.adbScanner = new AdbDeviceScanner();
        this.iosScanner = new IosDeviceScanner();
        this.config     = ConfigManager.getInstance();
    }

    /**
     * Descubre todos los dispositivos disponibles (Android + iOS).
     *
     * <p>Respeta las propiedades de configuración:
     * <ul>
     *   <li>{@code mobile.discovery.include.virtual} (default: {@code true})</li>
     *   <li>{@code mobile.discovery.include.physical} (default: {@code true})</li>
     * </ul>
     *
     * @return lista inmutable de dispositivos detectados; nunca null
     */
    public List<DeviceDescriptor> discoverAll() {
        boolean includeVirtual  = config.getBoolean(MobileConfigKeys.DISCOVERY_INCLUDE_VIRTUAL,  true);
        boolean includePhysical = config.getBoolean(MobileConfigKeys.DISCOVERY_INCLUDE_PHYSICAL, true);

        TestLogger.logInfo("DISCOVERY",
            "Iniciando descubrimiento de dispositivos (virtual=" + includeVirtual
            + ", physical=" + includePhysical + ")", null);

        List<DeviceDescriptor> all = new ArrayList<>();
        all.addAll(adbScanner.scan(includeVirtual, includePhysical));
        all.addAll(iosScanner.scan(includeVirtual));

        TestLogger.logInfo("DISCOVERY",
            "Descubrimiento completado: " + all.size() + " dispositivo(s) en total", null);
        return Collections.unmodifiableList(all);
    }

    /**
     * Descubre solo dispositivos Android.
     */
    public List<DeviceDescriptor> discoverAndroid() {
        boolean includeVirtual  = config.getBoolean(MobileConfigKeys.DISCOVERY_INCLUDE_VIRTUAL,  true);
        boolean includePhysical = config.getBoolean(MobileConfigKeys.DISCOVERY_INCLUDE_PHYSICAL, true);
        return Collections.unmodifiableList(adbScanner.scan(includeVirtual, includePhysical));
    }

    /**
     * Descubre solo dispositivos iOS.
     */
    public List<DeviceDescriptor> discoverIOS() {
        boolean includeVirtual = config.getBoolean(MobileConfigKeys.DISCOVERY_INCLUDE_VIRTUAL, true);
        return Collections.unmodifiableList(iosScanner.scan(includeVirtual));
    }

    /**
     * Indica si ADB está disponible en el sistema.
     */
    public boolean isAdbAvailable() {
        return adbScanner.isAdbAvailable();
    }

    /**
     * Indica si xcrun (Xcode) está disponible (solo macOS).
     */
    public boolean isXcrunAvailable() {
        return iosScanner.isXcrunAvailable();
    }
}
