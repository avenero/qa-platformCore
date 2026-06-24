package com.qa.databasecore.factory;

import com.qa.databasecore.config.JdbcDriverAllowlist;
import com.qa.databasecore.connector.DatabaseConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Named-connection driver resolution for {@link DbConnectorFactory#connectAndCache(String)}
 * (FEC-DB-NAMED Core follow-up).
 *
 * <p>The named path ({@code {name}.db.*}) now reads {@code {name}.db.driver} explicitly and
 * only falls back to URL-based auto-detection when it is absent/blank — mirroring the default
 * {@code db.*} path ({@link DbConnectorFactory#createFromConfig()}). This:
 * <ul>
 *   <li>honors the driver field a FE-configured connection sends down as {@code {name}.db.driver}, and</li>
 *   <li>enables backends the JDBC URL alone can't disambiguate — notably H2, the no-Docker smoke backend.</li>
 * </ul>
 *
 * <p>Config is supplied via System Properties (highest-priority {@code ConfigSource} after the
 * ExecutionContext, which is inactive here), matching the H2-in-memory style of
 * {@code DatabaseGivenStepsConnectTest} / {@code DbConnectorFactoryTest}.
 */
@DisplayName("DbConnectorFactory — named connection driver resolution ({name}.db.driver)")
class DbConnectorFactoryNamedConnectionTest {

    /** System-property keys set by a test, cleared in tearDown so they never leak across the JVM. */
    private final List<String> setKeys = new ArrayList<>();

    @AfterEach
    void tearDown() {
        DbConnectorFactory.disconnectAll();
        for (String key : setKeys) {
            System.clearProperty(key);
        }
        setKeys.clear();
    }

    private void setProp(String key, String value) {
        System.setProperty(key, value);
        setKeys.add(key);
    }

    @Test
    @DisplayName("explicit H2 driver: named connection opens a real H2 pool and runs a query")
    void namedConnection_explicitH2Driver_connectsAndQueries() throws Exception {
        String name = "smokenamed";
        String url = "jdbc:h2:mem:named_ok_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
        setProp(name + ".db.url", url);
        setProp(name + ".db.username", "sa");
        // Explicit driver — H2 is NOT auto-detectable from the URL, so reaching a live connection
        // here proves {name}.db.driver was read and honored (the gap this change closes).
        setProp(name + ".db.driver", "org.h2.Driver");

        DatabaseConnector connector = DbConnectorFactory.connectAndCache(name);
        assertThat(connector).isNotNull();

        try (Connection conn = connector.getDataSource().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }

        // Cached by name: a second call returns the same instance without re-reading config.
        assertThat(DbConnectorFactory.getCachedConnector(name)).isSameAs(connector);
    }

    @Test
    @DisplayName("no driver: H2 URL is rejected via URL auto-detection (documents why the field is needed)")
    void namedConnection_noDriver_h2UrlRejected() {
        String name = "smokenodriver";
        setProp(name + ".db.url", "jdbc:h2:mem:named_nodriver_" + System.nanoTime());
        setProp(name + ".db.username", "sa");
        // No {name}.db.driver configured -> falls back to detectDriverFromUrl, which has no H2 case.

        assertThatThrownBy(() -> DbConnectorFactory.connectAndCache(name))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No se pudo detectar el tipo de base de datos");
    }

    @Test
    @DisplayName("blank driver (whitespace) is treated as absent and falls back to URL detection")
    void namedConnection_blankDriver_fallsBackToUrlDetection() {
        String name = "smokeblankdrv";
        setProp(name + ".db.url", "jdbc:h2:mem:named_blank_" + System.nanoTime());
        setProp(name + ".db.username", "sa");
        setProp(name + ".db.driver", "   ");   // whitespace-only -> blank -> ignored, fall back

        assertThatThrownBy(() -> DbConnectorFactory.connectAndCache(name))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No se pudo detectar el tipo de base de datos");
    }

    @Test
    @DisplayName("disallowed explicit driver is rejected by the allowlist before HikariCP loads it (W2-M2-DB-DRIVER)")
    void namedConnection_disallowedDriver_rejectedByAllowlist() {
        String name = "smokeevildrv";
        setProp(name + ".db.url", "jdbc:h2:mem:named_evil_" + System.nanoTime());
        setProp(name + ".db.username", "sa");
        setProp(name + ".db.driver", "com.acme.EvilDriver");   // input-driven, not in the allowlist

        assertThatThrownBy(() -> DbConnectorFactory.connectAndCache(name))
            .isInstanceOf(SecurityException.class)
            .hasMessage(JdbcDriverAllowlist.MSG_DRIVER_NOT_ALLOWED);
    }
}
