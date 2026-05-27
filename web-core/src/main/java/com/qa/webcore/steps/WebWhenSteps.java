package com.qa.webcore.steps;

import com.qa.common.api.runtime.ExecutionContext;
import com.qa.webcore.driver.engine.BrowserEngine;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Steps WHEN del vocabulario Web semántico en inglés.
 *
 * <h2>Tres niveles de localización</h2>
 * <ul>
 *   <li><b>Nivel 1 — Semántico puro:</b> por texto visible, rol ARIA, aria-label.</li>
 *   <li><b>Nivel 1+ — Semántico con scope/índice:</b> por contenedor u ordinal.</li>
 *   <li><b>Nivel 2 — data-testid:</b> contrato DEV↔QA.</li>
 *   <li><b>Nivel 3 — CSS/XPath (escape hatch):</b> solo para legacy; genera log {@code [FRAGILE LOCATOR]}.</li>
 * </ul>
 *
 * @since 3.2.0
 */
public class WebWhenSteps {

    private static final Logger LOG = LoggerFactory.getLogger(WebWhenSteps.class);

    // =========================================================================
    // Nivel 1 — Clicks semánticos
    // =========================================================================

    @When("I click {string}")
    public void iClick(String text) {
        engine().click(LocatorResolver.byText(text));
    }

    @When("I click the {string} {string}")
    public void iClickTheRoleText(String role, String name) {
        engine().click(LocatorResolver.byRole(role, name));
    }

    @When("I double-click {string}")
    public void iDoubleClick(String text) {
        engine().doubleClick(LocatorResolver.byText(text));
    }

    @When("I right-click {string}")
    public void iRightClick(String text) {
        // PlaywrightBrowserEngine no expone rightClick directamente via string API;
        // se delega via evaluate con contextmenu event.
        engine().evaluate("""
            const el = document.evaluate(arguments[0], document, null,
                XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue
                || document.querySelector(arguments[0]);
            if (el) el.dispatchEvent(new MouseEvent('contextmenu', {bubbles: true}));
            """, LocatorResolver.byText(text));
    }

    // =========================================================================
    // Nivel 1 — Formularios semánticos
    // =========================================================================

    @When("I fill {string} with {string}")
    public void iFillWith(String label, String value) {
        engine().type(LocatorResolver.byLabel(label), value);
    }

    @When("I clear {string}")
    public void iClear(String label) {
        engine().clear(LocatorResolver.byLabel(label));
    }

    @When("I type {string} into {string}")
    public void iTypeInto(String value, String label) {
        engine().type(LocatorResolver.byLabel(label), value);
    }

    @When("I select {string} from the {string} dropdown")
    public void iSelectFromTheDropdown(String option, String label) {
        engine().selectOption(LocatorResolver.byLabel(label), option);
    }

    @When("I check the {string} checkbox")
    public void iCheckTheCheckbox(String label) {
        String selector = LocatorResolver.byRole("checkbox", label);
        engine().evaluate("""
            const el = document.querySelector(arguments[0]);
            if (el && !el.checked) el.click();
            """, selector);
    }

    @When("I uncheck the {string} checkbox")
    public void iUncheckTheCheckbox(String label) {
        String selector = LocatorResolver.byRole("checkbox", label);
        engine().evaluate("""
            const el = document.querySelector(arguments[0]);
            if (el && el.checked) el.click();
            """, selector);
    }

    @When("I upload the file {string} to {string}")
    public void iUploadTheFileTo(String filename, String label) {
        LOG.info("[WEB-WHEN] File upload '{}' to field '{}' — requires native file chooser interception.",
                filename, label);
        // La subida de archivos en Playwright requiere fileChooser API a nivel de Page.
        // Esta operación requiere acceso directo a la Page vía PlaywrightManager.getPage()
        // que está fuera del contrato de BrowserEngine.
        // Implementación completa disponible cuando BrowserEngine exponga uploadFile().
        throw new UnsupportedOperationException(
            "File upload requires PlaywrightManager.getPage().setInputFiles() — "
            + "pending BrowserEngine.uploadFile() API (RFC-WEBAPI-01)");
    }

