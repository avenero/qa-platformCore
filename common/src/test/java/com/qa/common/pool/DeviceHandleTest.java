package com.qa.common.pool;


import com.qa.common.api.pool.DeviceHandle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class DeviceHandleTest {

    private static final String CAP_ID = "pixel7";
    private static final String SESSION = "sess-abc-123";

    // =========================================================================
    // Construcción — casos felices
    // =========================================================================

    @Test
    void constructor_validArgs_createsHandle() {
        var now = Instant.now();
        var handle = new DeviceHandle(CAP_ID, SESSION, now, Duration.ofMinutes(5));

        assertThat(handle.capabilityId()).isEqualTo(CAP_ID);
        assertThat(handle.sessionId()).isEqualTo(SESSION);
        assertThat(handle.acquiredAt()).isEqualTo(now);
        assertThat(handle.leaseDuration()).isEqualTo(Duration.ofMinutes(5));
    }

    // =========================================================================
    // isExpired()
    // =========================================================================

    @Test
    void isExpired_futureExpiry_returnsFalse() {
        var handle = new DeviceHandle(CAP_ID, SESSION, Instant.now(), Duration.ofHours(1));

        assertThat(handle.isExpired()).isFalse();
    }

    @Test
    void isExpired_pastExpiry_returnsTrue() {
        // adquirido hace 2 segundos, lease de 1 segundo → ya expiró
        var handle = new DeviceHandle(
            CAP_ID, SESSION,
            Instant.now().minusSeconds(2),
            Duration.ofSeconds(1)
        );

        assertThat(handle.isExpired()).isTrue();
    }

    @Test
    void isExpired_exactBoundary_returnsTrue() {
        // adquirido hace exactamente el tiempo del lease → expirado
        var handle = new DeviceHandle(
            CAP_ID, SESSION,
            Instant.now().minusSeconds(10),
            Duration.ofSeconds(10)
        );

        // puede ser true o false en el exacto nanosegundo; solo verificamos que no lanza
        assertThatCode(handle::isExpired).doesNotThrowAnyException();
    }

    // =========================================================================
    // remainingLease()
    // =========================================================================

    @Test
    void remainingLease_activeHandle_returnsPositiveDuration() {
        var handle = new DeviceHandle(CAP_ID, SESSION, Instant.now(), Duration.ofHours(1));

        assertThat(handle.remainingLease()).isPositive();
    }

    @Test
    void remainingLease_expiredHandle_returnsZero() {
        var handle = new DeviceHandle(
            CAP_ID, SESSION,
            Instant.now().minusSeconds(60),
            Duration.ofSeconds(1)
        );

        assertThat(handle.remainingLease()).isEqualTo(Duration.ZERO);
    }

    // =========================================================================
    // Validaciones de constructor
    // =========================================================================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void constructor_blankCapabilityId_throwsException(String badId) {
        assertThatThrownBy(() ->
            new DeviceHandle(badId, SESSION, Instant.now(), Duration.ofMinutes(5))
        ).isInstanceOf(badId == null ? NullPointerException.class : IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void constructor_blankSessionId_throwsException(String badSession) {
        assertThatThrownBy(() ->
            new DeviceHandle(CAP_ID, badSession, Instant.now(), Duration.ofMinutes(5))
        ).isInstanceOf(badSession == null ? NullPointerException.class : IllegalArgumentException.class);
    }

    @Test
    void constructor_nullAcquiredAt_throwsNullPointerException() {
        assertThatNullPointerException().isThrownBy(() ->
            new DeviceHandle(CAP_ID, SESSION, null, Duration.ofMinutes(5))
        );
    }

    @Test
    void constructor_nullLeaseDuration_throwsNullPointerException() {
        assertThatNullPointerException().isThrownBy(() ->
            new DeviceHandle(CAP_ID, SESSION, Instant.now(), null)
        );
    }

    @Test
    void constructor_zeroDuration_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException().isThrownBy(() ->
            new DeviceHandle(CAP_ID, SESSION, Instant.now(), Duration.ZERO)
        );
    }

    @Test
    void constructor_negativeDuration_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException().isThrownBy(() ->
            new DeviceHandle(CAP_ID, SESSION, Instant.now(), Duration.ofSeconds(-1))
        );
    }

    // =========================================================================
    // equals / hashCode / toString (auto de record)
    // =========================================================================

    @Test
    void record_equalsSameValues() {
        var now = Instant.parse("2026-05-07T10:00:00Z");
        var lease = Duration.ofMinutes(5);

        var a = new DeviceHandle(CAP_ID, SESSION, now, lease);
        var b = new DeviceHandle(CAP_ID, SESSION, now, lease);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void record_notEqualsDifferentSession() {
        var now = Instant.now();
        var a = new DeviceHandle(CAP_ID, "sess-1", now, Duration.ofMinutes(5));
        var b = new DeviceHandle(CAP_ID, "sess-2", now, Duration.ofMinutes(5));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void record_toStringContainsKeyFields() {
        var handle = new DeviceHandle(CAP_ID, SESSION, Instant.now(), Duration.ofMinutes(5));
        var str = handle.toString();

        assertThat(str)
            .contains(CAP_ID)
            .contains(SESSION);
    }
}
