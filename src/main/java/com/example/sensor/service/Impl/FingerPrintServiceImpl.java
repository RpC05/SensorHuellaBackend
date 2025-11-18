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
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser null");
        }
        FingerPrint fingerprint = repository.findById(id)
                .orElseThrow(() -> new FingerPrintNotFoundException(id));
        return mapper.toResponseDto(fingerprint);
    }

    @Override
    public EnrollProgressDTO enrollFingerprint(FingerPrintRequestDTO requestDto) {
        try {
            log.info("Enrollando huella...");

            List<String> messages = serialService.sendCommandWithProgress("ENROLL");
            String lastMessage = messages.get(messages.size() - 1);

            // Buscar el ID en los mensajes (Arduino imprime "Stored!" seguido del ID)
            if (lastMessage.matches("\\d+")) {
                Integer fingerprintId = Integer.parseInt(lastMessage);
                
                FingerPrint fingerprint = mapper.toEntity(requestDto);
                fingerprint.setFingerprintId(fingerprintId);
                fingerprint.setActive(true);
                
                FingerPrint saved = repository.save(fingerprint);
                log.info("Huella ID:{} guardada", fingerprintId);

                return EnrollProgressDTO.builder()
                        .status("SUCCESS")
                        .messages(messages)
                        .fingerprint(mapper.toResponseDto(saved))
                        .build();
            } else {
                log.error("Error enrollando: {}", lastMessage);
                return EnrollProgressDTO.builder()
                        .status("ERROR")
                        .messages(messages)
                        .error(lastMessage)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error enrollando", e);
            throw new FingerPrintException("Error: " + e.getMessage());
        }
    }

    @Override
    public void deleteFingerprint(Integer id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("El ID no puede ser null");
            }
            FingerPrint fingerprint = repository.findById(id)
                    .orElseThrow(() -> new FingerPrintNotFoundException(id));

            log.info("Eliminando huella ID:{}", fingerprint.getFingerprintId());

            try {
                List<String> messages = serialService.sendCommandWithProgress("DELETE " + fingerprint.getFingerprintId());
                String lastMessage = messages.get(messages.size() - 1);
                
                if (!lastMessage.equals("Deleted!")) {
                    log.warn("Error en sensor: {}", lastMessage);
                }
            } catch (Exception e) {
                log.warn("Error eliminando del sensor: {}", e.getMessage());
            }

            repository.delete(fingerprint);
            log.info("Huella eliminada de la BD");

        } catch (FingerPrintNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error eliminando", e);
            throw new FingerPrintException("Error: " + e.getMessage());
        }
    }

    @Override
    public FingerPrintVerifyResponseDTO verifyFingerprint() {
        try {
            log.info("Verificando huella...");
            
            List<String> messages = serialService.sendCommandWithProgress("VERIFY");
            
            for (String msg : messages) {
                // Buscar: "Found ID #X with confidence of Y"
                if (msg.startsWith("Found ID #")) {
                    String[] parts = msg.split(" ");
                    Integer fingerprintId = Integer.parseInt(parts[2].replace("#", ""));
                    Integer confidence = Integer.parseInt(parts[6]);
                    
                    FingerPrint fingerprint = repository.findByFingerprintIdAndActiveTrue(fingerprintId)
                            .orElse(null);
                    
                    if (fingerprint != null) {
                        return FingerPrintVerifyResponseDTO.builder()
                                .found(true)
                                .fingerprintId(fingerprintId)
                                .confidence(confidence)
                                .nombres(fingerprint.getNombres())
                                .apellidoPaterno(fingerprint.getApellidoPaterno())
                                .apellidoMaterno(fingerprint.getApellidoMaterno())
                                .message("Huella verificada")
                                .build();
                    } else {
                        return FingerPrintVerifyResponseDTO.builder()
                                .found(true)
                                .fingerprintId(fingerprintId)
                                .confidence(confidence)
                                .message("Sin datos personales")
                                .build();
                    }
                } else if (msg.equals("Did not find a match")) {
                    return FingerPrintVerifyResponseDTO.builder()
                            .found(false)
                            .message("Huella no encontrada")
                            .build();
                } else if (msg.equals("No finger detected") || msg.equals("Image too messy") || msg.equals("Unknown error")) {
                    return FingerPrintVerifyResponseDTO.builder()
                            .found(false)
                            .message(msg)
                            .build();
                }
            }
            
            return FingerPrintVerifyResponseDTO.builder()
                    .found(false)
                    .message("No se recibió respuesta válida")
                    .build();

        } catch (Exception e) {
            log.error("Error verificando", e);
            throw new FingerPrintException("Error: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCount() {
        return repository.countByActiveTrue();
    }

    @Override
    public void emptyDatabase() {
        try {
            log.info("Vaciando base de datos del sensor...");
            
            String messages = serialService.sendCommand("EMPTY");
            
            if (messages.equals("Database emptied!")) {
                log.info("Base de datos del sensor vaciada");
                
                repository.deleteAll();
                log.info("Base de datos PostgreSQL vaciada");
            } else {
                log.error("Error vaciando sensor: {}", messages);
                throw new FingerPrintException("Error vaciando sensor: " + messages);
            }
        } catch (Exception e) {
            log.error("Error vaciando base de datos", e);
            throw new FingerPrintException("Error: " + e.getMessage());
        }
    }
}
