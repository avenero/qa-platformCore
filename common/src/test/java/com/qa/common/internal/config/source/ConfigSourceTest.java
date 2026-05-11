package com.qa.common.internal.config.source;

import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.ExecutionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TASK-K03 — tests por source.
 */
class ConfigSourceTest {

    // --- SystemPropertySource -------------------------------------------------

    @Test
    void systemPropertyReturnsValueWhenSet() {
        try {
            System.setProperty("k03.test.key", "value-sp");
            assertThat(new SystemPropertySource().get("k03.test.key")).contains("value-sp");
        } finally {
            System.clearProperty("k03.test.key");
        }
    }

    @Test
    void systemPropertyReturnsEmptyWhenAbsent() {
        assertThat(new SystemPropertySource().get("k03.absent.xyz")).isEmpty();
    }

    @Test
    void systemPropertyHandlesNullAndBlank() {
        SystemPropertySource s = new SystemPropertySource();
        assertThat(s.get(null)).isEmpty();
        assertThat(s.get("")).isEmpty();
    }

    // --- EnvVarSource ---------------------------------------------------------

    @Test
    void envVarTranslatesCamelCaseToUpperSnake() {
        assertThat(EnvVarSource.toEnvVarName("http.connectTimeout"))
                .isEqualTo("HTTP_CONNECT_TIMEOUT");
        assertThat(EnvVarSource.toEnvVarName("web.baseUrl"))
                .isEqualTo("WEB_BASE_URL");
        assertThat(EnvVarSource.toEnvVarName("mobile.appiumServerUrl"))
                .isEqualTo("MOBILE_APPIUM_SERVER_URL");
    }

    @Test
    void envVarLooksUpTranslatedKey() {
        Map<String, String> fakeEnv = Map.of(
                "HTTP_CONNECT_TIMEOUT", "PT5S",
                "LOGGING_MDC_ENABLED", "false");
        EnvVarSource s = new EnvVarSource(fakeEnv::get);
        assertThat(s.get("http.connectTimeout")).contains("PT5S");
        assertThat(s.get("logging.mdcEnabled")).contains("false");
        assertThat(s.get("absent")).isEmpty();
    }

    // --- YamlFileSource (in-memory map) ---------------------------------------

    @Test
    void yamlFileResolvesNestedKey() {
        Map<String, Object> http = new LinkedHashMap<>();
        http.put("engine", "APACHE");
        http.put("connectTimeout", "PT5S");
        Map<String, Object> root = Map.of("http", http);

        YamlFileSource s = new YamlFileSource(root);
        assertThat(s.get("http.engine")).contains("APACHE");
        assertThat(s.get("http.connectTimeout")).contains("PT5S");
        assertThat(s.get("http.absent")).isEmpty();
        assertThat(s.get("nonexistent.path")).isEmpty();
    }

    @Test
    void yamlFileEmptyRootReturnsEmpty() {
        assertThat(new YamlFileSource((Map<String, Object>) null).get("any")).isEmpty();
        assertThat(new YamlFileSource(new HashMap<>()).get("any")).isEmpty();
    }

    // --- PropertiesFileSource (TASK-K03M-F2) ---------------------------------

    @Test
    void propertiesFile_loadsFromConfigEnvFile_whenEnvSet() {
        Function<String, InputStream> classpath = fakeClasspath(Map.of(
                "config-qa.properties",  "k03m.key=from-env-file\nk03m.shared=qa-wins",
                "config-app.properties", "k03m.shared=app-loses"
        ));
        PropertiesFileSource s = new PropertiesFileSource("qa", classpath);

        assertThat(s.get("k03m.key")).contains("from-env-file");
        assertThat(s.get("k03m.shared")).contains("qa-wins");
        assertThat(s.name()).isEqualTo("PropertiesFile(config-qa.properties)");
    }

