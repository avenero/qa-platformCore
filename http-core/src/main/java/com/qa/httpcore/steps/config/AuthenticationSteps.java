package com.qa.httpcore.steps.config;

import com.qa.httpcore.implementations.BaseAuthenticationManager;
import com.qa.httpcore.implementations.ApacheHttpClientImpl;
import com.qa.httpcore.interfaces.AuthenticationService;
import com.qa.httpcore.interfaces.HttpClient;
import com.qa.httpcore.utils.ApiHelper;
import com.qa.common.api.exception.FrameworkBusinessException;
import com.qa.common.api.logging.TestLogger;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.api.runtime.annotation.StepDef;
import io.cucumber.java.en.Given;
import java.util.Map;

/**
 * Steps de autenticación HTTP: Bearer, Client Credentials, Basic, OAuth2, JWT, API Key.
 *
 * <p>Componente padre: {@code api.authentication}
 * ({@link com.qa.httpcore.components.ApiAuthComponent}).
 * Fase BDD: GIVEN.
 *
 * <p>Todos los steps canónicos llevan {@link StepDef} con ID explícito para garantizar
 * estabilidad frente a refactorizaciones. El formato es {@code api.authentication.{sub-id}}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class AuthenticationSteps {

    // ─── Obtención de servicios desde el ServiceRegistry ───────────

    private HttpClient getHttpClient() {
        return ExecutionContext.current().map(ctx -> ctx.service(HttpClient.class))
                .orElseGet(ApacheHttpClientImpl::new);
    }

    private AuthenticationService getAuthentication() {
        return ExecutionContext.current().map(ctx -> ctx.service(AuthenticationService.class)).
                orElseGet(() -> new BaseAuthenticationManager(getHttpClient()));
    }

    private ApiHelper getApiHelper() {
        return ApiHelper.forCurrentContext();
    }

    // =========================================================================
    // Client Credentials
    // =========================================================================

    /**
     * Agrega autenticación OAuth2 Client Credentials usando las credenciales configuradas
     * en el contexto de ejecución (sin parámetros en el step).
     */
    @StepDef(value = "api.authentication.client-credentials",
             displayName = "Autenticación Client Credentials")
    @Given("agrego autenticacion Client Credentials")
    public void agregoAutenticacionClientCredentials() throws FrameworkBusinessException {
        String token = getAuthentication().getClientCredentialsToken();
        getHttpClient().addHeader("Authorization", "Bearer " + token);
        TestLogger.logInfo("AUTH_STEPS", "Autenticacion Client Credentials configurada", null);
    }

    // Kept original Spanish with accent for backward compat — mismo ID
    @StepDef(value = "api.authentication.client-credentials",
             displayName = "Autenticación Client Credentials (con tilde)")
    @Given("agrego autenticación Client Credentials")
    public void agregoAutenticacionClientCredentialsAccent() throws FrameworkBusinessException {
        agregoAutenticacionClientCredentials();
    }

    // =========================================================================
    // Bearer Token
    // =========================================================================

    /**
     * Agrega autenticación Bearer usando un identificador genérico (RUT, DNI, email, etc.).
     * Step canónico — reemplaza al step legacy con "RUT" en el nombre.
     */
    @StepDef(value = "api.authentication.bearer.identifier",
             displayName = "Autenticación Bearer por identificador")
    @Given("agrego autenticación Bearer con identificador {string}")
    public void agregoAutenticacionBearerConIdentificador(String identifier)
            throws FrameworkBusinessException {
        String processed = ExecutionContext.requireCurrent().variables().resolve(identifier);
        String token = getAuthentication().getBearerTokenForIdentifier(processed);
        getHttpClient().addHeader("Authorization", "Bearer " + token);
        TestLogger.logInfo("AUTH_STEPS", "Autenticacion Bearer para identificador: " + processed, null);
    }

    /**
     * Agrega un token Bearer personalizado directamente (sin obtenerlo del servicio de auth).
     */
    @StepDef(value = "api.authentication.custom-token",
             displayName = "Agregar token personalizado")
    @Given("agrego el token personalizado {string}")
    public void agregoElTokenPersonalizado(String token) {
        getApiHelper().addBearerToken(token);
    }

    // =========================================================================
    // Basic Auth
    // =========================================================================

    /**
     * Agrega autenticación HTTP Basic con usuario y contraseña.
     */
    @StepDef(value = "api.authentication.basic",
             displayName = "Autenticación Basic Auth")
    @Given("agrego autenticación básica con usuario {string} y password {string}")
    public void agregoAutenticacionBasicaConUsuarioYPassword(String username, String password) {
        getApiHelper().addBasicAuthentication(username, password);
    }

    // =========================================================================
    // OAuth 2.0
    // =========================================================================

    /**
     * Configura autenticación OAuth2 Client Credentials con credenciales explícitas.
     */
    @StepDef(value = "api.authentication.oauth2",
             displayName = "Autenticación OAuth2 Client Credentials")
    @Given("configuro autenticación OAuth2 con client_id {string} y client_secret {string}")
    public void configuroOAuth2(String clientId, String clientSecret)
            throws FrameworkBusinessException {
        String token = getAuthentication().getClientCredentialsToken(clientId, clientSecret);
        getHttpClient().addHeader("Authorization", "Bearer " + token);
    }

    // =========================================================================
    // API Key
    // =========================================================================

    /**
     * Agrega una API Key como header HTTP.
     */
    @StepDef(value = "api.authentication.api-key.header",
             displayName = "API Key en header")
    @Given("agrego API Key {string} en header {string}")
    public void agregoApiKeyEnHeader(String apiKey, String headerName) {
        getHttpClient().addHeader(headerName,
                ExecutionContext.requireCurrent().variables().resolve(apiKey));
    }

    /**
     * Agrega una API Key como query parameter.
     */
    @StepDef(value = "api.authentication.api-key.query",
             displayName = "API Key como query param")
    @Given("agrego API Key {string} como query param {string}")
    public void agregoApiKeyComoQueryParam(String apiKey, String paramName) {
        getHttpClient().addQueryParam(paramName,
                ExecutionContext.requireCurrent().variables().resolve(apiKey));
    }

    // =========================================================================
    // JWT
    // =========================================================================

    /**
     * Configura autenticación JWT con claims personalizados definidos en una DataTable.
     */
    @StepDef(value = "api.authentication.jwt",
             displayName = "Autenticación JWT con claims")
    @Given("configuro JWT con las siguientes claims")
    public void configuroJwtConClaims(Map<String, String> claims) throws FrameworkBusinessException {
        String token = getAuthentication().getCustomToken("jwt", claims);
        getHttpClient().addHeader("Authorization", "Bearer " + token);
    }

    // =========================================================================
    // Sin autenticación
    // =========================================================================

    /**
     * Elimina cualquier header de autenticación previamente configurado.
     * Útil para probar endpoints que no requieren autenticación.
     */
    @StepDef(value = "api.authentication.none",
             displayName = "Sin autenticación")
    @Given("no agrego autenticación")
    public void noAgregoAutenticacion() {
        getHttpClient().removeHeader("Authorization");
    }

    /**
     * Simula un token JWT expirado en el header {@code Authorization}.
     * Útil para verificar que el API rechaza tokens caducados con 401.
     */
    @StepDef(value = "api.authentication.simular-token-expirado",
             displayName = "Simular token expirado en Authorization")
    @Given("simulo un token expirado en el header Authorization")
    public void simuloTokenExpirado() {
        getHttpClient().addHeader("Authorization", "Bearer EXPIRED_TOKEN_FOR_SECURITY_TEST");
    }

    /**
     * Simula un token JWT con formato inválido en el header {@code Authorization}.
     * Útil para verificar que el API rechaza tokens malformados con 401.
     */
    @StepDef(value = "api.authentication.simular-token-invalido",
             displayName = "Simular token inválido en Authorization")
    @Given("simulo un token inválido en el header Authorization")
    public void simuloTokenInvalido() {
        getHttpClient().addHeader("Authorization", "Bearer INVALID_TOKEN_xyz");
    }
}
