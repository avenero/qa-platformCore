package com.qa.common.driver;

import java.util.List;
import java.util.Objects;

/**
 * Informe de capacidades que un plugin reporta al motor de ejecución y al backend/frontend.
 *
 * <p>Permite al sistema saber qué plataformas, browsers o motores de BD están disponibles
 * en el entorno actual antes de que el usuario diseñe o ejecute un escenario. El FE puede
 * usar este informe para construir selectores dinámicos (ej: lista de browsers soportados).
 *
 * <h2>Uso típico</h2>
 * <pre>
 * // Plugin disponible con opciones concretas
 * CapabilityReport.available("WEB", List.of(
 *     new CapabilityDescriptor("chromium", "Chromium", "Chromium-based browser (default)"),
 *     new CapabilityDescriptor("firefox",  "Firefox",  "Mozilla Firefox")
 * ));
 *
 * // Plugin no disponible (Appium no instalado, por ejemplo)
 * CapabilityReport.unavailable("MOBILE", "Appium server not reachable at localhost:4723");
 * </pre>
 *
 * <h2>Contrato de inmutabilidad</h2>
 * <p>Este record es completamente inmutable. Las listas se copian defensivamente en
 * los factory methods para garantizar que el caller no pueda mutar el informe después
 * de crearlo.
 *
 * @param platformId        identificador de la plataforma ({@link com.qa.common.runtime.CorePlugin#platformId()})
 * @param available         {@code true} si el plugin puede operar en el entorno actual
 * @param unavailableReason razón de no disponibilidad; {@code null} si {@code available == true}
 * @param options           capacidades concretas disponibles (browsers, dispositivos, motores de BD, etc.)
 *
 * @author Abel Venero
 * @since 3.0.0
 * @see CapabilityDescriptor
 */
public record CapabilityReport(
    String platformId,
    boolean available,
    String unavailableReason,
    List<CapabilityDescriptor> options
) {

    /**
     * Constructor canónico con validación y copia defensiva.
     */
    public CapabilityReport {
        Objects.requireNonNull(platformId, "platformId no puede ser null");
        if (platformId.isBlank()) {
            throw new IllegalArgumentException("platformId no puede ser vacío");
        }
        if (!available && (unavailableReason == null || unavailableReason.isBlank())) {
            throw new IllegalArgumentException(
                "unavailableReason es obligatorio cuando available=false");
        }
        if (available && unavailableReason != null) {
            throw new IllegalArgumentException(
                "unavailableReason debe ser null cuando available=true");
        }
        options = options == null ? List.of() : List.copyOf(options);
    }

    // =========================================================================
    // Factory methods
    // =========================================================================

    /**
     * Crea un informe de plugin disponible con las opciones indicadas.
     *
     * @param platformId identificador de la plataforma (ej: "WEB", "HTTP", "DATABASE")
     * @param opts       lista de capacidades disponibles (puede ser vacía si el plugin no
     *                   expone opciones configurables)
     * @return informe disponible, nunca null
     */
    public static CapabilityReport available(String platformId, List<CapabilityDescriptor> opts) {
        return new CapabilityReport(platformId, true, null,
            opts == null ? List.of() : List.copyOf(opts));
    }

    /**
     * Crea un informe de plugin no disponible.
     *
     * @param platformId identificador de la plataforma
     * @param reason     razón técnica de la no disponibilidad (se incluye en logs y errores de UI)
     * @return informe no disponible, nunca null
     */
    public static CapabilityReport unavailable(String platformId, String reason) {
        Objects.requireNonNull(reason, "reason no puede ser null en unavailable()");
        return new CapabilityReport(platformId, false, reason, List.of());
    }
}
