package com.qa.common.internal.reporting;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.qa.common.api.reporter.bridge.ExecutionData;
import com.qa.common.api.reporter.bridge.ScenarioData;
import com.qa.common.api.reporter.bridge.StepData;
import com.qa.common.api.reporter.model.TestStatus;
import com.qa.common.internal.reporting.config.MutableReportingConfig;
import com.qa.common.internal.reporting.manager.pipeline.PipelineResult;
import com.qa.common.internal.reporting.manager.pipeline.ReportingPipeline;
import com.qa.common.internal.reporting.manager.pipeline.steps.ConversionStep;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Safety net for W2-CW1 (reporting model swap TestExecutionResult to ExecutionData).
 *
 * <p>Runs the reporting pipeline over a hand-crafted Cucumber fixture, projects the
 * in-memory report model down to ONLY its semantic fields (scenario/step names,
 * statuses, scenario totals), normalizes volatile values, and compares against a
 * committed golden. The projection is model-agnostic on purpose: W2-CW1 re-pointed
 * {@link #canonicalProjection} from the legacy {@code TestExecutionResult} to
 * {@link ExecutionData} and the golden did NOT change — proving the SPI flip is
 * data-equivalent. A diff against the golden is therefore an equivalence failure,
 * not an expected churn.
 *
 * <p>The pipeline emits HTML (not JSON) and has no {@code snapshot()} accessor, so the
 * harness serializes its own canonical projection rather than capturing pipeline output.
 */
class PipelineSnapshotTest {

    private static final String FIXTURE = "fixtures/3-scenarios-cucumber.json";
    private static final String GOLDEN = "golden/pipeline-3-scenarios.json";

    /** ISO-8601 instant prefix (date + time-of-day); volatile across runs. */
    private static final String ISO_8601_REGEX = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}";
    private static final String UUID_REGEX =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private static final ObjectWriter CANONICAL_WRITER = canonicalWriter();

    @Test
    void pipelineCanonicalProjectionMatchesGolden() throws Exception {
        String rawResults = readResource(FIXTURE);

        PipelineResult result = new ReportingPipeline.Builder()
                .withConfig(new MutableReportingConfig())
                .addStep(new ConversionStep())
                .build()
                .execute(rawResults);

        assertThat(result.isSuccess()).as("pipeline conversion succeeds").isTrue();
        ExecutionData model = result.getContext().getExecutionData();
        assertThat(model).as("conversion populates the report model").isNotNull();

        String actual = normalize(canonicalProjection(model));

        String expected = readResource(GOLDEN);
        if (expected == null) {
            fail("Golden '" + GOLDEN + "' missing. Create src/test/resources/" + GOLDEN
                    + " with exactly this content:\n>>>>>\n" + actual + "\n<<<<<");
        }
        assertThat(actual.strip()).isEqualTo(expected.strip());
    }

    /**
     * Projects the report model down to its semantic invariants only:
     * {@code {scenarios:[{name,status,steps:[{name,status}]}], totals:{passed,failed,skipped}}}.
     * No timestamps, IDs, durations or evidence — those are model-specific and would
     * break the CW1 equivalence the golden is meant to prove.
     */
    private static Map<String, Object> canonicalProjection(ExecutionData model) {
        List<Object> scenarios = new ArrayList<>();
        int passed = 0;
        int failed = 0;
        int skipped = 0;

        for (ScenarioData scenario : model.scenarios()) {
            List<Object> steps = new ArrayList<>();
            for (StepData step : scenario.steps()) {
                Map<String, Object> stepNode = new LinkedHashMap<>();
                stepNode.put("name", step.name());
                stepNode.put("status", statusName(step.status()));
                steps.add(stepNode);
            }

            Map<String, Object> scenarioNode = new LinkedHashMap<>();
            scenarioNode.put("name", scenario.scenarioName());
            scenarioNode.put("status", statusName(scenario.status()));
            scenarioNode.put("steps", steps);
            scenarios.add(scenarioNode);

            if (scenario.status() == TestStatus.PASS) {
                passed++;
            } else if (scenario.status() == TestStatus.FAIL) {
                failed++;
            } else if (scenario.status() == TestStatus.SKIP) {
                skipped++;
            }
        }

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("passed", passed);
        totals.put("failed", failed);
        totals.put("skipped", skipped);

        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("scenarios", scenarios);
        projection.put("totals", totals);
        return projection;
    }

    private static String statusName(TestStatus status) {
        return status != null ? status.name() : null;
    }

    /**
     * Serializes the projection with object keys sorted lexicographically and array
     * order preserved, then scrubs volatile ISO-8601 instants and UUIDs to fixed
     * tokens so the comparison is stable across runs.
     */
    private static String normalize(Map<String, Object> projection) throws Exception {
        String json = CANONICAL_WRITER.writeValueAsString(projection);
        return json
                .replaceAll(ISO_8601_REGEX, "T_FIXED")
                .replaceAll(UUID_REGEX, "UUID_FIXED");
    }

    private static ObjectWriter canonicalWriter() {
        ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(SerializationFeature.INDENT_OUTPUT);
        // Pin the line separator so the golden is identical on every platform.
        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
                .withObjectIndenter(indenter)
                .withArrayIndenter(indenter);
        return mapper.writer(printer);
    }

    private static String readResource(String path) throws Exception {
        try (InputStream in = PipelineSnapshotTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
