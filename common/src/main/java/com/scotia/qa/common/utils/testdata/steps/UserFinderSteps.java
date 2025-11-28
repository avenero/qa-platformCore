package com.scotia.qa.common.utils.testdata.steps;

import com.scotia.qa.common.cucumber.context.ScenarioContext;
import com.scotia.qa.common.http.exceptions.FrameworkBusinessException;
import com.scotia.qa.common.logging.TestLogger;
import com.scotia.qa.common.utils.testdata.model.TestUser;
import com.scotia.qa.common.utils.testdata.service.UserFinderService;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Steps de Cucumber para buscar y gestionar usuarios de prueba desde base de datos.
 *
 * <p>Estos steps permiten buscar usuarios con/sin características específicas
 * y los guarda en ScenarioContext para uso en otros steps.</p>
 *
 * <p><b>Prerequisitos:</b></p>
 * <ul>
 *   <li>Módulo debe tener dependencia de api-core (para HikariCP y drivers JDBC)</li>
 *   <li>Archivo test-data-queries.yml en src/test/resources/</li>
 *   <li>Configuración de BD en system properties o application.properties</li>
 * </ul>
 *
 * <p><b>Steps disponibles:</b></p>
 * <pre>
 * Given obtengo usuario con "cuenta-activa" disponible
 * Given obtengo usuario sin "prestamos" disponible
 * </pre>
 *
 * <p><b>Datos guardados en ScenarioContext:</b></p>
 * <ul>
 *   <li>testdata.user → Objeto TestUser completo</li>
 *   <li>testdata.firstName → Primer nombre del usuario</li>
 *   <li>testdata.lastName → Apellido del usuario</li>
 *   <li>testdata.fullName → Nombre completo (firstName + lastName)</li>
 *   <li>testdata.password → Password del usuario</li>
 *   <li>testdata.userId → ID del usuario</li>
 *   <li>testdata.email → Email (si está disponible)</li>
 *   <li>testdata.phone → Teléfono (si está disponible)</li>
 * </ul>
/**
 * @author Abnel Venero
 * @version 1.0.0
 * @since 2025-11-26
 */
public class UserFinderSteps {

    private static UserFinderService userService;
    private final List<String> reservedUserIds;

    public UserFinderSteps() {
        this.reservedUserIds = new ArrayList<>();
    }

    /**
     * Hook que se ejecuta SOLO si el escenario tiene tags relacionados con Database.
     * Esto evita inicializar conexión a BD innecesariamente en tests que no usan test data.
     *
     * Tags soportados: @database, @db, @sql, @testdata
     */
    @io.cucumber.java.Before(value = "@database or @db or @sql or @testdata", order = 10)
    public void beforeScenario(io.cucumber.java.Scenario scenario) {
        // Detectar nombre del módulo dinámicamente (ej: BANKING, AUTOS, etc.)
        String moduleName = com.scotia.qa.common.logging.ModuleDetector.detectModuleName();
        com.scotia.qa.common.logging.TestLogger.setFramework(moduleName);

        // Validar consistencia de tags del scenario
        com.scotia.qa.common.cucumber.validators.HookValidator.validateScenario(scenario);

        // Lazy init del servicio (solo primera vez y solo si tiene tags database)
        if (userService == null) {
            initializeService();
        }
    }

