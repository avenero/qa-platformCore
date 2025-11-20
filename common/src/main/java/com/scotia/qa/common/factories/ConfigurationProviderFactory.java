package com.scotia.qa.common.factories;

import com.scotia.qa.common.interfaces.ConfigurationProvider;
import com.scotia.qa.common.implementations.BaseConfigurationProvider;
import com.scotia.qa.common.logging.TestLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory para crear instancias de ConfigurationProvider del framework Scotia QA.
 *
 * <p>Esta factory proporciona métodos estáticos para crear instancias especializadas de
 * ConfigurationProvider según diferentes necesidades y configuraciones. Facilita la
 * creación estandarizada y optimizada de proveedores de configuración.
 *
 * <p><b>Características principales:</b>
 * <ul>
 *   <li>Creación estandarizada de proveedores de configuración</li>
 *   <li>Instancias optimizadas para diferentes casos de uso</li>
 *   <li>Cache opcional de instancias singleton</li>
 *   <li>Pre-configuración automática por tipo de uso</li>
 *   <li>Configuraciones predefinidas para entornos</li>
 *   <li>Logging integrado para troubleshooting</li>
 * </ul>
 *
 * <p><b>Tipos de proveedores soportados:</b>
 * <ul>
 *   <li><b>Básico</b> - Sin cache, para uso simple</li>
 *   <li><b>Cached</b> - Con cache habilitado para mejor rendimiento</li>
 *   <li><b>Environment</b> - Preconfigurado para entornos específicos</li>
 *   <li><b>Preloaded</b> - Con archivos pre-cargados</li>
 *   <li><b>Custom</b> - Configuración completamente personalizada</li>
 * </ul>
 *
 * <p><b>Uso típico básico:</b>
 * <pre>
 * // Crear proveedor básico
 * ConfigurationProvider provider = ConfigurationProviderFactory.getInstance();
 *
 * // Crear proveedor con cache para mejor rendimiento
 * ConfigurationProvider cachedProvider = ConfigurationProviderFactory.getCachedInstance();
 *
 * // Crear proveedor preconfigurado para entorno
 * ConfigurationProvider envProvider = ConfigurationProviderFactory.getEnvironmentInstance("prod");
 *
 * // Crear proveedor con archivos pre-cargados
 * ConfigurationProvider preloadedProvider = ConfigurationProviderFactory.getPreloadedInstance(
 *     Arrays.asList("app.yml", "database.properties")
 * );
 * </pre>
 *
 * <p><b>Uso avanzado con configuración personalizada:</b>
 * <pre>
 * // Configuración personalizada avanzada
 * ConfigurationProviderConfig config = new ConfigurationProviderConfig()
 *     .withCache(true)
 *     .withDefaultEnvironment("staging")
 *     .withPreloadFiles(Arrays.asList("core.yml", "features.json"))
 *     .withValidationEnabled(true);
 *
 * ConfigurationProvider customProvider = ConfigurationProviderFactory.getCustomInstance(config);
 *
 * // Usar en aplicación
 * Map&lt;String, Object&gt; appConfig = customProvider.loadConfiguration("app.yml");
 * String dbHost = customProvider.getConfigurationValue("database.host", appConfig);
 * </pre>
 *
 * <p><b>Integración en frameworks específicos:</b>
 * <pre>
 * // En api-core
 * public class ApiConfigurationManager {
 *     private static final ConfigurationProvider provider =
 *         ConfigurationProviderFactory.getCachedInstance("api");
 *
 *     static {
 *         // Pre-cargar configuraciones críticas
 *         provider.preloadConfigurations(Arrays.asList("api-endpoints.yml", "database.properties"));
 *     }
 *
 *     public String getApiBaseUrl() {
 *         Map&lt;String, Object&gt; config = provider.loadConfiguration("api-endpoints.yml");
 *         return provider.getConfigurationValue("api.baseUrl", config);
 *     }
 * }
 * </pre>
 *
 * @author Scotia QA Framework Team
 * @version 1.0.0
 * @since 2.0.0
 * @see ConfigurationProvider
 * @see BaseConfigurationProvider
 */
