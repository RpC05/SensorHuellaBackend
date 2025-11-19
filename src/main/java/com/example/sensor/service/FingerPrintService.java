package com.example.sensor.service;

import com.example.sensor.model.dto.EnrollProgressDTO;
import com.example.sensor.model.dto.FingerPrintRequestDTO;
import com.example.sensor.model.dto.FingerPrintResponseDTO;
import com.example.sensor.model.dto.FingerPrintUpdateDTO;
import com.example.sensor.model.dto.FingerPrintVerifyResponseDTO;

import java.util.List;

public interface FingerPrintService {
    List<FingerPrintResponseDTO> findAll();
    FingerPrintResponseDTO findById(Integer id);
    EnrollProgressDTO enrollFingerprint(FingerPrintRequestDTO requestDto);
    FingerPrintResponseDTO updateFingerprint(Integer id, FingerPrintUpdateDTO updateDto);
    void deleteFingerprint(Integer id);
    FingerPrintVerifyResponseDTO verifyFingerprint();
    Integer getCount();
    void emptyDatabase();
}
