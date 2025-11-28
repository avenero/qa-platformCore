package com.scotia.qa.common.utils.testdata.service;

import com.scotia.qa.common.database.factory.DbConnectorFactory;
import com.scotia.qa.common.database.interfaces.DatabaseConnector;
import com.scotia.qa.common.database.repository.QueryRepository;
import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.utils.testdata.model.TestUser;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para buscar usuarios de prueba basado en características.
 *
 * <p>Este servicio carga queries SQL desde un archivo YAML y ejecuta
 * búsquedas en la base de datos de test usando el módulo genérico database/.</p>
 *
 * <p><b>Prerequisitos:</b></p>
 * <ul>
 *   <li>Módulo debe tener dependencia de api-core (drivers JDBC)</li>
 *   <li>Configuración de BD en System Properties</li>
 * </ul>
 *
 * <p><b>Configuración requerida (System Properties):</b></p>
 * <pre>
 * db.url=jdbc:postgresql://host:5432/test_users
 * db.username=test_user
 * db.password=test_pass
 * db.driver=org.postgresql.Driver
 * </pre>
 *
 * <p><b>Configuración de queries (YAML):</b></p>
 * <pre>
 * queries:
 *   cuenta-activa:
 *     sql: "SELECT user_id, first_name, last_name, password FROM test_users WHERE..."
 *     description: "Usuario con cuenta activa"
 *
 *   sin-prestamos:
 *     sql: "SELECT ... WHERE NOT EXISTS ..."
 *     description: "Usuario sin préstamos"
 * </pre>
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * UserFinderService service = new UserFinderService("test-data-queries.yml");
 * TestUser user = service.findUserWith("cuenta-activa");
 * </pre>
 *
 * @author Abel Venero
 * @version 1.0.2
 * @since 2025-11-26
 */
public class UserFinderService {

    private static final String DEFAULT_QUERIES_FILE = "test-data-queries.yml";

    private final DatabaseConnector connector;
    private final QueryRepository repository;
    private final Map<String, Map<String, String>> queries;
    private final String queriesFile;

    /**
     * Constructor con archivo de queries por defecto.
     * Lee configuración de BD desde System Properties.
     */
    public UserFinderService() {
        this(DEFAULT_QUERIES_FILE);
    }

    /**
     * Constructor con archivo de queries custom.
     * Lee configuración de BD desde ConfigManager (config-{env}.properties).
     *
     * @param queriesFile Nombre del archivo YAML con queries
     */
    public UserFinderService(String queriesFile) {
        this.queriesFile = queriesFile;
        this.connector = DbConnectorFactory.createFromConfig();
        this.repository = new QueryRepository(connector);
        this.queries = loadQueriesFromYaml(queriesFile);

        TestLogger.logInfo("USER_FINDER",
            "UserFinderService inicializado desde ConfigManager", null);
    }

    /**
     * Constructor con configuración de BD explícita.
     *
     * @param queriesFile Archivo YAML
     * @param jdbcUrl URL de conexión
     * @param username Usuario BD
     * @param password Contraseña BD
     * @param driver Clase del driver JDBC
     */
    public UserFinderService(String queriesFile, String jdbcUrl,
                            String username, String password, String driver) {
        this.queriesFile = queriesFile;
        this.connector = DbConnectorFactory.create(jdbcUrl, username, password, driver);
        this.repository = new QueryRepository(connector);
        this.queries = loadQueriesFromYaml(queriesFile);

        TestLogger.logInfo("USER_FINDER",
            "UserFinderService inicializado con configuración explícita", null);
    }

    /**
     * Busca un usuario CON una característica específica.
     *
     * <p>Ejecuta el query correspondiente a la característica y retorna
     * el primer usuario encontrado.</p>
     *
     * @param characteristic Característica buscada (ej: "cuenta-activa")
     * @return Usuario encontrado, o null si no hay disponibles
     * @throws RuntimeException Si el query falla o la característica no existe
     */
    public TestUser findUserWith(String characteristic) {
        TestLogger.logInfo("USER_FINDER",
            "Buscando usuario CON: " + characteristic, null);

        String sql = getQueryForCharacteristic(characteristic);

        if (sql == null) {
            throw new RuntimeException(
                "No existe query configurada para característica: " + characteristic +
                ". Verifica " + queriesFile
            );
        }

        try {
            Map<String, Object> result = repository.queryForMap(sql);

            if (result.isEmpty()) {
                TestLogger.logWarning("USER_FINDER",
                    "No se encontró usuario disponible CON: " + characteristic, null);
                return null;
            }

            TestUser user = mapToTestUser(result);

            // Crear map que acepta valores null (Map.of() no acepta nulls)
            Map<String, Object> logContext = new HashMap<>();
            logContext.put("userId", user.getUserId() != null ? user.getUserId() : "N/A");
            logContext.put("email", user.getEmail() != null ? user.getEmail() : "N/A");
            logContext.put("characteristic", characteristic);

            String displayName = user.getFullName() != null ? user.getFullName() :
                                user.getUserId() != null ? user.getUserId() : "Usuario";

            TestLogger.logInfo("USER_FINDER",
                "Usuario encontrado: " + displayName,
                logContext);

            return user;

        } catch (SQLException e) {
            TestLogger.logError("USER_FINDER",
                "Error ejecutando query para: " + characteristic + " - " + e.getMessage(), null);
            throw new RuntimeException("Error buscando usuario", e);
        }
    }

