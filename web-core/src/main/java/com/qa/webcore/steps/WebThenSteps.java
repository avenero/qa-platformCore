package com.qa.webcore.steps;

import com.qa.common.internal.runtime.ExecutionContext;
import com.qa.webcore.driver.engine.BrowserEngine;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Steps THEN del vocabulario Web semántico en inglés.
 *
 * <h2>Tres niveles de localización</h2>
 * <ul>
 *   <li><b>Nivel 1 — Semántico puro:</b> texto visible, roles ARIA, atributos de accesibilidad.</li>
 *   <li><b>Nivel 2 — data-testid:</b> contrato DEV↔QA.</li>
 *   <li><b>Nivel 3 — CSS/XPath (escape hatch):</b> solo legacy; genera log {@code [FRAGILE LOCATOR]}.</li>
 * </ul>
 *
 * @since 3.2.0
 */
public class WebThenSteps {

    private static final Logger LOG = LoggerFactory.getLogger(WebThenSteps.class);
    private static final int DEFAULT_WAIT_MS = 5_000;

    // =========================================================================
    // Nivel 1 — Visibilidad de texto
    // =========================================================================

    @Then("I should see {string}")
    public void iShouldSee(String text) {
        String selector = LocatorResolver.byText(text);
        engine().waitForVisible(selector, DEFAULT_WAIT_MS);
        Assertions.assertThat(engine().isPresent(selector))
                .as("Expected to see text '%s' on the page", text)
                .isTrue();
    }

    @Then("I should not see {string}")
    public void iShouldNotSee(String text) {
        String selector = LocatorResolver.byText(text);
        Assertions.assertThat(engine().isPresent(selector))
                .as("Expected NOT to see text '%s' on the page", text)
                .isFalse();
    }

    @Then("I should see {string} inside {string}")
    public void iShouldSeeInsideContainer(String text, String container) {
        String selector = LocatorResolver.inContainer(LocatorResolver.byText(text), container);
        engine().waitForVisible(selector, DEFAULT_WAIT_MS);
        Assertions.assertThat(engine().isPresent(selector))
                .as("Expected to see '%s' inside '%s'", text, container)
                .isTrue();
    }

    // =========================================================================
    // Nivel 1 — Estado de campos de formulario
    // =========================================================================

    @Then("the {string} field should contain {string}")
    public void theFieldShouldContain(String label, String expectedValue) {
        String selector = LocatorResolver.byLabel(label);
        String actual = engine().find(selector).getAttribute("value");
        Assertions.assertThat(actual)
                .as("Field '%s' value", label)
                .contains(expectedValue);
    }

    @Then("the {string} field should be empty")
    public void theFieldShouldBeEmpty(String label) {
        String selector = LocatorResolver.byLabel(label);
        String actual = engine().find(selector).getAttribute("value");
        Assertions.assertThat(actual)
                .as("Field '%s' should be empty", label)
                .isNullOrEmpty();
    }

    @Then("the {string} checkbox should be checked")
    public void theCheckboxShouldBeChecked(String label) {
        String selector = LocatorResolver.byRole("checkbox", label);
        boolean checked = Boolean.TRUE.equals(
            engine().find(selector).getAttribute("checked") != null);
        Assertions.assertThat(engine().isPresent(selector + "[checked]"))
                .as("Checkbox '%s' should be checked", label)
                .isTrue();
    }

    @Then("the {string} checkbox should not be checked")
    public void theCheckboxShouldNotBeChecked(String label) {
        String selector = LocatorResolver.byRole("checkbox", label);
        Assertions.assertThat(engine().isPresent(selector + "[checked]"))
                .as("Checkbox '%s' should NOT be checked", label)
                .isFalse();
    }

    // =========================================================================
    // Nivel 1 — Estado de botones y enlaces
    // =========================================================================

    @Then("the {string} button should be enabled")
    public void theButtonShouldBeEnabled(String text) {
        String selector = LocatorResolver.byRole("button", text);
        Assertions.assertThat(engine().find(selector).isEnabled())
                .as("Button '%s' should be enabled", text)
                .isTrue();
    }

    @Then("the {string} button should be disabled")
    public void theButtonShouldBeDisabled(String text) {
        String selector = LocatorResolver.byRole("button", text);
        Assertions.assertThat(engine().find(selector).isEnabled())
                .as("Button '%s' should be disabled", text)
                .isFalse();
    }

    @Then("the {string} link should be visible")
    public void theLinkShouldBeVisible(String text) {
        String selector = LocatorResolver.byRole("link", text);
        Assertions.assertThat(engine().isPresent(selector) && engine().find(selector).isVisible())
                .as("Link '%s' should be visible", text)
                .isTrue();
    }

    @Then("the {string} link should not be visible")
    public void theLinkShouldNotBeVisible(String text) {
        String selector = LocatorResolver.byRole("link", text);
        boolean notVisible = !engine().isPresent(selector)
                || !engine().find(selector).isVisible();
        Assertions.assertThat(notVisible)
                .as("Link '%s' should NOT be visible", text)
                .isTrue();
    }

