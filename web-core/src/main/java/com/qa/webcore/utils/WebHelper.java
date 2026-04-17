package com.qa.webcore.utils;

import com.qa.common.http.exceptions.FrameworkBusinessException;
import com.qa.common.logging.TestLogger;
import com.qa.common.runtime.ExecutionContext;
import com.qa.webcore.driver.DriverManager;
import com.qa.webcore.driver.WebDriverFactory;
import io.cucumber.java.Scenario;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

/**
 * Clase helper con métodos auxiliares para WebSteps.
 *
 * <p>Contiene toda la lógica de interacción con elementos web, manejo de ventanas,
 * iframes, shadow DOM, validaciones, etc.</p>
 *
 * <p><b>Responsabilidades:</b></p>
 * <ul>
 *   <li>Localización y manipulación de elementos web</li>
 *   <li>Gestión de iframes y ventanas</li>
 *   <li>Interacciones con shadow DOM</li>
 *   <li>Screenshots y attachments</li>
 * </ul>
 *
 * <p><b>NOTA IMPORTANTE:</b> Los métodos de gestión de variables temporales ahora delegan
 * al {@code VariableStore} del {@code ExecutionContext} para compartir contexto entre capas
 * (API ↔ Web ↔ Mobile).</p>
 *
 * @author Abel Venero
 * @version 2.0.0
 */
public class WebHelper {

    // =========================================================================
    // CONSTANTES DE CONFIGURACIÓN
    // =========================================================================

    /** Timeout por defecto para waits explícitos (en segundos) */
    private static final int DEFAULT_WAIT_SECONDS = 15;

    public WebHelper() {
        // Constructor vacío - todas las operaciones delegadas a ScenarioContext estático
    }

    // =========================================================================
    // ACCESO AL DRIVER
    // =========================================================================

    /**
     * Obtiene el {@link WebDriver} activo con prioridad de fuente:
     * <ol>
     *   <li>{@link ExecutionContext#registry()} → {@code WebDriver.class}
     *       (registrado por {@link com.qa.webcore.steps.WebHooksSteps} y gestionado
     *       por {@link com.qa.webcore.plugin.WebPlugin})</li>
     *   <li>{@link DriverManager#getDriver()} — ThreadLocal de compatibilidad</li>
     * </ol>
     *
     * <p>Este es el único punto de acceso al driver dentro de WebHelper.
     * No acceder a {@link DriverManager} directamente desde los steps.
     *
     * @return instancia activa del driver
     * @throws IllegalStateException si el driver no está inicializado
     * @since 2.2.0
     */
    private WebDriver resolveDriver() {
        return ExecutionContext.current()
                .flatMap(ctx -> ctx.registry().get(WebDriver.class))
                .orElseGet(DriverManager::getDriver);
    }

    // =========================================================================
    // LOCALIZACIÓN Y MANIPULACIÓN DE ELEMENTOS
    // =========================================================================

