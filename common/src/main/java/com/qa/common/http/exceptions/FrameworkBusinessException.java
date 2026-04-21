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

    /**
     * Constructor con mensaje simple.
     *
     * @param message mensaje descriptivo del error de negocio
     */
    public FrameworkBusinessException(String message) {
        super(message);
    }

    /**
     * Constructor con mensaje y causa.
     *
     * @param message mensaje descriptivo del error de negocio
     * @param cause   excepción original que provocó este error
     */
    public FrameworkBusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor con prefijo de método para contexto.
     *
     * @param methodName nombre del método donde ocurrió el error
     * @param message    mensaje descriptivo del error de negocio
     */
    public FrameworkBusinessException(String methodName, String message) {
        super(methodName, message);
    }
}
