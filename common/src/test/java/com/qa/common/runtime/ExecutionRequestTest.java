package com.qa.common.runtime;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para {@link ExecutionRequest}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("ExecutionRequest")
class ExecutionRequestTest {

    private ExecutionConfig config;

    @BeforeEach
    void setUp() {
        config = new ExecutionConfig.Builder()
                .environment("test")
                .tags("@smoke")
                .build();
    }

    @Nested
    @DisplayName("Creacion con factory method")
    class CreacionTests {

        @Test
        @DisplayName("of() crea request con datos correctos")
        void ofCreaRequestConDatosCorrectos() {
            List<String> features = List.of("src/test/resources/login.feature");
            List<String> glue = List.of("com.qa.steps");

            ExecutionRequest request = ExecutionRequest.of(features, glue, config);

            assertThat(request.getFeaturePaths()).containsExactly("src/test/resources/login.feature");
            assertThat(request.getGluePaths()).containsExactly("com.qa.steps");
            assertThat(request.getConfig()).isSameAs(config);
        }

        @Test
        @DisplayName("of() con multiples paths funciona correctamente")
        void ofConMultiplesPathsFuncionaCorrectamente() {
            List<String> features = List.of("login.feature", "register.feature", "checkout.feature");
            List<String> glue = List.of("com.qa.steps", "com.qa.hooks");

            ExecutionRequest request = ExecutionRequest.of(features, glue, config);

            assertThat(request.getFeaturePaths()).hasSize(3);
            assertThat(request.getGluePaths()).hasSize(2);
        }

        @Test
        @DisplayName("featurePaths null lanza NullPointerException")
        void featurePathsNullLanzaNPE() {
            assertThatThrownBy(() -> ExecutionRequest.of(null, List.of(), config))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("gluePaths null lanza NullPointerException")
        void gluePathsNullLanzaNPE() {
            assertThatThrownBy(() -> ExecutionRequest.of(List.of(), null, config))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("config null lanza NullPointerException")
        void configNullLanzaNPE() {
            assertThatThrownBy(() -> ExecutionRequest.of(List.of(), List.of(), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Inmutabilidad")
    class InmutabilidadTests {

        @Test
        @DisplayName("featurePaths es inmutable")
        void featurePathsEsInmutable() {
            ExecutionRequest request = ExecutionRequest.of(
                    List.of("test.feature"), List.of("glue"), config);

            assertThatThrownBy(() -> request.getFeaturePaths().add("new.feature"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("gluePaths es inmutable")
        void gluePathsEsInmutable() {
            ExecutionRequest request = ExecutionRequest.of(
                    List.of("test.feature"), List.of("glue"), config);

            assertThatThrownBy(() -> request.getGluePaths().add("new.glue"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString con glue explicito muestra el conteo numerico")
        void toStringContieneConteos() {
            ExecutionRequest request = ExecutionRequest.of(
                    List.of("a.feature", "b.feature"),
                    List.of("com.steps"),
                    config);

            String str = request.toString();
            assertThat(str).contains("features=2").contains("glue=1");
        }

        @Test
        @DisplayName("toString con glue auto muestra 'auto(SPI)'")
        void toStringConGlueAutoMuestraLabel() {
            ExecutionRequest request = ExecutionRequest.of(
                    List.of("a.feature"), config);

            assertThat(request.toString()).contains("auto(SPI)");
        }
    }

    // =========================================================================
    // Factory method SPI (2 argumentos) — @since 2.2.0
    // =========================================================================

    @Nested
    @DisplayName("of(featurePaths, config) — modo SPI sin gluePaths")
    class FactorySinGlueTests {

        @Test
        @DisplayName("Crea request con gluePaths vacio")
        void creaRequestConGlueVacio() {
            ExecutionRequest request = ExecutionRequest.of(
                    List.of("login.feature"), config);

            assertThat(request.getGluePaths()).isEmpty();
        }

        @Test
        @DisplayName("isGlueAutoResolved() retorna true")
        void isGlueAutoResolvedRetornaTrue() {
            ExecutionRequest request = ExecutionRequest.of(
                    List.of("login.feature"), config);

            assertThat(request.isGlueAutoResolved()).isTrue();
        }

        @Test
        @DisplayName("featurePaths y config se preservan")
        void featurePathsYConfigSePreservan() {
            List<String> features = List.of("a.feature", "b.feature");
            ExecutionRequest request = ExecutionRequest.of(features, config);

            assertThat(request.getFeaturePaths()).containsExactlyInAnyOrderElementsOf(features);
            assertThat(request.getConfig()).isSameAs(config);
        }

        @Test
        @DisplayName("featurePaths null lanza NullPointerException")
        void featurePathsNullLanzaNPE() {
            assertThatThrownBy(() -> ExecutionRequest.of(null, config))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("config null lanza NullPointerException")
        void configNullLanzaNPE() {
            assertThatThrownBy(() -> ExecutionRequest.of(List.of("f.feature"), (ExecutionConfig) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("gluePaths resultante es inmutable")
        void gluePathsEsInmutable() {
            ExecutionRequest request = ExecutionRequest.of(List.of("f.feature"), config);

            assertThatThrownBy(() -> request.getGluePaths().add("com.qa.steps"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("isGlueAutoResolved()")
    class IsGlueAutoResolvedTests {

        @Test
        @DisplayName("Retorna false cuando se proporcionan gluePaths explícitos")
        void retornsFalseConGlueExplicito() {
            ExecutionRequest request = ExecutionRequest.of(
                    List.of("f.feature"), List.of("com.qa.steps"), config);

            assertThat(request.isGlueAutoResolved()).isFalse();
        }

        @Test
        @DisplayName("Retorna true cuando gluePaths es vacio (factory de 3 args con lista vacia)")
        void retornsTrueConGlueVacioExplicito() {
            ExecutionRequest request = ExecutionRequest.of(
                    List.of("f.feature"), List.of(), config);

            assertThat(request.isGlueAutoResolved()).isTrue();
        }

        @Test
        @DisplayName("Retorna true con factory de 2 args")
        void retornsTrueConFactory2Args() {
            assertThat(ExecutionRequest.of(List.of("f.feature"), config).isGlueAutoResolved()).isTrue();
        }
    }
}

