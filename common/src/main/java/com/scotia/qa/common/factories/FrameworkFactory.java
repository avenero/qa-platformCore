package com.scotia.qa.common.factories;

import com.scotia.qa.common.interfaces.HttpClient;
import com.scotia.qa.common.interfaces.ConfigurationService;
import com.scotia.qa.common.interfaces.AuthenticationService;
import com.scotia.qa.common.interfaces.DatabaseService;
import com.scotia.qa.common.interfaces.CucumberHooksService;
import com.scotia.qa.common.interfaces.ConfigurationProvider;
import com.scotia.qa.common.implementations.BaseConfigurationService;
import com.scotia.qa.common.logging.TestLogger;

/**
 * Factory principal del framework Scotia QA que coordina la creación de todos los servicios.
 *
 * <p>Esta factory actúa como punto de entrada unificado para crear instancias de todos
 * los servicios principales del framework de manera coordinada y consistente.
 * Proporciona métodos de conveniencia para configuraciones típicas y casos de uso comunes.
 *
 * <p><b>Características principales:</b>
 * <ul>
 *   <li>Punto de entrada único para todos los servicios del framework</li>
 *   <li>Configuraciones predefinidas para casos de uso comunes</li>
 *   <li>Creación coordinada de servicios interrelacionados</li>
 *   <li>Preconfiguración automática según el tipo de framework</li>
 *   <li>Logging unificado para troubleshooting</li>
 * </ul>
 *
 * <p><b>Servicios disponibles:</b>
 * <ul>
 *   <li><b>HttpClient</b> - Para comunicaciones HTTP</li>
 *   <li><b>AuthenticationService</b> - Para gestión de autenticación</li>
 *   <li><b>DatabaseService</b> - Para operaciones de base de datos</li>
 *   <li><b>ConfigurationService</b> - Para gestión de configuraciones</li>
 * </ul>
 *
 * <p><b>Uso típico básico:</b>
 * <pre>
 * // Crear servicios individuales
 * HttpClient httpClient = FrameworkFactory.createHttpClient();
 * AuthenticationService authService = FrameworkFactory.createAuthenticationService();
 * DatabaseService dbService = FrameworkFactory.createDatabaseService("oracle");
 * ConfigurationService configService = FrameworkFactory.createConfigurationService();
 *
 * // Crear kit completo para API testing
 * FrameworkComponents apiKit = FrameworkFactory.createApiTestingKit("oracle");
 * HttpClient client = apiKit.getHttpClient();
 * AuthenticationService auth = apiKit.getAuthenticationService();
 * DatabaseService db = apiKit.getDatabaseService();
 * ConfigurationService config = apiKit.getConfigurationService();
 * </pre>
 *
 * <p><b>Configuraciones por framework:</b>
 * <ul>
 *   <li><b>API Testing</b> - HttpClient + AuthService + DatabaseService + ConfigService</li>
 *   <li><b>Web Testing</b> - HttpClient configurado para web + AuthService</li>
 *   <li><b>Mobile Testing</b> - HttpClient optimizado + AuthService simplificado</li>
 * </ul>
 *
 * @author Scotia QA Framework Team
 * @version 1.0.0
 * @since 2.0.0
 * @see HttpClientFactory
 * @see AuthenticationServiceFactory
 * @see DatabaseServiceFactory
 */
public final class FrameworkFactory {

    private static final TestLogger.LoggerWrapper log = TestLogger.getLogger(FrameworkFactory.class);

    // Constructor privado para factory
    private FrameworkFactory() {
        throw new UnsupportedOperationException("FrameworkFactory es una clase factory");
    }

    // =================================================================================
    // CREACIÓN DE SERVICIOS INDIVIDUALES
    // =================================================================================

    /**
     * Crea una instancia de HttpClient usando configuración por defecto.
     * Delegado a HttpClientFactory para mantener separación de responsabilidades.
     *
     * @return nueva instancia de HttpClient
     */
    public static HttpClient createHttpClient() {
        log.debug("Creando HttpClient a través de FrameworkFactory");
        return HttpClientFactory.getInstance();
    }

    /**
     * Crea una instancia de HttpClient preconfigurada para JSON.
     *
     * @return HttpClient configurado para JSON
     */
    public static HttpClient createJsonHttpClient() {
        log.debug("Creando HttpClient para JSON a través de FrameworkFactory");
        return HttpClientFactory.getJsonInstance();
    }

