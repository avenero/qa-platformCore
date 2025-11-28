package com.scotia.qa.common.database.connectors;

/**
 * Conector específico para SQL Server Database.
 *
 * <p><b>Configuración (System Properties):</b></p>
 * <pre>
 * -Dsqlserver.db.url=jdbc:sqlserver://host:port;databaseName=db
 * -Dsqlserver.db.username=user
 * -Dsqlserver.db.password=password
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.2
 * @since 2025-11-26
 */
public class SQLServerConnector extends BaseConnector {

    private static final String DRIVER_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    public SQLServerConnector() {
        this(
            System.getProperty("sqlserver.db.url"),
            System.getProperty("sqlserver.db.username"),
            System.getProperty("sqlserver.db.password")
        );
    }

    public SQLServerConnector(String jdbcUrl, String username, String password) {
        super(jdbcUrl, username, password, DRIVER_CLASS, "SQLSERVER");
    }
}

