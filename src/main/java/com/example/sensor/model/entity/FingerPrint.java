package com.example.sensor.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "fingerprints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FingerPrint {
    @Id
    @Column(name = "fprint_id_int", nullable = false, unique = true)
    private Integer fingerprintId;

    @Column(name = "fprint_estado_bol", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "fprint_cre_dt", updatable = false)
    private LocalDateTime enrolledAt;

    @UpdateTimestamp
    @Column(name = "fprint_upd_dt")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "fingerPrint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccessLog> accessLogs;

    @OneToOne
    @JoinColumn(name = "usr_id_int", nullable = true, unique = true)
    private User user;
}