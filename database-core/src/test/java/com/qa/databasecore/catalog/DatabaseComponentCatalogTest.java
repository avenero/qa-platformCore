package com.qa.databasecore.catalog;

import com.qa.common.internal.runtime.catalog.ComponentCatalogWriter;
import com.qa.databasecore.plugin.DatabasePlugin;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * TASK-J03 — regenera {@code database-core/COMPONENTS.md} desde {@link DatabasePlugin}.
 *
 * <pre>
 * ./gradlew :database-core:test --tests "*DatabaseComponentCatalogTest"
 * </pre>
 */
class DatabaseComponentCatalogTest {

    @Test
    void regenerateComponentsMd() throws Exception {
        DatabasePlugin plugin = new DatabasePlugin();
        Path output = Paths.get(System.getProperty("user.dir"), "COMPONENTS.md");
        ComponentCatalogWriter.write(plugin, output, "database-core");
        org.junit.jupiter.api.Assertions.assertTrue(plugin.getComponents().size() > 0,
                "DatabasePlugin no expone componentes — algo se rompió en SPI/glue");
    }
}
