package com.scotia.qa.common.database.connectors;

import com.scotia.qa.common.database.config.DatabaseConfig;
import com.scotia.qa.common.database.interfaces.DatabaseConnector;
import com.scotia.qa.common.logging.TestLogger;
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
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException(
                connectorType.toLowerCase() + ".db.url no configurada"
            );
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException(
                connectorType.toLowerCase() + ".db.username no configurada"
            );
        }
        if (password == null) {
            throw new IllegalArgumentException(
                connectorType.toLowerCase() + ".db.password no configurada"
            );
        }
    }
}

