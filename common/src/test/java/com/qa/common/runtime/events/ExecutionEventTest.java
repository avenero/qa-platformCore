package com.qa.common.runtime.events;

import org.junit.jupiter.api.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para {@link ExecutionEvent}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("ExecutionEvent")
class ExecutionEventTest {

    @Nested
    @DisplayName("Creacion con factory methods")
    class CreacionTests {

        @Test
        @DisplayName("of(type, name) crea evento sin datos")
        void ofSinDatosCreaEvento() {
            ExecutionEvent event = ExecutionEvent.of(
                    ExecutionEvent.Type.SCENARIO_START, "Login Scenario");

            assertThat(event.getType()).isEqualTo(ExecutionEvent.Type.SCENARIO_START);
            assertThat(event.getName()).isEqualTo("Login Scenario");
            assertThat(event.getData()).isEmpty();
            assertThat(event.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("of(type, name, data) crea evento con datos")
        void ofConDatosCreaEvento() {
            Map<String, Object> data = Map.of("status", "passed", "duration", 150L);
            ExecutionEvent event = ExecutionEvent.of(
                    ExecutionEvent.Type.SCENARIO_END, "Login Scenario", data);

            assertThat(event.getType()).isEqualTo(ExecutionEvent.Type.SCENARIO_END);
            assertThat(event.getName()).isEqualTo("Login Scenario");
            assertThat(event.getData()).hasSize(2);
        }

        @Test
        @DisplayName("type null lanza NullPointerException")
        void typeNullLanzaNPE() {
            assertThatThrownBy(() -> ExecutionEvent.of(null, "test"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("name null lanza NullPointerException")
        void nameNullLanzaNPE() {
            assertThatThrownBy(() -> ExecutionEvent.of(ExecutionEvent.Type.CUSTOM, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("data null lanza NullPointerException")
        void dataNullLanzaNPE() {
            assertThatThrownBy(() -> ExecutionEvent.of(ExecutionEvent.Type.CUSTOM, "test", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Inmutabilidad")
    class InmutabilidadTests {

        @Test
        @DisplayName("getData retorna mapa inmutable")
        void getDataRetornaMapaInmutable() {
            ExecutionEvent event = ExecutionEvent.of(
                    ExecutionEvent.Type.STEP_START, "Given setup",
                    Map.of("step", "dado que configuro"));

            assertThatThrownBy(() -> event.getData().put("new", "value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("getData tipado")
    class GetDataTipadoTests {

        @Test
        @DisplayName("getData con tipo correcto retorna valor casteado")
        void getDataConTipoCorrectoRetornaValor() {
            ExecutionEvent event = ExecutionEvent.of(
                    ExecutionEvent.Type.CUSTOM, "test",
                    Map.of("count", 42));

            Integer count = event.getData("count", Integer.class);
            assertThat(count).isEqualTo(42);
        }

        @Test
        @DisplayName("getData con tipo incorrecto retorna null")
        void getDataConTipoIncorrectoRetornaNull() {
            ExecutionEvent event = ExecutionEvent.of(
                    ExecutionEvent.Type.CUSTOM, "test",
                    Map.of("count", 42));

            String wrongType = event.getData("count", String.class);
            assertThat(wrongType).isNull();
        }

        @Test
        @DisplayName("getData con clave inexistente retorna null")
        void getDataConClaveInexistenteRetornaNull() {
            ExecutionEvent event = ExecutionEvent.of(ExecutionEvent.Type.CUSTOM, "test");

            String value = event.getData("noExiste", String.class);
            assertThat(value).isNull();
        }
    }

    @Nested
    @DisplayName("Enum Type")
    class TypeEnumTests {

        @Test
        @DisplayName("Tiene los 7 tipos esperados")
        void tieneSieteTipos() {
            assertThat(ExecutionEvent.Type.values()).hasSize(7);
        }

        @Test
        @DisplayName("Contiene todos los tipos del ciclo de vida")
        void contieneTodosLosTipos() {
            assertThat(ExecutionEvent.Type.values()).containsExactly(
                    ExecutionEvent.Type.EXECUTION_START,
                    ExecutionEvent.Type.EXECUTION_END,
                    ExecutionEvent.Type.SCENARIO_START,
                    ExecutionEvent.Type.SCENARIO_END,
                    ExecutionEvent.Type.STEP_START,
                    ExecutionEvent.Type.STEP_END,
                    ExecutionEvent.Type.CUSTOM
            );
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString contiene tipo y nombre")
        void toStringContieneInfo() {
            ExecutionEvent event = ExecutionEvent.of(
                    ExecutionEvent.Type.SCENARIO_START, "Login Test");
            String str = event.toString();
            assertThat(str).contains("SCENARIO_START").contains("Login Test");
        }
    }
}

