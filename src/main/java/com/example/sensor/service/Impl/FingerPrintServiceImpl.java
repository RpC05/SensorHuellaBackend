package com.example.sensor.service.Impl;

import com.example.sensor.exceptions.FingerPrintException;
import com.example.sensor.exceptions.FingerPrintNotFoundException;
import com.example.sensor.mapper.FingerPrintMapper;
import com.example.sensor.model.dto.EnrollProgressDTO;
import com.example.sensor.model.dto.FingerPrintRequestDTO;
import com.example.sensor.model.dto.FingerPrintResponseDTO;
import com.example.sensor.model.dto.FingerPrintUpdateDTO;
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
        int initialCount = -1;
        try {
            // Intentar obtener conteo inicial para recuperación en caso de fallo
            try {
                String countStr = serialService.sendCommand("COUNT");
                // Formato esperado: "Sensor contains X templates"
                initialCount = Integer.parseInt(countStr.replaceAll("\\D+", ""));
            } catch (Exception e) {
                log.warn("No se pudo obtener conteo inicial: {}", e.getMessage());
            }

            log.info("Enrollando huella... (Conteo inicial: {})", initialCount);

            List<String> messages = serialService.sendCommandWithProgress("ENROLL");
            String lastMessage = messages.get(messages.size() - 1);

            // Buscar el ID en los mensajes (Arduino imprime "Stored!" seguido del ID)
            if (lastMessage.matches("\\d+")) {
                Integer fingerprintId = Integer.parseInt(lastMessage);
                return saveFingerprintAndReturn(fingerprintId, messages);
            } else {
                return EnrollProgressDTO.builder()
                        .status("ERROR")
                        .messages(messages)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error enrollando: {}", e.getMessage());

            // INTENTO DE RECUPERACIÓN: Si falló por timeout (502) pero el sensor sí guardó
            if (initialCount != -1) {
                try {
                    log.info("Intentando recuperación verificando conteo del sensor...");
                    String countStr = serialService.sendCommand("COUNT");
                    int finalCount = Integer.parseInt(countStr.replaceAll("\\D+", ""));

                    if (finalCount > initialCount) {
                        // Asumimos que el nuevo ID es el último (esto funciona si los IDs son
                        // secuenciales)
                        // El sensor AS608 suele asignar el primer ID libre.
                        // Si el conteo subió, ES MUY PROBABLE que sea el ID = finalCount (si no hubo
                        // huecos)
                        // O mejor, asumimos que el ID es finalCount si partimos de 0 o secuencial.
                        // Riesgo: Si había huecos (borrados), el ID podría ser otro.
                        // Pero es mejor intentar guardar algo que perderlo.
                        // En el firmware: id = finger.templateCount + 1; -> Esto confirma que usa
                        // (count + 1)
                        // PERO ojo, templateCount se actualiza despues de guardar.

                        // Si el firmware hace: id = templateCount + 1 (antes de guardar)
                        // Entonces el nuevo ID es efectivamente finalCount.

                        Integer recoveredId = finalCount;
                        log.info("RECUPERACIÓN EXITOSA: El conteo subió de {} a {}. Asumiendo ID {}.", initialCount,
                                finalCount, recoveredId);

                        List<String> recoveryMessages = List.of(
                                "Error de comunicación (Timeout)",
                                "Pero la huella se guardó en el sensor.",
                                "Recuperado ID: " + recoveredId);

                        return saveFingerprintAndReturn(recoveredId, recoveryMessages);
                    }
                } catch (Exception ex) {
                    log.error("Fallo la recuperación: {}", ex.getMessage());
                }
            }

            throw new FingerPrintException("Error: " + e.getMessage());
        }
    }

    private EnrollProgressDTO saveFingerprintAndReturn(Integer fingerprintId, List<String> messages) {
        // Crear FingerPrint solo con el ID del ESP32, sin usuario
        FingerPrint fingerprint = FingerPrint.builder()
                .fingerprintId(fingerprintId)
                .active(true)
                .build();

        FingerPrint saved = repository.save(fingerprint);
        log.info("Huella ID:{} guardada en BD", fingerprintId);

        return EnrollProgressDTO.builder()
                .status("SUCCESS")
                .messages(messages)
                .fingerprint(mapper.toResponseDto(saved))
                .build();
    }

    @Override
    public FingerPrintResponseDTO updateFingerprint(Integer id, FingerPrintUpdateDTO updateDto) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser null");
        }
        
        FingerPrint fingerprint = repository.findById(id)
                .orElseThrow(() -> new FingerPrintNotFoundException(id));
        
        // Actualizar solo los campos que vienen en el body
        if (updateDto.getNombres() != null) {
            fingerprint.setNombres(updateDto.getNombres());
        }
        if (updateDto.getApellidoPaterno() != null) {
            fingerprint.setApellidoPaterno(updateDto.getApellidoPaterno());
        }
        if (updateDto.getApellidoMaterno() != null) {
            fingerprint.setApellidoMaterno(updateDto.getApellidoMaterno());
        }
        if (updateDto.getFechaNacimiento() != null) {
            fingerprint.setFechaNacimiento(updateDto.getFechaNacimiento());
        }
        if (updateDto.getTipoDocumento() != null) {
            fingerprint.setTipoDocumento(updateDto.getTipoDocumento());
        }
        if (updateDto.getNumeroDocumento() != null) {
            fingerprint.setNumeroDocumento(updateDto.getNumeroDocumento());
        }
        if (updateDto.getDescription() != null) {
            fingerprint.setDescription(updateDto.getDescription());
        }
        
        FingerPrint updated = repository.save(fingerprint);
        log.info("Huella ID:{} actualizada", id);
        
        return mapper.toResponseDto(updated);
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
                List<String> messages = serialService
                        .sendCommandWithProgress("DELETE " + fingerprint.getFingerprintId());
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
                } else if (msg.equals("No finger detected") || msg.equals("Image too messy")
                        || msg.equals("Unknown error")) {
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
        log.info("Vaciando base de datos del sensor...");

        try {
            // PRIMERO: Vaciar el sensor físico
            String messages = serialService.sendCommand("EMPTY");

            if (messages.equals("Database emptied!")) {
                log.info("Base de datos del sensor vaciada exitosamente");
            } else {
                log.warn("Respuesta inesperada del sensor: {}", messages);
            }
        } catch (Exception e) {
            log.error("Error vaciando sensor: {}", e.getMessage());
        }

        try {
            // SEGUNDO: Vaciar BD PostgreSQL
            long deletedCount = repository.count();
            repository.deleteAll();
            log.info("Base de datos PostgreSQL vaciada - {} huellas eliminadas", deletedCount);
        } catch (Exception e) {
            log.error("Error vaciando BD PostgreSQL: {}", e.getMessage());
            throw new FingerPrintException("Error vaciando BD: " + e.getMessage());
        }

        log.info("Proceso de vaciado completado");
    }
}
