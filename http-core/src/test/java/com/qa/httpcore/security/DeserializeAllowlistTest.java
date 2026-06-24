package com.qa.httpcore.security;

import com.module.models.AllowlistSampleModel;
import com.qa.common.api.config.ConfigLoader;
import com.qa.common.api.config.ConfigLoaderHolder;
import com.qa.common.api.config.ExecutionProfile;
import com.qa.common.api.config.TypedConfig;
import com.qa.common.api.exception.FrameworkTechnicalException;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.httpcore.model.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Cobertura del guard default-deny de carga de clases para deserialización (W2-M2 / hallazgo M-2).
 */
@DisplayName("DeserializeAllowlist — allowlist default-deny de carga de clases (M-2)")
class DeserializeAllowlistTest {

    /** Config mutable por test; respaldada por una instancia fake de {@link ConfigLoader}. */
    private final Map<String, String> config = new HashMap<>();

    @BeforeEach
    void installFakeLoader() {
        config.clear();
        ConfigLoaderHolder.replace(new ConfigLoader() {
            @Override
            public <T extends TypedConfig> T load(Class<T> configClass) {
                throw new UnsupportedOperationException("no usado en este test");
            }

            @Override
            public <T extends TypedConfig> T reload(Class<T> configClass) {
                throw new UnsupportedOperationException("no usado en este test");
            }

            @Override
            public Optional<String> getRaw(String key) {
                return Optional.ofNullable(config.get(key));
            }
        });
    }

    @AfterEach
    void restore() {
        ConfigLoaderHolder.reset();
        ExecutionContext.deactivate();
    }

    @Nested
    @DisplayName("(a) clase en paquete allow-listed por defecto")
    class AllowedByDefault {

        @Test
        @DisplayName("FQCN bajo com.module.models. -> carga")
        void fqcnUnderDefaultPrefixLoads() throws Exception {
            Class<?> resolved =
                DeserializeAllowlist.resolve("com.module.models.AllowlistSampleModel");
            assertThat(resolved).isEqualTo(AllowlistSampleModel.class);
        }

        @Test
        @DisplayName("nombre simple se resuelve via prefijo allow-listed")
        void simpleNameResolvesViaPrefix() throws Exception {
            Class<?> resolved = DeserializeAllowlist.resolve("AllowlistSampleModel");
            assertThat(resolved).isEqualTo(AllowlistSampleModel.class);
        }

        @Test
        @DisplayName("FQCN allow-listed pero inexistente -> ClassNotFoundException (no denegacion)")
        void allowedButMissingThrowsClassNotFound() {
            assertThatExceptionOfType(ClassNotFoundException.class)
                .isThrownBy(() -> DeserializeAllowlist.resolve("com.module.models.NoSuchModel"));
        }

        @Test
        @DisplayName("java.util.Map / java.util.LinkedHashMap (Map generico) cargan por defecto (Lead 2026-06-23)")
        void javaUtilCollectionsAllowedByDefault() throws Exception {
            // Preserva escenarios qa-module-test (21_Workflow_EdgeCases:152, 13_StepsCoverage:233)
            assertThat(DeserializeAllowlist.resolve("java.util.Map")).isEqualTo(java.util.Map.class);
            assertThat(DeserializeAllowlist.resolve("java.util.LinkedHashMap"))
                .isEqualTo(java.util.LinkedHashMap.class);
        }
    }

    @Nested
    @DisplayName("(b) clase arbitraria fuera del allowlist -> denegada")
    class DeniedOutsideAllowlist {

        @Test
        @DisplayName("java.lang.Runtime -> FrameworkTechnicalException con MSG_CLASS_NOT_ALLOWED")
        void runtimeDenied() {
            assertThatExceptionOfType(FrameworkTechnicalException.class)
                .isThrownBy(() -> DeserializeAllowlist.resolve("java.lang.Runtime"))
                .withMessageContaining(DeserializeAllowlist.MSG_CLASS_NOT_ALLOWED);
        }

