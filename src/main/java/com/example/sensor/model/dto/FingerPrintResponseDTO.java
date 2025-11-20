package com.example.sensor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FingerPrintResponseDTO {
    private Integer fingerprintId;
    private Boolean active;
    private LocalDateTime enrolledAt;
    private LocalDateTime updatedAt;

    // Información del usuario asociado (si existe)
    private UserResponseDTO user;
}