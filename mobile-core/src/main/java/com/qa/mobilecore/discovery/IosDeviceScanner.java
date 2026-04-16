package com.qa.mobilecore.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.common.logging.TestLogger;
import com.qa.mobilecore.model.DeviceDescriptor;
import com.qa.mobilecore.model.DeviceType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Escanea dispositivos iOS disponibles vía {@code xcrun simctl}.
 *
 * <p>Solo funciona en <b>macOS</b> con Xcode instalado.
 * Detecta simuladores iOS en estado {@code Booted} (activos).
 *
 * <p>Para dispositivos físicos iOS, se requieren herramientas adicionales
 * ({@code cfgutil}, {@code libimobiledevice}) que están fuera del alcance de Fase 3.
 * Los dispositivos físicos iOS se deben configurar manualmente via {@code MobileConfigKeys.UDID}.
 *
 * <p><b>Comando ejecutado:</b> {@code xcrun simctl list devices --json}
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class IosDeviceScanner {

    private static final String SIMCTL_CMD = "xcrun simctl list devices --json";
    private static final int TIMEOUT_SEC = 10;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Escanea y retorna los simuladores iOS activos (estado Booted).
     *
     * @param includeSimulators incluir simuladores activos
     * @return lista de descriptores de dispositivos iOS detectados
     */
    public List<DeviceDescriptor> scan(boolean includeSimulators) {
        List<DeviceDescriptor> result = new ArrayList<>();

        if (!isMacOS()) {
            TestLogger.logInfo("IOS_SCANNER",
                "Escaneo iOS omitido (solo disponible en macOS)", null);
            return result;
        }

        if (!isXcrunAvailable()) {
            TestLogger.logWarning("IOS_SCANNER",
                "xcrun no encontrado. Instala Xcode para habilitar escaneo de simuladores iOS.", null);
            return result;
        }

        if (!includeSimulators) return result;

        String json = runSimctl();
        if (json == null || json.isBlank()) return result;

        result.addAll(parseSimctlJson(json));
        TestLogger.logInfo("IOS_SCANNER",
            "Escaneo simctl completado: " + result.size() + " simulador(es) activo(s)", null);
        return result;
    }

    /**
     * Verifica si xcrun está disponible (solo macOS con Xcode).
     */
    public boolean isXcrunAvailable() {
        if (!isMacOS()) return false;
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"xcrun", "--version"});
            return p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================================
    // Privados
    // =========================================================================

    private String runSimctl() {
        StringBuilder sb = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(
                new String[]{"sh", "-c", SIMCTL_CMD});

            if (!process.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                TestLogger.logWarning("IOS_SCANNER", "Timeout ejecutando simctl", null);
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
        } catch (Exception e) {
            TestLogger.logWarning("IOS_SCANNER", "Error ejecutando simctl: " + e.getMessage(), null);
            return null;
        }
        return sb.toString();
    }

    private List<DeviceDescriptor> parseSimctlJson(String json) {
        List<DeviceDescriptor> result = new ArrayList<>();
        try {
            JsonNode root    = MAPPER.readTree(json);
            JsonNode devices = root.path("devices");

            Iterator<Map.Entry<String, JsonNode>> runtimes = devices.properties().iterator();
            while (runtimes.hasNext()) {
                Map.Entry<String, JsonNode> runtimeEntry = runtimes.next();
                String runtimeKey = runtimeEntry.getKey(); // e.g. "com.apple.CoreSimulator.SimRuntime.iOS-17-0"

                if (!runtimeKey.contains("iOS")) continue;

                String iosVersion = extractIosVersion(runtimeKey);

                for (JsonNode sim : runtimeEntry.getValue()) {
                    String state = sim.path("state").asText("");
                    if (!"Booted".equalsIgnoreCase(state)) continue;

                    String udid = sim.path("udid").asText("");
                    String name = sim.path("name").asText("iOS Simulator");
                    String deviceId = "ios-sim-" + udid.substring(0, Math.min(8, udid.length())).toLowerCase();

                    DeviceDescriptor device = DeviceDescriptor.builder(deviceId, DeviceType.IOS_SIMULATOR)
                        .platformName("iOS")
                        .platformVersion(iosVersion)
                        .deviceName(name)
                        .udid(udid)
                        .build();

                    result.add(device);
                    TestLogger.logInfo("IOS_SCANNER",
                        "Simulador detectado: " + device, null);
                }
            }
        } catch (Exception e) {
            TestLogger.logWarning("IOS_SCANNER",
                "Error parseando JSON de simctl: " + e.getMessage(), null);
        }
        return result;
    }

    private String extractIosVersion(String runtimeKey) {
        // "com.apple.CoreSimulator.SimRuntime.iOS-17-0" → "17.0"
        try {
            String suffix = runtimeKey.substring(runtimeKey.lastIndexOf("iOS-") + 4);
            return suffix.replace("-", ".");
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isMacOS() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }
}
