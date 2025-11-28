package com.scotia.qa.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.scotia.qa.common.logging.TestLogger;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

/**
 * Utilidades técnicas para gestión de archivos de configuración - Framework Scotia QA.
 *
 * <p>Esta clase proporciona la <b>capa técnica base</b> para lectura y parsing de archivos
 * de configuración en diferentes formatos. Es completamente agnóstica del dominio y actúa
 * como fundamento técnico para servicios de configuración de mayor nivel.
 *
 * <p><b>🎯 Posición en la arquitectura:</b>
 * <pre>
 * ConfigurationProvider (Interface de alto nivel)
 *         ↓
 * BaseConfigurationProvider (Servicio con cache, validación, entornos)
 *         ↓
 * ConfigurationUtils (Capa técnica pura - ESTA CLASE)
 * </pre>
 *
 * <p><b>Formatos soportados:</b>
 * <ul>
 *   <li><b>YAML</b> - Archivos .yml y .yaml</li>
 *   <li><b>JSON</b> - Archivos .json</li>
 *   <li><b>Properties</b> - Archivos .properties</li>
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
 * <p><b>Características:</b>
 * <ul>
 *   <li>Métodos estáticos thread-safe</li>
 *   <li>Manejo robusto de errores</li>
 *   <li>Búsqueda automática de archivos</li>
 *   <li>Logging detallado para troubleshooting</li>
 *   <li>Sin estado - solo funciones puras</li>
 *   <li>Base técnica para ConfigurationProvider</li>
 * </ul>
 *
 * <p><b>⚡ Uso directo (casos simples):</b>
 * <pre>
 * // Lectura directa sin cache ni validación
 * Map&lt;String, Object&gt; yamlConfig = ConfigurationUtils.readYamlFile("application.yml");
 * Map&lt;String, Object&gt; jsonConfig = ConfigurationUtils.readJsonFile("config.json");
 * Properties props = ConfigurationUtils.readPropertiesFile("app.properties");
 *
 * // Parsing directo de contenido
 * Map&lt;String, Object&gt; data = ConfigurationUtils.parseYaml(yamlContent);
 *
 * // Navegación en estructuras
 * Object value = ConfigurationUtils.getNestedValue(config, "database.host");
 * </pre>
 *
 * <p><b>🔧 Uso en testing:</b>
 * <pre>
 * &#64;Test
 * public void testConfigurationParsing() {
 *     Map&lt;String, Object&gt; config = ConfigurationUtils.readYamlFile("test-config.yml");
 *     String host = ConfigurationUtils.getStringValue(config, "db.host", "localhost");
 *     assertEquals("expected-host", host);
 * }
 * </pre>
 *
 * <p><b>🚀 Para casos avanzados, usar ConfigurationProvider:</b>
 * <pre>
 * // Para cache, validación, entornos, etc.
 * ConfigurationProvider provider = ConfigurationProviderFactory.getCachedInstance();
 * Map&lt;String, Object&gt; config = provider.loadConfiguration("app.yml");
 * </pre>
 *
 * <p><b>📝 Nota de Arquitectura:</b>
 * Esta clase es la base técnica que permite a {@code BaseConfigurationProvider}
 * funcionar. Mantiene la separación de responsabilidades proporcionando funciones
 * puras sin estado, mientras que ConfigurationProvider añade servicios de alto nivel.
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 * @see com.scotia.qa.common.config.providers.ConfigurationProvider
 * @see com.scotia.qa.common.config.providers.BaseConfigurationProvider
 * @see com.scotia.qa.common.config.providers.ConfigurationProviderFactory
 */
public final class ConfigurationUtilities {

    private static final TestLogger.LoggerWrapper log = TestLogger.getLogger(ConfigurationUtilities.class);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    // Constructor privado para clase de utilidad
    private ConfigurationUtilities() {
        throw new UnsupportedOperationException("ConfigurationUtilities es una clase de utilidad");
    }

    // =================================================================================
    // MÉTODOS PÚBLICOS DE LECTURA DE ARCHIVOS
    // =================================================================================

    /**
     * Lee un archivo YAML y retorna su contenido como Map.
     *
     * @param fileName nombre del archivo YAML (ej: "application.yml")
     * @return Map con el contenido del archivo YAML
     * @throws RuntimeException si el archivo no se encuentra o hay errores de parsing
     */
    public static Map<String, Object> readYamlFile(String fileName) {
        validateFileName(fileName);

        try (InputStream inputStream = findConfigFile(fileName)) {
            if (inputStream == null) {
                throw new RuntimeException("Archivo YAML no encontrado: " + fileName);
            }

            Map<String, Object> result = YAML_MAPPER.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
            log.debug("Archivo YAML leído exitosamente: {} ({} claves)", fileName, result.size());
            return result;

        } catch (IOException e) {
            throw new RuntimeException("Error leyendo archivo YAML: " + fileName + " - " + e.getMessage(), e);
        }
    }

    /**
     * Lee un archivo JSON y retorna su contenido como Map.
     *
     * @param fileName nombre del archivo JSON (ej: "config.json")
     * @return Map con el contenido del archivo JSON
     * @throws RuntimeException si el archivo no se encuentra o hay errores de parsing
     */
    public static Map<String, Object> readJsonFile(String fileName) {
        validateFileName(fileName);

        try (InputStream inputStream = findConfigFile(fileName)) {
            if (inputStream == null) {
                throw new RuntimeException("Archivo JSON no encontrado: " + fileName);
            }

            Map<String, Object> result = JSON_MAPPER.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
            log.debug("Archivo JSON leído exitosamente: {} ({} claves)", fileName, result.size());
            return result;

        } catch (IOException e) {
            throw new RuntimeException("Error leyendo archivo JSON: " + fileName + " - " + e.getMessage(), e);
        }
    }

