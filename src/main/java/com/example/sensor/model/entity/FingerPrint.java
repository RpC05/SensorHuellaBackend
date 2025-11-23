package com.example.sensor.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fingerprints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FingerPrint {
    @Id
<<<<<<< Updated upstream
    @Column(name = "fprint_id_int", nullable = false, unique = true)
    private Integer fingerprintId;

    @Column(name = "fprint_estado_bol", nullable = false)
=======
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Column(name="fprint_id_int", nullable = false, unique = true)
    private Integer fingerprintId;

    @Column(name="fprint_estado_bol", nullable = false)
>>>>>>> Stashed changes
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "fprint_cre_dt", updatable = false)
    private LocalDateTime enrolledAt;

    @UpdateTimestamp
    @Column(name = "fprint_upd_dt")
    private LocalDateTime updatedAt;

    @OneToOne
<<<<<<< Updated upstream
    @JoinColumn(name = "usr_id_int", nullable = true, unique = true)
=======
    @JoinColumn(name = "usr_id_int", nullable = false, unique = true)
>>>>>>> Stashed changes
    private User user;
}