    // =========================================================================
    // Nivel 1 — Navegación y foco
    // =========================================================================

    @When("I hover over {string}")
    public void iHoverOver(String text) {
        engine().hover(LocatorResolver.byText(text));
    }

    @When("I press the {string} key")
    public void iPressTheKey(String key) {
        engine().evaluate("""
            document.dispatchEvent(new KeyboardEvent('keydown', {key: arguments[0], bubbles: true}));
            document.dispatchEvent(new KeyboardEvent('keyup',   {key: arguments[0], bubbles: true}));
            """, key);
    }

    @When("I press the {string}+{string} keys")
    public void iPressTheModifierKey(String modifier, String key) {
        engine().evaluate("""
            document.dispatchEvent(new KeyboardEvent('keydown',
                {key: arguments[1], ctrlKey: arguments[0]==='Ctrl', shiftKey: arguments[0]==='Shift',
                 altKey: arguments[0]==='Alt', metaKey: arguments[0]==='Meta', bubbles: true}));
            """, modifier, key);
    }

    @When("I scroll to {string}")
    public void iScrollTo(String text) {
        engine().scrollIntoView(LocatorResolver.byText(text));
    }

    @When("I scroll down")
    public void iScrollDown() {
        engine().evaluate("window.scrollBy(0, window.innerHeight)");
    }

    @When("I scroll up")
    public void iScrollUp() {
        engine().evaluate("window.scrollBy(0, -window.innerHeight)");
    }

    @When("I scroll down by {int} pixels")
    public void iScrollDownByPixels(int pixels) {
        engine().evaluate("window.scrollBy(0, arguments[0])", pixels);
    }

    @When("I submit the form")
    public void iSubmitTheForm() {
        engine().evaluate("""
            const form = document.querySelector('form');
            if (form) form.submit();
            """);
    }

    // =========================================================================
    // Nivel 1+ — Semántico con scope / índice
    // =========================================================================

    @When("I click {string} inside {string}")
    public void iClickInsideContainer(String text, String container) {
        engine().click(LocatorResolver.inContainer(LocatorResolver.byText(text), container));
    }

    @When("I click the {int}. {string}")
    public void iClickTheNth(int n, String text) {
        engine().click(LocatorResolver.nthOf(LocatorResolver.byText(text), n));
    }

    @When("I fill the {int}. {string} with {string}")
    public void iFillTheNthWith(int n, String label, String value) {
        engine().type(LocatorResolver.nthOf(LocatorResolver.byLabel(label), n), value);
    }

    @When("I click {string} in row {int}")
    public void iClickInRow(String text, int row) {
        String rowSelector = "tr >> nth=" + (row - 1);
        engine().click(LocatorResolver.inContainer(LocatorResolver.byText(text), rowSelector));
    }

    // =========================================================================
    // Nivel 2 — data-testid
    // =========================================================================

    @When("I click the element with testId {string}")
    public void iClickElementWithTestId(String testId) {
        engine().click(LocatorResolver.byTestId(testId));
    }

    @When("I fill the element with testId {string} with {string}")
    public void iFillElementWithTestIdWith(String testId, String value) {
        engine().type(LocatorResolver.byTestId(testId), value);
    }

    @When("I click the element with testId {string} inside {string}")
    public void iClickElementWithTestIdInsideContainer(String testId, String container) {
        engine().click(LocatorResolver.byTestIdInContainer(testId, container));
    }

    // =========================================================================
    // Nivel 3 — Escape hatch CSS/XPath
    // =========================================================================

    @When("I click the element with css {string}")
    public void iClickElementWithCss(String selector) {
        engine().click(LocatorResolver.byCss(selector));
    }

    @When("I click the element with xpath {string}")
    public void iClickElementWithXPath(String xpath) {
        engine().click(LocatorResolver.byXPath(xpath));
    }

    @When("I fill the element with css {string} with {string}")
    public void iFillElementWithCssWith(String selector, String value) {
        engine().type(LocatorResolver.byCss(selector), value);
    }

    @When("I fill the element with xpath {string} with {string}")
    public void iFillElementWithXPathWith(String xpath, String value) {
        engine().type(LocatorResolver.byXPath(xpath), value);
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
