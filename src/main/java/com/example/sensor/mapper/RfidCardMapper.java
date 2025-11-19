package com.example.sensor.mapper;

import com.example.sensor.model.dto.RfidCardRequestDTO;
import com.example.sensor.model.dto.RfidCardResponseDTO;
import com.example.sensor.model.entity.RfidCard;
import com.example.sensor.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class RfidCardMapper {

    public RfidCardResponseDTO toResponseDTO(RfidCard card) {
        if (card == null) return null;
        
        User user = card.getUser();
        String fullName = user != null ? 
                user.getNombres() + " " + user.getApellidoPaterno() + 
                (user.getApellidoMaterno() != null ? " " + user.getApellidoMaterno() : "") : null;
        
        return RfidCardResponseDTO.builder()
                .id(card.getId())
                .cardUid(card.getCardUid())
                .nombres(user != null ? user.getNombres() : null)
                .apellidoPaterno(user != null ? user.getApellidoPaterno() : null)
                .apellidoMaterno(user != null ? user.getApellidoMaterno() : null)
                .fullName(fullName)
                .tipoDocumento(user != null ? user.getTipoDocumento() : null)
                .numeroDocumento(user != null ? user.getNumeroDocumento() : null)
                .cargo(user != null ? user.getCargo() : null)
                .areaDepartamento(user != null ? user.getAreaDepartamento() : null)
                .active(card.getActive())
                .authorized(card.getAuthorized())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }

    public User toUserEntity(RfidCardRequestDTO dto) {
        if (dto == null) return null;
        
        return User.builder()
                .nombres(dto.getNombres())
                .apellidoPaterno(dto.getApellidoPaterno())
                .apellidoMaterno(dto.getApellidoMaterno())
                .tipoDocumento(dto.getTipoDocumento())
                .numeroDocumento(dto.getNumeroDocumento())
                .cargo(dto.getCargo())
                .areaDepartamento(dto.getAreaDepartamento())
                .active(true)
                .build();
    }

    public RfidCard toEntity(RfidCardRequestDTO dto, User user) {
        if (dto == null) return null;
        
        return RfidCard.builder()
                .cardUid(dto.getCardUid())
                .user(user)
                .active(true)
                .authorized(true)
                .build();
    }
}