        @Test
        @DisplayName("FQCN arbitrario fuera del allowlist -> denegado")
        void arbitraryFqcnDenied() {
            assertThatExceptionOfType(FrameworkTechnicalException.class)
                .isThrownBy(() -> DeserializeAllowlist.resolve("com.qa.httpcore.model.HttpResponse"))
                .withMessageContaining(DeserializeAllowlist.MSG_CLASS_NOT_ALLOWED);
        }

        @Test
        @DisplayName("className en blanco -> denegado (fail-closed)")
        void blankDenied() {
            assertThatExceptionOfType(FrameworkTechnicalException.class)
                .isThrownBy(() -> DeserializeAllowlist.resolve("   "))
                .withMessageContaining(DeserializeAllowlist.MSG_CLASS_NOT_ALLOWED);
        }
    }

    @Nested
    @DisplayName("(c) http.deserialize.allowed-packages agrega prefijos")
    class ConfiguredExtraPrefix {

        @Test
        @DisplayName("prefijo configurado (sin punto final) permite una clase antes denegada")
        void configuredPrefixAllowsClass() throws Exception {
            // sin el punto final -> el guard debe normalizarlo para no sangrar a paquetes hermanos
            config.put(DeserializeAllowlist.ALLOWED_PACKAGES_KEY, "com.qa.httpcore.model");

            Class<?> resolved =
                DeserializeAllowlist.resolve("com.qa.httpcore.model.HttpResponse");

            assertThat(resolved).isEqualTo(HttpResponse.class);
        }
    }

    @Nested
    @DisplayName("(d) legacy-mode: allow-any en non-PROD, ignorado en PROD")
    class LegacyMode {

        @Test
        @DisplayName("legacy-mode=true + perfil DEV -> allow-any permite java.lang.Runtime")
        void legacyAllowsAnyInNonProd() throws Exception {
            config.put(DeserializeAllowlist.LEGACY_MODE_KEY, "true");
            activateProfile(ExecutionProfile.DEV);

            Class<?> resolved = DeserializeAllowlist.resolve("java.lang.Runtime");

            assertThat(resolved).isEqualTo(Runtime.class);
        }

        @Test
        @DisplayName("legacy-mode=true sin contexto (fail-closed PROD) -> sigue denegando")
        void legacyIgnoredWhenProdFailClosed() {
            config.put(DeserializeAllowlist.LEGACY_MODE_KEY, "true");
            // sin ExecutionContext activo -> resolveProfile() = PROD (fail-closed)

            assertThatExceptionOfType(FrameworkTechnicalException.class)
                .isThrownBy(() -> DeserializeAllowlist.resolve("java.lang.Runtime"))
                .withMessageContaining(DeserializeAllowlist.MSG_CLASS_NOT_ALLOWED);
        }

        @Test
        @DisplayName("legacy-mode=true + perfil PROD explicito -> ignorado, denegado")
        void legacyIgnoredInExplicitProd() {
            config.put(DeserializeAllowlist.LEGACY_MODE_KEY, "true");
            activateProfile(ExecutionProfile.PROD);

            assertThatExceptionOfType(FrameworkTechnicalException.class)
                .isThrownBy(() -> DeserializeAllowlist.resolve("java.lang.Runtime"))
                .withMessageContaining(DeserializeAllowlist.MSG_CLASS_NOT_ALLOWED);
        }

        private void activateProfile(ExecutionProfile profile) {
            ExecutionContext.builder()
                .config(new ExecutionConfig.Builder().profile(profile).build())
                .build()
                .activate();
        }
    }

    @Test
    @DisplayName("El constructor privado lanza UnsupportedOperationException")
    void privateConstructorThrows() {
        assertThatExceptionOfType(java.lang.reflect.InvocationTargetException.class)
            .isThrownBy(() -> {
                var ctor = DeserializeAllowlist.class.getDeclaredConstructor();
                ctor.setAccessible(true);
                ctor.newInstance();
            })
            .withCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("MSG_CLASS_NOT_ALLOWED no contiene class-names (clave-libre, §D.4)")
    void messageIsClassNameFree() {
        assertThatNoException().isThrownBy(() ->
            assertThat(DeserializeAllowlist.MSG_CLASS_NOT_ALLOWED)
                .doesNotContain("java.lang", ".class"));
    }
}
