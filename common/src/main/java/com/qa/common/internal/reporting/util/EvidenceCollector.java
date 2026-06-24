package com.qa.common.internal.reporting.util;
import com.qa.common.api.Internal;

import com.qa.common.internal.reporting.model.Attachment;
import com.qa.common.api.logging.TestLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Recolecta evidencias capturadas durante los tests y las convierte en
 * Attachments para el sistema de Reporting (Extent Reports).
 *
 * FLUJO:
 * 1. screenshot / API response guardado en test-evidences/
 * 2. EvidenceCollector.collectScreenshots() → convierte a List&lt;Attachment&gt;
 * 3. ScenarioResult.setScreenshots() → adjunta al resultado
 * 4. CucumberResultAdapter proyecta a AttachmentData y ExtentReportGeneratorImpl
 *    los renderiza en el reporte HTML
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
@Internal

public class EvidenceCollector {

    /** Tamaño máximo de archivo (5 MB) para embeber contenido en los reportes. */
    private static final long MAX_EMBED_SIZE_BYTES = 5L * 1024 * 1024;

    /** Milisegundos en un día — usado para calcular la fecha de corte de limpieza. */
    private static final long MS_PER_DAY = 24L * 60 * 60 * 1000;

    private EvidenceCollector() {
        // Utility class
    }

    /**
     * Convierte un path de evidencia a Attachment.
     *
     * @param evidencePath path devuelto por EvidenceManager.saveXxx()
     * @param type tipo de attachment
     * @return Attachment listo para reporting
     */
    public static Attachment toAttachment(String evidencePath, Attachment.AttachmentType type) {
        if (evidencePath == null || evidencePath.isEmpty()) {
            TestLogger.logWarning("EVIDENCE_COLLECTOR",
                "⚠️ Evidence path vacío, retornando null", null);
            return null;
        }

        File file = new File(evidencePath);
        if (!file.exists()) {
            TestLogger.logWarning("EVIDENCE_COLLECTOR",
                "⚠️ Archivo de evidencia no encontrado: " + evidencePath, null);
            return null;
        }

        Attachment attachment = new Attachment();
        attachment.setName(file.getName());
        attachment.setPath(evidencePath);
        attachment.setType(type);
        attachment.setSizeBytes(file.length());
        attachment.setMimeType(type.getDefaultMimeType());

        // Cargar contenido si es pequeño (< MAX_EMBED_SIZE_BYTES para embeber en reportes)
        if (file.length() < MAX_EMBED_SIZE_BYTES) {
            try {
                byte[] content = Files.readAllBytes(file.toPath());
                attachment.setContent(content);
            } catch (IOException e) {
                TestLogger.logWarning("EVIDENCE_COLLECTOR",
                    "⚠️ No se pudo leer contenido de: " + evidencePath, null);
            }
        }

        TestLogger.logDebug("EVIDENCE_COLLECTOR",
            String.format("✓ Attachment creado: %s (%d bytes)", file.getName(), file.length()), null);

        return attachment;
    }

    /**
     * Convierte screenshot guardado a Attachment.
     *
     * @param screenshotPath path al screenshot (.png)
     * @return Attachment de tipo SCREENSHOT
     */
    public static Attachment screenshotToAttachment(String screenshotPath) {
        return toAttachment(screenshotPath, Attachment.AttachmentType.SCREENSHOT);
    }

    /**
     * Convierte respuesta API guardada a Attachment.
     *
     * @param apiResponsePath path al archivo de respuesta JSON
     * @return Attachment de tipo JSON
     */
    public static Attachment apiResponseToAttachment(String apiResponsePath) {
        return toAttachment(apiResponsePath, Attachment.AttachmentType.JSON);
    }

    /**
     * Convierte archivo de error guardado a Attachment.
     *
     * @param errorPath path al archivo de error JSON
     * @return Attachment de tipo JSON
     */
    public static Attachment errorToAttachment(String errorPath) {
        return toAttachment(errorPath, Attachment.AttachmentType.JSON);
    }

    /**
     * Recolecta todos los screenshots de un directorio (test-evidences de un scenario).
     *
     * @param scenarioEvidenceDir directorio de evidencias del scenario
     * @return lista de Attachments con todos los screenshots encontrados
     */
    public static List<Attachment> collectScreenshotsFromDirectory(String scenarioEvidenceDir) {
        List<Attachment> screenshots = new ArrayList<>();

        File dir = new File(scenarioEvidenceDir);
        if (!dir.exists() || !dir.isDirectory()) {
            TestLogger.logWarning("EVIDENCE_COLLECTOR",
                "⚠️ Directorio de evidencias no existe: " + scenarioEvidenceDir, null);
            return screenshots;
        }

        File[] files = dir.listFiles((d, name) -> name.startsWith("screenshot_") && name.endsWith(".png"));
        if (files == null || files.length == 0) {
            TestLogger.logDebug("EVIDENCE_COLLECTOR",
                "No se encontraron screenshots en: " + scenarioEvidenceDir, null);
            return screenshots;
        }

        TestLogger.logInfo("EVIDENCE_COLLECTOR",
            String.format("📸 Recolectando %d screenshots de: %s", files.length, scenarioEvidenceDir), null);

        for (File file : files) {
            Attachment attachment = screenshotToAttachment(file.getAbsolutePath());
            if (attachment != null) {
                screenshots.add(attachment);
            }
        }

        return screenshots;
    }

