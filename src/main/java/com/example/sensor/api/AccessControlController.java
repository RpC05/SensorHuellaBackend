package com.example.sensor.api;

import com.example.sensor.model.dto.*;
import com.example.sensor.service.AccessControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/access")
@RequiredArgsConstructor
@Slf4j
public class AccessControlController {
    
    private final AccessControlService accessService;
    
    @PostMapping("/cards")
    public ResponseEntity<RfidCardResponseDTO> registerCard(
            @Valid @RequestBody RfidCardRequestDTO requestDTO) {
        log.info("POST /access/cards - Registrando tarjeta: {}", requestDTO.getCardUid());
        RfidCardResponseDTO response = accessService.registerCard(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/cards")
    public ResponseEntity<List<RfidCardResponseDTO>> getAllCards() {
        List<RfidCardResponseDTO> cards = accessService.getAllCards();
        return ResponseEntity.ok(cards);
    }
    
    @GetMapping("/cards/uid/{cardUid}")
    public ResponseEntity<RfidCardResponseDTO> getCardByUid(@PathVariable String cardUid) {
        RfidCardResponseDTO card = accessService.getCardByUid(cardUid);
        return ResponseEntity.ok(card);
    }
    
    @PutMapping("/cards/{id}")
    public ResponseEntity<RfidCardResponseDTO> updateCard(
            @PathVariable Integer id,
            @Valid @RequestBody RfidCardRequestDTO requestDTO) {
        log.info("PUT /access/cards/{} - Actualizando tarjeta", id);
        RfidCardResponseDTO updated = accessService.updateCard(id, requestDTO);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/cards/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Integer id) {
        log.info("DELETE /access/cards/{} - Eliminando tarjeta", id);
        accessService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/cards/{id}/toggle-auth")
    public ResponseEntity<Void> toggleAuthorization(@PathVariable Integer id) {
        log.info("PATCH /access/cards/{}/toggle-auth", id);
        accessService.toggleCardAuthorization(id);
        return ResponseEntity.ok().build();
    }

    // ==================== REGISTRO DE ACCESOS ====================
    
    /**
     * Endpoint principal que llama el ESP32 cuando se lee una tarjeta
     */
    @PostMapping("/register")
    public ResponseEntity<AccessRegisterResponseDTO> registerAccess(
            @Valid @RequestBody AccessRegisterRequestDTO requestDTO) {
        log.info("POST /access/register - UID: {}", requestDTO.getCardUid());
        AccessRegisterResponseDTO response = accessService.registerAccess(requestDTO);
        return ResponseEntity.ok(response);
    }

    // ==================== CONSULTA DE LOGS ====================
    
    @GetMapping("/logs")
    public ResponseEntity<List<AccessLogResponseDTO>> getAccessLogs(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        if (start == null) {
            start = LocalDateTime.now().minusDays(7);
        }
        if (end == null) {
            end = LocalDateTime.now();
        }
        
        List<AccessLogResponseDTO> logs = accessService.getAccessLogs(start, end);
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/logs/today")
    public ResponseEntity<List<AccessLogResponseDTO>> getTodayAccesses() {
        List<AccessLogResponseDTO> logs = accessService.getTodayAccesses();
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/logs/card/{cardUid}")
    public ResponseEntity<List<AccessLogResponseDTO>> getAccessLogsByCard(
            @PathVariable String cardUid) {
        List<AccessLogResponseDTO> logs = accessService.getAccessLogsByCard(cardUid);
        return ResponseEntity.ok(logs);
    }
}