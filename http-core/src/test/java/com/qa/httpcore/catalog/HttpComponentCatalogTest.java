package com.qa.httpcore.catalog;

import com.qa.common.runtime.catalog.ComponentCatalogWriter;
import com.qa.httpcore.plugin.ApiPlugin;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * TASK-J03 — regenera {@code http-core/COMPONENTS.md} a partir de la lista
 * de componentes que expone {@link ApiPlugin}.
 *
 * <p>Se ejecuta on-demand (no anotamos {@code @Disabled} para que CI lo
 * incluya como guardia anti-drift: si fallara una aserción de no-vacío, es
 * señal de que el módulo perdió su catálogo). El test es **idempotente**:
 * sobrescribe el archivo cada vez con el mismo contenido si los componentes
 * no cambiaron.
 *
 * <pre>
 * ./gradlew :http-core:test --tests "*HttpComponentCatalogTest"
 * </pre>
 */
class HttpComponentCatalogTest {

    @Test
    void regenerateComponentsMd() throws Exception {
        ApiPlugin plugin = new ApiPlugin();
        Path output = Paths.get(System.getProperty("user.dir"), "COMPONENTS.md");
        ComponentCatalogWriter.write(plugin, output, "http-core");
        org.junit.jupiter.api.Assertions.assertTrue(plugin.getComponents().size() > 0,
                "ApiPlugin no expone componentes — algo se rompió en SPI/glue");
    }
}
