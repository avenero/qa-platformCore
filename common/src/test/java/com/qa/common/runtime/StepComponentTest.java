package com.qa.common.runtime;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para {@link StepComponent}.
 * Usa una implementacion dummy para verificar el contrato de la interfaz.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("StepComponent")
class StepComponentTest {

    /**
     * Implementacion minima para testing.
     */
    static class DummyStepComponent implements StepComponent {
        private final String name;
        private final BddPhase phase;

        DummyStepComponent(String name, BddPhase phase) {
            this.name = name;
            this.phase = phase;
        }

        @Override
        public String getName() { return name; }

        @Override
        public BddPhase getPhase() { return phase; }

        @Override
        public Class<?> getStepDefinitionClass() { return DummyStepComponent.class; }
    }

    /**
     * Implementacion con tags personalizados.
     */
    static class TaggedStepComponent implements StepComponent {
        @Override
        public String getName() { return "Tagged"; }

        @Override
        public BddPhase getPhase() { return BddPhase.WHEN; }

        @Override
        public Class<?> getStepDefinitionClass() { return TaggedStepComponent.class; }

        @Override
        public List<String> getRequiredTags() { return List.of("@api", "@rest"); }
    }

    @Nested
    @DisplayName("Contrato basico")
    class ContratoBasicoTests {

        @Test
        @DisplayName("getName retorna el nombre configurado")
        void getNameRetornaNombreConfigurado() {
            StepComponent component = new DummyStepComponent("HTTP Request", BddPhase.WHEN);
            assertThat(component.getName()).isEqualTo("HTTP Request");
        }

        @Test
        @DisplayName("getPhase retorna la fase configurada")
        void getPhaseRetornaFaseConfigurada() {
            StepComponent component = new DummyStepComponent("Setup", BddPhase.GIVEN);
            assertThat(component.getPhase()).isEqualTo(BddPhase.GIVEN);
        }

        @Test
        @DisplayName("getStepDefinitionClass retorna clase no null")
        void getStepDefinitionClassRetornaClaseNoNull() {
            StepComponent component = new DummyStepComponent("Test", BddPhase.THEN);
            assertThat(component.getStepDefinitionClass()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Metodos default")
    class MetodosDefaultTests {

        @Test
        @DisplayName("getDescription genera formato 'nombre [label]'")
        void getDescriptionGeneraFormatoCorrecto() {
            StepComponent component = new DummyStepComponent("Response Validation", BddPhase.THEN);
            assertThat(component.getDescription()).isEqualTo("Response Validation [Then]");
        }

        @Test
        @DisplayName("getDescription para GIVEN genera formato correcto")
        void getDescriptionParaGivenFormatoCorrecto() {
            StepComponent component = new DummyStepComponent("Database Setup", BddPhase.GIVEN);
            assertThat(component.getDescription()).isEqualTo("Database Setup [Given]");
        }

        @Test
        @DisplayName("getRequiredTags retorna lista vacia por defecto")
        void getRequiredTagsRetornaListaVaciaPorDefecto() {
            StepComponent component = new DummyStepComponent("Test", BddPhase.WHEN);
            assertThat(component.getRequiredTags()).isEmpty();
        }

        @Test
        @DisplayName("getRequiredTags puede ser sobrescrito con tags personalizados")
        void getRequiredTagsConTagsPersonalizados() {
            StepComponent component = new TaggedStepComponent();
            assertThat(component.getRequiredTags())
                    .hasSize(2)
                    .containsExactly("@api", "@rest");
        }
    }
}

