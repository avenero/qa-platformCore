package com.qa.databasecore.connector;

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

    /**
     * Constructor que lee la configuración desde las System Properties {@code sqlserver.db.*}.
     */
    public SQLServerConnector() {
        this(
            System.getProperty("sqlserver.db.url"),
            System.getProperty("sqlserver.db.username"),
            System.getProperty("sqlserver.db.password")
        );
    }

    /**
     * Constructor con parámetros explícitos de conexión.
     *
     * @param jdbcUrl  URL JDBC de SQL Server
     *                 (ej: {@code jdbc:sqlserver://host:port;databaseName=db})
     * @param username usuario de la base de datos
     * @param password contraseña de la base de datos
     */
    public SQLServerConnector(String jdbcUrl, String username, String password) {
        super(jdbcUrl, username, password, DRIVER_CLASS, "SQLSERVER");
    }
}

