package com.qa.databasecore.connector;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interfaz principal para conectores de base de datos.
 *
 * <p>Define el contrato para gestión de conexiones a bases de datos
 * usando HikariCP como pool de conexiones.</p>
 *
 * <p><b>Implementaciones disponibles:</b></p>
 * <ul>
 *   <li>{@code OracleConnector} - Para Oracle Database</li>
 *   <li>{@code PostgreSQLConnector} - Para PostgreSQL</li>
 *   <li>{@code MySQLConnector} - Para MySQL</li>
 *   <li>{@code SQLServerConnector} - Para SQL Server</li>
 * </ul>
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * // Por tipo (desde System Properties)
 * DatabaseConnector connector = DbConnectorFactory.getConnector("oracle");
 *
 * // Con parámetros explícitos
 * DatabaseConnector connector = DbConnectorFactory.getOracleConnector(url, user, pass);
 *
 * // Usar conexión
 * Connection conn = connector.getConnection();
 * // ... ejecutar queries
 * conn.close(); // Retorna al pool
 *
 * // Cerrar pool completo
 * connector.close();
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.2
 * @since 2025-11-26
 */
public interface DatabaseConnector extends AutoCloseable {

    /**
     * Obtiene el DataSource configurado para el conector.
     *
     * @return DataSource con pool de conexiones HikariCP
     */
    DataSource getDataSource();

    /**
     * Obtiene una conexión desde el pool.
     *
     * <p><b>IMPORTANTE:</b> La conexión debe cerrarse después de usarse
     * para retornarla al pool.</p>
     *
     * @return Conexión SQL del pool
     * @throws SQLException Si ocurre un error al obtener la conexión
     */
    default Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Cierra el conector y libera todos los recursos asociados.
     *
     * <p>Cierra el pool de conexiones completamente. Todas las conexiones
     * activas serán cerradas.</p>
     */
    @Override
    void close();
}
