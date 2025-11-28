package com.scotia.qa.common.database.connectors;

/**
 * Conector específico para Oracle Database.
 *
 * <p><b>Configuración (System Properties):</b></p>
 * <pre>
 * -Doracle.db.url=jdbc:oracle:thin:@//host:port/service
 * -Doracle.db.username=user
 * -Doracle.db.password=password
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.2
 * @since 2025-11-26
 */
public class OracleConnector extends BaseConnector {

    private static final String DRIVER_CLASS = "oracle.jdbc.OracleDriver";

    public OracleConnector() {
        this(
            System.getProperty("oracle.db.url"),
            System.getProperty("oracle.db.username"),
            System.getProperty("oracle.db.password")
        );
    }

    public OracleConnector(String jdbcUrl, String username, String password) {
        super(jdbcUrl, username, password, DRIVER_CLASS, "ORACLE");
    }
}

