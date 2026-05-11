package com.qa.common.driver;



import com.qa.common.api.driver.ElementLocator;
import com.qa.common.api.driver.Gesture;
import com.qa.common.api.driver.UiDriver;
import com.qa.common.api.runtime.ExecutionConfig;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de contrato para la interfaz {@link UiDriver}.
 *
 * <p><b>Objetivo:</b> verificar el contrato semántico de la interfaz sin ninguna
 * implementación real (no se inicia browser ni dispositivo). Se usa un stub mínimo
 * para cubrir:
 * <ul>
 *   <li>{@link UiDriver.DriverType} — enum WEB y MOBILE</li>
 *   <li>{@link UiDriver#performGesture(Gesture)} — default lanza UnsupportedOperationException</li>
 *   <li>El stub web respeta el contrato completo sin gestos</li>
 *   <li>El stub mobile puede sobrescribir {@code performGesture()}</li>
 * </ul>
 *
 * <p><b>Nota:</b> Los tests de integración reales (Playwright / Appium) viven en
 * {@code web-core} y {@code mobile-core} respectivamente.
 *
 * @author Abel Venero
 * @since 3.0.0
 */
@DisplayName("UiDriver — Contrato de interfaz")
class UiDriverContractTest {

    // =========================================================================
    // Stub web — no soporta gestos
    // =========================================================================

    private static class WebDriverStub implements UiDriver {
        private boolean opened = false;
        private boolean closed = false;
        private String lastLocation = "https://example.com";

        @Override public void open(ExecutionConfig config) { opened = true; }
        @Override public void close()                      { closed = true; }
        @Override public void click(ElementLocator l)      { /* no-op */ }
        @Override public void type(ElementLocator l, String text) { /* no-op */ }
        @Override public void waitFor(ElementLocator l, Duration t) { /* no-op */ }
        @Override public void screenshot(Path p)           { /* no-op */ }
        @Override public String getCurrentLocation()       { return lastLocation; }
        @Override public DriverType getType()              { return DriverType.WEB; }

        boolean isOpened() { return opened; }
        boolean isClosed() { return closed; }
    }

    // =========================================================================
    // Stub mobile — sobrescribe performGesture
    // =========================================================================

    private static class MobileDriverStub implements UiDriver {
        private Gesture lastGesture;
        private boolean opened = false;

        @Override public void open(ExecutionConfig config) { opened = true; }
        @Override public void close()                      { /* no-op */ }
        @Override public void click(ElementLocator l)      { /* no-op */ }
        @Override public void type(ElementLocator l, String t) { /* no-op */ }
        @Override public void waitFor(ElementLocator l, Duration t) { /* no-op */ }
        @Override public void screenshot(Path p)           { /* no-op */ }
        @Override public String getCurrentLocation()       { return "com.myapp/.MainActivity"; }
        @Override public DriverType getType()              { return DriverType.MOBILE; }

        @Override
        public void performGesture(Gesture gesture) {
            this.lastGesture = gesture;
        }

        Gesture getLastGesture() { return lastGesture; }
    }

    // =========================================================================
    // DriverType enum
    // =========================================================================

    @Nested
    @DisplayName("DriverType enum")
    class DriverTypeTest {

        @Test
        @DisplayName("DriverType tiene exactamente WEB y MOBILE")
        void enumContieneSoloWebYMobile() {
            UiDriver.DriverType[] values = UiDriver.DriverType.values();
            assertThat(values).containsExactlyInAnyOrder(
                UiDriver.DriverType.WEB,
                UiDriver.DriverType.MOBILE
            );
        }

        @Test
        @DisplayName("DriverType.WEB se puede parsear por nombre")
        void webSePuedeObtenerPorNombre() {
            assertThat(UiDriver.DriverType.valueOf("WEB")).isEqualTo(UiDriver.DriverType.WEB);
        }

        @Test
        @DisplayName("DriverType.MOBILE se puede parsear por nombre")
        void mobileSePuedeObtenerPorNombre() {
            assertThat(UiDriver.DriverType.valueOf("MOBILE")).isEqualTo(UiDriver.DriverType.MOBILE);
        }
    }

    // =========================================================================
    // performGesture() — default UnsupportedOperationException para web
    // =========================================================================

    @Nested
    @DisplayName("performGesture() — default web lanza UnsupportedOperationException")
    class PerformGestureDefaultTest {

        private final WebDriverStub webDriver = new WebDriverStub();

        @Test
        @DisplayName("Swipe lanza UnsupportedOperationException en implementación web")
        void swipeLanzaExcepcionEnWeb() {
            Gesture swipe = new Gesture.Swipe(Gesture.Swipe.Direction.UP);

            assertThatThrownBy(() -> webDriver.performGesture(swipe))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("WebDriverStub");
        }

        @Test
        @DisplayName("LongPress lanza UnsupportedOperationException en implementación web")
        void longPressLanzaExcepcionEnWeb() {
            Gesture longPress = new Gesture.LongPress(Duration.ofSeconds(1));

            assertThatThrownBy(() -> webDriver.performGesture(longPress))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Pinch lanza UnsupportedOperationException en implementación web")
        void pinchLanzaExcepcionEnWeb() {
            Gesture pinch = new Gesture.Pinch(Gesture.Pinch.Mode.ZOOM_IN);

            assertThatThrownBy(() -> webDriver.performGesture(pinch))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // =========================================================================
    // Stub web — ciclo de vida y contract
    // =========================================================================

    @Nested
    @DisplayName("WebDriverStub — contrato ciclo de vida")
    class WebDriverContractTest {

        private final WebDriverStub driver = new WebDriverStub();

        @Test
        @DisplayName("open() inicializa el driver")
        void openInicializaDriver() {
            ExecutionConfig config = new ExecutionConfig.Builder().build();
            driver.open(config);
            assertThat(driver.isOpened()).isTrue();
        }

        @Test
        @DisplayName("close() libera el driver")
        void closeLiberaDriver() {
            driver.close();
            assertThat(driver.isClosed()).isTrue();
        }

        @Test
        @DisplayName("getType() retorna WEB")
        void getTypeRetornaWeb() {
            assertThat(driver.getType()).isEqualTo(UiDriver.DriverType.WEB);
        }

        @Test
        @DisplayName("getCurrentLocation() retorna string no vacío")
        void getCurrentLocationNoVacio() {
            assertThat(driver.getCurrentLocation()).isNotBlank();
        }

        @Test
        @DisplayName("UiDriver implementa AutoCloseable")
        void implementaAutoCloseable() {
            assertThat(driver).isInstanceOf(AutoCloseable.class);
        }
    }

    // =========================================================================
    // Stub mobile — performGesture sobrescrito
    // =========================================================================

    @Nested
    @DisplayName("MobileDriverStub — performGesture sobrescrito")
    class MobileDriverContractTest {

        private final MobileDriverStub driver = new MobileDriverStub();

        @Test
        @DisplayName("getType() retorna MOBILE")
        void getTypeRetornaMobile() {
            assertThat(driver.getType()).isEqualTo(UiDriver.DriverType.MOBILE);
        }

        @Test
        @DisplayName("performGesture(Swipe.UP) no lanza excepcion")
        void swipeUpNoLanzaExcepcion() {
            Gesture swipe = new Gesture.Swipe(Gesture.Swipe.Direction.UP);
            assertThatCode(() -> driver.performGesture(swipe)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("performGesture almacena el gesto recibido")
        void gestureAlmacenado() {
            Gesture.LongPress lp = Gesture.LongPress.withDefaultDuration();
            driver.performGesture(lp);
            assertThat(driver.getLastGesture()).isEqualTo(lp);
        }

        @Test
        @DisplayName("performGesture acepta Pinch.ZOOM_IN")
        void pinchZoomInAceptado() {
            Gesture pinch = new Gesture.Pinch(Gesture.Pinch.Mode.ZOOM_IN);
            driver.performGesture(pinch);
            assertThat(driver.getLastGesture()).isEqualTo(pinch);
        }
    }
}
