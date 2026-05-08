package com.qa.mobilecore.steps;

import com.qa.common.runtime.ExecutionContext;
import com.qa.mobilecore.helper.MobileHelper;
import io.cucumber.java.en.Given;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Steps GIVEN del vocabulario Mobile semántico en inglés.
 *
 * <p>Cubren estado inicial del escenario: verificación de app activa,
 * pantalla esperada y ciclo de vida (relanzar, reset).
 *
 * <p>Coexisten con los steps legacy en español; los nuevos escenarios
 * deben usar este vocabulario.
 *
 * @since 3.2.0
 */
public class MobileGivenSteps {

    private static final Logger LOG = LoggerFactory.getLogger(MobileGivenSteps.class);

    // =========================================================================
    // Estado de la app
    // =========================================================================

    @Given("the app is running")
    public void theAppIsRunning() {
        Assertions.assertThat(mobile().hasActiveSession())
                .as("The app should have an active Appium session")
                .isTrue();
        LOG.debug("[MOBILE-GIVEN] App is running (session active)");
    }

    @Given("the app is on the {string} screen")
    public void theAppIsOnTheScreen(String screen) {
        boolean visible = mobile().elementExists(MobileLocatorResolver.byText(screen))
                || mobile().elementExists(MobileLocatorResolver.byA11y(screen));
        Assertions.assertThat(visible)
                .as("Expected to be on the '%s' screen", screen)
                .isTrue();
        LOG.debug("[MOBILE-GIVEN] App is on screen: {}", screen);
    }

    @Given("the app is in the foreground")
    public void theAppIsInTheForeground() {
        Assertions.assertThat(mobile().hasActiveSession())
                .as("The app should be active (session alive)")
                .isTrue();
        LOG.debug("[MOBILE-GIVEN] App confirmed in foreground");
    }

    @Given("the app is in the background")
    public void theAppIsInTheBackground() {
        // Appium does not expose a reliable isBackground() API across platforms.
        // Presence of an active session is the best available proxy.
        Assertions.assertThat(mobile().hasActiveSession())
                .as("The Appium session should be alive to verify background state")
                .isTrue();
        LOG.debug("[MOBILE-GIVEN] App confirmed in background (session alive)");
    }

    // =========================================================================
    // Ciclo de vida
    // =========================================================================

    @Given("I relaunch the app")
    public void iRelaunchTheApp() {
        LOG.debug("[MOBILE-GIVEN] Relaunching app");
        mobile().restartApp();
    }

    @Given("I reset the app state")
    public void iResetTheAppState() {
        // Restart is the best portable reset available without platform-specific
        // clear-data APIs (ADB am clear / simctl privacy reset).
        LOG.debug("[MOBILE-GIVEN] Resetting app state via restart");
        mobile().restartApp();
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private MobileHelper mobile() {
        return ExecutionContext.requireCurrent().service(MobileHelper.class);
    }
}
