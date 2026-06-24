package com.qa.mobilecore.discovery;

import com.qa.common.api.config.ConfigLoader;
import com.qa.common.api.config.ConfigLoaderHolder;
import com.qa.common.api.config.TypedConfig;
import com.qa.mobilecore.config.MobileConfigKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests de resolución del binario {@code adb} vía {@link MobileConfigKeys#ADB_PATH}.
 *
 * <p>Cubre el contrato de {@link AdbDeviceScanner#resolveBinary(String, String)}:
 * <ol>
 *   <li>clave sin configurar → nombre por defecto (fallback a {@code $PATH});</li>
 *   <li>clave = ruta ejecutable → usa esa ruta;</li>
 *   <li>clave = ruta no ejecutable → {@link IllegalStateException}.</li>
 * </ol>
 * Más el skip elegante en {@link AdbDeviceScanner#isAdbAvailable()} ante mala config.
 */
@DisplayName("AdbDeviceScanner — resolución del binario adb vía config")
class AdbDeviceScannerTest {

    @AfterEach
    void tearDown() {
        ConfigLoaderHolder.reset();
    }

    /** Sustituye el ConfigLoader global por uno que responde {@code getRaw} con la función dada. */
    private static void replaceLoader(Function<String, Optional<String>> getRaw) {
        ConfigLoaderHolder.replace(new ConfigLoader() {
            @Override public <T extends TypedConfig> T load(Class<T> c) {
                throw new UnsupportedOperationException("no usado en este test");
            }
            @Override public <T extends TypedConfig> T reload(Class<T> c) {
                throw new UnsupportedOperationException("no usado en este test");
            }
            @Override public Optional<String> getRaw(String key) {
                return getRaw.apply(key);
            }
        });
    }

    @Test
    @DisplayName("(a) clave sin configurar → usa el default 'adb' (resolución por $PATH)")
    void unset_returnsDefault() {
        replaceLoader(k -> Optional.empty());
        assertThat(AdbDeviceScanner.resolveBinary(MobileConfigKeys.ADB_PATH, "adb"))
            .isEqualTo("adb");
    }

    @Test
    @DisplayName("(b) clave = ruta absoluta ejecutable → usa esa ruta")
    void set_executablePath_returnsPath(@TempDir Path tmp) throws IOException {
        Path bin = Files.createFile(tmp.resolve("adb"));
        assumeTrue(bin.toFile().setExecutable(true), "el FS no soporta el bit de ejecución");
        assumeTrue(Files.isExecutable(bin), "el binario temporal no quedó ejecutable");

        replaceLoader(k -> MobileConfigKeys.ADB_PATH.equals(k)
            ? Optional.of(bin.toString()) : Optional.empty());

        assertThat(AdbDeviceScanner.resolveBinary(MobileConfigKeys.ADB_PATH, "adb"))
            .isEqualTo(bin.toString());
    }

    @Test
    @DisplayName("(c) clave = ruta inexistente → lanza IllegalStateException con clave y ruta")
    void set_nonExistentPath_throws() {
        String bad = "/no/such/path/adb-binary";
        replaceLoader(k -> MobileConfigKeys.ADB_PATH.equals(k)
            ? Optional.of(bad) : Optional.empty());

        assertThatThrownBy(() ->
                AdbDeviceScanner.resolveBinary(MobileConfigKeys.ADB_PATH, "adb"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(MobileConfigKeys.ADB_PATH)
            .hasMessageContaining(bad);
    }

    @Test
    @DisplayName("clave en blanco → tratada como ausente (default)")
    void blank_returnsDefault() {
        replaceLoader(k -> MobileConfigKeys.ADB_PATH.equals(k)
            ? Optional.of("   ") : Optional.empty());

        assertThat(AdbDeviceScanner.resolveBinary(MobileConfigKeys.ADB_PATH, "adb"))
            .isEqualTo("adb");
    }

    @Test
    @DisplayName("isAdbAvailable() con ruta mal configurada → false (skip elegante, sin lanzar)")
    void isAdbAvailable_badConfig_returnsFalseGracefully() {
        replaceLoader(k -> MobileConfigKeys.ADB_PATH.equals(k)
            ? Optional.of("/no/such/path/adb-binary") : Optional.empty());

        assertThat(new AdbDeviceScanner().isAdbAvailable()).isFalse();
    }
}
