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
    public ResponseEntity<RfidCardResponseDTO> registerCard() {
        log.info("POST /access/cards - Iniciando escaneo de tarjeta RFID...");
        return ResponseEntity.status(HttpStatus.CREATED).body(accessService.registerCardWithScan());
    }

    @GetMapping("/cards")
    public ResponseEntity<List<RfidCardResponseDTO>> getAllCards() {
        return ResponseEntity.ok(accessService.getAllCards());
    }

    @GetMapping("/cards/uid/{cardUid}")
    public ResponseEntity<RfidCardResponseDTO> getCardByUid(@PathVariable String cardUid) {
        return ResponseEntity.ok(accessService.getCardByUid(cardUid));
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

    @PatchMapping("/cards/{id}/toggle-active")
    public ResponseEntity<Void> toggleActiveState(@PathVariable Integer id) {
        log.info("PATCH /access/cards/{}/toggle-active", id);
        accessService.toggleCardActiveState(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<AccessRegisterResponseDTO> registerAccess(
            @Valid @RequestBody AccessRegisterRequestDTO requestDTO) {
        log.info("POST /access/register - UID: {}", requestDTO.getCardUid());
        return ResponseEntity.ok(accessService.registerAccess(requestDTO));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AccessLogResponseDTO>> getAccessLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        if (start == null)
            start = LocalDateTime.now().minusDays(7);
        if (end == null)
            end = LocalDateTime.now();
        return ResponseEntity.ok(accessService.getAccessLogs(start, end));
    }

    @GetMapping("/logs/today")
    public ResponseEntity<List<AccessLogResponseDTO>> getTodayAccesses() {
        return ResponseEntity.ok(accessService.getTodayAccesses());
    }

    @GetMapping("/logs/card/{cardUid}")
    public ResponseEntity<List<AccessLogResponseDTO>> getAccessLogsByCard(
            @PathVariable String cardUid) {
        return ResponseEntity.ok(accessService.getAccessLogsByCard(cardUid));
    }
}