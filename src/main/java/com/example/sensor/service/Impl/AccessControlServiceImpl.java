package com.example.sensor.service.Impl;

import com.example.sensor.exceptions.*;
import com.example.sensor.mapper.AccessLogMapper;
import com.example.sensor.mapper.RfidCardMapper;
import com.example.sensor.model.dto.*;
import com.example.sensor.model.entity.*;
import com.example.sensor.model.enums.AccessType;
import com.example.sensor.model.enums.AuthenticationMethod;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AccessControlServiceImpl implements AccessControlService {

        private final RfidCardRepository cardRepository;
        private final AccessLogRepository logRepository;
        private final RfidCardMapper rfidCardMapper;
        private final AccessLogMapper accessLogMapper;
        private final Esp32HttpServiceImpl esp32HttpService; // Para escaneo RFID

        @Override
        public RfidCardResponseDTO registerCardWithScan() {
                log.info("Iniciando escaneo de tarjeta RFID desde ESP32...");

                // Llamar al ESP32 para que escanee la tarjeta (espera física)
                String cardUid = esp32HttpService.scanRfidCard();

                log.info("UID detectado desde ESP32: {}", cardUid);

                // Verificar si ya existe
                if (cardRepository.existsByCardUid(cardUid)) {
                        throw new FingerPrintException("La tarjeta ya está registrada: " + cardUid);
                }

                // Crear y guardar
                RfidCard card = RfidCard.builder()
                                .cardUid(cardUid)
                                .active(true)
                                .authorized(true)
                                .build();

                RfidCard saved = cardRepository.save(card);
                log.info("Tarjeta RFID registrada automáticamente: ID {} - UID: {}",
                                saved.getId(), saved.getCardUid());

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
                return cardRepository.findAll().stream()
                                .map(rfidCardMapper::toResponseDTO)
                                .collect(Collectors.toList());
        }

        @Override
        public void deleteCard(Integer id) {
                RfidCard card = cardRepository.findById(id)
                                .orElseThrow(() -> new FingerPrintNotFoundException(id));
                cardRepository.delete(card);
                log.info("Tarjeta ID {} eliminada físicamente", id);
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
        public void toggleCardActiveState(Integer id) {
                RfidCard card = cardRepository.findById(id)
                                .orElseThrow(() -> new FingerPrintNotFoundException(id));
                card.setActive(!card.getActive());
                cardRepository.save(card);
                log.info("Tarjeta ID {} estado activo: {}", id, card.getActive());
        }

        @Override
        public AccessRegisterResponseDTO registerAccess(AccessRegisterRequestDTO requestDTO) {
                log.info("Registrando acceso para tarjeta: {}", requestDTO.getCardUid());

                RfidCard card = cardRepository.findByCardUidAndActiveTrue(requestDTO.getCardUid())
                                .orElse(null);

                boolean authorized = card != null && Boolean.TRUE.equals(card.getAuthorized());

                // Determinar tipo de acceso (ENTRADA/SALIDA) basado en el último registro
                AccessType accessType = AccessType.ENTRADA; // Por defecto es ENTRADA

                if (card != null) {
                        // Buscar el último acceso de esta tarjeta
                        Optional<AccessLog> lastAccess = logRepository.findLastAccessByCard(card.getId());

                        if (lastAccess.isPresent()) {
                                // Alternar: Si el último fue ENTRADA, ahora es SALIDA y viceversa
                                accessType = lastAccess.get().getAccessType() == AccessType.ENTRADA
                                                ? AccessType.SALIDA
                                                : AccessType.ENTRADA;
                        }
                        // Si no hay acceso previo, se mantiene ENTRADA (default)
                }

                // Determinar método de autenticación (RFID por defecto para
                // retrocompatibilidad)
                AuthenticationMethod authMethod = AuthenticationMethod.RFID;
                if (requestDTO.getAuthenticationMethod() != null) {
                        try {
                                authMethod = AuthenticationMethod
                                                .valueOf(requestDTO.getAuthenticationMethod().toUpperCase());
                        } catch (IllegalArgumentException e) {
                                log.warn("Método de autenticación inválido: {}, usando RFID",
                                                requestDTO.getAuthenticationMethod());
                        }
                }

                // Crear log de acceso
                AccessLog accessLog = AccessLog.builder()
                                .rfidCard(card)
                                .accessType(accessType)
                                .authenticationMethod(authMethod)
                                .authorized(authorized)
                                .location(requestDTO.getLocation())
                                .deviceId(requestDTO.getDeviceId())
                                .build();

                logRepository.save(accessLog);

                String personName = card != null && card.getUser() != null
                                ? card.getUser().getNombres() + " " + card.getUser().getApellidoPaterno()
                                : "Tarjeta no registrada";
                String cargo = card != null && card.getUser() != null ? card.getUser().getCargo() : null;
                String message = authorized ? "Acceso autorizado" : "Acceso denegado - Tarjeta no autorizada";

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
