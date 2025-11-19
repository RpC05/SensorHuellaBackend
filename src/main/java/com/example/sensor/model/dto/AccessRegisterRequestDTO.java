package com.example.sensor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessRegisterRequestDTO {
    private String cardUid;
    private String location;
    private String deviceId;
}