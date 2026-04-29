package com.qa.apicore.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rastrea la base URL efectiva durante la ejecución de un escenario.
 *
 * <p>Se crea una instancia por escenario (vive dentro de {@code ApiPlugin.onScenarioStart}) y
 * acumula:
 * <ul>
 *   <li>La URL efectiva actual ({@link #getEffectiveBaseUrl()}).</li>
 *   <li>El origen de ese valor ({@link #getSource()}).</li>
 *   <li>El historial completo de cambios ({@link #getHistory()}).</li>
 * </ul>
 *
 * <p>Al finalizar el escenario, {@link ApiPlugin} lee estos datos para construir
 * el {@link ExecutionMeta} que se envía al BE.
 */
public final class BaseUrlTracker {

    private static final Logger LOG = LoggerFactory.getLogger(BaseUrlTracker.class);

    private String        effectiveBaseUrl;
    private BaseUrlSource source;
    private final List<BaseUrlChange> history = new ArrayList<>();

    // ── Registro de cambios ───────────────────────────────────────────────────

    /**
     * Registra un cambio de base URL y lo aplica como valor efectivo actual.
     *
     * @param newUrl      URL ya normalizada (con schema, sin trailing slash).
     * @param src         Origen del cambio.
     * @param stepPattern Pattern del step causante; {@code null} si es el bootstrap.
     */
    public void record(String newUrl, BaseUrlSource src, String stepPattern) {
        boolean wasEnv = (source == BaseUrlSource.ENVIRONMENT);

        this.effectiveBaseUrl = newUrl;
        this.source           = src;
        this.history.add(new BaseUrlChange(newUrl, src, stepPattern, Instant.now()));

        if (wasEnv && src == BaseUrlSource.STEP_LITERAL) {
            LOG.warn("[BASE-URL] Sobrescritura: URL del ambiente reemplazada por URL literal " +
                     "del step '{}' → '{}'", stepPattern, newUrl);
        } else if (src == BaseUrlSource.ENVIRONMENT) {
            LOG.info("[BASE-URL] Bootstrap desde ambiente → '{}'", newUrl);
        } else {
            LOG.debug("[BASE-URL] Cambio ({}) → '{}' (step: '{}')", src, newUrl, stepPattern);
        }
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public boolean hasBaseUrl() {
        return effectiveBaseUrl != null;
    }

    public String getEffectiveBaseUrl() {
        return effectiveBaseUrl;
    }

    public BaseUrlSource getSource() {
        return source;
    }

    public List<BaseUrlChange> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /** {@code true} si hubo al menos un step que sobrescribió la URL del ambiente. */
    public boolean wasOverriddenFromEnvironment() {
        if (history.size() < 2) return false;
        return history.get(0).source() == BaseUrlSource.ENVIRONMENT
            && history.stream().anyMatch(c -> c.source() == BaseUrlSource.STEP_LITERAL);
    }
}
