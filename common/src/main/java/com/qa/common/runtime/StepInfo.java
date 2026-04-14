package com.qa.common.runtime;

import java.util.Map;
import java.util.Objects;

/**
 * DTO de metadata de un componente de steps para el Backend / Frontend.
 *
 * <p>Representa la vista "plana" de un {@link StepComponent} con su contexto
 * de plugin (capa), lista para serializar en el endpoint {@code GET /api/steps}.
 *
 * <p>Los mapas i18n ({@link #displayNameByLocale()}, {@link #descriptionByLocale()})
 * permiten al Frontend mostrar nombres y descripciones en ES/EN/FR sin lógica adicional.
 * Si un locale no está presente, los métodos helper hacen fallback a los valores
 * mono-idioma ({@link #displayName()}, {@link #description()}).
 *
 * <p>El campo {@link #layer()} identifica la capa origen del componente
 * (ej: {@code "api"}, {@code "web"}, {@code "mobile"}).
 * El campo {@link #pattern()} transporta el nombre canónico del componente
 * para uso documental; el Backend puede enriquecerlo con los patrones Cucumber
 * reales via reflexión sobre {@code getStepDefinitionClass()}.
 *
 * @author Abel Venero
 * @since 2.1.0
 * @see StepComponent
 * @see StepDiscoveryService#discoverAllAsStepInfo()
 */
public record StepInfo(
        String id,
        String pattern,
        String layer,
        String componentId,
        String phase,
        String category,
        String icon,
        int displayOrder,
        String displayName,
        String description,
        Map<String, String> displayNameByLocale,
        Map<String, String> descriptionByLocale
) {

    /**
     * Compact constructor: valida campos requeridos e impone inmutabilidad en los mapas.
     */
    public StepInfo {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(layer, "layer no puede ser null");
        Objects.requireNonNull(componentId, "componentId no puede ser null");
        Objects.requireNonNull(phase, "phase no puede ser null");
        Objects.requireNonNull(displayNameByLocale, "displayNameByLocale no puede ser null");
        Objects.requireNonNull(descriptionByLocale, "descriptionByLocale no puede ser null");
        displayNameByLocale = Map.copyOf(displayNameByLocale);
        descriptionByLocale = Map.copyOf(descriptionByLocale);
    }

    // =========================================================================
    // Locale helpers
    // =========================================================================

    /**
     * Nombre de display en el locale solicitado.
     *
     * <p>Busca en {@link #displayNameByLocale()}; si el locale no existe,
     * retorna {@link #displayName()} como fallback.
     *
     * @param locale locale corto ("es", "en", "fr")
     * @return nombre localizado, nunca null
     */
    public String getDisplayNameForLocale(String locale) {
        Objects.requireNonNull(locale, "locale no puede ser null");
        String localized = displayNameByLocale.get(locale);
        if (localized != null) {
            return localized;
        }
        return displayName != null ? displayName : id;
    }

    /**
     * Descripción en el locale solicitado.
     *
     * <p>Busca en {@link #descriptionByLocale()}; si el locale no existe,
     * retorna {@link #description()} como fallback.
     *
     * @param locale locale corto ("es", "en", "fr")
     * @return descripción localizada, nunca null
     */
    public String getDescriptionForLocale(String locale) {
        Objects.requireNonNull(locale, "locale no puede ser null");
        String localized = descriptionByLocale.get(locale);
        if (localized != null) {
            return localized;
        }
        return description != null ? description : "";
    }
}
