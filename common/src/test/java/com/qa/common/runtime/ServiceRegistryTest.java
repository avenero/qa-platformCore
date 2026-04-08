package com.qa.common.runtime;

import org.junit.jupiter.api.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para {@link ServiceRegistry}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("ServiceRegistry")
class ServiceRegistryTest {

    private ServiceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
    }

    // ---- Interfaces y clases dummy para testing ----
    interface DummyService {
        String getValue();
    }

    static class DummyServiceImpl implements DummyService {
        private final String value;
        DummyServiceImpl(String value) { this.value = value; }
        @Override
        public String getValue() { return value; }
    }

    interface AnotherService {
        int getNumber();
    }

    @Nested
    @DisplayName("registerInstance y get")
    class RegisterInstanceTests {

        @Test
        @DisplayName("Registrar y obtener instancia directa")
        void registrarYObtenerInstanciaDirecta() {
            DummyServiceImpl impl = new DummyServiceImpl("test");
            registry.registerInstance(DummyService.class, impl);

            Optional<DummyService> result = registry.get(DummyService.class);
            assertThat(result).isPresent();
            assertThat(result.get().getValue()).isEqualTo("test");
        }

        @Test
        @DisplayName("get retorna vacio para servicio no registrado")
        void getRetornaVacioParaServicioNoRegistrado() {
            Optional<DummyService> result = registry.get(DummyService.class);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("registerInstance con type null lanza NPE")
        void registerInstanceConTypeNullLanzaNPE() {
            assertThatThrownBy(() -> registry.registerInstance(null, new DummyServiceImpl("x")))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("registerInstance con instance null lanza NPE")
        void registerInstanceConInstanceNullLanzaNPE() {
            assertThatThrownBy(() -> registry.registerInstance(DummyService.class, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("registerLazy")
    class RegisterLazyTests {

        @Test
        @DisplayName("registerLazy inicializa al primer get")
        void registerLazyInicializaAlPrimerGet() {
            int[] callCount = {0};
            registry.registerLazy(DummyService.class, () -> {
                callCount[0]++;
                return new DummyServiceImpl("lazy");
            });

            // No debe haberse llamado aun
            assertThat(callCount[0]).isZero();

            // Primer get: inicializa
            Optional<DummyService> result = registry.get(DummyService.class);
            assertThat(result).isPresent();
            assertThat(result.get().getValue()).isEqualTo("lazy");
            assertThat(callCount[0]).isEqualTo(1);
        }

        @Test
        @DisplayName("registerLazy solo llama factory una vez")
        void registerLazySoloLlamaFactoryUnaVez() {
            int[] callCount = {0};
            registry.registerLazy(DummyService.class, () -> {
                callCount[0]++;
                return new DummyServiceImpl("once");
            });

            registry.get(DummyService.class);
            registry.get(DummyService.class);
            registry.get(DummyService.class);

            assertThat(callCount[0]).isEqualTo(1);
        }

        @Test
        @DisplayName("registerLazy con type null lanza NPE")
        void registerLazyConTypeNullLanzaNPE() {
            assertThatThrownBy(() -> registry.registerLazy(null, () -> new DummyServiceImpl("x")))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("registerLazy con factory null lanza NPE")
        void registerLazyConFactoryNullLanzaNPE() {
            assertThatThrownBy(() -> registry.registerLazy(DummyService.class, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("require")
    class RequireTests {

        @Test
        @DisplayName("require retorna servicio registrado")
        void requireRetornaServicioRegistrado() {
            registry.registerInstance(DummyService.class, new DummyServiceImpl("required"));
            DummyService service = registry.require(DummyService.class);
            assertThat(service.getValue()).isEqualTo("required");
        }

        @Test
        @DisplayName("require lanza ISE si no esta registrado")
        void requireLanzaISESiNoEstaRegistrado() {
            assertThatThrownBy(() -> registry.require(DummyService.class))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DummyService");
        }
    }

    @Nested
    @DisplayName("isRegistered, size, clear")
    class OperacionesTests {

        @Test
        @DisplayName("isRegistered retorna true para instancia registrada")
        void isRegisteredTrueParaInstancia() {
            registry.registerInstance(DummyService.class, new DummyServiceImpl("x"));
            assertThat(registry.isRegistered(DummyService.class)).isTrue();
        }

        @Test
        @DisplayName("isRegistered retorna true para factory registrada")
        void isRegisteredTrueParaFactory() {
            registry.registerLazy(DummyService.class, () -> new DummyServiceImpl("x"));
            assertThat(registry.isRegistered(DummyService.class)).isTrue();
        }

        @Test
        @DisplayName("isRegistered retorna false para no registrado")
        void isRegisteredFalseParaNoRegistrado() {
            assertThat(registry.isRegistered(DummyService.class)).isFalse();
        }

        @Test
        @DisplayName("isRegistered retorna false para null")
        void isRegisteredFalseParaNull() {
            assertThat(registry.isRegistered(null)).isFalse();
        }

        @Test
        @DisplayName("size cuenta servicios correctamente")
        void sizeCuentaServiciosCorrectamente() {
            assertThat(registry.size()).isZero();

            registry.registerInstance(DummyService.class, new DummyServiceImpl("x"));
            assertThat(registry.size()).isEqualTo(1);

            registry.registerLazy(AnotherService.class, () -> () -> 42);
            assertThat(registry.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("clear elimina todos los registros")
        void clearEliminaTodosLosRegistros() {
            registry.registerInstance(DummyService.class, new DummyServiceImpl("x"));
            registry.registerLazy(AnotherService.class, () -> () -> 42);

            registry.clear();

            assertThat(registry.size()).isZero();
            assertThat(registry.isRegistered(DummyService.class)).isFalse();
            assertThat(registry.isRegistered(AnotherService.class)).isFalse();
        }
    }

    @Nested
    @DisplayName("get con type null")
    class GetNullTests {

        @Test
        @DisplayName("get con type null lanza NPE")
        void getConTypeNullLanzaNPE() {
            assertThatThrownBy(() -> registry.get(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}

