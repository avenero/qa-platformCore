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

    private ExecutionConfig(Builder builder) {
        this.environment = builder.environment;
        this.browser = builder.browser;
        this.tags = builder.tags;
        this.parallelEnabled = builder.parallelEnabled;
        this.threadCount = builder.threadCount;
        this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
    }

    public String getEnvironment() {
        return environment;
    }

    public String getBrowser() {
        return browser;
    }

    public String getTags() {
        return tags;
    }

    public boolean isParallelEnabled() {
        return parallelEnabled;
    }

    public int getThreadCount() {
        return threadCount;
    }

    /**
     * Propiedades adicionales (clave-valor), inmutables.
     * @return mapa inmutable, nunca null
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Obtiene una propiedad por clave.
     * @param key clave
     * @return Optional con el valor, o vacio si no existe
     */
    public Optional<String> getProperty(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    /**
     * Obtiene una propiedad con valor por defecto.
     * @param key clave
     * @param defaultValue valor si no existe
     * @return valor encontrado o defaultValue
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

        public Builder() {}

        public Builder environment(String environment) {
            this.environment = Objects.requireNonNull(environment, "environment no puede ser null");
            return this;
        }

        public Builder browser(String browser) {
            this.browser = Objects.requireNonNull(browser, "browser no puede ser null");
            return this;
        }

        public Builder tags(String tags) {
            this.tags = Objects.requireNonNull(tags, "tags no puede ser null");
            return this;
        }

        public Builder parallelEnabled(boolean parallelEnabled) {
            this.parallelEnabled = parallelEnabled;
            return this;
        }

        public Builder threadCount(int threadCount) {
            if (threadCount < 1) {
                throw new IllegalArgumentException("threadCount debe ser >= 1, fue: " + threadCount);
            }
            this.threadCount = threadCount;
            return this;
        }

        public Builder property(String key, String value) {
            Objects.requireNonNull(key, "key no puede ser null");
            Objects.requireNonNull(value, "value no puede ser null");
            this.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, String> props) {
            Objects.requireNonNull(props, "properties no puede ser null");
            this.properties.putAll(props);
            return this;
        }

        public ExecutionConfig build() {
            return new ExecutionConfig(this);
        }
    }
}
