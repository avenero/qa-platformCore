package com.qa.common.runtime;

import com.qa.common.runtime.events.EventBus;
import io.cucumber.plugin.event.EventHandler;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.Location;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests unitarios para {@link ScenarioLifecycleBridge}.
 *
 * <p>Verifica que el bridge:
 * <ul>
 *   <li>Registra los handlers correctos en el {@link EventPublisher}</li>
 *   <li>Convierte {@link TestCaseStarted} en {@code onScenarioStart} con {@link ScenarioMetadata} correcto</li>
 *   <li>Convierte {@link TestCaseFinished} en {@code onScenarioEnd} con {@link ScenarioMetadata} correcto</li>
 *   <li>Absorbe excepciones del {@link LifecycleManager} sin propagar</li>
 * </ul>
 *
 * <p>Usa stubs manuales para evitar dependencias de frameworks de mocking,
 * consistente con el estilo de tests del proyecto.
 *
 * @author Abel Venero
 * @since 2.1.0
 */
@DisplayName("ScenarioLifecycleBridge")
class ScenarioLifecycleBridgeTest {

    // =========================================================================
    // Stubs internos
    // =========================================================================

    /**
     * {@link EventPublisher} stub que captura los handlers registrados
     * y permite disparar eventos manualmente en los tests.
     */
    private static class StubEventPublisher implements EventPublisher {

        private EventHandler<TestCaseStarted>  startedHandler;
        private EventHandler<TestCaseFinished> finishedHandler;

        @SuppressWarnings("unchecked")
        @Override
        public <T> void registerHandlerFor(Class<T> eventType, EventHandler<T> handler) {
            if (eventType == TestCaseStarted.class) {
                startedHandler = (EventHandler<TestCaseStarted>) handler;
            } else if (eventType == TestCaseFinished.class) {
                finishedHandler = (EventHandler<TestCaseFinished>) handler;
            }
        }

        @Override
        public <T> void removeHandlerFor(Class<T> eventType, EventHandler<T> handler) {
            // no-op en el stub
        }

        void fireStarted(TestCaseStarted event) {
            if (startedHandler != null) startedHandler.receive(event);
        }

        void fireFinished(TestCaseFinished event) {
            if (finishedHandler != null) finishedHandler.receive(event);
        }

        boolean hasStartedHandler()  { return startedHandler  != null; }
        boolean hasFinishedHandler() { return finishedHandler != null; }
    }

    /**
     * {@link LifecycleManager} stub que registra las llamadas recibidas.
     */
    private static class SpyLifecycleManager implements LifecycleManager {

        final List<ScenarioMetadata> startedScenarios  = new ArrayList<>();
        final List<ScenarioMetadata> finishedScenarios = new ArrayList<>();
        boolean throwOnStart = false;
        boolean throwOnEnd   = false;

        @Override
        public ExecutionContext initialize(ExecutionRequest request) {
            return ExecutionContext.builder().build();
        }

        @Override
        public void onScenarioStart(ExecutionContext context, ScenarioMetadata scenarioMetadata) {
            if (throwOnStart) throw new RuntimeException("error-start");
            startedScenarios.add(scenarioMetadata);
        }

        @Override
        public void onScenarioEnd(ExecutionContext context, ScenarioMetadata scenarioMetadata) {
            if (throwOnEnd) throw new RuntimeException("error-end");
            finishedScenarios.add(scenarioMetadata);
        }

        @Override
        public void shutdown(ExecutionContext context) { /* no-op */ }
    }

    /**
     * {@link TestCase} stub con los campos mínimos necesarios.
     */
    @SuppressWarnings("deprecation") // getLine / getScenarioDesignation: aún requeridos por TestCase (Cucumber)
    private static class StubTestCase implements TestCase {
        private final String name;
        private final List<String> tags;
        private final URI uri;
        private final int line;

        StubTestCase(String name, Collection<String> tags, String uri, int line) {
            this.name = name;
            this.tags = List.copyOf(tags);
            this.uri  = URI.create(uri);
            this.line = line;
        }