    /**
     * Step: Obtiene un usuario CON una característica específica.
     *
     * <p>Busca en la base de datos un usuario que cumpla con la característica
     * especificada, lo reserva automáticamente y lo guarda en ScenarioContext.</p>
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * Given obtengo usuario con "cuenta-activa" disponible
     * Given obtengo usuario con "tarjeta-credito" disponible
     * Given obtengo usuario con "prestamo-vigente" disponible
     * </pre>
     *
     * @param caracteristica Característica que debe tener el usuario
     * @throws RuntimeException Si no se encuentra usuario o hay error en BD
     */
    @Given("obtengo usuario con {string} disponible")
    public void obtengoUsuarioConCaracteristica(String caracteristica) {
        try {
            TestLogger.logInfo("USER_FINDER_STEPS",
                "Buscando usuario CON: " + caracteristica, null);

            // Buscar usuario
            TestUser user = userService.findUserWith(caracteristica);

            if (user == null) {
                throw new RuntimeException(
                    "❌ No se encontró usuario disponible con característica: " + caracteristica +
                    ". Verifica que existan usuarios en BD que cumplan el criterio."
                );
            }

            // Reservar usuario (marca como "en uso")
            String testName = obtenerNombreTest();
            userService.reserveUser(user.getUserId(), testName);

            // Trackear para liberar después
            reservedUserIds.add(user.getUserId());

            // Guardar en ScenarioContext (disponible en todos los steps)
            guardarUsuarioEnContexto(user, caracteristica);

            TestLogger.logInfo("USER_FINDER_STEPS",
                "✅ Usuario obtenido y reservado: " + user.getFullName(),
                Map.of(
                    "userId", user.getUserId(),
                    "caracteristica", caracteristica,
                    "testName", testName
                ));

        } catch (Exception e) {
            TestLogger.logError("USER_FINDER_STEPS",
                "Error obteniendo usuario CON: " + caracteristica + " - " + e.getMessage(), null);
            throw new RuntimeException(
                "No se pudo obtener usuario con característica: " + caracteristica, e
            );
        }
    }

    /**
     * Step: Obtiene un usuario SIN una característica específica.
     *
     * <p>Busca en la base de datos un usuario que NO tenga la característica
     * especificada, lo reserva automáticamente y lo guarda en ScenarioContext.</p>
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * Given obtengo usuario sin "prestamos" disponible
     * Given obtengo usuario sin "tarjetas" disponible
     * Given obtengo usuario sin "productos" disponible
     * </pre>
     *
     * @param caracteristica Característica que NO debe tener el usuario
     * @throws RuntimeException Si no se encuentra usuario o hay error en BD
     */
    @Given("obtengo usuario sin {string} disponible")
    public void obtengoUsuarioSinCaracteristica(String caracteristica) {
        try {
            TestLogger.logInfo("USER_FINDER_STEPS",
                "Buscando usuario SIN: " + caracteristica, null);

            // Buscar usuario
            TestUser user = userService.findUserWithout(caracteristica);

            if (user == null) {
                throw new RuntimeException(
                    "❌ No se encontró usuario disponible SIN característica: " + caracteristica +
                    ". Verifica que existan usuarios en BD que cumplan el criterio."
                );
            }

            // Reservar usuario
            String testName = obtenerNombreTest();
            userService.reserveUser(user.getUserId(), testName);

            // Trackear para liberar después
            reservedUserIds.add(user.getUserId());

            // Guardar en ScenarioContext
            guardarUsuarioEnContexto(user, "sin-" + caracteristica);

            TestLogger.logInfo("USER_FINDER_STEPS",
                "✅ Usuario SIN " + caracteristica + " obtenido: " + user.getFullName(),
                Map.of(
                    "userId", user.getUserId(),
                    "caracteristica", "sin-" + caracteristica,
                    "testName", testName
                ));

        } catch (Exception e) {
            TestLogger.logError("USER_FINDER_STEPS",
                "Error obteniendo usuario SIN: " + caracteristica + " - " + e.getMessage(), null);
            throw new RuntimeException(
                "No se pudo obtener usuario sin característica: " + caracteristica, e
            );
        }
    }

    /**
     * Hook que se ejecuta DESPUÉS de cada escenario para liberar usuarios reservados.
     *
     * <p>Esto asegura que los usuarios queden disponibles para otros tests,
     * incluso si el test falló.</p>
     */
    @After(order = 100) // Order alto para ejecutar después de otros hooks
    public void liberarUsuariosReservados() {
        if (reservedUserIds.isEmpty()) {
            return;
        }

        TestLogger.logInfo("USER_FINDER_STEPS",
            "Liberando " + reservedUserIds.size() + " usuario(s) reservado(s)", null);

        for (String userId : reservedUserIds) {
            try {
                userService.releaseUser(userId);

                TestLogger.logDebug("USER_FINDER_STEPS",
                    "Usuario liberado: " + userId, null);

            } catch (Exception e) {
                TestLogger.logWarning("USER_FINDER_STEPS",
                    "Error liberando usuario: " + userId + " - " + e.getMessage(), null);
            }
        }

        // Limpiar lista
        reservedUserIds.clear();
    }

