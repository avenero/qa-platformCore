package com.qa.common.runtime;


import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.ParamInfo;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.StepDefinitionInfo;
import com.qa.common.internal.runtime.StepDiscoveryService;
import com.qa.common.internal.runtime.StepMethodScanner;
import com.qa.common.api.runtime.annotation.StepDef;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests unitarios para {@link StepMethodScanner}.
 *
 * <p>Usa clases de steps stub con anotaciones Cucumber reales para verificar
 * el comportamiento del escáner de reflexión sin dependencias externas de mocking.
 * Cada clase stub está declarada como clase estática interna de esta clase de test.
 *
 * @author Abel Venero
 * @since 2.2.0
 */
@DisplayName("StepMethodScanner")
class StepMethodScannerTest {

    // =========================================================================
    // Clases de steps stub — simulan clases de steps reales de un plugin
    // =========================================================================

    /**
     * Clase de steps básica: cubre @Given, @When, @Then con y sin parámetros.
     */
    static class StubStepsBasicos {
        @Given("configuro el endpoint {string}")
        public void configurarEndpoint(String endpoint) { }

        @When("ejecuto la peticion con metodo {string}")
        public void ejecutarPeticion(String method) { }

        @Then("el codigo de respuesta es {int}")
        public void verificarCodigo(int code) { }

        @Given("no agrego autenticacion")
        public void sinAutenticacion() { }  // sin parámetros

        /** Método sin anotación Cucumber — el scanner debe ignorarlo. */
        public void metodoSinAnotacion(String ignorame) { }

        /** Método privado — getMethods() no lo incluye, el scanner lo ignora. */
        @SuppressWarnings("unused")
        private void metodoPrivado() { }
    }

    /**
     * Clase de steps con múltiples tokens en el patrón.
     */
    static class StubStepsMultiParam {
        @When("reintento {string} al endpoint {string} hasta que el codigo sea {int} con maximo {int} intentos")
        public void reintentar(String method, String endpoint, int expectedCode, int maxAttempts) { }
    }

    /**
     * Clase de steps con parámetro especial (DataTable via Map) — sin tokens en el patrón.
     */
    static class StubStepsDataTable {
        @Given("configuro JWT con las siguientes claims")
        public void configuroJwt(Map<String, String> claims) { }
    }

    /**
     * Clase de steps con anotaciones {@link StepDef} en los métodos.
     */
    static class StubStepsConStepDef {
        @StepDef("api.auth.bearer.rut")
        @Given("agrego autenticacion Bearer para RUT {string}")
        public void agregarBearer(String rut) { }

        @StepDef(value = "api.auth.old", deprecated = true, replacedBy = "api.auth.bearer.rut",
                 displayName = "Autenticacion Antigua (deprecated)")
        @Given("agrego token antiguo")
        public void tokenAntiguo() { }

        @Given("agrego step sin stepdef")
        public void stepSinStepDef() { }
    }

    /** Clase vacía sin ningún método de step — resultado debe ser lista vacía. */
    static class StubStepsVacios {
        public void metodoNormal() { }
    }

    // =========================================================================
    // Fábrica de stubs para ComponentInfo
    // =========================================================================

    private static StepDiscoveryService.ComponentInfo makeComponentInfo(
            String pluginName, String componentId, Class<?> stepClass) {
        StepComponent component = new StepComponent() {
            @Override public String getName()                  { return componentId; }
            @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
            @Override public Class<?> getStepDefinitionClass() { return stepClass; }
            @Override public String getId()                    { return componentId; }
        };
        return new StepDiscoveryService.ComponentInfo(pluginName, component);
    }

    private static StepDiscoveryService.ComponentInfo makeComponentInfoNull(
            String pluginName, String componentId) {
        StepComponent component = new StepComponent() {
            @Override public String getName()                  { return componentId; }
            @Override public BddPhase getPhase()               { return BddPhase.GIVEN; }
            @Override public Class<?> getStepDefinitionClass() { return null; }
            @Override public String getId()                    { return componentId; }
        };
        return new StepDiscoveryService.ComponentInfo(pluginName, component);
    }

    // =========================================================================
    // Constructor / método scan — precondiciones
    // =========================================================================

    @Nested
    @DisplayName("Precondiciones")
    class PrecondicionesTest {

        @Test
        @DisplayName("scan(null) lanza NullPointerException")
        void testScanNullLanzaException() {
            StepMethodScanner scanner = new StepMethodScanner();
            assertThatNullPointerException().isThrownBy(() -> scanner.scan(null));
        }

