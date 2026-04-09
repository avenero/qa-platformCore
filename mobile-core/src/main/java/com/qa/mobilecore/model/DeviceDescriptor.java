package com.qa.mobilecore.model;

import java.util.Objects;

/**
 * Descriptor inmutable de un dispositivo mobile.
 *
 * <p>Representa la identidad de un dispositivo (físico, emulador o simulador)
 * independientemente de las capabilities específicas de la app bajo prueba.
 * Las capabilities de la app se leen de {@code ExecutionConfig} en tiempo de ejecución.
 *
 * <p><b>Estrategia de localización de Appium:</b> el campo {@code appiumServerUrl}
 * contiene el puerto asignado por el {@code DevicePool} (ej: {@code http://localhost:4723}).
 * Dispositivos en paralelo reciben puertos distintos (4723, 4724, 4725...) para
 * evitar colisiones entre sesiones Appium simultáneas.
 *
 * <p><b>Ejemplo:</b>
 * <pre>
 * DeviceDescriptor d = DeviceDescriptor.builder("pixel6", DeviceType.ANDROID_EMULATOR)
 *     .platformVersion("13")
 *     .deviceName("Pixel_6_API_33")
 *     .appiumServerUrl("http://localhost:4723")
 *     .build();
 * </pre>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class DeviceDescriptor {

    private final String id;
    private final DeviceType type;
    private final String platformName;
    private final String platformVersion;
    private final String deviceName;
    private final String udid;
    private final String appiumServerUrl;

    private DeviceDescriptor(Builder b) {
        this.id              = Objects.requireNonNull(b.id,   "id no puede ser null");
        this.type            = Objects.requireNonNull(b.type, "type no puede ser null");
        this.platformName    = b.platformName;
        this.platformVersion = b.platformVersion;
        this.deviceName      = b.deviceName;
        this.udid            = b.udid;
        this.appiumServerUrl = b.appiumServerUrl != null ? b.appiumServerUrl : "http://localhost:4723";
    }

    // --- Getters ---

    public String getId()              { return id; }
    public DeviceType getType()        { return type; }
    public String getPlatformName()    { return platformName; }
    public String getPlatformVersion() { return platformVersion; }
    public String getDeviceName()      { return deviceName; }
    public String getUdid()            { return udid; }
    public String getAppiumServerUrl() { return appiumServerUrl; }

    /** @return true si el dispositivo es Android (emulador o físico). */
    public boolean isAndroid() { return type.isAndroid(); }

    /** @return true si el dispositivo es iOS (simulador o físico). */
    public boolean isIOS() { return type.isIOS(); }

    @Override
    public String toString() {
        return "DeviceDescriptor{id='" + id + "', type=" + type
                + ", platform='" + platformName + " " + platformVersion
                + "', device='" + deviceName + "', url='" + appiumServerUrl + "'}";
    }

    // --- Builder ---

    public static Builder builder(String id, DeviceType type) {
        return new Builder(id, type);
    }

    public static final class Builder {

        private final String id;
        private final DeviceType type;
        private String platformName;
        private String platformVersion;
        private String deviceName;
        private String udid;
        private String appiumServerUrl;

        private Builder(String id, DeviceType type) {
            this.id   = id;
            this.type = type;
            // Plataforma por defecto según tipo
            this.platformName = type.isAndroid() ? "Android" : "iOS";
        }

        public Builder platformName(String platformName) {
            this.platformName = platformName;
            return this;
        }

        public Builder platformVersion(String platformVersion) {
            this.platformVersion = platformVersion;
            return this;
        }

        public Builder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public Builder udid(String udid) {
            this.udid = udid;
            return this;
        }

        public Builder appiumServerUrl(String url) {
            this.appiumServerUrl = url;
            return this;
        }

        public DeviceDescriptor build() {
            return new DeviceDescriptor(this);
        }
    }
}
