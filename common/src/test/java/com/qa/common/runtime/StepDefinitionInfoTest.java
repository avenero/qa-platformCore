package com.qa.common.runtime;


import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.ParamInfo;
import com.qa.common.api.runtime.StepDefinitionInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
        return new StepDefinitionInfo(
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
                    new StepDefinitionInfo(null, "patron", List.of(),
                            BddPhase.GIVEN, "api", "api.auth", "display", false, null));
        }

        @Test
        @DisplayName("cucumberPattern null lanza NullPointerException")
        void testCucumberPatternNull() {
            assertThatNullPointerException().isThrownBy(() ->
                    new StepDefinitionInfo("id", null, List.of(),
                            BddPhase.GIVEN, "api", "api.auth", "display", false, null));
        }

        @Test
        @DisplayName("phase null lanza NullPointerException")
        void testPhaseNull() {
            assertThatNullPointerException().isThrownBy(() ->
                    new StepDefinitionInfo("id", "patron", List.of(),
                            null, "api", "api.auth", "display", false, null));
        }

        @Test
        @DisplayName("layer null lanza NullPointerException")
        void testLayerNull() {
            assertThatNullPointerException().isThrownBy(() ->
                    new StepDefinitionInfo("id", "patron", List.of(),
                            BddPhase.GIVEN, null, "api.auth", "display", false, null));
        }

        @Test
        @DisplayName("componentId null lanza NullPointerException")
        void testComponentIdNull() {
            assertThatNullPointerException().isThrownBy(() ->
                    new StepDefinitionInfo("id", "patron", List.of(),
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
            StepDefinitionInfo sdi = new StepDefinitionInfo(
                    "id", "patron", null, BddPhase.GIVEN, "api", "api.auth", "d", false, null);
            assertThat(sdi.params()).isEmpty();
        }

        @Test
        @DisplayName("params vacía se acepta sin error")
        void testParamsVacioAceptado() {
            StepDefinitionInfo sdi = new StepDefinitionInfo(
                    "id", "patron", List.of(), BddPhase.GIVEN, "api", "api.auth", "d", false, null);
            assertThat(sdi.params()).isEmpty();
        }

        @Test
        @DisplayName("params con elementos se copia en lista inmutable")
        void testParamsConElementos() {
            List<ParamInfo> original = new ArrayList<>();
            original.add(new ParamInfo(0, "rut", "String", "{string}"));

            StepDefinitionInfo sdi = new StepDefinitionInfo(
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
            StepDefinitionInfo sdi = new StepDefinitionInfo(
                    "id", "agrego autenticacion", List.of(),
                    BddPhase.GIVEN, "api", "api.auth", null, false, null);
            assertThat(sdi.displayName()).isEqualTo("agrego autenticacion");
        }

        @Test
        @DisplayName("displayName blank se deriva del cucumberPattern")
        void testDisplayNameBlankSeDeriva() {
            StepDefinitionInfo sdi = new StepDefinitionInfo(
                    "id", "ejecuto GET a {string}", List.of(),
                    BddPhase.WHEN, "api", "api.execution", "   ", false, null);
            assertThat(sdi.displayName()).isEqualTo("ejecuto GET a {string}");
        }

        @Test
        @DisplayName("displayName con valor propio se respeta")
        void testDisplayNameConValorRespetado() {
            StepDefinitionInfo sdi = new StepDefinitionInfo(
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
            StepDefinitionInfo sdi = new StepDefinitionInfo(
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
            StepDefinitionInfo sdi = new StepDefinitionInfo(
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
            StepDefinitionInfo sdi = new StepDefinitionInfo(
                    "id", "patron", List.of(),
                    BddPhase.GIVEN, "api", "api.auth", "d", true, "   ");
            assertThat(sdi.hasReplacement()).isFalse();
        }

        @Test
        @DisplayName("retorna true cuando replacementStepDefId tiene valor")
        void testReplacementConValor() {
            StepDefinitionInfo sdi = new StepDefinitionInfo(
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
            StepDefinitionInfo sdi = new StepDefinitionInfo(
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
            StepDefinitionInfo sdi = new StepDefinitionInfo(
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

            StepDefinitionInfo a = new StepDefinitionInfo(
                    "api.auth.bearer", "patron", p, BddPhase.GIVEN, "api", "api.auth", "d", false, null);
            StepDefinitionInfo b = new StepDefinitionInfo(
                    "api.auth.bearer", "patron", p, BddPhase.GIVEN, "api", "api.auth", "d", false, null);

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("Dos StepDefinitionInfo con stepDefId diferente no son iguales")
        void testDesigualdad() {
            StepDefinitionInfo a = new StepDefinitionInfo(
                    "api.auth.bearer", "patron", List.of(), BddPhase.GIVEN, "api", "api.auth", "d", false, null);
            StepDefinitionInfo b = new StepDefinitionInfo(
                    "api.auth.basic",  "patron", List.of(), BddPhase.GIVEN, "api", "api.auth", "d", false, null);

            assertThat(a).isNotEqualTo(b);
        }
    }
}
