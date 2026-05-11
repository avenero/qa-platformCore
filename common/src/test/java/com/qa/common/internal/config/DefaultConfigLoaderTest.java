package com.qa.common.internal.config;

import com.qa.common.api.config.ConfigValidationException;
import com.qa.common.api.config.HttpConfig;
import com.qa.common.api.config.LoggingConfig;
import com.qa.common.api.config.MobileConfig;
import com.qa.common.api.config.WebConfig;
import com.qa.common.api.runtime.HttpEngine;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.internal.config.source.ConfigSource;
import com.qa.common.internal.config.source.EnvVarSource;
import com.qa.common.internal.config.source.ExecutionContextSource;
import com.qa.common.internal.config.source.PropertiesFileSource;
import com.qa.common.internal.config.source.SystemPropertySource;
import com.qa.common.internal.config.source.YamlFileSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TASK-K03 — DefaultConfigLoader.
 *
 * <p>Cubre: defaults sin override, merge desde un source único, cache,
 * reload, priority order (SystemProperty > EnvVar > Yaml > Default),
 * parser de tipos, fallo de validación.
 */
class DefaultConfigLoaderTest {

    private DefaultConfigLoader loader;

    @AfterEach
    void cleanup() {
        if (loader != null) loader.close();
        System.clearProperty("http.engine");
        System.clearProperty("http.connectTimeout");
        System.clearProperty("http.maxRetries");
        System.clearProperty("k03m.dynamic.key");
        System.clearProperty("K03M_F3_VAR");
        System.clearProperty("k03m.f3.var");
        ExecutionContext.deactivate();
    }

    @Test
    @DisplayName("Sin sources → retorna defaults")
    void noSourcesReturnsDefaults() {
        loader = new DefaultConfigLoader(List.of());
        HttpConfig c = loader.load(HttpConfig.class);
        assertThat(c).isEqualTo(HttpConfig.defaults());
    }

    @Test
    @DisplayName("Cache: dos loads consecutivos retornan la misma instancia")
    void cacheIsHonored() {
        loader = new DefaultConfigLoader(List.of());
        HttpConfig c1 = loader.load(HttpConfig.class);
        HttpConfig c2 = loader.load(HttpConfig.class);
        assertThat(c1).isSameAs(c2);
    }

    @Test
    @DisplayName("reload() bypasa cache y regenera la instancia")
    void reloadBypassesCache() {
        loader = new DefaultConfigLoader(List.of(stub("http.maxRetries", "2")));
        HttpConfig first = loader.load(HttpConfig.class);
        HttpConfig reloaded = loader.reload(HttpConfig.class);
        assertThat(first).isNotSameAs(reloaded);
        assertThat(reloaded.maxRetries()).isEqualTo(2);
    }

