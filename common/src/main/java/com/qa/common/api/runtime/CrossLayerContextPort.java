package com.qa.common.api.runtime;

import java.util.Optional;

/**
 * Port de acceso al contexto compartido entre capas dentro de una ejecución BDD.
 *
 * <p>Permite que steps de diferentes capas (API, WEB, MOBILE) compartan datos
 * dentro del mismo escenario sin acoplarse directamente entre sí.
 *
 * <p>Implementación por defecto: {@link StepExecutionContext}.
 *
 * @since 2.1.0
 */
public interface CrossLayerContextPort {

    /**
     * Guarda un valor en el contexto con la clave indicada.
     * Sobreescribe el valor anterior si existe.
     */
    void store(String key, Object value);

    /**
     * Recupera el valor asociado a la clave.
     *
     * @param key clave usada en {@link #store(String, Object)}
     * @return Optional con el valor, o vacío si no existe
     */
    Optional<Object> retrieve(String key);

    /**
     * Resuelve las variables {@code {key}} dentro de una cadena de plantilla.
     *
     * <p>Ejemplo: {@code "GET /users/{userId}"} → {@code "GET /users/42"}
     * si {@code "userId" = 42} fue guardado previamente.
     *
     * @param template cadena que puede contener {@code {key}} como placeholders
     * @return cadena con los placeholders reemplazados por sus valores
     */
    String resolveVariables(String template);

    /**
     * Transfiere el valor de {@code fromKey} en este contexto a {@code toKey} en {@code target}.
     * Útil para pasar datos de una capa a otra de forma explícita.
     *
     * @param fromKey  clave de origen en este contexto
     * @param toKey    clave de destino en el contexto de la capa siguiente
     * @param target   contexto de la capa receptora
     */
    void transfer(String fromKey, String toKey, CrossLayerContextPort target);

    /** Limpia todos los valores almacenados. */
    void clear();

    /** @return true si el contexto no contiene ningún valor */
    boolean isEmpty();
}
