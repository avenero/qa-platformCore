package com.qa.common.api.exception;

import com.qa.common.utils.security.SecurityUtilities;

/**
 * Excepción de negocio del framework QA.
 *
 * <p>Se lanza ante errores funcionales que el framework puede clasificar con precisión:
 * <ul>
 *   <li>Aserciones fallidas (status code incorrecto, campo JSON ausente, texto no encontrado)</li>
 *   <li>Reglas de validación violadas (schema inválido, formato incorrecto)</li>
 *   <li>Configuración de step incorrecta (parámetro obligatorio faltante)</li>
 *   <li>Operaciones de datos inválidas (JSON malformado, tipo incompatible)</li>
 * </ul>
 *
 * <p><b>Distinción respecto a {@link FrameworkTechnicalException}:</b><br>
 * Una excepción de negocio indica que el test encontró un comportamiento inesperado
 * de la aplicación bajo prueba — no un fallo de infraestructura.
 *
 * @author QA Automation Framework Team
 * @since 1.0.0
 * @see FrameworkTechnicalException
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
     * Constructor con prefijo de método para contexto de depuración.
     *
     * @param methodName nombre del método donde ocurrió el error
     * @param message    mensaje descriptivo del error de negocio
     */
    public FrameworkBusinessException(String methodName, String message) {
        super(methodName, message);
    }
}
