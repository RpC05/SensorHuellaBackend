package com.example.sensor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FingerPrintRequestDTO {
    @NotBlank(message = "El nombre de usuario es requerido")
    private String name;

    private String description;
}
