package com.qa.common.internal.config.source;

import com.qa.common.api.Internal;
import com.qa.common.api.logging.TestLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

/**
 * Source de prioridad intermedia (entre {@link EnvVarSource} y {@link YamlFileSource}):
 * lee {@code .properties} desde el classpath con fallback en cascada.
 *
 * <p>Carga el primer archivo disponible:
 * <ol>
 *   <li>{@code config-{env}.properties}</li>
 *   <li>{@code config-app.properties}</li>
 *   <li>{@code config.properties}</li>
 * </ol>
 *
 * <p>Resolución de {@code env}:
 * <ol>
 *   <li>System property {@code env}</li>
 *   <li>Env var {@code TEST_ENV}</li>
 *   <li>Default {@code "dev"} (alineado con {@code FrameworkConfig.defaults().environment()})</li>
 * </ol>
 *
 * <p>Las claves se devuelven tal cual están en el archivo (formato libre — el
 * parser superior decide cómo interpretarlas). No hay interpolación de
 * {@code ${VAR}} aquí; eso vive en F3 ({@code VariableInterpolator}).
 *
 * @since TASK-K03M-F2
 */
@Internal(reason = "internal — usar com.qa.common.api.config.ConfigLoader para acceder")
public final class PropertiesFileSource implements ConfigSource {

    private static final TestLogger.LoggerWrapper LOG = TestLogger.getLogger(PropertiesFileSource.class);

    private final Properties properties;
    private final String loadedFrom;

    /** Producción: classloader real + env resuelto desde system/env. */
    public PropertiesFileSource() {
        this(resolveEnvironment(),
                PropertiesFileSource.class.getClassLoader()::getResourceAsStream);
    }

    /**
     * Constructor inyectable para tests. {@code resourceLoader} simula el
     * classpath: dado un nombre de recurso devuelve un {@link InputStream}
     * con su contenido o {@code null} si "no existe". Público porque otros
     * tests fuera del package lo necesitan; la clase entera es {@code @Internal}.
     */
    public PropertiesFileSource(String env, Function<String, InputStream> resourceLoader) {
        Loaded loaded = loadFirstAvailable(env, resourceLoader);
        this.properties = loaded.properties();
        this.loadedFrom = loaded.source();
    }

    @Override
    public Optional<String> get(String key) {
        if (key == null || key.isBlank() || properties.isEmpty()) return Optional.empty();
        String v = properties.getProperty(key);
        return (v == null) ? Optional.empty() : Optional.of(v);
    }

    @Override
    public String name() {
        return "PropertiesFile(" + loadedFrom + ")";
    }

    static String resolveEnvironment() {
        String env = System.getProperty("env");
        if (env == null || env.isBlank()) env = System.getenv("TEST_ENV");
        if (env == null || env.isBlank()) env = "dev";
        return env.toLowerCase().trim();
    }

    private static Loaded loadFirstAvailable(String env, Function<String, InputStream> loader) {
        String[] candidates = {
                "config-" + env + ".properties",
                "config-app.properties",
                "config.properties"
        };
        for (String name : candidates) {
            Properties p = tryLoad(name, loader);
            if (p != null) {
                LOG.info("PropertiesFileSource: cargado " + name);
                return new Loaded(p, name);
            }
        }
        return new Loaded(new Properties(), "<none>");
    }

    private static Properties tryLoad(String name, Function<String, InputStream> loader) {
        try (InputStream is = loader.apply(name)) {
            if (is == null) return null;
            Properties p = new Properties();
            p.load(is);
            return p;
        } catch (IOException e) {
            LOG.warn("PropertiesFileSource: error leyendo " + name + ": " + e.getMessage());
            return null;
        }
    }

    private record Loaded(Properties properties, String source) {}
}
