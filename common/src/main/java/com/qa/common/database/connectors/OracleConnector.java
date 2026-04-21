package com.qa.common.database.connectors;

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

    /**
     * Constructor que lee la configuración desde las System Properties {@code oracle.db.*}.
     */
    public OracleConnector() {
        this(
            System.getProperty("oracle.db.url"),
            System.getProperty("oracle.db.username"),
            System.getProperty("oracle.db.password")
        );
    }

    /**
     * Constructor con parámetros explícitos de conexión.
     *
     * @param jdbcUrl  URL JDBC de Oracle (ej: {@code jdbc:oracle:thin:@//host:port/service})
     * @param username usuario de la base de datos
     * @param password contraseña de la base de datos
     */
    public OracleConnector(String jdbcUrl, String username, String password) {
        super(jdbcUrl, username, password, DRIVER_CLASS, "ORACLE");
    }
}