    @Test
    @DisplayName("Source único sobreescribe defaults")
    void singleSourceOverrides() {
        loader = new DefaultConfigLoader(List.of(stub(
                "http.engine", "APACHE",
                "http.connectTimeout", "PT5S",
                "http.maxRetries", "3"
        )));
        HttpConfig c = loader.load(HttpConfig.class);
        assertThat(c.engine()).isEqualTo(HttpEngine.APACHE);
        assertThat(c.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(c.maxRetries()).isEqualTo(3);
        // Default sobre lo que no se overrideó
        assertThat(c.followRedirects()).isTrue();
    }

    @Test
    @DisplayName("Priority: SystemProperty > EnvVar > Yaml > Default")
    void priorityOrder() {
        // Yaml dice retries=1, Env dice 2, SysProp dice 3 → debería ganar 3
        Map<String, Object> yaml = Map.of("http", Map.of("maxRetries", "1"));
        Map<String, String> env = Map.of("HTTP_MAX_RETRIES", "2");
        System.setProperty("http.maxRetries", "3");

        loader = new DefaultConfigLoader(List.of(
                new SystemPropertySource(),
                new EnvVarSource(env::get),
                new YamlFileSource(yaml)
        ));
        HttpConfig c = loader.load(HttpConfig.class);
        assertThat(c.maxRetries()).isEqualTo(3);
    }

    @Test
    @DisplayName("Priority full chain (TASK-K03M-F2): SysProp > Env > Properties > Yaml > Default")
    void priorityOrderWithPropertiesFile() {
        Map<String, Object> yaml = Map.of("http", Map.of("maxRetries", "1"));
        Function<String, InputStream> classpath = name -> "config-dev.properties".equals(name)
                ? new ByteArrayInputStream("http.maxRetries=2".getBytes(StandardCharsets.UTF_8))
                : null;
        Map<String, String> env = Map.of("HTTP_MAX_RETRIES", "3");
        System.setProperty("http.maxRetries", "4");

        loader = new DefaultConfigLoader(List.of(
                new SystemPropertySource(),
                new EnvVarSource(env::get),
                new PropertiesFileSource("dev", classpath),
                new YamlFileSource(yaml)
        ));

        // Gana SysProp (4)
        assertThat(loader.load(HttpConfig.class).maxRetries()).isEqualTo(4);

        // Sin SysProp gana Env (3)
        System.clearProperty("http.maxRetries");
        loader.close();
        loader = new DefaultConfigLoader(List.of(
                new SystemPropertySource(),
                new EnvVarSource(env::get),
                new PropertiesFileSource("dev", classpath),
                new YamlFileSource(yaml)
        ));
        assertThat(loader.load(HttpConfig.class).maxRetries()).isEqualTo(3);

        // Sin SysProp ni Env gana Properties (2) — Yaml queda sombreado
        loader.close();
        loader = new DefaultConfigLoader(List.of(
                new SystemPropertySource(),
                new EnvVarSource(k -> null),
                new PropertiesFileSource("dev", classpath),
                new YamlFileSource(yaml)
        ));
        assertThat(loader.load(HttpConfig.class).maxRetries()).isEqualTo(2);

        // Solo Yaml → 1
        loader.close();
        loader = new DefaultConfigLoader(List.of(
                new SystemPropertySource(),
                new EnvVarSource(k -> null),
                new PropertiesFileSource("dev", n -> null),
                new YamlFileSource(yaml)
        ));
        assertThat(loader.load(HttpConfig.class).maxRetries()).isEqualTo(1);
    }

    @Test
    @DisplayName("Priority sin SysProp: gana EnvVar")
    void envOverYaml() {
        Map<String, Object> yaml = Map.of("http", Map.of("maxRetries", "1"));
        Map<String, String> env = Map.of("HTTP_MAX_RETRIES", "2");

        loader = new DefaultConfigLoader(List.of(
                new SystemPropertySource(),
                new EnvVarSource(env::get),
                new YamlFileSource(yaml)
        ));
        HttpConfig c = loader.load(HttpConfig.class);
        assertThat(c.maxRetries()).isEqualTo(2);
    }

    @Test
    @DisplayName("Parser: Duration en sufijo (30s, 1m, 500ms)")
    void durationParser() {
        assertThat(DefaultConfigLoader.RecordValueParser.parseDuration("30s"))
                .isEqualTo(Duration.ofSeconds(30));
        assertThat(DefaultConfigLoader.RecordValueParser.parseDuration("1m"))
                .isEqualTo(Duration.ofMinutes(1));
        assertThat(DefaultConfigLoader.RecordValueParser.parseDuration("500ms"))
                .isEqualTo(Duration.ofMillis(500));
        assertThat(DefaultConfigLoader.RecordValueParser.parseDuration("PT5S"))
                .isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Validación falla → ConfigValidationException agregando violations")
    void validationFails() {
        loader = new DefaultConfigLoader(List.of(stub("http.baseUrl", "ftp://invalid")));
        assertThatThrownBy(() -> loader.load(HttpConfig.class))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("HttpConfig")
                .hasMessageContaining("baseUrl");
    }

    @Test
    @DisplayName("Lista de strings con separador coma")
    void listParser() {
        loader = new DefaultConfigLoader(List.of(stub(
                "logging.redactKeys", "pwd, secret, mfa")));
        LoggingConfig c = loader.load(LoggingConfig.class);
        assertThat(c.redactKeys()).containsExactly("pwd", "secret", "mfa");
    }

    @Test
    @DisplayName("WebConfig defaults via loader")
    void webConfigViaLoader() {
        loader = new DefaultConfigLoader(List.of());
        WebConfig w = loader.load(WebConfig.class);
        assertThat(w).isEqualTo(WebConfig.defaults());
    }

    @Test
    @DisplayName("MobileConfig override platform via SystemProperty")
    void mobileConfigOverridePlatform() {
        loader = new DefaultConfigLoader(List.of(stub("mobile.platform", "ios")));
        MobileConfig m = loader.load(MobileConfig.class);
        assertThat(m.platform()).isEqualTo("ios");
    }

    // ── TASK-K03M-F1: getRaw escape hatch ────────────────────────────────────

    @Test
    @DisplayName("getRaw: SystemProperty gana sobre EnvVar (mismo orden de prioridad)")
    void getRaw_returnsValueFromHighestPrioritySource() {
        Map<String, String> env = Map.of("K03M_DYNAMIC_KEY", "from-env");
        System.setProperty("k03m.dynamic.key", "from-sysprop");

        loader = new DefaultConfigLoader(List.of(
                new SystemPropertySource(),
                new EnvVarSource(env::get)
        ));

        assertThat(loader.getRaw("k03m.dynamic.key")).contains("from-sysprop");
    }

    @Test
    @DisplayName("getRaw: retorna empty cuando ningún source tiene la key")
    void getRaw_returnsEmptyWhenAbsent() {
        loader = new DefaultConfigLoader(List.of(stub("other.key", "x")));
        assertThat(loader.getRaw("absent.key")).isEmpty();
        assertThat(loader.getRaw(null)).isEmpty();
        assertThat(loader.getRaw("  ")).isEmpty();
    }

    @Test
    @DisplayName("getRaw(key, default): retorna default cuando la key no existe")
    void getRaw_withDefault_returnsDefaultWhenAbsent() {
        loader = new DefaultConfigLoader(List.of(stub("present.key", "real")));
        assertThat(loader.getRaw("absent.key", "fallback")).isEqualTo("fallback");
        assertThat(loader.getRaw("present.key", "fallback")).isEqualTo("real");
    }

    // ── TASK-K03M-F3: VariableInterpolator wired into lookup() ───────────────

    @Test
    @DisplayName("lookup interpola ${VAR} en valores de SystemProperty")
    void lookup_interpolatesDollarVarFromSysProp() {
        System.setProperty("k03m.f3.var", "RESOLVED");
        loader = new DefaultConfigLoader(List.of(
                new SystemPropertySource(),
                stub("http.baseUrl", "https://${k03m.f3.var}.example.com")
        ));
        assertThat(loader.getRaw("http.baseUrl"))
                .contains("https://RESOLVED.example.com");
    }

    // ── TASK-K03M-F4: ExecutionContextSource priority + final-value contract ─

    @Test
    @DisplayName("ExecutionContext gana sobre SystemProperty (mismo contrato que ConfigManager step-0)")
    void executionContext_winsOverSystemProperty() {
        System.setProperty("k03m.dynamic.key", "from-sysprop");
        ExecutionConfig cfg = new ExecutionConfig.Builder()
                .property("k03m.dynamic.key", "from-execution-context")
                .build();
        ExecutionContext.builder().config(cfg).build().activate();

        loader = new DefaultConfigLoader(List.of(
                new ExecutionContextSource(),
                new SystemPropertySource()
        ));

        assertThat(loader.getRaw("k03m.dynamic.key")).contains("from-execution-context");
    }

    @Test
    @DisplayName("Valores de ExecutionContext son finales — NO se interpolan ${VAR}")
    void executionContext_valuesAreFinal_noInterpolation() {
        System.setProperty("K03M_F3_VAR", "should-not-be-substituted");
        ExecutionConfig cfg = new ExecutionConfig.Builder()
                .property("k03m.dynamic.key", "literal-${K03M_F3_VAR}-stays")
                .build();
        ExecutionContext.builder().config(cfg).build().activate();

        loader = new DefaultConfigLoader(List.of(
                new ExecutionContextSource(),
                new SystemPropertySource()
        ));

        // El valor del ExecutionContext queda literal (sin interpolar)
        assertThat(loader.getRaw("k03m.dynamic.key"))
                .contains("literal-${K03M_F3_VAR}-stays");
    }

    // -------------------------------------------------------------------------

    private static ConfigSource stub(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return new ConfigSource() {
            @Override public Optional<String> get(String key) {
                return Optional.ofNullable(map.get(key));
            }
            @Override public String name() { return "StubSource"; }
        };
    }
}
