package com.qa.databasecore.arch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Regresión BUG-CS-01 — Checkstyle LineLengthCheck: {@code DatabaseGivenSteps.java}
 * líneas 98 y 100 excedían el límite de 120 caracteres.
 *
 * <p><b>Causa raíz:</b> espacios de alineación cosmética en las etiquetas multi-valor
 * de un switch expression. La expresión
 * {@code case "postgresql", "postgres"            ->} tenía 12 espacios de padding
 * (línea resultante: 123 chars) y
 * {@code case "sqlserver", "sql-server", "mssql"  ->} tenía 2 espacios de padding
 * (línea resultante: 122 chars). El case más largo ("sqlserver"...) resultaba en
 * 121 chars incluso sin padding, por lo que requirió además un salto de línea.</p>
 *
 * <p><b>Fix aplicado:</b> eliminados los espacios de alineación de todos los arms
 * del switch; el arm "sqlserver" usa {@code ->} al final de la primera línea y el
 * factory call en la línea siguiente.</p>
 *
 * <p><b>Este test fallaba en main antes del fix</b> (líneas 98=123, 100=122 chars)
 * y pasa después del fix (máximo 112 chars en el archivo).</p>
 *
 * @see com.qa.databasecore.steps.canonical.DatabaseGivenSteps
 */
@DisplayName("LineLengthRegression — BUG-CS-01 DatabaseGivenSteps ≤ 120 chars/line")
class LineLengthRegressionTest {

    /** Checkstyle configured max line length for database-core. */
    private static final int MAX_LINE_LENGTH = 120;

    /**
     * Source path relative to the Gradle module root (where tests run from).
     * Skipped automatically in environments where the source tree is absent
     * (e.g., compiled-only JAR consumers).
     */
    private static final String SOURCE_RELATIVE =
            "src/main/java/com/qa/databasecore/steps/canonical/DatabaseGivenSteps.java";

    @Test
    @DisplayName("DatabaseGivenSteps.java — ninguna línea supera los 120 caracteres")
    void noLineExceedsMaxLength() throws IOException {
        Path sourceFile = Paths.get(SOURCE_RELATIVE);
        assumeTrue(Files.exists(sourceFile),
                "Source file not found — skipping in non-source environments: " + sourceFile.toAbsolutePath());

        List<String> violations = new ArrayList<>();
        List<String> lines = Files.readAllLines(sourceFile);
        for (int i = 0; i < lines.size(); i++) {
            int len = lines.get(i).length();
            if (len > MAX_LINE_LENGTH) {
                violations.add(String.format("  línea %d: %d chars (excede %d por %d) — %s",
                        i + 1, len, MAX_LINE_LENGTH, len - MAX_LINE_LENGTH,
                        lines.get(i).strip().substring(0, Math.min(60, lines.get(i).strip().length()))));
            }
        }

        assertThat(violations)
                .as("Violaciones LineLengthCheck en DatabaseGivenSteps.java "
                        + "(BUG-CS-01 — debe estar en 0 después del fix):\n"
                        + String.join("\n", violations))
                .isEmpty();
    }
}
