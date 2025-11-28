package com.scotia.qa.common.config.providers;

import com.scotia.qa.common.config.providers.ConfigurationProvider;
import com.scotia.qa.common.utils.ConfigurationUtilities;
import com.scotia.qa.common.logging.TestLogger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Implementación base del proveedor de configuración para el framework Scotia QA.
 *
 * <p>Esta clase proporciona una implementación completa y robusta del ConfigurationProvider
 * que unifica y extiende la funcionalidad de ConfigurationUtilities. Está diseñada para ser
 * thread-safe, eficiente y extensible por frameworks específicos.
 *
 * <p><b>Características implementadas:</b>
 * <ul>
 *   <li>Soporte completo para YAML, JSON y Properties</li>
 *   <li>Cache inteligente thread-safe opcional</li>
 *   <li>Búsqueda jerárquica de archivos</li>
 *   <li>Conversión automática de tipos</li>
 *   <li>Validación robusta de configuraciones</li>
 *   <li>Gestión por entornos</li>
 *   <li>Merge de múltiples archivos</li>
 *   <li>Estadísticas de uso en tiempo real</li>
 *   <li>Logging detallado para troubleshooting</li>
 * </ul>
 *
 * <p><b>Uso típico:</b>
 * <pre>
 * ConfigurationProvider provider = new BaseConfigurationProvider();
 * provider.setCacheEnabled(true);
 *
 * // Carga con detección automática
 * Map&lt;String, Object&gt; config = provider.loadConfiguration("app.yml");
 *
 * // Navegación y conversión de tipos
 * String dbHost = provider.getConfigurationValue("database.host", config, "localhost");
 * Integer port = provider.getConfigurationValue("database.port", config, Integer.class);
 * List&lt;String&gt; servers = provider.getConfigurationList("servers", config, String.class);
 *
 * // Merge de configuraciones por entorno
 * Map&lt;String, Object&gt; envConfig = provider.loadMergedEnvironmentConfiguration("app", "prod");
 * </pre>
 *
 * <p><b>Configuración avanzada:</b>
 * <pre>
 * BaseConfigurationProvider provider = new BaseConfigurationProvider();
 * provider.setCacheEnabled(true);
 * provider.setDefaultEnvironment("test");
 *
 * // Pre-carga para mejor rendimiento
 * provider.preloadConfigurations(Arrays.asList("app.yml", "database.properties"));
 *
 * // Validación automática
 * List&lt;String&gt; requiredKeys = Arrays.asList("database.host", "api.baseUrl");
 * boolean isValid = provider.validateConfiguration(config, requiredKeys);
 * </pre>
 *
 * @author Scotia QA Framework Team
 * @version 1.0.0
 * @since 2.0.0
 * @see ConfigurationProvider
 * @see ConfigurationUtilities
 */
public class BaseConfigurationProvider implements ConfigurationProvider {

    private static final TestLogger.LoggerWrapper log = TestLogger.getLogger(BaseConfigurationProvider.class);

    // Mappers para diferentes formatos
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Formatos soportados
    private static final Set<String> SUPPORTED_FORMATS =
        Set.of("yml", "yaml", "json", "properties");

    // Cache thread-safe para configuraciones
    private final Map<String, Map<String, Object>> configurationCache = new ConcurrentHashMap<>();
    private final Map<String, Properties> propertiesCache = new ConcurrentHashMap<>();

    // Configuración del proveedor
    private boolean cacheEnabled = false;
    private String defaultEnvironment = "default";

    // Estadísticas de uso
    private final AtomicInteger filesLoaded = new AtomicInteger(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    private final AtomicLong totalLoadTime = new AtomicLong(0);

    // =================================================================================
    // IMPLEMENTACIÓN DE CARGA POR FORMATO ESPECÍFICO
    // =================================================================================

    @Override
    public Map<String, Object> loadYamlConfiguration(String fileName) {
        validateFileName(fileName);
        log.debug("Cargando configuración YAML: {}", fileName);

        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> config = getCachedConfiguration(fileName, () -> ConfigurationUtilities.readYamlFile(fileName));
            log.debug("Configuración YAML cargada exitosamente: {} ({} keys)", fileName, config.size());
            return config;
        } catch (Exception e) {
            throw new ConfigurationException("Error cargando configuración YAML: " + fileName, e);
        } finally {
            recordLoadTime(startTime);
        }
    }

