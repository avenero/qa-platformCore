package com.qa.common.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * DTO inmutable que representa una solicitud de ejecucion BDD.
 *
 * <p>Contiene las rutas de features, glue code, tags, y la configuracion
 * necesaria para iniciar una ejecucion.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class ExecutionRequest {

    private final List<String> featurePaths;
    private final List<String> gluePaths;
    private final ExecutionConfig config;

    private ExecutionRequest(List<String> featurePaths, List<String> gluePaths, ExecutionConfig config) {
        this.featurePaths = Collections.unmodifiableList(featurePaths);
        this.gluePaths = Collections.unmodifiableList(gluePaths);
        this.config = Objects.requireNonNull(config, "config no puede ser null");
    }

    public static ExecutionRequest of(List<String> featurePaths, List<String> gluePaths, ExecutionConfig config) {
        Objects.requireNonNull(featurePaths, "featurePaths no puede ser null");
        Objects.requireNonNull(gluePaths, "gluePaths no puede ser null");
        return new ExecutionRequest(List.copyOf(featurePaths), List.copyOf(gluePaths), config);
    }

    public List<String> getFeaturePaths() { return featurePaths; }
    public List<String> getGluePaths() { return gluePaths; }
    public ExecutionConfig getConfig() { return config; }

    @Override
    public String toString() {
        return "ExecutionRequest{features=" + featurePaths.size()
                + ", glue=" + gluePaths.size() + ", config=" + config + "}";
    }
}