    /**
     * Busca un usuario SIN una característica específica.
     *
     * <p>Busca el query con prefijo "sin-" + característica.</p>
     *
     * @param characteristic Característica que NO debe tener (ej: "prestamos")
     * @return Usuario encontrado, o null si no hay disponibles
     * @throws RuntimeException Si el query falla o la característica no existe
     */
    public TestUser findUserWithout(String characteristic) {
        String queryKey = "sin-" + characteristic;

        TestLogger.logInfo("USER_FINDER",
            "Buscando usuario SIN: " + characteristic, null);

        String sql = getQueryForCharacteristic(queryKey);

        if (sql == null) {
            throw new RuntimeException(
                "No existe query configurada para: sin-" + characteristic +
                ". Verifica " + queriesFile
            );
        }

        try {
            Map<String, Object> result = repository.queryForMap(sql);

            if (result.isEmpty()) {
                TestLogger.logWarning("USER_FINDER",
                    "No se encontró usuario disponible SIN: " + characteristic, null);
                return null;
            }

            TestUser user = mapToTestUser(result);
            TestLogger.logInfo("USER_FINDER",
                "Usuario SIN " + characteristic + " encontrado: " + user.getFullName(),
                Map.of("userId", user.getUserId()));

            return user;

        } catch (SQLException e) {
            TestLogger.logError("USER_FINDER",
                "Error ejecutando query para: sin-" + characteristic + " - " + e.getMessage(), null);
            throw new RuntimeException("Error buscando usuario", e);
        }
    }

    /**
     * Reserva un usuario para uso exclusivo en un test.
     *
     * @param userId ID del usuario a reservar
     * @param testName Nombre del test que reserva
     * @throws RuntimeException Si falla la reserva
     */
    public void reserveUser(String userId, String testName) {
        try {
            String sql = "UPDATE test_users SET reserved_by = ?, reserved_at = CURRENT_TIMESTAMP WHERE user_id = ?";
            int rowsUpdated = repository.execute(sql, testName, userId);

            if (rowsUpdated > 0) {
                TestLogger.logInfo("USER_FINDER",
                    "Usuario reservado: " + userId,
                    Map.of("reservedBy", testName));
            } else {
                TestLogger.logWarning("USER_FINDER",
                    "No se pudo reservar usuario: " + userId, null);
            }
        } catch (SQLException e) {
            TestLogger.logError("USER_FINDER",
                "Error reservando usuario: " + userId + " - " + e.getMessage(), null);
            throw new RuntimeException("Error reservando usuario", e);
        }
    }

    /**
     * Libera un usuario para que pueda ser usado por otros tests.
     *
     * @param userId ID del usuario a liberar
     * @throws RuntimeException Si falla la liberación
     */
    public void releaseUser(String userId) {
        try {
            String sql = "UPDATE test_users SET reserved_by = NULL, reserved_at = NULL WHERE user_id = ?";
            int rowsUpdated = repository.execute(sql, userId);

            if (rowsUpdated > 0) {
                TestLogger.logInfo("USER_FINDER",
                    "Usuario liberado: " + userId, null);
            } else {
                TestLogger.logWarning("USER_FINDER",
                    "No se pudo liberar usuario: " + userId, null);
            }
        } catch (SQLException e) {
            TestLogger.logError("USER_FINDER",
                "Error liberando usuario: " + userId + " - " + e.getMessage(), null);
            throw new RuntimeException("Error liberando usuario", e);
        }
    }

    /**
     * Obtiene el SQL para una característica específica.
     */
    private String getQueryForCharacteristic(String characteristic) {
        if (queries.containsKey(characteristic)) {
            return queries.get(characteristic).get("sql");
        }
        return null;
    }

