package com.qa.common.driver;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para {@link CapabilityReport} y {@link CapabilityDescriptor}.
 *
 * @author Abel Venero
 * @since 3.0.0
 */
@DisplayName("CapabilityReport")
class CapabilityReportTest {

    // =========================================================================
    // CapabilityDescriptor
    // =========================================================================

    @Nested
    @DisplayName("CapabilityDescriptor")
    class CapabilityDescriptorTests {

        @Test
        @DisplayName("constructor canónico asigna campos correctamente")
        void constructorAsignaCampos() {
            CapabilityDescriptor d = new CapabilityDescriptor("rest", "REST", "HTTP endpoints");
            assertThat(d.id()).isEqualTo("rest");
            assertThat(d.name()).isEqualTo("REST");
            assertThat(d.description()).isEqualTo("HTTP endpoints");
        }

        @Test
        @DisplayName("id null lanza IllegalArgumentException")
        void idNullLanzaExcepcion() {
            assertThatThrownBy(() -> new CapabilityDescriptor(null, "name", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
        }

        @Test
        @DisplayName("id en blanco lanza IllegalArgumentException")
        void idBlancLanzaExcepcion() {
            assertThatThrownBy(() -> new CapabilityDescriptor("  ", "name", "desc"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("name null lanza IllegalArgumentException")
        void nameNullLanzaExcepcion() {
            assertThatThrownBy(() -> new CapabilityDescriptor("id", null, "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        }

        @Test
        @DisplayName("dos descriptores con mismos campos son iguales (record equality)")
        void igualdadPorValor() {
            CapabilityDescriptor d1 = new CapabilityDescriptor("rest", "REST", "desc");
            CapabilityDescriptor d2 = new CapabilityDescriptor("rest", "REST", "desc");
            assertThat(d1).isEqualTo(d2);
            assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
        }

        @Test
        @DisplayName("description puede ser null (campo opcional)")
        void descriptionPuedeSerNull() {
            assertThatCode(() -> new CapabilityDescriptor("id", "name", null))
                .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // CapabilityReport.available()
    // =========================================================================

    @Nested
    @DisplayName("factory available()")
    class AvailableTests {

        @Test
        @DisplayName("available() con lista llena crea informe correcto")
        void availableConListaLlena() {
            List<CapabilityDescriptor> opts = List.of(
                new CapabilityDescriptor("chromium", "Chromium", "Chromium browser"),
                new CapabilityDescriptor("firefox",  "Firefox",  "Mozilla Firefox")
            );
            CapabilityReport report = CapabilityReport.available("WEB", opts);

            assertThat(report.platformId()).isEqualTo("WEB");
            assertThat(report.available()).isTrue();
            assertThat(report.unavailableReason()).isNull();
            assertThat(report.options()).hasSize(2);
        }

        @Test
        @DisplayName("available() con lista vacia es valido")
        void availableConListaVacia() {
            CapabilityReport report = CapabilityReport.available("HTTP", List.of());
            assertThat(report.available()).isTrue();
            assertThat(report.options()).isEmpty();
        }

        @Test
        @DisplayName("available() con opts null trata como lista vacia")
        void availableConNullTrataComoVacia() {
            CapabilityReport report = CapabilityReport.available("HTTP", null);
            assertThat(report.available()).isTrue();
            assertThat(report.options()).isEmpty();
        }

        @Test
        @DisplayName("available() — la lista de options es inmutable")
        void availableOpcionesInmutables() {
            CapabilityReport report = CapabilityReport.available("WEB", List.of(
                new CapabilityDescriptor("chromium", "Chromium", null)
            ));
            assertThatThrownBy(() -> report.options().add(
                new CapabilityDescriptor("hack", "Hack", null)))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("available() — mutación de la lista fuente no afecta al record")
        void availableDefensiveCopy() {
            List<CapabilityDescriptor> mutable = new ArrayList<>();
            mutable.add(new CapabilityDescriptor("c1", "C1", null));
            CapabilityReport report = CapabilityReport.available("WEB", mutable);

            mutable.add(new CapabilityDescriptor("c2", "C2", null)); // mutamos fuente

            assertThat(report.options()).hasSize(1); // record no afectado
        }

        @Test
        @DisplayName("platformId null lanza NullPointerException")
        void availablePlatformIdNullLanzaNPE() {
            assertThatThrownBy(() -> CapabilityReport.available(null, List.of()))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("platformId vacio lanza IllegalArgumentException")
        void availablePlatformIdVacioLanzaException() {
            assertThatThrownBy(() -> CapabilityReport.available("", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // CapabilityReport.unavailable()
    // =========================================================================

    @Nested
    @DisplayName("factory unavailable()")
    class UnavailableTests {

        @Test
        @DisplayName("unavailable() crea informe correcto")
        void unavailableCreaInforme() {
            CapabilityReport report = CapabilityReport.unavailable("MOBILE",
                "Appium server not reachable at localhost:4723");

            assertThat(report.platformId()).isEqualTo("MOBILE");
            assertThat(report.available()).isFalse();
            assertThat(report.unavailableReason()).contains("Appium");
            assertThat(report.options()).isEmpty();
        }

        @Test
        @DisplayName("unavailable() con reason null lanza NullPointerException")
        void unavailableReasonNullLanzaNPE() {
            assertThatThrownBy(() -> CapabilityReport.unavailable("MOBILE", null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("available=false sin reason lanza IllegalArgumentException")
        void constructorDirectoFalsesinReasonFalla() {
            assertThatThrownBy(() -> new CapabilityReport("MOBILE", false, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unavailableReason");
        }

        @Test
        @DisplayName("available=true con reason no-null lanza IllegalArgumentException")
        void constructorDirectoTrueConReasonFalla() {
            assertThatThrownBy(() ->
                new CapabilityReport("WEB", true, "esto no debería estar aquí", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unavailableReason debe ser null");
        }
    }

    // =========================================================================
    // Igualdad de records (Java record canonical equality)
    // =========================================================================

    @Nested
    @DisplayName("Igualdad de records")
    class IgualdadTests {

        @Test
        @DisplayName("dos informes con mismos campos son iguales")
        void dosInformesIguales() {
            CapabilityReport r1 = CapabilityReport.available("WEB", List.of());
            CapabilityReport r2 = CapabilityReport.available("WEB", List.of());
            assertThat(r1).isEqualTo(r2);
            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        }

        @Test
        @DisplayName("informes con distinto platformId no son iguales")
        void plataformasDistintasNoSonIguales() {
            CapabilityReport r1 = CapabilityReport.available("WEB", List.of());
            CapabilityReport r2 = CapabilityReport.available("HTTP", List.of());
            assertThat(r1).isNotEqualTo(r2);
        }
    }
}
