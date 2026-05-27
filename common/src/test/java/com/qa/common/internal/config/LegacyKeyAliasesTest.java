package com.qa.common.internal.config;

import com.qa.common.api.config.HttpConfig;
import com.qa.common.api.config.MobileConfig;
import com.qa.common.api.runtime.ExecutionConfig;
import com.qa.common.api.runtime.ExecutionContext;
import com.qa.common.internal.config.source.ConfigSource;
import com.qa.common.internal.config.source.ExecutionContextSource;
import com.qa.common.internal.config.source.SystemPropertySource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TASK-K03M-F7 — LegacyKeyAliases + alias resolution en lookup.
 *
 * <p>Cubre:
 * <ol>
 *   <li>alias hit (key canónica resuelve via legacy en cualquier source)</li>
 *   <li>alias miss (key sin alias y sin valor → empty)</li>
 *   <li>no shadowing (canónico siempre tiene precedencia)</li>
 *   <li>flujo BE→ExecutionContext con keys dotted (CRÍTICO post K03-migrate)</li>
 * </ol>
 */
class LegacyKeyAliasesTest {

    private DefaultConfigLoader loader;

    @AfterEach
    void cleanup() {
        if (loader != null) loader.close();
        System.clearProperty("mobile.appiumServerUrl");
        System.clearProperty("mobile.appium.server.url");
        System.clearProperty("web.baseUrl");
        ExecutionContext.deactivate();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("candidatesFor: canónico primero, luego aliases legacy")
    void candidatesFor_returnsCanonicalThenAliases() {
        List<String> c = LegacyKeyAliases.candidatesFor("mobile.appiumServerUrl");
        assertThat(c).containsExactly("mobile.appiumServerUrl", "mobile.appium.server.url");
    }

    @Test
    @DisplayName("candidatesFor: key sin alias devuelve singleton")
    void candidatesFor_keyWithoutAliasReturnsSingleton() {
        List<String> c = LegacyKeyAliases.candidatesFor("mobile.platform");
        assertThat(c).containsExactly("mobile.platform");
    }

    @Test
    @DisplayName("candidatesFor(null) devuelve lista vacía defensivamente")
    void candidatesFor_nullReturnsEmpty() {
        assertThat(LegacyKeyAliases.candidatesFor(null)).isEmpty();
    }

    // ── 1. alias hit ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("alias hit: pide canónico, source tiene solo el alias legacy → resuelve")
    void aliasHit_resolvesViaLegacyKey() {
        loader = new DefaultConfigLoader(List.of(
                stubSource(Map.of("mobile.appium.server.url", "http://legacy:4723"))
        ));

        MobileConfig m = loader.load(MobileConfig.class);
        assertThat(m.appiumServerUrl()).isEqualTo("http://legacy:4723");
    }

    // ── 2. alias miss ────────────────────────────────────────────────────────

    @Test
    @DisplayName("alias miss: key sin alias y sin valor → defaults")
    void aliasMiss_returnsDefaultWhenNoSourceAndNoAlias() {
        loader = new DefaultConfigLoader(List.of(stubSource(Map.of())));
        // mobile.platform NO tiene alias en la tabla; sin valor → default "android"
        assertThat(loader.load(MobileConfig.class).platform()).isEqualTo("android");
        // Y para getRaw de key inventada también
        assertThat(loader.getRaw("nope.no.alias.exists")).isEmpty();
    }

    // ── 3. no shadowing ──────────────────────────────────────────────────────

    @Test
    @DisplayName("no shadowing: canónico tiene precedencia sobre alias en el mismo source")
    void noShadowing_canonicalWinsOverLegacyInSameSource() {
        loader = new DefaultConfigLoader(List.of(
                stubSource(new LinkedHashMap<>(Map.of(
                        "mobile.appiumServerUrl",     "http://CANONICAL:4723",
                        "mobile.appium.server.url",   "http://LEGACY:4723"
                )))
        ));
        assertThat(loader.load(MobileConfig.class).appiumServerUrl())
                .isEqualTo("http://CANONICAL:4723");
    }

    @Test
    @DisplayName("no shadowing entre sources: SysProp con key dotted no preempta canónico de stub posterior")
    void noShadowing_sysPropDottedDoesNotPreemptCanonical() {
        // Stub con key canónica
        ConfigSource canonicalStub = stubSource(Map.of("mobile.appiumServerUrl", "http://STUB-CANONICAL:4723"));
        // SysProp con key legacy
        System.setProperty("mobile.appium.server.url", "http://SYSPROP-LEGACY:4723");

        loader = new DefaultConfigLoader(List.of(
                new SystemPropertySource(),
                canonicalStub
        ));

        // SysProp tiene mayor prioridad → debe ganar (vía alias)
        assertThat(loader.load(MobileConfig.class).appiumServerUrl())
                .isEqualTo("http://SYSPROP-LEGACY:4723");
    }

    // ── 4. flujo BE→ExecutionContext con keys dotted ─────────────────────────

    @Test
    @DisplayName("BE manda dotted vía ExecutionContext, Core pide canónico → resuelve via alias")
    void executionContext_dottedKey_resolvedViaAlias() {
        // Simula el ExecutionConfigBuilder del BE empujando keys dotted
        ExecutionConfig cfg = new ExecutionConfig.Builder()
                .property("mobile.appium.server.url", "http://BE-SUPPLIED:4723")
                .property("mobile.device.id",         "BE-DEVICE-1")
                .build();
        ExecutionContext.builder().config(cfg).build().activate();

        loader = new DefaultConfigLoader(List.of(
                new ExecutionContextSource()
        ));

        MobileConfig m = loader.load(MobileConfig.class);
        assertThat(m.appiumServerUrl()).isEqualTo("http://BE-SUPPLIED:4723");
        assertThat(m.deviceId()).isEqualTo("BE-DEVICE-1");
    }

    @Test
    @DisplayName("base.url legacy (sin web. prefix) resuelve a web.baseUrl")
    void baseUrlLegacyAliasWithoutWebPrefix() {
        loader = new DefaultConfigLoader(List.of(stubSource(Map.of("base.url", "https://legacy.example.com"))));
        // web.baseUrl tiene 2 aliases: web.base.url y base.url
        // El loader pide web.baseUrl al cargar WebConfig → matchea base.url
        assertThat(loader.load(com.qa.common.api.config.WebConfig.class).baseUrl())
                .isEqualTo("https://legacy.example.com");
    }

    @Test
    @DisplayName("HTTP engine via legacy key 'http.client'")
    void httpEngineLegacyAlias() {
        loader = new DefaultConfigLoader(List.of(stubSource(Map.of("http.client", "APACHE"))));
        HttpConfig http = loader.load(HttpConfig.class);
        assertThat(http.engine().name()).isEqualTo("APACHE");
    }

    // -------------------------------------------------------------------------

    private static ConfigSource stubSource(Map<String, String> map) {
        return new ConfigSource() {
            @Override public Optional<String> get(String key) {
                return Optional.ofNullable(map.get(key));
            }
            @Override public String name() { return "Stub"; }
        };
    }
}
