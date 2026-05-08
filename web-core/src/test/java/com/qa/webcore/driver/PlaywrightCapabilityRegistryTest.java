package com.qa.webcore.driver;

import com.qa.common.pool.CapabilityDescriptor;
import com.qa.common.pool.DeviceHandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PlaywrightCapabilityRegistry")
class PlaywrightCapabilityRegistryTest {

    private PlaywrightCapabilityRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PlaywrightCapabilityRegistry();
    }

    // =========================================================================
    // platformId()
    // =========================================================================

    @Test
    @DisplayName("platformId retorna WEB")
    void platformId_returnsWEB() {
        assertThat(registry.platformId()).isEqualTo("WEB");
    }

    // =========================================================================
    // list()
    // =========================================================================

    @Nested
    @DisplayName("list()")
    class ListTests {

        @Test
        @DisplayName("retorna exactamente 4 browsers")
        void list_returnsFourBrowsers() {
            assertThat(registry.list()).hasSize(4);
        }

        @Test
        @DisplayName("contiene los 4 ids esperados")
        void list_containsExpectedIds() {
            List<String> ids = registry.list().stream()
                    .map(CapabilityDescriptor::id)
                    .toList();

            assertThat(ids).containsExactlyInAnyOrder("chromium", "firefox", "webkit", "edge");
        }

        @Test
        @DisplayName("todos los descriptores tienen platformId=WEB")
        void list_allDescriptorsHavePlatformIdWEB() {
            registry.list().forEach(d ->
                assertThat(d.platformId()).isEqualTo("WEB"));
        }

        @Test
        @DisplayName("todos los descriptores tienen type=BROWSER")
        void list_allDescriptorsHaveTypeBROWSER() {
            registry.list().forEach(d ->
                assertThat(d.type()).isEqualTo("BROWSER"));
        }

        @Test
        @DisplayName("todos los descriptores están disponibles")
        void list_allDescriptorsAvailable() {
            registry.list().forEach(d ->
                assertThat(d.available()).isTrue());
        }

        @Test
        @DisplayName("ningún descriptor está en uso")
        void list_noneInUse() {
            registry.list().forEach(d ->
                assertThat(d.inUse()).isFalse());
        }

        @Test
        @DisplayName("friendly names mapeados correctamente")
        void list_friendlyNamesMappedCorrectly() {
            registry.list().forEach(d ->
                assertThat(d.name()).isNotBlank());

            assertThat(registry.list().stream()
                    .filter(d -> d.id().equals("chromium"))
                    .findFirst()
                    .map(CapabilityDescriptor::name))
                .hasValue("Chrome / Chromium");

            assertThat(registry.list().stream()
                    .filter(d -> d.id().equals("firefox"))
                    .findFirst()
                    .map(CapabilityDescriptor::name))
                .hasValue("Mozilla Firefox");

            assertThat(registry.list().stream()
                    .filter(d -> d.id().equals("webkit"))
                    .findFirst()
                    .map(CapabilityDescriptor::name))
                .hasValue("Safari / WebKit");

            assertThat(registry.list().stream()
                    .filter(d -> d.id().equals("edge"))
                    .findFirst()
                    .map(CapabilityDescriptor::name))
                .hasValue("Microsoft Edge");
        }

        @Test
        @DisplayName("list() nunca retorna null")
        void list_neverNull() {
            assertThat(registry.list()).isNotNull();
        }
    }

    // =========================================================================
    // isAvailable()
    // =========================================================================

    @Nested
    @DisplayName("isAvailable()")
    class IsAvailableTests {

        @ParameterizedTest(name = "[{0}] es soportado")
        @ValueSource(strings = {"chromium", "firefox", "webkit", "edge"})
        @DisplayName("retorna true para browsers soportados")
        void isAvailable_supportedBrowsers_returnsTrue(String browserId) {
            assertThat(registry.isAvailable(browserId)).isTrue();
        }

        @ParameterizedTest(name = "[{0}] no es soportado")
        @ValueSource(strings = {"ie", "opera", "safari", "chrome", "invalid", " "})
        @DisplayName("retorna false para ids no soportados")
        void isAvailable_unsupportedIds_returnsFalse(String browserId) {
            assertThat(registry.isAvailable(browserId)).isFalse();
        }

        @Test
        @DisplayName("retorna false para null sin lanzar excepción")
        void isAvailable_nullId_returnsFalse() {
            assertThat(registry.isAvailable(null)).isFalse();
        }

        @Test
        @DisplayName("es case-sensitive — 'Chromium' no es 'chromium'")
        void isAvailable_caseSensitive_returnsFalse() {
            assertThat(registry.isAvailable("Chromium")).isFalse();
            assertThat(registry.isAvailable("FIREFOX")).isFalse();
        }
    }

    // =========================================================================
    // acquire()
    // =========================================================================

    @Nested
    @DisplayName("acquire()")
    class AcquireTests {

        @ParameterizedTest(name = "acquire({0}) retorna handle")
        @ValueSource(strings = {"chromium", "firefox", "webkit", "edge"})
        @DisplayName("retorna handle presente para browser soportado")
        void acquire_supportedBrowser_returnsHandle(String browserId) {
            Optional<DeviceHandle> handle = registry.acquire(browserId, Duration.ofSeconds(30));

            assertThat(handle).isPresent();
        }

        @Test
        @DisplayName("handle tiene capabilityId igual al id solicitado")
        void acquire_handle_hasCorrectCapabilityId() {
            DeviceHandle handle = registry.acquire("chromium", Duration.ofSeconds(1)).orElseThrow();

            assertThat(handle.capabilityId()).isEqualTo("chromium");
        }

        @Test
        @DisplayName("handle tiene sessionId no vacío")
        void acquire_handle_hasNonBlankSessionId() {
            DeviceHandle handle = registry.acquire("firefox", Duration.ofSeconds(1)).orElseThrow();

            assertThat(handle.sessionId()).isNotBlank();
        }

        @Test
        @DisplayName("cada acquire genera sessionId único")
        void acquire_multipleAcquires_generateUniqueSessionIds() {
            String s1 = registry.acquire("chromium", Duration.ofSeconds(1)).orElseThrow().sessionId();
            String s2 = registry.acquire("chromium", Duration.ofSeconds(1)).orElseThrow().sessionId();

            assertThat(s1).isNotEqualTo(s2);
        }

        @Test
        @DisplayName("handle tiene leaseDuration >= 100 años (prácticamente infinita)")
        void acquire_handle_hasLongLease() {
            DeviceHandle handle = registry.acquire("webkit", Duration.ofSeconds(1)).orElseThrow();

            assertThat(handle.leaseDuration())
                    .isEqualTo(PlaywrightCapabilityRegistry.WEB_LEASE)
                    .isGreaterThanOrEqualTo(Duration.ofDays(365L * 100));
        }

        @Test
        @DisplayName("handle no está expirado inmediatamente después de acquire")
        void acquire_handle_isNotExpiredImmediately() {
            DeviceHandle handle = registry.acquire("chromium", Duration.ofSeconds(1)).orElseThrow();

            assertThat(handle.isExpired()).isFalse();
        }

        @Test
        @DisplayName("retorna Optional.empty para browser no soportado")
        void acquire_unsupportedBrowser_returnsEmpty() {
            Optional<DeviceHandle> handle = registry.acquire("ie", Duration.ofSeconds(30));

            assertThat(handle).isEmpty();
        }

        @Test
        @DisplayName("lanza NullPointerException si capabilityId es null")
        void acquire_nullCapabilityId_throwsNPE() {
            assertThatThrownBy(() -> registry.acquire(null, Duration.ofSeconds(1)))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        @DisplayName("lanza excepción si capabilityId es blank o null")
        void acquire_blankCapabilityId_throwsException(String badId) {
            if (badId == null) {
                assertThatThrownBy(() -> registry.acquire(badId, Duration.ofSeconds(1)))
                        .isInstanceOf(NullPointerException.class);
            } else {
                assertThatThrownBy(() -> registry.acquire(badId, Duration.ofSeconds(1)))
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }

        @Test
        @DisplayName("lanza NullPointerException si timeout es null")
        void acquire_nullTimeout_throwsNPE() {
            assertThatThrownBy(() -> registry.acquire("chromium", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si timeout es cero")
        void acquire_zeroTimeout_throwsIllegalArgument() {
            assertThatThrownBy(() -> registry.acquire("chromium", Duration.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si timeout es negativo")
        void acquire_negativeTimeout_throwsIllegalArgument() {
            assertThatThrownBy(() -> registry.acquire("chromium", Duration.ofSeconds(-1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // release()
    // =========================================================================

    @Nested
    @DisplayName("release()")
    class ReleaseTests {

        @Test
        @DisplayName("release es no-op — no lanza excepción")
        void release_validHandle_isNoOp() {
            DeviceHandle handle = registry.acquire("chromium", Duration.ofSeconds(1)).orElseThrow();

            // No lanza excepción
            registry.release(handle);
        }

        @Test
        @DisplayName("release es idempotente — doble llamada no falla")
        void release_calledTwice_isIdempotent() {
            DeviceHandle handle = registry.acquire("firefox", Duration.ofSeconds(1)).orElseThrow();

            registry.release(handle);
            registry.release(handle); // segunda llamada silenciosa
        }

        @Test
        @DisplayName("release con null no lanza excepción (contrato idempotente)")
        void release_null_doesNotThrow() {
            // no-op incluso con null — web no tiene estado que limpiar
            registry.release(null);
        }
    }

    // =========================================================================
    // Contrato de simetría completo
    // =========================================================================

    @Test
    @DisplayName("ciclo acquire → uso → release no modifica disponibilidad")
    void fullCycle_availabilityIsUnchangedAfterRelease() {
        assertThat(registry.isAvailable("chromium")).isTrue();

        DeviceHandle handle = registry.acquire("chromium", Duration.ofSeconds(1)).orElseThrow();
        assertThat(registry.isAvailable("chromium")).isTrue(); // sigue disponible

        registry.release(handle);
        assertThat(registry.isAvailable("chromium")).isTrue(); // aún disponible
    }
}