    /**
     * Crea una instancia de AuthenticationService usando configuración por defecto.
     *
     * @return nueva instancia de AuthenticationService
     */
    public static AuthenticationService createAuthenticationService() {
        log.debug("Creando AuthenticationService a través de FrameworkFactory");
        return AuthenticationServiceFactory.getInstance();
    }

    /**
     * Crea una instancia de DatabaseService para el tipo de BD especificado.
     *
     * @param databaseType tipo de base de datos ("oracle", "sqlserver", etc.)
     * @return DatabaseService configurado para el tipo especificado
     */
    public static DatabaseService createDatabaseService(String databaseType) {
        log.debug("Creando DatabaseService para {} a través de FrameworkFactory", databaseType);
        return DatabaseServiceFactory.getInstance(databaseType);
    }

    /**
     * Crea una instancia de ConfigurationService usando configuración por defecto.
     *
     * @return nueva instancia de ConfigurationService
     */
    public static ConfigurationService createConfigurationService() {
        log.debug("Creando ConfigurationService a través de FrameworkFactory");
        return new BaseConfigurationService();
    }

    /**
     * Crea una instancia de ConfigurationService preconfigurada para un entorno específico.
     *
     * @param environment entorno a configurar ("dev", "test", "staging", "prod")
     * @return ConfigurationService configurado para el entorno especificado
     */
    public static ConfigurationService createConfigurationService(String environment) {
        log.debug("Creando ConfigurationService para entorno: {}", environment);
        BaseConfigurationService configService = new BaseConfigurationService();
        configService.setEnvironment(environment);
        return configService;
    }

    /**
     * Crea una instancia de CucumberHooksService para el framework especificado.
     *
     * @param frameworkType tipo de framework ("API", "WEB", "MOBILE")
     * @return CucumberHooksService configurado para el framework especificado
     */
    public static CucumberHooksService createCucumberHooksService(String frameworkType) {
        log.debug("Creando CucumberHooksService para framework: {}", frameworkType);
        return CucumberServiceFactory.getInstance(frameworkType);
    }

    /**
     * Crea una instancia de CucumberHooksService preconfigurada para API testing.
     *
     * @return CucumberHooksService optimizado para API testing
     */
    public static CucumberHooksService createApiCucumberHooksService() {
        log.debug("Creando CucumberHooksService para API testing a través de FrameworkFactory");
        return CucumberServiceFactory.getApiInstance();
    }

    /**
     * Crea una instancia de ConfigurationProvider usando configuración por defecto.
     *
     * @return nueva instancia de ConfigurationProvider
     */
    public static ConfigurationProvider createConfigurationProvider() {
        log.debug("Creando ConfigurationProvider a través de FrameworkFactory");
        return ConfigurationProviderFactory.getInstance();
    }

    /**
     * Crea una instancia de ConfigurationProvider con cache habilitado.
     *
     * @return ConfigurationProvider con cache habilitado para mejor rendimiento
     */
    public static ConfigurationProvider createCachedConfigurationProvider() {
        log.debug("Creando ConfigurationProvider con cache a través de FrameworkFactory");
        return ConfigurationProviderFactory.getCachedInstance();
    }

    /**
     * Crea una instancia de ConfigurationProvider específica para un framework.
     *
     * @param frameworkName nombre del framework ("api", "web", "mobile")
     * @return ConfigurationProvider optimizado para el framework especificado
     */
    public static ConfigurationProvider createFrameworkConfigurationProvider(String frameworkName) {
        log.debug("Creando ConfigurationProvider para framework: {}", frameworkName);
        return ConfigurationProviderFactory.getFrameworkInstance(frameworkName);
    }

    // =================================================================================
    // CONFIGURACIONES COORDINADAS POR FRAMEWORK
    // =================================================================================