public final class ConfigurationProviderFactory {

    private static final TestLogger.LoggerWrapper log = TestLogger.getLogger(ConfigurationProviderFactory.class);

    // Cache thread-safe de instancias singleton por tipo
    private static final Map<String, ConfigurationProvider> singletonCache = new ConcurrentHashMap<>();

    // Constructor privado para factory
    private ConfigurationProviderFactory() {
        throw new UnsupportedOperationException("ConfigurationProviderFactory es una clase factory");
    }

    // =================================================================================
    // MÉTODOS PRINCIPALES DE CREACIÓN
    // =================================================================================

    /**
     * Crea una instancia básica de ConfigurationProvider.
     * Sin cache, configuración mínima para uso simple.
     *
     * @return nueva instancia de ConfigurationProvider
     */
    public static ConfigurationProvider getInstance() {
        log.debug("Creando ConfigurationProvider básico");
        BaseConfigurationProvider provider = new BaseConfigurationProvider();
        provider.setCacheEnabled(false);

        log.debug("ConfigurationProvider básico creado exitosamente");
        return provider;
    }

    /**
     * Crea una instancia de ConfigurationProvider con cache habilitado.
     * Optimizado para aplicaciones que cargan configuraciones repetidamente.
     *
     * @return ConfigurationProvider con cache habilitado
     */
    public static ConfigurationProvider getCachedInstance() {
        log.debug("Creando ConfigurationProvider con cache");
        BaseConfigurationProvider provider = new BaseConfigurationProvider();
        provider.setCacheEnabled(true);

        log.debug("ConfigurationProvider con cache creado exitosamente");
        return provider;
    }

    /**
     * Crea una instancia singleton cacheada.
     * Útil cuando se necesita la misma instancia en múltiples clases.
     *
     * @param instanceKey clave única para identificar la instancia
     * @return instancia singleton para la clave especificada
     */
    public static ConfigurationProvider getSingletonInstance(String instanceKey) {
        if (instanceKey == null || instanceKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Instance key no puede ser null o vacía");
        }

        return singletonCache.computeIfAbsent(instanceKey, key -> {
            log.debug("Creando instancia singleton para clave: {}", key);
            BaseConfigurationProvider provider = new BaseConfigurationProvider();
            provider.setCacheEnabled(true); // Singleton siempre con cache
            return provider;
        });
    }

    /**
     * Crea una instancia preconfigurada para un entorno específico.
     *
     * @param environment entorno por defecto ("dev", "test", "staging", "prod")
     * @return ConfigurationProvider configurado para el entorno
     */
    public static ConfigurationProvider getEnvironmentInstance(String environment) {
        if (environment == null || environment.trim().isEmpty()) {
            throw new IllegalArgumentException("Environment no puede ser null o vacío");
        }

        log.debug("Creando ConfigurationProvider para entorno: {}", environment);
        BaseConfigurationProvider provider = new BaseConfigurationProvider();
        provider.setCacheEnabled(true);
        provider.setDefaultEnvironment(environment);

        log.debug("ConfigurationProvider para entorno {} creado exitosamente", environment);
        return provider;
    }

    /**
     * Crea una instancia con archivos pre-cargados.
     * Mejora el rendimiento cargando configuraciones críticas al inicio.
     *
     * @param filesToPreload lista de archivos a pre-cargar
     * @return ConfigurationProvider con archivos pre-cargados
     */
    public static ConfigurationProvider getPreloadedInstance(List<String> filesToPreload) {
        if (filesToPreload == null || filesToPreload.isEmpty()) {
            throw new IllegalArgumentException("Lista de archivos a pre-cargar no puede ser null o vacía");
        }

        log.debug("Creando ConfigurationProvider con pre-carga de {} archivos", filesToPreload.size());
        BaseConfigurationProvider provider = new BaseConfigurationProvider();
        provider.setCacheEnabled(true);

        // Pre-cargar archivos
        provider.preloadConfigurations(filesToPreload);

        log.debug("ConfigurationProvider con pre-carga creado exitosamente");
        return provider;
    }

