package com.example.antispoofingservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.antispoofingservice.model.MonitoringResult;
import com.example.antispoofingservice.service.MonitoringService;

@RestController
//@RequestMapping("seguridad")
@CrossOrigin(origins = "*") // Permite peticiones desde cualquier origen (para la UI)
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/start")
    public ResponseEntity<String> startMonitoring() {
        String message = monitoringService.startMonitoring();
        return ResponseEntity.ok(message);
    }

    @GetMapping("/stop")
    public ResponseEntity<String> stopMonitoring() {
        String message = monitoringService.stopMonitoring();
        return ResponseEntity.ok(message);
    }

    @GetMapping("/latest-results")
    public ResponseEntity<List<MonitoringResult>> getLatestResults() {
        List<MonitoringResult> results = monitoringService.getLatestResults();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/statistics")
    public ResponseEntity<MonitoringService.MonitoringStatistics> getStatistics() {
        MonitoringService.MonitoringStatistics stats = monitoringService.getStatistics();
        return ResponseEntity.ok(stats);
    }
}