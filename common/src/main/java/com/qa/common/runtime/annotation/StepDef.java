package com.qa.common.runtime.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declara el identificador estable de un step BDD individual (a nivel método).
 *
 * <p>Esta anotación complementa {@link StepId} elevando la granularidad del catálogo
 * desde el nivel de <em>componente</em> hasta el nivel de <em>método de step</em>.
 * Mientras {@link StepId} identifica el grupo de steps (ej: {@code "api.authentication"}),
 * {@code @StepDef} identifica el step individual dentro de ese grupo
 * (ej: {@code "api.authentication.bearer.rut"}).
 *
 * <h2>Cuándo usar {@code @StepDef}</h2>
 * <p>La anotación es <strong>opcional</strong>. Sin ella, el
 * {@link com.qa.common.runtime.StepMethodScanner} aún detecta el step y le asigna un ID
 * derivado del ID del componente y el nombre del método Java. Usa {@code @StepDef} cuando:
 * <ul>
 *   <li>Quieres un ID estable que no dependa del nombre del método Java (renombrar el método
 *       no romperá los escenarios persistidos en el Backend).</li>
 *   <li>El step está siendo deprecado y necesitas declarar un sucesor explícito.</li>
 *   <li>Quieres un {@link #displayName()} más legible que el texto literal del patrón Cucumber.</li>
 * </ul>
 *
 * <h2>Formato del ID</h2>
 * <pre>
 *   {componentId}.{sub-identificador}
 * </pre>
 * <p>donde {@code componentId} es el {@link StepId#value()} del componente padre.
 *
 * <h2>Ejemplos</h2>
 * <pre>
 *   // En AuthenticationSteps.java (componente padre: @StepId("api.authentication"))
 *   &#64;StepDef("api.authentication.bearer.rut")
 *   &#64;Given("agrego autenticación Bearer para RUT {string}")
 *   public void agregoAutenticacionBearerParaRUT(String rut) { ... }
 *
 *   &#64;StepDef("api.authentication.client.credentials")
 *   &#64;Given("agrego autenticacion Client Credentials")
 *   public void agregoAutenticacionClientCredentials() { ... }
 *
 *   // En NavigationSteps.java (componente padre: @StepId("web.navigation"))
 *   &#64;StepDef("web.navigation.open.url")
 *   &#64;When("navego a la URL {string}")
 *   public void navegoALaUrl(String url) { ... }
 * </pre>
 *
 * <h2>Ciclo de deprecación</h2>
 * <pre>
 *   // Paso 1: marcar el step como deprecated durante al menos una release
 *   &#64;StepDef(value = "api.auth.old.step", deprecated = true, replacedBy = "api.auth.bearer.rut")
 *   &#64;Given("patron antiguo")
 *   public void stepAntiguo() { ... }
 *
 *   // Paso 2: en la siguiente release, eliminar el método antiguo
 * </pre>
 *
 * <h2>Contrato de estabilidad</h2>
 * <p>El valor de {@link #value()} es un <strong>contrato público</strong>:
 * <ul>
 *   <li>El Backend puede almacenarlo en la BD al persistir escenarios.</li>
 *   <li>El Frontend lo usa como clave en el Scenario Builder para steps individuales.</li>
 *   <li><strong>Nunca cambies un ID sin marcar el anterior como
 *       {@link #deprecated() deprecated} y declarar {@link #replacedBy()}.</strong></li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.2.0
 * @see StepId
 * @see com.qa.common.runtime.StepMethodScanner
 * @see com.qa.common.runtime.StepDefinitionInfo
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StepDef {

    /**
     * Identificador estable del step individual.
     *
     * <p>Formato recomendado: {@code {componentId}.{sub-identificador}}
     *
     * <p>Ejemplos: {@code "api.authentication.bearer.rut"},
     * {@code "web.navigation.open.url"}, {@code "db.setup.connect.oracle"}.
     *
     * @return ID único del step, nunca blank
     */
    String value();

    /**
     * Indica si este step está deprecado.
     *
     * <p>El {@link com.qa.common.runtime.StepMethodScanner} emite una advertencia al
     * descubrirlo y propaga el flag en
     * {@link com.qa.common.runtime.StepDefinitionInfo#deprecated()}.
     *
     * @return {@code true} si el step está deprecado; {@code false} por defecto
     */
    boolean deprecated() default false;

    /**
     * ID del step que reemplaza a este cuando {@link #deprecated()} es {@code true}.
     *
     * <p>El Backend usa este valor para sugerir al Frontend qué step seleccionar
     * en lugar del deprecado y para migrar escenarios persistidos.
     * Vacío significa que no hay reemplazo conocido aún.
     *
     * @return ID del step sucesor, o string vacío si no aplica
     */
    String replacedBy() default "";

    /**
     * Nombre legible del step para el Frontend y la documentación generada.
     *
     * <p>Si está vacío, el consumidor debe derivar el nombre del patrón Cucumber
     * (el texto de {@code @Given}, {@code @When} o {@code @Then}), que en general
     * ya es suficientemente descriptivo.
     *
     * @return nombre de display, o string vacío para auto-derivar del patrón
     */
    String displayName() default "";
}
