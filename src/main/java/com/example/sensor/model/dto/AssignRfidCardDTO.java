package com.example.sensor.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignRfidCardDTO {
    @NotBlank(message = "El UID de la tarjeta es requerido")
    private String cardUid;
}
