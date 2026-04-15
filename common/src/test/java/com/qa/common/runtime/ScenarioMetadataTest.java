package com.qa.common.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests unitarios para {@link ScenarioMetadata}.
 *
 * <p>Verifica el constructor canónico, los factory methods y los helpers de consulta.
 *
 * @author Abel Venero
 * @since 2.1.0
 */
@DisplayName("ScenarioMetadata")
class ScenarioMetadataTest {

    // =========================================================================
    // Constructor canónico
    // =========================================================================

    @Nested
    @DisplayName("Constructor")
    class ConstructorTest {

        @Test
        @DisplayName("name null lanza NullPointerException")
        void testNameNullLanzaException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ScenarioMetadata(null, Set.of("@api"), "uri", 1));
        }

        @Test
        @DisplayName("tags null se normaliza a Set vacío")
        void testTagsNullSeNormalizaAVacio() {
            ScenarioMetadata meta = new ScenarioMetadata("test", null, "uri", 1);
            assertThat(meta.tags()).isEmpty();
        }

        @Test
        @DisplayName("uri null se normaliza a cadena vacía")
        void testUriNullSeNormalizaAVacio() {
            ScenarioMetadata meta = new ScenarioMetadata("test", Set.of(), null, 0);
            assertThat(meta.uri()).isEmpty();
        }

        @Test
        @DisplayName("El conjunto de tags es inmutable")
        void testTagsEsInmutable() {
            ScenarioMetadata meta = new ScenarioMetadata("test", Set.of("@api"), "uri", 1);
            assertThat(meta.tags())
                    .containsExactly("@api");

            // UnsupportedOperationException esperada al mutar el set
            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> meta.tags().add("@otro"));
        }

        @Test
        @DisplayName("Todos los campos se almacenan correctamente")
        void testCamposAlmacenados() {
            ScenarioMetadata meta = new ScenarioMetadata(
                    "Login con credenciales válidas",
                    Set.of("@api", "@smoke"),
                    "classpath:features/api/login.feature",
                    12);

            assertThat(meta.name()).isEqualTo("Login con credenciales válidas");
            assertThat(meta.tags()).containsExactlyInAnyOrder("@api", "@smoke");
            assertThat(meta.uri()).isEqualTo("classpath:features/api/login.feature");
            assertThat(meta.line()).isEqualTo(12);
        }
    }

    // =========================================================================
    // Factory methods
    // =========================================================================

    @Nested
    @DisplayName("Factory methods")
    class FactoryTest {

        @Test
        @DisplayName("of() crea instancia con todos los campos")
        void testOfCompleto() {
            ScenarioMetadata meta = ScenarioMetadata.of(
                    "Mi escenario",
                    List.of("@web", "@regression"),
                    "classpath:features/web/checkout.feature",
                    25);

            assertThat(meta.name()).isEqualTo("Mi escenario");
            assertThat(meta.tags()).containsExactlyInAnyOrder("@web", "@regression");
            assertThat(meta.uri()).isEqualTo("classpath:features/web/checkout.feature");
            assertThat(meta.line()).isEqualTo(25);
        }

        @Test
        @DisplayName("of() con tags null crea Set vacío")
        void testOfTagsNull() {
            ScenarioMetadata meta = ScenarioMetadata.of("test", null, "uri", 1);
            assertThat(meta.tags()).isEmpty();
        }

        @Test
        @DisplayName("ofTags() crea instancia con uri='' y line=0")
        void testOfTagsSimple() {
            ScenarioMetadata meta = ScenarioMetadata.ofTags("test", List.of("@api"));

            assertThat(meta.name()).isEqualTo("test");
            assertThat(meta.tags()).containsExactly("@api");
            assertThat(meta.uri()).isEmpty();
            assertThat(meta.line()).isZero();
        }

        @Test
        @DisplayName("ofTags() con tags null crea Set vacío")
        void testOfTagsNullTags() {
            ScenarioMetadata meta = ScenarioMetadata.ofTags("test", null);
            assertThat(meta.tags()).isEmpty();
        }

        @Test
        @DisplayName("of() copia la colección, no la referencia")
        void testOfCopiaColeccion() {
            List<String> tags = new java.util.ArrayList<>(List.of("@api"));
            ScenarioMetadata meta = ScenarioMetadata.of("test", tags, "uri", 1);

            // Mutar la lista original no afecta al record
            tags.add("@extra");
            assertThat(meta.tags()).containsExactly("@api");
        }
    }

    // =========================================================================
    // Helpers de consulta
    // =========================================================================

    @Nested
    @DisplayName("hasTag()")
    class HasTagTest {

        @Test
        @DisplayName("retorna true si el tag está presente")
        void testHasTagPresente() {
            ScenarioMetadata meta = ScenarioMetadata.ofTags("test", List.of("@api", "@smoke"));
            assertThat(meta.hasTag("@api")).isTrue();
            assertThat(meta.hasTag("@smoke")).isTrue();
        }

        @Test
        @DisplayName("retorna false si el tag no está presente")
        void testHasTagAusente() {
            ScenarioMetadata meta = ScenarioMetadata.ofTags("test", List.of("@api"));
            assertThat(meta.hasTag("@web")).isFalse();
        }

        @Test
        @DisplayName("es case-sensitive: @API != @api")
        void testHasTagCaseSensitive() {
            ScenarioMetadata meta = ScenarioMetadata.ofTags("test", List.of("@api"));
            assertThat(meta.hasTag("@API")).isFalse();
        }
    }

    @Nested
    @DisplayName("hasAnyTag()")
    class HasAnyTagTest {

        @Test
        @DisplayName("retorna true si alguno de los tags coincide")
        void testHasAnyTagConCoincidencia() {
            ScenarioMetadata meta = ScenarioMetadata.ofTags("test", List.of("@api", "@smoke"));
            assertThat(meta.hasAnyTag(List.of("@mobile", "@api"))).isTrue();
        }

        @Test
        @DisplayName("retorna false si ningún tag coincide")
        void testHasAnyTagSinCoincidencia() {
            ScenarioMetadata meta = ScenarioMetadata.ofTags("test", List.of("@api"));
            assertThat(meta.hasAnyTag(List.of("@mobile", "@web"))).isFalse();
        }

        @Test
        @DisplayName("retorna false con colección null")
        void testHasAnyTagColeccionNull() {
            ScenarioMetadata meta = ScenarioMetadata.ofTags("test", List.of("@api"));
            assertThat(meta.hasAnyTag(null)).isFalse();
        }

        @Test
        @DisplayName("retorna false con colección vacía")
        void testHasAnyTagColeccionVacia() {
            ScenarioMetadata meta = ScenarioMetadata.ofTags("test", List.of("@api"));
            assertThat(meta.hasAnyTag(List.of())).isFalse();
        }
    }

    @Nested
    @DisplayName("hasTags()")
    class HasTagsTest {

        @Test
        @DisplayName("retorna true si hay al menos un tag")
        void testHasTagsConTags() {
            ScenarioMetadata meta = ScenarioMetadata.ofTags("test", List.of("@api"));
            assertThat(meta.hasTags()).isTrue();
        }

        @Test
        @DisplayName("retorna false si no hay tags")
        void testHasTagsSinTags() {
            ScenarioMetadata meta = ScenarioMetadata.ofTags("test", List.of());
            assertThat(meta.hasTags()).isFalse();
        }
    }

    // =========================================================================
    // Record behavior (equals / hashCode / toString)
    // =========================================================================

    @Nested
    @DisplayName("Record — equals / hashCode / toString")
    class RecordBehaviorTest {

        @Test
        @DisplayName("Dos instancias con mismos valores son iguales")
        void testEquality() {
            ScenarioMetadata a = ScenarioMetadata.of("test", List.of("@api"), "uri", 5);
            ScenarioMetadata b = ScenarioMetadata.of("test", List.of("@api"), "uri", 5);
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("Instancias con valores distintos no son iguales")
        void testInequality() {
            ScenarioMetadata a = ScenarioMetadata.ofTags("test-a", List.of("@api"));
            ScenarioMetadata b = ScenarioMetadata.ofTags("test-b", List.of("@api"));
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("toString incluye name y tags")
        void testToString() {
            ScenarioMetadata meta = ScenarioMetadata.ofTags("Mi escenario", List.of("@api"));
            String str = meta.toString();
            assertThat(str).contains("Mi escenario");
            assertThat(str).contains("@api");
        }
    }
}
