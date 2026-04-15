package com.qa.mobilecore.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para {@link MobileConfigKeys}.
 *
 * <p>Verifica que:
 * <ul>
 *   <li>Las claves obligatorias (requeridas por la tarea) tienen el valor correcto</li>
 *   <li>Todas las constantes son {@code public static final String}</li>
 *   <li>No hay claves duplicadas (dos constantes con el mismo valor)</li>
 *   <li>El constructor privado no es instanciable</li>
 * </ul>
 *
 * @author Abel Venero
 * @since 2.2.0
 */
@DisplayName("MobileConfigKeys")
class MobileConfigKeysTest {

    // =========================================================================
    // Claves obligatorias — requeridas por la especificación de la tarea
    // =========================================================================

    @Nested
    @DisplayName("Claves obligatorias (especificación)")
    class ClavesObligatorias {

        @Test
        @DisplayName("PLATFORM tiene el valor 'mobile.platform'")
        void testPlatform() {
            assertThat(MobileConfigKeys.PLATFORM).isEqualTo("mobile.platform");
        }

        @Test
        @DisplayName("DEVICE_ID tiene el valor 'mobile.device.id'")
        void testDeviceId() {
            assertThat(MobileConfigKeys.DEVICE_ID).isEqualTo("mobile.device.id");
        }

        @Test
        @DisplayName("APPIUM_SERVER_URL tiene el valor 'mobile.appium.server.url'")
        void testAppiumServerUrl() {
            assertThat(MobileConfigKeys.APPIUM_SERVER_URL).isEqualTo("mobile.appium.server.url");
        }

        @Test
        @DisplayName("APP_PATH tiene el valor 'mobile.app.path'")
        void testAppPath() {
            assertThat(MobileConfigKeys.APP_PATH).isEqualTo("mobile.app.path");
        }
    }

    // =========================================================================
    // Claves de dispositivo
    // =========================================================================

    @Nested
    @DisplayName("Claves de dispositivo")
    class ClavesDispositivo {

        @Test
        @DisplayName("DEVICE_TYPE tiene el valor 'mobile.device.type'")
        void testDeviceType() {
            assertThat(MobileConfigKeys.DEVICE_TYPE).isEqualTo("mobile.device.type");
        }

        @Test
        @DisplayName("PLATFORM_VERSION tiene el valor 'mobile.platform.version'")
        void testPlatformVersion() {
            assertThat(MobileConfigKeys.PLATFORM_VERSION).isEqualTo("mobile.platform.version");
        }

        @Test
        @DisplayName("DEVICE_NAME tiene el valor 'mobile.device.name'")
        void testDeviceName() {
            assertThat(MobileConfigKeys.DEVICE_NAME).isEqualTo("mobile.device.name");
        }

        @Test
        @DisplayName("UDID tiene el valor 'mobile.udid'")
        void testUdid() {
            assertThat(MobileConfigKeys.UDID).isEqualTo("mobile.udid");
        }
    }

    // =========================================================================
    // Claves de Appium
    // =========================================================================

    @Nested
    @DisplayName("Claves de Appium")
    class ClavesAppium {

        @Test
        @DisplayName("APPIUM_BASE_PORT tiene el valor 'mobile.appium.base.port'")
        void testAppiumBasePort() {
            assertThat(MobileConfigKeys.APPIUM_BASE_PORT).isEqualTo("mobile.appium.base.port");
        }

        @Test
        @DisplayName("APPIUM_AUTO_START tiene el valor 'mobile.appium.auto.start'")
        void testAppiumAutoStart() {
            assertThat(MobileConfigKeys.APPIUM_AUTO_START).isEqualTo("mobile.appium.auto.start");
        }

        @Test
        @DisplayName("APPIUM_STARTUP_TIMEOUT_SEC tiene el valor 'mobile.appium.startup.timeout.sec'")
        void testAppiumStartupTimeoutSec() {
            assertThat(MobileConfigKeys.APPIUM_STARTUP_TIMEOUT_SEC)
                    .isEqualTo("mobile.appium.startup.timeout.sec");
        }

        @Test
        @DisplayName("IMPLICIT_WAIT_SEC tiene el valor 'mobile.implicit.wait.sec'")
        void testImplicitWaitSec() {
            assertThat(MobileConfigKeys.IMPLICIT_WAIT_SEC).isEqualTo("mobile.implicit.wait.sec");
        }
    }

    // =========================================================================
    // Claves de aplicación
    // =========================================================================

    @Nested
    @DisplayName("Claves de aplicación")
    class ClavesApp {

        @Test
        @DisplayName("APP_PACKAGE tiene el valor 'mobile.app.package'")
        void testAppPackage() {
            assertThat(MobileConfigKeys.APP_PACKAGE).isEqualTo("mobile.app.package");
        }

