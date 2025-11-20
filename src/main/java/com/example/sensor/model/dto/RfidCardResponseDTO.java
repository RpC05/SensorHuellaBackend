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
    private Boolean active;
    private Boolean authorized;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Información del usuario asociado (si existe)
    private UserResponseDTO user;
}