package com.qa.mobilecore.helper;

import com.qa.common.driver.Gesture;
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
import java.util.Objects;

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

    /** Duración para movimientos de multi-touch (pinch/zoom). */
    private static final Duration MULTITOUCH_DURATION = Duration.ofMillis(400);

    /** Factor del 25% del viewport (punto de inicio/fin para swipes). */
    private static final double SWIPE_NEAR_FACTOR = 0.25;

    /** Factor del 75% del viewport (punto de inicio/fin para swipes). */
    private static final double SWIPE_FAR_FACTOR = 0.75;

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
        Sequence seq = new Sequence(finger, 0).
            addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y)).
            addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg())).
            addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
        TestLogger.logInfo("GESTURE", "Tap en (" + x + ", " + y + ")", null);
    }

    /**
     * Doble tap sobre un elemento.
     */
    public static void doubleTap(AppiumDriver driver, WebElement element) {
        Point center = getCenter(element);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 0).
            addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), center.x, center.y)).
            addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg())).
            addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg())).
            addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg())).
            addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
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
        Sequence seq = new Sequence(finger, 0).addAction(finger.createPointerMove(
                Duration.ZERO, PointerInput.Origin.viewport(), center.x, center.y)).
            addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg())).
            addAction(finger.createPointerMove(
                Duration.ofMillis(durationMs), PointerInput.Origin.viewport(), center.x, center.y)).
            addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
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

        int startX;
        int startY;
        int endX;
        int endY;

        switch (direction.toLowerCase().trim()) {
            case "arriba", "up" -> {
                startX = width / 2;
                startY = (int)(height * SWIPE_FAR_FACTOR);
                endX = width / 2;
                endY = (int)(height * SWIPE_NEAR_FACTOR);
            }
            case "abajo", "down" -> {
                startX = width / 2;
                startY = (int)(height * SWIPE_NEAR_FACTOR);
                endX = width / 2;
                endY = (int)(height * SWIPE_FAR_FACTOR);
            }
            case "izquierda", "left" -> {
                startX = (int)(width * SWIPE_FAR_FACTOR);
                startY = height / 2;
                endX = (int)(width * SWIPE_NEAR_FACTOR);
                endY = height / 2;
            }
            case "derecha", "right" -> {
                startX = (int)(width * SWIPE_NEAR_FACTOR);
                startY = height / 2;
                endX = (int)(width * SWIPE_FAR_FACTOR);
                endY = height / 2;
            }
            default -> throw new IllegalArgumentException(
                "Direccion de swipe no reconocida: '" + direction + "'. " +
                "Valores validos: arriba, abajo, izquierda, derecha");
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
                    if (ElementLocatorHelper.exists(driver, "text:" + text)) {
                        break;
                    }
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
            new TouchPath(center.x - offset, center.y, center.x, center.y),
            new TouchPath(center.x + offset, center.y, center.x, center.y));
        TestLogger.logInfo("GESTURE", "Pinch sobre elemento", null);
    }

    /**
     * Zoom sobre un elemento — separar dedos (zoom in).
     */
    public static void zoom(AppiumDriver driver, WebElement element) {
        Point center = getCenter(element);
        int offset = 100;
        performMultiTouch(driver,
            new TouchPath(center.x, center.y, center.x - offset, center.y),
            new TouchPath(center.x, center.y, center.x + offset, center.y));
        TestLogger.logInfo("GESTURE", "Zoom sobre elemento", null);
    }

    // =========================================================================
    // Gesture (interfaz sellada de common)
    // =========================================================================

    /**
     * Ejecuta un {@link Gesture} sobre el driver usando los helpers W3C Actions de esta clase.
     *
     * <p>Punto de entrada unificado para {@link com.qa.mobilecore.driver.AppiumEngine#performGesture}.
     *
     * @param driver  driver Appium activo, no null
     * @param gesture gesto a ejecutar (Swipe, Pinch, LongPress), no null
     */
    public static void execute(AppiumDriver driver, Gesture gesture) {
        Objects.requireNonNull(driver,  "driver no puede ser null");
        Objects.requireNonNull(gesture, "gesture no puede ser null");
        switch (gesture) {
            case Gesture.Swipe s    -> swipe(driver, s.direction().name().toLowerCase());
            case Gesture.LongPress lp -> longPressAtCenter(driver, lp.duration().toMillis());
            case Gesture.Pinch p    -> {
                if (p.mode() == Gesture.Pinch.Mode.ZOOM_IN) {
                    pinchOrZoomAtCenter(driver, false);  // ZOOM_IN = spread (fingers apart)
                } else {
                    pinchOrZoomAtCenter(driver, true);   // ZOOM_OUT = pinch (fingers together)
                }
            }
        }
    }

    // =========================================================================
    // Privados
    // =========================================================================

    private static void swipeFromTo(AppiumDriver driver,
            int startX, int startY, int endX, int endY, Duration duration) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 0).addAction(finger.createPointerMove(
                Duration.ZERO, PointerInput.Origin.viewport(), startX, startY)).
            addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg())).
            addAction(finger.createPointerMove(
                duration, PointerInput.Origin.viewport(), endX, endY)).
            addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
    }

    /**
     * Encapsulates the start and end coordinates for a single touch finger path.
     *
     * @param startX start X coordinate in viewport pixels
     * @param startY start Y coordinate in viewport pixels
     * @param endX   end X coordinate in viewport pixels
     * @param endY   end Y coordinate in viewport pixels
     */
    private record TouchPath(int startX, int startY, int endX, int endY) {}

    /**
     * Performs a two-finger multi-touch gesture (e.g. pinch or zoom).
     *
     * @param driver driver instance
     * @param finger1 path for the first finger
     * @param finger2 path for the second finger
     */
    private static void performMultiTouch(AppiumDriver driver,
            TouchPath finger1Path, TouchPath finger2Path) {

        PointerInput finger1 = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
        PointerInput finger2 = new PointerInput(PointerInput.Kind.TOUCH, "finger2");

        Sequence seq1 = new Sequence(finger1, 0).addAction(finger1.createPointerMove(
                Duration.ZERO, PointerInput.Origin.viewport(),
                finger1Path.startX(), finger1Path.startY())).
            addAction(finger1.createPointerDown(PointerInput.MouseButton.LEFT.asArg())).
            addAction(finger1.createPointerMove(
                MULTITOUCH_DURATION, PointerInput.Origin.viewport(),
                finger1Path.endX(), finger1Path.endY())).
            addAction(finger1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        Sequence seq2 = new Sequence(finger2, 0).addAction(finger2.createPointerMove(
                Duration.ZERO, PointerInput.Origin.viewport(),
                finger2Path.startX(), finger2Path.startY())).
            addAction(finger2.createPointerDown(PointerInput.MouseButton.LEFT.asArg())).
            addAction(finger2.createPointerMove(
                MULTITOUCH_DURATION, PointerInput.Origin.viewport(),
                finger2Path.endX(), finger2Path.endY())).
            addAction(finger2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Arrays.asList(seq1, seq2));
    }

    private static Point getCenter(WebElement element) {
        Point loc = element.getLocation();
        Dimension sz = element.getSize();
        return new Point(loc.x + sz.width / 2, loc.y + sz.height / 2);
    }

    /**
     * Long press en el centro de la pantalla durante la duración indicada.
     *
     * @param driver     driver Appium activo
     * @param durationMs duración del press en milisegundos
     */
    private static void longPressAtCenter(AppiumDriver driver, long durationMs) {
        Dimension size = driver.manage().window().getSize();
        int cx = size.getWidth() / 2;
        int cy = size.getHeight() / 2;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 0)
            .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), cx, cy))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerMove(Duration.ofMillis(durationMs), PointerInput.Origin.viewport(), cx, cy))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
        TestLogger.logInfo("GESTURE", "Long press " + durationMs + "ms en centro de pantalla", null);
    }

    /**
     * Gesto de pinch o zoom en el centro de la pantalla.
     *
     * @param driver   driver Appium activo
     * @param isPinch  {@code true} → ZOOM_OUT (dedos se acercan al centro);
     *                 {@code false} → ZOOM_IN (dedos se alejan del centro)
     */
    private static void pinchOrZoomAtCenter(AppiumDriver driver, boolean isPinch) {
        Dimension size = driver.manage().window().getSize();
        int cx     = size.getWidth() / 2;
        int cy     = size.getHeight() / 2;
        int offset = Math.min(size.getWidth(), size.getHeight()) / 4;

        TouchPath f1;
        TouchPath f2;
        if (isPinch) {
            // Pinch / ZOOM_OUT: dedos se mueven DESDE los bordes HACIA el centro
            f1 = new TouchPath(cx - offset, cy, cx, cy);
            f2 = new TouchPath(cx + offset, cy, cx, cy);
        } else {
            // Zoom / ZOOM_IN: dedos se mueven DESDE el centro HACIA los bordes
            f1 = new TouchPath(cx, cy, cx - offset, cy);
            f2 = new TouchPath(cx, cy, cx + offset, cy);
        }
        performMultiTouch(driver, f1, f2);
        TestLogger.logInfo("GESTURE", (isPinch ? "Pinch" : "Zoom") + " en centro de pantalla", null);
    }
}
