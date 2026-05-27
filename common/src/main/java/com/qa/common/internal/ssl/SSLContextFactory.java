package com.qa.common.internal.ssl;

import com.qa.common.api.Internal;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * Factory de {@link SSLContext} con soporte para múltiples estrategias de confianza.
 *
 * <p>Estrategias disponibles:</p>
 * <ul>
 *   <li>{@link #createTrustAllContext()} — acepta cualquier certificado. <strong>Solo para testing.</strong></li>
 *   <li>{@link #createFromJksTrustStore(String, char[])} — JKS truststore con certificado personalizado.</li>
 *   <li>{@link #createFromPkcs12TrustStore(String, char[])} — PKCS12 truststore.</li>
 *   <li>{@link #createFromSystemTrustStore()} — delega en el truststore del JVM (cacerts).</li>
 *   <li>{@link #createFromClasspathJks(String, char[])} — JKS desde classpath (útil en CI/CD).</li>
 * </ul>
 *
 * <p>Ejemplo — truststore personalizado:</p>
 * <pre>
 * SSLContext ctx = SSLContextFactory.createFromJksTrustStore(
 *     "/etc/ssl/myTrustStore.jks", "changeit".toCharArray()
 * );
 * </pre>
 *
 * @since 2.1.0
 */
@Internal(reason = "internal — TASK-K05/K06 introducirá api/ssl/SslContextPort si externalizamos")
public final class SSLContextFactory {

    private static final String TLS = "TLS";

    private SSLContextFactory() {}

    // =========================================================================
    // Trust-all (testing only)
    // =========================================================================

    /**
     * Crea un {@link SSLContext} que acepta cualquier certificado sin validación.
     *
     * <p><strong>ADVERTENCIA:</strong> usar únicamente en ambientes de testing local.
     * No usar en staging/production — abre la conexión a ataques MITM.</p>
     *
     * @return SSLContext configurado con TrustManager que acepta todo
     * @throws SSLContextCreationException si la creación falla
     */
    public static SSLContext createTrustAllContext() {
        try {
            TrustManager[] trustAllManagers = new TrustManager[]{
                new X509TrustManager() {
                    @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                }
            };
            SSLContext ctx = SSLContext.getInstance(TLS);
            ctx.init(null, trustAllManagers, new SecureRandom());
            return ctx;
        } catch (Exception e) {
            throw new SSLContextCreationException("No se pudo crear SSLContext trust-all", e);
        }
    }

    // =========================================================================
    // Custom truststore — JKS
    // =========================================================================

    /**
     * Crea un {@link SSLContext} a partir de un truststore JKS en el sistema de archivos.
     *
     * @param trustStorePath ruta absoluta al archivo .jks
     * @param password       contraseña del truststore (use {@code "changeit".toCharArray()} para el default)
     * @return SSLContext inicializado con el truststore indicado
     * @throws SSLContextCreationException si el archivo no existe, la contraseña es incorrecta
     *                                     o el formato no es JKS válido
     */
    public static SSLContext createFromJksTrustStore(String trustStorePath, char[] password) {
        return createFromTrustStore(trustStorePath, password, "JKS", false);
    }

    // =========================================================================
    // Custom truststore — PKCS12
    // =========================================================================

    /**
     * Crea un {@link SSLContext} a partir de un truststore PKCS12 (.p12 / .pfx).
     *
     * @param trustStorePath ruta absoluta al archivo .p12 o .pfx
     * @param password       contraseña del truststore
     * @return SSLContext inicializado con el truststore indicado
     * @throws SSLContextCreationException si el archivo no es válido o la contraseña es incorrecta
     */
    public static SSLContext createFromPkcs12TrustStore(String trustStorePath, char[] password) {
        return createFromTrustStore(trustStorePath, password, "PKCS12", false);
    }

    // =========================================================================
    // System truststore (JVM cacerts)
    // =========================================================================

    /**
     * Crea un {@link SSLContext} usando el truststore del sistema JVM (cacerts).
     * Adecuado para llamadas a servidores con certificados firmados por CAs conocidas.
     *
     * @return SSLContext con el truststore del sistema
     * @throws SSLContextCreationException si la inicialización falla
     */
    public static SSLContext createFromSystemTrustStore() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            );
            tmf.init((KeyStore) null); // null → usa cacerts del JVM
            SSLContext ctx = SSLContext.getInstance(TLS);
            ctx.init(null, tmf.getTrustManagers(), new SecureRandom());
            return ctx;
        } catch (Exception e) {
            throw new SSLContextCreationException("No se pudo inicializar SSLContext con el truststore del sistema", e);
        }
    }

    // =========================================================================
    // Classpath truststore — para CI/CD con recursos embebidos
    // =========================================================================

    /**
     * Crea un {@link SSLContext} a partir de un truststore JKS cargado desde el classpath.
     * Conveniente cuando el truststore se empaqueta como recurso en el jar (CI/CD pipelines).
     *
     * <p>Ejemplo: si el archivo está en {@code src/test/resources/ssl/myTrustStore.jks},
     * usar {@code classpath:/ssl/myTrustStore.jks} o simplemente {@code /ssl/myTrustStore.jks}.</p>
     *
     * @param classpathResource path del recurso dentro del classpath (con o sin leading '/')
     * @param password          contraseña del truststore
     * @return SSLContext inicializado
     * @throws SSLContextCreationException si el recurso no se encuentra en el classpath
     */
    public static SSLContext createFromClasspathJks(String classpathResource, char[] password) {
        return createFromTrustStore(classpathResource, password, "JKS", true);
    }

    // =========================================================================
    // Custom truststore — byte[] overloads (certificados almacenados en BD)
    // =========================================================================

    /**
     * Crea un {@link SSLContext} a partir de bytes de un truststore JKS.
     * Usado cuando el certificado viene de la base de datos (no del filesystem).
     *
     * @param keystoreBytes bytes del keystore JKS (descifrados)
     * @param password      contraseña del truststore
     * @return SSLContext inicializado
     * @throws SSLContextCreationException si los bytes no son un JKS válido
     */
    public static SSLContext createFromJksTrustStore(byte[] keystoreBytes, char[] password) {
        return createFromTrustStoreBytes(keystoreBytes, password, "JKS");
    }

    /**
     * Crea un {@link SSLContext} a partir de bytes de un truststore PKCS12.
     * Usado cuando el certificado viene de la base de datos (no del filesystem).
     *
     * @param keystoreBytes bytes del keystore PKCS12 (descifrados)
     * @param password      contraseña del truststore
     * @return SSLContext inicializado
     * @throws SSLContextCreationException si los bytes no son un PKCS12 válido
     */
    public static SSLContext createFromPkcs12TrustStore(byte[] keystoreBytes, char[] password) {
        return createFromTrustStoreBytes(keystoreBytes, password, "PKCS12");
    }

    /**
     * Crea un {@link SSLContext} a partir de bytes PEM que contienen uno o más
     * certificados de CA en formato Base64 ({@code -----BEGIN CERTIFICATE-----}).
     *
     * @param pemBytes bytes del archivo PEM (descifrados)
     * @return SSLContext con los certificados PEM como trusted CAs
     * @throws SSLContextCreationException si el PEM no contiene certificados válidos
     */
    public static SSLContext createFromPemBytes(byte[] pemBytes) {
        try {
            java.security.cert.CertificateFactory cf =
                java.security.cert.CertificateFactory.getInstance("X.509");

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            java.io.InputStream pem = new java.io.ByteArrayInputStream(pemBytes);
            int certIdx = 0;
            for (java.security.cert.Certificate cert : cf.generateCertificates(pem)) {
                keyStore.setCertificateEntry("pem-ca-" + certIdx++, cert);
            }
            if (certIdx == 0) {
                throw new SSLContextCreationException(
                    "El archivo PEM no contiene ningún certificado válido", null);
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext ctx = SSLContext.getInstance(TLS);
            ctx.init(null, tmf.getTrustManagers(), new SecureRandom());
            return ctx;

        } catch (SSLContextCreationException e) {
            throw e;
        } catch (Exception e) {
            throw new SSLContextCreationException("No se pudo crear SSLContext desde bytes PEM", e);
        }
    }

    // =========================================================================
    // Implementación compartida
    // =========================================================================

    private static SSLContext createFromTrustStoreBytes(byte[] keystoreBytes, char[] password,
                                                         String storeType) {
        try {
            KeyStore keyStore = KeyStore.getInstance(storeType);
            try (java.io.InputStream is = new java.io.ByteArrayInputStream(keystoreBytes)) {
                keyStore.load(is, password);
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            SSLContext ctx = SSLContext.getInstance(TLS);
            ctx.init(null, tmf.getTrustManagers(), new SecureRandom());
            return ctx;
        } catch (Exception e) {
            throw new SSLContextCreationException(
                "No se pudo crear SSLContext desde bytes tipo=" + storeType, e);
        }
    }

    private static SSLContext createFromTrustStore(String path, char[] password,
                                                    String storeType, boolean fromClasspath) {
        try {
            KeyStore keyStore = KeyStore.getInstance(storeType);
            try (InputStream is = fromClasspath
                ? SSLContextFactory.class.getResourceAsStream(path)
                : new FileInputStream(path)) {

                if (is == null) {
                    throw new SSLContextCreationException(
                        "Recurso SSL no encontrado: " + path
                            + (fromClasspath ? " (classpath)" : " (filesystem)"), null);
                }
                keyStore.load(is, password);
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            );
            tmf.init(keyStore);

            SSLContext ctx = SSLContext.getInstance(TLS);
            ctx.init(null, tmf.getTrustManagers(), new SecureRandom());
            return ctx;

        } catch (SSLContextCreationException e) {
            throw e;
        } catch (Exception e) {
            throw new SSLContextCreationException(
                "No se pudo crear SSLContext desde truststore [" + path + "] tipo=" + storeType, e);
        }
    }

    // =========================================================================
    // Excepción tipada
    // =========================================================================

    /**
     * Excepción lanzada cuando la creación del {@link SSLContext} falla.
     * Es unchecked para no contaminar la firma de métodos del framework.
     */
    public static final class SSLContextCreationException extends RuntimeException {
        public SSLContextCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
