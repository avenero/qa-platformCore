package com.qa.databasecore.connector;

import com.qa.databasecore.config.DatabaseConfig;
import com.qa.common.logging.TestLogger;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Conector base abstracto que evita duplicación de código entre conectores específicos.
 *
 * @author Abel Venero
 * @version 1.0.2
 * @since 2025-11-26
 */
abstract class BaseConnector implements DatabaseConnector {

    protected final HikariDataSource dataSource;
    protected final String connectorType;

    /**
     * Constructor que crea el DataSource con HikariCP.
     */
    protected BaseConnector(String jdbcUrl, String username, String password,
                          String driverClass, String connectorType) {
        this.connectorType = connectorType;

        validateProperties(jdbcUrl, username, password);

        this.dataSource = (HikariDataSource) DatabaseConfig.createHikariDataSource(
            jdbcUrl, username, password, driverClass
        );

        TestLogger.logInfo(connectorType + "_CONNECTOR",
            connectorType + "Connector inicializado",
            Map.of("driver", driverClass));
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            String poolName = dataSource.getPoolName();
            dataSource.close();
            TestLogger.logInfo(connectorType + "_CONNECTOR",
                "Pool de conexiones cerrado: " + poolName, null);
        }
    }

    protected void validateProperties(String jdbcUrl, String username, String password) {
        // Validar URL siempre (obligatorio)
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException(
                connectorType.toLowerCase() + ".db.url no configurada"
            );
        }

        // Detectar Windows Authentication de SQL Server
        boolean isWindowsAuth = jdbcUrl.toLowerCase().contains("integratedsecurity=true");

        if (!isWindowsAuth) {
            // Autenticación SQL estándar: validar credenciales
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    connectorType.toLowerCase() + ".db.username no configurada. " +
                    "Si usas SQL Server con Windows Authentication, " +
                    "agrega 'integratedSecurity=true' a la URL JDBC"
                );
            }
            if (password == null) {
                throw new IllegalArgumentException(
                    connectorType.toLowerCase() + ".db.password no configurada"
                );
            }
        } else {
            // Windows Authentication detectada
            TestLogger.logInfo(connectorType + "_CONNECTOR",
                "✅ Windows Authentication detectada en URL JDBC (integratedSecurity=true)",
                Map.of("info", "Username/Password no requeridos"));
        }
    }
}

