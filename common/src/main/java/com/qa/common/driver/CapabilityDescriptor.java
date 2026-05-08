package com.qa.common.driver;

/**
 * Describe una capacidad concreta que un plugin soporta (browser, plataforma, motor de BD, etc.).
 *
 * <p>Es un descriptor inmutable con tres campos esenciales que el FE puede presentar
 * al usuario en un selector de plataforma/dispositivo.
 *
 * <p><b>Nota de diseño (TASK-C03):</b> Este record es un placeholder que será expandido
 * con metadatos adicionales (icono, versiones soportadas, restricciones de entorno, etc.)
 * cuando se implemente {@code TASK-C03 — CapabilityRegistry + descriptors}. La firma
 * actual ({@code id, name, description}) no cambiará — solo se agregarán campos opcionales.
 *
 * <h2>Ejemplos por plugin</h2>
 * <pre>
 * // WebPlugin
 * new CapabilityDescriptor("chromium", "Chromium", "Chromium-based browser (default)")
 * new CapabilityDescriptor("firefox",  "Firefox",  "Mozilla Firefox")
 *
 * // DatabasePlugin
 * new CapabilityDescriptor("mysql",      "MySQL",      "MySQL 8.x")
 * new CapabilityDescriptor("postgresql", "PostgreSQL", "PostgreSQL 14+")
 * </pre>
 *
 * @param id          identificador técnico único dentro del plugin (snake_case, sin espacios)
 * @param name        nombre legible para el usuario
 * @param description descripción breve de la capacidad
 *
 * @author Abel Venero
 * @since 3.0.0
 * @see CapabilityReport
 */
public record CapabilityDescriptor(
    String id,
    String name,
    String description
) {
    public CapabilityDescriptor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("CapabilityDescriptor.id no puede ser null ni vacío");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("CapabilityDescriptor.name no puede ser null ni vacío");
        }
    }
}
