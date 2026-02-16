package com.scotia.qa.common.driver;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para WebDriverManager - métodos de descarga desde Artifactory.
 *
 * <p>Estos tests cubren los nuevos métodos agregados para descarga de drivers:</p>
 * <ul>
 *   <li>detectOSForArtifactory() - detección de SO para rutas de Artifactory</li>
 *   <li>buildArtifactoryUrl() - construcción de URLs de descarga</li>
 *   <li>downloadDriverExecutable() - descarga directa de ejecutables</li>
 * </ul>
 *
 * @author Abel Venero
 * @version 1.0.0
 * @since 2025-02-15
 */
@DisplayName("WebDriverManager - Métodos de Artifactory Tests")
class WebDriverManagerArtifactoryTest {

    @TempDir
    Path tempCacheDir;

    // =========================================================================
    // TESTS DE detectOSForArtifactory()
    // =========================================================================

    @Nested
    @DisplayName("detectOSForArtifactory() Tests")
    class DetectOSTests {

        @Test
        @DisplayName("Debe retornar 'win' en Windows")
        void testDetectOSWindows() {
            // Given - Simular Windows
            String originalOS = System.getProperty("os.name");

            try {
                System.setProperty("os.name", "Windows 10");

                // When
                String result = invokeDetectOSForArtifactory();

                // Then
                assertThat(result).isEqualTo("win");
            } finally {
                System.setProperty("os.name", originalOS);
            }
        }

        @Test
        @DisplayName("Debe retornar 'mac' en macOS")
        void testDetectOSMac() {
            // Given
            String originalOS = System.getProperty("os.name");

            try {
                System.setProperty("os.name", "Mac OS X");

                // When
                String result = invokeDetectOSForArtifactory();

                // Then
                assertThat(result).isEqualTo("mac");
            } finally {
                System.setProperty("os.name", originalOS);
            }
        }

        @Test
        @DisplayName("Debe retornar 'linux' en Linux")
        void testDetectOSLinux() {
            // Given
            String originalOS = System.getProperty("os.name");

            try {
                System.setProperty("os.name", "Linux");

                // When
                String result = invokeDetectOSForArtifactory();

                // Then
                assertThat(result).isEqualTo("linux");
            } finally {
                System.setProperty("os.name", originalOS);
            }
        }

        /**
         * Helper para invocar método privado detectOSForArtifactory() usando reflexión.
         */
        private String invokeDetectOSForArtifactory() {
            try {
                var method = WebDriverManager.class.getDeclaredMethod("detectOSForArtifactory");
                method.setAccessible(true);
                return (String) method.invoke(null);
            } catch (Exception e) {
                throw new RuntimeException("Error invocando detectOSForArtifactory()", e);
            }
        }
    }

    // =========================================================================
    // TESTS DE buildArtifactoryUrl()
    // =========================================================================

    @Nested
    @DisplayName("buildArtifactoryUrl() Tests")
    class BuildArtifactoryUrlTests {

        @Test
        @DisplayName("Debe construir URL correcta para Chrome en Windows")
        void testBuildUrlChromeWindows() {
            // Given - Simular Windows
            String originalOS = System.getProperty("os.name");

            try {
                System.setProperty("os.name", "Windows 10");

                // When
                String url = invokeBuildArtifactoryUrl(
                    "https://artifactory.test.com/artifactory/libs-release",
                    "chromedriver",
                    "114.0.5735.90"
                );

                // Then
                assertThat(url)
                    .contains("/external/qa-drivers/chromedriver-win/chromedriver.exe")
                    .startsWith("https://artifactory.test.com/artifactory/libs-release");
            } finally {
                System.setProperty("os.name", originalOS);
            }
        }

        @Test
        @DisplayName("Debe construir URL correcta para Chrome en Mac")
        void testBuildUrlChromeMac() {
            // Given
            String originalOS = System.getProperty("os.name");

            try {
                System.setProperty("os.name", "Mac OS X");

                // When
                String url = invokeBuildArtifactoryUrl(
                    "https://artifactory.test.com/artifactory/libs-release",
                    "chromedriver",
                    "114.0.5735.90"
                );

                // Then
                assertThat(url)
                    .contains("/external/qa-drivers/chromedriver-mac/chromedriver")
                    .doesNotContain(".exe")
                    .startsWith("https://artifactory.test.com/artifactory/libs-release");
            } finally {
                System.setProperty("os.name", originalOS);
            }
        }