    /**
     * Carga queries desde archivo YAML.
     */
    private Map<String, Map<String, String>> loadQueriesFromYaml(String fileName) {
        try {
            TestLogger.logInfo("USER_FINDER",
                "Cargando queries desde: " + fileName, null);

            Yaml yaml = new Yaml();
            InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(fileName);

            if (inputStream == null) {
                throw new RuntimeException(
                    "Archivo no encontrado: " + fileName +
                    ". Debe estar en src/test/resources/ o src/main/resources/"
                );
            }

            Map<String, Object> config = yaml.load(inputStream);

            if (!config.containsKey("queries")) {
                throw new RuntimeException(
                    "El archivo " + fileName + " debe tener una sección 'queries'"
                );
            }

            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> loadedQueries =
                (Map<String, Map<String, String>>) config.get("queries");

            TestLogger.logInfo("USER_FINDER",
                "Queries cargadas exitosamente: " + loadedQueries.size() + " disponibles",
                Map.of("file", fileName));

            return loadedQueries;

        } catch (Exception e) {
            TestLogger.logError("USER_FINDER",
                "Error cargando queries desde: " + fileName + " - " + e.getMessage(), null);
            throw new RuntimeException("No se pudieron cargar queries", e);
        }
    }

    /**
     * Obtiene la lista de características disponibles.
     *
     * @return Map con todas las características configuradas
     */
    public Map<String, String> getAvailableCharacteristics() {
        Map<String, String> characteristics = new HashMap<>();

        queries.forEach((key, value) -> {
            String description = value.getOrDefault("description", "Sin descripción");
            characteristics.put(key, description);
        });

        return characteristics;
    }

    /**
     * Cierra el conector de base de datos.
     *
     * <p>Normalmente no es necesario llamar esto, pero puede ser útil
     * en cleanup de tests.</p>
     */
    public void close() {
        if (connector != null) {
            connector.close();
            TestLogger.logInfo("USER_FINDER", "Conector de BD cerrado", null);
        }
    }

    /**
     * Mapea un Map de ResultSet a objeto TestUser.
     *
     * @param data Map con datos del ResultSet
     * @return TestUser mapeado
     */
    private TestUser mapToTestUser(Map<String, Object> data) {
        // Mapear columnas soportando tanto mayúsculas (Oracle) como minúsculas (PostgreSQL, MySQL)
        // Oracle devuelve: ID_USER, FIRST_NAME, etc.
        // PostgreSQL/MySQL devuelven: user_id, first_name, etc.
        String userId = getStringCaseInsensitive(data, "user_id", "id_user");
        String firstName = getStringCaseInsensitive(data, "first_name");
        String lastName = getStringCaseInsensitive(data, "last_name");
        String password = getStringCaseInsensitive(data, "password");
        String email = getStringCaseInsensitive(data, "email");
        String phone = getStringCaseInsensitive(data, "phone");
        String idUserStatus = getStringCaseInsensitive(data, "id_user_status");
        String idDefaultEnvironment = getStringCaseInsensitive(data, "id_default_environment");
        String lastLogin = getStringCaseInsensitive(data, "last_login");
        String requestedSoftToken = getStringCaseInsensitive(data, "requested_soft_token");

        TestUser user = new TestUser(userId, firstName, lastName, password,
                                     email, phone, idUserStatus, idDefaultEnvironment,
                                     lastLogin, requestedSoftToken);

        // Guardar columnas adicionales como metadata
        data.forEach((key, value) -> {
            if (!isStandardColumn(key)) {
                user.setAdditionalData(key, value);
            }
        });

        return user;
    }

    /**
     * Helper para obtener String de Map con búsqueda case-insensitive.
     * Busca la columna en varios formatos: minúsculas, mayúsculas, alternativo.
     *
     * @param data Map con los datos
     * @param keys Nombres de columna a buscar (en orden de prioridad)
     * @return valor como String o null
     */
    private String getStringCaseInsensitive(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            // Buscar tal cual
            Object value = data.get(key);
            if (value != null) {
                return value.toString();
            }

            // Buscar en mayúsculas (Oracle)
            value = data.get(key.toUpperCase());
            if (value != null) {
                return value.toString();
            }

            // Buscar en minúsculas
            value = data.get(key.toLowerCase());
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    /**
     * Verifica si una columna es estándar (no debe guardarse en additionalData).
     * Soporta tanto minúsculas (PostgreSQL, MySQL) como mayúsculas (Oracle).
     */
    private boolean isStandardColumn(String columnName) {
        String lower = columnName.toLowerCase();
        return lower.equals("user_id") || lower.equals("id_user") ||
               lower.equals("first_name") || lower.equals("last_name") ||
               lower.equals("password") || lower.equals("email") ||
               lower.equals("phone") || lower.equals("id_user_status") ||
               lower.equals("id_default_environment") || lower.equals("last_login") ||
               lower.equals("requested_soft_token") || lower.equals("reserved_by") ||
               lower.equals("reserved_at");
    }
}

