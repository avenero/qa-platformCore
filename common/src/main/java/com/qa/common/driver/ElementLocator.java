package com.qa.common.driver;

import java.util.Objects;

/**
 * Contrato inmutable para identificar elementos de UI, agnóstico de la plataforma.
 *
 * <p>Unifica la forma en que web-core y mobile-core referencian elementos:
 * <ul>
 *   <li><b>Web</b> — {@link Strategy#CSS} y {@link Strategy#XPATH} son las más comunes</li>
 *   <li><b>Mobile</b> — {@link Strategy#ACCESSIBILITY_ID}, {@link Strategy#ID},
 *       {@link Strategy#CLASS_NAME} y {@link Strategy#TEXT}</li>
 * </ul>
 *
 * <p>Las implementaciones de {@link UiDriver} son responsables de traducir
 * el {@code ElementLocator} a los mecanismos nativos de cada plataforma
 * (Playwright {@code Locator}, Appium {@code By}, etc.).
 *
 * <h2>Ejemplos</h2>
 * <pre>
 * // Web
 * ElementLocator.css("#submit-btn")
 * ElementLocator.xpath("//button[@data-testid='login']")
 *
 * // Mobile
 * ElementLocator.accessibilityId("login_button")
 * ElementLocator.id("com.myapp:id/btn_login")
 * ElementLocator.text("Iniciar sesion")
 * </pre>
 *
 * @param strategy estrategia de localización, nunca null
 * @param value    expresión del locator (selector CSS, XPath, id, etc.), nunca null ni vacío
 *
 * @author Abel Venero
 * @since 3.0.0
 * @see UiDriver
 */
public record ElementLocator(Strategy strategy, String value) {

    /** Estrategias de localización de elementos. */
    public enum Strategy {
        /** Selector CSS — web (Playwright, Selenium). */
        CSS,
        /** Expresión XPath — web y mobile. */
        XPATH,
        /** Atributo HTML {@code id} — web. */
        ID,
        /** Resource-id de Android (ej: {@code "com.app:id/login"}) — mobile. */
        RESOURCE_ID,
        /** Accessibility ID / Content Description — mobile (Appium). */
        ACCESSIBILITY_ID,
        /** Nombre del elemento (atributo {@code name}) — web y mobile. */
        NAME,
        /** Nombre de clase del widget — mobile (Appium). */
        CLASS_NAME,
        /** Texto visible del elemento — web y mobile (fallback). */
        TEXT,
        /** Role ARIA o selector por rol de UI — web (Playwright). */
        ROLE
    }

    public ElementLocator {
        Objects.requireNonNull(strategy, "ElementLocator.strategy no puede ser null");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ElementLocator.value no puede ser null ni vacío");
        }
    }

    // =========================================================================
    // Factory methods
    // =========================================================================

    /** Crea un locator por selector CSS. */
    public static ElementLocator css(String selector) {
        return new ElementLocator(Strategy.CSS, selector);
    }

    /** Crea un locator por expresión XPath. */
    public static ElementLocator xpath(String expression) {
        return new ElementLocator(Strategy.XPATH, expression);
    }

    /** Crea un locator por atributo id (web) o resource-id (Android). */
    public static ElementLocator id(String id) {
        return new ElementLocator(Strategy.ID, id);
    }

    /** Crea un locator por accessibility ID (mobile). */
    public static ElementLocator accessibilityId(String id) {
        return new ElementLocator(Strategy.ACCESSIBILITY_ID, id);
    }

    /** Crea un locator por resource-id de Android (ej: {@code "com.app:id/login"}). */
    public static ElementLocator resourceId(String id) {
        return new ElementLocator(Strategy.RESOURCE_ID, id);
    }

    /** Crea un locator por accessibility ID — alias corto de {@link #accessibilityId(String)}. */
    public static ElementLocator a11y(String id) {
        return new ElementLocator(Strategy.ACCESSIBILITY_ID, id);
    }

    /** Crea un locator por nombre del elemento (atributo {@code name}). */
    public static ElementLocator name(String name) {
        return new ElementLocator(Strategy.NAME, name);
    }

    /** Crea un locator por nombre de clase del widget (mobile). */
    public static ElementLocator className(String name) {
        return new ElementLocator(Strategy.CLASS_NAME, name);
    }

    /** Crea un locator por texto visible del elemento. */
    public static ElementLocator text(String text) {
        return new ElementLocator(Strategy.TEXT, text);
    }

    /** Crea un locator por role ARIA o selector de rol de UI (web). */
    public static ElementLocator role(String role) {
        return new ElementLocator(Strategy.ROLE, role);
    }

    @Override
    public String toString() {
        return strategy.name() + "[" + value + "]";
    }
}
