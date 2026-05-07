package com.qa.mobilecore.appium;

import com.qa.common.logging.TestLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Gestor del servidor Appium.
 *
 * <p>Responsabilidades:
 * <ol>
 *   <li><b>Health check:</b> verifica si Appium responde en la URL dada ({@code /status})</li>
 *   <li><b>Auto-start opt-in:</b> levanta el proceso Appium localmente cuando
 *       {@code mobile.appium.auto.start=true} (solo para desarrollo local)</li>
 * </ol>
 *
 * <p><b>Política de uso:</b>
 * <ul>
 *   <li><b>CI/CD / Grid:</b> Appium ya corre en el servidor; solo se hace health check.
 *       Si no responde se falla rápido con mensaje claro.</li>
 *   <li><b>Local dev:</b> {@code mobile.appium.auto.start=true} levanta Appium
 *       automáticamente si no está corriendo. Requiere {@code appium} en PATH
 *       ({@code npm install -g appium}).</li>
 * </ul>
 *
 * <p><b>Instrucciones para el BE:</b> no es necesario invocar esta clase desde el BE.
 * El {@code MobileHelper} la invoca internamente antes de crear el driver.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public final class AppiumServerManager {

    private static final int  HEALTH_CHECK_TIMEOUT_MS  = 3_000;
    /** Intervalo base para el backoff exponencial en la espera de startup (ms). */
    private static final long POLL_BASE_INTERVAL_MS    = 500L;
    /** Máximo intervalo de poll individual (cap del backoff exponencial). */
    private static final long POLL_MAX_INTERVAL_MS     = 10_000L;
    private static final int  DEFAULT_APPIUM_PORT      = 4723;
    private static final long MILLIS_PER_SECOND        = 1000L;

    private AppiumServerManager() {}

    /**
     * Verifica que Appium esté disponible en la URL dada.
     * Si no responde y {@code autoStart} es {@code false}, falla con mensaje claro.
     * Si {@code autoStart} es {@code true}, intenta levantarlo (solo local/dev).
     *
     * @param serverUrl URL del servidor Appium (ej: {@code http://localhost:4723})
     * @param autoStart si {@code true}, intenta arrancar Appium al detectar que no responde
     * @param startupTimeoutSec segundos máximos de espera para que Appium esté listo
     * @throws IllegalStateException si Appium no está disponible y no se puede iniciar
     */
    public static void ensureRunning(String serverUrl, boolean autoStart, int startupTimeoutSec) {
        if (isResponding(serverUrl)) {
            TestLogger.logInfo("APPIUM_SERVER",
                "Appium disponible en: " + serverUrl, null);
            return;
        }

        if (!autoStart) {
            throw new IllegalStateException(
                "Appium no responde en: " + serverUrl + ". " +
                "Opciones:\n" +
                "  1) Inicia Appium manualmente: appium --port <PORT>\n" +
                "  2) Usa un grid/Docker con Appium levantado\n" +
                "  3) Activa auto-start (solo local dev): mobile.appium.auto.start=true");
        }

        TestLogger.logInfo("APPIUM_SERVER",
            "Appium no responde en " + serverUrl + ". Intentando auto-start...", null);
        startLocalAppium(serverUrl, startupTimeoutSec);
    }

    /**
     * Verifica si Appium responde en la URL dada con un GET a {@code /status}.
     *
     * @param serverUrl URL base del servidor Appium
     * @return true si el servidor responde con HTTP 200
     */
    public static boolean isResponding(String serverUrl) {
        try {
            String statusUrl = serverUrl.replaceAll("/+$", "") + "/status";
            URL url = URI.create(statusUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(HEALTH_CHECK_TIMEOUT_MS);
            conn.setReadTimeout(HEALTH_CHECK_TIMEOUT_MS);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================================
    // Auto-start (solo local/dev)
    // =========================================================================

    private static void startLocalAppium(String serverUrl, int timeoutSec) {
        if (!isAppiumInPath()) {
            throw new IllegalStateException(
                "Auto-start habilitado pero 'appium' no está en PATH. " +
                "Instala Appium globalmente: npm install -g appium " +
                "y luego: appium driver install uiautomator2 xcuitest");
        }

        int port = extractPort(serverUrl);
        Process appiumProcess = launchAppiumProcess(port);

        // Registrar shutdown hook para matar el proceso al terminar la JVM
        final Process proc = appiumProcess;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (proc != null && proc.isAlive()) {
                proc.destroyForcibly();
                TestLogger.logInfo("APPIUM_SERVER",
                    "Proceso Appium terminado (shutdown hook)", null);
            }
        }, "appium-shutdown-hook"));

        waitForAppium(serverUrl, timeoutSec);
    }

    private static boolean isAppiumInPath() {
        try {
            String cmd = isWindows() ? "where appium" : "which appium";
            Process p = Runtime.getRuntime().exec(
                isWindows() ? new String[]{"cmd", "/c", cmd} : new String[]{"sh", "-c", cmd});
            return p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static Process launchAppiumProcess(int port) {
        try {
            String[] cmd = isWindows()
                ? new String[]{"cmd", "/c", "appium", "--port", String.valueOf(port), "--log-level", "warn"}
                : new String[]{"appium", "--port", String.valueOf(port), "--log-level", "warn"};

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Consumir el output en un thread demonio para evitar bloqueos
            Thread reader = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        TestLogger.logInfo("APPIUM_SERVER", "[appium] " + line, null);
                    }
                } catch (Exception ignored) { }
            }, "appium-output-reader");
            reader.setDaemon(true);
            reader.start();

            TestLogger.logInfo("APPIUM_SERVER",
                "Proceso Appium iniciado en puerto: " + port, null);
            return process;

        } catch (Exception e) {
            throw new IllegalStateException(
                "Error al lanzar proceso Appium: " + e.getMessage(), e);
        }
    }

    /**
     * Espera a que Appium esté listo usando <b>exponential backoff</b>.
     *
     * <p>Estrategia: el intervalo entre intentos empieza en {@value #POLL_BASE_INTERVAL_MS} ms
     * y se duplica en cada intento fallido, con un techo de {@value #POLL_MAX_INTERVAL_MS} ms.
     * Esto evita saturar el puerto en los primeros milisegundos de arranque y sigue
     * siendo responsivo cuando Appium arranca rápido.
     *
     * <pre>
     * Intento 1 → espera  500ms
     * Intento 2 → espera 1000ms
     * Intento 3 → espera 2000ms
     * Intento 4 → espera 4000ms
     * Intento 5 → espera 8000ms
     * Intento 6+ → espera 10000ms (cap)
     * </pre>
     */
    private static void waitForAppium(String serverUrl, int timeoutSec) {
        long deadline = System.currentTimeMillis() + (timeoutSec * MILLIS_PER_SECOND);
        TestLogger.logInfo("APPIUM_SERVER",
            "Esperando que Appium este listo (timeout: " + timeoutSec + "s, backoff exponencial)...", null);

        int attempt = 0;
        while (System.currentTimeMillis() < deadline) {
            if (isResponding(serverUrl)) {
                TestLogger.logInfo("APPIUM_SERVER",
                    "Appium listo en: " + serverUrl + " (intento " + (attempt + 1) + ")", null);
                return;
            }
            // Backoff exponencial con cap: base * 2^attempt, máximo POLL_MAX_INTERVAL_MS
            long waitMs = Math.min(POLL_BASE_INTERVAL_MS * (1L << attempt), POLL_MAX_INTERVAL_MS);
            // No superar el tiempo restante hasta el deadline
            long remaining = deadline - System.currentTimeMillis();
            waitMs = Math.min(waitMs, Math.max(remaining, 0));

            if (waitMs > 0) {
                TestLogger.logInfo("APPIUM_SERVER",
                    "Appium no responde. Reintento " + (attempt + 1) + " en " + waitMs + "ms...", null);
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            attempt++;
        }

        throw new IllegalStateException(
            "Appium no estuvo listo en " + timeoutSec + "s después de " + attempt + " intentos. " +
            "Verifica la instalación o aumenta mobile.appium.startup.timeout.sec");
    }

    private static int extractPort(String serverUrl) {
        try {
            return URI.create(serverUrl).getPort();
        } catch (Exception e) {
            return DEFAULT_APPIUM_PORT;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
