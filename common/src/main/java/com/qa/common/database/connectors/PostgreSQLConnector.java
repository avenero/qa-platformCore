package com.qa.common.database.connectors;

/**
 * Conector específico para PostgreSQL Database.
 *
 * <p><b>Configuración (System Properties):</b></p>
 * <pre>
 * -Dpostgresql.db.url=jdbc:postgresql://host:port/database
 * -Dpostgresql.db.username=user
 * -Dpostgresql.db.password=password
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.2
 * @since 2025-11-26
 */
public class PostgreSQLConnector extends BaseConnector {

    private static final String DRIVER_CLASS = "org.postgresql.Driver";

    public PostgreSQLConnector() {
        this(
            System.getProperty("postgresql.db.url"),
            System.getProperty("postgresql.db.username"),
            System.getProperty("postgresql.db.password")
        );
    }

    public PostgreSQLConnector(String jdbcUrl, String username, String password) {
        super(jdbcUrl, username, password, DRIVER_CLASS, "POSTGRESQL");
    }
}

