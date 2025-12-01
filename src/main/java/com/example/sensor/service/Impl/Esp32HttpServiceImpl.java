package com.example.sensor.service.Impl;

import com.example.sensor.config.Esp32Config;
import com.example.sensor.exceptions.SerialCommunicationException;
import com.example.sensor.model.dto.Esp32CountResponseDTO;
import com.example.sensor.model.dto.Esp32EnrollResponseDTO;
import com.example.sensor.model.dto.Esp32VerifyResponseDTO;
import com.example.sensor.service.SerialService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de SerialService que se comunica con el ESP32 vía HTTP.
 * Permite despliegue en la nube usando Cloudflare Tunnel o ngrok.
 */
@Service
@Slf4j
@RequiredArgsConstructor 
public class Esp32HttpServiceImpl implements SerialService {

    private final WebClient esp32WebClient;
    private final Esp32Config config;

    @Override
    public String sendCommand(String command) throws Exception {
        log.debug("Enviando comando simple al ESP32: {}", command);

        try {
            if (command.equals("PING")) {
                return ping();
            } else if (command.equals("COUNT")) {
                return getCount();
            } else if (command.startsWith("DELETE ")) {
                String id = command.substring(7).trim();
                return deleteFingerprint(id);
            } else if (command.equals("EMPTY")) {
                return emptyDatabase();
            } else {
                throw new IllegalArgumentException("Comando no soportado: " + command);
            }
        } catch (WebClientResponseException e) {
            log.error("Error HTTP del ESP32: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SerialCommunicationException("Error comunicando con ESP32: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error enviando comando al ESP32: {}", e.getMessage());
            throw new SerialCommunicationException("Error de comunicación: " + e.getMessage());
        }
    }

    @Override
    public List<String> sendCommandWithProgress(String command) throws Exception {
        log.info(">>> Enviando comando con progreso al ESP32: [{}]", command);

        try {
            if (command.equals("ENROLL")) {
                return enrollFingerprint();
            } else if (command.equals("VERIFY")) {
                return verifyFingerprint();
            } else {
                throw new IllegalArgumentException("Comando con progreso no soportado: " + command);
            }
        } catch (WebClientResponseException e) {
            log.error("Error HTTP del ESP32: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SerialCommunicationException("Error comunicando con ESP32: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error enviando comando al ESP32: {}", e.getMessage());
            throw new SerialCommunicationException("Error de comunicación: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        try {
            ping();
            return true;
        } catch (Exception e) {
            log.warn("ESP32 no responde al ping: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Health check del ESP32
     */
    private String ping() {
        log.debug("Haciendo ping al ESP32...");

        String response = esp32WebClient.get()
                .uri("/api/fingerprint/ping")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(config.getQuickTimeout()))
                .retryWhen(Retry.fixedDelay(config.getMaxRetries(), Duration.ofMillis(500)))
                .block();

        log.debug("ESP32 respondió: {}", response);
        return response != null ? response : "PONG";
    }

    /**
     * Obtener conteo de huellas
     */
    private String getCount() {
        log.info("Consultando conteo de huellas al ESP32...");

        Esp32CountResponseDTO response = esp32WebClient.get()
                .uri("/api/fingerprint/count")
                .retrieve()
                .bodyToMono(Esp32CountResponseDTO.class)
                .timeout(Duration.ofMillis(config.getQuickTimeout()))
                .retryWhen(Retry.fixedDelay(config.getMaxRetries(), Duration.ofMillis(500)))
                .block();

        if (response != null) {
            String result = String.format("Sensor contains %d templates", response.getCount());
            log.info("<<< ESP32: {}", result);
            return result;
        }

        throw new SerialCommunicationException("No se recibió respuesta del conteo");
    }

    /**
     * Eliminar una huella por ID
     */
    private String deleteFingerprint(String id) {
        log.info("Eliminando huella ID {} en el ESP32...", id);

        String response = esp32WebClient.delete()
                .uri("/api/fingerprint/{id}", id)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(config.getQuickTimeout()))
                .retryWhen(Retry.fixedDelay(config.getMaxRetries(), Duration.ofMillis(500)))
                .block();

        log.info("<<< ESP32: {}", response);
        return response != null ? response : "Deleted!";
    }

    /**
     * Vaciar toda la base de datos del sensor
     */
    private String emptyDatabase() {
        log.info("Vaciando base de datos del sensor...");

        String response = esp32WebClient.delete()
                .uri("/api/fingerprint/empty")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(config.getQuickTimeout()))
                .retryWhen(Retry.fixedDelay(config.getMaxRetries(), Duration.ofMillis(500)))
                .block();

        log.info("<<< ESP32: {}", response);
        return response != null ? response : "Database emptied!";
    }

    /**
     * Enrollar una nueva huella (proceso largo)
     */
    private List<String> enrollFingerprint() {
        log.info("Iniciando proceso de enroll en ESP32...");

        Esp32EnrollResponseDTO response = esp32WebClient.post()
                .uri("/api/fingerprint/enroll")
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Esp32EnrollResponseDTO.class)
                .timeout(Duration.ofMillis(config.getReadTimeout()))
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error HTTP enrollando: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.error(new SerialCommunicationException("Error en enroll: " + e.getMessage()));
                })
                .block();

        if (response == null) {
            throw new SerialCommunicationException("No se recibió respuesta del enroll");
        }

        log.info("<<< ESP32: Enroll completado con status: {}", response.getStatus());

        List<String> messages = new ArrayList<>();

        if (response.getMessages() != null) {
            messages.addAll(response.getMessages());
        }

        if ("success".equalsIgnoreCase(response.getStatus())) {
            messages.add(response.getId().toString());
            log.info("Huella enrollada exitosamente con ID: {}", response.getId());
        } else {
            String error = response.getError() != null ? response.getError() : "Unknown error";
            messages.add("Error: " + error);
            log.error("Fallo en enroll: {}", error);
        }

        return messages;
    }

