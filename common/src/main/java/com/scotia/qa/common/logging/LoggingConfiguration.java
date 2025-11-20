package com.scotia.qa.common.logging;

import com.scotia.qa.common.http.exceptions.FrameworkTechnicalException;
import com.scotia.qa.common.utils.DataUtilities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configurador centralizado de logging para todos los frameworks.
 * Maneja la configuración de appenders, niveles y formateadores.
 *
 * @author Scotia QA Framework Team
 * @since 1.0.0
 */
public class LoggingConfiguration {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Configuración actual
    private static final Map<String, Object> currentConfig = new ConcurrentHashMap<>();
    private static boolean isConfigured = false;

    private LoggingConfiguration() {
        // Utility class - no instances
    }

    // =================================================================================
    // CONFIGURACIÓN PRINCIPAL
    // =================================================================================

    /**
     * Configura el sistema de logging con configuración por defecto.
     */
    public static void configureDefault(String framework) {
        try {
            TestLogger.logInfo("LOGGING_CONFIG",
                              String.format("Configuring default logging for framework: %s", framework), null);

            // Configuración por defecto
            String baseDir = "logs/" + framework.toLowerCase();
            String date = LocalDateTime.now().format(DATE_FORMAT);

            LoggingConfig config = LoggingConfig.builder()
                .framework(framework)
                .baseLogDirectory(baseDir)
                .enableConsoleLogging(true)
                .enableFileLogging(true)
                .logLevel("INFO")
                .datePattern(date)
                .maxFileSize("10MB")
                .maxBackupFiles(10)
                .enableStructuredLogging(true)
                .enableEvidenceLogging(true)
                .build();

            configure(config);

        } catch (Exception e) {
            TestLogger.logError("LOGGING_CONFIG",
                               String.format("Error configuring default logging: %s", e.getMessage()), null);
        }
    }

    /**
     * Configura el sistema de logging con configuración personalizada.
     */
    public static void configure(LoggingConfig config) {
        try {
            TestLogger.logInfo("LOGGING_CONFIG",
                              String.format("Configuring logging with custom configuration for framework: %s", config.getFramework()), null);

            // Crear directorios necesarios
            createLogDirectories(config);

            // Configurar appenders
            configureAppenders(config);

            // Configurar niveles
            configureLogLevels(config);

            // Configurar formateo
            configureFormatters(config);

            // Guardar configuración actual
            saveCurrentConfig(config);

            isConfigured = true;
            TestLogger.logInfo("LOGGING_CONFIG", "Logging configuration completed successfully", null);

        } catch (Exception e) {
            TestLogger.logError("LOGGING_CONFIG",
                               String.format("Error configuring logging: %s", e.getMessage()), null);
            throw new RuntimeException("Failed to configure logging", e);
        }
    }

    /**
     * Verifica si el sistema de logging está configurado.
     */
    public static boolean isConfigured() {
        return isConfigured;
    }

    /**
     * Obtiene la configuración actual.
     */
    public static Map<String, Object> getCurrentConfig() {
        return new ConcurrentHashMap<>(currentConfig);
    }

    // =================================================================================
    // CONFIGURACIÓN DE APPENDERS
    // =================================================================================

    /**
     * Configura los appenders de logging.
     */
    private static void configureAppenders(LoggingConfig config) {
        try {
            if (config.isEnableConsoleLogging()) {
                configureConsoleAppender(config);
            }

            if (config.isEnableFileLogging()) {
                configureFileAppender(config);
            }

            if (config.isEnableEvidenceLogging()) {
                configureEvidenceAppender(config);
            }

        } catch (Exception e) {
            TestLogger.logError("LOGGING_CONFIG",
                               String.format("Error configuring appenders: %s", e.getMessage()), null);
        }
    }

    /**
     * Configura el appender de consola.
     */
    private static void configureConsoleAppender(LoggingConfig config) {
        TestLogger.logDebug("LOGGING_CONFIG",
                           String.format("Configuring console appender for framework: %s", config.getFramework()), null);
        currentConfig.put("console_appender_enabled", true);
        currentConfig.put("console_pattern", getConsolePattern(config));
    }