        @Test
        @DisplayName("scanAll(null) lanza NullPointerException")
        void testScanAllNullLanzaException() {
            StepMethodScanner scanner = new StepMethodScanner();
            assertThatNullPointerException().isThrownBy(() -> scanner.scanAll(null));
        }
    }

    // =========================================================================
    // Componente con stepDefinitionClass null
    // =========================================================================

    @Nested
    @DisplayName("Componente sin stepDefinitionClass")
    class SinStepClassTest {

        @Test
        @DisplayName("Componente con stepClass null retorna lista vacía")
        void testStepClassNullRetornaVacio() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info = makeComponentInfoNull("api", "api.auth");
            List<StepDefinitionInfo> result = scanner.scan(info);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Componente con clase sin anotaciones Cucumber retorna lista vacía")
        void testClaseSinAnotacionesRetornaVacio() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.auth", StubStepsVacios.class);
            List<StepDefinitionInfo> result = scanner.scan(info);
            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // Detección de pasos — cantidad y fases
    // =========================================================================

    @Nested
    @DisplayName("Detección de steps por fase")
    class DeteccionPorFaseTest {

        @Test
        @DisplayName("Detecta todos los métodos @Given, @When y @Then de la clase")
        void testDetectaTodosLosSteps() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.basicos", StubStepsBasicos.class);

            List<StepDefinitionInfo> result = scanner.scan(info);

            // 4 métodos con anotaciones Cucumber, 2 sin ellas
            assertThat(result).hasSize(4);
        }

        @Test
        @DisplayName("@Given genera StepDefinitionInfo con fase GIVEN")
        void testGivenGeneraFaseGiven() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.basicos", StubStepsBasicos.class);

            List<StepDefinitionInfo> given = scanner.scan(info).stream()
                    .filter(s -> s.phase() == BddPhase.GIVEN)
                    .toList();

            assertThat(given).hasSize(2); // "configuro el endpoint" + "no agrego autenticacion"
        }

        @Test
        @DisplayName("@When genera StepDefinitionInfo con fase WHEN")
        void testWhenGeneraFaseWhen() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.basicos", StubStepsBasicos.class);

            List<StepDefinitionInfo> when = scanner.scan(info).stream()
                    .filter(s -> s.phase() == BddPhase.WHEN)
                    .toList();

