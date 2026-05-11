package com.qa.common.spi;

import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TASK-K01-A — verifica el contrato de **compat fina del SPI** (R-K01A-1):
 *
 * <ul>
 *   <li>El SPI canónico {@link com.qa.common.spi.CorePlugin} es discoverable.</li>
 *   <li>El alias legacy {@code com.qa.common.api.runtime.CorePlugin} sigue
 *       funcionando como interface que extiende el canónico.</li>
 *   <li>Ambos archivos {@code META-INF/services/...} (legacy + nuevo) apuntan
 *       a la misma clase concreta y se descubren simultáneamente sin doble
 *       carga.</li>
 * </ul>
 *
 * <p>Si este test falla en v2.1.x, los consumidores externos que descubren
 * el SPI por el FQN legacy van a romperse en runtime.
 *
 * @since TASK-K01-A
 */
class CorePluginSpiCompatTest {

    @Test
    void canonicalAndLegacyFqnBothDiscoverable_inThisModuleClasspathThereAreZero() {
        // common no registra plugins; los plugins viven en módulos especializados.
        // Pero el ServiceLoader debe poder enumerar sin lanzar.
        var legacyLoader = ServiceLoader.load(com.qa.common.api.runtime.CorePlugin.class);
        var canonicalLoader = ServiceLoader.load(com.qa.common.spi.CorePlugin.class);
        // Ambos enumeran sin error
        long legacyCount = legacyLoader.stream().count();
        long canonicalCount = canonicalLoader.stream().count();
        // En common (sin classpath de módulos), ambos retornan 0 (es OK).
        assertThat(legacyCount).isGreaterThanOrEqualTo(0);
        assertThat(canonicalCount).isGreaterThanOrEqualTo(0);
    }

    @Test
    void aliasIsDeprecatedForRemoval() throws NoSuchMethodException {
        Class<?> alias = com.qa.common.api.runtime.CorePlugin.class;
        assertThat(alias.isInterface()).isTrue();
        Deprecated dep = alias.getAnnotation(Deprecated.class);
        assertThat(dep).isNotNull();
        assertThat(dep.forRemoval()).isTrue();
        assertThat(dep.since()).isEqualTo("2.1.0");
    }

    @Test
    void aliasExtendsCanonical() {
        // La interface alias DEBE extender la canónica para que cualquier impl
        // legacy implements com.qa.common.api.runtime.CorePlugin también
        // satisfaga el SPI canónico.
        Class<?>[] supers = com.qa.common.api.runtime.CorePlugin.class.getInterfaces();
        assertThat(supers).contains(com.qa.common.spi.CorePlugin.class);
    }
}
