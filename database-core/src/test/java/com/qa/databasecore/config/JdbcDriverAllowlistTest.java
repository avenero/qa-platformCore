package com.qa.databasecore.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JdbcDriverAllowlist} (W2-M2-DB-DRIVER).
 *
 * <p>The driver FQCN is input-driven (config keys {@code db.driver} / {@code {name}.db.driver})
 * and is loaded/instantiated by HikariCP, so it must be constrained to a known set. These tests
 * exercise the allowlist policy directly; the wiring into {@code DbConnectorFactory} is covered by
 * {@code DbConnectorFactoryNamedConnectionTest} and {@code DbConnectorFactoryTest}.</p>
 *
 * <p>The config-driven extension is supplied via the {@code db.driver.allowlist} System Property
 * (highest-priority {@code ConfigSource} read by {@code ConfigLoaderHolder.getRaw}), matching the
 * style of the named-connection tests.</p>
 */
@DisplayName("JdbcDriverAllowlist")
class JdbcDriverAllowlistTest {

    @AfterEach
    void tearDown() {
        System.clearProperty(JdbcDriverAllowlist.CONFIG_KEY_EXTRA_DRIVERS);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "org.h2.Driver",
        "org.postgresql.Driver",
        "oracle.jdbc.OracleDriver",
        "com.mysql.cj.jdbc.Driver",
        "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    })
    @DisplayName("default-supported drivers are allowed")
    void defaultDriversAllowed(String fqcn) {
        assertThatCode(() -> JdbcDriverAllowlist.requireAllowed(fqcn))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("surrounding whitespace on an allowed FQCN is tolerated")
    void allowedDriverIsTrimmed() {
        assertThatCode(() -> JdbcDriverAllowlist.requireAllowed("  org.h2.Driver  "))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "com.acme.EvilDriver",
        "java.lang.Runtime",
        "org.h2.Driver.Nested",
        "h2.Driver"
    })
    @DisplayName("an arbitrary FQCN is rejected with MSG_DRIVER_NOT_ALLOWED")
    void arbitraryDriverRejected(String fqcn) {
        assertThatThrownBy(() -> JdbcDriverAllowlist.requireAllowed(fqcn))
            .isInstanceOf(SecurityException.class)
            .hasMessage(JdbcDriverAllowlist.MSG_DRIVER_NOT_ALLOWED);
    }

    @Test
    @DisplayName("null is rejected (not in the set)")
    void nullDriverRejected() {
        assertThatThrownBy(() -> JdbcDriverAllowlist.requireAllowed(null))
            .isInstanceOf(SecurityException.class)
            .hasMessage(JdbcDriverAllowlist.MSG_DRIVER_NOT_ALLOWED);
    }

    @Test
    @DisplayName("blank is rejected (not in the set)")
    void blankDriverRejected() {
        assertThatThrownBy(() -> JdbcDriverAllowlist.requireAllowed("   "))
            .isInstanceOf(SecurityException.class)
            .hasMessage(JdbcDriverAllowlist.MSG_DRIVER_NOT_ALLOWED);
    }

    @Test
    @DisplayName("db.driver.allowlist extends the set without recompiling")
    void configExtendsAllowlist() {
        // Not allowed by default...
        assertThatThrownBy(() -> JdbcDriverAllowlist.requireAllowed("com.acme.CustomDriver"))
            .isInstanceOf(SecurityException.class);

        // ...becomes allowed once declared (CSV, with an unrelated extra entry and whitespace).
        System.setProperty(JdbcDriverAllowlist.CONFIG_KEY_EXTRA_DRIVERS,
            " com.acme.CustomDriver , com.foo.OtherDriver ");

        assertThatCode(() -> JdbcDriverAllowlist.requireAllowed("com.acme.CustomDriver"))
            .doesNotThrowAnyException();
        // Defaults still allowed alongside the extensions.
        assertThatCode(() -> JdbcDriverAllowlist.requireAllowed("org.postgresql.Driver"))
            .doesNotThrowAnyException();
        // An FQCN not in defaults nor in the extension list stays rejected.
        assertThatThrownBy(() -> JdbcDriverAllowlist.requireAllowed("com.acme.StillBlocked"))
            .isInstanceOf(SecurityException.class)
            .hasMessage(JdbcDriverAllowlist.MSG_DRIVER_NOT_ALLOWED);
    }
}
