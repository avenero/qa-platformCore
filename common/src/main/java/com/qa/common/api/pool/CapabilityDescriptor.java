package com.qa.common.api.pool;
import com.qa.common.utils.security.SecurityUtilities;

import java.util.Map;
import java.util.Objects;

/**
 * Descriptor inmutable del estado en tiempo de ejecución de un recurso gestionado por un
 * {@link CapabilityRegistry}.
 *
 * <p>Representa la vista dinámica de una capacidad: no solo qué es el recurso, sino si está
 * libre, en uso, y cuándo podría liberarse. Esto permite al backend presentar al usuario la
 * disponibilidad real antes de lanzar una ejecución.
 *
 * <h2>Diferencia con {@code com.qa.common.api.driver.CapabilityDescriptor}</h2>
 * <p>{@code driver.CapabilityDescriptor} es la declaración <em>estática</em> de lo que un plugin
 * soporta (id, nombre, descripción breve). Este record, {@code pool.CapabilityDescriptor}, es la
 * vista <em>dinámica</em> de runtime con estado de disponibilidad real para la gestión del pool.
 *
 * <h2>Ejemplos por plataforma</h2>
 * <pre>
 * // Browser siempre disponible (web)
 * new CapabilityDescriptor("chromium", "Chromium", "WEB", "BROWSER", true)
 *
 * // Dispositivo móvil en uso con ETA
 * new CapabilityDescriptor(
 *     "pixel7", "Pixel 7 (Android 13)", "ANDROID", "REAL",
 *     false, true, 45L, Map.of("os_version", "13", "screen", "1080x2400")
 * )
 * </pre>
 *
 * <h2>Tipos de recurso ({@code type})</h2>
 * <ul>
 *   <li>{@code "BROWSER"} — navegador web (chromium, firefox, webkit)</li>
 *   <li>{@code "REAL"} — dispositivo físico</li>
 *   <li>{@code "EMULATOR"} — emulador/simulador</li>
 *   <li>{@code "DATABASE"} — motor de base de datos</li>
 * </ul>
 *
 * @param id                     identificador técnico único dentro del registry (ej: {@code "pixel7"})
 * @param name                   nombre legible para el usuario (ej: {@code "Pixel 7 (Android 13)"})
 * @param platformId             plataforma propietaria (ej: {@code "ANDROID"}, {@code "WEB"})
 * @param type                   tipo de recurso: {@code "REAL"}, {@code "EMULATOR"}, {@code "BROWSER"}, {@code "DATABASE"}
 * @param available              {@code true} si el recurso puede ser reservado ahora mismo
 * @param inUse                  {@code true} si hay una sesión activa usando este recurso
 * @param estimatedFreeInSeconds segundos estimados hasta que el recurso quede libre; {@code null} si desconocido
 * @param metadata               metadatos adicionales específicos de la plataforma (os_version, screen, etc.)
 *
 * @see CapabilityRegistry
 * @see DeviceHandle
 * @since 3.1.0
 */
public record CapabilityDescriptor(
    String id,
    String name,
    String platformId,
    String type,
    boolean available,
    boolean inUse,
    Long estimatedFreeInSeconds,
    Map<String, String> metadata
) {

    /** Validación compacta del constructor canónico. */
    public CapabilityDescriptor {
        Objects.requireNonNull(id, "id no puede ser null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede ser vacío");
        }
        Objects.requireNonNull(name, "name no puede ser null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name no puede ser vacío");
        }
        Objects.requireNonNull(platformId, "platformId no puede ser null");
        if (platformId.isBlank()) {
            throw new IllegalArgumentException("platformId no puede ser vacío");
        }
        Objects.requireNonNull(type, "type no puede ser null");
        if (type.isBlank()) {
            throw new IllegalArgumentException("type no puede ser vacío");
        }
        // Copia defensiva para garantizar inmutabilidad
        metadata = (metadata == null) ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Factory para el caso común: recurso disponible, sin sesión activa, sin ETA, sin metadatos.
     *
     * @param id         identificador técnico
     * @param name       nombre legible
     * @param platformId plataforma propietaria
     * @param type       tipo de recurso
     * @return descriptor marcado como disponible y sin contexto de sesión
     */
    public static CapabilityDescriptor available(String id, String name,
                                                 String platformId, String type) {
        return new CapabilityDescriptor(id, name, platformId, type, true, false, null, Map.of());
    }

    /**
     * Factory para recurso en uso con ETA opcional.
     *
     * @param id                     identificador técnico
     * @param name                   nombre legible
     * @param platformId             plataforma propietaria
     * @param type                   tipo de recurso
     * @param estimatedFreeInSeconds segundos estimados hasta que quede libre; puede ser {@code null}
     * @return descriptor marcado como no disponible y en uso
     */
    public static CapabilityDescriptor inUse(String id, String name, String platformId,
                                             String type, Long estimatedFreeInSeconds) {
        return new CapabilityDescriptor(
            id, name, platformId, type, false, true, estimatedFreeInSeconds, Map.of());
    }
}
