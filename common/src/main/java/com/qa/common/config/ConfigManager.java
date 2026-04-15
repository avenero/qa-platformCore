package com.qa.common.config;

import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.utils.ConfigurationUtilities;

import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Facade simplificado para gestión de configuraciones en el Runtime Engine.
 *
 * <p>Esta clase proporciona una API simple sobre {@link ConfigurationUtilities},
 * diseñada para facilitar el uso en componentes del Core y plugins.
 *
 * <p><b>Propósito:</b>
 * <ul>
 *   <li>API simple: {@code config.get("key")} para steps y plugins</li>
 *   <li>Orden de precedencia: ExecutionContext &gt; -D &gt; ENV &gt; config-{env}.properties &gt; defaults</li>
 *   <li>Resolución automática de variables: {@code ${VAR}}</li>
 *   <li>Detección automática de ambiente (dev/qa/prod)</li>
 * </ul>
 *
 * <p><b>Orden de precedencia de configuración (de mayor a menor):</b>
 * <ol>
 *   <li><b>ExecutionContext activo</b> — {@code ExecutionContext.current().config().getProperty(key)}.
 *       Sólo aplica cuando hay un contexto de ejecución en el hilo actual (durante la ejecución BDD).
 *       Permite aislar configuración por ejecución sin afectar otros hilos ni el singleton global.
 *       Los valores en {@link com.qa.common.runtime.ExecutionConfig#getProperty(String)} son
 *       definitivos: no se aplica resolución {@code ${VAR}} sobre ellos.</li>
 *   <li><b>System Properties</b> — {@code -Dkey=value} en línea de comandos</li>
 *   <li><b>Environment Variables</b> — {@code export KEY=value}</li>
 *   <li><b>Archivo específico de ambiente</b> — {@code config-{env}.properties}</li>
 *   <li><b>Archivo base</b> — {@code config.properties}</li>
 *   <li><b>Valor por defecto</b> — proporcionado en código</li>
 * </ol>
 *
 * <p><b>Arquitectura:</b>
 * <pre>
 * ConfigManager (Facade execution-aware)
 *      │
 *      ├─ step 0: ExecutionContext.current().config().getProperty(key)  ← per-ejecución (ThreadLocal)
 *      ├─ step 1: System.getProperty(key)                               ← -D flags globales
 *      ├─ step 2: System.getenv(key)                                    ← variables de entorno
 *      ├─ step 3: loadedProperties.getProperty(key)                     ← archivos config-*.properties
 *      └─ step 4: null / defaultValue                                   ← fallback
 *      ↓ delega lectura de archivos a
 * ConfigurationUtilities (Utilidades de lectura de archivos)
 * </pre>
 *
 * <p><b>Aislamiento por ejecución:</b>
 * El step 0 utiliza {@link ExecutionContext#current()}, que es un {@link ThreadLocal}.
 * Esto garantiza que cada hilo de ejecución ve su propia configuración sin interferir
 * con otros hilos (soporte para ejecución paralela de escenarios BDD).
 * En contextos sin ejecución activa (health-checks, {@code main}, etc.) el step 0
 * no produce valor y la cadena continúa normalmente, sin ningún NPE.
 *
 * <p><b>Cambio de contrato en 2.1.0:</b> se agregó el step 0 (ExecutionContext).
 * Las llamadas existentes a {@link #get(String)} y {@link #getWithPriority(String, String)}
 * funcionan sin cambios; ahora simplemente ven primero la configuración de la ejecución
 * activa cuando existe.
 *
 * @author QA Frameworks Core
 * @version 2.1.0
 * @since 1.0.3
 * @see ConfigurationUtilities
 * @see ExecutionContext
 * @see com.qa.common.runtime.ExecutionConfig
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

    // =========================================================================
    // MÉTODOS PÚBLICOS PRINCIPALES
    // =========================================================================

    /**
     * Obtiene un valor de configuración.
     *
     * <p>Orden de búsqueda:
     * <ol>
     *   <li><b>ExecutionContext activo</b> — {@code ExecutionContext.current().config().getProperty(key)}.
     *       Solo cuando hay una ejecución BDD en curso en el hilo actual. Los valores son definitivos
     *       (sin resolución de {@code ${VAR}}).</li>
     *   <li>System.getProperty(key)</li>
     *   <li>System.getenv(key)</li>
     *   <li>Archivo de configuración cargado</li>
     * </ol>
     *
     * @param key clave de configuración
     * @return valor configurado o null si no existe
     */
    public String get(String key) {
        // 0. ExecutionContext activo (mayor prioridad cuando hay ejecución BDD en curso)
        //    ThreadLocal → seguro en escenarios paralelos, sin NPE si no hay contexto.
        Optional<String> executionValue = resolveFromExecutionContext(key);
        if (executionValue.isPresent()) {
            return executionValue.get();
        }

        // 1. System Property (máxima prioridad global)
        String value = System.getProperty(key);

        // 2. Environment Variable (raw key)
        if (value == null) {
            value = System.getenv(key);
        }

        // 3. Archivo de configuración
        if (value == null && loadedProperties != null) {
            value = loadedProperties.getProperty(key);
        }

        // 4. Resolver variables ${VAR} si existen (solo para fuentes globales, no para ExecutionContext)
        if (value != null) {
            value = resolveEnvironmentVariables(value);
        }

        return value;
    }

    /**
     * Obtiene un valor de configuración con valor por defecto.
     *
     * @param key          clave de configuración
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
     * @param key          clave de configuración
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
     * @param key          clave de configuración
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
     *   <li><b>ExecutionContext activo</b> — {@code ExecutionContext.current().config().getProperty(key)}.
     *       Solo cuando hay una ejecución BDD activa en el hilo actual.</li>
     *   <li><b>System Property</b> — {@code -Dkey=value} (máxima prioridad global)</li>
     *   <li><b>Variable de Entorno</b> — {@code KEY_UPPER} (key.toUpperCase().replace('.','_'))</li>
     *   <li><b>Archivo de configuración</b> — {@code config-app.properties}</li>
     *   <li><b>Valor por defecto</b> — parámetro {@code defaultValue} (mínima prioridad)</li>
     * </ol>
     *
     * <p><b>Casos de uso:</b></p>
     * <ul>
     *   <li>Parametrizar navegador: {@code getWithPriority("web.browser", "chrome")}</li>
     *   <li>Override desde Jenkins: {@code -Dweb.browser=firefox}</li>
     *   <li>Override desde variables: {@code export WEB_BROWSER=edge}</li>
     *   <li>Override por ejecución: {@code ExecutionConfig.property("web.browser", "safari")}</li>
     * </ul>
     *
     * <p><b>Conversión de nombres para variables de entorno:</b></p>
     * <pre>
     * Key: "web.browser"           → Env Var: "WEB_BROWSER"
     * Key: "driver.chrome.version" → Env Var: "DRIVER_CHROME_VERSION"
     * </pre>
     *
     * @param key          clave de configuración (ej: "web.browser", "web.headless")
     * @param defaultValue valor por defecto si no se encuentra en ninguna fuente
     * @return valor resuelto según orden de prioridad
     */
    public String getWithPriority(String key, String defaultValue) {
        // 0. ExecutionContext activo (mayor prioridad cuando hay ejecución BDD en curso)
        Optional<String> executionValue = resolveFromExecutionContext(key);
        if (executionValue.isPresent()) {
            log.debug("✓ [{}] Usando ExecutionContext config: {}", key, executionValue.get());
            return executionValue.get();
        }

        // 1. System Property (-Dkey=value) - PRIORIDAD MÁXIMA GLOBAL
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isEmpty()) {
            log.debug("✓ [{}] Usando System Property: {}", key, sysProp);
            return sysProp;
        }

        // 2. Variable de entorno normalizada (key → KEY_UPPER)
        String envKey = key.toUpperCase().replace('.', '_');
        String envVar = System.getenv(envKey);
        if (envVar != null && !envVar.isEmpty()) {
            log.debug("✓ [{}] Usando variable de entorno {}: {}", key, envKey, envVar);
            return envVar;
        }

        // 3. Archivo de configuración (via get() que también resuelve ${VAR})
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
     * @param key          clave de configuración
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
     * Verifica si existe una configuración (incluyendo ExecutionContext activo).
     *
     * @param key clave de configuración
     * @return true si existe en cualquiera de las fuentes, false si no
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
     * Permite forzar recarga de configuración (archivos y propiedades globales).
     *
     * <p>No afecta los {@link ExecutionContext} activos en otros hilos,
     * ya que su configuración es inmutable y está en ThreadLocal.</p>
     */
    public synchronized void reload() {
        this.loadedProperties = loadConfigurationProperties();
        log.info("🔄 Configuración recargada para ambiente: {}", environment);
    }

    // =========================================================================
    // MÉTODOS PRIVADOS
    // =========================================================================

    /**
     * Intenta resolver la clave desde el {@link ExecutionContext} activo del hilo actual.
     *
     * <p>Usa {@link ExecutionContext#current()} (ThreadLocal) para obtener la configuración
     * específica de la ejecución en curso. Si no hay contexto activo, o si el contexto no
     * tiene un valor para la clave, retorna {@link Optional#empty()}.
     *
     * <p>Los valores de {@link com.qa.common.runtime.ExecutionConfig} son valores finales
     * (ya resueltos por el Backend), por lo que NO se aplica resolución de {@code ${VAR}}.
     *
     * <p>El bloque try-catch es defensivo: un fallo en el acceso al ThreadLocal nunca debe
     * romper la cadena de resolución de ConfigManager.
     *
     * @param key clave a buscar
     * @return Optional con el valor si existe en la ejecución activa, vacío en caso contrario
     */
    private Optional<String> resolveFromExecutionContext(String key) {
        try {
            return ExecutionContext.current()
                    .flatMap(ctx -> ctx.config().getProperty(key));
        } catch (Exception e) {
            // Defensivo: jamás romper la cadena de resolución por un error en ExecutionContext
            log.debug("No se pudo acceder a ExecutionContext para clave '{}': {}", key, e.getMessage());
            return Optional.empty();
        }
    }

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
     * Resuelve variables de entorno en formato {@code ${VAR}}.
     *
     * <p>Aplica SOLO a valores obtenidos desde System Properties, variables de entorno
     * y archivos de configuración. Los valores de {@link com.qa.common.runtime.ExecutionConfig}
     * no pasan por este método (son valores finales ya resueltos por el Backend).
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
