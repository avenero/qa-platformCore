package com.qa.common.runtime;


import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.ParamInfo;
import com.qa.common.api.runtime.StepDefinitionInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests unitarios para {@link StepDefinitionInfo}.
 *
 * <p>Verifica invariantes del record: validación de campos requeridos,
 * normalización de {@code displayName} y {@code params}, helpers de conveniencia
 * e inmutabilidad de la lista de parámetros.
 *
 * @author Abel Venero
 * @since 2.2.0
 */
@DisplayName("StepDefinitionInfo")
class StepDefinitionInfoTest {

    // =========================================================================
    // Fábrica de instancias de apoyo
    // =========================================================================

    /** Crea un StepDefinitionInfo mínimo y válido para los tests. */
    private static StepDefinitionInfo minimal() {
        return full13(
                "api.auth.bearer",
                "agrego autenticación Bearer para RUT {string}",
                List.of(new ParamInfo(0, "rut", "String", "{string}")),
                BddPhase.GIVEN,
                "api",
                "api.authentication",
                "Agregar Bearer",
                false,
                null
        );
    }

    /**
     * Construye el {@link StepDefinitionInfo} canónico (13 args) a partir de los 9 campos
     * esenciales, rellenando los mapas i18n vacíos y los flags de override en {@code false}.
     * Reemplaza al constructor de conveniencia de 9 args eliminado en v2.4.0 (W1-T1-DS7).
     */
    private static StepDefinitionInfo full13(
            String stepDefId,
            String cucumberPattern,
            List<ParamInfo> params,
            BddPhase phase,
            String layer,
            String componentId,
            String displayName,
            boolean deprecated,
            String replacementStepDefId) {
        return new StepDefinitionInfo(
                stepDefId, cucumberPattern, params, phase, layer, componentId,
                displayName, deprecated, replacementStepDefId,
                Map.of(), Map.of(), false, false);
    }

    // =========================================================================
    // Constructor — campos requeridos
    // =========================================================================

    @Nested
    @DisplayName("Constructor — validaciones de null")
    class ConstructorNullTest {

        @Test
        @DisplayName("stepDefId null lanza NullPointerException")
        void testStepDefIdNull() {
            assertThatNullPointerException().isThrownBy(() ->
                    full13(null, "patron", List.of(),
                            BddPhase.GIVEN, "api", "api.auth", "display", false, null));
        }

        @Test
        @DisplayName("cucumberPattern null lanza NullPointerException")
        void testCucumberPatternNull() {
            assertThatNullPointerException().isThrownBy(() ->
                    full13("id", null, List.of(),
                            BddPhase.GIVEN, "api", "api.auth", "display", false, null));
        }

        @Test
        @DisplayName("phase null lanza NullPointerException")
        void testPhaseNull() {
            assertThatNullPointerException().isThrownBy(() ->
                    full13("id", "patron", List.of(),
                            null, "api", "api.auth", "display", false, null));
        }

        @Test
        @DisplayName("layer null lanza NullPointerException")
        void testLayerNull() {
            assertThatNullPointerException().isThrownBy(() ->
                    full13("id", "patron", List.of(),
                            BddPhase.GIVEN, null, "api.auth", "display", false, null));
        }

        @Test
        @DisplayName("componentId null lanza NullPointerException")
        void testComponentIdNull() {
            assertThatNullPointerException().isThrownBy(() ->
                    full13("id", "patron", List.of(),
                            BddPhase.GIVEN, "api", null, "display", false, null));
        }
    }

    // =========================================================================
    // Constructor — normalización de params
    // =========================================================================

    @Nested
    @DisplayName("Constructor — normalización de params")
    class ParamsNormalizationTest {

        @Test
        @DisplayName("params null se normaliza a lista vacía")
        void testParamsNullNormalizadoAVacio() {
            StepDefinitionInfo sdi = full13(
                    "id", "patron", null, BddPhase.GIVEN, "api", "api.auth", "d", false, null);
            assertThat(sdi.params()).isEmpty();
        }