        @Test
        @DisplayName("Debe construir URL correcta para Firefox en Linux")
        void testBuildUrlFirefoxLinux() {
            // Given
            String originalOS = System.getProperty("os.name");

            try {
                System.setProperty("os.name", "Linux");

                // When
                String url = invokeBuildArtifactoryUrl(
                    "https://artifactory.test.com/artifactory/libs-release",
                    "geckodriver",
                    "0.33.0"
                );

                // Then
                assertThat(url)
                    .contains("/external/qa-drivers/geckodriver-linux/geckodriver")
                    .doesNotContain(".exe");
            } finally {
                System.setProperty("os.name", originalOS);
            }
        }

        @Test
        @DisplayName("Debe construir URL correcta para Edge en Windows")
        void testBuildUrlEdgeWindows() {
            // Given
            String originalOS = System.getProperty("os.name");

            try {
                System.setProperty("os.name", "Windows 10");

                // When
                String url = invokeBuildArtifactoryUrl(
                    "https://artifactory.test.com/artifactory/libs-release",
                    "edgedriver",
                    "114.0.1823.79"
                );

                // Then - Edge usa "msedgedriver.exe" como nombre
                assertThat(url)
                    .contains("/external/qa-drivers/edgedriver-win/msedgedriver.exe");
            } finally {
                System.setProperty("os.name", originalOS);
            }
        }

        /**
         * Helper para invocar método privado buildArtifactoryUrl().
         *
         * NOTA: buildArtifactoryUrl requiere que driver.artifactory.base.url esté configurado.
         * Como en tests no queremos depender de config real, vamos a setear la propiedad
         * temporalmente.
         */
        private String invokeBuildArtifactoryUrl(String baseUrl, String driverName, String version) {
            // Setear config temporalmente para el test
            String originalProp = System.getProperty("driver.artifactory.base.url");

            try {
                System.setProperty("driver.artifactory.base.url", baseUrl);

                var method = WebDriverManager.class.getDeclaredMethod("buildArtifactoryUrl",
                    String.class, String.class);
                method.setAccessible(true);
                return (String) method.invoke(null, driverName, version);
            } catch (Exception e) {
                throw new RuntimeException("Error invocando buildArtifactoryUrl()", e);
            } finally {
                if (originalProp != null) {
                    System.setProperty("driver.artifactory.base.url", originalProp);
                } else {
                    System.clearProperty("driver.artifactory.base.url");
                }
            }
        }
    }

    // =========================================================================
    // TESTS DE downloadDriverExecutable() - BÁSICOS (sin red real)
    // =========================================================================

    @Nested
    @DisplayName("downloadDriverExecutable() Tests")
    class DownloadDriverExecutableTests {

        @Test
        @DisplayName("Método downloadDriverExecutable() existe y es privado")
        void testDownloadDriverExecutableMethodExists() {
            // When/Then - Verificar que el método existe con la firma correcta
            assertThatCode(() -> {
                var method = WebDriverManager.class.getDeclaredMethod("downloadDriverExecutable",
                    String.class, String.class);
                assertThat(method).isNotNull();
                assertThat(java.lang.reflect.Modifier.isPrivate(method.getModifiers())).isTrue();
                assertThat(java.lang.reflect.Modifier.isStatic(method.getModifiers())).isTrue();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Método getExecutableName() genera nombres correctos")
        void testGetExecutableNameMethod() {
            // Este método es usado por downloadDriverExecutable
            // Verificamos que existe y funciona correctamente
            assertThatCode(() -> {
                var method = WebDriverManager.class.getDeclaredMethod("getExecutableName", String.class);
                method.setAccessible(true);

                String originalOS = System.getProperty("os.name");

                try {
                    // Test en Windows
                    System.setProperty("os.name", "Windows 10");
                    String chromeWin = (String) method.invoke(null, "chromedriver");
                    assertThat(chromeWin).isEqualTo("chromedriver.exe");

                    // Test en Mac
                    System.setProperty("os.name", "Mac OS X");
                    String chromeMac = (String) method.invoke(null, "chromedriver");
                    assertThat(chromeMac).isEqualTo("chromedriver");

                    // Test Edge en Windows
                    System.setProperty("os.name", "Windows 10");
                    String edgeWin = (String) method.invoke(null, "edgedriver");
                    assertThat(edgeWin).isEqualTo("msedgedriver.exe");

                } finally {
                    System.setProperty("os.name", originalOS);
                }
            }).doesNotThrowAnyException();
        }
    }
}

