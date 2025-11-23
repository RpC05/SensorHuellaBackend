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
public class AccessRegisterResponseDTO {
    private Boolean authorized;
    private String accessType;
    private String personName;
    private String cargo;
    private String message;
    private LocalDateTime timestamp;
}
