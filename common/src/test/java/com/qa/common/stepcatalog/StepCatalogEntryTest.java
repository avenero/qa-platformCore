package com.qa.common.stepcatalog;


import com.qa.common.api.stepcatalog.ParamSpec;
import com.qa.common.api.stepcatalog.StepCatalogEntry;
import com.qa.common.api.stepcatalog.StepLayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class StepCatalogEntryTest {

    @Test
    void constructor_setsAllFields() {
        var params = List.of(new ParamSpec("locator", "step.web.click.param.locator"));
        var entry = new StepCatalogEntry(
            "web.click.click",
            "I click {string}",
            StepLayer.WEB,
            "WHEN",
            "step.web.category.interaction",
            "step.web.click.click.description",
            params
        );

        assertThat(entry.stepId()).isEqualTo("web.click.click");
        assertThat(entry.pattern()).isEqualTo("I click {string}");
        assertThat(entry.layer()).isEqualTo(StepLayer.WEB);
        assertThat(entry.phase()).isEqualTo("WHEN");
        assertThat(entry.params()).hasSize(1);
        assertThat(entry.hasParams()).isTrue();
    }

    @Test
    void constructor_nullParams_becomesEmptyList() {
        var entry = new StepCatalogEntry(
            "web.scroll.down", "I scroll down", StepLayer.WEB, "WHEN",
            "step.web.category.interaction", "step.web.scroll.down.description",
            null
        );
        assertThat(entry.params()).isEmpty();
        assertThat(entry.hasParams()).isFalse();
    }

    @Test
    void constructor_nullStepId_throwsNPE() {
        assertThatNullPointerException().isThrownBy(() ->
            new StepCatalogEntry(null, "I click {string}", StepLayer.WEB, "WHEN",
                "cat", "desc", List.of())
        );
    }

    @Test
    void params_areImmutable() {
        var entry = new StepCatalogEntry(
            "id", "pattern", StepLayer.HTTP, "WHEN", "cat", "desc",
            List.of(new ParamSpec("name", "key"))
        );
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> entry.params().add(new ParamSpec("x", "y")));
    }
}
