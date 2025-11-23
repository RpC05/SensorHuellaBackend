package com.example.sensor.mapper;

import com.example.sensor.model.dto.RfidCardResponseDTO;
import com.example.sensor.model.entity.RfidCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RfidCardMapper {

    private final UserMapper userMapper;

    public RfidCardResponseDTO toResponseDTO(RfidCard card) {
        if (card == null) {
            return null;
        }

        return RfidCardResponseDTO.builder()
                .id(card.getId())
                .cardUid(card.getCardUid())
                .active(card.getActive())
                .authorized(card.getAuthorized())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .user(card.getUser() != null ? userMapper.toResponseDTO(card.getUser()) : null)
                .build();
    }
}
