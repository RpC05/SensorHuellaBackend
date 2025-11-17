package com.example.sensor.service.Impl;

import com.example.sensor.exceptions.FingerPrintException;
import com.example.sensor.exceptions.FingerPrintNotFoundException;
import com.example.sensor.mapper.FingerPrintMapper;
import com.example.sensor.model.dto.EnrollProgressDTO;
import com.example.sensor.model.dto.FingerPrintRequestDTO;
import com.example.sensor.model.dto.FingerPrintResponseDTO;
import com.example.sensor.model.dto.FingerPrintVerifyResponseDTO;
import com.example.sensor.model.entity.FingerPrint;
import com.example.sensor.repository.FingerPrintRepository;
import com.example.sensor.service.FingerPrintService;
import com.example.sensor.service.SerialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FingerPrintServiceImpl implements FingerPrintService {
    private final FingerPrintRepository repository;
    private final SerialService serialService;
    private final FingerPrintMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<FingerPrintResponseDTO> findAll() {
        return repository.findAllByActiveTrue()
                .stream()
                .map(mapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FingerPrintResponseDTO findById(Integer id) {
        FingerPrint fingerprint = repository.findById(id)
                .orElseThrow(() -> new FingerPrintNotFoundException(id));
        return mapper.toResponseDto(fingerprint);
    }

    @Override
    public EnrollProgressDTO enrollFingerprint(FingerPrintRequestDTO requestDto) {
        try {
            // Buscar siguiente ID disponible
            Integer nextId = findNextAvailableId();
            log.info("Enrollando nueva huella con ID: {}", nextId);

            // Crear entidad
            FingerPrint fingerprint = mapper.toEntity(requestDto);
            fingerprint.setFingerprintId(nextId);
            fingerprint.setActive(true);

            // Enviar comando a Arduino
            List<String> messages = serialService.sendCommandWithProgress("ENROLL:" + nextId);

            // Verificar respuesta
            String lastMessage = messages.get(messages.size() - 1);

            if (lastMessage.startsWith("SUCCESS:")) {
                // Guardar en BD
                FingerPrint saved = repository.save(fingerprint);
                log.info("Huella enrollada exitosamente: {}", saved.getId());

                return EnrollProgressDTO.builder()
                        .status("SUCCESS")
                        .messages(messages)
                        .fingerprint(mapper.toResponseDto(saved))
                        .build();
            } else {
                log.error("Error enrollando huella: {}", lastMessage);
                return EnrollProgressDTO.builder()
                        .status("ERROR")
                        .messages(messages)
                        .error(lastMessage)
                        .build();
            }

        } catch (Exception e) {
            log.error("Error enrollando huella", e);
            throw new FingerPrintException("Error al enrollar huella: " + e.getMessage());
        }
    }

    @Override
    public void deleteFingerprint(Integer id) {
        try {
            FingerPrint fingerprint = repository.findById(id)
                    .orElseThrow(() -> new FingerPrintNotFoundException(id));

            log.info("Eliminando huella ID: {} (Sensor ID: {})", id, fingerprint.getFingerprintId());

            // Enviar comando al Arduino
            String response = serialService.sendCommand("DELETE:" + fingerprint.getFingerprintId());

            if (response.startsWith("SUCCESS:")) {
                // Marcar como inactiva en BD (soft delete)
                fingerprint.setActive(false);
                repository.save(fingerprint);
                log.info("Huella eliminada exitosamente");
            } else {
                throw new FingerPrintException("Error al eliminar huella del sensor: " + response);
            }

        } catch (FingerPrintNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error eliminando huella", e);
            throw new FingerPrintException("Error al eliminar huella: " + e.getMessage());
        }
    }

    @Override
    public FingerPrintVerifyResponseDTO verifyFingerprint() {
        try {
            log.info("Verificando huella");
            String response = serialService.sendCommand("VERIFY");

            if (response.startsWith("VERIFIED:")) {
                // Parse: VERIFIED:5:95
                String[] parts = response.replace("VERIFIED:", "").split(":");
                Integer fingerprintId = Integer.parseInt(parts[0].trim());
                Integer confidence = Integer.parseInt(parts[1].trim());

                // Buscar en BD
                FingerPrint fingerprint = repository.findByFingerprintId(fingerprintId)
                        .orElseThrow(() -> new FingerPrintNotFoundException(
                                Integer.valueOf(fingerprintId)
                        ));

                log.info("Huella verificada: {} con confianza: {}", fingerprint.getName(), confidence);

                return FingerPrintVerifyResponseDTO.builder()
                        .found(true)
                        .fingerprintId(fingerprintId)
                        .confidence(confidence)
                        .userName(fingerprint.getName())
                        .message("Huella verificada exitosamente")
                        .build();

            } else if (response.equals("NOT_FOUND")) {
                log.info("Huella no encontrada");
                return FingerPrintVerifyResponseDTO.builder()
                        .found(false)
                        .message("Huella no encontrada en el sistema")
                        .build();
            } else {
                log.error("Error verificando huella: {}", response);
                return FingerPrintVerifyResponseDTO.builder()
                        .found(false)
                        .message("Error: " + response)
                        .build();
            }

        } catch (Exception e) {
            log.error("Error verificando huella", e);
            throw new FingerPrintException("Error al verificar huella: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCount() {
        return repository.countByActiveTrue();
    }

    private Integer findNextAvailableId() {
        Integer maxId = repository.findMaxFingerprintId();
        return (maxId == null ? 0 : maxId) + 1;
    }
}
