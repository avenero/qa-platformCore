package com.scotia.qa.common.interfaces;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.List;

/**
 * Interface que define el contrato para servicios de configuración del framework Scotia QA.
 *
 * Esta interface proporciona métodos para leer configuraciones desde diferentes formatos
 * de archivos (Properties, YAML, JSON) y gestionar la configuración base del framework,
 * incluyendo hosts y endpoints.
 *
 * Características principales:
 * - Lectura unificada de archivos de configuración
 * - Gestión de host base para peticiones HTTP
 * - Soporte para Properties, YAML y JSON
 * - Navegación jerárquica en estructuras YAML/JSON
 * - Cache de configuraciones para performance
 *
 * @author Scotia QA Framework Team
 * @version 1.0.0
 * @since 2024
 */
public interface ConfigurationService {

    // =================================================================================
    // SECCIÓN: GESTIÓN DE HOST BASE
    // =================================================================================

    /**
     * Establece el host base para las peticiones HTTP del framework.
     *
     * @param host URL base del servicio (ej: "https://api.example.com")
     */
    void setHost(String host);

    /**
     * Obtiene el host base actualmente configurado.
     *
     * @return Host base configurado o null si no se ha establecido
     */
    String getHost();

    /**
     * Verifica si hay un host configurado.
     *
     * @return true si hay un host configurado, false en caso contrario
     */
    boolean hasHost();

    /**
     * Resuelve variables de host desde diferentes fuentes de configuración.
     * Busca en este orden: System Properties → Environment Variables → hosts.properties
     *
     * @param hostVariable Variable o valor del host a resolver
     * @return Host resuelto o el valor original si no se puede resolver
     */
    String resolveHost(String hostVariable);

    // =================================================================================
    // SECCIÓN: LECTURA DE ARCHIVOS PROPERTIES
    // =================================================================================

    /**
     * Lee un archivo Properties completo desde el classpath.
     *
     * @param fileName Nombre del archivo Properties (ej: "config.properties")
     * @return Properties object envuelto en Optional, empty si el archivo no existe
     */
    Optional<Properties> readProperties(String fileName);

    /**
     * Obtiene una propiedad específica de un archivo Properties.
     *
     * @param fileName Nombre del archivo Properties
     * @param key Clave de la propiedad a leer
     * @return Valor de la propiedad o null si no se encuentra
     */
    String getProperty(String fileName, String key);

    /**
     * Obtiene una propiedad con valor por defecto si no se encuentra.
     *
     * @param fileName Nombre del archivo Properties
     * @param key Clave de la propiedad a leer
     * @param defaultValue Valor por defecto si la propiedad no existe
     * @return Valor de la propiedad o el valor por defecto
     */
    String getProperty(String fileName, String key, String defaultValue);

    /**
     * Obtiene todas las propiedades de un archivo como un Map.
     *
     * @param fileName Nombre del archivo Properties
     * @return Map con todas las propiedades o Map vacío si el archivo no existe
     */
    Map<String, String> getAllProperties(String fileName);

    // =================================================================================
    // SECCIÓN: LECTURA DE ARCHIVOS YAML
    // =================================================================================

    /**
     * Lee un archivo YAML completo desde el classpath.
     *
     * @param fileName Nombre del archivo YAML (ej: "config.yml" o "config.yaml")
     * @return Map con la estructura YAML envuelto en Optional, empty si el archivo no existe
     */
    Optional<Map<String, Object>> readYaml(String fileName);

    /**
     * Obtiene una propiedad específica de un archivo YAML usando notación punto.
     *
     * @param fileName Nombre del archivo YAML
     * @param path Path de la propiedad usando notación punto (ej: "endpoints.login.url")
     * @return Valor de la propiedad como String o null si no se encuentra
     */
    String getYamlProperty(String fileName, String path);

    /**
     * Obtiene una propiedad YAML con valor por defecto.
     *
     * @param fileName Nombre del archivo YAML
     * @param path Path de la propiedad usando notación punto
     * @param defaultValue Valor por defecto si la propiedad no existe
     * @return Valor de la propiedad o el valor por defecto
     */
    String getYamlProperty(String fileName, String path, String defaultValue);

