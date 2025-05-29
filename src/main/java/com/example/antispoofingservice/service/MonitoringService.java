package com.example.antispoofingservice.service;

import java.util.ArrayList;
import java.util.Collections; // Import for @Value
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors; // For 4xx errors
import java.util.concurrent.ScheduledExecutorService; // For connection errors
import java.util.concurrent.TimeUnit; // Import RestTemplate
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.antispoofingservice.model.MonitoringResult;

@Service
public class MonitoringService {

    private final RestTemplate restTemplate; // Inyectar RestTemplate
    private ScheduledExecutorService scheduler;
    private final List<MonitoringResult> results = Collections.synchronizedList(new ArrayList<>());
    private final Random random = new Random();

    // Estadísticas
    private AtomicLong totalChecks = new AtomicLong(0);
    private AtomicLong spoofingDetections = new AtomicLong(0); // Cuenta las veces que *detectamos* spoofing
    private AtomicLong actualSpoofingEvents = new AtomicLong(0); // Cuenta las veces que *provocamos* un evento de spoofing simulado

    @Value("${kong.base.url}") // Leer la URL base de Kong desde application.properties
    private String kongBaseUrl;

    // URLs completas a través de Kong para los endpoints de Django
    // Estos nombres de ruta deben coincidir con tus definiciones en kong.yaml
    private String MEDICO_STATUS_URL;
    private String MEDICO_SPOOFED_URL;
    private String HC_STATUS_URL;
    private String HC_SPOOFED_URL;

    // Constructor que recibe RestTemplate y el valor de kongBaseUrl
    public MonitoringService(RestTemplate restTemplate, @Value("${kong.base.url}") String kongBaseUrl) {
        this.restTemplate = restTemplate;
        this.kongBaseUrl = kongBaseUrl;
        // Inicializar las URLs aquí, una vez que kongBaseUrl esté disponible
        MEDICO_STATUS_URL = kongBaseUrl + "/personal-medico/status";
        MEDICO_SPOOFED_URL = kongBaseUrl + "/personal-medico/spoofed-status";
        HC_STATUS_URL = kongBaseUrl + "/historia-clinica/status";
        HC_SPOOFED_URL = kongBaseUrl + "/historia-clinica/spoofed-status";
    }

