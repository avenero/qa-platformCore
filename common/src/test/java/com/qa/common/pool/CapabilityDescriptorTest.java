package com.qa.common.pool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class CapabilityDescriptorTest {

    // =========================================================================
    // Constructor canónico — casos felices
    // =========================================================================

    @Test
    void canonicalConstructor_fullFields_createsCorrectRecord() {
        var meta = Map.of("os_version", "13", "screen", "1080x2400");
        var d = new CapabilityDescriptor(
            "pixel7", "Pixel 7 (Android 13)", "ANDROID", "REAL",
            false, true, 45L, meta
        );

        assertThat(d.id()).isEqualTo("pixel7");
        assertThat(d.name()).isEqualTo("Pixel 7 (Android 13)");
        assertThat(d.platformId()).isEqualTo("ANDROID");
        assertThat(d.type()).isEqualTo("REAL");
        assertThat(d.available()).isFalse();
        assertThat(d.inUse()).isTrue();
        assertThat(d.estimatedFreeInSeconds()).isEqualTo(45L);
        assertThat(d.metadata()).containsAllEntriesOf(meta);
    }

    @Test
    void canonicalConstructor_nullMetadata_defaultsToEmptyMap() {
        var d = new CapabilityDescriptor(
            "chromium", "Chromium", "WEB", "BROWSER", true, false, null, null);

        assertThat(d.metadata()).isEmpty();
    }

    @Test
    void canonicalConstructor_mutableMetadata_isDefensivelyCopied() {
        var mutableMeta = new HashMap<String, String>();
        mutableMeta.put("key", "value");

        var d = new CapabilityDescriptor(
            "chromium", "Chromium", "WEB", "BROWSER", true, false, null, mutableMeta);

        mutableMeta.put("injected", "attack");    // mutar el map original
        assertThat(d.metadata()).doesNotContainKey("injected");
    }

    // =========================================================================
    // Factory available()
    // =========================================================================

    @Test
    void available_factory_setsCorrectDefaults() {
        var d = CapabilityDescriptor.available("chromium", "Chromium", "WEB", "BROWSER");

        assertThat(d.available()).isTrue();
        assertThat(d.inUse()).isFalse();
        assertThat(d.estimatedFreeInSeconds()).isNull();
        assertThat(d.metadata()).isEmpty();
    }

    @Test
    void available_factory_setsFields() {
        var d = CapabilityDescriptor.available("mysql", "MySQL 8.x", "DATABASE", "DATABASE");

        assertThat(d.id()).isEqualTo("mysql");
        assertThat(d.platformId()).isEqualTo("DATABASE");
        assertThat(d.type()).isEqualTo("DATABASE");
    }

    // =========================================================================
    // Factory inUse()
    // =========================================================================

    @Test
    void inUse_factory_withEta_setsCorrectState() {
        var d = CapabilityDescriptor.inUse("pixel7", "Pixel 7", "ANDROID", "REAL", 60L);

        assertThat(d.available()).isFalse();
        assertThat(d.inUse()).isTrue();
        assertThat(d.estimatedFreeInSeconds()).isEqualTo(60L);
    }

    @Test
    void inUse_factory_nullEta_isAllowed() {
        var d = CapabilityDescriptor.inUse("iphone15", "iPhone 15", "IOS", "REAL", null);

        assertThat(d.estimatedFreeInSeconds()).isNull();
    }

    // =========================================================================
    // Validación de campos obligatorios
    // =========================================================================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void canonicalConstructor_blankId_throwsIllegalArgumentException(String badId) {
        assertThatThrownBy(() ->
            new CapabilityDescriptor(badId, "Name", "WEB", "BROWSER", true, false, null, Map.of())
        ).isInstanceOf(badId == null ? NullPointerException.class : IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void canonicalConstructor_blankName_throwsException(String badName) {
        assertThatThrownBy(() ->
            new CapabilityDescriptor("id", badName, "WEB", "BROWSER", true, false, null, Map.of())
        ).isInstanceOf(badName == null ? NullPointerException.class : IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void canonicalConstructor_blankPlatformId_throwsException(String badPlatform) {
        assertThatThrownBy(() ->
            new CapabilityDescriptor("id", "Name", badPlatform, "BROWSER", true, false, null, Map.of())
        ).isInstanceOf(badPlatform == null ? NullPointerException.class : IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void canonicalConstructor_blankType_throwsException(String badType) {
        assertThatThrownBy(() ->
            new CapabilityDescriptor("id", "Name", "WEB", badType, true, false, null, Map.of())
        ).isInstanceOf(badType == null ? NullPointerException.class : IllegalArgumentException.class);
    }

    // =========================================================================
    // equals / hashCode / toString (auto de record)
    // =========================================================================

    @Test
    void record_equalsSameValues() {
        var a = CapabilityDescriptor.available("chromium", "Chromium", "WEB", "BROWSER");
        var b = CapabilityDescriptor.available("chromium", "Chromium", "WEB", "BROWSER");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void record_notEqualsDifferentId() {
        var a = CapabilityDescriptor.available("chromium", "Chromium", "WEB", "BROWSER");
        var b = CapabilityDescriptor.available("firefox", "Firefox", "WEB", "BROWSER");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void record_toStringContainsAllFields() {
        var d = CapabilityDescriptor.available("chromium", "Chromium", "WEB", "BROWSER");
        var str = d.toString();

        assertThat(str)
            .contains("chromium")
            .contains("Chromium")
            .contains("WEB")
            .contains("BROWSER");
    }
}
