package com.cource.repository;

import com.cource.entity.AcademicTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicTermRepository extends JpaRepository<AcademicTerm, Long> {
    Optional<AcademicTerm> findByTermCode(String termCode);

    List<AcademicTerm> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate startDate, LocalDate endDate);
}