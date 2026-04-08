package com.qa.common.http.exceptions;

/**
 * Excepción de negocio del framework. Se utiliza para errores relacionados con validaciones de
 * datos, reglas de negocio, y problemas funcionales en las pruebas.
 *
 * @author QA Automation Framework Team
 * @since 1.0.0
 */
public class FrameworkBusinessException extends FrameworkException {

    private static final long serialVersionUID = 1L;

    /** Constructor con mensaje simple. */
    public FrameworkBusinessException(String message) {
        super(message);
    }

    /** Constructor con mensaje y causa. */
    public FrameworkBusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Constructor con prefijo de método para contexto. */
    public FrameworkBusinessException(String methodName, String message) {
        super(methodName, message);
    }
}
