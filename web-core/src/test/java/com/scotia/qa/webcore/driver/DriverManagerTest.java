package com.scotia.qa.webcore.driver;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Sequence;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para DriverManager.
 *
 * <p><b>Estrategia:</b> Stub de WebDriver para no requerir navegador real.</p>
 * <p><b>Cobertura:</b> Set/Get del driver, ThreadLocal, y validaciones de null.</p>
 *
 * @author Abel Venero
 * @since 1.0.0
 */
@DisplayName("DriverManager")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DriverManagerTest {

    // =========================================================================
    // Stub de WebDriver (sin Mockito)
    // =========================================================================

    /** Stub mínimo de WebDriver que registra si quit() fue llamado. */
    static class StubWebDriver implements WebDriver {
        boolean quitCalled = false;
        boolean quitShouldThrow = false;

        @Override public void quit() {
            if (quitShouldThrow) throw new RuntimeException("Error cerrando");
            quitCalled = true;
        }
        @Override public void get(String url) {}
        @Override public String getCurrentUrl() { return null; }
        @Override public String getTitle() { return null; }
        @Override public List<WebElement> findElements(By by) { return Collections.emptyList(); }
        @Override public WebElement findElement(By by) { return null; }
        @Override public String getPageSource() { return null; }
        @Override public void close() {}
        @Override public Set<String> getWindowHandles() { return Collections.emptySet(); }
        @Override public String getWindowHandle() { return null; }
        @Override public TargetLocator switchTo() { return null; }
        @Override public Navigation navigate() { return null; }
        @Override public Options manage() { return null; }
        @Override public void perform(Collection<Sequence> actions) {}
        @Override public void resetInputState() {}
    }

    // =========================================================================
    // Setup
    // =========================================================================

    @BeforeEach
    void setUp() {
        try { DriverManager.quitDriver(); } catch (Exception ignored) {}
    }

    @AfterEach
    void tearDown() {
        try { DriverManager.quitDriver(); } catch (Exception ignored) {}
    }

    // =========================================================================
    // setDriver / getDriver
    // =========================================================================

    @Nested
    @DisplayName("setDriver() / getDriver()")
    @Order(1)
    class SetGetDriverTests {

        @Test
        @DisplayName("getDriver lanza IllegalStateException si no se ha inicializado")
        void getDriverSinInicializarLanzaExcepcion() {
            assertThatThrownBy(DriverManager::getDriver)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no inicializado");
        }

        @Test
        @DisplayName("setDriver establece el driver correctamente")
        void setDriverEstableceDriver() {
            StubWebDriver stub = new StubWebDriver();
            DriverManager.setDriver(stub);
            assertThat(DriverManager.getDriver()).isSameAs(stub);
        }

        @Test
        @DisplayName("setDriver lanza IllegalArgumentException con driver null")
        void setDriverNullLanzaExcepcion() {
            assertThatThrownBy(() -> DriverManager.setDriver(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("setDriver reemplaza el driver existente cuando replace=true")
        void setDriverReemplazaConReplaceTrue() {
            StubWebDriver d1 = new StubWebDriver();
            StubWebDriver d2 = new StubWebDriver();
            DriverManager.setDriver(d1, true);
            DriverManager.setDriver(d2, true);
            assertThat(DriverManager.getDriver()).isSameAs(d2);
        }

        @Test
        @DisplayName("setDriver lanza IllegalStateException cuando replace=false y ya existe driver")
        void setDriverLanzaExcepcionConReplaceFalse() {
            StubWebDriver d1 = new StubWebDriver();
            StubWebDriver d2 = new StubWebDriver();
            DriverManager.setDriver(d1, false);
            assertThatThrownBy(() -> DriverManager.setDriver(d2, false))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    // =========================================================================
    // isDriverInitialized
    // =========================================================================

    @Nested
    @DisplayName("isDriverInitialized()")
    @Order(2)
    class IsDriverInitializedTests {

        @Test
        @DisplayName("Retorna false cuando no hay driver inicializado")
        void retornaFalseSinDriver() {
            assertThat(DriverManager.isDriverInitialized()).isFalse();
        }

        @Test
        @DisplayName("Retorna true cuando hay driver inicializado")
        void retornaTrueConDriver() {
            DriverManager.setDriver(new StubWebDriver());
            assertThat(DriverManager.isDriverInitialized()).isTrue();
        }
    }

    // =========================================================================
    // quitDriver
    // =========================================================================

    @Nested
    @DisplayName("quitDriver()")
    @Order(3)
    class QuitDriverTests {

        @Test
        @DisplayName("quitDriver cierra el driver y limpia el ThreadLocal")
        void quitDriverLimpiaThread() {
            StubWebDriver stub = new StubWebDriver();
            DriverManager.setDriver(stub);
            DriverManager.quitDriver();
            assertThat(DriverManager.isDriverInitialized()).isFalse();
            assertThat(stub.quitCalled).isTrue();
        }

        @Test
        @DisplayName("quitDriver no lanza excepción si no hay driver inicializado")
        void quitDriverSinDriverNoLanzaExcepcion() {
            assertThatNoException().isThrownBy(DriverManager::quitDriver);
        }

        @Test
        @DisplayName("quitDriver maneja excepción de quit() sin propagar")
        void quitDriverManejaExcepcionDeQuit() {
            StubWebDriver stub = new StubWebDriver();
            stub.quitShouldThrow = true;
            DriverManager.setDriver(stub);
            assertThatNoException().isThrownBy(DriverManager::quitDriver);
        }
    }

    // =========================================================================
    // ThreadLocal isolation
    // =========================================================================

    @Nested
    @DisplayName("ThreadLocal isolation")
    @Order(4)
    class ThreadLocalTests {

        @Test
        @DisplayName("Threads distintos tienen drivers distintos")
        void threadsSeparados() throws InterruptedException {
            StubWebDriver mainDriver = new StubWebDriver();
            AtomicBoolean childInitialized = new AtomicBoolean(false);

            DriverManager.setDriver(mainDriver);

            Thread child = new Thread(() ->
                childInitialized.set(DriverManager.isDriverInitialized())
            );
            child.start();
            child.join();

            assertThat(childInitialized.get()).isFalse();
            assertThat(DriverManager.isDriverInitialized()).isTrue();
        }
    }
}