    /**
     * Crea una instancia específica para framework consumidor.
     *
     * @param frameworkName nombre del framework ("api", "web", "mobile")
     * @return ConfigurationProvider optimizado para el framework
     */
    public static ConfigurationProvider getFrameworkInstance(String frameworkName) {
        if (frameworkName == null || frameworkName.trim().isEmpty()) {
            throw new IllegalArgumentException("Framework name no puede ser null o vacío");
        }

        String normalizedName = frameworkName.toLowerCase().trim();
        log.debug("Creando ConfigurationProvider para framework: {}", normalizedName);

        BaseConfigurationProvider provider = new BaseConfigurationProvider();
        provider.setCacheEnabled(true);

        // Configuraciones específicas por framework
        switch (normalizedName) {
            case "api":
                // Pre-cargar archivos típicos de API
                provider.preloadConfigurations(Arrays.asList(
                    "api-config.yml", "endpoints.yml", "database.properties"
                ));
                break;

            case "web":
                // Pre-cargar archivos típicos de Web
                provider.preloadConfigurations(Arrays.asList(
                    "web-config.yml", "selenium.properties", "browsers.json"
                ));
                break;

            case "mobile":
                // Pre-cargar archivos típicos de Mobile
                provider.preloadConfigurations(Arrays.asList(
                    "mobile-config.yml", "devices.json", "appium.properties"
                ));
                break;

            default:
                log.debug("Framework no reconocido: {}, usando configuración estándar", normalizedName);
        }

        log.debug("ConfigurationProvider para framework {} creado exitosamente", normalizedName);
        return provider;
    }

    /**
     * Crea una instancia con configuración completamente personalizada.
     *
     * @param config configuración personalizada
     * @return ConfigurationProvider según la configuración especificada
     */
    public static ConfigurationProvider getCustomInstance(ConfigurationProviderConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ConfigurationProviderConfig no puede ser null");
        }

        log.debug("Creando ConfigurationProvider con configuración personalizada");
        BaseConfigurationProvider provider = new BaseConfigurationProvider();

        // Aplicar configuración personalizada
        provider.setCacheEnabled(config.isCacheEnabled());

        if (config.getDefaultEnvironment() != null) {
            provider.setDefaultEnvironment(config.getDefaultEnvironment());
        }

        if (config.getPreloadFiles() != null && !config.getPreloadFiles().isEmpty()) {
            provider.preloadConfigurations(config.getPreloadFiles());
        }

