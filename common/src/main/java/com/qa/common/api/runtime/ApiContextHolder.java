package com.qa.common.api.runtime;

/**
 * Puerto neutral por-hilo para compartir el contexto HTTP de una sesión de browser
 * entre los módulos especializados {@code web-core} (productor) y {@code http-core}
 * (consumidor), sin que ninguno conozca al otro (FEC-API-SHIP-WEB-SHARE).
 *
 * <h2>Por qué {@link Object} y no {@code APIRequestContext}</h2>
 * <p>El valor guardado es, en la práctica, un {@code com.microsoft.playwright.APIRequestContext}
 * (el {@code request()} del {@code BrowserContext} activo, que comparte cookies/storage/auth
 * con las páginas del navegador). Pero {@code common} <b>no puede importar Playwright</b>
 * (ArchUnit {@code common_does_not_depend_on_automation_frameworks}, R-H03-FW). Como
 * {@code web-core} y {@code http-core} son <em>siblings</em> que no pueden referenciarse
 * entre sí, {@link Object} es el menor común denominador: {@code web-core} hace
 * {@code set(apiRequestContext)} (upcast a Object) y {@code http-core} hace
 * {@code (APIRequestContext) current()} (downcast). Sin SPI nuevo, sin dependencia nueva.
 *
 * <h2>Semántica</h2>
 * <ul>
 *   <li><b>ThreadLocal</b>: el contexto es por-hilo, igual que el {@code BrowserContext}
 *       de {@code PlaywrightManager}. En un escenario híbrido {@code @web+@api} los steps
 *       de ambos plugins corren en el mismo hilo, así que el step {@code @api} ve el valor
 *       publicado por el plugin {@code @web}.</li>
 *   <li><b>Vacío por defecto</b>: si no hay sesión de browser ({@code @api} puro),
 *       {@link #current()} retorna {@code null} y el motor HTTP cae a modo standalone.</li>
 *   <li><b>Limpieza obligatoria</b>: el productor debe llamar {@link #clear()} al terminar
 *       el escenario (en {@code finally}) para no filtrar un contexto ya cerrado al siguiente.</li>
 * </ul>
 *
 * @author Abel Venero
 * @since FEC-API-SHIP-WEB-SHARE
 */
public final class ApiContextHolder {

    private static final ThreadLocal<Object> HOLDER = new ThreadLocal<>();

    private ApiContextHolder() {
        throw new UnsupportedOperationException("ApiContextHolder es una clase utilitaria");
    }

    /**
     * Publica el contexto HTTP de la sesión de browser para el hilo actual.
     * Lo invoca {@code web-core.WebPlugin} al iniciar un escenario {@code @web}.
     *
     * @param apiContext típicamente un {@code APIRequestContext} de Playwright; puede ser
     *                   {@code null} (equivalente a no tener sesión compartida).
     */
    public static void set(Object apiContext) {
        HOLDER.set(apiContext);
    }

    /**
     * @return el contexto publicado para el hilo actual, o {@code null} si no hay sesión
     *         de browser activa ({@code @api} puro → el motor se auto-provee standalone).
     */
    public static Object current() {
        return HOLDER.get();
    }

    /** Limpia el contexto del hilo actual. Llamar en {@code finally} al cerrar el escenario. */
    public static void clear() {
        HOLDER.remove();
    }
}
