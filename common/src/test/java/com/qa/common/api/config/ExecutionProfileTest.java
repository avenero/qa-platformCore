package com.qa.common.api.config;

import com.qa.common.api.runtime.ExecutionConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ExecutionProfile} (INIT-EXEC-PROFILE / IEP-1).
 */
@DisplayName("ExecutionProfile")
class ExecutionProfileTest {

    @Nested
    @DisplayName("fromEnv — fail-closed parsing (ADR-IEP-1)")
    class FromEnv {

        @ParameterizedTest
        @CsvSource({
                "DEV,DEV",
                "TEST,TEST",
                "STAGING,STAGING",
                "PROD,PROD",
                "dev,DEV",
                "Prod,PROD",
                "staging,STAGING"
        })
        @DisplayName("recognised values resolve case-insensitively")
        void recognisedValues(String raw, ExecutionProfile expected) {
            assertThat(ExecutionProfile.fromEnv(raw)).isEqualTo(expected);
        }

        @Test
        @DisplayName("null → PROD (fail-closed)")
        void nullDefaultsToProd() {
            assertThat(ExecutionProfile.fromEnv(null)).isEqualTo(ExecutionProfile.PROD);
        }

        @Test
        @DisplayName("empty/whitespace → PROD (fail-closed)")
        void blankDefaultsToProd() {
            assertThat(ExecutionProfile.fromEnv("")).isEqualTo(ExecutionProfile.PROD);
            assertThat(ExecutionProfile.fromEnv(" ")).isEqualTo(ExecutionProfile.PROD);
            assertThat(ExecutionProfile.fromEnv("\t")).isEqualTo(ExecutionProfile.PROD);
            assertThat(ExecutionProfile.fromEnv("\n")).isEqualTo(ExecutionProfile.PROD);
        }

        @ParameterizedTest
        @ValueSource(strings = {"BANANA", "production", "qa", "PRD"})
        @DisplayName("unknown → PROD (fail-closed)")
        void unknownDefaultsToProd(String raw) {
            assertThat(ExecutionProfile.fromEnv(raw)).isEqualTo(ExecutionProfile.PROD);
        }
    }

    @Nested
    @DisplayName("Helpers")
    class Helpers {
        @Test
        void isProd_onlyForProd() {
            assertThat(ExecutionProfile.PROD.isProd()).isTrue();
            assertThat(ExecutionProfile.DEV.isProd()).isFalse();
            assertThat(ExecutionProfile.TEST.isProd()).isFalse();
            assertThat(ExecutionProfile.STAGING.isProd()).isFalse();
        }

        @Test
        void isPrivate_onlyForDevAndTest() {
            assertThat(ExecutionProfile.DEV.isPrivate()).isTrue();
            assertThat(ExecutionProfile.TEST.isPrivate()).isTrue();
            assertThat(ExecutionProfile.STAGING.isPrivate()).isFalse();
            assertThat(ExecutionProfile.PROD.isPrivate()).isFalse();
        }
    }

    @Nested
    @DisplayName("ExecutionConfig.Builder.profile() propagation")
    class BuilderPropagation {
        @Test
        @DisplayName("default profile is PROD (fail-closed)")
        void defaultIsProd() {
            ExecutionConfig cfg = new ExecutionConfig.Builder().build();
            assertThat(cfg.getProfile()).isEqualTo(ExecutionProfile.PROD);
        }

        @Test
        @DisplayName("builder.profile() overrides default")
        void builderSets() {
            ExecutionConfig cfg = new ExecutionConfig.Builder()
                    .profile(ExecutionProfile.DEV)
                    .build();
            assertThat(cfg.getProfile()).isEqualTo(ExecutionProfile.DEV);
        }

        @Test
        @DisplayName("builder.profile(null) rejects with NPE")
        void builderRejectsNull() {
            assertThatThrownBy(() -> new ExecutionConfig.Builder().profile(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("profile");
        }

        @Test
        @DisplayName("toString includes profile")
        void toStringIncludesProfile() {
            ExecutionConfig cfg = new ExecutionConfig.Builder()
                    .profile(ExecutionProfile.STAGING)
                    .build();
            assertThat(cfg.toString()).contains("profile=STAGING");
        }
    }
}