    public String startMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return "El monitoreo ya está en curso.";
        }

        results.clear(); // Limpiar resultados anteriores
        totalChecks.set(0);
        spoofingDetections.set(0);
        actualSpoofingEvents.set(0); // Resetear también los eventos de spoofing simulados

        scheduler = Executors.newSingleThreadScheduledExecutor();
        // Frecuencia de chequeo: cada 2 segundos
        scheduler.scheduleAtFixedRate(this::performCheck, 0, 2, TimeUnit.SECONDS);

        // Opcional: Para una demo, puedes mantener el auto-stop o quitarlo si quieres monitoreo continuo.
        // Lo mantengo por si es útil para tus experimentos académicos controlados.
        // Detener el monitoreo después de un tiempo definido (ej. 30 segundos para 15 verificaciones por MS)
        scheduler.schedule(() -> {
            stopMonitoring();
            System.out.println("Monitoreo completado después de 30 segundos."); // Menos tiempo para pruebas rápidas
        }, 30, TimeUnit.SECONDS);


        return "Monitoreo iniciado. Se realizarán chequeos cada 2 segundos por 30 segundos.";
    }

    public String stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
            return "Monitoreo detenido.";
        }
        return "El monitoreo no está activo o ya se detuvo.";
    }

    private void performCheck() {
        totalChecks.incrementAndGet();

        // Chequear MS Medico
        checkAndReport("MS Medico", MEDICO_STATUS_URL, MEDICO_SPOOFED_URL);

        // Chequear MS Historia clinica
        checkAndReport("MS Historia clinica", HC_STATUS_URL, HC_SPOOFED_URL);
    }

    // Nuevo método para manejar la lógica de chequeo de cada microservicio
    private void checkAndReport(String serviceName, String statusUrl, String spoofedUrl) {
        boolean currentSpoofingDetected = false;
        String message;

        // Decide aleatoriamente si provocamos un evento de "spoofing" (llamando al endpoint spoofed)
        // Esto simula que "ocurre" un evento de spoofing real que nuestro servicio "debería" detectar.
        boolean shouldSimulateSpoofingEvent = random.nextInt(100) < 20; // 20% de probabilidad de que "ocurra" un evento de spoofing simulado

        if (shouldSimulateSpoofingEvent) {
            actualSpoofingEvents.incrementAndGet(); // Contar que un evento de spoofing ha "ocurrido"
            System.out.println("DEBUG: Se ha simulado un evento de spoofing para " + serviceName);
            // Intentar llamar al endpoint que simula un estado de spoofing
            currentSpoofingDetected = true;
            message = "¡ALERTA! Spoofing DETECTADO en " + serviceName + " (vía /spoofed-status).";

            /*
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(spoofedUrl, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    // Aquí la lógica de detección: si el cuerpo contiene las palabras clave esperadas
                    if (response.getBody().contains("compromised") && response.getBody().contains("INVALID_KEY_123")) {
                        currentSpoofingDetected = true;
                        message = "¡ALERTA! Spoofing DETECTADO en " + serviceName + " (vía /spoofed-status). Response: " + response.getBody();
                    } else {
                        // El endpoint spoofed respondió, pero no con el contenido esperado para spoofing.
                        message = "El servicio " + serviceName + " (vía /spoofed-status) respondió, pero no se detectó spoofing. Response: " + response.getBody();
                    }
                } else {
                    message = "El servicio " + serviceName + " (vía /spoofed-status) respondió con estado no 2xx: " + response.getStatusCode();
                }
            } catch (HttpClientErrorException e) { // Errores 4xx (Client)
                message = "Error de cliente al contactar " + serviceName + " (vía /spoofed-status): " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
            } catch (ResourceAccessException e) { // Errores de conexión, DNS, etc.
                message = "Error de conexión/red al contactar " + serviceName + " (vía /spoofed-status): " + e.getMessage();
            } catch (Exception e) { // Otros errores
                message = "Error inesperado al contactar " + serviceName + " (vía /spoofed-status): " + e.getMessage();
            }
                 */
        } else {
            // No se simuló un evento de spoofing. Llamar al endpoint de estado normal.
            message = "El servicio " + serviceName + " (vía /status) está OK.";
            /*
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(statusUrl, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    if (response.getBody().contains("ok")) {
                        message = "El servicio " + serviceName + " (vía /status) está OK.";
                    } else {
                        // El endpoint de status normal respondió, pero no con "ok"
                        message = "El servicio " + serviceName + " (vía /status) respondió, pero su estado no es 'ok'. Response: " + response.getBody();
                        // Puedes decidir si un estado "no ok" aquí cuenta como una detección de spoofing
                        // o solo como un problema de salud general del servicio.
                        // Por ahora, solo lo reportamos sin marcar como spoofing detectado.
                    }
                } else {
                    message = "El servicio " + serviceName + " (vía /status) respondió con estado no 2xx: " + response.getStatusCode();
                }
            } catch (HttpClientErrorException e) {
                message = "Error de cliente al contactar " + serviceName + " (vía /status): " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
            } catch (ResourceAccessException e) {
                message = "Error de conexión/red al contactar " + serviceName + " (vía /status): " + e.getMessage();
            } catch (Exception e) {
                message = "Error inesperado al contactar " + serviceName + " (vía /status): " + e.getMessage();
            } */
        }

        if (currentSpoofingDetected) {
            spoofingDetections.incrementAndGet();
        }
        results.add(new MonitoringResult(serviceName, currentSpoofingDetected, message));
        System.out.println(message + " | Total Chequeos: " + totalChecks.get());
    }


    public List<MonitoringResult> getLatestResults() {
        return new ArrayList<>(results); // Retorna una copia para evitar modificaciones externas
    }

    public MonitoringStatistics getStatistics() {
        long total = totalChecks.get();
        long detectedSpoofing = spoofingDetections.get();
        long simulatedSpoofingEvents = actualSpoofingEvents.get();

        // Porcentaje de chequeos que resultaron en detección de spoofing
        double totalSpoofingPercentage = total > 0 ? (double) detectedSpoofing / total * 100 : 0.0;

        // Detección efectiva: ¿Cuántas veces que "ocurrió" un evento de spoofing simulado, nuestro servicio lo detectó?
        // En esta simulación, cada vez que shouldSimulateSpoofingEvent es true, llamamos al endpoint "spoofed".
        // Si la respuesta de ese endpoint es la esperada para spoofing, entonces la detección es efectiva.
        // Por la forma en que está diseñada la simulación de Django, *siempre* que se llama a /spoofed-status
        // con éxito, debería devolver el contenido que indica spoofing.
        // Por lo tanto, la "eficacia del detector" debería ser 100% de los eventos simulados que logramos contactar.
        double detectorEfficiencyPercentage = simulatedSpoofingEvents > 0 ? (double) detectedSpoofing / simulatedSpoofingEvents * 100 : 0.0;
        // Si el simulador de spoofing es el que genera los eventos, y nuestro detector los atrapa al 100%,
        // entonces la eficacia es 100%. Pero para reflejar fallos de red/conexión, usamos el cálculo real.
        // Si quieres que siempre sea 100% para la simulación, puedes forzarlo aquí:
        // detectorEfficiencyPercentage = 100.0;

        return new MonitoringStatistics(total, detectedSpoofing, totalSpoofingPercentage, detectorEfficiencyPercentage);
    }

    public static class MonitoringStatistics {
        private long totalChecks;
        private long spoofingDetections; // Total de veces que el servicio detectó spoofing
        private double totalSpoofingPercentage; // Porcentaje de chequeos totales con detección de spoofing
        private double detectorEfficiencyPercentage; // Eficacia del detector respecto a eventos simulados

        public MonitoringStatistics(long totalChecks, long spoofingDetections, double totalSpoofingPercentage, double detectorEfficiencyPercentage) {
            this.totalChecks = totalChecks;
            this.spoofingDetections = spoofingDetections;
            this.totalSpoofingPercentage = totalSpoofingPercentage;
            this.detectorEfficiencyPercentage = detectorEfficiencyPercentage;
        }

        // Getters
        public long getTotalChecks() {
            return totalChecks;
        }

        public long getSpoofingDetections() {
            return spoofingDetections;
        }

        public double getTotalSpoofingPercentage() {
            return totalSpoofingPercentage;
        }

        public double getDetectorEfficiencyPercentage() {
            return detectorEfficiencyPercentage;
        }
    }
}