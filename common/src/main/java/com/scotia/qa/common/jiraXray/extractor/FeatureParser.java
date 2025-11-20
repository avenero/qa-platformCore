package com.scotia.qa.common.jiraXray.extractor;

import com.scotia.qa.common.jiraXray.model.ScenarioInfo;
import com.scotia.qa.common.logging.TestLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Parser de archivos .feature para extraer scenarios con tags específicos.
 * Basado en la lógica de UpdateTestJira pero desacoplado y mejorado.
 * Integrado con el sistema de logging del framework Scotia QA.
 *
 * @author Scotia QA Framework Team
 * @since 1.0.0
 */
public class FeatureParser {

    /**
     * Busca scenarios en archivos .feature que contengan el tag especificado y el código de proyecto
     *
     * @param tagCode código del tag a buscar (ej: "@smoke")
     * @param projectCode código del proyecto Jira (ej: "QAAUY")
     * @return lista de scenarios encontrados
     */
    public List<ScenarioInfo> findScenariosWithTag(String tagCode, String projectCode) {
        Path featuresDir = Paths.get("src/test/resources/features");
        List<ScenarioInfo> scenarios = new ArrayList<>();

        if (!Files.exists(featuresDir)) {
            TestLogger.logWarning("FEATURE_PARSER", "⚠️ Directorio de features no existe: " + featuresDir, null);
            return scenarios;
        }

        try {
            Files.walk(featuresDir)
                    .filter(path -> path.toString().endsWith(".feature"))
                    .forEach(path -> {
                        try {
                            List<ScenarioInfo> fileScenarios = parseFeatureFile(path, tagCode, projectCode);
                            scenarios.addAll(fileScenarios);
                        } catch (IOException e) {
                            TestLogger.logException("FEATURE_PARSER", "❌ Error procesando archivo: " + path, e);
                        }
                    });

        } catch (IOException e) {
            TestLogger.logException("FEATURE_PARSER", "❌ Error recorriendo directorio de features", e);
        }

        return scenarios;
    }

    /**
     * Parsea un archivo .feature individual y extrae scenarios con los tags especificados
     */
    private List<ScenarioInfo> parseFeatureFile(Path filePath, String tagCode, String projectCode) throws IOException {
        List<ScenarioInfo> scenarios = new ArrayList<>();

        byte[] bytes = Files.readAllBytes(filePath);
        String content = new String(bytes, StandardCharsets.UTF_8);
        String[] lines = content.split("\n");

        // Verificar si el archivo contiene el tagCode
        boolean containsTagCode = Arrays.stream(lines)
                .anyMatch(line -> line.contains(tagCode));

        if (!containsTagCode) {
            return scenarios;
        }

        // Procesar línea por línea buscando tags con el proyecto especificado
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.startsWith("@") && line.contains(projectCode)) {
                String jiraKey = extractJiraKey(line, projectCode);

                if (jiraKey != null) {
                    // Buscar el scenario correspondiente
                    int scenarioLineIndex = findNextScenarioLine(lines, i);

                    if (scenarioLineIndex != -1) {
                        String scenarioName = lines[scenarioLineIndex].trim();
                        String scenarioContent = extractScenarioContent(lines, scenarioLineIndex);

                        ScenarioInfo scenario = new ScenarioInfo(
                            filePath.getFileName().toString(),
                            scenarioName,
                            jiraKey,
                            scenarioContent
                        );

                        scenarios.add(scenario);

                        TestLogger.logInfo("FEATURE_PARSER", "✅ Scenario encontrado: " + jiraKey + " -> " + scenarioName, null);
                    }
                }
            }
        }

        return scenarios;
    }

    /**
     * Extrae la clave de Jira del tag (ej: "@QAAUY-123" -> "QAAUY-123")
     */
    private String extractJiraKey(String tagLine, String projectCode) {
        String[] tags = tagLine.split("\\s+");

        for (String tag : tags) {
            if (tag.startsWith("@" + projectCode)) {
                return tag.replace("@", "");
            }
        }

        return null;
    }

    /**
     * Busca la siguiente línea que contiene "Scenario"
     */
    private int findNextScenarioLine(String[] lines, int startIndex) {
        for (int i = startIndex + 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("Scenario")) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Extrae el contenido completo del scenario (pasos Given/When/Then)
     */
    private String extractScenarioContent(String[] lines, int scenarioLineIndex) {
        List<String> scenarioLines = new ArrayList<>();

        // Empezar desde la línea siguiente al "Scenario:"
        for (int i = scenarioLineIndex + 1; i < lines.length; i++) {
            String line = lines[i].trim();

            // Parar si encontramos otro tag o scenario
            if (line.startsWith("@") || line.startsWith("Scenario")) {
                break;
            }

            // Agregar líneas no vacías y que no sean comentarios
            if (!line.isEmpty() && !line.startsWith("#")) {
                scenarioLines.add(line);
            }
        }

        // Formatear para Jira (escapar comillas y agregar \n)
        StringBuilder formatted = new StringBuilder();
        for (String line : scenarioLines) {
            formatted.append(line.replace("\"", "\\\"")).append("\\n");
        }

        return formatted.toString();
    }
}
