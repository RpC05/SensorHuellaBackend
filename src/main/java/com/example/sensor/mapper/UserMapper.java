package com.example.sensor.mapper;

import com.example.sensor.model.dto.UserRequestDTO;
import com.example.sensor.model.dto.UserResponseDTO;
import com.example.sensor.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserRequestDTO dto) {
        if (dto == null) return null;
        
        return User.builder()
                .nombres(dto.getNombres())
                .apellidoPaterno(dto.getApellidoPaterno())
                .apellidoMaterno(dto.getApellidoMaterno())
                .fechaNacimiento(dto.getFechaNacimiento())
                .tipoDocumento(dto.getTipoDocumento())
                .numeroDocumento(dto.getNumeroDocumento())
                .cargo(dto.getCargo())
                .areaDepartamento(dto.getAreaDepartamento())
                .active(true)
                .build();
    }

    public UserResponseDTO toResponseDTO(User user) {
        if (user == null) return null;
        
        String fullName = user.getNombres() + " " + user.getApellidoPaterno() +
                (user.getApellidoMaterno() != null ? " " + user.getApellidoMaterno() : "");
        
        return UserResponseDTO.builder()
                .id(user.getId())
                .nombres(user.getNombres())
                .apellidoPaterno(user.getApellidoPaterno())
                .apellidoMaterno(user.getApellidoMaterno())
                .fullName(fullName)
                .fechaNacimiento(user.getFechaNacimiento())
                .tipoDocumento(user.getTipoDocumento())
                .numeroDocumento(user.getNumeroDocumento())
                .cargo(user.getCargo())
                .areaDepartamento(user.getAreaDepartamento())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .hasFingerprint(user.getFingerPrint() != null)
                .hasRfidCard(user.getRfidCard() != null)
                .build();
    }

    public void updateEntityFromDTO(UserRequestDTO dto, User user) {
        if (dto == null || user == null) return;
        
        user.setNombres(dto.getNombres());
        user.setApellidoPaterno(dto.getApellidoPaterno());
        user.setApellidoMaterno(dto.getApellidoMaterno());
        user.setFechaNacimiento(dto.getFechaNacimiento());
        user.setTipoDocumento(dto.getTipoDocumento());
        user.setNumeroDocumento(dto.getNumeroDocumento());
        user.setCargo(dto.getCargo());
        user.setAreaDepartamento(dto.getAreaDepartamento());
    }
}
