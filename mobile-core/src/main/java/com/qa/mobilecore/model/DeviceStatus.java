package com.qa.mobilecore.model;

/**
 * Estado operativo de un dispositivo en el {@code DevicePool}.
 *
 * @author Abel Venero
 * @since 2.0.0
 */
public enum DeviceStatus {

    /** Disponible para ser asignado a una ejecución. */
    AVAILABLE,

    /** Actualmente asignado y en uso por una ejecución. */
    BUSY,

    /** Detectado pero no accesible (desconectado, ADB offline, etc.). */
    OFFLINE,

    /** Estado desconocido (recién descubierto, aún no verificado). */
    UNKNOWN
}
