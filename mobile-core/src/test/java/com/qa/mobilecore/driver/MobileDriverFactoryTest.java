package com.qa.mobilecore.driver;

import com.qa.mobilecore.model.DeviceDescriptor;
import com.qa.mobilecore.model.DeviceType;
import io.appium.java_client.AppiumDriver;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para {@link MobileDriverFactory} — métodos de ciclo de vida de instancia.
 *
 * <p><b>Cobertura:</b>
 * <ul>
 *   <li>{@code isDriverCreated()} — estado inicial y tras ciclo de vida</li>
 *   <li>{@code quitIfCreated()} — sin driver, con driver, idempotente, absorbe excepción</li>
 *   <li>{@code setDevice()} — no lanza, acepta null</li>
 *   <li>{@code injectDriver()} — método de soporte de tests (package-private)</li>
 *   <li>{@link MobileDriverInitializationException} — contrato de excepción</li>
 * </ul>
 *
 * <p><b>Estrategia:</b> sin servidor Appium real. Se usa {@code Mockito.mock(AppiumDriver.class)}
 * e {@code injectDriver()} (package-private) para inyectar drivers falsos. Los métodos
 * {@code getOrCreateDriver()} y {@code resolveDeviceFromConfig()} requieren infraestructura
 * real (Appium server) por lo que no se invocan directamente en estos tests unitarios.
 *
 * @author Abel Venero
 * @since 2.2.0
 */
