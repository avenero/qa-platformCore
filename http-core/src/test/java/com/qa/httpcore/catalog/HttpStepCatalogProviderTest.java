package com.qa.httpcore.catalog;

import com.qa.common.api.stepcatalog.StepCatalogEntry;
import com.qa.common.api.stepcatalog.StepLayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del catálogo i18n HTTP (W2-DS4): verifica que el provider expone la
 * entry del sucesor canónico {@code api.request.body}.
 *
 * <p>Java puro: instancia el provider sin Spring ni drivers nativos, respetando
 * el modelo de doble-consumo (CLI sin BE / embebido).
 */
class HttpStepCatalogProviderTest {

    private final HttpStepCatalogProvider provider = new HttpStepCatalogProvider();

    private Optional<StepCatalogEntry> entry(String stepId) {
        return provider.entries().stream()
                .filter(e -> stepId.equals(e.stepId()))
                .findFirst();
    }

    @Test
    @DisplayName("el catálogo expone una entry para el sucesor api.request.body")
    void exposesRequestBodySuccessorEntry() {
        assertThat(entry("api.request.body"))
                .as("entry api.request.body presente en HttpStepCatalogProvider.entries()")
                .isPresent();
    }

    @Test
    @DisplayName("la entry api.request.body es GIVEN, capa HTTP, categoría Request Configuration")
    void requestBodyEntryHasExpectedShape() {
        StepCatalogEntry e = entry("api.request.body").orElseThrow();

        assertThat(e.layer()).isEqualTo(StepLayer.HTTP);
        assertThat(e.phase()).isEqualTo("GIVEN");
        assertThat(e.categoryKey()).isEqualTo("step.http.category.request");
        assertThat(e.descriptionKey()).isEqualTo("step.http.request-body.description");
    }

    @Test
    @DisplayName("la entry api.request.body declara el parámetro body con su hint key")
    void requestBodyEntryDeclaresBodyParam() {
        StepCatalogEntry e = entry("api.request.body").orElseThrow();

        assertThat(e.params())
                .singleElement()
                .satisfies(p -> {
                    assertThat(p.name()).isEqualTo("body");
                    assertThat(p.hintKey()).isEqualTo("step.http.request-body.param.body");
                });
    }

    @Test
    @DisplayName("los stepId del catálogo son únicos (no se duplica api.request.body)")
    void stepIdsAreUnique() {
        long distinct = provider.entries().stream()
                .map(StepCatalogEntry::stepId)
                .distinct()
                .count();
        assertThat(distinct).isEqualTo(provider.entries().size());
    }
}
