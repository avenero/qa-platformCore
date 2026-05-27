package com.qa.common.api.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Duration;

/**
 * Configuración Web/Browser del Core (TASK-K03 + TASK-K03M-F6.b).
 *
 * <p>Prefijo: {@code web}. Las claves canónicas viven en
 * {@code com.qa.webcore.config.WebConfigKeys}; este record es la vista
 * tipada agnóstica al engine.
 *
 * <p>Las keys legacy (ej. {@code web.page.load.timeout.sec}) se mapean a las
 * canónicas camelCase vía {@code LegacyKeyAliasSource} (TASK-K03M-F7).
 *
 * <p>Keys NO incluidas en el record (acceder vía {@code ConfigLoader.getRaw}):
 * <ul>
 *   <li>{@code browser.engine}, {@code playwright.browser}, {@code playwright.timeout.ms},
 *       {@code playwright.screenshots.dir} — prefijo distinto a {@code web.*}.</li>
 *   <li>{@code web.grid.enabled}, {@code web.grid.url}, {@code driver.strategy},
 *       {@code host} — legacy/deprecated.</li>
 * </ul>
 *
 * @since TASK-K03
 */
public record WebConfig(
        @NotBlank @Pattern(regexp = "chromium|firefox|webkit|chrome|edge",
                message = "browser debe ser chromium/firefox/webkit/chrome/edge")
        String browser,
        boolean headless,
        @Min(320) int viewportWidth,
        @Min(240) int viewportHeight,
        @NotNull @Pattern(regexp = "^https?://.*",
                message = "baseUrl debe empezar con http:// o https://")
        String baseUrl,
        @NotNull Duration navigationTimeout,
        @NotNull Duration actionTimeout,
        @NotNull String proxyUrl,
        @NotNull Duration explicitWait,
        @NotNull Duration implicitWait
) implements TypedConfig {

    private static final int DEFAULT_VIEWPORT_WIDTH = 1920;
    private static final int DEFAULT_VIEWPORT_HEIGHT = 1080;
    private static final int DEFAULT_NAVIGATION_TIMEOUT_SEC = 30;
    private static final int DEFAULT_ACTION_TIMEOUT_SEC = 15;
    private static final int DEFAULT_EXPLICIT_WAIT_SEC = 10;

    public static WebConfig defaults() {
        return new WebConfig(
                "chromium",
                true,
                DEFAULT_VIEWPORT_WIDTH,
                DEFAULT_VIEWPORT_HEIGHT,
                "http://localhost",
                Duration.ofSeconds(DEFAULT_NAVIGATION_TIMEOUT_SEC),
                Duration.ofSeconds(DEFAULT_ACTION_TIMEOUT_SEC),
                "",
                Duration.ofSeconds(DEFAULT_EXPLICIT_WAIT_SEC),
                Duration.ZERO
        );
    }
}
