package com.qa.common.runtime;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Puente entre el sistema de eventos de Cucumber y el ciclo de vida de los plugins.
 *
 * <p>Registrado como plugin adicional en {@link CucumberRuntimeEngine#runCucumber},
 * traduce los eventos Cucumber {@link TestCaseStarted} / {@link TestCaseFinished}
 * en llamadas a {@link LifecycleManager#onScenarioStart} / {@link LifecycleManager#onScenarioEnd},
 * construyendo el {@link ScenarioMetadata} correspondiente.
 *
 * <p><b>Responsabilidad única:</b> este bridge no contiene lógica de negocio.
 * Solo extrae metadatos del {@link TestCase} de Cucumber y delega al
 * {@link LifecycleManager}, que a su vez notifica a los {@link CorePlugin} activos.
 *
 * <p><b>Thread safety:</b> implementa {@link ConcurrentEventListener} (no {@code EventListener})
 * para ser compatible con ejecución paralela de escenarios en Cucumber.
 * El {@link LifecycleManager} y sus plugins son los responsables de su propia thread-safety.
 *
 * <p><b>Manejo de errores:</b> cualquier excepción en el ciclo de vida se loggea
 * pero no aborta la ejecución del escenario. Esto es intencional: un error en
 * {@code onScenarioStart} de un plugin no debe bloquear el paso de Cucumber.
 * El plugin problemático emite un warning en log y la ejecución continúa.
 *
 * <p><b>Flujo de integración:</b>
 * <pre>
 *   Cucumber Runtime
 *       │── TestCaseStarted ──────────────────────────────────────────────────┐
 *       │                                                                      ▼
 *       │                                             ScenarioLifecycleBridge.onTestCaseStarted()
 *       │                                                     │
 *       │                                                     └──► buildMetadata(TestCase)
 *       │                                                     │        (name, tags, uri, line)
 *       │                                                     │
 *       │                                                     └──► LifecycleManager.onScenarioStart(ctx, metadata)
 *       │                                                               │
 *       │                                                               └──► CorePlugin[].onScenarioStart(ctx)
 *       │
 *       │── [Cucumber ejecuta steps] ──────────────────────────────────────────
 *       │
 *       └── TestCaseFinished ────────────────────────────────────────────────►
 *                                              ScenarioLifecycleBridge.onTestCaseFinished()
 *                                                    └──► LifecycleManager.onScenarioEnd(ctx, metadata)
 * </pre>
 *
 * @author Abel Venero
 * @since 2.1.0
 * @see CucumberRuntimeEngine
 * @see LifecycleManager
 * @see ScenarioMetadata
 */
public final class ScenarioLifecycleBridge implements ConcurrentEventListener {

    private static final Logger log = LoggerFactory.getLogger(ScenarioLifecycleBridge.class);

    private final LifecycleManager lifecycleManager;
    private final ExecutionContext  context;

    /**
     * Constructor principal.
     *
     * @param lifecycleManager coordinador del ciclo de vida, no null
     * @param context          contexto activo de la ejecución, no null
     */
    public ScenarioLifecycleBridge(LifecycleManager lifecycleManager, ExecutionContext context) {
        this.lifecycleManager = Objects.requireNonNull(lifecycleManager,
                "lifecycleManager no puede ser null");
        this.context = Objects.requireNonNull(context,
                "context no puede ser null");
    }

    // -------------------------------------------------------------------------
    // ConcurrentEventListener
    // -------------------------------------------------------------------------

    /**
     * Registra los handlers para {@link TestCaseStarted} y {@link TestCaseFinished}.
     * Invocado una sola vez por Cucumber al inicializar el plugin.
     */
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class,  this::onTestCaseStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onTestCaseFinished);
    }

    // -------------------------------------------------------------------------
    // Handlers internos
    // -------------------------------------------------------------------------

    /**
     * Invocado por Cucumber justo antes de ejecutar cada escenario.
     * Construye {@link ScenarioMetadata} y delega a {@link LifecycleManager#onScenarioStart}.
     */
    private void onTestCaseStarted(TestCaseStarted event) {
        TestCase testCase = event.getTestCase();
        ScenarioMetadata metadata = buildMetadata(testCase);
        log.debug("Escenario iniciado: '{}' [{}:{}] tags={}",
                metadata.name(), metadata.uri(), metadata.line(), metadata.tags());
        try {
            lifecycleManager.onScenarioStart(context, metadata);
        } catch (Exception e) {
            // No abortar la ejecución del escenario por un error en el lifecycle
            log.error("Error en onScenarioStart del LifecycleManager para escenario '{}': {}",
                    metadata.name(), e.getMessage(), e);
        }
    }

    /**
     * Invocado por Cucumber justo después de finalizar cada escenario.
     * Construye {@link ScenarioMetadata} y delega a {@link LifecycleManager#onScenarioEnd}.
     */
    private void onTestCaseFinished(TestCaseFinished event) {
        TestCase testCase = event.getTestCase();
        ScenarioMetadata metadata = buildMetadata(testCase);
        log.debug("Escenario finalizado: '{}' [{}:{}] resultado={}",
                metadata.name(), metadata.uri(), metadata.line(), event.getResult().getStatus());
        try {
            lifecycleManager.onScenarioEnd(context, metadata);
        } catch (Exception e) {
            // No abortar la finalización del escenario por un error en el lifecycle
            log.error("Error en onScenarioEnd del LifecycleManager para escenario '{}': {}",
                    metadata.name(), e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Construye un {@link ScenarioMetadata} a partir del {@link TestCase} de Cucumber.
     *
     * <p>Los tags de Cucumber incluyen el prefijo {@code @} por defecto.
     * Se copian a un {@link Set} inmutable para garantizar inmutabilidad
     * del record resultante.
     *
     * @param testCase test case de Cucumber, no null
     * @return metadata inmutable del escenario
     */
    private ScenarioMetadata buildMetadata(TestCase testCase) {
        Set<String> tags = testCase.getTags()
                .stream()
                .collect(Collectors.toUnmodifiableSet());

        return new ScenarioMetadata(
                testCase.getName(),
                tags,
                testCase.getUri().toString(),
                testCase.getLocation().getLine());
    }

    @Override
    public String toString() {
        return "ScenarioLifecycleBridge{lifecycleManager=" + lifecycleManager.getClass().getSimpleName() + "}";
    }
}
