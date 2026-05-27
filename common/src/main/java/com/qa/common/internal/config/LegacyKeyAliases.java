package com.qa.common.internal.config;

import com.qa.common.api.Internal;

import java.util.List;
import java.util.Map;

/**
 * Tabla estática que mapea claves canónicas camelCase (las que producen los
 * records {@code TypedConfig}) a sus equivalentes legacy con puntos
 * (las que viven en {@code config-app.properties} históricos y las que el BE
 * actualmente empuja vía {@code ExecutionConfig.properties}).
 *
 * <h2>Diseño (TASK-K03M-F7)</h2>
 *
 * <p>El blueprint original proponía {@code LegacyKeyAliasSource implements ConfigSource}
 * leyendo SOLO desde {@code PropertiesFileSource}. Análisis posterior reveló que
 * el BE empuja keys dotted al {@code ExecutionContext.properties} (verificado
 * en {@code ExecutionConfigBuilder.applyMobileConfig}: {@code "mobile.appium.server.url"},
 * {@code "mobile.device.id"}, …). Si el alias no cubre {@code ExecutionContextSource},
 * el flujo FE→BE→Core queda roto post K03-migrate hasta que K03COV-BE3 migre
 * el builder a camelCase.
 *
 * <p>Solución: alias resolution centralizado en {@link DefaultConfigLoader#lookup}.
 * Cada source recibe el conjunto de candidatos `[canónico, alias1, alias2, ...]`
 * — beneficio uniforme para SysProp/Env/Props/Yaml/ExecutionContext.
 *
 * <h2>Acoplamiento</h2>
 *
 * <p>La tabla DUPLICA strings de {@code MobileConfigKeys} y {@code WebConfigKeys}
 * (que viven en {@code mobile-core} y {@code web-core}). Es duplicación
 * intencional: {@code common} no puede importar de los módulos especializados
 * (ArchUnit guard {@code common_does_not_depend_on_specialized_modules}).
 *
 * @since TASK-K03M-F7
 */
@Internal(reason = "internal — usado por DefaultConfigLoader.lookup()")
public final class LegacyKeyAliases {

    /**
     * canonical (camelCase) → lista de aliases legacy (dotted) a probar.
     * Solo se incluyen keys donde el legacy difiere del canónico — keys idénticas
     * (ej. {@code mobile.platform}) no necesitan entrada.
     */
    private static final Map<String, List<String>> ALIASES = Map.ofEntries(
            // Mobile
            Map.entry("mobile.deviceId",                List.of("mobile.device.id")),
            Map.entry("mobile.deviceType",              List.of("mobile.device.type")),
            Map.entry("mobile.deviceName",              List.of("mobile.device.name")),
            Map.entry("mobile.platformVersion",         List.of("mobile.platform.version")),
            Map.entry("mobile.appiumServerUrl",         List.of("mobile.appium.server.url")),
            Map.entry("mobile.appiumBasePort",          List.of("mobile.appium.base.port")),
            Map.entry("mobile.appiumAutoStart",         List.of("mobile.appium.auto.start")),
            Map.entry("mobile.appiumStartupTimeout",    List.of("mobile.appium.startup.timeout.sec")),
            Map.entry("mobile.implicitWaitSec",         List.of("mobile.implicit.wait.sec")),
            Map.entry("mobile.appPackage",              List.of("mobile.app.package")),
            Map.entry("mobile.appActivity",             List.of("mobile.app.activity")),
            Map.entry("mobile.bundleId",                List.of("mobile.app.bundle.id")),
            Map.entry("mobile.appAutoLaunch",           List.of("mobile.app.auto.launch")),
            Map.entry("mobile.appNoReset",              List.of("mobile.app.no.reset")),
            Map.entry("mobile.discoveryAutoScan",       List.of("mobile.discovery.auto.scan")),
            Map.entry("mobile.discoveryIncludeVirtual", List.of("mobile.discovery.include.virtual")),
            Map.entry("mobile.discoveryIncludePhysical",List.of("mobile.discovery.include.physical")),
            // Web
            Map.entry("web.baseUrl",                    List.of("web.base.url", "base.url")),
            Map.entry("web.proxyUrl",                   List.of("web.proxy.url")),
            Map.entry("web.navigationTimeout",          List.of("web.page.load.timeout.sec")),
            Map.entry("web.explicitWait",               List.of("web.explicit.wait.sec")),
            Map.entry("web.implicitWait",               List.of("web.implicit.wait.sec")),
            // Framework
            Map.entry("framework.moduleName",           List.of("framework.module.name")),
            // HTTP
            Map.entry("http.engine",                    List.of("http.client")),
            // Reporting
            Map.entry("reporting.cucumberJsonPath",     List.of("reporting.cucumber.json.path")),
            // Database (típicas keys multi-DB, sin record para algunas)
            Map.entry("db.poolSizeMax",                 List.of("db.pool.size.max"))
    );

    private LegacyKeyAliases() {}

    /**
     * Devuelve la key pedida seguida de sus aliases legacy. Si no hay alias
     * registrado, devuelve solo la key canónica (singleton).
     *
     * <p>Garantiza orden estable: canónico siempre primero (no se "preempta" un
     * valor seteado explícitamente con la key canónica por su versión legacy).
     */
    public static List<String> candidatesFor(String canonicalKey) {
        if (canonicalKey == null) { return List.of(); }
        List<String> aliases = ALIASES.get(canonicalKey);
        if (aliases == null || aliases.isEmpty()) { return List.of(canonicalKey); }
        List<String> all = new java.util.ArrayList<>(aliases.size() + 1);
        all.add(canonicalKey);
        all.addAll(aliases);
        return java.util.List.copyOf(all);
    }
}
