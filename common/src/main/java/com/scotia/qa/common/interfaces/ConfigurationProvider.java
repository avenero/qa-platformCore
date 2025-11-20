package com.scotia.qa.common.interfaces;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Interface que define el contrato para proveedores de configuración del framework Scotia QA.
 *
 * <p>Esta interface proporciona una abstracción unificada para la lectura y gestión de archivos
 * de configuración en múltiples formatos (YAML, JSON, Properties). Está diseñada para ser
 * completamente genérica y agnóstica del dominio, permitiendo su uso por cualquier framework
 * consumidor.
 *
 * <p><b>Características principales:</b>
 * <ul>
 *   <li>Soporte multi-formato: YAML, JSON, Properties</li>
 *   <li>Búsqueda jerárquica inteligente de archivos</li>
 *   <li>Navegación por notación punto en estructuras anidadas</li>
 *   <li>Conversión automática de tipos de datos</li>
 *   <li>Cache opcional para mejora de rendimiento</li>
 *   <li>Validación de archivos y contenido</li>
 *   <li>Manejo robusto de errores</li>
 *   <li>Thread-safe para uso concurrente</li>
 * </ul>
 *
 * <p><b>Formatos soportados:</b>
 * <ul>
 *   <li><b>YAML/YML</b> - Archivos de configuración YAML</li>
 *   <li><b>JSON</b> - Archivos de configuración JSON</li>
 *   <li><b>Properties</b> - Archivos de propiedades tradicionales</li>
 * </ul>
 *
 * <p><b>Jerarquía de búsqueda de archivos:</b>
 * <ol>
 *   <li>Classpath: directorio {@code /config/}</li>
 *   <li>Classpath: raíz del proyecto</li>
 *   <li>Sistema de archivos: directorio actual</li>
 *   <li>Sistema de archivos: directorio {@code config/}</li>
 * </ol>
 *
 * <p><b>Uso típico básico:</b>
 * <pre>
 * ConfigurationProvider provider = ConfigurationProviderFactory.getInstance();
 *
 * // Lectura básica por formato
 * Map&lt;String, Object&gt; yamlConfig = provider.loadYamlConfiguration("app.yml");
 * Map&lt;String, Object&gt; jsonConfig = provider.loadJsonConfiguration("config.json");
 * Properties props = provider.loadPropertiesConfiguration("database.properties");
 *
 * // Detección automática de formato
 * Map&lt;String, Object&gt; autoConfig = provider.loadConfiguration("settings.yml");
 *
 * // Navegación con notación punto
 * String dbHost = provider.getConfigurationValue("database.host", yamlConfig);
 * Integer port = provider.getConfigurationValue("database.port", yamlConfig, Integer.class);
 * </pre>
 *
 * <p><b>Uso avanzado con cache y validación:</b>
 * <pre>
 * ConfigurationProvider provider = ConfigurationProviderFactory.getCachedInstance();
 *
 * // Carga con cache automático
 * Map&lt;String, Object&gt; config = provider.loadConfiguration("app.yml");
 *
 * // Validación de configuración
 * boolean isValid = provider.validateConfiguration(config, requiredKeys);
 *
 * // Merge de múltiples archivos
 * Map&lt;String, Object&gt; merged = provider.mergeConfigurations(
 *     Arrays.asList("base.yml", "env-specific.yml")
 * );
 *
 * // Conversión automática de tipos
 * List&lt;String&gt; servers = provider.getConfigurationList("servers", config, String.class);
 * DatabaseConfig dbConfig = provider.getConfigurationObject("database", config, DatabaseConfig.class);
 * </pre>
 *
 * <p><b>Implementación en frameworks específicos:</b>
 * <pre>
 * // En api-core
 * public class ApiConfigurationManager {
 *     private static final ConfigurationProvider provider =
 *         ConfigurationProviderFactory.getInstance();
 *
 *     public String getApiBaseUrl() {
 *         Map&lt;String, Object&gt; config = provider.loadConfiguration("api-config.yml");
 *         return provider.getConfigurationValue("api.baseUrl", config);
 *     }
 * }
 * </pre>
 *
 * @author Scotia QA Framework Team
 * @version 1.0.0
 * @since 2.0.0
 */
public interface ConfigurationProvider {

    // =================================================================================
    // CARGA DE CONFIGURACIONES POR FORMATO ESPECÍFICO
    // =================================================================================

