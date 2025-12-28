package com.cource.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cource.entity.Waitlist;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {
    List<Waitlist> findByStudentIdOrderByPositionAsc(Long studentId);

    List<Waitlist> findByOfferingIdOrderByPositionAsc(Long offeringId);
}
