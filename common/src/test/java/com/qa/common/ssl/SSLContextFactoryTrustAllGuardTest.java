package com.qa.common.ssl;

import com.qa.common.api.config.ExecutionProfile;
import com.qa.common.internal.ssl.SSLContextFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.net.ssl.SSLContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * W1-T3-M3 — trust-all SSL env guard (Core Audit 2026-05-27 §D.2).
 *
 * <p>Verifica el contrato:
 * <ul>
 *   <li>{@code profile=PROD + public host} → {@link IllegalStateException}.</li>
 *   <li>{@code profile=PROD + private host} (allowlist) → permitido con WARN.</li>
 *   <li>{@code profile in {DEV,TEST,STAGING}} → permitido con WARN para cualquier host.</li>
 * </ul>
 */
@DisplayName("SSLContextFactory.createTrustAllContext — W1-T3-M3 profile+host guard")
class SSLContextFactoryTrustAllGuardTest {

    @Nested
    @DisplayName("AC (a) — profile=PROD + public host + trust-all=true → throws")
    class ProdPublicHostThrows {

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {
            "https://api.example.com",
            "https://prod.qaplatform.io/health",
            "http://8.8.8.8:8080",
            "https://203.0.113.42",
            "https://qa.acme.com:443/v1"
        })
        void throwsForPublicHostUnderProd(String publicUrl) {
            assertThatThrownBy(() ->
                SSLContextFactory.createTrustAllContext(ExecutionProfile.PROD, publicUrl))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("trustAllSsl forbidden in PROD profile")
                .hasMessageContaining("EXECUTION_PROFILE=DEV/TEST");
        }

        @Test
        @DisplayName("null URL bajo PROD → tratado como host desconocido → throws")
        void nullUrlUnderProdThrows() {
            assertThatThrownBy(() ->
                SSLContextFactory.createTrustAllContext(ExecutionProfile.PROD, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("URL malformada bajo PROD → throws (fail-closed: \"unknown\" no matchea allowlist)")
        void malformedUrlUnderProdThrows() {
            assertThatThrownBy(() ->
                SSLContextFactory.createTrustAllContext(ExecutionProfile.PROD, "not a url at all ::: "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("profile=null bajo public host → fail-closed (PROD) → throws")
        void nullProfileFallsBackToProd() {
            assertThatThrownBy(() ->
                SSLContextFactory.createTrustAllContext(null, "https://api.example.com"))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("AC (b) — profile=PROD + private host + trust-all=true → succeeds with WARN")
    class ProdPrivateHostSucceeds {

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {
            "https://localhost:8080",
            "http://localhost",
            "https://internal-api.local",
            "https://gateway.internal/health",
            "https://127.0.0.1:9090",
            "https://10.0.0.5",
            "https://192.168.1.42",
            "https://192.168.100.200:8443/x"
        })
        void allowsPrivateHostsUnderProd(String privateUrl) {
            SSLContext ctx = SSLContextFactory.createTrustAllContext(ExecutionProfile.PROD, privateUrl);
            assertThat(ctx).isNotNull();
            assertThat(ctx.getProtocol()).isEqualTo("TLS");
        }
    }

    @Nested
    @DisplayName("AC (c) — non-PROD profile + any host + trust-all=true → succeeds with WARN")
    class NonProdAnyHostSucceeds {

        @ParameterizedTest(name = "[{index}] profile={0}")
        @ValueSource(strings = {"DEV", "TEST", "STAGING"})
        void allowsAnyHostUnderNonProd(String profileName) {
            ExecutionProfile profile = ExecutionProfile.valueOf(profileName);

            // Public host — would throw under PROD but is permitted here
            SSLContext ctx = SSLContextFactory.createTrustAllContext(profile, "https://api.example.com");
            assertThat(ctx).isNotNull();
            assertThat(ctx.getProtocol()).isEqualTo("TLS");
        }

        @Test
        @DisplayName("DEV + null URL → permitido (sin guard)")
        void devTolerantOfNullUrl() {
            SSLContext ctx = SSLContextFactory.createTrustAllContext(ExecutionProfile.DEV, null);
            assertThat(ctx).isNotNull();
        }
    }

    @Nested
    @DisplayName("Host allowlist edge cases (case-insensitive, anchored)")
    class AllowlistEdgeCases {

        @Test
        @DisplayName("hosts con mayúsculas matchean (case-insensitive)")
        void caseInsensitiveAllowlist() {
            SSLContext ctx = SSLContextFactory.createTrustAllContext(
                ExecutionProfile.PROD, "https://LOCALHOST");
            assertThat(ctx).isNotNull();
        }

        @Test
        @DisplayName("'localhostfake.com' NO matchea (anchored, no es prefix-match)")
        void anchoredAllowlistRejectsLookalikes() {
            assertThatThrownBy(() ->
                SSLContextFactory.createTrustAllContext(ExecutionProfile.PROD, "https://localhostfake.com"))
                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("'evil-internal-api.com' NO matchea como *.internal")
        void anchoredAllowlistRejectsInternalSubstring() {
            assertThatThrownBy(() ->
                SSLContextFactory.createTrustAllContext(ExecutionProfile.PROD, "https://evil-internal-api.com"))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
