package com.qa.common.runtime;


import com.qa.common.api.runtime.VariableStore;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para {@link VariableStore}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("VariableStore")
class VariableStoreTest {

    private VariableStore store;

    @BeforeEach
    void setUp() {
        store = new VariableStore();
    }

    @Nested
    @DisplayName("set y get")
    class SetGetTests {

        @Test
        @DisplayName("set y get String correctamente")
        void setYGetStringCorrectamente() {
            store.set("nombre", "Abel");
            Optional<String> valor = store.get("nombre", String.class);
            assertThat(valor).isPresent().contains("Abel");
        }

        @Test
        @DisplayName("set y get Integer correctamente")
        void setYGetIntegerCorrectamente() {
            store.set("edad", 30);
            Optional<Integer> valor = store.get("edad", Integer.class);
            assertThat(valor).isPresent().contains(30);
        }

        @Test
        @DisplayName("get retorna vacio para clave inexistente")
        void getRetornaVacioParaClaveInexistente() {
            Optional<String> valor = store.get("noExiste", String.class);
            assertThat(valor).isEmpty();
        }

        @Test
        @DisplayName("get retorna vacio si tipo no coincide")
        void getRetornaVacioSiTipoNoCoincide() {
            store.set("numero", 42);
            Optional<String> valor = store.get("numero", String.class);
            assertThat(valor).isEmpty();
        }

        @Test
        @DisplayName("set sobrescribe valor existente")
        void setSobrescribeValorExistente() {
            store.set("key", "original");
            store.set("key", "actualizado");
            assertThat(store.get("key", String.class)).contains("actualizado");
        }

        @Test
        @DisplayName("set con key null lanza NullPointerException")
        void setConKeyNullLanzaNPE() {
            assertThatThrownBy(() -> store.set(null, "valor"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("set con value null lanza NullPointerException")
        void setConValueNullLanzaNPE() {
            assertThatThrownBy(() -> store.set("key", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("get con key null lanza NullPointerException")
        void getConKeyNullLanzaNPE() {
            assertThatThrownBy(() -> store.get(null, String.class))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("get con type null lanza NullPointerException")
        void getConTypeNullLanzaNPE() {
            assertThatThrownBy(() -> store.get("key", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("require")
    class RequireTests {

        @Test
        @DisplayName("require retorna valor existente")
        void requireRetornaValorExistente() {
            store.set("token", "abc-123");
            String valor = store.require("token", String.class);
            assertThat(valor).isEqualTo("abc-123");
        }

        @Test
        @DisplayName("require lanza IllegalStateException si no existe")
        void requireLanzaISESiNoExiste() {
            assertThatThrownBy(() -> store.require("noExiste", String.class))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("noExiste");
        }

        @Test
        @DisplayName("require lanza IllegalStateException si tipo no coincide")
        void requireLanzaISESiTipoNoCoincide() {
            store.set("numero", 42);
            assertThatThrownBy(() -> store.require("numero", String.class))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("has, remove, size, clear")
    class OperacionesTests {

        @Test
        @DisplayName("has retorna true para clave existente")
        void hasRetornaTrueParaClaveExistente() {
            store.set("key", "value");
            assertThat(store.has("key")).isTrue();
        }

        @Test
        @DisplayName("has retorna false para clave inexistente")
        void hasRetornaFalseParaClaveInexistente() {
            assertThat(store.has("noExiste")).isFalse();
        }

        @Test
        @DisplayName("has retorna false para null")
        void hasRetornaFalseParaNull() {
            assertThat(store.has(null)).isFalse();
        }

        @Test
        @DisplayName("remove elimina variable existente")
        void removeEliminaVariableExistente() {
            store.set("key", "value");
            boolean removed = store.remove("key");
            assertThat(removed).isTrue();
            assertThat(store.has("key")).isFalse();
        }

        @Test
        @DisplayName("remove retorna false para clave inexistente")
        void removeRetornaFalseParaClaveInexistente() {
            assertThat(store.remove("noExiste")).isFalse();
        }

        @Test
        @DisplayName("remove retorna false para null")
        void removeRetornaFalseParaNull() {
            assertThat(store.remove(null)).isFalse();
        }

        @Test
        @DisplayName("size retorna cantidad correcta")
        void sizeRetornaCantidadCorrecta() {
            assertThat(store.size()).isZero();
            store.set("a", 1);
            store.set("b", 2);
            assertThat(store.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("clear elimina todas las variables")
        void clearEliminaTodasLasVariables() {
            store.set("a", 1);
            store.set("b", 2);
            store.set("c", 3);
            store.clear();
            assertThat(store.size()).isZero();
            assertThat(store.has("a")).isFalse();
        }
    }

    @Nested
    @DisplayName("getAll")
    class GetAllTests {

        @Test
        @DisplayName("getAll retorna vista inmutable con todas las variables")
        void getAllRetornaVistaInmutable() {
            store.set("x", 1);
            store.set("y", "dos");
            Map<String, Object> all = store.getAll();
            assertThat(all).hasSize(2).containsKeys("x", "y");
        }

        @Test
        @DisplayName("getAll retorna mapa inmutable")
        void getAllRetornaMapaInmutable() {
            store.set("key", "val");
            Map<String, Object> all = store.getAll();
            assertThatThrownBy(() -> all.put("new", "value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString contiene size y keys")
        void toStringContieneInfo() {
            store.set("token", "abc");
            String str = store.toString();
            assertThat(str).contains("VariableStore").contains("size=1").contains("token");
        }
    }
}

