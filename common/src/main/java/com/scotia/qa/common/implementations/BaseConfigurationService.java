package com.scotia.qa.common.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.scotia.qa.common.interfaces.ConfigurationService;
import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.utils.DataUtilities;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementación base del servicio de configuración para el framework Scotia QA.
 *
 * <p>Esta clase proporciona una implementación completa y robusta del servicio de configuración
 * que puede ser utilizada directamente o extendida por los frameworks específicos (API, Web, Mobile).
 * Unifica la lectura de archivos YAML, Properties y JSON con gestión inteligente de cache y resolución
 * de variables.
 *
 * <p><b>Características principales:</b>
 * <ul>
 *   <li>Lectura unificada de múltiples formatos: Properties, YAML, JSON</li>
 *   <li>Gestión inteligente de host con resolución de variables</li>
 *   <li>Cache automático para mejor rendimiento</li>
 *   <li>Búsqueda jerárquica de archivos (classpath, filesystem)</li>
 *   <li>Soporte para configuraciones por entorno</li>
 *   <li>Logging detallado para troubleshooting</li>
 *   <li>Thread-safe para uso concurrente</li>
 * </ul>
 *
 * <p><b>Jerarquía de búsqueda de archivos:</b>
 * <ol>
 *   <li>Classpath: directorio {@code /config/}</li>
 *   <li>Classpath: raíz del proyecto</li>
 *   <li>Sistema: directorio de trabajo actual</li>
 *   <li>Sistema: directorio {@code config/} en working directory</li>
 * </ol>
 *
 * <p><b>Uso típico:</b>
 * <pre>
 * ConfigurationService configService = new BaseConfigurationService();
 *
 * // Gestión de host
 * configService.setHost("https://api.example.com");
 * configService.setHost("HOST_VAR"); // Resuelve variable de entorno
 *
 * // Lectura de Properties
 * String dbUrl = configService.getProperty("database.properties", "url");
 * Map&lt;String, String&gt; allProps = configService.getAllProperties("app.properties");
 *
 * // Lectura de YAML
 * String apiHost = configService.getYamlProperty("endpoints.yml", "api.host");
 * Optional&lt;Map&lt;String, Object&gt;&gt; dbConfig = configService.getYamlNode("config.yml", "database");
 *
 * // Lectura de JSON
 * String version = configService.getJsonProperty("app.json", "version");
 *
 * // Configuraciones por entorno
 * configService.setEnvironment("test");
 * String testHost = configService.getEnvironmentProperty("api.host");
 * </pre>
 *
 * <p><b>Extensión en frameworks específicos:</b>
 * <pre>
 * // En api-core
 * public class ApiConfigurationService extends BaseConfigurationService {
 *
 *     public ApiConfigurationService() {
 *         super();
 *         setEnvironment("api");
 *         preloadConfigurations(Arrays.asList("api-endpoints.yml", "database.properties"));
 *     }
 *
 *     public String getApiBaseUrl() {
 *         return getYamlProperty("api-endpoints.yml", "base.url");
 *     }
 * }
 * </pre>
 *
 * @author Scotia QA Framework Team
 * @version 1.0.0
 * @since 2.0.0
 * @see ConfigurationService
 */
public class BaseConfigurationService implements ConfigurationService {

    private static final TestLogger.LoggerWrapper log = TestLogger.getLogger(BaseConfigurationService.class);

    // Mappers para diferentes formatos
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    // Cache thread-safe para configuraciones
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();

    // Estado del servicio
    private String currentHost;
    private String currentEnvironment = "default";

    // =================================================================================
    // IMPLEMENTACIÓN DE GESTIÓN DE HOST
    // =================================================================================

    @Override
    public void setHost(String host) {
        if (host == null) {
            this.currentHost = null;
            log.debug("Host limpiado (establecido a null)");
            return;
        }

        this.currentHost = resolveHost(host);
        log.debug("Host establecido: {}", this.currentHost);
    }