    /**
     * Recolecta todos los archivos de evidencia de un tipo específico.
     *
     * @param scenarioEvidenceDir directorio de evidencias del scenario
     * @param prefix prefijo del archivo (ej: "api_response_", "error_")
     * @param type tipo de attachment
     * @return lista de Attachments encontrados
     */
    public static List<Attachment> collectEvidencesByPrefix(String scenarioEvidenceDir,
                                                            String prefix,
                                                            Attachment.AttachmentType type) {
        List<Attachment> evidences = new ArrayList<>();

        File dir = new File(scenarioEvidenceDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return evidences;
        }

        File[] files = dir.listFiles((d, name) -> name.startsWith(prefix));
        if (files == null || files.length == 0) {
            return evidences;
        }

        for (File file : files) {
            Attachment attachment = toAttachment(file.getAbsolutePath(), type);
            if (attachment != null) {
                evidences.add(attachment);
            }
        }

        TestLogger.logDebug("EVIDENCE_COLLECTOR",
            String.format("📎 Recolectados %d archivos con prefijo '%s'", evidences.size(), prefix), null);

        return evidences;
    }

    /**
     * Recolecta TODAS las evidencias de un scenario (screenshots, API, errors, etc.).
     *
     * @param scenarioEvidenceDir directorio de evidencias del scenario
     * @return lista completa de Attachments
     */
    public static List<Attachment> collectAllEvidences(String scenarioEvidenceDir) {
        List<Attachment> allEvidences = new ArrayList<>();

        // Screenshots
        allEvidences.addAll(collectScreenshotsFromDirectory(scenarioEvidenceDir));

        // API responses
        allEvidences.addAll(collectEvidencesByPrefix(scenarioEvidenceDir, "api_response_",
            Attachment.AttachmentType.JSON));

        // Errors
        allEvidences.addAll(collectEvidencesByPrefix(scenarioEvidenceDir, "error_",
            Attachment.AttachmentType.JSON));

        // UI interactions
        allEvidences.addAll(collectEvidencesByPrefix(scenarioEvidenceDir, "ui_interaction_",
            Attachment.AttachmentType.JSON));

        TestLogger.logInfo("EVIDENCE_COLLECTOR",
            String.format("📦 Total evidencias recolectadas: %d", allEvidences.size()), null);

        return allEvidences;
    }

    /**
     * Obtiene el directorio de evidencias para el test actual según EvidenceManager.
     *
     * Asume estructura: test-evidences/{framework}/{feature}/{scenario}/
     *
     * @param framework framework (WEB, API, MOBILE)
     * @param feature nombre del feature
     * @param scenario nombre del scenario
     * @return path al directorio de evidencias
     */
    public static String getScenarioEvidenceDirectory(String framework, String feature, String scenario) {
        // Sanitizar nombres para filesystem
        String sanitizedFeature = sanitizeForFilesystem(feature);
        String sanitizedScenario = sanitizeForFilesystem(scenario);

        return String.format("test-evidences/%s/%s/%s",
            framework.toLowerCase(),
            sanitizedFeature,
            sanitizedScenario);
    }

    /**
     * Sanitiza string para usar como nombre de archivo/directorio.
     */
    private static String sanitizeForFilesystem(String input) {
        if (input == null) {
            return "unknown";
        }
        return input.replaceAll("[^a-zA-Z0-9-_]", "_").replaceAll("_{2,}", "_").toLowerCase();
    }

    /**
     * Limpia evidencias antiguas (opcional, para no llenar disco).
     *
     * @param daysToKeep días a mantener evidencias
     */
    public static void cleanOldEvidences(int daysToKeep) {
        Path evidenceBase = Paths.get("test-evidences");

        if (!Files.exists(evidenceBase)) {
            return;
        }

        long cutoffTime = System.currentTimeMillis() - (daysToKeep * MS_PER_DAY);

        try {
            Files.walk(evidenceBase).filter(Files::isRegularFile).filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                }).forEach(path -> {
                    try {
                        Files.delete(path);
                        TestLogger.logDebug("EVIDENCE_COLLECTOR",
                            "🗑️ Evidencia antigua eliminada: " + path, null);
                    } catch (IOException e) {
                        TestLogger.logWarning("EVIDENCE_COLLECTOR",
                            "⚠️ No se pudo eliminar: " + path, null);
                    }
                });

            TestLogger.logInfo("EVIDENCE_COLLECTOR",
                String.format("✓ Limpieza de evidencias completada (> %d días)", daysToKeep), null);

        } catch (IOException e) {
            TestLogger.logWarning("EVIDENCE_COLLECTOR",
                "⚠️ Error en limpieza de evidencias: " + e.getMessage(), null);
        }
    }
}

