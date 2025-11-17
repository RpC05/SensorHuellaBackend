package com.example.sensor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FingerPrintVerifyResponseDTO {
    private Boolean found;
    private Integer fingerprintId;
    private Integer confidence;
    private String userName;
    private String message;
}