    /**
     * Configura el appender de archivo.
     */
    private static void configureFileAppender(LoggingConfig config) {
        TestLogger.logDebug("LOGGING_CONFIG",
                           String.format("Configuring file appender for framework: %s", config.getFramework()), null);

        String logFile = String.format("%s/%s/%s-tests.log",
                                     config.getBaseLogDirectory(),
                                     config.getDatePattern(),
                                     config.getFramework().toLowerCase());

        currentConfig.put("file_appender_enabled", true);
        currentConfig.put("log_file_path", logFile);
        currentConfig.put("file_pattern", getFilePattern(config));
        currentConfig.put("max_file_size", config.getMaxFileSize());
        currentConfig.put("max_backup_files", config.getMaxBackupFiles());
    }

    /**
     * Configura el appender de evidencias.
     */
    private static void configureEvidenceAppender(LoggingConfig config) {
        TestLogger.logDebug("LOGGING_CONFIG",
                           String.format("Configuring evidence appender for framework: %s", config.getFramework()), null);

        String evidenceFile = String.format("%s/%s/%s-evidences.log",
                                           config.getBaseLogDirectory(),
                                           config.getDatePattern(),
                                           config.getFramework().toLowerCase());

        currentConfig.put("evidence_appender_enabled", true);
        currentConfig.put("evidence_file_path", evidenceFile);
        currentConfig.put("evidence_pattern", getEvidencePattern(config));
    }

    // =================================================================================
    // CONFIGURACIÓN DE NIVELES
    // =================================================================================

    /**
     * Configura los niveles de logging.
     */
    private static void configureLogLevels(LoggingConfig config) {
        TestLogger.logDebug("LOGGING_CONFIG",
                           String.format("Configuring log levels: %s", config.getLogLevel()), null);

        currentConfig.put("log_level", config.getLogLevel());
        currentConfig.put("framework_log_level", config.getLogLevel());

        // Configurar niveles específicos para diferentes paquetes
        currentConfig.put("cucumber_log_level", "INFO");
        currentConfig.put("evidence_log_level", "DEBUG");
        currentConfig.put("root_log_level", config.getLogLevel());
    }

    // =================================================================================
    // CONFIGURACIÓN DE FORMATEADORES
    // =================================================================================

    /**
     * Configura los formateadores de logging.
     */
    private static void configureFormatters(LoggingConfig config) {
        TestLogger.logDebug("LOGGING_CONFIG",
                           String.format("Configuring formatters for structured logging: %s", config.isEnableStructuredLogging()), null);

        currentConfig.put("structured_logging_enabled", config.isEnableStructuredLogging());
        currentConfig.put("timestamp_format", "yyyy-MM-dd HH:mm:ss.SSS");
        currentConfig.put("date_pattern", config.getDatePattern());
    }

    /**
     * Obtiene el patrón para consola.
     */
    private static String getConsolePattern(LoggingConfig config) {
        if (config.isEnableStructuredLogging()) {
            return "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{framework}][%X{scenario}] %logger{36} - %msg%n";
        } else {
            return "%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n";
        }
    }

    /**
     * Obtiene el patrón para archivo.
     */
    private static String getFilePattern(LoggingConfig config) {
        return "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{framework}][%X{feature}][%X{scenario}] %logger{36} - %msg%n";
    }

    /**
     * Obtiene el patrón para evidencias.
     */
    private static String getEvidencePattern(LoggingConfig config) {
        return "%d{yyyy-MM-dd HH:mm:ss.SSS} [EVIDENCE] [%X{framework}][%X{feature}][%X{scenario}] - %msg%n";
    }

    // =================================================================================
    // UTILIDADES
    // =================================================================================

    /**
     * Crea los directorios de logging necesarios.
     */
    private static void createLogDirectories(LoggingConfig config) throws FrameworkTechnicalException {
        String baseDir = config.getBaseLogDirectory();
        String dateDir = baseDir + "/" + config.getDatePattern();

        DataUtilities.createDirectoryIfNotExists(baseDir);
        DataUtilities.createDirectoryIfNotExists(dateDir);

        TestLogger.logDebug("LOGGING_CONFIG",
                           String.format("Created log directories: %s", dateDir), null);
    }

