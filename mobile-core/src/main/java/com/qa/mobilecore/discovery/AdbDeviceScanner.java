package com.qa.mobilecore.discovery;

import com.qa.common.api.config.ConfigLoaderHolder;
import com.qa.common.api.logging.TestLogger;
import com.qa.mobilecore.config.MobileConfigKeys;
import com.qa.mobilecore.model.DeviceDescriptor;
import com.qa.mobilecore.model.DeviceType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Escanea dispositivos Android disponibles vía ADB.
 *
 * <p>Ejecuta {@code adb devices -l} y parsea la salida para detectar:
 * <ul>
 *   <li>Dispositivos físicos conectados por USB o WiFi (serial alfanumérico o IP:puerto)</li>
 *   <li>Emuladores en ejecución (serial {@code emulator-XXXX})</li>
 * </ul>
 *
 * <p><b>Prerequisitos:</b> Android SDK instalado y {@code adb} en el PATH del sistema,
 * o bien la ruta al binario configurada en {@link com.qa.mobilecore.config.MobileConfigKeys#ADB_PATH}.
 *
 * <p><b>Formato de salida de ADB:</b>
 * <pre>
 * List of devices attached
 * emulator-5554  device  product:sdk_gphone64_x86_64  model:sdk_gphone64_x86_64
 * R38N801XXXX    device  product:SM-G991B  model:SM-G991B  device:o1s
 * 192.168.1.100:5555  device
 * offline-device  offline
 * </pre>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class AdbDeviceScanner {

    private static final int TIMEOUT_SEC = 10;

    /**
     * Escanea y retorna todos los dispositivos Android activos (excluye offline).
     *
     * @param includeVirtual  incluir emuladores
     * @param includePhysical incluir dispositivos físicos
     * @return lista de descriptores de dispositivos Android detectados
     */
    public List<DeviceDescriptor> scan(boolean includeVirtual, boolean includePhysical) {
        List<DeviceDescriptor> result = new ArrayList<>();

        if (!isAdbAvailable()) {
            TestLogger.logWarning("ADB_SCANNER",
                "ADB no encontrado en PATH. Escaneo Android omitido. " +
                "Instala Android SDK y asegura que 'adb' este en el PATH.", null);
            return result;
        }

        List<String> rawLines = runAdbDevices();
        for (String line : rawLines) {
            DeviceDescriptor device = parseLine(line, includeVirtual, includePhysical);
            if (device != null) {
                result.add(device);
                TestLogger.logInfo("ADB_SCANNER",
                    "Dispositivo detectado: " + device, null);
            }
        }

        TestLogger.logInfo("ADB_SCANNER",
            "Escaneo ADB completado: " + result.size() + " dispositivo(s) activo(s)", null);
        return result;
    }

    /**
     * Verifica si ADB está disponible en el sistema.
     *
     * <p>El binario se resuelve vía {@link MobileConfigKeys#ADB_PATH}; si no está
     * configurado se usa la resolución por {@code $PATH}. Una ruta configurada pero
     * no ejecutable se reporta como warning y el escaneo se omite (skip elegante).
     */
    public boolean isAdbAvailable() {
        try {
            String adb = resolveBinary(MobileConfigKeys.ADB_PATH, "adb");
            Process p = Runtime.getRuntime().exec(new String[]{adb, "version"});
            return p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (IllegalStateException e) {
            TestLogger.logWarning("ADB_SCANNER", e.getMessage(), null);
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================================
    // Privados
    // =========================================================================

    private List<String> runAdbDevices() {
        List<String> lines = new ArrayList<>();
        try {
            String adb = resolveBinary(MobileConfigKeys.ADB_PATH, "adb");
            // Direct exec (no shell): Runtime.exec resolves a bare "adb" via $PATH, and a
            // configured absolute path is used verbatim (handles paths with spaces safely).
            Process process = Runtime.getRuntime().exec(new String[]{adb, "devices", "-l"});

            if (!process.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                TestLogger.logWarning("ADB_SCANNER", "Timeout ejecutando 'adb devices'", null);
                return lines;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line.trim());
                }
            }
        } catch (Exception e) {
            TestLogger.logWarning("ADB_SCANNER", "Error ejecutando ADB: " + e.getMessage(), null);
        }
        return lines;
    }

    private DeviceDescriptor parseLine(String line, boolean includeVirtual, boolean includePhysical) {
        // Ignorar encabezado y líneas vacías
        if (line.isEmpty() || line.startsWith("List of devices") || line.startsWith("*")) {
            return null;
        }

        // Formato: "<serial>\t<state>\t[propiedades...]"
        String[] parts = line.split("\\s+");
        if (parts.length < 2) {
            return null;
        }

        String serial = parts[0];
        String state  = parts[1];

        // Solo procesar dispositivos activos
        if (!"device".equals(state)) {
            return null;
        }

        boolean isEmulator = serial.startsWith("emulator-");
        DeviceType type = isEmulator ? DeviceType.ANDROID_EMULATOR : DeviceType.ANDROID_PHYSICAL;

        if (isEmulator && !includeVirtual) {
            return null;
        }
        if (!isEmulator && !includePhysical) {
            return null;
        }

        // Extraer modelo si está disponible en las propiedades extendidas
        String model = extractProperty(line, "model");
        String deviceId = isEmulator ? serial : "android-" + serial.replaceAll("[^a-zA-Z0-9]", "-");

        return DeviceDescriptor.builder(deviceId, type).udid(serial).deviceName(model != null ? model : serial).
            platformName("Android").build();
    }

    private String extractProperty(String line, String key) {
        // Busca "key:value" en la línea (formato de 'adb devices -l')
        String prefix = key + ":";
        int idx = line.indexOf(prefix);
        if (idx < 0) {
            return null;
        }
        int start = idx + prefix.length();
        int end = line.indexOf(' ', start);
        return end < 0 ? line.substring(start) : line.substring(start, end);
    }

    /**
     * Resuelve la ruta del binario externo a partir de una clave de configuración.
     *
     * <p>Si {@code configKey} está definido con un valor no vacío, debe apuntar a un
     * archivo ejecutable o se lanza {@link IllegalStateException} (fail-fast ante
     * mala configuración). Si está ausente/vacío, retorna {@code defaultName} para
     * que aplique la resolución por {@code $PATH} (comportamiento retrocompatible).
     *
     * @param configKey   clave de config (p.ej. {@link MobileConfigKeys#ADB_PATH})
     * @param defaultName nombre del binario para el fallback por {@code $PATH}
     * @return ruta absoluta configurada, o {@code defaultName} para lookup por {@code $PATH}
     * @throws IllegalStateException si la clave está definida pero la ruta no es ejecutable
     */
    static String resolveBinary(String configKey, String defaultName) {
        String configured = ConfigLoaderHolder.get().getRaw(configKey).orElse(null);
        if (configured != null && !configured.isBlank()) {
            if (!Files.isExecutable(Paths.get(configured))) {
                throw new IllegalStateException(
                    configKey + " set but not executable: " + configured);
            }
            return configured;
        }
        return defaultName;
    }
}
