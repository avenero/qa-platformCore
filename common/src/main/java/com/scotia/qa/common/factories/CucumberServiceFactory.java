package com.scotia.qa.common.factories;

import com.scotia.qa.common.interfaces.CucumberHooksService;
import com.scotia.qa.common.implementations.BaseCucumberHooksService;
import com.scotia.qa.common.logging.TestLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory para crear instancias de CucumberHooksService del framework Scotia QA.
 *
 * <p>Esta factory proporciona métodos estáticos para crear instancias de CucumberHooksService
 * preconfiguradas para diferentes tipos de frameworks (API, Web, Mobile).
 * Facilita la configuración automática y reutilización de servicios optimizados.
 *
 * <p><b>Características principales:</b>
 * <ul>
 *   <li>Creación estandarizada de servicios de hooks de Cucumber</li>
 *   <li>Preconfiguración específica por tipo de framework</li>
 *   <li>Cache de instancias para reutilización thread-safe</li>
 *   <li>Configuraciones predefinidas para casos comunes</li>
 *   <li>Callbacks automáticos según el framework</li>
 *   <li>Logging integrado para troubleshooting</li>
 * </ul>
 *
 * <p><b>Frameworks soportados:</b>
 * <ul>
 *   <li><b>API</b> - Configurado para testing de APIs REST</li>
 *   <li><b>WEB</b> - Configurado para testing de aplicaciones web</li>
 *   <li><b>MOBILE</b> - Configurado para testing de aplicaciones móviles</li>
 *   <li><b>CUSTOM</b> - Configuración personalizada</li>
 * </ul>
 *
 * <p><b>Uso típico:</b>
 * <pre>
 * // Crear para API testing
 * CucumberHooksService apiHooks = CucumberServiceFactory.getInstance("API");
 *
 * // Crear para Web testing
 * CucumberHooksService webHooks = CucumberServiceFactory.getInstance("WEB");
 *
 * // Crear configurado para entorno específico
 * CucumberHooksService hooksService = CucumberServiceFactory.getConfiguredInstance("API", true);
 *
 * // Usar en hooks de Cucumber
 * public class ApiCucumberHooks {
 *     private static CucumberHooksService hooksService = CucumberServiceFactory.getInstance("API");
 *
 *     &#64;Before
 *     public void beforeScenario(Scenario scenario) {
 *         hooksService.executeBeforeScenario(scenario.getName(), getFeatureName(scenario));
 *     }
 * }
 * </pre>
 *
 * <p><b>Configuraciones automáticas por framework:</b>
 * <ul>
 *   <li><b>API</b> - Auto-evidencia habilitada, callbacks para HTTP, logging de requests</li>
 *   <li><b>WEB</b> - Screenshots automáticos, cleanup de drivers, performance tracking</li>
 *   <li><b>MOBILE</b> - Screenshots de app, state capture, cleanup de sessions</li>
 * </ul>
 *
 * @author Scotia QA Framework Team
 * @version 1.0.0
 * @since 2.0.0
 * @see CucumberHooksService
 * @see BaseCucumberHooksService
 */
public final class CucumberServiceFactory {

    private static final TestLogger.LoggerWrapper log = TestLogger.getLogger(CucumberServiceFactory.class);

    // Cache thread-safe de instancias por framework
    private static final Map<String, CucumberHooksService> instanceCache = new ConcurrentHashMap<>();

    // Constructor privado para factory
    private CucumberServiceFactory() {
        throw new UnsupportedOperationException("CucumberServiceFactory es una clase factory");
    }

    // =================================================================================
    // MÉTODOS PRINCIPALES DE CREACIÓN
    // =================================================================================

    /**
     * Crea una instancia de CucumberHooksService para el tipo de framework especificado.
     * Utiliza configuraciones optimizadas por defecto según el framework.
     *
     * @param frameworkType tipo de framework ("API", "WEB", "MOBILE")
     * @return nueva instancia de CucumberHooksService configurada
     * @throws IllegalArgumentException si frameworkType es null, vacío o no soportado
     */
    public static CucumberHooksService getInstance(String frameworkType) {
        validateFrameworkType(frameworkType);

        String normalizedType = frameworkType.toUpperCase().trim();
        log.debug("Creando CucumberHooksService para framework: {}", normalizedType);

        BaseCucumberHooksService hooksService = new BaseCucumberHooksService();
        hooksService.setFrameworkType(normalizedType);

        // Configurar según el tipo de framework
        configureForFramework(hooksService, normalizedType);

        log.debug("CucumberHooksService creado para framework: {}", normalizedType);
        return hooksService;
    }

    /**
     * Crea una instancia de CucumberHooksService con configuración avanzada.
     *
     * @param frameworkType tipo de framework
     * @param autoEvidenceEnabled habilitar captura automática de evidencias
     * @return CucumberHooksService configurado
     */
    public static CucumberHooksService getConfiguredInstance(String frameworkType, boolean autoEvidenceEnabled) {
        CucumberHooksService hooksService = getInstance(frameworkType);
        hooksService.setAutoEvidenceCaptureEnabled(autoEvidenceEnabled);

        log.debug("CucumberHooksService configurado - Framework: {}, AutoEvidence: {}",
            frameworkType, autoEvidenceEnabled);
        return hooksService;
    }

