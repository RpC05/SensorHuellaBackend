package com.example.sensor.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usr_id_int")
    private Integer id;

    @Column(name = "usr_nombres_vac", nullable = false)
    private String nombres;

    @Column(name = "usr_apellido_paterno_vac", nullable = false)
    private String apellidoPaterno;

    @Column(name = "usr_apellido_materno_vac")
    private String apellidoMaterno;

    @Column(name = "usr_fecha_nacimiento_dt")
    private LocalDate fechaNacimiento;

    @Column(name = "usr_tipo_documento_vac", nullable = false)
    private String tipoDocumento;

    @Column(name = "usr_nro_documento_vac", nullable = false, unique = true)
    private String numeroDocumento;

    @Column(name = "usr_cargo_vac")
    private String cargo;

    @Column(name = "usr_area_departamento_vac")
    private String areaDepartamento;

    @Column(name = "usr_estado_bol", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "usr_created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "usr_updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private FingerPrint fingerPrint;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private RfidCard rfidCard;
}
