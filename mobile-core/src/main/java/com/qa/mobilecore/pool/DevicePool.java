package com.qa.mobilecore.pool;

import com.qa.common.config.ConfigManager;
import com.qa.common.logging.TestLogger;
import com.qa.mobilecore.config.MobileConfigKeys;
import com.qa.mobilecore.discovery.DeviceDiscoveryService;
import com.qa.mobilecore.model.DeviceDescriptor;
import com.qa.mobilecore.model.DeviceStatus;
import com.qa.mobilecore.model.DeviceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pool de dispositivos mobile thread-safe.
 *
 * <p>Gestiona la asignación exclusiva de dispositivos a ejecuciones paralelas.
 * Cada dispositivo registrado tiene un puerto Appium único asignado
 * (base + índice), lo que permite levantar múltiples sesiones Appium
 * simultáneamente sin colisiones de puerto.
 *
 * <p><b>Estrategia de asignación de puertos:</b>
 * <pre>
 * Dispositivo 0 → puerto base (default 4723)
 * Dispositivo 1 → base + 1 (4724)
 * Dispositivo 2 → base + 2 (4725)
 * ...
 * </pre>
 *
 * <p><b>Ciclo de vida:</b>
 * <ol>
 *   <li>{@link #initialize(boolean)} — escanea dispositivos y los registra (llamado por MobilePlugin)</li>
 *   <li>{@link #acquire(String)} — asigna un dispositivo a una ejecución (thread-safe via CAS)</li>
 *   <li>{@link #release(String)} — libera el dispositivo al finalizar la ejecución</li>
 * </ol>
 *
 * <p><b>Integración con el Backend:</b>
 * El BE puede llamar a {@link #getAvailableDevices()} para obtener los dispositivos libres
 * y presentarlos en el FE. Al lanzar la ejecución, incluir {@code mobile.device.id}
 * en el {@code ExecutionRequest.properties} con el ID seleccionado por el usuario.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class DevicePool {

    private static final DevicePool INSTANCE = new DevicePool();
    private static final int DEFAULT_APPIUM_BASE_PORT = 4723;

    /** deviceId → estado actual del dispositivo */
    private final ConcurrentHashMap<String, AtomicReference<DeviceStatus>> statusMap =
        new ConcurrentHashMap<>();

    /** deviceId → descriptor completo */
    private final ConcurrentHashMap<String, DeviceDescriptor> descriptorMap =
        new ConcurrentHashMap<>();

    /** deviceId → URL Appium con puerto asignado */
    private final ConcurrentHashMap<String, String> appiumUrlMap = new ConcurrentHashMap<>();

    private final AtomicInteger portCounter = new AtomicInteger(0);

    private DevicePool() {}

    public static DevicePool getInstance() {
        return INSTANCE;
    }

    // =========================================================================
    // Inicialización
    // =========================================================================

    /**
     * Inicializa el pool: escanea dispositivos disponibles y los registra.
     * Es idempotente — si el pool ya tiene dispositivos registrados no vuelve a escanear.
     *
     * @param autoScan si {@code true}, usa {@link DeviceDiscoveryService} para escanear;
     *                 si {@code false}, el pool queda vacío hasta que se registren manualmente
     */
    public synchronized void initialize(boolean autoScan) {
        if (!descriptorMap.isEmpty()) {
            TestLogger.logInfo("DEVICE_POOL", "Pool ya inicializado con "
                + descriptorMap.size() + " dispositivo(s)", null);
            return;
        }

        ConfigManager config = ConfigManager.getInstance();
        int basePort = config.getInt(MobileConfigKeys.APPIUM_BASE_PORT, DEFAULT_APPIUM_BASE_PORT);

        if (autoScan) {
            DeviceDiscoveryService scanner = new DeviceDiscoveryService();
            List<DeviceDescriptor> discovered = scanner.discoverAll();
            for (DeviceDescriptor d : discovered) {
                int port = basePort + portCounter.getAndIncrement();
                String appiumUrl = "http://localhost:" + port;
                register(rebuildWithUrl(d, appiumUrl));
            }
        }

        TestLogger.logInfo("DEVICE_POOL",
            "Pool inicializado: " + descriptorMap.size() + " dispositivo(s) registrado(s)", null);
    }

    /**
     * Registra manualmente un dispositivo en el pool.
     * Útil para dispositivos configurados explícitamente en propiedades.
     */
    public synchronized void register(DeviceDescriptor device) {
        String id = device.getId();
        descriptorMap.put(id, device);
        statusMap.put(id, new AtomicReference<>(DeviceStatus.AVAILABLE));
        appiumUrlMap.put(id, device.getAppiumServerUrl());
        TestLogger.logInfo("DEVICE_POOL",
            "Dispositivo registrado: " + id + " → " + device.getAppiumServerUrl(), null);
    }

    // =========================================================================
    // Adquisición y liberación
    // =========================================================================

    /**
     * Adquiere un dispositivo para su uso exclusivo en una ejecución.
     *
     * <p>Si {@code preferredId} es no-nulo, intenta reservar ese dispositivo específico.
     * Si {@code preferredId} es nulo, toma el primero disponible.
     *
     * @param preferredId ID lógico del dispositivo preferido (puede ser null)
     * @return descriptor del dispositivo adquirido
     * @throws IllegalStateException si el dispositivo está ocupado o no hay ninguno libre
     */
    public DeviceDescriptor acquire(String preferredId) {
        if (preferredId != null && !preferredId.isBlank()) {
            return acquireById(preferredId);
        }
        return acquireFirst();
    }

    /**
     * Libera el dispositivo, dejándolo disponible para nuevas ejecuciones.
     *
     * @param deviceId ID lógico del dispositivo a liberar
     */
    public void release(String deviceId) {
        if (deviceId == null) {
            return;
        }
        AtomicReference<DeviceStatus> ref = statusMap.get(deviceId);
        if (ref != null) {
            ref.set(DeviceStatus.AVAILABLE);
            TestLogger.logInfo("DEVICE_POOL", "Dispositivo liberado: " + deviceId, null);
        }
    }

    // =========================================================================
    // Consultas
    // =========================================================================

    /**
     * Retorna todos los dispositivos actualmente disponibles (no ocupados).
     * El Backend puede usar este método para presentar opciones en el FE.
     */
    public List<DeviceDescriptor> getAvailableDevices() {
        List<DeviceDescriptor> result = new ArrayList<>();
        for (String id : descriptorMap.keySet()) {
            AtomicReference<DeviceStatus> ref = statusMap.get(id);
            if (ref != null && ref.get() == DeviceStatus.AVAILABLE) {
                result.add(descriptorMap.get(id));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Retorna todos los dispositivos registrados (independientemente de su estado).
     */
    public List<DeviceDescriptor> getAllDevices() {
        return Collections.unmodifiableList(new ArrayList<>(descriptorMap.values()));
    }

    /**
     * Retorna el estado actual de un dispositivo.
     */
    public Optional<DeviceStatus> getStatus(String deviceId) {
        AtomicReference<DeviceStatus> ref = statusMap.get(deviceId);
        return ref != null ? Optional.of(ref.get()) : Optional.empty();
    }

    /**
     * @return true si el pool tiene al menos un dispositivo registrado
     */
    public boolean hasDevices() {
        return !descriptorMap.isEmpty();
    }

    /**
     * @return cantidad de dispositivos registrados
     */
    public int size() {
        return descriptorMap.size();
    }

    /**
     * Reinicia el pool (para tests o reinicialización en caliente).
     */
    public synchronized void reset() {
        statusMap.clear();
        descriptorMap.clear();
        appiumUrlMap.clear();
        portCounter.set(0);
        TestLogger.logInfo("DEVICE_POOL", "Pool reiniciado", null);
    }

    // =========================================================================
    // Privados
    // =========================================================================

    private DeviceDescriptor acquireById(String deviceId) {
        AtomicReference<DeviceStatus> ref = statusMap.get(deviceId);
        if (ref == null) {
            throw new IllegalStateException(
                "Dispositivo '" + deviceId + "' no registrado en el pool. " +
                "Dispositivos disponibles: " + descriptorMap.keySet());
        }
        if (!ref.compareAndSet(DeviceStatus.AVAILABLE, DeviceStatus.BUSY)) {
            DeviceStatus current = ref.get();
            throw new IllegalStateException(
                "Dispositivo '" + deviceId + "' no disponible (estado: " + current + "). " +
                "Otro thread/ejecucion lo esta usando o esta offline.");
        }
        TestLogger.logInfo("DEVICE_POOL", "Dispositivo adquirido: " + deviceId, null);
        return descriptorMap.get(deviceId);
    }

    private DeviceDescriptor acquireFirst() {
        for (String id : descriptorMap.keySet()) {
            AtomicReference<DeviceStatus> ref = statusMap.get(id);
            if (ref != null && ref.compareAndSet(DeviceStatus.AVAILABLE, DeviceStatus.BUSY)) {
                TestLogger.logInfo("DEVICE_POOL",
                    "Dispositivo asignado automaticamente: " + id, null);
                return descriptorMap.get(id);
            }
        }
        throw new IllegalStateException(
            "No hay dispositivos disponibles en el pool. " +
            "Todos estan ocupados o el pool esta vacio. " +
            "Conecta un dispositivo/emulador o verifica el escaneo automatico.");
    }

    /**
     * Reconstruye un DeviceDescriptor con la URL Appium del puerto asignado.
     */
    private DeviceDescriptor rebuildWithUrl(DeviceDescriptor original, String appiumUrl) {
        return DeviceDescriptor.builder(original.getId(), original.getType()).platformName(original.getPlatformName()).
            platformVersion(original.getPlatformVersion()).deviceName(original.getDeviceName()).
            udid(original.getUdid()).appiumServerUrl(appiumUrl).build();
    }

    /**
     * Construye el descriptor de un dispositivo configurado manualmente desde propiedades.
     * Se usa cuando {@code mobile.device.id} apunta a un dispositivo que no fue auto-descubierto.
     */
    public static DeviceDescriptor buildFromConfig(
            String deviceId, String typeStr, String platform, String version,
            String name, String udid, String appiumUrl) {

        DeviceType type;
        try {
            type = DeviceType.valueOf(typeStr.toUpperCase());
        } catch (Exception e) {
            // Inferir tipo desde plataforma si el typeStr es inválido
            type = platform.equalsIgnoreCase("iOS")
                ? DeviceType.IOS_SIMULATOR : DeviceType.ANDROID_EMULATOR;
        }

        return DeviceDescriptor.builder(deviceId, type).
            platformName(platform).platformVersion(version).deviceName(name).
            udid(udid).appiumServerUrl(appiumUrl).build();
    }
}
