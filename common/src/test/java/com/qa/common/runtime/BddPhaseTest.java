package com.qa.common.runtime;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para {@link BddPhase}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("BddPhase")
class BddPhaseTest {

    @Nested
    @DisplayName("Valores del enum")
    class ValoresEnumTests {

        @Test
        @DisplayName("Debe tener exactamente 3 valores: GIVEN, WHEN, THEN")
        void debeContenerTresValores() {
            assertThat(BddPhase.values()).hasSize(3);
            assertThat(BddPhase.values()).containsExactly(
                    BddPhase.GIVEN, BddPhase.WHEN, BddPhase.THEN
            );
        }

        @Test
        @DisplayName("valueOf GIVEN retorna GIVEN")
        void valueOfGivenRetornaGiven() {
            assertThat(BddPhase.valueOf("GIVEN")).isEqualTo(BddPhase.GIVEN);
        }

        @Test
        @DisplayName("valueOf WHEN retorna WHEN")
        void valueOfWhenRetornaWhen() {
            assertThat(BddPhase.valueOf("WHEN")).isEqualTo(BddPhase.WHEN);
        }

        @Test
        @DisplayName("valueOf THEN retorna THEN")
        void valueOfThenRetornaThen() {
            assertThat(BddPhase.valueOf("THEN")).isEqualTo(BddPhase.THEN);
        }

        @Test
        @DisplayName("valueOf con valor invalido lanza IllegalArgumentException")
        void valueOfInvalidoLanzaExcepcion() {
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> BddPhase.valueOf("AND")
            );
        }
    }

    @Nested
    @DisplayName("Labels")
    class LabelTests {

        @Test
        @DisplayName("GIVEN tiene label 'Given'")
        void givenTieneLabelGiven() {
            assertThat(BddPhase.GIVEN.getLabel()).isEqualTo("Given");
        }

        @Test
        @DisplayName("WHEN tiene label 'When'")
        void whenTieneLabelWhen() {
            assertThat(BddPhase.WHEN.getLabel()).isEqualTo("When");
        }

        @Test
        @DisplayName("THEN tiene label 'Then'")
        void thenTieneLabelThen() {
            assertThat(BddPhase.THEN.getLabel()).isEqualTo("Then");
        }

        @ParameterizedTest
        @EnumSource(BddPhase.class)
        @DisplayName("Todos los labels no son null ni vacios")
        void todosLosLabelsNoSonNullNiVacios(BddPhase phase) {
            assertThat(phase.getLabel()).isNotNull().isNotEmpty();
        }
    }
}

