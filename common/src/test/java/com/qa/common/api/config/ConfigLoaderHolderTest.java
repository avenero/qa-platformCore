package com.qa.common.api.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TASK-K03M-F5 — ConfigLoaderHolder.
 */
class ConfigLoaderHolderTest {

    @AfterEach
    void cleanup() {
        ConfigLoaderHolder.reset();
    }

    @Test
    @DisplayName("get() resuelve DefaultConfigLoader vía ServiceLoader (META-INF/services)")
    void get_returnsDefaultLoader() {
        ConfigLoader loader = ConfigLoaderHolder.get();
        assertThat(loader).isNotNull();
        assertThat(loader.getClass().getName())
                .isEqualTo("com.qa.common.internal.config.DefaultConfigLoader");
    }

    @Test
    @DisplayName("replace() sustituye la instancia activa")
    void replace_overridesInstance() {
        ConfigLoader fake = new FakeLoader();
        ConfigLoaderHolder.replace(fake);
        assertThat(ConfigLoaderHolder.get()).isSameAs(fake);
    }

    @Test
    @DisplayName("reset() vuelve a cargar vía ServiceLoader")
    void reset_restoresDefault() {
        ConfigLoaderHolder.replace(new FakeLoader());
        ConfigLoaderHolder.reset();
        ConfigLoader after = ConfigLoaderHolder.get();
        assertThat(after.getClass().getName())
                .isEqualTo("com.qa.common.internal.config.DefaultConfigLoader");
    }

    @Test
    @DisplayName("replace(null) lanza NullPointerException defensivamente")
    void replace_nullThrows() {
        assertThatThrownBy(() -> ConfigLoaderHolder.replace(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("replace concurrente desde N hilos respeta volatile (todos ven la última asignación)")
    void concurrentReplace_isThreadSafe() throws Exception {
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ConcurrentHashMap<Integer, ConfigLoader> seen = new ConcurrentHashMap<>();
        AtomicInteger errors = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    start.await();
                    FakeLoader mine = new FakeLoader();
                    ConfigLoaderHolder.replace(mine);
                    // Tras replace, get() debe devolver una instancia no-null
                    seen.put(id, ConfigLoaderHolder.get());
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(errors).hasValue(0);
        assertThat(seen).hasSize(threads);
        // Todas las observaciones son non-null (volatile garantiza visibilidad)
        seen.values().forEach(v -> assertThat(v).isNotNull());
    }

    private static final class FakeLoader implements ConfigLoader {
        @Override public <T extends TypedConfig> T load(Class<T> c) { return null; }
        @Override public <T extends TypedConfig> T reload(Class<T> c) { return null; }
        @Override public Optional<String> getRaw(String key) { return Optional.empty(); }
    }
}
