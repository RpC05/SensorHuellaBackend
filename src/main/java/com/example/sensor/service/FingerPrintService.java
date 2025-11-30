package com.example.sensor.service;

import com.example.sensor.model.dto.EnrollProgressDTO;
import com.example.sensor.model.dto.FingerPrintRequestDTO;
import com.example.sensor.model.dto.FingerPrintResponseDTO;
import com.example.sensor.model.dto.FingerPrintVerifyResponseDTO;
import com.example.sensor.model.dto.FingerprintAccessRequestDTO;
import com.example.sensor.model.dto.AccessRegisterResponseDTO;

import java.util.List;

public interface FingerPrintService {
    List<FingerPrintResponseDTO> findAll();

    FingerPrintResponseDTO findById(Integer id);

    EnrollProgressDTO enrollFingerprint(FingerPrintRequestDTO requestDto);

    void deleteFingerprint(Integer id);

    FingerPrintVerifyResponseDTO verifyFingerprint();

    Integer getCount();

    void emptyDatabase();

    AccessRegisterResponseDTO registerFingerprintAccess(FingerprintAccessRequestDTO requestDTO);
}