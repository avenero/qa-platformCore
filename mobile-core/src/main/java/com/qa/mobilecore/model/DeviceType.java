package com.qa.mobilecore.model;

/**
 * Tipo de dispositivo mobile disponible para ejecución de pruebas.
 *
 * <p>Discrimina la estrategia de capabilities que {@code MobileDriverFactory}
 * construye y le permite a {@code AppiumServerManager} decidir si el servidor
 * necesita configuración especial (ej: conexión ADB, simctl, etc.).
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public enum DeviceType {

    /** Emulador Android (AVD). Solo requiere Android SDK + Appium. */
    ANDROID_EMULATOR,

    /** Dispositivo físico Android conectado por USB/WiFi (ADB). Requiere UDID. */
    ANDROID_PHYSICAL,

    /** Simulador iOS (simctl / Xcode). Solo macOS. */
    IOS_SIMULATOR,

    /** Dispositivo físico iOS. Solo macOS + Apple Developer cert + UDID. */
    IOS_PHYSICAL,

    /**
     * Dispositivo gestionado por un Appium Grid / Cloud (BrowserStack, Sauce Labs,
     * grid interno en Docker/Kubernetes). El grid asigna el dispositivo real.
     */
    REMOTE_GRID;

    /**
     * Indicates whether the platform is Android.
     *
     * @return true si la plataforma es Android (emulador o fisico).
     */
    public boolean isAndroid() {
        return this == ANDROID_EMULATOR || this == ANDROID_PHYSICAL;
    }

    /**
     * Indicates whether the platform is iOS.
     *
     * @return true si la plataforma es iOS (simulador o fisico).
     */
    public boolean isIOS() {
        return this == IOS_SIMULATOR || this == IOS_PHYSICAL;
    }

    /**
     * Indicates whether the device is a virtual device (emulator or simulator).
     *
     * @return true si se ejecuta en un emulador/simulador (no hardware real).
     */
    public boolean isVirtual() {
        return this == ANDROID_EMULATOR || this == IOS_SIMULATOR;
    }

    /**
     * Indicates whether the device is physical hardware.
     *
     * @return true si se ejecuta en hardware fisico.
     */
    public boolean isPhysical() {
        return this == ANDROID_PHYSICAL || this == IOS_PHYSICAL;
    }
}
