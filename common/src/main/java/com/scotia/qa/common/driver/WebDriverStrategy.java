package com.scotia.qa.common.driver;

/**
 * Estrategias de obtención de WebDrivers.
 *
 * <p>Define los modos de obtención de binarios de WebDriver:</p>
 * <ul>
 *   <li><b>FALLBACK:</b> Estrategia inteligente (local → cache → artifactory)</li>
 *   <li><b>LOCAL:</b> Solo buscar en path local configurado</li>
 *   <li><b>ARTIFACTORY:</b> Solo descargar desde Artifactory</li>
 * </ul>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 2025-12-11
 */
public enum WebDriverStrategy {
    /**
     * Estrategia de triple fallback (recomendada).
     * Orden: local path → caché → Artifactory
     */
    FALLBACK,

    /**
     * Solo buscar en path local fijo.
     * Falla si no existe en la ruta configurada.
     */
    LOCAL,

    /**
     * Solo descargar desde Artifactory.
     * Usa caché si ya fue descargado previamente.
     */
    ARTIFACTORY;

    /**
     * Parsea string de configuración a enum.
     *
     * @param strategy String de configuración (case-insensitive)
     * @return Enum correspondiente, FALLBACK por defecto
     */
    public static WebDriverStrategy fromString(String strategy) {
        if (strategy == null || strategy.trim().isEmpty()) {
            return FALLBACK;
        }

        try {
            return WebDriverStrategy.valueOf(strategy.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FALLBACK;
        }
    }
}

