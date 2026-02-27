package com.scotia.qa.common.utils;

import com.scotia.qa.common.logging.TestLogger;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * Utilidades centralizadas para configuración SSL/TLS en el framework.
 *
 * <p>Consolida toda la funcionalidad SSL del framework:</p>
 * <ul>
 *   <li>Truststore corporativo del framework ({@code common/ssl/myTrustStore.jks})</li>
 *   <li>Clientes HTTP seguros con SSL configurado</li>
 *   <li>Contextos SSL para testing (trust-all, truststore personalizado)</li>
 * </ul>
 *
 * @author Abel Venero
 * @version 2.0.0
 * @since 1.0.0
 */
public class SSLUtils {

    private static final String TRUSTSTORE_FILENAME = "myTrustStore.jks";
    private static final String TRUSTSTORE_PASSWORD = "changeit";
    private static SSLContext cachedSSLContext = null;

    private SSLUtils() {
        throw new UnsupportedOperationException("SSLUtils es una clase de utilidad");
    }

    // =========================================================================
    // FRAMEWORK TRUSTSTORE
    // =========================================================================

    /**
     * Carga el SSLContext con el truststore personalizado del framework.
     *
     * <p>Busca el truststore {@code myTrustStore.jks} en múltiples ubicaciones:
     * <ol>
     *   <li>{@code ../common/ssl/myTrustStore.jks} (desde módulo)</li>
     *   <li>{@code common/ssl/myTrustStore.jks} (desde raíz proyecto)</li>
     *   <li>{@code <parent-dir>/common/ssl/myTrustStore.jks} (ruta absoluta)</li>
     *   <li>{@code ssl/myTrustStore.jks} (desde common mismo)</li>
     * </ol>
     *
     * <p>El SSLContext se cachea para evitar recargas innecesarias.</p>
     *
     * @return SSLContext configurado con el truststore del framework, o null si no se encuentra
     */
    public static SSLContext loadFrameworkSSLContext() {
        // Usar caché si ya fue cargado
        if (cachedSSLContext != null) {
            return cachedSSLContext;
        }

        try {
            Path truststorePath = findTruststore();

            if (truststorePath == null) {
                TestLogger.logDebug("SSL_UTILS",
                    "⚠️ No se encontró " + TRUSTSTORE_FILENAME + " en las ubicaciones esperadas", null);
                return null;
            }

            // Cargar el truststore
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream trustStoreStream = new FileInputStream(truststorePath.toFile())) {
                trustStore.load(trustStoreStream, TRUSTSTORE_PASSWORD.toCharArray());
            }

            // Construir SSLContext con el truststore
            SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
                .build();

            TestLogger.logDebug("SSL_UTILS",
                "✅ SSLContext cargado con truststore del framework",
                Map.of(
                    "truststore", truststorePath.getFileName().toString(),
                    "path", truststorePath.toAbsolutePath().toString()
                ));

            // Cachear para futuras llamadas
            cachedSSLContext = sslContext;
            return sslContext;

        } catch (Exception e) {
            TestLogger.logWarning("SSL_UTILS",
                "⚠️ Error cargando truststore: " + e.getMessage(), null);
            return null;
        }
    }

    /**
     * Crea un cliente HTTP seguro con SSL configurado usando el truststore del framework.
     *
     * <p>Si no se encuentra el truststore, retorna un cliente HTTP con SSL por defecto.</p>
     *
     * @return CloseableHttpClient configurado con SSL
     */
    public static CloseableHttpClient createSecureHttpClient() {
        return createSecureHttpClient(null);
    }

    /**
     * Crea un cliente HTTP seguro con SSL configurado y credenciales opcionales.
     *
     * <p>El cliente resultante:
     * <ul>
     *   <li>Usa el truststore personalizado del framework si está disponible</li>
     *   <li>Configura credenciales HTTP si se proporcionan</li>
     *   <li>Verifica hostname correctamente</li>
     *   <li>Soporta certificados autofirmados (solo los del truststore)</li>
     * </ul>
     *
     * @param credentialsProvider Proveedor de credenciales HTTP (puede ser null)
     * @return CloseableHttpClient configurado
     */
    public static CloseableHttpClient createSecureHttpClient(
            org.apache.http.client.CredentialsProvider credentialsProvider) {

        try {
            SSLContext sslContext = loadFrameworkSSLContext();

            if (sslContext != null) {
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslContext,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier()
                );

                TestLogger.logDebug("SSL_UTILS",
                    "✅ HttpClient creado con SSL personalizado del framework", null);

                if (credentialsProvider != null) {
                    return HttpClients.custom()
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setSSLSocketFactory(sslsf)
                        .build();
                } else {
                    return HttpClients.custom()
                        .setSSLSocketFactory(sslsf)
                        .build();
                }
            }

            // Fallback: Cliente HTTP con SSL por defecto
            TestLogger.logDebug("SSL_UTILS",
                "⚠️ Usando SSL por defecto del JDK", null);

            if (credentialsProvider != null) {
                return HttpClients.custom()
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .build();
            } else {
                return HttpClients.createDefault();
            }

        } catch (Exception e) {
            TestLogger.logWarning("SSL_UTILS",
                "⚠️ Error creando HttpClient: " + e.getMessage() + " - Usando cliente por defecto", null);

            if (credentialsProvider != null) {
                return HttpClients.custom()
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .build();
            } else {
                return HttpClients.createDefault();
            }
        }
    }

    /**
     * Busca el archivo truststore en múltiples ubicaciones posibles.
     *
     * @return Path al truststore o null si no se encuentra
     */
    private static Path findTruststore() {
        String userDir = System.getProperty("user.dir");
        Path userDirPath = Paths.get(userDir);

        Path[] possiblePaths = {
            // 1. Relativo al working directory del módulo
            Paths.get("../common/ssl/" + TRUSTSTORE_FILENAME),

            // 2. Desde la raíz del proyecto multi-módulo
            Paths.get("common/ssl/" + TRUSTSTORE_FILENAME),

            // 3. Ruta absoluta desde user.dir parent
            userDirPath.getParent() != null ?
                userDirPath.getParent().resolve("common/ssl/" + TRUSTSTORE_FILENAME) : null,

            // 4. Si estamos ejecutando desde common mismo
            Paths.get("ssl/" + TRUSTSTORE_FILENAME),

            // 5. Desde src/main/resources (si se embebe en el JAR)
            Paths.get("src/main/resources/ssl/" + TRUSTSTORE_FILENAME),

            // 6. Buscar en directorios hermanos (para módulos fuera del framework)
            userDirPath.getParent() != null ?
                userDirPath.getParent().resolve("qa-scotia-frameworks/common/ssl/" + TRUSTSTORE_FILENAME) : null,

            // 7. Ruta absoluta común en proyectos Scotia
            Paths.get("/Users/" + System.getProperty("user.name") + "/Documents/qa-scotia-frameworks/common/ssl/" + TRUSTSTORE_FILENAME),

            // 8. Dos niveles arriba (para estructura profunda de módulos)
            userDirPath.getParent() != null && userDirPath.getParent().getParent() != null ?
                userDirPath.getParent().getParent().resolve("common/ssl/" + TRUSTSTORE_FILENAME) : null
        };

        TestLogger.logDebug("SSL_UTILS", "🔍 Buscando truststore desde working dir: " + userDir, null);

        for (int i = 0; i < possiblePaths.length; i++) {
            Path path = possiblePaths[i];
            if (path == null) continue;

            TestLogger.logDebug("SSL_UTILS",
                String.format("  [%d] Intentando: %s", i+1, path.toAbsolutePath()), null);

            if (Files.exists(path)) {
                TestLogger.logInfo("SSL_UTILS",
                    "🔐 Truststore encontrado en ubicación #" + (i+1),
                    Map.of("path", path.toAbsolutePath().toString()));
                return path;
            }
        }

        TestLogger.logWarning("SSL_UTILS",
            "⚠️ Truststore no encontrado en ninguna de las " + possiblePaths.length + " ubicaciones", null);

        return null;
    }

    /**
     * Limpia el caché del SSLContext.
     * Útil para tests o cuando se actualiza el truststore.
     */
    public static void clearCache() {
        cachedSSLContext = null;
        TestLogger.logDebug("SSL_UTILS", "🔄 Caché de SSLContext limpiado", null);
    }

    /**
     * Verifica si el truststore del framework está disponible.
     *
     * @return true si el truststore existe y es accesible
     */
    public static boolean isTruststoreAvailable() {
        return findTruststore() != null;
    }

    /**
     * Obtiene información sobre la configuración SSL actual.
     *
     * @return Mapa con información de debugging
     */
    public static Map<String, String> getSSLInfo() {
        Path truststorePath = findTruststore();

        return Map.of(
            "truststoreFound", truststorePath != null ? "true" : "false",
            "truststorePath", truststorePath != null ? truststorePath.toAbsolutePath().toString() : "N/A",
            "sslContextCached", cachedSSLContext != null ? "true" : "false",
            "defaultTruststorePassword", TRUSTSTORE_PASSWORD
        );
    }

    // =========================================================================
    // TESTING SSL UTILITIES (consolidado desde api-core)
    // =========================================================================

    /**
     * Crea un SSLContext que acepta TODOS los certificados SSL sin validación.
     *
     * <p><b>SOLO PARA TESTING:</b> Deshabilita completamente la validación de certificados.
     * Usar únicamente en entornos de desarrollo/testing controlados.
     *
     * @return SSLContext configurado para aceptar cualquier certificado
     * @throws RuntimeException si hay error configurando el contexto SSL
     */
    public static SSLContext createTrustAllSSLContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Error configurando SSL sin validación: " + e.getMessage(), e);
        }
    }

    /**
     * Crea un HostnameVerifier que acepta TODOS los hostnames sin validación.
     *
     * <p><b>SOLO PARA TESTING.</b> Usar en conjunto con {@link #createTrustAllSSLContext()}.
     *
     * @return HostnameVerifier que acepta cualquier hostname
     */
    public static HostnameVerifier createTrustAllHostnameVerifier() {
        return (hostname, session) -> true;
    }

    /**
     * Crea un SSLContext usando un TrustStore personalizado desde classpath o filesystem.
     *
     * @param trustStorePath     Ruta al archivo truststore (.jks)
     * @param trustStorePassword Password del truststore
     * @return SSLContext configurado con el truststore
     * @throws RuntimeException si hay error cargando el truststore
     */
    public static SSLContext createSSLContextWithTrustStore(String trustStorePath, String trustStorePassword) {
        if (trustStorePath == null || trustStorePath.trim().isEmpty()) {
            throw new IllegalArgumentException("TrustStore path no puede ser null o vacío");
        }
        try {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            InputStream stream = SSLUtils.class.getClassLoader().getResourceAsStream(trustStorePath);
            if (stream == null) {
                stream = new FileInputStream(trustStorePath);
            }
            try (InputStream s = stream) {
                char[] pwd = trustStorePassword != null ? trustStorePassword.toCharArray() : null;
                trustStore.load(s, pwd);
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Error configurando SSL con TrustStore: " + e.getMessage(), e);
        }
    }

    /**
     * Crea un SSLContext con la configuración por defecto del sistema operativo.
     *
     * @return SSLContext con configuración por defecto
     * @throws RuntimeException si hay error creando el contexto
     */
    public static SSLContext createDefaultSSLContext() {
        try {
            return SSLContext.getDefault();
        } catch (Exception e) {
            throw new RuntimeException("Error configurando SSL por defecto: " + e.getMessage(), e);
        }
    }

    /**
     * Indica si se recomienda deshabilitar la validación SSL según el ambiente.
     *
     * @param environment Ambiente actual (dev, qa, uat, prod)
     * @return true si el ambiente es de desarrollo/testing, false en producción
     */
    public static boolean shouldDisableSSLValidation(String environment) {
        if (environment == null) return false;
        String env = environment.toLowerCase().trim();
        return env.contains("dev") || env.contains("qa")
            || env.contains("test") || env.contains("local");
    }
}
