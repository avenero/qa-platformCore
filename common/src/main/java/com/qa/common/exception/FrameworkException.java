package com.qa.common.exception;

/**
 * Excepción base del framework de testing QA.
 *
 * <p>Todas las excepciones específicas del framework heredan de esta clase.
 * La jerarquía distingue dos categorías de error:
 * <ul>
 *   <li>{@link FrameworkBusinessException} — errores funcionales: validaciones,
 *       reglas de negocio, aserciones fallidas.</li>
 *   <li>{@link FrameworkTechnicalException} — errores de infraestructura:
 *       conectividad, configuración, drivers, timeouts.</li>
 * </ul>
 *
 * <p><b>Por qué está en {@code com.qa.common.exception} y no en {@code http.exceptions}:</b><br>
 * Esta jerarquía es transversal al framework completo (API, Web, Mobile, Database)
 * y no pertenece exclusivamente a la capa HTTP.
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
     * Constructor con prefijo de método para contexto de depuración.
     *
     * <p>Produce mensajes del estilo {@code "methodName: descripción del error"}.
     *
     * @param methodName nombre del método donde ocurrió el error
     * @param message    mensaje descriptivo del error
     */
    public FrameworkException(String methodName, String message) {
        super(methodName + ": " + message);
    }
}
