package com.example.sensor.api;

import com.example.sensor.model.dto.EnrollProgressDTO;
import com.example.sensor.model.dto.FingerPrintRequestDTO;
import com.example.sensor.model.dto.FingerPrintResponseDTO;
import com.example.sensor.model.dto.FingerPrintVerifyResponseDTO;
import com.example.sensor.service.FingerPrintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fingerprints")
@RequiredArgsConstructor
@Slf4j
public class FingerPrintController {
    private final FingerPrintService fingerprintService;

    @GetMapping
    public ResponseEntity<List<FingerPrintResponseDTO>> getAllFingerprints() {
        List<FingerPrintResponseDTO> fingerprints = fingerprintService.findAll();
        return ResponseEntity.ok(fingerprints);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FingerPrintResponseDTO> getFingerprintById(@PathVariable Integer id) {
        FingerPrintResponseDTO fingerprint = fingerprintService.findById(id);
        return ResponseEntity.ok(fingerprint);
    }

    @PostMapping
    public ResponseEntity<EnrollProgressDTO> enrollFingerprint(
            @RequestBody(required = false) FingerPrintRequestDTO requestDto) {
        log.info("POST /api/fingerprints - Iniciando proceso de enroll de huella");
        // Crear DTO vacío si no se envió body
        if (requestDto == null) {
            requestDto = new FingerPrintRequestDTO();
        }
        EnrollProgressDTO result = fingerprintService.enrollFingerprint(requestDto);

        if ("SUCCESS".equals(result.getStatus())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFingerprint(@PathVariable Integer id) {
        log.info("DELETE /api/fingerprints/{} - Eliminando huella", id);
        fingerprintService.deleteFingerprint(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<FingerPrintVerifyResponseDTO> verifyFingerprint() {
        log.info("POST /fingerprints/verify - Verificando huella");
        FingerPrintVerifyResponseDTO result = fingerprintService.verifyFingerprint();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> getCount() {
        log.info("GET /fingerprints/count - Contando huellas");
        Integer count = fingerprintService.getCount();
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/empty")
    public ResponseEntity<Void> emptyDatabase() {
        log.info("DELETE /fingerprints/empty - Vaciando base de datos del sensor");
        fingerprintService.emptyDatabase();
        return ResponseEntity.noContent().build();
    }
}