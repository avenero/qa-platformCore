package com.qa.apicore.factories;

import com.qa.apicore.implementations.BaseHttpClient;
import com.qa.apicore.interfaces.HttpClient;
import com.qa.common.logging.TestLogger;

/**
 * Factory para crear instancias de HttpClient del QA Automation Framework.
 *
 * <p>Esta factory proporciona métodos estáticos para crear instancias de HttpClient de manera
 * estandarizada y extensible. Soporta tanto la implementación base como implementaciones
 * específicas por framework (API, Web, Mobile).
 *
 * <p><b>Características principales:</b>
 *
 * <ul>
 *   <li>Creación estandarizada de clientes HTTP
 *   <li>Soporte para extensiones específicas por framework
 *   <li>Logging integrado para troubleshooting
 *   <li>Patrón Factory para facilitar testing y mocking
 *   <li>Thread-safe para uso concurrente
 * </ul>
 *
 * <p><b>Uso típico:</b>
 *
 * <pre>
 * // Obtener implementación base
 * HttpClient client = HttpClientFactory.getInstance();
 * client.setHost("https://api.example.com");
 *
 * // Obtener implementación específica por framework
 * HttpClient apiClient = HttpClientFactory.getInstance("api");
 * HttpClient webClient = HttpClientFactory.getInstance("web");
 * HttpClient mobileClient = HttpClientFactory.getInstance("mobile");
 * </pre>
 *
 * <p><b>Extensión en frameworks específicos:</b>
 *
 * <pre>
 * // Los frameworks específicos pueden registrar sus implementaciones
 * // llamando a este factory desde sus propias factories
 * public class ApiHttpClientFactory {
 *     public static HttpClient getApiClient() {
 *         HttpClient client = HttpClientFactory.getInstance();
 *         // Configurar específicamente para APIs
 *         client.addHeader("Accept", "application/json");
 *         return client;
 *     }
 * }
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 2.0.0
 * @see HttpClient
 * @see BaseHttpClient
 */
public final class HttpClientFactory {

  private static final TestLogger.LoggerWrapper log = TestLogger.getLogger(HttpClientFactory.class);

  // Constructor privado para factory
  private HttpClientFactory() {
    throw new UnsupportedOperationException("HttpClientFactory es una clase factory");
  }

  /**
   * Crea una nueva instancia de HttpClient usando la implementación base. Esta es la forma más
   * común y recomendada de obtener un HttpClient.
   *
   * @return nueva instancia de HttpClient
   */
  public static HttpClient getInstance() {
    log.debug("Creando nueva instancia de BaseHttpClient");
    return new BaseHttpClient();
  }

  /**
   * Crea una instancia de HttpClient específica para un framework.
   *
   * <p><b>Nota:</b> Actualmente retorna BaseHttpClient para todos los tipos. Los frameworks
   * específicos (api-core, web-core, mobile-core) pueden extender esta funcionalidad creando sus
   * propias implementaciones.
   *
   * @param frameworkType tipo de framework ("api", "web", "mobile", o cualquier otro)
   * @return instancia de HttpClient (actualmente siempre BaseHttpClient)
   */
  public static HttpClient getInstance(String frameworkType) {
    if (frameworkType == null || frameworkType.trim().isEmpty()) {
      log.warn("Framework type es null o vacío, usando implementación base");
      return getInstance();
    }

    String normalizedType = frameworkType.toLowerCase().trim();
    log.debug("Creando HttpClient para framework: {}", normalizedType);

    // Por ahora retornamos la implementación base para todos los tipos
    // Los frameworks específicos pueden extender esto creando sus propias implementaciones
    switch (normalizedType) {
      case "api":
        log.debug("Retornando BaseHttpClient para framework API");
        return getInstance();
      case "web":
        log.debug("Retornando BaseHttpClient para framework Web");
        return getInstance();
      case "mobile":
        log.debug("Retornando BaseHttpClient para framework Mobile");
        return getInstance();
      default:
        log.debug("Framework type '{}' no reconocido, usando implementación base", frameworkType);
        return getInstance();
    }
  }

  /**
   * Crea una instancia de HttpClient preconfigurada con un host específico. Método de conveniencia
   * para casos de uso comunes.
   *
   * @param host la URL base del host (ej: "https://api.example.com")
   * @return nueva instancia de HttpClient configurada con el host
   * @throws IllegalArgumentException si host es null o vacío
   */
  public static HttpClient getInstanceWithHost(String host) {
    if (host == null || host.trim().isEmpty()) {
      throw new IllegalArgumentException("Host no puede ser null o vacío");
    }

    HttpClient client = getInstance();
    client.setHost(host.trim());

    log.debug("HttpClient creado y configurado con host: {}", host);
    return client;
  }

  /**
   * Crea una instancia de HttpClient preconfigurada para JSON. Configura automáticamente los
   * headers necesarios para trabajar con JSON.
   *
   * @return nueva instancia de HttpClient configurada para JSON
   */
  public static HttpClient getJsonInstance() {
    HttpClient client = getInstance();
    client.configureForJson();

    log.debug("HttpClient creado y configurado para JSON");
    return client;
  }

  /**
   * Crea una instancia de HttpClient preconfigurada para JSON con host específico. Combina la
   * configuración de JSON y host en un solo método.
   *
   * @param host la URL base del host
   * @return nueva instancia de HttpClient configurada para JSON con el host
   * @throws IllegalArgumentException si host es null o vacío
   */
  public static HttpClient getJsonInstanceWithHost(String host) {
    if (host == null || host.trim().isEmpty()) {
      throw new IllegalArgumentException("Host no puede ser null o vacío");
    }

    HttpClient client = getInstance();
    client.setHost(host.trim());
    client.configureForJson();

    log.debug("HttpClient creado y configurado para JSON con host: {}", host);
    return client;
  }

  /**
   * Obtiene información de debug sobre las capacidades del factory. Útil para troubleshooting y
   * verificación de configuración.
   *
   * @return string con información del factory
   */
  public static String getFactoryInfo() {
    return String.format(
        "HttpClientFactory v1.0.0 - Implementación base: %s - Frameworks soportados: api, web, mobile",
        BaseHttpClient.class.getSimpleName());
  }
}
