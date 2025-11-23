package com.example.sensor.api;

import com.example.sensor.model.dto.AssignFingerPrintDTO;
import com.example.sensor.model.dto.AssignRfidCardDTO;
import com.example.sensor.model.dto.UserRequestDTO;
import com.example.sensor.model.dto.UserResponseDTO;
import com.example.sensor.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO requestDTO) {
        log.info("POST /users - Creando usuario: {}", requestDTO.getNumeroDocumento());
        UserResponseDTO response = userService.createUser(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() { 
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Integer id) { 
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UserRequestDTO requestDTO) {
        log.info("PUT /users/{} - Actualizando usuario", id);
        UserResponseDTO updated = userService.updateUser(id, requestDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        log.info("DELETE /users/{} - Eliminando usuario", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rfid")
    public ResponseEntity<UserResponseDTO> assignRfidCard(
            @PathVariable Integer id,
            @Valid @RequestBody AssignRfidCardDTO assignDTO) {
        log.info("POST /users/{}/rfid - Asignando tarjeta", id); 
        return ResponseEntity.ok(userService.assignRfidCard(id, assignDTO));
    }

    @PostMapping("/{id}/fingerprint")
    public ResponseEntity<UserResponseDTO> assignFingerPrint(
            @PathVariable Integer id,
            @Valid @RequestBody AssignFingerPrintDTO assignDTO) {
        log.info("POST /users/{}/fingerprint - Asignando huella", id); 
        return ResponseEntity.ok(userService.assignFingerPrint(id, assignDTO));
    }
}
