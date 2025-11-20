package com.scotia.qa.common.logging;

/**
 * Inicializador de configuración de logging para suprimir mensajes informativos de SLF4J.
 *
 * <p>Esta clase se carga automáticamente al inicio y configura las propiedades del sistema
 * necesarias para silenciar los mensajes de inicialización de SLF4J como:
 * "SLF4J(I): Connected with provider of type [ch.qos.logback.classic.spi.LogbackServiceProvider]"
 *
 * <p><b>Uso:</b> No es necesario llamar a esta clase explícitamente. El bloque static
 * se ejecuta automáticamente cuando la clase es cargada por el ClassLoader.
 *
 * @author Scotia QA Framework Team
 * @version 1.0.1
 * @since 1.0.1
 */
public class LoggingInitializer {

    static {
        // Suprimir mensajes informativos de SLF4J
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        System.setProperty("slf4j.internal.verbosity", "WARN");

        // Suprimir mensajes de status de Logback
        System.setProperty("logback.statusListenerClass", "ch.qos.logback.core.status.NopStatusListener");
    }

    /**
     * Constructor privado para prevenir instanciación.
     */
    private LoggingInitializer() {
        throw new UnsupportedOperationException("LoggingInitializer es una clase de utilidad y no debe ser instanciada");
    }

    /**
     * Método público para forzar la carga de la clase y ejecutar el bloque static.
     * Útil para asegurar que la configuración se aplique antes de cualquier logging.
     */
    public static void initialize() {
        // No hace nada, solo fuerza la carga de la clase y ejecución del bloque static
    }
}