    /**
     * Carga un archivo de configuración YAML.
     *
     * @param fileName nombre del archivo YAML (ej: "config.yml", "settings.yaml")
     * @return Map con la configuración cargada
     * @throws ConfigurationException si el archivo no existe o tiene errores de formato
     */
    Map<String, Object> loadYamlConfiguration(String fileName);

    /**
     * Carga un archivo de configuración JSON.
     *
     * @param fileName nombre del archivo JSON (ej: "config.json")
     * @return Map con la configuración cargada
     * @throws ConfigurationException si el archivo no existe o tiene errores de formato
     */
    Map<String, Object> loadJsonConfiguration(String fileName);

    /**
     * Carga un archivo de propiedades.
     *
     * @param fileName nombre del archivo Properties (ej: "app.properties")
     * @return Properties con la configuración cargada
     * @throws ConfigurationException si el archivo no existe o tiene errores de formato
     */
    Properties loadPropertiesConfiguration(String fileName);

    /**
     * Carga una configuración detectando automáticamente el formato por la extensión.
     *
     * @param fileName nombre del archivo de configuración
     * @return Map con la configuración cargada
     * @throws ConfigurationException si el formato no es soportado o hay errores
     */
    Map<String, Object> loadConfiguration(String fileName);

    // =================================================================================
    // CARGA CON CONFIGURACIONES OPCIONALES Y FALLBACKS
    // =================================================================================

    /**
     * Carga una configuración de manera opcional.
     * No lanza excepción si el archivo no existe.
     *
     * @param fileName nombre del archivo
     * @return Optional con la configuración o empty si no existe
     */
    Optional<Map<String, Object>> loadOptionalConfiguration(String fileName);

    /**
     * Carga una configuración con fallback.
     *
     * @param primaryFileName archivo principal a cargar
     * @param fallbackFileName archivo de respaldo si el principal no existe
     * @return configuración del archivo principal o de fallback
     */
    Map<String, Object> loadConfigurationWithFallback(String primaryFileName, String fallbackFileName);

    /**
     * Carga múltiples archivos de configuración y los fusiona en uno.
     * Los archivos posteriores sobrescriben valores de los anteriores.
     *
     * @param fileNames lista de nombres de archivos en orden de prioridad
     * @return configuración fusionada
     */
    Map<String, Object> mergeConfigurations(List<String> fileNames);

    // =================================================================================
    // NAVEGACIÓN Y EXTRACCIÓN DE VALORES
    // =================================================================================

    /**
     * Obtiene un valor de configuración usando notación punto.
     *
     * @param path ruta del valor (ej: "database.host", "api.endpoints.login")
     * @param configuration mapa de configuración
     * @return valor encontrado o null si no existe
     */
    Object getConfigurationValue(String path, Map<String, Object> configuration);

    /**
     * Obtiene un valor de configuración con conversión de tipo.
     *
     * @param <T> tipo de dato esperado
     * @param path ruta del valor
     * @param configuration mapa de configuración
     * @param targetClass clase del tipo esperado
     * @return valor convertido al tipo especificado
     * @throws ConfigurationException si la conversión falla
     */
    <T> T getConfigurationValue(String path, Map<String, Object> configuration, Class<T> targetClass);

    /**
     * Obtiene un valor con valor por defecto.
     *
     * @param <T> tipo de dato esperado
     * @param path ruta del valor
     * @param configuration mapa de configuración
     * @param defaultValue valor por defecto si no se encuentra
     * @return valor encontrado o defaultValue
     */
    <T> T getConfigurationValue(String path, Map<String, Object> configuration, T defaultValue);

    /**
     * Obtiene una lista de valores de configuración.
     *
     * @param <T> tipo de elementos de la lista
     * @param path ruta de la lista
     * @param configuration mapa de configuración
     * @param elementClass clase del tipo de elementos
     * @return lista de valores convertidos
     */
    <T> List<T> getConfigurationList(String path, Map<String, Object> configuration, Class<T> elementClass);

    /**
     * Obtiene un objeto complejo de configuración.
     * Útil para mapear secciones de configuración a objetos POJO.
     *
     * @param <T> tipo del objeto esperado
     * @param path ruta del objeto
     * @param configuration mapa de configuración
     * @param targetClass clase del objeto objetivo
     * @return objeto mapeado desde la configuración
     */
    <T> T getConfigurationObject(String path, Map<String, Object> configuration, Class<T> targetClass);

    // =================================================================================
    // VERIFICACIÓN Y VALIDACIÓN
    // =================================================================================

