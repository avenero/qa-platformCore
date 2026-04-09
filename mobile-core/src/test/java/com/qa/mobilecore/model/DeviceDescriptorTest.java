package com.qa.mobilecore.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests unitarios para DeviceDescriptor y DeviceType.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("DeviceDescriptor")
class DeviceDescriptorTest {

    // =========================================================================

    @Nested
    @DisplayName("Builder — construccion basica")
    class BuilderBasicoTest {

        @Test
        @DisplayName("Debe crear descriptor con id y type obligatorios")
        void testBuildConCamposObligatorios() {
            DeviceDescriptor d = DeviceDescriptor
                    .builder("pixel6", DeviceType.ANDROID_EMULATOR)
                    .build();

            assertThat(d.getId()).isEqualTo("pixel6");
            assertThat(d.getType()).isEqualTo(DeviceType.ANDROID_EMULATOR);
        }

        @Test
        @DisplayName("Debe asignar platformName por defecto 'Android' para tipos Android")
        void testPlatformNameDefaultAndroid() {
            DeviceDescriptor d = DeviceDescriptor
                    .builder("dev1", DeviceType.ANDROID_PHYSICAL).build();
            assertThat(d.getPlatformName()).isEqualTo("Android");
        }

        @Test
        @DisplayName("Debe asignar platformName por defecto 'iOS' para tipos iOS")
        void testPlatformNameDefaultIos() {
            DeviceDescriptor d = DeviceDescriptor
                    .builder("sim1", DeviceType.IOS_SIMULATOR).build();
            assertThat(d.getPlatformName()).isEqualTo("iOS");
        }

        @Test
        @DisplayName("Appium URL por defecto debe ser 'http://localhost:4723'")
        void testAppiumUrlDefault() {
            DeviceDescriptor d = DeviceDescriptor
                    .builder("dev", DeviceType.ANDROID_EMULATOR).build();
            assertThat(d.getAppiumServerUrl()).isEqualTo("http://localhost:4723");
        }

