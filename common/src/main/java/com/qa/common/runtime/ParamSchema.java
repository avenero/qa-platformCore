package com.qa.common.runtime;

import java.util.Map;
import java.util.Objects;

/**
 * Esquema lógico de un parámetro de step BDD, orientado al consumo por parte del
 * Backend, el Scenario Builder y el motor de lint/IA.
 *
 * <p>Complementa a {@link ParamInfo} (que captura los detalles de reflexión Java)
 * elevando la representación a un nivel semántico: en lugar de hablar de tipos Java
 * ({@code "String"}, {@code "int"}, {@code "Map"}), habla de tipos lógicos
 * ({@code "string"}, {@code "number"}, {@code "json"}) comprensibles por el Frontend
 * y usables para validación de macros.
 *
 * <h2>Tipos lógicos soportados</h2>
 * <table border="1">
 *   <tr><th>Tipo lógico</th><th>Tipos Java que lo generan</th></tr>
 *   <tr><td>{@code "string"}</td><td>String, CharSequence, y fallback por defecto</td></tr>
 *   <tr><td>{@code "number"}</td><td>int, Integer, long, Long, double, Double,
 *       float, Float, short, Short, BigDecimal, BigInteger</td></tr>
 *   <tr><td>{@code "boolean"}</td><td>boolean, Boolean</td></tr>
 *   <tr><td>{@code "json"}</td><td>Map (DataTable como mapa clave-valor)</td></tr>
 *   <tr><td>{@code "list"}</td><td>List (DataTable como lista de filas)</td></tr>
 *   <tr><td>{@code "table"}</td><td>DataTable (tipo nativo Cucumber)</td></tr>
 *   <tr><td>{@code "docstring"}</td><td>String sin token Cucumber (bloque triple comillas)</td></tr>
 * </table>
 *
 * <h2>Usos previstos</h2>
 * <ul>
 *   <li><strong>Macros / CustomSteps (BE):</strong> validar que los valores asignados
 *       a cada slot del macro son compatibles con el tipo lógico del parámetro real.</li>
 *   <li><strong>Scenario Builder (FE):</strong> renderizar el control de entrada correcto
 *       (text field, number input, toggle, JSON editor, data table) para cada parámetro.</li>
 *   <li><strong>Lint BDD:</strong> verificar que el valor literal en el feature file
 *       es parseable al tipo esperado antes de ejecutar.</li>
 *   <li><strong>Sugerencias IA:</strong> dar contexto semántico al modelo para que genere
 *       valores de ejemplo coherentes con el tipo del parámetro.</li>
 * </ul>
 *
 * <h2>Obtención</h2>
 * <p>El Backend y el FE no deben construir {@code ParamSchema} manualmente. La fuente
 * canónica es {@link StepDefinitionInfo#paramSchemas()}, que los deriva automáticamente
 * de los {@link ParamInfo} ya calculados por {@link StepMethodScanner}.
 *
 * <p>Para convertir un {@link ParamInfo} individualmente, usar el factory:
 * <pre>
 *   ParamSchema schema = ParamSchema.fromParamInfo(paramInfo);
 * </pre>
 *
 * @param name         nombre lógico del parámetro (igual que {@link ParamInfo#name()})
 * @param type         tipo lógico del parámetro (ver tabla supra)
 * @param required     {@code true} si el parámetro es obligatorio; siempre {@code true}
 *                     para parámetros con token Cucumber Expression
 * @param description  descripción opcional del parámetro; puede ser {@code null}
 *
 * @author Abel Venero
 * @since 2.3.0
 * @see ParamInfo
 * @see StepDefinitionInfo#paramSchemas()
 * @see StepMethodScanner
 */
