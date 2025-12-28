package com.cource.repository;

import com.cource.entity.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {
    
    // Find max position for an offering
    @Query("SELECT MAX(w.position) FROM Waitlist w WHERE w.offering.id = :offeringId AND w.status = 'PENDING'")
    Integer findMaxPositionByOfferingId(@Param("offeringId") Long offeringId);
    
    // Find first person in waitlist for an offering
    @Query("SELECT w FROM Waitlist w WHERE w.offering.id = :offeringId AND w.status = 'PENDING' ORDER BY w.position ASC")
    Optional<Waitlist> findFirstByOfferingIdOrderByPosition(@Param("offeringId") Long offeringId);
    
    // Check if student is already waitlisted
    boolean existsByStudentIdAndOfferingId(Long studentId, Long offeringId);
    
    // Count waitlisted students for an offering
    @Query("SELECT COUNT(w) FROM Waitlist w WHERE w.offering.id = :offeringId AND w.status = 'PENDING'")
    long countByOfferingIdAndStatus(@Param("offeringId") Long offeringId);
}