        @Override public String getName()                 { return name; }
        @Override public List<String> getTags()           { return tags; }
        @Override public URI getUri()                     { return uri; }
        @Override public Location getLocation()           { return new Location(line, 0); }
        @Override public UUID getId()                     { return UUID.randomUUID(); }
        @Override public Integer getLine()                { return line; }
        @Override public String getKeyword()              { return "Scenario"; }
        @Override public String getScenarioDesignation()  { return name; }

        @Override
        public List<io.cucumber.plugin.event.TestStep> getTestSteps() { return List.of(); }
    }

    // =========================================================================
    // Fixtures
    // =========================================================================

    private SpyLifecycleManager    spy;
    private ExecutionContext        context;
    private StubEventPublisher      publisher;
    private ScenarioLifecycleBridge bridge;

    @BeforeEach
    void setUp() {
        spy       = new SpyLifecycleManager();
        context   = new ExecutionContext(
                new ExecutionConfig.Builder().build(),
                new ServiceRegistry(),
                new VariableStore(),
                new EventBus());
        publisher = new StubEventPublisher();
        bridge    = new ScenarioLifecycleBridge(spy, context);
    }

    @AfterEach
    void tearDown() {
        ExecutionContext.deactivate();
    }

    // =========================================================================
    // Constructor
    // =========================================================================

    @Nested
    @DisplayName("Constructor")
    class ConstructorTest {

