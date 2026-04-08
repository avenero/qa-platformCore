package com.qa.apicore.interfaces;

/**
 * Contrato para configurar aspectos tecnicos del cliente HTTP:
 * timeouts, politica de retry y limpieza de estado.
 *
 * <p>Segregado desde {@link HttpClient} para cumplir el ISP.</p>
 *
 * @author Abel Venero
 * @since 2.0.0
 * @see HttpClient
 */
public interface HttpClientConfigurator {

    /**
     * Configura el timeout de conexion en ms.
     *
     * @param timeoutMs Timeout en ms (debe ser mayor a 0)
     * @throws IllegalArgumentException Si timeoutMs es menor o igual a 0
     */
    void setConnectionTimeout(int timeoutMs);

    /**
     * Configura el timeout de lectura en ms.
     *
     * @param timeoutMs Timeout en ms (debe ser mayor a 0)
     * @throws IllegalArgumentException Si timeoutMs es menor o igual a 0
     */
    void setReadTimeout(int timeoutMs);

    /**
     * Obtiene el timeout de conexion configurado.
     *
     * @return Timeout en ms, -1 si usa valores por defecto del sistema
     */
    int getConnectionTimeout();

    /**
     * Obtiene el timeout de lectura configurado.
     *
     * @return Timeout en ms, -1 si usa valores por defecto del sistema
     */
    int getReadTimeout();

    /**
     * Configura timeout de conexion Y lectura con el mismo valor (conveniencia).
     * Delega a {@link #setConnectionTimeout} y {@link #setReadTimeout}.
     *
     * @param timeoutMs timeout total en ms
     * @since 2.0.0
     */
    default void setTimeout(int timeoutMs) {
        setConnectionTimeout(timeoutMs);
        setReadTimeout(timeoutMs);
    }

    /**
     * Configura politica de reintentos en caso de fallos.
     *
     * @param maxRetries   Numero maximo de reintentos (mayor o igual a 0)
     * @param retryDelayMs Delay entre reintentos en ms (mayor o igual a 0)
     * @throws IllegalArgumentException Si alguno de los valores es negativo
     */
    void setRetryPolicy(int maxRetries, int retryDelayMs);

    /**
     * Verifica si el retry automatico esta habilitado.
     *
     * @return true si maxRetries es mayor a 0
     */
    boolean isRetryEnabled();

    /**
     * Obtiene el numero maximo de reintentos configurado.
     *
     * @return Numero maximo de reintentos, 0 si no hay retry
     */
    int getMaxRetries();

    /**
     * Obtiene el delay entre reintentos configurado.
     *
     * @return Delay en ms, 0 si no hay retry configurado
     */
    int getRetryDelay();

    /**
     * Limpia headers, query params, fields y body, pero mantiene el host configurado.
     */
    void clearRequestData();

    /**
     * Limpia completamente el cliente, incluyendo host y todos los datos configurados.
     */
    void reset();

    /**
     * Representacion del estado del cliente para debugging (sin exponer datos sensibles).
     *
     * @return Estado del cliente como string
     */
    String getDebugInfo();
}
