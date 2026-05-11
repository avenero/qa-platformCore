package com.qa.common.api.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Duration;

/**
 * Configuración Mobile/Appium del Core (TASK-K03 + TASK-K03M-F6.a).
 *
 * <p>Prefijo: {@code mobile}.
 *
 * <p>Cubre todas las claves de {@code com.qa.mobilecore.config.MobileConfigKeys}.
 * Las keys legacy con puntos (ej. {@code mobile.appium.server.url}) se mapean
 * a las canónicas camelCase (ej. {@code mobile.appiumServerUrl}) vía
 * {@code LegacyKeyAliasSource} (TASK-K03M-F7).
 *
 * @since TASK-K03
 */
public record MobileConfig(
        @NotBlank @Pattern(regexp = "android|ios|ANDROID|IOS",
                message = "platform debe ser android o ios")
        String platform,
        @NotNull String platformVersion,
        @NotNull String deviceId,
        @NotNull String deviceName,
        @NotBlank @Pattern(
                regexp = "ANDROID_EMULATOR|ANDROID_PHYSICAL|IOS_SIMULATOR|IOS_PHYSICAL|REMOTE_GRID",
                message = "deviceType debe ser ANDROID_EMULATOR | ANDROID_PHYSICAL | IOS_SIMULATOR | IOS_PHYSICAL | REMOTE_GRID")
        String deviceType,
        @NotNull String udid,
        @NotBlank @Pattern(regexp = "^https?://.*",
                message = "appiumServerUrl debe empezar con http:// o https://")
        String appiumServerUrl,
        @Min(1024) @Max(65535) int appiumBasePort,
        boolean appiumAutoStart,
        @NotNull Duration appiumStartupTimeout,
        @Min(0) int implicitWaitSec,
        @NotNull String appPath,
        @NotNull String appPackage,
        @NotNull String appActivity,
        @NotNull String bundleId,
        boolean appAutoLaunch,
        boolean appNoReset,
        @NotNull Duration newCommandTimeout,
        @Min(1) @Max(16) int devicePoolSize,
        boolean discoveryAutoScan,
        boolean discoveryIncludeVirtual,
        boolean discoveryIncludePhysical
) implements TypedConfig {

    public static MobileConfig defaults() {
        return new MobileConfig(
                "android",
                "",
                "",
                "",
                "ANDROID_EMULATOR",
                "",
                "http://localhost:4723",
                4723,
                false,
                Duration.ofSeconds(30),
                10,
                "",
                "",
                "",
                "",
                true,
                false,
                Duration.ofSeconds(120),
                1,
                true,
                true,
                true
        );
    }
}
