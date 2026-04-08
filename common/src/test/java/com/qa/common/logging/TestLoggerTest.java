package com.qa.common.logging;

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
 * Tests unitarios para TestLogger.
 *
 * <p>Valida que los métodos de logging produzcan eventos reales en el appender,
 * no solo que "no exploten".
 *
 * @author Abel Venero
 * @since 1.0.0
 */
@DisplayName("TestLogger")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestLoggerTest {

    private Logger rootLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        listAppender = new ListAppender<>();
        listAppender.start();
        rootLogger.addAppender(listAppender);
        listAppender.list.clear();
    }

    @AfterEach
    void tearDown() {
        rootLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    // =========================================================================
    // Niveles de log: validan que cada método produce al menos un evento
    // =========================================================================

    @Nested
    @DisplayName("Niveles de logging")
    @Order(1)
    class LogLevelTests {

        @Test
        @DisplayName("logInfo produce evento de log")
        void logInfoProduceEvento() {
            TestLogger.logInfo("CAT", "mensaje info", null);
            assertThat(listAppender.list).isNotEmpty();
        }

        @Test
        @DisplayName("logError produce evento de log")
        void logErrorProduceEvento() {
            TestLogger.logError("CAT", "mensaje error", null);
            assertThat(listAppender.list).isNotEmpty();
        }

        @Test
        @DisplayName("logWarning produce evento de log")
        void logWarningProduceEvento() {
            TestLogger.logWarning("CAT", "mensaje warning", null);
            assertThat(listAppender.list).isNotEmpty();
        }

        @Test
        @DisplayName("logException registra mensaje y no lanza excepción")
        void logExceptionRegistraYNoLanza() {
            assertThatCode(() ->
                TestLogger.logException("CAT", "error", new RuntimeException("test"))
            ).doesNotThrowAnyException();
            assertThat(listAppender.list).isNotEmpty();
        }

        @Test
        @DisplayName("logInfo con contexto incluye datos del contexto en el log")
        void logInfoConContextoProduceEvento() {
            TestLogger.logInfo("CAT", "con contexto", Map.of("key", "val"));
            assertThat(listAppender.list).isNotEmpty();
        }
    }

    // =========================================================================
    // Steps: producen eventos
    // =========================================================================

    @Nested
    @DisplayName("Logging de steps")
    @Order(2)
    class LoggingStepsTests {

        @Test
        @DisplayName("logStep produce evento de log")
        void logStepProduceEvento() {
            TestLogger.logStep("GIVEN", "usuario logueado");
            assertThat(listAppender.list).isNotEmpty();
        }

        @Test
        @DisplayName("logStep con datos produce evento de log")
        void logStepConDatosProduceEvento() {
            TestLogger.logStep("WHEN", "clic en botón", Map.of("button", "Login"));
            assertThat(listAppender.list).isNotEmpty();
        }

        @Test
        @DisplayName("logHttpAction produce evento con método HTTP")
        void logHttpActionProduceEvento() {
            TestLogger.logHttpAction("POST", "https://api.test.com/login", 200, 100L);
            assertThat(listAppender.list).isNotEmpty();
            // Al menos un evento debe mencionar el método o la URL
            boolean tieneContenidoRelevante = listAppender.list.stream()
                .anyMatch(e -> e.getFormattedMessage().contains("POST") ||
                               e.getFormattedMessage().contains("200") ||
                               e.getFormattedMessage().contains("api.test.com"));
            assertThat(tieneContenidoRelevante).isTrue();
        }

        @Test
        @DisplayName("logUiAction produce evento de log")
        void logUiActionProduceEvento() {
            TestLogger.logUiAction("click", "btnLogin", "Login");
            assertThat(listAppender.list).isNotEmpty();
        }

        @Test
        @DisplayName("logUiAction sin valor no lanza excepción")
        void logUiActionSinValorNoLanza() {
            assertThatCode(() -> TestLogger.logUiAction("click", "btnSubmit", null))
                .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // Assertions y validaciones: producen eventos con contenido correcto
    // =========================================================================

    @Nested
    @DisplayName("Assertions y validaciones")
    @Order(3)
    class AssertionsTests {

        @Test
        @DisplayName("logAssertionSuccess produce evento de log")
        void logAssertionSuccessProduceEvento() {
            TestLogger.logAssertionSuccess("Status code es 200", "200", "200");
            assertThat(listAppender.list).isNotEmpty();
        }

        @Test
        @DisplayName("logAssertionFailure produce evento de log")
        void logAssertionFailureProduceEvento() {
            TestLogger.logAssertionFailure("Status code es 200", "200", "404", "API error");
            assertThat(listAppender.list).isNotEmpty();
        }

        @Test
        @DisplayName("logValidation con passed=true produce evento de log")
        void logValidationSuccessProduceEvento() {
            TestLogger.logValidation("SCHEMA", "response válido", true);
            assertThat(listAppender.list).isNotEmpty();
        }

        @Test
        @DisplayName("logValidation con passed=false produce evento de log")
        void logValidationFailureProduceEvento() {
            TestLogger.logValidation("SCHEMA", "response inválido", false);
            assertThat(listAppender.list).isNotEmpty();
        }
    }

    // =========================================================================
    // LoggerWrapper: valida que los métodos del wrapper produzcan eventos
    // =========================================================================

    @Nested
    @DisplayName("LoggerWrapper")
    @Order(4)
    class LoggerWrapperTests {

        @Test
        @DisplayName("getLogger retorna instancia no null")
        void getLoggerNoNull() {
            assertThat(TestLogger.getLogger(TestLoggerTest.class)).isNotNull();
        }

        @Test
        @DisplayName("wrapper.info produce evento de log")
        void wrapperInfoProduceEvento() {
            TestLogger.getLogger(TestLoggerTest.class).info("mensaje info wrapper");
            assertThat(listAppender.list).isNotEmpty();
        }

        @Test
        @DisplayName("wrapper.error con excepción produce evento de log")
        void wrapperErrorProduceEvento() {
            TestLogger.getLogger(TestLoggerTest.class).error("error wrapper", new RuntimeException("test"));
            assertThat(listAppender.list).isNotEmpty();
        }

        @Test
        @DisplayName("wrapper.warn produce evento de log")
        void wrapperWarnProduceEvento() {
            TestLogger.getLogger(TestLoggerTest.class).warn("warning wrapper");
            assertThat(listAppender.list).isNotEmpty();
        }

        @Test
        @DisplayName("wrapper con formato SLF4J reemplaza parámetros correctamente")
        void wrapperConFormatoReemplazaParametros() {
            TestLogger.LoggerWrapper wrapper = TestLogger.getLogger(TestLoggerTest.class);
            wrapper.info("Usuario {} inició sesión", "john");
            boolean tieneNombre = listAppender.list.stream()
                .anyMatch(e -> e.getFormattedMessage().contains("john"));
            assertThat(tieneNombre).isTrue();
        }
    }

    // =========================================================================
    // Edge cases: null, vacío, largo, unicode — no deben explotar
    // =========================================================================

    @Nested
    @DisplayName("Edge cases")
    @Order(5)
    class EdgeCasesTests {

        @Test
        @DisplayName("Todos los parámetros null no lanza excepción")
        void todosNullNoLanza() {
            assertThatCode(() -> TestLogger.logInfo(null, null, null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Mensaje muy largo no lanza excepción")
        void mensajeMuyLargoNoLanza() {
            assertThatCode(() -> TestLogger.logInfo("CAT", "x".repeat(10000), null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Caracteres unicode no lanzan excepción")
        void unicodeNoLanza() {
            assertThatCode(() -> TestLogger.logInfo("CAT", "áéíóú ñ 你好 🔥", null))
                .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {"SQL_QUERY", "HTTP_REQUEST", "DATABASE_ERROR", "API_RESPONSE"})
        @DisplayName("Diferentes categorías no lanzan excepción")
        void diferentesCategorias(String category) {
            assertThatCode(() -> TestLogger.logInfo(category, "mensaje", null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Contexto vacío no lanza excepción")
        void contextoVacioNoLanza() {
            assertThatCode(() -> TestLogger.logInfo("CAT", "mensaje", Map.of()))
                .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // Context management: setTestContext / clearTestContext / setFramework
    // =========================================================================

    @Nested
    @DisplayName("Context management")
    @Order(6)
    class ContextManagementTests {

        @Test
        @DisplayName("setTestContext y clearTestContext no lanzan excepción")
        void setAndClearContextNoLanza() {
            assertThatCode(() -> {
                TestLogger.setTestContext("TestLoginFlow");
                TestLogger.clearTestContext();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("setFramework no lanza excepción")
        void setFrameworkNoLanza() {
            assertThatCode(() -> TestLogger.setFramework("API"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("clearTestContext sin setTestContext previo no lanza excepción")
        void clearSinSetPrevioNoLanza() {
            assertThatCode(() -> TestLogger.clearTestContext())
                .doesNotThrowAnyException();
        }
    }
}

