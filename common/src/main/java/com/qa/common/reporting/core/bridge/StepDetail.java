package com.qa.common.reporting.core.bridge;

/**
 * Contrato de auto-renderizado para detalles de protocolo adjuntos a un step de prueba.
 *
 * <p>Permite que {@link com.qa.common.reporting.core.service.ExtentReportGeneratorImpl}
 * delegue la representación HTML/texto de cada detalle al módulo de protocolo concreto
 * (http-core, database-core, web-core…), sin que {@code common} deba conocer semántica
 * de protocolo alguna.
 *
 * <h3>Diseño OCP/SRP</h3>
 * <ul>
 *   <li>Agregar soporte para un nuevo protocolo (SQL, gRPC, UI…) NO requiere modificar
 *       {@code common} — solo implementar esta interfaz en el módulo de protocolo
 *       correspondiente.</li>
 *   <li>{@code ExtentReportGeneratorImpl} coordina el cuándo y dónde del reporte;
 *       cada implementación concreta decide el qué y cómo.</li>
 * </ul>
 *
 * <h3>Implementaciones concretas (viven en módulos de protocolo, no en common)</h3>
 * <ul>
 *   <li>{@code http-core} — {@code HttpDetailData}, {@code HttpStepDetail}</li>
 *   <li>{@code database-core} — futuro {@code DbQueryDetail}</li>
 *   <li>{@code web-core} — futuro {@code UiActionDetail}</li>
 * </ul>
 *
 * <h3>Invariante de seguridad</h3>
 * El HTML retornado por {@link #renderHtml()} debe escapar todo contenido controlado
 * por el usuario. Aplicar HTML-escaping a cualquier campo proveniente de peticiones
 * o respuestas externas antes de interpolarlo.
 *
 * @since 2.3.0
 * @see StepData
 */
public interface StepDetail {

    /**
     * Fragmento HTML seguro para insertar en el reporte Extent.
     * El fragmento debe ser autónomo y no requerir estilos externos.
     * Todo el contenido de usuario debe ser HTML-escaped.
     */
    String renderHtml();

    /** Representación texto plano para logs y reportes no-HTML. */
    String renderText();
}
