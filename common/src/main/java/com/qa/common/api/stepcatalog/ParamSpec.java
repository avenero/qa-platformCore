package com.qa.common.api.stepcatalog;

import java.util.Objects;

/**
 * Especificación de un parámetro de step con su clave i18n para el hint.
 *
 * @param name    nombre del parámetro tal como aparece en el patrón Cucumber (ej: "locator")
 * @param hintKey clave en el bundle de propiedades i18n (ej: "step.web.click.param.locator")
 */
public record ParamSpec(String name, String hintKey) {

    public ParamSpec {
        Objects.requireNonNull(name,    "name no puede ser null");
        Objects.requireNonNull(hintKey, "hintKey no puede ser null");
    }
}
