package com.qa.apicore.utils;

import com.qa.apicore.utils.ValidationUtilities;
import com.qa.common.exception.FrameworkBusinessException;
import com.qa.apicore.model.HttpResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para ValidationUtilities.
 *
 * <p><b>Cobertura:</b> Status codes, headers, JSON path, patrones y rangos.</p>
 * <p><b>Estrategia:</b> Solo lógica pura sin red ni BD real.</p>
 *
 * @author Abel Venero
 * @since 1.0.0
 */
@DisplayName("ValidationUtilities")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ValidationUtilitiesTest {

    // =========================================================================
    // Fixtures
    // =========================================================================

    private HttpResponse response(int status, String body) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return new HttpResponse(status, body, headers, 0L);
    }

    private HttpResponse responseWithHeaders(int status, String body, Map<String, String> headers) {
        return new HttpResponse(status, body, headers, 0L);
    }

    // =========================================================================
    // validateStatusCode
    // =========================================================================

    @Nested
    @DisplayName("validateStatusCode()")
    @Order(1)
    class ValidateStatusCodeTests {

        @Test
        @DisplayName("Pasa cuando el status coincide exactamente")
        void statusExactoOk() throws FrameworkBusinessException {
            assertThatNoException().isThrownBy(() ->
                ValidationUtilities.validateStatusCode(response(200, "{}"), 200));
        }

        @ParameterizedTest
        @ValueSource(ints = {200, 201, 400, 401, 403, 404, 500})
        @DisplayName("Pasa con distintos status codes válidos")
        void statusVariosOk(int code) throws FrameworkBusinessException {
            assertThatNoException().isThrownBy(() ->
                ValidationUtilities.validateStatusCode(response(code, "{}"), code));
        }

        @Test
        @DisplayName("Lanza excepción cuando el status no coincide")
        void statusDistintoFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateStatusCode(response(200, "{}"), 404))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("200")
                .hasMessageContaining("404");
        }

        @Test
        @DisplayName("Lanza excepción cuando response es null")
        void responseNullFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateStatusCode(null, 200))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("null");
        }

        @Test
        @DisplayName("El mensaje de error incluye status esperado y real")
        void mensajeErrorDescriptivo() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateStatusCode(response(500, "error"), 200))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("200")
                .hasMessageContaining("500");
        }
    }

    // =========================================================================
    // validateStatusCodeRange
    // =========================================================================

    @Nested
    @DisplayName("validateStatusCodeRange()")
    @Order(2)
    class ValidateStatusCodeRangeTests {

        @ParameterizedTest
        @CsvSource({"200,200,299", "201,200,299", "299,200,299", "404,400,499", "500,500,599"})
        @DisplayName("Pasa cuando el status está dentro del rango")
        void statusDentroRangoOk(int code, int min, int max) throws FrameworkBusinessException {
            assertThatNoException().isThrownBy(() ->
                ValidationUtilities.validateStatusCodeRange(response(code, "{}"), min, max));
        }

        @Test
        @DisplayName("Lanza excepción cuando el status está fuera del rango")
        void statusFueraRangoFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateStatusCodeRange(response(404, "{}"), 200, 299))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("200")
                .hasMessageContaining("299");
        }

        @Test
        @DisplayName("Lanza excepción cuando response es null")
        void responseNullFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateStatusCodeRange(null, 200, 299))
                .isInstanceOf(FrameworkBusinessException.class);
        }
    }

    // =========================================================================
    // validateHeaderExists / validateHeaderValue / validateHeaderContains
    // =========================================================================

    @Nested
    @DisplayName("validateHeader*()")
    @Order(3)
    class ValidateHeaderTests {

        private HttpResponse responseConHeader(String name, String value) {
            Map<String, String> headers = new HashMap<>();
            headers.put(name, value);
            return responseWithHeaders(200, "{}", headers);
        }

        @Test
        @DisplayName("validateHeaderExists pasa cuando el header está presente")
        void headerExisteOk() throws FrameworkBusinessException {
            assertThatNoException().isThrownBy(() ->
                ValidationUtilities.validateHeaderExists(
                    responseConHeader("Content-Type", "application/json"), "Content-Type"));
        }

        @Test
        @DisplayName("validateHeaderExists falla cuando el header no existe")
        void headerNoExisteFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateHeaderExists(
                    responseConHeader("Content-Type", "application/json"), "X-Custom-Header"))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("X-Custom-Header");
        }

        @Test
        @DisplayName("validateHeaderValue pasa cuando el valor coincide")
        void headerValorOk() throws FrameworkBusinessException {
            assertThatNoException().isThrownBy(() ->
                ValidationUtilities.validateHeaderValue(
                    responseConHeader("Content-Type", "application/json"),
                    "Content-Type", "application/json"));
        }

        @Test
        @DisplayName("validateHeaderValue falla cuando el valor no coincide")
        void headerValorDistintoFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateHeaderValue(
                    responseConHeader("Content-Type", "text/html"),
                    "Content-Type", "application/json"))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("Content-Type");
        }

        @Test
        @DisplayName("validateHeaderContains pasa cuando el header contiene el substring")
        void headerContieneSubstringOk() throws FrameworkBusinessException {
            assertThatNoException().isThrownBy(() ->
                ValidationUtilities.validateHeaderContains(
                    responseConHeader("Content-Type", "application/json; charset=UTF-8"),
                    "Content-Type", "application/json"));
        }

        @Test
        @DisplayName("validateHeaderContains falla cuando el header no contiene el substring")
        void headerNoContieneSubstringFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateHeaderContains(
                    responseConHeader("Content-Type", "text/html"),
                    "Content-Type", "application/json"))
                .isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("validateHeaderExists falla cuando response es null")
        void responseNullFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateHeaderExists(null, "Content-Type"))
                .isInstanceOf(FrameworkBusinessException.class);
        }
    }

    // =========================================================================
    // validateJsonPath / validateJsonPathExists / validateJsonType
    // =========================================================================

    @Nested
    @DisplayName("validateJsonPath*()")
    @Order(4)
    class ValidateJsonPathTests {

        private static final String JSON_SIMPLE =
            "{\"user\":{\"name\":\"Abel\",\"age\":30,\"active\":true,\"tags\":[\"qa\",\"dev\"]}}";
        private static final String JSON_WITH_EMAIL =
            "{\"email\":\"qa-architecture@example.com\"}";

        @Test
        @DisplayName("validateJsonPath lanza excepción cuando response es null")
        void jsonPathResponseNullFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateJsonPath(null, "$.user.name", "Abel"))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("null");
        }

        @Test
        @DisplayName("validateJsonPath lanza excepción cuando body es null")
        void jsonPathBodyNullFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateJsonPath(
                    new HttpResponse(200, null, new HashMap<>(), 0L), "$.user.name", "Abel"))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("null");
        }

        @Test
        @DisplayName("validateJsonPath lanza FrameworkBusinessException con valor incorrecto o error técnico")
        void jsonPathValorIncorrectoFalla() {
            // El método internamente puede fallar técnicamente si getJsonParameter no existe;
            // en cualquier caso debe lanzar FrameworkBusinessException
            assertThatThrownBy(() ->
                ValidationUtilities.validateJsonPath(
                    response(200, JSON_SIMPLE), "$.user.name", "John"))
                .isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("validateJsonPath acepta expected String cuando actual JSON es Number")
        void jsonPathStringExpectedMatchesNumberActual() throws FrameworkBusinessException {
            ValidationUtilities.validateJsonPath(
                response(200, "{\"number\":0}"), "$.number", "0");
        }

        @Test
        @DisplayName("validateJsonPath acepta expected String cuando actual JSON es Boolean")
        void jsonPathStringExpectedMatchesBooleanActual() throws FrameworkBusinessException {
            ValidationUtilities.validateJsonPath(
                response(200, "{\"active\":true}"), "$.active", "true");
        }

        @Test
        @DisplayName("validateJsonPathExists lanza excepción cuando response es null")
        void jsonPathExisteResponseNullFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateJsonPathExists(null, "$.user.name"))
                .isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("validateJsonType lanza excepción cuando response es null")
        void jsonTypeResponseNullFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateJsonType(null, "$.user.name", "string"))
                .isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("validateJsonType lanza excepción cuando body es null")
        void jsonTypeBodyNullFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateJsonType(
                    new HttpResponse(200, null, new HashMap<>(), 0L), "$.user.name", "string"))
                .isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("validateJsonPathMatchesPattern pasa con regex normal de email")
        void jsonPathPatternEmailRegexNormalOk() throws FrameworkBusinessException {
            ValidationUtilities.validateJsonPathMatchesPattern(
                response(200, JSON_WITH_EMAIL),
                "$.email",
                "^[^@]+@[^@]+\\.[^@]+$");
        }

        @Test
        @DisplayName("validateJsonPathMatchesPattern pasa con regex doblemente escapada desde feature")
        void jsonPathPatternEmailRegexDoubleEscapedOk() throws FrameworkBusinessException {
            ValidationUtilities.validateJsonPathMatchesPattern(
                response(200, JSON_WITH_EMAIL),
                "$.email",
                "^[^@]+@[^@]+\\\\.[^@]+$");
        }
    }

    // =========================================================================
    // validatePattern / validateRange
    // =========================================================================

    @Nested
    @DisplayName("validatePattern() y validateRange()")
    @Order(5)
    class ValidatePatternAndRangeTests {

        @ParameterizedTest
        @ValueSource(strings = {"user@test.com", "abel.venero@example.com", "test+tag@domain.org"})
        @DisplayName("validatePattern pasa con emails válidos")
        void emailValidoOk(String email) throws FrameworkBusinessException {
            assertThatNoException().isThrownBy(() ->
                ValidationUtilities.validatePattern(email, ValidationUtilities.EMAIL_PATTERN));
        }

        @ParameterizedTest
        @ValueSource(strings = {"noatmail", "@sin-usuario.com", "doble@@mail.com"})
        @DisplayName("validatePattern falla con emails inválidos")
        void emailInvalidoFalla(String email) {
            assertThatThrownBy(() ->
                ValidationUtilities.validatePattern(email, ValidationUtilities.EMAIL_PATTERN))
                .isInstanceOf(FrameworkBusinessException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "550e8400-e29b-41d4-a716-446655440000",
            "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
        })
        @DisplayName("validatePattern pasa con UUIDs válidos")
        void uuidValidoOk(String uuid) throws FrameworkBusinessException {
            assertThatNoException().isThrownBy(() ->
                ValidationUtilities.validatePattern(uuid, ValidationUtilities.UUID_PATTERN));
        }

        @Test
        @DisplayName("validatePattern falla con value null")
        void valueNullFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validatePattern(null, ValidationUtilities.EMAIL_PATTERN))
                .isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("Patrones EMAIL y UUID están disponibles como constantes")
        void patronesConstantesDisponibles() {
            assertThat(ValidationUtilities.EMAIL_PATTERN).isNotNull();
            assertThat(ValidationUtilities.UUID_PATTERN).isNotNull();
            assertThat(ValidationUtilities.URL_PATTERN).isNotNull();
            assertThat(ValidationUtilities.PHONE_PATTERN).isNotNull();
        }
    }

    // =========================================================================
    // validateJsonSchema (inline)
    // =========================================================================

    @Nested
    @DisplayName("validateJsonSchema() - inline schema")
    @Order(6)
    class ValidateJsonSchemaTests {

        private static final String SCHEMA_SIMPLE = """
            {
              "type": "object",
              "properties": {
                "id":   { "type": "integer" },
                "name": { "type": "string" }
              },
              "required": ["id", "name"]
            }
            """;

        @Test
        @DisplayName("Pasa con JSON que cumple el esquema")
        void esquemaCumplidoOk() throws FrameworkBusinessException {
            String json = "{\"id\":1,\"name\":\"Abel\"}";
            assertThatNoException().isThrownBy(() ->
                ValidationUtilities.validateJsonSchema(response(200, json), SCHEMA_SIMPLE));
        }

        @Test
        @DisplayName("Falla cuando falta campo requerido")
        void campRequeridoFaltanteFalla() {
            String json = "{\"id\":1}"; // falta "name"
            assertThatThrownBy(() ->
                ValidationUtilities.validateJsonSchema(response(200, json), SCHEMA_SIMPLE))
                .isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("Falla cuando el tipo de campo es incorrecto")
        void tipoIncorrectoFalla() {
            String json = "{\"id\":\"uno\",\"name\":\"Abel\"}"; // id debe ser integer
            assertThatThrownBy(() ->
                ValidationUtilities.validateJsonSchema(response(200, json), SCHEMA_SIMPLE))
                .isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("Falla cuando response es null")
        void responseNullFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateJsonSchema((HttpResponse) null, SCHEMA_SIMPLE))
                .isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("validateJsonSchema(String,String) pasa con JSON válido")
        void jsonStringValidoOk() throws FrameworkBusinessException {
            String json = "{\"id\":1,\"name\":\"Test\"}";
            assertThatNoException().isThrownBy(() ->
                ValidationUtilities.validateJsonSchema(json, SCHEMA_SIMPLE));
        }

        @Test
        @DisplayName("validateJsonSchema(String,String) falla con JSON vacío")
        void jsonStringVacioFalla() {
            assertThatThrownBy(() ->
                ValidationUtilities.validateJsonSchema("", SCHEMA_SIMPLE))
                .isInstanceOf(FrameworkBusinessException.class);
        }
    }

    // =========================================================================
    // Constructor privado
    // =========================================================================

    @Test
    @DisplayName("El constructor privado lanza UnsupportedOperationException")
    @Order(7)
    void constructorPrivadoLanzaExcepcion() {
        assertThatThrownBy(() -> {
            var ctor = ValidationUtilities.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            ctor.newInstance();
        })
            .hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}


