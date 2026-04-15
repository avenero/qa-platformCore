package com.qa.webcore.driver;

/**
 * Excepción lanzada cuando no es posible inicializar un {@link org.openqa.selenium.WebDriver}.
 *
 * <p>Se produce cuando se agotan todas las estrategias de búsqueda del driver binario:
 * <ol>
 *   <li>Selenium Manager (automático en Selenium 4.6+)</li>
 *   <li>Búsqueda en PATH del sistema</li>
 * </ol>
 *
 * <p>El mensaje de la excepción siempre incluye instrucciones concretas de resolución.
 *
 * @author Abel Venero
 * @since 2.2.0
 * @see WebDriverFactory
 */
public class WebDriverInitializationException extends RuntimeException {

    /**
     * Crea la excepción con un mensaje descriptivo.
     *
     * @param message mensaje con causa y pasos de resolución, nunca null
     */
    public WebDriverInitializationException(String message) {
        super(message);
    }

    /**
     * Crea la excepción con mensaje y causa raíz.
     *
     * @param message mensaje con causa y pasos de resolución
     * @param cause   excepción original que provocó el fallo
     */
    public WebDriverInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
