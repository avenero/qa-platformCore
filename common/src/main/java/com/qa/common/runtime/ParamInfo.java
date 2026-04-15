package com.qa.common.runtime;

import java.util.Objects;

/**
 * Metadata de un parámetro individual de un step BDD.
 *
 * <p>Captura la información estática disponible vía reflexión para cada parámetro
 * de un método de step Cucumber. Combina dos perspectivas complementarias:
 * <ul>
 *   <li><strong>Perspectiva Java</strong>: nombre del parámetro y tipo Java (ej: {@code "String"},
 *       {@code "int"}, {@code "Map"}).</li>
 *   <li><strong>Perspectiva Cucumber</strong>: token de la Cucumber Expression extraído del patrón
 *       (ej: {@code "{string}"}, {@code "{int}"}, {@code "{word}"}). Puede ser {@code null}
 *       para parámetros especiales como {@code DataTable} o {@code DocString} que no tienen
 *       token explícito en el texto del patrón.</li>
 * </ul>
 *
 * <h2>Obtención de nombres</h2>
 * <p>El campo {@link #name()} requiere que el código fuente se compile con la flag
 * {@code -parameters} del compilador Java para que la JVM preserve los nombres de parámetros
 * en el bytecode. Sin esa flag, el valor fallback es {@code "arg0"}, {@code "arg1"}, etc.
 * (comportamiento estándar de Java pre-reflection-parameters).
 *
 * <h2>Parámetros especiales</h2>
 * <p>Cucumber inyecta algunos parámetros sin que aparezcan como tokens {@code {}} en el patrón:
 * <ul>
 *   <li>{@code io.cucumber.datatable.DataTable} — tabla de datos en el feature file.</li>
 *   <li>{@code io.cucumber.java.DocStringType} — bloque de texto multilínea.</li>
 * </ul>
 * Para estos, {@link #cucumberToken()} es {@code null} y {@link #hasCucumberToken()} retorna
 * {@code false}.
 *
 * @param position      posición 0-based del parámetro en la firma del método
 * @param name          nombre del parámetro Java (o {@code "argN"} si no hay info de debug)
 * @param javaType      nombre simple del tipo Java (ej: {@code "String"}, {@code "int"},
 *                      {@code "Map"}, {@code "DataTable"})
 * @param cucumberToken token Cucumber extraído del patrón (ej: {@code "{string}"},
 *                      {@code "{int}"}); {@code null} para parámetros especiales
 *
 * @author Abel Venero
 * @since 2.2.0
 * @see StepDefinitionInfo
 * @see StepMethodScanner
 */
public record ParamInfo(
        int position,
        String name,
        String javaType,
        String cucumberToken
) {

    /**
     * Compact constructor: valida invariantes de los campos requeridos.
     *
     * <p>{@link #cucumberToken()} es nullable a propósito: es {@code null} cuando el
     * parámetro es de tipo especial (DataTable, DocString) que Cucumber inyecta sin
     * generar un token {@code {}} en el patrón.
     */
    public ParamInfo {
        if (position < 0) {
            throw new IllegalArgumentException("position no puede ser negativa: " + position);
        }
        Objects.requireNonNull(name,     "name no puede ser null");
        Objects.requireNonNull(javaType, "javaType no puede ser null");
        // cucumberToken es nullable — null para DataTable/DocString
    }

    /**
     * Indica si este parámetro tiene un token Cucumber correspondiente en el patrón.
     *
     * <p>Retorna {@code false} para parámetros especiales como {@code DataTable} y
     * {@code DocString}, que son inyectados por el motor Cucumber sin presencia explícita
     * en el texto del patrón.
     *
     * @return {@code true} si {@link #cucumberToken()} no es null
     */
    public boolean hasCucumberToken() {
        return cucumberToken != null;
    }
}
