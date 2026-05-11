package com.qa.common.api.config;

import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Bridge estático a la implementación de {@link ConfigLoader} para call sites
 * sin DI (drop-in replacement de {@code ConfigManager.getInstance()} legacy
 * durante K03-migrate).
 *
 * <p>La implementación se descubre vía {@link ServiceLoader} para mantener la
 * frontera api ↔ internal limpia: este holder vive en {@code com.qa.common.api.config}
 * y NO importa {@code DefaultConfigLoader}. El descubrimiento usa el archivo
 * {@code META-INF/services/com.qa.common.api.config.ConfigLoader}.
 *
 * <p>Patrones de uso:
 * <pre>{@code
 * // Producción / consumidores Core (qa-module-test puro o BE):
 * HttpConfig http = ConfigLoaderHolder.get().load(HttpConfig.class);
 *
 * // Tests (override + restore):
 * ConfigLoader fake = new FakeLoader();
 * ConfigLoaderHolder.replace(fake);
 * try { ... } finally { ConfigLoaderHolder.reset(); }
 * }</pre>
 *
 * @since TASK-K03M-F5
 */
public final class ConfigLoaderHolder {

    private static volatile ConfigLoader instance;

    private ConfigLoaderHolder() {}

    /**
     * Devuelve la instancia activa. Si no hay override vía {@link #replace},
     * la primera llamada inicializa via {@link ServiceLoader}.
     *
     * @throws IllegalStateException si no hay implementación registrada en
     *     {@code META-INF/services/com.qa.common.api.config.ConfigLoader}
     */
    public static ConfigLoader get() {
        ConfigLoader local = instance;
        if (local != null) return local;
        synchronized (ConfigLoaderHolder.class) {
            if (instance == null) {
                instance = loadViaSpi();
            }
            return instance;
        }
    }

    /**
     * Reemplaza la instancia. Pensado solo para tests — el caller debe
     * restaurar con {@link #reset} en {@code @AfterEach}/{@code finally}.
     */
    public static void replace(ConfigLoader loader) {
        instance = Objects.requireNonNull(loader, "loader no puede ser null");
    }

    /**
     * Restaura la instancia al default (la próxima llamada a {@link #get}
     * recarga vía {@link ServiceLoader}).
     */
    public static void reset() {
        instance = null;
    }

    private static ConfigLoader loadViaSpi() {
        ServiceLoader<ConfigLoader> sl = ServiceLoader.load(ConfigLoader.class);
        Iterator<ConfigLoader> it = sl.iterator();
        if (it.hasNext()) return it.next();
        throw new IllegalStateException(
                "No hay implementación de ConfigLoader registrada via ServiceLoader. "
                        + "Verifica que el classpath contenga "
                        + "META-INF/services/com.qa.common.api.config.ConfigLoader");
    }
}
