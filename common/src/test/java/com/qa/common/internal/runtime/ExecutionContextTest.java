package com.qa.common.internal.runtime;


import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.VariableStore;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.api.runtime.ServiceRegistry;
import com.qa.common.api.runtime.events.EventBus;
import com.qa.common.api.runtime.events.ExecutionEvent;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para {@link ExecutionContext}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
@DisplayName("ExecutionContext")
class ExecutionContextTest {

    private ExecutionConfig config;
    private ServiceRegistry registry;
    private VariableStore variables;
    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        config = new ExecutionConfig.Builder()
                .environment("test")
                .build();
        registry = new ServiceRegistry();
        variables = new VariableStore();
        eventBus = new EventBus();

        // Asegurar que no hay contexto residual de otros tests
        ExecutionContext.deactivate();
    }

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    @Nested
    @DisplayName("Construccion")
    class ConstruccionTests {

        @Test
        @DisplayName("Constructor con todos los parametros validos")
        void constructorConParametrosValidos() {
            ExecutionContext ctx = new ExecutionContext(config, registry, variables, eventBus);
            assertThat(ctx.config()).isSameAs(config);
            assertThat(ctx.registry()).isSameAs(registry);
            assertThat(ctx.variables()).isSameAs(variables);
            assertThat(ctx.eventBus()).isSameAs(eventBus);
        }

        @Test
        @DisplayName("Config null lanza NullPointerException")
        void configNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionContext(null, registry, variables, eventBus))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Registry null lanza NullPointerException")
        void registryNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionContext(config, null, variables, eventBus))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Variables null lanza NullPointerException")
        void variablesNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionContext(config, registry, null, eventBus))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("EventBus null lanza NullPointerException")
        void eventBusNullLanzaNPE() {
            assertThatThrownBy(() -> new ExecutionContext(config, registry, variables, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("ThreadLocal (activate/deactivate/current)")
    class ThreadLocalTests {

        @Test
        @DisplayName("current retorna vacio sin contexto activo")
        void currentRetornaVacioSinContextoActivo() {
            Optional<ExecutionContext> ctx = ExecutionContext.current();
            assertThat(ctx).isEmpty();
        }

        @Test
        @DisplayName("activate establece el contexto actual")
        void activateEstableceContextoActual() {
            ExecutionContext ctx = new ExecutionContext(config, registry, variables, eventBus);
            ctx.activate();

            Optional<ExecutionContext> current = ExecutionContext.current();
            assertThat(current).isPresent().containsSame(ctx);
        }

        @Test
        @DisplayName("deactivate remueve el contexto actual")
        void deactivateRemueveContextoActual() {
            ExecutionContext ctx = new ExecutionContext(config, registry, variables, eventBus);
            ctx.activate();
            ExecutionContext.deactivate();

            assertThat(ExecutionContext.current()).isEmpty();
        }

        @Test
        @DisplayName("requireCurrent retorna contexto activo")
        void requireCurrentRetornaContextoActivo() {
            ExecutionContext ctx = new ExecutionContext(config, registry, variables, eventBus);
            ctx.activate();

            ExecutionContext current = ExecutionContext.requireCurrent();
            assertThat(current).isSameAs(ctx);
        }

        @Test
        @DisplayName("requireCurrent lanza ISE sin contexto activo")
        void requireCurrentLanzaISESinContextoActivo() {
            assertThatThrownBy(ExecutionContext::requireCurrent)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No hay ExecutionContext activo");
        }
    }

    @Nested
    @DisplayName("Atajos (service, publishEvent)")
    class AtajosTests {

        @Test
        @DisplayName("service() delega al registry.require()")
        void serviceDelegaAlRegistryRequire() {
            registry.registerInstance(String.class, "Hello World");
            ExecutionContext ctx = new ExecutionContext(config, registry, variables, eventBus);

            String result = ctx.service(String.class);
            assertThat(result).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("service() lanza ISE si no registrado")
        void serviceLanzaISESiNoRegistrado() {
            ExecutionContext ctx = new ExecutionContext(config, registry, variables, eventBus);

            assertThatThrownBy(() -> ctx.service(Integer.class))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("publishEvent() publica evento en el bus")
        void publishEventPublicaEventoEnElBus() {
            List<ExecutionEvent> received = new ArrayList<>();
            eventBus.subscribe(received::add);

            ExecutionContext ctx = new ExecutionContext(config, registry, variables, eventBus);
            ctx.publishEvent(ExecutionEvent.Type.SCENARIO_START, "Login Test");

            assertThat(received).hasSize(1);
            assertThat(received.get(0).getType()).isEqualTo(ExecutionEvent.Type.SCENARIO_START);
            assertThat(received.get(0).getName()).isEqualTo("Login Test");
        }
    }

    @Nested
    @DisplayName("cleanup")
    class CleanupTests {

        @Test
        @DisplayName("cleanup limpia variables, registry, eventBus y desactiva")
        void cleanupLimpiaRecursos() {
            ExecutionContext ctx = new ExecutionContext(config, registry, variables, eventBus);
            ctx.activate();

            variables.set("key", "value");
            registry.registerInstance(String.class, "svc");
            eventBus.subscribe(event -> {});

            ctx.cleanup();

            assertThat(variables.size()).isZero();
            assertThat(registry.size()).isZero();
            assertThat(eventBus.subscriberCount()).isZero();
            assertThat(ExecutionContext.current()).isEmpty();
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString contiene informacion relevante")
        void toStringContieneInfo() {
            ExecutionContext ctx = new ExecutionContext(config, registry, variables, eventBus);
            String str = ctx.toString();
            assertThat(str).contains("ExecutionContext")
                    .contains("config=")
                    .contains("registry=")
                    .contains("variables=");
        }
    }
}