    /**
     * Obtiene un nodo completo de un archivo YAML.
     *
     * @param fileName Nombre del archivo YAML
     * @param path Path del nodo usando notación punto (ej: "endpoints.auth")
     * @return Map con el nodo YAML envuelto en Optional, empty si no se encuentra
     */
    Optional<Map<String, Object>> getYamlNode(String fileName, String path);

    /**
     * Obtiene un array de un archivo YAML.
     *
     * @param fileName Nombre del archivo YAML
     * @param path Path del array usando notación punto
     * @return Lista con los elementos del array, empty si no se encuentra
     */
    List<Object> getYamlArray(String fileName, String path);

    // =================================================================================
    // SECCIÓN: LECTURA DE ARCHIVOS JSON
    // =================================================================================

    /**
     * Lee un archivo JSON completo desde el classpath.
     *
     * @param fileName Nombre del archivo JSON (ej: "config.json")
     * @return Map con la estructura JSON envuelto en Optional, empty si el archivo no existe
     */
    Optional<Map<String, Object>> readJson(String fileName);

    /**
     * Obtiene una propiedad específica de un archivo JSON usando notación punto.
     *
     * @param fileName Nombre del archivo JSON
     * @param path Path de la propiedad usando notación punto
     * @return Valor de la propiedad como String o null si no se encuentra
     */
    String getJsonProperty(String fileName, String path);

    /**
     * Obtiene una propiedad JSON con valor por defecto.
     *
     * @param fileName Nombre del archivo JSON
     * @param path Path de la propiedad usando notación punto
     * @param defaultValue Valor por defecto si la propiedad no existe
     * @return Valor de la propiedad o el valor por defecto
     */
    String getJsonProperty(String fileName, String path, String defaultValue);

    /**
     * Obtiene un nodo completo de un archivo JSON.
     *
     * @param fileName Nombre del archivo JSON
     * @param path Path del nodo usando notación punto
     * @return Map con el nodo JSON envuelto en Optional, empty si no se encuentra
     */
    Optional<Map<String, Object>> getJsonNode(String fileName, String path);

    // =================================================================================
    // SECCIÓN: UTILIDADES Y VERIFICACIONES
    // =================================================================================

    /**
     * Verifica si un archivo de configuración existe en el classpath.
     *
     * @param fileName Nombre del archivo a verificar
     * @return true si el archivo existe, false en caso contrario
     */
    boolean fileExists(String fileName);

    /**
     * Verifica si un archivo específico tiene contenido válido.
     *
     * @param fileName Nombre del archivo a verificar
     * @return true si el archivo existe y tiene contenido válido, false en caso contrario
     */
    boolean isFileValid(String fileName);

    /**
     * Obtiene información de debug sobre las configuraciones cargadas.
     *
     * @return String con información de debug (archivos cargados, cache, etc.)
     */
    String getDebugInfo();

    /**
     * Limpia el cache de configuraciones para forzar recarga desde archivos.
     */
    void clearCache();

    /**
     * Precarga configuraciones específicas en el cache para mejor performance.
     *
     * @param fileNames Lista de nombres de archivos a precargar
     */
    void preloadConfigurations(List<String> fileNames);

    // =================================================================================
    // SECCIÓN: GESTIÓN DE ENTORNOS
    // =================================================================================

    /**
     * Establece el entorno actual (dev, test, staging, prod).
     * Esto afecta la búsqueda de archivos de configuración específicos del entorno.
     *
     * @param environment Nombre del entorno
     */
    void setEnvironment(String environment);

    /**
     * Obtiene el entorno actual configurado.
     *
     * @return Entorno actual o "default" si no se ha configurado uno específico
     */
    String getEnvironment();

    /**
     * Obtiene una propiedad considerando el entorno actual.
     * Busca primero en config-{environment}.properties, luego en config.properties.
     *
     * @param key Clave de la propiedad
     * @return Valor de la propiedad o null si no se encuentra
     */
    String getEnvironmentProperty(String key);

    /**
     * Obtiene una propiedad YAML considerando el entorno actual.
     * Busca primero en config-{environment}.yml, luego en config.yml.
     *
     * @param path Path de la propiedad usando notación punto
     * @return Valor de la propiedad o null si no se encuentra
     */
    String getEnvironmentYamlProperty(String path);
}
