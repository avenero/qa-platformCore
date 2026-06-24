package com.qa.httpcore.i18n;

import com.qa.common.api.stepcatalog.ParamSpec;
import com.qa.common.api.stepcatalog.StepCatalogEntry;
import com.qa.common.api.stepcatalog.StepCatalogProvider;
import com.qa.httpcore.catalog.HttpStepCatalogProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guard de build (W1-T2-A8, ADR-CAT-01): paridad i18n del catálogo de steps
 * del Mundo #1 para la capa HTTP.
 *
 * <p>Verifica las tres cláusulas del contrato sobre las claves que las
 * {@link StepCatalogEntry} del {@link HttpStepCatalogProvider} referencian
 * ({@code categoryKey}, {@code descriptionKey}, {@code params[].hintKey}) frente
 * a los bundles {@code i18n/http_steps_<locale>.properties} (en/es/fr):
 * <ol>
 *   <li><b>Completitud:</b> cada clave referenciada existe y es no-blank.</li>
 *   <li><b>Sin huérfanos:</b> cada clave del bundle es referenciada por alguna
 *       entry (atrapa claves muertas — decisión G-3).</li>
 *   <li><b>Paridad triplete:</b> el set de claves de en/es/fr es idéntico
 *       (invariante #5).</li>
 * </ol>
 *
 * <p>Estas claves se autoescriben en la entry (≠ el id {@code @StepDef} del
 * Mundo #2, que no usa estos {@code .properties}). El test es Java puro: instancia
 * el provider y lee los bundles del classpath sin Spring ni drivers nativos, por lo
 * que respeta el modelo de doble-consumo (CLI sin BE / embebido).
 *
 * @since W1-T2-A8
 * @see StepCatalogProvider
 */
class StepKeyCompletenessTest {

    private static final List<String> LOCALES = List.of("en", "es", "fr");

    private final StepCatalogProvider provider = new HttpStepCatalogProvider();
    private final Set<String> referenced = referencedKeys(provider);

    @Test
    @DisplayName("toda clave referenciada por una entry existe y es no-blank en cada locale")
    void everyReferencedKeyIsPresentAndNonBlank() {
        for (String locale : LOCALES) {
            Properties bundle = loadBundle(provider.bundleBaseName(), locale);
            List<String> missing = new ArrayList<>();
            List<String> blank = new ArrayList<>();
            for (String key : referenced) {
                String value = bundle.getProperty(key);
                if (value == null) {
                    missing.add(key);
                } else if (value.isBlank()) {
                    blank.add(key);
                }
            }
            assertThat(missing)
                    .as("Claves referenciadas por el catálogo ausentes en %s_%s.properties",
                            provider.bundleBaseName(), locale)
                    .isEmpty();
            assertThat(blank)
                    .as("Claves referenciadas con valor blank en %s_%s.properties",
                            provider.bundleBaseName(), locale)
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("ninguna clave del bundle queda huérfana (sin entry que la referencie)")
    void noOrphanKeysInAnyLocale() {
        for (String locale : LOCALES) {
            Properties bundle = loadBundle(provider.bundleBaseName(), locale);
            List<String> orphans = new ArrayList<>();
            for (String key : bundle.stringPropertyNames()) {
                if (!referenced.contains(key)) {
                    orphans.add(key);
                }
            }
            assertThat(orphans)
                    .as("Claves huérfanas (no referenciadas por ninguna entry) en %s_%s.properties",
                            provider.bundleBaseName(), locale)
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("el set de claves es idéntico en en/es/fr (paridad triplete)")
    void keySetsAreIdenticalAcrossLocales() {
        Set<String> en = loadBundle(provider.bundleBaseName(), "en").stringPropertyNames();
        Set<String> es = loadBundle(provider.bundleBaseName(), "es").stringPropertyNames();
        Set<String> fr = loadBundle(provider.bundleBaseName(), "fr").stringPropertyNames();

        assertThat(es)
                .as("Las claves de %s_es.properties deben coincidir con _en (paridad triplete)",
                        provider.bundleBaseName())
                .containsExactlyInAnyOrderElementsOf(en);
        assertThat(fr)
                .as("Las claves de %s_fr.properties deben coincidir con _en (paridad triplete)",
                        provider.bundleBaseName())
                .containsExactlyInAnyOrderElementsOf(en);
    }

    /** Unión de las claves i18n referenciadas por todas las entries del provider. */
    private static Set<String> referencedKeys(StepCatalogProvider provider) {
        Set<String> keys = new LinkedHashSet<>();
        for (StepCatalogEntry entry : provider.entries()) {
            keys.add(entry.categoryKey());
            keys.add(entry.descriptionKey());
            for (ParamSpec param : entry.params()) {
                keys.add(param.hintKey());
            }
        }
        return keys;
    }

    /** Carga {@code /<baseName>_<locale>.properties} desde el classpath del módulo. */
    private static Properties loadBundle(String baseName, String locale) {
        String resource = "/" + baseName + "_" + locale + ".properties";
        Properties props = new Properties();
        try (InputStream in = StepKeyCompletenessTest.class.getResourceAsStream(resource)) {
            assertThat(in).as("Bundle i18n ausente en el classpath: %s", resource).isNotNull();
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el bundle i18n: " + resource, e);
        }
        return props;
    }
}
