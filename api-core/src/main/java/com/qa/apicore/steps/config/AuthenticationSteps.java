package com.qa.apicore.steps.config;

import com.qa.apicore.implementations.BaseAuthenticationManager;
import com.qa.apicore.implementations.BaseHttpClient;
import com.qa.apicore.interfaces.AuthenticationService;
import com.qa.apicore.interfaces.HttpClient;
import com.qa.apicore.utils.ApiHelper;
import com.qa.common.http.exceptions.FrameworkBusinessException;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import io.cucumber.java.en.Given;
import java.util.Map;

/**
 * Steps de autenticacion HTTP: Bearer, Client Credentials, Basic, OAuth2, JWT, API Key.
 * Migrado de ApiSteps.java + nuevos steps de seguridad.
 * @author Abel Venero
 * @since 2.0.0
 */
public class AuthenticationSteps {

    // ─── Obtención de servicios desde el ServiceRegistry (Fase 3 — Task 105) ───

    private HttpClient getHttpClient() {
        return ExecutionContext.current()
                .map(ctx -> ctx.service(HttpClient.class))
                .orElseGet(BaseHttpClient::new);
    }

    private AuthenticationService getAuthentication() {
        return ExecutionContext.current()
                .map(ctx -> ctx.service(AuthenticationService.class))
                .orElseGet(() -> new BaseAuthenticationManager(getHttpClient()));
    }

    private ApiHelper getApiHelper() {
        return ApiHelper.forCurrentContext();
    }

    @Given("agrego autenticacion Client Credentials")
    public void agregoAutenticacionClientCredentials() throws FrameworkBusinessException {
        String token = getAuthentication().getClientCredentialsToken();
        getHttpClient().addHeader("Authorization", "Bearer " + token);
        TestLogger.logInfo("AUTH_STEPS", "Autenticacion Client Credentials configurada", null);
    }

    // Kept original Spanish with accent for backward compat
    @Given("agrego autenticación Client Credentials")
    public void agregoAutenticacionClientCredentialsAccent() throws FrameworkBusinessException {
        agregoAutenticacionClientCredentials();
    }

    @Given("agrego autenticación Bearer para RUT {string}")
    public void agregoAutenticacionBearerParaRUT(String rut) throws FrameworkBusinessException {
        String processedRut = ExecutionContext.requireCurrent().variables().resolve(rut);
        String token = getAuthentication().getBearerTokenForIdentifier(processedRut);
        getHttpClient().addHeader("Authorization", "Bearer " + token);
        TestLogger.logInfo("AUTH_STEPS", "Autenticacion Bearer para RUT: " + processedRut, null);
    }

    @Given("agrego el token personalizado {string}")
    public void agregoElTokenPersonalizado(String token) {
        getApiHelper().addBearerToken(token);
    }

    @Given("agrego autenticación básica con usuario {string} y password {string}")
    public void agregoAutenticacionBasicaConUsuarioYPassword(String username, String password) {
        getApiHelper().addBasicAuthentication(username, password);
    }

    @Given("configuro autenticación OAuth2 con client_id {string} y client_secret {string}")
    public void configuroOAuth2(String clientId, String clientSecret) throws FrameworkBusinessException {
        String token = getAuthentication().getClientCredentialsToken(clientId, clientSecret);
        getHttpClient().addHeader("Authorization", "Bearer " + token);
    }

    @Given("agrego API Key {string} en header {string}")
    public void agregoApiKeyEnHeader(String apiKey, String headerName) {
        getHttpClient().addHeader(headerName, ExecutionContext.requireCurrent().variables().resolve(apiKey));
    }

    @Given("agrego API Key {string} como query param {string}")
    public void agregoApiKeyComoQueryParam(String apiKey, String paramName) {
        getHttpClient().addQueryParam(paramName, ExecutionContext.requireCurrent().variables().resolve(apiKey));
    }

    @Given("configuro JWT con las siguientes claims")
    public void configuroJwtConClaims(Map<String, String> claims) throws FrameworkBusinessException {
        String token = getAuthentication().getCustomToken("jwt", claims);
        getHttpClient().addHeader("Authorization", "Bearer " + token);
    }

    @Given("agrego token expirado para prueba de seguridad")
    public void agregoTokenExpirado() {
        getHttpClient().addHeader("Authorization", "Bearer EXPIRED_TOKEN_FOR_SECURITY_TEST");
    }

    @Given("agrego token inválido para prueba de seguridad")
    public void agregoTokenInvalido() {
        getHttpClient().addHeader("Authorization", "Bearer INVALID_TOKEN_xyz");
    }

    @Given("no agrego autenticación")
    public void noAgregoAutenticacion() {
        getHttpClient().removeHeader("Authorization");
    }
}