    /**
     * Crea un kit completo de componentes para API testing.
     * Incluye HttpClient, AuthenticationService y DatabaseService preconfigurados.
     *
     * @param databaseType tipo de base de datos a utilizar
     * @return FrameworkComponents configurados para API testing
     */
    public static FrameworkComponents createApiTestingKit(String databaseType) {
        log.debug("Creando kit completo para API testing con BD: {}", databaseType);

        HttpClient httpClient = HttpClientFactory.getJsonInstance();
        AuthenticationService authService = AuthenticationServiceFactory.getInstance(httpClient);
        DatabaseService databaseService = DatabaseServiceFactory.getInstance(databaseType);

        log.debug("Kit de API testing creado exitosamente");
        return new FrameworkComponents(httpClient, authService, databaseService);
    }

    /**
     * Crea un kit completo de componentes para web testing.
     * Incluye configuraciones optimizadas para testing de aplicaciones web.
     *
     * @return FrameworkComponents configurados para web testing
     */
    public static FrameworkComponents createWebTestingKit() {
        log.debug("Creando kit completo para web testing");

        HttpClient httpClient = HttpClientFactory.getInstance();
        // Configurar headers típicos de web
        httpClient.addHeader("User-Agent", "Scotia-QA-Framework-Web/1.0");
        httpClient.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

        AuthenticationService authService = AuthenticationServiceFactory.getInstance(httpClient);

        log.debug("Kit de web testing creado exitosamente");
        return new FrameworkComponents(httpClient, authService, null);
    }

    /**
     * Crea un kit completo de componentes para mobile testing.
     * Incluye configuraciones optimizadas para testing de aplicaciones móviles.
     *
     * @return FrameworkComponents configurados para mobile testing
     */
    public static FrameworkComponents createMobileTestingKit() {
        log.debug("Creando kit completo para mobile testing");

        HttpClient httpClient = HttpClientFactory.getInstance();
        // Configurar headers típicos de mobile
        httpClient.addHeader("User-Agent", "Scotia-QA-Framework-Mobile/1.0");
        httpClient.addHeader("Accept", "application/json");

        AuthenticationService authService = AuthenticationServiceFactory.getInstance(httpClient);

        log.debug("Kit de mobile testing creado exitosamente");
        return new FrameworkComponents(httpClient, authService, null);
    }

    /**
     * Crea una configuración personalizada de componentes del framework.
     * Permite especificar exactamente qué componentes incluir y cómo configurarlos.
     *
     * @param config configuración personalizada
     * @return FrameworkComponents según la configuración especificada
     */
    public static FrameworkComponents createCustomKit(FrameworkConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("FrameworkConfig no puede ser null");
        }

        log.debug("Creando kit personalizado según configuración");

        HttpClient httpClient = null;
        AuthenticationService authService = null;
        DatabaseService databaseService = null;

        // Crear HttpClient si está solicitado
        if (config.isHttpClientEnabled()) {
            if (config.isJsonMode()) {
                httpClient = HttpClientFactory.getJsonInstance();
            } else {
                httpClient = HttpClientFactory.getInstance();
            }

            // Configurar host si está especificado
            if (config.getBaseUrl() != null) {
                httpClient.setHost(config.getBaseUrl());
            }
        }

        // Crear AuthenticationService si está solicitado
        if (config.isAuthenticationEnabled()) {
            if (httpClient != null) {
                authService = AuthenticationServiceFactory.getInstance(httpClient);
            } else {
                authService = AuthenticationServiceFactory.getInstance();
            }
        }

        // Crear DatabaseService si está solicitado
        if (config.isDatabaseEnabled() && config.getDatabaseType() != null) {
            databaseService = DatabaseServiceFactory.getInstance(config.getDatabaseType());
        }