            assertThat(when).hasSize(1);
            assertThat(when.get(0).cucumberPattern()).isEqualTo("ejecuto la peticion con metodo {string}");
        }

        @Test
        @DisplayName("@Then genera StepDefinitionInfo con fase THEN")
        void testThenGeneraFaseThen() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.basicos", StubStepsBasicos.class);

            List<StepDefinitionInfo> then = scanner.scan(info).stream()
                    .filter(s -> s.phase() == BddPhase.THEN)
                    .toList();

            assertThat(then).hasSize(1);
            assertThat(then.get(0).cucumberPattern()).isEqualTo("el codigo de respuesta es {int}");
        }
    }

    // =========================================================================
    // Contexto del step (layer, componentId)
    // =========================================================================

    @Nested
    @DisplayName("Contexto del step: layer y componentId")
    class ContextoTest {

        @Test
        @DisplayName("layer se propaga del plugin al StepDefinitionInfo")
        void testLayerPropagado() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.basicos", StubStepsBasicos.class);

            scanner.scan(info).forEach(sdi ->
                    assertThat(sdi.layer()).isEqualTo("api"));
        }

        @Test
        @DisplayName("componentId se propaga al StepDefinitionInfo")
        void testComponentIdPropagado() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("web", "web.navigation", StubStepsBasicos.class);

            scanner.scan(info).forEach(sdi ->
                    assertThat(sdi.componentId()).isEqualTo("web.navigation"));
        }
    }

    // =========================================================================
    // Extracción de parámetros
    // =========================================================================

    @Nested
    @DisplayName("Extracción de parámetros")
    class ParametrosTest {

        @Test
        @DisplayName("Step sin parámetros genera lista de params vacía")
        void testSinParametros() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.basicos", StubStepsBasicos.class);

            StepDefinitionInfo sinParams = scanner.scan(info).stream()
                    .filter(s -> s.cucumberPattern().equals("no agrego autenticacion"))
                    .findFirst().orElseThrow();

            assertThat(sinParams.hasParams()).isFalse();
            assertThat(sinParams.params()).isEmpty();
        }

        @Test
        @DisplayName("Step con {string} genera ParamInfo con javaType String y token {string}")
        void testParamString() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.basicos", StubStepsBasicos.class);

            StepDefinitionInfo sdi = scanner.scan(info).stream()
                    .filter(s -> s.cucumberPattern().equals("configuro el endpoint {string}"))
                    .findFirst().orElseThrow();

            assertThat(sdi.params()).hasSize(1);
            ParamInfo p = sdi.params().get(0);
            assertThat(p.position()).isEqualTo(0);
            assertThat(p.javaType()).isEqualTo("String");
            assertThat(p.cucumberToken()).isEqualTo("{string}");
            assertThat(p.hasCucumberToken()).isTrue();
        }

        @Test
        @DisplayName("Step con {int} genera ParamInfo con javaType int y token {int}")
        void testParamInt() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.basicos", StubStepsBasicos.class);

            StepDefinitionInfo sdi = scanner.scan(info).stream()
                    .filter(s -> s.cucumberPattern().equals("el codigo de respuesta es {int}"))
                    .findFirst().orElseThrow();

            assertThat(sdi.params()).hasSize(1);
            ParamInfo p = sdi.params().get(0);
            assertThat(p.javaType()).isEqualTo("int");
            assertThat(p.cucumberToken()).isEqualTo("{int}");
        }

        @Test
        @DisplayName("Patrón multi-token genera ParamInfo por cada token en orden")
        void testMultipleTokens() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.multi", StubStepsMultiParam.class);

            List<StepDefinitionInfo> result = scanner.scan(info);
            assertThat(result).hasSize(1);

            StepDefinitionInfo sdi = result.get(0);
            assertThat(sdi.params()).hasSize(4);

            // Primeros dos son {string}
            assertThat(sdi.params().get(0).cucumberToken()).isEqualTo("{string}");
            assertThat(sdi.params().get(0).javaType()).isEqualTo("String");
            assertThat(sdi.params().get(1).cucumberToken()).isEqualTo("{string}");
            assertThat(sdi.params().get(1).javaType()).isEqualTo("String");

            // Últimos dos son {int}
            assertThat(sdi.params().get(2).cucumberToken()).isEqualTo("{int}");
            assertThat(sdi.params().get(2).javaType()).isEqualTo("int");
            assertThat(sdi.params().get(3).cucumberToken()).isEqualTo("{int}");
            assertThat(sdi.params().get(3).javaType()).isEqualTo("int");
        }

        @Test
        @DisplayName("Parámetro DataTable (Map sin token) recibe cucumberToken null")
        void testDataTableSinToken() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.datatable", StubStepsDataTable.class);

            List<StepDefinitionInfo> result = scanner.scan(info);
            assertThat(result).hasSize(1);

            StepDefinitionInfo sdi = result.get(0);
            assertThat(sdi.hasParams()).isTrue();
            assertThat(sdi.params()).hasSize(1);

            ParamInfo p = sdi.params().get(0);
            assertThat(p.javaType()).isEqualTo("Map");
            assertThat(p.cucumberToken()).isNull();
            assertThat(p.hasCucumberToken()).isFalse();
        }
    }

    // =========================================================================
    // Resolución de stepDefId
    // =========================================================================

    @Nested
    @DisplayName("Resolución de stepDefId")
    class StepDefIdTest {

        @Test
        @DisplayName("Sin @StepDef el id se deriva como componentId#methodName")
        void testIdDerivadoSinAnotacion() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.basicos", StubStepsBasicos.class);

            StepDefinitionInfo sdi = scanner.scan(info).stream()
                    .filter(s -> s.cucumberPattern().equals("no agrego autenticacion"))
                    .findFirst().orElseThrow();

            assertThat(sdi.stepDefId()).startsWith("api.basicos#");
        }

        @Test
        @DisplayName("@StepDef.value() se usa como stepDefId explícito")
        void testIdExplicitoConStepDef() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.auth", StubStepsConStepDef.class);

            StepDefinitionInfo sdi = scanner.scan(info).stream()
                    .filter(s -> s.cucumberPattern().equals("agrego autenticacion Bearer para RUT {string}"))
                    .findFirst().orElseThrow();

            assertThat(sdi.stepDefId()).isEqualTo("api.auth.bearer.rut");
        }

        @Test
        @DisplayName("Step sin @StepDef dentro de clase con @StepDef también se descubre")
        void testStepSinStepDefEnClaseConStepDef() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.auth", StubStepsConStepDef.class);

            StepDefinitionInfo sdi = scanner.scan(info).stream()
                    .filter(s -> s.cucumberPattern().equals("agrego step sin stepdef"))
                    .findFirst().orElseThrow();

            // ID derivado porque no tiene @StepDef
            assertThat(sdi.stepDefId()).startsWith("api.auth#");
        }
    }

    // =========================================================================
    // Deprecación con @StepDef
    // =========================================================================

    @Nested
    @DisplayName("Deprecación via @StepDef")
    class DeprecacionTest {

        @Test
        @DisplayName("Step con @StepDef(deprecated=true) tiene deprecated=true en el DTO")
        void testDeprecadoTrue() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.auth", StubStepsConStepDef.class);

            StepDefinitionInfo sdi = scanner.scan(info).stream()
                    .filter(s -> s.stepDefId().equals("api.auth.old"))
                    .findFirst().orElseThrow();

            assertThat(sdi.deprecated()).isTrue();
        }

        @Test
        @DisplayName("@StepDef.replacedBy() se propaga al replacementStepDefId")
        void testReplacedByPropagado() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.auth", StubStepsConStepDef.class);

            StepDefinitionInfo sdi = scanner.scan(info).stream()
                    .filter(s -> s.stepDefId().equals("api.auth.old"))
                    .findFirst().orElseThrow();

            assertThat(sdi.hasReplacement()).isTrue();
            assertThat(sdi.replacementStepDefId()).isEqualTo("api.auth.bearer.rut");
        }

        @Test
        @DisplayName("@StepDef.displayName() se usa como displayName cuando está presente")
        void testDisplayNameConStepDef() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.auth", StubStepsConStepDef.class);

            StepDefinitionInfo sdi = scanner.scan(info).stream()
                    .filter(s -> s.stepDefId().equals("api.auth.old"))
                    .findFirst().orElseThrow();

            assertThat(sdi.displayName()).isEqualTo("Autenticacion Antigua (deprecated)");
        }

        @Test
        @DisplayName("Step sin @StepDef tiene deprecated=false por defecto")
        void testNoDeprecadoPorDefecto() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.basicos", StubStepsBasicos.class);

            scanner.scan(info).forEach(sdi ->
                    assertThat(sdi.deprecated()).isFalse());
        }
    }

    // =========================================================================
    // scanAll()
    // =========================================================================

    @Nested
    @DisplayName("scanAll()")
    class ScanAllTest {

        @Test
        @DisplayName("Lista vacía retorna lista vacía")
        void testListaVaciaRetornaVacio() {
            StepMethodScanner scanner = new StepMethodScanner();
            assertThat(scanner.scanAll(List.of())).isEmpty();
        }

        @Test
        @DisplayName("scanAll agrega los steps de múltiples componentes")
        void testScanAllAgregaMultiplesComponentes() {
            StepMethodScanner scanner = new StepMethodScanner();

            StepDiscoveryService.ComponentInfo comp1 =
                    makeComponentInfo("api", "api.basicos", StubStepsBasicos.class);
            StepDiscoveryService.ComponentInfo comp2 =
                    makeComponentInfo("api", "api.multi", StubStepsMultiParam.class);

            List<StepDefinitionInfo> all = scanner.scanAll(List.of(comp1, comp2));

            // comp1 → 4 steps, comp2 → 1 step
            assertThat(all).hasSize(5);
        }

        @Test
        @DisplayName("scanAll incluye todos los layers distintos")
        void testScanAllDistintasCapas() {
            StepMethodScanner scanner = new StepMethodScanner();

            StepDiscoveryService.ComponentInfo apiComp =
                    makeComponentInfo("api", "api.basicos", StubStepsBasicos.class);
            StepDiscoveryService.ComponentInfo webComp =
                    makeComponentInfo("web", "web.nav", StubStepsVacios.class);

            List<StepDefinitionInfo> all = scanner.scanAll(List.of(apiComp, webComp));

            // Solo api.basicos tiene steps
            assertThat(all).hasSize(4);
            all.forEach(s -> assertThat(s.layer()).isEqualTo("api"));
        }

        @Test
        @DisplayName("La lista retornada por scanAll es inmutable")
        void testListaInmutable() {
            StepMethodScanner scanner = new StepMethodScanner();
            StepDiscoveryService.ComponentInfo info =
                    makeComponentInfo("api", "api.basicos", StubStepsBasicos.class);

            List<StepDefinitionInfo> result = scanner.scanAll(List.of(info));
            assertThrows(UnsupportedOperationException.class, () -> result.add(null));
        }
    }
}
