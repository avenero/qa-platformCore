package com.qa.mobileagent.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.common.driver.CapabilityDescriptor;
import com.qa.common.driver.CapabilityReport;
import com.qa.common.reporter.ScenarioOutcome;
import com.qa.common.reporter.StepReporter;
import com.qa.common.runtime.ExecutionConfig;
import com.qa.common.runtime.ExecutionResult;
import com.qa.common.transport.ExecutionHandle;
import com.qa.common.transport.ExecutionTransport;
import com.qa.mobileagent.api.dto.SubmitRequest;
import com.qa.mobileagent.api.dto.SubmitResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests E2E del wire-protocol v1 con un {@link ExecutionTransport} fake — no
 * arranca Cucumber real. Verifica los 4 endpoints + flujo SSE end-to-end.
 */
@SpringBootTest
@Import(AgentEndpointsIT.TestConfig.class)
class AgentEndpointsIT {

    @Autowired
    WebApplicationContext ctx;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    FakeTransport fake;

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    @Test
    void capabilitiesReturnsTransportCapabilities() throws Exception {
        mvc().perform(get("/v1/capabilities"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].platformId").value("MOBILE"));
    }

    @Test
    void submitRunReturnsAcceptedAndExecutionId() throws Exception {
        SubmitRequest req = sampleRequest();
        MvcResult result = mvc().perform(post("/v1/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.executionId").exists())
                .andExpect(jsonPath("$.eventsUrl").exists())
                .andReturn();

        SubmitResponse resp = mapper.readValue(result.getResponse().getContentAsString(), SubmitResponse.class);
        assertThat(resp.eventsUrl()).isEqualTo("/v1/runs/" + resp.executionId() + "/events");
    }

    @Test
    void cancelUnknownExecutionReturns404() throws Exception {
        mvc().perform(post("/v1/runs/" + UUID.randomUUID() + "/cancel"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eventsUnknownExecutionReturns404() throws Exception {
        mvc().perform(get("/v1/runs/" + UUID.randomUUID() + "/events"))
                .andExpect(status().isNotFound());
    }

    @Test
    void fullFlowSubmitThenStreamEventsThenComplete() throws Exception {
        SubmitRequest req = sampleRequest();
        // 1. submit
        MvcResult submit = mvc().perform(post("/v1/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(req)))
                .andExpect(status().isAccepted())
                .andReturn();
        String execId = mapper.readValue(submit.getResponse().getContentAsString(), SubmitResponse.class).executionId();

        // 2. dispara eventos en el reporter del fake (en un hilo separado)
        Executors.newSingleThreadExecutor().submit(() -> {
            StepReporter rep = fake.lastReporter();
            // espera mínima a que el SSE controller suscriba — el drain consume desde la cola del reporter,
            // así que podemos publicar antes y los eventos se entregarán igual.
            rep.onScenarioStarted(execId, "Login OK");
            rep.onStepStarted(execId, "Given user");
            rep.onStepPassed(execId, "Given user", Duration.ofMillis(50));
            rep.onScenarioCompleted(execId, ScenarioOutcome.PASSED, Duration.ofMillis(80));
            rep.onExecutionCompleted(new ExecutionResult.Builder()
                    .status(ExecutionResult.Status.PASSED)
                    .totalScenarios(1).passedScenarios(1).failedScenarios(0)
                    .duration(Duration.ofMillis(100))
                    .startTime(Instant.now()).endTime(Instant.now())
                    .build());
            fake.complete();
        });

        // 3. lee SSE — MockMvc + async dispatch
        MvcResult async = mvc().perform(get("/v1/runs/" + execId + "/events"))
                .andExpect(request().asyncStarted())
                .andReturn();

        // espera a que el async termine (cuando emitter.complete() se llama)
        async.getAsyncResult(TimeUnit.SECONDS.toMillis(10));
        MvcResult done = mvc().perform(asyncDispatch(async))
                .andExpect(status().isOk())
                .andReturn();

        String body = done.getResponse().getContentAsString();
        assertThat(body)
                .contains("SCENARIO_STARTED")
                .contains("STEP_PASSED")
                .contains("EXECUTION_COMPLETED");
    }

    // -------------------------------------------------------------------------

    private static SubmitRequest sampleRequest() {
        return new SubmitRequest(
                "qa", "", "@smoke", false, 1, "PLAYWRIGHT", false,
                Map.of("mobile.platform", "android"),
                Map.of("login.feature", "Feature: Login\n  Scenario: ok\n    Given user\n"));
    }

    /**
     * Bean reemplazable por test config. Los IT lo inyectan vía
     * {@link FakeTransport} primary.
     */
    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        FakeTransport fakeTransport() {
            return new FakeTransport();
        }
    }

    /**
     * Transport de test: completa el future cuando se llama {@link #complete()}
     * desde el test. Captura el reporter del último submit.
     */
    static class FakeTransport implements ExecutionTransport {

        private volatile StepReporter lastReporter;
        private volatile CompletableFuture<ExecutionResult> currentFuture;

        @Override
        public ExecutionHandle submit(ExecutionConfig config, List<String> featurePaths, StepReporter reporter) {
            this.lastReporter = reporter;
            CompletableFuture<ExecutionResult> f = new CompletableFuture<>();
            this.currentFuture = f;
            return new ExecutionHandle(UUID.randomUUID().toString(), f, () -> f.cancel(true));
        }

        @Override
        public List<CapabilityReport> describeCapabilities() {
            return List.of(CapabilityReport.available("MOBILE",
                    List.of(new CapabilityDescriptor("emulator-5554", "Pixel 5", "Android API 33"))));
        }

        StepReporter lastReporter() { return lastReporter; }

        void complete() {
            currentFuture.complete(new ExecutionResult.Builder()
                    .status(ExecutionResult.Status.PASSED)
                    .totalScenarios(1).passedScenarios(1)
                    .duration(Duration.ofMillis(100))
                    .startTime(Instant.now()).endTime(Instant.now())
                    .build());
        }
    }
}