        @Test
        @DisplayName("lifecycleManager null lanza NullPointerException")
        void testNullLifecycleManager() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ScenarioLifecycleBridge(null, context));
        }

        @Test
        @DisplayName("context null lanza NullPointerException")
        void testNullContext() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ScenarioLifecycleBridge(spy, null));
        }
    }

    // =========================================================================
    // setEventPublisher
    // =========================================================================

    @Nested
    @DisplayName("setEventPublisher()")
    class SetEventPublisherTest {

        @Test
        @DisplayName("Registra handler para TestCaseStarted")
        void testRegistraHandlerStarted() {
            bridge.setEventPublisher(publisher);
            assertThat(publisher.hasStartedHandler()).isTrue();
        }

        @Test
        @DisplayName("Registra handler para TestCaseFinished")
        void testRegistraHandlerFinished() {
            bridge.setEventPublisher(publisher);
            assertThat(publisher.hasFinishedHandler()).isTrue();
        }
    }

    // =========================================================================
    // onTestCaseStarted
    // =========================================================================

    @Nested
    @DisplayName("TestCaseStarted → onScenarioStart")
    class TestCaseStartedTest {

        @BeforeEach
        void registrar() {
            bridge.setEventPublisher(publisher);
        }

        @Test
        @DisplayName("Llama onScenarioStart con metadata correcta")
        void testLlamaOnScenarioStart() {
            TestCase tc = new StubTestCase(
                    "Login exitoso",
                    List.of("@api", "@smoke"),
                    "classpath:features/api/login.feature",
                    10);

            publisher.fireStarted(new TestCaseStarted(Instant.now(), tc));

            assertThat(spy.startedScenarios).hasSize(1);
            ScenarioMetadata meta = spy.startedScenarios.get(0);
            assertThat(meta.name()).isEqualTo("Login exitoso");
            assertThat(meta.tags()).containsExactlyInAnyOrder("@api", "@smoke");
            assertThat(meta.uri()).isEqualTo("classpath:features/api/login.feature");
            assertThat(meta.line()).isEqualTo(10);
        }

        @Test
        @DisplayName("No llama onScenarioEnd al recibir TestCaseStarted")
        void testNoLlamaEnd() {
            TestCase tc = new StubTestCase("test", List.of("@api"),
                    "classpath:features/test.feature", 1);
            publisher.fireStarted(new TestCaseStarted(Instant.now(), tc));

            assertThat(spy.finishedScenarios).isEmpty();
        }

        @Test
        @DisplayName("Escenario sin tags genera metadata con tags vacíos")
        void testSinTags() {
            TestCase tc = new StubTestCase("sin-tags", List.of(),
                    "classpath:features/test.feature", 1);
            publisher.fireStarted(new TestCaseStarted(Instant.now(), tc));

            assertThat(spy.startedScenarios).hasSize(1);
            assertThat(spy.startedScenarios.get(0).tags()).isEmpty();
        }

        @Test
        @DisplayName("Excepción en LifecycleManager no se propaga")
        void testExcepcionNoSePropaga() {
            spy.throwOnStart = true;
            TestCase tc = new StubTestCase("test-error", List.of("@api"),
                    "classpath:features/test.feature", 1);

            // No debe lanzar excepción
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> publisher.fireStarted(new TestCaseStarted(Instant.now(), tc)));
        }

        @Test
        @DisplayName("Múltiples escenarios son notificados en orden")
        void testMultiplesEscenarios() {
            publisher.fireStarted(new TestCaseStarted(Instant.now(),
                    new StubTestCase("escenario-1", List.of("@api"),
                            "classpath:features/t.feature", 1)));
            publisher.fireStarted(new TestCaseStarted(Instant.now(),
                    new StubTestCase("escenario-2", List.of("@web"),
                            "classpath:features/t.feature", 5)));

            assertThat(spy.startedScenarios).hasSize(2);
            assertThat(spy.startedScenarios.get(0).name()).isEqualTo("escenario-1");
            assertThat(spy.startedScenarios.get(1).name()).isEqualTo("escenario-2");
        }
    }

    // =========================================================================
    // onTestCaseFinished
    // =========================================================================

    @Nested
    @DisplayName("TestCaseFinished → onScenarioEnd")
    class TestCaseFinishedTest {

        @BeforeEach
        void registrar() {
            bridge.setEventPublisher(publisher);
        }

        @Test
        @DisplayName("Llama onScenarioEnd con metadata correcta")
        void testLlamaOnScenarioEnd() {
            TestCase tc = new StubTestCase(
                    "Checkout exitoso",
                    List.of("@web", "@regression"),
                    "classpath:features/web/checkout.feature",
                    20);
            Result result = new Result(Status.PASSED, Duration.ofMillis(100), null);

            publisher.fireFinished(new TestCaseFinished(Instant.now(), tc, result));

            assertThat(spy.finishedScenarios).hasSize(1);
            ScenarioMetadata meta = spy.finishedScenarios.get(0);
            assertThat(meta.name()).isEqualTo("Checkout exitoso");
            assertThat(meta.tags()).containsExactlyInAnyOrder("@web", "@regression");
            assertThat(meta.uri()).isEqualTo("classpath:features/web/checkout.feature");
            assertThat(meta.line()).isEqualTo(20);
        }

        @Test
        @DisplayName("No llama onScenarioStart al recibir TestCaseFinished")
        void testNoLlamaStart() {
            TestCase tc = new StubTestCase("test", List.of("@api"),
                    "classpath:features/test.feature", 1);
            Result result = new Result(Status.FAILED, Duration.ZERO,
                    new AssertionError("fallo esperado"));

            publisher.fireFinished(new TestCaseFinished(Instant.now(), tc, result));

            assertThat(spy.startedScenarios).isEmpty();
        }

        @Test
        @DisplayName("Excepción en LifecycleManager no se propaga")
        void testExcepcionNoSePropaga() {
            spy.throwOnEnd = true;
            TestCase tc = new StubTestCase("test-error", List.of("@api"),
                    "classpath:features/test.feature", 1);
            Result result = new Result(Status.PASSED, Duration.ZERO, null);

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> publisher.fireFinished(new TestCaseFinished(Instant.now(), tc, result)));
        }
    }

    // =========================================================================
    // Start + End simétrico por escenario
    // =========================================================================

    @Nested
    @DisplayName("Simetría Start / End por escenario")
    class SimmetriaTest {

        @Test
        @DisplayName("Por cada Started hay un Finished con la misma metadata")
        void testParCadaStartedHayFinished() {
            bridge.setEventPublisher(publisher);

            String nombre = "Escenario simétrico";
            TestCase tc = new StubTestCase(nombre, List.of("@api"),
                    "classpath:features/test.feature", 7);

            publisher.fireStarted(new TestCaseStarted(Instant.now(), tc));
            publisher.fireFinished(new TestCaseFinished(Instant.now(), tc,
                    new Result(Status.PASSED, Duration.ZERO, null)));

            assertThat(spy.startedScenarios).hasSize(1);
            assertThat(spy.finishedScenarios).hasSize(1);
            assertThat(spy.startedScenarios.get(0).name()).isEqualTo(nombre);
            assertThat(spy.finishedScenarios.get(0).name()).isEqualTo(nombre);
        }
    }
}
