package com.qa.common.internal.reporting.config;
import com.qa.common.api.Internal;
import com.qa.common.api.reporter.config.ExtentConfig;

import com.qa.common.api.config.ConfigLoader;
import com.qa.common.api.config.ConfigLoaderHolder;
import com.qa.common.api.config.ExtentReportConfig;
import com.qa.common.api.logging.TestLogger;
import com.qa.common.internal.config.ExtentSystemInfoLoader;

import java.util.Map;
import java.util.Properties;

/**
 * Configuración unificada (POJO interno del subsistema de reporting) consumida
 * por el pipeline {@code ReportingManager → ConversionStep → ExtentGenerationStep}.
 *
 * <p>Wrap los records {@link com.qa.common.api.config.ReportingConfig} y
 * {@link ExtentReportConfig} en una estructura mutable que el pipeline existente
 * puede modificar (p. ej. agregar systemInfo dinámicamente). La factory
 * {@link #load()} (TASK-K03M-M9) reemplaza al antiguo {@code fromConfigManager()}
 * y delega en {@link ConfigLoaderHolder} (TypedConfig + escape hatch {@code getRaw}).
 *
 * <p>Las integraciones externas (Jira/Xray) se gestionan desde el Backend, no
 * desde el Core. El Core sólo genera reportes locales (Extent).
 */
@Internal

public class MutableReportingConfig {

    private boolean enabled;
    private String environment;
    private ExtentConfig extent;

    public MutableReportingConfig() {
        this.extent = new ExtentConfig();
        this.enabled = true;
    }

    /**
     * Carga configuración desde {@link ConfigLoaderHolder} usando los records
     * tipados {@link com.qa.common.api.config.ReportingConfig} y
     * {@link ExtentReportConfig}, más {@link ExtentSystemInfoLoader} para el
     * scan dinámico {@code extent.systemInfo.*}.
     */
    public static MutableReportingConfig load() {
        ConfigLoader loader = ConfigLoaderHolder.get();
        com.qa.common.api.config.ReportingConfig api =
                loader.load(com.qa.common.api.config.ReportingConfig.class);
        ExtentReportConfig extentApi = loader.load(ExtentReportConfig.class);

        TestLogger.logInfo("REPORTING_CONFIG", "📄 Cargando configuración de reporting...", null);

        MutableReportingConfig config = new MutableReportingConfig();
        config.setEnabled(api.enabled());
        config.setEnvironment(api.environment());

        ExtentConfig ext = config.getExtent();
        ext.setEnabled(extentApi.enabled());
        ext.setOutputPath(extentApi.outputPath());
        ext.setReportName(extentApi.reportName());
        ext.setDocumentTitle(extentApi.documentTitle());

        // Si el usuario seteó extent.reportTitle explícitamente lo respetamos;
        // si no, computamos "Automated Tests - {moduleName}" como hacía el legacy.
        String explicitTitle = loader.getRaw("extent.reportTitle")
                .filter(v -> !v.isBlank()).orElse(null);
        if (explicitTitle != null) {
            ext.setReportTitle(explicitTitle);
        } else {
            String moduleName = loader.getRaw("framework.moduleName", "PLATFORM");
            ext.setReportTitle("Automated Tests - " + moduleName);
        }

        ext.setTheme(extentApi.theme());
        ext.setIncludeScreenshots(extentApi.includeScreenshots());
        ext.setIncludeSystemInfo(extentApi.includeSystemInfo());
        ext.setIncludeTimeline(extentApi.includeTimeline());

        Map<String, String> systemInfo = ExtentSystemInfoLoader.load(loader);
        systemInfo.forEach(ext::addSystemInfo);

        TestLogger.logInfo("REPORTING_CONFIG", "✅ Configuración cargada — Reporting: " + config.isEnabled()
            + " | Extent: " + ext.isEnabled(), null);
        return config;
    }

    /**
     * Carga configuración desde un objeto {@link Properties}.
     *
     * @param props propiedades de configuración
     * @return configuración cargada
     */
    public static MutableReportingConfig fromProperties(Properties props) {
        MutableReportingConfig config = new MutableReportingConfig();
        config.setEnabled(Boolean.parseBoolean(props.getProperty("reporting.enabled", "true")));
        config.setEnvironment(props.getProperty("reporting.environment", "local"));
        config.getExtent().setEnabled(Boolean.parseBoolean(props.getProperty("extent.enabled", "true")));
        config.getExtent().setOutputPath(props.getProperty("extent.outputPath", "build/reports/extent/"));
        return config;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public ExtentConfig getExtent() {
        return extent;
    }

    public void setExtent(ExtentConfig extent) {
        this.extent = extent;
    }

    @Override
    public String toString() {
        return "MutableReportingConfig{enabled=" + enabled + ", environment='" + environment + "', extent=" + extent + '}';
    }
}
