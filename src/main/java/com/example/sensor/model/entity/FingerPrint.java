package com.example.sensor.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

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

    @Column(name="fprint_name_vac", nullable = false)
    private String name;

    @Column(name="fprint_desc_vac")
    @JdbcTypeCode(SqlTypes.LONGNVARCHAR)
    private String description;

    @Column(name = "fprint_template_data_vac")
    @JdbcTypeCode(SqlTypes.LONGNVARCHAR)
    private String templateData;

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