    /**
     * Verifica si existe un archivo de configuración.
     *
     * @param fileName nombre del archivo a verificar
     * @return true si el archivo existe y es accesible
     */
    boolean configurationExists(String fileName);

    /**
     * Verifica si una configuración contiene una clave específica.
     *
     * @param path ruta de la clave usando notación punto
     * @param configuration mapa de configuración
     * @return true si la clave existe
     */
    boolean hasConfigurationKey(String path, Map<String, Object> configuration);

    /**
     * Valida que una configuración contenga las claves requeridas.
     *
     * @param configuration mapa de configuración
     * @param requiredKeys lista de claves requeridas (con notación punto)
     * @return true si todas las claves requeridas están presentes
     */
    boolean validateConfiguration(Map<String, Object> configuration, List<String> requiredKeys);

    /**
     * Valida que un archivo de configuración tenga formato correcto.
     *
     * @param fileName nombre del archivo a validar
     * @return true si el archivo es válido
     */
    boolean validateConfigurationFile(String fileName);

    /**
     * Obtiene todas las claves disponibles en una configuración.
     * Incluye claves anidadas con notación punto.
     *
     * @param configuration mapa de configuración
     * @return conjunto de todas las claves disponibles
     */
    Set<String> getAllConfigurationKeys(Map<String, Object> configuration);

    // =================================================================================
    // GESTIÓN DE ENTORNOS Y CONTEXTOS
    // =================================================================================

    /**
     * Carga configuración específica para un entorno.
     * Busca archivos con patrón: {base}-{environment}.{extension}
     *
     * @param baseName nombre base del archivo (ej: "config")
     * @param environment entorno (ej: "dev", "test", "prod")
     * @return configuración específica del entorno
     */
    Map<String, Object> loadEnvironmentConfiguration(String baseName, String environment);

    /**
     * Carga configuración con merge de entorno.
     * Carga base + específico del entorno y los fusiona.
     *
     * @param baseName nombre base (ej: "config")
     * @param environment entorno específico
     * @return configuración fusionada (base + entorno)
     */
    Map<String, Object> loadMergedEnvironmentConfiguration(String baseName, String environment);

    /**
     * Establece el entorno por defecto para operaciones de configuración.
     *
     * @param environment entorno por defecto
     */
    void setDefaultEnvironment(String environment);

    /**
     * Obtiene el entorno por defecto configurado.
     *
     * @return entorno por defecto actual
     */
    String getDefaultEnvironment();

    // =================================================================================
    // GESTIÓN DE CACHE Y RENDIMIENTO
    // =================================================================================

    /**
     * Habilita o deshabilita el cache de configuraciones.
     *
     * @param enabled true para habilitar cache
     */
    void setCacheEnabled(boolean enabled);

    /**
     * Verifica si el cache está habilitado.
     *
     * @return true si el cache está habilitado
     */
    boolean isCacheEnabled();

    /**
     * Limpia el cache de configuraciones.
     */
    void clearCache();

    /**
     * Pre-carga configuraciones en cache.
     * Útil para mejorar rendimiento en aplicaciones.
     *
     * @param fileNames lista de archivos a pre-cargar
     */
    void preloadConfigurations(List<String> fileNames);

    /**
     * Recarga una configuración específica, bypaseando el cache.
     *
     * @param fileName archivo a recargar
     * @return configuración recargada
     */
    Map<String, Object> reloadConfiguration(String fileName);

    // =================================================================================
    // INFORMACIÓN Y DEBUG
    // =================================================================================

    /**
     * Obtiene información de debug sobre el proveedor.
     *
     * @return string con información detallada para troubleshooting
     */
    String getProviderInfo();

    /**
     * Obtiene estadísticas de uso del proveedor.
     *
     * @return mapa con estadísticas (archivos cargados, cache hits, etc.)
     */
    Map<String, Object> getUsageStatistics();

    /**
     * Obtiene los formatos soportados por este proveedor.
     *
     * @return conjunto de extensiones soportadas
     */
    Set<String> getSupportedFormats();

    /**
     * Obtiene las rutas de búsqueda configuradas.
     *
     * @return lista de rutas donde se buscan los archivos
     */
    List<String> getSearchPaths();

    /**
     * Reinicia el proveedor a su estado inicial.
     */
    void reset();

    // =================================================================================
    // EXCEPCIONES PERSONALIZADAS
    // =================================================================================

    /**
     * Excepción personalizada para errores de configuración.
     */
    class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message) {
            super(message);
        }

        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