    /**
     * Verificar una huella
     */
    private List<String> verifyFingerprint() {
        log.info("Iniciando verificación de huella en ESP32...");

        Esp32VerifyResponseDTO response = esp32WebClient.post()
                .uri("/api/fingerprint/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Esp32VerifyResponseDTO.class)
                .timeout(Duration.ofMillis(config.getReadTimeout()))
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error HTTP verificando: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.error(new SerialCommunicationException("Error en verify: " + e.getMessage()));
                })
                .block();

        if (response == null) {
            throw new SerialCommunicationException("No se recibió respuesta de la verificación");
        }

        List<String> messages = new ArrayList<>();
        messages.add(response.getMessage());

        if (Boolean.TRUE.equals(response.getFound())) {
            String matchMessage = String.format("Found ID #%d with confidence of %d",
                    response.getId(), response.getConfidence());
            messages.add(matchMessage);
            log.info("<<< ESP32: {}", matchMessage);
        } else {
            log.info("<<< ESP32: {}", response.getMessage());
        }

        return messages;
    }

    /**
     * Escanear tarjeta RFID (espera física de tarjeta)
     * Llama al ESP32 para que espere una tarjeta y devuelve el UID detectado
     */
    public String scanRfidCard() {
        log.info("Solicitando escaneo de tarjeta RFID al ESP32...");

        // Create DTO simple para la respuesta
        var response = esp32WebClient.post()
                .uri("/api/rfid/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(RfidScanResponse.class)
                .timeout(Duration.ofMillis(config.getReadTimeout()))
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error HTTP escaneando RFID: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.error(new SerialCommunicationException("Error en RFID scan: " + e.getMessage()));
                })
                .block();

        if (response == null) {
            throw new SerialCommunicationException("No se recibió respuesta del escaneo RFID");
        }

        if (!Boolean.TRUE.equals(response.getSuccess())) {
            throw new SerialCommunicationException(
                    response.getMessage() != null ? response.getMessage() : "No se detectó tarjeta");
        }

        log.info("<<< ESP32: Tarjeta detectada con UID: {}", response.getUid());
        return response.getUid();
    }

    /**
     * DTO interno para parsear respuesta del ESP32 RFID scan
     */
    @Data
    private static class RfidScanResponse {
        private Boolean success;
        private String uid;
        private String message;
    }
}