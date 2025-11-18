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
        if (id == null) throw new IllegalArgumentException("El ID no puede ser null");
        FingerPrint fingerprint = repository.findById(id)
                .orElseThrow(() -> new FingerPrintNotFoundException(id));
        return mapper.toResponseDto(fingerprint);
    }

    @Override
    public EnrollProgressDTO enrollFingerprint(FingerPrintRequestDTO requestDto) {
        try {
            Integer nextId = findNextAvailableId();
            log.info("Enrollando nueva huella con ID: {}", nextId);

            FingerPrint fingerprint = mapper.toEntity(requestDto);
            fingerprint.setFingerprintId(nextId);
            fingerprint.setActive(true);

            // Enviar comando a Arduino
            List<String> messages = serialService.sendCommandWithProgress("ENROLL:" + nextId);

            // Buscar el template en las respuestas
            String templateData = null;
            for (String message : messages) {
                if (message.startsWith("TEMPLATE:")) {
                    templateData = message.substring(9); // Remover "TEMPLATE:"
                    log.info("Template capturado: {} caracteres", templateData.length());
                    break;
                }
            }

            // Verificar respuesta
            String lastMessage = messages.get(messages.size() - 1);

            if (lastMessage.startsWith("SUCCESS:") && templateData != null) {
                // Guardar template en BD
                fingerprint.setTemplateData(templateData);
                FingerPrint saved = repository.save(fingerprint);
                log.info("Huella enrollada exitosamente con template en BD: {}", saved.getId());

                return EnrollProgressDTO.builder()
                        .status("SUCCESS")
                        .messages(messages)
                        .fingerprint(mapper.toResponseDto(saved))
                        .build();
            } else {
                log.error("Error enrollando huella: {} (template: {})", lastMessage, templateData != null);
                return EnrollProgressDTO.builder()
                        .status("ERROR")
                        .messages(messages)
                        .error(templateData == null ? "No se recibió template" : lastMessage)
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
            if (id == null) {
                throw new IllegalArgumentException("El ID no puede ser null");
            }
            FingerPrint fingerprint = repository.findById(id)
                    .orElseThrow(() -> new FingerPrintNotFoundException(id));

            log.info("Eliminando huella ID: {} de BD (ya no se guarda en sensor)", id);

            // Solo marcar como inactiva en BD (soft delete)
            fingerprint.setActive(false);
            repository.save(fingerprint);
            log.info("Huella eliminada exitosamente de BD");

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
            log.info("Capturando huella para verificación");
            
            // Capturar huella actual del sensor
            List<String> messages = serialService.sendCommandWithProgress("CAPTURE");
            
            // Extraer template de las respuestas
            String capturedTemplate = null;
            for (String message : messages) {
                if (message.startsWith("TEMPLATE:")) {
                    capturedTemplate = message.substring(9);
                    log.info("Template capturado: {} caracteres", capturedTemplate.length());
                    break;
                }
            }
            
            if (capturedTemplate == null) {
                String lastMessage = messages.get(messages.size() - 1);
                log.error("Error capturando huella: {}", lastMessage);
                return FingerPrintVerifyResponseDTO.builder()
                        .found(false)
                        .message("Error capturando huella: " + lastMessage)
                        .build();
            }

            // Comparar contra todos los templates en BD
            List<FingerPrint> allFingerprints = repository.findAllByActiveTrue();
            log.info("📋 Comparando contra {} huellas en BD", allFingerprints.size());
            
            for (FingerPrint storedFingerprint : allFingerprints) {
                if (storedFingerprint.getTemplateData() != null) {
                    log.debug("Comparando con: {}", storedFingerprint.getName());
                    
                    // Comparar templates - algoritmo mejorado con pesos
                    int confidence = compareTemplatesWeighted(capturedTemplate, storedFingerprint.getTemplateData());
                    
                    log.info("{} -> similitud: {}%", storedFingerprint.getName(), confidence);
                    
                    // Umbral: 65% de similitud
                    if (confidence >= 65) {
                        log.info("HUELLA VERIFICADA: {} (similitud: {}%)", storedFingerprint.getName(), confidence);
                        
                        return FingerPrintVerifyResponseDTO.builder()
                                .found(true)
                                .fingerprintId(storedFingerprint.getFingerprintId())
                                .confidence(confidence)
                                .userName(storedFingerprint.getName())
                                .message("Huella verificada exitosamente")
                                .build();
                    }
                }
            }
            
            log.info("Huella no encontrada en BD (ninguna coincidencia ≥ 65%)");
            return FingerPrintVerifyResponseDTO.builder()
                    .found(false)
                    .message("Huella no encontrada en el sistema")
                    .build();

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

    // CORRELACIÓN NORMALIZADA
    private int compareTemplatesWeighted(String template1Hex, String template2Hex) {
        if (template1Hex == null || template2Hex == null) {
            return 0;
        }
        
        if (template1Hex.length() != 1024 || template2Hex.length() != 1024) {
            log.warn("Templates con longitud incorrecta: {} vs {}", template1Hex.length(), template2Hex.length());
            return 0;
        }
        
        // Convertir hex a bytes
        byte[] bytes1 = hexStringToByteArray(template1Hex);
        byte[] bytes2 = hexStringToByteArray(template2Hex);
        
        if (bytes1 == null || bytes2 == null || bytes1.length != 512 || bytes2.length != 512) {
            log.warn("Error convirtiendo templates a bytes");
            return 0;
        }
        
        // ESTRUCTURA DEL TEMPLATE AS608:
        // Bytes 0-8: Header (checksum, flags)
        // Bytes 9-511: Datos de minutiae (puntos característicos)
        
        double totalScore = 0.0;
        double maxScore = 0.0;
        
        // ZONA 1: Header (bytes 0-8) - Peso bajo (puede variar por condiciones)
        double headerScore = correlateRegion(bytes1, bytes2, 0, 8);
        totalScore += headerScore * 0.5; // Peso 0.5x
        maxScore += 0.5;
        log.debug("Header (0-8): {}", String.format("%.2f", headerScore));
        
        // ZONA 2: Minutiae principales (bytes 9-200) - CRÍTICA
        double zone1Score = correlateRegion(bytes1, bytes2, 9, 200);
        totalScore += zone1Score * 3.0; // Peso 3x - zona más importante
        maxScore += 3.0;
        log.debug("Zona crítica (9-200): {}", String.format("%.2f", zone1Score));
        
        // ZONA 3: Minutiae secundarias (bytes 200-350)
        double zone2Score = correlateRegion(bytes1, bytes2, 200, 350);
        totalScore += zone2Score * 2.0; // Peso 2x
        maxScore += 2.0;
        log.debug("Zona secundaria (200-350): {}", String.format("%.2f", zone2Score));
        
        // ZONA 4: Datos complementarios (bytes 350-511)
        double zone3Score = correlateRegion(bytes1, bytes2, 350, 511);
        totalScore += zone3Score * 1.0; // Peso 1x
        maxScore += 1.0;
        log.debug("Zona complementaria (350-511): {}", String.format("%.2f", zone3Score));
        
        // Calcular porcentaje final
        int finalScore = (int) ((totalScore / maxScore) * 100.0);
        
        log.debug("Score total: {}/{} = {}%", 
                  String.format("%.2f", totalScore), 
                  String.format("%.2f", maxScore), 
                  finalScore);
        
        return finalScore;
    }
    
    /**
     * Calcula correlación normalizada entre dos regiones
     * Retorna valor entre 0.0 (completamente diferente) y 1.0 (idéntico)
     * 
     * Usa correlación de Pearson adaptada para datos binarios
     */
    private double correlateRegion(byte[] bytes1, byte[] bytes2, int start, int end) {
        int length = end - start + 1;
        
        // Calcular valores medios
        double mean1 = 0, mean2 = 0;
        for (int i = start; i <= end && i < bytes1.length && i < bytes2.length; i++) {
            mean1 += (bytes1[i] & 0xFF);
            mean2 += (bytes2[i] & 0xFF);
        }
        mean1 /= length;
        mean2 /= length;
        
        // Calcular correlación
        double numerator = 0;
        double denominator1 = 0;
        double denominator2 = 0;
        
        for (int i = start; i <= end && i < bytes1.length && i < bytes2.length; i++) {
            double diff1 = (bytes1[i] & 0xFF) - mean1;
            double diff2 = (bytes2[i] & 0xFF) - mean2;
            
            numerator += diff1 * diff2;
            denominator1 += diff1 * diff1;
            denominator2 += diff2 * diff2;
        }
        
        // Evitar división por cero
        if (denominator1 == 0 || denominator2 == 0) {
            return 0.0;
        }
        
        double correlation = numerator / Math.sqrt(denominator1 * denominator2);
        
        // Normalizar a rango 0-1
        return (correlation + 1.0) / 2.0;
    }
    
    /**
     * Convierte string hexadecimal a array de bytes
     */
    private byte[] hexStringToByteArray(String hex) {
        try {
            int len = hex.length();
            if (len % 2 != 0) {
                return null;
            }
            
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                     + Character.digit(hex.charAt(i+1), 16));
            }
            return data;
        } catch (Exception e) {
            log.error("Error convirtiendo hex a bytes", e);
            return null;
        }
    }

    private Integer findNextAvailableId() {
        Integer maxId = repository.findMaxFingerprintId();
        return (maxId == null ? 0 : maxId) + 1;
    }
}