package com.qa.common.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests unitarios para {@link StepInfo}.
 *
 * <p>Verifica contrato de construccion, inmutabilidad de mapas i18n
 * y los helpers de resolución por locale.
 *
 * @author Abel Venero
 * @since 2.1.0
 */
@DisplayName("StepInfo")
class StepInfoTest {

    private static final Map<String, String> DISPLAY_NAMES = Map.of(
            "es", "Ejecucion HTTP",
            "en", "HTTP Execution",
            "fr", "Execution HTTP"
    );

    private static final Map<String, String> DESCRIPTIONS = Map.of(
            "es", "Envio de peticiones REST",
            "en", "Send REST requests",
            "fr", "Envoi de requetes REST"
    );

    private StepInfo buildFull() {
        return new StepInfo(
                "api.execution",
                "HTTP Execution",
                "api",
                "api.execution",
                "WHEN",
                "Ejecucion",
                "send",
                70,
                "Ejecucion HTTP",
                "Envio de peticiones REST",
                DISPLAY_NAMES,
                DESCRIPTIONS
        );
    }

    // =========================================================================
    // Construccion y acceso a campos
    // =========================================================================

    @Nested
    @DisplayName("Construccion")
    class ConstruccionTest {

        @Test
        @DisplayName("Todos los campos escalares se almacenan correctamente")
        void camposEscarales() {
            StepInfo info = buildFull();

            assertThat(info.id()).isEqualTo("api.execution");
            assertThat(info.pattern()).isEqualTo("HTTP Execution");
            assertThat(info.layer()).isEqualTo("api");
            assertThat(info.componentId()).isEqualTo("api.execution");
            assertThat(info.phase()).isEqualTo("WHEN");
            assertThat(info.category()).isEqualTo("Ejecucion");
            assertThat(info.icon()).isEqualTo("send");
            assertThat(info.displayOrder()).isEqualTo(70);
            assertThat(info.displayName()).isEqualTo("Ejecucion HTTP");
            assertThat(info.description()).isEqualTo("Envio de peticiones REST");
        }

        @Test
        @DisplayName("id null lanza NullPointerException")
        void idNullLanzaException() {
            assertThatNullPointerException().isThrownBy(() -> new StepInfo(
                    null, "p", "api", "api.x", "GIVEN",
                    "cat", "icon", 10, "dn", "desc",
                    Map.of(), Map.of()
            )).withMessageContaining("id");
        }

        @Test
        @DisplayName("layer null lanza NullPointerException")
        void layerNullLanzaException() {
            assertThatNullPointerException().isThrownBy(() -> new StepInfo(
                    "api.x", "p", null, "api.x", "GIVEN",
                    "cat", "icon", 10, "dn", "desc",
                    Map.of(), Map.of()
            )).withMessageContaining("layer");
        }

        @Test
        @DisplayName("componentId null lanza NullPointerException")
        void componentIdNullLanzaException() {
            assertThatNullPointerException().isThrownBy(() -> new StepInfo(
                    "api.x", "p", "api", null, "GIVEN",
                    "cat", "icon", 10, "dn", "desc",
                    Map.of(), Map.of()
            )).withMessageContaining("componentId");
        }

        @Test
        @DisplayName("phase null lanza NullPointerException")
        void phaseNullLanzaException() {
            assertThatNullPointerException().isThrownBy(() -> new StepInfo(
                    "api.x", "p", "api", "api.x", null,
                    "cat", "icon", 10, "dn", "desc",
                    Map.of(), Map.of()
            )).withMessageContaining("phase");
        }

        @Test
        @DisplayName("displayNameByLocale null lanza NullPointerException")
        void displayNameByLocaleNullLanzaException() {
            assertThatNullPointerException().isThrownBy(() -> new StepInfo(
                    "api.x", "p", "api", "api.x", "GIVEN",
                    "cat", "icon", 10, "dn", "desc",
                    null, Map.of()
            )).withMessageContaining("displayNameByLocale");
        }

        @Test
        @DisplayName("descriptionByLocale null lanza NullPointerException")
        void descriptionByLocaleNullLanzaException() {
            assertThatNullPointerException().isThrownBy(() -> new StepInfo(
                    "api.x", "p", "api", "api.x", "GIVEN",
                    "cat", "icon", 10, "dn", "desc",
                    Map.of(), null
            )).withMessageContaining("descriptionByLocale");
        }
    }

    // =========================================================================
    // Inmutabilidad de mapas
    // =========================================================================

    @Nested
    @DisplayName("Inmutabilidad de mapas i18n")
    class InmutabilidadTest {

        @Test
        @DisplayName("displayNameByLocale retornado es inmutable")
        void displayNameByLocaleEsInmutable() {
            StepInfo info = buildFull();
            assertThrows(UnsupportedOperationException.class,
                    () -> info.displayNameByLocale().put("de", "HTTP-Ausfuehrung"));
        }

        @Test
        @DisplayName("descriptionByLocale retornado es inmutable")
        void descriptionByLocaleEsInmutable() {
            StepInfo info = buildFull();
            assertThrows(UnsupportedOperationException.class,
                    () -> info.descriptionByLocale().put("de", "REST-Anfragen senden"));
        }

