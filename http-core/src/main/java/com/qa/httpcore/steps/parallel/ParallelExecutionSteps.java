package com.qa.httpcore.steps.parallel;

import com.qa.httpcore.factories.HttpClientFactory;
import com.qa.httpcore.interfaces.HttpClient;
import com.qa.common.api.logging.TestLogger;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.internal.concurrency.ParallelResult;
import com.qa.common.internal.concurrency.ParallelStepExecutor;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Steps para ejecutar peticiones HTTP en paralelo dentro de un mismo escenario.
 *
 * <p>Útil para verificar:
 * <ul>
 *   <li>Comportamiento bajo carga concurrente</li>
 *   <li>Race conditions en recursos compartidos</li>
 *   <li>Tiempo total de respuesta de N peticiones paralelas</li>
 * </ul>
 *
 * <p>Flujo típico en feature:
 * <pre>
 * Given configuro ejecución paralela con 5 hilos y 30 segundos de timeout
 * When  ejecuto 5 peticiones GET concurrentes a "/api/projects"
 * Then  todas las peticiones paralelas deberían completarse exitosamente
 * And   todas las peticiones paralelas deberían completarse en menos de 3000 milisegundos
 * </pre>
 *
 * @since 2.1.0
 */
public class ParallelExecutionSteps {

    private static final TestLogger.LoggerWrapper LOG =
            TestLogger.getLogger(ParallelExecutionSteps.class);

    private ParallelStepExecutor executor;

    // =========================================================================
    // GIVEN — configuración
    // =========================================================================

    @Given("configuro ejecución paralela con {int} hilos y {int} segundos de timeout")
    public void configuroEjecucionParalela(int hilos, int timeoutSecs) {
        executor = new ParallelStepExecutor(hilos, timeoutSecs);
        LOG.info("Ejecutor paralelo configurado: hilos={}, timeoutSeg={}", hilos, timeoutSecs);
    }

    // =========================================================================
    // WHEN — ejecución
    // =========================================================================

    @When("ejecuto {int} peticiones GET concurrentes a {string}")
    public void ejecutoPeticionesGetConcurrentes(int cantidad, String url) {
        requireExecutor();
        List<Callable<Object>> tareas = buildGetTasks(cantidad, url);
        executor.execute(tareas);
    }

    @When("ejecuto {int} peticiones POST concurrentes a {string}")
    public void ejecutoPeticionesPostConcurrentes(int cantidad, String url) {
        requireExecutor();
        List<Callable<Object>> tareas = buildPostTasks(cantidad, url);
        executor.execute(tareas);
    }

    // =========================================================================
    // THEN — validación
    // =========================================================================

    @Then("todas las peticiones paralelas deberían completarse exitosamente")
    public void todasLasPeticionesParalelasDeberianCompletarseExitosamente() {
        requireExecutor();
        List<ParallelResult> resultados = executor.getLastResults();
        Assertions.assertThat(resultados).as("No hay resultados de ejecución paralela").isNotEmpty();
        long fallos = resultados.stream().filter(r -> !r.success()).count();
        Assertions.assertThat(fallos)
                .as("Se esperaba que TODAS las peticiones paralelas fueran exitosas, pero %d fallaron", fallos)
                .isZero();
    }

    @Then("todas las peticiones paralelas deberían completarse en menos de {int} milisegundos")
    public void todasLasPeticionesParalelasDeberianCompletarseEnMenosDe(int maxMs) {
        requireExecutor();
        long actualMs = executor.getLastExecutionMs();
        Assertions.assertThat(actualMs)
                .as("Se esperaba completar en menos de %dms, pero tardó %dms", maxMs, actualMs)
                .isLessThan(maxMs);
    }

    @Then("ninguna petición paralela debería hacer timeout")
    public void ningunaPeticionParalelaDeberiaHacerTimeout() {
        requireExecutor();
        long timedOut = executor.getLastResults().stream().filter(r -> r.timedOut()).count();
        Assertions.assertThat(timedOut)
                .as("Se esperaba que ninguna petición paralela hiciera timeout, pero %d lo hicieron", timedOut)
                .isZero();
    }

    @Then("al menos {int} peticiones paralelas deberían completarse exitosamente")
    public void alMenosNPeticionesParalelasDeberianCompletarseExitosamente(int minExitosas) {
        requireExecutor();
        long exitosas = executor.getLastResults().stream().filter(r -> r.success()).count();
        Assertions.assertThat(exitosas)
                .as("Se esperaba que al menos %d peticiones fueran exitosas, pero solo %d lo fueron",
                        minExitosas, exitosas)
                .isGreaterThanOrEqualTo(minExitosas);
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    private void requireExecutor() {
        if (executor == null) {
            throw new IllegalStateException(
                "ParallelStepExecutor no configurado. " +
                "Usa: 'Given configuro ejecución paralela con {int} hilos y {int} segundos de timeout'");
        }
    }

    private List<Callable<Object>> buildGetTasks(int cantidad, String url) {
        List<Callable<Object>> tareas = new ArrayList<>(cantidad);
        String resolved = resolveUrl(url);
        for (int i = 0; i < cantidad; i++) {
            final int idx = i;
            tareas.add(() -> {
                HttpClient client = HttpClientFactory.getInstanceWithHost(resolved);
                client.get("");
                LOG.debug("GET paralelo #{} a {} completado", idx, resolved);
                return client.getLastResponse();
            });
        }
        return tareas;
    }

    private List<Callable<Object>> buildPostTasks(int cantidad, String url) {
        List<Callable<Object>> tareas = new ArrayList<>(cantidad);
        String resolved = resolveUrl(url);
        for (int i = 0; i < cantidad; i++) {
            final int idx = i;
            tareas.add(() -> {
                HttpClient client = HttpClientFactory.getInstanceWithHost(resolved);
                client.post("");
                LOG.debug("POST paralelo #{} a {} completado", idx, resolved);
                return client.getLastResponse();
            });
        }
        return tareas;
    }

    private String resolveUrl(String url) {
        return ExecutionContext.current()
                .map(ctx -> ctx.variables().resolve(url))
                .orElse(url);
    }
}
