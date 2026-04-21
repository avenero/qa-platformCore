package com.qa.common.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * DTO inmutable que representa una solicitud de ejecucion BDD.
 *
 * <p>Contiene las rutas de features, (opcionalmente) glue code, y la configuracion
 * necesaria para iniciar una ejecucion.
 *
 * <h2>Modos de uso</h2>
 * <ul>
 *   <li><b>Modo SPI (recomendado):</b> {@link #of(List, ExecutionConfig)} — sin gluePaths.
 *       El {@link CucumberRuntimeEngine} deriva los paquetes de glue automáticamente desde
 *       los plugins descubiertos vía {@link java.util.ServiceLoader}. El Backend no necesita
 *       mantener una lista manual de paquetes.</li>
 *   <li><b>Modo explícito (override):</b> {@link #of(List, List, ExecutionConfig)} — con gluePaths.
 *       Permite al Backend sobrescribir o extender la resolución automática para casos
 *       especiales (plugins custom, módulos de terceros).</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.0.0
 * @since 2.2.0 gluePaths es opcional; el engine los deriva de los plugins SPI si se omite
 */
public final class ExecutionRequest {

    private final List<String> featurePaths;
    private final List<String> gluePaths;
    private final ExecutionConfig config;

    private ExecutionRequest(List<String> featurePaths, List<String> gluePaths, ExecutionConfig config) {
        this.featurePaths = Collections.unmodifiableList(featurePaths);
        this.gluePaths    = Collections.unmodifiableList(gluePaths);
        this.config       = Objects.requireNonNull(config, "config no puede ser null");
    }

    /**
     * Crea un request SIN gluePaths explícitos.
     *
     * <p>El {@link CucumberRuntimeEngine} derivará los paquetes de glue automáticamente
     * de los plugins descubiertos vía SPI ({@link java.util.ServiceLoader}).
     * El Backend no necesita conocer ni mantener ningún paquete de steps.
     *
     * <p>Este es el modo de uso recomendado a partir de {@code 2.2.0}.
     *
     * @param featurePaths rutas a archivos {@code .feature} o directorios, no null
     * @param config       configuración inmutable de la ejecución, no null
     * @return nueva instancia de {@code ExecutionRequest} con gluePaths vacío
     * @since 2.2.0
     */
    public static ExecutionRequest of(List<String> featurePaths, ExecutionConfig config) {
        Objects.requireNonNull(featurePaths, "featurePaths no puede ser null");
        Objects.requireNonNull(config,       "config no puede ser null");
        return new ExecutionRequest(List.copyOf(featurePaths), List.of(), config);
    }

    /**
     * Crea un request CON gluePaths explícitos.
     *
     * <p>Usar cuando se necesite sobrescribir o extender la resolución automática de paquetes
     * (plugins custom, módulos de terceros no registrados en SPI, etc.).
     * Si {@code gluePaths} está vacío, el engine igualmente derivará los paquetes desde SPI.
     *
     * @param featurePaths rutas a archivos {@code .feature} o directorios, no null
     * @param gluePaths    paquetes Java con step definitions y hooks, no null (puede ser vacío)
     * @param config       configuración inmutable de la ejecución, no null
     * @return nueva instancia de {@code ExecutionRequest}
     */
    public static ExecutionRequest of(List<String> featurePaths, List<String> gluePaths, ExecutionConfig config) {
        Objects.requireNonNull(featurePaths, "featurePaths no puede ser null");
        Objects.requireNonNull(gluePaths,    "gluePaths no puede ser null");
        Objects.requireNonNull(config,       "config no puede ser null");
        return new ExecutionRequest(List.copyOf(featurePaths), List.copyOf(gluePaths), config);
    }

    /**
     * @return rutas de features, nunca null
     */
    public List<String> getFeaturePaths() {
        return featurePaths;
    }

    /**
     * @return paquetes de glue explícitos; puede ser vacío si se usó el factory sin gluePaths
     *         (en ese caso el engine los deriva automáticamente de los plugins SPI)
     */
    public List<String> getGluePaths() {
        return gluePaths;
    }

    /**
     * @return configuración de la ejecución, nunca null
     */
    public ExecutionConfig getConfig() {
        return config;
    }

    /**
     * @return {@code true} si el caller no proporcionó gluePaths explícitos;
     *         el engine los derivará de los plugins SPI
     * @since 2.2.0
     */
    public boolean isGlueAutoResolved() {
        return gluePaths.isEmpty();
    }

    @Override
    public String toString() {
        return "ExecutionRequest{features=" + featurePaths.size()
                + ", glue=" + (gluePaths.isEmpty() ? "auto(SPI)" : gluePaths.size())
                + ", config=" + config + "}";
    }
}
