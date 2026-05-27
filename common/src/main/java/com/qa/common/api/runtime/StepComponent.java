package com.qa.common.api.runtime;

import com.qa.common.api.runtime.annotation.StepId;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Contrato para un componente cohesivo de steps BDD.
 *
 * <p>Agrupa steps por responsabilidad y fase BDD.
 * Cada plugin declara sus componentes via {@link CorePlugin#getComponents()}.
 *
 * <p>Los metadatos de visualización ({@link #getId()}, {@link #getDisplayName()},
 * {@link #getCategory()}, {@link #getIcon()}, {@link #getDisplayOrder()}) son usados
 * por el Backend para exponer una API estructurada y por el Frontend para construir
 * la paleta visual de diseño de escenarios.
 *
 * <h2>Convención de IDs</h2>
 * <p>El ID de un componente es su <strong>contrato público con el Backend y el Frontend</strong>.
 * La forma canónica de declararlo es mediante la anotación {@link StepId} en la clase implementadora:
 * <pre>
 *   &#64;StepId("api.authentication")
 *   public class ApiAuthComponent implements StepComponent { ... }
 * </pre>
 * <p>Si la clase no lleva {@code @StepId}, {@link #getId()} deriva el ID del nombre del componente
 * (lowercase, espacios → puntos). Ver {@link StepId} para el formato completo y el contrato
 * de estabilidad.
 *
 * @author Abel Venero
 * @since 2.0.0
 * @see CorePlugin
 * @see BddPhase
 * @see StepId
 */
public interface StepComponent {

    /**
     * Nombre descriptivo del componente.
     * Ejemplo: "HTTP Request", "Response Validation", "Database Setup".
     * @return nombre, nunca null ni vacio
     */
    String getName();

    /**
     * Fase BDD principal del componente.
     * @return fase BDD, nunca null
     */
    BddPhase getPhase();

    /**
     * Clase que contiene las definiciones de steps (anotaciones Cucumber).
     * @return clase de steps, puede ser null si aún no está implementado
     */
    Class<?> getStepDefinitionClass();

    /**
     * Palabras clave de dominio para enriquecer el semantic matching del BE.
     *
     * <p>Permite al {@code ScenarioSuggestionEngine} encontrar este componente cuando el QA
     * escribe sinónimos, abreviaturas o jerga técnica que no aparecen literalmente en
     * {@link #getName()}, {@link #getDescription()} o {@link #getId()}.
     *
     * <p>Recomendaciones:
     * <ul>
     *   <li>5–15 keywords por componente (más es ruido).</li>
     *   <li>Incluir sinónimos en español, inglés y francés cuando aplique.</li>
     *   <li>Términos técnicos del dominio (bearer, jwt, jsonpath, xpath, swipe).</li>
     *   <li>Acciones canónicas (validar, verificar, comprobar, assert, check).</li>
     * </ul>
     *
     * <p>Implementación por defecto retorna lista vacía — totalmente opcional. Los componentes
     * que no sobrescriban este método siguen funcionando con keywords vacíos (backward-compatible).
     *
     * @return lista inmutable de keywords en minúsculas; nunca null
     * @since 2.1.0
     */
    default List<String> getKeywords() {
        return List.of();
    }

    /**
     * Descripcion del proposito del componente.
     * @return descripcion legible
     */
    default String getDescription() {
        return getName() + " [" + getPhase().getLabel() + "]";
    }

    /**
     * Tags de Cucumber requeridos para activar este componente.
     * Si vacio, se activa cuando el plugin padre se activa.
     * @return tags requeridos, nunca null
     */
    default List<String> getRequiredTags() {
        return List.of();
    }

    // =========================================================================
    // Metadatos ricos para Backend/Frontend (Fase 2)
    // =========================================================================

    /**
     * Identificador único y estable del componente.
     *
     * <p><strong>Orden de prioridad:</strong>
     * <ol>
     *   <li>Valor de la anotación {@link StepId} declarada en la clase implementadora.</li>
     *   <li>Derivación automática: {@code getName()} en lowercase con espacios y
     *       barras reemplazados por puntos.</li>
     * </ol>
     *
     * <p>Se recomienda siempre declarar {@code @StepId} en la clase concreta para que
     * el ID sea explícito, visible y reforzado como contrato estable.
     *
     * <p>Ejemplos de IDs válidos:
     * <ul>
     *   <li>{@code "api.authentication"}</li>
     *   <li>{@code "web.browser.config"}</li>
     *   <li>{@code "mobile.device.config"}</li>
     *   <li>{@code "db.setup"}</li>
     * </ul>
     *
     * @return ID único, nunca null ni blank
     * @see StepId
     */
    default String getId() {
        StepId annotation = this.getClass().getAnnotation(StepId.class);
        if (annotation != null && !annotation.value().isBlank()) {
            return annotation.value();
        }
        return getName().toLowerCase().replace(" ", ".").replace("/", ".");
    }

    /**
     * Nombre legible para el Frontend (paleta de componentes).
     * Por defecto retorna {@link #getName()}.
     * @return nombre de display, nunca null
     */
    default String getDisplayName() {
        return getName();
    }

    /**
     * Categoría para agrupar componentes en la UI.
     * Ejemplos: "Configuración de Petición", "Ejecución", "Validación de Respuesta".
     * @return categoría, nunca null
     */
    default String getCategory() {
        return getPhase().getLabel();
    }

    /**
     * Clave de ícono Material Icons para el Frontend.
     * @return clave del ícono (ej: "lock", "send", "check_circle")
     */
    default String getIcon() {
        return "extension";
    }

    /**
     * Orden de presentación en la paleta visual del Frontend.
     * Menor número = se muestra antes.
     * @return orden de display
     */
    default int getDisplayOrder() {
        return 100;
    }

    /**
     * Servicios del {@code ExecutionContext} requeridos por este componente.
     * El {@code LifecycleManager} puede verificar que estén registrados.
     * @return lista de tipos de servicios requeridos
     */
    default List<Class<?>> getRequiredServices() {
        return List.of();
    }

    /**
     * Tags que activan específicamente este componente (adicionales a los del plugin).
     * Vacío = se activa cuando el plugin padre se activa.
     * @return set de tags de activación
     */
    default Set<String> getActivationTags() {
        return Set.of();
    }

    // =========================================================================
    // Ciclo de vida — deprecación y migración
    // =========================================================================

    /**
     * Indica si este componente está marcado como deprecado.
     *
     * <p>Un componente deprecado sigue siendo funcional pero señaliza al Backend y al
     * Frontend que existe una alternativa más actualizada, identificada por
     * {@link #getReplacementStepId()}. El motor de lint BDD usa este flag para:
     * <ul>
     *   <li>Emitir advertencias al compilar/validar escenarios que usen el ID deprecated.</li>
     *   <li>Sugerir la migración automática al {@code replacementStepId} en el Scenario Builder.</li>
     *   <li>Reportar al Backend el estado de los escenarios que requieren actualización.</li>
     * </ul>
     *
     * <p>La implementación por defecto lee el atributo {@link StepId#deprecated()} de la
     * anotación {@link StepId} declarada en la clase concreta. Retorna {@code false} si la
     * clase no lleva la anotación o si el atributo no está marcado.
     *
     * @return {@code true} si el componente está deprecado; {@code false} por defecto
     * @see StepId#deprecated()
     * @see #getReplacementStepId()
     */
    default boolean isDeprecated() {
        StepId annotation = this.getClass().getAnnotation(StepId.class);
        return annotation != null && annotation.deprecated();
    }

    /**
     * Retorna el {@code stepId} del componente que reemplaza a este, si está deprecado.
     *
     * <p>El Backend usa este valor para:
     * <ul>
     *   <li>Sugerir al Frontend qué componente seleccionar en lugar del deprecado.</li>
     *   <li>Migrar automáticamente escenarios persistidos durante operaciones de lint/import.</li>
     *   <li>Validar que el reemplazo existe en el catálogo
     *       ({@link com.qa.common.internal.runtime.StepDiscoveryService#resolveStep}).</li>
     * </ul>
     *
     * <p>La implementación por defecto lee {@link StepId#replacedBy()} de la anotación
     * {@link StepId}. Retorna {@code null} si la clase no lleva la anotación, si
     * {@link #isDeprecated()} es {@code false}, o si {@code replacedBy} está vacío.
     *
     * @return ID del componente sucesor, o {@code null} si no aplica o no está declarado
     * @see StepId#replacedBy()
     * @see #isDeprecated()
     */
    default String getReplacementStepId() {
        StepId annotation = this.getClass().getAnnotation(StepId.class);
        if (annotation != null && !annotation.replacedBy().isBlank()) {
            return annotation.replacedBy();
        }
        return null;
    }

    // =========================================================================
    // Metadata multi-idioma para el Frontend (ES / EN / FR)
    // =========================================================================

    /**
     * Nombre de display del componente por locale para el Frontend.
     *
     * <p>Las claves del mapa son locales cortos: {@code "es"}, {@code "en"}, {@code "fr"}.
     * Si el mapa está vacío o no contiene el locale solicitado, el Frontend
     * debe hacer fallback a {@link #getDisplayName()} (español por defecto).
     *
     * @return mapa inmutable locale → nombre; nunca null
     */
    default Map<String, String> getDisplayNameByLocale() {
        return Map.of();
    }

    /**
     * Descripción del componente por locale para el Frontend.
     *
     * <p>Las claves del mapa son locales cortos: {@code "es"}, {@code "en"}, {@code "fr"}.
     * Si el mapa está vacío o no contiene el locale solicitado, el Frontend
     * debe hacer fallback a {@link #getDescription()} (español por defecto).
     *
     * @return mapa inmutable locale → descripción; nunca null
     */
    default Map<String, String> getDescriptionByLocale() {
        return Map.of();
    }
}
