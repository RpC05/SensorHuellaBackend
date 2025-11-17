package com.example.sensor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FingerPrintResponseDTO {
    private Integer id;
    private Integer fingerprintId;
    private String name;
    private String description;
    private LocalDateTime enrolledAt;
    private LocalDateTime updatedAt;
    private Boolean active;
}
