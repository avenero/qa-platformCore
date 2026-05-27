package com.qa.common.internal.config;

import com.qa.common.api.Internal;
import com.qa.common.api.config.ConfigLoader;
import com.qa.common.api.config.ConfigValidationException;
import com.qa.common.api.config.TypedConfig;
import com.qa.common.internal.config.source.ConfigSource;
import com.qa.common.internal.config.source.EnvVarSource;
import com.qa.common.internal.config.source.ExecutionContextSource;
import com.qa.common.internal.config.source.PropertiesFileSource;
import com.qa.common.internal.config.source.SystemPropertySource;
import com.qa.common.internal.config.source.YamlFileSource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación canónica de {@link ConfigLoader} (TASK-K03).
 *
 * <h2>Pipeline</h2>
 * <ol>
 *   <li>Lee el record {@code defaults()} como base.</li>
 *   <li>Itera los componentes del record. Para cada uno:
 *     <ul>
 *       <li>Construye la key canónica: {@code &lt;prefix&gt;.&lt;componentName&gt;}.</li>
 *       <li>Consulta los sources en orden de prioridad (SystemProperty &gt; EnvVar &gt; Yaml).</li>
 *       <li>Si encuentra valor, parsea al tipo del componente y reemplaza.</li>
 *     </ul>
 *   </li>
 *   <li>Construye un nuevo record con los valores resueltos.</li>
 *   <li>Valida con Bean Validation; lanza {@link ConfigValidationException} si falla.</li>
 *   <li>Cachea el resultado para llamadas subsiguientes a {@link #load}.</li>
 * </ol>
 *
 * <p>Soporte de tipos (parser):
 * <ul>
 *   <li>{@code String} — pasa directo.</li>
 *   <li>{@code int / long / boolean / double / float} — primitivos parseados.</li>
 *   <li>{@code Integer / Long / Boolean / Double / Float} — boxed.</li>
 *   <li>{@code java.time.Duration} — formato ISO-8601 {@code PT30S} o sufijos
 *       {@code 30s / 5m / 1h} (heurística simple).</li>
 *   <li>{@code Enum} — {@code Enum.valueOf(uppercase)}.</li>
 *   <li>{@code List&lt;String&gt;} — split por coma.</li>
 * </ul>
 *
 * @since TASK-K03
 */
@Internal(reason = "internal — usar com.qa.common.api.config.ConfigLoader como contrato público")
public class DefaultConfigLoader implements ConfigLoader {

    private final List<ConfigSource> sources;
    private final ValidatorFactory validatorFactory;
    private final Validator validator;
    private final ConcurrentHashMap<Class<?>, Object> cache = new ConcurrentHashMap<>();

    /**
     * Inicializa con los 5 sources canónicos en orden de prioridad
     * (ExecutionContext &gt; SystemProperty &gt; EnvVar &gt; PropertiesFile &gt; YamlFile).
     *
     * <p>{@link ExecutionContextSource} (TASK-K03M-F4) replica el step-0 del
     * legacy {@code ConfigManager}: en escenarios BDD paralelos, cada hilo lee
     * de su propio {@code ExecutionContext.current()}. Si no hay contexto
     * activo (ejecución sin BE), la cadena continúa sin NPE.
     *
     * <p>{@link PropertiesFileSource} (TASK-K03M-F2) preserva compat con
     * proyectos qa-module-test legacy que tienen {@code config-app.properties}
     * en {@code src/test/resources/}.
     */
    public DefaultConfigLoader() {
        this(List.of(
                new ExecutionContextSource(),
                new SystemPropertySource(),
                new EnvVarSource(),
                new PropertiesFileSource(),
                new YamlFileSource()));
    }

    /** Constructor inyectable para tests. */
    public DefaultConfigLoader(List<ConfigSource> sources) {
        this.sources = List.copyOf(sources);
        this.validatorFactory = Validation.buildDefaultValidatorFactory();
        this.validator = validatorFactory.getValidator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends TypedConfig> T load(Class<T> configClass) {
        return (T) cache.computeIfAbsent(configClass, k -> buildAndValidate((Class<T>) k));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends TypedConfig> T reload(Class<T> configClass) {
        cache.remove(configClass);
        return (T) cache.computeIfAbsent(configClass, k -> buildAndValidate((Class<T>) k));
    }

    /**
     * TASK-K03M-F1 — escape hatch para claves dinámicas. Reusa el mismo stack
     * de sources que {@link #load} sin parser ni validación.
     */
    @Override
    public Optional<String> getRaw(String key) {
        if (key == null || key.isBlank()) { return Optional.empty(); }
        return lookup(key);
    }

    /** Útil en shutdown hooks de tests. */
    public void close() {
        try { validatorFactory.close(); } catch (RuntimeException ignored) { /* best-effort */ }
    }

    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private <T extends TypedConfig> T buildAndValidate(Class<T> cls) {
        if (!cls.isRecord()) {
            throw new IllegalArgumentException(
                    "TypedConfig debe ser un record (R-K03-1): " + cls.getName());
        }
        T defaults = invokeDefaults(cls);
        String prefix = defaults.configPrefix();

        RecordComponent[] components = cls.getRecordComponents();
        Object[] values = new Object[components.length];
        Class<?>[] paramTypes = new Class<?>[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent c = components[i];
            paramTypes[i] = c.getType();
            String key = prefix + "." + c.getName();
            Optional<String> resolved = lookup(key);
            if (resolved.isPresent()) {
                values[i] = RecordValueParser.parse(resolved.get(), c);
            } else {
                // Use the value from defaults()
                values[i] = invokeAccessor(defaults, c);
            }
        }

        T instance;
        try {
            Constructor<T> canonical = cls.getDeclaredConstructor(paramTypes);
            canonical.setAccessible(true);
            instance = canonical.newInstance(values);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException(
                    "No se pudo instanciar el record " + cls.getName(), ex);
        }

        Set<ConstraintViolation<T>> violations = validator.validate(instance);
        if (!violations.isEmpty()) {
            throw new ConfigValidationException(cls, violations);
        }
        return instance;
    }

    /**
     * Resuelve {@code key} contra el stack de sources.
     *
     * <p>TASK-K03M-F7: para cada source, prueba la key canónica primero y
     * luego sus aliases legacy ({@link LegacyKeyAliases#candidatesFor}).
     * El orden exterior (sources) preserva la prioridad de source; el orden
     * interior (candidatos) garantiza que el canónico nunca es "shadowed" por
     * un alias en el mismo source.
     *
     * <p>TASK-K03M-F3: aplica {@link VariableInterpolator} al valor ganador
     * <strong>excepto</strong> cuando provino de {@link ExecutionContextSource}
     * — esos valores son finales (mismo contrato que {@code ConfigManager}
     * legacy: el BE entrega valores ya resueltos en {@code ExecutionConfig}).
     */
    private Optional<String> lookup(String key) {
        List<String> candidates = LegacyKeyAliases.candidatesFor(key);
        for (ConfigSource s : sources) {
            for (String candidate : candidates) {
                Optional<String> v = s.get(candidate);
                if (v.isPresent()) {
                    return s instanceof ExecutionContextSource
                            ? v
                            : Optional.of(VariableInterpolator.interpolate(v.get()));
                }
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private <T extends TypedConfig> T invokeDefaults(Class<T> cls) {
        try {
            var method = cls.getMethod("defaults");
            return (T) method.invoke(null);
        } catch (NoSuchMethodException nsme) {
            throw new IllegalArgumentException(
                    "TypedConfig debe declarar `public static " + cls.getSimpleName() + " defaults()` "
                            + "(R-K03-2): " + cls.getName(), nsme);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException(
                    "Error invocando defaults() en " + cls.getName(), ex);
        }
    }

    private static Object invokeAccessor(Object record, RecordComponent c) {
        try {
            return c.getAccessor().invoke(record);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Error leyendo " + c.getName(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // Type parser
    // -------------------------------------------------------------------------

    /**
     * Parser de string → tipo Java del component. Visible para test.
     */
    static final class RecordValueParser {

        private RecordValueParser() { }

        @SuppressWarnings({"unchecked", "rawtypes"})
        static Object parse(String raw, RecordComponent c) {
            if (raw == null) { return null; }
            Class<?> type = c.getType();
            String v = raw.trim();
            try {
                if (type == String.class) { return v; }
                if (type == int.class || type == Integer.class) { return Integer.parseInt(v); }
                if (type == long.class || type == Long.class) { return Long.parseLong(v); }
                if (type == boolean.class || type == Boolean.class) { return Boolean.parseBoolean(v); }
                if (type == double.class || type == Double.class) { return Double.parseDouble(v); }
                if (type == float.class || type == Float.class) { return Float.parseFloat(v); }
                if (type == Duration.class) { return parseDuration(v); }
                if (type.isEnum()) {
                    return Enum.valueOf((Class<Enum>) type, v.toUpperCase(Locale.ROOT));
                }
                if (List.class.isAssignableFrom(type)) {
                    // Asume List<String> (Genéricos borrados en runtime).
                    List<String> items = new ArrayList<>();
                    for (String s : v.split(",")) {
                        String trimmed = s.trim();
                        if (!trimmed.isEmpty()) { items.add(trimmed); }
                    }
                    return List.copyOf(items);
                }
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(
                        "No pude parsear valor '" + v + "' a " + type.getSimpleName()
                                + " para componente " + c.getName(), ex);
            }
            throw new IllegalArgumentException(
                    "Tipo no soportado por RecordValueParser para " + c.getName()
                            + ": " + type.getName()
                            + " (soporta String/primitivos/Duration/Enum/List<String>)");
        }

        /**
         * Acepta:
         * <ul>
         *   <li>ISO-8601: {@code PT30S}, {@code PT1H30M}</li>
         *   <li>Sufijos: {@code 30s}, {@code 5m}, {@code 1h}, {@code 500ms}</li>
         * </ul>
         */
        static Duration parseDuration(String v) {
            if (v.startsWith("P") || v.startsWith("-P") || v.startsWith("+P")) {
                return Duration.parse(v);
            }
            String lower = v.toLowerCase(Locale.ROOT);
            if (lower.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(lower.substring(0, lower.length() - 2).trim()));
            }
            if (lower.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(lower.substring(0, lower.length() - 1).trim()));
            }
            if (lower.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(lower.substring(0, lower.length() - 1).trim()));
            }
            if (lower.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(lower.substring(0, lower.length() - 1).trim()));
            }
            // fallback: número plano = segundos
            return Duration.ofSeconds(Long.parseLong(v.trim()));
        }
    }
}
