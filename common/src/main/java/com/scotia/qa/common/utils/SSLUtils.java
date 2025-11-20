package com.scotia.qa.common.utils;

import com.scotia.qa.common.logging.TestLogger;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
 * Utilidad para configuración y manejo de SSL/TLS en peticiones HTTP.
 *
 * <p>Esta clase proporciona métodos para:
 * <ul>
 *   <li>Deshabilitar validación SSL (útil para entornos de desarrollo/testing)</li>
 *   <li>Cargar TrustStores personalizados (.jks files)</li>
 *   <li>Configurar contextos SSL específicos</li>
 *   <li>Gestionar certificados autofirmados</li>
 * </ul>
 *
 * <p><b>⚠️ ADVERTENCIA DE SEGURIDAD:</b>
 * Deshabilitar la validación SSL debe usarse ÚNICAMENTE en entornos controlados de testing.
 * NUNCA usar en producción ya que compromete la seguridad de las comunicaciones.
 *
 * <p><b>Uso básico - Deshabilitar validación SSL:</b>
 * <pre>
 * // Para testing en ambientes con certificados autofirmados
 * SSLContext sslContext = SSLUtils.createTrustAllSSLContext();
 * HostnameVerifier hostnameVerifier = SSLUtils.createTrustAllHostnameVerifier();
 * </pre>
 *
 * <p><b>Uso avanzado - Cargar TrustStore personalizado:</b>
 * <pre>
 * // Cargar truststore desde archivo
 * SSLContext sslContext = SSLUtils.createSSLContextWithTrustStore(
 *     "ssl/myTrustStore.jks",
 *     "changeit"  // password del truststore
 * );
 * </pre>
 *
 * @author Scotia QA Framework Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class SSLUtils {

    private static final TestLogger.LoggerWrapper log = TestLogger.getLogger(SSLUtils.class);

    // Prevenir instanciación
    private SSLUtils() {
        throw new UnsupportedOperationException("Utility class - no debe ser instanciada");
    }

    /**
     * Crea un SSLContext que acepta TODOS los certificados SSL sin validación.
     *
     * <p><b>⚠️ SOLO PARA TESTING:</b> Este método deshabilita completamente la validación
     * de certificados SSL, lo cual es un riesgo de seguridad. Usar solo en entornos
     * de desarrollo y testing controlados.
     *
     * <p><b>Casos de uso típicos:</b>
     * <ul>
     *   <li>Testing contra servidores con certificados autofirmados</li>
     *   <li>Ambientes de desarrollo sin certificados válidos</li>
     *   <li>Testing en entornos de QA/UAT con HTTPS</li>
     * </ul>
     *
     * @return SSLContext configurado para aceptar cualquier certificado
     * @throws RuntimeException si hay error configurando el contexto SSL
     */
    public static SSLContext createTrustAllSSLContext() {
        try {
            // Crear TrustManager que acepta todos los certificados
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            // No validar certificados del cliente
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            // No validar certificados del servidor
                        }
                    }
            };

            // Instalar el TrustManager que acepta todo
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
     * <p><b>⚠️ SOLO PARA TESTING:</b> Este método deshabilita completamente la validación
     * de hostname, lo cual permite aceptar certificados de cualquier dominio.
     *
     * <p>Debe usarse en conjunto con {@link #createTrustAllSSLContext()} para
     * deshabilitar completamente la validación SSL/TLS.
     *
     * @return HostnameVerifier que acepta cualquier hostname
     */
    public static HostnameVerifier createTrustAllHostnameVerifier() {
        return (hostname, session) -> true;
    }

    /**
     * Crea un SSLContext usando un TrustStore personalizado.
     *
     * <p>Este método permite cargar certificados específicos desde un archivo TrustStore (.jks)
     * para validar conexiones SSL de forma segura contra servidores conocidos.
     *
     * <p><b>Uso recomendado:</b>
     * <pre>
     * SSLContext sslContext = SSLUtils.createSSLContextWithTrustStore(
     *     "ssl/myTrustStore.jks",
     *     "changeit"
     * );
     * </pre>
     *
     * @param trustStorePath Ruta al archivo truststore (.jks). Puede ser classpath o filesystem
     * @param trustStorePassword Password del truststore
     * @return SSLContext configurado con el truststore
     * @throws RuntimeException si hay error cargando o configurando el truststore
     */
    public static SSLContext createSSLContextWithTrustStore(String trustStorePath, String trustStorePassword) {
        if (trustStorePath == null || trustStorePath.trim().isEmpty()) {
            throw new IllegalArgumentException("TrustStore path no puede ser null o vacío");
        }

        try {
            log.info("Cargando TrustStore desde: {}", trustStorePath);

            // Cargar el KeyStore
            KeyStore trustStore = KeyStore.getInstance("JKS");

            // Intentar cargar desde classpath primero, luego filesystem
            InputStream trustStoreStream = getInputStreamForTrustStore(trustStorePath);

            if (trustStoreStream == null) {
                throw new RuntimeException("No se pudo encontrar el TrustStore: " + trustStorePath);
            }

            try {
                char[] passwordChars = trustStorePassword != null ? trustStorePassword.toCharArray() : null;
                trustStore.load(trustStoreStream, passwordChars);
                log.info("✅ TrustStore cargado exitosamente");
            } finally {
                trustStoreStream.close();
            }

            // Inicializar TrustManagerFactory con el KeyStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            // Crear SSLContext con el TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());

            log.info("✅ SSLContext con TrustStore configurado exitosamente");
            return sslContext;

        } catch (Exception e) {
            log.error("❌ Error cargando TrustStore '{}': {}", trustStorePath, e.getMessage());
            throw new RuntimeException("Error configurando SSL con TrustStore: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el InputStream para el TrustStore desde classpath o filesystem.
     *
     * @param trustStorePath Ruta al archivo
     * @return InputStream o null si no se encuentra
     */
    private static InputStream getInputStreamForTrustStore(String trustStorePath) {
        // Intentar desde classpath primero
        InputStream stream = SSLUtils.class.getClassLoader().getResourceAsStream(trustStorePath);

        if (stream != null) {
            log.debug("TrustStore encontrado en classpath: {}", trustStorePath);
            return stream;
        }

        // Intentar desde filesystem
        try {
            stream = new FileInputStream(trustStorePath);
            log.debug("TrustStore encontrado en filesystem: {}", trustStorePath);
            return stream;
        } catch (Exception e) {
            log.debug("TrustStore no encontrado en filesystem: {}", trustStorePath);
            return null;
        }
    }

    /**
     * Crea un SSLContext con configuración por defecto del sistema.
     *
     * <p>Usa los certificados del sistema operativo para validación SSL estándar.
     *
     * @return SSLContext con configuración por defecto
     * @throws RuntimeException si hay error creando el contexto
     */
    public static SSLContext createDefaultSSLContext() {
        try {
            log.debug("Creando SSLContext con configuración por defecto del sistema");
            return SSLContext.getDefault();
        } catch (Exception e) {
            log.error("Error creando SSLContext por defecto: {}", e.getMessage());
            throw new RuntimeException("Error configurando SSL por defecto: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene información sobre el estado de validación SSL recomendado.
     *
     * @param environment Ambiente actual (dev, qa, uat, prod)
     * @return true si se recomienda deshabilitar validación SSL, false si debe estar habilitada
     */
    public static boolean shouldDisableSSLValidation(String environment) {
        if (environment == null) {
            return false;
        }

        String env = environment.toLowerCase().trim();
        boolean shouldDisable = env.contains("dev") || env.contains("qa") || env.contains("test") || env.contains("local");

        if (shouldDisable) {
            log.warn("⚠️  Ambiente '{}' detectado - Validación SSL puede ser deshabilitada de forma segura", environment);
        } else {
            log.info("Ambiente '{}' detectado - Validación SSL debe permanecer HABILITADA", environment);
        }

        return shouldDisable;
    }
}

