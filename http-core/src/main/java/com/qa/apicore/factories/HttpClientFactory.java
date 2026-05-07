package com.qa.apicore.factories;

import com.qa.apicore.implementations.ApacheHttpClientImpl;
import com.qa.apicore.implementations.BaseHttpClient;
import com.qa.apicore.interfaces.HttpClient;
import com.qa.common.logging.TestLogger;

/**
 * Factory para crear instancias de {@link HttpClient}.
 *
 * <h2>Implementación por defecto — v2.2.0</h2>
 * <p>La implementación por defecto es {@link ApacheHttpClientImpl} (Apache HttpClient 5).
 * La selección puede sobreescribirse con la propiedad de sistema o de config
 * {@code http.client}:
 * <ul>
 *   <li>{@code apache} (default) — {@link ApacheHttpClientImpl}</li>
 *   <li>{@code unirest} (deprecated) — {@link BaseHttpClient}</li>
 * </ul>
 *
 * <h2>Uso típico</h2>
 * <pre>
 * HttpClient client = HttpClientFactory.getInstance();
 * client.setHost("https://api.example.com");
 * HttpResponse r = client.get("/users");
 * </pre>
 *
 * @author Abel Venero
 * @version 2.2.0
 * @since 2.0.0
 * @see HttpClient
 * @see ApacheHttpClientImpl
 */
public final class HttpClientFactory {

  /** Propiedad de config/sistema para seleccionar implementación (default: {@code apache}). */
  public static final String CLIENT_IMPL_KEY     = "http.client";
  public static final String IMPL_APACHE         = "apache";
  /** @deprecated Unirest está deprecado; usa {@code apache}. */
  @Deprecated(since = "2.2.0", forRemoval = true)
  public static final String IMPL_UNIREST        = "unirest";

  private static final TestLogger.LoggerWrapper LOG = TestLogger.getLogger(HttpClientFactory.class);

  private HttpClientFactory() {
    throw new UnsupportedOperationException("HttpClientFactory es una clase factory");
  }

  // =========================================================================
  // Métodos principales
  // =========================================================================

  /**
   * Crea una nueva instancia de {@link HttpClient} usando la implementación configurada
   * en la propiedad {@code http.client} (default: {@code apache}).
   *
   * @return nueva instancia de HttpClient
   */
  public static HttpClient getInstance() {
    String impl = resolveImpl();
    return createForImpl(impl);
  }

  /**
   * Crea una instancia de {@link HttpClient} específica para un framework.
   * Todos los tipos de framework usan la misma implementación seleccionada por {@code http.client}.
   *
   * @param frameworkType tipo de framework ("api", "web", "mobile", o cualquier otro)
   * @return nueva instancia de HttpClient
   */
  public static HttpClient getInstance(String frameworkType) {
    if (frameworkType == null || frameworkType.isBlank()) {
      LOG.warn("frameworkType es null o vacío, usando implementación por defecto");
      return getInstance();
    }
    LOG.debug("Creando HttpClient para framework: {}", frameworkType.toLowerCase().trim());
    return getInstance();
  }

  /**
   * Crea una instancia de {@link HttpClient} preconfigurada con host.
   *
   * @param host URL base (ej: {@code https://api.example.com})
   * @return instancia configurada
   * @throws IllegalArgumentException si host es null o vacío
   */
  public static HttpClient getInstanceWithHost(String host) {
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("Host no puede ser null o vacío");
    }
    HttpClient client = getInstance();
    client.setHost(host.trim());
    LOG.debug("HttpClient configurado con host: {}", host);
    return client;
  }

  /**
   * Crea una instancia de {@link HttpClient} preconfigurada para JSON.
   *
   * @return instancia configurada para JSON
   */
  public static HttpClient getJsonInstance() {
    HttpClient client = getInstance();
    client.configureForJson();
    LOG.debug("HttpClient configurado para JSON");
    return client;
  }

  /**
   * Crea una instancia de {@link HttpClient} preconfigurada para JSON con host.
   *
   * @param host URL base
   * @return instancia configurada para JSON con el host
   * @throws IllegalArgumentException si host es null o vacío
   */
  public static HttpClient getJsonInstanceWithHost(String host) {
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("Host no puede ser null o vacío");
    }
    HttpClient client = getInstance();
    client.setHost(host.trim());
    client.configureForJson();
    LOG.debug("HttpClient configurado para JSON con host: {}", host);
    return client;
  }

  /** @return información de debug del factory */
  public static String getFactoryInfo() {
    String impl = resolveImpl();
    return String.format(
        "HttpClientFactory v2.2.0 — implementación activa: %s (configurable vía '%s'). " +
        "Opciones: apache (default), unirest (deprecated).",
        impl, CLIENT_IMPL_KEY);
  }

  // =========================================================================
  // Privados
  // =========================================================================

  /**
   * Resuelve la implementación a usar: primero system property, luego config property.
   * Default: {@code apache}.
   */
  private static String resolveImpl() {
    String impl = System.getProperty(CLIENT_IMPL_KEY);
    if (impl == null || impl.isBlank()) {
      // Fallback a config gestionada por ConfigManager si está disponible en contexto
      try {
        impl = com.qa.common.config.ConfigManager.getInstance().get(CLIENT_IMPL_KEY, IMPL_APACHE);
      } catch (Exception e) {
        impl = IMPL_APACHE;
      }
    }
    return impl.trim().toLowerCase();
  }

  private static HttpClient createForImpl(String impl) {
    return switch (impl) {
      case IMPL_UNIREST -> {
        LOG.warn("Usando BaseHttpClient (Unirest) que está @Deprecated(forRemoval=true). " +
                 "Migra a la implementación Apache: -D{}={}", CLIENT_IMPL_KEY, IMPL_APACHE);
        yield new BaseHttpClient();
      }
      default -> {
        if (!IMPL_APACHE.equals(impl)) {
          LOG.warn("Implementación '{}' no reconocida, usando Apache HttpClient 5 por defecto", impl);
        }
        LOG.debug("Creando ApacheHttpClientImpl");
        yield new ApacheHttpClientImpl();
      }
    };
  }
}
