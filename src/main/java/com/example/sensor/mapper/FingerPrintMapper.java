package com.example.sensor.mapper;

import com.example.sensor.model.dto.FingerPrintResponseDTO;
import com.example.sensor.model.entity.FingerPrint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FingerPrintMapper {

    private final UserMapper userMapper;

    public FingerPrintResponseDTO toResponseDto(FingerPrint fingerPrint) {
        if (fingerPrint == null) {
            return null;
        }

        return FingerPrintResponseDTO.builder()
                .fingerprintId(fingerPrint.getFingerprintId())
                .active(fingerPrint.getActive())
                .enrolledAt(fingerPrint.getEnrolledAt())
                .updatedAt(fingerPrint.getUpdatedAt())
                .user(fingerPrint.getUser() != null ? userMapper.toResponseDTO(fingerPrint.getUser()) : null)
                .build();
    }
}
