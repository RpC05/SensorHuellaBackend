package com.example.sensor.model.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta del ESP32 al comando VERIFY
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Esp32VerifyResponseDTO {
    private Boolean found;       // true si encontró match
    private Integer id;          // ID de la huella encontrada
    private Integer confidence;  // Nivel de confianza (0-255)
    private String message;      // Mensaje descriptivo
}
