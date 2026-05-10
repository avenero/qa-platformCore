package com.qa.mobileagent.api.controller;

import com.qa.mobileagent.service.AgentExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wire-protocol v1 — {@code POST /v1/runs/&#123;id&#125;/cancel}: cancela
 * idempotentemente la ejecución (RFC-AGENT-01 §4.3).
 *
 * @since TASK-I04
 */
@RestController
@RequestMapping("/v1/runs")
public class CancelController {

    private final AgentExecutionService service;

    public CancelController(AgentExecutionService service) {
        this.service = service;
    }

    @PostMapping("/{executionId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable String executionId) {
        boolean found = service.cancel(executionId);
        return found ? ResponseEntity.accepted().build() : ResponseEntity.notFound().build();
    }
}
