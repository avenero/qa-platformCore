package com.qa.webcore.catalog;

import com.qa.common.api.runtime.StepDefinitionInfo;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.stepcatalog.ParamSpec;
import com.qa.common.api.stepcatalog.StepCatalogEntry;
import com.qa.common.internal.runtime.StepDiscoveryService;
import com.qa.webcore.plugin.WebPlugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * W2-DS3 — contrato del sucesor {@code "web.drag.drop"} / {@code "web.drag.resize"}.
 *
 * <p>Cubre los dos mundos del catálogo:
 * <ul>
 *   <li><b>Mundo #1 (i18n):</b> {@link WebStepCatalogProvider#entries()} expone ambas entries
 *       con su patrón EN, fase, categoría y param-hints.</li>
 *   <li><b>Mundo #2 (reflexión):</b> {@link StepDiscoveryService#discoverAllStepDefs()} registra
 *       cada id exactamente una vez (sin doble-registración) y el alias deprecado
 *       {@code "web.dragdrop"} no aporta steps (marcador), pero sigue resoluble como componente.</li>
 * </ul>
 */
class WebDragDropCatalogTest {

    private final WebStepCatalogProvider provider = new WebStepCatalogProvider();
    private final StepDiscoveryService discovery = new StepDiscoveryService(List.of(new WebPlugin()));

    // ── Mundo #1: i18n catalog provider ──────────────────────────────────────

    @Test
    @DisplayName("provider.entries() expone web.drag.drop con patrón EN y params source/target")
    void providerExposesDragDropEntry() {
        StepCatalogEntry drag = entryById("web.drag.drop");

        assertThat(drag.pattern()).isEqualTo("I drag {string} onto {string}");
        assertThat(drag.phase()).isEqualTo("WHEN");
        assertThat(drag.categoryKey()).isEqualTo("step.web.category.interaction");
        assertThat(drag.descriptionKey()).isEqualTo("step.web.drag.drop.description");
        assertThat(drag.params()).extracting(ParamSpec::name)
                .containsExactly("source", "target");
        assertThat(drag.params()).extracting(ParamSpec::hintKey)
                .containsExactly("step.web.drag.drop.param.source", "step.web.drag.drop.param.target");
    }

    @Test
    @DisplayName("provider.entries() expone web.drag.resize con params target/width/height")
    void providerExposesResizeEntry() {
        StepCatalogEntry resize = entryById("web.drag.resize");

        assertThat(resize.pattern()).isEqualTo("I resize {string} to width {int} and height {int}");
        assertThat(resize.phase()).isEqualTo("WHEN");
        assertThat(resize.categoryKey()).isEqualTo("step.web.category.interaction");
        assertThat(resize.descriptionKey()).isEqualTo("step.web.drag.resize.description");
        assertThat(resize.params()).extracting(ParamSpec::name)
                .containsExactly("target", "width", "height");
    }

    // ── Mundo #2: reflective step discovery ──────────────────────────────────

    @Test
    @DisplayName("ningún stepDefId se registra dos veces en el plugin Web (sin doble-registración)")
    void discoveryHasNoDuplicateStepDefIds() {
        Map<String, Long> countById = discovery.discoverAllStepDefs().stream()
                .collect(Collectors.groupingBy(StepDefinitionInfo::stepDefId, Collectors.counting()));

        List<String> duplicates = countById.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();

        assertThat(duplicates)
                .as("stepDefIds registrados más de una vez en WebPlugin")
                .isEmpty();
    }

    @Test
    @DisplayName("web.drag.drop y web.drag.resize aparecen una vez y NO están deprecados")
    void successorStepsRegisteredOnceAndNotDeprecated() {
        List<StepDefinitionInfo> defs = discovery.discoverAllStepDefs();

        StepDefinitionInfo drag = stepDef(defs, "web.drag.drop");
        StepDefinitionInfo resize = stepDef(defs, "web.drag.resize");

        assertThat(drag.componentId()).isEqualTo("web.drag.drop");
        assertThat(drag.deprecated()).isFalse();
        assertThat(resize.componentId()).isEqualTo("web.drag.drop");
        assertThat(resize.deprecated()).isFalse();
    }

    @Test
    @DisplayName("el alias deprecado web.dragdrop no aporta steps pero sigue resoluble como componente")
    void deprecatedAliasIsMarkerOnly() {
        boolean anyFromAlias = discovery.discoverAllStepDefs().stream()
                .anyMatch(d -> "web.dragdrop".equals(d.componentId()));
        assertThat(anyFromAlias)
                .as("el alias marcador web.dragdrop no debe aportar stepDefs")
                .isFalse();

        StepComponent alias = component("web.dragdrop");
        assertThat(alias.isDeprecated()).isTrue();
        assertThat(alias.getReplacementStepId()).isEqualTo("web.drag.drop");

        StepComponent successor = component("web.drag.drop");
        assertThat(successor.isDeprecated()).isFalse();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private StepCatalogEntry entryById(String stepId) {
        return provider.entries().stream()
                .filter(e -> e.stepId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("entry ausente en el provider: " + stepId));
    }

    private static StepDefinitionInfo stepDef(List<StepDefinitionInfo> defs, String stepDefId) {
        return defs.stream()
                .filter(d -> d.stepDefId().equals(stepDefId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("stepDef ausente en el catálogo: " + stepDefId));
    }

    private StepComponent component(String componentId) {
        return discovery.discoverAll().stream()
                .map(StepDiscoveryService.ComponentInfo::component)
                .filter(c -> c.getId().equals(componentId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("componente ausente: " + componentId));
    }
}
