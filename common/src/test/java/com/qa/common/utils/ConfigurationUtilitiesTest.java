package com.qa.common.utils;

import org.junit.jupiter.api.*;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para ConfigurationUtilities.
 *
 * <p>Cubre la lógica pura de parsing y navegación de configuraciones:</p>
 * <ul>
 *   <li>Validación de nombre de archivo (null, vacío)</li>
 *   <li>Archivos inexistentes → excepción clara</li>
 *   <li>parseYaml() / parseJson() desde string</li>
 *   <li>getNestedValue() con notación de puntos</li>
 *   <li>getStringValue() con default</li>
 *   <li>hasKey()</li>
 *   <li>Constructor privado (clase de utilidad)</li>
 * </ul>
 *
 * <p><b>Estrategia:</b> No se leen archivos del filesystem — se parsea
 * directamente desde strings para tests deterministas y sin I/O.</p>
 *
 * @author Abel Venero
 * @since 1.0.0
 */
@DisplayName("ConfigurationUtilities")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfigurationUtilitiesTest {

    // =========================================================================
    // Constructor privado (clase de utilidad)
    // =========================================================================

    @Test
    @DisplayName("Constructor privado lanza UnsupportedOperationException")
    @Order(0)
    void constructorPrivadoLanzaExcepcion() {
        assertThatThrownBy(() -> {
            var constructor = ConfigurationUtilities.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        }).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    // =========================================================================
    // readYamlFile() / readJsonFile() / readPropertiesFile() - validaciones
    // =========================================================================

    @Nested
    @DisplayName("Validaciones de nombre de archivo")
    @Order(1)
    class ValidacionNombreArchivoTests {

        @Test
        @DisplayName("readYamlFile() con null lanza IllegalArgumentException")
        void yamlNullLanzaExcepcion() {
            assertThatThrownBy(() -> ConfigurationUtilities.readYamlFile(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("readYamlFile() con vacío lanza IllegalArgumentException")
        void yamlVacioLanzaExcepcion() {
            assertThatThrownBy(() -> ConfigurationUtilities.readYamlFile(""))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("readJsonFile() con null lanza IllegalArgumentException")
        void jsonNullLanzaExcepcion() {
            assertThatThrownBy(() -> ConfigurationUtilities.readJsonFile(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("readJsonFile() con vacío lanza IllegalArgumentException")
        void jsonVacioLanzaExcepcion() {
            assertThatThrownBy(() -> ConfigurationUtilities.readJsonFile(""))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("readPropertiesFile() con null lanza IllegalArgumentException")
        void propertiesNullLanzaExcepcion() {
            assertThatThrownBy(() -> ConfigurationUtilities.readPropertiesFile(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("readPropertiesFile() con vacío lanza IllegalArgumentException")
        void propertiesVacioLanzaExcepcion() {
            assertThatThrownBy(() -> ConfigurationUtilities.readPropertiesFile(""))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // Archivos inexistentes → RuntimeException con mensaje claro
    // =========================================================================

    @Nested
    @DisplayName("Archivos inexistentes")
    @Order(2)
    class ArchivosInexistentesTests {

        @Test
        @DisplayName("readYamlFile() con archivo inexistente lanza RuntimeException")
        void yamlInexistenteLanzaRuntimeException() {
            assertThatThrownBy(() -> ConfigurationUtilities.readYamlFile("no-existe-xyz.yml"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no-existe-xyz.yml");
        }

        @Test
        @DisplayName("readJsonFile() con archivo inexistente lanza RuntimeException")
        void jsonInexistenteLanzaRuntimeException() {
            assertThatThrownBy(() -> ConfigurationUtilities.readJsonFile("no-existe-xyz.json"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no-existe-xyz.json");
        }

        @Test
        @DisplayName("readPropertiesFile() con archivo inexistente lanza RuntimeException")
        void propertiesInexistenteLanzaRuntimeException() {
            assertThatThrownBy(() -> ConfigurationUtilities.readPropertiesFile("no-existe-xyz.properties"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no-existe-xyz.properties");
        }
    }

    // =========================================================================
    // parseYaml() desde string
    // =========================================================================

    @Nested
    @DisplayName("parseYaml() desde string")
    @Order(3)
    class ParseYamlTests {

        @Test
        @DisplayName("Parsea YAML simple con claves de nivel raíz")
        void parseaYamlSimple() {
            String yaml = "host: localhost\nport: 5432\nenabled: true";
            Map<String, Object> result = ConfigurationUtilities.parseYaml(yaml);

            assertThat(result)
                .containsEntry("host", "localhost")
                .containsEntry("port", 5432)
                .containsEntry("enabled", true);
        }

        @Test
        @DisplayName("Parsea YAML anidado con estructura de nivel múltiple")
        void parseaYamlAnidado() {
            String yaml = "database:\n  host: localhost\n  port: 5432\napi:\n  timeout: 30000";
            Map<String, Object> result = ConfigurationUtilities.parseYaml(yaml);

            assertThat(result).containsKey("database");
            @SuppressWarnings("unchecked")
            Map<String, Object> db = (Map<String, Object>) result.get("database");
            assertThat(db)
                .containsEntry("host", "localhost")
                .containsEntry("port", 5432);
        }

        @Test
        @DisplayName("Parsea YAML con lista de valores")
        void parseaYamlConLista() {
            String yaml = "environments:\n  - dev\n  - qa\n  - prod";
            Map<String, Object> result = ConfigurationUtilities.parseYaml(yaml);

            assertThat(result).containsKey("environments");
            assertThat(result.get("environments")).isInstanceOf(java.util.List.class);
        }

        @Test
        @DisplayName("parseYaml() con null lanza IllegalArgumentException")
        void yamlNullLanzaExcepcion() {
            assertThatThrownBy(() -> ConfigurationUtilities.parseYaml(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parseYaml() con vacío lanza IllegalArgumentException")
        void yamlVacioLanzaExcepcion() {
            assertThatThrownBy(() -> ConfigurationUtilities.parseYaml(""))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parseYaml() con YAML inválido lanza RuntimeException")
        void yamlInvalidoLanzaRuntimeException() {
            // YAML malformado con tabulación ilegal
            String yamlInvalido = "key: value\n\tmalformed";
            assertThatThrownBy(() -> ConfigurationUtilities.parseYaml(yamlInvalido))
                .isInstanceOf(RuntimeException.class);
        }
    }

    // =========================================================================
    // parseJson() desde string
    // =========================================================================

    @Nested
    @DisplayName("parseJson() desde string")
    @Order(4)
    class ParseJsonTests {

        @Test
        @DisplayName("Parsea JSON simple con claves de nivel raíz")
        void parseaJsonSimple() {
            String json = "{\"host\":\"localhost\",\"port\":5432,\"enabled\":true}";
            Map<String, Object> result = ConfigurationUtilities.parseJson(json);

            assertThat(result)
                .containsEntry("host", "localhost")
                .containsEntry("port", 5432)
                .containsEntry("enabled", true);
        }

        @Test
        @DisplayName("Parsea JSON anidado")
        void parseaJsonAnidado() {
            String json = "{\"database\":{\"host\":\"db.server\",\"port\":1433}}";
            Map<String, Object> result = ConfigurationUtilities.parseJson(json);

            assertThat(result).containsKey("database");
            @SuppressWarnings("unchecked")
            Map<String, Object> db = (Map<String, Object>) result.get("database");
            assertThat(db).containsEntry("host", "db.server");
        }

        @Test
        @DisplayName("parseJson() con null lanza IllegalArgumentException")
        void jsonNullLanzaExcepcion() {
            assertThatThrownBy(() -> ConfigurationUtilities.parseJson(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parseJson() con vacío lanza IllegalArgumentException")
        void jsonVacioLanzaExcepcion() {
            assertThatThrownBy(() -> ConfigurationUtilities.parseJson(""))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parseJson() con JSON malformado lanza RuntimeException")
        void jsonMalformadoLanzaRuntimeException() {
            assertThatThrownBy(() -> ConfigurationUtilities.parseJson("{clave:sinComillas}"))
                .isInstanceOf(RuntimeException.class);
        }
    }

    // =========================================================================
    // getNestedValue() - navegación con notación de puntos
    // =========================================================================

    @Nested
    @DisplayName("getNestedValue() - navegación anidada")
    @Order(5)
    class GetNestedValueTests {

        private Map<String, Object> config;

        @BeforeEach
        void setUp() {
            String yaml = "database:\n  host: localhost\n  port: 5432\n  ssl:\n    enabled: true\napi:\n  timeout: 30000\nname: framework";
            config = ConfigurationUtilities.parseYaml(yaml);
        }

        @Test
        @DisplayName("Obtiene valor de nivel raíz")
        void obtieneClavePrimerNivel() {
            assertThat(ConfigurationUtilities.getNestedValue(config, "name")).isEqualTo("framework");
        }

        @Test
        @DisplayName("Obtiene valor anidado un nivel")
        void obtieneClaveDosNiveles() {
            assertThat(ConfigurationUtilities.getNestedValue(config, "database.host"))
                .isEqualTo("localhost");
        }

        @Test
        @DisplayName("Obtiene valor anidado dos niveles")
        void obtieneClaveTresNiveles() {
            assertThat(ConfigurationUtilities.getNestedValue(config, "database.ssl.enabled"))
                .isEqualTo(true);
        }

        @Test
        @DisplayName("Retorna null para clave inexistente")
        void retornaNullParaClaveInexistente() {
            assertThat(ConfigurationUtilities.getNestedValue(config, "database.usuario"))
                .isNull();
        }

        @Test
        @DisplayName("Retorna null para ruta con segmento no existente")
        void retornaNullParaRutaConSegmentoNoExistente() {
            assertThat(ConfigurationUtilities.getNestedValue(config, "noExiste.clave"))
                .isNull();
        }

        @Test
        @DisplayName("Retorna null cuando config es null")
        void retornaNullConfigNull() {
            assertThat(ConfigurationUtilities.getNestedValue(null, "clave")).isNull();
        }

        @Test
        @DisplayName("Retorna null cuando path es null")
        void retornaNullPathNull() {
            assertThat(ConfigurationUtilities.getNestedValue(config, null)).isNull();
        }

        @Test
        @DisplayName("Retorna null cuando path es vacío")
        void retornaNullPathVacio() {
            assertThat(ConfigurationUtilities.getNestedValue(config, "")).isNull();
        }

        @Test
        @DisplayName("Retorna el mapa completo de un nodo intermedio")
        void retornaMapaParaNodoIntermedio() {
            Object dbNode = ConfigurationUtilities.getNestedValue(config, "database");
            assertThat(dbNode).isInstanceOf(Map.class);
        }
    }

    // =========================================================================
    // getStringValue() - con default
    // =========================================================================

    @Nested
    @DisplayName("getStringValue() - con default")
    @Order(6)
    class GetStringValueTests {

        private Map<String, Object> config;

        @BeforeEach
        void setUp() {
            String yaml = "host: localhost\nnested:\n  timeout: 30000";
            config = ConfigurationUtilities.parseYaml(yaml);
        }

        @Test
        @DisplayName("Retorna valor como String cuando existe")
        void retornaValorComoString() {
            assertThat(ConfigurationUtilities.getStringValue(config, "host", "default"))
                .isEqualTo("localhost");
        }

        @Test
        @DisplayName("Retorna default cuando clave no existe")
        void retornaDefaultParaClaveInexistente() {
            assertThat(ConfigurationUtilities.getStringValue(config, "noExiste", "valorPorDefecto"))
                .isEqualTo("valorPorDefecto");
        }

        @Test
        @DisplayName("Convierte número a String correctamente")
        void conviertNumeroAString() {
            assertThat(ConfigurationUtilities.getStringValue(config, "nested.timeout", "0"))
                .isEqualTo("30000");
        }

        @Test
        @DisplayName("Retorna null como default cuando no existe clave y default es null")
        void retornaNullDefaultCuandoNoExiste() {
            assertThat(ConfigurationUtilities.getStringValue(config, "noExiste", null))
                .isNull();
        }
    }

    // =========================================================================
    // hasKey()
    // =========================================================================

    @Nested
    @DisplayName("hasKey()")
    @Order(7)
    class HasKeyTests {

        private Map<String, Object> config;

        @BeforeEach
        void setUp() {
            String yaml = "host: localhost\nnested:\n  port: 5432";
            config = ConfigurationUtilities.parseYaml(yaml);
        }

        @Test
        @DisplayName("Retorna true para clave existente de primer nivel")
        void trueParaClaveExistente() {
            assertThat(ConfigurationUtilities.hasKey(config, "host")).isTrue();
        }

        @Test
        @DisplayName("Retorna true para clave anidada existente")
        void trueParaClaveAnidadaExistente() {
            assertThat(ConfigurationUtilities.hasKey(config, "nested.port")).isTrue();
        }

        @Test
        @DisplayName("Retorna false para clave inexistente")
        void falseParaClaveInexistente() {
            assertThat(ConfigurationUtilities.hasKey(config, "noExiste")).isFalse();
        }

        @Test
        @DisplayName("Retorna false para ruta con nodo intermedio inexistente")
        void falseParaRutaConNodoInexistente() {
            assertThat(ConfigurationUtilities.hasKey(config, "nested.ssl.enabled")).isFalse();
        }

        @Test
        @DisplayName("Retorna false para config null")
        void falseParaConfigNull() {
            assertThat(ConfigurationUtilities.hasKey(null, "host")).isFalse();
        }

        @Test
        @DisplayName("Retorna false para path null")
        void falseParaPathNull() {
            assertThat(ConfigurationUtilities.hasKey(config, null)).isFalse();
        }
    }

}

