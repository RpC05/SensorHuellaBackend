package com.example.sensor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Integer id;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String fullName;
    private LocalDate fechaNacimiento;
    private String tipoDocumento;
    private String numeroDocumento;
    private String cargo;
    private String areaDepartamento;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Indicadores de asignaciones
    private Boolean hasFingerprint;
    private Boolean hasRfidCard;
}