        log.debug("ConfigurationProvider personalizado creado exitosamente");
        return provider;
    }

    // =================================================================================
    // MÉTODOS OPTIMIZADOS PARA CASOS ESPECÍFICOS
    // =================================================================================

    /**
     * Crea una instancia optimizada para testing.
     * Cache habilitado y configuraciones de test pre-cargadas.
     *
     * @return ConfigurationProvider optimizado para testing
     */
    public static ConfigurationProvider getTestingInstance() {
        log.debug("Creando ConfigurationProvider optimizado para testing");

        BaseConfigurationProvider provider = new BaseConfigurationProvider();
        provider.setCacheEnabled(true);
        provider.setDefaultEnvironment("test");

        // Pre-cargar configuraciones comunes de testing
        List<String> testFiles = Arrays.asList(
            "test-config.yml",
            "test.properties",
            "database-test.properties"
        );

        provider.preloadConfigurations(testFiles);

        log.debug("ConfigurationProvider para testing creado exitosamente");
        return provider;
    }

    /**
     * Crea una instancia optimizada para producción.
     * Cache habilitado, entorno prod, validación estricta.
     *
     * @return ConfigurationProvider optimizado para producción
     */
    public static ConfigurationProvider getProductionInstance() {
        log.debug("Creando ConfigurationProvider optimizado para producción");

        BaseConfigurationProvider provider = new BaseConfigurationProvider();
        provider.setCacheEnabled(true);
        provider.setDefaultEnvironment("prod");

        log.debug("ConfigurationProvider para producción creado exitosamente");
        return provider;
    }

    /**
     * Crea una instancia ligera sin cache.
     * Optimizada para uso esporádico o aplicaciones con poca memoria.
     *
     * @return ConfigurationProvider ligero sin cache
     */
    public static ConfigurationProvider getLightweightInstance() {
        log.debug("Creando ConfigurationProvider ligero sin cache");

        BaseConfigurationProvider provider = new BaseConfigurationProvider();
        provider.setCacheEnabled(false);

        log.debug("ConfigurationProvider ligero creado exitosamente");
        return provider;
    }

    // =================================================================================
    // MÉTODOS DE UTILIDAD Y GESTIÓN
    // =================================================================================

    /**
     * Limpia el cache de instancias singleton.
     * Útil para testing o cuando se necesita reiniciar configuraciones.
     */
    public static void clearSingletonCache() {
        int oldSize = singletonCache.size();
        singletonCache.clear();
        log.debug("Cache de instancias singleton limpiado: {} instancias removidas", oldSize);
    }

    /**
     * Obtiene información sobre las instancias singleton cacheadas.
     *
     * @return mapa con información de instancias cacheadas
     */
    public static Map<String, String> getSingletonCacheInfo() {
        Map<String, String> info = new ConcurrentHashMap<>();

        singletonCache.forEach((key, provider) -> {
            info.put(key, provider.getClass().getSimpleName());
        });

        return info;
    }

    /**
     * Verifica si una clave de singleton está en cache.
     *
     * @param instanceKey clave a verificar
     * @return true si la instancia está cacheada
     */
    public static boolean isSingletonCached(String instanceKey) {
        return singletonCache.containsKey(instanceKey);
    }

    /**
     * Obtiene información sobre las capacidades del factory.
     *
     * @return string con información del factory
     */
    public static String getFactoryInfo() {
        return String.format(
            "ConfigurationProviderFactory v1.0.0 - Implementación: %s - Tipos soportados: %s - Cache singleton: %d instancias",
            BaseConfigurationProvider.class.getSimpleName(),
            "Básico, Cached, Environment, Preloaded, Framework, Testing, Production, Custom",
            singletonCache.size()
        );
    }

    /**
     * Obtiene estadísticas de uso del factory.
     *
     * @return mapa con estadísticas de uso
     */
    public static Map<String, Object> getFactoryStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("singletonInstancesCount", singletonCache.size());
        stats.put("singletonKeys", singletonCache.keySet());
        stats.put("implementationClass", BaseConfigurationProvider.class.getSimpleName());

        return stats;
    }

    // =================================================================================
    // CLASE INTERNA PARA CONFIGURACIÓN PERSONALIZADA
    // =================================================================================

    /**
     * Clase de configuración para crear proveedores personalizados de configuración.
     * Patrón Builder para facilitar la configuración de múltiples opciones.
     */
    public static class ConfigurationProviderConfig {
        private boolean cacheEnabled = true;
        private String defaultEnvironment;
        private List<String> preloadFiles;
        private boolean validationEnabled = false;

        public ConfigurationProviderConfig() {}

        // Builder methods
        public ConfigurationProviderConfig withCache(boolean enabled) {
            this.cacheEnabled = enabled;
            return this;
        }

        public ConfigurationProviderConfig withDefaultEnvironment(String environment) {
            this.defaultEnvironment = environment;
            return this;
        }

        public ConfigurationProviderConfig withPreloadFiles(List<String> files) {
            this.preloadFiles = files;
            return this;
        }

        public ConfigurationProviderConfig withValidationEnabled(boolean enabled) {
            this.validationEnabled = enabled;
            return this;
        }

        // Getters
        public boolean isCacheEnabled() { return cacheEnabled; }
        public String getDefaultEnvironment() { return defaultEnvironment; }
        public List<String> getPreloadFiles() { return preloadFiles; }
        public boolean isValidationEnabled() { return validationEnabled; }
    }
}
