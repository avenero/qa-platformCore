package com.qa.webcore.utils;

import com.qa.common.internal.config.ConfigManager;
import com.qa.common.api.logging.TestLogger;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.webcore.driver.engine.BrowserElement;
import com.qa.webcore.driver.engine.BrowserEngine;
import io.cucumber.java.Scenario;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper web Playwright-first para soporte de steps legacy.
 */
public class WebHelper {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final String TEMP_VAR_PREFIX = "web.temp.";
    private static final int RUT_UNIQUE_LENGTH = 8;
    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    public String getConfigProperty(String key, String defaultValue) {
        return ExecutionContext.current()
                .map(ctx -> ctx.config().getProperty(key, defaultValue))
                .orElseGet(() -> ConfigManager.getInstance().get(key, defaultValue));
    }

    public String normalizeBrowserName(String browserName) {
        if (browserName == null || browserName.isBlank()) {
            return "chromium";
        }
        String normalized = browserName.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "chrome", "chromium" -> "chromium";
            case "firefox" -> "firefox";
            case "webkit", "safari" -> "webkit";
            default -> throw new IllegalArgumentException("Navegador no soportado: " + browserName);
        };
    }

    public String parseBrowserType(String browserName) {
        return normalizeBrowserName(browserName);
    }

    public boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "1", "si", "sí", "yes", "y", "on" -> true;
            case "false", "0", "no", "off", "n" -> false;
            default -> throw new IllegalArgumentException("Valor booleano inválido: " + value);
        };
    }

    public void captureScreen(Scenario scenario) {
        if (scenario == null) {
            return;
        }
        try {
            byte[] screenshot = engineOptional().map(BrowserEngine::screenshot).orElse(new byte[0]);
            if (screenshot.length > 0) {
                scenario.attach(screenshot, "image/png", "screenshot");
            }
        } catch (Exception e) {
            TestLogger.logWarning("WEB_HELPER", "No fue posible adjuntar screenshot: " + e.getMessage(), null);
        }
    }

    public void captureScreenOnFailure(Scenario scenario) {
        if (scenario != null && scenario.isFailed()) {
            captureScreen(scenario);
        }
    }

    public void attachInReport(Scenario scenario, String fileName) {
        if (scenario == null) {
            return;
        }
        byte[] screenshot = engine().screenshot();
        scenario.attach(screenshot, "image/png", fileName == null ? "screenshot" : fileName);
    }

    public void attachScenario(byte[] data, Scenario scenario) {
        if (scenario != null && data != null && data.length > 0) {
            scenario.attach(data, "image/png", "screenshot");
        }
    }

    public void attachScenario(String data, Scenario scenario) {
        if (scenario != null && data != null) {
            scenario.attach(data.getBytes(StandardCharsets.UTF_8), "text/plain", "scenario-data");
        }
    }

    public void generateFileTxt(String fileName) {
        String safeName = (fileName == null || fileName.isBlank()) ? "evidencia" : fileName;
        Path output = Path.of("target", safeName + "-" + Instant.now().toEpochMilli() + ".txt");
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, "Generado por WebHelper", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar archivo de evidencia: " + output, e);
        }
    }

    public void setCookie(String nombre, String valor) {
        engine().evaluate("document.cookie = `${arguments[0]}=${arguments[1]}; path=/`;", nombre, valor);
    }

    public String resolveVariables(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(value);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String var = matcher.group(1);
            String replacement = getTextVariableTemp(var);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement == null ? "" : replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public void saveVariableTemp(String value, String variableName) {
        ExecutionContext.requireCurrent().variables().set(TEMP_VAR_PREFIX + variableName, value);
    }

    public String getTextVariableTemp(String variableName) {
        return ExecutionContext.requireCurrent().variables()
                .get(TEMP_VAR_PREFIX + variableName, String.class)
                .orElse("");
    }

    public String getTextOf(String locator) {
        return safeText(engine().find(locator).getText());
    }

    public String getAttributeValue(String locator, String attribute) {
        return safeText(engine().find(locator).getAttribute(attribute));
    }

    public String generateRutUy() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, RUT_UNIQUE_LENGTH);
    }

    public boolean validateAdditionVariables(String var1, String var2, String varResult) {
        BigDecimal left = asDecimal(getTextVariableTemp(var1));
        BigDecimal right = asDecimal(getTextVariableTemp(var2));
        BigDecimal expected = asDecimal(getTextVariableTemp(varResult));
        return left.add(right).setScale(2, RoundingMode.HALF_UP)
                .compareTo(expected.setScale(2, RoundingMode.HALF_UP)) == 0;
    }

    public boolean isActive(String locator) {
        return engine().find(locator).isEnabled();
    }

    public void verifyTextInNestedShadow(String element, String text, String host1, String host2) {
        String actual = String.valueOf(engine().evaluate("""
            const h1 = document.querySelector(arguments[0]);
            const h2 = h1?.shadowRoot?.querySelector(arguments[1]);
            const el = h2?.shadowRoot?.querySelector(arguments[2]);
            return (el?.textContent ?? '').trim();
            """, host1, host2, element));
        assertTrue(actual.contains(resolveVariables(text)), "Texto no encontrado en nested shadow");
    }

    public void verifyTextInShadow(String element, String text, String host) {
        String actual = String.valueOf(engine().evaluate("""
            const h = document.querySelector(arguments[0]);
            const el = h?.shadowRoot?.querySelector(arguments[1]);
            return (el?.textContent ?? '').trim();
            """, host, element));
        assertTrue(actual.contains(resolveVariables(text)), "Texto no encontrado en shadow");
    }

    public boolean valueElementNotZero(String locator) {
        return asDecimal(elementValue(locator)).compareTo(BigDecimal.ZERO) != 0;
    }

    public boolean valueElementNotContains(String locator, String text) {
        return !elementValue(locator).contains(resolveVariables(text));
    }

    public boolean valueElementNotEmpty(String locator) {
        return !elementValue(locator).isBlank();
    }

    public void isFieldEquals(String campo, String expectedValue) {
        String actual = elementValue(campo);
        String expected = resolveVariables(expectedValue);
        assertTrue(actual.equals(expected), "Campo '" + campo + "' no coincide. actual=" + actual);
    }

    public void isElementShadowPresent(String element, String host) {
        boolean present = Boolean.TRUE.equals(engine().evaluate("""
            const h = document.querySelector(arguments[0]);
            return !!h?.shadowRoot?.querySelector(arguments[1]);
            """, host, element));
        assertTrue(present, "Elemento no presente dentro del shadow host");
    }

    public void findElementInShadow(String host, String element) {
        isElementShadowPresent(element, host);
    }

    public void validateFieldAcceptsOnlyNumbers(String locator) {
        assertTrue(elementValue(locator).matches("^\\d+$"), "El campo no contiene solo números");
    }

    public void validateFieldAcceptsOnlyLetters(String locator) {
        assertTrue(elementValue(locator).matches("^[\\p{L}\\s]+$"), "El campo no contiene solo letras");
    }

    public void validateFieldNoNumbersNoSpecialChars(String locator) {
        assertTrue(elementValue(locator).matches("^[A-Za-z\\s]+$"),
                "El campo contiene números o caracteres especiales");
    }

    public void validateEmailFormat(String locator) {
        assertTrue(elementValue(locator).matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"), "Formato de email inválido");
    }

    public void validatePhoneFormat(String locator, String prefix, int totalDigits) {
        String value = elementValue(locator);
        assertTrue(value.startsWith(prefix), "Prefijo telefónico inválido");
        String digits = value.replaceAll("\\D", "");
        assertTrue(digits.length() == totalDigits, "Cantidad de dígitos inválida");
    }

    public void validateFieldNoSpaces(String locator) {
        assertTrue(!elementValue(locator).contains(" "), "El campo contiene espacios");
    }

    public void validateThousandsSeparators(String locator) {
        assertTrue(elementValue(locator).matches("^-?\\d{1,3}(,\\d{3})*(\\.\\d+)?$"),
                "Formato con separador de miles inválido");
    }

    public void validateFieldMatchesPattern(String locator, String regexPattern) {
        assertTrue(elementValue(locator).matches(regexPattern), "El valor no coincide con el patrón esperado");
    }

    public void validateFormattedValue(String expectedValue) {
        String body = safeText(engine().find("body").getText());
        assertTrue(body.contains(resolveVariables(expectedValue)), "Valor formateado no encontrado en pantalla");
    }

    public void validateMinValue(String locator, int minValue) {
        assertTrue(asDecimal(elementValue(locator)).compareTo(BigDecimal.valueOf(minValue)) >= 0,
                "El valor es menor al mínimo esperado");
    }

    public void validateMaxValue(String locator, int maxValue) {
        assertTrue(asDecimal(elementValue(locator)).compareTo(BigDecimal.valueOf(maxValue)) <= 0,
                "El valor es mayor al máximo esperado");
    }

    public void validateFieldIsReadonly(String locator) {
        String readonly = engine().find(locator).getAttribute("readonly");
        assertTrue(readonly != null, "El campo no es readonly");
    }

    public void validatePlaceholder(String locator, String expectedPlaceholder) {
        assertTrue(resolveVariables(expectedPlaceholder).equals(engine().find(locator).getAttribute("placeholder")),
                "Placeholder inválido");
    }

    public void validateTooltip(String locator, String expectedTooltip) {
        String title = engine().find(locator).getAttribute("title");
        assertTrue(resolveVariables(expectedTooltip).equals(safeText(title)), "Tooltip inválido");
    }

    public void validateMessageIsVisible(String locator) {
        assertTrue(engine().isPresent(locator) && engine().find(locator).isVisible(), "Mensaje no visible");
    }

    public void validateMessageIsNotVisible(String locator) {
        assertTrue(!engine().isPresent(locator) || !engine().find(locator).isVisible(), "Mensaje visible");
    }

    public void validateMessageContainsText(String locator, String expectedText) {
        String actual = safeText(engine().find(locator).getText());
        assertTrue(actual.contains(resolveVariables(expectedText)), "Mensaje no contiene texto esperado");
    }

    public void validateFieldNotEmpty(String locator) {
        assertTrue(!elementValue(locator).isBlank(), "Campo vacío");
    }

    public void validateFieldIsEmpty(String locator) {
        assertTrue(elementValue(locator).isBlank(), "Campo no está vacío");
    }

    public void validateButtonIsEnabled(String locator) {
        assertTrue(engine().find(locator).isEnabled(), "Botón deshabilitado");
    }

    public void validateButtonIsDisabled(String locator) {
        assertTrue(!engine().find(locator).isEnabled(), "Botón habilitado");
    }

    public void validateButtonTextChange(String locator, String initialText, String finalText) {
        String current = safeText(engine().find(locator).getText());
        assertTrue(!resolveVariables(initialText).equals(current), "El texto no cambió");
        assertTrue(resolveVariables(finalText).equals(current), "Texto final no coincide");
    }

    public void validateMinLength(String locator, int minLength) {
        assertTrue(elementValue(locator).length() >= minLength, "Longitud menor al mínimo");
    }

    public void validateMaxLength(String locator, int maxLength) {
        assertTrue(elementValue(locator).length() <= maxLength, "Longitud mayor al máximo");
    }

    public void validateExactLength(String locator, int expectedLength) {
        assertTrue(elementValue(locator).length() == expectedLength, "Longitud exacta inválida");
    }

    public int countElements(String locator) {
        return engine().findAll(locator).size();
    }

    public void validateAttributeValue(String locator, String attribute, String expectedValue) {
        assertTrue(resolveVariables(expectedValue).equals(safeText(engine().find(locator).getAttribute(attribute))),
                "El atributo no coincide");
    }

    public void validateHasCssClass(String locator, String cssClass) {
        String klass = safeText(engine().find(locator).getAttribute("class"));
        assertTrue(List.of(klass.split("\\s+")).contains(cssClass), "No tiene class CSS esperada");
    }

    public void validateCheckboxSelected(String locator, boolean expectedSelected) {
        assertTrue(engine().find(locator).isSelected() == expectedSelected, "Estado de checkbox inválido");
    }

    public void backToPrincipalWindow() {
        // No-op en modo Playwright-only.
    }

    public void handleTabs() {
        engine().switchToNewWindow();
    }

    public void closedLastWindows() {
        engine().closeCurrentWindow();
    }

    public void waitCheckBox(String locator) {
        engine().waitForVisible(locator, DEFAULT_TIMEOUT_MS);
    }

    @SuppressWarnings("unchecked")
    public List<String> getListComboBox(String locator) {
        Object result = engine().evaluate("""
            const sel = document.querySelector(arguments[0]);
            if (!sel) return [];
            return [...sel.options].map(o => (o.textContent || '').trim());
            """, locator);
        if (result instanceof List<?> list) {
            List<String> values = new ArrayList<>(list.size());
            for (Object item : list) {
                values.add(item == null ? "" : item.toString());
            }
            return values;
        }
        return List.of();
    }

    public String getTextOfCombobox(String locator) {
        Object result = engine().evaluate("""
            const sel = document.querySelector(arguments[0]);
            return sel?.selectedOptions?.[0]?.textContent ?? '';
            """, locator);
        return safeText(result);
    }

    public boolean radioBtnIsSelect(String locator) {
        return engine().find(locator).isSelected();
    }

    public void validateDropdownOptions(String locator, List<String> expectedOptions) {
        List<String> options = getListComboBox(locator);
        List<String> normalizedExpected = expectedOptions.stream().map(this::resolveVariables).toList();
        assertTrue(options.equals(normalizedExpected), "Opciones del dropdown no coinciden");
    }

    public void validateDropdownOptions(String locator, String expectedOptions) {
        List<String> expected = List.of(expectedOptions.split(","))
                .stream()
                .map(String::trim)
                .map(this::resolveVariables)
                .toList();
        validateDropdownOptions(locator, expected);
    }

    public void validateDropdownOptionCount(String locator, int expectedCount) {
        assertTrue(getListComboBox(locator).size() == expectedCount, "Cantidad de opciones inválida");
    }

    public void validateSingleSelection(String locator) {
        Object result = engine().evaluate("""
            const sel = document.querySelector(arguments[0]);
            return !!sel && !sel.multiple;
            """, locator);
        assertTrue(Boolean.TRUE.equals(result), "El combo permite selección múltiple");
    }

    public void uploadFile(String locator, String filePath) {
        throw new UnsupportedOperationException(
                "uploadFile requiere API nativa de Playwright (setInputFiles) fuera del contrato BrowserEngine");
    }

    public void tabAction(String locator) {
        engine().click(locator);
        pressKey("Tab");
    }

    public void clickNestedShadow(String host1, String host2, String element) {
        engine().evaluate("""
            const h1 = document.querySelector(arguments[0]);
            const h2 = h1?.shadowRoot?.querySelector(arguments[1]);
            const el = h2?.shadowRoot?.querySelector(arguments[2]);
            el?.click();
            """, host1, host2, element);
    }

    public void clickElementInShadow(String element, String host) {
        engine().evaluate("""
            const h = document.querySelector(arguments[0]);
            const el = h?.shadowRoot?.querySelector(arguments[1]);
            el?.click();
            """, host, element);
    }

    public void clickJs(String locator) {
        engine().evaluate("document.querySelector(arguments[0])?.click();", locator);
    }

    public void rightClick(String locator) {
        engine().evaluate("""
            const el = document.querySelector(arguments[0]);
            if (!el) return;
            el.dispatchEvent(new MouseEvent('contextmenu', { bubbles: true }));
            """, locator);
    }

    public void doClicTeclaESC() {
        pressKey("Escape");
    }

    public void pressKey(String key) {
        engine().evaluate("""
            document.dispatchEvent(new KeyboardEvent('keydown', { key: arguments[0], bubbles: true }));
            document.dispatchEvent(new KeyboardEvent('keyup', { key: arguments[0], bubbles: true }));
            """, key);
    }

    public void pressKeyOnElement(String key, String locator) {
        engine().evaluate("""
            const el = document.querySelector(arguments[1]);
            if (!el) return;
            el.dispatchEvent(new KeyboardEvent('keydown', { key: arguments[0], bubbles: true }));
            el.dispatchEvent(new KeyboardEvent('keyup', { key: arguments[0], bubbles: true }));
            """, key, locator);
    }

    public void clickAndGoToElementInShadow(String element, String host) {
        clickElementInShadow(element, host);
    }

    public void clickAndGoToElementInShadowInNode(String element, String host1, String host2) {
        clickNestedShadow(host1, host2, element);
    }

    public void checkTextAndClic(String text) {
        String resolved = resolveVariables(text);
        engine().evaluate("""
            const targetText = arguments[0];
            const all = [...document.querySelectorAll('a,button,[role=button]')];
            const target = all.find(el => (el.textContent || '').trim() === targetText);
            target?.click();
            """, resolved);
    }

    public void waitForHome() {
        engine().waitForLoadState("load", DEFAULT_TIMEOUT_MS);
    }

    public void setTextWithWait(String locator, String text) {
        engine().waitForVisible(locator, DEFAULT_TIMEOUT_MS);
        engine().type(locator, resolveVariables(text));
    }

    public void clicButton(String locator) {
        engine().click(locator);
    }

    private BrowserEngine engine() {
        return ExecutionContext.requireCurrent().registry().require(BrowserEngine.class);
    }

    private Optional<BrowserEngine> engineOptional() {
        return ExecutionContext.current()
                .flatMap(ctx -> ctx.registry().get(BrowserEngine.class));
    }

    private String elementValue(String locator) {
        BrowserElement element = engine().find(locator);
        String value = safeText(element.getAttribute("value"));
        return value.isBlank() ? safeText(element.getText()) : value;
    }

    private BigDecimal asDecimal(String raw) {
        String normalized = safeText(raw).replaceAll("[^0-9,.-]", "").replace(",", ".");
        if (normalized.isBlank() || "-".equals(normalized) || ".".equals(normalized)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(normalized);
    }

    private String safeText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
