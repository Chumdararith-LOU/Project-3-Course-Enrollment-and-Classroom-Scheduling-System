package com.cource.repository;

import com.cource.entity.AcademicTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicTermRepository extends JpaRepository<AcademicTerm, Long> {
    Optional<AcademicTerm> findByTermCode(String termCode);

    List<AcademicTerm> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate startDate, LocalDate endDate);

    // Find the latest term code that starts with a given prefix (for auto-generation)
    @Query("SELECT t.termCode FROM AcademicTerm t WHERE t.termCode LIKE :prefix% ORDER BY t.termCode DESC")
    List<String> findTermCodesByPrefix(String prefix);

    // Count terms in a given year for auto-numbering
    @Query("SELECT COUNT(t) FROM AcademicTerm t WHERE YEAR(t.startDate) = :year")
    long countTermsByYear(int year);
}