package com.qa.common.database.connectors;

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

    public MySQLConnector() {
        this(
            System.getProperty("mysql.db.url"),
            System.getProperty("mysql.db.username"),
            System.getProperty("mysql.db.password")
        );
    }

    public MySQLConnector(String jdbcUrl, String username, String password) {
        super(jdbcUrl, username, password, DRIVER_CLASS, "MYSQL");
    }
}