    /**
     * Crea una instancia singleton cacheada para el framework especificado.
     * Útil cuando se necesita la misma instancia en múltiples clases.
     *
     * @param frameworkType tipo de framework
     * @return instancia singleton para el framework
     */
    public static CucumberHooksService getSingletonInstance(String frameworkType) {
        validateFrameworkType(frameworkType);

        String normalizedType = frameworkType.toUpperCase().trim();
        return instanceCache.computeIfAbsent(normalizedType, type -> {
            log.debug("Creando instancia singleton para framework: {}", type);
            return getInstance(type);
        });
    }

    /**
     * Crea una instancia específica para API testing.
     * Incluye configuraciones optimizadas para testing de APIs REST.
     *
     * @return CucumberHooksService configurado para API testing
     */
    public static CucumberHooksService getApiInstance() {
        log.debug("Creando CucumberHooksService para API testing");
        BaseCucumberHooksService hooksService = (BaseCucumberHooksService) getInstance("API");

        // Configuraciones específicas para APIs
        Map<String, Object> apiProperties = Map.of(
            "captureHttpRequests", true,
            "captureHttpResponses", true,
            "logApiPerformance", true,
            "validateJsonSchemas", true
        );
        hooksService.configureFrameworkProperties(apiProperties);

        log.debug("CucumberHooksService para API creado con configuraciones optimizadas");
        return hooksService;
    }

    /**
     * Crea una instancia específica para Web testing.
     * Incluye configuraciones optimizadas para testing de aplicaciones web.
     *
     * @return CucumberHooksService configurado para Web testing
     */
    public static CucumberHooksService getWebInstance() {
        log.debug("Creando CucumberHooksService para Web testing");
        BaseCucumberHooksService hooksService = (BaseCucumberHooksService) getInstance("WEB");

        // Configuraciones específicas para Web
        Map<String, Object> webProperties = Map.of(
            "captureScreenshots", true,
            "captureDomSnapshots", true,
            "trackPagePerformance", true,
            "validateAccessibility", false
        );
        hooksService.configureFrameworkProperties(webProperties);

        log.debug("CucumberHooksService para Web creado con configuraciones optimizadas");
        return hooksService;
    }

    /**
     * Crea una instancia específica para Mobile testing.
     * Incluye configuraciones optimizadas para testing de aplicaciones móviles.
     *
     * @return CucumberHooksService configurado para Mobile testing
     */
    public static CucumberHooksService getMobileInstance() {
        log.debug("Creando CucumberHooksService para Mobile testing");
        BaseCucumberHooksService hooksService = (BaseCucumberHooksService) getInstance("MOBILE");

        // Configuraciones específicas para Mobile
        Map<String, Object> mobileProperties = Map.of(
            "captureScreenshots", true,
            "captureAppState", true,
            "trackAppPerformance", true,
            "recordSessionVideos", false
        );
        hooksService.configureFrameworkProperties(mobileProperties);

        log.debug("CucumberHooksService para Mobile creado con configuraciones optimizadas");
        return hooksService;
    }

    /**
     * Crea una instancia con configuración completamente personalizada.
     *
     * @param config configuración personalizada
     * @return CucumberHooksService según la configuración especificada
     */
    public static CucumberHooksService getCustomInstance(CucumberServiceConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("CucumberServiceConfig no puede ser null");
        }

        log.debug("Creando CucumberHooksService con configuración personalizada");

        CucumberHooksService hooksService = getInstance(config.getFrameworkType());
        hooksService.setAutoEvidenceCaptureEnabled(config.isAutoEvidenceEnabled());

        if (config.getCustomProperties() != null && !config.getCustomProperties().isEmpty()) {
            hooksService.configureFrameworkProperties(config.getCustomProperties());
        }

        if (config.getInitializationCallback() != null) {
            hooksService.setFrameworkInitializationCallback(config.getInitializationCallback());
        }

        if (config.getCleanupCallback() != null) {
            hooksService.setFrameworkCleanupCallback(config.getCleanupCallback());
        }

        if (config.getGlobalSetupCallback() != null) {
            hooksService.setGlobalSetupCallback(config.getGlobalSetupCallback());
        }

        if (config.getGlobalTeardownCallback() != null) {
            hooksService.setGlobalTeardownCallback(config.getGlobalTeardownCallback());
        }

