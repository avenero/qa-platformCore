package com.qa.mobileagent.api.controller;

import com.qa.common.driver.CapabilityReport;
import com.qa.mobileagent.service.AgentExecutionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Wire-protocol v1 — {@code GET /v1/capabilities}: enumera las capabilities
 * que el agente puede atender (devices, plataformas, motores).
 *
 * <p>Usado por {@code BE.transportRouter} (TASK-I03) para decidir routing.
 *
 * @since TASK-I04
 */
@RestController
@RequestMapping("/v1")
public class CapabilitiesController {

    private final AgentExecutionService service;

    public CapabilitiesController(AgentExecutionService service) {
        this.service = service;
    }

    @GetMapping("/capabilities")
    public List<CapabilityReport> capabilities() {
        return service.capabilities();
    }
}
