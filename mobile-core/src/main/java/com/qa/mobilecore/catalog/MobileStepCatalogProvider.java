package com.qa.mobilecore.catalog;

import com.qa.common.api.stepcatalog.ParamSpec;
import com.qa.common.api.stepcatalog.StepCatalogEntry;
import com.qa.common.api.stepcatalog.StepCatalogProvider;
import com.qa.common.api.stepcatalog.StepLayer;

import java.util.List;

/**
 * Proveedor del catálogo i18n para la capa MOBILE (Appium).
 *
 * <p>Registra todos los steps canónicos de mobile-core con sus claves de
 * descripción y hints de parámetros. Las traducciones viven en
 * {@code i18n/mobile_steps_*.properties} dentro de este JAR.
 */
public class MobileStepCatalogProvider implements StepCatalogProvider {

    private static final String CAT_APP    = "step.mobile.category.app";
    private static final String CAT_INTER  = "step.mobile.category.interaction";
    private static final String CAT_FORM   = "step.mobile.category.forms";
    private static final String CAT_SCROLL = "step.mobile.category.scroll";
    private static final String CAT_GESTURE= "step.mobile.category.gestures";
    private static final String CAT_DEVICE = "step.mobile.category.device";
    private static final String CAT_VALID  = "step.mobile.category.assertions";
    private static final String CAT_PERM   = "step.mobile.category.permissions";

