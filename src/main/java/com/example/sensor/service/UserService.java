package com.example.sensor.service;

import com.example.sensor.model.dto.AssignFingerPrintDTO;
import com.example.sensor.model.dto.AssignRfidCardDTO;
import com.example.sensor.model.dto.UserRequestDTO;
import com.example.sensor.model.dto.UserResponseDTO;

import java.util.List;

public interface UserService {
    UserResponseDTO createUser(UserRequestDTO requestDTO);

    UserResponseDTO getUserById(Integer id);

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO updateUser(Integer id, UserRequestDTO requestDTO);

    void deleteUser(Integer id);

    // Asignaciones
    UserResponseDTO assignRfidCard(Integer userId, AssignRfidCardDTO assignDTO);

    UserResponseDTO assignFingerPrint(Integer userId, AssignFingerPrintDTO assignDTO);
}
