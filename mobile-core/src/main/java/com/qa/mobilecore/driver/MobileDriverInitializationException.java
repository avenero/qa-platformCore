package com.qa.mobilecore.driver;

/**
 * Excepción no-checked lanzada cuando no es posible inicializar o mantener
 * una sesión Appium.
 *
 * <p>Análoga a {@code WebDriverInitializationException} de {@code web-core}.
 * Sustituye a {@link RuntimeException} genérico para que los bloques catch de
 * los hooks y del plugin puedan distinguir fallos de sesión mobile de otros errores.
 *
 * @author Abel Venero
 * @since 2.2.0
 */
public class MobileDriverInitializationException extends RuntimeException {

    /**
     * Creates a new exception with the given message.
     *
     * @param message descripcion del fallo (plataforma, udid, URL de Appium, causa raiz).
     */
    public MobileDriverInitializationException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given message and cause.
     *
     * @param message descripcion del fallo.
     * @param cause   excepcion original de Appium / WebDriver.
     */
    public MobileDriverInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
