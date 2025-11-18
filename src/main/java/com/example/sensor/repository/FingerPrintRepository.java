package com.example.sensor.repository;

import com.example.sensor.model.entity.FingerPrint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FingerPrintRepository extends JpaRepository<FingerPrint,Integer> {
    Optional<FingerPrint> findByFingerprintId(Integer fingerprintId);
    Optional<FingerPrint> findByFingerprintIdAndActiveTrue(Integer fingerprintId);
    boolean existsByFingerprintId(Integer fingerprintId);
    List<FingerPrint> findAllByActiveTrue();

    @Query("SELECT COALESCE(MAX(f.fingerprintId), 0) FROM FingerPrint f")
    Integer findMaxFingerprintId();

    Integer countByActiveTrue();
}
