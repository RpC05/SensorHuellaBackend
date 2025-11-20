package com.example.sensor.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignFingerPrintDTO {
    @NotNull(message = "El ID de la huella es obligatorio")
    private Integer fingerprintId;
}