    @Test
    void propertiesFile_fallsBackToConfigApp_whenEnvFileMissing() {
        Function<String, InputStream> classpath = fakeClasspath(Map.of(
                "config-app.properties", "k03m.app=ok",
                "config.properties",     "k03m.app=ignored"
        ));
        PropertiesFileSource s = new PropertiesFileSource("missing-env", classpath);

        assertThat(s.get("k03m.app")).contains("ok");
        assertThat(s.name()).isEqualTo("PropertiesFile(config-app.properties)");
    }

    @Test
    void propertiesFile_fallsBackToConfigProperties_whenAppMissing() {
        Function<String, InputStream> classpath = fakeClasspath(Map.of(
                "config.properties", "k03m.bare=last-resort"
        ));
        PropertiesFileSource s = new PropertiesFileSource("dev", classpath);

        assertThat(s.get("k03m.bare")).contains("last-resort");
        assertThat(s.name()).isEqualTo("PropertiesFile(config.properties)");
    }

    @Test
    void propertiesFile_returnsEmpty_whenNoFile() {
        Function<String, InputStream> classpath = name -> null;
        PropertiesFileSource s = new PropertiesFileSource("dev", classpath);

        assertThat(s.get("any.key")).isEmpty();
        assertThat(s.get(null)).isEmpty();
        assertThat(s.get("  ")).isEmpty();
        assertThat(s.name()).isEqualTo("PropertiesFile(<none>)");
    }

    @Test
    void propertiesFile_resolveEnvironment_usesSystemPropertyThenEnvThenDev() {
        try {
            System.clearProperty("env");
            assertThat(PropertiesFileSource.resolveEnvironment())
                    .satisfiesAnyOf(
                            v -> assertThat(v).isEqualTo("dev"),
                            v -> assertThat(v).isEqualTo(System.getenv("TEST_ENV"))
                    );

            System.setProperty("env", "STAGING");
            assertThat(PropertiesFileSource.resolveEnvironment()).isEqualTo("staging");
        } finally {
            System.clearProperty("env");
        }
    }

    private static Function<String, InputStream> fakeClasspath(Map<String, String> files) {
        return name -> {
            String content = files.get(name);
            return content == null ? null : new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        };
    }

    // --- ExecutionContextSource (TASK-K03M-F4) -------------------------------

    @AfterEach
    void cleanupExecutionContext() {
        ExecutionContext.deactivate();
    }

    @Test
    void executionContext_returnsEmpty_whenNoActiveContext() {
        ExecutionContext.deactivate();
        ExecutionContextSource s = new ExecutionContextSource();

        assertThat(s.get("any.key")).isEmpty();
        assertThat(s.get(null)).isEmpty();
        assertThat(s.get("  ")).isEmpty();
        assertThat(s.name()).isEqualTo("ExecutionContext");
    }

    @Test
    void executionContext_returnsValueFromActiveContext() {
        ExecutionConfig cfg = new ExecutionConfig.Builder()
                .property("k03m.f4.key", "from-execution-context")
                .build();
        ExecutionContext ctx = ExecutionContext.builder().config(cfg).build();
        ctx.activate();

        ExecutionContextSource s = new ExecutionContextSource();
        assertThat(s.get("k03m.f4.key")).contains("from-execution-context");
        assertThat(s.get("absent.key")).isEmpty();
    }

    @Test
    void executionContext_isThreadIsolated() throws Exception {
        ExecutionConfig cfgA = new ExecutionConfig.Builder().property("k03m.f4.shared", "thread-A").build();
        ExecutionContext.builder().config(cfgA).build().activate();

        ExecutionContextSource s = new ExecutionContextSource();

        Thread tB = new Thread(() -> {
            ExecutionConfig cfgB = new ExecutionConfig.Builder().property("k03m.f4.shared", "thread-B").build();
            ExecutionContext.builder().config(cfgB).build().activate();
            try {
                assertThat(s.get("k03m.f4.shared")).contains("thread-B");
            } finally {
                ExecutionContext.deactivate();
            }
        });
        tB.start();
        tB.join();

        assertThat(s.get("k03m.f4.shared")).contains("thread-A");
    }
}
