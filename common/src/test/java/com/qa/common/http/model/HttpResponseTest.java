package com.qa.common.http.model;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;
import java.util.HashMap;

/**
 * Tests unitarios para HttpResponse.
 *
 * <p><b>Cobertura objetivo:</b> 95%</p>
 * <p><b>Total tests:</b> 15</p>
 *
 * @author Abel Venero
 * @since 1.0.1
 */
@DisplayName("HttpResponse Tests")
class HttpResponseTest {

    // =========================================================================
    // CONSTRUCTOR TESTS
    // =========================================================================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Debe crear respuesta básica con status y body")
        void testBasicConstructor() {
            // Given
            int status = 200;
            String body = "{\"message\":\"OK\"}";

            // When
            HttpResponse response = new HttpResponse(status, body);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(body);
            assertThat(response.getHeaders()).isEmpty();
            assertThat(response.getDuration()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Debe crear respuesta completa con headers y duration")
        void testCompleteConstructor() {
            // Given
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("X-Request-Id", "12345");

            // When
            HttpResponse response = new HttpResponse(200, "body", headers, 150L);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo("body");
            assertThat(response.getHeaders())
                .hasSize(2)
                .containsEntry("Content-Type", "application/json")
                .containsEntry("X-Request-Id", "12345");
            assertThat(response.getDuration()).isEqualTo(150L);
        }

        @Test
        @DisplayName("Debe manejar headers null sin lanzar excepción")
        void testNullHeaders() {
            // When
            HttpResponse response = new HttpResponse(200, "body", null, 100L);

            // Then
            assertThat(response.getHeaders()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Debe manejar body null")
        void testNullBody() {
            // When
            HttpResponse response = new HttpResponse(404, null);

            // Then
            assertThat(response.getBody()).isNull();
            assertThat(response.hasContent()).isFalse();
        }
    }

    // =========================================================================
    // IMMUTABILITY TESTS
    // =========================================================================

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Debe retornar copia de headers, no referencia")
        void testHeadersImmutabilityCopy() {
            // Given
            Map<String, String> originalHeaders = new HashMap<>();
            originalHeaders.put("Content-Type", "application/json");
            HttpResponse response = new HttpResponse(200, "", originalHeaders, 0L);

            // When - modificar el mapa original
            originalHeaders.put("X-Modified", "should-not-appear");

            // Then - el response no debe verse afectado
            assertThat(response.getHeaders())
                .hasSize(1)
                .doesNotContainKey("X-Modified");
        }

        @Test
        @DisplayName("Modificar headers retornados no debe afectar response")
        void testHeadersImmutabilityReturned() {
            // Given
            HttpResponse response = new HttpResponse(200, "",
                Map.of("Original", "value"), 0L);

            // When - modificar el Map retornado
            Map<String, String> returnedHeaders = response.getHeaders();
            returnedHeaders.put("New", "value");

            // Then - el response no debe verse afectado
            assertThat(response.getHeaders())
                .hasSize(1)
                .doesNotContainKey("New");
        }

        @Test
        @DisplayName("setHeaders debe reemplazar headers completamente")
        void testSetHeaders() {
            // Given
            HttpResponse response = new HttpResponse(200, "");
            Map<String, String> newHeaders = new HashMap<>();
            newHeaders.put("New-Header", "value");

            // When
            response.setHeaders(newHeaders);

            // Then
            assertThat(response.getHeaders())
                .hasSize(1)
                .containsEntry("New-Header", "value");
        }
    }

    // =========================================================================
    // STATUS CODE VALIDATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Status Code Validation Tests")
    class StatusCodeTests {

        @ParameterizedTest
        @ValueSource(ints = {200, 201, 202, 204, 299})
        @DisplayName("Debe identificar códigos 2xx como exitosos")
        void testSuccessfulStatusCodes(int statusCode) {
            // When
            HttpResponse response = new HttpResponse(statusCode, "");

            // Then
            assertThat(response.isSuccessful()).isTrue();
            assertThat(response.isClientError()).isFalse();
            assertThat(response.isServerError()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(ints = {400, 401, 403, 404, 422, 499})
        @DisplayName("Debe identificar códigos 4xx como errores de cliente")
        void testClientErrorStatusCodes(int statusCode) {
            // When
            HttpResponse response = new HttpResponse(statusCode, "");

            // Then
            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.isClientError()).isTrue();
            assertThat(response.isServerError()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(ints = {500, 501, 502, 503, 504, 599})
        @DisplayName("Debe identificar códigos 5xx como errores de servidor")
        void testServerErrorStatusCodes(int statusCode) {
            // When
            HttpResponse response = new HttpResponse(statusCode, "");

            // Then
            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.isClientError()).isFalse();
            assertThat(response.isServerError()).isTrue();
        }

        @ParameterizedTest
        @CsvSource({
            "100, false, false, false",  // Informational
            "199, false, false, false",
            "300, false, false, false",  // Redirection
            "399, false, false, false"
        })
        @DisplayName("Debe manejar códigos fuera de rangos estándar")
        void testEdgeCaseStatusCodes(int status, boolean success, boolean client, boolean server) {
            // When
            HttpResponse response = new HttpResponse(status, "");

            // Then
            assertThat(response.isSuccessful()).isEqualTo(success);
            assertThat(response.isClientError()).isEqualTo(client);
            assertThat(response.isServerError()).isEqualTo(server);
        }
    }

    // =========================================================================
    // CONTENT VALIDATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Content Validation Tests")
    class ContentTests {

        @Test
        @DisplayName("Debe detectar contenido cuando body no es null ni vacío")
        void testHasContentWithValidBody() {
            // When
            HttpResponse response = new HttpResponse(200, "content");

            // Then
            assertThat(response.hasContent()).isTrue();
        }

        @Test
        @DisplayName("Debe detectar sin contenido cuando body es null")
        void testHasContentWithNullBody() {
            // When
            HttpResponse response = new HttpResponse(204, null);

            // Then
            assertThat(response.hasContent()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  ", "\t", "\n", "   \t\n  "})
        @DisplayName("Debe detectar sin contenido cuando body es vacío o whitespace")
        void testHasContentWithEmptyBody(String emptyBody) {
            // When
            HttpResponse response = new HttpResponse(200, emptyBody);

            // Then
            assertThat(response.hasContent()).isFalse();
        }
    }

    // =========================================================================
    // GETTER TESTS
    // =========================================================================

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("getResponseBody debe ser alias de getBody")
        void testGetResponseBodyAlias() {
            // Given
            String body = "test-body";
            HttpResponse response = new HttpResponse(200, body);

            // Then
            assertThat(response.getResponseBody()).isEqualTo(response.getBody());
            assertThat(response.getResponseBody()).isEqualTo(body);
        }

        @Test
        @DisplayName("getHeader debe retornar header específico")
        void testGetSpecificHeader() {
            // Given
            Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "X-Custom", "value"
            );
            HttpResponse response = new HttpResponse(200, "", headers, 0L);

            // When/Then
            assertThat(response.getHeader("Content-Type")).isEqualTo("application/json");
            assertThat(response.getHeader("X-Custom")).isEqualTo("value");
            assertThat(response.getHeader("Non-Existent")).isNull();
        }

        @Test
        @DisplayName("getDuration debe retornar tiempo de ejecución")
        void testGetDuration() {
            // When
            HttpResponse response = new HttpResponse(200, "", null, 250L);

            // Then
            assertThat(response.getDuration()).isEqualTo(250L);
        }
    }

    // =========================================================================
    // UTILITY METHODS TESTS
    // =========================================================================

    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityTests {

        @Test
        @DisplayName("toString debe retornar información resumida")
        void testToString() {
            // Given
            HttpResponse response = new HttpResponse(200, "test-body",
                Map.of("key", "value"), 100L);

            // When
            String result = response.toString();

            // Then
            assertThat(result)
                .contains("200")           // status code
                .contains("9")             // body length
                .contains("1")             // headers count
                .contains("100");          // duration
        }

        @Test
        @DisplayName("getSummary debe retornar resumen para logging")
        void testGetSummary() {
            // Given
            HttpResponse response = new HttpResponse(404, "Not Found", null, 50L);

            // When
            String summary = response.getSummary();

            // Then
            assertThat(summary)
                .contains("Status: 404")
                .contains("9 bytes")
                .contains("50ms");
        }

        @Test
        @DisplayName("getSummary debe manejar body null")
        void testGetSummaryWithNullBody() {
            // When
            HttpResponse response = new HttpResponse(204, null, null, 10L);
            String summary = response.getSummary();

            // Then
            assertThat(summary).contains("0 bytes");
        }
    }
}