public record ParamSchema(
        String name,
        String type,
        boolean required,
        String description
) {

    // =========================================================================
    // Constantes — tipos lógicos canónicos
    // =========================================================================

    /** Tipo lógico para parámetros de texto. */
    public static final String TYPE_STRING    = "string";
    /** Tipo lógico para parámetros numéricos (enteros y decimales). */
    public static final String TYPE_NUMBER    = "number";
    /** Tipo lógico para parámetros booleanos. */
    public static final String TYPE_BOOLEAN   = "boolean";
    /** Tipo lógico para parámetros JSON / mapa clave-valor (DataTable como Map). */
    public static final String TYPE_JSON      = "json";
    /** Tipo lógico para parámetros de lista de filas (DataTable como List). */
    public static final String TYPE_LIST      = "list";
    /** Tipo lógico para DataTable nativo de Cucumber. */
    public static final String TYPE_TABLE     = "table";
    /** Tipo lógico para bloques de texto multilínea (DocString). */
    public static final String TYPE_DOCSTRING = "docstring";

    // =========================================================================
    // Mapeo Java type → tipo lógico
    // =========================================================================

    /**
     * Mapa estático de tipos Java simples a tipos lógicos.
     *
     * <p>Cubre los tipos más comunes en firmas de métodos de steps Cucumber.
     * Los tipos no presentes en este mapa reciben {@link #TYPE_STRING} como fallback.
     */
    private static final Map<String, String> JAVA_TO_LOGICAL = Map.ofEntries(
            // Numéricos primitivos
            Map.entry("int",        TYPE_NUMBER),
            Map.entry("long",       TYPE_NUMBER),
            Map.entry("double",     TYPE_NUMBER),
            Map.entry("float",      TYPE_NUMBER),
            Map.entry("short",      TYPE_NUMBER),
            Map.entry("byte",       TYPE_NUMBER),
            // Numéricos boxeados
            Map.entry("Integer",    TYPE_NUMBER),
            Map.entry("Long",       TYPE_NUMBER),
            Map.entry("Double",     TYPE_NUMBER),
            Map.entry("Float",      TYPE_NUMBER),
            Map.entry("Short",      TYPE_NUMBER),
            Map.entry("Byte",       TYPE_NUMBER),
            Map.entry("BigDecimal", TYPE_NUMBER),
            Map.entry("BigInteger", TYPE_NUMBER),
            // Booleanos
            Map.entry("boolean",    TYPE_BOOLEAN),
            Map.entry("Boolean",    TYPE_BOOLEAN),
            // DataTable / colecciones
            Map.entry("DataTable",  TYPE_TABLE),
            Map.entry("Map",        TYPE_JSON),
            Map.entry("List",       TYPE_LIST),
            // String es fallback, pero declarado explícitamente
            Map.entry("String",     TYPE_STRING)
    );

    // =========================================================================
    // Compact constructor
    // =========================================================================

    /**
     * Compact constructor: valida campos requeridos.
     *
     * <p>{@code description} es nullable: puede ser {@code null} cuando no hay
     * documentación adicional para el parámetro.
     */
    public ParamSchema {
        Objects.requireNonNull(name, "name no puede ser null");
        Objects.requireNonNull(type, "type no puede ser null");
        // description es nullable intencionalmente
    }

    // =========================================================================
    // Factory methods
    // =========================================================================

    /**
     * Crea un {@code ParamSchema} a partir de un {@link ParamInfo} con mapeo de tipos.
     *
     * <p>Reglas de derivación:
     * <ul>
     *   <li>Si el {@link ParamInfo} no tiene token Cucumber ({@link ParamInfo#hasCucumberToken()}
     *       es {@code false}) y el tipo Java es {@code "String"}, se clasifica como
     *       {@link #TYPE_DOCSTRING}.</li>
     *   <li>Si no tiene token y el tipo es {@code "Map"} o {@code "DataTable"} o {@code "List"},
     *       se clasifica como {@link #TYPE_JSON}/{@link #TYPE_TABLE}/{@link #TYPE_LIST}.</li>
     *   <li>Para todos los demás casos, se aplica el mapa {@link #JAVA_TO_LOGICAL}
     *       con fallback a {@link #TYPE_STRING}.</li>
     * </ul>
     *
     * @param paramInfo {@link ParamInfo} a convertir, no null
     * @return {@code ParamSchema} equivalente; nunca null
     * @throws NullPointerException si {@code paramInfo} es null
     */
    public static ParamSchema fromParamInfo(ParamInfo paramInfo) {
        Objects.requireNonNull(paramInfo, "paramInfo no puede ser null");

        String logicalType = resolveLogicalType(paramInfo);
        return new ParamSchema(
                paramInfo.name(),
                logicalType,
                true,   // todos los parámetros Cucumber son required
                null    // description: se enriquecerá en versiones futuras via @StepDef
        );
    }

    /**
     * Crea un {@code ParamSchema} a partir de un {@link ParamInfo} con descripción explícita.
     *
     * @param paramInfo   {@link ParamInfo} a convertir, no null
     * @param description descripción opcional del parámetro; puede ser null
     * @return {@code ParamSchema} equivalente; nunca null
     */
    public static ParamSchema fromParamInfo(ParamInfo paramInfo, String description) {
        Objects.requireNonNull(paramInfo, "paramInfo no puede ser null");
        String logicalType = resolveLogicalType(paramInfo);
        return new ParamSchema(paramInfo.name(), logicalType, true, description);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Indica si este parámetro es de tipo estructurado (table, json, list, docstring).
     *
     * <p>Útil para el Scenario Builder: los parámetros estructurados requieren
     * un editor especializado (tabla, editor JSON) en lugar de un input de texto simple.
     *
     * @return {@code true} si el tipo es {@code "table"}, {@code "json"}, {@code "list"}
     *         o {@code "docstring"}
     */
    public boolean isStructured() {
        return TYPE_TABLE.equals(type)
                || TYPE_JSON.equals(type)
                || TYPE_LIST.equals(type)
                || TYPE_DOCSTRING.equals(type);
    }

    /**
     * Indica si este parámetro es numérico.
     *
     * @return {@code true} si el tipo lógico es {@link #TYPE_NUMBER}
     */
    public boolean isNumeric() {
        return TYPE_NUMBER.equals(type);
    }

    // =========================================================================
    // Resolución interna de tipo lógico
    // =========================================================================

    /**
     * Resuelve el tipo lógico de un {@link ParamInfo} aplicando las reglas de derivación.
     */
    private static String resolveLogicalType(ParamInfo p) {
        // Parámetros sin token Cucumber (DataTable / DocString)
        if (!p.hasCucumberToken()) {
            return switch (p.javaType()) {
                case "String"    -> TYPE_DOCSTRING;
                case "Map"       -> TYPE_JSON;
                case "DataTable" -> TYPE_TABLE;
                case "List"      -> TYPE_LIST;
                default          -> TYPE_STRING;
            };
        }
        // Parámetros con token: mapa Java → lógico con fallback "string"
        return JAVA_TO_LOGICAL.getOrDefault(p.javaType(), TYPE_STRING);
    }
}
