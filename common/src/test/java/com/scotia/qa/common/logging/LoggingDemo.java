package com.scotia.qa.common.logging;

import org.junit.jupiter.api.*;

import java.util.Map;

/**
 * Clase de demostración del sistema de logging.
 * Ejecutar este test para ver cómo funcionan los logs con el nuevo sistema.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoggingDemo {

    @BeforeAll
    public static void setupModule() {
        System.out.println("\n========================================");
        System.out.println("  DEMO - Sistema de Logging Scotia QA");
        System.out.println("========================================\n");

        // Inicializar contexto del módulo
        LoggingInitializer.initModuleContext("API");
        LoggingInitializer.setEnvironment("qa");
    }

    @BeforeEach
    public void setupTest(TestInfo testInfo) {
        TestLogger.setTestContext(testInfo.getDisplayName());
    }

    @AfterEach
    public void cleanup() {
        TestLogger.clearTestContext();
        System.out.println(); // Línea en blanco entre tests
    }

    @Test
    @Order(1)
    @DisplayName("Demo 1: Logging Básico")
    public void demoLoggingBasico() {
        TestLogger.logInfo("DEMO", "Este es un mensaje de información básico", null);
        TestLogger.logDebug("DEMO", "Este es un mensaje de debug", null);
        TestLogger.logWarning("DEMO", "Este es un mensaje de advertencia", null);
    }

    @Test
    @Order(2)
    @DisplayName("Demo 2: Logging de Steps")
    public void demoLoggingSteps() {
        TestLogger.logStep("GIVEN", "Usuario válido configurado en el sistema");
        TestLogger.logStep("AND", "El usuario tiene permisos de administrador");
        TestLogger.logStep("WHEN", "El usuario intenta acceder al dashboard");
        TestLogger.logStep("THEN", "El dashboard se muestra correctamente");
        TestLogger.logStep("AND", "Todos los widgets están visibles");
    }

    @Test
    @Order(3)
    @DisplayName("Demo 3: Logging de HTTP Actions")
    public void demoHttpActions() {
        // Simular requests
        TestLogger.logHttpAction("GET", "/api/users", 200, 125);
        TestLogger.logHttpAction("POST", "/api/users", 201, 235);
        TestLogger.logHttpAction("PUT", "/api/users/123", 200, 180);
        TestLogger.logHttpAction("DELETE", "/api/users/123", 204, 95);
        TestLogger.logHttpAction("GET", "/api/users/999", 404, 45);
    }

    @Test
    @Order(4)
    @DisplayName("Demo 4: Logging con Contexto")
    public void demoLoggingConContexto() {
        Map<String, Object> userContext = Map.of(
            "userId", "12345",
            "username", "testuser",
            "email", "test@example.com",
            "role", "admin"
        );

        TestLogger.logInfo("USER", "Usuario autenticado exitosamente", userContext);

        Map<String, Object> apiContext = Map.of(
            "endpoint", "/api/auth/login",
            "method", "POST",
            "statusCode", 200,
            "duration", 250
        );

        TestLogger.logInfo("API", "Request completado", apiContext);
    }

    @Test
    @Order(5)
    @DisplayName("Demo 5: Logging de Validaciones")
    public void demoValidaciones() {
        TestLogger.logAssertionSuccess(
            "Status code debe ser 200",
            "200",
            "200"
        );

        TestLogger.logAssertionSuccess(
            "Response body contiene campo 'id'",
            "campo 'id' presente",
            "campo 'id' encontrado"
        );

        TestLogger.logValidation(
            "RESPONSE_TIME",
            "El tiempo de respuesta debe ser menor a 1 segundo",
            true
        );

        TestLogger.logValidation(
            "DATA_FORMAT",
            "Los datos deben estar en formato JSON",
            true
        );
    }

    @Test
    @Order(6)
    @DisplayName("Demo 6: Logging de UI Actions")
    public void demoUiActions() {
        TestLogger.logUiAction("NAVIGATE", "https://example.com/login", null);
        TestLogger.logUiAction("TYPE", "input#username", "testuser");
        TestLogger.logUiAction("TYPE", "input#password", "***");
        TestLogger.logUiAction("CLICK", "button#loginButton", null);
        TestLogger.logUiAction("WAIT", "div#dashboard", "5 segundos");
        TestLogger.logUiAction("VERIFY", "h1.title", "Dashboard visible");
    }

    @Test
    @Order(7)
    @DisplayName("Demo 7: Sanitización Automática")
    public void demoSanitizacion() {
        // Datos sensibles se sanitizan automáticamente
        Map<String, Object> sensitiveData = Map.of(
            "username", "admin",
            "password", "super-secret-password-123",
            "token", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            "apiKey", "sk_live_abc123xyz789",
            "normalData", "Este dato no es sensible"
        );

        TestLogger.logInfo("SECURITY", "Datos de autenticación (sanitizados)", sensitiveData);
    }

    @Test
    @Order(8)
    @DisplayName("Demo 8: Manejo de Errores")
    public void demoManejoErrores() {
        // Simular warning
        Map<String, Object> warningContext = Map.of(
            "endpoint", "/api/deprecated",
            "reason", "Este endpoint está deprecado"
        );
        TestLogger.logWarning("API", "Usando endpoint deprecado", warningContext);

        // Simular error (sin exception)
        Map<String, Object> errorContext = Map.of(
            "endpoint", "/api/users",
            "statusCode", 500,
            "message", "Internal Server Error"
        );
        TestLogger.logError("API", "Request falló con error 500", errorContext);

        // Simular exception
        try {
            throw new RuntimeException("Este es un error de ejemplo para demostración");
        } catch (Exception e) {
            TestLogger.logException("DEMO", "Excepción capturada durante la ejecución", e);
        }
    }

    @Test
    @Order(9)
    @DisplayName("Demo 9: API Wrapper Tradicional")
    public void demoApiTradicional() {
        TestLogger.LoggerWrapper logger = TestLogger.getLogger(LoggingDemo.class);

        logger.info("Este es un mensaje usando la API tradicional");
        logger.debug("Debug con parámetros: {} y {}", "valor1", "valor2");
        logger.warn("Advertencia usando wrapper");

        try {
            throw new IllegalStateException("Error de ejemplo para wrapper");
        } catch (Exception e) {
            logger.error("Error capturado con wrapper", e);
        }
    }

    @Test
    @Order(10)
    @DisplayName("Demo 10: Escenario Completo")
    public void demoEscenarioCompleto() {
        // Simular un flujo completo de test
        TestLogger.logStep("GIVEN", "Usuario con credenciales válidas");
        TestLogger.logInfo("SETUP", "Preparando datos de prueba", Map.of(
            "username", "testuser",
            "environment", "qa"
        ));

        TestLogger.logStep("WHEN", "El usuario envía request de login");
        TestLogger.logHttpAction("POST", "/api/auth/login", 200, 350);

        TestLogger.logStep("THEN", "El sistema responde con token válido");
        TestLogger.logAssertionSuccess(
            "Status code es 200",
            "200",
            "200"
        );

        TestLogger.logValidation(
            "RESPONSE_CONTENT",
            "Response contiene token de acceso",
            true
        );

        TestLogger.logStep("AND", "El usuario puede acceder al dashboard");
        TestLogger.logHttpAction("GET", "/api/dashboard", 200, 150);

        TestLogger.logInfo("TEST", "Test completado exitosamente", Map.of(
            "totalRequests", 2,
            "totalDuration", 500,
            "status", "PASS"
        ));
    }

    @AfterAll
    public static void tearDown() {
        System.out.println("\n========================================");
        System.out.println("  DEMO Completada");
        System.out.println("  Revisa los logs generados en:");
        System.out.println("  - Consola (arriba)");
        System.out.println("  - ./logs/api/api-tests.log");
        System.out.println("========================================\n");

        LoggingInitializer.clearAllContext();
    }
}

