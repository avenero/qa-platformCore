package com.qa.mobilecore.helper;

import com.qa.common.logging.TestLogger;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Resuelve expresiones de localización de elementos mobile a llamadas Appium.
 *
 * <p>Permite que los steps sean 100% genéricos: el locator se pasa como un
 * {@code String} con un prefijo opcional que indica la estrategia.
 *
 * <p><b>Estrategias soportadas:</b>
 * <pre>
 * "~login_button"              → AppiumBy.accessibilityId("login_button")
 * "id:com.app:id/btn_login"    → AppiumBy.id("com.app:id/btn_login")
 * "xpath://android.widget.Button[@text='Login']" → AppiumBy.xpath(...)
 * "class:android.widget.Button" → AppiumBy.className(...)
 * "text:Iniciar sesion"        → AppiumBy.xpath con text()/label/value
 * "Login"  (sin prefijo)       → accessibilityId por default (más portable)
 * </pre>
 *
 * <p><b>Convención de prefijos (compatible con sugerencia AI para el FE):</b>
 * Los prefijos están diseñados para ser simples e intuitivos para los QAs
 * y fácilmente entrenables en un modelo de sugerencias de steps.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class ElementLocatorHelper {

    private ElementLocatorHelper() {}

    /**
     * Encuentra el primer elemento que coincide con la expresión de localización.
     *
     * @param driver     AppiumDriver activo
     * @param expression expresión de localización (con o sin prefijo)
     * @return WebElement encontrado
     * @throws org.openqa.selenium.NoSuchElementException si el elemento no existe
     */
    public static WebElement find(AppiumDriver driver, String expression) {
        TestLogger.logInfo("LOCATOR", "Buscando elemento: " + expression, null);
        return driver.findElement(resolveBy(expression));
    }

    /**
     * Encuentra todos los elementos que coinciden con la expresión.
     *
     * @param driver     AppiumDriver activo
     * @param expression expresión de localización
     * @return lista (puede estar vacía)
     */
    public static List<WebElement> findAll(AppiumDriver driver, String expression) {
        TestLogger.logInfo("LOCATOR", "Buscando todos los elementos: " + expression, null);
        return driver.findElements(resolveBy(expression));
    }

    /**
     * Verifica si existe al menos un elemento con la expresión dada (sin lanzar excepción).
     */
    public static boolean exists(AppiumDriver driver, String expression) {
        try {
            return !driver.findElements(resolveBy(expression)).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================================
    // Resolución interna
    // =========================================================================

    private static org.openqa.selenium.By resolveBy(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("La expresion de localización no puede ser nula o vacía");
        }

        // "~<value>" → Accessibility ID (más portable entre Android e iOS)
        if (expression.startsWith("~")) {
            return AppiumBy.accessibilityId(expression.substring(1));
        }

        // "id:<value>" → Resource ID (Android) o name (iOS)
        if (expression.startsWith("id:")) {
            return AppiumBy.id(expression.substring(3));
        }

        // "xpath:<value>" → XPath
        if (expression.startsWith("xpath:")) {
            return AppiumBy.xpath(expression.substring(6));
        }

        // "class:<value>" → ClassName
        if (expression.startsWith("class:")) {
            return AppiumBy.className(expression.substring(6));
        }

        // "text:<value>" → XPath buscando por texto visible (Android y iOS)
        if (expression.startsWith("text:")) {
            String text = expression.substring(5);
            // Busca en @text (Android), @label y @value (iOS)
            return AppiumBy.xpath(
                "//*[@text='" + text + "' or @label='" + text + "' or @value='" + text + "']");
        }

        // "pred:<value>" → iOS predicate string
        if (expression.startsWith("pred:")) {
            return AppiumBy.iOSNsPredicateString(expression.substring(5));
        }

        // "chain:<value>" → iOS class chain
        if (expression.startsWith("chain:")) {
            return AppiumBy.iOSClassChain(expression.substring(6));
        }

        // "uia:<value>" → Android UIAutomator
        if (expression.startsWith("uia:")) {
            return AppiumBy.androidUIAutomator(expression.substring(4));
        }

        // Sin prefijo → Accessibility ID por default
        // Es la estrategia más portable entre Android e iOS y facilita la IA de sugerencias
        TestLogger.logInfo("LOCATOR",
            "Sin prefijo detectado, usando accessibilityId por default: " + expression, null);
        return AppiumBy.accessibilityId(expression);
    }
}
