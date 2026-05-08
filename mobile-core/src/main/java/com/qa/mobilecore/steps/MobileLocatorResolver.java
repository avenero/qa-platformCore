package com.qa.mobilecore.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolución semántica de locators mobile en tres niveles.
 *
 * <p>Genera expresiones de localización compatibles con
 * {@link com.qa.mobilecore.helper.ElementLocatorHelper} (protocolo de prefijos).
 *
 * <h2>Niveles de localización</h2>
 * <pre>
 * Nivel 1  — Semántico puro     : Accessibility ID / texto visible
 * Nivel 1+ — Semántico scoped   : contenedor u ordinal (genera XPath internamente)
 * Nivel 2  — Resource ID / Pred : contrato DEV↔QA por ID estable
 * Nivel 3  — XPath / UIA / Chain: escape hatch; emite [FRAGILE LOCATOR] en el log
 * </pre>
 *
 * <p>Prefijos de {@link com.qa.mobilecore.helper.ElementLocatorHelper}:
 * <ul>
 *   <li>{@code ~<value>}     → {@code AppiumBy.accessibilityId}</li>
 *   <li>{@code id:<value>}   → {@code AppiumBy.id} (resource-id en Android)</li>
 *   <li>{@code xpath:<expr>} → {@code AppiumBy.xpath}</li>
 *   <li>{@code text:<value>} → XPath cross-platform por texto visible</li>
 *   <li>{@code pred:<value>} → iOS NSPredicate string</li>
 *   <li>{@code chain:<value>}→ iOS class chain</li>
 *   <li>{@code uia:<value>}  → Android UIAutomator</li>
 * </ul>
 *
 * @since 3.2.0
 */
public final class MobileLocatorResolver {

    static final String FRAGILE_MARKER = "[FRAGILE LOCATOR]";

    private static final Logger LOG = LoggerFactory.getLogger(MobileLocatorResolver.class);

    private MobileLocatorResolver() {}

    // =========================================================================
    // Nivel 1 — Semántico puro
    // =========================================================================

    /**
     * Localiza por Accessibility ID / content-description.
     * Portable entre Android ({@code content-desc}) e iOS ({@code accessibilityLabel}).
     */
    public static String byA11y(String label) {
        return "~" + label;
    }

    /**
     * Localiza por texto visible (cross-platform: {@code @text}, {@code @label}, {@code @value}).
     */
    public static String byText(String text) {
        return "text:" + text;
    }

    // =========================================================================
    // Nivel 1+ — Semántico con scope / índice (genera XPath internamente)
    // =========================================================================

    /**
     * Localiza el n-ésimo elemento que coincide con la expresión (1-indexed).
     *
     * <p>Genera XPath indexado. XPath es 1-based: {@code n=1} es el primer elemento.
     *
     * @param expression expresión generada por un método byXxx de esta clase
     * @param n          índice 1-based (debe ser ≥ 1)
     * @throws IllegalArgumentException si {@code n} < 1
     */
    public static String nthOf(String expression, int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n debe ser >= 1, fue: " + n);
        }
        return "xpath:(" + toXPath(expression) + ")[" + n + "]";
    }

    /**
     * Localiza {@code inner} dentro del scope de {@code container}.
     *
     * <p>Genera XPath de descendiente para componer dos expresiones semánticas.
     *
     * @param inner     expresión del elemento a encontrar (generado por byXxx)
     * @param container expresión del contenedor padre (generado por byXxx)
     */
    public static String inContainer(String inner, String container) {
        return "xpath:" + toXPath(container) + toXPath(inner);
    }

    // =========================================================================
    // Nivel 2 — Resource ID / Predicate (contrato DEV↔QA)
    // =========================================================================

    /**
     * Localiza por Resource ID de Android ({@code com.app:id/element}) o ID genérico.
     *
     * <p>Más estable que XPath pero menos portable que Accessibility ID.
     */
    public static String byResourceId(String resourceId) {
        return "id:" + resourceId;
    }

    /**
     * Localiza por iOS NSPredicate String (solo iOS).
     *
     * <p>Ejemplo: {@code "label == 'Submit' AND enabled == true"}.
     */
    public static String byPredicate(String predicate) {
        return "pred:" + predicate;
    }

    // =========================================================================
    // Nivel 3 — Escape hatch (frágil; genera advertencia en el log)
    // =========================================================================

    /**
     * Localiza por XPath — escape hatch de último recurso.
     *
     * <p>Emite {@code [FRAGILE LOCATOR]} en el log. Preferir byA11y() o byResourceId().
     */
    public static String byXPath(String xpath) {
        LOG.warn("{} XPath '{}' — considerar migrar a byA11y() o byResourceId().",
            FRAGILE_MARKER, xpath);
        return "xpath:" + xpath;
    }

    /**
     * Localiza por Android UIAutomator — escape hatch para Android.
     *
     * <p>Emite {@code [FRAGILE LOCATOR]} en el log.
     */
    public static String byUiAutomator(String uiAutomatorExpr) {
        LOG.warn("{} UIAutomator '{}' — escape hatch solo Android.",
            FRAGILE_MARKER, uiAutomatorExpr);
        return "uia:" + uiAutomatorExpr;
    }

    /**
     * Localiza por iOS Class Chain — escape hatch para iOS.
     *
     * <p>Emite {@code [FRAGILE LOCATOR]} en el log.
     */
    public static String byClassChain(String classChain) {
        LOG.warn("{} Class chain '{}' — escape hatch solo iOS.",
            FRAGILE_MARKER, classChain);
        return "chain:" + classChain;
    }

    // =========================================================================
    // Privado — conversión interna a XPath puro (sin prefijo "xpath:")
    // =========================================================================

    /**
     * Convierte una expresión prefijada a un fragmento XPath puro (sin el prefijo "xpath:").
     * Usado internamente por nthOf() e inContainer().
     */
    static String toXPath(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("expression no puede ser nula o vacía");
        }
        if (expression.startsWith("~")) {
            String id = expression.substring(1);
            return "//*[@content-desc='" + id + "' or @name='" + id + "']";
        }
        if (expression.startsWith("text:")) {
            String text = expression.substring(5);
            return "//*[@text='" + text + "' or @label='" + text + "' or @value='" + text + "']";
        }
        if (expression.startsWith("id:")) {
            String id = expression.substring(3);
            return "//*[@resource-id='" + id + "']";
        }
        if (expression.startsWith("xpath:")) {
            return expression.substring(6);
        }
        // Sin prefijo → Accessibility ID por default (misma convención que ElementLocatorHelper)
        return "//*[@content-desc='" + expression + "' or @name='" + expression + "']";
    }
}
