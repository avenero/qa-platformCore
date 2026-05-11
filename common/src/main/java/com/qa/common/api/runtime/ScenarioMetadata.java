package com.qa.common.api.runtime;
import com.qa.common.utils.security.SecurityUtilities;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Metadatos inmutables de un escenario Cucumber.
 *
 * <p>Creado por {@link ScenarioLifecycleBridge} a partir del evento
 * {@code TestCaseStarted}/{@code TestCaseFinished} de Cucumber, y propagado
 * a {@link LifecycleManager#onScenarioStart} / {@link LifecycleManager#onScenarioEnd}
 * para que los {@link CorePlugin} puedan inspeccionarlo antes y después de cada escenario.
 *
 * <p><b>Campos:</b>
 * <ul>
 *   <li>{@code name}  — nombre del escenario tal como aparece en el {@code .feature}</li>
 *   <li>{@code tags}  — conjunto inmutable de tags (con prefijo @, ej: {@code @api}, {@code @smoke})</li>
 *   <li>{@code uri}   — ruta del archivo {@code .feature} (ej: {@code classpath:features/api/login.feature})</li>
 *   <li>{@code line}  — número de línea del escenario dentro del {@code .feature}</li>
 * </ul>
 *
 * <p><b>Formato de tags:</b> los tags siguen el convenio de Cucumber con prefijo {@code @}.
 * Ejemplo: {@code @api}, {@code @smoke}, {@code @regression}.
 *
 * <p><b>Ejemplo de uso en un CorePlugin:</b>
 * <pre>{@code
 * @Override
 * public void onScenarioStart(ExecutionContext context) {
 *     // context tiene la info del escenario si se necesita via VariableStore
 *     // ScenarioMetadata se pasa al LifecycleManager, no al plugin directamente
 * }
 * }</pre>
 *
 * @param name  nombre del escenario, nunca null
 * @param tags  tags del escenario, nunca null (puede ser vacío)
 * @param uri   URI del archivo .feature, nunca null (puede ser vacío)
 * @param line  número de línea del escenario (>= 0)
 *
 * @author Abel Venero
 * @since 2.1.0
 * @see ScenarioLifecycleBridge
 * @see LifecycleManager
 */
public record ScenarioMetadata(
        String name,
        Set<String> tags,
        String uri,
        int line
) {

    /**
     * Validación compacta del constructor canonical.
     * Garantiza que {@code name} y {@code uri} nunca sean null,
     * y que {@code tags} sea un conjunto inmutable.
     */
    public ScenarioMetadata {
        Objects.requireNonNull(name, "name no puede ser null");
        tags = (tags != null) ? Set.copyOf(tags) : Set.of();
        uri  = (uri  != null) ? uri              : "";
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Crea metadatos completos a partir de todos los campos.
     *
     * @param name  nombre del escenario
     * @param tags  colección de tags (se copia a conjunto inmutable)
     * @param uri   URI del .feature
     * @param line  línea del escenario
     * @return nueva instancia inmutable
     */
    public static ScenarioMetadata of(String name, Collection<String> tags, String uri, int line) {
        return new ScenarioMetadata(
                name,
                (tags != null) ? Set.copyOf(tags) : Set.of(),
                uri,
                line);
    }

    /**
     * Crea metadatos con solo nombre y tags (uri="" y line=0).
     *
     * <p>Conveniente para tests unitarios donde la ubicación física no importa.
     *
     * @param name  nombre del escenario
     * @param tags  colección de tags (puede ser null → Set vacío)
     * @return nueva instancia inmutable
     */
    public static ScenarioMetadata ofTags(String name, Collection<String> tags) {
        return new ScenarioMetadata(
                name,
                (tags != null) ? Set.copyOf(tags) : Set.of(),
                "",
                0);
    }

    // -------------------------------------------------------------------------
    // Query helpers
    // -------------------------------------------------------------------------

    /**
     * Verifica si el escenario tiene el tag indicado (con prefijo @).
     *
     * @param tag tag a buscar, ej: {@code "@api"}
     * @return {@code true} si el escenario contiene ese tag
     */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    /**
     * Verifica si el escenario tiene <em>alguno</em> de los tags indicados.
     *
     * @param candidateTags tags a buscar
     * @return {@code true} si al menos uno de los tags está presente
     */
    public boolean hasAnyTag(Collection<String> candidateTags) {
        if (candidateTags == null || candidateTags.isEmpty()) {
            return false;
        }
        return candidateTags.stream().anyMatch(tags::contains);
    }

    /**
     * Retorna {@code true} si el escenario no tiene ningún tag de capa.
     */
    public boolean hasTags() {
        return !tags.isEmpty();
    }

    @Override
    public String toString() {
        return "ScenarioMetadata{name='" + name + "', tags=" + tags
                + ", uri='" + uri + "', line=" + line + "}";
    }
}