        @Test
        @DisplayName("Los campos opcionales son null si no se configuran")
        void testCamposOpcionalesNull() {
            DeviceDescriptor d = DeviceDescriptor
                    .builder("dev", DeviceType.ANDROID_EMULATOR).build();
            assertThat(d.getPlatformVersion()).isNull();
            assertThat(d.getDeviceName()).isNull();
            assertThat(d.getUdid()).isNull();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("Builder — configuracion completa")
    class BuilderCompletoTest {

        @Test
        @DisplayName("Debe construir descriptor completo con todos los campos")
        void testBuildCompleto() {
            DeviceDescriptor d = DeviceDescriptor
                    .builder("pixel6", DeviceType.ANDROID_EMULATOR)
                    .platformName("Android")
                    .platformVersion("13")
                    .deviceName("Pixel_6_API_33")
                    .udid("emulator-5554")
                    .appiumServerUrl("http://localhost:4724")
                    .build();

            assertThat(d.getId()).isEqualTo("pixel6");
            assertThat(d.getType()).isEqualTo(DeviceType.ANDROID_EMULATOR);
            assertThat(d.getPlatformName()).isEqualTo("Android");
            assertThat(d.getPlatformVersion()).isEqualTo("13");
            assertThat(d.getDeviceName()).isEqualTo("Pixel_6_API_33");
            assertThat(d.getUdid()).isEqualTo("emulator-5554");
            assertThat(d.getAppiumServerUrl()).isEqualTo("http://localhost:4724");
        }

        @Test
        @DisplayName("Builder debe soportar API fluente (encadenamiento)")
        void testBuilderFluente() {
            DeviceDescriptor d = DeviceDescriptor
                    .builder("ios1", DeviceType.IOS_SIMULATOR)
                    .platformVersion("16.4")
                    .deviceName("iPhone 14 Pro")
                    .appiumServerUrl("http://localhost:4725")
                    .build();

            assertThat(d.getPlatformVersion()).isEqualTo("16.4");
            assertThat(d.getDeviceName()).isEqualTo("iPhone 14 Pro");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("Validacion de campos obligatorios")
    class ValidacionTest {

        @Test
        @DisplayName("id null debe lanzar NullPointerException")
        void testIdNullLanzaException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> DeviceDescriptor.builder(null, DeviceType.ANDROID_EMULATOR).build())
                    .withMessageContaining("id");
        }

        @Test
        @DisplayName("type null debe lanzar NullPointerException")
        void testTypeNullLanzaException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> DeviceDescriptor.builder("dev1", null).build())
                    .withMessageContaining("type");
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("Metodos de plataforma: isAndroid() / isIOS()")
    class PlataformaTest {

        @Test
        @DisplayName("ANDROID_EMULATOR debe ser Android y no iOS")
        void testAndroidEmulator() {
            DeviceDescriptor d = DeviceDescriptor.builder("d", DeviceType.ANDROID_EMULATOR).build();
            assertThat(d.isAndroid()).isTrue();
            assertThat(d.isIOS()).isFalse();
        }

        @Test
        @DisplayName("ANDROID_PHYSICAL debe ser Android y no iOS")
        void testAndroidPhysical() {
            DeviceDescriptor d = DeviceDescriptor.builder("d", DeviceType.ANDROID_PHYSICAL).build();
            assertThat(d.isAndroid()).isTrue();
            assertThat(d.isIOS()).isFalse();
        }

        @Test
        @DisplayName("IOS_SIMULATOR debe ser iOS y no Android")
        void testIosSimulator() {
            DeviceDescriptor d = DeviceDescriptor.builder("d", DeviceType.IOS_SIMULATOR).build();
            assertThat(d.isIOS()).isTrue();
            assertThat(d.isAndroid()).isFalse();
        }

        @Test
        @DisplayName("IOS_PHYSICAL debe ser iOS y no Android")
        void testIosPhysical() {
            DeviceDescriptor d = DeviceDescriptor.builder("d", DeviceType.IOS_PHYSICAL).build();
            assertThat(d.isIOS()).isTrue();
            assertThat(d.isAndroid()).isFalse();
        }

        @Test
        @DisplayName("REMOTE_GRID no es Android ni iOS")
        void testRemoteGrid() {
            DeviceDescriptor d = DeviceDescriptor.builder("d", DeviceType.REMOTE_GRID).build();
            assertThat(d.isAndroid()).isFalse();
            assertThat(d.isIOS()).isFalse();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("DeviceType enum")
    class DeviceTypeTest {

        @Test
        @DisplayName("ANDROID_EMULATOR: isVirtual=true, isPhysical=false")
        void testAndroidEmulatorFlags() {
            assertThat(DeviceType.ANDROID_EMULATOR.isVirtual()).isTrue();
            assertThat(DeviceType.ANDROID_EMULATOR.isPhysical()).isFalse();
        }

        @Test
        @DisplayName("ANDROID_PHYSICAL: isVirtual=false, isPhysical=true")
        void testAndroidPhysicalFlags() {
            assertThat(DeviceType.ANDROID_PHYSICAL.isVirtual()).isFalse();
            assertThat(DeviceType.ANDROID_PHYSICAL.isPhysical()).isTrue();
        }

        @Test
        @DisplayName("IOS_SIMULATOR: isVirtual=true, isPhysical=false")
        void testIosSimulatorFlags() {
            assertThat(DeviceType.IOS_SIMULATOR.isVirtual()).isTrue();
            assertThat(DeviceType.IOS_SIMULATOR.isPhysical()).isFalse();
        }

        @Test
        @DisplayName("IOS_PHYSICAL: isVirtual=false, isPhysical=true")
        void testIosPhysicalFlags() {
            assertThat(DeviceType.IOS_PHYSICAL.isVirtual()).isFalse();
            assertThat(DeviceType.IOS_PHYSICAL.isPhysical()).isTrue();
        }

        @Test
        @DisplayName("REMOTE_GRID: isVirtual=false, isPhysical=false")
        void testRemoteGridFlags() {
            assertThat(DeviceType.REMOTE_GRID.isVirtual()).isFalse();
            assertThat(DeviceType.REMOTE_GRID.isPhysical()).isFalse();
        }

        @ParameterizedTest(name = "DeviceType.{0} debe ser parseable via valueOf")
        @EnumSource(DeviceType.class)
        @DisplayName("Todos los tipos deben ser parseables via valueOf")
        void testAllTypesParseableViaValueOf(DeviceType type) {
            assertThat(DeviceType.valueOf(type.name())).isEqualTo(type);
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("toString()")
    class ToStringTest {

        @Test
        @DisplayName("toString debe contener id, type y appiumServerUrl")
        void testToStringContieneCamposClave() {
            DeviceDescriptor d = DeviceDescriptor
                    .builder("pixel6", DeviceType.ANDROID_EMULATOR)
                    .platformVersion("13")
                    .deviceName("Pixel_6_API_33")
                    .build();

            String str = d.toString();
            assertThat(str).contains("pixel6", "ANDROID_EMULATOR", "4723");
        }
    }
}
