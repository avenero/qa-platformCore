package com.qa.webcore.steps.wait;

import com.qa.common.api.logging.TestLogger;
import com.qa.common.internal.runtime.ExecutionContext;
import com.qa.common.api.runtime.annotation.StepDef;
import com.qa.webcore.driver.engine.BrowserEngine;
import com.qa.webcore.utils.WebHelper;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

import java.time.Duration;

/**
 * Steps de espera explicita e implicita.
 * Migrado de WebSteps.java.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class WaitSteps {

    /** Timeout largo para espera de visibilidad y habilitacion de elemento. */
    private static final int WAIT_STEPS_TIMEOUT = 60;

    /** Timeout para carga de pagina en steps de espera. */
    private static final int PAGE_LOAD_TIMEOUT = 90;

    /** Timeout para esperar visibilidad de texto en la pagina. */
    private static final int TEXT_VISIBLE_TIMEOUT = 15;

    /** Multiplicador de milisegundos por segundo. */
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long ENABLED_POLL_INTERVAL_MS = 100L;

    private static final int WAIT_TIMEOUT_MS = (int)(WAIT_STEPS_TIMEOUT * MILLIS_PER_SECOND);
    private static final int PAGE_LOAD_TIMEOUT_MS = (int)(PAGE_LOAD_TIMEOUT * MILLIS_PER_SECOND);
    private static final int TEXT_VISIBLE_TIMEOUT_MS = (int)(TEXT_VISIBLE_TIMEOUT * MILLIS_PER_SECOND);

    private final WebHelper helper = new WebHelper();
    private Scenario scenario;

    @When("espero hasta que elemento {string} este visible")
    public void esperarHastaQueElementoEsteVisible(String locator) {
        engine().waitForVisible(locator, WAIT_TIMEOUT_MS);
    }

    @When("espero hasta que elemento {string} este habilitado")
    public void esperarHastaQueElementoEsteHabilitado(String locator) {
        waitUntilEnabled(locator, WAIT_STEPS_TIMEOUT);
    }

    @When("espero hasta que elemento {string} no este visible")
    public void esperarHastaQueElementoNoEsteVisible(String locator) {
        engine().waitForHidden(locator, WAIT_TIMEOUT_MS);
        helper.captureScreen(scenario);
    }

    @When("espero {string} segundos")
    public void esperarUnTiempo(String segundos) {
        sleepQuietly(Integer.parseInt(segundos) * MILLIS_PER_SECOND);
        helper.captureScreen(scenario);
        TestLogger.logWarning("WAIT_STEPS", "Wait fijo: " + segundos + "s", null);
    }

    @When("espero hasta que la seccion termine de cargar")
    public void esperoHastaQueLaSeccionTermineDeCargar() {
        helper.captureScreen(scenario);
        engine().waitForLoadState("load", PAGE_LOAD_TIMEOUT_MS);
    }

    @StepDef(value = "web.wait.legacy-esperar-home",
             deprecated = true,
             displayName = "esperar que se muestre home (DEPRECATED — step de negocio)")
    @And("esperar que se muestre home")
    public void esperarQueSeMuestreHome() {
        helper.waitForHome();
        helper.captureScreen(scenario);
    }

    // =========================================================================
    // NUEVOS STEPS GENÉRICOS — Waits v2.2.1
    // =========================================================================

    /**
     * Espera hasta que el elemento desaparezca (alias más claro).
     */
    @When("espero hasta que el elemento {string} desaparezca")
    public void esperarHastaQueElElementoDesaparezca(String locator) {
        engine().waitForHidden(locator, WAIT_TIMEOUT_MS);
    }

    /**
     * Espera hasta que un texto sea visible en la página.
     */
    @When("espero hasta que el texto {string} sea visible en la página")
    public void esperarHastaQueElTextoSeaVisible(String text) {
        engine().waitForText("body", text, TEXT_VISIBLE_TIMEOUT_MS);
    }

    /**
     * Espera un número específico de segundos (int, más natural que string).
     */
    @When("espero {int} segundos")
    public void esperarSegundosInt(int segundos) {
        sleepQuietly(segundos * MILLIS_PER_SECOND);
        TestLogger.logWarning("WAIT_STEPS", "Wait fijo: " + segundos + "s", null);
    }

    private void waitUntilEnabled(String locator, int timeoutSeconds) {
        long timeoutMillis = timeoutSeconds * MILLIS_PER_SECOND;
        long deadlineNanos = System.nanoTime() + Duration.ofMillis(timeoutMillis).toNanos();
        while (System.nanoTime() <= deadlineNanos) {
            try {
                if (engine().find(locator).isEnabled()) {
                    return;
                }
            } catch (RuntimeException ignored) {
                // El elemento puede no estar listo aún; seguimos esperando.
            }
            sleepQuietly(ENABLED_POLL_INTERVAL_MS);
        }
        throw new IllegalStateException(
                "Timeout esperando que el elemento '" + locator + "' esté habilitado");
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupción durante espera explícita", e);
        }
    }

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }
}
