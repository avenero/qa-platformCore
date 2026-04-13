package com.qa.common.runtime;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

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

    /**
     * Implementacion con metadata i18n completa.
     */
    static class I18nStepComponent implements StepComponent {
        @Override
        public String getName() { return "HTTP Request"; }

        @Override
        public BddPhase getPhase() { return BddPhase.WHEN; }

        @Override
        public Class<?> getStepDefinitionClass() { return I18nStepComponent.class; }

        @Override
        public String getDisplayName() { return "HTTP Request"; }

        @Override
        public String getDescription() { return "Descripcion en espanol"; }

        @Override
        public Map<String, String> getDisplayNameByLocale() {
            return Map.of(
                "es", "Peticion HTTP",
                "en", "HTTP Request",
                "fr", "Requete HTTP"
            );
        }

        @Override
        public Map<String, String> getDescriptionByLocale() {
            return Map.of(
                "es", "Descripcion en espanol",
                "en", "Description in English",
                "fr", "Description en francais"
            );
        }
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

    @Nested
    @DisplayName("Metadata i18n — getDisplayNameByLocale()")
    class DisplayNameByLocaleTests {

        @Test
        @DisplayName("Retorna mapa vacio por defecto (sin override)")
        void defaultRetornaMapaVacio() {
            StepComponent component = new DummyStepComponent("Test", BddPhase.GIVEN);
            assertThat(component.getDisplayNameByLocale()).isEmpty();
        }

        @Test
        @DisplayName("Retorna traducciones ES/EN/FR cuando se sobrescribe")
        void retornaTraduccionesParaTodosLosLocales() {
            StepComponent component = new I18nStepComponent();
            Map<String, String> names = component.getDisplayNameByLocale();

            assertThat(names).containsKeys("es", "en", "fr");
            assertThat(names.get("es")).isEqualTo("Peticion HTTP");
            assertThat(names.get("en")).isEqualTo("HTTP Request");
            assertThat(names.get("fr")).isEqualTo("Requete HTTP");
        }

        @Test
        @DisplayName("Mapa retornado es inmutable (Map.of)")
        void mapaEsInmutable() {
            StepComponent component = new I18nStepComponent();
            Map<String, String> names = component.getDisplayNameByLocale();
            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> names.put("de", "HTTP-Anfrage")
            );
        }

        @Test
        @DisplayName("Retorna exactamente 3 locales")
        void retornaTresLocales() {
            StepComponent component = new I18nStepComponent();
            assertThat(component.getDisplayNameByLocale()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Metadata i18n — getDescriptionByLocale()")
    class DescriptionByLocaleTests {

        @Test
        @DisplayName("Retorna mapa vacio por defecto (sin override)")
        void defaultRetornaMapaVacio() {
            StepComponent component = new DummyStepComponent("Test", BddPhase.THEN);
            assertThat(component.getDescriptionByLocale()).isEmpty();
        }

        @Test
        @DisplayName("Retorna traducciones ES/EN/FR cuando se sobrescribe")
        void retornaTraduccionesParaTodosLosLocales() {
            StepComponent component = new I18nStepComponent();
            Map<String, String> descs = component.getDescriptionByLocale();

            assertThat(descs).containsKeys("es", "en", "fr");
            assertThat(descs.get("es")).isEqualTo("Descripcion en espanol");
            assertThat(descs.get("en")).isEqualTo("Description in English");
            assertThat(descs.get("fr")).isEqualTo("Description en francais");
        }

        @Test
        @DisplayName("Las traducciones no son nulas ni vacias")
        void traduccionesNoSonNulasNiVacias() {
            StepComponent component = new I18nStepComponent();
            component.getDescriptionByLocale().values()
                    .forEach(v -> assertThat(v).isNotNull().isNotEmpty());
        }
    }
}
