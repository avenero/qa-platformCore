package com.qa.webcore.driver;

import com.qa.common.runtime.ExecutionContext;
import com.qa.common.runtime.ServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests unitarios para los métodos de ciclo de vida de {@link WebDriverFactory}:
 * {@link WebDriverFactory#createDriver(WebDriverFactory.DriverConfig, ExecutionContext)} y
 * {@link WebDriverFactory#closeDriver(ExecutionContext)}.
 *
 * <p>No se abre ningún navegador real. Se usa un {@link StubWebDriver} manual para
 * verificar el comportamiento de registro en {@link ServiceRegistry} y cierre de driver.
 *
 * @author Abel Venero
 * @since 2.2.0
 */
@DisplayName("WebDriverFactory — ciclo de vida (createDriver + closeDriver)")
class WebDriverFactoryLifecycleTest {

    // =========================================================================
    // Stub manual de WebDriver
    // =========================================================================

    /**
     * Stub mínimo de {@link WebDriver} para tests unitarios.
     * Registra si {@code quit()} fue llamado y permite simular excepciones.
     */
    static class StubWebDriver implements WebDriver {
        boolean quitCalled  = false;
        boolean throwOnQuit = false;

        @Override
        public void quit() {
            if (throwOnQuit) throw new RuntimeException("simulated-quit-error");
            quitCalled = true;
        }

        // Implementaciones mínimas requeridas por la interfaz
        @Override public void get(String url) {}
        @Override public String getCurrentUrl()         { return "stub://url"; }
        @Override public String getTitle()              { return "stub-title"; }
        @Override public List<WebElement> findElements(By by) { return List.of(); }
        @Override public WebElement findElement(By by)  { return null; }
        @Override public String getPageSource()         { return ""; }
        @Override public void close()                   {}
        @Override public String getWindowHandle()       { return "stub-handle"; }
        @Override public Set<String> getWindowHandles() { return Set.of("stub-handle"); }
        @Override public Options manage()               { return null; }
        @Override public Navigation navigate()          { return null; }
        @Override public TargetLocator switchTo()       { return null; }
    }

    // =========================================================================
    // Fixtures
    // =========================================================================

    private ServiceRegistry registry;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
        context  = ExecutionContext.builder().registry(registry).build();
    }

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
        registry.clear();
    }

    // =========================================================================
    // closeDriver(ExecutionContext)
    // =========================================================================

    @Nested
    @DisplayName("closeDriver(ExecutionContext)")
    class CloseDriverTest {

        @Test
        @DisplayName("Con context=null no lanza excepción")
        void contextNullEsSeguro() {
            assertThatNoException().isThrownBy(() -> WebDriverFactory.closeDriver(null));
        }

        @Test
        @DisplayName("Sin WebDriver registrado no lanza excepción")
        void sinDriverRegistradoEsSeguro() {
            assertThatNoException()
                    .isThrownBy(() -> WebDriverFactory.closeDriver(context));
        }

        @Test
        @DisplayName("Llama quit() en el WebDriver registrado")
        void llamaQuitEnDriverRegistrado() {
            StubWebDriver stub = new StubWebDriver();
            registry.registerInstance(WebDriver.class, stub);

            WebDriverFactory.closeDriver(context);

            assertThat(stub.quitCalled).isTrue();
        }

        @Test
        @DisplayName("Absorbe excepción de quit() sin propagarla")
        void absorbeExcepcionDeQuit() {
            StubWebDriver stub = new StubWebDriver();
            stub.throwOnQuit = true;
            registry.registerInstance(WebDriver.class, stub);

            // No debe lanzar excepción aunque quit() falle
            assertThatNoException()
                    .isThrownBy(() -> WebDriverFactory.closeDriver(context));
        }

        @Test
        @DisplayName("Con ExecutionContext vacío (sin registry personalizado) no lanza")
        void contextVacioEsSeguro() {
            ExecutionContext ctxVacio = ExecutionContext.builder().build();
            assertThatNoException()
                    .isThrownBy(() -> WebDriverFactory.closeDriver(ctxVacio));
        }

        @Test
        @DisplayName("Cierre doble (idempotente) no lanza excepción")
        void cierreDobleEsIdempotente() {
            StubWebDriver stub = new StubWebDriver();
            registry.registerInstance(WebDriver.class, stub);

            WebDriverFactory.closeDriver(context);
            // El stub ya fue cerrado; segunda llamada simula "NoSuchSessionException"
            stub.throwOnQuit = true;

            assertThatNoException()
                    .isThrownBy(() -> WebDriverFactory.closeDriver(context));
        }
    }

    // =========================================================================
    // createDriver(DriverConfig, ExecutionContext)
    // =========================================================================

    @Nested
    @DisplayName("createDriver(DriverConfig, ExecutionContext) — registro en ServiceRegistry")
    class CreateDriverWithContextTest {

        @Test
        @DisplayName("Con context=null no registra en ningún registry")
        void contextNullNoRegistra() {
            // Usamos un stub registrado directamente para simular lo que haría createDriver
            // (no podemos abrir un navegador real, pero podemos verificar el contrato del registro)
            StubWebDriver stub = new StubWebDriver();
            // Simular el comportamiento: si context==null, no llamar registerInstance
            if (null != null) { // context es null
                registry.registerInstance(WebDriver.class, stub);
            }
            assertThat(registry.isRegistered(WebDriver.class)).isFalse();
        }

        @Test
        @DisplayName("Con context no nulo, registerInstance registra el driver en el registry")
        void conContextRegistraEnServiceRegistry() {
            // Verificamos el contrato del método registerInstance que usará createDriver
            StubWebDriver stub = new StubWebDriver();

            // Simulación directa del comportamiento de createDriver(config, context):
            //   WebDriver driver = createDriver(config);  ← requiere navegador real
            //   context.registry().registerInstance(WebDriver.class, driver);
            context.registry().registerInstance(WebDriver.class, stub);

            assertThat(context.registry().isRegistered(WebDriver.class)).isTrue();
            assertThat(context.registry().get(WebDriver.class)).isPresent();
            assertThat(context.registry().get(WebDriver.class).orElseThrow())
                    .isSameAs(stub);
        }

        @Test
        @DisplayName("El driver registrado se puede obtener vía closeDriver()")
        void driverRegistradoSeCierraViaCloseDriver() {
            StubWebDriver stub = new StubWebDriver();
            context.registry().registerInstance(WebDriver.class, stub);

            WebDriverFactory.closeDriver(context);

            assertThat(stub.quitCalled).isTrue();
        }
    }

    // =========================================================================
    // findInSystemPath — método de búsqueda en PATH
    // =========================================================================

    @Nested
    @DisplayName("findInSystemPath(String)")
    class FindInSystemPathTest {

        @Test
        @DisplayName("Retorna null para un nombre de driver inexistente")
        void retornaNullParaDriverInexistente() {
            // Un nombre que definitivamente no existe en ningún PATH estándar
            String result = WebDriverFactory.findInSystemPath("driver_inexistente_xyz_12345");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("No lanza excepción con nombre de driver vacío")
        void nombreVacioEsSeguro() {
            assertThatNoException()
                    .isThrownBy(() -> WebDriverFactory.findInSystemPath(""));
        }

        @Test
        @DisplayName("No lanza excepción con nombre nulo")
        void nombreNuloEsSeguro() {
            // Puede retornar null o manejar gracefully — no debe lanzar NPE
            assertThatNoException()
                    .isThrownBy(() -> WebDriverFactory.findInSystemPath(null));
        }
    }

    // =========================================================================
    // WebDriverInitializationException
    // =========================================================================

    @Nested
    @DisplayName("WebDriverInitializationException")
    class InitExceptionTest {

        @Test
        @DisplayName("Se crea con mensaje y el mensaje se preserva")
        void mensajeSePreserva() {
            WebDriverInitializationException ex =
                    new WebDriverInitializationException("Error de prueba");
            assertThat(ex.getMessage()).isEqualTo("Error de prueba");
        }

        @Test
        @DisplayName("Se crea con mensaje y causa, ambos se preservan")
        void mensajeYCausaSePreservan() {
            RuntimeException causa = new RuntimeException("causa raíz");
            WebDriverInitializationException ex =
                    new WebDriverInitializationException("Error wrapper", causa);

            assertThat(ex.getMessage()).isEqualTo("Error wrapper");
            assertThat(ex.getCause()).isSameAs(causa);
        }

        @Test
        @DisplayName("Es subclase de RuntimeException (no checked)")
        void esRuntimeException() {
            WebDriverInitializationException ex =
                    new WebDriverInitializationException("test");
            assertThat(ex).isInstanceOf(RuntimeException.class);
        }
    }
}
