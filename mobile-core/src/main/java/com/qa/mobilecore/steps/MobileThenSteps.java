package com.qa.mobilecore.steps;

import com.qa.common.internal.runtime.ExecutionContext;
import com.qa.mobilecore.helper.MobileHelper;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Steps THEN del vocabulario Mobile semántico en inglés.
 *
 * <h2>Tres niveles de localización</h2>
 * <ul>
 *   <li><b>Nivel 1 — Semántico puro:</b> texto visible, Accessibility ID, estado de controles.</li>
 *   <li><b>Nivel 2 — Resource ID / Accessibility ID explícito:</b> contrato DEV↔QA.</li>
 *   <li><b>Nivel 3 — XPath (escape hatch):</b> solo legacy; genera log {@code [FRAGILE LOCATOR]}.</li>
 * </ul>
 *
 * @since 3.2.0
 */
public class MobileThenSteps {

    private static final Logger LOG = LoggerFactory.getLogger(MobileThenSteps.class);
    private static final int DEFAULT_WAIT_TIMEOUT_SEC = 5;

    // =========================================================================
    // Nivel 1 — Visibilidad de texto / elementos
    // =========================================================================

    @Then("I should see {string} on the screen")
    public void iShouldSeeOnTheScreen(String text) {
        String selector = MobileLocatorResolver.byText(text);
        mobile().waitForText(text);
        Assertions.assertThat(mobile().elementExists(selector))
                .as("Expected to see '%s' on the screen", text)
                .isTrue();
    }

    @Then("I should not see {string} on the screen")
    public void iShouldNotSeeOnTheScreen(String text) {
        String selector = MobileLocatorResolver.byText(text);
        Assertions.assertThat(mobile().elementExists(selector))
                .as("Expected NOT to see '%s' on the screen", text)
                .isFalse();
    }

    @Then("I should see {string} inside {string}")
    public void iShouldSeeInsideContainer(String text, String container) {
        String selector = MobileLocatorResolver.inContainer(
            MobileLocatorResolver.byText(text),
            MobileLocatorResolver.byA11y(container));
        Assertions.assertThat(mobile().elementExists(selector))
                .as("Expected to see '%s' inside '%s'", text, container)
                .isTrue();
    }

    // =========================================================================
    // Nivel 1 — Estado de campos de formulario
    // =========================================================================

    @Then("the {string} field should contain {string}")
    public void theFieldShouldContain(String label, String expectedValue) {
        String selector = MobileLocatorResolver.byA11y(label);
        String actual = mobile().getText(selector);
        Assertions.assertThat(actual)
                .as("Field '%s' value", label)
                .contains(expectedValue);
    }

    @Then("the {string} field should be empty")
    public void theFieldShouldBeEmpty(String label) {
        String selector = MobileLocatorResolver.byA11y(label);
        String actual = mobile().getText(selector);
        Assertions.assertThat(actual)
                .as("Field '%s' should be empty", label)
                .isNullOrEmpty();
    }

    // =========================================================================
    // Nivel 1 — Estado de botones
    // =========================================================================

    @Then("the {string} button should be enabled")
    public void theButtonShouldBeEnabled(String label) {
        String selector = MobileLocatorResolver.byA11y(label);
        Assertions.assertThat(mobile().isEnabled(selector))
                .as("Button '%s' should be enabled", label)
                .isTrue();
    }

    @Then("the {string} button should be disabled")
    public void theButtonShouldBeDisabled(String label) {
        String selector = MobileLocatorResolver.byA11y(label);
        Assertions.assertThat(mobile().isEnabled(selector))
                .as("Button '%s' should be disabled", label)
                .isFalse();
    }

    // =========================================================================
    // Nivel 1 — Notificaciones y alertas
    // =========================================================================

    @Then("a toast {string} should appear")
    public void aToastShouldAppear(String text) {
        mobile().waitForText(text);
        Assertions.assertThat(mobile().elementExists(MobileLocatorResolver.byText(text)))
                .as("Toast with text '%s' should appear", text)
                .isTrue();
    }

    @Then("an alert with text {string} should be visible")
    public void anAlertWithTextShouldBeVisible(String text) {
        mobile().waitForText(text);
        Assertions.assertThat(mobile().elementExists(MobileLocatorResolver.byText(text)))
                .as("Alert with text '%s' should be visible", text)
                .isTrue();
    }

    // =========================================================================
    // Nivel 1 — Pantalla actual
    // =========================================================================

    @Then("the screen should show the {string} view")
    public void theScreenShouldShowTheView(String screen) {
        boolean visible = mobile().elementExists(MobileLocatorResolver.byA11y(screen))
                || mobile().elementExists(MobileLocatorResolver.byText(screen));
        Assertions.assertThat(visible)
                .as("Expected the screen to show the '%s' view", screen)
                .isTrue();
    }

    // =========================================================================
    // Nivel 2 — Accessibility ID
    // =========================================================================

    @Then("the element with accessibility id {string} should be visible")
    public void theElementWithAccessibilityIdShouldBeVisible(String id) {
        String selector = MobileLocatorResolver.byA11y(id);
        mobile().waitForVisible(selector);
        Assertions.assertThat(mobile().isVisible(selector))
                .as("Element with accessibility id '%s' should be visible", id)
                .isTrue();
    }

    @Then("the element with accessibility id {string} should not be visible")
    public void theElementWithAccessibilityIdShouldNotBeVisible(String id) {
        String selector = MobileLocatorResolver.byA11y(id);
        Assertions.assertThat(mobile().isVisible(selector))
                .as("Element with accessibility id '%s' should NOT be visible", id)
                .isFalse();
    }

    @Then("the element with accessibility id {string} should contain {string}")
    public void theElementWithAccessibilityIdShouldContain(String id, String text) {
        String selector = MobileLocatorResolver.byA11y(id);
        String actual = mobile().getText(selector);
        Assertions.assertThat(actual)
                .as("Element with accessibility id '%s'", id)
                .contains(text);
    }

    // =========================================================================
    // Nivel 2 — Resource ID
    // =========================================================================

    @Then("the element with resource id {string} should be visible")
    public void theElementWithResourceIdShouldBeVisible(String resourceId) {
        String selector = MobileLocatorResolver.byResourceId(resourceId);
        mobile().waitForVisible(selector);
        Assertions.assertThat(mobile().isVisible(selector))
                .as("Element with resource id '%s' should be visible", resourceId)
                .isTrue();
    }

    @Then("the element with resource id {string} should contain {string}")
    public void theElementWithResourceIdShouldContain(String resourceId, String text) {
        String selector = MobileLocatorResolver.byResourceId(resourceId);
        String actual = mobile().getText(selector);
        Assertions.assertThat(actual)
                .as("Element with resource id '%s'", resourceId)
                .contains(text);
    }

    // =========================================================================
    // Nivel 3 — Escape hatch XPath
    // =========================================================================

    @Then("the element with xpath {string} should be visible")
    public void theElementWithXPathShouldBeVisible(String xpath) {
        String selector = MobileLocatorResolver.byXPath(xpath);
        Assertions.assertThat(mobile().isVisible(selector))
                .as("XPath element '%s' should be visible", xpath)
                .isTrue();
    }

    @Then("the element with xpath {string} should contain {string}")
    public void theElementWithXPathShouldContain(String xpath, String text) {
        String selector = MobileLocatorResolver.byXPath(xpath);
        String actual = mobile().getText(selector);
        Assertions.assertThat(actual)
                .as("XPath element '%s'", xpath)
                .contains(text);
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private MobileHelper mobile() {
        return ExecutionContext.requireCurrent().service(MobileHelper.class);
    }
}
