package com.example.sensor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FingerPrintRequestDTO {
    @NotBlank(message = "Los nombres son requeridos")
    private String nombres;
    
    @NotBlank(message = "El apellido paterno es requerido")
    private String apellidoPaterno;
    
    private String apellidoMaterno;
    
    private LocalDate fechaNacimiento;
    
    @NotBlank(message = "El tipo de documento es requerido")
    private String tipoDocumento; // DNI, CE, Pasaporte
    
    @NotBlank(message = "El número de documento es requerido")
    private String numeroDocumento;
    
    private String description;
}