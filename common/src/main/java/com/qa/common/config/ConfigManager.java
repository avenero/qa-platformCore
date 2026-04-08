package com.qa.common.config;

import com.qa.common.logging.TestLogger;
import com.qa.common.utils.ConfigurationUtilities;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Facade simplificado para gestión de configuraciones en el Runtime Engine.
 *
 * <p>Esta clase proporciona una API simple sobre {@link ConfigurationUtilities},
 * diseñada para facilitar el uso en componentes del Core y plugins.
 *
 * <p><b>🎯 Propósito:</b>
 * <ul>
 *   <li>API simple: {@code config.get("key")} para steps y plugins</li>
 *   <li>Orden de precedencia: -D > ENV > config-{env}.properties > defaults</li>
 *   <li>Resolución automática de variables: {@code ${VAR}}</li>
 *   <li>Detección automática de ambiente (dev/qa/prod)</li>
 * </ul>
 *
 * <p><b>Orden de precedencia de configuración:</b>
 * <ol>
 *   <li><b>System Properties</b> - {@code -Dkey=value} en línea de comandos</li>
 *   <li><b>Environment Variables</b> - {@code export KEY=value}</li>
 *   <li><b>Archivo específico de ambiente</b> - {@code config-{env}.properties}</li>
 *   <li><b>Archivo base</b> - {@code config.properties}</li>
 *   <li><b>Valor por defecto</b> - Proporcionado en código</li>
 * </ol>
 *
 * <p><b>Arquitectura:</b>
 * <pre>
 * ConfigManager (Facade simple)
 *      ↓ delega a
 * ConfigurationUtilities (Utilidades de lectura de archivos)
 * </pre>
 *
 * @author QA Frameworks Core
 * @version 2.0.0
 * @since 1.0.3
 * @see ConfigurationUtilities
 */
public class ConfigManager {

    private static final TestLogger.LoggerWrapper log = TestLogger.getLogger(ConfigManager.class);
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private static ConfigManager instance;
    private final String environment;
    private Properties loadedProperties;

    /**
     * Constructor privado (Singleton).
     */
    private ConfigManager() {
        this.environment = resolveEnvironment();
        this.loadedProperties = loadConfigurationProperties();

        log.info("✅ ConfigManager inicializado - Ambiente: {}", environment);
    }

    /**
     * Obtiene la instancia singleton de ConfigManager.
     *
     * @return instancia de ConfigManager
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * Obtiene un valor de configuración.
     *
     * <p>Orden de búsqueda:
     * <ol>
     *   <li>System.getProperty(key)</li>
     *   <li>System.getenv(key)</li>
     *   <li>Archivo de configuración cargado</li>
     * </ol>
     *
     * @param key clave de configuración
     * @return valor configurado o null si no existe
     */
    public String get(String key) {
        // 1. System Property (máxima prioridad)
        String value = System.getProperty(key);

        // 2. Environment Variable
        if (value == null) {
            value = System.getenv(key);
        }

        // 3. Configuration file
        if (value == null && loadedProperties != null) {
            value = loadedProperties.getProperty(key);
        }

        // 4. Resolver variables ${VAR} si existen
        if (value != null) {
            value = resolveEnvironmentVariables(value);
        }

        return value;
    }

    /**
     * Obtiene un valor de configuración con valor por defecto.
     *
     * @param key clave de configuración
     * @param defaultValue valor por defecto
     * @return valor configurado o defaultValue si no existe
     */
    public String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Obtiene un valor entero de configuración.
     *
     * @param key clave de configuración
     * @param defaultValue valor por defecto
     * @return valor configurado como int o defaultValue
     */
    public int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                log.warn("⚠️ No se pudo parsear '{}' como int, usando default: {}", value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Obtiene un valor booleano de configuración.
     *
     * @param key clave de configuración
     * @param defaultValue valor por defecto
     * @return valor configurado como boolean o defaultValue
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value != null ? Boolean.parseBoolean(value.trim()) : defaultValue;
    }

