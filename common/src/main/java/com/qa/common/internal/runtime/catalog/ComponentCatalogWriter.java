package com.qa.common.internal.runtime.catalog;

import com.qa.common.spi.CorePlugin;
import com.qa.common.utils.security.SecurityUtilities;

import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Utility para emitir el {@code COMPONENTS.md} de un módulo a partir de la
 * lista de {@link StepComponent} expuesta por su {@link CorePlugin}.
 *
 * <p>TASK-J03 — single source of truth: el catálogo se autogenera desde el
 * código (no se edita a mano). Cada módulo registra un test con
 * {@code @Disabled} que se ejecuta on-demand para regenerar:
 *
 * <pre>
 * ./gradlew :http-core:test --tests "com.qa.httpcore.catalog.HttpComponentCatalogTest"
 * </pre>
 *
 * <p>Los archivos resultantes se commitean al repo y se renderizan en GitHub.
 * Si el catálogo y los componentes drift, basta con re-ejecutar el test.
 *
 * @since TASK-J03
 */
public final class ComponentCatalogWriter {

    private ComponentCatalogWriter() { }

    /**
     * Escribe el catálogo del plugin en {@code outputFile} (típicamente
     * {@code <module>/COMPONENTS.md}).
     */
    public static void write(CorePlugin plugin, Path outputFile, String moduleName) throws IOException {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(outputFile, "outputFile");
        Objects.requireNonNull(moduleName, "moduleName");

        List<StepComponent> components = plugin.getComponents();
        if (components == null) components = List.of();

        StringBuilder md = new StringBuilder();
        appendHeader(md, plugin, moduleName, components.size());

        components.stream()
                .sorted(Comparator
                        .comparing((StepComponent c) -> phaseOrder(c.getPhase()))
                        .thenComparingInt(StepComponent::getDisplayOrder)
                        .thenComparing(StepComponent::getId))
                .forEach(c -> appendComponent(md, c));

        appendFooter(md);
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, md.toString(), StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------

    private static void appendHeader(StringBuilder md, CorePlugin plugin,
                                     String moduleName, int total) {
        md.append("# ").append(moduleName).append(" — Component Catalog\n\n");
        md.append("> **Auto-generado** — TASK-J03. NO editar manualmente.\n");
        md.append("> Regenerar con `./gradlew :").append(moduleName)
                .append(":test --tests \"*ComponentCatalogTest\"`.\n");
        md.append(">\n");
        md.append("> **Plugin:** `").append(plugin.getClass().getName()).append("`  \n");
        md.append("> **Platform ID:** `").append(safeStr(plugin.platformId())).append("`  \n");
        md.append("> **Display:** ").append(safeStr(plugin.displayName())).append("  \n");
        md.append("> **Componentes:** ").append(total).append("  \n");
        md.append("> **Última generación:** ").append(LocalDate.now()).append("\n\n");

        md.append("Esta tabla es el **contrato público** de los `StepComponent` que el módulo ");
        md.append("`").append(moduleName).append("` expone al Backend (catálogo i18n) y al Frontend ");
        md.append("(paleta del Scenario Builder). Cada entrada se deriva por reflexión vía SPI ");
        md.append("(`ServiceLoader<CorePlugin>`).\n\n");

        md.append("**Convenciones**\n\n");
        md.append("- `@StepId`: identificador estable. Cambiar un id es **breaking** ");
        md.append("(rompe escenarios persistidos en BD del BE).\n");
        md.append("- Fase BDD: `GIVEN | WHEN | THEN | ANY`.\n");
        md.append("- Keywords: enriquecen el `ScenarioSuggestionEngine` (TASK-C06).\n");
        md.append("- Locale: si una clave no aparece en `displayNameByLocale`, el FE cae a `displayName` (ES).\n\n");
        md.append("---\n\n");
    }

    private static void appendComponent(StringBuilder md, StepComponent c) {
        md.append("## `").append(safeStr(c.getId())).append("`\n\n");

        if (c.isDeprecated()) {
            md.append("> ⚠️ **Deprecado** — usar `")
                    .append(safeStr(c.getReplacementStepId()))
                    .append("` en su lugar.\n\n");
        }

        md.append("- **Display:** ").append(buildDisplayMulti(c)).append("\n");
        md.append("- **Categoría:** ").append(safeStr(c.getCategory())).append("  ");
        md.append("· **Fase BDD:** `").append(c.getPhase().name()).append("`");
        md.append("  · **Display order:** `").append(c.getDisplayOrder()).append("`\n");
        md.append("- **Icono:** `").append(safeStr(c.getIcon())).append("`");

        List<String> kw = c.getKeywords();
        if (kw != null && !kw.isEmpty()) {
            md.append("  · **Keywords:** ").append(String.join(", ", kw));
        }
        md.append("\n");

        Map<String, String> descI18n = c.getDescriptionByLocale();
        String desc = descI18n != null && descI18n.containsKey("es")
                ? descI18n.get("es")
                : c.getDescription();
        if (desc != null && !desc.isBlank()) {
            md.append("- **Descripción:** ").append(desc.replace("\n", " ")).append("\n");
        }

        Class<?> stepDef = c.getStepDefinitionClass();
        if (stepDef != null) {
            md.append("- **Glue:** `").append(stepDef.getName()).append("`\n");
        }

        List<String> activationTags = c.getActivationTags() == null
                ? List.of() : c.getActivationTags().stream().sorted().toList();
        if (!activationTags.isEmpty()) {
            md.append("- **Activation tags:** `")
                    .append(String.join("`, `", activationTags))
                    .append("`\n");
        }

        md.append("\n");
    }

    private static void appendFooter(StringBuilder md) {
        md.append("---\n\n");
        md.append("> Para añadir un componente nuevo: implementar ");
        md.append("`com.qa.common.api.runtime.StepComponent`, anotar la clase con ");
        md.append("`@com.qa.common.api.runtime.annotation.StepId(\"<id>\")`, registrarla en el ");
        md.append("plugin (`getComponents()`), y regenerar este documento.\n\n");
        md.append("> Para deprecar: marcar `@StepId(value=..., deprecated=true, replacedBy=\"<nuevo-id>\")` ");
        md.append("y mantener la clase activa al menos un sprint para permitir migración FE.\n");
    }

    private static String buildDisplayMulti(StepComponent c) {
        Map<String, String> i18n = c.getDisplayNameByLocale();
        String es = i18n != null ? i18n.getOrDefault("es", c.getDisplayName()) : c.getDisplayName();
        String en = i18n != null ? i18n.get("en") : null;
        String fr = i18n != null ? i18n.get("fr") : null;
        StringBuilder out = new StringBuilder("**").append(safeStr(es)).append("** _(es)_");
        if (en != null && !en.isBlank()) out.append(" / **").append(en).append("** _(en)_");
        if (fr != null && !fr.isBlank()) out.append(" / **").append(fr).append("** _(fr)_");
        return out.toString();
    }

    private static int phaseOrder(BddPhase phase) {
        return switch (phase) {
            case GIVEN -> 0;
            case WHEN  -> 1;
            case THEN  -> 2;
        };
    }

    private static String safeStr(String s) {
        return s == null ? "" : s;
    }
}
