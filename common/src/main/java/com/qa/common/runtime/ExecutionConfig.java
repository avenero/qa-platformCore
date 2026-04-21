package com.qa.common.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * POJO inmutable de configuracion por ejecucion.
 *
 * <p>Encapsula toda la configuracion necesaria para una ejecucion BDD:
 * environment, browser, tags, etc. Se construye con el patron Builder.
 *
 * <p>Una vez construido, es thread-safe e inmutable.
 *
 * <h2>Claves de configuración oficiales por módulo</h2>
 *
 * <p>Las claves que se pueden enviar en {@link Builder#property(String, String)} /
 * {@link Builder#properties(java.util.Map)} están centralizadas en las clases de constantes
 * de cada módulo. Usar siempre esas constantes en lugar de cadenas literales:
 *
 * <ul>
 *   <li><b>Web/Selenium</b> — {@code com.qa.webcore.config.WebConfigKeys}
 *       <br>Principales: {@code "web.browser"}, {@code "web.headless"},
 *       {@code "web.base.url"}, {@code "web.grid.enabled"}, {@code "web.grid.url"},
 *       {@code "driver.strategy"}
 *   </li>
 *   <li><b>Mobile/Appium</b> — {@code com.qa.mobilecore.config.MobileConfigKeys}
 *       <br>Principales: {@code "mobile.platform"}, {@code "mobile.device.id"},
 *       {@code "mobile.appium.server.url"}, {@code "mobile.app.path"}
 *   </li>
 * </ul>
 *
 * <p><b>Nota de arquitectura:</b> {@code ExecutionConfig} pertenece al módulo {@code common}
 * y no puede importar directamente las clases de constantes de módulos superiores
 * ({@code web-core}, {@code mobile-core}) para evitar dependencias circulares.
 * Los valores de clave se documentan aquí como referencia; la fuente de verdad son
 * las clases {@code WebConfigKeys} y {@code MobileConfigKeys} en sus respectivos módulos.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class ExecutionConfig {

    private final String environment;
    private final String browser;
    private final String tags;
    private final boolean parallelEnabled;
    private final int threadCount;
    private final Map<String, String> properties;

    /**
     * Constructor privado; usar {@link Builder#build()}.
     *
     * @param builder instancia del builder con los valores configurados
     */
    private ExecutionConfig(Builder builder) {
        this.environment = builder.environment;
        this.browser = builder.browser;
        this.tags = builder.tags;
        this.parallelEnabled = builder.parallelEnabled;
        this.threadCount = builder.threadCount;
        this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
    }

    /**
     * Obtiene el ambiente de ejecución configurado.
     *
     * @return nombre del ambiente (ej: "qa", "prod")
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * Obtiene el navegador configurado para pruebas web.
     *
     * @return nombre del navegador (ej: "chrome", "firefox")
     */
    public String getBrowser() {
        return browser;
    }

    /**
     * Obtiene los tags de Cucumber para filtrar escenarios.
     *
     * @return expresión de tags (ej: "@smoke and @api")
     */
    public String getTags() {
        return tags;
    }

    /**
     * Indica si la ejecución paralela está habilitada.
     *
     * @return {@code true} si la ejecución paralela está activada
     */
    public boolean isParallelEnabled() {
        return parallelEnabled;
    }

    /**
     * Obtiene el número de threads para ejecución paralela.
     *
     * @return número de threads configurados
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * Obtiene las propiedades adicionales de configuración.
     *
     * @return mapa inmutable de propiedades, nunca null
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Obtiene una propiedad por clave como Optional.
     *
     * @param key clave de la propiedad
     * @return Optional con el valor, o vacío si no existe
     */
    public Optional<String> getProperty(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    /**
     * Obtiene una propiedad con valor por defecto si no existe.
     *
     * @param key          clave de la propiedad
     * @param defaultValue valor retornado si la clave no existe
     * @return valor encontrado o {@code defaultValue}
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    @Override
    public String toString() {
        return "ExecutionConfig{env='" + environment + "', browser='" + browser
                + "', tags='" + tags + "', parallel=" + parallelEnabled
                + ", threads=" + threadCount + ", props=" + properties.size() + "}";
    }

    /**
     * Builder para construir instancias inmutables de ExecutionConfig.
     */
    public static final class Builder {
        private String environment = "default";
        private String browser = "";
        private String tags = "";
        private boolean parallelEnabled = false;
        private int threadCount = 1;
        private final Map<String, String> properties = new HashMap<>();

        /**
         * Constructor por defecto con valores iniciales predefinidos.
         */
        public Builder() {
        }

        /**
         * Configura el ambiente de ejecución.
         *
         * @param environment nombre del ambiente (ej: "qa", "prod"), no null
         * @return este Builder para encadenamiento
         */
        public Builder environment(String environment) {
            this.environment = Objects.requireNonNull(environment, "environment no puede ser null");
            return this;
        }

        /**
         * Configura el navegador para pruebas web.
         *
         * @param browser nombre del navegador (ej: "chrome", "firefox"), no null
         * @return este Builder para encadenamiento
         */
        public Builder browser(String browser) {
            this.browser = Objects.requireNonNull(browser, "browser no puede ser null");
            return this;
        }

        /**
         * Configura los tags de Cucumber para filtrar escenarios.
         *
         * @param tags expresión de tags, no null
         * @return este Builder para encadenamiento
         */
        public Builder tags(String tags) {
            this.tags = Objects.requireNonNull(tags, "tags no puede ser null");
            return this;
        }

        /**
         * Activa o desactiva la ejecución paralela.
         *
         * @param parallelEnabled {@code true} para activar ejecución paralela
         * @return este Builder para encadenamiento
         */
        public Builder parallelEnabled(boolean parallelEnabled) {
            this.parallelEnabled = parallelEnabled;
            return this;
        }

        /**
         * Configura el número de threads para ejecución paralela.
         *
         * @param threadCount número de threads, debe ser mayor o igual a 1
         * @return este Builder para encadenamiento
         */
        public Builder threadCount(int threadCount) {
            if (threadCount < 1) {
                throw new IllegalArgumentException("threadCount debe ser >= 1, fue: " + threadCount);
            }
            this.threadCount = threadCount;
            return this;
        }

        /**
         * Agrega una propiedad clave-valor a la configuración.
         *
         * @param key   clave de la propiedad, no null
         * @param value valor de la propiedad, no null
         * @return este Builder para encadenamiento
         */
        public Builder property(String key, String value) {
            Objects.requireNonNull(key, "key no puede ser null");
            Objects.requireNonNull(value, "value no puede ser null");
            this.properties.put(key, value);
            return this;
        }

        /**
         * Agrega múltiples propiedades a la configuración.
         *
         * @param props mapa de propiedades a agregar, no null
         * @return este Builder para encadenamiento
         */
        public Builder properties(Map<String, String> props) {
            Objects.requireNonNull(props, "properties no puede ser null");
            this.properties.putAll(props);
            return this;
        }

        /**
         * Construye una instancia inmutable de {@link ExecutionConfig}.
         *
         * @return nueva instancia de ExecutionConfig
         */
        public ExecutionConfig build() {
            return new ExecutionConfig(this);
        }
    }
}
