package com.cource.repository;

import com.cource.entity.AcademicTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicTermRepository extends JpaRepository<AcademicTerm, Long> {
    List<AcademicTerm> findByIsActiveTrue();
}
