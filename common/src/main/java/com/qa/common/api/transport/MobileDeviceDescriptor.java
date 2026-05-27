package com.qa.common.api.transport;

/**
 * Vista pública (wire format) de un dispositivo mobile descubierto por el agente.
 *
 * <p>Usado por el endpoint {@code GET /v1/devices} del {@code mobile-agent}
 * (TASK-K03COV-BE7) para que el FE pueda construir un picker filtrable en
 * lugar de pedir {@code deviceId} como texto libre.
 *
 * <p><b>Decisión de diseño — UDID NO se publica.</b> {@code DeviceDescriptor}
 * interno de {@code mobile-core} expone {@code udid} (PII para devices físicos
 * BYOD). Este record omite ese campo a propósito: la UI no lo necesita para
 * seleccionar un device, y filtrar PII desde el wire format es la opción más
 * segura por defecto. El UDID viaja crudo solo en el camino de ejecución
 * (request → ExecutionConfigBuilder → Appium), nunca en discovery.
 *
 * @param id        identificador del device (ej. {@code "emulator-5554"}, serial ADB).
 * @param name      nombre humano (ej. {@code "Pixel_6_API_33"}).
 * @param type      {@code com.qa.mobilecore.model.DeviceType} como string
 *                  ({@code ANDROID_EMULATOR}, {@code ANDROID_PHYSICAL},
 *                  {@code IOS_SIMULATOR}, {@code IOS_PHYSICAL}, {@code REMOTE_GRID}).
 * @param platform  plataforma normalizada ({@code "android"} | {@code "ios"}).
 * @param version   versión de la plataforma (ej. {@code "13"}, {@code "17.4"}).
 * @param status    estado operativo: {@code AVAILABLE} | {@code BUSY} | {@code OFFLINE} | {@code UNKNOWN}.
 *
 * @since TASK-K03COV-BE7
 */
public record MobileDeviceDescriptor(
        String id,
        String name,
        String type,
        String platform,
        String version,
        String status
) {}
