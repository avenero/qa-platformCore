package com.qa.mobilecore.catalog;

import com.qa.common.runtime.catalog.ComponentCatalogWriter;
import com.qa.mobilecore.plugin.MobilePlugin;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * TASK-J03 — regenera {@code mobile-core/COMPONENTS.md} desde {@link MobilePlugin}.
 *
 * <pre>
 * ./gradlew :mobile-core:test --tests "*MobileComponentCatalogTest"
 * </pre>
 */
class MobileComponentCatalogTest {

    @Test
    void regenerateComponentsMd() throws Exception {
        MobilePlugin plugin = new MobilePlugin();
        Path output = Paths.get(System.getProperty("user.dir"), "COMPONENTS.md");
        ComponentCatalogWriter.write(plugin, output, "mobile-core");
        org.junit.jupiter.api.Assertions.assertTrue(plugin.getComponents().size() > 0,
                "MobilePlugin no expone componentes — algo se rompió en SPI/glue");
    }
}
