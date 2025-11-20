package com.scotia.qa.common.http;

/**
 * Enumeración de métodos HTTP estándar soportados por el framework Scotia QA.
 *
 * <p>Esta enumeración define todos los métodos HTTP que pueden ser utilizados
 * en las peticiones del cliente HTTP. Proporciona una forma type-safe de
 * especificar el método HTTP en lugar de usar strings directamente.
 *
 * <p>Métodos soportados:
 * <ul>
 *   <li>{@link #GET} - Para obtener recursos (idempotente, sin body)</li>
 *   <li>{@link #POST} - Para crear recursos (no idempotente, con body)</li>
 *   <li>{@link #PUT} - Para actualizar/crear recursos (idempotente, con body)</li>
 *   <li>{@link #DELETE} - Para eliminar recursos (idempotente, sin body)</li>
 *   <li>{@link #PATCH} - Para actualizaciones parciales (no idempotente, con body)</li>
 *   <li>{@link #HEAD} - Como GET pero solo headers (idempotente, sin body)</li>
 *   <li>{@link #OPTIONS} - Para obtener opciones del servidor (idempotente, sin body)</li>
 * </ul>
 *
 * <p>Ejemplo de uso:
 * <pre>
 * HttpClient client = new BaseHttpClient();
 * client.setHost("https://api.example.com");
 * HttpResponse response = client.executeRequest(HttpMethod.GET, "/users");
 * </pre>
 *
 * @author Scotia QA Framework
 * @since 1.0.0
 */
public enum HttpMethod {

    /**
     * Método GET - Utilizado para recuperar datos del servidor.
     * Es idempotente (múltiples llamadas tienen el mismo efecto).
     * Generalmente no incluye body en la petición.
     */
    GET,

    /**
     * Método POST - Utilizado para enviar datos al servidor y crear recursos.
     * No es idempotente (múltiples llamadas pueden tener efectos diferentes).
     * Típicamente incluye body con los datos a procesar.
     */
    POST,

    /**
     * Método PUT - Utilizado para actualizar o crear recursos en el servidor.
     * Es idempotente (múltiples llamadas tienen el mismo efecto).
     * Generalmente incluye body con todos los datos del recurso.
     */
    PUT,

    /**
     * Método DELETE - Utilizado para eliminar recursos del servidor.
     * Es idempotente (múltiples llamadas tienen el mismo efecto).
     * Generalmente no incluye body en la petición.
     */
    DELETE,

    /**
     * Método PATCH - Utilizado para actualizaciones parciales de recursos.
     * No es idempotente (múltiples llamadas pueden tener efectos diferentes).
     * Incluye body con solo los campos a actualizar.
     */
    PATCH,

    /**
     * Método HEAD - Similar a GET pero solo retorna headers, sin body.
     * Es idempotente (múltiples llamadas tienen el mismo efecto).
     * Útil para verificar metadatos sin descargar el contenido completo.
     */
    HEAD,

    /**
     * Método OPTIONS - Utilizado para obtener información sobre las opciones
     * de comunicación disponibles para el recurso de destino.
     * Es idempotente (múltiples llamadas tienen el mismo efecto).
     * Generalmente no incluye body en la petición.
     */
    OPTIONS;

    /**
     * Convierte el enum a su representación en string (uppercase).
     *
     * @return el nombre del método HTTP en mayúsculas
     */
    @Override
    public String toString() {
        return name().toUpperCase();
    }

    /**
     * Obtiene el HttpMethod a partir de un string, ignorando mayúsculas/minúsculas.
     *
     * @param method el string del método HTTP
     * @return el HttpMethod correspondiente
     * @throws IllegalArgumentException si el método no es válido
     */
    public static HttpMethod fromString(String method) {
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("El método HTTP no puede ser null o vacío");
        }

        try {
            return HttpMethod.valueOf(method.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Método HTTP no soportado: " + method +
                ". Métodos válidos: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS");
        }
    }

    /**
     * Verifica si el método HTTP típicamente permite body en la petición.
     *
     * @return true si el método permite body, false en caso contrario
     */
    public boolean allowsBody() {
        switch (this) {
            case POST:
            case PUT:
            case PATCH:
                return true;
            case GET:
            case DELETE:
            case HEAD:
            case OPTIONS:
                return false;
            default:
                return false;
        }
    }

    /**
     * Verifica si el método HTTP es idempotente.
     * Un método idempotente puede ser llamado múltiples veces sin efectos secundarios.
     *
     * @return true si el método es idempotente, false en caso contrario
     */
    public boolean isIdempotent() {
        switch (this) {
            case GET:
            case PUT:
            case DELETE:
            case HEAD:
            case OPTIONS:
                return true;
            case POST:
            case PATCH:
                return false;
            default:
                return false;
        }
    }

    /**
     * Verifica si el método HTTP es considerado "seguro" (no modifica el servidor).
     *
     * @return true si el método es seguro, false en caso contrario
     */
    public boolean isSafe() {
        switch (this) {
            case GET:
            case HEAD:
            case OPTIONS:
                return true;
            case POST:
            case PUT:
            case DELETE:
            case PATCH:
                return false;
            default:
                return false;
        }
    }
}
