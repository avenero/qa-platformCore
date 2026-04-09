package com.qa.mobilecore.helper;

import com.qa.common.logging.TestLogger;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Helper para gestos táctiles usando la W3C Actions API de Appium 8+.
 *
 * <p><b>Por qué W3C Actions:</b> {@code TouchAction} y {@code MultiTouchAction}
 * están deprecados desde Appium 8. La W3C Actions API (basada en {@link PointerInput}
 * y {@link Sequence}) es el estándar oficial y portable entre versiones.
 *
 * <p>Todos los gestos operan con coordenadas relativas al viewport del dispositivo,
 * calculadas como porcentajes de ancho/alto de pantalla para ser independientes
 * de la resolución del dispositivo.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class GestureHelper {

    /** Duración por defecto para movimientos de swipe. */
    private static final Duration SWIPE_DURATION = Duration.ofMillis(600);

    /** Duración por defecto para long press. */
    private static final Duration LONG_PRESS_DEFAULT = Duration.ofSeconds(2);

    private GestureHelper() {}

    // =========================================================================
    // Tap
    // =========================================================================

    /**
     * Tap simple sobre las coordenadas centrales de un elemento.
     */
    public static void tap(AppiumDriver driver, WebElement element) {
        Point center = getCenter(element);
        tapAt(driver, center.x, center.y);
    }

    /**
     * Tap simple en coordenadas absolutas.
     */
    public static void tapAt(AppiumDriver driver, int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 0)
            .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
        TestLogger.logInfo("GESTURE", "Tap en (" + x + ", " + y + ")", null);
    }

    /**
     * Doble tap sobre un elemento.
     */
    public static void doubleTap(AppiumDriver driver, WebElement element) {
        Point center = getCenter(element);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 0)
            .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), center.x, center.y))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
        TestLogger.logInfo("GESTURE", "Doble tap en: " + element, null);
    }

    // =========================================================================
    // Long Press
    // =========================================================================

    /**
     * Long press sobre un elemento con duración en milisegundos.
     */
    public static void longPress(AppiumDriver driver, WebElement element, long durationMs) {
        Point center = getCenter(element);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 0)
            .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), center.x, center.y))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerMove(Duration.ofMillis(durationMs), PointerInput.Origin.viewport(), center.x, center.y))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
        TestLogger.logInfo("GESTURE", "Long press " + durationMs + "ms en: " + element, null);
    }

    // =========================================================================
    // Swipe
    // =========================================================================

    /**
     * Swipe en una dirección relativa al viewport del dispositivo.
     *
     * @param direction "arriba" | "abajo" | "izquierda" | "derecha"
     */
    public static void swipe(AppiumDriver driver, String direction) {
        Dimension size = driver.manage().window().getSize();
        int width  = size.getWidth();
        int height = size.getHeight();

        int startX, startY, endX, endY;

        switch (direction.toLowerCase().trim()) {
            case "arriba", "up" -> {
                startX = width / 2;  startY = (int)(height * 0.75);
                endX   = width / 2;  endY   = (int)(height * 0.25);
            }
            case "abajo", "down" -> {
                startX = width / 2;  startY = (int)(height * 0.25);
                endX   = width / 2;  endY   = (int)(height * 0.75);
            }
            case "izquierda", "left" -> {
                startX = (int)(width * 0.75); startY = height / 2;
                endX   = (int)(width * 0.25); endY   = height / 2;
            }
            case "derecha", "right" -> {
                startX = (int)(width * 0.25); startY = height / 2;
                endX   = (int)(width * 0.75); endY   = height / 2;
            }
            default -> throw new IllegalArgumentException(
                "Dirección de swipe no reconocida: '" + direction + "'. " +
                "Valores válidos: arriba, abajo, izquierda, derecha");
        }

        swipeFromTo(driver, startX, startY, endX, endY, SWIPE_DURATION);
        TestLogger.logInfo("GESTURE", "Swipe hacia: " + direction, null);
    }

    /**
     * Swipe desde el centro de un elemento hacia el centro de otro.
     */
    public static void swipeBetween(AppiumDriver driver, WebElement from, WebElement to) {
        Point start = getCenter(from);
        Point end   = getCenter(to);
        swipeFromTo(driver, start.x, start.y, end.x, end.y, SWIPE_DURATION);
        TestLogger.logInfo("GESTURE", "Swipe entre elementos", null);
    }

    // =========================================================================
    // Scroll
    // =========================================================================

    /**
     * Hace scroll hasta que un texto sea visible en pantalla.
     * Delega en el mecanismo nativo de cada plataforma.
     */
    public static void scrollToText(AppiumDriver driver, String text) {
        try {
            if (driver instanceof AndroidDriver) {
                // Android: UIAutomator scroll nativo (más fiable)
                driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))" +
                    ".scrollIntoView(new UiSelector().textContains(\"" + text + "\"))"));
            } else if (driver instanceof IOSDriver) {
                // iOS: mobile:scroll con predicado
                driver.executeScript("mobile:scroll",
                    Map.of("direction", "down", "predicateString", "label CONTAINS '" + text + "'"));
            } else {
                // Fallback genérico: swipes repetidos hacia arriba
                for (int i = 0; i < 5; i++) {
                    if (ElementLocatorHelper.exists(driver, "text:" + text)) break;
                    swipe(driver, "arriba");
                }
            }
            TestLogger.logInfo("GESTURE", "Scroll hasta texto: " + text, null);
        } catch (Exception e) {
            TestLogger.logWarning("GESTURE",
                "No se pudo hacer scroll hasta '" + text + "': " + e.getMessage(), null);
        }
    }

    // =========================================================================
    // Pinch y Zoom (multi-touch)
    // =========================================================================

    /**
     * Pinch (pellizco) sobre un elemento — acercar dedos (zoom out).
     */
    public static void pinch(AppiumDriver driver, WebElement element) {
        Point center = getCenter(element);
        int offset = 100;
        performMultiTouch(driver,
            center.x - offset, center.y, center.x + offset, center.y,
            center.x,          center.y, center.x,          center.y);
        TestLogger.logInfo("GESTURE", "Pinch sobre elemento", null);
    }

    /**
     * Zoom sobre un elemento — separar dedos (zoom in).
     */
    public static void zoom(AppiumDriver driver, WebElement element) {
        Point center = getCenter(element);
        int offset = 100;
        performMultiTouch(driver,
            center.x, center.y, center.x,          center.y,
            center.x - offset, center.y, center.x + offset, center.y);
        TestLogger.logInfo("GESTURE", "Zoom sobre elemento", null);
    }

    // =========================================================================
    // Privados
    // =========================================================================

    private static void swipeFromTo(AppiumDriver driver,
            int startX, int startY, int endX, int endY, Duration duration) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 0)
            .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerMove(duration, PointerInput.Origin.viewport(), endX, endY))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
    }

    private static void performMultiTouch(AppiumDriver driver,
            int f1StartX, int f1StartY, int f2StartX, int f2StartY,
            int f1EndX,   int f1EndY,   int f2EndX,   int f2EndY) {

        PointerInput finger1 = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
        PointerInput finger2 = new PointerInput(PointerInput.Kind.TOUCH, "finger2");

        Sequence seq1 = new Sequence(finger1, 0)
            .addAction(finger1.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), f1StartX, f1StartY))
            .addAction(finger1.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger1.createPointerMove(Duration.ofMillis(400), PointerInput.Origin.viewport(), f1EndX, f1EndY))
            .addAction(finger1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        Sequence seq2 = new Sequence(finger2, 0)
            .addAction(finger2.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), f2StartX, f2StartY))
            .addAction(finger2.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger2.createPointerMove(Duration.ofMillis(400), PointerInput.Origin.viewport(), f2EndX, f2EndY))
            .addAction(finger2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Arrays.asList(seq1, seq2));
    }

    private static Point getCenter(WebElement element) {
        Point loc  = element.getLocation();
        Dimension sz = element.getSize();
        return new Point(loc.x + sz.width / 2, loc.y + sz.height / 2);
    }
}
