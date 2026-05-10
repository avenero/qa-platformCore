package com.qa.webcore.catalog;

import com.qa.common.runtime.catalog.ComponentCatalogWriter;
import com.qa.webcore.plugin.WebPlugin;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * TASK-J03 — regenera {@code web-core/COMPONENTS.md} desde {@link WebPlugin}.
 *
 * <pre>
 * ./gradlew :web-core:test --tests "*WebComponentCatalogTest"
 * </pre>
 */
class WebComponentCatalogTest {

    @Test
    void regenerateComponentsMd() throws Exception {
        WebPlugin plugin = new WebPlugin();
        Path output = Paths.get(System.getProperty("user.dir"), "COMPONENTS.md");
        ComponentCatalogWriter.write(plugin, output, "web-core");
        org.junit.jupiter.api.Assertions.assertTrue(plugin.getComponents().size() > 0,
                "WebPlugin no expone componentes — algo se rompió en SPI/glue");
    }
}
