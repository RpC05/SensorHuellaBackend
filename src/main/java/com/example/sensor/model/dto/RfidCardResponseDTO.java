package com.example.sensor.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RfidCardResponseDTO {
    private Integer id;
    private String cardUid;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String fullName;
    private String tipoDocumento;
    private String numeroDocumento;
    private String email;
    private String telefono;
    private String cargo;
    private String areaDepartamento;
    private Boolean active;
    private Boolean authorized;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String notes;
}