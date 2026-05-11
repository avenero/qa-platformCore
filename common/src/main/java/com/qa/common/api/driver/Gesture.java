package com.qa.common.api.driver;
import com.qa.common.utils.security.SecurityUtilities;

import java.time.Duration;
import java.util.Objects;

/**
 * Contrato sellado para gestos táctiles de UI mobile.
 *
 * <p>Las variantes cubren los gestos más comunes en apps nativas:
 * <ul>
 *   <li>{@link Swipe} — deslizamiento en una dirección cardinal</li>
 *   <li>{@link Pinch} — pellizco (zoom-out) o expansión (zoom-in) con dos dedos</li>
 *   <li>{@link LongPress} — pulsación larga sobre un elemento</li>
 * </ul>
 *
 * <p>Las implementaciones de {@link UiDriver} reciben un {@code Gesture} en
 * {@link UiDriver#performGesture(Gesture)} y lo traducen a la API nativa
 * (Appium W3C Actions API, XCUITest, UIAutomator2, etc.). Web-core lanza
 * {@link UnsupportedOperationException} — los gestos táctiles son exclusivos de mobile.
 *
 * <h2>Uso típico en un step BDD</h2>
 * <pre>
 * driver.performGesture(new Gesture.Swipe(Gesture.Swipe.Direction.UP));
 * driver.performGesture(new Gesture.LongPress(Duration.ofSeconds(2)));
 * driver.performGesture(new Gesture.Pinch(Gesture.Pinch.Mode.ZOOM_IN));
 * </pre>
 *
 * @author Abel Venero
 * @since 3.0.0
 * @see UiDriver
 */
public sealed interface Gesture permits Gesture.Swipe, Gesture.Pinch, Gesture.LongPress {

    // =========================================================================
    // Swipe — deslizamiento en dirección cardinal
    // =========================================================================

    /**
     * Deslizamiento en una dirección cardinal relativa al viewport.
     *
     * @param direction dirección del swipe
     */
    record Swipe(Direction direction) implements Gesture {

        /** Dirección del gesto de swipe. */
        public enum Direction {
            /** Deslizar hacia arriba (scroll down). */
            UP,
            /** Deslizar hacia abajo (scroll up). */
            DOWN,
            /** Deslizar hacia la izquierda. */
            LEFT,
            /** Deslizar hacia la derecha. */
            RIGHT
        }

        public Swipe {
            Objects.requireNonNull(direction, "Swipe.direction no puede ser null");
        }
    }

    // =========================================================================
    // Pinch — gesto de dos dedos (zoom in / zoom out)
    // =========================================================================

    /**
     * Gesto de pellizco o expansión con dos dedos.
     *
     * <p>{@link Mode#ZOOM_OUT} acerca los dedos (zoom-out / pinch).
     * {@link Mode#ZOOM_IN} aleja los dedos (zoom-in / spread).
     *
     * @param mode modo del gesto
     */
    record Pinch(Mode mode) implements Gesture {

        /** Modo del gesto de dos dedos. */
        public enum Mode {
            /** Acercar dedos (zoom-out / pinch). */
            ZOOM_OUT,
            /** Alejar dedos (zoom-in / spread). */
            ZOOM_IN
        }

        public Pinch {
            Objects.requireNonNull(mode, "Pinch.mode no puede ser null");
        }
    }

    // =========================================================================
    // LongPress — pulsación larga
    // =========================================================================

    /**
     * Pulsación larga sobre un elemento por una duración determinada.
     *
     * <p>La duración mínima recomendada es 500 ms; valores menores pueden
     * no ser reconocidos por el sistema operativo como long-press.
     *
     * @param duration duración de la pulsación, positiva y no nula
     */
    record LongPress(Duration duration) implements Gesture {

        /** Duración por defecto: 2 segundos. */
        public static final Duration DEFAULT_DURATION = Duration.ofSeconds(2);

        public LongPress {
            Objects.requireNonNull(duration, "LongPress.duration no puede ser null");
            if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException(
                    "LongPress.duration debe ser positiva, pero fue: " + duration);
            }
        }

        /** Crea un LongPress con la duración por defecto (2 segundos). */
        public static LongPress withDefaultDuration() {
            return new LongPress(DEFAULT_DURATION);
        }
    }
}
