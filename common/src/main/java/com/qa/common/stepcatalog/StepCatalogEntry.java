package com.qa.common.stepcatalog;

import java.util.List;
import java.util.Objects;

/**
 * Entrada del catálogo i18n para un step BDD individual.
 *
 * <p>Esta clase transporta <em>claves</em> de i18n, no valores resueltos.
 * El BE (a través de {@code StepI18nBundleLoader}) resuelve las claves
 * al locale solicitado en tiempo de request.
 *
 * <p>Invariante: el patrón Cucumber ({@link #pattern}) es siempre en inglés
 * — es el contrato literal que usa Cucumber para hacer matching con las
 * step definitions Java. Nunca se traduce.
 *
 * @param stepId         identificador estable del step (ej: "web.click.click")
 * @param pattern        patrón Cucumber en inglés (ej: "I click {string}")
 * @param layer          capa tecnológica del step
 * @param phase          fase BDD: GIVEN | WHEN | THEN
 * @param categoryKey    clave i18n de la categoría (ej: "step.web.category.interaction")
 * @param descriptionKey clave i18n de la descripción del step
 * @param params         especificaciones de parámetros con sus claves i18n
 */
public record StepCatalogEntry(
        String stepId,
        String pattern,
        StepLayer layer,
        String phase,
        String categoryKey,
        String descriptionKey,
        List<ParamSpec> params
) {

    public StepCatalogEntry {
        Objects.requireNonNull(stepId,         "stepId no puede ser null");
        Objects.requireNonNull(pattern,        "pattern no puede ser null");
        Objects.requireNonNull(layer,          "layer no puede ser null");
        Objects.requireNonNull(phase,          "phase no puede ser null");
        Objects.requireNonNull(categoryKey,    "categoryKey no puede ser null");
        Objects.requireNonNull(descriptionKey, "descriptionKey no puede ser null");
        params = (params != null) ? List.copyOf(params) : List.of();
    }

    public boolean hasParams() {
        return !params.isEmpty();
    }
}
