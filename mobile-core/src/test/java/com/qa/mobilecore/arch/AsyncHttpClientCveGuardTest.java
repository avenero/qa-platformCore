package com.qa.mobilecore.arch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guard de regresión para CVE-2024-53990 (async-http-client {@code CookieStore},
 * CVSS 9.2 CRITICAL, GHSA-mfj5-cf8g-g2fv).
 *
 * <p>async-http-client NO es dependencia declarada de mobile-core y hoy resuelve a cero jars
 * en {@code runtimeClasspath} (transitivamente ausente post Selenium 4.27 / Appium 9.4,
 * verificado 2026-06). El pin {@code 2.12.4} del bloque {@code constraints} en
 * {@code build.gradle} es un <em>cap de regresión</em>: si un bump transitivo futuro
 * reintrodujera un AHC vulnerable, nadie se enteraría. Este test rompe el build si eso ocurre.
 *
 * <p>Política fail-closed: PASA cuando AHC está ausente o resuelve a una versión segura;
 * FALLA cuando hay una versión vulnerable presente ({@code < 2.12.4} o exactamente
 * {@code 3.0.0}) o cuando hay un jar de AHC cuya versión no se puede determinar.
 *
 * <p>Versiones seguras: {@code 2.12.4+} (último 2.x, invariante #18) y {@code 3.0.1+}
 * (la 3.0.0 sigue siendo vulnerable). NO se adopta 3.x: arrastra netty 4.x y reabre el
 * invariante #19 (dos stacks HTTP en classpath). Ver {@code docs/SECURITY_ACCEPTED_RISKS.md} §B.
 *
 * @since W3-H2
 */
class AsyncHttpClientCveGuardTest {

    /**
     * Matchea el jar del artefacto CORE {@code async-http-client}. El dígito obligatorio
     * justo tras el prefijo excluye artefactos hermanos como
     * {@code async-http-client-netty-utils-*.jar} o {@code async-http-client-extras-*.jar}.
     */
    private static final Pattern AHC_CORE_JAR =
            Pattern.compile("^async-http-client-(\\d[\\w.\\-]*)\\.jar$");

    /** CVE-2024-53990 parchado en 2.12.4 (rama 2.x). */
    private static final int[] MIN_SAFE_2X = {2, 12, 4};

    /** 3.0.0 sigue siendo vulnerable; parchado recién en 3.0.1. */
    private static final int[] VULN_3X = {3, 0, 0};

    // ------------------------------------------------------------------
    // El guard real: escanea el runtimeClasspath efectivo del worker de test.
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Ningún async-http-client vulnerable (<2.12.4 o ==3.0.0) en el classpath (CVE-2024-53990)")
    void noVulnerableAsyncHttpClientOnRuntimeClasspath() {
        String classpath = System.getProperty("java.class.path", "");

        // Liveness: probar que escaneamos un classpath real y poblado (no uno vacío que
        // pasaría de forma vacua). JUnit siempre está presente cuando este test corre.
        assertTrue(classpath.contains("junit"),
                "java.class.path no parece un runtimeClasspath poblado; el guard no puede "
                + "probar la ausencia de AHC. Classpath: " + classpath);

        List<String> vulnerable = scanForVulnerableAhc(classpath);
        assertTrue(vulnerable.isEmpty(),
                "Detectado async-http-client vulnerable en el runtimeClasspath de mobile-core "
                + "(CVE-2024-53990, CVSS 9.2 CRITICAL). El pin 2.12.4 de mobile-core/build.gradle "
                + "debe sostenerse; NO bumpear a 3.0.0 (sigue vulnerable) ni adoptar 3.x (reabre el "
                + "invariante #19: dos stacks HTTP). Ver docs/SECURITY_ACCEPTED_RISKS.md §B. "
                + "Entradas ofensivas: " + vulnerable);
    }

    // ------------------------------------------------------------------
    // Lógica de detección (pura) — ejercitada de forma hermética abajo.
    // ------------------------------------------------------------------

    /** Devuelve los nombres de jars de AHC vulnerables presentes en el classpath dado. */
    static List<String> scanForVulnerableAhc(String classpath) {
        List<String> findings = new ArrayList<>();
        if (classpath == null || classpath.isEmpty()) {
            return findings;
        }
        for (String entry : classpath.split(Pattern.quote(File.pathSeparator))) {
            if (entry.isEmpty()) {
                continue;
            }
            String name = new File(entry).getName();
            Matcher m = AHC_CORE_JAR.matcher(name);
            if (m.matches() && isVulnerable(m.group(1))) {
                findings.add(name);
            }
        }
        return findings;
    }

    /**
     * @return {@code true} si la versión de async-http-client es vulnerable a CVE-2024-53990
     *         ({@code < 2.12.4} o exactamente {@code 3.0.0}), o si no se puede parsear
     *         (fail-closed).
     */
    static boolean isVulnerable(String version) {
        int[] v;
        try {
            v = parse(version);
        } catch (RuntimeException ex) {
            return true; // fail-closed: presente pero indeterminable
        }
        return compare(v, MIN_SAFE_2X) < 0 || compare(v, VULN_3X) == 0;
    }

    private static int[] parse(String version) {
        String core = version.split("[-_+]", 2)[0]; // descarta cualquier -qualifier / metadata
        String[] parts = core.split("\\.");
        int[] out = new int[Math.max(3, parts.length)];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Integer.parseInt(parts[i].trim()); // lanza -> fail-closed
        }
        return out;
    }

    private static int compare(int[] a, int[] b) {
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int ai = i < a.length ? a[i] : 0;
            int bi = i < b.length ? b[i] : 0;
            if (ai != bi) {
                return Integer.compare(ai, bi);
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // Smoke hermético (card C3): un async-http-client-2.12.3.jar plantado debe ser
    // detectado -> prueba que el guard NO es un no-op. Determinista, offline, permanente.
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Smoke: un async-http-client-2.12.3.jar plantado es detectado (el guard no es no-op)")
    void detectsPlantedVulnerableJar() {
        String fakeCp = String.join(File.pathSeparator,
                "/repo/junit-jupiter-5.10.0.jar",
                "/repo/async-http-client-2.12.3.jar",
                "/repo/jackson-databind-2.17.0.jar");
        assertTrue(scanForVulnerableAhc(fakeCp).contains("async-http-client-2.12.3.jar"),
                "el guard no detectó un async-http-client-2.12.3.jar vulnerable plantado");
    }

    @Test
    @DisplayName("El pin seguro 2.12.4 y el hermano netty-utils NO se marcan")
    void safeAndSiblingArtifactsNotFlagged() {
        String cp = String.join(File.pathSeparator,
                "/repo/async-http-client-2.12.4.jar",
                "/repo/async-http-client-netty-utils-2.12.4.jar");
        assertTrue(scanForVulnerableAhc(cp).isEmpty(),
                "se marcó por error el 2.12.4 seguro o el hermano netty-utils");
    }

    @Test
    @DisplayName("AHC ausente pasa (cap de regresión intacto, no es dependencia viva)")
    void absentArtifactPasses() {
        String cp = String.join(File.pathSeparator,
                "/repo/junit.jar", "/repo/selenium-java-4.27.0.jar");
        assertTrue(scanForVulnerableAhc(cp).isEmpty(), "AHC ausente no debe producir hallazgos");
    }

    @ParameterizedTest
    @CsvSource({
        "2.12.3, true",
        "2.12.4, false",
        "2.12.5, false",
        "2.0.0,  true",
        "1.9.40, true",
        "3.0.0,  true",
        "3.0.1,  false",
        "3.1.0,  false",
    })
    @DisplayName("isVulnerable respeta la frontera de CVE-2024-53990: <2.12.4 o ==3.0.0")
    void isVulnerableMatchesCveBoundaries(String version, boolean expected) {
        assertEquals(expected, isVulnerable(version), "versión " + version);
    }

    @Test
    @DisplayName("Versión no parseable falla closed (se trata como vulnerable)")
    void unparseableVersionFailsClosed() {
        assertTrue(isVulnerable("no-es-version"), "una versión de AHC no parseable debe fallar closed");
    }
}
