package com.qa.common.api.runtime;


/**
 * Alias deprecated del SPI canónico {@link com.qa.common.spi.CorePlugin}.
 *
 * <h2>Por qué existe</h2>
 *
 * <p>TASK-K01-A (2026-05-11): el SPI {@code CorePlugin} migró del paquete legacy
 * {@code com.qa.common.runtime} al paquete canónico {@code com.qa.common.spi}
 * (consistente con el mapa publicado en
 * {@code docs/COMMON_PACKAGE_MAP.md}). Este alias se mantiene durante toda la
 * serie {@code v2.1.x} como **compat fina** para:
 *
 * <ul>
 *   <li>Consumidores externos que descubren el SPI por el nombre de archivo
 *       {@code META-INF/services/com.qa.common.runtime.CorePlugin}.</li>
 *   <li>Plugins propios que aún declaran {@code implements CorePlugin}
 *       importando esta interface (warning de deprecación al uso).</li>
 * </ul>
 *
 * <h2>Plan de salida</h2>
 *
 * <p>Eliminación prevista en {@code v2.2.0}. Para ese release:
 *
 * <ol>
 *   <li>Eliminar este archivo.</li>
 *   <li>Eliminar el archivo SPI legacy
 *       {@code META-INF/services/com.qa.common.runtime.CorePlugin} en los 4
 *       módulos especializados.</li>
 *   <li>Bump del {@code MIGRATION_GUIDE} con la fecha de eliminación.</li>
 * </ol>
 *
 * <h2>Reglas inviolables</h2>
 *
 * <ul>
 *   <li><b>R-K01A-1:</b> mientras coexistan {@code runtime.CorePlugin} y
 *       {@code spi.CorePlugin}, el SPI se descubre por <strong>ambos</strong>
 *       archivos {@code META-INF/services/...}. Eliminar uno antes de v2.2
 *       rompe consumidores externos.</li>
 *   <li><b>R-K01A-2:</b> esta interface alias queda anotada
 *       {@code @Deprecated(forRemoval=true)} y emite warning al uso.
 *       <strong>NO</strong> es path para código nuevo.</li>
 * </ul>
 *
 * @deprecated since 2.1.0, for removal in 2.2.0 — usar
 *             {@link com.qa.common.spi.CorePlugin} directamente.
 */
@Deprecated(since = "2.1.0", forRemoval = true)
public interface CorePlugin extends com.qa.common.spi.CorePlugin {
}
