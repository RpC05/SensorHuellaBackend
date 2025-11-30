package com.example.sensor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FingerprintAccessRequestDTO {
    private Integer fingerprintId;
    private Integer confidence;
    private String location;
    private String deviceId;
    private String authenticationMethod; // "FINGERPRINT"
}