    /**
     * Obtiene configuración con prioridad de fuentes múltiples.
     *
     * <p>Orden de precedencia (de mayor a menor prioridad):</p>
     * <ol>
     *   <li><b>System Property</b> - {@code -Dkey=value} (máxima prioridad)</li>
     *   <li><b>Variable de Entorno</b> - {@code KEY_UPPER}</li>
     *   <li><b>Archivo de configuración</b> - {@code config-app.properties}</li>
     *   <li><b>Valor por defecto</b> - parámetro {@code defaultValue} (mínima prioridad)</li>
     * </ol>
     *
     * <p><b>Casos de uso:</b></p>
     * <ul>
     *   <li>Parametrizar navegador: {@code getWithPriority("web.browser", "chrome")}</li>
     *   <li>Override desde Jenkins: {@code -Dweb.browser=firefox}</li>
     *   <li>Override desde variables: {@code export WEB_BROWSER=edge}</li>
     * </ul>
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>{@code
     * // Configuración en config-app.properties:
     * web.browser=chrome
     *
     * // Ejecución:
     * ./gradlew test -Dweb.browser=firefox
     *
     * // Resultado:
     * String browser = config.getWithPriority("web.browser", "chrome");
     * // → "firefox" (System Property tiene prioridad sobre config file)
     * }</pre>
     *
     * <p><b>Conversión de nombres:</b></p>
     * <pre>
     * Key: "web.browser"        → Env Var: "WEB_BROWSER"
     * Key: "driver.chrome.version" → Env Var: "DRIVER_CHROME_VERSION"
     * </pre>
     *
     * @param key Clave de configuración (ej: "web.browser", "web.headless")
     * @param defaultValue Valor por defecto si no se encuentra en ninguna fuente
     * @return Valor resuelto según orden de prioridad
     */
    public String getWithPriority(String key, String defaultValue) {
        // 1. System Property (-Dkey=value) - PRIORIDAD MÁXIMA
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isEmpty()) {
            log.debug("✓ [{}] Usando System Property: {}", key, sysProp);
            return sysProp;
        }

        // 2. Variable de entorno (KEY → KEY_UPPER)
        String envKey = key.toUpperCase().replace('.', '_');
        String envVar = System.getenv(envKey);
        if (envVar != null && !envVar.isEmpty()) {
            log.debug("✓ [{}] Usando variable de entorno {}: {}", key, envKey, envVar);
            return envVar;
        }

        // 3. Archivo de configuración (config-app.properties)
        String configValue = get(key);
        if (configValue != null && !configValue.isEmpty()) {
            log.debug("✓ [{}] Usando config file: {}", key, configValue);
            return configValue;
        }

