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
public class AccessLogResponseDTO {
    private Long id;
    private String cardUid;
    private Integer cardId;
    private String personName;
    private String cargo;
    private String accessType;
    private Boolean authorized;
    private LocalDateTime accessTime;
    private String location;
    private String deviceId;
    private String notes;
}
