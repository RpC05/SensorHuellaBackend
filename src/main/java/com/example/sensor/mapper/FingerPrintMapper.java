package com.example.sensor.mapper;

import com.example.sensor.model.dto.FingerPrintRequestDTO;
import com.example.sensor.model.dto.FingerPrintResponseDTO;
import com.example.sensor.model.entity.FingerPrint;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FingerPrintMapper {
    private final ModelMapper modelMapper;

    public FingerPrint toEntity(FingerPrintRequestDTO fingerPrintRequestDTO) {
        return modelMapper.map(fingerPrintRequestDTO, FingerPrint.class);
    }

    public FingerPrintResponseDTO toResponseDto(FingerPrint fingerPrint) {
        return modelMapper.map(fingerPrint, FingerPrintResponseDTO.class);
    }

    public void updateEntityFromDto(FingerPrintRequestDTO requestDto, FingerPrint fingerPrint) {
        modelMapper.map(requestDto, fingerPrint);
    }
}
