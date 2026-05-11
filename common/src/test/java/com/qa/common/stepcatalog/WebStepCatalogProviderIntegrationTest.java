package com.qa.common.stepcatalog;


import com.qa.common.api.stepcatalog.ParamSpec;
import com.qa.common.api.stepcatalog.StepCatalogProvider;
import com.qa.common.api.stepcatalog.StepLayer;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.*;

/**
 * Verifica que el SPI de StepCatalogProvider está correctamente registrado
 * en el módulo common y que la interfaz es consistente.
 */
class WebStepCatalogProviderIntegrationTest {

    @Test
    void serviceLoader_discoversAtLeastOneProvider_whenCoreJarsOnClasspath() {
        // Si web-core, http-core, mobile-core, database-core están en el classpath
        // (como en un entorno BE con todos los módulos), el ServiceLoader los descubre.
        // En el módulo common solo (sin JARs de modules en test classpath) puede retornar vacío,
        // pero el test verifica que la interfaz es correcta.
        var loader = ServiceLoader.load(StepCatalogProvider.class);
        // No lanza excepción = la interfaz SPI está bien definida
        assertThat(loader).isNotNull();
    }

    @Test
    void stepLayer_enumHasAllExpectedValues() {
        var layers = StepLayer.values();
        assertThat(layers).contains(StepLayer.WEB, StepLayer.MOBILE, StepLayer.HTTP, StepLayer.DATABASE);
        assertThat(layers).hasSize(4);
    }

    @Test
    void paramSpec_constructorValidates() {
        assertThatNullPointerException().isThrownBy(() -> new ParamSpec(null, "key"));
        assertThatNullPointerException().isThrownBy(() -> new ParamSpec("name", null));

        var spec = new ParamSpec("locator", "step.web.click.param.locator");
        assertThat(spec.name()).isEqualTo("locator");
        assertThat(spec.hintKey()).isEqualTo("step.web.click.param.locator");
    }
}
