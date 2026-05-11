package com.qa.common.internal.config.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.qa.common.api.Internal;
import com.qa.common.api.logging.TestLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Source de prioridad 3: lee archivos YAML (default {@code application.yml}
 * en el classpath) y resuelve claves canónicas camelCase navegando estructura
 * anidada.
 *
 * <p>Ejemplo de YAML:
 * <pre>
 * http:
 *   engine: APACHE
 *   connectTimeout: PT5S
 * </pre>
 *
 * <p>Para la key {@code "http.connectTimeout"} retorna {@code "PT5S"}.
 *
 * <p>Si el archivo no existe o no es legible, la fuente queda vacía (no falla).
 *
 * @since TASK-K03
 */
@Internal(reason = "internal — usar com.qa.common.api.config.ConfigLoader para acceder")
public final class YamlFileSource implements ConfigSource {

    private static final TestLogger.LoggerWrapper LOG = TestLogger.getLogger(YamlFileSource.class);

    private final Map<String, Object> root;

    /**
     * Carga {@code application.yml} buscándolo en (en este orden):
     * <ol>
     *   <li>{@code config-file} system property — path absoluto.</li>
     *   <li>{@code CONFIG_FILE} env var — path absoluto.</li>
     *   <li>Classpath: {@code application.yml}, {@code application.yaml}.</li>
     * </ol>
     */
    public YamlFileSource() {
        this(detectAndLoad());
    }

    /** Constructor para tests con root map inyectable. */
    public YamlFileSource(Map<String, Object> root) {
        this.root = root == null ? Collections.emptyMap() : root;
    }

    @Override
    public Optional<String> get(String key) {
        if (key == null || key.isBlank() || root.isEmpty()) return Optional.empty();
        Object node = root;
        for (String part : key.split("\\.")) {
            if (!(node instanceof Map<?, ?> m)) return Optional.empty();
            node = m.get(part);
            if (node == null) return Optional.empty();
        }
        return Optional.of(String.valueOf(node));
    }

    @Override
    public String name() {
        return "YamlFile";
    }

    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> detectAndLoad() {
        String explicit = System.getProperty("config-file");
        if (explicit == null) explicit = System.getenv("CONFIG_FILE");
        if (explicit != null && !explicit.isBlank()) {
            return safeLoadFile(Path.of(explicit));
        }
        for (String classpathName : new String[]{"application.yml", "application.yaml"}) {
            try (InputStream is = YamlFileSource.class.getClassLoader()
                    .getResourceAsStream(classpathName)) {
                if (is != null) {
                    ObjectMapper m = new ObjectMapper(new YAMLFactory());
                    Map<String, Object> root = m.readValue(is, LinkedHashMap.class);
                    return root != null ? root : Collections.emptyMap();
                }
            } catch (IOException e) {
                LOG.warn("YamlFileSource: error leyendo '" + classpathName + "': " + e.getMessage());
            }
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> safeLoadFile(Path path) {
        if (!Files.exists(path)) {
            LOG.warn("YamlFileSource: archivo no existe: " + path);
            return Collections.emptyMap();
        }
        try (InputStream is = Files.newInputStream(path)) {
            ObjectMapper m = new ObjectMapper(new YAMLFactory());
            Map<String, Object> root = m.readValue(is, LinkedHashMap.class);
            return root != null ? root : new HashMap<>();
        } catch (IOException e) {
            LOG.warn("YamlFileSource: error leyendo '" + path + "': " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}
