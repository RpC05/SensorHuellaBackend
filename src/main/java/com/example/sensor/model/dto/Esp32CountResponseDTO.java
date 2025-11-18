package com.example.sensor.model.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta del ESP32 al comando COUNT
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Esp32CountResponseDTO {
    private Integer count;  // Número de huellas en el sensor
}