    @Override
    public Map<String, Object> loadJsonConfiguration(String fileName) {
        validateFileName(fileName);
        log.debug("Cargando configuración JSON: {}", fileName);

        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> config = getCachedConfiguration(fileName, () -> ConfigurationUtilities.readJsonFile(fileName));
            log.debug("Configuración JSON cargada exitosamente: {} ({} keys)", fileName, config.size());
            return config;
        } catch (Exception e) {
            throw new ConfigurationException("Error cargando configuración JSON: " + fileName, e);
        } finally {
            recordLoadTime(startTime);
        }
    }

    @Override
    public Properties loadPropertiesConfiguration(String fileName) {
        validateFileName(fileName);
        log.debug("Cargando configuración Properties: {}", fileName);

        long startTime = System.currentTimeMillis();
        try {
            Properties properties = getCachedProperties(fileName, () -> ConfigurationUtilities.readPropertiesFile(fileName));
            log.debug("Configuración Properties cargada exitosamente: {} ({} properties)", fileName, properties.size());
            return properties;
        } catch (Exception e) {
            throw new ConfigurationException("Error cargando configuración Properties: " + fileName, e);
        } finally {
            recordLoadTime(startTime);
        }
    }

    @Override
    public Map<String, Object> loadConfiguration(String fileName) {
        validateFileName(fileName);

        String extension = getFileExtension(fileName).toLowerCase();
        log.debug("Cargando configuración con auto-detección: {} (formato: {})", fileName, extension);

        switch (extension) {
            case "yml":
            case "yaml":
                return loadYamlConfiguration(fileName);
            case "json":
                return loadJsonConfiguration(fileName);
            case "properties":
                // Convertir Properties a Map para consistencia
                Properties props = loadPropertiesConfiguration(fileName);
                return props.entrySet().stream()
                    .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue()
                    ));
            default:
                throw new ConfigurationException("Formato de archivo no soportado: " + extension +
                    ". Formatos soportados: " + String.join(", ", SUPPORTED_FORMATS));
        }
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE CARGA OPCIONAL Y FALLBACKS
    // =================================================================================

    @Override
    public Optional<Map<String, Object>> loadOptionalConfiguration(String fileName) {
        try {
            Map<String, Object> config = loadConfiguration(fileName);
            log.debug("Configuración opcional cargada exitosamente: {}", fileName);
            return Optional.of(config);
        } catch (ConfigurationException e) {
            log.debug("Configuración opcional no encontrada: {} - {}", fileName, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Object> loadConfigurationWithFallback(String primaryFileName, String fallbackFileName) {
        log.debug("Cargando configuración con fallback: {} -> {}", primaryFileName, fallbackFileName);

        Optional<Map<String, Object>> primary = loadOptionalConfiguration(primaryFileName);
        if (primary.isPresent()) {
            log.debug("Configuración primaria cargada: {}", primaryFileName);
            return primary.get();
        }

        log.debug("Configuración primaria no encontrada, usando fallback: {}", fallbackFileName);
        return loadConfiguration(fallbackFileName);
    }

    @Override
    public Map<String, Object> mergeConfigurations(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            throw new IllegalArgumentException("Lista de archivos no puede ser null o vacía");
        }

        log.debug("Fusionando configuraciones: {}", fileNames);
        Map<String, Object> merged = new LinkedHashMap<>();

        for (String fileName : fileNames) {
            Optional<Map<String, Object>> config = loadOptionalConfiguration(fileName);
            if (config.isPresent()) {
                mergeMapInto(merged, config.get());
                log.debug("Configuración fusionada: {} ({} keys agregadas)", fileName, config.get().size());
            } else {
                log.debug("Configuración omitida (no encontrada): {}", fileName);
            }
        }

        log.debug("Configuraciones fusionadas exitosamente: {} archivos -> {} keys total",
            fileNames.size(), merged.size());
        return merged;
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE NAVEGACIÓN Y EXTRACCIÓN
    // =================================================================================

    @Override
    public Object getConfigurationValue(String path, Map<String, Object> configuration) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        if (configuration == null) {
            return null;
        }

        Object value = ConfigurationUtilities.getNestedValue(configuration, path);
        log.debug("Valor obtenido para path '{}': {}", path, value);
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getConfigurationValue(String path, Map<String, Object> configuration, Class<T> targetClass) {
        Object value = getConfigurationValue(path, configuration);
        if (value == null) {
            return null;
        }

        return convertValue(value, targetClass, path);
    }

    @Override
    public <T> T getConfigurationValue(String path, Map<String, Object> configuration, T defaultValue) {
        T value = (T) getConfigurationValue(path, configuration, defaultValue.getClass());
        return value != null ? value : defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getConfigurationList(String path, Map<String, Object> configuration, Class<T> elementClass) {
        Object value = getConfigurationValue(path, configuration);
        if (value == null) {
            return new ArrayList<>();
        }

        if (!(value instanceof List)) {
            throw new ConfigurationException("El valor en path '" + path + "' no es una lista");
        }

        List<?> rawList = (List<?>) value;
        return rawList.stream()
            .map(item -> convertValue(item, elementClass, path))
            .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getConfigurationObject(String path, Map<String, Object> configuration, Class<T> targetClass) {
        Object value = getConfigurationValue(path, configuration);
        if (value == null) {
            return null;
        }

        if (!(value instanceof Map)) {
            throw new ConfigurationException("El valor en path '" + path + "' no es un objeto");
        }

        try {
            // Usar ObjectMapper para conversión a POJO
            String json = objectMapper.writeValueAsString(value);
            return objectMapper.readValue(json, targetClass);
        } catch (Exception e) {
            throw new ConfigurationException("Error convirtiendo objeto en path '" + path +
                "' a clase " + targetClass.getSimpleName(), e);
        }
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE VERIFICACIÓN Y VALIDACIÓN
    // =================================================================================

    @Override
    public boolean configurationExists(String fileName) {
        try {
            // Intentar cargar sin excepción para verificar existencia
            loadOptionalConfiguration(fileName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasConfigurationKey(String path, Map<String, Object> configuration) {
        return getConfigurationValue(path, configuration) != null;
    }

    @Override
    public boolean validateConfiguration(Map<String, Object> configuration, List<String> requiredKeys) {
        if (configuration == null || requiredKeys == null) {
            return false;
        }

        for (String key : requiredKeys) {
            if (!hasConfigurationKey(key, configuration)) {
                log.debug("Clave requerida faltante en configuración: {}", key);
                return false;
            }
        }

        log.debug("Configuración válida: todas las {} claves requeridas están presentes", requiredKeys.size());
        return true;
    }

    @Override
    public boolean validateConfigurationFile(String fileName) {
        try {
            loadConfiguration(fileName);
            log.debug("Archivo de configuración válido: {}", fileName);
            return true;
        } catch (ConfigurationException e) {
            log.debug("Archivo de configuración inválido {}: {}", fileName, e.getMessage());
            return false;
        }
    }

    @Override
    public Set<String> getAllConfigurationKeys(Map<String, Object> configuration) {
        if (configuration == null) {
            return new HashSet<>();
        }

        Set<String> keys = new HashSet<>();
        collectKeys(configuration, "", keys);

        log.debug("Obtenidas {} claves de configuración", keys.size());
        return keys;
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE GESTIÓN DE ENTORNOS
    // =================================================================================

    @Override
    public Map<String, Object> loadEnvironmentConfiguration(String baseName, String environment) {
        if (environment == null || environment.trim().isEmpty()) {
            environment = defaultEnvironment;
        }

        // Intentar diferentes extensiones para archivos de entorno
        List<String> possibleFiles = Arrays.asList(
            baseName + "-" + environment + ".yml",
            baseName + "-" + environment + ".yaml",
            baseName + "-" + environment + ".json",
            baseName + "-" + environment + ".properties"
        );

        log.debug("Buscando configuración de entorno para: {} en {}", environment, possibleFiles);

        for (String fileName : possibleFiles) {
            Optional<Map<String, Object>> config = loadOptionalConfiguration(fileName);
            if (config.isPresent()) {
                log.debug("Configuración de entorno encontrada: {}", fileName);
                return config.get();
            }
        }

        throw new ConfigurationException("No se encontró configuración para entorno: " + environment +
            " con base: " + baseName);
    }

    @Override
    public Map<String, Object> loadMergedEnvironmentConfiguration(String baseName, String environment) {
        log.debug("Cargando configuración fusionada para base: {} y entorno: {}", baseName, environment);

        // Cargar configuración base
        List<String> baseFiles = Arrays.asList(
            baseName + ".yml",
            baseName + ".yaml",
            baseName + ".json",
            baseName + ".properties"
        );

        Map<String, Object> baseConfig = new LinkedHashMap<>();
        boolean baseLoaded = false;

        for (String fileName : baseFiles) {
            Optional<Map<String, Object>> config = loadOptionalConfiguration(fileName);
            if (config.isPresent()) {
                baseConfig = config.get();
                baseLoaded = true;
                log.debug("Configuración base cargada: {}", fileName);
                break;
            }
        }

        if (!baseLoaded) {
            log.warn("No se encontró configuración base para: {}", baseName);
        }

        // Cargar y fusionar configuración de entorno
        try {
            Map<String, Object> envConfig = loadEnvironmentConfiguration(baseName, environment);
            mergeMapInto(baseConfig, envConfig);
            log.debug("Configuración de entorno fusionada exitosamente");
        } catch (ConfigurationException e) {
            log.debug("Configuración de entorno no encontrada, usando solo base: {}", e.getMessage());
        }

        return baseConfig;
    }

    @Override
    public void setDefaultEnvironment(String environment) {
        this.defaultEnvironment = environment != null ? environment.trim() : "default";
        log.debug("Entorno por defecto establecido: {}", this.defaultEnvironment);
    }

    @Override
    public String getDefaultEnvironment() {
        return defaultEnvironment;
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE GESTIÓN DE CACHE
    // =================================================================================

    @Override
    public void setCacheEnabled(boolean enabled) {
        this.cacheEnabled = enabled;
        log.debug("Cache de configuraciones: {}", enabled ? "HABILITADO" : "DESHABILITADO");
        if (!enabled) {
            clearCache();
        }
    }

    @Override
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    @Override
    public void clearCache() {
        int configCount = configurationCache.size();
        int propertiesCount = propertiesCache.size();

        configurationCache.clear();
        propertiesCache.clear();

        log.debug("Cache limpiado: {} configuraciones y {} properties removidas",
            configCount, propertiesCount);
    }

    @Override
    public void preloadConfigurations(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return;
        }

        log.debug("Pre-cargando {} configuraciones", fileNames.size());

        for (String fileName : fileNames) {
            try {
                loadOptionalConfiguration(fileName);
                log.debug("Pre-cargada: {}", fileName);
            } catch (Exception e) {
                log.debug("Error pre-cargando {}: {}", fileName, e.getMessage());
            }
        }

        log.debug("Pre-carga completada. Cache actual: {} entradas",
            configurationCache.size() + propertiesCache.size());
    }

    @Override
    public Map<String, Object> reloadConfiguration(String fileName) {
        log.debug("Recargando configuración (bypass cache): {}", fileName);

        // Remover del cache si existe
        configurationCache.remove(fileName);
        propertiesCache.remove(fileName);

        // Cargar nuevamente
        return loadConfiguration(fileName);
    }

    // =================================================================================
    // IMPLEMENTACIÓN DE INFORMACIÓN Y DEBUG
    // =================================================================================

    @Override
    public String getProviderInfo() {
        StringBuilder info = new StringBuilder();
        info.append("BaseConfigurationProvider v1.0.0\n");
        info.append("====================================\n");
        info.append("Cache habilitado: ").append(cacheEnabled).append("\n");
        info.append("Entorno por defecto: ").append(defaultEnvironment).append("\n");
        info.append("Formatos soportados: ").append(String.join(", ", SUPPORTED_FORMATS)).append("\n");
        info.append("Configuraciones en cache: ").append(configurationCache.size()).append("\n");
        info.append("Properties en cache: ").append(propertiesCache.size()).append("\n");
        info.append("Archivos cargados (total): ").append(filesLoaded.get()).append("\n");
        info.append("Cache hits: ").append(cacheHits.get()).append("\n");
        info.append("Cache misses: ").append(cacheMisses.get()).append("\n");
        info.append("Tiempo total de carga: ").append(totalLoadTime.get()).append("ms\n");

        return info.toString();
    }

    @Override
    public Map<String, Object> getUsageStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("cacheEnabled", cacheEnabled);
        stats.put("defaultEnvironment", defaultEnvironment);
        stats.put("configurationsCached", configurationCache.size());
        stats.put("propertiesCached", propertiesCache.size());
        stats.put("filesLoaded", filesLoaded.get());
        stats.put("cacheHits", cacheHits.get());
        stats.put("cacheMisses", cacheMisses.get());
        stats.put("cacheHitRate", calculateCacheHitRate());
        stats.put("totalLoadTimeMs", totalLoadTime.get());
        stats.put("averageLoadTimeMs", calculateAverageLoadTime());

        return stats;
    }

    @Override
    public Set<String> getSupportedFormats() {
        return new HashSet<>(SUPPORTED_FORMATS);
    }

    @Override
    public List<String> getSearchPaths() {
        // Rutas de búsqueda según ConfigurationUtilities
        return Arrays.asList(
            "classpath:/config/",
            "classpath:/",
            "file:./",
            "file:./config/"
        );
    }

    @Override
    public void reset() {
        clearCache();
        defaultEnvironment = "default";
        cacheEnabled = false;
        filesLoaded.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        totalLoadTime.set(0);

        log.debug("ConfigurationProvider reiniciado a estado inicial");
    }

    // =================================================================================
    // MÉTODOS PRIVADOS DE UTILIDAD
    // =================================================================================

    private Map<String, Object> getCachedConfiguration(String fileName, ConfigurationLoader loader) {
        if (!cacheEnabled) {
            cacheMisses.incrementAndGet();
            filesLoaded.incrementAndGet();
            return loader.load();
        }

        return configurationCache.computeIfAbsent(fileName, key -> {
            cacheMisses.incrementAndGet();
            filesLoaded.incrementAndGet();
            return loader.load();
        });
    }

    private Properties getCachedProperties(String fileName, PropertiesLoader loader) {
        if (!cacheEnabled) {
            cacheMisses.incrementAndGet();
            filesLoaded.incrementAndGet();
            return loader.load();
        }

        return propertiesCache.computeIfAbsent(fileName, key -> {
            cacheMisses.incrementAndGet();
            filesLoaded.incrementAndGet();
            return loader.load();
        });
    }

    @SuppressWarnings("unchecked")
    private void mergeMapInto(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map && target.get(key) instanceof Map) {
                // Merge recursivo para mapas anidados
                mergeMapInto((Map<String, Object>) target.get(key), (Map<String, Object>) value);
            } else {
                target.put(key, value);
            }
        }
    }

    private void collectKeys(Map<String, Object> map, String prefix, Set<String> keys) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            keys.add(key);

            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
                collectKeys(nestedMap, key, keys);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object value, Class<T> targetClass, String path) {
        try {
            if (targetClass.isInstance(value)) {
                return (T) value;
            }

            if (targetClass == String.class) {
                return (T) value.toString();
            }

            if (targetClass == Integer.class || targetClass == int.class) {
                return (T) Integer.valueOf(value.toString());
            }

            if (targetClass == Long.class || targetClass == long.class) {
                return (T) Long.valueOf(value.toString());
            }

            if (targetClass == Boolean.class || targetClass == boolean.class) {
                return (T) Boolean.valueOf(value.toString());
            }

            if (targetClass == Double.class || targetClass == double.class) {
                return (T) Double.valueOf(value.toString());
            }

            // Para otros tipos, intentar conversión JSON
            String json = objectMapper.writeValueAsString(value);
            return objectMapper.readValue(json, targetClass);

        } catch (Exception e) {
            throw new ConfigurationException("Error convirtiendo valor en path '" + path +
                "' a tipo " + targetClass.getSimpleName() + ": " + value, e);
        }
    }

    private void validateFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de archivo no puede ser null o vacío");
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    private void recordLoadTime(long startTime) {
        long loadTime = System.currentTimeMillis() - startTime;
        totalLoadTime.addAndGet(loadTime);
    }

    private double calculateCacheHitRate() {
        int total = cacheHits.get() + cacheMisses.get();
        return total > 0 ? (double) cacheHits.get() / total * 100.0 : 0.0;
    }

    private double calculateAverageLoadTime() {
        int loaded = filesLoaded.get();
        return loaded > 0 ? (double) totalLoadTime.get() / loaded : 0.0;
    }

    // =================================================================================
    // INTERFACES FUNCIONALES PRIVADAS
    // =================================================================================

    @FunctionalInterface
    private interface ConfigurationLoader {
        Map<String, Object> load();
    }

    @FunctionalInterface
    private interface PropertiesLoader {
        Properties load();
    }
}

