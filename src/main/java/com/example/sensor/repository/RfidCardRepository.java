package com.example.sensor.repository;

import com.example.sensor.model.entity.RfidCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface RfidCardRepository extends JpaRepository<RfidCard, Integer> {
    Optional<RfidCard> findByCardUid(String cardUid);
    Optional<RfidCard> findByCardUidAndActiveTrue(String cardUid);
    boolean existsByCardUid(String cardUid);
    List<RfidCard> findAllByActiveTrue();
    List<RfidCard> findAllByActiveTrueAndAuthorizedTrue();
    
    @Query("SELECT COUNT(c) FROM RfidCard c WHERE c.active = true")
    Long countActiveCards();
}