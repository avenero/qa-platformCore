package com.qa.mobilecore.steps;

import com.qa.common.api.runtime.ExecutionContext;
import com.qa.mobilecore.helper.MobileHelper;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Steps WHEN del vocabulario Mobile semántico en inglés.
 *
 * <h2>Tres niveles de localización</h2>
 * <ul>
 *   <li><b>Nivel 1 — Semántico puro:</b> Accessibility ID / texto visible.</li>
 *   <li><b>Nivel 1+ — Semántico con scope/índice:</b> contenedor u ordinal (genera XPath).</li>
 *   <li><b>Nivel 2 — Resource ID / Accessibility ID explícito:</b> contrato DEV↔QA.</li>
 *   <li><b>Nivel 3 — XPath (escape hatch):</b> solo legacy; genera log {@code [FRAGILE LOCATOR]}.</li>
 * </ul>
 *
 * @since 3.2.0
 */
public class MobileWhenSteps {

    private static final Logger LOG = LoggerFactory.getLogger(MobileWhenSteps.class);

    /** Default long-press duration (ms). */
    private static final long LONG_PRESS_DEFAULT_MS = 800L;

    // =========================================================================
    // Nivel 1 — Taps y gestos básicos (Accessibility ID)
    // =========================================================================

    @When("I tap {string}")
    public void iTap(String label) {
        mobile().tap(MobileLocatorResolver.byA11y(label));
    }

    @When("I double-tap {string}")
    public void iDoubleTap(String label) {
        mobile().doubleTap(MobileLocatorResolver.byA11y(label));
    }

    @When("I long press {string}")
    public void iLongPress(String label) {
        mobile().longPress(MobileLocatorResolver.byA11y(label), LONG_PRESS_DEFAULT_MS);
    }

    // =========================================================================
    // Nivel 1 — Formularios semánticos
    // =========================================================================

    @When("I fill {string} with {string}")
    public void iFillWith(String label, String value) {
        mobile().type(MobileLocatorResolver.byA11y(label), value);
    }

    @When("I clear {string}")
    public void iClear(String label) {
        mobile().clearField(MobileLocatorResolver.byA11y(label));
    }

    @When("I select {string} from the {string} picker")
    public void iSelectFromThePicker(String option, String label) {
        mobile().tap(MobileLocatorResolver.byA11y(label));
        mobile().tap(MobileLocatorResolver.byText(option));
    }

    // =========================================================================
    // Nivel 1 — Scroll y swipe
    // =========================================================================

    /**
     * "Scroll down" in UX terms: the user sees content lower in the page.
     * On device: finger moves up (swipe "up" = content moves down).
     */
    @When("I scroll down")
    public void iScrollDown() {
        mobile().swipe("up");
    }

    /**
     * "Scroll up" in UX terms: the user sees content higher in the page.
     * On device: finger moves down (swipe "down" = content moves up).
     */
    @When("I scroll up")
    public void iScrollUp() {
        mobile().swipe("down");
    }

    @When("I scroll left")
    public void iScrollLeft() {
        mobile().swipe("left");
    }

    @When("I scroll right")
    public void iScrollRight() {
        mobile().swipe("right");
    }

    @When("I scroll down until I see {string}")
    public void iScrollDownUntilISee(String text) {
        mobile().scrollToText(text);
    }

    @When("I scroll up until I see {string}")
    public void iScrollUpUntilISee(String text) {
        mobile().scrollUpToText(text);
    }

    /**
     * Swipes left on the element's general screen area.
     *
     * <p>Typical use: reveal a delete/archive action on a list row.
     * Note: this performs a full-screen swipe; element-scoped swipe is not
     * available in the current BrowserEngine API contract.
     */
    @When("I swipe left on {string}")
    public void iSwipeLeftOn(String label) {
        LOG.debug("[MOBILE-WHEN] Swipe left (full screen) for element: {}", label);
        mobile().swipe("left");
    }

    @When("I swipe right on {string}")
    public void iSwipeRightOn(String label) {
        LOG.debug("[MOBILE-WHEN] Swipe right (full screen) for element: {}", label);
        mobile().swipe("right");
    }

    // =========================================================================
    // Nivel 1 — Gestos táctiles avanzados
    // =========================================================================