        // 4. Valor por defecto
        log.debug("✓ [{}] Usando default: {}", key, defaultValue);
        return defaultValue;
    }

    /**
     * Obtiene un valor long de configuración.
     *
     * @param key clave de configuración
     * @param defaultValue valor por defecto
     * @return valor configurado como long o defaultValue
     */
    public long getLong(String key, long defaultValue) {
        String value = get(key);
        if (value != null) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                log.warn("⚠️ No se pudo parsear '{}' como long, usando default: {}", value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Verifica si existe una configuración.
     *
     * @param key clave de configuración
     * @return true si existe, false si no
     */
    public boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * Obtiene el ambiente actual.
     *
     * @return ambiente (dev/qa/uat/prod)
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * Permite forzar recarga de configuración.
     * Útil en tests o cuando cambia el ambiente.
     */
    public synchronized void reload() {
        this.loadedProperties = loadConfigurationProperties();
        log.info("🔄 Configuración recargada para ambiente: {}", environment);
    }

    // =================================================================================
    // MÉTODOS PRIVADOS
    // =================================================================================

    /**
     * Resuelve el ambiente desde System Properties o Environment Variables.
     *
     * @return ambiente detectado (default: qa)
     */
    private String resolveEnvironment() {
        String env = System.getProperty("env");
        if (env == null) {
            env = System.getenv("TEST_ENV");
        }
        if (env == null) {
            env = "qa"; // Default
        }
        return env.toLowerCase().trim();
    }

    /**
     * Carga el archivo de configuración apropiado según el ambiente.
     *
     * <p>Estrategia de búsqueda flexible:</p>
     * <ol>
     *   <li>config-{env}.properties (ej: config-qa.properties)</li>
     *   <li>config-app.properties (nombre estándar recomendado)</li>
     *   <li>config.properties (fallback genérico)</li>
     *   <li>Cualquier archivo config-*.properties encontrado</li>
     * </ol>
     *
     * @return Properties cargado o vacío si no existe
     */
    private Properties loadConfigurationProperties() {
        // 1. Intentar config-{env}.properties
        String envConfigFile = "config-" + environment + ".properties";
        Properties props = tryLoadProperties(envConfigFile);
        if (props != null) {
            log.info("✅ Configuración cargada: {}", envConfigFile);
            return props;
        }

        // 2. Intentar config-app.properties (nombre estándar recomendado)
        props = tryLoadProperties("config-app.properties");
        if (props != null) {
            log.info("✅ Configuración cargada: config-app.properties");
            return props;
        }

        // 3. Intentar config.properties (fallback)
        props = tryLoadProperties("config.properties");
        if (props != null) {
            log.info("✅ Configuración cargada: config.properties");
            return props;
        }

        // 4. No se encontró archivo de configuración
        log.warn("⚠️ No se encontró archivo de configuración, usando solo System Properties y ENV");
        log.warn("📝 Solución: Crea 'config-app.properties' en src/test/resources/ del módulo");
        log.warn("   Ejemplo: cp config-app.properties.template config-app.properties");

        return new Properties();
    }

    /**
     * Intenta cargar un archivo .properties sin lanzar excepción.
     *
     * @param fileName nombre del archivo
     * @return Properties cargado o null si no existe/falla
     */
    private Properties tryLoadProperties(String fileName) {
        try {
            return ConfigurationUtilities.readPropertiesFile(fileName);
        } catch (Exception e) {
            log.debug("No se encontró: {} ({})", fileName, e.getMessage());
            return null;
        }
    }

    /**
     * Resuelve variables de entorno en formato ${VAR}.
     *
     * <p>Ejemplo:
     * <pre>
     * Input:  "jdbc:postgresql://${DB_HOST}:5432/banking"
     * Output: "jdbc:postgresql://qa-db.example.com:5432/banking"
     * </pre>
     *
     * @param value valor con posibles variables
     * @return valor con variables resueltas
     */
    private String resolveEnvironmentVariables(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }

        Matcher matcher = ENV_VAR_PATTERN.matcher(value);
        StringBuffer result = new StringBuffer(); // StringBuffer requerido por Matcher.appendReplacement

        while (matcher.find()) {
            String varName = matcher.group(1); // Extrae "DB_URL" de "${DB_URL}"

            // 1. Buscar en System Properties (máxima prioridad)
            String varValue = System.getProperty(varName);

            // 2. Buscar en Environment Variables
            if (varValue == null) {
                varValue = System.getenv(varName);
            }

            // 3. Intentar con guiones bajos convertidos a puntos (db.url → DB_URL)
            if (varValue == null) {
                String upperVarName = varName.toUpperCase().replace(".", "_");
                varValue = System.getenv(upperVarName);
            }

            // 4. Intentar con puntos convertidos a guiones bajos (DB_URL → db.url)
            if (varValue == null) {
                String lowerVarName = varName.toLowerCase().replace("_", ".");
                varValue = System.getProperty(lowerVarName);
            }

            // Si no se encuentra, dejar sin resolver y avisar
            if (varValue == null) {
                log.debug("Variable de entorno '{}' no encontrada (puede ser opcional)", varName);
                varValue = matcher.group(0); // Dejar ${VAR} sin cambios
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(varValue));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
