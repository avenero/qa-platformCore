package com.qa.databasecore.config;

import com.qa.common.api.config.ConfigLoaderHolder;
import com.qa.common.api.logging.TestLogger;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Allowlist de drivers JDBC permitidos antes de {@code HikariConfig.setDriverClassName}.
 *
 * <p>El FQCN del driver puede provenir de configuración controlada por el proyecto/escenario
 * ({@code db.driver} y {@code {name}.db.driver}); HikariCP carga e instancia esa clase, por lo
 * que un FQCN arbitrario es un vector de carga de clases. Esta allowlist restringe el driver a un
 * conjunto de backends soportados.</p>
 *
 * <p>Conjunto por defecto: H2, PostgreSQL, Oracle, MySQL y SQL Server. Ampliable sin recompilar
 * vía la clave de configuración {@value #CONFIG_KEY_EXTRA_DRIVERS} (lista de FQCNs adicionales
 * separados por coma). Los drivers derivados de constantes internas (auto-detección por URL/tipo
 * o {@code BaseDatabaseConfiguration}) ya pertenecen al conjunto y no son input-driven.</p>
 */
public final class JdbcDriverAllowlist {

    /** Clave de configuración para ampliar la allowlist (CSV de FQCNs adicionales). */
    static final String CONFIG_KEY_EXTRA_DRIVERS = "db.driver.allowlist";

    /**
     * Mensaje de rechazo cuando el FQCN del driver no está permitido.
     *
     * <p>Constante nombrada (Nota Q2 — sin i18n, sin literal inline en el sitio de validación).</p>
     */
    public static final String MSG_DRIVER_NOT_ALLOWED =
        "Driver JDBC no permitido. Soportados: H2, PostgreSQL, Oracle, MySQL, SQL Server. "
        + "Para habilitar otro, agregar su FQCN a la clave de configuracion '"
        + CONFIG_KEY_EXTRA_DRIVERS + "'.";

    /** FQCNs JDBC soportados de fábrica. */
    private static final Set<String> DEFAULT_ALLOWED = Set.of(
        "org.h2.Driver",
        "org.postgresql.Driver",
        "oracle.jdbc.OracleDriver",
        "com.mysql.cj.jdbc.Driver",
        "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    );

    private JdbcDriverAllowlist() {
    }

    /**
     * Valida que {@code driverClassName} pertenezca a la allowlist.
     *
     * @param driverClassName FQCN del driver ya resuelto desde configuración (input-driven)
     * @throws SecurityException si el FQCN no está permitido; el mensaje es {@link #MSG_DRIVER_NOT_ALLOWED}
     */
    public static void requireAllowed(String driverClassName) {
        String fqcn = driverClassName == null ? "" : driverClassName.trim();
        if (!allowed().contains(fqcn)) {
            TestLogger.logWarning("DB_DRIVER_ALLOWLIST",
                "Driver JDBC rechazado por allowlist",
                Map.of("driver", fqcn));
            throw new SecurityException(MSG_DRIVER_NOT_ALLOWED);
        }
    }

    /** Conjunto efectivo = defaults + FQCNs adicionales declarados en {@link #CONFIG_KEY_EXTRA_DRIVERS}. */
    private static Set<String> allowed() {
        String extra = ConfigLoaderHolder.get().getRaw(CONFIG_KEY_EXTRA_DRIVERS, "");
        if (extra == null || extra.isBlank()) {
            return DEFAULT_ALLOWED;
        }
        Set<String> merged = new LinkedHashSet<>(DEFAULT_ALLOWED);
        for (String token : extra.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                merged.add(trimmed);
            }
        }
        return merged;
    }
}
