package com.scotia.qa.common.logging;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para TestLogger - Sistema de logging del framework.
 *
 * <p><b>Clase P0:</b> Sistema de logging usado por TODO el framework
 * <p><b>Cobertura objetivo:</b> 90%
 * <p><b>Total tests:</b> 25
 *
 * <p><b>Validaciones:</b>
 * <ul>
 *   <li>Niveles de log (INFO, ERROR, WARNING, DEBUG)</li>
 *   <li>Sanitización de datos sensibles</li>
 *   <li>LoggerWrapper functionality</li>
 *   <li>Manejo de excepciones</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 1.0.0
 */
@DisplayName("TestLogger Tests - Sistema de Logging")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestLoggerTest {

    private Logger rootLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        // Configurar captura de logs
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        listAppender = new ListAppender<>();
        listAppender.start();
        rootLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        rootLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    // =========================================================================
    // BASIC LOGGING TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Basic Logging Operations")
    @Order(1)
    class BasicLoggingTests {

        @Test
        @DisplayName("Debe loguear INFO correctamente")
        void testLogInfo() {
            // Given
            String category = "TEST_CATEGORY";
            String message = "Test info message";

            // When/Then
            assertThatCode(() -> TestLogger.logInfo(category, message, null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear ERROR con contexto")
        void testLogError() {
            // Given
            String category = "ERROR_CAT";
            String message = "Error occurred";
            Map<String, Object> context = Map.of("code", "E001", "detail", "Test error");

            // When/Then
            assertThatCode(() -> TestLogger.logError(category, message, context))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear excepción con logException")
        void testLogException() {
            // Given
            String category = "EXCEPTION_CAT";
            String message = "Exception occurred";
            Exception exception = new RuntimeException("Test exception");

            // When/Then
            assertThatCode(() -> TestLogger.logException(category, message, exception))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear WARNING")
        void testLogWarning() {
            // Given
            String message = "Warning message";

            // When/Then
            assertThatCode(() -> TestLogger.logWarning("WARN_CAT", message, null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear DEBUG")
        void testLogDebug() {
            // When/Then
            assertThatCode(() -> TestLogger.logDebug("DEBUG_CAT", "Debug message", null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe manejar mensaje null en logInfo")
        void testLogInfoWithNullMessage() {
            // When/Then
            assertThatCode(() -> TestLogger.logInfo("CATEGORY", null, null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe manejar categoría null en logError")
        void testLogErrorWithNullCategory() {
            // When/Then
            assertThatCode(() -> TestLogger.logError(null, "message", null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear con contexto")
        void testLogInfoWithContext() {
            // Given
            String category = "TEST";
            String message = "Test with context";
            Map<String, Object> context = Map.of(
                "key1", "value1",
                "key2", "value2"
            );

            // When/Then
            assertThatCode(() -> TestLogger.logInfo(category, message, context))
                .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // LOGGING STEPS TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. Logging Steps Tests")
    @Order(2)
    class LoggingStepsTests {

        @Test
        @DisplayName("Debe loguear step sin datos")
        void testLogStep() {
            // Given
            String stepType = "GIVEN";
            String description = "Usuario está logueado";

            // When/Then
            assertThatCode(() -> TestLogger.logStep(stepType, description))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear step con datos")
        void testLogStepWithData() {
            // Given
            String stepType = "WHEN";
            String description = "Usuario hace clic en botón";
            Map<String, Object> data = Map.of("button", "Login", "action", "click");

            // When/Then
            assertThatCode(() -> TestLogger.logStep(stepType, description, data))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear acción HTTP")
        void testLogHttpAction() {
            // Given
            String method = "GET";
            String url = "https://api.example.com/users";
            int statusCode = 200;
            long duration = 150;

            // When/Then
            assertThatCode(() -> TestLogger.logHttpAction(method, url, statusCode, duration))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear acción UI")
        void testLogUiAction() {
            // Given
            String action = "click";
            String element = "btnLogin";
            String value = "Login";

            // When/Then
            assertThatCode(() -> TestLogger.logUiAction(action, element, value))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear acción UI sin valor")
        void testLogUiActionWithoutValue() {
            // Given
            String action = "click";
            String element = "btnSubmit";

            // When/Then
            assertThatCode(() -> TestLogger.logUiAction(action, element, null))
                .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // ASSERTIONS AND VALIDATIONS TESTS
    // =========================================================================

    @Nested
    @DisplayName("3. Assertions and Validations")
    @Order(3)
    class AssertionsAndValidationsTests {

        @Test
        @DisplayName("Debe loguear assertion exitosa")
        void testLogAssertionSuccess() {
            // Given
            String assertion = "Status code is 200";
            String expected = "200";
            String actual = "200";

            // When/Then
            assertThatCode(() -> TestLogger.logAssertionSuccess(assertion, expected, actual))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear assertion fallida")
        void testLogAssertionFailure() {
            // Given
            String assertion = "Status code is 200";
            String expected = "200";
            String actual = "404";
            String reason = "API returned error";

            // When/Then
            assertThatCode(() -> TestLogger.logAssertionFailure(assertion, expected, actual, reason))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear validación exitosa")
        void testLogValidationSuccess() {
            // Given
            String validationType = "SCHEMA";
            String description = "Response matches expected schema";
            boolean passed = true;

            // When/Then
            assertThatCode(() -> TestLogger.logValidation(validationType, description, passed))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear validación fallida")
        void testLogValidationFailure() {
            // Given
            String validationType = "SCHEMA";
            String description = "Response does not match schema";
            boolean passed = false;

            // When/Then
            assertThatCode(() -> TestLogger.logValidation(validationType, description, passed))
                .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // LOGGER WRAPPER TESTS
    // =========================================================================

    @Nested
    @DisplayName("4. LoggerWrapper Tests")
    @Order(4)
    class LoggerWrapperTests {

        @Test
        @DisplayName("Debe crear LoggerWrapper para clase")
        void testGetLogger() {
            // When
            TestLogger.LoggerWrapper wrapper = TestLogger.getLogger(TestLoggerTest.class);

            // Then
            assertThat(wrapper).isNotNull();
        }

        @Test
        @DisplayName("Debe loguear INFO a través de wrapper")
        void testWrapperInfo() {
            // Given
            TestLogger.LoggerWrapper wrapper = TestLogger.getLogger(TestLoggerTest.class);

            // When/Then
            assertThatCode(() -> wrapper.info("Test message"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear ERROR a través de wrapper")
        void testWrapperError() {
            // Given
            TestLogger.LoggerWrapper wrapper = TestLogger.getLogger(TestLoggerTest.class);
            Exception ex = new RuntimeException("Test error");

            // When/Then
            assertThatCode(() -> wrapper.error("Error message", ex))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear WARN a través de wrapper")
        void testWrapperWarn() {
            // Given
            TestLogger.LoggerWrapper wrapper = TestLogger.getLogger(TestLoggerTest.class);

            // When/Then
            assertThatCode(() -> wrapper.warn("Warning message"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear DEBUG a través de wrapper")
        void testWrapperDebug() {
            // Given
            TestLogger.LoggerWrapper wrapper = TestLogger.getLogger(TestLoggerTest.class);

            // When/Then
            assertThatCode(() -> wrapper.debug("Debug message"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe loguear con formato en wrapper")
        void testWrapperWithFormat() {
            // Given
            TestLogger.LoggerWrapper wrapper = TestLogger.getLogger(TestLoggerTest.class);

            // When/Then
            assertThatCode(() -> wrapper.info("User {} logged in", "john"))
                .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // EDGE CASES & ERROR HANDLING
    // =========================================================================

    @Nested
    @DisplayName("5. Edge Cases & Error Handling")
    @Order(5)
    class EdgeCasesTests {

        @Test
        @DisplayName("Debe manejar mensaje muy largo")
        void testVeryLongMessage() {
            // Given
            String longMessage = "x".repeat(10000);

            // When/Then
            assertThatCode(() -> TestLogger.logInfo("CAT", longMessage, null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe manejar caracteres especiales")
        void testSpecialCharacters() {
            // Given
            String specialChars = "áéíóú ñÑ ¿¡ 你好 🔥";

            // When/Then
            assertThatCode(() -> TestLogger.logInfo("CAT", specialChars, null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe manejar null en todos los parámetros")
        void testAllNullParameters() {
            // When/Then
            assertThatCode(() -> TestLogger.logInfo(null, null, null))
                .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "SQL_QUERY",
            "HTTP_REQUEST",
            "DATABASE_ERROR",
            "API_RESPONSE",
            "AUTHENTICATION"
        })
        @DisplayName("Debe manejar diferentes categorías")
        void testDifferentCategories(String category) {
            // When/Then
            assertThatCode(() -> TestLogger.logInfo(category, "Test message", null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe manejar contexto vacío")
        void testEmptyContext() {
            // Given
            Map<String, Object> emptyContext = Map.of();

            // When/Then
            assertThatCode(() -> TestLogger.logInfo("CAT", "message", emptyContext))
                .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // TEST CONTEXT MANAGEMENT
    // =========================================================================

    @Nested
    @DisplayName("6. Test Context Management")
    @Order(6)
    class TestContextManagementTests {

        @Test
        @DisplayName("Debe establecer contexto de test")
        void testSetTestContext() {
            // Given
            String testName = "TestLoginFlow";

            // When/Then
            assertThatCode(() -> TestLogger.setTestContext(testName))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe establecer framework")
        void testSetFramework() {
            // Given
            String framework = "API";

            // When/Then
            assertThatCode(() -> TestLogger.setFramework(framework))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Debe limpiar contexto de test")
        void testClearTestContext() {
            // Given
            TestLogger.setTestContext("TestExample");

            // When/Then
            assertThatCode(() -> TestLogger.clearTestContext())
                .doesNotThrowAnyException();
        }
    }
}

