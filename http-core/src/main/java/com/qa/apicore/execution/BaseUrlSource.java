package com.qa.apicore.execution;

/**
 * Origen de la base URL efectiva usada durante la ejecución de un escenario.
 *
 * <p>El {@link BaseUrlTracker} registra este valor en cada cambio de host,
 * permitiendo al BE persitir y al FE mostrar de dónde salió la URL final.
 */
public enum BaseUrlSource {

    /** URL tomada automáticamente del ambiente seleccionado en la plataforma (bootstrap). */
    ENVIRONMENT,

    /** URL establecida por un step con un literal hardcodeado (ej. {@code "https://api.prod.com"}). */
    STEP_LITERAL,

    /** URL establecida por un step que resolvió una variable del ambiente ({@code ${KEY}}). */
    STEP_VARIABLE,

    /** URL leída de un archivo {@code *.properties} local (comportamiento legacy). */
    PROPERTY_FILE,

    /** Ningún origen explícito: se usó el default interno del Core. Siempre genera un warning. */
    DEFAULT
}
