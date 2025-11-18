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
@Table(name = "fingerprints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FingerPrint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="fprint_id_int", nullable = false, unique = true)
    private Integer fingerprintId;

    // Datos personales
    @Column(name="fprint_nombres_vac", nullable = false)
    private String nombres;

    @Column(name="fprint_apellido_paterno_vac", nullable = false)
    private String apellidoPaterno;

    @Column(name="fprint_apellido_materno_vac")
    private String apellidoMaterno;

    @Column(name="fprint_fecha_nacimiento_dt")
    private LocalDate fechaNacimiento;

    @Column(name="fprint_tipo_documento_vac")
    private String tipoDocumento;

    @Column(name="fprint_nro_documento_vac", unique = true)
    private String numeroDocumento;

    @Column(name="fprint_desc_vac")
    private String description;

    @CreationTimestamp
    @Column(name="fprint_cre_dt", updatable = false)
    private LocalDateTime enrolledAt;

    @UpdateTimestamp
    @Column(name="fprint_upd_dt")
    private LocalDateTime updatedAt;

    @Column(name="fprint_estado_bol", nullable = false)
    @Builder.Default
    private Boolean active = true;
}