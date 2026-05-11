package com.qa.common.runtime;


import com.qa.common.api.runtime.ExecutionResult;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para {@link ExecutionResult}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("ExecutionResult")
class ExecutionResultTest {

    @Nested
    @DisplayName("Builder - valores por defecto")
    class BuilderDefaultsTests {

        @Test
        @DisplayName("Builder con defaults tiene status PASSED")
        void builderConDefaultsTieneStatusPassed() {
            ExecutionResult result = new ExecutionResult.Builder().build();
            assertThat(result.getStatus()).isEqualTo(ExecutionResult.Status.PASSED);
        }

        @Test
        @DisplayName("Builder con defaults tiene duration ZERO")
        void builderConDefaultsTieneDurationZero() {
            ExecutionResult result = new ExecutionResult.Builder().build();
            assertThat(result.getDuration()).isEqualTo(Duration.ZERO);
        }

        @Test
        @DisplayName("Builder con defaults tiene errors vacio")
        void builderConDefaultsTieneErrorsVacio() {
            ExecutionResult result = new ExecutionResult.Builder().build();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Builder con defaults tiene startTime y endTime no null")
        void builderConDefaultsTieneTiemposNoNull() {
            ExecutionResult result = new ExecutionResult.Builder().build();
            assertThat(result.getStartTime()).isNotNull();
            assertThat(result.getEndTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Builder - configuracion personalizada")
    class BuilderCustomTests {

        @Test
        @DisplayName("Configurar resultado exitoso completo")
        void configurarResultadoExitosoCompleto() {
            Instant start = Instant.now();
            Instant end = start.plusSeconds(45);

            ExecutionResult result = new ExecutionResult.Builder()
                    .status(ExecutionResult.Status.PASSED)
                    .totalScenarios(10)
                    .passedScenarios(10)
                    .failedScenarios(0)
                    .duration(Duration.ofSeconds(45))
                    .startTime(start)
                    .endTime(end)
                    .build();

            assertThat(result.getStatus()).isEqualTo(ExecutionResult.Status.PASSED);
            assertThat(result.getTotalScenarios()).isEqualTo(10);
            assertThat(result.getPassedScenarios()).isEqualTo(10);
            assertThat(result.getFailedScenarios()).isZero();
            assertThat(result.getDuration()).isEqualTo(Duration.ofSeconds(45));
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Configurar resultado fallido con errores")
        void configurarResultadoFallidoConErrores() {
            ExecutionResult result = new ExecutionResult.Builder()
                    .status(ExecutionResult.Status.FAILED)
                    .totalScenarios(5)
                    .passedScenarios(3)
                    .failedScenarios(2)
                    .errors(List.of("Login failed", "Timeout in checkout"))
                    .build();

            assertThat(result.getStatus()).isEqualTo(ExecutionResult.Status.FAILED);
            assertThat(result.getFailedScenarios()).isEqualTo(2);
            assertThat(result.getErrors()).hasSize(2)
                    .containsExactly("Login failed", "Timeout in checkout");
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Status ERROR marca isSuccess como false")
        void statusErrorMarcaIsSuccessFalse() {
            ExecutionResult result = new ExecutionResult.Builder()
                    .status(ExecutionResult.Status.ERROR)
                    .build();
            assertThat(result.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder - validaciones")
    class BuilderValidationTests {

        @Test
        @DisplayName("status null lanza NullPointerException")
        void statusNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionResult.Builder().status(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("duration null lanza NullPointerException")
        void durationNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionResult.Builder().duration(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("startTime null lanza NullPointerException")
        void startTimeNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionResult.Builder().startTime(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("endTime null lanza NullPointerException")
        void endTimeNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionResult.Builder().endTime(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("errors null lanza NullPointerException")
        void errorsNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionResult.Builder().errors(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Inmutabilidad")
    class InmutabilidadTests {

        @Test
        @DisplayName("errors es inmutable")
        void errorsEsInmutable() {
            ExecutionResult result = new ExecutionResult.Builder()
                    .errors(List.of("error1"))
                    .build();

            assertThatThrownBy(() -> result.getErrors().add("error2"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Status enum")
    class StatusEnumTests {

        @Test
        @DisplayName("Tiene exactamente 3 valores")
        void tieneTresValores() {
            assertThat(ExecutionResult.Status.values()).hasSize(3);
        }

        @Test
        @DisplayName("Contiene PASSED, FAILED, ERROR")
        void contieneValoresEsperados() {
            assertThat(ExecutionResult.Status.values()).containsExactly(
                    ExecutionResult.Status.PASSED,
                    ExecutionResult.Status.FAILED,
                    ExecutionResult.Status.ERROR
            );
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString contiene informacion relevante")
        void toStringContieneInfo() {
            ExecutionResult result = new ExecutionResult.Builder()
                    .status(ExecutionResult.Status.PASSED)
                    .totalScenarios(5)
                    .passedScenarios(5)
                    .build();

            String str = result.toString();
            assertThat(str).contains("PASSED").contains("total=5").contains("passed=5");
        }
    }
}

