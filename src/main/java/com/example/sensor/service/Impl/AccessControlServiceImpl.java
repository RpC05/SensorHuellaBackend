package com.example.sensor.service.Impl;

import com.example.sensor.exceptions.*;
import com.example.sensor.mapper.AccessLogMapper;
import com.example.sensor.mapper.RfidCardMapper;
import com.example.sensor.model.dto.*;
import com.example.sensor.model.entity.*;
import com.example.sensor.model.enums.AccessType;
import com.example.sensor.repository.*;
import com.example.sensor.service.AccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AccessControlServiceImpl implements AccessControlService {
    
    private final RfidCardRepository cardRepository;
    private final AccessLogRepository logRepository;
    private final UserRepository userRepository;
    private final RfidCardMapper rfidCardMapper;
    private final AccessLogMapper accessLogMapper;

    @Override
    public RfidCardResponseDTO registerCard(RfidCardRequestDTO requestDTO) {
        log.info("Registrando nueva tarjeta RFID: {}", requestDTO.getCardUid());
        
        if (cardRepository.existsByCardUid(requestDTO.getCardUid())) {
            throw new FingerPrintException("La tarjeta ya está registrada");
        }
        
        // Si no vienen datos de usuario, crear tarjeta sin usuario (para asignar después)
        if (requestDTO.getNombres() == null || requestDTO.getApellidoPaterno() == null) {
            RfidCard card = RfidCard.builder()
                    .cardUid(requestDTO.getCardUid())
                    .active(true)
                    .authorized(true)
                    .build();
            
            RfidCard saved = cardRepository.save(card);
            log.info("Tarjeta registrada sin usuario: ID {}", saved.getId());
            return rfidCardMapper.toResponseDTO(saved);
        }
        
        // Si vienen datos de usuario, crear usuario y tarjeta
        User user = rfidCardMapper.toUserEntity(requestDTO);
        User savedUser = userRepository.save(user);
        
        RfidCard card = rfidCardMapper.toEntity(requestDTO, savedUser);
        RfidCard saved = cardRepository.save(card);
        
        log.info("Tarjeta registrada con usuario: ID {}", saved.getId());
        return rfidCardMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RfidCardResponseDTO getCardByUid(String cardUid) {
        RfidCard card = cardRepository.findByCardUid(cardUid)
                .orElseThrow(() -> new FingerPrintNotFoundException(null));
        return rfidCardMapper.toResponseDTO(card);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RfidCardResponseDTO> getAllCards() {
        return cardRepository.findAllByActiveTrue().stream()
                .map(rfidCardMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RfidCardResponseDTO updateCard(Integer id, RfidCardRequestDTO requestDTO) {
        RfidCard card = cardRepository.findById(id)
                .orElseThrow(() -> new FingerPrintNotFoundException(id)); 
        
        RfidCard updated = cardRepository.save(card);
        return rfidCardMapper.toResponseDTO(updated);
    }

    @Override
    public void deleteCard(Integer id) {
        RfidCard card = cardRepository.findById(id)
                .orElseThrow(() -> new FingerPrintNotFoundException(id));
        card.setActive(false);
        cardRepository.save(card);
    }

    @Override
    public void toggleCardAuthorization(Integer id) {
        RfidCard card = cardRepository.findById(id)
                .orElseThrow(() -> new FingerPrintNotFoundException(id));
        card.setAuthorized(!card.getAuthorized());
        cardRepository.save(card);
        log.info("Tarjeta ID {} autorización: {}", id, card.getAuthorized());
    }

    @Override
    public AccessRegisterResponseDTO registerAccess(AccessRegisterRequestDTO requestDTO) {
        log.info("Registrando acceso para tarjeta: {}", requestDTO.getCardUid());
        
        RfidCard card = cardRepository.findByCardUidAndActiveTrue(requestDTO.getCardUid())
                .orElse(null);
        
        boolean authorized = card != null && Boolean.TRUE.equals(card.getAuthorized());
        
        // Determinar tipo de acceso (ENTRADA/SALIDA) basado en el último registro
        AccessType accessType = AccessType.ENTRADA;
        
        if (card != null) {
            logRepository.findLastAccessByCard(card.getId()).ifPresent(lastAccess -> {
                // Si el último fue ENTRADA, ahora es SALIDA
                // Implementar lógica si es necesario
            });
        }
        
        // Crear log de acceso
        AccessLog accessLog = AccessLog.builder()
                .rfidCard(card)
                .accessType(accessType)
                .authorized(authorized)
                .location(requestDTO.getLocation())
                .deviceId(requestDTO.getDeviceId())
                .build();
        
        logRepository.save(accessLog);
        
        String personName = card != null && card.getUser() != null ? 
                card.getUser().getNombres() + " " + card.getUser().getApellidoPaterno() : 
                "Tarjeta no registrada";
        String cargo = card != null && card.getUser() != null ? card.getUser().getCargo() : null;
        String message = authorized ? 
                "Acceso autorizado" : 
                "Acceso denegado - Tarjeta no autorizada";
        
        log.info("Acceso registrado: {} - {} - {}", 
                requestDTO.getCardUid(), accessType, authorized ? "AUTORIZADO" : "DENEGADO");
        
        return AccessRegisterResponseDTO.builder()
                .authorized(authorized)
                .accessType(accessType.name())
                .personName(personName)
                .cargo(cargo)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponseDTO> getAccessLogs(LocalDateTime start, LocalDateTime end) {
        return logRepository.findByAccessTimeBetween(start, end).stream()
                .map(accessLogMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponseDTO> getAccessLogsByCard(String cardUid) {
        return logRepository.findByRfidCard_CardUidOrderByAccessTimeDesc(cardUid).stream()
                .map(accessLogMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessLogResponseDTO> getTodayAccesses() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return getAccessLogs(startOfDay, endOfDay);
    }
}
