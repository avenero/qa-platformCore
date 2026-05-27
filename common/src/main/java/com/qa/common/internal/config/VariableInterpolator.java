package com.qa.common.internal.config;

import com.qa.common.api.Internal;
import com.qa.common.api.logging.TestLogger;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reemplaza placeholders {@code ${VAR}} en valores de configuración resolviendo
 * contra (en orden):
 * <ol>
 *   <li>{@link System#getProperty(String)} con el nombre literal.</li>
 *   <li>{@link System#getenv(String)} con el nombre literal.</li>
 *   <li>{@code System.getenv} con la variante {@code UPPER_SNAKE} (puntos → underscore).</li>
 *   <li>{@code System.getProperty} con la variante {@code lowercase.dotted} (underscore → punto).</li>
 * </ol>
 *
 * <p>Si una variable no se resuelve, queda literal {@code ${VAR}} y se loguea
 * a nivel {@code DEBUG} (no warn — el valor puede ser opcional).
 *
 * <p>Algoritmo portado verbatim de {@code ConfigManager.resolveEnvironmentVariables}
 * para preservar contrato (TASK-K03M-F3). El llamador
 * ({@code DefaultConfigLoader.lookup}) decide si invocar o no este interpolador
 * según la fuente del valor — los valores que vienen de {@code ExecutionContextSource}
 * son finales y NO se interpolan (consistente con contrato actual del legacy).
 *
 * @since TASK-K03M-F3
 */
@Internal(reason = "internal — usado por DefaultConfigLoader.lookup()")
public final class VariableInterpolator {

    private static final TestLogger.LoggerWrapper LOG = TestLogger.getLogger(VariableInterpolator.class);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private VariableInterpolator() {}

    public static String interpolate(String value) {
        if (value == null || !value.contains("${")) { return value; }

        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String resolved = resolve(varName);
            if (resolved == null) {
                LOG.debug("VariableInterpolator: '" + varName + "' no resuelta — queda literal");
                resolved = matcher.group(0);
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String resolve(String varName) {
        String v = System.getProperty(varName);
        if (v != null) { return v; }

        v = System.getenv(varName);
        if (v != null) { return v; }

        String upperSnake = varName.toUpperCase(Locale.ROOT).replace('.', '_');
        v = System.getenv(upperSnake);
        if (v != null) { return v; }

        String lowerDotted = varName.toLowerCase(Locale.ROOT).replace('_', '.');
        return System.getProperty(lowerDotted);
    }
}
