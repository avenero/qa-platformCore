package com.qa.apicore.steps.config;

import com.qa.common.runtime.annotation.StepMetadata;
import com.qa.apicore.execution.BaseUrlSource;
import com.qa.apicore.plugin.ApiPlugin;
import com.qa.apicore.utils.ApiHelper;
import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.annotation.StepDef;
import io.cucumber.java.en.Given;

/**
 * Steps de configuración de URL/Ambiente para peticiones HTTP.
 *
 * <p>Componente padre: {@code api.url} ({@link com.qa.apicore.components.ApiUrlComponent}).
 * Fase BDD: GIVEN.
 *
 * <p><b>Gestión de base URL:</b> todos los steps que modifican el host pasan por
 * {@link ApiPlugin#applyBaseUrlOverride}, que:
 * <ol>
 *   <li>Valida la política {@code base.url.override.allowed} del proyecto.</li>
 *   <li>Registra el cambio en el {@link com.qa.apicore.execution.BaseUrlTracker}.</li>
 *   <li>Aplica el nuevo host al {@link com.qa.apicore.interfaces.HttpClient}.</li>
 * </ol>
 *
 * <p>Los steps marcados con {@code @StepMetadata(canOverrideBaseUrl=true, usesLiteralUrl=true)}
 * pueden ser bloqueados por el preflight-checker del BE en proyectos con política estricta.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class UrlConfigSteps {

    /** Milliseconds per second, used for converting timeout seconds to milliseconds. */
    private static final int MILLIS_PER_SECOND = 1000;

    private ApiHelper apiHelper() { return ApiHelper.forCurrentContext(); }

    // =========================================================================
    // Steps canónicos — con @StepDef estable
    // =========================================================================

    /**
     * Configura el endpoint a partir de una clave de propiedad (ej: {@code "api.baseurl"})
     * o de una URL directa. Es el step principal de configuración de destino HTTP.
     *
     * <p>Si la clave resuelve a una URL absoluta, se registra como {@link BaseUrlSource#STEP_VARIABLE}.
     * Si la clave comienza con {@code http://} o {@code https://} directamente, se trata
     * como {@link BaseUrlSource#STEP_LITERAL}.
     */
    @StepMetadata(canOverrideBaseUrl = true, usesLiteralUrl = false)
    @StepDef(value = "api.url.set-endpoint",
             displayName = "Configurar endpoint")
    @Given("configuro el endpoint {string}")
    public void configuroElEndpoint(String propertyKey) {
        // Si parece una URL directa, registrar como literal; si es clave, como variable
        if (propertyKey.startsWith("http://") || propertyKey.startsWith("https://")) {
            ApiPlugin.applyBaseUrlOverride(
                ExecutionContext.requireCurrent(),
                propertyKey,
                BaseUrlSource.STEP_LITERAL,
                "configuro el endpoint {string}"
            );
        } else {
            // Clave de configuración: delegar a ApiHelper, que resuelve la propiedad
            apiHelper().configureEndpoint(propertyKey);
        }
    }

    /**
     * Configura el endpoint ensamblando base URL y path por separado.
     * Acepta tanto claves de configuración como URLs literales como base.
     */
    @StepMetadata(canOverrideBaseUrl = true, usesLiteralUrl = true)
    @StepDef(value = "api.url.set-base-path",
             displayName = "Configurar endpoint con base y path")
    @Given("configuro endpoint con base {string} y path {string}")
    public void configuroEndpointConBaseYPath(String baseOrKey, String pathOrKey) {
        if (baseOrKey.startsWith("http://") || baseOrKey.startsWith("https://")) {
            String base = baseOrKey.endsWith("/") ? baseOrKey : baseOrKey + "/";
            String path = pathOrKey.startsWith("/") ? pathOrKey.substring(1) : pathOrKey;
            ApiPlugin.applyBaseUrlOverride(
                ExecutionContext.requireCurrent(),
                base + path,
                BaseUrlSource.STEP_LITERAL,
                "configuro endpoint con base {string} y path {string}"
            );
        } else {
            apiHelper().configureEndpointFromConfig(baseOrKey, pathOrKey);
        }
    }

    /**
     * Establece el host base directamente con una URL literal.
     *
     * <p>Este step puede ser bloqueado en proyectos con política estricta
     * ({@code allowBaseUrlOverride=false}).
     */
    @StepMetadata(canOverrideBaseUrl = true, usesLiteralUrl = true)
    @StepDef(value = "api.url.set-host",
             displayName = "Establecer host base")
    @Given("establezco el host base como {string}")
    public void establezcoElHostBaseComo(String host) {
        ApiPlugin.applyBaseUrlOverride(
            ExecutionContext.requireCurrent(),
            host,
            BaseUrlSource.STEP_LITERAL,
            "establezco el host base como {string}"
        );
    }

    /**
     * Configura la URL completa del request en un único step (URL literal).
     */
    @StepMetadata(canOverrideBaseUrl = true, usesLiteralUrl = true)
    @StepDef(value = "api.url.set-full-url",
             displayName = "Configurar URL completa")
    @Given("configuro la URL completa {string}")
    public void configuroLaUrlCompleta(String fullUrl) {
        ApiPlugin.applyBaseUrlOverride(
            ExecutionContext.requireCurrent(),
            fullUrl,
            BaseUrlSource.STEP_LITERAL,
            "configuro la URL completa {string}"
        );
    }

    /**
     * Configura la base URL leyendo el valor de una variable definida en el ambiente.
     *
     * <p>Este step es "portable": no hardcodea la URL, sino que la resuelve desde
     * la variable del ambiente seleccionado en la plataforma.
     * No es bloqueado por la política estricta.
     */
    @StepMetadata(canOverrideBaseUrl = true, usesLiteralUrl = false)
    @StepDef(value = "api.url.set-host-from-variable",
             displayName = "Configurar host desde variable de ambiente")
    @Given("configuro la URL base desde la variable {string}")
    public void configuroUrlBaseDesdeVariable(String variableKey) {
        ExecutionContext ctx = ExecutionContext.requireCurrent();
        // Variables del ambiente se inyectan por el BE como "env.KEY"
        String value = ctx.config().getProperty("env." + variableKey)
            .or(() -> ctx.config().getProperty(variableKey))
            .orElseThrow(() -> new IllegalArgumentException(
                "La variable '" + variableKey + "' no está definida en el ambiente seleccionado. " +
                "Verificá que el ambiente tenga esa variable configurada en la plataforma."
            ));

        ApiPlugin.applyBaseUrlOverride(
            ctx,
            value,
            BaseUrlSource.STEP_VARIABLE,
            "configuro la URL base desde la variable {string}"
        );
    }

    /**
     * Fuerza el uso de la base URL del ambiente seleccionado, descartando cualquier
     * host que el cliente tuviera previamente.
     */
    @StepMetadata(canOverrideBaseUrl = false, usesLiteralUrl = false)
    @StepDef(value = "api.url.use-environment",
             displayName = "Usar URL base del ambiente seleccionado")
    @Given("uso la URL base del ambiente seleccionado")
    public void usoUrlBaseDelAmbiente() {
        ExecutionContext ctx = ExecutionContext.requireCurrent();
        String url = ctx.config().getProperty("base.url")
            .or(() -> ctx.config().getProperty("api.base.url"))
            .filter(v -> !v.isBlank())
            .orElseThrow(() -> new IllegalStateException(
                "No hay base URL configurada en el ambiente seleccionado. " +
                "Verificá que el ambiente tenga una Base URL definida en la plataforma."
            ));

        ApiPlugin.applyBaseUrlOverride(
            ctx,
            url,
            BaseUrlSource.ENVIRONMENT,
            "uso la URL base del ambiente seleccionado"
        );
    }

    /**
     * Configura el protocolo de comunicación ({@code "http"} o {@code "https"}).
     */
    @StepMetadata(canOverrideBaseUrl = false)
    @StepDef(value = "api.url.set-protocol",
             displayName = "Configurar protocolo HTTP/HTTPS")
    @Given("configuro el protocolo {string}")
    public void configuroElProtocolo(String protocol) {
        apiHelper().setProtocol(protocol.toLowerCase());
    }

    /**
     * Configura el timeout global de la petición HTTP en segundos.
     */
    @StepMetadata(canOverrideBaseUrl = false)
    @StepDef(value = "api.url.set-timeout",
             displayName = "Configurar timeout de petición")
    @Given("configuro el timeout de la petición a {int} segundos")
    public void configuroTimeoutEnSegundos(int seconds) {
        apiHelper().setTimeout(seconds * MILLIS_PER_SECOND);
    }

    /**
     * Configura la codificación del body de la petición (ej: {@code "UTF-8"}).
     */
    @StepMetadata(canOverrideBaseUrl = false)
    @StepDef(value = "api.url.set-encoding",
             displayName = "Configurar codificación de petición")
    @Given("configuro la codificación de la petición como {string}")
    public void configuroCodificacion(String encoding) {
        apiHelper().setEncoding(encoding);
    }

}
