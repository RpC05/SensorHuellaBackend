package com.example.sensor.service.Impl;

import com.example.sensor.exceptions.FingerPrintException;
import com.example.sensor.exceptions.FingerPrintNotFoundException;
import com.example.sensor.mapper.UserMapper;
import com.example.sensor.model.dto.AssignFingerPrintDTO;
import com.example.sensor.model.dto.AssignRfidCardDTO;
import com.example.sensor.model.dto.UserRequestDTO;
import com.example.sensor.model.dto.UserResponseDTO;
import com.example.sensor.model.entity.FingerPrint;
import com.example.sensor.model.entity.RfidCard;
import com.example.sensor.model.entity.User;
import com.example.sensor.repository.FingerPrintRepository;
import com.example.sensor.repository.RfidCardRepository;
import com.example.sensor.repository.UserRepository;
import com.example.sensor.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RfidCardRepository rfidCardRepository;
    private final FingerPrintRepository fingerPrintRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponseDTO createUser(UserRequestDTO requestDTO) {
        log.info("Creando usuario: {}", requestDTO.getNumeroDocumento());

        if (userRepository.existsByNumeroDocumento(requestDTO.getNumeroDocumento())) {
            throw new FingerPrintException("Ya existe un usuario con ese número de documento");
        }

        User user = userMapper.toEntity(requestDTO);
        User saved = userRepository.save(user);

        log.info("Usuario creado: ID {}", saved.getId());
        return userMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new FingerPrintNotFoundException(id));
        return userMapper.toResponseDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO updateUser(Integer id, UserRequestDTO requestDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new FingerPrintNotFoundException(id));

        userMapper.updateEntityFromDTO(requestDTO, user);
        User updated = userRepository.save(user);

        log.info("Usuario actualizado: ID {}", id);
        return userMapper.toResponseDTO(updated);
    }

    @Override
    public void deleteUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new FingerPrintNotFoundException(id));

        user.setActive(false);
        userRepository.save(user);
        log.info("Usuario desactivado: ID {}", id);
    }

    @Override
    public UserResponseDTO assignRfidCard(Integer userId, AssignRfidCardDTO assignDTO) {
        log.info("Asignando tarjeta {} a usuario {}", assignDTO.getCardUid(), userId);

        // Verificar que el usuario existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new FingerPrintNotFoundException(userId));

        // Verificar que el usuario no tenga tarjeta asignada
        if (user.getRfidCard() != null) {
            throw new FingerPrintException("El usuario ya tiene una tarjeta RFID asignada");
        }

        // Buscar la tarjeta sin usuario
        RfidCard card = rfidCardRepository.findByCardUid(assignDTO.getCardUid())
                .orElseThrow(() -> new FingerPrintException("Tarjeta no encontrada"));

        // Verificar que la tarjeta no esté asignada a otro usuario
        if (card.getUser() != null) {
            throw new FingerPrintException("La tarjeta ya está asignada a otro usuario");
        }

        // Asignar
        card.setUser(user);
        rfidCardRepository.save(card);

        log.info("Tarjeta asignada exitosamente");
        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO assignFingerPrint(Integer userId, AssignFingerPrintDTO assignDTO) {
        log.info("Asignando huella ID {} a usuario {}", assignDTO.getFingerprintId(), userId);

        // Verificar que el usuario existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new FingerPrintNotFoundException(userId));

        // Verificar que el usuario no tenga huella asignada
        if (user.getFingerPrint() != null) {
            throw new FingerPrintException("El usuario ya tiene una huella asignada");
        }

        // Buscar la huella
        FingerPrint fingerPrint = fingerPrintRepository.findByFingerprintId(assignDTO.getFingerprintId())
                .orElseThrow(
                        () -> new FingerPrintException("Huella no encontrada con ID: " + assignDTO.getFingerprintId()));

        // Verificar que la huella no esté asignada a otro usuario
        if (fingerPrint.getUser() != null) {
            throw new FingerPrintException("La huella ya está asignada a otro usuario");
        }

        // Asignar
        fingerPrint.setUser(user);
        fingerPrintRepository.save(fingerPrint);

        log.info("Huella asignada exitosamente");
        return userMapper.toResponseDTO(user);
    }
}
