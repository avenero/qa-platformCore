package com.qa.webcore.components.bdd;

import com.qa.common.api.runtime.BddPhase;
import com.qa.common.api.runtime.StepComponent;
import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;

/**
 * Componente de steps Web: Drag and Drop (DEPRECATED — alias marcador).
 *
 * <p>Este componente está marcado como <strong>deprecated</strong> desde la versión 2.1.0.
 * El stepId {@code "web.dragdrop"} fue normalizado a {@code "web.drag.drop"} para consistencia
 * con la convención de separación por puntos usada en toda la plataforma
 * (ej: {@code "web.browser.config"}, {@code "web.validation.element"}).
 *
 * <p><strong>Migración:</strong> Reemplazar el uso de {@code "web.dragdrop"} por
 * {@code "web.drag.drop"} en todos los escenarios persistidos. El componente
 * sucesor {@link WebDragDropComponent} ({@code "web.drag.drop"}) ya expone los steps reales
 * desde la versión 2.2.0.
 *
 * <p>Este componente se mantiene registrado durante el ciclo de deprecación como
 * <strong>marcador del id {@code "web.dragdrop"}</strong> (retrocompatibilidad de resolución
 * por id), pero {@link #getStepDefinitionClass()} retorna {@code null}: la clase de steps la
 * enruta ahora {@link WebDragDropComponent}, de modo que el catálogo no registra dos veces los
 * mismos {@code @StepDef}. La ejecución no se ve afectada — el glue se resuelve por paquete.
 *
 * @author Abel Venero
 * @since 2.0.0
 * @deprecated Usar stepId {@code "web.drag.drop"} ({@link WebDragDropComponent}) disponible desde v2.2.0.
 */
@SuppressWarnings("DeprecatedIsStillUsed") // el plugin lo registra durante el ciclo de deprecación
@Deprecated(since = "2.1.0", forRemoval = false)
@StepId(value = "web.dragdrop", deprecated = true, replacedBy = "web.drag.drop")
public class DragDropComponent implements StepComponent {

    /** Display order for this component in the UI. */
    private static final int DISPLAY_ORDER = 95;

    @Override
    public String getName() {
        return "Drag and Drop";
    }

    @Override
    public String getDisplayName() {
        return "Drag and Drop";
    }

    @Override
    public String getDescription() {
        return "Arrastrar y soltar elementos";
    }

    @Override
    public BddPhase getPhase() {
        return BddPhase.WHEN;
    }

    @Override
    public String getCategory() {
        return "Interaccion";
    }

    @Override
    public String getIcon() {
        return "open_with";
    }

    @Override
    public int getDisplayOrder() {
        return DISPLAY_ORDER;
    }

    @Override
    public Class<?> getStepDefinitionClass() {
        // Alias deprecado: marcador del id "web.dragdrop". La clase de steps la enruta ahora el
        // sucesor WebDragDropComponent ("web.drag.drop"); retornar null evita que el catálogo
        // (StepDiscoveryService) registre dos veces los mismos @StepDef de DragDropSteps.
        return null;
    }

    @Override
    public List<String> getKeywords() {
        return List.of(
            "drag", "drop", "arrastar", "soltar", "move",
            "mover", "reorder", "dnd", "drag-and-drop", "glisser-deposer"
        );
    }

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
