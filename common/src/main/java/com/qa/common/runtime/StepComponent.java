package com.qa.common.runtime;

import java.util.List;
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
 * @author Abel Venero
 * @since 2.0.0
 * @see CorePlugin
 * @see BddPhase
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
     * Identificador único del componente.
     * Ejemplo: "api.authentication", "web.navigation", "mobile.gesture".
     * Por defecto se construye a partir del nombre en minúsculas con puntos.
     * @return ID único, nunca null
     */
    default String getId() {
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
}
