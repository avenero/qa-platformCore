package com.qa.databasecore.connector;

/**
 * Conector específico para MySQL Database.
 *
 * <p><b>Configuración (System Properties):</b></p>
 * <pre>
 * -Dmysql.db.url=jdbc:mysql://host:port/database
 * -Dmysql.db.username=user
 * -Dmysql.db.password=password
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.2
 * @since 2025-11-26
 */
public class MySQLConnector extends BaseConnector {

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    /**
     * Constructor que lee la configuración desde las System Properties {@code mysql.db.*}.
     */
    public MySQLConnector() {
        this(
            System.getProperty("mysql.db.url"),
            System.getProperty("mysql.db.username"),
            System.getProperty("mysql.db.password")
        );
    }

    /**
     * Constructor con parámetros explícitos de conexión.
     *
     * @param jdbcUrl  URL JDBC de MySQL (ej: {@code jdbc:mysql://host:port/db})
     * @param username usuario de la base de datos
     * @param password contraseña de la base de datos
     */
    public MySQLConnector(String jdbcUrl, String username, String password) {
        super(jdbcUrl, username, password, DRIVER_CLASS, "MYSQL");
    }
}