    private static final List<StepCatalogEntry> ENTRIES = List.of(

        // ── App lifecycle (GIVEN) ─────────────────────────────────────────────
        entry("mobile.app.running",
              "the app is running",
              "GIVEN", CAT_APP, "step.mobile.app.running.description",
              List.of()),

        entry("mobile.app.screen",
              "the app is on the {string} screen",
              "GIVEN", CAT_APP, "step.mobile.app.screen.description",
              List.of(param("screen", "step.mobile.app.screen.param.screen"))),

        entry("mobile.app.foreground",
              "the app is in the foreground",
              "GIVEN", CAT_APP, "step.mobile.app.foreground.description",
              List.of()),

        entry("mobile.app.background",
              "the app is in the background",
              "GIVEN", CAT_APP, "step.mobile.app.background.description",
              List.of()),

        entry("mobile.app.relaunch",
              "I relaunch the app",
              "GIVEN", CAT_APP, "step.mobile.app.relaunch.description",
              List.of()),

        entry("mobile.app.reset",
              "I reset the app state",
              "GIVEN", CAT_APP, "step.mobile.app.reset.description",
              List.of()),

        // ── Interacción básica (WHEN) ─────────────────────────────────────────
        entry("mobile.when.tap",
              "I tap {string}",
              "WHEN", CAT_INTER, "step.mobile.when.tap.description",
              List.of(param("locator", "step.mobile.param.locator"))),

        entry("mobile.when.double-tap",
              "I double-tap {string}",
              "WHEN", CAT_INTER, "step.mobile.when.double-tap.description",
              List.of(param("locator", "step.mobile.param.locator"))),

        entry("mobile.when.long-press",
              "I long press {string}",
              "WHEN", CAT_INTER, "step.mobile.when.long-press.description",
              List.of(param("locator", "step.mobile.param.locator"))),

        entry("mobile.when.tap-inside",
              "I tap {string} inside {string}",
              "WHEN", CAT_INTER, "step.mobile.when.tap-inside.description",
              List.of(param("locator",   "step.mobile.param.locator"),
                      param("container", "step.mobile.param.container"))),

        entry("mobile.when.tap-nth",
              "I tap the {int}. {string}",
              "WHEN", CAT_INTER, "step.mobile.when.tap-nth.description",
              List.of(param("index",   "step.mobile.param.index"),
                      param("locator", "step.mobile.param.locator"))),

        entry("mobile.when.tap-accessibility",
              "I tap the element with accessibility id {string}",
              "WHEN", CAT_INTER, "step.mobile.when.tap-accessibility.description",
              List.of(param("accessibilityId", "step.mobile.param.accessibilityId"))),

        entry("mobile.when.tap-resource-id",
              "I tap the element with resource id {string}",
              "WHEN", CAT_INTER, "step.mobile.when.tap-resource-id.description",
              List.of(param("resourceId", "step.mobile.param.resourceId"))),

        entry("mobile.when.tap-xpath",
              "I tap the element with xpath {string}",
              "WHEN", CAT_INTER, "step.mobile.when.tap-xpath.description",
              List.of(param("xpath", "step.mobile.param.xpath"))),

        // ── Formularios (WHEN) ────────────────────────────────────────────────
        entry("mobile.when.fill",
              "I fill {string} with {string}",
              "WHEN", CAT_FORM, "step.mobile.when.fill.description",
              List.of(param("locator", "step.mobile.param.locator"),
                      param("value",   "step.mobile.param.value"))),

        entry("mobile.when.fill-nth",
              "I fill the {int}. {string} with {string}",
              "WHEN", CAT_FORM, "step.mobile.when.fill-nth.description",
              List.of(param("index",   "step.mobile.param.index"),
                      param("locator", "step.mobile.param.locator"),
                      param("value",   "step.mobile.param.value"))),

        entry("mobile.when.fill-accessibility",
              "I fill the element with accessibility id {string} with {string}",
              "WHEN", CAT_FORM, "step.mobile.when.fill-accessibility.description",
              List.of(param("accessibilityId", "step.mobile.param.accessibilityId"),
                      param("value",           "step.mobile.param.value"))),

        entry("mobile.when.fill-resource-id",
              "I fill the element with resource id {string} with {string}",
              "WHEN", CAT_FORM, "step.mobile.when.fill-resource-id.description",
              List.of(param("resourceId", "step.mobile.param.resourceId"),
                      param("value",      "step.mobile.param.value"))),

        entry("mobile.when.fill-xpath",
              "I fill the element with xpath {string} with {string}",
              "WHEN", CAT_FORM, "step.mobile.when.fill-xpath.description",
              List.of(param("xpath",  "step.mobile.param.xpath"),
                      param("value",  "step.mobile.param.value"))),

        entry("mobile.when.clear",
              "I clear {string}",
              "WHEN", CAT_FORM, "step.mobile.when.clear.description",
              List.of(param("locator", "step.mobile.param.locator"))),

        entry("mobile.when.picker",
              "I select {string} from the {string} picker",
              "WHEN", CAT_FORM, "step.mobile.when.picker.description",
              List.of(param("option", "step.mobile.when.picker.param.option"),
                      param("picker", "step.mobile.when.picker.param.picker"))),

        // ── Scroll (WHEN) ─────────────────────────────────────────────────────
        entry("mobile.when.scroll-down",
              "I scroll down",
              "WHEN", CAT_SCROLL, "step.mobile.when.scroll-down.description",
              List.of()),

        entry("mobile.when.scroll-up",
              "I scroll up",
              "WHEN", CAT_SCROLL, "step.mobile.when.scroll-up.description",
              List.of()),

        entry("mobile.when.scroll-left",
              "I scroll left",
              "WHEN", CAT_SCROLL, "step.mobile.when.scroll-left.description",
              List.of()),

        entry("mobile.when.scroll-right",
              "I scroll right",
              "WHEN", CAT_SCROLL, "step.mobile.when.scroll-right.description",
              List.of()),

        entry("mobile.when.scroll-down-until",
              "I scroll down until I see {string}",
              "WHEN", CAT_SCROLL, "step.mobile.when.scroll-down-until.description",
              List.of(param("locator", "step.mobile.param.locator"))),

        entry("mobile.when.scroll-up-until",
              "I scroll up until I see {string}",
              "WHEN", CAT_SCROLL, "step.mobile.when.scroll-up-until.description",
              List.of(param("locator", "step.mobile.param.locator"))),

        // ── Gestos (WHEN) ─────────────────────────────────────────────────────
        entry("mobile.when.swipe-left",
              "I swipe left on {string}",
              "WHEN", CAT_GESTURE, "step.mobile.when.swipe-left.description",
              List.of(param("locator", "step.mobile.param.locator"))),

        entry("mobile.when.swipe-right",
              "I swipe right on {string}",
              "WHEN", CAT_GESTURE, "step.mobile.when.swipe-right.description",
              List.of(param("locator", "step.mobile.param.locator"))),

        entry("mobile.when.pinch-zoom-in",
              "I pinch to zoom in on {string}",
              "WHEN", CAT_GESTURE, "step.mobile.when.pinch-zoom-in.description",
              List.of(param("locator", "step.mobile.param.locator"))),

        entry("mobile.when.pinch-zoom-out",
              "I pinch to zoom out on {string}",
              "WHEN", CAT_GESTURE, "step.mobile.when.pinch-zoom-out.description",
              List.of(param("locator", "step.mobile.param.locator"))),

        // ── Dispositivo (WHEN) ────────────────────────────────────────────────
        entry("mobile.when.back",
              "I press the back button",
              "WHEN", CAT_DEVICE, "step.mobile.when.back.description",
              List.of()),

        entry("mobile.when.home",
              "I press the home button",
              "WHEN", CAT_DEVICE, "step.mobile.when.home.description",
              List.of()),

        entry("mobile.when.hide-keyboard",
              "I hide the keyboard",
              "WHEN", CAT_DEVICE, "step.mobile.when.hide-keyboard.description",
              List.of()),

        // ── Permisos (WHEN) ───────────────────────────────────────────────────
        entry("mobile.when.grant-permission",
              "I grant the {string} permission",
              "WHEN", CAT_PERM, "step.mobile.when.grant-permission.description",
              List.of(param("permission", "step.mobile.when.permission.param"))),

        entry("mobile.when.deny-permission",
              "I deny the {string} permission",
              "WHEN", CAT_PERM, "step.mobile.when.deny-permission.description",
              List.of(param("permission", "step.mobile.when.permission.param")))
    );

    @Override
    public StepLayer layer() {
        return StepLayer.MOBILE;
    }

    @Override
    public String bundleBaseName() {
        return "i18n/mobile_steps";
    }

    @Override
    public List<StepCatalogEntry> entries() {
        return ENTRIES;
    }

    private static StepCatalogEntry entry(String stepId, String pattern, String phase,
                                          String categoryKey, String descriptionKey,
                                          List<ParamSpec> params) {
        return new StepCatalogEntry(stepId, pattern, StepLayer.MOBILE, phase,
                                    categoryKey, descriptionKey, params);
    }

    private static ParamSpec param(String name, String hintKey) {
        return new ParamSpec(name, hintKey);
    }
}
