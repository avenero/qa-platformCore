package com.qa.mobilecore.config;

/**
 * Constantes de configuración para el módulo mobile-core.
 *
 * <p>Centraliza todas las claves que pueden configurarse vía:
 * <ol>
 *   <li>{@code ExecutionConfig.properties} — enviado por el Backend por ejecución (máxima prioridad)</li>
 *   <li>Sistema properties {@code -Dkey=value}</li>
 *   <li>Variables de entorno {@code MOBILE_PLATFORM=ANDROID}</li>
 *   <li>Archivos {@code config-app.properties} del proyecto</li>
 * </ol>
 *
 * <p><b>Ejemplo de uso en config-app.properties:</b>
 * <pre>
 * mobile.platform=ANDROID
 * mobile.device.type=ANDROID_EMULATOR
 * mobile.device.name=Pixel_6_API_33
 * mobile.platform.version=13
 * mobile.appium.server.url=http://localhost:4723
 * mobile.app.path=/opt/apps/myapp.apk
 * mobile.app.auto.launch=true
 * </pre>
 *
 * <p><b>Indicaciones para el Backend (BE):</b>
 * Enviar estas propiedades en el {@code ExecutionRequest.properties} para cada ejecución.
 * Las claves prefijadas con {@code mobile.*} son exclusivas de este módulo.
 * El BE puede obtener la lista de dispositivos disponibles vía
 * {@code DeviceDiscoveryService} antes de enviar la request.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class MobileConfigKeys {

    private MobileConfigKeys() {}

    // =========================================================================
    // Dispositivo
    // =========================================================================

    /**
     * Identificador lógico del dispositivo en el pool.
     * <br>Ejemplo: {@code device-pixel6}, {@code emulator-5554}, {@code iphone14-sim}
     * <br>Si no se especifica, el pool asigna el primero disponible.
     */
    public static final String DEVICE_ID = "mobile.device.id";

    /**
     * Tipo de dispositivo.
     * Valores válidos: {@code ANDROID_EMULATOR}, {@code ANDROID_PHYSICAL},
     * {@code IOS_SIMULATOR}, {@code IOS_PHYSICAL}, {@code REMOTE_GRID}.
     */
    public static final String DEVICE_TYPE = "mobile.device.type";

    /**
     * Plataforma mobile.
     * Valores: {@code ANDROID} | {@code IOS} (case-insensitive).
     */
    public static final String PLATFORM = "mobile.platform";

    /**
     * Versión de la plataforma. Ejemplo: {@code "13"}, {@code "16.4"}.
     */
    public static final String PLATFORM_VERSION = "mobile.platform.version";

    /**
     * Nombre del dispositivo / AVD. Ejemplo: {@code Pixel_6_API_33}, {@code iPhone 15}.
     */
    public static final String DEVICE_NAME = "mobile.device.name";

    /**
     * UDID del dispositivo (requerido para dispositivos físicos).
     * Android: serial de ADB. iOS: UDID de 40 caracteres.
     */
    public static final String UDID = "mobile.udid";

    // =========================================================================
    // Appium
    // =========================================================================

    /**
     * URL del servidor Appium. Ejemplo: {@code http://localhost:4723}.
     * Para Remote Grid: URL del hub (BrowserStack, Sauce Labs, grid interno).
     */
    public static final String APPIUM_SERVER_URL = "mobile.appium.server.url";

    /**
     * Puerto base para Appium. Default: {@code 4723}.
     * El DevicePool asigna puertos secuenciales (BASE, BASE+1, BASE+2...) a cada dispositivo.
     */
    public static final String APPIUM_BASE_PORT = "mobile.appium.base.port";

    /**
     * Si {@code true}, intenta levantar Appium automáticamente cuando no responde.
     * <br><b>Solo para desarrollo local.</b> En CI/CD dejar en {@code false} (default).
     * Requiere {@code appium} instalado en PATH ({@code npm install -g appium}).
     */
    public static final String APPIUM_AUTO_START = "mobile.appium.auto.start";

    /**
     * Segundos de timeout para health check de Appium server. Default: {@code 30}.
     */
    public static final String APPIUM_STARTUP_TIMEOUT_SEC = "mobile.appium.startup.timeout.sec";

    /**
     * Segundos de implicit wait del driver Appium. Default: {@code 10}.
     * Preferir usar explicit waits en los steps.
     */
    public static final String IMPLICIT_WAIT_SEC = "mobile.implicit.wait.sec";

    // =========================================================================
    // Aplicación
    // =========================================================================

    /**
     * Ruta absoluta o relativa al APK/IPA/APP bajo prueba.
     * Ejemplo: {@code /opt/apps/myapp.apk}, {@code apps/MyApp.ipa}.
     */
    public static final String APP_PATH = "mobile.app.path";

    /**
     * Package de la app Android. Ejemplo: {@code com.example.myapp}.
     */
    public static final String APP_PACKAGE = "mobile.app.package";

    /**
     * Activity principal de la app Android. Ejemplo: {@code .MainActivity}.
     */
    public static final String APP_ACTIVITY = "mobile.app.activity";

    /**
     * Bundle ID de la app iOS. Ejemplo: {@code com.example.MyApp}.
     */
    public static final String BUNDLE_ID = "mobile.app.bundle.id";

    /**
     * Si {@code true}, la app se lanza automáticamente al crear el driver. Default: {@code true}.
     */
    public static final String APP_AUTO_LAUNCH = "mobile.app.auto.launch";

    /**
     * Si {@code true}, reinicia la app al inicio de cada sesión. Default: {@code false}.
     */
    public static final String APP_NO_RESET = "mobile.app.no.reset";

    // =========================================================================
    // Pool y descubrimiento
    // =========================================================================

    /**
     * Si {@code true}, escanea dispositivos automáticamente al arrancar. Default: {@code true}.
     * Usa ADB para Android y simctl para iOS.
     */
    public static final String DISCOVERY_AUTO_SCAN = "mobile.discovery.auto.scan";

    /**
     * Si {@code true}, incluye emuladores/simuladores en el escaneo. Default: {@code true}.
     */
    public static final String DISCOVERY_INCLUDE_VIRTUAL = "mobile.discovery.include.virtual";

    /**
     * Si {@code true}, incluye dispositivos físicos en el escaneo. Default: {@code true}.
     */
    public static final String DISCOVERY_INCLUDE_PHYSICAL = "mobile.discovery.include.physical";
}
