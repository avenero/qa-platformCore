package com.scotia.qa.common.http.exceptions;

/**
 * Excepción base del framework de testing. Todas las excepciones específicas del framework deben
 * heredar de esta clase.
 *
 * @author Scotia QA Framework Team
 * @since 1.0.0
 */
public abstract class FrameworkException extends Exception {

    private static final long serialVersionUID = 1L;

    /** Constructor con mensaje simple. */
    public FrameworkException(String message) {
        super(message);
    }

    /** Constructor con mensaje y causa. */
    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Constructor con prefijo de método para contexto. */
    public FrameworkException(String methodName, String message) {
        super(methodName + ": " + message);
    }
}
