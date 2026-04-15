package com.qa.webcore.components.bdd;

import com.qa.webcore.steps.interaction.DragDropSteps;
import com.qa.common.runtime.BddPhase;
import com.qa.common.runtime.StepComponent;
import com.qa.common.runtime.annotation.StepId;

import java.util.Map;

/**
 * Componente de steps Web: Drag and Drop (DEPRECATED).
 *
 * <p>Este componente está marcado como <strong>deprecated</strong> desde la versión 2.1.0.
 * El stepId {@code "web.dragdrop"} fue normalizado a {@code "web.drag.drop"} para consistencia
 * con la convención de separación por puntos usada en toda la plataforma
 * (ej: {@code "web.browser.config"}, {@code "web.validation.element"}).
 *
 * <p><strong>Migración:</strong> Reemplazar el uso de {@code "web.dragdrop"} por
 * {@code "web.drag.drop"} en todos los escenarios persistidos. El componente
 * sucesor {@code "web.drag.drop"} estará disponible desde la versión 2.2.0.
 *
 * <p>Este componente se mantiene activo durante el ciclo de deprecación para garantizar
 * retrocompatibilidad con escenarios persistidos en la base de datos antes de la migración.
 *
 * @author Abel Venero
 * @since 2.0.0
 * @deprecated Usar stepId {@code "web.drag.drop"} disponible desde v2.2.0.
 */
@SuppressWarnings("DeprecatedIsStillUsed") // el plugin lo registra durante el ciclo de deprecación
@Deprecated(since = "2.1.0", forRemoval = false)
@StepId(value = "web.dragdrop", deprecated = true, replacedBy = "web.drag.drop")
public class DragDropComponent implements StepComponent {
    @Override public String getName()                  { return "Drag and Drop"; }
    @Override public String getDisplayName()           { return "Drag and Drop"; }
    @Override public String getDescription()           { return "Arrastrar y soltar elementos"; }
    @Override public BddPhase getPhase()               { return BddPhase.WHEN; }
    @Override public String getCategory()              { return "Interaccion"; }
    @Override public String getIcon()                  { return "open_with"; }
    @Override public int getDisplayOrder()             { return 95; }
    @Override public Class<?> getStepDefinitionClass() { return DragDropSteps.class; }

    @Override
    public Map<String, String> getDisplayNameByLocale() {
        return Map.of(
            "es", "Drag and Drop",
            "en", "Drag and Drop",
            "fr", "Glisser-deposer"
        );
    }

    @Override
    public Map<String, String> getDescriptionByLocale() {
        return Map.of(
            "es", "Arrastrar y soltar elementos",
            "en", "Drag and drop elements",
            "fr", "Faire glisser et deposer des elements"
        );
    }
}
