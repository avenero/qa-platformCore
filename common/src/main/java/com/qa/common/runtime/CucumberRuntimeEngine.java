package com.qa.common.runtime;

import io.cucumber.core.options.CommandlineOptionsParser;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.options.RuntimeOptionsBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cucumber.plugin.ConcurrentEventListener;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Motor de ejecucion BDD. Entry point para el Backend.
 *
 * <p>Stateless: cada llamada a {@link #execute(ExecutionRequest)} crea su propio
 * {@link ExecutionContext}, ejecuta los features via Cucumber Runtime, y retorna
 * un {@link ExecutionResult} inmutable.
 *
 * <p>Uso tipico (via SPI):
 * <pre>
 *   CucumberRuntimeEngine engine = CucumberRuntimeEngine.withServiceLoader();
 *   ExecutionRequest request = ExecutionRequest.of(featurePaths, gluePaths, config);
 *   ExecutionResult result = engine.execute(request);
 * </pre>
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public class CucumberRuntimeEngine {

    private static final Logger log = LoggerFactory.getLogger(CucumberRuntimeEngine.class);

    private final LifecycleManager lifecycleManager;
    private final StepDiscoveryService discoveryService;

    /**
     * Constructor principal. Recibe dependencias ya construidas.
     *
     * @param lifecycleManager coordinador del ciclo de vida
     * @param discoveryService servicio de descubrimiento de steps
     */
    public CucumberRuntimeEngine(LifecycleManager lifecycleManager, StepDiscoveryService discoveryService) {
        this.lifecycleManager = Objects.requireNonNull(lifecycleManager, "lifecycleManager no puede ser null");
        this.discoveryService = Objects.requireNonNull(discoveryService, "discoveryService no puede ser null");
        log.info("CucumberRuntimeEngine inicializado con {} plugins y {} componentes",
                discoveryService.getPlugins().size(), discoveryService.totalComponents());
    }

    /**
     * Factory method que descubre plugins via Java SPI (ServiceLoader).
     *
     * <p>Metodo de entrada recomendado para produccion y para el Backend
     * cuando invoca el Core como runtime autonomo.
     *
     * @return instancia lista para ejecutar
     */
    public static CucumberRuntimeEngine withServiceLoader() {
        List<CorePlugin> plugins = StreamSupport
                .stream(ServiceLoader.load(CorePlugin.class).spliterator(), false)
                .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
                .collect(Collectors.toList());
        log.info("Plugins descubiertos via SPI: {}", plugins.size());
        plugins.forEach(p -> log.info("  Plugin cargado: {} (order={})", p.getName(), p.getOrder()));

        LifecycleManager lifecycleManager = new DefaultLifecycleManager(plugins);
        StepDiscoveryService discoveryService = new StepDiscoveryService(plugins);
        return new CucumberRuntimeEngine(lifecycleManager, discoveryService);
    }

    /**
     * Ejecuta una suite BDD completa dado un {@link ExecutionRequest}.
     *
     * <p>El flujo es:
     * <ol>
     *   <li>Inicializar {@link ExecutionContext} via {@link LifecycleManager}</li>
     *   <li>Crear un {@link InMemoryResultCollector} para capturar resultados</li>
     *   <li>Crear un {@link ScenarioLifecycleBridge} que notifica a plugins por escenario</li>
     *   <li>Invocar Cucumber Runtime con ambos plugins adicionales</li>
     *   <li>Construir y retornar {@link ExecutionResult} inmutable</li>
     *   <li>Garantizar shutdown del contexto en el bloque finally</li>
     * </ol>
     *
     * @param request solicitud de ejecucion, no null
     * @return resultado de la ejecucion, nunca null
     */
    public ExecutionResult execute(ExecutionRequest request) {
        return execute(request, List.of());
    }

    /**
     * Ejecuta una suite BDD con listeners adicionales de Cucumber (ej: step-level events).
     *
     * <p>Variante que permite al Backend (u otro caller) inyectar
     * {@link ConcurrentEventListener} adicionales que reciben eventos por paso y escenario
     * en tiempo real — sin necesidad de subclasificar el engine.
     *
     * <p>Ejemplo de uso desde el Backend:
     * <pre>
     *   ConcurrentEventListener stepBridge = new StepEventBridge(webSocketCallback);
     *   ExecutionResult result = engine.execute(request, List.of(stepBridge));
     * </pre>
     *
     * @param request             solicitud de ejecucion, no null
     * @param additionalListeners listeners Cucumber extras (pueden capturar TestStepFinished, etc.)
     * @return resultado de la ejecucion, nunca null
     * @since 2.1.0
     */
    public ExecutionResult execute(ExecutionRequest request,
                                   List<ConcurrentEventListener> additionalListeners) {
        Objects.requireNonNull(request, "request no puede ser null");
        Objects.requireNonNull(additionalListeners, "additionalListeners no puede ser null");

        log.info("Iniciando ejecucion BDD: features={}, glue={}, tags='{}', extraListeners={}",
                request.getFeaturePaths().size(),
                request.isGlueAutoResolved() ? "auto(SPI)" : request.getGluePaths().size(),
                request.getConfig().getTags(),
                additionalListeners.size());

        ExecutionContext context = lifecycleManager.initialize(request);
        InMemoryResultCollector collector = new InMemoryResultCollector();

        try {
            byte exitStatus = runCucumber(request, context, collector, additionalListeners);
            log.info("Cucumber Runtime finalizado con exit status: {}", exitStatus);

            ExecutionResult result = collector.buildResult();

            // Si Cucumber reporto fallo pero el collector no registra escenarios,
            // es un error de configuracion (glue no encontrado, feature path invalido, etc.)
            if (exitStatus != 0 && result.isSuccess() && result.getTotalScenarios() == 0) {
                result = new ExecutionResult.Builder()
                        .status(ExecutionResult.Status.ERROR)
                        .errors(List.of("Cucumber exit status != 0 sin escenarios ejecutados. " +
                                "Verificar featurePaths y gluePaths. Exit code: " + exitStatus))
                        .build();
            }

            return result;

        } catch (Exception e) {
            log.error("Error fatal durante ejecucion BDD: {}", e.getMessage(), e);
            return new ExecutionResult.Builder()
                    .status(ExecutionResult.Status.ERROR)
                    .errors(List.of("Error fatal: " + e.getMessage()))
                    .build();
        } finally {
            lifecycleManager.shutdown(context);
        }
    }

    /**
     * Invoca el Cucumber Runtime con los argumentos construidos.
     *
     * <p>Registra plugins adicionales:
     * <ol>
     *   <li>{@link InMemoryResultCollector} — captura resultados de la ejecución</li>
     *   <li>{@link ScenarioLifecycleBridge} — conecta eventos Cucumber con el ciclo de vida
     *       de plugins ({@code onScenarioStart}/{@code onScenarioEnd} por escenario)</li>
     *   <li>Listeners adicionales inyectados por el caller (ej: step-level event bridge)</li>
     * </ol>
     *
     * <p><b>Nota para subclases:</b> este método es {@code protected} para permitir
     * override en tests de integración. Si se sobreescribe, asegurarse de instanciar
     * el {@link ScenarioLifecycleBridge} o de invocar los callbacks manualmente.
     *
     * @param request             solicitud de ejecucion
     * @param context             contexto activo (activado en ThreadLocal por {@code initialize()})
     * @param collector           listener que captura resultados en memoria
     * @param additionalListeners listeners Cucumber extras inyectados por el caller
     * @return exit status de Cucumber (0 = exito, != 0 = fallo)
     */
    protected byte runCucumber(ExecutionRequest request,
                               ExecutionContext context,
                               InMemoryResultCollector collector,
                               List<ConcurrentEventListener> additionalListeners) {
        try {
            List<String> args = buildCucumberArgs(request);
            log.debug("Cucumber args: {}", args);

            CommandlineOptionsParser parser = new CommandlineOptionsParser(new ByteArrayOutputStream());
            RuntimeOptionsBuilder optionsBuilder = parser.parse(args.toArray(new String[0]));
            RuntimeOptions runtimeOptions = optionsBuilder.build();

            // Bridge que notifica onScenarioStart/onScenarioEnd a los plugins
            // por cada TestCaseStarted/TestCaseFinished que Cucumber emite.
            ScenarioLifecycleBridge lifecycleBridge =
                    new ScenarioLifecycleBridge(lifecycleManager, context);

            // Combinar plugins base + listeners adicionales del caller
            Object[] allPlugins = Stream.concat(
                    Stream.of(collector, lifecycleBridge),
                    additionalListeners.stream()
            ).toArray();

            io.cucumber.core.runtime.Runtime runtime = io.cucumber.core.runtime.Runtime.builder()
                    .withRuntimeOptions(runtimeOptions)
                    .withAdditionalPlugins(allPlugins)
                    .withClassLoader(() -> Thread.currentThread().getContextClassLoader())
                    .build();

            runtime.run();
            return runtime.exitStatus();

        } catch (Exception e) {
            log.error("Error en Cucumber Runtime: {}", e.getMessage(), e);
            throw new RuntimeException("Error en Cucumber Runtime: " + e.getMessage(), e);
        }
    }

    /**
     * Construye la lista de argumentos para el CommandlineOptionsParser de Cucumber.
     *
     * <p>Orden:
     * <ol>
     *   <li>--glue paths — derivados de plugins SPI si el request no los especifica explícitamente</li>
     *   <li>--tags — si están definidos en la configuración</li>
     *   <li>--monochrome — salida sin color para logs estructurados</li>
     *   <li>feature paths al final (posicionales, sin flag)</li>
     * </ol>
     *
     * @param request solicitud de ejecucion
     * @return lista de argumentos lista para parsear
     */
    private List<String> buildCucumberArgs(ExecutionRequest request) {
        List<String> args = new ArrayList<>();

        // Glue paths: paquetes donde Cucumber busca step definitions y hooks.
        // Se usan los explícitos del request si los hay; si no, se derivan de los plugins SPI.
        for (String glue : resolveGluePaths(request)) {
            args.add("--glue");
            args.add(glue);
        }

        // Tags: expresion de tags Cucumber (ej: "@api and @smoke")
        String tags = request.getConfig().getTags();
        if (tags != null && !tags.isBlank()) {
            args.add("--tags");
            args.add(tags);
        }

        // Salida monocromatica para compatibilidad con logs estructurados (Logback)
        args.add("--monochrome");

        // Feature paths: rutas a archivos .feature o directorios (argumentos posicionales)
        args.addAll(request.getFeaturePaths());

        return args;
    }

    /**
     * Resuelve los paquetes de glue para una ejecución.
     *
     * <p>Estrategia con prioridad:
     * <ol>
     *   <li>Si el request provee gluePaths explícitos → se usan tal cual (override del caller).</li>
     *   <li>Si el request tiene gluePaths vacío → se derivan automáticamente de todos los plugins
     *       descubiertos vía SPI, invocando {@link CorePlugin#getGluePackages()} en cada uno.</li>
     * </ol>
     *
     * <p>La derivación automática garantiza que el Backend no necesita conocer los paquetes
     * de steps de cada módulo. Añadir un nuevo plugin al classpath es suficiente.
     *
     * @param request solicitud de ejecucion
     * @return lista de paquetes Java (sin duplicados, ordenada), nunca null ni vacía si hay plugins
     * @since 2.2.0
     */
    List<String> resolveGluePaths(ExecutionRequest request) {
        if (!request.isGlueAutoResolved()) {
            log.debug("[SPI] Usando glue paths explícitos del request ({}): {}",
                    request.getGluePaths().size(), request.getGluePaths());
            return request.getGluePaths();
        }

        List<String> derived = discoveryService.resolveGluePaths();
        log.info("[SPI] Glue paths auto-derivados de {} plugins: {}",
                discoveryService.getPlugins().size(), derived);

        if (derived.isEmpty()) {
            log.warn("[SPI] Ningún glue path derivado de los plugins. "
                    + "Verificar que los plugins tengan componentes con getStepDefinitionClass() no nulo.");
        }
        return derived;
    }

    // --- Accessors ---

    /**
     * Retorna el servicio de descubrimiento de steps.
     * Usado por el Backend para exponer el catalogo de componentes via REST.
     *
     * @return instancia de {@link StepDiscoveryService}
     */
    public StepDiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    /**
     * Retorna el gestor del ciclo de vida.
     *
     * @return instancia de {@link LifecycleManager}
     */
    public LifecycleManager getLifecycleManager() {
        return lifecycleManager;
    }

    @Override
    public String toString() {
        return "CucumberRuntimeEngine{plugins=" + discoveryService.getPlugins().size()
                + ", components=" + discoveryService.totalComponents() + "}";
    }
}
