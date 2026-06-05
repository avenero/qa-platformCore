package com.qa.httpcore.bootstrap;

import com.qa.common.api.runtime.HttpEngine;
import com.qa.httpcore.factories.HttpClientFactory;
import com.qa.httpcore.implementations.PlaywrightHttpEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registra el motor HTTP {@link HttpEngine#PLAYWRIGHT} en {@link HttpClientFactory}
 * durante el bootstrap del plugin de API (FEC-API-SHIP-CORE).
 *
 * <h2>Por qué existe</h2>
 * <p>{@link HttpEngine#resolveDefault()} retorna {@code PLAYWRIGHT} y el FE lo ofrece
 * como motor por defecto, pero hasta ahora {@code HttpClientFactory} sólo tenía
 * pre-registrado {@code APACHE} — toda ejecución {@code @api} caía silenciosamente a
 * Apache aunque la UI dijera PLAYWRIGHT. Este bootstrap cierra esa brecha registrando
 * el motor real.</p>
 *
 * <h2>Modo standalone self-provision</h2>
 * <p>El supplier registrado construye un {@link PlaywrightHttpEngine} en modo
 * <em>standalone</em> (constructor sin argumentos): cuando no hay sesión de browser
 * disponible, el engine auto-provee su propio {@code APIRequestContext} de Playwright.
 * De esta forma {@code @api} puro funciona sin browser y sin acoplar {@code http-core}
 * a {@code web-core}, preservando la regla de aislamiento de módulos y el consumo dual
 * (CLI {@code qa-module-test} sin BE).</p>
 *
 * <p>El cableado con la sesión del browser para escenarios híbridos {@code @web+@api}
 * lo aporta FEC-API-SHIP-WEB-SHARE editando este mismo bootstrap; {@code common}
 * permanece Playwright-free.</p>
 *
 * @author Abel Venero
 * @since FEC-API-SHIP-CORE
 * @see HttpClientFactory#register(HttpEngine, java.util.function.Supplier)
 * @see PlaywrightHttpEngine
 */
public final class HttpEngineBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(HttpEngineBootstrap.class);

    private HttpEngineBootstrap() {
        throw new UnsupportedOperationException("HttpEngineBootstrap es una clase utilitaria");
    }

    /**
     * Registra el motor {@link HttpEngine#PLAYWRIGHT} con un supplier en modo standalone
     * self-provision. Es idempotente: {@link HttpClientFactory#register} sobreescribe el
     * supplier anterior, así que invocarlo varias veces (p.ej. una vez por ejecución desde
     * {@code ApiPlugin.registerServices}) es seguro.
     *
     * <p>Defensivo ante {@link NoClassDefFoundError}: si una distribución del assembly
     * excluye el binario/clases de Playwright del classpath, la resolución de
     * {@code PlaywrightHttpEngine::new} fallaría al cargar la clase. En ese caso se loguea
     * en INFO y NO se registra el motor; {@link HttpClientFactory} seguirá usando
     * {@code APACHE} como fallback seguro (la ejecución no falla).</p>
     */
    public static void register() {
        try {
            HttpClientFactory.register(HttpEngine.PLAYWRIGHT, PlaywrightHttpEngine::new);
        } catch (NoClassDefFoundError e) {
            LOG.info("Motor HTTP PLAYWRIGHT no disponible (Playwright ausente del classpath: {}). "
                    + "HttpClientFactory usará APACHE como fallback.", e.getMessage());
        }
    }
}