    /**
     * Guarda usuario en ScenarioContext para uso en otros steps.
     */
    private void guardarUsuarioEnContexto(TestUser user, String caracteristica) throws FrameworkBusinessException {
        // Objeto completo
        ScenarioContext.setByLayer("testdata", "user", user);

        // Campos individuales (para fácil acceso)
        ScenarioContext.setByLayer("testdata", "firstName", user.getFirstName());
        ScenarioContext.setByLayer("testdata", "lastName", user.getLastName());
        ScenarioContext.setByLayer("testdata", "fullName", user.getFullName());
        ScenarioContext.setByLayer("testdata", "password", user.getPassword());
        ScenarioContext.setByLayer("testdata", "userId", user.getUserId());

        // Campos opcionales
        if (user.getEmail() != null) {
            ScenarioContext.setByLayer("testdata", "email", user.getEmail());
        }
        if (user.getPhone() != null) {
            ScenarioContext.setByLayer("testdata", "phone", user.getPhone());
        }
        if (user.getIdUserStatus() != null) {
            ScenarioContext.setByLayer("testdata", "idUserStatus", user.getIdUserStatus());
        }
        if (user.getIdDefaultEnvironment() != null) {
            ScenarioContext.setByLayer("testdata", "idDefaultEnvironment", user.getIdDefaultEnvironment());
        }
        if (user.getLastLogin() != null) {
            ScenarioContext.setByLayer("testdata", "lastLogin", user.getLastLogin());
        }
        if (user.getRequestedSoftToken() != null) {
            ScenarioContext.setByLayer("testdata", "requestedSoftToken", user.getRequestedSoftToken());
        }

        // Metadata
        ScenarioContext.setByLayer("testdata", "caracteristica", caracteristica);

        // Datos adicionales (si existen)
        user.getAllAdditionalData().forEach((key, value) ->
                {
                    try {
                        ScenarioContext.setByLayer("testdata", key, value);
                    } catch (FrameworkBusinessException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    /**
     * Obtiene el nombre del test actual para tracking.
     */
    private String obtenerNombreTest() {
        // Intentar obtener desde ScenarioContext
        Object testName = ScenarioContext.getFromAnyLayer("testName");

        if (testName != null) {
            return testName.toString();
        }

        // Fallback a thread name
        return Thread.currentThread().getName();
    }

    /**
     * Inicializa el servicio UserFinder.
     */
    private void initializeService() {
        try {
            TestLogger.logInfo("USER_FINDER_STEPS",
                "Inicializando UserFinderService...", null);

            // Intentar cargar queries custom del módulo, si no existe usa default
            String queriesFile = System.getProperty("test.data.queries.file",
                "test-data-queries.yml");

            userService = new UserFinderService(queriesFile);

            // Log características disponibles
            Map<String, String> characteristics = userService.getAvailableCharacteristics();

            TestLogger.logInfo("USER_FINDER_STEPS",
                "UserFinderService inicializado correctamente",
                Map.of(
                    "queriesFile", queriesFile,
                    "characteristicsAvailable", characteristics.size()
                ));

        } catch (Exception e) {
            TestLogger.logError("USER_FINDER_STEPS",
                "Error inicializando UserFinderService: " + e.getMessage(), null);
            throw new RuntimeException(
                "No se pudo inicializar sistema de búsqueda de usuarios. " +
                "Verifica: (1) Dependencia api-core, (2) test-data-queries.yml, " +
                "(3) Configuración de BD", e
            );
        }
    }
}

