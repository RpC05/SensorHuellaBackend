package com.example.sensor.service;

import com.example.sensor.model.dto.*;
import java.time.LocalDateTime;
import java.util.List;

public interface AccessControlService {
    // Registro de tarjetas RFID
    RfidCardResponseDTO registerCardWithScan();

    RfidCardResponseDTO getCardByUid(String cardUid);

    List<RfidCardResponseDTO> getAllCards();

    void deleteCard(Integer id);

    void toggleCardAuthorization(Integer id);

    AccessRegisterResponseDTO registerAccess(AccessRegisterRequestDTO requestDTO);

    List<AccessLogResponseDTO> getAccessLogs(LocalDateTime start, LocalDateTime end);

    List<AccessLogResponseDTO> getAccessLogsByCard(String cardUid);

    List<AccessLogResponseDTO> getTodayAccesses();
}
