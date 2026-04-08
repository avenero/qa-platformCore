package com.qa.common.database.helpers;

import com.qa.common.database.interfaces.DatabaseConnector;
import com.qa.common.http.exceptions.FrameworkBusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para DatabaseHelper.
 *
 * <p><b>Estrategia de cobertura:</b></p>
 * <ul>
 *   <li>Métodos que operan sobre {@code Map<String,Object>} se testean sin BD real.</li>
 *   <li>{@code executeQuery()} y {@code executeStatement()} se testean solo para
 *       validar la precondición de {@code connector == null}; la ruta de conexión
 *       real requeriría una BD embebida que está fuera del scope de este sprint.</li>
 * </ul>
 *
 * <p><b>Cobertura objetivo:</b> ~75 % (toda la lógica pura sin JDBC real)</p>
 *
 * @author Abel Venero
 * @since 1.0.0
 */
@DisplayName("DatabaseHelper")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseHelperTest {

    // =========================================================================
    // Fixtures reutilizables
    // =========================================================================

    /** Resultado típico de una query con datos. */
    private Map<String, Object> resultConDatos() {
        Map<String, Object> r = new HashMap<>();
        r.put("ID", 42);
        r.put("NOMBRE", "Abel");
        r.put("EMAIL", "abel@example.com");
        r.put("_rowCount", 1);
        r.put("_hasResults", true);
        return r;
    }

    /** Resultado de una query que no devolvió filas. */
    private Map<String, Object> resultSinDatos() {
        Map<String, Object> r = new HashMap<>();
        r.put("_rowCount", 0);
        r.put("_hasResults", false);
        return r;
    }

    // =========================================================================
    // executeQuery() — precondición connector null
    // =========================================================================

    @Nested
    @DisplayName("executeQuery() - precondición connector")
    @Order(1)
    class ExecuteQueryPrecondicionTests {

        @Test
        @DisplayName("Lanza FrameworkBusinessException cuando connector es null")
        void connectorNullLanzaExcepcion() {
            assertThatThrownBy(() ->
                DatabaseHelper.executeQuery(null, "SELECT 1", null)
            )
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("conexión");
        }

        @Test
        @DisplayName("El mensaje de excepción indica cómo establecer la conexión")
        void mensajeExcepcionEsDescriptivo() {
            assertThatThrownBy(() ->
                DatabaseHelper.executeQuery(null, "SELECT * FROM users", null)
            )
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("establezco conexion");
        }
    }

    // =========================================================================
    // executeStatement() — precondición connector null
    // =========================================================================

    @Nested
    @DisplayName("executeStatement() - precondición connector")
    @Order(2)
    class ExecuteStatementPrecondicionTests {

        @Test
        @DisplayName("Lanza FrameworkBusinessException cuando connector es null")
        void connectorNullLanzaExcepcion() {
            assertThatThrownBy(() ->
                DatabaseHelper.executeStatement(null, "DELETE FROM temp", null)
            )
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("conexión");
        }

        @Test
        @DisplayName("El mensaje de excepción indica cómo establecer la conexión")
        void mensajeExcepcionEsDescriptivo() {
            assertThatThrownBy(() ->
                DatabaseHelper.executeStatement(null, "INSERT INTO t VALUES(1)", "")
            )
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("establezco conexion");
        }
    }

    // =========================================================================
    // getColumnValue()
    // =========================================================================

    @Nested
    @DisplayName("getColumnValue()")
    @Order(3)
    class GetColumnValueTests {

        @Test
        @DisplayName("Retorna valor correcto para columna existente (case exacto)")
        void retornaValorColumnaExistente() {
            assertThat(DatabaseHelper.getColumnValue(resultConDatos(), "ID"))
                .isEqualTo(42);
        }

        @Test
        @DisplayName("Búsqueda es case-insensitive")
        void busquedaCaseInsensitive() {
            // El mapa tiene "NOMBRE" en mayúsculas, buscamos en minúsculas
            assertThat(DatabaseHelper.getColumnValue(resultConDatos(), "nombre"))
                .isEqualTo("Abel");
        }

        @Test
        @DisplayName("Búsqueda en mayúsculas encuentra columna en minúsculas")
        void busquedaMayusculasEncontranMinusculas() {
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("email", "test@test.com");
            resultado.put("_rowCount", 1);
            resultado.put("_hasResults", true);

            assertThat(DatabaseHelper.getColumnValue(resultado, "EMAIL"))
                .isEqualTo("test@test.com");
        }

        @Test
        @DisplayName("Retorna null para columna inexistente")
        void retornaNullParaColumnaInexistente() {
            assertThat(DatabaseHelper.getColumnValue(resultConDatos(), "COLUMNA_QUE_NO_EXISTE"))
                .isNull();
        }

        @Test
        @DisplayName("Retorna null cuando queryResult es null")
        void retornaNullCuandoResultadoEsNull() {
            assertThat(DatabaseHelper.getColumnValue(null, "ID")).isNull();
        }

        @Test
        @DisplayName("Retorna null cuando queryResult está vacío")
        void retornaNullCuandoResultadoEstaVacio() {
            assertThat(DatabaseHelper.getColumnValue(new HashMap<>(), "ID")).isNull();
        }

        @Test
        @DisplayName("Retorna null cuando el valor de la columna es null")
        void retornaNullCuandoValorColumnaEsNull() {
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("ESTADO", null);
            resultado.put("_rowCount", 1);
            resultado.put("_hasResults", true);

            assertThat(DatabaseHelper.getColumnValue(resultado, "ESTADO")).isNull();
        }

        @Test
        @DisplayName("Retorna valor de columna cuando es de tipo String")
        void retornaValorString() {
            assertThat(DatabaseHelper.getColumnValue(resultConDatos(), "EMAIL"))
                .isEqualTo("abel@example.com");
        }

        @Test
        @DisplayName("Retorna valor de columna cuando es de tipo Integer")
        void retornaValorInteger() {
            assertThat(DatabaseHelper.getColumnValue(resultConDatos(), "ID"))
                .isInstanceOf(Integer.class)
                .isEqualTo(42);
        }
    }

    // =========================================================================
    // hasResults()
    // =========================================================================

    @Nested
    @DisplayName("hasResults()")
    @Order(4)
    class HasResultsTests {

        @Test
        @DisplayName("Retorna true cuando _hasResults es true")
        void trueConFlagTrue() {
            assertThat(DatabaseHelper.hasResults(resultConDatos())).isTrue();
        }

        @Test
        @DisplayName("Retorna false cuando _hasResults es false")
        void falseConFlagFalse() {
            assertThat(DatabaseHelper.hasResults(resultSinDatos())).isFalse();
        }

        @Test
        @DisplayName("Retorna false cuando queryResult es null")
        void falseConNull() {
            assertThat(DatabaseHelper.hasResults(null)).isFalse();
        }

        @Test
        @DisplayName("Retorna false cuando el mapa está vacío")
        void falseConMapaVacio() {
            assertThat(DatabaseHelper.hasResults(new HashMap<>())).isFalse();
        }

        @Test
        @DisplayName("Fallback: retorna true si hay más de 2 claves (sin flag _hasResults)")
        void fallbackTresClavesSinFlag() {
            // Sin _hasResults pero con 3 columnas reales
            Map<String, Object> sinFlag = new HashMap<>();
            sinFlag.put("ID", 1);
            sinFlag.put("NOMBRE", "test");
            sinFlag.put("EMAIL", "test@test.com");
            // No tiene _hasResults ni _rowCount

            assertThat(DatabaseHelper.hasResults(sinFlag)).isTrue();
        }

        @Test
        @DisplayName("Fallback: retorna false si hay 2 o menos claves sin flag")
        void fallbackDosClavesSinFlag() {
            Map<String, Object> sinFlag = new HashMap<>();
            sinFlag.put("ID", 1);
            sinFlag.put("NOMBRE", "test");
            // Solo 2 claves, sin _hasResults

            assertThat(DatabaseHelper.hasResults(sinFlag)).isFalse();
        }
    }

    // =========================================================================
    // getRowCount()
    // =========================================================================

    @Nested
    @DisplayName("getRowCount()")
    @Order(5)
    class GetRowCountTests {

        @Test
        @DisplayName("Retorna conteo correcto cuando _rowCount está presente")
        void retornaConteoCorrecto() {
            assertThat(DatabaseHelper.getRowCount(resultConDatos())).isEqualTo(1);
        }

        @Test
        @DisplayName("Retorna 0 cuando _rowCount es 0")
        void retornaCeroSinResultados() {
            assertThat(DatabaseHelper.getRowCount(resultSinDatos())).isEqualTo(0);
        }

        @Test
        @DisplayName("Retorna 0 cuando queryResult es null")
        void retornaCeroConNull() {
            assertThat(DatabaseHelper.getRowCount(null)).isEqualTo(0);
        }

        @Test
        @DisplayName("Retorna 0 cuando queryResult no tiene clave _rowCount")
        void retornaCeroSinClave() {
            Map<String, Object> sinClave = new HashMap<>();
            sinClave.put("NOMBRE", "test");
            assertThat(DatabaseHelper.getRowCount(sinClave)).isEqualTo(0);
        }

        @Test
        @DisplayName("Retorna 0 cuando _rowCount tiene tipo inesperado")
        void retornaCeroConTipoIncorrecto() {
            Map<String, Object> tipoRaro = new HashMap<>();
            tipoRaro.put("_rowCount", "no-es-un-int");
            assertThat(DatabaseHelper.getRowCount(tipoRaro)).isEqualTo(0);
        }

        @Test
        @DisplayName("Retorna conteo correcto para múltiples filas")
        void retornaConteoCorrecto_MultiplesFilas() {
            Map<String, Object> multi = new HashMap<>();
            multi.put("_rowCount", 25);
            multi.put("_hasResults", true);
            assertThat(DatabaseHelper.getRowCount(multi)).isEqualTo(25);
        }
    }

    // =========================================================================
    // validateHasResults()
    // =========================================================================

    @Nested
    @DisplayName("validateHasResults()")
    @Order(6)
    class ValidateHasResultsTests {

        @Test
        @DisplayName("No lanza excepción cuando hay resultados")
        void noLanzaConResultados() {
            assertThatCode(() -> DatabaseHelper.validateHasResults(resultConDatos()))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Lanza FrameworkBusinessException cuando queryResult es null")
        void lanzaExcepcionConNull() {
            assertThatThrownBy(() -> DatabaseHelper.validateHasResults(null))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("No hay resultados");
        }

        @Test
        @DisplayName("Lanza FrameworkBusinessException cuando no hay filas")
        void lanzaExcepcionSinFilas() {
            assertThatThrownBy(() -> DatabaseHelper.validateHasResults(resultSinDatos()))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("0 filas");
        }

        @Test
        @DisplayName("El mensaje de excepción sugiere ejecutar la consulta primero")
        void mensajeSugierePasoAnterior() {
            assertThatThrownBy(() -> DatabaseHelper.validateHasResults(null))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("ejecuto la consulta");
        }
    }

    // =========================================================================
    // validateNoResults()
    // =========================================================================

    @Nested
    @DisplayName("validateNoResults()")
    @Order(7)
    class ValidateNoResultsTests {

        @Test
        @DisplayName("No lanza excepción cuando no hay resultados")
        void noLanzaSinResultados() {
            assertThatCode(() -> DatabaseHelper.validateNoResults(resultSinDatos()))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Lanza FrameworkBusinessException cuando queryResult es null")
        void lanzaExcepcionConNull() {
            assertThatThrownBy(() -> DatabaseHelper.validateNoResults(null))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("No hay resultados");
        }

        @Test
        @DisplayName("Lanza FrameworkBusinessException cuando sí hay filas")
        void lanzaExcepcionConFilas() {
            assertThatThrownBy(() -> DatabaseHelper.validateNoResults(resultConDatos()))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("NO retorne resultados");
        }

        @Test
        @DisplayName("El mensaje indica la cantidad de filas encontradas")
        void mensajeIndicaCantidadFilas() {
            Map<String, Object> variasFilas = new HashMap<>();
            variasFilas.put("_rowCount", 5);
            variasFilas.put("_hasResults", true);
            variasFilas.put("ID", 1);
            variasFilas.put("NOMBRE", "test");
            variasFilas.put("EMAIL", "t@t.com");

            assertThatThrownBy(() -> DatabaseHelper.validateNoResults(variasFilas))
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("5");
        }
    }

    // =========================================================================
    // validateColumnValue()
    // =========================================================================

    @Nested
    @DisplayName("validateColumnValue()")
    @Order(8)
    class ValidateColumnValueTests {

        @Test
        @DisplayName("No lanza excepción cuando el valor coincide exactamente")
        void noLanzaConValorCorrecto() {
            assertThatCode(() ->
                DatabaseHelper.validateColumnValue(resultConDatos(), "NOMBRE", "Abel")
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("No lanza excepción para comparación numérica como string")
        void noLanzaConNumeroComoString() {
            assertThatCode(() ->
                DatabaseHelper.validateColumnValue(resultConDatos(), "ID", "42")
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Lanza excepción cuando queryResult es null")
        void lanzaExcepcionConResultadoNull() {
            assertThatThrownBy(() ->
                DatabaseHelper.validateColumnValue(null, "ID", "42")
            )
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("No hay resultados");
        }

        @Test
        @DisplayName("Lanza excepción cuando queryResult está vacío")
        void lanzaExcepcionConResultadoVacio() {
            assertThatThrownBy(() ->
                DatabaseHelper.validateColumnValue(new HashMap<>(), "ID", "42")
            ).isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("Lanza excepción cuando la columna no existe en el resultado")
        void lanzaExcepcionColumnaInexistente() {
            assertThatThrownBy(() ->
                DatabaseHelper.validateColumnValue(resultConDatos(), "TELEFONO", "123")
            )
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("TELEFONO");
        }

        @Test
        @DisplayName("Lanza excepción cuando el valor no coincide")
        void lanzaExcepcionValorIncorrecto() {
            assertThatThrownBy(() ->
                DatabaseHelper.validateColumnValue(resultConDatos(), "NOMBRE", "Pedro")
            )
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("Pedro")
                .hasMessageContaining("Abel");
        }

        @Test
        @DisplayName("El mensaje de error muestra el valor esperado y el actual")
        void mensajeMuestraEsperadoYActual() {
            assertThatThrownBy(() ->
                DatabaseHelper.validateColumnValue(resultConDatos(), "EMAIL", "otro@banco.com")
            )
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("otro@banco.com")   // esperado
                .hasMessageContaining("abel@example.com"); // actual
        }

        @Test
        @DisplayName("La comparación es exacta (case-sensitive para valores)")
        void comparacionEsCaseSensitive() {
            // "abel" (minúsculas) no debe coincidir con "Abel" (capitalizado)
            assertThatThrownBy(() ->
                DatabaseHelper.validateColumnValue(resultConDatos(), "NOMBRE", "abel")
            ).isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("Lanza excepción cuando la columna existe pero su valor es null")
        void lanzaExcepcionValorColumnaEsNull() {
            Map<String, Object> conNull = new HashMap<>();
            conNull.put("ESTADO", null);
            conNull.put("_rowCount", 1);
            conNull.put("_hasResults", true);

            assertThatThrownBy(() ->
                DatabaseHelper.validateColumnValue(conNull, "ESTADO", "ACTIVO")
            )
                .isInstanceOf(FrameworkBusinessException.class)
                .hasMessageContaining("null");
        }
    }

    // =========================================================================
    // Integración lógica: flujos encadenados con Map puro
    // =========================================================================

    @Nested
    @DisplayName("Flujos encadenados (sin BD real)")
    @Order(9)
    class FlujoEncadenadoTests {

        @Test
        @DisplayName("Flujo: obtener columna → validar valor → exitoso")
        void flujoObtenerYValidar() throws FrameworkBusinessException {
            Map<String, Object> resultado = resultConDatos();

            // Verificar que hay resultados
            assertThat(DatabaseHelper.hasResults(resultado)).isTrue();
            assertThat(DatabaseHelper.getRowCount(resultado)).isEqualTo(1);

            // Obtener valor y validarlo
            Object nombre = DatabaseHelper.getColumnValue(resultado, "NOMBRE");
            assertThat(nombre).isEqualTo("Abel");

            // Validación formal
            DatabaseHelper.validateColumnValue(resultado, "NOMBRE", "Abel");
            DatabaseHelper.validateHasResults(resultado);
        }

        @Test
        @DisplayName("Flujo: resultado vacío → validateNoResults pasa → validateHasResults falla")
        void flujoResultadoVacio() {
            Map<String, Object> vacio = resultSinDatos();

            assertThat(DatabaseHelper.hasResults(vacio)).isFalse();
            assertThat(DatabaseHelper.getRowCount(vacio)).isEqualTo(0);

            assertThatCode(() -> DatabaseHelper.validateNoResults(vacio))
                .doesNotThrowAnyException();

            assertThatThrownBy(() -> DatabaseHelper.validateHasResults(vacio))
                .isInstanceOf(FrameworkBusinessException.class);
        }

        @Test
        @DisplayName("getColumnValue es case-insensitive pero validateColumnValue es case-sensitive en valores")
        void columnaCaseInsensitiveValorCaseSensitive() throws FrameworkBusinessException {
            Map<String, Object> resultado = resultConDatos();

            // La columna se puede buscar en cualquier case
            Object valorPorMinusculas = DatabaseHelper.getColumnValue(resultado, "nombre");
            Object valorPorMayusculas = DatabaseHelper.getColumnValue(resultado, "NOMBRE");
            assertThat(valorPorMinusculas).isEqualTo(valorPorMayusculas).isEqualTo("Abel");

            // El valor comparado es case-sensitive
            DatabaseHelper.validateColumnValue(resultado, "nombre", "Abel"); // OK
        }

        @Test
        @DisplayName("Múltiples columnas extraídas del mismo resultado")
        void multiplesColumnasDelMismoResultado() {
            Map<String, Object> resultado = resultConDatos();

            assertThat(DatabaseHelper.getColumnValue(resultado, "ID")).isEqualTo(42);
            assertThat(DatabaseHelper.getColumnValue(resultado, "NOMBRE")).isEqualTo("Abel");
            assertThat(DatabaseHelper.getColumnValue(resultado, "EMAIL")).isEqualTo("abel@example.com");
        }
    }
}