    // =========================================================================
    // Nivel 1 — Página y URL
    // =========================================================================

    @Then("the page title should be {string}")
    public void thePageTitleShouldBe(String expectedTitle) {
        Assertions.assertThat(engine().getTitle())
                .as("Page title")
                .isEqualTo(expectedTitle);
    }

    @Then("the page title should contain {string}")
    public void thePageTitleShouldContain(String text) {
        Assertions.assertThat(engine().getTitle())
                .as("Page title")
                .contains(text);
    }

    @Then("the url should be {string}")
    public void theUrlShouldBe(String expectedUrl) {
        Assertions.assertThat(engine().getCurrentUrl())
                .as("Current URL")
                .isEqualTo(expectedUrl);
    }

    @Then("the url should contain {string}")
    public void theUrlShouldContain(String fragment) {
        Assertions.assertThat(engine().getCurrentUrl())
                .as("Current URL")
                .contains(fragment);
    }

    // =========================================================================
    // Nivel 1 — Conteo y notificaciones
    // =========================================================================

    @Then("I should see {int} items matching {string}")
    public void iShouldSeeItemsMatching(int expectedCount, String text) {
        int actual = engine().findAll(LocatorResolver.byText(text)).size();
        Assertions.assertThat(actual)
                .as("Expected %d items matching '%s' but found %d", expectedCount, text, actual)
                .isEqualTo(expectedCount);
    }

    @Then("a toast notification with text {string} should appear")
    public void aToastNotificationShouldAppear(String text) {
        // Toast notifications son ephemeral; usamos waitForVisible con timeout corto
        String selector = LocatorResolver.byText(text);
        engine().waitForVisible(selector, DEFAULT_WAIT_MS);
        Assertions.assertThat(engine().isPresent(selector))
                .as("Toast notification with text '%s'", text)
                .isTrue();
    }

    @Then("a dialog with text {string} should be visible")
    public void aDialogWithTextShouldBeVisible(String text) {
        String selector = LocatorResolver.inContainer(
            LocatorResolver.byText(text), "[role=\"dialog\"]");
        engine().waitForVisible(selector, DEFAULT_WAIT_MS);
        Assertions.assertThat(engine().isPresent(selector))
                .as("Dialog with text '%s'", text)
                .isTrue();
    }

    // =========================================================================
    // Nivel 2 — data-testid
    // =========================================================================

    @Then("the element with testId {string} should be visible")
    public void theElementWithTestIdShouldBeVisible(String testId) {
        String selector = LocatorResolver.byTestId(testId);
        engine().waitForVisible(selector, DEFAULT_WAIT_MS);
        Assertions.assertThat(engine().isPresent(selector) && engine().find(selector).isVisible())
                .as("Element with testId '%s' should be visible", testId)
                .isTrue();
    }

    @Then("the element with testId {string} should not be visible")
    public void theElementWithTestIdShouldNotBeVisible(String testId) {
        String selector = LocatorResolver.byTestId(testId);
        boolean notVisible = !engine().isPresent(selector)
                || !engine().find(selector).isVisible();
        Assertions.assertThat(notVisible)
                .as("Element with testId '%s' should NOT be visible", testId)
                .isTrue();
    }

    @Then("the element with testId {string} should contain {string}")
    public void theElementWithTestIdShouldContain(String testId, String text) {
        String selector = LocatorResolver.byTestId(testId);
        String actual = engine().find(selector).getText();
        Assertions.assertThat(actual)
                .as("Element with testId '%s'", testId)
                .contains(text);
    }

    @Then("the element with testId {string} should have the value {string}")
    public void theElementWithTestIdShouldHaveValue(String testId, String expectedValue) {
        String selector = LocatorResolver.byTestId(testId);
        String actual = engine().find(selector).getAttribute("value");
        Assertions.assertThat(actual)
                .as("Element with testId '%s' value", testId)
                .isEqualTo(expectedValue);
    }

    // =========================================================================
    // Nivel 3 — CSS/XPath escape hatch
    // =========================================================================

    @Then("the element with css {string} should be visible")
    public void theElementWithCssShouldBeVisible(String selector) {
        String resolved = LocatorResolver.byCss(selector);
        Assertions.assertThat(engine().isPresent(resolved) && engine().find(resolved).isVisible())
                .as("CSS element '%s' should be visible", selector)
                .isTrue();
    }

    @Then("the element with css {string} should contain {string}")
    public void theElementWithCssShouldContain(String selector, String text) {
        String resolved = LocatorResolver.byCss(selector);
        Assertions.assertThat(engine().find(resolved).getText())
                .as("CSS element '%s'", selector)
                .contains(text);
    }

    @Then("the element with xpath {string} should be visible")
    public void theElementWithXPathShouldBeVisible(String xpath) {
        String resolved = LocatorResolver.byXPath(xpath);
        Assertions.assertThat(engine().isPresent(resolved) && engine().find(resolved).isVisible())
                .as("XPath element '%s' should be visible", xpath)
                .isTrue();
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
