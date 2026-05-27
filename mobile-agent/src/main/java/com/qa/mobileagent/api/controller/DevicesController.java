package com.qa.mobileagent.api.controller;

import com.qa.common.api.transport.MobileDeviceDescriptor;
import com.qa.mobileagent.service.DeviceDiscoveryQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Wire-protocol v1 — {@code GET /v1/devices}: enumera devices descubiertos
 * por el agente (ADB scan + xcrun) en formato {@link MobileDeviceDescriptor}.
 *
 * <p>Consumido por el BE (TASK-K03COV-BE7 lado BE — bloqueado, ver
 * {@code initiatives/K03COV.md} §BE-COV-7) para alimentar un picker en el FE.
 *
 * <p>Si el discovery lanza excepción (ej. ADB no instalado en el host del
 * agente, sin permisos) devolvemos {@code 503 Service Unavailable} para que el
 * BE pueda degradar gracefully a "manual deviceId required" sin tirar 500.
 *
 * @since TASK-K03COV-BE7 (parte agente)
 */
@RestController
@RequestMapping("/v1")
public class DevicesController {

    private static final Logger LOG = LoggerFactory.getLogger(DevicesController.class);
    private static final int HTTP_503_SERVICE_UNAVAILABLE = 503;

    private final DeviceDiscoveryQueryService service;

    public DevicesController(DeviceDiscoveryQueryService service) {
        this.service = service;
    }

    @GetMapping("/devices")
    public ResponseEntity<List<MobileDeviceDescriptor>> devices() {
        try {
            return ResponseEntity.ok(service.listAvailable());
        } catch (RuntimeException e) {
            LOG.warn("Device discovery failed on agent: {}", e.getMessage());
            return ResponseEntity.status(HTTP_503_SERVICE_UNAVAILABLE).build();
        }
    }
}
