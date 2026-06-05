package com.qa.httpcore.bootstrap;

import com.microsoft.playwright.APIRequestContext;
import com.qa.common.api.runtime.ApiContextHolder;
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
 * <h2>Sesión compartida o standalone (FEC-API-SHIP-WEB-SHARE)</h2>
 * <p>El supplier registrado construye un {@link PlaywrightHttpEngine} cuyo proveedor de
 * contexto consulta {@link ApiContextHolder} <b>en cada petición</b>:
 * <ul>
 *   <li>Escenario híbrido {@code @web+@api}: {@code web-core.WebPlugin} publicó el
 *       {@code APIRequestContext} del browser en el holder → el motor reutiliza
 *       cookies/storage/auth de la sesión del navegador (el diferenciador de UC-2).</li>
 *   <li>{@code @api} puro: el holder está vacío ({@code null}) → el motor se auto-provee
 *       un contexto standalone (FEC-API-SHIP-CORE). Sin browser, sin regresión.</li>
 * </ul>
 * La resolución <em>por petición</em> (no capturada en construcción) evita reutilizar un
 * contexto de browser ya cerrado entre escenarios.</p>
 *
 * <p>El acoplamiento {@code http-core}↔{@code web-core} se evita usando el puerto neutral
 * {@link ApiContextHolder} (en {@code common}, tipado {@code Object}): {@code web-core}
 * publica, {@code http-core} castea a {@code APIRequestContext}. {@code common} permanece
 * Playwright-free y el consumo dual (CLI {@code qa-module-test} sin BE) se preserva.</p>
 *
 * @author Abel Venero
 * @since FEC-API-SHIP-CORE
 * @see HttpClientFactory#register(HttpEngine, java.util.function.Supplier)
 * @see PlaywrightHttpEngine
 * @see ApiContextHolder
 */
public final class HttpEngineBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(HttpEngineBootstrap.class);

    private HttpEngineBootstrap() {
        throw new UnsupportedOperationException("HttpEngineBootstrap es una clase utilitaria");
    }

    /**
     * Registra el motor {@link HttpEngine#PLAYWRIGHT} con un supplier que prefiere la sesión
     * del browser ({@link ApiContextHolder}) y cae a standalone si no hay. Es idempotente:
     * {@link HttpClientFactory#register} sobreescribe el supplier anterior, así que invocarlo
     * varias veces (p.ej. una vez por ejecución desde {@code ApiPlugin.registerServices}) es seguro.
     *
     * <p>Defensivo ante {@link NoClassDefFoundError}: si una distribución del assembly
     * excluye las clases de Playwright del classpath, la resolución del supplier fallaría al
     * cargar {@code PlaywrightHttpEngine}/{@code APIRequestContext}. En ese caso se loguea en
     * INFO y NO se registra el motor; {@link HttpClientFactory} seguirá usando {@code APACHE}
     * como fallback seguro (la ejecución no falla).</p>
     */
    public static void register() {
        try {
            // Proveedor por-petición: en cada request el engine consulta el holder. Con sesión
            // de browser publicada (@web+@api) reutiliza su contexto (cookies/auth compartidos);
            // sin ella (@api puro) el holder da null y el engine se auto-provee standalone.
            // El cast Object→APIRequestContext es seguro: sólo web-core puebla el holder, con un
            // APIRequestContext (o null). Resolver por-petición evita capturar un contexto stale.
            HttpClientFactory.register(HttpEngine.PLAYWRIGHT,
                    () -> new PlaywrightHttpEngine(() -> (APIRequestContext) ApiContextHolder.current()));
        } catch (NoClassDefFoundError e) {
            LOG.info("Motor HTTP PLAYWRIGHT no disponible (Playwright ausente del classpath: {}). "
                    + "HttpClientFactory usará APACHE como fallback.", e.getMessage());
        }
    }
}