        @Test
        @DisplayName("APP_ACTIVITY tiene el valor 'mobile.app.activity'")
        void testAppActivity() {
            assertThat(MobileConfigKeys.APP_ACTIVITY).isEqualTo("mobile.app.activity");
        }

        @Test
        @DisplayName("BUNDLE_ID tiene el valor 'mobile.app.bundle.id'")
        void testBundleId() {
            assertThat(MobileConfigKeys.BUNDLE_ID).isEqualTo("mobile.app.bundle.id");
        }

        @Test
        @DisplayName("APP_AUTO_LAUNCH tiene el valor 'mobile.app.auto.launch'")
        void testAppAutoLaunch() {
            assertThat(MobileConfigKeys.APP_AUTO_LAUNCH).isEqualTo("mobile.app.auto.launch");
        }

        @Test
        @DisplayName("APP_NO_RESET tiene el valor 'mobile.app.no.reset'")
        void testAppNoReset() {
            assertThat(MobileConfigKeys.APP_NO_RESET).isEqualTo("mobile.app.no.reset");
        }
    }

    // =========================================================================
    // Claves de descubrimiento
    // =========================================================================

    @Nested
    @DisplayName("Claves de pool y descubrimiento")
    class ClavesDescubrimiento {

        @Test
        @DisplayName("DISCOVERY_AUTO_SCAN tiene el valor 'mobile.discovery.auto.scan'")
        void testDiscoveryAutoScan() {
            assertThat(MobileConfigKeys.DISCOVERY_AUTO_SCAN).isEqualTo("mobile.discovery.auto.scan");
        }

        @Test
        @DisplayName("DISCOVERY_INCLUDE_VIRTUAL tiene el valor 'mobile.discovery.include.virtual'")
        void testDiscoveryIncludeVirtual() {
            assertThat(MobileConfigKeys.DISCOVERY_INCLUDE_VIRTUAL)
                    .isEqualTo("mobile.discovery.include.virtual");
        }

        @Test
        @DisplayName("DISCOVERY_INCLUDE_PHYSICAL tiene el valor 'mobile.discovery.include.physical'")
        void testDiscoveryIncludePhysical() {
            assertThat(MobileConfigKeys.DISCOVERY_INCLUDE_PHYSICAL)
                    .isEqualTo("mobile.discovery.include.physical");
        }
    }

    // =========================================================================
    // Estructura de la clase
    // =========================================================================

    @Nested
    @DisplayName("Estructura de la clase")
    class EstructuraClase {

        @Test
        @DisplayName("El constructor es privado — no instanciable desde fuera")
        void testConstructorPrivado() throws Exception {
            Constructor<MobileConfigKeys> constructor =
                    MobileConfigKeys.class.getDeclaredConstructor();
            assertThat(Modifier.isPrivate(constructor.getModifiers()))
                    .as("El constructor debe ser private para impedir instanciación externa")
                    .isTrue();
        }

        @Test
        @DisplayName("Todas las constantes son public static final String")
        void testTodasConstantesPublicStaticFinalString() {
            List<Field> camposPublicos = Arrays.stream(MobileConfigKeys.class.getDeclaredFields())
                    .filter(f -> !f.isSynthetic())
                    .toList();

            assertThat(camposPublicos).isNotEmpty();

            for (Field f : camposPublicos) {
                int mods = f.getModifiers();
                assertThat(Modifier.isPublic(mods))
                        .as("Campo '%s' debe ser public", f.getName()).isTrue();
                assertThat(Modifier.isStatic(mods))
                        .as("Campo '%s' debe ser static", f.getName()).isTrue();
                assertThat(Modifier.isFinal(mods))
                        .as("Campo '%s' debe ser final", f.getName()).isTrue();
                assertThat(f.getType())
                        .as("Campo '%s' debe ser String", f.getName()).isEqualTo(String.class);
            }
        }

        @Test
        @DisplayName("No hay claves duplicadas entre constantes")
        void testSinValoresDuplicados() throws Exception {
            Field[] campos = MobileConfigKeys.class.getDeclaredFields();

            List<String> valores = Arrays.stream(campos)
                    .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
                    .map(f -> {
                        try { return (String) f.get(null); }
                        catch (IllegalAccessException e) { return null; }
                    })
                    .toList();

            long distintos = valores.stream().distinct().count();
            assertThat(distintos)
                    .as("Hay claves con el mismo valor (duplicados): %s", valores)
                    .isEqualTo(valores.size());
        }

        @Test
        @DisplayName("Ninguna constante tiene valor null ni vacío")
        void testSinValoresNullOVacios() throws Exception {
            Field[] campos = MobileConfigKeys.class.getDeclaredFields();

            for (Field f : campos) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                    String valor = (String) f.get(null);
                    assertThat(valor)
                            .as("La constante '%s' tiene valor null o vacío", f.getName())
                            .isNotNull()
                            .isNotBlank();
                }
            }
        }
    }
}
