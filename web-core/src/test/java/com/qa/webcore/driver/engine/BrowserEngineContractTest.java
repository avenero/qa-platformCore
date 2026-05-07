package com.qa.webcore.driver.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BrowserEngine contract")
class BrowserEngineContractTest {

    @Test
    @DisplayName("Define API obligatoria de P0-2 y evita metodos deprecados")
    void apiMatchesArchitecturalContract() {
        Set<String> methodNames = Set.of(BrowserEngine.class.getDeclaredMethods())
                .stream()
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(methodNames).contains(
                "find",
                "findAll",
                "isPresent",
                "click",
                "type",
                "clear",
                "selectOption",
                "hover",
                "doubleClick",
                "navigateTo",
                "getCurrentUrl",
                "getTitle",
                "back",
                "refresh",
                "waitForVisible",
                "waitForText",
                "waitForHidden",
                "waitForUrl",
                "waitForLoadState",
                "findInFrame",
                "clickInFrame",
                "screenshot",
                "screenshotElement",
                "evaluate",
                "scrollIntoView",
                "switchToNewWindow",
                "getWindowTitle",
                "closeCurrentWindow",
                "getAlertText",
                "acceptAlert",
                "dismissAlert",
                "isActive"
        );

        assertThat(methodNames)
                .as("La interfaz no debe exponer waitForNavigation (C-06)")
                .doesNotContain("waitForNavigation");
        assertThat(methodNames)
                .as("La interfaz no debe exponer switchToFrame/switchToDefaultContent (C-07)")
                .doesNotContain("switchToFrame", "switchToDefaultContent");
    }

    @Test
    @DisplayName("Element contract usa BrowserElement sin colision con Playwright")
    void browserElementContractExists() {
        Set<String> methodNames = Set.of(BrowserElement.class.getDeclaredMethods())
                .stream()
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(methodNames).containsExactlyInAnyOrder(
                "getText",
                "getAttribute",
                "isVisible",
                "isEnabled",
                "isSelected",
                "click",
                "type"
        );
    }
}