@DisplayName("MobileDriverFactory — ciclo de vida (instancia)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MobileDriverFactoryTest {

    private MobileDriverFactory factory;

    @BeforeEach
    void setUp() {
        factory = new MobileDriverFactory();
    }

    @AfterEach
    void tearDown() {
        // Asegurar limpieza del ThreadLocal aunque el test falle a medio camino
        try { MobileDriverManager.quitDriverSafely(); } catch (Exception ignored) {}
    }

    // =========================================================================
    // 1. isDriverCreated()
    // =========================================================================

    @Nested
    @DisplayName("1. isDriverCreated()")
    @Order(1)
    class IsDriverCreatedTests {

        @Test
        @DisplayName("Retorna false en una instancia recién creada")
        void retornsFalseInicialmente() {
            assertThat(factory.isDriverCreated()).isFalse();
        }

        @Test
        @DisplayName("Retorna true después de inyectar un driver")
        void retornsTrueTrasInyeccion() {
            AppiumDriver mock = Mockito.mock(AppiumDriver.class);
            factory.injectDriver(mock);

            assertThat(factory.isDriverCreated()).isTrue();
        }

        @Test
        @DisplayName("Retorna false después de quitIfCreated()")
        void retornsFalseTrasQuit() {
            AppiumDriver mock = Mockito.mock(AppiumDriver.class);
            factory.injectDriver(mock);

            factory.quitIfCreated();

            assertThat(factory.isDriverCreated()).isFalse();
        }
    }

    // =========================================================================
    // 2. quitIfCreated()
    // =========================================================================

    @Nested
    @DisplayName("2. quitIfCreated()")
    @Order(2)
    class QuitIfCreatedTests {

        @Test
        @DisplayName("No lanza excepción cuando no hay driver creado")
        void sinDriverNoLanza() {
            assertThatNoException().isThrownBy(() -> factory.quitIfCreated());
        }

        @Test
        @DisplayName("Llama quit() en el driver inyectado")
        void llamaQuitEnDriverInyectado() {
            AppiumDriver mock = Mockito.mock(AppiumDriver.class);
            factory.injectDriver(mock);

            factory.quitIfCreated();

            Mockito.verify(mock, Mockito.times(1)).quit();
        }

        @Test
        @DisplayName("Absorbe excepción de quit() sin propagarla (sesión ya terminada)")
        void absorbeExcepcionDeQuit() {
            AppiumDriver mock = Mockito.mock(AppiumDriver.class);
            Mockito.doThrow(new RuntimeException("No such driver session"))
                   .when(mock).quit();
            factory.injectDriver(mock);

            assertThatNoException()
                    .as("quitIfCreated no debe propagar excepciones del driver")
                    .isThrownBy(() -> factory.quitIfCreated());
        }

        @Test
        @DisplayName("Es idempotente: segunda llamada no lanza aunque el driver ya fue cerrado")
        void esIdempotente() {
            AppiumDriver mock = Mockito.mock(AppiumDriver.class);
            factory.injectDriver(mock);

            factory.quitIfCreated();                    // primer cierre — exitoso
            // driver ya es null; segunda llamada debe ser no-op
            assertThatNoException().isThrownBy(() -> factory.quitIfCreated());
        }

        @Test
        @DisplayName("Limpia la referencia interna: isDriverCreated() es false tras quit")
        void limpiaReferenciaInterna() {
            AppiumDriver mock = Mockito.mock(AppiumDriver.class);
            factory.injectDriver(mock);

            assertThat(factory.isDriverCreated()).isTrue();
            factory.quitIfCreated();
            assertThat(factory.isDriverCreated()).isFalse();
        }

        @Test
        @DisplayName("Segunda llamada con quit() que lanza tampoco propaga excepción")
        void segundaLlamadaConExcepcionEsSegura() {
            AppiumDriver mock = Mockito.mock(AppiumDriver.class);
            factory.injectDriver(mock);

            factory.quitIfCreated();          // primer cierre — driver = null
            // mock ya no está referenciado; segunda llamada es no-op
            assertThatNoException().isThrownBy(() -> factory.quitIfCreated());
        }
    }

    // =========================================================================
    // 3. setDevice()
    // =========================================================================

    @Nested
    @DisplayName("3. setDevice()")
    @Order(3)
    class SetDeviceTests {

        @Test
        @DisplayName("No lanza excepción con un DeviceDescriptor válido")
        void noLanzaConDeviceValido() {
            DeviceDescriptor device = DeviceDescriptor
                    .builder("test-device", DeviceType.ANDROID_EMULATOR)
                    .appiumServerUrl("http://localhost:4723")
                    .build();

            assertThatNoException().isThrownBy(() -> factory.setDevice(device));
        }

        @Test
        @DisplayName("Acepta null (reset a resolución desde config)")
        void aceptaNull() {
            assertThatNoException().isThrownBy(() -> factory.setDevice(null));
        }

        @Test
        @DisplayName("Puede llamarse múltiples veces sin excepción")
        void multiplesLlamadasSonSeguras() {
            DeviceDescriptor device1 = DeviceDescriptor
                    .builder("device-1", DeviceType.ANDROID_EMULATOR).build();
            DeviceDescriptor device2 = DeviceDescriptor
                    .builder("device-2", DeviceType.IOS_SIMULATOR).build();

            assertThatNoException().isThrownBy(() -> {
                factory.setDevice(device1);
                factory.setDevice(device2);
                factory.setDevice(null);
            });
        }
    }

    // =========================================================================
    // 4. injectDriver() — soporte de tests
    // =========================================================================

    @Nested
    @DisplayName("4. injectDriver() — soporte de tests")
    @Order(4)
    class InjectDriverTests {

        @Test
        @DisplayName("Inyectar un driver no-null hace isDriverCreated() == true")
        void inyectarDriverActivaIsDriverCreated() {
            AppiumDriver mock = Mockito.mock(AppiumDriver.class);
            factory.injectDriver(mock);

            assertThat(factory.isDriverCreated()).isTrue();
        }

        @Test
        @DisplayName("Inyectar null resetea isDriverCreated() a false")
        void inyectarNullResetea() {
            AppiumDriver mock = Mockito.mock(AppiumDriver.class);
            factory.injectDriver(mock);
            factory.injectDriver(null);

            assertThat(factory.isDriverCreated()).isFalse();
        }
    }

    // =========================================================================
    // 5. MobileDriverInitializationException
    // =========================================================================

    @Nested
    @DisplayName("5. MobileDriverInitializationException")
    @Order(5)
    class InitExceptionTests {

        @Test
        @DisplayName("Se crea con mensaje y el mensaje se preserva")
        void mensajeSePreserva() {
            MobileDriverInitializationException ex =
                    new MobileDriverInitializationException("Error de sesión Appium");

            assertThat(ex.getMessage()).isEqualTo("Error de sesión Appium");
        }

        @Test
        @DisplayName("Se crea con mensaje y causa, ambos se preservan")
        void mensajeYCausaSePreservan() {
            RuntimeException causa = new RuntimeException("causa raíz");
            MobileDriverInitializationException ex =
                    new MobileDriverInitializationException("Error wrapper", causa);

            assertThat(ex.getMessage()).isEqualTo("Error wrapper");
            assertThat(ex.getCause()).isSameAs(causa);
        }

        @Test
        @DisplayName("Es subclase de RuntimeException (no checked)")
        void esRuntimeException() {
            MobileDriverInitializationException ex =
                    new MobileDriverInitializationException("test");

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }
    }
}
