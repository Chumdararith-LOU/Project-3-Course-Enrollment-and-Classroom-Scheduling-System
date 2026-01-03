package com.cource.repository;

import com.cource.entity.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    @Query("SELECT MAX(w.position) FROM Waitlist w WHERE w.offering.id = :offeringId AND w.status = 'PENDING'")
    Integer findMaxPositionByOfferingId(@Param("offeringId") Long offeringId);

    @Query("SELECT w FROM Waitlist w WHERE w.offering.id = :offeringId AND w.status = 'PENDING' ORDER BY w.position ASC")
    Optional<Waitlist> findFirstByOfferingIdOrderByPosition(@Param("offeringId") Long offeringId);

    boolean existsByStudentIdAndOfferingId(Long studentId, Long offeringId);

    @Query("SELECT COUNT(w) FROM Waitlist w WHERE w.offering.id = :offeringId AND w.status = 'PENDING'")
    long countByOfferingIdAndStatus(@Param("offeringId") Long offeringId);

    List<Waitlist> findByStudentIdOrderByPositionAsc(Long studentId);

    List<Waitlist> findByOfferingIdOrderByPositionAsc(Long offeringId);
}