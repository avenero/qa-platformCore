package com.qa.common.api.runtime;

import com.qa.common.api.runtime.CrossLayerContextPort;
import com.qa.common.utils.security.SecurityUtilities;

import com.qa.common.api.logging.TestLogger;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contexto en memoria para compartir datos entre capas (API, WEB, MOBILE)
 * dentro de un mismo escenario BDD.
 *
 * <p>A diferencia de {@link VariableStore} (que persiste durante toda la suite),
 * {@code StepExecutionContext} está pensado para un único escenario cross-layer.
 * Cada capa puede leer y escribir valores, que son accesibles globalmente
 * via {@link #current()} en el mismo hilo o mediante traspaso explícito.
 *
 * <p>El ciclo de vida recomendado es:
 * <ol>
 *   <li>{@link #start()} al inicio del escenario (en el hook de Cucumber)</li>
 *   <li>Steps de cualquier capa llaman a {@link #store}/{@link #retrieve}</li>
 *   <li>{@link #clear()} al finalizar el escenario para evitar fugas</li>
 * </ol>
 *
 * @since 2.1.0
 */
public final class StepExecutionContext implements CrossLayerContextPort {

    private static final TestLogger.LoggerWrapper LOG =
            TestLogger.getLogger(StepExecutionContext.class);

    private static final ThreadLocal<StepExecutionContext> CURRENT = new ThreadLocal<>();

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");

    private final Map<String, Object> variables = new ConcurrentHashMap<>();

    private StepExecutionContext() {}

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Crea e instala un nuevo contexto vacío en el hilo actual.
     * Si ya existía uno, lo reemplaza (y loguea una advertencia).
     */
    public static StepExecutionContext start() {
        if (CURRENT.get() != null) {
            LOG.warn("StepExecutionContext.start() llamado con contexto activo — reemplazando");
        }
        StepExecutionContext ctx = new StepExecutionContext();
        CURRENT.set(ctx);
        return ctx;
    }

    /**
     * Devuelve el contexto activo del hilo, o vacío si no hay ninguno.
     */
    public static Optional<StepExecutionContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /**
     * Devuelve el contexto activo; lanza excepción si no hay ninguno.
     */
    public static StepExecutionContext requireCurrent() {
        StepExecutionContext ctx = CURRENT.get();
        if (ctx == null) {
            throw new IllegalStateException(
                "StepExecutionContext no está activo. Llama a StepExecutionContext.start() antes.");
        }
        return ctx;
    }

    /**
     * Limpia el contexto del hilo actual y lo elimina del ThreadLocal.
     */
    public static void clearCurrent() {
        CURRENT.remove();
    }

    // =========================================================================
    // CrossLayerContextPort implementation
    // =========================================================================

    @Override
    public void store(String key, Object value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key no puede ser null o vacío");
        }
        variables.put(key, value);
        LOG.debug("CrossLayer store: {} = {}", key, value);
    }

    @Override
    public Optional<Object> retrieve(String key) {
        return Optional.ofNullable(variables.get(key));
    }

    @Override
    public String resolveVariables(String template) {
        if (template == null) {
            return null;
        }
        if (variables.isEmpty()) {
            return template;
        }

        StringBuffer sb = new StringBuffer();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1);
            Object val = variables.get(key);
            matcher.appendReplacement(sb, val != null ? Matcher.quoteReplacement(val.toString()) : matcher.group(0));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public void transfer(String fromKey, String toKey, CrossLayerContextPort target) {
        retrieve(fromKey).ifPresent(v -> target.store(toKey, v));
    }

    @Override
    public void clear() {
        variables.clear();
        LOG.debug("CrossLayer context cleared");
    }

    @Override
    public boolean isEmpty() {
        return variables.isEmpty();
    }

    /** @return copia inmutable del mapa de variables (útil para depuración) */
    public Map<String, Object> snapshot() {
        return Collections.unmodifiableMap(variables);
    }
}
