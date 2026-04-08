package com.qa.common.reporting.core.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extractor de test keys desde tags de Cucumber.
 *
 * Busca tags en formato: @PROJECTKEY-123, @QAAUY-456, etc.
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class TagExtractor {

    // Patrón para Jira test keys: @PROJECTKEY-NUMBER
    private static final Pattern TEST_KEY_PATTERN = Pattern.compile("@([A-Z]{2,10}-\\d+)");

    private TagExtractor() {
        // Utility class
    }

    /**
     * Extrae el test key desde una lista de tags.
     *
     * @param tags lista de tags de Cucumber (ej: [@smoke, @QAAUY-123, @test])
     * @return test key (ej: "QAAUY-123") o null si no se encuentra
     */
    public static String extractTestKey(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }

        for (String tag : tags) {
            Matcher matcher = TEST_KEY_PATTERN.matcher(tag);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    /**
     * Extrae el project key del test key.
     *
     * @param testKey test key completo (ej: "QAAUY-123")
     * @return project key (ej: "QAAUY") o null
     */
    public static String extractProjectKey(String testKey) {
        if (testKey == null || !testKey.contains("-")) {
            return null;
        }

        return testKey.substring(0, testKey.indexOf("-"));
    }

    /**
     * Valida si un string es un test key válido.
     *
     * @param testKey string a validar
     * @return true si es válido (formato PROJECTKEY-NUMBER)
     */
    public static boolean isValidTestKey(String testKey) {
        if (testKey == null) {
            return false;
        }

        return TEST_KEY_PATTERN.matcher("@" + testKey).find();
    }
}

