package com.scotia.qa.common.jiraXray.extractor;

import com.scotia.qa.common.logging.TestLogger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrae metadata (testKey, projectKey) desde tags de tests.
 * Ejemplo: @QAAUY-582 → testKey="QAAUY-582", projectKey="QAAUY"
 * Integrado con el sistema de logging del framework Scotia QA.
 *
 * @author Abel Venero
 * @since 1.0.0
 */
public class TagBasedExtractor {

    // Patrón para testKey: @QAAUY-582, @TEST-123, etc.
    // Formato: @[LETRAS MAYÚSCULAS]-[NÚMEROS]
    private static final Pattern TEST_KEY_PATTERN = Pattern.compile("@([A-Z]+-\\d+)");

    /**
     * Extrae el testKey desde una lista de tags.
     * Busca el primer tag que coincida con el patrón @PROJECT-123
     *
     * @param tags lista de nombres de tags (ej: ["@test", "@QAAUY-582", "@smoke"])
     * @return testKey o null si no se encuentra
     */
    public String extractTestKey(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }

        for (String tag : tags) {
            if (tag == null) continue;

            Matcher matcher = TEST_KEY_PATTERN.matcher(tag);
            if (matcher.matches()) {
                String testKey = matcher.group(1);
                TestLogger.logInfo("TAG_EXTRACTOR", "🏷️ TestKey extraído desde tag: " + tag + " -> " + testKey, null);
                return testKey;
            }
        }

        return null;
    }

    /**
     * Extrae el projectKey desde el testKey.
     * Ejemplo: QAAUY-582 → QAAUY
     *
     * @param testKey test key en formato PROJECT-123
     * @return projectKey o null si el formato es inválido
     */
    public String extractProjectKeyFromTestKey(String testKey) {
        if (testKey == null || !testKey.contains("-")) {
            return null;
        }

        String projectKey = testKey.split("-")[0];
        TestLogger.logInfo("TAG_EXTRACTOR", "📂 ProjectKey extraído desde testKey: " + testKey + " -> " + projectKey, null);
        return projectKey;
    }
}

