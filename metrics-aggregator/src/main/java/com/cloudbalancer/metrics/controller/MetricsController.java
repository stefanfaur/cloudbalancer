package com.cloudbalancer.metrics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * Placeholder metrics controller. Will be fully implemented in Task 5.10.
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    @GetMapping("/workers")
    public ResponseEntity<List<?>> getWorkers() {
        return ResponseEntity.ok(Collections.emptyList());
    }
}