    /**
     * Lee un archivo Properties y retorna su contenido.
     *
     * @param fileName nombre del archivo Properties (ej: "app.properties")
     * @return Properties con el contenido del archivo
     * @throws RuntimeException si el archivo no se encuentra o hay errores de lectura
     */
    public static Properties readPropertiesFile(String fileName) {
        validateFileName(fileName);

        try (InputStream inputStream = findConfigFile(fileName)) {
            if (inputStream == null) {
                throw new RuntimeException("Archivo Properties no encontrado: " + fileName);
            }

            Properties properties = new Properties();
            properties.load(inputStream);
            log.debug("Archivo Properties leído exitosamente: {} ({} propiedades)", fileName, properties.size());
            return properties;

        } catch (IOException e) {
            throw new RuntimeException("Error leyendo archivo Properties: " + fileName + " - " + e.getMessage(), e);
        }
    }

    // =================================================================================
    // MÉTODOS DE PARSING DIRECTO
    // =================================================================================

    /**
     * Parsea contenido YAML desde string.
     *
     * @param yamlContent contenido YAML como string
     * @return Map con el contenido parseado
     * @throws RuntimeException si hay errores de parsing
     */
    public static Map<String, Object> parseYaml(String yamlContent) {
        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Contenido YAML no puede ser null o vacío");
        }

        try {
            Map<String, Object> result = YAML_MAPPER.readValue(yamlContent, new TypeReference<Map<String, Object>>() {});
            log.debug("Contenido YAML parseado exitosamente ({} claves)", result.size());
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Error parseando contenido YAML: " + e.getMessage(), e);
        }
    }

    /**
     * Parsea contenido JSON desde string.
     *
     * @param jsonContent contenido JSON como string
     * @return Map con el contenido parseado
     * @throws RuntimeException si hay errores de parsing
     */
    public static Map<String, Object> parseJson(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Contenido JSON no puede ser null o vacío");
        }

        try {
            Map<String, Object> result = JSON_MAPPER.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});
            log.debug("Contenido JSON parseado exitosamente ({} claves)", result.size());
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Error parseando contenido JSON: " + e.getMessage(), e);
        }
    }

    // =================================================================================
    // MÉTODOS DE UTILIDAD PARA NAVEGACIÓN DE CONFIGURACIONES
    // =================================================================================

    /**
     * Obtiene un valor de configuración usando notación de puntos.
     * Ej: "database.url" busca config.get("database").get("url")
     *
     * @param config mapa de configuración
     * @param path ruta usando notación de puntos
     * @return valor encontrado o null si no existe
     */
    @SuppressWarnings("unchecked")
    public static Object getNestedValue(Map<String, Object> config, String path) {
        if (config == null || path == null || path.trim().isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = config;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Obtiene un valor de configuración como String usando notación de puntos.
     *
     * @param config mapa de configuración
     * @param path ruta usando notación de puntos
     * @param defaultValue valor por defecto si no se encuentra
     * @return valor como String o defaultValue
     */
    public static String getStringValue(Map<String, Object> config, String path, String defaultValue) {
        Object value = getNestedValue(config, path);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Verifica si existe una clave en la configuración usando notación de puntos.
     *
     * @param config mapa de configuración
     * @param path ruta usando notación de puntos
     * @return true si la clave existe
     */
    public static boolean hasKey(Map<String, Object> config, String path) {
        return getNestedValue(config, path) != null;
    }

    // =================================================================================
    // MÉTODOS PRIVADOS DE UTILIDAD
    // =================================================================================

    /**
     * Busca un archivo de configuración en múltiples ubicaciones.
     */
    private static InputStream findConfigFile(String fileName) {
        // 1. Classpath: /config/ directory
        InputStream stream = ConfigurationUtilities.class.getResourceAsStream("/config/" + fileName);
        if (stream != null) {
            log.debug("Archivo encontrado en classpath /config/: {}", fileName);
            return stream;
        }

        // 2. Classpath: root
        stream = ConfigurationUtilities.class.getResourceAsStream("/" + fileName);
        if (stream != null) {
            log.debug("Archivo encontrado en classpath root: {}", fileName);
            return stream;
        }

        // 3. Sistema: directorio actual
        try {
            Path currentDir = Paths.get(System.getProperty("user.dir"), fileName);
            if (Files.exists(currentDir)) {
                log.debug("Archivo encontrado en directorio actual: {}", fileName);
                return Files.newInputStream(currentDir);
            }
        } catch (IOException e) {
            log.debug("No se pudo leer desde directorio actual: {} - {}", fileName, e.getMessage());
        }

        // 4. Sistema: directorio config/
        try {
            Path configDir = Paths.get(System.getProperty("user.dir"), "config", fileName);
            if (Files.exists(configDir)) {
                log.debug("Archivo encontrado en directorio config/: {}", fileName);
                return Files.newInputStream(configDir);
            }
        } catch (IOException e) {
            log.debug("No se pudo leer desde directorio config/: {} - {}", fileName, e.getMessage());
        }

        log.warn("Archivo de configuración no encontrado en ninguna ubicación: {}", fileName);
        return null;
    }

    /**
     * Valida que el nombre del archivo sea válido.
     */
    private static void validateFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de archivo no puede ser null o vacío");
        }
    }
}
