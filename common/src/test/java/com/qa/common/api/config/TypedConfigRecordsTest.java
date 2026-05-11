package com.qa.common.api.config;

import com.qa.common.api.runtime.HttpEngine;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TASK-K03 — verifica que cada record TypedConfig:
 * <ul>
 *   <li>{@code defaults()} produce instancia válida (sin violations).</li>
 *   <li>{@code configPrefix()} retorna el prefijo esperado.</li>
 *   <li>Constraints de Bean Validation rechazan valores inválidos.</li>
 * </ul>
 */
class TypedConfigRecordsTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("HttpConfig.defaults() es válido y prefix='http'")
    void httpConfigDefaults() {
        HttpConfig c = HttpConfig.defaults();
        assertThat(c.configPrefix()).isEqualTo("http");
        assertThat(validator.validate(c)).isEmpty();
    }

    @Test
    @DisplayName("HttpConfig rechaza baseUrl no-http")
    void httpConfigRejectsInvalidUrl() {
        HttpConfig bad = new HttpConfig(
                HttpEngine.APACHE, "ftp://bad", Duration.ofSeconds(1),
                Duration.ofSeconds(1), 0, true);
        Set<ConstraintViolation<HttpConfig>> v = validator.validate(bad);
        assertThat(v).anySatisfy(cv -> assertThat(cv.getPropertyPath().toString()).isEqualTo("baseUrl"));
    }

    @Test
    @DisplayName("HttpConfig rechaza maxRetries > 10")
    void httpConfigRejectsTooManyRetries() {
        HttpConfig bad = new HttpConfig(
                HttpEngine.APACHE, "https://x", Duration.ofSeconds(1),
                Duration.ofSeconds(1), 999, true);
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    @Test
    @DisplayName("WebConfig.defaults() válido + prefix='web'")
    void webConfigDefaults() {
        WebConfig c = WebConfig.defaults();
        assertThat(c.configPrefix()).isEqualTo("web");
        assertThat(validator.validate(c)).isEmpty();
    }

    @Test
    @DisplayName("WebConfig rechaza browser desconocido")
    void webConfigRejectsBadBrowser() {
        WebConfig bad = new WebConfig("netscape", false, 800, 600,
                "http://x", Duration.ofSeconds(1), Duration.ofSeconds(1),
                "", Duration.ofSeconds(10), Duration.ZERO);
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    @Test
    @DisplayName("MobileConfig.defaults() válido + prefix='mobile'")
    void mobileConfigDefaults() {
        MobileConfig c = MobileConfig.defaults();
        assertThat(c.configPrefix()).isEqualTo("mobile");
        assertThat(validator.validate(c)).isEmpty();
    }

    @Test
    @DisplayName("MobileConfig rechaza platform desconocido")
    void mobileConfigRejectsBadPlatform() {
        MobileConfig bad = new MobileConfig(
                "blackberry", "", "", "", "ANDROID_EMULATOR", "",
                "http://x", 4723, false, Duration.ofSeconds(30), 10,
                "", "", "", "", true, false,
                Duration.ofSeconds(120), 1, true, true, true);
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    @Test
    @DisplayName("LoggingConfig.defaults() válido + prefix='logging'")
    void loggingConfigDefaults() {
        LoggingConfig c = LoggingConfig.defaults();
        assertThat(c.configPrefix()).isEqualTo("logging");
        assertThat(validator.validate(c)).isEmpty();
        assertThat(c.redactKeys()).contains("password", "token");
    }

    @Test
    @DisplayName("LoggingConfig rechaza rootLevel desconocido")
    void loggingConfigRejectsBadLevel() {
        LoggingConfig bad = new LoggingConfig("SUPER", "text", List.of(), true, false);
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    @Test
    @DisplayName("ConcurrencyConfig.defaults() válido + prefix='concurrency'")
    void concurrencyConfigDefaults() {
        ConcurrencyConfig c = ConcurrencyConfig.defaults();
        assertThat(c.configPrefix()).isEqualTo("concurrency");
        assertThat(validator.validate(c)).isEmpty();
        assertThat(c.maxParallelism()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("ConcurrencyConfig rechaza maxParallelism > 64")
    void concurrencyConfigRejectsTooManyThreads() {
        ConcurrencyConfig bad = new ConcurrencyConfig(999, 0, "exponential",
                Duration.ofMillis(1), "qa");
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    // ── TASK-K03M-F6: nuevos records y extensiones ──────────────────────────

    @Test
    @DisplayName("FrameworkConfig.defaults() válido + prefix='framework'")
    void frameworkConfigDefaults() {
        FrameworkConfig c = FrameworkConfig.defaults();
        assertThat(c.configPrefix()).isEqualTo("framework");
        assertThat(validator.validate(c)).isEmpty();
        assertThat(c.moduleName()).isEqualTo("PLATFORM");
        assertThat(c.environment()).isEqualTo("dev");
    }

    @Test
    @DisplayName("FrameworkConfig rechaza environment fuera del whitelist")
    void frameworkConfigRejectsBadEnvironment() {
        FrameworkConfig bad = new FrameworkConfig("PLATFORM", "production");
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    @Test
    @DisplayName("FrameworkConfig rechaza moduleName en blanco")
    void frameworkConfigRejectsBlankModule() {
        FrameworkConfig bad = new FrameworkConfig("  ", "qa");
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    @Test
    @DisplayName("MobileConfig.defaults() pasa Bean Validation con todos los nuevos campos")
    void mobileConfigExtendedDefaults() {
        MobileConfig c = MobileConfig.defaults();
        assertThat(c.configPrefix()).isEqualTo("mobile");
        assertThat(validator.validate(c)).isEmpty();
        assertThat(c.deviceType()).isEqualTo("ANDROID_EMULATOR");
        assertThat(c.appiumBasePort()).isEqualTo(4723);
        assertThat(c.appiumStartupTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(c.implicitWaitSec()).isEqualTo(10);
        assertThat(c.discoveryAutoScan()).isTrue();
    }

    @Test
    @DisplayName("MobileConfig rechaza deviceType fuera del whitelist")
    void mobileConfigRejectsBadDeviceType() {
        MobileConfig bad = new MobileConfig(
                "android", "", "", "", "BLACKBERRY_SIM", "",
                "http://localhost:4723", 4723, false, Duration.ofSeconds(30), 10,
                "", "", "", "", true, false,
                Duration.ofSeconds(120), 1, true, true, true);
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    @Test
    @DisplayName("MobileConfig rechaza appiumBasePort fuera de rango")
    void mobileConfigRejectsBadPort() {
        MobileConfig bad = new MobileConfig(
                "android", "", "", "", "ANDROID_EMULATOR", "",
                "http://localhost:4723", 80, false, Duration.ofSeconds(30), 10,
                "", "", "", "", true, false,
                Duration.ofSeconds(120), 1, true, true, true);
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    @Test
    @DisplayName("WebConfig.defaults() pasa Bean Validation con campos nuevos (proxyUrl/explicitWait/implicitWait)")
    void webConfigExtendedDefaults() {
        WebConfig c = WebConfig.defaults();
        assertThat(validator.validate(c)).isEmpty();
        assertThat(c.proxyUrl()).isEmpty();
        assertThat(c.explicitWait()).isEqualTo(Duration.ofSeconds(10));
        assertThat(c.implicitWait()).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("DatabaseConfig.defaults() válido + prefix override 'db'")
    void databaseConfigDefaults() {
        DatabaseConfig c = DatabaseConfig.defaults();
        assertThat(c.configPrefix()).isEqualTo("db");
        assertThat(validator.validate(c)).isEmpty();
        assertThat(c.poolSizeMax()).isEqualTo(10);
    }

    @Test
    @DisplayName("DatabaseConfig acepta password vacío (Windows integrated auth)")
    void databaseConfigAllowsEmptyPassword() {
        DatabaseConfig c = new DatabaseConfig(
                "jdbc:sqlserver://host;integratedSecurity=true",
                "", "", "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                "sqlserver", 5);
        assertThat(validator.validate(c)).isEmpty();
    }

    @Test
    @DisplayName("DatabaseConfig rechaza poolSizeMax fuera de [1..100]")
    void databaseConfigRejectsBadPoolSize() {
        DatabaseConfig low  = new DatabaseConfig("jdbc:h2:mem:t", "sa", "", "org.h2.Driver", "h2", 0);
        DatabaseConfig high = new DatabaseConfig("jdbc:h2:mem:t", "sa", "", "org.h2.Driver", "h2", 200);
        assertThat(validator.validate(low)).isNotEmpty();
        assertThat(validator.validate(high)).isNotEmpty();
    }

    @Test
    @DisplayName("DatabaseConfig rechaza url null")
    void databaseConfigRejectsNullUrl() {
        DatabaseConfig bad = new DatabaseConfig(null, "sa", "", "org.h2.Driver", "h2", 10);
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    @Test
    @DisplayName("ReportingConfig.defaults() válido + prefix='reporting'")
    void reportingConfigDefaults() {
        ReportingConfig c = ReportingConfig.defaults();
        assertThat(c.configPrefix()).isEqualTo("reporting");
        assertThat(validator.validate(c)).isEmpty();
        assertThat(c.cucumberJsonPath()).contains("cucumber.json");
    }

    @Test
    @DisplayName("ReportingConfig rechaza environment en blanco")
    void reportingConfigRejectsBlankEnvironment() {
        ReportingConfig bad = new ReportingConfig(true, "", "build/r/cucumber.json");
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    @Test
    @DisplayName("ExtentReportConfig.defaults() válido + prefix override 'extent'")
    void extentReportConfigDefaults() {
        ExtentReportConfig c = ExtentReportConfig.defaults();
        assertThat(c.configPrefix()).isEqualTo("extent");
        assertThat(validator.validate(c)).isEmpty();
        assertThat(c.theme()).isEqualTo("STANDARD");
    }

    @Test
    @DisplayName("ExtentReportConfig rechaza theme fuera de STANDARD|DARK")
    void extentReportConfigRejectsBadTheme() {
        ExtentReportConfig bad = new ExtentReportConfig(
                true, "build/", "report.html", "title", "report",
                "RAINBOW", true, true, true);
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    @Test
    @DisplayName("ExtentReportConfig rechaza outputPath en blanco")
    void extentReportConfigRejectsBlankOutputPath() {
        ExtentReportConfig bad = new ExtentReportConfig(
                true, "", "report.html", "title", "report",
                "STANDARD", true, true, true);
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    // ── Existentes ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("ConfigValidationException agrega todas las violations")
    void exceptionAggregatesViolations() {
        WebConfig bad = new WebConfig("xxx", false, 100, 100,
                "ftp://x", Duration.ofSeconds(1), Duration.ofSeconds(1),
                "", Duration.ofSeconds(10), Duration.ZERO);
        Set<ConstraintViolation<WebConfig>> violations = validator.validate(bad);
        assertThatThrownBy(() -> {
            throw new ConfigValidationException(WebConfig.class, violations);
        })
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("WebConfig")
                .hasMessageContaining("browser")
                .hasMessageContaining("violation");
    }
}