    @When("I pinch to zoom in on {string}")
    public void iPinchToZoomInOn(String label) {
        mobile().zoom(MobileLocatorResolver.byA11y(label));
    }

    @When("I pinch to zoom out on {string}")
    public void iPinchToZoomOutOn(String label) {
        mobile().pinch(MobileLocatorResolver.byA11y(label));
    }

    // =========================================================================
    // Nivel 1 — Teclado y botones de sistema
    // =========================================================================

    @When("I press the back button")
    public void iPressTheBackButton() {
        mobile().pressSystemKey("BACK");
    }

    @When("I press the home button")
    public void iPressTheHomeButton() {
        mobile().pressSystemKey("HOME");
    }

    @When("I hide the keyboard")
    public void iHideTheKeyboard() {
        mobile().hideKeyboard();
    }

    // =========================================================================
    // Nivel 1+ — Semántico con scope / índice
    // =========================================================================

    @When("I tap {string} inside {string}")
    public void iTapInsideContainer(String label, String container) {
        mobile().tap(MobileLocatorResolver.inContainer(
            MobileLocatorResolver.byA11y(label),
            MobileLocatorResolver.byA11y(container)));
    }

    @When("I tap the {int}. {string}")
    public void iTapTheNth(int n, String label) {
        mobile().tap(MobileLocatorResolver.nthOf(MobileLocatorResolver.byA11y(label), n));
    }

    @When("I fill the {int}. {string} with {string}")
    public void iFillTheNthWith(int n, String label, String value) {
        mobile().type(MobileLocatorResolver.nthOf(MobileLocatorResolver.byA11y(label), n), value);
    }

    // =========================================================================
    // Nivel 2 — Accessibility ID explícito
    // =========================================================================

    @When("I tap the element with accessibility id {string}")
    public void iTapElementWithAccessibilityId(String id) {
        mobile().tap(MobileLocatorResolver.byA11y(id));
    }

    @When("I fill the element with accessibility id {string} with {string}")
    public void iFillElementWithAccessibilityIdWith(String id, String value) {
        mobile().type(MobileLocatorResolver.byA11y(id), value);
    }

    // =========================================================================
    // Nivel 2 — Resource ID (Android)
    // =========================================================================

    @When("I tap the element with resource id {string}")
    public void iTapElementWithResourceId(String resourceId) {
        mobile().tap(MobileLocatorResolver.byResourceId(resourceId));
    }

    @When("I fill the element with resource id {string} with {string}")
    public void iFillElementWithResourceIdWith(String resourceId, String value) {
        mobile().type(MobileLocatorResolver.byResourceId(resourceId), value);
    }

    // =========================================================================
    // Nivel 3 — Escape hatch XPath
    // =========================================================================

    @When("I tap the element with xpath {string}")
    public void iTapElementWithXPath(String xpath) {
        mobile().tap(MobileLocatorResolver.byXPath(xpath));
    }

    @When("I fill the element with xpath {string} with {string}")
    public void iFillElementWithXPathWith(String xpath, String value) {
        mobile().type(MobileLocatorResolver.byXPath(xpath), value);
    }

    // =========================================================================
    // Permisos del sistema
    // =========================================================================

    @When("I grant the {string} permission")
    public void iGrantThePermission(String permission) {
        mobile().grantPermission(permission);
    }

    @When("I deny the {string} permission")
    public void iDenyThePermission(String permission) {
        mobile().denyPermission(permission);
    }

    @When("I accept the system alert")
    public void iAcceptTheSystemAlert() {
        mobile().acceptSystemDialog();
    }

    @When("I dismiss the system alert")
    public void iDismissTheSystemAlert() {
        mobile().dismissSystemDialog();
    }

    // =========================================================================
    // Contexto WebView (apps híbridas)
    // =========================================================================

    @When("I switch to the web view")
    public void iSwitchToTheWebView() {
        mobile().switchToFirstWebView();
    }

    @When("I switch to the native view")
    public void iSwitchToTheNativeView() {
        mobile().switchToNative();
    }

    @When("I switch to the web view {string}")
    public void iSwitchToTheWebView(String name) {
        mobile().switchToWebView(name);
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private MobileHelper mobile() {
        return ExecutionContext.requireCurrent().service(MobileHelper.class);
    }
}
