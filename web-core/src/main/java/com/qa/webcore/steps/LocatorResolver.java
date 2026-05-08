package com.qa.webcore.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convierte descripciones semánticas de elementos en selector strings compatibles con Playwright.
 *
 * <h2>Tres niveles de localización</h2>
 * <ul>
 *   <li><b>Nivel 1 — Semántico puro:</b> rol ARIA, texto visible, aria-label. Resiliente a
 *       cambios de CSS, IDs y estructura DOM. Siempre preferido.</li>
 *   <li><b>Nivel 2 — data-testid:</b> contrato explícito entre QA y DEV.
 *       Estable por convención, no por implementación.</li>
 *   <li><b>Nivel 3 — CSS/XPath (escape hatch):</b> para apps legacy o terceros. Registra
 *       {@value #FRAGILE_MARKER} en el log para que el equipo sea consciente de la fragilidad.</li>
 * </ul>
 *
 * <h2>Formato de selector</h2>
 * <p>Los strings retornados son compatibles con el motor de selectores de Playwright y con
 * {@code PlaywrightBrowserEngine.normalizeSelector()}: se usan directamente en
 * {@link com.qa.webcore.driver.engine.BrowserEngine#find(String)},
 * {@link com.qa.webcore.driver.engine.BrowserEngine#click(String)}, etc.
 *
 * @since 3.2.0
 */
public final class LocatorResolver {

    static final String FRAGILE_MARKER = "[FRAGILE LOCATOR]";
    private static final Logger LOG = LoggerFactory.getLogger(LocatorResolver.class);

    private LocatorResolver() {}

    // =========================================================================
    // Nivel 1 — Semántico puro
    // =========================================================================

    /**
     * Selector por texto visible exacto.
     * Playwright: {@code text=value} (búsqueda normalizada, insensible a espacios).
     *
     * @param text texto visible del elemento
     */
    public static String byText(String text) {
        return "text=" + text;
    }

    /**
     * Selector por rol ARIA + nombre accesible.
     * Playwright: {@code role=button[name="Submit"]}
     *
     * @param role rol ARIA estándar (button, link, heading, textbox, checkbox, combobox, etc.)
     * @param name nombre accesible del elemento (texto visible del botón, aria-label, etc.)
     */
    public static String byRole(String role, String name) {
        return "role=" + role.toLowerCase() + "[name=\"" + name + "\"]";
    }

    /**
     * Selector por rol ARIA sin nombre (útil para aserciones de existencia de elementos de rol).
     *
     * @param role rol ARIA estándar
     */
    public static String byRole(String role) {
        return "role=" + role.toLowerCase();
    }

    /**
     * Selector por etiqueta ARIA ({@code aria-label}).
     * Para inputs, textareas y selects con aria-label explícito.
     *
     * <p>Si el campo usa el patrón HTML clásico ({@code <label for="id">}), preferir
     * {@link #byTestId} o contactar al equipo DEV para añadir {@code aria-label}.
     *
     * @param label valor del atributo {@code aria-label}
     */
    public static String byLabel(String label) {
        return "[aria-label=\"" + label + "\"]";
    }

    /**
     * Selector por placeholder de campo de formulario.
     * Complemento a {@link #byLabel} cuando no hay aria-label pero sí placeholder.
     *
     * @param placeholder texto del placeholder
     */
    public static String byPlaceholder(String placeholder) {
        return "[placeholder=\"" + placeholder + "\"]";
    }

    // =========================================================================
    // Nivel 1+ — Semántico con scope o índice
    // =========================================================================

    /**
     * Selector encadenado: elemento dentro de un contenedor semántico.
     * Usa la sintaxis {@code container >> inner} de Playwright para scoping.
     *
     * @param inner     selector del elemento buscado (cualquier nivel)
     * @param container selector del contenedor (cualquier nivel)
     */
    public static String inContainer(String inner, String container) {
        return container + " >> " + inner;
    }

    /**
     * Selector de la n-ésima ocurrencia (1-based) de un elemento.
     * Playwright usa índices 0-based internamente; este método convierte.
     *
     * @param selector selector base del elemento
     * @param n        posición 1-based (el 1.° elemento, el 2.° elemento…)
     */
    public static String nthOf(String selector, int n) {
        if (n < 1) throw new IllegalArgumentException("n debe ser >= 1, fue: " + n);
        return selector + " >> nth=" + (n - 1);
    }

    // =========================================================================
    // Nivel 2 — data-testid (contrato DEV↔QA)
    // =========================================================================

    /**
     * Selector por atributo {@code data-testid} — contrato acordado entre DEV y QA.
     * Estable por convención: el DEV añade el atributo, el QA lo referencia.
     *
     * @param testId valor del atributo {@code data-testid}
     */
    public static String byTestId(String testId) {
        return "[data-testid=\"" + testId + "\"]";
    }

    /**
     * Selector por testId dentro de un contenedor.
     *
     * @param testId    atributo {@code data-testid} del elemento
     * @param container selector del contenedor
     */
    public static String byTestIdInContainer(String testId, String container) {
        return inContainer(byTestId(testId), container);
    }

    // =========================================================================
    // Nivel 3 — Escape hatch CSS/XPath
    // =========================================================================

    /**
     * Selector CSS literal (escape hatch — último recurso).
     *
     * <p>Registra {@value #FRAGILE_MARKER} en el log. El equipo debe ser consciente de que
     * este test es frágil y debe migrarse a Nivel 1 o 2 cuando sea posible.
     *
     * @param cssSelector selector CSS (ej: {@code .my-class}, {@code #my-id > button})
     */
    public static String byCss(String cssSelector) {
        LOG.warn("{} CSS selector '{}' — considerar migrar a selector semántico o data-testid.",
                FRAGILE_MARKER, cssSelector);
        return cssSelector;
    }

    /**
     * Selector XPath literal (escape hatch — último recurso).
     *
     * <p>Registra {@value #FRAGILE_MARKER} en el log. El engine añade el prefijo {@code xpath=}
     * si no está presente (compatible con {@code PlaywrightBrowserEngine.normalizeSelector}).
     *
     * @param xpath expresión XPath (ej: {@code //button[@id='submit']})
     */
    public static String byXPath(String xpath) {
        LOG.warn("{} XPath selector '{}' — considerar migrar a selector semántico o data-testid.",
                FRAGILE_MARKER, xpath);
        return xpath.startsWith("xpath=") ? xpath : "xpath=" + xpath;
    }
}
