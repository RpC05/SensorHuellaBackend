package com.example.sensor.mapper;

import com.example.sensor.model.dto.AccessLogResponseDTO;
import com.example.sensor.model.entity.AccessLog;
import com.example.sensor.model.entity.RfidCard;
import com.example.sensor.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AccessLogMapper {

    public AccessLogResponseDTO toResponseDTO(AccessLog log) {
        if (log == null)
            return null;

        RfidCard card = log.getRfidCard();
        User user = card != null ? card.getUser() : null;
        String personName = user != null ? user.getNombres() + " " + user.getApellidoPaterno() : "Desconocido";

        return AccessLogResponseDTO.builder()
                .id(log.getId() != null ? log.getId().longValue() : null)
                .cardUid(card != null ? card.getCardUid() : null)
                .cardId(card != null ? card.getId() : null)
                .personName(personName)
                .cargo(user != null ? user.getCargo() : null)
                .accessType(log.getAccessType().name())
                .authenticationMethod(
                        log.getAuthenticationMethod() != null ? log.getAuthenticationMethod().name() : "RFID")
                .authorized(log.getAuthorized())
                .accessTime(log.getAccessTime())
                .location(log.getLocation())
                .deviceId(log.getDeviceId())
                .notes(log.getNotes())
                .build();
    }
}
