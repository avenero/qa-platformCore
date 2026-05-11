package com.qa.common.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca una clase como **detalle de implementación interna** de `common`.
 *
 * <p>Las clases anotadas con {@code @Internal} viven típicamente en
 * {@code com.qa.common.internal.**}. Su contrato puede cambiar entre minor
 * releases sin warning — los consumidores externos (proyectos que importan
 * `common`, módulos especializados del Core, BE) <strong>NO deben
 * depender</strong> de ellas.
 *
 * <h2>¿Qué pasa si lo importás?</h2>
 *
 * <p>El guard ArchUnit {@code ApiInternalBoundaryTest} (en {@code common}) y
 * sus contrapartes en cada módulo especializado y BE fallan el build si una
 * clase fuera de {@code common} importa una clase {@code @Internal} (con
 * excepciones whitelisted documentadas en el propio test).
 *
 * <h2>¿Cuál es la alternativa?</h2>
 *
 * <p>Cada clase {@code @Internal} típicamente tiene un contrato público
 * equivalente en {@code com.qa.common.api.**} o se accede vía un puerto
 * {@code com.qa.common.spi.**}. El atributo {@link #reason()} indica cuál
 * es la API pública a usar.
 *
 * <h2>Ejemplo</h2>
 *
 * <pre>
 * &#64;Internal(reason = "Usar com.qa.common.api.runtime.LifecycleManager")
 * public class DefaultLifecycleManager implements LifecycleManager { ... }
 * </pre>
 *
 * <h2>Reglas inviolables (TASK-K02)</h2>
 *
 * <ul>
 *   <li><b>R-K02-1:</b> toda clase pública en {@code com.qa.common.internal.**}
 *       debe llevar {@code @Internal}.</li>
 *   <li><b>R-K02-2:} los consumidores externos NO importan clases
 *       {@code @Internal}. Excepciones se documentan en el test ArchUnit del
 *       consumer, con razón y plan de salida (puerto / api equivalente).</li>
 *   <li><b>R-K02-3:</b> {@code api.*} NO importa {@code internal.*} (regla
 *       direccional — internal implementa api).</li>
 * </ul>
 *
 * @author Abel Venero
 * @since TASK-K02 (2026-05-11)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface Internal {
    /**
     * Razón breve por la que la clase es interna + sugerencia de qué usar
     * en su lugar. Aparece en Javadoc generado y en error messages de
     * ArchUnit (cuando aplique).
     */
    String reason() default "Implementation detail. Use the corresponding api/* contract.";
}
