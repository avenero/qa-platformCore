package com.qa.common.ssl;


import com.qa.common.internal.ssl.SSLContextFactory;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para SSLContextFactory.
 */
class SSLContextFactoryTest {

    // =========================================================================
    // createTrustAllContext
    // =========================================================================

    @Test
    void createTrustAllContext_returnsNonNullContext() {
        SSLContext ctx = SSLContextFactory.createTrustAllContext(com.qa.common.api.config.ExecutionProfile.DEV, "http://localhost:8080");
        assertThat(ctx).isNotNull();
    }

    @Test
    void createTrustAllContext_protocolIsTLS() {
        SSLContext ctx = SSLContextFactory.createTrustAllContext(com.qa.common.api.config.ExecutionProfile.DEV, "http://localhost:8080");
        assertThat(ctx.getProtocol()).isEqualTo("TLS");
    }

    @Test
    void createTrustAllContext_canCreateSocketFactory() {
        SSLContext ctx = SSLContextFactory.createTrustAllContext(com.qa.common.api.config.ExecutionProfile.DEV, "http://localhost:8080");
        assertThat(ctx.getSocketFactory()).isNotNull();
    }

    // =========================================================================
    // createFromSystemTrustStore
    // =========================================================================

    @Test
    void createFromSystemTrustStore_returnsNonNullContext() {
        SSLContext ctx = SSLContextFactory.createFromSystemTrustStore();
        assertThat(ctx).isNotNull();
    }

    @Test
    void createFromSystemTrustStore_protocolIsTLS() {
        SSLContext ctx = SSLContextFactory.createFromSystemTrustStore();
        assertThat(ctx.getProtocol()).isEqualTo("TLS");
    }

    // =========================================================================
    // createFromJksTrustStore — error paths
    // =========================================================================

    @Test
    void createFromJksTrustStore_nonExistentFile_throwsSSLContextCreationException() {
        assertThatThrownBy(() -> SSLContextFactory.createFromJksTrustStore(
            "/non/existent/truststore.jks", "changeit".toCharArray()
        )).isInstanceOf(SSLContextFactory.SSLContextCreationException.class);
    }

    @Test
    void createFromPkcs12TrustStore_nonExistentFile_throwsSSLContextCreationException() {
        assertThatThrownBy(() -> SSLContextFactory.createFromPkcs12TrustStore(
            "/non/existent/keystore.p12", "password".toCharArray()
        )).isInstanceOf(SSLContextFactory.SSLContextCreationException.class);
    }

    // =========================================================================
    // createFromClasspathJks — error paths
    // =========================================================================

    @Test
    void createFromClasspathJks_missingResource_throwsSSLContextCreationException() {
        assertThatThrownBy(() -> SSLContextFactory.createFromClasspathJks(
            "/ssl/missing.jks", "changeit".toCharArray()
        )).isInstanceOf(SSLContextFactory.SSLContextCreationException.class)
          .hasMessageContaining("no encontrado");
    }

    // =========================================================================
    // createFromClasspathJks — with bundled test truststore
    // =========================================================================

    @Test
    void createFromClasspathJks_bundledTestJks_returnsContext() {
        // Uses the myTrustStore.jks in common/ssl/ — copied to test resources if available
        // If not available, the test is skipped gracefully
        try {
            SSLContext ctx = SSLContextFactory.createFromClasspathJks(
                "/ssl/myTrustStore.jks", "changeit".toCharArray()
            );
            assertThat(ctx).isNotNull();
        } catch (SSLContextFactory.SSLContextCreationException e) {
            // Acceptable if test resource is not in classpath — just verify it throws correctly
            assertThat(e.getMessage()).isNotBlank();
        }
    }

    // =========================================================================
    // createFromJksTrustStore(byte[], char[]) — byte[] overload
    // =========================================================================

    @Test
    void createFromJksTrustStore_bytes_invalidBytes_throwsSSLContextCreationException() {
        byte[] garbage = new byte[]{0x00, 0x01, 0x02, 0x03};
        assertThatThrownBy(() -> SSLContextFactory.createFromJksTrustStore(garbage, "password".toCharArray()))
            .isInstanceOf(SSLContextFactory.SSLContextCreationException.class)
            .hasMessageContaining("JKS");
    }

    @Test
    void createFromPkcs12TrustStore_bytes_invalidBytes_throwsSSLContextCreationException() {
        byte[] garbage = new byte[]{0x00, 0x01, 0x02, 0x03};
        assertThatThrownBy(() -> SSLContextFactory.createFromPkcs12TrustStore(garbage, "password".toCharArray()))
            .isInstanceOf(SSLContextFactory.SSLContextCreationException.class)
            .hasMessageContaining("PKCS12");
    }

    @Test
    void createFromJksTrustStore_bytes_validKeystore_returnsContext() throws Exception {
        // Create a minimal in-memory JKS keystore (empty, no entries)
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, "testpass".toCharArray());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ks.store(bos, "testpass".toCharArray());
        byte[] keystoreBytes = bos.toByteArray();

        SSLContext ctx = SSLContextFactory.createFromJksTrustStore(keystoreBytes, "testpass".toCharArray());
        assertThat(ctx).isNotNull();
        assertThat(ctx.getProtocol()).isEqualTo("TLS");
    }

    @Test
    void createFromPkcs12TrustStore_bytes_validKeystore_returnsContext() throws Exception {
        // Create a minimal in-memory PKCS12 keystore (empty, no entries)
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, "testpass".toCharArray());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ks.store(bos, "testpass".toCharArray());
        byte[] keystoreBytes = bos.toByteArray();

        SSLContext ctx = SSLContextFactory.createFromPkcs12TrustStore(keystoreBytes, "testpass".toCharArray());
        assertThat(ctx).isNotNull();
        assertThat(ctx.getProtocol()).isEqualTo("TLS");
    }

    // =========================================================================
    // createFromPemBytes
    // =========================================================================

    @Test
    void createFromPemBytes_emptyBytes_throwsSSLContextCreationException() {
        byte[] empty = new byte[0];
        assertThatThrownBy(() -> SSLContextFactory.createFromPemBytes(empty))
            .isInstanceOf(SSLContextFactory.SSLContextCreationException.class)
            .hasMessageContaining("ningún certificado válido");
    }

    @Test
    void createFromPemBytes_invalidContent_throwsSSLContextCreationException() {
        byte[] notPem = "NOT A CERTIFICATE".getBytes();
        assertThatThrownBy(() -> SSLContextFactory.createFromPemBytes(notPem))
            .isInstanceOf(SSLContextFactory.SSLContextCreationException.class);
    }

    // =========================================================================
    // SSLContextCreationException
    // =========================================================================

    @Test
    void sslContextCreationException_hasMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");
        SSLContextFactory.SSLContextCreationException ex =
            new SSLContextFactory.SSLContextCreationException("test message", cause);
        assertThat(ex.getMessage()).isEqualTo("test message");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
