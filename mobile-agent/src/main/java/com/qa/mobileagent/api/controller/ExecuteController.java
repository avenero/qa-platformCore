package com.qa.mobileagent.api.controller;

import com.qa.mobileagent.api.dto.SubmitRequest;
import com.qa.mobileagent.api.dto.SubmitResponse;
import com.qa.mobileagent.service.AgentExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Wire-protocol v1 — {@code POST /v1/runs}: submit an execution.
 *
 * @since TASK-I04
 */
@RestController
@RequestMapping("/v1/runs")
public class ExecuteController {

    private final AgentExecutionService service;

    public ExecuteController(AgentExecutionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SubmitResponse> execute(@Valid @RequestBody SubmitRequest req) throws IOException {
        SubmitResponse resp = service.submit(req);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
    }
}
