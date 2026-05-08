package com.qa.common.driver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para {@link Gesture} y sus variantes selladas.
 *
 * @author Abel Venero
 * @since 3.0.0
 */
@DisplayName("Gesture — sealed type")
class GestureTest {

    // =========================================================================
    // Gesture.Swipe
    // =========================================================================

    @Nested
    @DisplayName("Gesture.Swipe")
    class SwipeTests {

        @Test
        @DisplayName("Swipe con Direction.UP se crea correctamente")
        void swipeUpCreado() {
            Gesture.Swipe swipe = new Gesture.Swipe(Gesture.Swipe.Direction.UP);
            assertThat(swipe.direction()).isEqualTo(Gesture.Swipe.Direction.UP);
        }

        @Test
        @DisplayName("Swipe con direction null lanza NullPointerException")
        void swipeNullDirectionLanzaException() {
            assertThatNullPointerException()
                .isThrownBy(() -> new Gesture.Swipe(null));
        }

        @Test
        @DisplayName("Direction enum contiene las cuatro direcciones cardinales")
        void directionContieneTodasLasVariantes() {
            assertThat(Gesture.Swipe.Direction.values()).containsExactlyInAnyOrder(
                Gesture.Swipe.Direction.UP,
                Gesture.Swipe.Direction.DOWN,
                Gesture.Swipe.Direction.LEFT,
                Gesture.Swipe.Direction.RIGHT
            );
        }

        @Test
        @DisplayName("Swipe implementa Gesture (sealed)")
        void swipeImplementaGesture() {
            Gesture g = new Gesture.Swipe(Gesture.Swipe.Direction.DOWN);
            assertThat(g).isInstanceOf(Gesture.class);
        }

        @Test
        @DisplayName("Dos Swipe con misma Direction son iguales (record)")
        void dosSwipeIguales() {
            Gesture.Swipe a = new Gesture.Swipe(Gesture.Swipe.Direction.LEFT);
            Gesture.Swipe b = new Gesture.Swipe(Gesture.Swipe.Direction.LEFT);
            assertThat(a).isEqualTo(b);
        }
    }

    // =========================================================================
    // Gesture.Pinch
    // =========================================================================

    @Nested
    @DisplayName("Gesture.Pinch")
    class PinchTests {

        @Test
        @DisplayName("Pinch con Mode.ZOOM_IN se crea correctamente")
        void pinchZoomInCreado() {
            Gesture.Pinch pinch = new Gesture.Pinch(Gesture.Pinch.Mode.ZOOM_IN);
            assertThat(pinch.mode()).isEqualTo(Gesture.Pinch.Mode.ZOOM_IN);
        }

        @Test
        @DisplayName("Pinch con Mode.ZOOM_OUT se crea correctamente")
        void pinchZoomOutCreado() {
            Gesture.Pinch pinch = new Gesture.Pinch(Gesture.Pinch.Mode.ZOOM_OUT);
            assertThat(pinch.mode()).isEqualTo(Gesture.Pinch.Mode.ZOOM_OUT);
        }

        @Test
        @DisplayName("Pinch con mode null lanza NullPointerException")
        void pinchNullModeLanzaException() {
            assertThatNullPointerException()
                .isThrownBy(() -> new Gesture.Pinch(null));
        }

        @Test
        @DisplayName("Mode enum tiene ZOOM_IN y ZOOM_OUT")
        void modeContieneAmbosValores() {
            assertThat(Gesture.Pinch.Mode.values()).containsExactlyInAnyOrder(
                Gesture.Pinch.Mode.ZOOM_IN,
                Gesture.Pinch.Mode.ZOOM_OUT
            );
        }

        @Test
        @DisplayName("Pinch implementa Gesture (sealed)")
        void pinchImplementaGesture() {
            Gesture g = new Gesture.Pinch(Gesture.Pinch.Mode.ZOOM_OUT);
            assertThat(g).isInstanceOf(Gesture.class);
        }
    }

    // =========================================================================
    // Gesture.LongPress
    // =========================================================================

    @Nested
    @DisplayName("Gesture.LongPress")
    class LongPressTests {

        @Test
        @DisplayName("LongPress con duración válida se crea correctamente")
        void longPressCreado() {
            Gesture.LongPress lp = new Gesture.LongPress(Duration.ofSeconds(3));
            assertThat(lp.duration()).isEqualTo(Duration.ofSeconds(3));
        }

        @Test
        @DisplayName("withDefaultDuration() retorna 2 segundos")
        void defaultDurationEsDosSegundos() {
            Gesture.LongPress lp = Gesture.LongPress.withDefaultDuration();
            assertThat(lp.duration()).isEqualTo(Duration.ofSeconds(2));
        }

        @Test
        @DisplayName("LongPress con duration null lanza NullPointerException")
        void longPressNullDurationLanzaException() {
            assertThatNullPointerException()
                .isThrownBy(() -> new Gesture.LongPress(null));
        }

        @Test
        @DisplayName("LongPress con duration cero lanza IllegalArgumentException")
        void longPressCeroDurationLanzaException() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new Gesture.LongPress(Duration.ZERO));
        }

        @Test
        @DisplayName("LongPress con duration negativa lanza IllegalArgumentException")
        void longPressNegativaDurationLanzaException() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new Gesture.LongPress(Duration.ofSeconds(-1)));
        }

        @Test
        @DisplayName("LongPress implementa Gesture (sealed)")
        void longPressImplementaGesture() {
            Gesture g = Gesture.LongPress.withDefaultDuration();
            assertThat(g).isInstanceOf(Gesture.class);
        }

        @Test
        @DisplayName("Dos LongPress con igual duracion son iguales (record)")
        void dosLongPressIguales() {
            Gesture.LongPress a = new Gesture.LongPress(Duration.ofMillis(500));
            Gesture.LongPress b = new Gesture.LongPress(Duration.ofMillis(500));
            assertThat(a).isEqualTo(b);
        }
    }

    // =========================================================================
    // Pattern matching sobre sealed type
    // =========================================================================

    @Nested
    @DisplayName("Pattern matching sobre Gesture sealed")
    class PatternMatchingTests {

        @Test
        @DisplayName("Pattern matching distingue los tres tipos de Gesture")
        void patternMatchingFunciona() {
            Gesture[] gestures = {
                new Gesture.Swipe(Gesture.Swipe.Direction.UP),
                new Gesture.Pinch(Gesture.Pinch.Mode.ZOOM_IN),
                Gesture.LongPress.withDefaultDuration()
            };
            int swipeCount = 0, pinchCount = 0, longPressCount = 0;
            for (Gesture g : gestures) {
                if (g instanceof Gesture.Swipe)     swipeCount++;
                else if (g instanceof Gesture.Pinch) pinchCount++;
                else if (g instanceof Gesture.LongPress) longPressCount++;
            }
            assertThat(swipeCount).isEqualTo(1);
            assertThat(pinchCount).isEqualTo(1);
            assertThat(longPressCount).isEqualTo(1);
        }
    }
}
