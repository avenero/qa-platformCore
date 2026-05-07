package com.qa.common.reporting.core.bridge;

import java.util.Map;

/**
 * Contrato mínimo de un snapshot HTTP seguro para el pipeline de reporting de {@code common}.
 *
 * <p>Esta interfaz permite que {@link StepData}, {@link com.qa.common.reporting.core.model.StepResult}
 * y {@link com.qa.common.reporting.core.service.ExtentReportGeneratorImpl} operen sobre datos HTTP
 * sin acoplarse a los tipos concretos definidos en {@code http-core}
 * ({@code HttpDetailData}, {@code HttpStepDetail}, {@code HttpDetailRedactor}).
 *
 * <h3>Invariante de seguridad</h3>
 * Todos los datos expuestos por esta interfaz deben estar ya <b>redactados</b>: sin tokens,
 * credenciales ni datos PCI-DSS. La responsabilidad de aplicar la redacción recae en el constructor
 * del tipo concreto (p.ej., {@code HttpDetailRedactor} en {@code http-core}).
 *
 * <h3>Diseño</h3>
 * Los métodos usan nomenclatura de estilo record (sin prefijo {@code get}) para que los records de
 * {@code http-core} puedan implementar la interfaz simplemente declarando los componentes con los
 * nombres correctos.
 *
 * @since 2.2.0
 * @see StepData
 * @see com.qa.common.reporting.core.model.StepResult
 */
public interface HttpStepSummary {

    /** Método HTTP en mayúsculas (GET, POST, PUT, …). Puede ser null si desconocido. */
    String method();

    /** Path relativo de la URL, sin host, query string ni credenciales. */
    String urlPath();

    /** Código de estado HTTP de la respuesta. */
    int httpStatus();

    /** ID de correlación de la petición para trazabilidad. Puede ser null. */
    String requestId();

    /** Resumen redactado del cuerpo de la petición, truncado a ≤500 chars. Puede ser null. */
    String requestSummary();

    /** Resumen redactado del cuerpo de la respuesta, truncado a ≤500 chars. Puede ser null. */
    String responseSummary();

    /** Tamaño original del body de la petición en bytes. Puede ser null. */
    Integer requestBodySizeBytes();

    /** Tamaño original del body de la respuesta en bytes. Puede ser null. */
    Integer responseBodySizeBytes();

    /** Duración de la petición en milisegundos. Puede ser null. */
    Long durationMs();

    /**
     * Headers de petición con valores sensibles reemplazados por {@code "REDACTED"}.
     * Puede ser null si no se capturaron headers.
     */
    Map<String, String> requestHeaders();

    /**
     * Headers de respuesta con valores sensibles reemplazados por {@code "REDACTED"}.
     * Puede ser null si no se capturaron headers.
     */
    Map<String, String> responseHeaders();
}
