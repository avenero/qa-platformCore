package com.qa.common.api.runtime.annotation;
import com.qa.common.utils.security.SecurityUtilities;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declara el identificador estable de un {@code StepComponent}.
 *
 * <h2>Propósito</h2>
 * <p>Esta anotación fija el ID público de un componente de steps directamente en
 * su declaración de clase, haciendo la convención explícita, visible y reforzada.
 * El {@link com.qa.common.internal.runtime.StepDiscoveryService} la lee en tiempo de ejecución
 * para construir el catálogo de steps y permite al Backend resolver un componente
 * a partir de su {@code stepId} sin recorrer todos los plugins.
 *
 * <h2>Formato del ID</h2>
 * <pre>
 *   {capa}.{dominio}[.{subdominio}]
 * </pre>
 * <ul>
 *   <li>{@code capa}      — capa origen: {@code api}, {@code web}, {@code mobile}, {@code db}</li>
 *   <li>{@code dominio}   — responsabilidad principal en minúsculas con puntos</li>
 *   <li>{@code subdominio}— refinamiento opcional cuando hay varios componentes en el mismo dominio</li>
 * </ul>
 *
 * <h2>Ejemplos</h2>
 * <pre>
 *   &#64;StepId("api.authentication")
 *   &#64;StepId("api.response.body")
 *   &#64;StepId("web.browser.config")
 *   &#64;StepId("web.validation.element")
 *   &#64;StepId("mobile.device.config")
 *   &#64;StepId("db.setup")
 * </pre>
 *
 * <h2>Contrato de estabilidad</h2>
 * <p>El valor de {@link #value()} es un <strong>contrato público</strong>:
 * <ul>
 *   <li>El Backend lo almacena en la BD al persistir escenarios y ejecuciones.</li>
 *   <li>El Frontend lo usa como clave en el Scenario Builder.</li>
 *   <li><strong>Nunca cambies un ID sin marcar el anterior como
 *       {@link #deprecated() deprecated} y declarar {@link #replacedBy()}.</strong></li>
 * </ul>
 *
 * <h2>Ciclo de deprecación</h2>
 * <pre>
 *   // Paso 1: marcar el ID anterior como deprecated (mínimo una release)
 *   &#64;StepId(value = "api.old.id", deprecated = true, replacedBy = "api.new.id")
 *   public class MyComponent implements StepComponent { ... }
 *
 *   // Paso 2: en la siguiente release, cambiar al ID nuevo
 *   &#64;StepId("api.new.id")
 *   public class MyComponent implements StepComponent { ... }
 * </pre>
 *
 * @author Abel Venero
 * @since 2.1.0
 * @see com.qa.common.api.runtime.StepComponent
 * @see com.qa.common.internal.runtime.StepDiscoveryService
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StepId {

    /**
     * Identificador estable del componente.
     * <p>Formato: {@code {capa}.{dominio}[.{subdominio}]}
     * <p>Ejemplos: {@code "api.authentication"}, {@code "web.browser.config"}, {@code "db.setup"}.
     *
     * @return ID único, nunca blank
     */
    String value();

    /**
     * Indica si este ID está deprecado.
     * <p>Cuando {@code true}, el {@link com.qa.common.internal.runtime.StepDiscoveryService}
     * emite una advertencia en el log al descubrir el componente y expone la información
     * de deprecación en el DTO {@link com.qa.common.api.runtime.StepInfo}.
     *
     * @return {@code true} si el ID está deprecado; {@code false} por defecto
     */
    boolean deprecated() default false;

    /**
     * ID del componente que reemplaza a este cuando {@link #deprecated()} es {@code true}.
     * <p>String vacío significa que no hay reemplazo conocido.
     *
     * @return ID de reemplazo, o string vacío si no aplica
     */
    String replacedBy() default "";
}
