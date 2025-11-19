package com.example.sensor.model.entity;

import java.time.LocalDateTime;
import com.example.sensor.model.enums.AccessType;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "access_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class AccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_id_int")
    private Integer id; 

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type_enum", nullable = false)
    private AccessType accessType;

    @Column(name = "access_authorized_bol", nullable = false)
    private Boolean authorized;

    @CreationTimestamp
    @Column(name = "access_time_dt", nullable = false)
    private LocalDateTime accessTime;

    @Column(name = "access_location_vac")
    private String location;

    @Column(name = "access_device_id_vac")
    private String deviceId;

    @Column(name = "access_notes_vac")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfid_id_int")
    private RfidCard rfidCard;
}
