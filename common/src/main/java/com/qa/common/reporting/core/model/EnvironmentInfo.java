package com.qa.common.reporting.core.model;
import com.qa.common.utils.security.SecurityUtilities;

import java.util.HashMap;
import java.util.Map;

/**
 * Información del entorno de ejecución (OS, browser, device, etc.).
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 1.0.0
 */
public class EnvironmentInfo {

    private String os;
    private String osVersion;
    private String javaVersion;

    // Web testing
    private String browser;
    private String browserVersion;

    // Mobile testing
    private String device;
    private String platform;
    private String platformVersion;
    private String appVersion;

    // Custom info
    private Map<String, String> customInfo;

    public EnvironmentInfo() {
        this.customInfo = new HashMap<>();
        autoDetectSystemInfo();
    }

    /**
     * Auto-detecta información del sistema operativo y JVM.
     */
    private void autoDetectSystemInfo() {
        this.os = System.getProperty("os.name");
        this.osVersion = System.getProperty("os.version");
        this.javaVersion = System.getProperty("java.version");
    }

    /**
     * Agrega información personalizada al mapa de propiedades custom.
     *
     * @param key   clave de la propiedad
     * @param value valor de la propiedad
     */
    public void addCustomInfo(String key, String value) {
        this.customInfo.put(key, value);
    }

    // Getters and Setters

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public void setBrowserVersion(String browserVersion) {
        this.browserVersion = browserVersion;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public void setPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public Map<String, String> getCustomInfo() {
        return customInfo;
    }

    public void setCustomInfo(Map<String, String> customInfo) {
        this.customInfo = customInfo;
    }

    @Override
    public String toString() {
        return "EnvironmentInfo{" +
                "os='" + os + '\'' +
                ", javaVersion='" + javaVersion + '\'' +
                ", browser='" + browser + '\'' +
                ", device='" + device + '\'' +
                '}';
    }
}