        @Test
        @DisplayName("params vacía se acepta sin error")
        void testParamsVacioAceptado() {
            StepDefinitionInfo sdi = full13(
                    "id", "patron", List.of(), BddPhase.GIVEN, "api", "api.auth", "d", false, null);
            assertThat(sdi.params()).isEmpty();
        }

        @Test
        @DisplayName("params con elementos se copia en lista inmutable")
        void testParamsConElementos() {
            List<ParamInfo> original = new ArrayList<>();
            original.add(new ParamInfo(0, "rut", "String", "{string}"));

            StepDefinitionInfo sdi = full13(
                    "id", "patron", original, BddPhase.GIVEN, "api", "api.auth", "d", false, null);
            assertThat(sdi.params()).hasSize(1);
        }

        @Test
        @DisplayName("la lista de params retornada es inmutable")
        void testParamsInmutable() {
            StepDefinitionInfo sdi = minimal();
            List<ParamInfo> params = sdi.params();
            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> params.add(new ParamInfo(99, "x", "String", null))
            );
        }
    }

    // =========================================================================
    // Constructor — normalización de displayName
    // =========================================================================

    @Nested
    @DisplayName("Constructor — normalización de displayName")
    class DisplayNameNormalizationTest {

        @Test
        @DisplayName("displayName null se deriva del cucumberPattern")
        void testDisplayNameNullSeDerivaDePetron() {
            StepDefinitionInfo sdi = full13(
                    "id", "agrego autenticacion", List.of(),
                    BddPhase.GIVEN, "api", "api.auth", null, false, null);
            assertThat(sdi.displayName()).isEqualTo("agrego autenticacion");
        }

        @Test
        @DisplayName("displayName blank se deriva del cucumberPattern")
        void testDisplayNameBlankSeDeriva() {
            StepDefinitionInfo sdi = full13(
                    "id", "ejecuto GET a {string}", List.of(),
                    BddPhase.WHEN, "api", "api.execution", "   ", false, null);
            assertThat(sdi.displayName()).isEqualTo("ejecuto GET a {string}");
        }

        @Test
        @DisplayName("displayName con valor propio se respeta")
        void testDisplayNameConValorRespetado() {
            StepDefinitionInfo sdi = full13(
                    "id", "agrego autenticacion", List.of(),
                    BddPhase.GIVEN, "api", "api.auth", "Agregar Autenticación", false, null);
            assertThat(sdi.displayName()).isEqualTo("Agregar Autenticación");
        }
    }

    // =========================================================================
    // hasParams()
    // =========================================================================

    @Nested
    @DisplayName("hasParams()")
    class HasParamsTest {

        @Test
        @DisplayName("retorna false cuando no hay parámetros")
        void testSinParametros() {
            StepDefinitionInfo sdi = full13(
                    "id", "no agrego autenticacion", List.of(),
                    BddPhase.GIVEN, "api", "api.auth", "d", false, null);
            assertThat(sdi.hasParams()).isFalse();
        }

        @Test
        @DisplayName("retorna true cuando hay al menos un parámetro")
        void testConUnParametro() {
            assertThat(minimal().hasParams()).isTrue();
        }

        @Test
        @DisplayName("retorna true cuando params es null (se normaliza a vacío → false)")
        void testConParamsNull() {
            StepDefinitionInfo sdi = full13(
                    "id", "patron", null,
                    BddPhase.GIVEN, "api", "api.auth", "d", false, null);
            assertThat(sdi.hasParams()).isFalse();
        }
    }

    // =========================================================================
    // hasReplacement()
    // =========================================================================

    @Nested
    @DisplayName("hasReplacement()")
    class HasReplacementTest {

        @Test
        @DisplayName("retorna false cuando replacementStepDefId es null")
        void testReplacementNull() {
            assertThat(minimal().hasReplacement()).isFalse();
        }

        @Test
        @DisplayName("retorna false cuando replacementStepDefId es blank")
        void testReplacementBlank() {
            StepDefinitionInfo sdi = full13(
                    "id", "patron", List.of(),
                    BddPhase.GIVEN, "api", "api.auth", "d", true, "   ");
            assertThat(sdi.hasReplacement()).isFalse();
        }

        @Test
        @DisplayName("retorna true cuando replacementStepDefId tiene valor")
        void testReplacementConValor() {
            StepDefinitionInfo sdi = full13(
                    "id", "patron", List.of(),
                    BddPhase.GIVEN, "api", "api.auth", "d", true, "api.auth.nuevo");
            assertThat(sdi.hasReplacement()).isTrue();
            assertThat(sdi.replacementStepDefId()).isEqualTo("api.auth.nuevo");
        }
    }

    // =========================================================================
    // Campos accesibles correctamente
    // =========================================================================

    @Nested
    @DisplayName("Acceso a campos del record")
    class CamposTest {

        @Test
        @DisplayName("Todos los campos del record se leen correctamente")
        void testCamposTodos() {
            ParamInfo param = new ParamInfo(0, "rut", "String", "{string}");
            StepDefinitionInfo sdi = full13(
                    "api.auth.bearer.rut",
                    "agrego autenticación Bearer para RUT {string}",
                    List.of(param),
                    BddPhase.GIVEN,
                    "api",
                    "api.authentication",
                    "Agregar Bearer por RUT",
                    false,
                    null
            );

            assertThat(sdi.stepDefId()).isEqualTo("api.auth.bearer.rut");
            assertThat(sdi.cucumberPattern()).isEqualTo("agrego autenticación Bearer para RUT {string}");
            assertThat(sdi.params()).containsExactly(param);
            assertThat(sdi.phase()).isEqualTo(BddPhase.GIVEN);
            assertThat(sdi.layer()).isEqualTo("api");
            assertThat(sdi.componentId()).isEqualTo("api.authentication");
            assertThat(sdi.displayName()).isEqualTo("Agregar Bearer por RUT");
            assertThat(sdi.deprecated()).isFalse();
            assertThat(sdi.replacementStepDefId()).isNull();
        }

        @Test
        @DisplayName("Step deprecado tiene todos sus flags correctos")
        void testStepDeprecado() {
            StepDefinitionInfo sdi = full13(
                    "api.auth.old",
                    "agrego Bearer token",
                    List.of(),
                    BddPhase.GIVEN,
                    "api",
                    "api.authentication",
                    "Agregar Bearer (deprecated)",
                    true,
                    "api.auth.bearer.rut"
            );

            assertThat(sdi.deprecated()).isTrue();
            assertThat(sdi.hasReplacement()).isTrue();
            assertThat(sdi.replacementStepDefId()).isEqualTo("api.auth.bearer.rut");
        }
    }

    // =========================================================================
    // Igualdad de records
    // =========================================================================

    @Nested
    @DisplayName("Igualdad de records")
    class IgualdadTest {

        @Test
        @DisplayName("Dos StepDefinitionInfo con los mismos campos son iguales")
        void testIgualdad() {
            List<ParamInfo> p = List.of(new ParamInfo(0, "rut", "String", "{string}"));

            StepDefinitionInfo a = full13(
                    "api.auth.bearer", "patron", p, BddPhase.GIVEN, "api", "api.auth", "d", false, null);
            StepDefinitionInfo b = full13(
                    "api.auth.bearer", "patron", p, BddPhase.GIVEN, "api", "api.auth", "d", false, null);

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("Dos StepDefinitionInfo con stepDefId diferente no son iguales")
        void testDesigualdad() {
            StepDefinitionInfo a = full13(
                    "api.auth.bearer", "patron", List.of(), BddPhase.GIVEN, "api", "api.auth", "d", false, null);
            StepDefinitionInfo b = full13(
                    "api.auth.basic",  "patron", List.of(), BddPhase.GIVEN, "api", "api.auth", "d", false, null);

            assertThat(a).isNotEqualTo(b);
        }
    }
}
