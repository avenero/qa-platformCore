package com.qa.common.api.runtime.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Metadata de comportamiento de un step BDD, complementaria a {@link StepDef}.
 *
 * <p>El {@code StepCatalogService} del BE lee esta anotación durante el descubrimiento
 * para enriquecer el catálogo con información de seguridad y trazabilidad de URL base:
 *
 * <ul>
 *   <li>{@link #canOverrideBaseUrl()} — si el step puede cambiar el host de destino.</li>
 *   <li>{@link #usesLiteralUrl()} — si la URL está hardcodeada en el pattern.</li>
 * </ul>
 *
 * <p><b>Ejemplo:</b>
 * <pre>{@code
 * @StepMetadata(canOverrideBaseUrl = true, usesLiteralUrl = true)
 * @Given("establezco el host base como {string}")
 * public void establezcoElHostBaseComo(String host) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StepMetadata {

    /**
     * {@code true} si este step puede cambiar la base URL que gobierna las peticiones HTTP.
     * El preflight-checker del BE usa este flag para validar la política del proyecto.
     */
    boolean canOverrideBaseUrl() default false;

    /**
     * {@code true} si la URL está hardcodeada como literal en el step
     * (en lugar de resolverse desde una variable del ambiente).
     * Un step con {@code usesLiteralUrl=true} ignora el ambiente seleccionado en la plataforma.
     */
    boolean usesLiteralUrl() default false;
}
