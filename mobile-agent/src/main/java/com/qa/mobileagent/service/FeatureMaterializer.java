package com.qa.mobileagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Escribe los contenidos de archivos {@code .feature} recibidos por la red en
 * un workspace local del agente, devolviendo las rutas absolutas que
 * {@code InProcessTransport} puede consumir.
 *
 * <p>Cada ejecución usa un subdirectorio único bajo {@code workspaceRoot}.
 * El directorio se borra recursivamente vía {@link #cleanup(Path)} cuando la
 * ejecución termina.
 *
 * @since TASK-I04
 */
public class FeatureMaterializer {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureMaterializer.class);

    private final Path workspaceRoot;

    public FeatureMaterializer(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    /**
     * Materializa los contenidos en {@code workspaceRoot/&#123;executionId&#125;}.
     *
     * <p>Sanitiza nombres de fichero para impedir path traversal: rechaza
     * cualquier filename que contenga {@code ..} o un separador absoluto.
     */
    public List<String> materialize(String executionId, Map<String, String> contents) throws IOException {
        Path execDir = workspaceRoot.resolve(executionId);
        Files.createDirectories(execDir);
        List<String> paths = new ArrayList<>(contents.size());
        for (Map.Entry<String, String> e : contents.entrySet()) {
            String filename = sanitize(e.getKey());
            Path file = execDir.resolve(filename);
            if (!file.normalize().startsWith(execDir.normalize())) {
                throw new IllegalArgumentException("path traversal rechazado: " + e.getKey());
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, e.getValue() == null ? "" : e.getValue(), StandardCharsets.UTF_8);
            paths.add(file.toAbsolutePath().toString());
        }
        return paths;
    }

    public void cleanup(Path execDir) {
        if (execDir == null || !Files.exists(execDir)) { return; }
        try (var stream = Files.walk(execDir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) { /* best-effort */ }
            });
        } catch (IOException e) {
            LOG.warn("No pude limpiar workspace {}: {}", execDir, e.getMessage());
        }
    }

    public Path execDir(String executionId) {
        return workspaceRoot.resolve(executionId);
    }

    private static String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename vacío");
        }
        if (filename.contains("..") || filename.startsWith("/") || filename.startsWith("\\")) {
            throw new IllegalArgumentException("filename inseguro: " + filename);
        }
        return filename;
    }
}
