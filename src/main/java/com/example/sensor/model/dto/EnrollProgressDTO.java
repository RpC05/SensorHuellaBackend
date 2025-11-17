package com.example.sensor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollProgressDTO {
    private String status; // PROCESSING, SUCCESS, ERROR
    private List<String> messages;
    private FingerPrintResponseDTO fingerprint;
    private String error;
}
