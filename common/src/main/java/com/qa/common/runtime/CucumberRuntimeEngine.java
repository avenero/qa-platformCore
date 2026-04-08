package com.qa.common.runtime;

import io.cucumber.core.options.CommandlineOptionsParser;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.options.RuntimeOptionsBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
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
     *   <li>Invocar Cucumber Runtime con los argumentos construidos</li>
     *   <li>Construir y retornar {@link ExecutionResult} inmutable</li>
     *   <li>Garantizar shutdown del contexto en el bloque finally</li>
     * </ol>
     *
     * @param request solicitud de ejecucion, no null
     * @return resultado de la ejecucion, nunca null
     */
    public ExecutionResult execute(ExecutionRequest request) {
        Objects.requireNonNull(request, "request no puede ser null");
        log.info("Iniciando ejecucion BDD: features={}, glue={}, tags='{}'",
                request.getFeaturePaths().size(),
                request.getGluePaths().size(),
                request.getConfig().getTags());

        ExecutionContext context = lifecycleManager.initialize(request);
        InMemoryResultCollector collector = new InMemoryResultCollector();

        try {
            byte exitStatus = runCucumber(request, collector);
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
     * <p>Metodo protegido para permitir override en tests.
     *
     * @param request   solicitud de ejecucion
     * @param collector listener que captura resultados en memoria
     * @return exit status de Cucumber (0 = exito, != 0 = fallo)
     */
    protected byte runCucumber(ExecutionRequest request, InMemoryResultCollector collector) {
        try {
            List<String> args = buildCucumberArgs(request);
            log.debug("Cucumber args: {}", args);

            CommandlineOptionsParser parser = new CommandlineOptionsParser(new ByteArrayOutputStream());
            RuntimeOptionsBuilder optionsBuilder = parser.parse(args.toArray(new String[0]));
            RuntimeOptions runtimeOptions = optionsBuilder.build();

            io.cucumber.core.runtime.Runtime runtime = io.cucumber.core.runtime.Runtime.builder()
                    .withRuntimeOptions(runtimeOptions)
                    .withAdditionalPlugins(collector)
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
     *   <li>--glue paths (uno por cada glue path)</li>
     *   <li>--tags (si estan definidos en la configuracion)</li>
     *   <li>--monochrome (salida sin color para logs estructurados)</li>
     *   <li>feature paths al final (posicionales, sin flag)</li>
     * </ol>
     *
     * @param request solicitud de ejecucion
     * @return lista de argumentos lista para parsear
     */
    private List<String> buildCucumberArgs(ExecutionRequest request) {
        List<String> args = new ArrayList<>();

        // Glue paths: paquetes donde Cucumber busca step definitions y hooks
        for (String glue : request.getGluePaths()) {
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