    @Override
    public String getHost() {
        return currentHost;
    }

    @Override
    public boolean hasHost() {
        return currentHost != null && !currentHost.trim().isEmpty();
    }

    @Override
    public String resolveHost(String hostVariable) {
        if (hostVariable == null || hostVariable.trim().isEmpty()) {
            return hostVariable;
        }

        String host = hostVariable.trim();

        // Si ya es una URL completa, devolverla tal como está
        if (host.startsWith("http://") || host.startsWith("https://")) {
            log.debug("Host ya es URL completa: {}", host);
            return host;
        }

        // 1. Intentar resolver desde variables del sistema
        String systemProperty = System.getProperty(host);
        if (DataUtilities.isValidString(systemProperty)) {
            log.debug("Host resuelto desde system property: {} -> {}", host, systemProperty);
            return systemProperty;
        }

        // 2. Intentar resolver desde variables de entorno
        String envVariable = System.getenv(host);
        if (DataUtilities.isValidString(envVariable)) {
            log.debug("Host resuelto desde variable de entorno: {} -> {}", host, envVariable);
            return envVariable;
        }

        // 3. Intentar cargar desde archivo hosts.properties
        try {
            Optional<Properties> hostsProps = readProperties("hosts.properties");
            if (hostsProps.isPresent()) {
                String propertyValue = hostsProps.get().getProperty(host);
                if (DataUtilities.isValidString(propertyValue)) {
                    log.debug("Host resuelto desde hosts.properties: {} -> {}", host, propertyValue);
                    return propertyValue;
                }
            }
        } catch (Exception e) {
            log.debug("No se pudo cargar hosts.properties: {}", e.getMessage());
        }

        // 4. Si no se puede resolver, devolver el valor original
        log.debug("No se pudo resolver la variable de host: {}. Usando valor original.", host);
        return host;
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE LECTURA DE PROPERTIES
    // =================================================================================

    @Override
    public Optional<Properties> readProperties(String fileName) {
        validateFileName(fileName, "Properties");

        String cacheKey = "props:" + fileName;
        if (configCache.containsKey(cacheKey)) {
            log.debug("Properties obtenidas del cache: {}", fileName);
            return Optional.of((Properties) configCache.get(cacheKey));
        }

        try {
            Optional<InputStream> stream = findFile(fileName);
            if (stream.isPresent()) {
                Properties props = new Properties();
                props.load(stream.get());
                configCache.put(cacheKey, props);
                log.debug("Properties cargadas y cacheadas exitosamente: {}", fileName);
                return Optional.of(props);
            }
        } catch (Exception e) {
            log.warn("Error al leer archivo Properties {}: {}", fileName, e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public String getProperty(String fileName, String key) {
        if (key == null || key.trim().isEmpty()) {
            log.warn("Key es null o vacía para archivo: {}", fileName);
            return null;
        }

        Optional<Properties> props = readProperties(fileName);
        if (props.isPresent()) {
            String value = props.get().getProperty(key);
            log.debug("Propiedad obtenida de {}: {} = {}", fileName, key, value);
            return value;
        }

        log.debug("Archivo no encontrado o propiedad no existe: {} -> {}", fileName, key);
        return null;
    }

    @Override
    public String getProperty(String fileName, String key, String defaultValue) {
        String value = getProperty(fileName, key);
        String result = (value != null) ? value : defaultValue;
        log.debug("Propiedad con default de {}: {} = {} (default: {})", fileName, key, result, defaultValue);
        return result;
    }

    @Override
    public Map<String, String> getAllProperties(String fileName) {
        Optional<Properties> props = readProperties(fileName);
        if (props.isPresent()) {
            Map<String, String> result = props.get().entrySet().stream()
                .collect(Collectors.toMap(
                    e -> e.getKey().toString(),
                    e -> e.getValue().toString()
                ));
            log.debug("Obtenidas {} propiedades de: {}", result.size(), fileName);
            return result;
        }

        log.debug("Archivo Properties no encontrado: {}, retornando Map vacío", fileName);
        return new HashMap<>();
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE LECTURA DE YAML
    // =================================================================================

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> readYaml(String fileName) {
        validateFileName(fileName, "YAML");

        String cacheKey = "yaml:" + fileName;
        if (configCache.containsKey(cacheKey)) {
            log.debug("YAML obtenido del cache: {}", fileName);
            return Optional.of((Map<String, Object>) configCache.get(cacheKey));
        }

        try {
            Optional<InputStream> stream = findFile(fileName);
            if (stream.isPresent()) {
                Map<String, Object> result = YAML_MAPPER.readValue(stream.get(), Map.class);
                configCache.put(cacheKey, result);
                log.debug("YAML cargado y cacheado exitosamente: {}", fileName);
                return Optional.of(result);
            }
        } catch (Exception e) {
            log.warn("Error al leer archivo YAML {}: {}", fileName, e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public String getYamlProperty(String fileName, String path) {
        if (path == null || path.trim().isEmpty()) {
            log.warn("Path es null o vacío para archivo: {}", fileName);
            return null;
        }

        Optional<Map<String, Object>> yamlData = readYaml(fileName);
        if (yamlData.isPresent()) {
            Object value = getNestedValue(yamlData.get(), path);
            String result = (value != null) ? value.toString() : null;
            log.debug("Propiedad YAML obtenida de {}: {} = {}", fileName, path, result);
            return result;
        }

        log.debug("Archivo YAML no encontrado o propiedad no existe: {} -> {}", fileName, path);
        return null;
    }

    @Override
    public String getYamlProperty(String fileName, String path, String defaultValue) {
        String value = getYamlProperty(fileName, path);
        String result = (value != null) ? value : defaultValue;
        log.debug("Propiedad YAML con default de {}: {} = {} (default: {})", fileName, path, result, defaultValue);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getYamlNode(String fileName, String path) {
        if (path == null || path.trim().isEmpty()) {
            return readYaml(fileName);
        }

        Optional<Map<String, Object>> yamlData = readYaml(fileName);
        if (yamlData.isPresent()) {
            Object value = getNestedValue(yamlData.get(), path);
            if (value instanceof Map) {
                Map<String, Object> result = (Map<String, Object>) value;
                log.debug("Nodo YAML obtenido de {}: {} con {} elementos", fileName, path, result.size());
                return Optional.of(result);
            }
        }

        log.debug("Nodo YAML no encontrado: {} -> {}", fileName, path);
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object> getYamlArray(String fileName, String path) {
        Optional<Map<String, Object>> yamlData = readYaml(fileName);
        if (yamlData.isPresent()) {
            Object value = getNestedValue(yamlData.get(), path);
            if (value instanceof List) {
                List<Object> result = (List<Object>) value;
                log.debug("Array YAML obtenido de {}: {} con {} elementos", fileName, path, result.size());
                return result;
            }
        }

        log.debug("Array YAML no encontrado: {} -> {}", fileName, path);
        return new ArrayList<>();
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE LECTURA DE JSON
    // =================================================================================

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> readJson(String fileName) {
        validateFileName(fileName, "JSON");

        String cacheKey = "json:" + fileName;
        if (configCache.containsKey(cacheKey)) {
            log.debug("JSON obtenido del cache: {}", fileName);
            return Optional.of((Map<String, Object>) configCache.get(cacheKey));
        }

        try {
            Optional<InputStream> stream = findFile(fileName);
            if (stream.isPresent()) {
                Map<String, Object> result = JSON_MAPPER.readValue(stream.get(), Map.class);
                configCache.put(cacheKey, result);
                log.debug("JSON cargado y cacheado exitosamente: {}", fileName);
                return Optional.of(result);
            }
        } catch (Exception e) {
            log.warn("Error al leer archivo JSON {}: {}", fileName, e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public String getJsonProperty(String fileName, String path) {
        if (path == null || path.trim().isEmpty()) {
            log.warn("Path es null o vacío para archivo: {}", fileName);
            return null;
        }

        Optional<Map<String, Object>> jsonData = readJson(fileName);
        if (jsonData.isPresent()) {
            Object value = getNestedValue(jsonData.get(), path);
            String result = (value != null) ? value.toString() : null;
            log.debug("Propiedad JSON obtenida de {}: {} = {}", fileName, path, result);
            return result;
        }

        log.debug("Archivo JSON no encontrado o propiedad no existe: {} -> {}", fileName, path);
        return null;
    }

    @Override
    public String getJsonProperty(String fileName, String path, String defaultValue) {
        String value = getJsonProperty(fileName, path);
        String result = (value != null) ? value : defaultValue;
        log.debug("Propiedad JSON con default de {}: {} = {} (default: {})", fileName, path, result, defaultValue);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getJsonNode(String fileName, String path) {
        if (path == null || path.trim().isEmpty()) {
            return readJson(fileName);
        }

        Optional<Map<String, Object>> jsonData = readJson(fileName);
        if (jsonData.isPresent()) {
            Object value = getNestedValue(jsonData.get(), path);
            if (value instanceof Map) {
                Map<String, Object> result = (Map<String, Object>) value;
                log.debug("Nodo JSON obtenido de {}: {} con {} elementos", fileName, path, result.size());
                return Optional.of(result);
            }
        }

        log.debug("Nodo JSON no encontrado: {} -> {}", fileName, path);
        return Optional.empty();
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE UTILIDADES Y VERIFICACIONES
    // =================================================================================

    @Override
    public boolean fileExists(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        Optional<InputStream> stream = findFile(fileName);
        boolean exists = stream.isPresent();

        if (stream.isPresent()) {
            try {
                stream.get().close();
            } catch (Exception e) {
                log.debug("Error cerrando stream para verificación: {}", e.getMessage());
            }
        }

        log.debug("Verificación de existencia de archivo {}: {}", fileName, exists);
        return exists;
    }

    @Override
    public boolean isFileValid(String fileName) {
        if (!fileExists(fileName)) {
            return false;
        }

        try {
            String extension = getFileExtension(fileName).toLowerCase();
            switch (extension) {
                case "properties":
                    return readProperties(fileName).isPresent();
                case "yml":
                case "yaml":
                    return readYaml(fileName).isPresent();
                case "json":
                    return readJson(fileName).isPresent();
                default:
                    // Para otros tipos, solo verificar existencia
                    return true;
            }
        } catch (Exception e) {
            log.debug("Error validando archivo {}: {}", fileName, e.getMessage());
            return false;
        }
    }

    @Override
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("BaseConfigurationService Debug Info:\n");
        info.append("=====================================\n");
        info.append("Current Host: ").append(currentHost != null ? currentHost : "NOT_SET").append("\n");
        info.append("Current Environment: ").append(currentEnvironment).append("\n");
        info.append("Cache Size: ").append(configCache.size()).append(" entries\n");
        info.append("Cached Files:\n");

        configCache.keySet().stream().sorted().forEach(key ->
            info.append("  - ").append(key).append("\n")
        );

        return info.toString();
    }

    @Override
    public void clearCache() {
        int oldSize = configCache.size();
        configCache.clear();
        log.debug("Cache limpiado: {} entradas removidas", oldSize);
    }

    @Override
    public void preloadConfigurations(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            log.debug("Lista de archivos vacía para precarga");
            return;
        }

        log.debug("Precargando {} configuraciones", fileNames.size());
        for (String fileName : fileNames) {
            try {
                String extension = getFileExtension(fileName).toLowerCase();
                switch (extension) {
                    case "properties":
                        readProperties(fileName);
                        break;
                    case "yml":
                    case "yaml":
                        readYaml(fileName);
                        break;
                    case "json":
                        readJson(fileName);
                        break;
                    default:
                        log.debug("Tipo de archivo no soportado para precarga: {}", fileName);
                }
            } catch (Exception e) {
                log.warn("Error precargando configuración {}: {}", fileName, e.getMessage());
            }
        }
        log.debug("Precarga completada. Cache actual: {} entradas", configCache.size());
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE GESTIÓN DE ENTORNOS
    // =================================================================================

    @Override
    public void setEnvironment(String environment) {
        this.currentEnvironment = (environment != null && !environment.trim().isEmpty())
            ? environment.trim() : "default";
        log.debug("Entorno establecido: {}", this.currentEnvironment);
    }

    @Override
    public String getEnvironment() {
        return currentEnvironment;
    }

    @Override
    public String getEnvironmentProperty(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        // Buscar primero en archivo específico del entorno
        if (!"default".equals(currentEnvironment)) {
            String envFile = "config-" + currentEnvironment + ".properties";
            String value = getProperty(envFile, key);
            if (value != null) {
                log.debug("Propiedad encontrada en archivo de entorno {}: {} = {}", envFile, key, value);
                return value;
            }
        }

        // Buscar en archivo por defecto
        String value = getProperty("config.properties", key);
        log.debug("Propiedad de entorno obtenida de config.properties: {} = {}", key, value);
        return value;
    }

    @Override
    public String getEnvironmentYamlProperty(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        // Buscar primero en archivo específico del entorno
        if (!"default".equals(currentEnvironment)) {
            String envFile = "config-" + currentEnvironment + ".yml";
            String value = getYamlProperty(envFile, path);
            if (value != null) {
                log.debug("Propiedad YAML encontrada en archivo de entorno {}: {} = {}", envFile, path, value);
                return value;
            }
        }

        // Buscar en archivo por defecto
        String value = getYamlProperty("config.yml", path);
        log.debug("Propiedad YAML de entorno obtenida de config.yml: {} = {}", path, value);
        return value;
    }

    // =================================================================================
    // MÉTODOS PRIVADOS DE UTILIDAD
    // =================================================================================

    /**
     * Busca un archivo en diferentes ubicaciones siguiendo la jerarquía establecida.
     */
    private Optional<InputStream> findFile(String fileName) {
        // 1. Buscar en classpath /config/
        InputStream stream = getClass().getResourceAsStream("/config/" + fileName);
        if (stream != null) {
            log.debug("Archivo encontrado en /config/: {}", fileName);
            return Optional.of(stream);
        }

        // 2. Buscar en classpath raíz
        stream = getClass().getResourceAsStream("/" + fileName);
        if (stream != null) {
            log.debug("Archivo encontrado en classpath raíz: {}", fileName);
            return Optional.of(stream);
        }

        // 3. Buscar en directorio de trabajo actual
        try {
            Path currentDir = Paths.get(fileName);
            if (Files.exists(currentDir)) {
                stream = Files.newInputStream(currentDir);
                log.debug("Archivo encontrado en directorio actual: {}", fileName);
                return Optional.of(stream);
            }
        } catch (Exception e) {
            log.debug("Error buscando en directorio actual: {}", e.getMessage());
        }

        // 4. Buscar en directorio config/
        try {
            Path configDir = Paths.get("config", fileName);
            if (Files.exists(configDir)) {
                stream = Files.newInputStream(configDir);
                log.debug("Archivo encontrado en directorio config/: {}", fileName);
                return Optional.of(stream);
            }
        } catch (Exception e) {
            log.debug("Error buscando en directorio config/: {}", e.getMessage());
        }

        log.debug("Archivo no encontrado en ninguna ubicación: {}", fileName);
        return Optional.empty();
    }

    /**
     * Obtiene un valor anidado de un Map usando notación punto.
     */
    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String path) {
        if (map == null || path == null) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Valida que el nombre de archivo no sea null o vacío.
     */
    private void validateFileName(String fileName, String fileType) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de archivo " + fileType + " no puede ser null o vacío");
        }
    }

    /**
     * Obtiene la extensión de un archivo.
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}
