package com.qa.mobileagent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeatureMaterializerTest {

    @Test
    void writesFeatureContentsAndReturnsAbsolutePaths(@TempDir Path workspace) throws IOException {
        FeatureMaterializer m = new FeatureMaterializer(workspace);
        List<String> paths = m.materialize("exec-1", Map.of(
                "login.feature", "Feature: Login\n",
                "checkout.feature", "Feature: Checkout\n"));

        assertThat(paths).hasSize(2);
        assertThat(paths).allSatisfy(p -> assertThat(Path.of(p)).exists());
        assertThat(Files.readString(Path.of(paths.get(0)))).startsWith("Feature:");
    }

    @Test
    void rejectsPathTraversalFilenames(@TempDir Path workspace) {
        FeatureMaterializer m = new FeatureMaterializer(workspace);
        assertThatThrownBy(() -> m.materialize("exec-1", Map.of("../etc/passwd", "x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inseguro");
    }

    @Test
    void cleanupRemovesExecDirRecursively(@TempDir Path workspace) throws IOException {
        FeatureMaterializer m = new FeatureMaterializer(workspace);
        m.materialize("exec-2", Map.of("a.feature", "x", "b.feature", "y"));
        Path execDir = m.execDir("exec-2");
        assertThat(execDir).exists();

        m.cleanup(execDir);
        assertThat(execDir).doesNotExist();
    }
}
