package com.qa.common.internal.config;

import com.qa.common.api.Internal;
import com.qa.common.api.config.ConfigLoader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Recolector del scan dinámico {@code extent.systemInfo.*} (TASK-K03M-M9).
 *
 * <p>El subsistema Extent permite enriquecer el reporte con metadata de sistema
 * usando claves de prefijo arbitrario: {@code extent.systemInfo.Tester=Abel},
 * {@code extent.systemInfo.Build=42}, {@code EXTENT_SYSTEMINFO_FECHA=auto}, etc.
 * Esto NO encaja en un record {@code TypedConfig} (cantidad/nombre de keys
 * desconocidos en compile-time), por lo que se resuelve con este helper aparte.
 *
 * <p>Sources cubiertos:
 * <ul>
 *   <li>{@code System.getProperties()} con prefijo {@code extent.systemInfo.}</li>
 *   <li>{@code System.getenv()} con prefijo {@code EXTENT_SYSTEMINFO_} (capitalizado)</li>
 *   <li>{@code extent.systemInfo.Fecha} con valor {@code "auto"} o {@code ${...}} → timestamp actual</li>
 * </ul>
 */
@Internal(reason = "internal — usado por ReportingConfig.load()")
public final class ExtentSystemInfoLoader {

    private static final String SYSPROP_PREFIX = "extent.systemInfo.";
    private static final String ENV_PREFIX = "EXTENT_SYSTEMINFO_";
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private ExtentSystemInfoLoader() {}

    public static Map<String, String> load(ConfigLoader loader) {
        Map<String, String> out = new LinkedHashMap<>();

        System.getProperties().forEach((key, value) -> {
            String keyStr = key.toString();
            if (keyStr.startsWith(SYSPROP_PREFIX)) {
                out.put(keyStr.substring(SYSPROP_PREFIX.length()), value.toString());
            }
        });

        System.getenv().forEach((key, value) -> {
            if (key.startsWith(ENV_PREFIX)) {
                String infoKey = capitalizeWords(
                        key.substring(ENV_PREFIX.length()).replace("_", " ").toLowerCase());
                out.put(infoKey, value);
            }
        });

        String fecha = loader.getRaw(SYSPROP_PREFIX + "Fecha").orElse(null);
        if (fecha != null && (fecha.contains("${") || fecha.equalsIgnoreCase("auto"))) {
            out.put("Fecha", LocalDateTime.now().format(FECHA_FMT));
        }

        return out;
    }

    private static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return result.toString().trim();
    }
}