        log.debug("CucumberHooksService personalizado creado exitosamente");
        return hooksService;
    }

    // =================================================================================
    // MÉTODOS DE UTILIDAD Y VERIFICACIÓN
    // =================================================================================

    /**
     * Verifica si un tipo de framework es soportado.
     *
     * @param frameworkType tipo de framework a verificar
     * @return true si es soportado, false en caso contrario
     */
    public static boolean isSupportedFrameworkType(String frameworkType) {
        if (frameworkType == null || frameworkType.trim().isEmpty()) {
            return false;
        }

        String normalizedType = frameworkType.toUpperCase().trim();
        return "API".equals(normalizedType) ||
               "WEB".equals(normalizedType) ||
               "MOBILE".equals(normalizedType) ||
               "CUSTOM".equals(normalizedType);
    }

    /**
     * Obtiene los tipos de framework soportados.
     *
     * @return array con los tipos de framework soportados
     */
    public static String[] getSupportedFrameworkTypes() {
        return new String[]{"API", "WEB", "MOBILE", "CUSTOM"};
    }

    /**
     * Limpia el cache de instancias singleton.
     * Útil para testing o cuando se necesita reiniciar configuraciones.
     */
    public static void clearCache() {
        int oldSize = instanceCache.size();
        instanceCache.clear();
        log.debug("Cache de instancias limpiado: {} instancias removidas", oldSize);
    }

    /**
     * Obtiene información de debug sobre las capacidades del factory.
     *
     * @return string con información del factory
     */
    public static String getFactoryInfo() {
        return String.format(
            "CucumberServiceFactory v1.0.0 - Implementación: %s - Frameworks soportados: %s - Cache: %d instancias",
            BaseCucumberHooksService.class.getSimpleName(),
            String.join(", ", getSupportedFrameworkTypes()),
            instanceCache.size()
        );
    }

    // =================================================================================
    // MÉTODOS PRIVADOS DE UTILIDAD
    // =================================================================================

    /**
     * Valida que el tipo de framework sea válido.
     */
    private static void validateFrameworkType(String frameworkType) {
        if (frameworkType == null || frameworkType.trim().isEmpty()) {
            throw new IllegalArgumentException("Framework type no puede ser null o vacío");
        }

        if (!isSupportedFrameworkType(frameworkType)) {
            throw new IllegalArgumentException(
                String.format("Tipo de framework no soportado: %s. Tipos soportados: %s",
                    frameworkType, String.join(", ", getSupportedFrameworkTypes()))
            );
        }
    }

    /**
     * Configura el servicio según el tipo de framework.
     */
    private static void configureForFramework(BaseCucumberHooksService hooksService, String frameworkType) {
        // Configuraciones comunes
        hooksService.setAutoEvidenceCaptureEnabled(true);

        // Configuraciones específicas por framework
        switch (frameworkType) {
            case "API":
                // Para API: focus en requests/responses y performance
                log.debug("Aplicando configuración para framework API");
                break;

            case "WEB":
                // Para Web: focus en screenshots y DOM
                log.debug("Aplicando configuración para framework WEB");
                break;

            case "MOBILE":
                // Para Mobile: focus en screenshots y app state
                log.debug("Aplicando configuración para framework MOBILE");
                break;

            case "CUSTOM":
                // Para Custom: configuración mínima
                log.debug("Aplicando configuración mínima para framework CUSTOM");
                hooksService.setAutoEvidenceCaptureEnabled(false);
                break;

            default:
                log.warn("Tipo de framework no reconocido para configuración: {}", frameworkType);
        }
    }

    // =================================================================================
    // CLASE INTERNA PARA CONFIGURACIÓN PERSONALIZADA
    // =================================================================================

    /**
     * Clase de configuración para crear servicios personalizados de Cucumber.
     * Patrón Builder para facilitar la configuración de múltiples opciones.
     */
    public static class CucumberServiceConfig {
        private String frameworkType = "CUSTOM";
        private boolean autoEvidenceEnabled = true;
        private Map<String, Object> customProperties;
        private Runnable initializationCallback;
        private Runnable cleanupCallback;
        private Runnable globalSetupCallback;
        private Runnable globalTeardownCallback;

        public CucumberServiceConfig(String frameworkType) {
            this.frameworkType = frameworkType;
        }

        // Builder methods
        public CucumberServiceConfig withAutoEvidence(boolean enabled) {
            this.autoEvidenceEnabled = enabled;
            return this;
        }

        public CucumberServiceConfig withCustomProperties(Map<String, Object> properties) {
            this.customProperties = properties;
            return this;
        }

        public CucumberServiceConfig withInitializationCallback(Runnable callback) {
            this.initializationCallback = callback;
            return this;
        }

        public CucumberServiceConfig withCleanupCallback(Runnable callback) {
            this.cleanupCallback = callback;
            return this;
        }

        public CucumberServiceConfig withGlobalSetupCallback(Runnable callback) {
            this.globalSetupCallback = callback;
            return this;
        }

        public CucumberServiceConfig withGlobalTeardownCallback(Runnable callback) {
            this.globalTeardownCallback = callback;
            return this;
        }

        // Getters
        public String getFrameworkType() { return frameworkType; }
        public boolean isAutoEvidenceEnabled() { return autoEvidenceEnabled; }
        public Map<String, Object> getCustomProperties() { return customProperties; }
        public Runnable getInitializationCallback() { return initializationCallback; }
        public Runnable getCleanupCallback() { return cleanupCallback; }
        public Runnable getGlobalSetupCallback() { return globalSetupCallback; }
        public Runnable getGlobalTeardownCallback() { return globalTeardownCallback; }
    }
}
