package com.qa.common.http.exceptions;

/**
 * Excepción base del framework de testing. Todas las excepciones específicas del framework deben
 * heredar de esta clase.
 *
 * @author QA Automation Framework Team
 * @since 1.0.0
 */
public abstract class FrameworkException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor con mensaje simple.
     *
     * @param message mensaje descriptivo del error
     */
    public FrameworkException(String message) {
        super(message);
    }

    /**
     * Constructor con mensaje y causa.
     *
     * @param message mensaje descriptivo del error
     * @param cause   excepción original que provocó este error
     */
    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor con prefijo de método para contexto.
     *
     * @param methodName nombre del método donde ocurrió el error
     * @param message    mensaje descriptivo del error
     */
    public FrameworkException(String methodName, String message) {
        super(methodName + ": " + message);
    }
}
