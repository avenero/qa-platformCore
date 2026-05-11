package com.qa.webcore.catalog;

import com.qa.common.api.stepcatalog.ParamSpec;
import com.qa.common.api.stepcatalog.StepCatalogEntry;
import com.qa.common.api.stepcatalog.StepCatalogProvider;
import com.qa.common.api.stepcatalog.StepLayer;

import java.util.List;

/**
 * Proveedor del catálogo i18n para la capa WEB (Playwright).
 *
 * <p>Registra todos los steps canónicos de web-core con sus claves de
 * descripción y hints de parámetros. Las traducciones viven en
 * {@code i18n/web_steps_*.properties} dentro de este JAR.
 */
public class WebStepCatalogProvider implements StepCatalogProvider {

    private static final String CAT_NAV   = "step.web.category.navigation";
    private static final String CAT_INTER = "step.web.category.interaction";
    private static final String CAT_FORM  = "step.web.category.forms";
    private static final String CAT_VALID = "step.web.category.assertions";
    private static final String CAT_WAIT  = "step.web.category.wait";
    private static final String CAT_CFG   = "step.web.category.config";

    private static final List<StepCatalogEntry> ENTRIES = List.of(

        // ── Configuración ────────────────────────────────────────────────────
        entry("web.config.navigate",
              "I am on the {string} page",
              "GIVEN", CAT_CFG, "step.web.config.navigate.description",
              List.of(param("url", "step.web.config.navigate.param.url"))),

        // ── Navegación ───────────────────────────────────────────────────────
        entry("web.navigation.go",
              "I navigate to {string}",
              "WHEN", CAT_NAV, "step.web.navigation.go.description",
              List.of(param("url", "step.web.navigation.go.param.url"))),

        entry("web.navigation.new-tab",
              "I open a new browser tab",
              "WHEN", CAT_NAV, "step.web.navigation.new-tab.description",
              List.of()),

        entry("web.navigation.maximize",
              "I maximize the browser window",
              "WHEN", CAT_NAV, "step.web.navigation.maximize.description",
              List.of()),

        entry("web.navigation.key",
              "I press the {string} key",
              "WHEN", CAT_NAV, "step.web.navigation.key.description",
              List.of(param("key", "step.web.navigation.key.param.key"))),

        entry("web.navigation.combo",
              "I press the {string}+{string} keys",
              "WHEN", CAT_NAV, "step.web.navigation.combo.description",
              List.of(param("modifier", "step.web.navigation.combo.param.modifier"),
                      param("key", "step.web.navigation.combo.param.key"))),

        // ── Frame ────────────────────────────────────────────────────────────
        entry("web.frame.leave",
              "I leave the iframe",
              "WHEN", CAT_NAV, "step.web.frame.leave.description",
              List.of()),

        // ── Window ───────────────────────────────────────────────────────────

        // ── Click / Interacción ──────────────────────────────────────────────
        entry("web.click.click",
              "I click {string}",
              "WHEN", CAT_INTER, "step.web.click.click.description",
              List.of(param("locator", "step.web.click.param.locator"))),

        entry("web.click.click-inside",
              "I click {string} inside {string}",
              "WHEN", CAT_INTER, "step.web.click.click-inside.description",
              List.of(param("locator", "step.web.click.param.locator"),
                      param("container", "step.web.click.param.container"))),

        entry("web.click.click-row",
              "I click {string} in row {int}",
              "WHEN", CAT_INTER, "step.web.click.click-row.description",
              List.of(param("locator", "step.web.click.param.locator"),
                      param("row", "step.web.click.param.row"))),

        entry("web.click.double-click",
              "I double-click {string}",
              "WHEN", CAT_INTER, "step.web.click.double-click.description",
              List.of(param("locator", "step.web.click.param.locator"))),

        entry("web.click.right-click",
              "I right-click {string}",
              "WHEN", CAT_INTER, "step.web.click.right-click.description",
              List.of(param("locator", "step.web.click.param.locator"))),

        entry("web.click.hover",
              "I hover over {string}",
              "WHEN", CAT_INTER, "step.web.click.hover.description",
              List.of(param("locator", "step.web.click.param.locator"))),

        // ── Alert ────────────────────────────────────────────────────────────
        entry("web.alert.accept",
              "I accept the system alert",
              "WHEN", CAT_INTER, "step.web.alert.accept.description",
              List.of()),

        entry("web.alert.dismiss",
              "I dismiss the system alert",
              "WHEN", CAT_INTER, "step.web.alert.dismiss.description",
              List.of()),

        // ── Input / Forms ────────────────────────────────────────────────────
        entry("web.input.fill",
              "I fill {string} with {string}",
              "WHEN", CAT_FORM, "step.web.input.fill.description",
              List.of(param("label", "step.web.input.fill.param.label"),
                      param("value", "step.web.input.fill.param.value"))),

        entry("web.input.fill-nth",
              "I fill the {int}. {string} with {string}",
              "WHEN", CAT_FORM, "step.web.input.fill-nth.description",
              List.of(param("index", "step.web.input.fill-nth.param.index"),
                      param("label", "step.web.input.fill.param.label"),
                      param("value", "step.web.input.fill.param.value"))),

        entry("web.input.clear",
              "I clear {string}",
              "WHEN", CAT_FORM, "step.web.input.clear.description",
              List.of(param("locator", "step.web.click.param.locator"))),

        // ── Select ───────────────────────────────────────────────────────────
        entry("web.select.dropdown",
              "I select {string} from the {string} dropdown",
              "WHEN", CAT_FORM, "step.web.select.dropdown.description",
              List.of(param("option", "step.web.select.dropdown.param.option"),
                      param("dropdown", "step.web.select.dropdown.param.dropdown"))),

        // ── Scroll ───────────────────────────────────────────────────────────
        entry("web.scroll.down",
              "I scroll down",
              "WHEN", CAT_INTER, "step.web.scroll.down.description",
              List.of()),

        entry("web.scroll.up",
              "I scroll up",
              "WHEN", CAT_INTER, "step.web.scroll.up.description",
              List.of()),

        entry("web.scroll.to",
              "I scroll to {string}",
              "WHEN", CAT_INTER, "step.web.scroll.to.description",
              List.of(param("locator", "step.web.click.param.locator"))),

        entry("web.scroll.down-pixels",
              "I scroll down by {int} pixels",
              "WHEN", CAT_INTER, "step.web.scroll.down-pixels.description",
              List.of(param("pixels", "step.web.scroll.pixels.param.pixels"))),

        entry("web.scroll.down-until",
              "I scroll down until I see {string}",
              "WHEN", CAT_INTER, "step.web.scroll.down-until.description",
              List.of(param("locator", "step.web.click.param.locator"))),

        entry("web.scroll.up-until",
              "I scroll up until I see {string}",
              "WHEN", CAT_INTER, "step.web.scroll.up-until.description",
              List.of(param("locator", "step.web.click.param.locator"))),

        // ── Wait ─────────────────────────────────────────────────────────────
        entry("web.wait.visible",
              "I wait for {string} to be visible",
              "WHEN", CAT_WAIT, "step.web.wait.visible.description",
              List.of(param("locator", "step.web.click.param.locator"))),

        entry("web.wait.hidden",
              "I wait for {string} to be hidden",
              "WHEN", CAT_WAIT, "step.web.wait.hidden.description",
              List.of(param("locator", "step.web.click.param.locator"))),

        // ── Element Validation ───────────────────────────────────────────────
        entry("web.assert.visible",
              "I should see {string}",
              "THEN", CAT_VALID, "step.web.assert.visible.description",
              List.of(param("locator", "step.web.click.param.locator"))),

        entry("web.assert.not-visible",
              "I should not see {string}",
              "THEN", CAT_VALID, "step.web.assert.not-visible.description",
              List.of(param("locator", "step.web.click.param.locator"))),

        entry("web.assert.text",
              "the element {string} should contain {string}",
              "THEN", CAT_VALID, "step.web.assert.text.description",
              List.of(param("locator", "step.web.click.param.locator"),
                      param("text", "step.web.assert.text.param.text"))),

        entry("web.assert.title",
              "the page title should be {string}",
              "THEN", CAT_VALID, "step.web.assert.title.description",
              List.of(param("title", "step.web.assert.title.param.title"))),

        entry("web.assert.url-contains",
              "the page url should contain {string}",
              "THEN", CAT_VALID, "step.web.assert.url-contains.description",
              List.of(param("fragment", "step.web.assert.url-contains.param.fragment")))
    );

    @Override
    public StepLayer layer() {
        return StepLayer.WEB;
    }

    @Override
    public String bundleBaseName() {
        return "i18n/web_steps";
    }

    @Override
    public List<StepCatalogEntry> entries() {
        return ENTRIES;
    }

    // ── builders ─────────────────────────────────────────────────────────────

    private static StepCatalogEntry entry(String stepId, String pattern, String phase,
                                          String categoryKey, String descriptionKey,
                                          List<ParamSpec> params) {
        return new StepCatalogEntry(stepId, pattern, StepLayer.WEB, phase,
                                    categoryKey, descriptionKey, params);
    }

    private static ParamSpec param(String name, String hintKey) {
        return new ParamSpec(name, hintKey);
    }
}
