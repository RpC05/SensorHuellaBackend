package com.example.sensor.model.dto;

/**
 * DTO vacío para FingerPrint.
 * Solo se usa para triggear el proceso de enroll vía ESP32.
 * El ESP32 detecta la huella y retorna el ID.
 */
public class FingerPrintRequestDTO {
    // Empty DTO - solo usado para llamar al ESP32 a enrollar
}