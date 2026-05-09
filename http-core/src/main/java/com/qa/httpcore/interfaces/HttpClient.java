package com.qa.httpcore.interfaces;

/**
 * Contrato compuesto para clientes HTTP del QA Automation Framework.
 *
 * <p>Une las cuatro responsabilidades segregadas según el ISP
 * (Interface Segregation Principle):
 * <ul>
 *   <li>{@link HttpRequestBuilder}    — configuración de petición (host, headers,
 *       body, cookies, user-context, content-type)</li>
 *   <li>{@link HttpRequestExecutor}   — ejecución de peticiones (GET, POST, PUT,
 *       DELETE, PATCH y variantes con timeout / redirects)</li>
 *   <li>{@link HttpResponseAccessor}  — acceso de solo lectura a la última respuesta
 *       (URL, método, duración)</li>
 *   <li>{@link HttpClientConfigurator} — configuración técnica (timeouts, retry,
 *       reset, debug)</li>
 * </ul>
 *
 * <h3>Por qué segregar</h3>
 * <p>Los steps de API raramente necesitan todo el contrato a la vez:
 * <ul>
 *   <li>Los <b>given-steps</b> solo necesitan {@link HttpRequestBuilder}.</li>
 *   <li>Los <b>when-steps</b> solo necesitan {@link HttpRequestExecutor}.</li>
 *   <li>Los <b>then-steps</b> solo necesitan {@link HttpResponseAccessor}.</li>
 *   <li>Los <b>config-steps</b> solo necesitan {@link HttpClientConfigurator}.</li>
 * </ul>
 * Al declarar la dependencia con la sub-interface más estrecha se mejora la
 * testabilidad, se reduce el acoplamiento y se explicita la intención.
 *
 * <h3>Compatibilidad hacia atrás</h3>
 * <p>{@code BaseHttpClient} ya implementa todos los métodos; este cambio es
 * puramente aditivo y no rompe ningún código existente.
 *
 * @author Abel Venero
 * @version 2.0.0
 * @since 2024
 */
public interface HttpClient
        extends HttpRequestBuilder,
                HttpRequestExecutor,
                HttpResponseAccessor,
                HttpClientConfigurator {

    // Contrato compuesto — todos los métodos son heredados de las sub-interfaces.
    // No agregar métodos aquí; usad la sub-interface correcta según responsabilidad.
}
