package com.example.sensor.repository;

import com.example.sensor.model.entity.AccessLog; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccessLogRepository extends JpaRepository<AccessLog, Integer> {
    List<AccessLog> findByRfidCard_CardUidOrderByAccessTimeDesc(String cardUid);
    List<AccessLog> findByAccessTimeBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT a FROM AccessLog a WHERE a.rfidCard.id = :cardId ORDER BY a.accessTime DESC")
    Optional<AccessLog> findLastAccessByCard(@Param("cardId") Integer cardId);
    
    @Query("SELECT COUNT(a) FROM AccessLog a WHERE a.authorized = true")
    Long countAuthorizedAccesses();
    
    @Query("SELECT COUNT(DISTINCT a.rfidCard.id) FROM AccessLog a WHERE a.accessTime >= :startDate")
    Long countUniqueUsersToday(@Param("startDate") LocalDateTime startDate);
}