        log.debug("Kit personalizado creado exitosamente");
        return new FrameworkComponents(httpClient, authService, databaseService);
    }

    // =================================================================================
    // MÉTODOS DE UTILIDAD Y INFORMACIÓN
    // =================================================================================

    /**
     * Prueba la conectividad de todos los servicios configurados.
     * Útil para validar que el entorno está correctamente configurado.
     *
     * @param components componentes a probar
     * @return true si todos los servicios responden correctamente
     */
    public static boolean testAllConnections(FrameworkComponents components) {
        log.debug("Probando conectividad de todos los servicios");

        boolean allGood = true;

        // Probar HttpClient (si está disponible)
        if (components.getHttpClient() != null) {
            try {
                // Test básico: verificar que el cliente esté bien configurado
                String debugInfo = components.getHttpClient().getDebugInfo();
                log.debug("HttpClient status: {}", debugInfo);
            } catch (Exception e) {
                log.warn("Error testing HttpClient: {}", e.getMessage());
                allGood = false;
            }
        }

        // Probar DatabaseService (si está disponible)
        if (components.getDatabaseService() != null) {
            try {
                boolean dbConnected = components.getDatabaseService().testConnection();
                log.debug("DatabaseService connection: {}", dbConnected ? "OK" : "FAILED");
                allGood &= dbConnected;
            } catch (Exception e) {
                log.warn("Error testing DatabaseService: {}", e.getMessage());
                allGood = false;
            }
        }

        log.debug("Test de conectividad completado: {}", allGood ? "EXITOSO" : "FALLIDO");
        return allGood;
    }

    /**
     * Obtiene información completa sobre las capacidades del framework.
     *
     * @return string con información detallada del framework
     */
    public static String getFrameworkInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Scotia QA Framework v2.0.0\n");
        info.append("=========================\n");
        info.append("Factories disponibles:\n");
        info.append("- ").append(HttpClientFactory.getFactoryInfo()).append("\n");
        info.append("- ").append(AuthenticationServiceFactory.getFactoryInfo()).append("\n");
        info.append("- ").append(DatabaseServiceFactory.getFactoryInfo()).append("\n");
        info.append("- ").append(CucumberServiceFactory.getFactoryInfo()).append("\n");
        info.append("- ").append(ConfigurationProviderFactory.getFactoryInfo()).append("\n");
        info.append("- BaseConfigurationService v1.0.0 - Multi-format configuration reader").append("\n");
        info.append("- ConfigurationUtilities v1.0.0 - Technical layer for file reading").append("\n");
        info.append("\nConfigurations soportadas: API Testing, Web Testing, Mobile Testing, Custom");

        return info.toString();
    }

    // =================================================================================
    // CLASES INTERNAS PARA CONFIGURACIÓN Y COMPONENTES
    // =================================================================================

    /**
     * Contenedor para todos los componentes del framework.
     * Facilita el manejo coordinado de múltiples servicios.
     */
    public static class FrameworkComponents {
        private final HttpClient httpClient;
        private final AuthenticationService authenticationService;
        private final DatabaseService databaseService;

        public FrameworkComponents(HttpClient httpClient,
                                 AuthenticationService authenticationService,
                                 DatabaseService databaseService) {
            this.httpClient = httpClient;
            this.authenticationService = authenticationService;
            this.databaseService = databaseService;
        }

        public HttpClient getHttpClient() { return httpClient; }
        public AuthenticationService getAuthenticationService() { return authenticationService; }
        public DatabaseService getDatabaseService() { return databaseService; }

        /**
         * Limpia todos los recursos de los componentes.
         */
        public void cleanup() {
            if (httpClient != null) {
                httpClient.reset();
            }
            if (databaseService != null) {
                databaseService.cleanup();
            }
            // AuthenticationService se limpia automáticamente
        }
    }

    /**
     * Configuración para crear kits personalizados del framework.
     */
    public static class FrameworkConfig {
        private boolean httpClientEnabled = true;
        private boolean authenticationEnabled = true;
        private boolean databaseEnabled = false;
        private boolean jsonMode = true;
        private String baseUrl;
        private String databaseType;

        public FrameworkConfig() {}

        // Builder methods
        public FrameworkConfig withHttpClient(boolean enabled) {
            this.httpClientEnabled = enabled;
            return this;
        }

        public FrameworkConfig withAuthentication(boolean enabled) {
            this.authenticationEnabled = enabled;
            return this;
        }

        public FrameworkConfig withDatabase(String databaseType) {
            this.databaseEnabled = (databaseType != null);
            this.databaseType = databaseType;
            return this;
        }

        public FrameworkConfig withJsonMode(boolean jsonMode) {
            this.jsonMode = jsonMode;
            return this;
        }

        public FrameworkConfig withBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        // Getters
        public boolean isHttpClientEnabled() { return httpClientEnabled; }
        public boolean isAuthenticationEnabled() { return authenticationEnabled; }
        public boolean isDatabaseEnabled() { return databaseEnabled; }
        public boolean isJsonMode() { return jsonMode; }
        public String getBaseUrl() { return baseUrl; }
        public String getDatabaseType() { return databaseType; }
    }
}