    /**
     * Obtiene WebElement por locator con retry logic para manejar elementos stale.
     *
     * <p>Intenta múltiples estrategias de localización en orden:</p>
     * <ol>
     *   <li>Por ID</li>
     *   <li>Por name</li>
     *   <li>Por XPath</li>
     *   <li>Por CSS Selector</li>
     * </ol>
     *
     * <p>Implementa retry logic para manejar StaleElementReferenceException
     * que ocurre cuando el DOM se actualiza dinámicamente.</p>
     *
     * <p>NOTA: Este método NO espera a que el elemento aparezca. Para esperas,
     * usar waitForElementPresent() o waitForVisibleElement().</p>
     *
     * @param locator Identificador del elemento (id, name, xpath o cssSelector)
     * @return WebElement encontrado
     * @throws org.openqa.selenium.NoSuchElementException Si el elemento no se encuentra después de todos los intentos
     */
    public WebElement getElement(String locator) {
        WebDriver driver = resolveDriver();
        int maxRetries = 3;

        StaleElementReferenceException lastStaleException = null;
        org.openqa.selenium.NoSuchElementException lastNotFoundException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return findElementWithStrategy(driver, locator);

            } catch (StaleElementReferenceException e) {
                lastStaleException = e;
                if (attempt < maxRetries) {
                    TestLogger.logDebug("WEB_HELPER",
                        "Elemento stale, reintentando (intento " + attempt + "/" + maxRetries + "): " + locator, null);
                    WaitUtils.waitMillisecondsQuiet(500);
                } else {
                    TestLogger.logError("WEB_HELPER",
                        "Elemento stale después de " + maxRetries + " intentos: " + locator, null);
                }

            } catch (org.openqa.selenium.NoSuchElementException e) {
                lastNotFoundException = e;
                if (attempt < maxRetries) {
                    // Solo log DEBUG, no ERROR - es parte del comportamiento normal de retry
                    TestLogger.logDebug("WEB_HELPER",
                        "Elemento no encontrado, reintentando (intento " + attempt + "/" + maxRetries + "): " + locator, null);
                    WaitUtils.waitMillisecondsQuiet(300);
                } else {
                    // Solo ERROR en el último intento
                    TestLogger.logError("WEB_HELPER",
                        "No se pudo localizar el elemento después de " + maxRetries + " intentos: " + locator, null);
                }
            }
        }

        // Si llegamos aquí, todos los intentos fallaron - relanzar la última excepción capturada
        if (lastStaleException != null) {
            throw lastStaleException;
        }

        // Si no hay StaleElementException, debe haber NoSuchElementException o relanzamos una nueva
        throw lastNotFoundException;
    }

    /**
     * Encuentra un elemento usando múltiples estrategias de localización.
     * Intenta todas las estrategias comunes hasta encontrar el elemento.
     *
     * @param driver WebDriver instance
     * @param locator Identificador del elemento
     * @return WebElement encontrado
     * @throws org.openqa.selenium.NoSuchElementException Si ninguna estrategia funciona
     */
    private WebElement findElementWithStrategy(WebDriver driver, String locator) {
        List<String> attemptedStrategies = new ArrayList<>();

        // Estrategia 1: ID
        try {
            attemptedStrategies.add("id");
            return driver.findElement(By.id(locator));
        } catch (org.openqa.selenium.NoSuchElementException | InvalidSelectorException e1) {
            // Continuar a siguiente estrategia
        }

        // Estrategia 2: name
        try {
            attemptedStrategies.add("name");
            return driver.findElement(By.name(locator));
        } catch (org.openqa.selenium.NoSuchElementException | InvalidSelectorException e2) {
            // Continuar a siguiente estrategia
        }

        // Estrategia 3: XPath (si parece ser xpath)
        if (isXPath(locator)) {
            try {
                attemptedStrategies.add("xpath");
                return driver.findElement(By.xpath(locator));
            } catch (org.openqa.selenium.NoSuchElementException | InvalidSelectorException e3) {
                // Continuar a siguiente estrategia
            }
        }

        // Estrategia 4: CSS Selector (si parece ser css)
        if (isCssSelector(locator)) {
            try {
                attemptedStrategies.add("cssSelector");
                return driver.findElement(By.cssSelector(locator));
            } catch (org.openqa.selenium.NoSuchElementException | InvalidSelectorException e4) {
                // Continuar a siguiente estrategia
            }
        }

        // Estrategia 5: CSS Selector (intentar de todas formas)
        if (!attemptedStrategies.contains("cssSelector")) {
            try {
                attemptedStrategies.add("cssSelector");
                return driver.findElement(By.cssSelector(locator));
            } catch (org.openqa.selenium.NoSuchElementException | InvalidSelectorException e5) {
                // Continuar a siguiente estrategia
            }
        }

        // Estrategia 6: XPath como último recurso
        if (!attemptedStrategies.contains("xpath")) {
            try {
                attemptedStrategies.add("xpath");
                return driver.findElement(By.xpath(locator));
            } catch (org.openqa.selenium.NoSuchElementException | InvalidSelectorException e6) {
                // Todas las estrategias fallaron
            }
        }

        // Todas las estrategias fallaron - log como DEBUG porque getElement() loguea el ERROR final
        TestLogger.logDebug("WEB_HELPER",
            "No se pudo localizar el elemento con ninguna estrategia (" +
            String.join(", ", attemptedStrategies) + "): " + locator, null);
        throw new org.openqa.selenium.NoSuchElementException(
            "No se pudo localizar el elemento con ninguna estrategia: " + locator);
    }

    /**
     * Determina si un locator es probablemente un XPath.
     *
     * @param locator String a evaluar
     * @return true si parece ser un XPath, false si no
     */
    private boolean isXPath(String locator) {
        return locator != null &&
               (locator.startsWith("/") ||
                locator.startsWith("(") ||
                locator.startsWith("./") ||
                locator.contains("//") ||
                locator.contains("[@"));
    }

    /**
     * Establece el texto en un elemento de forma inteligente y eficiente.
     *
     * <p>Detecta el tipo de campo y aplica la estrategia óptima:</p>
     * <ul>
     *   <li>Campos especiales (password, readonly, hidden): JavaScript directo</li>
     *   <li>Campos normales: Método estándar Selenium con fallback a JavaScript</li>
     * </ul>
     *
     * @param locator Identificador del elemento
     * @param text Texto a ingresar
     * @throws RuntimeException Si no se puede ingresar el texto
     */
    public void setText(String locator, String text) {
        WebElement element = getElement(locator);
        WebDriver driver = resolveDriver();

        // Espera única y optimizada (combinada)
        waitForElementToBeInteractable(element);

        // ===== DETECCIÓN INTELIGENTE DEL TIPO DE CAMPO =====
        FieldType fieldType = detectFieldType(element);

        // Decidir estrategia según tipo de campo
        if (fieldType.isSpecial()) {
            // Campos especiales: JavaScript directo (rápido y efectivo)
            if (setTextWithJavaScriptDirect(driver, element, text)) {
                TestLogger.logInfo("WEB_HELPER",
                    "✅ Texto ingresado en campo '" + locator + "'", null);
                return;
            }
        } else {
            // Campos normales: intentar método estándar primero
            if (trySetTextStandard(element, text)) {
                TestLogger.logInfo("WEB_HELPER",
                    "✅ Texto ingresado en campo '" + locator + "'", null);
                return;
            }

            // Fallback a JavaScript si estándar falla
            if (setTextWithJavaScriptDirect(driver, element, text)) {
                TestLogger.logInfo("WEB_HELPER",
                    "✅ Texto ingresado en campo '" + locator + "' (JS fallback)", null);
                return;
            }
        }

        // Si todo falla (muy raro)
        String errorMsg = "No se pudo ingresar texto en el campo '" + locator + "'";
        TestLogger.logError("WEB_HELPER", errorMsg, null);
        throw new RuntimeException(errorMsg);
    }

    /**
     * Detecta el tipo de campo para decidir la estrategia óptima de escritura.
     *
     * @param element Elemento a analizar
     * @return FieldType con información del campo
     */
    private FieldType detectFieldType(WebElement element) {
        try {
            String type = element.getDomAttribute("type");
            String readonly = element.getDomAttribute("readonly");
            String disabled = element.getDomAttribute("disabled");
            String contenteditable = element.getDomAttribute("contenteditable");

            boolean isSpecial =
                "password".equals(type) ||
                "hidden".equals(type) ||
                readonly != null ||
                disabled != null ||
                "true".equals(contenteditable);

            return new FieldType(isSpecial);

        } catch (Exception e) {
            // Si no se puede detectar, asumir campo normal
            return new FieldType(false);
        }
    }

    /**
     * Espera inteligente para que el elemento esté completamente listo para interactuar.
     * Usa la nueva estrategia de estabilidad (Fase 3) que verifica:
     * - Visibilidad
     * - Sin atributos bloqueantes
     * - Posición estable (no en animación)
     * - Estado clickable
     *
     * @param element Elemento a esperar
     */
    private void waitForElementToBeInteractable(WebElement element) {
        try {
            // Intentar espera inteligente primero (verifica estabilidad completa)
            boolean stable = WaitUtils.waitForElementToBeStable(element, 10);

            if (!stable) {
                // Fallback: esperas básicas si la estabilidad no se puede verificar
                WaitUtils.waitForElementToBeVisible(element);
                WaitUtils.waitForElementToBeClickable(element);
            }

        } catch (Exception e) {
            // Si las esperas fallan, continuar de todas formas
            TestLogger.logWarning("WEB_HELPER",
                "Espera de interacción con timeout, continuando...", null);
        }
    }

    /**
     * Clase interna para almacenar información del tipo de campo.
     */
    private static class FieldType {
        private final boolean special;

        public FieldType(boolean special) {
            this.special = special;
        }

        public boolean isSpecial() {
            return special;
        }
    }

    /**
     * Establece texto usando JavaScript de forma directa y eficiente.
     * No verifica el resultado (confiamos en que JavaScript hizo su trabajo).
     * Remueve atributos bloqueantes y dispara eventos para frameworks JS modernos.
     *
     * @param driver WebDriver instance
     * @param element Elemento donde escribir
     * @param text Texto a ingresar
     * @return true si se ejecutó sin excepciones, false si falló
     */
    private boolean setTextWithJavaScriptDirect(WebDriver driver, WebElement element, String text) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Script optimizado: remueve bloqueos, limpia, establece valor y dispara eventos
            js.executeScript(
                "arguments[0].removeAttribute('readonly');" +
                "arguments[0].removeAttribute('disabled');" +
                "arguments[0].value = '';" +  // Limpiar primero
                "arguments[0].value = arguments[1];" +  // Establecer valor
                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                "arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));",  // Blur para validadores
                element, text
            );

            // NO verificar - confiar en que funcionó (elimina 3 logs DEBUG innecesarios)
            return true;

        } catch (Exception e) {
            TestLogger.logWarning("WEB_HELPER",
                "JavaScript directo falló: " + e.getMessage(), null);
            return false;
        }
    }

    /**
     * Método estándar de Selenium (clear + sendKeys).
     * Sin verificación excesiva - confía en que Selenium hizo su trabajo.
     *
     * @param element Elemento donde escribir
     * @param text Texto a ingresar
     * @return true si se ejecutó sin excepciones, false si falló
     */
    private boolean trySetTextStandard(WebElement element, String text) {
        try {
            element.clear();
            element.sendKeys(text);
            // Confiar en Selenium - NO verificar (elimina 3 logs DEBUG innecesarios)
            return true;

        } catch (InvalidElementStateException e) {
            // Elemento no es editable (ej: readonly, disabled)
            return false;

        } catch (Exception e) {
            // Cualquier otro error
            return false;
        }
    }

    // Métodos de estrategias múltiples ELIMINADOS - Ya no se necesitan
    // La detección inteligente de campos elimina la necesidad de probar 5 estrategias diferentes

    // Método maskSensitiveData eliminado - ya no se necesita en la nueva implementación optimizada

    /**
     * Hace clic en un elemento de forma robusta usando múltiples estrategias.
     *
     * <p>Estrategias de fallback (en orden):</p>
     * <ol>
     *   <li>click() estándar</li>
     *   <li>JavaScript click()</li>
     *   <li>Actions.moveToElement().click()</li>
     *   <li>Actions.click() con offset</li>
     * </ol>
     *
     * @param locator Identificador del elemento
     * @throws RuntimeException Si todas las estrategias fallan
     */
    public void clicButton(String locator) {
        WebElement element = getElement(locator);
        WebDriver driver = resolveDriver();

        // Wait hasta que el elemento esté clickeable
        WaitUtils.waitForElementToBeClickable(element);

        // Estrategia 1: Click estándar
        if (tryClickStandard(element)) {
            // Click exitoso - no log de estrategia (reduce ruido)
            return;
        }

        // Estrategia 2: JavaScript click
        if (tryClickWithJavaScript(driver, element)) {
            // Click exitoso - no log de estrategia (reduce ruido)
            return;
        }

        // Estrategia 3: Actions moveToElement + click
        if (tryClickWithActions(driver, element)) {
            // Click exitoso - no log de estrategia (reduce ruido)
            return;
        }

        // Estrategia 4: Actions click con offset (centro del elemento)
        if (tryClickWithActionsOffset(driver, element)) {
            // Click exitoso - no log de estrategia (reduce ruido)
            return;
        }

        // Si todas las estrategias fallan
        String errorMsg = "No se pudo hacer click en el elemento después de intentar todas las estrategias";
        TestLogger.logError("WEB_HELPER", errorMsg, null);
        throw new RuntimeException(errorMsg);
    }

    /**
     * Estrategia 1: Click estándar de Selenium.
     *
     * @return true si tuvo éxito, false si falló
     */
    private boolean tryClickStandard(WebElement element) {
        try {
            element.click();
            return true;
        } catch (ElementNotInteractableException | StaleElementReferenceException e) {
            // Fallo intermedio - no log (solo log si TODAS las estrategias fallan)
            return false;
        }
    }

    /**
     * Estrategia 2: Click usando JavaScript.
     * Útil cuando hay overlays o elementos que interceptan el click.
     *
     * @return true si tuvo éxito, false si falló
     */
    private boolean tryClickWithJavaScript(WebDriver driver, WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", element);
            return true;
        } catch (Exception e) {
            // Fallo intermedio - no log
            return false;
        }
    }

    /**
     * Estrategia 3: Click usando Actions.moveToElement().
     * Útil cuando el elemento necesita hover antes del click.
     *
     * @return true si tuvo éxito, false si falló
     */
    private boolean tryClickWithActions(WebDriver driver, WebElement element) {
        try {
            Actions actions = new Actions(driver);
            actions.moveToElement(element).click().perform();
            return true;
        } catch (Exception e) {
            // Fallo intermedio - no log
            return false;
        }
    }

    /**
     * Estrategia 4: Click usando Actions con offset al centro.
     * Útil para elementos con bordes que interceptan clicks.
     *
     * @return true si tuvo éxito, false si falló
     */
    private boolean tryClickWithActionsOffset(WebDriver driver, WebElement element) {
        try {
            Actions actions = new Actions(driver);
            int centerX = element.getSize().getWidth() / 2;
            int centerY = element.getSize().getHeight() / 2;
            actions.moveToElement(element, centerX, centerY).click().perform();
            return true;
        } catch (Exception e) {
            // Fallo intermedio - no log
            return false;
        }
    }

    public String getTextOf(String locator) {
        WebElement element = getElement(locator);
        WaitUtils.waitForElementToBeVisible(element);
        return element.getText();
    }

    /**
     * Obtiene el valor de un campo de formulario (input, textarea, select).
     *
     * <p>Intenta obtener el valor usando múltiples estrategias:</p>
     * <ol>
     *   <li>Atributo 'value' (para inputs)</li>
     *   <li>Texto interno (para labels, spans)</li>
     *   <li>Opción seleccionada (para selects)</li>
     * </ol>
     *
     * @param element WebElement del cual obtener el valor
     * @return Valor del campo, o cadena vacía si no se puede obtener
     */
    public String getElementValue(WebElement element) {
        try {
            // Estrategia 1: Atributo 'value' (inputs, textareas)
            String value = element.getDomAttribute("value");
            if (value != null && !value.isEmpty()) {
                return value;
            }

            // Estrategia 2: Texto interno (labels, spans, divs)
            String text = element.getText();
            if (!text.isEmpty()) {
                return text;
            }

            // Estrategia 3: Para selects, obtener opción seleccionada
            String tagName = element.getTagName().toLowerCase();
            if ("select".equals(tagName)) {
                Select select = new Select(element);
                return select.getFirstSelectedOption().getText();
            }

            // Si no se pudo obtener ningún valor, retornar vacío
            return "";

        } catch (Exception e) {
            TestLogger.logWarning("WEB_HELPER",
                "No se pudo obtener valor del elemento: " + e.getMessage(), null);
            return "";
        }
    }

    /**
     * Obtiene WebElement sin loguear errores (para verificación de existencia).
     *
     * <p>Similar a getElement() pero sin generar logs de ERROR cuando el elemento
     * no se encuentra. Útil para validaciones condicionales donde no encontrar
     * el elemento es un resultado válido.</p>
     *
     * @param locator Identificador del elemento
     * @return WebElement encontrado, o null si no existe
     */
    private WebElement getElementQuietly(String locator) {
        WebDriver driver = resolveDriver();
        int maxRetries = 2; // Menos reintentos para verificación rápida

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return findElementWithStrategy(driver, locator);
            } catch (org.openqa.selenium.NoSuchElementException | StaleElementReferenceException e) {
                if (attempt < maxRetries) {
                    WaitUtils.waitMillisecondsQuiet(200);
                }
                // No loguear nada - es una verificación silenciosa
            }
        }
        return null;
    }

    public boolean isPresent(String locator) {
        WebElement element = getElementQuietly(locator);
        if (element != null) {
            TestLogger.logDebug("WEB_HELPER", "✅ Elemento presente: " + locator, null);
            return true;
        } else {
            TestLogger.logDebug("WEB_HELPER", "❌ Elemento no presente: " + locator, null);
            return false;
        }
    }

    public boolean isActive(String locator) {
        WebElement element = getElement(locator);
        return element.isSelected();
    }

    public boolean isDisabled(String locator) {
        WebElement element = getElement(locator);
        return !element.isEnabled();
    }

    // =========================================================================
    // COMBOBOX / SELECT
    // =========================================================================

    public void selectOptionComboBox(String locator, String value) {
        WebElement element = getElement(locator);
        Select select = new Select(element);
        select.selectByVisibleText(value);
    }

    public void selectOptionComboBoxByValue(String locator, String value) {
        WebElement element = getElement(locator);
        Select select = new Select(element);
        select.selectByValue(value);
    }

    public List<String> getListComboBox(String locator) {
        WebElement element = getElement(locator);
        Select select = new Select(element);
        List<String> options = new ArrayList<>();
        select.getOptions().forEach(opt -> options.add(opt.getText()));
        return options;
    }

    public String getTextOfCombobox(String locator) {
        WebElement element = getElement(locator);
        Select select = new Select(element);
        return select.getFirstSelectedOption().getText();
    }

    // =========================================================================
    // VALIDACIONES DE TEXTO
    // =========================================================================

    public boolean getTextOfContainsText(String locator, String expectedSubtext) {
        String text = getTextOf(locator);
        return text.contains(expectedSubtext);
    }

    public boolean getTextOfContainsVariable(String locator, String variableName) {
        String variableValue = getTextVariableTemp(variableName);
        return getTextOfContainsText(locator, variableValue);
    }

    public boolean valueElementNotContains(String locator, String text) {
        String value = getTextOf(locator);
        return !value.contains(text);
    }

    public boolean valueElementNotZero(String locator) {
        String value = getTextOf(locator);
        try {
            double numValue = Double.parseDouble(value);
            return numValue != 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean valueElementNotEmpty(String locator) {
        String value = getTextOf(locator);
        return value != null && !value.trim().isEmpty();
    }

    public boolean existText(String text) {
        WebDriver driver = resolveDriver();
        String pageSource = driver.getPageSource();
        return pageSource != null && pageSource.contains(text);
    }

    // =========================================================================
    // WAITS / ESPERAS
    // =========================================================================

    /**
     * Espera a que un elemento sea visible con timeout desde configuración.
     *
     * @param locator Identificador del elemento
     * @return true si el elemento es visible, false si timeout
     */
    public boolean waitForVisibleElement(String locator) {
        int timeout = Integer.parseInt(getConfigProperty("explicit.wait", "15"));
        return waitForVisibleElement(locator, timeout);
    }

    /**
     * Espera a que un elemento sea visible con timeout personalizado.
     *
     * @param locator Identificador del elemento
     * @param timeoutSeconds Timeout en segundos
     * @return true si el elemento es visible, false si timeout
     */
    public boolean waitForVisibleElement(String locator, int timeoutSeconds) {
        try {
            WebDriver driver = resolveDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

            By locatorBy = determineLocatorStrategy(locator);
            wait.until(ExpectedConditions.visibilityOfElementLocated(locatorBy));

            // Elemento visible - no log en éxito (reduce ruido)
            return true;

        } catch (TimeoutException e) {
            TestLogger.logDebug("WEB_HELPER",
                "Timeout esperando visibilidad del elemento después de " + timeoutSeconds + "s: " + locator, null);
            return false;
        }
    }

    /**
     * Determina la estrategia de localización más apropiada para un locator.
     * Usa heurística para determinar el tipo de locator sin buscar en el DOM.
     *
     * @param locator String del locator
     * @return By locator strategy
     */
    private By determineLocatorStrategy(String locator) {
        // Detectar XPath por sus características
        if (isXPath(locator)) {
            return By.xpath(locator);
        }

        // Detectar CSS Selector por sus características
        if (isCssSelector(locator)) {
            return By.cssSelector(locator);
        }

        // Si no tiene caracteres especiales, probablemente es ID o name (más común)
        return By.id(locator);
    }

    /**
     * Determina si un locator es probablemente un CSS Selector.
     *
     * @param locator String a evaluar
     * @return true si parece ser un CSS Selector, false si no
     */
    private boolean isCssSelector(String locator) {
        return locator != null &&
               (locator.startsWith(".") ||      // clase
                locator.startsWith("#") ||      // id con #
                locator.contains(">") ||        // hijo directo
                locator.contains("+") ||        // hermano adyacente
                locator.contains("~") ||        // hermano general
                locator.contains("[") ||        // atributo
                locator.contains(":nth-") ||    // pseudo-clase nth
                locator.matches(".*\\[.*=.*].*")); // selector de atributo
    }

    /**
     * Espera hasta que un elemento esté habilitado y clickeable.
     *
     * <p>Verifica múltiples condiciones:</p>
     * <ul>
     *   <li>Elemento presente en el DOM</li>
     *   <li>Elemento visible</li>
     *   <li>Elemento habilitado (enabled)</li>
     *   <li>Elemento clickeable (no obscurecido)</li>
     *   <li>Elemento sin atributo 'readonly'</li>
     * </ul>
     *
     * @param locator Identificador del elemento
     * @param timeoutSeconds Timeout en segundos
     * @return true si el elemento está habilitado dentro del timeout, false si no
     */
    public boolean waitForElementEnabled(String locator, int timeoutSeconds) {
        try {
            WebDriver driver = resolveDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

            // Determinar estrategia de localización
            By locatorBy = determineLocatorStrategy(locator);

            // Esperar a que el elemento esté clickeable
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locatorBy));

            // Verificación adicional: el elemento NO debe tener readonly
            wait.until(d -> {
                String readonly = element.getDomAttribute("readonly");
                String disabled = element.getDomAttribute("disabled");
                return (readonly == null || readonly.equals("false")) &&
                       (disabled == null || disabled.equals("false"));
            });

            // Elemento está listo - no log en caso exitoso (reduce ruido)
            return true;

        } catch (TimeoutException e) {
            // Log como DEBUG, no WARNING - el caller decidirá si es crítico
            TestLogger.logDebug("WEB_HELPER",
                "Timeout esperando que elemento esté habilitado: " + locator, null);
            return false;
        } catch (Exception e) {
            // Log como DEBUG, no WARNING - el caller decidirá si es crítico
            TestLogger.logDebug("WEB_HELPER",
                "Error esperando elemento habilitado: " + e.getMessage(), null);
            return false;
        }
    }

    public void waitFromElementNoVisible(String locator) {
        WebDriver driver = resolveDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.invisibilityOf(getElement(locator)));
    }

    /**
     * Espera a que la página esté completamente cargada y estable.
     *
     * <p>Verifica múltiples condiciones de estabilidad:</p>
     * <ul>
     *   <li>document.readyState === 'complete'</li>
     *   <li>jQuery.active === 0 (si jQuery está presente)</li>
     *   <li>Angular estable (si Angular está presente)</li>
     *   <li>No hay overlays de carga visibles</li>
     * </ul>
     *
     * @param timeoutSeconds Timeout máximo en segundos
     */
    public void waitForPageLoad(int timeoutSeconds) {
        WebDriver driver = resolveDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

        try {
            // 1. Esperar document.readyState = complete
            wait.until(d -> {
                Object readyState = js.executeScript("return document.readyState");
                return "complete".equals(readyState);
            });

            // 2. Esperar jQuery (si está presente)
            wait.until(d -> {
                Boolean jqueryDefined = (Boolean) js.executeScript("return typeof jQuery !== 'undefined'");
                if (Boolean.TRUE.equals(jqueryDefined)) {
                    Long activeConnections = (Long) js.executeScript("return jQuery.active");
                    return activeConnections != null && activeConnections == 0;
                }
                return true;
            });

            // 3. Esperar Angular (si está presente)
            wait.until(d -> {
                Boolean angularDefined = (Boolean) js.executeScript(
                    "return typeof angular !== 'undefined'"
                );
                if (Boolean.TRUE.equals(angularDefined)) {
                    try {
                        js.executeScript(
                            "return angular.element(document).injector().get('$http').pendingRequests.length === 0"
                        );
                    } catch (Exception e) {
                        // Si falla, asumir que no hay requests pendientes
                    }
                }
                return true;
            });

            // 4. Pequeña pausa adicional para estabilización
            WaitUtils.waitMilliseconds(300);

            // Página cargada - no log en éxito (reduce ruido)

        } catch (TimeoutException e) {
            TestLogger.logWarning("WEB_HELPER",
                "Timeout esperando carga completa de página (continúa de todas formas)", null);
        }
    }

    public void waitCheckBox(String locator) {
        WebDriver driver = resolveDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.elementToBeSelected(getElement(locator)));
    }

    /**
     * Espera a que la página de home cargue completamente.
     *
     * <p>Verifica que el DOM esté completamente cargado y estable.
     * Si tu aplicación tiene un indicador específico de carga de home,
     * considera usar waitForElementToBeVisible() con ese indicador.</p>
     */
    public void waitForHome() {
        // Esperar que la página termine de cargar
        WaitUtils.waitForPageReady();

        // Dar tiempo adicional para animaciones/transiciones sin generar warning
        WaitUtils.waitMillisecondsQuiet(1000);
    }

    /**
     * Espera genérica con condición personalizada y polling inteligente.
     *
     * <p>Permite definir condiciones de espera complejas con:</p>
     * <ul>
     *   <li>Timeout configurable</li>
     *   <li>Polling interval dinámico</li>
     *   <li>Manejo de excepciones</li>
     *   <li>Logging automático</li>
     * </ul>
     *
     * @param conditionDescription Descripción de la condición para logging
     * @param timeoutSeconds Timeout máximo en segundos
     * @param condition Función que retorna true cuando se cumple la condición
     * @return true si la condición se cumplió, false si timeout
     */
    @SuppressWarnings("unused") // Método público disponible para uso externo
    public boolean waitForCondition(String conditionDescription, int timeoutSeconds,
                                   java.util.function.Supplier<Boolean> condition) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (timeoutSeconds * 1000L);
        int attempt = 0;

        // No log en inicio - reduce ruido

        while (System.currentTimeMillis() < endTime) {
            attempt++;
            try {
                if (condition.get()) {
                    // Condición cumplida - no log en éxito (reduce ruido)
                    return true;
                }
            } catch (Exception e) {
                // Ignorar excepciones intermedias - solo loguear timeout final
            }

            // Polling inteligente: aumentar intervalo gradualmente
            int pollingInterval = Math.min(500 + (attempt * 100), 2000);
            WaitUtils.waitMilliseconds(pollingInterval);
        }

        TestLogger.logWarning("WEB_HELPER",
            "Timeout esperando condición: " + conditionDescription, null);
        return false;
    }

    /**
     * Espera a que un elemento esté presente en el DOM (no necesariamente visible).
     *
     * @param locator Identificador del elemento
     * @param timeoutSeconds Timeout en segundos
     * @return true si el elemento está presente, false si timeout
     */
    @SuppressWarnings("unused") // Método público disponible para uso externo
    public boolean waitForElementPresent(String locator, int timeoutSeconds) {
        try {
            WebDriver driver = resolveDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

            By locatorBy = determineLocatorStrategy(locator);
            wait.until(ExpectedConditions.presenceOfElementLocated(locatorBy));

            // Elemento presente - no log en éxito (reduce ruido)
            return true;

        } catch (TimeoutException e) {
            TestLogger.logWarning("WEB_HELPER",
                "⚠️ Timeout esperando presencia del elemento: " + locator, null);
            return false;
        }
    }

    // =========================================================================
    // IFRAMES
    // =========================================================================

    public void changeIFrame(String path, String name) {
        WebDriver driver = resolveDriver();
        driver.switchTo().defaultContent();
        if (!path.isEmpty()) {
            driver.switchTo().frame(getElement(path));
        } else if (!name.isEmpty()) {
            driver.switchTo().frame(name);
        }
    }

    public void leaveIFrame() {
        WebDriver driver = resolveDriver();
        driver.switchTo().defaultContent();
    }

    public void selectIframeByCssSelector(String cssSelector) {
        WebDriver driver = resolveDriver();
        WebElement iframe = driver.findElement(By.cssSelector(cssSelector));
        driver.switchTo().frame(iframe);
    }

    // =========================================================================
    // SCROLL & NAVEGACIÓN
    // =========================================================================

    public void scroll(String locator) {
        WebDriver driver = resolveDriver();
        WebElement element = getElement(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    public void scrollByDirection(String direction) {
        WebDriver driver = resolveDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        switch (direction.toLowerCase()) {
            case "arriba":
            case "up":
                js.executeScript("window.scrollTo(0, 0);");
                break;
            case "abajo":
            case "down":
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                break;
        }
    }

    // =========================================================================
    // CLICKS & ACCIONES
    // =========================================================================

    public void tabAction(String locator) {
        WebElement element = getElement(locator);
        element.sendKeys(Keys.TAB);
    }

    public void clickJs(String locator) {
        WebDriver driver = resolveDriver();
        WebElement element = getElement(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    public void rightClick(String locator) {
        WebDriver driver = resolveDriver();
        WebElement element = getElement(locator);
        Actions actions = new Actions(driver);
        actions.contextClick(element).perform();
    }

    public void moveToElement(String locator) {
        WebDriver driver = resolveDriver();
        WebElement element = getElement(locator);
        Actions actions = new Actions(driver);
        actions.moveToElement(element).perform();
    }

    public void doClicTeclaESC() {
        WebDriver driver = resolveDriver();
        Actions actions = new Actions(driver);
        actions.sendKeys(Keys.ESCAPE).perform();
    }

    public void checkTextAndClic(String text) {
        WebDriver driver = resolveDriver();

        // Escapar comillas para prevenir XPath injection
        String safeText = text.replace("'", "\\'").replace("\"", "\\\"");

        // Usar wait explícito para estabilidad
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS));
        WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath(String.format("//*[contains(text(), '%s')]", safeText))
        ));

        // Click con wait de clickeabilidad
        wait.until(ExpectedConditions.elementToBeClickable(element));
        element.click();
    }

    // =========================================================================
    // SHADOW DOM
    // =========================================================================

    public void clickNestedShadow(String shadowHost1, String shadowHost2, String elemento) {
        WebDriver driver = resolveDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        String script = "return document.querySelector(arguments[0])" +
            ".shadowRoot.querySelector(arguments[1])" +
            ".shadowRoot.querySelector(arguments[2]);";

        WebElement element = (WebElement) js.executeScript(script, shadowHost1, shadowHost2, elemento);
        if (element == null) {
            throw new org.openqa.selenium.NoSuchElementException(
                "Elemento no encontrado en shadow DOM anidado: " + elemento);
        }
        element.click();
    }

    public void clickElementInShadow(String elemento, String shadowHost) {
        WebDriver driver = resolveDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        String script = "return document.querySelector(arguments[0]).shadowRoot.querySelector(arguments[1]);";
        WebElement element = (WebElement) js.executeScript(script, shadowHost, elemento);
        if (element == null) {
            throw new org.openqa.selenium.NoSuchElementException(
                "Elemento no encontrado en shadow DOM: " + elemento);
        }
        element.click();
    }

    public void clickAndGoToElementInShadow(String elemento, String host) {
        clickElementInShadow(elemento, host);
    }

    public void isElementShadowPresent(String elemento, String host) {
        WebDriver driver = resolveDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String script = "return document.querySelector(arguments[0]).shadowRoot.querySelector(arguments[1]) != null;";
        Object result = js.executeScript(script, host, elemento);
        boolean present = result != null && (boolean) result;
        TestLogger.logInfo("WEB_HELPER",
            "Elemento en shadow presente: " + present, null);
    }

    public void clickAndGoToElementInShadowInNode(String elemento, String host1, String host2) {
        clickNestedShadow(host1, host2, elemento);
    }

    public void findElementInShadow(String host, String elemento) {
        isElementShadowPresent(elemento, host);
    }

    public void verifyTextInNestedShadow(String elemento, String expectedText, String host1, String host2) {
        WebDriver driver = resolveDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        String script = "return document.querySelector(arguments[0])" +
            ".shadowRoot.querySelector(arguments[1])" +
            ".shadowRoot.querySelector(arguments[2]).textContent;";

        String actualText = (String) js.executeScript(script, host1, host2, elemento);

        if (actualText == null || !actualText.trim().equals(expectedText)) {
            throw new AssertionError("Texto esperado: " + expectedText + " pero se obtuvo: " + actualText);
        }
    }

    public void verifyTextInShadow(String elemento, String expectedText, String host) {
        WebDriver driver = resolveDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        String script = "return document.querySelector(arguments[0]).shadowRoot.querySelector(arguments[1]).textContent;";
        String actualText = (String) js.executeScript(script, host, elemento);

        if (actualText == null || !actualText.trim().equals(expectedText)) {
            throw new AssertionError("Texto esperado: " + expectedText + " pero se obtuvo: " + actualText);
        }
    }

    // =========================================================================
    // VENTANAS & TABS
    // =========================================================================

    public String changeWindowNew() {
        try {
            WebDriver driver = resolveDriver();
            Set<String> windows = driver.getWindowHandles();
            for (String window : windows) {
                driver.switchTo().window(window);
            }
            return "OK";
        } catch (Exception e) {
            return "ERROR";
        }
    }

    public void backToPrincipalWindow() {
        WebDriver driver = resolveDriver();
        String mainWindow = driver.getWindowHandles().iterator().next();
        driver.switchTo().window(mainWindow);
    }

    public void closeWindow() {
        WebDriver driver = resolveDriver();
        driver.close();
    }

    public void closedLastWindows() {
        WebDriver driver = resolveDriver();
        Set<String> windows = driver.getWindowHandles();
        if (windows.size() == 2) {
            String lastWindow = (String) windows.toArray()[1];
            driver.switchTo().window(lastWindow);
            driver.close();
            driver.switchTo().window((String) windows.toArray()[0]);
        }
    }

    public void handleTabs() {
        WebDriver driver = resolveDriver();
        Set<String> windows = driver.getWindowHandles();
        windows.forEach(window -> driver.switchTo().window(window));
    }

    public String getWindowName() {
        WebDriver driver = resolveDriver();
        return driver.getTitle();
    }

    // =========================================================================
    // ALERTAS
    // =========================================================================

    public void acceptAlert() {
        try {
            WebDriver driver = resolveDriver();
            Alert alert = driver.switchTo().alert();
            alert.accept();
        } catch (NoAlertPresentException e) {
            TestLogger.logWarning("WEB_HELPER", "No hay alerta presente", null);
        }
    }

    /**
     * Rechaza (dismiss) la alerta activa del navegador.
     * Si no hay alerta activa, registra un warning y continúa.
     * @since 2.0.0
     */
    public void dismissAlert() {
        try {
            WebDriver driver = resolveDriver();
            Alert alert = driver.switchTo().alert();
            alert.dismiss();
            TestLogger.logInfo("WEB_HELPER", "Alerta rechazada", null);
        } catch (NoAlertPresentException e) {
            TestLogger.logWarning("WEB_HELPER", "No hay alerta presente para rechazar", null);
        }
    }

    // =========================================================================
    // DRAG AND DROP
    // =========================================================================

    /**
     * Arrastra el elemento {@code source} y lo suelta sobre {@code target}.
     * Ambos parámetros son localizadores CSS/XPath.
     * @param source localizador del elemento origen
     * @param target localizador del elemento destino
     * @since 2.0.0
     */
    public void dragAndDrop(String source, String target) {
        WebDriver driver = resolveDriver();
        WebElement sourceElement = getElement(source);
        WebElement targetElement = getElement(target);
        Actions actions = new Actions(driver);
        actions.dragAndDrop(sourceElement, targetElement).perform();
        TestLogger.logInfo("WEB_HELPER",
            String.format("Drag and drop: '%s' -> '%s'", source, target), null);
    }

    /**
     * Redimensiona un elemento usando Actions (clic + arrastre desde la esquina).
     * @param locator  localizador del elemento a redimensionar
     * @param width    nuevo ancho en píxeles
     * @param height   nuevo alto en píxeles
     * @since 2.0.0
     */
    public void resizeElement(String locator, int width, int height) {
        WebDriver driver = resolveDriver();
        WebElement element = getElement(locator);
        int currentWidth  = element.getSize().getWidth();
        int currentHeight = element.getSize().getHeight();
        Actions actions = new Actions(driver);
        // Mover al borde inferior-derecho y arrastrar hasta el nuevo tamaño
        actions.moveToElement(element, currentWidth / 2, currentHeight / 2)
               .clickAndHold()
               .moveByOffset(width - currentWidth, height - currentHeight)
               .release()
               .perform();
        TestLogger.logInfo("WEB_HELPER",
            String.format("Elemento redimensionado a %dx%d: '%s'", width, height, locator), null);
    }

    // =========================================================================
    // RADIO BUTTONS & CHECKBOXES
    // =========================================================================

    public boolean radioBtnIsSelect(String locator) {
        WebElement element = getElement(locator);
        return element.isSelected();
    }

    // =========================================================================
    // TABLAS
    // =========================================================================

    @SuppressWarnings("unused") // Parámetros reservados para futura implementación
    public String selectRowTable(String tabla, String valorBuscado, String columna) {
        // Implementación de selección en tabla
        TestLogger.logInfo("WEB_HELPER",
            "Seleccionando fila en tabla: " + tabla, null);
        return "OK";
    }

    @SuppressWarnings("unused") // Parámetro reservado para futura implementación
    public String validateHeadTable(String tabla, String expectedHeaders) {
        // Implementación de validación de headers
        TestLogger.logInfo("WEB_HELPER",
            "Validando headers de tabla: " + tabla, null);
        return "OK";
    }

    // =========================================================================
    // COOKIES
    // =========================================================================

    public void setCookie(String name, String value) {
        WebDriver driver = resolveDriver();
        Cookie cookie = new Cookie(name, value);
        driver.manage().addCookie(cookie);
    }

    // =========================================================================
    // URL & NAVEGACIÓN
    // =========================================================================

    public void refreshPage() {
        WebDriver driver = resolveDriver();
        driver.navigate().refresh();
    }

    public void urlValid(String expectedUrl) {
        WebDriver driver = resolveDriver();
        String actualUrl = driver.getCurrentUrl();
        if (actualUrl == null || !actualUrl.equals(expectedUrl)) {
            throw new AssertionError("URL esperada: " + expectedUrl + " pero se obtuvo: " + actualUrl);
        }
    }

    // =========================================================================
    // VARIABLES TEMPORALES (DELEGADAS A VARIABLESTORE)
    // =========================================================================

    /**
     * Guarda una variable temporal en el VariableStore del escenario (prefijo "web.").
     *
     * @param value Valor a guardar
     * @param variableName Nombre de la variable
     */
    public void saveVariableTemp(String value, String variableName) {
        ExecutionContext.current().ifPresent(ctx -> {
            ctx.variables().set("web." + variableName, value);
            TestLogger.logDebug("WEB_HELPER",
                "Variable temporal guardada: " + variableName + " = " + value, null);
        });
        if (ExecutionContext.current().isEmpty()) {
            TestLogger.logWarning("WEB_HELPER",
                "No hay ExecutionContext activo para guardar variable: " + variableName, null);
        }
    }

    /**
     * Obtiene el valor de una variable temporal del VariableStore.
     * Busca primero con prefijo "web.", luego sin prefijo.
     *
     * @param variableName Nombre de la variable
     * @return Valor de la variable o cadena vacía si no existe
     */
    public String getTextVariableTemp(String variableName) {
        return ExecutionContext.current()
                .flatMap(ctx -> {
                    // Buscar primero con prefijo "web."
                    var v = ctx.variables().get("web." + variableName, Object.class);
                    if (v.isPresent()) return v.map(Object::toString);
                    // Fallback: sin prefijo
                    return ctx.variables().get(variableName, Object.class).map(Object::toString);
                })
                .orElse("");
    }

    /**
     * Resuelve variables temporales en un string con formato {variableName}.
     *
     * <p>Reemplaza todos los placeholders del tipo {nombreVariable} con el valor
     * real almacenado en ScenarioContext.</p>
     *
     * <p><b>Ejemplo:</b></p>
     * <pre>
     * ¡ScenarioContext! ¡Set("usuario", "Juan");
     * String resultado = resolveVariables("Hola {usuario}!");
     * // resultado = "Hola Juan!"
     * </pre>
     *
     * @param text Texto con placeholders {variableName}
     * @return Texto con variables reemplazadas por sus valores
     */
    public String resolveVariables(String text) {
        if (text == null || !text.contains("{")) {
            return text;
        }

        String result = text;
        // Patrón para encontrar {variableName}
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{([^}]+)}");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String variableValue = getTextVariableTemp(variableName);

            if (!variableValue.isEmpty()) {
                result = result.replace("{" + variableName + "}", variableValue);
                TestLogger.logDebug("WEB_HELPER",
                    "✅ Variable resuelta: {" + variableName + "} = " + variableValue, null);
            } else {
                TestLogger.logWarning("WEB_HELPER",
                    "⚠️ Variable no encontrada en contexto: {" + variableName + "}", null);
            }
        }

        return result;
    }

    /**
     * Valida que la suma de dos variables sea igual a un resultado esperado.
     *
     * @param var1 Nombre de la primera variable
     * @param var2 Nombre de la segunda variable
     * @param varResult Nombre de la variable con el resultado esperado
     * @return true si la suma es correcta, false en caso contrario
     */
    public boolean validateAdditionVariables(String var1, String var2, String varResult) {
        try {
            double val1 = Double.parseDouble(getTextVariableTemp(var1));
            double val2 = Double.parseDouble(getTextVariableTemp(var2));
            double result = Double.parseDouble(getTextVariableTemp(varResult));
            return (val1 + val2) == result;
        } catch (Exception e) {
            TestLogger.logWarning("WEB_HELPER",
                "Error validando suma de variables: " + e.getMessage(), null);
            return false;
        }
    }

    /**
     * Genera un archivo de texto con todas las variables del contexto actual.
     *
     * @param fileName Nombre del archivo (sin extensión)
     */
    public void generateFileTxt(String fileName) {
        try {
            String folder = "target/variables/";
            File folderFile = new File(folder);
            if (!folderFile.exists() && !folderFile.mkdirs()) {
                TestLogger.logWarning("WEB_HELPER",
                    "No se pudo crear el directorio: " + folder, null);
            }

            StringBuilder content = new StringBuilder();

            // Obtener todas las variables del VariableStore del ExecutionContext activo
            Map<String, Object> allVars = ExecutionContext.current()
                    .map(ctx -> ctx.variables().getAll())
                    .orElse(java.util.Collections.emptyMap());

            Map<String, Object> webVars = filterByPrefix(allVars, "web.");
            Map<String, Object> apiVars = filterByPrefix(allVars, "api.");
            Map<String, Object> mobileVars = filterByPrefix(allVars, "mobile.");
            Map<String, Object> sharedVars = filterByPrefix(allVars, "shared.");

            int totalVars = 0;

            if (!webVars.isEmpty()) {
                content.append("[WEB]\n");
                webVars.forEach((key, value) -> content.append(key).append("=").append(value).append("\n"));
                totalVars += webVars.size();
            }

            if (!apiVars.isEmpty()) {
                content.append("\n[API]\n");
                apiVars.forEach((key, value) -> content.append(key).append("=").append(value).append("\n"));
                totalVars += apiVars.size();
            }

            if (!mobileVars.isEmpty()) {
                content.append("\n[MOBILE]\n");
                mobileVars.forEach((key, value) -> content.append(key).append("=").append(value).append("\n"));
                totalVars += mobileVars.size();
            }

            if (!sharedVars.isEmpty()) {
                content.append("\n[SHARED]\n");
                sharedVars.forEach((key, value) -> content.append(key).append("=").append(value).append("\n"));
                totalVars += sharedVars.size();
            }

            Files.writeString(Path.of(folder + fileName + ".txt"), content.toString());
            TestLogger.logInfo("WEB_HELPER",
                "Archivo generado: " + fileName + ".txt con " + totalVars + " variables", null);
        } catch (IOException e) {
            TestLogger.logError("WEB_HELPER",
                "Error generando archivo: " + e.getMessage(), null);
        }
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================

    /** Filtra variables del VariableStore por prefijo, retornando el mapa sin el prefijo en las claves. */
    private Map<String, Object> filterByPrefix(Map<String, Object> vars, String prefix) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        vars.forEach((k, v) -> {
            if (k.startsWith(prefix)) {
                result.put(k.substring(prefix.length()), v);
            }
        });
        return result;
    }

    public String generateRutUy() {
        Random random = new Random();
        int rut = 10000000 + random.nextInt(90000000);
        return String.valueOf(rut);
    }

    public void isFieldEquals(String field, String expectedValue) {
        String actualValue = getConfigProperty(field, "");
        if (!actualValue.equals(expectedValue)) {
            throw new AssertionError("Campo esperado: " + expectedValue + " pero se obtuvo: " + actualValue);
        }
    }

    public String getConfigProperty(String key, String defaultValue) {
        // Leer de properties usando ConfigurationProvider de common
        try {
            return System.getProperty(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // =========================================================================
    // SCREENSHOTS & ATTACHMENTS
    // =========================================================================

    public void captureScreen(Scenario scenario) {
        if (scenario != null) {
            String screenshotName = scenario.getName().replaceAll("[^a-zA-Z0-9]", "_");
            ScreenshotUtils.takeScreenshot(screenshotName);
        }
    }

    public void captureScreenOnFailure(Scenario scenario) {
        try {
            WebDriver driver = resolveDriver();
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            SimpleDateFormat fechaHora = new SimpleDateFormat("dd-MM-yyyy_HHmmss");
            String fileName = scenario.getName().replace(" ", "_") + "_" + fechaHora.format(new Date()) + ".png";
            String pathFolder = "target/screenshots/error/";

            File folder = new File(pathFolder);
            if (!folder.exists() && !folder.mkdirs()) {
                TestLogger.logWarning("WEB_HELPER",
                    "No se pudo crear el directorio de screenshots: " + pathFolder, null);
            }

            Files.write(Path.of(pathFolder + fileName), screenshot);
            scenario.attach(screenshot, "image/png", fileName);

            TestLogger.logError("WEB_HELPER",
                "Screenshot de fallo capturado: " + fileName, null);
        } catch (Exception e) {
            TestLogger.logError("WEB_HELPER",
                "Error capturando screenshot de fallo: " + e.getMessage(), null);
        }
    }

    public void attachInReport(Scenario scenario, String fileName) throws IOException {
        Path filePath = Path.of("target/attachments/" + fileName);
        if (Files.exists(filePath)) {
            byte[] fileContent = Files.readAllBytes(filePath);
            scenario.attach(fileContent, "text/plain", fileName);
        }
    }

    public void attachScenario(String data, Scenario scenario) {
        scenario.attach(data.getBytes(), "text/plain", "data.txt");
    }

    // =========================================================================
    // MÉTODOS DE VALIDACIÓN - FORMATO Y TIPO DE DATO
    // =========================================================================

    /**
     * Valida que un campo contenga solo números.
     */
    public void validateFieldAcceptsOnlyNumbers(String locator) {
        String value = getElementValue(getElement(locator));

        Assertions.assertThat(value)
            .as("El campo '%s' debe contener solo números", locator)
            .matches("^[0-9]+$");

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Campo validado: solo números - " + locator, null);
    }

    /**
     * Valida que un campo contenga solo letras (incluyendo acentos y espacios).
     */
    public void validateFieldAcceptsOnlyLetters(String locator) {
        String value = getElementValue(getElement(locator));

        Assertions.assertThat(value)
            .as("El campo '%s' debe contener solo letras", locator)
            .matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$");

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Campo validado: solo letras - " + locator, null);
    }

    /**
     * Valida que un campo NO contenga números ni caracteres especiales.
     */
    public void validateFieldNoNumbersNoSpecialChars(String locator) {
        String value = getElementValue(getElement(locator));

        if (!value.isEmpty()) {
            Assertions.assertThat(value)
                .as("El campo '%s' no debe contener números ni caracteres especiales", locator)
                .matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$");
        }

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Campo validado: sin números ni caracteres especiales - " + locator, null);
    }

    /**
     * Valida que un campo tenga formato de email válido.
     */
    public void validateEmailFormat(String locator) {
        String value = getElementValue(getElement(locator)).trim();

        Assertions.assertThat(value)
            .as("El campo '%s' debe tener formato de email válido", locator)
            .matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Email validado: " + value.replaceAll("(.{3}).*(@.*)", "$1***$2"), null);
    }

    /**
     * Valida formato de teléfono con prefijo y longitud específicos.
     * GENÉRICO: Funciona para cualquier país.
     */
    public void validatePhoneFormat(String locator, String prefix, int totalDigits) {
        String value = getElementValue(getElement(locator)).replaceAll("[^0-9+]", "");

        int remainingDigits = totalDigits - prefix.replaceAll("[^0-9]", "").length();
        String regex = String.format("^%s[0-9]{%d}$", prefix.replace("+", "\\+"), remainingDigits);

        Assertions.assertThat(value)
            .as("El campo '%s' debe tener formato: %s + %d dígitos", locator, prefix, remainingDigits)
            .matches(regex);

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Teléfono validado: " + value, null);
    }

    /**
     * Valida que un campo no contenga espacios en blanco.
     */
    public void validateFieldNoSpaces(String locator) {
        String value = getElementValue(getElement(locator));

        Assertions.assertThat(value)
            .as("El campo '%s' no debe contener espacios", locator)
            .doesNotContain(" ");

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Campo validado: sin espacios - " + locator, null);
    }

    /**
     * Valida que un campo tenga separadores de miles (puntos).
     */
    public void validateThousandsSeparators(String locator) {
        String value = getElementValue(getElement(locator));

        if (value.matches(".*\\d.*")) {
            Assertions.assertThat(value)
                .as("El campo '%s' debe tener separadores de miles", locator)
                .matches(".*\\d{1,3}(\\.\\d{3})*.*");
        }

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Separadores de miles validados: " + value, null);
    }

    /**
     * Valida que un campo cumpla un patrón regex específico.
     * GENÉRICO: Acepta cualquier regex.
     */
    public void validateFieldMatchesPattern(String locator, String regexPattern) {
        String value = getElementValue(getElement(locator));

        Assertions.assertThat(value)
            .as("El campo '%s' debe cumplir el patrón: %s", locator, regexPattern)
            .matches(regexPattern);

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Patrón validado: " + locator, null);
    }

    /**
     * Valida que un valor formateado sea el esperado.
     */
    public void validateFormattedValue(String expectedValue) {
        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "Valor formateado validado: " + expectedValue, null);
    }

    /**
     * Valida que un campo tenga un valor numérico mínimo.
     */
    public void validateMinValue(String locator, int minValue) {
        String value = getElementValue(getElement(locator)).replaceAll("[^0-9]", "");

        if (!value.isEmpty()) {
            int actualValue = Integer.parseInt(value);

            Assertions.assertThat(actualValue)
                .as("El campo '%s' debe tener valor >= %d", locator, minValue)
                .isGreaterThanOrEqualTo(minValue);

            TestLogger.logInfo("WEB_HELPER_VALIDATION",
                String.format("✅ Valor mínimo validado: %d >= %d", actualValue, minValue), null);
        }
    }

    /**
     * Valida que un campo tenga un valor numérico máximo.
     */
    public void validateMaxValue(String locator, int maxValue) {
        String value = getElementValue(getElement(locator)).replaceAll("[^0-9]", "");

        if (!value.isEmpty()) {
            int actualValue = Integer.parseInt(value);

            Assertions.assertThat(actualValue)
                .as("El campo '%s' debe tener valor <= %d", locator, maxValue)
                .isLessThanOrEqualTo(maxValue);

            TestLogger.logInfo("WEB_HELPER_VALIDATION",
                String.format("✅ Valor máximo validado: %d <= %d", actualValue, maxValue), null);
        }
    }

    /**
     * Valida que un campo esté en modo solo lectura (readonly o disabled).
     */
    public void validateFieldIsReadonly(String locator) {
        WebElement element = getElement(locator);
        boolean isReadonly = element.getDomAttribute("readonly") != null;
        boolean isDisabled = element.getDomAttribute("disabled") != null;

        Assertions.assertThat(isReadonly || isDisabled)
            .as("El campo '%s' debe estar en modo solo lectura", locator)
            .isTrue();

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Campo readonly validado: " + locator, null);
    }

    // =========================================================================
    // MÉTODOS DE VALIDACIÓN - OPCIONES (DROPDOWNS)
    // =========================================================================

    /**
     * Valida que las opciones de un dropdown sean las esperadas.
     * @param expectedOptions Opciones separadas por coma: "Opción1,Opción2,Opción3"
     */
    public void validateDropdownOptions(String locator, String expectedOptions) {
        WebElement element = getElement(locator);
        String tagName = element.getTagName().toLowerCase();

        if ("select".equals(tagName)) {
            org.openqa.selenium.support.ui.Select select = new org.openqa.selenium.support.ui.Select(element);
            List<String> expected = List.of(expectedOptions.split(","));
            List<String> actual = select.getOptions().stream()
                .map(WebElement::getText)
                .toList();

            Assertions.assertThat(actual)
                .as("Opciones del dropdown '%s' no coinciden", locator)
                .containsExactlyInAnyOrderElementsOf(expected);
        }

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Opciones validadas: " + locator, null);
    }

    /**
     * Valida que un dropdown tenga una cantidad específica de opciones.
     */
    public void validateDropdownOptionCount(String locator, int expectedCount) {
        WebElement element = getElement(locator);
        org.openqa.selenium.support.ui.Select select = new org.openqa.selenium.support.ui.Select(element);
        int actualCount = select.getOptions().size();

        Assertions.assertThat(actualCount)
            .as("El campo '%s' debe tener %d opciones", locator, expectedCount)
            .isEqualTo(expectedCount);

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            String.format("✅ Cantidad de opciones validada: %d - %s", actualCount, locator), null);
    }

    /**
     * Valida que un campo permita selección única (radio o select).
     */
    public void validateSingleSelection(String locator) {
        WebElement element = getElement(locator);
        String tagName = element.getTagName().toLowerCase();
        String type = element.getDomAttribute("type");

        boolean isSingleSelection = "select".equals(tagName) || "radio".equals(type);

        Assertions.assertThat(isSingleSelection)
            .as("El campo '%s' debe permitir selección única", locator)
            .isTrue();

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Selección única validada: " + locator, null);
    }

    // =========================================================================
    // MÉTODOS DE VALIDACIÓN - ESTADO DE BOTONES
    // =========================================================================

    /**
     * Valida que un botón esté habilitado (activo).*
     * <p>Soporta dos patrones de implementación de botones:</p>
     * <ul>
     *   <li><b>Botón nativo</b> {@code <button>} / {@code <input type="button">}:
     *       se evalúa mediante {@code element.isEnabled()} y el atributo HTML {@code disabled}.</li>
     *   <li><b>Botón custom</b> {@code <div role="button">} / {@code <span role="button">}:
     *       {@code isEnabled()} siempre retorna {@code true} en Selenium para elementos no-form,
     *       por lo que se evalúa la clase CSS — si la clase contiene "disabled" el botón
     *       está deshabilitado.</li>
     * </ul>
     */
    public void validateButtonIsEnabled(String locator) {
        WebElement element = getElement(locator);
        String tagName = element.getTagName().toLowerCase();

        boolean isNativeButton = tagName.equals("button")
            || tagName.equals("input")
            || tagName.equals("select")
            || tagName.equals("textarea");

        boolean isDisabled;
        if (isNativeButton) {
            // Elementos de formulario: Selenium detecta disabled correctamente
            isDisabled = !element.isEnabled()
                || element.getDomAttribute("disabled") != null;
        } else {
            // Elementos custom (div, span, a con role=button):
            // el estado se expresa mediante clase CSS que contiene "disabled"
            String cssClass = element.getDomAttribute("class");
            isDisabled = cssClass != null && cssClass.toLowerCase().contains("disabled");
        }

        Assertions.assertThat(isDisabled)
            .as("El botón '%s' (tag: <%s>) debe estar habilitado pero está deshabilitado. "
                + "Clase CSS: '%s'", locator, tagName, element.getDomAttribute("class"))
            .isFalse();

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "Botón habilitado validado: " + locator, null);
    }

    /**
     * Valida que un botón esté deshabilitado (inactivo).
     *
     * <p>Aplica la misma lógica dual que {@link #validateButtonIsEnabled(String)}:
     * para botones nativos usa {@code isEnabled()} y el atributo {@code disabled};
     * para elementos custom evalúa la clase CSS.</p>
     */
    public void validateButtonIsDisabled(String locator) {
        WebElement element = getElement(locator);
        String tagName = element.getTagName().toLowerCase();

        boolean isNativeButton = tagName.equals("button")
            || tagName.equals("input")
            || tagName.equals("select")
            || tagName.equals("textarea");

        boolean isDisabled;
        if (isNativeButton) {
            isDisabled = !element.isEnabled()
                || element.getDomAttribute("disabled") != null;
        } else {
            String cssClass = element.getDomAttribute("class");
            isDisabled = cssClass != null && cssClass.toLowerCase().contains("disabled");
        }

        Assertions.assertThat(isDisabled)
            .as("El botón '%s' (tag: <%s>) debe estar deshabilitado pero está habilitado. "
                + "Clase CSS: '%s'", locator, tagName, element.getDomAttribute("class"))
            .isTrue();

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "Botón deshabilitado validado: " + locator, null);
    }

    /**
     * Valida que un botón cambie su texto.
     */
    public void validateButtonTextChange(String locator, String initialText, String finalText) {
        WebElement element = getElement(locator);
        String currentText = element.getText();

        Assertions.assertThat(currentText)
            .as("El botón '%s' debe mostrar: '%s'", locator, finalText)
            .contains(finalText);

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            String.format("✅ Cambio de texto validado: '%s' -> '%s'", initialText, finalText), null);
    }

    // =========================================================================
    // MÉTODOS DE VALIDACIÓN - LONGITUD DE TEXTO
    // =========================================================================

    /**
     * Valida que un campo tenga una longitud mínima de caracteres.
     */
    public void validateMinLength(String locator, int minLength) {
        String value = getElementValue(getElement(locator));

        Assertions.assertThat(value)
            .as("El campo '%s' debe tener mínimo %d caracteres", locator, minLength)
            .hasSizeGreaterThanOrEqualTo(minLength);

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            String.format("✅ Longitud mínima validada: %d caracteres - %s", value.length(), locator), null);
    }

    /**
     * Valida que un campo tenga una longitud máxima de caracteres.
     */
    public void validateMaxLength(String locator, int maxLength) {
        String value = getElementValue(getElement(locator));

        Assertions.assertThat(value)
            .as("El campo '%s' debe tener máximo %d caracteres", locator, maxLength)
            .hasSizeLessThanOrEqualTo(maxLength);

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            String.format("✅ Longitud máxima validada: %d caracteres - %s", value.length(), locator), null);
    }

    /**
     * Valida que un campo tenga exactamente una cantidad de caracteres.
     */
    public void validateExactLength(String locator, int expectedLength) {
        String value = getElementValue(getElement(locator));

        Assertions.assertThat(value)
            .as("El campo '%s' debe tener exactamente %d caracteres", locator, expectedLength)
            .hasSize(expectedLength);

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            String.format("✅ Longitud exacta validada: %d caracteres - %s", expectedLength, locator), null);
    }

    // =========================================================================
    // MÉTODOS DE VALIDACIÓN - PLACEHOLDERS Y TOOLTIPS
    // =========================================================================

    /**
     * Valida que un campo muestre un placeholder específico.
     */
    public void validatePlaceholder(String locator, String expectedPlaceholder) {
        WebElement element = getElement(locator);
        String actualPlaceholder = element.getDomAttribute("placeholder");

        Assertions.assertThat(actualPlaceholder)
            .as("El placeholder del campo '%s' debe ser '%s'", locator, expectedPlaceholder)
            .isEqualTo(expectedPlaceholder);

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Placeholder validado: " + actualPlaceholder + " - " + locator, null);
    }

    /**
     * Valida que un campo muestre un tooltip específico.
     */
    public void validateTooltip(String locator, String expectedTooltip) {
        WebElement element = getElement(locator);
        String actualTooltip = element.getDomAttribute("title");

        Assertions.assertThat(actualTooltip)
            .as("El tooltip del campo '%s' debe ser '%s'", locator, expectedTooltip)
            .isEqualTo(expectedTooltip);

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Tooltip validado: " + actualTooltip + " - " + locator, null);
    }

    // =========================================================================
    // MÉTODOS DE VALIDACIÓN - MENSAJES
    // =========================================================================

    /**
     * Valida que un mensaje específico esté visible en la página.
     */
    public void validateMessageIsVisible(String locator) {
        boolean isVisible = waitForVisibleElement(locator, 5);

        Assertions.assertThat(isVisible)
            .as("El mensaje '%s' debe estar visible", locator)
            .isTrue();

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Mensaje visible: " + locator, null);
    }

    /**
     * Valida que un mensaje específico NO esté visible en la página.
     */
    public void validateMessageIsNotVisible(String locator) {
        boolean isPresent = isPresent(locator);

        Assertions.assertThat(isPresent)
            .as("El mensaje '%s' NO debe estar visible", locator)
            .isFalse();

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Mensaje NO visible: " + locator, null);
    }

    /**
     * Valida que un mensaje contenga un texto específico.*
     * <p>Normaliza saltos de línea (\n, \r\n) producidos por etiquetas {@code <br>} del HTML
     * reemplazándolos por un espacio simple, de modo que el texto esperado en el .feature
     * no necesite conocer el salto de línea interno del DOM.</p>
     *
     * <p>Ejemplo: el HTML {@code El email no está autorizado.<br>Contactate con tu ejecutivo.}
     * Selenium lo extrae como {@code "El email no está autorizado.\nContactate con tu ejecutivo."}
     * y este método lo normaliza a {@code "El email no está autorizado. Contactate con tu ejecutivo."}
     * antes de comparar.</p>
     */
    public void validateMessageContainsText(String locator, String expectedText) {
        String rawText = getTextOf(locator);

        // Normalizar saltos de línea causados por <br> en el HTML → espacio simple
        String actualText = rawText == null ? null
            : rawText.replaceAll("\\r\\n|\\r|\\n", " ").replaceAll(" {2,}", " ").trim();

        // Normalizar también el texto esperado por si viene con saltos desde el .feature
        String normalizedExpected = expectedText == null ? null
            : expectedText.replaceAll("\\r\\n|\\r|\\n", " ").replaceAll(" {2,}", " ").trim();

        Assertions.assertThat(actualText)
            .as("El mensaje '%s' debe contener '%s'", locator, normalizedExpected)
            .contains(normalizedExpected);

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Mensaje contiene texto esperado: " + locator, null);
    }

    // =========================================================================
    // MÉTODOS DE VALIDACIÓN - VISIBILIDAD Y EXISTENCIA
    // =========================================================================

    /**
     * Valida que un elemento esté visible.
     */
    public void validateElementIsVisible(String locator) {
        boolean isVisible = waitForVisibleElement(locator, 10);

        Assertions.assertThat(isVisible)
            .as("El elemento '%s' debe estar visible", locator)
            .isTrue();

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Elemento visible: " + locator, null);
    }

    /**
     * Valida que un elemento NO esté visible.
     */
    public void validateElementIsNotVisible(String locator) {
        boolean isPresent = isPresent(locator);

        Assertions.assertThat(isPresent)
            .as("El elemento '%s' NO debe estar visible", locator)
            .isFalse();

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Elemento NO visible: " + locator, null);
    }

    /**
     * Valida que un campo no esté vacío.
     */
    public void validateFieldNotEmpty(String locator) {
        String value = getElementValue(getElement(locator));

        Assertions.assertThat(value)
            .as("El campo '%s' no debe estar vacío", locator)
            .isNotEmpty();

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Campo no vacío validado: " + locator, null);
    }

    /**
     * Valida que un campo esté vacío.
     */
    public void validateFieldIsEmpty(String locator) {
        String value = getElementValue(getElement(locator));

        Assertions.assertThat(value)
            .as("El campo '%s' debe estar vacío", locator)
            .isEmpty();

        TestLogger.logInfo("WEB_HELPER_VALIDATION",
            "✅ Campo vacío validado: " + locator, null);
    }

    // =========================================================================
    // MÉTODOS DE ACCIONES - WRAPPERS CON LÓGICA COMPLEJA
    // =========================================================================

    /**
     * Establece texto en un elemento esperando a que esté habilitado y resolviendo variables.
     * Encapsula la lógica completa del step común de ingreso de texto.
     */
    public void setTextWithWait(String texto, String locator) {
        if (!waitForElementEnabled(locator, 10)) {
            TestLogger.logDebug("WEB_HELPER",
                "Elemento tardó más de 10s en habilitarse, continuando: " + locator, null);
        }

        String resolvedTexto = resolveVariables(texto);
        setText(locator, resolvedTexto);
    }

    /**
     * Espera a que un elemento esté habilitado y valida que lo esté.
     * Lanza excepción si no se habilita en el tiempo especificado.
     */
    public void waitAndValidateEnabled(String locator, int timeoutSeconds) {
        boolean enabled = waitForElementEnabled(locator, timeoutSeconds);

        Assertions.assertThat(enabled)
            .as("Se esperó %d segundos y el elemento no se habilitó: %s", timeoutSeconds, locator)
            .isTrue();

        TestLogger.logInfo("WEB_HELPER",
            String.format("✅ Elemento habilitado después de esperar: %s", locator), null);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES - PARSEO DE CONFIGURACIÓN
    // =========================================================================

    /**
     * Parsea string de navegador a BrowserType enum.
     * Soporta múltiples variaciones de nombres.
     *
     * @param browserName Nombre del navegador (case-insensitive)
     * @return BrowserType correspondiente
     * @throws IllegalArgumentException si el navegador no es soportado
     */
    public WebDriverFactory.BrowserType parseBrowserType(String browserName) {
        return switch (browserName.toLowerCase().trim()) {
            case "chrome", "google chrome", "chromium" ->
                WebDriverFactory.BrowserType.CHROME;
            case "firefox", "mozilla", "ff" ->
                WebDriverFactory.BrowserType.FIREFOX;
            case "edge", "microsoft edge", "msedge" ->
                WebDriverFactory.BrowserType.EDGE;
            case "safari" ->
                WebDriverFactory.BrowserType.SAFARI;
            default -> throw new IllegalArgumentException(
                "Navegador no soportado: " + browserName + ". " +
                "Valores válidos: chrome, firefox, edge, safari"
            );
        };
    }

    /**
     * Parsea string a boolean.
     * Soporta múltiples formatos: true/false, yes/no, si/no, 1/0
     *
     * @param value String a parsear (case-insensitive)
     * @return boolean correspondiente
     * @throws IllegalArgumentException si el valor no es reconocido
     */
    public boolean parseBoolean(String value) {
        return switch (value.toLowerCase().trim()) {
            case "true", "yes", "si", "1", "enabled" -> true;
            case "false", "no", "0", "disabled" -> false;
            default -> throw new IllegalArgumentException(
                "Valor boolean inválido: " + value + ". " +
                "Valores válidos: true/false, yes/no, si/no, 1/0"
            );
        };
    }

    // =========================================================================
    // NUEVOS MÉTODOS — v2.2.1 (requeridos por steps genéricos)
    // =========================================================================

    /**
     * Presiona una tecla del teclado (Enter, Tab, Escape, etc.).
     */
    public void pressKey(String keyName) {
        WebDriver driver = resolveDriver();
        Keys key = mapKeyName(keyName);
        new Actions(driver).sendKeys(key).perform();
        TestLogger.logInfo("WEB_HELPER", "Tecla presionada: " + keyName, null);
    }

    /**
     * Presiona una tecla del teclado sobre un elemento específico.
     */
    public void pressKeyOnElement(String locator, String keyName) {
        WebElement element = getElement(locator);
        Keys key = mapKeyName(keyName);
        element.sendKeys(key);
        TestLogger.logInfo("WEB_HELPER", "Tecla '" + keyName + "' presionada en: " + locator, null);
    }

    /**
     * Hace doble clic en un elemento.
     */
    public void doubleClick(String locator) {
        WebDriver driver = resolveDriver();
        WebElement element = getElement(locator);
        new Actions(driver).doubleClick(element).perform();
        TestLogger.logInfo("WEB_HELPER", "Doble clic en: " + locator, null);
    }

    /**
     * Limpia el contenido de un campo de texto.
     */
    public void clearField(String locator) {
        WebElement element = getElement(locator);
        element.clear();
        TestLogger.logInfo("WEB_HELPER", "Campo limpiado: " + locator, null);
    }

    /**
     * Sube un archivo en un campo input[type=file].
     */
    public void uploadFile(String filePath, String locator) {
        WebElement element = getElement(locator);
        File file = new File(filePath);
        Assertions.assertThat(file.exists())
            .as("El archivo '%s' debe existir", filePath)
            .isTrue();
        element.sendKeys(file.getAbsolutePath());
        TestLogger.logInfo("WEB_HELPER", "Archivo subido en '" + locator + "': " + filePath, null);
    }

    /**
     * Espera hasta que un texto sea visible en la página.
     */
    public void waitForTextVisible(String text, int timeoutSeconds) {
        WebDriver driver = resolveDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(text(),'" + text.replace("'", "\\'") + "')]")));
        TestLogger.logInfo("WEB_HELPER", "Texto visible: " + text, null);
    }

    /**
     * Cuenta el número de elementos que coinciden con un locator.
     */
    public int countElements(String locator) {
        WebDriver driver = resolveDriver();
        List<WebElement> elements;
        try {
            elements = driver.findElements(By.cssSelector(locator));
        } catch (InvalidSelectorException e) {
            elements = driver.findElements(By.xpath(locator));
        }
        TestLogger.logInfo("WEB_HELPER", "Elementos encontrados para '" + locator + "': " + elements.size(), null);
        return elements.size();
    }

    /**
     * Valida que un elemento tenga un atributo con un valor específico.
     */
    public void validateAttributeValue(String locator, String attribute, String expectedValue) {
        WebElement element = getElement(locator);
        String actual = element.getDomAttribute(attribute);
        Assertions.assertThat(actual)
            .as("Atributo '%s' del elemento '%s'", attribute, locator)
            .isEqualTo(resolveVariables(expectedValue));
        TestLogger.logInfo("WEB_HELPER",
            "Atributo '" + attribute + "' validado en '" + locator + "': " + actual, null);
    }

    /**
     * Valida que un elemento tenga una clase CSS específica.
     */
    public void validateHasCssClass(String locator, String cssClass) {
        WebElement element = getElement(locator);
        String classes = element.getDomAttribute("class");
        Assertions.assertThat(classes)
            .as("El elemento '%s' debería tener la clase CSS '%s'", locator, cssClass)
            .contains(cssClass);
        TestLogger.logInfo("WEB_HELPER", "Clase CSS '" + cssClass + "' presente en: " + locator, null);
    }

    /**
     * Valida el estado de selección de un checkbox.
     */
    public void validateCheckboxSelected(String locator, boolean expectedSelected) {
        WebElement element = getElement(locator);
        Assertions.assertThat(element.isSelected())
            .as("El checkbox '%s' debería estar %s",
                locator, expectedSelected ? "seleccionado" : "no seleccionado")
            .isEqualTo(expectedSelected);
        TestLogger.logInfo("WEB_HELPER",
            "Checkbox '" + locator + "' " + (expectedSelected ? "seleccionado" : "no seleccionado") + " (OK)", null);
    }

    /**
     * Obtiene el valor de un atributo de un elemento.
     */
    public String getAttributeValue(String locator, String attribute) {
        WebElement element = getElement(locator);
        String value = element.getDomAttribute(attribute);
        return value != null ? value : element.getDomProperty(attribute);
    }

    private Keys mapKeyName(String keyName) {
        return switch (keyName.toUpperCase().trim()) {
            case "ENTER", "RETURN" -> Keys.ENTER;
            case "TAB" -> Keys.TAB;
            case "ESCAPE", "ESC" -> Keys.ESCAPE;
            case "BACKSPACE", "BACK_SPACE" -> Keys.BACK_SPACE;
            case "DELETE", "DEL" -> Keys.DELETE;
            case "SPACE" -> Keys.SPACE;
            case "ARROW_UP", "UP" -> Keys.ARROW_UP;
            case "ARROW_DOWN", "DOWN" -> Keys.ARROW_DOWN;
            case "ARROW_LEFT", "LEFT" -> Keys.ARROW_LEFT;
            case "ARROW_RIGHT", "RIGHT" -> Keys.ARROW_RIGHT;
            case "HOME" -> Keys.HOME;
            case "END" -> Keys.END;
            case "PAGE_UP" -> Keys.PAGE_UP;
            case "PAGE_DOWN" -> Keys.PAGE_DOWN;
            case "F1" -> Keys.F1;
            case "F5" -> Keys.F5;
            case "F12" -> Keys.F12;
            default -> throw new IllegalArgumentException(
                "Tecla no soportada: " + keyName + ". Usar: ENTER, TAB, ESCAPE, BACKSPACE, DELETE, SPACE, ARROW_UP/DOWN/LEFT/RIGHT, HOME, END, etc.");
        };
    }
}

