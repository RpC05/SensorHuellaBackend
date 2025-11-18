package com.example.sensor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FingerPrintResponseDTO {
    private Integer id;
    private Integer fingerprintId;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String tipoDocumento;
    private String numeroDocumento;
    private String description;
    private LocalDateTime enrolledAt;
    private LocalDateTime updatedAt;
    private Boolean active;
}