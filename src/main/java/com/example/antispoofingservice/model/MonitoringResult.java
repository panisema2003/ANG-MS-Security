package com.example.antispoofingservice.model;

import java.time.LocalDateTime;

public class MonitoringResult {
    private String microserviceName;
    private LocalDateTime timestamp;
    private boolean spoofingDetected;
    private String message;

    public MonitoringResult(String microserviceName, boolean spoofingDetected, String message) {
        this.microserviceName = microserviceName;
        this.timestamp = LocalDateTime.now();
        this.spoofingDetected = spoofingDetected;
        this.message = message;
    }

    // Getters
    public String getMicroserviceName() {
        return microserviceName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isSpoofingDetected() {
        return spoofingDetected;
    }

    public String getMessage() {
        return message;
    }

    // Setters (opcional, si necesitas modificarlos después de la creación)
    public void setMicroserviceName(String microserviceName) {
        this.microserviceName = microserviceName;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setSpoofingDetected(boolean spoofingDetected) {
        this.spoofingDetected = spoofingDetected;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}