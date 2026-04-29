package com.qa.common.exception;

/**
 * Excepción técnica del framework QA.
 *
 * <p>Se lanza ante errores de infraestructura que impiden la ejecución del test
 * independientemente del comportamiento de la aplicación bajo prueba:
 * <ul>
 *   <li>Fallo al inicializar WebDriver, Appium o cliente HTTP</li>
 *   <li>Problemas de conectividad de red (timeout, DNS, SSL)</li>
 *   <li>Configuración incorrecta o ausente (URL de base no definida)</li>
 *   <li>Error al leer/escribir archivos (feature files, configuración)</li>
 *   <li>Fallo en operaciones de cifrado o seguridad</li>
 *   <li>Indisponibilidad de base de datos o servicios de soporte</li>
 * </ul>
 *
 * <p><b>Distinción respecto a {@link FrameworkBusinessException}:</b><br>
 * Una excepción técnica indica un fallo del entorno de testing, no un comportamiento
 * inesperado de la aplicación. Suele requerir intervención de infraestructura.
 *
 * @author QA Automation Framework Team
 * @since 1.0.0
 * @see FrameworkBusinessException
 */
public class FrameworkTechnicalException extends FrameworkException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor con mensaje simple.
     *
     * @param message mensaje descriptivo del error técnico
     */
    public FrameworkTechnicalException(String message) {
        super(message);
    }

    /**
     * Constructor con mensaje y causa.
     *
     * @param message mensaje descriptivo del error técnico
     * @param cause   excepción original que provocó este error
     */
    public FrameworkTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor con prefijo de método para contexto de depuración.
     *
     * @param methodName nombre del método donde ocurrió el error
     * @param message    mensaje descriptivo del error técnico
     */
    public FrameworkTechnicalException(String methodName, String message) {
        super(methodName, message);
    }
}
