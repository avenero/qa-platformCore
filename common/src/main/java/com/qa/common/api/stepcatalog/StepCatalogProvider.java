package com.qa.common.api.stepcatalog;

import java.util.List;

/**
 * SPI de catálogo i18n de steps BDD.
 *
 * <p>Cada módulo Core (web-core, mobile-core, http-core, database-core) implementa
 * esta interfaz y la registra vía {@link java.util.ServiceLoader} en
 * {@code META-INF/services/com.qa.common.api.stepcatalog.StepCatalogProvider}.
 *
 * <h2>Diseño</h2>
 * <ul>
 *   <li>Sin dependencia de Spring — el módulo Core es Java puro.</li>
 *   <li>Retorna <em>claves</em> i18n, no valores resueltos. El BE resuelve
 *       traducciones vía {@code StepI18nBundleLoader}.</li>
 *   <li>El bundle de propiedades vive en el mismo JAR que el provider
 *       (ej: {@code web-core.jar!/i18n/web_steps_es.properties}).</li>
 * </ul>
 *
 * <h2>Ejemplo de implementación</h2>
 * <pre>
 *   public class WebStepCatalogProvider implements StepCatalogProvider {
 *       {@literal @}Override public StepLayer layer() { return StepLayer.WEB; }
 *       {@literal @}Override public String bundleBaseName() { return "i18n/web_steps"; }
 *       {@literal @}Override public List&lt;StepCatalogEntry&gt; entries() { return ENTRIES; }
 *   }
 * </pre>
 */
public interface StepCatalogProvider {

    /** Capa tecnológica que representa este provider. */
    StepLayer layer();

    /**
     * Base name del ResourceBundle para este módulo.
     *
     * <p>El BE lo usa para cargar {@code {baseName}_es.properties},
     * {@code {baseName}_en.properties}, etc. desde el classpath.
     * Convención: {@code "i18n/{layer}_steps"} (ej: {@code "i18n/web_steps"}).
     */
    String bundleBaseName();

    /**
     * Lista de entradas del catálogo con sus claves i18n.
     * No debe retornar null ni entries con campos null.
     */
    List<StepCatalogEntry> entries();
}
