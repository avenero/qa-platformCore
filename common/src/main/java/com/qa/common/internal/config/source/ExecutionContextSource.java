package com.qa.common.internal.config.source;

import com.qa.common.api.Internal;
import com.qa.common.api.logging.TestLogger;
import com.qa.common.api.runtime.ExecutionContext;

import java.util.Optional;

/**
 * Source de prioridad MÁXIMA: lee de {@code ExecutionContext.current().config()}
 * (ThreadLocal). Permite que ejecuciones BDD paralelas tengan configuración
 * aislada por hilo, replicando el step-0 de {@code ConfigManager} legacy
 * (TASK-K03M-F4).
 *
 * <p>Si no hay {@link ExecutionContext} activo en el hilo (e.g. ejecución
 * sin BE, health-check, {@code main}), retorna {@link Optional#empty()} sin
 * NPE — la cadena de sources continúa normalmente.
 *
 * <p>Los valores devueltos por esta fuente son <strong>finales</strong>:
 * {@code DefaultConfigLoader.lookup} NO les aplica
 * {@link com.qa.common.internal.config.VariableInterpolator interpolación de
 * {@code ${VAR}}} — los valores de {@code ExecutionConfig} ya vienen resueltos
 * por el BE (mismo contrato que {@code ConfigManager} hoy).
 *
 * @since TASK-K03M-F4
 */
@Internal(reason = "internal — usar com.qa.common.api.config.ConfigLoader para acceder")
public final class ExecutionContextSource implements ConfigSource {

    private static final TestLogger.LoggerWrapper LOG = TestLogger.getLogger(ExecutionContextSource.class);

    @Override
    public Optional<String> get(String key) {
        if (key == null || key.isBlank()) { return Optional.empty(); }
        try {
            return ExecutionContext.current().flatMap(ctx -> ctx.config().getProperty(key));
        } catch (RuntimeException e) {
            LOG.debug("ExecutionContextSource: error accediendo a ExecutionContext para '"
                    + key + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String name() {
        return "ExecutionContext";
    }
}
