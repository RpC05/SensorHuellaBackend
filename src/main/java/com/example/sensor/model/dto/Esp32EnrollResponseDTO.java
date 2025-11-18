package com.example.sensor.model.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la respuesta del ESP32 al comando ENROLL
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Esp32EnrollResponseDTO {
    private String status;  // "success", "error"
    private Integer id;     // ID asignado en el sensor
    private List<String> messages; // Mensajes de progreso
    private String error;   // Mensaje de error si falló
}