        @Test
        @DisplayName("Modificar el mapa fuente no altera el StepInfo")
        void modificarMapaFuenteNoAfecta() {
            Map<String, String> mutableNames = new HashMap<>();
            mutableNames.put("es", "Nombre Original");

            StepInfo info = new StepInfo(
                    "x", "p", "api", "x", "GIVEN",
                    "cat", "icon", 1, "dn", "desc",
                    mutableNames, Map.of()
            );

            mutableNames.put("es", "Nombre Modificado");

            assertThat(info.displayNameByLocale().get("es")).isEqualTo("Nombre Original");
        }
    }

    // =========================================================================
    // getDisplayNameForLocale()
    // =========================================================================

    @Nested
    @DisplayName("getDisplayNameForLocale()")
    class DisplayNameForLocaleTest {

        @Test
        @DisplayName("Retorna traduccion en espanol")
        void retornaEs() {
            assertThat(buildFull().getDisplayNameForLocale("es")).isEqualTo("Ejecucion HTTP");
        }

        @Test
        @DisplayName("Retorna traduccion en ingles")
        void retornaEn() {
            assertThat(buildFull().getDisplayNameForLocale("en")).isEqualTo("HTTP Execution");
        }

        @Test
        @DisplayName("Retorna traduccion en frances")
        void retornaFr() {
            assertThat(buildFull().getDisplayNameForLocale("fr")).isEqualTo("Execution HTTP");
        }

        @Test
        @DisplayName("Hace fallback a displayName si locale ausente")
        void fallbackADisplayName() {
            assertThat(buildFull().getDisplayNameForLocale("pt")).isEqualTo("Ejecucion HTTP");
        }

        @Test
        @DisplayName("Hace fallback a id si displayName es null y locale ausente")
        void fallbackAIdSiDisplayNameNull() {
            StepInfo info = new StepInfo(
                    "api.execution", "p", "api", "api.execution", "WHEN",
                    "cat", "icon", 10, null, null,
                    Map.of(), Map.of()
            );
            assertThat(info.getDisplayNameForLocale("en")).isEqualTo("api.execution");
        }

        @Test
        @DisplayName("Mapa vacio siempre usa fallback")
        void mapaVacioUsaFallback() {
            StepInfo info = new StepInfo(
                    "api.url", "p", "api", "api.url", "GIVEN",
                    "cat", "icon", 10, "URL / Ambiente", "desc",
                    Map.of(), Map.of()
            );
            assertThat(info.getDisplayNameForLocale("en")).isEqualTo("URL / Ambiente");
        }

        @Test
        @DisplayName("locale null lanza NullPointerException")
        void localeNullLanzaException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> buildFull().getDisplayNameForLocale(null))
                    .withMessageContaining("locale");
        }
    }

    // =========================================================================
    // getDescriptionForLocale()
    // =========================================================================

    @Nested
    @DisplayName("getDescriptionForLocale()")
    class DescriptionForLocaleTest {

        @Test
        @DisplayName("Retorna descripcion en espanol")
        void retornaEs() {
            assertThat(buildFull().getDescriptionForLocale("es")).isEqualTo("Envio de peticiones REST");
        }

        @Test
        @DisplayName("Retorna descripcion en ingles")
        void retornaEn() {
            assertThat(buildFull().getDescriptionForLocale("en")).isEqualTo("Send REST requests");
        }

        @Test
        @DisplayName("Retorna descripcion en frances")
        void retornaFr() {
            assertThat(buildFull().getDescriptionForLocale("fr")).isEqualTo("Envoi de requetes REST");
        }

        @Test
        @DisplayName("Hace fallback a description si locale ausente")
        void fallbackADescription() {
            assertThat(buildFull().getDescriptionForLocale("de")).isEqualTo("Envio de peticiones REST");
        }

        @Test
        @DisplayName("Hace fallback a cadena vacia si description es null y locale ausente")
        void fallbackACadenaVaciaSiDescriptionNull() {
            StepInfo info = new StepInfo(
                    "api.x", "p", "api", "api.x", "GIVEN",
                    "cat", "icon", 10, "dn", null,
                    Map.of(), Map.of()
            );
            assertThat(info.getDescriptionForLocale("en")).isEmpty();
        }

        @Test
        @DisplayName("locale null lanza NullPointerException")
        void localeNullLanzaException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> buildFull().getDescriptionForLocale(null))
                    .withMessageContaining("locale");
        }
    }

    // =========================================================================
    // Semantica de record
    // =========================================================================

    @Nested
    @DisplayName("Semantica de record (equals / toString)")
    class RecordSemanticsTest {

        @Test
        @DisplayName("Dos StepInfo con los mismos valores son iguales")
        void dosInstanciasIgualesConMismosValores() {
            StepInfo a = buildFull();
            StepInfo b = buildFull();
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("toString contiene el id del componente")
        void toStringContieneId() {
            assertThat(buildFull().toString()).contains("api.execution");
        }
    }
}