    /**
     * Guarda la configuración actual.
     */
    private static void saveCurrentConfig(LoggingConfig config) {
        currentConfig.put("framework", config.getFramework());
        currentConfig.put("base_directory", config.getBaseLogDirectory());
        currentConfig.put("date_pattern", config.getDatePattern());
        currentConfig.put("configured_at", LocalDateTime.now().toString());
    }

    /**
     * Genera un reporte de configuración.
     */
    public static String generateConfigurationReport() {
        if (!isConfigured) {
            return "Logging system is not configured yet.";
        }

        StringBuilder report = new StringBuilder();
        report.append("=== LOGGING CONFIGURATION REPORT ===\n");

        currentConfig.forEach((key, value) -> {
            report.append(String.format("%-25s: %s\n", key, value));
        });

        return report.toString();
    }

    // =================================================================================
    // CLASE DE CONFIGURACIÓN
    // =================================================================================

    /**
     * Clase para configuración de logging.
     */
    public static class LoggingConfig {
        private String framework;
        private String baseLogDirectory;
        private boolean enableConsoleLogging;
        private boolean enableFileLogging;
        private String logLevel;
        private String datePattern;
        private String maxFileSize;
        private int maxBackupFiles;
        private boolean enableStructuredLogging;
        private boolean enableEvidenceLogging;

        // Constructor privado
        private LoggingConfig() {}

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getFramework() { return framework; }
        public String getBaseLogDirectory() { return baseLogDirectory; }
        public boolean isEnableConsoleLogging() { return enableConsoleLogging; }
        public boolean isEnableFileLogging() { return enableFileLogging; }
        public String getLogLevel() { return logLevel; }
        public String getDatePattern() { return datePattern; }
        public String getMaxFileSize() { return maxFileSize; }
        public int getMaxBackupFiles() { return maxBackupFiles; }
        public boolean isEnableStructuredLogging() { return enableStructuredLogging; }
        public boolean isEnableEvidenceLogging() { return enableEvidenceLogging; }

        // Builder class
        public static class Builder {
            private LoggingConfig config = new LoggingConfig();

            public Builder framework(String framework) {
                config.framework = framework;
                return this;
            }

            public Builder baseLogDirectory(String baseLogDirectory) {
                config.baseLogDirectory = baseLogDirectory;
                return this;
            }

            public Builder enableConsoleLogging(boolean enableConsoleLogging) {
                config.enableConsoleLogging = enableConsoleLogging;
                return this;
            }

            public Builder enableFileLogging(boolean enableFileLogging) {
                config.enableFileLogging = enableFileLogging;
                return this;
            }

            public Builder logLevel(String logLevel) {
                config.logLevel = logLevel;
                return this;
            }

            public Builder datePattern(String datePattern) {
                config.datePattern = datePattern;
                return this;
            }

            public Builder maxFileSize(String maxFileSize) {
                config.maxFileSize = maxFileSize;
                return this;
            }

            public Builder maxBackupFiles(int maxBackupFiles) {
                config.maxBackupFiles = maxBackupFiles;
                return this;
            }

            public Builder enableStructuredLogging(boolean enableStructuredLogging) {
                config.enableStructuredLogging = enableStructuredLogging;
                return this;
            }

            public Builder enableEvidenceLogging(boolean enableEvidenceLogging) {
                config.enableEvidenceLogging = enableEvidenceLogging;
                return this;
            }

            public LoggingConfig build() {
                // Validaciones
                if (config.framework == null || config.framework.trim().isEmpty()) {
                    throw new IllegalArgumentException("Framework is required");
                }
                if (config.baseLogDirectory == null || config.baseLogDirectory.trim().isEmpty()) {
                    config.baseLogDirectory = "logs";
                }
                if (config.logLevel == null || config.logLevel.trim().isEmpty()) {
                    config.logLevel = "INFO";
                }
                if (config.datePattern == null || config.datePattern.trim().isEmpty()) {
                    config.datePattern = LocalDateTime.now().format(DATE_FORMAT);
                }
                if (config.maxFileSize == null || config.maxFileSize.trim().isEmpty()) {
                    config.maxFileSize = "10MB";
                }
                if (config.maxBackupFiles <= 0) {
                    config.maxBackupFiles = 10;
                }

                return config;
            }
        }
    }
}
