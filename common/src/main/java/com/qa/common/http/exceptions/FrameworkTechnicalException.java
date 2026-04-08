package com.qa.common.http.exceptions;

/**
 * Excepción técnica del framework. Se utiliza para errores relacionados con la infraestructura,
 * configuración, conectividad y problemas técnicos que no están relacionados con la lógica de
 * negocio.
 *
 * @author QA Automation Framework Team
 * @since 1.0.0
 */
public class FrameworkTechnicalException extends FrameworkException {

    private static final long serialVersionUID = 1L;

    /** Constructor con mensaje simple. */
    public FrameworkTechnicalException(String message) {
        super(message);
    }

    /** Constructor con mensaje y causa. */
    public FrameworkTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Constructor con prefijo de método para contexto. */
    public FrameworkTechnicalException(String methodName, String message) {
        super(methodName, message);
    }
}
