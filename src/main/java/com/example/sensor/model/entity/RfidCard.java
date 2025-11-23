package com.example.sensor.model.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rfid_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RfidCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rfid_id_int")
    private Integer id;

    @Column(name = "rfid_uid_vac", nullable = false, unique = true)
    private String cardUid;

    @Column(name = "rfid_estado_bol", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "rfid_autorizado_bol", nullable = false)
    @Builder.Default
    private Boolean authorized = true;

    @CreationTimestamp
    @Column(name = "rfid_cre_dt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "rfid_upd_dt")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "rfidCard", cascade = CascadeType.ALL)
    private List<AccessLog> accessLogs;

    @OneToOne
    @JoinColumn(name = "usr_id_int")
    private User user;
}